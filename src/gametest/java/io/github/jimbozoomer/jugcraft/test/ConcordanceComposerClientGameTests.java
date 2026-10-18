package io.github.jimbozoomer.jugcraft.test;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.client.ComposerClient;
import io.github.jimbozoomer.jugcraft.client.ComposerScreen;
import io.github.jimbozoomer.jugcraft.client.ComposerWorkspace;
import io.github.jimbozoomer.jugcraft.concordance.ComposedSpells;
import io.github.jimbozoomer.jugcraft.concordance.Composing;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceSpells;
import io.github.jimbozoomer.jugcraft.concordance.Inscription;
import io.github.jimbozoomer.jugcraft.concordance.JugcraftConcordance;
import io.github.jimbozoomer.jugcraft.concordance.composer.ComposerResultPayload;
import io.github.jimbozoomer.jugcraft.concordance.rules.FocusPool;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import java.util.Locale;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.input.InputQuirks;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.spell_engine.api.spell.registry.SpellRegistry;
import net.spell_engine.internals.SpellExecution;
import net.spell_engine.internals.casting.SpellCast;
import net.spell_engine.internals.container.SpellContainerSource;
import net.spell_engine.internals.target.SpellTarget;

/**
 * A real client composes by selecting stars over the dedicated-server protocol, checks an invalid enhancement,
 * undoes, inscribes and casts through Spell Engine (2 Focus, a lit wall). Also exercises focus restoration,
 * both UI entry points, a second branch and large/small layouts. No spell is typed. One client.
 */
