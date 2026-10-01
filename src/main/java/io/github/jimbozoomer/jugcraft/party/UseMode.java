package io.github.jimbozoomer.jugcraft.party;

/**
 * Whether an automated system (or a job such as a blueprint) is for its owner alone or shared with the
 * owner's party. Every automated Jugcraft system uses this one switch and {@link PartyManager#mayServe}.
 */
public enum UseMode {
	PERSONAL,
	PARTY;

	public String id() {
		return name().toLowerCase(java.util.Locale.ROOT);
	}
}
