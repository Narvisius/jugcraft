package io.github.jimbozoomer.jugcraft.party;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Saves parties in the world folder as a small versioned text file ({@code jugcraft/parties.txt}),
 * with no Minecraft types, so it can be tested on its own.
 *
 * <pre>
 * format=1
 * party=&lt;party uuid&gt;;&lt;leader uuid&gt;;&lt;member uuid&gt;,&lt;member uuid&gt;,...
 * name=&lt;player uuid&gt;;&lt;last known name&gt;
 * </pre>
 *
 * Writes go to a temporary file first and are then moved into place, so a crash mid-save never
 * leaves a half-written file. Invites are not saved; they expire within minutes.
 */
public final class PartyStore {
	public static final int FORMAT = 1;
	public static final String FILE_NAME = "parties.txt";

	private PartyStore() {
	}

	/** Thrown for a file that cannot be understood; the caller keeps its data and logs the problem. */
	public static final class FormatException extends IOException {
		public FormatException(String message) {
			super(message);
		}
	}

	public static String write(PartyManager manager) {
		StringBuilder out = new StringBuilder();
		out.append("# Jugcraft parties. Edit only while the server is stopped.\n");
		out.append("format=").append(FORMAT).append('\n');
		for (PartyManager.Party party : manager.parties()) {
			out.append("party=").append(party.id()).append(';').append(party.leader()).append(';');
			List<UUID> members = party.members();
			for (int i = 0; i < members.size(); i++) {
				if (i > 0) {
					out.append(',');
				}
				out.append(members.get(i));
			}
			out.append('\n');
		}
		manager.namesSnapshot().entrySet().stream()
				.sorted(Map.Entry.comparingByKey())
				.forEach(entry -> out.append("name=").append(entry.getKey()).append(';').append(entry.getValue()).append('\n'));
		return out.toString();
	}

	/** Parses {@code text} and replaces the manager's parties. On any error nothing is changed. */
	public static void read(PartyManager manager, String text) throws FormatException {
		List<PartyManager.Party> parties = new ArrayList<>();
		Map<UUID, String> names = new HashMap<>();
		boolean sawFormat = false;
		int lineNumber = 0;
		for (String raw : text.split("\n", -1)) {
			lineNumber++;
			String line = raw.strip();
			if (line.isEmpty() || line.startsWith("#")) {
				continue;
			}
			int eq = line.indexOf('=');
			if (eq < 0) {
				throw new FormatException("Line " + lineNumber + ": expected key=value");
			}
			String key = line.substring(0, eq);
			String value = line.substring(eq + 1);
			switch (key) {
				case "format" -> {
					int format;
					try {
						format = Integer.parseInt(value);
					} catch (NumberFormatException e) {
						throw new FormatException("Line " + lineNumber + ": bad format number");
					}
					if (format > FORMAT) {
						throw new FormatException("File format " + format + " is newer than this version of Jugcraft (" + FORMAT + ")");
					}
					sawFormat = true;
				}
				case "party" -> {
					String[] parts = value.split(";", -1);
					if (parts.length != 3 || parts[2].isEmpty()) {
						throw new FormatException("Line " + lineNumber + ": party needs id;leader;members");
					}
					List<UUID> members = new ArrayList<>();
					for (String member : parts[2].split(",")) {
						members.add(uuid(member, lineNumber));
					}
					parties.add(PartyManager.newParty(uuid(parts[0], lineNumber), uuid(parts[1], lineNumber), members));
				}
				case "name" -> {
					int semicolon = value.indexOf(';');
					if (semicolon < 0) {
						throw new FormatException("Line " + lineNumber + ": name needs uuid;name");
					}
					names.put(uuid(value.substring(0, semicolon), lineNumber), cleanName(value.substring(semicolon + 1)));
				}
				default -> {
					// Unknown keys from a future minor version are ignored rather than fatal.
				}
			}
		}
		if (!sawFormat) {
			throw new FormatException("Missing format line");
		}
		manager.load(parties, names);
	}

	public static void save(PartyManager manager, Path file) throws IOException {
		Files.createDirectories(file.getParent());
		Path temp = file.resolveSibling(file.getFileName() + ".tmp");
		Files.writeString(temp, write(manager), StandardCharsets.UTF_8);
		try {
			Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (AtomicMoveNotSupportedException e) {
			Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
		}
		manager.clearDirty();
	}

	/** Loads the file if it exists; a missing file means "no parties yet". */
	public static void load(PartyManager manager, Path file) throws IOException {
		if (!Files.isRegularFile(file)) {
			manager.load(List.of(), Map.of());
			return;
		}
		read(manager, Files.readString(file, StandardCharsets.UTF_8));
	}

	/** Player names only ever contain letters, digits and underscores; anything else is dropped. */
	static String cleanName(String name) {
		StringBuilder clean = new StringBuilder();
		for (int i = 0; i < name.length() && clean.length() < 16; i++) {
			char c = name.charAt(i);
			if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_') {
				clean.append(c);
			}
		}
		return clean.toString();
	}

	private static UUID uuid(String text, int lineNumber) throws FormatException {
		try {
			return UUID.fromString(text.strip());
		} catch (IllegalArgumentException e) {
			throw new FormatException("Line " + lineNumber + ": bad UUID '" + text + "'");
		}
	}
}
