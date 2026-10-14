package io.github.jimbozoomer.jugcraft.test;

import com.zigythebird.playeranim.animation.PlayerAnimationController;
import com.zigythebird.playeranim.api.PlayerAnimationAccess;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.client.CastPoses;
import io.github.jimbozoomer.jugcraft.client.JournalClient;
import io.github.jimbozoomer.jugcraft.client.VigilClient;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import io.github.jimbozoomer.jugcraft.concordance.Examination;
import io.github.jimbozoomer.jugcraft.concordance.JugcraftConcordance;
import io.github.jimbozoomer.jugcraft.concordance.rules.FocusPool;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import io.github.jimbozoomer.jugcraft.concordance.vigil.VigilGesturePayload;
import io.github.jimbozoomer.jugcraft.guns.JugcraftGuns;
import io.github.jimbozoomer.jugcraft.weapons.JugcraftArms;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.spell_engine.api.spell.registry.SpellRegistry;
import net.spell_engine.client.animation.AnimatablePlayer;
import net.spell_engine.client.animation.SpellAnimationStack;
import net.spell_engine.internals.casting.SpellCast;
import net.spell_engine.internals.casting.SpellCaster;

/**
 * Roadmap step 31 in a real client: the first success by ordinary controls (docs/features/arcane-concordance-journey.md).
 * <p>
 * A fresh world, and a closed stone room with no light in it. The hotbar holds three luminous specimens and an
 * Initiate's Wand; the specimens and the wand are given, not found or crafted, so this tests the controls, not the
 * search. Only keys are pressed, as a player would press them:
 * <ul>
 * <li>a number key picks each specimen, and sneak with use examines it. Three different specimens examined in the dark
 * understand First Light, which teaches Aegis, Kindle and Revelation;</li>
 * <li>2 picks the wand. With it held, Spell Engine's spell bar puts the first invocation on the use key and the next on
 * the number key 2. Holding each casts it once: both are cast, Focus is spent, the wand is still in hand (2 cast
 * rather than changed the slot), and the Kindled light lights the room (screenshot
 * {@code jugcraft_concordance_journey_first_light});</li>
 * <li>J opens the journal (screenshot {@code jugcraft_concordance_journey_journal}).</li>
 * </ul>
 * Then, in a second world, Concordance Chunk A's ticket A-07: a cast's pose gives way to a gun or an arm
 * ({@link #aCastGivesWayToAGunOrAnArm}). CI job {@code client}, with the optional integrations installed.
 */
public class ConcordanceJourneyClientGameTests implements FabricClientGameTest {
	private static final List<String> FIRST_INVOCATIONS = List.of("jugcraft:aegis", "jugcraft:kindle", "jugcraft:revelation");

	@Override
	public void runTest(ClientGameTestContext context) {
		theFirstSuccessByOrdinaryControls(context);
		aCastGivesWayToAGunOrAnArm(context);
	}

