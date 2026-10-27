package io.github.jimbozoomer.jugcraft.test;

import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.cache.GeckoLibResources;
import com.geckolib.cache.model.BakedGeoModel;
import com.geckolib.renderer.GeoArmorRenderer;
import eu.pb4.trinkets.api.TrinketsApi;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import io.github.jimbozoomer.jugcraft.concordance.ember.EmberGear;
import io.github.jimbozoomer.jugcraft.concordance.rime.Frostward;
import io.github.jimbozoomer.jugcraft.concordance.rime.Rime;
import io.github.jimbozoomer.jugcraft.concordance.rime.RimeGear;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import io.github.jimbozoomer.jugcraft.concordance.strata.Brace;
import io.github.jimbozoomer.jugcraft.lair.yeti.YetiKingEntity;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
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
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.AABB;

/**
 * Concordance Chunk B, unit 4 in a real client (docs/features/arcane-concordance-winter.md):
 * <ul>
 * <li>First the client checks its resources: Frostward's icon, the owner's (B-08); and for the travel kit (B-10) a
 * GeckoLib renderer for each of the twelve pieces, one for each set, Nereid's and Aquamancer's wearing the owner's medium
 * model (Ember's) and Winterguard's its own, each in its set's own sheet; the heavy model loaded with every bone the
 * renderer poses and the slim sleeves it hides; and the three sheets.</li>
 * <li>{@code jugcraft_winter_frostward_hud}: the player under Frostward (Winter Brine), given by command.</li>
 * <li>A row by day, left to right from the front: vanilla iron on an armour stand (the control), Nereid's, Aquamancer's
 * and Winterguard's sets on stands (Nereid's and Aquamancer's sheets are the owner's same bytes, so they should look
 * alike), Winterguard's with Protection IV (its glint), Winterguard's on a zombie (arms raised: the sleeves must follow
 * them), and Aquamancer's on a small stand (which should show nothing). The server checks what each wears first. The row
 * turns to face the camera, then three-quarter, side and back; each view is shot whole, then the three sets close up.</li>
 * <li>The player in Winterguard's, standing and sneaking with the real sneak key, from the front and behind.</li>
 * <li>The Yeti King's frost on the real, ticking player, a hundred ticks of it: in Winterguard's it still lands and slows
 * them, but no freezing blow takes health; taken off, it does (natural regeneration held off meanwhile, then put
 * back).</li>
 * <li>The real Trinkets slots on the real player, Rimekeeping understood: a Focus of Water worn makes Preserve hold two
 * minutes, and the Water Bangle, its count set back thirty seconds, braces the player through the server's own tick
 * ({@code jugcraft_rime_kit_bangle_brace}, the research's toasts cleared first); then the survival inventory, where
 * Trinkets shows its slots ({@code jugcraft_rime_kit_slots}; since Chunk B unit 5, B-11).</li>
 * <li>The fifteen icons in frames on a wall.</li>
 * </ul>
 * The shots are for people to look at; nothing here judges a picture. The HUD is hidden for the scenes, shown for the
 * HUD shots, and put back as it was. CI job {@code client}.
 */
