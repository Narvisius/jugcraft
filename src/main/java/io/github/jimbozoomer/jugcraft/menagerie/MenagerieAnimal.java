package io.github.jimbozoomer.jugcraft.menagerie;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.*;
import com.geckolib.util.GeckoLibUtil;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.control.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.ai.navigation.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Server-owned taming, breeding and habitat behavior; original mount clips follow movement. */
public class MenagerieAnimal extends Animal implements GeoEntity {
    private static final EntityDataAccessor<Integer> VARIANT=SynchedEntityData.defineId(MenagerieAnimal.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> OWNER=SynchedEntityData.defineId(MenagerieAnimal.class,EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> SITTING=SynchedEntityData.defineId(MenagerieAnimal.class,EntityDataSerializers.BOOLEAN),SADDLED=SynchedEntityData.defineId(MenagerieAnimal.class,EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> CHEST=SynchedEntityData.defineId(MenagerieAnimal.class,EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String> RECOVERED_ACTION=SynchedEntityData.defineId(MenagerieAnimal.class,EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Long> RECOVERED_ACTION_START=SynchedEntityData.defineId(MenagerieAnimal.class,EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Integer> RECOVERED_CHARGE=SynchedEntityData.defineId(MenagerieAnimal.class,EntityDataSerializers.INT);
    public float recoveredCharge(){int t=entityData.get(RECOVERED_CHARGE);return t<=0?0:1-t/20F;}
    protected void recoveredCharge(int ticks){entityData.set(RECOVERED_CHARGE,Math.clamp(ticks,0,20));}
    public String recoveredAction(){return entityData.get(RECOVERED_ACTION);}
    public void recoveredAction(String action){if(!level().isClientSide()&&RecoveredCreatureActions.duration(species.id(),action)>0){entityData.set(RECOVERED_ACTION,action);entityData.set(RECOVERED_ACTION_START,level().getGameTime());}}
    public float recoveredActionTime(float partial){long start=entityData.get(RECOVERED_ACTION_START);float elapsed=level().getGameTime()-start+partial;return start<0||elapsed<0||elapsed>=RecoveredCreatureActions.duration(species.id(),recoveredAction())?-1:elapsed;}
    protected final SimpleContainer cargo=new SimpleContainer(27);
    private final Menagerie.Species species;
    private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
    private final List<RecoveredCreaturePart> recoveredParts=new ArrayList<>();
    @Override public void remove(Entity.RemovalReason reason){
        // A segment can be outside the ticking area. Do not wait for its next tick
        // to clean it up when its authoritative parent leaves the world.
        for(var part:recoveredParts)part.discard();
        recoveredParts.clear();
        super.remove(reason);
    }
    private float previousRecoveredSit,recoveredSit;
    private float previousPropeller,propeller,recoveredSquish;
    private boolean previousGround;
    private float recoveredFlap,previousFlap,recoveredSwoop,previousSwoop,previousBirdYaw;
    public float recoveredFlap(float partial){return net.minecraft.util.Mth.lerp(partial,previousFlap,recoveredFlap);}
    public float recoveredSwoop(float partial){return net.minecraft.util.Mth.lerp(partial,previousSwoop,recoveredSwoop);}
    private void recoveredBirdMotion(){
        previousFlap=recoveredFlap;previousSwoop=recoveredSwoop;
        float vertical=(float)getDeltaMovement().y,pitch=-vertical*57.295776F;
        switch(species.source()){
            case "bald_eagle" -> {
                if(pitch<.1F){recoveredFlap=Math.min(-pitch*.2F,1);recoveredSwoop=Math.max(0,recoveredSwoop-1);}
                else{recoveredFlap=Math.max(0,recoveredFlap-.1F);if(recoveredSwoop<pitch*.2F)recoveredSwoop=Math.min(pitch*.2F,recoveredSwoop+1);}
                if(recoveredCharge()>0)recoveredFlap=Math.min(2,recoveredFlap+.2F);
            }
            case "blue_jay" -> {if(vertical>=0)recoveredFlap=Math.min(1,recoveredFlap+.25F);else if(vertical<-.07F)recoveredFlap=Math.max(0,recoveredFlap-.25F);}
            case "seagull" -> {float turn=Math.abs(net.minecraft.util.Mth.wrapDegrees(getYRot()-previousBirdYaw));recoveredFlap=turn>8?Math.min(1,recoveredFlap+.1F):pitch<0?Math.min(-pitch*.2F,1):Math.max(0,recoveredFlap-.05F);}
        }
        previousBirdYaw=getYRot();
    }
    public float recoveredSquish(){return recoveredSquish;}
    public float recoveredPropeller(float partial){return net.minecraft.util.Mth.lerp(partial,previousPropeller,propeller);}
    public float recoveredSitProgress(float partial){return net.minecraft.util.Mth.lerp(partial,previousRecoveredSit,recoveredSit);}
    @Override public void tick(){super.tick();previousRecoveredSit=recoveredSit;recoveredSit=net.minecraft.util.Mth.clamp(recoveredSit+(sitting()?.2F:-.2F),0,1);RecoveredCreaturePart.update(this,recoveredParts);
        recoveredBirdMotion();recoveredSquish=Math.max(0,recoveredSquish-.15F);if(onGround()&&!previousGround)recoveredSquish=.5F;previousGround=onGround();
        if(species.id().equals("cavern_notor")){previousPropeller=propeller;if(!onGround())propeller+=(float)Math.max(getDeltaMovement().horizontalDistance()*10,3)*20;else propeller=net.minecraft.util.Mth.approachDegrees(propeller,0,15);}
    }
    private int produceTicks=6000,attackTicks,flowerVariant=-1,flowerRegrowTicks,harvestTicks;
    public MenagerieAnimal(EntityType<? extends Animal> type,Level level,Menagerie.Species species){
        super(type,level);this.species=species;
        if(LavaSwimming.species(species)){navigation=new LavaSwimming.Navigation(this,level);moveControl=new LavaSwimming.Control(this);}
        else if(species.aquatic()){navigation=new WaterBoundPathNavigation(this,level);moveControl=new SmoothSwimmingMoveControl(this,85,10,.08F,.02F,false);}
        else if(species.flying()){var nav=new FlyingPathNavigation(this,level);nav.setCanFloat(true);navigation=nav;moveControl=new FlyingMoveControl(this,12,!BirdFlightGoal.applies(species));}
        // BreedGoal captures ServerLevel in its constructor. Client replicas only
        // render synchronized state and must never construct server AI goals.
        if(!level.isClientSide())installGoals();
    }
    public Menagerie.Species species(){return species;}
    public int variant(){return Math.floorMod(entityData.get(VARIANT),species.variants().size());}
    public void variant(int variant){entityData.set(VARIANT,Math.floorMod(variant,species.variants().size()));}
    public String texture(){var v=species.variants().get(variant());String texture=isBaby()?v.babyTexture():v.texture();return species.id().equals("grove_lizard")&&sitting()?texture.replace("/lizard_","/sad_lizard_"):texture;}
    public boolean sitting(){return entityData.get(SITTING);}
    public boolean saddled(){return entityData.get(SADDLED);}
    public boolean chested(){return entityData.get(CHEST);}
    public boolean readyToHarvest(){return harvestTicks==0;}
    public void harvested(){harvestTicks=6000;}
    public boolean carriesCargo(){return species.mount()||species.id().equals("companion_roly_poly");}
    public void refreshArmor(){getAttribute(Attributes.ARMOR).setBaseValue(switch(armor()){case "straddle"->8;case "netherite"->13;case "diamond"->11;case "golden"->7;case "iron"->5;case "leather"->3;default->0;});}
    public String armor(){String id=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(getItemBySlot(EquipmentSlot.BODY).getItem()).getPath();if(id.equals("beast_straddle_helmet"))return "straddle";if(id.startsWith("companion_")&&id.endsWith("_dragonfly_armor")){String tier=id.substring(10,id.length()-16);return tier.equals("gold")?"golden":tier;}return id.endsWith("horse_armor")?id.substring(0,id.indexOf('_')):"";}
    public @Nullable UUID owner(){try{return UUID.fromString(entityData.get(OWNER));}catch(IllegalArgumentException e){return null;}}
    public void tame(UUID owner){entityData.set(OWNER,owner.toString());setPersistenceRequired();setTarget(null);}
    void saddle(){entityData.set(SADDLED,true);}
    public boolean ownedBy(Player p){return p.getUUID().equals(owner());}
    public void sit(boolean value){entityData.set(SITTING,value);if(value){getNavigation().stop();setTarget(null);}}
    public String model(){if(species.id().equals("grove_cindershell")&&!isBaby()&&!getItemBySlot(EquipmentSlot.BODY).isEmpty())return "grove/cindershell/cindershell_furnace";if(species.id().equals("grove_lizard")&&sitting())return "grove/lizard/"+(species.variants().get(variant()).name().contains("mushroom")?"sad_mushroom_lizard":"sad_lizard");var v=species.variants().get(variant());String model=isBaby()?v.babyModel():v.model();return model==null||model.isEmpty()?species.model():model;}
    public void equipSaved(EquipmentSlot slot,ItemStack stack){setItemSlotAndDropWhenKilled(slot,stack);}
    public void equipBody(ItemStack stack){setItemSlotAndDropWhenKilled(EquipmentSlot.BODY,stack);}
    public void shearFlowers(){flowerVariant=variant();flowerRegrowTicks=6000;variant(3);}
    public static AttributeSupplier.Builder attributes(Menagerie.Species s){return Animal.createAnimalAttributes().add(Attributes.MAX_HEALTH,s.health()).add(Attributes.MOVEMENT_SPEED,s.mount()?.3:s.shape().equals("shell")?.12:.23).add(Attributes.FLYING_SPEED,.35).add(Attributes.ATTACK_DAMAGE,s.width()>1.5?8:3).add(Attributes.FOLLOW_RANGE,24);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(VARIANT,0);b.define(OWNER,"");b.define(SITTING,false);b.define(SADDLED,false);b.define(CHEST,false);b.define(RECOVERED_ACTION,"");b.define(RECOVERED_ACTION_START,-1L);b.define(RECOVERED_CHARGE,0);}
    @Override protected void registerGoals(){}
    private void installGoals(){
        goalSelector.addGoal(0,new Goal(){
            {setFlags(EnumSet.of(Flag.MOVE,Flag.JUMP));}
            public boolean canUse(){return sitting()||isVehicle();}
            public void tick(){getNavigation().stop();}
        });
        if(!species.aquatic())goalSelector.addGoal(1,new FloatGoal(this));
        goalSelector.addGoal(2,new BreedGoal(this,1));
        goalSelector.addGoal(3,new TemptGoal(this,1,s->isFood(s),false));
        goalSelector.addGoal(4,new FollowParentGoal(this,1));
        if(!species.mood().equals("passive")){
            goalSelector.addGoal(2,new MeleeAttackGoal(this,1.15,true));targetSelector.addGoal(1,new HurtByTargetGoal(this));
            if(species.predator())targetSelector.addGoal(3,new NearestAttackableTargetGoal<>(this,Animal.class,30,true,false,(e,l)->owner()==null&&e instanceof Animal a&&canHunt(a)));
            if(species.mood().equals("hostile"))targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,Player.class,20,true,false,(e,l)->owner()==null&&e instanceof Player p&&!p.isCreative()&&!p.isSpectator()&&!FableCreatures.wards(this,p)));
        }else goalSelector.addGoal(1,new PanicGoal(this,1.3));
        goalSelector.addGoal(5,new Goal(){
            {setFlags(EnumSet.of(Flag.MOVE));}
            public boolean canUse(){return !sitting()&&!isVehicle()&&owner()!=null&&level() instanceof ServerLevel l&&Authority.present(l,owner()) instanceof ServerPlayer p&&distanceToSqr(p)>36&&distanceToSqr(p)<1024;}
            public void tick(){if(tickCount%10==0&&level() instanceof ServerLevel l){var p=Authority.present(l,owner());if(p!=null)getNavigation().moveTo(p,1);}}
        });
        goalSelector.addGoal(6,LavaSwimming.species(species)?new LavaSwimming.Wander(this):species.aquatic()?new RandomSwimmingGoal(this,1,40):species.flying()?(BirdFlightGoal.applies(species)?new BirdFlightGoal(this):new WaterAvoidingRandomFlyingGoal(this,.8)):new WaterAvoidingRandomStrollGoal(this,.8));
        goalSelector.addGoal(7,new LookAtPlayerGoal(this,Player.class,8));goalSelector.addGoal(8,new RandomLookAroundGoal(this));
    }
    public boolean canHunt(Animal prey){return prey.getType()!=getType()&&prey.getBbWidth()<getBbWidth()&&!prey.isLeashed()
        &&!(prey instanceof MenagerieAnimal pet&&pet.owner()!=null)
        &&!(prey instanceof net.minecraft.world.entity.TamableAnimal pet&&pet.isTame())
        &&!(prey instanceof net.minecraft.world.entity.animal.equine.AbstractHorse horse&&horse.isTamed());}
    @Override public boolean isFood(ItemStack stack){return !species.mood().equals("hostile")&&stack.is(Menagerie.feed(species));}
    @Override public boolean canMate(Animal other){return !species.mood().equals("hostile")&&other instanceof MenagerieAnimal m&&m.getType()==getType()&&super.canMate(other);}
    private int inheritedVariant(){return flowerVariant>=0?flowerVariant:variant();}
    @Override public @Nullable AgeableMob getBreedOffspring(ServerLevel level,AgeableMob mate){
        if(species.mood().equals("hostile"))return null;
        var child=Menagerie.TYPES.get(species.id()).create(level,EntitySpawnReason.BREEDING);if(child!=null){child.variant(random.nextBoolean()?inheritedVariant():mate instanceof MenagerieAnimal a?a.inheritedVariant():inheritedVariant());if(owner()!=null&&mate instanceof MenagerieAnimal a&&owner().equals(a.owner()))child.tame(owner());}return child;
    }
    @Override public void spawnChildFromBreeding(ServerLevel level,Animal mate){
        // These supplied eggs already hatch, but previously breeding skipped
        // them and produced a baby directly. Drop one collectable egg so water
        // breeding does not require overwriting the parents' habitat blocks.
        String incubated=switch(species.id()){
            case "beast_caiman","beast_platypus","beast_terrapin","cavern_atlatitan","cavern_grottoceratops","cavern_relicheirus","cavern_subterranodon","cavern_tremorsaurus","cavern_vallumraptor"->species.id()+"_egg";
            case "beast_triops"->"beast_triops_eggs";
            default->"";
        };
        if(!incubated.isEmpty()){
            if(owner()!=null){var p=Authority.present(level,owner());if(p==null||!Authority.mayChange(level,p,blockPosition()))return;}
            if(mate instanceof MenagerieAnimal m&&m.owner()!=null){var p=Authority.present(level,m.owner());if(p==null||!Authority.mayChange(level,p,blockPosition()))return;}
            if(spawnAtLocation(level,new ItemStack(Menagerie.item(incubated)))!=null){setAge(6000);mate.setAge(6000);resetLove();mate.resetLove();}
            return;
        }
        String egg=switch(species.source()){case "alligator","ostrich","tortoise"->species.source()+"_egg";case "snail"->"snail_eggs";default->"";};
        if(species.id().startsWith("wildlife_")&&!egg.isEmpty()||species.id().equals("grove_lizard")){
            var block=Menagerie.BLOCKS.get(species.id().equals("grove_lizard")?"grove_lizard_egg_block":"wildlife_"+egg);var at=blockPosition();
            if(block!=null&&level.getBlockState(at).isAir()&&level.getBlockState(at.below()).isSolidRender()){
                if(owner()!=null){var p=Authority.present(level,owner());if(p==null||!Authority.mayChange(level,p,at))return;}
                level.setBlockAndUpdate(at,block.defaultBlockState());setAge(6000);mate.setAge(6000);resetLove();mate.resetLove();return;
            }
        }
        super.spawnChildFromBreeding(level,mate);
    }
    @Override public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor l,DifficultyInstance difficulty,EntitySpawnReason reason,@Nullable SpawnGroupData group){
        int total=species.variants().stream().mapToInt(v->Math.max(0,v.weight())).sum();int choice=random.nextInt(Math.max(1,total));
        for(int i=0;i<species.variants().size();i++){choice-=Math.max(0,species.variants().get(i).weight());if(choice<0){variant(i);break;}}
        if(reason==EntitySpawnReason.NATURAL||reason==EntitySpawnReason.CHUNK_GENERATION){
            var biome=l.getBiome(blockPosition());
            if(species.id().equals("grove_lizard"))variant(biome.is(net.minecraft.tags.BiomeTags.IS_JUNGLE)?2+random.nextInt(2):biome.is(net.minecraft.world.level.biome.Biomes.MUSHROOM_FIELDS)?4:random.nextInt(2));
            if(species.id().equals("grove_sporeling"))variant(biome.is(net.minecraft.tags.BiomeTags.IS_NETHER)?2+random.nextInt(4):random.nextInt(2));
        }
        return super.finalizeSpawn(l,difficulty,reason,group);
    }
    @Override public InteractionResult mobInteract(Player player,InteractionHand hand){
        ItemStack held=player.getItemInHand(hand);
        if(player instanceof ServerPlayer p&&!Authority.mayChange(p.level(),p,blockPosition()))return InteractionResult.FAIL;
        if(MenagerieItems.capture(this,player,hand))return InteractionResult.SUCCESS;
        var grove=GroveCreatures.interact(this,player,hand);if(grove.consumesAction())return grove;
        var companion=CompanionCreatures.interact(this,player,hand);if(companion.consumesAction())return companion;
        if(isFood(held)&&owner()==null&&(species.id().startsWith("grove_")||species.id().startsWith("companion_")||species.mount()||species.small()||Set.of("capybara","duck","tortoise","zebra").contains(species.source()))){
            if(player instanceof ServerPlayer p){RecoveredCreatureActions.play(this,"eat","chew","nibble");held.consume(1,p);if(random.nextInt(species.mount()?3:2)==0){tame(p.getUUID());p.level().broadcastEntityEvent(this,(byte)7);}else p.level().broadcastEntityEvent(this,(byte)6);}return InteractionResult.SUCCESS;
        }
        if(ownedBy(player)){
            if(held.getItem() instanceof DyeItem dye){
                for(int i=0;i<species.variants().size();i++)if(species.variants().get(i).name().equals("dye_"+held.getOrDefault(net.minecraft.core.component.DataComponents.DYE,DyeColor.WHITE).getName())){if(!level().isClientSide()){variant(i);held.consume(1,player);}return InteractionResult.SUCCESS;}
            }
            if(carriesCargo()&&held.is(Items.CHEST)&&!chested()){
                if(!level().isClientSide()){entityData.set(CHEST,true);held.consume(1,player);}return InteractionResult.SUCCESS;
            }
            if(species.mount()&&net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(held.getItem()).getPath().endsWith("horse_armor")){
                if(player instanceof ServerPlayer p){var previous=getItemBySlot(EquipmentSlot.BODY);setItemSlotAndDropWhenKilled(EquipmentSlot.BODY,held.copyWithCount(1));held.consume(1,p);if(!previous.isEmpty()&&!p.getInventory().add(previous))spawnAtLocation(p.level(),previous);getAttribute(Attributes.ARMOR).setBaseValue(switch(armor()){case "diamond"->11;case "golden"->7;case "iron"->5;default->3;});}return InteractionResult.SUCCESS;
            }
            if(carriesCargo()&&chested()&&player.isShiftKeyDown()&&held.is(Items.STICK)){
                if(player instanceof ServerPlayer p)p.openMenu(new SimpleMenuProvider((id,inv,who)->new net.minecraft.world.inventory.ChestMenu(net.minecraft.world.inventory.MenuType.GENERIC_9x3,id,inv,cargo,3){
                    @Override public boolean stillValid(Player who){return isAlive()&&ownedBy(who)&&who.distanceToSqr(MenagerieAnimal.this)<64&&who instanceof ServerPlayer server&&Authority.mayChange(server.level(),server,blockPosition());}
                },Component.literal(species.name()+" · Saddlebags")));return InteractionResult.SUCCESS;
            }
            if(isFood(held)&&getHealth()<getMaxHealth()){if(!level().isClientSide()){heal(5);RecoveredCreatureActions.play(this,"eat","chew","nibble");held.consume(1,player);}return InteractionResult.SUCCESS;}
            if(species.mount()&&held.is(Items.SADDLE)&&!saddled()){if(!level().isClientSide()){entityData.set(SADDLED,true);held.consume(1,player);}return InteractionResult.SUCCESS;}
            if(held.isEmpty()){
                if(player.isShiftKeyDown()){if(!level().isClientSide()){sit(!sitting());player.sendSystemMessage(Component.literal(sitting()?"Stay":"Follow"));}return InteractionResult.SUCCESS;}
                if(species.mount()&&saddled()&&!isBaby()){if(!level().isClientSide()){sit(false);player.startRiding(this);}return InteractionResult.SUCCESS;}
            }
        }else if(owner()!=null&&!isFood(held))return InteractionResult.PASS;
        boolean food=isFood(held);var result=super.mobInteract(player,hand);if(food&&result.consumesAction())RecoveredCreatureActions.play(this,"eat","chew","nibble");return result;
    }
    @Override public @Nullable LivingEntity getControllingPassenger(){return getFirstPassenger() instanceof Player p&&ownedBy(p)&&saddled()?p:null;}
    @Override protected Vec3 getRiddenInput(Player player,Vec3 input){
        float strafe=player.xxa*.5F,forward=player.zza;double vertical=0;
        if(species.flying()||species.aquatic())vertical=forward>0?-Math.sin(Math.toRadians(player.getXRot()))*.6:0;
        if(player instanceof ServerPlayer p?p.getLastClientInput().jump():player.isJumping()){
            if(species.flying()||species.aquatic())vertical=.7;else if(onGround())jumpFromGround();
        }
        return new Vec3(strafe,vertical,forward<=0?forward*.25:forward);
    }
    @Override protected void tickRidden(Player p,Vec3 input){super.tickRidden(p,input);setYRot(p.getYRot());yBodyRot=getYRot();yHeadRot=getYRot();setXRot(p.getXRot()*.35F);}
    @Override protected float getRiddenSpeed(Player p){return (float)getAttributeValue(Attributes.MOVEMENT_SPEED)*(species.flying()?1.4F:1.15F);}
    @Override protected Vec3 getPassengerAttachmentPoint(Entity passenger,EntityDimensions dimensions,float scale){return new Vec3(0,dimensions.height()*.7,0);}
    @Override public void travel(Vec3 input){
        if((species.flying()&&(isVehicle()||(BirdFlightGoal.applies(species)?!getNavigation().isDone()&&!sitting():!onGround())))||(species.aquatic()&&inHomeFluid())){
            setNoGravity(true);moveRelative(isVehicle()?getSpeed():.025F,input);move(MoverType.SELF,getDeltaMovement());setDeltaMovement(getDeltaMovement().scale(.9));
        }else{setNoGravity(false);super.travel(input);}
    }
    private boolean inHomeFluid(){return LavaSwimming.species(species)?LavaSwimming.fluid(species,level().getFluidState(blockPosition())):isInWater();}
    @Override public boolean canBreatheUnderwater(){return species!=null&&(species.aquatic()||species.amphibious());}
    @Override public boolean causeFallDamage(double distance,float multiplier,DamageSource source){return !species.flying()&&super.causeFallDamage(distance,multiplier,source);}
    @Override public boolean doHurtTarget(ServerLevel l,Entity target){
        if(target instanceof Player p&&FableCreatures.wards(this,p)){setTarget(null);return false;}
        if(owner()!=null){var p=Authority.present(l,owner());if(p==null||!(target instanceof LivingEntity living)||!io.github.jimbozoomer.jugcraft.weapons.TwoHanded.allowed(p,l,living))return false;}
        RecoveredCreatureActions.play(this,"attack","bite","maul","swipe","punch","sting","stomp");
        attackTicks=12;swing(InteractionHand.MAIN_HAND,net.minecraft.world.item.component.SwingAnimation.DEFAULT);if(species.clips().contains("animation.mount.attack")||species.clips().contains("attack"))triggerAnim("combat","strike");boolean hit=super.doHurtTarget(l,target);
        if(hit&&target instanceof LivingEntity living&&(species.source().contains("scorpion")||species.source().equals("snake")||species.source().equals("jellyfish")))living.addEffect(new MobEffectInstance(MobEffects.POISON,80,0),this);
        if(hit&&target instanceof LivingEntity living&&species.id().startsWith("fable_")){
            if(species.source().equals("hound")||species.source().endsWith("mojo"))living.addEffect(new MobEffectInstance(FableCreatures.ROOT,40,0),this);
            if(species.source().equals("scarecrow"))living.addEffect(new MobEffectInstance(FableCreatures.CONVULSION,60,0),this);
            if((species.source().equals("frogman")||species.source().equals("peacock_spider"))&&variant()>0)living.addEffect(new MobEffectInstance(MobEffects.POISON,80,variant()>1?1:0),this);
            if(species.source().equals("corpse_eater"))living.addEffect(new MobEffectInstance(MobEffects.HUNGER,100,0),this);
        }
        return hit;
    }
    @Override protected void customServerAiStep(ServerLevel l){
        super.customServerAiStep(l);if(attackTicks>0)attackTicks--;
        RecoveredCreatureActions.idle(this);
        FableCreatures.tick(this,l);if(harvestTicks>0)harvestTicks--;
        if(species.id().equals("fauna_pixie")&&owner()!=null&&readyToHarvest()){spawnAtLocation(l,new ItemStack(Menagerie.item("fauna_pixie_dust")));harvested();}
        if(flowerVariant>=0&&--flowerRegrowTicks<=0){variant(flowerVariant);flowerVariant=-1;}
        if(species.aquatic()&&!inHomeFluid()&&tickCount%20==0)hurtServer(l,damageSources().dryOut(),1);
        if(species.source().equals("firefly")&&tickCount%20==0)l.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,getX(),getY()+.2,getZ(),1,0,0,0,0);
        if(isBaby()||--produceTicks>0)return;produceTicks=6000+random.nextInt(6000);
        String product=switch(species.id()){case "wildlife_duck"->"wildlife_duck_egg";case "wildlife_deer"->"wildlife_antler";case "wildlife_firefly"->"wildlife_glow_goop";case "companion_jumping_spider"->"companion_silk";case "companion_otter"->"companion_clam";case "companion_weevil"->"companion_acorn";case "grove_yeti"->"grove_yeti_antler";case "grove_cindershell"->"grove_cindershell_shell_shard";default->"";};
        if(species.id().equals("mount_firebird"))product="mount_firebird_feather"+(variant()==0?"":Integer.toString(variant()+1));
        if(!product.isEmpty()&&l.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,getBoundingBox().inflate(4)).size()<12)spawnAtLocation(l,new ItemStack(Menagerie.item(product)));
    }
    private @Nullable SoundEvent voice(String action){
        if(species.id().startsWith("landscape_"))return Menagerie.sound("landscape.entity."+species.source()+"."+action);
        for(String collection:List.of("ecology","fauna","mutant","beast","companion","cavern","rotten"))if(species.id().startsWith(collection+"_")){
            for(String event:List.of(action,action.equals("ambient")?"idle":action.equals("death")?"die":action))
                for(String key:List.of(collection+"."+species.source()+"_"+event,collection+".entity."+species.source()+"."+event,collection+"_entity."+species.source()+"."+event)){
                    var sound=Menagerie.sound(key);if(sound!=null)return sound;
                }
            return null;
        }

        if(species.id().startsWith("grove_")){
            String voice=species.source();
            if(voice.equals("sporeling"))voice=variant()==5?"sporeling_warped":variant()>=2&&variant()<=4?"sporeling_nether":"sporeling_overworld";
            if(voice.equals("little_grebe")&&isBaby()&&action.equals("ambient"))voice="little_grebe_chick";
            return Menagerie.sound("grove_entity."+voice+"."+action);
        }
        if(species.id().startsWith("fable_")){String name=species.source().endsWith("mojo")?"mojo":species.source().endsWith("wisp")?"wisp":species.source();return Menagerie.sound("fable_"+name+"_"+(action.equals("ambient")?"idle":action));}
        if(!species.mount()){
            var voice=Menagerie.sound("wildlife_entity."+species.source()+"."+action);
            if(voice==null&&action.equals("ambient"))voice=Menagerie.sound("wildlife_entity."+species.source()+".idle");
            return voice;
        }
        String source=switch(species.source()){case "netherbat"->"bat";case "geckotoalizard"->"groundlizard";default->species.source();};
        String prefix="mount_"+source+"_"+action;
        var voices=Menagerie.SOUNDS.entrySet().stream().filter(e->e.getKey().startsWith(prefix)).map(Map.Entry::getValue).toList();
        return voices.isEmpty()?null:voices.get(random.nextInt(voices.size()));
    }
    @Override protected @Nullable SoundEvent getAmbientSound(){return voice("ambient");}
    @Override protected @Nullable SoundEvent getHurtSound(DamageSource d){var s=voice("hurt");return s==null?SoundEvents.GENERIC_HURT:s;}
    @Override protected @Nullable SoundEvent getDeathSound(){var s=voice("death");return s==null?SoundEvents.GENERIC_DEATH:s;}
    @Override protected void dropCustomDeathLoot(ServerLevel l,DamageSource d,boolean hit){super.dropCustomDeathLoot(l,d,hit);FableCreatures.dropPurse(this,l);if(saddled())spawnAtLocation(l,new ItemStack(Items.SADDLE));if(chested())spawnAtLocation(l,new ItemStack(Items.CHEST));for(int i=0;i<cargo.getContainerSize();i++){var stack=cargo.removeItemNoUpdate(i);if(!stack.isEmpty())spawnAtLocation(l,stack);}}
    @Override protected void addAdditionalSaveData(ValueOutput out){super.addAdditionalSaveData(out);out.putInt("MenagerieSchema",1);out.putInt("Variant",variant());out.putString("VariantName",species.variants().get(variant()).name());out.putString("Keeper",entityData.get(OWNER));out.putBoolean("Stay",sitting());out.putBoolean("Saddled",saddled());out.putInt("ProduceTicks",produceTicks);out.putInt("FlowerVariant",flowerVariant);out.putInt("FlowerRegrowTicks",flowerRegrowTicks);out.putInt("HarvestTicks",harvestTicks);out.putBoolean("Chested",chested());var items=NonNullList.withSize(27,ItemStack.EMPTY);for(int i=0;i<27;i++)items.set(i,cargo.getItem(i));ContainerHelper.saveAllItems(out,items);}
    @Override protected void readAdditionalSaveData(ValueInput in){super.readAdditionalSaveData(in);variant(in.getIntOr("Variant",0));String variantName=in.getStringOr("VariantName","");for(int i=0;i<species.variants().size();i++)if(species.variants().get(i).name().equals(variantName)){variant(i);break;}String owner=in.getStringOr("Keeper","");try{if(!owner.isEmpty())UUID.fromString(owner);}catch(IllegalArgumentException e){owner="";}entityData.set(OWNER,owner);sit(in.getBooleanOr("Stay",false));entityData.set(SADDLED,in.getBooleanOr("Saddled",false));produceTicks=Math.clamp(in.getIntOr("ProduceTicks",6000),1,12000);flowerVariant=Math.clamp(in.getIntOr("FlowerVariant",-1),-1,2);flowerRegrowTicks=Math.clamp(in.getIntOr("FlowerRegrowTicks",0),0,6000);harvestTicks=Math.clamp(in.getIntOr("HarvestTicks",0),0,6000);refreshArmor();entityData.set(CHEST,in.getBooleanOr("Chested",false));var items=NonNullList.withSize(27,ItemStack.EMPTY);ContainerHelper.loadAllItems(in,items);for(int i=0;i<27;i++)cargo.setItem(i,items.get(i));}
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers){
        controllers.add(new AnimationController<MenagerieAnimal>("movement",6,state->{
            String action=sitting()?"sit":species.aquatic()&&inHomeFluid()?"swim":species.flying()&&!onGround()?"fly":state.isMoving()?"walk":"idle";
            if(isBaby()&&species.clips().contains("baby_"+action))action="baby_"+action;
            if(!isBaby()&&species.id().equals("grove_cactem")&&variant()==1&&action.equals("walk"))action="cactem_elder_walk";
            if(species.id().startsWith("mount_")){
                String wanted="animation.mount."+action;
                if(!species.clips().contains(wanted))wanted="animation.mount."+(action.equals("swim")?"fly":action.equals("idle")?"neutral":"walk");
                if(!species.clips().contains(wanted))wanted=species.clips().getFirst();action=wanted;
            }
            if(!species.clips().contains(action))return com.geckolib.animation.object.PlayState.STOP;
            return state.setAndContinue(RawAnimation.begin().thenLoop(action));
        }));
        if(species.clips().contains("animation.mount.attack")||species.clips().contains("attack"))controllers.add(new AnimationController<MenagerieAnimal>("combat",2,state->com.geckolib.animation.object.PlayState.STOP).triggerableAnim("strike",RawAnimation.begin().thenPlay(species.clips().contains("attack")?"attack":"animation.mount.attack")));
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
}



