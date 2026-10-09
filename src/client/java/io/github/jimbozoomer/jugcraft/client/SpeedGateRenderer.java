package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.nuclear.PlantInterior;
import io.github.jimbozoomer.jugcraft.nuclear.SpeedGateBlock;
import io.github.jimbozoomer.jugcraft.nuclear.SpeedGateBlockEntity;
import java.util.ArrayList;
import java.util.EnumMap;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

/**
 * A speed gate's moving parts (nuclear/SpeedGateBlock.java): its two glass doors, sliding out of their pedestals to
 * meet in the middle and back into the pedestals' slots, and the lane lights along the pedestals (blue waiting, green
 * open); and the sign gantry over the lane, which stands higher than a block model may reach. They are the lane
 * block's own models (its door_left, door_right, leds_wait, leds_go and gantry parts, drawn facing north:
 * tools/speed_gate.py), the doors set in this lane's slot, slid and turned to the gate's facing here, lit as the
 * block is (the light bars, lights and signs at their own glow).
 */
public class SpeedGateRenderer implements BlockEntityRenderer<SpeedGateBlockEntity, SpeedGateRenderer.State> {
	private static final RenderType RENDER_TYPE = RenderTypes.entityTranslucentCull(TextureAtlas.LOCATION_BLOCKS);
	private static final Map<SpeedGateBlock.Part, Cached> QUADS = new EnumMap<>(SpeedGateBlock.Part.class);

	public static final class State extends BlockEntityRenderState {
		Direction facing = Direction.NORTH;
		float open;
		/** +1: the doors run behind the line, in the lane block; -1: in the block ahead (SpeedGateBlock.ownSlot). */
		int slot = 1;
	}

	private record Cached(BlockStateModel model, List<BakedQuad> quads) {
	}

	public static void register() {
		BlockEntityRendererRegistry.register(PlantInterior.SPEED_GATE_ENTITY, SpeedGateRenderer::new);
	}

	public SpeedGateRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(SpeedGateBlockEntity gate, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(gate, state, crumbling);
		state.facing = gate.getBlockState().getValue(SpeedGateBlock.FACING);
		state.open = gate.getLevel() == null ? 0 : gate.openness(gate.getLevel().getGameTime(), partialTick);
		state.slot = gate.getLevel() == null || SpeedGateBlock.ownSlot(gate.getLevel(), gate.getBlockPos(), state.facing) ? 1 : -1;
	}

	@Override
	public boolean shouldRenderOffScreen() {
		return true;
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		Direction f = state.facing;
		// This lane's slot, in the north-facing model's frame: +z is behind the line, into the lane block itself.
		float dz = state.slot * SpeedGateBlock.SLOT / 16F;
		float slide = SpeedGateBlock.DOOR_WIDTH / 16F * state.open;
		int light = state.lightCoords;
		pose.pushPose();
		pose.translate(0.5F, 0, 0.5F);
		pose.rotateDegrees(Axis.YP, -f.toYRot() + 180);
		pose.translate(-0.5F, 0, -0.5F);
		collector.submitCustomGeometry(pose, RENDER_TYPE, (p, consumer) ->
				draw(p, consumer, state.open > 0.01F ? SpeedGateBlock.Part.LEDS_GO : SpeedGateBlock.Part.LEDS_WAIT, 0, 0, light));
		// The sign gantry, modelled a block lower than it stands; its post on the west or north pedestal.
		pose.pushPose();
		pose.translate(0, 1, 0);
		SpeedGateBlock.Part gantry = f == Direction.SOUTH || f == Direction.WEST ? SpeedGateBlock.Part.GANTRY_EAST : SpeedGateBlock.Part.GANTRY;
		collector.submitCustomGeometry(pose, RENDER_TYPE, (p, consumer) -> draw(p, consumer, gantry, 0, 0, light));
		pose.popPose();
		pose.pushPose();
		pose.translate(-slide, 0, dz);
		collector.submitCustomGeometry(pose, RENDER_TYPE, (p, consumer) -> draw(p, consumer, SpeedGateBlock.Part.DOOR_LEFT, 0, 0, light));
		pose.popPose();
		pose.pushPose();
		pose.translate(slide, 0, dz);
		collector.submitCustomGeometry(pose, RENDER_TYPE, (p, consumer) -> draw(p, consumer, SpeedGateBlock.Part.DOOR_RIGHT, 0, 0, light));
		pose.popPose();
		pose.popPose();
	}

	/** One of the lane block's part models (facing north), its glowing faces at their own light. */
	private static void draw(PoseStack.Pose pose, VertexConsumer consumer, SpeedGateBlock.Part part, float dx, float dz, int light) {
		for (BakedQuad quad : quads(part)) {
			int emission = quad.materialInfo().lightEmission();
			int lit = emission > 0 ? LightCoordsUtil.lightCoordsWithEmission(light, emission) : light;
			Direction d = quad.direction();
			for (int i = 0; i < 4; i++) {
				Vector3fc v = quad.position(i);
				long uv = quad.packedUV(i);
				consumer.addVertex(pose, v.x() + dx, v.y(), v.z() + dz).setColor(0xFFFFFFFF).setUv(UVPair.unpackU(uv), UVPair.unpackV(uv))
						.setOverlay(OverlayTexture.NO_OVERLAY).setLight(lit).setNormal(pose, d.getStepX(), d.getStepY(), d.getStepZ());
			}
		}
	}

	private static List<BakedQuad> quads(SpeedGateBlock.Part part) {
		Minecraft minecraft = Minecraft.getInstance();
		BlockState state = PlantInterior.BLOCKS.get("speed_gate").defaultBlockState().setValue(SpeedGateBlock.PART, part);
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
