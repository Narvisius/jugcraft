package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;

/**
 * Draws a worn Ghost Sheet over the whole wearer, not just the head: a hood with eye holes over the head, the sheet
 * hanging from the shoulders past the hips, and the cloth over the arms and thighs. Each piece is fixed to the body
 * part under it, so the sheet turns with the head, bends when sneaking and swings with the arms and legs. Added to
 * every humanoid renderer (players, costumed zombies and skeletons, armor stands). The sheet's equipment asset has no
 * layers, so the armor layer draws nothing for it and the item is not drawn as a block on the head; having an asset is
 * also what puts it in the render state's head equipment, which this layer reads.
 *
 * <p>The texture (entity/ghost_sheet, 128x64) is laid out like a vanilla model's boxes; tools/regatta_textures.py
 * paints the same {@link #HOOD}... boxes, the eyes on the hood's front.
 */
public class GhostSheetLayer<S extends HumanoidRenderState, M extends HumanoidModel<S>> extends RenderLayer<S, M> {
	private static final RenderType SHEET = RenderTypes.entityCutout(Jugcraft.id("textures/entity/ghost_sheet.png"));
	private static final float TEXTURE_WIDTH = 128;
	private static final float TEXTURE_HEIGHT = 64;

	/** A box in a body part's own space, in pixels, with where its faces sit on the texture and their sizes there. */
	private record Box(float x0, float y0, float z0, float x1, float y1, float z1, int u, int v, int w, int h, int d) {
	}

	/** Over the head: a hood a pixel clear of it, rounded on top. */
	static final Box HOOD = new Box(-5, -9, -5, 5, 1, 5, 0, 0, 10, 10, 10);
	static final Box CROWN = new Box(-4, -10, -4, 4, -9, 4, 40, 0, 8, 1, 8);
	/** Hanging from the shoulders over the chest and back, then flaring a little to below the knees. */
	static final Box DRAPE = new Box(-5, -0.6F, -3, 5, 12.5F, 3, 0, 20, 10, 13, 6);
	static final Box SKIRT = new Box(-5.5F, 12, -4, 5.5F, 21, 4, 40, 20, 11, 9, 8);
	/** Over the arms (wide or slim) and the thighs. */
	static final Box RIGHT_SLEEVE = new Box(-3.6F, -2.6F, -2.6F, 1.6F, 10.6F, 2.6F, 80, 0, 5, 13, 5);
	static final Box LEFT_SLEEVE = new Box(-1.6F, -2.6F, -2.6F, 3.6F, 10.6F, 2.6F, 100, 0, 5, 13, 5);
	static final Box RIGHT_THIGH = new Box(-2.6F, -0.6F, -2.6F, 2.6F, 9, 2.6F, 80, 20, 5, 9, 5);
	static final Box LEFT_THIGH = new Box(-2.6F, -0.6F, -2.6F, 2.6F, 9, 2.6F, 100, 20, 5, 9, 5);

	public GhostSheetLayer(RenderLayerParent<S, M> parent) {
		super(parent);
	}

	/** Adds the layer to every humanoid renderer. */
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static void register() {
		LivingEntityRenderLayerRegistrationCallback.EVENT.register((type, renderer, helper, context) -> {
			if (renderer.getModel() instanceof HumanoidModel<?>) {
				helper.register(new GhostSheetLayer((RenderLayerParent) renderer));
			}
		});
	}

	@Override
	public void submit(PoseStack pose, SubmitNodeCollector collector, int light, S state, float yRot, float xRot) {
		if (!state.headEquipment.is(JugcraftAgriculture.item("ghost_sheet"))) {
			return;
		}
		M model = getParentModel();
		part(pose, collector, light, model.head, HOOD, CROWN);
		part(pose, collector, light, model.body, DRAPE, SKIRT);
		part(pose, collector, light, model.rightArm, RIGHT_SLEEVE);
		part(pose, collector, light, model.leftArm, LEFT_SLEEVE);
		part(pose, collector, light, model.rightLeg, RIGHT_THIGH);
		part(pose, collector, light, model.leftLeg, LEFT_THIGH);
	}

