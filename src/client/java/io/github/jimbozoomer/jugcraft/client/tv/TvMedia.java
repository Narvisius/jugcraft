package io.github.jimbozoomer.jugcraft.client.tv;

import java.nio.ByteBuffer;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * What the TVs need of a video decoder: a reader for a stream's picture and one for its sound. The decoder (FFmpeg,
 * through JavaCV: {@link TvFfmpeg}) is fetched the first time a TV plays and lives in its own class loader
 * ({@link TvCodec}), so nothing else names its classes; this is how the rest of the mod talks to it.
 */
public interface TvMedia {
	/** {@link Reader#picture}: the stream ended. */
	long END = Long.MIN_VALUE;
	/** {@link Reader#picture}: that frame had no picture (try again). */
	long NONE = Long.MIN_VALUE + 1;
	/** The sound's rate (mono, 16-bit). */
	int RATE = 48000;

	/** One stream being read (on its own thread). */
	interface Reader extends AutoCloseable {
		void start() throws Exception;

		boolean hasVideo();

		boolean hasAudio();

		/** The picture's size as it comes. */
		int width();

		int height();

		double frameRate();

		/** The stream's length (ms), 0 if not known. */
		long lengthMs();

		/** Scales the pictures to w x h. */
		void scaleTo(int w, int h);

		/**
		 * Jumps to {@code ms} into the stream: to the key frame at or before it (the pictures from there on decode; a
		 * key frame can be several seconds before {@code ms}, so the reader then {@link #skip}s up to where it's wanted).
		 */
		void seek(long ms) throws Exception;

		/** Decodes the next picture without making it ready to show (catching up): its time (ms), or {@link #END}. */
		long skip() throws Exception;

		/** The next picture, copied into {@code into} (w x h, RGBA rows): its time (ms), {@link #END} or {@link #NONE}. */
		long picture(ByteBuffer into) throws Exception;

		/** The next stretch of sound (mono, {@link #RATE}), its time (ms) in {@code at[0]}; null at the end. */
		short @Nullable [] sound(long[] at) throws Exception;

		@Override
		void close();
	}

	/**
	 * A reader of {@code url} with FFmpeg's options for opening it ({@code user_agent}, {@code headers}...); only web
	 * protocols are allowed under it.
	 */
	Reader open(String url, Map<String, String> options, boolean picture);
}
