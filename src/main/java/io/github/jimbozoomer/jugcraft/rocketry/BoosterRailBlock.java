package io.github.jimbozoomer.jugcraft.rocketry;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * The booster rail (batch 42, docs/features/booster-rails.md): a straight rail with rocket thrusters, powered like a
 * powered rail (and passing power along a line of them the same way). It does not slow carts when unpowered. Use solid
 * propellant on it to load it; while powered and loaded, it boosts carts ({@link BoosterRailBlockEntity}).
 */
public class BoosterRailBlock extends PoweredRailBlock implements EntityBlock {
	public BoosterRailBlock(Properties properties) {
		super(properties);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new BoosterRailBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide() || type != JugcraftRocketry.BOOSTER_RAIL_ENTITY) {
			return null;
		}
		return (tickLevel, pos, tickState, entity) -> ((BoosterRailBlockEntity) entity).serverTick((ServerLevel) tickLevel, pos, tickState);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
			BlockHitResult hit) {
		if (!stack.is(JugcraftRocketry.SOLID_PROPELLANT)) {
			return super.useItemOn(stack, state, level, pos, player, hand, hit);
		}
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof BoosterRailBlockEntity rail) {
			rail.load(player, stack);
		}
		return InteractionResult.SUCCESS;
	}
}
