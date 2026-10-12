package io.github.jimbozoomer.jugcraft.lair;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A lair: a pocket dimension of its own, into whose instance slots its structure template is placed fresh by
 * {@link Lairs}. Every position here is relative to the template's corner; the numbers are its layout tool's
 * (tools/hollow_acre.py, tools/spindle_loft.py, tools/glacier_hall.py, tools/cinder_kiln.py; checked by
 * tools/check_mod_data.py).
 */
public enum Lair {
	/** Vesperine's lair (docs/features/hollow-acre.md). */
	HOLLOW_ACRE("hollow_acre", 64, 48, 76, new Vec3(32.0, 17.0, 66.5), 180.0F, 32.0, 38.0, 44, -8, new BlockPos(32, 46, -30), 13),
	/** Madame Tatterlace's lair (docs/features/spindle-loft.md): indoors, so no moon. */
	SPINDLE_LOFT("spindle_loft", 81, 64, 100, new Vec3(40.5, 38.0, 82.5), 180.0F, 40.5, 56.5, 48, 22, null, 0),
	/** The Yeti King's lair (docs/features/glacier-hall.md): a cavern in a glacier, so no moon. */
	GLACIER_HALL("glacier_hall", 80, 44, 88, new Vec3(39.5, 15.0, 81.5), 180.0F, 39.5, 43.5, 46, 2, null, 0),
	/** The Cinder Tyrant's lair (docs/features/cinder-kiln.md): a kiln under a volcano, its vent open to a smoky sky. */
	CINDER_KILN("cinder_kiln", 72, 46, 80, new Vec3(41.5, 13.0, 63.5), 165.0F, 35.5, 38.5, 40, 1, null, 0);

	/** Blocks between one instance slot and the next, along X (tools/lairs.py SPACING). */
	public static final int SPACING = 1024;
	/** The template's lowest layer (tools/lairs.py BASE_Y). */
	public static final int BASE_Y = 64;

	public final String id;
	public final ResourceKey<Level> dimension;
	public final Identifier template;
	public final int width;
	public final int height;
	public final int length;
	/** Where players arrive, and which way they face (yaw). */
	public final Vec3 arrival;
	public final float arrivalYaw;
	/** The lair's centre (x, z): players farther than {@link #bounds} from it, or below {@link #floor}, are thrown back. */
	public final double centreX;
	public final double centreZ;
	public final int bounds;
	public final int floor;
	/** The moon's centre and radius (it faces south), placed with each instance; null for a lair with no moon. */
	public final @Nullable BlockPos moon;
	public final int moonRadius;

	Lair(String id, int width, int height, int length, Vec3 arrival, float arrivalYaw, double centreX, double centreZ, int bounds,
			int floor, @Nullable BlockPos moon, int moonRadius) {
		this.id = id;
		this.dimension = ResourceKey.create(Registries.DIMENSION, Jugcraft.id(id));
		this.template = Jugcraft.id("lair/" + id);
		this.width = width;
		this.height = height;
		this.length = length;
		this.arrival = arrival;
		this.arrivalYaw = arrivalYaw;
		this.centreX = centreX;
		this.centreZ = centreZ;
		this.bounds = bounds;
		this.floor = floor;
		this.moon = moon;
		this.moonRadius = moonRadius;
	}

	/** The template's corner in slot {@code slot}: the lair's centre lands on x = slot * SPACING, z = 0. */
	public BlockPos origin(int slot) {
		return new BlockPos(slot * SPACING - (int) centreX, BASE_Y, -(int) centreZ);
	}

	/** Which slot a world position falls in (the nearest). */
	public int slotAt(double x) {
		return (int) Math.floor((x + SPACING / 2.0) / SPACING);
	}

	public static @Nullable Lair of(ResourceKey<Level> dimension) {
		for (Lair lair : values()) {
			if (lair.dimension.equals(dimension)) {
				return lair;
			}
		}
		return null;
	}

	public static @Nullable Lair byId(String id) {
		for (Lair lair : values()) {
			if (lair.id.equals(id)) {
				return lair;
			}
		}
		return null;
	}
}
