package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.drone.CargoPackagerBlockEntity;
import io.github.jimbozoomer.jugcraft.drone.JugcraftDrones;
import io.github.jimbozoomer.jugcraft.tower.crew.TowerCrew;
import io.github.jimbozoomer.jugcraft.tower.squad.DropPod;
import io.github.jimbozoomer.jugcraft.tower.squad.JugcraftSquads;
import io.github.jimbozoomer.jugcraft.tower.squad.Legionary;
import io.github.jimbozoomer.jugcraft.tower.squad.Requisitions;
import io.github.jimbozoomer.jugcraft.tower.squad.SquadAccounts;
import io.github.jimbozoomer.jugcraft.tower.squad.SquadDrop;
import io.github.jimbozoomer.jugcraft.tower.squad.SquadFilm;
import io.github.jimbozoomer.jugcraft.tower.squad.SquadFilmCamera;
import io.github.jimbozoomer.jugcraft.tower.squad.SquadOrders;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Drop pod squads (release (a); the plan approved 9 November 2026): requisitions paid from a tower's store and then its
 * network, used up; a mark refused out of range, under a roof, with no charge or with a squad already out, and taken
 * otherwise; the drop (pods 10 to 20 blocks apart round the mark, each landing, its hatch blowing and its soldier out,
 * the leader with his crest, the pod blowing itself apart once its soldier is clear, nothing broken); a soldier fights
 * monsters, never the crew, tamed animals or the player they follow; the next in line takes the crest when the leader
 * falls; release (b): the leader's orders (hold in a ring, patrol, follow; not home in a fight) and a squad sent home
 * boarding the ship that comes for them, one by one, the leader last, and the ship leaving. The tests that put soldiers
 * down run in their own batches (no other test's monsters or ships near).
 */
public class SquadGameTests {
	/** A tower's store as a map (the core's own holds only what its build still needs). */
	private static final class MapStore implements Requisitions.Store {
		final Map<Item, Integer> held = new HashMap<>();

		@Override
		public int stored(Item item) {
			return held.getOrDefault(item, 0);
		}

		@Override
		public int take(Item item, int count) {
			int took = Math.min(stored(item), count);
			held.put(item, stored(item) - took);
			return took;
		}
	}

	@GameTest(structure = "jugcraft-test:drone_depot")
	public void requisitionPaysStoreFirstThenNetwork(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		ServerLevel level = helper.getLevel();
		BlockPos at = helper.absolutePos(new BlockPos(2, 1, 2));
		level.setBlock(at, JugcraftDrones.CARGO_PACKAGER.defaultBlockState(), Block.UPDATE_ALL);
		CargoPackagerBlockEntity net = (CargoPackagerBlockEntity) level.getBlockEntity(at);
		helper.assertTrue(net != null, "a cargo packager");
		for (int i = 0; i < 6; i++) {
			net.setItem(i, new ItemStack(Items.IRON_INGOT, 64));
		}
		net.setItem(6, new ItemStack(Items.COPPER_INGOT, 64));
		net.setItem(7, new ItemStack(Items.COPPER_INGOT, 64));
		net.setItem(8, new ItemStack(Items.COPPER_INGOT, 64));
		net.setItem(9, new ItemStack(Items.COPPER_INGOT, 30));
		net.setItem(10, new ItemStack(Items.GOLD_INGOT, 64));
		net.setItem(11, new ItemStack(Items.GOLD_INGOT, 26));
		net.setItem(12, new ItemStack(Items.DIAMOND, 30));
		MapStore store = new MapStore();
		store.held.put(Items.IRON_INGOT, 150);
		Requisitions.Purse purse = new Requisitions.Purse(store, net);
		int[] have = purse.have();
		helper.assertTrue(have[0] == 534 && have[1] == 222 && have[2] == 90 && have[3] == 30, "the purse counts store and network: " + java.util.Arrays.toString(have));
		helper.assertTrue(!Requisitions.affords(have, 1), "a squad of 5 is more than it holds");
		helper.assertTrue(!Requisitions.pay(purse, 1) && purse.have()[0] == 534, "refused, nothing's taken");
		helper.assertTrue(Requisitions.pay(purse, 0), "a squad of 3 is paid");
		int[] left = purse.have();
		helper.assertTrue(store.stored(Items.IRON_INGOT) == 0, "the store's iron went first");
		helper.assertTrue(left[0] == 534 - 288 && left[1] == 222 - 192 && left[2] == 90 - 72 && left[3] == 30 - 18,
				"the rest from the network, used up: " + java.util.Arrays.toString(left));
		SquadAccounts accounts = SquadAccounts.get(level);
		UUID who = UUID.randomUUID();
		accounts.add(who, 0);
		helper.assertTrue(accounts.charges(who)[0] == 1 && accounts.spend(who, 0) && !accounts.spend(who, 0), "a charge on the account, spent once");
		helper.succeed();
	}

	/** A stone pad (48 x 48) at the structure's floor, open to the sky (the test's barrier lid over it taken off). */
	private static BlockPos pad(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		for (int x = 0; x < 48; x++) {
			for (int z = 0; z < 48; z++) {
				level.setBlock(helper.absolutePos(new BlockPos(x, 0, z)), Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
				for (int y = 1; y < 12; y++) {
					BlockPos up = helper.absolutePos(new BlockPos(x, y, z));
					if (!level.getBlockState(up).isAir()) {
						level.setBlock(up, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
					}
				}
			}
		}
		BlockPos centre = helper.absolutePos(new BlockPos(24, 1, 24));
		for (int x = -40; x <= 40; x += 16) {
			for (int z = -40; z <= 40; z += 16) {
				level.setChunkForced((centre.getX() + x) >> 4, (centre.getZ() + z) >> 4, true);
			}
		}
		return centre;
	}

	/** A survival player (the game's mock one in the level is creative whatever it's told) at {@code at}, the sight up. */
	private static ServerPlayer spotter(GameTestHelper helper, BlockPos at) {
		ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
		player.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 8.5, 180, 0);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JugcraftSquads.DESIGNATOR));
		player.startUsingItem(InteractionHand.MAIN_HAND);
		return player;
	}

	@GameTest(environment = "jugcraft-test:drop_squad", structure = "jugcraft-test:tatterlace_arena", maxTicks = 100)
	public void markChecksRangeSkyAccountAndField(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		ServerLevel level = helper.getLevel();
		BlockPos centre = pad(helper);
		ServerPlayer player = spotter(helper, centre);
		SquadAccounts accounts = SquadAccounts.get(level);
		accounts.clearField(player.getUUID());
		Vec3 mark = Vec3.atBottomCenterOf(centre);
		helper.assertTrue(SquadDrop.mark(player, mark.add(0, 0, -300), 3) == SquadDrop.TOO_FAR, "out of range");
		helper.assertTrue(SquadDrop.mark(player, mark, 3) == SquadDrop.NO_CHARGE, "nothing on the account");
		accounts.add(player.getUUID(), 0);
		BlockPos roof = centre.above(4);
		level.setBlock(roof, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
		helper.assertTrue(SquadDrop.mark(player, mark, 3) == SquadDrop.NO_SKY, "under a roof");
		level.setBlock(roof, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
		var pig = net.minecraft.world.entity.EntityTypes.PIG.create(level, EntitySpawnReason.TRIGGERED);
		helper.assertTrue(pig != null, "a stand-in");
		pig.snapTo(centre.getX() + 0.5, centre.getY(), centre.getZ() + 0.5, 0, 0);
		level.addFreshEntity(pig);
		accounts.out(player.getUUID(), pig.getUUID());
		helper.assertTrue(SquadDrop.mark(player, mark, 3) == SquadDrop.IN_FIELD, "a squad already out");
		pig.discard();
		accounts.out(player.getUUID(), UUID.randomUUID());
		helper.assertTrue(!accounts.inField(level.getServer(), player.getUUID()), "one out of reach is recalled, not in the way");
		player.stopUsingItem();
		helper.assertTrue(SquadDrop.mark(player, mark, 3) == SquadDrop.NOT_SIGHTED, "not looking through the sight");
		player.startUsingItem(InteractionHand.MAIN_HAND);
		helper.assertTrue(SquadDrop.mark(player, mark, 3) == SquadDrop.OK, "marked");
		helper.assertTrue(accounts.charges(player.getUUID())[0] == 0, "the charge is spent");
		// (this player's: another test's pods may be coming down near)
		List<DropPod> pods = level.getEntitiesOfClass(DropPod.class, new AABB(centre).inflate(40), p -> player.getUUID().equals(p.owner()));
		helper.assertTrue(pods.size() == 3, "three pods launched, not " + pods.size());
		helper.assertTrue(accounts.inField(level.getServer(), player.getUUID()), "the squad is out");
		pods.forEach(DropPod::discard);
		accounts.clearField(player.getUUID());
		helper.succeed();
	}

	/**
	 * The pods' sounds (tools/drop_pod_sounds.py: real recordings for the roar, the boom, the retros and the impact):
	 * every event registered and in sounds.json, every file it names there in the mod; the old made-up scream gone.
	 */
	@GameTest
	public void podSoundsAreAllInPlace(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		com.google.gson.JsonObject sounds;
		try (var in = SquadGameTests.class.getResourceAsStream("/assets/jugcraft/sounds.json")) {
			helper.assertTrue(in != null, "sounds.json is in the mod");
			sounds = com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
		} catch (java.io.IOException e) {
			throw new AssertionError(e);
		}
		for (String name : new String[] {"roar_far", "roar_near", "boom", "retro", "impact", "hatch", "selfdestruct", "mark"}) {
			String key = "drop_pod." + name;
			helper.assertTrue(net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.containsKey(io.github.jimbozoomer.jugcraft.Jugcraft.id(key)),
					"the sound event " + key + " is registered");
			helper.assertTrue(sounds.has(key), key + " is in sounds.json");
			for (var e : sounds.getAsJsonObject(key).getAsJsonArray("sounds")) {
				String file = e.getAsJsonObject().get("name").getAsString().replace("jugcraft:", "");
				helper.assertTrue(SquadGameTests.class.getResource("/assets/jugcraft/sounds/" + file + ".ogg") != null, key + "'s " + file + ".ogg is in the mod");
			}
		}
		helper.assertTrue(!sounds.has("drop_pod.scream") && SquadGameTests.class.getResource("/assets/jugcraft/sounds/drop_pod/scream.ogg") == null,
				"the old scream is gone");
		helper.assertTrue(sounds.getAsJsonObject("drop_pod.impact").getAsJsonArray("sounds").size() == 3
				&& sounds.getAsJsonObject("drop_pod.boom").getAsJsonArray("sounds").size() == 3, "three impacts, the boom at three pitches");
		helper.succeed();
	}

	/**
	 * The pods' flights (Justin, 10 November 2026): they set off over about seven seconds in no set order, each from its
	 * own point in the sky about the flare (the leader's the flare), spread wide, still landing where they did; and in
	 * a hundred drops of every size no two come within reach of each other on the way down.
	 */
	@GameTest
	public void podFlightsAreSpreadStaggeredAndNeverMeet(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		net.minecraft.util.RandomSource rng = net.minecraft.util.RandomSource.create(42);
		Vec3 mark = new Vec3(0.5, 64, 0.5);
		double widest = 0;
		for (int drop = 0; drop < 100; drop++) {
			int n = JugcraftSquads.SIZES[drop % JugcraftSquads.SIZES.length];
			List<Vec3> spots = new java.util.ArrayList<>();
			spots.add(mark.add(rng.nextDouble() * 4 - 2, 0, rng.nextDouble() * 4 - 2));
			while (spots.size() < n) {
				double a = rng.nextDouble() * Math.PI * 2, r = 8 + rng.nextDouble() * 12;
				Vec3 s = mark.add(Math.cos(a) * r, rng.nextInt(5) - 2, Math.sin(a) * r);
				if (spots.stream().allMatch(o -> Math.hypot(o.x - s.x, o.z - s.z) >= JugcraftSquads.SPREAD_MIN * 0.7)) {
					spots.add(s);
				}
			}
			List<SquadDrop.Plan> plans = SquadDrop.plan(mark, spots, rng);
			helper.assertTrue(plans.size() == n, "a flight for each pod");
			int lo = plans.stream().mapToInt(SquadDrop.Plan::delay).min().orElse(-1), hi = plans.stream().mapToInt(SquadDrop.Plan::delay).max().orElse(-1);
			helper.assertTrue(lo == 0 && hi == DropPod.SPREAD_TICKS && plans.stream().mapToInt(SquadDrop.Plan::delay).distinct().count() == n,
					"drop " + drop + ": the pods set off over " + DropPod.SPREAD_TICKS + " ticks, none together: " + lo + " to " + hi);
			for (int i = 0; i < n; i++) {
				SquadDrop.Plan p = plans.get(i);
				helper.assertTrue(p.spot().equals(spots.get(i)), "each lands where it was to");
				org.joml.Vector3f end = DropPod.path(p.flare(), p.seed(), DropPod.FLIGHT, new org.joml.Vector3f());
				helper.assertTrue(end.length() < 0.01F, "drop " + drop + " pod " + i + " comes down on its spot: " + end);
				for (int j = 0; j < i; j++) {
					double c = SquadDrop.clearance(p, plans.get(j));
					helper.assertTrue(c >= 0, "drop " + drop + ": pods " + i + " and " + j + " come within " + c + " of touching");
					Vec3 a = p.spot().add(p.flare().x(), p.flare().y(), p.flare().z());
					SquadDrop.Plan q = plans.get(j);
					Vec3 b = q.spot().add(q.flare().x(), q.flare().y(), q.flare().z());
					widest = Math.max(widest, Math.hypot(a.x - b.x, a.z - b.z));
				}
			}
		}
		helper.assertTrue(widest > 60, "the pods' points in the sky are spread wide: " + widest);
		helper.succeed();
	}

	@GameTest(environment = "jugcraft-test:drop_squad", structure = "jugcraft-test:tatterlace_arena", maxTicks = 860)
	public void podsLandApartSoldiersOutPodsBlowWhenClear(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		ServerLevel level = helper.getLevel();
		BlockPos centre = pad(helper);
		UUID owner = UUID.randomUUID();
		Vec3 mark = Vec3.atBottomCenterOf(centre);
		List<Vec3> spots = SquadDrop.spots(level, mark, 5, level.getRandom());
		helper.assertTrue(spots.size() == 5, "five places to land, found " + spots.size());
		helper.assertTrue(spots.get(0).distanceTo(mark) <= 6.5, "the leader's near the mark");
		for (int i = 0; i < spots.size(); i++) {
			for (int j = i + 1; j < spots.size(); j++) {
				double d = Math.hypot(spots.get(i).x - spots.get(j).x, spots.get(i).z - spots.get(j).z);
				helper.assertTrue(d >= JugcraftSquads.SPREAD_MIN * 0.7 - 1e-6, "pods " + i + " and " + j + " " + d + " apart");
			}
		}
		SquadDrop.launch(level, owner, mark.add(0, 0, 10), mark, spots, 1, level.getRandom());
		long start = level.getGameTime();
		// they land one after another over about seven seconds, in no set order; the last's soldier out last
		List<DropPod> launched = level.getEntitiesOfClass(DropPod.class, new AABB(centre).inflate(40), p -> owner.equals(p.owner()));
		helper.assertTrue(launched.size() == 5, "five pods launched");
		int first = launched.stream().mapToInt(DropPod::land).min().orElse(0), last = launched.stream().mapToInt(DropPod::land).max().orElse(0);
		helper.assertTrue(first == DropPod.FLIGHT && last == DropPod.FLIGHT + DropPod.SPREAD_TICKS,
				"the landings spread over " + DropPod.SPREAD_TICKS + " ticks: " + first + " to " + last);
		helper.assertTrue(launched.stream().mapToInt(DropPod::land).distinct().count() == 5, "no two land together");
		int out = last + DropPod.HATCH_AFTER;
		helper.onEachTick(() -> {
			long age = level.getGameTime() - start;
			AABB area = new AABB(centre).inflate(40);
			List<DropPod> pods = level.getEntitiesOfClass(DropPod.class, area, p -> owner.equals(p.owner()));
			List<Legionary> men = level.getEntitiesOfClass(Legionary.class, area, l -> owner.equals(l.owner()));
			if (age == out - 2) {
				// (the first out may have walked clear already, and their pods gone)
				DropPod lastPod = launched.stream().filter(pd -> pd.land() == last).findFirst().orElseThrow();
				helper.assertTrue(men.size() == 4 && pods.contains(lastPod), "four out before the last, its pod down: " + men.size() + ", " + pods.size());
				for (Vec3 s : spots) {
					helper.assertTrue(level.getBlockState(BlockPos.containing(s.x, s.y - 0.5, s.z)).is(Blocks.STONE), "nothing broken where a pod hit");
				}
			}
			if (age == out + 3) {
				helper.assertTrue(men.size() == 5, "five soldiers out, not " + men.size());
				long leaders = men.stream().filter(Legionary::leader).count();
				Legionary lead = men.stream().filter(Legionary::leader).findFirst().orElse(null);
				helper.assertTrue(leaders == 1 && lead != null && (lead.gear() & Legionary.CRESTS[1]) != 0,
						"one leader, a tesserarius's crest on him");
				// (dazed only for a while after his own hatch, which may have been seconds before the last)
				DropPod his = launched.stream().filter(pd -> pd.place() == 0).findFirst().orElseThrow();
				long outFor = age - (his.land() + DropPod.HATCH_AFTER);
				helper.assertTrue(outFor < Legionary.DAZE_TICKS - 1 ? lead.dazed() : outFor > Legionary.DAZE_TICKS + 21 ? !lead.dazed() : true,
						"dazed just after he came out: " + lead.dazed() + ", out " + outFor + " ticks");
				helper.assertTrue(pods.stream().anyMatch(pd -> pd.place() == 0), "the leader's pod waits for him to get clear");
				// all but the leader stride off, well clear of their own pods (the first out may have wandered): their pods blow
				for (Legionary l : men) {
					if (!l.leader()) {
						DropPod own = launched.stream().filter(pd -> pd.place() == l.place()).findFirst().orElseThrow();
						Vec3 away = l.position().subtract(own.position()).multiply(1, 0, 1);
						away = away.lengthSqr() < 1e-4 ? new Vec3(1, 0, 0) : away.normalize();
						l.teleportTo(own.getX() + away.x * 14, l.getY(), own.getZ() + away.z * 14);
					}
				}
			}
			if (age == out + 8) {
				helper.assertTrue(pods.size() == 1, "four pods blew themselves apart, " + pods.size() + " left");
				helper.assertTrue(level.getBlockState(BlockPos.containing(spots.get(1).x, spots.get(1).y - 0.5, spots.get(1).z)).is(Blocks.STONE),
						"and broke nothing");
				Legionary lead = men.stream().filter(Legionary::leader).findFirst().orElse(null);
				helper.assertTrue(lead != null, "the leader");
				lead.kill(level);
			}
			if (age == out + 8 + Legionary.DAZE_TICKS + 100) {
				helper.assertTrue(men.size() == 4 && men.stream().filter(Legionary::leader).count() == 1, "the next in line leads");
				Legionary next = men.stream().filter(Legionary::leader).findFirst().orElseThrow();
				helper.assertTrue(next.place() == 1 && (next.gear() & Legionary.CRESTS[1]) != 0, "the second soldier, with the crest");
				helper.assertTrue(pods.isEmpty(), "the leader's pod went too (its soldier gone)");
				men.forEach(Legionary::discard);
				SquadAccounts.get(level).clearField(owner);
				helper.succeed();
			}
		});
	}

	@GameTest(environment = "jugcraft-test:drop_squad", structure = "jugcraft-test:drone_depot", maxTicks = 300)
	public void legionaryFightsMonstersNotCrewTamedOrTheirPlayer(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		ServerLevel level = helper.getLevel();
		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				level.setBlock(helper.absolutePos(new BlockPos(x, 0, z)), Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
			}
		}
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		BlockPos at = helper.absolutePos(new BlockPos(2, 1, 8));
		player.snapTo(at.getX() + 0.5, at.getY(), at.getZ() - 1.5, 0, 0);
		Legionary l = JugcraftSquads.LEGIONARY.create(level, EntitySpawnReason.TRIGGERED);
		helper.assertTrue(l != null, "a legionary");
		l.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, -90, 0);
		l.setUp(player.getUUID(), UUID.randomUUID(), 0, true, 0);
		level.addFreshEntity(l);
		SquadAccounts.get(level).out(player.getUUID(), l.getUUID());     // (on the record, as a pod's soldier is)
		var crew = io.github.jimbozoomer.jugcraft.tower.JugcraftTower.TOWER_CREW.create(level, EntitySpawnReason.TRIGGERED);
		var wolf = net.minecraft.world.entity.EntityTypes.WOLF.create(level, EntitySpawnReason.TRIGGERED);
		var zombie = net.minecraft.world.entity.EntityTypes.HUSK.create(level, EntitySpawnReason.TRIGGERED);
		helper.assertTrue(crew != null && wolf != null && zombie != null, "the others");
		wolf.tame(player);
		helper.assertTrue(!l.mayFight(crew) && !l.mayFight(wolf) && !l.mayFight(player) && l.mayFight(zombie), "who they'd fight");
		var other = helper.makeMockServerPlayer(GameType.SURVIVAL);
		helper.assertTrue(l.mayFight(other) == level.isPvpAllowed(), "another player only where players may fight");
		helper.assertTrue(!l.mayFight(helper.makeMockServerPlayer(GameType.CREATIVE)), "never a creative player");
		// a husk (no burning in the sun) down the range, held still; the crew member and the wolf to the side
		zombie.snapTo(at.getX() + 12.5, at.getY(), at.getZ() + 0.5, 90, 0);
		zombie.setNoAi(true);
		crew.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 4.5, 0, 0);
		crew.setNoAi(true);
		wolf.snapTo(at.getX() + 0.5, at.getY(), at.getZ() - 4.5, 0, 0);
		wolf.setNoAi(true);
		level.addFreshEntity(zombie);
		level.addFreshEntity(crew);
		level.addFreshEntity(wolf);
		float crewHealth = crew.getHealth(), wolfHealth = wolf.getHealth();
		helper.succeedWhen(() -> {
			helper.assertTrue(!zombie.isAlive() && zombie.getLastHurtByMob() == l, "the husk's shot down, by the legionary");
			helper.assertTrue(crew.getHealth() == crewHealth && wolf.getHealth() == wolfHealth && player.getHealth() == player.getMaxHealth(),
					"nobody else is hurt");
			l.discard();
			crew.discard();
			wolf.discard();
			SquadAccounts.get(level).clearField(player.getUUID());
		});
	}

	/** A legionary's plasma blade (the power armour sheets): as security's (TowerGameTests.bladeFight), from their own goal. */
	@GameTest(environment = "jugcraft-test:plasma_blade", structure = "jugcraft-test:drone_depot", maxTicks = 300)
	public void legionaryTakesTheBladeToWhatGetsClose(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		ServerLevel level = helper.getLevel();
		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				level.setBlock(helper.absolutePos(new BlockPos(x, 0, z)), Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
			}
		}
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		BlockPos at = helper.absolutePos(new BlockPos(4, 1, 8));
		player.snapTo(at.getX() + 0.5, at.getY(), at.getZ() - 4.5, 0, 0);
		Legionary l = JugcraftSquads.LEGIONARY.create(level, EntitySpawnReason.TRIGGERED);
		var husk = net.minecraft.world.entity.EntityTypes.HUSK.create(level, EntitySpawnReason.TRIGGERED);
		helper.assertTrue(l != null && husk != null, "a legionary and a husk");
		l.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, -90, 0);
		l.setUp(player.getUUID(), UUID.randomUUID(), 0, true, 0);
		l.tickCount = Legionary.DAZE_TICKS + 1;                           // (not dazed: as if out of the pod a while)
		level.addFreshEntity(l);
		SquadAccounts.get(level).out(player.getUUID(), l.getUUID());
		husk.snapTo(at.getX() + 3.5, at.getY(), at.getZ() + 0.5, 90, 0);  // 3 blocks off: in reach
		husk.setNoAi(true);
		husk.setPersistenceRequired();
		husk.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(400);
		husk.setHealth(400);
		husk.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
		level.addFreshEntity(husk);
		TowerGameTests.bladeFight(helper, l, husk, l::act, () -> {
			l.discard();
			SquadAccounts.get(level).clearField(player.getUUID());
		});
	}

	/**
	 * The plasma blade's moves by distance (tower/crew/PlasmaBlade, driven by hand on a legionary with its own mind off):
	 * out at once for a husk in reach and no move while it deploys; at arm's length a thrust (the rifle's damage); with
	 * the husk close, a slash (the baton's); with it gone out of reach, stowed.
	 */
	@GameTest(environment = "jugcraft-test:plasma_blade", structure = "jugcraft-test:drone_depot", maxTicks = 200)
	public void plasmaBladeThrustsAtArmsLengthSlashesClose(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		ServerLevel level = helper.getLevel();
		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				level.setBlock(helper.absolutePos(new BlockPos(x, 0, z)), Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
			}
		}
		BlockPos at = helper.absolutePos(new BlockPos(4, 1, 8));
		Legionary l = JugcraftSquads.LEGIONARY.create(level, EntitySpawnReason.TRIGGERED);
		var husk = net.minecraft.world.entity.EntityTypes.HUSK.create(level, EntitySpawnReason.TRIGGERED);
		helper.assertTrue(l != null && husk != null, "a legionary and a husk");
		l.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, -90, 0);
		l.setNoAi(true);
		level.addFreshEntity(l);
		husk.snapTo(at.getX() + 2.3, at.getY(), at.getZ() + 0.5, 90, 0);  // 2.3 blocks off: 2 from its side
		husk.setNoAi(true);
		husk.setPersistenceRequired();
		husk.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(400);
		husk.setHealth(400);
		level.addFreshEntity(husk);
		var fight = new io.github.jimbozoomer.jugcraft.tower.crew.PlasmaBlade.Fight();
		int[] act = {0}, ticks = {0}, stage = {0};
		float[] last = {husk.getHealth()};
		int deploy = io.github.jimbozoomer.jugcraft.tower.crew.PlasmaBlade.DEPLOY_TICKS;
		helper.onEachTick(() -> {
			if (stage[0] > 2) {
				return;
			}
			int before = act[0];
			boolean has = fight.tick(level, l, husk, true, true, a -> act[0] = a);
			ticks[0]++;
			if (act[0] == TowerCrew.ACT_THRUST && before != TowerCrew.ACT_THRUST) {
				// the lunge takes the point in (they're held still here: the husk is brought to where the lunge would end)
				husk.snapTo(at.getX() + 1.55, at.getY(), at.getZ() + 0.5, 90, 0);
			}
			float blow = last[0] - husk.getHealth();
			last[0] = husk.getHealth();
			if (ticks[0] == 1) {
				helper.assertTrue(has && fight.out() && act[0] == TowerCrew.ACT_BLADE, "out at once for a husk in reach");
			}
			if (ticks[0] < deploy) {
				helper.assertTrue(act[0] == TowerCrew.ACT_BLADE && blow == 0, "no move while it deploys");
			}
			if (stage[0] == 0 && blow > 0) {
				// (a husk's armour takes a little off: a thrust's 24 lands as about 23.6, a slash's 18 as about 17.7)
				helper.assertTrue(blow > 21 && blow <= io.github.jimbozoomer.jugcraft.tower.crew.PlasmaBlade.THRUST_DAMAGE, "at arm's length a thrust: " + blow);
				husk.snapTo(at.getX() + 1.3, at.getY(), at.getZ() + 0.5, 90, 0);          // close: 1 block from its side
				stage[0] = 1;
			} else if (stage[0] == 1 && blow > 0) {
				helper.assertTrue(blow > 15 && blow <= io.github.jimbozoomer.jugcraft.tower.crew.PlasmaBlade.SLASH_DAMAGE, "close, a slash: " + blow);
				husk.snapTo(at.getX() + 10.5, at.getY(), at.getZ() + 0.5, 90, 0);         // out of reach
				stage[0] = 2;
				ticks[0] = 1000;
			} else if (stage[0] == 2 && !has) {
				helper.assertTrue(!fight.out(), "stowed with nothing in reach");
				helper.assertTrue(ticks[0] - 1000 <= io.github.jimbozoomer.jugcraft.tower.crew.PlasmaBlade.STOW_AFTER
						+ io.github.jimbozoomer.jugcraft.tower.crew.PlasmaBlade.MOVE_TICKS + 2, "within a second or so: " + (ticks[0] - 1000));
				stage[0] = 3;
				l.discard();
				husk.discard();
				helper.succeed();
			}
		});
	}

	/** A squad of {@code n} for {@code owner} standing round {@code at} (on the record, as out of their pods). */
	private static List<Legionary> squadAt(ServerLevel level, UUID owner, Vec3 at, int n) {
		UUID squad = UUID.randomUUID();
		List<Legionary> out = new java.util.ArrayList<>();
		for (int k = 0; k < n; k++) {
			Legionary l = JugcraftSquads.LEGIONARY.create(level, EntitySpawnReason.TRIGGERED);
			if (l == null) {
				continue;
			}
			l.snapTo(at.x + (k % 3 - 1) * 2.0, at.y, at.z + (k / 3) * 2.0, 180, 0);
			l.setUp(owner, squad, JugcraftSquads.index(n), k == 0, k);
			level.addFreshEntity(l);
			SquadAccounts.get(level).out(owner, l.getUUID());
			out.add(l);
		}
		return out;
	}

	@GameTest(environment = "jugcraft-test:drop_squad", structure = "jugcraft-test:tatterlace_arena", maxTicks = 600)
	public void leaderOrdersHoldPatrolFollowNotHomeInAFight(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		ServerLevel level = helper.getLevel();
		BlockPos centre = pad(helper);
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.snapTo(centre.getX() + 0.5, centre.getY(), centre.getZ() - 2.5, 0, 0);
		List<Legionary> men = squadAt(level, player.getUUID(), Vec3.atBottomCenterOf(centre), 3);
		helper.assertTrue(men.size() == 3, "a squad of three");
		Legionary lead = men.get(0);
		long start = level.getGameTime();
		Vec3[] spot = new Vec3[1];
		helper.onEachTick(() -> {
			long t = level.getGameTime() - start;
			if (t == 90) {
				helper.assertTrue(SquadOrders.order(level, lead, Legionary.HOLD, 200, null) == SquadOrders.OK, "hold");
				spot[0] = lead.anchor();
				helper.assertTrue(spot[0] != null && men.stream().allMatch(l -> l.order() == Legionary.HOLD), "the whole squad holds, at the leader's spot");
				player.teleportTo(centre.getX() + 0.5, centre.getY(), centre.getZ() + 18.5);
			}
			if (t == 200) {
				helper.assertTrue(lead.position().distanceTo(spot[0]) < 2.5, "the leader stays on the spot with his player gone off: "
						+ lead.position().distanceTo(spot[0]));
				for (Legionary l : men.subList(1, men.size())) {
					double d = l.position().distanceTo(spot[0]);
					helper.assertTrue(Math.abs(d - Legionary.RING) < 1.6, "the rest in a ring round it: " + d);
				}
				// a fight: not sent home
				var pig = net.minecraft.world.entity.EntityTypes.PIG.create(level, EntitySpawnReason.TRIGGERED);
				helper.assertTrue(pig != null, "a pig");
				pig.snapTo(centre.getX() + 6.5, centre.getY(), centre.getZ() + 0.5, 0, 0);
				level.addFreshEntity(pig);
				men.get(1).setLastHurtByMob(pig);
				helper.assertTrue(SquadOrders.order(level, lead, Legionary.HOME, 200, null) == SquadOrders.IN_COMBAT, "not sent home in a fight");
				pig.discard();
				helper.assertTrue(SquadOrders.order(level, lead, Legionary.PATROL, 10, null) == SquadOrders.OK
						&& men.stream().allMatch(l -> l.order() == Legionary.PATROL && l.radius() == Legionary.PATROL_MIN), "a patrol, its radius at least 50");
				helper.assertTrue(SquadOrders.order(level, lead, Legionary.FOLLOW, 200, null) == SquadOrders.OK, "follow");
			}
			if (t == 330) {
				double d = lead.position().distanceTo(player.position());
				helper.assertTrue(d < Legionary.LEAD_SLACK + 2, "following, the leader's come up to his player: " + d);
				men.forEach(Legionary::discard);
				SquadAccounts.get(level).clearField(player.getUUID());
				helper.succeed();
			}
		});
	}

	/** The pad, and a stone road from its north edge {@code length} blocks on (+z), clear above, its chunks kept loaded. */
	private static BlockPos road(GameTestHelper helper, int length) {
		ServerLevel level = helper.getLevel();
		BlockPos centre = pad(helper);
		for (int x = 14; x < 34; x++) {
			for (int z = 48; z < 48 + length; z++) {
				level.setBlock(helper.absolutePos(new BlockPos(x, 0, z)), Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
				for (int y = 1; y < 16; y++) {
					BlockPos up = helper.absolutePos(new BlockPos(x, y, z));
					if (!level.getBlockState(up).isAir()) {
						level.setBlock(up, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
					}
				}
			}
		}
		for (int z = 40; z <= 48 + length + 16; z += 16) {
			for (int x = -16; x <= 16; x += 16) {
				level.setChunkForced((centre.getX() + x) >> 4, (centre.getZ() - 24 + z) >> 4, true);
			}
		}
		return centre;
	}

	/**
	 * Justin, 11 November 2026: "I flew around for a second ... and now they're just not following me ... players might have
	 * an ability to fly later on, so I don't want anything to interfere with ... these guys following them". A following
	 * squad whose player flies off 80 blocks, 14 up, is with them again within seconds: on the ground under them, all of
	 * them, none left behind.
	 */
	@GameTest(environment = "jugcraft-test:drop_squad", structure = "jugcraft-test:tatterlace_arena", maxTicks = 500)
	public void squadKeepsUpWithAPlayerWhoFliesOff(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		ServerLevel level = helper.getLevel();
		BlockPos centre = road(helper, 100);
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.snapTo(centre.getX() + 0.5, centre.getY(), centre.getZ() - 2.5, 0, 0);
		List<Legionary> men = squadAt(level, player.getUUID(), Vec3.atBottomCenterOf(centre), 5);
		helper.assertTrue(men.size() == 5, "a squad of five");
		long start = level.getGameTime();
		Vec3 sky = new Vec3(centre.getX() + 0.5, centre.getY() + 14, centre.getZ() + 80.5);
		helper.onEachTick(() -> {
			long t = level.getGameTime() - start;
			if (t >= 90) {
				// flying off up the road and hanging there (the mock player doesn't move by itself)
				player.getAbilities().flying = true;
				player.teleportTo(sky.x, sky.y, sky.z);
				player.setOnGround(false);
			}
			if (t >= 100) {
				boolean all = true;
				StringBuilder where = new StringBuilder();
				for (Legionary l : men) {
					double dx = l.getX() - sky.x, dz = l.getZ() - sky.z;
					double across = Math.sqrt(dx * dx + dz * dz);
					boolean with = across <= Legionary.LEASH && l.onGround() && Math.abs(l.getY() - centre.getY()) < 1.5;
					all &= with;
					where.append(String.format(" %.1f across, y %.1f;", across, l.getY() - centre.getY()));
				}
				if (all) {
					helper.assertTrue(t < 300, "with him within ten seconds, got " + t + " ticks");
					men.forEach(Legionary::discard);
					SquadAccounts.get(level).clearField(player.getUUID());
					helper.succeed();
				} else if (t > 480) {
					helper.fail("the squad on the ground under their flying player:" + where);
				}
			}
		});
	}

	/**
	 * Following, a fight doesn't keep them from their player: with him walking off 30 blocks they let go of the mob they're
	 * shooting at (a tough one, so it isn't over at once) and come up to him; back with it, they take it on again.
	 */
	@GameTest(environment = "jugcraft-test:drop_squad", structure = "jugcraft-test:tatterlace_arena", maxTicks = 700)
	public void followingSquadLetsAFightGoToKeepUp(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		ServerLevel level = helper.getLevel();
		BlockPos centre = road(helper, 60);
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.snapTo(centre.getX() + 0.5, centre.getY(), centre.getZ() + 2.5, 0, 0);
		List<Legionary> men = squadAt(level, player.getUUID(), Vec3.atBottomCenterOf(centre), 3);
		var husk = net.minecraft.world.entity.EntityTypes.HUSK.create(level, EntitySpawnReason.TRIGGERED);
		helper.assertTrue(husk != null, "a husk");
		husk.snapTo(centre.getX() + 0.5, centre.getY(), centre.getZ() - 14.5, 0, 0);
		husk.setNoAi(true);
		husk.setPersistenceRequired();
		husk.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(2000);
		husk.setHealth(2000);
		level.addFreshEntity(husk);
		long start = level.getGameTime();
		int[] stage = {0};
		long[] since = {0};
		helper.onEachTick(() -> {
			long t = level.getGameTime() - start;
			husk.setHealth(2000);                                   // (a fight that goes on)
			if (stage[0] == 0 && men.stream().anyMatch(l -> l.getTarget() == husk)) {
				stage[0] = 1;
				since[0] = t;
				player.teleportTo(centre.getX() + 0.5, centre.getY(), centre.getZ() + 30.5);   // he walks off up the road
			} else if (stage[0] == 1) {
				boolean let = men.stream().noneMatch(l -> l.getTarget() == husk);
				double d = men.get(0).distanceTo(player);
				if (let && d < Legionary.LEAD_SLACK + 2) {
					stage[0] = 2;
					since[0] = t;
					player.teleportTo(centre.getX() + 0.5, centre.getY(), centre.getZ() - 4.5);  // and back by the husk
				} else if (t - since[0] > 300) {
					helper.fail("following, they let the fight go and come up to him: targets "
							+ men.stream().map(l -> l.getTarget() == null ? "none" : l.getTarget().getType().toShortString()).toList() + ", leader " + d + " off");
				}
			} else if (stage[0] == 2) {
				if (men.stream().anyMatch(l -> l.getTarget() == husk)) {
					husk.discard();
					men.forEach(Legionary::discard);
					SquadAccounts.get(level).clearField(player.getUUID());
					helper.succeed();
				} else if (t - since[0] > 300) {
					helper.fail("back with it, they take it on again");
				}
			} else if (stage[0] == 0 && t > 300) {
				helper.fail("they take on the husk to begin with");
			}
		});
	}

	@GameTest(environment = "jugcraft-test:drop_squad_home", structure = "jugcraft-test:tatterlace_arena", maxTicks = 1500)
	public void sentHomeAShipComesTheyBoardAndItLeaves(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		ServerLevel level = helper.getLevel();
		pad(helper);
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		BlockPos squadAt = helper.absolutePos(new BlockPos(24, 1, 40));
		player.snapTo(squadAt.getX() + 4.5, squadAt.getY(), squadAt.getZ() + 3.5, 180, 0);
		List<Legionary> men = squadAt(level, player.getUUID(), Vec3.atBottomCenterOf(squadAt), 3);
		helper.assertTrue(men.size() == 3, "a squad of three");
		Legionary lead = men.get(0);
		UUID squad = lead.squad();
		// the ship sets down on the pad, its ramp toward them (its stern +z)
		BlockPos land = helper.absolutePos(new BlockPos(24, 1, 4));
		var site = new io.github.jimbozoomer.jugcraft.tower.crew.CrewShipSite.Site(Vec3.atBottomCenterOf(land), 0F, false,
				io.github.jimbozoomer.jugcraft.tower.crew.CrewShipFlight.HINGE_Y, Vec3.atBottomCenterOf(squadAt));
		helper.assertTrue(SquadOrders.order(level, lead, Legionary.HOME, 200, site) == SquadOrders.OK, "sent home");
		helper.assertTrue(men.stream().allMatch(l -> l.order() == Legionary.HOME), "the whole squad");
		helper.assertTrue(SquadOrders.order(level, lead, Legionary.HOME, 200, site) == SquadOrders.ON_THE_WAY, "not twice");
		var ship = level.getEntitiesOfClass(io.github.jimbozoomer.jugcraft.tower.crew.CrewShip.class, new AABB(land).inflate(4),
				io.github.jimbozoomer.jugcraft.tower.crew.CrewShip::pickup).stream().findFirst().orElse(null);
		helper.assertTrue(ship != null && squad.equals(ship.pickupSquad()), "a pickup ship for them");
		ship.testPhase(io.github.jimbozoomer.jugcraft.tower.crew.CrewShipFlight.OPEN);   // (its flight in skipped)
		boolean[] boarded = {false};
		helper.onEachTick(() -> {
			List<Legionary> left = level.getEntitiesOfClass(Legionary.class, new AABB(squadAt).inflate(80), l -> squad.equals(l.squad()));
			if (!boarded[0] && left.isEmpty()) {
				var looks = ship.passengerLooks();
				helper.assertTrue(looks.size() == 3 && looks.stream().allMatch(java.util.Objects::nonNull), "all three aboard: " + looks);
				helper.assertTrue((looks.get(2).gear() & Legionary.CRESTS[0]) != 0, "the decanus last");
				helper.assertTrue(!SquadAccounts.get(level).inField(level.getServer(), player.getUUID()), "the squad's home: the next can be called");
				boarded[0] = true;
			}
			if (boarded[0] && ship.isRemoved()) {
				helper.succeed();
			}
		});
	}

	/**
	 * Squad prices (Justin, 11 November 2026): the first of each size three times what it was, and each one a player
	 * requisitions raises that size's price for them by 10% (compounding, each payment rounded up), no one else's and no
	 * other size's.
	 */
	@GameTest
	public void squadPricesTripledAndRiseTenPercentEachBuy(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		helper.assertTrue(java.util.Arrays.equals(JugcraftSquads.COST[0], new int[] {288, 192, 72, 18})
				&& java.util.Arrays.equals(JugcraftSquads.COST[1], new int[] {480, 336, 120, 30})
				&& java.util.Arrays.equals(JugcraftSquads.COST[2], new int[] {672, 480, 192, 48}), "the base prices tripled");
		helper.assertTrue(java.util.Arrays.equals(JugcraftSquads.price(0, 0), JugcraftSquads.COST[0]), "the first at the base price");
		helper.assertTrue(java.util.Arrays.equals(JugcraftSquads.price(0, 1), new int[] {317, 212, 80, 20}), "the second 10% up, rounded up: "
				+ java.util.Arrays.toString(JugcraftSquads.price(0, 1)));
		helper.assertTrue(java.util.Arrays.equals(JugcraftSquads.price(0, 2), new int[] {349, 233, 88, 22}), "the third 10% on that: "
				+ java.util.Arrays.toString(JugcraftSquads.price(0, 2)));
		helper.assertTrue(java.util.Arrays.equals(JugcraftSquads.price(2, 3), new int[] {895, 639, 256, 64}), "a squad of 7 after three: "
				+ java.util.Arrays.toString(JugcraftSquads.price(2, 3)));
		SquadAccounts accounts = SquadAccounts.get(helper.getLevel());
		UUID a = UUID.randomUUID(), b = UUID.randomUUID();
		helper.assertTrue(java.util.Arrays.equals(accounts.price(a, 0), JugcraftSquads.COST[0]), "a new player pays the base price");
		accounts.bought(a, 0);
		helper.assertTrue(java.util.Arrays.equals(accounts.price(a, 0), JugcraftSquads.price(0, 1)), "one bought: their next squad of 3 costs 10% more");
		helper.assertTrue(java.util.Arrays.equals(accounts.price(a, 1), JugcraftSquads.COST[1]) && java.util.Arrays.equals(accounts.price(a, 2),
				JugcraftSquads.COST[2]), "their other sizes as they were");
		helper.assertTrue(java.util.Arrays.equals(accounts.price(b, 0), JugcraftSquads.COST[0]), "another player's prices as they were");
		accounts.bought(a, 0);
		helper.assertTrue(accounts.bought(a)[0] == 2 && java.util.Arrays.equals(accounts.price(a, 0), JugcraftSquads.price(0, 2)), "two bought");
		int[] have = JugcraftSquads.price(0, 2);
		helper.assertTrue(Requisitions.affords(have, accounts.price(a, 0)) && !Requisitions.affords(JugcraftSquads.price(0, 1), accounts.price(a, 0)),
				"the tower must hold their price now, not the base");
		helper.succeed();
	}

	/**
	 * A squad's day in the field (Justin, 11 November 2026: "the legionaries will automatically call a drop ship to return
	 * one in game day after they're called in"): a minute before it's up the squad is warned (their player told), and once
	 * it's up the leader calls a ship home himself, as SEND HOME does; it finds a landing zone and comes for them.
	 */
	@GameTest(environment = "jugcraft-test:drop_squad_home", structure = "jugcraft-test:tatterlace_arena", maxTicks = 600)
	public void squadCallsTheShipHomeWhenItsDayIsUp(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		ServerLevel level = helper.getLevel();
		BlockPos centre = bigPad(helper, 72);
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.snapTo(centre.getX() + 0.5, centre.getY(), centre.getZ() + 6.5, 180, 0);
		List<Legionary> men = squadAt(level, player.getUUID(), Vec3.atBottomCenterOf(centre), 3);
		helper.assertTrue(men.size() == 3, "a squad of three");
		Legionary lead = men.get(0);
		UUID squad = lead.squad();
		long now = level.getGameTime();
		// called in a day ago, less a minute and five seconds: dazed a few seconds, then the minute's warning
		men.forEach(l -> l.setCalledAt(now - Legionary.FIELD_TICKS + Legionary.RECALL_WARNING + 100));
		helper.assertTrue(lead.recallAt() == now + Legionary.RECALL_WARNING + 100, "the ship due a day after the call-in");
		long[] upAt = {-1};
		helper.onEachTick(() -> {
			long t = level.getGameTime() - now;
			if (t == 140) {
				helper.assertTrue(men.stream().allMatch(Legionary::warned), "warned a minute before: " + men.stream().map(Legionary::warned).toList());
				helper.assertTrue(lead.order() != Legionary.HOME, "not sent home yet");
				// (the minute passes)
				men.forEach(l -> l.setCalledAt(level.getGameTime() - Legionary.FIELD_TICKS));
				upAt[0] = level.getGameTime();
			}
			if (upAt[0] >= 0 && lead.order() == Legionary.HOME) {
				helper.assertTrue(level.getGameTime() - upAt[0] <= Legionary.RECALL_RETRY + 2, "called as soon as the day was up: "
						+ (level.getGameTime() - upAt[0]) + " ticks");
				helper.assertTrue(men.stream().allMatch(l -> l.order() == Legionary.HOME), "the whole squad sent home");
				var ship = level.getEntitiesOfClass(io.github.jimbozoomer.jugcraft.tower.crew.CrewShip.class, new AABB(centre).inflate(120),
						s -> s.pickup() && squad.equals(s.pickupSquad())).stream().findFirst().orElse(null);
				helper.assertTrue(ship != null, "a ship on its way for them");
				ship.discard();
				men.forEach(Legionary::discard);
				SquadAccounts.get(level).clearField(player.getUUID());
				helper.succeed();
			}
		});
	}

	/** A stone pad {@code r} blocks round the structure's middle, clear to 14 up and kept loaded; its middle. */
	private static BlockPos bigPad(GameTestHelper helper, int r) {
		ServerLevel level = helper.getLevel();
		BlockPos centre = helper.absolutePos(new BlockPos(24, 1, 24));
		for (int x = -r; x <= r; x++) {
			for (int z = -r; z <= r; z++) {
				level.setBlock(centre.offset(x, -1, z), Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
				for (int y = 0; y < 14; y++) {
					BlockPos up = centre.offset(x, y, z);
					if (!level.getBlockState(up).isAir()) {
						level.setBlock(up, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
					}
				}
			}
		}
		for (int x = -r - 16; x <= r + 16; x += 16) {
			for (int z = -r - 16; z <= r + 16; z += 16) {
				level.setChunkForced((centre.getX() + x) >> 4, (centre.getZ() + z) >> 4, true);
			}
		}
		return centre;
	}

	/**
	 * The squad's film (tower/squad/SquadFilm; Justin, 11 November 2026): on open flat ground the seven pods come down
	 * apart, the soldiers form up on the spot they're ordered to hold, clear of the pods, so every pod blows itself apart;
	 * then the Warden comes up, goes for them, and they take it down (it's hit, again and again, and dies), most of them
	 * standing. Timings go to the log for the film camera.
	 */
	@GameTest(environment = "jugcraft-test:squad_film", structure = "jugcraft-test:tatterlace_arena", maxTicks = 3200)
	public void squadFilmDropFormUpAndTakeDownAWarden(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		ServerLevel level = helper.getLevel();
		BlockPos centre = helper.absolutePos(new BlockPos(24, 1, 24));
		int r = 72;
		for (int x = -r; x <= r; x++) {
			for (int z = -r; z <= r; z++) {
				level.setBlock(centre.offset(x, -1, z), Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
				for (int y = 0; y < 14; y++) {
					BlockPos up = centre.offset(x, y, z);
					if (!level.getBlockState(up).isAir()) {
						level.setBlock(up, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
					}
				}
			}
		}
		for (int x = -r - 16; x <= r + 16; x += 16) {
			for (int z = -r - 16; z <= r + 16; z += 16) {
				level.setChunkForced((centre.getX() + x) >> 4, (centre.getZ() + z) >> 4, true);
			}
		}
		// the mark off-centre so the squad's spot and the Warden's ground are on the pad too
		Vec3 mark = Vec3.atBottomCenterOf(centre.offset(22, 0, 0));
		StringBuilder note = new StringBuilder();
		SquadFilm film = SquadFilm.at(level, mark, new Vec3(1, 0, 0), note);
		helper.assertTrue(film != null, "a scene: " + note);
		helper.assertTrue(Math.abs(film.camSky.y - (mark.y + 2.2)) < 0.01, "the camera's eye over the ground: " + film.camSky);
		UUID owner = UUID.randomUUID();
		// no monsters coming in (as in the film's world): the squad's only foe the Warden
		boolean spawned = level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.SPAWN_MONSTERS);
		level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.SPAWN_MONSTERS, false, level.getServer());
		film.clear(level, owner);
		film.launch(level, owner);
		List<DropPod> launched = film.pods(level);
		helper.assertTrue(launched.size() == SquadFilm.SIZE, "seven pods launched, not " + launched.size());
		// the flare over the tower's side (the seed's first draw)
		helper.assertTrue(launched.stream().filter(p -> p.place() == 0).allMatch(p -> p.flare().x() > 30 && Math.abs(p.flare().z()) < 8),
				"the leader's flare toward +x: " + launched.get(0).flare());
		long start = level.getGameTime();
		int[] wardenAt = {-1};
		// the film's camera flown alongside: what it frames, shot by shot
		SquadFilmCamera cam = new SquadFilmCamera(film);
		Map<SquadFilmCamera.Shot, int[]> seen = new java.util.EnumMap<>(SquadFilmCamera.Shot.class);
		double[] nearest = {1e9, 1e9};                      // the camera's least distance from the Warden's middle, from a soldier
		helper.onEachTick(() -> {
			film.tick(level);
			long now = level.getGameTime(), age = now - start;
			cam.step(level, now, true);
			int[] count = seen.computeIfAbsent(cam.shot, k -> new int[2]);
			count[0]++;
			Vec3 view = Vec3.directionFromRotation(cam.pitch, cam.yaw);
			BlockPos in = BlockPos.containing(cam.pos);
			helper.assertTrue(level.getBlockState(in).getCollisionShape(level, in).isEmpty(), "the camera in a block at " + cam.tag(now));
			for (Legionary l : film.squad(level)) {
				double dl = Math.sqrt(l.getBoundingBox().distanceToSqr(cam.pos));
				nearest[1] = Math.min(nearest[1], dl);
				if (dl < 1.0) {
					System.out.println("[squad film] near a soldier: " + cam.tag(now) + " " + String.format(java.util.Locale.ROOT, "%.2f", dl) + " man at " + SquadFilm.fmt(l.position()));
				}
			}
			net.minecraft.world.entity.Entity w = level.getEntity(film.wardenId);
			if (w != null) {
				nearest[0] = Math.min(nearest[0], cam.pos.distanceTo(w.position().add(0, 1.5, 0)));
			}
			// framed: the falling pods while they fall, the Warden while it rises and fights
			if (cam.shot == SquadFilmCamera.Shot.SKY && age > 70) {
				boolean any = false;
				for (DropPod pd : film.pods(level)) {
					if (age >= pd.setsOff() && age < pd.land()) {
						org.joml.Vector3f at = pd.at(age, new org.joml.Vector3f());
						Vec3 to = pd.position().add(at.x, at.y, at.z).subtract(cam.pos).normalize();
						any |= Math.toDegrees(Math.acos(Math.min(1, to.dot(view)))) < 32;
					}
				}
				count[1] += any ? 1 : 0;
			} else if ((cam.shot == SquadFilmCamera.Shot.FIGHT || cam.shot == SquadFilmCamera.Shot.RISE) && w != null) {
				Vec3 to = w.position().add(0, 1.5, 0).subtract(cam.pos).normalize();
				count[1] += Math.toDegrees(Math.acos(Math.min(1, to.dot(view)))) < 25 ? 1 : 0;
			}
			if (wardenAt[0] < 0 && (cam.wantWarden || age > 1300)) {
				if (film.formedAt < 0) {
					StringBuilder why = new StringBuilder();
					for (DropPod pd : film.pods(level)) {
						why.append(" pod ").append(pd.place()).append(" at ").append(SquadFilm.fmt(pd.position()));
					}
					for (Legionary l : film.squad(level)) {
						why.append(" | man ").append(l.place()).append(" at ").append(SquadFilm.fmt(l.position())).append(" order ").append(l.order())
								.append(" dazed ").append(l.dazed()).append(" target ").append(l.getTarget()).append(" nav ").append(l.getNavigation().isDone())
								.append(" from spot ").append(String.format(java.util.Locale.ROOT, "%.1f", Math.hypot(l.getX() - film.anchor.x, l.getZ() - film.anchor.z)));
					}
					helper.assertTrue(false, "formed up by " + age + ": " + film.events() + why + " spot " + SquadFilm.fmt(film.anchor));
				}
				helper.assertTrue(film.podsLeft == 0, "every pod blew itself apart: " + film.podsLeft + " left");
				helper.assertTrue(film.menAlive == SquadFilm.SIZE, "all seven out and standing: " + film.menAlive);
				film.raiseWarden(level);
				wardenAt[0] = (int) age;
			}
			if (cam.shot == SquadFilmCamera.Shot.END) {
				StringBuilder shots = new StringBuilder();
				seen.forEach((k, v) -> shots.append(k).append(' ').append(v[0]).append(" ticks (framed ").append(v[1]).append(") "));
				String report = "formed " + (film.formedAt - start) + ", Warden up " + (film.wardenUp - start) + ", emerged "
						+ (film.wardenEmerged - start) + ", died " + (film.wardenDied - start) + ", " + film.wardenHits + " hits, " + film.menAlive
						+ " standing; camera: " + shots + String.format(java.util.Locale.ROOT, "nearest the Warden %.1f, a soldier %.1f; ", nearest[0], nearest[1])
						+ cam.cues + "; " + film.events();
				System.out.println("[squad film] " + report);
				helper.assertTrue(film.wardenDied >= 0 && film.wardenHits >= 15, "they took it down, hit again and again: " + report);
				helper.assertTrue(film.menAlive >= 1, "some of them standing: " + report);
				int[] sky = seen.get(SquadFilmCamera.Shot.SKY), fight = seen.get(SquadFilmCamera.Shot.FIGHT), rise = seen.get(SquadFilmCamera.Shot.RISE);
				helper.assertTrue(sky != null && sky[1] > (sky[0] - 70) * 0.8, "the falling pods in the picture: " + report);
				helper.assertTrue(fight != null && fight[1] > fight[0] * 0.85 && rise != null && rise[1] > rise[0] * 0.6, "the Warden in the picture: " + report);
				helper.assertTrue(nearest[0] > 2.5 && nearest[1] > SquadFilmCamera.CLEAR - 0.05, "the camera clear of the Warden and the soldiers: " + report);
				for (SquadFilmCamera.Shot k : SquadFilmCamera.Shot.values()) {
					helper.assertTrue(k == SquadFilmCamera.Shot.END || seen.containsKey(k), "every shot: " + k + " missing: " + report);
				}
				film.squad(level).forEach(Legionary::discard);
				SquadAccounts.get(level).clearField(owner);
				level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.SPAWN_MONSTERS, spawned, level.getServer());
				helper.succeed();
			}
		});
	}
}
