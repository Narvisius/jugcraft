package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.FireworkShape;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.ShowLauncherBlock;
import io.github.jimbozoomer.jugcraft.agriculture.ShowLauncherBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.SpookyFireworkItem;
import io.github.jimbozoomer.jugcraft.agriculture.SpookyRocket;
import java.util.List;
import java.util.Optional;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * In-game tests for spooky fireworks: the pictures, a rocket set off by hand (it climbs for its flight time and bursts,
 * breaking and hurting nothing), flight and twinkle from the recipe, the Show Launcher (loading, hoppers' sides,
 * comparators, a show in sequence, in volleys and as a finale, fanned out, vanilla rockets too, redstone and an empty hand
 * to start and stop, sneaking to change mode, spilling when broken), dispensers, and data.
 *
 * <p>The test areas' barrier ceilings are lifted where a rocket should be seen to climb; elsewhere a rocket that meets
 * the ceiling just bursts there.
 */
public class FireworkGameTests {
	private static Item item(String id) {
		return JugcraftAgriculture.item(id);
	}

	private static ItemStack firework(FireworkShape shape, int count, int flight) {
		ItemStack stack = new ItemStack(item(shape.item()), count);
		stack.set(DataComponents.FIREWORKS, new Fireworks(flight, List.of()));
		return stack;
	}

	private static void floor(GameTestHelper helper) {
		for (int x = 0; x <= 7; x++) {
			for (int z = 0; z <= 7; z++) {
				helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
			}
		}
	}

	/** Lifts the test's barrier ceiling over (x, z), so a rocket climbs freely there. */
	private static void openSky(GameTestHelper helper, int x, int z) {
		for (int y = 3; y <= 24; y++) {
			if (helper.getBlockState(new BlockPos(x, y, z)).is(Blocks.BARRIER)) {
				helper.setBlock(new BlockPos(x, y, z), Blocks.AIR);
			}
		}
	}

