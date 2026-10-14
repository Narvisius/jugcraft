package local.peepo;

import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.sounds.*;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.*;

/** Loaded, claimed hive harvesting. Smoke and real tools/bottles are required at completion. */
public final class HiveCompanionJob implements CompanionJob {
    private final BeehiveBlockEntity hive;
    private final VanillaCompanionPort port;
    private int progress;
    private boolean comb;
    private List<ItemStack> pending;
    private UUID worker;
    private Vec3 entrance;
    private long lease,nextSpace;
    private float checkedHeight;
    private long nextClearance;
    public HiveCompanionJob(BeehiveBlockEntity hive,VanillaCompanionPort port){this.hive=hive;this.port=port;}
    public HiveCompanionJob prepare(PeepoEntity npc){
        expire();long now=npc.level().getGameTime();
        if(worker==null && (now>=nextSpace || checkedHeight!=npc.getBbHeight())){
            nextSpace=now+20;checkedHeight=npc.getBbHeight();entrance=null;
            for(var side:Direction.Plane.HORIZONTAL)for(int dy=0;dy>=-1;dy--){
                var p=Vec3.atBottomCenterOf(stationPosition().relative(side).offset(0,dy,0));
                if(npc.navigationMemory.failed(npc,stationPosition(),p))continue;
                var floor=BlockPos.containing(p).below();
                if(!npc.level().hasChunkAt(floor) || !npc.level().getBlockState(floor).isFaceSturdy(npc.level(),floor,Direction.UP))continue;
                if(!CompanionHazards.safeAt(npc,p) || !npc.level().noCollision(new AABB(p.x-.24,p.y,p.z-.24,p.x+.24,p.y+npc.getBbHeight(),p.z+.24)))continue;
                if(entrance==null || npc.position().distanceToSqr(p)<npc.position().distanceToSqr(entrance))entrance=p;
            }
        }
        return this;
    }
    private PeepoEntity occupant(){return hive.getLevel() instanceof ServerLevel l && worker!=null && l.getEntity(worker) instanceof PeepoEntity p?p:null;}
    private void expire(){if(worker!=null && (hive.isRemoved() || occupant()==null || !occupant().isAlive() || hive.getLevel().getGameTime()>lease))removed();}
    public void removed(){var p=occupant();if(p!=null)p.setWorkAnimation(WorkAnimation.NONE,stationPosition());worker=null;nextSpace=0;progress=0;pending=null;}
    public boolean timeCritical(){return progress>0;}
    public WorkAnimation animation(){return comb?WorkAnimation.HIVE_SHEAR:WorkAnimation.HIVE_BOTTLE;}
    public static boolean ready(net.minecraft.world.level.block.entity.BlockEntity block){
        return !block.isRemoved() && block.getLevel() instanceof ServerLevel l && l.hasChunkAt(block.getBlockPos())
            && block.getBlockState().getBlock() instanceof BeehiveBlock && block.getBlockState().hasProperty(BeehiveBlock.HONEY_LEVEL) && block.getBlockState().getValue(BeehiveBlock.HONEY_LEVEL)>=5
            && l.getGameRules().get(GameRules.MOB_GRIEFING) && CampfireBlock.isSmokeyPos(l,block.getBlockPos());
    }
    public static boolean needsShears(PeepoEntity npc,CompanionAssignments.Target target){
        if(target==null || !target.present(npc.level()))return false;
        var block=npc.level().getBlockEntity(target.at().pos());return block instanceof BeehiveBlockEntity && ready(block) && VanillaCompanionPort.of(block).honeycomb();
    }
    CompanionStatus readiness(){
        if(hive.isRemoved() || !(hive.getLevel() instanceof ServerLevel l) || !l.hasChunkAt(stationPosition()))return CompanionStatus.MISSING;
        if(!l.getGameRules().get(GameRules.MOB_GRIEFING))return CompanionStatus.GARDEN_DISABLED;
        if(!(hive.getBlockState().getBlock() instanceof BeehiveBlock))return CompanionStatus.MISSING;
        if(!hive.getBlockState().hasProperty(BeehiveBlock.HONEY_LEVEL) || hive.getBlockState().getValue(BeehiveBlock.HONEY_LEVEL)<5)return CompanionStatus.HONEY_GROWING;
        if(!CampfireBlock.isSmokeyPos(l,stationPosition()))return CompanionStatus.NEEDS_SMOKE;
        if(!port.honeycomb() && port.hiveBottles()==0)return CompanionStatus.NEEDS_BOTTLES;
        var products=List.of(new ItemStack(port.honeycomb()?Items.HONEYCOMB:Items.HONEY_BOTTLE,port.honeycomb()?3:1));
        return port.harvest(products,false)?CompanionStatus.READY:CompanionStatus.FULL;
    }
    public Kind kind(){return Kind.WORK;}
    public void approachFailed(PeepoEntity npc){nextSpace=nextClearance=0;entrance=null;}
    public BlockPos stationPosition(){return hive.getBlockPos();}
    public Vec3 approachPosition(){return entrance==null?Vec3.atBottomCenterOf(stationPosition()):entrance;}
    public boolean availableTo(PeepoEntity p){expire();return !hive.isRemoved() && (worker==null || worker.equals(p.getUUID()));}
    public boolean isOccupant(PeepoEntity p){return worker!=null && worker.equals(p.getUUID()) && entrance!=null && p.position().distanceToSqr(entrance)<.64;}
    public CompanionStatus workStatus(PeepoEntity p){return status(p,false);}
    public CompanionStatus planningFacts(PeepoEntity p){return status(p,true);}
    private CompanionStatus status(PeepoEntity p,boolean planning){
        if(!availableTo(p))return CompanionStatus.OCCUPIED;
        if(!p.orders.tamed() || !p.assignments.assignedWork(stationPosition()) || !CompanionJobs.permitted(p,stationPosition()))return CompanionStatus.FORBIDDEN;
        if(entrance==null)return CompanionStatus.BLOCKED;
        if(p.level().getGameTime()>=nextClearance){
            nextClearance=p.level().getGameTime()+20;
            var floor=BlockPos.containing(entrance).below();
            if(!p.level().hasChunkAt(floor) || !p.level().getBlockState(floor).isFaceSturdy(p.level(),floor,Direction.UP)
                || !CompanionHazards.safeAt(p,entrance) || !p.level().noCollision(new AABB(entrance.x-.24,entrance.y,entrance.z-.24,entrance.x+.24,entrance.y+p.getBbHeight(),entrance.z+.24))){entrance=null;nextSpace=0;return CompanionStatus.BLOCKED;}
        }
        var state=planning?CompanionReadiness.shared(hive,this::readiness):readiness();
        if(state==CompanionStatus.READY && port.honeycomb() && !p.belongings.getItem(9).is(Items.SHEARS))return CompanionStatus.NEEDS_SHEARS;
        return state;
    }
    public boolean claim(PeepoEntity p){if(workStatus(p)!=CompanionStatus.READY)return false;if(worker==null){progress=0;pending=null;comb=port.honeycomb();}worker=p.getUUID();lease=p.level().getGameTime()+100;return true;}
    public boolean occupy(PeepoEntity p){
        if(!isOccupant(p))return false;lease=p.level().getGameTime()+100;
        double dx=stationPosition().getX()+.5-entrance.x,dz=stationPosition().getZ()+.5-entrance.z;
        float yaw=(float)Math.toDegrees(Math.atan2(-dx,dz));p.setYRot(yaw);p.yBodyRot=yaw;p.setYHeadRot(yaw);return true;
    }
    public void release(PeepoEntity p){if(worker!=null && worker.equals(p.getUUID()))removed();}
    public CompanionStatus work(PeepoEntity p){
        if(!isOccupant(p) || !p.preferences.canWork() || !p.orders.station(this))return CompanionStatus.IDLE;
        var status=workStatus(p);if(status!=CompanionStatus.READY)return status;
        if(comb!=port.honeycomb()){progress=0;pending=null;comb=port.honeycomb();}
        var level=(ServerLevel)p.level();var tool=p.belongings.getItem(9);
        if(progress==59){
            if(pending==null){
                var drops=new ArrayList<ItemStack>();
                if(comb)Block.dropFromBlockInteractLootTable(level,BuiltInLootTables.HARVEST_BEEHIVE,stationPosition(),hive.getBlockState(),hive,tool,p,(l,item)->{if(drops.size()<33)drops.add(item.copy());});
                else drops.add(new ItemStack(Items.HONEY_BOTTLE));
                pending=drops;
            }
            if(pending.size()>32 || !port.harvest(pending,false))return CompanionStatus.FULL;
        }
        try(var tx=Transaction.openOuter()){if(p.extractEnergy(4,tx)!=4)return CompanionStatus.RECOVERING;tx.commit();}
        if(++progress<60)return CompanionStatus.WORKING;
        port.harvest(pending,true);
        ((BeehiveBlock)hive.getBlockState().getBlock()).resetHoneyLevel(level,hive.getBlockState(),stationPosition());
        if(comb){tool.hurtAndBreak(1,p,EquipmentSlot.MAINHAND);p.belongings.setChanged();}
        level.playSound(null,stationPosition(),comb?SoundEvents.BEEHIVE_SHEAR:SoundEvents.BOTTLE_FILL,SoundSource.BLOCKS,1,1);
        level.gameEvent(p,comb?GameEvent.SHEAR:GameEvent.FLUID_PICKUP,stationPosition());
        progress=0;pending=null;return CompanionStatus.WORKING;
    }
}
