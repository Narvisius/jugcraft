package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.drone.DepotDisplayBlockEntity;
import io.github.jimbozoomer.jugcraft.drone.HoloTableBlock;
import io.github.jimbozoomer.jugcraft.tower.TowerCoreBlockEntity;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.BlockPos;

/** Checks the actual client projection, including coarse cells at the enlarged apron edges. */
public final class TowerRenderChecks {
	private TowerRenderChecks() { }

	public static void independentProgressCaches(ClientGameTestContext context) {
		context.runOnClient(client -> {
			var state = io.github.jimbozoomer.jugcraft.tower.JugcraftTower.CORE.defaultBlockState();
			var first = new TowerCoreBlockEntity(BlockPos.ZERO, state);
			var second = new TowerCoreBlockEntity(BlockPos.ZERO, state);
			var tag = new net.minecraft.nbt.CompoundTag();
			tag.putInt("layout_version", io.github.jimbozoomer.jugcraft.tower.TowerData.INDUSTRIAL_LAYOUT);
			tag.putInt("tier", 1);
			first.loadWithComponents(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING, client.level.registryAccess(), tag));
			tag.putInt("tier", 2); tag.putInt("building", 3); tag.putIntArray("filled", new int[] {0, 2, 5});
			second.loadWithComponents(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING, client.level.registryAccess(), tag));
			var a = HoloMapRenderer.cachedTowerCells(first);
			var b = HoloMapRenderer.cachedTowerCells(second);
			if (a == b || b.size() <= a.size() || a != HoloMapRenderer.cachedTowerCells(first)
					|| b != HoloMapRenderer.cachedTowerCells(second)) throw new AssertionError("Tower projection caches interfere at identical coordinates");
			if (b.stream().noneMatch(cell -> cell[3] == 1)) throw new AssertionError("Delivered construction tiles are not highlighted");
			var data = first.layout(); var tier = data.tier(1);
			float clearance = (float) tier.air.length / (tier.place.length + tier.air.length);
			if (HoloMapRenderer.firstTierPlaced(data, clearance) != 0
					|| !HoloMapRenderer.towerCells(data, 0, 1, clearance, new java.util.BitSet()).isEmpty()) {
				throw new AssertionError("The hologram projects blocks before site clearance has placed them");
			}
		});
	}

	public static void hologramFitsTable(ClientGameTestContext context, BlockPos origin) {
		context.runOnClient(client -> {
			var core = (TowerCoreBlockEntity) client.level.getBlockEntity(origin);
			int desks = 0;
			for (int[] block : core.layout().tier(1).place) {
				var candidate = client.level.getBlockEntity(origin.offset(block[0], block[1], block[2]));
				if (!(candidate instanceof DepotDisplayBlockEntity desk)
						|| !(desk.getBlockState().getBlock() instanceof io.github.jimbozoomer.jugcraft.tower.ConsoleDeskBlock)) continue;
				desks++;
				if (!core.terminalPos().equals(desk.terminal())) throw new AssertionError("Desk is linked to another tower");
				var renderer = new ConsoleDeskRenderer(client.font); var readout = renderer.createRenderState();
				renderer.extractRenderState(desk, readout, 0, client.player.position(), null);
				var terminal = (io.github.jimbozoomer.jugcraft.drone.DroneTerminalBlockEntity) client.level.getBlockEntity(desk.terminal());
				var view = terminal.view();
				if (view == null || readout.lines.size() != 7 || !readout.lines.get(1).equals("FLEET " + (view.docks.size() + view.flights.size()) + "/" + view.maxDrones)) {
					throw new AssertionError("Desk readout is not the linked terminal's real live fleet");
				}
			}
			if (desks != 2) throw new AssertionError("Expected two wall computers; found " + desks);
			int masters = 0;
			for (int[] block : core.layout().tier(1).place) {
				var candidate = client.level.getBlockEntity(origin.offset(block[0], block[1], block[2]));
				if (!(candidate instanceof DepotDisplayBlockEntity console)
						|| !(console.getBlockState().getBlock() instanceof io.github.jimbozoomer.jugcraft.tower.MasterConsoleBlock)) continue;
				masters++;
				if (!console.isAnchor() || !core.terminalPos().equals(console.terminal())) throw new AssertionError("Master console is not pinned to its exact tower terminal");
				var renderer = new MasterConsoleRenderer(client.font); var readout = renderer.createRenderState();
				renderer.extractRenderState(console, readout, 0, client.player.position(), null);
				var view = ((io.github.jimbozoomer.jugcraft.drone.DroneTerminalBlockEntity) client.level.getBlockEntity(console.terminal())).view();
				if (view == null || readout.main.size() != 6 || !readout.main.getFirst().equals("TOWER CONTROL")
						|| !readout.main.get(2).equals("FLEET " + (view.docks.size() + view.flights.size()) + "/" + view.maxDrones)
						|| !readout.power.get(2).equals("DRAW " + view.draw())
						|| !readout.supply.get(1).equals("JOBS " + view.openTargets)) {
					throw new AssertionError("Master console instruments are not reading their linked terminal's live fleet, power and jobs");
				}
			}
			if (masters != 1) throw new AssertionError("Expected one master console controller; found " + masters);
			var master = io.github.jimbozoomer.jugcraft.tower.JugcraftTower.BLOCKS.get("tower_master_console");
			var masterSource = MbsPreview.blockSource((net.minecraft.world.item.BlockItem) master.asItem());
			if (masterSource == null) throw new AssertionError("Master console has no real MBS placement preview");
			var floor = origin.offset(0, 0, 4);
			var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(floor)
					.relative(net.minecraft.core.Direction.UP, .5), net.minecraft.core.Direction.UP, floor, false);
			var place = new net.minecraft.world.item.context.BlockPlaceContext(client.player, net.minecraft.world.InteractionHand.MAIN_HAND,
					new net.minecraft.world.item.ItemStack(master), hit);
			var ghost = masterSource.ghost(place);
			if (ghost == null || ghost.cells().size() != 6 || ghost.fits() || !ghost.blocked().contains(place.getClickedPos()))
				throw new AssertionError("Master console preview does not show all six parts or mark an occupied controller cell");
			var facing = client.player.getDirection().getOpposite();
			for (int part = 0; part < 6; part++) {
				var cell = ghost.cells().get(part);
				if (!cell.pos().equals(io.github.jimbozoomer.jugcraft.tower.MasterConsoleBlock.FOOTPRINT.partPos(place.getClickedPos(), facing, part))
						|| cell.state().getValue(io.github.jimbozoomer.jugcraft.tower.MasterConsoleBlock.PART) != part)
					throw new AssertionError("Master console preview uses a different position or part than placement");
			}
			masterHeadroomPreview(client, origin, masterSource);
			int wallScreens = 0;
			for (int[] block : core.layout().tier(1).place) {
				var candidate = client.level.getBlockEntity(origin.offset(block[0], block[1], block[2]));
				if (!(candidate instanceof DepotDisplayBlockEntity screen) || !screen.isAnchor()
						|| !(screen.getBlockState().getBlock() instanceof io.github.jimbozoomer.jugcraft.drone.ControlScreenBlock)) continue;
				wallScreens++;
				if (!core.terminalPos().equals(screen.terminal())) throw new AssertionError("Wall screen is linked to another tower");
				var renderer = new ControlScreenRenderer(client.font); var readout = renderer.createRenderState();
				renderer.extractRenderState(screen, readout, 0, client.player.position(), null);
				var terminal = (io.github.jimbozoomer.jugcraft.drone.DroneTerminalBlockEntity) client.level.getBlockEntity(screen.terminal());
				var view = terminal.view();
				String fleet = view == null ? "" : "FLEET " + (view.docks.size() + view.flights.size()) + "/" + view.maxDrones;
				if (view == null || readout.lines.size() < 7 || readout.lines.stream().noneMatch(line -> plain(line.text()).startsWith(fleet))) {
					throw new AssertionError("Formed wall screen is not displaying the linked terminal's live fleet");
				}
			}
			if (wallScreens != 2) throw new AssertionError("Expected two formed live wall screens; found " + wallScreens);
			for (int[] block : core.layout().tier(1).place) {
				var candidate = client.level.getBlockEntity(origin.offset(block[0], block[1], block[2]));
				if (!(candidate instanceof DepotDisplayBlockEntity table) || !table.isAnchor()
						|| !(table.getBlockState().getBlock() instanceof HoloTableBlock)) continue;
				var renderer = new HoloMapRenderer();
				var state = renderer.createRenderState();
				renderer.extractRenderState(table, state, 0, client.player.position(), null);
				if (state.boxes.size() < 50) throw new AssertionError("The table is missing its linked tower projection");
				for (float[] box : state.boxes) {
					if (box[0] < -1 || box[2] < -1 || box[3] > 2 || box[5] > 2) {
						throw new AssertionError("The tower projection extends beyond the physical 3x3 table");
					}
					if (box[1] < HoloTableBlock.HEIGHT / 16f || box[4] > 4.1f) {
						throw new AssertionError("The tower projection exceeds the operations-table display volume");
					}
				}
				return;
			}
			throw new AssertionError("The tower has no formed hologram-table anchor");
		});
	}
	/** Temporary client-only fixtures verify preview failures without altering the server's tower or world. */
	private static void masterHeadroomPreview(net.minecraft.client.Minecraft client, BlockPos origin,
			io.github.jimbozoomer.jugcraft.placement.StructureGhost.Source source) {
		var level = client.level;
		var block = (io.github.jimbozoomer.jugcraft.tower.MasterConsoleBlock) io.github.jimbozoomer.jugcraft.tower.JugcraftTower.BLOCKS.get("tower_master_console");
		var facing = client.player.getDirection().getOpposite();
		var saved = new java.util.LinkedHashMap<BlockPos, net.minecraft.world.level.block.state.BlockState>();
		java.util.function.BiConsumer<BlockPos, net.minecraft.world.level.block.state.BlockState> put = (at, state) -> {
			saved.putIfAbsent(at, level.getBlockState(at));
			level.setBlock(at, state, net.minecraft.world.level.block.Block.UPDATE_CLIENTS | net.minecraft.world.level.block.Block.UPDATE_KNOWN_SHAPE);
		};
		try {
			BlockPos anchor = origin.offset(37, 2, 37), floor = anchor.below();
			put.accept(floor, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
			for (int part = 0; part < 6; part++) {
				BlockPos at = io.github.jimbozoomer.jugcraft.tower.MasterConsoleBlock.FOOTPRINT.partPos(anchor, facing, part);
				put.accept(at, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
				if (part >= 3) put.accept(at.above(), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
			}
			var context = previewContext(client, block, floor);
			var clear = source.ghost(context);
			if (clear == null || !clear.fits() || block.getStateForPlacement(context) == null) throw new AssertionError("Clear master-console preview disagrees with real placement");
			BlockPos lowCeiling = io.github.jimbozoomer.jugcraft.tower.MasterConsoleBlock.FOOTPRINT.partPos(anchor, facing, 4).above();
			put.accept(lowCeiling, net.minecraft.world.level.block.Blocks.GLASS.defaultBlockState());
			var blocked = source.ghost(context);
			if (blocked == null || blocked.fits() || !blocked.blocked().contains(lowCeiling) || block.getStateForPlacement(context) != null)
				throw new AssertionError("Low glass ceiling is not marked as blocked by both preview and real placement");
			BlockPos topFloor = new BlockPos(anchor.getX(), level.getMaxY() - 1, anchor.getZ());
			put.accept(topFloor, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
			var top = previewContext(client, block, topFloor); var high = source.ghost(top);
			if (high == null || high.fits() || block.getStateForPlacement(top) != null
					|| high.blocked().stream().filter(level::isOutsideBuildHeight).count() != 3)
				throw new AssertionError("Topmost console placement preview does not mark all three out-of-world monitor clearances");
		} finally {
			for (var original : saved.entrySet()) level.setBlock(original.getKey(), original.getValue(),
					net.minecraft.world.level.block.Block.UPDATE_CLIENTS | net.minecraft.world.level.block.Block.UPDATE_KNOWN_SHAPE);
		}
	}
	private static net.minecraft.world.item.context.BlockPlaceContext previewContext(net.minecraft.client.Minecraft client,
			io.github.jimbozoomer.jugcraft.tower.MasterConsoleBlock block, BlockPos floor) {
		var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(floor)
				.relative(net.minecraft.core.Direction.UP, .5), net.minecraft.core.Direction.UP, floor, false);
		return new net.minecraft.world.item.context.BlockPlaceContext(client.player, net.minecraft.world.InteractionHand.MAIN_HAND,
				new net.minecraft.world.item.ItemStack(block), hit);
	}
	private static String plain(net.minecraft.util.FormattedCharSequence text) {
		StringBuilder out = new StringBuilder();
		text.accept((index, style, codepoint) -> { out.appendCodePoint(codepoint); return true; });
		return out.toString();
	}
}
