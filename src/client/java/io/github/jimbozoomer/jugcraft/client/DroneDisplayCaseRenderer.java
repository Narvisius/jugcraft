package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.drone.DepotDisplayBlockEntity;
import io.github.jimbozoomer.jugcraft.drone.DroneTier;
import io.github.jimbozoomer.jugcraft.tower.DroneDisplayCaseBlock;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * The Drone Display Case (tower/DroneDisplayCaseBlock; work list item 1, approved 22 October 2026 without the placard):
 * its mesh from tools/drone_display_case.py turned to its front (the plinth, the turntable, the glass hood, the light
 * hood, the amber wash on the floor round it), the drone of its tier standing on the turntable and turning once a minute
 * (drawn smaller if it would not fit under the glass), and on the light hood's ticker rail ({@link TickerRail}, amber LED
 * dots) the drone's name in four evenly spaced spots, running round all four sides.
 */
public final class DroneDisplayCaseRenderer implements BlockEntityRenderer<DepotDisplayBlockEntity, DroneDisplayCaseRenderer.State> {
	static final String MESH_NAME = "drone_display_case";
	static final Identifier TEXTURE = Jugcraft.id("textures/entity/drone_display_case.png");
	private static final RenderType MESH = RenderTypes.entityCutout(TEXTURE);
	private static final RenderType GLASS = RenderTypes.entityTranslucentCull(TEXTURE);
	private static final RenderType GLOW = GlowTypes.glow(TEXTURE);
	private static final RenderType DOTS = RenderTypes.entityCutout(DroneDepotRenderer.TEXTURE);
	/** The ticker faces' live kind in the mesh; the rail's rows and colour; its speed (dot columns a tick). */
	static final int TICKER = 7, ROWS = 9, COLUMNS = 187, AMBER = 0xFFFFB048;
	static final double SPEED = 0.5;
	/** The drone turns once a minute. */
	private static final double TURN = 1200;
	/** The room the drone has under the glass: how far it may reach from the middle as it turns, and how high. */
	private static final float ROOM_R = 2.15f, ROOM_H = 2.2f;

	public DroneDisplayCaseRenderer(BlockEntityRendererProvider.Context context) {
	}

	public static final class State extends BlockEntityRenderState {
		int tier;
		float facing;
		float yaw;
		float scale = 1;
		int droneLight;
		final HoloGeometry.Quads dots = HoloGeometry.Quads.perFrame();
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public int getViewDistance() {
		return 96;
	}

	/** The case reaches two and a half blocks from its master cell. */
	@Override
	public boolean shouldRenderOffScreen() {
		return true;
	}

	private static TickerRail rail;
	private static boolean railTried;

	/** The rail round the light hood, from the mesh's four ticker faces (in the rail's order), or null. */
	static TickerRail rail(ConsoleMesh mesh) {
		if (!railTried) {
			railTried = true;
			float[][][] faces = new float[4][][];
			for (int i = 0; i < mesh.liveKind.length; i++) {
				if (mesh.liveKind[i] == TICKER && mesh.liveId[i] >= 0 && mesh.liveId[i] < 4) {
					faces[mesh.liveId[i]] = mesh.live[i];
				}
			}
			for (float[][] f : faces) {
				if (f == null) {
					return null;
				}
			}
			double top = faces[0][0][1], bottom = faces[0][3][1];
			TickerRail.Builder b = TickerRail.at((top + bottom) / 2);
			for (float[][] f : faces) {
				b.line(f[0][0], f[0][2], f[1][0], f[1][2]);
			}
			double side = Math.hypot(faces[0][1][0] - faces[0][0][0], faces[0][1][2] - faces[0][0][2]);
			rail = b.closed().dots(side / COLUMNS, ROWS).band((top - bottom) / 2, 0, 0.003).colours(AMBER, AMBER, 0).speed(SPEED).copies(4)
					.build();
		}
		return rail;
	}

	/** What the rail says: "T4 · FAN-WING RUNNER" (the tier left off a name too long for a quarter of the rail). */
	static String tickerText(int tier) {
		String name = DroneTier.byNumber(tier).map(DroneTerminalScreen::tierName).orElse("DRONE");
		String full = "T" + tier + " " + TickerRail.SEPARATOR + " " + name;
		return full.length() * 6 <= COLUMNS - 12 ? full : name;
	}

