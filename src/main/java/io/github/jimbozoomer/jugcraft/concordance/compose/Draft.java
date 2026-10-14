package io.github.jimbozoomer.jugcraft.concordance.compose;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;

/**
 * A spell being written in the composer screen (Chunk A, A-03): its parts in order, level by level (the spell, then its
 * branch, then that branch's), the modifiers joined to each part, the part chosen, and an undo history. Both of the
 * composer's views (the plain screen and the GuiLib workspace) edit one draft, so it survives switching between them.
 * <p>
 * A draft is only a way of writing text ({@link #text()}): the server checks and inscribes that text with the same
 * compiler a typed command uses, so nothing here decides whether a spell is legal. Words are put where the grammar
 * wants them (a delivery first, then a selection, operations, an ending last; a new delivery or selection replaces the
 * old), which keeps a spell built from buttons in order; typed text is taken as written. Pure Java.
 */
public final class Draft {
	/** How many edits can be undone. */
	public static final int UNDO_LIMIT = 64;
	/** The most levels: a spell and the branches the grammar allows below it. */
	public static final int MAX_LEVELS = Grammar.MAX_DEPTH + 1;

	/** One written part: a component's full id and the full ids of the modifiers joined to it. */
	public record Part(String id, List<String> modifiers) {
		public Part {
			modifiers = List.copyOf(modifiers);
		}

		/** As written: {@code light+prolong}. */
		public String written() {
			StringBuilder out = new StringBuilder(Composition.shortName(id));
			for (String modifier : modifiers) {
				out.append('+').append(Composition.shortName(modifier));
			}
			return out.toString();
		}
	}

	private record Snapshot(List<List<Part>> levels, @Nullable String typed, int level, int index) {
	}

	private final List<List<Part>> levels = new ArrayList<>();
	/** Text that does not read as a spell, kept as typed (it is what is checked); null when the parts are the spell. */
	private @Nullable String typed;
	private int level;
	private int index = -1;
	private final Deque<Snapshot> undo = new ArrayDeque<>();

	public Draft() {
		levels.add(new ArrayList<>());
	}

	/** The text the draft stands for: what is checked and inscribed. */
	public String text() {
		if (typed != null) {
			return typed;
		}
		List<String> written = new ArrayList<>();
		for (List<Part> parts : levels) {
			List<String> words = new ArrayList<>();
			for (Part part : parts) {
				words.add(part.written());
			}
			written.add(String.join(" ", words));
		}
		// An empty branch still shows as "then", so the check can say a branch needs a spell.
		while (written.size() > 1 && written.getLast().isEmpty() && written.get(written.size() - 2).isEmpty()) {
			written.removeLast();
		}
		return String.join(" then ", written).trim();
	}

	/** Whether nothing is written. */
	public boolean isEmpty() {
		return text().isEmpty();
	}

	/** The levels: the spell first, then each branch. Read-only copies. */
	public List<List<Part>> levels() {
		List<List<Part>> out = new ArrayList<>();
		for (List<Part> parts : levels) {
			out.add(List.copyOf(parts));
		}
		return out;
	}

	/** Whether the text was typed and does not read as a spell (so there are no parts to show). */
	public boolean typed() {
		return typed != null;
	}

	/**
	 * The chosen part's level; with none chosen, the first level emptied of its words before the last (where the grammar
	 * needs words next), or else the last.
	 */
	public int level() {
		if (index >= 0) {
			return level;
		}
		for (int i = 0; i < levels.size() - 1; i++) {
			if (levels.get(i).isEmpty()) {
				return i;
			}
		}
		return levels.size() - 1;
	}

	/** The chosen part's place in its level; -1 when none is chosen. */
	public int index() {
		return index;
	}

	public @Nullable Part chosen() {
		return index < 0 ? null : levels.get(level).get(index);
	}

	/** Chooses a part (or none, with an index of -1); a choice outside the draft chooses none. */
	public void choose(int level, int index) {
		if (level >= 0 && level < levels.size() && index >= 0 && index < levels.get(level).size()) {
			this.level = level;
			this.index = index;
		} else {
			this.index = -1;
		}
	}

	/**
	 * Replaces the draft with written text: typing in the text box, or loading an inscription. Text that reads as a
	 * spell becomes its parts; any other text is kept as typed, and the check will say what is wrong with it.
	 */
	public void read(String text, boolean undoable) {
		if (text.equals(text())) {
			return;
		}
		if (undoable) {
			remember();
		}
		levels.clear();
		index = -1;
		typed = null;
		CompositionParser.Parsed parsed = text.isBlank() ? null : CompositionParser.parse(text);
		if (parsed != null && parsed.composition() != null && parsed.composition().depth() < MAX_LEVELS) {
			for (Composition at = parsed.composition(); at != null; at = at.then()) {
				List<Part> parts = new ArrayList<>();
				for (Composition.Part part : at.parts()) {
					parts.add(new Part(part.component(), part.modifiers()));
				}
				levels.add(parts);
			}
		} else {
			levels.add(new ArrayList<>());
			typed = text.isBlank() ? null : text;
		}
	}

