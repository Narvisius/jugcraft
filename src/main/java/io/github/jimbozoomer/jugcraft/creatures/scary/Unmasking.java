package io.github.jimbozoomer.jugcraft.creatures.scary;

import io.github.jimbozoomer.jugcraft.agriculture.TrickOrTreat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.gamerules.GameRules;
import org.jspecify.annotations.Nullable;

/**
 * Unmasking (docs/features/costumed-villains-unmasking.md): when a player beats a costumed villain, the costume comes
 * off. One of {@value #CROOKS} crooks for that villain stands caught where it fell ({@link UnmaskedCrook}), everyone
 * near is told who it was and hears their excuse, the costume's mask drops as a trophy (when mobs drop loot) and the
 * player earns "Mystery Solved". A villain that dies any other way keeps its secret.
 */
public final class Unmasking {
	/** How many crooks each villain may turn out to be (their names and excuses are in the lang file). */
	public static final int CROOKS = 3;
	public static final String ADVANCEMENT = "mystery_solved";
	/** Each villain's key (its entity id) and its mask, in the order the masks are listed. */
	private static final Map<String, Item> MASKS = new LinkedHashMap<>();
	private static final Map<EntityType<?>, String> VILLAINS = new LinkedHashMap<>();

	private Unmasking() {
	}

	public static void register() {
		villain(ScaryMod.SPACE_KOOK, "space_kook", "spectral_cosmonaut_helmet");
		villain(ScaryMod.CAPTAIN_CUTLER, "captain_cutler", "drowned_diver_helmet");
		villain(ScaryMod.PHANTOM_SHADOW, "phantom_shadow", "emerald_phantom_mask");
		villain(ScaryMod.BLACK_KNIGHT, "black_knight", "onyx_warden_helm");
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity.level() instanceof ServerLevel level && source.getEntity() instanceof ServerPlayer player
					&& VILLAINS.containsKey(entity.getType())) {
				unmask(level, entity, player);
			}
		});
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(output -> MASKS.values().forEach(output::accept));
	}

	private static void villain(EntityType<?> type, String key, String mask) {
		ResourceKey<Item> id = ResourceKey.create(Registries.ITEM, ScaryMod.id(mask));
		MASKS.put(key, Registry.register(BuiltInRegistries.ITEM, id,
				new Item(new Item.Properties().setId(id).stacksTo(1).rarity(Rarity.UNCOMMON))));
		VILLAINS.put(type, key);
	}

	/** The mask a villain's costume drops, by the villain's key (its entity id); null if it has none. */
	public static @Nullable Item mask(String key) {
		return MASKS.get(key);
	}

	/**
	 * Unmasks {@code villain}, which {@code player} has just beaten: the crook stands where it fell, everyone near is
	 * told, the mask drops and the player earns the advancement. Returns the crook (null if it could not be made).
	 */
	public static @Nullable UnmaskedCrook unmask(ServerLevel level, LivingEntity villain, ServerPlayer player) {
		String key = VILLAINS.get(villain.getType());
		if (key == null) {
			return null;
		}
		int which = villain.getRandom().nextInt(CROOKS);
		Component name = Component.translatable("entity.jugcraft.unmasked_crook." + key + "." + which);
		UnmaskedCrook crook = ScaryMod.UNMASKED_CROOK.create(level, EntitySpawnReason.EVENT);
		if (crook != null) {
			crook.snapTo(villain.getX(), villain.getY(), villain.getZ(), villain.getYRot(), 0.0F);
			crook.setCustomName(name);
			crook.setCustomNameVisible(true);
			crook.setSkin("townsfolk_" + (1 + villain.getRandom().nextInt(5)));
			level.addFreshEntity(crook);
		}
		Component reveal = Component.translatable("message.jugcraft.unmasked.reveal", villain.getType().getDescription(), name)
				.withStyle(ChatFormatting.GOLD);
		Component excuse = Component.translatable("message.jugcraft.unmasked.says", name,
				Component.translatable("message.jugcraft.unmasked." + key + "." + which));
		for (ServerPlayer near : level.getPlayers(p -> p.distanceToSqr(villain) < UnmaskedCrook.HEARING * UnmaskedCrook.HEARING)) {
			near.sendSystemMessage(reveal);
			near.sendSystemMessage(excuse);
		}
		level.playSound(null, villain.getX(), villain.getY(), villain.getZ(), SoundEvents.VILLAGER_NO, SoundSource.NEUTRAL, 1.0F, 1.0F);
		if (level.getGameRules().get(GameRules.MOB_DROPS)) {
			ItemStack mask = new ItemStack(MASKS.get(key));
			mask.set(DataComponents.LORE, new ItemLore(List.of(Component.translatable("tooltip.jugcraft.unmasked_mask", name))));
			level.addFreshEntity(new ItemEntity(level, villain.getX(), villain.getY() + 0.5, villain.getZ(), mask));
		}
		TrickOrTreat.award(player, ADVANCEMENT);
		return crook;
	}
}
