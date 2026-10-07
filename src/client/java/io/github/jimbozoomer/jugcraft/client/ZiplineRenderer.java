package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.rocketry.ZiplineAnchorBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

/**
 * A zipline's steel line (batch 40), drawn taut from the anchor that holds it to the other one, tinted over the grapple
 * line's plain light texture. Drawn off screen too, since a line can be long.
 */
public class ZiplineRenderer implements BlockEntityRenderer<ZiplineAnchorBlockEntity, ZiplineRenderer.State> {
	private static final RenderType LINE = RenderTypes.entityCutout(Jugcraft.id("textures/entity/grapple_hook.png"));
	private static final int STEEL = 0xFF5A5E66;
	/** Half the line's width, in blocks. */
	private static final float WIDTH = 0.7F / 16;

	public static final class State extends BlockEntityRenderState {
		@Nullable Vec3 offset;
	}

	public ZiplineRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(ZiplineAnchorBlockEntity anchor, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(anchor, state, crumbling);
		BlockPos link = anchor.link();
		state.offset = link != null && anchor.draws() ? Vec3.atLowerCornerOf(link.subtract(anchor.getBlockPos())) : null;
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		Vec3 offset = state.offset;
		if (offset == null) {
			return;
		}
		int light = state.lightCoords;
		Vector3f a = new Vector3f(0.5F, (float) ZiplineAnchorBlockEntity.ATTACH, 0.5F);
		Vector3f b = new Vector3f(a).add((float) offset.x, (float) offset.y, (float) offset.z);
		collector.submitCustomGeometry(pose, LINE, (matrix, buffer) -> {
			strip(buffer, matrix, light, a, b, new Vector3f(0, WIDTH, 0));
			Vector3f side = new Vector3f(b).sub(a).cross(0, 1, 0);
			strip(buffer, matrix, light, a, b, side.lengthSquared() < 1.0E-8F ? new Vector3f(WIDTH, 0, 0) : side.normalize().mul(WIDTH));
		});
	}

	/**
	 * A flat strip of line from {@code a} to {@code b}, {@code across} wide each side, seen from both sides: two sides
	 * lifted off the strip's middle ({@link DecorDraw#twoSided}), never one plane drawn twice.
	 */
	private static void strip(VertexConsumer buffer, PoseStack.Pose matrix, int light, Vector3f a, Vector3f b, Vector3f across) {
		Vector3f[] at = {new Vector3f(a).sub(across), new Vector3f(a).add(across), new Vector3f(b).add(across), new Vector3f(b).sub(across)};
		float[][] uv = {{0, 1}, {1, 1}, {1, 0}, {0, 0}};
		float[][] corners = new float[4][];
		for (int i = 0; i < 4; i++) {
			corners[i] = new float[] {at[i].x(), at[i].y(), at[i].z(), uv[i][0], uv[i][1]};
		}
		Vector3f normal = new Vector3f(across).cross(new Vector3f(b).sub(a));
		normal = normal.lengthSquared() < 1.0E-12F ? new Vector3f(0, 1, 0) : normal.normalize();
		DecorDraw.twoSided(buffer, matrix, corners, normal.x(), normal.y(), normal.z(), STEEL, light, DecorDraw.TWO_SIDED_LIFT);
	}

	@Override
	public boolean shouldRenderOffScreen() {
		return true;
	}

	@Override
	public int getViewDistance() {
		return 128;
	}
}
