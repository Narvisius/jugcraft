package local.peepo;

import java.util.*;
import io.github.jimbozoomer.jugcraft.agriculture.*;
import io.github.jimbozoomer.jugcraft.concordance.garden.*;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.*;
import net.fabricmc.fabric.api.transfer.v1.transaction.*;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.core.*;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;

/** Saved garden plots and shared garden/livestock produce provenance in the existing eight cargo slots. */
public final class CompanionGarden extends SnapshotParticipant<CompanionGarden.Snapshot> {
    public static final int PLOT_LIMIT=8, WORK_TICKS=80, HOE_TICKS=40, ENERGY_PER_TICK=4;
    private static final Identifier PLAN=PeepoMod.id("garden");
    private final PeepoEntity npc;
    private final GardenMaintenance maintenance;
    private final Map<BlockPos,Job> jobs=new HashMap<>();
    private static final int SITE_LIMIT=256;
    private final Map<CompanionAssignments.Target,LinkedHashMap<BlockPos,BlockState>> plantingSites=new HashMap<>();
    private record Site(BlockPos pos,BlockState state){
        static final com.mojang.serialization.Codec<Site> CODEC=com.mojang.serialization.codecs.RecordCodecBuilder.create(i->i.group(
            BlockPos.CODEC.fieldOf("Pos").forGetter(Site::pos),BlockState.CODEC.fieldOf("Plant").forGetter(Site::state)).apply(i,Site::new));
    }
    private void pruneSites(){plantingSites.keySet().removeIf(t->{for(int row=1;row<=4;row++)if(t.equals(npc.assignments.get(row)))return false;return true;});}
    void assignmentRemoved(CompanionAssignments.Target target){if(target!=null){maintenance.removed(target);plantingSites.remove(target);var job=jobs.remove(target.at().pos());if(job!=null)job.release(npc);}}
    private BlockState remembered(CompanionAssignments.Target target,BlockPos pos){var sites=plantingSites.get(target);return sites==null?null:sites.get(pos);}
    private void remember(CompanionAssignments.Target target,BlockPos pos,BlockState state){
        if(target.gardenZone()==null || target.orchard() || !CompanionGardenCrops.supported(state) || CompanionGardenCrops.fruit(state) || !CompanionGardenCrops.base(npc.level(),pos,state))return;
        var sites=plantingSites.computeIfAbsent(target,t->new LinkedHashMap<>());
        if(sites.containsKey(pos) || sites.size()<SITE_LIMIT)sites.put(pos.immutable(),CompanionGardenCrops.start(state));
    }
    // Provenance only, not another inventory. Never export unmarked, player-given cargo.
    private final ItemStack[] harvest=new ItemStack[8];
    private final GlobalPos[] origins=new GlobalPos[8];
    record Snapshot(List<ItemStack> stacks,List<GlobalPos> origins){}
    private record Lease(UUID owner,long until){}
    private static final Map<Level,LinkedHashMap<BlockPos,Lease>> LEASES=new WeakHashMap<>();
    CompanionGarden(PeepoEntity npc){this.npc=npc;this.maintenance=new GardenMaintenance(npc);Arrays.fill(harvest,ItemStack.EMPTY);}

