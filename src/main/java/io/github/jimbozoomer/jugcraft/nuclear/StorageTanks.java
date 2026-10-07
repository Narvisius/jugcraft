package io.github.jimbozoomer.jugcraft.nuclear;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.tower.FurnitureBlock;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

/**
 * The storage tank MBS and the safety kit round it (docs/features/nuclear.md, "Storage tanks"; tools/storage_tank.py):
 * tank plates, the invisible interior, the inlet and outlet nozzles (the outlet is the tank's controller), the fill
 * line, manway, roof vent and roof hatch, the railing that joins like a fence, the self-closing railing gate and the
 * steel ladder.
 */
public final class StorageTanks {
	public static final Map<String, Block> BLOCKS = new LinkedHashMap<>();
	public static Block PLATE, INTERIOR, OUTLET, INLET, HATCH, RAILING, GATE, LADDER;
	public static BlockEntityType<StorageTankPortBlockEntity> PORT_ENTITY;
	public static BlockEntityType<StorageTankHatchBlockEntity> HATCH_ENTITY;
	public static BlockEntityType<RailingGateBlockEntity> GATE_ENTITY;

	/** Opens a tank's screen on the client (set by the client; the outlet's position). */
	public static java.util.function.Consumer<BlockPos> openScreen = pos -> {
	};

	private StorageTanks() {
	}

	/**
	 * The outlet of the formed tank whose shell (or roof) {@code plate} is part of: a walk over the joined plates to
	 * the one an outlet stands against, facing out from it. Null if there is none near enough.
	 */
	public static @org.jspecify.annotations.Nullable BlockPos outletOf(net.minecraft.world.level.Level level, BlockPos plate) {
		java.util.Set<BlockPos> seen = new java.util.HashSet<>();
		java.util.ArrayDeque<BlockPos> queue = new java.util.ArrayDeque<>();
		queue.add(plate.immutable());
		seen.add(plate.immutable());
		while (!queue.isEmpty() && seen.size() < 8000) {
			BlockPos p = queue.poll();
			for (Direction d : Direction.values()) {
				BlockPos q = p.relative(d);
				if (!level.isLoaded(q) || !seen.add(q)) {
					continue;
				}
				BlockState state = level.getBlockState(q);
				if (state.getBlock() instanceof StorageTankPlateBlock) {
					queue.add(q);
				} else if (d.getAxis().isHorizontal() && state.getBlock() instanceof StorageTankOutletBlock
						&& state.getValue(StorageTankPortBlock.FACING) == d && state.getValue(StorageTankPortBlock.FORMED)) {
					return q;
				}
			}
		}
		return null;
	}

	/** A piece set against a wall or a roof: placed facing out from the face clicked. */
	static final class TankDecorBlock extends FurnitureBlock {
		TankDecorBlock(Properties properties, double[] shape) {
			super(properties, shape);
		}

		@Override
		public BlockState getStateForPlacement(BlockPlaceContext context) {
			Direction face = context.getClickedFace();
			return defaultBlockState().setValue(FACING, face.getAxis().isHorizontal() ? face : context.getHorizontalDirection().getOpposite());
		}
	}

	public static void register() {
		BlockBehaviour.Properties steel = BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(2.0F, 6.0F);
		PLATE = block("storage_tank_plate", StorageTankPlateBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(2.5F, 8.0F)
				.mapColor(MapColor.SNOW));
		INTERIOR = block("storage_tank_interior", StorageTankInteriorBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.SNOW)
				.strength(-1.0F, 3_600_000.0F).noLootTable().pushReaction(PushReaction.IMMOVEABLE).isValidSpawn((s, l, p, e) -> false)
				.isSuffocating((s, l, p) -> false).isViewBlocking((s, l, p, box) -> false), false);
		OUTLET = block("storage_tank_outlet", StorageTankOutletBlock::new, steel.noOcclusion());
		INLET = block("storage_tank_inlet", p -> new StorageTankPortBlock(p, false), BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
				.strength(2.0F, 6.0F).noOcclusion());
		block("storage_tank_fill_pipe", TankFillPipeBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(1.5F, 6.0F).noOcclusion());
		block("storage_tank_manway", p -> new TankDecorBlock(p, new double[] {1.8, 1.8, 12.0, 14.2, 14.2, 16}),
				BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(2.0F, 6.0F).noOcclusion());
		block("storage_tank_vent", TankVentBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(1.5F, 6.0F).noOcclusion());
		HATCH = block("storage_tank_hatch", StorageTankHatchBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(2.5F, 8.0F)
				.mapColor(MapColor.SNOW).noOcclusion());
		RAILING = block("railing", RailingBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(1.5F, 6.0F)
				.sound(SoundType.METAL).noOcclusion());
		GATE = block("railing_gate", RailingGateBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(1.5F, 6.0F)
				.sound(SoundType.METAL).noOcclusion());
		LADDER = block("steel_ladder", SteelLadderBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(1.0F, 6.0F)
				.sound(SoundType.METAL).noOcclusion().pushReaction(PushReaction.POPPED));

		PORT_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("storage_tank_port"),
				FabricBlockEntityTypeBuilder.create(StorageTankPortBlockEntity::new, OUTLET, INLET).build());
		HATCH_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("storage_tank_hatch"),
				FabricBlockEntityTypeBuilder.create(StorageTankHatchBlockEntity::new, HATCH).build());
		GATE_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("railing_gate"),
				FabricBlockEntityTypeBuilder.create(RailingGateBlockEntity::new, GATE).build());
		// Pipes join a nozzle at its outer flange.
		FluidStorage.SIDED.registerForBlockEntity((port, side) -> side == port.getBlockState().getValue(StorageTankPortBlock.FACING)
				? port.portStorage() : null, PORT_ENTITY);

		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(output -> BLOCKS.forEach((id, block) -> {
			if (block != INTERIOR) {
				output.accept(block);
			}
		}));
	}

	private static Block block(String name, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties properties) {
		return block(name, factory, properties, true);
	}

	private static Block block(String name, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties properties, boolean item) {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Jugcraft.id(name));
		Block block = Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(properties.setId(key)));
		if (item) {
			ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Jugcraft.id(name));
			Registry.register(BuiltInRegistries.ITEM, itemKey, new BlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));
		}
		BLOCKS.put(name, block);
		return block;
	}
}
