package io.github.jimbozoomer.jugcraft.concordance.ritual;

import io.github.jimbozoomer.jugcraft.concordance.rules.EvidenceRule;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Matches a ritual's offerings against what lies in an anchor's slots. Pure and deterministic: offerings naming an
 * item are matched before those naming a tag, each from the lowest slot first, so the same slots always give the same
 * plan. The plan is worked out once, when the ritual starts, and the slots are locked until it ends, so what is
 * consumed at completion is exactly what was reserved.
 */
public final class Offerings {
	private Offerings() {
	}

	/** What one slot holds (an empty slot has count 0). */
	public record Slot(String item, int count) {
		public static final Slot EMPTY = new Slot("minecraft:air", 0);
	}

	/**
	 * How many to take from each slot, which item each slot gave, and the slot the transformed item comes from
	 * ({@code source}, or -1 when the ritual transforms nothing).
	 */
	public record Plan(List<Integer> take, List<String> items, int source) {
		public Plan {
			take = List.copyOf(take);
			items = List.copyOf(items);
			if (take.size() != items.size()) {
				throw new IllegalArgumentException("a plan names an item for each slot");
			}
		}

		public int total() {
			int total = 0;
			for (int each : take) {
				total += each;
			}
			return total;
		}
	}

	/** An offering not fully present: how many there are of how many it needs. */
	public record Shortfall(RitualDefinition.Offering offering, int have) {
	}

	/** The outcome: a plan when everything is present, else every shortfall. */
	public record Match(@Nullable Plan plan, List<Shortfall> missing) {
		public Match {
			missing = List.copyOf(missing);
		}
	}

	public static Match match(RitualDefinition ritual, List<Slot> slots, EvidenceRule.TagLookup tags) {
		int[] left = new int[slots.size()];
		int[] take = new int[slots.size()];
		for (int i = 0; i < slots.size(); i++) {
			left[i] = slots.get(i).count();
		}
		String source = ritual.result() instanceof RitualDefinition.Transform transform ? transform.from() : null;
		int sourceSlot = -1;
		List<RitualDefinition.Offering> order = new ArrayList<>(ritual.offerings());
		order.sort(Comparator.comparing(RitualDefinition.Offering::tag));
		List<Shortfall> missing = new ArrayList<>();
		for (RitualDefinition.Offering offering : order) {
			int need = offering.count();
			for (int i = 0; i < slots.size() && need > 0; i++) {
				if (left[i] == 0 || !offering.matches(slots.get(i).item(), tags)) {
					continue;
				}
				int taken = Math.min(need, left[i]);
				left[i] -= taken;
				take[i] += taken;
				need -= taken;
				if (sourceSlot < 0 && !offering.tag() && offering.item().equals(source)) {
					sourceSlot = i;
				}
			}
			if (need > 0) {
				missing.add(new Shortfall(offering, offering.count() - need));
			}
		}
		if (!missing.isEmpty() || source != null && sourceSlot < 0) {
			return new Match(null, missing);
		}
		List<Integer> plan = new ArrayList<>();
		List<String> items = new ArrayList<>();
		for (int i = 0; i < slots.size(); i++) {
			plan.add(take[i]);
			items.add(take[i] > 0 ? slots.get(i).item() : Slot.EMPTY.item());
		}
		return new Match(new Plan(plan, items, sourceSlot), List.of());
	}

	/**
	 * Whether a plan reserved earlier still holds in these slots: each slot it takes from still holds the same item,
	 * at least as many, and the source slot still holds the item that is transformed. The slots are locked while a
	 * ritual runs, so this is a guard against anything that got round the lock, checked again at completion.
	 */
	public static boolean holds(RitualDefinition ritual, Plan plan, List<Slot> slots) {
		if (plan.take().size() != slots.size()) {
			return false;
		}
		for (int i = 0; i < slots.size(); i++) {
			int take = plan.take().get(i);
			if (take > 0 && (slots.get(i).count() < take || !slots.get(i).item().equals(plan.items().get(i)))) {
				return false;
			}
		}
		if (ritual.result() instanceof RitualDefinition.Transform transform) {
			return plan.source() >= 0 && plan.source() < slots.size() && slots.get(plan.source()).item().equals(transform.from());
		}
		return true;
	}
}
