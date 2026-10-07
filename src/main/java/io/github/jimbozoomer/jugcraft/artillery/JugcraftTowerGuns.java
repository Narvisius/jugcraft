package io.github.jimbozoomer.jugcraft.artillery;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.phys.Vec3;

/**
 * Batch 54: the tower guns (docs/features/tower-guns.md), heavy emplacements for a 3x3 or 5x5 tower top, and the Great
 * Shell. Keep the numbers in sync with tools/tower_guns.py; tools/check_mod_data.py checks each {@code spec(...)} line.
 */
public final class JugcraftTowerGuns {
	public static final double GREAT_SPEED = 3.2;
	public static final double GREAT_GRAVITY = 0.05;
	public static final double GREAT_RADIUS = 7.0;
	public static final float GREAT_DAMAGE = 48F;

	/**
	 * One kind of tower gun. footprint: blocks a side. shell: "heavy", "flak" or "great". speed: the shell's muzzle speed.
	 * pivotHeight, pivotForward, barrelLength: where the barrel turns and how long it is, in blocks. barrels: each
	 * barrel's offset across the gun, in blocks. seats: where the crew stand, in blocks from the gun's feet (the gunner
	 * first).
	 */
	public record Spec(String id, int footprint, String shell, double speed, int cooldown, float traverse, float minPitch,
			float maxPitch, boolean highArc, boolean automatic, double pivotHeight, double pivotForward, double barrelLength,
			int health, float height, double[] barrels, Vec3[] seats) {
		public EntityType<ArtilleryShell> shellType() {
			return switch (shell) {
				case "flak" -> JugcraftArtillery.FLAK_SHELL;
				case "great" -> GREAT_SHELL;
				default -> JugcraftArtillery.HEAVY_SHELL;
			};
		}

		public Item ammo() {
			return switch (shell) {
				case "flak" -> JugcraftArtillery.FLAK_SHELL_ITEM;
				case "great" -> GREAT_SHELL_ITEM;
				default -> JugcraftArtillery.HEAVY_SHELL_ITEM;
			};
		}

		public double gravity() {
			return switch (shell) {
				case "flak" -> JugcraftArtillery.FLAK_GRAVITY;
				case "great" -> GREAT_GRAVITY;
				default -> JugcraftArtillery.HEAVY_GRAVITY;
			};
		}

		public Item item() {
			return ITEMS.get(id);
		}
	}

	public static final List<Spec> SPECS = List.of(
			spec("bastion_mortar", 3, "heavy", 3.0, 70, 3.0F, 45.0F, 85.0F, true, false, 1.6875, 0.125, 2.75, 160, 2.4F, new double[] {0.0}, new Vec3(0.0, 0.75, -0.9375), new Vec3(0.6875, 0.75, -0.5625)),
			spec("bastion_autocannon", 3, "flak", 4.0, 6, 10.0F, -10.0F, 85.0F, false, true, 1.5625, 0.375, 3.125, 120, 2.2F, new double[] {-0.3125, 0.3125}, new Vec3(0.0, 0.75, -1.0)),
			spec("grand_mortar", 5, "great", 3.2, 200, 1.0F, 45.0F, 85.0F, true, false, 3.125, 0.25, 5.375, 320, 4.0F, new double[] {0.0}, new Vec3(0.0, 0.9375, -1.625), new Vec3(1.25, 0.9375, -1.0), new Vec3(-1.25, 0.9375, -1.0)),
			spec("fortress_rifle", 5, "heavy", 4.5, 120, 1.5F, -5.0F, 45.0F, false, false, 2.125, 0.875, 6.5, 300, 3.0F, new double[] {0.0}, new Vec3(0.0, 0.9375, -1.875), new Vec3(1.125, 0.9375, -1.625)),
			spec("triple_battery", 5, "heavy", 3.0, 140, 1.5F, -5.0F, 60.0F, false, false, 2.0, 0.875, 4.875, 300, 2.8F, new double[] {-0.6875, 0.0, 0.6875}, new Vec3(0.0, 0.9375, -1.875), new Vec3(1.125, 0.9375, -1.625)));

	/** Each gun's entity type and item, by id, in the order above. */
	public static final Map<String, EntityType<TowerGun>> TYPES = new LinkedHashMap<>();
	public static final Map<String, Item> ITEMS = new LinkedHashMap<>();
	private static final Map<EntityType<?>, Spec> BY_TYPE = new HashMap<>();
	public static EntityType<ArtilleryShell> GREAT_SHELL;
	public static Item GREAT_SHELL_ITEM;

	private JugcraftTowerGuns() {
	}

	private static Spec spec(String id, int footprint, String shell, double speed, int cooldown, float traverse, float minPitch,
			float maxPitch, boolean highArc, boolean automatic, double pivotHeight, double pivotForward, double barrelLength,
			int health, float height, double[] barrels, Vec3... seats) {
		return new Spec(id, footprint, shell, speed, cooldown, traverse, minPitch, maxPitch, highArc, automatic, pivotHeight,
				pivotForward, barrelLength, health, height, barrels, seats);
	}

	public static Spec specOf(EntityType<?> type) {
		Spec spec = BY_TYPE.get(type);
		if (spec == null) {
			throw new IllegalStateException("Not a tower gun: " + type);
		}
		return spec;
	}

	public static EntityType<TowerGun> type(String id) {
		return TYPES.get(id);
	}

	public static Item item(String id) {
		return ITEMS.get(id);
	}

	/** Registers the guns, their items and the Great Shell. Call after {@link JugcraftArtillery#register()}. */
	public static void register() {
		GREAT_SHELL = entity("great_shell", EntityType.Builder.<ArtilleryShell>of(ArtilleryShell::new, MobCategory.MISC)
				.sized(0.5F, 0.5F).clientTrackingRange(16).updateInterval(1));
		ResourceKey<Item> shellKey = ResourceKey.create(Registries.ITEM, Jugcraft.id("great_shell"));
		GREAT_SHELL_ITEM = Registry.register(BuiltInRegistries.ITEM, shellKey, new Item(new Item.Properties().setId(shellKey).stacksTo(16)) {
			@Override
			public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
					Consumer<Component> tooltip, TooltipFlag flag) {
				tooltip.accept(Component.translatable("tooltip.jugcraft.great_shell").withStyle(ChatFormatting.GRAY));
			}
		});
		for (Spec spec : SPECS) {
			// A hair under the footprint, so a gun fits on a tower top exactly that wide.
			EntityType<TowerGun> type = entity(spec.id(), EntityType.Builder.<TowerGun>of(TowerGun::new, MobCategory.MISC)
					.sized(spec.footprint() - 0.1F, spec.height()).noLootTable().clientTrackingRange(12).updateInterval(2));
			TYPES.put(spec.id(), type);
			BY_TYPE.put(type, spec);
			ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Jugcraft.id(spec.id()));
			ITEMS.put(spec.id(), Registry.register(BuiltInRegistries.ITEM, key,
					new PlaceEntityItem(new Item.Properties().setId(key).stacksTo(1), () -> type, "tooltip.jugcraft." + spec.id())));
		}
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(output -> {
			for (Item item : ITEMS.values()) {
				output.accept(item);
			}
			output.accept(GREAT_SHELL_ITEM);
		});
	}

	private static <T extends net.minecraft.world.entity.Entity> EntityType<T> entity(String id, EntityType.Builder<T> builder) {
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, Jugcraft.id(id));
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
	}
}
