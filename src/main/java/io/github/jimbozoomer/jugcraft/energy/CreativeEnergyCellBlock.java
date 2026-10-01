package io.github.jimbozoomer.jugcraft.energy;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The Creative Energy Cell: an endless source of power for testing builds. It pushes up to 1,000,000 JE a
 * tick into every neighbour and cable on all six sides, and swallows anything put into it. It has no recipe;
 * only creative players can place it, and it cannot be broken in survival.
 */
public class CreativeEnergyCellBlock extends BaseEntityBlock implements EnergyConnectable {
	public static final long OUTPUT_PER_TICK = 1_000_000;
	public static Block BLOCK;
	public static BlockEntityType<Entity> ENTITY;

	public CreativeEnergyCellBlock(Properties properties) {
		super(properties);
	}

	public static void register() {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Jugcraft.id("creative_energy_cell"));
		BLOCK = Registry.register(BuiltInRegistries.BLOCK, key, new CreativeEnergyCellBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BEDROCK)
				.setId(key).lightLevel(state -> 10)));
		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Jugcraft.id("creative_energy_cell"));
		Registry.register(BuiltInRegistries.ITEM, itemKey, new CreativeOnlyBlockItem(BLOCK, new Item.Properties().setId(itemKey)
				.useBlockDescriptionPrefix().rarity(net.minecraft.world.item.Rarity.EPIC)));
		ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("creative_energy_cell"),
				FabricBlockEntityTypeBuilder.create(Entity::new, BLOCK).build());
		EnergyStorage.SIDED.registerForBlockEntity((cell, side) -> Infinite.INSTANCE, ENTITY);
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(output -> output.accept(BLOCK));
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new Entity(pos, state);
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level instanceof ServerLevel ? createTickerHelper(type, ENTITY,
				(tickLevel, pos, tickState, cell) -> EnergyNetworks.pushToNeighbors(tickLevel, pos, Infinite.INSTANCE, OUTPUT_PER_TICK,
						java.util.List.of(Direction.values()))) : null;
	}

	/** The block entity: it only exists so cables and machines find the endless storage here. */
	public static class Entity extends BlockEntity {
		public Entity(BlockPos pos, BlockState state) {
			super(ENTITY, pos, state);
		}
	}

	/** An endless storage: always full, gives whatever is asked, takes whatever is offered. */
	static final class Infinite implements EnergyStorage {
		static final Infinite INSTANCE = new Infinite();

		@Override
		public boolean supportsInsertion() {
			return true;
		}

		@Override
		public long insert(long maxAmount, TransactionContext transaction) {
			return Math.max(0, maxAmount);
		}

		@Override
		public boolean supportsExtraction() {
			return true;
		}

		@Override
		public long extract(long maxAmount, TransactionContext transaction) {
			return Math.max(0, maxAmount);
		}

		@Override
		public long getAmount() {
			return Long.MAX_VALUE / 4;
		}

		@Override
		public long getCapacity() {
			return Long.MAX_VALUE / 4;
		}
	}

	/** Places only for creative players; anyone else is told it is creative only. */
	public static final class CreativeOnlyBlockItem extends BlockItem {
		public CreativeOnlyBlockItem(Block block, Properties properties) {
			super(block, properties);
		}

		@Override
		public InteractionResult place(BlockPlaceContext context) {
			if (context.getPlayer() == null || !context.getPlayer().isCreative()) {
				if (context.getPlayer() != null && !context.getLevel().isClientSide()) {
					context.getPlayer().sendOverlayMessage(Component.translatable("message.jugcraft.creative_only"));
				}
				return InteractionResult.FAIL;
			}
			return super.place(context);
		}
	}
}
