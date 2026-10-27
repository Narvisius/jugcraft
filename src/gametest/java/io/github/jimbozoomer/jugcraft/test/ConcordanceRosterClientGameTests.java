package io.github.jimbozoomer.jugcraft.test;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.client.PactScreen;
import io.github.jimbozoomer.jugcraft.client.RosterClient;
import io.github.jimbozoomer.jugcraft.client.RosterScreen;
import io.github.jimbozoomer.jugcraft.client.ScreenKit;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import io.github.jimbozoomer.jugcraft.concordance.courier.CourierLedger;
import io.github.jimbozoomer.jugcraft.concordance.courier.CourierPostBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.courier.Couriers;
import io.github.jimbozoomer.jugcraft.concordance.logistics.Logistics;
import io.github.jimbozoomer.jugcraft.concordance.logistics.Place;
import io.github.jimbozoomer.jugcraft.concordance.logistics.Request;
import io.github.jimbozoomer.jugcraft.concordance.logistics.RequestState;
import io.github.jimbozoomer.jugcraft.concordance.resource.BoundWill;
import io.github.jimbozoomer.jugcraft.concordance.resource.BoundWillLedger;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import io.github.jimbozoomer.jugcraft.concordance.spirits.BoundWills;
import io.github.jimbozoomer.jugcraft.concordance.spirits.GatheringShadeEntity;
import io.github.jimbozoomer.jugcraft.concordance.spirits.PactHistory;
import io.github.jimbozoomer.jugcraft.concordance.spirits.PactPerchBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.spirits.PactStatePayload;
import io.github.jimbozoomer.jugcraft.concordance.spirits.PorterKeyItem;
import io.github.jimbozoomer.jugcraft.concordance.spirits.RosterRequestPayload;
import io.github.jimbozoomer.jugcraft.concordance.spirits.RosterStatePayload;
import io.github.jimbozoomer.jugcraft.concordance.spirits.SpiritAnchorBlock;
import io.github.jimbozoomer.jugcraft.concordance.spirits.SpiritAnchorBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.spirits.WorkerRoster;
import io.github.jimbozoomer.jugcraft.concordance.spirits.Workers;
import io.github.jimbozoomer.jugcraft.concordance.spirits.WorkshopHandEntity;
import io.github.jimbozoomer.jugcraft.concordance.worker.PactDefinition;
import io.github.jimbozoomer.jugcraft.concordance.worker.Pacts;
import io.github.jimbozoomer.jugcraft.concordance.worker.Status;
import io.github.jimbozoomer.jugcraft.concordance.worker.WorkerDefinition;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Concordance Chunk D, D-07 (unit 3): the roster screen in a real client, on the game's own dedicated server. One client;
 * results are logged with the prefix "[roster-check]"; the player is in survival; the day is held at noon.
 * <ul>
 * <li><b>Empty</b>: opened by its key (bound here for the test: it is not bound at first) before anything is kept
 * ({@code jugcraft_roster_empty}).</li>
 * <li><b>Rows</b>: Stolas's pact, a Gathering Shade's agreement and a workshop hand, each drawn from its own model, a courier
 * request stranded with two glowstone and an open one, each state in words beside its mark ({@code jugcraft_roster}); at
 * GUI scale 4 in a 1920x1080 window and in the smallest GUI, 320x240 ({@code jugcraft_roster_scale4},
 * {@code jugcraft_roster_small}); every control on screen and named. Each answer carries its mark (a tick or a cross);
 * subtitles are drawn over the screen.</li>
 * <li><b>Suspend and resume</b> Stolas's pact by keyboard ({@code jugcraft_roster_suspended}).</li>
 * <li><b>End by keyboard alone</b>: End asks first, the keyboard on Keep it ({@code jugcraft_roster_end_confirm}); Esc keeps
 * the agreement; Tab and Enter on End it end the shade's agreement once ({@code jugcraft_roster_ended}).</li>
 * <li><b>Recover and cancel</b>: the stranded glowstone into the inventory, once ({@code jugcraft_roster_recovered}); the
 * open request cancelled.</li>
 * <li>(D-09) <b>The workshop hand at work</b>: three copper ingots carried across the yard, frozen with its cargo aboard
 * ({@code jugcraft_workshop_hand_at_work}), then delivered once.</li>
 * <li><b>Full</b>: 32 roster entries, 32 Bound Wills and 16 requests: 64 rows listed and the rest counted, eight pages turned
 * with Page Down ({@code jugcraft_roster_full}, {@code jugcraft_roster_full_small}).</li>
 * </ul>
 * The narrator is not heard, and one client is not two.
 */
public class ConcordanceRosterClientGameTests implements FabricClientGameTest {
	private static final int WAIT = 200;
	/** Ticks between two actions, above the server's roster rate gate. */
	private static final int SETTLE = 20;
	private static final String STOLAS = "jugcraft:stolas";
	private static final String SHADE = "jugcraft:gathering_shade";
	private static final String HAND = "jugcraft:workshop_hand";
	/** The key the test binds the roster to (it is not bound at first). */
	private static final int TEST_KEY = InputConstants.KEY_F7;
	/** D-09: ticks the workshop hand may take to claim, carry and deliver one request. */
	private static final int WORK_WAIT = 1200;