    /** Normalize only supported seeds/raw produce; never preserve cursor components or grant an item. */
    public static Item selectionSeed(ItemStack icon){
        if(icon.isEmpty())return null;
        if(seedItem(icon))return icon.getItem();
        if(icon.is(Items.PUMPKIN))return Items.PUMPKIN_SEEDS;
        if(icon.is(Items.MELON) || icon.is(Items.MELON_SLICE))return Items.MELON_SEEDS;
        if(icon.is(Items.WHEAT))return Items.WHEAT_SEEDS;
        if(icon.is(Items.BEETROOT))return Items.BEETROOT_SEEDS;
        for(var kind:TallCrop.values())if(icon.is(JugcraftAgriculture.item(kind.produceId)))return JugcraftAgriculture.item(kind.seedId);
        for(var entry:Map.of("flax","flax_seeds","cabbage","cabbage_seeds","oats","oat_seeds","barley","barley_seeds").entrySet()){
            var item=net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("jugcraft",entry.getKey()));
            if(item!=null && icon.is(item))return JugcraftAgriculture.item(entry.getValue());
        }
        return null;
    }
    void selectionChanged(BlockPos anchor){npc.resetCompanionRoutine();jobs.remove(anchor);}
    void inputsChanged(BlockPos anchor){var job=jobs.get(anchor);if(job!=null)job.nextScan=0;npc.readiness.clear();}
    private boolean accepts(CompanionAssignments.Target target,ItemStack seed){var selected=npc.assignments.gardenSeed(target);return acceptsSeed(target,seed) && (selected==null || seed.is(selected));}

    public static boolean farmland(Level level,BlockPos pos){return level.hasChunkAt(pos) && cropSoil(level.getBlockState(pos));}
    private static boolean cropSoil(BlockState state){return state.getBlock() instanceof FarmlandBlock || state.getBlock() instanceof RichFarmlandBlock || state.getBlock() instanceof PlanterBoxBlock || state.getBlock() instanceof VerdantBedBlock;}
    private static boolean mushroomSoil(BlockState state){return state.getBlock() instanceof RichSoilBlock || state.is(MushroomColonyBlock.GROWS_ON);}
    public static boolean hanging(CompanionAssignments.Target target){return target.garden() && target.face()==Direction.DOWN;}
    public static boolean mushrooms(CompanionAssignments.Target target){return target.gardenZone()==null && target.garden() && !hanging(target) && mushroomSoil(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(target.block()).defaultBlockState());}
    public static boolean cuttingTool(ItemStack stack){return stack.is(Items.SHEARS) || stack.is(JugcraftAgriculture.KNIVES);}
    static boolean acceptsSeed(CompanionAssignments.Target target,ItemStack seed){
        if(target.orchard())return false;
        boolean bundle=seed.getItem() instanceof BlockItem item && item.getBlock() instanceof HerbBundleBlock;
        boolean mushroom=seed.is(Items.BROWN_MUSHROOM) || seed.is(Items.RED_MUSHROOM);
        return hanging(target)?bundle:mushrooms(target)?mushroom:!bundle && !mushroom && seedItem(seed);
    }
    static boolean toolFits(CompanionAssignments.Target target,ItemStack stack){return !target.orchard() && !hanging(target) && (mushrooms(target)?cuttingTool(stack):stack.is(ItemTags.HOES));}
    public static BlockPos cropPosition(CompanionAssignments.Target target,BlockPos cell){return target.orchard()?cell:hanging(target)?cell.below():cell.above();}
    public static boolean dryingAnchor(Level level,BlockPos pos){return level.hasChunkAt(pos) && level.hasChunkAt(pos.below()) && level.getBlockState(pos.below()).getBlock() instanceof HerbBundleBlock && Block.canSupportCenter(level,pos,Direction.DOWN);}
    public static boolean gardenAnchor(Level level,BlockPos pos){return level.hasChunkAt(pos) && (farmland(level,pos) || mushroomSoil(level.getBlockState(pos)) || dryingAnchor(level,pos));}
    public boolean validCell(Level level,CompanionAssignments.Target target,BlockPos cell){
        if(!level.hasChunkAt(cell) || !level.hasChunkAt(cropPosition(target,cell)))return false;
        if(target.gardenZone()!=null && !target.gardenZone().contains(cell))return false;
        if(target.orchard())return level.getBlockState(cell).getBlock() instanceof FruitingLeavesBlock;
        if(target.gardenZone()!=null){
            var zone=target.gardenZone();var pos=cell.above();var state=level.getBlockState(pos);
            if(CompanionGardenCrops.supported(state) && CompanionGardenCrops.base(level,pos,state)
                && (zone.ground(cell) || state.is(Blocks.COCOA) || CompanionGardenCrops.vine(state)))return true;
            if(remembered(target,pos)!=null && (state.isAir() || state.is(Blocks.WATER)))return true;
            if(!zone.ground(cell))return false;
            if(maintenance.repairable(target,cell))return true;
            if(cropSoil(level.getBlockState(cell)) || level.getBlockState(cell).is(CranberryBushBlock.BOG_SOIL) && CranberryBushBlock.isShallowWater(level,pos))return true;
            var selected=npc.assignments.gardenSeed(target);
            if(selected!=null && state.isAir() && CompanionGardenCrops.neighborsLoaded(level,pos)){
                var plant=CompanionGardenCrops.seedState(new ItemStack(selected));
                if(plant!=null && plant.canSurvive(level,pos))return true;
            }
            return false;
        }
        return hanging(target)?Block.canSupportCenter(level,cell,Direction.DOWN):mushrooms(target)?mushroomSoil(level.getBlockState(cell)):cropSoil(level.getBlockState(cell));
    }
    static List<BlockPos> standingFloors(CompanionAssignments.Target target,BlockPos cell){
        var floors=new ArrayList<BlockPos>(10);
        for(int depth=target.orchard()?1:hanging(target)?2:target.gardenZone()!=null?-1:0;depth<=(target.orchard()?GardenZone.CANOPY_HEIGHT+1:hanging(target)?3:target.gardenZone()!=null?2:0);depth++){
            var center=cell.below(depth);floors.add(center);
            for(var side:Direction.Plane.HORIZONTAL)floors.add(center.relative(side));
        }
        return floors;
    }
    /** Orchard picking reaches an unobstructed cluster overhead; walking remains on safe solid ground. */
    public static boolean inReach(CompanionAssignments.Target target,BlockPos cell,Vec3 point){
        var pos=cropPosition(target,cell);if(!target.orchard())return pos.distToCenterSqr(point)<=6.25;
        double dx=pos.getX()+.5-point.x,dz=pos.getZ()+.5-point.z,dy=pos.getY()+.5-point.y;
        return dx*dx+dz*dz<=2.25 && dy>=0 && dy<=GardenZone.CANOPY_HEIGHT+.5;
    }
    /** Small rolling set for supply planning and path candidates, never an all-zone scan per item. */
    List<BlockPos> cells(CompanionAssignments.Target target){
        if(target.gardenZone()==null)return target.plot();
        var job=job(target.at().pos());if(job==null)return List.of();job.discoverCells();return List.copyOf(job.known);
    }
    /** Bound both the result and exploration; edges are horizontal, never diagonal or across missing chunks. */
    static List<BlockPos> discover(PeepoEntity npc,BlockPos anchor){
        boolean drying=dryingAnchor(npc.level(),anchor),fungi=!drying && mushroomSoil(npc.level().getBlockState(anchor));
        var found=new ArrayList<BlockPos>(PLOT_LIMIT);var queue=new ArrayDeque<BlockPos>();var seen=new HashSet<BlockPos>();
        queue.add(anchor);seen.add(anchor);
        while(!queue.isEmpty() && found.size()<PLOT_LIMIT){
            var pos=queue.removeFirst();
            if(!npc.level().hasChunkAt(pos) || !(drying?dryingAnchor(npc.level(),pos):fungi?mushroomSoil(npc.level().getBlockState(pos)):farmland(npc.level(),pos)) || !CompanionJobs.permitted(npc,pos) || !CompanionJobs.permitted(npc,drying?pos.below():pos.above()) || npc.assignments.gardenContains(pos)
                || pos.distToCenterSqr(npc.position())>64*64)continue;
            found.add(pos.immutable());
            for(var side:Direction.Plane.HORIZONTAL){var next=pos.relative(side);if(seen.add(next))queue.addLast(next);}
        }
        return List.copyOf(found);
    }
    /** Old assignments have no plot; malformed/disconnected save or sync data never widens a plot. */
    static List<BlockPos> sanitize(BlockPos anchor,List<BlockPos> cells){
        if(cells.isEmpty() || !cells.getFirst().equals(anchor))return List.of();
        var result=new ArrayList<BlockPos>(PLOT_LIMIT);
        for(int i=0;i<Math.min(PLOT_LIMIT,cells.size());i++){
            var pos=cells.get(i);
            if(pos.getY()!=anchor.getY() || result.contains(pos))continue;
            if(result.isEmpty() || result.stream().anyMatch(p->p.distManhattan(pos)==1))result.add(pos.immutable());
        }
        return List.copyOf(result);
    }
    public Job job(BlockPos anchor){
        int row=npc.assignments.workPriority(anchor);var target=row<5?npc.assignments.get(row):null;
        if(target==null || !target.garden() || !target.present(npc.level()))return null;
        pruneSites();
        jobs.entrySet().removeIf(e->!npc.assignments.assignedWork(e.getKey()));
        var job=jobs.get(anchor);
        if(job==null || !job.target.equals(target)){job=new Job(target);jobs.put(anchor,job);}
        return job;
    }
    private boolean enabled(){return npc.level() instanceof ServerLevel server && server.getGameRules().get(GameRules.MOB_GRIEFING);}
    boolean accessible(CompanionAssignments.Target target,BlockPos soil){return validCell(npc.level(),target,soil) && CompanionJobs.permitted(npc,soil) && CompanionJobs.permitted(npc,cropPosition(target,soil))
        && (!(npc.level().getBlockState(soil).getBlock() instanceof VerdantBedBlock) || MagicGardenCompanion.bedAvailable(npc,soil));}
    private boolean hoe(){return npc.belongings.getItem(9).is(ItemTags.HOES);}
    private boolean ripe(BlockPos pos,BlockState state){return ripe(state) || CompanionGardenCrops.ripe(npc.level(),pos,state);}
    private static boolean ripe(BlockState state){return state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state)
        || state.getBlock() instanceof TallCropBlock && state.getValue(TallCropBlock.SECTION)==0 && TallCropBlock.isRipe(state)
        || state.getBlock() instanceof MushroomColonyBlock && MushroomColonyBlock.isGrown(state)
        || state.getBlock() instanceof HerbBundleBlock && state.getValue(HerbBundleBlock.DRIED);}
    private BlockState planting(CompanionAssignments.Target target,ItemStack seed,BlockPos pos){
        if(seed.isEmpty() || !CompanionGardenCrops.neighborsLoaded(npc.level(),pos))return null;
        var current=npc.level().getBlockState(pos);BlockState planted=null;
        if(hanging(target)){
            if(seed.getItem() instanceof BlockItem item && item.getBlock() instanceof HerbBundleBlock)planted=item.getBlock().defaultBlockState();
        }else if(mushrooms(target)){
            var colony=JugcraftAgriculture.colony(seed.getItem());if(colony!=null)planted=colony.defaultBlockState();
        }else if(CompanionGardenCrops.seed(seed)){
            if(target.gardenZone()==null)return null;
            var learned=remembered(target,pos);
            if(learned!=null && seed.is(CompanionGardenCrops.seedFor(learned)))planted=learned;
            else if(npc.assignments.gardenSeed(target)==seed.getItem() && target.gardenZone().ground(pos.below()))planted=CompanionGardenCrops.seedState(seed);
        }else if(seed.getItem() instanceof BlockItem item && item.getBlock() instanceof CropBlock crop)planted=crop.getStateForAge(0);
        else for(var kind:TallCrop.values())if(seed.is(JugcraftAgriculture.item(kind.seedId))){
            planted=JugcraftAgriculture.TALL_CROPS.get(kind).defaultBlockState();break;
        }
        if(planted==null)return null;
        if(planted.getBlock() instanceof OrganismCropBlock magic && (!MagicGardenCompanion.bedAvailable(npc,pos.below()) || magic.organism()==null || !magic.organism().crop()))return null;
        boolean flooded=planted.getBlock() instanceof PaddyCropBlock || planted.getBlock() instanceof CranberryBushBlock;
        if(flooded){if(!current.is(Blocks.WATER) || !CranberryBushBlock.isShallowWater(npc.level(),pos))return null;}
        else if(planted.getBlock() instanceof TallCropBlock tall && tall.crop().trellis){if(!(current.getBlock() instanceof TrellisBlock))return null;}
        else if(!current.isAir())return null;
        return planted.canSurvive(npc.level(),pos)?planted:null;
    }
    private int seedSlot(CompanionAssignments.Target target,BlockPos pos){for(int i=0;i<8;i++)if(!npc.transport.reserved(i) && accepts(target,npc.belongings.getItem(i)) && planting(target,npc.belongings.getItem(i),pos)!=null)return i;return -1;}
    private static boolean seedItem(ItemStack stack){
        if(stack.is(Items.BROWN_MUSHROOM) || stack.is(Items.RED_MUSHROOM) || CompanionGardenCrops.seed(stack))return true;
        if(stack.getItem() instanceof BlockItem item && (item.getBlock() instanceof CropBlock || item.getBlock() instanceof HerbBundleBlock))return true;
        for(var kind:TallCrop.values())if(stack.is(JugcraftAgriculture.item(kind.seedId)))return true;
        return false;
    }
    private static Item seedFor(CropBlock crop){return crop instanceof JugcraftCropBlock jug?jug.seed():crop.asItem();}
    private int seedReserve(ItemStack stack){
        if(!seedItem(stack))return 0;
        int cells=0;for(int row=1;row<=4;row++){var t=npc.assignments.get(row);if(t!=null && t.garden() && t.local(npc.level()) && accepts(t,stack))cells+=t.gardenZone()==null?t.plot().size():Math.min(8,t.gardenZone().width()*t.gardenZone().depth());}
        return cells;
    }
    private int availableCount(ItemStack stack){int count=0;for(int i=0;i<8;i++)if(!npc.transport.reserved(i) && ItemStack.isSameItemSameComponents(stack,npc.belongings.getItem(i)))count+=npc.belongings.getItem(i).getCount();return count;}
    /** Keep an emergency seed buffer; player tools/food are never considered outputs. */
    ItemStack output(int slot,CompanionAssignments.Target target){
        if(slot<0 || slot>=8 || npc.transport.reserved(slot))return ItemStack.EMPTY;
        if(!target.at().equals(origins[slot]))return ItemStack.EMPTY;
        var held=npc.belongings.getItem(slot);var mark=harvest[slot];
        if(mark.isEmpty() || !ItemStack.isSameItemSameComponents(held,mark))return ItemStack.EMPTY;
        int n=Math.min(held.getCount(),mark.getCount());
        n=Math.min(n,Math.max(0,availableCount(held)-seedReserve(held)));
        return n>0?held.copyWithCount(n):ItemStack.EMPTY;
    }
    void collected(int slot,int amount){var mark=harvest[slot].copy();mark.shrink(amount);harvest[slot]=mark;if(mark.isEmpty())origins[slot]=null;}
    void playerEditedCargo(List<ItemStack> before){for(int i=0;i<8;i++)if(!ItemStack.matches(before.get(i),npc.belongings.getItem(i))){harvest[i]=ItemStack.EMPTY;origins[i]=null;}}
    void consumed(int slot,int count,TransactionContext tx){
        if(slot<0 || slot>=8 || harvest[slot].isEmpty() || npc.transport.reserved(slot))return;
        updateSnapshots(tx);collected(slot,count);
    }
    @Override protected Snapshot createSnapshot(){return new Snapshot(Arrays.stream(harvest).map(ItemStack::copy).toList(),new ArrayList<>(Arrays.asList(origins)));}
    @Override protected void readSnapshot(Snapshot saved){for(int i=0;i<8;i++){harvest[i]=saved.stacks.get(i);origins[i]=saved.origins.get(i);}}
    /** Only combine harvests with other marked harvests, leaving player-provided stacks alone. */
    boolean storeHarvest(ItemStack stack,GlobalPos origin,TransactionContext tx){
        if(stack.isEmpty())return true;updateSnapshots(tx);int remaining=stack.getCount();
        for(int pass=0;pass<2;pass++)for(int i=0;i<8 && remaining>0;i++){
            if(npc.transport.reserved(i))continue;
            var held=npc.belongings.getItem(i);boolean same=origin.equals(origins[i]) && !harvest[i].isEmpty() && ItemStack.isSameItemSameComponents(held,harvest[i]) && ItemStack.isSameItemSameComponents(held,stack);
            if(pass==0 && same){
                int n=Math.min(remaining,held.getMaxStackSize()-held.getCount());if(n<=0)continue;
                int marked=Math.min(harvest[i].getCount(),held.getCount());
                // CompanionFood owns snapshots of the actual inventory.
                if(npc.food.takeCargo(i,held,held.getCount(),tx)!=held.getCount())return false;
                if(!npc.food.putCargo(i,held.copyWithCount(held.getCount()+n),tx))return false;
                harvest[i]=stack.copyWithCount(marked+n);origins[i]=origin;remaining-=n;
            }else if(pass==1 && held.isEmpty()){
                int n=Math.min(remaining,stack.getMaxStackSize());if(!npc.food.putCargo(i,stack.copyWithCount(n),tx))return false;
                harvest[i]=stack.copyWithCount(n);origins[i]=origin;remaining-=n;
            }
        }
        return remaining==0;
    }
    public void save(ValueOutput out){
        maintenance.save(out);
        pruneSites();for(int row=1;row<=4;row++){
            var sites=plantingSites.get(npc.assignments.get(row));if(sites!=null)out.store("GardenSites"+row,Site.CODEC.listOf(),sites.entrySet().stream().map(e->new Site(e.getKey(),e.getValue())).toList());
        }
        for(int i=0;i<8;i++)if(!harvest[i].isEmpty() && origins[i]!=null){out.store("GardenHarvest"+i,ItemStack.CODEC,harvest[i]);out.store("GardenOrigin"+i,GlobalPos.CODEC,origins[i]);}}
    public void load(ValueInput in){
        maintenance.load(in);
        jobs.clear();plantingSites.clear();Arrays.fill(harvest,ItemStack.EMPTY);Arrays.fill(origins,null);
        for(int row=1;row<=4;row++){
            var target=npc.assignments.get(row);if(target==null || target.gardenZone()==null || target.orchard())continue;
            var sites=new LinkedHashMap<BlockPos,BlockState>();
            for(var site:in.read("GardenSites"+row,Site.CODEC.listOf()).orElse(List.of())){
                if(sites.size()>=SITE_LIMIT)break;
                if(target.gardenZone().contains(site.pos.below()) && CompanionGardenCrops.supported(site.state))sites.put(site.pos,CompanionGardenCrops.start(site.state));
            }
            if(!sites.isEmpty())plantingSites.put(target,sites);
        }
        for(int i=0;i<8;i++){
            var mark=in.read("GardenHarvest"+i,ItemStack.CODEC).orElse(ItemStack.EMPTY);var held=npc.belongings.getItem(i);
            var origin=in.read("GardenOrigin"+i,GlobalPos.CODEC).orElse(null);
            if(origin!=null && !mark.isEmpty() && ItemStack.isSameItemSameComponents(mark,held)){harvest[i]=mark.copyWithCount(Math.clamp(mark.getCount(),0,held.getCount()));origins[i]=origin;}
        }
    }

    /** The porter fetches seeds into its real cargo, then acknowledges them on arrival at the plot. */
    CompanionLogistics.Port port(CompanionAssignments.Target target){return new CompanionLogistics.Port(){
        public Identifier plan(){var seed=npc.assignments.gardenSeed(target);return seed==null?PLAN:net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(seed);}
        public int needed(ItemStack candidate){
            if(!enabled())return 0;
            if(GardenMaintenance.fertilizer(candidate)){
                if(!npc.assignments.gardenMaintenance(target,CompanionAssignments.FERTILIZE))return 0;
                boolean useful=false;for(var cell:cells(target))if(maintenance.useful(target,cropPosition(target,cell),candidate)){useful=true;break;}
                return useful?Math.max(0,GardenMaintenance.CARRY_LIMIT-maintenance.carried()):0;
            }
            if(!seedItem(candidate) || !accepts(target,candidate))return 0;
            int holes=0,carried=0;
            var compatible=new ArrayList<BlockPos>(PLOT_LIMIT);
            for(var soil:cells(target))if(accessible(target,soil) && planting(target,candidate,cropPosition(target,soil))!=null){holes++;compatible.add(cropPosition(target,soil));}
            if(holes==0)return 0;
            for(int i=0;i<8;i++)if(!npc.transport.reserved(i)){
                var stack=npc.belongings.getItem(i);
                if(!accepts(target,stack))continue;
                for(var pos:compatible)if(planting(target,stack,pos)!=null){carried+=stack.getCount();break;}
            }
            return Math.max(0,holes-carried);
        }
        public Storage<ItemVariant> inputs(){return new Storage<>(){
            public long insert(ItemVariant resource,long maxAmount,TransactionContext tx){
                if(resource.isBlank() || maxAmount<=0)return 0;var stack=resource.toStack(1);int amount=(int)Math.min(maxAmount,needed(stack));
                return amount<=0?0:npc.food.store(stack.copyWithCount(amount),tx);
            }
            public long extract(ItemVariant resource,long maxAmount,TransactionContext tx){return 0;}
            public boolean supportsExtraction(){return false;}
            public Iterator<StorageView<ItemVariant>> iterator(){return Collections.emptyIterator();}
        };}
        public Storage<ItemVariant> outputs(){return Storage.empty();}
    };}

    public final class Job implements CompanionJob {
        final CompanionAssignments.Target target;
        private BlockPos soil;
        private BlockState expected;
        private Vec3 approach;
        private int progress,cursor,discoveryCursor,action; // 0 harvest/plant, 1 repair, 2 fertilize
        private long nextDiscovery;
        private final ArrayList<BlockPos> known=new ArrayList<>(GardenZone.SCAN_SLICE);
        private boolean roundComplete;
        public boolean discoveryPending(){return target.gardenZone()!=null && !roundComplete;}
        private void discoverCells(){discoverCells(false);}
        private void discoverCells(boolean admitted){
            if(target.gardenZone()==null)return;
            long now=npc.level().getGameTime();if(now<nextDiscovery || !admitted && !CompanionBudget.search(npc))return;
            var zone=target.gardenZone();nextDiscovery=now+20;roundComplete=false;
            for(int n=0;n<Math.min(GardenZone.SCAN_SLICE,zone.size());n++){
                var cell=zone.cell(discoveryCursor++);if(discoveryCursor>=zone.size()){discoveryCursor=0;roundComplete=true;}
                if(CompanionJobs.permitted(npc,cell))maintenance.observe(target,cell);
                if(validCell(npc.level(),target,cell)){
                    if(accessible(target,cell))remember(target,cropPosition(target,cell),npc.level().getBlockState(cropPosition(target,cell)));
                    if(!known.contains(cell)){if(known.size()>=GardenZone.SCAN_SLICE)known.removeFirst();known.add(cell);}
                }else known.remove(cell);
                if(roundComplete)break;
            }
        }
        private long nextScan;
        private CompanionStatus status=CompanionStatus.IDLE;
        Job(CompanionAssignments.Target target){this.target=target;}
        public Kind kind(){return Kind.WORK;}
        public WorkAnimation animation(){
            if(!magicCell() && !target.orchard() && !hanging(target) && !mushrooms(target) && hoe() && expected!=null && (action==1 || action==0 && ripe(expected)))return progress>0?WorkAnimation.HARVEST:WorkAnimation.NONE;
            if(action!=0 || expected==null)return WorkAnimation.INTERACT;
            // Keep distant overhead work on its existing pose; ground picking uses a close reach.
            if(target.orchard() || hanging(target))return WorkAnimation.INTERACT;
            return expected.isAir() || expected.is(Blocks.WATER) || expected.getBlock() instanceof TrellisBlock?WorkAnimation.PLANT:WorkAnimation.PICK;
        }
        private boolean magicCell(){return soil!=null && npc.level().getBlockState(soil).getBlock() instanceof VerdantBedBlock;}
        boolean benefitsFromTool(){return action!=2 && !magicCell();}
        public BlockPos animationTarget(){return soil==null?stationPosition():animation()==WorkAnimation.PLANT || animation()==WorkAnimation.PICK?cropPosition(target,soil):hanging(target)?soil.below():soil;}
        public BlockPos stationPosition(){return target.at().pos();}
        public Vec3 approachPosition(){return approach==null?Vec3.atBottomCenterOf(stationPosition().above()):approach;}
        private boolean assigned(){int row=npc.assignments.workPriority(stationPosition());return row<5 && target.equals(npc.assignments.get(row)) && target.present(npc.level());}
        private Lease lease(){var leases=LEASES.get(npc.level());return soil==null || leases==null?null:leases.get(soil);}
        public boolean availableTo(PeepoEntity other){var lease=lease();return other==npc && assigned() && (lease==null || lease.until<=npc.level().getGameTime() || lease.owner.equals(npc.getUUID()));}
        private Vec3 standing(BlockPos cell){
            var options=new ArrayList<Vec3>(5);
            for(var floor:standingFloors(target,cell)){
                if(!hanging(target) && floor.equals(cell) && hoe() && ripe(npc.level().getBlockState(cell.above())))continue;
                addStanding(options,floor);
            }
            if(options.isEmpty() && !hanging(target) && !target.orchard())addStanding(options,cell);
            options.removeIf(point->!inReach(target,cell,point) || npc.navigationMemory.failed(npc,stationPosition(),point) || target.orchard() && !clearReach(cell,point) || !target.orchard() && !clearCropReach(cell,point));
            return options.stream().min(Comparator.comparingDouble(npc.position()::distanceToSqr)).orElse(null);
        }
        private boolean clearCropReach(BlockPos cell,Vec3 point){
            var pos=cropPosition(target,cell);var state=npc.level().getBlockState(pos);
            if(!CompanionGardenCrops.supported(state) && !(state.getBlock() instanceof PaddyCropBlock) && remembered(target,pos)==null)return true;
            var eye=point.add(0,npc.getEyeHeight(),0);var end=Vec3.atCenterOf(pos);
            // Aim near the water surface so a short companion on the bank does not raycast through its edge.
            if(state.getBlock() instanceof PaddyCropBlock || state.getBlock() instanceof CranberryBushBlock)end=end.add(0,.4,0);
            // The reach is already capped to 2.5 blocks. Verify the traversed neighborhood is loaded first.
            if(!CompanionGardenCrops.neighborsLoaded(npc.level(),pos))return false;
            var hit=npc.level().clip(new net.minecraft.world.level.ClipContext(eye,end,net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,npc));
            if(hit.getType()==HitResult.Type.MISS || hit.getBlockPos().equals(pos))return true;
            // A tall paddy's upper section is part of the crop being reached, not an intervening obstacle.
            var hitState=npc.level().getBlockState(hit.getBlockPos());
            return state.getBlock() instanceof PaddyCropBlock paddy && hitState.is(paddy)
                && paddy.bottom(hit.getBlockPos(),hitState).equals(pos);
        }
        private boolean clearReach(BlockPos fruit,Vec3 point){
            // Do not pick through trunks, floors or another canopy layer.
            for(int y=BlockPos.containing(point).getY()+1;y<fruit.getY();y++){
                var p=new BlockPos(fruit.getX(),y,fruit.getZ());if(!npc.level().hasChunkAt(p) || !npc.level().getBlockState(p).getCollisionShape(npc.level(),p).isEmpty())return false;
            }
            return true;
        }
        public void approachFailed(PeepoEntity other){release(other);nextScan=npc.level().getGameTime()+1;}
        private void addStanding(List<Vec3> points,BlockPos floor){
            if(!npc.level().hasChunkAt(floor) || !npc.level().hasChunkAt(floor.above()) || !CompanionJobs.permitted(npc,floor))return;
            var state=npc.level().getBlockState(floor);double top;
            if(state.getBlock() instanceof VerdantBedBlock)top=1;
            else if(cropSoil(state))top=.9375;
            else if(state.isFaceSturdy(npc.level(),floor,Direction.UP))top=1;else return;
            var at=new Vec3(floor.getX()+.5,floor.getY()+top,floor.getZ()+.5);double r=npc.getBbWidth()/2+.02;
            if(npc.level().getFluidState(floor.above()).isEmpty() && CompanionHazards.safeAt(npc,at) && npc.level().noCollision(new AABB(at.x-r,at.y+.001,at.z-r,at.x+r,at.y+npc.getBbHeight(),at.z+r)))points.add(new Vec3(at.x,floor.getY()+1,at.z));
        }
        private boolean current(){return soil!=null && accessible(target,soil) && npc.level().getBlockState(cropPosition(target,soil)).equals(expected)
            && (action==0 || action==1 && maintenance.repairable(target,soil) && maintenance.ready(target) || action==2 && maintenance.fertilizable(target,cropPosition(target,soil)));}
        public CompanionStatus workStatus(PeepoEntity other){
            if(!assigned())return CompanionStatus.MISSING;
            if(!enabled())return CompanionStatus.GARDEN_DISABLED;
            if(!npc.preferences.canWork())return CompanionStatus.RECOVERING;
            if(soil!=null){if(current())return availableTo(other)?CompanionStatus.READY:CompanionStatus.OCCUPIED;release(npc);}
            long now=npc.level().getGameTime();if(now<nextScan)return status;
            if(!CompanionBudget.search(npc))return CompanionStatus.WAITING;
            if(target.gardenZone()!=null)discoverCells(true);
            nextScan=now+80+Math.floorMod(npc.getId(),20);status=hanging(target)?CompanionStatus.DRYING:CompanionStatus.GROWING;
            // Legacy plots stay fixed; zones examine only the rolling bounded discovery window.
            var scan=target.gardenZone()==null?target.plot():List.copyOf(known);
            if(target.gardenZone()!=null && !roundComplete)nextScan=now+20;
            int approaches=0;
            search:
            for(int pass=0;pass<2;pass++)for(int n=0;n<scan.size();n++){
                var cell=scan.get((cursor+n)%scan.size());
                if(!npc.level().hasChunkAt(cell)){status=CompanionStatus.UNLOADED;continue;}
                if(!validCell(npc.level(),target,cell)){status=CompanionStatus.MISSING;continue;}
                if(!accessible(target,cell)){
                    status=npc.level().getBlockEntity(cell) instanceof VerdantBedBlockEntity bed
                        ?!Garden.enabled()?CompanionStatus.MAGIC_DISABLED:!bed.awake()?CompanionStatus.DORMANT_GARDEN:CompanionStatus.FORBIDDEN:CompanionStatus.FORBIDDEN;
                    continue;
                }
                var pos=cropPosition(target,cell);var state=npc.level().getBlockState(pos);
                int task=0;
                if(pass==0){
                    if(maintenance.repairable(target,cell))continue;
                    if(CompanionGardenCrops.fruit(state) && !fruitAllowed(pos,state))continue;
                    if(target.orchard() && !(state.getBlock() instanceof FruitingLeavesBlock && state.getValue(FruitingLeavesBlock.FRUIT)==FruitingLeavesBlock.RIPE))continue;
                    if(!target.orchard() && !ripe(pos,state)){
                        if(!state.isAir() && !state.is(Blocks.WATER) && !(state.getBlock() instanceof TrellisBlock))continue;
                        if(seedSlot(target,pos)<0){status=hanging(target)?CompanionStatus.NO_BUNDLES:CompanionStatus.NO_SEEDS;continue;}
                    }
                    if(state.getBlock() instanceof MushroomColonyBlock && ripe(state) && !cuttingTool(npc.belongings.getItem(9))){status=CompanionStatus.NEEDS_CUTTER;continue;}
                }else{
                    if(!maintenance.ready(target))continue;
                    if(maintenance.repairable(target,cell)){if(!hoe()){status=CompanionStatus.NEEDS_HOE;continue;}task=1;}
                    else if(maintenance.fertilizable(target,pos)){if(maintenance.slot(target,pos)<0){status=CompanionStatus.NEEDS_FERTILIZER;continue;}task=2;}
                    else continue;
                }
                if(target.gardenZone()!=null && ++approaches>4){cursor=(cursor+n)%scan.size();nextScan=now+20;break search;}
                var point=standing(cell);if(point==null){status=CompanionStatus.BLOCKED;continue;}
                remember(target,pos,state);soil=cell;expected=state;approach=point;action=task;
                if(!availableTo(npc)){soil=null;status=CompanionStatus.OCCUPIED;continue;}
                cursor=(cursor+n+1)%scan.size();return status=CompanionStatus.READY;
            }
            return status;
        }
        public boolean claim(PeepoEntity other){
            if(soil==null || !availableTo(other) || !current() || !enabled())return false;
            var leases=LEASES.computeIfAbsent(npc.level(),l->new LinkedHashMap<>());long now=npc.level().getGameTime();
            // Only bounded cleanup, and no level/entity references in lease values.
            if(leases.size()>=4096){
                var retained=new LinkedHashMap<BlockPos,Lease>();var iterator=leases.entrySet().iterator();int reads=0;
                while(iterator.hasNext() && reads++<64){var entry=iterator.next();if(entry.getValue().until>now)retained.put(entry.getKey(),entry.getValue());iterator.remove();}
                leases.putAll(retained); // Rotate live entries so they cannot hide expired ones indefinitely.
                if(!leases.containsKey(soil) && leases.size()>=4096)return false;
            }
            leases.put(soil,new Lease(npc.getUUID(),now+100));return true;
        }
        public boolean occupy(PeepoEntity other){return npc.position().distanceToSqr(approachPosition())<=.64 && claim(other);}
        public void release(PeepoEntity other){
            if(other!=npc)return;var leases=LEASES.get(npc.level());var lease=lease();
            if(lease!=null && lease.owner.equals(npc.getUUID()))leases.remove(soil);
            soil=null;expected=null;approach=null;progress=0;action=0;if(status==CompanionStatus.READY)status=CompanionStatus.IDLE;
        }
        public CompanionStatus work(PeepoEntity other){
            if(!current() || !occupy(other) || !enabled())return CompanionStatus.IDLE;
            var cropPos=cropPosition(target,soil);
            if(action==1 && !hoe())return CompanionStatus.NEEDS_HOE;
            if(action==2 && maintenance.slot(target,cropPos)<0)return CompanionStatus.NEEDS_FERTILIZER;
            if(expected.getBlock() instanceof MushroomColonyBlock && !cuttingTool(npc.belongings.getItem(9)))return CompanionStatus.NEEDS_CUTTER;
            if(!inReach(target,soil,npc.position()) || target.orchard() && !clearReach(soil,npc.position()) || !target.orchard() && !clearCropReach(soil,npc.position()))return CompanionStatus.BLOCKED;
            boolean tool=action==1 || action==0 && !magicCell() && !target.orchard() && !hanging(target) && (mushrooms(target)?ripe(expected) && cuttingTool(npc.belongings.getItem(9)):!CompanionGardenCrops.supported(expected) && hoe());
            // Finish the last fraction of the approach before lifting a full-size hoe.
            if(tool && !mushrooms(target) && ripe(expected) && progress==0 && npc.position().distanceToSqr(approachPosition())>.0225){
                var at=approachPosition();npc.getMoveControl().setWantedPosition(at.x,at.y,at.z,.7);
                return CompanionStatus.WORKING;
            }
            try(var tx=Transaction.openOuter()){
                if(npc.extractEnergy(ENERGY_PER_TICK,tx)!=ENERGY_PER_TICK)return CompanionStatus.RECOVERING;
                tx.commit();
            }
            float yaw=(float)Math.toDegrees(Math.atan2(-(cropPos.getX()+.5-npc.getX()),cropPos.getZ()+.5-npc.getZ()));
            npc.setYRot(yaw);npc.yBodyRot=yaw;npc.setYHeadRot(yaw);
            if(animation()==WorkAnimation.PLANT){int slot=seedSlot(target,cropPos);if(slot>=0)npc.setWorkPie(npc.belongings.getItem(slot));}
            else if(animation()==WorkAnimation.PICK)npc.setWorkPie(expected.getBlock() instanceof MushroomColonyBlock?npc.assignedHand():ItemStack.EMPTY);
            if(++progress<(tool?HOE_TICKS:WORK_TICKS))return CompanionStatus.WORKING;
            status=finish(cropPos);
            nextScan=npc.level().getGameTime()+(status==CompanionStatus.FULL?200:1);
            if(status==CompanionStatus.IDLE && tool){var held=npc.belongings.getItem(9);held.hurtAndBreak(1,npc,EquipmentSlot.MAINHAND);npc.belongings.setChanged();}
            // Release only this cell. The routine keeps the plot session and selects the next
            // live cell under the shared search budget; no stale crop actions are queued.
            release(npc);
            return status;
        }
        private boolean fruitAllowed(BlockPos pos,BlockState state){
            var stem=CompanionGardenCrops.fruitStem(npc.level(),pos,state);
            return stem!=null && CompanionJobs.permitted(npc,stem) && (target.gardenZone()!=null?target.gardenZone().contains(stem):target.plot().contains(stem.below()));
        }
        private CompanionStatus finishSpecial(BlockPos pos){
            if(CompanionGardenCrops.fruit(expected) && !fruitAllowed(pos,expected))return CompanionStatus.FORBIDDEN;
            var server=(ServerLevel)npc.level();var plan=CompanionGardenCrops.harvest(npc,pos,expected);
            if(plan==null)return CompanionStatus.GROWING;
            for(var change:plan.changes())if(!CompanionJobs.permitted(npc,change.pos()) || !server.getBlockState(change.pos()).equals(change.before()))return CompanionStatus.FORBIDDEN;
            boolean replant=!plan.cocoa();
            if(plan.cocoa())for(var drop:plan.drops())if(!replant && drop.is(Items.COCOA_BEANS)){drop.shrink(1);replant=true;}
            var changes=new ArrayList<>(plan.changes());
            try(var tx=Transaction.openOuter()){
                if(!replant)for(int i=0;i<8;i++)if(!npc.transport.reserved(i) && npc.belongings.getItem(i).is(Items.COCOA_BEANS)){
                    replant=npc.food.takeCargo(i,npc.belongings.getItem(i),1,tx)==1;if(replant)break;
                }
                if(!replant)changes.set(0,new CompanionGardenCrops.Change(pos,expected,Blocks.AIR.defaultBlockState()));
                for(var drop:plan.drops())if(!storeHarvest(drop,target.at(),tx))return CompanionStatus.FULL;
                int applied=0;
                for(var change:changes){
                    if(change.before().equals(change.after())){applied++;continue;}
                    if(!server.setBlock(change.pos(),change.after(),Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE)){
                        for(int i=applied-1;i>=0;i--){var restore=changes.get(i);server.setBlock(restore.pos(),restore.before(),Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);}
                        return CompanionStatus.BLOCKED;
                    }
                    applied++;
                }
                tx.commit();
            }
            for(var change:changes){server.updateNeighborsAt(change.pos(),change.before().getBlock());change.after().updateNeighbourShapes(server,change.pos(),Block.UPDATE_ALL);}
            server.playSound(null,pos,CompanionGardenCrops.column(expected)?SoundEvents.CROP_BREAK:CompanionGardenCrops.vine(expected)?SoundEvents.CAVE_VINES_PICK_BERRIES:SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES,SoundSource.BLOCKS,.7F,1);
            return CompanionStatus.IDLE;
        }
        private CompanionStatus finish(BlockPos pos){
            if(!current() || !assigned() || !enabled())return CompanionStatus.IDLE;
            var server=(ServerLevel)npc.level();
            if(action==1)return maintenance.repair(target,soil)?CompanionStatus.IDLE:CompanionStatus.BLOCKED;
            if(action==2){maintenance.fertilize(target,pos);return CompanionStatus.IDLE;}
            if(magicCell() && (!MagicGardenCompanion.mayChange(npc,soil) || !MagicGardenCompanion.mayChange(npc,pos)))return CompanionStatus.FORBIDDEN;
            if(expected.getBlock() instanceof OrganismCropBlock magic){
                var definition=magic.organism();var drops=magic.harvestYield(expected);
                if(definition==null || !definition.crop() || drops.isEmpty())return CompanionStatus.GROWING;
                // Native pick yields and data-defined regrowth; no seed charge, uprooting or player research practice.
                try(var tx=Transaction.openOuter()){
                    for(var drop:drops)if(!storeHarvest(drop,target.at(),tx))return CompanionStatus.FULL;
                    if(!server.setBlock(pos,magic.getStateForAge(definition.replant()),Block.UPDATE_CLIENTS))return CompanionStatus.BLOCKED;
                    tx.commit();
                }
                server.playSound(null,pos,SoundEvents.CROP_BREAK,SoundSource.BLOCKS,.8F,1);
                return CompanionStatus.IDLE;
            }
            if(CompanionGardenCrops.supported(expected))return finishSpecial(pos);
            if(target.orchard() && expected.getBlock() instanceof FruitingLeavesBlock leaves){
                if(expected.getValue(FruitingLeavesBlock.FRUIT)!=FruitingLeavesBlock.RIPE)return CompanionStatus.GROWING;
                var produce=leaves.harvestFruit(server.getRandom());
                try(var tx=Transaction.openOuter()){
                    if(!storeHarvest(produce,target.at(),tx))return CompanionStatus.FULL;
                    if(!server.setBlock(pos,expected.setValue(FruitingLeavesBlock.FRUIT,0),Block.UPDATE_CLIENTS))return CompanionStatus.BLOCKED;
                    tx.commit();
                }
                server.playSound(null,pos,SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES,SoundSource.BLOCKS,.7F,1);
                return CompanionStatus.IDLE;
            }else if(expected.getBlock() instanceof TallCropBlock tall){
                var kind=tall.crop();int height=kind.height(TallCropBlock.MAX_AGE);
                boolean sameCrop=accepts(target,new ItemStack(JugcraftAgriculture.item(kind.seedId)));
                var previous=new ArrayList<BlockState>(height);
                for(int section=0;section<height;section++){
                    var part=pos.above(section);if(!CompanionJobs.permitted(npc,part))return CompanionStatus.FORBIDDEN;
                    var state=server.getBlockState(part);
                    if(!state.is(tall) || state.getValue(TallCropBlock.SECTION)!=section || !TallCropBlock.isRipe(state))return CompanionStatus.IDLE;
                    previous.add(state);
                }
                var produce=new ItemStack(tall.produce(previous.getFirst()),kind.pickMin+server.getRandom().nextInt(kind.pickMax-kind.pickMin+1));
                try(var tx=Transaction.openOuter()){
                    if(!storeHarvest(produce,target.at(),tx))return CompanionStatus.FULL;
                    // Retire the whole plant before shape updates can break its other sections and drop duplicate loot.
                    int flags=Block.UPDATE_CLIENTS | (sameCrop?0:Block.UPDATE_KNOWN_SHAPE);
                    for(int section=height-1;section>=0;section--){
                        var part=pos.above(section);
                        // Retire a different crop only when ripe; keep player-built trellises intact.
                        var empty=kind.trellis?JugcraftAgriculture.block("trellis").defaultBlockState():section==0 && tall instanceof PaddyCropBlock?Blocks.WATER.defaultBlockState():Blocks.AIR.defaultBlockState();
                        var after=sameCrop && section<kind.height(kind.pickReset)?tall.stateFor(kind.pickReset,section):empty;
                        if(!server.setBlock(part,after,flags)){
                            for(int restore=height-1;restore>section;restore--)server.setBlock(pos.above(restore),previous.get(restore),Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                            return CompanionStatus.BLOCKED;
                        }
                    }
                    tx.commit();
                }
            }else if(expected.getBlock() instanceof MushroomColonyBlock colony){
                if(!cuttingTool(npc.belongings.getItem(9)))return CompanionStatus.NEEDS_CUTTER;
                var produce=new ItemStack(colony.mushroom(),MushroomColonyBlock.PICK_MIN+server.getRandom().nextInt(MushroomColonyBlock.PICK_MAX-MushroomColonyBlock.PICK_MIN+1));
                var after=accepts(target,new ItemStack(colony.mushroom()))?expected.setValue(MushroomColonyBlock.AGE,MushroomColonyBlock.PICK_RESET):Blocks.AIR.defaultBlockState();
                try(var tx=Transaction.openOuter()){
                    if(!storeHarvest(produce,target.at(),tx))return CompanionStatus.FULL;
                    if(!server.setBlock(pos,after,Block.UPDATE_ALL))return CompanionStatus.BLOCKED;
                    tx.commit();
                }
            }else if(expected.getBlock() instanceof HerbBundleBlock){
                if(!expected.getValue(HerbBundleBlock.DRIED))return CompanionStatus.DRYING;
                var drops=Block.getDrops(expected,server,pos,null,npc,ItemStack.EMPTY);
                try(var tx=Transaction.openOuter()){
                    for(var drop:drops)if(!storeHarvest(drop,target.at(),tx))return CompanionStatus.FULL;
                    if(!server.setBlock(pos,Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL))return CompanionStatus.BLOCKED;
                    tx.commit();
                }
            }else if(expected.getBlock() instanceof CropBlock crop){
                var drops=Block.getDrops(expected,server,pos,null,npc,ItemStack.EMPTY);Item seed=seedFor(crop);boolean replant=false;
                boolean sameCrop=accepts(target,new ItemStack(seed));
                for(var drop:drops)if(sameCrop && !replant && drop.is(seed)){drop.shrink(1);replant=true;}
                try(var tx=Transaction.openOuter()){
                    if(sameCrop && !replant)for(int i=0;i<8;i++)if(!npc.transport.reserved(i) && npc.belongings.getItem(i).is(seed)){
                        var stack=npc.belongings.getItem(i);replant=npc.food.takeCargo(i,stack,1,tx)==1;break;
                    }
                    for(var drop:drops)if(!storeHarvest(drop,target.at(),tx))return CompanionStatus.FULL;
                    if(!server.setBlock(pos,replant?crop.getStateForAge(0):Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL))return CompanionStatus.BLOCKED;
                    tx.commit();
                }
            }else{
                int slot=seedSlot(target,pos);if(slot<0)return CompanionStatus.NO_SEEDS;
                var seed=npc.belongings.getItem(slot);var planted=planting(target,seed,pos);if(planted==null)return CompanionStatus.NO_SEEDS;
                try(var tx=Transaction.openOuter()){
                    if(npc.food.takeCargo(slot,seed,1,tx)!=1)return CompanionStatus.NO_SEEDS;
                    if(!server.setBlock(pos,planted,Block.UPDATE_ALL))return CompanionStatus.BLOCKED;
                    tx.commit();
                    remember(target,pos,planted);
                }
            }
            server.playSound(null,pos,SoundEvents.CROP_BREAK,SoundSource.BLOCKS,.5F,1.1F);
            return CompanionStatus.IDLE;
        }
    }
}
