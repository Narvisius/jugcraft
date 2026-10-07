package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.mixin.AttackStrengthAccessor;
import io.github.jimbozoomer.jugcraft.weapons.ArmBowItem;
import io.github.jimbozoomer.jugcraft.weapons.ArmCrossbowItem;
import io.github.jimbozoomer.jugcraft.weapons.JugcraftArms;
import io.github.jimbozoomer.jugcraft.weapons.WeaponArts;
import java.lang.reflect.Field;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * In-game tests for Arms VI (batch 55): the katana's seven cuts and the brazier mace's fire as the server works them,
 * the longbow's and arbalest's shots (their speed and the arrows' base damage, read back from the arrows they loose) and
 * the shields' blocking (how soon each is raised, how far round it covers, and the tower shield's brace and weight).
 * Wielders are mock players facing south (+z) with a full attack charge; foes and shield-bearers are still husks with
 * no armor, which tick their own item use as a player does.
 */
public class ArmsVIGameTests {
	private static final String ARENA = "jugcraft-test:arms_arena";
	private static final int CHARGED = 100;
	private static final float HEALTH = 20.0F;

	/** Seven cuts: each foe ahead and in reach takes seven cuts, none before the first is due; one behind and one beyond reach none. */
	@GameTest(structure = ARENA, maxTicks = 60, skyAccess = true)
	public void sevenCutsStrikeEveryFoeAheadSevenTimes(GameTestHelper helper) {
		floor(helper);
		ServerPlayer player = wielder(helper, "steel_katana", new BlockPos(8, 2, 4));
		List<Mob> ahead = List.of(husk(helper, new BlockPos(8, 2, 6)), husk(helper, new BlockPos(9, 2, 6)));
		Mob behind = husk(helper, new BlockPos(8, 2, 2));
		Mob beyond = husk(helper, new BlockPos(8, 2, 10));
		float base = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
		ItemStack stack = player.getMainHandItem();
		helper.assertTrue(WeaponArts.start(player, stack), "Seven cuts did not start");
		helper.assertTrue(WeaponArts.busy(player) && player.getCooldowns().isOnCooldown(stack), "Seven cuts did not keep the katana busy and cooling");
		helper.runAfterDelay(JugcraftArms.CUTS_FIRST - 1, () -> helper.assertTrue(ahead.stream().allMatch(foe -> foe.getHealth() == HEALTH),
				"A cut landed before the first was due"));
		int last = JugcraftArms.CUTS_FIRST + (JugcraftArms.CUTS_COUNT - 1) * JugcraftArms.CUTS_EVERY;
		helper.runAfterDelay(last - 1, () -> {
			float six = base * JugcraftArms.CUTS_SHARE * (JugcraftArms.CUTS_COUNT - 1);
			helper.assertTrue(ahead.stream().allMatch(foe -> near(HEALTH - foe.getHealth(), six)),
					"Before the last cut the foes ahead had taken " + ahead.stream().map(foe -> HEALTH - foe.getHealth()).toList() + ", not " + six);
		});
		helper.runAfterDelay(JugcraftArms.ARTS.get("katana").ticks() + 2, () -> {
			float expected = base * JugcraftArms.CUTS_SHARE * JugcraftArms.CUTS_COUNT;
			Jugcraft.LOGGER.info("[arms vi] seven cuts of {} each: ahead {}, behind {}, beyond {}", base * JugcraftArms.CUTS_SHARE,
					ahead.stream().map(Mob::getHealth).toList(), behind.getHealth(), beyond.getHealth());
			for (Mob foe : ahead) {
				helper.assertTrue(near(HEALTH - foe.getHealth(), expected), "A foe ahead took " + (HEALTH - foe.getHealth()) + ", not " + expected);
			}
			helper.assertTrue(behind.getHealth() == HEALTH && beyond.getHealth() == HEALTH, "The cuts reached behind or beyond the katana's reach");
			helper.assertTrue(!WeaponArts.busy(player), "Seven cuts never ended");
			helper.succeed();
		});
	}

