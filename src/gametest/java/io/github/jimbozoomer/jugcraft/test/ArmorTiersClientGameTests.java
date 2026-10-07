package io.github.jimbozoomer.jugcraft.test;

import com.google.gson.JsonParser;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.client.WornModelLayer;
import io.github.jimbozoomer.jugcraft.gear.JugcraftGear;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.AABB;

/**
 * Client game test for the armor-only tiers (docs/features/bloodthorn-armor.md), the owner's armor designs worn as 3D
 * models, one scene for each tier in JugcraftGear.ARMOR_TIERS, so a new tier is shown with no change here.
 * <ul>
 * <li>First the client checks that it read a worn model for every body part worn_models.json gives each piece
 * ({@link WornModelLayer#bones}), and that every piece has one.</li>
 * <li>A row by day, left to right from the front: vanilla netherite on an armor stand with arms, for scale, then the
 * tier on a stand, the tier with Protection IV on every piece (its glint) and the tier on a zombie. The server checks
 * what each wears before any shot. The row turns to face the camera, then three-quarter (its right side), side (its
 * left) and back; each view is shot whole, then the tier's stand close up.</li>
 * <li>The player in the tier from the front, standing, then sneaking with the real sneak key, from the front and from
 * behind.</li>
 * <li>The four icons in frames on a wall.</li>
 * </ul>
 * Shots are named jugcraft_armor_tier_&lt;tier&gt;_*. The HUD is hidden whatever state an earlier test left it in, and
 * put back at the end. CI job {@code client}.
 */
