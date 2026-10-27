package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceSpells;
import io.github.jimbozoomer.jugcraft.concordance.JugcraftConcordance;
import io.github.jimbozoomer.jugcraft.concordance.rime.Rime;
import io.github.jimbozoomer.jugcraft.concordance.rime.Rimebind;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import io.github.jimbozoomer.jugcraft.concordance.strata.Brace;
import io.github.jimbozoomer.jugcraft.concordance.strata.Strata;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.spell_engine.internals.container.SpellContainerSource;

/**
 * Concordance Chunk B, unit 3 in a real client (docs/features/arcane-concordance-rime-strata.md): the owner's icons for
 * Rimebind, Brace and Preserve, and for Rimebound, Thawing and Braced, are in the client's resources; then four shots
 * for people to look at:
 * <ul>
 * <li>{@code jugcraft_rime_strata_spell_bar}: Rimekeeping and Stratawrighting understood and the Initiate's Wand in
 * hand, so Spell Engine's bar holds Rimebind, Brace and Preserve beside First Light's three;</li>
 * <li>{@code jugcraft_rime_strata_spell_bar_scale1}: the same bar at GUI scale 1, where all six slots should fit (the
 * width is logged; since Chunk B unit 5, B-11);</li>
 * <li>{@code jugcraft_rime_strata_status_hud}: the player Rimebound, Thawing (no icon) and Braced;</li>
 * <li>{@code jugcraft_rime_strata_status_inventory}: the same in the inventory's list, Thawing listed.</li>
 * </ul>
 * Learning raises advancement toasts over the top right, where the HUD draws effect icons and the inventory lists
 * effects, so they are cleared before each shot. Nothing here judges a picture. The HUD is shown for the shots and put
 * back as it was. CI job {@code client}.
 */
public class ConcordanceRimeStrataClientGameTests implements FabricClientGameTest {
	private static final List<String> SPELLS = List.of("rimebind", "brace", "preserve");
	private static final List<String> STATUSES = List.of("rimebound", "rime_thaw", "braced");

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
			singleplayer.getConnection().waitForChunksRender();
			TestServerContext server = singleplayer.getServer();
			server.runCommand("gamerule minecraft:send_command_feedback false");
			server.runCommand("gamerule minecraft:spawn_mobs false");
			server.runCommand("gamerule minecraft:advance_time false");
			server.runCommand("time set noon");
			server.runCommand("weather clear");
			server.runCommand("gamemode survival @a");
			BlockPos origin = context.computeOnClient(client -> BlockPos.containing(client.player.position()));
			server.runCommand(String.format(Locale.ROOT, "fill %d %d %d %d %d %d minecraft:snow_block", origin.getX() - 6, origin.getY() - 1,
					origin.getZ() - 6, origin.getX() + 6, origin.getY() - 1, origin.getZ() + 6));
			server.runCommand(String.format(Locale.ROOT, "fill %d %d %d %d %d %d minecraft:air", origin.getX() - 6, origin.getY(), origin.getZ() - 6,
					origin.getX() + 6, origin.getY() + 6, origin.getZ() + 6));
			server.runCommand(String.format(Locale.ROOT, "tp @p %.1f %d %.1f 180 10", origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5));
			context.waitTicks(20);
			boolean hudWasHidden = context.computeOnClient(client -> client.gui.hud.isHidden());
			setHudHidden(context, false);

			String missing = context.computeOnClient(ConcordanceRimeStrataClientGameTests::missing);
			Jugcraft.LOGGER.info("[rime strata client] the owner's icons: {}", missing.isEmpty() ? "present" : missing);
			check(missing.isEmpty(), "The client lacks Rime's and Strata's icons: " + missing);

