package local.peepo;

import java.util.*;
import com.google.gson.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;
import io.github.jimbozoomer.jugcraft.machine.*;
import io.github.jimbozoomer.jugcraft.agriculture.CookingPotBlockEntity;

/** Home, four jobs, lunch, supply and output links. No chunk loads. */
public final class CompanionAssignments {
    public record Target(GlobalPos at,Identifier block,Direction face,List<BlockPos> plot){
        public Target { plot=CompanionGarden.sanitize(at.pos(),plot); }
        public Target(GlobalPos at,Identifier block,Direction face){this(at,block,face,List.of());}
        public Target(GlobalPos at,Identifier block){this(at,block,Direction.UP);}
        public boolean garden(){return !plot.isEmpty();}
        public boolean shearing(){return block.equals(CompanionLivestock.SHEARING);}
        public boolean milking(){return block.equals(CompanionLivestock.MILKING);}
        public boolean livestock(){return shearing() || milking();}
        public boolean carriesProduce(){return garden() || livestock();}
        public String name(){return shearing()?"Shearing (8-block radius)":milking()?"Milking (8-block radius)":garden()?"Garden ("+plot.size()+" blocks)":BuiltInRegistries.BLOCK.getValue(block).getName().getString();}
        public boolean local(Level level){return at.dimension().equals(level.dimension());}
        public boolean present(Level level){return local(level)&&level.hasChunkAt(at.pos())&&(livestock() || BuiltInRegistries.BLOCK.getKey(level.getBlockState(at.pos()).getBlock()).equals(block));}
    }
    private final PeepoEntity npc;
    public static final int LUNCH=5,SUPPLY=6,OUTPUT=7,COUNT=14;
    public static final int SUPPLY_COLOR=0xFF55AAFF,OUTPUT_COLOR=0xFFFFDD55;
    private final Target[] targets=new Target[COUNT];
    public final CompanionSupplies supplies;
    public static int containerSlot(boolean output,int index){return SUPPLY+index*2+(output?1:0);}
    public static boolean supplySlot(int slot){return slot>=SUPPLY && slot<COUNT && (slot-SUPPLY)%2==0;}
    // Two bits per direction: Auto (0), On (1), Off (2). Kept separate from route identity.
    private final int[] transportModes=new int[COUNT];
    // Kept with the row through reorder/compaction, but outside transport route identity.
    private final Identifier[] gardenSeeds=new Identifier[COUNT];
    public net.minecraft.world.item.Item gardenSeed(Target target){
        if(target==null || !target.garden())return null;
        int row=workPriority(target.at.pos());
        return row<5 && target.equals(targets[row]) && gardenSeeds[row]!=null?BuiltInRegistries.ITEM.getValue(gardenSeeds[row]):null;
    }
    public boolean selectGardenSeed(int row,net.minecraft.world.item.ItemStack icon){
        if(npc.level().isClientSide() || row<1 || row>4 || targets[row]==null || !targets[row].garden() || !targets[row].present(npc.level()) || !CompanionJobs.permitted(npc,targets[row].at.pos()))return false;
        var seed=icon.isEmpty()?null:CompanionGarden.selectionSeed(icon);
        if(!icon.isEmpty() && seed==null)return false;
        gardenSeeds[row]=seed==null?null:BuiltInRegistries.ITEM.getKey(seed);
        npc.garden.selectionChanged(targets[row].at.pos());return true;
    }
    public int transportMode(int row,boolean supply){return row>=1 && row<=4?(transportModes[row]>>(supply?0:2))&3:0;}
    public boolean cycleTransport(int row,boolean supply){
        if(npc.level().isClientSide() || row<1 || row>4 || targets[row]==null)return false;
        int shift=supply?0:2;
        transportModes[row]=(transportModes[row]&~(3<<shift))|((transportMode(row,supply)+1)%3)<<shift;
        return true;
    }
    public boolean transportAllowed(Target target,boolean supply){
        if(target==null)return false;
        int row=workPriority(target.at.pos());if(row>4 || !target.equals(targets[row]))return false;
        int mode=transportMode(row,supply);if(mode==2)return false;if(mode==1)return true;
        return !target.present(npc.level()) || !(npc.level().getBlockEntity(target.at.pos()) instanceof MachineBlockEntity machine) || !machine.itemAutomation.external(supply);
    }
    /** Menu-only status: 0 available, 1 off, 2 external automation, 3 unsupported, 4 unavailable, 5 no recipe. */
    public int transportDisplay(int row,boolean supply){
        int mode=transportMode(row,supply);var target=targets[row];int reason=0;
        if(target==null || !target.present(npc.level()))reason=4;
        else {
            var port=CompanionLogistics.resolve(npc,target);
            if(port==null)reason=3;
            else if(!target.livestock() && npc.level().getBlockEntity(target.at.pos()) instanceof MachineBlockEntity machine
                && (supply?!machine.companionPort.selectable():machine.kind().outputSlot()>=machine.kind().slots))reason=3;
            else if(mode==2)reason=1;
            else if(!transportAllowed(target,supply))reason=2;
            else if(supply && port.plan()==null)reason=5;
        }
        return mode|(reason<<2);
    }
    private boolean homeManaged,workManaged;
    private String cached="";private List<Target> clientTargets=Collections.nCopies(COUNT,null);
    CompanionAssignments(PeepoEntity npc){this.npc=npc;supplies=new CompanionSupplies(npc);}
    public Target get(int slot){return targets[slot];}
    public boolean homeManaged(){return homeManaged;}
    public boolean workManaged(){return workManaged;}
    public static BlockPos canonical(Level level,BlockPos pos){
        var state=level.getBlockState(pos);
        if(state.getBlock() instanceof CropBlock && CompanionGarden.farmland(level,pos.below()))return pos.below();
        if(state.getBlock() instanceof io.github.jimbozoomer.jugcraft.agriculture.TallCropBlock crop){
            var soil=crop.bottom(pos,state).below();if(CompanionGarden.farmland(level,soil))return soil;
        }
        if(state.getBlock() instanceof ChestBlock && state.getValue(ChestBlock.TYPE)==ChestType.RIGHT)return pos.relative(ChestBlock.getConnectedDirection(state));
        if(state.getBlock() instanceof WheelBlock)return WheelBlock.master(pos,state);
        if(state.getBlock() instanceof MachineBlock machine)return machine.masterPos(pos,state);
        if(state.getBlock() instanceof BedBlock && state.getValue(BlockStateProperties.BED_PART)==BedPart.FOOT)return pos.relative(state.getValue(BlockStateProperties.HORIZONTAL_FACING));
        return pos;
    }
    public static boolean bed(Level level,BlockPos pos){var b=level.getBlockState(pos).getBlock();return b instanceof CompanionBedBlock || b instanceof BedBlock;}
    public static boolean work(Level level,BlockPos pos){if(CompanionGarden.farmland(level,pos))return true;var be=level.getBlockEntity(pos);return KitchenCompanionPort.of(be)!=null || be instanceof WheelBlockEntity || be instanceof MachineBlockEntity || be instanceof CookingPotBlockEntity || be instanceof io.github.jimbozoomer.jugcraft.agriculture.CiderPressBlockEntity || be instanceof io.github.jimbozoomer.jugcraft.agriculture.CanningKettleBlockEntity || be instanceof io.github.jimbozoomer.jugcraft.agriculture.HearthOvenBlockEntity || be instanceof io.github.jimbozoomer.jugcraft.kinetic.HandCrankBlockEntity;}
    public String assignContainer(Level level,BlockPos clicked,Direction face,boolean output){
        var pos=canonical(level,clicked);
        if(!level.hasChunkAt(pos) || pos.distToCenterSqr(npc.position())>64*64)return "Keep a loaded container within 64 blocks of the companion.";
        var target=new Target(GlobalPos.of(level.dimension(),pos),BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()),face);
        var storage=CompanionStorage.find(npc,target);
        if(storage==null || (output?!storage.supportsInsertion():!storage.supportsExtraction()))return "This container face is unavailable, locked, or does not support that transfer.";
        int previous=-1;
        for(int i=0;i<COUNT;i++)if(targets[i]!=null && targets[i].at.equals(target.at)){
            if(i<SUPPLY)return "This container already has another role for this companion.";
            previous=i;
        }
        if(previous>=0 && supplySlot(previous)!=output){targets[previous]=target;changed();return "Container face updated.";}
        int slot=-1;for(int i=0;i<4;i++)if(targets[containerSlot(output,i)]==null){slot=containerSlot(output,i);break;}
        if(slot<0)return "All four "+(output?"Output":"Supply")+" slots are full. Remove one first; existing links are kept.";
        if(previous>=0)clear(previous);
        targets[slot]=target;changed();
        return (output?"Output":"Supply")+" "+((slot-SUPPLY)/2+1)+" assigned: "+target.name()+(output?" (yellow).":" (blue).")+" Right-click to switch role; left-click to remove.";
    }
    /** New containers fill Supply; the same gesture switches only that container's role. */
    public String cycleContainer(Level level,BlockPos clicked,Direction face){
        var pos=canonical(level,clicked);var at=GlobalPos.of(level.dimension(),pos);
        for(int i=SUPPLY;i<COUNT;i++)if(targets[i]!=null && targets[i].at.equals(at))return assignContainer(level,pos,face,supplySlot(i));
        for(int i=0;i<4;i++)if(targets[containerSlot(false,i)]==null)return assignContainer(level,pos,face,false);
        return assignContainer(level,pos,face,true);
    }
    public String assign(Level level,BlockPos clicked){
        var pos=canonical(level,clicked);
        if(!level.hasChunkAt(pos))return "That target is not loaded.";
        boolean home=bed(level,pos);
        boolean lunch=level.getBlockEntity(pos) instanceof LunchBlockEntity;
        if(lunch && !((LunchBlockEntity)level.getBlockEntity(pos)).feeds(npc))return "This lunch source is locked or does not allow this companion.";
        if(!home && !lunch && !work(level,pos))return "Choose farmland, a bed, workstation, lunch crate, or lunch cover.";
        if(pos.distToCenterSqr(npc.position())>64*64)return "Keep assignments within 64 blocks of the companion.";
        var at=GlobalPos.of(level.dimension(),pos);
        for(var t:targets)if(t!=null && t.at.equals(at))return "Already assigned to this companion.";
        boolean garden=CompanionGarden.farmland(level,pos);
        if(garden && gardenContains(pos))return "This farmland is already in an assigned garden plot.";
        int slot=0;
        if(lunch)slot=LUNCH;
        else if(!home){slot=1;while(slot<5 && targets[slot]!=null)slot++;if(slot==5)return "All four work slots are full. Left-click an assigned station to remove it.";}
        var plot=garden?CompanionGarden.discover(npc,pos):List.<BlockPos>of();
        if(garden && plot.isEmpty())return "No accessible farmland here.";
        targets[slot]=new Target(at,BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()),Direction.UP,plot);
        transportModes[slot]=0;gardenSeeds[slot]=null;
        if(home)homeManaged=true;if(!lunch)workManaged=true;
        changed();
        if(lunch){npc.report.lunch(CompanionStatus.READY);return "Lunch source assigned: "+targets[slot].name();}
        npc.orders.assigned(home,at);
        return (home?"Home assigned: ":"Work "+slot+" assigned: ")+targets[slot].name()+(home || CompanionJobs.resolve(npc,pos)!=null || CompanionLogistics.resolve(npc,targets[slot])!=null?"":" (work behavior not implemented yet)");
    }
    public String assignAnimal(net.minecraft.world.entity.animal.Animal animal){
        var level=npc.level();var pos=animal.getOnPos();
        if(!level.hasChunkAt(pos) || pos.distToCenterSqr(npc.position())>64*64 || !CompanionJobs.permitted(npc,pos)
            || !CompanionJobs.permitted(npc,animal.blockPosition()))return "Choose an accessible animal within 64 blocks of the companion.";
        var id=CompanionLivestock.assignmentType(animal);if(id==null)return "Choose a sheep or cow.";
        var at=GlobalPos.of(level.dimension(),pos);
        for(var t:targets)if(t!=null && t.at.equals(at))return "This point already has an assignment. Select an animal on another block.";
        int slot=1;while(slot<5 && targets[slot]!=null)slot++;
        if(slot==5)return "All four work slots are full. Remove a job first.";
        targets[slot]=new Target(at,id);transportModes[slot]=0;gardenSeeds[slot]=null;workManaged=true;
        changed();npc.orders.assigned(false,at);
        return "Work "+slot+" assigned: "+targets[slot].name()+". Supply "+(targets[slot].shearing()?"shears in Hand or a Tools supply.":"empty buckets in cargo or Supply.");
    }
    public String remove(Level level,BlockPos clicked){
        var pos=canonical(level,clicked);var at=GlobalPos.of(level.dimension(),pos);
        for(int i=1;i<5;i++)if(targets[i]!=null && targets[i].livestock() && targets[i].local(level) && targets[i].at.pos().equals(clicked)){clear(i);return "Animal work assignment removed.";}
        for(int i=0;i<COUNT;i++)if(targets[i]!=null && (targets[i].at.equals(at) || targets[i].local(level) && targets[i].plot.contains(pos))){clear(i);return i==0?"Home removed.":i==LUNCH?"Lunch source removed.":supplySlot(i)?"Supply removed.":i>=SUPPLY?"Output removed.":"Work assignment removed.";}
        return "This block is not assigned to the selected companion.";
    }
    public void clear(int slot){
        if(slot<0 || slot>=COUNT || targets[slot]==null)return;
        supplies.cleared(slot);targets[slot]=null;transportModes[slot]=0;gardenSeeds[slot]=null;if(slot>0 && slot<LUNCH)compactWork();changed();if(slot<LUNCH)npc.orders.assignmentRemoved(slot==0);
        if(slot==LUNCH)npc.report.lunch(CompanionStatus.READY);
    }
    private void compactWork(){
        int next=1;
        for(int i=1;i<5;i++)if(targets[i]!=null){supplies.moveRow(i,next);targets[next]=targets[i];transportModes[next]=transportModes[i];gardenSeeds[next]=gardenSeeds[i];next++;}
        while(next<5){targets[next]=null;transportModes[next]=0;gardenSeeds[next]=null;next++;}
    }
    public boolean moveWork(int slot,int direction){
        if(npc.level().isClientSide() || slot<1 || slot>4 || Math.abs(direction)!=1)return false;
        int other=slot+direction;
        if(other<1 || other>4 || targets[slot]==null || targets[other]==null)return false;
        supplies.swapRows(slot,other);var swap=targets[slot];targets[slot]=targets[other];targets[other]=swap;
        int modes=transportModes[slot];transportModes[slot]=transportModes[other];transportModes[other]=modes;
        var seed=gardenSeeds[slot];gardenSeeds[slot]=gardenSeeds[other];gardenSeeds[other]=seed;
        changed();npc.orders.workReordered();return true;
    }
    public int workPriority(BlockPos pos){
        for(int i=1;i<5;i++)if(targets[i]!=null && targets[i].local(npc.level()) && targets[i].at.pos().equals(pos))return i;
        return 5;
    }
    boolean gardenContains(BlockPos pos){for(int i=1;i<5;i++)if(targets[i]!=null && targets[i].local(npc.level()) && targets[i].plot.contains(pos))return true;return false;}
    void changed(){npc.readiness.clear();npc.syncAssignments(encode());}
    public List<BlockPos> loadedStations(){
        var result=new ArrayList<BlockPos>(5);
        for(int i=0;i<5;i++){var t=targets[i];if(t!=null && t.present(npc.level()))result.add(t.at.pos());}
        return result;
    }
    public boolean assignedWork(BlockPos pos){for(int i=1;i<5;i++)if(targets[i]!=null && targets[i].present(npc.level()) && targets[i].at.pos().equals(pos))return true;return false;}
    public boolean foodNear(Vec3 point,int radius){for(var t:targets)if(t!=null && t.local(npc.level()) && t.at.pos().distToCenterSqr(point)<=radius*radius)return true;return false;}
    public Vec3 homeApproach(){return approach(targets[0]);}
    public Vec3 workApproach(){
        for(int i=1;i<5;i++){
            var t=targets[i];if(t==null || !t.present(npc.level()))continue;
            var job=CompanionJobs.resolve(npc,t.at.pos());
            if(job==null || !CompanionJobs.permitted(npc,t.at.pos()) || job.planningStatus(npc)!=CompanionStatus.READY || !npc.isUsingJobAt(t.at.pos()) && !job.worthStarting(npc))continue;
            return job.approachPosition();
        }
        return null;
    }
    private Vec3 approach(Target target){
        if(target==null || !target.present(npc.level()))return null;
        var pos=target.at.pos();var be=npc.level().getBlockEntity(pos);
        if(be instanceof CompanionBedEntity bed)return bed.entrance();
        if(be instanceof CompanionStation station)return station.approachPosition();
        var vanilla=AssignedVanillaBed.create(npc,pos);return vanilla==null?null:vanilla.approachPosition();
    }
    public void save(ValueOutput out){
        supplies.save(out);
        out.putBoolean("AssignedHomeManaged",homeManaged);out.putBoolean("AssignedWorkManaged",workManaged);
        for(int i=0;i<COUNT;i++)if(targets[i]!=null){var child=out.child("Assignment"+i);child.store("At",GlobalPos.CODEC,targets[i].at);child.putString("Block",targets[i].block.toString());child.putInt("Face",targets[i].face.ordinal());child.putInt("SupplyMode",transportMode(i,true));child.putInt("OutputMode",transportMode(i,false));if(targets[i].garden())child.store("Plot",BlockPos.CODEC.listOf(),targets[i].plot);if(gardenSeeds[i]!=null)child.store("GardenSeed",Identifier.CODEC,gardenSeeds[i]);}
    }
    public void load(ValueInput in){
        homeManaged=in.getBooleanOr("AssignedHomeManaged",false);workManaged=in.getBooleanOr("AssignedWorkManaged",false);
        Arrays.fill(targets,null);
        Arrays.fill(transportModes,0);Arrays.fill(gardenSeeds,null);
        for(int i=0;i<COUNT;i++){
            var child=in.child("Assignment"+i);if(child.isEmpty())continue;
            var at=child.get().read("At",GlobalPos.CODEC).orElse(null);var id=Identifier.tryParse(child.get().getStringOr("Block",""));
            if(at!=null && id!=null)targets[i]=new Target(at,id,Direction.values()[Math.clamp(child.get().getIntOr("Face",Direction.UP.ordinal()),0,5)],i>=1 && i<=4?child.get().read("Plot",BlockPos.CODEC.listOf()).orElse(List.of()):List.of());
            var savedSeed=child.get().read("GardenSeed",Identifier.CODEC).orElse(null);
            if(targets[i]!=null && targets[i].garden() && savedSeed!=null){
                var item=BuiltInRegistries.ITEM.getValue(savedSeed);
                if(item!=null && CompanionGarden.selectionSeed(new net.minecraft.world.item.ItemStack(item))==item)gardenSeeds[i]=savedSeed;
            }
            transportModes[i]=Math.clamp(child.get().getIntOr("SupplyMode",0),0,2)|(Math.clamp(child.get().getIntOr("OutputMode",0),0,2)<<2);
        }
        supplies.load(in);compactWork();changed();
    }
    private String encode(){
        var array=new JsonArray();
        for(var t:targets){if(t==null){array.add(JsonNull.INSTANCE);continue;}var o=new JsonObject();o.addProperty("d",t.at.dimension().identifier().toString());o.addProperty("p",t.at.pos().asLong());o.addProperty("b",t.block.toString());o.addProperty("f",t.face.ordinal());if(t.garden()){var cells=new JsonArray();for(var cell:t.plot)cells.add(cell.asLong());o.add("g",cells);}array.add(o);}
        return array.toString();
    }
    public List<Target> view(){
        if(!npc.level().isClientSide())return Arrays.asList(targets);
        String data=npc.assignmentData();if(data.equals(cached))return clientTargets;cached=data;
        var list=new ArrayList<Target>(Collections.nCopies(COUNT,null));
        try{var array=JsonParser.parseString(data).getAsJsonArray();for(int i=0;i<Math.min(COUNT,array.size());i++)if(array.get(i).isJsonObject()){
            var o=array.get(i).getAsJsonObject();var dim=ResourceKey.create(Registries.DIMENSION,Identifier.parse(o.get("d").getAsString()));
            var plot=new ArrayList<BlockPos>();if(o.has("g")){var cells=o.getAsJsonArray("g");for(int j=0;j<Math.min(CompanionGarden.PLOT_LIMIT,cells.size());j++)plot.add(BlockPos.of(cells.get(j).getAsLong()));}
            list.set(i,new Target(GlobalPos.of(dim,BlockPos.of(o.get("p").getAsLong())),Identifier.parse(o.get("b").getAsString()),Direction.values()[Math.clamp(o.has("f")?o.get("f").getAsInt():Direction.UP.ordinal(),0,5)],plot));
        }}catch(RuntimeException ignored){}clientTargets=Collections.unmodifiableList(list);return clientTargets;
    }
}
