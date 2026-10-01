package io.github.jimbozoomer.jugcraft.deposit;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A surface deposit block (coal, iron, copper or tin). It cannot be mined for its ore: picks only break it, for
 * nothing. A deposit drill standing on it takes its {@link Deposits#CAPACITY} units out, a few at a time, until it
 * turns to stone. Right-click it to read how much is left.
 */
public class DepositBlock extends Block {
	private final Identifier yield;

	public DepositBlock(Identifier yield, Properties properties) {
		super(properties);
		this.yield = yield;
	}

	/** What a drill gets for each unit: coal, or the raw ore. */
	public Item yield() {
		return BuiltInRegistries.ITEM.getValue(yield);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level instanceof ServerLevel server) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.deposit", getName(),
					String.format("%,d", Deposits.remaining(server, pos)), String.format("%,d", Deposits.CAPACITY)));
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
		if (!level.getBlockState(pos).is(this)) {
			Deposits.forget(level, pos);
		}
	}
}