	private record Setup(BlockPos second, BlockPos destination, UUID pact, UUID shade, UUID shadeBody, long stranded, long open) {
	}

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestDedicatedServerContext server = context.worldBuilder().createServer()) {
			try (TestDedicatedServerConnection connection = server.connect()) {
				connection.waitForChunksRender();
				server.runCommand("time set noon");
				server.runCommand("weather clear");
				server.runCommand("gamerule minecraft:advance_time false");
				server.runCommand("gamerule minecraft:spawn_mobs false");
				server.runCommand("gamemode survival @a");
				context.waitTicks(20);
				BlockPos stand = context.computeOnClient(client -> client.player.blockPosition());
				server.runOnServer(minecraft -> ground(minecraft, stand));
				server.runCommand(String.format(Locale.ROOT, "tp @a %.1f %d %.1f 0 0", stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5));
				context.waitTicks(10);
				connection.waitForChunksRender();
				int scale = context.computeOnClient(client -> client.options.guiScale().get());
				int width = context.computeOnClient(client -> client.getWindow().getWidth());
				int height = context.computeOnClient(client -> client.getWindow().getHeight());
				KeyMapping key = RosterClient.key();
				check("key.keyboard.unknown".equals(key.getDefaultKey().getName()), "The roster's key is not bound at first");
				context.runOnClient(client -> {
					client.options.guiScale().set(2);
					client.resizeGui();
					key.setKey(InputConstants.Type.KEYBOARD.getOrCreate(TEST_KEY));
					KeyMapping.resetMapping();
				});
				try {
					String clash = context.computeOnClient(client -> Arrays.stream(client.options.keyMappings)
							.filter(other -> other != key && other.same(key)).map(KeyMapping::getName).toList().toString());
					check(clash.equals("[]"), "The test's binding for the roster shares no other key: " + clash);
					empty(context);
					Setup setup = server.computeOnServer(minecraft -> prepare(minecraft, stand));
					rows(context);
					sizes(context);
					suspendAndResume(context, server, setup);
					keyboardEnd(context, server, setup);
					recoverAndCancel(context, server, setup);
					handAtWork(context, server, stand); // D-09
					full(context, server, setup);
				} finally {
					// The window, its GUI scale and the key go back as they were, whatever failed.
					context.getInput().resizeWindow(width, height);
					context.runOnClient(client -> {
						key.setKey(key.getDefaultKey());
						KeyMapping.resetMapping();
						client.options.guiScale().set(scale);
						client.resizeGui();
					});
				}
			}
		}
	}

	/** A stone floor with open air over it round where the player stands. */
	private static void ground(MinecraftServer minecraft, BlockPos stand) {
		ServerLevel level = minecraft.overworld();
		for (int dx = -6; dx <= 6; dx++) {
			for (int dz = -2; dz <= 8; dz++) {
				level.setBlockAndUpdate(stand.offset(dx, -1, dz), Blocks.STONE.defaultBlockState());
				for (int dy = 0; dy <= 3; dy++) {
					level.setBlockAndUpdate(stand.offset(dx, dy, dz), Blocks.AIR.defaultBlockState());
				}
			}
		}
	}

	/**
	 * The player's Stolas pact (sealed as the perch seals it, the compact left out as the worker tests leave it), a Gathering
	 * Shade at their anchor, a workshop hand at an anchor bound to their Courier Post (D-08), and at a second post no
	 * courier serves two requests: one stranded with two glowstone (a courier released with them aboard), one open.
	 */
	private static Setup prepare(MinecraftServer minecraft, BlockPos stand) {
		ServerPlayer player = player(minecraft);
		ServerLevel level = minecraft.overworld();
		UUID holder = player.getUUID();
		long now = level.getGameTime();
		ConcordanceProgress.grant(player, "jugcraft:first_light", ResearchState.UNDERSTOOD);
		ConcordanceProgress.grant(player, Workers.RESEARCH, ResearchState.UNDERSTOOD);
		ConcordanceProgress.grant(player, WorkshopHandEntity.REGARD, ResearchState.MASTERED);
		RateGate.forget(holder);
		BlockPos perchAt = stand.offset(0, 0, 3);
		level.setBlockAndUpdate(perchAt, Workers.PACT_PERCH.defaultBlockState());
		PactPerchBlockEntity perch = (PactPerchBlockEntity) level.getBlockEntity(perchAt);
		perch.holder().setOwner(holder);
		PactDefinition offer = PactPerchBlockEntity.definition();
		check(offer != null && perch.holder().seal(level, player, PactPerchBlockEntity.sealing(offer, null, null), now).outcome() == Pacts.Outcome.DONE,
				"Stolas's pact is sealed at the player's perch");
		SpiritAnchorBlockEntity shadeAnchor = anchor(level, stand.offset(-3, 0, 3), holder);
		WorkerDefinition.Spirit shadeTerms = Workers.catalog().get(GatheringShadeEntity.DEFINITION, WorkerDefinition.Spirit.class);
		check(shadeTerms != null && shadeAnchor.seal(level, player, shadeTerms, now).outcome() == Pacts.Outcome.DONE, "A Gathering Shade is sealed");
		BlockPos postAt = stand.offset(3, 0, 6);
		post(level, postAt, holder);
		SpiritAnchorBlockEntity handAnchor = anchor(level, stand.offset(3, 0, 3), holder);
		ItemStack key = new ItemStack(Workers.PORTER_KEY);
		key.set(Couriers.KEY_POST, GlobalPos.of(level.dimension(), postAt));
		check(PorterKeyItem.bind(player, level, handAnchor, key, false).changed() && GlobalPos.of(level.dimension(), postAt).equals(handAnchor.post()),
				"The hand's anchor is bound to the player's post");
		SpiritAnchorBlock.use(player, level, handAnchor, false);
		check(handAnchor.agreement() != null && level.getEntity(handAnchor.spirit()) instanceof WorkshopHandEntity, "A workshop hand is sealed there");
		BlockPos secondAt = stand.offset(-3, 0, 6);
		BlockPos sourceAt = stand.offset(-4, 0, 6);
		BlockPos destinationAt = stand.offset(-2, 0, 6);
		post(level, secondAt, holder);
		level.setBlockAndUpdate(sourceAt, Blocks.CHEST.defaultBlockState());
		level.setBlockAndUpdate(destinationAt, Blocks.CHEST.defaultBlockState());
		((ChestBlockEntity) level.getBlockEntity(sourceAt)).setItem(0, new ItemStack(Items.GLOWSTONE_DUST, 4));
		CourierLedger ledger = CourierLedger.of(minecraft);
		Place at = Couriers.place(level, secondAt);
		Place to = Couriers.place(level, destinationAt);
		Logistics.Filed stranded = ledger.file(holder, at, to, new ItemStack(Items.GLOWSTONE_DUST), 2, level.registryAccess(), now);
		Storage<ItemVariant> source = ItemStorage.SIDED.find(level, sourceAt, null);
		UUID courier = new UUID(0xD07L, 1L);
		check(stranded.outcome() == Logistics.Outcome.DONE && ledger.claim(stranded.id(), courier, now) == Logistics.Outcome.DONE
				&& ledger.reserve(stranded.id(), courier, Couriers.place(level, sourceAt), source, now) == Logistics.Outcome.DONE
				&& ledger.pickUp(stranded.id(), courier, source, now) == Logistics.Outcome.DONE
				&& ledger.release(stranded.id(), courier, "worker_removed", false, now) == Logistics.Outcome.DONE
				&& ledger.ledger().get(stranded.id()).state() == RequestState.STRANDED && ledger.ledger().get(stranded.id()).progress().carried() == 2,
				"Two glowstone wait stranded at the second post");
		Logistics.Filed open = ledger.file(holder, at, to, new ItemStack(Items.REDSTONE), 3, level.registryAccess(), now);
		check(open.outcome() == Logistics.Outcome.DONE, "An open request is filed");
		return new Setup(secondAt, destinationAt, perch.holder().agreement().record(), shadeAnchor.agreement().record(), shadeAnchor.spirit(),
				stranded.id(), open.id());
	}

	private static SpiritAnchorBlockEntity anchor(ServerLevel level, BlockPos at, UUID owner) {
		level.setBlockAndUpdate(at, Workers.SPIRIT_ANCHOR.defaultBlockState());
		SpiritAnchorBlockEntity anchor = (SpiritAnchorBlockEntity) level.getBlockEntity(at);
		anchor.holder().setOwner(owner);
		return anchor;
	}

	private static CourierPostBlockEntity post(ServerLevel level, BlockPos at, UUID owner) {
		level.setBlockAndUpdate(at, Couriers.COURIER_POST.defaultBlockState());
		CourierPostBlockEntity post = (CourierPostBlockEntity) level.getBlockEntity(at);
		post.setOwner(owner);
		return post;
	}

	private static void empty(ClientGameTestContext context) {
		openByKey(context);
		check(context.computeOnClient(client -> RosterClient.state().rows().isEmpty() && RosterClient.state().more() == 0
				&& screen(client).control("empty") != null), "Before anything is kept the roster says it is empty");
		fits(context, "the empty roster");
		named(context);
		shot(context, "jugcraft_roster_empty");
		context.getInput().pressKey(InputConstants.KEY_ESCAPE);
		context.waitForScreen(null);
	}

	private static void rows(ClientGameTestContext context) {
		openByKey(context);
		context.waitFor(client -> row(STOLAS) != null && row(SHADE) != null && row(HAND) != null && stranded() != null && open() != null, WAIT);
		context.waitTicks(3);
		String described = context.computeOnClient(client -> describe(RosterClient.state()));
		Jugcraft.LOGGER.info("[roster-check] rows: {}", described);
		check(context.computeOnClient(client -> {
			RosterStatePayload.Row stolas = row(STOLAS);
			RosterStatePayload.Row shade = row(SHADE);
			RosterStatePayload.Row hand = row(HAND);
			return stolas.offers(RosterRequestPayload.SUSPEND) && stolas.offers(RosterRequestPayload.END) && stolas.loaded()
					&& !stolas.service().getString().isEmpty() && !stolas.next().getString().isEmpty()
					&& shade.offers(RosterRequestPayload.END) && !shade.waiting().getString().isEmpty()
					&& hand.offers(RosterRequestPayload.END) && !hand.waiting().getString().isEmpty();
		}), "Stolas's pact (Suspend, End, his service and next offering), the shade's agreement and the hand (End and a reason) are listed");
		check(context.computeOnClient(client -> stranded().offers(RosterRequestPayload.RECOVER) && open().offers(RosterRequestPayload.CANCEL)
				&& !open().offers(RosterRequestPayload.RECOVER)), "The stranded request offers Recover; the open one Cancel and not Recover");
		// The body draws only what is inside it (ComposerScroll), so each row is brought into view by the keyboard and then
		// read: Stolas, the shade and the hand drawn from their own models, the stranded request as its item.
		List<UUID> shown = context.computeOnClient(client -> List.of(row(STOLAS).id(), row(SHADE).id(), row(HAND).id(), stranded().id()));
		List<String> wanted = List.of("portrait", "portrait", "portrait", "icon");
		List<String> drawn = new ArrayList<>();
		for (int i = 0; i < shown.size(); i++) {
			UUID id = shown.get(i);
			tabTo(context, "row:" + id);
			context.waitFor(client -> !screen(client).drawn(id).isEmpty(), WAIT);
			String how = context.computeOnClient(client -> screen(client).drawn(id));
			drawn.add(how);
			check(wanted.get(i).equals(how), "Row " + i + " is drawn as its " + wanted.get(i) + ", once it is in view: " + how);
		}
		Jugcraft.LOGGER.info("[roster-check] drawn: {}", drawn);
		// Back to the first row, so the shot below shows the roster from its top.
		tabTo(context, "row:" + shown.getFirst());
		context.waitTicks(2);
		List<UUID> unworded = context.computeOnClient(ConcordanceRosterClientGameTests::unworded);
		check(unworded.isEmpty(), "Every row says its state in words beside its mark: " + unworded);
		check(context.computeOnClient(client -> screen(client).getNarrationMessage().getString()
				.contains(Component.translatable("screen.jugcraft.roster.page", 1, 1, RosterClient.state().rows().size()).getString())),
				"The screen says which page it shows");
		fits(context, "the roster at GUI scale 2");
		named(context);
		shot(context, "jugcraft_roster");
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
		check(applied == 4, "The high-scale shot is at GUI scale 4: " + applied);
		fits(context, "the roster at GUI scale 4");
		named(context);
		shot(context, "jugcraft_roster_scale4");
		smallest(context);
		fits(context, "the roster in the smallest GUI");
		named(context);
		shot(context, "jugcraft_roster_small");
		context.getInput().resizeWindow(width, height);
		context.runOnClient(client -> {
			client.options.guiScale().set(2);
			client.resizeGui();
		});
		context.waitTicks(10);
	}

	private static void suspendAndResume(ClientGameTestContext context, TestDedicatedServerContext server, Setup setup) {
		UUID id = context.computeOnClient(client -> row(STOLAS).id());
		context.waitTicks(SETTLE);
		tabTo(context, "suspend:" + id);
		PactStatePayload.Result suspended = answer(context);
		check(context.computeOnClient(client -> screen(client).control("result") instanceof PactScreen.Verdict verdict && !verdict.refused()),
				"The answer carries its tick (D-P103)");
		Jugcraft.LOGGER.info("[roster-check] suspend: {} ({})", suspended.reason(), suspended.message().getString());
		check(suspended.done() && "suspended".equals(suspended.reason()) && server.computeOnServer(m -> hold(m, setup.pact())) == BoundWill.Hold.HOLDER,
				"Suspend from the roster holds Stolas's pact on its Bound Will: " + suspended.reason());
		context.waitFor(client -> row(STOLAS).offers(RosterRequestPayload.RESUME) && row(STOLAS).icon() == RosterStatePayload.SUSPENDED, WAIT);
		shot(context, "jugcraft_roster_suspended");
		context.waitTicks(SETTLE);
		tabTo(context, "resume:" + id);
		PactStatePayload.Result resumed = answer(context);
		check(resumed.done() && "resumed".equals(resumed.reason()) && server.computeOnServer(m -> hold(m, setup.pact())) == BoundWill.Hold.NONE,
				"Resume lifts it: " + resumed.reason());
	}

	private static void keyboardEnd(ClientGameTestContext context, TestDedicatedServerContext server, Setup setup) {
		UUID id = context.computeOnClient(client -> row(SHADE).id());
		int heard = context.computeOnClient(client -> RosterClient.lastAnswer());
		context.waitTicks(SETTLE);
		tabTo(context, "end:" + id);
		context.getInput().pressKey(InputConstants.KEY_RETURN);
		context.waitFor(client -> client.gui.screen() instanceof ConfirmScreen, WAIT);
		String no = Component.translatable("screen.jugcraft.roster.end.no").getString();
		check(context.computeOnClient(client -> focusedLabel(client).equals(no)), "End asks first, the keyboard on Keep it");
		check(context.computeOnClient(client -> client.gui.screen() instanceof ConfirmScreen confirm
				&& confirm.getNarrationMessage().getString().contains(Component.translatable("entity.jugcraft.gathering_shade").getString())),
				"It names who leaves, in the server's words");
		shot(context, "jugcraft_roster_end_confirm");
		context.getInput().pressKey(InputConstants.KEY_ESCAPE);
		context.waitForScreen(RosterScreen.class);
		context.waitTicks(10);
		check(context.computeOnClient(client -> RosterClient.lastAnswer()) == heard
				&& server.computeOnServer(m -> BoundWills.of(m).ledger().get(setup.shade()) != null), "Esc keeps the agreement: nothing is sent");
		tabTo(context, "end:" + id);
		context.getInput().pressKey(InputConstants.KEY_RETURN);
		context.waitFor(client -> client.gui.screen() instanceof ConfirmScreen, WAIT);
		tabToLabel(context, Component.translatable("screen.jugcraft.roster.end.yes").getString());
		PactStatePayload.Result ended = answer(context);
		String after = server.computeOnServer(minecraft -> {
			List<Pacts.Departure> history = PactHistory.of(minecraft).history(player(minecraft).getUUID());
			return (BoundWills.of(minecraft).ledger().get(setup.shade()) == null) + " " + (minecraft.overworld().getEntity(setup.shadeBody()) == null)
					+ " " + (!history.isEmpty() && history.getLast().record().equals(setup.shade()) && Pacts.ENDED.equals(history.getLast().reason()));
		});
		Jugcraft.LOGGER.info("[roster-check] ended: {}; gone, departed, kept = {}", ended.reason(), after);
		check(ended.done() && "ended".equals(ended.reason()) && after.equals("true true true"),
				"Tab and Enter on End it end the shade's agreement once, it departs and the departure is kept: " + after);
		context.waitFor(client -> row(SHADE) == null, WAIT);
		check(context.computeOnClient(client -> screen(client).focusedKey().startsWith("row:")),
				"The keyboard stays on the roster, on the row now in the ended one's place");
		shot(context, "jugcraft_roster_ended");
	}

	private static void recoverAndCancel(ClientGameTestContext context, TestDedicatedServerContext server, Setup setup) {
		UUID strandedId = context.computeOnClient(client -> stranded().id());
		context.waitTicks(SETTLE);
		tabTo(context, "recover:" + strandedId);
		PactStatePayload.Result recovered = answer(context);
		String after = server.computeOnServer(m -> (CourierLedger.of(m).ledger().get(setup.stranded()) == null) + " "
				+ player(m).getInventory().countItem(Items.GLOWSTONE_DUST));
		Jugcraft.LOGGER.info("[roster-check] recovered: {}; closed, glowstone carried = {}", recovered.reason(), after);
		check(recovered.done() && "recovered".equals(recovered.reason()) && after.equals("true 2"),
				"Recover hands the two stranded glowstone to the player once and closes the request: " + after);
		context.waitFor(client -> stranded() == null, WAIT);
		shot(context, "jugcraft_roster_recovered");
		UUID openId = context.computeOnClient(client -> open().id());
		context.waitTicks(SETTLE);
		tabTo(context, "cancel:" + openId);
		PactStatePayload.Result cancelled = answer(context);
		check(cancelled.done() && "cancelled".equals(cancelled.reason())
				&& server.computeOnServer(m -> CourierLedger.of(m).ledger().get(setup.open()) == null), "Cancel closes the open request: " + cancelled.reason());
	}

	/** D-09: the workshop hand at work and the request it carries. */
	private record Work(UUID hand, long request) {
	}

	// D-09
	/**
	 * Concordance Chunk D, D-09 (08:169-181; D-P135): the workshop hand at work. Its holder files one request at the hand's
	 * post: three copper ingots, from a chest at one end of the yard to a chest at the other, both inside the hand's area.
	 * The hand claims it, takes the three and walks them over; while they are aboard the world is frozen, the ledger and the
	 * hand's synced status are read, and it is photographed from beside its path, aimed at, with the HUD shown (Jade's
	 * overlay of it when Jade is loaded) ({@code jugcraft_workshop_hand_at_work}). Thawed, it delivers all three once and the
	 * request closes, so the full roster below still files its sixteen.
	 */
	private static void handAtWork(ClientGameTestContext context, TestDedicatedServerContext server, BlockPos stand) {
		context.getInput().pressKey(InputConstants.KEY_ESCAPE);
		context.waitForScreen(null);
		BlockPos source = stand.offset(6, 0, 8);
		BlockPos destination = stand.offset(-1, 0, 8);
		Work work = server.computeOnServer(minecraft -> {
			ServerLevel level = minecraft.overworld();
			level.setBlockAndUpdate(source, Blocks.CHEST.defaultBlockState());
			level.setBlockAndUpdate(destination, Blocks.CHEST.defaultBlockState());
			((ChestBlockEntity) level.getBlockEntity(source)).setItem(0, new ItemStack(Items.COPPER_INGOT, 3));
			UUID hand = ((SpiritAnchorBlockEntity) level.getBlockEntity(stand.offset(3, 0, 3))).spirit();
			check(hand != null && level.getEntity(hand) instanceof WorkshopHandEntity, "The workshop hand serves at its anchor");
			Logistics.Filed filed = CourierLedger.of(minecraft).file(player(minecraft).getUUID(), Couriers.place(level, stand.offset(3, 0, 6)),
					Couriers.place(level, destination), new ItemStack(Items.COPPER_INGOT), 3, level.registryAccess(), level.getGameTime());
			check(filed.outcome() == Logistics.Outcome.DONE, "Its holder files three copper ingots at the hand's post: " + filed.outcome());
			return new Work(hand, filed.id());
		});
		server.waitFor(minecraft -> minecraft.overworld().getEntity(work.hand()) instanceof WorkshopHandEntity hand && hand.task() == work.request()
				&& hand.carriedCount() > 0 && hand.status() == Status.TRAVELLING, WORK_WAIT);
		server.runCommand("tick freeze");
		try {
			String carrying = server.computeOnServer(minecraft -> {
				ServerLevel level = minecraft.overworld();
				WorkshopHandEntity hand = (WorkshopHandEntity) level.getEntity(work.hand());
				Request request = CourierLedger.of(minecraft).ledger().get(work.request());
				boolean aboard = request != null && request.progress().aboard() && hand.getUUID().equals(request.progress().worker());
				return hand.carriedCount() + " " + aboard + " " + ((ChestBlockEntity) level.getBlockEntity(source)).countItem(Items.COPPER_INGOT);
			});
			BlockPos at = server.computeOnServer(minecraft -> minecraft.overworld().getEntity(work.hand()).blockPosition());
			server.runCommand(String.format(Locale.ROOT, "tp @a %.1f %d %.1f 0 0", at.getX() + 0.5, at.getY(), at.getZ() - 2.0));
			context.waitFor(client -> client.player.position().distanceTo(Vec3.atBottomCenterOf(at)) < 3.0 && seen(client, work.hand()) != null, WAIT);
			float[] aim = context.computeOnClient(client -> {
				WorkshopHandEntity hand = seen(client, work.hand());
				Vec3 eye = client.player.getEyePosition();
				Vec3 to = hand.position().add(0.0, hand.getBbHeight() * 0.5, 0.0);
				double dx = to.x - eye.x, dy = to.y - eye.y, dz = to.z - eye.z;
				return new float[] {(float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F,
						(float) -(Mth.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * Mth.RAD_TO_DEG)};
			});
			context.getInput().lookAt(aim[0], aim[1]);
			context.waitFor(client -> client.hitResult instanceof EntityHitResult hit && hit.getEntity().getUUID().equals(work.hand()), WAIT);
			boolean travelling = context.computeOnClient(client -> seen(client, work.hand()) instanceof WorkshopHandEntity hand
					&& hand.status() == Status.TRAVELLING);
			Jugcraft.LOGGER.info("[roster-check] the hand at work: carried, aboard on its own claim, left at the source = {}; seen travelling {}; Jade {}",
					carrying, travelling, FabricLoader.getInstance().isModLoaded("jade") ? "loaded" : "not loaded");
			check(carrying.equals("3 true 0") && travelling,
					"The hand carries the three aboard on its own claim, the source empty, and the client sees it travelling: " + carrying);
			context.waitTicks(10);
			shot(context, "jugcraft_workshop_hand_at_work");
		} finally {
			server.runCommand("tick unfreeze");
		}
		server.waitFor(minecraft -> CourierLedger.of(minecraft).ledger().get(work.request()) == null, WORK_WAIT);
		String delivered = server.computeOnServer(minecraft -> ((ChestBlockEntity) minecraft.overworld().getBlockEntity(destination)).countItem(Items.COPPER_INGOT)
				+ " " + ((ChestBlockEntity) minecraft.overworld().getBlockEntity(source)).countItem(Items.COPPER_INGOT));
		Jugcraft.LOGGER.info("[roster-check] the hand delivered: destination and source = {}", delivered);
		check(delivered.equals("3 0"), "It delivers all three once and the request closes: " + delivered);
		server.runCommand(String.format(Locale.ROOT, "tp @a %.1f %d %.1f 0 0", stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5));
		context.waitTicks(10);
		openByKey(context);
	}

	// D-09
	private static @Nullable WorkshopHandEntity seen(Minecraft client, UUID hand) {
		if (client.level == null || client.player == null) {
			return null;
		}
		List<WorkshopHandEntity> found = client.level.getEntitiesOfClass(WorkshopHandEntity.class, client.player.getBoundingBox().inflate(16.0),
				entity -> entity.getUUID().equals(hand));
		return found.isEmpty() ? null : found.getFirst();
	}

	private static void full(ClientGameTestContext context, TestDedicatedServerContext server, Setup setup) {
		server.runOnServer(minecraft -> fill(minecraft, setup));
		context.waitTicks(SETTLE);
		tabTo(context, "refresh");
		context.getInput().pressKey(InputConstants.KEY_RETURN);
		context.waitFor(client -> RosterClient.state() != null && RosterClient.state().rows().size() == RosterStatePayload.MAX_ROWS, WAIT);
		int more = context.computeOnClient(client -> RosterClient.state().more());
		Jugcraft.LOGGER.info("[roster-check] full: {} listed, {} more", RosterStatePayload.MAX_ROWS, more);
		check(more > 0 && context.computeOnClient(client -> screen(client).pages()) == RosterStatePayload.MAX_ROWS / RosterScreen.ROWS_PER_PAGE,
				"64 rows are listed on eight pages and the rest are counted: " + more);
		for (int i = 0; i < 7; i++) {
			context.getInput().pressKey(InputConstants.KEY_PAGEDOWN);
			context.waitTicks(2);
		}
		check(context.computeOnClient(client -> screen(client).page() == 7 && screen(client).control("more") != null),
				"Page Down reaches the last page, which says how many more there are");
		fits(context, "the full roster's last page");
		named(context);
		shot(context, "jugcraft_roster_full");
		int width = context.computeOnClient(client -> client.getWindow().getWidth());
		int height = context.computeOnClient(client -> client.getWindow().getHeight());
		smallest(context);
		fits(context, "the full roster in the smallest GUI");
		shot(context, "jugcraft_roster_full_small");
		context.getInput().resizeWindow(width, height);
		context.runOnClient(client -> {
			client.options.guiScale().set(2);
			client.resizeGui();
		});
		context.waitTicks(10);
		context.getInput().pressKey(InputConstants.KEY_ESCAPE);
		context.waitForScreen(null);
	}

	/** The roster filled past its 64 rows: 32 workers (not loaded), 32 Bound Wills (placeless records) and 16 open requests. */
	private static void fill(MinecraftServer minecraft, Setup setup) {
		ServerLevel level = minecraft.overworld();
		UUID holder = player(minecraft).getUUID();
		long now = level.getGameTime();
		WorkerRoster roster = WorkerRoster.of(minecraft);
		for (int i = 0; i < WorkerRoster.MAX_PER_PLAYER && roster.workers(holder).size() < WorkerRoster.MAX_PER_PLAYER; i++) {
			roster.note(holder, new UUID(0xD07L, 100L + i), "construct", Status.IDLE, level.dimension().identifier().toString(), setup.second().offset(i, 0, 40));
		}
		BoundWills wills = BoundWills.of(minecraft);
		for (int i = 0; i < BoundWillLedger.MAX_PER_HOLDER && wills.ledger().heldBy(holder).size() < BoundWillLedger.MAX_PER_HOLDER; i++) {
			wills.seal(new BoundWill(new UUID(0xD07L, 200L + i), SHADE, SHADE, holder, now, false));
		}
		CourierLedger ledger = CourierLedger.of(minecraft);
		Place post = Couriers.place(level, setup.second());
		Place to = Couriers.place(level, setup.destination());
		for (int i = 0; i < Logistics.MAX_OPEN_PER_PLAYER && ledger.ledger().openFor(holder) < Logistics.MAX_OPEN_PER_PLAYER; i++) {
			ledger.file(holder, post, to, new ItemStack(Items.REDSTONE), 1, level.registryAccess(), now);
		}
		check(roster.workers(holder).size() == WorkerRoster.MAX_PER_PLAYER && wills.ledger().heldBy(holder).size() == BoundWillLedger.MAX_PER_HOLDER
				&& ledger.ledger().openFor(holder) == Logistics.MAX_OPEN_PER_PLAYER, "32 workers, 32 Bound Wills and 16 open requests");
	}

	private static void openByKey(ClientGameTestContext context) {
		context.getInput().pressKey(RosterClient.key());
		context.waitForScreen(RosterScreen.class);
		context.waitFor(client -> RosterClient.state() != null, WAIT);
		context.waitTicks(3);
	}

	/** The smallest GUI Minecraft gives, as the screen fit check makes it: a 640x480 window at the automatic scale, 320x240. */
	private static void smallest(ClientGameTestContext context) {
		context.getInput().resizeWindow(ScreenKit.SMALLEST_GUI_WIDTH * 2, ScreenKit.SMALLEST_GUI_HEIGHT * 2);
		context.runOnClient(client -> {
			client.options.guiScale().set(0);
			client.resizeGui();
		});
		context.waitTicks(10);
		String gui = context.computeOnClient(client -> client.getWindow().getGuiScaledWidth() + "x" + client.getWindow().getGuiScaledHeight());
		check(gui.equals(ScreenKit.SMALLEST_GUI_WIDTH + "x" + ScreenKit.SMALLEST_GUI_HEIGHT), "The small window gives the smallest GUI: " + gui);
	}

	private static void tabTo(ClientGameTestContext context, String key) {
		for (int i = 0; i < 96; i++) {
			if (key.equals(context.computeOnClient(client -> client.gui.screen() instanceof RosterScreen screen ? screen.focusedKey() : ""))) {
				return;
			}
			context.getInput().pressKey(InputConstants.KEY_TAB);
			context.waitTicks(1);
		}
		throw new AssertionError("Tab never reached " + key);
	}

	private static void tabToLabel(ClientGameTestContext context, String label) {
		for (int i = 0; i < 8; i++) {
			if (context.computeOnClient(client -> focusedLabel(client).equals(label))) {
				return;
			}
			context.getInput().pressKey(InputConstants.KEY_TAB);
			context.waitTicks(1);
		}
		throw new AssertionError("Tab never reached " + label);
	}

	private static String focusedLabel(Minecraft client) {
		return client.gui.screen() != null && client.gui.screen().getFocused() instanceof AbstractWidget widget ? widget.getMessage().getString() : "";
	}

	/** Enter on the focused control, then the server's answer to it (never an earlier one). */
	private static PactStatePayload.Result answer(ClientGameTestContext context) {
		int before = context.computeOnClient(client -> RosterClient.lastAnswer());
		context.getInput().pressKey(InputConstants.KEY_RETURN);
		context.waitFor(client -> RosterClient.lastAnswer() > before && !RosterClient.waiting() && client.gui.screen() instanceof RosterScreen, WAIT);
		context.waitTicks(2);
		PactStatePayload.Result result = context.computeOnClient(client -> RosterClient.result());
		check(result != null, "The server answered");
		return result;
	}

	private static void fits(ClientGameTestContext context, String where) {
		List<String> outside = context.computeOnClient(client -> {
			RosterScreen screen = screen(client);
			List<String> out = new ArrayList<>(screen.outside());
			ScreenRectangle area = screen.panelArea();
			if (area.left() < 0 || area.top() < 0 || area.right() > screen.width || area.bottom() > screen.height) {
				out.add("panelArea");
			}
			return out;
		});
		check(outside.isEmpty(), "Every control of the roster is on screen (" + where + "): " + outside);
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

	private static List<UUID> unworded(Minecraft client) {
		List<UUID> out = new ArrayList<>();
		for (RosterStatePayload.Row row : RosterClient.state().rows()) {
			AbstractWidget card = screen(client).control("row:" + row.id());
			if (card == null || row.icon() < 0 || row.icon() >= RosterStatePayload.ICONS.size()) {
				continue;
			}
			String word = Component.translatable("screen.jugcraft.roster.state." + RosterStatePayload.ICONS.get(row.icon())).getString();
			if (!card.getMessage().getString().contains(word)) {
				out.add(row.id());
			}
		}
		return out;
	}

	private static RosterStatePayload.@Nullable Row row(String type) {
		RosterStatePayload state = RosterClient.state();
		if (state != null) {
			for (RosterStatePayload.Row row : state.rows()) {
				if (!row.request() && row.portrait().equals(type)) {
					return row;
				}
			}
		}
		return null;
	}

	private static RosterStatePayload.@Nullable Row stranded() {
		RosterStatePayload state = RosterClient.state();
		return state == null ? null : state.rows().stream().filter(row -> row.request() && row.icon() == RosterStatePayload.STRANDED).findFirst().orElse(null);
	}

	private static RosterStatePayload.@Nullable Row open() {
		RosterStatePayload state = RosterClient.state();
		return state == null ? null : state.rows().stream().filter(row -> row.request() && row.icon() != RosterStatePayload.STRANDED
				&& row.offers(RosterRequestPayload.CANCEL)).findFirst().orElse(null);
	}

	private static String describe(@Nullable RosterStatePayload state) {
		if (state == null) {
			return "no state";
		}
		List<String> rows = new ArrayList<>();
		for (RosterStatePayload.Row row : state.rows()) {
			String word = row.icon() >= 0 && row.icon() < RosterStatePayload.ICONS.size() ? RosterStatePayload.ICONS.get(row.icon()) : "?";
			rows.add(row.kind().getString() + " " + row.portrait() + " " + word + (row.loaded() ? " loaded" : "") + " actions " + row.actions()
					+ " (" + row.waiting().getString() + ")");
		}
		return (state.enabled() ? "on" : "off") + ", " + rows.size() + " rows, " + state.more() + " more: " + rows;
	}

	private static BoundWill.@Nullable Hold hold(MinecraftServer minecraft, UUID record) {
		BoundWill will = BoundWills.of(minecraft).ledger().get(record);
		return will == null || will.pact() == null ? null : will.pact().hold();
	}

	private static void shot(ClientGameTestContext context, String name) {
		context.runOnClient(client -> client.gui.toastManager().clear());
		context.waitTicks(2);
		context.takeScreenshot(name);
	}

	private static RosterScreen screen(Minecraft client) {
		return (RosterScreen) client.gui.screen();
	}

	private static ServerPlayer player(MinecraftServer minecraft) {
		return minecraft.getPlayerList().getPlayers().getFirst();
	}

	private static void check(boolean condition, String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}
}
