package io.github.jimbozoomer.jugcraft.train;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** The passenger coach (docs/features/trains.md): {@value #SEATS} seats, in two facing pairs. Use it to climb aboard. */
public class PassengerCar extends TrainCar {
	public static final double LENGTH = 4.0;
	public static final float WIDTH = 1.4F;
	public static final float HEIGHT = 2.65F;
	public static final int SEATS = 4;
	/** The seats, in blocks (x across, y up, z forwards). */
	private static final Vec3[] SEAT_SPOTS = {new Vec3(0.3, 1.05, 0.8), new Vec3(-0.3, 1.05, 0.8), new Vec3(0.3, 1.05, -0.8),
			new Vec3(-0.3, 1.05, -0.8)};

	public PassengerCar(EntityType<? extends PassengerCar> type, Level level) {
		super(type, level);
	}

	@Override
	public double length() {
		return LENGTH;
	}

	@Override
	public ItemStack asItem() {
		return new ItemStack(JugcraftTrains.PASSENGER_CAR_ITEM);
	}

	@Override
	public int seats() {
		return SEATS;
	}

	@Override
	protected Vec3 seat(int index) {
		return SEAT_SPOTS[index];
	}
}
