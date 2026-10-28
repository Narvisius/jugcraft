package io.github.jimbozoomer.jugcraft.client.tv;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import org.jspecify.annotations.Nullable;

/**
 * Plays a YouTube stream the way yt-dlp fetches it (docs/features/flat-screen-tv.md): FFmpeg reads it from this
 * game's own address (127.0.0.1, a random link only this game knows) and the relay fetches it from YouTube a piece at a
 * time ({@link #CHUNK}, as yt-dlp does: YouTube refuses or slows one long request for a big file), with the headers
 * yt-dlp says to send, answering FFmpeg's jumps (byte ranges) as YouTube would. Nothing is kept: each piece is passed on
 * as it comes. Only the streams the helper found are served, and nothing outside this computer can reach it.
 */
final class TvRelay {
	/** How much is asked of YouTube at once (yt-dlp's piece size for YouTube). */
	static final long CHUNK = 10L * 1024 * 1024;
	private static final int MAX_SOURCES = 64;
	/** Headers Java's HTTP client sets itself (it refuses them from us). */
	private static final Set<String> RESTRICTED = Set.of("connection", "content-length", "expect", "host", "upgrade", "range", "accept-encoding");
	private static final HttpClient HTTP = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL)
			.connectTimeout(Duration.ofSeconds(20)).build();
	private static final Map<String, Source> SOURCES = new ConcurrentHashMap<>();
	private static @Nullable HttpServer server;
	private static int port;

	/** A stream served: YouTube's address for it, the headers to send, its length once known (-1 before). */
	private record Source(String url, Map<String, String> headers, AtomicLong length, long added) {
	}

	private TvRelay() {
	}

	/** The address FFmpeg reads {@code url} from (fetched in pieces with {@code headers}). */
	static synchronized String serve(String url, Map<String, String> headers) throws IOException {
		if (server == null) {
			HttpServer s = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 16);
			s.createContext("/s/", TvRelay::handle);
			s.setExecutor(Executors.newCachedThreadPool(r -> {
				Thread t = new Thread(r, "Jugcraft TV relay");
				t.setDaemon(true);
				return t;
			}));
			s.start();
			server = s;
			port = s.getAddress().getPort();
		}
		if (SOURCES.size() >= MAX_SOURCES) {
			SOURCES.entrySet().stream().min(java.util.Comparator.comparingLong(e -> e.getValue().added()))
					.ifPresent(e -> SOURCES.remove(e.getKey()));
		}
		String id = UUID.randomUUID().toString().replace("-", "");
		SOURCES.put(id, new Source(url, Map.copyOf(headers), new AtomicLong(-1), System.nanoTime()));
		return "http://127.0.0.1:" + port + "/s/" + id;
	}

	private static void handle(HttpExchange ex) throws IOException {
		try (ex) {
			String id = ex.getRequestURI().getPath().substring(3);
			Source s = SOURCES.get(id);
			if (s == null || !ex.getRequestMethod().equalsIgnoreCase("GET") && !ex.getRequestMethod().equalsIgnoreCase("HEAD")) {
				ex.sendResponseHeaders(404, -1);
				return;
			}
			// FFmpeg asks "bytes=a-" (or "bytes=a-b"): from a on
			long from = 0, to = -1;
			String range = ex.getRequestHeaders().getFirst("Range");
			boolean ranged = range != null && range.startsWith("bytes=");
			if (ranged) {
				String[] ab = range.substring(6).split("-", 2);
				try {
					from = ab[0].isBlank() ? 0 : Long.parseLong(ab[0].trim());
					to = ab.length < 2 || ab[1].isBlank() ? -1 : Long.parseLong(ab[1].trim());
				} catch (NumberFormatException e) {
					from = 0;
					to = -1;
				}
			}
			long known = s.length().get();
			if (known >= 0 && from >= known) {
				ex.getResponseHeaders().add("Content-Range", "bytes */" + known);
				ex.sendResponseHeaders(416, -1);
				return;
			}
			HttpResponse<InputStream> first = fetch(s, from, to);
			// A stream YouTube has only just made out can be refused for a second or two, then answer as any other (Justin's
			// 27 October 2026 test: the same address 403 four times in 1.4 s, then 206 from then on): asked again a few
			// times, until it has once answered.
			for (int tries = 0; first.statusCode() == 403 && s.length().get() < 0 && tries < 4; tries++) {
				first.body().close();
				Thread.sleep(1000);
				first = fetch(s, from, to);
			}
			int code = first.statusCode();
			if (code != 200 && code != 206) {
				first.body().close();
				Jugcraft.LOGGER.info("TV relay: YouTube answered {} for a stream piece at {}", code, from);
				ex.sendResponseHeaders(code == 403 || code == 404 || code == 410 ? code : 502, -1);
				return;
			}
			long total = total(first);
			if (total >= 0) {
				s.length().set(total);
			}
			long last = total < 0 ? -1 : to >= 0 ? Math.min(to, total - 1) : total - 1;
			String type = first.headers().firstValue("Content-Type").orElse("application/octet-stream");
			ex.getResponseHeaders().add("Content-Type", type);
			ex.getResponseHeaders().add("Accept-Ranges", "bytes");
			if (last >= 0) {
				ex.getResponseHeaders().add("Content-Range", "bytes " + from + "-" + last + "/" + total);
			}
			boolean head = ex.getRequestMethod().equalsIgnoreCase("HEAD");
			ex.sendResponseHeaders(ranged || from > 0 ? 206 : 200, head ? -1 : last >= 0 ? last - from + 1 : 0);
			if (head) {
				first.body().close();
				return;
			}
			OutputStream out = ex.getResponseBody();
			long at = from;
			HttpResponse<InputStream> piece = first;
			boolean whole = code == 200;          // YouTube sent it all from its start (it didn't take the range)
			while (true) {
				try (InputStream in = piece.body()) {
					if (whole && from > 0) {
						in.skipNBytes(from);
					}
					at += in.transferTo(out);
				}
				if (whole) {
					return;
				}
				out.flush();
				if (last < 0 || at > last) {
					return;
				}
				piece = fetch(s, at, to);
				if (piece.statusCode() != 200 && piece.statusCode() != 206) {
					piece.body().close();
					Jugcraft.LOGGER.info("TV relay: YouTube answered {} for a stream piece at {}", piece.statusCode(), at);
					return;
				}
			}
		} catch (IOException e) {
			// FFmpeg went away (a seek, the screen let go): nothing to do
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	/** The piece of the stream from {@code from} (to {@code to}, or a {@link #CHUNK} on). */
	private static HttpResponse<InputStream> fetch(Source s, long from, long to) throws IOException, InterruptedException {
		long end = from + CHUNK - 1;
		if (to >= 0) {
			end = Math.min(end, to);
		}
		HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(s.url())).timeout(Duration.ofSeconds(30)).GET()
				.header("Range", "bytes=" + from + "-" + end);
		for (Map.Entry<String, String> h : s.headers().entrySet()) {
			if (!RESTRICTED.contains(h.getKey().toLowerCase(Locale.ROOT))) {
				b.header(h.getKey(), h.getValue());
			}
		}
		return HTTP.send(b.build(), HttpResponse.BodyHandlers.ofInputStream());
	}

	/** The stream's whole length: from YouTube's Content-Range ("bytes a-b/T"), else (all of it sent) its length. */
	private static long total(HttpResponse<InputStream> r) {
		String cr = r.headers().firstValue("Content-Range").orElse("");
		int slash = cr.lastIndexOf('/');
		if (slash >= 0) {
			try {
				return Long.parseLong(cr.substring(slash + 1).trim());
			} catch (NumberFormatException ignored) {
				// "*": not said
			}
		}
		long len = r.headers().firstValueAsLong("Content-Length").orElse(-1);
		return r.statusCode() == 200 && len >= 0 ? len : -1;
	}

	/** The headers to pass on as FFmpeg's would be: none (the relay sends yt-dlp's). */
	static Map<String, String> options() {
		return new LinkedHashMap<>();
	}
}
