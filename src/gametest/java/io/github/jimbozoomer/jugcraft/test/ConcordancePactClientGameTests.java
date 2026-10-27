package io.github.jimbozoomer.jugcraft.test;

import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.state.AnimationPoint;
import com.geckolib.renderer.GeoEntityRenderer;
import com.mojang.blaze3d.platform.InputConstants;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.client.PactClient;
import io.github.jimbozoomer.jugcraft.client.PactScreen;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import io.github.jimbozoomer.jugcraft.concordance.JugcraftConcordance;
import io.github.jimbozoomer.jugcraft.concordance.resource.BoundWill;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import io.github.jimbozoomer.jugcraft.concordance.sky.AstralClaims;
import io.github.jimbozoomer.jugcraft.concordance.sky.AstrolabeItem;
import io.github.jimbozoomer.jugcraft.concordance.sky.Sky;
import io.github.jimbozoomer.jugcraft.concordance.spirits.AgreementHolder;
import io.github.jimbozoomer.jugcraft.concordance.spirits.BoundWills;
import io.github.jimbozoomer.jugcraft.concordance.spirits.PactKeeping;
import io.github.jimbozoomer.jugcraft.concordance.spirits.PactPerchBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.spirits.PactReading;
import io.github.jimbozoomer.jugcraft.concordance.spirits.PactRequestPayload;
import io.github.jimbozoomer.jugcraft.concordance.spirits.PactStatePayload;
import io.github.jimbozoomer.jugcraft.concordance.spirits.StolasEntity;
import io.github.jimbozoomer.jugcraft.concordance.worker.Pacts;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundEventListener;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Concordance Chunk D, D-04 and D-05 (unit 2) and D-06 (unit 3): the pact perch's screen in a real client, on the game's
 * own dedicated server, and Stolas's body. One client. Results are logged with the prefix "[pact-check]". The player is in
 * survival, so what is spent is spent.
 * <ul>
 * <li><b>The conversation</b>: with Stolas's Regard understood and two patterns observed, the owner's perch is used; Stolas
 * arrives and speaks his three beats, then his terms, reached by Enter alone, the keyboard on their first line, never on
 * Accept ({@code jugcraft_pact_conversation}, {@code jugcraft_pact_terms}). Every term is there; nothing is recorded.</li>
 * <li><b>Sizes</b>: the terms and a beat at GUI scale 4 in a 1920x1080 window and in a 640x360 window; every control on
 * screen, every reachable control with words for the narrator.</li>
 * <li><b>Acceptance by keyboard</b>: Accept without the compact's materials is refused as cannot_afford and spends nothing
 * ({@code jugcraft_pact_refused}); given them, Accept seals once and spends once ({@code jugcraft_pact_kept}).</li>
 * <li><b>His body</b>: the screen closed and the pact kept; Stolas on his perch drawn by GeckoLib ({@code jugcraft_pact_stolas}).</li>
 * <li>(D-06) <b>His body</b>: each loop as the server's state drives it and each gesture as the server fires it, frozen inside its telling pose ({@code jugcraft_stolas_*}); his real arrival and departure play their gestures. (D-09) A timed sequence through his refusal ({@code jugcraft_stolas_refuse_seq_1} to {@code _4}); every body shot is taken from his front, south of his perch (D-P133).</li>
 * <li>(D-06) <b>His cues</b>: his refusal and assent are fired by the server with their subtitles (seen only on a machine
 * with a sound device), each answer marked with a tick or a cross; Jade's readings of him and his perch, and their overlays
 * with Jade loaded ({@code jugcraft_pact_perch_jade}, {@code jugcraft_pact_stolas_jade}).</li>
 * <li>(D-05) <b>His service</b>: the Lantern Star recalled at its cost under the shared recall claim; an amethyst shard paid
 * ahead ({@code jugcraft_pact_service}).</li>
 * <li>(D-05) <b>The end</b>: End asks first ({@code jugcraft_pact_end_confirm}), the keyboard on Keep it (D-06); Esc keeps
 * the pact; yes ends it once, the offering back in the holder's inventory, Stolas gone ({@code jugcraft_pact_ended}).</li>
 * </ul>
 * The perch is opened by the server's use of it (no aimed click), and each step that opens it waits for a menu newer than
 * the one open before, with its first state. The narrator is not heard, and one client is not two.
 */
