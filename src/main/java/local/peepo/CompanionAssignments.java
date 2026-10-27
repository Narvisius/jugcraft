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
    public record Target(GlobalPos at,Identifier block,Direction face,List<BlockPos> plot,AnimalCareZone zone,GardenZone gardenZone,BlockPos fishingWater){
        public Target { if(fishingWater!=null)fishingWater=block.equals(CompanionFishing.ID) && CompanionFishing.validCast(at.pos(),fishingWater)?fishingWater.immutable():null;if(gardenZone!=null && (!GardenZone.valid(gardenZone.first(),gardenZone.last()) || !gardenZone.first().equals(at.pos()) || !block.equals(gardenZone.orchard()?GardenZone.ORCHARD:GardenZone.FARM)))gardenZone=null;plot=CompanionGarden.sanitize(at.pos(),plot);if(zone!=null && (!block.equals(CompanionLivestock.CARE) || !AnimalCareZone.valid(zone.first(),zone.last()) || !zone.first().equals(at.pos())))zone=null; }
        public Target(GlobalPos at,Identifier block,Direction face,List<BlockPos> plot,AnimalCareZone zone,GardenZone gardenZone){this(at,block,face,plot,zone,gardenZone,null);}
        public Target(GlobalPos at,Identifier block,Direction face,List<BlockPos> plot,AnimalCareZone zone){this(at,block,face,plot,zone,null);}
        public Target(GlobalPos at,Identifier block,Direction face,List<BlockPos> plot){this(at,block,face,plot,null);}
        public Target(GlobalPos at,Identifier block,Direction face){this(at,block,face,List.of());}
        public Target(GlobalPos at,Identifier block){this(at,block,Direction.UP);}
        public boolean garden(){return gardenZone!=null || !plot.isEmpty();}
        public boolean orchard(){return gardenZone!=null && gardenZone.orchard();}
        public boolean shearing(){return block.equals(CompanionLivestock.SHEARING);}
        public boolean milking(){return block.equals(CompanionLivestock.MILKING);}
        public boolean eggs(){return block.equals(CompanionLivestock.EGGS);}
        public boolean care(){return block.equals(CompanionLivestock.CARE);}
        public boolean task(int task){return care()?zone!=null && zone.enabled(task):task==AnimalCareZone.SHEAR?shearing():task==AnimalCareZone.MILK?milking():task==AnimalCareZone.EGGS && eggs();}
        public boolean livestock(){return shearing() || milking() || eggs() || care();}
        public AABB animalBox(){return zone!=null?zone.box():new AABB(at.pos().above()).inflate(CompanionLivestock.RADIUS,3,CompanionLivestock.RADIUS);}
        public boolean animalContains(Vec3 point){return zone!=null?zone.contains(point):Math.abs(point.y-at.pos().getY()-1)<=3 && point.distanceToSqr(Vec3.atBottomCenterOf(at.pos().above()))<=CompanionLivestock.RADIUS*CompanionLivestock.RADIUS;}
        public boolean fishing(){return block.equals(CompanionFishing.ID);}
        public boolean water(){return block.equals(CompanionWater.WATER);}
        public boolean treeTap(){return block.equals(io.github.jimbozoomer.jugcraft.Jugcraft.id("tree_tap"));}
        public boolean batHouse(){return block.equals(io.github.jimbozoomer.jugcraft.Jugcraft.id("bat_house"));}
        public boolean carriesProduce(){return fishing() || garden() || livestock() || batHouse() || water() || treeTap();}
        public String name(){return fishing()?"Fishing":water()?"Water collection":gardenZone!=null?(orchard()?"Orchard":"Farmland")+" ("+gardenZone.width()+" x "+gardenZone.depth()+")":care()?"Livestock Management"+(zone==null?"":" ("+zone.width()+" x "+zone.depth()+")"):shearing()?"Shearing (8-block radius)":milking()?"Milking (8-block radius)":eggs()?"Egg collection (8-block radius)":garden()?(CompanionGarden.hanging(this)?"Herb drying (":CompanionGarden.mushrooms(this)?"Mushroom garden (":"Garden (")+plot.size()+" blocks)":BuiltInRegistries.BLOCK.getValue(block).getName().getString();}
        public boolean local(Level level){return at.dimension().equals(level.dimension());}
        public boolean present(Level level){return local(level)&&level.hasChunkAt(at.pos())&&(!care() || zone!=null)&&(!fishing() || fishingWater!=null)&&(fishing() || water() || gardenZone!=null || livestock() || BuiltInRegistries.BLOCK.getKey(level.getBlockState(at.pos()).getBlock()).equals(block));}
    }
    private final PeepoEntity npc;
    public static final int LUNCH=5,SUPPLY=6,OUTPUT=7,COUNT=14;
    public static final int SUPPLY_COLOR=0xFF55AAFF,OUTPUT_COLOR=0xFFFFDD55;
    private final Target[] targets=new Target[COUNT];
    private final String[] names=new String[COUNT],containerNames=new String[COUNT];
    private String[] clientNames=new String[COUNT];
    private long nextLabelRefresh;
    public final CompanionSupplies supplies;
    public static int containerSlot(boolean output,int index){return SUPPLY+index*2+(output?1:0);}
    public static boolean supplySlot(int slot){return slot>=SUPPLY && slot<COUNT && (slot-SUPPLY)%2==0;}
    // Two bits per direction: Auto (0), On (1), Off (2). Kept separate from route identity.
    private final int[] transportModes=new int[COUNT];
    // Kept with the row through reorder/compaction, but outside transport route identity.
    private final Identifier[] gardenSeeds=new Identifier[COUNT];
    private final int[] gardenMaintenance=new int[COUNT];
    public static final int REPAIR=32,FERTILIZE=64;
    public boolean gardenMaintenance(int row,int task){if(npc.level().isClientSide())view();return row>=1 && row<=4 && (gardenMaintenance[row]&task)!=0;}
    boolean gardenMaintenance(Target target,int task){int row=workPriority(target.at.pos());return row<5 && target.equals(targets[row]) && gardenMaintenance(row,task);}
    public boolean toggleGardenMaintenance(int row,int task){
        if(npc.level().isClientSide() || row<1 || row>4 || task!=REPAIR && task!=FERTILIZE)return false;
        var target=targets[row];if(target==null || target.gardenZone()==null || target.orchard())return false;
        npc.garden.selectionChanged(target.at.pos());gardenMaintenance[row]^=task;changed();return true;
    }
    public net.minecraft.world.item.Item gardenSeed(Target target){
        if(target==null || !target.garden())return null;
        int row=workPriority(target.at.pos());
        return row<5 && target.equals(targets[row]) && gardenSeeds[row]!=null?BuiltInRegistries.ITEM.getValue(gardenSeeds[row]):null;
    }
    public boolean selectGardenSeed(int row,net.minecraft.world.item.ItemStack icon){
        if(npc.level().isClientSide() || row<1 || row>4 || targets[row]==null || !targets[row].garden() || !targets[row].present(npc.level()) || !CompanionJobs.permitted(npc,targets[row].at.pos()))return false;
        var seed=icon.isEmpty()?null:CompanionGarden.selectionSeed(icon);
        if(!icon.isEmpty() && (seed==null || !CompanionGarden.acceptsSeed(targets[row],new net.minecraft.world.item.ItemStack(seed))))return false;
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
        int mode=transportMode(row,supply);if(mode==2)return false;if(mode==1 || target.fishing())return true;
        if(target.present(npc.level()) && npc.level().getBlockEntity(target.at.pos()) instanceof io.github.jimbozoomer.jugcraft.machine.form.FormMachineBlockEntity form)return !form.companionAutomation.external(supply);
        return !target.present(npc.level()) || !(npc.level().getBlockEntity(target.at.pos()) instanceof MachineBlockEntity machine) || !machine.itemAutomation.external(supply);
    }
    /** Menu-only status: 0 available, 1 off, 2 external automation, 3 unsupported, 4 unavailable, 5 no recipe. */
    public int transportDisplay(int row,boolean supply){
        int mode=transportMode(row,supply);var target=targets[row];int reason=0;
        if(target==null || !target.present(npc.level()))reason=4;
        else if(target.fishing())reason=mode==2?1:0;
        else {
            var port=CompanionLogistics.resolve(npc,target);
            if(port==null)reason=3;
            else if(npc.level().getBlockEntity(target.at.pos()) instanceof io.github.jimbozoomer.jugcraft.machine.form.FormMachineBlockEntity form && (supply?!form.companionPort.supportsSupply():!form.companionPort.supportsOutput()))reason=3;
            else if(!target.livestock() && npc.level().getBlockEntity(target.at.pos()) instanceof MachineBlockEntity machine
                && (supply?!machine.companionPort.supportsSupply():!machine.companionPort.supportsOutput()))reason=3;
            else if(!supply && (npc.level().getBlockEntity(target.at.pos()) instanceof io.github.jimbozoomer.jugcraft.agriculture.FarmStandBlockEntity || ServingCompanion.of(npc.level().getBlockEntity(target.at.pos())) instanceof ServingCompanion serving && !serving.hasOutput()))reason=3;
            else if(supply && npc.level().getBlockEntity(target.at.pos()) instanceof io.github.jimbozoomer.jugcraft.concordance.garden.GleanerBlockEntity
                || !supply && npc.level().getBlockEntity(target.at.pos()) instanceof io.github.jimbozoomer.jugcraft.concordance.garden.MulchMawBlockEntity)reason=3;
            else if(supply && target.batHouse())reason=3;
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
    public String label(int row){
        var target=view().get(row);if(target==null)return "Not assigned";
        String name=npc.level().isClientSide()?clientNames[row]:row>=SUPPLY?containerNames[row]:names[row];
        return name==null || name.isBlank()?target.name():name;
    }
    public String customJobName(int row){return row>=1 && row<=4 && names[row]!=null?names[row]:"";}
    public boolean renameJob(int row,String name){if(npc.level().isClientSide() || row<1 || row>4 || targets[row]==null)return false;names[row]=CompanionNames.clean(name);changed();return true;}
    void refreshLabels(){
        long now=npc.level().getGameTime();if(npc.level().isClientSide() || now<nextLabelRefresh)return;nextLabelRefresh=now+20;boolean changed=false;
        for(int row=SUPPLY;row<COUNT;row++){var t=targets[row];String name=t==null?null:CompanionNames.container(npc.level(),t);if(!Objects.equals(name,containerNames[row])){containerNames[row]=name;changed=true;}}
        if(changed)changed();
    }
    public String assignFishing(BlockPos seat,BlockPos water){
        if(npc.level().isClientSide() || !CompanionFishing.validCast(seat,water))return "Choose water within 10 blocks and no more than 3 blocks below the seat.";
        if(!CompanionJobs.permitted(npc,seat) || !CompanionJobs.permitted(npc,water) || seat.distToCenterSqr(npc.position())>4096 || water.distToCenterSqr(npc.position())>4096)return "Keep both positions loaded, accessible and within 64 blocks of the companion.";
        if(!CompanionWater.source(npc.level(),water) || !CompanionFishing.safeSeat(npc,seat,water))return "Choose source water and a clear, dry shore/dock edge with room to cast.";
        var at=GlobalPos.of(npc.level().dimension(),seat);for(var t:targets)if(t!=null && t.at.equals(at))return "That seat is already assigned. Choose another block.";
        int row=1;while(row<5 && targets[row]!=null)row++;if(row==5)return "All four work slots are full. Remove a job first.";
        targets[row]=new Target(at,CompanionFishing.ID,Direction.UP,List.of(),null,null,water);
        transportModes[row]=0;gardenSeeds[row]=null;gardenMaintenance[row]=0;names[row]=null;workManaged=true;changed();npc.orders.assigned(false,at);
        return "Fishing assigned. Equip a rod or enable Tools on Supply; link Output for catches.";
    }
    public String assignZone(BlockPos first,BlockPos last){
        if(npc.level().isClientSide() || !AnimalCareZone.valid(first,last))return "Choose corners at most 16 x 16 blocks, with at most 3 blocks of ground-height difference.";
        if(!CompanionJobs.permitted(npc,first) || !CompanionJobs.permitted(npc,last) || first.distToCenterSqr(npc.position())>4096 || last.distToCenterSqr(npc.position())>4096)return "Keep both corners loaded, accessible and within 64 blocks of the companion.";
        var at=GlobalPos.of(npc.level().dimension(),first);for(var t:targets)if(t!=null && t.at.equals(at))return "This first corner is already assigned. Choose another corner.";
        int row=1;while(row<5 && targets[row]!=null)row++;if(row==5)return "All four work slots are full. Remove a job first.";
        targets[row]=new Target(at,CompanionLivestock.CARE,Direction.UP,List.of(),new AnimalCareZone(first,last,AnimalCareZone.DEFAULT_TASKS));transportModes[row]=0;gardenSeeds[row]=null;gardenMaintenance[row]=0;names[row]=null;workManaged=true;changed();npc.orders.assigned(false,at);
        return "Work "+row+" assigned: "+targets[row].name()+". Shearing, milking and eggs enabled.";
    }
    public String assignGardenZone(BlockPos first,BlockPos last,boolean orchard){
        if(npc.level().isClientSide() || !GardenZone.valid(first,last))return "Choose corners at most 16 x 16, with at most 3 blocks of ground-height difference.";
        if(!CompanionJobs.permitted(npc,first) || !CompanionJobs.permitted(npc,last) || first.distToCenterSqr(npc.position())>4096 || last.distToCenterSqr(npc.position())>4096)return "Keep both corners loaded, accessible and within 64 blocks of the companion.";
        var at=GlobalPos.of(npc.level().dimension(),first);for(var t:targets)if(t!=null && t.at.equals(at))return "This first corner is already assigned. Choose another corner.";
        int row=1;while(row<5 && targets[row]!=null)row++;if(row==5)return "All four work slots are full. Remove a job first.";
        targets[row]=new Target(at,orchard?GardenZone.ORCHARD:GardenZone.FARM,Direction.UP,List.of(),null,new GardenZone(first,last,orchard));
        transportModes[row]=0;gardenSeeds[row]=null;gardenMaintenance[row]=0;names[row]=null;workManaged=true;changed();npc.orders.assigned(false,at);
        return "Assigned: "+targets[row].name()+". "+(orchard?"Ripe fruit goes to linked Output.":"Link Supply for seeds and Output for harvests.");
    }
    public boolean toggleAnimalTask(int row,int task){
        if(npc.level().isClientSide() || row<1 || row>4 || task!=1 && task!=2 && task!=4 && task!=8 && task!=16)return false;var t=targets[row];if(t==null || !t.care() || t.zone==null)return false;
        npc.resetCompanionRoutine();targets[row]=new Target(t.at,t.block,t.face,t.plot,t.zone.withTasks(t.zone.tasks()^task));changed();return true;
    }
    public boolean population(int row,Identifier type,int target,int keep,boolean remove){
        if(npc.level().isClientSide() || row<1 || row>4)return false;var t=targets[row];if(t==null || !t.care() || t.zone==null)return false;
        if(!remove && t.zone.population(type)==null && t.zone.populations().size()>=AnimalCareZone.MAX_SPECIES)return false;
        npc.resetCompanionRoutine();targets[row]=new Target(t.at,t.block,t.face,t.plot,t.zone.withPopulation(type,target,keep,remove));changed();return true;
    }
    public boolean homeManaged(){return homeManaged;}
    public boolean workManaged(){return workManaged;}
    public static BlockPos canonical(Level level,BlockPos pos){
        var state=level.getBlockState(pos);
        if(state.getBlock() instanceof io.github.jimbozoomer.jugcraft.agriculture.HerbBundleBlock)return pos.above();
        if(state.getBlock() instanceof io.github.jimbozoomer.jugcraft.agriculture.MushroomColonyBlock)return pos.below();
        if(state.getBlock() instanceof CropBlock && CompanionGarden.farmland(level,pos.below()))return pos.below();
        if(state.getBlock() instanceof io.github.jimbozoomer.jugcraft.agriculture.TallCropBlock crop){
            var soil=crop.bottom(pos,state).below();if(CompanionGarden.farmland(level,soil))return soil;
        }
        if(state.getBlock() instanceof ChestBlock && state.getValue(ChestBlock.TYPE)==ChestType.RIGHT)return pos.relative(ChestBlock.getConnectedDirection(state));
        if(state.getBlock() instanceof io.github.jimbozoomer.jugcraft.agriculture.FarmStandBlock stand)return stand.masterPos(pos,state);
        if(state.getBlock() instanceof io.github.jimbozoomer.jugcraft.workshop.WorkshopBlock workshop)return workshop.master(pos,state);
        if(state.getBlock() instanceof WheelBlock)return WheelBlock.master(pos,state);
        if(state.getBlock() instanceof MachineBlock machine)return machine.masterPos(pos,state);
        if(state.getBlock() instanceof BedBlock && state.getValue(BlockStateProperties.BED_PART)==BedPart.FOOT)return pos.relative(state.getValue(BlockStateProperties.HORIZONTAL_FACING));
        return pos;
    }
    public static boolean bed(Level level,BlockPos pos){var b=level.getBlockState(pos).getBlock();return b instanceof CompanionBedBlock || b instanceof BedBlock;}
    public static boolean work(Level level,BlockPos pos){if(CompanionWater.source(level,pos) || level.getBlockState(pos).getBlock() instanceof io.github.jimbozoomer.jugcraft.agriculture.TreeTapBlock)return true;if(CompanionGarden.gardenAnchor(level,pos) || level.getBlockState(pos).getBlock() instanceof net.minecraft.world.level.block.ComposterBlock)return true;var be=level.getBlockEntity(pos);if(be instanceof io.github.jimbozoomer.jugcraft.machine.form.FormMachineBlockEntity form)return form.form().companionAssistance();return be instanceof CraftingTableCompanionBlockEntity || be instanceof io.github.jimbozoomer.jugcraft.workshop.WorkshopBlockEntity || be instanceof io.github.jimbozoomer.jugcraft.farming.SprinklerBlockEntity || ServingCompanion.of(be)!=null || MagicGardenCompanion.supports(be) || be instanceof io.github.jimbozoomer.jugcraft.agriculture.BatHouseBlockEntity || be instanceof io.github.jimbozoomer.jugcraft.agriculture.FarmStandBlockEntity || be instanceof io.github.jimbozoomer.jugcraft.concordance.CrucibleBlockEntity || VanillaCompanionPort.supports(be) || KitchenCompanionPort.of(be)!=null || be instanceof WheelBlockEntity || be instanceof MachineBlockEntity || be instanceof CookingPotBlockEntity || be instanceof io.github.jimbozoomer.jugcraft.agriculture.CiderPressBlockEntity || be instanceof io.github.jimbozoomer.jugcraft.agriculture.CiderBarrelBlockEntity || be instanceof io.github.jimbozoomer.jugcraft.agriculture.CandyKettleBlockEntity || be instanceof io.github.jimbozoomer.jugcraft.agriculture.SpinningWheelBlockEntity || be instanceof io.github.jimbozoomer.jugcraft.agriculture.CanningKettleBlockEntity || be instanceof io.github.jimbozoomer.jugcraft.agriculture.HearthOvenBlockEntity || be instanceof io.github.jimbozoomer.jugcraft.kinetic.HandCrankBlockEntity;}
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
        if(!CompanionJobs.permitted(npc,clicked) || !CompanionJobs.permitted(npc,pos))return "The companion owner cannot access this claim.";
        if(ServingCompanion.of(level.getBlockEntity(pos)) instanceof ServingCompanion serving && !serving.allowed(npc))return "This serving station does not allow this companion.";
        if(level.getBlockEntity(pos) instanceof io.github.jimbozoomer.jugcraft.agriculture.FarmStandBlockEntity stand && !npc.orders.foodAccess(stand.owner(),false))return "Only the Farm Stand owner's companion can restock it.";
        if(level.getBlockEntity(pos) instanceof io.github.jimbozoomer.jugcraft.concordance.garden.LivingDeviceBlockEntity device && MagicGardenCompanion.supports(device) && !MagicGardenCompanion.keeperAccess(npc,device.keeper()))return "Only the keeper or their party can assign this garden device.";
        boolean home=bed(level,pos);
        boolean lunch=level.getBlockEntity(pos) instanceof LunchBlockEntity;
        if(lunch && !((LunchBlockEntity)level.getBlockEntity(pos)).feeds(npc))return "This lunch source is locked or does not allow this companion.";
        if(!home && !lunch && !work(level,pos))return "Choose garden soil, a hanging herb bundle, bed, workstation, lunch crate, or lunch cover.";
        if(pos.distToCenterSqr(npc.position())>64*64)return "Keep assignments within 64 blocks of the companion.";
        var at=GlobalPos.of(level.dimension(),pos);
        for(var t:targets)if(t!=null && t.at.equals(at))return "Already assigned to this companion.";
        boolean garden=CompanionGarden.gardenAnchor(level,pos);
        if(CompanionGarden.farmland(level,pos))return "Use the Planner Farmland zone option and select two ground corners.";
        if(garden && gardenContains(pos))return "This position is already in an assigned garden plot.";
        int slot=0;
        if(lunch)slot=LUNCH;
        else if(!home){slot=1;while(slot<5 && targets[slot]!=null)slot++;if(slot==5)return "All four work slots are full. Left-click an assigned station to remove it.";}
        var plot=garden?CompanionGarden.discover(npc,pos):List.<BlockPos>of();
        if(garden && plot.isEmpty())return "No accessible garden positions here.";
        targets[slot]=new Target(at,BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()),garden && CompanionGarden.dryingAnchor(level,pos)?Direction.DOWN:Direction.UP,plot);
        transportModes[slot]=0;gardenSeeds[slot]=null;gardenMaintenance[slot]=0;
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
        var id=CompanionLivestock.assignmentType(animal);if(id==null)return "Choose a sheep, cow or chicken.";
        var at=GlobalPos.of(level.dimension(),pos);
        for(var t:targets)if(t!=null && t.at.equals(at))return "This point already has an assignment. Select an animal on another block.";
        int slot=1;while(slot<5 && targets[slot]!=null)slot++;
        if(slot==5)return "All four work slots are full. Remove a job first.";
        targets[slot]=new Target(at,id);transportModes[slot]=0;gardenSeeds[slot]=null;gardenMaintenance[slot]=0;workManaged=true;
        changed();npc.orders.assigned(false,at);
        return "Work "+slot+" assigned: "+targets[slot].name()+". "+(targets[slot].eggs()?"No tools needed; link Output to deliver collected eggs.":"Supply "+(targets[slot].shearing()?"shears in Hand or a Tools supply.":"empty buckets in cargo or Supply."));
    }
    public String remove(Level level,BlockPos clicked){
        var pos=canonical(level,clicked);var at=GlobalPos.of(level.dimension(),pos);
        for(int i=1;i<5;i++)if(targets[i]!=null && targets[i].livestock() && targets[i].local(level) && targets[i].at.pos().equals(clicked)){clear(i);return "Animal work assignment removed.";}
        for(int i=0;i<COUNT;i++)if(targets[i]!=null && (targets[i].at.equals(at) || targets[i].local(level) && (targets[i].plot.contains(pos) || targets[i].gardenZone!=null && (targets[i].gardenZone.first().equals(clicked) || targets[i].gardenZone.last().equals(clicked) || targets[i].gardenZone.last().equals(pos))))){clear(i);return i==0?"Home removed.":i==LUNCH?"Lunch source removed.":supplySlot(i)?"Supply removed.":i>=SUPPLY?"Output removed.":"Work assignment removed.";}
        return "This block is not assigned to the selected companion.";
    }
    public void clear(int slot){
        if(slot<0 || slot>=COUNT || targets[slot]==null)return;
        npc.garden.assignmentRemoved(targets[slot]);supplies.cleared(slot);targets[slot]=null;names[slot]=null;containerNames[slot]=null;transportModes[slot]=0;gardenSeeds[slot]=null;gardenMaintenance[slot]=0;if(slot>0 && slot<LUNCH)compactWork();changed();if(slot<LUNCH)npc.orders.assignmentRemoved(slot==0);
        if(slot==LUNCH)npc.report.lunch(CompanionStatus.READY);
    }
    private void compactWork(){
        int next=1;
        for(int i=1;i<5;i++)if(targets[i]!=null){supplies.moveRow(i,next);targets[next]=targets[i];transportModes[next]=transportModes[i];gardenSeeds[next]=gardenSeeds[i];gardenMaintenance[next]=gardenMaintenance[i];names[next]=names[i];next++;}
        while(next<5){targets[next]=null;transportModes[next]=0;gardenSeeds[next]=null;gardenMaintenance[next]=0;names[next]=null;next++;}
    }
    public boolean moveWork(int slot,int direction){
        if(npc.level().isClientSide() || slot<1 || slot>4 || Math.abs(direction)!=1)return false;
        int other=slot+direction;
        if(other<1 || other>4 || targets[slot]==null || targets[other]==null)return false;
        supplies.swapRows(slot,other);var swap=targets[slot];targets[slot]=targets[other];targets[other]=swap;
        int modes=transportModes[slot];transportModes[slot]=transportModes[other];transportModes[other]=modes;
        var name=names[slot];names[slot]=names[other];names[other]=name;
        int maintenance=gardenMaintenance[slot];gardenMaintenance[slot]=gardenMaintenance[other];gardenMaintenance[other]=maintenance;
        var seed=gardenSeeds[slot];gardenSeeds[slot]=gardenSeeds[other];gardenSeeds[other]=seed;
        changed();npc.orders.workReordered();return true;
    }
    public int workPriority(BlockPos pos){
        for(int i=1;i<5;i++)if(targets[i]!=null && targets[i].local(npc.level()) && targets[i].at.pos().equals(pos))return i;
        return 5;
    }
    boolean gardenContains(BlockPos pos){for(int i=1;i<5;i++)if(targets[i]!=null && targets[i].local(npc.level()) && (targets[i].plot.contains(pos) || targets[i].gardenZone!=null && targets[i].gardenZone.contains(pos)))return true;return false;}
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
        for(int i=0;i<COUNT;i++)if(targets[i]!=null){var child=out.child("Assignment"+i);if(targets[i].fishingWater!=null)child.store("FishingWater",BlockPos.CODEC,targets[i].fishingWater);child.putInt("GardenMaintenance",gardenMaintenance[i]);child.store("At",GlobalPos.CODEC,targets[i].at);child.putString("Block",targets[i].block.toString());child.putInt("Face",targets[i].face.ordinal());child.putInt("SupplyMode",transportMode(i,true));child.putInt("OutputMode",transportMode(i,false));if(targets[i].zone!=null)targets[i].zone.save(child);if(targets[i].gardenZone!=null)targets[i].gardenZone.save(child);if(names[i]!=null)child.putString("JobName",names[i]);if(targets[i].garden())child.store("Plot",BlockPos.CODEC.listOf(),targets[i].plot);if(gardenSeeds[i]!=null)child.store("GardenSeed",Identifier.CODEC,gardenSeeds[i]);}
    }
    public void load(ValueInput in){
        homeManaged=in.getBooleanOr("AssignedHomeManaged",false);workManaged=in.getBooleanOr("AssignedWorkManaged",false);
        Arrays.fill(targets,null);Arrays.fill(names,null);Arrays.fill(containerNames,null);nextLabelRefresh=0;
        Arrays.fill(transportModes,0);Arrays.fill(gardenSeeds,null);Arrays.fill(gardenMaintenance,0);
        for(int i=0;i<COUNT;i++){
            var child=in.child("Assignment"+i);if(child.isEmpty())continue;
            var at=child.get().read("At",GlobalPos.CODEC).orElse(null);var id=Identifier.tryParse(child.get().getStringOr("Block",""));
            if(at!=null && id!=null)targets[i]=new Target(at,id,Direction.values()[Math.clamp(child.get().getIntOr("Face",Direction.UP.ordinal()),0,5)],i>=1 && i<=4?child.get().read("Plot",BlockPos.CODEC.listOf()).orElse(List.of()):List.of(),AnimalCareZone.load(child.get()),i>=1 && i<=4?GardenZone.load(child.get()):null,child.get().read("FishingWater",BlockPos.CODEC).orElse(null));
            if(i>=1 && i<=4)names[i]=CompanionNames.clean(child.get().getStringOr("JobName",""));
            gardenMaintenance[i]=child.get().getIntOr("GardenMaintenance",0)&(REPAIR|FERTILIZE);
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
        for(int row=0;row<COUNT;row++){var t=targets[row];if(t==null){array.add(JsonNull.INSTANCE);continue;}var o=new JsonObject();if(t.fishingWater!=null)o.addProperty("fw",t.fishingWater.asLong());o.addProperty("gm",gardenMaintenance[row]);o.addProperty("d",t.at.dimension().identifier().toString());o.addProperty("p",t.at.pos().asLong());o.addProperty("b",t.block.toString());o.addProperty("f",t.face.ordinal());String label=row>=SUPPLY?containerNames[row]:names[row];if(label!=null)o.addProperty("n",label);if(t.zone!=null){o.addProperty("za",t.zone.first().asLong());o.addProperty("zb",t.zone.last().asLong());o.addProperty("zt",t.zone.tasks());var rules=new JsonArray();for(var rule:t.zone.populations()){var r=new JsonObject();r.addProperty("id",rule.type().toString());r.addProperty("target",rule.target());r.addProperty("keep",rule.keep());rules.add(r);}o.add("zp",rules);}if(t.gardenZone!=null){o.addProperty("ga",t.gardenZone.first().asLong());o.addProperty("gb",t.gardenZone.last().asLong());o.addProperty("go",t.gardenZone.orchard());}if(!t.plot.isEmpty()){var cells=new JsonArray();for(var cell:t.plot)cells.add(cell.asLong());o.add("g",cells);}array.add(o);}
        return array.toString();
    }
    public List<Target> view(){
        if(!npc.level().isClientSide())return Arrays.asList(targets);
        String data=npc.assignmentData();if(data.equals(cached))return clientTargets;cached=data;
        var list=new ArrayList<Target>(Collections.nCopies(COUNT,null));clientNames=new String[COUNT];Arrays.fill(gardenMaintenance,0);
        try{var array=JsonParser.parseString(data).getAsJsonArray();for(int i=0;i<Math.min(COUNT,array.size());i++)if(array.get(i).isJsonObject()){
            var o=array.get(i).getAsJsonObject();gardenMaintenance[i]=o.has("gm")?o.get("gm").getAsInt()&(REPAIR|FERTILIZE):0;var dim=ResourceKey.create(Registries.DIMENSION,Identifier.parse(o.get("d").getAsString()));
            var plot=new ArrayList<BlockPos>();if(o.has("g")){var cells=o.getAsJsonArray("g");for(int j=0;j<Math.min(CompanionGarden.PLOT_LIMIT,cells.size());j++)plot.add(BlockPos.of(cells.get(j).getAsLong()));}
            if(o.has("n"))clientNames[i]=CompanionNames.clean(o.get("n").getAsString());
            AnimalCareZone zone=null;if(o.has("za") && o.has("zb")){var a=BlockPos.of(o.get("za").getAsLong());var b=BlockPos.of(o.get("zb").getAsLong());if(AnimalCareZone.valid(a,b)){var rules=new ArrayList<AnimalCareZone.Population>();if(o.has("zp")){var ps=o.getAsJsonArray("zp");for(int j=0;j<Math.min(AnimalCareZone.MAX_SPECIES,ps.size());j++){var r=ps.get(j).getAsJsonObject();rules.add(new AnimalCareZone.Population(Identifier.parse(r.get("id").getAsString()),r.get("target").getAsInt(),r.get("keep").getAsInt()));}}zone=new AnimalCareZone(a,b,o.has("zt")?o.get("zt").getAsInt():7,rules);}}
            GardenZone gardenZone=null;if(o.has("ga") && o.has("gb")){var a=BlockPos.of(o.get("ga").getAsLong());var b=BlockPos.of(o.get("gb").getAsLong());if(GardenZone.valid(a,b))gardenZone=new GardenZone(a,b,o.has("go") && o.get("go").getAsBoolean());}
            list.set(i,new Target(GlobalPos.of(dim,BlockPos.of(o.get("p").getAsLong())),Identifier.parse(o.get("b").getAsString()),Direction.values()[Math.clamp(o.has("f")?o.get("f").getAsInt():Direction.UP.ordinal(),0,5)],plot,zone,gardenZone,o.has("fw")?BlockPos.of(o.get("fw").getAsLong()):null));
        }}catch(RuntimeException ignored){}clientTargets=Collections.unmodifiableList(list);return clientTargets;
    }
}
