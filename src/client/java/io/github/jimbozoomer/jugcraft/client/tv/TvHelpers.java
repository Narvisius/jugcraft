package io.github.jimbozoomer.jugcraft.client.tv;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import net.fabricmc.loader.api.FabricLoader;
import org.jspecify.annotations.Nullable;

/**
 * The video helper (docs/features/flat-screen-tv.md): YouTube, Rumble and Kick pages aren't videos, so a TV asks yt-dlp
 * (github.com/yt-dlp/yt-dlp) for the page's video and sound streams ({@link Site}: each site's ways of asking). yt-dlp needs a JavaScript engine for YouTube,
 * Deno (github.com/denoland/deno), unless one is already installed. Neither ships with the mod: the first YouTube link
 * this game plays, or the GET HELPER button, downloads both from their GitHub releases into
 * .minecraft/jugcraft/tv-helpers, each checked against the SHA-256 digest GitHub publishes for it. yt-dlp is fetched
 * again when YouTube stops working with the one we have (at most twice a day).
 * <p>Nothing is downloaded but the helper: the streams it finds are played straight from YouTube, a few seconds ahead.
 * It asks first for YouTube's HLS streams (the picture and the sound each cut into short pieces: any length of video
 * plays, and a jump anywhere in it is one piece asked for), then for the big files, played through {@link TvRelay}.
 * What it finds is kept a while, so a screen walked away from and back to, or a playlist's next video, starts at once.
 */
final class TvHelpers {
	private static final String UA = "Jugcraft-TV (Minecraft mod)";
	/**
	 * HLS: the picture (H.264, light to decode, at most 720p) and the sound, each in short pieces. YouTube gives them
	 * apart now (Justin's 27 October 2026 test, 21-tv-jump-test.bat: no 720p HLS with its sound in it any more, so the
	 * old choice fell through to the big files); every piece of them answered at once, 44 minutes and 2 hours in, and
	 * FFmpeg jumped 44 minutes into the picture's in a tenth of a second.
	 */
	private static final String HLS = "bv*[protocol^=m3u8][height<=720][vcodec^=avc1]+ba[protocol^=m3u8]"
			+ "/b[protocol^=m3u8][height<=720][vcodec^=avc1]"
			+ "/bv*[protocol^=m3u8][height<=720][vcodec!^=av01]+ba[protocol^=m3u8]/b[protocol^=m3u8][height<=720]";
	/** DASH: H.264 at most 720p with its sound, else any but AV1. */
	private static final String DASH = "bv*[height<=720][vcodec^=avc1][protocol=https]+ba[protocol=https]"
			+ "/bv*[height<=720][vcodec!^=av01][protocol=https]+ba[protocol=https]";
	/** A big file with picture and sound in it (at most 720p, else any). */
	private static final String FILE = "b[height<=720][protocol=https]/b[protocol=https]";
	/**
	 * One way of asking YouTube for a video's streams: which of YouTube's apps yt-dlp asks as ({@code clients}), which
	 * streams ({@code format}), and whether a stream sent as one big file is played through {@link TvRelay} (fetched in
	 * pieces as yt-dlp would; its address then asked for over IPv4, as Java connects).
	 */
	record Way(String name, @Nullable String clients, String format, boolean relay, List<String> extra) {
		Way(String name, @Nullable String clients, String format, boolean relay) {
			this(name, clients, format, relay, List.of());
		}
	}

	/**
	 * A site whose pages yt-dlp turns into streams: its name as the screen's window shows it and its ways of asking, tried
	 * in turn when the streams won't open (the one that last worked first).
	 */
	record Site(String name, Way[] ways) {
	}

