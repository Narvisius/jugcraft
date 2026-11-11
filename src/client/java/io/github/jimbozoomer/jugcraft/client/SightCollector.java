package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import java.util.function.Function;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.Mesh;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.MeshView;
import net.fabricmc.fabric.api.client.rendering.v1.SubmitRenderPhase;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.feature.submit.SubmitNode;
import net.minecraft.client.renderer.gizmos.DrawableGizmoPrimitives;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.UvMapping;
import net.minecraft.client.resources.model.geometry.ItemQuads;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/**
 * Passes everything a block entity's renderer submits on to the real collector, measuring where it lands ({@link Sight}
 * learns each block entity's extent this way): custom geometry vertex by vertex, models, items and text round the
 * origin of their pose.
 */
final class SightCollector implements SubmitNodeCollector {
	/** How far round a model's, item's or text's origin it is taken to reach (blocks). */
	private static final double AROUND = 1.0;

	private final OrderedSubmitNodeCollector out;
	private final SubmitNodeCollector root;
	private final Sight.Bounds bounds;
	private final Matrix4f inv;
	private final int ox, oy, oz;

	SightCollector(SubmitNodeCollector out, Sight.Bounds bounds, Matrix4f inv, int ox, int oy, int oz) {
		this(out, out, bounds, inv, ox, oy, oz);
	}

	private SightCollector(OrderedSubmitNodeCollector out, SubmitNodeCollector root, Sight.Bounds bounds, Matrix4f inv, int ox, int oy, int oz) {
		this.out = out;
		this.root = root;
		this.bounds = bounds;
		this.inv = inv;
		this.ox = ox;
		this.oy = oy;
		this.oz = oz;
	}

	private void around(PoseStack pose) {
		bounds.addAround(inv, pose.last().pose(), AROUND, ox, oy, oz);
	}

	@Override
	public OrderedSubmitNodeCollector order(int order) {
		if (root == null) {
			throw new UnsupportedOperationException();
		}
		return new SightCollector(root.order(order), null, bounds, inv, ox, oy, oz);
	}

	@Override
	public void submitShadow(PoseStack pose, float radius, List<EntityRenderState.ShadowPiece> pieces) {
		out.submitShadow(pose, radius, pieces);
	}

	@Override
	public void submitNameTag(PoseStack pose, Vec3 at, int y, Component text, boolean seeThrough, int light, CameraRenderState camera) {
		around(pose);
		out.submitNameTag(pose, at, y, text, seeThrough, light, camera);
	}

	@Override
	public void submitText(PoseStack pose, float x, float y, FormattedCharSequence text, boolean shadow, Font.DisplayMode mode, int light, int colour,
			int background, int outline) {
		around(pose);
		out.submitText(pose, x, y, text, shadow, mode, light, colour, background, outline);
	}

	@Override
	public void submitTextBackground(PoseStack pose, float x0, float y0, float x1, float y1, int colour, Font.DisplayMode mode, int light) {
		around(pose);
		out.submitTextBackground(pose, x0, y0, x1, y1, colour, mode, light);
	}

	@Override
	public void submitFlame(PoseStack pose, EntityRenderState state, Quaternionf rotation) {
		around(pose);
		out.submitFlame(pose, state, rotation);
	}

	@Override
	public void submitLeash(PoseStack pose, EntityRenderState.LeashState leash) {
		around(pose);
		out.submitLeash(pose, leash);
	}

	@Override
	public <S> void submitModel(Model<? super S> model, S state, PoseStack pose, RenderType type, int light, int overlay, int colour, UvMapping uv,
			int outline) {
		around(pose);
		out.submitModel(model, state, pose, type, light, overlay, colour, uv, outline);
	}

	@Override
	public <S> void submitCrumblingOverlay(Model<? super S> model, S state, PoseStack pose, RenderType type, int light, int overlay, int colour,
			ModelFeatureRenderer.CrumblingOverlay crumbling) {
		out.submitCrumblingOverlay(model, state, pose, type, light, overlay, colour, crumbling);
	}

	@Override
	public void submitMovingBlock(PoseStack pose, MovingBlockRenderState state, int light) {
		around(pose);
		out.submitMovingBlock(pose, state, light);
	}

	@Override
	public void submitBlockModel(PoseStack pose, RenderType type, List<BlockStateModelPart> parts, int[] tints, int light, int overlay, int outline) {
		around(pose);
		out.submitBlockModel(pose, type, parts, tints, light, overlay, outline);
	}

	@Override
	public void submitBreakingBlockModel(PoseStack pose, List<BlockStateModelPart> parts, int progress, boolean flag) {
		out.submitBreakingBlockModel(pose, parts, progress, flag);
	}

	@Override
	public void submitShapeOutline(PoseStack pose, VoxelShape shape, RenderType type, int colour, float width, boolean flag) {
		around(pose);
		out.submitShapeOutline(pose, shape, type, colour, width, flag);
	}

