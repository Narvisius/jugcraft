package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Where a Spooky Music Box is in its tune. The tune is original: an eight-bar waltz in A minor, its melody on the
 * bell and a low harp on each bar's first beat, {@value #BEATS} beats of {@value #TICKS_PER_BEAT} ticks.
 */
public class MusicBoxBlockEntity extends BlockEntity {
	public static final int TICKS_PER_BEAT = 6;
	public static final int BEATS = 24;
	public static final int BELL = 0;
	public static final int HARP = 1;
	/**
	 * The tune: {beat, note, instrument} for each note. Notes count semitones up from F#3 like a note block's (0 to
	 * 24): 3 is A3, 15 A4, 18 C5, 22 E5.
	 */
	public static final int[][] TUNE = {
			{0, 15, BELL}, {1, 18, BELL}, {2, 22, BELL}, {3, 21, BELL}, {4, 22, BELL}, {5, 18, BELL},
			{6, 17, BELL}, {7, 14, BELL}, {8, 17, BELL}, {9, 15, BELL},
			{12, 15, BELL}, {13, 20, BELL}, {14, 23, BELL}, {15, 22, BELL}, {16, 20, BELL}, {17, 17, BELL},
			{18, 18, BELL}, {19, 17, BELL}, {20, 14, BELL}, {21, 15, BELL},
			{0, 3, HARP}, {3, 3, HARP}, {6, 2, HARP}, {9, 3, HARP}, {12, 8, HARP}, {15, 3, HARP}, {18, 2, HARP}, {21, 3, HARP}};

	private int tick;

	public MusicBoxBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftAgriculture.MUSIC_BOX_ENTITY, pos, state);
	}

	public int tick() {
		return tick;
	}

	/** Back to the start of the tune. */
	public void start() {
		tick = 0;
		setChanged();
	}

	/** A note block's pitch for {@code note}: two to the power of (note - 12) / 12. */
	public static float pitch(int note) {
		return (float) Math.pow(2.0, (note - 12) / 12.0);
	}

	/** Plays the notes due this tick; at the end of the tune plays it again while powered, or shuts the lid. */
	public void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
		if (tick % TICKS_PER_BEAT == 0) {
			int beat = tick / TICKS_PER_BEAT;
			for (int[] note : TUNE) {
				if (note[0] == beat) {
					Holder<SoundEvent> sound = note[2] == BELL ? SoundEvents.NOTE_BLOCK_BELL : SoundEvents.NOTE_BLOCK_HARP;
					level.playSound(null, pos, sound.value(), SoundSource.RECORDS, note[2] == BELL ? 0.7F : 0.5F, pitch(note[1]));
					level.sendParticles(ParticleTypes.NOTE, pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5, 0, note[1] / 24.0, 0.0, 0.0, 1.0);
				}
			}
		}
		tick++;
		if (tick >= BEATS * TICKS_PER_BEAT) {
			tick = 0;
			if (!state.getValue(MusicBoxBlock.POWERED)) {
				level.setBlock(pos, state.setValue(MusicBoxBlock.OPEN, false), Block.UPDATE_ALL);
			}
		}
		setChanged();
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		tick = Math.floorMod(input.getIntOr("tick", 0), BEATS * TICKS_PER_BEAT);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putInt("tick", tick);
	}
}
