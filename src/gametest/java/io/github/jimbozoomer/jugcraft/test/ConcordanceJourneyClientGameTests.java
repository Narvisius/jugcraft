package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.client.JournalClient;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import io.github.jimbozoomer.jugcraft.concordance.Examination;
import io.github.jimbozoomer.jugcraft.concordance.JugcraftConcordance;
import io.github.jimbozoomer.jugcraft.concordance.rules.FocusPool;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.spell_engine.api.spell.registry.SpellRegistry;
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
 * CI job {@code client}, with the optional integrations installed.
 */
public class ConcordanceJourneyClientGameTests implements FabricClientGameTest {
	private static final List<String> FIRST_INVOCATIONS = List.of("jugcraft:aegis", "jugcraft:kindle", "jugcraft:revelation");

	@Override
	public void runTest(ClientGameTestContext context) {
		theFirstSuccessByOrdinaryControls(context);
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
