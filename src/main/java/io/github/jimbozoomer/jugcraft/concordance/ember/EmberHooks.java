package io.github.jimbozoomer.jugcraft.concordance.ember;

import java.util.function.Function;

/**
 * Where the client hands GeckoLib the fire sets' renderers (one per set; client/EmberClient sets it), so the shared
 * {@link EmberArmorItem} names no client class, not even GeckoLib's GeoRenderProvider (whose methods name client
 * renderers: the shared source set cannot compile against them), as the guns do (guns/GunHooks). On a dedicated server it
 * stays empty: GeckoLib asks for it only to draw a worn piece. The client sets it at startup; a client without it would
 * fail on the first piece it drew.
 */
public final class EmberHooks {
	/** The piece's GeoRenderProvider, as an Object (GeckoLib's own getRenderProvider() is typed so for the same reason). */
	public static Function<EmberArmorItem, Object> armor = item -> null;

	private EmberHooks() {
	}
}
