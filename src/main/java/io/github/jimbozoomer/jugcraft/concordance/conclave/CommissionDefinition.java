package io.github.jimbozoomer.jugcraft.concordance.conclave;

import org.jspecify.annotations.Nullable;

/**
 * A commission the Conclave posts (roadmap step 23, data: concordance/commission): the tradition asking, its tier (the
 * rank that may take it), what it asks (items delivered, or an activity practised in the last week), the renown it
 * brings the first time (less each repeat) and the reward it pays in goods. Each may be fulfilled once a week, and only
 * a few times in all.
 *
 * @param item the item to deliver, or null for a practice commission
 * @param activity the practice to have carried through in the last week, or null for a delivery
 */
public record CommissionDefinition(String id, String tradition, int tier, @Nullable String item, int count, @Nullable String activity, int renown,
		String reward, int rewardCount) {
	public boolean delivery() {
		return item != null;
	}
}
