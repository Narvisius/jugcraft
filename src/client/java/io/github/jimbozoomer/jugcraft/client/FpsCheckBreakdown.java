package io.github.jimbozoomer.jugcraft.client;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedFrame;
import jdk.jfr.consumer.RecordedStackTrace;
import jdk.jfr.consumer.RecordedThread;
import jdk.jfr.consumer.RecordingFile;

/**
 * Where each view's frame time went, from the check's own Flight Recorder recording (r186; until then I worked it out by
 * hand from his recordings): the render thread's samples while the view was measured, each put down to the outermost
 * of the mod's classes on its stack (a renderer), or to the game's own world drawing, or anything else; each one's share
 * of the samples times the view's average frame time is its milliseconds a frame. Samples come every 10 ms or so, so a
 * view's figures are good to about a tenth of a millisecond at best.
 */
public final class FpsCheckBreakdown {
	/** A measured view: its name, wall-clock start and end (ms), frames and their total time (ms). */
	public record Window(String name, long start, long end, int frames, double totalMs) {
	}

	private static final String RENDER_THREAD = "Render thread";
	private static final String OURS = "io.github.jimbozoomer.jugcraft.";
	/** Lines of the report each view gets. */
	static final int TOP = 10;

	private FpsCheckBreakdown() {
	}

	/** Reads the recording, writes every view's figures to {@code csv}, and gives the report's lines (top {@link #TOP} a view). */
	public static List<String> write(Path jfr, List<Window> windows, Path csv) throws IOException {
		List<Map<String, Integer>> counts = new ArrayList<>();
		int[] totals = new int[windows.size()];
		for (int i = 0; i < windows.size(); i++) {
			counts.add(new HashMap<>());
		}
		try (RecordingFile file = new RecordingFile(jfr)) {
			while (file.hasMoreEvents()) {
				RecordedEvent e = file.readEvent();
				if (!"jdk.ExecutionSample".equals(e.getEventType().getName())) {
					continue;
				}
				RecordedThread thread = e.getThread("sampledThread");
				if (thread == null || !RENDER_THREAD.equals(thread.getJavaName())) {
					continue;
				}
				long t = e.getStartTime().toEpochMilli();
				int w = find(windows, t);
				if (w < 0) {
					continue;
				}
				totals[w]++;
				counts.get(w).merge(owner(e.getStackTrace()), 1, Integer::sum);
			}
		}
		List<String> lines = new ArrayList<>();
		StringBuilder out = new StringBuilder("view,owner,samples,share,ms_per_frame\n");
		lines.add("Where each view's frame time went (render thread, from profile.jfr; ms a frame):");
		for (int i = 0; i < windows.size(); i++) {
			Window w = windows.get(i);
			if (totals[i] == 0 || w.frames() == 0) {
				lines.add(String.format(Locale.ROOT, "  %s: no samples", w.name()));
				continue;
			}
			double frameMs = w.totalMs() / w.frames();
			List<Map.Entry<String, Integer>> sorted = new ArrayList<>(counts.get(i).entrySet());
			sorted.sort((a, b) -> b.getValue() - a.getValue());
			StringBuilder top = new StringBuilder();
			int shown = 0;
			for (Map.Entry<String, Integer> entry : sorted) {
				double share = entry.getValue() / (double) totals[i];
				out.append(String.format(Locale.ROOT, "%s,%s,%d,%.4f,%.3f%n", w.name(), entry.getKey(), entry.getValue(), share, share * frameMs));
				if (shown++ < TOP) {
					top.append(String.format(Locale.ROOT, "%s%s %.2f", top.length() == 0 ? "" : ", ", entry.getKey(), share * frameMs));
				}
			}
			lines.add(String.format(Locale.ROOT, "  %-28s %5.2f ms (%d samples): %s", w.name(), frameMs, totals[i], top));
		}
		Files.writeString(csv, out, StandardCharsets.UTF_8);
		return lines;
	}

	private static int find(List<Window> windows, long t) {
		for (int i = 0; i < windows.size(); i++) {
			Window w = windows.get(i);
			if (t >= w.start() && t <= w.end()) {
				return i;
			}
		}
		return -1;
	}

	/** The outermost of the mod's classes on the stack (its top-level name), else the game's world drawing or the rest. */
	static String owner(RecordedStackTrace stack) {
		if (stack == null) {
			return "(no stack)";
		}
		List<RecordedFrame> frames = stack.getFrames();
		String ours = null;
		boolean level = false;
		for (RecordedFrame f : frames) {
			if (f.getMethod() == null || f.getMethod().getType() == null) {
				continue;
			}
			String type = f.getMethod().getType().getName();
			if (type.startsWith(OURS)) {
				ours = type;                               // the last one found is the outermost (frames run top first)
			} else if (type.equals("net.minecraft.client.renderer.GameRenderer") && "renderLevel".equals(f.getMethod().getName())) {
				level = true;
			}
		}
		if (ours != null) {
			int inner = ours.indexOf('$');                 // inner classes and lambdas ("X$$Lambda.0x...") count as their class
			String top = inner > 0 ? ours.substring(0, inner) : ours;
			return top.substring(top.lastIndexOf('.') + 1);
		}
		return level ? "(game: world)" : "(game: other)";
	}
}
