package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The High Striker's base (part 0): where the puck is going ({@code target}, a {@link HighStrikerBlock#LEVEL}), whether it
 * is on its way down, and who struck it, so the bell's prize goes to them. Saved, so a strike in flight finishes after a
 * restart.
 */
public class HighStrikerBlockEntity extends BlockEntity {
	private int target;
	private boolean falling;
	private @Nullable UUID striker;

	public HighStrikerBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftAgriculture.HIGH_STRIKER_ENTITY, pos, state);
	}

	/** Whether a puck is in flight (struck and not yet back at the bottom). */
	public boolean busy() {
		return target > 0 || getBlockState().getValue(HighStrikerBlock.LEVEL) > 0;
	}

	public int target() {
		return target;
	}

	/** Sends the puck up to {@code level} for {@code player}, if it is at rest; returns whether it went. */
	public boolean launch(ServerLevel level, int puck, @Nullable ServerPlayer player) {
		if (busy() || puck <= 0) {
			return false;
		}
		target = Math.min(puck, HighStrikerBlock.RUNG);
		falling = false;
		striker = player == null ? null : player.getUUID();
		setChanged();
		level.scheduleTick(worldPosition, getBlockState().getBlock(), HighStrikerBlock.RISE_TICKS);
		return true;
	}

	/** One step of the animation, on the base's scheduled tick. */
	public void step(ServerLevel level, BlockState state) {
		if (!(state.getBlock() instanceof HighStrikerBlock block)) {
			return;
		}
		int puck = state.getValue(HighStrikerBlock.LEVEL);
		if (falling) {
			puck--;
			block.setLevel(level, worldPosition, Math.max(0, puck));
			if (puck > 0) {
				level.scheduleTick(worldPosition, block, 1);
			} else {
				falling = false;
				target = 0;
				striker = null;
			}
		} else if (puck < target) {
			puck++;
			block.setLevel(level, worldPosition, puck);
			Midway.climb(level, worldPosition, puck);
			if (puck == HighStrikerBlock.RUNG) {
				Midway.ring(level, worldPosition, striker != null && level.getEntity(striker) instanceof ServerPlayer player ? player : null);
			}
			level.scheduleTick(worldPosition, block, puck < target ? HighStrikerBlock.RISE_TICKS : HighStrikerBlock.HOLD_TICKS);
			falling = puck >= target;
		} else {
			// Nothing to climb to (a restart between steps): fall back.
			falling = true;
			level.scheduleTick(worldPosition, block, 1);
		}
		setChanged();
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		target = Math.clamp(input.getIntOr("target", 0), 0, HighStrikerBlock.RUNG);
		falling = input.getBooleanOr("falling", false);
		striker = input.read("striker", UUIDUtil.CODEC).orElse(null);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putInt("target", target);
		output.putBoolean("falling", falling);
		if (striker != null) {
			output.store("striker", UUIDUtil.CODEC, striker);
		}
	}
}
