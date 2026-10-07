package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.gear.JugcraftGear;
import io.github.jimbozoomer.jugcraft.weapons.ArmItem;
import io.github.jimbozoomer.jugcraft.weapons.JugcraftArms;
import java.util.Locale;
import java.util.Map;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.AttackRange;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.item.component.Weapon;
import net.minecraft.world.level.block.Blocks;

/**
 * In-game tests for the arms (batch 42): every arm carries its kind's components as the game sees them once registries
 * load (the parry's blocking is filled in only then), a longsword's parry stops part of a blow from in front but nothing
 * from behind or a fall, and a war hammer or flanged mace breaks a shield's guard where a sword does not. Husks stand in
 * for the fighters: they tick their own item use, as a player does, without a client, and do not burn by day.
 */
public class ArmsGameTests {
	private static final TagKey<Item> SPEARS = TagKey.create(Registries.ITEM, Identifier.withDefaultNamespace("spears"));

	@GameTest
	public void armsCarryTheirKindsComponents(GameTestHelper helper) {
		helper.assertTrue(JugcraftArms.ITEMS.size() == JugcraftArms.METALS.size() * (JugcraftArms.KINDS.size() + 2),
				"There are " + JugcraftArms.ITEMS.size() + " arms");
		for (Map.Entry<String, Item> entry : JugcraftArms.ITEMS.entrySet()) {
			String id = entry.getKey();
			ItemStack stack = new ItemStack(entry.getValue());
			String kind = ((ArmItem) entry.getValue()).kind();
			AttackRange range = stack.get(DataComponents.ATTACK_RANGE);
			SwingAnimation swing = stack.get(DataComponents.ATTACK_ANIMATION);
			Weapon weapon = stack.get(DataComponents.WEAPON);
			helper.assertTrue(range != null && swing != null && weapon != null, id + " lacks its range, swing or weapon");
			helper.assertTrue(stack.isDamageableItem(), id + " does not wear");
			Jugcraft.LOGGER.info(String.format(Locale.ROOT, "[arms] %s: hits %.1f, %.2f attacks/s, reach %.2f-%.2f, swing %s %d ticks, "
					+ "shield break %.1f s, parry %s, pierce %s, charge %s, sword %s", id, 1.0 + attack(stack), 4.0 + speed(stack),
					range.minReach(), range.maxReach(), swing.type().getSerializedName(), swing.duration(), weapon.disableBlockingForSeconds(),
					stack.has(DataComponents.BLOCKS_ATTACKS), stack.has(DataComponents.PIERCING_WEAPON), stack.has(DataComponents.KINETIC_WEAPON),
					stack.is(ItemTags.SWORDS)));
			if (kind.equals("spear") || kind.equals("lance")) {
				helper.assertTrue(stack.has(DataComponents.KINETIC_WEAPON) && stack.has(DataComponents.PIERCING_WEAPON),
						id + " does not charge and thrust like a spear");
				helper.assertTrue(stack.is(SPEARS), id + " is not in #minecraft:spears");
				float reach = kind.equals("lance") ? JugcraftArms.LANCE_MAX_REACH : 4.5F;
				helper.assertTrue(range.maxReach() == reach, id + " reaches " + range.maxReach() + ", not " + reach);
				continue;
			}
			JugcraftArms.Kind traits = JugcraftArms.KINDS.stream().filter(each -> each.name().equals(kind)).findFirst().orElseThrow();
			helper.assertTrue(range.minReach() == traits.minReach() && range.maxReach() == traits.maxReach(),
					id + " reaches " + range.minReach() + "-" + range.maxReach());
			helper.assertTrue(swing.type() == traits.swing() && swing.duration() == traits.swingTicks(), id + " swings " + swing);
			helper.assertTrue(weapon.disableBlockingForSeconds() == traits.disable(), id + " breaks a guard for "
					+ weapon.disableBlockingForSeconds() + " s");
			helper.assertTrue(stack.has(DataComponents.BLOCKS_ATTACKS) == traits.parry() > 0.0F, id + " parries wrongly");
			helper.assertTrue(stack.has(DataComponents.PIERCING_WEAPON) == traits.pierce(), id + " pierces wrongly");
			helper.assertTrue(stack.is(ItemTags.SWORDS) == traits.sword(), id + " is wrongly in or out of #minecraft:swords");
			float bonus = id.startsWith("bronze_") ? 2.0F : 2.5F;
			helper.assertTrue(Math.abs(attack(stack) - (traits.damage() + bonus)) < 1.0E-4, id + " hits for " + attack(stack));
		}
		helper.succeed();
	}

