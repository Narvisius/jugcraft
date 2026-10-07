package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.client.DroneDepotRenderer.Geometry;
import io.github.jimbozoomer.jugcraft.nuclear.StorageTankHatchBlock;
import io.github.jimbozoomer.jugcraft.nuclear.StorageTankOutletBlock;
import io.github.jimbozoomer.jugcraft.nuclear.StorageTankPortBlock;
import io.github.jimbozoomer.jugcraft.nuclear.StorageTankPortBlockEntity;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.transfer.v1.client.fluid.FluidVariantRendering;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A storage tank nozzle's moving parts and, for a formed tank's outlet, what is painted and hung on the shell.
 * <ul>
 * <li>The gate valve's rising stem and red handwheel: opening, the wheel turns two turns and the stem rises out of the
 * yoke; shutting, the other way.</li>
 * <li>The float gauge up the shell over the outlet: a white board marked in tenths, a red pointer riding up and down
 * with the level between two guide wires, its cable up to a pulley on the roof's edge and over into the tank.</li>
 * <li>The stencil on the widest clear side of the shell, filled in from what the tank holds (TankProducts; Justin,
 * Oct 7): a name tag's title, a plant tank's name and tag or else the liquid's name, its UN number, the NFPA 704 fire
 * diamond with its numbers and special mark, and the warning lines of its kind. An empty tank keeps the last liquid's
 * label. Dark lettering, light on a corrosive tank's dark shell.</li>
 * </ul>
 */
public class StorageTankRenderer implements BlockEntityRenderer<StorageTankPortBlockEntity, StorageTankRenderer.State> {
	public static final Identifier TEXTURE = Jugcraft.id("textures/entity/storage_tank.png");
	private static final RenderType RENDER_TYPE = RenderTypes.entityCutout(TEXTURE);
	/** The inside of the tank: drawn facing in only, so it never shows from outside. */
	private static final RenderType LINING_TYPE = RenderTypes.entityCutoutCull(TEXTURE);
	/** The liquid: the fluid's own (animated) texture from the block atlas, a little see-through for water. */
	private static final RenderType FLUID_TYPE = RenderTypes.entityTranslucentCull(TextureAtlas.LOCATION_BLOCKS);
	/** The sheet is 512 x 256: 64 px regions, 8 across and 4 down (tools/storage_tank.py SHEET). */
	private static final float PX_U = 1 / 512F, PX_V = 1 / 256F;
	static final float[] RED_IRON = region(0, 0), CHROME = region(1, 0), GUNMETAL = region(2, 0), YELLOW = region(3, 0);
	static final float[] GAUGE_BOARD = region(0, 1), POINTER = region(1, 1), BLACK = region(2, 1), GALVANISED = region(3, 1);
	static final float[] WHITE = region(0, 2), NFPA_BLUE = region(1, 2), NFPA_RED = region(2, 2), NFPA_YELLOW = region(3, 2);
	static final float[] RIM = region(0, 3), SPRING = region(1, 3), HAZARD_TAG = region(2, 3), NFPA_WHITE = region(3, 3);
	static final float[] LINING = region(4, 0), LINING_FLOOR = region(5, 0), LINING_ROOF = region(6, 0), GASKET = region(7, 0);
	/** A corrosive tank's darker paint (its hatch lid). */
	static final float[] DARK_PAINT = region(4, 1);

	private static float[] region(int col, int row) {
		return new float[] {(col * 64 + 0.5F) * PX_U, (row * 64 + 0.5F) * PX_V, (col * 64 + 63.5F) * PX_U, (row * 64 + 63.5F) * PX_V};
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
		/** The fire diamond's health, fire and instability numbers, and its special mark ("", "OX", "W"). */
		final int[] nfpa = new int[3];
		String special = "";
		int stencilLight;
		/** The inside of the tank (null when not formed) and the liquid: its level (blocks above the outlet), look and light. */
		@Nullable Lining lining;
		final List<float[]> ceilings = new ArrayList<>();
		float surface;
		boolean liquid;
		@Nullable TextureAtlasSprite sprite;
		int liquidColour, liningLight, liquidLight;
	}

