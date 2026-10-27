package local.peepo;

import java.util.*;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.*;

/** Fixed pen jobs collect real dropped eggs, using the shared routine and produce ledger. */
final class CompanionEggCollection {
    static final int WORK_TICKS=20, ENERGY_PER_TICK=4;
    private record Claim(UUID worker,long until){}
    private static final AttachmentType<Claim> CLAIM=AttachmentRegistry.<Claim>builder().buildAndRegister(PeepoMod.id("egg_work_claim"));
    private final PeepoEntity npc;
    private final Map<BlockPos,Job> jobs=new HashMap<>(),lootJobs=new HashMap<>();
    static void initialize(){} // Register the transient claim before entities can spawn.
    CompanionEggCollection(PeepoEntity npc){this.npc=npc;}
    CompanionJob job(CompanionAssignments.Target target){return job(target,false);}
    CompanionJob lootJob(CompanionAssignments.Target target){return job(target,true);}
    private CompanionJob job(CompanionAssignments.Target target,boolean loot){
        var jobs=loot?lootJobs:this.jobs;
        jobs.entrySet().removeIf(e->!npc.assignments.assignedWork(e.getKey()));
        var old=jobs.get(target.at().pos());
        if(old==null || !old.target.equals(target)){if(old!=null)old.release(npc);old=new Job(target,loot);jobs.put(target.at().pos(),old);}
        return old;
    }
    private long now(){return npc.level().getGameTime();}
    private boolean enabled(){return npc.level() instanceof ServerLevel level && level.getGameRules().get(GameRules.MOB_GRIEFING);}

