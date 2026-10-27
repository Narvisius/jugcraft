package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.creatures.scary.ScaryMod;
import io.github.jimbozoomer.jugcraft.creatures.scary.UnmaskedCrook;
import io.github.jimbozoomer.jugcraft.creatures.scary.Unmasking;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

/**
 * Unmasking (docs/features/costumed-villains-unmasking.md): each costumed villain a player beats reveals a named crook
 * and drops its mask with the crook's name on it; a villain that dies any other way reveals nobody; the crook cannot be
 * hurt and is led away after {@link UnmaskedCrook#CAUGHT_TICKS} ticks.
 */
public class UnmaskingGameTests {
	private static final BlockPos AT = new BlockPos(8, 1, 8);

	private static void floor(GameTestHelper helper) {
		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
			}
		}
	}

	private static List<UnmaskedCrook> crooks(GameTestHelper helper) {
		return helper.getLevel().getEntitiesOfClass(UnmaskedCrook.class, new AABB(helper.absolutePos(AT)).inflate(16), UnmaskedCrook::isAlive);
	}

	private static void beatenByAPlayer(GameTestHelper helper, EntityType<? extends Mob> type, String key) {
		floor(helper);
		ServerLevel level = helper.getLevel();
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		Mob villain = helper.spawnWithNoFreeWill(type, AT);
		villain.hurtServer(level, level.damageSources().playerAttack(player), 10_000.0F);
		helper.succeedWhen(() -> {
			helper.assertTrue(!villain.isAlive(), "The villain should be beaten");
			List<UnmaskedCrook> crooks = crooks(helper);
			helper.assertTrue(crooks.size() == 1, "One crook should stand where it fell, not " + crooks.size());
			helper.assertTrue(crooks.getFirst().hasCustomName(), "The crook should be named");
			Item mask = Unmasking.mask(key);
			helper.assertTrue(mask != null, "The villain should have a mask");
			List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(AT)).inflate(4),
					drop -> drop.getItem().is(mask));
			helper.assertTrue(drops.size() == 1, "Its mask should drop once, not " + drops.size() + " times");
			helper.assertTrue(drops.getFirst().getItem().has(DataComponents.LORE), "The mask should name who wore it");
		});
	}

	@GameTest(structure = "jugcraft-test:drone_depot", maxTicks = 100)
	public void theSpectralCosmonautIsUnmasked(GameTestHelper helper) {
		beatenByAPlayer(helper, ScaryMod.SPACE_KOOK, "space_kook");
	}

	@GameTest(structure = "jugcraft-test:drone_depot", maxTicks = 100)
	public void theDrownedDiverIsUnmasked(GameTestHelper helper) {
		beatenByAPlayer(helper, ScaryMod.CAPTAIN_CUTLER, "captain_cutler");
	}

	@GameTest(structure = "jugcraft-test:drone_depot", maxTicks = 100)
	public void theEmeraldPhantomIsUnmasked(GameTestHelper helper) {
		beatenByAPlayer(helper, ScaryMod.PHANTOM_SHADOW, "phantom_shadow");
	}

	@GameTest(structure = "jugcraft-test:drone_depot", maxTicks = 100)
	public void theOnyxWardenIsUnmasked(GameTestHelper helper) {
		beatenByAPlayer(helper, ScaryMod.BLACK_KNIGHT, "black_knight");
	}

	@GameTest(structure = "jugcraft-test:drone_depot", maxTicks = 100)
	public void aVillainThatDiesOtherwiseKeepsItsSecret(GameTestHelper helper) {
		floor(helper);
		ServerLevel level = helper.getLevel();
		Mob villain = helper.spawnWithNoFreeWill(ScaryMod.SPACE_KOOK, AT);
		villain.hurtServer(level, level.damageSources().generic(), 10_000.0F);
		helper.runAfterDelay(20, () -> {
			helper.assertTrue(!villain.isAlive(), "The villain should be dead");
			helper.assertTrue(crooks(helper).isEmpty(), "Nobody should be unmasked");
			Item mask = Unmasking.mask("space_kook");
			helper.assertTrue(level.getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(AT)).inflate(4),
					drop -> drop.getItem().is(mask)).isEmpty(), "No mask should drop");
			helper.succeed();
		});
	}

	@GameTest(structure = "jugcraft-test:drone_depot", maxTicks = UnmaskedCrook.CAUGHT_TICKS + 60)
	public void theCrookCannotBeHurtAndIsLedAway(GameTestHelper helper) {
		floor(helper);
		ServerLevel level = helper.getLevel();
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		UnmaskedCrook crook = helper.spawn(ScaryMod.UNMASKED_CROOK, AT);
		helper.assertFalse(crook.hurtServer(level, level.damageSources().playerAttack(player), 100.0F), "A player's blow should not hurt the crook");
		helper.assertTrue(crook.isAlive() && crook.getHealth() == crook.getMaxHealth(), "The crook should be unhurt");
		helper.runAfterDelay(UnmaskedCrook.CAUGHT_TICKS / 2, () -> helper.assertTrue(crook.isAlive(), "The crook should still stand caught"));
		helper.runAfterDelay(UnmaskedCrook.CAUGHT_TICKS + 20, () -> {
			helper.assertTrue(crook.isRemoved(), "The crook should have been led away");
			helper.succeed();
		});
	}
}
