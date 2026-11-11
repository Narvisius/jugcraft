package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.drone.DepotDisplayBlockEntity;
import io.github.jimbozoomer.jugcraft.drone.HoloTableBlock;
import io.github.jimbozoomer.jugcraft.test.TriPillarArena;
import io.github.jimbozoomer.jugcraft.tower.TowerCoreBlockEntity;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.BlockPos;

/** Checks the actual client projections and readouts of a Drone Tower (the Tri-Pillar's control building). */
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

	/**
	 * The control building of the Tri-Pillar whose core is at {@code origin} (built to tier 1 or more, so its terminal is
	 * linked to the tower): every desk, the Tower Control Console and the three Curved Video Walls are pinned to the tower's
	 * terminal out in the yard and show its live readout (the console's floor band joining its raised floor); the walls
	 * stand round the core, one of each board; the console's placement preview matches real placement; the central
	 * table shows the tower model over the table and under its projector, with the status ticker on the atrium's amber
	 * edge.
	 */
	public static void hologramFitsTable(ClientGameTestContext context, BlockPos origin) {
		context.runOnClient(client -> {
			var core = (TowerCoreBlockEntity) client.level.getBlockEntity(origin);
			var data = core.layout();
			int expectedDesks = 0;
			for (int[] block : data.foundation.place) {
				var kind = data.state(block[3]).getBlock();
				if (kind instanceof io.github.jimbozoomer.jugcraft.tower.ConsoleDeskBlock) expectedDesks++;
			}
			int desks = 0;
			for (int[] block : data.foundation.place) {
				var candidate = client.level.getBlockEntity(origin.offset(block[0], block[1], block[2]));
				if (!(candidate instanceof DepotDisplayBlockEntity desk)
						|| !(desk.getBlockState().getBlock() instanceof io.github.jimbozoomer.jugcraft.tower.ConsoleDeskBlock)) continue;
				desks++;
				if (!core.terminalPos().equals(desk.terminal())) throw new AssertionError("Desk is linked to another tower");
				// each desk draws its row's role's page for its place in the row, from the linked terminal's live view
				// (work list item 14): the role's code in the title bar, the tower's tier by the clock, and on the flight
				// director's first page the fleet as the terminal has it
				var renderer = new ConsoleDeskRenderer(client.font); var readout = renderer.createRenderState();
				renderer.extractRenderState(desk, readout, 0, null, null);
				var terminal = (io.github.jimbozoomer.jugcraft.drone.DroneTerminalBlockEntity) client.level.getBlockEntity(desk.terminal());
				var view = terminal.view();
				var deskState = desk.getBlockState();
				int role = deskState.getValue(io.github.jimbozoomer.jugcraft.tower.ConsoleDeskBlock.ROLE);
				var texts = ConsoleDeskRenderer.page(readout);
				if (view == null || !readout.desk || readout.role != role || readout.page != io.github.jimbozoomer.jugcraft.tower.ConsoleDeskBlock.page(deskState)
						|| texts.stream().noneMatch(t -> t.s().equals(DeskPages.CODES[role]))
						|| texts.stream().noneMatch(t -> t.s().startsWith("T" + view.towerTier + "  "))) {
					throw new AssertionError("Desk at " + desk.getBlockPos() + " is not drawing its role's live page");
				}
				if (role == 0 && readout.page == 0 && texts.stream().noneMatch(t -> t.s().equals((view.docks.size() + view.flights.size()) + "/" + view.maxDrones))) {
					throw new AssertionError("The flight director's fleet summary is not the linked terminal's real live fleet");
				}
			}
			if (desks == 0 || desks != expectedDesks) throw new AssertionError("Expected the foundation's " + expectedDesks + " desks; found " + desks);
			int consoles = 0;
			for (int[] block : data.foundation.place) {
				var candidate = client.level.getBlockEntity(origin.offset(block[0], block[1], block[2]));
				if (!(candidate instanceof DepotDisplayBlockEntity console)
						|| !(console.getBlockState().getBlock() instanceof io.github.jimbozoomer.jugcraft.tower.TowerControlConsoleBlock)) continue;
				consoles++;
				if (!console.isAnchor() || !core.terminalPos().equals(console.terminal())) throw new AssertionError("Tower Control Console is not pinned to its exact tower terminal");
				var mesh = ConsoleMesh.get(TowerControlConsoleRenderer.MESH_NAME);
				if (mesh == null || mesh.live.length != 28 || mesh.quads[ConsoleMesh.SOLID].length == 0)
					throw new AssertionError("The Tower Control Console's mesh did not load with its 28 live faces");
				if (ConsoleMesh.get("console_chair") == null) throw new AssertionError("The Console Chair's mesh did not load");
				var renderer = new TowerControlConsoleRenderer(client.font); var readout = renderer.createRenderState();
				renderer.extractRenderState(console, readout, 0, client.player.position(), null);
				var view = ((io.github.jimbozoomer.jugcraft.drone.DroneTerminalBlockEntity) client.level.getBlockEntity(console.terminal())).view();
				String fleet = view == null ? "" : (view.docks.size() + view.flights.size()) + " / " + view.maxDrones;
				String draw = view == null ? "" : String.format(java.util.Locale.ROOT, "%,d", view.draw()).replace(',', ' ') + " JE/t";
				if (view == null || readout.texts.stream().noneMatch(t -> t.s().equals(fleet)) || readout.texts.stream().noneMatch(t -> t.s().equals(draw)))
					throw new AssertionError("The Tower Control Console's pages are not reading their linked terminal's live fleet and power");
				// since the rotunda cut the band's ends step down to the hall floor (east and west); towards its operators it
				// joins the raised floor
				for (int e = 0; e < Math.min(readout.exposed.length, mesh.edgeCells.length); e++) {
					if (readout.exposed[e] && mesh.edgeCells[e][2] == 0)
						throw new AssertionError("The console's floor band shows a step edge towards its operators: it should join the raised floor");
				}
			}
			if (consoles != 1) throw new AssertionError("Expected one Tower Control Console; found " + consoles);
			var consoleBlock = (io.github.jimbozoomer.jugcraft.tower.TowerControlConsoleBlock) io.github.jimbozoomer.jugcraft.tower.JugcraftTower.TOWER_CONTROL_CONSOLE;
			var consoleSource = MbsPreview.blockSource((net.minecraft.world.item.BlockItem) consoleBlock.asItem());
			if (consoleSource == null) throw new AssertionError("The Tower Control Console has no placement preview");
			// The floor under the hall console's controller: placing there is blocked by the console itself.
			var floor = origin.offset(TriPillarArena.MASTER_CONSOLE).below();
			var place = previewContext(client, consoleBlock, floor);
			var ghost = consoleSource.ghost(place);
			int cells = io.github.jimbozoomer.jugcraft.tower.TowerControlConsoleBlock.FOOTPRINT.size();
			if (ghost == null || ghost.cells().size() != cells || ghost.fits() || !ghost.blocked().contains(place.getClickedPos()))
				throw new AssertionError("The console's preview does not show all " + cells + " parts or mark its occupied controller cell");
			var facing = client.player.getDirection().getOpposite();
			for (int part = 0; part < cells; part++) {
				var cell = ghost.cells().get(part);
				if (!cell.pos().equals(io.github.jimbozoomer.jugcraft.tower.TowerControlConsoleBlock.partPos(place.getClickedPos(), facing, part))
						|| cell.state().getValue(io.github.jimbozoomer.jugcraft.tower.TowerControlConsoleBlock.PART) != part)
					throw new AssertionError("The console's preview uses a different position or part than placement");
			}
			consoleHeadroomPreview(client, consoleSource);
			// the Curved Video Walls on the rotunda's cut faces: one of each board, aimed at the core, pinned, live
			int walls = 0;
			java.util.Set<io.github.jimbozoomer.jugcraft.tower.CurvedVideoWallBlock.Board> boards = java.util.EnumSet.noneOf(
					io.github.jimbozoomer.jugcraft.tower.CurvedVideoWallBlock.Board.class);
			for (int[] block : data.foundation.place) {
				var candidate = client.level.getBlockEntity(origin.offset(block[0], block[1], block[2]));
				if (!(candidate instanceof DepotDisplayBlockEntity wall)
						|| !(wall.getBlockState().getBlock() instanceof io.github.jimbozoomer.jugcraft.tower.CurvedVideoWallBlock)) continue;
				walls++;
				var state = wall.getBlockState();
				var board = state.getValue(io.github.jimbozoomer.jugcraft.tower.CurvedVideoWallBlock.BOARD);
				boards.add(board);
				int[] v = io.github.jimbozoomer.jugcraft.tower.CurvedVideoWallBlock.AIMS[state.getValue(io.github.jimbozoomer.jugcraft.tower.CurvedVideoWallBlock.AIM)];
				if (wall.getBlockPos().getX() + v[0] != origin.getX() || wall.getBlockPos().getZ() + v[1] != origin.getZ())
					throw new AssertionError("A video wall is not aimed at the tower's core: " + wall.getBlockPos());
				if (!core.terminalPos().equals(wall.terminal())) throw new AssertionError("A video wall is not pinned to its tower's terminal");
				var mesh = ConsoleMesh.get(CurvedVideoWallRenderer.MESH_NAME);
				if (mesh == null || mesh.live.length != 30 || mesh.quads[ConsoleMesh.SOLID].length == 0 || CurvedVideoWallRenderer.facets() == null)
					throw new AssertionError("The video wall's mesh did not load with its 30 facets");
				var renderer = new CurvedVideoWallRenderer(client.font); var readout = renderer.createRenderState();
				renderer.extractRenderState(wall, readout, 0, client.player.position(), null);
				var view = ((io.github.jimbozoomer.jugcraft.drone.DroneTerminalBlockEntity) client.level.getBlockEntity(wall.terminal())).view();
				StringBuilder all = new StringBuilder();
				readout.texts.forEach(t -> all.append(t.s()).append('|'));
				String joined = all.toString().replace("|", "");
				int fleet = view == null ? -1 : view.docks.size() + view.flights.size();
				String expected = switch (board) {
					case OPERATIONS -> fleet + " / " + (view == null ? 0 : view.maxDrones);
					case FLEET -> fleet + " DRONES";
					case LOGISTICS -> "BATTERY " + (view == null || view.capacity <= 0 ? 0 : view.energy * 100 / view.capacity) + " %";
				};
				// an idle depot's boards come to 470-820 quads; "NO DEPOT LINK" draws about 100
				if (view == null || readout.board.size() < 300 || !joined.contains(expected))
					throw new AssertionError("The " + board + " video wall is not drawing its linked terminal's live board (wanted '" + expected + "', "
							+ readout.board.size() + " quads)");
			}
			if (walls != 3 || boards.size() != 3) throw new AssertionError("Expected three video walls, one of each board; found " + walls + " " + boards);
			// the lobby's Drone Display Cases: the mesh with its glass and its four ticker faces, the drone (made to fit under
			// the glass) and its name four times round the rail
			int cases = 0;
			for (int[] block : data.foundation.place) {
				var candidate = client.level.getBlockEntity(origin.offset(block[0], block[1], block[2]));
				if (!(candidate instanceof DepotDisplayBlockEntity box)
						|| !(box.getBlockState().getBlock() instanceof io.github.jimbozoomer.jugcraft.tower.DroneDisplayCaseBlock)) continue;
				cases++;
				var mesh = ConsoleMesh.get(DroneDisplayCaseRenderer.MESH_NAME);
				var rail = mesh == null ? null : DroneDisplayCaseRenderer.rail(mesh);
				if (mesh == null || mesh.live.length != 4 || mesh.quads[ConsoleMesh.SOLID].length == 0 || mesh.quads[ConsoleMesh.GLASS].length == 0
						|| rail == null || rail.columns() != 4 * DroneDisplayCaseRenderer.COLUMNS)
					throw new AssertionError("The display case's mesh did not load with its glass and its four ticker faces");
				int tier = box.getBlockState().getValue(io.github.jimbozoomer.jugcraft.tower.DroneDisplayCaseBlock.TIER);
				String text = DroneDisplayCaseRenderer.tickerText(tier);
				String name = tier == 4 ? "FAN-WING RUNNER" : tier == 1 ? "COURIER QUAD" : null;
				if (name == null || !text.equals("T" + tier + " \u00B7 " + name))
					throw new AssertionError("The display case's rail does not name its drone: " + text);
				var renderer = new DroneDisplayCaseRenderer(null); var readout = renderer.createRenderState();
				renderer.extractRenderState(box, readout, 0, client.player.position(), null);
				int lit = 0;
				for (int column : TickerRail.columns(text)) lit += Integer.bitCount(column);
				if (readout.dots.size() != 4 * lit)
					throw new AssertionError("The display case's rail does not show its drone's name four times: " + readout.dots.size() + " dots for " + lit + " a name");
				float[] reach = DroneMesh.reach(tier);
				if (reach == null || reach[0] * readout.scale > 2.16f || reach[1] * readout.scale > 2.21f)
					throw new AssertionError("The display case's drone does not fit under its glass");
			}
			if (cases != 2) throw new AssertionError("Expected the lobby's two display cases; found " + cases);
			// the lobby's Crystal Tree: the planter's mesh, the atlas map, the tree grown from its seed (another seed, another
			// tree), within its planter and under the hall's ceiling, and live (it draws)
			int trees = 0;
			for (int[] block : data.foundation.place) {
				var candidate = client.level.getBlockEntity(origin.offset(block[0], block[1], block[2]));
				if (!(candidate instanceof DepotDisplayBlockEntity treeBe)
						|| !(treeBe.getBlockState().getBlock() instanceof io.github.jimbozoomer.jugcraft.tower.CrystalTreeBlock)) continue;
				trees++;
				var planter = ConsoleMesh.get(CrystalTreeRenderer.MESH_NAME);
				if (planter == null || planter.quads[ConsoleMesh.SOLID].length == 0 || planter.quads[ConsoleMesh.GLOW].length == 0)
					throw new AssertionError("The Crystal Tree's planter mesh did not load");
				if (!CrystalTreeRenderer.tiles().containsKey("crystal") || !CrystalTreeRenderer.tiles().containsKey("halo"))
					throw new AssertionError("The Crystal Tree's atlas map did not load");
				long seed = io.github.jimbozoomer.jugcraft.tower.CrystalTreeBlock.seed(treeBe.getBlockPos());
				var tree = CrystalTreeRenderer.tree(seed);
				var other = CrystalTreeRenderer.tree(io.github.jimbozoomer.jugcraft.tower.CrystalTreeBlock.seed(treeBe.getBlockPos().east(9)));
				if (tree.quads() < 20000 || tree.halos.length / CrystalTreeGeometry.HALO < 500 || tree.threads.length == 0)
					throw new AssertionError("The Crystal Tree grew too little: " + tree.quads() + " quads");
				if (java.util.Arrays.equals(tree.bark, other.bark))
					throw new AssertionError("Two Crystal Trees in different places grew alike");
				for (int o = 0; o < tree.bark.length; o += CrystalTreeGeometry.BARK) {
					double r = Math.hypot(tree.bark[o] - 0.5, tree.bark[o + 2] - 0.5);
					if (r > 4.0 || tree.bark[o + 1] > 8.6 || tree.bark[o + 1] < 0.3)
						throw new AssertionError("The Crystal Tree reaches out of its planter or into the ceiling: r " + r + ", y " + tree.bark[o + 1]);
				}
				var renderer = new CrystalTreeRenderer(null); var readout = renderer.createRenderState();
				renderer.extractRenderState(treeBe, readout, 0, client.player.position(), null);
				if (!readout.present || readout.seed != seed) throw new AssertionError("The Crystal Tree's renderer does not see its tree");
			}
			if (trees != 1) throw new AssertionError("Expected the lobby's Crystal Tree; found " + trees);
			// the lounge's Long Coffee Table: its mesh with the runner's glass, the floor wash and the two ticker lines, each
			// line a rail of 226 LED columns, and the words running along them (Justin's: brought to you by Narv-Tech)
			int tables = 0;
			for (int[] block : data.foundation.place) {
				var candidate = client.level.getBlockEntity(origin.offset(block[0], block[1], block[2]));
				if (!(candidate instanceof DepotDisplayBlockEntity tableBe)
						|| !(tableBe.getBlockState().getBlock() instanceof io.github.jimbozoomer.jugcraft.tower.LongCoffeeTableBlock)) continue;
				tables++;
				var mesh = ConsoleMesh.get(LongCoffeeTableRenderer.MESH_NAME);
				var lines = mesh == null ? null : LongCoffeeTableRenderer.lines(mesh);
				if (mesh == null || mesh.quads[ConsoleMesh.SOLID].length == 0 || mesh.quads[ConsoleMesh.GLASS].length == 0
						|| mesh.quads[ConsoleMesh.GLOW].length == 0 || lines == null)
					throw new AssertionError("The Long Coffee Table's mesh did not load with its glass, its wash and its two ticker lines");
				for (var line : lines) {
					if (line.rail().columns() != 226) throw new AssertionError("A ticker line of the table has " + line.rail().columns() + " columns, not 226");
				}
				if (!LongCoffeeTableRenderer.TEXT.startsWith("DRONE TOWER BROUGHT TO YOU BY NARV-TECH."))
					throw new AssertionError("The table's ticker says the wrong thing: " + LongCoffeeTableRenderer.TEXT);
				var renderer = new LongCoffeeTableRenderer(null); var readout = renderer.createRenderState();
				renderer.extractRenderState(tableBe, readout, 0, client.player.position(), null);
				if (!readout.present || readout.dots.size() < 400)
					throw new AssertionError("The table's ticker does not run: " + readout.dots.size() + " dots");
			}
			if (tables != 1) throw new AssertionError("Expected the lounge's Long Coffee Table; found " + tables);
			// the workshop's Drone Parts Shelving (item 9): both stockings' meshes load; stock a's live faces are its 8 packs'
			// four gauge LEDs, the 8 slot LEDs and the two screens; the renderer sees both units; every pack charges through
			// its four LEDs and is ready at three
			for (String stock : new String[] {"a", "b"}) {
				var mesh = ConsoleMesh.get(DronePartsShelvingRenderer.meshName(stock));
				if (mesh == null || mesh.quads[ConsoleMesh.SOLID].length == 0)
					throw new AssertionError("The Drone Parts Shelving's mesh " + stock + " did not load");
				if (stock.equals("a")) {
					int gauges = 0, slots = 0, screens = 0;
					for (byte k : mesh.liveKind) {
						if (k == DronePartsShelvingRenderer.GAUGE) gauges++;
						else if (k == DronePartsShelvingRenderer.SLOT) slots++;
						else if (k == DronePartsShelvingRenderer.STATION || k == DronePartsShelvingRenderer.CHARGER) screens++;
					}
					if (gauges != 32 || slots != 8 || screens != 2)
						throw new AssertionError("The shelving's live faces: " + gauges + " gauge LEDs, " + slots + " slot LEDs, " + screens + " screens");
				}
			}
			for (int p = 0; p < 8; p++) {
				java.util.Set<Integer> seen = new java.util.HashSet<>();
				for (double t = 0; t < DronePartsShelvingRenderer.PACK_CYCLE; t += 10) seen.add(DronePartsShelvingRenderer.packLevel(p, t));
				if (!seen.containsAll(java.util.List.of(1, 2, 3, 4)))
					throw new AssertionError("Pack " + p + " never shows all four gauge levels: " + seen);
			}
			int shelves = 0;
			for (BlockPos at : TriPillarArena.PARTS_SHELVING) {
				var candidate = client.level.getBlockEntity(origin.offset(at));
				if (!(candidate instanceof DepotDisplayBlockEntity shelfBe)) continue;
				var renderer = new DronePartsShelvingRenderer(null);
				var readout = renderer.createRenderState();
				renderer.extractRenderState(shelfBe, readout, 0, client.player.position(), null);
				if (readout.present) shelves++;
			}
			if (shelves != 2) throw new AssertionError("Expected both Drone Parts Shelving units on the parts wall; the renderer saw " + shelves);
			// r177 (Justin's frame rate, "Static parts baked in"): the shelving's mesh is baked into the chunk (client/BakedFixtures):
			// each master's model is the baking one and puts out every solid and full-bright quad of its stock's mesh
			if (BakedFixtures.ON) {
				int baked = 0;
				for (BlockPos at : TriPillarArena.PARTS_SHELVING) {
					BlockPos p = origin.offset(at);
					var st = client.level.getBlockState(p);
					if (!(st.getBlock() instanceof io.github.jimbozoomer.jugcraft.tower.DronePartsShelvingBlock)) continue;
					var model = client.getModelManager().getBlockStateModelSet().get(st);
					if (!(model instanceof BakedFixtures))
						throw new AssertionError("The shelving's model is not the baked one: " + model.getClass().getSimpleName());
					int[] n = {0};
					model.emitQuads(net.fabricmc.fabric.api.client.renderer.v1.Renderer.get().quadEmitter(q -> n[0]++), client.level, p, st,
							net.minecraft.util.RandomSource.create(0), d -> false);
					var mesh = ConsoleMesh.get(DronePartsShelvingRenderer.meshName(st.getValue(io.github.jimbozoomer.jugcraft.tower.DronePartsShelvingBlock.STOCK)
							.getSerializedName()));
					int want = (mesh.quads[ConsoleMesh.SOLID].length + mesh.quads[ConsoleMesh.LIT].length) / (4 * ConsoleMesh.CORNER);
					if (n[0] != want) throw new AssertionError("The baked shelving put out " + n[0] + " quads, not its mesh's " + want);
					baked++;
				}
				if (baked != 2) throw new AssertionError("Expected both shelving units baked; found " + baked);
			}
			// the service bay's robot arm (item 8): its seven meshes load; at every stop of its cycle, and all along the
			// moves between them, its joints put the socket's tip where it was sent (within reach: the forward kinematics
			// as drawn agree with the inverse within a millimetre); the drone it works stands side-on to it (facing north)
			for (String part : ServiceArm.PARTS) {
				var mesh = ConsoleMesh.get("service_arm_" + part);
				if (mesh == null || mesh.quads[ConsoleMesh.SOLID].length + mesh.quads[ConsoleMesh.LIT].length == 0)
					throw new AssertionError("The service arm's " + part + " mesh did not load");
			}
			for (double t = 0; t < ServiceArm.LENGTH; t += 0.05) {
				int i = ServiceArm.CYCLE.length - 1;
				while (i > 0 && ServiceArm.ARRIVE[i] > t) i--;
				var a = ServiceArm.CYCLE[i];
				var b = ServiceArm.CYCLE[(i + 1) % ServiceArm.CYCLE.length];
				double in = t - ServiceArm.ARRIVE[i];
				double e = in < a.hold() ? 0 : (1 - Math.cos(Math.min(1, (in - a.hold()) / ServiceArm.MOVE[i]) * Math.PI)) / 2;
				double x = a.x() + (b.x() - a.x()) * e, y = a.y() + (b.y() - a.y()) * e, z = a.z() + (b.z() - a.z()) * e;
				var pose = ServiceArm.pose(t);
				var tip = ServiceArm.tip(new double[] {pose.j1(), pose.j2(), pose.fore(), pose.wrist()});
				double miss = Math.sqrt((tip.x - x) * (tip.x - x) + (tip.y - y) * (tip.y - y) + (tip.z - z) * (tip.z - z));
				if (miss > 0.001)
					throw new AssertionError("The service arm misses its stop by " + miss + " m at " + t + " s of its cycle (" + a.name() + ")");
			}
			if (ServiceArm.LENGTH < 20 || ServiceArm.LENGTH > 40)
				throw new AssertionError("The service arm's cycle takes " + ServiceArm.LENGTH + " s");
			BlockPos standAt = origin.offset(TriPillarArena.SERVICE_STAND), armAt = origin.offset(TriPillarArena.SERVICE_ARM);
			var stand = client.level.getBlockState(standAt);
			if (!stand.hasProperty(io.github.jimbozoomer.jugcraft.tower.HallFixtureBlock.FACING)
					|| stand.getValue(io.github.jimbozoomer.jugcraft.tower.HallFixtureBlock.FACING) != net.minecraft.core.Direction.NORTH
					|| !(client.level.getBlockEntity(armAt) instanceof DepotDisplayBlockEntity))
				throw new AssertionError("The service bay's T4 does not lie side-on to its arm: " + stand + ", arm " + client.level.getBlockState(armAt));
		});
		// The central table: its tower model is worked out off the render thread, so it is looked at until it is there
		// (and the ticker, which moves along the ring, until one of its letters is on the amber edge).
		boolean ring = context.computeOnClient(client -> client.level.getBlockState(origin.offset(TriPillarArena.TICKER_EDGE))
				.is(io.github.jimbozoomer.jugcraft.test.TowerGameTests.modBlock("amber_light_strip"))
				|| io.github.jimbozoomer.jugcraft.tower.JugcraftTower.isNeonLine(client.level.getBlockState(origin.offset(TriPillarArena.TICKER_EDGE))));
		int wanted = ring ? MODEL | TICKER : MODEL, seen = 0;
		for (int i = 0; i < 400 && (seen & wanted) != wanted; i++) {
			seen |= context.computeOnClient(client -> towerHologram(client, origin));
			if ((seen & wanted) != wanted) context.waitTicks(1);
		}
		if ((seen & MODEL) == 0) throw new AssertionError("The central table's tower model was not worked out within 400 ticks");
		if ((seen & wanted) != wanted) throw new AssertionError("The status ticker never showed on the atrium's amber edge");
		context.runOnClient(client -> holoProjector(client, origin));
	}

	/**
	 * The Holo Projector in the lobby (work list item 15): its housing and the head load whole (every triangle, eye triangle
	 * and card quad with its visibility bits); seen from in front, only the head's nearest surface is drawn, and from behind
	 * a different part of it; the hologram builds tens of thousands of quads, all in the lobby's air (over the deck, under the
	 * ceiling, inside the ring's reach); the head is turned at whoever it watches, or with nobody near, where the projector
	 * faces.
	 */
	private static void holoProjector(net.minecraft.client.Minecraft client, BlockPos origin) {
		BlockPos at = origin.offset(TriPillarArena.HOLO_PROJECTOR);
		if (!(client.level.getBlockEntity(at) instanceof DepotDisplayBlockEntity be)
				|| !(be.getBlockState().getBlock() instanceof io.github.jimbozoomer.jugcraft.tower.HoloProjectorBlock))
			throw new AssertionError("No Holo Projector in the lobby at " + at + ": " + client.level.getBlockState(at));
		var housing = ConsoleMesh.get(HoloProjectorRenderer.MESH_NAME);
		if (housing == null || housing.quads[ConsoleMesh.SOLID].length == 0 || housing.quads[ConsoleMesh.LIT].length == 0)
			throw new AssertionError("The Holo Projector's housing mesh did not load with its solid and lit faces");
		var mesh = HoloHeadMesh.get();
		if (mesh == null) throw new AssertionError("The holo head did not load");
		if (mesh.prims != mesh.triCount + 2 * mesh.eyeTriCount + mesh.quadCount || mesh.triCount < 40000 || mesh.eyeTriCount == 0
				|| mesh.quadCount == 0 || mesh.seen.length != mesh.dirs.length / 3 || mesh.ringGlyph.length == 0 || mesh.ringLength <= 0)
			throw new AssertionError("The holo head's parts do not add up: " + mesh.triCount + " triangles, " + mesh.eyeTriCount
					+ " per eye, " + mesh.quadCount + " card quads, " + mesh.prims + " with visibility");
		// in head space (units of the head's height, the face looking along +z), a camera far in front and one far behind
		// (offline: 61% of the triangles from in front, 13% from behind, under half of those from both)
		int front = 0, back = 0, both = 0;
		for (int p = 0; p < mesh.triCount; p++) {
			boolean f = mesh.seen(p, 0, 0.5f, 8), b = mesh.seen(p, 0, 0.5f, -8);
			if (f) front++;
			if (b) back++;
			if (f && b) both++;
		}
		if (front < mesh.triCount / 4 || front > mesh.triCount * 3 / 4 || back < mesh.triCount / 20 || both > back * 3 / 4)
			throw new AssertionError("The holo head does not draw only its nearest surface: " + front + " seen from in front, " + back
					+ " from behind, " + both + " from both, of " + mesh.triCount);
		var renderer = new HoloProjectorRenderer(null);
		var state = renderer.createRenderState();
		renderer.extractRenderState(be, state, 0, client.player.position(), null);
		if (!state.drawn || state.corners < 4 * 10000 || state.corners % 4 != 0 || state.colour.length < state.corners)
			throw new AssertionError("The Holo Projector's hologram did not build: " + state.corners + " corners");
		for (int c = 0; c < state.corners; c++) {
			int o = c * HoloProjectorGeometry.Builder.FLOATS;
			float x = state.holo[o], y = state.holo[o + 1], z = state.holo[o + 2];
			if (!(y > io.github.jimbozoomer.jugcraft.tower.HoloProjectorBlock.SLAB && y < 9 && Math.hypot(x - 0.5, z - 0.5) < 4.6))
				throw new AssertionError("The hologram leaves the lobby's air over its projector: corner " + c + " at " + x + ", " + y + ", " + z);
		}
		var facing = be.getBlockState().getValue(io.github.jimbozoomer.jugcraft.tower.HoloProjectorBlock.FACING);
		double want = state.watching ? Math.atan2(state.target[0] - 0.5, state.target[2] - 0.5) : HoloProjectorRenderer.facingYaw(facing);
		double off = Math.abs(Math.IEEEremainder(state.yaw - want, 2 * Math.PI));
		if (off > 0.2)
			throw new AssertionError("The holo head is not turned " + (state.watching ? "at the player it watches" : "where its projector faces")
					+ ": yaw " + state.yaw + ", wanted " + want);
		if (!state.watching && client.player.getEyePosition().distanceTo(net.minecraft.world.phys.Vec3.atBottomCenterOf(at).add(0, HoloProjectorGeometry.HEAD_Y, 0))
				< HoloProjectorRenderer.WATCH - 0.5)
			throw new AssertionError("The holo head does not watch the player within " + HoloProjectorRenderer.WATCH + " blocks");
	}

	/**
	 * The Cradle Struts under the tier-2 supply cradle (work list item 12): their mesh loads (the members cut in pieces
	 * along their length for their shading, every corner shaded), all of it inside the struts' reach from their master
	 * (from the chamfer up to the deck's underside, out under the cradle), drawn unturned for the pair facing south.
	 */
	public static void cradleStruts(ClientGameTestContext context, BlockPos origin) {
		context.runOnClient(client -> {
			BlockPos at = origin.offset(TriPillarArena.CRADLE_STRUTS);
			if (!(client.level.getBlockEntity(at) instanceof DepotDisplayBlockEntity be)
					|| !(be.getBlockState().getBlock() instanceof io.github.jimbozoomer.jugcraft.tower.CradleStrutsBlock))
				throw new AssertionError("No Cradle Struts under the tier-2 cradle at " + at + ": " + client.level.getBlockState(at));
			var mesh = ConsoleMesh.get(CradleStrutsRenderer.MESH_NAME);
			if (mesh == null || mesh.quads[ConsoleMesh.SOLID].length < 1000 * 4 * ConsoleMesh.CORNER)
				throw new AssertionError("The Cradle Struts' mesh did not load whole");
			float[] q = mesh.quads[ConsoleMesh.SOLID];
			float low = 1;
			for (int o = 0; o + ConsoleMesh.CORNER <= q.length; o += ConsoleMesh.CORNER) {
				float x = q[o], y = q[o + 1], z = q[o + 2], shade = q[o + 8];
				if (x < -4.5f || x > 3.5f || y < -0.5f || y > 7.001f || z < -0.5f || z > 7.5f)
					throw new AssertionError("The struts' mesh leaves their reach: " + x + ", " + y + ", " + z);
				low = Math.min(low, shade);
			}
			if (low <= 0 || low > 0.8f) throw new AssertionError("The struts' mesh has no shading baked in: least " + low);
			var renderer = new CradleStrutsRenderer(null);
			var state = renderer.createRenderState();
			renderer.extractRenderState(be, state, 0, client.player.position(), null);
			if (state.turns != 0) throw new AssertionError("The struts facing south are turned " + state.turns + " quarters");
		});
	}

	/**
	 * The Big Map Table (work list item 16): its mesh whole (the table within its 13 x 13 octagon and 0.87 high, its four
	 * corner panels as live faces); with the player in the map room the server scans the world round the tower and the
	 * client makes the map's mesh: inside the well, over the table's floor, never higher than the headroom over it, the
	 * tower's own blocks standing on it; the panels say what the map shows.
	 */
	public static void bigMapTable(ClientGameTestContext context, BlockPos origin) {
		BlockPos at = origin.offset(TriPillarArena.BIG_MAP_TABLE);
		context.runOnClient(client -> {
			var mesh = ConsoleMesh.get(BigMapTableRenderer.MESH_NAME);
			if (mesh == null || mesh.quads[ConsoleMesh.SOLID].length < 800 * 4 * ConsoleMesh.CORNER)
				throw new AssertionError("The Big Map Table's mesh did not load whole");
			for (float[] q : new float[][] {mesh.quads[ConsoleMesh.SOLID], mesh.quads[ConsoleMesh.LIT]}) {
				for (int o = 0; o + ConsoleMesh.CORNER <= q.length; o += ConsoleMesh.CORNER) {
					float x = q[o] - 0.5f, y = q[o + 1], z = q[o + 2] - 0.5f;
					// 0.87 the top; its four corner touch panels' bezels, tilted 18 degrees, stand up to 0.955 (tools/big_map_table.py)
					if (Math.abs(x) > 6.75f || Math.abs(z) > 6.75f || y < -0.001f || y > 0.96f)
						throw new AssertionError("The table's mesh leaves its octagon: " + x + ", " + y + ", " + z);
				}
			}
			int panels = 0;
			for (int f = 0; f < mesh.live.length; f++) if (mesh.liveKind[f] == ConsoleMesh.LCD) panels++;
			if (panels != 4) throw new AssertionError("The table has " + panels + " corner panels, not 4");
		});
		// stand in the room: the scan starts with someone near, a map comes in within its first refresh
		BigMap.Mesh map = null;
		for (int k = 0; k < 60 && map == null; k++) {
			context.waitTicks(20);
			map = context.computeOnClient(client -> BigMap.mesh(at));
		}
		if (map == null) throw new AssertionError("No map came in for the Big Map Table at " + at + " in a minute");
		BigMap.Mesh shown = map;
		context.runOnClient(client -> {
			double reach = io.github.jimbozoomer.jugcraft.tower.MapScan.MAP_R + 0.05;
			int towerCells = 0;
			for (float[][] lots : new float[][][] {shown.solid, shown.glow}) {
				for (float[] q : lots) {
					for (int o = 0; o + BigMap.STRIDE <= q.length; o += 4) {
						double x = q[o] - 0.5, y = q[o + 1], z = q[o + 2] - 0.5;
						if (Math.hypot(x, z) > reach + 0.2 || y < BigMap.FLOOR - 1e-4 || y > BigMap.FLOOR + io.github.jimbozoomer.jugcraft.tower.MapScan.HEADROOM + 0.01)
							throw new AssertionError("The map leaves the well or its headroom: " + x + ", " + y + ", " + z);
					}
				}
			}
			var grid = shown.grid;
			if (!grid.centre.equals(origin)) throw new AssertionError("The map is round " + grid.centre + ", not the tower's core " + origin);
			// the control building stands on the map: the samples over its floor are higher than the land round it
			int middle = (grid.n / 2) * grid.n + grid.n / 2;
			if (grid.ground[middle] <= origin.getY()) throw new AssertionError("The tower is not on its own map: " + grid.ground[middle]);
			var renderer = new BigMapTableRenderer();
			var state = renderer.createRenderState();
			if (!(client.level.getBlockEntity(at) instanceof io.github.jimbozoomer.jugcraft.tower.BigMapTableBlockEntity be))
				throw new AssertionError("No Big Map Table at " + at);
			renderer.extractRenderState(be, state, 0, client.player.getEyePosition(), null);
			if (state.map == null || !state.reach.startsWith("VIEW ") || !state.chips[0])
				throw new AssertionError("The panels do not show the map: " + state.reach);
		});
	}

	/**
	 * The Tower Diorama Case (layout 44: the diorama's own case, where the hall's small table stood): whole, its anchor
	 * drawing the tower diorama, whose model loads whole and stands inside the case's 3x3, over the tray and under its
	 * height; and a frame draws only some of it (the faces turned away are left out).
	 */
	public static void towerDiorama(ClientGameTestContext context, BlockPos origin) {
		context.runOnClient(client -> {
			BlockPos at = origin.offset(TriPillarArena.MODEL_TABLE);
			var block = client.level.getBlockState(at);
			if (!(block.getBlock() instanceof io.github.jimbozoomer.jugcraft.tower.RoomKitBlock kit) || !"diorama".equals(kit.spec.display())
					|| !kit.isAnchor(block)) throw new AssertionError("No Tower Diorama Case anchored at " + at + ": " + block);
			for (BlockPos cell : kit.cells(at, block)) {
				if (client.level.getBlockState(cell).getBlock() != kit) throw new AssertionError("The diorama case is not whole: " + cell);
			}
			if (!(client.level.getBlockEntity(at) instanceof io.github.jimbozoomer.jugcraft.tower.RoomKitBlockEntity entity))
				throw new AssertionError("The diorama case's anchor has no block entity to draw the model");
			var renderer = (RoomKitDisplayRenderer) client.getBlockEntityRenderDispatcher().<io.github.jimbozoomer.jugcraft.tower.RoomKitBlockEntity,
					RoomKitDisplayRenderer.State>getRenderer(entity);
			var state = renderer.createRenderState();
			renderer.extractRenderState(entity, state, 0, client.player.position(), null);
			if (!state.diorama) throw new AssertionError("The diorama case does not draw the diorama");
			TowerDiorama diorama = TowerDiorama.get();
			if (diorama == null || diorama.count() < 20000) throw new AssertionError("The tower diorama did not load whole");
			float top = HoloTableBlock.HEIGHT / 16f;   // the model is made standing on a hologram table's top (the case lifts it to its tray)
			int shown = 0;
			for (int kind = 0; kind < 2; kind++) {
				for (int d = 0; d < TowerDiorama.DIRECTIONS; d++) {
					float[] q = diorama.quads[kind][d];
					for (int o = 0; o + TowerDiorama.CORNER <= q.length; o += TowerDiorama.CORNER) {
						float x = q[o] - 0.5f, y = q[o + 1], z = q[o + 2] - 0.5f;
						if (Math.abs(x) > 1.4f || Math.abs(z) > 1.4f || y < top || y > TowerDiorama.HEIGHT)
							throw new AssertionError("The diorama leaves the case: " + x + ", " + y + ", " + z);
					}
					int[] run = TowerDiorama.front(d, diorama.planes[kind][d], new double[] {state.camX, state.camX, state.camY, state.camY,
							state.camZ, state.camZ}[d]);
					shown += run[1] - run[0];
				}
			}
			if (shown <= 0 || shown >= diorama.count()) throw new AssertionError("A frame draws " + shown + " of the diorama's " + diorama.count() + " faces");
		});
	}

	/** What {@link #towerHologram} saw: the tower model, in place; the ticker's letters. */
	private static final int MODEL = 1, TICKER = 2;

	/**
	 * The central table (over the core): the tower hologram, with the projector hanging over it. Once its model is worked
	 * out, the model lies over the table, inside the atrium (the 1:12 control building is under 10 across) and under the projector.
	 */
	private static int towerHologram(net.minecraft.client.Minecraft client, BlockPos origin) {
		var core = (TowerCoreBlockEntity) client.level.getBlockEntity(origin);
		var table = formedTable(client, origin.offset(TriPillarArena.CENTRAL_TABLE), core);
		var renderer = new HoloMapRenderer();
		var state = renderer.createRenderState();
		renderer.extractRenderState(table, state, 0, client.player.position(), null);
		if (state.mode != HoloMapRenderer.MODE_TOWER) throw new AssertionError("The central table does not show the tower hologram: mode " + state.mode);
		if (state.parts.isEmpty()) throw new AssertionError("The central table has no projector over it");
		float projector = Float.MAX_VALUE;
		for (var part : state.parts) projector = Math.min(projector, part.y() + Math.min(part.y0(), part.y1()));
		int seen = 0;
		for (var label : state.labels) {
			if (label.text().length() != 1) throw new AssertionError("The tower hologram shows a label that is not a ticker letter: " + label.text());
			seen = TICKER;
		}
		if (state.model == null || state.model.length == 0) return seen;
		// As HoloMapRenderer.submit draws it: cell units, scaled 3:12 from the table's middle (shifted half a model block); it
		// turns about the middle, so every corner must stay inside the atrium whichever way it faces.
		double scale = HoloGeometry.TOWER_CELL / HoloMapRenderer.MODEL_SCALE, shift = 0.5 - 0.5 / HoloMapRenderer.MODEL_SCALE;
		double top = Double.NEGATIVE_INFINITY;
		for (int i = 0; i + 3 < state.model.length; i += 4) { // per corner: x, y, z, colour
			double x = shift + state.model[i] * scale, y = state.modelBase + state.model[i + 1] * scale, z = shift + state.model[i + 2] * scale;
			if (Math.hypot(x - 0.5, z - 0.5) > 7) throw new AssertionError("The turning tower model leaves the atrium round the table: " + x + ", " + z);
			if (y < HoloTableBlock.HEIGHT / 16f) throw new AssertionError("The tower model dips into the table: " + y);
			top = Math.max(top, y);
		}
		if (top >= projector || top >= 33) throw new AssertionError("The tower model reaches the projector: top " + top + ", projector " + projector);
		// over the Holo Command Table, the model floats clear of its glass field and touch stations (the top at 1)
		if (table.getBlockState().getBlock() instanceof io.github.jimbozoomer.jugcraft.tower.HoloCommandTableBlock && state.modelBase < 1.5f) {
			throw new AssertionError("The tower model sits too low over the Holo Command Table: " + state.modelBase);
		}
		// It turns slowly in place: half a tick later it has turned a little further.
		var later = renderer.createRenderState();
		renderer.extractRenderState(table, later, 0.5f, client.player.position(), null);
		double turned = Math.floorMod((long) Math.round((later.spin - state.spin) * 1e6), (long) Math.round(2 * Math.PI * 1e6)) / 1e6;
		if (turned <= 0 || turned > 0.01) throw new AssertionError("The tower model does not turn slowly: " + state.spin + " -> " + later.spin);
		return seen | MODEL;
	}

	/** The formed holo table anchored at {@code at}, pinned to {@code core}'s terminal. */
	private static DepotDisplayBlockEntity formedTable(net.minecraft.client.Minecraft client, BlockPos at, TowerCoreBlockEntity core) {
		if (!(client.level.getBlockEntity(at) instanceof DepotDisplayBlockEntity table) || !table.isAnchor()
				|| !(table.getBlockState().getBlock() instanceof HoloTableBlock
						|| table.getBlockState().getBlock() instanceof io.github.jimbozoomer.jugcraft.tower.HoloCommandTableBlock big
						&& big.complete(client.level, at))) throw new AssertionError("No formed holo table anchored at " + at);
		if (!core.terminalPos().equals(table.terminal())) throw new AssertionError("The holo table at " + at + " is not pinned to its tower's terminal");
		return table;
	}
	/** Temporary client-only fixtures verify preview failures without altering the server's tower or world. */
	private static void consoleHeadroomPreview(net.minecraft.client.Minecraft client, io.github.jimbozoomer.jugcraft.placement.StructureGhost.Source source) {
		var level = client.level;
		var block = (io.github.jimbozoomer.jugcraft.tower.TowerControlConsoleBlock) io.github.jimbozoomer.jugcraft.tower.JugcraftTower.TOWER_CONTROL_CONSOLE;
		var facing = client.player.getDirection().getOpposite();
		var saved = new java.util.LinkedHashMap<BlockPos, net.minecraft.world.level.block.state.BlockState>();
		java.util.function.BiConsumer<BlockPos, net.minecraft.world.level.block.state.BlockState> put = (at, state) -> {
			saved.putIfAbsent(at, level.getBlockState(at));
			level.setBlock(at, state, net.minecraft.world.level.block.Block.UPDATE_CLIENTS | net.minecraft.world.level.block.Block.UPDATE_KNOWN_SHAPE);
		};
		try {
			// a clear patch of ground out by the player: the console's cells and the headroom over them emptied
			BlockPos anchor = client.player.blockPosition().offset(0, 40, 0), floor = anchor.below();
			int cells = io.github.jimbozoomer.jugcraft.tower.TowerControlConsoleBlock.FOOTPRINT.size();
			for (int part = 0; part < cells; part++) {
				BlockPos at = io.github.jimbozoomer.jugcraft.tower.TowerControlConsoleBlock.partPos(anchor, facing, part);
				put.accept(at.below(), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
				put.accept(at, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
				put.accept(at.above(), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
			}
			var context = previewContext(client, block, floor);
			var clear = source.ghost(context);
			if (clear == null || !clear.fits() || block.getStateForPlacement(context) == null) throw new AssertionError("A clear console preview disagrees with real placement");
			// glass over the desk of the TOWER CONTROL position (the master cell is under its riser)
			BlockPos lowCeiling = anchor.above();
			put.accept(lowCeiling, net.minecraft.world.level.block.Blocks.GLASS.defaultBlockState());
			var blocked = source.ghost(context);
			if (blocked == null || blocked.fits() || !blocked.blocked().contains(lowCeiling) || block.getStateForPlacement(context) != null)
				throw new AssertionError("A low glass ceiling over the desk is not marked as blocked by both preview and real placement");
		} finally {
			for (var original : saved.entrySet()) level.setBlock(original.getKey(), original.getValue(),
					net.minecraft.world.level.block.Block.UPDATE_CLIENTS | net.minecraft.world.level.block.Block.UPDATE_KNOWN_SHAPE);
		}
	}
	private static net.minecraft.world.item.context.BlockPlaceContext previewContext(net.minecraft.client.Minecraft client,
			net.minecraft.world.level.block.Block block, BlockPos floor) {
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
