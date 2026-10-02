package io.github.jimbozoomer.jugcraft.drone;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.energy.CreativeEnergyCellBlock;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
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
 * The Creative Supply Crate, for testing: while one is within {@link #RANGE} blocks of a drone depot's terminal,
 * that depot gets every building block its drones ask for without using items (the packager is not touched).
 * It has no recipe; only creative players can place it, and it cannot be broken in survival.
 */
public class CreativeSupplyCrateBlock extends BaseEntityBlock {
	public static final int RANGE = 48;
	public static Block BLOCK;
	public static BlockEntityType<Entity> ENTITY;
	private static final Map<Level, Set<BlockPos>> CRATES = new WeakHashMap<>();

	public CreativeSupplyCrateBlock(Properties properties) {
		super(properties);
	}

	public static void register() {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Jugcraft.id("creative_supply_crate"));
		BLOCK = Registry.register(BuiltInRegistries.BLOCK, key, new CreativeSupplyCrateBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BEDROCK)
				.setId(key).lightLevel(state -> 8)));
		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Jugcraft.id("creative_supply_crate"));
		Registry.register(BuiltInRegistries.ITEM, itemKey, new CreativeEnergyCellBlock.CreativeOnlyBlockItem(BLOCK, new Item.Properties()
				.setId(itemKey).useBlockDescriptionPrefix().rarity(net.minecraft.world.item.Rarity.EPIC)));
		ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("creative_supply_crate"),
				FabricBlockEntityTypeBuilder.create(Entity::new, BLOCK).build());
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(output -> output.accept(BLOCK));
	}

	/** Is a Creative Supply Crate within {@link #RANGE} blocks of {@code pos}? */
	static boolean near(Level level, BlockPos pos) {
		Set<BlockPos> crates = CRATES.get(level);
		if (crates == null || crates.isEmpty()) {
			return false;
		}
		for (BlockPos crate : Set.copyOf(crates)) {
			if (crate.distSqr(pos) <= (double) RANGE * RANGE) {
				if (level.isLoaded(crate) && level.getBlockState(crate).is(BLOCK)) {
					return true;
				}
				crates.remove(crate);
			}
		}
		return false;
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
				(tickLevel, pos, tickState, crate) -> CRATES.computeIfAbsent(tickLevel, k -> new HashSet<>()).add(pos.immutable())) : null;
	}

	/** Exists only so the crate announces itself to nearby depots while it is loaded. */
	public static class Entity extends BlockEntity {
		public Entity(BlockPos pos, BlockState state) {
			super(ENTITY, pos, state);
		}

		@Override
		public void setRemoved() {
			super.setRemoved();
			if (level != null && CRATES.get(level) != null) {
				CRATES.get(level).remove(getBlockPos());
			}
		}
	}
}
