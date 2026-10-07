package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Holds nothing: it lets the client draw the moving or lit parts of a decoration that its block model can't show (the
 * Rocking Chair rocking, the Lurking Eyes blinking, a Silhouette Window glowing, the Giant Fake Spider swaying, the
 * Haunted Chandelier swinging, the Suit of Armor's helmet turning, the Spirit Mirror's face, Tattered Curtains swaying
 * and the Creepy Doll's head, the Lab Table's patient, the Specimen Jar's specimen, the Mummy Sarcophagus's lid and
 * mummy, and the Raven). Each of those blocks has its own block entity type of this class. A block event may
 * {@link #mark} the time something happened (the raven's flap), for the client to animate from; it is not saved.
 */
public class DecorationBlockEntity extends BlockEntity {
	private long marked = Long.MIN_VALUE;

	public DecorationBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	/** Records the game time of a moment to animate from. */
	public void mark(long time) {
		marked = time;
	}

	/** The last game time {@link #mark}ed, or {@link Long#MIN_VALUE}. */
	public long marked() {
		return marked;
	}
}
