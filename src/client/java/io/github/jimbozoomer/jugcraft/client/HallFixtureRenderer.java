package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jimbozoomer.jugcraft.drone.DepotDisplayBlockEntity;
import io.github.jimbozoomer.jugcraft.drone.DepotView;
import io.github.jimbozoomer.jugcraft.drone.DroneTerminalBlockEntity;
import io.github.jimbozoomer.jugcraft.tower.HallFixtureBlock;
import io.github.jimbozoomer.jugcraft.tower.JugcraftTower;
import io.github.jimbozoomer.jugcraft.tower.TowerCoreBlockEntity;
import io.github.jimbozoomer.jugcraft.tower.TowerData;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * The Tri-Pillar hall's live fittings (tower/HallFixtureBlock; models: tools/tripillar_fixtures.py), from their tower's
 * depot view: the touch consoles' screens, the info totem's two screens, the floating FLEET / TOWER / SUPPLY panels over
 * their floor emitters, the drones on the display plinths (on a slow turntable) and on the service bay's scissor-lift
 * cradle, and the service bay's 6-axis robot arm working over it (client/ServiceArm).
 */
public final class HallFixtureRenderer implements BlockEntityRenderer<DepotDisplayBlockEntity, HallFixtureRenderer.State> {
	private static final RenderType HOLO = GlowTypes.glow(DroneDepotRenderer.TEXTURE);
	private static final RenderType SOLID = RenderTypes.entityCutout(DroneDepotRenderer.TEXTURE);
	private static final int AMBER = 0xFFFFBA74, TEXT = 0xFFC3DBDC, WARN = 0xFFFF895B, DIM = 0xFF7F959A;
	/** The touch console's screen plane (tools/tripillar_fixtures.py TOUCH_PIVOT, TOUCH_TILT). */
	private static final float TOUCH_PIVOT_Y = 12 / 16f, TOUCH_PIVOT_Z = 8 / 16f, TOUCH_TILT = (float) Math.toRadians(22.5);

	enum Kind { TOUCH, TOTEM, EMITTER, STAND, ARM }

	public static final class State extends BlockEntityRenderState {
		Kind kind = Kind.TOUCH;
		Direction facing = Direction.NORTH;
		HallFixtureBlock.Panel panel = HallFixtureBlock.Panel.FLEET;
		HallFixtureBlock.Style style = HallFixtureBlock.Style.DISPLAY;
		int tier = 1;
		final List<String> lines = new ArrayList<>();
		final List<Integer> colors = new ArrayList<>();
		float time;
		/** The service arm's own running time (seconds; it stops while a player is in its keep-clear square). */
		double armTime;
		int light = LightCoordsUtil.FULL_BRIGHT;
		float progress = -1;
	}

	private final Font font;

