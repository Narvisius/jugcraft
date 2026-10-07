package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.artillery.CrewedGun;
import io.github.jimbozoomer.jugcraft.artillery.FlakGun;
import io.github.jimbozoomer.jugcraft.artillery.ObservationBalloon;
import io.github.jimbozoomer.jugcraft.artillery.SelfPropelledHowitzer;
import io.github.jimbozoomer.jugcraft.artillery.SiegeMortar;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

/**
 * Draws the big guns and the observation balloon (batch 51) from the parts tools/artillery.py exports
 * (assets/jugcraft/artillery_quads.json): each gun's mount turned to its aim and its cradle raised to its elevation, the
 * barrel recoiling back through the cradle when it fires, the howitzer's tracks running, and the balloon's winch cable
 * stretched down to its anchor.
 * Pivots in pixels: keep in sync with tools/artillery.py.
 */
public final class ArtilleryRenderers {
	private static final float[] MORTAR_TURNTABLE = {0, 10, 0};
	private static final float[] MORTAR_TRUNNION = {0, 30, 2};
	private static final float[] HOWITZER_GUN = {6, 32, 4};
	private static final float[] FLAK_HEAD = {0, 18, 0};
	private static final float[][] HOWITZER_TRACK = {{-38, 1}, {36, 1}, {44, 8}, {44, 16}, {36, 23}, {-38, 23}, {-46, 16}, {-46, 8}};
	private static final int HOWITZER_TRACK_INNER = 17;
	private static final int HOWITZER_TRACK_OUTER = 29;
	private static final int LINK_PITCH = 6;
	private static final float RECOIL_TICKS = 8.0F;

	private ArtilleryRenderers() {
	}

	/** What every gun's renderer needs. */
	public static class GunState extends EntityRenderState {
		float yRot;
		float aimYaw;
		float aimPitch;
		float recoil;
		float leftTrack;
		float rightTrack;
	}

