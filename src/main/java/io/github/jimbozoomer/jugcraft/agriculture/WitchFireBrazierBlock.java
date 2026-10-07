package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Witch Fire Brazier: a black iron bowl of coals on four legs, burning ({@link #LIT}, light {@value #LIGHT}) with a
 * tall flame. Use a dye on it to turn the flame its colour ({@link #FLAME}: orange, green, purple or blue; the dye is
 * used up). Flint and steel or a fire charge lights it; a shovel puts it out. Its fire is witch fire: it burns nothing
 * and nobody. The flames are drawn by the client (client/WitchFireBrazierRenderer.java).
 */
public class WitchFireBrazierBlock extends BaseEntityBlock {
	public static final int LIGHT = 15;
	public static final BooleanProperty LIT = BlockStateProperties.LIT;
	public static final EnumProperty<Flame> FLAME = EnumProperty.create("flame", Flame.class);
	private static final VoxelShape SHAPE = Block.box(1.5, 0.0, 1.5, 14.5, 11.5, 14.5);

	public enum Flame implements StringRepresentable {
		ORANGE(0xFFFF9A2A, DyeColor.ORANGE), GREEN(0xFF62FF4A, DyeColor.GREEN), PURPLE(0xFFC866FF, DyeColor.PURPLE),
		BLUE(0xFF58B8FF, DyeColor.BLUE);

		/** The flame's colour (ARGB), for the client to tint it with. */
		public final int color;
		private final DyeColor dye;

		Flame(int color, DyeColor dye) {
			this.color = color;
			this.dye = dye;
		}

		/** The flame a dye turns it, or null. */
		public static @Nullable Flame of(ItemStack stack) {
			DyeColor color = ScarecrowBlock.dyeColor(stack);
			for (Flame flame : values()) {
				if (flame.dye == color) {
					return flame;
				}
			}
			return null;
		}

		@Override
		public String getSerializedName() {
			return name().toLowerCase(Locale.ROOT);
		}
	}

	public WitchFireBrazierBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(LIT, true).setValue(FLAME, Flame.ORANGE));
	}

	public static int light(BlockState state) {
		return state.getValue(LIT) ? LIGHT : 0;
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new DecorationBlockEntity(JugcraftAgriculture.BRAZIER_ENTITY, pos, state);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	/** A dye colours the flame; flint and steel or a fire charge lights it; a shovel puts it out. */
	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
			BlockHitResult hit) {
		Flame flame = Flame.of(stack);
		if (flame != null) {
			if (flame == state.getValue(FLAME)) {
				return InteractionResult.TRY_WITH_EMPTY_HAND;
			}
			if (!level.isClientSide()) {
				level.setBlock(pos, state.setValue(FLAME, flame), Block.UPDATE_ALL);
				level.playSound(null, pos, SoundEvents.DYE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
				level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.5F, 1.4F);
				level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
				stack.consume(1, player);
			}
			return InteractionResult.SUCCESS;
		}
		if (stack.is(ItemTags.SHOVELS) && state.getValue(LIT)) {
			if (!level.isClientSide()) {
				level.setBlock(pos, state.setValue(LIT, false), Block.UPDATE_ALL);
				level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.8F, 1.0F);
				level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
			}
			return InteractionResult.SUCCESS;
		}
		return CandleLighting.light(stack, state, level, pos, player, hand, LIT);
	}

	/** Smoke, and sparks of the flame's colour. */
	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (!state.getValue(LIT)) {
			return;
		}
		double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.4;
		double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.4;
		if (random.nextInt(3) == 0) {
			level.addParticle(ParticleTypes.SMOKE, x, pos.getY() + 1.3, z, 0.0, 0.04, 0.0);
		}
		ParticleOptions spark = switch (state.getValue(FLAME)) {
			case GREEN -> ParticleTypes.HAPPY_VILLAGER;
			case PURPLE -> ParticleTypes.WITCH;
			case BLUE -> ParticleTypes.SOUL_FIRE_FLAME;
			default -> ParticleTypes.FLAME;
		};
		if (random.nextInt(2) == 0) {
			level.addParticle(spark, x, pos.getY() + 0.9, z, 0.0, 0.03, 0.0);
		}
		if (random.nextInt(10) == 0) {
			level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5, SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 0.6F, 1.0F, false);
		}
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(LIT, FLAME);
	}
}
