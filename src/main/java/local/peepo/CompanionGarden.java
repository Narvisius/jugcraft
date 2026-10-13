package local.peepo;

import java.util.*;
import io.github.jimbozoomer.jugcraft.agriculture.*;
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
    private final Map<BlockPos,Job> jobs=new HashMap<>();
    // Provenance only, not another inventory. Never export unmarked, player-given cargo.
    private final ItemStack[] harvest=new ItemStack[8];
    private final GlobalPos[] origins=new GlobalPos[8];
    record Snapshot(List<ItemStack> stacks,List<GlobalPos> origins){}
    private record Lease(UUID owner,long until){}
    private static final Map<Level,LinkedHashMap<BlockPos,Lease>> LEASES=new WeakHashMap<>();
    CompanionGarden(PeepoEntity npc){this.npc=npc;Arrays.fill(harvest,ItemStack.EMPTY);}

    /** Normalize only supported seeds/raw produce; never preserve cursor components or grant an item. */
    public static Item selectionSeed(ItemStack icon){
        if(icon.isEmpty())return null;
        if(seedItem(icon))return icon.getItem();
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
    private boolean accepts(CompanionAssignments.Target target,ItemStack seed){var selected=npc.assignments.gardenSeed(target);return selected==null || seed.is(selected);}

    public static boolean farmland(Level level,BlockPos pos){return level.hasChunkAt(pos) && level.getBlockState(pos).getBlock() instanceof FarmlandBlock;}
    /** Bound both the result and exploration; edges are horizontal, never diagonal or across missing chunks. */
    static List<BlockPos> discover(PeepoEntity npc,BlockPos anchor){
        var found=new ArrayList<BlockPos>(PLOT_LIMIT);var queue=new ArrayDeque<BlockPos>();var seen=new HashSet<BlockPos>();
        queue.add(anchor);seen.add(anchor);
        while(!queue.isEmpty() && found.size()<PLOT_LIMIT){
            var pos=queue.removeFirst();
            if(!farmland(npc.level(),pos) || !CompanionJobs.permitted(npc,pos) || npc.assignments.gardenContains(pos)
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
        jobs.entrySet().removeIf(e->!npc.assignments.assignedWork(e.getKey()));
        var job=jobs.get(anchor);
        if(job==null || !job.target.equals(target)){job=new Job(target);jobs.put(anchor,job);}
        return job;
    }
    private boolean enabled(){return npc.level() instanceof ServerLevel server && server.getGameRules().get(GameRules.MOB_GRIEFING);}
    private boolean accessible(BlockPos soil){return farmland(npc.level(),soil) && CompanionJobs.permitted(npc,soil) && CompanionJobs.permitted(npc,soil.above());}
    private boolean hoe(){return npc.belongings.getItem(9).is(ItemTags.HOES);}
    private static boolean ripe(BlockState state){return state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state)
        || state.getBlock() instanceof TallCropBlock && state.getValue(TallCropBlock.SECTION)==0 && TallCropBlock.isRipe(state);}
    private BlockState planting(ItemStack seed,BlockPos pos){
        if(seed.isEmpty() || !npc.level().hasChunkAt(pos))return null;
        BlockState planted=null;
        if(seed.getItem() instanceof BlockItem item && item.getBlock() instanceof CropBlock crop)planted=crop.getStateForAge(0);
        else for(var kind:TallCrop.values())if(seed.is(JugcraftAgriculture.item(kind.seedId))){
            var current=npc.level().getBlockState(pos);
            if(kind.trellis?!(current.getBlock() instanceof TrellisBlock):!current.isAir())return null;
            planted=JugcraftAgriculture.TALL_CROPS.get(kind).defaultBlockState();break;
        }
        if(planted==null || (!(planted.getBlock() instanceof TallCropBlock tall && tall.crop().trellis) && !npc.level().getBlockState(pos).isAir()))return null;
        return planted.canSurvive(npc.level(),pos)?planted:null;
    }
    private int seedSlot(CompanionAssignments.Target target,BlockPos pos){for(int i=0;i<8;i++)if(!npc.transport.reserved(i) && accepts(target,npc.belongings.getItem(i)) && planting(npc.belongings.getItem(i),pos)!=null)return i;return -1;}
    private static boolean seedItem(ItemStack stack){
        if(stack.getItem() instanceof BlockItem item && item.getBlock() instanceof CropBlock)return true;
        for(var kind:TallCrop.values())if(stack.is(JugcraftAgriculture.item(kind.seedId)))return true;
        return false;
    }
    private static Item seedFor(CropBlock crop){return crop instanceof JugcraftCropBlock jug?jug.seed():crop.asItem();}
    private int seedReserve(ItemStack stack){
        if(!seedItem(stack))return 0;
        int cells=0;for(int row=1;row<=4;row++){var t=npc.assignments.get(row);if(t!=null && t.garden() && t.local(npc.level()) && accepts(t,stack))cells+=t.plot().size();}
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
    public void save(ValueOutput out){for(int i=0;i<8;i++)if(!harvest[i].isEmpty() && origins[i]!=null){out.store("GardenHarvest"+i,ItemStack.CODEC,harvest[i]);out.store("GardenOrigin"+i,GlobalPos.CODEC,origins[i]);}}
    public void load(ValueInput in){
        jobs.clear();Arrays.fill(harvest,ItemStack.EMPTY);Arrays.fill(origins,null);
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
            if(!enabled() || !seedItem(candidate) || !accepts(target,candidate))return 0;
            int holes=0,carried=0;
            var compatible=new ArrayList<BlockPos>(PLOT_LIMIT);
            for(var soil:target.plot())if(accessible(soil) && planting(candidate,soil.above())!=null){holes++;compatible.add(soil.above());}
            if(holes==0)return 0;
            for(int i=0;i<8;i++)if(!npc.transport.reserved(i)){
                var stack=npc.belongings.getItem(i);
                if(!accepts(target,stack))continue;
                for(var pos:compatible)if(planting(stack,pos)!=null){carried+=stack.getCount();break;}
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
        private int progress,cursor;
        private long nextScan;
        private CompanionStatus status=CompanionStatus.IDLE;
        Job(CompanionAssignments.Target target){this.target=target;}
        public Kind kind(){return Kind.WORK;}
        public WorkAnimation animation(){return hoe() && expected!=null && ripe(expected)?progress>0?WorkAnimation.HARVEST:WorkAnimation.NONE:WorkAnimation.INTERACT;}
        public BlockPos animationTarget(){return soil==null?stationPosition():soil;}
        public BlockPos stationPosition(){return target.at().pos();}
        public Vec3 approachPosition(){return approach==null?Vec3.atBottomCenterOf(stationPosition().above()):approach;}
        private boolean assigned(){int row=npc.assignments.workPriority(stationPosition());return row<5 && target.equals(npc.assignments.get(row)) && target.present(npc.level());}
        private Lease lease(){var leases=LEASES.get(npc.level());return soil==null || leases==null?null:leases.get(soil);}
        public boolean availableTo(PeepoEntity other){var lease=lease();return other==npc && assigned() && (lease==null || lease.until<=npc.level().getGameTime() || lease.owner.equals(npc.getUUID()));}
        private Vec3 standing(BlockPos cell){
            var options=new ArrayList<Vec3>(5);
            // Prefer a path beside tall crops; collision checks also accommodate ordinary farmland's 15/16 height.
            for(var side:Direction.Plane.HORIZONTAL){var floor=cell.relative(side);addStanding(options,floor);}
            if(options.isEmpty() || !hoe() || !ripe(npc.level().getBlockState(cell.above())))addStanding(options,cell);
            return options.stream().min(Comparator.comparingDouble(npc.position()::distanceToSqr)).orElse(null);
        }
        private void addStanding(List<Vec3> points,BlockPos floor){
            if(!npc.level().hasChunkAt(floor) || !npc.level().hasChunkAt(floor.above()) || !CompanionJobs.permitted(npc,floor))return;
            var state=npc.level().getBlockState(floor);double top;
            if(state.getBlock() instanceof FarmlandBlock)top=.9375;
            else if(state.isFaceSturdy(npc.level(),floor,Direction.UP))top=1;else return;
            var at=new Vec3(floor.getX()+.5,floor.getY()+top,floor.getZ()+.5);double r=npc.getBbWidth()/2+.02;
            if(CompanionHazards.safeAt(npc,at) && npc.level().noCollision(new AABB(at.x-r,at.y+.001,at.z-r,at.x+r,at.y+npc.getBbHeight(),at.z+r)))points.add(new Vec3(at.x,floor.getY()+1,at.z));
        }
        private boolean current(){return soil!=null && accessible(soil) && npc.level().getBlockState(soil.above()).equals(expected);}
        public CompanionStatus workStatus(PeepoEntity other){
            if(!assigned())return CompanionStatus.MISSING;
            if(!enabled())return CompanionStatus.GARDEN_DISABLED;
            if(!npc.preferences.canWork())return CompanionStatus.RECOVERING;
            if(soil!=null){if(current())return availableTo(other)?CompanionStatus.READY:CompanionStatus.OCCUPIED;release(npc);}
            long now=npc.level().getGameTime();if(now<nextScan)return status;
            if(!CompanionBudget.search(npc))return CompanionStatus.WAITING;
            nextScan=now+80+Math.floorMod(npc.getId(),20);status=CompanionStatus.GROWING;
            // One pass over at most eight fixed cells; no flood fill or path creation while inspecting status.
            for(int n=0;n<target.plot().size();n++){
                var cell=target.plot().get((cursor+n)%target.plot().size());
                if(!npc.level().hasChunkAt(cell)){status=CompanionStatus.UNLOADED;continue;}
                if(!farmland(npc.level(),cell)){status=CompanionStatus.MISSING;continue;}
                if(!accessible(cell)){status=CompanionStatus.FORBIDDEN;continue;}
                var state=npc.level().getBlockState(cell.above());
                if(!ripe(state)){
                    if(!state.isAir() && !(state.getBlock() instanceof TrellisBlock))continue;
                    if(seedSlot(target,cell.above())<0){status=CompanionStatus.NO_SEEDS;continue;}
                }
                var point=standing(cell);if(point==null){status=CompanionStatus.BLOCKED;continue;}
                soil=cell;expected=state;approach=point;
                if(!availableTo(npc)){soil=null;status=CompanionStatus.OCCUPIED;continue;}
                cursor=(cursor+n+1)%target.plot().size();return status=CompanionStatus.READY;
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
            soil=null;expected=null;approach=null;progress=0;if(status==CompanionStatus.READY)status=CompanionStatus.IDLE;
        }
        public CompanionStatus work(PeepoEntity other){
            if(!current() || !occupy(other) || !enabled())return CompanionStatus.IDLE;
            var cropPos=soil.above();
            if(cropPos.distToCenterSqr(npc.position())>6.25)return CompanionStatus.BLOCKED;
            boolean tool=hoe();
            // Finish the last fraction of the approach before lifting a full-size hoe.
            if(tool && ripe(expected) && progress==0 && npc.position().distanceToSqr(approachPosition())>.0225){
                var at=approachPosition();npc.getMoveControl().setWantedPosition(at.x,at.y,at.z,.7);
                return CompanionStatus.WORKING;
            }
            try(var tx=Transaction.openOuter()){
                if(npc.extractEnergy(ENERGY_PER_TICK,tx)!=ENERGY_PER_TICK)return CompanionStatus.RECOVERING;
                tx.commit();
            }
            float yaw=(float)Math.toDegrees(Math.atan2(-(cropPos.getX()+.5-npc.getX()),cropPos.getZ()+.5-npc.getZ()));
            npc.setYRot(yaw);npc.yBodyRot=yaw;npc.setYHeadRot(yaw);
            if(++progress<(tool?HOE_TICKS:WORK_TICKS))return CompanionStatus.WORKING;
            status=finish(cropPos);
            nextScan=npc.level().getGameTime()+(status==CompanionStatus.FULL?200:1);
            if(status==CompanionStatus.IDLE && tool){var held=npc.belongings.getItem(9);held.hurtAndBreak(1,npc,EquipmentSlot.MAINHAND);npc.belongings.setChanged();}
            // Release only this cell. The routine keeps the plot session and selects the next
            // live cell under the shared search budget; no stale crop actions are queued.
            release(npc);
            return status;
        }
        private CompanionStatus finish(BlockPos pos){
            if(!current() || !assigned() || !enabled())return CompanionStatus.IDLE;
            var server=(ServerLevel)npc.level();
            if(expected.getBlock() instanceof TallCropBlock tall){
                var kind=tall.crop();int height=kind.height(TallCropBlock.MAX_AGE);
                boolean sameCrop=accepts(target,new ItemStack(JugcraftAgriculture.item(kind.seedId)));
                var previous=new ArrayList<BlockState>(height);
                for(int section=0;section<height;section++){
                    var part=pos.above(section);if(!CompanionJobs.permitted(npc,part))return CompanionStatus.FORBIDDEN;
                    var state=server.getBlockState(part);
                    if(!state.is(tall) || state.getValue(TallCropBlock.SECTION)!=section || !TallCropBlock.isRipe(state))return CompanionStatus.IDLE;
                    previous.add(state);
                }
                var produce=new ItemStack(JugcraftAgriculture.item(kind.produceId),kind.pickMin+server.getRandom().nextInt(kind.pickMax-kind.pickMin+1));
                try(var tx=Transaction.openOuter()){
                    if(!storeHarvest(produce,target.at(),tx))return CompanionStatus.FULL;
                    for(int section=height-1;section>=0;section--){
                        var part=pos.above(section);var before=previous.get(section);
                        // Retire a different crop only when ripe; keep player-built trellises intact.
                        var after=sameCrop?before.setValue(TallCropBlock.AGE,kind.pickReset):kind.trellis?JugcraftAgriculture.block("trellis").defaultBlockState():Blocks.AIR.defaultBlockState();
                        if(!server.setBlock(part,after,Block.UPDATE_CLIENTS)){
                            for(int restore=height-1;restore>section;restore--)server.setBlock(pos.above(restore),previous.get(restore),Block.UPDATE_CLIENTS);
                            return CompanionStatus.BLOCKED;
                        }
                    }
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
                var seed=npc.belongings.getItem(slot);var planted=planting(seed,pos);if(planted==null)return CompanionStatus.NO_SEEDS;
                try(var tx=Transaction.openOuter()){
                    if(npc.food.takeCargo(slot,seed,1,tx)!=1)return CompanionStatus.NO_SEEDS;
                    if(!server.setBlock(pos,planted,Block.UPDATE_ALL))return CompanionStatus.BLOCKED;
                    tx.commit();
                }
            }
            server.playSound(null,pos,SoundEvents.CROP_BREAK,SoundSource.BLOCKS,.5F,1.1F);
            return CompanionStatus.IDLE;
        }
    }
}