public class ConcordanceComposerClientGameTests implements FabricClientGameTest {
	private static final String SPELL = "touch struck light";
	private static final int WAIT = 200;

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestDedicatedServerContext server = context.worldBuilder().createServer()) {
			try (TestDedicatedServerConnection connection = server.connect()) {
				connection.waitForChunksRender();
				server.runCommand("time set noon");
				server.runCommand("weather clear");
				server.runCommand("gamemode survival @a");
				BlockPos stand = context.computeOnClient(client -> client.player.blockPosition());
				server.runOnServer(minecraft -> prepare(minecraft, stand));
				// Facing south (yaw 0), level: the wall prepared three blocks ahead is the first surface a touch meets.
				server.runCommand(String.format(Locale.ROOT, "tp @a %.1f %d %.1f 0 0", stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5));
				context.waitTicks(20);
				connection.waitForChunksRender();

				// The same native constellation screen is used with and without optional UI libraries.
				context.runOnClient(client -> ComposerClient.open(client, null));
				context.setScreen(() -> new ComposerScreen(null));
				context.waitFor(client -> ComposerClient.ready() && ComposerClient.state().words().size() == 7, WAIT);
				context.waitTicks(5);
				context.clickScreenButton("ui.jugcraft.word.touch");
                context.waitTicks(2);
                context.clickScreenButton("ui.jugcraft.word.struck");
                context.waitTicks(2);
                context.clickScreenButton("ui.jugcraft.word.light");
				context.waitFor(client -> answered(Composing.Outcome.VALID, SPELL), WAIT);
				context.waitTicks(5);
				context.runOnClient(client -> client.gui.toastManager().clear());
                context.waitTicks(2);
                context.takeScreenshot("jugcraft_concordance_composer_valid");

				// An edit the wand cannot hold: told why, the word marked, nothing to inscribe.
				context.clickScreenButton("ui.jugcraft.stage.3");
                context.waitTicks(2);
                context.clickScreenButton("ui.jugcraft.word.prolong");
				context.waitFor(client -> answered(Composing.Outcome.INVALID, SPELL + "+prolong"), WAIT);
				String problem = context.computeOnClient(client -> ComposerClient.result().report().blamed() + " "
						+ ComposerClient.result().report().lines().stream().map(line -> line.getString()).toList());
				Jugcraft.LOGGER.info("[composer-check] the illegal edit: {}", problem);
				check(context.computeOnClient(client -> ComposerClient.result().report().blamed().contains("light") && !ComposerClient.inscribable()),
						"Prolonged light should be refused with light marked and Inscribe disabled: " + problem);
				context.waitTicks(5);
				context.runOnClient(client -> client.gui.toastManager().clear());
                context.waitTicks(2);
                context.takeScreenshot("jugcraft_concordance_composer_problem");

				// One keyboard undo removes the entire enhancement selection.
				pressUndo(context);
				context.waitFor(client -> ComposerClient.draft().text().equals(SPELL), WAIT);
				context.waitFor(client -> answered(Composing.Outcome.VALID, SPELL) && ComposerClient.inscribable(), WAIT);

				// Inscribe, by its button.
				context.clickScreenButton("screen.jugcraft.composer.inscribe");
				context.waitFor(client -> answered(Composing.Outcome.INSCRIBED, SPELL), WAIT);
				String written = server.computeOnServer(minecraft -> {
					Inscription inscription = ComposedSpells.inscription(minecraft.getPlayerList().getPlayers().get(0).getMainHandItem());
					return inscription == null ? "nothing" : inscription.text() + " / " + inscription.focus() + " / " + inscription.cooldown();
				});
				Jugcraft.LOGGER.info("[composer-check] inscribed by the composer: {}", written);
				check(written.equals(SPELL + " / 2 / 20"), "The wand should carry the spell, its 2 Focus and 20-tick cooldown: " + written);
				context.waitTicks(5);
				context.runOnClient(client -> client.gui.toastManager().clear());
                context.waitTicks(2);
                context.takeScreenshot("jugcraft_concordance_composer_inscribed");

				// Cast through Spell Engine: it lights the wall's open side and takes its Focus once.
				context.setScreen(() -> null);
				String cast = server.computeOnServer(minecraft -> cast(minecraft, stand));
				Jugcraft.LOGGER.info("[composer-check] the inscribed spell cast: {}", cast);
				check(cast.equals("lit, Focus " + (FocusPool.MAX - 2)), "The composed spell should light the wall and cost 2 Focus: " + cast);

				// Reopened: the inscription shown, the draft kept.
				context.runOnClient(client -> ComposerClient.open(client, null));
				context.setScreen(() -> new ComposerScreen(null));
				context.waitFor(client -> ComposerClient.ready() && ComposerClient.state().inscribed().equals(SPELL)
						&& ComposerClient.draft().text().equals(SPELL), WAIT);

                // Tab reaches narrated controls; refreshing a reply preserves the current focused control.
                context.getInput().pressKey(InputConstants.KEY_TAB);
                context.waitTicks(2);
                String focused = context.computeOnClient(client -> ((net.minecraft.client.gui.components.AbstractWidget) client.gui.screen().getFocused()).getMessage().getString());
                context.runOnClient(client -> ComposerClient.changed());
                context.waitTicks(10);
                String restored = context.computeOnClient(client -> ((net.minecraft.client.gui.components.AbstractWidget) client.gui.screen().getFocused()).getMessage().getString());
                check(focused.equals(restored), "A server answer must preserve keyboard focus: " + focused + " / " + restored);

				int scale = context.computeOnClient(client -> client.options.guiScale().get());
				context.runOnClient(client -> {
					client.options.guiScale().set(4);
					client.resizeGui();
				});
				context.waitTicks(10);
				context.runOnClient(client -> client.gui.toastManager().clear());
                context.waitTicks(2);
                context.takeScreenshot("jugcraft_concordance_composer_scale4");
				context.runOnClient(client -> {
					client.options.guiScale().set(scale);
					client.resizeGui();
				});
				if (FabricLoader.getInstance().isModLoaded("guilib")) {
					context.setScreen(() -> null);
					context.runOnClient(client -> ComposerWorkspace.open());
					context.waitTicks(10);
					context.runOnClient(client -> client.gui.toastManager().clear());
                context.waitTicks(2);
                context.takeScreenshot("jugcraft_concordance_composer_workspace");
					context.setScreen(() -> new ComposerScreen(null));
				}
                server.runOnServer(minecraft -> {
                    ServerPlayer player = minecraft.getPlayerList().getPlayers().get(0);
                    ConcordanceProgress.grant(player, "jugcraft:first_light", ResearchState.MASTERED);
                    player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JugcraftConcordance.ADEPT_WAND));
                });
                context.waitTicks(10);
                context.runOnClient(client -> { ComposerClient.draft().clear(); ComposerClient.open(client, null); });
                context.waitFor(client -> ComposerClient.ready() && ComposerClient.state().words().size() > 7, WAIT);
                context.waitTicks(5);
                context.clickScreenButton("ui.jugcraft.word.ray"); context.waitTicks(2);
                context.clickScreenButton("ui.jugcraft.word.creatures"); context.waitTicks(2);
                context.runOnClient(client -> client.gui.toastManager().clear());
                context.waitTicks(2);
                context.takeScreenshot("jugcraft_constellation_effects");
                context.clickScreenButton("ui.jugcraft.word.reveal");
                context.waitFor(client -> ComposerClient.inscribable(), WAIT);
                context.clickScreenButton("ui.jugcraft.stage.3"); context.waitTicks(5);
                context.runOnClient(client -> client.gui.toastManager().clear());
                context.waitTicks(2);
                context.takeScreenshot("jugcraft_constellation_enhancements");
                context.getInput().resizeWindow(1280, 800);
                context.runOnClient(client -> { client.options.guiScale().set(2); client.resizeGui(); });
                context.waitTicks(10);
                context.clickScreenButton("ui.jugcraft.stage.2"); context.waitTicks(12);
                context.takeScreenshot("jugcraft_constellation_large");
                context.clickScreenButton(net.minecraft.network.chat.Component.translatable("ui.jugcraft.paths", 1).getString()); context.waitTicks(3);
                context.clickScreenButton("ui.jugcraft.branch.add"); context.waitTicks(3);
                context.clickScreenButton("ui.jugcraft.word.here"); context.waitTicks(3);
                context.clickScreenButton("ui.jugcraft.word.struck"); context.waitTicks(3);
                context.clickScreenButton("ui.jugcraft.word.ward");
                context.waitFor(client -> ComposerClient.inscribable(), WAIT);
                check(context.computeOnClient(client -> ComposerClient.draft().text().equals("ray creatures reveal then here struck ward")),
                    "Visual branch should retain the first path and append the second path");
                context.waitTicks(10);
                context.takeScreenshot("jugcraft_constellation_branch");
                check(context.computeOnClient(client -> client.gui.screen().children().stream().noneMatch(w -> w instanceof net.minecraft.client.gui.components.EditBox)),
                    "The visual composer must not require or expose a text box");
                // Last, a small window (the runner restores the window's size after each test).
				context.getInput().resizeWindow(640, 360);
				context.waitTicks(10);
				context.runOnClient(client -> client.gui.toastManager().clear());
                context.waitTicks(2);
                context.takeScreenshot("jugcraft_concordance_composer_small");
				context.setScreen(() -> null);
			}
		}
	}

	/**
	 * Ctrl+Z (Cmd+Z on macOS) through the keyboard handler, as a real key press arrives. Fabric's TestInput sends every key
	 * with no modifiers, even while holdControl holds Ctrl, so it cannot express Ctrl+Z in 26.3 (as CarvingClientGameTests
	 * found).
	 */
	private static void pressUndo(ClientGameTestContext context) {
		context.runOnClient(client -> {
			KeyEvent undo = new KeyEvent(InputConstants.KEY_Z, InputConstants.KEYCODE_Z, InputQuirks.EDIT_SHORTCUT_KEY_MODIFIER);
			client.keyboardHandler.keyPress(client.getWindow().handle(), InputConstants.PRESS, undo);
			client.keyboardHandler.keyPress(client.getWindow().handle(), InputConstants.RELEASE, undo);
		});
	}

	/** The latest answer is settled (nothing newer due or on its way) and is {@code outcome} for {@code text}. */
	private static boolean answered(Composing.Outcome outcome, String text) {
		ComposerResultPayload result = ComposerClient.result();
		return result != null && !ComposerClient.checking() && !ComposerClient.writing() && result.what() == outcome && result.text().equals(text);
	}

	/** The wand in hand, First Light understood, Focus full, and a two-high stone wall three blocks south. */
	private static void prepare(MinecraftServer minecraft, BlockPos stand) {
		ServerPlayer player = minecraft.getPlayerList().getPlayers().get(0);
		ServerLevel level = minecraft.overworld();
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JugcraftConcordance.INITIATE_WAND));
		ConcordanceProgress.grant(player, "jugcraft:first_light", ResearchState.UNDERSTOOD);
		ConcordanceProgress.setFocus(player, FocusPool.MAX);
		for (int dz = 1; dz <= 2; dz++) {
			for (int dy = 0; dy <= 1; dy++) {
				level.setBlockAndUpdate(stand.offset(0, dy, dz), Blocks.AIR.defaultBlockState());
			}
		}
		level.setBlockAndUpdate(stand.offset(0, 0, 3), Blocks.STONE.defaultBlockState());
		level.setBlockAndUpdate(stand.offset(0, 1, 3), Blocks.STONE.defaultBlockState());
	}

	/** Casts the inscribed spell as Spell Engine would and says what happened. */
	private static String cast(MinecraftServer minecraft, BlockPos stand) {
		ServerPlayer player = minecraft.getPlayerList().getPlayers().get(0);
		ServerLevel level = minecraft.overworld();
		SpellContainerSource.setDirty(player, ConcordanceSpells.SOURCE);
		SpellContainerSource.setDirty(player, "main_hand");
		SpellContainerSource.update(player);
		SpellExecution.performSpell(level, player, SpellRegistry.from(level).get(Identifier.parse(ComposedSpells.SPELL)).orElseThrow(),
				SpellTarget.SearchResult.empty(), SpellCast.Action.RELEASE, 1.0F);
		boolean lit = level.getBlockState(stand.offset(0, 1, 2)).is(JugcraftConcordance.LUMEN_MOTE);
		return (lit ? "lit" : "not lit") + ", Focus " + ConcordanceProgress.currentFocus(player);
	}

	private static void check(boolean condition, String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}
}
