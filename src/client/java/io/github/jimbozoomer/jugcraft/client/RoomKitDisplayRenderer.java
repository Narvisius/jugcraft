package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jimbozoomer.jugcraft.tower.RoomKit;
import io.github.jimbozoomer.jugcraft.tower.RoomKitBlock;
import io.github.jimbozoomer.jugcraft.tower.RoomKitBlockEntity;
import io.github.jimbozoomer.jugcraft.tower.RoomKitDisplayBlock;
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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * The room kits' live displays (tower/RoomKitDisplayBlock).
 *
 * <p>The Ops Wall Screen (3 x 2, or 5 x 3): its role's three pages in turn, eight seconds each, drawn as the console desks
 * draw theirs (DeskPages from DeskData, live from the tower terminal it is pinned to) fitted to its glass inside a thin
 * bezel; role 15, the training room's lesson, the instructor's current slide ({@link #lesson}). Drawn only to someone in
 * front of it, the small print left out from further off.
 *
 * <p>The live faces (Tri-Pillar work list item 22 B2; the manifest's "live", tools/training_kit.live): pictures on the
 * pieces' models the game draws over them, each where the model has it (its top-left, top-right and bottom-left
 * corners in the block), a little off the model's face:
 * <ul>
 * <li>page: a laptop's or monitor's page, as the console desks draw it, from the tower's terminal (close to, a few
 * blocks for a laptop: further off its baked picture, the same page, shows);
 * <li>seg_clock: a seven-segment clock's four figures, the tower's time of day (plus the zone's hours), its colon
 * blinking each second;
 * <li>counter: the bottle filler's count (its baked count plus the bottles it has filled);
 * <li>charge: the floor scrubber's battery gauge, charging each morning after the night's cleaning (05:00 to 11:00),
 * the next bar blinking, then CHARGED;
 * <li>text: a readout (an appliance's or a phone's clock, an instrument's figure, a simulator's height and speed);
 * <li>wave: a trace (an oscilloscope's, a patient monitor's ECG);
 * <li>bar: a gauge's bar (a motor's throttle);
 * <li>progress: a job's per cent and its bar (a 3D printer's print).
 * </ul>
 * LiveFaces works each one out.
 */
public final class RoomKitDisplayRenderer implements BlockEntityRenderer<RoomKitBlockEntity, RoomKitDisplayRenderer.State> {
	private static final RenderType SCREEN = DeskScreen.SCREEN;
	/** The glass's distance in from the block's front (px: the panel is 1.4 px deep against the wall). */
	private static final float GLASS = 1.4F / 16;
	/** The bezel round the picture (blocks), and each page's time on the screen (ticks). */
	private static final float BEZEL = 0.07F;
	private static final int PAGE_TICKS = 160;
	private static final float LIFT = 0.0012F;
	/** A live face's lift off the model's face (blocks): a little over 0.09 px, the least gap between two materials. */
	private static final float FACE_LIFT = 0.1F / 16;
	/**
	 * How far a live face is drawn (blocks): a page only as far as it would keep its larger lines (12 of its widths), the
	 * rest as far as they could still be made out (160 of their sizes: a phone's line a few blocks, a clock across a room).
	 * Further off, the baked picture under it shows; one with nothing baked under it ("far": a simulator's figures, left
	 * out of its view) is drawn as far as any.
	 */
	private static final double LIVE_REACH = 48, PAGE_REACH = 12;
	/** How often a live page is drawn again (ticks; staggered). */
	private static final int PAGE_REFRESH = 20;
	private final Font font;
	private static final java.util.Map<RoomKitBlockEntity, Drawn> DRAWN = new java.util.WeakHashMap<>();
	private static final java.util.Map<RoomKitBlockEntity, LiveFaces.Picture[]> PICTURES = new java.util.WeakHashMap<>();
	private static boolean failed;

	private static final class Drawn {
		DeskScreen screen;
		long tick = Long.MIN_VALUE;
		int role = -1, page = -1, detail = -1;
	}

	/** One live face to draw this frame: its frame in the block (x across, y up, z out of it), how its picture sits in it. */
	static final class Face {
		final Matrix4f basis = new Matrix4f();
		float k, ox, oy, lift, layerLift;
		/** Its text drawn with the game's drop shadow (a HUD's, over the view). */
		boolean shadow;
		float[] quads;
		int floats;
		final List<DeskScreen.Text> texts = new ArrayList<>();
	}

