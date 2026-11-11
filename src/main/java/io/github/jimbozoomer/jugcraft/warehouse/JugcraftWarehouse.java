package io.github.jimbozoomer.jugcraft.warehouse;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;

/**
 * The automated warehouse (claude/jugcraft-warehouse-plan.md). Part W1 (inbound): lane tiles, pallet racks, the Intake
 * Case Packer, the Palletizing Station, the Goods In panel, charging docks, the Control Terminal, trunking; the pallet,
 * the stacker drone and the pallet drone. Part W2 (outbound): Ready Storage and its extension module, the Goods Out
 * panel (pickup window, kiosk, Output Port); orders. Part W3: lifts and floors; W4: liquids in drums; W5: fleet and
 * control (the switchboard, status board, signal box). IDs match tools/warehouse_data.py.
 */
public final class JugcraftWarehouse {
	public static final List<Item> ITEMS = new ArrayList<>();
	public static Block LANE, PACK_SPOT, RACK, DOCK, CONDUIT, TRUNKING, PART, LOAD;
	public static Block INTAKE, STATION, TERMINAL, GOODS_IN, READY_STORAGE, READY_STORAGE_EXT, GOODS_OUT;
	public static Block LIFT_LANDING, LIFT_SHAFT, LIFT_DRIVE, WALKWAY, CROSSING, CROSSING_BEACON, AISLE_SIGN, END_BARRIER, BOLLARD, SLAB;
	public static Block DRUM_FILLER, DRUM_EMPTIER, LIQUIDS_IN, LIQUIDS_OUT;
	public static Block SWITCHBOARD, STATUS_BOARD, SIGNAL_BOX;
	public static Item RACK_SPRINKLER_KIT;
	public static Item PALLET, STACKER_DRONE, PALLET_DRONE;
	public static BlockEntityType<RackBlockEntity> RACK_ENTITY;
	public static BlockEntityType<DockBlockEntity> DOCK_ENTITY;
	public static BlockEntityType<IntakeBlockEntity> INTAKE_ENTITY;
	public static BlockEntityType<StationBlockEntity> STATION_ENTITY;
	public static BlockEntityType<TerminalBlockEntity> TERMINAL_ENTITY;
	public static BlockEntityType<GoodsInBlockEntity> GOODS_IN_ENTITY;
	public static BlockEntityType<ReadyStorageBlockEntity> READY_STORAGE_ENTITY;
	public static BlockEntityType<GoodsOutBlockEntity> GOODS_OUT_ENTITY;
	public static BlockEntityType<LiftBlockEntity> LIFT_ENTITY;
	public static BlockEntityType<AisleSignBlockEntity> AISLE_SIGN_ENTITY;
	public static BlockEntityType<FillerBlockEntity> FILLER_ENTITY;
	public static BlockEntityType<EmptierBlockEntity> EMPTIER_ENTITY;
	public static BlockEntityType<LiquidsInBlockEntity> LIQUIDS_IN_ENTITY;
	public static BlockEntityType<LiquidsOutBlockEntity> LIQUIDS_OUT_ENTITY;
	public static BlockEntityType<SwitchboardBlockEntity> SWITCHBOARD_ENTITY;
	public static BlockEntityType<StatusBoardBlockEntity> STATUS_BOARD_ENTITY;
	public static BlockEntityType<SignalBoxBlockEntity> SIGNAL_BOX_ENTITY;
	public static MenuType<GoodsInMenu> GOODS_IN_MENU;
	public static MenuType<TerminalMenu> TERMINAL_MENU;
	public static MenuType<GoodsOutMenu> GOODS_OUT_MENU;
	public static MenuType<LiquidsInMenu> LIQUIDS_IN_MENU;
	public static MenuType<LiquidsOutMenu> LIQUIDS_OUT_MENU;
	public static MenuType<SwitchboardMenu> SWITCHBOARD_MENU;
	public static MenuType<SignalBoxMenu> SIGNAL_BOX_MENU;

	private JugcraftWarehouse() {
	}

	public static void register() {
		registerContent();
		registerHooks();
	}

