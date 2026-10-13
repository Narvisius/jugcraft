package io.github.jimbozoomer.jugcraft.concordance.garden;

import io.github.jimbozoomer.jugcraft.concordance.ecology.Factor;
import io.github.jimbozoomer.jugcraft.concordance.ecology.Habitat;
import io.github.jimbozoomer.jugcraft.concordance.ecology.Organism;
import io.github.jimbozoomer.jugcraft.concordance.ecology.Verdict;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The Habitat Gauge (roadmap step 14): the garden's sensor. Set on a Verdant Bed where a plant would be, it reads the
 * habitat there every {@value #GAUGE_TICKS} ticks (through the bed's sample, so it costs no more than a plant) and
 * gives a comparator signal: in {@link Mode#SUITABILITY}, how well its attuned organism would grow (15 thriving, 8
 * slowly, 0 not at all); in a factor's mode, that factor scaled to 0 to 15. The gauge itself is not a plant, so the
 * diversity it reads is its neighbours'.
 */
public class HabitatGaugeBlockEntity extends LivingDeviceBlockEntity {
	public static final int GAUGE_TICKS = 40;
	public static final int THRIVING = 15;
	public static final int TOLERATING = 8;

	public enum Mode {
		SUITABILITY("suitability", null),
		MOISTURE("moisture", Factor.MOISTURE),
		LIGHT("light", Factor.LIGHT),
		NUTRIENTS("nutrients", Factor.NUTRIENTS),
		DIVERSITY("diversity", Factor.DIVERSITY),
		DISTURBANCE("disturbance", Factor.DISTURBANCE);

		public final String id;
		public final @Nullable Factor factor;

		Mode(String id, @Nullable Factor factor) {
			this.id = id;
			this.factor = factor;
		}

		public Mode next() {
			return values()[(ordinal() + 1) % values().length];
		}
	}

	private Mode mode = Mode.SUITABILITY;
	/** The organism suitability is judged for ("" until a crop is used on it). */
	private String attuned = "";
	private int signal;

	public HabitatGaugeBlockEntity(BlockPos pos, BlockState state) {
		super(Garden.GAUGE_ENTITY, pos, state);
	}

	public Mode mode() {
		return mode;
	}

	public String attuned() {
		return attuned;
	}

	public int signal() {
		return signal;
	}

	public void cycle(ServerLevel level) {
		mode = mode.next();
		setChanged();
		read(level);
	}

	public void attune(ServerLevel level, String organism) {
		attuned = organism;
		mode = Mode.SUITABILITY;
		setChanged();
		read(level);
	}

	void serverTick(ServerLevel level) {
		if (due(level, GAUGE_TICKS)) {
			read(level);
		}
	}

	/** Reads the habitat now and sets the signal (and the bulb); returns the signal. */
	public int read(ServerLevel level) {
		int value = 0;
		if (!Garden.enabled()) {
			status("disabled");
		} else if (!awake) {
			status("dormant");
		} else if (!(level.getBlockEntity(worldPosition.below()) instanceof VerdantBedBlockEntity bed)) {
			status("no_bed");
		} else {
			Habitat habitat = bed.habitat(level);
			if (habitat == null) {
				status("waiting");
			} else if (mode.factor != null) {
				value = Math.min(15, habitat.get(mode.factor) * 15 / mode.factor.max);
				status("working");
			} else {
				Organism organism = Garden.catalog().organism(attuned);
				if (organism == null) {
					status("idle");
				} else {
					Verdict verdict = Verdict.of(organism.niche(), habitat);
					value = switch (verdict.growth()) {
						case THRIVING -> THRIVING;
						case TOLERATING -> TOLERATING;
						case STALLED -> 0;
					};
					status("working");
				}
			}
		}
		if (value != signal) {
			signal = value;
			setChanged();
			BlockState state = getBlockState();
			boolean open = signal > 0;
			if (state.getValue(HabitatGaugeBlock.OPEN) != open) {
				level.setBlock(worldPosition, state.setValue(HabitatGaugeBlock.OPEN, open), Block.UPDATE_ALL);
			}
			level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
			sync();
		}
		return signal;
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		String saved = input.getStringOr("mode", Mode.SUITABILITY.id);
		mode = Mode.SUITABILITY;
		for (Mode each : Mode.values()) {
			if (each.id.equals(saved)) {
				mode = each;
			}
		}
		attuned = input.getStringOr("attuned", "");
		signal = Math.clamp(input.getIntOr("signal", 0), 0, 15);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putString("mode", mode.id);
		output.putString("attuned", attuned);
		output.putInt("signal", signal);
	}

	@Override
	protected void clientData(CompoundTag tag) {
		tag.putString("mode", mode.id);
		tag.putString("attuned", attuned);
		tag.putInt("signal", signal);
	}
}
