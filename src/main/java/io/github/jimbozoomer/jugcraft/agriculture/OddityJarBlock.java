package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * An Oddity Jar (Halloween decorations batch 17): a squat glass jar under an iron lid with a paper label, and something
 * in it ({@link Kind}), drawn by the client: eyeballs that roll to follow the nearest player within {@value #WATCH_RANGE}
 * blocks, a heart that beats ({@link BeatingHeartJarBlock}, a redstone clock), a bat that wakes ({@link BatJarBlock}), a
 * coiled two-headed snake whose tongues flick when the jar is used, and a hand that drums its fingers
 * ({@link HandJarBlock}, which points while powered).
 */
public class OddityJarBlock extends Block implements EntityBlock {
	public static final int WATCH_RANGE = 8;
	/** How long the snake's tongues flick for after the jar is used, in ticks. */
	public static final int FLICK_TICKS = 20;
	/** The jar's box {x0, y0, z0, x1, y1, z1} in pixels (the Giant's Beating Heart's is three times this). */
	public static final double[] BOX = {3.9, 0.0, 3.9, 12.1, 13.6, 12.1};
	private static final VoxelShape SHAPE = Block.box(BOX[0], BOX[1], BOX[2], BOX[3], BOX[4], BOX[5]);
	private final Kind kind;

	/** What is in the jar. */
	public enum Kind implements StringRepresentable {
		EYEBALLS, HEART, BAT, SNAKE, HAND;

		@Override
		public String getSerializedName() {
			return name().toLowerCase(Locale.ROOT);
		}
	}

	public OddityJarBlock(Properties properties, Kind kind) {
		super(properties);
		this.kind = kind;
	}

	public Kind kind() {
		return kind;
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new DecorationBlockEntity(JugcraftAgriculture.ODDITY_JAR_ENTITY, pos, state);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	/** Using the snake's jar makes both its heads flick their tongues and hiss. */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (kind != Kind.SNAKE || !player.getMainHandItem().isEmpty()) {
			return InteractionResult.PASS;
		}
		if (!level.isClientSide()) {
			level.blockEvent(pos, this, 1, 0);
			level.playSound(null, pos, SoundEvents.SPIDER_AMBIENT, SoundSource.BLOCKS, 0.4F, 1.6F);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int param) {
		if (level.getBlockEntity(pos) instanceof DecorationBlockEntity jar) {
			jar.mark(level.getGameTime());
			return true;
		}
		return false;
	}
}
