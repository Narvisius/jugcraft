package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The Grandfather Clock's works, on its lower half: each tick it reads the overworld clock, and when the hour turns it
 * pulses and begins to strike the hour, a chime every {@value GrandfatherClockBlock#STRIKE_GAP} ticks. A clock placed
 * (or loaded) mid-hour waits for the next. The hour it last struck is saved.
 */
public class GrandfatherClockBlockEntity extends BlockEntity {
	private static final long UNSET = Long.MIN_VALUE;
	private long lastHour = UNSET;
	private int strikesLeft;
	private long nextStrike;

	public GrandfatherClockBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftAgriculture.GRANDFATHER_CLOCK_ENTITY, pos, state);
	}

	public int strikesLeft() {
		return strikesLeft;
	}

	public long lastHour() {
		return lastHour;
	}

	void serverTick(ServerLevel level, BlockPos pos) {
		advance(level, pos, level.getOverworldClockTime());
	}

	/** Moves the works on to {@code dayTime} (the tick above, and tests): a new hour pulses and strikes; strikes ring out. */
	public void advance(ServerLevel level, BlockPos pos, long dayTime) {
		long hour = GrandfatherClockBlock.hourIndex(dayTime);
		long now = level.getGameTime();
		if (lastHour == UNSET) {
			lastHour = hour;
			setChanged();
		} else if (hour != lastHour) {
			lastHour = hour;
			strikesLeft = GrandfatherClockBlock.clockHour(dayTime);
			nextStrike = now;
			setChanged();
			GrandfatherClockBlock.pulse(level, pos);
		}
		if (strikesLeft > 0 && now >= nextStrike) {
			strikesLeft--;
			nextStrike = now + GrandfatherClockBlock.STRIKE_GAP;
			level.playSound(null, pos.above(), SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, 1.2F, 0.5F);
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		lastHour = input.getLongOr("last_hour", UNSET);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		if (lastHour != UNSET) {
			output.putLong("last_hour", lastHour);
		}
	}
}
