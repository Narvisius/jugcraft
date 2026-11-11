package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity;
import io.github.jimbozoomer.jugcraft.tower.AisleContainmentDoorBlock;
import io.github.jimbozoomer.jugcraft.tower.AisleContainmentDoorBlockEntity;
import io.github.jimbozoomer.jugcraft.tower.JugcraftTower;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

/**
 * An Aisle Containment Door (tower/AisleContainmentDoorBlock.java; work list item 13): its frame, the two leaves slid
 * as far as the door is open (each a block over its end rack when it is), the sensors' lights (green ready, amber while
 * open) and the label's text: COLD AISLE and the aisle's tag on the plate, and over each end rack its row (read live from
 * the rack's own number, A-08 for row A). The models are the master's own (its part models, drawn facing north:
 * tools/aisle_containment.py), turned to the door's facing here and lit as the master is (the lights at their own glow).
 */
public class AisleContainmentDoorRenderer implements BlockEntityRenderer<AisleContainmentDoorBlockEntity, AisleContainmentDoorRenderer.State> {
	private static final RenderType RENDER_TYPE = RenderTypes.entityTranslucentCull(TextureAtlas.LOCATION_BLOCKS);
	private static final Map<AisleContainmentDoorBlock.Part, Cached> QUADS = new EnumMap<>(AisleContainmentDoorBlock.Part.class);
	/** The plate's face (px, the door facing north) and where its texts go; the rows' tags over the end racks. */
	private static final float PLATE_Z = 16 - 16 * 0.147F, TEXT_Y = 30.8F, TAG_X = 24F;
	private static final int BLUE = 0x184696, WHITE = 0xFFFFFF, INK = 0x18181A;
	private final Font font;

	public static final class State extends BlockEntityRenderState {
		Direction facing = Direction.NORTH;
		float open;
		/** The rows under the tags at x -8 (over the master's end rack) and x 40, and the aisle's tag. */
		String rowLeft = "ROW A", rowRight = "ROW B", aisle = "CA-1";
	}

	private record Cached(BlockStateModel model, List<BakedQuad> quads) {
	}

