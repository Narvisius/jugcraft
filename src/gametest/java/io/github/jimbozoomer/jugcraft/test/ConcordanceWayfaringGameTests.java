package io.github.jimbozoomer.jugcraft.test;

import eu.pb4.trinkets.api.SlotAttributes;
import eu.pb4.trinkets.api.SlotType;
import eu.pb4.trinkets.api.TrinketInventory;
import eu.pb4.trinkets.api.TrinketSlotAccess;
import eu.pb4.trinkets.api.TrinketsApi;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import io.github.jimbozoomer.jugcraft.concordance.dream.DreamRules;
import io.github.jimbozoomer.jugcraft.concordance.dreaming.Dreaming;
import io.github.jimbozoomer.jugcraft.concordance.reliquary.Reliquary;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import io.github.jimbozoomer.jugcraft.concordance.sympathy.Sympathy;
import io.github.jimbozoomer.jugcraft.concordance.trinket.Wayfaring;
import io.github.jimbozoomer.jugcraft.concordance.trinket.WornDisplay;
import io.github.jimbozoomer.jugcraft.concordance.trinket.WornTrinketItem;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

/**
 * Wayfaring, the trinkets slice's part 1 (docs/features/arcane-concordance-trinkets.md): the owner's belt, boots and
 * charms. Mock players never tick, so Trinkets' own equipment scan never runs here: each test asks the items what Trinkets
 * would ask (their modifiers, whether they go on or come off) and drives Fabric's damage events through their real
 * invokers, so the listeners answer in the order the game runs them.
 */
public class ConcordanceWayfaringGameTests {
	private static final BlockPos STAND = new BlockPos(3, 2, 3);
	private static final List<String> CHARMS = List.of("angelic_feather", "kraken_shell", "infernal_claws", "angelheart_vial", "phoenix_down");
	private static final List<String> FEET = List.of("amphibian_boot", "ice_breaker");

	private static void floor(GameTestHelper helper) {
		for (int x = 0; x <= 7; x++) {
			for (int z = 0; z <= 7; z++) {
				helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
			}
		}
	}

