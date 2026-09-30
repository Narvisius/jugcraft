package io.github.jimbozoomer.jugcraft.kinetic;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Keeps a hand crank turning for a while after each right-click (see {@link HandCrankBlock}). */
public class HandCrankBlockEntity extends BlockEntity {
	/** KE per tick while turning. */
	public static final long OUTPUT = 16;
	public static final int TICKS_PER_CRANK = 100;
	public static final int MAX_TICKS = 400;
	/** Food exhaustion per right-click (sprinting uses 0.1 per meter). */
	public static final float EXHAUSTION = 0.5F;

	private int ticksLeft;

	public HandCrankBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftKinetics.HAND_CRANK_ENTITY, pos, state);
	}

	void crank(Player player) {
		addTurns(TICKS_PER_CRANK);
		player.causeFoodExhaustion(EXHAUSTION);
	}

	/** Keeps the crank turning for {@code ticks} more ticks, up to {@link #MAX_TICKS}. */
	public void addTurns(int ticks) {
		ticksLeft = Math.min(MAX_TICKS, ticksLeft + ticks);
		setChanged();
	}

	int secondsLeft() {
		return (ticksLeft + 19) / 20;
	}

	void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
		boolean turning = ticksLeft > 0;
		if (turning) {
			ticksLeft--;
			KineticNetworks.push(level, pos, state.getValue(HandCrankBlock.FACING), OUTPUT);
			setChanged();
		}
		if (state.getValue(ShaftBlock.TURNING) != turning) {
			level.setBlock(pos, state.setValue(ShaftBlock.TURNING, turning), Block.UPDATE_CLIENTS);
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		ticksLeft = input.getInt("ticks_left").orElse(0);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putInt("ticks_left", ticksLeft);
	}
}
