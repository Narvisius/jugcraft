package io.github.jimbozoomer.jugcraft.machine.form;

import java.util.*;
import io.github.jimbozoomer.jugcraft.chemistry.*;
import io.github.jimbozoomer.jugcraft.machine.MachineBlock;
import local.peepo.*;
import net.fabricmc.fabric.api.transfer.v1.item.*;
import net.fabricmc.fabric.api.transfer.v1.storage.*;
import net.fabricmc.fabric.api.transfer.v1.storage.base.*;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.*;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.*;

/** Item-only companion access to native form inventories. Lanes, fluids and recipe locks remain native. */
public final class FormCompanionPort {
    private final FormMachineBlockEntity machine;
    public final CompanionRecipeFilter filter = new CompanionRecipeFilter();
    private Identifier selected;
    private FluidRecipes.Entry selectedEntry;
    private List<FluidRecipes.Entry> selectedCatalog;
    private List<FluidRecipes.Entry> filteredSource, filteredRecipes;
    private List<FluidRecipes.Entry> stockSource;
    private Set<Item> stockProducts=Set.of();
    private int filteredRevision = -1;
    private Direction permissionFacing;
    private List<BlockPos> footprint = List.of(), claimChunks = List.of();

    public FormCompanionPort(FormMachineBlockEntity machine) { this.machine = machine; }
    public boolean supportsSupply() { return machine.form().itemInputs() > 0 || !machine.form().sockets().isEmpty(); }
    public boolean supportsOutput() { return machine.form().firstUpgradeSlot() > 0; }
    public boolean allows(FluidRecipe recipe) {
        return filter.empty() || icons(recipe).stream().anyMatch(filter::allows);
    }
    /** Native processing also reads the filter; rebuild only on an edit or recipe reload, never each tick. */
    List<FluidRecipes.Entry> filtered(List<FluidRecipes.Entry> recipes) {
        if (filter.empty()) return recipes;
        if (filteredSource != recipes || filteredRevision != filter.revision()) {
            filteredSource = recipes; filteredRevision = filter.revision();
            filteredRecipes = recipes.stream().filter(e -> allows(e.recipe())).toList();
        }
        return filteredRecipes;
    }
    private static List<ItemStack> icons(FluidRecipe recipe) {
        var icons = new ArrayList<ItemStack>();
        for (var result : recipe.results()) icons.add(result.create());
        for (var result : recipe.fluidResults()) {
            var bucket = result.fluid().getBucket();
            if (bucket != Items.AIR) icons.add(new ItemStack(bucket));
        }
        return icons;
    }
    public boolean acceptsFilter(ItemStack stack) {
        return stack.isEmpty() || machine.getLevel() instanceof ServerLevel level
            && machine.recipes(level.getServer()).stream().anyMatch(e -> icons(e.recipe()).stream().anyMatch(s -> s.is(stack.getItem())));
    }
    public boolean selectable() {
        return machine.getLevel() instanceof ServerLevel level
            && machine.recipes(level.getServer()).stream().anyMatch(e -> !icons(e.recipe()).isEmpty());
    }
    public void filterChanged() { selected = null; selectedEntry = null; selectedCatalog = null; CompanionReadiness.invalidate(machine); }

