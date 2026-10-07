package io.github.jimbozoomer.jugcraft.tools;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * The rocket pack, worn in the chest slot. Holding jump in the air fires it: the client lifts the
 * player (movement is the client's) and tells the server each tick ({@link RocketThrustPayload}), which
 * takes {@link #ENERGY_PER_TICK} JE and cancels fall damage. With no charge it does nothing.
 * <p>On a dedicated server with {@code allow-flight=false}, hovering for more than a few seconds gets a
 * player kicked for flying, as with other jetpacks; set {@code allow-flight=true} to use it there.
 */
public class RocketPackItem extends Item implements Chargeable, Jetpack {
	public static final long CAPACITY = 200_000;
	public static final long ENERGY_PER_TICK = 50;
	/** Upward speed added per tick of thrust (gravity takes 0.08), and the fastest climb (blocks per tick). */
	public static final double THRUST = 0.14;
	public static final double MAX_CLIMB = 0.45;

	public RocketPackItem(Properties properties) {
		super(properties);
	}

	@Override
	public long baseCapacity() {
		return CAPACITY;
	}

	/** Whether this player wears a jetpack (a rocket pack or exosuit chestplate) with enough charge for a tick of thrust. */
	public static boolean canThrust(Player player) {
		ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
		return wornJetpack(chest) && Chargeable.energy(chest) >= ENERGY_PER_TICK;
	}

	private static boolean wornJetpack(ItemStack chest) {
		return chest.getItem() instanceof Jetpack jetpack && jetpack.isJetpack(chest) && chest.getItem() instanceof Chargeable;
	}

	/** Server side of one tick of thrust: pays for it and cancels the fall. At most once per tick per player. */
	public static void thrust(ServerPlayer player) {
		long now = player.level().getGameTime();
		Long last = LAST_THRUST.put(player.getUUID(), now);
		if (last != null && last == now) {
			return;
		}
		ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
		if (wornJetpack(chest) && Chargeable.drain(chest, ENERGY_PER_TICK)) {
			player.resetFallDistance();
		}
	}

	/** Game time of each player's last paid thrust, so a client cannot fire more than once a tick. */
	private static final Map<UUID, Long> LAST_THRUST = new HashMap<>();

	@Override
	public boolean isBarVisible(ItemStack stack) {
		return true;
	}

	@Override
	public int getBarWidth(ItemStack stack) {
		return PoweredToolItem.barWidth(stack);
	}

	@Override
	public int getBarColor(ItemStack stack) {
		return PoweredToolItem.BAR_COLOR;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
			TooltipFlag flag) {
		tooltip.accept(PoweredToolItem.energyLine(stack));
		ToolUpgrades.appendTooltip(stack, tooltip);
		tooltip.accept(Component.translatable("tooltip.jugcraft.rocket_pack").withStyle(ChatFormatting.GRAY));
	}
}