	/**
	 * The inside of a formed tank, worked out once from the outlet's columns: the walls round the space and its floor
	 * (quads facing in), the cells of the roof's underside (a hatch over one leaves it open), and the runs themselves.
	 */
	static final class Lining {
		final int[] columns;
		final float[] walls;
		final int[] ceilings;
		final int bottom, top;

		Lining(int[] columns) {
			this.columns = columns;
			java.util.Map<Long, List<int[]>> runs = new java.util.HashMap<>();
			int lo = Integer.MAX_VALUE, hi = Integer.MIN_VALUE;
			for (int c : columns) {
				int x = StorageTankPortBlockEntity.columnX(c), z = StorageTankPortBlockEntity.columnZ(c);
				int y0 = StorageTankPortBlockEntity.columnBottom(c), y1 = StorageTankPortBlockEntity.columnTop(c);
				runs.computeIfAbsent(key(x, z), k -> new ArrayList<>()).add(new int[] {y0, y1});
				lo = Math.min(lo, y0);
				hi = Math.max(hi, y1);
			}
			bottom = lo == Integer.MAX_VALUE ? 0 : lo;
			top = hi == Integer.MIN_VALUE ? 0 : hi;
			List<Float> quads = new ArrayList<>();
			List<Integer> roof = new ArrayList<>();
			int[][] sides = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
			for (int c : columns) {
				int x = StorageTankPortBlockEntity.columnX(c), z = StorageTankPortBlockEntity.columnZ(c);
				int y0 = StorageTankPortBlockEntity.columnBottom(c), y1 = StorageTankPortBlockEntity.columnTop(c);
				// The floor and the roof's underside.
				add(quads, 0, x, y0, z, x + 1, y0, z + 1);
				roof.add(c);
				for (int[] d : sides) {
					List<int[]> beside = runs.get(key(x + d[0], z + d[1]));
					for (int y = y0; y <= y1; y++) {
						if (!within(beside, y)) {
							int side = d[0] == 1 ? 1 : d[0] == -1 ? 2 : d[1] == 1 ? 3 : 4;
							add(quads, side, x, y, z, x + 1, y + 1, z + 1);
						}
					}
				}
			}
			walls = new float[quads.size()];
			for (int i = 0; i < walls.length; i++) {
				walls[i] = quads.get(i);
			}
			ceilings = roof.stream().mapToInt(Integer::intValue).toArray();
		}

		private static long key(int x, int z) {
			return (long) x << 32 | (z & 0xFFFFFFFFL);
		}

		private static boolean within(@Nullable List<int[]> runs, int y) {
			if (runs != null) {
				for (int[] r : runs) {
					if (y >= r[0] && y <= r[1]) {
						return true;
					}
				}
			}
			return false;
		}

