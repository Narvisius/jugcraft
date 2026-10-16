package io.github.jimbozoomer.jugcraft.client;

import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.Werewolf;
import io.github.jimbozoomer.jugcraft.lair.alpha.AlphaWerewolfEntity;
import io.github.jimbozoomer.jugcraft.lair.alpha.JugcraftAlpha;
import io.github.jimbozoomer.jugcraft.lair.alpha.PackmateEntity;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;

/**
 * The Alpha Werewolf and his pack on the client (docs/features/alpha-werewolf.md): GeckoLib draws him from his own model,
 * playing the clips for what the server synced, his glowmask (his moon-white eyes, shown once he wakes) drawn full
 * bright. His muzzle, arms and tail reach past his box, so he is kept drawn while any of him can be on screen; in the
 * trees (his Tree Leap) he is unseen, as every invisible body is. His Packmates are drawn as the brown werewolves are
 * ({@link WerewolfModel}, its fur and its red eyes), smaller by their scale, hunched with jaws open while they harry.
 * His shadows and his trail are the server's particles and blocks. Nothing here decides anything.
 */
final class AlphaWerewolfClient {
	/** His shadow, wide as his paws' spread. */
	static final float SHADOW = 1.2F;
	private static final Identifier PACK_FUR = Jugcraft.id("textures/entity/werewolf_" + Werewolf.Kind.BROWN.id + ".png");
	private static final RenderType PACK_EYES = RenderTypes.eyes(Jugcraft.id("textures/entity/werewolf_" + Werewolf.Kind.BROWN.id + "_eyes.png"));

	private AlphaWerewolfClient() {
	}

	static void register() {
		EntityRendererRegistry.register(JugcraftAlpha.ALPHA_WEREWOLF, context -> new GeoEntityRenderer<AlphaWerewolfEntity, EntityRenderState>(
				context, JugcraftAlpha.ALPHA_WEREWOLF) {
			{
				shadowRadius = SHADOW;
			}

			@Override
			protected AABB getBoundingBoxForCulling(AlphaWerewolfEntity alpha, float partialTick) {
				return alpha.getBoundingBox().inflate(1.0, 0.8, 1.0);
			}
		}.withRenderLayer(AutoGlowingGeoLayer::new));
		EntityRendererRegistry.register(JugcraftAlpha.PACKMATE, PackmateRenderer::new);
	}

	/** A Packmate: a brown werewolf's body and fur, its eyes glowing red. */
	static final class PackmateRenderer extends MobRenderer<PackmateEntity, WerewolfRenderer.State, WerewolfModel> {
		PackmateRenderer(EntityRendererProvider.Context context) {
			super(context, new WerewolfModel(context.bakeLayer(WerewolfModel.LAYER)), 0.7F);
			addLayer(new EyesLayer<>(this) {
				@Override
				public RenderType renderType() {
					return PACK_EYES;
				}
			});
		}

		@Override
		public WerewolfRenderer.State createRenderState() {
			return new WerewolfRenderer.State();
		}

		@Override
		public void extractRenderState(PackmateEntity packmate, WerewolfRenderer.State state, float partialTick) {
			super.extractRenderState(packmate, state, partialTick);
			state.hunting = packmate.isAggressive();
			state.kind = Werewolf.Kind.BROWN;
		}

		@Override
		public Identifier getTextureLocation(WerewolfRenderer.State state) {
			return PACK_FUR;
		}
	}
}
