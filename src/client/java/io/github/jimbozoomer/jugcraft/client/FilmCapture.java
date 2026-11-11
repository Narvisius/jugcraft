package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.textures.GpuTexture;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEventListener;
import net.minecraft.client.sounds.WeighedSoundEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The film camera's recorder (client/FilmShoot): each frame the game draws (the world only, before the screen's
 * overlays: mixin/client/FilmCaptureMixin), up to {@value #FPS} a second, is copied off the graphics card without
 * holding the game up (a few buffers in turn, read back when the card is done with them) and written as JPEG by a few
 * threads of its own, each to files of its own ({@code frames-T-N.mjpg}, the JPEGs one after another), every frame's file,
 * place and time in {@code frames.csv}; a frame wider than 1920 is scaled down to 1920 wide. Every sound the game plays
 * goes in {@code sounds.csv} with its time, where and how loud (the game's own mix is written by OpenAL itself: the bat
 * starts the game with OpenAL's wave writer; this list is the fallback, and lines the mix up).
 */
public final class FilmCapture {
	private static final Logger LOG = LoggerFactory.getLogger("Jugcraft film");
	static final int FPS = 30;
	private static final int MAX_BUFFERS = 8, ENCODERS = 6, MAX_QUEUE = 64, MAX_WIDTH = 1920;
	private static final long PART_BYTES = 150L << 20;
	private static final float QUALITY = 0.9F;

	static volatile boolean rolling;
	private static Path dir;
	private static long startNs, slot;
	private static final ArrayDeque<GpuBuffer> free = new ArrayDeque<>();
	private static int buffers, bufW, bufH;
	private static ThreadPoolExecutor pool;
	private static final AtomicInteger encoding = new AtomicInteger(), encoded = new AtomicInteger();
	static int frames, dropped, busy;
	static volatile String format = "?";
	private static final List<String> frameLines = Collections.synchronizedList(new ArrayList<>());
	private static final List<String> soundLines = Collections.synchronizedList(new ArrayList<>());
	private static final List<String> problems = Collections.synchronizedList(new ArrayList<>());
	private static final List<Writer> writers = Collections.synchronizedList(new ArrayList<>());
	private static final ThreadLocal<Writer> WRITER = new ThreadLocal<>();
	private static SoundEventListener listener;

	private FilmCapture() {
	}

	/** What's written alongside each frame (the camera's shot and place), set by FilmShoot each tick. */
	static volatile String tag = "";

	/** Starts listening for sounds (before the marker plays). */
	static void listen(Path folder) {
		dir = folder;
		soundLines.add("nanos,sound,category,x,y,z,volume,pitch,relative,attenuation,range");
		listener = (SoundInstance s, WeighedSoundEvents events, float range) -> soundLines.add(String.format(Locale.ROOT,
				"%d,%s,%s,%.3f,%.3f,%.3f,%.3f,%.3f,%s,%s,%.1f", System.nanoTime(), s.getIdentifier(), s.getSource().getName(), s.getX(), s.getY(), s.getZ(),
				s.getVolume(), s.getPitch(), s.isRelative(), s.getAttenuation(), range));
		Minecraft.getInstance().getSoundManager().addListener(listener);
	}

	static void start() {
		frameLines.add("frame,file,offset,length,nanos,width,height,tag");
		pool = new ThreadPoolExecutor(ENCODERS, ENCODERS, 30, TimeUnit.SECONDS, new ArrayBlockingQueue<>(MAX_QUEUE + 8), r -> {
			Thread t = new Thread(r, "Jugcraft film encoder");
			t.setDaemon(true);
			t.setPriority(Thread.NORM_PRIORITY - 1);
			return t;
		});
		startNs = System.nanoTime();
		slot = 0;
		rolling = true;
		LOG.info("Rolling");
	}

	static long startNs() {
		return startNs;
	}

	static void stop() {
		rolling = false;
	}

	/** Whether every frame copied has been written. */
	static boolean drained() {
		return encoding.get() == 0 && (buffers == free.size() || buffers == 0);
	}

	/** Each frame, once the world is drawn (the render thread). */
	public static void frame() {
		if (!rolling) {
			return;
		}
		long now = System.nanoTime();
		long s = (now - startNs) * FPS / 1_000_000_000L;
		if (s < slot) {
			return;
		}
		slot = s + 1;
		try {
			RenderTarget target = Minecraft.getInstance().gameRenderer.mainRenderTarget();
			GpuTexture tex = target.getColorTexture();
			if (tex == null) {
				return;
			}
			int w = target.width, h = target.height;
			int block = tex.getFormat().blockSize();
			format = tex.getFormat() + " (" + block + " bytes)";
			if (block != 4) {
				problems.add("Frame format " + format + ": not captured");
				rolling = false;
				return;
			}
			if (w != bufW || h != bufH) {
				// the window changed size: new buffers (the old closed once they're back)
				while (!free.isEmpty()) {
					free.poll().close();
					buffers--;
				}
				bufW = w;
				bufH = h;
			}
			GpuBuffer buf = free.poll();
			if (buf == null) {
				if (buffers >= MAX_BUFFERS) {
					busy++;
					return;
				}
				buf = RenderSystem.getDevice().createBuffer(() -> "Jugcraft film frame", GpuBuffer.USAGE_MAP_READ | GpuBuffer.USAGE_COPY_DST, (long) w * h * 4);
				buffers++;
			}
			int index = frames++;
			GpuBuffer b = buf;
			String t = tag;
			RenderSystem.getDevice().createCommandEncoder().copyTextureToBuffer(tex, b, 0, () -> readBack(b, w, h, index, now, t), 0);
		} catch (Throwable e) {
			problems.add("Frame " + frames + ": " + e);
			LOG.error("Frame capture failed", e);
			rolling = false;
		}
	}

	/** The card is done copying a frame: off it, and to an encoder (the render thread). */
	private static void readBack(GpuBuffer buf, int w, int h, int index, long nanos, String t) {
		byte[] rgba = new byte[w * h * 4];
		try (GpuBufferSlice.MappedView view = buf.map(true, false)) {
			ByteBuffer data = view.data();
			data.get(0, rgba, 0, Math.min(rgba.length, data.remaining()));
		} catch (Throwable e) {
			problems.add("Reading frame " + index + ": " + e);
			return;
		} finally {
			if (w == bufW && h == bufH) {
				free.add(buf);
			} else {
				buf.close();
				buffers--;
			}
		}
		if (encoding.get() >= MAX_QUEUE) {
			dropped++;
			return;
		}
		encoding.incrementAndGet();
		try {
			pool.execute(() -> {
				try {
					encode(rgba, w, h, index, nanos, t);
				} catch (Throwable e) {
					problems.add("Encoding frame " + index + ": " + e);
				} finally {
					encoding.decrementAndGet();
				}
			});
		} catch (Throwable e) {
			encoding.decrementAndGet();
			dropped++;
		}
	}

	private static final class Writer {
		final int thread;
		int part = -1;
		long written;
		OutputStream out;
		String name;
		ImageWriter jpeg;
		ImageWriteParam param;

		Writer(int thread) {
			this.thread = thread;
		}

		void next() throws IOException {
			if (out != null) {
				out.close();
			}
			part++;
			name = String.format(Locale.ROOT, "frames-%d-%d.mjpg", thread, part);
			out = new java.io.BufferedOutputStream(Files.newOutputStream(dir.resolve(name)), 1 << 20);
			written = 0;
		}
	}

	private static void encode(byte[] rgba, int w, int h, int index, long nanos, String t) throws IOException {
		BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);
		byte[] bgr = ((DataBufferByte) img.getRaster().getDataBuffer()).getData();
		// the card's rows run from the bottom up
		for (int y = 0; y < h; y++) {
			int s = (h - 1 - y) * w * 4, d = y * w * 3;
			for (int x = 0; x < w; x++, s += 4, d += 3) {
				bgr[d] = rgba[s + 2];
				bgr[d + 1] = rgba[s + 1];
				bgr[d + 2] = rgba[s];
			}
		}
		if (w > MAX_WIDTH) {
			int sw = MAX_WIDTH, sh = Math.round((float) h * MAX_WIDTH / w) & ~1;
			BufferedImage small = new BufferedImage(sw, sh, BufferedImage.TYPE_3BYTE_BGR);
			Graphics2D g = small.createGraphics();
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g.drawImage(img, 0, 0, sw, sh, null);
			g.dispose();
			img = small;
		}
		Writer wr = WRITER.get();
		if (wr == null) {
			wr = new Writer(writers.size());
			writers.add(wr);
			WRITER.set(wr);
			wr.jpeg = ImageIO.getImageWritersByFormatName("jpeg").next();
			wr.param = wr.jpeg.getDefaultWriteParam();
			wr.param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
			wr.param.setCompressionQuality(QUALITY);
		}
		ByteArrayOutputStream bytes = new ByteArrayOutputStream(1 << 19);
		try (MemoryCacheImageOutputStream ios = new MemoryCacheImageOutputStream(bytes)) {
			wr.jpeg.setOutput(ios);
			wr.jpeg.write(null, new IIOImage(img, null, null), wr.param);
		}
		if (wr.out == null || wr.written > PART_BYTES) {
			wr.next();
		}
		long offset = wr.written;
		bytes.writeTo(wr.out);
		wr.written += bytes.size();
		encoded.incrementAndGet();
		frameLines.add(String.format(Locale.ROOT, "%d,%s,%d,%d,%d,%d,%d,%s", index, wr.name, offset, bytes.size(), nanos, img.getWidth(), img.getHeight(), t));
	}

	/** Closes the files and writes the lists (once everything copied is written). */
	static List<String> finish() {
		List<String> out = new ArrayList<>();
		if (listener != null) {
			Minecraft.getInstance().getSoundManager().removeListener(listener);
			listener = null;
		}
		if (pool != null) {
			pool.shutdown();
			try {
				pool.awaitTermination(60, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
		synchronized (writers) {
			for (Writer w : writers) {
				try {
					if (w.out != null) {
						w.out.close();
					}
				} catch (IOException e) {
					problems.add("Closing " + w.name + ": " + e);
				}
				if (w.jpeg != null) {
					w.jpeg.dispose();
				}
			}
		}
		while (!free.isEmpty()) {
			free.poll().close();
		}
		try {
			synchronized (frameLines) {
				Files.write(dir.resolve("frames.csv"), frameLines, StandardCharsets.UTF_8);
			}
			synchronized (soundLines) {
				Files.write(dir.resolve("sounds.csv"), soundLines, StandardCharsets.UTF_8);
			}
		} catch (IOException e) {
			problems.add("Writing the lists: " + e);
		}
		out.add(String.format(Locale.ROOT, "Frames: %d copied, %d written, %d dropped (encoders behind), %d skipped (card busy); format %s",
				frames, encoded.get(), dropped, busy, format));
		synchronized (problems) {
			out.addAll(problems);
		}
		return out;
	}
}
