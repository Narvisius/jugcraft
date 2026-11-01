package io.github.jimbozoomer.jugcraft.ribbits;
import net.minecraft.world.level.block.BonemealSource;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.*;
import com.geckolib.util.GeckoLibUtil;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.finance.LandRegistry;
import io.github.jimbozoomer.jugcraft.town.TownProtection;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.trading.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;

/** Supplied Ribbit jobs and poses: persistent residents, finite trades and bounded loaded-chunk work. */
public final class Ribbit extends PathfinderMob implements GeoEntity,Merchant {
    private static final EntityDataAccessor<Integer> PROF=SynchedEntityData.defineId(Ribbit.class,EntityDataSerializers.INT),ACTION=SynchedEntityData.defineId(Ribbit.class,EntityDataSerializers.INT),INSTRUMENT=SynchedEntityData.defineId(Ribbit.class,EntityDataSerializers.INT),UMBRELLA=SynchedEntityData.defineId(Ribbit.class,EntityDataSerializers.INT);
    public static final String[] PROFESSIONS={"nitwit","gardener","sorcerer","fisherman","merchant"},INSTRUMENTS={"bass","bongo","flute","guitar"};
    private static final EntityDataAccessor<Integer> BAND=SynchedEntityData.defineId(Ribbit.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> PRIDE=SynchedEntityData.defineId(Ribbit.class,EntityDataSerializers.BOOLEAN);
    private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
    private BlockPos home,work;
    private UUID keeper;
    private int actionTicks,cooldown=100,searchCursor,fishWait=600;
    private long restockDay=-1;
    private MerchantOffers offers;
    private Player trading;
    public Ribbit(EntityType<? extends PathfinderMob> type,Level level,int profession){super(type,level);entityData.set(PROF,profession<0?random.nextInt(5):profession);entityData.set(UMBRELLA,1+random.nextInt(3));setPersistenceRequired();}
    public String profession(){return PROFESSIONS[entityData.get(PROF)];}
    public int bandId(){return entityData.get(BAND);}
    public int action(){return entityData.get(ACTION);}
    public String instrument(){return INSTRUMENTS[Math.clamp(entityData.get(INSTRUMENT),0,3)];}
    public String model(){return action()==5?"umbrella/"+profession()+"/umbrella_"+entityData.get(UMBRELLA):action()==1?instrument()+"_ribbit":(entityData.get(PRIDE)?"pride":profession())+"_ribbit";}
    public static AttributeSupplier.Builder attributes(){return Mob.createMobAttributes().add(Attributes.MAX_HEALTH,15).add(Attributes.MOVEMENT_SPEED,.125).add(Attributes.FOLLOW_RANGE,32);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(PROF,0);b.define(ACTION,0);b.define(INSTRUMENT,0);b.define(UMBRELLA,1);b.define(PRIDE,false);b.define(BAND,0);}
    @Override public boolean canBreatheUnderwater(){return true;}
    @Override protected SoundEvent getAmbientSound(){return action()==1?null:OwnerRibbits.sound("entity.ribbit.ambient");}
    @Override protected SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source){return OwnerRibbits.sound("entity.ribbit.hurt");}
    @Override protected SoundEvent getDeathSound(){return OwnerRibbits.sound("entity.ribbit.death");}
    @Override protected void playStepSound(BlockPos pos,net.minecraft.world.level.block.state.BlockState state){playSound(OwnerRibbits.sound("entity.ribbit.step"),.15F,1);}
    @Override protected void registerGoals(){
        goalSelector.addGoal(0,new FloatGoal(this));goalSelector.addGoal(1,new PanicGoal(this,1.5));
        goalSelector.addGoal(5,new WaterAvoidingRandomStrollGoal(this,.7){@Override public boolean canUse(){return action()==0&&work==null&&trading==null&&super.canUse();}});
        goalSelector.addGoal(6,new LookAtPlayerGoal(this,Player.class,6));goalSelector.addGoal(7,new RandomLookAroundGoal(this));
    }
    private boolean mayTend(ServerLevel level,BlockPos pos){
        if(!level.hasChunkAt(pos)||TownProtection.shieldsBlock(level,pos))return false;
        if(keeper!=null){var actor=Authority.present(level,keeper);return actor!=null&&Authority.mayChange(level,actor,pos);}
        return LandRegistry.get(level.getServer()).claim(LandRegistry.keyOf(pos))==null;
    }
    @Override protected void customServerAiStep(ServerLevel level){
        super.customServerAiStep(level);if(tickCount%1200==1)entityData.set(PRIDE,profession().equals("nitwit")&&java.time.LocalDate.now(java.time.ZoneOffset.UTC).getMonthValue()==6&&new java.util.Random(getUUID().getLeastSignificantBits()).nextFloat()<.33F);if(home==null)home=blockPosition().immutable();
        if(trading!=null){if(!stillValid(trading))trading=null;else{getNavigation().stop();entityData.set(ACTION,0);return;}}
        long day=level.getDefaultClockTime()/12000;if(day>restockDay){restockDay=day;if(offers!=null)offers.forEach(MerchantOffer::resetUses);}
        if(!onGround()&&!isInWater()&&getDeltaMovement().y<-.08){entityData.set(ACTION,5);setDeltaMovement(getDeltaMovement().multiply(1,.6,1));fallDistance=0;return;}
        if(action()==5)entityData.set(ACTION,0);
        if(level.getDefaultClockTime()%24000>13000){entityData.set(ACTION,0);work=null;if(tickCount%40==0&&distanceToSqr(Vec3.atCenterOf(home))>4)getNavigation().moveTo(home.getX()+.5,home.getY(),home.getZ()+.5,1);return;}
        if(cooldown>0){cooldown--;return;}
        if(actionTicks>0){getNavigation().stop();actionTicks--;if(actionTicks==0)finishJob(level);return;}
        if(tickCount%20!=0)return;
        if(profession().equals("sorcerer")){
            if(level.getNearestPlayer(this,32)!=null){entityData.set(ACTION,4);actionTicks=22;playSound(OwnerRibbits.sound("entity.ribbit.magic"),1,1);}else cooldown=100;
        }else if(profession().equals("gardener"))garden(level);
        else if(profession().equals("fisherman"))fish(level);
        else if(profession().equals("nitwit"))band(level);
    }
    private void garden(ServerLevel level){
        if(work!=null&&(!mayTend(level,work)||!(level.getBlockState(work).getBlock() instanceof CropBlock crop)||crop.isMaxAge(level.getBlockState(work))))work=null;
        if(work==null)for(int i=0;i<64;i++){
            int n=searchCursor++%(33*33*7);var pos=home.offset(n%33-16,n/(33*33)-3,n/33%33-16);
            if(mayTend(level,pos)&&level.getBlockState(pos).getBlock() instanceof CropBlock crop&&!crop.isMaxAge(level.getBlockState(pos))){work=pos.immutable();break;}
        }
        if(work==null)return;
        if(distanceToSqr(Vec3.atCenterOf(work))<5){entityData.set(ACTION,2);actionTicks=34;getLookControl().setLookAt(work.getX()+.5,work.getY(),work.getZ()+.5);}
        else getNavigation().moveTo(work.getX()+.5,work.getY(),work.getZ()+.5,1);
    }
    private void fish(ServerLevel level){
        if(work==null)for(int i=0;i<64;i++){
            int n=searchCursor++%(33*33*7);var pos=home.offset(n%33-16,n/(33*33)-3,n/33%33-16);
            if(!level.hasChunkAt(pos)||!level.getFluidState(pos).is(net.minecraft.tags.FluidTags.WATER)||!level.getBlockState(pos.above()).isAir())continue;
            for(var d:Direction.Plane.HORIZONTAL){var bank=pos.relative(d);if(level.hasChunkAt(bank)&&level.getBlockState(bank).isFaceSturdy(level,bank,Direction.UP)&&level.getBlockState(bank.above()).isAir()){work=pos.immutable();getNavigation().moveTo(bank.getX()+.5,bank.getY()+1,bank.getZ()+.5,1);break;}}if(work!=null)break;
        }
        if(work!=null&&distanceToSqr(Vec3.atCenterOf(work))<9&&!isInWater()){
            entityData.set(ACTION,3);actionTicks=fishWait=600+random.nextInt(1201);getLookControl().setLookAt(work.getX()+.5,work.getY(),work.getZ()+.5);
        }else if(work!=null&&getNavigation().isDone())work=null;
    }
    private void band(ServerLevel level){
        if(isInWater())return;
        var nearby=level.getEntitiesOfClass(Ribbit.class,getBoundingBox().inflate(16),r->r!=this&&r.action()==1);
        if(!nearby.isEmpty()){
            var leader=nearby.getFirst();if(distanceToSqr(leader)>9){getNavigation().moveTo(leader,1);return;}
            var used=new HashSet<String>();for(var r:nearby)used.add(r.instrument());int chosen=-1;
            for(int i=0;i<4;i++)if(!used.contains(INSTRUMENTS[i])){chosen=i;break;}if(chosen<0){cooldown=100;return;}entityData.set(INSTRUMENT,chosen);entityData.set(BAND,leader.bandId());
        }else {entityData.set(INSTRUMENT,random.nextInt(4));entityData.set(BAND,getId());}
        entityData.set(ACTION,1);actionTicks=2000+random.nextInt(1001);
    }
    private void finishJob(ServerLevel level){
        switch(action()){
            case 2->{if(work!=null)for(var pos:BlockPos.betweenClosed(work.offset(-1,0,-1),work.offset(1,0,1)))if(mayTend(level,pos)&&level.getBlockState(pos).getBlock() instanceof CropBlock crop){var state=level.getBlockState(pos);if(!crop.isMaxAge(state)){crop.performBonemeal(level,random,pos,state,BonemealSource.MOB);level.sendParticles(ParticleTypes.HAPPY_VILLAGER,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,3,.2,.2,.2,0);}}cooldown=1200;}
            case 3->{if(work!=null&&level.hasChunkAt(work)&&level.getFluidState(work).is(net.minecraft.tags.FluidTags.WATER))level.sendParticles(ParticleTypes.SPLASH,work.getX()+.5,work.getY()+1,work.getZ()+.5,8,.2,.1,.2,0);cooldown=100;}
            case 4->{var buffs=List.of(MobEffects.REGENERATION,MobEffects.SPEED,MobEffects.JUMP_BOOST,MobEffects.FIRE_RESISTANCE,MobEffects.WATER_BREATHING,MobEffects.LUCK);var buff=buffs.get(random.nextInt(buffs.size()));for(var p:level.getEntitiesOfClass(ServerPlayer.class,getBoundingBox().inflate(32,5,32),p->p.isAlive()&&!p.isSpectator()).stream().limit(32).toList())p.addEffect(new MobEffectInstance(buff,buff==MobEffects.REGENERATION?1200:2400));cooldown=12000;}
            default->cooldown=200;
        }work=null;entityData.set(ACTION,0);
    }
    @Override protected InteractionResult mobInteract(Player user,InteractionHand hand){
        if(!(user instanceof ServerPlayer p))return InteractionResult.SUCCESS;var held=p.getItemInHand(hand);
        if(p.isShiftKeyDown()&&held.is(Items.AMETHYST_SHARD)){
            if(keeper!=null&&!keeper.equals(p.getUUID())||!Authority.mayChange(p.level(),p,blockPosition()))return InteractionResult.FAIL;
            keeper=p.getUUID();home=blockPosition().immutable();held.consume(1,p);p.level().broadcastEntityEvent(this,(byte)12);return InteractionResult.SUCCESS;
        }
        if(!getOffers().isEmpty()&&(trading==null||trading==p)){trading=p;openTradingScreen(p,getDisplayName(),1);return InteractionResult.SUCCESS;}
        return super.mobInteract(user,hand);
    }
    @Override public MerchantOffers getOffers(){
        if(offers!=null)return offers;offers=new MerchantOffers();
        if(profession().equals("merchant")){
            for(String block:new String[]{"brown_toadstool","red_toadstool","toadstool_stem"})sell(OwnerRibbits.BLOCKS.get(block).asItem(),1,8+random.nextInt(9),32);
            sell(OwnerRibbits.BLOCKS.get("mossy_oak_planks").asItem(),1+random.nextInt(2),16+random.nextInt(17),32);
            sell(OwnerRibbits.BLOCKS.get("swamp_lantern").asItem(),2+random.nextInt(2),4+random.nextInt(5),32);sell(OwnerRibbits.MARACA,6+random.nextInt(3),1,4);
        }else if(profession().equals("fisherman")){
            // Intermediary source IDs resolved against Fabric Yarn 1.21.1 Items mappings.
            var choices=new LinkedHashSet<Integer>();while(choices.size()<4)choices.add(random.nextInt(7));
            for(int choice:choices)switch(choice){
                case 0->sell(Items.AXOLOTL_BUCKET,4+random.nextInt(5),1,16);
                case 1->sell(Items.TROPICAL_FISH_BUCKET,4+random.nextInt(5),1,16);
                case 2->sell(Items.COOKED_COD,4+random.nextInt(5),8+random.nextInt(17),16);
                case 3->sell(Items.COOKED_SALMON,4+random.nextInt(5),8+random.nextInt(17),16);
                case 4->{var rod=net.minecraft.world.item.enchantment.EnchantmentHelper.enchantItem(random,new ItemStack(Items.FISHING_ROD),5+random.nextInt(15),registryAccess(),Optional.empty());offers.add(new MerchantOffer(new ItemCost(Items.AMETHYST_SHARD,12+random.nextInt(5)),rod,4,0,.05F));}
                case 5,6->offers.add(new MerchantOffer(new ItemCost(choice==5?Items.COD:Items.SALMON,16+random.nextInt(17)),new ItemStack(Items.AMETHYST_SHARD,4+random.nextInt(5)),16,0,.05F));
            }
        }return offers;
    }
    private void sell(Item item,int price,int count,int uses){offers.add(new MerchantOffer(new ItemCost(Items.AMETHYST_SHARD,price),new ItemStack(item,count),uses,1,.05F));}
    @Override public void setTradingPlayer(Player p){trading=p;}@Override public Player getTradingPlayer(){return trading;}@Override public void overrideOffers(MerchantOffers o){offers=o;}@Override public void notifyTrade(MerchantOffer o){o.increaseUses();playSound(getNotifyTradeSound(),1,1);}@Override public void notifyTradeUpdated(ItemStack s){}@Override public int getVillagerXp(){return 0;}@Override public void overrideXp(int xp){}@Override public boolean showProgressBar(){return false;}@Override public SoundEvent getNotifyTradeSound(){return OwnerRibbits.sound("entity.ribbit.ambient");}@Override public boolean isClientSide(){return level().isClientSide();}@Override public boolean stillValid(Player p){return isAlive()&&distanceToSqr(p)<=64&&!p.isSpectator();}
    @Override protected void addAdditionalSaveData(ValueOutput out){super.addAdditionalSaveData(out);out.putInt("profession",entityData.get(PROF));out.putInt("umbrella",entityData.get(UMBRELLA));out.putInt("job_cooldown",cooldown);out.putLong("restock_day",restockDay);if(home!=null)out.store("home",BlockPos.CODEC,home);if(keeper!=null)out.store("keeper",UUIDUtil.CODEC,keeper);if(offers!=null)out.store("offers",MerchantOffers.CODEC,offers);}
    @Override protected void readAdditionalSaveData(ValueInput in){super.readAdditionalSaveData(in);entityData.set(PROF,Math.clamp(in.getIntOr("profession",entityData.get(PROF)),0,4));entityData.set(UMBRELLA,Math.clamp(in.getIntOr("umbrella",1),1,3));cooldown=Math.clamp(in.getIntOr("job_cooldown",100),0,12000);home=in.read("home",BlockPos.CODEC).orElse(null);keeper=in.read("keeper",UUIDUtil.CODEC).orElse(null);restockDay=in.getLongOr("restock_day",-1);offers=in.read("offers",MerchantOffers.CODEC).orElse(null);entityData.set(ACTION,0);actionTicks=0;work=null;trading=null;}
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers){
        controllers.add(new AnimationController<Ribbit>("body",5,s->{
            boolean rain=level().isRainingAt(blockPosition());String clip;
            switch(action()){
                case 1->clip="play_"+instrument();
                case 2->clip=rain?"water_crops_holding":"water_crops";
                case 3->clip=rain?"fishing_holding":"fishing";
                case 4->clip=rain?"spell_holding":"spell";
                case 5->clip=profession().equals("fisherman")?"fisherman_ribbit_fall":"ribbit_fall";
                default->{String pose=s.isMoving()&&!isInWater()?"walk":"idle";clip=pose+(profession().equals("fisherman")?(rain?"_holding_fisherman":"_holding_2"):entityData.get(PRIDE)?"_holding_2":rain?(profession().equals("gardener")||profession().equals("sorcerer")?"_holding_hat":"_holding_1"):"");}
            }return s.setAndContinue(RawAnimation.begin().thenLoop(clip));
        }));
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
}