			// Both researches understood and the wand in hand: Spell Engine's bar. One research a tick (see learnEach).
			learnEach(context, server, "jugcraft:first_light", Rime.RESEARCH, Strata.RESEARCH);
			server.runOnServer(ConcordanceRimeStrataClientGameTests::arm);
			context.waitTicks(40);
			singleplayer.getConnection().waitForChunksRender();
			clearToasts(context);
			context.takeScreenshot("jugcraft_rime_strata_spell_bar");
			// The same bar at GUI scale 1, where it has room: Spell Engine centres it 170 GUI pixels left of the screen's
			// middle, 20 a slot, so at CI's default scale (427 wide) a bar of 5 or more runs off the left edge (the record's
			// Rollout: the spell bar's place).
			int scale = context.computeOnClient(client -> client.options.guiScale().get());
			context.runOnClient(client -> {
				client.options.guiScale().set(1);
				client.resizeGui();
			});
			context.waitTicks(10);
			int width = context.computeOnClient(client -> client.getWindow().getGuiScaledWidth());
			Jugcraft.LOGGER.info("[rime strata client] the spell bar at GUI scale 1, {} GUI pixels wide", width);
			clearToasts(context);
			context.takeScreenshot("jugcraft_rime_strata_spell_bar_scale1");
			context.runOnClient(client -> {
				client.options.guiScale().set(scale);
				client.resizeGui();
			});
			context.waitTicks(5);

			// Rimebound, Thawing (hidden, as Rimebind leaves it) and Braced, on the HUD and in the inventory.
			server.runOnServer(minecraft -> {
				ServerPlayer player = player(minecraft);
				player.addEffect(new MobEffectInstance(Rimebind.RIMEBOUND, 1200, 0));
				player.addEffect(new MobEffectInstance(Rimebind.RIME_THAW, 1240, 0, false, false, false));
				player.addEffect(new MobEffectInstance(Brace.BRACED, 1200, 0));
			});
			context.waitTicks(20);
			boolean shown = context.computeOnClient(client -> client.player.hasEffect(Rimebind.RIMEBOUND) && client.player.hasEffect(Rimebind.RIME_THAW)
					&& client.player.hasEffect(Brace.BRACED));
			check(shown, "The client was not told of Rimebound, Thawing and Braced");
			clearToasts(context);
			context.takeScreenshot("jugcraft_rime_strata_status_hud");
			context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
			context.waitTicks(10);
			clearToasts(context);
			context.takeScreenshot("jugcraft_rime_strata_status_inventory");
			context.setScreen(() -> null);
			server.runOnServer(minecraft -> player(minecraft).removeAllEffects());
			setHudHidden(context, hudWasHidden);
		}
	}

	/** The spell and status icons the client cannot find, or an empty string. */
	private static String missing(Minecraft client) {
		List<String> missing = new ArrayList<>();
		for (String spell : SPELLS) {
			if (client.getResourceManager().getResource(Jugcraft.id("textures/spell/" + spell + ".png")).isEmpty()) {
				missing.add("textures/spell/" + spell + ".png");
			}
		}
		for (String status : STATUSES) {
			if (client.getResourceManager().getResource(Jugcraft.id("textures/mob_effect/" + status + ".png")).isEmpty()) {
				missing.add("textures/mob_effect/" + status + ".png");
			}
		}
		return String.join(", ", missing);
	}

	/** First Light, Rimekeeping and Stratawrighting understood, the Initiate's Wand in hand, and the spell bar refreshed. */
	/**
	 * Understands each research in its own server tick. Each learning sends Modonomicon's book state, and the payload
	 * holds a live map that the next learning in the same tick can change while the connection is still encoding it: CI
	 * saw {@code modonomicon:sync_book_visual_states} fail to encode and the client drop (run 38679932446).
	 */
	private static void learnEach(ClientGameTestContext context, TestServerContext server, String... researches) {
		for (String research : researches) {
			server.runOnServer(minecraft -> ConcordanceProgress.grant(player(minecraft), research, ResearchState.UNDERSTOOD));
			context.waitTicks(2);
		}
	}

	/** The wand in hand, and Spell Engine told to rebuild the bar. */
	private static void arm(MinecraftServer minecraft) {
		ServerPlayer player = player(minecraft);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JugcraftConcordance.INITIATE_WAND));
		SpellContainerSource.setDirty(player, ConcordanceSpells.SOURCE);
		SpellContainerSource.setDirty(player, "main_hand");
		SpellContainerSource.update(player);
	}

	private static ServerPlayer player(MinecraftServer minecraft) {
		return minecraft.getPlayerList().getPlayers().get(0);
	}

	/** Removes the shown and queued toasts (learning's advancements), then waits for a frame drawn without them. */
	private static void clearToasts(ClientGameTestContext context) {
		context.runOnClient(client -> client.gui.toastManager().clear());
		context.waitTicks(5);
	}

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
}
