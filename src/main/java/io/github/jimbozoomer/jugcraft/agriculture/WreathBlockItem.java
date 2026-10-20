package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.gameevent.GameEvent;

/** Walls use the existing block. Doors need an attachment that fits either physical face of the moving panel. */
public final class WreathBlockItem extends BlockItem {
	public WreathBlockItem(Block block, Properties properties) { super(block, properties); }

	@Override public InteractionResult useOn(UseOnContext context) {
		var level = context.getLevel(); var pos = context.getClickedPos(); var door = level.getBlockState(pos);
		if (!(door.getBlock() instanceof DoorBlock)) return super.useOn(context);
		var face = context.getClickedFace(); var front = DoorWreath.frontFace(door);
		var player = context.getPlayer(); var stack = context.getItemInHand();
		if (face.getAxis() != front.getAxis() || (player != null
				&& (!level.mayInteract(player, pos) || !player.mayUseItemAt(pos, face, stack)))) return InteractionResult.FAIL;
		if (level instanceof ServerLevel server) {
			var state = stack.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY).apply(getBlock().defaultBlockState());
			if (DoorWreath.attach(server, pos, face == front, state) == null) return InteractionResult.FAIL;
			stack.consume(1, player);
			level.playSound(null, pos, SoundEvents.GRASS_PLACE, SoundSource.BLOCKS, 1.0F, 0.8F);
			level.gameEvent(player, GameEvent.ENTITY_PLACE, pos);
		}
		return InteractionResult.SUCCESS;
	}
}
