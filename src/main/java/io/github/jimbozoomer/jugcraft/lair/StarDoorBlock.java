package io.github.jimbozoomer.jugcraft.lair;

import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * A block of the Cydonian Sanctum's Star Door (docs/features/cydonian-sanctum.md): carved red stone filling the Dust
 * Gallery's north end. Three of its blocks carry a constellation ({@link Figure}) on the gallery's side; each burns
 * ({@link #LIT}, giving light) while one of the star dials shows it. A lair fixture that cannot be broken or used. When all
 * three burn, the door is marked {@link #OPENING}, and each of its blocks crumbles away on its own scheduled tick, a row at
 * a time from the top ({@link CydonianSanctum#open}). It is the only block of the Sanctum that changes; each instance is
 * placed fresh, door and all.
 */
public class StarDoorBlock extends Block {
	public static final EnumProperty<Figure> FIGURE = EnumProperty.create("figure", Figure.class);
	public static final BooleanProperty LIT = LairBrazierBlock.LIT;
	public static final BooleanProperty OPENING = BooleanProperty.create("opening");
	/** The light a lit figure gives. */
	public static final int LIGHT = 7;
	/** Ticks from the door opening to its top row crumbling, and between one row and the next. */
	public static final int FIRST_ROW_TICKS = 10;
	public static final int ROW_TICKS = 6;

	/** What a block of the door carries: nothing, or one of the three constellations, west to east. */
	public enum Figure implements StringRepresentable {
		PLAIN, HUNTER, SWAN, CROWN;

		/** The constellation a dial must show to light this figure (null for a plain block). */
		public StarDialBlock.Stars stars() {
			return switch (this) {
				case HUNTER -> StarDialBlock.Stars.HUNTER;
				case SWAN -> StarDialBlock.Stars.SWAN;
				case CROWN -> StarDialBlock.Stars.CROWN;
				case PLAIN -> null;
			};
		}

		@Override
		public String getSerializedName() {
			return name().toLowerCase(Locale.ROOT);
		}
	}

	public StarDoorBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FIGURE, Figure.PLAIN).setValue(LIT, false).setValue(OPENING, false));
	}

	/** The light a block of the door gives in {@code state}. */
	public static int light(BlockState state) {
		return state.getValue(LIT) ? LIGHT : 0;
	}

	/** Its time come while the door is opening, the block crumbles away; a stray tick on a door placed fresh does nothing. */
	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (!state.getValue(OPENING)) {
			return;
		}
		level.levelEvent(LevelEvent.PARTICLES_DESTROY_BLOCK, pos, Block.getId(state));
		level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
		if (random.nextInt(3) == 0) {
			level.playSound(null, pos, SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 0.8F, 0.6F);
		}
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FIGURE, LIT, OPENING);
	}
}
