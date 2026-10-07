package io.github.jimbozoomer.jugcraft.gear;

import com.mojang.serialization.Codec;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

/**
 * The pneumatic grapple (batch 30, docs/features/pneumatic-grapple.md): a harpoon gun that runs on compressed nitrogen
 * from the air separation unit. Keep the numbers in sync with tools/grapple.py; tools/check_mod_data.py checks them.
 */
public final class JugcraftGrapple {
	/** Nitrogen a grapple holds, in mB, and what one shot uses. */
	public static final int CAPACITY = 4_000;
	public static final int SHOT_COST = 25;
	/** How far the hook flies before the line runs out, in blocks, and how fast it leaves the gun. */
	public static final int RANGE = 32;
	public static final float LAUNCH_SPEED = 2.5F;
	/** Ticks between shots. */
	public static final int COOLDOWN = 10;

	/** Nitrogen in a grapple, in mB. */
	public static DataComponentType<Integer> NITROGEN;
	public static Item PNEUMATIC_GRAPPLE;
	public static EntityType<GrappleHook> GRAPPLE_HOOK;

	private JugcraftGrapple() {
	}

	public static void register() {
		NITROGEN = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Jugcraft.id("nitrogen"),
				DataComponentType.<Integer>builder().persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT).build());
		PNEUMATIC_GRAPPLE = JugcraftGear.item("pneumatic_grapple", properties -> new PneumaticGrappleItem(properties.stacksTo(1)
				.rarity(Rarity.UNCOMMON).component(NITROGEN, 0)));
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, Jugcraft.id("grapple_hook"));
		GRAPPLE_HOOK = Registry.register(BuiltInRegistries.ENTITY_TYPE, key,
				EntityType.Builder.<GrappleHook>of(GrappleHook::new, MobCategory.MISC).noSummon()
						.sized(0.25F, 0.25F).clientTrackingRange(8).updateInterval(1).build(key));
	}
}
