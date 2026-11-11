package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.drone.DepotDisplayBlockEntity;
import io.github.jimbozoomer.jugcraft.tower.CradleStrutsBlock;
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
 * The Cradle Struts (tower/CradleStrutsBlock; work list item 12): the pair's mesh from tools/cradle_struts.py, made for
 * the pair facing south, turned a quarter clockwise (seen from above) about the master cell's middle for each step
 * round to its facing. Shaded by the world with the corners' baked ambient occlusion.
 */
public final class CradleStrutsRenderer implements BlockEntityRenderer<DepotDisplayBlockEntity, CradleStrutsRenderer.State> {
	static final String MESH_NAME = "cradle_struts";
	static final Identifier TEXTURE = Jugcraft.id("textures/entity/cradle_struts.png");
	private static final RenderType MESH = RenderTypes.entityCutout(TEXTURE);

	public CradleStrutsRenderer(BlockEntityRendererProvider.Context context) {
	}

	public static final class State extends BlockEntityRenderState {
		int turns;
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	/** The struts rise seven blocks and reach four across from their master cell. */
	@Override
	public boolean shouldRenderOffScreen() {
		return true;
	}

	@Override
	public int getViewDistance() {
		return 160;
	}

	@Override
	public void extractRenderState(DepotDisplayBlockEntity be, State state, float partialTick, Vec3 cameraPos,
			ModelFeatureRenderer.CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(be, state, crumbling);
		BlockState block = be.getBlockState();
		Direction facing = block.getBlock() instanceof CradleStrutsBlock ? block.getValue(CradleStrutsBlock.FACING) : Direction.SOUTH;
		state.turns = CradleStrutsBlock.turns(facing);
	}

	@Override
	public void submit(State state, PoseStack poses, SubmitNodeCollector collector, CameraRenderState camera) {
		ConsoleMesh mesh = ConsoleMesh.get(MESH_NAME);
		if (mesh == null || BakedFixtures.baked(io.github.jimbozoomer.jugcraft.tower.JugcraftTower.CRADLE_STRUTS)) {   // baked: in the chunk's mesh
			return;
		}
		poses.pushPose();
		poses.translate(0.5, 0, 0.5);
		// a quarter clockwise seen from above takes (x, z) to (-z, x)
		poses.mulPose(new Matrix4f().rotationY((float) (-Math.PI / 2 * state.turns)));
		poses.translate(-0.5, 0, -0.5);
		int light = state.lightCoords;
		collector.submitCustomGeometry(poses, MESH, (pose, consumer) -> {
			DroneDepotRenderer.Geometry g = new DroneDepotRenderer.Geometry(pose, consumer);
			ConsoleMesh.emit(g, mesh.quads[ConsoleMesh.SOLID], light, false);
		});
		poses.popPose();
	}
}
