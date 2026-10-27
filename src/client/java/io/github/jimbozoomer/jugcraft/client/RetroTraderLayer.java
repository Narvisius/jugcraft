package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback;
import net.minecraft.client.model.npc.VillagerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityTypes;
import org.joml.Quaternionf;

/**
 * Draws the Retro Trader's living face, his beanie and, during his stomp-clap, his free arms ({@link RetroTraderPose}).
 * The face is two quads a hair in front of the head's front, turning and nodding with the head: the eyes (rows 0-6:
 * brows, glasses, eyes) and the mouth (rows 7-9: moustache, mouth, beard), each a frame of
 * textures/entity/retro_trader/expressions.png. The beanie is three boxes on the head from the same sheet (BEANIE in
 * tools/retro_trader_look.py): a ribbed cuff standing just off his hairline, a knit crown above the head and a smaller
 * dome on top, set back a little so it slouches. The free arms are two 4x10x4 boxes from the same sheet, posed about
 * the shoulders; the model mixin hides the villager's folded arms while they are drawn. Added to the villager renderer
 * only; other villagers have no pose and are left alone.
 */
public class RetroTraderLayer extends RenderLayer<VillagerRenderState, VillagerModel> {
	private static final Identifier TEXTURE = Jugcraft.id("textures/entity/retro_trader/expressions.png");
	private static final RenderType RENDER_TYPE = RenderTypes.entityCutout(TEXTURE);
	private static final float SHEET = 64.0F;
	/** The face's distance in front of the head's front, in pixels. */
	private static final float FACE_OFFSET = 0.03F;
	/**
	 * The beanie's boxes in head space (pixels): origin x, y, z, size w, h, d, texture size w, h, d and texture offset u,
	 * v. Keep in step with BEANIE in tools/retro_trader_look.py.
	 */
	private static final float[][] BEANIE = {
			{-4.5F, -10.0F, -4.5F, 9.0F, 2.0F, 9.0F, 9, 2, 9, 0, 20},
			{-4.3F, -12.4F, -4.0F, 8.6F, 2.4F, 8.6F, 9, 3, 9, 0, 48},
			{-3.5F, -13.2F, -3.0F, 7.0F, 0.8F, 7.0F, 7, 1, 7, 36, 20}};

	public RetroTraderLayer(RenderLayerParent<VillagerRenderState, VillagerModel> parent) {
		super(parent);
	}

	/** Adds the layer to the villager renderer. */
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static void register() {
		LivingEntityRenderLayerRegistrationCallback.EVENT.register((type, renderer, helper, context) -> {
			if (type == EntityTypes.VILLAGER && renderer.getModel() instanceof VillagerModel) {
				helper.register(new RetroTraderLayer((RenderLayerParent) renderer));
			}
		});
	}

	@Override
	public void submit(PoseStack pose, SubmitNodeCollector collector, int light, VillagerRenderState state, float yRot, float xRot) {
		RetroTraderPose look = ((RetroTraderPose.Holder) state).jugcraft$retroTrader();
		if (look == null || state.isInvisible) {
			return;
		}
		int overlay = LivingEntityRenderer.getOverlayCoords(state, 0.0F);
		VillagerModel model = getParentModel();
		if (model.getHead().visible) {
			pose.pushPose();
			model.getHead().translateAndRotate(pose);
			int eyes = look.eyes;
			int mouth = look.mouth;
			collector.submitCustomGeometry(pose, RENDER_TYPE, (matrix, buffer) -> {
				face(buffer, matrix, -10, 7, 8 * (eyes % 8), 7 * (eyes / 8), light, overlay);
				face(buffer, matrix, -3, 3, 8 * (mouth % 8), 16 + 3 * (mouth / 8), light, overlay);
				for (float[] box : BEANIE) {
					cube(buffer, matrix, box[0], box[1], box[2], box[3], box[4], box[5], (int) box[6], (int) box[7], (int) box[8], (int) box[9],
							(int) box[10], light, overlay);
				}
			});
			pose.popPose();
		}
		if (look.freeArms) {
			arm(pose, collector, -RetroTraderPose.SHOULDER_X, look.rightArm, look.bounce, 0, 32, light, overlay);
			arm(pose, collector, RetroTraderPose.SHOULDER_X, look.leftArm, look.bounce, 16, 32, light, overlay);
		}
	}

