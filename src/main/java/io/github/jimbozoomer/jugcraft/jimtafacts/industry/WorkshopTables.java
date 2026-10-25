package io.github.jimbozoomer.jugcraft.jimtafacts.industry;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.energy.*;
import io.github.jimbozoomer.jugcraft.logistics.ItemConnectable;
import io.github.jimbozoomer.jugcraft.party.JugcraftParties;
import io.github.jimbozoomer.jugcraft.tools.Chargeable;
import java.util.*;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.menu.v1.*;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.transfer.v1.item.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;
import org.jspecify.annotations.Nullable;

/** Original workshop tables with finite JE charging and explicit, reusable robot programs. */
public final class WorkshopTables {
    public static final Map<String, Block> BLOCKS = new LinkedHashMap<>();
    public static BlockEntityType<Node> TYPE;
    public static ExtendedMenuType<Menu, BlockPos> MENU;
    public static Item BLANK_BOARD;
    private WorkshopTables() {}

    public static void register() {
        for (String name : List.of("charging", "programming")) {
            var key = ResourceKey.create(Registries.BLOCK, Jugcraft.id("jimta_" + name + "_table"));
            var block = Registry.register(BuiltInRegistries.BLOCK, key, new Table(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion().setId(key), name));
            BLOCKS.put(name, block);
            var item = ResourceKey.create(Registries.ITEM, key.identifier());
            Registry.register(BuiltInRegistries.ITEM, item, new BlockItem(block,
                    new Item.Properties().setId(item).useBlockDescriptionPrefix()));
        }
        var key = ResourceKey.create(Registries.ITEM, Jugcraft.id("jimta_blank_robot_board"));
        BLANK_BOARD = Registry.register(BuiltInRegistries.ITEM, key, new Item(new Item.Properties().setId(key)));
        TYPE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("jimta_workshop_table"),
                FabricBlockEntityTypeBuilder.create(Node::new, BLOCKS.values().toArray(Block[]::new)).build());
        MENU = Registry.register(BuiltInRegistries.MENU, Jugcraft.id("jimta_workshop_table"),
                new ExtendedMenuType<>(Menu::new, BlockPos.STREAM_CODEC));
        EnergyStorage.SIDED.registerForBlockEntity((node, side) -> node.energy, TYPE);
        ItemStorage.SIDED.registerForBlockEntity((node, side) -> ContainerStorage.of(node, side), TYPE);
        PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, entity) ->
                !(entity instanceof Node node) || node.identityAllowed(player));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(out -> {
            BLOCKS.values().forEach(out::accept); out.accept(BLANK_BOARD);
        });
    }

    static final class Table extends Block implements EntityBlock, EnergyConnectable, ItemConnectable {
        final String kind;
        Table(Properties properties, String kind) { super(properties); this.kind = kind; }
        @Override protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
            return Block.box(0, 0, 0, 16, 9, 16);
        }
        @Override public BlockEntity newBlockEntity(BlockPos p, BlockState s) { return new Node(p, s); }
        @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l, BlockState s, BlockEntityType<T> type) {
            return !l.isClientSide() && type == TYPE ? (world, p, state, entity) -> {
                if (world.getGameTime() % 5 == 0) ((Node) entity).step();
            } : null;
        }
        @Override public void setPlacedBy(Level level, BlockPos p, BlockState s, LivingEntity placer, ItemStack stack) {
            if (placer != null && level.getBlockEntity(p) instanceof Node node) { node.owner = placer.getUUID(); node.setChanged(); }
        }
        @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos p, Player player, BlockHitResult hit) {
            if (level instanceof ServerLevel && level.getBlockEntity(p) instanceof Node node) {
                if (!node.permitted(player)) return InteractionResult.FAIL;
                player.openMenu(node);
            }
            return InteractionResult.SUCCESS;
        }
    }

    public static final class Node extends BaseContainerBlockEntity implements WorldlyContainer, ExtendedMenuProvider<BlockPos> {
        private NonNullList<ItemStack> items = NonNullList.withSize(2, ItemStack.EMPTY);
        public UUID owner;
        public int selected, status;
        // Programming costs 2,000 JE atomically. Charging transfers at most 640 JE per five ticks.
        public final SimpleEnergyStorage energy = new SimpleEnergyStorage(40_000, 512, 0, this::setChanged);
        private final ContainerData data = new ContainerData() {
            public int get(int i) { return switch (i) {
                case 0 -> selected; case 1 -> (int) energy.getAmount(); case 2 -> status;
                case 3 -> (int) Math.min(1000, Chargeable.energy(items.get(0)) * 1000 / Math.max(1, Chargeable.capacity(items.get(0))));
                default -> 0;
            }; }
            public void set(int i, int v) {} public int getCount() { return 4; }
        };
        public Node(BlockPos p, BlockState s) { super(TYPE, p, s); }
        public boolean programming() { return ((Table) getBlockState().getBlock()).kind.equals("programming"); }
        public boolean identityAllowed(Player player) {
            return owner == null || owner.equals(player.getUUID()) || JugcraftParties.sameParty(owner, player.getUUID());
        }
        public boolean permitted(Player player) {
            return !isRemoved() && level != null && level.getBlockEntity(worldPosition) == this && player.isAlive()
                    && !player.isSpectator() && identityAllowed(player) && Authority.mayChange(level, player, worldPosition);
        }
        private boolean automated() {
            var actor = level instanceof ServerLevel server ? Authority.answering(server, owner) : null;
            return actor != null && permitted(actor);
        }
        @Override public boolean stillValid(Player player) { return super.stillValid(player) && permitted(player); }
        @Override protected Component getDefaultName() { return getBlockState().getBlock().getName(); }
        @Override protected NonNullList<ItemStack> getItems() { return items; }
        @Override protected void setItems(NonNullList<ItemStack> value) { items = value; }
        @Override public int getContainerSize() { return 2; }
        @Override public BlockPos getScreenOpeningData(ServerPlayer player) { return worldPosition; }
        @Override protected AbstractContainerMenu createMenu(int id, Inventory player) { return new Menu(id, player, worldPosition, this, data, programming()); }
        @Override public boolean canPlaceItem(int slot, ItemStack stack) {
            return slot == 0 && (programming() ? stack.is(BLANK_BOARD) || WorkshopRobots.program(stack) != null
                    : stack.getCount() == 1 && stack.getItem() instanceof Chargeable);
        }
        @Override public int[] getSlotsForFace(Direction side) { return side == Direction.DOWN ? new int[]{programming() ? 1 : 0} : new int[]{0}; }
        @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
            return automated() && side != Direction.DOWN && canPlaceItem(slot, stack);
        }
        @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
            return automated() && (programming() ? slot == 1 : slot == 0 && stack.getItem() instanceof Chargeable
                    && Chargeable.energy(stack) >= Chargeable.capacity(stack));
        }
        public void step() {
            status = 0;
            if (!automated()) { status = 1; return; }
            if (level.hasNeighborSignal(worldPosition)) { status = 2; return; }
            var input = items.get(0);
            if (input.isEmpty() || !canPlaceItem(0, input)) { status = 3; return; }
            if (programming()) {
                var result = new ItemStack(WorkshopRobots.BOARDS.get(WorkshopRobots.PROGRAMS.get(selected)));
                if (input.is(result.getItem())) { status = 4; return; }
                var output = items.get(1);
                if (!output.isEmpty() && (!ItemStack.isSameItemSameComponents(output, result) || output.getCount() >= output.getMaxStackSize())) { status = 5; return; }
                if (energy.getAmount() < 2000) { status = 6; return; }
                input.shrink(1);
                if (output.isEmpty()) items.set(1, result); else output.grow(1);
                energy.setAmount(energy.getAmount() - 2000);
            } else {
                if (Chargeable.energy(input) >= Chargeable.capacity(input)) { status = 4; return; }
                long moved = Chargeable.charge(input, Math.min(640, energy.getAmount()));
                if (moved <= 0) { status = 6; return; }
                energy.setAmount(energy.getAmount() - moved);
            }
            status = 7; setChanged();
        }
        @Override public void preRemoveSideEffects(BlockPos p, BlockState s) { if (level != null) Containers.dropContents(level, p, this); clearContent(); }
        @Override protected void saveAdditional(ValueOutput out) {
            super.saveAdditional(out); ContainerHelper.saveAllItems(out, items);
            out.putLong("Energy", energy.getAmount()); out.putInt("Program", selected);
            if (owner != null) out.store("Owner", UUIDUtil.CODEC, owner);
        }
        @Override protected void loadAdditional(ValueInput in) {
            super.loadAdditional(in); items = NonNullList.withSize(2, ItemStack.EMPTY); ContainerHelper.loadAllItems(in, items);
            energy.setAmount(in.getLongOr("Energy", 0)); selected = Math.clamp(in.getIntOr("Program", 0), 0, WorkshopRobots.PROGRAMS.size() - 1);
            owner = in.read("Owner", UUIDUtil.CODEC).orElse(null);
        }
    }

    public static final class Menu extends AbstractContainerMenu {
        public final boolean programming;
        public final ContainerData data;
        private final Container inventory;
        private final BlockPos pos;
        public Menu(int id, Inventory player, BlockPos pos) {
            this(id, player, pos, new SimpleContainer(2), new SimpleContainerData(4),
                    player.player.level().getBlockState(pos).is(BLOCKS.get("programming")));
        }
        Menu(int id, Inventory player, BlockPos pos, Container inventory, ContainerData data, boolean programming) {
            super(MENU, id); this.pos = pos; this.inventory = inventory; this.data = data; this.programming = programming; addDataSlots(data);
            addSlot(new Slot(inventory, 0, programming ? 8 : 80, programming ? 36 : 18) {
                @Override public boolean mayPlace(ItemStack stack) { return inventory instanceof Node node ? node.canPlaceItem(0, stack)
                        : programming ? stack.is(BLANK_BOARD) || WorkshopRobots.program(stack) != null : stack.getItem() instanceof Chargeable; }
                @Override public int getMaxStackSize() { return programming ? 64 : 1; }
            });
            if (programming) addSlot(new Slot(inventory, 1, 8, 90) { @Override public boolean mayPlace(ItemStack s) { return false; } });
            int y = programming ? 123 : 50;
            for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) addSlot(new Slot(player, 9 + row * 9 + col, 8 + col * 18, y + row * 18));
            for (int col = 0; col < 9; col++) addSlot(new Slot(player, col, 8 + col * 18, y + 58));
        }
        @Override public boolean stillValid(Player p) { return p.level().isClientSide() || p.level().getBlockEntity(pos) == inventory && inventory.stillValid(p); }
        @Override public boolean clickMenuButton(Player p, int id) {
            if (!programming || id < 0 || id >= WorkshopRobots.PROGRAMS.size() || !stillValid(p) || !(inventory instanceof Node node)) return false;
            node.selected = id; node.setChanged(); broadcastChanges(); return true;
        }
        @Override public ItemStack quickMoveStack(Player p, int i) {
            if (!stillValid(p) || i < 0 || i >= slots.size()) return ItemStack.EMPTY;
            var slot = slots.get(i); if (!slot.hasItem()) return ItemStack.EMPTY;
            var stack = slot.getItem(); var result = stack.copy(); int own = programming ? 2 : 1;
            if (i < own ? !moveItemStackTo(stack, own, slots.size(), true) : !moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
            if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
            return result;
        }
    }
}