	public AisleContainmentDoorRenderer(BlockEntityRendererProvider.Context context) {
		this.font = context.font();
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(AisleContainmentDoorBlockEntity door, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(door, state, crumbling);
		state.facing = door.getBlockState().getValue(AisleContainmentDoorBlock.FACING);
		Level level = door.getLevel();
		state.open = level == null ? 0 : door.openness(level.getGameTime(), partialTick);
		char a = level == null ? 0 : row(level, door.getBlockPos(), state.facing, -1);
		char b = level == null ? 0 : row(level, door.getBlockPos(), state.facing, 2);
		state.rowLeft = "ROW " + (a == 0 ? 'A' : a);
		state.rowRight = "ROW " + (b == 0 ? (a == 0 ? 'B' : (char) (a + 1)) : b);
		char first = (char) Math.min(a == 0 ? 'A' : a, b == 0 ? 'B' : b);
		state.aisle = "CA-" + ((first - 'A') / 2 + 1);
	}

	/** The row letter of the rack behind the door at model cell dx (its number's first letter), or 0. */
	private static char row(Level level, BlockPos master, Direction facing, int dx) {
		int[] t = AisleContainmentDoorBlock.turn(dx, 1, facing);
		BlockPos at = master.offset(t[0], 0, t[1]);
		if (level.getBlockEntity(at) instanceof LivePanelBlockEntity rack
				&& io.github.jimbozoomer.jugcraft.nuclear.ServerRoom.kind(rack.getBlockState()) == io.github.jimbozoomer.jugcraft.nuclear.ServerRoom.RACK) {
			// (Its number as worked out here: the room only looks live, the server numbers nothing.)
			String label = EdgTelemetry.simLabel(rack, level);
			label = label.isEmpty() ? rack.srvLabel : label;
			char c = label.isEmpty() ? 0 : label.charAt(0);
			return c >= 'A' && c <= 'Z' ? c : 0;
		}
		return 0;
	}

	@Override
	public boolean shouldRenderOffScreen() {
		return true;
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		int light = state.lightCoords;
		float slide = AisleContainmentDoorBlock.SLIDE * state.open / 16F;
		boolean moving = state.open > 0.01F;
		pose.pushPose();
		pose.translate(0.5F, 0, 0.5F);
		pose.rotateDegrees(Axis.YP, -state.facing.toYRot() + 180);
		pose.translate(-0.5F, 0, -0.5F);
		collector.submitCustomGeometry(pose, RENDER_TYPE, (p, consumer) -> {
			draw(p, consumer, AisleContainmentDoorBlock.Part.FRAME_LEFT, 0, light);
			draw(p, consumer, AisleContainmentDoorBlock.Part.FRAME_RIGHT, 1, light);
			draw(p, consumer, moving ? AisleContainmentDoorBlock.Part.LED_OPEN : AisleContainmentDoorBlock.Part.LED_READY, 0, light);
			draw(p, consumer, AisleContainmentDoorBlock.Part.LEAF_LEFT, -slide, light);
			draw(p, consumer, AisleContainmentDoorBlock.Part.LEAF_RIGHT, 1 + slide, light);
		});
		// The label's text, printed on the plates (facing out: -z here), read from outside.
		pose.scale(1 / 16F, 1 / 16F, 1 / 16F);
		text(pose, collector, state.aisle, 23.7F, 0.6F, WHITE, light);
		text(pose, collector, "COLD AISLE", 14.77F, 0.78F, BLUE, light);
		text(pose, collector, state.rowLeft, 16 - TAG_X, 0.58F, INK, light);
		text(pose, collector, state.rowRight, 16 + TAG_X, 0.58F, INK, light);
		pose.popPose();
	}

	/** A line of text centred on x (px), its capitals size px high, on the label's face. */
	private void text(PoseStack pose, SubmitNodeCollector collector, String s, float x, float size, int colour, int light) {
		pose.pushPose();
		pose.translate(x, TEXT_Y, PLATE_Z - 0.03F);
		pose.rotateDegrees(Axis.YP, 180);
		float k = size / 7F;
		pose.scale(k, -k, k);
		var line = Texts.of(s);
		float w = font.width(line);
		collector.submitText(pose, -w / 2, -4.5F, line, false, Font.DisplayMode.POLYGON_OFFSET, light, 0xFF000000 | colour, 0, 0);
		pose.popPose();
	}

	/** One of the master's part models (facing north), moved dx blocks across, its glowing faces at their own light. */
	private static void draw(PoseStack.Pose pose, VertexConsumer consumer, AisleContainmentDoorBlock.Part part, float dx, int light) {
		for (BakedQuad quad : quads(part)) {
			int emission = quad.materialInfo().lightEmission();
			int lit = emission > 0 ? LightCoordsUtil.lightCoordsWithEmission(light, emission) : light;
			Direction d = quad.direction();
			for (int i = 0; i < 4; i++) {
				Vector3fc v = quad.position(i);
				long uv = quad.packedUV(i);
				consumer.addVertex(pose, v.x() + dx, v.y(), v.z()).setColor(0xFFFFFFFF).setUv(UVPair.unpackU(uv), UVPair.unpackV(uv))
						.setOverlay(OverlayTexture.NO_OVERLAY).setLight(lit).setNormal(pose, d.getStepX(), d.getStepY(), d.getStepZ());
			}
		}
	}

	private static List<BakedQuad> quads(AisleContainmentDoorBlock.Part part) {
		Minecraft minecraft = Minecraft.getInstance();
		BlockState state = JugcraftTower.BLOCKS.get("aisle_containment_door").defaultBlockState().setValue(AisleContainmentDoorBlock.PART, part);
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
