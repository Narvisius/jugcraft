package io.github.jimbozoomer.jugcraft.machine.form;

/**
 * How a form organizes its work (docs/features/industrial-factory-implementation-plan.md, operating profiles). A
 * profile sets how many batches one controller runs side by side (lanes), the size of each process tank and the
 * share of the unmodified working draw each lane pays. Lanes scale linearly: two lanes take two sets of inputs and
 * give two sets of outputs, each with its own reserved room. The figures are the plan's draft defaults.
 *
 * @param lanes independent batches at once
 * @param bufferMb millibuckets in each declared process tank
 * @param energyPercent percent of the unmodified energy per tick each running lane pays
 */
public enum OperatingProfile {
	ENTRY(1, 2_000, 100),
	EXPANDED(2, 8_000, 90),
	BULK(4, 16_000, 80);

	private final int lanes;
	private final int bufferMb;
	private final int energyPercent;

	OperatingProfile(int lanes, int bufferMb, int energyPercent) {
		this.lanes = lanes;
		this.bufferMb = bufferMb;
		this.energyPercent = energyPercent;
	}

	public int lanes() {
		return lanes;
	}

	public int bufferMb() {
		return bufferMb;
	}

	public int energyPercent() {
		return energyPercent;
	}

	/**
	 * JE per tick one running lane pays for a draw of {@code use} (already including upgrade cards), rounded up so a
	 * saving can never round a cost down to nothing.
	 */
	public long lanePerTick(long use) {
		if (use <= 0) {
			return 0;
		}
		return Math.max(1, (use * energyPercent + 99) / 100);
	}
}
