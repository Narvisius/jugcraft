package io.github.jimbozoomer.jugcraft.warehouse;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Part W5: the switchboard's screen (client/warehouse/SwitchboardScreen.java): the incomer (what comes in, what the
 * warehouse draws, what is stored, MAIN OFF), the six circuits (switch each on or off). No slots; while it is open the
 * server sends its terminal's CONTROL page (ControlPayload) every second.
 */
public class SwitchboardMenu extends AbstractContainerMenu {
	/** Buttons: 0..5 a circuit on or off, MAIN the main switch. */
	public static final int MAIN = 6;
	public final BlockPos board;
	private final Player player;
	private int ticks;

	public SwitchboardMenu(int id, Inventory inventory, BlockPos board) {
		super(JugcraftWarehouse.SWITCHBOARD_MENU, id);
		this.board = board;
		this.player = inventory.player;
	}

	@Override
	public boolean stillValid(Player player) {
		return !player.isSpectator() && player.level().isLoaded(board) && player.level().getBlockEntity(board) instanceof SwitchboardBlockEntity
				&& player.distanceToSqr(Vec3.atCenterOf(board)) <= 10 * 10;
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		return ItemStack.EMPTY;
	}

	@Override
	public boolean clickMenuButton(Player player, int id) {
		if (!(player instanceof ServerPlayer server) || !stillValid(player) || !(player.level().getBlockEntity(board) instanceof SwitchboardBlockEntity sb)) {
			return false;
		}
		TerminalBlockEntity t = sb.terminal();
		if (t != null && !t.mayUse(player)) {
			t.deny(player);
			return false;
		}
		if (id == MAIN) {
			sb.setMain(!sb.mainOn());
			return true;
		}
		if (id >= 0 && id < WarehousePower.Circuit.values().length) {
			if (t == null) {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.warehouse.no_terminal"));
				return false;
			}
			t.toggleCircuit(id);
			ServerPlayNetworking.send(server, t.controlView(server));
			return true;
		}
		return false;
	}

	@Override
	public void broadcastChanges() {
		super.broadcastChanges();
		if (player instanceof ServerPlayer server && ticks++ % 20 == 0 && server.level().getBlockEntity(board) instanceof SwitchboardBlockEntity sb) {
			TerminalBlockEntity t = sb.terminal();
			if (t != null) {
				ServerPlayNetworking.send(server, t.controlView(server));
			}
		}
	}
}
