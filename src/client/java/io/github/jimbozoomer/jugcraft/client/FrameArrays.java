package io.github.jimbozoomer.jugcraft.client;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import net.minecraft.client.Minecraft;

/**
 * Float arrays for one frame's drawing, used again the next frame instead of thrown away (Justin's JFR, 8 November 2026:
 * about 400 MB of garbage a second from the displays' per-frame buffers, a 10-16 ms collector pause every 4-5 s). An
 * array handed out is the caller's until the next frame's world extraction starts; it must not be kept past that (a
 * render state's buffers, a draw call's snapshot). Arrays not asked for again the frame after are let go, so odd sizes
 * don't pile up. Off the render thread it simply allocates.
 */
public final class FrameArrays {
	/** Handed out this frame. */
	private static final ArrayList<float[]> USED = new ArrayList<>();
	/** Free since the frame before, by length. */
	private static final Int2ObjectOpenHashMap<ArrayDeque<float[]>> FREE = new Int2ObjectOpenHashMap<>();
	private static Thread renderThread;

	private FrameArrays() {
	}

	/** A new frame's world extraction starts: last frame's arrays are free again, older ones let go. */
	public static void beginFrame() {
		renderThread = Thread.currentThread();
		FREE.clear();
		for (float[] a : USED) {
			FREE.computeIfAbsent(a.length, k -> new ArrayDeque<>()).push(a);
		}
		USED.clear();
	}

	/** An array of exactly {@code length} floats for this frame (contents left over: write before reading). */
	public static float[] floats(int length) {
		if (Thread.currentThread() != renderThread || length == 0) {
			return new float[length];
		}
		ArrayDeque<float[]> free = FREE.get(length);
		float[] a = free == null || free.isEmpty() ? new float[length] : free.pop();
		USED.add(a);
		return a;
	}

	/** The first {@code length} floats of {@code from}, in an array of exactly that length for this frame. */
	public static float[] copy(float[] from, int length) {
		float[] a = floats(length);
		System.arraycopy(from, 0, a, 0, length);
		return a;
	}

	/** Whether this thread draws (frame arrays are only reused there). */
	static boolean onRenderThread() {
		return Thread.currentThread() == renderThread && Minecraft.getInstance().isSameThread();
	}
}
