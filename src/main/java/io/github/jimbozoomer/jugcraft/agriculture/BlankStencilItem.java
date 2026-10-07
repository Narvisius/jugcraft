package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A Blank Stencil. Used on a carved side of a hand-carved pumpkin, it traces that side's design into a
 * Pumpkin Stencil ({@link JugcraftAgriculture#STENCIL}); the pumpkin is not changed. Held in the other
 * hand while carving, the stencil can be pressed into any pumpkin (blown up three times on a giant one).
 * The design is read from the block on the server.
 */
public class BlankStencilItem extends Item {
	public BlankStencilItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		Direction side = context.getClickedFace();
		BlockState state = level.getBlockState(pos);
		if (side.getAxis() == Direction.Axis.Y || !(state.getBlock() instanceof CarvedPumpkinBlock)
				|| !(level.getBlockEntity(pos) instanceof CarvedPumpkinBlockEntity pumpkin)) {
			return InteractionResult.PASS;
		}
		int[] face = pumpkin.carving().face(PumpkinCarving.faceIndex(state.getValue(CarvedPumpkinBlock.FACING), side));
		if (CarvingFace.isBlank(face)) {
			return InteractionResult.PASS;
		}
		if (!level.isClientSide()) {
			ItemStack stencil = new ItemStack(JugcraftAgriculture.item("pumpkin_stencil"));
			stencil.set(JugcraftAgriculture.STENCIL, PumpkinCarving.BLANK.withFace(0, face));
			Player player = context.getPlayer();
			context.getItemInHand().consume(1, player);
			if (player == null || !player.getInventory().add(stencil)) {
				Block.popResourceFromFace(level, pos, side, stencil);
			}
			level.playSound(null, pos, SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 1.0F);
		}
		return InteractionResult.SUCCESS;
	}
}
