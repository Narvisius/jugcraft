package io.github.jimbozoomer.jugcraft.warehouse;

import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

/**
 * What stands on a pallet: up to eight cases (two layers of four). Its look (a rack's LOAD state, the warehouse_load
 * models) is the kind of its cases (or mixed) and how many there are; wrapped or strapped once it is finished.
 */
public final class PalletLoad {
	public static final int MAX_CASES = 8;
	/** The load kinds, as tools/warehouse.py LOADS (the rack's LOAD = 1 + kind * 8 + cases - 1). */
	public static final int MIXED = 6;
	public static final Codec<PalletLoad> CODEC = CaseLoad.CODEC.listOf().xmap(PalletLoad::new, p -> p.cases);
	public static final StreamCodec<RegistryFriendlyByteBuf, PalletLoad> STREAM_CODEC = CaseLoad.STREAM_CODEC
			.apply(ByteBufCodecs.list(MAX_CASES)).map(PalletLoad::new, p -> p.cases);
	private final List<CaseLoad> cases;

	public PalletLoad() {
		this(List.of());
	}

	public PalletLoad(List<CaseLoad> cases) {
		this.cases = new ArrayList<>(cases);
	}

	public List<CaseLoad> cases() {
		return cases;
	}

	public int size() {
		return cases.size();
	}

	public boolean isEmpty() {
		return cases.isEmpty();
	}

	public boolean full() {
		return cases.size() >= MAX_CASES;
	}

	public void add(CaseLoad c) {
		if (!full()) {
			cases.add(c);
		}
	}

	public PalletLoad copy() {
		return new PalletLoad(cases);
	}

	/** The load kind index (tools/warehouse.py LOADS order: CaseKind's, then mixed). */
	public int kind() {
		if (cases.isEmpty()) {
			return CaseKind.CARTON.ordinal();
		}
		CaseKind first = cases.get(0).kind();
		for (CaseLoad c : cases) {
			if (c.kind() != first) {
				return MIXED;
			}
		}
		return first.ordinal();
	}

	/** Its look: the rack's LOAD value (0 for no cases; a pallet of drums, part W4: after the case loads). */
	public int look() {
		if (cases.isEmpty()) {
			return 0;
		}
		Drums.Kind drum = Drums.isLiquid(cases.get(0)) ? Drums.kindOf(cases.get(0).item()) : null;
		if (drum != null) {
			return Drums.LOAD0 + drum.ordinal() * Drums.PER_PALLET + Math.min(Drums.PER_PALLET, cases.size()) - 1;
		}
		return look(kind(), cases.size());
	}

	/** The liquid in its drums (mB), for a pallet of drums. */
	public long liquid() {
		long n = 0;
		for (CaseLoad c : cases) {
			if (Drums.isLiquid(c)) {
				n += c.count();
			}
		}
		return n;
	}

	public static int look(int kind, int cases) {
		return 1 + kind * MAX_CASES + Math.min(MAX_CASES, cases) - 1;
	}

	/** Whether the first case is of this item (pallets are kept to one item while they can be). */
	public boolean startsWith(ItemStack item) {
		return !cases.isEmpty() && cases.get(0).sameItem(item);
	}

	/** Everything on it as stacks. */
	public List<ItemStack> stacks() {
		List<ItemStack> out = new ArrayList<>();
		for (CaseLoad c : cases) {
			out.addAll(c.stacks());
		}
		return out;
	}

	/** A short line for players: "8 cases: 2,048 Iron Ingot" or "5 cases, 3 kinds". */
	public String describe() {
		if (cases.isEmpty()) {
			return "empty";
		}
		if (Drums.isLiquid(cases.get(0))) {
			return cases.size() + (cases.size() == 1 ? " drum: " : " drums: ") + Drums.buckets(liquid()) + " "
					+ Drums.name(cases.get(0).item()).getString();
		}
		java.util.Map<String, Integer> counts = new java.util.LinkedHashMap<>();
		for (CaseLoad c : cases) {
			counts.merge(c.item().getHoverName().getString(), c.count(), Integer::sum);
		}
		StringBuilder b = new StringBuilder();
		b.append(cases.size()).append(cases.size() == 1 ? " case: " : " cases: ");
		int k = 0;
		for (java.util.Map.Entry<String, Integer> e : counts.entrySet()) {
			if (k++ > 0) {
				b.append(", ");
			}
			b.append(String.format(java.util.Locale.ROOT, "%,d %s", e.getValue(), e.getKey()));
		}
		return b.toString();
	}
}
