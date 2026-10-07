package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Fortune Teller's Table's last reading: when, which card turned and where the planchette went (packed by
 * {@link FortuneTellerTableBlock#reading}), set on the server when it is asked for (for its cooldown) and on each client
 * by the reading's block event (to animate from). It is not saved.
 */
public class FortuneTellerTableBlockEntity extends BlockEntity {
	private long marked = Long.MIN_VALUE / 2;
	private int reading;

	public FortuneTellerTableBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftAgriculture.FORTUNE_TABLE_ENTITY, pos, state);
	}

	void read(long time, int reading) {
		this.marked = time;
		this.reading = reading;
	}

	/** The game time of the last reading. */
	public long marked() {
		return marked;
	}

	public int card() {
		return reading / FortuneTellerTableBlock.ANSWERS;
	}

	/** Where the planchette went: 0 YES, 1 NO, 2 GOODBYE. */
	public int answer() {
		return reading % FortuneTellerTableBlock.ANSWERS;
	}

	@Override
	public boolean triggerEvent(int id, int param) {
		if (id == FortuneTellerTableBlock.READ && level != null) {
			// The server noted the reading when it was asked for; clients note it when they hear of it.
			if (level.isClientSide()) {
				read(level.getGameTime(), param);
			}
			return true;
		}
		return super.triggerEvent(id, param);
	}
}
