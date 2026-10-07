package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.agriculture.Epitaph;
import io.github.jimbozoomer.jugcraft.agriculture.HeadstoneBlock;
import io.github.jimbozoomer.jugcraft.agriculture.HeadstoneBlockEntity;
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
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws a memorial's inscriptions cut into its stone, each where its {@link HeadstoneBlock.Layout} says: on a front, on
 * a side (a mausoleum's crypt fronts), or on the top of a table tomb or ledger stone, read from its foot. Each line is
 * centred and drawn as large as fits the space, up to the place's largest letters; if the lines together are too tall,
 * all shrink alike. The letters take the stone's ink and fade into it as it weathers ({@code tools/graveyard.py}:
 * ink_fade), so a neglected stone grows hard to read until it is scrubbed.
 */
public class HeadstoneRenderer implements BlockEntityRenderer<HeadstoneBlockEntity, HeadstoneRenderer.State> {
	/** How much each stage of weathering fades the letters towards the stone. */
	public static final float[] INK_FADE = {0.0F, 0.18F, 0.36F, 0.52F};
	/** How far in front of the face the letters sit, so they never sink into the stone. */
	private static final float OUT = 0.005F;
	/** The gap between lines, as a share of a line. */
	private static final float LEADING = 0.25F;

	private final Font font;

	/** One inscription ready to draw: its place, its lines and the scale of each. */
	record Cut(HeadstoneBlock.Text text, List<FormattedCharSequence> lines, List<Float> scales) {
	}

	public static final class State extends BlockEntityRenderState {
		List<Cut> cuts = List.of();
		Direction facing = Direction.NORTH;
		int ink;
	}

	public HeadstoneRenderer(BlockEntityRendererProvider.Context context) {
		font = context.font();
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	/** {@code ink} faded towards {@code stone} by {@code amount} (0 to 1), channel by channel. */
	public static int fade(int ink, int stone, float amount) {
		int out = 0xFF000000;
		for (int shift = 0; shift <= 16; shift += 8) {
			int a = (ink >> shift) & 0xFF;
			int b = (stone >> shift) & 0xFF;
			out |= Math.round(a + (b - a) * amount) << shift;
		}
		return out;
	}

	@Override
	public void extractRenderState(HeadstoneBlockEntity stone, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(stone, state, crumbling);
		state.cuts = List.of();
		BlockState block = stone.getBlockState();
		if (!(block.getBlock() instanceof HeadstoneBlock headstone)) {
			return;
		}
		HeadstoneBlock.Layout layout = headstone.layout();
		List<HeadstoneBlock.Text> texts = layout.texts();
		List<Cut> cuts = new ArrayList<>();
		for (int slot = 0; slot < texts.size(); slot++) {
			Epitaph epitaph = stone.inscription(slot);
			if (!epitaph.isBlank()) {
				cuts.add(cut(texts.get(slot), epitaph));
			}
		}
		if (cuts.isEmpty()) {
			return;
		}
		state.cuts = cuts;
		state.facing = block.getValue(HeadstoneBlock.FACING);
		state.ink = fade(layout.stone().ink, layout.stone().fadeTo, INK_FADE[HeadstoneBlock.stage(block)]);
	}

	/** Each line of {@code epitaph} as large as it fits {@code text}'s place, all shrunk alike if they are too tall together. */
	private Cut cut(HeadstoneBlock.Text text, Epitaph epitaph) {
		List<FormattedCharSequence> lines = new ArrayList<>();
		List<Float> scales = new ArrayList<>();
		float width = text.width() / 16.0F;
		float total = 0.0F;
		for (String line : epitaph.lines()) {
			List<FormattedCharSequence> split = line.isEmpty() ? List.of() : font.split(FormattedText.of(line), Integer.MAX_VALUE);
			FormattedCharSequence sequence = split.isEmpty() ? FormattedCharSequence.EMPTY : split.get(0);
			int wide = Math.max(1, font.width(sequence));
			float scale = Math.min(text.maxScale(), width / wide);
			lines.add(sequence);
			scales.add(scale);
			total += font.lineHeight * scale * (1.0F + LEADING);
		}
		float room = text.height() / 16.0F;
		if (total > room) {
			float shrink = room / total;
			scales.replaceAll(scale -> scale * shrink);
		}
		return new Cut(text, lines, scales);
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		for (Cut cut : state.cuts) {
			submit(state, cut, pose, collector);
		}
	}

	private void submit(State state, Cut cut, PoseStack pose, SubmitNodeCollector collector) {
		HeadstoneBlock.Text text = cut.text();
		pose.pushPose();
		pose.translate(0.5F, 0.0F, 0.5F);
		// Turn so that +z points out of a north-facing memorial's front (model -z), +x to its left (model -x).
		pose.rotateDegrees(Axis.YP, -state.facing.toYRot());
		float x = -(text.x() - 8.0F) / 16.0F;
		float y = text.y() / 16.0F;
		float z = -(text.z() - 8.0F) / 16.0F;
		switch (text.face()) {
			case TOP -> {
				pose.translate(x, y + OUT, z);
				// Lie flat, letters up and the lines reading away from someone standing at the foot.
				pose.rotateDegrees(Axis.XP, -90.0F);
			}
			case BACK -> {
				pose.translate(x, y, z - OUT);
				pose.rotateDegrees(Axis.YP, 180.0F);
			}
			case EAST -> {
				// Model +x is -x here: the letters face that way and read towards the front.
				pose.translate(x - OUT, y, z);
				pose.rotateDegrees(Axis.YP, -90.0F);
			}
			case WEST -> {
				pose.translate(x + OUT, y, z);
				pose.rotateDegrees(Axis.YP, 90.0F);
			}
			default -> pose.translate(x, y, z + OUT);
		}
		float total = 0.0F;
		for (float scale : cut.scales()) {
			total += font.lineHeight * scale * (1.0F + LEADING);
		}
		float top = total / 2.0F;
		for (int i = 0; i < cut.lines().size(); i++) {
			float scale = cut.scales().get(i);
			FormattedCharSequence line = cut.lines().get(i);
			float step = font.lineHeight * scale * (1.0F + LEADING);
			pose.pushPose();
			pose.translate(0.0F, top - font.lineHeight * scale * LEADING / 2.0F, 0.0F);
			pose.scale(scale, -scale, scale);
			collector.submitText(pose, -font.width(line) / 2.0F, 0.0F, line, false, Font.DisplayMode.POLYGON_OFFSET, state.lightCoords,
					state.ink, 0, 0);
			pose.popPose();
			top -= step;
		}
		pose.popPose();
	}
}
