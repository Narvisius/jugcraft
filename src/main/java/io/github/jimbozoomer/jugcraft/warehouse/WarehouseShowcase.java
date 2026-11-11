package io.github.jimbozoomer.jugcraft.warehouse;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * {@code /warehouse demo} (operators only): builds a small working warehouse just north of you (18 blocks deep, 17 wide)
 * and sets it going: lanes, the Intake Case Packer against the north wall with the Goods In panel set in the wall behind
 * it, a Palletizing Station loaded with pallets, two rows of pallet racks four high, the terminal, three charging docks
 * on the west wall with a stacker drone and two pallet drones, and the intake's bay filled with a mix of items to store;
 * part W2: Ready Storage against the north wall with the Goods Out panel in the wall behind it, and three pallets
 * already in the racks (iron, oak logs, coal) to order from; part W4: the liquid line east of it (the Drum Emptying
 * Station behind the Liquids Out panel, the Drum Filling Station behind the Liquids In panel, loaded with empty drums
 * and pallets, 60 buckets of water in its tank to drum), a pallet of water drums stored; part W5: unlimited power (no
 * switchboard), a status board and a signal box (FLEET BUSY, a lamp over it) on the west wall.
 */
public final class WarehouseShowcase {
	private WarehouseShowcase() {
	}

