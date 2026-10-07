package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.client.DroneDepotRenderer.Geometry;
import io.github.jimbozoomer.jugcraft.nuclear.StorageTankOutletBlock;
import io.github.jimbozoomer.jugcraft.nuclear.StorageTankPortBlock;
import io.github.jimbozoomer.jugcraft.nuclear.StorageTankPortBlockEntity;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A storage tank nozzle's moving parts and, for a formed tank's outlet, what is painted and hung on the shell.
 * <ul>
 * <li>The gate valve's rising stem and red handwheel: opening, the wheel turns two turns and the stem rises out of the
 * yoke; shutting, the other way.</li>
 * <li>The float gauge up the shell over the outlet: a white board marked in tenths, a red pointer riding up and down
 * with the level between two guide wires, its cable up to a pulley on the roof's edge and over into the tank.</li>
 * <li>The stencil on the widest clear side of the shell: the tank's name and tag for a plant's tank (with the fire
 * diamond and a no-smoking line on a diesel tank), or what it holds for any other.</li>
 * </ul>
 */
public class StorageTankRenderer implements BlockEntityRenderer<StorageTankPortBlockEntity, StorageTankRenderer.State> {
	public static final Identifier TEXTURE = Jugcraft.id("textures/entity/storage_tank.png");
	private static final RenderType RENDER_TYPE = RenderTypes.entityCutout(TEXTURE);
	private static final float PX = 1 / 256F;
	static final float[] RED_IRON = region(0, 0), CHROME = region(1, 0), GUNMETAL = region(2, 0), YELLOW = region(3, 0);
	static final float[] GAUGE_BOARD = region(0, 1), POINTER = region(1, 1), BLACK = region(2, 1), GALVANISED = region(3, 1);
	static final float[] WHITE = region(0, 2), NFPA_BLUE = region(1, 2), NFPA_RED = region(2, 2), NFPA_YELLOW = region(3, 2);
	static final float[] RIM = region(0, 3), SPRING = region(1, 3), HAZARD_TAG = region(2, 3), NFPA_WHITE = region(3, 3);

	private static float[] region(int col, int row) {
		return new float[] {(col * 64 + 0.5F) * PX, (row * 64 + 0.5F) * PX, (col * 64 + 63.5F) * PX, (row * 64 + 63.5F) * PX};
	}

	private final Font font;

	public static final class State extends BlockEntityRenderState {
		Direction facing = Direction.NORTH;
		boolean outlet;
		/** 0 shut .. 1 open (the stem's rise), and the wheel's turn in degrees. */
		float lift, spin;
		int gauge;
		float fill;
		int[] gaugeLight = new int[0];
		@Nullable Direction stencilFace;
		Vec3 stencilCenter = Vec3.ZERO;
		float stencilWidth, stencilHeight;
		final List<FormattedCharSequence> lines = new ArrayList<>();
		final List<Integer> widths = new ArrayList<>(), colours = new ArrayList<>();
		final List<Float> sizes = new ArrayList<>();
		boolean diamond;
		int stencilLight;
	}

