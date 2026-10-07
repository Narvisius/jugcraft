package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.artillery.TowerGun;
import java.util.List;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.AABB;

/**
 * Draws a tower gun (batch 54) from the parts tools/tower_guns.py exports (assets/jugcraft/tower_gun_quads.json): its
 * fixed plinth, its turntable turned to the aim, and its cradle raised on the trunnions with the barrel recoiling back
 * through it when it fires.
 * Pivots in pixels from the gun's feet: keep in sync with tools/tower_guns.py (tools/check_mod_data.py checks them).
 */
public class TowerGunRenderer extends EntityRenderer<TowerGun, TowerGunRenderer.State> {
	private static final float RECOIL_TICKS = 8.0F;

	/** A gun's turntable and trunnion pivots, in pixels, and how far its barrel kicks back. */
	record Pivots(String id, float[] turntable, float[] trunnion, float recoil) {
	}

	static final List<Pivots> PIVOTS = List.of(
			pivots("bastion_mortar", new float[] {0, 9, 0}, new float[] {0, 27, 2}, 5.0F),
			pivots("bastion_autocannon", new float[] {0, 9, 0}, new float[] {0, 25, 6}, 2.0F),
			pivots("grand_mortar", new float[] {0, 12, 0}, new float[] {0, 50, 4}, 8.0F),
			pivots("fortress_rifle", new float[] {0, 12, 0}, new float[] {0, 34, 14}, 10.0F),
			pivots("triple_battery", new float[] {0, 12, 0}, new float[] {0, 32, 14}, 7.0F));

	public static final class State extends EntityRenderState {
		String id = "";
		float yRot;
		float aimYaw;
		float aimPitch;
		float recoil;
	}

	public TowerGunRenderer(EntityRendererProvider.Context context) {
		super(context);
		shadowRadius = 2.0F;
	}

	private static Pivots pivots(String id, float[] turntable, float[] trunnion, float recoil) {
		return new Pivots(id, turntable, trunnion, recoil);
	}

	private static Pivots of(String id) {
		for (Pivots pivots : PIVOTS) {
			if (pivots.id().equals(id)) {
				return pivots;
			}
		}
		return PIVOTS.get(0);
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	protected AABB getBoundingBoxForCulling(TowerGun gun, float partialTick) {
		// The barrel reaches well past the footprint: the Grand Mortar's muzzle stands 8.6 blocks up at full elevation.
		return gun.getBoundingBox().inflate(7.0, 5.0, 7.0);
	}

	@Override
	public void extractRenderState(TowerGun gun, State state, float partialTick) {
		super.extractRenderState(gun, state, partialTick);
		state.id = gun.spec().id();
		state.yRot = gun.getYRot(partialTick);
		state.aimYaw = gun.aimYaw(partialTick);
		state.aimPitch = gun.aimPitch();
		float since = gun.sinceShot() + partialTick;
		state.recoil = since < RECOIL_TICKS ? of(state.id).recoil() * (1.0F - since / RECOIL_TICKS) : 0.0F;
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		Pivots pivots = of(state.id);
		float[] table = pivots.turntable();
		float[] trunnion = pivots.trunnion();
		pose.pushPose();
		pose.rotateDegrees(Axis.YP, -state.yRot);
		draw(state.id + "_base", pose, collector, state.lightCoords);
		pose.popPose();
		pose.pushPose();
		pose.translate(table[0] / 16.0F, table[1] / 16.0F, table[2] / 16.0F);
		pose.rotateDegrees(Axis.YP, -state.aimYaw);
		draw(state.id + "_turntable", pose, collector, state.lightCoords);
		pose.translate((trunnion[0] - table[0]) / 16.0F, (trunnion[1] - table[1]) / 16.0F, (trunnion[2] - table[2]) / 16.0F);
		pose.rotateDegrees(Axis.XP, -state.aimPitch);
		// The cradle (breech or housing) stays put when the gun fires; the barrel recoils back through it.
		draw(state.id + "_cradle", pose, collector, state.lightCoords);
		pose.translate(0, 0, -state.recoil / 16.0F);
		draw(state.id + "_barrel", pose, collector, state.lightCoords);
		pose.popPose();
		super.submit(state, pose, collector, camera);
	}

	private static void draw(String part, PoseStack pose, SubmitNodeCollector collector, int light) {
		QuadModel model = DecorQuads.get(part);
		if (model != null) {
			model.submit(pose, collector, light);
		}
	}
}
