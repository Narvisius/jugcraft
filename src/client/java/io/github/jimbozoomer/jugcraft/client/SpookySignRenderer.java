package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.agriculture.SpookySignBlock;
import io.github.jimbozoomer.jugcraft.agriculture.SpookySignBlockEntity;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Paints a Spooky Sign's words on the front of its board in blood red: its own words if it has any, else its painted
 * warning. Long words are wrapped and the text is made as large as fits the board, up to {@value #MAX_SCALE} blocks a
 * font pixel; lines past the last that fits are left off.
 */
public class SpookySignRenderer implements BlockEntityRenderer<SpookySignBlockEntity, SpookySignRenderer.State> {
	private static final int PAINT = 0xFFD41C1C;
	/** The board's face: its middle's height, its width and height (pixels) and how far in front of the block's middle. */
	private static final float MIDDLE_Y = 10.75F;
	private static final float WIDTH = 13.5F;
	private static final float HEIGHT = 8.0F;
	private static final float FRONT = 0.5F;
	private static final float OUT = 0.005F;
	private static final float MAX_SCALE = 1.0F / 32;
	/** The smallest the letters get: the width to wrap at, in font pixels, is the board's width at this size. */
	private static final float MIN_SCALE = 1.0F / 150;
	private static final int MAX_LINES = 4;

	private final Font font;

	public static final class State extends BlockEntityRenderState {
		List<FormattedCharSequence> lines = List.of();
		Direction facing = Direction.NORTH;
		float scale = MAX_SCALE;
	}

	public SpookySignRenderer(BlockEntityRendererProvider.Context context) {
		font = context.font();
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(SpookySignBlockEntity sign, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(sign, state, crumbling);
		state.lines = List.of();
		BlockState block = sign.getBlockState();
		if (!(block.getBlock() instanceof SpookySignBlock)) {
			return;
		}
		state.facing = block.getValue(SpookySignBlock.FACING);
		FormattedText text = sign.text().isEmpty() ? Component.translatable(block.getValue(SpookySignBlock.WORDS).key()) : FormattedText.of(sign.text());
		List<FormattedCharSequence> lines = font.split(text, Math.round(WIDTH / 16.0F / MIN_SCALE));
		if (lines.size() > MAX_LINES) {
			lines = List.copyOf(lines.subList(0, MAX_LINES));
		}
		int widest = 1;
		for (FormattedCharSequence line : lines) {
			widest = Math.max(widest, font.width(line));
		}
		float byWidth = WIDTH / 16.0F / widest;
		float byHeight = HEIGHT / 16.0F / Math.max(1, lines.size() * font.lineHeight);
		state.scale = Math.min(MAX_SCALE, Math.min(byWidth, byHeight));
		state.lines = lines;
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		if (state.lines.isEmpty()) {
			return;
		}
		pose.pushPose();
		pose.translate(0.5F, MIDDLE_Y / 16.0F, 0.5F);
		// Turn so that -z is out of the board's face, as for a sign facing north, then step out to it.
		pose.rotateDegrees(Axis.YP, -RockingChairRenderer.yRotation(state.facing) + 180.0F);
		pose.translate(0.0F, 0.0F, FRONT / 16.0F + OUT);
		pose.scale(state.scale, -state.scale, state.scale);
		float top = -state.lines.size() * font.lineHeight / 2.0F + 1.0F;
		for (int i = 0; i < state.lines.size(); i++) {
			FormattedCharSequence line = state.lines.get(i);
			collector.submitText(pose, -font.width(line) / 2.0F, top + i * font.lineHeight, line, false, Font.DisplayMode.POLYGON_OFFSET,
					state.lightCoords, PAINT, 0, 0);
		}
		pose.popPose();
	}
}
