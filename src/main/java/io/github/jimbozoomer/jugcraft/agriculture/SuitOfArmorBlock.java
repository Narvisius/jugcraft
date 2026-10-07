package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The Suit of Armor: a full suit of plate on a wooden stand, two blocks tall, a halberd in its gauntlet. Its helmet
 * slowly turns to follow the nearest player within {@value #WATCH_RANGE} blocks, at most {@value #TURN_SPEED} degrees
 * a tick and {@value #MAX_TURN} degrees either way, and looks ahead again when nobody is near; at night a red glow
 * shows in its visor. The client draws the helmet (client/SuitOfArmorRenderer.java, from the block entity on the upper
 * half); using the suit makes it clank.
 */
public class SuitOfArmorBlock extends TallDecorationBlock implements EntityBlock {
	public static final double WATCH_RANGE = 10.0;
	public static final float TURN_SPEED = 4.0F;
	public static final float MAX_TURN = 75.0F;

	public SuitOfArmorBlock(Properties properties) {
		super(properties, Block.box(3.0, 0.0, 3.0, 13.0, 16.0, 13.0), Block.box(2.0, 0.0, 3.0, 14.0, 16.0, 13.0));
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return state.getValue(HALF) == DoubleBlockHalf.UPPER ? new DecorationBlockEntity(JugcraftAgriculture.SUIT_OF_ARMOR_ENTITY, pos, state) : null;
	}

	/**
	 * Where the helmet of a suit facing {@code facing}, its head at {@code head}, should look to watch {@code target}:
	 * degrees clockwise (seen from above) from straight ahead, at most {@value #MAX_TURN} either way; 0 with no target.
	 */
	public static float watchYaw(Vec3 head, Direction facing, @Nullable Vec3 target) {
		if (target == null) {
			return 0.0F;
		}
		double dx = target.x - head.x;
		double dz = target.z - head.z;
		if (dx * dx + dz * dz < 1.0E-4) {
			return 0.0F;
		}
		float angle = (float) Math.toDegrees(Math.atan2(dx, -dz));
		return Mth.clamp(Mth.wrapDegrees(angle - facing.toYRot() + 180.0F), -MAX_TURN, MAX_TURN);
	}

	/** {@code current} turned toward {@code target} by at most {@code step} degrees. */
	public static float turnToward(float current, float target, float step) {
		return current + Mth.clamp(target - current, -step, step);
	}

	/** Clank. */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide()) {
			level.playSound(null, pos, SoundEvents.ARMOR_EQUIP_GENERIC.value(), SoundSource.BLOCKS, 1.0F, 0.6F + level.getRandom().nextFloat() * 0.2F);
		}
		return InteractionResult.SUCCESS;
	}
}
