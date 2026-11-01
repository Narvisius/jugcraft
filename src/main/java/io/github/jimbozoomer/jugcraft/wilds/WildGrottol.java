package io.github.jimbozoomer.jugcraft.wilds;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.weapons.TwoHanded;
import java.util.*;
import net.fabricmc.fabric.api.biome.v1.*;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.core.particles.*;
import net.minecraft.network.syncher.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.tags.*;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.Vec3;

/** Original pickaxe/silk-touch rewards, timid escape, digging and empty-minecart escape. */
public final class WildGrottol extends PathfinderMob {
    private final WildRailTune tune=new WildRailTune();
    public static java.util.function.Consumer<WildGrottol> playTune=e->{};
    @Override public void handleEntityEvent(byte event){if(event==61)playTune.accept(this);else super.handleEntityEvent(event);}
    public static EntityType<WildGrottol> TYPE;
    private static final EntityDataAccessor<Boolean> DEEPSLATE=SynchedEntityData.defineId(WildGrottol.class,EntityDataSerializers.BOOLEAN),BLACKPINK=SynchedEntityData.defineId(WildGrottol.class,EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> ACTION=SynchedEntityData.defineId(WildGrottol.class,EntityDataSerializers.INT),START=SynchedEntityData.defineId(WildGrottol.class,EntityDataSerializers.INT);
    private int fleeing,calm,actionAge,diamonds;
    private boolean released;
    public WildGrottol(EntityType<? extends WildGrottol> type,Level l){super(type,l);xpReward=15;}
    static void register(){var key=ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id("wilds_grottol"));TYPE=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.<WildGrottol>of(WildGrottol::new,MobCategory.MONSTER).sized(.9F,1.2F).clientTrackingRange(8).updateInterval(1).build(key));FabricDefaultAttributeRegistry.register(TYPE,Mob.createMobAttributes().add(Attributes.MAX_HEALTH,20).add(Attributes.MOVEMENT_SPEED,.5).add(Attributes.KNOCKBACK_RESISTANCE,1).add(Attributes.STEP_HEIGHT,1.15));SpawnPlacements.register(TYPE,SpawnPlacementTypes.ON_GROUND,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,(t,l,reason,p,r)->p.getY()<=16&&net.minecraft.world.entity.monster.Monster.isDarkEnoughToSpawn(l,p,r)&&l.getBlockState(p.below()).is(BlockTags.BASE_STONE_OVERWORLD)&&!l.canSeeSky(p));BiomeModifications.addSpawn(BiomeSelectors.foundInOverworld().and(BiomeSelectors.includeByKey(net.minecraft.world.level.biome.Biomes.MUSHROOM_FIELDS).negate()),MobCategory.MONSTER,TYPE,2,1,1);}
    @Override protected void registerGoals(){goalSelector.addGoal(3,new FloatGoal(this));goalSelector.addGoal(4,new RandomStrollGoal(this,.3));goalSelector.addGoal(8,new RandomLookAroundGoal(this));goalSelector.addGoal(8,new LookAtPlayerGoal(this,Player.class,8));}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(DEEPSLATE,false);b.define(BLACKPINK,false);b.define(ACTION,0);b.define(START,0);}
    public boolean deepslate(){return entityData.get(DEEPSLATE);}public boolean blackpink(){return entityData.get(BLACKPINK);}public int action(){return entityData.get(ACTION);}public float actionTime(float partial){return level().getGameTime()-entityData.get(START)+partial;}
    private void begin(int n){entityData.set(ACTION,n);entityData.set(START,(int)level().getGameTime());actionAge=0;getNavigation().stop();}
    @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor l,DifficultyInstance d,EntitySpawnReason reason,SpawnGroupData group){if(!released)entityData.set(DEEPSLATE,getY()<8);return super.finalizeSpawn(l,d,reason,group);}
    @Override public void tick(){super.tick();if(!(level() instanceof ServerLevel l)||!isAlive())return;
        if(action()!=0){getNavigation().stop();if(action()==2&&++actionAge>=20){discard();return;}if(action()==1&&++actionAge>=47)entityData.set(ACTION,0);if(action()==2&&actionAge%4==3){playSound(OwnerWilds.sound("grottol.burrow"),1,.8F+random.nextFloat()*.4F);l.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,l.getBlockState(blockPosition().below())),getX(),getY()+.05,getZ(),8,.25,.025,.25,.1);}return;}
        if(isPassenger()){if(getVehicle() instanceof Minecart cart){boolean rail=l.getBlockState(cart.blockPosition()).is(BlockTags.RAILS)||l.getBlockState(cart.blockPosition().below()).is(BlockTags.RAILS);if(rail){if(tune.visit(cart)){entityData.set(BLACKPINK,true);l.broadcastEntityEvent(this,(byte)61);}var motion=cart.getDeltaMovement();var direction=motion.horizontalDistanceSqr()<.000001?cart.getLookAngle():motion;cart.setDeltaMovement(direction.normalize().scale(2.7));cart.needsSync=true;}else if(cart.onGround())stopRiding();}return;}
        tune.clear();
        Player threat=l.getNearestPlayer(this,16);boolean scared=threat!=null&&!threat.isCreative()&&!threat.isSpectator()&&hasLineOfSight(threat)&&!threat.isShiftKeyDown();
        if(scared){fleeing++;calm=0;if(tickCount%10==0){var carts=new ArrayList<Minecart>();l.getEntities(net.minecraft.world.level.entity.EntityTypeTest.forClass(Minecart.class),getBoundingBox().inflate(8,4,8),e->e.isAlive()&&!e.isVehicle()&&l.getBlockState(e.blockPosition()).is(BlockTags.RAILS),carts,8);var cart=carts.stream().min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);if(cart!=null){if(distanceToSqr(cart)<=2.1025)startRiding(cart,true,false);else getNavigation().moveTo(cart,.5);}else{Vec3 away=position().subtract(threat.position()).normalize();var target=position().add(away.scale(8));getNavigation().moveTo(target.x,target.y,target.z,.7);}}}
        else if(++calm>=50)fleeing=0;
        if(fleeing>=55&&!isPersistenceRequired()&&l.getBlockState(blockPosition().below()).is(TagKey.create(Registries.BLOCK,Jugcraft.id("wilds_can_grottol_dig"))))begin(2);
        else if(!scared&&getNavigation().isDone()&&random.nextInt(180)==0)begin(1);
        if(tickCount%6==0&&getDeltaMovement().horizontalDistanceSqr()>.0025)playSound(OwnerWilds.sound("grottol.step"),1,1.8F);
    }
    private static int enchant(ServerPlayer p,ResourceKey<Enchantment> key){return p.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(key).map(h->EnchantmentHelper.getItemEnchantmentLevel(h,p.getMainHandItem())).orElse(0);}
    @Override public boolean hurtServer(ServerLevel l,DamageSource source,float amount){
        if(!isAlive())return false;
        if(source.getEntity() instanceof ServerPlayer p&&!source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)){
            if(!TwoHanded.allowed(p,l,this))return false;
            if(enchant(p,Enchantments.SILK_TOUCH)>0){var captured=new ItemStack(OwnerWilds.item("captured_grottol"));var data=new net.minecraft.nbt.CompoundTag();data.putBoolean("deepslate",deepslate());data.putBoolean("blackpink",blackpink());data.putFloat("health",getHealth());captured.set(DataComponents.CUSTOM_DATA,CustomData.of(data));if(hasCustomName())captured.set(DataComponents.CUSTOM_NAME,getCustomName());var drop=spawnAtLocation(l,captured);if(drop==null)return false;discard();return true;}
            if(p.getMainHandItem().is(ItemTags.PICKAXES)||p.getMainHandItem().is(TagKey.create(Registries.ITEM,Jugcraft.id("wilds_can_hit_grottol")))){diamonds=enchant(p,Enchantments.FORTUNE)>0?2:1;return super.hurtServer(l,source,getHealth());}
            if(tickCount%5==0)playSound(OwnerWilds.sound("grottol.undamaged"),.4F,2);return false;
        }
        if(source.getEntity() instanceof Mob)return false;return super.hurtServer(l,source,amount);
    }
    @Override protected void dropCustomDeathLoot(ServerLevel l,DamageSource source,boolean recentlyHit){super.dropCustomDeathLoot(l,source,recentlyHit);if(diamonds>0)spawnAtLocation(l,new ItemStack(Items.DIAMOND,diamonds));diamonds=0;}
    @Override protected void tickDeath(){deathTime++;if(deathTime==1&&!level().isClientSide())playSound(OwnerWilds.sound("grottol.die"),1,1.3F);if(deathTime>=73&&!level().isClientSide())remove(RemovalReason.KILLED);}
    @Override protected void addAdditionalSaveData(ValueOutput out){super.addAdditionalSaveData(out);out.putBoolean("deepslate",deepslate());out.putBoolean("blackpink",blackpink());out.putBoolean("released",released);}
    @Override protected void readAdditionalSaveData(ValueInput in){super.readAdditionalSaveData(in);entityData.set(DEEPSLATE,in.getBooleanOr("deepslate",false));entityData.set(BLACKPINK,in.getBooleanOr("blackpink",false));released=in.getBooleanOr("released",false);}
    public static final class Captured extends Item {
        public Captured(Properties p){super(p.stacksTo(1));}
        @Override public InteractionResult useOn(UseOnContext c){if(!(c.getPlayer() instanceof ServerPlayer p))return InteractionResult.SUCCESS;var at=c.getClickedPos().relative(c.getClickedFace());var l=p.level();if(c.getClickedFace()==Direction.DOWN||!l.hasChunkAt(at)||!Authority.mayChange(l,p,at)||!Authority.mayChange(l,p,at.above()))return InteractionResult.FAIL;var mob=TYPE.create(l,EntitySpawnReason.MOB_SUMMONED);if(mob==null)return InteractionResult.FAIL;mob.snapTo(at.getX()+.5,at.getY(),at.getZ()+.5,p.getYRot()+180,0);var data=c.getItemInHand().getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();mob.entityData.set(DEEPSLATE,data.getBooleanOr("deepslate",false));mob.entityData.set(BLACKPINK,data.getBooleanOr("blackpink",false));mob.setHealth(Math.clamp(data.getFloatOr("health",20),1,20));mob.released=true;mob.setPersistenceRequired();if(c.getItemInHand().has(DataComponents.CUSTOM_NAME))mob.setCustomName(c.getItemInHand().get(DataComponents.CUSTOM_NAME));if(!l.noCollision(mob)||!l.getWorldBorder().isWithinBounds(mob.getBoundingBox())||!l.addFreshEntity(mob))return InteractionResult.FAIL;c.getItemInHand().consume(1,p);return InteractionResult.SUCCESS;}
    }
}