	/** A parrying longsword stops 60% of a blow from in front, and none of one from behind or of a fall. */
	@GameTest
	public void aLongswordParriesPartOfABlowFromTheFront(GameTestHelper helper) {
		ItemStack longsword = new ItemStack(JugcraftArms.ITEMS.get("steel_longsword"));
		BlocksAttacks parry = longsword.get(DataComponents.BLOCKS_ATTACKS);
		helper.assertTrue(parry != null && parry.bypassedBy().isPresent(), "The longsword's parry is missing or blocks everything");
		floor(helper);
		Mob front = fighter(helper, new BlockPos(1, 2, 1), longsword.copy(), 0.0F);
		Mob back = fighter(helper, new BlockPos(4, 2, 1), longsword.copy(), 180.0F);
		Mob fall = fighter(helper, new BlockPos(7, 2, 1), longsword.copy(), 0.0F);
		Mob attacker = fighter(helper, new BlockPos(1, 2, 3), ItemStack.EMPTY, 180.0F);
		Mob attacker2 = fighter(helper, new BlockPos(4, 2, 3), ItemStack.EMPTY, 180.0F);
		for (Mob husk : new Mob[] {front, back, fall}) {
			husk.startUsingItem(InteractionHand.MAIN_HAND);
		}
		helper.runAfterDelay(6, () -> {
			ServerLevel level = helper.getLevel();
			helper.assertTrue(front.isBlocking() && back.isBlocking(), "The husks are not parrying after 6 ticks");
			front.hurtServer(level, level.damageSources().mobAttack(attacker), 10.0F);
			back.hurtServer(level, level.damageSources().mobAttack(attacker2), 10.0F);
			fall.hurtServer(level, level.damageSources().fall(), 10.0F);
			Jugcraft.LOGGER.info("[arms] parry: 10 from the front leaves {}, from behind {}, a fall {}", front.getHealth(),
					back.getHealth(), fall.getHealth());
			helper.assertTrue(Math.abs(front.getHealth() - 16.0F) < 0.01F, "A parried blow of 10 left " + front.getHealth() + " of 20");
			helper.assertTrue(Math.abs(back.getHealth() - 10.0F) < 0.01F, "A blow from behind left " + back.getHealth() + " of 20");
			helper.assertTrue(Math.abs(fall.getHealth() - 10.0F) < 0.01F, "A fall of 10 left " + fall.getHealth() + " of 20");
			helper.succeed();
		});
	}

	/**
	 * A war hammer's hit stops a shield blocking for 5 seconds, as an axe's does; a bronze sword's does not. This is the
	 * number vanilla asks the attacker for when a player blocks its hit (mobs' shields are only knocked back).
	 */
	@GameTest
	public void aWarHammerBreaksAShieldsGuard(GameTestHelper helper) {
		floor(helper);
		Mob hammerer = fighter(helper, new BlockPos(1, 2, 1), new ItemStack(JugcraftArms.ITEMS.get("bronze_war_hammer")), 0.0F);
		Mob maceman = fighter(helper, new BlockPos(3, 2, 1), new ItemStack(JugcraftArms.ITEMS.get("steel_flanged_mace")), 0.0F);
		Mob swordsman = fighter(helper, new BlockPos(5, 2, 1), new ItemStack(JugcraftGear.ITEMS.get("bronze_sword")), 0.0F);
		Jugcraft.LOGGER.info("[arms] a hit stops a shield blocking for: war hammer {} s, flanged mace {} s, sword {} s",
				hammerer.getSecondsToDisableBlocking(), maceman.getSecondsToDisableBlocking(), swordsman.getSecondsToDisableBlocking());
		helper.assertTrue(hammerer.getSecondsToDisableBlocking() == 5.0F, "The war hammer breaks a guard for "
				+ hammerer.getSecondsToDisableBlocking() + " s");
		helper.assertTrue(maceman.getSecondsToDisableBlocking() == 3.0F, "The flanged mace breaks a guard for "
				+ maceman.getSecondsToDisableBlocking() + " s");
		helper.assertTrue(swordsman.getSecondsToDisableBlocking() == 0.0F, "A bronze sword breaks a guard");
		helper.succeed();
	}

	private static void floor(GameTestHelper helper) {
		for (int x = 0; x <= 7; x++) {
			for (int z = 0; z <= 7; z++) {
				helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
			}
		}
	}

	/** A still husk (it does not burn by day) with no armor, facing south (yaw 0) or north (180), holding the stack. */
	private static Mob fighter(GameTestHelper helper, BlockPos pos, ItemStack held, float yaw) {
		@SuppressWarnings("unchecked")
		EntityType<Mob> type = (EntityType<Mob>) BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace("husk"));
		Mob husk = helper.spawnWithNoFreeWill(type, pos);
		husk.getAttribute(Attributes.ARMOR).setBaseValue(0.0);
		husk.setItemSlot(EquipmentSlot.MAINHAND, held);
		husk.setYRot(yaw);
		husk.setYHeadRot(yaw);
		husk.setYBodyRot(yaw);
		return husk;
	}

	private static double attack(ItemStack stack) {
		return modifier(stack, Item.BASE_ATTACK_DAMAGE_ID);
	}

	private static double speed(ItemStack stack) {
		return modifier(stack, Item.BASE_ATTACK_SPEED_ID);
	}

	private static double modifier(ItemStack stack, Identifier id) {
		return stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, net.minecraft.world.item.component.ItemAttributeModifiers.EMPTY)
				.modifiers().stream().filter(entry -> entry.modifier().id().equals(id)).mapToDouble(entry -> entry.modifier().amount())
				.sum();
	}
}
