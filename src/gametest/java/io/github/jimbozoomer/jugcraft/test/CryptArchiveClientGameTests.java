package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.LongDecorationBlock;
import io.github.jimbozoomer.jugcraft.agriculture.SarcophagusBlock;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import io.github.jimbozoomer.jugcraft.lair.CryptArchive;
import io.github.jimbozoomer.jugcraft.lair.ExhumationRite;
import io.github.jimbozoomer.jugcraft.lair.GraveSealBlock;
import io.github.jimbozoomer.jugcraft.lair.JugcraftLairs;
import io.github.jimbozoomer.jugcraft.lair.Lair;
import io.github.jimbozoomer.jugcraft.lair.LairInstance;
import io.github.jimbozoomer.jugcraft.lair.LairRules;
import io.github.jimbozoomer.jugcraft.lair.Lairs;
import io.github.jimbozoomer.jugcraft.lair.MistGateEntity;
import io.github.jimbozoomer.jugcraft.lair.NameStoneBlock;
import io.github.jimbozoomer.jugcraft.lair.NameStoneBlock.Glow;
import io.github.jimbozoomer.jugcraft.lair.NameStoneBlock.Rune;
import io.github.jimbozoomer.jugcraft.lair.PhylacteryBlock;
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
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Client game test for the Archive of Last Names and the Writ of Exhumation (docs/features/crypt-archive.md), in a real
 * world, which has the lair dimensions: first the writ from end to end with the one player in survival
 * ({@link #endToEnd}); then pictures: the mist welling up from a sarcophagus in a crypt where the writ was served, and, in
 * the archive, the view down the stair from the landing, the hall from the stair's foot, the dais and the phylactery, the
 * Sexton's alcove with its seal and name-stone, a name-stone lit and one scraped blank, the throne's apse, the Lectern of
 * Last Names, the hall from the throne's apse and the Grey Mist in the way back up; last, the game's own block light on every
 * open cell of the hall's floor. CI job {@code client}.
 */
public class CryptArchiveClientGameTests implements FabricClientGameTest {
	private static final Lair ARCHIVE = Lair.CRYPT_ARCHIVE;
	/** Where tools/crypt_archive.py puts things (template positions), beside what CryptArchive.java gives. */
	private static final BlockPos UNDER_ARRIVAL = new BlockPos(32, 10, 63);
	private static final BlockPos[] EXIT_MIST = {new BlockPos(31, 11, 66), new BlockPos(32, 12, 66), new BlockPos(33, 13, 66)};
	private static final BlockPos PLINTH = new BlockPos(32, 5, 34);
	/** The least block light on any open cell of the hall's floor (tools/crypt_archive.py HALL_LIGHT, checked by the audit). */
	private static final int HALL_LIGHT = 7;

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
			server.runCommand("time set noon");
			server.runCommand("weather clear");
			// A crypt of stone bricks with a roof, lit inside, and ground beside it under the open sky.
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 10, y, z - 8, x + 10, y + 8, z + 8));
			server.runCommand("fill %d %d %d %d %d %d minecraft:stone".formatted(x - 10, y - 1, z - 8, x + 10, y - 1, z + 8));
			server.runCommand("fill %d %d %d %d %d %d minecraft:stone_bricks hollow".formatted(x - 7, y - 1, z - 5, x - 1, y + 4, z + 3));
			server.runCommand("setblock %d %d %d minecraft:light[level=12]".formatted(x - 2, y + 3, z + 2));
			server.runCommand("setblock %d %d %d minecraft:light[level=12]".formatted(x - 6, y + 3, z - 4));
			context.waitTicks(10);
			// A deepslate sarcophagus in the crypt, and another out under the sky, each lying north (foot to head).
			BlockPos crypt = new BlockPos(x - 4, y, z - 3);
			BlockPos open = new BlockPos(x + 4, y, z - 3);
			server.runOnServer(minecraft -> {
				tomb(minecraft.overworld(), crypt);
				tomb(minecraft.overworld(), open);
			});
			context.waitTicks(10);
			endToEnd(context, server, crypt, open, new BlockPos(x - 2, y, z));

			// The pictures, in creative, the mist held open long enough to take them all.
			server.runCommand("gamemode creative @a");
			String gateSeconds = JugcraftConfig.textOption("lairs.gate_seconds");
			try {
				JugcraftConfig.setTextOption("lairs.gate_seconds", "600");
				AtomicReference<LairInstance> instance = new AtomicReference<>();
				server.runOnServer(minecraft -> {
					ServerPlayer player = player(minecraft);
					ServerLevel overworld = minecraft.overworld();
					player.teleportTo(overworld, x - 1.5, y, z + 0.5, Set.of(), 135.0F, 0.0F, true);
					player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JugcraftLairs.EXHUMATION_WRIT));
					ExhumationRite.Serving serving = ExhumationRite.serve(overworld, player, crypt.south(), player.getMainHandItem());
					check(serving.missing() == null, "The writ opened no archive for the pictures: " + serving);
					instance.set(serving.instance());
				});
				context.waitTicks(40);
				// The open tomb in the crypt (the camera leaves the archive by command, which ends the visit; the mist stays).
				shootAt(context, singleplayer, "minecraft:overworld", x - 1.5, y + 0.5, z + 1.5, crypt.getX() + 0.5, y + 0.8, crypt.getZ() + 1.0,
						"jugcraft_exhumation_writ_mist");
				server.runOnServer(minecraft -> check(Lairs.enter(player(minecraft), instance.get()), "The camera could not go back in"));
				context.waitTicks(40);
				singleplayer.getConnection().waitForChunksRender();
				BlockPos o = ARCHIVE.origin(0);
				String dim = ARCHIVE.dimension.identifier().toString();
				int stand = CryptArchive.STAND;
				shoot(context, singleplayer, dim, o.getX() + 32.5, o.getY() + CryptArchive.LANDING_STAND, o.getZ() + 63.5, 180, 14,
						"jugcraft_crypt_archive_arrival");
				shoot(context, singleplayer, dim, o.getX() + 32.5, o.getY() + stand + 1, o.getZ() + 53.5, 180, 4, "jugcraft_crypt_archive_hall");
				shootAt(context, singleplayer, dim, o.getX() + 37.5, o.getY() + stand, o.getZ() + 39.5, o.getX() + 32.5, o.getY() + stand + 1.5,
						o.getZ() + 34.5, "jugcraft_crypt_archive_phylactery");
				shootAt(context, singleplayer, dim, o.getX() + 11.5, o.getY() + stand, o.getZ() + 36.5, o.getX() + 5.5, o.getY() + stand + 2.0,
						o.getZ() + 34.5, "jugcraft_crypt_archive_sexton");
				server.runOnServer(minecraft -> {
					ServerLevel lair = minecraft.getLevel(ARCHIVE.dimension);
					check(NameStoneBlock.setGlow(lair, CryptArchive.stone(o, Rune.QUILL), Glow.LIT), "The Scribe's name did not light");
					check(NameStoneBlock.setGlow(lair, CryptArchive.stone(o, Rune.CROWN), Glow.ERASED), "The Crowned's name was not scraped");
				});
				shootAt(context, singleplayer, dim, o.getX() + 54.5, o.getY() + stand, o.getZ() + 32.5, o.getX() + 59.5, o.getY() + stand + 2.0,
						o.getZ() + 34.5, "jugcraft_crypt_archive_name_lit");
				shootAt(context, singleplayer, dim, o.getX() + 29.5, o.getY() + CryptArchive.APSE_STAND, o.getZ() + 7.5, o.getX() + 32.5,
						o.getY() + CryptArchive.APSE_STAND + 2.5, o.getZ() + 5.5, "jugcraft_crypt_archive_name_erased");
				shoot(context, singleplayer, dim, o.getX() + 32.5, o.getY() + stand, o.getZ() + 23.5, 180, -6, "jugcraft_crypt_archive_throne");
				// The lectern from the stair behind it (its book faces the stair), the hall beyond.
				shootAt(context, singleplayer, dim, o.getX() + 32.5, o.getY() + stand + 2, o.getZ() + 56.5, o.getX() + 32.5, o.getY() + stand + 0.5,
						o.getZ() + 50.5, "jugcraft_crypt_archive_lectern");
				// The hall from the throne's apse, down the north lane over the dais to the stair: the Lich's own view.
				shootAt(context, singleplayer, dim, o.getX() + 32.5, o.getY() + CryptArchive.APSE_STAND + 2, o.getZ() + 11.5, o.getX() + 32.5,
						o.getY() + stand, o.getZ() + 40.5, "jugcraft_crypt_archive_from_throne");
				shoot(context, singleplayer, dim, o.getX() + 32.5, o.getY() + CryptArchive.LANDING_STAND, o.getZ() + 62.5, 0, 0,
						"jugcraft_crypt_archive_exit");

				// The game's own light on every open cell of the hall's floor, where the Lich and whoever fights him are seen.
				server.runOnServer(minecraft -> {
					ServerLevel lair = minecraft.getLevel(ARCHIVE.dimension);
					int least = 15;
					int cells = 0;
					BlockPos darkest = null;
					for (int dx = CryptArchive.HALL_WEST; dx <= CryptArchive.HALL_EAST; dx++) {
						for (int dz = CryptArchive.HALL_NORTH; dz <= CryptArchive.HALL_SOUTH; dz++) {
							BlockPos cell = o.offset(dx, stand, dz);
							BlockState state = lair.getBlockState(cell);
							if (!state.isAir() && !state.is(Blocks.LIGHT) && !state.getCollisionShape(lair, cell).isEmpty()) {
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
					Jugcraft.LOGGER.info("[lairs] the Archive of Last Names' hall: {} open cells, the darkest at block light {} ({})", cells, least,
							darkest);
					check(cells > 1400 && least >= HALL_LIGHT, "The archive's hall has " + cells + " open cells, the darkest at block light " + least
							+ " (" + darkest + "), under " + HALL_LIGHT);
				});
			} finally {
				JugcraftConfig.setTextOption("lairs.gate_seconds", gateSeconds);
				server.runOnServer(minecraft -> Lairs.reset());
			}
		}
	}

	/**
	 * The Writ of Exhumation from end to end, step by step, with the player in survival (the instances are the whole
	 * server's, so this one test owns them):
	 * <ol>
	 * <li>served on the sarcophagus out under the sky, the writ is refused and kept;</li>
	 * <li>served by the player's hand on the sarcophagus in the crypt, it opens an instance in the archive's own dimension,
	 * places the archive fresh (the landing under the arrival, the Grey Mist in the way back up, each name's seal whole and
	 * name-stone dark with its rune, the lectern, the whole phylactery on its plinth, the throne), uses up the writ and
	 * rests it, grinds the lid aside on both halves, raises a mist over the tomb, and takes the player down, unable to
	 * build;</li>
	 * <li>in the archive a grave seal and the lectern are fixtures and a name-stone is not; a seal used with no Lich to bind
	 * it stays whole; a name scraped from its stone is cut again when the player reads the lectern; falling off the archive
	 * throws them back to the landing for the mist's toll;</li>
	 * <li>leaving takes the player back to where they stood; while the mist is risen a second writ served there is refused
	 * and kept, and using the open sarcophagus with an empty hand takes them back into the same archive;</li>
	 * <li>when the instance closes the player goes home, the mist is gone and the lid grinds shut.</li>
	 * </ol>
	 */
	private static void endToEnd(ClientGameTestContext context, TestServerContext server, BlockPos crypt, BlockPos open, BlockPos inside) {
		server.runCommand("gamemode survival @a");
		context.waitTicks(5);
		AtomicReference<LairInstance> first = new AtomicReference<>();
		AtomicReference<Vec3> stood = new AtomicReference<>();
		BlockPos cryptFoot = crypt.south();

		// 1. Refused under the open sky.
		server.runOnServer(minecraft -> {
			ServerPlayer player = player(minecraft);
			ServerLevel overworld = minecraft.overworld();
			player.teleportTo(overworld, open.getX() + 2.5, open.getY(), open.getZ() + 1.5, Set.of(), 90.0F, 0.0F, true);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JugcraftLairs.EXHUMATION_WRIT, 2));
			ExhumationRite.Serving serving = ExhumationRite.serve(overworld, player, open.south(), player.getMainHandItem());
			check(serving.missing() == ExhumationRite.Missing.OPEN_SKY && player.getMainHandItem().getCount() == 2,
					"Under the open sky the writ was not refused, or was used up: " + serving + ", " + player.getMainHandItem());
			player.teleportTo(overworld, inside.getX() + 0.5, inside.getY(), inside.getZ() + 0.5, Set.of(), 135.0F, 0.0F, true);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JugcraftLairs.EXHUMATION_WRIT));
		});
		context.waitTicks(10);

		// 2. In the crypt, the writ served by hand opens the archive and the player goes down the stair.
		server.runOnServer(minecraft -> {
			ServerPlayer player = player(minecraft);
			ServerLevel overworld = minecraft.overworld();
			stood.set(player.position());
			ExhumationRite.Missing ready = ExhumationRite.check(overworld, crypt);
			check(ready == null, "The writ could not open the archive here: " + ready);
			BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(cryptFoot), Direction.UP, cryptFoot, false);
			InteractionResult served = player.gameMode.useItemOn(player, overworld, player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
			check(served.consumesAction(), "Serving the writ did not succeed: " + served);
		});
		server.runOnServer(minecraft -> {
			ServerPlayer player = player(minecraft);
			ServerLevel overworld = minecraft.overworld();
			LairInstance instance = Lairs.open(ARCHIVE).isEmpty() ? null : Lairs.open(ARCHIVE).getFirst();
			check(instance != null, "The writ opened no instance");
			first.set(instance);
			check(player.getMainHandItem().isEmpty(), "The writ was not used up: " + player.getMainHandItem());
			check(player.getCooldowns().isOnCooldown(new ItemStack(JugcraftLairs.EXHUMATION_WRIT)), "The writ was not rested after its serving");
			check(overworld.getBlockState(crypt).getValue(SarcophagusBlock.OPEN) && overworld.getBlockState(cryptFoot).getValue(SarcophagusBlock.OPEN),
					"The lid did not grind aside on both halves");
			ServerLevel lair = minecraft.getLevel(ARCHIVE.dimension);
			check(lair != null && lair.getMinY() == 0, "The archive's dimension: from y 0");
			BlockPos o = instance.origin();
			check(lair.getBlockState(o.offset(UNDER_ARRIVAL)).is(Blocks.DEEPSLATE_TILES), "No landing under the arrival");
			for (BlockPos mist : EXIT_MIST) {
				check(lair.getBlockState(o.offset(mist)).is(JugcraftLairs.LAIR_EXIT), "No Grey Mist in the way back up at " + mist);
			}
			for (Rune rune : Rune.values()) {
				BlockState seal = lair.getBlockState(CryptArchive.seal(o, rune));
				BlockState stone = lair.getBlockState(CryptArchive.stone(o, rune));
				check(seal.is(JugcraftLairs.GRAVE_SEAL) && seal.getValue(NameStoneBlock.RUNE) == rune && !seal.getValue(GraveSealBlock.BROKEN),
						"No whole seal of the " + rune.title() + " at " + CryptArchive.SEALS[rune.ordinal()]);
				check(stone.is(JugcraftLairs.NAME_STONE) && stone.getValue(NameStoneBlock.RUNE) == rune && stone.getValue(NameStoneBlock.GLOW) == Glow.DARK,
						"No dark name-stone of the " + rune.title() + " at " + CryptArchive.STONES[rune.ordinal()]);
			}
			check(lair.getBlockState(o.offset(CryptArchive.LECTERN)).is(JugcraftLairs.ARCHIVE_LECTERN), "No Lectern of Last Names");
			check(lair.getBlockState(o.offset(CryptArchive.PHYLACTERY)).is(JugcraftLairs.PHYLACTERY)
					&& !lair.getBlockState(o.offset(CryptArchive.PHYLACTERY)).getValue(PhylacteryBlock.CRACKED)
					&& lair.getBlockState(o.offset(PLINTH)).is(JugcraftAgriculture.block("chiseled_crypt_stone")), "No whole phylactery on its plinth");
			check(lair.getBlockState(o.offset(CryptArchive.THRONE)).is(JugcraftAgriculture.block("bone_throne")), "No throne in the apse");
			check(player.level() == lair && player.position().distanceTo(instance.arrival()) < 0.5,
					"The player did not come to the landing: " + player.level().dimension() + " " + player.position());
			check(!player.getAbilities().mayBuild, "In a lair a player cannot build");
			MistGateEntity mist = ExhumationRite.mist(overworld, crypt);
			check(mist != null && mist.instance() == instance && mist.lair() == ARCHIVE, "No mist over the tomb where the writ was served");
		});
		context.waitTicks(40);

		// 3. The seals and the lectern are fixtures, a name-stone is not; a seal with no Lich stays whole; a scraped name is
		// read back at the lectern; the archive's edge.
		server.runOnServer(minecraft -> {
			ServerPlayer player = player(minecraft);
			ServerLevel lair = minecraft.getLevel(ARCHIVE.dimension);
			BlockPos o = first.get().origin();
			BlockPos seal = CryptArchive.seal(o, Rune.SPADE);
			BlockHitResult atSeal = new BlockHitResult(Vec3.atCenterOf(seal), Direction.EAST, seal, false);
			check(LairRules.useBlock(player, lair, InteractionHand.MAIN_HAND, atSeal) == InteractionResult.PASS, "A grave seal cannot be used");
			BlockPos stone = CryptArchive.stone(o, Rune.SPADE);
			check(LairRules.useBlock(player, lair, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(stone), Direction.EAST, stone, false))
					== InteractionResult.FAIL, "A name-stone can be used");
			player.teleportTo(seal.getX() + 3.5, seal.getY() - 1.0, seal.getZ() + 0.5);
			InteractionResult used = player.gameMode.useItemOn(player, lair, ItemStack.EMPTY, InteractionHand.MAIN_HAND, atSeal);
			check(used.consumesAction() && !lair.getBlockState(seal).getValue(GraveSealBlock.BROKEN),
					"With no Lich to bind it the seal broke, or the use was not taken: " + used);
			BlockPos crowned = CryptArchive.stone(o, Rune.CROWN);
			check(NameStoneBlock.setGlow(lair, crowned, Glow.ERASED), "The Crowned's name was not scraped");
			BlockPos lectern = o.offset(CryptArchive.LECTERN);
			player.teleportTo(lectern.getX() + 1.5, lectern.getY(), lectern.getZ() + 1.5);
			InteractionResult read = player.gameMode.useItemOn(player, lair, ItemStack.EMPTY, InteractionHand.MAIN_HAND,
					new BlockHitResult(Vec3.atCenterOf(lectern), Direction.SOUTH, lectern, false));
			check(read.consumesAction() && lair.getBlockState(crowned).getValue(NameStoneBlock.GLOW) == Glow.DARK,
					"Reading the lectern did not cut the Crowned's name again: " + read + ", " + lair.getBlockState(crowned));
			Vec3 arrival = first.get().arrival();
			player.teleportTo(o.getX() + 32.5, o.getY() + ARCHIVE.floor - 2.0, o.getZ() + 34.5);
			player.setHealth(20.0F);
			check(Lairs.edges(player, first.get()) && player.position().distanceTo(arrival) < 0.5
					&& player.getHealth() == 20.0F - Lairs.EDGE_DAMAGE, "Falling out of the archive did not end on the landing");
			player.setHealth(20.0F);
		});

		// 4. Out, a second writ refused at the risen mist, and back in through the open tomb.
		server.runOnServer(minecraft -> {
			ServerPlayer player = player(minecraft);
			check(Lairs.leave(player) && player.level() == minecraft.overworld() && player.position().distanceTo(stood.get()) < 1.0E-6,
					"The player did not come back to where they stood: " + player.position());
		});
		context.waitTicks(40);
		server.runOnServer(minecraft -> {
			ServerPlayer player = player(minecraft);
			ServerLevel overworld = minecraft.overworld();
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JugcraftLairs.EXHUMATION_WRIT));
			check(ExhumationRite.serve(overworld, player, cryptFoot, player.getMainHandItem()).missing() == ExhumationRite.Missing.MISTING
					&& player.getMainHandItem().getCount() == 1, "A second writ at the risen mist was not refused, or was used up");
			player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			check(ExhumationRite.mist(overworld, crypt) != null, "The mist sank early");
			InteractionResult followed = player.gameMode.useItemOn(player, overworld, ItemStack.EMPTY, InteractionHand.MAIN_HAND,
					new BlockHitResult(Vec3.atCenterOf(cryptFoot), Direction.UP, cryptFoot, false));
			check(followed.consumesAction() && player.level() == minecraft.getLevel(ARCHIVE.dimension)
					&& Lairs.instance(player.getAttached(Lairs.VISIT)) == first.get(), "The open tomb did not take the player back into the same archive");
		});
		context.waitTicks(40);

		// 5. Closed, the player goes home, the mist is gone and the lid grinds shut.
		server.runOnServer(minecraft -> {
			ServerPlayer player = player(minecraft);
			Lairs.close(minecraft, first.get());
			check(player.level() == minecraft.overworld(), "Closing the archive did not send the player home");
		});
		context.waitTicks(50);
		server.runOnServer(minecraft -> {
			ServerLevel overworld = minecraft.overworld();
			check(ExhumationRite.mist(overworld, crypt) == null
					&& overworld.getEntitiesOfClass(MistGateEntity.class, new AABB(crypt).inflate(3)).isEmpty(), "The mist outlived its archive");
			check(!overworld.getBlockState(crypt).getValue(SarcophagusBlock.OPEN) && !overworld.getBlockState(cryptFoot).getValue(SarcophagusBlock.OPEN),
					"The lid did not grind shut when the mist was gone");
			player(minecraft).setHealth(20.0F);
			Lairs.reset();
		});
		context.waitTicks(20);
		Jugcraft.LOGGER.info("[lairs] client game test: the Writ of Exhumation passed end to end");
	}

	/** Lays a deepslate sarcophagus with its head at {@code head}, lying north (its foot to the south). */
	private static void tomb(ServerLevel level, BlockPos head) {
		BlockState foot = JugcraftAgriculture.block("deepslate_sarcophagus").defaultBlockState()
				.setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH).setValue(LongDecorationBlock.PART, BedPart.FOOT);
		level.setBlock(head.south(), foot, 3);
		level.setBlock(head, foot.setValue(LongDecorationBlock.PART, BedPart.HEAD), 3);
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
		server.runCommand(String.format(Locale.ROOT, "execute in %s run tp @a %.1f %.1f %.1f %d %d", dimension, x, y, z, yaw, pitch));
		context.waitTicks(30);
		singleplayer.getConnection().waitForChunksRender();
		context.waitTicks(10);
		context.takeScreenshot(name);
	}
}
