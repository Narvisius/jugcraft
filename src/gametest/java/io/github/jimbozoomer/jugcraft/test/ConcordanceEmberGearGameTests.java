package io.github.jimbozoomer.jugcraft.test;

import eu.pb4.trinkets.api.SlotAttributes;
import eu.pb4.trinkets.api.TrinketSlotAccess;
import eu.pb4.trinkets.api.TrinketsApi;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceSpells;
import io.github.jimbozoomer.jugcraft.concordance.Invocations;
import io.github.jimbozoomer.jugcraft.concordance.JugcraftConcordance;
import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import io.github.jimbozoomer.jugcraft.concordance.ember.Ember;
import io.github.jimbozoomer.jugcraft.concordance.ember.EmberArmorItem;
import io.github.jimbozoomer.jugcraft.concordance.ember.EmberGear;
import io.github.jimbozoomer.jugcraft.concordance.ember.EmberTrinketItem;
import io.github.jimbozoomer.jugcraft.concordance.rules.FocusPool;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.item.v1.EnchantingContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.api.spell.registry.SpellRegistry;
import net.spell_engine.internals.SpellExecution;
import net.spell_engine.internals.casting.SpellCast;
import net.spell_engine.internals.casting.SpellCaster;
import net.spell_engine.internals.container.SpellContainerSource;
import net.spell_engine.internals.target.SpellTarget;
import net.spell_power.api.SpellSchools;

/**
 * Ember, part 2: the Hearthbinder's regalia (docs/features/arcane-concordance-ember-regalia.md). The eleven items are made
 * and worn as designed (the owner's two slots, two fire sets, half a point of fire on each piece); the foci give
 * fire Spell Power through Trinkets; the regalia at its most raises Cinderbolt from 3 to 6, and fewer than four pieces add
 * nothing; the Fire Bangle's blow leaves a Hearthbinder's target smouldering only on their own melee hit, only where they
 * may harm it; and Spell Power's attribute enchantments are refused on the sets.
 */
