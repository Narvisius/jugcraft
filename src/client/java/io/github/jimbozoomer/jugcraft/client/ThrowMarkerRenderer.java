package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.ThrowMarker;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/**
 * A trebuchet's landing marker: a little chequered flag on a stake, its distance shown above it (the entity's
 * name). Texture (16 x 16): the stake in the left two columns, the flag in the right three quarters' top half.
 */
public class ThrowMarkerRenderer extends EntityRenderer<ThrowMarker, EntityRenderState> {
	private static final Identifier TEXTURE = Jugcraft.id("textures/entity/throw_marker.png");
	private static final float POLE = 0.03F;
	private static final float HEIGHT = 1.0F;

	public ThrowMarkerRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public EntityRenderState createRenderState() {
		return new EntityRenderState();
	}

	@Override
	public void submit(EntityRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		int light = state.lightCoords;
		collector.submitCustomGeometry(pose, RenderTypes.entityCutout(TEXTURE), (matrix, buffer) -> {
			// The stake: four sides.
			float p = POLE;
			quad(buffer, matrix, light, new float[][] {{-p, HEIGHT, -p}, {-p, 0, -p}, {p, 0, -p}, {p, HEIGHT, -p}}, 0, 0, 0.125F, 1, 0, 0, -1);
			quad(buffer, matrix, light, new float[][] {{p, HEIGHT, p}, {p, 0, p}, {-p, 0, p}, {-p, HEIGHT, p}}, 0, 0, 0.125F, 1, 0, 0, 1);
			quad(buffer, matrix, light, new float[][] {{-p, HEIGHT, p}, {-p, 0, p}, {-p, 0, -p}, {-p, HEIGHT, -p}}, 0, 0, 0.125F, 1, -1, 0, 0);
			quad(buffer, matrix, light, new float[][] {{p, HEIGHT, -p}, {p, 0, -p}, {p, 0, p}, {p, HEIGHT, p}}, 0, 0, 0.125F, 1, 1, 0, 0);
			// The flag: both faces.
			float top = HEIGHT;
			float bottom = HEIGHT - 0.3F;
			quad(buffer, matrix, light, new float[][] {{p, top, 0}, {p, bottom, 0}, {0.5F, bottom, 0}, {0.5F, top, 0}}, 0.25F, 0, 1, 0.5F, 0, 0, 1);
			quad(buffer, matrix, light, new float[][] {{0.5F, top, 0}, {0.5F, bottom, 0}, {p, bottom, 0}, {p, top, 0}}, 1, 0, 0.25F, 0.5F, 0, 0, -1);
		});
		super.submit(state, pose, collector, camera);
	}

	/** One quad: corners top-left, bottom-left, bottom-right, top-right; UVs from (u0, v0) to (u1, v1). */
	private static void quad(VertexConsumer buffer, PoseStack.Pose matrix, int light, float[][] corners, float u0, float v0, float u1, float v1,
			float nx, float ny, float nz) {
		float[][] uv = {{u0, v0}, {u0, v1}, {u1, v1}, {u1, v0}};
		for (int i = 0; i < 4; i++) {
			buffer.addVertex(matrix, corners[i][0], corners[i][1], corners[i][2]).setColor(0xFFFFFFFF).setUv(uv[i][0], uv[i][1])
					.setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(matrix, nx, ny, nz);
		}
	}
}
