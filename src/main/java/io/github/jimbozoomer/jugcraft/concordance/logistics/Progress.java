package io.github.jimbozoomer.jugcraft.concordance.logistics;

import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * How a request stands (roadmap step 18). {@code carried} items are in transit and are held by the ledger, the one
 * authoritative record of them: never by a worker's brain, its animation or its navigation. {@code aboard} says whether
 * the claiming courier has taken them up (false: stranded, waiting at the post). A claim is a {@code worker} with a
 * {@code lease}: it lapses unless the worker renews it, so a removed or unloaded worker never holds a request for good.
 * {@code reserved} items at {@code source} are promised to this claim and to no other.
 */
public record Progress(int delivered, int carried, int reserved, @Nullable Place source, @Nullable UUID worker, long lease,
		boolean aboard, boolean returning, String note, long notBefore) {
	public static final Progress NEW = new Progress(0, 0, 0, null, null, 0L, false, false, "", 0L);
}
