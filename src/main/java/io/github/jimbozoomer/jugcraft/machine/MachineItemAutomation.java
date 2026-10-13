package io.github.jimbozoomer.jugcraft.machine;

import java.util.Iterator;
import net.fabricmc.fabric.api.transfer.v1.item.*;
import net.fabricmc.fabric.api.transfer.v1.storage.*;
import net.fabricmc.fabric.api.transfer.v1.transaction.*;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.core.*;
import net.minecraft.world.level.block.HopperBlock;
import io.github.jimbozoomer.jugcraft.logistics.*;

/** Per-machine, loaded-neighbor detection. Probes and aborted transactions never count as activity. */
public final class MachineItemAutomation extends SnapshotParticipant<Integer> {
    private final MachineBlockEntity machine;
    private final Storage<ItemVariant>[] ports;
    private int pending, connected, checkedSides=-1;
    private long lastInput=Long.MIN_VALUE,lastOutput=Long.MIN_VALUE,nextCheck;
    private static final ThreadLocal<Integer> COMPANION=ThreadLocal.withInitial(()->0);
    @SuppressWarnings("unchecked")
    public MachineItemAutomation(MachineBlockEntity machine){this.machine=machine;ports=new Storage[7];}
    /** Scope includes both transfers and transaction closure; restores nesting even after failures. */
    public static final class CompanionScope implements AutoCloseable {
        private CompanionScope(){COMPANION.set(COMPANION.get()+1);}
        public void close(){int depth=COMPANION.get()-1;if(depth==0)COMPANION.remove();else COMPANION.set(depth);}
    }
    public static CompanionScope companionTransfer(){return new CompanionScope();}
    public void outputMoved(long amount){if(amount>0 && COMPANION.get()==0 && machine.getLevel()!=null)lastOutput=machine.getLevel().getGameTime();}
    private void moved(boolean input,long amount,TransactionContext tx){
        if(amount<=0 || COMPANION.get()!=0)return;
        updateSnapshots(tx);pending|=input?1:2;
    }
    protected Integer createSnapshot(){return pending;}
    protected void readSnapshot(Integer snapshot){pending=snapshot;}
    protected void onFinalCommit(){
        if(machine.getLevel()!=null){long now=machine.getLevel().getGameTime();if((pending&1)!=0)lastInput=now;if((pending&2)!=0)lastOutput=now;}
        pending=0;
    }
    public Storage<ItemVariant> port(Direction side){
        int index=side==null?6:side.ordinal();
        if(ports[index]==null)ports[index]=observe(side);
        return ports[index];
    }
    private Storage<ItemVariant> observe(Direction side){return new Storage<>(){
        private Storage<ItemVariant> cached;
        private int config=-1;
        private Storage<ItemVariant> inner(){
            int now=machine.sides().pack()*8+machine.getBlockState().getValue(MachineBlock.FACING).ordinal();
            if(cached==null || config!=now){config=now;cached=ContainerStorage.of(machine,side);}
            return cached;
        }
        public boolean supportsInsertion(){
            if(side==null)return inner().supportsInsertion();
            for(int slot:machine.getSlotsForFace(side))if(slot<machine.kind().outputSlot() || machine.kind()==MachineKind.COAL_GENERATOR || machine.kind().isBoiler())return true;
            return false;
        }
        public boolean supportsExtraction(){
            if(side==null)return inner().supportsExtraction();
            for(int slot:machine.getSlotsForFace(side))if(machine.canTakeItemThroughFace(slot,machine.getItem(slot),side))return true;
            return false;
        }
        public long getVersion(){return inner().getVersion();}
        public long insert(ItemVariant item,long amount,TransactionContext tx){long moved=inner().insert(item,amount,tx);moved(true,moved,tx);return moved;}
        public long extract(ItemVariant item,long amount,TransactionContext tx){long moved=inner().extract(item,amount,tx);moved(false,moved,tx);return moved;}
        public Iterator<StorageView<ItemVariant>> iterator(){
            var it=inner().iterator();return new Iterator<>(){
                public boolean hasNext(){return it.hasNext();}
                public StorageView<ItemVariant> next(){var view=it.next();return new StorageView<>(){
                    public long extract(ItemVariant item,long amount,TransactionContext tx){long moved=view.extract(item,amount,tx);moved(false,moved,tx);return moved;}
                    public boolean isResourceBlank(){return view.isResourceBlank();}
                    public ItemVariant getResource(){return view.getResource();}
                    public long getAmount(){return view.getAmount();}
                    public long getCapacity(){return view.getCapacity();}
                    public StorageView<ItemVariant> getUnderlyingView(){return view.getUnderlyingView();}
                };}
            };
        }
    };}
    private boolean redstoneLoaded(BlockPos pos){
        for(var side:Direction.values())if(!machine.getLevel().hasChunkAt(pos.relative(side)))return false;
        return true;
    }
    public boolean external(boolean input){
        var level=machine.getLevel();if(level==null || level.isClientSide())return false;
        long now=level.getGameTime();int sides=machine.sides().pack();
        if(now>=nextCheck || checkedSides!=sides){
            nextCheck=now+40;checkedSides=sides;connected=0;
            var state=machine.getBlockState();var block=(MachineBlock)state.getBlock();
            var facing=state.getValue(MachineBlock.FACING);var footprint=block.footprint(state);
            for(int part=0;part<footprint.size();part++){
                var pos=footprint.partPos(machine.getBlockPos(),facing,part);
                if(!level.hasChunkAt(pos) || level.getBlockState(pos).getBlock()!=block)continue;
                for(var side:Direction.values()){
                    var adjacent=pos.relative(side);if(!level.hasChunkAt(adjacent))continue;
                    var neighbor=level.getBlockState(adjacent);
                    if(neighbor.getBlock()==block && block.masterPos(adjacent,neighbor).equals(machine.getBlockPos()))continue;
                    boolean takes=false,gives=false;
                    for(int slot:machine.getSlotsForFace(side)){
                        if(slot<machine.kind().outputSlot())takes=true;
                        if(slot>=machine.kind().outputSlot() && slot<machine.kind().slots)gives=true;
                    }
                    if(neighbor.getBlock() instanceof HopperBlock && neighbor.getValue(HopperBlock.ENABLED)){
                        if(takes && neighbor.getValue(HopperBlock.FACING)==side.getOpposite())connected|=1;
                        if(gives && side==Direction.DOWN)connected|=2;
                    }
                    if(gives && neighbor.getBlock() instanceof PneumaticExtractorBlock
                        && neighbor.getValue(PneumaticExtractorBlock.FACING)==side.getOpposite() && redstoneLoaded(adjacent) && !level.hasNeighborSignal(adjacent))connected|=2;
                    // Conveyors (including slopes/splitters) report their actual committed insertions.
                    // Passive pipes alone prove nothing. Their actual committed transfers are recorded above.
                    if(gives && machine.sides().eject() && !(neighbor.getBlock() instanceof ItemPipeBlock)){
                        if(neighbor.getBlock() instanceof MachineBlock mb && !level.hasChunkAt(mb.masterPos(adjacent,neighbor)))continue;
                        var destination=ItemStorage.SIDED.find(level,adjacent,side.getOpposite());
                        if(destination!=null && destination.supportsInsertion())connected|=2;
                    }
                }
            }
        }
        long last=input?lastInput:lastOutput;
        return (connected&(input?1:2))!=0 || last!=Long.MIN_VALUE && now>=last && now-last<200;
    }
}
