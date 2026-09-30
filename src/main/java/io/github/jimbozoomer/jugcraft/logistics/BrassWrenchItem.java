package io.github.jimbozoomer.jugcraft.logistics;

import io.github.jimbozoomer.jugcraft.energy.CableBlock;
import io.github.jimbozoomer.jugcraft.fluid.ElectricPumpBlock;
import io.github.jimbozoomer.jugcraft.fluid.FluidPipeBlock;
import io.github.jimbozoomer.jugcraft.fluid.FluidTankBlock;
import io.github.jimbozoomer.jugcraft.machine.LargeMachineBlock;
import io.github.jimbozoomer.jugcraft.machine.MachineBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Brass wrench, for Jugcraft machines and logistics blocks:
 * <ul>
 * <li>Right-click turns a machine to face the next way (extractors and sorters cycle through all
 * six directions). Multi-block machines cannot be turned.</li>
 * <li>Sneak + right-click dismantles the block: it drops as an item, and anything inside drops
 * too, so nothing is lost.</li>
 * </ul>
 */
public class BrassWrenchItem extends Item {
	public BrassWrenchItem(Properties properties) {
		super(properties);
	}

	public static boolean isWrenchable(Block block) {
		return block instanceof MachineBlock || block instanceof CableBlock || block instanceof FluidPipeBlock
				|| block instanceof ItemPipeBlock || block instanceof PneumaticExtractorBlock || block instanceof ItemSorterBlock
				|| block instanceof FluidTankBlock || block instanceof ElectricPumpBlock;
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		BlockState state = level.getBlockState(pos);
		Player player = context.getPlayer();
		if (!isWrenchable(state.getBlock())) {
			return InteractionResult.PASS;
		}
		if (player != null && player.isSecondaryUseActive()) {
			if (!level.isClientSide()) {
				level.destroyBlock(pos, !player.isCreative(), player);
			}
			return InteractionResult.SUCCESS;
		}
		BlockState turned = turn(state);
		if (turned == null) {
			if (player != null && !level.isClientSide() && state.getBlock() instanceof LargeMachineBlock) {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.wrench.large"));
			}
			return InteractionResult.PASS;
		}
		if (!level.isClientSide()) {
			level.setBlockAndUpdate(pos, turned);
		}
		return InteractionResult.SUCCESS;
	}

	private static BlockState turn(BlockState state) {
		Block block = state.getBlock();
		if (block instanceof LargeMachineBlock) {
			return null;
		}
		if (block instanceof MachineBlock) {
			return state.setValue(MachineBlock.FACING, state.getValue(MachineBlock.FACING).getClockWise());
		}
		if (block instanceof PneumaticExtractorBlock || block instanceof ItemSorterBlock) {
			Direction facing = state.getValue(PneumaticExtractorBlock.FACING);
			return state.setValue(PneumaticExtractorBlock.FACING, Direction.values()[(facing.ordinal() + 1) % 6]);
		}
		return null;
	}
}
