package io.github.jimbozoomer.jugcraft.weapons;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/**
 * Explosive weapons (batch 18): the grenade in flight. The items (guncotton, grenade, grenade launcher) are in
 * PetroItems. Grenades hurt living things only and never break blocks ({@link Blast}).
 */
public final class JugcraftWeapons {
	public static EntityType<GrenadeEntity> GRENADE;

	private JugcraftWeapons() {
	}

	public static void register() {
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, Jugcraft.id("grenade"));
		GRENADE = Registry.register(BuiltInRegistries.ENTITY_TYPE, key,
				EntityType.Builder.<GrenadeEntity>of(GrenadeEntity::new, MobCategory.MISC)
						.sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(10).build(key));
	}
}