	public HallFixtureRenderer(BlockEntityRendererProvider.Context context) {
		this.font = context.font();
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public int getViewDistance() {
		return 64;
	}

	/** The service bay's cradle and arm reach well out of their cells. */
	@Override
	public boolean shouldRenderOffScreen() {
		return true;
	}

	@Override
	public void extractRenderState(DepotDisplayBlockEntity fixture, State state, float partialTick, Vec3 cameraPos,
			ModelFeatureRenderer.CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(fixture, state, crumbling);
		state.lines.clear();
		state.colors.clear();
		state.progress = -1;
		BlockState block = fixture.getBlockState();
		Block b = block.getBlock();
		state.facing = block.getValue(HallFixtureBlock.FACING);
		state.kind = b == JugcraftTower.BLOCKS.get("touch_console") ? Kind.TOUCH : b == JugcraftTower.BLOCKS.get("tower_info_totem") ? Kind.TOTEM
				: b == JugcraftTower.BLOCKS.get("holo_emitter") ? Kind.EMITTER : b == JugcraftTower.BLOCKS.get("drone_display_stand") ? Kind.STAND
				: Kind.ARM;
		if (state.kind == Kind.EMITTER) {
			state.panel = block.getValue(HallFixtureBlock.Emitter.PANEL);
		}
		if (state.kind == Kind.STAND) {
			state.tier = block.getValue(HallFixtureBlock.Stand.TIER);
			state.style = block.getValue(HallFixtureBlock.Stand.STYLE);
		}
		Level level = fixture.getLevel();
		if (level == null) {
			return;
		}
		state.time = level.getGameTime() + partialTick;
		state.light = LightCoordsUtil.getLightCoords(level, fixture.getBlockPos().above());
		if (state.kind == Kind.ARM) {
			state.armTime = ServiceArm.clock(fixture, level, fixture.getBlockPos(), level.getGameTime() + partialTick);
		}
		if (state.kind == Kind.STAND || state.kind == Kind.ARM) {
			return;
		}
		DepotView view = fixture.terminal() != null && level.getBlockEntity(fixture.terminal()) instanceof DroneTerminalBlockEntity t ? t.view() : null;
		TowerCoreBlockEntity core = view != null && view.towerPos != null && level.getBlockEntity(view.towerPos) instanceof TowerCoreBlockEntity c ? c : null;
		if (view == null) {
			line(state, "DRONE TOWER", AMBER);
			line(state, fixture.terminal() == null ? "NO TERMINAL LINK" : "SYNCHRONIZING...", DIM);
			return;
		}
		switch (state.kind) {
			case TOUCH -> touch(state, view, core);
			case TOTEM -> totem(state, view, core);
			default -> panel(state, view, core);
		}
	}

	private static void line(State state, String text, int color) {
		state.lines.add(text);
		state.colors.add(color);
	}

	private static int docked(DepotView view, int tier) {
		int n = 0;
		for (DepotView.DockView d : view.docks) {
			if (tier == 0 || d.tier() == tier) n++;
		}
		return n;
	}

	private static int out(DepotView view, int tier) {
		int n = 0;
		for (DepotView.FlightView f : view.flights) {
			if (tier == 0 || f.tier() == tier) n++;
		}
		return n;
	}

	private static String tierName(int tier) {
		return Component.translatable("tower.jugcraft.tier." + tier).getString().toUpperCase(Locale.ROOT);
	}

	private static void building(State state, TowerCoreBlockEntity core) {
		if (core == null) {
			return;
		}
		if (core.converting() || core.buildingFoundation() || core.building() > 0) {
			int pct = Math.round(core.buildProgress() * 100);
			line(state, (core.converting() ? "REBUILDING " : core.buildingFoundation() ? "FOUNDATION " : "BUILDING T" + core.building() + " ") + pct + "%", AMBER);
			state.progress = core.buildProgress();
		} else if (core.nextStep() != null) {
			TowerData.Tier next = core.nextStep();
			line(state, core.needsFoundation() ? "NEXT: FOUNDATION" : "NEXT: T" + next.number + " " + tierName(next.number), DIM);
		} else {
			line(state, "TOWER COMPLETE", DIM);
		}
	}

	private static void touch(State state, DepotView view, TowerCoreBlockEntity core) {
		line(state, "TRI-PILLAR  T" + view.towerTier, AMBER);
		line(state, "FLEET " + (view.docks.size() + view.flights.size()) + "/" + view.maxDrones, TEXT);
		line(state, "OUT " + view.flights.size() + "  ACTIVE " + view.active(), TEXT);
		line(state, "BATTERY " + (view.capacity <= 0 ? 0 : view.energy * 100 / view.capacity) + "%", TEXT);
		line(state, view.missing.isEmpty() ? "SUPPLY OK  JOBS " + view.openTargets : "SUPPLY SHORT " + view.missing.size(),
				view.missing.isEmpty() ? TEXT : WARN);
		building(state, core);
		line(state, "TOUCH: DEPOT SCREEN", DIM);
	}

	private static void totem(State state, DepotView view, TowerCoreBlockEntity core) {
		line(state, "DRONE TOWER", AMBER);
		line(state, "TRI-PILLAR", TEXT);
		line(state, "", TEXT);
		line(state, "TIER " + view.towerTier, AMBER);
		line(state, view.towerTier >= 1 ? tierName(view.towerTier) : "FOUNDATION", TEXT);
		line(state, "", TEXT);
		line(state, "FLEET " + (view.docks.size() + view.flights.size()) + "/" + view.maxDrones, TEXT);
		line(state, "DOCKED " + view.docks.size(), TEXT);
		line(state, "OUT " + view.flights.size(), view.flights.isEmpty() ? TEXT : AMBER);
		line(state, "BATTERY " + (view.capacity <= 0 ? 0 : view.energy * 100 / view.capacity) + "%", view.lowPower ? WARN : TEXT);
		line(state, "", TEXT);
		building(state, core);
	}

	private static void panel(State state, DepotView view, TowerCoreBlockEntity core) {
		switch (state.panel) {
			case FLEET -> {
				line(state, "FLEET", AMBER);
				for (int t = 1; t <= Math.max(1, view.towerTier); t++) {
					int home = docked(view, t), away = out(view, t);
					line(state, "T" + t + "  " + home + " DOCKED  " + away + " OUT", away > 0 ? AMBER : TEXT);
				}
				line(state, "TOTAL " + (view.docks.size() + view.flights.size()) + "/" + view.maxDrones, TEXT);
			}
			case TOWER -> {
				line(state, "TOWER", AMBER);
				line(state, "TIER " + view.towerTier + (view.towerTier >= 1 ? "  " + tierName(view.towerTier) : ""), TEXT);
				line(state, "BAYS " + view.doors.size() + "  DRONES UP TO T" + view.maxTier, TEXT);
				line(state, "DRAW " + view.draw() + " JE/t  " + view.loadPercent() + "%", TEXT);
				building(state, core);
			}
			case SUPPLY -> {
				line(state, "SUPPLY", AMBER);
				line(state, "PICKUPS " + view.pickups.size() + (view.packager ? "  PACKAGER OK" : "  NO PACKAGER"), view.packager ? TEXT : WARN);
				if (view.cargo != null) {
					line(state, "STORE " + view.cargo.occupiedSlots() + "/" + view.cargo.totalSlots() + " SLOTS  " + view.cargo.items() + " ITEMS", TEXT);
				}
				line(state, "JOBS OPEN " + view.openTargets, TEXT);
				int shown = 0;
				for (var e : view.missing.entrySet()) {
					if (shown++ >= 3) break;
					line(state, "SHORT " + e.getValue() + " " + e.getKey().toUpperCase(Locale.ROOT), WARN);
				}
				if (view.missing.isEmpty()) line(state, "NOTHING SHORT", TEXT);
			}
		}
	}

	// ------------------------------------------------------------------ drawing

	private static float facingYaw(Direction facing) {
		return switch (facing) {
			case EAST -> -(float) Math.PI / 2;
			case SOUTH -> (float) Math.PI;
			case WEST -> (float) Math.PI / 2;
			default -> 0;
		};
	}

	@Override
	public void submit(State state, PoseStack poses, SubmitNodeCollector collector, CameraRenderState camera) {
		switch (state.kind) {
			case TOUCH -> touchScreen(state, poses, collector);
			case TOTEM -> totemScreens(state, poses, collector);
			case EMITTER -> floatingPanel(state, poses, collector);
			case STAND -> stand(state, poses, collector);
			case ARM -> ServiceArm.submit(poses, collector, state.facing, state.armTime, state.light);
		}
	}

	/** Model space as the blockstate turns it: (0, 0, 0)..(1, 1, 1) with the piece facing north. */
	private static void turn(PoseStack poses, Direction facing) {
		poses.translate(.5, 0, .5);
		poses.mulPose(new Matrix4f().rotationY(facingYaw(facing)));
		poses.translate(-.5, 0, -.5);
	}

	private void text(State state, PoseStack poses, SubmitNodeCollector collector, float pixels, int width, int lineHeight) {
		poses.scale(1 / pixels, -1 / pixels, 1 / pixels);
		for (int i = 0; i < state.lines.size(); i++) {
			String s = font.plainSubstrByWidth(state.lines.get(i), width);
			collector.submitText(poses, 0, i * lineHeight, Texts.of(s), false, Font.DisplayMode.POLYGON_OFFSET,
					LightCoordsUtil.FULL_BRIGHT, state.colors.get(i), 0, 0);
		}
	}

	/** The touch console's screen, on its head's plane tilted back towards the user. */
	private void touchScreen(State state, PoseStack poses, SubmitNodeCollector collector) {
		if (state.lines.isEmpty()) return;
		poses.pushPose();
		turn(poses, state.facing);
		poses.translate(0, TOUCH_PIVOT_Y, TOUCH_PIVOT_Z);
		poses.mulPose(new Matrix4f().rotationX(TOUCH_TILT));
		poses.translate(0, -TOUCH_PIVOT_Y, -TOUCH_PIVOT_Z);
		// the screen's top surface (y 13.16 px) from x 2.9 px, its far edge at z 3.9 px; text reads down the slope
		poses.translate(2.9 / 16, 13.17 / 16, 3.9 / 16);
		poses.mulPose(new Matrix4f().rotationX((float) -Math.PI / 2));
		text(state, poses, collector, 190, 128, 11);
		poses.popPose();
	}

	/** The totem's portrait screens, front (south) and back (north). */
	private void totemScreens(State state, PoseStack poses, SubmitNodeCollector collector) {
		if (state.lines.isEmpty()) return;
		for (int side = 0; side < 2; side++) {
			poses.pushPose();
			turn(poses, side == 0 ? state.facing : state.facing.getOpposite());
			poses.translate(3.9 / 16, 28.6 / 16, 10.12 / 16);
			text(state, poses, collector, 200, 104, 14);
			poses.popPose();
		}
		if (state.progress >= 0) {
			for (int side = 0; side < 2; side++) {
				float p = state.progress;
				poses.pushPose();
				turn(poses, side == 0 ? state.facing : state.facing.getOpposite());
				collector.submitCustomGeometry(poses, HOLO, (pose, consumer) -> {
					DroneDepotRenderer.Geometry g = new DroneDepotRenderer.Geometry(pose, consumer);
					g.light = LightCoordsUtil.FULL_BRIGHT;
					g.color = 0x80403020;
					g.box(3.9 / 16, 6 / 16f, 10.1 / 16, 12.1 / 16, 6.6 / 16, 10.15 / 16, DroneModel.WHITE);
					g.color = 0xE0FFAA3A;
					g.box(3.9 / 16, 6 / 16f, 10.1 / 16, (3.9 + 8.2 * p) / 16, 6.6 / 16, 10.16 / 16, DroneModel.WHITE);
				});
				poses.popPose();
			}
		}
	}

	/**
	 * A floating holo panel over its emitter: a light cone up from the lens, a translucent amber sheet 2.6 x 1.5 blocks
	 * with a brighter frame and a slow scan line, the live readout in front of it, facing the operators.
	 */
	private void floatingPanel(State state, PoseStack poses, SubmitNodeCollector collector) {
		float w = 1.3f, bottom = 1.5f, top = 3.0f, flicker = 0.92f + 0.08f * (float) Math.sin(state.time * 0.37);
		float scan = bottom + (top - bottom) * ((state.time % 60) / 60f);
		poses.pushPose();
		turn(poses, state.facing);
		collector.submitCustomGeometry(poses, HOLO, (pose, consumer) -> {
			DroneDepotRenderer.Geometry g = new DroneDepotRenderer.Geometry(pose, consumer);
			g.light = LightCoordsUtil.FULL_BRIGHT;
			int sheet = ((int) (0x34 * flicker) << 24) | 0xFFA040, edge = ((int) (0xB0 * flicker) << 24) | 0xFFB050;
			float z0 = .5f - .01f, z1 = .5f + .01f;
			g.color = sheet;
			g.box(.5 - w, bottom, z0, .5 + w, top, z1, DroneModel.WHITE);
			g.color = edge;
			g.box(.5 - w, top - .03, z0 - .002, .5 + w, top, z1 + .002, DroneModel.WHITE);
			g.box(.5 - w, bottom, z0 - .002, .5 + w, bottom + .03, z1 + .002, DroneModel.WHITE);
			g.box(.5 - w, bottom, z0 - .002, .5 - w + .03, top, z1 + .002, DroneModel.WHITE);
			g.box(.5 + w - .03, bottom, z0 - .002, .5 + w, top, z1 + .002, DroneModel.WHITE);
			g.color = 0x40FFC070;
			g.box(.5 - w, scan, z0 - .003, .5 + w, scan + .015, z1 + .003, DroneModel.WHITE);
			// the light cone: thin rays from the lens to the sheet's bottom corners
			g.color = 0x22FFA040;
			for (float x : new float[] {-w + .1f, -w / 2, 0, w / 2, w - .1f}) {
				float steps = 8;
				for (int i = 0; i < steps; i++) {
					float t0 = i / steps, t1 = (i + 1) / steps;
					float xa = .5f + x * t0, xb = .5f + x * t1, ya = .125f + (bottom - .125f) * t0, yb = .125f + (bottom - .125f) * t1;
					g.box(Math.min(xa, xb) - .006, ya, .494, Math.max(xa, xb) + .006, yb, .506, DroneModel.WHITE);
				}
			}
		});
		if (!state.lines.isEmpty()) {
			poses.pushPose();
			poses.translate(.5 - w + .1, top - .1, .5 + .02);
			text(state, poses, collector, 64, 156, 11);
			poses.popPose();
		}
		poses.popPose();
	}

	/**
	 * A drone stand: under glass the drone stands on a turntable that turns once a minute; in the service bay it stands on
	 * a scissor-lift cradle raised a block off the pad (base frame, two pairs of scissor arms, the deck).
	 */
	private void stand(State state, PoseStack poses, SubmitNodeCollector collector) {
		boolean service = state.style == HallFixtureBlock.Style.SERVICE;
		float deck = service ? 1.15f : 2.25f / 16;
		// the drone's nose towards the stand's front (where its visitors stand); under glass it turns once a minute
		float yaw = facingYaw(state.facing) + (service ? 0 : (float) (state.time / 1200.0 * Math.PI * 2));
		int light = state.light;
		if (service) {
			collector.submitCustomGeometry(poses, SOLID, (pose, consumer) -> {
				DroneDepotRenderer.Geometry g = new DroneDepotRenderer.Geometry(pose, consumer);
				g.light = light;
				float half = 1.1f, len = 1.25f;
				g.color = 0xFF2A2E33;
				g.box(.5 - half, 0, .5 - len, .5 + half, .12, .5 + len, DroneModel.WHITE);
				g.box(.5 - half, deck - .1, .5 - len, .5 + half, deck, .5 + len, DroneModel.WHITE);
				g.color = 0xFFC98A3A;
				g.box(.5 - half - .01, deck - .1, .5 - len - .01, .5 + half + .01, deck - .06, .5 + len + .01, DroneModel.WHITE);
				// scissor arms on both long sides: two bars crossing at the middle
				float rise = deck - .2f, run = 2 * len - .3f, angle = (float) Math.atan2(rise, run);
				float bar = (float) Math.hypot(rise, run);
				g.color = 0xFF4A5056;
				for (float side : new float[] {-half + .12f, half - .2f}) {
					for (int sense = -1; sense <= 1; sense += 2) {
						g.push();
						g.translate(.5 + side, .12 + rise / 2, .5).pitch(sense * angle);
						g.box(0, -.04, -bar / 2, .08, .04, bar / 2, DroneModel.WHITE);
						g.pop();
					}
				}
				g.color = 0xFF1A1C1F;
				g.box(.5 - half + .1, .12 + rise / 2 - .05, .45, .5 + half - .1, .12 + rise / 2 + .05, .55, DroneModel.WHITE);
				g.color = 0xFFFFFFFF;
			});
		}
		int tier = state.tier;
		// on the cradle the drone's plan is centred on it (the service arm's stops are the T4's fans as it lies so,
		// client/ServiceArm); on a plinth it turns about its own origin, as it always has
		float x = .5f, z = .5f;
		if (service) {
			float[] mid = DroneMesh.middle(tier);
			float c = (float) Math.cos(yaw), s = (float) Math.sin(yaw);
			x -= mid[0] * c + mid[1] * s;
			z -= -mid[0] * s + mid[1] * c;
		}
		float dx = x, dz = z;
		Retained.mark(collector);                        // a drone on a stand is kept on the card (Retained)
		collector.submitCustomGeometry(poses, DroneMesh.RENDER_TYPE, (pose, consumer) -> {
			DroneDepotRenderer.Geometry g = new DroneDepotRenderer.Geometry(pose, consumer, DroneMesh.RENDER_TYPE);
			DroneModel.draw(g, dx, deck, dz, yaw, tier, -1, 0, light, 0);
		});
	}
}
