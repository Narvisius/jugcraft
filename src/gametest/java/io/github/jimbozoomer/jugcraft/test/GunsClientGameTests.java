package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.client.GunsClient;
import io.github.jimbozoomer.jugcraft.client.guns.GunArmsLayer;
import io.github.jimbozoomer.jugcraft.client.guns.GunEffects;
import io.github.jimbozoomer.jugcraft.client.guns.GunLaser;
import io.github.jimbozoomer.jugcraft.client.guns.GunPose;
import io.github.jimbozoomer.jugcraft.client.guns.GunRenderer;
import io.github.jimbozoomer.jugcraft.client.guns.GunScope;
import io.github.jimbozoomer.jugcraft.client.guns.GunView;
import io.github.jimbozoomer.jugcraft.guns.EnergyCellItem;
import io.github.jimbozoomer.jugcraft.guns.GunItem;
import io.github.jimbozoomer.jugcraft.guns.GunShots;
import io.github.jimbozoomer.jugcraft.guns.GunSpec;
import io.github.jimbozoomer.jugcraft.guns.JugcraftGuns;
import io.github.jimbozoomer.jugcraft.guns.MobGuns;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
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
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.AABB;

/**
 * Client game test for the guns (slice 1), end to end through the real input: each gun drawn in first person, aimed down
 * its sights and fired so at a husk with the attack key (the server lands the shot and spends a round), reloaded with the
 * reload key (part way through and done: the rounds come out of the inventory), and inspected; the Thunderpipe's
 * shell-at-a-time reload part way; each gun that takes attachments held with two sets of them fitted (slice 5), the
 * client seeing a fitted magazine's capacity; each gun held in third person and shown in the inventory with the
 * attachments. Slice 6: each gun narrows the view aimed, shows a muzzle flash fired and throws the spent casings its
 * animations cue; in third person the player is posed holding it, and fires it. Slice 7: a third set of attachments
 * with a bayonet (and the guns whose parts use shared textures), and a stab with the stab key that hurts the husk; and the
 * scopes, each aimed through on the Longhorn Rifle (the view through it or the reflex dot, the narrowed view, the slower
 * mouse). Slice 8C: the heavy weapons fire their own ammunition (grenades, blaze powder) the same way, and the
 * Thresher's trigger is held until its barrels have spun up and it fires. Slice 9F: aimed down its sights, each gun's
 * arms are drawn at half their size, and a fitted stock is left out. Screenshots jugcraft_guns_* (CI job
 * {@code client}).
 */
