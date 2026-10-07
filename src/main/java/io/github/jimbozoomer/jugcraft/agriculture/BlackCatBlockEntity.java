package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Watches for runners for a {@link BlackCatBlock}: every {@value #PERIOD} ticks it looks at the level's players (no
 * entity search). A sprinting player within {@link BlackCatBlock#REACH} blocks, not a spectator, sets it hissing; it
 * settles when its time is up and won't hiss again until its cooldown is over. Both times are saved.
 */
public class BlackCatBlockEntity extends BlockEntity {
	public static final int PERIOD = 5;

	private long calmAt;
	private long readyAt;

	public BlackCatBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftAgriculture.BLACK_CAT_ENTITY, pos, state);
	}

	void serverTick(ServerLevel level) {
		if (Math.floorMod(level.getGameTime() + worldPosition.asLong(), PERIOD) == 0) {
			update(level);
		}
	}

	/** Starts or stops hissing for who is running past; returns whether it hisses. */
	public boolean update(ServerLevel level) {
		BlockState state = getBlockState();
		if (!(state.getBlock() instanceof BlackCatBlock)) {
			return false;
		}
		long time = level.getGameTime();
		boolean hissing = state.getValue(BlackCatBlock.HISSING);
		if (!hissing && time >= readyAt && runnerNear(level)) {
			hiss(level);
			return true;
		}
		if (hissing && time >= calmAt) {
			level.setBlock(worldPosition, state.setValue(BlackCatBlock.HISSING, false), Block.UPDATE_ALL);
			setChanged();
			return false;
		}
		return hissing;
	}

	/** Arches its back and hisses now, for {@link BlackCatBlock#HISS_TICKS} ticks, then rests. */
	public void hiss(ServerLevel level) {
		long time = level.getGameTime();
		calmAt = time + BlackCatBlock.HISS_TICKS;
		readyAt = calmAt + BlackCatBlock.COOLDOWN_TICKS;
		level.setBlock(worldPosition, getBlockState().setValue(BlackCatBlock.HISSING, true), Block.UPDATE_ALL);
		level.playSound(null, worldPosition, SoundEvents.CREEPER_PRIMED, SoundSource.BLOCKS, 1.0F, 1.6F);
		setChanged();
	}

	private boolean runnerNear(ServerLevel level) {
		Vec3 centre = Vec3.atCenterOf(worldPosition);
		for (ServerPlayer player : level.players()) {
			if (!player.isSpectator() && player.isSprinting() && player.distanceToSqr(centre) <= BlackCatBlock.REACH * BlackCatBlock.REACH) {
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
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putLong("calm_at", calmAt);
		output.putLong("ready_at", readyAt);
	}
}