	public static final class State extends BlockEntityRenderState {
		Direction facing = Direction.NORTH;
		boolean visible;
		int w, h;
		float liftScale = 1;
		float[] quads = new float[0];
		int quadFloats;
		final List<DeskScreen.Text> texts = new ArrayList<>();
		final List<Face> faces = new ArrayList<>();
		int faceCount;
		/** A goods lift door's leaves, indicator and car (client/GoodsLiftRenderer). */
		final GoodsLiftRenderer.Lift lift = new GoodsLiftRenderer.Lift();
		/** A Tower Diorama Case's model (client/TowerDiorama): drawn, the camera in its own space, the light in the case. */
		boolean diorama;
		double camX, camY, camZ;
		int dioramaLight;
		BlockPos dioramaAt = BlockPos.ZERO;

		Face face() {
			if (faceCount == faces.size()) {
				faces.add(new Face());
			}
			Face f = faces.get(faceCount++);
			f.texts.clear();
			return f;
		}
	}

	public RoomKitDisplayRenderer(BlockEntityRendererProvider.Context context) {
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

	/** A wall screen reaches up to 5 blocks across and 3 down from its anchor. */
	@Override
	public boolean shouldRenderOffScreen() {
		return true;
	}

	@Override
	public void extractRenderState(RoomKitBlockEntity entity, State state, float partialTick, Vec3 cameraPos,
			ModelFeatureRenderer.CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(entity, state, crumbling);
		state.visible = false;
		state.quadFloats = 0;
		state.texts.clear();
		state.faceCount = 0;
		state.lift.active = false;
		state.diorama = false;
		BlockState block = entity.getBlockState();
		Level level = entity.getLevel();
		if (!(block.getBlock() instanceof RoomKitDisplayBlock kit) || level == null) {
			return;
		}
		List<RoomKit.Live> live = kit.live(block);
		if (!live.isEmpty()) {
			try {
				liveFaces(entity, live, level, state, partialTick, cameraPos);
			} catch (RuntimeException e) {
				state.faceCount = 0;
				if (!failed) {
					failed = true;
					io.github.jimbozoomer.jugcraft.Jugcraft.LOGGER.error("A live face of {} failed", kit.spec.id(), e);
				}
			}
		}
		if ("wall_screen".equals(kit.spec.display()) && kit.isAnchor(block)) {
			wallScreen(entity, kit, block, level, state, partialTick, cameraPos);
		}
		if ("goods_lift".equals(kit.spec.display())) {
			GoodsLiftRenderer.extract(entity, block, level, partialTick, font, state.lift);
		}
		if ("diorama".equals(kit.spec.display()) && kit.isAnchor(block) && TowerDiorama.get() != null) {
			// The model stands on the case's tray (tools/diorama_case.py TRAY_TOP), as it stood on the hologram table's top
			BlockPos at = entity.getBlockPos();
			state.diorama = true;
			state.dioramaAt = at.immutable();
			state.camX = cameraPos == null ? 0 : cameraPos.x - at.getX();
			state.camY = cameraPos == null ? 0 : cameraPos.y - at.getY() - DIORAMA_LIFT;
			state.camZ = cameraPos == null ? 0 : cameraPos.z - at.getZ();
			state.dioramaLight = net.minecraft.util.LightCoordsUtil.getLightCoords(level, at.above());
		}
	}

	/** How much higher the diorama stands on the case's tray than on the hologram table's top it was made for (blocks). */
	static final double DIORAMA_LIFT = (14.6 - 13.0) / 16.0;

	private void wallScreen(RoomKitBlockEntity entity, RoomKitDisplayBlock kit, BlockState block, Level level, State state, float partialTick,
			Vec3 cameraPos) {
		state.facing = block.hasProperty(RoomKitBlock.FACING_PROP) ? block.getValue(RoomKitBlock.FACING_PROP) : Direction.NORTH;
		int w = 1, h = 1;
		for (RoomKit.Cell c : kit.spec.cells()) {
			w = Math.max(w, 1 - c.dx());
			h = Math.max(h, 1 - c.dy());
		}
		state.w = w;
		state.h = h;
		// the glass faces the way the screen faces; only someone in front of it sees it
		Vec3 normal = new Vec3(state.facing.getStepX(), 0, state.facing.getStepZ());
		Vec3 middle = Vec3.atCenterOf(entity.getBlockPos());
		double distance = cameraPos == null ? 0 : cameraPos.distanceTo(middle);
		state.visible = cameraPos == null || cameraPos.subtract(middle).dot(normal) > -0.3;
		if (!state.visible) {
			return;
		}
		int detail = distance < 8 ? 0 : distance < 18 ? 1 : distance < 36 ? 2 : 3;
		state.liftScale = (float) Math.max(1, Math.min(8, (distance / 18) * (distance / 18)));
		IntegerProperty roleProp = kit.role();
		int role = roleProp == null ? 0 : block.getValue(roleProp);
		int page = (int) ((level.getGameTime() / PAGE_TICKS) % 3);
		double now = level.getGameTime() + partialTick;
		Drawn drawn = DRAWN.computeIfAbsent(entity, k -> new Drawn());
		if (drawn.screen == null || drawn.detail != detail || drawn.role != role || drawn.page != page || level.getGameTime() - drawn.tick >= 10) {
			if (drawn.screen == null || drawn.detail != detail) {
				drawn.screen = new DeskScreen(font, detail);
			}
			drawn.screen.clear();
			try {
				if (role == RoomKitDisplayBlock.LESSON) {
					lesson(drawn.screen);
				} else {
					var link = entity.terminal();
					DeskData data = link == null ? null : DeskData.get(level, link, now);
					DeskPages.draw(drawn.screen, role, page, data);
				}
			} catch (RuntimeException e) {
				if (!failed) {
					failed = true;
					io.github.jimbozoomer.jugcraft.Jugcraft.LOGGER.error("Ops Wall Screen page {} of role {} failed", page + 1, role, e);
				}
				drawn.screen.clear();
				drawn.screen.background(DeskScreen.BG);
				drawn.screen.text(DeskScreen.W / 2, 60, "PAGE ERROR", DeskScreen.RED, 1.3, "center");
			}
			drawn.tick = level.getGameTime();
			drawn.role = role;
			drawn.page = page;
			drawn.detail = detail;
		}
		state.quads = drawn.screen.quads;
		state.quadFloats = drawn.screen.size;
		state.texts.addAll(drawn.screen.texts);
	}

	// ------------------------------------------------------------------ the live faces

	private void liveFaces(RoomKitBlockEntity entity, List<RoomKit.Live> live, Level level, State state, float partialTick, Vec3 cameraPos) {
		LiveFaces.Picture[] pictures = PICTURES.get(entity);
		if (pictures == null || pictures.length != live.size()) {
			pictures = new LiveFaces.Picture[live.size()];
			PICTURES.put(entity, pictures);
		}
		BlockPos pos = entity.getBlockPos();
		for (int i = 0; i < live.size(); i++) {
			RoomKit.Live f = live.get(i);
			float[] tl = f.tl(), tr = f.tr(), bl = f.bl();
			float ex = tr[0] - tl[0], ey = tr[1] - tl[1], ez = tr[2] - tl[2];
			float dx = bl[0] - tl[0], dy = bl[1] - tl[1], dz = bl[2] - tl[2];
			float w = (float) Math.sqrt(ex * ex + ey * ey + ez * ez), h = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
			if (w < 1e-4F || h < 1e-4F) {
				continue;
			}
			// the face's normal, out towards whoever reads it: down the picture crossed with across it
			float nx = dy * ez - dz * ey, ny = dz * ex - dx * ez, nz = dx * ey - dy * ex;
			float nl = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
			nx /= nl;
			ny /= nl;
			nz /= nl;
			double distance = 0;
			if (cameraPos != null) {
				double rx = cameraPos.x - (pos.getX() + tl[0] + (ex + dx) / 2), ry = cameraPos.y - (pos.getY() + tl[1] + (ey + dy) / 2),
						rz = cameraPos.z - (pos.getZ() + tl[2] + (ez + dz) / 2);
				distance = Math.sqrt(rx * rx + ry * ry + rz * rz);
				boolean page = "page".equals(f.kind());
				// (a TV's picture, big enough to watch across a room, as far as 12 of its widths)
				double reach = page ? f.integer("role", 0) == RoomKitDisplayBlock.TV ? 12 * w : Math.min(PAGE_REACH, 12 * w)
						: f.params().has("far") ? LIVE_REACH : Math.min(LIVE_REACH, 160 * Math.max(w, h));
				if (rx * nx + ry * ny + rz * nz <= 0.01 || distance > reach) {
					continue;
				}
			}
			LiveFaces.Picture picture = pictures[i];
			if (picture == null) {
				picture = pictures[i] = new LiveFaces.Picture();
			}
			Face face = state.face();
			long day = level.getOverworldClockTime(), ticks = level.getGameTime();
			switch (f.kind()) {
				case "page" -> page(entity, f, level, picture, face, w, h, distance);
				case "seg_clock" -> LiveFaces.clock(f, day, ticks, picture, w, h);
				case "counter" -> LiveFaces.counter(font, f, entity.count(), picture, w, h);
				case "charge" -> LiveFaces.charge(font, f, day, ticks, picture, w, h);
				case "text" -> LiveFaces.text(font, f, day, ticks, seed(f, pos, i), picture, w, h);
				case "wave" -> LiveFaces.wave(f, ticks, partialTick, seed(f, pos, i), picture, w, h);
				case "bar" -> LiveFaces.bar(f, ticks, seed(f, pos, i), picture, w, h);
				case "progress" -> LiveFaces.progress(font, f, ticks, seed(f, pos, i), picture, w, h);
				default -> {
					state.faceCount--;
					continue;
				}
			}
			if (!"page".equals(f.kind())) {
				face.k = 1;
				face.ox = face.oy = 0;
				face.quads = picture.quads;
				face.floats = picture.size;
				face.texts.addAll(picture.texts);
			}
			face.shadow = f.params().has("shadow") && f.params().get("shadow").getAsBoolean();
			// x across the picture, y up it, z out of it, from its top-left corner
			face.basis.set(ex / w, ey / w, ez / w, 0, -dx / h, -dy / h, -dz / h, 0, nx, ny, nz, 0, tl[0], tl[1], tl[2], 1);
			float liftScale = (float) Math.max(1, Math.min(8, (distance / 18) * (distance / 18)));
			face.layerLift = LIFT * liftScale;
			face.lift = Math.max(FACE_LIFT, 2 * LIFT * liftScale);
		}
	}

	/**
	 * A face's own seed, so two of a kind side by side don't read alike; or the one it is given ("seed": a simulator's
	 * figures, the same on its pod and on its repeater at the instructor's desk).
	 */
	private static int seed(RoomKit.Live f, BlockPos pos, int face) {
		int given = f.integer("seed", -1);
		return given >= 0 ? given : Math.floorMod(pos.hashCode() * 31 + face, 997);
	}

	/**
	 * A laptop's or monitor's page, as the console desks draw it: fitted to its glass, drawn again every second. The TV's
	 * channel (role {@link RoomKitDisplayBlock#TV}, client/TowerTv) fills its glass across, and is drawn again every few
	 * ticks, so its ticker runs.
	 */
	private void page(RoomKitBlockEntity entity, RoomKit.Live f, Level level, LiveFaces.Picture p, Face face, float w, float h, double distance) {
		// its small print as a wall screen's at the distance the screen would look as big from
		double apparent = distance * 3 / w;
		int detail = apparent < 8 ? 0 : apparent < 18 ? 1 : 2;
		long now = level.getGameTime();
		int role = f.integer("role", 0), page = f.integer("page", 0);
		boolean tv = role == RoomKitDisplayBlock.TV;
		long slot = tv ? Math.floorDiv(now, TowerTv.TICKER_TICKS) : Math.floorDiv(now + (entity.getBlockPos().hashCode() & 0xFF), PAGE_REFRESH);
		if (p.screen == null || p.detail != detail || p.key != slot) {
			if (p.screen == null || p.detail != detail) {
				p.screen = new DeskScreen(font, detail);
			}
			p.screen.clear();
			try {
				if (tv) {
					var link = entity.terminal();
					TowerTv.draw(p.screen, link == null ? null : DeskData.get(level, link, now), now, DeskScreen.W * h / w);
				} else if (role == RoomKitDisplayBlock.LESSON) {
					lesson(p.screen);
				} else {
					var link = entity.terminal();
					DeskPages.draw(p.screen, role, page, link == null ? null : DeskData.get(level, link, now));
				}
			} catch (RuntimeException e) {
				if (!failed) {
					failed = true;
					io.github.jimbozoomer.jugcraft.Jugcraft.LOGGER.error("A live page {} of role {} failed", page + 1, role, e);
				}
				p.screen.clear();
				p.screen.background(DeskScreen.BG);
			}
			p.detail = detail;
			p.key = slot;
		}
		float k = tv ? w / DeskScreen.W : Math.min(w / DeskScreen.W, h / DeskScreen.H);
		face.k = k;
		face.ox = tv ? 0 : (w - DeskScreen.W * k) / 2;
		face.oy = tv ? 0 : (h - DeskScreen.H * k) / 2;
		face.quads = p.screen.quads;
		face.floats = p.screen.size;
		face.texts.addAll(p.screen.texts);
	}

	@Override
	public void submit(State state, PoseStack poses, SubmitNodeCollector collector, CameraRenderState camera) {
		GoodsLiftRenderer.submit(state.lift, poses, collector, font, state.lightCoords);
		if (state.diorama) {
			TowerDiorama d = TowerDiorama.get();
			BlockPos a = state.dioramaAt;
			if (d != null && (camera.cullFrustum == null || camera.cullFrustum.isVisible(new net.minecraft.world.phys.AABB(a.getX() - 1, a.getY(),
					a.getZ() - 1, a.getX() + 2, a.getY() + 4, a.getZ() + 2)))) {
				poses.pushPose();
				poses.translate(0, DIORAMA_LIFT, 0);
				d.submit(poses, collector, state.dioramaLight, state.camX, state.camY, state.camZ);
				poses.popPose();
			}
		}
		for (int i = 0; i < state.faceCount; i++) {
			Face face = state.faces.get(i);
			if (face.floats == 0 && face.texts.isEmpty()) {
				continue;
			}
			poses.pushPose();
			poses.mulPose(face.basis);
			poses.translate(face.ox, -face.oy, 0);
			draw(poses, collector, face.quads, face.floats, face.texts, face.k, face.lift, face.layerLift, face.shadow);
			poses.popPose();
		}
		if (!state.visible || state.quadFloats == 0) {
			return;
		}
		poses.pushPose();
		// the screen facing north has its glass to the north: as a console desk facing south has its own
		poses.translate(.5, 0, .5);
		poses.mulPose(new org.joml.Matrix4f().rotationY((float) ConsoleDeskRenderer.facingTurn(state.facing.getOpposite())));
		poses.translate(-.5, 0, -.5);
		// now x runs to the viewer's right across the parts, y up, z out of the glass; the anchor is the top-left block
		float k = Math.min((state.w - 2 * BEZEL) / DeskScreen.W, (state.h - 2 * BEZEL) / DeskScreen.H);
		float ox = (state.w - DeskScreen.W * k) / 2, top = 1 - (state.h - DeskScreen.H * k) / 2;
		poses.translate(ox, top, GLASS);
		float lift = LIFT * state.liftScale;
		draw(poses, collector, state.quads, state.quadFloats, state.texts, k, lift, lift, false);
		poses.popPose();
	}

	/**
	 * A picture's shapes and text, in its units ({@code k} blocks each) across and down from the pose's origin (x right, y
	 * up, z out): each shape {@code layerLift} above the one under it, the lowest {@code lift} out, the text over them all.
	 */
	private void draw(PoseStack poses, SubmitNodeCollector collector, float[] q, int floats, List<DeskScreen.Text> texts, float k, float lift,
			float layerLift, boolean shadow) {
		float[] white = DroneModel.WHITE;
		float u = (white[0] + white[2]) / 2, v = (white[1] + white[3]) / 2;
		if (floats > 0) {
			collector.submitCustomGeometry(poses, SCREEN, (pose, consumer) -> {
				float[] up = DeskScreen.litNormal(pose);
				for (int o = 0; o + DeskScreen.STRIDE <= floats; o += DeskScreen.STRIDE) {
					int colour = Float.floatToRawIntBits(q[o + 8]);
					float z = lift + layerLift * q[o + 9];
					for (int i = 0; i < 4; i++) {
						consumer.addVertex(pose, q[o + 2 * i] * k, -q[o + 2 * i + 1] * k, z).setColor(colour).setUv(u, v)
								.setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightCoordsUtil.FULL_BRIGHT).setNormal(pose, up[0], up[1], up[2]);
					}
				}
			});
		}
		float textLift = lift + layerLift * (DeskScreen.MAX_LAYER + 1);
		for (DeskScreen.Text t : texts) {
			float width = font.width(t.s()) * t.scale();
			float x = t.x() - (t.align() == 2 ? width : t.align() == 1 ? width / 2 : 0);
			poses.pushPose();
			poses.translate(x * k, -t.y() * k, textLift);
			poses.scale(k * t.scale(), -k * t.scale(), k * t.scale());
			collector.submitText(poses, 0, 0, Texts.of(t.s()), shadow, Font.DisplayMode.POLYGON_OFFSET,
					LightCoordsUtil.FULL_BRIGHT, t.colour(), 0, 0);
			poses.popPose();
		}
	}