	@Override
	public void submitItem(PoseStack pose, ItemDisplayContext context, int light, int overlay, int outline, int[] tints, ItemQuads quads,
			ItemStackRenderState.FoilType foil) {
		around(pose);
		out.submitItem(pose, context, light, overlay, outline, tints, quads, foil);
	}

	@Override
	public void submitCustomGeometry(PoseStack pose, RenderType type, SubmitNodeCollector.CustomGeometryRenderer renderer) {
		Sight.Bounds b = bounds;
		Matrix4f m = inv;
		int x = ox, y = oy, z = oz;
		out.submitCustomGeometry(pose, type, (p, consumer) -> renderer.render(p, new SightConsumer(consumer, b, m, x, y, z)));
	}

	@Override
	public void submitQuadParticleGroup(QuadParticleRenderState state) {
		out.submitQuadParticleGroup(state);
	}

	@Override
	public void submitGizmoPrimitives(DrawableGizmoPrimitives.Group group, CameraRenderState camera, boolean flag) {
		out.submitGizmoPrimitives(group, camera, flag);
	}

	// Fabric's additions

	@Override
	public void submitBlockModel(PoseStack pose, Function<ChunkSectionLayer, RenderType> types, boolean flag, List<BlockStateModelPart> parts, Mesh mesh,
			int[] tints, int light, int overlay, int outline) {
		around(pose);
		out.submitBlockModel(pose, types, flag, parts, mesh, tints, light, overlay, outline);
	}

	@Override
	public void submitBreakingBlockModel(PoseStack pose, List<BlockStateModelPart> parts, Mesh mesh, int progress, boolean flag) {
		out.submitBreakingBlockModel(pose, parts, mesh, progress, flag);
	}

	@Override
	public void submitItem(PoseStack pose, ItemDisplayContext context, int light, int overlay, int outline, int[] tints, ItemQuads quads, MeshView mesh,
			ItemStackRenderState.FoilType foil) {
		around(pose);
		out.submitItem(pose, context, light, overlay, outline, tints, quads, mesh, foil);
	}

	@Override
	public <T extends SubmitNode> void submitCustom(SubmitRenderPhase<T> phase, T node) {
		out.submitCustom(phase, node);
	}

	/** Passes vertices on, adding each one's position to the block entity's learned extent. */
	static final class SightConsumer implements VertexConsumer {
		private final VertexConsumer out;
		private final Sight.Bounds bounds;
		private final Matrix4f inv;
		private final int ox, oy, oz;

		SightConsumer(VertexConsumer out, Sight.Bounds bounds, Matrix4f inv, int ox, int oy, int oz) {
			this.out = out;
			this.bounds = bounds;
			this.inv = inv;
			this.ox = ox;
			this.oy = oy;
			this.oz = oz;
		}

		/** A box {@code r} round (x, y, z) (where its corners would land) into the extent, without drawing anything (a kept shape). */
		void learn(float x, float y, float z, float r) {
			for (int k = 0; k < 8; k++) {
				bounds.add(inv, x + ((k & 1) == 0 ? -r : r), y + ((k & 2) == 0 ? -r : r), z + ((k & 4) == 0 ? -r : r), ox, oy, oz);
			}
		}

		@Override
		public VertexConsumer addVertex(float x, float y, float z) {
			bounds.add(inv, x, y, z, ox, oy, oz);
			out.addVertex(x, y, z);
			return this;
		}

		@Override
		public void addVertex(float x, float y, float z, int colour, float u, float v, int overlay, int light, float nx, float ny, float nz) {
			bounds.add(inv, x, y, z, ox, oy, oz);
			out.addVertex(x, y, z, colour, u, v, overlay, light, nx, ny, nz);
		}

		@Override
		public VertexConsumer setColor(int r, int g, int b, int a) {
			out.setColor(r, g, b, a);
			return this;
		}

		@Override
		public VertexConsumer setColor(int argb) {
			out.setColor(argb);
			return this;
		}

		@Override
		public VertexConsumer setUv(float u, float v) {
			out.setUv(u, v);
			return this;
		}

		@Override
		public VertexConsumer setUv1(int u, int v) {
			out.setUv1(u, v);
			return this;
		}

		@Override
		public VertexConsumer setUv2(int u, int v) {
			out.setUv2(u, v);
			return this;
		}

		@Override
		public VertexConsumer setUv3(float u, float v) {
			out.setUv3(u, v);
			return this;
		}

		@Override
		public VertexConsumer setNormal(float x, float y, float z) {
			out.setNormal(x, y, z);
			return this;
		}

		@Override
		public VertexConsumer setLineWidth(float width) {
			out.setLineWidth(width);
			return this;
		}
	}
}
