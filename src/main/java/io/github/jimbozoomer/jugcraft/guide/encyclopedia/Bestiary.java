package io.github.jimbozoomer.jugcraft.guide.encyclopedia;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.ServerStatsCounter;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * The bestiary (docs/features/bestiary-field-notes.md), written after the lair dossiers on the Expeditions page: every
 * Jugcraft creature this player has defeated or been defeated by, most defeated first, with the health, attack and
 * armour the game itself gives it and the player's own tally. A creature the player has not fought is left out, so the
 * page spoils nothing and invents nothing. It reads only the player's statistics and the creatures' default attributes.
 */
public final class Bestiary {
	/** How many creatures the page lists at most; the rest are counted in one line. */
	public static final int MAX_CREATURES = 24;

	private Bestiary() {
	}

	/** One creature this player has fought. */
	public record Entry(EntityType<?> type, int defeated, int defeatedBy) {
	}

	public static void write(ServerPlayer player, PageWriter out) {
		List<Entry> fought = fought(player);
		if (fought.isEmpty()) {
			return;
		}
		out.heading(Component.translatable("encyclopedia.jugcraft.bestiary.heading"));
		out.fact(Component.translatable("encyclopedia.jugcraft.bestiary.intro"));
		for (Entry entry : fought.subList(0, Math.min(MAX_CREATURES, fought.size()))) {
			out.heading(entry.type().getDescription());
			Item egg = BuiltInRegistries.ITEM.getValue(Jugcraft.id(BuiltInRegistries.ENTITY_TYPE.getKey(entry.type()).getPath() + "_spawn_egg"));
			List<Item> icons = egg == Items.AIR ? List.of() : List.of(egg);
			Component stats = stats(entry.type());
			if (stats != null) {
				out.fact(stats, null, icons);
			}
			out.fact(Component.translatable("encyclopedia.jugcraft.bestiary.record", entry.defeated(), entry.defeatedBy()),
					null, stats == null ? icons : List.of());
		}
		if (fought.size() > MAX_CREATURES) {
			out.fact(Component.translatable("encyclopedia.jugcraft.bestiary.more", fought.size() - MAX_CREATURES));
		}
	}

	/** Jugcraft's living creatures this player has defeated or been defeated by, most defeated first, then by id. */
	public static List<Entry> fought(ServerPlayer player) {
		ServerStatsCounter stats = player.getStats();
		List<Entry> out = new ArrayList<>();
		for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
			if (!BuiltInRegistries.ENTITY_TYPE.getKey(type).getNamespace().equals(Jugcraft.MOD_ID)
					|| type.getCategory() == MobCategory.MISC || !DefaultAttributes.hasSupplier(type)) {
				continue;
			}
			int defeated = stats.getValue(Stats.ENTITY_KILLED.get(type));
			int defeatedBy = stats.getValue(Stats.ENTITY_KILLED_BY.get(type));
			if (defeated > 0 || defeatedBy > 0) {
				out.add(new Entry(type, defeated, defeatedBy));
			}
		}
		out.sort(Comparator.comparingInt(Entry::defeated).reversed()
				.thenComparing(entry -> BuiltInRegistries.ENTITY_TYPE.getKey(entry.type()).toString()));
		return out;
	}

	/** The creature's default health, attack and armour, or null if it has none to show. */
	@SuppressWarnings("unchecked")
	private static Component stats(EntityType<?> type) {
		AttributeSupplier attributes = DefaultAttributes.getSupplier((EntityType<? extends LivingEntity>) type);
		String health = number(attributes, Attributes.MAX_HEALTH);
		String armour = number(attributes, Attributes.ARMOR);
		if (health == null) {
			return null;
		}
		String attack = number(attributes, Attributes.ATTACK_DAMAGE);
		return attack == null ? Component.translatable("encyclopedia.jugcraft.bestiary.stats.no_attack", health, armour == null ? "0" : armour)
				: Component.translatable("encyclopedia.jugcraft.bestiary.stats", health, attack, armour == null ? "0" : armour);
	}

	private static String number(AttributeSupplier attributes, Holder<Attribute> attribute) {
		if (!attributes.hasAttribute(attribute)) {
			return null;
		}
		double value = attributes.getValue(attribute);
		return value == Math.rint(value) ? Long.toString((long) value) : String.format(java.util.Locale.ROOT, "%.1f", value);
	}
}
