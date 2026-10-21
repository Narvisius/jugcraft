package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import io.github.jimbozoomer.jugcraft.lair.AltarKeystoneRite;
import io.github.jimbozoomer.jugcraft.lair.CydonianSanctum;
import io.github.jimbozoomer.jugcraft.lair.JugcraftLairs;
import io.github.jimbozoomer.jugcraft.lair.Lair;
import io.github.jimbozoomer.jugcraft.lair.LairInstance;
import io.github.jimbozoomer.jugcraft.lair.LairRules;
import io.github.jimbozoomer.jugcraft.lair.Lairs;
import io.github.jimbozoomer.jugcraft.lair.MistGateEntity;
import io.github.jimbozoomer.jugcraft.lair.StarDialBlock;
import io.github.jimbozoomer.jugcraft.lair.StarDialBlock.Stars;
import io.github.jimbozoomer.jugcraft.lair.StarDoorBlock;
import io.github.jimbozoomer.jugcraft.world.AdamOndiAhman;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
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
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The Altar Keystone and the Cydonian Sanctum (docs/features/cydonian-sanctum.md) in a real world, from end to end: an
 * Ancient Altar on its three altars is built in a new world, the keystone is set into it by day (refused) and at night by
 * the player's hand, the altars kindle and the red mist opens and takes the player onto Mars, into an instance placed
 * fresh; there the thin air, a dust vent's puff, the fixtures, the star dials and the Star Door, a fall, the Red Mist home,
 * a second keystone at the open mist, following back in with an empty hand, and the instance closing. Then the pictures,
 * and the light down the Dust Gallery.
 */
