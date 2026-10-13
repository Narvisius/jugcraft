package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;

/**
 * A vanilla pie set down as a pie (the owner's pumpkin pie art; tools/feasts.py PLACED_PIES): sneak and use the pie on a
 * block to set it down whole, then eat or cut it {@value PieBlock#SLICES} slices at a time like any pie. A whole one picks
 * up (and breaks) as the vanilla item; its slices add up to what the whole pie gives.
 */
public class PlacedPieBlock extends PieBlock {
	private final Item whole;

	public PlacedPieBlock(Item whole, String slice, int nutrition, float saturation, Properties properties) {
		super(slice, nutrition, saturation, properties);
		this.whole = whole;
	}

	public Item whole() {
		return whole;
	}

	@Override
	protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
		return new ItemStack(whole);
	}

	/**
	 * Sets {@code pie} down where a sneaking player uses the vanilla pie it is made from, as a block item would place it;
	 * PASS when the player isn't sneaking with that pie or it can't go there.
	 */
	public static InteractionResult setDown(PlacedPieBlock pie, Player player, Level level, InteractionHand hand, BlockHitResult hit) {
		ItemStack stack = player.getItemInHand(hand);
		if (player.isSpectator() || !player.isSecondaryUseActive() || !stack.is(pie.whole())) {
			return InteractionResult.PASS;
		}
		BlockPlaceContext context = new BlockPlaceContext(new UseOnContext(player, hand, hit));
		BlockPos pos = context.getClickedPos();
		BlockState state = pie.defaultBlockState();
		if (!context.canPlace() || !state.canSurvive(level, pos) || !player.mayUseItemAt(pos, hit.getDirection(), stack)
				|| !level.isUnobstructed(state, pos, CollisionContext.empty())) {
			return InteractionResult.PASS;
		}
		if (!level.isClientSide()) {
			level.setBlock(pos, state, Block.UPDATE_ALL);
			level.playSound(null, pos, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 0.8F, 1.2F);
			level.gameEvent(player, GameEvent.BLOCK_PLACE, pos);
			stack.consume(1, player);
		}
		return InteractionResult.SUCCESS;
	}
}
