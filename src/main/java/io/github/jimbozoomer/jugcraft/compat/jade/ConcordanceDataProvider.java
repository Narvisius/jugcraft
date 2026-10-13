package io.github.jimbozoomer.jugcraft.compat.jade;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.CircleAnchorBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.CrucibleBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.JugcraftConcordance;
import io.github.jimbozoomer.jugcraft.concordance.KindledLanternItem;
import io.github.jimbozoomer.jugcraft.concordance.LampwrightBenchBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.LeyPylonBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.LumenSconceBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.alchemy.Formula;
import io.github.jimbozoomer.jugcraft.concordance.celestial.Pattern;
import io.github.jimbozoomer.jugcraft.concordance.ecology.Habitat;
import io.github.jimbozoomer.jugcraft.concordance.garden.GleanerBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.garden.HabitatGaugeBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.garden.LivingDeviceBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.garden.MulchMawBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.garden.VerdantBedBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.garden.VerdantHeartBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.ritual.StructurePattern;
import io.github.jimbozoomer.jugcraft.concordance.ritual.StructureValidator;
import io.github.jimbozoomer.jugcraft.concordance.sky.ObservatoryBlockEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

/**
 * The Concordance's blocks for Jade: the Lampwright's Bench (how far its study has come, whether notes wait for
 * someone, the Radiance in the lantern on it), the Lumen Sconce (its Radiance), the Circle Anchor (its ritual's phase
 * and progress and the circle's first faults), the Ley Pylon (its Ley Charge), the Alembic Crucible (its heat,
 * volume and formula step), the garden (roadmap step 14: a bed's moisture, nutrients and habitat; each living
 * device's status and store) and the Orrery Observatory (roadmap step 15: its resonance, status and visible patterns). Bounded snapshots of the one block looked at; they name no player, reveal no one's
 * research and change nothing (a bed's habitat comes from its sample, taken again no more often than a plant would).
 */
public enum ConcordanceDataProvider implements IServerDataProvider<BlockAccessor> {
	BENCH("lampwright_bench"),
	SCONCE("lumen_sconce"),
	ANCHOR("circle_anchor"),
	PYLON("ley_pylon"),
	CRUCIBLE("crucible"),
	BED("verdant_bed"),
	HEART("verdant_heart"),
	MAW("mulch_maw"),
	GAUGE("habitat_gauge"),
	GLEANER("gleaner"),
	OBSERVATORY("observatory");

	/** The most patterns an observatory's tooltip names. */
	public static final int SHOWN_PATTERNS = 3;

	/** The most structural faults the anchor's tooltip lists (the command lists them all). */
	public static final int SHOWN_FAULTS = 3;

	public final Identifier id;
	public final String dataKey;

	ConcordanceDataProvider(String path) {
		this.id = Jugcraft.id(path);
		this.dataKey = Jugcraft.MOD_ID + ":" + path;
	}

	@Override
	public Identifier getUid() {
		return id;
	}

	@Override
	public void appendServerData(CompoundTag data, BlockAccessor accessor) {
		long now = accessor.getLevel().getGameTime();
		if (this == BENCH && accessor.getBlockEntity() instanceof LampwrightBenchBlockEntity bench) {
			data.put(dataKey, snapshot(bench, now));
		} else if (this == SCONCE && accessor.getBlockEntity() instanceof LumenSconceBlockEntity sconce) {
			data.put(dataKey, snapshot(sconce, now));
		} else if (this == ANCHOR && accessor.getBlockEntity() instanceof CircleAnchorBlockEntity anchor
				&& accessor.getLevel() instanceof ServerLevel level) {
			data.put(dataKey, snapshot(anchor, level));
		} else if (this == CRUCIBLE && accessor.getBlockEntity() instanceof CrucibleBlockEntity crucible) {
			data.put(dataKey, snapshot(crucible));
		} else if (this == BED && accessor.getBlockEntity() instanceof VerdantBedBlockEntity bed && accessor.getLevel() instanceof ServerLevel level) {
			data.put(dataKey, bedSnapshot(bed, level));
		} else if (accessor.getBlockEntity() instanceof LivingDeviceBlockEntity device
				&& (this == HEART || this == MAW || this == GAUGE || this == GLEANER)) {
			data.put(dataKey, deviceSnapshot(device));
		} else if (this == OBSERVATORY && accessor.getBlockEntity() instanceof ObservatoryBlockEntity observatory
				&& accessor.getLevel() instanceof ServerLevel level) {
			data.put(dataKey, observatorySnapshot(observatory, level));
		} else if (this == PYLON && accessor.getBlockEntity() instanceof LeyPylonBlockEntity pylon) {
			CompoundTag snapshot = new CompoundTag();
			snapshot.putInt("ley", (int) pylon.ley());
			data.put(dataKey, snapshot);
		}
	}

	public static CompoundTag snapshot(LampwrightBenchBlockEntity bench, long now) {
		CompoundTag snapshot = new CompoundTag();
		if (bench.studying()) {
			snapshot.putInt("study", Math.clamp(100 * bench.progress() / LampwrightBenchBlockEntity.STUDY_TICKS, 0, 100));
		}
		if (bench.notesWaiting()) {
			snapshot.putInt("notes", 1);
		}
		ItemStack work = bench.getItem(LampwrightBenchBlockEntity.WORK);
		if (work.is(JugcraftConcordance.KINDLED_LANTERN)) {
			snapshot.putInt("radiance", KindledLanternItem.remaining(work, now));
		}
		return snapshot;
	}

