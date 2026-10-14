package io.github.jimbozoomer.jugcraft.lair;

import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A name-stone of the Archive of Last Names (docs/features/crypt-archive.md): a tablet of pale limestone on an alcove's
 * back wall, over its grave seal, carved with one of the Crypt Lich's three names and its rune ({@link Rune}). A lair
 * fixture that cannot be broken or used. It is {@link Glow#DARK dark}, {@link Glow#LIT lit} (the name the Lich answers
 * to: its carving burns with soul-fire and gives light {@value #LIGHT}, tools/lairs.py NAME_STONE) or {@link Glow#ERASED
 * erased} (scraped blank; reading the name back at the Lectern of Last Names cuts it again, {@link ArchiveLecternBlock}).
 * {@link #FACING} is the way its face looks, out of the wall.
 */
public class NameStoneBlock extends HorizontalDirectionalBlock {
	public static final EnumProperty<Rune> RUNE = EnumProperty.create("rune", Rune.class);
	public static final EnumProperty<Glow> GLOW = EnumProperty.create("glow", Glow.class);
	public static final int LIGHT = 9;
	private static final VoxelShape NORTH = Block.box(0, 0, 13, 16, 15, 16);
	private static final VoxelShape SOUTH = Block.box(0, 0, 0, 16, 15, 3);
	private static final VoxelShape EAST = Block.box(0, 0, 0, 3, 15, 16);
	private static final VoxelShape WEST = Block.box(13, 0, 0, 16, 15, 16);

	/** The Lich's three names and their runes, in the order of tools/lairs.py NAMES. */
	public enum Rune implements StringRepresentable {
		SPADE("sexton"), CROWN("crowned"), QUILL("scribe");

		private final String title;

		Rune(String title) {
			this.title = title;
		}

		/** The name the rune stands for: "sexton", "crowned" or "scribe". */
		public String title() {
			return title;
		}

		/** The name, as shown: "the Sexton", "the Crowned", "the Scribe". */
		public Component display() {
			return Component.translatable("lair.jugcraft.crypt_archive.name." + title);
		}

		@Override
		public String getSerializedName() {
			return name().toLowerCase(Locale.ROOT);
		}
	}

	/** How a name-stone shows its name. */
	public enum Glow implements StringRepresentable {
		DARK, LIT, ERASED;

		@Override
		public String getSerializedName() {
			return name().toLowerCase(Locale.ROOT);
		}
	}

	public NameStoneBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(RUNE, Rune.SPADE).setValue(GLOW, Glow.DARK));
	}

	/** The light a name-stone gives in {@code state}. */
	public static int light(BlockState state) {
		return state.getValue(GLOW) == Glow.LIT ? LIGHT : 0;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return switch (state.getValue(FACING)) {
			case SOUTH -> SOUTH;
			case EAST -> EAST;
			case WEST -> WEST;
			default -> NORTH;
		};
	}

	/** Sets the name-stone at {@code pos} to {@code glow}; whether it was a name-stone that changed. */
	public static boolean setGlow(Level level, BlockPos pos, Glow glow) {
		BlockState state = level.getBlockState(pos);
		if (!(state.getBlock() instanceof NameStoneBlock) || state.getValue(GLOW) == glow) {
			return false;
		}
		level.setBlock(pos, state.setValue(GLOW, glow), Block.UPDATE_ALL);
		return true;
	}

	/** Lit, now and then a flicker of soul-fire rises off the carving. */
	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (state.getValue(GLOW) != Glow.LIT || random.nextInt(3) != 0) {
			return;
		}
		Direction out = state.getValue(FACING);
		double x = pos.getX() + 0.5 - out.getStepX() * 0.3 + (out.getStepX() == 0 ? (random.nextDouble() - 0.5) * 0.7 : 0.0);
		double z = pos.getZ() + 0.5 - out.getStepZ() * 0.3 + (out.getStepZ() == 0 ? (random.nextDouble() - 0.5) * 0.7 : 0.0);
		level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, x, pos.getY() + 0.3 + random.nextDouble() * 0.5, z, 0.0, 0.01, 0.0);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, RUNE, GLOW);
	}
}
