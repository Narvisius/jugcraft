package io.github.jimbozoomer.jugcraft.test;

import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.InputConstants;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.client.PactClient;
import io.github.jimbozoomer.jugcraft.client.PactScreen;
import io.github.jimbozoomer.jugcraft.client.RosterClient;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import io.github.jimbozoomer.jugcraft.concordance.sky.AstralClaims;
import io.github.jimbozoomer.jugcraft.concordance.sky.AstrolabeItem;
import io.github.jimbozoomer.jugcraft.concordance.sky.Sky;
import io.github.jimbozoomer.jugcraft.concordance.spirits.AgreementHolder;
import io.github.jimbozoomer.jugcraft.concordance.spirits.BoundWills;
import io.github.jimbozoomer.jugcraft.concordance.spirits.PactCompact;
import io.github.jimbozoomer.jugcraft.concordance.spirits.PactHistory;
import io.github.jimbozoomer.jugcraft.concordance.spirits.PactMenu;
import io.github.jimbozoomer.jugcraft.concordance.spirits.PactPerchBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.spirits.PactRequestPayload;
import io.github.jimbozoomer.jugcraft.concordance.spirits.PactStatePayload;
import io.github.jimbozoomer.jugcraft.concordance.spirits.Roster;
import io.github.jimbozoomer.jugcraft.concordance.spirits.RosterRequestPayload;
import io.github.jimbozoomer.jugcraft.concordance.spirits.StolasEntity;
import io.github.jimbozoomer.jugcraft.concordance.worker.PactDefinition;
import io.github.jimbozoomer.jugcraft.concordance.worker.Pacts;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.Stream;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.UserWhiteListEntry;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Concordance Chunk D, D-09 (D-S22, D-P136; E1, E5): two client JVMs at one pact perch. This client is the holder, on the
 * game's own dedicated server (Fabric's client game test API), opened on 127.0.0.1 only, offline, room for two. A second,
 * separate client JVM ("PactPeer", its own temporary profile under owner-network-evidence/, whose peer.log the Build
 * workflow's client shards upload) joins as a stranger, on this branch's own pattern, OwnerCollectionsNetworkTests (#435),
 * not #467's harness; the two coordinate through files. Results are logged with the prefix "[pact-network]".
 * <ul>
 * <li>The stranger, south of the perch facing Stolas, sees him arrive when the holder invites him
 * ({@code jugcraft_pact_peer_arrival}).</li>
 * <li>Used by the stranger, the holder's perch says only its owner may invite him and offers no Accept; the stranger's
 * Accept, sent all the same with the compact in hand, is refused as not permitted and spends nothing
 * ({@code jugcraft_pact_peer_refused}); the holder sees Stolas refuse, and their own conversation is untouched.</li>
 * <li>The holder accepts: one pact, theirs; the stranger sees him assent.</li>
 * <li>At the kept perch the stranger's End is refused as not the holder's; the stranger's roster lists nothing of the
 * holder's and refuses an End naming the holder's pact as not listed.</li>
 * <li>The holder ends it once; the stranger sees him depart ({@code jugcraft_pact_peer_departure}) and keeps all it
 * carried.</li>
 * </ul>
 * Two automated clients on one machine with offline names, no people: not the manual two-account check (docs/TESTING.md).
 */
public final class ConcordancePactNetworkTests implements FabricClientGameTest {
	private static final String PEER = "jugcraft.pactTestPeer";
	private static final String COORDINATION = "jugcraft.pactTestCoord";
	private static final String PEER_NAME = "PactPeer";
	private static final String STOLAS = "jugcraft:stolas";
	private static final String PERCH = "jugcraft:pact_perch";
	private static final int WAIT = 200;
	/** Client ticks the other JVM may take for a step: it starts, joins and renders on its own. */
	private static final int PEER_WAIT = 2400;

	@Override
	public void runTest(ClientGameTestContext context) {
		if (System.getProperty(PEER) != null) {
			peer(context);
			return;
		}
		Process peer = null;
		try {
			System.setProperty(COORDINATION, Path.of("owner-network-evidence", "pact-" + UUID.randomUUID()).toAbsolutePath().toString());
			Files.createDirectories(coordination());
			Properties properties = new Properties();
			properties.setProperty("server-ip", "127.0.0.1");
			properties.setProperty("online-mode", "false");
			properties.setProperty("view-distance", "4");
			properties.setProperty("simulation-distance", "4");
			properties.setProperty("max-players", "2");
			try (TestDedicatedServerContext server = context.worldBuilder().adjustSettings(settings -> {
				settings.setWorldType(new WorldCreationUiState.WorldTypeEntry(settings.getSettings().worldgenLoadContext()
						.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT)));
				settings.setGenerateStructures(false);
			}).createServer(properties)) {
				try (TestDedicatedServerConnection connection = server.connect()) {
					connection.waitForChunksRender();
					server.runCommand("time set noon");
					server.runCommand("weather clear");
					server.runCommand("gamerule minecraft:spawn_mobs false");
					context.waitTicks(20);
					BlockPos stand = context.computeOnClient(client -> client.player.blockPosition());
					BlockPos perch = stand.south(2);
					UUID holder = context.computeOnClient(client -> client.player.getUUID());
					server.runOnServer(minecraft -> prepare(minecraft, stand, perch, holder));
					server.runOnServer(minecraft -> minecraft.getPlayerList().getWhiteList().add(new UserWhiteListEntry(NameAndId.createOffline(PEER_NAME))));
					InProcessServerConfigs.beforeAdditionalClient(server);
					peer = launchPeer(server.computeOnServer(MinecraftServer::getPort));
					Process joining = peer;
					server.waitFor(minecraft -> {
						if (!joining.isAlive()) {
							throw failure("The second client exited before joining: " + joining.exitValue());
						}
						return minecraft.getPlayerList().getPlayerByName(PEER_NAME) != null;
					}, PEER_WAIT);
					server.runCommand("gamemode survival @a");
					String pocket = server.computeOnServer(minecraft -> {
						ServerPlayer stranger = stranger(minecraft);
						ready(minecraft, stranger);
						return pocket(stranger);
					});
					server.runCommand(String.format(Locale.ROOT, "tp %s %.1f %d %.1f 180 0", PEER_NAME, perch.getX() + 0.5, perch.getY(), perch.getZ() + 3.0));
					mark("perch.txt", Long.toString(perch.asLong()));
					mark("ready", "");
					awaitPeer(context, peer, "watching");
					// The holder invites him (their perch's own use, as ConcordancePactClientGameTests opens it).
					int before = context.computeOnClient(client -> client.player.containerMenu.containerId);
					server.runOnServer(minecraft -> use(minecraft, minecraft.getPlayerList().getPlayer(holder), perch));
					context.waitFor(client -> client.gui.screen() instanceof PactScreen screen && screen.getMenu().containerId != before
							&& PactClient.state() != null && PactClient.state().mode() == PactStatePayload.NEGOTIATE
							&& PactClient.state().offers(PactRequestPayload.ACCEPT), WAIT);
					awaitPeer(context, peer, "saw-arrival");
					// The stranger uses the holder's perch, and asks to accept all the same.
					server.runOnServer(minecraft -> use(minecraft, stranger(minecraft), perch));
					awaitPeer(context, peer, "peer-refused");
					context.waitFor(client -> StolasEntity.Gesture.REFUSE.animation.equals(gestured(client, perch)), WAIT);
					String untouched = server.computeOnServer(minecraft -> held(minecraft, holder) + " "
							+ BoundWills.of(minecraft).ledger().heldBy(stranger(minecraft).getUUID()).size() + " " + pocket(stranger(minecraft)).equals(pocket));
					Jugcraft.LOGGER.info("[pact-network] the stranger's Accept: {}; holder holds, stranger's records, stranger's pocket kept = {}",
							read("accept.txt"), untouched);
					check(untouched.equals("false 0 true") && context.computeOnClient(client -> PactClient.state() != null
							&& PactClient.state().mode() == PactStatePayload.NEGOTIATE && PactClient.state().offers(PactRequestPayload.ACCEPT)),
							"The stranger's Accept sealed and spent nothing, and the holder's conversation is untouched: " + untouched);
					PactStatePayload.Result sealed = ask(context, PactRequestPayload.ACCEPT);
					check(sealed.done() && PactMenu.SEALED.equals(sealed.reason()), "The holder's Accept seals the pact: " + sealed.reason());
					UUID record = server.computeOnServer(minecraft -> Pacts.pactWith(BoundWills.of(minecraft).ledger(), holder, STOLAS).id());
					int escrow = server.computeOnServer(minecraft -> escrow(minecraft, record));
					mark("record.txt", record.toString());
					mark("sealed", "");
					awaitPeer(context, peer, "saw-assent");
					// The stranger at the kept perch: its End there, and from its roster.
					server.runOnServer(minecraft -> use(minecraft, stranger(minecraft), perch));
					awaitPeer(context, peer, "peer-ended-nothing");
					String kept = server.computeOnServer(minecraft -> held(minecraft, holder) + " " + escrow(minecraft, record) + " "
							+ (((PactPerchBlockEntity) minecraft.overworld().getBlockEntity(perch)).stolas(minecraft.overworld()) != null) + " "
							+ BoundWills.of(minecraft).ledger().heldBy(stranger(minecraft).getUUID()).size());
					Jugcraft.LOGGER.info("[pact-network] the stranger's End and roster End: {}; held, escrow, Stolas on his perch, stranger's records = {}",
							read("ended.txt"), kept);
					check(kept.equals("true " + escrow + " true 0"), "The stranger ended nothing and named nothing of the holder's: " + kept);
					PactStatePayload.Result ended = ask(context, PactRequestPayload.END);
					String after = server.computeOnServer(minecraft -> {
						List<Pacts.Departure> history = PactHistory.of(minecraft).history(holder);
						return held(minecraft, holder) + " " + (!history.isEmpty() && history.getLast().record().equals(record)
								&& Pacts.ENDED.equals(history.getLast().reason())) + " " + pocket(stranger(minecraft)).equals(pocket);
					});
					Jugcraft.LOGGER.info("[pact-network] the holder ends it: {}; held, departure kept, stranger's pocket kept = {}", ended.reason(), after);
					check(ended.done() && Pacts.ENDED.equals(ended.reason()) && after.equals("false true true"), "The holder ends it once: " + after);
					mark("ended", "");
					awaitPeer(context, peer, "peer-finished");
					Process leaving = peer;
					context.waitFor(client -> !leaving.isAlive(), PEER_WAIT);
					if (peer.exitValue() != 0) {
						throw failure("The second client failed: " + peer.exitValue());
					}
					context.getInput().pressKey(InputConstants.KEY_ESCAPE);
					context.waitForScreen(null);
				}
			}
			mark("passed", "Two client JVMs at one perch: the stranger was refused Accept, End and a roster End; the holder sealed and ended once.");
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		} finally {
			if (peer != null && peer.isAlive()) {
				peer.destroy();
			}
		}
	}

	/** The second JVM: the stranger. Each step waits for the holder's mark and leaves its own. */
	private static void peer(ClientGameTestContext context) {
		mark("peer-entered", "");
		String address = "127.0.0.1:" + System.getProperty(PEER);
		context.runOnClient(client -> ConnectScreen.startConnecting(new TitleScreen(), client, ServerAddress.parseString(address),
				new ServerData("Pact disposable test", address, ServerData.Type.OTHER), false, null));
		context.waitFor(client -> {
			if (client.gui.screen() instanceof DisconnectedScreen) {
				throw new AssertionError("The second client was disconnected while joining; see peer.log");
			}
			return client.level != null && client.player != null;
		}, PEER_WAIT);
		awaitMark(context, "ready");
		BlockPos perch = BlockPos.of(Long.parseLong(read("perch.txt")));
		context.waitFor(client -> client.player.blockPosition().closerThan(perch, 4.0), WAIT);
		context.runOnClient(client -> {
			if (!client.gui.hud.isHidden()) {
				client.gui.hud.toggle();
			}
		});
		// Its world drawn round the perch before it says it is watching (a predicate, never a fixed wait).
		Block perchBlock = BuiltInRegistries.BLOCK.getValue(Identifier.parse(PERCH));
		context.waitFor(client -> client.level.getBlockState(perch).is(perchBlock) && client.levelRenderer.hasRenderedAllSections(), PEER_WAIT);
		// "Loading terrain" closes once this client has compiled its own section (or after the game's own 30 s): the arrival
		// is seen over the world, not under the loading screen (run 40133540741's picture was the loading screen).
		context.waitFor(client -> client.gui.screen() == null, PEER_WAIT);
		mark("watching", "");
		context.waitFor(client -> StolasEntity.Gesture.ARRIVE.animation.equals(gestured(client, perch)), PEER_WAIT);
		shot(context, "jugcraft_pact_peer_arrival");
		mark("saw-arrival", "");
		context.waitFor(client -> client.gui.screen() instanceof PactScreen && PactClient.state() != null, PEER_WAIT);
		String owner = context.computeOnClient(client -> Component.translatable("message.jugcraft.concordance.pact.missing.owner").getString());
		String shown = context.computeOnClient(client -> PactClient.state().mode() + " " + PactClient.state().offers(PactRequestPayload.ACCEPT) + " "
				+ PactClient.state().notes().stream().anyMatch(note -> note.getString().equals(owner)));
		check(shown.equals(PactStatePayload.NEGOTIATE + " false true"),
				"The stranger is told only the perch's owner may invite him, and is offered no Accept: " + shown);
		// His terms first: turning to them asks to hear them (CHOOSE), and that answer's state carries no result, so the Accept
		// is sent after it has come, and its refusal is the answer shown under them (run 40137136287 turned after, and lost it).
		PactStatePayload beforeTerms = context.computeOnClient(client -> PactClient.state());
		tabTo(context, "next");
		for (int beat = 0; beat < 3; beat++) {
			context.getInput().pressKey(InputConstants.KEY_RETURN);
			context.waitTicks(3);
		}
		context.waitFor(client -> client.gui.screen() instanceof PactScreen screen && screen.page() == PactScreen.TERMS_PAGE
				&& PactClient.state() != null && PactClient.state() != beforeTerms, WAIT);
		PactStatePayload.Result accept = ask(context, PactRequestPayload.ACCEPT);
		mark("accept.txt", accept.reason());
		check(!accept.done() && Pacts.Outcome.NOT_PERMITTED.id.equals(accept.reason()),
				"The stranger's Accept, sent all the same, is refused as not permitted: " + accept.reason());
		// The refusal, under his terms, brought into view by the keyboard before the picture.
		tabTo(context, "result");
		check(context.computeOnClient(client -> client.gui.screen() instanceof PactScreen screen && screen.page() == PactScreen.TERMS_PAGE
				&& screen.control("result") instanceof PactScreen.Verdict verdict && verdict.refused()),
				"The stranger's screen shows the refusal under his terms");
		shot(context, "jugcraft_pact_peer_refused");
		context.getInput().pressKey(InputConstants.KEY_ESCAPE);
		context.waitForScreen(null);
		mark("peer-refused", "");
		awaitMark(context, "sealed");
		context.waitFor(client -> StolasEntity.Gesture.ASSENT.animation.equals(gestured(client, perch)), WAIT);
		mark("saw-assent", "");
		context.waitFor(client -> client.gui.screen() instanceof PactScreen && PactClient.state() != null, PEER_WAIT);
		String taken = context.computeOnClient(client -> Component.translatable("message.jugcraft.concordance.pact.reading.taken").getString());
		check(context.computeOnClient(client -> PactClient.state().mode() == PactStatePayload.CLOSED && !PactClient.state().offers(PactRequestPayload.END)
				&& PactClient.state().notes().stream().anyMatch(note -> note.getString().equals(taken))),
				"The kept perch tells the stranger it is taken, and offers no End");
		PactStatePayload.Result end = ask(context, PactRequestPayload.END);
		check(!end.done() && AgreementHolder.Ended.NOT_HOLDER.name().toLowerCase(Locale.ROOT).equals(end.reason()),
				"The stranger's End is refused as not the holder's: " + end.reason());
		context.getInput().pressKey(InputConstants.KEY_ESCAPE);
		context.waitForScreen(null);
		UUID record = UUID.fromString(read("record.txt"));
		context.runOnClient(client -> RosterClient.refresh());
		context.waitFor(client -> RosterClient.state() != null, WAIT);
		check(context.computeOnClient(client -> RosterClient.state().rows().stream().noneMatch(row -> row.id().equals(record))),
				"The stranger's roster lists nothing of the holder's");
		int heard = context.computeOnClient(client -> RosterClient.lastAnswer());
		check(context.computeOnClient(client -> RosterClient.request(RosterRequestPayload.END, record, 0)) > 0, "The stranger's roster End is sent");
		context.waitFor(client -> RosterClient.lastAnswer() > heard && !RosterClient.waiting(), WAIT);
		PactStatePayload.Result named = context.computeOnClient(client -> RosterClient.result());
		check(named != null && !named.done() && Roster.NOT_LISTED.equals(named.reason()),
				"A roster End naming the holder's pact is refused as not listed: " + (named == null ? "no answer" : named.reason()));
		mark("ended.txt", end.reason() + " " + named.reason());
		mark("peer-ended-nothing", "");
		context.waitFor(client -> StolasEntity.Gesture.DEPART.animation.equals(gestured(client, perch)), PEER_WAIT);
		shot(context, "jugcraft_pact_peer_departure");
		context.waitFor(client -> stolas(client, perch) == null, WAIT);
		mark("peer-finished", "");
		context.runOnClient(client -> {
			client.disconnectWithSavingScreen();
			client.gui.setScreen(new TitleScreen());
		});
		context.waitForScreen(TitleScreen.class);
	}

	/** The second JVM's own client entrypoint (its manifest names only this test): it says it started, for peer.log. */
	public static final class PeerStartup implements ClientModInitializer {
		@Override
		public void onInitializeClient() {
			Jugcraft.LOGGER.info("[pact-network] second client: test mode {}, port {}", System.getProperty("fabric.client.gametest"),
					System.getProperty(PEER));
			mark("peer-initialized", "");
		}
	}

	/** Open stone round the holder's place, their perch two blocks south, and them ready to invite him, the compact in hand. */
	private static void prepare(MinecraftServer minecraft, BlockPos stand, BlockPos perch, UUID holder) {
		check(BuiltInRegistries.BLOCK.containsKey(Identifier.parse(PERCH)), "The pact perch is registered");
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
		check(level.getBlockEntity(perch) instanceof AgreementHolder.Holds, "The perch keeps its pact through the shared holder");
		((AgreementHolder.Holds) level.getBlockEntity(perch)).holder().setOwner(holder);
		ready(minecraft, minecraft.getPlayerList().getPlayer(holder));
	}

	/** Stolas's Regard understood, two patterns observed, a full astrolabe and the compact's materials. */
	private static void ready(MinecraftServer minecraft, ServerPlayer player) {
		ConcordanceProgress.grant(player, "jugcraft:first_light", ResearchState.UNDERSTOOD);
		ConcordanceProgress.grant(player, "jugcraft:binding_arts", ResearchState.UNDERSTOOD);
		ConcordanceProgress.grant(player, "jugcraft:stolas_regard", ResearchState.UNDERSTOOD);
		long now = minecraft.overworld().getGameTime();
		AstralClaims claims = AstralClaims.of(minecraft);
		claims.observe(player.getUUID(), "jugcraft:full_moon", now);
		claims.observe(player.getUUID(), "jugcraft:lantern_star", now);
		player.getInventory().clearContent();
		ItemStack astrolabe = new ItemStack(Sky.ASTROLABE);
		AstrolabeItem.setCharge(astrolabe, AstrolabeItem.CAPACITY);
		player.getInventory().setItem(0, astrolabe);
		for (PactCompact.Need need : needs()) {
			player.getInventory().add(new ItemStack(need.item(), need.count()));
		}
	}

	private static List<PactCompact.Need> needs() {
		PactDefinition definition = PactPerchBlockEntity.definition();
		List<PactCompact.Need> needs = definition == null ? null : PactCompact.needs(definition);
		check(needs != null && !needs.isEmpty(), "Stolas's offer and its compact are loaded");
		return needs;
	}

	/** What a player carries of the compact, and their astrolabe's resonance. */
	private static String pocket(ServerPlayer player) {
		List<String> counts = new ArrayList<>();
		for (PactCompact.Need need : needs()) {
			counts.add(BuiltInRegistries.ITEM.getKey(need.item()) + "=" + player.getInventory().countItem(need.item()));
		}
		Inventory inventory = player.getInventory();
		int charge = -1;
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			if (inventory.getItem(slot).is(Sky.ASTROLABE)) {
				charge = AstrolabeItem.charge(inventory.getItem(slot));
			}
		}
		counts.add("resonance=" + charge);
		return String.join(" ", counts);
	}

	private static void use(MinecraftServer minecraft, ServerPlayer player, BlockPos perch) {
		ServerLevel level = minecraft.overworld();
		level.getBlockState(perch).useWithoutItem(level, player, new BlockHitResult(Vec3.atCenterOf(perch), Direction.NORTH, perch, false));
	}

	private static ServerPlayer stranger(MinecraftServer minecraft) {
		ServerPlayer player = minecraft.getPlayerList().getPlayerByName(PEER_NAME);
		check(player != null, "The second client is on the server");
		return player;
	}

	private static boolean held(MinecraftServer minecraft, UUID holder) {
		return Pacts.holds(BoundWills.of(minecraft).ledger(), holder, STOLAS);
	}

	private static int escrow(MinecraftServer minecraft, UUID record) {
		return BoundWills.of(minecraft).escrow(record).stream().mapToInt(ItemStack::getCount).sum();
	}

	/** Sends {@code action} for the pact menu this client has open, then waits for the server's answer to it. */
	private static PactStatePayload.Result ask(ClientGameTestContext context, int action) {
		int before = context.computeOnClient(client -> PactClient.lastAnswer());
		int sent = context.computeOnClient(client -> client.player.containerMenu instanceof PactMenu menu ? PactClient.request(menu, action, 0) : -1);
		check(sent > 0, "The request was sent");
		context.waitFor(client -> PactClient.lastAnswer() > before && !PactClient.waiting() && PactClient.state() != null, WAIT);
		return context.computeOnClient(client -> PactClient.state().result().orElseThrow());
	}

	private static @Nullable StolasEntity stolas(Minecraft client, BlockPos perch) {
		if (client.level == null) {
			return null;
		}
		List<StolasEntity> found = client.level.getEntitiesOfClass(StolasEntity.class, new AABB(perch).inflate(4));
		return found.isEmpty() ? null : found.getFirst();
	}

	/** The gesture last triggered on him, as this client saw it (it stays set after it plays). */
	private static @Nullable RawAnimation gestured(Minecraft client, BlockPos perch) {
		StolasEntity stolas = stolas(client, perch);
		if (stolas == null) {
			return null;
		}
		AnimatableManager<StolasEntity> manager = stolas.getAnimatableInstanceCache().getManagerForId(stolas.getId());
		AnimationController<StolasEntity> gestures = manager.getAnimationControllers().get(StolasEntity.GESTURE_CONTROLLER);
		return gestures == null ? null : gestures.getCurrentRawAnimation();
	}

	/** Tab until the pact screen's focused control is {@code key} (as the pact client test does). */
	private static void tabTo(ClientGameTestContext context, String key) {
		for (int i = 0; i < 64; i++) {
			if (key.equals(context.computeOnClient(client -> client.gui.screen() instanceof PactScreen screen ? screen.focusedKey() : ""))) {
				return;
			}
			context.getInput().pressKey(InputConstants.KEY_TAB);
			context.waitTicks(1);
		}
		throw new AssertionError("Tab never reached " + key);
	}

	/** A picture with no toast over it (the peer's own profile keeps it, under owner-network-evidence/). */
	private static void shot(ClientGameTestContext context, String name) {
		context.runOnClient(client -> client.gui.toastManager().clear());
		context.waitTicks(2);
		context.takeScreenshot(name);
	}

	private static void awaitPeer(ClientGameTestContext context, Process peer, String name) {
		context.waitFor(client -> {
			if (marked(name)) {
				return true;
			}
			if (!peer.isAlive()) {
				throw failure("The second client exited (" + peer.exitValue() + ") before " + name);
			}
			return false;
		}, PEER_WAIT);
	}

	private static void awaitMark(ClientGameTestContext context, String name) {
		context.waitFor(client -> marked(name), PEER_WAIT);
	}

	private static Path coordination() {
		return Path.of(System.getProperty(COORDINATION, "owner-network-evidence")).toAbsolutePath();
	}

	private static void mark(String name, String text) {
		try {
			Files.createDirectories(coordination());
			Files.writeString(coordination().resolve(name), text);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static boolean marked(String name) {
		return Files.exists(coordination().resolve(name));
	}

	private static String read(String name) {
		try {
			return Files.readString(coordination().resolve(name)).trim();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static AssertionError failure(String message) {
		ArrayDeque<String> tail = new ArrayDeque<>();
		try (Stream<String> lines = Files.lines(coordination().resolve("peer.log"))) {
			lines.forEach(line -> {
				if (tail.size() == 120) {
					tail.removeFirst();
				}
				tail.addLast(line);
			});
		} catch (IOException | UncheckedIOException e) {
			tail.addLast("The second client's log could not be read: " + e);
		}
		return new AssertionError(message + "\nThe second client's log, last lines:\n" + String.join("\n", tail));
	}

	/** OwnerCollectionsNetworkTests.launchPeer (#435), with this class's names: a second client JVM on the same classpath. */
	private static Process launchPeer(int port) throws IOException {
		Path profile = coordination().resolve("peer-profile");
		Files.createDirectories(profile);
		Files.writeString(profile.resolve("options.txt"), "preferredGraphicsBackend:\"opengl\"\nfullscreen:false\nlang:en_us\n");
		Path metadata = FabricLoader.getInstance().getModContainer("jugcraft-test").orElseThrow().findPath("fabric.mod.json").orElseThrow();
		Path resources = coordination().resolve("peer-test-resources");
		Files.createDirectories(resources);
		try (Stream<Path> paths = Files.walk(metadata.getParent())) {
			for (Path file : paths.filter(Files::isRegularFile).toList()) {
				Path target = resources.resolve(metadata.getParent().relativize(file));
				Files.createDirectories(target.getParent());
				Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING);
			}
		}
		JsonObject json = JsonParser.parseString(Files.readString(metadata)).getAsJsonObject();
		JsonArray tests = new JsonArray();
		tests.add(ConcordancePactNetworkTests.class.getName());
		json.getAsJsonObject("entrypoints").add("fabric-client-gametest", tests);
		JsonArray client = new JsonArray();
		client.add(ConcordancePactNetworkTests.class.getName() + "$PeerStartup");
		json.getAsJsonObject("entrypoints").add("client", client);
		Files.writeString(resources.resolve("fabric.mod.json"), json.toString());
		String oldResources = metadata.getParent().toAbsolutePath().toString();
		Path config = Path.of(System.getProperty("user.dir")).toAbsolutePath();
		while (config != null && !Files.isRegularFile(config.resolve(".gradle/loom-cache/launch.cfg"))) {
			config = config.getParent();
		}
		if (config == null) {
			throw new IllegalStateException("Cannot locate this checkout's Loom launch.cfg");
		}
		List<String> launch = Files.readAllLines(config.resolve(".gradle/loom-cache/launch.cfg"));
		String assets = null;
		String assetIndex = null;
		for (int i = 0; i + 1 < launch.size(); i++) {
			if (launch.get(i).trim().equals("--assetsDir")) {
				assets = launch.get(i + 1).trim();
			}
			if (launch.get(i).trim().equals("--assetIndex")) {
				assetIndex = launch.get(i + 1).trim();
			}
		}
		if (assets == null || assetIndex == null) {
			throw new IllegalStateException("Loom's launch config lacks asset paths");
		}
		List<String> command = new ArrayList<>();
		command.add(Path.of(System.getProperty("java.home"), "bin", System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java").toString());
		command.add("-Xmx2G");
		command.add("-XX:ActiveProcessorCount=4");
		command.add("-XX:ErrorFile=" + coordination().resolve("hs_err_peer.log"));
		for (Map.Entry<Object, Object> entry : System.getProperties().entrySet()) {
			String key = entry.getKey().toString();
			if (key.startsWith("fabric.") || key.startsWith("fabric-api.") || key.startsWith("org.lwjgl.") || key.equals("java.library.path")
					|| key.equals("log4j.configurationFile")) {
				command.add("-D" + key + "=" + entry.getValue().toString().replace(oldResources, resources.toString()));
			}
		}
		command.add("-D" + PEER + "=" + port);
		command.add("-D" + COORDINATION + "=" + coordination());
		command.add("-cp");
		command.add(System.getProperty("java.class.path").replace(oldResources, resources.toString()));
		command.add("net.fabricmc.loader.impl.launch.knot.KnotClient");
		command.addAll(List.of("--username", PEER_NAME, "--version", "26.3", "--accessToken", "0", "--gameDir", profile.toString(),
				"--assetIndex", assetIndex, "--assetsDir", assets));
		Path arguments = coordination().resolve("peer-java.args");
		Files.write(arguments, command.subList(1, command.size()).stream().map(a -> "\"" + a.replace("\\", "\\\\").replace("\"", "\\\"") + "\"").toList());
		return new ProcessBuilder(command.getFirst(), "@" + arguments).directory(profile.toFile()).redirectErrorStream(true)
				.redirectOutput(coordination().resolve("peer.log").toFile()).start();
	}

	private static void check(boolean condition, String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}
}
