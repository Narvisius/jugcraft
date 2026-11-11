package io.github.jimbozoomer.jugcraft.nuclear;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A plant computer room piece (older rooms round 2: ServerRoom; tools/server_room.py): a rack, the UPS lineup (module,
 * battery cabinet, maintenance bypass, PDU), a CRAC, an agent cylinder, the clean agent releasing panel, its horn and
 * strobe, the plant process computer console. Its hitbox follows what it is. A click on a rack swings its front door open
 * or shut (OPEN, Justin: "Open on click"); on a CRAC's isolator, turns the unit off or on; on the releasing panel's keys,
 * ACK, SILENCE, RESET, DRILL; anywhere else, says how the room stands. The room only looks live: no piece ticks; the
 * UPS module (the hub) has a scheduled tick when it is put down (it looks for the room's pieces) and while its agent
 * system is busy (ServerRoom.step).
 */
public class ServerRoomPanelBlock extends LivePanelBlock {
	public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
	/** The releasing panel's keys (tools/server_room.py RP_KEYS: middles across, their row; model pixels) and their reach. */
	static final double[] KEYS_X = {4.6, 6.8, 9.0, 11.2};
	static final double KEYS_Y = 13.8, KEY_R = 0.95;
	/** A CRAC's isolator (tools/server_room.py crac_unit_boxes: across the two blocks, its handle). */
	static final double[] ISOLATOR = {21.4, 20.6, 26.6, 24.8};

	public ServerRoomPanelBlock(Properties properties, int width, int rows, int variants) {
		super(properties, width, rows, variants);
		registerDefaultState(defaultBlockState().setValue(OPEN, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(OPEN);
	}

	@Override
	public @Nullable BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
		BlockState state = super.getStateForPlacement(context);
		return state == null ? null : state.setValue(OPEN, false);
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		super.onPlace(state, level, pos, oldState, movedByPiston);
		if (!level.isClientSide() && !oldState.is(this) && ServerRoom.kind(state) == ServerRoom.HUB && state.getValue(partProperty()) == origin()) {
			// The hub looks for its CRACs, detectors and cylinders once the room round it is down.
			level.scheduleTick(pos, this, 40);
		}
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, net.minecraft.util.RandomSource random) {
		if (ServerRoom.kind(state) == ServerRoom.HUB && level.getBlockEntity(pos) instanceof LivePanelBlockEntity hub) {
			ServerRoom.step(hub, level);
		}
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		double[] box = switch (ServerRoom.kind(state)) {
			case ServerRoom.RACK -> new double[] {0.15, 0, 0.55, 15.85, 16, 15.45};
			case ServerRoom.CYLINDER -> new double[] {3.4, 0, 2.6, 12.6, 16, 11.8};
			case ServerRoom.RELEASE -> new double[] {2.6, 0, 0, 13.4, 16, 3.0};
			case ServerRoom.ALARM_HORN -> new double[] {4.6, 4.0, 0, 11.4, 14.4, 3.1};
			case ServerRoom.PPC -> new double[] {0, 0, 1, 16, 13, 16};
			default -> new double[] {0.2, 0, 0.5, 15.8, 16, 15.4};
		};
		return turned(box, state.getValue(FACING));
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
		ServerLevel server = (ServerLevel) level;
		int kind = ServerRoom.kind(state);
		double[] at = local(facing, origin, hit.getLocation());
		if (kind == ServerRoom.RACK) {
			boolean open = !state.getValue(OPEN);
			for (int p = 0; p < width * rows; p++) {
				BlockPos there = origin.offset(offset(facing, p));
				BlockState s = level.getBlockState(there);
				if (s.is(this)) {
					level.setBlock(there, s.setValue(OPEN, open), Block.UPDATE_ALL);
				}
			}
			level.playSound(null, pos, open ? SoundEvents.IRON_TRAPDOOR_OPEN : SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.5F, 1.4F);
			return InteractionResult.SUCCESS_SERVER;
		}
		LivePanelBlockEntity hub = kind == ServerRoom.HUB ? panel : panel.panel != null && level.getBlockEntity(panel.panel)
				instanceof LivePanelBlockEntity h ? h : ServerRoom.nearestHub(server, origin);
		if (kind == ServerRoom.RELEASE && hub != null) {
			for (int k = 0; k < KEYS_X.length; k++) {
				if (Math.hypot(at[0] - KEYS_X[k], at[1] - KEYS_Y) <= KEY_R) {
					level.playSound(null, pos, SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.8F, 1.3F);
					ServerRoom.key(hub, server, k, player);
					return InteractionResult.SUCCESS_SERVER;
				}
			}
		}
		if (kind == ServerRoom.COOLER && at[0] >= ISOLATOR[0] && at[0] <= ISOLATOR[2] && at[1] >= ISOLATOR[1] && at[1] <= ISOLATOR[3]) {
			panel.mode = panel.mode == 0 ? 1 : 0;
			panel.syncNow(server);
			ServerRoom.isolator(server, origin);
			level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.7F, panel.mode == 0 ? 1.0F : 0.8F);
			String unit = LivePanelBlockEntity.variant(state) == 0 ? "CRAC-1" : "CRAC-2";
			player.sendOverlayMessage(Component.translatable(panel.mode == 0 ? "message.jugcraft.nuclear.srv_crac_on" : "message.jugcraft.nuclear.srv_crac_off", unit));
			return InteractionResult.SUCCESS_SERVER;
		}
		player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.srv_status", ServerRoom.describe(hub, kind)));
		return InteractionResult.SUCCESS_SERVER;
	}
}
