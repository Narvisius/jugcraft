package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.agriculture.GravestoneBlock;
import io.github.jimbozoomer.jugcraft.agriculture.GravestoneBlockEntity;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws a gravestone's engraving on its face, centred and word-wrapped like sign text, in the place and size
 * its {@link GravestoneBlock.Style} gives; lines past the last that fits are left off.
 */
public class GravestoneRenderer implements BlockEntityRenderer<GravestoneBlockEntity, GravestoneRenderer.State> {
	/** Near black, as if cut deep into the pale stone. */
	private static final int INK = 0xFF1C1C20;
	/** How far in front of the face the letters sit, so they never sink into the stone. */
	private static final float OUT = 0.005F;

	private final Font font;

	public static final class State extends BlockEntityRenderState {
		List<FormattedCharSequence> lines = List.of();
		Direction facing = Direction.NORTH;
		GravestoneBlock.@Nullable Style style;
	}

	public GravestoneRenderer(BlockEntityRendererProvider.Context context) {
		font = context.font();
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(GravestoneBlockEntity stone, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(stone, state, crumbling);
		state.style = null;
		BlockState block = stone.getBlockState();
		if (stone.text().isEmpty() || !(block.getBlock() instanceof GravestoneBlock gravestone)) {
			return;
		}
		GravestoneBlock.Style style = gravestone.style();
		state.style = style;
		state.facing = block.getValue(GravestoneBlock.FACING);
		int width = Math.round(style.textWidth / 16.0F / style.fontScale);
		List<FormattedCharSequence> lines = font.split(FormattedText.of(stone.text()), width);
		state.lines = lines.size() > style.lines ? List.copyOf(lines.subList(0, style.lines)) : lines;
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		GravestoneBlock.Style style = state.style;
		if (style == null || state.lines.isEmpty()) {
			return;
		}
		pose.pushPose();
		pose.translate(0.5F, style.textY / 16.0F, 0.5F);
		// Turn so that +z points out of the engraved face, then step out to it.
		pose.rotateDegrees(Axis.YP, -state.facing.toYRot());
		pose.translate(0.0F, 0.0F, style.front / 16.0F + OUT);
		pose.scale(style.fontScale, -style.fontScale, style.fontScale);
		float top = -state.lines.size() * font.lineHeight / 2.0F;
		for (int i = 0; i < state.lines.size(); i++) {
			FormattedCharSequence line = state.lines.get(i);
			collector.submitText(pose, -font.width(line) / 2.0F, top + i * font.lineHeight, line, false, Font.DisplayMode.POLYGON_OFFSET,
					state.lightCoords, INK, 0, 0);
		}
		pose.popPose();
	}
}
