package io.github.jimbozoomer.jugcraft.client.warehouse;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.warehouse.WarehousePiece;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.LightCoordsUtil;

/**
 * A live screen's page for the warehouse's renderers: lines of writing (lit, on the dark glass of the model's screen),
 * in font pixels from the page's top left; and the frame helpers that put a renderer into a piece's own pixels.
 */
final class Page {
	static final int WHITE = 0xFFE8EEF4, GREY = 0xFF8A96A6, CYAN = 0xFF5ADCF0, GREEN = 0xFF64E68C, AMBER = 0xFFFFB83C, RED = 0xFFFF5046,
			DIM = 0xFF465464, TITLE = 0xFF1E3C64;
	private final Font font;
	final List<Line> lines = new ArrayList<>();

	record Line(float x, float y, float scale, FormattedCharSequence text, int colour) {
	}

	Page(Font font) {
		this.font = font;
	}

	Page add(float x, float y, float scale, String text, int colour) {
		lines.add(new Line(x, y, scale, io.github.jimbozoomer.jugcraft.client.Texts.of(text), colour));
		return this;
	}

	Page right(float x, float y, float scale, String text, int colour) {
		lines.add(new Line(x - font.width(text) * scale, y, scale, io.github.jimbozoomer.jugcraft.client.Texts.of(text), colour));
		return this;
	}

	/** A bar of block glyphs: on cells lit, the rest dim. */
	Page bar(float x, float y, float scale, int cells, double fill, int colour) {
		int n = (int) Math.round(Math.max(0, Math.min(1, fill)) * cells);
		String on = "█".repeat(n), off = "█".repeat(cells - n);
		add(x, y, scale, on, colour);
		if (!off.isEmpty()) {
			add(x + font.width(on) * scale, y, scale, off, 0xFF1C2833);
		}
		return this;
	}

	/** The page's title line (white, its right-hand word cyan). */
	Page title(float width, String text, String right) {
		add(0, 0, 1, text, WHITE);
		if (!right.isEmpty()) {
			right(width, 0, 1, right, CYAN);
		}
		return this;
	}

	int width(String s) {
		return font.width(s);
	}

	/**
	 * Draws the page on a screen face facing +z in the current frame (pixels): its top left at (x, y), z the face, the
	 * page's font pixel scale.
	 */
	void draw(PoseStack pose, SubmitNodeCollector collector, float x, float y, float z, float scale) {
		pose.pushPose();
		pose.translate(x, y, z + 0.012F);
		pose.scale(scale, -scale, scale);
		for (Line line : lines) {
			pose.pushPose();
			pose.translate(line.x(), line.y(), 0);
			pose.scale(line.scale(), line.scale(), 1);
			collector.submitText(pose, 0, 0, line.text(), false, Font.DisplayMode.POLYGON_OFFSET, LightCoordsUtil.FULL_BRIGHT, line.colour(), 0, 0);
			pose.popPose();
		}
		pose.popPose();
	}

	/** Into a block's own model pixels, turned as its blockstate turns its model (facing: the way its user looks). */
	static void blockFrame(PoseStack pose, Direction facing) {
		float y = switch (facing) {
			case EAST -> 90;
			case SOUTH -> 180;
			case WEST -> 270;
			default -> 0;
		};
		pose.translate(0.5F, 0, 0.5F);
		pose.rotateDegrees(Axis.YP, -y);
		pose.translate(-0.5F, 0, -0.5F);
		pose.scale(1 / 16F, 1 / 16F, 1 / 16F);
	}

	/** Into a piece's own pixels, from its origin block (the front row's middle column). */
	static void pieceFrame(PoseStack pose, Direction facing, WarehousePiece piece) {
		blockFrame(pose, facing);
		pose.translate(-16F * (piece.width / 2), 0, -16F * (piece.depth - 1));
	}
}