	/**
	 * The ways tried in turn when a video's streams won't open (YouTube refusing them: Justin's 27 October 2026 video got
	 * HTTP 403 on every stream yt-dlp found as YouTube's TV app): HLS first (short pieces, read by FFmpeg straight from
	 * YouTube), then the big files, always through the relay, as YouTube's default apps, Vision Pro, Android and the
	 * embedded player. A big file must never be read straight: asked for "everything from here on" partway in, YouTube
	 * doesn't answer at all (Justin's 27 October 2026 test: 40 s and nothing, where a 10 MB piece came in a fifth of a
	 * second), and that was the jump 44 minutes in that waited for good. The HLS way has nothing else to fall back on
	 * (no big files in it): with no HLS, the next way.
	 */
	static final Way[] WAYS = {
			new Way("hls", "default,web_safari", HLS, false),
			new Way("relay", "default", DASH + "/" + FILE, true),
			new Way("visionos", "visionos", DASH + "/" + FILE, true),
			new Way("android", "android", "18/" + FILE, true),
			new Way("embedded", "web_embedded", DASH + "/" + FILE, true)};
	/**
	 * Rumble and Kick (Justin, 27 October 2026; 22-tv-sites-test.bat): their streams are HLS with the sound in them, at
	 * most 720p here. Rumble's own pages turn yt-dlp away (HTTP 403, its bot check) unless it asks as a browser would
	 * ({@code --impersonate}, which the Windows yt-dlp has built in); its video servers then answer anyone, every piece at
	 * once at any depth (2 h 45 min and 4 h 8 min into a 4.6-hour episode). Kick answers yt-dlp plainly; its live
	 * streams open in a third of a second.
	 */
	private static final String SITE_HLS = "b[height<=720][protocol^=m3u8]/b[height<=720]/b";
	static final Site YOUTUBE = new Site("YouTube", WAYS);
	static final Site RUMBLE = new Site("Rumble", new Way[] {
			new Way("rumble", null, SITE_HLS, false, List.of("--impersonate", "chrome")),
			new Way("rumble as safari", null, SITE_HLS, false, List.of("--impersonate", "safari")),
			new Way("rumble plainly", null, SITE_HLS, false)});
	static final Site KICK = new Site("Kick", new Way[] {
			new Way("kick", null, SITE_HLS, false),
			new Way("kick as chrome", null, SITE_HLS, false, List.of("--impersonate", "chrome"))});
	/** The way that last played each site's video here (by its name): tried first. */
	private static final Map<String, Integer> LAST_GOOD = new java.util.concurrent.ConcurrentHashMap<>();
	/** How long what yt-dlp found is kept (its stream addresses last some hours). */
	private static final long KEEP_MS = 30 * 60 * 1000L;
	private static final Map<String, Kept> KEPT = new java.util.concurrent.ConcurrentHashMap<>();
	private static final java.util.Set<String> FETCHING = java.util.concurrent.ConcurrentHashMap.newKeySet();
	private static final long RECHECK_MS = 12 * 3600 * 1000L;
	private static final HttpClient HTTP = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL)
			.connectTimeout(Duration.ofSeconds(20)).build();

	/** Set when the player has asked for the helper (played a YouTube link, or GET HELPER). */
	static volatile boolean consented;
	/** What the download is doing, for the screens; its progress 0 to 100. */
	static volatile String status = "";
	static volatile int progress;
	static volatile boolean busy;
	static volatile @Nullable String failure;
	private static @Nullable Boolean denoOnPath;

	/**
	 * What yt-dlp found: the video and sound streams (the same when they come together), how to ask for them, the title;
	 * whether they are all HLS (in short pieces) or all big files fetched over https ({@code files}: the relay can take
	 * them).
	 */
	record Resolved(String video, @Nullable String audio, Map<String, String> headers, String title, long durationMs, boolean live,
			boolean hls, boolean files) {
	}

	private record Kept(Resolved r, long at) {
	}

	private TvHelpers() {
	}

	static boolean isYouTube(String url) {
		return site(url) == YOUTUBE;
	}

	/** The site the link is a page of (its videos found by yt-dlp), or null: a stream address played as it is. */
	static @Nullable Site site(String url) {
		String host;
		try {
			host = URI.create(url).getHost();
		} catch (IllegalArgumentException e) {
			return null;
		}
		if (host == null) {
			return null;
		}
		host = host.toLowerCase(Locale.ROOT);
		if (host.equals("youtu.be") || is(host, "youtube.com") || is(host, "youtube-nocookie.com")) {
			return YOUTUBE;
		}
		if (is(host, "rumble.com")) {
			return RUMBLE;
		}
		if (is(host, "kick.com")) {
			return KICK;
		}
		return null;
	}

	private static boolean is(String host, String domain) {
		return host.equals(domain) || host.endsWith("." + domain);
	}

	/** Whether the link needs the helper (a site's page, not a stream). */
	static boolean needsHelper(String url) {
		return site(url) != null;
	}

	/** The way that last played a video of {@code site} here (tried first). */
	static int lastGood(Site site) {
		return Math.min(site.ways().length - 1, LAST_GOOD.getOrDefault(site.name(), 0));
	}

	static void good(Site site, int way) {
		LAST_GOOD.put(site.name(), way);
	}

	static Path dir() {
		return FabricLoader.getInstance().getGameDir().resolve("jugcraft").resolve("tv-helpers");
	}

	private static boolean windows() {
		return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
	}

	private static boolean mac() {
		String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
		return os.contains("mac") || os.contains("darwin");
	}

	private static boolean arm() {
		String a = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
		return a.contains("aarch64") || a.contains("arm64");
	}

	static Path ytdlp() {
		return dir().resolve(windows() ? "yt-dlp.exe" : "yt-dlp");
	}

	static Path deno() {
		return dir().resolve(windows() ? "deno.exe" : "deno");
	}

	private static String ytdlpAsset() {
		if (windows()) {
			return arm() ? "yt-dlp_arm64.exe" : "yt-dlp.exe";
		}
		if (mac()) {
			return "yt-dlp_macos";
		}
		return arm() ? "yt-dlp_linux_aarch64" : "yt-dlp_linux";
	}

	private static String denoAsset() {
		String arch = arm() ? "aarch64" : "x86_64";
		String os = windows() ? "pc-windows-msvc" : mac() ? "apple-darwin" : "unknown-linux-gnu";
		return "deno-" + arch + "-" + os + ".zip";
	}

	/** Whether a Deno is installed already (on the PATH), so ours isn't needed. */
	private static synchronized boolean systemDeno() {
		if (denoOnPath == null) {
			denoOnPath = false;
			try {
				Process p = new ProcessBuilder("deno", "--version").redirectErrorStream(true).start();
				p.getInputStream().readAllBytes();
				denoOnPath = p.waitFor(5, TimeUnit.SECONDS) && p.exitValue() == 0;
			} catch (Exception e) {
				denoOnPath = false;
			}
		}
		return denoOnPath;
	}

	static boolean installed() {
		return Files.isRegularFile(ytdlp()) && (Files.isRegularFile(deno()) || systemDeno());
	}

	/** Starts getting the helper in the background, if it isn't here and isn't on its way. */
	static synchronized void installAsync() {
		consented = true;
		if (busy || installed() && failure == null) {
			return;
		}
		busy = true;
		failure = null;
		Thread t = new Thread(() -> {
			try {
				install(false);
				status = "";
			} catch (Exception e) {
				Jugcraft.LOGGER.warn("TV: couldn't get the YouTube helper", e);
				failure = e.getMessage() == null ? e.toString() : e.getMessage();
				status = "";
			} finally {
				busy = false;
			}
		}, "Jugcraft TV helper download");
		t.setDaemon(true);
		t.start();
	}

	/** Downloads what is missing (and yt-dlp again if {@code updateYtdlp} and there is a newer one). */
	private static void install(boolean updateYtdlp) throws IOException, InterruptedException {
		Files.createDirectories(dir());
		if (!Files.isRegularFile(ytdlp()) || updateYtdlp) {
			JsonObject release = latest("yt-dlp/yt-dlp");
			String tag = release.has("tag_name") ? release.get("tag_name").getAsString() : "";
			Path version = dir().resolve("yt-dlp.version");
			Files.writeString(dir().resolve("yt-dlp.checked"), Long.toString(System.currentTimeMillis()));
			if (!Files.isRegularFile(ytdlp()) || !tag.equals(Files.isRegularFile(version) ? Files.readString(version).trim() : "")) {
				Path part = dir().resolve("yt-dlp.part");
				fetch(release, ytdlpAsset(), part, "yt-dlp");
				Files.move(part, ytdlp(), StandardCopyOption.REPLACE_EXISTING);
				ytdlp().toFile().setExecutable(true);
				Files.writeString(version, tag);
			}
		}
		if (!Files.isRegularFile(deno()) && !systemDeno()) {
			JsonObject release = latest("denoland/deno");
			Path zip = dir().resolve("deno.zip.part");
			fetch(release, denoAsset(), zip, "Deno");
			status = "Unpacking Deno";
			Path part = dir().resolve("deno.part");
			boolean found = false;
			try (ZipInputStream in = new ZipInputStream(Files.newInputStream(zip))) {
				for (ZipEntry e = in.getNextEntry(); e != null; e = in.getNextEntry()) {
					String name = e.getName();
					if (!e.isDirectory() && (name.equals("deno") || name.equals("deno.exe"))) {
						Files.copy(in, part, StandardCopyOption.REPLACE_EXISTING);
						found = true;
						break;
					}
				}
			}
			Files.deleteIfExists(zip);
			if (!found) {
				throw new IOException("Deno's download had no deno in it");
			}
			Files.move(part, deno(), StandardCopyOption.REPLACE_EXISTING);
			deno().toFile().setExecutable(true);
		}
	}

	private static JsonObject latest(String repo) throws IOException, InterruptedException {
		HttpRequest req = HttpRequest.newBuilder(URI.create("https://api.github.com/repos/" + repo + "/releases/latest"))
				.header("Accept", "application/vnd.github+json").header("User-Agent", UA).timeout(Duration.ofSeconds(30)).build();
		HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
		if (res.statusCode() != 200) {
			throw new IOException("GitHub answered " + res.statusCode() + " for " + repo);
		}
		return JsonParser.parseString(res.body()).getAsJsonObject();
	}

	/** Downloads the release's asset {@code name} to {@code to}, checking it against the digest GitHub gives for it. */
	private static void fetch(JsonObject release, String name, Path to, String what) throws IOException, InterruptedException {
		JsonObject asset = null;
		for (JsonElement e : release.getAsJsonArray("assets")) {
			if (e.getAsJsonObject().get("name").getAsString().equals(name)) {
				asset = e.getAsJsonObject();
			}
		}
		if (asset == null) {
			throw new IOException(what + " has no " + name + " for this computer");
		}
		String digest = asset.has("digest") && !asset.get("digest").isJsonNull() ? asset.get("digest").getAsString() : "";
		if (!digest.startsWith("sha256:")) {
			// An older release without GitHub's digests: the checksum file the project publishes beside it.
			digest = "sha256:" + publishedSum(release, name);
		}
		long size = asset.has("size") ? asset.get("size").getAsLong() : 0;
		HttpRequest req = HttpRequest.newBuilder(URI.create(asset.get("browser_download_url").getAsString())).header("User-Agent", UA)
				.timeout(Duration.ofMinutes(10)).build();
		HttpResponse<InputStream> res = HTTP.send(req, HttpResponse.BodyHandlers.ofInputStream());
		if (res.statusCode() != 200) {
			throw new IOException("Downloading " + name + ": " + res.statusCode());
		}
		MessageDigest sha;
		try {
			sha = MessageDigest.getInstance("SHA-256");
		} catch (java.security.NoSuchAlgorithmException e) {
			throw new IOException(e);
		}
		long got = 0;
		try (InputStream in = res.body(); OutputStream out = Files.newOutputStream(to)) {
			byte[] buf = new byte[1 << 16];
			for (int n; (n = in.read(buf)) > 0; ) {
				out.write(buf, 0, n);
				sha.update(buf, 0, n);
				got += n;
				progress = size > 0 ? (int) (got * 100 / size) : 0;
				status = "Getting " + what;
			}
		}
		String hex = HexFormat.of().formatHex(sha.digest());
		if (!hex.equalsIgnoreCase(digest.substring(7))) {
			Files.deleteIfExists(to);
			throw new IOException(name + " didn't match its checksum");
		}
	}

	/** The SHA-256 of asset {@code name} from the release's SHA2-256SUMS (yt-dlp) or {@code name}.sha256sum (Deno). */
	private static String publishedSum(JsonObject release, String name) throws IOException, InterruptedException {
		for (JsonElement e : release.getAsJsonArray("assets")) {
			JsonObject a = e.getAsJsonObject();
			String n = a.get("name").getAsString();
			if (n.equals("SHA2-256SUMS") || n.equals(name + ".sha256sum")) {
				HttpRequest req = HttpRequest.newBuilder(URI.create(a.get("browser_download_url").getAsString())).header("User-Agent", UA)
						.timeout(Duration.ofSeconds(30)).build();
				String body = HTTP.send(req, HttpResponse.BodyHandlers.ofString()).body();
				for (String line : body.split("\\R")) {
					java.util.regex.Matcher m = java.util.regex.Pattern.compile("([0-9a-fA-F]{64})").matcher(line);
					if (m.find() && (n.endsWith(".sha256sum") || line.trim().endsWith(" " + name) || line.trim().endsWith("*" + name))) {
						return m.group(1);
					}
				}
			}
		}
		throw new IOException("No checksum published for " + name);
	}

	/** A YouTube link to a playlist (its {@code list=}): the screen may play the whole list. */
	static boolean isPlaylist(String url) {
		return isYouTube(url) && !query(url, "list").isEmpty();
	}

	/** The value of {@code key} in the link's query ("" if none). */
	static String query(String url, String key) {
		try {
			String q = URI.create(url).getRawQuery();
			if (q != null) {
				for (String part : q.split("&")) {
					int eq = part.indexOf('=');
					if (eq > 0 && part.substring(0, eq).equals(key)) {
						return java.net.URLDecoder.decode(part.substring(eq + 1), StandardCharsets.UTF_8);
					}
				}
			}
		} catch (IllegalArgumentException ignored) {
			// not a link
		}
		return "";
	}

	/** Forgets what yt-dlp found lately (a screen started over finds its streams afresh). */
	static void forgetAll() {
		KEPT.clear();
	}

	/** What yt-dlp found for {@code url} the way {@code attempt} ({@link #WAYS}), if found lately. */
	static @Nullable Resolved kept(String url, int attempt) {
		Kept k = KEPT.get(attempt + " " + url);
		return k != null && System.currentTimeMillis() - k.at() < KEEP_MS ? k.r() : null;
	}

	/** Has yt-dlp find {@code url}'s streams in the background (a playlist's next video), to be {@link #kept}. */
	static void prefetch(String url) {
		Site site = site(url);
		if (site == null) {
			return;
		}
		int way = lastGood(site);
		if (!installed() || kept(url, way) != null || !FETCHING.add(url)) {
			return;
		}
		Thread t = new Thread(() -> {
			try {
				resolve(url, way);
			} catch (Exception e) {
				// it will be asked again when it's due
			} finally {
				FETCHING.remove(url);
			}
		}, "Jugcraft TV next video");
		t.setDaemon(true);
		t.start();
	}

	/**
	 * Asks yt-dlp for the video at {@code url} the way {@code attempt} ({@link #WAYS}); when that fails with an old
	 * yt-dlp, gets the newest and asks again.
	 */
	static Resolved resolve(String url, int attempt) throws IOException, InterruptedException {
		Resolved had = kept(url, attempt);
		if (had != null) {
			return had;
		}
		Resolved r = resolveNow(url, attempt);
		KEPT.put(attempt + " " + url, new Kept(r, System.currentTimeMillis()));
		if (KEPT.size() > 64) {
			long now = System.currentTimeMillis();
			KEPT.values().removeIf(k -> now - k.at() > KEEP_MS);
		}
		return r;
	}

	private static Resolved resolveNow(String url, int attempt) throws IOException, InterruptedException {
		try {
			return run(url, attempt);
		} catch (IOException first) {
			Path checked = dir().resolve("yt-dlp.checked");
			long last = 0;
			try {
				last = Long.parseLong(Files.readString(checked).trim());
			} catch (Exception ignored) {
				// never checked
			}
			if (System.currentTimeMillis() - last < RECHECK_MS) {
				throw first;
			}
			synchronized (TvHelpers.class) {
				install(true);
			}
			return run(url, attempt);
		}
	}

	/** Runs yt-dlp with {@code args} on {@code url} (the JavaScript engine given it), returning what it printed. */
	private static String runYtdlp(List<String> args, String url, int seconds) throws IOException, InterruptedException {
		return runYtdlp(args, url, seconds, WAYS[0].clients(), List.of());
	}

	/**
	 * As {@link #runYtdlp(List, String, int)}, asking as YouTube's apps {@code clients} (null: not YouTube), with the
	 * way's {@code extra} arguments ({@code --impersonate chrome}...).
	 */
	private static String runYtdlp(List<String> args, String url, int seconds, @Nullable String clients, List<String> extra)
			throws IOException, InterruptedException {
		List<String> cmd = new ArrayList<>(List.of(ytdlp().toString(), "--ignore-config", "--no-warnings", "--no-progress"));
		if (clients != null) {
			cmd.add("--extractor-args");
			cmd.add("youtube:player_client=" + clients);
		}
		cmd.addAll(extra);
		cmd.addAll(args);
		if (Files.isRegularFile(deno())) {
			cmd.add("--js-runtimes");
			cmd.add("deno:" + deno());
		}
		cmd.add("--");
		cmd.add(url);
		Path err = Files.createTempFile(dir(), "yt-dlp-", ".err");
		try {
			Process p = new ProcessBuilder(cmd).directory(dir().toFile()).redirectError(err.toFile()).start();
			byte[] out;
			try (InputStream in = p.getInputStream()) {
				out = in.readAllBytes();
			}
			if (!p.waitFor(seconds, TimeUnit.SECONDS)) {
				p.destroyForcibly();
				throw new IOException("yt-dlp took too long");
			}
			if (p.exitValue() != 0) {
				String why = "";
				try {
					List<String> lines = Files.readAllLines(err, StandardCharsets.UTF_8);
					Files.write(dir().resolve("last-error.txt"), lines, StandardCharsets.UTF_8);
					for (String l : lines) {
						if (l.startsWith("ERROR:")) {
							why = l.substring(6).trim();
						}
					}
				} catch (IOException ignored) {
					// no message
				}
				throw new IOException(why.isEmpty() ? "yt-dlp couldn't read the link" : why);
			}
			return new String(out, StandardCharsets.UTF_8).trim();
		} finally {
			Files.deleteIfExists(err);
		}
	}

	private static Resolved run(String url, int attempt) throws IOException, InterruptedException {
		Site site = site(url);
		Way[] ways = site == null ? WAYS : site.ways();
		Way way = ways[Math.min(attempt, ways.length - 1)];
		List<String> args = new ArrayList<>(List.of("--no-playlist", "-j", "-f", way.format()));
		if (way.relay()) {
			args.add("--force-ipv4");      // the streams' addresses are made out to the address asking: the relay asks over IPv4
		}
		String out = runYtdlp(args, url, 90, way.clients(), way.extra());
		JsonObject j = JsonParser.parseString(out.split("\n")[0]).getAsJsonObject();
		String title = j.has("title") && !j.get("title").isJsonNull() ? j.get("title").getAsString() : "";
		long duration = j.has("duration") && !j.get("duration").isJsonNull() ? Math.round(j.get("duration").getAsDouble() * 1000) : 0;
		boolean live = j.has("is_live") && !j.get("is_live").isJsonNull() && j.get("is_live").getAsBoolean();
		String video = null, audio = null;
		boolean hls = false, files = true;
		Map<String, String> headers = new LinkedHashMap<>();
		if (j.has("requested_formats") && j.get("requested_formats").isJsonArray()) {
			JsonArray formats = j.getAsJsonArray("requested_formats");
			hls = !formats.isEmpty();
			for (JsonElement e : formats) {
				JsonObject f = e.getAsJsonObject();
				files &= protocol(f).equals("https");
				hls &= protocol(f).startsWith("m3u8");
				String vcodec = f.has("vcodec") && !f.get("vcodec").isJsonNull() ? f.get("vcodec").getAsString() : "none";
				String u = f.get("url").getAsString();
				if (!vcodec.equals("none") && video == null) {
					video = u;
				} else if (audio == null) {
					audio = u;
				}
				headers(f, headers);
			}
		}
		if (video == null && j.has("url")) {
			video = j.get("url").getAsString();
			audio = video;
			headers(j, headers);
			hls = protocol(j).startsWith("m3u8");
			files = protocol(j).equals("https");
		}
		if (video == null) {
			throw new IOException("yt-dlp found no video there");
		}
		return new Resolved(video, audio, headers, title, duration, live, hls, files && !live);
	}

	private static String protocol(JsonObject f) {
		return f.has("protocol") && !f.get("protocol").isJsonNull() ? f.get("protocol").getAsString() : "";
	}

	/**
	 * The playlist a link names (its {@code list=}), as yt-dlp lists it without opening each video: each video's link,
	 * title and length; the videos it can't play (private, taken down: no length) left out. At most 300.
	 */
	static TvProgram playlist(String url) throws IOException, InterruptedException {
		String out = runYtdlp(List.of("--flat-playlist", "--yes-playlist", "--playlist-end", "300", "-J"), url, 120);
		JsonObject j = JsonParser.parseString(out).getAsJsonObject();
		List<TvProgram.Entry> entries = new ArrayList<>();
		if (j.has("entries") && j.get("entries").isJsonArray()) {
			for (JsonElement e : j.getAsJsonArray("entries")) {
				if (!e.isJsonObject()) {
					continue;
				}
				JsonObject f = e.getAsJsonObject();
				String id = f.has("id") && !f.get("id").isJsonNull() ? f.get("id").getAsString() : "";
				double secs = f.has("duration") && !f.get("duration").isJsonNull() ? f.get("duration").getAsDouble() : 0;
				if (id.isEmpty() || secs <= 0) {
					continue;
				}
				String title = f.has("title") && !f.get("title").isJsonNull() ? f.get("title").getAsString() : "";
				entries.add(new TvProgram.Entry(id, "https://www.youtube.com/watch?v=" + id, title, Math.round(secs * 1000)));
			}
		}
		if (entries.isEmpty()) {
			throw new IOException("the playlist has no videos to play");
		}
		String title = j.has("title") && !j.get("title").isJsonNull() ? j.get("title").getAsString() : "";
		// It starts at the video the link names (v=), or at its index=, or at the top.
		String v = query(url, "v");
		int start = 0;
		for (int i = 0; i < entries.size(); i++) {
			if (entries.get(i).id().equals(v)) {
				start = i;
				break;
			}
		}
		if (v.isEmpty()) {
			try {
				start = Math.max(0, Math.min(entries.size() - 1, Integer.parseInt(query(url, "index")) - 1));
			} catch (NumberFormatException ignored) {
				start = 0;
			}
		}
		return new TvProgram(title, entries, start);
	}

	private static void headers(JsonObject f, Map<String, String> into) {
		if (f.has("http_headers") && f.get("http_headers").isJsonObject()) {
			for (Map.Entry<String, JsonElement> h : f.getAsJsonObject("http_headers").entrySet()) {
				into.putIfAbsent(h.getKey(), h.getValue().getAsString());
			}
		}
	}
}
