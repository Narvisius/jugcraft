package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jimbozoomer.jugcraft.nuclear.DeskScreenBlockEntity;
import io.github.jimbozoomer.jugcraft.nuclear.RowFurnitureBlock;
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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * An office computer's screen (the administration building part 1; nuclear/DeskScreenBlockEntity.java): the plant
 * intranet's home page written on the desk's monitor (or laptop): its title and the time, the unit's state in its
 * colour, reactor power, the generator's output, the alarms, the next outage, the safety motto. Where the screen is on
 * each desk (model pixels, the desk facing north: x0, y0, x1, y1, its face's z) follows tools/plant_office2.py.
 */
public class DeskScreenRenderer implements BlockEntityRenderer<DeskScreenBlockEntity, DeskScreenRenderer.State> {
	/** The page is laid out PAGE font pixels wide whatever the screen's size. */
	static final float PAGE = 160F;
	private final Font font;

	record Line(float x, float y, float scale, FormattedCharSequence text, int colour) {
	}

	public static class State extends BlockEntityRenderState {
		Direction facing = Direction.NORTH;
		float[] rect;
		final List<Line> lines = new ArrayList<>();
	}

	public DeskScreenRenderer(BlockEntityRendererProvider.Context context) {
		this.font = context.font();
	}

	/** The screen on a desk in this state, or null (no computer on it). */
	static float[] rect(BlockState state) {
		String id = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
		int v = state.getBlock() instanceof RowFurnitureBlock row ? row.variantOf(state) : 0;
		return switch (id) {
			case "office_desk" -> new float[] {2.8F, 15.3F, 13.2F, 21.7F, 4.1F};
			case "office_desk_long" -> v == 0 ? new float[] {2.8F, 15.3F, 13.2F, 21.7F, 4.1F} : v == 4 ? new float[] {4F, 12.9F, 12F, 18F, 6F} : null;
			case "glass_desk" -> v == 0 ? new float[] {2.8F, 14.8F, 13.2F, 21.2F, 4.1F} : null;
			case "executive_desk" -> v == 0 ? new float[] {0.4F, 15F, 7.7F, 21F, 4.0F} : null;
			case "reception_desk", "doc_issue_counter" -> v == 0 ? new float[] {3.5F, 13F, 12.5F, 19F, 6.8F} : null;
			default -> null;
		};
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(DeskScreenBlockEntity screen, State state, float partialTick, Vec3 cameraPos,
			ModelFeatureRenderer.CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(screen, state, crumbling);
		BlockState block = screen.getBlockState();
		state.facing = block.hasProperty(HorizontalDirectionalBlock.FACING) ? block.getValue(HorizontalDirectionalBlock.FACING) : Direction.NORTH;
		state.rect = rect(block);
		state.lines.clear();
		if (state.rect == null) {
			return;
		}
		long day = screen.getLevel() == null ? 0 : screen.getLevel().getOverworldClockTime();
		left(state, 4, 2.5F, 0.85F, "PLANT INTRANET", 0xFFECF0F6);
		right(state, 4, 2.5F, 0.8F, io.github.jimbozoomer.jugcraft.nuclear.TscRoom.clock(day), 0xFF78C6EE);
		String unit;
		int colour;
		switch (screen.status) {
			case 1 -> {
				unit = screen.power >= 5 ? "AT POWER" : "STARTUP";
				colour = 0xFF6EE68C;
			}
			case 2 -> {
				unit = "SHUT DOWN";
				colour = 0xFFFFC440;
			}
			case 3 -> {
				unit = "TRIPPED";
				colour = 0xFFFF6050;
			}
			default -> {
				unit = "NO DATA";
				colour = 0xFFA8B0BC;
			}
		}
		boolean on = screen.status != 0;
		row(state, 0, "UNIT 1", unit, colour);
		row(state, 1, "REACTOR POWER", on ? screen.power + " %" : "----", 0xFFECF0F6);
		row(state, 2, "GENERATION", !on ? "----" : screen.mwe > 0 ? screen.mwe + " MWE" : "OFF LINE", screen.mwe > 0 ? 0xFFECF0F6 : 0xFFFFC440);
		row(state, 3, "ALARMS", !on ? "----" : screen.alarms == 0 ? "NONE" : screen.alarms + " ACTIVE", screen.alarms == 0 ? 0xFF6EE68C : 0xFFFF6050);
		row(state, 4, "NEXT OUTAGE", "RFO-24", 0xFFECF0F6);
		row(state, 5, "SAFETY FIRST", "", 0xFFECF0F6);
	}

	private void row(State state, int i, String label, String value, int colour) {
		float y = 17 + i * 13F;
		left(state, 4, y, 0.78F, label, 0xFFA8B0BC);
		right(state, 4, y, 0.78F, value, colour);
	}

	private void left(State state, float x, float y, float scale, String text, int colour) {
		state.lines.add(new Line(x, y, scale, Texts.of(text), colour));
	}

	private void right(State state, float inset, float y, float scale, String text, int colour) {
		float w = font.width(text) * scale;
		state.lines.add(new Line(PAGE - inset - w, y, scale, Texts.of(text), colour));
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		if (state.rect == null || state.lines.isEmpty()) {
			return;
		}
		float[] r = state.rect;
		pose.pushPose();
		// The desk faces the way its user looks; the screen faces back toward the user (the model's +z side).
		Direction out = state.facing.getOpposite();
		pose.translate(0.5, 0, 0.5);
		pose.mulPose(new org.joml.Matrix4f().rotationY((float) Math.toRadians(-out.toYRot())));
		// Local frame: x to the user's right, y up, z toward the user; model pixel (x, y, z) is at (x/16 - 0.5, y/16, z/16 - 0.5).
		float w = (r[2] - r[0]) / 16F;
		float s = w / PAGE;
		pose.translate(r[0] / 16F - 0.5F, r[3] / 16F - 0.15F / 16F, r[4] / 16F - 0.5F + 0.004F);
		pose.scale(s, -s, s);
		float maxY = (r[3] - r[1] - 0.3F) / 16F / s;
		for (Line line : state.lines) {
			if (line.y() + 8 * line.scale() > maxY) {
				continue;
			}
			pose.pushPose();
			pose.translate(line.x(), line.y(), 0);
			pose.scale(line.scale(), line.scale(), 1);
			collector.submitText(pose, 0, 0, line.text(), false, Font.DisplayMode.POLYGON_OFFSET, LightCoordsUtil.FULL_BRIGHT, line.colour(), 0, 0);
			pose.popPose();
		}
		pose.popPose();
	}
}