	private static ServerPlayer player(GameTestHelper helper, BlockPos standAt, ItemStack held) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		BlockPos absolute = helper.absolutePos(standAt);
		player.setPos(absolute.getX() + 0.5, absolute.getY(), absolute.getZ() + 0.5);
		player.setItemInHand(InteractionHand.MAIN_HAND, held);
		return player;
	}

	private static InteractionResult use(GameTestHelper helper, ServerPlayer player, BlockPos pos) {
		BlockPos absolute = helper.absolutePos(pos);
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute).relative(Direction.UP, 0.5), Direction.UP, absolute, false);
		return player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
	}

	private static ShowLauncherBlockEntity launcher(GameTestHelper helper, BlockPos pos, ShowLauncherBlock.Mode mode) {
		helper.setBlock(pos, JugcraftAgriculture.block("show_launcher").defaultBlockState().setValue(ShowLauncherBlock.MODE, mode));
		return helper.getBlockEntity(pos, ShowLauncherBlockEntity.class);
	}

	private static List<SpookyRocket> rockets(GameTestHelper helper, BlockPos around, double reach) {
		return helper.getLevel().getEntitiesOfClass(SpookyRocket.class, new AABB(helper.absolutePos(around)).inflate(reach, 16.0, reach));
	}

	// ---------------------------------------------------------------- the pictures

	/** Every picture is well formed and has a firework that bursts into it, flight 1 unless crafted otherwise. */
	@GameTest(maxTicks = 20)
	public void everyPictureHasItsFirework(GameTestHelper helper) {
		for (FireworkShape shape : FireworkShape.values()) {
			helper.assertTrue(shape.wellFormed() && shape.sparks() >= 40, shape + " is a picture of " + shape.sparks() + " sparks");
			Item firework = item(shape.item());
			helper.assertTrue(firework instanceof SpookyFireworkItem spooky && spooky.shape() == shape, shape + "'s firework bursts into it");
			Fireworks fireworks = new ItemStack(firework).get(DataComponents.FIREWORKS);
			helper.assertTrue(fireworks != null && fireworks.flightDuration() == 1, "A plain " + shape.item() + " has flight 1");
		}
		helper.assertTrue(FireworkShape.PUMPKIN.colourAt(4, 0) == -1 && FireworkShape.PUMPKIN.colourAt(5, 0) == 0x58C83C,
				"The jack o'lantern has a green stem and holes round it");
		helper.succeed();
	}

	// ---------------------------------------------------------------- set off by hand

	/** Used on the ground, a spooky firework goes up (one used), climbs for its flight time and bursts, breaking and hurting nothing. */
	@GameTest(maxTicks = 80)
	public void aSpookyFireworkClimbsAndBursts(GameTestHelper helper) {
		floor(helper);
		BlockPos ground = new BlockPos(4, 1, 4);
		openSky(helper, 4, 4);
		ServerPlayer player = player(helper, new BlockPos(2, 2, 2), firework(FireworkShape.GHOST, 2, 1));
		float health = player.getHealth();
		use(helper, player, ground);
		helper.assertTrue(player.getMainHandItem().getCount() == 1, "One firework is used");
		List<SpookyRocket> launched = rockets(helper, ground, 1.0);
		helper.assertTrue(launched.size() == 1, "One rocket goes up: " + launched.size());
		SpookyRocket rocket = launched.get(0);
		helper.assertTrue(rocket.shape() == FireworkShape.GHOST && !rocket.twinkles(), "It is a ghost, not twinkling");
		int flight = SpookyRocket.LIFETIME_BASE * 2;
		helper.assertTrue(rocket.lifetime() >= flight && rocket.lifetime() <= flight + SpookyRocket.LIFETIME_SPREAD,
				"Flight 1 flies " + flight + " to " + (flight + SpookyRocket.LIFETIME_SPREAD) + " ticks: " + rocket.lifetime());
		double start = rocket.getY();
		helper.runAfterDelay(10, () -> helper.assertTrue(!rocket.isRemoved() && rocket.getY() > start + 1.5,
				"After half a second it is still climbing: " + (rocket.getY() - start) + " blocks up"));
		helper.succeedWhen(() -> {
			helper.assertTrue(rocket.isRemoved(), "Waiting for it to burst (life " + rocket.life() + " of " + rocket.lifetime() + ")");
			helper.assertTrue(helper.getBlockState(ground).is(Blocks.STONE) && player.getHealth() == health, "Nothing is broken or hurt");
		});
	}

	/** Flight 3 flies longer; a twinkling firework twinkles; the recipes make three of the flight and twinkle crafted. */
	@GameTest(maxTicks = 20)
	public void flightAndTwinkleComeFromTheRecipe(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ItemStack high = firework(FireworkShape.BAT, 1, 3);
		high.set(JugcraftAgriculture.TWINKLE, true);
		SpookyRocket rocket = new SpookyRocket(level, 0.0, 0.0, 0.0, high, Vec3.ZERO, false);
		int flight = SpookyRocket.LIFETIME_BASE * 4;
		helper.assertTrue(rocket.lifetime() >= flight && rocket.lifetime() <= flight + SpookyRocket.LIFETIME_SPREAD,
				"Flight 3 flies " + flight + " to " + (flight + SpookyRocket.LIFETIME_SPREAD) + " ticks: " + rocket.lifetime());
		helper.assertTrue(rocket.twinkles() && rocket.shape() == FireworkShape.BAT, "It twinkles, and is a bat");
		for (FireworkShape shape : FireworkShape.values()) {
			for (int f = 1; f <= 3; f++) {
				for (String twinkle : new String[] {"", "_twinkle"}) {
					String id = shape.item() + "_" + f + twinkle;
					helper.assertTrue(level.recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id(id))).isPresent(), "Recipe " + id + " loads");
				}
			}
		}
		Optional<RecipeHolder<?>> recipe = level.recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id("pumpkin_firework_2_twinkle")));
		helper.assertTrue(recipe.isPresent() && recipe.get().value() instanceof CraftingRecipe, "The twinkling pumpkin recipe is a crafting recipe");
		CraftingRecipe crafting = (CraftingRecipe) recipe.get().value();
		CraftingInput input = CraftingInput.of(3, 2, List.of(new ItemStack(Items.PAPER), new ItemStack(Items.GUNPOWDER), new ItemStack(Items.GUNPOWDER),
				new ItemStack(Items.CARVED_PUMPKIN), new ItemStack(Items.GLOWSTONE_DUST), ItemStack.EMPTY));
		helper.assertTrue(crafting.matches(input, level), "Paper, two gunpowder, a carved pumpkin and glowstone dust match it");
		ItemStack made = crafting.assemble(input);
		Fireworks fireworks = made.get(DataComponents.FIREWORKS);
		helper.assertTrue(made.is(item("pumpkin_firework")) && made.getCount() == 3 && fireworks != null && fireworks.flightDuration() == 2
				&& Boolean.TRUE.equals(made.get(JugcraftAgriculture.TWINKLE)), "It makes three twinkling flight-2 jack o'lantern fireworks: " + made);
		helper.assertTrue(level.recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id("show_launcher"))).isPresent(),
				"The launcher's recipe loads");
		helper.assertTrue(level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,
				Jugcraft.id("blocks/show_launcher"))) != LootTable.EMPTY, "The launcher's loot table loads");
		helper.succeed();
	}

	// ---------------------------------------------------------------- the Show Launcher

	/**
	 * Rockets load sixteen to a tube, matching tubes first; other things don't; hoppers may load rockets from any side and
	 * unload only from below; comparators read the loaded tubes; a full launcher says so; broken, it spills its rockets.
	 */
	@GameTest(maxTicks = 40)
	public void theLauncherLoadsRockets(GameTestHelper helper) {
		floor(helper);
		BlockPos pos = new BlockPos(3, 2, 3);
		ShowLauncherBlockEntity launcher = launcher(helper, pos, ShowLauncherBlock.Mode.SEQUENCE);
		ServerPlayer player = player(helper, new BlockPos(1, 2, 3), firework(FireworkShape.PUMPKIN, 20, 1));
		use(helper, player, pos);
		helper.assertTrue(launcher.tube(0).getCount() == 16 && launcher.tube(1).getCount() == 4 && player.getMainHandItem().isEmpty(),
				"Twenty fill one tube and start a second");
		player.setItemInHand(InteractionHand.MAIN_HAND, firework(FireworkShape.PUMPKIN, 5, 1));
		use(helper, player, pos);
		helper.assertTrue(launcher.tube(1).getCount() == 9 && launcher.tube(2).isEmpty(), "More of the same top up the second tube");
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.FIREWORK_ROCKET, 3));
		use(helper, player, pos);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK, 3));
		use(helper, player, pos);
		helper.assertTrue(launcher.tube(2).is(Items.FIREWORK_ROCKET) && launcher.tube(2).getCount() == 3, "Vanilla rockets load into their own tube");
		helper.assertTrue(player.getMainHandItem().getCount() == 3 && launcher.tube(3).isEmpty(), "Sticks don't load");
		helper.assertTrue(launcher.loadedTubes() == 3 && launcher.rockets() == 28, "Three tubes, 28 rockets");
		helper.assertTrue(helper.getLevel().getBlockState(helper.absolutePos(pos)).getAnalogOutputSignal(helper.getLevel(), helper.absolutePos(pos),
				Direction.NORTH) == 4, "Comparators read three tubes as 4");
		helper.assertTrue(launcher.canPlaceItemThroughFace(5, firework(FireworkShape.SKULL, 1, 1), Direction.UP)
				&& !launcher.canPlaceItemThroughFace(5, new ItemStack(Items.DIRT), Direction.UP)
				&& !launcher.canPlaceItemThroughFace(0, firework(FireworkShape.SKULL, 1, 1), Direction.UP), "Hoppers load rockets into empty or matching tubes");
		helper.assertTrue(launcher.canTakeItemThroughFace(0, launcher.tube(0), Direction.DOWN) && !launcher.canTakeItemThroughFace(0, launcher.tube(0),
				Direction.NORTH), "and unload only from below");
		for (int i = 0; i < 6; i++) {
			launcher.load(firework(FireworkShape.values()[i % 4], 16, 2));
		}
		player.setItemInHand(InteractionHand.MAIN_HAND, firework(FireworkShape.BAT, 1, 3));
		use(helper, player, pos);
		helper.assertTrue(player.getMainHandItem().getCount() == 1 && launcher.loadedTubes() == 9, "A full launcher takes no more");
		int rockets = launcher.rockets();
		helper.getLevel().destroyBlock(helper.absolutePos(pos), true);
		helper.succeedWhen(() -> {
			int spilled = 0;
			for (ItemEntity entity : helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(pos)).inflate(3.0))) {
				spilled += SpookyFireworkItem.rocket(entity.getItem()) ? entity.getItem().getCount() : 0;
			}
			helper.assertTrue(spilled == rockets, "Broken, it spills all " + rockets + " rockets: " + spilled);
		});
	}

	/** In sequence, a show fires one rocket every half second round the loaded tubes in turn, and stops when they are empty. */
	@GameTest(maxTicks = 60)
	public void aShowInSequence(GameTestHelper helper) {
		floor(helper);
		BlockPos pos = new BlockPos(3, 2, 3);
		openSky(helper, 3, 3);
		ShowLauncherBlockEntity launcher = launcher(helper, pos, ShowLauncherBlock.Mode.SEQUENCE);
		launcher.setItem(0, firework(FireworkShape.PUMPKIN, 2, 1));
		launcher.setItem(4, firework(FireworkShape.BAT, 1, 1));
		helper.assertTrue(launcher.start() && launcher.running(), "The show starts");
		int step = ShowLauncherBlockEntity.SEQUENCE_TICKS;
		helper.runAfterDelay(5, () -> helper.assertTrue(launcher.tube(0).getCount() == 1 && launcher.tube(4).getCount() == 1,
				"First the jack o'lantern from the first tube"));
		helper.runAfterDelay(5 + step, () -> helper.assertTrue(launcher.tube(0).getCount() == 1 && launcher.tube(4).isEmpty(),
				"half a second later the bat from the next loaded tube"));
		helper.runAfterDelay(5 + 2 * step, () -> {
			helper.assertTrue(launcher.rockets() == 0 && !launcher.running(), "then round again to the last jack o'lantern, and the show is over");
			helper.succeed();
		});
	}

	/**
	 * In volleys, a show fires a row of three every second, front row first (a vanilla rocket in a row goes up as one);
	 * a finale fires one from every tube at once, fanned out from the middle, and stops.
	 */
	@GameTest(maxTicks = 80)
	public void volleysAndAFinale(GameTestHelper helper) {
		floor(helper);
		BlockPos volley = new BlockPos(1, 2, 1);
		BlockPos finale = new BlockPos(6, 2, 6);
		ShowLauncherBlockEntity rows = launcher(helper, volley, ShowLauncherBlock.Mode.VOLLEY);
		ShowLauncherBlockEntity all = launcher(helper, finale, ShowLauncherBlock.Mode.FINALE);
		for (int tube = 0; tube < ShowLauncherBlockEntity.TUBES; tube++) {
			rows.setItem(tube, tube == 4 ? new ItemStack(Items.FIREWORK_ROCKET) : firework(FireworkShape.SKULL, 1, 1));
			all.setItem(tube, firework(FireworkShape.GHOST, 2, 1));
		}
		rows.start();
		all.start();
		int step = ShowLauncherBlockEntity.VOLLEY_TICKS;
		helper.runAfterDelay(3, () -> {
			helper.assertTrue(rows.tube(0).isEmpty() && rows.tube(1).isEmpty() && rows.tube(2).isEmpty() && rows.rockets() == 6,
					"The front row goes up together");
			helper.assertTrue(all.rockets() == 9 && !all.running(), "The finale fires one from every tube at once, and stops");
			BlockPos centre = helper.absolutePos(finale);
			List<SpookyRocket> fanned = rockets(helper, finale, 1.5);
			helper.assertTrue(fanned.size() == 9, "Nine rockets over the finale launcher: " + fanned.size());
			for (SpookyRocket rocket : fanned) {
				double off = rocket.getX() - (centre.getX() + 0.5);
				helper.assertTrue(Math.abs(off) < 0.1 || Math.signum(off) == Math.signum(rocket.getDeltaMovement().x),
						"Each leans out from the middle: at " + off + ", moving " + rocket.getDeltaMovement().x);
			}
		});
		helper.runAfterDelay(3 + step, () -> {
			helper.assertTrue(rows.rockets() == 3, "A second later the middle row");
			helper.assertTrue(!helper.getLevel().getEntitiesOfClass(FireworkRocketEntity.class, new AABB(helper.absolutePos(volley)).inflate(2.0, 16.0, 2.0))
					.isEmpty(), "with the vanilla rocket in it");
		});
		helper.runAfterDelay(3 + 2 * step, () -> {
			helper.assertTrue(rows.rockets() == 0 && !rows.running(), "then the back row, and it is over");
			helper.succeed();
		});
	}

	/** A rising redstone signal starts a show and the next stops it; so does an empty hand; sneaking changes the mode. */
	@GameTest(maxTicks = 20)
	public void redstoneAndHandsRunTheShow(GameTestHelper helper) {
		floor(helper);
		BlockPos pos = new BlockPos(3, 2, 3);
		ShowLauncherBlockEntity launcher = launcher(helper, pos, ShowLauncherBlock.Mode.SEQUENCE);
		launcher.setItem(0, firework(FireworkShape.SKULL, 16, 1));
		helper.setBlock(pos.east(), Blocks.REDSTONE_BLOCK);
		helper.assertTrue(launcher.running() && helper.getBlockState(pos).getValue(ShowLauncherBlock.POWERED), "A signal starts the show");
		helper.setBlock(pos.east(), Blocks.AIR);
		helper.assertTrue(launcher.running() && !helper.getBlockState(pos).getValue(ShowLauncherBlock.POWERED), "and it runs on when the signal ends");
		helper.setBlock(pos.east(), Blocks.REDSTONE_BLOCK);
		helper.assertTrue(!launcher.running(), "The next signal stops it");
		helper.setBlock(pos.east(), Blocks.AIR);
		ServerPlayer player = player(helper, new BlockPos(1, 2, 3), ItemStack.EMPTY);
		use(helper, player, pos);
		helper.assertTrue(launcher.running(), "An empty hand starts it");
		use(helper, player, pos);
		helper.assertTrue(!launcher.running(), "and stops it");
		player.setShiftKeyDown(true);
		use(helper, player, pos);
		helper.assertTrue(helper.getBlockState(pos).getValue(ShowLauncherBlock.MODE) == ShowLauncherBlock.Mode.VOLLEY && !launcher.running(),
				"Sneaking, an empty hand turns the dial to volleys");
		player.setShiftKeyDown(false);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
		use(helper, player, pos);
		helper.assertTrue(!launcher.running(), "A stick does nothing");
		launcher.clearContent();
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		use(helper, player, pos);
		helper.assertTrue(!launcher.running(), "An empty launcher won't start");
		helper.succeed();
	}

	/** A dispenser facing up sets off a spooky firework. */
	@GameTest(maxTicks = 40)
	public void dispensersSetThemOff(GameTestHelper helper) {
		floor(helper);
		BlockPos pos = new BlockPos(4, 2, 4);
		openSky(helper, 4, 4);
		helper.setBlock(pos, Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, Direction.UP));
		DispenserBlockEntity dispenser = helper.getBlockEntity(pos, DispenserBlockEntity.class);
		dispenser.setItem(0, firework(FireworkShape.SKULL, 1, 2));
		helper.setBlock(pos.east(), Blocks.REDSTONE_BLOCK);
		helper.succeedWhen(() -> {
			List<SpookyRocket> launched = rockets(helper, pos, 1.0);
			helper.assertTrue(dispenser.isEmpty() && launched.size() == 1 && launched.get(0).shape() == FireworkShape.SKULL
					&& launched.get(0).getY() > helper.absolutePos(pos).getY() + 0.5, "Waiting for the dispenser to set it off");
		});
	}
}
