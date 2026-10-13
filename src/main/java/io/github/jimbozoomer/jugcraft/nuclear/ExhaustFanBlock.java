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
 * A battery room's exhaust fan (round 26, #9; Justin: "Hydrogen monitor and fan"): a wall fan over the room's
 * hydrogen exhaust louvre, its HAND-OFF-AUTO station under it (tools/plant_panels.py exhaust_fan). Right-click the
 * selector to turn it: HAND, OFF, AUTO, round again. On AUTO it runs slowly while the battery floats and fast while it
 * charges hard; on HAND fast; on OFF it stops, and the hydrogen monitor's reading creeps up (client/EdgTelemetry.java
 * exhaustFan, hydrogen). Right-click anywhere else to see how it is set. Its selector is kept by its placing block
 * (LivePanelBlockEntity mode).
 */
public class ExhaustFanBlock extends WallLivePanelBlock {
	public static final String[] MODES = {"HAND", "OFF", "AUTO"};
	public static final int HAND = 0, OFF = 1, AUTO = 2;
	/** The selector's middle and how far round it a click turns it (model pixels, its lower block). */
	static final double[] SELECTOR = {8.0, 8.2, 1.4};

	public ExhaustFanBlock(Properties properties) {
		super(properties, 2, 5.6);
	}

	/** Whether this point of its front (model pixels) is on the selector. */
	public static boolean selectorAt(double x, double y) {
		return Math.abs(x - SELECTOR[0]) <= SELECTOR[2] && Math.abs(y - SELECTOR[1]) <= SELECTOR[2];
	}

	/** The selector turned one click: HAND to OFF to AUTO to HAND. */
	public static void turn(ServerLevel level, LivePanelBlockEntity fan) {
		fan.mode = (fan.mode + 1) % MODES.length;
		fan.changedMode(level);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		Direction facing = state.getValue(FACING);
		BlockPos origin = pos.subtract(offset(facing, state.getValue(partProperty())));
		if (!(level.getBlockEntity(origin) instanceof LivePanelBlockEntity fan)) {
			return InteractionResult.PASS;
		}
		double[] at = local(facing, origin, hit.getLocation());
		String label = LivePanelBlockEntity.variant(state) == 1 ? "1B" : "1A";
		if (selectorAt(at[0], at[1]) && at[2] > 2.5) {
			turn((ServerLevel) level, fan);
			level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.5F, 1.2F);
		}
		player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.exhaust_fan", label, MODES[fan.mode % MODES.length]));
		return InteractionResult.SUCCESS_SERVER;
	}
}
