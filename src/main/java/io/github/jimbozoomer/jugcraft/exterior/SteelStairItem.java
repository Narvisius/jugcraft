package io.github.jimbozoomer.jugcraft.exterior;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The Steel Stair Section's item: places a section climbing away from you; used on the top of a section climbing the
 * same way, it carries the flight on (the new section's first cell one along and one up from that section's top).
 */
public class SteelStairItem extends BlockItem {
	public SteelStairItem(Block block, Properties properties) {
		super(block, properties);
	}

	@Override
	public @Nullable BlockPlaceContext updatePlacementContext(BlockPlaceContext context) {
		BlockPos below = context.getClickedPos().below();
		BlockState top = context.getLevel().getBlockState(below);
		Direction look = context.getHorizontalDirection();
		if (top.getBlock() instanceof SteelStairBlock && top.getValue(SteelStairBlock.PART) == 3 && top.getValue(SteelStairBlock.FACING) == look) {
			BlockPos origin = SteelStairBlock.origin(below, top).relative(look, 3).above(2);
			return BlockPlaceContext.at(context, origin, Direction.UP);
		}
		return context;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
		out.accept(Component.translatable("tooltip.jugcraft.exterior.steel_stair").withStyle(ChatFormatting.GRAY));
	}
}
