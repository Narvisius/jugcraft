package io.github.jimbozoomer.jugcraft.wilds;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.weapons.TwoHanded;
import java.util.*;
import net.fabricmc.fabric.api.biome.v1.*;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.*;
import net.minecraft.core.particles.*;
import net.minecraft.core.registries.*;
import net.minecraft.network.syncher.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.tags.*;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.*;

/** Stationary carnivorous plant and the source's three feeding milestones to adulthood. */
public final class WildFoliaath extends Monster {
    public static EntityType<WildFoliaath> ADULT,BABY;
    private static final EntityDataAccessor<Integer> GROWTH=SynchedEntityData.defineId(WildFoliaath.class,EntityDataSerializers.INT),ACTIVE=SynchedEntityData.defineId(WildFoliaath.class,EntityDataSerializers.INT),ACTION=SynchedEntityData.defineId(WildFoliaath.class,EntityDataSerializers.INT),START=SynchedEntityData.defineId(WildFoliaath.class,EntityDataSerializers.INT);
    private UUID planter;
    private int actionAge,cooldown;
    public WildFoliaath(EntityType<? extends WildFoliaath> t,Level l){super(t,l);xpReward=5;}
    static void register(){ADULT=type("foliaath",.5F,2.5F);BABY=type("baby_foliaath",.4F,.4F);for(var t:List.of(ADULT,BABY))FabricDefaultAttributeRegistry.register(t,Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,t==BABY?1:10).add(Attributes.MOVEMENT_SPEED,0).add(Attributes.KNOCKBACK_RESISTANCE,1).add(Attributes.ATTACK_DAMAGE,8).add(Attributes.FOLLOW_RANGE,11));SpawnPlacements.register(ADULT,WildSpawns.SOLID_FLOOR,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,(t,l,reason,p,r)->p.getY()>=60&&Monster.isDarkEnoughToSpawn(l,p,r)&&WildSpawns.outsideSettlements(l,p,reason,false)&&(l.getBlockState(p.below()).is(BlockTags.ANIMALS_SPAWNABLE_ON)||l.getBlockState(p.below()).is(BlockTags.LEAVES)||l.getBlockState(p.below()).is(BlockTags.LOGS)));BiomeModifications.addSpawn(BiomeSelectors.tag(BiomeTags.IS_JUNGLE),MobCategory.MONSTER,ADULT,70,1,4);}
    private static EntityType<WildFoliaath> type(String name,float w,float h){var key=ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id("wilds_"+name));return Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.<WildFoliaath>of(WildFoliaath::new,MobCategory.MONSTER).sized(w,h).clientTrackingRange(8).build(key));}
    @Override protected void registerGoals(){}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(GROWTH,0);b.define(ACTIVE,0);b.define(ACTION,0);b.define(START,0);}
    public boolean seedling(){return getType()==BABY;}
    public int growth(){return entityData.get(GROWTH);}
    public int activation(){return entityData.get(ACTIVE);}
    public int action(){return entityData.get(ACTION);}
    public float actionTime(float partial){return level().getGameTime()-entityData.get(START)+partial;}
    public boolean hungry(){int n=growth();return seedling()&&(n==600||n==1200||n==1800);}
    private void begin(int action){entityData.set(ACTION,action);entityData.set(START,(int)level().getGameTime());actionAge=0;}
    private boolean mayBite(LivingEntity e){if(!e.isAlive()||e instanceof WildFoliaath||e.isInvulnerable()||!(e instanceof Player||e instanceof Animal)||e instanceof Player p&&(p.isCreative()||p.isSpectator()))return false;if(planter==null)return true;var p=Authority.present((ServerLevel)level(),planter);return p!=null&&e!=p&&TwoHanded.allowed(p,p.level(),e);}
    @Override public void tick(){
        super.tick();setDeltaMovement(0,getDeltaMovement().y,0);setYRot(0);yBodyRot=0;if(!(level() instanceof ServerLevel l)||!isAlive())return;
        if(cooldown>0)cooldown--;if(action()!=0&&++actionAge>=(action()==1?14:action()==2?20:10))entityData.set(ACTION,0);
        if(seedling()){
            if(!hungry()&&tickCount%20==0)entityData.set(GROWTH,Math.min(2400,growth()+1));
            if(growth()>=2400){var adult=convertTo(ADULT,ConversionParams.single(this,true,true),EntitySpawnReason.CONVERSION,e->{e.planter=planter;e.setPersistenceRequired();});return;}
            entityData.set(ACTIVE,hungry()?Math.min(30,activation()+1):Math.max(0,activation()-1));
            if(hungry()&&action()==0&&tickCount%5==0&&mayFeed(l)){var drops=new ArrayList<ItemEntity>();l.getEntities(net.minecraft.world.level.entity.EntityTypeTest.forClass(ItemEntity.class),getBoundingBox().inflate(.4,.2,.4),e->e.isAlive()&&e.getItem().is(ItemTags.MEAT),drops,8);for(var drop:drops){UUID reserved=((io.github.jimbozoomer.jugcraft.mixin.AlchemicalItemAccess)drop).jugcraft$pickupTarget();if(reserved!=null&&!reserved.equals(planter))continue;var owner=drop.getOwner();if(planter!=null&&owner!=null&&!owner.getUUID().equals(planter))continue;eat(drop.getItem());if(drop.getItem().isEmpty())drop.discard();break;}}
            return;
        }
        if(tickCount%10==0){var candidates=new ArrayList<LivingEntity>();l.getEntities(net.minecraft.world.level.entity.EntityTypeTest.forClass(LivingEntity.class),getBoundingBox().inflate(10.5,2,10.5),this::mayBite,candidates,32);setTarget(candidates.stream().filter(this::hasLineOfSight).min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null));}
        var target=getTarget();boolean awake=target!=null&&mayBite(target)&&distanceToSqr(target)<=110.25&&hasLineOfSight(target);int before=activation();entityData.set(ACTIVE,Math.clamp(before+(awake?1:-1),0,30));if(before==0&&awake)playSound(OwnerWilds.sound("foliaath.emerge"),1,1);
        if(awake){getLookControl().setLookAt(target,90,90);if(activation()>=24&&distanceToSqr(target)<=16&&action()==0&&cooldown==0){begin(1);cooldown=24;}}
        if(action()==1&&actionAge==3&&target!=null&&mayBite(target)&&distanceToSqr(target)<=16&&hasLineOfSight(target)){target.hurtServer(l,l.damageSources().mobAttack(this),8);playSound(OwnerWilds.sound("foliaath.bite1"),1,1);}
    }
    private boolean mayFeed(ServerLevel l){if(planter==null)return true;var keeper=Authority.present(l,planter);return keeper!=null&&Authority.mayChange(l,keeper,blockPosition());}
    private void eat(ItemStack food){if(!(level() instanceof ServerLevel l)||!hungry())return;var particle=food.copyWithCount(1);food.shrink(1);entityData.set(GROWTH,growth()+1);begin(2);playSound(OwnerWilds.sound("foliaath.baby_eat"),.5F,1.2F);l.sendParticles(new ItemParticleOption(ParticleTypes.ITEM,net.minecraft.world.item.ItemStackTemplate.fromNonEmptyStack(particle)),getX(),getY()+.2,getZ(),15,.2,.1,.2,.05);}
    @Override protected InteractionResult mobInteract(Player p,InteractionHand hand){if(hungry()&&p.getItemInHand(hand).is(ItemTags.MEAT)){if(p instanceof ServerPlayer actor){if(!Authority.mayChange(actor.level(),actor,blockPosition()))return InteractionResult.FAIL;if(actor.hasInfiniteMaterials())eat(p.getItemInHand(hand).copy());else eat(p.getItemInHand(hand));}return InteractionResult.SUCCESS;}return super.mobInteract(p,hand);}
    @Override public boolean hurtServer(ServerLevel l,net.minecraft.world.damagesource.DamageSource source,float amount){if(!seedling()&&activation()<24&&!source.is(DamageTypeTags.BYPASSES_INVULNERABILITY))return false;boolean hit=super.hurtServer(l,source,amount);if(hit&&isAlive()&&!seedling())begin(3);return hit;}
    @Override public void knockback(double strength,double x,double z,net.minecraft.world.damagesource.DamageSource source,float force,boolean causedByAttack){}
    @Override public void checkDespawn(){if(level().getDifficulty()!=net.minecraft.world.Difficulty.PEACEFUL||!seedling()&&planter==null)super.checkDespawn();}
    @Override protected void tickDeath(){if(seedling()){super.tickDeath();return;}deathTime++;if(deathTime>=50&&!level().isClientSide()){level().broadcastEntityEvent(this,(byte)60);remove(RemovalReason.KILLED);}}
    @Override public boolean removeWhenFarAway(double distance){return !seedling()&&planter==null;}
    @Override protected void addAdditionalSaveData(ValueOutput out){super.addAdditionalSaveData(out);out.putInt("growth",growth());out.putInt("active",activation());if(planter!=null)out.store("planter",UUIDUtil.CODEC,planter);}
    @Override protected void readAdditionalSaveData(ValueInput in){super.readAdditionalSaveData(in);entityData.set(GROWTH,Math.clamp(in.getIntOr("growth",0),0,2400));entityData.set(ACTIVE,Math.clamp(in.getIntOr("active",0),0,30));planter=in.read("planter",UUIDUtil.CODEC).orElse(null);}
    public static final class Seed extends Item {
        private final boolean adult;
        public Seed(Properties p){this(false,p);}public Seed(boolean adult,Properties p){super(p);this.adult=adult;}
        @Override public InteractionResult useOn(UseOnContext c){if(!(c.getPlayer() instanceof ServerPlayer p))return InteractionResult.SUCCESS;var l=p.level();var at=c.getClickedPos().above();var ground=l.getBlockState(at.below());if(!l.hasChunkAt(at)||!l.isEmptyBlock(at)||!Authority.mayChange(l,p,at)||!ground.is(BlockTags.DIRT)&&!ground.is(Blocks.MOSS_BLOCK))return InteractionResult.FAIL;var plant=(adult?ADULT:BABY).create(l,EntitySpawnReason.MOB_SUMMONED);if(plant==null)return InteractionResult.FAIL;plant.snapTo(at.getX()+.5,at.getY(),at.getZ()+.5,0,0);if(!l.noCollision(plant)||!l.getWorldBorder().isWithinBounds(plant.getBoundingBox()))return InteractionResult.FAIL;plant.planter=p.getUUID();plant.setPersistenceRequired();if(!l.addFreshEntity(plant))return InteractionResult.FAIL;c.getItemInHand().consume(1,p);return InteractionResult.SUCCESS;}
    }
}
