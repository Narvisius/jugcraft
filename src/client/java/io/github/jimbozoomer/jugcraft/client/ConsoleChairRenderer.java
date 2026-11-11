package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jimbozoomer.jugcraft.tower.ConsoleChairBlock;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * The Console Chair (tower/ConsoleChairBlock): its mesh from tools/tower_control_console.py, turned to the chair's
 * rotation and pulled up {@link ConsoleChairBlock#PUSH} towards where it faces, standing on the raised floor's top
 * when it is set on one (the floor tile itself is the block's model).
 */
public final class ConsoleChairRenderer implements BlockEntityRenderer<ConsoleChairBlock.ConsoleChairBlockEntity, ConsoleChairRenderer.State> {
	private static final RenderType MESH = RenderTypes.entityCutout(ConsoleMesh.TEXTURE);

	public ConsoleChairRenderer(BlockEntityRendererProvider.Context context) {
	}

	public static final class State extends BlockEntityRenderState {
		int rotation;
		boolean platform;
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(ConsoleChairBlock.ConsoleChairBlockEntity chair, State state, float partialTick, Vec3 cameraPos,
			ModelFeatureRenderer.CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(chair, state, crumbling);
		BlockState block = chair.getBlockState();
		state.rotation = block.getValue(ConsoleChairBlock.ROTATION);
		state.platform = block.getValue(ConsoleChairBlock.PLATFORM);
	}

	@Override
	public void submit(State state, PoseStack poses, SubmitNodeCollector collector, CameraRenderState camera) {
		ConsoleMesh mesh = ConsoleMesh.get("console_chair");
		if (mesh == null) {
			return;
		}
		double yaw = Math.toRadians(state.rotation * 22.5);
		poses.pushPose();
		poses.translate(0.5 - Math.sin(yaw) * ConsoleChairBlock.PUSH, state.platform ? 3 / 16.0 : 0, 0.5 + Math.cos(yaw) * ConsoleChairBlock.PUSH);
		// the mesh faces +z (south, yaw 0); a yaw turns it clockwise seen from above
		poses.mulPose(new Matrix4f().rotationY((float) -yaw));
		int light = state.lightCoords;
		collector.submitCustomGeometry(poses, MESH, (pose, consumer) -> {
			DroneDepotRenderer.Geometry g = new DroneDepotRenderer.Geometry(pose, consumer);
			ConsoleMesh.emit(g, mesh.quads[ConsoleMesh.SOLID], light, false);
			ConsoleMesh.emit(g, mesh.quads[ConsoleMesh.LIT], light, true);
		});
		poses.popPose();
	}
}
