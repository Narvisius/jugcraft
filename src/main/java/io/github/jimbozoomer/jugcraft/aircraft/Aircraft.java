package io.github.jimbozoomer.jugcraft.aircraft;

import io.github.jimbozoomer.jugcraft.SmoothFlight;
import io.github.jimbozoomer.jugcraft.concordance.*;
import io.github.jimbozoomer.jugcraft.artillery.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.util.Mth;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CookingFuel;
import net.minecraft.world.level.*;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.phys.*;

/** Server-owned flight and inventory. The imported client art never decides position, shots or fuel. */
public final class Aircraft extends Entity {
    private static final EntityDataAccessor<Float> POWER=SynchedEntityData.defineId(Aircraft.class,EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> CONTROLS=SynchedEntityData.defineId(Aircraft.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> PAINT=SynchedEntityData.defineId(Aircraft.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> WEAPONS=SynchedEntityData.defineId(Aircraft.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<ItemStack> BANNER=SynchedEntityData.defineId(Aircraft.class,EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Float> AIM_YAW=SynchedEntityData.defineId(Aircraft.class,EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> AIM_PITCH=SynchedEntityData.defineId(Aircraft.class,EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> SHOT=SynchedEntityData.defineId(Aircraft.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DISPLAY=SynchedEntityData.defineId(Aircraft.class,EntityDataSerializers.INT);
    private final AircraftSpec spec;
    private final SimpleContainer inventory;
    private UUID owner;
    private int bodyPaint=0xFFFFFFFF;
    private int forward,turn,vertical,inputTick=-100,fuel,boostTicks;
    private boolean fire,boost;
    private float damage;
    private double fuelFraction;
    private final Map<Integer,Integer> ammunition=new HashMap<>(),shotTicks=new HashMap<>();
    private final Map<Integer,ItemStack> loadedAmmo=new HashMap<>();
    private final Map<Integer,String> loadedWeapon=new HashMap<>();
    private int lastRepair=-100;
    public float rotation,previousRotation;
    public Aircraft(EntityType<? extends Aircraft> type,Level level,AircraftSpec spec){
        super(type,level);this.spec=spec;inventory=new SimpleContainer(spec.sockets().size());
    }
    public AircraftSpec spec(){return spec;}
    public SimpleContainer inventory(){return inventory;}
    public void setOwner(UUID id){owner=id;}
    public UUID owner(){return owner;}
    public boolean mayUse(Player player){return owner==null||owner.equals(player.getUUID())||player.getAbilities().instabuild
        ||io.github.jimbozoomer.jugcraft.party.JugcraftParties.sameParty(owner,player.getUUID());}
    public int fuel(){return fuel;}
    public float healthFraction(){return Mth.clamp(1-damage/(float)(40*stat("durability",1)),0,1);}
    public int loadedRounds(int slot){return ammunition.getOrDefault(slot,0);}
    public float power(){return entityData.get(POWER);}
    public int paint(){return entityData.get(PAINT);}
    public void paint(int color){bodyPaint=0xFF000000|(color&0xFFFFFF);entityData.set(PAINT,bodyPaint);}
    public int control(int axis){return ((entityData.get(CONTROLS)>>(axis*2))&3)-1;}
    public int weapons(){return entityData.get(WEAPONS);}
    public ItemStack banner(){return entityData.get(BANNER);}
    public float aimYaw(){return entityData.get(AIM_YAW);}
    public float aimPitch(){return entityData.get(AIM_PITCH);}
    public float recoil(){return Math.max(0,1-(level().getGameTime()-entityData.get(SHOT))/6F);}
    public int instruments(){return entityData.get(DISPLAY);}
    public boolean musclePowered(){return spec.name().equals("gyrodyne");}
    public ContainerData status(){return new ContainerData(){public int get(int n){return n==0?Math.min(fuel,32767):(int)(power()*100);}public void set(int n,int v){}public int getCount(){return 2;}};}
    public static String name(ItemStack item){String p=BuiltInRegistries.ITEM.getKey(item.getItem()).toString();return p.startsWith("jugcraft:aircraft_")?p.substring("jugcraft:aircraft_".length()):"";}
    public static boolean accepts(String socket,ItemStack item){
        if(item.isEmpty())return false;
        return switch(socket){
            case "boiler"->item.has(DataComponents.COOKING_FUEL);
            case "upgrade"->AircraftSpec.UPGRADES.containsKey(name(item));
            case "weapon"->Set.of("rotary_cannon","heavy_crossbow","telescope","bomb_bay").contains(name(item));
            case "banner"->item.getItem() instanceof BannerItem;
            case "dye"->item.has(DataComponents.DYE);
            case "booster"->item.is(Items.GUNPOWDER)||item.is(Items.FIREWORK_ROCKET);
            default->true;
        };
    }
    public double stat(String key,double fallback){
        double modifier=0;Set<String> counted=new HashSet<>();
        for(int i=0;i<inventory.getContainerSize();i++)if(spec.sockets().get(i).type().equals("upgrade")){
            String name=name(inventory.getItem(i));var upgrade=AircraftSpec.UPGRADES.get(name);
            if(upgrade!=null&&counted.add(name)&&upgrade.has(key))modifier+=upgrade.get(key).getAsDouble();
        }
        return spec.value(key,fallback)*Math.clamp(1+modifier,key.equals("wind")?0:.1,5);
    }
    public InteractionResult use(Player player,InteractionHand hand){
        if(!mayUse(player)){if(!level().isClientSide())player.sendOverlayMessage(Component.literal("This aircraft belongs to another pilot."));return InteractionResult.SUCCESS;}
        if(level() instanceof ServerLevel level&&!Authority.mayChange(level,player,blockPosition()))return InteractionResult.FAIL;
        if(!player.isSecondaryUseActive()&&!player.isPassenger()&&player.getItemInHand(hand).isEmpty()&&damage>0){
            if(level() instanceof ServerLevel level&&tickCount-lastRepair>=5){
                lastRepair=tickCount;damage=Math.max(0,damage-(float)(40*stat("durability",1)*.025));
                if(!player.getAbilities().instabuild)player.causeFoodExhaustion(.5F);
                level.playSound(null,blockPosition(),SoundEvents.ANVIL_USE,SoundSource.PLAYERS,.3F,.8F);
                player.sendOverlayMessage(Component.literal("Aircraft repaired · "+Math.round(healthFraction()*100)+"% hull"));
            }return InteractionResult.SUCCESS;
        }
        if(player.isSecondaryUseActive()){
            if(!level().isClientSide())player.openMenu(new SimpleMenuProvider((id,inv,p)->new AircraftMenu(id,inv,this),getDisplayName()));
            return InteractionResult.SUCCESS;
        }
        if(!player.isPassenger()&&getPassengers().size()<spec.seats()){
            if(!level().isClientSide()){if(owner==null)owner=player.getUUID();player.startRiding(this);}
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
    public void steer(ServerPlayer player,AircraftInput input){
        if(getFirstPassenger()!=player||!mayUse(player))return;
        forward=Mth.clamp(input.forward(),-1,1);turn=Mth.clamp(input.turn(),-1,1);vertical=Mth.clamp(input.vertical(),-1,1);
        fire=input.fire();boost=input.boost();inputTick=tickCount;
    }
    @Override public LinearInterpolationHandler createInterpolationHandler(){return SmoothFlight.handler(this);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){b.define(POWER,0F);b.define(CONTROLS,21);b.define(PAINT,0xFFFFFFFF);b.define(WEAPONS,0);b.define(BANNER,ItemStack.EMPTY);b.define(AIM_YAW,0F);b.define(AIM_PITCH,0F);b.define(SHOT,-100);b.define(DISPLAY,0);}
    @Override public boolean isPickable(){return !isRemoved();}
    @Override protected boolean canAddPassenger(Entity rider){return getPassengers().size()<spec.seats();}
    @Override protected Vec3 getPassengerAttachmentPoint(Entity passenger,EntityDimensions d,float scale){return spec.seat(getPassengers().indexOf(passenger),getPassengers().size()).xRot(getXRot()*Mth.DEG_TO_RAD).yRot(-getYRot()*Mth.DEG_TO_RAD).add(0,spec.groundOffset(),0);}
    @Override public void tick(){
        super.tick();previousRotation=rotation;rotation+=power();
        if(!(level() instanceof ServerLevel level))return;
        ServerPlayer pilot=getFirstPassenger() instanceof ServerPlayer p&&mayUse(p)?p:null;
        boolean listening=pilot!=null&&tickCount-inputTick<=10&&pilot.containerMenu==pilot.inventoryMenu;
        int f=listening?forward:0,t=listening?turn:0,v=listening?vertical:0;
        entityData.set(CONTROLS,(f+1)|((t+1)<<2)|((v+1)<<4));
        boolean driving=pilot!=null&&(spec.aeroplane()?f!=0||!onGround():true);
        if(musclePowered()){
            fuel=driving&&(pilot.getAbilities().instabuild||pilot.getFoodData().getFoodLevel()>5)?20:0;
            if(driving&&fuel>0&&!pilot.getAbilities().instabuild)pilot.causeFoodExhaustion(.005F);
        }else if(driving&&fuel<=0)refuel(level);
        if(driving&&fuel>0&&power()<.001F)level.playSound(null,blockPosition(),JugcraftAircraft.SOUNDS.get(spec.name().equals("warship")?"engine_start_warship":spec.name().equals("bamboo_hopper")?"engine_start_bamboo_hopper":"engine_start"),SoundSource.NEUTRAL,.7F,1);
        float target=driving&&fuel>0?(musclePowered()&&f<=0?power()*.98F:1):0;
        float power=Mth.lerp(.06F,power(),target);if(power<.001F)power=0;
        entityData.set(POWER,power);
        if(driving&&fuel>0){fuelFraction+=stat("fuel",1);int used=(int)fuelFraction;fuel=Math.max(0,fuel-used);fuelFraction-=used;}
        if(listening&&boost&&boostTicks==0&&power>.5F&&!hasTelescope()&&consume("booster",null))boostTicks=60;
        double boostFactor=boostTicks>0?1.5:1; if(boostTicks>0)boostTicks--;
        setYRot(getYRot()-(float)(t*stat("yawSpeed",4)*.3*power));
        Vec3 heading=new Vec3(-Math.sin(getYRot()*Mth.DEG_TO_RAD),0,Math.cos(getYRot()*Mth.DEG_TO_RAD));
        Vec3 motion=getDeltaMovement();double speed=motion.x*heading.x+motion.z*heading.z;
        double max=stat("engineSpeed",.02)*(musclePowered()?1:25)*boostFactor;
        double wanted=power*(f>0?max:f<0?-max*.25:0);
        double acceleration=.012*stat("acceleration",1)*(onGround()?1:0.7);
        speed+=Mth.clamp(wanted-speed,-acceleration,acceleration);
        speed*=1-Math.clamp(stat("friction",.001),0,.05);
        double dy;
        if(spec.aeroplane()){
            // A wing needs forward speed; holding climb while parked cannot levitate a biplane.
            double lift=Mth.clamp(Math.max(0,speed)/Math.max(.1,max*.55),0,1);
            dy=motion.y*.9-.04*(1-lift)+v*.025*lift*power;
            if(v==0&&lift>.95)dy*=1-Math.clamp(stat("stabilizer",.1),.01,.8);
            setXRot(Mth.lerp(.12F,getXRot(),(float)(-v*18*lift)));
        }else{dy=power>.3?(v*stat("verticalSpeed",.02)*5*power+motion.y*(1-Math.clamp(stat("stabilizer",.3),.05,.8))*(v==0?1:0)):Math.max(-.15,motion.y-.015);setXRot(Mth.lerp(.1F,getXRot(),f*-4F));}
        if(pilot==null&&onGround()){speed*=.7;dy=Math.min(0,dy);}
        if(!level.getWorldBorder().isWithinBounds(blockPosition().offset((int)Math.signum(heading.x*speed),0,(int)Math.signum(heading.z*speed))))speed=0;
        if(getY()+dy>=level.getMaxY()-2)dy=Math.min(0,dy);
        double wind=onGround()?0:Math.sin((level.getGameTime()+getId()*31)*.003)*stat("wind",.02)*.05;
        setDeltaMovement(heading.x*speed+wind,Math.clamp(dy,-.5,.35),heading.z*speed);
        move(MoverType.SELF,getDeltaMovement());
        if(horizontalCollision)setDeltaMovement(0,getDeltaMovement().y,0);
        for(var p:getPassengers())p.resetFallDistance();resetFallDistance();
        synchronizeEquipment();
        entityData.set(AIM_YAW,pilot==null?0F:Mth.wrapDegrees(pilot.getYRot()-getYRot()));
        entityData.set(AIM_PITCH,pilot==null?0F:Mth.clamp(pilot.getXRot()-getXRot(),-80,80));
        if(listening&&fire&&pilot!=null)fire(level,pilot);
        if(pilot!=null&&tickCount%40==0)pilot.sendOverlayMessage(Component.literal((musclePowered()?"Pedal W to spin rotor":"Fuel "+fuel/20+"s")+"  ·  WASD steer  ·  Space climb / Sprint descend  ·  Attack weapon  ·  Use boost"));
    }
    private void synchronizeEquipment(){
        int paint=bodyPaint,weapons=0,index=0,instruments=0;ItemStack banner=ItemStack.EMPTY;
        for(int i=0;i<inventory.getContainerSize();i++){
            var s=inventory.getItem(i);String type=spec.sockets().get(i).type();
            if(type.equals("dye")&&s.has(DataComponents.DYE))paint=0xFF000000|s.get(DataComponents.DYE).getTextureDiffuseColor();
            if(type.equals("banner")&&s.getItem() instanceof BannerItem)banner=s.copyWithCount(1);
            if(type.equals("upgrade")){String n=name(s);if(n.equals("gyroscope_hud"))instruments|=1;if(n.equals("gyroscope_dials"))instruments|=2;}
            if(type.equals("weapon")){int n=switch(name(s)){case "rotary_cannon"->1;case "heavy_crossbow"->2;case "bomb_bay"->3;case "telescope"->4;default->0;};weapons|=n<<(index++*3);}
        }
        entityData.set(PAINT,paint);entityData.set(WEAPONS,weapons);
        if(!ItemStack.matches(banner(),banner))entityData.set(BANNER,banner);
        entityData.set(DISPLAY,instruments);
    }
    public boolean hasTelescope(){for(int i=0;i<5;i++)if(((weapons()>>(i*3))&7)==4)return true;return false;}
    private void refuel(ServerLevel level){
        for(int i=0;i<inventory.getContainerSize();i++)if(spec.sockets().get(i).type().equals("boiler")){
            var s=inventory.getItem(i);if(!s.has(DataComponents.COOKING_FUEL))continue;
            var ctx=new LootContext.Builder(new LootParams.Builder(level).create(LootContextParamSets.EMPTY)).create(Optional.empty());
            int ticks=net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt.getFromItem(s,DataComponents.COOKING_FUEL,CookingFuel::burnTime,ctx,0);
            var remainder=s.getItem().getCraftingRemainder();if(remainder!=null&&!remainder.create().isEmpty()&&s.getCount()!=1)continue;
            if(ticks>0){fuel=Math.clamp(ticks,1,1000000);s.shrink(1);if(remainder!=null&&s.isEmpty())inventory.setItem(i,remainder.create());return;}
        }
    }
    private boolean consume(String socket,Item item){
        for(int i=0;i<inventory.getContainerSize();i++)if(spec.sockets().get(i).type().equals(socket)){
            var s=inventory.getItem(i);if(!s.isEmpty()&&(item==null?accepts(socket,s):s.is(item))){s.shrink(1);return true;}
        }return false;
    }
    private ItemStack ammunition(int slot,String weapon,boolean creative){
        int cost=weapon.equals("rotary_cannon")?10:weapon.equals("heavy_crossbow")?50:20;
        if(!weapon.equals(loadedWeapon.get(slot))){ammunition.remove(slot);loadedAmmo.remove(slot);loadedWeapon.put(slot,weapon);}
        if(creative)return loadedAmmo.getOrDefault(slot,new ItemStack(weapon.equals("heavy_crossbow")?Items.ARROW:weapon.equals("bomb_bay")?Items.TNT:Items.GUNPOWDER));
        if(ammunition.getOrDefault(slot,0)<cost){
            for(int i=0;i<inventory.getContainerSize();i++)if(spec.sockets().get(i).type().equals("inventory")){
                var stack=inventory.getItem(i);
                boolean valid=switch(weapon){case "heavy_crossbow"->stack.is(Items.ARROW)||stack.is(Items.TIPPED_ARROW)||stack.is(Items.SPECTRAL_ARROW);case "bomb_bay"->stack.is(Items.TNT);default->stack.is(Items.GUNPOWDER);};
                if(valid){loadedAmmo.put(slot,stack.copyWithCount(1));stack.shrink(1);ammunition.put(slot,100);break;}
            }
        }
        if(ammunition.getOrDefault(slot,0)<cost)return ItemStack.EMPTY;
        ammunition.put(slot,ammunition.get(slot)-cost);return loadedAmmo.get(slot).copy();
    }
    private void fire(ServerLevel level,ServerPlayer pilot){
        int mountIndex=-1;
        for(int i=0;i<inventory.getContainerSize();i++)if(spec.sockets().get(i).type().equals("weapon")){
            mountIndex++;
            String weapon=name(inventory.getItem(i));boolean bomb=weapon.equals("bomb_bay"),crossbow=weapon.equals("heavy_crossbow");
            if(!bomb&&!crossbow&&!weapon.equals("rotary_cannon"))continue;
            if(tickCount-shotTicks.getOrDefault(i,-100)<(bomb||crossbow?20:5))continue;
            var mounts=spec.definition().getAsJsonArray("weaponMounts");
            if(mounts==null||mountIndex>=mounts.size())continue;
            var positions=mounts.get(mountIndex).getAsJsonObject().getAsJsonArray(bomb?"DROP":crossbow?"FRONT":"ROTATING");
            if(positions==null||positions.isEmpty())continue;
            var ammo=ammunition(i,weapon,pilot.getAbilities().instabuild);if(ammo.isEmpty())continue;
            shotTicks.put(i,tickCount);
            entityData.set(SHOT,(int)level.getGameTime());
            for(var mount:positions){
            var m=mount.getAsJsonObject();Vec3 muzzle=position().add(0,spec.groundOffset(),0).add(new Vec3(m.get("x").getAsDouble(),m.get("y").getAsDouble(),m.get("z").getAsDouble()).xRot(getXRot()*Mth.DEG_TO_RAD).yRot(-getYRot()*Mth.DEG_TO_RAD));
            if(bomb){
                var shell=new ArtilleryShell(JugcraftArtillery.HEAVY_SHELL,level,pilot);
                shell.setPos(muzzle.add(0,-.8,0));shell.setDeltaMovement(getDeltaMovement().add(0,-.2,0));level.addFreshEntity(shell);
            }else if(crossbow){
                Vec3 direction=pilot.getLookAngle();Vec3 from=muzzle.add(direction.scale(1.25));
                net.minecraft.world.entity.projectile.arrow.AbstractArrow arrow=ammo.is(Items.SPECTRAL_ARROW)
                    ?new net.minecraft.world.entity.projectile.arrow.SpectralArrow(level,from.x,from.y,from.z,ammo,inventory.getItem(i))
                    :new net.minecraft.world.entity.projectile.arrow.Arrow(level,from.x,from.y,from.z,ammo,inventory.getItem(i));
                arrow.setOwner(pilot);arrow.addTag("jugcraft_aircraft_arrow");arrow.pickup=net.minecraft.world.entity.projectile.arrow.AbstractArrow.Pickup.DISALLOWED;
                arrow.shoot(direction.x,direction.y+.1,direction.z,3,0);arrow.setDeltaMovement(arrow.getDeltaMovement().add(getDeltaMovement()));level.addFreshEntity(arrow);
                level.playSound(null,blockPosition(),SoundEvents.CROSSBOW_SHOOT,SoundSource.PLAYERS,1,.8F);
            }else{
                Vec3 direction=pilot.getLookAngle();var round=new AircraftBullet(level,pilot);
                round.setPos(muzzle.add(direction));round.shoot(direction.x,direction.y,direction.z,4,1);
                round.setDeltaMovement(round.getDeltaMovement().add(getDeltaMovement()));level.addFreshEntity(round);
                level.playSound(null,blockPosition(),JugcraftAircraft.SOUNDS.get("cannon"),SoundSource.PLAYERS,1,1);
            }
            }
        }
    }
    @Override public boolean hurtServer(ServerLevel level,DamageSource source,float amount){
        if(!(source.getEntity() instanceof Player player)||!mayUse(player)||!Authority.mayChange(level,player,blockPosition())||isRemoved()||isVehicle())return false;
        damage+=amount;
        if(player.getAbilities().instabuild||healthFraction()<=0){
            if(!player.getAbilities().instabuild){var item=new ItemStack(JugcraftAircraft.ITEMS.get(spec.name()));item.set(DataComponents.DYED_COLOR,new net.minecraft.world.item.component.DyedItemColor(paint()&0xFFFFFF));spawnAtLocation(level,item);}
            Containers.dropContents(level,this,inventory);discard();
        }return true;
    }
    @Override protected void addAdditionalSaveData(ValueOutput out){
        if(owner!=null)out.putString("owner",owner.toString());out.putInt("fuel",fuel);out.putInt("paint",bodyPaint);out.putFloat("damage",damage);
        for(int i:ammunition.keySet()){out.putInt("ammo_"+i,ammunition.get(i));out.putString("ammo_weapon_"+i,loadedWeapon.get(i));out.store("ammo_item_"+i,ItemStack.OPTIONAL_CODEC,loadedAmmo.getOrDefault(i,ItemStack.EMPTY));}
        var items=NonNullList.withSize(inventory.getContainerSize(),ItemStack.EMPTY);for(int i=0;i<items.size();i++)items.set(i,inventory.getItem(i));ContainerHelper.saveAllItems(out,items);
    }
    @Override protected void readAdditionalSaveData(ValueInput in){
        try{owner=UUID.fromString(in.getString("owner").orElse(""));}catch(IllegalArgumentException e){owner=null;}
        fuel=Math.clamp(in.getInt("fuel").orElse(0),0,1000000);damage=in.getFloatOr("damage",0F);
        ammunition.clear();loadedAmmo.clear();loadedWeapon.clear();
        for(int i=0;i<inventory.getContainerSize();i++)if(spec.sockets().get(i).type().equals("weapon")){
            ammunition.put(i,Math.clamp(in.getInt("ammo_"+i).orElse(0),0,100));loadedWeapon.put(i,in.getString("ammo_weapon_"+i).orElse(""));loadedAmmo.put(i,in.read("ammo_item_"+i,ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
        }
        paint(in.getInt("paint").orElse(0xFFFFFFFF));
        var items=NonNullList.withSize(inventory.getContainerSize(),ItemStack.EMPTY);ContainerHelper.loadAllItems(in,items);for(int i=0;i<items.size();i++)inventory.setItem(i,items.get(i));
    }
}
