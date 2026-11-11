package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.nuclear.LeadDoorBlock;
import io.github.jimbozoomer.jugcraft.nuclear.PlantDoorBlock;
import io.github.jimbozoomer.jugcraft.nuclear.PlantDoorBlockEntity;
import io.github.jimbozoomer.jugcraft.nuclear.PlantDoors;
import io.github.jimbozoomer.jugcraft.nuclear.PlantInterior;
import io.github.jimbozoomer.jugcraft.nuclear.RestroomBlocks;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

/**
 * A plant door's leaf while it swings (nuclear/PlantDoorBlock.java MOVING: the block model then shows only the frame):
 * the leaf shut, with its hardware (a part model of plant_door_part: PlantDoors.leafPart), turned about its hinge pin
 * by how far it has swung (PlantDoorBlockEntity.angle); and on the steel and lead doors the closer's arm, its main
 * arm from the spindle on the leaf and its forearm to the shoe under the head, meeting at the elbow. Everything in
 * the vanilla door's frame (facing east, hinge left at z 0: tools/plant_doors.py), turned to the door's facing.
 */
public class PlantDoorRenderer implements BlockEntityRenderer<PlantDoorBlockEntity, PlantDoorRenderer.State> {
	private static final RenderType RENDER_TYPE = RenderTypes.entityTranslucentCull(TextureAtlas.LOCATION_BLOCKS);
	private static final Map<Integer, Cached> QUADS = new HashMap<>();

	/** A kind of door's hinge pin (x, z, hinge left), and its closer: spindle (x, z), shoe (x, z); px. */
	private record Geometry(double px, double pz, double sx, double sz, double fx, double fz, int arm) {
	}

	// tools/plant_doors.py SPECS / spindle / shoe, ARM_MAIN, ARM_FORE; tools/restroom.py DOOR_LEAF, LEAF_Z.
	private static final Geometry STEEL = new Geometry(3.4, 0.9, 0.7, 6.8, 0.7, 10.8, PlantDoors.STEEL_ARM);
	private static final Geometry GLASS = new Geometry(3.0, 1.1, 0, 0, 0, 0, -1);
	private static final Geometry LEAD = new Geometry(3.4, 1.1, 0.0, 7.0, 0.0, 11.0, PlantDoors.LEAD_ARM);
	private static final Geometry STALL = new Geometry(0.65, 1.7, 0, 0, 0, 0, -1);
	private static final double ARM_MAIN = 7.8, ARM_FORE = 3.8;

	public static final class State extends BlockEntityRenderState {
		boolean moving, right;
		Direction facing = Direction.EAST;
		int part;
		float angle;
		Geometry geometry = STEEL;
	}

	private record Cached(BlockStateModel model, List<BakedQuad> quads) {
	}

	public static void register() {
		BlockEntityRendererRegistry.register(PlantDoors.ENTITY, PlantDoorRenderer::new);
	}

	public PlantDoorRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(PlantDoorBlockEntity door, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(door, state, crumbling);
		BlockState block = door.getBlockState();
		state.moving = block.getBlock() instanceof PlantDoorBlock && block.getValue(PlantDoorBlock.MOVING);
		state.angle = door.getLevel() == null ? 0 : door.angle(door.getLevel().getGameTime(), partialTick);
		if (!state.moving) {
			return;
		}
		state.facing = block.getValue(DoorBlock.FACING);
		state.right = block.getValue(DoorBlock.HINGE) == DoorHingeSide.RIGHT;
		state.part = PlantDoors.leafPart(block);
		Block b = block.getBlock();
		state.geometry = b instanceof RestroomBlocks.StallDoor ? STALL : b instanceof LeadDoorBlock ? LEAD
				: b == PlantInterior.BLOCKS.get("glass_door") ? GLASS : STEEL;
	}

