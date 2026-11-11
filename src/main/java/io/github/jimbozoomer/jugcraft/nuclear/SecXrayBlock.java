package io.github.jimbozoomer.jugcraft.nuclear;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The gatehouse's X-ray baggage scanner (SecurityGatehouse; three blocks long, two high): the roller bed out, the
 * tunnel housing, the belt in; its operator's monitor over the out end. Right-click it holding something to put it
 * through (its screen says CLEAR or SEARCH; you keep it); empty-handed, how many bags it has seen.
 */
public class SecXrayBlock extends LivePanelBlock {
	public SecXrayBlock(Properties properties) {
		super(properties, 3, 2, 1);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		Direction facing = state.getValue(FACING);
		int p = state.getValue(partProperty()), c = p % width, r = p / width;
		if (r == 0) {
			return switch (c) {
				case 0 -> Shapes.or(turned(new double[] {0, 0, 1.5, 16, 8.5, 14.5}, facing), turned(new double[] {12, 0, 1, 16, 16, 15}, facing));
				case 1 -> turned(new double[] {0, 0, 1, 16, 16, 15}, facing);
				default -> Shapes.or(turned(new double[] {0, 0, 1, 4, 16, 15}, facing), turned(new double[] {4, 0, 2, 16, 8.5, 14}, facing));
			};
		}
		return switch (c) {
			case 0 -> Shapes.or(turned(new double[] {0.6, 5, 13.2, 10.6, 13, 15.4}, facing), turned(new double[] {12, 0, 1, 16, 6, 15}, facing),
					turned(new double[] {5.2, 0, 13.6, 6, 5, 14.4}, facing));
			case 1 -> turned(new double[] {0, 0, 1, 16, 6, 15}, facing);
			default -> turned(new double[] {0, 0, 1, 4, 6, 15}, facing);
		};
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		return use(state, level, pos, player, ItemStack.EMPTY);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
			BlockHitResult hit) {
		return use(state, level, pos, player, stack);
	}

	private InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, ItemStack stack) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		BlockPos origin = pos.subtract(offset(state.getValue(FACING), state.getValue(partProperty())));
		if (!(level.getBlockEntity(origin) instanceof LivePanelBlockEntity x)) {
			return InteractionResult.PASS;
		}
		if (stack.isEmpty()) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.xr_status", x.xrCount));
			return InteractionResult.SUCCESS_SERVER;
		}
		boolean search = SecurityGatehouse.scan(x, (ServerLevel) level, stack);
		player.sendOverlayMessage(Component.translatable(search ? "message.jugcraft.nuclear.xr_search" : "message.jugcraft.nuclear.xr_clear",
				stack.getHoverName()));
		return InteractionResult.SUCCESS_SERVER;
	}
}