public class ArmorTiersClientGameTests implements FabricClientGameTest {
	private static final String[] PIECES = {"helmet", "chestplate", "leggings", "boots"};
	private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
	/** The equipment keys in a summon command, and the item replace slots after "armor.", for each piece. */
	private static final String[] KEYS = {"head", "chest", "legs", "feet"};
	/** The body parts a key in worn_models.json can end with (WornModelLayer's Bone). */
	private static final String[] BONES = {"head", "body", "right_arm", "left_arm", "right_leg", "left_leg"};
	/** The row, west to east (left to right from the front): each wearer, and whether it wears the tier (or netherite). */
	private static final String[] WEARERS = {"armor_stand", "armor_stand", "armor_stand", "zombie"};
	private static final boolean[] WEARS_TIER = {false, true, true, true};
	/** The wearer whose pieces are enchanted, and the tier's stand of the close-ups. */
	private static final int ENCHANTED = 2;
	private static final int CLOSE = 1;
	/** Blocks between the wearers' centres; the row's z from the origin; the cameras' distances south of the row. */
	private static final double SPACING = 2.5;
	private static final double ROW_Z = -5.5;
	private static final double ROW_CAMERA = 6.5;
	private static final double CLOSE_CAMERA = 2.8;
	/** The views: which way the row turns (yaw; 0 faces the camera, which looks north) and the shots' names. */
	private static final int[] TURNS = {0, -45, 90, 180};
	private static final String[] VIEWS = {"front", "three_quarter", "side", "back"};

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext singleplayer = context.worldBuilder()
				.adjustSettings(creator -> creator.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE)).create()) {
			singleplayer.getConnection().waitForChunksRender();
			BlockPos origin = context.computeOnClient(client -> BlockPos.containing(client.player.position()));
			int x = origin.getX();
			int y = origin.getY();
			int z = origin.getZ();
			TestServerContext server = singleplayer.getServer();
			server.runCommand("time set noon");
			server.runCommand("weather clear");
			server.runCommand("gamerule minecraft:send_command_feedback false");
			server.runCommand("gamerule minecraft:advance_time false");
			server.runCommand("gamerule minecraft:spawn_mobs false");
			server.runCommand("fill %d %d %d %d %d %d minecraft:grass_block".formatted(x - 10, y - 1, z - 16, x + 12, y - 1, z + 8));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 10, y, z - 16, x + 12, y + 8, z + 8));
			context.waitTicks(10);
			boolean hudWasHidden = context.computeOnClient(client -> client.gui.hud.isHidden());
			setHudHidden(context, true);

			String unread = context.computeOnClient(client -> unreadModels(client));
			check(unread.isEmpty(), "Worn models the client did not read: " + unread);

			for (String tier : JugcraftGear.ARMOR_TIERS) {
				showcase(context, singleplayer, x, y, z, tier);
			}
			setHudHidden(context, hudWasHidden);
		}
	}

	/** One tier's scene: the row in four views, the player standing and sneaking, and the icons. */
	private static void showcase(ClientGameTestContext context, TestSingleplayerContext singleplayer, int x, int y, int z, String tier) {
		TestServerContext server = singleplayer.getServer();
		String shot = "jugcraft_armor_tier_" + tier;
		double rowZ = z + ROW_Z;
		for (int i = 0; i < WEARERS.length; i++) {
			summon(server, WEARERS[i], wearerX(x, i), y, rowZ, WEARS_TIER[i] ? "jugcraft:" + tier : "minecraft:netherite");
		}
		context.waitTicks(5);
		double enchantedX = wearerX(x, ENCHANTED);
		server.runOnServer(minecraft -> enchant(minecraft.overworld(),
				new AABB(enchantedX - 1, y - 1, rowZ - 1, enchantedX + 1, y + 3, rowZ + 1)));
		context.waitTicks(15);
		String wrong = server.computeOnServer(minecraft -> checkRow(minecraft.overworld(),
				new AABB(x - 8, y - 1, rowZ - 2, x + 9, y + 3, rowZ + 2), tier));
		Jugcraft.LOGGER.info("[armor tiers client] the {} row: {}", tier, wrong.isEmpty() ? "dressed as the shots need" : wrong);
		check(wrong.isEmpty(), "The " + tier + " row is not dressed as the shots need: " + wrong);

		// Each view: the row turned, shot whole, then the tier's stand close up. The camera is the player, so players are
		// left out of the selector.
		String box = "x=%d,y=%d,z=%d,dx=17,dy=4,dz=5".formatted(x - 8, y - 1, z - 8);
		for (int view = 0; view < VIEWS.length; view++) {
			server.runCommand("execute as @e[type=!minecraft:player,%s] at @s run tp @s ~ ~ ~ %d 0".formatted(box, TURNS[view]));
			// A zombie's helmet wears away in the sun, and without one it burns: a fresh helmet each view.
			server.runCommand("item replace entity @e[type=minecraft:zombie,%s] armor.head with jugcraft:%s_helmet".formatted(box, tier));
			shoot(context, singleplayer, x + 0.5, y, rowZ + ROW_CAMERA, 6, shot + "_" + VIEWS[view]);
			shoot(context, singleplayer, wearerX(x, CLOSE), y, rowZ + CLOSE_CAMERA, 8, shot + "_close_" + VIEWS[view]);
		}
		server.runCommand("kill @e[type=!minecraft:player,%s]".formatted(box));
		server.runCommand("kill @e[type=minecraft:item]");

		// The player in the tier from the front: standing, then sneaking with the sneak key held, and that from behind.
		for (int p = 0; p < PIECES.length; p++) {
			server.runCommand("item replace entity @p armor.%s with jugcraft:%s_%s".formatted(KEYS[p], tier, PIECES[p]));
		}
		context.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
		shoot(context, singleplayer, x + 0.5, y, z + 3.5, 0, shot + "_player");
		context.getInput().holdKey(options -> options.keyShift);
		context.waitTicks(15);
		boolean sneaking = context.computeOnClient(client -> client.player.isShiftKeyDown());
		context.takeScreenshot(shot + "_player_sneaking");
		context.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_BACK));
		context.waitTicks(5);
		context.takeScreenshot(shot + "_player_sneaking_back");
		context.getInput().releaseKey(options -> options.keyShift);
		context.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));
		for (String key : KEYS) {
			server.runCommand("item replace entity @p armor.%s with minecraft:air".formatted(key));
		}
		Jugcraft.LOGGER.info("[armor tiers client] the player sneaking for the {} sneaking shot: {}", tier, sneaking);
		check(sneaking, "The player was not sneaking for the " + tier + " sneaking shot");

		// The four icons in frames on a wall, helmet to boots from left to right.
		server.runCommand("fill %d %d %d %d %d %d minecraft:spruce_planks".formatted(x - 1, y, z - 12, x + 4, y + 3, z - 12));
		for (int p = 0; p < PIECES.length; p++) {
			frame(server, x + p, y + 1, z - 11, tier + "_" + PIECES[p]);
		}
		context.waitTicks(20);
		AABB wall = new AABB(x - 1, y, z - 12, x + 5, y + 4, z - 10);
		int frames = server.computeOnServer(minecraft -> minecraft.overworld().getEntitiesOfClass(Entity.class, wall,
				entity -> type(entity).equals("item_frame")).size());
		check(frames == PIECES.length, frames + " item frames on the " + tier + " wall, not " + PIECES.length);
		shoot(context, singleplayer, x + 2.0, y, z - 8.5, -2, shot + "_icons");
		server.runCommand("kill @e[type=minecraft:item_frame]");
		server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 1, y, z - 12, x + 4, y + 3, z - 12));
	}

	/** The x of wearer {@code i}'s centre, the row centred on the origin's block. */
	private static double wearerX(int x, int i) {
		return x + 0.5 + SPACING * (i - (WEARERS.length - 1) / 2.0);
	}

	/** Summons a {@code wearer} (an armor stand with arms, or a zombie standing still) facing south, in {@code set}'s four pieces. */
	private static void summon(TestServerContext server, String wearer, double x, int y, double z, String set) {
		StringBuilder equipment = new StringBuilder();
		for (int p = 0; p < PIECES.length; p++) {
			equipment.append(p == 0 ? "" : ",").append("%s:{id:\"%s_%s\",count:1}".formatted(KEYS[p], set, PIECES[p]));
		}
		String kind = wearer.equals("zombie") ? "NoAI:1b,PersistenceRequired:1b,Silent:1b" : "ShowArms:1b,NoBasePlate:1b";
		server.runCommand(String.format(Locale.ROOT, "summon minecraft:%s %.2f %d %.1f {%s,Rotation:[0f,0f],equipment:{%s}}", wearer, x, y,
				z, kind, equipment));
	}

	/** Swaps every piece the armor stands in {@code around} wear for the same piece with Protection IV. */
	private static void enchant(ServerLevel level, AABB around) {
		var enchantments = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
		for (LivingEntity stand : level.getEntitiesOfClass(LivingEntity.class, around, entity -> type(entity).equals("armor_stand"))) {
			for (EquipmentSlot slot : SLOTS) {
				ItemStack piece = new ItemStack(stand.getItemBySlot(slot).getItem());
				piece.enchant(enchantments.getOrThrow(Enchantments.PROTECTION), 4);
				stand.setItemSlot(slot, piece);
			}
		}
	}

	/**
	 * What is wrong with the row in {@code around}, or an empty string: from west to east, each wearer must be of its
	 * kind and wear its four pieces (netherite, or the tier's), glinting on the enchanted one alone.
	 */
	private static String checkRow(ServerLevel level, AABB around, String tier) {
		List<LivingEntity> found = new ArrayList<>(level.getEntitiesOfClass(LivingEntity.class, around,
				entity -> type(entity).equals("armor_stand") || type(entity).equals("zombie")));
		if (found.size() != WEARERS.length) {
			return found.size() + " wearers in the row, not " + WEARERS.length;
		}
		found.sort(Comparator.comparingDouble(LivingEntity::getX));
		List<String> wrong = new ArrayList<>();
		for (int i = 0; i < WEARERS.length; i++) {
			LivingEntity wearer = found.get(i);
			if (!type(wearer).equals(WEARERS[i])) {
				wrong.add("wearer " + (i + 1) + " is a " + type(wearer) + ", not a " + WEARERS[i]);
			}
			String set = WEARS_TIER[i] ? "jugcraft:" + tier : "minecraft:netherite";
			for (int p = 0; p < PIECES.length; p++) {
				ItemStack stack = wearer.getItemBySlot(SLOTS[p]);
				String worn = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
				if (!worn.equals(set + "_" + PIECES[p])) {
					wrong.add("wearer " + (i + 1) + " wears " + worn + " on its " + KEYS[p] + ", not " + set + "_" + PIECES[p]);
				}
				if (stack.hasFoil() != (i == ENCHANTED)) {
					wrong.add("wearer " + (i + 1) + "'s " + PIECES[p] + (i == ENCHANTED ? " does not glint" : " glints"));
				}
			}
		}
		return String.join("; ", wrong);
	}

	/**
	 * What the client has not read of worn_models.json, or an empty string: every piece of every tier must have worn
	 * models, and one for every body part the file gives it. Logs what each one has.
	 */
	private static String unreadModels(Minecraft client) {
		Optional<Resource> resource = client.getResourceManager().getResource(Jugcraft.id("worn_models.json"));
		if (resource.isEmpty()) {
			return "there is no worn_models.json";
		}
		Set<String> keys;
		try (Reader reader = resource.get().openAsReader()) {
			keys = JsonParser.parseReader(reader).getAsJsonObject().keySet();
		} catch (IOException e) {
			return "worn_models.json could not be read: " + e;
		}
		List<String> wrong = new ArrayList<>();
		List<String> read = new ArrayList<>();
		for (String tier : JugcraftGear.ARMOR_TIERS) {
			for (String piece : PIECES) {
				String id = tier + "_" + piece;
				int expected = parts(keys, id);
				int bones = WornModelLayer.bones(JugcraftGear.ITEMS.get(id));
				read.add(id + " " + bones);
				if (expected == 0) {
					wrong.add(id + " has no worn models in worn_models.json");
				} else if (bones != expected) {
					wrong.add(id + " has " + bones + " of its " + expected + " body parts");
				}
			}
		}
		Jugcraft.LOGGER.info("[armor tiers client] worn body parts read: {}", String.join(", ", read));
		return String.join("; ", wrong);
	}

	/** How many body parts worn_models.json has a model of {@code name} for. */
	private static int parts(Set<String> keys, String name) {
		int count = 0;
		for (String bone : BONES) {
			if (keys.contains(name + "_" + bone)) {
				count++;
			}
		}
		return count;
	}

	private static String type(Entity entity) {
		return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath();
	}

	private static void frame(TestServerContext server, int x, int y, int z, String item) {
		server.runCommand("summon minecraft:item_frame %d %d %d {Facing:3b,Fixed:1b,Item:{id:\"jugcraft:%s\",count:1}}".formatted(x, y, z, item));
	}

	/** Stands the camera at (x, y, z) looking north and {@code pitch} down, waits for the world to draw, and takes the shot. */
	private static void shoot(ClientGameTestContext context, TestSingleplayerContext singleplayer, double x, int y, double z, int pitch,
			String name) {
		singleplayer.getServer().runCommand(String.format(Locale.ROOT, "tp @p %.2f %d %.2f 180 %d", x, y, z, pitch));
		context.waitTicks(40);
		singleplayer.getConnection().waitForChunksRender();
		context.takeScreenshot(name);
	}

	/** Hides or shows the HUD, hand and chat (F1) whatever state an earlier test left it in, as DroneClientGameTests does. */
	private static void setHudHidden(ClientGameTestContext context, boolean hidden) {
		context.runOnClient(client -> {
			if (client.gui.hud.isHidden() != hidden) {
				client.gui.hud.toggle();
			}
		});
	}

	private static void check(boolean ok, String message) {
		if (!ok) {
			throw new AssertionError(message);
		}
	}
}
