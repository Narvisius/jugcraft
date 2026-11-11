package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.tower.RoomKitRotorBlock;
import io.github.jimbozoomer.jugcraft.tower.RoomKitRotorBlockEntity;
import java.util.List;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * The turning rotors of a room kit piece (tower/RoomKitRotorBlock: the roof plant's dry cooler, its twelve EC fans).
 * Each rotor is its blades, plates pitched 24 degrees and swept a little from the hub to the tip (the hub, the motor and
 * the wire guard are the model's), turning at about two turns a second by the game time, each fan a little faster or
 * slower than the next and started at its own angle. The rotors' places come from the manifest (tools/roof_plant.py
 * ROTORS: pixels in the piece's structure as it faces north), turned with the piece. Client only: nothing on the server
 * turns them.
 */
public final class RoomKitRotorRenderer implements BlockEntityRenderer<RoomKitRotorBlockEntity, RoomKitRotorRenderer.State> {
	static final Identifier TEXTURE = Jugcraft.id("textures/entity/roof_fan_blade.png");
	private static final RenderType MESH = RenderTypes.entityCutout(TEXTURE);
	private static final float[] UV = {0F, 0F, 1F, 1F};
	private static final double PITCH = Math.toRadians(24);
	/** Turns a second, before each rotor's own small difference. */
	private static final double SPEED = 2.1;

	public RoomKitRotorRenderer(BlockEntityRendererProvider.Context context) {
	}

	public static final class State extends BlockEntityRenderState {
		int turns;
		double time;
		long seed;
		List<float[]> rotors = List.of();
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	/** The rotors reach six blocks from the anchor. */
	@Override
	public boolean shouldRenderOffScreen() {
		return true;
	}

	@Override
	public int getViewDistance() {
		return 96;
	}

	@Override
	public void extractRenderState(RoomKitRotorBlockEntity be, State state, float partialTick, Vec3 cameraPos,
			ModelFeatureRenderer.CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(be, state, crumbling);
		BlockState block = be.getBlockState();
		if (block.getBlock() instanceof RoomKitRotorBlock piece) {
			Direction facing = piece.facingOf(block);
			state.turns = switch (facing) {
				case EAST -> 1;
				case SOUTH -> 2;
				case WEST -> 3;
				default -> 0;
			};
			state.rotors = piece.spec.rotors();
		} else {
			state.rotors = List.of();
		}
		state.time = be.getLevel() == null ? 0 : (be.getLevel().getGameTime() + partialTick) / 20.0;
		state.seed = be.getBlockPos().asLong();
	}

	@Override
	public void submit(State state, PoseStack poses, SubmitNodeCollector collector, CameraRenderState camera) {
		if (state.rotors.isEmpty()) {
			return;
		}
		poses.pushPose();
		poses.translate(0.5, 0, 0.5);
		// a quarter clockwise seen from above takes (x, z) to (-z, x), as the blockstate turns the models
		poses.mulPose(new Matrix4f().rotationY((float) (-Math.PI / 2 * state.turns)));
		poses.translate(-0.5, 0, -0.5);
		int light = state.lightCoords;
		collector.submitCustomGeometry(poses, MESH, (pose, consumer) -> {
			DroneDepotRenderer.Geometry g = new DroneDepotRenderer.Geometry(pose, consumer);
			g.light = light;
			int i = 0;
			for (float[] r : state.rotors) {
				long h = state.seed * 31 + i * 0x9E3779B97F4A7C15L;
				double speed = SPEED * (0.9 + 0.2 * ((h >>> 11 & 0xFF) / 255.0));
				double start = (h >>> 23 & 0xFFFF) / 65535.0 * Math.PI * 2;
				g.push();
				g.translate(r[0] / 16.0, r[1] / 16.0, r[2] / 16.0);
				g.yaw(start + state.time * speed * Math.PI * 2);
				blades(g, r[3] / 16.0, r[4] / 16.0, Math.round(r[5]));
				g.pop();
				i++;
			}
		});
		poses.popPose();
	}

	/** n blades from the hub to the tip: each a plate pitched about its own length, a little wider and swept at the tip. */
	private static void blades(DroneDepotRenderer.Geometry g, double hub, double tip, int n) {
		for (int k = 0; k < n; k++) {
			g.push();
			g.yaw(Math.PI * 2 * k / n);
			float[] root = corner(hub, 1.0 / 16, 0);
			float[] end = corner(tip, 1.6 / 16, 0.12);
			// a = root leading, b = tip leading, c = tip trailing, d = root trailing
			float ax = root[0], ay = root[1], az = root[2], dx = root[3], dy = root[4], dz = root[5];
			float bx = end[0], by = end[1], bz = end[2], cx = end[3], cy = end[4], cz = end[5];
			// the plate's normal (up its pitched face)
			float ex = bx - ax, ey = by - ay, ez = bz - az, fx = dx - ax, fy = dy - ay, fz = dz - az;
			float nx = ey * fz - ez * fy, ny = ez * fx - ex * fz, nz = ex * fy - ey * fx;
			float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
			if (len < 1e-6F) {
				g.pop();
				continue;
			}
			float sgn = ny < 0 ? -1 : 1;
			nx *= sgn / len;
			ny *= sgn / len;
			nz *= sgn / len;
			g.face(ax, ay, az, bx, by, bz, cx, cy, cz, dx, dy, dz, nx, ny, nz, UV[0], UV[1], UV[0], UV[3], UV[2], UV[3], UV[2], UV[1]);
			g.pop();
		}
	}

	/** The leading and trailing edge of a blade at radius r (its half-width w, swept by `sweep` radians). */
	private static float[] corner(double r, double w, double sweep) {
		double s = Math.sin(sweep), c = Math.cos(sweep);
		double ux = s, uz = c;                  // along the blade (yaw 0 faces +z)
		double vx = c, vz = -s;                 // across it
		double dy = w * Math.sin(PITCH), dh = w * Math.cos(PITCH);
		return new float[] {(float) (ux * r + vx * dh), (float) dy, (float) (uz * r + vz * dh),
				(float) (ux * r - vx * dh), (float) -dy, (float) (uz * r - vz * dh)};
	}
}
