package io.github.jimbozoomer.jugcraft.test;

import eu.pb4.trinkets.api.TrinketSlotAccess;
import eu.pb4.trinkets.api.TrinketsApi;
import eu.pb4.trinkets.api.client.TrinketRendererRegistry;
import eu.pb4.trinkets.api.client.renderer.AttachmentSettings;
import eu.pb4.trinkets.api.client.renderer.TrinketRenderContext;
import eu.pb4.trinkets.api.client.renderer.element.TrinketRenderElement;
import eu.pb4.trinkets.impl.client.render.ClientTrinketsManager;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.client.ConcordanceClientOptions;
import io.github.jimbozoomer.jugcraft.client.trinket.UnlessCoveredTrinketElement;
import io.github.jimbozoomer.jugcraft.concordance.trinket.Wayfaring;
import io.github.jimbozoomer.jugcraft.concordance.trinket.WornDisplay;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

/**
 * Client game test for Wayfaring, the trinkets slice's part 1 (docs/features/arcane-concordance-trinkets.md), on the real,
 * ticking player, whose equipment Trinkets scans every tick (the server tests' mock players are never scanned):
 * <ul>
 * <li>a worn Leather Belt gives a second Charm slot, on the server and, synced, on the client; a charm goes in it;</li>
 * <li>the worn feather, boot and Ice Breaker give their attributes under their own names, and once;</li>
 * <li>part 1b: Trinkets has what it needs to draw the Leather Belt and the Amphibian Boot on the body (no Java renderer in
 * the way; each render definition loaded and, run as Trinkets runs it, drawing the belt's model on the body and the boot's
 * on each leg; the worn models among the client's resources; the owner's worn sheets in the items atlas; nothing for the
 * Ice Breaker, which has no worn sheet). Then, as the owner asked, the belt is hidden under an iron chestplate and under
 * iron leggings, the boots under iron boots, neither under an elytra, and both while the player's Show my worn trinkets
 * setting is off: turned as the settings screen turns it, the choice goes to the server, which keeps it on the player and
 * sends it back to this client as it sends it to everyone who sees them, and the drawing follows what came back.
 * Whether they look right only the shots show: the player wearing them from the front and from behind, whole, closer and
 * from each quarter, under a chestplate, under boots, with the setting off, and sneaking, for people to look at;</li>
 * <li>with the second charm taken out first, the belt comes off, the slot goes, and nothing falls to the ground;</li>
 * <li>the eight icons in frames on a wall (three of them the owner's animated strips), and the survival inventory, where
 * Trinkets shows its slots (the player is put in survival for it: a creative player is shown the creative screen), for
 * people to look at.</li>
 * </ul>
 * The HUD is hidden whatever state an earlier test left it in, and put back at the end.
 */
