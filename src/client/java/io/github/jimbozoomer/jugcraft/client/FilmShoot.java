package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.tower.TowerRegistry;
import io.github.jimbozoomer.jugcraft.tower.squad.JugcraftSquads;
import io.github.jimbozoomer.jugcraft.tower.squad.SquadFilm;
import io.github.jimbozoomer.jugcraft.tower.squad.SquadFilmCamera;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.InactivityFpsLimit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The film camera (Justin, 11 November 2026: "Make a mp4 clip of the video and audio of a 7 drop pod drop to them forming
 * up and all the pods blowing up ... have them also fight a warden, and have it show off the hole in its body as they
 * take it down"; "Real game footage but you do it all, I'm not on my pc right now"). Dormant unless the game folder
 * holds {@code jugcraft-film.txt} when the game starts (29-film.bat writes it, copies his world to the throwaway
 * {@code JugcraftFilm} and starts the game straight into it, with OpenAL writing the game's sound to a wave file); the
 * file is deleted as soon as it's read, and the film runs in no world but that copy.
 *
 * <p>In the world it fills his main screen, hides the HUD and name tags, turns the music off and the other sounds up to
 * full (his settings are put back before the game closes), finds open ground near his tower and sets the scene there
 * (tower/squad/SquadFilm: the server's half), plays a marker sound to line the sound up with the pictures, and rolls
 * (client/FilmCapture): a squad of seven called down, the camera on the ground looking up as the flare shows and the pods
 * come down through the sky after it, following them in; beside the landing as the hatches blow and the soldiers walk
 * out to hold their spot, the pods blowing apart behind them; on the squad formed up; low by the Warden as it comes up
 * out of the ground and roars, the bolts burning through it; round behind it as it goes for them, its back to the camera
 * so the holes show the light through; on it as it falls; and up over the squad. Then it cuts, plays the marker again,
 * writes everything to {@code film/} in the game folder (the frames, frames.csv, sounds.csv, scene.txt) and closes the
 * game. The camera is the player in spectator, moved each tick by springs toward where each shot wants it, so it never
 * jumps.
 */
public final class FilmShoot {
	private static final Logger LOG = LoggerFactory.getLogger("Jugcraft film");
	private static final String TRIGGER = "jugcraft-film.txt";
	/** The world folder the bat copies his world to; the film runs in no other. */
	private static final String WORLD = "JugcraftFilm";
	/** Seconds: the whole run's limit, and the action's. */
	private static final double RUN_MAX = 15 * 60, ACTION_MAX = 4 * 60;
	/** The time of day the scene is shot at, and the screen's frame cap while it rolls. */
	private static final int TIME = 7000, FRAME_CAP = 120;

	private enum Stage { WAIT_WORLD, LOADING, SETUP, SETTLE, MARK, ROLL, ACTION, CUT, WRAP, DONE }

	private static boolean active;
	private static Path out;
	private static Stage stage = Stage.WAIT_WORLD;
	private static long stageStart, runStart;
	private static volatile SquadFilm scene;
	private static volatile UUID player;
	private static volatile boolean wantLaunch, wantWarden;
	private static final AtomicBoolean serverReady = new AtomicBoolean();
	private static volatile String setupNote = "";
	private static volatile boolean serverFailed;
	private static final List<String> errors = Collections.synchronizedList(new ArrayList<>());
	private static final List<String> timeline = new ArrayList<>();
	private static long markNs, endMarkNs;
	private static String audioDevice = "?", window = "?";

	/** The camera (made with the scene). */
	private static volatile SquadFilmCamera cam;

	// his settings, put back at the end
	private static boolean settingsChanged, origVsync, origPause, origBob, origHud;
	private static int origFps;
	private static double origFovEffect;
	private static InactivityFpsLimit origIdle;
	private static final Map<SoundSource, Double> origVolumes = new EnumMap<>(SoundSource.class);
	private static boolean fullscreened, origExclusive;

	private FilmShoot() {
	}

	/** Called from the client initializer: arms the film if its file is there (and deletes the file). */
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
		out = game.resolve("film");
		try {
			if (Files.isDirectory(out)) {
				try (var old = Files.walk(out)) {
					old.sorted(java.util.Comparator.reverseOrder()).filter(p -> !p.equals(out)).forEach(p -> p.toFile().delete());
				}
			}
			Files.createDirectories(out);
		} catch (IOException e) {
			LOG.warn("Could not make {}", out, e);
		}
		LOG.info("Armed: the film goes to {}", out);
		ClientTickEvents.END_CLIENT_TICK.register(FilmShoot::tick);
		ServerTickEvents.END_SERVER_TICK.register(FilmShoot::serverTick);
	}

	private static double since(long start) {
		return (System.nanoTime() - start) / 1e9;
	}

	private static void go(Stage next) {
		stage = next;
		stageStart = System.nanoTime();
		LOG.info("Stage {}", next);
	}

	private static void tick(Minecraft mc) {
		if (!active || stage == Stage.DONE) {
			return;
		}
		try {
			step(mc);
		} catch (Throwable t) {
			LOG.error("Film failed at {}", stage, t);
			errors.add("At " + stage + "/" + (cam == null ? "-" : cam.shot) + ": " + t);
			if (stage.ordinal() >= Stage.WRAP.ordinal()) {
				go(Stage.DONE);
				mc.stop();
			} else {
				go(Stage.CUT);
			}
		}
	}

	private static void step(Minecraft mc) {
		if (stage.ordinal() >= Stage.LOADING.ordinal()) {
			mc.getFramerateLimitTracker().onInputReceived(); // never the idle frame rate
			if (mc.gui.screen() instanceof net.minecraft.client.gui.screens.PauseScreen) {
				mc.gui.setScreen(null);
			}
			if (stage.ordinal() < Stage.CUT.ordinal() && since(runStart) > RUN_MAX) {
				errors.add("Ran out of time (" + RUN_MAX / 60 + " minutes) at " + stage);
				go(Stage.CUT);
			}
		}
		if (stage.ordinal() >= Stage.SETTLE.ordinal() && stage.ordinal() <= Stage.CUT.ordinal()) {
			camera(mc);
		}
		switch (stage) {
			case WAIT_WORLD -> {
				if (mc.level != null && mc.player != null && mc.getSingleplayerServer() != null) {
					Path world = mc.getSingleplayerServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).toAbsolutePath().normalize();
					if (world.getFileName() == null || !WORLD.equals(world.getFileName().toString())) {
						LOG.warn("Not the film's world ({}): the film stays off", world);
						active = false;
						return;
					}
					runStart = System.nanoTime();
					applySettings(mc);
					go(Stage.LOADING);
				}
			}
			case LOADING -> {
				if (since(stageStart) > 12 && (mc.levelRenderer.hasRenderedAllSections() || since(stageStart) > 60)) {
					IntegratedServer server = mc.getSingleplayerServer();
					serverReady.set(false);
					server.execute(() -> {
						try {
							setup(server);
						} catch (Throwable t) {
							errors.add("Setup failed: " + t);
							LOG.error("Setup failed", t);
						}
						serverReady.set(true);
					});
					go(Stage.SETUP);
				}
			}
			case SETUP -> {
				if (serverReady.get()) {
					if (scene == null) {
						errors.add("No scene: " + setupNote);
						go(Stage.WRAP);
					} else {
						cam = new SquadFilmCamera(scene);
						go(Stage.SETTLE);
					}
				}
			}
			case SETTLE -> {
				window = mc.getWindow().getWidth() + " x " + mc.getWindow().getHeight();
				if (since(stageStart) > 10 && mc.levelRenderer.hasRenderedAllSections() || since(stageStart) > 45) {
					audioDevice = audioDevice();
					FilmCapture.listen(out);
					mc.getMusicManager().stopPlaying();
					markNs = System.nanoTime();
					mc.getSoundManager().play(SimpleSoundInstance.forUI(JugcraftSquads.MARK, 1.0F, 1.0F));
					go(Stage.MARK);
				}
			}
			case MARK -> {
				if (since(stageStart) > 1.5) {
					FilmCapture.start();
					go(Stage.ROLL);
				}
			}
			case ROLL -> {
				if (since(stageStart) > 2.0) {
					wantLaunch = true;
					go(Stage.ACTION);
				}
			}
			case ACTION -> {
				if (cam != null && cam.shot == SquadFilmCamera.Shot.END || serverFailed) {
					go(Stage.CUT);
				} else if (since(stageStart) > ACTION_MAX) {
					errors.add("The action ran past " + ACTION_MAX + " s (at " + (cam == null ? "?" : cam.shot) + ")");
					go(Stage.CUT);
				}
			}
			case CUT -> {
				FilmCapture.stop();
				if (endMarkNs == 0 && since(stageStart) > 0.5 && markNs != 0) {
					endMarkNs = System.nanoTime();
					mc.getSoundManager().play(SimpleSoundInstance.forUI(JugcraftSquads.MARK, 1.0F, 1.0F));
				}
				if (since(stageStart) > 2.5) {
					go(Stage.WRAP);
				}
			}
			case WRAP -> {
				if (FilmCapture.drained() || since(stageStart) > 60) {
					List<String> capture = markNs == 0 ? List.of("Nothing was rolled.") : FilmCapture.finish();
					writeReport(capture);
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

	// ------------------------------------------------------------------ the server's half

	private static void setup(IntegratedServer server) {
		ServerLevel level = server.overworld();
		List<ServerPlayer> players = server.getPlayerList().getPlayers();
		if (players.isEmpty()) {
			errors.add("No player");
			return;
		}
		ServerPlayer p = players.get(0);
		player = p.getUUID();
		StringBuilder note = new StringBuilder();
		Vec3 near = p.position();
		SquadFilm s = SquadFilm.find(level, near, TowerRegistry.get(level).allCores(), note);
		setupNote = note.toString();
		if (s == null) {
			return;
		}
		GameRules rules = level.getGameRules();
		rules.set(GameRules.ADVANCE_TIME, false, server);
		rules.set(GameRules.ADVANCE_WEATHER, false, server);
		rules.set(GameRules.SPAWN_MONSTERS, false, server);
		command(server, "time set " + TIME);
		command(server, "weather clear 1000000");
		command(server, "gamemode spectator @a");
		s.clear(level, player);
		Vec3 feet = s.camSky.subtract(0, 1.62, 0), flare = new SquadFilmCamera(s).flare();
		command(server, String.format(Locale.ROOT, "execute in minecraft:overworld run tp @a %.3f %.3f %.3f facing %.3f %.3f %.3f",
				feet.x, feet.y, feet.z, flare.x, flare.y, flare.z));
		scene = s;
	}

	private static void serverTick(MinecraftServer server) {
		SquadFilm s = scene;
		if (!active || s == null || serverFailed) {
			return;
		}
		try {
			ServerLevel level = server.overworld();
			if (wantLaunch && s.launched < 0) {
				s.launch(level, player);
			}
			if (wantWarden && s.wardenUp < 0) {
				s.raiseWarden(level);
			}
			s.tick(level);
		} catch (Throwable t) {
			serverFailed = true;
			errors.add("The scene failed: " + t);
			LOG.error("The scene failed", t);
		}
	}

	private static void command(MinecraftServer server, String command) {
		server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), command);
	}

	/** Where the flare hangs: over the drop, off toward the tower. */
	private static Vec3 flare(SquadFilm s) {
		return s.mark.add(s.out.scale(45)).add(0, 210, 0);
	}

	// ------------------------------------------------------------------ the camera

	/** The camera a tick on (tower/squad/SquadFilmCamera works out where), the player put there. */
	private static void camera(Minecraft mc) {
		SquadFilmCamera c = cam;
		LocalPlayer p = mc.player;
		ClientLevel level = mc.level;
		if (c == null || p == null || level == null) {
			return;
		}
		long now = level.getGameTime();
		int cues = c.cues.size();
		c.step(level, now, stage == Stage.ACTION);
		for (int i = cues; i < c.cues.size(); i++) {
			timeline.add(System.nanoTime() + " " + c.cues.get(i));
			LOG.info("Shot {}", c.cues.get(i));
		}
		if (c.wantWarden) {
			wantWarden = true;
		}
		p.setPos(c.pos.x, c.pos.y - p.getEyeHeight(), c.pos.z);
		p.setYRot(c.yaw);
		p.setXRot(c.pitch);
		p.setYHeadRot(c.yaw);
		p.setDeltaMovement(Vec3.ZERO);
		FilmCapture.tag = c.tag(now);
	}

	// ------------------------------------------------------------------ settings

	private static void applySettings(Minecraft mc) {
		var o = mc.options;
		origVsync = o.enableVsync().get();
		origFps = o.framerateLimit().get();
		origIdle = o.inactivityFpsLimit().get();
		origPause = o.pauseOnLostFocus;
		origBob = o.bobView().get();
		origFovEffect = o.fovEffectScale().get();
		origHud = mc.gui.hud.isHidden();
		for (SoundSource source : SoundSource.values()) {
			origVolumes.put(source, o.getSoundSourceOptionInstance(source).get());
		}
		settingsChanged = true;
		o.enableVsync().set(false);
		o.framerateLimit().set(FRAME_CAP);
		o.inactivityFpsLimit().set(InactivityFpsLimit.MINIMIZED);
		o.pauseOnLostFocus = false;
		o.bobView().set(false);
		o.fovEffectScale().set(0.0);
		if (!mc.gui.hud.isHidden()) {
			mc.gui.hud.toggle();                           // no HUD, no name tags
		}
		Map<SoundSource, Double> film = new EnumMap<>(SoundSource.class);
		film.put(SoundSource.MASTER, 1.0);
		film.put(SoundSource.MUSIC, 0.0);
		film.put(SoundSource.RECORDS, 0.0);
		film.put(SoundSource.WEATHER, 0.4);
		film.put(SoundSource.BLOCKS, 1.0);
		film.put(SoundSource.HOSTILE, 1.0);
		film.put(SoundSource.NEUTRAL, 1.0);
		film.put(SoundSource.PLAYERS, 1.0);
		film.put(SoundSource.AMBIENT, 0.5);
		film.put(SoundSource.VOICE, 0.0);
		film.put(SoundSource.UI, 1.0);
		film.forEach((source, v) -> o.getSoundSourceOptionInstance(source).set(v));
		mc.getMusicManager().stopPlaying();
		try {
			// his main screen, filled (borderless, at its own size)
			long handle = mc.getWindow().handle();
			org.lwjgl.sdl.SDLHints.SDL_SetHint(org.lwjgl.sdl.SDLHints.SDL_HINT_VIDEO_MINIMIZE_ON_FOCUS_LOSS, "0");
			int primary = org.lwjgl.sdl.SDLVideo.SDL_GetPrimaryDisplay();
			if (org.lwjgl.sdl.SDLVideo.SDL_GetDisplayForWindow(handle) != primary) {
				try (var rect = org.lwjgl.sdl.SDL_Rect.malloc()) {
					if (org.lwjgl.sdl.SDLVideo.SDL_GetDisplayBounds(primary, rect)) {
						org.lwjgl.sdl.SDLVideo.SDL_SetWindowPosition(handle, rect.x() + 60, rect.y() + 60);
					}
				}
			}
			// the game's own borderless fullscreen (applied at the next frame, on the screen the window is on)
			origExclusive = mc.getWindow().isExclusiveFullscreen();
			if (!o.fullscreen().get()) {
				mc.getWindow().setExclusiveFullscreen(false);
				mc.getWindow().setFullscreen(true);
				fullscreened = true;
			}
		} catch (Throwable t) {
			errors.add("Could not fill the screen: " + t);
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
		o.bobView().set(origBob);
		o.fovEffectScale().set(origFovEffect);
		origVolumes.forEach((source, v) -> o.getSoundSourceOptionInstance(source).set(v));
		if (mc.gui.hud.isHidden() != origHud) {
			mc.gui.hud.toggle();
		}
		if (fullscreened) {
			try {
				mc.getWindow().setFullscreen(false);
				mc.getWindow().setExclusiveFullscreen(origExclusive);
			} catch (Throwable t) {
				errors.add("Could not put the window back: " + t);
			}
		}
		settingsChanged = false;
	}

	/** The sound device OpenAL opened (the wave writer when the bat set it up). */
	private static String audioDevice() {
		try {
			long context = org.lwjgl.openal.ALC10.alcGetCurrentContext();
			long device = org.lwjgl.openal.ALC10.alcGetContextsDevice(context);
			return org.lwjgl.openal.ALC10.alcGetString(device, org.lwjgl.openal.ALC10.ALC_DEVICE_SPECIFIER);
		} catch (Throwable t) {
			return "unknown (" + t + ")";
		}
	}

	// ------------------------------------------------------------------ the report

	private static void writeReport(List<String> capture) {
		List<String> lines = new ArrayList<>();
		lines.add("Jugcraft film, " + LocalDateTime.now().withNano(0));
		lines.add("Window " + window + "; sound device: " + audioDevice + "; ALSOFT_CONF " + System.getenv("ALSOFT_CONF"));
		lines.add("marker " + markNs + " rolled " + (markNs == 0 ? 0 : FilmCapture.startNs()) + " end marker " + endMarkNs);
		lines.add("");
		for (String s : setupNote.split("\n")) {
			if (!s.isBlank()) {
				lines.add(s);
			}
		}
		lines.add("");
		SquadFilm s = scene;
		if (s != null) {
			lines.add("launched at game tick " + s.launched + "; events (ticks from the launch):");
			s.events().forEach(e -> lines.add("  " + e));
		}
		lines.add("");
		lines.add("Shots (nanos, game tick, shot):");
		timeline.forEach(t -> lines.add("  " + t));
		lines.add("");
		lines.addAll(capture);
		lines.add("");
		lines.add(String.format(Locale.ROOT, "Run took %.1f minutes.", since(runStart) / 60));
		if (errors.isEmpty()) {
			lines.add("No errors.");
		} else {
			lines.add("Errors:");
			synchronized (errors) {
				errors.forEach(e -> lines.add("  " + e));
			}
		}
		try {
			Files.write(out.resolve("scene.txt"), lines, StandardCharsets.UTF_8);
		} catch (IOException e) {
			LOG.warn("Could not write the report", e);
		}
	}
}
