package io.github.jimbozoomer.jugcraft.test;

import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.cache.GeckoLibResources;
import com.geckolib.cache.model.BakedGeoModel;
import com.geckolib.renderer.GeoArmorRenderer;
import eu.pb4.trinkets.api.TrinketsApi;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Invocations;
import io.github.jimbozoomer.jugcraft.concordance.ember.Ember;
import io.github.jimbozoomer.jugcraft.concordance.ember.EmberGear;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.AABB;

/**
 * Client game test for Ember's regalia (docs/features/arcane-concordance-ember-regalia.md): the two fire sets worn as the
 * owner's GeckoLib model, and the foci in the real Trinkets slot.
 * <ul>
 * <li>First the client checks that GeckoLib has a renderer for each piece of both sets, one for each set, wearing the
 * owner's model with that set's own texture; that it loaded the model with every bone the renderer poses (and the slim
 * sleeves it hides); and that both sets' textures are there.</li>
 * <li>A row by day, left to right from the front: vanilla iron on an armour stand (the control), the Pyromaniac's (light)
 * set and the Pyromancer's (medium) set on stands (the owner's light texture is the medium sheet, so they should look
 * alike), the Pyromancer's with Protection IV (its glint), the Pyromancer's on a zombie (arms raised: the sleeves must
 * follow them), and on a small stand (which should show nothing). The server checks what each wears first. The row turns
 * to face the camera, then three-quarter, side and back; each view is shot whole, then the light and medium stands close
 * up.</li>
 * <li>The player in the set, standing and sneaking with the real sneak key, from the front and behind. On the real,
 * ticking player the server then puts a Focus of Fire in the Spell Focus slot: with the set that is 6 fire Spell Power
 * above the base; with the lesser focus instead, 4.</li>
 * <li>The eleven icons in frames on a wall: the foci, bangle and light set above, the medium set below.</li>
 * </ul>
 * The shots are for people to look at; nothing here judges a picture. The HUD is hidden whatever state an earlier test
 * left it in, and put back at the end.
 */
public class ConcordanceEmberGearClientGameTests implements FabricClientGameTest {
	private static final String[] PIECES = {"pyromancers_hat", "pyromancers_robes", "pyromancers_leggings", "pyromancers_boots"};
	private static final String[] LIGHT = {"pyromaniacs_hood", "pyromaniacs_tunic", "pyromaniacs_pants", "pyromaniacs_shoes"};
	private static final String[] IRON = {"iron_helmet", "iron_chestplate", "iron_leggings", "iron_boots"};
	private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
	/** The equipment keys in a summon command, and the item replace slots after "armor.", for each piece. */
	private static final String[] KEYS = {"head", "chest", "legs", "feet"};
	/** The bones GeckoLib poses to the wearer, and the slim sleeves the renderer hides. */
	private static final List<String> BONES = List.of("armorHead", "armorBody", "armorRightArm", "armorLeftArm", "armorRightLeg",
			"armorLeftLeg", "armorRightBoot", "armorLeftBoot", "armorRightArmSlim", "armorLeftArmSlim");
	/** The row, west to east (left to right from the front), and what each wears. */
	private static final String[] WEARERS = {"armor_stand", "armor_stand", "armor_stand", "armor_stand", "zombie", "armor_stand"};
	private static final String[][] WORN = {IRON, LIGHT, PIECES, PIECES, PIECES, PIECES};
	private static final int LIGHT_STAND = 1;
	private static final int SET = 2;
	private static final int ENCHANTED = 3;
	private static final int SMALL = 5;
	private static final double SPACING = 2.5;
	private static final double ROW_Z = -5.5;
	private static final double ROW_CAMERA = 6.0;
	private static final double PAIR_CAMERA = 2.6;
	private static final int[] TURNS = {0, -45, 90, 180};
	private static final String[] VIEWS = {"front", "three_quarter", "side", "back"};
	/** The icons, above (the foci, the bangle and the light set) and below (the medium set). */
	private static final String[] ICONS_ABOVE = {"lesser_fire_focus", "fire_focus", "fire_bangle", "pyromaniacs_hood", "pyromaniacs_tunic",
			"pyromaniacs_pants", "pyromaniacs_shoes"};

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