	private static void extract(CrewedGun gun, GunState state, float partialTick, float recoil) {
		state.yRot = gun.getYRot(partialTick);
		state.aimYaw = gun.aimYaw(partialTick);
		state.aimPitch = gun.aimPitch();
		float since = gun.sinceShot() + partialTick;
		state.recoil = since < RECOIL_TICKS ? recoil * (1.0F - since / RECOIL_TICKS) : 0.0F;
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

	/** The siege mortar: the fixed base, the turntable turned to the aim and the barrel raised on its trunnions. */
	public static class Mortar extends EntityRenderer<SiegeMortar, GunState> {
		public Mortar(EntityRendererProvider.Context context) {
			super(context);
			shadowRadius = 1.8F;
		}

		@Override
		public GunState createRenderState() {
			return new GunState();
		}

		@Override
		protected AABB getBoundingBoxForCulling(SiegeMortar gun, float partialTick) {
			// The barrel stands 5.4 blocks up at full elevation (tools/check_mod_data.py checks every gun's reach).
			return gun.getBoundingBox().inflate(3.0, 3.25, 3.0);
		}

		@Override
		public void extractRenderState(SiegeMortar gun, GunState state, float partialTick) {
			super.extractRenderState(gun, state, partialTick);
			extract(gun, state, partialTick, 6.0F);
		}

		@Override
		public void submit(GunState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
			pose.pushPose();
			pose.rotateDegrees(Axis.YP, -state.yRot);
			draw("mortar_base", pose, collector, state.lightCoords);
			pose.popPose();
			pose.pushPose();
			at(pose, MORTAR_TURNTABLE);
			pose.rotateDegrees(Axis.YP, -state.aimYaw);
			draw("mortar_turntable", pose, collector, state.lightCoords);
			pose.translate((MORTAR_TRUNNION[0] - MORTAR_TURNTABLE[0]) / 16.0F, (MORTAR_TRUNNION[1] - MORTAR_TURNTABLE[1]) / 16.0F,
					(MORTAR_TRUNNION[2] - MORTAR_TURNTABLE[2]) / 16.0F);
			pose.rotateDegrees(Axis.XP, -state.aimPitch);
			draw("mortar_cradle", pose, collector, state.lightCoords);
			pose.translate(0, 0, -state.recoil / 16.0F);
			draw("mortar_barrel", pose, collector, state.lightCoords);
			pose.popPose();
			super.submit(state, pose, collector, camera);
		}
	}

	/** The self-propelled howitzer: the carriage, its running tracks and the gun turned within its arc. */
	public static class Howitzer extends EntityRenderer<SelfPropelledHowitzer, GunState> {
		private static final float PERIMETER;
		private static final int LINKS;

		static {
			float total = 0;
			for (int i = 0; i < HOWITZER_TRACK.length; i++) {
				float[] a = HOWITZER_TRACK[i];
				float[] b = HOWITZER_TRACK[(i + 1) % HOWITZER_TRACK.length];
				total += (float) Math.hypot(b[0] - a[0], b[1] - a[1]);
			}
			PERIMETER = total;
			LINKS = (int) (total / LINK_PITCH);
		}

		public Howitzer(EntityRendererProvider.Context context) {
			super(context);
			shadowRadius = 2.0F;
		}

		@Override
		public GunState createRenderState() {
			return new GunState();
		}

		@Override
		protected AABB getBoundingBoxForCulling(SelfPropelledHowitzer gun, float partialTick) {
			// The barrel's muzzle brake stands 7.7 blocks up at full elevation.
			return gun.getBoundingBox().inflate(5.0, 5.5, 5.0);
		}

		@Override
		public void extractRenderState(SelfPropelledHowitzer gun, GunState state, float partialTick) {
			super.extractRenderState(gun, state, partialTick);
			extract(gun, state, partialTick, 8.0F);
			state.leftTrack = gun.leftTrack();
			state.rightTrack = gun.rightTrack();
		}

		@Override
		public void submit(GunState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
			pose.pushPose();
			pose.rotateDegrees(Axis.YP, -state.yRot);
			draw("howitzer_body", pose, collector, state.lightCoords);
			QuadModel link = DecorQuads.get("landship_link");
			if (link != null) {
				float middle = (HOWITZER_TRACK_INNER + HOWITZER_TRACK_OUTER) / 2.0F;
				track(link, middle, state.leftTrack, pose, collector, state.lightCoords);
				track(link, -middle, state.rightTrack, pose, collector, state.lightCoords);
			}
			at(pose, HOWITZER_GUN);
			pose.rotateDegrees(Axis.YP, -Mth.wrapDegrees(state.aimYaw - state.yRot));
			pose.rotateDegrees(Axis.XP, -state.aimPitch);
			draw("howitzer_gun", pose, collector, state.lightCoords);
			pose.translate(0, 0, -state.recoil / 16.0F);
			draw("howitzer_barrel", pose, collector, state.lightCoords);
			pose.popPose();
			super.submit(state, pose, collector, camera);
		}

		private static void track(QuadModel link, float x, float run, PoseStack pose, SubmitNodeCollector collector, int light) {
			float offset = Mth.positiveModulo(run * 16.0F, PERIMETER);
			for (int k = 0; k < LINKS; k++) {
				float d = Mth.positiveModulo(k * PERIMETER / LINKS + offset, PERIMETER);
				for (int i = 0; i < HOWITZER_TRACK.length; i++) {
					float[] a = HOWITZER_TRACK[i];
					float[] b = HOWITZER_TRACK[(i + 1) % HOWITZER_TRACK.length];
					float length = (float) Math.hypot(b[0] - a[0], b[1] - a[1]);
					if (d <= length) {
						pose.pushPose();
						pose.translate(x / 16.0F, (a[1] + (b[1] - a[1]) * d / length) / 16.0F, (a[0] + (b[0] - a[0]) * d / length) / 16.0F);
						pose.rotateDegrees(Axis.XP, (float) Math.toDegrees(Math.atan2(-(b[1] - a[1]), b[0] - a[0])));
						link.submit(pose, collector, light);
						pose.popPose();
						break;
					}
					d -= length;
				}
			}
		}
	}

	/** The flak gun: the cross mount and the head turned and raised to its aim. */
	public static class Flak extends EntityRenderer<FlakGun, GunState> {
		public Flak(EntityRendererProvider.Context context) {
			super(context);
			shadowRadius = 1.2F;
		}

		@Override
		public GunState createRenderState() {
			return new GunState();
		}

		@Override
		protected AABB getBoundingBoxForCulling(FlakGun gun, float partialTick) {
			// The barrels stand 4 blocks up at full elevation.
			return gun.getBoundingBox().inflate(2.0, 2.5, 2.0);
		}

		@Override
		public void extractRenderState(FlakGun gun, GunState state, float partialTick) {
			super.extractRenderState(gun, state, partialTick);
			extract(gun, state, partialTick, 2.0F);
		}

		@Override
		public void submit(GunState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
			pose.pushPose();
			pose.rotateDegrees(Axis.YP, -state.yRot);
			draw("flak_mount", pose, collector, state.lightCoords);
			pose.popPose();
			pose.pushPose();
			at(pose, FLAK_HEAD);
			pose.rotateDegrees(Axis.YP, -state.aimYaw);
			pose.rotateDegrees(Axis.XP, -state.aimPitch);
			draw("flak_head", pose, collector, state.lightCoords);
			pose.translate(0, 0, -state.recoil / 16.0F);
			draw("flak_barrels", pose, collector, state.lightCoords);
			pose.popPose();
			super.submit(state, pose, collector, camera);
		}
	}

	/** The observation balloon: its basket, the envelope high above and the winch cable down to its anchor. */
	public static class Balloon extends EntityRenderer<ObservationBalloon, Balloon.State> {
		public static final class State extends EntityRenderState {
			float yRot;
			float cable;
		}

		public Balloon(EntityRendererProvider.Context context) {
			super(context);
			shadowRadius = 0.8F;
		}

		@Override
		public State createRenderState() {
			return new State();
		}

		@Override
		protected AABB getBoundingBoxForCulling(ObservationBalloon balloon, float partialTick) {
			return balloon.getBoundingBox().inflate(4.5, 8.0, 4.5).expandTowards(0, -40, 0);
		}

		@Override
		public void extractRenderState(ObservationBalloon balloon, State state, float partialTick) {
			super.extractRenderState(balloon, state, partialTick);
			state.yRot = balloon.getYRot(partialTick);
			state.cable = Math.max(0.0F, (float) (Mth.lerp(partialTick, balloon.yo, balloon.getY()) - balloon.syncedAnchorY()));
		}

		@Override
		public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
			pose.pushPose();
			pose.rotateDegrees(Axis.YP, -state.yRot);
			draw("observation_basket", pose, collector, state.lightCoords);
			draw("balloon_envelope", pose, collector, state.lightCoords);
			if (state.cable > 0.1F) {
				pose.scale(1.0F, state.cable, 1.0F);
				draw("balloon_cable", pose, collector, state.lightCoords);
			}
			pose.popPose();
			super.submit(state, pose, collector, camera);
		}
	}

	/** Whether the entity is a big gun's first rider, for the input sender. */
	static boolean crewing(Entity vehicle, Entity player) {
		return vehicle instanceof CrewedGun gun && gun.getFirstPassenger() == player;
	}
}
