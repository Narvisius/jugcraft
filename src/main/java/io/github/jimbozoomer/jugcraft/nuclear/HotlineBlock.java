package io.github.jimbozoomer.jugcraft.nuclear;

import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The plant manager's direct line to the control room (tools/admin_rooms.py hotline_boxes): lift the handset (right-click)
 * and the shift manager says how the unit stands, from the nearest reactor: its power and output, the alarms in, a trip.
 */
public class HotlineBlock extends MultiFurnitureBlock {
	public HotlineBlock(Properties properties) {
		super(properties, 1, 1, true);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return turned(new double[] {3, 1.2, 0, 13, 15.8, 5}, state.getValue(FACING));
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		level.playSound(null, pos, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, 0.4F, 1.9F);
		player.sendSystemMessage(report((ServerLevel) level, pos));
		return InteractionResult.SUCCESS_SERVER;
	}

	/** What the shift manager says. */
	public static Component report(ServerLevel level, BlockPos pos) {
		EquipmentBlockEntity vessel = StatusScreenBlockEntity.nearestVessel(level, pos);
		if (vessel == null) {
			return Component.translatable("message.jugcraft.nuclear.hotline_none").withStyle(ChatFormatting.GRAY);
		}
		ReactorCore core = vessel.core;
		int power = (int) Math.round(Math.max(0, core.heat()) * 100);
		int mwe = core.electric > 0 ? (int) Math.round(DeskScreenBlockEntity.RATED_MWE * Math.min(1, Math.max(0, core.heat()))) : 0;
		int alarms = Integer.bitCount(core.alarms);
		String state;
		if (core.scrammed) {
			state = String.format(Locale.ROOT, "the unit tripped; we're stabilizing in hot standby, %d alarm%s in", alarms, alarms == 1 ? "" : "s");
		} else if (core.shutDown()) {
			state = "the unit is shut down, " + OpsOffice.mode(RelayRoom.Reactor.of(core, 0)).trim().toLowerCase(Locale.ROOT);
		} else {
			state = String.format(Locale.ROOT, "Unit 1 is at %d%% power, %d MWe on the grid, %s", power, mwe,
					alarms == 0 ? "no alarms in" : alarms + " alarm" + (alarms == 1 ? "" : "s") + " in");
		}
		return Component.translatable("message.jugcraft.nuclear.hotline", state).withStyle(core.scrammed ? ChatFormatting.RED : ChatFormatting.YELLOW);
	}
}
