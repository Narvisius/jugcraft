package io.github.jimbozoomer.jugcraft.concordance.conclave;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The rules of standing in the Starbound Conclave (roadmap step 23, docs/features/arcane-concordance-conclave.md). Pure:
 * the server says what happened and when, and these rules say what it is worth and why not. Renown comes only from
 * recognised contributions, each bounded: a research state once, a teaching once per entry and learner (and only a few
 * per learner), a project stage once, and a commission once a week with less each time and nothing after its last
 * repeat. Ranks ask for renown, breadth and variety together, so no one infinitely repeatable deed reaches any of them.
 */
public final class Conclave {
	/** One week of game time. */
	public static final long WEEK = 168_000L;
	/** A member who has contributed nothing for this long is lapsed until they contribute again (renown is kept). */
	public static final long OBLIGATION_TICKS = WEEK;
	/** Renown a research entry's states bring, once each. */
	public static final Map<String, Integer> RESEARCH_RENOWN = Map.of("encountered", 1, "observed", 2, "understood", 5, "mastered", 10);
	/** Renown for teaching one entry to one learner (they advanced by reading your notes). */
	public static final int TEACHING_RENOWN = 3;
	/** The most teachings one learner can bring their teachers in all. */
	public static final int TEACHING_PER_LEARNER = 4;
	/** The renown a tradition needs to count towards a rank's breadth. */
	public static final int TRADITION_RENOWN = 5;
	/** A commission's renown each time it is fulfilled, in percent: then it is exhausted. */
	public static final List<Integer> DIMINISHING = List.of(100, 50, 25);

	/** What a contribution brought: renown (0 when refused), the reason when refused (else ""), the standing after. */
	public record Award(int renown, String reason, Standing next) {
		public boolean given() {
			return reason.isEmpty();
		}
	}

	private Conclave() {
	}

	/** Why the oath cannot be sworn now, or the empty string. */
	public static String oath(Standing standing, boolean firstLight) {
		if (standing.member()) {
			return "already";
		}
		return firstLight ? "" : "unknown";
	}

	/** The rank {@code standing} holds: the highest whose renown, breadth and variety it meets. */
	public static Rank rank(Standing standing) {
		Rank held = Rank.ASPIRANT;
		for (Rank rank : Rank.values()) {
			if (standing.renown() >= rank.renown && traditions(standing) >= rank.traditions && standing.kinds().size() >= rank.kinds) {
				held = rank;
			}
		}
		return held;
	}

	/** Traditions in which at least {@link #TRADITION_RENOWN} renown has been earned. */
	public static int traditions(Standing standing) {
		int counted = 0;
		for (int renown : standing.traditions().values()) {
			if (renown >= TRADITION_RENOWN) {
				counted++;
			}
		}
		return counted;
	}

	/** Whether a member has met their obligation: some contribution within the last {@link #OBLIGATION_TICKS}. */
	public static boolean goodStanding(Standing standing, long now) {
		return standing.member() && now - standing.lastContribution() <= OBLIGATION_TICKS;
	}

	/** A research entry reached {@code state} (from the research engine): renown once per entry and state. */
	public static Award research(Standing standing, String research, String tradition, String state, long now) {
		if (!standing.member()) {
			return new Award(0, "not_member", standing);
		}
		int points = RESEARCH_RENOWN.getOrDefault(state, 0);
		String key = "research:" + research + ":" + state;
		if (points == 0) {
			return new Award(0, "nothing", standing);
		}
		if (standing.awarded(key) > 0) {
			return new Award(0, "already", standing);
		}
		return new Award(points, "", standing.add(Kind.RESEARCH, tradition, key, points, now));
	}

	/** Why {@code commission} cannot be fulfilled now, or the empty string. */
	public static String mayFulfil(Standing standing, CommissionDefinition commission, long now) {
		if (!standing.member()) {
			return "not_member";
		}
		if (rank(standing).tier < commission.tier()) {
			return "rank";
		}
		if (commission.tier() > 1 && !goodStanding(standing, now)) {
			return "lapsed";
		}
		String key = "commission:" + commission.id();
		if (standing.awarded(key) >= DIMINISHING.size()) {
			return "exhausted";
		}
		long last = standing.recent(key);
		if (last != Long.MIN_VALUE && now - last < WEEK) {
			return "cooldown";
		}
		if (!commission.delivery()) {
			long practised = standing.recent("practice:" + commission.activity());
			if (practised == Long.MIN_VALUE || now - practised > WEEK) {
				return "not_practised";
			}
		}
		return "";
	}

	/** The renown {@code commission} brings the {@code times}+1th time (0 once exhausted). */
	public static int commissionRenown(CommissionDefinition commission, int times) {
		return times < DIMINISHING.size() ? Math.max(1, commission.renown() * DIMINISHING.get(times) / 100) : 0;
	}

	/** Fulfils {@code commission} (its items are the caller's to take once this is given). */
	public static Award commission(Standing standing, CommissionDefinition commission, long now) {
		String reason = mayFulfil(standing, commission, now);
		if (!reason.isEmpty()) {
			return new Award(0, reason, standing);
		}
		String key = "commission:" + commission.id();
		int points = commissionRenown(commission, standing.awarded(key));
		return new Award(points, "", standing.add(Kind.COMMISSION, commission.tradition(), key, points, now).did(key, now));
	}

	/** A member carried {@code activity} through (for practice commissions); nothing else changes. */
	public static Standing practised(Standing standing, String activity, long now) {
		return standing.member() ? standing.did("practice:" + activity, now) : standing;
	}

	/**
	 * {@code learner} advanced {@code research} by reading the author's notes: renown for the author once per entry and
	 * learner, and at most {@link #TEACHING_PER_LEARNER} for one learner in all. Never oneself.
	 */
	public static Award teaching(Standing author, UUID authorId, String research, String tradition, UUID learner, long now) {
		if (!author.member()) {
			return new Award(0, "not_member", author);
		}
		if (authorId.equals(learner)) {
			return new Award(0, "self", author);
		}
		String key = "teaching:" + research + ":" + learner;
		if (author.awarded(key) > 0) {
			return new Award(0, "already", author);
		}
		String tally = "learner:" + learner;
		if (author.awarded(tally) >= TEACHING_PER_LEARNER) {
			return new Award(0, "learner_limit", author);
		}
		return new Award(TEACHING_RENOWN, "", author.add(Kind.TEACHING, tradition, key, TEACHING_RENOWN, now).count(tally));
	}

	/** A project stage (or the whole project, {@code stage} "complete") finished with this member contributing: once. */
	public static Award project(Standing standing, String project, String stage, String tradition, int points, long now) {
		if (!standing.member()) {
			return new Award(0, "not_member", standing);
		}
		String key = "project:" + project + ":" + stage;
		if (standing.awarded(key) > 0) {
			return new Award(0, "already", standing);
		}
		return new Award(points, "", standing.add(Kind.PROJECT, tradition, key, points, now));
	}
}
