package io.github.jimbozoomer.jugcraft.concordance.encyclopedia;

import io.github.jimbozoomer.jugcraft.concordance.Alchemy;
import io.github.jimbozoomer.jugcraft.concordance.CircleAnchorBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.ComposeText;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceCommand;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceData;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import io.github.jimbozoomer.jugcraft.concordance.CrucibleBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.JugcraftConcordance;
import io.github.jimbozoomer.jugcraft.concordance.LeyPylonBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.alchemy.Assay;
import io.github.jimbozoomer.jugcraft.concordance.alchemy.CrucibleInspection;
import io.github.jimbozoomer.jugcraft.concordance.compose.Text;
import io.github.jimbozoomer.jugcraft.concordance.diagnostic.Diagnostic;
import io.github.jimbozoomer.jugcraft.concordance.ecology.Graft;
import io.github.jimbozoomer.jugcraft.concordance.ecology.HabitatInspection;
import io.github.jimbozoomer.jugcraft.concordance.ecology.Niche;
import io.github.jimbozoomer.jugcraft.concordance.ecology.Organism;
import io.github.jimbozoomer.jugcraft.concordance.ecology.Verdict;
import io.github.jimbozoomer.jugcraft.concordance.garden.Garden;
import io.github.jimbozoomer.jugcraft.concordance.garden.OrganismCropBlock;
import io.github.jimbozoomer.jugcraft.concordance.garden.VerdantBedBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.resource.Conversion;
import io.github.jimbozoomer.jugcraft.concordance.ritual.Readiness;
import io.github.jimbozoomer.jugcraft.concordance.ritual.RitualDefinition;
import io.github.jimbozoomer.jugcraft.concordance.ritual.RitualMachine;
import io.github.jimbozoomer.jugcraft.concordance.ritual.StructurePattern;
import io.github.jimbozoomer.jugcraft.concordance.rules.Knowledge;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchEngine;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import io.github.jimbozoomer.jugcraft.guide.encyclopedia.EncyclopediaRequestPayload;
import io.github.jimbozoomer.jugcraft.guide.encyclopedia.PageWriter;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The encyclopedia's Inspect page (Concordance Chunk A, A-04 and A-07): shown when the encyclopedia is opened while
 * looking at a Circle Anchor or a Ley Pylon within {@value ConcordanceCommand#CIRCLE_READ_RANGE} blocks, in a loaded
 * chunk, and an Alembic Crucible within the reader's own block reach, at three levels by research and a carried Assay
 * Glass (Concordance Chunk C, C-01). For an anchor it is the anchor's own report, the same lines chat,
 * {@code /jugcraft concordance circle} and Jade show ({@link CircleAnchorBlockEntity#status(ServerLevel, java.util.function.Consumer)},
 * from its cached report: there is no second validator), then the rituals the player knows there; for a pylon, its
 * charge, how it fills and whose rituals it lends to; for a crucible, what anyone sees (heat and what heats it, volume,
 * tank, formula step, why it waits, the risk line), its slots, and with the glass what the glass would tell
 * ({@link CrucibleInspection}). The position is the client's claim, so it is checked here: the same reach as the command
 * gives a player (a crucible: the player's own reach), and never a chunk that is not loaded. It only reads. Since
 * Concordance Chunk C (C-03), for an anchor also the readiness of the rite the offerings answer, every shortfall at once,
 * for the reader as its leader; while a rite holds it, what it has spent apart from what it still holds; how the last
 * attempt broke off; and the rite's stage and payment table with its backlash. A rite the reader has not learned is never
 * named. Since Concordance Chunk C (C-07) also a Verdant Bed, or a crop through the bed below it, within the reader's own
 * block reach: the bed's moisture and nutrients, the crop's step, the verdict and each factor against the plant's niche
 * with how far it reaches, the light as sky and lamps (HabitatInspection). Since C-09, a bed's page names the graft it
 * carries and the ranges it changes, and a crop's page does when the crop is the graft's host; the factors are judged by
 * the niche the bed gives (VerdantBedBlockEntity.niche).
 */
public final class InspectPage {
	public static final String PAGE = "inspect";
	/** How far beyond the reader's own block reach a crucible is read: the allowance the server gives a use, never the anchor's 16 (C-P26). */
	public static final double CRUCIBLE_REACH = 1.0;
	/** How far beyond the reader's own block reach a Verdant Bed or a crop is read: as a crucible's (C-07, C-P54). */
	public static final double GARDEN_REACH = 1.0;

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
		} else if (entity instanceof CrucibleBlockEntity crucible && player.isWithinBlockInteractionRange(pos, CRUCIBLE_REACH)) {
			crucible(player, level, pos, crucible, out);
		} else if (entity instanceof VerdantBedBlockEntity bed && player.isWithinBlockInteractionRange(pos, GARDEN_REACH)) {
			garden(level, pos, level.getBlockState(pos).getBlock(), pos, bed, out);
		} else if (entity == null && level.getBlockState(pos).getBlock() instanceof OrganismCropBlock crop
				&& player.isWithinBlockInteractionRange(pos, GARDEN_REACH)) {
			// A crop has no block entity: the bed below it holds what it grows by (Concordance Chunk C, C-07).
			if (level.getBlockEntity(pos.below()) instanceof VerdantBedBlockEntity soil) {
				garden(level, pos, crop, pos.below(), soil, out);
			} else {
				out.heading(Component.translatable("encyclopedia.jugcraft.inspect.at", crop.getName(), pos.getX(), pos.getY(), pos.getZ()));
				out.fact(Component.translatable("message.jugcraft.concordance.garden.no_bed"));
			}
		}
	}

	private static void anchor(ServerPlayer player, ServerLevel level, BlockPos pos, CircleAnchorBlockEntity anchor, PageWriter out) {
		out.heading(Component.translatable("encyclopedia.jugcraft.inspect.at", Component.translatable("block.jugcraft.circle_anchor"),
				pos.getX(), pos.getY(), pos.getZ()));
		anchor.status(level, out::fact);
		Knowledge knowledge = ConcordanceProgress.knowledge(player);
		readiness(player, level, anchor, knowledge, out);
		out.heading(Component.translatable("encyclopedia.jugcraft.inspect.rituals"));
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

	/**
	 * Concordance Chunk C (C-03): readiness, or spent and held, how the last attempt broke off, and the stage and payment
	 * table (plan 03:107, 03:122). From the anchor (CircleAnchorBlockEntity.readiness); a rite the reader has not learned is
	 * never named and its table not shown. No sign: this page only reads.
	 */
	private static void readiness(ServerPlayer player, ServerLevel level, CircleAnchorBlockEntity anchor, Knowledge knowledge, PageWriter out) {
		Readiness readiness = anchor.readiness(level, player);
		RitualDefinition rite = readiness == null ? null : readiness.rite();
		boolean known = rite != null && ResearchEngine.knowsRitual(knowledge, rite);
		Component unknown = Component.translatable("encyclopedia.jugcraft.inspect.unknown_rite");
		Component name = known ? CircleAnchorBlockEntity.ritualName(rite.id()) : unknown;
		if (readiness != null && readiness.spent() == null) {
			if (rite == null) {
				out.fact(Component.translatable("encyclopedia.jugcraft.inspect.readiness.unknown"));
			} else {
				out.heading(Component.translatable("encyclopedia.jugcraft.inspect.readiness", name));
				if (readiness.lines().isEmpty()) {
					out.fact(Component.translatable("encyclopedia.jugcraft.inspect.readiness.ready"));
				}
				for (Readiness.Line line : readiness.lines()) {
					out.fact(ComposeText.show(line.diagnostic().text()));
				}
			}
		} else if (readiness != null) {
			out.heading(Component.translatable(anchor.phase() == RitualMachine.Phase.COMPLETE ? "encyclopedia.jugcraft.inspect.completed"
					: "encyclopedia.jugcraft.inspect.under_way", name));
			out.fact(ComposeText.show(readiness.spentText()));
			heldOfferings(readiness, out);
			for (Readiness.Line line : readiness.lines()) {
				out.fact(ComposeText.show(line.diagnostic().text()));
			}
		}
		Readiness.Ending ending = anchor.ending();
		if (ending != null && anchor.phase() == RitualMachine.Phase.IDLE) {
			RitualDefinition last = anchor.run().ritual() == null ? null : ConcordanceData.rules().ritual(anchor.run().ritual());
			Component lastName = last != null && ResearchEngine.knowsRitual(knowledge, last) ? CircleAnchorBlockEntity.ritualName(last.id()) : unknown;
			out.fact(Component.translatable("encyclopedia.jugcraft.inspect.last", CircleAnchorBlockEntity.endingLine(lastName, ending)));
		}
		StructurePattern pattern = known ? ConcordanceData.rules().structure(rite.structure()) : null;
		if (pattern != null) {
			out.heading(Component.translatable("encyclopedia.jugcraft.inspect.stages", name));
			for (Text stage : Readiness.stages(rite, pattern.channels().size())) {
				out.fact(ComposeText.show(stage));
			}
		}
	}

	/** What a running rite holds until it ends, with each item's icon (unknown items left out). */
	private static void heldOfferings(Readiness readiness, PageWriter out) {
		MutableComponent items = Component.empty();
		List<Item> icons = new ArrayList<>();
		for (Readiness.Held held : readiness.held()) {
			Identifier id = Identifier.tryParse(held.item());
			Item item = id == null ? Items.AIR : BuiltInRegistries.ITEM.getValue(id);
			if (item == Items.AIR) {
				continue;
			}
			if (!icons.isEmpty()) {
				items.append(", ");
			}
			items.append(Component.translatable("encyclopedia.jugcraft.inspect.anchor.count", held.count(), new ItemStack(item).getHoverName()));
			icons.add(item);
		}
		if (!icons.isEmpty()) {
			out.fact(Component.translatable("encyclopedia.jugcraft.inspect.anchor.held", items), null, icons);
		}
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

	/**
	 * A Verdant Bed, or the crop on it (Concordance Chunk C, C-07, C-P14, C-P54): the bed's moisture and nutrients; dormant,
	 * or with no reading this tick, it says so; otherwise the bed's own reading (VerdantBedBlockEntity.inspect), each factor
	 * naming how far it reaches, and, with a crop or a Verdant Heart on it, its step and the verdict growth would reach from
	 * the same reading, each factor judged against that organism's niche (HabitatInspection). It notes, grows and marks nothing.
	 */
	private static void garden(ServerLevel level, BlockPos pos, Block looked, BlockPos bedPos, VerdantBedBlockEntity bed, PageWriter out) {
		out.heading(Component.translatable("encyclopedia.jugcraft.inspect.at", looked.getName(), pos.getX(), pos.getY(), pos.getZ()));
		out.fact(Component.translatable("message.jugcraft.concordance.garden.bed", bed.moisture(), bed.nutrients(), VerdantBedBlockEntity.CAPACITY));
		BlockState plant = level.getBlockState(bedPos.above());
		Organism organism = Garden.organism(plant);
		HabitatInspection.State state = bed.awake() ? bed.inspect(level) : null;
		if (!bed.awake()) {
			out.fact(Component.translatable("message.jugcraft.concordance.garden.dormant"));
		} else if (state == null) {
			out.fact(Garden.status("waiting"));
		} else if (organism == null) {
			out.heading(Component.translatable("encyclopedia.jugcraft.inspect.garden.here"));
			out.fact(ComposeText.show(HabitatInspection.unplanted()));
			facts(out, HabitatInspection.unjudged(state));
		} else {
			Niche niche = bed.niche(organism);
			Verdict verdict = Verdict.of(niche, state.habitat());
			if (plant.getBlock() instanceof OrganismCropBlock crop) {
				out.fact(Component.translatable("message.jugcraft.concordance.garden.crop", crop.getName(), crop.getAge(plant), OrganismCropBlock.MAX_AGE,
						ComposeText.show(HabitatInspection.pace(verdict, crop.isMaxAge(plant)))));
			}
			out.heading(Component.translatable("encyclopedia.jugcraft.inspect.garden.habitat", plant.getBlock().getName()));
			out.fact(ComposeText.show(HabitatInspection.verdict(verdict)));
			facts(out, HabitatInspection.judged(state, niche, verdict));
			// Concordance Chunk C (C-08, C-P58): a mixed bed's bonus (the strain is in the disturbance row).
			Text mixed = bed.mixedLine(organism);
			if (mixed != null) {
				out.fact(ComposeText.show(mixed));
			}
		}
		grafted(out, bed, looked instanceof OrganismCropBlock crop ? crop.organismId() : null);
		out.codex("garden/habitats", "jugcraft:verdant_husbandry", ResearchState.ENCOUNTERED);
		out.codex("garden/organisms", "jugcraft:verdant_husbandry", ResearchState.OBSERVED);
	}

	private static void facts(PageWriter out, List<Text> lines) {
		for (Text line : lines) {
			out.fact(ComposeText.show(line));
		}
	}

	/**
	 * An Alembic Crucible (C-01, C-P3): what anyone sees and its slots; the makeup only with an Assay Glass carried, at the
	 * level the glass would tell this reader (Alchemy.glassLevel). Reads the crucible's own reason; changes nothing.
	 */
	private static void crucible(ServerPlayer player, ServerLevel level, BlockPos pos, CrucibleBlockEntity crucible, PageWriter out) {
		out.heading(Component.translatable("encyclopedia.jugcraft.inspect.at", Component.translatable("block.jugcraft.crucible"),
				pos.getX(), pos.getY(), pos.getZ()));
		int reading = FirstLightRoute.carries(player, stack -> stack.is(JugcraftConcordance.ASSAY_GLASS)) ? Alchemy.glassLevel(player) : Assay.SPOON;
		Diagnostic reason = crucible.waiting();
		// Its mixture is not its formula's own batch when it was there before the formula was set (step 0) or the rules
		// refuse the formula: its own harm is then named too, since a dose of it can still be bottled by hand (Assay.risks).
		boolean apart = !crucible.mixture().isEmpty() && (crucible.step() == 0 || crucible.incompatibility() != null);
		CrucibleInspection.State state = new CrucibleInspection.State(crucible.temperature(), crucible.below(level),
				crucible.held(ConcordanceProgress.now(level)), crucible.mixture(), crucible.tankParts(), CrucibleBlockEntity.WATER_PER_BUCKET,
				crucible.program(), crucible.step(), reason == null ? null : reason.text(), apart);
		List<CrucibleInspection.Section> sections = CrucibleInspection.read(state, reading, Alchemy::harmfulStatus, Alchemy.catalog());
		lines(out, sections.getFirst());
		slots(crucible, out);
		if (reading < Assay.GLASS) {
			out.fact(Component.translatable("encyclopedia.jugcraft.inspect.crucible.glass"));
		}
		for (CrucibleInspection.Section section : sections.subList(1, sections.size())) {
			lines(out, section);
		}
		out.codex("alembic/crucible", "jugcraft:alembic_arts", ResearchState.ENCOUNTERED);
		out.codex("alembic/sampling", "jugcraft:alembic_arts", ResearchState.ENCOUNTERED);
		out.codex("alembic/formulas", "jugcraft:alembic_arts", ResearchState.UNDERSTOOD);
	}

	private static void lines(PageWriter out, CrucibleInspection.Section section) {
		if (section.heading() != null) {
			out.heading(ComposeText.show(section.heading()));
		}
		for (Text line : section.lines()) {
			out.fact(ComposeText.show(line));
		}
	}

	/** What waits in its buffer, its bottles or bowls, and what it has made: counts and icons, as anyone could look in. */
	private static void slots(CrucibleBlockEntity crucible, PageWriter out) {
		List<ItemStack> buffer = new ArrayList<>();
		for (int slot = 0; slot < CrucibleBlockEntity.BUFFER_SLOTS; slot++) {
			buffer.add(crucible.getItem(slot));
		}
		slot(out, "encyclopedia.jugcraft.inspect.crucible.buffer", buffer);
		slot(out, "encyclopedia.jugcraft.inspect.crucible.bottles", List.of(crucible.getItem(CrucibleBlockEntity.BOTTLE_SLOT)));
		slot(out, "encyclopedia.jugcraft.inspect.crucible.output", List.of(crucible.getItem(CrucibleBlockEntity.OUTPUT_SLOT)));
	}

	private static void slot(PageWriter out, String key, List<ItemStack> stacks) {
		MutableComponent held = Component.empty();
		List<Item> icons = new ArrayList<>();
		for (ItemStack stack : stacks) {
			if (stack.isEmpty()) {
				continue;
			}
			if (!icons.isEmpty()) {
				held.append(", ");
			}
			held.append(Component.translatable("encyclopedia.jugcraft.inspect.crucible.count", stack.getCount(), stack.getHoverName()));
			icons.add(stack.getItem());
		}
		out.fact(Component.translatable(key, icons.isEmpty() ? Component.translatable("encyclopedia.jugcraft.inspect.crucible.none") : held), null, icons);
	}

	/**
	 * Concordance Chunk C (C-09, C-P65): the graft a bed carries, when the rules know it, and on a crop's page only when the
	 * crop ({@code crop}, its organism; null on the bed's page) is the graft's host, as Jade names it: its name with its host
	 * and donor, then each range it changes against the host's own. The factor rows above already read the grafted niche
	 * (VerdantBedBlockEntity.niche). Reads only.
	 */
	private static void grafted(PageWriter out, VerdantBedBlockEntity bed, @Nullable String crop) {
		Graft held = Garden.catalog().graft(bed.graft());
		if (held == null || crop != null && !held.host().equals(crop)) {
			return;
		}
		List<Text> lines = Garden.catalog().graftLines(held.id());
		if (lines.isEmpty()) {
			return;
		}
		out.heading(ComposeText.show(lines.getFirst()));
		for (Text line : lines.subList(1, lines.size())) {
			out.fact(ComposeText.show(line));
		}
	}
}
