package io.github.jimbozoomer.jugcraft.guns;

import com.geckolib.animatable.manager.AnimatableManager;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Where the client hands GeckoLib a gun's animation controllers and renderer (client/GunsClient sets both, and slice 10G's
 * other-hand trigger), so the shared {@link GunItem} names no client class, not even GeckoLib's GeoRenderProvider (whose
 * methods name client renderers: the shared source set cannot compile against them). On a dedicated server they stay
 * empty: GeckoLib asks for neither there.
 */
public final class GunHooks {
	public static BiConsumer<GunItem, AnimatableManager.ControllerRegistrar> controllers = (gun, registrar) -> {
	};
	/** The gun's GeoRenderProvider, as an Object (GeckoLib's own getRenderProvider() is typed so for the same reason). */
	public static Function<GunItem, Object> renderer = gun -> null;
	/**
	 * Slice 10G, two guns at once: right click reached the gun in the player's other hand ({@link GunItem#use}, on the
	 * client), so its trigger is pulled. Empty on a dedicated server.
	 */
	public static Runnable offTrigger = () -> {
	};

	private GunHooks() {
	}
}
