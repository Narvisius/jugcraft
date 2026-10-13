package io.github.jimbozoomer.jugcraft.concordance.conclave;

/**
 * One requirement of a project stage (roadmap step 23): {@code deliver} {@code count} of an item into the project,
 * {@code practice} an activity {@code count} times (each carried through by any contributor), or {@code research}: a
 * contributor who understands the research presents it (always once, so one player can meet it).
 */
public record Requirement(String id, String type, String target, int count) {
	public static final String DELIVER = "deliver";
	public static final String PRACTICE = "practice";
	public static final String RESEARCH = "research";
}
