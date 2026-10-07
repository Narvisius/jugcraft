package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
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
 * Where a Phantom Pipe Organ is in its tune, on the organ's master block. The tune is the opening of J. S. Bach's
 * Toccata and Fugue in D minor, BWV 565 (public domain), arranged here for note-block sounds: the famous falling
 * phrase three times, an octave lower each time, then the rolled chord over a low D. The game time it started is
 * sent to clients so that they can press the keys in time.
 */
public class PipeOrganBlockEntity extends BlockEntity {
	public static final int TUNE_TICKS = 200;
	public static final int FLUTE = 0;
	public static final int HARP = 1;
	public static final int BASS = 2;
	/**
	 * The tune: {tick, note, instrument} for each note. Notes count semitones up from a note block's lowest note
	 * (0 to 24): the flute's runs from F#4, the harp's from F#3 and the bass's from F#1. A held note is struck again.
	 */
	public static final int[][] TUNE = {
			{0, 15, FLUTE}, {0, 15, HARP}, {3, 13, FLUTE}, {3, 13, HARP}, {6, 15, FLUTE}, {6, 15, HARP}, {14, 15, FLUTE}, {14, 15, HARP},
			{26, 13, FLUTE}, {26, 13, HARP}, {28, 11, FLUTE}, {28, 11, HARP}, {30, 10, FLUTE}, {30, 10, HARP}, {32, 8, FLUTE}, {32, 8, HARP},
			{34, 7, FLUTE}, {34, 7, HARP}, {40, 7, FLUTE}, {40, 7, HARP}, {46, 8, FLUTE}, {46, 8, HARP}, {54, 8, FLUTE}, {54, 8, HARP},
			{66, 15, BASS}, {66, 15, HARP}, {69, 13, BASS}, {69, 13, HARP}, {72, 15, BASS}, {72, 15, HARP}, {80, 15, BASS}, {80, 15, HARP},
			{90, 10, BASS}, {90, 10, HARP}, {92, 11, BASS}, {92, 11, HARP}, {94, 7, BASS}, {94, 7, HARP}, {96, 8, BASS}, {96, 8, HARP},
			{104, 8, BASS}, {104, 8, HARP}, {116, 15, BASS}, {119, 13, BASS}, {122, 15, BASS}, {130, 15, BASS}, {138, 13, BASS}, {140, 11, BASS},
			{142, 10, BASS}, {144, 8, BASS}, {146, 7, BASS}, {152, 7, BASS}, {158, 8, BASS}, {164, 8, BASS}, {170, 7, HARP}, {170, 8, BASS},
			{171, 10, HARP}, {172, 13, HARP}, {173, 16, HARP}, {174, 19, HARP}, {175, 22, HARP}, {180, 8, BASS}, {184, 8, BASS}, {184, 8, FLUTE},
			{184, 8, HARP}, {184, 11, HARP}, {184, 15, HARP}, {184, 20, HARP}, {192, 8, BASS}, {192, 8, FLUTE}};

	private int tick;
	private long startTime;

	public PipeOrganBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftAgriculture.PIPE_ORGAN_ENTITY, pos, state);
	}

	public int tick() {
		return tick;
	}

	/** The game time the tune last started (on the client too). */
	public long startTime() {
		return startTime;
	}

	/** Back to the start of the tune. */
	public void start() {
		tick = 0;
		startTime = level != null ? level.getGameTime() : 0;
		setChanged();
		if (level != null) {
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
		}
	}

	/** A note block's pitch for {@code note}. */
	public static float pitch(int note) {
		return MusicBoxBlockEntity.pitch(note);
	}

	/** Which of 28 keys (left to right) a note presses: lower notes on the left, the flute an octave above the harp. */
	public static int key(int note, int instrument) {
		int semitone = note + (instrument == FLUTE ? 12 : instrument == BASS ? -24 : 0) + 24;
		return Math.max(0, Math.min(27, semitone * 28 / 61));
	}

	/** Plays the notes due this tick; at the end of the tune the organ falls quiet. */
	public void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
		for (int[] note : TUNE) {
			if (note[0] == tick) {
				Holder<SoundEvent> sound = note[2] == FLUTE ? SoundEvents.NOTE_BLOCK_FLUTE : note[2] == BASS ? SoundEvents.NOTE_BLOCK_BASS
						: SoundEvents.NOTE_BLOCK_HARP;
				level.playSound(null, pos.above(), sound.value(), SoundSource.RECORDS, note[2] == BASS ? 1.0F : 0.8F, pitch(note[1]));
				if (level.getRandom().nextInt(3) == 0) {
					level.sendParticles(ParticleTypes.NOTE, pos.getX() + 0.5 + (level.getRandom().nextDouble() - 0.5) * 2.5, pos.getY() + 2.0,
							pos.getZ() + 0.5, 0, note[1] / 24.0, 0.0, 0.0, 1.0);
				}
			}
		}
		tick++;
		if (tick >= TUNE_TICKS) {
			tick = 0;
			level.setBlock(pos, state.setValue(PipeOrganBlock.PLAYING, false), Block.UPDATE_ALL);
		}
		setChanged();
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		tick = Math.floorMod(input.getIntOr("tick", 0), TUNE_TICKS);
		startTime = input.getLongOr("start_time", 0L);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putInt("tick", tick);
		output.putLong("start_time", startTime);
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return saveWithoutMetadata(registries);
	}
}