	/** The blocks, items and block entities (all the startup check needs). */
	public static void registerContent() {
		BlockBehaviour.Properties steel = BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(2.0F, 6.0F).noOcclusion();
		BlockBehaviour.Properties floor = BlockBehaviour.Properties.ofFullCopy(Blocks.SMOOTH_STONE).strength(1.8F, 6.0F);
		LANE = block("warehouse_lane", LaneBlock::new, floor, "lane");
		PACK_SPOT = block("warehouse_pack_spot", PackSpotBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.SMOOTH_STONE).strength(1.8F, 6.0F), "lane");
		RACK = block("warehouse_rack", RackBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(2.0F, 6.0F).noOcclusion()
				.pushReaction(PushReaction.IMMOVEABLE), "rack");
		DOCK = block("warehouse_dock", DockBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(2.0F, 6.0F).noOcclusion()
				.pushReaction(PushReaction.IMMOVEABLE), "dock");
		CONDUIT = block("warehouse_conduit", p -> new DecorBlock("conduit", p), steel.pushReaction(PushReaction.IMMOVEABLE), null);
		TRUNKING = block("warehouse_trunking", p -> new DecorBlock("trunking", p),
				BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(2.0F, 6.0F).noOcclusion(), "trunking");
		BlockBehaviour.Properties machine = BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(3.0F, 8.0F).noOcclusion()
				.pushReaction(PushReaction.IMMOVEABLE);
		INTAKE = block("warehouse_intake", p -> new PieceBlock(WarehousePiece.INTAKE, p), machine, "intake");
		STATION = block("warehouse_station", p -> new PieceBlock(WarehousePiece.STATION, p),
				BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(3.0F, 8.0F).noOcclusion().pushReaction(PushReaction.IMMOVEABLE), "station");
		TERMINAL = block("warehouse_terminal", p -> new PieceBlock(WarehousePiece.TERMINAL, p),
				BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(2.0F, 6.0F).noOcclusion().pushReaction(PushReaction.IMMOVEABLE), "terminal");
		GOODS_IN = block("warehouse_goods_in", p -> new PieceBlock(WarehousePiece.GOODS_IN, p),
				BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(3.0F, 8.0F).noOcclusion().pushReaction(PushReaction.IMMOVEABLE), "goods_in");
		// Part W2: Ready Storage (and its extension module), the Goods Out panel.
		READY_STORAGE = block("warehouse_ready_storage", p -> new PieceBlock(WarehousePiece.READY_STORAGE, p), machine, "ready_storage");
		READY_STORAGE_EXT = block("warehouse_ready_storage_ext", p -> new PieceBlock(WarehousePiece.READY_STORAGE_EXT, p), machine, "ready_storage_ext");
		GOODS_OUT = block("warehouse_goods_out", p -> new PieceBlock(WarehousePiece.GOODS_OUT, p),
				BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(3.0F, 8.0F).noOcclusion().pushReaction(PushReaction.IMMOVEABLE), "goods_out");
		// Part W3: the Pallet Lift, the floors' fittings.
		LIFT_LANDING = block("warehouse_lift_landing", p -> new PieceBlock(WarehousePiece.LIFT_LANDING, p), machine, "lift_landing");
		LIFT_SHAFT = block("warehouse_lift_shaft", p -> new PieceBlock(WarehousePiece.LIFT_SHAFT, p),
				BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(3.0F, 8.0F).noOcclusion().pushReaction(PushReaction.IMMOVEABLE), "lift_shaft");
		LIFT_DRIVE = block("warehouse_lift_drive", p -> new PieceBlock(WarehousePiece.LIFT_DRIVE, p),
				BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(3.0F, 8.0F).noOcclusion().pushReaction(PushReaction.IMMOVEABLE), "lift_drive");
		WALKWAY = block("warehouse_walkway", WalkwayBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.SMOOTH_STONE).strength(1.8F, 6.0F), "walkway");
		CROSSING = block("warehouse_crossing", CrossingBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.SMOOTH_STONE).strength(1.8F, 6.0F), "crossing");
		CROSSING_BEACON = block("warehouse_crossing_beacon", BeaconBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(1.5F, 6.0F)
				.noOcclusion().lightLevel(s -> s.getValue(BeaconBlock.LIT) ? 8 : 0), "crossing_beacon");
		AISLE_SIGN = block("warehouse_aisle_sign", AisleSignBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(1.0F, 3.0F)
				.noOcclusion().noCollision(), "aisle_sign");
		END_BARRIER = block("warehouse_end_barrier", p -> new DecorBlock("end_barrier", p), BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
				.strength(2.0F, 6.0F).noOcclusion(), "end_barrier");
		BOLLARD = block("warehouse_bollard", p -> new DecorBlock("bollard", p), BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
				.strength(2.0F, 6.0F).noOcclusion(), null);
		SLAB = block("warehouse_slab", SlabBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.SMOOTH_STONE).strength(1.8F, 6.0F), "slab");
		RACK_SPRINKLER_KIT = item("rack_sprinkler_kit", RackSprinklerKit::new, new Item.Properties().stacksTo(64));
		// Part W4: liquids in drums.
		DRUM_FILLER = block("warehouse_drum_filler", p -> new PieceBlock(WarehousePiece.DRUM_FILLER, p), machine, "drum_filler");
		DRUM_EMPTIER = block("warehouse_drum_emptier", p -> new PieceBlock(WarehousePiece.DRUM_EMPTIER, p), machine, "drum_emptier");
		LIQUIDS_IN = block("warehouse_liquids_in", p -> new PieceBlock(WarehousePiece.LIQUIDS_IN, p),
				BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(3.0F, 8.0F).noOcclusion().pushReaction(PushReaction.IMMOVEABLE), "liquids_in");
		LIQUIDS_OUT = block("warehouse_liquids_out", p -> new PieceBlock(WarehousePiece.LIQUIDS_OUT, p),
				BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(3.0F, 8.0F).noOcclusion().pushReaction(PushReaction.IMMOVEABLE), "liquids_out");
		// Part W5: fleet and control: the switchboard (power in by cable), the status board, the signal box.
		SWITCHBOARD = block("warehouse_switchboard", p -> new PieceBlock(WarehousePiece.SWITCHBOARD, p), machine, "switchboard");
		STATUS_BOARD = block("warehouse_status_board", p -> new PieceBlock(WarehousePiece.STATUS_BOARD, p),
				BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(2.0F, 6.0F).noOcclusion().pushReaction(PushReaction.IMMOVEABLE)
						.lightLevel(s -> 6), "status_board");
		SIGNAL_BOX = block("warehouse_signal_box", p -> new PieceBlock(WarehousePiece.SIGNAL_BOX, p),
				BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(2.0F, 6.0F).noOcclusion().pushReaction(PushReaction.IMMOVEABLE), "signal_box");
		// The hidden blocks the client draws moving things with: never placed, no item, nothing to break.
		BlockBehaviour.Properties hidden = BlockBehaviour.Properties.of().noCollision().noOcclusion().strength(-1.0F, 3600000.0F).noLootTable();
		PART = hiddenBlock("warehouse_part", p -> new HiddenBlock(HiddenBlock.PART, p), hidden);
		LOAD = hiddenBlock("warehouse_load", p -> new HiddenBlock(HiddenBlock.LOAD, p),
				BlockBehaviour.Properties.of().noCollision().noOcclusion().strength(-1.0F, 3600000.0F).noLootTable());

		PALLET = item("warehouse_pallet", p -> new TooltipItem(p, "pallet"), new Item.Properties().stacksTo(64));
		STACKER_DRONE = item("stacker_drone", p -> new DroneItem(DroneKind.STACKER, p), new Item.Properties().stacksTo(1));
		PALLET_DRONE = item("pallet_drone", p -> new DroneItem(DroneKind.PALLET, p), new Item.Properties().stacksTo(1));

		RACK_ENTITY = entity("warehouse_rack", FabricBlockEntityTypeBuilder.create(RackBlockEntity::new, RACK));
		DOCK_ENTITY = entity("warehouse_dock", FabricBlockEntityTypeBuilder.create(DockBlockEntity::new, DOCK));
		INTAKE_ENTITY = entity("warehouse_intake", FabricBlockEntityTypeBuilder.create(IntakeBlockEntity::new, INTAKE));
		STATION_ENTITY = entity("warehouse_station", FabricBlockEntityTypeBuilder.create(StationBlockEntity::new, STATION));
		TERMINAL_ENTITY = entity("warehouse_terminal", FabricBlockEntityTypeBuilder.create(TerminalBlockEntity::new, TERMINAL));
		GOODS_IN_ENTITY = entity("warehouse_goods_in", FabricBlockEntityTypeBuilder.create(GoodsInBlockEntity::new, GOODS_IN));
		READY_STORAGE_ENTITY = entity("warehouse_ready_storage", FabricBlockEntityTypeBuilder.create(ReadyStorageBlockEntity::new, READY_STORAGE));
		GOODS_OUT_ENTITY = entity("warehouse_goods_out", FabricBlockEntityTypeBuilder.create(GoodsOutBlockEntity::new, GOODS_OUT));
		LIFT_ENTITY = entity("warehouse_lift_landing", FabricBlockEntityTypeBuilder.create(LiftBlockEntity::new, LIFT_LANDING));
		AISLE_SIGN_ENTITY = entity("warehouse_aisle_sign", FabricBlockEntityTypeBuilder.create(AisleSignBlockEntity::new, AISLE_SIGN));
		FILLER_ENTITY = entity("warehouse_drum_filler", FabricBlockEntityTypeBuilder.create(FillerBlockEntity::new, DRUM_FILLER));
		EMPTIER_ENTITY = entity("warehouse_drum_emptier", FabricBlockEntityTypeBuilder.create(EmptierBlockEntity::new, DRUM_EMPTIER));
		LIQUIDS_IN_ENTITY = entity("warehouse_liquids_in", FabricBlockEntityTypeBuilder.create(LiquidsInBlockEntity::new, LIQUIDS_IN));
		LIQUIDS_OUT_ENTITY = entity("warehouse_liquids_out", FabricBlockEntityTypeBuilder.create(LiquidsOutBlockEntity::new, LIQUIDS_OUT));
		SWITCHBOARD_ENTITY = entity("warehouse_switchboard", FabricBlockEntityTypeBuilder.create(SwitchboardBlockEntity::new, SWITCHBOARD));
		STATUS_BOARD_ENTITY = entity("warehouse_status_board", FabricBlockEntityTypeBuilder.create(StatusBoardBlockEntity::new, STATUS_BOARD));
		SIGNAL_BOX_ENTITY = entity("warehouse_signal_box", FabricBlockEntityTypeBuilder.create(SignalBoxBlockEntity::new, SIGNAL_BOX));
		Drums.registerContent();
	}

	/** The screens, pipes, packets and the creative tab. */
	private static void registerHooks() {
		GOODS_IN_MENU = Registry.register(BuiltInRegistries.MENU, Jugcraft.id("warehouse_goods_in"),
				new ExtendedMenuType<>(GoodsInMenu::new, GoodsInMenu.Opening.STREAM_CODEC));
		TERMINAL_MENU = Registry.register(BuiltInRegistries.MENU, Jugcraft.id("warehouse_terminal"),
				new ExtendedMenuType<>(TerminalMenu::new, BlockPos.STREAM_CODEC.cast()));
		GOODS_OUT_MENU = Registry.register(BuiltInRegistries.MENU, Jugcraft.id("warehouse_goods_out"),
				new ExtendedMenuType<>(GoodsOutMenu::new, GoodsOutMenu.Opening.STREAM_CODEC));
		LIQUIDS_IN_MENU = Registry.register(BuiltInRegistries.MENU, Jugcraft.id("warehouse_liquids_in"),
				new ExtendedMenuType<>(LiquidsInMenu::new, LiquidsInMenu.Opening.STREAM_CODEC));
		LIQUIDS_OUT_MENU = Registry.register(BuiltInRegistries.MENU, Jugcraft.id("warehouse_liquids_out"),
				new ExtendedMenuType<>(LiquidsOutMenu::new, LiquidsOutMenu.Opening.STREAM_CODEC));
		SWITCHBOARD_MENU = Registry.register(BuiltInRegistries.MENU, Jugcraft.id("warehouse_switchboard"),
				new ExtendedMenuType<>(SwitchboardMenu::new, BlockPos.STREAM_CODEC.cast()));
		SIGNAL_BOX_MENU = Registry.register(BuiltInRegistries.MENU, Jugcraft.id("warehouse_signal_box"),
				new ExtendedMenuType<>(SignalBoxMenu::new, BlockPos.STREAM_CODEC.cast()));
		// Part W5: the incoming cable connects to any of the switchboard's faces.
		io.github.jimbozoomer.jugcraft.energy.EnergyStorage.SIDED.registerForBlocks((level, pos, state, entity, side) -> {
			if (!(state.getBlock() instanceof PieceBlock piece)) {
				return null;
			}
			BlockPos origin = piece.origin(pos, state);
			return level.isLoaded(origin) && level.getBlockEntity(origin) instanceof SwitchboardBlockEntity sb ? sb.energy : null;
		}, SWITCHBOARD);
		// Part W4: pipes into the Liquids In panel's inlets and tanker coupling, out of the Liquids Out panel's OUT flange
		// (both on their faces outside); the drums are liquid containers.
		net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage.SIDED.registerForBlocks((level, pos, state, entity, side) -> {
			if (!(state.getBlock() instanceof PieceBlock piece) || side == null) {
				return null;
			}
			Direction facing = state.getValue(PieceBlock.FACING);
			int part = piece.part(state);
			BlockPos origin = piece.origin(pos, state);
			if (side != facing.getOpposite() || !level.isLoaded(origin)) {
				return null;
			}
			if (piece.piece == WarehousePiece.LIQUIDS_IN && level.getBlockEntity(origin) instanceof LiquidsInBlockEntity in) {
				return in.inlet(LiquidsInBlockEntity.inletOf(piece.piece, part));
			}
			if (piece.piece == WarehousePiece.LIQUIDS_OUT && part == WarehousePiece.LIQUIDS_OUT.part(0, 0, 0)
					&& level.getBlockEntity(origin) instanceof LiquidsOutBlockEntity out) {
				return out.output();
			}
			return null;
		}, LIQUIDS_IN, LIQUIDS_OUT);
		Drums.registerHooks();

		// Pipes: into the intake's back ports, and into the Goods In panel's inlets (on their faces outside).
		ItemStorage.SIDED.registerForBlocks((level, pos, state, entity, side) -> {
			if (!(state.getBlock() instanceof PieceBlock piece) || side == null) {
				return null;
			}
			Direction facing = state.getValue(PieceBlock.FACING);
			int part = piece.part(state);
			BlockPos origin = piece.origin(pos, state);
			if (!level.isLoaded(origin)) {
				return null;
			}
			if (piece.piece == WarehousePiece.INTAKE) {
				return piece.piece.depthRow(part) == 0 && side == facing && level.getBlockEntity(origin) instanceof IntakeBlockEntity in ? in.input() : null;
			}
			if (piece.piece == WarehousePiece.GOODS_IN && side == facing.getOpposite() && level.getBlockEntity(origin) instanceof GoodsInBlockEntity goods) {
				return goods.inlet(GoodsInBlockEntity.inletOf(piece.piece, part));
			}
			// The Output Port's OUT flange (the Goods Out panel's right column, bottom, outside): extractors take from its buffer.
			if (piece.piece == WarehousePiece.GOODS_OUT && side == facing.getOpposite() && part == WarehousePiece.GOODS_OUT.part(2, 0, 0)
					&& level.getBlockEntity(origin) instanceof GoodsOutBlockEntity goods) {
				return goods.output();
			}
			return null;
		}, INTAKE, GOODS_IN, GOODS_OUT);

		PayloadTypeRegistry.clientboundPlay().register(DronePayload.TYPE, DronePayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(TerminalPayload.TYPE, TerminalPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(BuildingPayload.TYPE, BuildingPayload.CODEC);
		WarehouseBuilding.register();
		PayloadTypeRegistry.clientboundPlay().register(GoodsInPayload.TYPE, GoodsInPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(GoodsOutPayload.TYPE, GoodsOutPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(LiquidsPayload.TYPE, LiquidsPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(ControlPayload.TYPE, ControlPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(MapPayload.TYPE, MapPayload.CODEC);
		WarehouseActionPayload.register();
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(output -> ITEMS.forEach(output::accept));
		WarehouseShowcase.register();
	}

	private static BlockBehaviour.Properties key(BlockBehaviour.Properties properties, String name) {
		return properties.setId(ResourceKey.create(Registries.BLOCK, Jugcraft.id(name)));
	}

	private static Block block(String name, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties properties, String tooltip) {
		Block block = Registry.register(BuiltInRegistries.BLOCK, Jugcraft.id(name), factory.apply(key(properties, name)));
		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Jugcraft.id(name));
		Item.Properties ip = new Item.Properties().setId(itemKey).useBlockDescriptionPrefix();
		Item item = Registry.register(BuiltInRegistries.ITEM, itemKey, tooltip == null ? new BlockItem(block, ip) : new TooltipBlockItem(block, ip, tooltip));
		ITEMS.add(item);
		return block;
	}

	private static Block hiddenBlock(String name, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties properties) {
		return Registry.register(BuiltInRegistries.BLOCK, Jugcraft.id(name), factory.apply(key(properties, name)));
	}

	private static Item item(String name, Function<Item.Properties, Item> factory, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Jugcraft.id(name));
		Item item = Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
		ITEMS.add(item);
		return item;
	}

	private static <T extends net.minecraft.world.level.block.entity.BlockEntity> BlockEntityType<T> entity(String name, FabricBlockEntityTypeBuilder<T> builder) {
		return Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id(name), builder.build());
	}

	/** A warehouse block's item with its line of help (tooltip.jugcraft.warehouse.*). */
	static final class TooltipBlockItem extends BlockItem {
		private final String tooltip;

		TooltipBlockItem(Block block, Properties properties, String tooltip) {
			super(block, properties);
			this.tooltip = tooltip;
		}

		@Override
		public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
			out.accept(Component.translatable("tooltip.jugcraft.warehouse." + tooltip).withStyle(ChatFormatting.GRAY));
		}
	}

	static final class TooltipItem extends Item {
		private final String tooltip;

		TooltipItem(Properties properties, String tooltip) {
			super(properties);
			this.tooltip = tooltip;
		}

		@Override
		public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
			out.accept(Component.translatable("tooltip.jugcraft.warehouse." + tooltip).withStyle(ChatFormatting.GRAY));
		}
	}
}
