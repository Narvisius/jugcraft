package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.tower.SlidingDoorBlock;
import io.github.jimbozoomer.jugcraft.tower.SlidingDoorBlockEntity;
import io.github.jimbozoomer.jugcraft.tower.SlidingDoorStyle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The automatic sliding doors (tower/SlidingDoorBlock.java; take 1 of the sliding doors sheet, approved with clear glass
 * inside): drawn here from {@link SlidingDoorStyle}'s numbers, in the door's own frame (the door facing north: its outside
 * at -z, x across from the master cell, blocks), turned to its facing as the aisle containment doors are.
 *
 * <ul>
 * <li>The entrance: dark anodised jambs and the operator header over the doorway with ENTRANCE on its face, the amber
 * line under it and the motion sensor (green ready, amber while open), the stainless threshold, the two fixed framed
 * sidelights at the back plane and the two framed leaves (stainless kick plates) sliding in front of them.</li>
 * <li>A room's door: a satin lining round the doorway, the track cover on the wall's outside face over the doorway and
 * the wall a leaf slides over, its sensor, and the leaves hanging from it: clear framed glass with a mid rail and kick
 * plates, or steel with a vision panel, kick plates and recessed pulls.</li>
 * </ul>
 *
 * The solid faces go with the solid things and the glass after them (client/warehouse/WhQuads.java: drawn together, a
 * pane drawn before the frame behind it hid the frame); the glass is drawn farthest plane first from where the camera is.
 * No two faces that look the same way share a plane where they overlap (no z-fighting).
 */
public class SlidingDoorRenderer implements BlockEntityRenderer<SlidingDoorBlockEntity, SlidingDoorRenderer.State> {
	private static final RenderType SOLID = RenderTypes.entityCutoutCull(TextureAtlas.LOCATION_BLOCKS);
	private static final RenderType CLEAR = RenderTypes.entityTranslucentCull(TextureAtlas.LOCATION_BLOCKS);
	/** The server doors' own textures (tools/aisle_containment.py), tinted: their indexes in {@link #SPRITES}. */
	static final int ALU = 0, GLASS = 1, LENS = 2, AMBER = 3, GREEN = 4;
	private static final SpriteId[] SPRITES = {sprite("acd_alu"), sprite("acd_glass"), sprite("acd_lens"), sprite("acd_led_amber"),
			sprite("acd_led_green")};
	/** A texture's place on the atlas (kept, and made again only when the atlas gives a different sprite: no garbage per frame). */
	record Tex(float u0, float v0, float u1, float v1) {
		float u(float f) {
			return u0 + (u1 - u0) * f;
		}

		float v(float f) {
			return v0 + (v1 - v0) * f;
		}
	}

	private static final TextureAtlasSprite[] SEEN = new TextureAtlasSprite[SPRITES.length];
	private static final Tex[] TEX = new Tex[SPRITES.length];
	/** Where the textures come from (a check outside the game gives its own). */
	static java.util.function.IntFunction<Tex> textures = SlidingDoorRenderer::lookup;
	/** Tints on the brushed aluminium: dark anodised, satin, stainless, the steel leaves' paint, the pulls. */
	private static final int ANOD = 0xFF2E3134, SATIN = 0xFFE9E9EA, STAIN = 0xFFFFFFFF, STEEL = 0xFFCDD8E2, PULL = 0xFF52565C,
			WHITE = 0xFFFFFFFF;
	private static final int GLOW = LightCoordsUtil.FULL_BRIGHT;
	/** The entrance header's lettering: its middle (blocks up the header's face), its capitals' height, its colour. */
	private static final float LABEL_Y = 2.775F, LABEL_SIZE = 0.115F;
	private static final int LABEL_COLOUR = 0xFFF2F2F2;
	private static final String LABEL = "E  N  T  R  A  N  C  E";
	private final Font font;

	public static final class State extends BlockEntityRenderState {
		SlidingDoorStyle style = SlidingDoorStyle.DOUBLE_GLASS;
		Direction facing = Direction.NORTH;
		boolean mirrored;
		float open;
		/** The camera's depth in the door's frame (z: 0 its outside face, +z in), to draw the glass farthest first. */
		double cameraZ;
		/** A room door's lining's depth (frame z from, to) on its left, its right and over it: the wall's there. */
		final float[] lining = {0, 1, 0, 1, 0, 1};
	}

	public SlidingDoorRenderer(BlockEntityRendererProvider.Context context) {
		this.font = context.font();
	}

