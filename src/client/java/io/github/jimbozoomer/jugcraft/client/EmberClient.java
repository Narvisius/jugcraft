package io.github.jimbozoomer.jugcraft.client;

import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.renderer.GeoArmorRenderer;
import io.github.jimbozoomer.jugcraft.client.ember.EmberArmorRenderer;
import io.github.jimbozoomer.jugcraft.concordance.ember.EmberArmorItem;
import io.github.jimbozoomer.jugcraft.concordance.ember.EmberHooks;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/**
 * Ember on the client: the fire sets' GeckoLib renderers, handed to GeckoLib through {@link EmberHooks} (as the guns' are,
 * GunsClient). One renderer draws a set's four pieces; it is made the first time one of them is drawn.
 */
final class EmberClient {
	/** Each piece's GeoRenderProvider, made once. */
	private static final Map<EmberArmorItem, GeoRenderProvider> PROVIDERS = new HashMap<>();
	/** Each set's renderer, by set. */
	private static final Map<String, EmberArmorRenderer> RENDERERS = new HashMap<>();

	private EmberClient() {
	}

	static void register() {
		EmberHooks.armor = item -> PROVIDERS.computeIfAbsent(item, key -> new GeoRenderProvider() {
			@Override
			public GeoArmorRenderer<?, ?> getGeoArmorRenderer(ItemStack stack, EquipmentSlot slot) {
				return RENDERERS.computeIfAbsent(key.set(), set -> new EmberArmorRenderer(set, key.model()));
			}
		});
	}
}