	/**
	 * The anchor: its phase and progress, how many have joined, and its first faults as {@code role|problem|x,y,z}
	 * (from the anchor's cached report: looking at it never rescans the circle more than the anchor would).
	 */
	public static CompoundTag snapshot(CircleAnchorBlockEntity anchor, ServerLevel level) {
		CompoundTag snapshot = new CompoundTag();
		snapshot.putString("phase", anchor.phase().id);
		snapshot.putInt("joined", anchor.run().joined().size());
		snapshot.putInt("done", anchor.run().done());
		snapshot.putInt("steps", anchor.steps());
		StructureValidator.Report report = anchor.report(level);
		if (report != null) {
			snapshot.putInt("faults", report.faults().size());
			for (int i = 0; i < Math.min(SHOWN_FAULTS, report.faults().size()); i++) {
				StructureValidator.Fault fault = report.faults().get(i);
				StructurePattern.Offset offset = fault.part().offset();
				BlockPos at = anchor.getBlockPos().offset(offset.x(), offset.y(), offset.z());
				snapshot.putString("fault" + i, fault.part().role().id + "|" + fault.problem().id + "|" + at.getX() + "," + at.getY() + "," + at.getZ());
			}
		}
		return snapshot;
	}

	/**
	 * The crucible: temperature and band, volume, and, following a formula, its step and what it waits for (the next
	 * operation's text). Never the mixture's makeup: that takes sampling.
	 */
	public static CompoundTag snapshot(CrucibleBlockEntity crucible) {
		CompoundTag snapshot = new CompoundTag();
		snapshot.putInt("temperature", crucible.temperature());
		snapshot.putString("band", crucible.band().id);
		snapshot.putInt("parts", crucible.parts());
		Formula program = crucible.program();
		if (program != null) {
			snapshot.putInt("step", crucible.step());
			snapshot.putInt("steps", program.operations().size());
			if (crucible.step() < program.operations().size()) {
				snapshot.putString("next", program.operations().get(crucible.step()).text());
			}
		}
		return snapshot;
	}

	/** A Verdant Bed: moisture, nutrients, whether it is awake, and its habitat (from its sample). */
	public static CompoundTag bedSnapshot(VerdantBedBlockEntity bed, ServerLevel level) {
		CompoundTag snapshot = new CompoundTag();
		snapshot.putInt("nutrients", bed.nutrients());
		snapshot.putInt("capacity", VerdantBedBlockEntity.CAPACITY);
		snapshot.putBoolean("awake", bed.awake());
		Habitat habitat = bed.awake() ? bed.habitat(level) : null;
		if (habitat != null) {
			snapshot.putIntArray("habitat", new int[] {habitat.moisture(), habitat.light(), habitat.nutrients(), habitat.diversity(),
					habitat.disturbance()});
		}
		snapshot.putString("growth", bed.growth());
		snapshot.putString("reasons", String.join("\n", bed.reasons()));
		return snapshot;
	}

	/** A living device: its status, and its Verdance, digestion or reading. */
	public static CompoundTag deviceSnapshot(LivingDeviceBlockEntity device) {
		CompoundTag snapshot = new CompoundTag();
		snapshot.putString("device", device.status());
		snapshot.putBoolean("awake", device.awake());
		if (device instanceof VerdantHeartBlockEntity heart) {
			snapshot.putLong("verdance", heart.verdance());
			snapshot.putInt("verdance_capacity", VerdantHeartBlockEntity.CAPACITY);
		} else if (device instanceof GleanerBlockEntity gleaner) {
			snapshot.putLong("verdance", gleaner.verdance());
			snapshot.putInt("verdance_capacity", GleanerBlockEntity.CAPACITY);
		} else if (device instanceof MulchMawBlockEntity maw) {
			snapshot.putInt("held", maw.held());
		} else if (device instanceof HabitatGaugeBlockEntity gauge) {
			snapshot.putString("mode", gauge.mode().id);
			snapshot.putInt("signal", gauge.signal());
		}
		return snapshot;
	}

	/**
	 * An observatory: its resonance, why it is idle, whether a Starwatcher has aligned it, and the patterns up and visible
	 * from it now (the server's world clock, weather and sky; never the client's).
	 */
	public static CompoundTag observatorySnapshot(ObservatoryBlockEntity observatory, ServerLevel level) {
		CompoundTag snapshot = new CompoundTag();
		snapshot.putString("sky", observatory.status());
		snapshot.putBoolean("aligned", observatory.aligned());
		snapshot.putLong("resonance", observatory.resonance());
		snapshot.putInt("resonance_capacity", ObservatoryBlockEntity.CAPACITY);
		List<String> visible = new ArrayList<>();
		for (Pattern pattern : observatory.visible(level)) {
			if (visible.size() < SHOWN_PATTERNS) {
				visible.add(pattern.id());
			}
		}
		snapshot.putString("visible", String.join("\n", visible));
		return snapshot;
	}

	public static CompoundTag snapshot(LumenSconceBlockEntity sconce, long now) {
		CompoundTag snapshot = new CompoundTag();
		snapshot.putInt("radiance", (int) sconce.remaining(now));
		return snapshot;
	}
}