			// GeckoLib draws each piece, from the owner's model with every bone the renderer needs.
			String missing = context.computeOnClient(client -> unreadModel(client));
			Jugcraft.LOGGER.info("[ember regalia client] GeckoLib renderer and model: {}", missing.isEmpty() ? "present" : missing);
			check(missing.isEmpty(), "The fire sets cannot be drawn: " + missing);

			double rowZ = z + ROW_Z;
			for (int i = 0; i < WEARERS.length; i++) {
				summon(server, WEARERS[i], wearerX(x, i), y, rowZ, WORN[i], i == SMALL);
			}
			context.waitTicks(5);
			double enchantedX = wearerX(x, ENCHANTED);
			server.runOnServer(minecraft -> enchant(minecraft.overworld(),
					new AABB(enchantedX - 1, y - 1, rowZ - 1, enchantedX + 1, y + 3, rowZ + 1)));
			context.waitTicks(15);
			String wrong = server.computeOnServer(minecraft -> checkRow(minecraft.overworld(),
					new AABB(x - 8, y - 1, rowZ - 2, x + 9, y + 3, rowZ + 2)));
			Jugcraft.LOGGER.info("[ember regalia client] the row: {}", wrong.isEmpty() ? "dressed as the shots need" : wrong);
			check(wrong.isEmpty(), "The row is not dressed as the shots need: " + wrong);

			String box = "x=%d,y=%d,z=%d,dx=17,dy=4,dz=5".formatted(x - 8, y - 1, z - 8);
			double rowX = (wearerX(x, 0) + wearerX(x, WEARERS.length - 1)) / 2;
		double pairX = (wearerX(x, LIGHT_STAND) + wearerX(x, SET)) / 2;
			for (int view = 0; view < VIEWS.length; view++) {
				server.runCommand("execute as @e[type=!minecraft:player,%s] at @s run tp @s ~ ~ ~ %d 0".formatted(box, TURNS[view]));
				shoot(context, singleplayer, rowX, y, rowZ + ROW_CAMERA, 6, "jugcraft_ember_regalia_" + VIEWS[view]);
				shoot(context, singleplayer, pairX, y, rowZ + PAIR_CAMERA, 10, "jugcraft_ember_regalia_pair_" + VIEWS[view]);
			}
			server.runCommand("kill @e[type=!minecraft:player,%s]".formatted(box));
			server.runCommand("kill @e[type=minecraft:item]");