	static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
				Commands.literal("warehouse").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
						.then(Commands.literal("demo").executes(context -> {
							var player = context.getSource().getPlayerOrException();
							build(player.level(), player.blockPosition());
							context.getSource().sendSuccess(() -> Component.literal("Built a demo warehouse just north of you: watch the drones store"
										+ " what is in the intake. On the outside of its north wall: the Goods In panel, and the Goods Out panel (order"
										+ " at its kiosk, collect at its window), the Liquids Out and Liquids In panels (its filling station drums the"
										+ " water in its tank). Four pallets are already stored, one of them water drums."), false);
							return 1;
						}))));
	}

	/** Builds the demo with its south edge just north of {@code at}, its floor at at's level. */
	public static void build(ServerLevel level, BlockPos at) {
		BlockPos o = at.offset(-8, 0, -20);
		// Clear it, floor it, the north and west walls.
		for (int u = -1; u <= 23; u++) {
			for (int v = -1; v <= 18; v++) {
				for (int y = 0; y <= 7; y++) {
					level.setBlockAndUpdate(o.offset(u, y, v), Blocks.AIR.defaultBlockState());
				}
				level.setBlockAndUpdate(o.offset(u, -1, v), Blocks.SMOOTH_STONE.defaultBlockState());
			}
		}
		for (int u = 0; u <= 22; u++) {
			for (int y = 0; y <= 4; y++) {
				level.setBlockAndUpdate(o.offset(u, y, 0), Blocks.STONE_BRICKS.defaultBlockState());
			}
		}
		for (int v = 0; v <= 16; v++) {
			for (int y = 0; y <= 4; y++) {
				level.setBlockAndUpdate(o.offset(0, y, v), Blocks.STONE_BRICKS.defaultBlockState());
			}
		}
		// Lanes: the whole floor from the machines' fronts south.
		for (int u = 1; u <= 21; u++) {
			for (int v = 3; v <= 15; v++) {
				level.setBlockAndUpdate(o.offset(u, -1, v), JugcraftWarehouse.LANE.defaultBlockState());
			}
		}
		// The Goods In panel in the north wall (its face outside, north), the intake against the wall inside; the same for
		// Goods Out and Ready Storage.
		for (int u : new int[] {2, 3, 4, 7, 8, 9, 11, 12, 13, 15, 16, 17}) {
			for (int y = 0; y <= 1; y++) {
				level.setBlockAndUpdate(o.offset(u, y, 0), Blocks.AIR.defaultBlockState());
			}
		}
		piece(level, JugcraftWarehouse.GOODS_OUT, WarehousePiece.GOODS_OUT, o.offset(8, 0, 0), Direction.SOUTH);
		piece(level, JugcraftWarehouse.READY_STORAGE, WarehousePiece.READY_STORAGE, o.offset(8, 0, 2), Direction.NORTH);
		piece(level, JugcraftWarehouse.GOODS_IN, WarehousePiece.GOODS_IN, o.offset(3, 0, 0), Direction.SOUTH);
		BlockPos intake = piece(level, JugcraftWarehouse.INTAKE, WarehousePiece.INTAKE, o.offset(3, 0, 2), Direction.NORTH);
		BlockPos station = piece(level, JugcraftWarehouse.STATION, WarehousePiece.STATION, o.offset(6, 0, 2), Direction.NORTH);
		level.setBlockAndUpdate(o.offset(6, -1, 2), JugcraftWarehouse.PACK_SPOT.defaultBlockState());
		BlockPos terminal = piece(level, JugcraftWarehouse.TERMINAL, WarehousePiece.TERMINAL, o.offset(20, 0, 2), Direction.NORTH);
		// Part W5: the demo runs without a switchboard (unlimited power); a status board on the west wall, and a signal box
		// by it lighting a lamp while the fleet is busy.
		if (level.getBlockEntity(terminal) instanceof TerminalBlockEntity t) {
			t.setUnlimitedPower(true);
		}
		piece(level, JugcraftWarehouse.STATUS_BOARD, WarehousePiece.STATUS_BOARD, o.offset(1, 2, 12), Direction.WEST);
		BlockPos box = piece(level, JugcraftWarehouse.SIGNAL_BOX, WarehousePiece.SIGNAL_BOX, o.offset(1, 2, 15), Direction.WEST);
		if (level.getBlockEntity(box) instanceof SignalBoxBlockEntity sb) {
			sb.setMode(SignalBoxBlockEntity.OUTPUT);
			sb.setRule(SignalBoxBlockEntity.FLEET_BUSY);
		}
		level.setBlockAndUpdate(o.offset(1, 3, 15), Blocks.REDSTONE_LAMP.defaultBlockState());
		// Part W4: the liquid line.
		piece(level, JugcraftWarehouse.LIQUIDS_OUT, WarehousePiece.LIQUIDS_OUT, o.offset(12, 0, 0), Direction.SOUTH);
		piece(level, JugcraftWarehouse.DRUM_EMPTIER, WarehousePiece.DRUM_EMPTIER, o.offset(12, 0, 2), Direction.NORTH);
		piece(level, JugcraftWarehouse.LIQUIDS_IN, WarehousePiece.LIQUIDS_IN, o.offset(16, 0, 0), Direction.SOUTH);
		BlockPos filler = piece(level, JugcraftWarehouse.DRUM_FILLER, WarehousePiece.DRUM_FILLER, o.offset(16, 0, 2), Direction.NORTH);
		if (level.getBlockEntity(filler) instanceof FillerBlockEntity f) {
			LiquidLine.load(f);
		}
		// Docks on the west wall: a stacker drone and two pallet drones.
		DroneKind[] fleet = {DroneKind.STACKER, DroneKind.PALLET, DroneKind.PALLET};
		for (int k = 0; k < fleet.length; k++) {
			BlockPos dock = o.offset(1, 0, 5 + k);
			level.setBlockAndUpdate(dock, JugcraftWarehouse.DOCK.defaultBlockState().setValue(DockBlock.FACING, Direction.WEST));
			level.setBlockAndUpdate(dock.above(), JugcraftWarehouse.CONDUIT.defaultBlockState().setValue(DecorBlock.FACING, Direction.WEST));
			if (level.getBlockEntity(dock) instanceof DockBlockEntity d) {
				d.setDrone(fleet[k], 0);
			}
		}
		// Two rows of racks back to back, four high, their aisles each side.
		for (int u = 4; u <= 13; u++) {
			for (int y = 0; y <= 3; y++) {
				level.setBlockAndUpdate(o.offset(u, y, 9), JugcraftWarehouse.RACK.defaultBlockState().setValue(RackBlock.FACING, Direction.SOUTH));
				level.setBlockAndUpdate(o.offset(u, y, 10), JugcraftWarehouse.RACK.defaultBlockState().setValue(RackBlock.FACING, Direction.NORTH));
			}
		}
		if (level.getBlockEntity(station) instanceof StationBlockEntity s) {
			s.addPallets(StationBlockEntity.MAGAZINE);
		}
		// Three pallets already stored (ground places of the north row), for orders straight away.
		Object[][] stored = {{Items.IRON_INGOT, 8}, {Items.OAK_LOG, 6}, {Items.COAL, 8}};
		for (int k = 0; k < stored.length; k++) {
			if (level.getBlockEntity(o.offset(4 + k, 0, 9)) instanceof RackBlockEntity rack) {
				java.util.List<CaseLoad> cases = new java.util.ArrayList<>();
				ItemStack item = new ItemStack((Item) stored[k][0]);
				for (int c = 0; c < (Integer) stored[k][1]; c++) {
					cases.add(new CaseLoad(item, CaseLoad.capacity(item)));
				}
				rack.setLoad(new PalletLoad(cases));
			}
		}
		// A pallet of water drums (ground place of the north row, next to the item pallets).
		if (level.getBlockEntity(o.offset(7, 0, 9)) instanceof RackBlockEntity rack) {
			java.util.List<CaseLoad> drums = new java.util.ArrayList<>();
			for (int k = 0; k < Drums.PER_PALLET; k++) {
				drums.add(new CaseLoad(Drums.book(net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant.of(net.minecraft.world.level.material.Fluids.WATER)),
						(int) Drums.CAPACITY));
			}
			rack.setLoad(new PalletLoad(drums));
		}
		// What there is to store.
		if (level.getBlockEntity(intake) instanceof IntakeBlockEntity in) {
			Object[][] goods = {{Items.COBBLESTONE, 1024}, {Items.IRON_INGOT, 512}, {Items.OAK_LOG, 256}, {Items.WHEAT_SEEDS, 256},
					{Items.BREAD, 64}, {Items.IRON_PICKAXE, 2}, {Items.WATER_BUCKET, 2}};
			try (Transaction t = Transaction.openOuter()) {
				for (Object[] g : goods) {
					in.input().insert(ItemVariant.of((Item) g[0]), (Integer) g[1], t);
				}
				t.commit();
			}
		}
		Warehouses.changed(level, o);
	}

	/** The demo's filling station loaded: empty drums of every kind, pallets, 60 buckets of water to drum. */
	private static final class LiquidLine {
		static void load(FillerBlockEntity f) {
			int[] kinds = {0, 0, 0, 0, 1, 1, 2, 3};
			for (int i = 0; i < kinds.length; i++) {
				f.magazine()[i] = kinds[i];
			}
			f.setPallets(4);
			try (Transaction t = Transaction.openOuter()) {
				f.buffer.insert(net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant.of(net.minecraft.world.level.material.Fluids.WATER),
						Drums.droplets(60_000), t);
				t.commit();
			}
		}
	}

	private static BlockPos piece(ServerLevel level, Block block, WarehousePiece piece, BlockPos origin, Direction facing) {
		BlockState state = block.defaultBlockState().setValue(PieceBlock.FACING, facing);
		PieceBlock pb = (PieceBlock) block;
		for (int p = 0; p < piece.parts(); p++) {
			level.setBlockAndUpdate(origin.offset(piece.offset(facing, p)), state.setValue(pb.partProperty(), p));
		}
		return origin;
	}
}