	@Override
	public boolean shouldRenderOffScreen() {
		return true;
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		if (!state.moving) {
			return;
		}
		Geometry g = state.geometry;
		int light = state.lightCoords;
		float sign = state.right ? -1 : 1;
		pose.pushPose();
		// The vanilla door's frame (facing east) turned to the door's facing, in model pixels.
		pose.translate(0.5F, 0, 0.5F);
		pose.rotateDegrees(Axis.YP, -yRotation(state.facing));
		pose.translate(-0.5F, 0, -0.5F);
		pose.scale(1 / 16F, 1 / 16F, 1 / 16F);
		float pz = (float) (state.right ? 16 - g.pz() : g.pz());
		pose.pushPose();
		pose.translate((float) g.px(), 0, pz);
		pose.rotateDegrees(Axis.YP, sign * state.angle);
		pose.translate((float) -g.px(), 0, -pz);
		int leaf = state.part;
		collector.submitCustomGeometry(pose, RENDER_TYPE, (p, consumer) -> draw(p, consumer, leaf, light));
		pose.popPose();
		if (g.arm() >= 0) {
			double[][] arm = arm(g, state.angle);
			for (int k = 0; k < 2; k++) {
				double[] a = arm[k], b = arm[k + 1];
				double az = state.right ? 16 - a[1] : a[1], bz = state.right ? 16 - b[1] : b[1];
				float turn = (float) Math.toDegrees(Math.atan2(b[0] - a[0], bz - az));
				pose.pushPose();
				pose.translate((float) a[0], 0, (float) az);
				pose.rotateDegrees(Axis.YP, turn);
				int link = g.arm() + k;
				collector.submitCustomGeometry(pose, RENDER_TYPE, (p, consumer) -> draw(p, consumer, link, light));
				pose.popPose();
			}
		}
		pose.popPose();
	}

	/** The closer's arm with the leaf open deg (hinge left): spindle, elbow, shoe (x, z). tools/plant_doors.py elbow. */
	static double[][] arm(Geometry g, double deg) {
		double r = Math.toRadians(deg);
		double dx = g.sx() - g.px(), dz = g.sz() - g.pz();
		double sx = g.px() + dx * Math.cos(r) + dz * Math.sin(r), sz = g.pz() - dx * Math.sin(r) + dz * Math.cos(r);
		double fx = g.fx(), fz = g.fz();
		double ux = fx - sx, uz = fz - sz;
		double d = Math.min(Math.hypot(ux, uz), ARM_MAIN + ARM_FORE - 1e-6);
		double a = (ARM_MAIN * ARM_MAIN - ARM_FORE * ARM_FORE + d * d) / (2 * d);
		double h = Math.sqrt(Math.max(0, ARM_MAIN * ARM_MAIN - a * a));
		double mx = sx + a * ux / d, mz = sz + a * uz / d;
		double e1x = mx + h * uz / d, e1z = mz - h * ux / d, e2x = mx - h * uz / d, e2z = mz + h * ux / d;
		boolean first = e1z > e2z;
		return new double[][] {{sx, sz}, {first ? e1x : e2x, first ? e1z : e2z}, {fx, fz}};
	}

	private static float yRotation(Direction facing) {
		return switch (facing) {
			case SOUTH -> 90;
			case WEST -> 180;
			case NORTH -> 270;
			default -> 0;
		};
	}

	private static void draw(PoseStack.Pose pose, VertexConsumer consumer, int part, int light) {
		for (BakedQuad quad : quads(part)) {
			int emission = quad.materialInfo().lightEmission();
			int lit = emission > 0 ? LightCoordsUtil.lightCoordsWithEmission(light, emission) : light;
			Direction d = quad.direction();
			for (int i = 0; i < 4; i++) {
				Vector3fc v = quad.position(i);
				long uv = quad.packedUV(i);
				// The part models are in blocks (a block model's 0..1); here the pose is in pixels.
				consumer.addVertex(pose, v.x() * 16, v.y() * 16, v.z() * 16).setColor(0xFFFFFFFF).setUv(UVPair.unpackU(uv), UVPair.unpackV(uv))
						.setOverlay(OverlayTexture.NO_OVERLAY).setLight(lit).setNormal(pose, d.getStepX(), d.getStepY(), d.getStepZ());
			}
		}
	}

	private static List<BakedQuad> quads(int part) {
		Minecraft minecraft = Minecraft.getInstance();
		BlockState state = PlantDoors.PART_BLOCK.defaultBlockState().setValue(PlantDoors.PART, part);
		BlockStateModel model = minecraft.getModelManager().getBlockStateModelSet().get(state);
		Cached cached = QUADS.get(part);
		if (cached != null && cached.model() == model) {
			return cached.quads();
		}
		List<BlockStateModelPart> parts = new ArrayList<>();
		model.collectParts(RandomSource.create(42L), parts);
		List<BakedQuad> quads = new ArrayList<>();
		for (BlockStateModelPart p : parts) {
			for (Direction side : Direction.values()) {
				quads.addAll(p.getQuads(side));
			}
			quads.addAll(p.getQuads(null));
		}
		QUADS.put(part, new Cached(model, quads));
		return quads;
	}
}
