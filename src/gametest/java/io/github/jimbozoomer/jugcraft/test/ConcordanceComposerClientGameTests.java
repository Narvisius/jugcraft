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
 * Chunk A (A-02, A-03, A-08 steps 2 to 4) in a real client on a dedicated server, so every composer message is encoded,
 * sent and decoded. A survival player who understands First Light opens the plain Spell Composer and types a spell; the
 * server says what it does and costs (screenshot {@code jugcraft_concordance_composer_valid}). They type a modifier the
 * wand cannot hold and are told exactly why, with the word marked and Inscribe disabled
 * ({@code jugcraft_concordance_composer_problem}); Ctrl+Z undoes it; the Inscribe button writes the spell on the wand
 * ({@code jugcraft_concordance_composer_inscribed}); the inscribed spell, cast through Spell Engine, lights the wall it
 * touches and takes its 2 Focus once. Reopened, the composer shows what is inscribed and keeps the draft; Tab chooses
 * the spell's first word, and the keyboard stays there when the screen is laid out again; the composer is shown
 * at GUI scale 4 ({@code jugcraft_concordance_composer_scale4}), with GuiLib installed as in CI as the GuiLib workspace
 * over the same draft ({@code jugcraft_concordance_composer_workspace}), and in a small window
 * ({@code jugcraft_concordance_composer_small}). Results are logged with the prefix "[composer-check]". One client.
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

				// The plain composer, whatever else is installed; the spell's text box has the keyboard.
				context.runOnClient(client -> ComposerClient.open(client, null));
				context.setScreen(() -> new ComposerScreen(null));
				context.waitFor(client -> ComposerClient.ready() && ComposerClient.state().words().size() == 7, WAIT);
				context.waitTicks(5);
				context.getInput().typeChars(SPELL);
				context.waitFor(client -> answered(Composing.Outcome.VALID, SPELL), WAIT);
				context.waitTicks(5);
				context.takeScreenshot("jugcraft_concordance_composer_valid");

				// An edit the wand cannot hold: told why, the word marked, nothing to inscribe.
				context.getInput().typeChars("+prolong");
				context.waitFor(client -> answered(Composing.Outcome.INVALID, SPELL + "+prolong"), WAIT);
				String problem = context.computeOnClient(client -> ComposerClient.result().report().blamed() + " "
						+ ComposerClient.result().report().lines().stream().map(line -> line.getString()).toList());
				Jugcraft.LOGGER.info("[composer-check] the illegal edit: {}", problem);
				check(context.computeOnClient(client -> ComposerClient.result().report().blamed().contains("light") && !ComposerClient.inscribable()),
						"Prolonged light should be refused with light marked and Inscribe disabled: " + problem);
				context.waitTicks(5);
				context.takeScreenshot("jugcraft_concordance_composer_problem");

				// Ctrl+Z, a key press at a time (each typed character is one undo), takes the typing back to the valid spell.
				for (int i = 0; i < "+prolong".length(); i++) {
					pressUndo(context);
				}
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

				// A keyboard player keeps their place: Tab from the text box reaches the spell's first word and chooses it, and
				// laying the screen out again (as every answer and edit does) leaves the keyboard and the choice where they were.
				context.getInput().pressKey(InputConstants.KEY_TAB);
				context.waitTicks(2);
				check(context.computeOnClient(client -> ComposerClient.draft().level() == 0 && ComposerClient.draft().index() == 0),
						"Tab from the text box should choose the spell's first word");
				context.runOnClient(client -> ComposerClient.changed());
				context.waitTicks(10);
				int chosen = context.computeOnClient(client -> ComposerClient.draft().index());
				Jugcraft.LOGGER.info("[composer-check] the word chosen by keyboard, after the screen was laid out again: {}", chosen);
				check(chosen == 0, "Laying the screen out again should leave the keyboard on the chosen word, not walk it on: " + chosen);
				int scale = context.computeOnClient(client -> client.options.guiScale().get());
				context.runOnClient(client -> {
					client.options.guiScale().set(4);
					client.resizeGui();
				});
				context.waitTicks(10);
				context.takeScreenshot("jugcraft_concordance_composer_scale4");
				context.runOnClient(client -> {
					client.options.guiScale().set(scale);
					client.resizeGui();
				});
				if (FabricLoader.getInstance().isModLoaded("guilib")) {
					context.setScreen(() -> null);
					context.runOnClient(client -> ComposerWorkspace.open());
					context.waitTicks(10);
					context.takeScreenshot("jugcraft_concordance_composer_workspace");
					context.setScreen(() -> new ComposerScreen(null));
				}
				// Last, a small window (the runner restores the window's size after each test).
				context.getInput().resizeWindow(640, 360);
				context.waitTicks(10);
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