	/**
	 * Writes a word where the grammar wants it, in the chosen part's level (or the last): a delivery first and a
	 * selection after it (each replacing one already there), an operation after the last operation, an ending last
	 * (replacing one already there). A modifier is joined to the chosen part instead ({@link #join}). The word written
	 * is chosen. Nothing is written that would make the text longer than {@value Grammar#MAX_TEXT} characters. Returns
	 * whether anything changed.
	 *
	 * @param id the word's full id
	 * @param slot the slot the word fills
	 * @param slots the slot of each word already written, by full id (words it does not know are left where they are)
	 */
	public boolean add(String id, Slot slot, Function<String, @Nullable Slot> slots) {
		if (slot == Slot.MODIFIER) {
			return join(id);
		}
		remember();
		untype();
		int at = level();
		List<Part> parts = levels.get(at);
		int place = switch (slot) {
			case DELIVERY -> replace(parts, slots, Slot.DELIVERY, 0);
			case SELECTION -> replace(parts, slots, Slot.SELECTION, after(parts, slots, Slot.DELIVERY));
			case TERMINATION -> replace(parts, slots, Slot.TERMINATION, parts.size());
			default -> before(parts, slots, Slot.TERMINATION);
		};
		parts.add(place, new Part(id, List.of()));
		choose(at, place);
		return fits();
	}

	/** Removes the word of {@code slot} already in {@code parts}, if any, and says where the new one goes. */
	private static int replace(List<Part> parts, Function<String, @Nullable Slot> slots, Slot slot, int otherwise) {
		for (int i = 0; i < parts.size(); i++) {
			if (slots.apply(parts.get(i).id()) == slot) {
				parts.remove(i);
				return i;
			}
		}
		return Math.min(otherwise, parts.size());
	}

	/** Just after the last word of {@code slot} (or the start). */
	private static int after(List<Part> parts, Function<String, @Nullable Slot> slots, Slot slot) {
		int place = 0;
		for (int i = 0; i < parts.size(); i++) {
			if (slots.apply(parts.get(i).id()) == slot) {
				place = i + 1;
			}
		}
		return place;
	}

	/** Just before the first word of {@code slot} (or the end). */
	private static int before(List<Part> parts, Function<String, @Nullable Slot> slots, Slot slot) {
		for (int i = 0; i < parts.size(); i++) {
			if (slots.apply(parts.get(i).id()) == slot) {
				return i;
			}
		}
		return parts.size();
	}

	/** Joins a modifier to the chosen part, if it has room, does not have it already and the text stays in bounds. */
	public boolean join(String modifier) {
		Part chosen = chosen();
		if (chosen == null || chosen.modifiers().contains(modifier) || chosen.modifiers().size() >= Grammar.MAX_MODIFIERS) {
			return false;
		}
		remember();
		List<String> joined = new ArrayList<>(chosen.modifiers());
		joined.add(modifier);
		levels.get(level).set(index, new Part(chosen.id(), joined));
		return fits();
	}

	/** After an edit: true if the text is within the grammar's length; otherwise the edit is undone and false. */
	private boolean fits() {
		if (text().length() <= Grammar.MAX_TEXT) {
			return true;
		}
		undo();
		return false;
	}

	/** Takes the last modifier off the chosen part. */
	public boolean unjoin() {
		Part chosen = chosen();
		if (chosen == null || chosen.modifiers().isEmpty()) {
			return false;
		}
		remember();
		levels.get(level).set(index, new Part(chosen.id(), chosen.modifiers().subList(0, chosen.modifiers().size() - 1)));
		return true;
	}

	/** Removes the chosen part (with its modifiers); the part after it, or else before it, is chosen. */
	public boolean remove() {
		if (chosen() == null) {
			return false;
		}
		remember();
		List<Part> parts = levels.get(level);
		parts.remove(index);
		choose(level, Math.min(index, parts.size() - 1));
		return true;
	}

	/** Moves the chosen part earlier ({@code step} -1) or later (+1) in its level. */
	public boolean move(int step) {
		if (chosen() == null) {
			return false;
		}
		List<Part> parts = levels.get(level);
		int to = index + step;
		if (to < 0 || to >= parts.size()) {
			return false;
		}
		remember();
		Part part = parts.remove(index);
		parts.add(to, part);
		index = to;
		return true;
	}

	/** Whether another branch may be begun: every level has words and the grammar allows one more. */
	public boolean canBranch() {
		return typed == null && levels.size() < MAX_LEVELS && levels.stream().noneMatch(List::isEmpty);
	}

	/** Begins a branch after the last level; new words go there. */
	public boolean branch() {
		if (!canBranch()) {
			return false;
		}
		remember();
		levels.add(new ArrayList<>());
		index = -1;
		return true;
	}

	/** Removes the last branch with its words. */
	public boolean unbranch() {
		if (typed != null || levels.size() < 2) {
			return false;
		}
		remember();
		levels.removeLast();
		if (level >= levels.size()) {
			index = -1;
		}
		return true;
	}

	/** Empties the draft (undoable). */
	public boolean clear() {
		if (isEmpty() && levels.size() == 1) {
			return false;
		}
		remember();
		levels.clear();
		levels.add(new ArrayList<>());
		typed = null;
		index = -1;
		return true;
	}

	/** Undoes the last edit. */
	public boolean undo() {
		Snapshot last = undo.pollFirst();
		if (last == null) {
			return false;
		}
		levels.clear();
		for (List<Part> parts : last.levels()) {
			levels.add(new ArrayList<>(parts));
		}
		typed = last.typed();
		level = last.level();
		index = last.index();
		return true;
	}

	public boolean canUndo() {
		return !undo.isEmpty();
	}

	/** Forgets everything, history included (a new world). */
	public void reset() {
		levels.clear();
		levels.add(new ArrayList<>());
		typed = null;
		index = -1;
		undo.clear();
	}

	private void remember() {
		undo.addFirst(new Snapshot(levels(), typed, level, index));
		while (undo.size() > UNDO_LIMIT) {
			undo.removeLast();
		}
	}

	/** Typed text that did not read as a spell is dropped when a word is chosen from the buttons. */
	private void untype() {
		if (typed != null) {
			typed = null;
			levels.clear();
			levels.add(new ArrayList<>());
			index = -1;
		}
	}
}
