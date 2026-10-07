package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The Bowling Pumpkin: a small, round, heavy pumpkin for bowling at Skeleton Pins. Use it to roll it along the ground
 * the way you face ({@link BowlingPumpkin}); it scores on the nearest Bowling Scoreboard within {@value #SCOREBOARD_REACH}
 * blocks of you, and comes to rest as an item to pick up again.
 */
public class BowlingPumpkinItem extends Item {
	public static final int SCOREBOARD_REACH = 6;

	public BowlingPumpkinItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (level instanceof ServerLevel server) {
			Vec3 look = player.getLookAngle();
			Vec3 flat = new Vec3(look.x, 0.0, look.z);
			if (flat.lengthSqr() < 1.0E-4) {
				return InteractionResult.FAIL;
			}
			Vec3 direction = flat.normalize();
			Vec3 start = player.position().add(direction.scale(0.8)).add(0.0, 0.1, 0.0);
			BowlingPumpkin pumpkin = new BowlingPumpkin(server, start, direction, stack, scoreboardNear(server, player.blockPosition()));
			server.addFreshEntity(pumpkin);
			level.playSound(null, player.blockPosition(), SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.8F, 0.5F);
			stack.consume(1, player);
		}
		return InteractionResult.SUCCESS;
	}

	/** The nearest Bowling Scoreboard within reach of {@code around}, or null. */
	static BlockPos scoreboardNear(ServerLevel level, BlockPos around) {
		BlockPos best = null;
		double bestDistance = Double.MAX_VALUE;
		for (BlockPos pos : BlockPos.betweenClosed(around.offset(-SCOREBOARD_REACH, -2, -SCOREBOARD_REACH), around.offset(SCOREBOARD_REACH, 2, SCOREBOARD_REACH))) {
			if (level.getBlockEntity(pos) instanceof BowlingScoreboardBlockEntity) {
				double distance = pos.distSqr(around);
				if (distance < bestDistance) {
					bestDistance = distance;
					best = pos.immutable();
				}
			}
		}
		return best;
	}
}