public class CydonianSanctumClientGameTests implements FabricClientGameTest {
	private static final Lair SANCTUM = Lair.CYDONIAN_SANCTUM;
	/** The least block light on any open cell of the Dust Gallery's floor (tools/cydonian_sanctum.py GALLERY_LIGHT_LEAST). */
	private static final int GALLERY_LIGHT = 7;
	/** The gallery's open cells (tools/cydonian_sanctum.py GALLERY). */
	private static final int GALLERY_WEST = 34;
	private static final int GALLERY_EAST = 38;
	private static final int GALLERY_NORTH = 76;
	private static final int GALLERY_SOUTH = 115;
	/** How high over the crater's floor the player drops from, to feel the thin air (blocks). */
	private static final int DROP = 12;

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
			// The three altars, stacked as the knoll's are (the lowest of cobblestone, then stone bricks, then chiseled), and
			// the Ancient Altar on top.
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 12, y, z - 12, x + 12, y + 12, z + 12));
			server.runCommand("fill %d %d %d %d %d %d minecraft:grass_block".formatted(x - 12, y - 1, z - 12, x + 12, y - 1, z + 12));
			server.runCommand("fill %d %d %d %d %d %d minecraft:mossy_cobblestone".formatted(x - 5, y, z - 5, x + 5, y, z + 5));
			server.runCommand("fill %d %d %d %d %d %d minecraft:stone_bricks".formatted(x - 3, y + 1, z - 3, x + 3, y + 1, z + 3));
			server.runCommand("fill %d %d %d %d %d %d minecraft:chiseled_stone_bricks".formatted(x - 1, y + 2, z - 1, x + 1, y + 2, z + 1));
			BlockPos altar = new BlockPos(x, y + 3, z);
			server.runOnServer(minecraft -> minecraft.overworld().setBlock(altar, AdamOndiAhman.ANCIENT_ALTAR.defaultBlockState(), 3));
			context.waitTicks(10);
			endToEnd(context, server, altar, new BlockPos(x + 6, y, z));
			pictures(context, singleplayer, server, altar, x, y, z);
		}
	}

	/**
	 * The keystone and the Sanctum from end to end, step by step, with the player in survival (the instances are the whole
	 * server's, so this one test owns them).
	 */
	private static void endToEnd(ClientGameTestContext context, TestServerContext server, BlockPos altar, BlockPos stand) {
		server.runCommand("gamemode survival @a");
		context.waitTicks(5);
		AtomicReference<LairInstance> first = new AtomicReference<>();
		AtomicReference<Vec3> stood = new AtomicReference<>();
		AtomicInteger taken = new AtomicInteger();
		BlockHitResult atAltar = new BlockHitResult(Vec3.atCenterOf(altar), Direction.UP, altar, false);

		// 1. By day the keystone is refused and kept.
		server.runOnServer(minecraft -> {
			ServerPlayer player = player(minecraft);
			ServerLevel overworld = minecraft.overworld();
			player.teleportTo(overworld, stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, Set.of(), 90.0F, 0.0F, true);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JugcraftLairs.ALTAR_KEYSTONE, 2));
			AltarKeystoneRite.Setting setting = AltarKeystoneRite.set(overworld, player, altar, player.getMainHandItem());
			check(setting.missing() == AltarKeystoneRite.Missing.NOT_NIGHT && player.getMainHandItem().getCount() == 2,
					"By day the keystone was not refused, or was used up: " + setting + ", " + player.getMainHandItem());
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JugcraftLairs.ALTAR_KEYSTONE));
		});
		server.runCommand("time set midnight");
		context.waitTicks(10);

		// 2. At night, set by hand: the keystone crumbles, the Sanctum is placed fresh and the altars kindle.
		server.runOnServer(minecraft -> {
			ServerPlayer player = player(minecraft);
			ServerLevel overworld = minecraft.overworld();
			stood.set(player.position());
			AltarKeystoneRite.Missing ready = AltarKeystoneRite.check(overworld, altar, true);
			check(ready == null, "The keystone could not open the Sanctum here: " + ready);
			InteractionResult set = player.gameMode.useItemOn(player, overworld, player.getMainHandItem(), InteractionHand.MAIN_HAND, atAltar);
			check(set.consumesAction(), "Setting the keystone did not succeed: " + set);
			check(player.getMainHandItem().isEmpty(), "The keystone was not used up: " + player.getMainHandItem());
			check(player.getCooldowns().isOnCooldown(new ItemStack(JugcraftLairs.ALTAR_KEYSTONE)), "The keystone was not rested after its setting");
			check(AltarKeystoneRite.kindling(overworld, altar), "The altars are not kindling");
			LairInstance instance = Lairs.open(SANCTUM).isEmpty() ? null : Lairs.open(SANCTUM).getFirst();
			check(instance != null, "The keystone opened no instance");
			first.set(instance);
			ServerLevel lair = minecraft.getLevel(SANCTUM.dimension);
			check(lair != null && lair.getMinY() == 0, "The Sanctum's dimension: from y 0");
			BlockPos o = instance.origin();
			for (BlockPos mist : CydonianSanctum.exitCells(o)) {
				check(lair.getBlockState(mist).is(JugcraftLairs.RED_MIST), "No Red Mist in the gate chamber's arch at " + mist.subtract(o));
			}
			for (BlockPos door : CydonianSanctum.doorCells(o)) {
				check(lair.getBlockState(door).is(JugcraftLairs.STAR_DOOR), "No Star Door at " + door.subtract(o));
			}
			List<Stars> starting = List.of(Stars.PLOUGH, Stars.LYRE, Stars.SCORPION);
			for (int i = 0; i < CydonianSanctum.DIALS.length; i++) {
				BlockState dial = lair.getBlockState(o.offset(CydonianSanctum.DIALS[i]));
				check(dial.is(JugcraftLairs.STAR_DIAL) && dial.getValue(StarDialBlock.STARS) == starting.get(i),
						"Star dial " + i + " is not at " + CydonianSanctum.DIALS[i] + " showing " + starting.get(i) + ": " + dial);
			}
			for (int line : CydonianSanctum.VENT_LINES) {
				for (int vx = CydonianSanctum.VENT_WEST; vx <= CydonianSanctum.VENT_EAST; vx++) {
					check(lair.getBlockState(o.offset(vx, CydonianSanctum.PAVE, line)).is(JugcraftLairs.DUST_VENT),
							"No dust vent at " + vx + ", " + line);
				}
			}
		});
		context.waitTicks(AltarKeystoneRite.KINDLE_TICKS * AltarKeystoneRite.ALTARS.length + 10);

		// 3. The mist opened and took the player onto Mars, unable to build. (With nobody left by the altar its chunk is
		// hidden, and the mist with it, until someone comes near again: step 6 finds it there.)
		server.runOnServer(minecraft -> {
			ServerPlayer player = player(minecraft);
			ServerLevel overworld = minecraft.overworld();
			ServerLevel lair = minecraft.getLevel(SANCTUM.dimension);
			check(!AltarKeystoneRite.kindling(overworld, altar), "The altars are still kindling");
			check(player.level() == lair && player.position().distanceTo(first.get().arrival()) < 0.5,
					"The player did not step out in the gate chamber: " + player.level().dimension() + " " + player.position());
			check(first.get().gateUntil() > overworld.getGameTime(), "No red mist was opened into the Sanctum: " + first.get().gateUntil());
			check(!player.getAbilities().mayBuild, "In a lair a player cannot build");
		});
		context.waitTicks(25);

		// 4. The thin air; a dust vent's puff; the fixtures.
		server.runOnServer(minecraft -> {
			ServerPlayer player = player(minecraft);
			ServerLevel lair = minecraft.getLevel(SANCTUM.dimension);
			MobEffectInstance jump = player.getEffect(MobEffects.JUMP_BOOST);
			check(jump != null && jump.getAmplifier() == CydonianSanctum.JUMP_AMPLIFIER, "On Mars the player does not jump higher: " + jump);
			check(Math.abs(player.getAttributeValue(Attributes.FALL_DAMAGE_MULTIPLIER) - CydonianSanctum.FALL_DAMAGE) < 1.0E-6,
					"On Mars the player's falls are not softened: " + player.getAttributeValue(Attributes.FALL_DAMAGE_MULTIPLIER));
			BlockPos o = first.get().origin();
			BlockPos dial = o.offset(CydonianSanctum.DIALS[0]);
			check(LairRules.useBlock(player, lair, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(dial), Direction.EAST, dial, false))
					== InteractionResult.PASS, "A star dial cannot be used");
			BlockPos vent = o.offset(36, CydonianSanctum.PAVE, CydonianSanctum.VENT_LINES[0]);
			check(LairRules.useBlock(player, lair, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(vent), Direction.UP, vent, false))
					== InteractionResult.FAIL, "A dust vent can be used");
			player.setHealth(20.0F);
			taken.set(damageTaken(player));
			player.teleportTo(vent.getX() + 0.5, vent.getY() + 1.0, vent.getZ() + 0.5);
		});
		// Two of the vents' periods, so a puff comes after the short shelter a player has on arriving in a dimension. The
		// damage is counted rather than read off the health, which peaceful mends a point a second.
		context.waitTicks(CydonianSanctum.VENT_PERIOD * 2 + 10);
		server.runOnServer(minecraft -> {
			ServerPlayer player = player(minecraft);
			float struck = (damageTaken(player) - taken.get()) / 10.0F;
			check(struck >= CydonianSanctum.VENT_DAMAGE - 0.01F, "Standing on a dust vent, the player was never struck: " + struck + " damage taken");
			player.setHealth(20.0F);
		});

		// 5. The star dials, turned by hand, light the door's figures, and the Star Door grinds open.
		server.runOnServer(minecraft -> {
			ServerPlayer player = player(minecraft);
			ServerLevel lair = minecraft.getLevel(SANCTUM.dimension);
			BlockPos o = first.get().origin();
			List<Stars> wanted = List.of(Stars.HUNTER, Stars.SWAN, Stars.CROWN);
			for (int i = 0; i < CydonianSanctum.DIALS.length; i++) {
				BlockPos dial = o.offset(CydonianSanctum.DIALS[i]);
				player.teleportTo(dial.getX() + (i == 1 ? -1.5 : 2.5), o.getY() + CydonianSanctum.STAND, dial.getZ() + 0.5);
				InteractionResult turned = player.gameMode.useItemOn(player, lair, ItemStack.EMPTY, InteractionHand.MAIN_HAND,
						new BlockHitResult(Vec3.atCenterOf(dial), i == 1 ? Direction.WEST : Direction.EAST, dial, false));
				check(turned.consumesAction() && lair.getBlockState(dial).getValue(StarDialBlock.STARS) == wanted.get(i),
						"Dial " + i + " did not turn to " + wanted.get(i) + ": " + turned + ", " + lair.getBlockState(dial));
			}
			for (BlockPos door : CydonianSanctum.doorCells(o)) {
				BlockState state = lair.getBlockState(door);
				check(!state.is(JugcraftLairs.STAR_DOOR) || state.getValue(StarDoorBlock.OPENING), "The door is not opening at " + door.subtract(o));
			}
		});
		context.waitTicks(StarDoorBlock.FIRST_ROW_TICKS + StarDoorBlock.ROW_TICKS * CydonianSanctum.DOOR_HEIGHT + 20);
		server.runOnServer(minecraft -> {
			ServerLevel lair = minecraft.getLevel(SANCTUM.dimension);
			check(CydonianSanctum.doorOpen(lair, first.get().origin()), "The Star Door did not grind open");
			// The thin air: a drop onto the crater's floor hurts half as much (and the jump's own cushion a block more).
			ServerPlayer player = player(minecraft);
			BlockPos o = first.get().origin();
			player.setHealth(20.0F);
			taken.set(damageTaken(player));
			player.teleportTo(o.getX() + CydonianSanctum.CENTRE_X, o.getY() + CydonianSanctum.STAND + DROP, o.getZ() + CydonianSanctum.CENTRE_Z - 6.0);
			player.setDeltaMovement(Vec3.ZERO);
		});
		context.waitTicks(60);
		server.runOnServer(minecraft -> {
			ServerPlayer player = player(minecraft);
			float lost = (damageTaken(player) - taken.get()) / 10.0F;
			// A full drop of DROP blocks costs DROP - 3 hearts' halves (at least DROP - 4 with the jump's cushion); Mars takes
			// half, rounded up at most.
			check(lost > 0.0F && lost <= Math.ceil((DROP - 3) * CydonianSanctum.FALL_DAMAGE) + 0.01F, "The drop did not hurt half as much: lost "
					+ lost);
			player.setHealth(20.0F);
		});

		// 6. Home through the Red Mist, the thin air gone; a second keystone at the open mist refused; back in by hand.
		server.runOnServer(minecraft -> {
			ServerPlayer player = player(minecraft);
			ServerLevel lair = minecraft.getLevel(SANCTUM.dimension);
			BlockPos exit = CydonianSanctum.exitCells(first.get().origin()).getFirst();
			player.teleportTo(exit.getX() + 0.5, exit.getY(), exit.getZ() - 1.5);
			InteractionResult left = player.gameMode.useItemOn(player, lair, ItemStack.EMPTY, InteractionHand.MAIN_HAND,
					new BlockHitResult(Vec3.atCenterOf(exit), Direction.SOUTH, exit, false));
			check(left.consumesAction() && player.level() == minecraft.overworld() && player.position().distanceTo(stood.get()) < 1.0E-6,
					"The Red Mist did not take the player back to where they stood: " + left + ", " + player.position());
		});
		context.waitTicks(30);
		server.runOnServer(minecraft -> {
			ServerPlayer player = player(minecraft);
			ServerLevel overworld = minecraft.overworld();
			check(player.getAttributeValue(Attributes.FALL_DAMAGE_MULTIPLIER) == 1.0, "Home, the player's falls are still softened");
			MistGateEntity mist = AltarKeystoneRite.mist(overworld, altar);
			check(mist != null && mist.instance() == first.get() && mist.lair() == SANCTUM, "No red mist over the altar");
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JugcraftLairs.ALTAR_KEYSTONE));
			check(AltarKeystoneRite.set(overworld, player, altar, player.getMainHandItem(), true).missing() == AltarKeystoneRite.Missing.MISTING
					&& player.getMainHandItem().getCount() == 1, "A second keystone at the open mist was not refused, or was used up");
			player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			InteractionResult followed = player.gameMode.useItemOn(player, overworld, ItemStack.EMPTY, InteractionHand.MAIN_HAND, atAltar);
			check(followed.consumesAction() && player.level() == minecraft.getLevel(SANCTUM.dimension)
					&& Lairs.instance(player.getAttached(Lairs.VISIT)) == first.get(), "The altar did not take the player back into the same Sanctum");
		});
		context.waitTicks(40);

		// 7. Closed, the player goes home, and the mist is gone.
		server.runOnServer(minecraft -> {
			ServerPlayer player = player(minecraft);
			Lairs.close(minecraft, first.get());
			check(player.level() == minecraft.overworld(), "Closing the Sanctum did not send the player home");
		});
		context.waitTicks(20);
		server.runOnServer(minecraft -> {
			ServerLevel overworld = minecraft.overworld();
			check(AltarKeystoneRite.mist(overworld, altar) == null
					&& overworld.getEntitiesOfClass(MistGateEntity.class, new AABB(altar).inflate(3)).isEmpty(), "The mist outlived its Sanctum");
			player(minecraft).setHealth(20.0F);
			Lairs.reset();
			AltarKeystoneRite.reset();
		});
		context.waitTicks(20);
		Jugcraft.LOGGER.info("[lairs] client game test: the Altar Keystone and the Cydonian Sanctum passed end to end");
	}

	/** The pictures, in creative, the mist held open long enough to take them all; then the light down the gallery. */
	private static void pictures(ClientGameTestContext context, TestSingleplayerContext singleplayer, TestServerContext server, BlockPos altar,
			int x, int y, int z) {
		server.runCommand("gamemode creative @a");
		server.runCommand("time set midnight");
		String gateSeconds = JugcraftConfig.textOption("lairs.gate_seconds");
		try {
			JugcraftConfig.setTextOption("lairs.gate_seconds", "600");
			AtomicReference<LairInstance> instance = new AtomicReference<>();
			server.runOnServer(minecraft -> {
				ServerPlayer player = player(minecraft);
				ServerLevel overworld = minecraft.overworld();
				player.teleportTo(overworld, x + 9.5, y + 2, z + 9.5, Set.of(), 135.0F, 0.0F, true);
				player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JugcraftLairs.ALTAR_KEYSTONE));
				AltarKeystoneRite.Setting setting = AltarKeystoneRite.set(overworld, player, altar, player.getMainHandItem(), true);
				check(setting.missing() == null, "The keystone opened no Sanctum for the pictures: " + setting);
				instance.set(setting.instance());
			});
			// The altars kindling (two of the three alight), then the red mist and its column (the camera is out of reach).
			context.waitTicks(AltarKeystoneRite.KINDLE_TICKS + 8);
			context.takeScreenshot("jugcraft_altar_keystone_kindling");
			context.waitTicks(AltarKeystoneRite.KINDLE_TICKS * 2 + 20);
			shootAt(context, singleplayer, "minecraft:overworld", x + 12.5, y + 1, z + 12.5, x + 0.5, y + 8, z + 0.5,
					"jugcraft_altar_keystone_red_mist");
			server.runOnServer(minecraft -> check(Lairs.enter(player(minecraft), instance.get()), "The camera could not go in"));
			context.waitTicks(40);
			singleplayer.getConnection().waitForChunksRender();
			BlockPos o = SANCTUM.origin(instance.get().slot);
			String dim = SANCTUM.dimension.identifier().toString();
			int stand = CydonianSanctum.STAND;
			shoot(context, singleplayer, dim, o.getX() + 36.5, o.getY() + stand, o.getZ() + 127.5, 180, 4, "jugcraft_cydonian_sanctum_arrival");
			shoot(context, singleplayer, dim, o.getX() + 36.5, o.getY() + stand + 1, o.getZ() + 113.5, 180, 12, "jugcraft_cydonian_sanctum_gallery");
			shootAt(context, singleplayer, dim, o.getX() + 36.5, o.getY() + stand, o.getZ() + 99.5, o.getX() + 39.5, o.getY() + stand + 1.5,
					o.getZ() + 99.5, "jugcraft_cydonian_sanctum_dial");
			shoot(context, singleplayer, dim, o.getX() + 36.5, o.getY() + stand, o.getZ() + 79.5, 180, -4, "jugcraft_cydonian_sanctum_star_door");
			server.runOnServer(minecraft -> {
				ServerLevel lair = minecraft.getLevel(SANCTUM.dimension);
				CydonianSanctum.match(lair, o, List.of(Stars.HUNTER, Stars.CROWN, Stars.LYRE), null);
			});
			shoot(context, singleplayer, dim, o.getX() + 36.5, o.getY() + stand, o.getZ() + 78.5, 180, -4, "jugcraft_cydonian_sanctum_star_door_lit");
			server.runOnServer(minecraft -> CydonianSanctum.match(minecraft.getLevel(SANCTUM.dimension), o,
					List.of(Stars.HUNTER, Stars.CROWN, Stars.SWAN), null));
			context.waitTicks(60);
			shoot(context, singleplayer, dim, o.getX() + 36.5, o.getY() + stand, o.getZ() + 79.5, 180, -2, "jugcraft_cydonian_sanctum_door_open");
			// The crater from the passage, the stones and the plinth across it, the moons over the north rim.
			shoot(context, singleplayer, dim, o.getX() + 36.5, o.getY() + stand + 2, o.getZ() + 70.5, 180, -8, "jugcraft_cydonian_sanctum_crater");
			shootAt(context, singleplayer, dim, o.getX() + 36.5, o.getY() + stand + 2, o.getZ() + 22.5, o.getX() + 36.5, o.getY() + stand,
					o.getZ() + 60.5, "jugcraft_cydonian_sanctum_from_plinth");
			shootAt(context, singleplayer, dim, o.getX() + 36.5, o.getY() + stand, o.getZ() + 40.5, o.getX() + 34.5, o.getY() + stand + 40,
					o.getZ() + 6.5, "jugcraft_cydonian_sanctum_moons");
			shootAt(context, singleplayer, dim, o.getX() + 39.5, o.getY() + stand, o.getZ() + 40.5, o.getX() + 44.5, o.getY() + stand + 2.5,
					o.getZ() + 38.5, "jugcraft_cydonian_sanctum_standing_stone");

			// The game's own light on every open cell of the gallery's floor.
			server.runOnServer(minecraft -> {
				ServerLevel lair = minecraft.getLevel(SANCTUM.dimension);
				int least = 15;
				int cells = 0;
				BlockPos darkest = null;
				for (int dx = GALLERY_WEST; dx <= GALLERY_EAST; dx++) {
					for (int dz = GALLERY_NORTH; dz <= GALLERY_SOUTH; dz++) {
						BlockPos cell = o.offset(dx, stand, dz);
						BlockState state = lair.getBlockState(cell);
						if (!state.isAir() && !state.is(Blocks.LIGHT)) {
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
				Jugcraft.LOGGER.info("[lairs] the Cydonian Sanctum's gallery: {} open cells, the darkest at block light {} ({})", cells, least, darkest);
				check(cells >= 190 && least >= GALLERY_LIGHT, "The gallery has " + cells + " open cells, the darkest at block light " + least
						+ " (" + darkest + "), under " + GALLERY_LIGHT);
			});
		} finally {
			JugcraftConfig.setTextOption("lairs.gate_seconds", gateSeconds);
			server.runOnServer(minecraft -> {
				Lairs.reset();
				AltarKeystoneRite.reset();
			});
		}
	}

	private static ServerPlayer player(MinecraftServer minecraft) {
		return minecraft.getPlayerList().getPlayers().get(0);
	}

	/** All the damage {@code player} has taken, in tenths of a point (the game's own count). */
	private static int damageTaken(ServerPlayer player) {
		return player.getStats().getValue(Stats.CUSTOM.get(Stats.DAMAGE_TAKEN));
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
