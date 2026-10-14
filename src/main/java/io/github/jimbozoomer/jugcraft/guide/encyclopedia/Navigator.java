package io.github.jimbozoomer.jugcraft.guide.encyclopedia;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Which encyclopedia pages a player has and which one they are reading (Concordance Chunk A, A-04): the model both of
 * the encyclopedia's views share, so switching between them keeps the place. It takes the server's pages (newest answer
 * only), adds pages the client builds for itself (the key guide), keeps the page being read while it still exists, and
 * remembers the pages followed by a link for Back. Pure Java, so the server's game tests check it too.
 */
public final class Navigator {
	/** How many pages Back remembers. */
	public static final int HISTORY = 16;

	private List<EncyclopediaPayload.Page> pages = List.of();
	private String current = "";
	private final Deque<String> history = new ArrayDeque<>();
	private int applied = -1;
	private boolean received;

	/**
	 * Takes an answer to request {@code sequence}: the server's pages, then {@code local} ones. An answer older than the
	 * one last taken is ignored (false). The page being read stays if it is still there (except on the first answer,
	 * which opens on {@code prefer} or the first page, not on a page the client made while it waited); otherwise
	 * {@code prefer} (if given and present), otherwise the first.
	 */
	public boolean receive(int sequence, List<EncyclopediaPayload.Page> server, List<EncyclopediaPayload.Page> local, @Nullable String prefer) {
		if (sequence < applied) {
			return false;
		}
		boolean first = !received;
		applied = sequence;
		received = true;
		List<EncyclopediaPayload.Page> all = new ArrayList<>(server);
		all.addAll(local);
		pages = List.copyOf(all);
		if (first || find(current) == null) {
			current = prefer != null && find(prefer) != null ? prefer : pages.isEmpty() ? "" : pages.getFirst().id();
		}
		history.removeIf(id -> find(id) == null);
		return true;
	}

	/** Replaces the client's own pages, keeping the server's (the key guide changes when a key is bound). */
	public void local(List<EncyclopediaPayload.Page> server, List<EncyclopediaPayload.Page> local) {
		List<EncyclopediaPayload.Page> all = new ArrayList<>(server);
		all.addAll(local);
		pages = List.copyOf(all);
		if (find(current) == null) {
			current = pages.isEmpty() ? "" : pages.getFirst().id();
		}
	}

	/** Whether any answer has come. */
	public boolean received() {
		return received;
	}

	/** The request the answer last taken was for (-1 before any). */
	public int applied() {
		return applied;
	}

	public List<EncyclopediaPayload.Page> pages() {
		return pages;
	}

	/** The page being read; null when there are none. */
	public EncyclopediaPayload.@Nullable Page current() {
		return find(current);
	}

	/** Turns to the page {@code id} (a tab or a link); the page left is remembered for Back. False if there is no such page. */
	public boolean go(String id) {
		if (find(id) == null || id.equals(current)) {
			return false;
		}
		if (!current.isEmpty()) {
			history.push(current);
			while (history.size() > HISTORY) {
				history.removeLast();
			}
		}
		current = id;
		return true;
	}

	/** Turns to the page {@code step} tabs away, wrapping round (the views' Left and Right). */
	public boolean turn(int step) {
		if (pages.isEmpty()) {
			return false;
		}
		int at = 0;
		for (int i = 0; i < pages.size(); i++) {
			if (pages.get(i).id().equals(current)) {
				at = i;
			}
		}
		return go(pages.get(Math.floorMod(at + step, pages.size())).id());
	}

	/** Goes back to the page read before the last turn (one still sent, and not the one being read); false if there is none. */
	public boolean back() {
		while (!history.isEmpty()) {
			String id = history.pop();
			if (find(id) != null && !id.equals(current)) {
				current = id;
				return true;
			}
		}
		return false;
	}

	public boolean canGoBack() {
		return history.stream().anyMatch(id -> find(id) != null && !id.equals(current));
	}

	/** Forgets everything (the player left the world). */
	public void reset() {
		pages = List.of();
		current = "";
		history.clear();
		applied = -1;
		received = false;
	}

	private EncyclopediaPayload.@Nullable Page find(String id) {
		for (EncyclopediaPayload.Page page : pages) {
			if (page.id().equals(id)) {
				return page;
			}
		}
		return null;
	}
}
