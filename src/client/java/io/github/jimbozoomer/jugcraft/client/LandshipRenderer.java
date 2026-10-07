package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.landship.Landship;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

/**
 * Draws the Landship from the parts tools/landship.py exports (assets/jugcraft/landship_quads.json): the hull facing
 * the way it drives, a track link every few pixels round each side frame (running as that side's track runs, so the
 * tracks counter-rotate when it turns on the spot), and the turret and cannon following where the driver looks, the
 * barrel kicking back when it fires.
 */
public class LandshipRenderer extends EntityRenderer<Landship, LandshipRenderer.State> {
	/** The track's path round each side frame, in pixels as {z, y}: keep in sync with TRACK_PATH in tools/landship.py. */
	private static final float[][] TRACK_PATH = {{-30, 1}, {28, 1}, {42, 16}, {32, 32}, {-36, 30}, {-42, 14}};
	private static final int LINK_PITCH = 6;
	private static final int TRACK_INNER = 17;
	private static final int TRACK_OUTER = 28;
	/** The turret's pivot, and the barrel's pivot from it, in pixels: keep in sync with tools/landship.py. */
	private static final float[] TURRET = {0, 36, -2};
	private static final float[] BARREL = {0, 6, 9};
	/** How far the barrel kicks back (pixels) and over how many ticks it runs out again. */
	private static final float RECOIL = 4.0F;
	private static final float RECOIL_TICKS = 8.0F;
	private static final float PERIMETER;
	private static final int LINKS;

	static {
		float total = 0;
		for (int i = 0; i < TRACK_PATH.length; i++) {
			float[] a = TRACK_PATH[i];
			float[] b = TRACK_PATH[(i + 1) % TRACK_PATH.length];
			total += (float) Math.hypot(b[0] - a[0], b[1] - a[1]);
		}
		PERIMETER = total;
		LINKS = (int) (total / LINK_PITCH);
	}

	public static final class State extends EntityRenderState {
		float yRot;
		float leftTrack;
		float rightTrack;
		float turretYaw;
		float barrelPitch;
		float recoil;
	}

	public LandshipRenderer(EntityRendererProvider.Context context) {
		super(context);
		shadowRadius = 2.0F;
	}

	/**
	 * The hull and barrel are longer than the hitbox, and the raised barrel's muzzle stands 3.75 blocks up (tools/check_mod_data.py
	 * checks the box holds it).
	 */
	@Override
	protected AABB getBoundingBoxForCulling(Landship landship, float partialTick) {
		return landship.getBoundingBox().inflate(2.0, 1.25, 2.0);
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(Landship landship, State state, float partialTick) {
		super.extractRenderState(landship, state, partialTick);
		state.yRot = landship.getYRot(partialTick);
		state.leftTrack = landship.leftTrack();
		state.rightTrack = landship.rightTrack();
		Entity driver = landship.getFirstPassenger();
		state.turretYaw = driver != null ? Mth.wrapDegrees(driver.getViewYRot(partialTick) - state.yRot) : 0.0F;
		state.barrelPitch = driver != null ? Mth.clamp(driver.getViewXRot(partialTick), Landship.BARREL_UP, Landship.BARREL_DOWN) : 0.0F;
		float since = landship.sinceShot() + partialTick;
		state.recoil = since < RECOIL_TICKS ? RECOIL * (1.0F - since / RECOIL_TICKS) : 0.0F;
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		QuadModel link = DecorQuads.get("landship_link");
		pose.pushPose();
		pose.rotateDegrees(Axis.YP, -state.yRot);
		draw("landship_body", pose, collector, state.lightCoords);
		if (link != null) {
			float middle = (TRACK_INNER + TRACK_OUTER) / 2.0F;
			track(link, middle, state.leftTrack, pose, collector, state.lightCoords);
			track(link, -middle, state.rightTrack, pose, collector, state.lightCoords);
		}
		pose.pushPose();
		at(pose, TURRET);
		pose.rotateDegrees(Axis.YP, -state.turretYaw);
		draw("landship_turret", pose, collector, state.lightCoords);
		at(pose, BARREL);
		pose.rotateDegrees(Axis.XP, state.barrelPitch);
		pose.translate(0, 0, -state.recoil / 16.0F);
		draw("landship_barrel", pose, collector, state.lightCoords);
		pose.popPose();
		pose.popPose();
		super.submit(state, pose, collector, camera);
	}

	/** One side's links, spread evenly round the path and moved along it by how far that track has run. */
	private static void track(QuadModel link, float x, float run, PoseStack pose, SubmitNodeCollector collector, int light) {
		float offset = Mth.positiveModulo(run * 16.0F, PERIMETER);
		for (int k = 0; k < LINKS; k++) {
			float d = Mth.positiveModulo(k * PERIMETER / LINKS + offset, PERIMETER);
			for (int i = 0; i < TRACK_PATH.length; i++) {
				float[] a = TRACK_PATH[i];
				float[] b = TRACK_PATH[(i + 1) % TRACK_PATH.length];
				float length = (float) Math.hypot(b[0] - a[0], b[1] - a[1]);
				if (d <= length) {
					float z = a[0] + (b[0] - a[0]) * d / length;
					float y = a[1] + (b[1] - a[1]) * d / length;
					pose.pushPose();
					pose.translate(x / 16.0F, y / 16.0F, z / 16.0F);
					// Lay the link along this stretch, its tread facing out of the frame.
					pose.rotateDegrees(Axis.XP, (float) Math.toDegrees(Math.atan2(-(b[1] - a[1]), b[0] - a[0])));
					link.submit(pose, collector, light);
					pose.popPose();
					break;
				}
				d -= length;
			}
		}
	}

	private static void at(PoseStack pose, float[] pixels) {
		pose.translate(pixels[0] / 16.0F, pixels[1] / 16.0F, pixels[2] / 16.0F);
	}

	private static void draw(String part, PoseStack pose, SubmitNodeCollector collector, int light) {
		QuadModel model = DecorQuads.get(part);
		if (model != null) {
			model.submit(pose, collector, light);
		}
	}
}
