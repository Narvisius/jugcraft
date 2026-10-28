package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.client.tv.TvSpeakers;
import io.github.jimbozoomer.jugcraft.tv.TvSpeakerBlock;
import io.github.jimbozoomer.jugcraft.tv.TvSpeakerBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * The surround speaker (tv/TvSpeakerBlock; approved from the mockup sheet 27 October 2026): its mesh from
 * tools/tv_speaker.py turned its sixteen ways, drawn from the lower half for both blocks. What's live: the woofer's cone
 * (its own mesh) pushed out and pulled in with the low end of its screen's sound as this game plays it; the status
 * light on the front (white while its screen's sound plays, dim while it's linked and quiet, amber unlinked) and the
 * LINK light on the back (cyan linked, off unlinked); both blink cyan for two seconds after a link is made.
 *
 * <p>Live faces (client/ConsoleMesh): kind 8 the status light (id 0 front, 1 back), drawn from the mesh's white patch
 * (its first edge tile) in their colour, full bright.
 */
public final class TvSpeakerRenderer implements BlockEntityRenderer<TvSpeakerBlockEntity, TvSpeakerRenderer.State> {
	static final Identifier TEXTURE = Jugcraft.id("textures/entity/tv_speaker.png");
	private static final RenderType MESH = RenderTypes.entityCutout(TEXTURE);
	static final int STATUS = 8;
	/** How far the cone moves out and in at most (metres; a 6.5 in woofer's few millimetres). */
	static final float EXCURSION = 0.0018F;
	static final int WHITE = 0xFFE2EEFF, IDLE = 0xFF3C424C, AMBER = 0xFFFFB04A, CYAN = 0xFF5ADCF0, CYAN_DIM = 0xFF1C5862, OFF = 0xFF101214;
	/** The lights blink this long after a link is made (ticks). */
	static final long BLINK = 40;

	public TvSpeakerRenderer(BlockEntityRendererProvider.Context context) {
	}

	public static final class State extends BlockEntityRenderState {
		boolean present;
		float turn;
		int front, back, upperLight;
		float cone;
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public int getViewDistance() {
		return 64;
	}

	/** It's drawn from its lower block for both. */
	@Override
	public boolean shouldRenderOffScreen() {
		return true;
	}

	/** Which way a speaker turned {@code rotation} faces, as a turn of its mesh (front to +z). */
	static float turn(int rotation) {
		return (float) -Math.toRadians(rotation * 22.5);
	}

	@Override
	public void extractRenderState(TvSpeakerBlockEntity be, State state, float partialTick, Vec3 cameraPos,
			ModelFeatureRenderer.CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(be, state, crumbling);
		BlockState bs = be.getBlockState();
		Level level = be.getLevel();
		state.present = level != null && bs.getBlock() instanceof TvSpeakerBlock && bs.getValue(TvSpeakerBlock.HALF) == DoubleBlockHalf.LOWER;
		if (!state.present) {
			return;
		}
		state.turn = turn(bs.getValue(TvSpeakerBlock.ROTATION));
		state.upperLight = LightCoordsUtil.getLightCoords(level, be.getBlockPos().above());
		BlockPos screen = be.screen() == null ? null : TvSpeakers.screenOf(be);
		boolean sounding = TvSpeakers.sounding(screen);
		long since = be.linkedAt() == Long.MIN_VALUE ? Long.MAX_VALUE : level.getGameTime() - be.linkedAt();
		if (since >= 0 && since < BLINK && be.screen() != null) {
			int c = (since / 4) % 2 == 0 ? CYAN : OFF;
			state.front = c;
			state.back = c;
		} else if (screen == null) {
			state.front = AMBER;
			state.back = OFF;
		} else {
			state.front = sounding ? WHITE : IDLE;
			state.back = sounding ? CYAN : CYAN_DIM;
		}
		state.cone = sounding ? EXCURSION * Math.max(-1, Math.min(1, TvSpeakers.bass(screen) * 5)) : 0;
	}

	@Override
	public void submit(State state, PoseStack poses, SubmitNodeCollector collector, CameraRenderState camera) {
		if (!state.present) {
			return;
		}
		ConsoleMesh mesh = ConsoleMesh.get("tv_speaker");
		ConsoleMesh cone = ConsoleMesh.get("tv_speaker_cone");
		if (mesh == null) {
			return;
		}
		poses.pushPose();
		poses.translate(0.5, 0, 0.5);
		poses.mulPose(new Matrix4f().rotationY(state.turn));
		int lower = state.lightCoords, upper = state.upperLight;
		float[] white = mesh.edgeTiles[0];
		collector.submitCustomGeometry(poses, MESH, (pose, consumer) -> {
			DroneDepotRenderer.Geometry g = new DroneDepotRenderer.Geometry(pose, consumer);
			emit(g, mesh.quads[ConsoleMesh.SOLID], lower, upper);
			ConsoleMesh.emit(g, mesh.quads[ConsoleMesh.LIT], LightCoordsUtil.FULL_BRIGHT, true);
			for (int i = 0; i < mesh.liveKind.length; i++) {
				if (mesh.liveKind[i] == STATUS) {
					face(g, mesh.live[i], white, mesh.liveId[i] == 0 ? state.front : state.back);
				}
			}
			if (cone != null) {
				g.push();
				g.translate(0, 0, state.cone);
				emit(g, cone.quads[ConsoleMesh.SOLID], lower, upper);
				g.pop();
			}
		});
		poses.popPose();
	}

	/** Shaded quads lit by the world: the stand by its own block's light, the speaker by the block it's in. */
	private static void emit(DroneDepotRenderer.Geometry g, float[] corners, int lower, int upper) {
		for (int o = 0; o + ConsoleMesh.CORNER <= corners.length; o += ConsoleMesh.CORNER) {
			int s = Math.round(corners[o + 8] * 255);
			g.meshVertex(corners[o], corners[o + 1], corners[o + 2], corners[o + 3], corners[o + 4], corners[o + 5], corners[o + 6],
					corners[o + 7], 0xFF000000 | s << 16 | s << 8 | s, corners[o + 1] < 1 ? lower : upper);
		}
	}

	/** A live face (tl tr br bl) filled with one colour, full bright, a hair in front of it. */
	private static void face(DroneDepotRenderer.Geometry g, float[][] f, float[] white, int argb) {
		Vector3f right = new Vector3f(f[1][0] - f[0][0], f[1][1] - f[0][1], f[1][2] - f[0][2]);
		Vector3f down = new Vector3f(f[3][0] - f[0][0], f[3][1] - f[0][1], f[3][2] - f[0][2]);
		Vector3f n = new Vector3f(down).cross(right).normalize();
		float u = (white[0] + white[2]) / 2, v = (white[1] + white[3]) / 2;
		float lift = 0.0004f;
		for (int c : new int[] {0, 3, 2, 1}) {
			g.meshVertex(f[c][0] + n.x * lift, f[c][1] + n.y * lift, f[c][2] + n.z * lift, u, v, n.x, n.y, n.z, argb, LightCoordsUtil.FULL_BRIGHT);
		}
	}
}