public class ConcordanceWinterClientGameTests implements FabricClientGameTest {
	private static final List<String> STATUSES = List.of("frostward");
	private static final List<String> SETS = List.of("nereids", "aquamancers", "winterguards");
	private static final String[] NEREIDS = {"nereids_hood", "nereids_tunic", "nereids_pants", "nereids_shoes"};
	private static final String[] AQUAMANCERS = {"aquamancers_hat", "aquamancers_robes", "aquamancers_leggings", "aquamancers_boots"};
	private static final String[] WINTERGUARDS = {"winterguards_helmet", "winterguards_chestplate", "winterguards_leggings",
			"winterguards_boots"};
	private static final String[] IRON = {"iron_helmet", "iron_chestplate", "iron_leggings", "iron_boots"};
	private static final Map<String, String[]> SET_PIECES = Map.of("nereids", NEREIDS, "aquamancers", AQUAMANCERS,
			"winterguards", WINTERGUARDS);
	/** The GeckoLib model each set is worn as. */
	private static final Map<String, String> MODELS = Map.of("nereids", EmberGear.ARMOR_MODEL, "aquamancers", EmberGear.ARMOR_MODEL,
			"winterguards", RimeGear.HEAVY_MODEL);
	private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
	/** The equipment keys in a summon command, and the item replace slots after "armor.", for each piece. */
	private static final String[] KEYS = {"head", "chest", "legs", "feet"};
	/** The bones GeckoLib poses to the wearer, and the slim sleeves the renderer hides. */
	private static final List<String> BONES = List.of("armorHead", "armorBody", "armorRightArm", "armorLeftArm", "armorRightLeg",
			"armorLeftLeg", "armorRightBoot", "armorLeftBoot", "armorRightArmSlim", "armorLeftArmSlim");
	/** The row, west to east (left to right from the front), and what each wears. */
	private static final String[] WEARERS = {"armor_stand", "armor_stand", "armor_stand", "armor_stand", "armor_stand", "zombie",
			"armor_stand"};
	private static final String[][] WORN = {IRON, NEREIDS, AQUAMANCERS, WINTERGUARDS, WINTERGUARDS, WINTERGUARDS, AQUAMANCERS};
	/** The three sets' own stands, shot close up (the middle one centred). */
	private static final int MIDDLE = 2;
	private static final int ENCHANTED = 4;
	private static final int SMALL = 6;
	private static final double SPACING = 2.5;
	private static final double ROW_Z = -5.5;
	private static final double ROW_CAMERA = 7.0;
	private static final double CLOSE_CAMERA = 3.6;
	private static final int[] TURNS = {0, -45, 90, 180};
	private static final String[] VIEWS = {"front", "three_quarter", "side", "back"};
	/** Ticks of the Yeti King's frost on the real player, each tick another breath. */
	private static final int FROST_TICKS = 100;
	/** The icons, above (the foci, the bangle and Nereid's set) and below (Aquamancer's and Winterguard's). */
	private static final String[] ICONS_ABOVE = {"lesser_water_focus", "water_focus", "water_bangle", "nereids_hood", "nereids_tunic",
			"nereids_pants", "nereids_shoes"};
	private static final String[] ICONS_BELOW = {"aquamancers_hat", "aquamancers_robes", "aquamancers_leggings", "aquamancers_boots",
			"winterguards_helmet", "winterguards_chestplate", "winterguards_leggings", "winterguards_boots"};

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
			server.runCommand("gamerule minecraft:send_command_feedback false");
			server.runCommand("gamerule minecraft:spawn_mobs false");
			server.runCommand("gamerule minecraft:advance_time false");
			server.runCommand("time set noon");
			server.runCommand("weather clear");
			server.runCommand("fill %d %d %d %d %d %d minecraft:grass_block".formatted(x - 12, y - 1, z - 16, x + 12, y - 1, z + 8));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 12, y, z - 16, x + 12, y + 8, z + 8));
			context.waitTicks(10);
			boolean hudWasHidden = context.computeOnClient(client -> client.gui.hud.isHidden());

			// The client's resources: Frostward's icon, and GeckoLib's renderers, model and sheets for the kit.
			String missing = context.computeOnClient(ConcordanceWinterClientGameTests::missing);
			Jugcraft.LOGGER.info("[winter client] the owner's icons: {}", missing.isEmpty() ? "present" : missing);
			check(missing.isEmpty(), "The client lacks the winter icons: " + missing);
			String unread = context.computeOnClient(ConcordanceWinterClientGameTests::unreadModel);
			Jugcraft.LOGGER.info("[winter client] GeckoLib renderers, models and sheets: {}", unread.isEmpty() ? "present" : unread);
			check(unread.isEmpty(), "The water sets cannot be drawn: " + unread);

			// Frostward on the HUD, as a draught of Winter Brine leaves it (given by command: presentation, not evidence).
			server.runCommand("gamemode survival @p");
			server.runCommand(String.format(Locale.ROOT, "tp @p %.1f %d %.1f 180 10", x + 0.5, y, z + 0.5));
			setHudHidden(context, false);
			server.runCommand("effect give @p jugcraft:frostward 60");
			context.waitTicks(20);
			boolean shown = context.computeOnClient(client -> client.player.hasEffect(Frostward.FROSTWARD));
			check(shown, "The client was not told of Frostward");
			context.takeScreenshot("jugcraft_winter_frostward_hud");
			server.runCommand("effect clear @p");
			server.runCommand("gamemode creative @p");
			setHudHidden(context, true);

			// The row.
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
					new AABB(x - 10, y - 1, rowZ - 2, x + 11, y + 3, rowZ + 2)));
			Jugcraft.LOGGER.info("[winter client] the row: {}", wrong.isEmpty() ? "dressed as the shots need" : wrong);
			check(wrong.isEmpty(), "The row is not dressed as the shots need: " + wrong);

			String box = "x=%d,y=%d,z=%d,dx=21,dy=4,dz=5".formatted(x - 10, y - 1, z - 8);
			double rowX = (wearerX(x, 0) + wearerX(x, WEARERS.length - 1)) / 2;
			double closeX = wearerX(x, MIDDLE);
			for (int view = 0; view < VIEWS.length; view++) {
				server.runCommand("execute as @e[type=!minecraft:player,%s] at @s run tp @s ~ ~ ~ %d 0".formatted(box, TURNS[view]));
				shoot(context, singleplayer, rowX, y, rowZ + ROW_CAMERA, 6, "jugcraft_rime_kit_" + VIEWS[view]);
				shoot(context, singleplayer, closeX, y, rowZ + CLOSE_CAMERA, 10, "jugcraft_rime_kit_close_" + VIEWS[view]);
			}
			server.runCommand("kill @e[type=!minecraft:player,%s]".formatted(box));
			server.runCommand("kill @e[type=minecraft:item]");

			// The player in Winterguard's: standing, then sneaking with the sneak key held, from the front and from behind.
			for (int p = 0; p < WINTERGUARDS.length; p++) {
				server.runCommand("item replace entity @p armor.%s with jugcraft:%s".formatted(KEYS[p], WINTERGUARDS[p]));
			}
			context.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
			shoot(context, singleplayer, x + 0.5, y, z + 3.5, 0, "jugcraft_rime_kit_player");
			context.getInput().holdKey(options -> options.keyShift);
			context.waitTicks(15);
			boolean sneaking = context.computeOnClient(client -> client.player.isShiftKeyDown());
			context.takeScreenshot("jugcraft_rime_kit_player_sneaking");
			context.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_BACK));
			context.waitTicks(5);
			context.takeScreenshot("jugcraft_rime_kit_player_sneaking_back");
			context.getInput().releaseKey(options -> options.keyShift);
			context.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));
			check(sneaking, "The player was not sneaking for the sneaking shot");

			// The Yeti King's frost on the real, ticking player: in Winterguard's it lands and slows, but takes no health.
			server.runCommand("gamemode survival @p");
			server.runCommand("gamerule minecraft:natural_health_regeneration false");
			server.runCommand("effect clear @p");
			context.waitTicks(5);
			setHudHidden(context, false);
			float before = server.computeOnServer(minecraft -> player(minecraft).getHealth());
			double slowest = frost(context, server);
			float dressed = server.computeOnServer(minecraft -> player(minecraft).getHealth());
			Jugcraft.LOGGER.info("[winter client] his frost in Winterguard's: health {} -> {}, slowest {}", before, dressed, slowest);
			context.takeScreenshot("jugcraft_rime_kit_frost_hud");
			check(dressed == before, "In the kit his frost took health: " + before + " -> " + dressed);
			check(slowest <= -0.049, "In the kit his frost should still slow the player (powder_snow modifier), not " + slowest);
			for (String key : KEYS) {
				server.runCommand("item replace entity @p armor.%s with minecraft:air".formatted(key));
			}
			float max = server.computeOnServer(minecraft -> player(minecraft).getMaxHealth());
			float start = server.computeOnServer(minecraft -> player(minecraft).getHealth());
			frost(context, server);
			float bare = server.computeOnServer(minecraft -> player(minecraft).getHealth());
			Jugcraft.LOGGER.info("[winter client] his frost bare: health {} -> {}", start, bare);
			check(bare <= max - 1.0F, "Bare, frozen through for " + FROST_TICKS + " ticks, the player should take freezing blows: " + bare);
			server.runOnServer(minecraft -> player(minecraft).setTicksFrozen(0));
			server.runCommand("effect give @p minecraft:instant_health 1 5");
			server.runCommand("gamerule minecraft:natural_health_regeneration true");
			server.runCommand("gamemode creative @p");
			context.waitTicks(5);

			// The real Trinkets slots on the real player: a Focus of Water lengthens Preserve, and the bangle braces through
			// the server's own tick.
			// Rimekeeping understood, one research a tick: each learning sends Modonomicon's book state as a live map that a
			// second learning in the same tick can change while it is still being encoded (CI run 38679932446 dropped a
			// client so).
			for (String research : List.of("jugcraft:first_light", Rime.RESEARCH)) {
				server.runOnServer(minecraft -> ConcordanceProgress.grant(player(minecraft), research, ResearchState.UNDERSTOOD));
				context.waitTicks(2);
			}
			String slotted = server.computeOnServer(ConcordanceWinterClientGameTests::wearKit);
			check(slotted.isEmpty(), slotted);
			context.waitTicks(5);
			int hold = server.computeOnServer(minecraft -> RimeGear.holdTicks(player(minecraft)));
			check(hold == RimeGear.FOCUS_HOLD_TICKS, "A Focus of Water worn should make Preserve hold " + RimeGear.FOCUS_HOLD_TICKS + ", not " + hold);
			server.runOnServer(minecraft -> player(minecraft).setAttached(RimeGear.BANGLE_LAST,
					(long) minecraft.getTickCount() - RimeGear.BANGLE_RECHARGE_TICKS - 20));
			context.waitTicks(25);
			boolean braced = server.computeOnServer(minecraft -> player(minecraft).hasEffect(Brace.BRACED));
			Jugcraft.LOGGER.info("[winter client] the Water Bangle braced the player: {}", braced);
			check(braced, "The Water Bangle did not brace a Rimekeeper who had gone thirty seconds without a Brace");
			// The research granted raises advancement toasts over the top right, where the HUD draws Braced's icon: cleared
			// first, as ConcordanceRimeStrataClientGameTests does. The purple pixel shapes that may stand mid-view are the
			// shared effect boundary's mark for the Brace that just landed (ConcordanceEffects.apply shows Sign.CHARM, which
			// SignClient draws as vanilla witch particles at 60% of the wearer's height): seen from inside the wearer's own
			// body in first person, they rise through the camera for up to two seconds. Expected, not a missing texture.
			clearToasts(context);
			context.takeScreenshot("jugcraft_rime_kit_bangle_brace");
			// The Focus of Water and the Water Bangle in their slots: the survival inventory, where Trinkets shows its slot
			// groups (for a creative player vanilla opens the creative screen instead).
			server.runCommand("gamemode survival @p");
			context.waitFor(client -> !client.player.hasInfiniteMaterials(), 100);
			context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
			context.waitTicks(10);
			boolean inventoryShown = context.computeOnClient(client -> client.gui.screen() instanceof InventoryScreen);
			context.takeScreenshot("jugcraft_rime_kit_slots");
			context.setScreen(() -> null);
			server.runCommand("gamemode creative @p");
			check(inventoryShown, "The slots shot shows another screen than the survival inventory");
			server.computeOnServer(ConcordanceWinterClientGameTests::takeOffKit);
			server.runCommand("effect clear @p");
			setHudHidden(context, true);

			// The fifteen icons in frames on a wall: seven above, eight below.
			server.runCommand("fill %d %d %d %d %d %d minecraft:spruce_planks".formatted(x - 2, y, z - 12, x + 7, y + 3, z - 12));
			for (int i = 0; i < ICONS_ABOVE.length; i++) {
				frame(server, x - 1 + i, y + 2, z - 11, ICONS_ABOVE[i]);
			}
			for (int i = 0; i < ICONS_BELOW.length; i++) {
				frame(server, x - 1 + i, y + 1, z - 11, ICONS_BELOW[i]);
			}
			context.waitTicks(20);
			int icons = ICONS_ABOVE.length + ICONS_BELOW.length;
			int frames = server.computeOnServer(minecraft -> minecraft.overworld().getEntitiesOfClass(Entity.class,
					new AABB(x - 2, y, z - 12, x + 8, y + 4, z - 10), entity -> type(entity).equals("item_frame")).size());
			check(frames == icons, frames + " item frames on the wall, not " + icons);
			shoot(context, singleplayer, x + 3.0, y, z - 6.5, -9, "jugcraft_rime_kit_icons");
			setHudHidden(context, hudWasHidden);
		}
	}

	/** The status icons the client cannot find, or an empty string. */
	private static String missing(Minecraft client) {
		List<String> missing = new ArrayList<>();
		for (String status : STATUSES) {
			if (client.getResourceManager().getResource(Jugcraft.id("textures/mob_effect/" + status + ".png")).isEmpty()) {
				missing.add("textures/mob_effect/" + status + ".png");
			}
		}
		return String.join(", ", missing);
	}

	/**
	 * What GeckoLib lacks to draw the three water sets, or an empty string: a renderer for each piece, the same one for a
	 * set's four pieces and three in all, each wearing its set's model with its set's own sheet (named here, not asked of
	 * the item); the heavy model and its bones; the sheets.
	 */
	private static String unreadModel(Minecraft client) {
		List<String> wrong = new ArrayList<>();
		Map<String, GeoArmorRenderer<?, ?>> bySet = new HashMap<>();
		for (String set : SETS) {
			Identifier modelId = Jugcraft.id("armor/" + MODELS.get(set));
			Identifier texture = Jugcraft.id("textures/armor/" + set + ".png");
			String[] pieces = SET_PIECES.get(set);
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
				GeoArmorRenderer<?, ?> first = bySet.putIfAbsent(set, renderer);
				if (first != null && first != renderer) {
					wrong.add(pieces[p] + " has a renderer of its own, not its set's");
				}
			}
		}
		Set<GeoArmorRenderer<?, ?>> distinct = Collections.newSetFromMap(new IdentityHashMap<>());
		distinct.addAll(bySet.values());
		if (distinct.size() != SETS.size()) {
			wrong.add("the three sets share renderers, so one sheet would be drawn on another set: " + distinct.size());
		}
		for (String set : SETS) {
			if (client.getResourceManager().getResource(Jugcraft.id("textures/armor/" + set + ".png")).isEmpty()) {
				wrong.add("there is no textures/armor/" + set + ".png");
			}
		}
		Identifier heavy = Jugcraft.id("armor/" + RimeGear.HEAVY_MODEL);
		BakedGeoModel model = GeckoLibResources.getBakedModels().getModel(heavy);
		if (model == null || model.isMissingno()) {
			wrong.add("the owner's model " + heavy + " did not load");
		} else {
			for (String bone : BONES) {
				if (model.getBone(bone).isEmpty()) {
					wrong.add("the heavy model has no bone " + bone);
				}
			}
		}
		return String.join("; ", wrong);
	}

	private static ServerPlayer player(MinecraftServer minecraft) {
		return minecraft.getPlayerList().getPlayers().getFirst();
	}

	/**
	 * {@value #FROST_TICKS} ticks of the Yeti King's breath on the real player, one a tick, stacking to his cap: returns the
	 * most the frost slowed them (the lowest amount of vanilla's powder_snow speed modifier seen).
	 */
	private static double frost(ClientGameTestContext context, TestServerContext server) {
		double slowest = 0.0;
		for (int i = 0; i < FROST_TICKS; i++) {
			server.runOnServer(minecraft -> YetiKingEntity.chill(player(minecraft), 2 * YetiKingEntity.BLIZZARD_FROST, true));
			context.waitTicks(1);
			double slow = server.computeOnServer(minecraft -> {
				AttributeModifier modifier = player(minecraft).getAttribute(Attributes.MOVEMENT_SPEED)
						.getModifier(Identifier.withDefaultNamespace("powder_snow"));
				return modifier == null ? 0.0 : modifier.amount();
			});
			slowest = Math.min(slowest, slow);
		}
		return slowest;
	}

	/** A Focus of Water in the Spell Focus slot and the Water Bangle in a Bracelet slot (Rimekeeping understood first); an empty string, or what is wrong. */
	private static String wearKit(MinecraftServer minecraft) {
		ServerPlayer player = player(minecraft);
		var focus = TrinketsApi.getAttachment(player).getInventory("chest/spell_focus");
		var bracelets = TrinketsApi.getAttachment(player).getInventory("hand/bracelet");
		if (focus == null || bracelets == null) {
			return "The player has no Spell Focus or Bracelet slot";
		}
		focus.setItem(0, new ItemStack(RimeGear.WATER_FOCUS));
		bracelets.setItem(0, new ItemStack(RimeGear.WATER_BANGLE));
		return RimeGear.wearsBangle(player) ? "" : "The Water Bangle in the Bracelet slot is not worn";
	}

	private static String takeOffKit(MinecraftServer minecraft) {
		ServerPlayer player = player(minecraft);
		TrinketsApi.getAttachment(player).getInventory("chest/spell_focus").setItem(0, ItemStack.EMPTY);
		TrinketsApi.getAttachment(player).getInventory("hand/bracelet").setItem(0, ItemStack.EMPTY);
		return "";
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
			for (int p = 0; p < SLOTS.length; p++) {
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

	/** Removes the shown and queued toasts (the research's advancements), then waits for a frame drawn without them. */
	private static void clearToasts(ClientGameTestContext context) {
		context.runOnClient(client -> client.gui.toastManager().clear());
		context.waitTicks(5);
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
