package io.github.jimbozoomer.jugcraft.client.tv;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import org.jspecify.annotations.Nullable;

/**
 * The TVs' video decoder, fetched the first time this game plays a TV (docs/features/flat-screen-tv.md): JavaCV, JavaCPP
 * and Bytedeco's LGPL FFmpeg build, with the natives for this computer only (about 35 MB), from Maven Central into
 * .minecraft/jugcraft/tv-helpers/lib, each checked against the SHA-1 Maven Central publishes for it. They're loaded in
 * a class loader of their own with {@link TvFfmpeg}, which the rest of the mod reaches through {@link TvMedia}. No
 * third-party jar ships inside Jugcraft's (docs/FRAMEWORKS.md).
 */
final class TvCodec {
	static final String JAVACV = "1.5.14", FFMPEG = "8.1.2-1.5.14";
	private static final String CENTRAL = "https://repo1.maven.org/maven2/org/bytedeco/";
	private static final String BRIDGE = "io.github.jimbozoomer.jugcraft.client.tv.TvFfmpeg";
	private static final HttpClient HTTP = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL)
			.connectTimeout(Duration.ofSeconds(20)).build();
	private static @Nullable TvMedia media;
	static volatile String status = "";
	static volatile int progress;
	static volatile @Nullable String failure;

	private TvCodec() {
	}

	/** JavaCPP's name for this computer (windows-x86_64...), or null if Bytedeco builds none for it. */
	static @Nullable String platform() {
		String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT), arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
		boolean arm = arch.contains("aarch64") || arch.contains("arm64"), x64 = arch.contains("amd64") || arch.contains("x86_64");
		if (os.contains("win")) {
			return x64 ? "windows-x86_64" : null;
		}
		if (os.contains("mac") || os.contains("darwin")) {
			return arm ? "macosx-arm64" : "macosx-x86_64";
		}
		if (os.contains("linux")) {
			return arm ? "linux-arm64" : x64 ? "linux-x86_64" : null;
		}
		return null;
	}

	/** The decoder, fetched and loaded if it isn't yet (blocking: a TV's thread). */
	static synchronized TvMedia media() throws Exception {
		if (media != null) {
			return media;
		}
		String platform = platform();
		if (platform == null) {
			throw new IOException("no video player for this computer");
		}
		String[][] jars = {
				{"javacv", JAVACV, ""}, {"javacpp", JAVACV, ""}, {"javacpp", JAVACV, platform}, {"ffmpeg", FFMPEG, ""}, {"ffmpeg", FFMPEG, platform}};
		Path lib = TvHelpers.dir().resolve("lib");
		Files.createDirectories(lib);
		List<URL> urls = new ArrayList<>();
		for (int i = 0; i < jars.length; i++) {
			String a = jars[i][0], v = jars[i][1], c = jars[i][2];
			String file = a + "-" + v + (c.isEmpty() ? "" : "-" + c) + ".jar";
			Path at = lib.resolve(file);
			if (!Files.isRegularFile(at) || !Files.isRegularFile(lib.resolve(file + ".ok"))) {
				status = "GETTING THE VIDEO PLAYER " + (i + 1) + "/" + jars.length;
				fetch(CENTRAL + a + "/" + v + "/" + file, at);
				Files.writeString(lib.resolve(file + ".ok"), "checked against Maven Central's SHA-1\n");
			}
			urls.add(at.toUri().toURL());
		}
		status = "";
		ClassLoader loader = new Own(urls.toArray(new URL[0]), TvCodec.class.getClassLoader());
		media = (TvMedia) loader.loadClass(BRIDGE).getDeclaredConstructor().newInstance();
		return media;
	}

	private static void fetch(String url, Path to) throws IOException, InterruptedException {
		String sum = HTTP.send(HttpRequest.newBuilder(URI.create(url + ".sha1")).timeout(Duration.ofSeconds(30)).build(),
				HttpResponse.BodyHandlers.ofString()).body().trim();
		if (!sum.matches("(?s)[0-9a-fA-F]{40}.*")) {
			throw new IOException("Maven Central gave no checksum for " + to.getFileName());
		}
		sum = sum.substring(0, 40);
		HttpResponse<InputStream> res = HTTP.send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofMinutes(10)).build(),
				HttpResponse.BodyHandlers.ofInputStream());
		if (res.statusCode() != 200) {
			throw new IOException("Downloading " + to.getFileName() + ": " + res.statusCode());
		}
		long size = res.headers().firstValueAsLong("Content-Length").orElse(0);
		MessageDigest sha;
		try {
			sha = MessageDigest.getInstance("SHA-1");
		} catch (java.security.NoSuchAlgorithmException e) {
			throw new IOException(e);
		}
		Path part = to.resolveSibling(to.getFileName() + ".part");
		long got = 0;
		try (InputStream in = res.body(); OutputStream out = Files.newOutputStream(part)) {
			byte[] buf = new byte[1 << 16];
			for (int n; (n = in.read(buf)) > 0; ) {
				out.write(buf, 0, n);
				sha.update(buf, 0, n);
				got += n;
				progress = size > 0 ? (int) (got * 100 / size) : 0;
			}
		}
		if (!HexFormat.of().formatHex(sha.digest()).equalsIgnoreCase(sum)) {
			Files.deleteIfExists(part);
			throw new IOException(to.getFileName() + " didn't match its checksum");
		}
		Files.move(part, to, StandardCopyOption.REPLACE_EXISTING);
	}

	/** Bytedeco's classes and the bridge from the fetched jars; everything else (the game, the mod's TvMedia) from the game's. */
	private static final class Own extends URLClassLoader {
		private final ClassLoader game;

		Own(URL[] urls, ClassLoader game) {
			super("jugcraft-tv", urls, game);
			this.game = game;
		}

		@Override
		protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
			synchronized (getClassLoadingLock(name)) {
				Class<?> c = findLoadedClass(name);
				if (c == null) {
					if (name.startsWith("org.bytedeco.")) {
						c = findClass(name);
					} else if (name.equals(BRIDGE) || name.startsWith(BRIDGE + "$")) {
						try (InputStream in = game.getResourceAsStream(name.replace('.', '/') + ".class")) {
							if (in == null) {
								throw new ClassNotFoundException(name);
							}
							byte[] b = in.readAllBytes();
							c = defineClass(name, b, 0, b.length);
						} catch (IOException e) {
							throw new ClassNotFoundException(name, e);
						}
					} else {
						return super.loadClass(name, resolve);
					}
				}
				if (resolve) {
					resolveClass(c);
				}
				return c;
			}
		}
	}
}
