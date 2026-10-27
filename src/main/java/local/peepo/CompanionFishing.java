package local.peepo;

import java.util.*;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.minecraft.world.phys.*;

/** Explicit dry seat + water jobs. No fake player, ticking hook entity or chunk tickets. */
public final class CompanionFishing {
    public static final Identifier ID=PeepoMod.id("fishing_job");
    public static final int RANGE=10,BATCH=8;
    private static final net.minecraft.tags.TagKey<Item> RODS=net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,Identifier.fromNamespaceAndPath("c","tools/fishing_rod"));
    private final PeepoEntity npc;
    private final Map<BlockPos,Job> jobs=new HashMap<>();
    private record Lease(UUID owner,long until){}
    private static final Map<Level,LinkedHashMap<BlockPos,Lease>> LEASES=new WeakHashMap<>();
    // One already-landed catch can wait for cargo, never another cast. Persist it before any interruption.
    private List<ItemStack> pending=List.of();
    private GlobalPos pendingAt;
    CompanionFishing(PeepoEntity npc){this.npc=npc;}
    public static boolean rod(ItemStack stack){return !stack.isEmpty() && (stack.getItem() instanceof FishingRodItem || stack.is(RODS)) && (!stack.isDamageableItem() || stack.getDamageValue()<stack.getMaxDamage());}
    public static boolean validCast(BlockPos seat,BlockPos water){long dx=(long)water.getX()-seat.getX(),dz=(long)water.getZ()-seat.getZ();return dx*dx+dz*dz>0 && dx*dx+dz*dz<=RANGE*RANGE && water.getY()<=seat.getY() && (long)seat.getY()-water.getY()<=3;}
    public static Vec3 seatPosition(BlockPos seat,BlockPos water){
        var d=new Vec3(water.getX()-seat.getX(),0,water.getZ()-seat.getZ()).normalize();
        // Approach the square block's edge even for diagonal casts, retaining a small support inset.
        double axis=Math.max(Math.abs(d.x),Math.abs(d.z));
        return Vec3.atBottomCenterOf(seat.above()).add(axis>0?d.scale(.48/axis):Vec3.ZERO);
    }
    public static boolean safeSeat(PeepoEntity npc,BlockPos seat,BlockPos water){
        var level=npc.level();if(!validCast(seat,water) || !level.hasChunkAt(seat.above(2)) || !level.hasChunkAt(water) || !level.getBlockState(seat).isFaceSturdy(level,seat,Direction.UP))return false;
        var point=seatPosition(seat,water);double r=npc.getBbWidth()/2+.01;
        if(!level.getFluidState(BlockPos.containing(point)).isEmpty() || !CompanionHazards.safeAt(npc,point) || !level.noCollision(new AABB(point.x-r,point.y,point.z-r,point.x+r,point.y+npc.getBbHeight(),point.z+r)))return false;
        var end=Vec3.atCenterOf(water).add(0,.4,0);var start=point.add(0,.5,0);
        for(int i=0;i<=20;i++)if(!level.hasChunkAt(BlockPos.containing(start.lerp(end,i/20.0))))return false;
        var hit=level.clip(new ClipContext(start,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,npc));return hit.getType()==HitResult.Type.MISS || hit.getBlockPos().equals(water);
    }
    private boolean assigned(CompanionAssignments.Target t){int row=npc.assignments.workPriority(t.at().pos());return row<5 && t.equals(npc.assignments.get(row)) && t.present(npc.level());}
    public Job job(BlockPos pos){
        int row=npc.assignments.workPriority(pos);var t=row<5?npc.assignments.get(row):null;if(t==null || !t.fishing() || !t.present(npc.level()))return null;
        jobs.entrySet().removeIf(e->{boolean stale=!assigned(e.getValue().target);if(stale)e.getValue().release(npc);return stale;});
        return jobs.computeIfAbsent(pos,p->new Job(t));
    }
    private int freeSlots(){int n=0;for(int i=0;i<8;i++)if(!npc.transport.reserved(i) && npc.belongings.getItem(i).isEmpty())n++;return n;}
    private boolean hasCatch(CompanionAssignments.Target t){for(int i=0;i<8;i++)if(!npc.garden.output(i,t).isEmpty())return true;return false;}
    public boolean deferOutput(CompanionAssignments.Target t){
        if(t==null || !t.fishing() || !assigned(t) || !npc.preferences.canWork() || !rod(npc.assignedHand()) || !pending.isEmpty() || freeSlots()<=1)return false;
        var j=jobs.get(t.at().pos());return j!=null && j.catches<BATCH && j.workStatus(npc)==CompanionStatus.READY;
    }
    public boolean outputBlocked(CompanionAssignments.Target t){var j=jobs.get(t.at().pos());return j!=null && j.outputBlocked;}
    public String detail(CompanionAssignments.Target t){var j=jobs.get(t.at().pos());if(j==null)return "Fishing: up to eight catches per delivery.";return switch(j.phase){case 0->"Casting into the marked water.";case 2->"A bite!";case 3->"Reeling in the catch.";default->"Waiting for a bite. "+j.catches+" / 8 catches; "+(j.openWater?"open-water treasure eligible.":"fish/junk only in this water.");};}
    CompanionLogistics.Port port(CompanionAssignments.Target t){return new CompanionLogistics.Port(){
        public Identifier plan(){return ID;} public int needed(ItemStack stack){return 0;}
        public Storage<ItemVariant> inputs(){return Storage.empty();} public Storage<ItemVariant> outputs(){return Storage.empty();}
        public CompanionStatus status(){var j=job(t.at().pos());return j==null?CompanionStatus.MISSING:j.workStatus(npc);}
    };}
    public void tickPending(){if(!npc.level().isClientSide() && npc.level().getGameTime()%20==Math.floorMod(npc.getId(),20))flushPending();}
    private void flushPending(){
        if(pending.isEmpty() || pendingAt==null)return;
        var rest=new ArrayList<ItemStack>();
        boolean linked=false;for(int row=1;row<=4;row++){var t=npc.assignments.get(row);if(t!=null && t.fishing() && t.at().equals(pendingAt)){linked=true;break;}}
        for(var item:pending)try(var tx=Transaction.openOuter()){
            boolean stored=linked?npc.garden.storeHarvest(item,pendingAt,tx):npc.food.store(item,tx)==item.getCount();
            if(stored)tx.commit();else rest.add(item);
        }
        pending=List.copyOf(rest);if(pending.isEmpty()){pendingAt=null;npc.readiness.clear();npc.transport.workChanged();}
    }
    void dropPending(ServerLevel level){for(var item:pending)npc.spawnAtLocation(level,item);pending=List.of();pendingAt=null;}
    void save(ValueOutput out){if(!pending.isEmpty() && pendingAt!=null){var c=out.child("FishingCatch");c.store("At",GlobalPos.CODEC,pendingAt);c.store("Items",ItemStack.CODEC.listOf(),pending);}}
    void load(ValueInput in){pending=List.of();pendingAt=null;jobs.clear();var c=in.child("FishingCatch");if(c.isPresent()){pendingAt=c.get().read("At",GlobalPos.CODEC).orElse(null);if(pendingAt!=null)pending=c.get().read("Items",ItemStack.CODEC.listOf()).orElse(List.of());}}
    /** Used only as the native loot context, so fishing-hook predicates still see open-water eligibility. */
    private static final class LootHook extends FishingHook {
        final boolean open;
        LootHook(ServerLevel level,PeepoEntity owner,BlockPos water,boolean open){super(EntityTypes.FISHING_BOBBER,level);this.open=open;setOwner(owner);setPos(Vec3.atCenterOf(water).add(0,.4,0));}
        @Override public boolean isOpenWaterFishing(){return open;}
    }
    public final class Job implements CompanionJob {
        final CompanionAssignments.Target target;
        int phase,ticks,wait,catches;long nextCheck;boolean safe,openWater,castOpenWater,claimed,outputBlocked;
        ItemStack castRod=ItemStack.EMPTY;
        Job(CompanionAssignments.Target t){target=t;}
        public Kind kind(){return Kind.WORK;}
        public BlockPos stationPosition(){return target.at().pos();}
        public BlockPos animationTarget(){return target.fishingWater();}
        public Vec3 approachPosition(){return seatPosition(stationPosition(),target.fishingWater());}
        public double arrivalDistanceSquared(){return .0025;}
        public WorkAnimation animation(){return switch(phase){case 0->WorkAnimation.FISH_CAST;case 2->WorkAnimation.FISH_BITE;case 3->WorkAnimation.FISH_REEL;default->WorkAnimation.FISH_WAIT;};}
        public boolean timeCritical(){return claimed && (phase==2 || phase==3);}
        private Lease lease(){var m=LEASES.get(npc.level());return m==null?null:m.get(stationPosition());}
        public boolean availableTo(PeepoEntity other){var l=lease();return other==npc && assigned(target) && (l==null || l.until<=npc.level().getGameTime() || l.owner.equals(npc.getUUID()));}
        private void environment(){
            long now=npc.level().getGameTime();if(now<nextCheck)return;if(!CompanionBudget.search(npc))return;nextCheck=now+20;
            safe=safeSeat(npc,stationPosition(),target.fishingWater()) && !npc.level().getFluidState(target.fishingWater().above()).is(FluidTags.WATER);
            outputBlocked=false;
            if(hasCatch(target) && npc.assignments.transportAllowed(target,false) && !npc.assignments.supplies.containers(target,true).isEmpty()){
                var output=npc.assignments.supplies.combined(target,true);
                try(var tx=Transaction.openOuter()){
                    for(int i=0;i<8;i++){var item=npc.garden.output(i,target);if(!item.isEmpty() && output.insert(ItemVariant.of(item),item.getCount(),tx)<item.getCount()){outputBlocked=true;break;}}
                }
            }
            // Native open-water footprint: two source-water layers and two clear layers above.
            openWater=true;var level=npc.level();
            for(int y=-1;y<=2 && openWater;y++)for(int x=-2;x<=2 && openWater;x++)for(int z=-2;z<=2;z++){
                var p=target.fishingWater().offset(x,y,z);if(!level.hasChunkAt(p)){openWater=false;break;}
                var b=level.getBlockState(p);var f=level.getFluidState(p);
                if(y<=0?!(f.is(FluidTags.WATER) && f.isSource() && b.getCollisionShape(level,p).isEmpty()):!(b.isAir() || b.is(Blocks.LILY_PAD))){openWater=false;break;}
            }
        }
        public CompanionStatus workStatus(PeepoEntity other){
            if(other!=npc || !assigned(target))return CompanionStatus.MISSING;
            if(!CompanionJobs.permitted(npc,stationPosition()) || !CompanionJobs.permitted(npc,target.fishingWater()))return CompanionStatus.FORBIDDEN;
            if(!((ServerLevel)npc.level()).getGameRules().get(GameRules.MOB_GRIEFING))return CompanionStatus.GARDEN_DISABLED;
            if(!availableTo(other))return CompanionStatus.OCCUPIED;
            if(!CompanionWater.source(npc.level(),target.fishingWater()))return CompanionStatus.NO_WATER_SOURCE;
            if(!rod(npc.assignedHand()))return CompanionStatus.NEEDS_ROD;
            environment();if(!safe)return CompanionStatus.FISHING_BLOCKED;
            if(pending.isEmpty() && !hasCatch(target))catches=0;
            if(outputBlocked || !pending.isEmpty() || freeSlots()==0 || catches>=BATCH && hasCatch(target))return CompanionStatus.FULL;
            return CompanionStatus.READY;
        }
        public boolean claim(PeepoEntity other){
            if(workStatus(other)!=CompanionStatus.READY)return false;
            var map=LEASES.computeIfAbsent(npc.level(),l->new LinkedHashMap<>());long now=npc.level().getGameTime();
            if(!map.containsKey(stationPosition()) && map.size()>=4096){var it=map.entrySet().iterator();int n=0;while(it.hasNext() && n++<64)if(it.next().getValue().until<=now)it.remove();if(map.size()>=4096)return false;}
            map.put(stationPosition(),new Lease(npc.getUUID(),now+100));claimed=true;return true;
        }
        public boolean occupy(PeepoEntity other){return npc.position().distanceToSqr(approachPosition())<=.04 && claim(other);}
        public void release(PeepoEntity other){if(other!=npc)return;var l=lease();if(l!=null && l.owner.equals(npc.getUUID()))LEASES.get(npc.level()).remove(stationPosition());claimed=false;phase=ticks=wait=0;castRod=ItemStack.EMPTY;}
        public CompanionStatus work(PeepoEntity other){
            if(!npc.preferences.canWork() || !npc.orders.station(this) || !occupy(other))return CompanionStatus.IDLE;
            var level=(ServerLevel)npc.level();var rod=npc.assignedHand();
            if(castRod.isEmpty()){castRod=rod.copy();castOpenWater=openWater;}else if(!ItemStack.matches(castRod,rod)){release(npc);return CompanionStatus.IDLE;}
            castOpenWater&=openWater;
            int effort=1+(phase==0 || phase==3?2:0);
            try(var tx=Transaction.openOuter()){if(npc.extractWorkEnergy(CompanionUniforms.Type.FISHERMAN,effort,tx)!=effort)return CompanionStatus.RECOVERING;tx.commit();}
            var water=Vec3.atCenterOf(target.fishingWater());float yaw=(float)Math.toDegrees(Math.atan2(-(water.x-npc.getX()),water.z-npc.getZ()));npc.setYRot(yaw);npc.yBodyRot=yaw;npc.setYHeadRot(yaw);
            ticks++;
            if(phase==0){if(ticks==1)sound(SoundEvents.FISHING_BOBBER_THROW);if(ticks>=20){phase=1;ticks=0;wait=Math.max(20,100+npc.getRandom().nextInt(501)-(int)(EnchantmentHelper.getFishingTimeReduction(level,rod,npc)*20))+20+npc.getRandom().nextInt(61);if(npc.uniforms.wearing(CompanionUniforms.Type.FISHERMAN))wait=Math.max(20,(int)Math.ceil(wait*.85));}}
            else if(phase==1){
                int speed=1;if(level.isRainingAt(target.fishingWater().above()) && npc.getRandom().nextFloat()<.25F)speed++;if(!level.canSeeSky(target.fishingWater().above()) && npc.getRandom().nextFloat()<.5F)speed--;
                if((wait-=speed)<=0){phase=2;ticks=0;sound(SoundEvents.FISHING_BOBBER_SPLASH);level.sendParticles(ParticleTypes.SPLASH,water.x,water.y+.4,water.z,6,.12,0,.12,.02);}
            }else if(phase==2 && ticks>=10){phase=3;ticks=0;sound(SoundEvents.FISHING_BOBBER_RETRIEVE);}
            else if(phase==3 && ticks>=20){
                var hook=new LootHook(level,npc,target.fishingWater(),castOpenWater);
                var params=new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN,hook.position()).withParameter(LootContextParams.TOOL,rod).withParameter(LootContextParams.THIS_ENTITY,hook).withLuck(EnchantmentHelper.getFishingLuckBonus(level,rod,npc)).create(LootContextParamSets.FISHING);
                var loot=level.getServer().reloadableRegistries().getLootTable(BuiltInLootTables.FISHING).getRandomItems(params);
                var fish=loot.stream().filter(s->s.is(net.minecraft.tags.ItemTags.FISHES)).findFirst();
                if(fish.isPresent())try(var tx=Transaction.openOuter()){
                    if(npc.uniforms.bonus(CompanionUniforms.Type.FISHERMAN,tx))loot.add(fish.get().copy());tx.commit();
                }
                pendingAt=target.at();pending=loot.stream().limit(64).filter(s->!s.isEmpty()).map(ItemStack::copy).toList();
                for(int i=64;i<loot.size();i++)npc.spawnAtLocation(level,loot.get(i));
                rod.hurtAndBreak(1,npc,EquipmentSlot.MAINHAND);
                var mending=level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(Enchantments.MENDING);
                if(!loot.isEmpty() && !rod.isEmpty() && rod.isDamaged() && EnchantmentHelper.getItemEnchantmentLevel(mending,rod)>0)rod.setDamageValue(Math.max(0,rod.getDamageValue()-EnchantmentHelper.modifyDurabilityToRepairFromXp(level,rod,1+npc.getRandom().nextInt(6))));
                npc.belongings.setChanged();catches++;phase=ticks=0;castRod=ItemStack.EMPTY;
                flushPending();nextCheck=0;npc.readiness.clear();npc.transport.workChanged();return workStatus(npc)==CompanionStatus.READY?CompanionStatus.WORKING:CompanionStatus.IDLE;
            }
            return CompanionStatus.WORKING;
        }
        private void sound(SoundEvent sound){npc.level().playSound(null,npc.blockPosition(),sound,SoundSource.NEUTRAL,.5F,1);}
    }
}
