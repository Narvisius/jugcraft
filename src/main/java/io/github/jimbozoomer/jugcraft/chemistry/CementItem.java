package io.github.jimbozoomer.jugcraft.chemistry;

import io.github.jimbozoomer.jugcraft.town.TownProtection;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/** Cement (batch 32): used on construction foam, it sets the foam into concrete, a block at a time. */
public class CementItem extends ConstructionChemistry.Described {
	public CementItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		if (!level.getBlockState(pos).is(ConstructionChemistry.CONSTRUCTION_FOAM)) {
			return InteractionResult.PASS;
		}
		Player player = context.getPlayer();
		ItemStack stack = context.getItemInHand();
		if (player != null && (!player.mayUseItemAt(pos, context.getClickedFace(), stack) || !level.mayInteract(player, pos)
				|| TownProtection.denies(player, level, pos))) {
			return InteractionResult.FAIL;
		}
		if (!level.isClientSide()) {
			level.setBlockAndUpdate(pos, ConstructionChemistry.CONCRETE.defaultBlockState());
			level.playSound(null, pos, SoundEvents.MUD_PLACE, SoundSource.BLOCKS, 1.0F, 0.8F);
			stack.consume(1, player);
		}
		return InteractionResult.SUCCESS;
	}
}
