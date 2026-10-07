package io.github.jimbozoomer.jugcraft.agriculture;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * The Spirit Board's séances ({@link SpiritBoardBlockEntity}): the board's letters and where they are, the restless spirits'
 * names and what they wish for. A séance needs a lit candle within {@value #CANDLE_RANGE} blocks of the board (anything in
 * the block tag {@code jugcraft:seance_candles} that is lit) and players' fingers on the planchette, up to
 * {@value #MAX_HANDS}, each within {@value #HAND_RANGE} blocks of it. If a restless spirit is within {@value #SPIRIT_RANGE}
 * blocks, the planchette goes to YES and spells its name and the thing it wishes for, then GOODBYE; if not, NO and
 * GOODBYE. It moves to a letter every {@value #LETTER_TICKS} ticks with one player's fingers on it, every
 * {@value #FAST_LETTER_TICKS} with two or more. Given what it wishes for, a revealed spirit is laid to rest
 * ({@link RestlessSpirit}): {@value #REST_XP} experience and Luck for {@value #REST_LUCK_TICKS} ticks to whoever gave it.
 */
public final class SpiritBoard {
	public static final int CANDLE_RANGE = 4;
	public static final int SPIRIT_RANGE = 16;
	public static final int HAND_RANGE = 3;
	public static final int MAX_HANDS = 4;
	public static final int LETTER_TICKS = 20;
	public static final int FAST_LETTER_TICKS = 12;
	/** How long the board rests after a séance before another can start. */
	public static final int COOLDOWN_TICKS = 40;
	/** How far away players see the letters spelled out. */
	public static final int WATCH_RANGE = 8;
	public static final int REST_XP = 20;
	public static final int REST_LUCK_TICKS = 6000;
	public static final TagKey<Block> CANDLES = TagKey.create(Registries.BLOCK, Jugcraft.id("seance_candles"));

	/** Stops that aren't letters: the planchette's resting place in the middle, YES, NO and GOODBYE. */
	public static final char REST = ' ';
	public static final char YES = '+';
	public static final char NO = '-';
	public static final char GOODBYE = '.';

	/** The board's face, in pixels of its 64 by 48 picture: the top two arcs of letters, the numbers and GOODBYE under them. */
	public static final int FACE_WIDTH = 64;
	public static final int FACE_HEIGHT = 48;

	/** The names the spirits give, spelled on the board. */
	public static final String[] NAMES = {"MABEL", "OTIS", "EZRA", "HATTIE", "JASPER", "AGNES", "SILAS", "WINNIE", "AMOS", "PRUDENCE",
			"ELIJAH", "OPAL", "CORNELIUS", "BEATRIX", "HORACE", "LUELLA"};

	/** What a spirit wishes for, as spelled on the board, and the items that grant it ({@code jugcraft:spirit_wishes/<word>}). */
	public enum Wish {
		PIE, CANDLE, CIDER, SWEATER, CANDY, APPLE, ROSE, PUMPKIN;

		public final String word = name();
		public final TagKey<Item> items = TagKey.create(Registries.ITEM, Jugcraft.id("spirit_wishes/" + name().toLowerCase(java.util.Locale.ROOT)));

		public String key() {
			return name().toLowerCase(java.util.Locale.ROOT);
		}
	}

	private SpiritBoard() {
	}

	/**
	 * Where {@code stop} is on the board's face, in its pixels: {x, y}, the middle of the letter (or word, or of the board
	 * for {@link #REST}), or null if the board has no such stop.
	 */
	public static float[] place(char stop) {
		if (stop >= 'A' && stop <= 'M') {
			int i = stop - 'A';
			return new float[] {7.5F + 4 * i, 15.5F - arc(i)};
		}
		if (stop >= 'N' && stop <= 'Z') {
			int i = stop - 'N';
			return new float[] {7.5F + 4 * i, 24.5F - arc(i)};
		}
		if (stop >= '0' && stop <= '9') {
			int i = stop == '0' ? 9 : stop - '1';
			return new float[] {13.5F + 4 * i, 33.5F};
		}
		return switch (stop) {
			case YES -> new float[] {11.5F, 6.5F};
			case NO -> new float[] {52.5F, 6.5F};
			case GOODBYE -> new float[] {32.5F, 41.5F};
			case REST -> new float[] {32.0F, 28.0F};
			default -> null;
		};
	}

	/** How far up the {@code i}th of an arc's thirteen letters is raised, in pixels: none at the ends, three in the middle. */
	public static int arc(int i) {
		return Math.round(3.0F * (float) Math.sin(Math.PI * (i + 0.5) / 13.0));
	}

	/** Whether a lit candle is within {@value #CANDLE_RANGE} blocks of {@code pos}. Looks at 9 by 9 by 9 blocks once. */
	public static boolean candlelit(Level level, BlockPos pos) {
		for (BlockPos near : BlockPos.betweenClosed(pos.offset(-CANDLE_RANGE, -CANDLE_RANGE, -CANDLE_RANGE),
				pos.offset(CANDLE_RANGE, CANDLE_RANGE, CANDLE_RANGE))) {
			BlockState state = level.getBlockState(near);
			if (state.is(CANDLES) && state.hasProperty(BlockStateProperties.LIT) && state.getValue(BlockStateProperties.LIT)) {
				return true;
			}
		}
		return false;
	}

	/** What a spirit named {@code name} wishing for {@code wish} spells: YES, its name, its wish and GOODBYE. */
	public static String message(int name, Wish wish) {
		return YES + "" + REST + NAMES[name] + REST + wish.word + GOODBYE;
	}

	/** What the board spells when no spirit is near: NO and GOODBYE. */
	public static String nobody() {
		return NO + "" + GOODBYE;
	}
}