	public void theFirstSuccessByOrdinaryControls(ClientGameTestContext context) {
		try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
			singleplayer.getConnection().waitForChunksRender();
			TestServerContext server = singleplayer.getServer();
			server.runCommand("gamerule minecraft:send_command_feedback false");
			server.runCommand("gamerule minecraft:spawn_mobs false");
			server.runCommand("time set 6000");
			BlockPos origin = context.computeOnClient(client -> BlockPos.containing(client.player.position()));
			int x = origin.getX();
			int y = origin.getY();
			int z = origin.getZ();
			// A closed room five blocks across inside: no sky and no lamp, so it is dark at noon.
			server.runCommand(String.format(Locale.ROOT, "fill %d %d %d %d %d %d minecraft:stone hollow", x - 3, y - 1, z - 3, x + 3, y + 4, z + 3));
			server.runOnServer(minecraft -> {
				ServerPlayer player = minecraft.getPlayerList().getPlayers().get(0);
				player.getInventory().clearContent();
				player.getInventory().setItem(0, new ItemStack(Items.AMETHYST_SHARD));
				player.getInventory().setItem(1, new ItemStack(JugcraftConcordance.INITIATE_WAND));
				player.getInventory().setItem(2, new ItemStack(Items.GLOWSTONE_DUST));
				player.getInventory().setItem(3, new ItemStack(Items.GLOW_INK_SAC));
				Examination.forget(player.getUUID());
			});
			// Facing north, at the wall three blocks away.
			server.runCommand(String.format(Locale.ROOT, "tp @p %.1f %d %.1f 180 0", x + 0.5, y, z + 0.5));
			context.waitTicks(40);

			// Each specimen: its number key, then sneak and use.
			for (int slot : new int[] {0, 2, 3}) {
				context.getInput().pressKey(options -> options.keyHotbarSlots[slot]);
				context.waitTicks(2);
				context.getInput().holdKey(options -> options.keyShift);
				context.waitTicks(2);
				context.getInput().pressKey(options -> options.keyUse);
				context.waitTicks(2);
				context.getInput().releaseKey(options -> options.keyShift);
				context.waitTicks(Examination.COOLDOWN_TICKS + 2);
			}
			ResearchState firstLight = onServer(singleplayer, player -> ConcordanceProgress.knowledge(player).state("jugcraft:first_light"));
			if (firstLight != ResearchState.UNDERSTOOD) {
				int light = onServer(singleplayer, player -> player.level().getMaxLocalRawBrightness(BlockPos.containing(player.getEyePosition())));
				throw new AssertionError("Three specimens examined by sneak and use, in light " + light + ", left First Light " + firstLight);
			}

			// The wand: 2 picks it. The use key casts the first invocation on the spell bar, the number key 2 the next.
			context.getInput().pressKey(options -> options.keyHotbarSlots[1]);
			context.waitTicks(20);
			context.getInput().holdKey(options -> options.keyUse);
			context.waitTicks(24);
			context.getInput().releaseKey(options -> options.keyUse);
			context.waitTicks(10);
			List<String> afterUse = cast(singleplayer);
			context.getInput().holdKey(options -> options.keyHotbarSlots[1]);
			context.waitTicks(24);
			context.getInput().releaseKey(options -> options.keyHotbarSlots[1]);
			context.waitTicks(10);
			List<String> afterTwo = cast(singleplayer);
			int focus = onServer(singleplayer, ConcordanceProgress::currentFocus);
			boolean held = onServer(singleplayer, player -> player.getMainHandItem().is(JugcraftConcordance.INITIATE_WAND));
			if (afterUse.size() != 1 || afterTwo.size() != 2 || !afterTwo.containsAll(afterUse) || focus >= FocusPool.MAX || !held) {
				throw new AssertionError("Holding use, then 2, with the wand should cast two invocations: after use " + afterUse
						+ ", after 2 " + afterTwo + ", Focus " + focus + ", wand still held " + held);
			}
			if (afterTwo.contains("jugcraft:kindle") && !onServer(singleplayer, player -> lit(player, origin))) {
				throw new AssertionError("Kindle was cast but no Kindled light is in the room");
			}
			Jugcraft.LOGGER.info("Concordance journey: the use key cast {}, the number key 2 {}; Focus left {}", afterUse,
					afterTwo.stream().filter(id -> !afterUse.contains(id)).toList(), focus);
			context.takeScreenshot("jugcraft_concordance_journey_first_light");

			// J opens the journal.
			context.getInput().pressKey(options -> JournalClient.key());
			context.waitFor(client -> JournalClient.received() && client.gui.screen() != null);
			context.waitTicks(10);
			context.takeScreenshot("jugcraft_concordance_journey_journal");
			context.setScreen(() -> null);
		}
	}

	/**
	 * Concordance Chunk A, ticket A-07 (docs/features/arcane-concordance-presentation.md): a cast's pose never plays over
	 * a gun or an arm in the main hand ({@link CastPoses}). The hotbar holds the Initiate's Wand (1), a Longhorn Rifle (2)
	 * and a Steel Longsword (3). Kindle's release clip is started on this client's player the way Spell Engine's own
	 * handler starts it when the server's release arrives ({@link AnimatablePlayer#playSpellAnimation}):
	 * <ul>
	 * <li>with the wand in hand it plays on, laid over the player by Player Animation Library;</li>
	 * <li>2 takes the rifle part way through: by the next tick the clip has stopped, nothing is laid over the player (so
	 * nothing replaces the first-person hands and gun with the gun drawn at the clip's posed hand) and nothing starts it
	 * again (screenshot {@code jugcraft_concordance_cast_pose_gun});</li>
	 * <li>a release that arrives with the rifle already in hand is stopped the same way, seen from the front
	 * ({@code jugcraft_concordance_cast_pose_gun_third_person}), and so is one cut short by 3 taking the longsword
	 * ({@code jugcraft_concordance_cast_pose_arm_third_person});</li>
	 * <li>the Vigil's offering gesture, sent by the server as when an offering is made, does not start on a player holding
	 * the rifle, and one playing with the wand in hand stops when 2 takes the rifle.</li>
	 * </ul>
	 * One client cannot watch another, so the same rule for other players this client sees is not tested here.
	 */
	public void aCastGivesWayToAGunOrAnArm(ClientGameTestContext context) {
		try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
			singleplayer.getConnection().waitForChunksRender();
			TestServerContext server = singleplayer.getServer();
			server.runCommand("gamerule minecraft:send_command_feedback false");
			server.runCommand("gamerule minecraft:spawn_mobs false");
			server.runCommand("time set noon");
			server.runCommand("weather clear");
			BlockPos origin = context.computeOnClient(client -> BlockPos.containing(client.player.position()));
			int x = origin.getX();
			int y = origin.getY();
			int z = origin.getZ();
			server.runCommand(String.format(Locale.ROOT, "fill %d %d %d %d %d %d minecraft:smooth_stone", x - 6, y - 1, z - 6, x + 6, y - 1, z + 6));
			server.runCommand(String.format(Locale.ROOT, "fill %d %d %d %d %d %d minecraft:air", x - 6, y, z - 6, x + 6, y + 5, z + 6));
			server.runCommand(String.format(Locale.ROOT, "tp @p %.1f %d %.1f 180 0", x + 0.5, y, z + 0.5));
			server.runOnServer(minecraft -> {
				ServerPlayer player = minecraft.getPlayerList().getPlayers().get(0);
				player.getInventory().clearContent();
				player.getInventory().setItem(0, new ItemStack(JugcraftConcordance.INITIATE_WAND));
				player.getInventory().setItem(1, new ItemStack(JugcraftGuns.GUNS.get("longhorn_rifle")));
				player.getInventory().setItem(2, new ItemStack(JugcraftArms.ITEMS.get("steel_longsword")));
			});
			// The HUD shown, so the hand and what it holds are drawn; it is put back as it was at the end.
			boolean hudWasHidden = context.computeOnClient(client -> client.gui.hud.isHidden());
			setHudHidden(context, false);
			context.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));
			context.getInput().pressKey(options -> options.keyHotbarSlots[0]);
			context.waitTicks(20);
			long stopped = context.computeOnClient(client -> CastPoses.stopped());

			// The wand in hand: the release plays on. Then 2 takes the rifle with the clip part way through.
			release(context);
			context.waitTicks(2);
			check(releasing(context) && animated(context), "With the wand in hand, Kindle's release clip did not play");
			context.getInput().pressKey(options -> options.keyHotbarSlots[1]);
			check(!releasing(context) && !animated(context) && context.computeOnClient(client -> CastPoses.stopped()) > stopped,
					"Kindle's release clip played on after 2 took the rifle");
			// Past where the clip would have ended: nothing has started it again.
			context.waitTicks(14);
			check(!animated(context), "A clip was laid over the player holding the rifle again");
			context.takeScreenshot("jugcraft_concordance_cast_pose_gun");

			// A release that arrives with the rifle already in hand, seen from the front.
			context.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
			context.waitTicks(5);
			stopped = context.computeOnClient(client -> CastPoses.stopped());
			release(context);
			context.waitTicks(2);
			check(!releasing(context) && !animated(context) && context.computeOnClient(client -> CastPoses.stopped()) > stopped,
					"A release that arrived with the rifle in hand was laid over the player");
			context.waitTicks(10);
			context.takeScreenshot("jugcraft_concordance_cast_pose_gun_third_person");

			// The same with an arm: 1 takes the wand back, a release plays, and 3 takes the longsword part way through it.
			context.getInput().pressKey(options -> options.keyHotbarSlots[0]);
			context.waitTicks(10);
			release(context);
			context.waitTicks(2);
			check(releasing(context), "With the wand in hand again, Kindle's release clip did not play");
			context.getInput().pressKey(options -> options.keyHotbarSlots[2]);
			check(!releasing(context) && !animated(context), "Kindle's release clip played on after 3 took the longsword");
			context.waitTicks(14);
			context.takeScreenshot("jugcraft_concordance_cast_pose_arm_third_person");

			// The Vigil's offering gesture: not started on a player holding the rifle; started with the wand in hand, and
			// stopped when 2 takes the rifle.
			context.getInput().pressKey(options -> options.keyHotbarSlots[1]);
			context.waitTicks(5);
			stopped = context.computeOnClient(client -> CastPoses.stopped());
			offering(singleplayer);
			context.waitTicks(20);
			check(!context.computeOnClient(client -> playing(client.player, VigilClient.LAYER))
					&& context.computeOnClient(client -> CastPoses.stopped()) == stopped,
					"The Vigil's offering gesture started on a player holding the rifle");
			context.getInput().pressKey(options -> options.keyHotbarSlots[0]);
			context.waitTicks(5);
			offering(singleplayer);
			context.waitFor(client -> playing(client.player, VigilClient.LAYER));
			context.getInput().pressKey(options -> options.keyHotbarSlots[1]);
			check(!context.computeOnClient(client -> playing(client.player, VigilClient.LAYER)),
					"The Vigil's offering gesture played on after 2 took the rifle");
			long gaveWay = context.computeOnClient(client -> CastPoses.stopped());
			Jugcraft.LOGGER.info("Concordance cast poses: {} clips and gestures gave way to a gun or an arm", gaveWay);
			context.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));
			setHudHidden(context, hudWasHidden);
		}
	}

	/** Starts Kindle's release clip on this client's player, as Spell Engine does when the server says a cast was released. */
	private static void release(ClientGameTestContext context) {
		context.runOnClient(client -> ((AnimatablePlayer) client.player).playSpellAnimation(SpellCast.Animation.RELEASE,
				"jugcraft:kindle_release", 1.0F));
	}

	/** Whether Spell Engine's release clip is playing on this client's player. */
	private static boolean releasing(ClientGameTestContext context) {
		return context.computeOnClient(client -> playing(client.player, SpellAnimationStack.RELEASE_ID));
	}

	/** Whether a clip is playing on one of the player's animation layers. */
	private static boolean playing(Player player, Identifier layer) {
		return PlayerAnimationAccess.getPlayerAnimationLayer(player, layer) instanceof PlayerAnimationController controller
				&& controller.isPlayingTriggeredAnimation();
	}

	/**
	 * Whether Player Animation Library lays anything over this client's player: what it needs to override the arms in
	 * third person and, in first person, to replace the hands and the held item with the held item drawn on the posed
	 * model.
	 */
	private static boolean animated(ClientGameTestContext context) {
		return context.computeOnClient(client -> PlayerAnimationAccess.getPlayerAnimManager(client.player).isActive());
	}

	/** The server tells this client to play the Vigil's offering gesture on its player, as when an offering is made. */
	private static void offering(TestSingleplayerContext singleplayer) {
		singleplayer.getServer().runOnServer(minecraft -> {
			ServerPlayer player = minecraft.getPlayerList().getPlayers().get(0);
			ServerPlayNetworking.send(player, new VigilGesturePayload(player.getId()));
		});
	}

	/** Hides or shows the HUD, hand and chat (F1) whatever state an earlier test left it in, as GunsClientGameTests does. */
	private static void setHudHidden(ClientGameTestContext context, boolean hidden) {
		context.runOnClient(client -> {
			if (client.gui.hud.isHidden() != hidden) {
				client.gui.hud.toggle();
			}
		});
	}

	private static void check(boolean ok, String message) {
		if (!ok) {
			throw new AssertionError(message);
		}
	}

	private static <T> T onServer(TestSingleplayerContext singleplayer, Function<ServerPlayer, T> read) {
		return singleplayer.getServer().computeOnServer(minecraft -> read.apply(minecraft.getPlayerList().getPlayers().get(0)));
	}

	/**
	 * Which of the invocations First Light teaches have been cast: Kindle by the evidence its cast records towards
	 * mastering First Light (its cooldown is too short to be seen afterwards), Aegis and Revelation by their cooldowns
	 * (12 and 8 seconds).
	 */
	private static List<String> cast(TestSingleplayerContext singleplayer) {
		return onServer(singleplayer, player -> {
			List<String> cast = new ArrayList<>();
			for (String id : FIRST_INVOCATIONS) {
				boolean done;
				if (id.equals("jugcraft:kindle")) {
					done = ConcordanceProgress.knowledge(player).progress("jugcraft:first_light").evidence().keySet().stream()
							.anyMatch(key -> key.startsWith("invoke:"));
				} else {
					var spell = SpellRegistry.from(player.level()).get(Identifier.parse(id)).orElse(null);
					done = spell != null && ((SpellCaster.Player) player).getCooldownManager().isCoolingDown(spell);
				}
				if (done) {
					cast.add(id);
				}
			}
			return cast;
		});
	}

	/** Whether a Kindled light stands in the room round {@code origin}. */
	private static boolean lit(ServerPlayer player, BlockPos origin) {
		ServerLevel level = (ServerLevel) player.level();
		for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-2, 0, -2), origin.offset(2, 3, 2))) {
			if (level.getBlockState(pos).is(JugcraftConcordance.LUMEN_MOTE)) {
				return true;
			}
		}
		return false;
	}
}