	/**
	 * The training room's lesson (tools/class_kit.py lesson_slide): wind round buildings, the instructor's red pen over
	 * it, on the school's white slide.
	 */
	static void lesson(DeskScreen s) {
		float W = DeskScreen.W, H = DeskScreen.H;
		int white = DeskScreen.rgb(248, 248, 246), navy = DeskScreen.rgb(26, 44, 84), ink = DeskScreen.rgb(24, 30, 44),
				blue = DeskScreen.rgb(40, 110, 200), red = DeskScreen.rgb(220, 40, 40), grey = DeskScreen.rgb(150, 156, 166);
		s.background(white);
		s.rect(0, 0, W, H * 0.11, navy);
		s.text(W * 0.02, H * 0.025, "TRI-PILLAR FLIGHT SCHOOL", white, 0.8);
		s.text(W * 0.98, H * 0.025, "MODULE 3  /  WEATHER", DeskScreen.rgb(220, 226, 236), 0.7, "right");
		s.text(W * 0.04, H * 0.15, "LESSON 4: WIND ROUND BUILDINGS", ink, 1.15);
		double gy = H * 0.86;
		s.line(W * 0.04, gy, W * 0.96, gy, DeskScreen.rgb(90, 90, 96), 0.8);
		s.rect(W * 0.36, H * 0.42, W * 0.14, gy - H * 0.42, grey);
		s.frame(W * 0.36, H * 0.42, W * 0.14, gy - H * 0.42, DeskScreen.rgb(80, 84, 92));
		s.rect(W * 0.58, H * 0.6, W * 0.08, gy - H * 0.6, grey);
		s.frame(W * 0.58, H * 0.6, W * 0.08, gy - H * 0.6, DeskScreen.rgb(80, 84, 92));
		for (int i = 0; i < 4; i++) {
			double y = H * (0.36 + i * 0.08);
			s.line(W * 0.06, y, W * 0.32, y, blue, 0.9);
			s.line(W * 0.32, y - 2, W * 0.34, y, blue, 0.9);
			s.line(W * 0.32, y + 2, W * 0.34, y, blue, 0.9);
		}
		s.arc(W * 0.55, H * 0.63, H * 0.17, 200, 520, blue, 0.9);
		s.arc(W * 0.72, H * 0.70, H * 0.14, 200, 520, blue, 0.9);
		s.text(W * 0.07, H * 0.27, "WIND", blue, 0.8);
		s.ring(W * 0.555, H * 0.63, H * 0.2, red, 1.1);
		s.line(W * 0.62, H * 0.5, W * 0.74, H * 0.36, red, 1.1);
		s.text(W * 0.75, H * 0.29, "TURBULENT!", red, 0.9);
		s.text(W * 0.75, H * 0.37, "fly upwind side", red, 0.7);
		String[] lines = {"1. Wind speeds up over roofs", "2. Eddies form downwind", "3. Keep 3x the height away"};
		for (int i = 0; i < lines.length; i++) {
			s.text(W * 0.04, H * (0.6 + i * 0.06), lines[i], ink, 0.62);
		}
		s.rect(W * 0.9, H * 0.9, W * 0.08, H * 0.07, DeskScreen.rgb(230, 232, 236));
		s.text(W * 0.94, H * 0.915, "7 / 18", DeskScreen.rgb(60, 64, 70), 0.55, "center");
	}
}
