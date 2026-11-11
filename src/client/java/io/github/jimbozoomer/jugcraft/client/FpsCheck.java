package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.tower.JugcraftTower;
import io.github.jimbozoomer.jugcraft.tower.TowerRegistry;
import io.github.jimbozoomer.jugcraft.tower.crew.CrewShip;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.InactivityFpsLimit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A frame rate check that runs itself (Justin, 8 November 2026: "do a check for FPS on your own in a way that I don't
 * have to control it myself"). Dormant unless the game folder holds {@code jugcraft-fps-check.txt} when the game
 * starts (28-fps-check.bat writes it, and starts the game straight into a throwaway copy of his world, folder
 * {@code JugcraftFpsCheck}); the file is deleted as soon as it is read, so the next launch plays normally, and the
 * check runs in no world but that copy.
 *
 * <p>Once the world is in, it turns off the frame cap and vsync for the run (his settings are put back before the game
 * closes; nothing is saved), keeps the game from dropping to its idle frame rate, finds the nearest tower, and puts the
 * camera (in spectator) at a list of views: the tower from outside, a view of each of the tower's big fixtures, a
 * parked drop ship near, between its two cut-offs and far, and its bay from inside, first without the ship and then
 * with it, and a couple at night. At each view it waits for the chunks to finish building, times every frame for 8
 * seconds (the time between frames and the game's own work time for each), notes the server's tick time, takes a
 * screenshot, and at a few views also makes the game's own 10-second profile (the F3+L report). A Java Flight Recorder
 * recording covers the whole run. Results go to {@code fpscheck/} in the game folder (report.txt, frames.csv,
 * profile.jfr, the screenshots and the profiles); then the game closes itself.
 *
 * <p>Made to match how he plays (r186, after his JFR of 8 November 2026 read 42 fps inside where the check read 54-64):
 * the hangars are filled with drones and some sent flying (the check's world had none), the ship stays parked outside
 * for the indoor views, there is a 30-second walk round the lobby and up a floor (with {@link Sight} off, on, and on
 * with his frame cap and vsync) and a view close in front of a wall, the window fills his main screen as he plays, and
 * the report gives, for each view, each renderer's milliseconds a frame worked out from the recording
 * ({@link FpsCheckBreakdown}), how much {@link Sight} skipped, the window size and the drones docked and flying.
 */
public final class FpsCheck {
	private static final Logger LOG = LoggerFactory.getLogger("Jugcraft FPS check");
	private static final String TRIGGER = "jugcraft-fps-check.txt";
	/** The world folder the bat copies his world to; the check runs in no other. */
	private static final String WORLD = "JugcraftFpsCheck";
	/** Seconds: each view's measuring time, the least and most time its chunks get first, and the whole run's limit. */
	private static final double MEASURE = 8, SETTLE_MIN = 4, SETTLE_MAX = 25, RUN_MAX = 30 * 60;

	private enum Stage { WAIT_WORLD, LOADING, SETUP, NEXT, SETTLE, MEASURE, SHOT, METRICS, FINISH, CLOSE, DONE }

	private static boolean active;
	private static Path out;
	private static Stage stage = Stage.WAIT_WORLD;
	private static long stageStart, runStart, wallStart;
	private static final List<FpsCheckPlan.View> views = new ArrayList<>();
	private static int viewIndex = -1;
	private static final AtomicBoolean serverDone = new AtomicBoolean();
	private static volatile String setupNote = "";
	private static volatile UUID shipId;
	private static volatile Vec3 shipLand;
	private static volatile float shipYaw;
	/** Drones docked and flying at the views (server thread writes, client reads). */
	private static volatile String droneNote = "";
	/** Each measured view's wall-clock start and end (for the breakdown from the recording). */
	private static final List<FpsCheckBreakdown.Window> windows = new ArrayList<>();
	private static long measureStartMs;
	/** Sight's counts summed over the measured frames. */
	private static long sightChecked, sightSkipped, keptDrawn;
	private static final StringBuilder report = new StringBuilder();
	private static final List<String> errors = java.util.Collections.synchronizedList(new ArrayList<>());
	private static Object recording;

	// his settings, put back at the end
	private static boolean origVsync, origPause, origFullscreen;
	private static int origFps;
	private static InactivityFpsLimit origIdle;
	private static boolean settingsChanged;

	// frames: the time between one frame and the next, and the game's own time for the frame (ns)
	private static final long[] gap = new long[400_000], work = new long[400_000];
	private static int frames;
	private static long lastFrame;
	private static boolean timing;

	private FpsCheck() {
	}

	/** Called from the client initializer: arms the check if its file is there (and deletes the file). */
	public static void init() {
		Path game = FabricLoader.getInstance().getGameDir();
		Path trigger = game.resolve(TRIGGER);
		if (!Files.exists(trigger)) {
			return;
		}
		try {
			Files.delete(trigger);
		} catch (IOException e) {
			LOG.warn("Could not delete {}", trigger, e);
		}
		active = true;
		out = game.resolve("fpscheck");
		try {
			if (Files.isDirectory(out)) {
				try (var old = Files.walk(out)) {
					old.sorted(java.util.Comparator.reverseOrder()).filter(p -> !p.equals(out)).forEach(p -> p.toFile().delete());
				}
			}
			Files.createDirectories(out.resolve("profiles"));
		} catch (IOException e) {
			LOG.warn("Could not make {}", out, e);
		}
		LOG.info("Armed: results go to {}", out);
		HudElementRegistry.addLast(Jugcraft.id("fps_check"), (graphics, delta) -> frame());
		ClientTickEvents.END_CLIENT_TICK.register(FpsCheck::tick);
	}

	private static void frame() {
		long now = System.nanoTime();
		if (timing && lastFrame != 0 && frames < gap.length) {
			gap[frames] = now - lastFrame;
			work[frames] = Minecraft.getInstance().getFrameTimeNs();
			frames++;
			sightChecked += Sight.lastChecked;
			sightSkipped += Sight.lastSkipped;
			keptDrawn += Retained.lastDrawn;
		}
		lastFrame = now;
	}

	private static double since(long start) {
		return (System.nanoTime() - start) / 1e9;
	}

	private static void go(Stage next) {
		stage = next;
		stageStart = System.nanoTime();
	}

	private static void tick(Minecraft mc) {
		if (!active || stage == Stage.DONE) {
			return;
		}
		try {
			step(mc);
		} catch (Throwable t) {
			LOG.error("FPS check failed at {}", stage, t);
			errors.add("At " + stage + (viewIndex >= 0 && viewIndex < views.size() ? " (" + views.get(viewIndex).name() + ")" : "") + ": " + t);
			if (stage == Stage.FINISH || stage == Stage.CLOSE) {
				go(Stage.DONE);
				mc.stop();
			} else {
				go(Stage.FINISH);
			}
		}
	}

	private static void step(Minecraft mc) {
		if (stage.ordinal() >= Stage.LOADING.ordinal()) {
			mc.getFramerateLimitTracker().onInputReceived(); // never drop to the idle frame rate
			if (mc.gui.screen() instanceof PauseScreen) {
				mc.gui.setScreen(null);
			}
			if (stage.ordinal() < Stage.FINISH.ordinal() && since(runStart) > RUN_MAX) {
				errors.add("Ran out of time (" + RUN_MAX / 60 + " minutes) at view " + viewIndex);
				timing = false;
				go(Stage.FINISH);
			}
		}
		switch (stage) {
			case WAIT_WORLD -> {
				if (mc.level != null && mc.player != null && mc.getSingleplayerServer() != null) {
					// only ever in the throwaway copy the bat makes: never in a world someone plays in
					Path world = mc.getSingleplayerServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).toAbsolutePath().normalize();
					if (world.getFileName() == null || !WORLD.equals(world.getFileName().toString())) {
						LOG.warn("Not the check's world ({}): the FPS check stays off", world);
						active = false;
						return;
					}
					runStart = System.nanoTime();
					wallStart = System.currentTimeMillis();
					applySettings(mc);
					go(Stage.LOADING);
				}
			}
			case LOADING -> {
				// let the world finish coming in around where he left off
				if (since(stageStart) > 15 && (mc.levelRenderer.hasRenderedAllSections() || since(stageStart) > 60)) {
					startRecording();
					serverDone.set(false);
					IntegratedServer server = mc.getSingleplayerServer();
					server.execute(() -> {
						try {
							setup(server);
						} catch (Throwable t) {
							setupNote += "Setup failed: " + t + "\n";
							LOG.error("Setup failed", t);
						}
						serverDone.set(true);
					});
					go(Stage.SETUP);
				}
			}
			case SETUP -> {
				if (serverDone.get()) {
					header(mc);
					if (views.isEmpty()) {
						errors.add("No views (no tower found?)");
						go(Stage.FINISH);
					} else {
						go(Stage.NEXT);
					}
				}
			}
			case NEXT -> {
				viewIndex++;
				if (viewIndex >= views.size()) {
					go(Stage.FINISH);
					return;
				}
				FpsCheckPlan.View v = views.get(viewIndex);
				// the "sight_off" views: what can't be seen drawn anyway and no shapes kept on the card, as before r186
				Sight.enabled = v.sight();
				Retained.enabled = v.sight();
				// his frame cap and vsync for the views that show what he sees; otherwise off, to measure the work
				mc.options.enableVsync().set(v.vsync() && origVsync);
				mc.options.framerateLimit().set(v.vsync() ? origFps : net.minecraft.client.Options.UNLIMITED_FRAMERATE_CUTOFF);
				IntegratedServer server = mc.getSingleplayerServer();
				serverDone.set(false);
				server.execute(() -> {
					try {
						place(server, v);
					} catch (Throwable t) {
						errors.add("Placing " + v.name() + ": " + t);
					}
					serverDone.set(true);
				});
				go(Stage.SETTLE);
			}
			case SETTLE -> {
				FpsCheckPlan.View v = views.get(viewIndex);
				boolean shipThere = shipOnClient(mc) == v.ship();
				if (serverDone.get() && since(stageStart) > SETTLE_MIN
						&& ((mc.levelRenderer.hasRenderedAllSections() && shipThere) || since(stageStart) > SETTLE_MAX)) {
					frames = 0;
					lastFrame = 0;
					sightChecked = sightSkipped = keptDrawn = 0;
					measureStartMs = System.currentTimeMillis();
					timing = true;
					go(Stage.MEASURE);
				}
			}
			case MEASURE -> {
				FpsCheckPlan.View v = views.get(viewIndex);
				if (v.walking() && mc.player != null) {
					walkTo(mc, v.at(since(stageStart)));
				}
				if (since(stageStart) >= (v.walking() ? v.seconds() : MEASURE)) {
					timing = false;
					windows.add(new FpsCheckBreakdown.Window(v.name(), measureStartMs, System.currentTimeMillis(), frames, measuredMs()));
					record(mc, views.get(viewIndex));
					Screenshot.grab(mc.gameDirectory, String.format(Locale.ROOT, "fpscheck_%02d_%s.png", viewIndex + 1, views.get(viewIndex).name()),
							mc.gameRenderer.mainRenderTarget(), 1, message -> { });
					go(Stage.SHOT);
				}
			}
			case SHOT -> {
				if (since(stageStart) > 1.5) {
					if (views.get(viewIndex).profile() && !mc.getMetricsRecorder().isRecording()) {
						mc.debugClientMetricsStart(message -> note("  profile: " + message.getString()));
						go(Stage.METRICS);
					} else {
						go(Stage.NEXT);
					}
				}
			}
			case METRICS -> {
				if (since(stageStart) > 3 && !mc.getMetricsRecorder().isRecording() || since(stageStart) > 20) {
					go(Stage.NEXT);
				}
			}
			case FINISH -> {
				timing = false;
				Sight.enabled = true;
				Retained.enabled = true;
				stopRecording();
				IntegratedServer server = mc.getSingleplayerServer();
				if (server != null && shipId != null) {
					server.execute(() -> removeShips(server.overworld(), null));
				}
				go(Stage.CLOSE);
			}
			case CLOSE -> {
				if (since(stageStart) > 6) { // let the last profile and screenshot finish writing
					copyProfiles(mc);
					breakdown();
					footer();
					restoreSettings(mc);
					go(Stage.DONE);
					LOG.info("Done; closing the game");
					mc.stop();
				}
			}
			default -> {
			}
		}
	}

	// ------------------------------------------------------------------ settings

	private static void applySettings(Minecraft mc) {
		var o = mc.options;
		origVsync = o.enableVsync().get();
		origFps = o.framerateLimit().get();
		origIdle = o.inactivityFpsLimit().get();
		origPause = o.pauseOnLostFocus;
		origFullscreen = o.fullscreen().get();
		settingsChanged = true;
		o.enableVsync().set(false);
		o.framerateLimit().set(net.minecraft.client.Options.UNLIMITED_FRAMERATE_CUTOFF);
		o.inactivityFpsLimit().set(InactivityFpsLimit.MINIMIZED);
		o.pauseOnLostFocus = false;
		try {
			long window = mc.getWindow().handle();
			int primary = org.lwjgl.sdl.SDLVideo.SDL_GetPrimaryDisplay();
			if (org.lwjgl.sdl.SDLVideo.SDL_GetDisplayForWindow(window) != primary) {
				try (var rect = org.lwjgl.sdl.SDL_Rect.malloc()) {
					if (org.lwjgl.sdl.SDLVideo.SDL_GetDisplayBounds(primary, rect)) {
						org.lwjgl.sdl.SDLVideo.SDL_SetWindowPosition(window, rect.x() + 60, rect.y() + 60);
					}
				}
			}
			// his main screen filled, as he plays (r185's check ran in an 854 x 480 window: maximizing alone didn't
			// take); fullscreen stays fullscreen if that's how he plays
			if (!origFullscreen) {
				try (var rect = org.lwjgl.sdl.SDL_Rect.malloc()) {
					if (org.lwjgl.sdl.SDLVideo.SDL_GetDisplayUsableBounds(primary, rect)) {
						org.lwjgl.sdl.SDLVideo.SDL_RestoreWindow(window);
						org.lwjgl.sdl.SDLVideo.SDL_SetWindowPosition(window, rect.x(), rect.y() + 32);
						org.lwjgl.sdl.SDLVideo.SDL_SetWindowSize(window, rect.w(), rect.h() - 32);
					}
				}
				org.lwjgl.sdl.SDLVideo.SDL_MaximizeWindow(window);
				org.lwjgl.sdl.SDLVideo.SDL_SyncWindow(window);
			}
		} catch (Throwable t) {
			errors.add("Could not size the window: " + t);
		}
	}

	private static void restoreSettings(Minecraft mc) {
		if (!settingsChanged) {
			return;
		}
		var o = mc.options;
		o.enableVsync().set(origVsync);
		o.framerateLimit().set(origFps);
		o.inactivityFpsLimit().set(origIdle);
		o.pauseOnLostFocus = origPause;
		settingsChanged = false;
	}

	// ------------------------------------------------------------------ the views (server thread)

	private static void setup(IntegratedServer server) {
		ServerLevel level = server.overworld();
		List<ServerPlayer> players = server.getPlayerList().getPlayers();
		Vec3 me = players.isEmpty() ? Vec3.ZERO : players.get(0).position();
		FpsCheckPlan.Plan plan = FpsCheckPlan.plan(level, me, TowerRegistry.get(level).allCores());
		setupNote = plan.note();
		if (plan.views().isEmpty()) {
			return;
		}
		command(server, "gamemode spectator @a");
		command(server, "weather clear 100000");
		command(server, "time set 6000");
		removeShips(level, null);
		shipLand = plan.land();
		shipYaw = plan.yaw();
		views.addAll(plan.views());
		// the hangars full (his JFR had drones the check's world didn't): before and after
		StringBuilder fleet = new StringBuilder("Drones before: ").append(drones(level)).append("; ");
		int added = 0;
		for (var terminal : terminals(level)) {
			added += terminal.fillForCheck(400);
		}
		fleet.append("added ").append(added).append(" to fill the hangars; now ").append(drones(level));
		setupNote += fleet + "\n";
	}

	/** The drone terminals of the towers near where the ship lands (the tower the check uses). */
	private static List<io.github.jimbozoomer.jugcraft.drone.DroneTerminalBlockEntity> terminals(ServerLevel level) {
		List<io.github.jimbozoomer.jugcraft.drone.DroneTerminalBlockEntity> out = new ArrayList<>();
		for (net.minecraft.core.BlockPos core : TowerRegistry.get(level).allCores()) {
			if (shipLand != null && core.distToCenterSqr(shipLand) > 300 * 300) {
				continue;
			}
			if (level.getBlockEntity(core) instanceof io.github.jimbozoomer.jugcraft.tower.TowerCoreBlockEntity tower && tower.terminal() != null) {
				out.add(tower.terminal());
			}
		}
		return out;
	}

	/** "N docked, M flying" over the terminals. */
	private static String drones(ServerLevel level) {
		int all = 0, flying = 0;
		for (var terminal : terminals(level)) {
			all += terminal.fleet().size();
			flying += terminal.scheduler().activeFlights();
		}
		return (all - flying) + " docked, " + flying + " flying";
	}

	private static void place(IntegratedServer server, FpsCheckPlan.View v) {
		ServerLevel level = server.overworld();
		command(server, "time set " + v.time());
		removeShips(level, v.ship() ? shipId : null);
		if (v.ship() && (shipId == null || level.getEntity(shipId) == null)) {
			CrewShip ship = JugcraftTower.CREW_SHIP.create(level, EntitySpawnReason.TRIGGERED);
			ship.park(shipLand, shipYaw, level.getGameTime());
			level.addFreshEntity(ship);
			shipId = ship.getUUID();
		}
		if (!v.ship()) {
			shipId = null;
		}
		// a few more drones out each view (round trips over the ground round the tower, carrying nothing)
		net.minecraft.util.RandomSource random = level.getRandom();
		for (var terminal : terminals(level)) {
			net.minecraft.core.BlockPos home = terminal.getBlockPos();
			double a = random.nextDouble() * Math.PI * 2, r = 25 + random.nextDouble() * 35;
			int x = home.getX() + (int) (Math.cos(a) * r), z = home.getZ() + (int) (Math.sin(a) * r);
			net.minecraft.core.BlockPos to = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,
					new net.minecraft.core.BlockPos(x, 0, z));
			terminal.flyForCheck(level, to, 5);
		}
		droneNote = drones(level);
		command(server, String.format(Locale.ROOT, "tp @a %.3f %.3f %.3f facing %.3f %.3f %.3f",
				v.cam().x, v.cam().y, v.cam().z, v.look().x, v.look().y, v.look().z));
	}

	/** Puts the camera at a walk's point (feet there, as the views' teleports do), looking at its look point. */
	private static void walkTo(Minecraft mc, Vec3[] at) {
		Vec3 cam = at[0], eye = cam.add(0, mc.player.getEyeHeight(), 0), d = at[1].subtract(eye);
		float yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
		float pitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)));
		mc.player.snapTo(cam.x, cam.y, cam.z, yaw, pitch);
		mc.player.setDeltaMovement(Vec3.ZERO);
	}

	/** The measured frames' total time (ms). */
	private static double measuredMs() {
		long total = 0;
		for (int i = 0; i < frames; i++) {
			total += gap[i];
		}
		return total / 1e6;
	}

	/** Takes away every drop ship but {@code keep}. */
	private static void removeShips(ServerLevel level, UUID keep) {
		List<Entity> gone = new ArrayList<>();
		for (Entity e : level.getAllEntities()) {
			if (e instanceof CrewShip && !e.getUUID().equals(keep)) {
				gone.add(e);
			}
		}
		gone.forEach(Entity::discard);
	}

	private static boolean shipOnClient(Minecraft mc) {
		if (mc.level == null) {
			return false;
		}
		for (Entity e : mc.level.entitiesForRendering()) {
			if (e instanceof CrewShip) {
				return true;
			}
		}
		return false;
	}

	private static void command(IntegratedServer server, String command) {
		server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), command);
	}

	// ------------------------------------------------------------------ results

	private static void header(Minecraft mc) {
		var o = mc.options;
		line("Jugcraft frame rate check, " + LocalDateTime.now().withNano(0));
		line("");
		try {
			var info = com.mojang.blaze3d.systems.RenderSystem.getDevice().getDeviceInfo();
			line("GPU: " + info.name() + " (" + info.vendorName() + "), driver " + info.driverInfo());
		} catch (Throwable t) {
			line("GPU: unknown (" + t + ")");
		}
		line("CPU threads: " + Runtime.getRuntime().availableProcessors() + "; Java " + System.getProperty("java.version")
				+ "; max heap " + Runtime.getRuntime().maxMemory() / (1 << 20) + " MB");
		line("Window: " + mc.getWindow().getWidth() + " x " + mc.getWindow().getHeight());
		line("His settings: render distance " + o.renderDistance().get() + ", simulation " + o.simulationDistance().get()
				+ ", graphics " + o.graphicsPreset().get() + ", entity distance " + o.entityDistanceScaling().get()
				+ ", frame cap " + origFps + ", vsync " + origVsync + " (cap and vsync off for the check)");
		line("Baked fixtures: " + !"false".equals(System.getProperty("jugcraft.bakedFixtures")));
		line("");
		for (String s : setupNote.split("\n")) {
			if (!s.isBlank()) {
				line(s);
			}
		}
		line("");
		line("Sight (r186: what can't be seen is skipped) and kept shapes (r186: drones at rest drawn from the graphics card) are on except");
		line("in the views marked sight_off (as before r186); vsync and the frame cap are his only in the views marked his_vsync.");
		line("\"skipped\" is how many of the always-drawn block entities Sight left out a frame, of how many; \"kept\" how many shapes a");
		line("frame were drawn from the card.");
		line("");
		line(String.format(Locale.ROOT, "%-30s %6s %7s %7s %7s %7s %7s %8s %7s %6s %11s %10s %5s  %s", "view", "frames", "avg fps", "1% low",
				"med ms", "p95 ms", "p99 ms", "worst ms", "work ms", "mspt", "window", "skipped", "kept", "drones"));
		flush();
		try {
			Files.writeString(out.resolve("frames.csv"), "view,frame,gap_ms,work_ms\n", StandardCharsets.UTF_8);
		} catch (IOException e) {
			errors.add("frames.csv: " + e);
		}
	}

	private static void record(Minecraft mc, FpsCheckPlan.View v) {
		int n = frames;
		if (n < 2) {
			line(String.format(Locale.ROOT, "%-30s %6d  (too few frames)", v.name(), n));
			flush();
			return;
		}
		long[] g = Arrays.copyOf(gap, n);
		long[] w = Arrays.copyOf(work, n);
		long total = 0;
		for (long x : g) {
			total += x;
		}
		long[] sorted = g.clone();
		Arrays.sort(sorted);
		long[] ws = w.clone();
		Arrays.sort(ws);
		double avgFps = n / (total / 1e9);
		// 1% low: the frame rate over the slowest 1% of frames
		int k = Math.max(1, n / 100);
		long slow = 0;
		for (int i = n - k; i < n; i++) {
			slow += sorted[i];
		}
		double low = k / (slow / 1e9);
		IntegratedServer server = mc.getSingleplayerServer();
		double mspt = server == null ? -1 : server.getAverageTickTimeNanos() / 1e6;
		String window = mc.getWindow().getWidth() + "x" + mc.getWindow().getHeight();
		String skipped = sightChecked == 0 ? "-" : String.format(Locale.ROOT, "%d/%d", Math.round((double) sightSkipped / n), Math.round((double) sightChecked / n));
		line(String.format(Locale.ROOT, "%-30s %6d %7.1f %7.1f %7.2f %7.2f %7.2f %8.1f %7.2f %6.1f %11s %10s %5d  %s", v.name(), n, avgFps, low,
				ms(sorted[n / 2]), ms(sorted[(int) (n * 0.95)]), ms(sorted[(int) (n * 0.99)]), ms(sorted[n - 1]), ms(ws[n / 2]), mspt, window, skipped,
				Math.round((double) keptDrawn / n), droneNote));
		flush();
		StringBuilder csv = new StringBuilder(n * 24);
		for (int i = 0; i < n; i++) {
			csv.append(v.name()).append(',').append(i).append(',').append(String.format(Locale.ROOT, "%.3f,%.3f", ms(g[i]), ms(w[i]))).append('\n');
		}
		try {
			Files.writeString(out.resolve("frames.csv"), csv, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
			Files.writeString(out.resolve("views.txt"), String.format(Locale.ROOT, "%s cam %s look %s ship %s time %d sight %s vsync %s walk %s start %d end %d%n",
					v.name(), FpsCheckPlan.fmt(v.cam()), FpsCheckPlan.fmt(v.look()), v.ship(), v.time(), v.sight(), v.vsync(), v.walking(), measureStartMs,
					System.currentTimeMillis()),
					StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
		} catch (IOException e) {
			errors.add("frames.csv: " + e);
		}
	}

	private static double ms(long ns) {
		return ns / 1e6;
	}

	private static void note(String s) {
		synchronized (report) {
			line(s);
		}
		flush();
	}

	private static void line(String s) {
		synchronized (report) {
			report.append(s).append('\n');
		}
	}

	private static void flush() {
		try (Writer w = Files.newBufferedWriter(out.resolve("report.txt"), StandardCharsets.UTF_8)) {
			synchronized (report) {
				w.write(report.toString());
			}
		} catch (IOException e) {
			LOG.warn("Could not write the report", e);
		}
	}

	/** Each view's renderers' milliseconds a frame, from the recording (appended to the report and renderers.csv). */
	private static void breakdown() {
		Path jfr = out.resolve("profile.jfr");
		if (!Files.exists(jfr) || windows.isEmpty()) {
			return;
		}
		try {
			List<String> lines = FpsCheckBreakdown.write(jfr, windows, out.resolve("renderers.csv"));
			line("");
			lines.forEach(FpsCheck::line);
			flush();
		} catch (Throwable t) {
			errors.add("Breakdown from the recording: " + t);
		}
	}

	private static void footer() {
		line("");
		line(String.format(Locale.ROOT, "Run took %.1f minutes.", since(runStart) / 60));
		if (errors.isEmpty()) {
			line("No errors.");
		} else {
			line("Errors:");
			errors.forEach(e -> line("  " + e));
		}
		flush();
	}

	private static void copyProfiles(Minecraft mc) {
		try {
			Path from = mc.gameDirectory.toPath().resolve("debug").resolve("profiling");
			if (Files.isDirectory(from)) {
				try (var list = Files.list(from)) {
					for (Path p : (Iterable<Path>) list::iterator) {
						if (Files.getLastModifiedTime(p).toMillis() >= wallStart) {
							Files.copy(p, out.resolve("profiles").resolve(p.getFileName()), StandardCopyOption.REPLACE_EXISTING);
						}
					}
				}
			}
			Path shots = mc.gameDirectory.toPath().resolve("screenshots");
			if (Files.isDirectory(shots)) {
				try (var list = Files.list(shots)) {
					for (Path p : (Iterable<Path>) list::iterator) {
						if (p.getFileName().toString().startsWith("fpscheck_")) {
							Files.move(p, out.resolve(p.getFileName()), StandardCopyOption.REPLACE_EXISTING);
						}
					}
				}
			}
		} catch (IOException e) {
			errors.add("Copying profiles and screenshots: " + e);
		}
	}

	// ------------------------------------------------------------------ Java Flight Recorder

	private static void startRecording() {
		try {
			jdk.jfr.Recording r = new jdk.jfr.Recording(jdk.jfr.Configuration.getConfiguration("profile"));
			r.setName("jugcraft-fps-check");
			r.setToDisk(true);
			r.start();
			recording = r;
		} catch (Throwable t) {
			errors.add("Flight recorder did not start: " + t);
		}
	}

	private static void stopRecording() {
		if (recording instanceof jdk.jfr.Recording r) {
			try {
				r.stop();
				r.dump(out.resolve("profile.jfr"));
				r.close();
			} catch (Throwable t) {
				errors.add("Flight recorder did not save: " + t);
			}
			recording = null;
		}
	}
}
