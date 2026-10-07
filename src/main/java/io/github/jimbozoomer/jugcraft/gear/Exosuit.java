package io.github.jimbozoomer.jugcraft.gear;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.tools.Chargeable;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.ArmorType;

/**
 * The exosuit's powers (batch 28, docs/features/exosuit.md), run on the server for every player each tick. Each piece
 * works on its own charge, and only while it is worn and charged. Keep the numbers in sync with tools/exosuit.py;
 * tools/check_mod_data.py checks them.
 * <ul>
 * <li>Helmet: night vision while it is dark, for {@link #NIGHT_VISION_PER_TICK} JE a tick.</li>
 * <li>Chestplate: an energy shield of up to {@link #SHIELD_POINTS} absorption points, one regrown every
 * {@link #SHIELD_INTERVAL} ticks for {@link #SHIELD_PER_POINT} JE. It is also a jetpack ({@code RocketPackItem}).</li>
 * <li>Leggings: {@link #SPEED_BONUS} more walking speed, for {@link #SPEED_PER_TICK} JE a tick.</li>
 * <li>Boots: no fall damage and a full-block step, for {@link #BOOTS_PER_TICK} JE a tick.</li>
 * </ul>
 * Attribute bonuses are transient modifiers added while a piece works and removed as soon as it does not, so nothing
 * is left behind when a piece runs flat or comes off.
 */
public final class Exosuit {
	public static final long CAPACITY = 400_000;
	public static final long NIGHT_VISION_PER_TICK = 2;
	public static final int SHIELD_POINTS = 8;
	public static final int SHIELD_INTERVAL = 10;
	public static final long SHIELD_PER_POINT = 4_000;
	public static final long SPEED_PER_TICK = 1;
	public static final double SPEED_BONUS = 0.3;
	public static final long BOOTS_PER_TICK = 1;
	public static final double STEP_BONUS = 0.5;
	/** The night vision lasts this long and is topped up when it falls below NIGHT_VISION_REFRESH (no flicker). */
	public static final int NIGHT_VISION_DURATION = 260;
	public static final int NIGHT_VISION_REFRESH = 220;
	/** Darker than this (raw light 0-15) and the helmet's night vision comes on. */
	public static final int DARK_BELOW = 8;

	public static final Identifier SHIELD = Jugcraft.id("exosuit_shield");
	public static final Identifier SPEED = Jugcraft.id("exosuit_speed");
	public static final Identifier STEP = Jugcraft.id("exosuit_step");
	public static final Identifier FALL = Jugcraft.id("exosuit_fall");

	private Exosuit() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> server.getPlayerList().getPlayers().forEach(Exosuit::tick));
	}

	/** One tick of the suit's powers for this player. */
	public static void tick(ServerPlayer player) {
		helmet(player, player.getItemBySlot(EquipmentSlot.HEAD));
		chestplate(player, player.getItemBySlot(EquipmentSlot.CHEST));
		ItemStack legs = player.getItemBySlot(EquipmentSlot.LEGS);
		boolean fast = worn(legs, ArmorType.LEGGINGS) && Chargeable.drain(legs, SPEED_PER_TICK);
		modifier(player, Attributes.MOVEMENT_SPEED, SPEED, fast ? SPEED_BONUS : 0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
		ItemStack feet = player.getItemBySlot(EquipmentSlot.FEET);
		boolean boots = worn(feet, ArmorType.BOOTS) && Chargeable.drain(feet, BOOTS_PER_TICK);
		modifier(player, Attributes.STEP_HEIGHT, STEP, boots ? STEP_BONUS : 0, AttributeModifier.Operation.ADD_VALUE);
		modifier(player, Attributes.FALL_DAMAGE_MULTIPLIER, FALL, boots ? -1 : 0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
	}

	private static void helmet(ServerPlayer player, ItemStack head) {
		BlockPos eyes = BlockPos.containing(player.getEyePosition());
		boolean dark = player.level().getMaxLocalRawBrightness(eyes) < DARK_BELOW;
		MobEffectInstance current = player.getEffect(MobEffects.NIGHT_VISION);
		if (worn(head, ArmorType.HELMET) && dark && Chargeable.drain(head, NIGHT_VISION_PER_TICK)) {
			if (current == null || current.getDuration() < NIGHT_VISION_REFRESH) {
				// Ambient and without particles: the suit's own night vision, which it may take away again.
				player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, NIGHT_VISION_DURATION, 0, true, false, true));
			}
		} else if (current != null && current.isAmbient() && !current.isVisible()
				&& current.getDuration() <= NIGHT_VISION_DURATION) {
			player.removeEffect(MobEffects.NIGHT_VISION);
		}
	}

	private static void chestplate(ServerPlayer player, ItemStack chest) {
		boolean shield = worn(chest, ArmorType.CHESTPLATE) && Chargeable.energy(chest) >= SHIELD_PER_POINT;
		// The shield's capacity first: absorption cannot be set above the maximum.
		modifier(player, Attributes.MAX_ABSORPTION, SHIELD, shield ? SHIELD_POINTS : 0, AttributeModifier.Operation.ADD_VALUE);
		if (shield && player.level().getGameTime() % SHIELD_INTERVAL == 0 && player.getAbsorptionAmount() <= SHIELD_POINTS - 1
				&& Chargeable.drain(chest, SHIELD_PER_POINT)) {
			player.setAbsorptionAmount(player.getAbsorptionAmount() + 1);
		}
	}

	/** Whether this stack is an exosuit piece of this type (and so in its own slot). */
	public static boolean worn(ItemStack stack, ArmorType type) {
		return stack.getItem() instanceof ExosuitItem exosuit && exosuit.type() == type;
	}

	/** Adds the modifier when amount is not zero, and removes it when it is. */
	private static void modifier(ServerPlayer player, Holder<Attribute> attribute, Identifier id, double amount,
			AttributeModifier.Operation operation) {
		AttributeInstance instance = player.getAttribute(attribute);
		if (instance == null) {
			return;
		}
		if (amount == 0) {
			instance.removeModifier(id);
		} else if (!instance.hasModifier(id)) {
			instance.addTransientModifier(new AttributeModifier(id, amount, operation));
		}
	}
}
