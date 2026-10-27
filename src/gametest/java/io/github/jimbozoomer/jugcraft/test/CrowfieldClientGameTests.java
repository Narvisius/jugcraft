package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.HalloweenSeason;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.ScarecrowBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.TallDecorationBlock;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import io.github.jimbozoomer.jugcraft.lair.Crowfield;
import io.github.jimbozoomer.jugcraft.lair.Crowfield.Hill;
import io.github.jimbozoomer.jugcraft.lair.HarvestCrownRite;
import io.github.jimbozoomer.jugcraft.lair.HowlingTotemRite;
import io.github.jimbozoomer.jugcraft.lair.JugcraftLairs;
import io.github.jimbozoomer.jugcraft.lair.KingsGourdBlock;
import io.github.jimbozoomer.jugcraft.lair.Lair;
import io.github.jimbozoomer.jugcraft.lair.LairInstance;
import io.github.jimbozoomer.jugcraft.lair.LairRules;
import io.github.jimbozoomer.jugcraft.lair.Lairs;
import io.github.jimbozoomer.jugcraft.lair.MistGateEntity;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Client game test for the Crowfield and the Harvest Crown (docs/features/crowfield.md), in a real world, which has the
 * lair dimensions: first the crown from end to end with the one player in survival ({@link #endToEnd}); then pictures:
 * the Grey Mist rising by the crowned scarecrow, and, in the field, the farmyard from the arrival, the yard's hay cart and
 * lamp post, the field through the farm gate with the moon over the north-west corn, the King's Post on its mound, a
 * hill with its green gourd, a gourd ripening and one lit, the corn wall close to, the whole field from over the yard and
 * the Grey Mist at the end of the lane; last, the game's own block light on every open cell of the field. CI job
 * {@code client}.
 */
public class CrowfieldClientGameTests implements FabricClientGameTest {
	private static final Lair FIELD = Lair.CROWFIELD;
	/** Where tools/crowfield.py puts things (template positions), beside what Crowfield.java gives. */
	private static final BlockPos UNDER_ARRIVAL = new BlockPos(34, 4, 61);
	private static final BlockPos LANTERN = new BlockPos(40, 7, 57);
	private static final BlockPos POST = new BlockPos(34, 6, 32);
	private static final BlockPos CORN = new BlockPos(34, 5, 5);
	/**
	 * The least block light on any open cell of the field, and how many open cells it has (tools/crowfield.py FIELD_LIGHT
	 * and its open cells, checked by the audit).
	 */
	private static final int FIELD_LIGHT = 7;
	private static final int FIELD_CELLS = 1890;

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
			// A level patch of grass open to the sky, and a stone roof over one corner of it.
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 10, y, z - 10, x + 12, y + 40, z + 8));
			server.runCommand("fill %d %d %d %d %d %d minecraft:dirt".formatted(x - 10, y - 3, z - 10, x + 12, y - 2, z + 8));
			server.runCommand("fill %d %d %d %d %d %d minecraft:grass_block".formatted(x - 10, y - 1, z - 10, x + 12, y - 1, z + 8));
			server.runCommand("fill %d %d %d %d %d %d minecraft:stone".formatted(x - 9, y + 3, z - 9, x - 5, y + 3, z - 5));
			context.waitTicks(10);

			BlockPos scarecrow = new BlockPos(x + 2, y, z - 3);
			BlockPos roofed = new BlockPos(x - 7, y, z - 7);
			endToEnd(context, server, scarecrow, roofed);

			// The pictures, in creative, the mist held open long enough to take them all.
			server.runCommand("gamemode creative @a");
			String gateSeconds = JugcraftConfig.textOption("lairs.gate_seconds");
			try {
				JugcraftConfig.setTextOption("lairs.gate_seconds", "600");
				AtomicReference<LairInstance> instance = new AtomicReference<>();
				BlockPos stand = scarecrow.south(2);
				server.runOnServer(minecraft -> {
					ServerPlayer player = player(minecraft);
					ServerLevel overworld = minecraft.overworld();
					player.teleportTo(overworld, stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, Set.of(), 180.0F, 0.0F, true);
					player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JugcraftLairs.HARVEST_CROWN));
					HarvestCrownRite.Crowning crowning = HarvestCrownRite.crown(overworld, player, scarecrow, player.getMainHandItem());
					check(crowning.missing() == null, "The crown opened no field for the pictures: " + crowning);
					instance.set(crowning.instance());
				});
				context.waitTicks(40);
				// The mist by the scarecrow (the camera leaves the field by command, which ends the visit; the mist stays).
				shootAt(context, singleplayer, "minecraft:overworld", stand.getX() + 3.5, y, stand.getZ() + 4.5, stand.getX() + 0.5, y + 1.2,
						stand.getZ() - 0.5, "jugcraft_harvest_crown_mist");
				server.runOnServer(minecraft -> check(Lairs.enter(player(minecraft), instance.get()), "The camera could not go back in"));
				context.waitTicks(40);
				singleplayer.getConnection().waitForChunksRender();
				BlockPos o = FIELD.origin(0);
				String dim = FIELD.dimension.identifier().toString();
				int floor = Crowfield.STAND;
				shoot(context, singleplayer, dim, o.getX() + 34.5, o.getY() + floor, o.getZ() + 63.5, 180, -4, "jugcraft_crowfield_arrival");
				shootAt(context, singleplayer, dim, o.getX() + 37.5, o.getY() + floor, o.getZ() + 62.5, o.getX() + 29.5, o.getY() + floor + 0.8,
						o.getZ() + 58.5, "jugcraft_crowfield_farmyard");
				shoot(context, singleplayer, dim, o.getX() + 34.5, o.getY() + floor, o.getZ() + 56.5, 170, -8, "jugcraft_crowfield_field");
				shootAt(context, singleplayer, dim, o.getX() + 34.5, o.getY() + floor, o.getZ() + 42.5, o.getX() + POST.getX() + 0.5,
						o.getY() + floor + 3.0, o.getZ() + POST.getZ() + 0.5, "jugcraft_crowfield_post");
				BlockPos hill = Hill.WEST.at(o);
				shootAt(context, singleplayer, dim, hill.getX() + 4.5, o.getY() + floor, hill.getZ() + 3.5, hill.getX() + 0.5, hill.getY() + 0.3,
						hill.getZ() + 0.5, "jugcraft_crowfield_hill");
				// A gourd ripening on one hill and one lit on the next, held so for the pictures (nothing changes them by itself).
				BlockPos south = Hill.SOUTH.at(o);
				BlockPos southWest = Hill.SOUTH_WEST.at(o);
				server.runOnServer(minecraft -> {
					ServerLevel lair = minecraft.getLevel(FIELD.dimension);
					check(KingsGourdBlock.setStage(lair, south, KingsGourdBlock.RIPENING) && KingsGourdBlock.setStage(lair, southWest, KingsGourdBlock.LIT),
							"No gourds on the south hills to ripen for the pictures");
				});
				shootAt(context, singleplayer, dim, south.getX() - 6.0, o.getY() + floor, south.getZ() + 8.5, south.getX() - 6.0, south.getY() + 0.3,
						south.getZ() + 0.5, "jugcraft_crowfield_gourds_ripening");
				server.runOnServer(minecraft -> {
					ServerLevel lair = minecraft.getLevel(FIELD.dimension);
					KingsGourdBlock.setStage(lair, south, KingsGourdBlock.GREEN);
					KingsGourdBlock.setStage(lair, southWest, KingsGourdBlock.GREEN);
				});
				shootAt(context, singleplayer, dim, o.getX() + CORN.getX() + 0.5, o.getY() + floor, o.getZ() + CORN.getZ() + 8.5,
						o.getX() + CORN.getX() + 0.5, o.getY() + floor + 1.5, o.getZ() + CORN.getZ() + 0.5, "jugcraft_crowfield_corn");
				// From over the farmyard (within the field's bounds), the whole field.
				shoot(context, singleplayer, dim, o.getX() + 34.5, o.getY() + floor + 30, o.getZ() + 66.5, 180, 40, "jugcraft_crowfield_above");
				shoot(context, singleplayer, dim, o.getX() + 34.5, o.getY() + floor, o.getZ() + 63.5, 0, 0, "jugcraft_crowfield_exit");

				// The game's own light on every open cell of the field, where the King fights.
				server.runOnServer(minecraft -> {
					ServerLevel lair = minecraft.getLevel(FIELD.dimension);
					int least = 15;
					int cells = 0;
					BlockPos darkest = null;
					for (int dx = 0; dx < FIELD.width; dx++) {
						for (int dz = 0; dz < FIELD.length; dz++) {
							if (!Crowfield.inField(dx + 0.5, dz + 0.5)) {
								continue;
							}
							BlockPos cell = o.offset(dx, floor, dz);
							if (!standable(lair, cell) || !standable(lair, cell.above())) {
								continue;
							}
							int light = lair.getBrightness(LightLayer.BLOCK, cell);
							cells++;
							if (light < least) {
								least = light;
								darkest = cell.subtract(o);
							}
						}
					}
					Jugcraft.LOGGER.info("[lairs] the Crowfield: {} open cells, the darkest at block light {} ({})", cells, least, darkest);
					check(cells == FIELD_CELLS && least >= FIELD_LIGHT, "The field has " + cells + " open cells (not " + FIELD_CELLS
							+ "), the darkest at block light " + least + " (" + darkest + "), under " + FIELD_LIGHT);
				});
			} finally {
				JugcraftConfig.setTextOption("lairs.gate_seconds", gateSeconds);
				server.runOnServer(minecraft -> Lairs.reset());
			}
		}
	}

	/**
	 * The Harvest Crown from end to end, step by step, with the player in survival (the instances are the whole server's,
	 * so this one test owns them):
	 * <ol>
	 * <li>used on the bare scarecrow the crown is refused and kept; given a head, among too little harvest the same; with
	 * the harvest in, by day the same, and off-season on a server that keeps the rituals to the Halloween event; at night
	 * on a scarecrow under a roof, the same;</li>
	 * <li>used by the player's hand on the headed scarecrow at night among the harvest under the open sky, it opens an
	 * instance in the field's own dimension, places the field fresh (the yard's floor under the arrival, the Grey Mist at
	 * the end of the lane, each hill's gourd green, the King's Post, the lamp post's lantern), is used up and rested,
	 * leaves the scarecrow's head and the pumpkins as they were, raises Grey Mist where the player stood, and brings them to
	 * in the farmyard, unable to build;</li>
	 * <li>in the field a gourd is a fixture and the corn is not; a ripening gourd picked by hand leaves only its vine; a
	 * green one is not picked; falling off the field throws them back to the farmyard for the mist's toll;</li>
	 * <li>leaving takes the player back to where they stood; while the mist is risen a second crown used there is refused
	 * and kept, and using the mist takes them back into the same field;</li>
	 * <li>when the instance closes the player goes home and the mist is gone.</li>
	 * </ol>
	 */
	private static void endToEnd(ClientGameTestContext context, TestServerContext server, BlockPos scarecrow, BlockPos roofed) {
		server.runCommand("gamemode survival @a");
		context.waitTicks(5);
		AtomicReference<LairInstance> first = new AtomicReference<>();
		AtomicReference<Vec3> stood = new AtomicReference<>();
		BlockPos stand = scarecrow.south(2);
		int[][] spots = {{-2, -2}, {0, -3}, {2, -2}, {3, 0}, {2, 2}, {-2, 2}, {-3, 0}, {-3, -1}};

		// 1. Refused bare-headed, then among too little harvest, then by day, off-season and under a roof. The roofed scarecrow
		// has no harvest of its own: the roof is checked first, and pumpkins round it would count toward the other's.
		server.runOnServer(minecraft -> {
			ServerPlayer player = player(minecraft);
			ServerLevel overworld = minecraft.overworld();
			placeScarecrow(overworld, scarecrow);
			placeScarecrow(overworld, roofed);
			for (int i = 0; i < spots.length - 1; i++) {
				overworld.setBlock(scarecrow.offset(spots[i][0], 0, spots[i][1]), i % 2 == 0 ? Blocks.PUMPKIN.defaultBlockState()
						: JugcraftAgriculture.block("white_pumpkin").defaultBlockState(), 3);
			}
			player.teleportTo(overworld, stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, Set.of(), 180.0F, 0.0F, true);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JugcraftLairs.HARVEST_CROWN, 2));
			HarvestCrownRite.Crowning crowning = HarvestCrownRite.crown(overworld, player, scarecrow, player.getMainHandItem());
			check(crowning.missing() == HarvestCrownRite.Missing.BARE && player.getMainHandItem().getCount() == 2,
					"On a bare-headed scarecrow, the crown was not refused, or was used up: " + crowning + ", " + player.getMainHandItem());
			head(overworld, scarecrow).setHead(new ItemStack(Items.CARVED_PUMPKIN));
			head(overworld, roofed).setHead(new ItemStack(Items.CARVED_PUMPKIN));
			crowning = HarvestCrownRite.crown(overworld, player, scarecrow, player.getMainHandItem());
			check(crowning.missing() == HarvestCrownRite.Missing.NO_HARVEST && player.getMainHandItem().getCount() == 2,
					"Among too little harvest, the crown was not refused, or was used up: " + crowning + ", " + player.getMainHandItem());
			int[] last = spots[spots.length - 1];
			overworld.setBlock(scarecrow.offset(last[0], 0, last[1]), JugcraftAgriculture.block("warty_gourd").defaultBlockState(), 3);
		});
		server.runCommand("time set noon");
		context.waitTicks(10);
		String offSeason = JugcraftConfig.textOption("lairs.off_season");
		server.runOnServer(minecraft -> {
			ServerPlayer player = player(minecraft);
			ServerLevel overworld = minecraft.overworld();
			HarvestCrownRite.Crowning crowning = HarvestCrownRite.crown(overworld, player, scarecrow, player.getMainHandItem());
			check(crowning.missing() == HarvestCrownRite.Missing.NOT_NIGHT && player.getMainHandItem().getCount() == 2,
					"By day, the crown was not refused, or was used up: " + crowning + ", " + player.getMainHandItem());
		});
		server.runCommand("time set midnight");
		context.waitTicks(10);
		server.runOnServer(minecraft -> {
			ServerPlayer player = player(minecraft);
			ServerLevel overworld = minecraft.overworld();
			HalloweenSeason.Mode mode = HalloweenSeason.mode();
			try {
				JugcraftConfig.setTextOption("lairs.off_season", "off");
				HalloweenSeason.setMode(HalloweenSeason.Mode.OFF);
				HarvestCrownRite.Crowning crowning = HarvestCrownRite.crown(overworld, player, scarecrow, player.getMainHandItem());
				check(crowning.missing() == HarvestCrownRite.Missing.OFF_SEASON && player.getMainHandItem().getCount() == 2,
						"Off-season, the crown was not refused, or was used up: " + crowning + ", " + player.getMainHandItem());
				HalloweenSeason.setMode(HalloweenSeason.Mode.ON);
				check(HarvestCrownRite.check(overworld, scarecrow, HowlingTotemRite.feet(player), true,
						Lairs.offSeason() || HalloweenSeason.active()) == null, "During the Halloween event the crown was refused off-season");
			} finally {
				JugcraftConfig.setTextOption("lairs.off_season", offSeason);
				HalloweenSeason.setMode(mode);
			}
			player.teleportTo(overworld, roofed.getX() + 0.5, roofed.getY(), roofed.getZ() + 2.5, Set.of(), 180.0F, 0.0F, true);
			HarvestCrownRite.Crowning crowning = HarvestCrownRite.crown(overworld, player, roofed, player.getMainHandItem());
			check(crowning.missing() == HarvestCrownRite.Missing.ROOFED && player.getMainHandItem().getCount() == 2,
					"Under a roof, the crown was not refused, or was used up: " + crowning + ", " + player.getMainHandItem());
			player.teleportTo(overworld, stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, Set.of(), 180.0F, 0.0F, true);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JugcraftLairs.HARVEST_CROWN));
		});
		context.waitTicks(10);

		// 2. At night, on the headed scarecrow among the harvest under the open sky, the crown opens the field.
		server.runOnServer(minecraft -> {
			ServerPlayer player = player(minecraft);
			ServerLevel overworld = minecraft.overworld();
			stood.set(player.position());
			HarvestCrownRite.Missing ready = HarvestCrownRite.check(overworld, scarecrow, HowlingTotemRite.feet(player), !overworld.isBrightOutside(),
					Lairs.offSeason() || HalloweenSeason.active());
			check(ready == null, "The crown could not open the field here: " + ready);
			InteractionResult used = player.gameMode.useItemOn(player, overworld, player.getMainHandItem(), InteractionHand.MAIN_HAND,
					new BlockHitResult(Vec3.atCenterOf(scarecrow.above()), Direction.SOUTH, scarecrow.above(), false));
			check(used.consumesAction(), "Using the crown on the scarecrow did not succeed: " + used);
		});
		server.runOnServer(minecraft -> {
			ServerPlayer player = player(minecraft);
			ServerLevel overworld = minecraft.overworld();
			LairInstance instance = Lairs.open(FIELD).isEmpty() ? null : Lairs.open(FIELD).getFirst();
			check(instance != null, "The crown opened no instance");
			first.set(instance);
			check(player.getMainHandItem().isEmpty(), "The crown was not used up: " + player.getMainHandItem());
			check(player.getCooldowns().isOnCooldown(new ItemStack(JugcraftLairs.HARVEST_CROWN)), "The crown was not rested after its use");
			check(head(overworld, scarecrow).head().is(Items.CARVED_PUMPKIN) && HarvestCrownRite.harvest(overworld, scarecrow) >= HarvestCrownRite.HARVEST_MIN,
					"The crowning took the scarecrow's head or the harvest");
			ServerLevel lair = minecraft.getLevel(FIELD.dimension);
			check(lair != null && lair.getMinY() == 0, "The Crowfield's dimension: from y 0");
			BlockPos o = instance.origin();
			// The arrival stands on the trodden path through the yard, a sixteenth short of a whole block: floor, not a full face.
			check(!lair.getBlockState(o.offset(UNDER_ARRIVAL)).getCollisionShape(lair, o.offset(UNDER_ARRIVAL)).isEmpty(),
					"No yard floor under the arrival");
			for (BlockPos mist : Crowfield.exitCells()) {
				check(lair.getBlockState(o.offset(mist)).is(JugcraftLairs.LAIR_EXIT), "No Grey Mist at the end of the lane at " + mist);
			}
			for (Hill hill : Hill.values()) {
				BlockState state = lair.getBlockState(hill.at(o));
				check(state.is(JugcraftLairs.KINGS_GOURD) && state.getValue(KingsGourdBlock.STAGE) == KingsGourdBlock.GREEN, "No green gourd on the "
						+ hill.title() + " hill");
			}
			check(lair.getBlockState(o.offset(POST)).is(Blocks.STRIPPED_DARK_OAK_LOG), "No King's Post at the centre");
			check(lair.getBlockState(o.offset(LANTERN)).is(Blocks.LANTERN), "No lantern on the yard's lamp post");
			check(player.level() == lair && player.position().distanceTo(instance.arrival()) < 0.5,
					"The player did not come to in the farmyard: " + player.level().dimension() + " " + player.position());
			check(!player.getAbilities().mayBuild, "In a lair a player cannot build");
			MistGateEntity mist = HarvestCrownRite.mist(overworld, stand);
			check(mist != null && mist.instance() == instance && mist.lair() == FIELD, "No mist risen where the crown was used");
		});
		context.waitTicks(40);

		// 3. A gourd is a fixture, the corn is not; a ripening gourd picked by hand; a green one not; the field's edge.
		server.runOnServer(minecraft -> {
			ServerPlayer player = player(minecraft);
			ServerLevel lair = minecraft.getLevel(FIELD.dimension);
			BlockPos o = first.get().origin();
			BlockPos gourd = Hill.WEST.at(o);
			BlockHitResult atGourd = new BlockHitResult(Vec3.atCenterOf(gourd), Direction.EAST, gourd, false);
			check(LairRules.useBlock(player, lair, InteractionHand.MAIN_HAND, atGourd) == InteractionResult.PASS, "A gourd cannot be used");
			BlockPos corn = o.offset(CORN);
			check(lair.getBlockState(corn).is(JugcraftLairs.CROWFIELD_CORN), "No corn north of the field at " + CORN);
			check(LairRules.useBlock(player, lair, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(corn), Direction.SOUTH, corn, false))
					== InteractionResult.FAIL, "Crowfield corn can be used");
			player.teleportTo(gourd.getX() + 2.5, o.getY() + Crowfield.STAND, gourd.getZ() + 0.5);
			InteractionResult green = player.gameMode.useItemOn(player, lair, ItemStack.EMPTY, InteractionHand.MAIN_HAND, atGourd);
			check(green.consumesAction() && lair.getBlockState(gourd).getValue(KingsGourdBlock.STAGE) == KingsGourdBlock.GREEN,
					"A green gourd was picked: " + green);
			KingsGourdBlock.setStage(lair, gourd, KingsGourdBlock.SWELLING);
			InteractionResult picked = player.gameMode.useItemOn(player, lair, ItemStack.EMPTY, InteractionHand.MAIN_HAND, atGourd);
			check(picked.consumesAction() && lair.getBlockState(gourd).getValue(KingsGourdBlock.STAGE) == KingsGourdBlock.PICKED,
					"A ripening gourd was not picked by hand: " + picked);
			Vec3 arrival = first.get().arrival();
			player.teleportTo(o.getX() + Crowfield.CENTRE_X, o.getY() + FIELD.floor - 2.0, o.getZ() + Crowfield.CENTRE_Z);
			player.setHealth(20.0F);
			check(Lairs.edges(player, first.get()) && player.position().distanceTo(arrival) < 0.5
					&& player.getHealth() == 20.0F - Lairs.EDGE_DAMAGE, "Falling out of the field did not end in the farmyard");
			player.setHealth(20.0F);
		});

		// 4. Out, a second crown refused at the risen mist, and back in through the mist.
		server.runOnServer(minecraft -> {
			ServerPlayer player = player(minecraft);
			check(Lairs.leave(player) && player.level() == minecraft.overworld() && player.position().distanceTo(stood.get()) < 1.0E-6,
					"The player did not come back to where they stood: " + player.position());
		});
		context.waitTicks(40);
		server.runOnServer(minecraft -> {
			ServerPlayer player = player(minecraft);
			ServerLevel overworld = minecraft.overworld();
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JugcraftLairs.HARVEST_CROWN));
			check(HarvestCrownRite.crown(overworld, player, scarecrow, player.getMainHandItem()).missing() == HarvestCrownRite.Missing.MISTING
					&& player.getMainHandItem().getCount() == 1, "A second crown at the risen mist was not refused, or was used up");
			MistGateEntity mist = HarvestCrownRite.mist(overworld, stand);
			check(mist != null, "The mist sank early");
			mist.use(player, InteractionHand.MAIN_HAND);
			check(player.level() == minecraft.getLevel(FIELD.dimension) && Lairs.instance(player.getAttached(Lairs.VISIT)) == first.get(),
					"The mist did not take the player back into the same field");
		});
		context.waitTicks(40);

		// 5. Closed, the player goes home and the mist is gone.
		server.runOnServer(minecraft -> {
			ServerPlayer player = player(minecraft);
			Lairs.close(minecraft, first.get());
			check(player.level() == minecraft.overworld(), "Closing the field did not send the player home");
		});
		context.waitTicks(50);
		server.runOnServer(minecraft -> {
			ServerLevel overworld = minecraft.overworld();
			check(HarvestCrownRite.mist(overworld, stand) == null
					&& overworld.getEntitiesOfClass(MistGateEntity.class, new AABB(stand).inflate(3)).isEmpty(), "The mist outlived its field");
			player(minecraft).setHealth(20.0F);
			Lairs.reset();
		});
		context.waitTicks(20);
		Jugcraft.LOGGER.info("[lairs] client game test: the Harvest Crown passed end to end");
	}

	/** A scarecrow with its lower half at {@code at}. */
	private static void placeScarecrow(ServerLevel level, BlockPos at) {
		Block block = JugcraftAgriculture.block("scarecrow");
		level.setBlock(at, block.defaultBlockState().setValue(TallDecorationBlock.HALF, DoubleBlockHalf.LOWER), 3);
		level.setBlock(at.above(), block.defaultBlockState().setValue(TallDecorationBlock.HALF, DoubleBlockHalf.UPPER), 3);
	}

	/** The scarecrow's upper half, which wears its head. */
	private static ScarecrowBlockEntity head(ServerLevel level, BlockPos lower) {
		if (!(level.getBlockEntity(lower.above()) instanceof ScarecrowBlockEntity scarecrow)) {
			throw new AssertionError("No scarecrow at " + lower);
		}
		return scarecrow;
	}

	/** Whether a player could stand in {@code cell}: nothing in it, nothing to collide with, or a carpet of moss on the floor. */
	private static boolean standable(ServerLevel level, BlockPos cell) {
		BlockState state = level.getBlockState(cell);
		return state.isAir() || state.is(Blocks.LIGHT) || state.is(Blocks.MOSS_CARPET) || state.getCollisionShape(level, cell).isEmpty();
	}

	private static ServerPlayer player(MinecraftServer minecraft) {
		return minecraft.getPlayerList().getPlayers().get(0);
	}

	private static void check(boolean condition, String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}

	/** Stands the camera at (x, y, z) in {@code dimension} (its feet), looking at (tx, ty, tz), and shoots. */
	private static void shootAt(ClientGameTestContext context, TestSingleplayerContext singleplayer, String dimension, double x, double y,
			double z, double tx, double ty, double tz, String name) {
		double dx = tx - x;
		double dy = ty - (y + 1.62);
		double dz = tz - z;
		int yaw = (int) Math.round(Math.toDegrees(Math.atan2(-dx, dz)));
		int pitch = (int) Math.round(-Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz))));
		shoot(context, singleplayer, dimension, x, y, z, yaw, pitch, name);
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
		server.runCommand(String.format(Locale.ROOT, "execute in %s run tp @a %.2f %.2f %.2f %d %d", dimension, x, y, z, yaw, pitch));
		context.waitTicks(30);
		singleplayer.getConnection().waitForChunksRender();
		context.waitTicks(10);
		context.takeScreenshot(name);
	}
}
