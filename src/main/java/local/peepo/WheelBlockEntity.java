package local.peepo;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.Vec3;
import io.github.jimbozoomer.jugcraft.energy.*;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;

public final class WheelBlockEntity extends BlockEntity implements CompanionJob, net.minecraft.world.MenuProvider {
    private int outputRate;
    private final net.minecraft.world.inventory.ContainerData menuData = new net.minecraft.world.inventory.ContainerData() {
        public int get(int index) {
            var p = npc();
            return switch (index) {
                case 0 -> (int) energy.getAmount();
                case 1 -> working() ? 1 : 0;
                case 2 -> p == null ? 0 : (int) (100L * p.getEnergy() / p.getEnergyCapacity());
                case 3 -> outputRate;
                case 4 -> p == null ? 0 : p.isJughead() ? 2 : 1;
                case 5 -> p == null ? 0 : p.getId() & 0xffff;
                case 6 -> p == null ? 0 : (p.getId() >>> 16) & 0xffff;
                default -> 0;
            };
        }
        public void set(int index, int value) {}
        public int getCount() { return WheelMenu.DATA_COUNT; }
    };
    @Override public net.minecraft.network.chat.Component getDisplayName() {
        return net.minecraft.network.chat.Component.translatable("container.peepo_companion.wheel");
    }
    @Override public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id, net.minecraft.world.entity.player.Inventory inventory, net.minecraft.world.entity.player.Player player) {
        return new WheelMenu(id, inventory, menuData, net.minecraft.world.inventory.ContainerLevelAccess.create(level, worldPosition));
    }
    public final SimpleEnergyStorage energy=new SimpleEnergyStorage(32000,64,64,this::setChanged);
    public final EnergyStorage output=new EnergyStorage() {
        public boolean supportsInsertion(){return false;}
        public long insert(long n,TransactionContext tx){return 0;}
        public boolean supportsExtraction(){return true;}
        public long extract(long n,TransactionContext tx){return energy.extract(n,tx);}
        public long getAmount(){return energy.getAmount();}
        public long getCapacity(){return energy.getCapacity();}
    };
    private UUID occupant;
    private long lease;
    private boolean mounted;
    public float angle;
    public WheelBlockEntity(BlockPos pos,BlockState state){super(GeneratorWheel.ENTITY,pos,state);}
    public Direction facing(){return getBlockState().getValue(WheelBlock.FACING);}
    private Vec3 point(double x,double y,double z) {
        var right=facing().getClockWise();var back=facing().getOpposite();
        return new Vec3(worldPosition.getX()+.5+right.getStepX()*(x-.5)+back.getStepX()*(z-.5),
            worldPosition.getY()+y,worldPosition.getZ()+.5+right.getStepZ()*(x-.5)+back.getStepZ()*(z-.5));
    }
    private PeepoEntity npc(){return level instanceof ServerLevel s && occupant!=null && s.getEntity(occupant) instanceof PeepoEntity p?p:null;}
    private void expire(){if(occupant!=null && (level.getGameTime()>lease || npc()==null || !npc().isAlive())){var old=npc();if(old!=null)release(old);else{occupant=null;mounted=false;}}}
    public Kind kind(){return Kind.WHEEL;}
    public WorkAnimation animation(){return WorkAnimation.NONE;}
    public CompanionStatus workStatus(PeepoEntity p){return !availableTo(p)?CompanionStatus.OCCUPIED:energySpace()==0?CompanionStatus.FULL:CompanionStatus.READY;}
    public boolean worthStarting(PeepoEntity p){return workStatus(p)==CompanionStatus.READY && energySpace()>=640;}
    public CompanionStatus work(PeepoEntity p){return CompanionWork.transfer(p,this)>0?CompanionStatus.WORKING:workStatus(p);}
    public Vec3 approachPosition(){return mounted?point(1,5/16.0,.5):point(1,0,-.45);}
    public boolean availableTo(PeepoEntity p){expire();return !isRemoved() && (occupant==null || occupant.equals(p.getUUID()));}
    public boolean claim(PeepoEntity p){if(!availableTo(p))return false;occupant=p.getUUID();lease=level.getGameTime()+240;return true;}
    public boolean occupy(PeepoEntity p){
        if(!availableTo(p) || occupant==null)return false;
        lease=level.getGameTime()+20;mounted=true;
        var at=point(1,5/16.0,.5);float yaw=facing().getClockWise().toYRot();
        CompanionMotion.position(p,at,yaw);p.setDeltaMovement(Vec3.ZERO);p.getNavigation().stop();return true;
    }
    public void release(PeepoEntity p){
        if(occupant==null || !occupant.equals(p.getUUID()))return;
        if(mounted){var at=point(1,0,-.45);
            if(p.isCompanionUnloading())p.setBedExit(BlockPos.containing(at));
            else p.snapTo(at.x,at.y,at.z,p.getYRot(),0);
        }
        p.setWheelRunning(false);occupant=null;mounted=false;
    }
    public int energySpace(){return (int)(energy.getCapacity()-energy.getAmount());}
    public int insertEnergy(int n,TransactionContext tx){return (int)energy.insert(n,tx);}
    public boolean working(){var p=npc();return mounted && p!=null && p.isWheelRunning();}
    public static void tick(Level level,BlockPos pos,BlockState state,WheelBlockEntity wheel){
        if(level.isClientSide()){if(state.getValue(WheelBlock.RUNNING))wheel.angle=(wheel.angle+9)%360;return;}
        wheel.expire();
        Direction right=wheel.facing().getClockWise();
        long moved=EnergyNetworks.pushToNeighbors(level,pos,wheel.output,64,List.of(right.getOpposite()));
        moved+=EnergyNetworks.pushToNeighbors(level,pos.relative(right),wheel.output,64-moved,List.of(right));
        wheel.outputRate=(int)moved;
        boolean running=wheel.working();
        if(state.getValue(WheelBlock.RUNNING)!=running)level.setBlock(pos,state.setValue(WheelBlock.RUNNING,running),Block.UPDATE_ALL);
    }
    @Override protected void saveAdditional(ValueOutput out){super.saveAdditional(out);out.putLong("Energy",energy.getAmount());}
    @Override protected void loadAdditional(ValueInput in){super.loadAdditional(in);energy.setAmount(in.getLongOr("Energy",0));}
    @Override public void setRemoved(){var p=npc();if(p!=null)release(p);super.setRemoved();if(level!=null)EnergyNetworks.invalidate(level);}
}