public class ConcordanceEmberGearGameTests {
	private static final String FIRST_LIGHT = "jugcraft:first_light";
	private static final String HEARTHBINDING = "jugcraft:hearthbinding";
	private static final UUID NEIGHBOUR = UUID.fromString("00000000-0000-0000-0000-00000000e3b2");
	/** The Pyromancer's (medium) pieces, head to feet. */
	private static final List<String> PIECES = List.of("pyromancers_hat", "pyromancers_robes", "pyromancers_leggings", "pyromancers_boots");
	/** The Pyromaniac's (light) pieces, head to feet. */
	private static final List<String> LIGHT = List.of("pyromaniacs_hood", "pyromaniacs_tunic", "pyromaniacs_pants", "pyromaniacs_shoes");
	private static final List<String> SETS = List.of("pyromaniacs", "pyromancers");
	private static final Map<String, List<String>> SET_PIECES = Map.of("pyromaniacs", LIGHT, "pyromancers", PIECES);
	private static final List<EquipmentSlot> SLOTS = List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);
	private static final List<String> SLOT_TAGS = List.of("head", "chest", "leg", "foot");
	private static final Map<String, int[]> DEFENSE = Map.of("pyromaniacs", new int[] {1, 2, 1, 1}, "pyromancers", new int[] {1, 3, 2, 1});
	/** Vanilla's per-slot base durability (11, 16, 15, 13) times each set's 7 and 10. */
	private static final Map<String, int[]> DURABILITY = Map.of("pyromaniacs", new int[] {77, 112, 105, 91},
			"pyromancers", new int[] {110, 160, 150, 130});
	/** Ticks to wait out vanilla's hurt immunity (a second hit within 10 ticks counts only for what it exceeds). */
	private static final int HURT_IMMUNITY_TICKS = 12;

	private static void floor(GameTestHelper helper) {
		for (int x = 0; x <= 7; x++) {
			for (int z = 0; z <= 7; z++) {
				helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
			}
		}
	}

	/** A survival player with the Initiate's Wand on {@code standAt}, facing south (+Z). */
	private static ServerPlayer player(GameTestHelper helper, BlockPos standAt) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		BlockPos absolute = helper.absolutePos(standAt);
		player.snapTo(absolute.getX() + 0.5, absolute.getY(), absolute.getZ() + 0.5, 0.0F, 0.0F);
		player.setYHeadRot(0.0F);
		player.setYBodyRot(0.0F);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JugcraftConcordance.INITIATE_WAND));
		RateGate.forget(player.getUUID());
		return player;
	}

	/** First Light understood, then Hearthbinding at {@code state}, and the spell bar refreshed. */
	private static void learn(ServerPlayer player, ResearchState state) {
		ConcordanceProgress.grant(player, FIRST_LIGHT, ResearchState.UNDERSTOOD);
		ConcordanceProgress.grant(player, HEARTHBINDING, state);
		SpellContainerSource.setDirty(player, ConcordanceSpells.SOURCE);
		SpellContainerSource.setDirty(player, "main_hand");
		SpellContainerSource.update(player);
	}

	private static void cast(ServerPlayer player, String id) {
		ServerLevel level = player.level();
		((SpellCaster.Player) player).getCooldownManager().reset(null);
		Holder<Spell> spell = SpellRegistry.from(level).get(Identifier.parse(id)).orElseThrow();
		SpellExecution.performSpell(level, player, spell, SpellTarget.SearchResult.empty(), SpellCast.Action.RELEASE, 1.0F);
	}

	private static Item item(String id) {
		return BuiltInRegistries.ITEM.getValue(Jugcraft.id(id));
	}

	/** A vanilla item by its id (as ArmorTiersGameTests looks them up: not every item has an Items constant in 26.3). */
	private static Item vanilla(String path) {
		return BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(path));
	}

	private static boolean in(ItemStack stack, String tag) {
		return stack.is(TagKey.create(Registries.ITEM, Identifier.parse(tag)));
	}

	/** Every spell_power:fire modifier on an item's own attribute modifiers, as "slot group=amount". */
	private static List<String> fireModifiers(ItemStack stack) {
		return stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY).modifiers().stream()
				.filter(entry -> entry.attribute().getRegisteredName().equals(Ember.SCHOOL))
				.map(entry -> entry.slot().getSerializedName() + "=" + entry.modifier().amount()).toList();
	}

	private static double amount(ItemStack stack, String attribute) {
		return stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY).modifiers().stream()
				.filter(entry -> entry.attribute().getRegisteredName().equals(attribute))
				.mapToDouble(entry -> entry.modifier().amount()).sum();
	}

	/** The wearer's own Trinkets slot {@code index} of slot type {@code type} (e.g. "chest/spell_focus"). */
	private static TrinketSlotAccess slot(ServerPlayer wearer, String type, int index) {
		return TrinketsApi.getAttachment(wearer).getInventory(type).getOrCreateSlotAccess(index);
	}

	/**
	 * What a piece of the regalia gives worn in a Trinkets slot ("attribute=amount@id"), asked as Trinkets asks it: with
	 * the wearer's own slot and the identifier Trinkets gives that slot.
	 */
	private static List<String> trinketModifiers(ItemStack stack, ServerPlayer wearer, String type, int index) {
		List<String> seen = new ArrayList<>();
		TrinketSlotAccess access = slot(wearer, type, index);
		((EmberTrinketItem) stack.getItem()).forEachTrinketModifier(stack, access, wearer, SlotAttributes.getIdentifier(access),
				(attribute, modifier) -> seen.add(attribute.getRegisteredName() + "=" + modifier.amount() + "@" + modifier.id()));
		return seen;
	}

	/** The eleven items: registered, one to a stack, fire resistant; the owner's two slots; each set's numbers and tags. */
	@GameTest(maxTicks = 20)
	public void theRegaliaIsMadeAndWornAsDesigned(GameTestHelper helper) {
		List<String> wrong = new ArrayList<>();
		Object netherite = new ItemStack(vanilla("netherite_helmet")).get(DataComponents.DAMAGE_RESISTANT);
		Map<String, Item> fields = Map.of("lesser_fire_focus", EmberGear.LESSER_FIRE_FOCUS, "fire_focus", EmberGear.FIRE_FOCUS,
				"fire_bangle", EmberGear.FIRE_BANGLE);
		for (String id : List.of("lesser_fire_focus", "fire_focus", "fire_bangle")) {
			ItemStack stack = new ItemStack(item(id));
			if (stack.isEmpty() || stack.getItem() != fields.get(id) || !(stack.getItem() instanceof EmberTrinketItem)) {
				wrong.add(id + " is not registered as EmberGear's trinket");
				continue;
			}
			if (stack.getMaxStackSize() != 1 || !netherite.equals(stack.get(DataComponents.DAMAGE_RESISTANT))) {
				wrong.add(id + " is not one to a stack and fire resistant");
			}
			if (!fireModifiers(stack).isEmpty()) {
				wrong.add(id + " carries fire Spell Power on the item itself (it would count in the hand): " + fireModifiers(stack));
			}
		}
		for (String id : List.of("lesser_fire_focus", "fire_focus")) {
			if (!in(new ItemStack(item(id)), "trinkets:chest/spell_focus") || in(new ItemStack(item(id)), "trinkets:chest/necklace")) {
				wrong.add(id + " is not worn in the Spell Focus slot only");
			}
		}
		if (!in(new ItemStack(EmberGear.FIRE_BANGLE), "trinkets:hand/bracelet") || in(new ItemStack(EmberGear.FIRE_BANGLE), "trinkets:hand/ring")) {
			wrong.add("the Fire Bangle is not worn in a Bracelet slot only");
		}
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		var focusSlot = TrinketsApi.getAttachment(player).getInventory("chest/spell_focus");
		var bracelets = TrinketsApi.getAttachment(player).getInventory("hand/bracelet");
		if (focusSlot == null || focusSlot.getContainerSize() != 1) {
			wrong.add("players do not have one Spell Focus slot: " + (focusSlot == null ? "none" : focusSlot.getContainerSize()));
		}
		if (bracelets == null || bracelets.getContainerSize() != 2) {
			wrong.add("players do not have two Bracelet slots: " + (bracelets == null ? "none" : bracelets.getContainerSize()));
		}
		if (TrinketsApi.getAttachment(player).getInventory("chest/necklace") == null) {
			wrong.add("players lost the necklace slot the Hearthstone is worn in");
		}
		for (String set : SETS) {
			TagKey<Item> repairs = TagKey.create(Registries.ITEM, Jugcraft.id("repairs_" + set + "_gear"));
			if (!new ItemStack(vanilla("white_wool")).is(repairs) || new ItemStack(Items.IRON_INGOT).is(repairs)) {
				wrong.add("#jugcraft:repairs_" + set + "_gear is not wool");
			}
			for (int p = 0; p < SLOTS.size(); p++) {
				String id = SET_PIECES.get(set).get(p);
				ItemStack stack = new ItemStack(item(id));
				if (stack.isEmpty() || !(stack.getItem() instanceof EmberArmorItem piece) || stack.getItem() != EmberGear.ARMOR.get(id)) {
					wrong.add(id + " is not registered as EmberGear's armour");
					continue;
				}
				if (!piece.set().equals(set) || !piece.model().equals(EmberGear.ARMOR_MODEL)) {
					wrong.add(id + " is worn as " + piece.set() + " on " + piece.model() + ", not " + set + " on " + EmberGear.ARMOR_MODEL);
				}
				var equippable = stack.get(DataComponents.EQUIPPABLE);
				if (equippable == null || equippable.slot() != SLOTS.get(p)
						|| !equippable.assetId().equals(Optional.of(ResourceKey.create(EquipmentAssets.ROOT_ID, Jugcraft.id(set))))) {
					wrong.add(id + " is not worn in " + SLOTS.get(p) + " as jugcraft:" + set + ": " + equippable);
				}
				int defense = DEFENSE.get(set)[p];
				if (amount(stack, "minecraft:armor") != defense || amount(stack, "minecraft:armor_toughness") != 0.0
						|| amount(stack, "minecraft:knockback_resistance") != 0.0) {
					wrong.add(id + " does not have its protection " + defense + ": " + amount(stack, "minecraft:armor"));
				}
				String group = EquipmentSlotGroup.bySlot(SLOTS.get(p)).getSerializedName();
				if (!fireModifiers(stack).equals(List.of(group + "=" + EmberGear.ROBE_PIECE_POWER))) {
					wrong.add(id + " does not give half a point of fire in its own slot: " + fireModifiers(stack));
				}
				if (stack.getMaxDamage() != DURABILITY.get(set)[p]) {
					wrong.add(id + " lasts " + stack.getMaxDamage() + ", not " + DURABILITY.get(set)[p]);
				}
				var enchantable = stack.get(DataComponents.ENCHANTABLE);
				if (enchantable == null || enchantable.value() != EmberGear.ARMOR_ENCHANTABILITY) {
					wrong.add(id + " is not enchantable at " + EmberGear.ARMOR_ENCHANTABILITY);
				}
				var repairableType = BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(Identifier.withDefaultNamespace("repairable"));
				Object repairable = repairableType == null ? null : stack.get(repairableType);
				if (repairable == null || !String.valueOf(repairable).contains("jugcraft:repairs_" + set + "_gear")) {
					wrong.add(id + " is not repaired with #jugcraft:repairs_" + set + "_gear: " + repairable);
				}
				if (!netherite.equals(stack.get(DataComponents.DAMAGE_RESISTANT))) {
					wrong.add(id + " does not resist fire");
				}
				for (String tag : List.of(SLOT_TAGS.get(p) + "_armor", "enchantable/armor", "enchantable/" + SLOT_TAGS.get(p) + "_armor",
						"enchantable/durability", "enchantable/equippable")) {
					if (!in(stack, "minecraft:" + tag)) {
						wrong.add(id + " is not in #minecraft:" + tag);
					}
				}
			}
		}
		helper.assertTrue(wrong.isEmpty(), "The regalia: " + String.join("; ", wrong));
		helper.succeed();
	}

	/** Worn, the foci give 2 and 4 fire Spell Power through Trinkets (one modifier for each slot); the bangle gives none. */
	@GameTest(maxTicks = 20)
	public void theFociGiveFireSpellPowerThroughTrinkets(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		ItemStack lesser = new ItemStack(EmberGear.LESSER_FIRE_FOCUS);
		ItemStack focus = new ItemStack(EmberGear.FIRE_FOCUS);
		List<String> lesserWorn = trinketModifiers(lesser, player, "chest/spell_focus", 0);
		List<String> focusWorn = trinketModifiers(focus, player, "chest/spell_focus", 0);
		helper.assertTrue(lesserWorn.size() == 1 && lesserWorn.getFirst().startsWith(Ember.SCHOOL + "=" + EmberGear.LESSER_FOCUS_POWER + "@"),
				"The Lesser Focus of Fire gives 2 fire Spell Power: " + lesserWorn);
		helper.assertTrue(focusWorn.size() == 1 && focusWorn.getFirst().startsWith(Ember.SCHOOL + "=" + EmberGear.FOCUS_POWER + "@"),
				"The Focus of Fire gives 4: " + focusWorn);
		helper.assertTrue(!trinketModifiers(focus, player, "chest/spell_focus", 1).equals(focusWorn),
				"Each slot's modifier has its own id, so two worn foci would not replace each other");
		helper.assertTrue(trinketModifiers(new ItemStack(EmberGear.FIRE_BANGLE), player, "hand/bracelet", 0).isEmpty(),
				"The Fire Bangle gives no Spell Power");
		helper.succeed();
	}

	/**
	 * The regalia at its most (a Focus of Fire and the whole Pyromancer's set) gives 6 fire Spell Power: Cinderbolt deals
	 * 6 for its usual 5 Focus. Three pieces of the set give 5.5, which adds nothing over the focus alone: Cinderbolt deals 5.
	 */
	@GameTest(maxTicks = 80)
	public void theRegaliaRaisesTheFireToItsCeiling(GameTestHelper helper) {
		floor(helper);
		ServerPlayer player = player(helper, new BlockPos(1, 2, 1));
		Mob target = helper.spawnWithNoFreeWill(EntityTypes.VILLAGER, new BlockPos(1, 2, 5));
		learn(player, ResearchState.UNDERSTOOD);
		AttributeInstance fire = player.getAttribute(SpellSchools.getSchool(Ember.SCHOOL).attributeEntry);
		ItemStack focus = new ItemStack(EmberGear.FIRE_FOCUS);
		TrinketSlotAccess worn = slot(player, "chest/spell_focus", 0);
		((EmberTrinketItem) focus.getItem()).forEachTrinketModifier(focus, worn, player, SlotAttributes.getIdentifier(worn),
				(attribute, modifier) -> fire.addTransientModifier(modifier));
		List<AttributeModifier> pieces = new ArrayList<>();
		for (String id : PIECES) {
			new ItemStack(item(id)).getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY).modifiers().stream()
					.filter(entry -> entry.attribute().getRegisteredName().equals(Ember.SCHOOL))
					.forEach(entry -> pieces.add(entry.modifier()));
		}
		pieces.forEach(fire::addTransientModifier);
		helper.assertTrue(pieces.size() == 4 && Invocations.powerAboveBase(player, Ember.SCHOOL) == 6.0,
				"A Focus of Fire and the whole set: 6 fire Spell Power above the base: " + Invocations.powerAboveBase(player, Ember.SCHOOL));
		float before = target.getHealth();
		cast(player, "jugcraft:cinderbolt");
		helper.assertTrue(before - target.getHealth() == 6.0F, "With it Cinderbolt deals 6: " + before + " -> " + target.getHealth());
		helper.assertTrue(ConcordanceProgress.currentFocus(player) == FocusPool.MAX - 5, "for its usual 5 Focus: "
				+ ConcordanceProgress.currentFocus(player));
		helper.runAfterDelay(2, () -> {
			target.removeEffect(Ember.SMOULDER);
			target.clearFire();
			fire.removeModifier(pieces.getLast().id());
			helper.assertTrue(Invocations.powerAboveBase(player, Ember.SCHOOL) == 5.5, "Three pieces and the focus: 5.5");
			helper.runAfterDelay(HURT_IMMUNITY_TICKS, () -> {
				float healthy = target.getHealth();
				ConcordanceProgress.setFocus(player, FocusPool.MAX);
				cast(player, "jugcraft:cinderbolt");
				helper.assertTrue(healthy - target.getHealth() == 5.0F,
						"Fewer than four pieces add nothing: Cinderbolt deals the focus's 5: " + healthy + " -> " + target.getHealth());
				helper.succeed();
			});
		});
	}

	/**
	 * The Fire Bangle's blow: a Hearthbinder's own melee hit leaves the creature smouldering for 3 seconds. Not before
	 * Hearthbinding is understood, not from a blow that is not the player's own melee hit, not on a creature claimed by
	 * someone else, not where the blow lands beyond the reach of the weapon in hand (as a Shock arc's second foe may), not
	 * from the saddle, and not once the bangle is taken off; and it costs no Focus.
	 */
	@GameTest(maxTicks = 40)
	public void theBangleLeavesAHearthbindersBlowSmouldering(GameTestHelper helper) {
		floor(helper);
		ServerLevel level = helper.getLevel();
		ServerPlayer player = player(helper, new BlockPos(3, 2, 1));
		// Within a bare hand's reach of the player (at most 2.8 blocks from the eye to the hitbox; the reach is 3).
		List<Mob> villagers = new ArrayList<>();
		for (int i = 0; i < 6; i++) {
			villagers.add(helper.spawnWithNoFreeWill(EntityTypes.VILLAGER, new BlockPos(1 + i, 2, 2)));
		}
		// Out of reach: about 6.8 blocks from the eye to the hitbox.
		Mob far = helper.spawnWithNoFreeWill(EntityTypes.VILLAGER, new BlockPos(7, 2, 7));
		var bracelets = TrinketsApi.getAttachment(player).getInventory("hand/bracelet");
		helper.assertTrue(bracelets != null, "Players have the Bracelet slots");
		bracelets.setItem(0, new ItemStack(EmberGear.FIRE_BANGLE));
		helper.assertTrue(EmberGear.wearsBangle(player), "The bangle in a Bracelet slot is worn");
		villagers.get(0).hurtServer(level, level.damageSources().playerAttack(player), 1.0F);
		helper.assertTrue(!villagers.get(0).hasEffect(Ember.SMOULDER), "Before Hearthbinding is understood, the blow is a blow");
		learn(player, ResearchState.UNDERSTOOD);
		int focus = ConcordanceProgress.currentFocus(player);
		villagers.get(1).hurtServer(level, level.damageSources().playerAttack(player), 1.0F);
		MobEffectInstance smoulder = villagers.get(1).getEffect(Ember.SMOULDER);
		helper.assertTrue(smoulder != null && smoulder.getDuration() > 50 && smoulder.getDuration() <= EmberGear.BANGLE_SMOULDER_TICKS,
				"A Hearthbinder's blow leaves the creature smouldering for 3 seconds: " + smoulder);
		helper.assertTrue(ConcordanceProgress.currentFocus(player) == focus, "and costs no Focus");
		villagers.get(2).hurtServer(level, level.damageSources().mobAttack(player), 1.0F);
		helper.assertTrue(!villagers.get(2).hasEffect(Ember.SMOULDER), "A blow that is not the player's own melee hit does not burn");
		try (TestClaims claims = TestClaims.open()) {
			claims.creature(villagers.get(3), NEIGHBOUR);
			villagers.get(3).hurtServer(level, level.damageSources().playerAttack(player), 1.0F);
			helper.assertTrue(!villagers.get(3).hasEffect(Ember.SMOULDER), "A neighbour's claimed creature is not set smouldering");
		}
		far.hurtServer(level, level.damageSources().playerAttack(player), 1.0F);
		helper.assertTrue(far.getHealth() < far.getMaxHealth() && !far.hasEffect(Ember.SMOULDER),
				"A blow landing beyond the reach of the weapon in hand (a Shock arc's second foe) does not burn");
		Mob mount = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(3, 2, 0));
		helper.assertTrue(player.startRiding(mount, true, true), "The player could not mount");
		villagers.get(4).hurtServer(level, level.damageSources().playerAttack(player), 1.0F);
		helper.assertTrue(!villagers.get(4).hasEffect(Ember.SMOULDER), "From the saddle the blow does not burn");
		player.stopRiding();
		bracelets.setItem(0, ItemStack.EMPTY);
		villagers.get(5).hurtServer(level, level.damageSources().playerAttack(player), 1.0F);
		helper.assertTrue(!EmberGear.wearsBangle(player) && !villagers.get(5).hasEffect(Ember.SMOULDER),
				"With the bangle taken off, the blow does not burn");
		helper.succeed();
	}

	/** Spell Power's attribute enchantments (Sunfire) are refused on both fire sets; ordinary armour enchantments are not. */
	@GameTest(maxTicks = 20)
	public void spellPowerEnchantmentsAreRefusedOnTheSet(GameTestHelper helper) {
		var enchantments = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
		Holder<Enchantment> sunfire = enchantments.getOrThrow(ResourceKey.create(Registries.ENCHANTMENT, Identifier.parse("spell_power:sunfire")));
		Holder<Enchantment> protection = enchantments.getOrThrow(ResourceKey.create(Registries.ENCHANTMENT, Identifier.withDefaultNamespace("protection")));
		for (String id : Stream.concat(LIGHT.stream(), PIECES.stream()).toList()) {
			ItemStack piece = new ItemStack(item(id));
			for (EnchantingContext context : EnchantingContext.values()) {
				helper.assertTrue(!piece.canBeEnchantedWith(sunfire, context), id + " takes Sunfire (" + context + ")");
			}
			helper.assertTrue(piece.canBeEnchantedWith(protection, EnchantingContext.ACCEPTABLE), id + " refuses Protection");
		}
		helper.succeed();
	}
}
