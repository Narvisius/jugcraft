package io.github.jimbozoomer.jugcraft.client;

import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import io.github.jimbozoomer.jugcraft.concordance.dreaming.DreamWispEntity;
import io.github.jimbozoomer.jugcraft.concordance.dreaming.Dreaming;
import io.github.jimbozoomer.jugcraft.concordance.spirits.ClockworkPorterEntity;
import io.github.jimbozoomer.jugcraft.concordance.spirits.GatheringShadeEntity;
import io.github.jimbozoomer.jugcraft.concordance.spirits.HearthlingEntity;
import io.github.jimbozoomer.jugcraft.concordance.spirits.StolasEntity;
import io.github.jimbozoomer.jugcraft.concordance.spirits.Workers;
import io.github.jimbozoomer.jugcraft.concordance.spirits.WorkshopHandEntity;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.phys.AABB;

/**
 * Familiars, spirits and constructs on the client (roadmap step 17): GeckoLib draws each from its own model and plays
 * the clip for the status the server synced. Nothing here decides what a worker does. Dream wisps (step 22) are drawn
 * here too: they only drift. So is Stolas (Concordance Chunk D): the owner's Flashjack rig in his raven form, drawn larger
 * with his eyes lit (D-S5, D-06).
 */
final class WorkerClient {
	/** Stolas's body, larger than the owner's Flashjack rig it is (tools/concordance_pacts.py BODY_SCALE; D-S5). */
	static final float STOLAS_SCALE = 1.4F;

	private WorkerClient() {
	}

	static void register() {
		EntityRendererRegistry.register(Workers.HEARTHLING,
				context -> new GeoEntityRenderer<HearthlingEntity, EntityRenderState>(context, Workers.HEARTHLING));
		EntityRendererRegistry.register(Workers.GATHERING_SHADE,
				context -> new GeoEntityRenderer<GatheringShadeEntity, EntityRenderState>(context, Workers.GATHERING_SHADE));
		EntityRendererRegistry.register(Workers.CLOCKWORK_PORTER,
				context -> new GeoEntityRenderer<ClockworkPorterEntity, EntityRenderState>(context, Workers.CLOCKWORK_PORTER));
		EntityRendererRegistry.register(Workers.WORKSHOP_HAND,
				context -> new GeoEntityRenderer<WorkshopHandEntity, EntityRenderState>(context, Workers.WORKSHOP_HAND));
		EntityRendererRegistry.register(Dreaming.WISP, context -> new GeoEntityRenderer<DreamWispEntity, EntityRenderState>(context, Dreaming.WISP));
		EntityRendererRegistry.register(Workers.STOLAS, context -> new GeoEntityRenderer<StolasEntity, EntityRenderState>(context, Workers.STOLAS) {
			{
				shadowRadius = 0.4F;
			}

			@Override
			protected AABB getBoundingBoxForCulling(StolasEntity stolas, float partialTick) {
				// His spread wings reach past his box, and he comes down from above it and leaves upwards (his arrive and
				// depart gestures): he stays drawn while any of him can be on screen.
				return stolas.getBoundingBox().inflate(1.2, 0.1, 1.2).expandTowards(0.0, 2.0, 0.0);
			}

			@Override
			public boolean shouldRender(StolasEntity stolas, Frustum culler, double camX, double camY, double camZ, float partialTicks) {
				// Leaving, he is drawn only where his departing gesture was fired: one who began watching him after it sees
				// him already gone, never perched until he vanishes (the unit review).
				return (!stolas.leaving() || departs(stolas)) && super.shouldRender(stolas, culler, camX, camY, camZ, partialTicks);
			}
		}.withRenderLayer(AutoGlowingGeoLayer::new).withScale(STOLAS_SCALE));
	}

	/**
	 * Whether this client was sent his departing gesture: his gesture controller was triggered with it, and keeps it once it
	 * has played (it holds its last frame). Read from the trigger, not from the clip drawn, since a clip starts only when he
	 * is drawn.
	 */
	static boolean departs(StolasEntity stolas) {
		AnimatableManager<StolasEntity> manager = stolas.getAnimatableInstanceCache().getManagerForId(stolas.getId());
		AnimationController<StolasEntity> gestures = manager.getAnimationControllers().get(StolasEntity.GESTURE_CONTROLLER);
		return gestures != null && StolasEntity.Gesture.DEPART.animation.equals(gestures.getCurrentRawAnimation());
	}
}