public class ConcordancePactClientGameTests implements FabricClientGameTest {
	private static final int WAIT = 200;
	private static final String STOLAS = "jugcraft:stolas";
	private static final String PERCH = "jugcraft:pact_perch";
	private static final String REGARD = "jugcraft:stolas_regard";
	private static final String BINDING_ARTS = "jugcraft:binding_arts";
	private static final String FIRST_LIGHT = "jugcraft:first_light";
	private static final String FULL_MOON = "jugcraft:full_moon";
	private static final String LANTERN_STAR = "jugcraft:lantern_star";
	// D-06: the worker's own controller and his loops' clips (tools/concordance_pacts.py LOOPS; check_stolas_body).
	private static final String MAIN = "main";
	private static final String IDLE = "animation.stolas.idle";
	private static final String LISTEN = "animation.stolas.listen";
	private static final String WAITING = "animation.stolas.waiting";
	private static final String SUSPENDED = "animation.stolas.suspended";
	// D-09 (D-P134): the times (seconds into his refusal) its timed sequence is frozen at, and its shots
	// (tools/concordance_pacts.py REFUSAL_SEQUENCE, SEQUENCE_SHOTS).
	private static final double[] REFUSAL_TIMES = {0.25, 0.65, 1.05, 1.45};
	private static final String[] REFUSAL_SHOTS = {"jugcraft_stolas_refuse_seq_1", "jugcraft_stolas_refuse_seq_2", "jugcraft_stolas_refuse_seq_3",
			"jugcraft_stolas_refuse_seq_4"};

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestDedicatedServerContext server = context.worldBuilder().createServer()) {
			try (TestDedicatedServerConnection connection = server.connect()) {
				connection.waitForChunksRender();
				server.runCommand("time set noon");
				server.runCommand("weather clear");
				server.runCommand("gamerule minecraft:spawn_mobs false");
				server.runCommand("gamemode survival @a");
				context.waitTicks(20);
				BlockPos stand = context.computeOnClient(client -> client.player.blockPosition());
				BlockPos perch = stand.south(2);
				server.runOnServer(minecraft -> prepare(minecraft, stand, perch));
				server.runCommand(String.format(Locale.ROOT, "tp @a %.1f %d %.1f 0 0", stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5));
				context.waitTicks(10);
				connection.waitForChunksRender();
				int scale = context.computeOnClient(client -> client.options.guiScale().get());
				context.runOnClient(client -> {
					client.options.guiScale().set(2);
					client.resizeGui();
				});
				conversation(context, server, perch);
				sizes(context);
				Hearing hearing = hear(context); // D-06
				acceptance(context, server);
				heard(context, server, hearing, perch); // D-06
				body(context, server, connection, perch);
				poses(context, server, perch); // D-06
				jade(context, server, perch); // D-06
				service(context, server, perch); // D-05
				ending(context, server, perch); // D-05
				context.runOnClient(client -> {
					client.options.guiScale().set(scale);
					client.resizeGui();
				});
			}
		}
	}

	/** Stolas's Regard understood, two patterns observed, a full astrolabe in hand, the owner's perch two blocks south. */
	private static void prepare(MinecraftServer minecraft, BlockPos stand, BlockPos perch) {
		check(BuiltInRegistries.BLOCK.containsKey(Identifier.parse(PERCH)) && BuiltInRegistries.ENTITY_TYPE.containsKey(Identifier.parse(STOLAS)),
				"The pact perch and Stolas are registered");
		ServerPlayer player = player(minecraft);
		ServerLevel level = minecraft.overworld();
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -1; dz <= 6; dz++) {
				level.setBlockAndUpdate(stand.offset(dx, -1, dz), Blocks.STONE.defaultBlockState());
				for (int dy = 0; dy <= 3; dy++) {
					level.setBlockAndUpdate(stand.offset(dx, dy, dz), Blocks.AIR.defaultBlockState());
				}
			}
		}
		level.setBlockAndUpdate(perch, BuiltInRegistries.BLOCK.getValue(Identifier.parse(PERCH)).defaultBlockState());
		check(level.getBlockEntity(perch) instanceof AgreementHolder.Holds, "The perch keeps its pact through the shared holder (D-02)");
		((AgreementHolder.Holds) level.getBlockEntity(perch)).holder().setOwner(player.getUUID());
		ConcordanceProgress.grant(player, FIRST_LIGHT, ResearchState.UNDERSTOOD);
		ConcordanceProgress.grant(player, BINDING_ARTS, ResearchState.UNDERSTOOD);
		ConcordanceProgress.grant(player, REGARD, ResearchState.UNDERSTOOD);
		AstralClaims claims = AstralClaims.of(minecraft);
		claims.observe(player.getUUID(), FULL_MOON, level.getGameTime());
		claims.observe(player.getUUID(), LANTERN_STAR, level.getGameTime());
		player.getInventory().clearContent();
		ItemStack astrolabe = new ItemStack(Sky.ASTROLABE);
		AstrolabeItem.setCharge(astrolabe, AstrolabeItem.CAPACITY);
		player.getInventory().setItem(0, astrolabe);
	}

	private static void conversation(ClientGameTestContext context, TestDedicatedServerContext server, BlockPos perch) {
		open(context, server, perch);
		context.waitFor(client -> PactClient.state() != null && PactClient.state().mode() == PactStatePayload.NEGOTIATE
				&& stolas(client, perch) != null, WAIT);
		check(StolasEntity.Gesture.ARRIVE.animation.equals(context.computeOnClient(client -> gestured(client, perch))),
				"Invited, he arrives at the perch with his gesture (D-06)");
		String shown = context.computeOnClient(client -> describe(PactClient.state()));
		Jugcraft.LOGGER.info("[pact-check] invited: {}", shown);
		check(context.computeOnClient(client -> PactClient.state().beats().size() == 3 && PactClient.state().notes().isEmpty()
				&& PactClient.state().offers(PactRequestPayload.ACCEPT)
				&& PactClient.state().terms().stream().map(PactStatePayload.Term::key).toList().equals(PactStatePayload.TERMS)),
				"Stolas speaks three beats, nothing stands in the way, and his terms name every term, in order: " + shown);
		fits(context, "a beat at GUI scale 2");
		named(context);
		shot(context, "jugcraft_pact_conversation");
		tabTo(context, "next");
		for (int beat = 0; beat < 3; beat++) {
			context.getInput().pressKey(InputConstants.KEY_RETURN);
			context.waitTicks(3);
		}
		check(context.computeOnClient(client -> screen(client).page() == PactScreen.TERMS_PAGE && "para:0".equals(screen(client).focusedKey())),
				"Enter three times reaches his terms, the keyboard on their first line, never on Accept");
		fits(context, "the terms at GUI scale 2");
		named(context);
		shot(context, "jugcraft_pact_terms");
		check(!server.computeOnServer(ConcordancePactClientGameTests::held), "Hearing his terms records nothing");
	}

	private static void sizes(ClientGameTestContext context) {
		int width = context.computeOnClient(client -> client.getWindow().getWidth());
		int height = context.computeOnClient(client -> client.getWindow().getHeight());
		context.getInput().resizeWindow(1920, 1080);
		context.runOnClient(client -> {
			client.options.guiScale().set(4);
			client.resizeGui();
		});
		context.waitTicks(10);
		int applied = context.computeOnClient(client -> client.getWindow().getGuiScale());
		check(applied == 4, "The high-scale shots are at GUI scale 4: " + applied);
		fits(context, "the terms at GUI scale 4");
		shot(context, "jugcraft_pact_terms_scale4");
		press(context, "back");
		fits(context, "a beat at GUI scale 4");
		shot(context, "jugcraft_pact_conversation_scale4");
		press(context, "next");
		context.runOnClient(client -> {
			client.options.guiScale().set(2);
			client.resizeGui();
		});
		context.getInput().resizeWindow(640, 360);
		context.waitTicks(10);
		fits(context, "the terms in a 640x360 window");
		shot(context, "jugcraft_pact_terms_small");
		press(context, "back");
		fits(context, "a beat in a 640x360 window");
		shot(context, "jugcraft_pact_conversation_small");
		press(context, "next");
		context.getInput().resizeWindow(width, height);
		context.waitTicks(10);
		check(context.computeOnClient(client -> screen(client).page() == PactScreen.TERMS_PAGE), "Back on his terms");
	}

	private static void acceptance(ClientGameTestContext context, TestDedicatedServerContext server) {
		tabTo(context, "accept");
		PactStatePayload.Result refused = answer(context);
		Jugcraft.LOGGER.info("[pact-check] Accept without the compact's materials: {} ({})", refused.reason(), refused.message().getString());
		check("cannot_afford".equals(refused.reason()) && !refused.done() && context.computeOnClient(client -> PactClient.state().mode()) == PactStatePayload.NEGOTIATE,
				"Without the compact's materials Accept is refused with its reason: " + refused.reason());
		check(!server.computeOnServer(ConcordancePactClientGameTests::held)
				&& server.computeOnServer(ConcordancePactClientGameTests::charge) == AstrolabeItem.CAPACITY, "A refusal records and spends nothing");
		// D-06: the screen rebuilds on its next tick; the refusal carries its mark, a cross, not colour alone (waited for: critique 4).
		context.waitFor(client -> screen(client).control("result") instanceof PactScreen.Verdict verdict && verdict.refused(), WAIT);
		shot(context, "jugcraft_pact_refused");
		List<String> cost = context.computeOnClient(client -> PactClient.state().compact().stream()
				.map(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()) + " " + stack.getCount()).toList());
		int resonance = context.computeOnClient(client -> PactClient.state().resonance());
		Jugcraft.LOGGER.info("[pact-check] the Open Hand Compact: {} and {} resonance", cost, resonance);
		check(!cost.isEmpty() && resonance > 0 && resonance <= AstrolabeItem.CAPACITY, "The compact names its materials and resonance (D-S13)");
		server.runOnServer(minecraft -> cost.forEach(line -> player(minecraft).getInventory().add(stack(line))));
		context.waitTicks(5);
		tabTo(context, "accept");
		PactStatePayload.Result sealed = answer(context);
		check(sealed.done() && "sealed".equals(sealed.reason()) && context.computeOnClient(client -> PactClient.state().mode()) == PactStatePayload.KEEP,
				"Accepting with the compact seals the pact: " + sealed.reason());
		String spent = server.computeOnServer(minecraft -> held(minecraft) + " " + charge(minecraft) + " "
				+ cost.stream().mapToInt(line -> player(minecraft).getInventory().countItem(stack(line).getItem())).sum());
		Jugcraft.LOGGER.info("[pact-check] sealed: held, astrolabe, materials left = {}", spent);
		check(spent.equals("true " + (AstrolabeItem.CAPACITY - resonance) + " 0"), "Sealed once, materials and resonance spent once: " + spent);
		// D-06: sealed, the answer carries the tick, once the screen has rebuilt for it (critique 4).
		context.waitFor(client -> screen(client).control("result") instanceof PactScreen.Verdict verdict && !verdict.refused(), WAIT);
		fits(context, "the kept pact at GUI scale 2");
		named(context);
		shot(context, "jugcraft_pact_kept");
	}

	private static void body(ClientGameTestContext context, TestDedicatedServerContext server, TestDedicatedServerConnection connection,
			BlockPos perch) {
		context.getInput().pressKey(InputConstants.KEY_ESCAPE);
		context.waitForScreen(null);
		check(server.computeOnServer(ConcordancePactClientGameTests::held), "Closing the screen keeps the pact");
		check(context.computeOnClient(client -> {
			Entity stolas = stolas(client, perch);
			return stolas != null && client.getEntityRenderDispatcher().getRenderer(stolas) instanceof GeoEntityRenderer<?, ?>;
		}), "Stolas stays on his perch, drawn by GeckoLib (his raven body, D-06)");
		boolean hidden = context.computeOnClient(client -> client.gui.hud.isHidden());
		context.runOnClient(client -> {
			if (!client.gui.hud.isHidden()) {
				client.gui.hud.toggle();
			}
		});
		server.runCommand(String.format(Locale.ROOT, "tp @a %.1f %d %.1f 180 0", perch.getX() + 0.5, perch.getY(), perch.getZ() + 3.0));
		context.waitTicks(30);
		connection.waitForChunksRender();
		server.runCommand("tick freeze");
		shot(context, "jugcraft_pact_stolas");
		server.runCommand("tick unfreeze");
		context.runOnClient(client -> {
			if (client.gui.hud.isHidden() != hidden) {
				client.gui.hud.toggle();
			}
		});
	}

	/**
	 * D-05: the kept pact's service. The amethyst shard is given before the one open (critique 6: a second open within the
	 * perch's rate gate would be refused), so the first state already offers it.
	 */
	private static void service(ClientGameTestContext context, TestDedicatedServerContext server, BlockPos perch) {
		server.runOnServer(minecraft -> player(minecraft).getInventory().add(new ItemStack(Items.AMETHYST_SHARD)));
		context.waitTicks(5);
		open(context, server, perch);
		context.waitFor(client -> PactClient.state() != null && PactClient.state().mode() == PactStatePayload.KEEP, WAIT);
		int star = context.computeOnClient(client -> index(PactClient.state().patterns(), LANTERN_STAR));
		check(star >= 0 && context.computeOnClient(client -> PactClient.state().patterns().get(star).ready()),
				"His service offers the patterns the holder observed, this occurrence still recallable (D-S1)");
		int cost = context.computeOnClient(client -> PactClient.state().patterns().get(star).amount());
		int before = server.computeOnServer(ConcordancePactClientGameTests::charge);
		press(context, "pattern:" + star);
		check(context.computeOnClient(client -> {
			AbstractWidget recall = screen(client).control("recall");
			Component named = Component.translatable("screen.jugcraft.pact.recall", PactClient.state().patterns().get(star).name());
			return recall != null && recall.active && recall.getMessage().getString().equals(named.getString());
		}), "Choosing the Lantern Star names it on Recall");
		tabTo(context, "recall");
		PactStatePayload.Result recalled = answer(context);
		String claimed = server.computeOnServer(minecraft -> charge(minecraft) + " "
				+ AstralClaims.of(minecraft).ledger(player(minecraft).getUUID()).claims().containsKey("recall:" + LANTERN_STAR));
		Jugcraft.LOGGER.info("[pact-check] Star-reckoning: {}, astrolabe and claim = {}", recalled.reason(), claimed);
		check(recalled.done() && "recalled".equals(recalled.reason()) && claimed.equals((before - cost) + " true"),
				"Star-reckoning recalls the chosen pattern at its recall cost, under the shared claim: " + claimed);
		int shard = context.computeOnClient(client -> index(PactClient.state().offerings(), "minecraft:amethyst_shard"));
		check(shard >= 0, "The amethyst shard carried is offered (D-S3)");
		tabTo(context, "offer:" + shard);
		PactStatePayload.Result offered = answer(context);
		String escrow = server.computeOnServer(minecraft -> BoundWills.of(minecraft).escrow(pact(minecraft).id()).stream()
				.mapToInt(ItemStack::getCount).sum() + " " + player(minecraft).getInventory().countItem(Items.AMETHYST_SHARD));
		Jugcraft.LOGGER.info("[pact-check] offering: {}, escrow and shards = {}", offered.reason(), escrow);
		check(offered.done() && "deposited".equals(offered.reason()) && escrow.equals("1 0"), "The offering is paid ahead: " + escrow);
		shot(context, "jugcraft_pact_service");
	}

	/** D-05: End asks first; Esc keeps the pact; its yes ends it once, the offering back, Stolas gone. */
	private static void ending(ClientGameTestContext context, TestDedicatedServerContext server, BlockPos perch) {
		press(context, "end");
		context.waitForScreen(ConfirmScreen.class);
		check(context.computeOnClient(client -> client.gui.screen().getFocused() instanceof AbstractWidget keep
				&& keep.getMessage().getString().equals(Component.translatable("screen.jugcraft.pact.end.no").getString())),
				"End asks first, the keyboard on Keep it (D-06)");
		shot(context, "jugcraft_pact_end_confirm");
		context.getInput().pressKey(InputConstants.KEY_ESCAPE);
		context.waitForScreen(PactScreen.class);
		context.waitTicks(5);
		check(server.computeOnServer(ConcordancePactClientGameTests::held), "Keep it (Esc) ends nothing");
		press(context, "end");
		context.waitForScreen(ConfirmScreen.class);
		tabToLabel(context, Component.translatable("screen.jugcraft.pact.end.yes").getString());
		PactStatePayload.Result ended = answer(context);
		check(StolasEntity.Gesture.DEPART.animation.equals(context.computeOnClient(client -> gestured(client, perch))),
				"Ending, he makes his departing gesture before he leaves the world (D-06)");
		String after = server.computeOnServer(minecraft -> held(minecraft) + " " + player(minecraft).getInventory().countItem(Items.AMETHYST_SHARD));
		Jugcraft.LOGGER.info("[pact-check] ended: {}, held and shards = {}", ended.reason(), after);
		check(ended.done() && "ended".equals(ended.reason()) && after.equals("false 1")
				&& context.computeOnClient(client -> PactClient.state().mode()) == PactStatePayload.CLOSED,
				"Ending ends the pact once and hands what was paid ahead back: " + after);
		context.waitFor(client -> stolas(client, perch) == null, WAIT);
		shot(context, "jugcraft_pact_ended");
		context.getInput().pressKey(InputConstants.KEY_ESCAPE);
		context.waitForScreen(null);
	}

	// D-06
	/**
	 * His body (D-06, D-S5), the pact kept and its holder before him, two and a half blocks south of his perch (he is
	 * summoned facing south, so each pose shows his face: D-P133): each loop as the server's
	 * state drives it (perched; listening while his perch's screen is open; hunched while the pact is suspended; standing
	 * and waiting while the Concordance is off), and each gesture as the server fires it, caught inside its telling pose.
	 * Each is checked on the client's own animation controller with the world frozen there (CinderTyrantClientGameTests'
	 * pattern), and photographed with the HUD hidden. His gestures change nothing. His astir loop (the owner's
	 * idle.flapping, while an offering is due) is not posed: making one due here would change the upkeep the later steps
	 * read (a recorded deviation; tools/concordance_pacts.py BODY_SHOTS).
	 */
	private static void poses(ClientGameTestContext context, TestDedicatedServerContext server, BlockPos perch) {
		boolean hidden = context.computeOnClient(client -> client.gui.hud.isHidden());
		context.runOnClient(client -> {
			if (!client.gui.hud.isHidden()) {
				client.gui.hud.toggle();
			}
		});
		loopPose(context, server, perch, IDLE, "jugcraft_stolas_idle", false);
		open(context, server, perch);
		context.waitFor(client -> stolas(client, perch) instanceof StolasEntity stolas && stolas.listening(), WAIT);
		loopPose(context, server, perch, LISTEN, "jugcraft_stolas_listen", true);
		context.waitFor(client -> IDLE.equals(drawing(client, perch, MAIN)), WAIT);
		check(server.computeOnServer(minecraft -> PactKeeping.suspend(minecraft.overworld(), player(minecraft), holder(minecraft, perch),
				minecraft.overworld().getGameTime())) == AgreementHolder.Toggle.SUSPENDED, "Its holder suspends the pact");
		loopPose(context, server, perch, SUSPENDED, "jugcraft_stolas_suspended", false);
		check(server.computeOnServer(minecraft -> PactKeeping.resume(minecraft.overworld(), player(minecraft), holder(minecraft, perch),
				minecraft.overworld().getGameTime())) == AgreementHolder.Toggle.RESUMED, "and resumes it");
		context.waitFor(client -> IDLE.equals(drawing(client, perch, MAIN)), WAIT);
		boolean on = server.computeOnServer(minecraft -> JugcraftConfig.isFeatureEnabled(JugcraftConcordance.FEATURE));
		server.runOnServer(minecraft -> JugcraftConfig.setFeatureEnabled(JugcraftConcordance.FEATURE, false));
		try {
			loopPose(context, server, perch, WAITING, "jugcraft_stolas_waiting", false);
		} finally {
			server.runOnServer(minecraft -> JugcraftConfig.setFeatureEnabled(JugcraftConcordance.FEATURE, on));
		}
		context.waitFor(client -> IDLE.equals(drawing(client, perch, MAIN)), WAIT);
		gesturePose(context, server, perch, StolasEntity.Gesture.INSPECT, 0.40, 1.10, "jugcraft_stolas_inspect");
		gesturePose(context, server, perch, StolasEntity.Gesture.ASSENT, 0.45, 1.25, "jugcraft_stolas_assent");
		gesturePose(context, server, perch, StolasEntity.Gesture.REFUSE, 0.50, 1.30, "jugcraft_stolas_refuse");
		refusalSequence(context, server, perch); // D-09
		// He leaves (depart holds its last frame, nothing of him drawn) and comes back (his arrival replaces it).
		gesturePose(context, server, perch, StolasEntity.Gesture.DEPART, 0.30, 0.80, "jugcraft_stolas_depart");
		gesturePose(context, server, perch, StolasEntity.Gesture.ARRIVE, 0.70, 1.20, "jugcraft_stolas_arrive");
		context.waitFor(client -> drawing(client, perch, StolasEntity.GESTURE_CONTROLLER).isEmpty() && IDLE.equals(drawing(client, perch, MAIN)), WAIT);
		check(server.computeOnServer(ConcordancePactClientGameTests::held) && server.computeOnServer(minecraft -> presence(minecraft, perch).isAlive()),
				"His gestures change nothing: the pact is kept and he is on his perch");
		context.runOnClient(client -> {
			if (client.gui.hud.isHidden() != hidden) {
				client.gui.hud.toggle();
			}
		});
	}

	// D-06
	/** Waits until his main controller draws {@code clip} past its blend, freezes the world, closes the screen if asked, checks and photographs it, thaws it. */
	private static void loopPose(ClientGameTestContext context, TestDedicatedServerContext server, BlockPos perch, String clip, String name,
			boolean closeScreen) {
		context.waitFor(client -> clip.equals(drawing(client, perch, MAIN)) && !blending(client, perch, MAIN), WAIT);
		server.runCommand("tick freeze");
		if (closeScreen) {
			context.getInput().pressKey(InputConstants.KEY_ESCAPE);
			context.waitForScreen(null);
		}
		context.waitTicks(5);
		check(context.computeOnClient(client -> clip.equals(drawing(client, perch, MAIN))), "Frozen while he draws " + clip);
		shot(context, name);
		server.runCommand("tick unfreeze");
	}

	// D-06
	/**
	 * The server fires {@code gesture}; the world is frozen at the middle of its telling pose ({@code from} to {@code to}
	 * seconds into it), where the client's gesture controller must be drawing it; photographed, thawed, and played out.
	 */
	private static void gesturePose(ClientGameTestContext context, TestDedicatedServerContext server, BlockPos perch, StolasEntity.Gesture gesture,
			double from, double to, String name) {
		int middle = (int) Math.round((from + to) * 10.0);
		server.runOnServer(minecraft -> presence(minecraft, perch).gesture(gesture));
		context.waitTicks(middle);
		server.runCommand("tick freeze");
		context.waitTicks(5);
		double at = context.computeOnClient(client -> gesture.clip().equals(drawing(client, perch, StolasEntity.GESTURE_CONTROLLER))
				? controller(client, perch, StolasEntity.GESTURE_CONTROLLER).getCurrentAnimationTime() : -1.0);
		Jugcraft.LOGGER.info("[pact-check] his {} frozen {} s into its clip", gesture.id, at);
		check(at >= from && at <= to, "His " + gesture.id + " is caught in its telling pose, " + from + " to " + to + " s into it: " + at);
		shot(context, name);
		server.runCommand("tick unfreeze");
		context.waitTicks(gesture.ticks - middle + 5);
	}

	// D-09
	/**
	 * A timed sequence through his refusal (08:176; D-P134), the tell a player most needs to read without words (03:159): the
	 * server fires it once, and the world is frozen at each of REFUSAL_TIMES, where the client's gesture controller must be
	 * drawing it, later into the clip than the frame before; each frame's time is logged and photographed. It changes nothing.
	 */
	private static void refusalSequence(ClientGameTestContext context, TestDedicatedServerContext server, BlockPos perch) {
		StolasEntity.Gesture refuse = StolasEntity.Gesture.REFUSE;
		server.runOnServer(minecraft -> presence(minecraft, perch).gesture(refuse));
		double last = -1.0;
		int waited = 0;
		for (int i = 0; i < REFUSAL_TIMES.length; i++) {
			int at = (int) Math.round(REFUSAL_TIMES[i] * 20.0);
			context.waitTicks(at - waited);
			waited = at;
			server.runCommand("tick freeze");
			try {
				context.waitTicks(5);
				double time = context.computeOnClient(client -> refuse.clip().equals(drawing(client, perch, StolasEntity.GESTURE_CONTROLLER))
						? controller(client, perch, StolasEntity.GESTURE_CONTROLLER).getCurrentAnimationTime() : -1.0);
				Jugcraft.LOGGER.info("[pact-check] his refusal frozen {} s into its clip (frame {} of {}, asked {} s)", time, i + 1, REFUSAL_TIMES.length,
						REFUSAL_TIMES[i]);
				check(time > last && time >= 0.0 && time < refuse.ticks / 20.0,
						"His refusal's frame " + (i + 1) + " lies inside its clip, after the one before: " + time);
				last = time;
				shot(context, REFUSAL_SHOTS[i]);
			} finally {
				// The world thaws whatever failed (a shared switch).
				server.runCommand("tick unfreeze");
			}
		}
		context.waitTicks(refuse.ticks - waited + 5);
	}

	// D-06
	private static @Nullable AnimationController<StolasEntity> controller(Minecraft client, BlockPos perch, String name) {
		if (!(stolas(client, perch) instanceof StolasEntity stolas)) {
			return null;
		}
		AnimatableManager<StolasEntity> manager = stolas.getAnimatableInstanceCache().getManagerForId(stolas.getId());
		return manager.getAnimationControllers().get(name);
	}

	// D-06
	/** The clip one of his controllers is drawing now ("" when it draws nothing). */
	private static String drawing(Minecraft client, BlockPos perch, String name) {
		AnimationController<StolasEntity> controller = controller(client, perch, name);
		AnimationPoint point = controller == null || !controller.isAnimatingBones() ? null : controller.getCurrentAnimationPoint();
		return point == null ? "" : point.animation().name();
	}

	// D-06
	private static boolean blending(Minecraft client, BlockPos perch, String name) {
		AnimationController<StolasEntity> controller = controller(client, perch, name);
		return controller != null && controller.isTransitioning();
	}

	// D-06
	/** The gesture last triggered on him (it stays set after it plays). */
	private static @Nullable RawAnimation gestured(Minecraft client, BlockPos perch) {
		AnimationController<StolasEntity> gestures = controller(client, perch, StolasEntity.GESTURE_CONTROLLER);
		return gestures == null ? null : gestures.getCurrentRawAnimation();
	}

	// D-06
	/** Stolas at {@code perch} on the server (the presence, else the visitor; never one leaving). */
	private static StolasEntity presence(MinecraftServer minecraft, BlockPos perch) {
		StolasEntity stolas = ((PactPerchBlockEntity) minecraft.overworld().getBlockEntity(perch)).stolas(minecraft.overworld());
		check(stolas != null, "Stolas is on his perch");
		return stolas;
	}

	// D-06
	private static AgreementHolder holder(MinecraftServer minecraft, BlockPos perch) {
		return ((AgreementHolder.Holds) minecraft.overworld().getBlockEntity(perch)).holder();
	}

	// D-06
	private record Hearing(List<String> heard, SoundEventListener listener, boolean sounding, boolean subtitles) {
	}

	// D-06
	/** Subtitles shown and a listener on the sound manager before the refusal and the acceptance (03:159). */
	private static Hearing hear(ClientGameTestContext context) {
		List<String> heard = new CopyOnWriteArrayList<>();
		SoundEventListener listener = (sound, events, range) -> {
			Component subtitle = events.getSubtitle();
			if (subtitle != null) {
				heard.add(subtitle.getString());
			}
		};
		boolean subtitles = context.computeOnClient(client -> client.options.showSubtitles().get());
		boolean sounding = context.computeOnClient(client -> {
			client.options.showSubtitles().set(true);
			client.getSoundManager().addListener(listener);
			// The sound engine starts only with a sound device; without one (CI's runners) nothing plays and no subtitle shows.
			return client.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F)) != SoundEngine.PlayResult.NOT_STARTED;
		});
		return new Hearing(heard, listener, sounding, subtitles);
	}

	// D-06
	/** His refuse and assent fired by the server, their subtitles his; heard where the machine can play sound. */
	private static void heard(ClientGameTestContext context, TestDedicatedServerContext server, Hearing hearing, BlockPos perch) {
		String refuse = context.computeOnClient(client -> Component.translatable("subtitles.jugcraft.stolas.refuse").getString());
		String assent = context.computeOnClient(client -> Component.translatable("subtitles.jugcraft.stolas.assent").getString());
		check(!refuse.startsWith("subtitles.") && !assent.startsWith("subtitles.")
				&& context.computeOnClient(client -> refuse.equals(subtitle(client, "stolas.refuse")) && assent.equals(subtitle(client, "stolas.assent"))),
				"His refuse and assent have their subtitles in the client's sounds: " + refuse + ", " + assent);
		List<String> cues = server.computeOnServer(minecraft -> presence(minecraft, perch).cues());
		Jugcraft.LOGGER.info("[pact-check] Stolas's cues so far: {}", cues);
		check(cues.contains("refuse") && "assent".equals(cues.getLast()), "The server fired his refusal, then his assent: " + cues);
		try {
			if (hearing.sounding()) {
				context.waitFor(client -> hearing.heard().contains(refuse) && hearing.heard().contains(assent), WAIT);
				Jugcraft.LOGGER.info("[pact-check] subtitles heard: {}", hearing.heard());
			} else {
				Jugcraft.LOGGER.info("[pact-check] no sound device: the sound engine is off, so no subtitle can show; checked as defined and as fired");
			}
		} finally {
			context.runOnClient(client -> {
				client.getSoundManager().removeListener(hearing.listener());
				client.options.showSubtitles().set(hearing.subtitles());
			});
		}
	}

	// D-06
	/** Jade's readings of Stolas and his perch (PactReading, read on the server: no Jade class); with Jade loaded, their overlays. */
	private static void jade(ClientGameTestContext context, TestDedicatedServerContext server, BlockPos perch) {
		String readings = server.computeOnServer(minecraft -> {
			ServerLevel level = minecraft.overworld();
			CompoundTag ofPerch = PactReading.holder(level, (PactPerchBlockEntity) level.getBlockEntity(perch), level.getGameTime());
			CompoundTag ofHim = PactReading.stolas(level, presence(minecraft, perch), level.getGameTime());
			String both = ofPerch + " " + ofHim;
			boolean right = "kept".equals(ofPerch.getStringOr("standing", "")) && STOLAS.equals(ofPerch.getStringOr("counterpart", ""))
					&& "star_reckoning".equals(ofPerch.getStringOr("service", "")) && "active".equals(ofPerch.getStringOr("state", ""))
					&& ofPerch.getIntOr("due_days", -1) == 7 && "kept".equals(ofHim.getStringOr("standing", ""))
					&& PactReading.KEYS.containsAll(ofPerch.keySet()) && PactReading.KEYS.containsAll(ofHim.keySet())
					&& !both.contains(player(minecraft).getUUID().toString()) && !both.contains(player(minecraft).getName().getString());
			return (right ? "ok " : "bad ") + both;
		});
		Jugcraft.LOGGER.info("[pact-check] Jade's readings of the perch and of Stolas: {}", readings);
		check(readings.startsWith("ok"), "Jade's readings: the pact, service, state and next offering, naming no player: " + readings);
		if (FabricLoader.getInstance().isModLoaded("jade")) {
			boolean hidden = context.computeOnClient(client -> client.gui.hud.isHidden());
			context.runOnClient(client -> {
				if (client.gui.hud.isHidden()) {
					client.gui.hud.toggle();
				}
			});
			try {
				server.runCommand(String.format(Locale.ROOT, "tp @a %.1f %d %.1f 180 0", perch.getX() + 0.5, perch.getY(), perch.getZ() + 3.0));
				context.waitTicks(10);
				context.getInput().lookAt(perch);
				context.waitTicks(20);
				shot(context, "jugcraft_pact_perch_jade");
				context.getInput().lookAt(perch.above());
				context.waitTicks(20);
				shot(context, "jugcraft_pact_stolas_jade");
			} finally {
				context.runOnClient(client -> {
					if (client.gui.hud.isHidden() != hidden) {
						client.gui.hud.toggle();
					}
				});
			}
		} else {
			Jugcraft.LOGGER.info("[pact-check] Jade is not loaded: no Jade shot of Stolas or his perch");
		}
	}

	// D-06
	private static String subtitle(Minecraft client, String event) {
		WeighedSoundEvents events = client.getSoundManager().getSoundEvent(Jugcraft.id(event));
		Component subtitle = events == null ? null : events.getSubtitle();
		return subtitle == null ? "" : subtitle.getString();
	}

	/**
	 * The perch used by its owner on the server (its own interaction; no click is aimed), and its menu's first state. It waits
	 * for a menu newer than the one open before, so a use the server refused (its rate gate) fails here, never passing
	 * against the screen and state already shown.
	 */
	private static void open(ClientGameTestContext context, TestDedicatedServerContext server, BlockPos perch) {
		int before = context.computeOnClient(client -> client.player.containerMenu.containerId);
		server.runOnServer(minecraft -> {
			ServerLevel level = minecraft.overworld();
			level.getBlockState(perch).useWithoutItem(level, player(minecraft), new BlockHitResult(Vec3.atCenterOf(perch), Direction.NORTH, perch, false));
		});
		context.waitForScreen(PactScreen.class);
		context.waitFor(client -> client.gui.screen() instanceof PactScreen screen && screen.getMenu().containerId != before
				&& PactClient.state() != null, WAIT);
	}

	private static void press(ClientGameTestContext context, String key) {
		tabTo(context, key);
		context.getInput().pressKey(InputConstants.KEY_RETURN);
		context.waitTicks(3);
	}

	private static void tabTo(ClientGameTestContext context, String key) {
		for (int i = 0; i < 48; i++) {
			if (key.equals(context.computeOnClient(client -> client.gui.screen() instanceof PactScreen screen ? screen.focusedKey() : ""))) {
				return;
			}
			context.getInput().pressKey(InputConstants.KEY_TAB);
			context.waitTicks(1);
		}
		throw new AssertionError("Tab never reached " + key);
	}

	/** D-05: Tab until the focused control's words are {@code label} (a vanilla screen's button, as the confirmation's). */
	private static void tabToLabel(ClientGameTestContext context, String label) {
		for (int i = 0; i < 8; i++) {
			if (context.computeOnClient(client -> client.gui.screen().getFocused() instanceof AbstractWidget widget
					&& widget.getMessage().getString().equals(label))) {
				return;
			}
			context.getInput().pressKey(InputConstants.KEY_TAB);
			context.waitTicks(1);
		}
		throw new AssertionError("Tab never reached " + label);
	}

	/** Enter on the focused control, then the server's answer to it (its own sequence; never an earlier answer). */
	private static PactStatePayload.Result answer(ClientGameTestContext context) {
		int before = context.computeOnClient(client -> PactClient.lastAnswer());
		context.getInput().pressKey(InputConstants.KEY_RETURN);
		context.waitFor(client -> PactClient.lastAnswer() > before && !PactClient.waiting() && PactClient.state() != null, WAIT);
		return context.computeOnClient(client -> PactClient.state().result().orElseThrow());
	}

	private static void fits(ClientGameTestContext context, String where) {
		List<String> outside = context.computeOnClient(client -> screen(client).outside());
		check(outside.isEmpty(), "Every control of the pact screen is on screen (" + where + "): " + outside);
	}

	private static void named(ClientGameTestContext context) {
		List<String> blank = context.computeOnClient(client -> blank(client.gui.screen()));
		check(blank.isEmpty(), "Every reachable control has words for the narrator: " + blank);
	}

	private static List<String> blank(GuiEventListener parent) {
		List<String> out = new ArrayList<>();
		if (parent instanceof ContainerEventHandler container) {
			for (GuiEventListener child : container.children()) {
				out.addAll(blank(child));
			}
		} else if (parent instanceof AbstractWidget widget && widget.active && widget.getMessage().getString().isBlank()) {
			out.add(widget.getClass().getSimpleName() + " at " + widget.getX() + "," + widget.getY());
		}
		return out;
	}

	private static void shot(ClientGameTestContext context, String name) {
		context.runOnClient(client -> client.gui.toastManager().clear());
		context.waitTicks(2);
		context.takeScreenshot(name);
	}

	private static PactScreen screen(Minecraft client) {
		return (PactScreen) client.gui.screen();
	}

	private static @Nullable Entity stolas(Minecraft client, BlockPos perch) {
		EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse(STOLAS));
		List<Entity> found = client.level.getEntities((Entity) null, new AABB(perch).inflate(4), entity -> entity.getType() == type);
		return found.isEmpty() ? null : found.getFirst();
	}

	private static String describe(@Nullable PactStatePayload state) {
		return state == null ? "no state" : "mode " + state.mode() + ", " + state.beats().size() + " beats, terms "
				+ state.terms().stream().map(PactStatePayload.Term::key).toList() + ", notes "
				+ state.notes().stream().map(Component::getString).toList();
	}

	/** D-05: the index of the choice {@code id} in a state's list, or -1. */
	private static int index(List<PactStatePayload.Choice> choices, String id) {
		for (int i = 0; i < choices.size(); i++) {
			if (choices.get(i).id().equals(id)) {
				return i;
			}
		}
		return -1;
	}

	private static ItemStack stack(String line) {
		String[] part = line.split(" ");
		return new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(part[0])), Integer.parseInt(part[1]));
	}

	private static ServerPlayer player(MinecraftServer minecraft) {
		return minecraft.getPlayerList().getPlayers().getFirst();
	}

	private static boolean held(MinecraftServer minecraft) {
		return Pacts.holds(BoundWills.of(minecraft).ledger(), player(minecraft).getUUID(), STOLAS);
	}

	/** D-05: the player's pact with Stolas (its record, for its escrow). */
	private static BoundWill pact(MinecraftServer minecraft) {
		UUID holder = player(minecraft).getUUID();
		return BoundWills.of(minecraft).ledger().heldBy(holder).stream()
				.filter(will -> Pacts.counterpart(will.counterpart()).equals(STOLAS)).findFirst().orElseThrow();
	}

	private static int charge(MinecraftServer minecraft) {
		Inventory inventory = player(minecraft).getInventory();
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			if (inventory.getItem(slot).is(Sky.ASTROLABE)) {
				return AstrolabeItem.charge(inventory.getItem(slot));
			}
		}
		return -1;
	}

	private static void check(boolean condition, String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}
}