	/**
	 * The brazier mace: a hit sets the foe alight for IGNITE_SECONDS; used on an unlit campfire it lights it, and on stone
	 * it sets fire on the face used, each at IGNITE_WEAR.
	 */
	@GameTest
	public void aBrazierMaceSetsFoesAlightAndLightsBlocks(GameTestHelper helper) {
		floor(helper, 7);
		ServerPlayer player = wielder(helper, "bronze_brazier_mace", new BlockPos(1, 2, 1));
		ItemStack mace = player.getMainHandItem();
		Mob foe = husk(helper, new BlockPos(1, 2, 3));
		mace.getItem().hurtEnemy(mace, foe, player);
		int burning = foe.getRemainingFireTicks();
		BlockPos campfire = new BlockPos(4, 2, 2);
		helper.setBlock(campfire, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
		BlockPos campfireAt = helper.absolutePos(campfire);
		InteractionResult lit = player.gameMode.useItemOn(player, helper.getLevel(), mace, InteractionHand.MAIN_HAND,
				new BlockHitResult(Vec3.atCenterOf(campfireAt), Direction.UP, campfireAt, false));
		BlockPos stone = new BlockPos(6, 1, 5);
		BlockPos stoneAt = helper.absolutePos(stone);
		InteractionResult fire = player.gameMode.useItemOn(player, helper.getLevel(), mace, InteractionHand.MAIN_HAND,
				new BlockHitResult(Vec3.atCenterOf(stoneAt).relative(Direction.UP, 0.5), Direction.UP, stoneAt, false));
		BlockState campfireState = helper.getBlockState(campfire);
		BlockState above = helper.getBlockState(stone.above());
		Jugcraft.LOGGER.info("[arms vi] brazier mace: foe burning {} ticks; campfire {} -> {}; stone {} -> {}; wear {}", burning, lit,
				campfireState, fire, above, mace.getDamageValue());
		helper.assertTrue(burning == JugcraftArms.IGNITE_SECONDS * 20, "The brazier mace's hit left the foe burning " + burning + " ticks");
		helper.assertTrue(lit.consumesAction() && campfireState.getValue(CampfireBlock.LIT), "The brazier mace did not light the campfire");
		helper.assertTrue(fire.consumesAction() && above.is(Blocks.FIRE), "The brazier mace did not set fire on the stone: " + above);
		helper.assertTrue(mace.getDamageValue() == 2 * JugcraftArms.IGNITE_WEAR, "Lighting wore the mace by " + mace.getDamageValue());
		helper.succeed();
	}

	/**
	 * A longbow at vanilla's full draw (20 ticks) is not yet fully drawn; at its own draw it is, and its arrow leaves at
	 * the longbow's speed with its base damage, where vanilla's bow looses at 3.0.
	 */
	@GameTest
	public void aLongbowDrawsLongerAndLoosesFaster(GameTestHelper helper) {
		floor(helper, 7);
		for (String id : List.of("bronze_longbow", "steel_longbow")) {
			ArmBowItem item = (ArmBowItem) JugcraftArms.KIT.get(id);
			JugcraftArms.Ranged ranged = item.ranged();
			float atTwenty = ArmBowItem.power(BowItem.MAX_DRAW_DURATION, ranged.draw());
			helper.assertTrue(atTwenty < 1.0F && ArmBowItem.power(ranged.draw(), ranged.draw()) == 1.0F
					&& BowItem.getPowerForTime(BowItem.MAX_DRAW_DURATION) == 1.0F, id + " is drawn " + atTwenty + " at a bow's full draw");
		}
		AbstractArrow longbow = loose(helper, "steel_longbow", new BlockPos(1, 2, 1));
		AbstractArrow bow = looseVanilla(helper, new BlockPos(5, 2, 1));
		JugcraftArms.Ranged steel = ((ArmBowItem) JugcraftArms.KIT.get("steel_longbow")).ranged();
		double speed = longbow.getDeltaMovement().length();
		double vanilla = bow.getDeltaMovement().length();
		double damage = baseDamage(longbow);
		Jugcraft.LOGGER.info("[arms vi] steel longbow: arrow at {} blocks a tick, base damage {}, crit {}; vanilla bow's at {}", speed, damage,
				longbow.isCritArrow(), vanilla);
		helper.assertTrue(Math.abs(speed - steel.speed()) < 0.1, "The longbow's arrow left at " + speed + ", not " + steel.speed());
		helper.assertTrue(Math.abs(vanilla - 3.0) < 0.1 && speed > vanilla, "Vanilla's bow loosed at " + vanilla);
		helper.assertTrue(Math.abs(damage - steel.damage()) < 1.0E-4 && longbow.isCritArrow(), "The longbow's arrow has base damage " + damage);
		helper.succeed();
	}

	/** An arbalest's bolt leaves faster than a crossbow's (3.15) with the arbalest's base damage; the crossbow's has vanilla's 2.0. */
	@GameTest
	public void anArbalestShootsFasterAndHarder(GameTestHelper helper) {
		floor(helper, 7);
		ArmCrossbowItem item = (ArmCrossbowItem) JugcraftArms.KIT.get("steel_arbalest");
		AbstractArrow bolt = fire(helper, new ItemStack(item), new BlockPos(1, 2, 1));
		AbstractArrow vanilla = fire(helper, new ItemStack(Items.CROSSBOW), new BlockPos(5, 2, 1));
		double speed = bolt.getDeltaMovement().length();
		double damage = baseDamage(bolt);
		Jugcraft.LOGGER.info("[arms vi] steel arbalest: bolt at {} blocks a tick, base damage {}; vanilla crossbow's at {}, base damage {}",
				speed, damage, vanilla.getDeltaMovement().length(), baseDamage(vanilla));
		helper.assertTrue(Math.abs(speed - item.ranged().speed()) < 0.1, "The arbalest's bolt left at " + speed + ", not " + item.ranged().speed());
		helper.assertTrue(Math.abs(vanilla.getDeltaMovement().length() - JugcraftArms.CROSSBOW_SPEED) < 0.1,
				"Vanilla's crossbow shot at " + vanilla.getDeltaMovement().length());
		helper.assertTrue(Math.abs(damage - item.ranged().damage()) < 1.0E-4 && Math.abs(baseDamage(vanilla) - ArmCrossbowItem.VANILLA_DAMAGE) < 1.0E-4,
				"The arbalest's bolt has base damage " + damage);
		helper.succeed();
	}

	/**
	 * Shields carry their numbers, and block by them: a heater shield is up within its short delay while a tower shield is
	 * not yet; once both are up, a blow from well round to the side (108 degrees off ahead) is stopped by the tower shield's
	 * wider cover but not the heater's. The tower shield braces its bearer and weighs on them.
	 */
	@GameTest(structure = ARENA, maxTicks = 40, skyAccess = true)
	public void shieldsBlockByTheirNumbers(GameTestHelper helper) {
		floor(helper);
		for (JugcraftArms.Shield shield : JugcraftArms.SHIELDS) {
			ItemStack stack = new ItemStack(JugcraftArms.KIT.get(shield.metal() + "_" + shield.name()));
			BlocksAttacks blocks = stack.get(DataComponents.BLOCKS_ATTACKS);
			helper.assertTrue(blocks != null && blocks.blockDelaySeconds() == shield.delay() && blocks.disableCooldownScale() == shield.disable()
					&& blocks.damageReductions().getFirst().horizontalBlockingAngle() == shield.angle(), stack + " blocks as " + blocks);
			helper.assertTrue(stack.getMaxDamage() == shield.durability(), stack + " lasts " + stack.getMaxDamage());
		}
		Mob tower = bearer(helper, "steel_tower_shield", new BlockPos(3, 2, 8));
		Mob heater = bearer(helper, "steel_heater_shield", new BlockPos(11, 2, 8));
		Mob towerFoe = husk(helper, new BlockPos(6, 2, 7));
		Mob heaterFoe = husk(helper, new BlockPos(14, 2, 7));
		JugcraftArms.Shield steelTower = JugcraftArms.SHIELDS.stream().filter(s -> s.name().equals("tower_shield") && s.metal().equals("steel"))
				.findFirst().orElseThrow();
		tower.startUsingItem(InteractionHand.MAIN_HAND);
		heater.startUsingItem(InteractionHand.MAIN_HAND);
		helper.runAfterDelay(4, () -> helper.assertTrue(heater.isBlocking() && !tower.isBlocking(),
				"After 4 ticks the heater shield is " + (heater.isBlocking() ? "up" : "down") + ", the tower shield " + (tower.isBlocking() ? "up" : "down")));
		helper.runAfterDelay(12, () -> {
			ServerLevel level = helper.getLevel();
			helper.assertTrue(heater.isBlocking() && tower.isBlocking(), "The shields are not both up after 12 ticks");
			// A held item's attribute modifiers are put on its holder as it ticks, so they are read here, not at once.
			AttributeModifier brace = tower.getAttribute(Attributes.KNOCKBACK_RESISTANCE).getModifier(Jugcraft.id("shield_brace"));
			AttributeModifier weight = tower.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(Jugcraft.id("shield_weight"));
			helper.assertTrue(brace != null && near((float) brace.amount(), steelTower.brace()) && weight != null
					&& near((float) weight.amount(), -steelTower.weight()), "The tower shield's brace " + brace + " or weight " + weight + " is wrong");
			helper.assertTrue(heater.getAttribute(Attributes.KNOCKBACK_RESISTANCE).getModifier(Jugcraft.id("shield_brace")) == null,
					"The heater shield braces its bearer");
			tower.hurtServer(level, level.damageSources().mobAttack(towerFoe), 10.0F);
			heater.hurtServer(level, level.damageSources().mobAttack(heaterFoe), 10.0F);
			Jugcraft.LOGGER.info("[arms vi] a blow of 10 from 108 degrees off ahead leaves the tower shield's bearer {}, the heater's {}",
					tower.getHealth(), heater.getHealth());
			helper.assertTrue(tower.getHealth() == HEALTH, "The tower shield let through " + (HEALTH - tower.getHealth()));
			helper.assertTrue(near(heater.getHealth(), HEALTH - 10.0F), "The heater shield stopped a blow from its flank: " + heater.getHealth());
			helper.succeed();
		});
	}

	/** Looses a fully drawn arrow from an arm's longbow held by a fresh mock player at pos; returns the arrow. */
	private static AbstractArrow loose(GameTestHelper helper, String id, BlockPos pos) {
		ServerPlayer player = archer(helper, new ItemStack(JugcraftArms.KIT.get(id)), pos);
		ArmBowItem item = (ArmBowItem) JugcraftArms.KIT.get(id);
		ItemStack bow = player.getMainHandItem();
		item.releaseUsing(bow, helper.getLevel(), player, bow.getUseDuration(player) - item.ranged().draw());
		return arrow(helper, player);
	}

	/** Looses a fully drawn arrow from vanilla's bow, for comparison. */
	private static AbstractArrow looseVanilla(GameTestHelper helper, BlockPos pos) {
		ServerPlayer player = archer(helper, new ItemStack(Items.BOW), pos);
		ItemStack bow = player.getMainHandItem();
		bow.getItem().releaseUsing(bow, helper.getLevel(), player, bow.getUseDuration(player) - BowItem.MAX_DRAW_DURATION);
		return arrow(helper, player);
	}

	/** Loads a crossbow (or arbalest) with an arrow and fires it as vanilla does on use; returns the arrow. */
	private static AbstractArrow fire(GameTestHelper helper, ItemStack crossbow, BlockPos pos) {
		ServerPlayer player = archer(helper, crossbow, pos);
		ItemStack held = player.getMainHandItem();
		held.set(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.ofNonEmpty(List.of(new ItemStack(Items.ARROW))));
		Item item = held.getItem();
		((net.minecraft.world.item.CrossbowItem) item).performShooting(helper.getLevel(), player, InteractionHand.MAIN_HAND, held,
				JugcraftArms.CROSSBOW_SPEED, 1.0F, null);
		return arrow(helper, player);
	}

	private static ServerPlayer archer(GameTestHelper helper, ItemStack weapon, BlockPos pos) {
		ServerPlayer player = wielder(helper, null, pos);
		player.setItemInHand(InteractionHand.MAIN_HAND, weapon);
		player.getInventory().add(new ItemStack(Items.ARROW, 16));
		return player;
	}

	/** The arrow nearest the player, just loosed. */
	private static AbstractArrow arrow(GameTestHelper helper, ServerPlayer player) {
		List<AbstractArrow> arrows = helper.getLevel().getEntitiesOfClass(AbstractArrow.class, new AABB(player.blockPosition()).inflate(2.0),
				arrow -> arrow.getOwner() == player);
		helper.assertTrue(arrows.size() == 1, "Expected one arrow from the shot, found " + arrows.size());
		return arrows.getFirst();
	}

	/** An arrow's base damage (AbstractArrow keeps it private; the game multiplies it by the arrow's speed on a hit). */
	private static double baseDamage(AbstractArrow arrow) {
		try {
			Field field = AbstractArrow.class.getDeclaredField("baseDamage");
			field.setAccessible(true);
			return field.getDouble(arrow);
		} catch (ReflectiveOperationException exception) {
			throw new IllegalStateException("Cannot read an arrow's base damage", exception);
		}
	}

	/** A mock player facing south (+z) standing at pos, in survival, the arm (if any) in hand and a full attack charge. */
	private static ServerPlayer wielder(GameTestHelper helper, String arm, BlockPos pos) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		BlockPos at = helper.absolutePos(pos);
		player.setPos(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
		player.setYRot(0.0F);
		player.setXRot(0.0F);
		player.setYHeadRot(0.0F);
		if (arm != null) {
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JugcraftArms.ITEMS.get(arm)));
		}
		((AttackStrengthAccessor) player).jugcraft$setAttackStrengthTicker(CHARGED);
		return player;
	}

	/** A still husk facing south, holding a shield of the kit in its main hand. */
	private static Mob bearer(GameTestHelper helper, String shield, BlockPos pos) {
		Mob husk = husk(helper, pos);
		husk.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(JugcraftArms.KIT.get(shield)));
		husk.setYRot(0.0F);
		husk.setYHeadRot(0.0F);
		husk.setYBodyRot(0.0F);
		return husk;
	}

	private static boolean near(float a, float b) {
		return Math.abs(a - b) < 1.0E-3F;
	}

	private static void floor(GameTestHelper helper) {
		floor(helper, 15);
	}

	private static void floor(GameTestHelper helper, int size) {
		for (int x = 0; x <= size; x++) {
			for (int z = 0; z <= size; z++) {
				helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
			}
		}
	}

	/** A still husk with no armor. */
	private static Mob husk(GameTestHelper helper, BlockPos pos) {
		@SuppressWarnings("unchecked")
		EntityType<Mob> type = (EntityType<Mob>) BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace("husk"));
		Mob husk = helper.spawnWithNoFreeWill(type, pos);
		husk.getAttribute(Attributes.ARMOR).setBaseValue(0.0);
		return husk;
	}
}
