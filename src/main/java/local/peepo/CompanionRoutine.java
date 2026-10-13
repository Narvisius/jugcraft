package local.peepo;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Shared travel/claim/work/rest lifecycle. Productive behavior lives in CompanionJob adapters. */
final class CompanionRoutine extends Goal {
    private final PeepoEntity npc;
    private CompanionStation station;
    private BlockEntity block;
    private long nextLeisure,chairUntil,nextSearch,deadline,nextPriority;
    private long handoffUntil,sessionUntil,resumeUntil;
    private boolean active;
    private int repath;
    private final CompanionNavigation.Progress travel=new CompanionNavigation.Progress();
    private CompanionStatus state=CompanionStatus.IDLE;
    private final Map<BlockPos,Long> unreachable=new HashMap<>();
    CompanionRoutine(PeepoEntity npc){this.npc=npc;nextSearch=npc.level().getGameTime()+Math.floorMod(npc.getId(),80);setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
    void resetOrders(){release();active=false;nextSearch=0;}
    boolean isActive(){return active;}
    boolean atJob(BlockPos pos){return station instanceof CompanionJob && station.stationPosition().equals(pos);}
    /** Let a productive session finish before ordinary same/lower-priority hauling, at most 20 seconds. */
    boolean deferTransport(BlockPos pos){
        if(!active || !(station instanceof CompanionJob) || npc.level().getGameTime()>=sessionUntil
            || !npc.preferences.canWork() || !npc.orders.station(station))return false;
        if(state!=CompanionStatus.WORKING && state!=CompanionStatus.TRAVELLING && state!=CompanionStatus.IDLE && state!=CompanionStatus.WAITING)return false;
        int free=0;for(int i=0;i<8;i++)if(npc.belongings.getItem(i).isEmpty())free++;
        if(free<=1)return false;
        return npc.assignments.workPriority(pos)>=npc.assignments.workPriority(station.stationPosition());
    }
    CompanionStatus state(){return state;}
    CompanionStatus status(BlockPos pos){
        if(unreachable.getOrDefault(pos,0L)>npc.level().getGameTime())return CompanionStatus.BLOCKED;
        return station!=null && station.stationPosition().equals(pos)?state:null;
    }
    @Override public boolean requiresUpdateEveryTick(){return true;}
    private boolean needsRest(){return !npc.preferences.onShift() || npc.isRecovering() || npc.getEnergy()<npc.getEnergyCapacity()*95/100;}
    private boolean useful(CompanionStation s){
        if(!npc.orders.station(s) || !s.availableTo(npc))return false;
        if(s instanceof CompanionJob job)return npc.preferences.canWork() && CompanionJobs.permitted(npc,s.stationPosition())
            && job.planningStatus(npc)==CompanionStatus.READY;
        return switch(s.kind()){
            case CHAIR->needsRest() || npc.level().getGameTime()>=nextLeisure;
            case BED->needsRest() && npc.isRestNight();
            default->false;
        };
    }
    private boolean candidate(CompanionStation s){return useful(s) && CompanionHazards.safeAt(npc,s.approachPosition()) && (!(s instanceof CompanionJob job) || job.worthStarting(npc));}
    private int rank(CompanionStation s){return s instanceof CompanionJob?0:s.kind()==CompanionStation.Kind.BED?1:2;}
    private int pathRange(){return npc.assignments.workManaged()||npc.assignments.homeManaged()?64:16;}
    private void blocked(BlockPos pos,long now){if(unreachable.size()>=32)unreachable.clear();unreachable.put(pos,now+200);state=CompanionStatus.BLOCKED;}
    private void rejected(CompanionStation s,long now){
        npc.navigationMemory.reject(npc,s.stationPosition(),s.approachPosition());
        s.approachFailed(npc);npc.readiness.clear();blocked(s.stationPosition(),now);
        if(s instanceof CompanionJob)unreachable.put(s.stationPosition(),now+20);
    }
    private CompanionStation resolve(BlockPos pos){
        var job=CompanionJobs.resolve(npc,pos);if(job!=null)return job;
        var be=npc.level().getBlockEntity(pos);return be instanceof CompanionStation s?s:AssignedVanillaBed.create(npc,pos);
    }
    private void adopt(CompanionStation s,long now){
        station=s;block=npc.level().getBlockEntity(s.stationPosition());deadline=now+600;chairUntil=now+600;
        nextPriority=now+100;repath=0;travel.reset();state=CompanionStatus.TRAVELLING;
        handoffUntil=0;resumeUntil=0;sessionUntil=now+400;
    }
    private void search(){
        long now=npc.level().getGameTime();if(now<nextSearch)return;
        // Match ground navigation's readiness: an airborne start is not a blocked
        // workstation. Do not consume the seat scan or search cooldown before landing.
        if(!npc.onGround() && !npc.isInLiquid() && !npc.isPassenger())return;
        if(!CompanionBudget.search(npc)){state=CompanionStatus.WAITING;return;}
        state=CompanionStatus.IDLE;
        nextSearch=now+80+Math.floorMod(npc.getId(),20);unreachable.entrySet().removeIf(e->e.getValue()<=now);
        List<CompanionStation> candidates=new ArrayList<>();
        for(var pos:npc.assignments.loadedStations()){
            var s=resolve(pos);if(s!=null && candidate(s) && !unreachable.containsKey(pos))candidates.add(s);
        }
        // An assigned productive job needs no ambient machine scan.
        if(candidates.isEmpty()){
            int cx=npc.blockPosition().getX()>>4,cz=npc.blockPosition().getZ()>>4;
            int reads=0;
            outer:for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++){
                var origin=new BlockPos((cx+dx)*16,npc.blockPosition().getY(),(cz+dz)*16);
                for(var pos:CompanionStationIndex.positions(npc.level(),origin)){
                    if(++reads>128 || candidates.size()>=24)break outer;
                    var s=resolve(pos);
                    if(s!=null && candidate(s) && npc.position().distanceToSqr(s.approachPosition())<=256 && !unreachable.containsKey(pos))candidates.add(s);
                }
            }
        }
        candidates.sort(Comparator.<CompanionStation>comparingInt(this::rank)
            .thenComparingInt(s->s instanceof CompanionJob && npc.assignments.workManaged()?npc.assignments.workPriority(s.stationPosition()):0)
            .thenComparingDouble(s->npc.position().distanceToSqr(s.approachPosition())));
        int attempts=0;
        for(var s:candidates){
            if(attempts++>=2)break;
            if(!CompanionBudget.path(npc)){nextSearch=now+1;state=CompanionStatus.WAITING;return;}
            var path=npc.getNavigation().createPath(BlockPos.containing(s.approachPosition()),0,pathRange());
            if(path==null || !path.canReach()){rejected(s,now);continue;}
            if(s.claim(npc)){adopt(s,now);npc.getNavigation().moveTo(path,1);travel.started(npc,s.approachPosition());repath=40;return;}
        }
        if(needsRest() || now>=nextLeisure){
            var surface=CompanionSeats.find(npc,unreachable);
            if(surface!=null && surface.claim(npc))adopt(surface,now);
        }
    }
    @Override public boolean canUse(){
        if(!npc.orders.routineAllowed() || npc.isEating() || npc.isWheelRunning() || npc.getRestMode()!=CompanionEnergy.Rest.NONE)return false;
        search();return station!=null || npc.isRecovering() || !npc.preferences.onShift();
    }
    @Override public boolean canContinueToUse(){return npc.orders.routineAllowed() && !npc.isEating()
        && (station!=null || npc.isRecovering() || !npc.preferences.onShift()
            || resumeUntil>npc.level().getGameTime() && npc.preferences.canWork());}
    @Override public void start(){active=true;}
    private void release(){
        if(station!=null)station.release(npc);
        npc.readiness.clear();
        travel.reset();
        handoffUntil=0;resumeUntil=0;
        station=null;block=null;npc.setWheelRunning(false);npc.setWorkAnimation(WorkAnimation.NONE,npc.blockPosition());npc.setRestMode(CompanionEnergy.Rest.NONE);npc.getNavigation().stop();state=CompanionStatus.IDLE;
    }
    @Override public void stop(){release();active=false;}
    /** Keep movement ownership across a short recipe/budget handoff, never across a real failure. */
    private boolean hold(CompanionStatus status,long now){
        if(status!=CompanionStatus.IDLE && status!=CompanionStatus.WAITING && status!=CompanionStatus.NO_INPUT
            && status!=CompanionStatus.NO_POWER && status!=CompanionStatus.NO_HEAT)return false;
        if(handoffUntil==0){
            handoffUntil=now+40;
            if(status==CompanionStatus.NO_INPUT || status==CompanionStatus.NO_HEAT)npc.transport.workChanged();
        }
        if(now>=handoffUntil)return false;
        state=status;npc.getNavigation().stop();npc.setWorkAnimation(WorkAnimation.NONE,npc.blockPosition());
        return true;
    }
    private void reconsiderWork(){
        if(station==null || !npc.assignments.workManaged() || !npc.preferences.canWork())return;
        long now=npc.level().getGameTime();if(now<nextPriority)return;
        // Optional idle rest must not hide newly ready work; recovery/off-shift rest stays protected.
        int priority=station instanceof CompanionJob?npc.assignments.workPriority(station.stationPosition()):5;
        if(priority<=1){nextPriority=now+100;return;}
        if(!CompanionBudget.search(npc))return;
        nextPriority=now+80+Math.floorMod(npc.getId(),20);
        int attempts=0;
        for(int i=1;i<priority;i++){
            var target=npc.assignments.get(i);if(target==null || !target.present(npc.level()) || unreachable.getOrDefault(target.at().pos(),0L)>now)continue;
            var job=CompanionJobs.resolve(npc,target.at().pos());if(job==null || !candidate(job))continue;
            // Ground navigation cannot plan a reliable route from a mounted seat/pot.
            // Keep MOVE ownership until grounded at the safe exit. Releasing the
            // goal here lets a stroll win while navigation still rejects the airborne start.
            if(!(station instanceof CompanionJob) || npc.isNoGravity()){
                release();resumeUntil=now+40;nextSearch=now;state=CompanionStatus.WAITING;return;
            }
            if(attempts++>=2)break;
            if(!CompanionBudget.path(npc)){nextPriority=now+1;return;}
            var path=npc.getNavigation().createPath(BlockPos.containing(job.approachPosition()),0,pathRange());
            if(path==null || !path.canReach()){rejected(job,now);continue;}
            if(job.claim(npc)){release();adopt(job,now);npc.getNavigation().moveTo(path,1);travel.started(npc,job.approachPosition());repath=40;return;}
        }
    }
    @Override public void tick(){
        long now=npc.level().getGameTime();
        if(station!=null && station.kind()==CompanionStation.Kind.CHAIR && !npc.isRecovering() && npc.preferences.onShift() && now>=chairUntil){nextLeisure=now+600;release();}
        if(station!=null && (!npc.level().hasChunkAt(station.stationPosition()) || block!=null && (block.isRemoved() || npc.level().getBlockEntity(block.getBlockPos())!=block)))release();
        if(station instanceof CompanionJob job){
            if(!npc.preferences.canWork() || !npc.orders.station(station) || !station.availableTo(npc) || !CompanionJobs.permitted(npc,station.stationPosition()))release();
            else {
                var readiness=job.planningStatus(npc);
                if(readiness!=CompanionStatus.READY){
                    if(hold(readiness,now)){reconsiderWork();return;}
                    npc.transport.workChanged();release();nextSearch=now;
                }else if(handoffUntil!=0){handoffUntil=0;deadline=now+600;repath=0;travel.reset();}
            }
        }else if(station!=null && !useful(station))release();
        if(station==null && resumeUntil!=0){
            if(now>=resumeUntil || !npc.preferences.canWork()){release();return;}
            if(!npc.onGround()){npc.getNavigation().stop();state=CompanionStatus.WAITING;return;}
        }
        if(station==null)search();
        reconsiderWork();
        if(station==null){npc.getNavigation().stop();return;}
        var target=station.approachPosition();
        if(npc.position().distanceToSqr(target)>.64){
            state=CompanionStatus.TRAVELLING;
            if(now>deadline){var pos=station.stationPosition();release();blocked(pos,now);return;}
            if(--repath<=0){
                if(!station.claim(npc)){release();return;}
                repath=40;
            }
            if(travel.needsPath(npc,target)){
                if(!CompanionBudget.path(npc)){state=CompanionStatus.WAITING;return;}
                npc.getNavigation().stop(); // A stuck route must not be returned by vanilla's same-target path cache.
                var path=npc.getNavigation().createPath(BlockPos.containing(target),0,pathRange());
                if(path==null || !path.canReach()){var old=station;rejected(old,now);release();state=CompanionStatus.BLOCKED;return;}
                npc.getNavigation().moveTo(path,1);travel.started(npc,target);
            }
            return;
        }
        if(!CompanionHazards.safeAt(npc,target)){var old=station;rejected(old,now);release();state=CompanionStatus.BLOCKED;return;}
        npc.getNavigation().stop();if(!station.occupy(npc)){release();return;}
        if(station instanceof CompanionJob job){
            npc.setRestMode(CompanionEnergy.Rest.NONE);state=job.work(npc);
            if(state!=CompanionStatus.WORKING){if(!hold(state,now)){npc.transport.workChanged();release();nextSearch=now;}}
            else npc.setWorkAnimation(job.animation(),job.animationTarget());
        }else{
            npc.setRestMode(station.kind()==CompanionStation.Kind.BED?CompanionEnergy.Rest.SLEEPING:CompanionEnergy.Rest.SITTING);state=CompanionStatus.RESTING;
        }
    }
}
