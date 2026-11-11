package io.github.jimbozoomer.jugcraft.nuclear;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A diesel room's monorail hoist pendant station (round 31 part 3, tools/diesel_fittings.py): on the wall under the
 * monorail's end. Its buttons drive the nearest monorail (Monorail): UP, DOWN, EAST and WEST a block a click
 * (Justin: "One step per click"), STOP holds it where it is.
 */
public class HoistPendantBlock extends WallLivePanelBlock {
	/** Its buttons' middles on its front (model pixels, both blocks) and their reach: STOP, UP, DOWN, EAST, WEST. */
	static final double[][] BUTTONS = {{8.0, 20.6, 1.5}, {9.4, 17.4, 1.2}, {9.4, 14.6, 1.2}, {9.4, 11.6, 1.2}, {9.4, 8.8, 1.2}};

	public HoistPendantBlock(Properties properties) {
		super(properties, 2, 6.3);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		Direction facing = state.getValue(FACING);
		BlockPos origin = pos.subtract(offset(facing, state.getValue(partProperty())));
		double[] at = local(facing, origin, hit.getLocation());
		int button = -1;
		for (int k = 0; k < BUTTONS.length; k++) {
			if (Math.hypot(at[0] - BUTTONS[k][0], at[1] - BUTTONS[k][1]) <= BUTTONS[k][2]) {
				button = k;
			}
		}
		if (button < 0) {
			return InteractionResult.PASS;
		}
		EquipmentBlockEntity rail = Monorail.near((ServerLevel) level, origin);
		if (rail == null) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.hoist_no_rail", Monorail.REACH));
			return InteractionResult.SUCCESS_SERVER;
		}
		level.playSound(null, pos, SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.7F, 1.3F);
		Monorail.press((ServerLevel) level, rail, button, player);
		return InteractionResult.SUCCESS_SERVER;
	}
}
