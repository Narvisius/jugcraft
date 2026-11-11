package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jimbozoomer.jugcraft.drone.DepotDisplayBlockEntity;
import io.github.jimbozoomer.jugcraft.tower.ConsoleDeskBlock;
import io.github.jimbozoomer.jugcraft.tower.FurnitureBlock;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The Console Desk's monitor (Tri-Pillar work list item 14): its role's page for where it stands in its row (the left
 * end the first, the middle the second, the right end the third: tower/ConsoleDeskBlock#page), drawn on its glass, turned
 * in with the monitor at the row's ends (DeskPages, from DeskData: live from the tower terminal it is pinned to). The
 * page's shapes go on in layers just off the glass (DeskScreen), its text over them, all full bright; from further off
 * the smaller print is left out, and nothing is drawn to anyone behind the monitor.
 *
 * <p>The depot terminal's own monitor (client/DroneDepotRenderer) still shows the seven-line readout this desk had
 * before: {@link #populate}.
 */
public final class ConsoleDeskRenderer implements BlockEntityRenderer<DepotDisplayBlockEntity, ConsoleDeskRenderer.State> {
	private static final float PIXELS = 144;
	/** The page's shapes: the depot texture's white, coloured, glowing and see-through where they are. */
	private static final RenderType SCREEN = DeskScreen.SCREEN;
	/** Screen units to blocks across and down the glass (it is 13.6 by 8.25 px). */
	static final float KX = (ConsoleDeskBlock.GLASS[2] - ConsoleDeskBlock.GLASS[0]) / 16 / DeskScreen.W,
			KY = (ConsoleDeskBlock.GLASS[3] - ConsoleDeskBlock.GLASS[1]) / 16 / DeskScreen.H;
	/** How far off the glass (blocks) the page's background goes, each layer above it, and the text over them all. */
	static final float LIFT = 0.0006F, TEXT_LIFT = LIFT * (DeskScreen.MAX_LAYER + 2);
	private final Font font;
	/** Each desk's page as last drawn, and when: a desk near the camera redraws every frame, the rest five times a second. */
	private static final java.util.Map<DepotDisplayBlockEntity, Drawn> DRAWN = new java.util.WeakHashMap<>();
	/** Logged once: a page threw. */
	private static boolean failed;

	private static final class Drawn {
		DeskScreen screen;
		long tick = Long.MIN_VALUE;
		int role = -1, page = -1, detail = -1;
	}

	public static final class State extends BlockEntityRenderState {
		Direction facing = Direction.NORTH;
		/** The depot terminal's monitor: its lines. */
		final List<String> lines = new ArrayList<>();
		boolean warning;
		/** A desk: its role, which of its pages, its monitor's turn, whether the camera is in front of it. */
		boolean desk, visible;
		int role, page;
		float angle;
		/** The layers' spacing off the glass, wider from further off (the depth buffer's steps grow with the distance squared). */
		float liftScale = 1;
		float[] quads = new float[0];
		int quadFloats;
		final List<DeskScreen.Text> texts = new ArrayList<>();
	}

	public ConsoleDeskRenderer(BlockEntityRendererProvider.Context context) {
		this(context.font());
	}

	ConsoleDeskRenderer(Font font) {
		this.font = font;
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public int getViewDistance() {
		return 48;
	}

	@Override
	public void extractRenderState(DepotDisplayBlockEntity desk, State state, float partialTick, Vec3 cameraPos,
			ModelFeatureRenderer.CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(desk, state, crumbling);
		state.lines.clear();
		state.warning = false;
		state.texts.clear();
		state.quadFloats = 0;
		BlockState block = desk.getBlockState();
		state.facing = block.getValue(FurnitureBlock.FACING);
		state.desk = block.getBlock() instanceof ConsoleDeskBlock;
		Level level = desk.getLevel();
		if (!state.desk || level == null) {
			return;
		}
		state.role = block.getValue(ConsoleDeskBlock.ROLE);
		state.page = ConsoleDeskBlock.page(block);
		state.angle = ConsoleDeskBlock.angle(block.getValue(ConsoleDeskBlock.LEFT), block.getValue(ConsoleDeskBlock.RIGHT));
		// the glass faces +z on the desk facing north, turned with the desk and then with the monitor
		double turn = facingTurn(state.facing) + Math.toRadians(state.angle);
		double nx = Math.sin(turn), nz = Math.cos(turn);
		Vec3 middle = Vec3.atBottomCenterOf(desk.getBlockPos()).add(0, 1.2, 0);
		double distance = cameraPos == null ? 0 : cameraPos.distanceTo(middle);
		state.visible = cameraPos == null || (cameraPos.x - middle.x) * nx + (cameraPos.z - middle.z) * nz > -0.15;
		if (!state.visible) {
			return;
		}
		int detail = distance < 6 ? 0 : distance < 14 ? 1 : distance < 30 ? 2 : 3;
		state.liftScale = (float) Math.max(1, Math.min(8, (distance / 18) * (distance / 18)));
		double now = level.getGameTime() + partialTick;
		Drawn drawn = DRAWN.computeIfAbsent(desk, k -> new Drawn());
		if (drawn.screen == null || drawn.detail != detail || drawn.role != state.role || drawn.page != state.page
				|| detail == 0 || level.getGameTime() - drawn.tick >= 4) {
			var link = desk.terminal();
			if (drawn.screen == null || drawn.detail != detail) {
				drawn.screen = new DeskScreen(font, detail);
			}
			drawn.screen.clear();
			try {
				DeskData data = link == null ? null : DeskData.get(level, link, now);
				DeskPages.draw(drawn.screen, state.role, state.page, data);
			} catch (RuntimeException e) {
				// a page that cannot be drawn says so on the screen rather than taking the frame down with it
				if (!failed) {
					failed = true;
					io.github.jimbozoomer.jugcraft.Jugcraft.LOGGER.error("Console Desk page {} of role {} failed", state.page + 1, state.role, e);
				}
				drawn.screen.clear();
				drawn.screen.background(DeskScreen.BG);
				drawn.screen.text(DeskScreen.W / 2, 60, "PAGE ERROR", DeskScreen.RED, 1.3, "center");
			}
			drawn.tick = level.getGameTime();
			drawn.role = state.role;
			drawn.page = state.page;
			drawn.detail = detail;
		}
		state.quads = drawn.screen.quads;
		state.quadFloats = drawn.screen.size;
		state.texts.addAll(drawn.screen.texts);
	}

	/** The desk's turn from facing north (radians about +y), as its blockstate turns its model. */
	static double facingTurn(Direction facing) {
		return switch (facing) {
			case EAST -> -Math.PI / 2;
			case SOUTH -> Math.PI;
			case WEST -> Math.PI / 2;
			default -> 0;
		};
	}

	/** The depot terminal's seven lines (client/DroneDepotRenderer draws them on its own monitor). */
	static void populate(State state, io.github.jimbozoomer.jugcraft.drone.@org.jspecify.annotations.Nullable DepotView view, double now) {
		state.lines.clear(); state.warning = false;
		if (view == null) { state.lines.add("DRONE OPERATIONS"); state.lines.add("SYNCHRONIZING..."); return; }
		int total = view.docks.size() + view.flights.size();
		state.lines.add("T" + view.towerTier + "  FLIGHT OPERATIONS");
		state.lines.add("FLEET " + total + "/" + view.maxDrones);
		state.lines.add("ACTIVE " + view.active() + "  AIR " + view.airborne(now));
		state.lines.add("BATTERY " + (view.capacity <= 0 ? 0 : view.energy * 100 / view.capacity) + "%");
		state.lines.add("DRAW " + view.draw() + " JE/t " + view.loadPercent() + "%");
		state.lines.add("PADS " + view.padCount() + "  FREE " + Math.max(0, view.maxDrones - total));
		state.warning = view.pickups.isEmpty() || !view.packager || !view.missing.isEmpty();
		state.lines.add(view.pickups.isEmpty() ? "NO PICKUP" : !view.packager ? "NO PACKAGER"
				: !view.missing.isEmpty() ? "SUPPLY SHORT " + view.missing.size() : "SUPPLY OK  JOBS " + view.openTargets);
	}

	@Override
	public void submit(State state, PoseStack poses, SubmitNodeCollector collector, CameraRenderState camera) {
		if (!state.desk) {
			lines(state, poses, collector);
			return;
		}
		if (!state.visible || state.quadFloats == 0) {
			return;
		}
		poses.pushPose();
		poses.translate(.5, 0, .5);
		poses.mulPose(new org.joml.Matrix4f().rotationY((float) facingTurn(state.facing)));
		poses.translate(-.5, 0, -.5);
		float[] pivot = ConsoleDeskBlock.PIVOT, glass = ConsoleDeskBlock.GLASS;
		poses.translate(pivot[0] / 16, 0, pivot[2] / 16);
		poses.mulPose(new org.joml.Matrix4f().rotationY((float) Math.toRadians(state.angle)));
		poses.translate(-pivot[0] / 16, 0, -pivot[2] / 16);
		// the glass's top left as the operator sees it: x across, y down the glass, z out of it
		poses.translate(glass[0] / 16, glass[3] / 16, glass[4] / 16);
		float[] q = state.quads;
		int floats = state.quadFloats;
		float lift = LIFT * state.liftScale;
		float[] white = DroneModel.WHITE;
		float u = (white[0] + white[2]) / 2, v = (white[1] + white[3]) / 2;
		collector.submitCustomGeometry(poses, SCREEN, (pose, consumer) -> {
			float[] up = DeskScreen.litNormal(pose);
			for (int o = 0; o + DeskScreen.STRIDE <= floats; o += DeskScreen.STRIDE) {
				int colour = Float.floatToRawIntBits(q[o + 8]);
				float z = lift * (q[o + 9] + 1);
				for (int k = 0; k < 4; k++) {
					consumer.addVertex(pose, q[o + 2 * k] * KX, -q[o + 2 * k + 1] * KY, z).setColor(colour).setUv(u, v)
							.setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightCoordsUtil.FULL_BRIGHT).setNormal(pose, up[0], up[1], up[2]);
				}
			}
		});
		for (DeskScreen.Text t : state.texts) {
			float width = font.width(t.s()) * t.scale();
			float x = t.x() - (t.align() == 2 ? width : t.align() == 1 ? width / 2 : 0);
			poses.pushPose();
			poses.translate(x * KX, -t.y() * KY, TEXT_LIFT * state.liftScale);
			poses.scale(KX * t.scale(), -KY * t.scale(), KX * t.scale());
			collector.submitText(poses, 0, 0, Texts.of(t.s()), false, Font.DisplayMode.POLYGON_OFFSET,
					LightCoordsUtil.FULL_BRIGHT, t.colour(), 0, 0);
			poses.popPose();
		}
		poses.popPose();
	}

	/** The depot terminal's monitor: its seven lines. */
	private void lines(State state, PoseStack poses, SubmitNodeCollector collector) {
		if (state.lines.isEmpty()) return;
		poses.pushPose();
		poses.translate(.5, 0, .5);
		poses.mulPose(new org.joml.Matrix4f().rotationY((float) facingTurn(state.facing)));
		poses.translate(-.5 + 1.4 / 16, 24.1 / 16, -.5 + 3.85 / 16);
		poses.scale(1 / PIXELS, -1 / PIXELS, 1 / PIXELS);
		for (int i = 0; i < state.lines.size(); i++) {
			String line = font.plainSubstrByWidth(state.lines.get(i), 119);
			int color = i == 0 ? 0xFFFFBA74 : state.warning && i == state.lines.size() - 1 ? 0xFFFF895B : 0xFFC3DBDC;
			collector.submitText(poses, 0, i * 10, Texts.of(line), false,
					Font.DisplayMode.POLYGON_OFFSET, LightCoordsUtil.FULL_BRIGHT, color, 0, 0);
		}
		poses.popPose();
	}

	/** For the tests: the page a desk shows now, as drawn (its text). */
	static List<DeskScreen.Text> page(State state) {
		return state.texts;
	}

}
