package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.DecorationBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.SpecimenJarBlock;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws what floats in a Specimen Jar: the specimen, bobbing ({@link SpecimenJarBlock#bob}) and turning slowly in the
 * middle of the fluid, lit by the fluid's glow, and three bubbles rising past it. The bigger specimen jars draw the same
 * ({@link #draw}), scaled up with them.
 */
public class SpecimenJarRenderer implements BlockEntityRenderer<DecorationBlockEntity, SpecimenJarRenderer.State> {
	private static final RenderType BUBBLE = RenderTypes.entityCutout(Jugcraft.id("textures/entity/specimen_jar_bubble.png"));
	private static final int GLOW = 0xF000C0;
	/** The middle of the fluid, in pixels, about which the specimen is modelled. */
	private static final float MIDDLE = 6.0F;
	private static final float BUBBLE_SIZE = 0.5F / 16;

	public static final class State extends BlockEntityRenderState {
		SpecimenJarBlock.Specimen specimen = SpecimenJarBlock.Specimen.EYE;
		float bob;
		float turn;
		float time;
	}

	public SpecimenJarRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(DecorationBlockEntity jar, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(jar, state, crumbling);
		BlockState block = jar.getBlockState();
		if (!(block.getBlock() instanceof SpecimenJarBlock) || jar.getLevel() == null) {
			return;
		}
		state.specimen = block.getValue(SpecimenJarBlock.SPECIMEN);
		state.time = jar.getLevel().getGameTime() % 24000 + partialTick;
		state.bob = SpecimenJarBlock.bob(jar.getBlockPos(), state.time);
		state.turn = turn(jar.getBlockPos(), state.time);
	}

	/** Which way the specimen in the jar at {@code pos} has turned at {@code time} (ticks with the partial tick), in degrees. */
	static float turn(BlockPos pos, float time) {
		return (time * 0.8F + (pos.hashCode() & 0xFF)) % 360.0F;
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		draw(pose, collector, state.specimen, state.bob, state.turn, state.time);
	}

	/**
	 * The specimen and its bubbles, in a jar's block ({@code pose} at its corner): risen {@code bob} pixels and turned
	 * {@code turn} degrees, the bubbles where they are at {@code time} (ticks).
	 */
	static void draw(PoseStack pose, SubmitNodeCollector collector, SpecimenJarBlock.Specimen kind, float bob, float turn, float time) {
		QuadModel specimen = DecorQuads.get("specimen_" + kind.getSerializedName());
		pose.pushPose();
		if (specimen != null) {
			pose.pushPose();
			pose.translate(0.5F, (MIDDLE + bob) / 16, 0.5F);
			pose.rotateDegrees(Axis.YP, turn);
			pose.translate(-0.5F, -MIDDLE / 16, -0.5F);
			specimen.submit(pose, collector, GLOW);
			pose.popPose();
		}
		collector.submitCustomGeometry(pose, BUBBLE, (matrix, buffer) -> {
			for (int i = 0; i < 3; i++) {
				float rise = ((time / 40.0F + i / 3.0F) % 1.0F);
				float x = (6.0F + i * 2.0F) / 16;
				float z = (7.0F + (i % 2) * 2.5F) / 16;
				float y = (2.0F + rise * 8.5F) / 16;
				DecorDraw.box(buffer, matrix, x, y, z, x + BUBBLE_SIZE, y + BUBBLE_SIZE, z + BUBBLE_SIZE, 0, 0, 1, 1, 0xFFFFFFFF, GLOW, DecorDraw.ALL);
			}
		});
		pose.popPose();
	}
}
