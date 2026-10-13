package io.github.jimbozoomer.jugcraft.concordance.resource;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Shares a limited supply among the containers asking for it, the same way every time for the same requests (it
 * never depends on the order they were found in, the tick, or chance). The policy, for networks of devices:
 * <ol>
 * <li>Higher priority is served first. A priority level gets all it asks for before a lower one gets any.</li>
 * <li>Within the level that cannot all be served, each request gets the whole-unit floor of its share in proportion
 * to its demand; the units left over go one each to the largest remainders, ties to the smaller id.</li>
 * <li>Nobody gets more than they asked for. What no one asked for stays with the supplier
 * ({@link Allocation#excess}): an allocation never creates or loses anything.</li>
 * </ol>
 */
public final class Allocator {
	private Allocator() {
	}

	public record Request(String id, int priority, long demand) {
		public Request {
			if (demand < 0) {
				throw new IllegalArgumentException("negative demand");
			}
		}
	}

	/** What each request receives (by id, in id order) and what is left over. */
	public record Allocation(Map<String, Long> grants, long excess) {
	}

	public static Allocation allocate(long supply, List<Request> requests) {
		if (supply < 0) {
			throw new IllegalArgumentException("negative supply");
		}
		Map<String, Long> grants = new TreeMap<>();
		TreeMap<Integer, List<Request>> levels = new TreeMap<>(Comparator.reverseOrder());
		for (Request request : requests) {
			if (grants.put(request.id(), 0L) != null) {
				throw new IllegalArgumentException("duplicate request " + request.id());
			}
			levels.computeIfAbsent(request.priority(), unused -> new ArrayList<>()).add(request);
		}
		long left = supply;
		for (List<Request> level : levels.values()) {
			if (left == 0) {
				break;
			}
			level.sort(Comparator.comparing(Request::id));
			long demand = 0;
			for (Request request : level) {
				demand = Math.addExact(demand, request.demand());
			}
			if (demand <= left) {
				for (Request request : level) {
					grants.put(request.id(), request.demand());
				}
				left -= demand;
				continue;
			}
			// Proportional shares in exact arithmetic: floor(left * demand_i / demand), remainders kept for the rest.
			BigInteger total = BigInteger.valueOf(demand);
			BigInteger pool = BigInteger.valueOf(left);
			Map<String, BigInteger> remainders = new LinkedHashMap<>();
			long given = 0;
			for (Request request : level) {
				BigInteger[] share = pool.multiply(BigInteger.valueOf(request.demand())).divideAndRemainder(total);
				grants.put(request.id(), share[0].longValueExact());
				given += share[0].longValueExact();
				remainders.put(request.id(), share[1]);
			}
			List<String> order = new ArrayList<>(remainders.keySet());
			order.sort(Comparator.comparing((String id) -> remainders.get(id)).reversed().thenComparing(Comparator.naturalOrder()));
			long spare = left - given;
			for (int i = 0; i < spare; i++) {
				String id = order.get(i);
				grants.put(id, grants.get(id) + 1);
			}
			left = 0;
		}
		return new Allocation(java.util.Collections.unmodifiableMap(grants), left);
	}
}