		/** A quad facing into the space: 0 the floor, 1..4 the wall on the +x, -x, +z, -z side of a cell (kind, then the cell's box). */
		private static void add(List<Float> quads, int kind, float x0, float y0, float z0, float x1, float y1, float z1) {
			quads.add((float) kind);
			quads.add(x0);
			quads.add(y0);
			quads.add(z0);
			quads.add(x1);
			quads.add(y1);
			quads.add(z1);
		}
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
		state.lining = null;
		state.ceilings.clear();
		state.liquid = false;
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
		float target = capacity <= 0 || port.fluid.isResourceBlank() ? 0 : Math.min(1, port.fluid.amount / (float) capacity);
		// The level eases to where it now is (the server sends it twice a second), so it rises and falls smoothly.
		long nanos = System.nanoTime();
		if (port.shownFill < 0) {
			port.shownFill = target;
		} else {
			float dt = Math.min(1, (nanos - port.shownAt) / 1.0E9F);
			port.shownFill += (target - port.shownFill) * Math.min(1, dt * 2.5F);
		}
		port.shownAt = nanos;
		state.fill = port.shownFill;
		inside(port, state, level, pos);
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
			stencil(state, port, block);
		}
	}

	/** What is painted on the shell (see the class comment). */
	private void stencil(State state, StorageTankPortBlockEntity port, BlockState block) {
		io.github.jimbozoomer.jugcraft.nuclear.TankRating rating = block.getValue(StorageTankOutletBlock.RATING);
		boolean dark = rating == io.github.jimbozoomer.jugcraft.nuclear.TankRating.CORROSIVE;
		int ink = dark ? 0xFFF2F2EE : 0xFF1C1C1E;
		StorageTankOutletBlock.Service service = block.getValue(StorageTankOutletBlock.SERVICE);
		String key = "tank.jugcraft." + service.getSerializedName();
		net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant liquid = port.label();
		io.github.jimbozoomer.jugcraft.nuclear.TankProducts.Product product = io.github.jimbozoomer.jugcraft.nuclear.TankProducts.of(liquid);
		if (!port.title().isEmpty()) {
			line(state, Component.literal(port.title().toUpperCase(java.util.Locale.ROOT)), 0.075F, ink);
		} else if (service != StorageTankOutletBlock.Service.NONE) {
			line(state, Component.translatable(key + ".name"), 0.075F, ink);
		}
		if (service != StorageTankOutletBlock.Service.NONE) {
			line(state, Component.translatable(key + ".tag"), 0.06F, ink);
		}
		if (product == null) {
			return;
		}
		// A plant tank's name says what it holds; any other names its liquid (under the title, if it has one).
		String name = FluidVariantAttributes.getName(liquid).getString().toUpperCase(java.util.Locale.ROOT);
		if (service == StorageTankOutletBlock.Service.NONE) {
			line(state, Component.literal(name), port.title().isEmpty() ? 0.075F : 0.055F, ink);
		}
		if (!product.un().isEmpty()) {
			line(state, Component.literal("UN " + product.un()), 0.045F, ink);
		}
		boolean first = true;
		for (String warning : product.warnings()) {
			int colour = switch (warning) {
				case "cryogenic" -> 0xFF2A64B4;
				case "corrosive", "no_water", "face_shield" -> dark ? 0xFFF2CE2A : 0xFF8A6A00;
				default -> 0xFFB01818;
			};
			line(state, Component.translatable("tank.jugcraft.warning." + warning), first ? 0.045F : 0.04F, colour);
			first = false;
		}
		if (product.diamond()) {
			state.diamond = true;
			state.nfpa[0] = product.health();
			state.nfpa[1] = product.fire();
			state.nfpa[2] = product.instability();
			state.special = product.special();
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

	/** The inside of the tank and the liquid in it, seen through an open roof hatch. */
	private static void inside(StorageTankPortBlockEntity port, State state, Level level, BlockPos pos) {
		int[] columns = port.columns();
		if (columns.length == 0) {
			return;
		}
		int key = java.util.Arrays.hashCode(columns);
		if (!(port.lining instanceof Lining) || port.liningKey != key) {
			port.lining = new Lining(columns);
			port.liningKey = key;
		}
		Lining lining = (Lining) port.lining;
		state.lining = lining;
		// Under a hatch the roof is open.
		for (int c : lining.ceilings) {
			int x = StorageTankPortBlockEntity.columnX(c), z = StorageTankPortBlockEntity.columnZ(c), y = StorageTankPortBlockEntity.columnTop(c) + 1;
			if (!(level.getBlockState(pos.offset(x, y, z)).getBlock() instanceof StorageTankHatchBlock)) {
				state.ceilings.add(new float[] {x, y, z});
			}
		}
		// Daylight comes in at the hatch: the inside is a good deal darker than the roof.
		int c0 = columns[0];
		int roof = LightCoordsUtil.getLightCoords(level, pos.offset(StorageTankPortBlockEntity.columnX(c0), lining.top + 2,
				StorageTankPortBlockEntity.columnZ(c0)));
		int sky = LightCoordsUtil.sky(roof), block = LightCoordsUtil.block(roof);
		state.liningLight = LightCoordsUtil.pack(Math.max(block - 1, 7), Math.max(sky - 3, 0));
		state.liquidLight = LightCoordsUtil.pack(Math.max(block, 9), Math.max(sky - 2, 0));
		if (state.fill > 0.001F && !port.fluid.isResourceBlank()) {
			FluidVariant variant = port.fluid.variant;
			FluidModel model = Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(variant.getFluid().defaultFluidState());
			state.sprite = model.stillMaterial().sprite();
			int rgb = (Minecraft.getInstance().level != null ? FluidVariantRendering.getColor(variant, Minecraft.getInstance().level, pos)
					: FluidVariantRendering.getColor(variant)) & 0xFFFFFF;
			boolean water = variant.getFluid().isSame(Fluids.WATER);
			state.liquidColour = (water ? 0xD8000000 : 0xF4000000) | rgb;
			int height = lining.top + 1 - lining.bottom;
			state.surface = Math.min(lining.top + 1 - 0.04F, lining.bottom + state.fill * height);
			state.liquid = true;
		}
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		if (state.lining != null) {
			lining(state, pose, collector);
		}
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

	/** The tank's inside walls, floor and roof (facing in), and the liquid's surface at its level. */
	private static void lining(State state, PoseStack pose, SubmitNodeCollector collector) {
		Lining lining = state.lining;
		int light = state.liningLight;
		collector.submitCustomGeometry(pose, LINING_TYPE, (p, consumer) -> {
			Geometry g = new Geometry(p, consumer);
			g.light = light;
			float[] w = lining.walls;
			for (int i = 0; i < w.length; i += 7) {
				int kind = (int) w[i];
				float ax = w[i + 1], ay = w[i + 2], az = w[i + 3], bx = w[i + 4], by = w[i + 5], bz = w[i + 6];
				switch (kind) {
					case 0 -> g.quad(ax, ay + 0.002F, az, ax, ay + 0.002F, bz, bx, ay + 0.002F, bz, bx, ay + 0.002F, az, 0, 1, 0, LINING_FLOOR);
					// A wall on the +x side of the cell faces -x, and so on.
					case 1 -> g.quad(bx, ay, bz, bx, by, bz, bx, by, az, bx, ay, az, -1, 0, 0, LINING);
					case 2 -> g.quad(ax, ay, az, ax, by, az, ax, by, bz, ax, ay, bz, 1, 0, 0, LINING);
					case 3 -> g.quad(ax, ay, bz, ax, by, bz, bx, by, bz, bx, ay, bz, 0, 0, -1, LINING);
					default -> g.quad(bx, ay, az, bx, by, az, ax, by, az, ax, ay, az, 0, 0, 1, LINING);
				}
			}
			for (float[] c : state.ceilings) {
				float x = c[0], y = c[1] - 0.002F, z = c[2];
				g.quad(x, y, z + 1, x, y, z, x + 1, y, z, x + 1, y, z + 1, 0, -1, 0, LINING_ROOF);
			}
		});
		if (!state.liquid || state.sprite == null) {
			return;
		}
		TextureAtlasSprite sprite = state.sprite;
		float y = state.surface;
		int colour = state.liquidColour, liquidLight = state.liquidLight;
		float[] uv = {sprite.getU0(), sprite.getV0(), sprite.getU1(), sprite.getV1()};
		collector.submitCustomGeometry(pose, FLUID_TYPE, (p, consumer) -> {
			Geometry g = new Geometry(p, consumer);
			g.light = liquidLight;
			g.color = colour;
			for (int c : lining.columns) {
				int y0 = StorageTankPortBlockEntity.columnBottom(c), y1 = StorageTankPortBlockEntity.columnTop(c);
				if (y < y0 || y > y1 + 1) {
					continue;
				}
				float x = StorageTankPortBlockEntity.columnX(c), z = StorageTankPortBlockEntity.columnZ(c);
				g.quad(x, y, z, x, y, z + 1, x + 1, y, z + 1, x + 1, y, z, 0, 1, 0, uv);
			}
		});
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
			// NFPA 704: health (blue, left), fire (red, top), instability (yellow, right), the special mark (white, bottom).
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
			String[] numbers = {String.valueOf(state.nfpa[1]), String.valueOf(state.nfpa[0]), String.valueOf(state.nfpa[2]), state.special};
			float[][] at = {{0, s / 2}, {-s / 2, 0}, {s / 2, 0}, {0, -s / 2}};
			for (int i = 0; i < 4; i++) {
				if (numbers[i].isEmpty()) {
					continue;
				}
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