    /** Check the complete footprint, including claims across chunk borders. Never load a part. */
    public boolean permitted(PeepoEntity npc) {
        if (machine.isRemoved() || machine.getLevel() != npc.level() || !machine.form().companionAssistance()) return false;
        permissionGeometry();
        // Land claims are chunk-wide. Check each distinct chunk once, not once per casing block.
        for (var at : claimChunks) if (!CompanionJobs.permitted(npc, at)) return false;
        for (var at : footprint) if (io.github.jimbozoomer.jugcraft.town.TownProtection.shieldsBlock(npc.level(), at)
            || !npc.level().getWorldBorder().isWithinBounds(at)) return false;
        return true;
    }
    /** Cache geometry only; permission decisions always read current ownership/trust and protection. */
    private void permissionGeometry() {
        var facing = machine.getBlockState().getValue(MachineBlock.FACING);
        if (permissionFacing == facing) return;
        permissionFacing = facing;
        var form = machine.form(); var parts = new ArrayList<BlockPos>(); var chunks = new LinkedHashMap<Long,BlockPos>();
        for (int part = 0; part < form.footprint().size(); part++) {
            var at = form.footprint().partPos(machine.getBlockPos(), facing, part);
            parts.add(at); chunks.putIfAbsent(io.github.jimbozoomer.jugcraft.finance.LandRegistry.keyOf(at), at);
        }
        footprint = List.copyOf(parts); claimChunks = List.copyOf(chunks.values());
    }
    public boolean playerMayEdit(net.minecraft.world.entity.player.Player player) {
        permissionGeometry();
        for (var at : claimChunks) if (!machine.getLevel().hasChunkAt(at)
            || !io.github.jimbozoomer.jugcraft.finance.LandRegistry.mayBuild(player, machine.getLevel(), at)) return false;
        return true;
    }
    public boolean accessible(PeepoEntity npc) {
        return npc.orders.tamed() && npc.assignments.assignedWork(machine.getBlockPos()) && permitted(npc)
            && machine.companionStatus() != CompanionStatus.UNLOADED && machine.companionStatus() != CompanionStatus.MACHINE_UNFORMED;
    }
    public List<ItemStack> installed() {
        var items = new ArrayList<ItemStack>();
        for (int i = 0; i < machine.form().itemInputs(); i++) items.add(machine.container().getItem(i));
        return items;
    }
    private List<FluidRecipes.Entry> choices() {
        return machine.getLevel() instanceof ServerLevel level ? machine.allowedRecipes(level.getServer()) : List.of();
    }
    private FluidRecipes.Entry choice() {
        var e = selectedEntry;
        if (e == null || !(machine.getLevel() instanceof ServerLevel level)
            || machine.recipes(level.getServer()) != selectedCatalog || !allows(e.recipe())
            || machine.recipeLock() != null && !machine.recipeLock().equals(e.id())
            || machine.targetReached(e.id(), e.recipe()) || !machine.consistent(e.recipe(), installed())) return null;
        return e;
    }
    private boolean hasTool(FluidRecipe recipe) {
        if (recipe.tool().isEmpty()) return true;
        for (int i = machine.form().firstSocketSlot(); i < machine.form().firstUpgradeSlot(); i++)
            if (recipe.tool().get().test(machine.container().getItem(i))) return true;
        return false;
    }
    public FluidRecipes.Entry stockRecipe(){return machine.getLevel() instanceof ServerLevel l && selectedCatalog==machine.recipes(l.getServer())?selectedEntry:null;}
    private void stockCatalog(){if(machine.getLevel() instanceof ServerLevel l){var source=machine.recipes(l.getServer());if(source!=stockSource){var products=new HashSet<Item>();for(var e:source)if(!e.recipe().items().isEmpty())for(var r:e.recipe().results())products.add(r.create().getItem());stockSource=source;stockProducts=Set.copyOf(products);}}}
    public boolean stockTargets(){stockCatalog();return !stockProducts.isEmpty();}
    public boolean stockProduct(ItemStack icon){stockCatalog();return stockProducts.contains(icon.getItem());}
    private int toolSlot(FluidRecipe recipe, ItemStack stack) {
        if (hasTool(recipe) || !recipe.tool().orElseThrow().test(stack)) return -1;
        for (int i = 0; i < machine.form().sockets().size(); i++) {
            int slot = machine.form().firstSocketSlot() + i;
            if (!machine.socketLocked(i)
                && machine.form().sockets().get(i).accepts(stack)) return slot;
        }
        return -1;
    }
    /** Called only by the budgeted delivery planner; catalog matching never runs in helper ticks. */
    private void prepare(Storage<ItemVariant> source) {
        prepare(source,null);
    }
    public void prepare(Storage<ItemVariant> source,java.util.function.ToDoubleFunction<ItemStack> priority) {
        var supplies = RecipeSupplies.available(source);
        var installed = installed();
        var choices = new ArrayList<>(choices());
        java.util.function.ToDoubleFunction<FluidRecipes.Entry> score=e->priority==null?0:e.recipe().results().stream().map(r->r.create()).filter(filter::allows).mapToDouble(priority).max().orElse(-1);
        choices.sort(Comparator.<FluidRecipes.Entry>comparingDouble(score).reversed().thenComparingInt(e -> e.id().equals(selected) ? 0 : 1));
        int inspected = 0;
        for (var e : choices) {
            if (++inspected > 256) break;
            var recipe = e.recipe();
            if (score.applyAsDouble(e)<=-2 || machine.targetReached(e.id(), recipe) || !machine.consistent(recipe, installed)
                || !recipe.fluidsMatch(machine.tanks())) continue;
            var parts = recipe.items().stream().map(p -> new RecipeSupplies.Part(p.ingredient(), p.count())).toList();
            if (RecipeSupplies.completes(parts, installed, supplies)
                && (hasTool(recipe) || supplies.stream().anyMatch(s -> toolSlot(recipe, s) >= 0))) {
                selected = e.id(); selectedEntry = e;
                selectedCatalog = machine.recipes(((ServerLevel)machine.getLevel()).getServer()); return;
            }
        }
        if (choice() == null) selected = null;
    }
    private int[] needs(ItemStack stack) {
        int[] needs = new int[machine.form().containerSize()];
        var entry = choice();
        if (entry == null || stack.isEmpty() || machine.paused()
            || !io.github.jimbozoomer.jugcraft.config.JugcraftConfig.isFeatureEnabled("machines")) return needs;
        var recipe = entry.recipe();
        // A bounded, balanced buffer of up to eight batches, independent of source chest size.
        int batches = 8;
        for (var part : recipe.items()) batches = Math.min(batches, Math.max(1, 64 / part.count()));
        for (int slot = 0; slot < recipe.items().size(); slot++) {
            var part = recipe.items().get(slot); var held = machine.container().getItem(slot);
            if (part.ingredient().test(stack) && (held.isEmpty() || ItemStack.isSameItemSameComponents(stack, held)))
                needs[slot] = Math.max(0, Math.min(stack.getMaxStackSize(), part.count() * batches) - held.getCount());
        }
        int tool = toolSlot(recipe, stack);
        if (tool >= 0 && machine.container().getItem(tool).isEmpty()) needs[tool] = 1;
        return needs;
    }
    private boolean outputSlot(int slot) {
        var form = machine.form();
        if (slot >= form.firstOutputSlot() && slot < form.firstSocketSlot()) return true;
        var stack = machine.container().getItem(slot); if (stack.isEmpty()) return false;
        if (slot >= form.firstSocketSlot()) {
            var entry = choice();
            return !machine.socketLocked(slot-form.firstSocketSlot()) && entry != null
                && entry.recipe().tool().isPresent() && !entry.recipe().tool().get().test(stack);
        }
        for (int lane = 0; lane < form.profile().lanes(); lane++) if (machine.lane(lane) != null) return false;
        return choices().stream().noneMatch(e -> slot < e.recipe().items().size() && e.recipe().items().get(slot).ingredient().test(stack));
    }
    public CompanionLogistics.Port forCompanion(PeepoEntity npc) { return new CompanionLogistics.Port() {
        public void prepare(Storage<ItemVariant> source) { if (accessible(npc)) FormCompanionPort.this.prepare(source); }
        public Identifier plan() { var entry = choice(); return entry == null ? null : entry.id(); }
        public int needed(ItemStack stack) { return accessible(npc) ? Arrays.stream(needs(stack)).sum() : 0; }
        public int stockAuxiliaryNeed(ItemStack stack){var e=choice();return e!=null && toolSlot(e.recipe(),stack)>=0?1:0;}
        public CompanionStatus status() { return accessible(npc) ? machine.companionStatus() : CompanionStatus.FORBIDDEN; }
        public Storage<ItemVariant> inputs() { return new InsertionOnlyStorage<>() {
            public long insert(ItemVariant item, long amount, TransactionContext tx) {
                StoragePreconditions.notBlankNotNegative(item, amount);
                if (!accessible(npc) || amount == 0) return 0;
                var needs = needs(item.toStack(1)); var inventory = ContainerStorage.of(machine.container(), null);
                long moved = 0;
                var entry=choice();if(entry==null)return 0;
                // Fill complete recipe layers first, including recipes that repeat an ingredient.
                for(int layer=1;layer<=8 && moved<amount;layer++)for(int i=0;i<needs.length && moved<amount;i++)if(needs[i]>0){
                    int cap=i<entry.recipe().items().size()?entry.recipe().items().get(i).count()*layer:1;
                    int room=Math.min(needs[i],Math.max(0,cap-machine.container().getItem(i).getCount()));
                    if(room>0){long n=inventory.getSlot(i).insert(item,Math.min(room,amount-moved),tx);needs[i]-=(int)n;moved+=n;}
                }
                return moved;
            }
        }; }
        public Storage<ItemVariant> outputs() {
            var slots = new ArrayList<Storage<ItemVariant>>();
            for (int i = 0; i < machine.form().firstUpgradeSlot(); i++) {
                final int slot = i;
                slots.add(new SingleSlotStorage<>() {
                    public boolean supportsInsertion() { return false; }
                    public long insert(ItemVariant item, long amount, TransactionContext tx) { return 0; }
                    public long extract(ItemVariant item, long amount, TransactionContext tx) {
                        StoragePreconditions.notBlankNotNegative(item, amount);
                        return accessible(npc) && outputSlot(slot) ? ContainerStorage.of(machine.container(), null).getSlot(slot).extract(item, amount, tx) : 0;
                    }
                    public ItemVariant getResource() { return accessible(npc) && outputSlot(slot) ? ItemVariant.of(machine.container().getItem(slot)) : ItemVariant.blank(); }
                    public boolean isResourceBlank() { return getResource().isBlank(); }
                    public long getAmount() { return accessible(npc) && outputSlot(slot) ? machine.container().getItem(slot).getCount() : 0; }
                    public long getCapacity() { return 64; }
                });
            }
            return new CombinedStorage<>(slots);
        }
    }; }