	/** The rotation that turns the mesh (its front north) to {@code facing}. */
	static float facingYaw(Direction facing) {
		return (float) Math.toRadians(180 - facing.toYRot());
	}

	@Override
	public void extractRenderState(DepotDisplayBlockEntity be, State state, float partialTick, Vec3 cameraPos,
			ModelFeatureRenderer.CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(be, state, crumbling);
		state.dots.clear();
		BlockState bs = be.getBlockState();
		Level level = be.getLevel();
		if (!(bs.getBlock() instanceof DroneDisplayCaseBlock) || level == null) {
			state.tier = 0;
			return;
		}
		state.tier = bs.getValue(DroneDisplayCaseBlock.TIER);
		state.facing = facingYaw(bs.getValue(DroneDisplayCaseBlock.FACING));
		double now = level.getGameTime() + partialTick;
		// nose to the front, turning slowly
		state.yaw = (float) (Math.PI + (now % TURN) / TURN * 2 * Math.PI);
		float[] reach = DroneMesh.reach(state.tier);
		state.scale = reach == null ? 1 : Math.min(1, Math.min(ROOM_R / Math.max(reach[0], 0.01f), ROOM_H / Math.max(reach[1], 0.01f)));
		// lit from the light hood over it
		int l = state.lightCoords;
		state.droneLight = LightCoordsUtil.pack(Math.max(LightCoordsUtil.block(l), 13), LightCoordsUtil.sky(l));
		ConsoleMesh mesh = ConsoleMesh.get(MESH_NAME);
		TickerRail r = mesh == null ? null : rail(mesh);
		if (r != null) {
			r.draw(tickerText(state.tier), now, null, (kind, ax, ay, az, bx, by, bz, cx, cy, cz, dx, dy, dz, nx, nz, argb) ->
					state.dots.facing(ax, ay, az, bx, by, bz, cx, cy, cz, dx, dy, dz, nx, 0, nz, argb, argb, argb, argb));
		}
	}

	@Override
	public void submit(State state, PoseStack poses, SubmitNodeCollector collector, CameraRenderState camera) {
		ConsoleMesh mesh = ConsoleMesh.get(MESH_NAME);
		if (mesh == null || state.tier == 0) {
			return;
		}
		poses.pushPose();
		poses.translate(0.5, 0, 0.5);
		poses.mulPose(new Matrix4f().rotationY(state.facing));
		poses.translate(-0.5, 0, -0.5);
		int light = state.lightCoords;
		collector.submitCustomGeometry(poses, MESH, (pose, consumer) -> {
			DroneDepotRenderer.Geometry g = new DroneDepotRenderer.Geometry(pose, consumer);
			ConsoleMesh.emit(g, mesh.quads[ConsoleMesh.SOLID], light, false);
			ConsoleMesh.emit(g, mesh.quads[ConsoleMesh.LIT], LightCoordsUtil.FULL_BRIGHT, true);
		});
		if (state.dots.size() > 0) {
			float[] dots = state.dots.toArray();
			collector.submitCustomGeometry(poses, DOTS, (pose, consumer) -> HoloMapRenderer.emitLit(new DroneDepotRenderer.Geometry(pose, consumer),
					dots, LightCoordsUtil.FULL_BRIGHT, 255));
		}
		int tier = state.tier, droneLight = state.droneLight;
		float yaw = state.yaw, scale = state.scale;
		Retained.mark(collector);
		collector.submitCustomGeometry(poses, DroneMesh.RENDER_TYPE, (pose, consumer) -> DroneModel.draw(new DroneDepotRenderer.Geometry(pose, consumer, DroneMesh.RENDER_TYPE),
				0.5f, (float) DroneDisplayCaseBlock.DECK, 0.5f, yaw, tier, -1, 0, droneLight, 0, scale));
		collector.submitCustomGeometry(poses, GLOW, (pose, consumer) -> ConsoleMesh.emit(new DroneDepotRenderer.Geometry(pose, consumer),
				mesh.quads[ConsoleMesh.GLOW], LightCoordsUtil.FULL_BRIGHT, true));
		collector.submitCustomGeometry(poses, GLASS, (pose, consumer) -> ConsoleMesh.emit(new DroneDepotRenderer.Geometry(pose, consumer),
				mesh.quads[ConsoleMesh.GLASS], light, false));
		poses.popPose();
	}
}
