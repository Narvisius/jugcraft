package io.github.jimbozoomer.jugcraft.client.arms;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.weapons.ThrownArm;
import java.util.Map;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;

/**
 * Draws a thrown arm in flight (Arms VIII, batch 59) as its own 3D model, the one it is held as (its item model outside
 * the inventory), turned to its flight: the javelin and the harpoon point first along it, the francisca tumbles end over
 * end, and the chakram spins flat. The model lies on the diagonal with its point to the top right, as its icon does, so
 * it is first turned point up.
 */
public class ThrownArmRenderer extends EntityRenderer<ThrownArm, ThrownArmRenderer.State> {
	/** How large each kind is drawn in flight: its 3D model's square, in blocks (tools/arms.py: FLIGHT). */
	public static final Map<String, Float> FLIGHT = Map.of("javelin", 1.4F, "francisca", 0.85F, "chakram", 0.8F, "harpoon", 1.4F);
	/** Degrees a tick the francisca tumbles end over end, and the chakram spins. */
	public static final float TUMBLE = 36.0F;
	public static final float SPIN = 48.0F;

	private final ItemModelResolver itemModels;

	public static final class State extends EntityRenderState {
		final ItemStackRenderState arm = new ItemStackRenderState();
		String kind = "javelin";
		float yRot;
		float xRot;
	}

	public ThrownArmRenderer(EntityRendererProvider.Context context) {
		super(context);
		itemModels = context.getItemModelResolver();
		shadowRadius = 0.15F;
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(ThrownArm arm, State state, float partialTick) {
		super.extractRenderState(arm, state, partialTick);
		state.kind = arm.kind();
		state.yRot = arm.getYRot(partialTick);
		state.xRot = arm.getXRot(partialTick);
		itemModels.updateForTopItem(state.arm, arm.getItem(), ItemDisplayContext.NONE, arm.level(), null, arm.getId());
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		if (!state.arm.isEmpty()) {
			float size = FLIGHT.getOrDefault(state.kind, 1.0F);
			pose.pushPose();
			pose.translate(0.0F, 0.25F, 0.0F);
			// A projectile's yaw turns from +z towards +x as its flight does; its pitch is up from level.
			pose.rotateDegrees(Axis.YP, state.yRot);
			switch (state.kind) {
				case "francisca" -> {
					// End over end, in the upright plane of its flight.
					pose.rotateDegrees(Axis.XP, state.ageInTicks * TUMBLE);
					pose.rotateDegrees(Axis.YP, 90.0F);
				}
				case "chakram" -> {
					// Flat, spinning.
					pose.rotateDegrees(Axis.YP, state.ageInTicks * SPIN);
					pose.rotateDegrees(Axis.XP, 90.0F);
				}
				default -> pose.rotateDegrees(Axis.XP, 90.0F - state.xRot);   // point first along its flight
			}
			pose.rotateDegrees(Axis.ZP, 45.0F);   // point up
			pose.scale(size, size, size);
			state.arm.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
			pose.popPose();
		}
		super.submit(state, pose, collector, camera);
	}
}
