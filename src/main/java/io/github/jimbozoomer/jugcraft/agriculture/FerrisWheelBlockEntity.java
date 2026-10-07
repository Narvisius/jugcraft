package io.github.jimbozoomer.jugcraft.agriculture;

import io.github.jimbozoomer.jugcraft.kinetic.KineticConsumer;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Ferris Wheel booth's drive (fall addition 27): it takes kinetic energy from any shaft, gearbox or source touching
 * the booth, up to {@value FerrisWheel#NEED} KE a tick in all, and hands it to the wheel over the booth, which turns as
 * fast as that allows ({@link FerrisWheel#power}). It takes nothing while there is no wheel. The energy goes into the
 * turning, so nothing comes back out; it keeps nothing between ticks and saves nothing.
 */
public class FerrisWheelBlockEntity extends BlockEntity implements KineticConsumer {
	private long tick = -1;
	private long taken;

	public FerrisWheelBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftAgriculture.FERRIS_WHEEL_BOOTH, pos, state);
	}

	@Override
	public long acceptKinetic(Direction side, long maxAmount) {
		if (!(level instanceof ServerLevel server)) {
			return 0;
		}
		long now = server.getGameTime();
		if (now != tick) {
			tick = now;
			taken = 0;
		}
		long take = Math.min(maxAmount, FerrisWheel.NEED - taken);
		if (take <= 0) {
			return 0;
		}
		List<FerrisWheel> wheels = FerrisWheelBlock.wheels(server, worldPosition);
		if (wheels.isEmpty()) {
			return 0;
		}
		taken += take;
		wheels.get(0).power(take);
		return take;
	}
}