public class ConcordanceWayfaringClientGameTests implements FabricClientGameTest {
	private static final String[] ICONS = {"leather_belt", "angelic_feather", "kraken_shell", "infernal_claws", "angelheart_vial",
			"phoenix_down", "amphibian_boot", "ice_breaker"};
	/** What the worn items should give the player, each once: attribute -> modifier id -> amount. */
	private static final Map<String, Map<String, Double>> WORN = Map.of(
			"minecraft:jump_strength", Map.of("jugcraft:wayfaring/angelic_feather/jump_strength", Wayfaring.FEATHER_JUMP),
			"minecraft:water_movement_efficiency", Map.of("jugcraft:wayfaring/amphibian_boot/water_movement_efficiency", Wayfaring.AMPHIBIAN_SWIM),
			"minecraft:oxygen_bonus", Map.of("jugcraft:wayfaring/amphibian_boot/oxygen_bonus", Wayfaring.AMPHIBIAN_OXYGEN),
			"minecraft:knockback_resistance", Map.of("jugcraft:wayfaring/ice_breaker/knockback_resistance", Wayfaring.ICE_BREAKER_KNOCKBACK));

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
			server.runCommand("fill %d %d %d %d %d %d minecraft:grass_block".formatted(x - 6, y - 1, z - 14, x + 8, y - 1, z + 4));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 6, y, z - 14, x + 8, y + 6, z + 4));
			context.waitTicks(10);
			boolean hudWasHidden = context.computeOnClient(client -> client.gui.hud.isHidden());
			setHudHidden(context, true);

			// The belt, a feather in the first Charm slot, the boot and the Ice Breaker on the feet: Trinkets' own scan applies them.
			server.runOnServer(minecraft -> {
				var attachment = TrinketsApi.getAttachment(player(minecraft));
				attachment.getInventory(Wayfaring.BELT_SLOT).setItem(0, new ItemStack(Wayfaring.LEATHER_BELT));
				attachment.getInventory(Wayfaring.CHARM_SLOT).setItem(0, new ItemStack(Wayfaring.ANGELIC_FEATHER));
				attachment.getInventory(Wayfaring.FEET_SLOT).setItem(0, new ItemStack(Wayfaring.AMPHIBIAN_BOOT));
				attachment.getInventory(Wayfaring.FEET_SLOT).setItem(1, new ItemStack(Wayfaring.ICE_BREAKER));
			});
			context.waitTicks(10);
			int serverCharms = server.computeOnServer(minecraft -> charms(minecraft));
			context.waitTicks(10);
			int clientCharms = context.computeOnClient(client -> {
				var charms = TrinketsApi.getAttachment(client.player).getInventory(Wayfaring.CHARM_SLOT);
				return charms == null ? -1 : charms.getContainerSize();
			});
			Jugcraft.LOGGER.info("[wayfaring client] Charm slots with the belt: {} on the server, {} on the client", serverCharms, clientCharms);
			check(serverCharms == 2 && clientCharms == 2, "A worn Leather Belt should give two Charm slots, not " + serverCharms
					+ " on the server and " + clientCharms + " on the client");
			// A Phoenix Down in the second slot: a feather as well, so the jump is still given once.
			server.runOnServer(minecraft -> TrinketsApi.getAttachment(player(minecraft)).getInventory(Wayfaring.CHARM_SLOT)
					.setItem(1, new ItemStack(Wayfaring.PHOENIX_DOWN)));
			context.waitTicks(10);
			String given = server.computeOnServer(minecraft -> given(minecraft));
			Jugcraft.LOGGER.info("[wayfaring client] attributes worn: {}", given.isEmpty() ? "as designed" : given);
			check(given.isEmpty(), "The worn items' attributes are not as designed: " + given);

			// Part 1b: the belt and boot drawn on the player, who wears both (and the Ice Breaker, which is not drawn), unless
			// armour covers them or the player has hidden them. The setting is on whatever this client had, and put back after.
			boolean shownBefore = context.computeOnClient(client -> ConcordanceClientOptions.wornTrinkets());
			String unchosen = choose(context, server, true);
			check(unchosen.isEmpty(), "Show my worn trinkets: " + unchosen);
			try {
				String undrawn = context.computeOnClient(client -> undrawn(client));
				Jugcraft.LOGGER.info("[wayfaring client] worn models: {}", undrawn.isEmpty()
						? "the belt drawn on the body and the boot on each leg, models present, sheets stitched" : undrawn);
				check(undrawn.isEmpty(), "Trinkets cannot draw the belt and boot on the body: " + undrawn);
				String misHidden = misHidden(context, server);
				Jugcraft.LOGGER.info("[wayfaring client] hidden under armour and by the setting: {}",
						misHidden.isEmpty() ? "as designed" : misHidden);
				check(misHidden.isEmpty(), "The belt and boots are not hidden as designed: " + misHidden);
				wornShots(context, singleplayer, x, y, z);
			} finally {
				choose(context, server, shownBefore);
			}

			// Off in order: the second charm, then the belt. The slot goes and nothing is dropped.
			server.runOnServer(minecraft -> TrinketsApi.getAttachment(player(minecraft)).getInventory(Wayfaring.CHARM_SLOT)
					.setItem(1, ItemStack.EMPTY));
			context.waitTicks(5);
			server.runOnServer(minecraft -> TrinketsApi.getAttachment(player(minecraft)).getInventory(Wayfaring.BELT_SLOT)
					.setItem(0, ItemStack.EMPTY));
			context.waitTicks(10);
			int after = server.computeOnServer(minecraft -> charms(minecraft));
			int dropped = server.computeOnServer(minecraft -> minecraft.overworld().getEntitiesOfClass(ItemEntity.class,
					new AABB(x - 6, y - 2, z - 6, x + 6, y + 4, z + 6)).size());
			check(after == 1 && dropped == 0, "Without the belt there should be one Charm slot and nothing dropped, not " + after
					+ " slots and " + dropped + " dropped");

			// The survival inventory, where Trinkets shows its slots: for a creative player vanilla opens the creative screen.
			server.runCommand("gamemode survival @p");
			context.waitFor(client -> !client.player.hasInfiniteMaterials(), 100);
			context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
			context.waitTicks(10);
			boolean inventoryShown = context.computeOnClient(client -> client.gui.screen() instanceof InventoryScreen);
			context.takeScreenshot("jugcraft_wayfaring_inventory");
			context.setScreen(() -> null);
			server.runCommand("gamemode creative @p");
			check(inventoryShown, "The inventory shot shows another screen than the survival inventory");
			server.runOnServer(minecraft -> {
				var attachment = TrinketsApi.getAttachment(player(minecraft));
				attachment.getInventory(Wayfaring.CHARM_SLOT).setItem(0, ItemStack.EMPTY);
				attachment.getInventory(Wayfaring.FEET_SLOT).setItem(0, ItemStack.EMPTY);
				attachment.getInventory(Wayfaring.FEET_SLOT).setItem(1, ItemStack.EMPTY);
			});

			// The eight icons in frames on a wall.
			server.runCommand("fill %d %d %d %d %d %d minecraft:spruce_planks".formatted(x - 2, y, z - 12, x + 7, y + 3, z - 12));
			for (int i = 0; i < ICONS.length; i++) {
				frame(server, x - 1 + i, y + 1, z - 11, ICONS[i]);
			}
			context.waitTicks(20);
			int frames = server.computeOnServer(minecraft -> minecraft.overworld().getEntitiesOfClass(Entity.class,
					new AABB(x - 2, y, z - 12, x + 8, y + 4, z - 10), entity -> BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType())
							.getPath().equals("item_frame")).size());
			check(frames == ICONS.length, frames + " item frames on the wall, not " + ICONS.length);
			shoot(context, singleplayer, x + 3.0, y, z - 7.5, 0, "jugcraft_wayfaring_icons");
			setHudHidden(context, hudWasHidden);
		}
	}

	private static ServerPlayer player(MinecraftServer minecraft) {
		return minecraft.getPlayerList().getPlayers().getFirst();
	}

	private static int charms(MinecraftServer minecraft) {
		var charms = TrinketsApi.getAttachment(player(minecraft)).getInventory(Wayfaring.CHARM_SLOT);
		return charms == null ? -1 : charms.getContainerSize();
	}

	/** What differs from WORN in the player's attributes (each expected modifier present once, with its amount), or "". */
	private static String given(MinecraftServer minecraft) {
		ServerPlayer player = player(minecraft);
		List<String> wrong = new ArrayList<>();
		WORN.forEach((attribute, modifiers) -> {
			var holder = BuiltInRegistries.ATTRIBUTE.get(Identifier.parse(attribute));
			AttributeInstance instance = holder.isPresent() ? player.getAttribute(holder.get()) : null;
			if (instance == null) {
				wrong.add(attribute + " missing");
				return;
			}
			long ours = instance.getModifiers().stream().filter(modifier -> modifier.id().getPath().startsWith("wayfaring/")).count();
			if (ours != modifiers.size()) {
				wrong.add(attribute + " has " + ours + " Wayfaring modifiers");
			}
			modifiers.forEach((id, amount) -> {
				var modifier = instance.getModifier(Identifier.parse(id));
				if (modifier == null || modifier.amount() != amount) {
					wrong.add(attribute + " " + id + " is " + (modifier == null ? "absent" : modifier.amount()));
				}
			});
		});
		return String.join("; ", wrong);
	}

	/** A worn item drawn on the body: its Trinkets slot, the model parts it is drawn on and its worn models. */
	private record Worn(Item item, String slot, List<String> parts, List<String> models) {
	}

	private static final List<Worn> DRAWN = List.of(
			new Worn(Wayfaring.LEATHER_BELT, Wayfaring.BELT_SLOT, List.of("body"), List.of("leather_belt_worn")),
			new Worn(Wayfaring.AMPHIBIAN_BOOT, Wayfaring.FEET_SLOT, List.of("right_leg", "left_leg"),
					List.of("amphibian_boot_worn_right", "amphibian_boot_worn_left")));

	/**
	 * What keeps Trinkets from drawing the belt and boot on the body, or "": a Java renderer for either (Trinkets would use
	 * it instead of the data); a render definition not loaded or not matched to its item, or that, run as Trinkets runs it
	 * (drawnParts), draws on other parts than the belt's body and the boot's two legs, or nothing (an element baked from a
	 * model with no quads draws nothing); a worn model missing from the client's resources (the game would bake its
	 * missing-model cube instead, which this cannot tell apart: only the shots show the models are the owner's); a worn
	 * sheet missing from the items atlas; or a definition for the Ice Breaker, which has no worn sheet.
	 */
	private static String undrawn(Minecraft client) {
		List<String> wrong = new ArrayList<>();
		var atlas = client.getAtlasManager().getAtlasOrThrow(AtlasIds.ITEMS);
		for (Worn worn : DRAWN) {
			String id = BuiltInRegistries.ITEM.getKey(worn.item()).getPath();
			if (TrinketRendererRegistry.hasRenderer(worn.item())) {
				wrong.add(id + " has a Java renderer, which Trinkets uses instead of its render definition");
			}
			List<String> parts = drawnParts(client, worn.slot());
			if (!parts.equals(worn.parts())) {
				wrong.add(id + " is drawn on " + parts + ", not " + worn.parts());
			}
			for (String name : worn.models()) {
				if (client.getResourceManager().getResource(Jugcraft.id("models/item/" + name + ".json")).isEmpty()) {
					wrong.add("the worn model " + name + " is missing from the client's resources");
				}
			}
			Identifier sheet = Jugcraft.id("item/" + id + "_worn");
			if (atlas.getSprite(sheet) == atlas.missingSprite()) {
				wrong.add(sheet + " is not in the items atlas");
			}
		}
		if (!ClientTrinketsManager.INSTANCE.getResolved(new ItemStack(Wayfaring.ICE_BREAKER)).isEmpty()) {
			wrong.add("the Ice Breaker has a render definition, but no worn sheet to draw");
		}
		return String.join("; ", wrong);
	}

	/**
	 * What differs from the hiding the owner asked for, or "": what the belt and the boot would draw on the player (drawnParts)
	 * bare, under an iron chestplate, iron leggings, iron boots and an elytra one at a time, with the Show my worn trinkets
	 * setting off, and with it on again. The belt is hidden under the chestplate and the leggings, the boots under the
	 * boots, neither under the elytra (drawn as wings, off the waist), and both with the setting off, once the server has
	 * the choice and its answer has come back (choose). The element's own armour test (UnlessCoveredTrinketElement.covered)
	 * is asked too, so a change to it picks this class in CI's client test choice (tools/select_client_tests.py).
	 */
	private static String misHidden(ClientGameTestContext context, TestServerContext server) {
		List<String> wrong = new ArrayList<>();
		List<String> belt = DRAWN.get(0).parts();
		List<String> boots = DRAWN.get(1).parts();
		hiding(context, server, "bare", null, true, belt, boots, wrong);
		hiding(context, server, "under an iron chestplate", "armor.chest with minecraft:iron_chestplate", true, List.of(), boots, wrong);
		hiding(context, server, "under iron leggings", "armor.legs with minecraft:iron_leggings", true, List.of(), boots, wrong);
		hiding(context, server, "under iron boots", "armor.feet with minecraft:iron_boots", true, belt, List.of(), wrong);
		hiding(context, server, "under an elytra", "armor.chest with minecraft:elytra", true, belt, boots, wrong);
		hiding(context, server, "with the setting off", null, false, List.of(), List.of(), wrong);
		hiding(context, server, "with the setting on again", null, true, belt, boots, wrong);
		return String.join("; ", wrong);
	}

	/** Puts {@code armour} on (if any) and the setting as {@code shown}, compares what is drawn, then takes the armour off. */
	private static void hiding(ClientGameTestContext context, TestServerContext server, String state, String armour, boolean shown,
			List<String> belt, List<String> boots, List<String> wrong) {
		if (armour != null) {
			server.runCommand("item replace entity @p " + armour);
		}
		String unchosen = choose(context, server, shown);
		if (!unchosen.isEmpty()) {
			wrong.add(state + ": " + unchosen);
		}
		context.waitTicks(5);
		// The armour this client sees, through the element's own test: covered exactly where armour should hide the item.
		boolean beltCovered = context.computeOnClient(client -> UnlessCoveredTrinketElement.covered(client.player,
				List.of(EquipmentSlot.CHEST, EquipmentSlot.LEGS)));
		boolean bootsCovered = context.computeOnClient(client -> UnlessCoveredTrinketElement.covered(client.player,
				List.of(EquipmentSlot.FEET)));
		if (beltCovered != (shown && belt.isEmpty()) || bootsCovered != (shown && boots.isEmpty())) {
			wrong.add(state + ": this client sees the belt's armour slots " + (beltCovered ? "covered" : "bare") + " and the boots' "
					+ (bootsCovered ? "covered" : "bare"));
		}
		List<String> drawnBelt = context.computeOnClient(client -> drawnParts(client, Wayfaring.BELT_SLOT));
		List<String> drawnBoots = context.computeOnClient(client -> drawnParts(client, Wayfaring.FEET_SLOT));
		if (!drawnBelt.equals(belt) || !drawnBoots.equals(boots)) {
			wrong.add(state + ": the belt is drawn on " + drawnBelt + ", not " + belt + ", and the boots on " + drawnBoots + ", not "
					+ boots);
		}
		if (armour != null) {
			server.runCommand("item replace entity @p " + armour.substring(0, armour.indexOf(' ')) + " with minecraft:air");
		}
	}

	/**
	 * Turns this player's Show my worn trinkets setting as the settings screen does (which sends the choice to the server),
	 * then waits, at most 100 ticks, until the server's player and this client's agree with it (the server holds a change
	 * that comes within WornDisplay.CHANGE_TICKS of the last until its tick, then sends it to this client as to everyone
	 * who sees the player); "" once they do, or what each has.
	 */
	private static String choose(ClientGameTestContext context, TestServerContext server, boolean shown) {
		context.runOnClient(client -> ConcordanceClientOptions.setWornTrinkets(shown));
		boolean onServer = !shown;
		boolean onClient = !shown;
		for (int waited = 0; waited <= 100; waited++) {
			onServer = server.computeOnServer(minecraft -> WornDisplay.hidden(player(minecraft)));
			onClient = context.computeOnClient(client -> WornDisplay.hidden(client.player));
			if (onServer == !shown && onClient == !shown) {
				return "";
			}
			context.waitTicks(1);
		}
		return "chose to " + (shown ? "show" : "hide") + " them, but after 100 ticks the server has them " + (onServer ? "hidden" : "shown")
				+ " and this client " + (onClient ? "hidden" : "shown");
	}

	/**
	 * The model parts the render definition of what the player wears first in {@code slot} would draw on now: its elements
	 * run as Trinkets runs them while it takes the player's render state (with the real player, so the armour and the
	 * setting count), against a stand-in render context that only records where each model would be attached.
	 */
	private static List<String> drawnParts(Minecraft client, String slot) {
		List<String> parts = new ArrayList<>();
		TrinketSlotAccess access = TrinketsApi.getAttachment(client.player).getSlotAccess(slot, 0);
		ItemStack stack = access.get();
		TrinketRenderContext recorder = (TrinketRenderContext) Proxy.newProxyInstance(TrinketRenderContext.class.getClassLoader(),
				new Class<?>[] {TrinketRenderContext.class}, (proxy, method, args) -> switch (method.getName()) {
					case "setupAttachedRenderer" -> {
						parts.add(((AttachmentSettings) args[0]).modelPart());
						yield null;
					}
					case "type" -> TrinketRenderContext.Type.FULL;
					case "minecraft" -> client;
					case "hashCode" -> System.identityHashCode(proxy);
					case "equals" -> proxy == args[0];
					case "toString" -> "the test's recording render context";
					default -> null;
				});
		for (TrinketRenderElement.Baked baked : ClientTrinketsManager.INSTANCE.getResolved(stack)) {
			baked.apply(client.player, stack, access, client.level, recorder, null);
		}
		return parts;
	}

	/**
	 * The player wearing the belt and the boot, from the front and from behind: whole (field of view 70, looking level), then
	 * closer (50, the camera above, looking down 20 degrees, so the feet stay in the picture), then from each quarter (the
	 * view turned 45 degrees, which shows the boots' sides and fins), then under an iron chestplate (no belt), under iron
	 * boots (no boots) and with the Show my worn trinkets setting off (neither), then sneaking from behind. Standing still, a
	 * player's body turns only once the head is
	 * more than 50 degrees past it, and a teleport turns only the head, so the body is set facing north on the client; the
	 * later turns, 45 degrees at most, are the view's alone. The creative guide every creative player is given would be in
	 * the hand, in front of the belt, so the hand is emptied first. The camera is put back to first person at 70 after.
	 */
	private static void wornShots(ClientGameTestContext context, TestSingleplayerContext singleplayer, int x, int y, int z) {
		TestServerContext server = singleplayer.getServer();
		server.runCommand(String.format(Locale.ROOT, "tp @p %.2f %d %.2f 180 0", x + 0.5, y, z - 2.5));
		server.runCommand("item replace entity @p weapon.mainhand with minecraft:air");
		context.waitTicks(10);
		context.runOnClient(client -> {
			client.player.setYBodyRot(180.0F);
			client.player.yBodyRotO = 180.0F;
		});
		context.waitTicks(5);
		singleplayer.getConnection().waitForChunksRender();
		look(context, server, CameraType.THIRD_PERSON_FRONT, 70, 180, 0, "jugcraft_wayfaring_worn_front");
		look(context, server, CameraType.THIRD_PERSON_BACK, 70, 180, 0, "jugcraft_wayfaring_worn_back");
		// The front camera sits opposite the look: looking up 20 degrees puts it above, looking down at the player.
		look(context, server, CameraType.THIRD_PERSON_FRONT, 50, 180, -20, "jugcraft_wayfaring_worn_front_close");
		look(context, server, CameraType.THIRD_PERSON_BACK, 50, 180, 20, "jugcraft_wayfaring_worn_back_close");
		// Facing north, a view turned to 135 (north-west) puts the front camera at the wearer's left and the back camera at
		// their right; 225 (north-east) the other way round.
		look(context, server, CameraType.THIRD_PERSON_FRONT, 50, 135, -20, "jugcraft_wayfaring_worn_front_left");
		look(context, server, CameraType.THIRD_PERSON_FRONT, 50, 225, -20, "jugcraft_wayfaring_worn_front_right");
		look(context, server, CameraType.THIRD_PERSON_BACK, 50, 135, 20, "jugcraft_wayfaring_worn_back_right");
		look(context, server, CameraType.THIRD_PERSON_BACK, 50, 225, 20, "jugcraft_wayfaring_worn_back_left");
		// Armour worn over them hides them, and so does the setting.
		server.runCommand("item replace entity @p armor.chest with minecraft:iron_chestplate");
		look(context, server, CameraType.THIRD_PERSON_FRONT, 50, 180, -20, "jugcraft_wayfaring_worn_under_chestplate");
		server.runCommand("item replace entity @p armor.chest with minecraft:air");
		server.runCommand("item replace entity @p armor.feet with minecraft:iron_boots");
		look(context, server, CameraType.THIRD_PERSON_FRONT, 50, 180, -20, "jugcraft_wayfaring_worn_under_boots");
		server.runCommand("item replace entity @p armor.feet with minecraft:air");
		String hidden = choose(context, server, false);
		look(context, server, CameraType.THIRD_PERSON_FRONT, 50, 180, -20, "jugcraft_wayfaring_worn_setting_off");
		String shown = choose(context, server, true);
		check(hidden.isEmpty() && shown.isEmpty(), "Show my worn trinkets, for the shots: " + hidden + shown);
		context.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_BACK));
		server.runCommand("execute as @p at @s run tp @s ~ ~ ~ 180 20");
		context.getInput().holdKey(options -> options.keyShift);
		context.waitTicks(15);
		boolean sneaking = context.computeOnClient(client -> client.player.isShiftKeyDown());
		context.takeScreenshot("jugcraft_wayfaring_worn_sneaking_back");
		context.getInput().releaseKey(options -> options.keyShift);
		context.runOnClient(client -> {
			client.options.setCameraType(CameraType.FIRST_PERSON);
			client.options.fov().set(70);
		});
		check(sneaking, "The player was not sneaking for the sneaking shot");
	}

	/** Turns the player's view (not their place, so their body stays as it is) and takes a shot with that camera. */
	private static void look(ClientGameTestContext context, TestServerContext server, CameraType camera, int fov, int yaw, int pitch,
			String name) {
		server.runCommand("execute as @p at @s run tp @s ~ ~ ~ " + yaw + " " + pitch);
		context.runOnClient(client -> {
			client.options.setCameraType(camera);
			client.options.fov().set(fov);
		});
		context.waitTicks(10);
		context.takeScreenshot(name);
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
