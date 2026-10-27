package io.github.jimbozoomer.jugcraft.test;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.client.EncyclopediaClient;
import io.github.jimbozoomer.jugcraft.client.EncyclopediaScreen;
import io.github.jimbozoomer.jugcraft.compat.jade.ConcordanceDataProvider;
import io.github.jimbozoomer.jugcraft.concordance.Alchemy;
import io.github.jimbozoomer.jugcraft.concordance.CircleAnchorBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceData;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import io.github.jimbozoomer.jugcraft.concordance.CrucibleBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.JugcraftConcordance;
import io.github.jimbozoomer.jugcraft.concordance.LeyPylonBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import io.github.jimbozoomer.jugcraft.concordance.alchemy.Formula;
import io.github.jimbozoomer.jugcraft.concordance.alchemy.Product;
import io.github.jimbozoomer.jugcraft.concordance.diagnostic.Code;
import io.github.jimbozoomer.jugcraft.concordance.diagnostic.Diagnostic;
import io.github.jimbozoomer.jugcraft.concordance.garden.Garden;
import io.github.jimbozoomer.jugcraft.concordance.garden.OrganismCropBlock;
import io.github.jimbozoomer.jugcraft.concordance.garden.VerdantBedBlock;
import io.github.jimbozoomer.jugcraft.concordance.garden.VerdantBedBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.ritual.Readiness;
import io.github.jimbozoomer.jugcraft.concordance.ritual.RitualMachine;
import io.github.jimbozoomer.jugcraft.concordance.ritual.StructurePattern;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import io.github.jimbozoomer.jugcraft.energy.CreativeEnergyCellBlock;
import io.github.jimbozoomer.jugcraft.fluid.ElectricPumpBlockEntity;
import io.github.jimbozoomer.jugcraft.fluid.JugcraftFluids;
import io.github.jimbozoomer.jugcraft.guide.encyclopedia.EncyclopediaPayload;
import io.github.jimbozoomer.jugcraft.machine.JugcraftMachines;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Concordance Chunk C, C-10: the living workshop in a real client, on the game's own dedicated server, at the ordinary
 * GUI scale. One client; one server tick per client tick (Fabric's client game tests). Results are logged with the prefix
 * "[workshop-check]".
 * <ul>
 * <li><b>The workshop in one view</b> ({@code jugcraft_concordance_workshop}): an Alembic Crucible following the Stillroot
 * Poultice's formula in two parts on a Crucible Stand fed by a Creative Energy Cell through a copper cable, an Electric
 * Pump on a walled spring piping its water, a Lesser Circle with its four Ley Pylons charged, and nine Verdant Beds of
 * Mendvetch with a Mulch Maw and a Gleaner, seen from above and behind with the HUD shown. The stand is lit, and the
 * crucible has drawn the water only the pump gave it. Then 100 frames' render times are logged: Mesa's software renderer's
 * on a CI runner, never a player's hardware (C-10's client frame time).</li>
 * <li><b>The stand's tell</b> (plan 08:176): the stand lit while it heats ({@code jugcraft_concordance_stand_lit}) and dark
 * once its crucible waits for Mendvetch ({@code jugcraft_concordance_stand_dark}; both the reason and the dark stand are
 * checked as it is shot), from the same place, at noon.</li>
 * <li><b>The crucible's Inspect page on its stand</b> ({@code jugcraft_encyclopedia_inspect_crucible_stand}): it names the
 * stand heating it, with no key or id.</li>
 * <li><b>Root and Rivet under way</b>, begun beside the workshop: the anchor's Inspect page shows what the rite has spent
 * apart from what it holds, and its stage and payment table ({@code jugcraft_encyclopedia_inspect_root_and_rivet}; at GUI
 * scale 4, checked as applied, in a 1920x1080 window, {@code jugcraft_encyclopedia_inspect_root_and_rivet_scale4}: in
 * the test's own 854x480 window the game allows no more than 2); with Jade loaded, Jade's snapshot is checked and
 * its overlay shot ({@code jugcraft_concordance_anchor_root_and_rivet_jade}). It completes: one Rooted Thallite
 * Foundation from exactly its offerings, 2 Ley Charge a step from each pylon, taken by its participant.</li>
 * <li><b>The batch</b>: four Mendvetch, the piped water and the stand's heat make two Stillroot Poultices in the crucible's
 * two bowls.</li>
 * </ul>
 * No worker is in the scene (the Peepo port and the couriers have their own tests). Without Jade the class runs the same
 * but for the Jade check and shot (plan 08:175). CI job {@code client}.
 */
public class ConcordanceWorkshopClientGameTests implements FabricClientGameTest {
	private static final int WAIT = 600;
	/** Frames sampled for C-10's client frame-time observation. */
	private static final int FRAMES = 100;
	/** The Stillroot Poultice in two parts (C-02): four Mendvetch, stirred warm four times; ten pulses after its water. */
	private static final String POULTICE = "water 2; add jugcraft:mendvetch jugcraft:raw; add jugcraft:mendvetch jugcraft:raw; "
			+ "add jugcraft:mendvetch jugcraft:raw; add jugcraft:mendvetch jugcraft:raw; stir warm; stir warm; stir warm; stir warm";
	private static final String ROOT_AND_RIVET = "jugcraft:root_and_rivet";
	private static final long PYLON_LEY = 64L;
	/** Root and Rivet's draw a step: 2 Ley Charge from each of the Lesser Circle's four pylons (ritual/root_and_rivet.json). */
	private static final int LEY_A_STEP = 8;

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestDedicatedServerContext server = context.worldBuilder().createServer()) {
			try (TestDedicatedServerConnection connection = server.connect()) {
				connection.waitForChunksRender();
				server.runCommand("time set noon");
				server.runCommand("weather clear");
				server.runCommand("gamerule minecraft:spawn_mobs false");
				server.runCommand("gamerule minecraft:random_tick_speed 0"); // a still garden: only this test changes it
				server.runCommand("gamemode survival @a");
				context.waitTicks(20);
				Workshop at = new Workshop(context.computeOnClient(client -> client.player.blockPosition()));
				build(context, server, at);
				standLit(context, server, at);
				crucibleOnItsStand(context, at);
				overview(context, server, connection, at);
				rootAndRivet(context, server, at);
				theBatch(context, server, at);
			}
		}
	}

	/**
	 * Where everything stands, from the player's first block: the circle ahead (south), the powered crucible to its left
	 * (east), the beds to its right (west). Nothing of the circle's (lesser_circle.json: reach 3) is within 3 blocks of a bed.
	 */
	private record Workshop(BlockPos origin) {
		BlockPos anchor() {
			return origin.offset(0, 0, 9);
		}

		BlockPos stand() {
			return origin.offset(7, 0, 11);
		}

		BlockPos pot() {
			return stand().above();
		}

		BlockPos cable() {
			return stand().west();
		}

		BlockPos cell() {
			return cable().west();
		}

		BlockPos spring() {
			return stand().east(2);
		}

		BlockPos pump() {
			return spring().above();
		}

		List<BlockPos> walls() {
			BlockPos spring = spring();
			return List.of(spring.west(), spring.east(), spring.north(), spring.south());
		}

		List<BlockPos> beds() {
			List<BlockPos> beds = new ArrayList<>();
			for (int dx = -9; dx <= -7; dx++) {
				for (int dz = 10; dz <= 12; dz++) {
					beds.add(origin.offset(dx, 0, dz));
				}
			}
			return beds;
		}

		BlockPos maw() {
			return origin.offset(-8, 0, 13);
		}

		BlockPos gleaner() {
			return origin.offset(-10, 0, 11);
		}

		/** The camera's place for the whole view, on a barrier above and behind the yard. */
		BlockPos lookout() {
			return origin.offset(0, 7, -2);
		}

		/** Where a reader stands for the crucible and its stand: 3 blocks off, within reach. */
		BlockPos reader() {
			return origin.offset(7, 0, 8);
		}

		/** Root and Rivet's leader: inside the ring on no part (as EncyclopediaClientGameTests.anchorReadiness), within reach. */
		BlockPos leader() {
			return anchor().offset(-1, 0, -2);
		}
	}

	private static void build(ClientGameTestContext context, TestDedicatedServerContext server, Workshop at) {
		String built = server.computeOnServer(minecraft -> {
			ServerLevel level = minecraft.overworld();
			ServerPlayer player = first(minecraft);
			ConcordanceProgress.grant(player, "jugcraft:first_light", ResearchState.UNDERSTOOD);
			ConcordanceProgress.grant(player, "jugcraft:circle_lore", ResearchState.UNDERSTOOD);
			// The powered crucible as C-05's workshop builds it: the cell and cable west of the stand, the pump's spring walled east of it.
			at.walls().forEach(wall -> level.setBlockAndUpdate(wall, Blocks.STONE.defaultBlockState()));
			level.setBlockAndUpdate(at.spring(), Blocks.WATER.defaultBlockState());
			level.setBlockAndUpdate(at.cell(), CreativeEnergyCellBlock.BLOCK.defaultBlockState());
			level.setBlockAndUpdate(at.cable(), JugcraftMachines.COPPER_CABLE.defaultBlockState());
			level.setBlockAndUpdate(at.stand(), JugcraftConcordance.CRUCIBLE_STAND.defaultBlockState());
			level.setBlockAndUpdate(at.pot(), JugcraftConcordance.CRUCIBLE.defaultBlockState());
			level.setBlockAndUpdate(at.pump(), JugcraftFluids.ELECTRIC_PUMP.defaultBlockState());
			level.setBlockAndUpdate(at.pump().west(), JugcraftFluids.BRONZE_FLUID_PIPE.defaultBlockState());
			if (!(level.getBlockEntity(at.pump()) instanceof ElectricPumpBlockEntity pumping)
					|| !(level.getBlockEntity(at.pot()) instanceof CrucibleBlockEntity crucible)) {
				return "no pump or crucible";
			}
			pumping.energy().setAmount(ElectricPumpBlockEntity.ENERGY_CAPACITY);
			crucible.follow(Formula.parse(POULTICE)); // as the formula item sets it, without its checks; no water is poured: the pump gives it
			crucible.setItem(0, new ItemStack(Garden.MENDVETCH, 4));
			crucible.setItem(CrucibleBlockEntity.BOTTLE_SLOT, new ItemStack(Items.BOWL, 2));
			// The Lesser Circle, its pylons charged for Root and Rivet's whole run and more.
			level.setBlockAndUpdate(at.anchor(), JugcraftConcordance.CIRCLE_ANCHOR.defaultBlockState());
			for (StructurePattern.Part part : ConcordanceData.rules().structure("jugcraft:lesser_circle").parts()) {
				BlockPos where = at.anchor().offset(part.offset().x(), part.offset().y(), part.offset().z());
				if (part.role() == StructurePattern.Role.CHANNEL) {
					level.setBlockAndUpdate(where, JugcraftConcordance.LEY_PYLON.defaultBlockState());
					((LeyPylonBlockEntity) level.getBlockEntity(where)).setLey(level, PYLON_LEY);
				} else if (part.role() == StructurePattern.Role.BOUNDARY) {
					level.setBlockAndUpdate(where, JugcraftConcordance.WARDING_STONE.defaultBlockState());
				}
			}
			// Nine Verdant Beds of Mendvetch at steps 1 to 3, wet and fed, a Mulch Maw and a Gleaner beside them.
			List<BlockPos> beds = at.beds();
			for (int i = 0; i < beds.size(); i++) {
				BlockPos bed = beds.get(i);
				level.setBlockAndUpdate(bed, Garden.VERDANT_BED.defaultBlockState().setValue(VerdantBedBlock.MOISTURE, VerdantBedBlock.WET));
				BlockState crop = ((OrganismCropBlock) Garden.MENDVETCH_CROP).getStateForAge(1 + i % OrganismCropBlock.MAX_AGE);
				level.setBlockAndUpdate(bed.above(), crop);
				if (level.getBlockEntity(bed) instanceof VerdantBedBlockEntity soil) {
					soil.awaken(player.getUUID());
					soil.setNutrients(10);
				}
			}
			level.setBlockAndUpdate(at.maw(), Garden.MULCH_MAW.defaultBlockState());
			level.setBlockAndUpdate(at.gleaner(), Garden.GLEANER.defaultBlockState());
			return crucible.program() != null ? "ok" : "no formula";
		});
		check(built.equals("ok"), "The workshop should be built: " + built);
		// The crucible draws its two parts only once the pump has piped them; from then on it works through ten more pulses.
		BlockPos stand = at.stand();
		BlockPos pot = at.pot();
		for (int wait = 0; wait < WAIT && !server.computeOnServer(minecraft -> lit(minecraft.overworld(), stand)
				&& minecraft.overworld().getBlockEntity(pot) instanceof CrucibleBlockEntity crucible && crucible.step() >= 1); wait++) {
			context.waitTick();
		}
		String state = server.computeOnServer(minecraft -> describe(minecraft.overworld(), at));
		Jugcraft.LOGGER.info("[workshop-check] built: {}", state);
		check(server.computeOnServer(minecraft -> lit(minecraft.overworld(), stand)
				&& minecraft.overworld().getBlockEntity(pot) instanceof CrucibleBlockEntity crucible && crucible.step() >= 1),
				"The stand should be lit, heating its crucible, and the crucible should have drawn the water the pump piped: " + state);
	}

	/** The stand close up while it heats (08:176's tell); read again at the end, dark. */
	private static void standLit(ClientGameTestContext context, TestDedicatedServerContext server, Workshop at) {
		standAt(context, server, at);
		boolean lit = server.computeOnServer(minecraft -> lit(minecraft.overworld(), at.stand()));
		Jugcraft.LOGGER.info("[workshop-check] the stand close up: lit {}", lit);
		check(lit, "The stand should still be lit while its crucible works through its formula");
		context.takeScreenshot("jugcraft_concordance_stand_lit");
	}

	private static void crucibleOnItsStand(ClientGameTestContext context, Workshop at) {
		context.getInput().lookAt(at.pot());
		context.waitTicks(5);
		open(context);
		String page = context.computeOnClient(client -> ((EncyclopediaScreen) client.gui.screen()).page());
		List<String> rows = rows(context);
		String standName = context.computeOnClient(client -> I18n.get("block.jugcraft.crucible_stand"));
		Jugcraft.LOGGER.info("[workshop-check] the crucible on its stand: page {}, rows {}", page, rows);
		check(page.equals("inspect") && rows.stream().anyMatch(row -> row.contains(standName)) && unkeyed(rows),
				"The crucible's Inspect page should name the Crucible Stand heating it, with no key or id: " + rows);
		clearToasts(context);
		context.takeScreenshot("jugcraft_encyclopedia_inspect_crucible_stand");
		close(context);
	}

	/** The whole workshop from above and behind, at the ordinary GUI scale with the HUD shown; then its frame times. */
	private static void overview(ClientGameTestContext context, TestDedicatedServerContext server, TestDedicatedServerConnection connection,
			Workshop at) {
		BlockPos lookout = at.lookout();
		server.runOnServer(minecraft -> minecraft.overworld().setBlockAndUpdate(lookout.below(), Blocks.BARRIER.defaultBlockState()));
		server.runCommand(String.format(Locale.ROOT, "tp @a %.1f %d %.1f 0 35", lookout.getX() + 0.5, lookout.getY(), lookout.getZ() + 0.5));
		context.waitTicks(40);
		connection.waitForChunksRender();
		clearToasts(context);
		String scale = context.computeOnClient(client -> "GUI scale " + client.getWindow().getGuiScale() + " (option "
				+ client.options.guiScale().get() + "), window " + client.getWindow().getWidth() + "x" + client.getWindow().getHeight());
		String state = server.computeOnServer(minecraft -> describe(minecraft.overworld(), at));
		Jugcraft.LOGGER.info("[workshop-check] the whole workshop, {}: {}", scale, state);
		check(server.computeOnServer(minecraft -> lit(minecraft.overworld(), at.stand())), "The stand should be lit in the whole view: " + state);
		context.takeScreenshot("jugcraft_concordance_workshop");
		Jugcraft.LOGGER.info("[workshop-check] frame time with the whole workshop in view, {}: {}", scale, frameTimes(context));
	}

	/**
	 * C-10's client frame time: how long each of the next FRAMES frames took the client to render (Minecraft.getFrameTimeNs:
	 * the level's and the GUI's update and the render, to the blit), with the whole workshop in view. One frame a tick here
	 * (Fabric's client game tests); in CI, Mesa's software renderer: that renderer's figures, never a player's hardware.
	 */
	private static String frameTimes(ClientGameTestContext context) {
		long[] frames = new long[FRAMES];
		for (int i = 0; i < FRAMES; i++) {
			context.waitTick();
			frames[i] = context.computeOnClient(client -> client.getFrameTimeNs());
		}
		Arrays.sort(frames);
		return String.format(Locale.ROOT, "median %.2f ms, p95 %.2f ms, max %.2f ms over %d frames (LIBGL_ALWAYS_SOFTWARE=%s)",
				frames[FRAMES / 2] / 1.0e6, frames[(int) Math.ceil(0.95 * FRAMES) - 1] / 1.0e6, frames[FRAMES - 1] / 1.0e6, FRAMES,
				System.getenv("LIBGL_ALWAYS_SOFTWARE"));
	}

	private static void rootAndRivet(ClientGameTestContext context, TestDedicatedServerContext server, Workshop at) {
		BlockPos anchorAt = at.anchor();
		BlockPos leader = at.leader();
		server.runCommand(String.format(Locale.ROOT, "tp @a %.1f %d %.1f 0 0", leader.getX() + 0.5, leader.getY(), leader.getZ() + 0.5));
		context.waitTicks(10);
		String started = server.computeOnServer(minecraft -> {
			ServerLevel level = minecraft.overworld();
			ServerPlayer player = first(minecraft);
			ConcordanceProgress.setFocus(player, 20);
			CircleAnchorBlockEntity anchor = (CircleAnchorBlockEntity) level.getBlockEntity(anchorAt);
			for (ItemStack stack : List.of(new ItemStack(thallitePlate()), new ItemStack(Items.COPPER_INGOT, 4), new ItemStack(Garden.MENDVETCH, 4))) {
				anchor.offer(player, stack);
			}
			RateGate.forget(player.getUUID());
			anchor.use(player, level, false);
			return anchor.phase().id + " " + anchor.run().ritual() + ", Focus " + ConcordanceProgress.currentFocus(player);
		});
		Jugcraft.LOGGER.info("[workshop-check] Root and Rivet begun beside the workshop: {}", started);
		check(started.equals(RitualMachine.Phase.CHANNELING.id + " " + ROOT_AND_RIVET + ", Focus 14"),
				"Root and Rivet should channel, its six Focus paid: " + started);

		context.getInput().lookAt(anchorAt);
		context.waitTicks(5);
		open(context);
		String page = context.computeOnClient(client -> ((EncyclopediaScreen) client.gui.screen()).page());
		List<String> rows = rows(context);
		boolean shown = context.computeOnClient(client -> {
			EncyclopediaPayload.Page current = EncyclopediaClient.navigator().current();
			List<EncyclopediaPayload.Row> found = current == null ? List.of() : current.rows();
			return found.stream().anyMatch(row -> holds(row.text(), "encyclopedia.jugcraft.inspect.under_way"))
					&& found.stream().anyMatch(row -> holds(row.text(), "compose.jugcraft.ritual.spent"))
					&& found.stream().anyMatch(row -> holds(row.text(), "encyclopedia.jugcraft.inspect.anchor.held"))
					&& found.stream().anyMatch(row -> holds(row.text(), "compose.jugcraft.ritual.stage.backlash"));
		});
		String phase = server.computeOnServer(minecraft -> ((CircleAnchorBlockEntity) minecraft.overworld().getBlockEntity(anchorAt)).phase().id);
		Jugcraft.LOGGER.info("[workshop-check] Root and Rivet under way ({}): page {}, rows {}", phase, page, rows);
		check(page.equals("inspect") && shown && unkeyed(rows) && phase.equals(RitualMachine.Phase.CHANNELING.id),
				"While Root and Rivet channels, the Inspect page should show what it has spent apart from what it holds, and its stage and "
						+ "payment table, in words: " + rows);
		clearToasts(context);
		context.takeScreenshot("jugcraft_encyclopedia_inspect_root_and_rivet");
		// A high GUI scale needs a larger window: Fabric's client game tests hold it at 854x480, where Window.calculateScale
		// stops at 2 (a scale n needs width / n >= 320 and height / n >= 240), so scale 4 needs at least 1280x960.
		int scale = context.computeOnClient(client -> client.options.guiScale().get());
		int width = context.computeOnClient(client -> client.getWindow().getWidth());
		int height = context.computeOnClient(client -> client.getWindow().getHeight());
		context.getInput().resizeWindow(1920, 1080);
		context.runOnClient(client -> {
			client.options.guiScale().set(4);
			client.resizeGui();
			client.gui.toastManager().clear();
		});
		context.waitTicks(10);
		int applied = context.computeOnClient(client -> client.getWindow().getGuiScale());
		Jugcraft.LOGGER.info("[workshop-check] Root and Rivet's Inspect page at GUI scale {} in a 1920x1080 window", applied);
		check(applied == 4, "The high-scale shot should be at GUI scale 4: " + applied);
		context.takeScreenshot("jugcraft_encyclopedia_inspect_root_and_rivet_scale4");
		context.runOnClient(client -> {
			client.options.guiScale().set(scale);
			client.resizeGui();
		});
		context.getInput().resizeWindow(width, height);
		context.waitTicks(5);
		close(context);

		if (FabricLoader.getInstance().isModLoaded("jade")) {
			String jade = server.computeOnServer(minecraft -> {
				CompoundTag shownTag = ConcordanceDataProvider.snapshot((CircleAnchorBlockEntity) minecraft.overworld().getBlockEntity(anchorAt),
						minecraft.overworld());
				int done = shownTag.getIntOr("done", -1);
				boolean right = shownTag.getStringOr("phase", "").equals(RitualMachine.Phase.CHANNELING.id) && shownTag.getIntOr("needed", -1) == 1
						&& shownTag.getIntOr("joined", -1) == 1 && shownTag.getIntOr("spent_focus", -1) == 6 && shownTag.getIntOr("reserved", -1) == 9
						&& done >= 0 && shownTag.getIntOr("spent_ley", -1) == LEY_A_STEP * done;
				return (right ? "ok " : "bad ") + shownTag;
			});
			Jugcraft.LOGGER.info("[workshop-check] Root and Rivet in Jade: {}", jade);
			check(jade.startsWith("ok"), "Jade's snapshot of Root and Rivet under way: " + jade);
			context.getInput().lookAt(anchorAt);
			context.waitTicks(20);
			clearToasts(context);
			context.takeScreenshot("jugcraft_concordance_anchor_root_and_rivet_jade");
		} else {
			Jugcraft.LOGGER.info("[workshop-check] Jade is not loaded: no Jade check or shot of Root and Rivet");
		}

		for (int wait = 0; wait < WAIT && server.computeOnServer(minecraft -> ((CircleAnchorBlockEntity) minecraft.overworld().getBlockEntity(anchorAt))
				.phase() != RitualMachine.Phase.COMPLETE); wait++) {
			context.waitTick();
		}
		String done = server.computeOnServer(minecraft -> {
			ServerLevel level = minecraft.overworld();
			ServerPlayer player = first(minecraft);
			CircleAnchorBlockEntity anchor = (CircleAnchorBlockEntity) level.getBlockEntity(anchorAt);
			List<String> faults = new ArrayList<>();
			if (anchor.phase() != RitualMachine.Phase.COMPLETE || !anchor.output().is(JugcraftConcordance.ROOTED_THALLITE_FOUNDATION)
					|| anchor.output().getCount() != 1) {
				faults.add("made " + anchor.output() + " in " + anchor.phase().id);
			}
			if (!anchor.offerings().stream().allMatch(ItemStack::isEmpty)) {
				faults.add("left " + anchor.offerings());
			}
			for (StructurePattern.Part part : ConcordanceData.rules().structure("jugcraft:lesser_circle").channels()) {
				long ley = ((LeyPylonBlockEntity) level.getBlockEntity(anchorAt.offset(part.offset().x(), part.offset().y(), part.offset().z()))).ley();
				if (ley != PYLON_LEY - 2 * 4) {
					faults.add("a pylon holds " + ley);
				}
			}
			if (!new Readiness.Spent(6, 32, 4, 0, 0).equals(anchor.spent())) {
				faults.add("spent " + anchor.spent());
			}
			RateGate.forget(player.getUUID());
			anchor.use(player, level, false); // the participant takes it
			int taken = player.getInventory().countItem(JugcraftConcordance.ROOTED_THALLITE_FOUNDATION);
			if (taken != 1 || !anchor.output().isEmpty()) {
				faults.add("taken " + taken + ", left " + anchor.output());
			}
			return faults.isEmpty() ? "ok" : String.join("; ", faults);
		});
		Jugcraft.LOGGER.info("[workshop-check] Root and Rivet completed beside the stand and pump: {}", done);
		check(done.equals("ok"), "Root and Rivet should make one foundation from exactly its offerings, 2 Ley Charge a step from each pylon: " + done);
	}

	/**
	 * Four Mendvetch and the piped water make two Stillroot Poultices; the stand goes dark while the repeat waits for more.
	 * The wait is for the reason itself (MISSING_INGREDIENT), not only a dark stand: between the batch and that wait the
	 * repeat's first steps may light the stand again.
	 */
	private static void theBatch(ClientGameTestContext context, TestDedicatedServerContext server, Workshop at) {
		BlockPos pot = at.pot();
		BlockPos stand = at.stand();
		for (int wait = 0; wait < WAIT && !server.computeOnServer(minecraft -> minecraft.overworld().getBlockEntity(pot) instanceof CrucibleBlockEntity crucible
				&& crucible.getItem(CrucibleBlockEntity.OUTPUT_SLOT).getCount() == 2 && waitsForMendvetch(crucible) && !lit(minecraft.overworld(), stand)); wait++) {
			context.waitTick();
		}
		String made = server.computeOnServer(minecraft -> {
			ServerLevel level = minecraft.overworld();
			if (!(level.getBlockEntity(pot) instanceof CrucibleBlockEntity crucible)) {
				return "bad: no crucible";
			}
			ItemStack output = crucible.getItem(CrucibleBlockEntity.OUTPUT_SLOT);
			Product product = Alchemy.product(output);
			Diagnostic reason = crucible.waiting();
			boolean right = output.getCount() == 2 && product != null && product.id().equals(Alchemy.STILLROOT_POULTICE)
					&& crucible.getItem(0).isEmpty() && crucible.getItem(CrucibleBlockEntity.BOTTLE_SLOT).isEmpty() && waitsForMendvetch(crucible)
					&& !lit(level, stand);
			return (right ? "ok " : "bad ") + output + " (" + (product == null ? "no product" : product.id()) + "), buffer " + crucible.getItem(0)
					+ ", bowls " + crucible.getItem(CrucibleBlockEntity.BOTTLE_SLOT) + ", stand lit " + lit(level, stand) + ", waiting "
					+ (reason == null ? "for nothing" : reason.encode());
		});
		Jugcraft.LOGGER.info("[workshop-check] the powered crucible's batch: {}", made);
		check(made.startsWith("ok"), "Four Mendvetch, the piped water and the stand's heat should make two Stillroot Poultices, "
				+ "and the stand go dark while the repeat waits for Mendvetch: " + made);
		standAt(context, server, at);
		String still = server.computeOnServer(minecraft -> describe(minecraft.overworld(), at));
		Jugcraft.LOGGER.info("[workshop-check] the stand close up, the repeat waiting: {}", still);
		check(server.computeOnServer(minecraft -> !lit(minecraft.overworld(), stand)
				&& minecraft.overworld().getBlockEntity(pot) instanceof CrucibleBlockEntity crucible && waitsForMendvetch(crucible)),
				"The stand should still be dark, its crucible waiting for Mendvetch, when it is shot: " + still);
		context.takeScreenshot("jugcraft_concordance_stand_dark");
	}

	/** Whether the crucible's repeat waits for an ingredient (its Mendvetch). Server only. */
	private static boolean waitsForMendvetch(CrucibleBlockEntity crucible) {
		Diagnostic reason = crucible.waiting();
		return reason != null && reason.code() == Code.MISSING_INGREDIENT;
	}

	/** Stands the player at the reader's place looking at the stand, its toasts cleared. */
	private static void standAt(ClientGameTestContext context, TestDedicatedServerContext server, Workshop at) {
		BlockPos reader = at.reader();
		server.runCommand(String.format(Locale.ROOT, "tp @a %.1f %d %.1f 0 20", reader.getX() + 0.5, reader.getY(), reader.getZ() + 0.5));
		context.waitTicks(10);
		context.getInput().lookAt(at.stand());
		context.waitTicks(10);
		clearToasts(context);
	}

	/** One line of the workshop's state for the log. Server only. */
	private static String describe(ServerLevel level, Workshop at) {
		StringBuilder out = new StringBuilder("stand lit ").append(lit(level, at.stand()));
		if (level.getBlockEntity(at.pot()) instanceof CrucibleBlockEntity crucible) {
			Diagnostic reason = crucible.waiting();
			out.append("; crucible ").append(crucible.temperature()).append(" degrees, step ").append(crucible.step()).append(", tank ")
					.append(crucible.tankParts()).append(" parts, waiting ").append(reason == null ? "for nothing" : reason.encode());
		}
		if (level.getBlockEntity(at.anchor()) instanceof CircleAnchorBlockEntity anchor) {
			out.append("; anchor ").append(anchor.phase().id);
		}
		int awake = 0;
		for (BlockPos bed : at.beds()) {
			if (level.getBlockEntity(bed) instanceof VerdantBedBlockEntity soil && soil.awake()) {
				awake++;
			}
		}
		return out.append("; beds awake ").append(awake).append(" of ").append(at.beds().size()).toString();
	}

	private static boolean lit(ServerLevel level, BlockPos stand) {
		BlockState state = level.getBlockState(stand);
		return state.hasProperty(BlockStateProperties.LIT) && state.getValue(BlockStateProperties.LIT);
	}

	/** The Thallite plate (registered by the materials, so it has no Java constant). */
	private static Item thallitePlate() {
		return BuiltInRegistries.ITEM.getValue(Identifier.parse("jugcraft:thallite_plate"));
	}

	/** Opens the encyclopedia with its key (looking at a block, it turns to the Inspect page) and waits for this opening's answer. */
	private static void open(ClientGameTestContext context) {
		context.getInput().pressKey(EncyclopediaClient.key());
		context.waitForScreen(EncyclopediaScreen.class);
		int asked = context.computeOnClient(client -> EncyclopediaClient.sequence());
		context.waitFor(client -> EncyclopediaClient.navigator().applied() >= asked, WAIT);
		context.waitTicks(15);
	}

	private static void close(ClientGameTestContext context) {
		context.getInput().pressKey(InputConstants.KEY_ESCAPE);
		context.waitForScreen(null);
	}

	private static List<String> rows(ClientGameTestContext context) {
		return context.computeOnClient(client -> {
			EncyclopediaPayload.Page current = EncyclopediaClient.navigator().current();
			return current == null ? List.<String>of() : current.rows().stream().map(row -> row.text().getString()).toList();
		});
	}

	/** No translation key or Jugcraft id shows. */
	private static boolean unkeyed(List<String> rows) {
		return rows.stream().noneMatch(row -> row.contains("jugcraft.") || row.contains("jugcraft:"));
	}

	private static void clearToasts(ClientGameTestContext context) {
		context.runOnClient(client -> client.gui.toastManager().clear());
		context.waitTicks(5);
	}

	/** Whether the translation key {@code key} is in {@code component}, its arguments or its siblings. */
	private static boolean holds(Component component, String key) {
		if (component.getContents() instanceof TranslatableContents translatable) {
			if (translatable.getKey().equals(key)) {
				return true;
			}
			for (Object arg : translatable.getArgs()) {
				if (arg instanceof Component inner && holds(inner, key)) {
					return true;
				}
			}
		}
		return component.getSiblings().stream().anyMatch(sibling -> holds(sibling, key));
	}

	private static ServerPlayer first(MinecraftServer minecraft) {
		return minecraft.getPlayerList().getPlayers().get(0);
	}

	private static void check(boolean condition, String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}
}
