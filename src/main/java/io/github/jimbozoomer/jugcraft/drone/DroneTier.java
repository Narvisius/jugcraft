package io.github.jimbozoomer.jugcraft.drone;

import java.util.Locale;
import java.util.Optional;

/**
 * Drone tiers. Each tier improves capacity or speed over the one before, and its recipe consumes the
 * previous tier's drone. Numbers are kept in sync with {@code tools/drones.py} (checked by
 * {@code tools/check_mod_data.py}). Every tier has an item, a recipe and a 3D look; a depot can use a tier
 * only once its Drone Tower has reached that tier (see docs/features/drone-tower.md).
 */
public enum DroneTier {
	//        blocks/trip  speed (blocks/s)  upkeep (JE/t)  size             craftable now
	COURIER_QUAD(1, 4, 8, DroneSize.SMALL, true),
	SURVEY_HEXACOPTER(2, 4, 12, DroneSize.SMALL, true),
	LIFTER_OCTOCOPTER(4, 4, 20, DroneSize.SMALL, true),
	DUCTED_FAN_RUNNER(4, 8, 32, DroneSize.MEDIUM, true),
	TILTROTOR_CARRIER(8, 8, 48, DroneSize.MEDIUM, true),
	TANDEM_FREIGHTER(16, 8, 64, DroneSize.MEDIUM, true),
	HYBRID_AEROSTAT(32, 8, 96, DroneSize.LARGE, true),
	ION_WIND_GLIDER(32, 16, 128, DroneSize.LARGE, true),
	SUPERCONDUCTING_LIFTER(64, 20, 192, DroneSize.LARGE, true),
	/** Creative only (no recipe): a small quad with four times tier 9's load and speed that needs no power. */
	CREATIVE_DRONE(256, 80, 0, DroneSize.SMALL, true);

	private final int capacity;
	private final int speed;
	private final int upkeep;
	private final DroneSize size;
	private final boolean available;

	DroneTier(int capacity, int speed, int upkeep, DroneSize size, boolean available) {
		this.capacity = capacity;
		this.speed = speed;
		this.upkeep = upkeep;
		this.size = size;
		this.available = available;
	}

	/** Tier number, 1 to 9 (10: the Creative Drone). */
	public int number() {
		return ordinal() + 1;
	}

	/** The creative-only drone: any depot with a tower may fly it, and it draws no power. */
	public boolean creative() {
		return this == CREATIVE_DRONE;
	}

	/** Item and data ID, for example {@code drone_t1} ({@code creative_drone} for the Creative Drone). */
	public String id() {
		return creative() ? "creative_drone" : "drone_t" + number();
	}

	/** Blocks carried per trip. */
	public int capacity() {
		return capacity;
	}

	/** Cruise speed in blocks per second. */
	public int speed() {
		return speed;
	}

	/** Energy the depot draws for this drone while the fleet is working (JE per tick). */
	public int upkeep() {
		return upkeep;
	}

	public DroneSize size() {
		return size;
	}

	/** Whether this tier has an item and recipe in this version. */
	public boolean available() {
		return available;
	}

	public String displayKey() {
		return "item.jugcraft." + id();
	}

	public static Optional<DroneTier> byNumber(int number) {
		return number >= 1 && number <= values().length ? Optional.of(values()[number - 1]) : Optional.empty();
	}

	public static Optional<DroneTier> byId(String id) {
		for (DroneTier tier : values()) {
			if (tier.id().equals(id.toLowerCase(Locale.ROOT))) {
				return Optional.of(tier);
			}
		}
		return Optional.empty();
	}
}
