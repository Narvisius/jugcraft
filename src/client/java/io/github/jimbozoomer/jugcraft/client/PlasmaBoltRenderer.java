package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.tower.crew.PlasmaBolt;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * The plasma bolt (tower/crew/PlasmaBolt) as plasma in air looks (Justin, 8 November 2026, take 1 of the bolt sheet, A
 * "ionised air"): excited nitrogen glows blue-violet and the hottest, densest part white, with no hard edge; it crackles
 * with thin filaments and the air behind it, still glowing, cools from blue-violet through violet to a dim orange as it
 * fades. Drawn as soft glows over the scene (the eyes layer, which lays them on by their opacity: Glows; written as
 * added light on black-backed textures it drew a black lump in the game), each turned to face the camera round the line of
 * flight: a white-hot core stretched along it in a blue-violet sheath and a wider, dimmer corona, a round glow at its
 * head (so it reads head-on too), filaments that crackle anew every tick, and the cooling wake back along its path.
 */
public class PlasmaBoltRenderer extends EntityRenderer<PlasmaBolt, PlasmaBoltRenderer.State> {
	/** (No longer drawn from a model; the layer stays registered so nothing else has to change.) */
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Jugcraft.id("plasma_bolt"), "main");
	private static final RenderType GLOW_TYPE = PlasmaGlows.GLOW_TYPE, FILAMENT_TYPE = PlasmaGlows.FILAMENT_TYPE;
	/** The colours (added light): white-hot, the nitrogen's blue-violet, the corona, cooling violet, the last orange. */
	private static final float[] WHITE = {1.0F, 0.97F, 1.0F}, SHEATH = {0.46F, 0.43F, 1.0F}, CORONA = {0.22F, 0.16F, 0.62F},
			VIOLET = {0.78F, 0.30F, 0.70F}, EMBER = {1.0F, 0.45F, 0.22F};
	/** How far back the wake reaches (blocks), and the bolt's middle over its feet. */
	private static final float WAKE = 3.2F, MID = 0.125F;

	public static class State extends EntityRenderState {
		float dx, dy = 0, dz = 1;
		/** How far it has come from the rifle (blocks): the wake reaches no further back. */
		float flown;
		float age;
		int seed;
	}

	public PlasmaBoltRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		mesh.getRoot().addOrReplaceChild("core", CubeListBuilder.create(), PartPose.ZERO);
		return LayerDefinition.create(mesh, 32, 32);
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(PlasmaBolt bolt, State state, float partialTick) {
		super.extractRenderState(bolt, state, partialTick);
		Vec3 v = bolt.getDeltaMovement();
		if (v.lengthSqr() < 1e-8) {
			v = Vec3.directionFromRotation(bolt.getXRot(partialTick), bolt.getYRot(partialTick));
		}
		v = v.normalize();
		state.dx = (float) v.x;
		state.dy = (float) v.y;
		state.dz = (float) v.z;
		state.flown = (float) bolt.flown(partialTick);
		state.age = bolt.tickCount + partialTick;
		state.seed = bolt.getId();
	}

	@Override
	protected int getBlockLightLevel(PlasmaBolt bolt, BlockPos pos) {
		return 15;
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		Vector3f axis = new Vector3f(state.dx, state.dy, state.dz);
		Vector3f view = camera.pos == null ? new Vector3f(0, 0, 1)
				: new Vector3f((float) (state.x - camera.pos.x), (float) (state.y + MID - camera.pos.y), (float) (state.z - camera.pos.z));
		if (view.lengthSquared() < 1e-6F) {
			view.set(0, 0, 1);
		}
		view.normalize();
		final float flown = state.flown, age = state.age;
		final int seed = state.seed;
		int tick = (int) age;
		float flicker = 0.88F + 0.12F * hash(seed, tick);
		pose.pushPose();
		pose.translate(0, MID, 0);
		collector.submitCustomGeometry(pose, GLOW_TYPE, (p, out) -> glows(p, out, axis, view, flown, flicker));
		collector.submitCustomGeometry(pose, FILAMENT_TYPE, (p, out) -> filaments(p, out, axis, view, seed, tick));
		pose.popPose();
		super.submit(state, pose, collector, camera);
	}

	/** The wake, the corona, sheath and core along the flight, and the round glow at its head (the bolt's middle at the origin). */
	static void glows(PoseStack.Pose p, VertexConsumer out, Vector3f axis, Vector3f view, float flown, float flicker) {
		Vector3f c = new Vector3f();
		// the wake: the air it went through, cooling and fading behind it
		float wake = Math.min(WAKE, Math.max(0, flown - 0.3F));
		int n = 16;
		for (int i = 0; i < n; i++) {
			float t = (i + 0.5F) / n, back = 0.35F + t * wake;
			if (back > flown) {
				break;
			}
			float[] rgb = cooling(t);
			float k = 0.42F * (float) Math.pow(1 - t, 1.6) * flicker;
			axial(p, out, new Vector3f(axis).mul(-back), axis, view, wake / n * 2.4F, 0.14F + 0.22F * t, rgb, k);
		}
		// the corona, the sheath and the white-hot core, stretched along the flight
		axial(p, out, c, axis, view, 1.8F, 0.52F, CORONA, 0.85F * flicker);
		axial(p, out, c, axis, view, 1.25F, 0.26F, SHEATH, 1.0F * flicker);
		axial(p, out, c, axis, view, 0.85F, 0.10F, WHITE, 1.0F);
		axial(p, out, new Vector3f(axis).mul(0.22F), axis, view, 0.4F, 0.13F, WHITE, 0.8F);
		// a round glow at its head (so it reads as a ball of plasma head-on)
		sprite(p, out, new Vector3f(axis).mul(0.25F), view, 0.55F, SHEATH, 0.55F * flicker);
		sprite(p, out, new Vector3f(axis).mul(0.25F), view, 0.2F, WHITE, 0.9F);
	}

	/** The filaments crackling off it (new each tick: {@code tick} and the bolt's {@code seed} pick them). */
	static void filaments(PoseStack.Pose p, VertexConsumer out, Vector3f axis, Vector3f view, int seed, int tick) {
		java.util.Random r = new java.util.Random(seed * 7919L + tick * 104729L);
		for (int f = 0; f < 6; f++) {
			Vector3f at = new Vector3f(axis).mul(-0.35F + r.nextFloat() * 0.7F);
			Vector3f d = new Vector3f(r.nextFloat() - 0.5F, r.nextFloat() - 0.5F, r.nextFloat() - 0.5F);
			d.sub(new Vector3f(axis).mul(d.dot(axis) * 0.7F));
			if (d.lengthSquared() < 1e-4F) {
				continue;
			}
			d.normalize();
			float length = 0.16F + r.nextFloat() * 0.22F;
			int segs = 5;
			for (int s = 0; s < segs; s++) {
				d.add((r.nextFloat() - 0.5F) * 1.1F, (r.nextFloat() - 0.5F) * 1.1F, (r.nextFloat() - 0.5F) * 1.1F).normalize();
				Vector3f next = new Vector3f(d).mul(length / segs).add(at);
				Vector3f mid = new Vector3f(at).add(next).mul(0.5F);
				float k = 0.85F * (1 - (float) s / segs);
				axial(p, out, mid, new Vector3f(next).sub(at).normalize(), view, length / segs * 1.15F, 0.03F, new float[] {0.78F, 0.74F, 1.0F}, k);
				at = next;
			}
		}
	}

	/** The wake's colour this far back along it (0 at the bolt, 1 at its end): blue-violet, violet, then a dim orange. */
	private static float[] cooling(float t) {
		float[] out = new float[3];
		for (int i = 0; i < 3; i++) {
			out[i] = SHEATH[i] * (1 - t) + VIOLET[i] * t * (1 - t) * 2 + EMBER[i] * t * t;
		}
		return out;
	}

	/** A soft glow stretched along {@code axis}, turned round it to face the view (PlasmaGlows: the baton's plasma is drawn the same). */
	private static void axial(PoseStack.Pose p, VertexConsumer out, Vector3f c, Vector3f axis, Vector3f view, float length, float width, float[] rgb, float k) {
		PlasmaGlows.axial(p, out, c, axis, view, length, width, rgb, k, 1);
	}

	/** A round soft glow facing the view, {@code size} across. */
	private static void sprite(PoseStack.Pose p, VertexConsumer out, Vector3f c, Vector3f view, float size, float[] rgb, float k) {
		PlasmaGlows.sprite(p, out, c, view, size, rgb, k, 1);
	}

	private static float hash(int a, int b) {
		int h = a * 73856093 ^ b * 19349663;
		h ^= h >>> 13;
		h *= 0x5bd1e995;
		h ^= h >>> 15;
		return (h & 0xFFFF) / 65535F;
	}
}
