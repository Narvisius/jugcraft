package io.github.jimbozoomer.jugcraft.tower;

import io.github.jimbozoomer.jugcraft.drone.DepotDisplayBlockEntity;
import io.github.jimbozoomer.jugcraft.drone.DroneDepots;
import io.github.jimbozoomer.jugcraft.drone.DroneTerminalBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/** A passive desk readout. Tower construction pins its link; there is no server ticker. */
public final class ConsoleDeskBlock extends FurnitureBlock implements EntityBlock, DepotDisplayBlockEntity.Display {
	/** Client callback keeps the clicked desk alongside the target terminal. */
	public static java.util.function.BiConsumer<BlockPos, BlockPos> openScreen = (terminal, access) -> { };
	public ConsoleDeskBlock(Properties properties) {
		super(properties, new double[] {0, 0, 0, 16, 12, 16}, new double[] {.6, 15.6, 2, 15.4, 25, 3.75});
	}

	@Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new DepotDisplayBlockEntity(JugcraftTower.CONSOLE_ENTITY, pos, state);
	}
	@Override public boolean isAnchor(BlockState state) { return true; }
	@Override public int linkRange() { return 24; }

	@Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof DepotDisplayBlockEntity display) {
			var nearest = DroneDepots.nearest(level, pos, linkRange());
			if (nearest != null) display.pin(nearest.getBlockPos());
		}
	}

	@Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level.getBlockEntity(pos) instanceof DepotDisplayBlockEntity display && display.terminal() != null
				&& level.getBlockEntity(display.terminal()) instanceof DroneTerminalBlockEntity terminal) {
			if (level.isClientSide()) openScreen.accept(display.terminal(), pos);
			else {
				// Match direct terminal use: imported, unowned depots belong to their first operator.
				if (terminal.owner() == null) {
					terminal.setOwner(player.getUUID());
					player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("message.jugcraft.drone.claimed"));
				}
				terminal.rescanNow(); terminal.refreshClients();
			}
		}
		return InteractionResult.SUCCESS;
	}

	/** A supplied access position grants only a nearby real console's exact loaded terminal link. */
	public static boolean mayAccessTerminal(ServerPlayer player, BlockPos terminalPos, BlockPos accessPos) {
		var level = player.level();
		if (!level.isLoaded(terminalPos) || !(level.getBlockEntity(terminalPos) instanceof DroneTerminalBlockEntity)
				|| !level.isLoaded(accessPos) || player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(accessPos)) > 8 * 8) return false;
		if (terminalPos.equals(accessPos)) return true;
		var state = level.getBlockState(accessPos);
		if (state.getBlock() instanceof MasterConsoleBlock console) {
			var display = console.display(level, accessPos, state);
			return display != null && display.getBlockPos().distSqr(terminalPos) <= console.linkRange() * console.linkRange()
					&& terminalPos.equals(display.terminal());
		}
		return level.getBlockState(accessPos).getBlock() instanceof ConsoleDeskBlock desk
				&& accessPos.distSqr(terminalPos) <= desk.linkRange() * desk.linkRange()
				&& level.getBlockEntity(accessPos) instanceof DepotDisplayBlockEntity display
				&& terminalPos.equals(display.terminal());
	}
}
