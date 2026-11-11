package io.github.jimbozoomer.jugcraft.nuclear;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A diesel generator room panel whose controls work (round 31 part 2): the engine control panel, the generator control
 * panel and the generator protection relays, labelled by train (EDG 1A, EDG 1B). A click on its front is turned into
 * the piece's own frame and handed to DieselPanels with the generator the panel is linked to (LivePanelBlockEntity).
 */
public class EdgPanelBlock extends LivePanelBlock {
	private final int panel;

	/** panel: DieselPanels.ENGINE, GENERATOR or PROTECTION. */
	public EdgPanelBlock(Properties properties, int width, int panel) {
		super(properties, width, 2, 2);
		this.panel = panel;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		Direction facing = state.getValue(FACING);
		BlockPos origin = pos.subtract(offset(facing, state.getValue(partProperty())));
		if (!(level.getBlockEntity(origin) instanceof LivePanelBlockEntity live)) {
			return InteractionResult.PASS;
		}
		if (live.generator == null || !(level.getBlockEntity(live.generator) instanceof EquipmentBlockEntity edg)
				|| edg.kind() != EquipmentKind.EMERGENCY_DIESEL_GENERATOR) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.edg_panel_no_edg"));
			return InteractionResult.SUCCESS_SERVER;
		}
		double[] at = local(facing, origin, hit.getLocation());
		// The layout is across the whole piece; the placing block is its middle column.
		double x = at[0] + 16 * (origin() % width);
		return DieselPanels.click((ServerLevel) level, pos, player, edg, panel, x, at[1]);
	}
}
