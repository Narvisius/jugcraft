package io.github.jimbozoomer.jugcraft.warehouse;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * The Warehouse Control Terminal's screen (client/warehouse/TerminalScreen.java): no slots; while it is open the
 * server sends its pages (TerminalPayload) every second; part W5: the CONTROL page (ControlPayload) and the LAYOUT
 * page's map (MapPayload, when it changed).
 */
public class TerminalMenu extends AbstractContainerMenu {
	public final BlockPos terminal;
	private final Player player;
	private int ticks;
	private int mapSent = -1;

	public TerminalMenu(int id, Inventory inventory, BlockPos terminal) {
		super(JugcraftWarehouse.TERMINAL_MENU, id);
		this.terminal = terminal;
		this.player = inventory.player;
	}

	@Override
	public boolean stillValid(Player player) {
		return !player.isSpectator() && player.level().isLoaded(terminal) && player.level().getBlockEntity(terminal) instanceof TerminalBlockEntity
				&& player.distanceToSqr(Vec3.atCenterOf(terminal)) <= 10 * 10;
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		return ItemStack.EMPTY;
	}

	@Override
	public void broadcastChanges() {
		super.broadcastChanges();
		if (player instanceof ServerPlayer server && ticks++ % 20 == 0
				&& server.level().getBlockEntity(terminal) instanceof TerminalBlockEntity t) {
			ServerPlayNetworking.send(server, t.snapshot(server.level().getGameTime()));
			ServerPlayNetworking.send(server, WarehouseBuilding.view(server.level(), t, t.network(), t.occupied()));
			// Part W5: the CONTROL page, and the LAYOUT page's map when it changed (or now and then).
			ServerPlayNetworking.send(server, t.controlView(server));
			MapPayload map = t.map(server.level());
			if (map.version() != mapSent || ticks % 200 == 1) {
				mapSent = map.version();
				ServerPlayNetworking.send(server, map);
			}
		}
	}
}
