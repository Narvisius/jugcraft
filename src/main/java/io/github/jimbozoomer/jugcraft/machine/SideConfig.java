package io.github.jimbozoomer.jugcraft.machine;

import net.minecraft.core.Direction;

/**
 * Item routing for a processing machine, per face, relative to the machine's front: which faces
 * take ingredients in, which give results out, and whether the machine pushes (ejects) results into
 * neighbouring inventories and item pipes by itself, and how it reacts to redstone. Players change
 * it with the buttons on the machine's screen. Defaults match the old fixed behaviour: in from the top
 * and sides, out of the bottom, redstone ignored.
 */
public final class SideConfig {
	/** Faces in button order: 0 front, 1 back, 2 left, 3 right, 4 top, 5 bottom ("left" as seen from the front). */
	public enum Face {
		FRONT, BACK, LEFT, RIGHT, TOP, BOTTOM;

		public static Face of(Direction side, Direction facing) {
			if (side == Direction.UP) {
				return TOP;
			}
			if (side == Direction.DOWN) {
				return BOTTOM;
			}
			if (side == facing) {
				return FRONT;
			}
			if (side == facing.getOpposite()) {
				return BACK;
			}
			// Standing in front of a north-facing machine you look south: east is your left, west your right.
			return side == facing.getClockWise() ? LEFT : RIGHT;
		}
	}

	public enum Mode {
		INPUT, OUTPUT, BOTH, NONE;

		public boolean input() {
			return this == INPUT || this == BOTH;
		}

		public boolean output() {
			return this == OUTPUT || this == BOTH;
		}

		Mode next() {
			return values()[(ordinal() + 1) % values().length];
		}
	}

	/** Whether the machine may run given the redstone signal it receives. */
	public enum Redstone {
		/** Runs regardless of redstone. */
		IGNORED,
		/** Runs only while powered by redstone. */
		HIGH,
		/** Runs only while not powered. */
		LOW;

		public boolean allows(boolean powered) {
			return this == IGNORED || (this == HIGH) == powered;
		}

		Redstone next() {
			return values()[(ordinal() + 1) % values().length];
		}
	}

	/** Button id of the eject toggle; ids 0..5 are the faces. */
	public static final int EJECT_BUTTON = 6;
	/** Button id that cycles the redstone mode. */
	public static final int REDSTONE_BUTTON = 7;

	private final Mode[] modes = new Mode[Face.values().length];
	private boolean eject;
	private Redstone redstone = Redstone.IGNORED;

	public SideConfig() {
		reset();
	}

	private void reset() {
		for (Face face : Face.values()) {
			modes[face.ordinal()] = face == Face.BOTTOM ? Mode.OUTPUT : Mode.INPUT;
		}
		eject = false;
		redstone = Redstone.IGNORED;
	}

	public Mode mode(Face face) {
		return modes[face.ordinal()];
	}

	public Mode mode(Direction side, Direction facing) {
		return mode(Face.of(side, facing));
	}

	public boolean eject() {
		return eject;
	}

	public Redstone redstone() {
		return redstone;
	}

	/** Handles a screen button: cycle a face's mode, toggle ejecting or cycle the redstone mode. Returns false for unknown ids. */
	public boolean click(int button) {
		if (button == EJECT_BUTTON) {
			eject = !eject;
			return true;
		}
		if (button == REDSTONE_BUTTON) {
			redstone = redstone.next();
			return true;
		}
		if (button < 0 || button >= modes.length) {
			return false;
		}
		modes[button] = modes[button].next();
		return true;
	}

	/** Two bits per face, the eject bit and two redstone bits: 15 bits, so it fits one synced (16-bit) menu value. */
	public int pack() {
		int packed = (eject ? 1 << 12 : 0) | redstone.ordinal() << 13;
		for (int i = 0; i < modes.length; i++) {
			packed |= modes[i].ordinal() << (i * 2);
		}
		return packed;
	}

	public void unpack(int packed) {
		for (int i = 0; i < modes.length; i++) {
			modes[i] = Mode.values()[(packed >> (i * 2)) & 3];
		}
		eject = (packed & (1 << 12)) != 0;
		redstone = Redstone.values()[Math.min(2, (packed >> 13) & 3)];
	}

	/** Reads a packed value back without a machine (for the screen). */
	public static SideConfig of(int packed) {
		SideConfig config = new SideConfig();
		config.unpack(packed);
		return config;
	}

	/**
	 * Every face gives results out and the machine pushes them into whatever inventory or pipe touches it: for
	 * machines with no inputs, such as the deposit drill, so a chest beside one fills without any setup.
	 */
	public static SideConfig allOutputs() {
		SideConfig config = new SideConfig();
		java.util.Arrays.fill(config.modes, Mode.OUTPUT);
		config.eject = true;
		return config;
	}

	/** The default config packed, for saves written before side configuration existed. */
	public static int defaults() {
		return new SideConfig().pack();
	}
}
