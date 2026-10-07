package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.agriculture.BowlingScore;
import io.github.jimbozoomer.jugcraft.agriculture.BowlingScoreboardBlock;
import io.github.jimbozoomer.jugcraft.agriculture.BowlingScoreboardBlockEntity;
import java.util.ArrayList;
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
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Chalks the game on a Bowling Scoreboard's slate: the frame (or GAME OVER), the marks of the last
 * {@value #SHOWN_FRAMES} frames (X a strike, / a spare, - no pins) and the score; before the first roll, an invitation
 * to roll. The lines are made as large as fits the slate, up to {@value #MAX_SCALE} blocks a font pixel.
 */
public class BowlingScoreboardRenderer implements BlockEntityRenderer<BowlingScoreboardBlockEntity, BowlingScoreboardRenderer.State> {
	private static final int CHALK = 0xFFF2F0E6;
	private static final int SHOWN_FRAMES = 3;
	/** The slate's face: its middle's height, its width and height (pixels) and how far in front of the block's middle. */
	private static final float MIDDLE_Y = 10.5F;
	private static final float WIDTH = 11.0F;
	private static final float HEIGHT = 8.0F;
	private static final float FRONT = 0.5F;
	private static final float OUT = 0.005F;
	private static final float MAX_SCALE = 1.0F / 40;

	private final Font font;

	public static final class State extends BlockEntityRenderState {
		List<FormattedCharSequence> lines = List.of();
		Direction facing = Direction.NORTH;
		float scale = MAX_SCALE;
	}

	public BowlingScoreboardRenderer(BlockEntityRendererProvider.Context context) {
		font = context.font();
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(BowlingScoreboardBlockEntity board, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(board, state, crumbling);
		state.lines = List.of();
		BlockState block = board.getBlockState();
		if (!(block.getBlock() instanceof BowlingScoreboardBlock)) {
			return;
		}
		state.facing = block.getValue(BowlingScoreboardBlock.FACING);
		List<Integer> rolls = board.rolls();
		int pins = board.pins();
		List<Component> text = new ArrayList<>();
		if (rolls.isEmpty() || pins <= 0) {
			text.add(Component.translatable("message.jugcraft.bowling_scoreboard.ready"));
		} else {
			int[] next = BowlingScore.next(rolls, pins);
			text.add(next[0] >= BowlingScore.FRAMES ? Component.translatable("message.jugcraft.bowling_scoreboard.game_over")
					: Component.translatable("message.jugcraft.bowling_scoreboard.frame", next[0] + 1));
			List<String> marks = BowlingScore.marks(rolls, pins);
			text.add(Component.literal(String.join(" ", marks.subList(Math.max(0, marks.size() - SHOWN_FRAMES), marks.size()))));
			text.add(Component.translatable("message.jugcraft.bowling_scoreboard.score", BowlingScore.score(rolls, pins)));
		}
		List<FormattedCharSequence> lines = new ArrayList<>();
		int widest = 1;
		for (Component line : text) {
			for (FormattedCharSequence sequence : font.split(line, Integer.MAX_VALUE / 2)) {
				lines.add(sequence);
				widest = Math.max(widest, font.width(sequence));
			}
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
		// Turn so that -z is out of the slate's face, as for a board facing north, then step out to it.
		pose.rotateDegrees(Axis.YP, -RockingChairRenderer.yRotation(state.facing) + 180.0F);
		pose.translate(0.0F, 0.0F, FRONT / 16.0F + OUT);
		pose.scale(state.scale, -state.scale, state.scale);
		float top = -state.lines.size() * font.lineHeight / 2.0F + 1.0F;
		for (int i = 0; i < state.lines.size(); i++) {
			FormattedCharSequence line = state.lines.get(i);
			collector.submitText(pose, -font.width(line) / 2.0F, top + i * font.lineHeight, line, false, Font.DisplayMode.POLYGON_OFFSET,
					state.lightCoords, CHALK, 0, 0);
		}
		pose.popPose();
	}
}
