package io.github.jimbozoomer.jugcraft.control;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * The logic controller's screen (batch 36): its rules and channel readings as menu data, and every edit a menu button
 * ({@code rule * 8 + op}, see {@link LogicControllerBlockEntity#click}) checked on the server.
 */
public class LogicControllerMenu extends AbstractContainerMenu {
	public static final double RANGE = 8.0;
	public final BlockPos pos;
	private final ContainerData data;
	private final @Nullable LogicControllerBlockEntity controller;

	/** Client side. */
	public LogicControllerMenu(int containerId, Inventory inventory, BlockPos pos) {
		this(containerId, inventory, pos, new SimpleContainerData(LogicControllerBlockEntity.DATA_COUNT), null);
	}

	LogicControllerMenu(int containerId, Inventory inventory, BlockPos pos, ContainerData data,
			@Nullable LogicControllerBlockEntity controller) {
		super(JugcraftControl.CONTROLLER_MENU, containerId);
		this.pos = pos;
		this.data = data;
		this.controller = controller;
		checkContainerDataCount(data, LogicControllerBlockEntity.DATA_COUNT);
		addDataSlots(data);
	}

	public int rule(int rule, int field) {
		return data.get(rule * LogicControllerBlockEntity.FIELDS + field);
	}

	/** A channel's reading, 0 to 100, or -1 with no sensor. */
	public int reading(int channel) {
		return data.get(LogicControllerBlockEntity.RULES * LogicControllerBlockEntity.FIELDS + channel) - 1;
	}

	public boolean isOn(int channel) {
		return (data.get(LogicControllerBlockEntity.DATA_COUNT - 1) & (1 << channel)) != 0;
	}

	@Override
	public boolean stillValid(Player player) {
		if (player.level().isClientSide()) {
			return true;
		}
		return player.level().getBlockEntity(pos) == controller && controller != null && !controller.isRemoved()
				&& player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= RANGE * RANGE;
	}

	@Override
	public boolean clickMenuButton(Player player, int id) {
		if (!(player instanceof ServerPlayer server) || controller == null || !stillValid(player)
				|| !player.mayBuild() || !server.level().mayInteract(player, pos)) {
			return false;
		}
		boolean changed = controller.click(id / 8, id % 8);
		if (changed) {
			broadcastChanges();
		}
		return changed;
	}

	@Override
	public ItemStack quickMoveStack(Player player, int slot) {
		return ItemStack.EMPTY;
	}
}
