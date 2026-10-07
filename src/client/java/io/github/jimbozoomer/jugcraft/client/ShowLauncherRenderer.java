package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.ShowLauncherBlock;
import io.github.jimbozoomer.jugcraft.agriculture.ShowLauncherBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.SpookyFireworkItem;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The rockets loaded in a Show Launcher: the nose of the next rocket peeking out of each loaded tube, a paper body in
 * the colour of the picture it bursts into (a vanilla rocket's red), with a pale tip. Drawn for a launcher facing north
 * (the front row of tubes at the north) and turned to its facing.
 */
public class ShowLauncherRenderer implements BlockEntityRenderer<ShowLauncherBlockEntity, ShowLauncherRenderer.State> {
	private static final RenderType PAPER = RenderTypes.entityCutout(Jugcraft.id("textures/entity/show_launcher_rocket.png"));
	private static final int VANILLA_RED = 0xC8302A;

	public static final class State extends BlockEntityRenderState {
		final int[] colours = new int[ShowLauncherBlockEntity.TUBES];
		Direction facing = Direction.NORTH;
	}

	public ShowLauncherRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(ShowLauncherBlockEntity launcher, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(launcher, state, crumbling);
		for (int tube = 0; tube < ShowLauncherBlockEntity.TUBES; tube++) {
			ItemStack rocket = launcher.tube(tube);
			state.colours[tube] = rocket.isEmpty() ? -1 : rocket.getItem() instanceof SpookyFireworkItem spooky ? spooky.shape().colour() : VANILLA_RED;
		}
		state.facing = launcher.getBlockState().hasProperty(ShowLauncherBlock.FACING) ? launcher.getBlockState().getValue(ShowLauncherBlock.FACING)
				: Direction.NORTH;
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		int light = state.lightCoords;
		pose.pushPose();
		pose.translate(0.5F, 0.0F, 0.5F);
		pose.rotateDegrees(Axis.YP, -RockingChairRenderer.yRotation(state.facing));
		pose.translate(-0.5F, 0.0F, -0.5F);
		collector.submitCustomGeometry(pose, PAPER, (matrix, buffer) -> {
			for (int tube = 0; tube < ShowLauncherBlockEntity.TUBES; tube++) {
				int colour = state.colours[tube];
				if (colour < 0) {
					continue;
				}
				// Tube centres are 3, 8 and 13 pixels across; the front row is at the north.
				float x = 3.0F + 5.0F * (tube % 3);
				float z = 3.0F + 5.0F * (tube / 3);
				TintedBoxes.box(buffer, matrix, x - 1.25F, 12.0F, z - 1.25F, x + 1.25F, 15.0F, z + 1.25F, 0xFF000000 | colour, light);
				TintedBoxes.box(buffer, matrix, x - 0.6F, 15.0F, z - 0.6F, x + 0.6F, 16.0F, z + 0.6F, 0xFFF4EEDC, light);
			}
		});
		pose.popPose();
	}
}
