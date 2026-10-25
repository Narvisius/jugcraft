package io.github.jimbozoomer.jugcraft.jimtafacts.network;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.fluid.*;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.transfer.v1.fluid.*;
import net.fabricmc.fabric.api.transfer.v1.fluid.base.SingleFluidStorage;
import net.minecraft.core.*;
import net.minecraft.core.component.*;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.BlockHitResult;

/** Source36-slot single sky chests and16-bucket tank, backed by actual native containers. */
public final class DigitalUtilityStorage {
    public static BlockEntityType<Chest> CHEST;
    public static BlockEntityType<Tank> TANK;
    public static final String[] BLOCKS = {"sky_stone_chest", "smooth_sky_stone_chest", "sky_stone_tank"};
    private DigitalUtilityStorage() {}
    public static void register() {
        for (var name : BLOCKS) {
            var key = ResourceKey.create(Registries.BLOCK, Jugcraft.id("jimta_" + name));
            var properties = BlockBehaviour.Properties.ofFullCopy(Blocks.STONE).strength(3, 1200).noOcclusion().setId(key);
            Block block = name.endsWith("chest") ? new SkyChest(properties) : new TankBlock(properties);
            Registry.register(BuiltInRegistries.BLOCK, key, block); DigitalStorage.BLOCKS.put(name, block);
            var itemKey = ResourceKey.create(Registries.ITEM, key.identifier()); Registry.register(BuiltInRegistries.ITEM, itemKey, new BlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));
        }
        CHEST = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("jimta_sky_chest"), FabricBlockEntityTypeBuilder.create(Chest::new, DigitalStorage.BLOCKS.get(BLOCKS[0]), DigitalStorage.BLOCKS.get(BLOCKS[1])).build());
        TANK = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("jimta_sky_tank"), FabricBlockEntityTypeBuilder.create(Tank::new, DigitalStorage.BLOCKS.get(BLOCKS[2])).build());
        FluidStorage.SIDED.registerForBlockEntity((tank, side) -> tank.storage, TANK);
    }
    public static final class SkyChest extends ChestBlock {
        SkyChest(Properties p) { super(() -> CHEST, SoundEvents.CHEST_OPEN, SoundEvents.CHEST_CLOSE, p); }
        @Override public BlockEntity newBlockEntity(BlockPos p, BlockState s) { return new Chest(p, s); }
        @Override protected RenderShape getRenderShape(BlockState s) { return RenderShape.INVISIBLE; }
        @Override public BlockState getStateForPlacement(BlockPlaceContext c) { var state = super.getStateForPlacement(c); return state == null ? null : state.setValue(ChestBlock.TYPE, ChestType.SINGLE); }
        @Override protected BlockState updateShape(BlockState s, LevelReader l, ScheduledTickAccess ticks, BlockPos p, Direction d, BlockPos n, BlockState other, RandomSource r) { return super.updateShape(s, l, ticks, p, d, n, other, r).setValue(ChestBlock.TYPE, ChestType.SINGLE); }
        @Override protected InteractionResult useWithoutItem(BlockState s, Level l, BlockPos p, Player player, BlockHitResult hit) { if (player instanceof ServerPlayer actor && !Authority.mayChange(actor.level(), actor, p)) return InteractionResult.FAIL; return super.useWithoutItem(s, l, p, player, hit); }
    }
    public static final class Chest extends ChestBlockEntity {
        public Chest(BlockPos p, BlockState s) { super(CHEST, p, s); setItems(NonNullList.withSize(36, ItemStack.EMPTY)); }
        @Override public int getContainerSize() { return 36; }
        @Override protected Component getDefaultName() { return getBlockState().getBlock().getName(); }
        @Override protected AbstractContainerMenu createMenu(int id, Inventory inv) { return new ChestMenu(MenuType.GENERIC_9x4, id, inv, this, 4); }
        @Override public boolean stillValid(Player player) { return super.stillValid(player) && (!(player instanceof ServerPlayer p) || Authority.mayChange(p.level(), p, worldPosition)); }
    }
    public static final class TankBlock extends Block implements EntityBlock, FluidConnectable {
        TankBlock(Properties p) { super(p); }
        @Override public BlockEntity newBlockEntity(BlockPos p, BlockState s) { return new Tank(p, s); }
        @Override protected InteractionResult useItemOn(ItemStack stack, BlockState s, Level l, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
            if (player instanceof ServerPlayer p && Authority.mayChange(p.level(), p, pos) && l.getBlockEntity(pos) instanceof Tank tank && FluidStorageUtil.interactWithFluidStorage(tank.storage, player, hand)) return InteractionResult.SUCCESS;
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        @Override protected InteractionResult useWithoutItem(BlockState s, Level l, BlockPos pos, Player player, BlockHitResult hit) {
            if (player instanceof ServerPlayer p && Authority.mayChange(p.level(), p, pos) && l.getBlockEntity(pos) instanceof Tank tank) p.sendOverlayMessage(Component.literal(tank.storage.amount / 81 + " / 16,000 mB · ").append(tank.storage.isResourceBlank() ? Component.literal("Empty") : FluidVariantAttributes.getName(tank.storage.variant)));
            return InteractionResult.SUCCESS;
        }
        @Override protected boolean hasAnalogOutputSignal(BlockState s) { return true; }
        @Override protected int getAnalogOutputSignal(BlockState s, Level l, BlockPos pos, Direction d) { return l.getBlockEntity(pos) instanceof Tank t ? (int) Math.ceil(t.storage.amount * 15.0 / t.storage.getCapacity()) : 0; }
    }
    public static final class Tank extends BlockEntity {
        public final SingleFluidStorage storage = SingleFluidStorage.withFixedCapacity(16 * FluidConstants.BUCKET, this::changed);
        public Tank(BlockPos p, BlockState s) { super(TANK, p, s); }
        private void changed() { setChanged(); if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS); }
        @Override public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() { return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this); }
        @Override public net.minecraft.nbt.CompoundTag getUpdateTag(HolderLookup.Provider lookups) { return saveWithoutMetadata(lookups); }
        @Override protected void loadAdditional(ValueInput in) { super.loadAdditional(in); storage.readValue(in); }
        @Override protected void saveAdditional(ValueOutput out) { super.saveAdditional(out); storage.writeValue(out); }
        @Override protected void collectImplicitComponents(DataComponentMap.Builder b) { super.collectImplicitComponents(b); var stored = StoredFluid.of(storage); if (stored != null) b.set(JugcraftFluids.STORED_FLUID, stored); }
        @Override protected void applyImplicitComponents(DataComponentGetter components) { super.applyImplicitComponents(components); var stored = components.get(JugcraftFluids.STORED_FLUID); if (stored != null) stored.restore(storage); }
    }
}
