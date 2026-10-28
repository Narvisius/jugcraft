package io.github.jimbozoomer.jugcraft.client.tv;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.Map;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.jspecify.annotations.Nullable;

/**
 * The TVs' decoder: FFmpeg (LGPL build by Bytedeco) through JavaCV's FFmpegFrameGrabber. Loaded only by
 * {@link TvCodec}'s class loader, beside the JavaCV and FFmpeg jars it fetched; nothing else in the mod names this class
 * or JavaCV's (they aren't on the game's class path).
 */
public final class TvFfmpeg implements TvMedia {
	public TvFfmpeg() {
		avutil.av_log_set_level(avutil.AV_LOG_QUIET);
	}

	@Override
	public Reader open(String url, Map<String, String> options, boolean picture) {
		FFmpegFrameGrabber g = new FFmpegFrameGrabber(url);
		// Only the web: no local files, nothing but http(s) underneath (a playlist can't point the reader at a disk).
		g.setOption("protocol_whitelist", "http,https,tls,tcp,crypto");
		g.setOption("rw_timeout", "15000000");
		g.setOption("reconnect", "1");
		g.setOption("reconnect_streamed", "1");
		g.setOption("reconnect_on_network_error", "1");
		g.setOption("reconnect_delay_max", "4");
		// Rumble names its HLS pieces .tar (MPEG-TS inside): FFmpeg 7+ refuses pieces whose name isn't a video's unless
		// told any name will do (Justin's 27 October 2026 test). Only web protocols are allowed anyway (above).
		g.setOption("allowed_segment_extensions", "ALL");
		g.setOption("extension_picky", "0");
		for (Map.Entry<String, String> e : options.entrySet()) {
			g.setOption(e.getKey(), e.getValue());
		}
		if (picture) {
			g.setPixelFormat(avutil.AV_PIX_FMT_RGBA);
		} else {
			g.setSampleFormat(avutil.AV_SAMPLE_FMT_S16);
			g.setAudioChannels(1);
			g.setSampleRate(RATE);
		}
		return new R(g);
	}

	private static final class R implements Reader {
		private final FFmpegFrameGrabber g;
		private int w, h;

		R(FFmpegFrameGrabber g) {
			this.g = g;
		}

		@Override
		public void start() throws Exception {
			g.start();
			w = g.getImageWidth();
			h = g.getImageHeight();
		}

		@Override
		public boolean hasVideo() {
			return g.hasVideo();
		}

		@Override
		public boolean hasAudio() {
			return g.hasAudio();
		}

		@Override
		public int width() {
			return g.getImageWidth();
		}

		@Override
		public int height() {
			return g.getImageHeight();
		}

		@Override
		public double frameRate() {
			return g.getFrameRate();
		}

		@Override
		public long lengthMs() {
			return Math.max(0, g.getLengthInTime() / 1000);
		}

		@Override
		public void scaleTo(int w, int h) {
			this.w = w;
			this.h = h;
			g.setImageWidth(w);
			g.setImageHeight(h);
		}

		@Override
		public void seek(long ms) throws Exception {
			g.setTimestamp(Math.max(0, ms) * 1000L);
		}

		@Override
		public long skip() throws Exception {
			// decoded, not scaled or copied: catching up is several times faster than playing
			Frame f = g.grabFrame(false, true, false, false);
			return f == null ? END : f.timestamp / 1000;
		}

		@Override
		public long picture(ByteBuffer into) throws Exception {
			Frame f = g.grabImage();
			if (f == null) {
				return END;
			}
			if (f.image == null || f.imageWidth != w || f.imageHeight != h || !(f.image[0] instanceof ByteBuffer src)) {
				return NONE;
			}
			int row = w * 4, stride = f.imageStride > 0 ? f.imageStride : row;
			ByteBuffer s = src.duplicate();
			ByteBuffer d = into.duplicate();
			d.clear();
			if (stride == row) {
				s.position(0).limit(row * h);
				d.put(s);
			} else {
				for (int y = 0; y < h; y++) {
					s.limit(y * stride + row).position(y * stride);
					d.put(s);
				}
			}
			return f.timestamp / 1000;
		}

		@Override
		public short @Nullable [] sound(long[] at) throws Exception {
			Frame f = g.grabSamples();
			if (f == null) {
				return null;
			}
			if (f.samples == null || !(f.samples[0] instanceof ShortBuffer sb)) {
				return new short[0];
			}
			int channels = Math.max(1, f.audioChannels);
			ShortBuffer s = sb.duplicate();
			s.rewind();
			int n = s.remaining() / channels;
			short[] out = new short[n];
			for (int i = 0; i < n; i++) {
				int sum = 0;
				for (int c = 0; c < channels; c++) {
					sum += s.get(i * channels + c);
				}
				out[i] = (short) (sum / channels);
			}
			at[0] = f.timestamp / 1000;
			return out;
		}

		@Override
		public void close() {
			try {
				g.close();
			} catch (Exception ignored) {
				// closing anyway
			}
		}
	}
}
