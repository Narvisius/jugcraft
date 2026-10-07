package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.BalloonControlPayload;
import io.github.jimbozoomer.jugcraft.agriculture.Balloons;
import io.github.jimbozoomer.jugcraft.agriculture.FiestaWinds;
import io.github.jimbozoomer.jugcraft.agriculture.HotAirBalloon;
import io.github.jimbozoomer.jugcraft.agriculture.HotAirBalloonItem;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.MooringPostBlock;
import io.github.jimbozoomer.jugcraft.agriculture.Pibal;
import io.github.jimbozoomer.jugcraft.agriculture.PibalItem;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.Vec3;

/**
 * Game tests for the hot-air balloon fiesta (fall addition 29), in a 44 by 44 by 26 empty arena so a balloon has room.
 * Run by CI's {@code runGameTest}.
 */
public class HotAirBalloonGameTests {
	private static final String ARENA = "jugcraft-test:drone_tower";
	/** Where a balloon is set up in the arena: on the stone floor at its middle. */
	private static final BlockPos GROUND = new BlockPos(22, 0, 22);

	private static ServerPlayer player(GameTestHelper helper, BlockPos standAt) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		// Test players start out holding the Creative Tower Guide.
		player.getInventory().clearContent();
		BlockPos absolute = helper.absolutePos(standAt);
		player.setPos(absolute.getX() + 0.5, absolute.getY(), absolute.getZ() + 0.5);
		return player;
	}

	private static void floor(GameTestHelper helper) {
		for (int x = 14; x <= 30; x++) {
			for (int z = 14; z <= 30; z++) {
				helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
			}
		}
	}

	private static HotAirBalloon setUp(GameTestHelper helper, HotAirBalloon.Kind kind, int fuel) {
		floor(helper);
		HotAirBalloon balloon = HotAirBalloonItem.setUp(helper.getLevel(), helper.absolutePos(GROUND), kind, 0.0F, fuel);
		helper.assertTrue(balloon != null, "The balloon is set up");
		return balloon;
	}

	/** On open ground it stands, its fuel from the item; under a low roof or over air it can't be set up. */
	@GameTest(structure = ARENA, skyAccess = true)
	public void itNeedsOpenGround(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		HotAirBalloon balloon = setUp(helper, HotAirBalloon.Kind.PUMPKIN, 400);
		helper.assertTrue(balloon.kind() == HotAirBalloon.Kind.PUMPKIN && balloon.fuel() == 400 && balloon.grounded(),
				"It stands on the ground, a Jack-o'-Lantern with its fuel");
		BlockPos low = helper.absolutePos(new BlockPos(16, 0, 16));
		helper.setBlock(new BlockPos(16, 3, 16), Blocks.STONE);
		helper.assertTrue(!HotAirBalloonItem.roomFor(level, low), "Not under a block three up");
		helper.assertTrue(!HotAirBalloonItem.roomFor(level, low.above(5)), "Nor over air");
		helper.assertTrue(HotAirBalloonItem.roomFor(level, helper.absolutePos(new BlockPos(28, 0, 28))), "But on open ground");
		helper.succeed();
	}

	/**
	 * A generator's fuels load the tanks (coal a quarter of its burn), anything else doesn't; full tanks refuse more.
	 * Packed up, a balloon keeps its fuel in the item.
	 */
	@GameTest(structure = ARENA, skyAccess = true)
	public void fuelLoadsAndIsKept(GameTestHelper helper) {
		HotAirBalloon balloon = setUp(helper, HotAirBalloon.Kind.HARVEST, 0);
		ServerPlayer player = player(helper, new BlockPos(22, 1, 20));
		ItemStack coal = new ItemStack(Items.COAL, 2);
		helper.assertTrue(balloon.refuel(coal, player) && balloon.fuel() == 400 && coal.getCount() == 1, "Coal gives 400 units: " + balloon.fuel());
		helper.assertTrue(!balloon.refuel(new ItemStack(Items.DIRT), player), "Dirt isn't fuel");
		balloon.setFuel(HotAirBalloon.MAX_FUEL - 100);
		helper.assertTrue(!balloon.refuel(coal, player) && coal.getCount() == 1, "Full tanks refuse more");
		balloon.setFuel(1234);
		player.setShiftKeyDown(true);
		Balloons.use(player, helper.getLevel(), InteractionHand.MAIN_HAND, balloon);
		helper.assertTrue(balloon.isRemoved(), "Sneaking with an empty hand packs it up");
		ItemStack packed = ItemStack.EMPTY;
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			if (player.getInventory().getItem(slot).is(JugcraftAgriculture.item("harvest_balloon"))) {
				packed = player.getInventory().getItem(slot);
			}
		}
		helper.assertTrue(HotAirBalloonItem.fuel(packed) == 1234, "and the item keeps its fuel: " + HotAirBalloonItem.fuel(packed));
		helper.succeed();
	}

	/**
	 * Up to four climb in; the first is the pilot, and only the pilot's keys count. The burner heats it, burning fuel,
	 * until it lifts off and climbs; the vent cools it.
	 */
	@GameTest(structure = ARENA, skyAccess = true, maxTicks = 400)
	public void theBurnerLiftsItAndTheVentCoolsIt(GameTestHelper helper) {
		HotAirBalloon balloon = setUp(helper, HotAirBalloon.Kind.HARVEST, 2000);
		ServerPlayer pilot = player(helper, new BlockPos(22, 1, 20));
		ServerPlayer friend = player(helper, new BlockPos(21, 1, 20));
		helper.assertTrue(pilot.startRiding(balloon) && friend.startRiding(balloon) && balloon.pilot() == pilot, "Two climb in; the first pilots");
		for (int i = 0; i < 3; i++) {
			ServerPlayer more = player(helper, new BlockPos(20 + i, 1, 24));
			boolean in = more.startRiding(balloon);
			helper.assertTrue(in == (i < 2), "Four fit, not five");
		}
		helper.assertTrue(!BalloonControlPayload.apply(friend, true, false) && !balloon.burnerKey(), "A passenger's keys don't count");
		helper.assertTrue(BalloonControlPayload.apply(pilot, true, false) && balloon.burnerKey(), "The pilot's do");
		double startY = balloon.getY();
		balloon.setHeat(0.6F);
		helper.runAfterDelay(120, () -> {
			String state = " (heat " + balloon.heat() + ", fuel " + balloon.fuel() + ", burner " + balloon.burnerKey() + ", burning " + balloon.burning()
					+ ", pilot " + (balloon.pilot() == pilot) + ", riders " + balloon.getPassengers().size() + ", grounded " + balloon.grounded()
					+ ", removed " + balloon.isRemoved() + ", y " + balloon.getY() + ", sea " + helper.getLevel().getSeaLevel() + ", climb "
					+ HotAirBalloon.climbFor(balloon.heat(), balloon.getY(), helper.getLevel().getSeaLevel()) + ", motion " + balloon.getDeltaMovement() + ")";
			helper.assertTrue(balloon.getY() > startY + 1.0, "Fired, it lifts off and climbs: " + (balloon.getY() - startY) + " up" + state);
			helper.assertTrue(balloon.fuel() <= 2000 - 110, "burning fuel: " + balloon.fuel() + " left");
			float hot = balloon.heat();
			BalloonControlPayload.apply(pilot, false, true);
			helper.runAfterDelay(40, () -> {
				helper.assertTrue(balloon.heat() < hot - 0.2F, "Venting cools it fast: " + hot + " to " + balloon.heat());
				helper.succeed();
			});
		});
	}

	/**
	 * Riders sit facing along the basket's walls, round it like a pinwheel (the game draws every rider seated, so their
	 * legs stay inside the wicker), and look no more than {@value HotAirBalloon#LOOK} degrees either way from there.
	 */
	@GameTest(structure = ARENA, skyAccess = true)
	public void ridersSitAlongTheWalls(GameTestHelper helper) {
		HotAirBalloon balloon = setUp(helper, HotAirBalloon.Kind.HARVEST, 0);
		ServerPlayer first = player(helper, new BlockPos(22, 1, 20));
		ServerPlayer second = player(helper, new BlockPos(21, 1, 20));
		helper.assertTrue(first.startRiding(balloon) && second.startRiding(balloon), "Two climb in");
		// The first sits facing 90 degrees left of the basket's front; turn them to look straight behind that.
		first.setYRot(balloon.getYRot() + 90.0F);
		helper.runAfterDelay(2, () -> {
			float firstSits = Mth.wrapDegrees(first.yBodyRot - (balloon.getYRot() - 90.0F));
			float secondSits = Mth.wrapDegrees(second.yBodyRot - balloon.getYRot());
			float looks = Math.abs(Mth.wrapDegrees(first.getYRot() - (balloon.getYRot() - 90.0F)));
			helper.assertTrue(Math.abs(firstSits) < 1.0F && Math.abs(secondSits) < 1.0F,
					"Each sits along a wall: " + firstSits + ", " + secondSits);
			helper.assertTrue(looks <= HotAirBalloon.LOOK + 0.01F, "and looks no more than " + HotAirBalloon.LOOK + " either way: " + looks);
			helper.succeed();
		});
	}

	/** On the ground it stays put; aloft it drifts with the wind at its envelope's height. */
	@GameTest(structure = ARENA, skyAccess = true, maxTicks = 200)
	public void aloftItDriftsWithTheWind(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		HotAirBalloon grounded = setUp(helper, HotAirBalloon.Kind.MOON, 0);
		HotAirBalloon aloft = HotAirBalloonItem.setUp(level, helper.absolutePos(new BlockPos(16, 0, 16)), HotAirBalloon.Kind.HARVEST, 0.0F, 0);
		helper.assertTrue(aloft != null, "A second is set up");
		aloft.setPos(aloft.getX(), aloft.getY() + 6, aloft.getZ());
		ServerPlayer pilot = player(helper, new BlockPos(16, 1, 16));
		pilot.startRiding(aloft);
		aloft.setHeat(0.53F);
		Vec3 start = grounded.position();
		Vec3 from = aloft.position();
		Vec3 wind = aloft.wind(level);
		helper.runAfterDelay(80, () -> {
			helper.assertTrue(grounded.position().subtract(start).horizontalDistance() < 0.05, "The one on the ground stays put");
			Vec3 moved = aloft.position().subtract(from);
			double along = moved.x * wind.x + moved.z * wind.z;
			helper.assertTrue(moved.horizontalDistance() > 0.5 && along > 0.0,
					"The one aloft drifts with the wind: moved " + moved + ", wind " + wind);
			helper.succeed();
		});
	}

	/**
	 * The winds: the same for the same day, two lowest layers roughly opposite (the box), stronger higher up and in
	 * rain, swinging through the day, blended across a boundary.
	 */
	@GameTest
	public void theWindsHaveLayers(GameTestHelper helper) {
		long seed = 12345L;
		for (long day = 0; day < 20; day++) {
			double low = FiestaWinds.heading(seed, day, 0);
			double next = FiestaWinds.heading(seed, day, 1);
			double apart = Math.abs(((next - low) % 360.0 + 360.0) % 360.0 - 180.0);
			helper.assertTrue(apart <= FiestaWinds.BOX_SPREAD + 1e-6, "Day " + day + ": the two lowest layers blow within 30 degrees of opposite: " + apart);
			helper.assertTrue(FiestaWinds.heading(seed, day, 3) == FiestaWinds.heading(seed, day, 3), "The same every time");
			helper.assertTrue(FiestaWinds.speed(seed, day, 7) > FiestaWinds.speed(seed, day, 0), "Stronger high up");
		}
		Vec3 calm = FiestaWinds.layerWind(seed, 1000L, 2, false);
		Vec3 storm = FiestaWinds.layerWind(seed, 1000L, 2, true);
		helper.assertTrue(Math.abs(storm.length() - calm.length() * FiestaWinds.STORM) < 1e-9, "Half as strong again in rain");
		Vec3 morning = FiestaWinds.layerWind(seed, 0L, 2, false);
		Vec3 noon = FiestaWinds.layerWind(seed, 6000L, 2, false);
		helper.assertTrue(morning.subtract(noon).length() > 1e-4, "They swing through the day");
		int sea = 63;
		Vec3 inLow = FiestaWinds.at(seed, sea, 0L, sea + 4.0, false);
		Vec3 boundary = FiestaWinds.at(seed, sea, 0L, sea + FiestaWinds.LAYER - 1.0, false);
		Vec3 inNext = FiestaWinds.at(seed, sea, 0L, sea + FiestaWinds.LAYER + 4.0, false);
		helper.assertTrue(inLow.equals(FiestaWinds.layerWind(seed, 0L, 0, false)) && inNext.equals(FiestaWinds.layerWind(seed, 0L, 1, false)),
				"Inside a layer, its own wind");
		helper.assertTrue(!boundary.equals(inLow) && !boundary.equals(inNext), "Near its top, blending into the next");
		helper.assertTrue("E".equals(FiestaWinds.compass(new Vec3(1, 0, 0))) && "N".equals(FiestaWinds.compass(new Vec3(0, 0, -1))),
				"Compass points read as a pilot reads them");
		helper.succeed();
	}

	/**
	 * A Mooring Post ties the nearest balloon and casts it off again; tethered, it can't go further than its rope or
	 * higher than the tether; breaking the post casts it off.
	 */
	@GameTest(structure = ARENA, skyAccess = true)
	public void aMooringPostTethersIt(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		HotAirBalloon balloon = setUp(helper, HotAirBalloon.Kind.HARVEST, 0);
		BlockPos post = helper.absolutePos(new BlockPos(18, 1, 22));
		helper.setBlock(new BlockPos(18, 1, 22), JugcraftAgriculture.block(MooringPostBlock.ID));
		helper.assertTrue(MooringPostBlock.toggle(level, post).endsWith("tied") && post.equals(balloon.mooring()), "Using the post ties the balloon");
		Vec3 out = HotAirBalloon.tether(post, post.getX() + 0.5 + HotAirBalloon.ROPE, post.getY() + 2, post.getZ() + 0.5, 0.2, 0.0, 0.1);
		helper.assertTrue(out.x <= 0.0 && out.z > 0.09, "At the rope's end it can't go further out, only along: " + out);
		Vec3 up = HotAirBalloon.tether(post, post.getX() + 0.5, post.getY() + 1 + HotAirBalloon.TETHER_HEIGHT - 0.05, post.getZ() + 0.5, 0.0, 0.2, 0.0);
		helper.assertTrue(up.y <= 0.05 + 1e-9, "nor higher than the tether: " + up);
		Vec3 free = HotAirBalloon.tether(post, post.getX() + 2.5, post.getY() + 3, post.getZ() + 0.5, 0.1, 0.1, 0.1);
		helper.assertTrue(free.equals(new Vec3(0.1, 0.1, 0.1)), "Within them it goes as the wind takes it");
		helper.assertTrue(MooringPostBlock.toggle(level, post).endsWith("untied") && balloon.mooring() == null, "Used again, it casts off");
		MooringPostBlock.toggle(level, post);
		level.destroyBlock(post, false);
		helper.assertTrue(balloon.mooring() == null, "Breaking the post casts it off");
		helper.assertTrue(MooringPostBlock.toggle(level, helper.absolutePos(new BlockPos(5, 1, 5))).endsWith("none"), "Nothing to tie far off");
		helper.succeed();
	}

	/** A pibal let go rises with the wind, and pops at the end of its life. */
	@GameTest(structure = ARENA, skyAccess = true, maxTicks = 100)
	public void aPibalRisesAndPops(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Vec3 at = Vec3.atCenterOf(helper.absolutePos(new BlockPos(22, 3, 22)));
		Pibal pibal = PibalItem.release(level, at);
		Pibal old = PibalItem.release(level, at.add(4, 0, 4));
		helper.assertTrue(pibal != null && old != null, "Two pibals are let go");
		old.tickCount = Pibal.LIFE - 2;
		helper.runAfterDelay(40, () -> {
			helper.assertTrue(pibal.getY() > at.y + 40 * Pibal.RISE * 0.8, "It rises: " + (pibal.getY() - at.y));
			helper.assertTrue(new Vec3(pibal.getX() - at.x, 0, pibal.getZ() - at.z).length() > 0.5, "drifting with the wind");
			helper.assertTrue(old.isRemoved(), "An old one pops");
			helper.succeed();
		});
	}

	/** How it climbs for its heat, and what counts as a box for The Box. */
	@GameTest
	public void climbAndTheBox(GameTestHelper helper) {
		int sea = 63;
		helper.assertTrue(HotAirBalloon.climbFor(1.0F, sea, sea) == HotAirBalloon.MAX_CLIMB, "Hottest, it climbs its fastest");
		helper.assertTrue(HotAirBalloon.climbFor(0.0F, sea, sea) == -HotAirBalloon.MAX_SINK, "Cold, it sinks its fastest");
		helper.assertTrue(Math.abs(HotAirBalloon.climbFor(HotAirBalloon.NEUTRAL, sea, sea)) < 1e-9, "At the neutral heat it floats");
		helper.assertTrue(HotAirBalloon.climbFor(0.55F, sea + 100, sea) < HotAirBalloon.climbFor(0.55F, sea, sea), "Higher, the air is thinner");
		Vec3 home = new Vec3(0, 70, 0);
		helper.assertTrue(HotAirBalloon.boxed(home, new Vec3(10, 70, 5), 80), "Out 80 and home within 16: a box");
		helper.assertTrue(!HotAirBalloon.boxed(home, new Vec3(10, 70, 5), 40), "Not out far enough");
		helper.assertTrue(!HotAirBalloon.boxed(home, new Vec3(30, 70, 0), 80), "Not home");
		helper.succeed();
	}

	/** The recipes, advancements and loot load. */
	@GameTest
	public void balloonDataLoads(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		for (String id : List.of("harvest_balloon", "pumpkin_balloon", "harvest_moon_balloon", "balloon_burner", MooringPostBlock.ID, "pibal")) {
			helper.assertTrue(level.recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id(id))).isPresent(), id + " has a recipe");
		}
		for (String id : List.of("up_up_and_away", "the_box", "mass_ascension")) {
			helper.assertTrue(level.getServer().getAdvancements().get(Jugcraft.id(id)) != null, id + " loads");
		}
		helper.assertTrue(level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,
				Jugcraft.id("blocks/" + MooringPostBlock.ID))) != LootTable.EMPTY, "The post's loot loads");
		helper.succeed();
	}
}
