package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * A Dust Sheet: use it on a chair, slab, stairs, chest or another block of the {@code jugcraft:dust_sheet_coverable}
 * block tag to drape it over that block ({@link DustSheetBlock}). It goes over a block that has a use of its own (a
 * chair you would sit in, a chest you would open) too: JugcraftAgriculture lets the sheet in hand act first.
 */
public class DustSheetItem extends Item {
	public DustSheetItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		return tryCover(context.getPlayer(), context.getLevel(), context.getHand(), context.getClickedPos(), context.getClickedFace());
	}

	/** Covers the block at {@code pos} with the sheet in {@code hand} if it may be covered; PASS if not. */
	public static InteractionResult tryCover(@Nullable Player player, Level level, InteractionHand hand, BlockPos pos, Direction side) {
		if (player == null || player.isSpectator() || !DustSheetBlock.coverable(level, pos, level.getBlockState(pos))
				|| !player.mayUseItemAt(pos, side, player.getItemInHand(hand))) {
			return InteractionResult.PASS;
		}
		if (level instanceof ServerLevel server) {
			DustSheetBlock.cover(server, pos, player);
			player.getItemInHand(hand).consume(1, player);
		}
		return InteractionResult.SUCCESS;
	}
}
