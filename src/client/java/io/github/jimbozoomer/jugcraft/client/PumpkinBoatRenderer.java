package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.CarvingFace;
import io.github.jimbozoomer.jugcraft.agriculture.GiantPumpkinBlock;
import io.github.jimbozoomer.jugcraft.agriculture.PumpkinBoat;
import io.github.jimbozoomer.jugcraft.agriculture.PumpkinBoatData;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * Draws a pumpkin boat as the hollow shell of the giant pumpkin it was cut from: the skin outside (the giant
 * pumpkin's own side tiles, the band of its old face that {@link PumpkinBoat.Kind} keeps), cut flesh on the
 * rim, inside and on the floor, and a barge's carving over the skin, glowing when a torch is inside. The floor
 * sits above the water line, so no water shows inside. Wobbles when hit, like a boat.
 */
public class PumpkinBoatRenderer extends EntityRenderer<PumpkinBoat, PumpkinBoatRenderer.State> {
	private static final float WALL = 2.0F / 16;
	private static final float OUT = 0.002F;
	private static final Identifier FLESH = Jugcraft.id("textures/entity/pumpkin_boat_flesh.png");
	private static final Identifier BOTTOM = Jugcraft.id("textures/block/giant_pumpkin_bottom.png");

	public static final class State extends EntityRenderState {
		float yRot;
		float hurtTime;
		int hurtDir;
		float damageTime;
		PumpkinBoat.Kind kind = PumpkinBoat.Kind.RACER;
		final int[][] faces = new int[4][];
		boolean carved;
		boolean lit;
		@Nullable RenderType carving;
	}

	public PumpkinBoatRenderer(EntityRendererProvider.Context context) {
		super(context);
		shadowRadius = 1.0F;
	}

	/** The shell is a little wider and much taller than the boat's hitbox. */
	@Override
	protected AABB getBoundingBoxForCulling(PumpkinBoat boat, float partialTick) {
		return boat.getBoundingBox().inflate(0.25, 1.0, 0.25);
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(PumpkinBoat boat, State state, float partialTick) {
		super.extractRenderState(boat, state, partialTick);
		state.yRot = boat.getYRot(partialTick);
		state.hurtTime = boat.getHurtTime() - partialTick;
		state.hurtDir = boat.getHurtDir();
		state.damageTime = Math.max(boat.getDamage() - partialTick, 0.0F);
		state.kind = boat.kind();
		PumpkinBoatData data = boat.data();
		state.carved = data.carved();
		state.lit = data.lit();
		state.carving = null;
		if (state.carved) {
			for (int i = 0; i < 4; i++) {
				state.faces[i] = data.face(i);
			}
			state.carving = CarvingTextures.getGiant(state.faces, state.lit, false);
		}
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		PumpkinBoat.Kind kind = state.kind;
		int size = kind == PumpkinBoat.Kind.BARGE ? GiantPumpkinBlock.MAX_SIZE : 2;
		int light = state.lightCoords;
		pose.pushPose();
		pose.rotateDegrees(Axis.YP, 180.0F - state.yRot);
		if (state.hurtTime > 0.0F) {
			pose.rotateDegrees(Axis.XP, Mth.sin(state.hurtTime) * state.hurtTime * state.damageTime / 10.0F * state.hurtDir);
		}
		float half = kind.width / 2;
		float top = kind.height;
		// The skin: each side tile of the old face, cropped to the band the boat keeps.
		for (int column = 0; column < size; column++) {
			for (int row = 0; row < size; row++) {
				float from = Math.max(row, kind.faceTop);
				float to = Math.min(row + 1, kind.faceTop + kind.height);
				if (to <= from) {
					continue;
				}
				RenderType type = RenderTypes.entitySolid(Jugcraft.id("textures/block/giant_pumpkin_side_" + size + "_" + column + "_" + row + ".png"));
				float v0 = from - row;
				float v1 = to - row;
				float y0 = top - (from - kind.faceTop);
				float y1 = top - (to - kind.faceTop);
				int c = column;
				collector.submitCustomGeometry(pose, type, (matrix, buffer) -> {
					for (Direction side : Direction.Plane.HORIZONTAL) {
						wall(buffer, matrix, side, half, kind.width, c, c + 1, y1, y0, 0.0F, 1.0F, v1, v0, light, false);
					}
				});
			}
		}
		collector.submitCustomGeometry(pose, RenderTypes.entitySolid(BOTTOM), (matrix, buffer) -> {
			for (int x = 0; x < size; x++) {
				for (int z = 0; z < size; z++) {
					float x0 = -half + x * kind.width / size;
					float z0 = -half + z * kind.width / size;
					flat(buffer, matrix, x0, z0, x0 + kind.width / size, z0 + kind.width / size, 0.0F, false, 0, 0, 1, 1, light);
				}
			}
		});
		float inner = half - WALL;
		collector.submitCustomGeometry(pose, RenderTypes.entitySolid(FLESH), (matrix, buffer) -> {
			// Inside walls, floor and rim, in flesh.
			for (Direction side : Direction.Plane.HORIZONTAL) {
				wall(buffer, matrix, side, inner, inner * 2, 0.0F, inner * 2, kind.floor, top, 0.0F, 1.0F, 1.0F, 0.0F, light, true);
			}
			// The flesh texture is stretched over each piece (UVs 0 to 1), so it never has to repeat.
			flat(buffer, matrix, -inner, -inner, inner, inner, kind.floor, true, 0, 0, 1, 1, light);
			flat(buffer, matrix, -half, -half, half, -inner, top, true, 0, 0, 1, WALL, light);
			flat(buffer, matrix, -half, inner, half, half, top, true, 0, 0, 1, WALL, light);
			flat(buffer, matrix, -half, -inner, -inner, inner, top, true, 0, 0, WALL, 1, light);
			flat(buffer, matrix, inner, -inner, half, inner, top, true, 0, 0, WALL, 1, light);
		});
		RenderType carving = state.carving;
		if (carving != null) {
			// The carving over the skin: the same band of each 48x48 face.
			int glow = state.lit ? LightCoordsUtil.FULL_BRIGHT : light;
			int[][] faces = state.faces;
			float v0 = kind.faceTop / size;
			float v1 = (kind.faceTop + kind.height) / size;
			collector.submitCustomGeometry(pose, carving, (matrix, buffer) -> {
				for (Direction side : Direction.Plane.HORIZONTAL) {
					int index = side.get2DDataValue();
					if (!CarvingFace.isBlank(faces[index])) {
						wall(buffer, matrix, side, half + OUT, kind.width, 0.0F, kind.width, 0.0F, top, index / 4.0F, (index + 1) / 4.0F, v1, v0,
								glow, false);
					}
				}
			});
		}
		pose.popPose();
		super.submit(state, pose, collector, camera);
	}

	/**
	 * One upright quad on {@code side} at distance {@code at} from the middle, spanning {@code l0} to {@code l1}
	 * from the left of a wall {@code width} wide as seen from outside, and {@code y0} (bottom) to {@code y1} high;
	 * it faces out, or in for the inside of the shell.
	 */
	private static void wall(VertexConsumer buffer, PoseStack.Pose matrix, Direction side, float at, float width, float l0, float l1,
			float y0, float y1, float u0, float u1, float vBottom, float vTop, int light, boolean inward) {
		// Seen from outside, left to right: north runs from +x to -x, south -x to +x, west -z to +z, east +z to -z.
		boolean falling = side == Direction.NORTH || side == Direction.EAST;
		float half = width / 2;
		float left = falling ? half - l0 : -half + l0;
		float right = falling ? half - l1 : -half + l1;
		float[] across = {left, left, right, right};
		float[] ys = {y1, y0, y0, y1};
		float[][] uv = {{u0, vTop}, {u0, vBottom}, {u1, vBottom}, {u1, vTop}};
		int nx = side.getStepX() * (inward ? -1 : 1);
		int nz = side.getStepZ() * (inward ? -1 : 1);
		int[] order = inward ? new int[] {3, 2, 1, 0} : new int[] {0, 1, 2, 3};
		for (int i : order) {
			float s = across[i];
			float[] corner = switch (side) {
				case NORTH -> new float[] {s, ys[i], -at};
				case SOUTH -> new float[] {s, ys[i], at};
				case WEST -> new float[] {-at, ys[i], s};
				default -> new float[] {at, ys[i], s};
			};
			buffer.addVertex(matrix, corner[0], corner[1], corner[2]).setColor(0xFFFFFFFF).setUv(uv[i][0], uv[i][1])
					.setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(matrix, nx, 0, nz);
		}
	}

	/** One flat quad at height {@code y}, facing up or down. */
	private static void flat(VertexConsumer buffer, PoseStack.Pose matrix, float x0, float z0, float x1, float z1, float y, boolean up,
			float u0, float v0, float u1, float v1, int light) {
		float[][] corners = up ? new float[][] {{x0, z0, u0, v0}, {x0, z1, u0, v1}, {x1, z1, u1, v1}, {x1, z0, u1, v0}}
				: new float[][] {{x0, z0, u0, v0}, {x1, z0, u1, v0}, {x1, z1, u1, v1}, {x0, z1, u0, v1}};
		for (float[] corner : corners) {
			buffer.addVertex(matrix, corner[0], y, corner[1]).setColor(0xFFFFFFFF).setUv(corner[2], corner[3])
					.setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(matrix, 0, up ? 1 : -1, 0);
		}
	}
}
