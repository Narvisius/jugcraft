package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.guns.GunShots;
import io.github.jimbozoomer.jugcraft.guns.JugcraftGuns;
import java.util.Map;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * In-game tests for heavy bullets (docs/features/guns.md, the knockback and armour-piercing follow-up): the Jawbreaker's
 * bullets knock back what they hurt further than a hit's own push, and pass through 4.5 points of its armour, as the
 * owner's Uppercut data has them. Shooters face south (+z).
 */
public class HeavyBulletsGameTests {
	/**
	 * The heavy bullets are the owner's, at the owner's numbers: the Jawbreaker's (the Uppercut's) knock back 0.6 further
	 * and pierce 4.5 points of armour; slice 15C's Impetus Shotgun's (the Inertial's) knock back 0.65 further, and the
	 * Hawker Rifle's and the Quill Rifle's (the MAS Peddler's and the Zilk 45's) pierce 1 and 1.5 points.
	 */
	@GameTest
	public void heavyBulletsAreTheOwners(GameTestHelper helper) {
		helper.assertTrue(JugcraftGuns.KNOCKBACK.equals(Map.of("jawbreaker", 0.6F, "impetus_shotgun", 0.65F))
				&& JugcraftGuns.PIERCING.equals(Map.of("jawbreaker", 4.5F, "hawker_rifle", 1.0F, "quill_rifle", 1.5F)),
				"The heavy bullets are not the owner's: " + JugcraftGuns.KNOCKBACK + ", " + JugcraftGuns.PIERCING);
		helper.assertTrue(JugcraftGuns.knockback(JugcraftGuns.GUNS.get("kreuzer_pistol")) == 0.0F
				&& JugcraftGuns.piercing(JugcraftGuns.GUNS.get("kreuzer_pistol")) == 0.0F, "The Kreuzer Pistol's bullets are heavy too");
		helper.succeed();
	}

	/**
	 * Three pigs in armour of 10 points that a hit can push. One is shot by the Jawbreaker and one by the Kreuzer Pistol,
	 * side by side: the Jawbreaker's is knocked away faster. The third takes the Jawbreaker's damage as a plain bullet
	 * hit: the Jawbreaker's own hit took more from its pig, through the armour, and no more than its damage.
	 */
	@GameTest(structure = GunsGameTests.ARENA, maxTicks = 40)
	public void jawbreakerKnocksBackAndPiercesArmour(GameTestHelper helper) {
		GunsGameTests.floor(helper);
		Mob struck = armouredPig(helper, new BlockPos(1, 2, 4));
		Mob light = armouredPig(helper, new BlockPos(5, 2, 4));
		Mob plain = armouredPig(helper, new BlockPos(9, 2, 4));
		ServerPlayer jaw = NetherGunsGameTests.shooter(helper, "jawbreaker", new BlockPos(1, 2, 1), struck);
		ServerPlayer kreuzer = NetherGunsGameTests.shooter(helper, "kreuzer_pistol", new BlockPos(5, 2, 1), light);
		helper.assertTrue(GunShots.fire(jaw) && GunShots.fire(kreuzer), "The Jawbreaker or the Kreuzer Pistol did not fire");
		double pushed = struck.getDeltaMovement().horizontalDistance();
		double nudged = light.getDeltaMovement().horizontalDistance();
		helper.assertTrue(pushed > nudged + 0.2, "The Jawbreaker's hit did not knock its pig away faster: " + pushed + " against " + nudged);
		float damage = JugcraftGuns.SPECS.get("jawbreaker").damage();
		DamageSource bullet = new DamageSource(helper.getLevel().registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE)
				.getOrThrow(JugcraftGuns.BULLET), jaw, jaw);
		plain.hurtServer(helper.getLevel(), bullet, damage);
		float pierced = 200.0F - struck.getHealth();
		float stopped = 200.0F - plain.getHealth();
		helper.assertTrue(pierced > stopped + 0.5F && pierced <= damage + 1.0E-3F,
				"Through 10 points of armour the Jawbreaker took " + pierced + ", a plain bullet hit of " + damage + " took " + stopped);
		helper.succeed();
	}

	/** A sturdy pig of 200 health in 10 points of armour, which a hit can push. */
	private static Mob armouredPig(GameTestHelper helper, BlockPos at) {
		Mob pig = NetherGunsGameTests.sturdyPig(helper, at);
		pig.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0.0);
		pig.getAttribute(Attributes.ARMOR).setBaseValue(10.0);
		return pig;
	}
}
