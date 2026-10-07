package io.github.jimbozoomer.jugcraft.nuclear;

/**
 * A container built to hold radioactive material (the lead pig, the lead-lined locker, the shielded waste drum):
 * only this share of what it holds gets out (Radiation, RadiationField). Ordinary chests let 80% out.
 */
public interface ShieldedContainer {
	/** The share of its contents' radiation that gets out (0..1). */
	double leakage();

	/** The same for a container's block, so its item (a lead pig carried with its contents) is shielded too. */
	interface Block {
		double leakage();
	}
}
