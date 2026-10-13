package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.HeadstoneBlock;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import io.github.jimbozoomer.jugcraft.lair.GraveGoods;
import io.github.jimbozoomer.jugcraft.lair.JugcraftLairs;
import io.github.jimbozoomer.jugcraft.lair.Lair;
import io.github.jimbozoomer.jugcraft.lair.LairInstance;
import io.github.jimbozoomer.jugcraft.lair.LairRules;
import io.github.jimbozoomer.jugcraft.lair.Lairs;
import io.github.jimbozoomer.jugcraft.lair.LastRites;
import io.github.jimbozoomer.jugcraft.lair.MistGateEntity;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Client game test for the lairs and the Hollow Acre (docs/features/hollow-acre.md), in a real world: unlike a
 * game-test server's, it has the lair dimensions. First the framework from end to end, with the one player in survival
 * ({@link #endToEnd}); then pictures: the Last Rites' grave at midnight with its candles, wreath and the mist gate open
 * over it, and, through the gate, the Hollow Acre from the arrival point, above the Mown Circle, before the bone
 * chapel's throne, at the lych gate's Grey Mist, and from off the island. CI job {@code client}.
 */
public class LairClientGameTests implements FabricClientGameTest {
	private static final Lair ACRE = Lair.HOLLOW_ACRE;

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext singleplayer = context.worldBuilder()
				.adjustSettings(creator -> creator.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE)).create()) {
			singleplayer.getConnection().waitForChunksRender();
			context.runOnClient(client -> {
				if (!client.gui.hud.isHidden()) {
					client.gui.hud.toggle();
				}
			});
			BlockPos origin = context.computeOnClient(client -> BlockPos.containing(client.player.position()));
			int x = origin.getX();
			int y = origin.getY();
			int z = origin.getZ();
			TestServerContext server = singleplayer.getServer();
			server.runCommand("gamerule minecraft:send_command_feedback false");
			server.runCommand("difficulty peaceful");
			server.runCommand("time set midnight");
			server.runCommand("weather clear");
			server.runCommand("fill %d %d %d %d %d %d minecraft:grass_block".formatted(x - 8, y - 3, z - 10, x + 12, y - 1, z + 6));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 8, y, z - 10, x + 12, y + 8, z + 6));
			context.waitTicks(10);

			BlockPos grave = new BlockPos(x + 2, y, z - 5);
			server.runOnServer(minecraft -> prepare(minecraft.overworld(), grave));
			endToEnd(context, server, grave, new Vec3(x - 4.5, y, z + 2.5));

			// The Last Rites' grave, and the gate they open over it.
			server.runCommand("gamemode creative @a");
			server.runOnServer(minecraft -> {
				prepare(minecraft.overworld(), grave);
				LairInstance instance = Lairs.open(minecraft, ACRE);
				check(instance != null, "No Hollow Acre opened");
				MistGateEntity.open(minecraft.overworld(), grave, instance, 6000);
			});
			context.waitTicks(20);
			shoot(context, singleplayer, "minecraft:overworld", x + 2.5, y + 1, z + 1.5, 180, 15, "jugcraft_last_rites");

			// Through the gate.
			server.runOnServer(minecraft -> Lairs.enter(player(minecraft), Lairs.open(ACRE).getFirst()));
			context.waitTicks(40);
			singleplayer.getConnection().waitForChunksRender();
			BlockPos o = ACRE.origin(0);
			String acre = ACRE.dimension.identifier().toString();
			shoot(context, singleplayer, acre, o.getX() + 32.0, o.getY() + 18, o.getZ() + 67.5, 180, 8, "jugcraft_hollow_acre_arrival");
			shoot(context, singleplayer, acre, o.getX() + 32.0, o.getY() + 42, o.getZ() + 63.5, 180, 40, "jugcraft_hollow_acre_arena");
			shoot(context, singleplayer, acre, o.getX() + 31.5, o.getY() + 19, o.getZ() + 27.5, 180, 6, "jugcraft_hollow_acre_chapel");
			shoot(context, singleplayer, acre, o.getX() + 32.0, o.getY() + 18, o.getZ() + 63.5, 0, 4, "jugcraft_hollow_acre_gate");
			shoot(context, singleplayer, acre, o.getX() + 82.0, o.getY() + 12, o.getZ() + 75.0, 126, 2, "jugcraft_hollow_acre_island");
			server.runOnServer(minecraft -> Lairs.reset());
		}
	}

	/**
	 * A lair from end to end, step by step, with the player in survival (the instances are the whole server's, so
	 * this one test owns them):
	 * <ol>
	 * <li>the Death Knell rung at the prepared grave opens an instance in the Hollow Acre's own dimension, places the
	 * island fresh (its throne, exit, braziers and moon where the template puts them), takes the wreath, opens a gate
	 * over the grave and takes the ringer in, unable to build;</li>
	 * <li>building and using blocks are refused, the lair's exit is not;</li>
	 * <li>straying past the edge throws the player back for 4 damage, never below half a heart;</li>
	 * <li>dying there gathers what was dropped as Grave Goods, handed back on respawning;</li>
	 * <li>the gate takes the player back in, and leaving takes them back to exactly where they stood, facing the same
	 * way, able to build again;</li>
	 * <li>a full party lets nobody else in;</li>
	 * <li>an instance left empty closes after 30 seconds, and its gate with it; a restart closes them all and sends
	 * anyone inside home;</li>
	 * <li>no more instances open than lairs.instances allows.</li>
	 * </ol>
	 */
	private static void endToEnd(ClientGameTestContext context, TestServerContext server, BlockPos grave, Vec3 outside) {
		server.runCommand("gamemode survival @a");
		context.waitTicks(5);
		AtomicReference<LairInstance> first = new AtomicReference<>();
		AtomicReference<Vec3> stood = new AtomicReference<>();

		// 1. The rites, rung: an instance opens and the ringer goes in.
		server.runOnServer(minecraft -> {
			ServerPlayer player = player(minecraft);
			ServerLevel overworld = minecraft.overworld();
			player.teleportTo(overworld, grave.getX() + 0.5, grave.getY(), grave.getZ() + 2.5, Set.of(), 37.5F, 12.0F, true);
			stood.set(player.position());
			LastRites.Ring ring = LastRites.ring(overworld, player, true);
			LairInstance instance = ring.instance();
			check(ring.missing() == null && instance != null, "The rites opened no instance: " + ring);
			first.set(instance);
			check(!overworld.getBlockState(grave.south()).is(JugcraftLairs.MOURNING_WREATH), "The wreath was not taken");
			ServerLevel lair = minecraft.getLevel(ACRE.dimension);
			check(lair != null && lair.getMinY() == 0 && !lair.dimensionType().hasCeiling(), "The Hollow Acre's dimension: from y 0, no ceiling");
			BlockPos origin = instance.origin();
			check(lair.getBlockState(origin.offset(31, 17, 13)).is(block("bone_throne")), "No Bone Throne in the chapel");
			check(lair.getBlockState(origin.offset(31, 17, 69)).is(JugcraftLairs.LAIR_EXIT)
					&& lair.getBlockState(origin.offset(32, 18, 69)).is(JugcraftLairs.LAIR_EXIT), "No Grey Mist in the lych gate");
			check(lair.getBlockState(origin.offset(17, 17, 21)).is(JugcraftLairs.LAIR_BRAZIER), "No ward brazier");
			check(lair.getBlockState(origin.offset(ACRE.moon)).is(JugcraftLairs.LAIR_MOON), "No moon");
			check(player.level() == lair && player.position().distanceTo(instance.arrival()) < 0.5,
					"The ringer did not arrive: " + player.level().dimension() + " " + player.position());
			check(!player.getAbilities().mayBuild, "In a lair a player cannot build");
			List<MistGateEntity> gates = overworld.getEntitiesOfClass(MistGateEntity.class, new AABB(grave).inflate(3));
			check(gates.size() == 1 && gates.get(0).instance() == instance, "No gate over the grave");
		});
		context.waitTicks(40);

		// 2. Nothing can be built or used, but the lair's own exit; 3. the edges.
		server.runOnServer(minecraft -> {
			ServerPlayer player = player(minecraft);
			ServerLevel lair = minecraft.getLevel(ACRE.dimension);
			LairInstance instance = first.get();
			BlockPos origin = instance.origin();
			check(LairRules.denies(player, lair), "The lair lets the player change it");
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STONE));
			BlockPos soil = origin.offset(30, 16, 60);
			BlockHitResult onSoil = new BlockHitResult(Vec3.atCenterOf(soil).relative(Direction.UP, 0.5), Direction.UP, soil, false);
			check(LairRules.useBlock(player, lair, InteractionHand.MAIN_HAND, onSoil) == InteractionResult.FAIL, "A block could be placed in the lair");
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
			check(LairRules.useItem(player, lair, InteractionHand.MAIN_HAND) == InteractionResult.FAIL, "A bucket could be emptied");
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.ENDER_PEARL));
			check(LairRules.useItem(player, lair, InteractionHand.MAIN_HAND) == InteractionResult.PASS, "Ender pearls work");
			player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			BlockPos exit = origin.offset(31, 17, 69);
			BlockHitResult onExit = new BlockHitResult(Vec3.atCenterOf(exit), Direction.NORTH, exit, false);
			check(LairRules.useBlock(player, lair, InteractionHand.MAIN_HAND, onExit) == InteractionResult.PASS, "The exit cannot be used");

			Vec3 arrival = instance.arrival();
			player.teleportTo(arrival.x + ACRE.bounds + 5, arrival.y, arrival.z);
			player.setHealth(20.0F);
			check(Lairs.edges(player, instance), "Flying off the island was not noticed");
			check(player.position().distanceTo(arrival) < 0.5 && player.getHealth() == 20.0F - Lairs.EDGE_DAMAGE,
					"The mist did not throw the player back for " + Lairs.EDGE_DAMAGE + ": " + player.getHealth());
			player.teleportTo(arrival.x, origin.getY() + ACRE.floor - 2.0, arrival.z);
			player.setHealth(2.0F);
			check(Lairs.edges(player, instance) && player.getHealth() == 1.0F, "Falling off took more than to half a heart");
			check(!Lairs.edges(player, instance), "Standing at the arrival point is not off the edge");
			player.setHealth(20.0F);
		});

		// 4. Grave Goods: what was dropped dying is gathered at once and handed back on respawning (outside).
		server.runOnServer(minecraft -> {
			ServerPlayer player = player(minecraft);
			ServerLevel lair = minecraft.getLevel(ACRE.dimension);
			ItemEntity dropped = new ItemEntity(lair, player.getX(), player.getY() + 1.0, player.getZ(), new ItemStack(Items.DIAMOND, 3));
			lair.addFreshEntity(dropped);
			ExperienceOrb orb = new ExperienceOrb(lair, player.getX(), player.getY(), player.getZ(), 7);
			lair.addFreshEntity(orb);
			Lairs.died(player);
			GraveGoods goods = player.getAttached(Lairs.GRAVE_GOODS);
			check(goods != null && goods.count() == 3 && goods.experience() == 7 && dropped.isRemoved() && orb.isRemoved(),
					"The drops were not gathered as Grave Goods: " + goods);
			check(player.getAttached(Lairs.VISIT) == null, "A death in a lair ends the visit");
			player.teleportTo(minecraft.overworld(), outside.x, outside.y, outside.z, Set.of(), 0.0F, 0.0F, true);
			int before = player.totalExperience;
			int diamonds = player.getInventory().countItem(Items.DIAMOND);
			check(Lairs.handBack(player), "No Grave Goods were handed back");
			check(player.getInventory().countItem(Items.DIAMOND) == diamonds + 3 && player.totalExperience == before + 7
					&& player.getAttached(Lairs.GRAVE_GOODS) == null, "The Grave Goods did not all come back");
		});
		context.waitTicks(40);

		// 5. Back in through the gate, then out: back to exactly where the player stood, facing the same way.
		AtomicReference<float[]> facing = new AtomicReference<>();
		server.runOnServer(minecraft -> {
			ServerPlayer player = player(minecraft);
			ServerLevel overworld = minecraft.overworld();
			List<MistGateEntity> gates = overworld.getEntitiesOfClass(MistGateEntity.class, new AABB(grave).inflate(3));
			check(gates.size() == 1, "The gate closed early");
			stood.set(player.position());
			facing.set(new float[] {player.getYRot(), player.getXRot()});
			gates.get(0).use(player, InteractionHand.MAIN_HAND);
			check(player.level() == minecraft.getLevel(ACRE.dimension) && Lairs.instance(player.getAttached(Lairs.VISIT)) == first.get(),
					"The gate did not take the player back in");
		});
		context.waitTicks(40);
		server.runOnServer(minecraft -> {
			ServerPlayer player = player(minecraft);
			check(Lairs.leave(player), "The player could not leave");
			check(player.level() == minecraft.overworld() && player.position().distanceTo(stood.get()) < 1.0E-6
					&& player.getYRot() == facing.get()[0] && player.getXRot() == facing.get()[1],
					"The player came back elsewhere: " + player.position() + " " + player.getYRot() + " " + player.getXRot());
			check(player.getAbilities().mayBuild && player.getAttached(Lairs.VISIT) == null, "Out of the lair a player builds again");
		});
		context.waitTicks(20);

		// 6. A full party lets nobody else in.
		server.runOnServer(minecraft -> {
			ServerPlayer player = player(minecraft);
			String partySize = JugcraftConfig.textOption("lairs.party_size");
			try {
				JugcraftConfig.setTextOption("lairs.party_size", "1");
				LairInstance second = Lairs.open(minecraft, ACRE);
				check(second != null, "No second instance opened");
				second.admit(UUID.randomUUID());
				check(!Lairs.enter(player, second) && player.level() == minecraft.overworld() && player.getAttached(Lairs.VISIT) == null,
						"A player came into a full party");
				Lairs.close(minecraft, second);
			} finally {
				JugcraftConfig.setTextOption("lairs.party_size", partySize);
			}
		});

		// 7. Empty for 30 seconds, the instance closes, and its gate; a restart closes them all and sends anyone home.
		server.runOnServer(minecraft -> {
			ServerLevel lair = minecraft.getLevel(ACRE.dimension);
			LairInstance instance = first.get();
			long now = lair.getGameTime();
			Lairs.check(minecraft, lair, ACRE, now);
			check(instance.isOpen(), "An instance closed the moment it was empty");
			Lairs.check(minecraft, lair, ACRE, now + Lairs.EMPTY_SECONDS * 20L);
			check(!instance.isOpen(), "An instance empty for " + Lairs.EMPTY_SECONDS + " seconds stayed open");
			check(minecraft.overworld().getEntitiesOfClass(MistGateEntity.class, new AABB(grave).inflate(3)).isEmpty(),
					"The gate outlived its instance");
		});
		AtomicReference<Vec3> home = new AtomicReference<>();
		server.runOnServer(minecraft -> {
			ServerPlayer player = player(minecraft);
			LairInstance third = Lairs.open(minecraft, ACRE);
			check(third != null, "No instance opened after one closed");
			home.set(player.position());
			check(Lairs.enter(player, third), "The player could not go in");
		});
		context.waitTicks(40);
		server.runOnServer(minecraft -> {
			ServerPlayer player = player(minecraft);
			ServerLevel lair = minecraft.getLevel(ACRE.dimension);
			Lairs.reset();
			Lairs.check(minecraft, lair, ACRE, lair.getGameTime());
			check(player.level() == minecraft.overworld() && player.position().distanceTo(home.get()) < 1.0E-6,
					"A player in a closed instance was not sent home: " + player.level().dimension() + " " + player.position());
		});
		context.waitTicks(40);

		// 8. The cap: no more instances than lairs.instances.
		server.runOnServer(minecraft -> {
			String instances = JugcraftConfig.textOption("lairs.instances");
			try {
				JugcraftConfig.setTextOption("lairs.instances", "2");
				LairInstance a = Lairs.open(minecraft, ACRE);
				LairInstance b = Lairs.open(minecraft, ACRE);
				check(a != null && b != null && Lairs.open(minecraft, ACRE) == null, "More instances opened than the cap");
				Lairs.close(minecraft, a);
				check(Lairs.open(minecraft, ACRE) != null, "A closed instance's slot was not free again");
			} finally {
				JugcraftConfig.setTextOption("lairs.instances", instances);
				Lairs.reset();
			}
		});
		Jugcraft.LOGGER.info("[lairs] client game test: the framework passed end to end");
	}

	/** A Gothic headstone facing south, a Mourning Wreath before it, and four blocks of three lit candles round it. */
	private static void prepare(ServerLevel level, BlockPos grave) {
		level.setBlock(grave, block("gothic_headstone").defaultBlockState().setValue(HeadstoneBlock.FACING, Direction.SOUTH), Block.UPDATE_ALL);
		level.setBlock(grave.south(), JugcraftLairs.MOURNING_WREATH.defaultBlockState(), Block.UPDATE_ALL);
		for (BlockPos candle : new BlockPos[] {grave.west(2), grave.east(2), grave.west(2).south(2), grave.east(2).south(2)}) {
			level.setBlock(candle, Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.LIT, true).setValue(CandleBlock.CANDLES, 3),
					Block.UPDATE_ALL);
		}
	}

	private static ServerPlayer player(MinecraftServer minecraft) {
		return minecraft.getPlayerList().getPlayers().get(0);
	}

	private static Block block(String id) {
		return BuiltInRegistries.BLOCK.getValue(Jugcraft.id(id));
	}

	private static void check(boolean condition, String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}

	/** Stands the camera at (x, y, z) in {@code dimension}, looking along yaw and pitch, waits for the world to draw, and shoots. */
	private static void shoot(ClientGameTestContext context, TestSingleplayerContext singleplayer, String dimension, double x, double y,
			double z, int yaw, int pitch, String name) {
		TestServerContext server = singleplayer.getServer();
		server.runOnServer(minecraft -> {
			// Flying, so the camera stays where it is put (set on the server, which tells the client).
			ServerPlayer player = player(minecraft);
			player.getAbilities().flying = true;
			player.onUpdateAbilities();
		});
		server.runCommand(String.format(Locale.ROOT, "execute in %s run tp @a %.1f %.1f %.1f %d %d", dimension, x, y, z, yaw, pitch));
		context.waitTicks(30);
		singleplayer.getConnection().waitForChunksRender();
		context.waitTicks(10);
		context.takeScreenshot(name);
	}
}