	/** Draws {@code boxes} in the space of {@code part}, posed as the wearer's part is (not over a part not shown). */
	private static void part(PoseStack pose, SubmitNodeCollector collector, int light, ModelPart part, Box... boxes) {
		if (!part.visible) {
			return;
		}
		pose.pushPose();
		part.translateAndRotate(pose);
		collector.submitCustomGeometry(pose, SHEET, (matrix, buffer) -> {
			for (Box box : boxes) {
				box(buffer, matrix, box, light);
			}
		});
		pose.popPose();
	}

	/**
	 * The six faces of a box, textured as a vanilla model's box is: the top and bottom in a row above the four sides,
	 * which run west, north, east, south. Model space has y downwards, so the top face is the one at y0.
	 */
	private static void box(VertexConsumer buffer, PoseStack.Pose matrix, Box b, int light) {
		int u = b.u();
		int v = b.v();
		int w = b.w();
		int h = b.h();
		int d = b.d();
		// Top (y0) and bottom (y1).
		face(buffer, matrix, light, 0, -1, 0, u + d, v, w, d,
				b.x0(), b.y0(), b.z0(), b.x1(), b.y0(), b.z0(), b.x1(), b.y0(), b.z1(), b.x0(), b.y0(), b.z1());
		face(buffer, matrix, light, 0, 1, 0, u + d + w, v, w, d,
				b.x0(), b.y1(), b.z1(), b.x1(), b.y1(), b.z1(), b.x1(), b.y1(), b.z0(), b.x0(), b.y1(), b.z0());
		// West (x0), north (z0), east (x1), south (z1): each from its top-left corner as seen from outside.
		face(buffer, matrix, light, -1, 0, 0, u, v + d, d, h,
				b.x0(), b.y0(), b.z1(), b.x0(), b.y0(), b.z0(), b.x0(), b.y1(), b.z0(), b.x0(), b.y1(), b.z1());
		face(buffer, matrix, light, 0, 0, -1, u + d, v + d, w, h,
				b.x0(), b.y0(), b.z0(), b.x1(), b.y0(), b.z0(), b.x1(), b.y1(), b.z0(), b.x0(), b.y1(), b.z0());
		face(buffer, matrix, light, 1, 0, 0, u + d + w, v + d, d, h,
				b.x1(), b.y0(), b.z0(), b.x1(), b.y0(), b.z1(), b.x1(), b.y1(), b.z1(), b.x1(), b.y1(), b.z0());
		face(buffer, matrix, light, 0, 0, 1, u + 2 * d + w, v + d, w, h,
				b.x1(), b.y0(), b.z1(), b.x0(), b.y0(), b.z1(), b.x0(), b.y1(), b.z1(), b.x1(), b.y1(), b.z1());
	}

	/**
	 * One face: corners in pixels (top-left, top-right, bottom-right, bottom-left on the texture), the texture rectangle
	 * at ({@code tu}, {@code tv}) {@code tw} by {@code th} texels, and the outward normal.
	 */
	private static void face(VertexConsumer buffer, PoseStack.Pose matrix, int light, float nx, float ny, float nz,
			int tu, int tv, int tw, int th, float... corners) {
		float[][] uv = {{tu, tv}, {tu + tw, tv}, {tu + tw, tv + th}, {tu, tv + th}};
		for (int i = 0; i < 4; i++) {
			buffer.addVertex(matrix, corners[i * 3] / 16.0F, corners[i * 3 + 1] / 16.0F, corners[i * 3 + 2] / 16.0F).setColor(0xFFFFFFFF)
					.setUv(uv[i][0] / TEXTURE_WIDTH, uv[i][1] / TEXTURE_HEIGHT).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
					.setNormal(matrix, nx, ny, nz);
		}
	}
}
