package local.peepo;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.*;

/** Assigned lunch first; bounded ambient fallback only when no source has been assigned. */
final class FindLunchGoal extends Goal {
    private final PeepoEntity npc;
    private LunchBlockEntity lunch;
    private Vec3 approach;
    private long nextSearch,deadline,failedUntil;
    private BlockPos failed;
    private int cursor;
    private final CompanionNavigation.Progress travel=new CompanionNavigation.Progress();
    private net.minecraft.world.level.pathfinder.Path selectedPath;
    FindLunchGoal(PeepoEntity npc){this.npc=npc;nextSearch=npc.level().getGameTime()+Math.floorMod(npc.getId(),80);setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
    @Override public boolean requiresUpdateEveryTick(){return true;}
    private boolean wantsVisit(){
        boolean bound=npc.assignments.get(CompanionAssignments.LUNCH)!=null;
        return npc.needsAutomaticFood() && npc.food.meals()==0 || bound && (npc.food.needsSupplies() && npc.food.hasRoom() || npc.food.hasReturns());
    }
    private boolean valid(){
        if(lunch==null || !npc.level().hasChunkAt(lunch.getBlockPos()) || npc.level().getBlockEntity(lunch.getBlockPos())!=lunch || !lunch.feeds(npc))return false;
        var bound=npc.assignments.get(CompanionAssignments.LUNCH);
        return (bound==null || bound.present(npc.level()) && bound.at().pos().equals(lunch.getBlockPos())) && npc.orders.food(Vec3.atCenterOf(lunch.getBlockPos()));
    }
    @Override public boolean canUse(){
        long now=npc.level().getGameTime();
        if(!npc.orders.tamed() || !wantsVisit() || npc.isEating() || now<nextSearch)return false;
        if(!CompanionBudget.search(npc))return false;
        nextSearch=now+80+Math.floorMod(npc.getId(),20);
        List<LunchBlockEntity> candidates=new ArrayList<>();
        var bound=npc.assignments.get(CompanionAssignments.LUNCH);
        if(bound!=null){
            if(!bound.present(npc.level()))return false;
            if(npc.level().getBlockEntity(bound.at().pos()) instanceof LunchBlockEntity source && source.feeds(npc)
                && source.getBlockPos().distToCenterSqr(npc.position())<=64*64 && npc.orders.food(Vec3.atCenterOf(source.getBlockPos())))candidates.add(source);
        }else{
            var origin=npc.blockPosition();int reads=0;
            outer:for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)
                for(var pos:CompanionStationIndex.positions(npc.level(),origin.offset(dx*16,0,dz*16))){
                    if(++reads>128 || candidates.size()>=16)break outer;
                    if(pos.distToCenterSqr(npc.position())<=256 && npc.orders.food(Vec3.atCenterOf(pos))
                        && npc.level().getBlockEntity(pos) instanceof LunchBlockEntity source && source.feeds(npc))candidates.add(source);
                }
        }
        candidates.sort(Comparator.comparingDouble(b->b.getBlockPos().distToCenterSqr(npc.position())));
        int paths=0;
        for(int n=0;n<Math.min(4,candidates.size());n++){
            var source=candidates.get(Math.floorMod(cursor++,candidates.size()));
            if(source.getBlockPos().equals(failed) && now<failedUntil){npc.report.lunch(CompanionStatus.BLOCKED);continue;}
            if(!npc.food.hasReturns() && source.takeMeal(npc,false).isEmpty()){npc.report.lunch(CompanionStatus.NO_FOOD);continue;}
            List<BlockPos> points=new ArrayList<>();
            for(Direction d:Direction.Plane.HORIZONTAL)for(int dy=0;dy>=-1;dy--){
                var pos=source.sourcePos().relative(d).offset(0,dy,0);var at=Vec3.atBottomCenterOf(pos);
                if(npc.level().hasChunkAt(pos) && npc.orders.food(at)
                    && npc.level().getBlockState(pos.below()).isFaceSturdy(npc.level(),pos.below(),Direction.UP)
                    && CompanionHazards.safeAt(npc,at) && npc.level().noCollision(npc,new AABB(at.x-.22,at.y,at.z-.22,at.x+.22,at.y+1,at.z+.22)))points.add(pos);
            }
            points.removeIf(p->npc.navigationMemory.failed(npc,source.getBlockPos(),Vec3.atBottomCenterOf(p)));
            points.sort(Comparator.comparingDouble(p->p.distToCenterSqr(npc.position())));
            for(var pos:points){
                if(paths++>=2)break;
                if(!CompanionBudget.path(npc)){nextSearch=now+1;return false;}
                var path=npc.getNavigation().createPath(pos,0,bound==null?16:64);
                if(path!=null && path.canReach()){lunch=source;approach=Vec3.atBottomCenterOf(pos);selectedPath=path;return true;}
                npc.navigationMemory.reject(npc,source.getBlockPos(),Vec3.atBottomCenterOf(pos));
            }
            failed=source.getBlockPos();failedUntil=now+200;npc.report.lunch(CompanionStatus.BLOCKED);
        }
        if(candidates.isEmpty() && npc.needsAutomaticFood())npc.report.lunch(CompanionStatus.NO_FOOD);
        return false;
    }
    @Override public boolean canContinueToUse(){return valid() && wantsVisit() && !npc.isEating() && npc.level().getGameTime()<deadline;}
    @Override public void start(){npc.resetCompanionRoutine();npc.leaveCompanionBed();deadline=npc.level().getGameTime()+600;travel.reset();if(selectedPath!=null){npc.getNavigation().moveTo(selectedPath,1);travel.started(npc,approach);}selectedPath=null;npc.report.lunch(CompanionStatus.FETCHING_FOOD);}
    @Override public void stop(){lunch=null;approach=null;npc.getNavigation().stop();if(npc.report.lunch()==CompanionStatus.FETCHING_FOOD)npc.report.lunch(CompanionStatus.READY);}
    @Override public void tick(){
        if(!valid())return;
        npc.report.lunch(CompanionStatus.FETCHING_FOOD);
        if(npc.position().distanceToSqr(approach)<=.64){
            npc.food.returnContainers(lunch);
            var meal=npc.needsAutomaticFood() && npc.food.meals()==0?lunch.takeMeal(npc,true):net.minecraft.world.item.ItemStack.EMPTY;
            if(npc.assignments.get(CompanionAssignments.LUNCH)!=null)for(int i=0;i<4 && npc.food.needsSupplies();i++)if(!lunch.stockMeal(npc))break;
            if(!meal.isEmpty())npc.beginLunchMeal(meal,GlobalPos.of(npc.level().dimension(),lunch.getBlockPos()));
            var result=meal.isEmpty() && npc.needsAutomaticFood() && npc.food.meals()==0?CompanionStatus.NO_FOOD:npc.food.hasReturns()?CompanionStatus.FULL:CompanionStatus.READY;
            npc.report.lunch(result);
            deadline=0;nextSearch=npc.level().getGameTime()+(result==CompanionStatus.FULL?600:100);return;
        }
        if(travel.needsPath(npc,approach) && CompanionBudget.path(npc)){
            npc.getNavigation().stop();
            var path=npc.getNavigation().createPath(BlockPos.containing(approach),0,64);
            if(path==null || !path.canReach()){
                npc.navigationMemory.reject(npc,lunch.getBlockPos(),approach);
                failed=lunch.getBlockPos();failedUntil=npc.level().getGameTime()+200;npc.report.lunch(CompanionStatus.BLOCKED);deadline=0;return;
            }
            npc.getNavigation().moveTo(path,1);travel.started(npc,approach);
        }
    }
}
