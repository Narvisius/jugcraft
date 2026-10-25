package io.github.jimbozoomer.jugcraft.jimtafacts.network;

import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.energy.SimpleEnergyStorage;
import io.github.jimbozoomer.jugcraft.party.JugcraftParties;
import java.util.*;
import net.fabricmc.fabric.api.transfer.v1.item.*;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.*;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Contents live in removable cells, never in a second network database. */
public final class DigitalNode extends BlockEntity implements Container {
    DigitalFacades.Covers host;
    private @Nullable UUID owner;
    UUID linkIdentity = UUID.randomUUID();
    DigitalLinks.@Nullable Peer peer;
    boolean linkInput;
    private NonNullList<ItemStack> items = NonNullList.withSize(18, ItemStack.EMPTY);
    private ItemStack filter = ItemStack.EMPTY;
    final List<DigitalPatterns.Order> orders = new ArrayList<>();
    final List<DigitalProcessing.Job> processing = new ArrayList<>();
    final DigitalReservations reservations=new DigitalReservations(this);
    String processingStatus = "Queued";
    public final SimpleEnergyStorage energy;
    int burn;
    int ioCursor;
    public long crankAt = -100;
    public int crankTurns;
    long condensed;
    final DigitalCondenser.Port condenserStorage = new DigitalCondenser.Port(this);
    int threshold = 64;
    boolean emitBelow;
    long observed;
    // Public appearance only. Never put cells, filters, owner IDs or their components in render packets.
    private final int[] visibleCells = new int[10], visibleStatus = new int[10];
    private final long[] visibleBlink = new long[10], previousAmounts = new long[10];
    private ItemStack visibleTop = ItemStack.EMPTY, visibleMiddle = ItemStack.EMPTY, visibleResult = ItemStack.EMPTY;
    private ItemStack beforeWorkInput = ItemStack.EMPTY;
    private long visibleCycle = -100;
    private boolean renderDirty = true, renderInitialized;
    private int renderHash;
    private final SnapshotParticipant<List<ItemStack>> inventoryTransaction = new SnapshotParticipant<>() {
        protected List<ItemStack> createSnapshot() { return items.stream().map(ItemStack::copy).toList(); }
        protected void readSnapshot(List<ItemStack> snapshot) { for (int i = 0; i < items.size(); i++) items.set(i, snapshot.get(i).copy()); }
        protected void onFinalCommit() { setChanged(); }
    };
    public DigitalNode(BlockPos pos, BlockState state) { super(DigitalStorage.NODE, pos, state); energy = new SimpleEnergyStorage(kind().equals("dense_energy_cell") ? 1_600_000 : kind().equals("energy_cell") ? 200_000 : 32_000, 4096, 4096, this::setChanged); }
    public String kind() { return ((DigitalNodeBlock) getBlockState().getBlock()).kind; }
    public static boolean poweredKind(String kind) { return Set.of("controller", "energy_acceptor", "me_chest", "energy_cell", "dense_energy_cell", "vibration_chamber", "charger", "inscriber", "crank", "crystal_resonance_generator").contains(kind); }
    public boolean poweredSource() { return poweredKind(kind()); }
    public void owner(UUID value) { owner = value; setChanged(); }
    UUID ownerId(){return owner;}
    public @Nullable ServerPlayer actor() { return level instanceof ServerLevel server ? Authority.answering(server, owner) : null; }
    void snapshotCells(TransactionContext tx) { inventoryTransaction.updateSnapshots(tx); }
    public boolean permitted(ServerPlayer player) {
        if (level != player.level() || isRemoved() || host!=null&&(host.isRemoved()||!host.parts.contains(this)) || player.isSpectator() || !player.isAlive() || !Authority.mayChange(player.level(), player, worldPosition)) return false;
        if (owner == null && player.distanceToSqr(Vec3.atCenterOf(worldPosition)) <= 64) owner(player.getUUID());
        return owner != null && (owner.equals(player.getUUID()) || JugcraftParties.sameParty(owner, player.getUUID()));
    }
    public int cellSlots() { return kind().equals("drive") ? 10 : kind().equals("me_chest") ? 1 : 0; }
    public void filter(ItemStack stack) { filter = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1); setChanged(); }
    public ItemStack filter() { return filter.copy(); }
    public void open(ServerPlayer player) {
        if (!permitted(player)) return;
        if(kind().equals("spatial_anchor")){player.sendOverlayMessage(Component.literal(processingStatus+". After a world restart, visit once to reactivate. Limit: four anchors per owner."));return;}
        if (DigitalQuantum.open(this, player)) return;
        if (kind().endsWith("terminal") && player.isShiftKeyDown() && player.getMainHandItem().isEmpty() && getItem(0).is(DigitalStorage.ITEMS.get("view_cell"))) {
            player.getInventory().placeItemBackInInventory(removeItem(0, 1), net.minecraft.util.Prediction.SERVER_ONLY); player.sendOverlayMessage(Component.literal("View cell removed.")); return;
        }
        if (DigitalLinks.tunnel(kind())) { player.sendOverlayMessage(Component.literal("Memory Card: sneak-use input, then use matching output. Both ends must stay loaded and permitted.")); return; }
        if (kind().equals("pattern_encoding_terminal")) {
            player.openMenu(new SimpleMenuProvider((id, inv, p) -> new PatternEncoding.Menu(id, inv, this), getBlockState().getBlock().getName()));
        } else if (kind().equals("pattern_provider") && player.isShiftKeyDown()) {
            var network=DigitalNetwork.find(this,player);
            for(var root:processing.stream().map(DigitalProcessing.Job::root).distinct().toList())DigitalProcessing.cancelTree(network,root);
            player.sendOverlayMessage(Component.literal("Queued job trees cancelled; already-paid batches finish into storage."));
        } else if (DigitalControls.controls(kind())) {
            player.openMenu(new SimpleMenuProvider((id, inv, p) -> new DigitalControls.Menu(id, inv, this), getBlockState().getBlock().getName()));
        } else if (kind().endsWith("terminal") || kind().equals("me_chest") && !player.isShiftKeyDown()) {
            player.openMenu(new SimpleMenuProvider((id, inv, p) -> new DigitalTerminal.Menu(id, inv, this), Component.literal("ME Storage")));
        } else if (cellSlots() > 0 || DigitalUpgrades.port(kind()) || kind().equals("interface") || kind().equals("pattern_provider") || DigitalWorkshops.hasInventory(kind())) {
            player.openMenu(new SimpleMenuProvider((id, inv, p) -> new DigitalTerminal.DriveMenu(id, inv, this), getBlockState().getBlock().getName()));
        } else if (DigitalProcessors.cpu(kind()) || kind().equals("crafting_monitor")) {
            player.openMenu(new SimpleMenuProvider((id, inv, p) -> new DigitalJobs.Menu(id, inv, this), Component.literal("Crafting jobs")));
        } else player.sendOverlayMessage(Component.literal(poweredSource() ? energy.getAmount() + " / " + energy.getCapacity() + " JE" : "Sneak-use an item to select the transfer filter."));
    }
    public boolean request(DigitalPatterns.Pattern pattern, int count) {
        if (!DigitalProcessors.cpu(kind()) || orders.size() >= DigitalProcessors.queueLimit(kind()) || count < 1 || count > 64) return false;
        var order=new DigitalPatterns.Order(pattern,count);
        if(!(level instanceof ServerLevel)||!DigitalJobLedger.get(this).track(this,order.root()))return false;
        orders.add(order); setChanged(); return true;
    }
    public List<CellContents.Entry> contents() {
        var result = new ArrayList<CellContents.Entry>();
        for (int i = 0; i < cellSlots(); i++) if (items.get(i).getItem() instanceof DigitalStorage.CellItem)
            result.addAll(items.get(i).getOrDefault(DigitalStorage.CONTENTS, CellContents.EMPTY).entries());
        return result;
    }
    public long insert(ItemStack sample, long maximum, TransactionContext tx) {
        if (sample.isEmpty() || maximum <= 0 || sample.getItem() instanceof DigitalStorage.CellItem) return 0;
        long left = Math.min(maximum, Integer.MAX_VALUE);
        for (int i = 0; i < cellSlots() && left > 0; i++) {
            var cell = items.get(i);
            if (!(cell.getItem() instanceof DigitalStorage.CellItem type)) continue;
            if(DigitalCreativeCell.creative(cell))continue;
            if (DigitalWorkshops.filtered(cell, sample)) continue;
            var contents = cell.getOrDefault(DigitalStorage.CONTENTS, CellContents.EMPTY);
            int accepted = contents.accepted(sample.copyWithCount((int) left), type.capacity);
            if (accepted <= 0) { if (DigitalCellCards.voids(cell, sample)) return Math.min(maximum, Integer.MAX_VALUE); continue; }
            inventoryTransaction.updateSnapshots(tx);
            cell.set(DigitalStorage.CONTENTS, contents.add(sample, accepted)); left -= accepted;
            if (left > 0 && DigitalCellCards.voids(cell, sample)) return Math.min(maximum, Integer.MAX_VALUE);
        }
        return Math.min(maximum, Integer.MAX_VALUE) - left;
    }
    public long extract(ItemStack sample, long maximum, TransactionContext tx) {
        if (maximum <= 0) return 0;
        long left = maximum;
        for (int i = 0; i < cellSlots() && left > 0; i++) {
            var cell = items.get(i);
            if (!(cell.getItem() instanceof DigitalStorage.CellItem)) continue;
            var contents = cell.getOrDefault(DigitalStorage.CONTENTS, CellContents.EMPTY);
            int removed = (int) Math.min(left, contents.count(sample));
            if (removed <= 0) continue;
            if(DigitalCreativeCell.creative(cell)){left-=removed;continue;}
            inventoryTransaction.updateSnapshots(tx);
            cell.set(DigitalStorage.CONTENTS, contents.remove(sample, removed)); left -= removed;
        }
        return maximum - left;
    }
    public void tick() {
        if (!(level instanceof ServerLevel server)) return;
        if (kind().equals("inscriber") && server.getGameTime() >= visibleCycle + 16
                && !ItemStack.matches(visibleMiddle, bare(getItem(1)))) renderDirty = true;
        if (renderDirty && Math.floorMod(server.getGameTime() + worldPosition.asLong(), 5) == 0) syncVisibleState();
        if (Math.floorMod(server.getGameTime() + worldPosition.asLong(), 20) != 0) return;
        DigitalPlanes.appearance(this);
        var actor = Authority.answering(server, owner);
        if (actor == null || !permitted(actor)) { DigitalSpatialAnchors.release(this); DigitalControls.update(this, new DigitalNetwork(List.of(), false)); DigitalLinks.reset(this);if(getBlockState().getValue(DigitalNodeBlock.LIT))updateState(getBlockState().setValue(DigitalNodeBlock.LIT,false));return; }
        if(!DigitalJobLedger.get(this).observe(this))return;
        DigitalWorkshops.configureCell(this);
        DigitalUtilityNodes.tick(this);
        DigitalQuantum.tick(this, actor);
        DigitalPowerBridge.tick(this, actor);
        if (!server.hasNeighborSignal(worldPosition)) DigitalWorkshops.generateFuel(this);
        var network = DigitalNetwork.find(this, actor);
        boolean on = network.online();
        if(host!=null&&(kind().equals("toggle_bus")||kind().equals("inverted_toggle_bus")))on&=DigitalCableBlock.toggleOpen(level,worldPosition,kind());
        if(kind().equals("spatial_anchor"))on=DigitalSpatialAnchors.tick(this,network);
        DigitalLinks.tick(this, network, actor);
        DigitalControls.update(this, network);
        DigitalDisplays.tick(this, network);
        if (kind().equals("condenser") && !server.hasNeighborSignal(worldPosition)) DigitalCondenser.tick(this);
        if (getBlockState().getValue(DigitalNodeBlock.LIT) != on) updateState(getBlockState().setValue(DigitalNodeBlock.LIT, on));
        if (getBlockState().hasProperty(DigitalNodeBlock.CHARGE)) {
            int charge = (int) ((energy.getAmount() * 4 + energy.getCapacity() - 1) / energy.getCapacity());
            if (getBlockState().getValue(DigitalNodeBlock.CHARGE) != charge) updateState(getBlockState().setValue(DigitalNodeBlock.CHARGE, charge));
        }
        if (!on || (DigitalUpgrades.port(kind()) ? DigitalUpgrades.paused(this) : server.hasNeighborSignal(worldPosition))) return;
        DigitalFluidAutomation.tick(this, actor);
        DigitalProcessing.tick(this, network, actor);
        DigitalPlanes.tick(this, network, actor);
        beforeWorkInput = bare(getItem(kind().equals("inscriber") ? 1 : 0));
        DigitalWorkshops.work(this, network, actor);
        if (DigitalProcessors.cpu(kind())) { DigitalProcessors.tick(this, network, server); return; }
        if (kind().equals("interface")) {
            for (int slot = 0; slot < items.size(); slot++) {
                var stack = items.get(slot); if (stack.isEmpty()) continue;
                try (Transaction tx = Transaction.openOuter()) {
                    int n = (int) network.insert(stack, Math.min(16, stack.getCount()), tx);
                    if (n > 0) { inventoryTransaction.updateSnapshots(tx); stack.shrink(n); tx.commit(); }
                }
                return;
            }
        }
        if (!kind().equals("import_bus") && !kind().equals("export_bus")) return;
        Direction face = getBlockState().getValue(DigitalNodeBlock.FACING);
        BlockPos target = worldPosition.relative(face);
        if (!server.isLoaded(target) || server.getBlockEntity(target) instanceof DigitalNode || server.getBlockEntity(target) instanceof DigitalFacades.Covers || !Authority.mayChange(server, actor, target)
                || !io.github.jimbozoomer.jugcraft.finance.ClaimTransfers.allowed(server, worldPosition, target)) return;
        Storage<ItemVariant> adjacent = ItemStorage.SIDED.find(server, target, face.getOpposite());
        if (adjacent == null) return;
        if (kind().equals("import_bus")) {
            int inspected = 0;
            for (var view : adjacent) {
                if (++inspected > 128) break;
                if (view.isResourceBlank() || view.getAmount() <= 0) continue;
                var sample = view.getResource().toStack();
                if (!DigitalUpgrades.accepts(this, sample)) continue;
                try (Transaction tx = Transaction.openOuter()) {
                    long n = network.insert(sample, Math.min(DigitalUpgrades.batch(this), view.getAmount()), tx);
                    if (n > 0 && adjacent.extract(view.getResource(), n, tx) == n) { tx.commit(); DigitalProcessing.received(network, target, sample, n); return; }
                }
            }
        } else {
            var samples = DigitalUpgrades.samples(this);
            // An inverted export selects real matching stock rather than fabricating a blacklist item.
            if (DigitalUpgrades.has(this, "inverter_card") || DigitalUpgrades.has(this, "fuzzy_card")) samples = network.contents().stream().map(CellContents.Entry::sample).filter(s -> DigitalUpgrades.accepts(this, s)).limit(9).toList();
            int budget = DigitalUpgrades.batch(this);
            int each = DigitalUpgrades.has(this, "equal_distribution_card") ? Math.max(1, budget / Math.max(1, samples.size())) : budget;
            for (var sample : samples) {
                if (budget <= 0) break;
                boolean missing = true;
                try (Transaction tx = Transaction.openOuter()) {
                    long n = network.extract(sample, Math.min(budget, each), tx);
                    if (n > 0) missing = false;
                    if (n > 0 && adjacent.insert(ItemVariant.of(sample), n, tx) == n) { tx.commit(); budget -= (int) n; }
                }
                if (missing) DigitalUpgrades.requestMissing(this, network, sample);
            }
        }
    }
    /** Trigger only after a real workshop transaction commits; this carries no gameplay authority. */
    public void renderWorkCycle() {
        if (level == null || level.isClientSide() || !kind().equals("inscriber")) return;
        visibleCycle = level.getGameTime(); visibleMiddle = beforeWorkInput; renderDirty = true; syncVisibleState();
    }
    public int visibleCell(int slot) { return visibleCells[Math.clamp(slot, 0, 9)]; }
    public int visibleCellStatus(int slot) { return visibleStatus[Math.clamp(slot, 0, 9)]; }
    public long visibleCellBlink(int slot) { return visibleBlink[Math.clamp(slot, 0, 9)]; }
    public long visibleCycle() { return visibleCycle; }
    public ItemStack visibleTop() { return visibleTop.copy(); }
    public ItemStack visibleMiddle() { return visibleMiddle.copy(); }
    public ItemStack visibleResult() { return visibleResult.copy(); }
    public long visibleCount() { return observed; }
    private static ItemStack bare(ItemStack stack) { return stack.isEmpty() ? ItemStack.EMPTY : new ItemStack(stack.getItem()); }
    void updateState(BlockState state){if(host!=null){setBlockState(state);host.changed();}else if(level!=null)level.setBlock(worldPosition,state,Block.UPDATE_CLIENTS);}
    void updateSignal(BlockState state){
        if(host==null){if(level!=null)level.setBlock(worldPosition,state,Block.UPDATE_ALL);return;}
        updateState(state);DigitalCableBlock.refreshLamp(host);if(level!=null)level.updateNeighborsAt(worldPosition,host.getBlockState().getBlock());
    }
    @Override public void setChanged() { if(host!=null)host.setChanged();else super.setChanged(); renderDirty = true; }
    private void syncVisibleState() {
        if (level == null || level.isClientSide()) return;
        int oldHash = renderHash; refreshVisibleState(); renderDirty = false;
        if (renderHash != oldHash) {if(host!=null)host.changed();else level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);}
    }
    private void refreshVisibleState() {
        if (level == null || level.isClientSide()) return;
        for (int slot = 0; slot < 10; slot++) {
            ItemStack stack = slot < cellSlots() ? getItem(slot) : ItemStack.EMPTY;
            int model = 0, status = 0; long used = 0, capacity = 0; int types = 0;
            if (stack.getItem() instanceof DigitalStorage.CellItem cell) {
                var content = stack.getOrDefault(DigitalStorage.CONTENTS, CellContents.EMPTY);
                used = content.amount(); types = content.entries().size(); capacity = cell.capacity;
            } else if (stack.getItem() instanceof FluidCells.Cell cell) {
                var content = stack.getOrDefault(FluidCells.CONTENTS, FluidCells.Contents.EMPTY);
                used = content.used(); types = content.entries().size(); capacity = cell.capacity;
            }
            if (capacity > 0) {
                String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
                int[] sizes = {1, 4, 16, 64, 256};
                for (int tier = 0; tier < sizes.length; tier++) if (id.endsWith("_" + sizes[tier] + "k")) model = 1 + tier;
                if (stack.getItem() instanceof FluidCells.Cell) model += 5;
                status = used == 0 ? 1 : used >= capacity ? 4 : types >= 63 ? 3 : 2;
                if(DigitalCreativeCell.creative(stack)){model=DigitalCreativeCell.fluid(stack)?6:1;status=contentConfigured(stack)?2:1;}
            }
            if (renderInitialized && model != 0 && model == visibleCells[slot] && used != previousAmounts[slot]) visibleBlink[slot] = level.getGameTime() + 8;
            if (model == 0) visibleBlink[slot] = 0;
            visibleCells[slot] = model; visibleStatus[slot] = status; previousAmounts[slot] = used;
        }
        if (kind().equals("charger")) visibleMiddle = bare(getItem(0).isEmpty() ? getItem(1) : getItem(0));
        if (kind().equals("inscriber")) {
            visibleTop = bare(getItem(0)); visibleResult = bare(getItem(4));
            if (level.getGameTime() >= visibleCycle + 16) visibleMiddle = bare(getItem(1));
        }
        renderInitialized = true;
        if (DigitalDisplays.monitor(kind())) visibleResult = bare(filter);
        renderHash = Objects.hash(Arrays.hashCode(visibleCells), Arrays.hashCode(visibleStatus), Arrays.hashCode(visibleBlink),
                visibleTop.getItem(), visibleMiddle.getItem(), visibleResult.getItem(), visibleCycle, DigitalDisplays.monitor(kind()) ? observed : 0);
    }
    private static boolean contentConfigured(ItemStack stack){return !stack.getOrDefault(DigitalStorage.CONTENTS,CellContents.EMPTY).entries().isEmpty()||DigitalCreativeCell.fluid(stack);}
    @Override public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }
    @Override public boolean triggerEvent(int type, int data) {
        if (kind().equals("crank") && type == 1 && level != null) { crankAt = level.getGameTime(); crankTurns = (crankTurns + 1) % 4; return true; }
        return super.triggerEvent(type, data);
    }
    @Override public net.minecraft.nbt.CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        refreshVisibleState();
        var out = TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING, registries);
        out.putBoolean("render_only", true);
        for (int i = 0; i < 10; i++) { out.putInt("cell_" + i, visibleCells[i]); out.putInt("status_" + i, visibleStatus[i]); out.putLong("blink_" + i, visibleBlink[i]); }
        // Recreate bare stacks before encoding even if a future caller changes the transient view fields.
        out.store("top", ItemStack.OPTIONAL_CODEC, bare(visibleTop)); out.store("middle", ItemStack.OPTIONAL_CODEC, bare(visibleMiddle));
        out.store("result", ItemStack.OPTIONAL_CODEC, bare(visibleResult)); out.putLong("cycle", visibleCycle);
        if (DigitalDisplays.monitor(kind())) { out.store("result", ItemStack.OPTIONAL_CODEC, bare(filter)); out.putLong("display_count", observed); }
        return out.buildResult();
    }
    private void loadRenderState(ValueInput in) {
        for (int i = 0; i < 10; i++) {
            visibleCells[i] = Math.clamp(in.getIntOr("cell_" + i, 0), 0, 10);
            visibleStatus[i] = Math.clamp(in.getIntOr("status_" + i, 0), 0, 4); visibleBlink[i] = in.getLongOr("blink_" + i, 0);
        }
        visibleTop = bare(in.read("top", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
        visibleMiddle = bare(in.read("middle", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
        visibleResult = bare(in.read("result", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
        visibleCycle = in.getLongOr("cycle", -100);
        observed = Math.max(0, in.getLongOr("display_count", 0));
    }
    @Override public int getContainerSize() { return items.size(); }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int slot) { return slot >= 0 && slot < items.size() ? items.get(slot) : ItemStack.EMPTY; }
    @Override public ItemStack removeItem(int slot, int n) { if (slot == 0) DigitalWorkshops.configureCell(this); var result = ContainerHelper.removeItem(items, slot, n); if (!result.isEmpty()) setChanged(); return result; }
    @Override public ItemStack removeItemNoUpdate(int slot) { return ContainerHelper.takeItem(items, slot); }
    @Override public void setItem(int slot, ItemStack stack) { if (slot >= 0 && slot < items.size()) { items.set(slot, stack); setChanged(); } }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return DigitalUpgrades.port(kind()) ? DigitalUpgrades.acceptsSlot(this, slot, stack) : DigitalWorkshops.canInsert(kind(), slot, stack) || kind().equals("interface") || kind().equals("pattern_provider") && (stack.has(DigitalPatterns.PATTERN) || stack.has(DigitalProcessing.PATTERN)) || slot >= 0 && slot < cellSlots() && (stack.getItem() instanceof DigitalStorage.CellItem || stack.getItem() instanceof FluidCells.Cell); }
    @Override public boolean stillValid(Player player) { return player instanceof ServerPlayer sp && level != null && (host==null?level.getBlockEntity(worldPosition)==this:level.getBlockEntity(worldPosition)==host&&host.parts.contains(this)) && player.distanceToSqr(Vec3.atCenterOf(worldPosition)) <= 64 && permitted(sp); }
    @Override public void clearContent() { items.clear(); reservations.clear(); setChanged(); }
    @Override public void setRemoved(){DigitalSpatialAnchors.release(this);super.setRemoved();}
    @Override public void preRemoveSideEffects(BlockPos pos, BlockState state) { if (level != null && !level.isClientSide()) { DigitalSpatialAnchors.release(this);if (kind().equals("quantum_link")) DigitalQuantum.clearFrame(this); DigitalJobLedger.get(this).removed(this); reservations.dropAll();Containers.dropContents(level, pos, this); items.clear(); } }
    @Override protected void collectImplicitComponents(net.minecraft.core.component.DataComponentMap.Builder c) { super.collectImplicitComponents(c); if (poweredSource()) c.set(DigitalWorkshops.ENERGY, energy.getAmount()); if (kind().equals("condenser")) c.set(DigitalCondenser.MATTER, condensed); }
    @Override protected void applyImplicitComponents(net.minecraft.core.component.DataComponentGetter c) { super.applyImplicitComponents(c); energy.setAmount(c.getOrDefault(DigitalWorkshops.ENERGY, 0L)); condensed = c.getOrDefault(DigitalCondenser.MATTER, 0L); }
    @Override protected void loadAdditional(ValueInput in) {
        if (in.getBooleanOr("render_only", false)) { loadRenderState(in); return; }
        super.loadAdditional(in); items = NonNullList.withSize(18, ItemStack.EMPTY); ContainerHelper.loadAllItems(in, items);
        condensed = Math.clamp(in.getLongOr("condensed", 0), 0, 2_097_152L * net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants.BUCKET);
        owner = in.read("owner", UUIDUtil.CODEC).orElse(null); energy.setAmount(in.getLongOr("energy", 0)); burn = Math.clamp(in.getIntOr("burn", 0), 0, 16000);
        linkIdentity = in.read("link_identity", UUIDUtil.CODEC).orElseGet(UUID::randomUUID); peer = in.read("link_peer", DigitalLinks.Peer.CODEC).orElse(null); linkInput = in.getBooleanOr("link_input", false);
        threshold = Math.clamp(in.getIntOr("threshold", 64), 1, 1_000_000); emitBelow = in.getBooleanOr("emit_below", false);
        filter = in.read("filter", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        orders.clear(); orders.addAll(in.read("orders", DigitalPatterns.Order.CODEC.listOf(0, 64)).orElse(List.of())); renderDirty = true;
        processing.clear(); processing.addAll(in.read("processing", DigitalProcessing.Job.CODEC.listOf(0, 16)).orElse(List.of()));
        reservations.load(in);
    }
    @Override protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out); out.putInt("schema", 1); ContainerHelper.saveAllItems(out, items);
        out.putLong("condensed", condensed);
        if (owner != null) out.store("owner", UUIDUtil.CODEC, owner);
        out.store("link_identity", UUIDUtil.CODEC, linkIdentity); if (peer != null) out.store("link_peer", DigitalLinks.Peer.CODEC, peer); out.putBoolean("link_input", linkInput);
        out.putInt("burn", burn); out.putLong("energy", energy.getAmount()); out.putInt("threshold", threshold); out.putBoolean("emit_below", emitBelow);
        out.store("filter", ItemStack.OPTIONAL_CODEC, filter); out.store("orders", DigitalPatterns.Order.CODEC.listOf(0, 64), List.copyOf(orders));
        out.store("processing", DigitalProcessing.Job.CODEC.listOf(0, 16), List.copyOf(processing));
        reservations.save(out);
    }
}
