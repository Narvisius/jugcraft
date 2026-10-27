package io.github.jimbozoomer.jugcraft.workshop;

import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import io.github.jimbozoomer.jugcraft.machine.MachineInput;
import io.github.jimbozoomer.jugcraft.machine.form.WorkLane;
import java.util.*;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.*;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CookingFuel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt;
import org.jspecify.annotations.Nullable;

/** Ordinary furnace fuel plus the shared durable work lane. Packets supply presence, never progress. */
public final class WorkshopBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer, ExtendedMenuProvider<WorkshopCatalog> {
    public static final int TOOL = 3, FUEL = 4, OUTPUT = 5, LINING = 6, CRUCIBLE = 7, INSERT = 8, SLOTS = 9, DATA_COUNT = 9;
    public final local.peepo.WorkshopCompanion companion = new local.peepo.WorkshopCompanion(this);
    private NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
    private @Nullable WorkLane lane;
    private @Nullable Identifier selected;
    private ItemStack requiredTool = ItemStack.EMPTY;
    private boolean heated, highHeat;
    private int fuel;
    private @Nullable ServerPlayer worker;
    private long heartbeat = Long.MIN_VALUE;
    public WorkshopBlockEntity(BlockPos pos, BlockState state) { super(JugcraftWorkshop.ENTITY, pos, state); }
    public int station() { return ((WorkshopBlock) getBlockState().getBlock()).station; }
    public boolean busy() { return lane != null; }
    public int progress() { return lane == null ? 0 : lane.progress(); }
    public int fuelCredit() { return fuel; }
    public @Nullable WorkLane lane() { return lane; }
    public WorkshopCatalog catalog() { return WorkshopCatalog.get(((ServerLevel) level).getServer(), station()); }
    public WorkshopCatalog.@Nullable Entry selection() {
        var catalog = catalog(); int index = catalog.index(selected);
        return catalog.entries().isEmpty() ? null : catalog.entries().get(Math.max(0, index));
    }
    public boolean select(Identifier id) {
        if (busy() || catalog().index(id) < 0) return false;
        selected = id; setChanged(); return true;
    }
    public boolean mayOperate(Player player) {
        if (level == null || isRemoved() || !player.isAlive() || !super.stillValid(player)) return false;
        var block = (WorkshopBlock) getBlockState().getBlock();
        if (!block.complete(level, worldPosition, getBlockState()) || !block.mayUse(level, worldPosition, getBlockState(), player)) return false;
        if (station()==2 && needsAir() && bellows()!=null) {
            BlockPos p=bellows().getBlockPos();
            return io.github.jimbozoomer.jugcraft.placement.StructurePermissions.mayChange(level,p,player)
                && (!(level instanceof ServerLevel s) || io.github.jimbozoomer.jugcraft.concordance.Authority.mayChange(s,player,p));
        }
        return true;
    }
    @Override public boolean stillValid(Player player) { return mayOperate(player); }
    public boolean pulse(ServerPlayer player) {
        if ((station() != 0 && station() != 2) || !mayOperate(player) || !(player.containerMenu instanceof WorkshopMenu menu) || menu.machine() != this) return false;
        long now = level.getGameTime();
        if (worker != null && worker != player && working(now)) return false;
        worker = player; heartbeat = now; return true;
    }
    public void release(Player player) { if (worker == player) { worker = null; heartbeat = Long.MIN_VALUE; } }
    public boolean playerWorking() { return level != null && working(level.getGameTime()); }
    private boolean working(long now) {
        return worker != null && now >= heartbeat && now - heartbeat <= 2 && worker.level() == level
            && worker.containerMenu instanceof WorkshopMenu menu && menu.machine() == this && menu.currentCatalog() && mayOperate(worker);
    }
    public boolean needsAir() { return lane == null ? selection()!=null && selection().recipe().highHeat() : highHeat; }
    public @Nullable BellowsBlockEntity bellows() {
        for (Direction side : Direction.Plane.HORIZONTAL) {
            BlockPos p=worldPosition.relative(side);
            if(level.hasChunkAt(p) && level.getBlockEntity(p) instanceof BellowsBlockEntity b
                    && b.getBlockState().getValue(BellowsBlock.FACING)==side.getOpposite()) return b;
        }
        return null;
    }
    public boolean wantsShaftAir() { return needsAir() && !working(level.getGameTime()) && readiness()==0; }
    public int airflow() { return !needsAir() || bellows()==null ? 0 : working(level.getGameTime()) ? 1 : bellows().hasAir() ? 2 : 0; }
    private int displayedAirflow() { int ready=airflow(); var b=bellows(); return ready!=0 || !needsAir() || b==null ? ready : b.currentStroke(); }
    private int displayedStatus() { int ready=status(); return ready==11 && displayedAirflow()!=0 || ready==6 && companion.job.operating() ? 0 : ready; }
    private MachineInput inputs() { return new MachineInput(List.of(items.get(0), items.get(1), items.get(2))); }
    private boolean fits(int slot, ItemStack stack) {
        ItemStack current = items.get(slot);
        return current.isEmpty() ? stack.getCount() <= stack.getMaxStackSize()
            : ItemStack.isSameItemSameComponents(current, stack) && current.getCount() + stack.getCount() <= current.getMaxStackSize();
    }
    private boolean installed() { return station() != 2 || items.get(LINING).is(JugcraftWorkshop.ITEMS.get("basic_ceramic_lining"))
        && items.get(CRUCIBLE).is(JugcraftWorkshop.ITEMS.get("ceramic_crucible")); }
    public int fuelTicks(ItemStack stack) {
        return level instanceof ServerLevel server ? Math.clamp(ResolvableInt.getFromItem(stack, DataComponents.COOKING_FUEL,
            CookingFuel::burnTime, getLootContext(server), 0), 0, 1_000_000) : 0;
    }
    private boolean hasHeat() { return fuel > 0 || fuelTicks(items.get(FUEL)) > 0 && remainderFits(); }
    private boolean remainderFits() {
        var remainder = items.get(FUEL).getItem().getCraftingRemainder();
        return remainder == null || remainder.create().isEmpty() || items.get(FUEL).getCount() == 1;
    }
    private boolean spendHeat() {
        if (fuel <= 0) {
            int value = fuelTicks(items.get(FUEL));
            if (value <= 0 || !remainderFits()) return false;
            var remainder = items.get(FUEL).getItem().getCraftingRemainder();
            items.get(FUEL).shrink(1);
            if (items.get(FUEL).isEmpty() && remainder != null) items.set(FUEL, remainder.create());
            fuel = value;
        }
        fuel--; return true;
    }
    /** Status: ready, ingredients, tool, installed ceramics, fuel, output, hold Work, disabled, incomplete. */
    public int status() {
        int ready=readiness();
        if(ready!=0)return ready;
        if(needsAir() && airflow()==0)return 11;
        if(station()==0 && !working(level.getGameTime()))return 6;
        return 0;
    }
    private int readiness() {
        if (!JugcraftConfig.isFeatureEnabled("machines")) return 7;
        if (!((WorkshopBlock) getBlockState().getBlock()).complete(level, worldPosition, getBlockState())) return 8;
        if (!installed()) return 3;
        var choice = selection();
        if (lane == null && (choice == null || choice.recipe().take(inputs()) == null)) return 1;
        ItemStack tool = lane == null ? choice.recipe().toolStack() : requiredTool;
        if (!tool.isEmpty() && !ItemStack.isSameItemSameComponents(tool, items.get(TOOL))) return 2;
        ItemStack result = lane == null ? choice.recipe().result().create() : lane.itemResults().getFirst().stack();
        if (!fits(OUTPUT, result)) return 5;
        if ((lane == null ? choice.recipe().heated() : heated) && !hasHeat()) return 4;
        if(needsAir()) {
            if(!JugcraftConfig.isFeatureEnabled("silicon"))return 7;
            if(!items.get(INSERT).is(JugcraftWorkshop.ITEMS.get("crucible_heat_insert")))return 9;
            if(bellows()==null)return 10;
        }
        return 0;
    }
    public void tick(ServerLevel server) {
        boolean upgraded=station()==2 && items.get(INSERT).is(JugcraftWorkshop.ITEMS.get("crucible_heat_insert"));
        if(station()==2) for(int part=0;part<2;part++) {
            BlockPos p=worldPosition.above(part); var s=server.getBlockState(p);
            if(s.is(getBlockState().getBlock()) && s.getValue(WorkshopBlock.UPGRADED)!=upgraded)
                server.setBlock(p,s.setValue(WorkshopBlock.UPGRADED,upgraded),Block.UPDATE_CLIENTS);
        }
        int ready = status();
        boolean active = ready == 0 || ready == 6 && companion.job.consumeWork();
        if (active) {
            if (lane == null) {
                var choice = selection(); var recipe = choice.recipe(); int[] take = recipe.take(inputs());
                var reserved = new ArrayList<WorkLane.ItemPart>();
                for (int i = 0; i < 3; i++) if (take[i] > 0) reserved.add(new WorkLane.ItemPart(i, items.get(i).split(take[i])));
                requiredTool = recipe.toolStack(); heated = recipe.heated(); highHeat = recipe.highHeat(); selected = choice.id();
                lane = new WorkLane(choice.id(), reserved, List.of(), List.of(new WorkLane.ItemPart(OUTPUT, recipe.result().create())),
                    List.of(), recipe.time(), requiredTool.isEmpty() ? -1 : TOOL, false, 0);
                if(highHeat) {
                    var b=bellows(); var p=b==null ? worldPosition : b.getBlockPos();
                    server.playSound(null,p,SoundEvents.FIRE_EXTINGUISH,SoundSource.BLOCKS,0.5F,0.65F);
                }
                // One cue for each newly committed manual batch, never for resuming partial work.
                if (station() == 0 && playerWorking()) {
                    boolean soldering = requiredTool.is(JugcraftWorkshop.ITEMS.get("soldering_tool"));
                    var sound = requiredTool.is(JugcraftWorkshop.ITEMS.get("wire_cutters")) ? SoundEvents.SHEEP_SHEAR
                        : soldering ? SoundEvents.FIRE_EXTINGUISH : SoundEvents.ANVIL_HIT;
                    worker.connection.send(new ClientboundSoundPacket(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound),
                        SoundSource.BLOCKS, worker.getX(), worker.getEyeY(), worker.getZ(), 0.5F,
                        soldering ? 1.6F : 1.15F, server.getRandom().nextLong()));
                }
            }
            if (!heated || spendHeat()) {
                if(highHeat) bellows().worked(working(server.getGameTime()));
                lane.advance(1);
                if (lane.progress() >= lane.duration()) {
                    var result = lane.itemResults().getFirst().stack();
                    if (items.get(OUTPUT).isEmpty()) items.set(OUTPUT, result.copy()); else items.get(OUTPUT).grow(result.getCount());
                    lane = null; requiredTool = ItemStack.EMPTY;
                }
                setChanged();
            }
        }
        if (getBlockState().getValue(WorkshopBlock.LIT) != active)
            server.setBlock(worldPosition, getBlockState().setValue(WorkshopBlock.LIT, active), Block.UPDATE_ALL);
    }
    public boolean cancel() {
        if (lane == null || lane.inputs().stream().anyMatch(p -> !fits(p.slot(), p.stack()))) return false;
        for (var part : lane.inputs()) {
            if (items.get(part.slot()).isEmpty()) items.set(part.slot(), part.stack().copy());
            else items.get(part.slot()).grow(part.stack().getCount());
        }
        lane = null; requiredTool = ItemStack.EMPTY; worker = null; setChanged(); return true;
    }
    public boolean locked(int slot) { return busy() && (slot == TOOL || slot == LINING || slot == CRUCIBLE || slot == INSERT); }
    private boolean acceptsInput(int slot, ItemStack stack) {
        if (slot < 0 || slot >= 3) return false;
        if (level == null || level.isClientSide()) return true;
        var choice = selection();
        if (choice == null) return false;
        for (var part : choice.recipe().parts()) {
            if (!part.ingredient().test(stack)) continue;
            // Reserve different input slots for different ingredients, so one hopper's copper cannot
            // fill every slot and prevent the tin/zinc from entering the selected alloy batch.
            boolean duplicate = false;
            for (int i = 0; i < 3; i++) if (i != slot && part.ingredient().test(items.get(i))) duplicate = true;
            if (!duplicate) return true;
        }
        return false;
    }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) {
        if (locked(slot)) return false;
        return switch (slot) {
            case TOOL -> station() == 0 && (stack.is(JugcraftWorkshop.ITEMS.get("workshop_hammer")) || stack.is(JugcraftWorkshop.ITEMS.get("wire_cutters")) || stack.is(JugcraftWorkshop.ITEMS.get("soldering_tool")));
            case FUEL -> stack.has(DataComponents.COOKING_FUEL);
            case LINING -> station() == 2 && stack.is(JugcraftWorkshop.ITEMS.get("basic_ceramic_lining"));
            case CRUCIBLE -> station() == 2 && stack.is(JugcraftWorkshop.ITEMS.get("ceramic_crucible"));
            case INSERT -> station() == 2 && stack.is(JugcraftWorkshop.ITEMS.get("crucible_heat_insert"));
            case OUTPUT -> false;
            default -> acceptsInput(slot, stack);
        };
    }
    @Override public int[] getSlotsForFace(Direction side) { return side == Direction.DOWN ? new int[]{OUTPUT, FUEL} : side == Direction.UP ? new int[]{0, 1, 2} : new int[]{FUEL, 0, 1, 2}; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) { return (slot < 3 || slot == FUEL) && canPlaceItem(slot, stack); }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return slot == OUTPUT || slot == FUEL && !stack.has(DataComponents.COOKING_FUEL); }
    @Override protected NonNullList<ItemStack> getItems() { return items; }
    @Override protected void setItems(NonNullList<ItemStack> items) { this.items = items; }
    @Override public int getContainerSize() { return SLOTS; }
    @Override protected Component getDefaultName() { return Component.translatable("block.jugcraft." + JugcraftWorkshop.STATIONS[station()]); }
    @Override public WorkshopCatalog getScreenOpeningData(ServerPlayer player) { return catalog(); }
    @Override protected AbstractContainerMenu createMenu(int id, Inventory inventory) { return new WorkshopMenu(id, inventory, this, data(), catalog()); }
    private ContainerData data() { return new ContainerData() {
        public int get(int i) { return switch (i) {
            case 0 -> progress(); case 1 -> lane == null ? selection() == null ? 1 : selection().recipe().time() : lane.duration();
            case 2 -> fuel & 65535; case 3 -> fuel >>> 16; case 4 -> displayedStatus();
            case 5 -> Math.max(0, catalog().index(selected)); case 6 -> busy() ? 1 : 0; case 8 -> displayedAirflow(); default -> station();
        }; }
        public void set(int i, int value) { }
        public int getCount() { return DATA_COUNT; }
    }; }
    @Override public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level == null) return;
        companion.job.removed();
        Containers.dropContents(level, pos, items); items.clear();
        if (lane != null) for (var part : lane.inputs()) Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), part.stack().copy());
        lane = null;
    }
    @Override protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out); ContainerHelper.saveAllItems(out, items); companion.filter.save(out);
        out.putInt("workshop_version", 1); out.putInt("heat", fuel); out.putBoolean("heated", heated); out.putBoolean("high_heat", highHeat);
        if (selected != null) out.store("selected", Identifier.CODEC, selected);
        if (lane != null) out.store("work_lane", WorkLane.CODEC, lane);
        if (!requiredTool.isEmpty()) out.store("required_tool", ItemStack.CODEC, requiredTool);
    }
    @Override protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in); items = NonNullList.withSize(SLOTS, ItemStack.EMPTY); ContainerHelper.loadAllItems(in, items); companion.filter.load(in);
        fuel = Math.clamp(in.getIntOr("heat", 0), 0, 1_000_000); heated = in.getBooleanOr("heated", false); highHeat = in.getBooleanOr("high_heat", false);
        selected = in.read("selected", Identifier.CODEC).orElse(null);
        lane = in.read("work_lane", WorkLane.CODEC).orElse(null);
        requiredTool = in.read("required_tool", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        worker = null; heartbeat = Long.MIN_VALUE;
    }
}
