package io.github.jimbozoomer.jugcraft.concordance;

import io.github.jimbozoomer.jugcraft.concordance.rules.Evidence;
import io.github.jimbozoomer.jugcraft.concordance.rules.Knowledge;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchEngine;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/**
 * Research Notes: personal knowledge made into a shared record. Used blank, a sheet takes down what its writer has
 * begun of each research entry (up to understood; mastery cannot be written down). Used written, by anyone but its
 * author, it gives the reader the evidence those notes are allowed to stand for: First Light, for example, accepts notes
 * from someone who understood it as a way to understand it, but the reader must still encounter and observe it for
 * themselves, and must still practise to master it. Reading the same author twice counts once. The sheet is not used
 * up by reading, so one writer's notes can teach a whole library of readers.
 * <p>
 * All of it runs on the server, limited to one write or read per player every {@value #COOLDOWN_TICKS} ticks.
 */
public class ResearchNotesItem extends Item {
	public static final int COOLDOWN_TICKS = 10;

	public ResearchNotesItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (!(player instanceof ServerPlayer server) || !(level instanceof ServerLevel)) {
			return InteractionResult.SUCCESS;
		}
		if (!JugcraftConfig.isFeatureEnabled(JugcraftConcordance.FEATURE)) {
			server.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.disabled"));
			return InteractionResult.FAIL;
		}
		if (!RateGate.allow(server, "notes", COOLDOWN_TICKS)) {
			return InteractionResult.FAIL;
		}
		ItemStack stack = player.getItemInHand(hand);
		ResearchNotes notes = stack.get(JugcraftConcordance.RESEARCH_NOTES);
		return notes == null ? write(server, hand, stack) : read(server, notes);
	}

	/** Takes down what the player knows on one blank sheet. */
	public static InteractionResult write(ServerPlayer player, InteractionHand hand, ItemStack blank) {
		Knowledge knowledge = ConcordanceProgress.knowledge(player);
		Map<String, ResearchState> entries = new LinkedHashMap<>();
		for (Map.Entry<String, Knowledge.Progress> entry : knowledge.entries().entrySet()) {
			if (ConcordanceData.rules().research(entry.getKey()) != null && entry.getValue().state() != ResearchState.NONE) {
				entries.put(entry.getKey(), entry.getValue().state());
			}
		}
		if (entries.isEmpty()) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.notes.nothing_to_write"));
			return InteractionResult.FAIL;
		}
		ResearchNotes notes = new ResearchNotes(player.getUUID(), player.getName().getString(), entries,
				ConcordanceProgress.now(player.level()));
		ItemStack written = blank.copyWithCount(1);
		written.set(JugcraftConcordance.RESEARCH_NOTES, notes);
		blank.consume(1, player);
		if (blank.isEmpty()) {
			player.setItemInHand(hand, written);
		} else {
			player.getInventory().placeItemBackInInventory(written, Prediction.SERVER_ONLY);
		}
		player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.notes.written", notes.entries().size()));
		return InteractionResult.SUCCESS;
	}

	/** Reads someone else's notes: evidence where the research allows notes, nothing for the author themselves. */
	public static InteractionResult read(ServerPlayer player, ResearchNotes notes) {
		Component author = Component.literal(notes.authorName());
		if (notes.author().equals(player.getUUID())) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.notes.own"));
			return InteractionResult.FAIL;
		}
		boolean recorded = false;
		boolean advanced = false;
		for (Map.Entry<String, ResearchState> entry : notes.entries().entrySet()) {
			ResearchEngine.Result result = ConcordanceProgress.record(player,
					new Evidence.ReadNotes(entry.getKey(), notes.author().toString(), entry.getValue()));
			recorded |= result.recorded();
			advanced |= !result.transitions().isEmpty();
		}
		if (!advanced) {
			// A research step announces itself; otherwise say what came of the reading.
			player.sendOverlayMessage(Component.translatable(recorded ? "message.jugcraft.concordance.notes.pending"
					: "message.jugcraft.concordance.notes.nothing_new", author));
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
			TooltipFlag flag) {
		ResearchNotes notes = stack.get(JugcraftConcordance.RESEARCH_NOTES);
		if (notes == null) {
			tooltip.accept(Component.translatable("tooltip.jugcraft.research_notes.blank").withStyle(ChatFormatting.GRAY));
			return;
		}
		tooltip.accept(Component.translatable("tooltip.jugcraft.research_notes.author", notes.authorName()).withStyle(ChatFormatting.GRAY));
		for (Map.Entry<String, ResearchState> entry : notes.entries().entrySet()) {
			tooltip.accept(Component.translatable("tooltip.jugcraft.research_notes.entry", ConcordanceProgress.researchName(entry.getKey()),
					Component.translatable("research_state.jugcraft." + entry.getValue().id())).withStyle(ChatFormatting.DARK_AQUA));
		}
	}
}
