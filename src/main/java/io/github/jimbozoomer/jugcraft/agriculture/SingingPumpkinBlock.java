package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * A Singing Pumpkin (Halloween decorations batch 20, Pumpkin Night): a jack-o'-lantern with a hinged mouth and rolling
 * eyes that sings in one of four voices. Like a note block it is tuned by use across {@value #NOTES} notes (two octaves;
 * sneaking tunes it down), and a rising redstone signal makes it sing its note: the client opens its mouth round in time
 * and widens its eyes (client/SingingPumpkinRenderer.java). Wire a row to a sequencer and they sing a tune together. The
 * voices are Jugcraft's own sounds, synthesised by tools/choir_sounds.py.
 */
public class SingingPumpkinBlock extends HorizontalDirectionalBlock implements EntityBlock {
	public static final int NOTES = 25;
	public static final int OPEN_TICKS = 24;
	public static final int LIGHT = 12;
	public static final int EVENT_SING = 1;
	public static final IntegerProperty NOTE = BlockStateProperties.NOTE;
	public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
	private static final String[] NAMES = {"F#", "G", "G#", "A", "A#", "B", "C", "C#", "D", "D#", "E", "F"};

	/** The four voices, low to high (tools/decor20.py VOICES). */
	public enum Voice {
		BASS, TENOR, ALTO, SOPRANO;

		public String id() {
			return name().toLowerCase(Locale.ROOT);
		}
	}

	private final Voice voice;

	public SingingPumpkinBlock(Properties properties, Voice voice) {
		super(properties);
		this.voice = voice;
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(NOTE, 12).setValue(POWERED, false));
	}

	public Voice voice() {
		return voice;
	}

	/** The pitch to play {@code note} at (note 12 is the voice's middle note, pitch 1), as a note block plays: a semitone a note. */
	public static float pitch(int note) {
		return (float) Math.pow(2.0, (note - 12) / 12.0);
	}

	/** {@code note}'s name with the octave above the voice's lowest note, as 0 to 2: F#0 to F#2. */
	public static String noteName(int note) {
		return NAMES[note % 12] + (note / 12 + (note % 12 >= 6 ? 1 : 0));
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite())
				.setValue(POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos()));
	}

	/** Used, it goes up a note (down while sneaking), wrapping round, and sings it. */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level instanceof ServerLevel server) {
			int note = Math.floorMod(state.getValue(NOTE) + (player.isSecondaryUseActive() ? -1 : 1), NOTES);
			BlockState tuned = state.setValue(NOTE, note);
			level.setBlock(pos, tuned, Block.UPDATE_ALL);
			sing(server, pos, tuned);
			player.sendOverlayMessage(Component.translatable("message.jugcraft.singing_pumpkin", noteName(note)));
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, @Nullable Orientation orientation, boolean moved) {
		super.neighborChanged(state, level, pos, neighbor, orientation, moved);
		if (!(level instanceof ServerLevel server)) {
			return;
		}
		boolean powered = level.hasNeighborSignal(pos);
		if (powered == state.getValue(POWERED)) {
			return;
		}
		BlockState changed = state.setValue(POWERED, powered);
		level.setBlock(pos, changed, Block.UPDATE_CLIENTS);
		if (powered) {
			sing(server, pos, changed);
		}
	}

	/** Sings its note now: the voice, a note particle, and the mouth on clients. */
	public static void sing(ServerLevel level, BlockPos pos, BlockState state) {
		if (!(state.getBlock() instanceof SingingPumpkinBlock pumpkin)) {
			return;
		}
		int note = state.getValue(NOTE);
		SoundEvent sound = JugcraftAgriculture.singingVoice(pumpkin.voice);
		level.playSound(null, pos, sound, SoundSource.RECORDS, 3.0F, pitch(note));
		level.sendParticles(ParticleTypes.NOTE, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 0, note / 24.0, 0.0, 0.0, 1.0);
		level.blockEvent(pos, pumpkin, EVENT_SING, note);
		level.gameEvent(null, GameEvent.NOTE_BLOCK_PLAY, pos);
	}

	/** Clients mark when it sang, for the mouth. */
	@Override
	protected boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int param) {
		if (id == EVENT_SING && level.getBlockEntity(pos) instanceof DecorationBlockEntity pumpkin) {
			pumpkin.mark(level.getGameTime());
			return true;
		}
		return super.triggerEvent(state, level, pos, id, param);
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new DecorationBlockEntity(JugcraftAgriculture.SINGING_PUMPKIN_ENTITY, pos, state);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, NOTE, POWERED);
	}
}
