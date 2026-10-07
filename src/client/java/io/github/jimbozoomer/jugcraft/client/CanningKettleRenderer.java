package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.CanningKettleBlockEntity;
import java.util.List;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * What is in a Canning Kettle: up to four jars standing on its rack, two by two, and the water round them, lapping a
 * little higher while it boils.
 */
public class CanningKettleRenderer implements BlockEntityRenderer<CanningKettleBlockEntity, CanningKettleRenderer.State> {
	private static final RenderType WATER = RenderTypes.entityCutout(Jugcraft.id("textures/entity/kettle_water.png"));
	private static final float[][] SPOTS = {{5.5F, 1.5F, 5.5F}, {10.5F, 1.5F, 5.5F}, {5.5F, 1.5F, 10.5F}, {10.5F, 1.5F, 10.5F}};

	public static final class State extends BlockEntityRenderState {
		boolean water;
		boolean boiling;
		List<ItemStack> jars = List.of();
		float time;
	}

	public CanningKettleRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(CanningKettleBlockEntity kettle, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(kettle, state, crumbling);
		state.water = kettle.water();
		state.boiling = kettle.boiling();
		state.jars = kettle.jars();
		state.time = kettle.getLevel() == null ? 0 : kettle.getLevel().getGameTime() + partialTick;
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		int light = state.lightCoords;
		PreserveJars.submit(collector, pose, state.jars, SPOTS, 3.5F, 5.5F, light);
		if (state.water) {
			float level = 6.5F + (state.boiling ? 0.25F * (float) Math.sin(state.time * 0.8F) : 0.0F);
			collector.submitCustomGeometry(pose, WATER, (matrix, buffer) -> TintedBoxes.top(buffer, matrix, 3, 3, 13, 13, level, 0xFFFFFFFF, light));
		}
	}
}