public class GunsClientGameTests implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext singleplayer = context.worldBuilder()
				.adjustSettings(creator -> creator.setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL)).create()) {
			singleplayer.getConnection().waitForChunksRender();
			BlockPos origin = context.computeOnClient(client -> BlockPos.containing(client.player.position()));
			int x = origin.getX();
			int y = origin.getY();
			int z = origin.getZ();
			TestServerContext server = singleplayer.getServer();
			server.runCommand("time set noon");
			server.runCommand("weather clear");
			server.runCommand("gamerule minecraft:send_command_feedback false");
			server.runCommand("fill %d %d %d %d %d %d minecraft:smooth_stone".formatted(x - 8, y - 1, z - 12, x + 8, y - 1, z + 4));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 8, y, z - 12, x + 8, y + 6, z + 4));
			server.runCommand("fill %d %d %d %d %d %d minecraft:stone_bricks".formatted(x - 4, y, z - 12, x + 4, y + 4, z - 12));
			// The player looks north at a still husk seven blocks away, in front of a brick wall.
			server.runCommand(String.format(Locale.ROOT, "tp @p %.1f %d %.1f 180 5", x + 0.5, y, z + 0.5));
			server.runCommand(String.format(Locale.ROOT, "summon minecraft:husk %.1f %d %.1f {NoAI:1b,PersistenceRequired:1b,"
					+ "Rotation:[0f,0f],attributes:[{id:\"minecraft:armor\",base:0.0d},{id:\"minecraft:max_health\",base:1000.0d},"
					+ "{id:\"minecraft:knockback_resistance\",base:1.0d}],Health:1000.0f}", x + 0.5, y, z - 6.5));
			context.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));
			// The HUD shown (F1 hides the hand, and the gun and its flash with it), whatever an earlier test in the shard left;
			// its state is put back at the end.
			boolean hudWasHidden = context.computeOnClient(client -> client.gui.hud.isHidden());
			setHudHidden(context, false);

			for (String gun : JugcraftGuns.SPECS.keySet()) {
				GunSpec spec = JugcraftGuns.SPECS.get(gun);
				// Slice 8C: a round, a grenade or blaze powder, by its item id.
				Item ammo = JugcraftGuns.ammo(spec);
				String round = BuiltInRegistries.ITEM.getKey(ammo).toString();
				int capacity = spec.capacity();
				int spinUp = JugcraftGuns.spinUp(JugcraftGuns.GUNS.get(gun));
				// Slice 8D: an energy weapon loads each round's charge from Energy Cells.
				int charge = JugcraftGuns.charge(JugcraftGuns.GUNS.get(gun));
				// Each gun starts from the same aim: every shot kicks the view up, and twelve guns' kicks would lift it
				// over the husk.
				server.runCommand(String.format(Locale.ROOT, "tp @p %.1f %d %.1f 180 5", x + 0.5, y, z + 0.5));
				server.runCommand("clear @p");
				server.runCommand("item replace entity @p weapon.mainhand with jugcraft:%s[jugcraft:loaded_rounds=%d]".formatted(gun, capacity));
				if (charge > 0) {
					server.runCommand("give @p %s[jugcraft:energy=%dL] 2".formatted(round, EnergyCellItem.CAPACITY));
				} else {
					server.runCommand("give @p %s 32".formatted(round));
				}
				context.waitTicks(30);
				context.takeScreenshot("jugcraft_guns_" + gun + "_held");
				float heldSize = context.computeOnClient(client -> GunArmsLayer.lastSize());

				// Aimed (the aimed spread keeps the shot on the husk), then fired down the sights.
				context.getInput().holdKey(options -> options.keyUse);
				context.waitTicks(10);
				context.takeScreenshot("jugcraft_guns_" + gun + "_aimed");
				// Slice 9F: aimed down its sights, the arms are drawn at half their size; a gun without sights (it stays at
				// the hip) keeps them as they are.
				float aimedSize = context.computeOnClient(client -> GunArmsLayer.lastSize());
				boolean sighted = sighted(context, gun);
				Jugcraft.LOGGER.info("[guns] {} arms drawn at size {} held and {} aimed (sights: {})", gun, heldSize, aimedSize, sighted);
				if (Math.abs(heldSize - 1.0F) > 1.0E-3F || Math.abs(aimedSize - (sighted ? 0.5F : 1.0F)) > 1.0E-3F) {
					throw new AssertionError("The " + gun + "'s arms were drawn at size " + heldSize + " held and " + aimedSize
							+ " aimed, not 1 and " + (sighted ? "0.5" : "1"));
				}
				// Slice 6: aimed, the view narrows by the gun's zoom (GunFovMixin hands vanilla's modifier to GunView).
				float fovIn = context.computeOnClient(client -> GunView.lastFovIn());
				float fovOut = context.computeOnClient(client -> GunView.lastFovOut());
				Jugcraft.LOGGER.info("[guns] {} aimed: field of view modifier {} -> {}", gun, fovIn, fovOut);
				if (!(fovOut < fovIn * 0.99F)) {
					throw new AssertionError("Aiming the " + gun + " does not narrow the view: modifier " + fovIn + " -> " + fovOut
							+ (Float.isNaN(fovIn) ? " (GunFovMixin never ran: has AbstractClientPlayer.getFieldOfViewModifier moved?)" : ""));
				}
				float before = health(server, x, y, z);
				long flashes = context.computeOnClient(client -> GunEffects.flashes());
				long ejected = context.computeOnClient(client -> GunEffects.ejected());
				long traces = context.computeOnClient(client -> GunEffects.traced());
				// The shot, with its flash (it shows for two ticks). A rotary gun's trigger is held while its barrels spin
				// up, then a shot or two, and let go.
				if (spinUp > 0) {
					context.getInput().holdKey(options -> options.keyAttack);
					context.waitTicks(spinUp + 2);
					context.takeScreenshot("jugcraft_guns_" + gun + "_fired");
					context.getInput().releaseKey(options -> options.keyAttack);
				} else {
					context.getInput().pressKey(options -> options.keyAttack);
					context.takeScreenshot("jugcraft_guns_" + gun + "_fired");
				}
				long flashed = context.computeOnClient(client -> GunEffects.flashes()) - flashes;
				Jugcraft.LOGGER.info("[guns] {} fired: {} muzzle flash frames drawn", gun, flashed);
				if (flashed <= 0) {
					throw new AssertionError("The " + gun + " showed no muzzle flash when fired");
				}
				context.waitTicks(11);
				if (charge > 0) {
					// Slice 8D: the server says where the shot went, and the client draws it.
					long drawn = context.computeOnClient(client -> GunEffects.traced()) - traces;
					Jugcraft.LOGGER.info("[guns] {} fired: {} beam or arc drawn", gun, drawn);
					if (drawn <= 0) {
						throw new AssertionError("The " + gun + "'s shot was not drawn (GunTracePayload)");
					}
				}
				context.getInput().releaseKey(options -> options.keyUse);
				context.waitTicks(5);
				float after = health(server, x, y, z);
				int loaded = server.computeOnServer(minecraft -> GunItem.loaded(player(minecraft).getMainHandItem()));
				int spent = capacity - loaded;
				Jugcraft.LOGGER.info("[guns] {} fired at a husk: health {} -> {}, rounds {} -> {}", gun, before, after, capacity, loaded);
				if (after >= before || spent < 1 || spinUp == 0 && spent != 1) {
					throw new AssertionError("The " + gun + " did not hit the husk and spend " + (spinUp > 0 ? "rounds" : "a round")
							+ ": health " + before + " -> " + after + ", rounds " + capacity + " -> " + loaded);
				}

				context.getInput().pressKey(options -> GunsClient.reloadKey());
				int ticks = spec.reloadTicks(spent);
				context.waitTicks(Math.max(5, ticks / 2));
				context.takeScreenshot("jugcraft_guns_" + gun + "_reloading");
				context.waitTicks(ticks);
				int reloaded = server.computeOnServer(minecraft -> GunItem.loaded(player(minecraft).getMainHandItem()));
				if (charge > 0) {
					// Slice 8D: the reload drew the rounds' charge from the cells and left them.
					long cells = server.computeOnServer(minecraft -> GunShots.charged(player(minecraft).getInventory(), ammo));
					int kept = server.computeOnServer(minecraft -> GunShots.count(player(minecraft).getInventory(), ammo));
					Jugcraft.LOGGER.info("[guns] {} reloaded: {} rounds loaded, {} JE left in its {} cells", gun, reloaded, cells, kept);
					if (reloaded != capacity || cells != 2L * EnergyCellItem.CAPACITY - (long) spent * charge || kept != 2) {
						throw new AssertionError("The " + gun + " reload did not draw the " + spent + " rounds' charge from its cells: "
								+ reloaded + " loaded, " + cells + " JE in " + kept + " cells left");
					}
				} else {
					int left = server.computeOnServer(minecraft -> GunShots.count(player(minecraft).getInventory(), ammo));
					// An item of blaze powder loads four bursts: a reload takes whole items (slice 8C).
					int taken = (spent + JugcraftGuns.perItem(spec) - 1) / JugcraftGuns.perItem(spec);
					Jugcraft.LOGGER.info("[guns] {} reloaded: {} rounds loaded, {} {} left", gun, reloaded, left, round);
					if (reloaded != capacity || left != 32 - taken) {
						throw new AssertionError("The " + gun + " reload did not load the " + spent + " rounds spent from the inventory: "
								+ reloaded + " loaded, " + left + " left");
					}
				}

				// The owner's animations cue a spent casing (or, for a paper cartridge, a puff from the lock) on the shot or the
				// reload; every gun whose animations have the cue threw one.
				long thrown = context.computeOnClient(client -> GunEffects.ejected()) - ejected;
				boolean cued = context.computeOnClient(client -> client.getResourceManager()
						.getResource(Jugcraft.id("geckolib/animations/item/" + gun + ".animation.json")).map(resource -> {
							try (InputStream in = resource.open()) {
								return new String(in.readAllBytes(), StandardCharsets.UTF_8).contains("\"eject_casing\"");
							} catch (IOException e) {
								return false;
							}
						}).orElse(false));
				Jugcraft.LOGGER.info("[guns] {} fired and reloaded: {} casings thrown (its animations cue them: {})", gun, thrown, cued);
				if (cued && thrown <= 0) {
					throw new AssertionError("The " + gun + "'s animations cue a spent casing, but none was thrown");
				}

				context.getInput().pressKey(options -> GunsClient.inspectKey());
				context.waitTicks(30);
				context.takeScreenshot("jugcraft_guns_" + gun + "_inspect");
				context.waitTicks(100);
			}

			// The Thunderpipe from empty: both shells, part way through the second.
			server.runCommand("item replace entity @p weapon.mainhand with jugcraft:thunderpipe[jugcraft:loaded_rounds=0]");
			context.waitTicks(30);
			context.getInput().pressKey(options -> GunsClient.reloadKey());
			context.waitTicks(JugcraftGuns.SPECS.get("thunderpipe").shellStart() + JugcraftGuns.SPECS.get("thunderpipe").shellEach() + 6);
			context.takeScreenshot("jugcraft_guns_thunderpipe_shell");
			context.waitTicks(40);

			// Attachments: each gun that takes any, with one of each slot it has from each set it takes any of, held and
			// aimed. The fourth set is slice 9E's Tactical Grip and Laser Sight.
			List<List<String>> sets = List.of(List.of("silencer", "extended_magazine", "light_stock", "light_grip"),
					List.of("extended_barrel", "speed_magazine", "weighted_stock", "vertical_grip"),
					List.of("muzzle_brake", "wooden_stock", "iron_bayonet"), List.of("tactical_grip", "laser_sight"));
			for (String gun : JugcraftGuns.ACCEPTS.keySet()) {
				for (int set = 0; set < sets.size(); set++) {
					List<String> fitted = sets.get(set).stream().filter(JugcraftGuns.ACCEPTS.get(gun)::contains).toList();
					if (fitted.isEmpty()) {
						continue;
					}
					server.runCommand("item replace entity @p weapon.mainhand with jugcraft:%s[jugcraft:loaded_rounds=1,jugcraft:attachments=%s]"
							.formatted(gun, snbt(fitted)));
					context.waitTicks(20);
					long stowed = context.computeOnClient(client -> GunRenderer.stowed());
					context.takeScreenshot("jugcraft_guns_" + gun + "_fitted_" + (set + 1));
					long stowedHeld = context.computeOnClient(client -> GunRenderer.stowed()) - stowed;
					context.getInput().holdKey(options -> options.keyUse);
					context.waitTicks(10);
					context.takeScreenshot("jugcraft_guns_" + gun + "_fitted_" + (set + 1) + "_aimed");
					// Slice 9F: a fitted stock is drawn at the hip and left out aimed down the gun's sights.
					long stowedAimed = context.computeOnClient(client -> GunRenderer.stowed()) - stowed - stowedHeld;
					context.getInput().releaseKey(options -> options.keyUse);
					boolean stocked = fitted.stream().anyMatch(name -> JugcraftGuns.ATTACHMENTS.get(name).slot().equals("stock"));
					if (stocked) {
						Jugcraft.LOGGER.info("[guns] {} with {}: the stock left out in {} frames held and {} aimed", gun, fitted, stowedHeld,
								stowedAimed);
					}
					if (stowedHeld != 0 || (stowedAimed > 0) != (stocked && sighted(context, gun))) {
						throw new AssertionError("The " + gun + " with " + fitted + ": its stock was left out in " + stowedHeld
								+ " frames held and " + stowedAimed + " aimed");
					}
					context.waitTicks(5);
				}
			}
			// Slice 7: the stab key stabs the husk with a Steel Bayonet from two blocks off; the server deals the blow.
			server.runCommand(String.format(Locale.ROOT, "tp @p %.1f %d %.1f 180 10", x + 0.5, y, z - 4.0));
			server.runCommand("item replace entity @p weapon.mainhand with jugcraft:patchwork_carbine[jugcraft:attachments=%s]"
					.formatted(snbt(List.of("steel_bayonet"))));
			context.waitTicks(20);
			float unstabbed = health(server, x, y, z);
			context.getInput().pressKey(options -> GunsClient.stabKey());
			context.takeScreenshot("jugcraft_guns_bayonet_stab");
			context.waitTicks(5);
			float stabbed = health(server, x, y, z);
			Jugcraft.LOGGER.info("[guns] a Steel Bayonet stab: husk health {} -> {}", unstabbed, stabbed);
			if (!(stabbed < unstabbed)) {
				throw new AssertionError("The Steel Bayonet's stab did not hurt the husk: health " + unstabbed + " -> " + stabbed);
			}
			server.runCommand(String.format(Locale.ROOT, "tp @p %.1f %d %.1f 180 5", x + 0.5, y, z + 0.5));

			server.runCommand("item replace entity @p weapon.mainhand with jugcraft:rust_midge[jugcraft:attachments=[\"extended_magazine\"]]");
			context.waitTicks(10);
			int seen = context.computeOnClient(client -> GunItem.spec(client.player.getMainHandItem()).capacity());
			Jugcraft.LOGGER.info("[guns] the client sees a Rust Midge with an Extended Magazine hold {} rounds", seen);
			if (seen != 30) {
				throw new AssertionError("The client sees the Extended Magazine's Rust Midge hold " + seen + " rounds, not 30");
			}

			// Slice 7, the scopes, each on the Longhorn Rifle aimed at the husk: through the Long and Medium Scopes the view
			// through the scope fills the screen and the view narrows by the scope's zoom; the Reflex Sight keeps the gun in
			// view and shows its dot. Through the Long Scope the mouse turns the player more slowly. Slice 9E's Laser Sight
			// shows its dot aimed as the Reflex Sight does, and held, draws its red dot where the gun points.
			Map<String, Float> zooms = Map.of("long_scope", 0.3F, "medium_scope", 0.5F, "reflex_sight", 0.85F, "laser_sight", 0.9F);
			for (String scope : List.of("long_scope", "medium_scope", "reflex_sight", "laser_sight")) {
				server.runCommand(String.format(Locale.ROOT, "tp @p %.1f %d %.1f 180 5", x + 0.5, y, z + 0.5));
				long lasered = context.computeOnClient(client -> GunLaser.dots());
				server.runCommand("item replace entity @p weapon.mainhand with jugcraft:longhorn_rifle[jugcraft:loaded_rounds=1,jugcraft:attachments=%s]"
						.formatted(snbt(List.of(scope))));
				context.waitTicks(20);
				context.takeScreenshot("jugcraft_guns_" + scope);
				long laserDots = context.computeOnClient(client -> GunLaser.dots()) - lasered;
				if (scope.equals("laser_sight")) {
					double toDot = context.computeOnClient(client -> GunLaser.lastOwn() == null ? -1.0
							: GunLaser.lastOwn().distanceTo(client.player.getEyePosition()));
					Jugcraft.LOGGER.info("[guns] the Laser Sight held: {} dots drawn in 20 ticks, the last {} blocks from the eye", laserDots, toDot);
					if (laserDots <= 0 || toDot <= 0.0) {
						throw new AssertionError("The Laser Sight drew " + laserDots + " dots where the gun points (the last " + toDot + " blocks off)");
					}
				} else if (laserDots > 0) {
					throw new AssertionError("The " + scope + " drew " + laserDots + " laser dots");
				}
				long views = context.computeOnClient(client -> GunScope.views());
				long dots = context.computeOnClient(client -> GunScope.dots());
				context.getInput().holdKey(options -> options.keyUse);
				context.waitTicks(10);
				context.takeScreenshot("jugcraft_guns_" + scope + "_aimed");
				float fovIn = context.computeOnClient(client -> GunView.lastFovIn());
				float fovOut = context.computeOnClient(client -> GunView.lastFovOut());
				long viewed = context.computeOnClient(client -> GunScope.views()) - views;
				long dotted = context.computeOnClient(client -> GunScope.dots()) - dots;
				Jugcraft.LOGGER.info("[guns] aimed through the {}: field of view modifier {} -> {}; {} frames of the view through it, {} of the dot",
						scope, fovIn, fovOut, viewed, dotted);
				if (Math.abs(fovOut - fovIn * zooms.get(scope)) > 0.01F) {
					throw new AssertionError("Aimed through the " + scope + " the view did not narrow by its zoom " + zooms.get(scope) + ": "
							+ fovIn + " -> " + fovOut);
				}
				if (scope.equals("reflex_sight") || scope.equals("laser_sight") ? dotted <= 0 || viewed > 0 : viewed <= 0) {
					throw new AssertionError("Aimed through the " + scope + ", " + viewed + " frames of the view through a scope and "
							+ dotted + " of the reflex dot were drawn");
				}
				if (scope.equals("long_scope")) {
					float yaw = context.computeOnClient(client -> client.player.getYRot());
					context.computeOnClient(client -> GunView.lowestTurnScale());
					context.getInput().moveCursor(60.0, 0.0);
					context.waitTicks(2);
					float turned = context.computeOnClient(client -> client.player.getYRot()) - yaw;
					double scale = context.computeOnClient(client -> GunView.lowestTurnScale());
					Jugcraft.LOGGER.info("[guns] the mouse moved through the Long Scope: the player turned {} degrees, turn scale {}", turned, scale);
					if (Math.abs(turned) > 1.0E-4F && scale > 0.99) {
						throw new AssertionError("Through the Long Scope the mouse turned the player at full speed (GunMouseMixin)");
					}
				}
				context.getInput().releaseKey(options -> options.keyUse);
				context.waitTicks(5);
			}

			// Seen from outside (slice 6): the gun arm raised along the look, the other across to the fore-end for a gun held
			// in both hands; then a shot, its flash seen from in front.
			long posed = context.computeOnClient(client -> GunPose.posed());
			context.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
			for (String gun : JugcraftGuns.SPECS.keySet()) {
				server.runCommand(String.format(Locale.ROOT, "tp @p %.1f %d %.1f 180 5", x + 0.5, y, z + 0.5));
				server.runCommand("item replace entity @p weapon.mainhand with jugcraft:%s[jugcraft:loaded_rounds=1]".formatted(gun));
				context.waitTicks(20);
				context.takeScreenshot("jugcraft_guns_" + gun + "_third_person");
				context.getInput().pressKey(options -> options.keyAttack);
				context.takeScreenshot("jugcraft_guns_" + gun + "_third_person_fired");
				context.waitTicks(10);
			}
			context.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_BACK));
			for (String gun : List.of("longhorn_rifle", "warden_pistol")) {
				server.runCommand("item replace entity @p weapon.mainhand with jugcraft:" + gun);
				context.waitTicks(20);
				context.takeScreenshot("jugcraft_guns_" + gun + "_third_person_back");
			}
			context.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));
			long posedFrames = context.computeOnClient(client -> GunPose.posed()) - posed;
			Jugcraft.LOGGER.info("[guns] in third person the player was posed holding a gun for {} frames", posedFrames);
			if (posedFrames <= 0) {
				throw new AssertionError("In third person the player was never posed holding a gun (GunPose)");
			}

			// Slice 10F: three raider gunners, one with each of the service arms they carry, four blocks off and facing the
			// player, each posed holding its gun as a player holds one. The player's hands are empty, so only theirs pose.
			server.runCommand("clear @p");
			long unposed = context.computeOnClient(client -> GunPose.posed());
			for (int i = 0; i < MobGuns.ARMS.size(); i++) {
				String tag = "jugcraft_gunner_" + i;
				server.runCommand(String.format(Locale.ROOT, "summon jugcraft:raider_gunner %.1f %d %.1f {NoAI:1b,PersistenceRequired:1b,"
						+ "Rotation:[0f,0f],Tags:[\"%s\"]}", x - 1.5 + 2 * i, y, z - 3.5, tag));
				server.runCommand("item replace entity @e[tag=%s] weapon.mainhand with jugcraft:%s".formatted(tag, MobGuns.ARMS.get(i).gun()));
			}
			server.runCommand(String.format(Locale.ROOT, "tp @p %.1f %d %.1f 180 5", x + 0.5, y, z + 0.5));
			context.waitTicks(20);
			context.takeScreenshot("jugcraft_guns_raider_gunners");
			long gunnerFrames = context.computeOnClient(client -> GunPose.posed()) - unposed;
			Jugcraft.LOGGER.info("[guns] the raider gunners were posed holding their guns for {} frames", gunnerFrames);
			server.runCommand("kill @e[type=jugcraft:raider_gunner]");
			if (gunnerFrames <= 0) {
				throw new AssertionError("The raider gunners were never posed holding their guns (GunPose)");
			}

			// Slice 10G: two guns at once. A Sentry Pistol in the main hand and a Warden Pistol in the other, both loaded, and
			// Light Rounds to reload them: left click fires the first; right click, held, fires the second once and aims
			// neither; then the other hand holds a Rattler Pistol, which fires for as long as right click is held. G reloads
			// the main gun, then (it being full) the other. Seen from outside, both arms are raised, each with its gun.
			server.runCommand("clear @p");
			server.runCommand(String.format(Locale.ROOT, "tp @p %.1f %d %.1f 180 5", x + 0.5, y, z + 0.5));
			server.runCommand("item replace entity @p weapon.mainhand with jugcraft:sentry_pistol[jugcraft:loaded_rounds=8]");
			server.runCommand("item replace entity @p weapon.offhand with jugcraft:warden_pistol[jugcraft:loaded_rounds=12]");
			server.runCommand("give @p jugcraft:light_round 32");
			context.waitTicks(30);
			context.takeScreenshot("jugcraft_guns_dual_held");
			float dualBefore = health(server, x, y, z);
			long dualFlashes = context.computeOnClient(client -> GunEffects.flashes());
			context.getInput().pressKey(options -> options.keyAttack);
			context.takeScreenshot("jugcraft_guns_dual_main_fired");
			context.waitTicks(10);
			context.getInput().holdKey(options -> options.keyUse);
			context.takeScreenshot("jugcraft_guns_dual_other_fired");
			context.waitTicks(10);
			boolean dualAimed = context.computeOnClient(client -> client.player.isUsingItem());
			context.getInput().releaseKey(options -> options.keyUse);
			context.waitTicks(5);
			long dualFlashed = context.computeOnClient(client -> GunEffects.flashes()) - dualFlashes;
			int mainLeft = server.computeOnServer(minecraft -> GunItem.loaded(player(minecraft).getMainHandItem()));
			int otherLeft = server.computeOnServer(minecraft -> GunItem.loaded(player(minecraft).getOffhandItem()));
			Jugcraft.LOGGER.info("[guns] two guns fired, left click then right click held: rounds 8 -> {} and 12 -> {}, {} flash frames, "
					+ "husk {} -> {}, aimed: {}", mainLeft, otherLeft, dualFlashed, dualBefore, health(server, x, y, z), dualAimed);
			if (mainLeft != 7 || otherLeft != 11 || dualFlashed <= 0 || dualAimed) {
				throw new AssertionError("Two guns did not each fire one shot, flash and stay unaimed: rounds 8 -> " + mainLeft + " and 12 -> "
						+ otherLeft + ", " + dualFlashed + " flash frames, aimed: " + dualAimed);
			}
			context.getInput().pressKey(options -> GunsClient.reloadKey());
			context.waitTicks(JugcraftGuns.SPECS.get("sentry_pistol").reload() / 2);
			context.takeScreenshot("jugcraft_guns_dual_reloading");
			context.waitTicks(JugcraftGuns.SPECS.get("sentry_pistol").reload() / 2 + 10);
			context.getInput().pressKey(options -> GunsClient.reloadKey());
			context.waitTicks(JugcraftGuns.SPECS.get("warden_pistol").reload() + 10);
			int mainReloaded = server.computeOnServer(minecraft -> GunItem.loaded(player(minecraft).getMainHandItem()));
			int otherReloaded = server.computeOnServer(minecraft -> GunItem.loaded(player(minecraft).getOffhandItem()));
			int roundsLeft = server.computeOnServer(minecraft -> GunShots.count(player(minecraft).getInventory(), JugcraftGuns.ROUNDS.get("light_round")));
			Jugcraft.LOGGER.info("[guns] G pressed twice with two guns: rounds {} and {}, {} Light Rounds left", mainReloaded, otherReloaded, roundsLeft);
			if (mainReloaded != 8 || otherReloaded != 12 || roundsLeft != 30) {
				throw new AssertionError("G did not reload the main gun and then the other: rounds " + mainReloaded + " and " + otherReloaded
						+ ", " + roundsLeft + " Light Rounds left");
			}
			server.runCommand("item replace entity @p weapon.offhand with jugcraft:rattler_pistol[jugcraft:loaded_rounds=20]");
			context.waitTicks(20);
			context.getInput().holdKey(options -> options.keyUse);
			context.waitTicks(15);
			context.getInput().releaseKey(options -> options.keyUse);
			context.waitTicks(5);
			int rattlerLeft = server.computeOnServer(minecraft -> GunItem.loaded(player(minecraft).getOffhandItem()));
			Jugcraft.LOGGER.info("[guns] right click held for 15 ticks with a Rattler Pistol in the other hand: rounds 20 -> {}", rattlerLeft);
			if (rattlerLeft > 18) {
				throw new AssertionError("Right click held fired the other hand's Rattler Pistol only " + (20 - rattlerLeft) + " times");
			}
			long posedDual = context.computeOnClient(client -> GunPose.posedDual());
			context.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
			context.waitTicks(20);
			context.takeScreenshot("jugcraft_guns_dual_third_person");
			context.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_BACK));
			context.waitTicks(20);
			context.takeScreenshot("jugcraft_guns_dual_third_person_back");
			context.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));
			long dualFrames = context.computeOnClient(client -> GunPose.posedDual()) - posedDual;
			Jugcraft.LOGGER.info("[guns] in third person the player was posed holding a gun in each hand for {} frames", dualFrames);
			if (dualFrames <= 0) {
				throw new AssertionError("In third person the player was never posed holding a gun in each hand (GunPose)");
			}

			// The icons, in two inventories (they no longer fit one): the guns and rounds, then the attachments.
			server.runCommand("clear @p");
			for (String gun : JugcraftGuns.SPECS.keySet()) {
				server.runCommand("give @p jugcraft:" + gun);
			}
			for (String round : JugcraftGuns.AMMO) {
				server.runCommand("give @p jugcraft:%s 16".formatted(round));
			}
			// Slice 8D: an Energy Cell charged, glowing, and one spent.
			server.runCommand("give @p jugcraft:energy_cell[jugcraft:energy=%dL]".formatted(EnergyCellItem.CAPACITY));
			server.runCommand("give @p jugcraft:energy_cell");
			server.runCommand("give @p jugcraft:patchwork_carbine[jugcraft:attachments=%s]".formatted(
					snbt(List.of("baffled_silencer", "extended_magazine", "wooden_stock", "vertical_grip"))));
			context.waitTicks(10);
			context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
			context.waitTicks(10);
			context.takeScreenshot("jugcraft_guns_inventory");
			context.setScreen(() -> null);
			server.runCommand("clear @p");
			for (String attachment : JugcraftGuns.ATTACHMENTS.keySet()) {
				server.runCommand("give @p jugcraft:" + attachment);
			}
			context.waitTicks(10);
			context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
			context.waitTicks(10);
			context.takeScreenshot("jugcraft_guns_inventory_attachments");
			context.setScreen(() -> null);
			setHudHidden(context, hudWasHidden);
		}
	}

	/** Hides or shows the HUD, hand and chat (F1) whatever state an earlier test left it in, as ArmorTiersClientGameTests does. */
	private static void setHudHidden(ClientGameTestContext context, boolean hidden) {
		context.runOnClient(client -> {
			if (client.gui.hud.isHidden() != hidden) {
				client.gui.hud.toggle();
			}
		});
	}

	/** Whether the gun's model has sights, which aiming slides onto the middle of the screen (slice 9F). */
	private static boolean sighted(ClientGameTestContext context, String gun) {
		return context.computeOnClient(client -> client.getResourceManager()
				.getResource(Jugcraft.id("geckolib/models/item/" + gun + ".geo.json")).map(resource -> {
					try (InputStream in = resource.open()) {
						return new String(in.readAllBytes(), StandardCharsets.UTF_8).contains("\"sight\"");
					} catch (IOException e) {
						return false;
					}
				}).orElse(false));
	}

	/** A list of attachment ids as SNBT, for an item component in a command. */
	private static String snbt(List<String> ids) {
		return ids.stream().map(id -> "\"" + id + "\"").collect(Collectors.joining(",", "[", "]"));
	}

	private static ServerPlayer player(MinecraftServer minecraft) {
		return minecraft.getPlayerList().getPlayers().getFirst();
	}

	/** The husk's health, on the server. */
	private static float health(TestServerContext server, int x, int y, int z) {
		return server.computeOnServer(minecraft -> minecraft.overworld().getEntitiesOfClass(LivingEntity.class,
				new AABB(x - 4, y - 2, z - 10, x + 5, y + 4, z), entity -> BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath().equals("husk"))
				.stream().findFirst().map(LivingEntity::getHealth).orElse(-1.0F));
	}
}
