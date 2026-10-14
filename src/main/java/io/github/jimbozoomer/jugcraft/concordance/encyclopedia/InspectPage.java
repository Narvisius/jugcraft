package io.github.jimbozoomer.jugcraft.concordance.encyclopedia;

import io.github.jimbozoomer.jugcraft.concordance.CircleAnchorBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceCommand;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceData;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import io.github.jimbozoomer.jugcraft.concordance.JugcraftConcordance;
import io.github.jimbozoomer.jugcraft.concordance.LeyPylonBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.resource.Conversion;
import io.github.jimbozoomer.jugcraft.concordance.ritual.RitualDefinition;
import io.github.jimbozoomer.jugcraft.concordance.ritual.RitualMachine;
import io.github.jimbozoomer.jugcraft.concordance.rules.Knowledge;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchEngine;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import io.github.jimbozoomer.jugcraft.guide.encyclopedia.EncyclopediaRequestPayload;
import io.github.jimbozoomer.jugcraft.guide.encyclopedia.PageWriter;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

/**
 * The encyclopedia's Inspect page (Concordance Chunk A, A-04 and A-07): shown when the encyclopedia is opened while
 * looking at a Circle Anchor or a Ley Pylon within {@value ConcordanceCommand#CIRCLE_READ_RANGE} blocks, in a loaded
 * chunk. For an anchor it is the anchor's own report, the same lines chat, {@code /jugcraft concordance circle} and Jade
 * show ({@link CircleAnchorBlockEntity#status(ServerLevel, java.util.function.Consumer)}, from its cached report: there
 * is no second validator), then the rituals the player knows there; for a pylon, its charge, how it fills and whose
 * rituals it lends to. The position is the client's claim, so it is checked here: the same reach as the command gives
 * a player, and never a chunk that is not loaded. It only reads.
 */
public final class InspectPage {
	public static final String PAGE = "inspect";

	private InspectPage() {
	}

	public static void write(ServerPlayer player, EncyclopediaRequestPayload request, PageWriter out) {
		if (!JugcraftConfig.isFeatureEnabled(JugcraftConcordance.FEATURE) || request.looking().isEmpty()) {
			return;
		}
		BlockPos pos = request.looking().get();
		ServerLevel level = player.level();
		double reach = ConcordanceCommand.CIRCLE_READ_RANGE;
		if (!level.isLoaded(pos) || player.distanceToSqr(Vec3.atCenterOf(pos)) > reach * reach) {
			return;
		}
		BlockEntity entity = level.getBlockEntity(pos);
		if (entity instanceof CircleAnchorBlockEntity anchor) {
			anchor(player, level, pos, anchor, out);
		} else if (entity instanceof LeyPylonBlockEntity pylon) {
			pylon(player, pos, pylon, out);
		}
	}

	private static void anchor(ServerPlayer player, ServerLevel level, BlockPos pos, CircleAnchorBlockEntity anchor, PageWriter out) {
		out.heading(Component.translatable("encyclopedia.jugcraft.inspect.at", Component.translatable("block.jugcraft.circle_anchor"),
				pos.getX(), pos.getY(), pos.getZ()));
		anchor.status(level, out::fact);
		out.heading(Component.translatable("encyclopedia.jugcraft.inspect.rituals"));
		Knowledge knowledge = ConcordanceProgress.knowledge(player);
		List<RitualDefinition> rituals = ConcordanceData.rules().ritualsAt(CircleAnchorBlockEntity.ANCHOR);
		int unknown = 0;
		for (RitualDefinition ritual : rituals) {
			if (!ResearchEngine.knowsRitual(knowledge, ritual)) {
				unknown++;
				continue;
			}
			out.fact(CircleAnchorBlockEntity.ritualName(ritual.id()), Component.translatable("encyclopedia.jugcraft.inspect.ritual", ritual.participants(),
					ritual.focus(), ritual.steps(), ritual.steps() * RitualMachine.STEP_TICKS / 20, ritual.ley()), List.of());
		}
		if (unknown == rituals.size()) {
			out.fact(Component.translatable("encyclopedia.jugcraft.inspect.no_rituals"));
		}
		if (unknown > 0) {
			out.fact(Component.translatable("encyclopedia.jugcraft.inspect.rituals_unknown", unknown));
		}
		out.codex("circles/lesser_circle", "jugcraft:circle_lore", ResearchState.ENCOUNTERED);
		out.codex("circles/rituals", "jugcraft:circle_lore", ResearchState.UNDERSTOOD);
	}

	private static void pylon(ServerPlayer player, BlockPos pos, LeyPylonBlockEntity pylon, PageWriter out) {
		out.heading(Component.translatable("encyclopedia.jugcraft.inspect.at", Component.translatable("block.jugcraft.ley_pylon"),
				pos.getX(), pos.getY(), pos.getZ()));
		out.fact(pylon.status());
		Conversion conversion = ConcordanceData.rules().conversions().get(LeyPylonBlockEntity.CONVERSION);
		out.fact(conversion == null ? Component.translatable("encyclopedia.jugcraft.inspect.pylon.fill_energy", LeyPylonBlockEntity.JE_PER_LEY)
				: Component.translatable("encyclopedia.jugcraft.inspect.pylon.fill", conversion.fromAmount(), conversion.toAmount(), LeyPylonBlockEntity.JE_PER_LEY));
		out.fact(Component.translatable(pylon.lends(List.of(player.getUUID())) ? "encyclopedia.jugcraft.inspect.pylon.lends"
				: "encyclopedia.jugcraft.inspect.pylon.owned"));
		out.codex("circles/ley_pylon", "jugcraft:circle_lore", ResearchState.ENCOUNTERED);
	}
}
