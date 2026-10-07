package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Which slice of a Barmbrack hides the ring: picked at random when the loaf is placed (or, for a loaf that never was,
 * when its first slice is eaten), on the server, and saved; found once.
 */
public class BarmbrackBlockEntity extends BlockEntity {
	private int ring = -1;
	private boolean found;

	public BarmbrackBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftAgriculture.BARMBRACK_ENTITY, pos, state);
	}

	void hideRing(RandomSource random) {
		ring = random.nextInt(BarmbrackBlock.SLICES);
		found = false;
		setChanged();
	}

	/** The slice the ring is in (-1 before it is hidden). */
	public int ring() {
		return ring;
	}

	/** Whether eating {@code slice} finds the ring (it is found only once). */
	public boolean ringIn(int slice, RandomSource random) {
		if (ring < 0) {
			ring = slice + random.nextInt(BarmbrackBlock.SLICES - slice);
		}
		boolean here = !found && slice == ring;
		found |= here;
		setChanged();
		return here;
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		ring = input.getIntOr("ring", -1);
		found = input.getBooleanOr("found", false);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putInt("ring", ring);
		output.putBoolean("found", found);
	}
}
