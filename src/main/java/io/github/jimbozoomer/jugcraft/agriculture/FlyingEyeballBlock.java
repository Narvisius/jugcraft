package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A Flying Eyeball (the haunted house's props): a bloodshot eye on red bat wings that hovers in its block. Nothing
 * holds it up and nothing collides with it. The client draws it (client/FlyingEyeballRenderer.java) bobbing
 * {@value #HOVER_PIXELS} pixels up and down every {@value #BOB_TICKS} ticks, beating its wings every {@value #FLAP_TICKS}
 * ticks, and turning, at most {@value #TURN_SPEED} degrees a tick, to stare at the nearest player within
 * {@value #WATCH_RANGE} blocks. Now and then its wings are heard.
 */
public class FlyingEyeballBlock extends BaseEntityBlock {
	public static final float HOVER_PIXELS = 1.25F;
	public static final int BOB_TICKS = 60;
	public static final int FLAP_TICKS = 9;
	public static final int WATCH_RANGE = 10;
	public static final float TURN_SPEED = 9.0F;
	/** The eye's middle above the block's floor at rest, in pixels. */
	public static final float CENTRE_Y = 8.5F;
	private static final VoxelShape SHAPE = Block.box(4.0, 4.0, 4.0, 12.0, 13.0, 12.0);

	public FlyingEyeballBlock(Properties properties) {
		super(properties);
	}

	/** How far (pixels) the eyeball at {@code pos} has risen or fallen at {@code time} (ticks with the partial tick). */
	public static float bob(BlockPos pos, float time) {
		float phase = (pos.hashCode() & 0xFF) / 256.0F;
		return HOVER_PIXELS * Mth.sin((time / BOB_TICKS + phase) * Mth.TWO_PI);
	}

	/** How far (degrees) its wings are raised above level at {@code time}: from a little below to well above. */
	public static float flap(BlockPos pos, float time) {
		float phase = ((pos.hashCode() >> 8) & 0xFF) / 256.0F;
		return 20.0F + 38.0F * Mth.sin((time / FLAP_TICKS + phase) * Mth.TWO_PI);
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new DecorationBlockEntity(JugcraftAgriculture.FLYING_EYEBALL_ENTITY, pos, state);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (random.nextInt(60) == 0) {
			level.playLocalSound(pos, SoundEvents.PHANTOM_FLAP, SoundSource.BLOCKS, 0.25F, 1.6F + random.nextFloat() * 0.3F, false);
		}
	}
}