	/** A strip of the face: {@code rows} rows from head y {@code top}, showing the sheet's frame at (u, v). */
	private static void face(VertexConsumer buffer, PoseStack.Pose matrix, float top, int rows, int u, int v, int light, int overlay) {
		float z = (-4 - FACE_OFFSET) / 16;
		float x0 = -4.0F / 16;
		float x1 = 4.0F / 16;
		float y0 = top / 16;
		float y1 = (top + rows) / 16;
		float u0 = u / SHEET;
		float u1 = (u + 8) / SHEET;
		float v0 = v / SHEET;
		float v1 = (v + rows) / SHEET;
		vertex(buffer, matrix, x1, y0, z, u1, v0, light, overlay, 0, 0, -1);
		vertex(buffer, matrix, x0, y0, z, u0, v0, light, overlay, 0, 0, -1);
		vertex(buffer, matrix, x0, y1, z, u0, v1, light, overlay, 0, 0, -1);
		vertex(buffer, matrix, x1, y1, z, u1, v1, light, overlay, 0, 0, -1);
	}

	private static void arm(PoseStack pose, SubmitNodeCollector collector, float shoulderX, float[] rotation, float bounce, int u, int v,
			int light, int overlay) {
		pose.pushPose();
		pose.translate(shoulderX / 16, (RetroTraderPose.SHOULDER_Y + bounce) / 16, RetroTraderPose.SHOULDER_Z / 16);
		pose.rotate(new Quaternionf().rotationZYX(rotation[2], rotation[1], rotation[0]));
		collector.submitCustomGeometry(pose, RENDER_TYPE, (matrix, buffer) -> cube(buffer, matrix, -2, -2, -2, 4, 10, 4, 4, 10, 4, u, v, light, overlay));
		pose.popPose();
	}

	/**
	 * A box from (x, y, z), {@code w} by {@code h} by {@code d} pixels, textured like a model part's cube of texture size
	 * {@code tw} by {@code th} by {@code td} at texture offset (u, v): the same faces, corners, texture rectangles and
	 * normals as the game's own cubes. The texture size is whole pixels; the box may be a little larger or smaller.
	 */
	private static void cube(VertexConsumer buffer, PoseStack.Pose matrix, float x, float y, float z, float w, float h, float d, int tw,
			int th, int td, int u, int v, int light, int overlay) {
		float f = x / 16;
		float g = y / 16;
		float k0 = z / 16;
		float i = (x + w) / 16;
		float j = (y + h) / 16;
		float k = (z + d) / 16;
		float[][] corners = {null, {f, g, k0}, {i, g, k0}, {i, j, k0}, {f, j, k0}, {f, g, k}, {i, g, k}, {i, j, k}, {f, j, k}};
		float u0 = u;
		float u1 = u + td;
		float u2 = u + td + tw;
		float u3 = u + td + tw + tw;
		float u4 = u + td + tw + td;
		float u5 = u + td + tw + td + tw;
		float v0 = v;
		float v1 = v + td;
		float v2 = v + td + th;
		polygon(buffer, matrix, corners, new int[] {6, 5, 1, 2}, u1, v0, u2, v1, 0, -1, 0, light, overlay);
		polygon(buffer, matrix, corners, new int[] {3, 4, 8, 7}, u2, v1, u3, v0, 0, 1, 0, light, overlay);
		polygon(buffer, matrix, corners, new int[] {1, 5, 8, 4}, u0, v1, u1, v2, -1, 0, 0, light, overlay);
		polygon(buffer, matrix, corners, new int[] {2, 1, 4, 3}, u1, v1, u2, v2, 0, 0, -1, light, overlay);
		polygon(buffer, matrix, corners, new int[] {6, 2, 3, 7}, u2, v1, u4, v2, 1, 0, 0, light, overlay);
		polygon(buffer, matrix, corners, new int[] {5, 6, 7, 8}, u4, v1, u5, v2, 0, 0, 1, light, overlay);
	}

	/** One face of a cube: corners in order get (ub, va), (ua, va), (ua, vb), (ub, vb), as the game's cubes do. */
	private static void polygon(VertexConsumer buffer, PoseStack.Pose matrix, float[][] corners, int[] order, float ua, float va, float ub,
			float vb, float nx, float ny, float nz, int light, int overlay) {
		float[][] uv = {{ub, va}, {ua, va}, {ua, vb}, {ub, vb}};
		for (int n = 0; n < 4; n++) {
			float[] c = corners[order[n]];
			vertex(buffer, matrix, c[0], c[1], c[2], uv[n][0] / SHEET, uv[n][1] / SHEET, light, overlay, nx, ny, nz);
		}
	}

	private static void vertex(VertexConsumer buffer, PoseStack.Pose matrix, float x, float y, float z, float u, float v, int light, int overlay,
			float nx, float ny, float nz) {
		buffer.addVertex(matrix, x, y, z).setColor(0xFFFFFFFF).setUv(u, v).setOverlay(overlay).setLight(light).setNormal(matrix, nx, ny, nz);
	}
}
