package io.github.jimbozoomer.jugcraft.client;

import com.mojang.serialization.MapCodec;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.tower.crew.PlasmaBolt;
import io.github.jimbozoomer.jugcraft.tower.crew.PlasmaRifle;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperties;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * The item model property "jugcraft:plasma_charge": how much of the NT-7's cooldown is left, 1 the instant it fires, 0
 * when it's ready (P6: "the purple barrel remains dark until it's producing plasma for the shot ... once you shoot, it
 * suddenly glows purple and becomes darker until the next shot is ready"). Its item definition picks the accelerator's
 * glow and the cell's bars by it (tools/plasma_rifle_nt7.py STATES). A player's from their cooldown; anyone else who
 * fires bolts (the tower's security, drawing the rifle in their hands: TowerCrewRenderer) from when this client last
 * saw a bolt of theirs appear.
 */
public record PlasmaCharge() implements RangeSelectItemModelProperty {
	public static final MapCodec<PlasmaCharge> MAP_CODEC = MapCodec.unit(new PlasmaCharge());
	/** When this client last saw each shooter's bolt appear (entity id to game time). */
	private static final Int2LongOpenHashMap LAST = new Int2LongOpenHashMap();

	public static void register() {
		RangeSelectItemModelProperties.ID_MAPPER.put(Jugcraft.id("plasma_charge"), MAP_CODEC);
		ClientEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (entity instanceof PlasmaBolt bolt && bolt.getOwner() != null) {
				shot(bolt.getOwner(), level.getGameTime());
			}
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> LAST.clear());
	}

	/** Records a shot by this shooter at this game time (a bolt of theirs appearing). */
	public static void shot(Entity shooter, long time) {
		if (LAST.size() > 256) {
			LAST.clear();
		}
		LAST.put(shooter.getId(), time);
	}

	/** How much of the cooldown is left for this holder: 1 at the shot, 0 ready. */
	public static float left(ItemStack stack, @Nullable LivingEntity holder, @Nullable ClientLevel level) {
		if (holder instanceof Player player) {
			return player.getCooldowns().getCooldownPercent(stack, partialTick());
		}
		if (holder == null || level == null || !LAST.containsKey(holder.getId())) {
			return 0.0F;
		}
		float since = level.getGameTime() - LAST.get(holder.getId()) + partialTick();
		return Mth.clamp(1.0F - since / PlasmaRifle.COOLDOWN, 0.0F, 1.0F);
	}

	private static float partialTick() {
		return Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
	}

	@Override
	public float get(ItemStack stack, ClientLevel level, ItemOwner owner, int seed) {
		return left(stack, owner == null ? null : owner.asLivingEntity(), level);
	}

	@Override
	public MapCodec<PlasmaCharge> type() {
		return MAP_CODEC;
	}
}
