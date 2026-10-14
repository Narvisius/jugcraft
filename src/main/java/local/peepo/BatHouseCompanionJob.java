package local.peepo;

import java.util.UUID;
import io.github.jimbozoomer.jugcraft.agriculture.*;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.*;

/** Scoops a real ledge into marked companion cargo; no new inventory, bat scan or production ticker. */
public final class BatHouseCompanionJob implements CompanionJob {
    public static final int WORK_TICKS=40, ENERGY_PER_TICK=4;
    private final BatHouseBlockEntity house;
    private UUID worker,planner,spaceOwner;
    private Vec3 entrance;
    private long lease,nextSpace,nextRoom;
    private int progress,spaceCount;
    private float checkedHeight;
    private boolean room;
    public BatHouseCompanionJob(BatHouseBlockEntity house){this.house=house;}
    public Kind kind(){return Kind.WORK;}
    public BlockPos stationPosition(){return house.getBlockPos();}
    public Vec3 approachPosition(){return entrance==null?Vec3.atBottomCenterOf(stationPosition()):entrance;}
    public boolean timeCritical(){return progress>0;}
    private PeepoEntity occupant(){return house.getLevel() instanceof ServerLevel level && worker!=null && level.getEntity(worker) instanceof PeepoEntity p?p:null;}
    private void expire(){var p=occupant();if(worker!=null && (house.isRemoved() || p==null || !p.isAlive() || house.getLevel().getGameTime()>lease))removed();}
    public void removed(){var p=occupant();if(p!=null)p.setWorkAnimation(WorkAnimation.NONE,stationPosition());worker=null;entrance=null;progress=0;nextSpace=nextRoom=0;}
    private boolean live(PeepoEntity p){return house.getLevel()==p.level() && !house.isRemoved() && p.level().hasChunkAt(stationPosition()) && p.level().getBlockEntity(stationPosition())==house;}
    public BatHouseCompanionJob prepare(PeepoEntity p){
        expire();long now=p.level().getGameTime();
        if(live(p) && worker==null && (now>=nextSpace || !p.getUUID().equals(planner) || checkedHeight!=p.getBbHeight())){
            nextSpace=now+20;planner=p.getUUID();checkedHeight=p.getBbHeight();entrance=null;
            var front=house.getBlockState().getValue(BatHouseBlock.FACING);
            for(var side:new Direction[]{front,front.getClockWise(),front.getCounterClockWise()})for(int dy=0;dy>=-1;dy--){
                var point=Vec3.atBottomCenterOf(stationPosition().relative(side).offset(0,dy,0));
                if(p.navigationMemory.failed(p,stationPosition(),point) || !safe(p,point))continue;
                if(entrance==null || p.position().distanceToSqr(point)<p.position().distanceToSqr(entrance))entrance=point;
            }
        }
        return this;
    }
    private boolean safe(PeepoEntity p,Vec3 point){
        if(point==null || !live(p))return false;
        var feet=BlockPos.containing(point);var floor=feet.below();
        if(!p.level().hasChunkAt(floor) || !p.level().hasChunkAt(feet.above()) || !CompanionJobs.permitted(p,floor)
            || !p.level().getBlockState(floor).isFaceSturdy(p.level(),floor,Direction.UP) || !CompanionHazards.safeAt(p,point))return false;
        double r=p.getBbWidth()/2+.02;
        if(!p.level().noCollision(new AABB(point.x-r,point.y,point.z-r,point.x+r,point.y+p.getBbHeight(),point.z+r)))return false;
        var front=house.getBlockState().getValue(BatHouseBlock.FACING);
        var ledge=Vec3.atCenterOf(stationPosition()).add(front.getStepX()*.3,-.25,front.getStepZ()*.3);
        var eye=point.add(0,p.getEyeHeight(),0);
        if(eye.distanceToSqr(ledge)>6.25)return false;
        var hit=p.level().clip(new ClipContext(eye,ledge,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));
        return hit.getType()==HitResult.Type.MISS || hit.getBlockPos().equals(stationPosition());
    }
    public void approachFailed(PeepoEntity p){if(worker==null || worker.equals(p.getUUID())){entrance=null;nextSpace=0;}}
    public boolean availableTo(PeepoEntity p){expire();return live(p) && (worker==null || worker.equals(p.getUUID()));}
    private boolean fits(PeepoEntity p){try(var tx=Transaction.openOuter()){return p.garden.storeHarvest(new ItemStack(JugcraftAgriculture.item("bat_guano"),house.guano()),GlobalPos.of(p.level().dimension(),stationPosition()),tx);}}
    public CompanionStatus workStatus(PeepoEntity p){
        if(!live(p))return CompanionStatus.MISSING;
        if(!availableTo(p))return CompanionStatus.OCCUPIED;
        if(!p.orders.tamed() || !p.assignments.assignedWork(stationPosition()) || !CompanionJobs.permitted(p,stationPosition()))return CompanionStatus.FORBIDDEN;
        if(!JugcraftConfig.isFeatureEnabled(JugcraftAgriculture.FEATURE))return CompanionStatus.BAT_HOUSE_DISABLED;
        if(!(p.level() instanceof ServerLevel level) || !level.getGameRules().get(GameRules.MOB_GRIEFING))return CompanionStatus.GARDEN_DISABLED;
        if(house.guano()==0)return CompanionStatus.GUANO_WAIT;
        if(entrance==null)return CompanionStatus.BLOCKED;
        long now=level.getGameTime();
        if(now>=nextRoom || !p.getUUID().equals(spaceOwner) || spaceCount!=house.guano()){
            nextRoom=now+20;spaceOwner=p.getUUID();spaceCount=house.guano();room=fits(p);
        }
        return room?CompanionStatus.READY:CompanionStatus.FULL;
    }
    public boolean claim(PeepoEntity p){
        if(workStatus(p)!=CompanionStatus.READY)return false;
        if(worker==null)progress=0;worker=p.getUUID();lease=p.level().getGameTime()+100;return true;
    }
    public boolean occupy(PeepoEntity p){
        if(worker==null || !worker.equals(p.getUUID()) || entrance==null || p.position().distanceToSqr(entrance)>.64 || !safe(p,p.position()))return false;
        lease=p.level().getGameTime()+100;
        float yaw=(float)Math.toDegrees(Math.atan2(-(stationPosition().getX()+.5-p.getX()),stationPosition().getZ()+.5-p.getZ()));
        p.setYRot(yaw);p.yBodyRot=yaw;p.setYHeadRot(yaw);return true;
    }
    public void release(PeepoEntity p){if(worker!=null && worker.equals(p.getUUID()))removed();}
    public CompanionStatus work(PeepoEntity p){
        if(!p.preferences.canWork() || !p.orders.station(this) || !occupy(p))return CompanionStatus.IDLE;
        var status=workStatus(p);if(status!=CompanionStatus.READY)return status;
        boolean finishing=progress+1>=WORK_TICKS;
        try(var tx=Transaction.openOuter()){
            if(p.extractEnergy(ENERGY_PER_TICK,tx)!=ENERGY_PER_TICK)return CompanionStatus.RECOVERING;
            if(finishing){
                int count=house.guano();
                if(!p.garden.storeHarvest(new ItemStack(JugcraftAgriculture.item("bat_guano"),count),GlobalPos.of(p.level().dimension(),stationPosition()),tx))return CompanionStatus.FULL;
                if(!house.collectGuano(count,tx))return CompanionStatus.GUANO_WAIT;
            }
            tx.commit();
        }
        if(++progress<WORK_TICKS)return CompanionStatus.WORKING;
        progress=0;nextRoom=0;
        p.level().playSound(null,stationPosition(),SoundEvents.ROOTED_DIRT_BREAK,SoundSource.BLOCKS,.8F,1);
        p.level().gameEvent(p,GameEvent.BLOCK_CHANGE,stationPosition());
        p.transport.workChanged();p.readiness.clear();
        return CompanionStatus.GUANO_WAIT;
    }
}