			// The player in the set: standing, then sneaking with the sneak key held, from the front and from behind.
			for (int p = 0; p < PIECES.length; p++) {
				server.runCommand("item replace entity @p armor.%s with jugcraft:%s".formatted(KEYS[p], PIECES[p]));
			}
			context.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
			shoot(context, singleplayer, x + 0.5, y, z + 3.5, 0, "jugcraft_ember_regalia_player");
			context.getInput().holdKey(options -> options.keyShift);
			context.waitTicks(15);
			boolean sneaking = context.computeOnClient(client -> client.player.isShiftKeyDown());
			context.takeScreenshot("jugcraft_ember_regalia_player_sneaking");
			context.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_BACK));
			context.waitTicks(5);
			context.takeScreenshot("jugcraft_ember_regalia_player_sneaking_back");
			context.getInput().releaseKey(options -> options.keyShift);
			context.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));
			check(sneaking, "The player was not sneaking for the sneaking shot");

			// The real Trinkets slot on the real, ticking player: the focus's Spell Power joins the set's.
			String slotted = server.computeOnServer(minecraft -> wear(minecraft, new ItemStack(EmberGear.FIRE_FOCUS)));
			check(slotted.isEmpty(), slotted);
			context.waitTicks(10);
			double withFocus = server.computeOnServer(minecraft -> fire(minecraft));
			Jugcraft.LOGGER.info("[ember regalia client] fire Spell Power with the set and a Focus of Fire: {}", withFocus);
			check(withFocus == 6.0, "The set and a Focus of Fire should give 6 fire Spell Power above the base, not " + withFocus);
			slotted = server.computeOnServer(minecraft -> wear(minecraft, new ItemStack(EmberGear.LESSER_FIRE_FOCUS)));
			check(slotted.isEmpty(), slotted);
			context.waitTicks(10);
			double withLesser = server.computeOnServer(minecraft -> fire(minecraft));
			check(withLesser == 4.0, "The set and a Lesser Focus of Fire should give 4, not " + withLesser);
			server.computeOnServer(minecraft -> wear(minecraft, ItemStack.EMPTY));

			// The eleven icons in frames on a wall: seven above, the medium set below.
			server.runCommand("fill %d %d %d %d %d %d minecraft:spruce_planks".formatted(x - 2, y, z - 12, x + 6, y + 3, z - 12));
			for (int i = 0; i < ICONS_ABOVE.length; i++) {
				frame(server, x - 1 + i, y + 2, z - 11, ICONS_ABOVE[i]);
			}
			for (int i = 0; i < PIECES.length; i++) {
				frame(server, x + 1 + i, y + 1, z - 11, PIECES[i]);
			}
			context.waitTicks(20);
			int icons = ICONS_ABOVE.length + PIECES.length;
			int frames = server.computeOnServer(minecraft -> minecraft.overworld().getEntitiesOfClass(Entity.class,
					new AABB(x - 2, y, z - 12, x + 7, y + 4, z - 10), entity -> type(entity).equals("item_frame")).size());
			check(frames == icons, frames + " item frames on the wall, not " + icons);
			shoot(context, singleplayer, x + 2.5, y, z - 6.5, -9, "jugcraft_ember_regalia_icons");
			setHudHidden(context, hudWasHidden);
		}
	}

	/**
	 * What GeckoLib lacks to draw the sets, or an empty string: a renderer for each piece, the same one for a set's four
	 * pieces and another for the other set, each wearing the owner's model with its set's own texture (named here, not
	 * asked of the item); the model and its bones; the textures.
	 */
	private static String unreadModel(Minecraft client) {
		List<String> wrong = new ArrayList<>();
		Identifier modelId = Jugcraft.id("armor/" + EmberGear.ARMOR_MODEL);
		Map<String, GeoArmorRenderer<?, ?>> bySet = new HashMap<>();
		for (Map.Entry<String, String[]> set : Map.of("pyromaniacs", LIGHT, "pyromancers", PIECES).entrySet()) {
			Identifier texture = Jugcraft.id("textures/armor/" + set.getKey() + ".png");
			String[] pieces = set.getValue();
			for (int p = 0; p < pieces.length; p++) {
				ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.getValue(Jugcraft.id(pieces[p])));
				GeoRenderProvider provider = GeoRenderProvider.of(stack);
				GeoArmorRenderer<?, ?> renderer = provider == null || provider == GeoRenderProvider.DEFAULT ? null
						: provider.getGeoArmorRenderer(stack, SLOTS[p]);
				if (renderer == null) {
					wrong.add(pieces[p] + " has no GeckoLib armour renderer");
					continue;
				}
				HumanoidRenderState state = new HumanoidRenderState();
				Identifier worn = renderer.getGeoModel().getModelResource(state);
				Identifier painted = renderer.getGeoModel().getTextureResource(state);
				if (!worn.equals(modelId) || !painted.equals(texture)) {
					wrong.add(pieces[p] + " is drawn as " + worn + " in " + painted + ", not " + modelId + " in " + texture);
				}
				GeoArmorRenderer<?, ?> first = bySet.putIfAbsent(set.getKey(), renderer);
				if (first != null && first != renderer) {
					wrong.add(pieces[p] + " has a renderer of its own, not its set's");
				}
			}
		}
		if (bySet.get("pyromaniacs") != null && bySet.get("pyromaniacs") == bySet.get("pyromancers")) {
			wrong.add("the two sets share one renderer, so one texture would be drawn on both");
		}
		for (String texture : List.of("pyromaniacs", "pyromancers")) {
			if (client.getResourceManager().getResource(Jugcraft.id("textures/armor/" + texture + ".png")).isEmpty()) {
				wrong.add("there is no textures/armor/" + texture + ".png");
			}
		}
		BakedGeoModel model = GeckoLibResources.getBakedModels().getModel(modelId);
		if (model == null || model.isMissingno()) {
			wrong.add("the owner's model " + modelId + " did not load");
		} else {
			for (String bone : BONES) {
				if (model.getBone(bone).isEmpty()) {
					wrong.add("the model has no bone " + bone);
				}
			}
		}
		return String.join("; ", wrong);
	}

	private static ServerPlayer player(MinecraftServer minecraft) {
		return minecraft.getPlayerList().getPlayers().getFirst();
	}

	/** Puts {@code focus} in the player's Spell Focus slot; an empty string, or what is wrong. */
	private static String wear(MinecraftServer minecraft, ItemStack focus) {
		var slot = TrinketsApi.getAttachment(player(minecraft)).getInventory("chest/spell_focus");
		if (slot == null) {
			return "The player has no Spell Focus slot";
		}
		slot.setItem(0, focus);
		return "";
	}

	private static double fire(MinecraftServer minecraft) {
		return Invocations.powerAboveBase(player(minecraft), Ember.SCHOOL);
	}

	private static double wearerX(int x, int i) {
		return x + 0.5 + SPACING * (i - WEARERS.length / 2);
	}

	/** Summons a {@code wearer} (an armour stand with arms, maybe small, or a zombie standing still) facing south, in {@code pieces}. */
	private static void summon(TestServerContext server, String wearer, double x, int y, double z, String[] pieces, boolean small) {
		StringBuilder equipment = new StringBuilder();
		for (int p = 0; p < pieces.length; p++) {
			String id = pieces[p].startsWith("iron_") ? "minecraft:" + pieces[p] : "jugcraft:" + pieces[p];
			equipment.append(p == 0 ? "" : ",").append("%s:{id:\"%s\",count:1}".formatted(KEYS[p], id));
		}
		String kind = wearer.equals("zombie") ? "NoAI:1b,PersistenceRequired:1b,Silent:1b"
				: "ShowArms:1b,NoBasePlate:1b" + (small ? ",Small:1b" : "");
		server.runCommand(String.format(Locale.ROOT, "summon minecraft:%s %.1f %d %.1f {%s,Rotation:[0f,0f],equipment:{%s}}", wearer, x, y,
				z, kind, equipment));
	}

	/** Swaps every piece the armour stands in {@code around} wear for the same piece with Protection IV. */
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

	/** What is wrong with the row, or an empty string: each wearer of its kind, in its pieces, glinting on the enchanted one alone. */
	private static String checkRow(ServerLevel level, AABB around) {
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
			for (int p = 0; p < PIECES.length; p++) {
				ItemStack stack = wearer.getItemBySlot(SLOTS[p]);
				String worn = WORN[i][p];
				Item expected = BuiltInRegistries.ITEM.getValue(worn.startsWith("iron_") ? Identifier.withDefaultNamespace(worn) : Jugcraft.id(worn));
				if (stack.getItem() != expected) {
					wrong.add("wearer " + (i + 1) + " wears " + BuiltInRegistries.ITEM.getKey(stack.getItem()) + " on its " + KEYS[p]);
				}
				if (stack.hasFoil() != (i == ENCHANTED)) {
					wrong.add("wearer " + (i + 1) + "'s " + KEYS[p] + (i == ENCHANTED ? " does not glint" : " glints"));
				}
			}
		}
		return String.join("; ", wrong);
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
