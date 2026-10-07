package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A Specimen Jar: a tall glass jar of glowing green fluid (light {@value #LIGHT}) under an iron lid, with something
 * floating in it ({@link #SPECIMEN}): an eye, a tentacle, a tiny pumpkin or a brain. Sneak-use it to put in the next;
 * broken, it keeps its specimen. The specimen bobs and turns slowly, with bubbles rising past it (drawn by the client,
 * client/SpecimenJarRenderer.java).
 */
public class SpecimenJarBlock extends BaseEntityBlock {
	public static final int LIGHT = 7;
	/** How far the specimen bobs (pixels) and the ticks one bob takes. */
	public static final float BOB = 0.75F;
	public static final int BOB_TICKS = 90;
	public static final EnumProperty<Specimen> SPECIMEN = EnumProperty.create("specimen", Specimen.class);
	/** The jar's box {x0, y0, z0, x1, y1, z1} in pixels (each bigger specimen jar's is this, made bigger). */
	public static final double[] BOX = {4.0, 0.0, 4.0, 12.0, 13.0, 12.0};
	private static final VoxelShape SHAPE = Block.box(BOX[0], BOX[1], BOX[2], BOX[3], BOX[4], BOX[5]);

	public enum Specimen implements StringRepresentable {
		EYE, TENTACLE, PUMPKIN, BRAIN;

		public Specimen next() {
			return values()[(ordinal() + 1) % values().length];
		}

		@Override
		public String getSerializedName() {
			return name().toLowerCase(java.util.Locale.ROOT);
		}
	}

	public SpecimenJarBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(SPECIMEN, Specimen.EYE));
	}

	/** How far (in pixels) the specimen at {@code pos} has risen or fallen at {@code time} (ticks with the partial tick). */
	public static float bob(BlockPos pos, float time) {
		float phase = (pos.hashCode() & 0xFF) / 256.0F;
		return BOB * Mth.sin((time / BOB_TICKS + phase) * Mth.TWO_PI);
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new DecorationBlockEntity(JugcraftAgriculture.SPECIMEN_JAR_ENTITY, pos, state);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	/** Sneak-use puts in the next specimen. */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!player.isSecondaryUseActive()) {
			return InteractionResult.PASS;
		}
		if (!level.isClientSide()) {
			Specimen next = state.getValue(SPECIMEN).next();
			level.setBlock(pos, state.setValue(SPECIMEN, next), Block.UPDATE_ALL);
			level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 0.6F, 0.7F);
			player.sendOverlayMessage(Component.translatable("message.jugcraft.specimen_jar." + next.getSerializedName()));
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(SPECIMEN);
	}
}