	/** A survival player on {@code STAND}; with {@code lore}, one who understands Relic Lore. */
	private static ServerPlayer wayfarer(GameTestHelper helper, boolean lore) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		BlockPos absolute = helper.absolutePos(STAND);
		player.snapTo(absolute.getX() + 0.5, absolute.getY(), absolute.getZ() + 0.5, 0.0F, 0.0F);
		if (lore) {
			ConcordanceProgress.grant(player, "jugcraft:first_light", ResearchState.UNDERSTOOD);
			ConcordanceProgress.grant(player, Reliquary.RESEARCH, ResearchState.UNDERSTOOD);
		}
		RateGate.forget(player.getUUID());
		return player;
	}

	private static Item item(String id) {
		return BuiltInRegistries.ITEM.getValue(Jugcraft.id(id));
	}

	private static boolean in(ItemStack stack, String tag) {
		return stack.is(TagKey.create(Registries.ITEM, Identifier.parse(tag)));
	}

	private static TrinketInventory inventory(ServerPlayer wearer, String type) {
		return TrinketsApi.getAttachment(wearer).getInventory(type);
	}

	private static TrinketSlotAccess slot(ServerPlayer wearer, String type, int index) {
		return inventory(wearer, type).getOrCreateSlotAccess(index);
	}

	/** What an item gives worn in a slot ("attribute=amount@id"), asked as Trinkets asks it. */
	private static List<String> modifiers(ItemStack stack, ServerPlayer wearer, String type, int index) {
		List<String> seen = new ArrayList<>();
		TrinketSlotAccess access = slot(wearer, type, index);
		((WornTrinketItem) stack.getItem()).forEachTrinketModifier(stack, access, wearer, SlotAttributes.getIdentifier(access),
				(attribute, modifier) -> seen.add(attribute.getRegisteredName() + "=" + modifier.amount() + "@" + modifier.id()));
		return seen;
	}

	/** The belt's Charm slot modifier, applied by hand as Trinkets' scan would (it is saved with the wearer: take it off). */
	private static AttributeModifier belt(ServerPlayer wearer) {
		List<AttributeModifier> found = new ArrayList<>();
		ItemStack belt = new ItemStack(Wayfaring.LEATHER_BELT);
		TrinketSlotAccess access = slot(wearer, Wayfaring.BELT_SLOT, 0);
		((WornTrinketItem) belt.getItem()).forEachTrinketModifier(belt, access, wearer, SlotAttributes.getIdentifier(access), (attribute, modifier) -> {
			if (attribute.equals(Wayfaring.CHARM_SLOT_COUNT) && attribute.value() instanceof SlotAttributes.SlotModifyingAttribute) {
				found.add(modifier);
			}
		});
		AttributeModifier modifier = found.getFirst();
		inventory(wearer, Wayfaring.CHARM_SLOT).addSlotCountModifier(modifier);
		return modifier;
	}

	/** A damage source of a vanilla type by its key (not every type has a DamageSources shortcut in 26.3). */
	private static DamageSource source(ServerLevel level, ResourceKey<DamageType> type) {
		return new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(type));
	}

	private static boolean allowDeath(ServerPlayer player, DamageSource source) {
		return ServerLivingEntityEvents.ALLOW_DEATH.invoker().allowDeath(player, source, 1000.0F);
	}

	/**
	 * The eight items: registered, one to a stack, the owner's names; the three slots players have (the belt slot
	 * Trinkets', the charm and feet slots Jugcraft's with the owner's icons and no cosmetic copies); each slot's tag holds
	 * exactly its items; every attribute the items name exists; and none is ever weighed at the Assayer's Scale.
	 */
	@GameTest(maxTicks = 20)
	public void theTrinketsAreMadeAndWornAsDesigned(GameTestHelper helper) {
		ServerPlayer player = wayfarer(helper, false);
		helper.assertTrue(Wayfaring.ITEMS.size() == 8 && Wayfaring.ITEMS.values().stream().allMatch(item -> item instanceof WornTrinketItem
				&& new ItemStack(item).getMaxStackSize() == 1), "Eight worn items, one to a stack: " + Wayfaring.ITEMS.keySet());
		Map<String, Integer> amounts = Map.of(Wayfaring.BELT_SLOT, 1, Wayfaring.CHARM_SLOT, Wayfaring.CHARM_SLOTS, Wayfaring.FEET_SLOT, 2);
		amounts.forEach((type, amount) -> {
			TrinketInventory inventory = inventory(player, type);
			helper.assertTrue(inventory != null && inventory.getContainerSize() == amount, "Players have " + amount + " " + type + " slots");
		});
		SlotType charm = inventory(player, Wayfaring.CHARM_SLOT).slotType();
		SlotType feet = inventory(player, Wayfaring.FEET_SLOT).slotType();
		helper.assertTrue(charm.icon().equals(Jugcraft.id("container/slots/charm")) && feet.icon().equals(Jugcraft.id("container/slots/feet"))
				&& !charm.supportsCosmeticSlots() && !feet.supportsCosmeticSlots(),
				"The charm and feet slots draw the owner's icons and have no cosmetic copies: " + charm.icon() + ", " + feet.icon());
		helper.assertTrue(in(new ItemStack(Wayfaring.LEATHER_BELT), "trinkets:legs/belt")
				&& CHARMS.stream().allMatch(id -> in(new ItemStack(item(id)), "trinkets:legs/charm") && !in(new ItemStack(item(id)), "trinkets:feet/boots"))
				&& FEET.stream().allMatch(id -> in(new ItemStack(item(id)), "trinkets:feet/boots") && !in(new ItemStack(item(id)), "trinkets:legs/charm")),
				"Each slot takes exactly its items");
		for (String attribute : List.of("minecraft:jump_strength", "minecraft:water_movement_efficiency", "minecraft:oxygen_bonus",
				"minecraft:knockback_resistance", Wayfaring.SLOT_COUNT)) {
			helper.assertTrue(BuiltInRegistries.ATTRIBUTE.get(Identifier.parse(attribute)).isPresent(), "The attribute " + attribute + " exists");
		}
		for (Map.Entry<String, Item> entry : Wayfaring.ITEMS.entrySet()) {
			helper.assertTrue(in(new ItemStack(entry.getValue()), "jugcraft:equivalence/excluded"), entry.getKey() + " is never weighed");
		}
		helper.assertTrue(new ItemStack(Wayfaring.LEATHER_BELT).getHoverName().getString().equals("Leather Belt")
				&& new ItemStack(BuiltInRegistries.ITEM.getValue(Jugcraft.id("belt"))).getHoverName().getString().equals("Drive Belt"),
				"The owner's Leather Belt and the kinetic Drive Belt are told apart");
		helper.succeed();
	}

	/**
	 * Only someone who understands Relic Lore can put one on; each gives exactly the generator's modifiers, named by its
	 * kind, so two of a kind (or a Phoenix Down beside a feather) give them once.
	 */
	@GameTest(maxTicks = 20)
	public void onlyRelicLoreWearsThemAndTwoOfAKindNeverAddUp(GameTestHelper helper) {
		ServerPlayer novice = wayfarer(helper, false);
		ServerPlayer player = wayfarer(helper, true);
		for (Map.Entry<String, Item> entry : Wayfaring.ITEMS.entrySet()) {
			ItemStack stack = new ItemStack(entry.getValue());
			String type = entry.getKey().equals("leather_belt") ? Wayfaring.BELT_SLOT : FEET.contains(entry.getKey()) ? Wayfaring.FEET_SLOT
					: Wayfaring.CHARM_SLOT;
			WornTrinketItem worn = (WornTrinketItem) entry.getValue();
			helper.assertTrue(!worn.canEquip(stack, slot(novice, type, 0), novice) && worn.canEquip(stack, slot(player, type, 0), player),
					entry.getKey() + " goes on only once Relic Lore is understood");
		}
		Map<String, List<String>> expected = Map.of(
				"leather_belt", List.of("trinkets:slot_count/legs/charm=1.0@jugcraft:wayfaring/leather_belt/slot_count/legs/charm"),
				"angelic_feather", List.of("minecraft:jump_strength=" + Wayfaring.FEATHER_JUMP + "@jugcraft:wayfaring/angelic_feather/jump_strength"),
				"phoenix_down", List.of("minecraft:jump_strength=" + Wayfaring.FEATHER_JUMP + "@jugcraft:wayfaring/angelic_feather/jump_strength"),
				"kraken_shell", List.of(), "infernal_claws", List.of(), "angelheart_vial", List.of(),
				"amphibian_boot", List.of("minecraft:oxygen_bonus=" + Wayfaring.AMPHIBIAN_OXYGEN + "@jugcraft:wayfaring/amphibian_boot/oxygen_bonus",
						"minecraft:water_movement_efficiency=" + Wayfaring.AMPHIBIAN_SWIM + "@jugcraft:wayfaring/amphibian_boot/water_movement_efficiency"),
				"ice_breaker", List.of("minecraft:knockback_resistance=" + Wayfaring.ICE_BREAKER_KNOCKBACK + "@jugcraft:wayfaring/ice_breaker/knockback_resistance"));
		expected.forEach((id, wanted) -> {
			String type = id.equals("leather_belt") ? Wayfaring.BELT_SLOT : FEET.contains(id) ? Wayfaring.FEET_SLOT : Wayfaring.CHARM_SLOT;
			List<String> first = modifiers(new ItemStack(item(id)), player, type, 0);
			List<String> second = modifiers(new ItemStack(item(id)), player, type, 1);
			helper.assertTrue(first.stream().sorted().toList().equals(wanted) && first.equals(second),
					id + " gives " + wanted + " from any slot, under the same names: " + first + " / " + second);
		});
		// Trinkets adds a modifier by removing its id first: a feather and a down worn together add the jump once.
		AttributeInstance jump = player.getAttribute(Attributes.JUMP_STRENGTH);
		double base = jump.getValue();
		List<AttributeModifier> worn = new ArrayList<>();
		for (String id : List.of("angelic_feather", "phoenix_down")) {
			ItemStack stack = new ItemStack(item(id));
			TrinketSlotAccess access = slot(player, Wayfaring.CHARM_SLOT, worn.size());
			((WornTrinketItem) stack.getItem()).forEachTrinketModifier(stack, access, player, SlotAttributes.getIdentifier(access), (attribute, modifier) -> {
				jump.removeModifier(modifier.id());
				jump.addTransientModifier(modifier);
				worn.add(modifier);
			});
		}
		helper.assertTrue(Math.abs(jump.getValue() - base - Wayfaring.FEATHER_JUMP) < 1.0E-9,
				"A feather and a Phoenix Down jump as one feather: " + base + " -> " + jump.getValue());
		worn.forEach(modifier -> jump.removeModifier(modifier.id()));
		helper.succeed();
	}

	/**
	 * A worn Leather Belt adds a Charm slot; it cannot come off while that slot holds a charm (Trinkets would drop the
	 * charm on the ground), and can once it is empty.
	 */
	@GameTest(maxTicks = 20)
	public void theBeltKeepsItsCharm(GameTestHelper helper) {
		ServerPlayer player = wayfarer(helper, true);
		TrinketInventory charms = inventory(player, Wayfaring.CHARM_SLOT);
		inventory(player, Wayfaring.BELT_SLOT).setItem(0, new ItemStack(Wayfaring.LEATHER_BELT));
		AttributeModifier modifier = belt(player);
		try {
			helper.assertTrue(charms.getContainerSize() == Wayfaring.CHARM_SLOTS + Wayfaring.BELT_CHARM_SLOTS, "The belt adds a Charm slot");
			TrinketSlotAccess beltSlot = slot(player, Wayfaring.BELT_SLOT, 0);
			ItemStack belt = beltSlot.get();
			WornTrinketItem item = (WornTrinketItem) belt.getItem();
			charms.setItem(0, new ItemStack(Wayfaring.KRAKEN_SHELL));
			helper.assertTrue(item.canUnequip(belt, beltSlot, player), "A charm in the first slot keeps nothing on");
			charms.setItem(1, new ItemStack(Wayfaring.INFERNAL_CLAWS));
			helper.assertTrue(!item.canUnequip(belt, beltSlot, player) && WornTrinketItem.holdsAddedCharm(player),
					"With a charm in the belt's slot, the belt stays on");
			charms.setItem(1, ItemStack.EMPTY);
			helper.assertTrue(item.canUnequip(belt, beltSlot, player), "With that slot empty it comes off");
		} finally {
			charms.removeSlotCountModifier(modifier.id());
			charms.setItem(0, ItemStack.EMPTY);
			inventory(player, Wayfaring.BELT_SLOT).setItem(0, ItemStack.EMPTY);
		}
		helper.assertTrue(charms.getContainerSize() == Wayfaring.CHARM_SLOTS, "Off, the Charm slot is gone again");
		helper.succeed();
	}

	/**
	 * The feather, shell and claws take their harm from food instead of health: each only its own harm (the claws not
	 * lava), only worn where it works, only with food in the bar, and only once Relic Lore is understood.
	 */
	@GameTest(maxTicks = 20)
	public void charmsTakeTheirHarmFromFood(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer player = wayfarer(helper, true);
		TrinketInventory charms = inventory(player, Wayfaring.CHARM_SLOT);
		var sources = level.damageSources();
		helper.assertTrue(!Wayfaring.absorbs(player, sources.fall()), "Nothing worn, nothing taken");
		charms.setItem(0, new ItemStack(Wayfaring.ANGELIC_FEATHER));
		helper.assertTrue(Wayfaring.absorbs(player, sources.fall()) && !Wayfaring.absorbs(player, sources.drown())
				&& !Wayfaring.absorbs(player, sources.generic()), "The feather takes a fall, and only a fall");
		helper.assertTrue(!ServerLivingEntityEvents.ALLOW_DAMAGE.invoker().allowDamage(player, sources.fall(), 6.0F),
				"The fall's harm is refused to health");
		charms.setItem(0, new ItemStack(Wayfaring.PHOENIX_DOWN));
		helper.assertTrue(Wayfaring.absorbs(player, sources.fall()), "A Phoenix Down is a feather as well");
		charms.setItem(0, new ItemStack(Wayfaring.KRAKEN_SHELL));
		helper.assertTrue(Wayfaring.absorbs(player, sources.drown()) && !Wayfaring.absorbs(player, sources.fall()), "The shell takes drowning");
		charms.setItem(0, new ItemStack(Wayfaring.INFERNAL_CLAWS));
		helper.assertTrue(Wayfaring.absorbs(player, sources.onFire()) && Wayfaring.absorbs(player, sources.inFire())
				&& Wayfaring.absorbs(player, sources.hotFloor()) && !Wayfaring.absorbs(player, sources.lava()),
				"The claws take fire and hot floors, not lava");
		player.getFoodData().setFoodLevel(0);
		helper.assertTrue(!Wayfaring.absorbs(player, sources.onFire())
				&& ServerLivingEntityEvents.ALLOW_DAMAGE.invoker().allowDamage(player, sources.onFire(), 1.0F),
				"With an empty food bar the harm lands");
		player.getFoodData().setFoodLevel(20);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Wayfaring.INFERNAL_CLAWS));
		charms.setItem(0, ItemStack.EMPTY);
		helper.assertTrue(!Wayfaring.absorbs(player, sources.onFire()), "Held, not worn, the claws take nothing");
		ServerPlayer novice = wayfarer(helper, false);
		inventory(novice, Wayfaring.CHARM_SLOT).setItem(0, new ItemStack(Wayfaring.ANGELIC_FEATHER));
		helper.assertTrue(!Wayfaring.absorbs(novice, sources.fall()), "Before Relic Lore (put on by other means) the feather takes nothing");
		inventory(novice, Wayfaring.CHARM_SLOT).setItem(0, ItemStack.EMPTY);
		helper.succeed();
	}

	/**
	 * What a charm's blow costs, through the real event: half a food point a point of harm (a 6-harm fall, 3 points);
	 * within vanilla's hurt cooldown a further blow costs only what it is bigger by (so fire trying to hurt every tick is
	 * paid as often as it would land); a blow the bar cannot pay for lands in full and costs nothing.
	 */
	@GameTest(maxTicks = 20)
	public void aCharmsBlowIsPaidInFood(GameTestHelper helper) {
		var sources = helper.getLevel().damageSources();
		ServerPlayer player = wayfarer(helper, true);
		inventory(player, Wayfaring.CHARM_SLOT).setItem(0, new ItemStack(Wayfaring.ANGELIC_FEATHER));
		player.getFoodData().setFoodLevel(20);
		helper.assertTrue(!ServerLivingEntityEvents.ALLOW_DAMAGE.invoker().allowDamage(player, sources.fall(), 6.0F)
				&& player.getFoodData().getFoodLevel() == 17, "A 6-harm fall costs 3 food points: " + player.getFoodData().getFoodLevel());
		helper.assertTrue(!ServerLivingEntityEvents.ALLOW_DAMAGE.invoker().allowDamage(player, sources.fall(), 4.0F)
				&& player.getFoodData().getFoodLevel() == 17, "A smaller blow within the hurt cooldown costs nothing");
		helper.assertTrue(!ServerLivingEntityEvents.ALLOW_DAMAGE.invoker().allowDamage(player, sources.fall(), 10.0F)
				&& player.getFoodData().getFoodLevel() == 15, "A bigger one costs what it is bigger by (4 harm, 2 points): "
						+ player.getFoodData().getFoodLevel());
		ServerPlayer hungry = wayfarer(helper, true);
		inventory(hungry, Wayfaring.CHARM_SLOT).setItem(0, new ItemStack(Wayfaring.ANGELIC_FEATHER));
		hungry.getFoodData().setFoodLevel(2);
		helper.assertTrue(ServerLivingEntityEvents.ALLOW_DAMAGE.invoker().allowDamage(hungry, sources.fall(), 10.0F)
				&& hungry.getFoodData().getFoodLevel() == 2, "A fall the bar cannot pay for (5 points of 2) lands in full, costing no food");
		helper.assertTrue(!ServerLivingEntityEvents.ALLOW_DAMAGE.invoker().allowDamage(hungry, sources.fall(), 4.0F)
				&& hungry.getFoodData().getFoodLevel() == 0, "One it can pay for (2 points of 2) is taken");
		inventory(player, Wayfaring.CHARM_SLOT).setItem(0, ItemStack.EMPTY);
		inventory(hungry, Wayfaring.CHARM_SLOT).setItem(0, ItemStack.EMPTY);
		helper.succeed();
	}

	/**
	 * Death saves, through the real event: a worn vial leaves the wearer on a little health with Regeneration II and is
	 * used up; the Phoenix Down rises at full health with Regeneration II and Fire Resistance and becomes an Angelic
	 * Feather in its slot; a vial answers before a down; and neither answers the void, a held totem, or Relic Lore not
	 * understood.
	 */
	@GameTest(maxTicks = 20)
	public void vialsAndDownAnswerDeath(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer player = wayfarer(helper, true);
		TrinketInventory charms = inventory(player, Wayfaring.CHARM_SLOT);
		inventory(player, Wayfaring.BELT_SLOT).setItem(0, new ItemStack(Wayfaring.LEATHER_BELT));
		AttributeModifier belt = belt(player);
		try {
			charms.setItem(0, new ItemStack(Wayfaring.PHOENIX_DOWN));
			charms.setItem(1, new ItemStack(Wayfaring.ANGELHEART_VIAL));
			player.setHealth(1.0F);
			helper.assertTrue(!allowDeath(player, level.damageSources().generic()), "The vial answers death");
			MobEffectInstance regeneration = player.getEffect(MobEffects.REGENERATION);
			helper.assertTrue(player.getHealth() == Wayfaring.VIAL_HEALTH && charms.getItem(1).isEmpty() && charms.getItem(0).is(Wayfaring.PHOENIX_DOWN)
					&& regeneration != null && regeneration.getAmplifier() == 1 && regeneration.getDuration() <= Wayfaring.VIAL_REGENERATION_TICKS,
					"The vial answered first: " + player.getHealth() + " health, the vial used up, the down kept, " + regeneration);
			player.removeAllEffects();
			player.setHealth(1.0F);
			helper.assertTrue(!allowDeath(player, level.damageSources().generic()), "The Phoenix Down answers the next death");
			helper.assertTrue(player.getHealth() == player.getMaxHealth() && charms.getItem(0).is(Wayfaring.ANGELIC_FEATHER)
					&& player.hasEffect(MobEffects.REGENERATION) && player.hasEffect(MobEffects.FIRE_RESISTANCE),
					"At full health, healing and proof against fire; the down is an Angelic Feather where it was worn");
			charms.setItem(0, new ItemStack(Wayfaring.ANGELHEART_VIAL));
			helper.assertTrue(!Wayfaring.save(player, source(level, DamageTypes.FELL_OUT_OF_WORLD)) && charms.getItem(0).is(Wayfaring.ANGELHEART_VIAL),
					"The void passes through: the vial is not spent");
			player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace("totem_of_undying"))));
			helper.assertTrue(!Wayfaring.save(player, level.damageSources().generic()) && charms.getItem(0).is(Wayfaring.ANGELHEART_VIAL),
					"A held totem answers in the vial's place");
			player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
			ServerPlayer novice = wayfarer(helper, false);
			inventory(novice, Wayfaring.CHARM_SLOT).setItem(0, new ItemStack(Wayfaring.ANGELHEART_VIAL));
			helper.assertTrue(!Wayfaring.save(novice, level.damageSources().generic()), "Before Relic Lore it does not answer");
			inventory(novice, Wayfaring.CHARM_SLOT).setItem(0, ItemStack.EMPTY);
		} finally {
			charms.removeSlotCountModifier(belt.id());
			charms.setItem(0, ItemStack.EMPTY);
			inventory(player, Wayfaring.BELT_SLOT).setItem(0, ItemStack.EMPTY);
		}
		helper.succeed();
	}

	/**
	 * A vial worn during a dream (put on after the dream began) does not answer a death in it: the saves run before
	 * Dreaming's listener, see the open dream and decline, and the dream then ends as a death, as it would without the vial.
	 */
	@GameTest(maxTicks = 20)
	public void aDreamDeathIsNotSaved(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer dreamer = wayfarer(helper, true);
		ConcordanceProgress.grant(dreamer, Sympathy.RESEARCH, ResearchState.UNDERSTOOD);
		ConcordanceProgress.grant(dreamer, Dreaming.RESEARCH, ResearchState.UNDERSTOOD);
		ConcordanceProgress.setFocus(dreamer, 20);
		BlockPos censer = helper.absolutePos(new BlockPos(2, 1, 5));
		helper.setBlock(new BlockPos(2, 1, 5), Dreaming.CENSER);
		String reason = Dreaming.begin(dreamer, level, censer, true);
		helper.assertTrue(reason.isEmpty() && Dreaming.dreaming(dreamer), "The dream begins: " + reason);
		TrinketInventory charms = inventory(dreamer, Wayfaring.CHARM_SLOT);
		charms.setItem(0, new ItemStack(Wayfaring.ANGELHEART_VIAL));
		try {
			helper.assertTrue(allowDeath(dreamer, level.damageSources().generic()), "The death in the dream goes ahead");
			helper.assertTrue(charms.getItem(0).is(Wayfaring.ANGELHEART_VIAL) && !Dreaming.dreaming(dreamer),
					"The vial is not spent, and the dream ended as a death");
		} finally {
			charms.setItem(0, ItemStack.EMPTY);
			if (Dreaming.dreaming(dreamer)) {
				Dreaming.end(dreamer, DreamRules.End.WOKE);
			}
		}
		helper.succeed();
	}

	/**
	 * The Ice Breaker: a plain fall that hurts its wearer harms, throws back and slows the nearest hostile creatures
	 * within its radius (wider for a harder fall), at most twelve, and nothing else: not a villager, not one beyond the
	 * radius; a great foe in the boundary's immune tag (the Elder Guardian) takes the harm but is not slowed; nothing after
	 * an ender pearl's landing, nor before Relic Lore. (Claims never shelter hostile creatures: the boundary lets anyone
	 * fight them.)
	 */
	@GameTest(maxTicks = 40)
	public void theIceBreakersWaveThrowsBackFoes(GameTestHelper helper) {
		floor(helper);
		ServerLevel level = helper.getLevel();
		ServerPlayer player = wayfarer(helper, true);
		inventory(player, Wayfaring.FEET_SLOT).setItem(1, new ItemStack(Wayfaring.ICE_BREAKER));
		Mob near = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(5, 2, 3));
		Mob villager = helper.spawnWithNoFreeWill(EntityTypes.VILLAGER, new BlockPos(3, 2, 5));
		Mob immune = helper.spawnWithNoFreeWill(EntityTypes.ELDER_GUARDIAN, new BlockPos(1, 2, 3));
		// About 4.2 blocks off: within a hard fall's wave (8 harm: 5 blocks), beyond a light one's (1 harm: 3.25 blocks).
		Mob far = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(6, 2, 6));
		{
			ServerLivingEntityEvents.AFTER_DAMAGE.invoker().afterDamage(player, source(level, DamageTypes.ENDER_PEARL), 5.0F, 5.0F, false);
			helper.assertTrue(near.getHealth() == near.getMaxHealth(), "An ender pearl's landing sends no wave");
			ServerLivingEntityEvents.AFTER_DAMAGE.invoker().afterDamage(player, level.damageSources().fall(), 1.0F, 1.0F, false);
			MobEffectInstance slowed = near.getEffect(MobEffects.SLOWNESS);
			helper.assertTrue(near.getHealth() < near.getMaxHealth() && slowed != null && slowed.getDuration() <= Wayfaring.WAVE_SLOW_TICKS
					&& near.getDeltaMovement().horizontalDistance() > 0.0, "A fall's wave harms, slows and throws back the zombie: " + slowed);
			helper.assertTrue(far.getHealth() == far.getMaxHealth(), "A light fall's wave stops short of a zombie four blocks off");
			helper.assertTrue(villager.getHealth() == villager.getMaxHealth() && !villager.hasEffect(MobEffects.SLOWNESS),
					"A villager is not a foe");
			// The boundary's tolerance governs harmful control only (Tolerance.governs): the harm lands, the slowing does not.
			helper.assertTrue(immune.getHealth() < immune.getMaxHealth() && !immune.hasEffect(MobEffects.SLOWNESS),
					"An Elder Guardian (immune to the Concordance's control) takes the harm but is not slowed: " + immune.getHealth());
			ServerLivingEntityEvents.AFTER_DAMAGE.invoker().afterDamage(player, level.damageSources().fall(), 8.0F, 8.0F, false);
			helper.assertTrue(far.getHealth() < far.getMaxHealth(), "A hard fall's wave reaches it");
		}
		ServerPlayer novice = wayfarer(helper, false);
		inventory(novice, Wayfaring.FEET_SLOT).setItem(0, new ItemStack(Wayfaring.ICE_BREAKER));
		Mob spared = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(3, 2, 1));
		ServerLivingEntityEvents.AFTER_DAMAGE.invoker().afterDamage(novice, level.damageSources().fall(), 8.0F, 8.0F, false);
		helper.assertTrue(spared.getHealth() == spared.getMaxHealth(), "Before Relic Lore the boots send no wave");
		// At most twelve: fourteen zombies in reach of the widest wave, the nearest twelve reached.
		List<Mob> crowd = new ArrayList<>();
		for (int i = 0; i < 14; i++) {
			crowd.add(helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(i % 7, 2, i < 7 ? 0 : 7)));
		}
		int reached = Wayfaring.wave(level, player, 100.0F);
		helper.assertTrue(reached > 0 && reached <= Wayfaring.WAVE_TARGETS, "Of eighteen foes in reach, the wave reaches at most twelve: " + reached);
		inventory(player, Wayfaring.FEET_SLOT).setItem(1, ItemStack.EMPTY);
		inventory(novice, Wayfaring.FEET_SLOT).setItem(0, ItemStack.EMPTY);
		helper.succeed();
	}

	/**
	 * Part 1c: whether a wearer's belt and boots are drawn is their own choice, as the server takes it from their client
	 * (WornDisplay.choose, which the payload's receiver calls for the player who sent it): hidden, and shown again; a
	 * choice that changes nothing is dropped and leaves nothing waiting; a change within CHANGE_TICKS of the last waits for
	 * its tick, a newer choice replaces it, and the newest lands. The choice is sent to clients, kept through death and
	 * never saved (the client sends it again on joining). Whether everyone who sees the wearer is sent it is the attachment
	 * type's (check_mod_data holds it to AttachmentSyncPredicate.all()); the client test sees it come back.
	 */
	@GameTest(maxTicks = 40)
	public void eachWearerChoosesWhetherTheirTrinketsShow(GameTestHelper helper) {
		ServerPlayer player = wayfarer(helper, false);
		helper.assertFalse(WornDisplay.hidden(player), "A player who has chosen nothing shows their worn trinkets");
		WornDisplay.choose(player, true);
		helper.assertTrue(!WornDisplay.hidden(player) && !WornDisplay.pending(player), "Choosing to show what shows changes nothing");
		WornDisplay.choose(player, false);
		helper.assertTrue(WornDisplay.hidden(player) && !WornDisplay.pending(player), "Hiding them takes effect at once");
		WornDisplay.choose(player, true);
		helper.assertTrue(WornDisplay.hidden(player) && WornDisplay.pending(player), "Showing them again so soon waits for its tick");
		WornDisplay.choose(player, false);
		helper.assertTrue(WornDisplay.hidden(player) && !WornDisplay.pending(player),
				"A newer choice to keep them hidden replaces the waiting one, and nothing waits");
		WornDisplay.choose(player, true);
		helper.assertTrue(WornDisplay.HIDDEN.isSynced() && WornDisplay.HIDDEN.copyOnDeath() && !WornDisplay.HIDDEN.isPersistent(),
				"The choice is sent to clients, kept through death and not saved");
		helper.runAfterDelay(WornDisplay.CHANGE_TICKS + 2, () -> {
			helper.assertTrue(!WornDisplay.hidden(player) && !WornDisplay.pending(player), "The waiting choice lands once its tick comes");
			helper.succeed();
		});
	}
}