	private static SpriteId sprite(String name) {
		return new SpriteId(TextureAtlas.LOCATION_BLOCKS, Identifier.fromNamespaceAndPath("jugcraft", "block/" + name));
	}

	private static Tex lookup(int i) {
		TextureAtlasSprite s = Minecraft.getInstance().getAtlasManager().get(SPRITES[i]);
		if (s != SEEN[i] || TEX[i] == null) {
			SEEN[i] = s;
			TEX[i] = new Tex(s.getU0(), s.getV0(), s.getU1(), s.getV1());
		}
		return TEX[i];
	}

	private static Tex get(int i) {
		return textures.apply(i);
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(SlidingDoorBlockEntity door, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(door, state, crumbling);
		BlockState block = door.getBlockState();
		state.style = door.style();
		state.facing = block.hasProperty(SlidingDoorBlock.FACING) ? block.getValue(SlidingDoorBlock.FACING) : Direction.NORTH;
		state.mirrored = block.hasProperty(SlidingDoorBlock.MIRRORED) && block.getValue(SlidingDoorBlock.MIRRORED);
		Level level = door.getLevel();
		state.open = level == null ? 0 : door.openness(level.getGameTime(), partialTick);
		// out along the facing is -z of the frame; the cell's middle is z 0.5
		double dx = camera.x - (door.getBlockPos().getX() + 0.5), dz = camera.z - (door.getBlockPos().getZ() + 0.5);
		state.cameraZ = 0.5 - (dx * state.facing.getStepX() + dz * state.facing.getStepZ());
		if (!state.style.entrance() && level != null && (door.liningAt == Long.MIN_VALUE || level.getGameTime() - door.liningAt >= LINING_EVERY
				|| level.getGameTime() < door.liningAt)) {
			door.liningAt = level.getGameTime();
			BlockPos pos = door.getBlockPos();
			depth(level, pos, state.facing, -1, 0, door.lining, 0);
			depth(level, pos, state.facing, state.style.width, 0, door.lining, 2);
			depth(level, pos, state.facing, 0, 2, door.lining, 4);
		}
		System.arraycopy(door.lining, 0, state.lining, 0, 6);
	}

	/** Ticks between looks at the walls round a room door (for its lining's depth). */
	private static final int LINING_EVERY = 40;

	/**
	 * The depth (frame z from, to) of the wall in the door's frame cell (fx, fy), into {@code out} at {@code at}: what of
	 * the block there stands in the wall's line (framed glass is a thin panel on the wall's outside face, a painted wall
	 * the whole block). Nothing there, or a whole block: the whole depth.
	 */
	private static void depth(Level level, BlockPos master, Direction facing, int fx, int fy, float[] out, int at) {
		int[] t = io.github.jimbozoomer.jugcraft.tower.AisleContainmentDoorBlock.turn(fx, 0, facing);
		BlockPos p = master.offset(t[0], fy, t[1]);
		float z0 = 0, z1 = 1;
		if (level.isLoaded(p)) {
			net.minecraft.world.phys.shapes.VoxelShape shape = level.getBlockState(p).getShape(level, p);
			if (!shape.isEmpty()) {
				Direction.Axis axis = facing.getAxis();
				double lo = shape.min(axis), hi = shape.max(axis);
				// the frame's z runs in from the outside face: against the facing
				boolean positive = facing.getAxisDirection() == Direction.AxisDirection.POSITIVE;
				z0 = (float) (positive ? 1 - hi : lo);
				z1 = (float) (positive ? 1 - lo : hi);
				if (z1 - z0 < 0.05F) {
					z0 = 0;
					z1 = 1;
				}
			}
		}
		out[at] = z0;
		out[at + 1] = z1;
	}

	@Override
	public boolean shouldRenderOffScreen() {
		return true;
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		final SlidingDoorStyle style = state.style;
		final boolean mirrored = state.mirrored;
		final float open = state.open;
		final double cameraZ = state.cameraZ;
		final int light = state.lightCoords;
		final float[] lining = state.lining;   // (a new state each frame: an off-screen-drawn block entity's)
		pose.pushPose();
		pose.translate(0.5F, 0, 0.5F);
		pose.rotateDegrees(Axis.YP, -state.facing.toYRot() + 180);
		pose.translate(-0.5F, 0, -0.5F);
		collector.submitCustomGeometry(pose, SOLID, (p, c) -> {
			if (style.entrance()) {
				entranceSolid(p, c, open, light);
			} else {
				roomSolid(p, c, style, mirrored, open, lining, light);
			}
		});
		collector.submitCustomGeometry(pose, CLEAR, (p, c) -> {
			if (style.entrance()) {
				entranceGlass(p, c, open, cameraZ, light);
			} else {
				roomGlass(p, c, style, mirrored, open, light);
			}
		});
		if (style.entrance()) {
			label(pose, collector, light);
		}
		pose.popPose();
	}

	// ------------------------------------------------------------------ the entrance

	static void entranceSolid(PoseStack.Pose p, VertexConsumer c, float open, int light) {
		Tex alu = get(ALU);
		double w = SlidingDoorStyle.ENTRANCE.width, j = SlidingDoorStyle.E_JAMB, h = SlidingDoorStyle.E_HEADER;
		// the jambs (up to the header), the header across the top row
		double f = SlidingDoorStyle.E_HEAD_Z0, back = SlidingDoorStyle.E_HEAD_Z1;
		box(p, c, alu, ANOD, light, 0, 0, f, j, h, back);
		box(p, c, alu, ANOD, light, w - j, 0, f, w, h, back);
		box(p, c, alu, ANOD, light, 0, h, f, w, 3, back);
		// the amber line along its underside's front edge, the motion sensor under it and its light (green ready, amber while open)
		box(p, c, get(AMBER), WHITE, GLOW, j + 0.02, h - 0.005, f + 0.03, w - j - 0.02, h + 0.01, f + 0.06);
		box(p, c, get(LENS), WHITE, light, w / 2 - 0.14, 2.47, f - 0.01, w / 2 + 0.14, h + 0.005, f + 0.12);
		box(p, c, get(open > 0.01F ? AMBER : GREEN), WHITE, GLOW, w / 2 - 0.02, 2.49, f - 0.017, w / 2 + 0.02, 2.51, f - 0.01);
		// the threshold between the jambs
		box(p, c, alu, STAIN, light, j, 0, f, w - j, 0.015, back);
		double top = SlidingDoorStyle.E_LEAF_TOP, leaf = SlidingDoorStyle.E_LEAF;
		// the fixed sidelights (back plane) and the leaves (front plane), slid as far as the door is open
		for (int side = 0; side < 2; side++) {
			double x0 = side == 0 ? j : w - j - leaf;
			frame(p, c, alu, ANOD, light, x0, 0.015, x0 + leaf, top, SlidingDoorStyle.E_SIDE_Z, SlidingDoorStyle.E_PANEL, 0.06, 0.08, 0.12,
					false, Double.NaN);
		}
		double s = leaf * open, mid = w / 2;
		frame(p, c, alu, ANOD, light, mid - leaf - s, 0.015, mid - s, top, SlidingDoorStyle.E_LEAF_Z, SlidingDoorStyle.E_PANEL, 0.07, 0.08, 0.14,
				true, Double.NaN);
		frame(p, c, alu, ANOD, light, mid + s, 0.015, mid + leaf + s, top, SlidingDoorStyle.E_LEAF_Z, SlidingDoorStyle.E_PANEL, 0.07, 0.08, 0.14,
				true, Double.NaN);
	}

	static void entranceGlass(PoseStack.Pose p, VertexConsumer c, float open, double cameraZ, int light) {
		Tex glass = get(GLASS);
		double w = SlidingDoorStyle.ENTRANCE.width, j = SlidingDoorStyle.E_JAMB, top = SlidingDoorStyle.E_LEAF_TOP, leaf = SlidingDoorStyle.E_LEAF;
		double s = leaf * open, mid = w / 2;
		boolean outside = cameraZ < (SlidingDoorStyle.E_LEAF_Z + SlidingDoorStyle.E_SIDE_Z) / 2;
		for (int pass = 0; pass < 2; pass++) {
			boolean sidelights = (pass == 0) == outside;   // farthest first
			if (sidelights) {
				for (int side = 0; side < 2; side++) {
					double x0 = side == 0 ? j : w - j - leaf;
					framePane(p, c, glass, light, x0, 0.015, x0 + leaf, top, SlidingDoorStyle.E_SIDE_Z, 0.06, 0.08, 0.12, Double.NaN);
				}
			} else {
				framePane(p, c, glass, light, mid - leaf - s, 0.015, mid - s, top, SlidingDoorStyle.E_LEAF_Z, 0.07, 0.08, 0.14, Double.NaN);
				framePane(p, c, glass, light, mid + s, 0.015, mid + leaf + s, top, SlidingDoorStyle.E_LEAF_Z, 0.07, 0.08, 0.14, Double.NaN);
			}
		}
	}

	/** ENTRANCE across the header's face, read from outside. */
	private void label(PoseStack pose, SubmitNodeCollector collector, int light) {
		pose.pushPose();
		pose.translate(SlidingDoorStyle.ENTRANCE.width / 2.0F, LABEL_Y, (float) SlidingDoorStyle.E_HEAD_Z0 - 0.002F);
		pose.rotateDegrees(Axis.YP, 180);
		float k = LABEL_SIZE / 7F;
		pose.scale(k, -k, k);
		var line = Texts.of(LABEL);
		float width = font.width(line);
		collector.submitText(pose, -width / 2, -3.5F, line, false, Font.DisplayMode.POLYGON_OFFSET, light, LABEL_COLOUR, 0, 0);
		pose.popPose();
	}

	// ------------------------------------------------------------------ a room's door

	/** Which way a room door's leaf {@code i} slides (-1 or +1). */
	private static int way(SlidingDoorStyle style, boolean mirrored, int i) {
		if (style.leaves == 2) {
			return i == 0 ? -1 : 1;
		}
		return mirrored ? -1 : 1;
	}

	/** Leaf {@code i}'s left edge, shut, in the frame. */
	private static double leafX(SlidingDoorStyle style, int i) {
		return style.leaves == 2 ? i : 0;
	}

	private static final double LEAF_TOP = 2.06, LEAF_Z = (SlidingDoorStyle.R_LEAF_Z0 + SlidingDoorStyle.R_LEAF_Z1) / 2,
			LEAF_DEPTH = SlidingDoorStyle.R_LEAF_Z1 - SlidingDoorStyle.R_LEAF_Z0;

	static void roomSolid(PoseStack.Pose p, VertexConsumer c, SlidingDoorStyle style, boolean mirrored, float open, float[] lining, int light) {
		Tex alu = get(ALU);
		double w = style.width, l = SlidingDoorStyle.R_LINING;
		// the lining round the doorway, as deep as the wall there (proud of its faces by a hair)
		box(p, c, alu, SATIN, light, 0, 0, lining[0] - 0.005, l, 2, lining[1] + 0.005);
		box(p, c, alu, SATIN, light, w - l, 0, lining[2] - 0.005, w, 2, lining[3] + 0.005);
		box(p, c, alu, SATIN, light, l, 2 - l, lining[4] - 0.005, w - l, 2, lining[5] + 0.005);
		// the track cover over the doorway and the wall the leaves slide over
		double a = style.leaves == 2 || mirrored ? -1.06 : -0.06, b = style.leaves == 2 || !mirrored ? w + 1.06 : w + 0.06;
		box(p, c, alu, SATIN, light, a, SlidingDoorStyle.R_TRACK_Y0, SlidingDoorStyle.R_TRACK_Z, b, SlidingDoorStyle.R_TRACK_Y1, 0);
		double mid = w / 2;
		box(p, c, get(LENS), WHITE, light, mid - 0.1, 1.95, -0.175, mid + 0.1, 2.01, -0.1);
		box(p, c, get(open > 0.01F ? AMBER : GREEN), WHITE, GLOW, mid - 0.015, 1.965, -0.178, mid + 0.015, 1.985, -0.175);
		for (int i = 0; i < style.leaves; i++) {
			int way = way(style, mirrored, i);
			double x0 = leafX(style, i) + way * open, x1 = x0 + 1;
			if (style.glass) {
				frame(p, c, alu, SATIN, light, x0, 0.01, x1, LEAF_TOP, LEAF_Z, LEAF_DEPTH, 0.06, 0.13, 0.12, true, 1.05);
			} else {
				steelLeaf(p, c, alu, light, x0, x1, way, style.leaves == 2);
			}
		}
	}

	static void roomGlass(PoseStack.Pose p, VertexConsumer c, SlidingDoorStyle style, boolean mirrored, float open, int light) {
		Tex glass = get(GLASS);
		for (int i = 0; i < style.leaves; i++) {
			int way = way(style, mirrored, i);
			double x0 = leafX(style, i) + way * open, x1 = x0 + 1;
			if (style.glass) {
				framePane(p, c, glass, light, x0, 0.01, x1, LEAF_TOP, LEAF_Z, 0.06, 0.13, 0.12, 1.05);
			} else {
				double vx = vision(x0, way, style.leaves == 2);
				pane(p, c, glass, WHITE, light, vx, VISION_Y0, vx + VISION_W, VISION_Y1, LEAF_Z);
			}
		}
	}

	private static final double VISION_W = 0.14, VISION_Y0 = 1.0, VISION_Y1 = 1.7, BEAD = 0.02;

	/** A steel leaf's vision panel's left edge: near the meeting edge on a double, the middle on a single. */
	private static double vision(double x0, int way, boolean pair) {
		if (!pair) {
			return x0 + 0.5 - VISION_W / 2;
		}
		return way < 0 ? x0 + 0.62 : x0 + 0.24;
	}

	/** A steel leaf: the slab round its vision panel, the panel's bead both sides, kick plates and pulls both sides. */
	private static void steelLeaf(PoseStack.Pose p, VertexConsumer c, Tex alu, int light, double x0, double x1, int way, boolean pair) {
		double z0 = SlidingDoorStyle.R_LEAF_Z0, z1 = SlidingDoorStyle.R_LEAF_Z1, y0 = 0.01, y1 = LEAF_TOP;
		double hx0 = vision(x0, way, pair), hx1 = hx0 + VISION_W, hy0 = VISION_Y0, hy1 = VISION_Y1;
		box(p, c, alu, STEEL, light, x0, y0, z0, hx0, y1, z1);
		box(p, c, alu, STEEL, light, hx1, y0, z0, x1, y1, z1);
		box(p, c, alu, STEEL, light, hx0, y0, z0, hx1, hy0, z1);
		box(p, c, alu, STEEL, light, hx0, hy1, z0, hx1, y1, z1);
		for (int face = 0; face < 2; face++) {
			double za = face == 0 ? z0 - 0.006 : z1, zb = face == 0 ? z0 : z1 + 0.006;
			// the bead round the panel
			box(p, c, alu, SATIN, light, hx0 - BEAD, hy0 - BEAD, za, hx1 + BEAD, hy0, zb);
			box(p, c, alu, SATIN, light, hx0 - BEAD, hy1, za, hx1 + BEAD, hy1 + BEAD, zb);
			box(p, c, alu, SATIN, light, hx0 - BEAD, hy0, za, hx0, hy1, zb);
			box(p, c, alu, SATIN, light, hx1, hy0, za, hx1 + BEAD, hy1, zb);
			// the kick plate
			double ka = face == 0 ? z0 - 0.004 : z1, kb = face == 0 ? z0 : z1 + 0.004;
			box(p, c, alu, STAIN, light, x0 + 0.04, 0.03, ka, x1 - 0.04, 0.22, kb);
			// the recessed pull at the leading edge (the edge that meets the other leaf or the lining)
			double px = way < 0 ? x1 - 0.1 : x0 + 0.06;
			box(p, c, alu, PULL, light, px, 0.95, za, px + 0.04, 1.15, zb);
		}
	}

	// ------------------------------------------------------------------ parts

	/**
	 * A framed leaf's (or sidelight's) frame, centred on plane {@code z}, {@code depth} thick: stiles, top and bottom rails,
	 * kick plates on the bottom rail both sides if {@code kick}, a mid rail at {@code mid} unless NaN.
	 */
	private static void frame(PoseStack.Pose p, VertexConsumer c, Tex alu, int tint, int light, double x0, double y0, double x1,
			double y1, double z, double depth, double stile, double rail, double bottom, boolean kick, double mid) {
		double za = z - depth / 2, zb = z + depth / 2;
		box(p, c, alu, tint, light, x0, y0, za, x0 + stile, y1, zb);
		box(p, c, alu, tint, light, x1 - stile, y0, za, x1, y1, zb);
		box(p, c, alu, tint, light, x0 + stile, y1 - rail, za, x1 - stile, y1, zb);
		box(p, c, alu, tint, light, x0 + stile, y0, za, x1 - stile, y0 + bottom, zb);
		if (kick) {
			box(p, c, alu, STAIN, light, x0 + stile, y0 + 0.02, za - 0.004, x1 - stile, y0 + bottom - 0.02, za);
			box(p, c, alu, STAIN, light, x0 + stile, y0 + 0.02, zb, x1 - stile, y0 + bottom - 0.02, zb + 0.004);
		}
		if (!Double.isNaN(mid)) {
			box(p, c, alu, tint, light, x0 + stile, mid - 0.03, za + 0.01, x1 - stile, mid + 0.03, zb - 0.01);
		}
	}

	/** The glass in a {@link #frame}: one sheet, or two either side of its mid rail. */
	private static void framePane(PoseStack.Pose p, VertexConsumer c, Tex glass, int light, double x0, double y0, double x1,
			double y1, double z, double stile, double rail, double bottom, double mid) {
		if (Double.isNaN(mid)) {
			pane(p, c, glass, WHITE, light, x0 + stile, y0 + bottom, x1 - stile, y1 - rail, z);
		} else {
			pane(p, c, glass, WHITE, light, x0 + stile, y0 + bottom, x1 - stile, mid - 0.03, z);
			pane(p, c, glass, WHITE, light, x0 + stile, mid + 0.03, x1 - stile, y1 - rail, z);
		}
	}

	/** A sheet of glass at plane z, drawn from both sides (each side's face culled from the other: one plane, never twinned). */
	private static void pane(PoseStack.Pose p, VertexConsumer c, Tex s, int argb, int light, double x0, double y0, double x1,
			double y1, double z) {
		float ax = (float) x0, ay = (float) y0, bx = (float) x1, by = (float) y1, fz = (float) z;
		float us = (float) Math.min(1, x1 - x0), vs = (float) Math.min(1, y1 - y0);
		quad(p, c, s, argb, light, ax, ay, fz, ax, by, fz, bx, by, fz, bx, ay, fz, 0, 0, -1, us, vs);
		quad(p, c, s, argb, light, bx, ay, fz, bx, by, fz, ax, by, fz, ax, ay, fz, 0, 0, 1, us, vs);
	}

	/** A box (blocks, the frame's), each face counter-clockwise seen from outside, its texture as big as the face (to a whole sprite). */
	private static void box(PoseStack.Pose p, VertexConsumer c, Tex s, int argb, int light, double x0, double y0, double z0,
			double x1, double y1, double z1) {
		float ax = (float) x0, ay = (float) y0, az = (float) z0, bx = (float) x1, by = (float) y1, bz = (float) z1;
		float sx = (float) Math.min(1, x1 - x0), sy = (float) Math.min(1, y1 - y0), sz = (float) Math.min(1, z1 - z0);
		quad(p, c, s, argb, light, ax, by, az, ax, by, bz, bx, by, bz, bx, by, az, 0, 1, 0, sx, sz);
		quad(p, c, s, argb, light, ax, ay, bz, ax, ay, az, bx, ay, az, bx, ay, bz, 0, -1, 0, sx, sz);
		quad(p, c, s, argb, light, ax, ay, az, ax, by, az, bx, by, az, bx, ay, az, 0, 0, -1, sx, sy);
		quad(p, c, s, argb, light, bx, ay, bz, bx, by, bz, ax, by, bz, ax, ay, bz, 0, 0, 1, sx, sy);
		quad(p, c, s, argb, light, ax, ay, bz, ax, by, bz, ax, by, az, ax, ay, az, -1, 0, 0, sz, sy);
		quad(p, c, s, argb, light, bx, ay, az, bx, by, az, bx, by, bz, bx, ay, bz, 1, 0, 0, sz, sy);
	}

	/** Corners a, b, c, d: a to b is the texture's v, b to c its u ({@code us} and {@code vs} of the sprite). */
	private static void quad(PoseStack.Pose p, VertexConsumer c, Tex s, int argb, int light, float ax, float ay, float az,
			float bx, float by, float bz, float cx, float cy, float cz, float dx, float dy, float dz, float nx, float ny, float nz,
			float us, float vs) {
		float u0 = s.u(0), u1 = s.u(us), v0 = s.v(0), v1 = s.v(vs);
		vertex(p, c, ax, ay, az, u0, v1, argb, light, nx, ny, nz);
		vertex(p, c, bx, by, bz, u0, v0, argb, light, nx, ny, nz);
		vertex(p, c, cx, cy, cz, u1, v0, argb, light, nx, ny, nz);
		vertex(p, c, dx, dy, dz, u1, v1, argb, light, nx, ny, nz);
	}

	private static void vertex(PoseStack.Pose p, VertexConsumer c, float x, float y, float z, float u, float v, int argb, int light, float nx,
			float ny, float nz) {
		c.addVertex(p, x, y, z).setColor(argb).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, nx, ny, nz);
	}
}