    private final class Job implements CompanionJob {
        private final CompanionAssignments.Target target;
        private final boolean loot;
        private UUID itemId;
        private Vec3 approach;
        private long nextScan,nextApproach;
        private int progress;
        private CompanionStatus status=CompanionStatus.EGGS_WAIT;
        Job(CompanionAssignments.Target target,boolean loot){this.target=target;this.loot=loot;}
        public Kind kind(){return Kind.WORK;}
        public BlockPos stationPosition(){return target.at().pos();}
        public Vec3 approachPosition(){return approach==null?Vec3.atBottomCenterOf(stationPosition().above()):approach;}
        public BlockPos animationTarget(){var item=item();return item==null?stationPosition():item.blockPosition();}
        private ItemEntity item(){return itemId!=null && ((ServerLevel)npc.level()).getEntity(itemId) instanceof ItemEntity item?item:null;}
        private boolean assigned(){int row=npc.assignments.workPriority(stationPosition());return row<5 && target.equals(npc.assignments.get(row)) && target.present(npc.level()) && (loot || target.task(AnimalCareZone.EGGS)) && CompanionJobs.permitted(npc,stationPosition());}
        private boolean eligible(ItemEntity item){
            return item.isAlive() && !item.hasPickUpDelay() && (loot?CompanionSlaughterLoot.belongs(item,npc,target):item.getItem().is(ItemTags.EGGS) && !CompanionSlaughterLoot.marked(item)) && item.onGround()
                && !item.isInWater() && !item.isOnFire()
                && (loot?target.animalBox().inflate(1).contains(item.position()):target.animalContains(item.position()))
                && CompanionJobs.permitted(npc,item.blockPosition());
        }
        private boolean free(ItemEntity item){var claim=item.getAttached(CLAIM);return claim==null || claim.until<=now() || claim.worker.equals(npc.getUUID());}
        private Vec3 standing(ItemEntity item){
            var center=item.blockPosition();var floors=new ArrayList<BlockPos>(5);floors.add(center.below());
            for(var side:Direction.Plane.HORIZONTAL)floors.add(center.below().relative(side));
            Vec3 best=null;
            for(var floor:floors){
                if(!CompanionJobs.permitted(npc,floor) || !CompanionJobs.permitted(npc,floor.above())
                    || !npc.level().getBlockState(floor).isFaceSturdy(npc.level(),floor,Direction.UP))continue;
                var p=Vec3.atBottomCenterOf(floor.above());double r=npc.getBbWidth()/2+.015;
                if(p.distanceToSqr(item.position())>1.5625 || !CompanionHazards.safeAt(npc,p)
                    || npc.navigationMemory.failed(npc,stationPosition(),p)
                    || !npc.level().noCollision(npc,new AABB(p.x-r,p.y+.01,p.z-r,p.x+r,p.y+npc.getBbHeight(),p.z+r)))continue;
                if(best==null || npc.position().distanceToSqr(p)<npc.position().distanceToSqr(best))best=p;
            }
            return best;
        }
        private boolean find(){
            var current=item();
            if(current!=null && eligible(current) && free(current)){
                if(now()>=nextApproach){nextApproach=now()+10;approach=standing(current);}
                if(approach==null)status=CompanionStatus.BLOCKED;
                return approach!=null;
            }
            if(itemId!=null)release(npc);
            if(now()<nextScan)return false;
            if(!CompanionBudget.search(npc)){status=CompanionStatus.WAITING;return false;}
            nextScan=now()+80+Math.floorMod(npc.getId(),20);status=CompanionStatus.EGGS_WAIT;
            var candidates=new ArrayList<ItemEntity>(24);
            ((ServerLevel)npc.level()).getEntities(EntityTypeTest.forClass(ItemEntity.class),(loot?target.animalBox().inflate(1):target.animalBox()),e->eligible(e) && free(e),candidates,24);
            candidates.sort(Comparator.comparingDouble(npc::distanceToSqr));
            for(var candidate:candidates){var point=standing(candidate);if(point==null){status=CompanionStatus.BLOCKED;continue;}itemId=candidate.getUUID();approach=point;nextApproach=now()+10;return true;}
            return false;
        }
        public boolean availableTo(PeepoEntity other){var item=item();return other==npc && assigned() && (item==null || free(item));}
        public CompanionStatus workStatus(PeepoEntity other){
            if(other!=npc || !assigned())return CompanionStatus.MISSING;
            if(!enabled())return CompanionStatus.GARDEN_DISABLED;
            return find()?CompanionStatus.READY:status;
        }
        public boolean claim(PeepoEntity other){
            var item=item();if(other!=npc || !assigned() || !enabled() || item==null || !eligible(item) || !free(item))return false;
            item.setAttached(CLAIM,new Claim(npc.getUUID(),now()+100));return true;
        }
        public boolean occupy(PeepoEntity other){return npc.position().distanceToSqr(approachPosition())<=.64 && claim(other);}
        public void approachFailed(PeepoEntity other){release(other);nextScan=now()+20;}
        public void release(PeepoEntity other){
            if(other!=npc)return;var item=item();
            if(item!=null){var claim=item.getAttached(CLAIM);if(claim!=null && claim.worker.equals(npc.getUUID()))item.removeAttached(CLAIM);}
            itemId=null;approach=null;progress=0;
        }
        public CompanionStatus work(PeepoEntity other){
            var item=item();if(item==null || !occupy(other))return CompanionStatus.IDLE;
            // The shared routine arrives within 0.8 blocks; finish that step before
            // testing the shorter pickup reach, especially when approaching beside a fence.
            if(npc.position().distanceToSqr(approach)>.0225){npc.getMoveControl().setWantedPosition(approach.x,approach.y,approach.z,.7);return CompanionStatus.WORKING;}
            if(npc.distanceToSqr(item)>1.5625 || !npc.hasLineOfSight(item))return CompanionStatus.BLOCKED;
            npc.getLookControl().setLookAt(item,20,30);
            try(var tx=Transaction.openOuter()){if(npc.extractWorkEnergy(CompanionUniforms.Type.RANCH_HAND,ENERGY_PER_TICK,tx)!=ENERGY_PER_TICK)return CompanionStatus.RECOVERING;tx.commit();}
            if(++progress<WORK_TICKS)return CompanionStatus.WORKING;
            var result=collect(item);release(npc);status=result;nextScan=now()+(result==CompanionStatus.FULL?100:1);
            npc.transport.workChanged();return result;
        }
        private CompanionStatus collect(ItemEntity item){
            // Bounded capacity probes include provenance and reserved cargo; aborted probes restore both.
            var stack=item.getItem();int low=0,high=Math.min(stack.getCount(),8*stack.getMaxStackSize());
            while(low<high){int mid=(low+high+1)/2;try(var tx=Transaction.openOuter()){
                if(npc.garden.storeHarvest(stack.copyWithCount(mid),target.at(),tx))low=mid;else high=mid-1;
            }}
            if(low==0)return CompanionStatus.FULL;
            try(var tx=Transaction.openOuter()){
                if(!npc.garden.storeHarvest(stack.copyWithCount(low),target.at(),tx))return CompanionStatus.FULL;
                tx.commit();
            }
            // No interleaving on the server thread: remove only what entered the real cargo.
            npc.take(item,low);var rest=stack.copy();rest.shrink(low);
            if(rest.isEmpty())item.discard();else item.setItem(rest);
            npc.level().playSound(null,npc.blockPosition(),SoundEvents.ITEM_PICKUP,SoundSource.NEUTRAL,.2F,1.1F);
            return CompanionStatus.IDLE;
        }
    }
}
