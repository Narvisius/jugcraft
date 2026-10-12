package io.github.jimbozoomer.jugcraft.concordance.trinket;

import eu.pb4.trinkets.api.SlotAttributes;
import eu.pb4.trinkets.api.TrinketSlotAccess;
import eu.pb4.trinkets.api.TrinketsApi;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceEffects;
import io.github.jimbozoomer.jugcraft.concordance.dreaming.Dreaming;
import io.github.jimbozoomer.jugcraft.concordance.effect.Cause;
import io.github.jimbozoomer.jugcraft.concordance.effect.EffectKind;
import io.github.jimbozoomer.jugcraft.concordance.effect.EffectSpec;
import io.github.jimbozoomer.jugcraft.concordance.effect.Intent;
import io.github.jimbozoomer.jugcraft.concordance.effect.Ledger;
import io.github.jimbozoomer.jugcraft.concordance.effect.Stacking;
import io.github.jimbozoomer.jugcraft.concordance.reliquary.Reliquary;
import io.github.jimbozoomer.jugcraft.concordance.sign.Sign;
import io.github.jimbozoomer.jugcraft.concordance.sign.Signs;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.Event;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Wayfaring, the trinkets slice's part 1 (docs/features/arcane-concordance-trinkets.md, tools/concordance_trinkets.py):
 * the owner's belt, boots and charms, worn in Trinkets slots once Relic Lore is understood ({@link WornTrinketItem}).
 * <ul>
 * <li>The Leather Belt adds a Charm slot (a Trinkets slot-count modifier on {@link #CHARM_SLOT}).</li>
 * <li>The Angelic Feather, Kraken Shell and Infernal Claws take falling's, drowning's and fire's harm from the wearer's
 * food instead of their health, while they have food ({@link #allowDamage}).</li>
 * <li>The Angelheart Vial and the Phoenix Down answer a blow that would kill ({@link #save}), before a dream ends for it
 * and before a held totem's turn.</li>
 * <li>The Amphibian Boot and the Ice Breaker are worn on the feet; a fall that hurts an Ice Breaker's wearer sends a wave
 * through the ground ({@link #wave}) at hostile creatures only, through the shared effect boundary (which lets anyone
 * fight them; its tolerance tags still keep a few great foes from being thrown or slowed).</li>
 * <li>Whether the belt and boots are drawn on a wearer, for everyone who sees them, is that player's own choice
 * ({@link WornDisplay}); armour over them hides them too (client: trinket/UnlessCoveredTrinketElement).</li>
 * </ul>
 * Nothing here ticks: the attributes are Trinkets modifiers, and the rest answers three damage events. With the
 * Concordance switched off the items, slots and modifiers stay, but nothing answers the events.
 */
public final class Wayfaring {
	/** The Trinkets slots (data/trinkets/slots): Trinkets' own belt slot, and the two Jugcraft adds with the owner's icons. */
	public static final String BELT_SLOT = "legs/belt";
	public static final String CHARM_SLOT = "legs/charm";
	public static final String FEET_SLOT = "feet/boots";
	/** Charm slots without a belt, and how many a worn Leather Belt adds. */
	public static final int CHARM_SLOTS = 1;
	public static final int BELT_CHARM_SLOTS = 1;
	/** Exhaustion each point of harm a charm takes from food costs (vanilla: 4 is one point of food). */
	public static final float ABSORB_EXHAUSTION = 2.0F;
	/**
	 * Ticks after a charm takes a blow in which a further blow costs only what it is bigger by: vanilla's hurt cooldown,
	 * which a blow the charm takes never starts (fire and hot floors try to hurt every tick).
	 */
	public static final int ABSORB_COOLDOWN_TICKS = 10;
	/** Attribute modifiers while worn (added values). */
	public static final double FEATHER_JUMP = 0.03;
	public static final double AMPHIBIAN_SWIM = 0.5;
	public static final double AMPHIBIAN_OXYGEN = 1.0;
	public static final double ICE_BREAKER_KNOCKBACK = 0.1;
	/** The Ice Breaker's wave: radius (blocks) for the fall's harm, up to a ceiling; how many it reaches; what it does. */
	public static final double WAVE_RADIUS = 3.0;
	public static final double WAVE_RADIUS_PER_HARM = 0.25;
	public static final double WAVE_MAX_RADIUS = 6.0;
	public static final int WAVE_TARGETS = 12;
	public static final int WAVE_DAMAGE = 2;
	public static final int WAVE_PUSH = 6;
	public static final int WAVE_SLOW_TICKS = 40;
	/** The death saves: the vial's health and Regeneration II; the Phoenix Down's Regeneration II and Fire Resistance. */
	public static final float VIAL_HEALTH = 4.0F;
	public static final int VIAL_REGENERATION_TICKS = 100;
	public static final int PHOENIX_REGENERATION_TICKS = 200;
	public static final int PHOENIX_FIRE_RESISTANCE_TICKS = 200;
	/**
	 * The phase the death saves run in, ordered before the default phase: Dreaming's own listener (default phase) ends an
	 * open dream as a death, so only before it can a save see the dream and decline.
	 */
	public static final Identifier SAVE_PHASE = Jugcraft.id("wayfaring_saves");
	/** The attribute a Leather Belt's Charm slot is counted in (Trinkets registers it). */
	public static final String SLOT_COUNT = "trinkets:slot_count/" + CHARM_SLOT;
	/** What harm each charm takes from food instead of health: damage type ids (tools/concordance_trinkets.py ABSORBS). */
	public static final Map<String, List<String>> ABSORBS = Map.of(
			"angelic_feather", List.of("minecraft:fall"),
			"phoenix_down", List.of("minecraft:fall"),
			"kraken_shell", List.of("minecraft:drown"),
			"infernal_claws", List.of("minecraft:in_fire", "minecraft:on_fire", "minecraft:campfire", "minecraft:hot_floor"));

	static final EffectSpec VIAL_REGENERATION = new EffectSpec(EffectKind.STATUS, Intent.HELPFUL, 1, VIAL_REGENERATION_TICKS,
			"minecraft:regeneration", Stacking.STRONGEST, null);
	static final List<EffectSpec> PHOENIX_STATUSES = List.of(
			new EffectSpec(EffectKind.STATUS, Intent.HELPFUL, 1, PHOENIX_REGENERATION_TICKS, "minecraft:regeneration", Stacking.STRONGEST, null),
			new EffectSpec(EffectKind.STATUS, Intent.HELPFUL, 0, PHOENIX_FIRE_RESISTANCE_TICKS, "minecraft:fire_resistance", Stacking.STRONGEST, null));
	/** The wave, in order: harm, a throw away from where the wearer landed, Slowness I. */
	static final List<EffectSpec> WAVE = List.of(
			EffectSpec.of(EffectKind.DAMAGE, Intent.HARMFUL, WAVE_DAMAGE, 0),
			EffectSpec.of(EffectKind.MOVEMENT, Intent.HARMFUL, WAVE_PUSH, 0),
			new EffectSpec(EffectKind.STATUS, Intent.HARMFUL, 0, WAVE_SLOW_TICKS, "minecraft:slowness", Stacking.STRONGEST, null));
	private static final Map<String, List<ResourceKey<DamageType>>> ABSORBED = new LinkedHashMap<>();
	/** Vanilla's tag of harm its hurt cooldown lets through (named by id: the 26.3 constant is not used elsewhere here). */
	private static final TagKey<DamageType> BYPASSES_COOLDOWN = TagKey.create(Registries.DAMAGE_TYPE,
			Identifier.withDefaultNamespace("bypasses_cooldown"));
	/** The last blow each player's charm took, for the cooldown (server thread only; forgotten with the player). */
	private static final Map<ServerPlayer, Taken> TAKEN = new WeakHashMap<>();

	private record Taken(long tick, float amount) {
	}

	/** The Charm slot count attribute (trinkets:slot_count/legs/charm). */
	public static Holder<Attribute> CHARM_SLOT_COUNT;
	public static Item LEATHER_BELT;
	public static Item ANGELIC_FEATHER;
	public static Item KRAKEN_SHELL;
	public static Item INFERNAL_CLAWS;
	public static Item ANGELHEART_VIAL;
	public static Item PHOENIX_DOWN;
	public static Item AMPHIBIAN_BOOT;
	public static Item ICE_BREAKER;
	/** Every item by id, belt first. */
	public static final Map<String, Item> ITEMS = new LinkedHashMap<>();

	private Wayfaring() {
	}

	public static void register() {
		CHARM_SLOT_COUNT = SlotAttributes.createAttributeForSlot(CHARM_SLOT);
		LEATHER_BELT = item("leather_belt", "leather_belt", Map.of(SLOT_COUNT, (double) BELT_CHARM_SLOTS), Rarity.COMMON, false);
		ANGELIC_FEATHER = item("angelic_feather", "angelic_feather", Map.of("minecraft:jump_strength", FEATHER_JUMP), Rarity.UNCOMMON, false);
		KRAKEN_SHELL = item("kraken_shell", "kraken_shell", Map.of(), Rarity.UNCOMMON, false);
		INFERNAL_CLAWS = item("infernal_claws", "infernal_claws", Map.of(), Rarity.UNCOMMON, true);
		ANGELHEART_VIAL = item("angelheart_vial", "angelheart_vial", Map.of(), Rarity.UNCOMMON, false);
		// Worn, the down is a feather as well: the feather's modifiers, under the feather's names.
		PHOENIX_DOWN = item("phoenix_down", "angelic_feather", Map.of("minecraft:jump_strength", FEATHER_JUMP), Rarity.RARE, true);
		AMPHIBIAN_BOOT = item("amphibian_boot", "amphibian_boot", Map.of("minecraft:water_movement_efficiency", AMPHIBIAN_SWIM,
				"minecraft:oxygen_bonus", AMPHIBIAN_OXYGEN), Rarity.UNCOMMON, false);
		ICE_BREAKER = item("ice_breaker", "ice_breaker", Map.of("minecraft:knockback_resistance", ICE_BREAKER_KNOCKBACK), Rarity.UNCOMMON, false);
		ABSORBS.forEach((item, types) -> ABSORBED.put(item, types.stream()
				.map(type -> ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.parse(type))).toList()));
		ServerLivingEntityEvents.ALLOW_DAMAGE.register(Wayfaring::allowDamage);
		ServerLivingEntityEvents.ALLOW_DEATH.addPhaseOrdering(SAVE_PHASE, Event.DEFAULT_PHASE);
		ServerLivingEntityEvents.ALLOW_DEATH.register(SAVE_PHASE,
				(entity, source, amount) -> !(entity instanceof ServerPlayer player) || !save(player, source));
		ServerLivingEntityEvents.AFTER_DAMAGE.register(Wayfaring::afterDamage);
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(output -> ITEMS.values().forEach(output::accept));
		WornDisplay.register();
	}

	private static Item item(String id, String kind, Map<String, Double> modifiers, Rarity rarity, boolean fireResistant) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Jugcraft.id(id));
		Item.Properties properties = new Item.Properties().stacksTo(1).rarity(rarity).setId(key);
		Item item = Registry.register(BuiltInRegistries.ITEM, key,
				new WornTrinketItem(fireResistant ? properties.fireResistant() : properties, kind, modifiers));
		ITEMS.put(id, item);
		return item;
	}

	/**
	 * The worn slot (where Trinkets applies effects: not a cosmetic slot) holding the first stack {@code which} accepts,
	 * or null.
	 */
	public static @Nullable TrinketSlotAccess worn(LivingEntity entity, Predicate<ItemStack> which) {
		AtomicReference<TrinketSlotAccess> found = new AtomicReference<>();
		TrinketsApi.getAttachment(entity).forEach((access, stack) -> {
			if (found.get() == null && !stack.isEmpty() && !access.cosmetic() && access.canApplyEffects() && which.test(stack)) {
				found.set(access);
			}
		});
		return found.get();
	}

	public static boolean wears(LivingEntity entity, Item item) {
		return worn(entity, stack -> stack.is(item)) != null;
	}

	/**
	 * Whether a worn charm covers this harm to {@code player}: falling for the feather (and the down), drowning for the
	 * shell, fire but not lava for the claws. Only while the food bar is not empty, and only once Relic Lore is
	 * understood. Whether the bar can pay for a blow is {@link #absorb}'s to say.
	 */
	public static boolean absorbs(ServerPlayer player, DamageSource source) {
		if (player.getFoodData().getFoodLevel() <= 0 || !Reliquary.enabled() || !Reliquary.knows(player)) {
			return false;
		}
		for (var entry : ABSORBED.entrySet()) {
			if (entry.getValue().stream().anyMatch(type -> source.is(type)) && wears(player, ITEMS.get(entry.getKey()))) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Takes a blow of {@code amount} harm (before armour) from {@code player}'s food instead of their health, if a worn
	 * charm covers it and the food bar can pay: {@link #ABSORB_EXHAUSTION} a point, whole food points straight from the
	 * bar and the rest as exhaustion. Within {@link #ABSORB_COOLDOWN_TICKS} of a blow it took, a blow costs only what it is
	 * bigger by, as vanilla's hurt cooldown would let through. A blow the bar cannot pay for lands in full. Returns whether
	 * the blow was taken.
	 */
	public static boolean absorb(ServerPlayer player, DamageSource source, float amount) {
		if (amount <= 0.0F || !absorbs(player, source)) {
			return false;
		}
		long now = player.level().getGameTime();
		Taken last = TAKEN.get(player);
		boolean cooling = last != null && now >= last.tick() && now - last.tick() < ABSORB_COOLDOWN_TICKS
				&& !source.is(BYPASSES_COOLDOWN);
		float exhaustion = (cooling ? Math.max(0.0F, amount - last.amount()) : amount) * ABSORB_EXHAUSTION;
		int points = (int) (exhaustion / 4.0F);
		FoodData food = player.getFoodData();
		if (points > food.getFoodLevel()) {
			return false;
		}
		food.setFoodLevel(food.getFoodLevel() - points);
		player.causeFoodExhaustion(exhaustion - points * 4.0F);
		TAKEN.put(player, cooling ? new Taken(last.tick(), Math.max(amount, last.amount())) : new Taken(now, amount));
		return true;
	}

	private static boolean allowDamage(LivingEntity entity, DamageSource source, float amount) {
		return !(entity instanceof ServerPlayer player) || !absorb(player, source, amount);
	}

	/**
	 * A blow that would kill {@code player}: a worn Angelheart Vial answers it (used up: a little health and Regeneration),
	 * else a worn Phoenix Down (full health, Regeneration and Fire Resistance; it becomes an Angelic Feather in its slot).
	 * Neither answers harm that passes through invulnerability (the void, /kill), a death in a dream (the dream ends as it
	 * would), or a blow a held totem answers. Returns whether it was answered.
	 */
	public static boolean save(ServerPlayer player, DamageSource source) {
		if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || !Reliquary.enabled() || Dreaming.dreaming(player) || holdsTotem(player)
				|| !Reliquary.knows(player)) {
			return false;
		}
		TrinketSlotAccess vial = worn(player, stack -> stack.is(ANGELHEART_VIAL));
		TrinketSlotAccess down = vial == null ? worn(player, stack -> stack.is(PHOENIX_DOWN)) : null;
		if (vial == null && down == null) {
			return false;
		}
		ServerLevel level = player.level();
		if (vial != null) {
			vial.set(ItemStack.EMPTY);
			player.setHealth(Math.min(VIAL_HEALTH, player.getMaxHealth()));
			statuses(level, player, "angelheart_vial", List.of(VIAL_REGENERATION));
			level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.2F);
		} else {
			down.set(new ItemStack(ANGELIC_FEATHER));
			player.setHealth(player.getMaxHealth());
			statuses(level, player, "phoenix_down", PHOENIX_STATUSES);
			level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0F, 0.8F);
		}
		Signs.show(level, player.position().add(0.0, 1.0, 0.0), Sign.DONE, null);
		return true;
	}

	/** Whether either hand holds something that answers death itself (a totem: vanilla's turn comes after this one). */
	private static boolean holdsTotem(Player player) {
		for (InteractionHand hand : InteractionHand.values()) {
			if (player.getItemInHand(hand).has(DataComponents.DEATH_PROTECTION)) {
				return true;
			}
		}
		return false;
	}

	/** The save's statuses on the wearer, as the item's, through the shared effect boundary. */
	private static void statuses(ServerLevel level, ServerPlayer player, String item, List<EffectSpec> statuses) {
		Cause cause = Cause.of(player.getUUID(), Cause.Origin.ITEM, Jugcraft.id(item).toString(), ConcordanceEffects.nextSerial());
		Ledger ledger = new Ledger(new Ledger.Limits(1, statuses.size() * EffectKind.STATUS.work, 0));
		for (int i = 0; i < statuses.size(); i++) {
			ConcordanceEffects.apply(new ConcordanceEffects.Context(level, cause, player, ledger, item + "/" + i, player.position()),
					statuses.get(i), player);
		}
	}

	/** The Ice Breaker: after a plain fall that hurt its wearer (not an ender pearl's landing), on foot, the wave. */
	private static void afterDamage(LivingEntity entity, DamageSource source, float baseDamage, float damage, boolean blocked) {
		if (damage <= 0.0F || !source.is(DamageTypes.FALL) || !(entity instanceof ServerPlayer player) || player.isPassenger()
				|| !Reliquary.enabled() || !wears(player, ICE_BREAKER) || !Reliquary.knows(player)) {
			return;
		}
		wave(player.level(), player, damage);
	}

	/**
	 * The Ice Breaker's wave from where {@code player} landed, for a fall that did {@code harm}: the nearest hostile
	 * creatures within its radius, up to {@link #WAVE_TARGETS}, are harmed, thrown back and slowed, each as
	 * {@code player}'s, through the shared effect boundary. Returns how many it reached.
	 */
	public static int wave(ServerLevel level, ServerPlayer player, float harm) {
		double radius = Math.min(WAVE_MAX_RADIUS, WAVE_RADIUS + harm * WAVE_RADIUS_PER_HARM);
		Vec3 origin = player.position();
		List<LivingEntity> near = new ArrayList<>(level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius),
				creature -> creature != player && creature instanceof Enemy && creature.isAlive() && creature.distanceToSqr(origin) <= radius * radius));
		near.sort(Comparator.comparingDouble(creature -> creature.distanceToSqr(origin)));
		Cause cause = Cause.of(player.getUUID(), Cause.Origin.ITEM, Jugcraft.id("ice_breaker").toString(), ConcordanceEffects.nextSerial());
		int work = WAVE.stream().mapToInt(spec -> spec.kind().work).sum();
		Ledger ledger = new Ledger(new Ledger.Limits(WAVE_TARGETS, WAVE_TARGETS * work, 0));
		int reached = 0;
		for (LivingEntity target : near.subList(0, Math.min(WAVE_TARGETS, near.size()))) {
			boolean applied = false;
			for (int i = 0; i < WAVE.size(); i++) {
				applied |= ConcordanceEffects.apply(new ConcordanceEffects.Context(level, cause, player, ledger, "ice_breaker/" + i, origin),
						WAVE.get(i), target).applied();
			}
			if (applied) {
				reached++;
			}
		}
		Signs.show(level, origin.add(0.0, 0.2, 0.0), Sign.SHOVE, null);
		level.playSound(null, origin.x, origin.y, origin.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.4F, 1.4F);
		return reached;
	}
}
