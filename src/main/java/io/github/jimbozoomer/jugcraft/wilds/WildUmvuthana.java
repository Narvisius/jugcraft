package io.github.jimbozoomer.jugcraft.wilds;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.*;
import com.geckolib.util.GeckoLibUtil;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.*;
import net.fabricmc.fabric.api.biome.v1.*;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.network.syncher.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.*;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.trading.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;

/** Original masked warriors, raptor packs, healers and owned mask companions. */
public final class WildUmvuthana extends Monster implements GeoEntity,Merchant {
    public static EntityType<WildUmvuthana> TYPE,RAPTOR,CRANE,FOLLOWER;
    public static final String[] MASKS={"fury","fear","rage","bliss","misery","faith"};
    public static final int IDLE=0,SLASH=1,COUNTER=2,BLOCK=3,DART=4,HEAL=5,TELEPORT=6,EMERGE=7,RETRACT=8,HURT=9,ALERT=10,ROAR=11;
    private static final EntityDataAccessor<Integer> MASK=SynchedEntityData.defineId(WildUmvuthana.class,EntityDataSerializers.INT),ACTION=SynchedEntityData.defineId(WildUmvuthana.class,EntityDataSerializers.INT),START=SynchedEntityData.defineId(WildUmvuthana.class,EntityDataSerializers.INT),WEAPON=SynchedEntityData.defineId(WildUmvuthana.class,EntityDataSerializers.INT);
    private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
    private UUID owner,leader,provoked;
    private static final EntityDataAccessor<Boolean> COMBAT=SynchedEntityData.defineId(WildUmvuthana.class,EntityDataSerializers.BOOLEAN);
    private ItemStack storedMask=ItemStack.EMPTY;
    private int age,cooldown=40,blocks,teleportCooldown,side=1;
    private boolean packCreated,returnPaid;
    private Vec3 destination;
    private Player customer;private MerchantOffers offers;private int offerTime;
    public WildUmvuthana(EntityType<? extends WildUmvuthana> type,Level level){super(type,level);xpReward=type==RAPTOR?8:5;}
    static void register(){TYPE=type("umvuthana",.8F,1.8F);RAPTOR=type("umvuthana_raptor",.9F,2);CRANE=type("umvuthana_crane",.9F,2);FOLLOWER=type("umvuthana_follower_player",.8F,1.8F);
        for(var t:List.of(TYPE,RAPTOR,CRANE,FOLLOWER))FabricDefaultAttributeRegistry.register(t,Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,t==RAPTOR?10:8).add(Attributes.ATTACK_DAMAGE,t==RAPTOR?6:3).add(Attributes.MOVEMENT_SPEED,.3).add(Attributes.FOLLOW_RANGE,32));
        SpawnPlacements.register(RAPTOR,SpawnPlacementTypes.ON_GROUND,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,(t,l,r,p,random)->l.getDifficulty()!=Difficulty.PEACEFUL&&p.getY()>=60&&WildSpawns.outsideSettlements(l,p,r,true)&&(l.getBlockState(p.below()).is(BlockTags.ANIMALS_SPAWNABLE_ON)||l.getBlockState(p.below()).is(BlockTags.SAND)));
        BiomeModifications.addSpawn(BiomeSelectors.tag(BiomeTags.IS_SAVANNA),MobCategory.MONSTER,RAPTOR,5,1,1);
    }
    private static EntityType<WildUmvuthana> type(String name,float w,float h){var key=ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id("wilds_"+name));return Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.<WildUmvuthana>of(WildUmvuthana::new,MobCategory.MONSTER).sized(w,h).clientTrackingRange(10).updateInterval(1).build(key));}
    @Override protected void registerGoals(){goalSelector.addGoal(1,new FloatGoal(this));goalSelector.addGoal(8,new WaterAvoidingRandomStrollGoal(this,.6){@Override public boolean canUse(){return action()==IDLE&&owner==null&&customer==null&&getTarget()==null&&super.canUse();}@Override public boolean canContinueToUse(){return action()==IDLE&&customer==null&&getTarget()==null&&super.canContinueToUse();}});}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(MASK,1);b.define(ACTION,IDLE);b.define(START,0);b.define(WEAPON,0);b.define(COMBAT,false);}
    public int mask(){return entityData.get(MASK);}public int action(){return entityData.get(ACTION);}public int weapon(){return entityData.get(WEAPON);}public boolean elite(){return mask()==0||mask()==5;}public UUID ownerId(){return owner;}
    public ServerPlayer playerOwner(){return level() instanceof ServerLevel l&&owner!=null?l.getServer().getPlayerList().getPlayer(owner):null;}
    public float actionTime(float partial){return level().getGameTime()-entityData.get(START)+partial;}
    public void mask(int mask){entityData.set(MASK,Math.clamp(mask,0,5));getAttribute(Attributes.MAX_HEALTH).setBaseValue(mask==0?10:8);getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(mask==0?6:3);entityData.set(WEAPON,mask==5?2:random.nextInt(3)==0?1:0);setHealth(getMaxHealth());}
    void bind(LivingEntity chief,int mask){mask(mask);leader=chief.getUUID();packCreated=true;setPersistenceRequired();begin(EMERGE);}
    void bindPlayer(ServerPlayer player,int mask,ItemStack original){bind(player,mask);owner=player.getUUID();storedMask=original.copyWithCount(1);if(original.isDamageableItem())setHealth(Math.max(.5F,getMaxHealth()*(1-original.getDamageValue()/(float)original.getMaxDamage())));if(original.has(net.minecraft.core.component.DataComponents.CUSTOM_NAME))setCustomName(original.getHoverName());}
    private void begin(int action){entityData.set(ACTION,action);entityData.set(START,(int)level().getGameTime());age=0;navigation.stop();moveControl.strafe(0,0);if(action==HEAL)playSound(OwnerWilds.sound("umvuthana.healstart1"),2,1);if(action==TELEPORT)playSound(OwnerWilds.sound("umvuthana.teleport1"),2,1);if(action==ALERT)playSound(OwnerWilds.sound("umvuthana.alert"),1,1);}
    @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor l,DifficultyInstance d,EntitySpawnReason reason,SpawnGroupData group){var result=super.finalizeSpawn(l,d,reason,group);mask(getType()==RAPTOR?0:getType()==CRANE?5:1+random.nextInt(4));if(getType()==RAPTOR&&!packCreated){packCreated=true;for(int i=0,n=3+random.nextInt(2);i<n;i++){var child=TYPE.create(level(),EntitySpawnReason.MOB_SUMMONED);if(child==null)continue;child.bind(this,1+random.nextInt(4));child.setPos(position().add(Math.cos(i*Math.PI*2/n)*1.5,0,Math.sin(i*Math.PI*2/n)*1.5));if(l.getLevel().noCollision(child))l.addFreshEntity(child);}}return result;}
    private LivingEntity chief(ServerLevel l){if(owner!=null)return playerOwner();var e=leader==null?null:l.getEntity(leader);return e instanceof LivingEntity living?living:null;}
    private boolean enemy(LivingEntity e){if(!WildSolar.canHarm(this,e))return false;if(owner!=null){var p=playerOwner();return p!=null&&(p.getLastHurtMob()==e||p.getLastHurtByMob()==e||e instanceof Mob m&&m.getTarget()==p)&&p.level()==level();}return e instanceof Player p&&WildBluff.victim(p)&&(!WildSunEquipment.masked(p)||p.getUUID().equals(provoked))||e instanceof WildElokosa;}
    @Override public void tick(){super.tick();if(!(level() instanceof ServerLevel l)||!isAlive())return;if(cooldown>0)cooldown--;if(teleportCooldown>0)teleportCooldown--;var chief=chief(l);entityData.set(COMBAT,getTarget()!=null);
        if(customer!=null){if(!stillValid(customer)){if(customer instanceof ServerPlayer p)p.closeContainer();customer=null;}else{navigation.stop();getLookControl().setLookAt(customer,30,30);return;}}
        if(trader()&&--offerTime<=0){offers=null;offerTime=6000+random.nextInt(18001);}
        if(owner==null&&leader==null&&getTarget()==null&&hasHome()&&!isWithinHome()&&tickCount%20==0)navigation.moveTo(getHomePosition().getX()+.5,getHomePosition().getY(),getHomePosition().getZ()+.5,.6);
        if(owner!=null&&(chief==null||chief.level()!=l)){navigation.stop();setTarget(null);return;}
        if(owner!=null&&action()!=RETRACT&&chief instanceof ServerPlayer p&&(!p.isAlive()||!p.getItemBySlot(EquipmentSlot.HEAD).is(OwnerWilds.item("sol_visage"))))begin(RETRACT);
        if(action()!=IDLE){age++;var target=getTarget();if(target!=null)getLookControl().setLookAt(target,30,30);
            if(action()==SLASH&&age==5)setDeltaMovement(getDeltaMovement().add(getLookAngle().multiply(.5,0,.5)));
            if((action()==SLASH&&age==13)||(action()==COUNTER&&age==7))strike(l,target,action()==COUNTER?3:1);
            if(action()==DART&&age==12&&target!=null&&hasLineOfSight(target)&&enemy(target)){var dart=new WildWeapons.Dart(l,this,new ItemStack(OwnerWilds.item("dart")),new ItemStack(OwnerWilds.item("blowgun")));var v=target.getEyePosition().subtract(dart.position());dart.shoot(v.x,v.y+v.horizontalDistance()*.1,v.z,1.6F,3);dart.pickup=net.minecraft.world.entity.projectile.arrow.AbstractArrow.Pickup.DISALLOWED;l.addFreshEntity(dart);playSound(OwnerWilds.sound("umvuthana.blowdart"),1,1);}
            if(action()==HEAL){if(chief==null||!chief.isAlive()||distanceToSqr(chief)>121||!hasLineOfSight(chief)){begin(IDLE);return;}getLookControl().setLookAt(chief,30,30);if(age>6){chief.addEffect(new net.minecraft.world.effect.MobEffectInstance(WildSunEquipment.SUNBLOCK,20,0,true,false),this);if(age%20==0)chief.heal(chief instanceof WildUmvuthi?.15F:.3F);}if(age>200){begin(IDLE);cooldown=20;}return;}
            if(action()==TELEPORT&&age==16&&destination!=null&&safe(destination)){setPos(destination);destination=null;playSound(OwnerWilds.sound("umvuthana.teleport2"),2,1);}
            if(action()==RETRACT&&age>=11){returnMask(l);return;}
            int duration=switch(action()){case SLASH->22;case COUNTER->18;case BLOCK->10;case DART->24;case EMERGE->21;case RETRACT->11;case TELEPORT->30;case ALERT->15;case ROAR->35;default->12;};if(age>=duration)begin(IDLE);return;
        }
        if(mask()==5&&chief!=null&&chief.isAlive()){
            if(distanceToSqr(chief)>100||!hasLineOfSight(chief)){if(tickCount%10==0)navigation.moveTo(chief,.9);}else if(chief.getHealth()<chief.getMaxHealth()||chief instanceof Mob m&&m.getTarget()!=null){begin(HEAL);return;}
            if(teleportCooldown==0&&tickCount%20==0&&l.getNearestPlayer(this,4)!=null&&owner==null){var p=l.getNearestPlayer(this,4);var away=position().subtract(p.position()).normalize().scale(8);tryTeleport(position().add(away));}return;
        }
        if(tickCount%10==0){var target=getTarget();if(target!=null&&(!enemy(target)||distanceToSqr(target)>1600))setTarget(null);if(getTarget()==null){var possible=new ArrayList<LivingEntity>();l.getEntities(net.minecraft.world.level.entity.EntityTypeTest.forClass(LivingEntity.class),getBoundingBox().inflate(24),e->enemy(e)&&hasLineOfSight(e),possible,32);possible.stream().min(Comparator.comparingDouble(this::distanceToSqr)).ifPresent(e->{setTarget(e);begin(ALERT);});}}
        var target=getTarget();if(target==null){if(chief!=null&&distanceToSqr(chief)>16&&tickCount%10==0)navigation.moveTo(chief,1);return;}getLookControl().setLookAt(target,30,30);double distance=distanceToSqr(target);
        if(cooldown==0&&hasLineOfSight(target)&&distance<(weapon()==1?144:14.1)){cooldown=40+random.nextInt(40);begin(weapon()==1?DART:SLASH);return;}
        if(tickCount%100==0&&random.nextBoolean())side=-side;double range=weapon()==1?8:5;if(distance>(range+2)*(range+2)){if(tickCount%10==0)navigation.moveTo(target,.9);}else if(cooldown<10&&weapon()!=1){navigation.moveTo(target,1.2);}else{navigation.stop();moveControl.strafe(distance<range*range?-.25F:.1F,side*.5F);}
    }
    private boolean safe(Vec3 pos){var p=BlockPos.containing(pos);return level().hasChunkAt(p)&&level().getWorldBorder().isWithinBounds(p)&&level().noCollision(this,getBoundingBox().move(pos.subtract(position())))&&!level().getBlockState(p.below()).isAir();}
    private void tryTeleport(Vec3 at){for(int y=2;y>=-3;y--){var pos=at.add(0,y,0);if(safe(pos)){destination=pos;teleportCooldown=100;begin(TELEPORT);return;}}}
    private void strike(ServerLevel l,LivingEntity e,float mul){if(e==null||!enemy(e)||!hasLineOfSight(e)||distanceToSqr(e)>9)return;if(e.hurtServer(l,damageSources().mobAttack(this),(float)getAttributeValue(Attributes.ATTACK_DAMAGE)*mul)){e.knockback(.4, getX()-e.getX(),getZ()-e.getZ(),damageSources().mobAttack(this),(float)getAttributeValue(Attributes.ATTACK_DAMAGE)*mul);playSound(OwnerWilds.sound("umvuthana.attack1"),1,1);}}
    @Override public boolean hurtServer(ServerLevel l,DamageSource source,float amount){
        if(source.getEntity() instanceof LivingEntity e&&mask()==0&&!source.is(DamageTypeTags.BYPASSES_SHIELD)&&(action()==IDLE||action()==HURT||action()==BLOCK)&&getLookAngle().dot(e.position().subtract(position()).normalize())>-.342){blocks++;begin(blocks>=5?COUNTER:BLOCK);if(blocks>=5)blocks=0;playSound(OwnerWilds.sound("wroughtnaut.undamaged"),.4F,2);return false;}
        boolean hit=super.hurtServer(l,source,amount);if(hit&&isAlive()){if(source.getEntity() instanceof LivingEntity e&&WildSolar.canHarm(this,e)){provoked=e.getUUID();setTarget(e);}begin(HURT);if(mask()==5&&teleportCooldown==0)tryTeleport(position().add(random.nextInt(13)-6,0,random.nextInt(13)-6));}return hit;
    }
    @Override protected InteractionResult mobInteract(Player p,InteractionHand hand){if(owner!=null&&owner.equals(p.getUUID())&&p.isShiftKeyDown()){if(!level().isClientSide())begin(RETRACT);return InteractionResult.SUCCESS;}if(trader()&&p instanceof ServerPlayer player&&stillValid(p)){if(customer!=null&&customer!=p)return InteractionResult.FAIL;setTradingPlayer(p);openTradingScreen(p,getDisplayName(),1);return InteractionResult.SUCCESS;}return super.mobInteract(p,hand);}
    private boolean trader(){return getType()==TYPE&&owner==null&&leader==null;}
    @Override public void setTradingPlayer(Player p){customer=p;}@Override public Player getTradingPlayer(){return customer;}
    @Override public MerchantOffers getOffers(){if(offers==null){offers=WildGroveTrades.choose(random);if(offerTime<=0)offerTime=6000+random.nextInt(18001);}return offers;}
    @Override public void overrideOffers(MerchantOffers value){offers=value;}
    @Override public void notifyTrade(MerchantOffer offer){offer.increaseUses();playSound(getNotifyTradeSound(),1,1);}
    @Override public void notifyTradeUpdated(ItemStack result){}@Override public int getVillagerXp(){return 0;}@Override public void overrideXp(int xp){}@Override public boolean showProgressBar(){return false;}
    @Override public SoundEvent getNotifyTradeSound(){return OwnerWilds.sound("umvuthana.idle1");}@Override public boolean isClientSide(){return level().isClientSide();}
    @Override public boolean stillValid(Player p){return trader()&&isAlive()&&p.isAlive()&&!p.isSpectator()&&p.level()==level()&&distanceToSqr(p)<=64&&hasLineOfSight(p)&&getTarget()==null&&WildSunEquipment.masked(p)&&!p.getUUID().equals(provoked);}
    private void returnMask(ServerLevel l){if(returnPaid){discard();return;}returnPaid=true;if(!storedMask.isEmpty()){var stack=storedMask.copy();if(stack.isDamageableItem())stack.setDamageValue(Math.min(stack.getMaxDamage()-1,(int)Math.ceil((1-getHealth()/getMaxHealth())*stack.getMaxDamage())));spawnAtLocation(l,stack);}WildSunEquipment.releaseFollower(l,owner,getUUID());discard();}
    @Override protected void dropFromLootTable(ServerLevel l,DamageSource source,boolean recent){if(owner==null)dropFromLootTable(l,source,recent,ResourceKey.create(Registries.LOOT_TABLE,Jugcraft.id("entities/wilds_umvuthana_"+MASKS[mask()])));}
    @Override protected void dropCustomDeathLoot(ServerLevel l,DamageSource source,boolean recent){super.dropCustomDeathLoot(l,source,recent);if(owner!=null)WildSunEquipment.releaseFollower(l,owner,getUUID());}
    @Override public void remove(RemovalReason reason){if(level() instanceof ServerLevel l&&(reason==RemovalReason.KILLED||reason==RemovalReason.DISCARDED))WildSunEquipment.releaseFollower(l,owner,getUUID());super.remove(reason);}
    @Override public void checkDespawn(){if(owner==null&&leader==null)super.checkDespawn();}
    @Override protected void tickDeath(){navigation.stop();if(++deathTime>=70&&!level().isClientSide())remove(RemovalReason.KILLED);}
    @Override protected SoundEvent getHurtSound(DamageSource s){return OwnerWilds.sound("umvuthana.hurt");}@Override protected SoundEvent getDeathSound(){return OwnerWilds.sound("umvuthana.die");}@Override protected SoundEvent getAmbientSound(){return OwnerWilds.sound("umvuthana.idle1");}
    public String clip(){if(!isAlive())return "die";return switch(action()){case SLASH->"attack_slash_right";case COUNTER->"block_counter";case BLOCK->"block";case HEAL->actionTime(0)<6?"heal_start":"heal_loop";case TELEPORT->actionTime(0)<7?"teleport_start":actionTime(0)<17?"teleport_loop":"teleport_end";case EMERGE->"emerge";case RETRACT->"retract";case HURT->"hurt_right_aggressive";case ALERT->"alert";case ROAR->"roar";default->entityData.get(COMBAT)?"idle_aggressive":"idle_neutral";};}
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar c){c.add(new AnimationController<WildUmvuthana>("body",2,s->s.setAndContinue(action()==IDLE||action()==HEAL?RawAnimation.begin().thenLoop(clip()):RawAnimation.begin().thenPlayAndHold(clip()))));c.add(new AnimationController<WildUmvuthana>("mask",0,s->s.setAndContinue(RawAnimation.begin().thenLoop("mask_twitch"))));}
    @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
    @Override protected void addAdditionalSaveData(ValueOutput out){super.addAdditionalSaveData(out);if(offers!=null)out.store("grove_offers",MerchantOffers.CODEC,offers);out.putInt("offer_time",offerTime);if(provoked!=null)out.store("provoked",UUIDUtil.CODEC,provoked);out.putInt("mask",mask());out.putInt("weapon",weapon());out.putBoolean("pack_created",packCreated);if(owner!=null)out.store("owner",UUIDUtil.CODEC,owner);if(leader!=null)out.store("leader",UUIDUtil.CODEC,leader);if(!storedMask.isEmpty())out.store("mask_item",ItemStack.CODEC,storedMask);out.putBoolean("return_paid",returnPaid);}
    @Override protected void readAdditionalSaveData(ValueInput in){super.readAdditionalSaveData(in);customer=null;offers=in.read("grove_offers",MerchantOffers.CODEC).orElse(null);offerTime=Math.clamp(in.getIntOr("offer_time",0),0,24000);provoked=in.read("provoked",UUIDUtil.CODEC).orElse(null);float hp=getHealth();mask(in.getIntOr("mask",1));setHealth(Math.min(hp,getMaxHealth()));entityData.set(WEAPON,Math.clamp(in.getIntOr("weapon",0),0,2));packCreated=in.getBooleanOr("pack_created",true);owner=in.read("owner",UUIDUtil.CODEC).orElse(null);leader=in.read("leader",UUIDUtil.CODEC).orElse(null);storedMask=in.read("mask_item",ItemStack.CODEC).orElse(ItemStack.EMPTY);returnPaid=in.getBooleanOr("return_paid",false);}
}
