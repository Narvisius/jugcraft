package io.github.jimbozoomer.jugcraft.client.blueprint;

import io.github.jimbozoomer.jugcraft.blueprint.BlueprintItem;
import io.github.jimbozoomer.jugcraft.blueprint.BlueprintNetwork;
import io.github.jimbozoomer.jugcraft.blueprint.BlueprintStacks;
import io.github.jimbozoomer.jugcraft.blueprint.JugcraftBlueprints;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.client.player.ClientHotbarScrollEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * Shift+scroll with a stacked building's blueprint in hand (the pre-made warehouses, BlueprintStacks) picks how many
 * floors it is staked with, from its least to as many as fit under the build limit where you look; the placement
 * preview shows it at once. The server sets the held blueprint to that many floors (BlueprintNetwork.FloorsPayload).
 */
public final class BlueprintFloors {
	private BlueprintFloors() {
	}

	public static void register() {
		ClientHotbarScrollEvents.ALLOW.register((inventory, current, next, dx, dy) -> {
			Minecraft minecraft = Minecraft.getInstance();
			if (minecraft.player == null || !minecraft.hasShiftDown() || dy == 0) {
				return true;
			}
			ItemStack held = minecraft.player.getMainHandItem();
			String id = held.is(JugcraftBlueprints.BLUEPRINT) ? BlueprintItem.idOf(held) : "";
			BlueprintStacks.Stack stack = BlueprintStacks.of(id);
			if (stack == null || BlueprintStacks.isAdd(id)) {
				return true;
			}
			BlueprintItem.Target target = BlueprintItem.target(minecraft.player, held, 1.0F);
			int stakeY = target != null ? target.stake().getY() : minecraft.player.blockPosition().getY();
			int max = Math.min(BlueprintStacks.MAX, Math.max(stack.min(), stack.maxFloors(stakeY, minecraft.player.level().getMaxY())));
			int floors = Math.max(stack.min(), Math.min(max, BlueprintStacks.floors(id) + (dy > 0 ? 1 : -1)));
			if (floors != BlueprintStacks.floors(id) || !id.contains("@")) {
				// Shown at once here; the server sets the held blueprint the same (and syncs it back).
				BlueprintItem.setFloors(held, floors);
				ClientPlayNetworking.send(new BlueprintNetwork.FloorsPayload(floors));
			}
			minecraft.player.sendOverlayMessage(Component.translatable("message.jugcraft.blueprint.floors",
					Component.translatable("blueprint.jugcraft." + stack.kind()), floors, stack.min(), max));
			return false;
		});
	}
}
