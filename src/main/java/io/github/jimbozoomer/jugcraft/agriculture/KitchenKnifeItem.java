package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.gameevent.GameEvent;
import org.jspecify.annotations.Nullable;

/**
 * A kitchen knife (the Farmhouse Kitchen, tools/kitchen.py KNIVES): a light, quick blade. On a Cutting Board it cuts what
 * lies there ({@link CuttingBoardBlock}); on a pie or the roast turkey it cuts a slice or a serving as the Carving
 * Knife does (item tag {@code jugcraft:knives}); and used on a cake it cuts a Slice of Cake to take away, one of the
 * cake's seven bites (the last slice takes the cake). A cake is cut before anything else is tried
 * ({@link #sliceCake}, from the use-block event), so a hungry player with a knife slices the cake rather than eating it.
 */
public class KitchenKnifeItem extends Item {
	public KitchenKnifeItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		return sliceCake(context.getPlayer(), context.getLevel(), context.getHand(), context.getClickedPos(), context.getClickedFace());
	}

	/** Cuts a Slice of Cake from the cake at {@code pos} with the knife in {@code hand}; PASS if there is no cake or knife. */
	public static InteractionResult sliceCake(@Nullable Player player, Level level, InteractionHand hand, BlockPos pos, Direction side) {
		BlockState state = level.getBlockState(pos);
		if (player == null || player.isSpectator() || !state.is(Blocks.CAKE) || !player.getItemInHand(hand).is(JugcraftAgriculture.KNIVES)
				|| !player.mayUseItemAt(pos, side, player.getItemInHand(hand))) {
			return InteractionResult.PASS;
		}
		if (!level.isClientSide()) {
			ItemStack slice = new ItemStack(JugcraftAgriculture.item("cake_slice"));
			if (!player.getInventory().add(slice)) {
				Block.popResource(level, pos, slice);
			}
			int bites = state.getValue(BlockStateProperties.BITES);
			if (bites >= 6) {
				level.removeBlock(pos, false);
			} else {
				level.setBlock(pos, state.setValue(BlockStateProperties.BITES, bites + 1), Block.UPDATE_ALL);
			}
			level.playSound(null, pos, SoundEvents.HONEY_BLOCK_SLIDE, SoundSource.BLOCKS, 0.6F, 1.4F);
			level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
			player.getItemInHand(hand).hurtAndBreak(1, player, hand);
		}
		return InteractionResult.SUCCESS;
	}
}
