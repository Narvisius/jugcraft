package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.drone.DepotDisplayBlockEntity;
import io.github.jimbozoomer.jugcraft.tower.HoloProjectorBlock;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * The Holo Projector (tower/HoloProjectorBlock; work list item 15, approved 23 October 2026): its housing from
 * tools/holo_projector.py turned to its facing, and over it the hologram, all in one sorted see-through batch
 * ({@link HoloHeadMesh}'s texture):
 *
 * <ul>
 * <li>the head, 3.4 blocks from chin to crown, its turning point 5.2 over the floor, turned to the nearest player within
 * 20 blocks (smoothly; nobody near, it faces where the projector faces, a little down); each eye on that player;</li>
 * <li>drawn as the approved sheet: see-through and brighter at its edges, lit softly from a light fixed to the head, scan
 * lines and a slow band running up it, its neck fading away under the choker; only its nearest surface shows
 * ({@link HoloHeadMesh#seen});</li>
 * <li>the hair's locks and the brows as textured cards, the iris and pupil drawn on the eyeballs round the gaze;</li>
 * <li>the ring of solid letters ("You are being watched", twice round) turning once in 24 seconds, leaning back to face
 * the people below, fainter where seen from behind;</li>
 * <li>the light from the lens: a streaked cone opening out and twelve rays from the emitters, all fading away well under
 * the head, and motes rising in it.</li>
 * </ul>
 */
public final class HoloProjectorRenderer implements BlockEntityRenderer<DepotDisplayBlockEntity, HoloProjectorRenderer.State> {
	static final String MESH_NAME = "holo_projector";
	static final Identifier TEXTURE = Jugcraft.id("textures/entity/holo_projector.png");
	private static final RenderType HOUSING = RenderTypes.entityCutout(TEXTURE);
	static final Identifier HOLO_TEXTURE = Jugcraft.id("textures/entity/holo_head.png");
	private static final RenderType HOLO = GlowTypes.glow(HOLO_TEXTURE);

	/** How far it looks for someone to watch (blocks, from the head). */
	static final double WATCH = 20;
	/** How far down the head may pitch (or up). */
	static final float PITCH_MAX = 0.9f;
	/**
	 * Seconds between rebuilds of the hologram's quads (Justin's frame rate, 7 November 2026, "Hologram rebuilt less"):
	 * 15 a second while someone is close and it follows them (it was 30); 5 a second when nobody is near and its head has
	 * settled, or the camera is more than {@link #FAR} blocks off, where its slow scan and drift look the same.
	 */
	static final double REBUILD = 1 / 15.0, REBUILD_IDLE = 1 / 5.0;
	static final double FAR = 40;

	public HoloProjectorRenderer(BlockEntityRendererProvider.Context context) {
	}

	public static final class State extends BlockEntityRenderState {
		float facing;
		/** The hologram's quads, 4 corners each: x, y, z, u, v; their colours; and how many corners. */
		float[] holo = new float[0];
		int[] colour = new int[0];
		int corners;
		boolean drawn;
		/** What it is watching (block-local), and its head's yaw and pitch, for the checks. */
		double[] target = new double[3];
		float yaw, pitch;
		boolean watching;
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public int getViewDistance() {
		return 96;
	}

	/** The head floats five blocks over its block. */
	@Override
	public boolean shouldRenderOffScreen() {
		return true;
	}

	/** Each projector's head as it turns (by position): its yaw and pitch now, and when they were last moved. */
	private static final class Turn {
		float yaw, pitch;
		double time = Double.NaN;
		float[] buffer = new float[0];
		int[] colours = new int[0];
		int corners;
		double built = Double.NaN;
		// for putting the quads in order: their keys, and the arrays they are put into (swapped with the two above)
		long[] keys = new long[0];
		float[] spare = new float[0];
		int[] spareColours = new int[0];
	}

	/**
	 * Puts the hologram's quads in order, the farthest from the camera first (r181; Justin, 8 November 2026: "whatever
	 * you did to the hologram of this head, you fucked the textures up"). Its glow has drawn unsorted since r176 to save
	 * the game's sort every frame, and so the inside of the head (the mouth, the far side, the hair behind) came out on
	 * top of the face. Sorted here instead, once a build (15 times a second, 5 when idle or far), with no garbage: the
	 * face draws over what is behind it as it did before r176. {@code cx, cy, cz}: the camera in the block's frame.
	 */
	static void backToFront(Turn t, float cx, float cy, float cz) {
		int quads = t.corners / 4;
		if (quads < 2) {
			return;
		}
		final int f = HoloProjectorGeometry.Builder.FLOATS, qf = f * 4;
		if (t.keys.length < quads) {
			t.keys = new long[quads + quads / 4];
		}
		float[] q = t.buffer;
		for (int i = 0; i < quads; i++) {
			int o = i * qf;
			float x = (q[o] + q[o + f] + q[o + 2 * f] + q[o + 3 * f]) * 0.25f - cx;
			float y = (q[o + 1] + q[o + f + 1] + q[o + 2 * f + 1] + q[o + 3 * f + 1]) * 0.25f - cy;
			float z = (q[o + 2] + q[o + f + 2] + q[o + 2 * f + 2] + q[o + 3 * f + 2]) * 0.25f - cz;
			// a positive float's bits sort as its value: take them from the largest so the farthest comes first
			t.keys[i] = (long) (Integer.MAX_VALUE - Float.floatToIntBits(x * x + y * y + z * z)) << 32 | i;
		}
		java.util.Arrays.sort(t.keys, 0, quads);
		if (t.spare.length < q.length) {
			t.spare = new float[q.length];
		}
		if (t.spareColours.length < t.colours.length) {
			t.spareColours = new int[t.colours.length];
		}
		for (int k = 0; k < quads; k++) {
			int i = (int) t.keys[k];
			System.arraycopy(q, i * qf, t.spare, k * qf, qf);
			System.arraycopy(t.colours, i * 4, t.spareColours, k * 4, 4);
		}
		float[] fs = t.buffer;
		t.buffer = t.spare;
		t.spare = fs;
		int[] cs = t.colours;
		t.colours = t.spareColours;
		t.spareColours = cs;
	}

	private static final Map<Long, Turn> TURNS = new HashMap<>();

	/** The way (radians, from +z towards +x) the master's facing points. */
	static float facingYaw(Direction facing) {
		return (float) Math.atan2(facing.getStepX(), facing.getStepZ());
	}

	private static float wrap(float a) {
		while (a > Math.PI) {
			a -= (float) (2 * Math.PI);
		}
		while (a < -Math.PI) {
			a += (float) (2 * Math.PI);
		}
		return a;
	}

	@Override
	public void extractRenderState(DepotDisplayBlockEntity be, State state, float partialTick, Vec3 cameraPos,
			ModelFeatureRenderer.CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(be, state, crumbling);
		state.drawn = false;
		BlockState bs = be.getBlockState();
		Level level = be.getLevel();
		HoloHeadMesh mesh = HoloHeadMesh.get();
		if (!(bs.getBlock() instanceof HoloProjectorBlock) || level == null || mesh == null) {
			return;
		}
		BlockPos pos = be.getBlockPos();
		Direction facing = bs.getValue(HoloProjectorBlock.FACING);
		state.facing = (float) Math.toRadians(180 - facing.toYRot());
		double now = (level.getGameTime() + partialTick) / 20.0;
		Vec3 pivot = new Vec3(pos.getX() + 0.5, pos.getY() + HoloProjectorGeometry.HEAD_Y, pos.getZ() + 0.5);
		// whom it watches: the nearest player within 20 blocks of the head
		Player watched = null;
		double best = WATCH * WATCH;
		for (Player p : level.players()) {
			if (p.isSpectator() || !p.isAlive()) {
				continue;
			}
			double d = p.getEyePosition(partialTick).distanceToSqr(pivot);
			if (d <= best) {
				best = d;
				watched = p;
			}
		}
		Vec3 target;
		if (watched != null) {
			target = watched.getEyePosition(partialTick);
		} else {
			// nobody near: where the projector faces, a little down, as if at someone coming in
			target = pivot.add(facing.getStepX() * 12.0, -3.5, facing.getStepZ() * 12.0);
		}
		state.watching = watched != null;
		double dx = target.x - pivot.x, dy = target.y - pivot.y, dz = target.z - pivot.z;
		float wantYaw = (float) Math.atan2(dx, dz);
		float wantPitch = (float) Math.max(-PITCH_MAX, Math.min(PITCH_MAX, Math.atan2(-dy, Math.hypot(dx, dz))));
		Turn turn = TURNS.computeIfAbsent(pos.asLong(), k -> new Turn());
		if (Double.isNaN(turn.time) || now < turn.time || now - turn.time > 5) {
			turn.yaw = wantYaw;
			turn.pitch = wantPitch;
		} else {
			float dt = (float) Math.min(0.25, now - turn.time);
			float ease = (float) (1 - Math.exp(-dt * 5));
			float dyaw = wrap(wantYaw - turn.yaw) * ease, dpitch = (wantPitch - turn.pitch) * ease;
			float most = 2.4f * dt;
			turn.yaw = wrap(turn.yaw + Math.max(-most, Math.min(most, dyaw)));
			turn.pitch += Math.max(-most, Math.min(most, dpitch));
		}
		turn.time = now;
		state.yaw = turn.yaw;
		state.pitch = turn.pitch;
		Vec3 local = target.subtract(pos.getX(), pos.getY(), pos.getZ());
		state.target[0] = local.x;
		state.target[1] = local.y;
		state.target[2] = local.z;
		// the hologram is some 35,000 quads: built 15 times a second (5 when idle or far), drawn every frame
		boolean settled = Math.abs(wrap(wantYaw - turn.yaw)) < 0.01f && Math.abs(wantPitch - turn.pitch) < 0.01f;
		boolean far = cameraPos.distanceToSqr(pivot) > FAR * FAR;
		double every = (far || (watched == null && settled)) ? REBUILD_IDLE : REBUILD;
		if (Double.isNaN(turn.built) || now < turn.built || now - turn.built >= every) {
			HoloProjectorGeometry.Builder b = new HoloProjectorGeometry.Builder(turn.buffer, turn.colours);
			HoloProjectorGeometry.build(mesh, b, turn.yaw, turn.pitch, (float) local.x, (float) local.y, (float) local.z,
					(float) (cameraPos.x - pos.getX()), (float) (cameraPos.y - pos.getY()), (float) (cameraPos.z - pos.getZ()), now, pos.getY());
			turn.buffer = b.data;
			turn.colours = b.colour;
			turn.corners = b.corners;
			turn.built = now;
			backToFront(turn, (float) (cameraPos.x - pos.getX()), (float) (cameraPos.y - pos.getY()), (float) (cameraPos.z - pos.getZ()));
		}
		state.holo = turn.buffer;
		state.colour = turn.colours;
		state.corners = turn.corners;
		state.drawn = true;
	}

	// ------------------------------------------------------------------ drawing

	@Override
	public void submit(State state, PoseStack poses, SubmitNodeCollector collector, CameraRenderState camera) {
		ConsoleMesh housing = ConsoleMesh.get(MESH_NAME);
		if (housing != null) {
			poses.pushPose();
			poses.translate(0.5, 0, 0.5);
			poses.mulPose(new Matrix4f().rotationY(state.facing));
			poses.translate(-0.5, 0, -0.5);
			int light = state.lightCoords;
			collector.submitCustomGeometry(poses, HOUSING, (pose, consumer) -> {
				DroneDepotRenderer.Geometry g = new DroneDepotRenderer.Geometry(pose, consumer);
				ConsoleMesh.emit(g, housing.quads[ConsoleMesh.SOLID], light, false);
				ConsoleMesh.emit(g, housing.quads[ConsoleMesh.LIT], LightCoordsUtil.FULL_BRIGHT, true);
			});
			poses.popPose();
		}
		if (!state.drawn || state.corners == 0) {
			return;
		}
		float[] q = state.holo;
		int[] colour = state.colour;
		int corners = state.corners;
		collector.submitCustomGeometry(poses, HOLO, (pose, consumer) -> emit(pose, consumer, q, colour, corners));
	}

	/**
	 * The hologram's corners into the buffer (Justin, 6 November 2026: the hologram's drawing on the pieces' fast path):
	 * each placed by the pose in one step and handed over whole, its one normal worked out once, without the game's
	 * per-corner helpers that made two new vectors for every one of its 140,000 corners every frame. Drawn exactly as
	 * before.
	 */
	private static void emit(PoseStack.Pose pose, VertexConsumer consumer, float[] q, int[] colour, int corners) {
		Matrix4f place = pose.pose();
		org.joml.Vector3f at = new org.joml.Vector3f(), up = pose.transformNormal(0, 1, 0, new org.joml.Vector3f());
		float nx = up.x, ny = up.y, nz = up.z;
		for (int c = 0; c < corners; c++) {
			int o = c * HoloProjectorGeometry.Builder.FLOATS;
			place.transformPosition(q[o], q[o + 1], q[o + 2], at);
			consumer.addVertex(at.x, at.y, at.z, colour[c], q[o + 3], q[o + 4], OverlayTexture.NO_OVERLAY, LightCoordsUtil.FULL_BRIGHT, nx, ny, nz);
		}
	}
}