	public StorageTankRenderer(BlockEntityRendererProvider.Context context) {
		this.font = context.font();
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(StorageTankPortBlockEntity port, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(port, state, crumbling);
		BlockState block = port.getBlockState();
		Level level = port.getLevel();
		state.facing = block.getValue(StorageTankPortBlock.FACING);
		state.outlet = block.getBlock() instanceof StorageTankOutletBlock;
		long now = level == null ? 0 : level.getGameTime();
		boolean open = block.getValue(StorageTankPortBlock.OPEN);
		if (port.animStart == Long.MIN_VALUE) {
			port.animStart = now - 100;
			port.animOpen = open;
		}
		if (open != port.animOpen) {
			port.animOpen = open;
			port.animStart = now;
		}
		float t = Math.min(1, Math.max(0, (now + partialTick - port.animStart) / 24F));
		t = t * t * (3 - 2 * t);
		state.lift = open ? t : 1 - t;
		state.spin = state.lift * 720;
		state.gauge = 0;
		state.stencilFace = null;
		state.lines.clear();
		state.widths.clear();
		state.colours.clear();
		state.sizes.clear();
		state.diamond = false;
		if (!state.outlet || !port.formed() || level == null) {
			return;
		}
		BlockPos pos = port.getBlockPos();
		state.gauge = port.gaugeHeight();
		long capacity = port.fluid.capacity();
		state.fill = capacity <= 0 ? 0 : Math.min(1, port.fluid.amount / (float) capacity);
		state.gaugeLight = new int[state.gauge + 2];
		for (int k = 0; k < state.gaugeLight.length; k++) {
			state.gaugeLight[k] = LightCoordsUtil.getLightCoords(level, pos.above(k));
		}
		Direction face = port.stencilFace();
		if (face != null) {
			state.stencilFace = face;
			state.stencilCenter = port.stencilCenter();
			state.stencilWidth = (float) port.stencilWidth();
			state.stencilHeight = (float) port.stencilHeight();
			Vec3 world = Vec3.atLowerCornerOf(pos).add(state.stencilCenter).add(face.getStepX() * 0.5, 0, face.getStepZ() * 0.5);
			state.stencilLight = LightCoordsUtil.getLightCoords(level, BlockPos.containing(world));
			StorageTankOutletBlock.Service service = block.getValue(StorageTankOutletBlock.SERVICE);
			if (service != StorageTankOutletBlock.Service.NONE) {
				String key = "tank.jugcraft." + service.getSerializedName();
				line(state, Component.translatable(key + ".name"), 0.075F, 0xFF1C1C1E);
				line(state, Component.translatable(key + ".tag"), 0.06F, 0xFF1C1C1E);
				if (service == StorageTankOutletBlock.Service.DIESEL_FUEL) {
					line(state, Component.translatable(key + ".warning"), 0.04F, 0xFFB01818);
					line(state, Component.translatable(key + ".warning2"), 0.04F, 0xFFB01818);
					state.diamond = true;
				}
			} else if (!port.fluid.isResourceBlank() && port.fluid.amount > 0) {
				line(state, Component.literal(FluidVariantAttributes.getName(port.fluid.variant).getString().toUpperCase(java.util.Locale.ROOT)),
						0.075F, 0xFF1C1C1E);
			}
		}
	}

	/** One painted line, wrapped at spaces into as many lines as it takes to fit the clear width at its size. */
	private void line(State state, Component text, float size, int colour) {
		float width = Math.max(1.0F, state.stencilWidth - 0.8F);
		String[] words = text.getString().split(" ");
		StringBuilder current = new StringBuilder();
		for (String word : words) {
			String tried = current.length() == 0 ? word : current + " " + word;
			if (current.length() > 0 && font.width(tried) * size > width) {
				add(state, current.toString(), size, colour);
				current = new StringBuilder(word);
			} else {
				current = new StringBuilder(tried);
			}
		}
		if (current.length() > 0) {
			add(state, current.toString(), size, colour);
		}
	}

	private void add(State state, String text, float size, int colour) {
		Component c = Component.literal(text);
		state.lines.add(c.getVisualOrderText());
		state.widths.add(font.width(c));
		state.sizes.add(size);
		state.colours.add(colour);
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		int light = state.lightCoords;
		pose.pushPose();
		pose.translate(0.5F, 0.0F, 0.5F);
		pose.rotateDegrees(Axis.YP, -yRotation(state.facing));
		pose.translate(-0.5F, 0.0F, -0.5F);
		pose.scale(1 / 16F, 1 / 16F, 1 / 16F);
		float vc = state.outlet ? 8.65F : 5.7F;
		collector.submitCustomGeometry(pose, RENDER_TYPE, (p, consumer) -> {
			Geometry g = new Geometry(p, consumer);
			g.light = light;
			valve(g, vc, state.lift, state.spin);
			if (state.gauge > 0) {
				gauge(g, state);
			}
		});
		pose.popPose();
		if (state.stencilFace != null && !state.lines.isEmpty()) {
			stencil(state, pose, collector);
		}
	}

	/** The stem (rising 2.2 px as it opens) and the handwheel, turned about the stem. */
	private static void valve(Geometry g, float vc, float lift, float spin) {
		float rise = lift * 2.2F;
		LineRenderer.box(g, 7.6, 12.6, vc - 0.4, 8.4, 16.2 + rise, vc + 0.4, CHROME);
		g.push();
		g.translate(8, 15.75, vc);
		g.yaw(Math.toRadians(spin));
		double r = 3.4, half = r * Math.tan(Math.toRadians(22.5));
		for (int k = 0; k < 8; k++) {
			g.push();
			g.yaw(Math.toRadians(45 * k));
			LineRenderer.box(g, -half, -0.3, r - 0.75, half, 0.3, r, RED_IRON);
			g.pop();
		}
		LineRenderer.box(g, -r + 0.6, -0.18, -0.35, r - 0.6, 0.18, 0.35, RED_IRON);
		LineRenderer.box(g, -0.35, -0.18, -r + 0.6, 0.35, 0.18, r - 0.6, RED_IRON);
		LineRenderer.box(g, -0.9, -0.42, -0.9, 0.9, 0.42, 0.9, CHROME);
		// A knob on the rim to turn it by.
		LineRenderer.box(g, r - 0.6, 0.3, -0.3, r - 0.1, 1.5, 0.3, RED_IRON);
		g.pop();
	}

	/** The float gauge on the shell above the outlet (the shell face is at z = 16). */
	private static void gauge(Geometry g, State state) {
		int blocks = state.gauge;
		double bottom = 17.0, top = 16.0 * (blocks + 1) - 1.0;
		// The board, a block of markings at a time, on two stand-off brackets per block.
		for (int k = 0; k < blocks; k++) {
			g.light = state.gaugeLight[Math.min(k + 1, state.gaugeLight.length - 1)];
			double y0 = Math.max(bottom, 16.0 * (k + 1)), y1 = Math.min(top, 16.0 * (k + 2));
			LineRenderer.panel(g, 5.6, y0, 14.2, 10.4, y1, 14.7, LineRenderer.NORTH, GAUGE_BOARD, WHITE);
			LineRenderer.box(g, 7.4, y0 + 6, 14.7, 8.6, y0 + 7.2, 16.0, GUNMETAL);
		}
		// Guide wires either side, the pointer between them, the cable up to the pulley.
		g.light = state.gaugeLight[Math.min(blocks, state.gaugeLight.length - 1)];
		LineRenderer.box(g, 4.4, bottom, 13.9, 4.8, top + 2, 14.3, BLACK);
		LineRenderer.box(g, 11.2, bottom, 13.9, 11.6, top + 2, 14.3, BLACK);
		double py = bottom + 1.2 + state.fill * (top - bottom - 2.4);
		g.light = state.gaugeLight[Math.min((int) (py / 16), state.gaugeLight.length - 1)];
		LineRenderer.box(g, 3.4, py - 1.8, 13.0, 12.6, py + 1.8, 14.0, POINTER);
		LineRenderer.box(g, 4.2, py - 0.3, 12.7, 11.8, py + 0.3, 13.0, BLACK);
		g.light = state.gaugeLight[state.gaugeLight.length - 1];
		LineRenderer.box(g, 7.8, py + 1.0, 12.9, 8.2, top + 3.4, 13.3, BLACK);
		// The pulley in its bracket on the roof's edge, the cable over it into the tank.
		double pc = top + 4.2;
		LineRenderer.box(g, 6.6, top + 1.0, 13.0, 7.0, pc + 1.2, 18.0, GUNMETAL);
		LineRenderer.box(g, 9.0, top + 1.0, 13.0, 9.4, pc + 1.2, 18.0, GUNMETAL);
		g.push();
		g.translate(8, pc, 14.4);
		for (int k = 0; k < 4; k++) {
			g.push();
			g.pitch(Math.toRadians(45 * k));
			LineRenderer.box(g, -0.9, -1.4, -0.6, 0.9, 1.4, 0.6, CHROME);
			g.pop();
		}
		g.pop();
		LineRenderer.box(g, 7.8, pc + 1.3, 13.2, 8.2, pc + 1.7, 17.6, BLACK);
		LineRenderer.box(g, 7.8, pc - 3.0, 17.2, 8.2, pc + 1.7, 17.6, BLACK);
	}

	/** The painted name on the shell (and a diesel tank's fire diamond). */
	private void stencil(State state, PoseStack pose, SubmitNodeCollector collector) {
		Direction face = state.stencilFace;
		float width = Math.max(1.0F, state.stencilWidth - 0.8F);
		// Each line at its size, smaller only if one word alone is wider than the clear width.
		float total = 0;
		float[] scale = new float[state.lines.size()];
		for (int i = 0; i < state.lines.size(); i++) {
			scale[i] = Math.min(state.sizes.get(i), width / Math.max(1, state.widths.get(i)));
			total += scale[i] * 11;
		}
		float diamond = state.diamond ? Math.min(1.5F, width * 0.4F) : 0;
		total += diamond > 0 ? diamond * 1.5F : 0;
		// All of it smaller if it is taller than the clear shell between the foot and the band.
		if (state.stencilHeight > 0 && total > state.stencilHeight) {
			float fit = state.stencilHeight / total;
			for (int i = 0; i < scale.length; i++) {
				scale[i] *= fit;
			}
			diamond *= fit;
			total = state.stencilHeight;
		}
		pose.pushPose();
		pose.translate(state.stencilCenter.x, state.stencilCenter.y + total / 2, state.stencilCenter.z);
		pose.mulPose(new org.joml.Matrix4f().rotationY((float) Math.toRadians(-face.toYRot())));
		pose.translate(0, 0, 0.012F);
		float y = 0;
		for (int i = 0; i < state.lines.size(); i++) {
			pose.pushPose();
			pose.translate(0, -y, 0);
			pose.scale(scale[i], -scale[i], scale[i]);
			collector.submitText(pose, -state.widths.get(i) / 2F, 0, state.lines.get(i), false, Font.DisplayMode.POLYGON_OFFSET,
					state.stencilLight, state.colours.get(i), 0, 0);
			pose.popPose();
			y += scale[i] * 11;
		}
		if (diamond > 0) {
			// NFPA 704: health 1 (blue, left), fire 2 (red, top), instability 0 (yellow, right), nothing special (white).
			float s = diamond / 2;
			float cy = -y - s * 1.1F;
			int light = state.stencilLight;
			pose.pushPose();
			pose.translate(0, cy, 0.004F);
			collector.submitCustomGeometry(pose, RENDER_TYPE, (p, consumer) -> {
				Geometry g = new Geometry(p, consumer);
				g.light = light;
				float q = s / 2;
				diamondQuad(g, 0, q, q, NFPA_RED);
				diamondQuad(g, -q, 0, q, NFPA_BLUE);
				diamondQuad(g, q, 0, q, NFPA_YELLOW);
				diamondQuad(g, 0, -q, q, NFPA_WHITE);
			});
			String[] numbers = {"2", "1", "0"};
			float[][] at = {{0, s / 2}, {-s / 2, 0}, {s / 2, 0}};
			for (int i = 0; i < 3; i++) {
				pose.pushPose();
				pose.translate(at[i][0], at[i][1], 0.006F);
				float k = s * 0.055F;
				pose.scale(k, -k, k);
				FormattedCharSequence n = Component.literal(numbers[i]).getVisualOrderText();
				collector.submitText(pose, -font.width(numbers[i]) / 2F, -4, n, false, Font.DisplayMode.POLYGON_OFFSET, light, 0xFF101012, 0, 0);
				pose.popPose();
			}
			pose.popPose();
		}
		pose.popPose();
	}

	/** One square of the diamond: a square turned 45 degrees, centred at (x, y), half its diagonal {@code h}. */
	private static void diamondQuad(Geometry g, float x, float y, float h, float[] uv) {
		float d = h * 0.96F;
		g.quad(x - d, y, 0, x, y - d, 0, x + d, y, 0, x, y + d, 0, 0, 0, 1, uv);
	}

	private static float yRotation(Direction facing) {
		return switch (facing) {
			case EAST -> 90;
			case SOUTH -> 180;
			case WEST -> 270;
			default -> 0;
		};
	}

	@Override
	public boolean shouldRenderOffScreen() {
		return true;
	}

	@Override
	public int getViewDistance() {
		return 160;
	}
}
