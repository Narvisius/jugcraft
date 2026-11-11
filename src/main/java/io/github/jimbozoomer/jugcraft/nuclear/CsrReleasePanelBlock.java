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
 * The cable spreading room's pre-action releasing panel (punch list #15, tools/spreading_room.py release_panel): on the
 * wall by the door, its lamps and display live (client/EdgTelemetry.java csr). Its keys: ACK, SILENCE, RESET and DRILL
 * (SpreadingRoomFire.key); a click elsewhere on it says how the system stands. Turbine hall part 3: lettered for the area it
 * protects (its variant: the cable spreading room, the warehouse, the turbine building's lube oil room), stepped on by
 * sneaking and clicking it with an empty hand.
 */
public class CsrReleasePanelBlock extends WallLivePanelBlock {
	/** The keys' middles across the front (model pixels: x across, y up from the floor), their row, their reach. */
	static final double[] KEYS_X = {4.9, 7.0, 9.1, 11.2};
	static final double KEYS_Y = 13.7, KEY_R = 0.95;

	/** Its labels (tools/spreading_room.py ZONES: turbine hall part 3 letters it for the area it protects). */
	public static final String[] ZONES = {"CSR", "WH", "LOR"};

	public CsrReleasePanelBlock(Properties properties) {
		super(properties, ZONES.length, 3.2);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		Direction facing = state.getValue(FACING);
		BlockPos origin = pos.subtract(offset(facing, state.getValue(partProperty())));
		if (!(level.getBlockEntity(origin) instanceof LivePanelBlockEntity panel)) {
			return InteractionResult.PASS;
		}
		double[] at = local(facing, origin, hit.getLocation());
		ServerLevel server = (ServerLevel) level;
		net.minecraft.world.level.block.state.properties.IntegerProperty v = variantProperty();
		if (player.isShiftKeyDown() && player.getMainHandItem().isEmpty() && v != null) {
			// Sneaking with an empty hand: lettered for the next area (the spreading room, the warehouse, the lube oil room).
			int next = (state.getValue(v) + 1) % variants;
			for (int p = 0; p < width * rows; p++) {
				BlockPos part = origin.offset(offset(facing, p));
				BlockState s = level.getBlockState(part);
				if (s.getBlock() == this) {
					level.setBlock(part, s.setValue(v, next), net.minecraft.world.level.block.Block.UPDATE_CLIENTS
							| net.minecraft.world.level.block.Block.UPDATE_KNOWN_SHAPE);
				}
			}
			level.playSound(null, pos, SoundEvents.ITEM_FRAME_ROTATE_ITEM, SoundSource.BLOCKS, 0.6F, 1.2F);
			player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.relay_relabel", ZONES[next]));
			return InteractionResult.SUCCESS_SERVER;
		}
		for (int k = 0; k < KEYS_X.length; k++) {
			if (Math.hypot(at[0] - KEYS_X[k], at[1] - KEYS_Y) <= KEY_R) {
				level.playSound(null, pos, SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.8F, 1.3F);
				SpreadingRoomFire.key(panel, server, k, player);
				return InteractionResult.SUCCESS_SERVER;
			}
		}
		String[] lines = SpreadingRoomFire.display(panel.csrPhase, panel.csrFlags, panel.csrSince, level.getGameTime(), panel.csrReleasedAt);
		player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.csr_status", lines[0], lines[1]));
		return InteractionResult.SUCCESS_SERVER;
	}
}
