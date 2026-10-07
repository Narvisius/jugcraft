package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Watches for visitors for a {@link PorchWitchBlock}, on its lower half: every {@value #PERIOD} ticks it looks at the
 * level's players (no entity search). When someone (not a spectator) comes within {@link PorchWitchBlock#REACH} blocks
 * and nobody was there before, she cackles, if she is rested; she settles when her time is up. Someone who stays
 * doesn't set her off again: they have to go and come back. Her times and whether anyone is near are saved.
 */
public class PorchWitchBlockEntity extends BlockEntity {
	public static final int PERIOD = 5;

	private long calmAt;
	private long readyAt;
	private boolean someoneNear;

	public PorchWitchBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftAgriculture.PORCH_WITCH_ENTITY, pos, state);
	}

	void serverTick(ServerLevel level) {
		if (Math.floorMod(level.getGameTime() + worldPosition.asLong(), PERIOD) == 0) {
			update(level);
		}
	}

	/** Starts or stops cackling for who has come up; returns whether she cackles. */
	public boolean update(ServerLevel level) {
		BlockState state = getBlockState();
		if (!(state.getBlock() instanceof PorchWitchBlock)) {
			return false;
		}
		long time = level.getGameTime();
		boolean cackling = state.getValue(PorchWitchBlock.CACKLING);
		boolean near = visitorNear(level);
		boolean arrived = near && !someoneNear;
		if (near != someoneNear) {
			someoneNear = near;
			setChanged();
		}
		if (!cackling && arrived && time >= readyAt) {
			cackle(level);
			return true;
		}
		if (cackling && time >= calmAt) {
			TallDecorationBlock.setBoth(level, worldPosition, state.setValue(PorchWitchBlock.CACKLING, false));
			setChanged();
			return false;
		}
		return cackling;
	}

	/** Throws her head back and cackles now, for {@link PorchWitchBlock#CACKLE_TICKS} ticks, then rests. */
	public void cackle(ServerLevel level) {
		long time = level.getGameTime();
		calmAt = time + PorchWitchBlock.CACKLE_TICKS;
		readyAt = calmAt + PorchWitchBlock.COOLDOWN_TICKS;
		TallDecorationBlock.setBoth(level, worldPosition, getBlockState().setValue(PorchWitchBlock.CACKLING, true));
		level.playSound(null, worldPosition.above(), SoundEvents.WITCH_CELEBRATE, SoundSource.BLOCKS, 1.0F, 1.1F);
		setChanged();
	}

	private boolean visitorNear(ServerLevel level) {
		Vec3 centre = Vec3.atCenterOf(worldPosition);
		for (ServerPlayer player : level.players()) {
			if (!player.isSpectator() && player.distanceToSqr(centre) <= PorchWitchBlock.REACH * PorchWitchBlock.REACH) {
				return true;
			}
		}
		return false;
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		calmAt = input.getLongOr("calm_at", 0L);
		readyAt = input.getLongOr("ready_at", 0L);
		someoneNear = input.getBooleanOr("someone_near", false);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putLong("calm_at", calmAt);
		output.putLong("ready_at", readyAt);
		output.putBoolean("someone_near", someoneNear);
	}
}
