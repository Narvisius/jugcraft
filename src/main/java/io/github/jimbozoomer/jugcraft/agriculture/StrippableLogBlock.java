package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A Jugcraft log or wood block (chestnut, larch). Any axe (item tag {@code minecraft:axes}) strips it, keeping its axis,
 * as vanilla axes strip vanilla logs: the block handles the axe itself, so no vanilla tool data changes.
 */
public class StrippableLogBlock extends RotatedPillarBlock {
	private final String strippedId;

	public StrippableLogBlock(Properties properties, String strippedId) {
		super(properties);
		this.strippedId = strippedId;
	}

	/** The stripped form of this log. */
	public Block stripped() {
		return JugcraftAgriculture.block(strippedId);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		if (!stack.is(ItemTags.AXES)) {
			return super.useItemOn(stack, state, level, pos, player, hand, hit);
		}
		level.playSound(player, pos, SoundEvents.AXE_STRIP.value(), SoundSource.BLOCKS, 1.0F, 1.0F);
		if (!level.isClientSide()) {
			level.setBlock(pos, stripped().defaultBlockState().setValue(AXIS, state.getValue(AXIS)), Block.UPDATE_ALL_IMMEDIATE);
			level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, state));
			stack.hurtAndBreak(1, player, hand);
		}
		return InteractionResult.SUCCESS;
	}
}