    /** Ground-level perimeter only: a controller's adjacent block may be inside a large machine. */
    public List<BlockPos> entrances(PeepoEntity npc) {
        var result = new LinkedHashSet<BlockPos>(); var form = machine.form();
        var front = machine.getBlockState().getValue(MachineBlock.FACING);
        for (int column = 0; column < form.width(); column++) for (int row = 0; row < form.depth(); row++) {
            int cell = form.cellIndex(column, row, 0);
            if (form.cell(cell) != FormCell.STRUCTURE) continue;
            var anchor = form.cellPos(machine.getBlockPos(), front, cell);
            for (var side : Direction.Plane.HORIZONTAL) {
                if (!(row == 0 && side == front || row == form.depth()-1 && side == front.getOpposite()
                    || column == 0 && side == front.getClockWise() || column == form.width()-1 && side == front.getCounterClockWise())) continue;
                for (int dy = 0; dy >= -1; dy--) {
                    var at = anchor.relative(side).offset(0, dy, 0); var p = Vec3.atBottomCenterOf(at);
                    double r = npc.getBbWidth()/2 + .01;
                    if (CompanionJobs.permitted(npc, at) && npc.level().hasChunkAt(at.below())
                        && npc.level().getBlockState(at.below()).isFaceSturdy(npc.level(), at.below(), Direction.UP)
                        && CompanionHazards.safeAt(npc, p) && npc.level().noCollision(new AABB(p.x-r,p.y,p.z-r,p.x+r,p.y+npc.getBbHeight(),p.z+r))) result.add(at);
                }
            }
        }
        return List.copyOf(result);
    }
}
