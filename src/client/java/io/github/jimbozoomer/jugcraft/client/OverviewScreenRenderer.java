package io.github.jimbozoomer.jugcraft.client;

import static io.github.jimbozoomer.jugcraft.client.NuclearRenderer.v;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.client.NuclearRenderer.Frame;
import io.github.jimbozoomer.jugcraft.client.NuclearRenderer.Mesh;
import io.github.jimbozoomer.jugcraft.nuclear.ControlBoard;
import io.github.jimbozoomer.jugcraft.nuclear.DisplayWall;
import io.github.jimbozoomer.jugcraft.nuclear.MultiFurnitureBlock;
import io.github.jimbozoomer.jugcraft.nuclear.OverviewScreen;
import io.github.jimbozoomer.jugcraft.nuclear.OverviewScreenBlockEntity;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The plant overview screen drawn (round 34; tools/overview_screen.py lays out its frame and pages, nuclear/OverviewScreen
 * works out what they show): from its bottom middle block, the frame (title bar, alarm strip) and the page up (the site
 * map, a building's, or NO PLANT DATA) over the display's black glass, at full brightness, on the screen's own legend
 * texture (textures/entity/overview_screen_legends.png); its live parts by the plant's state (buildings' fills, lines'
 * flow, lamps, bars, the trends' traces, the alarm slots) and its live words in the game's font.
 */
public class OverviewScreenRenderer implements BlockEntityRenderer<OverviewScreenBlockEntity, OverviewScreenRenderer.State> {
	static final Identifier TEXTURE = Jugcraft.id("textures/entity/overview_screen_legends.png");
	static final RenderType TYPE = RenderTypes.entityCutout(TEXTURE);
	/** Each section's fixed picture, built once. */
	private static final Map<String, Mesh> FIXED = new HashMap<>();
	private final Font font;

	public static class State extends BlockEntityRenderState {
		Direction facing = Direction.NORTH;
		OverviewScreenBlockEntity screen;
		double clock;
		long day;
		boolean rain, thunder;
	}

	public OverviewScreenRenderer(BlockEntityRendererProvider.Context context) {
		this.font = context.font();
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public boolean shouldRenderOffScreen() {
		return true;
	}

	@Override
	public void extractRenderState(OverviewScreenBlockEntity screen, State state, float partialTick, Vec3 cameraPos,
			ModelFeatureRenderer.CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(screen, state, crumbling);
		state.facing = screen.getBlockState().getBlock() instanceof MultiFurnitureBlock ? screen.getBlockState().getValue(MultiFurnitureBlock.FACING) : Direction.NORTH;
		state.screen = screen;
		Level level = screen.getLevel();
		state.clock = level == null ? 0 : level.getGameTime() + partialTick;
		net.minecraft.client.multiplayer.ClientLevel world = net.minecraft.client.Minecraft.getInstance().level;
		state.day = world == null ? 6000 : world.getOverworldClockTime();
		state.rain = level != null && level.isRaining();
		state.thunder = level != null && level.isThundering();
	}

	/** The section the screen shows over its frame now. */
	static String page(OverviewScreenBlockEntity s) {
		return !s.linked ? OverviewScreen.section(OverviewScreen.NO_SIGNAL) : OverviewScreen.section(s.page);
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		OverviewScreenBlockEntity s = state.screen;
		JsonObject frame = ControlBoards.section(OverviewScreen.KEY), page = ControlBoards.section(page(s));
		if (s == null || frame == null || page == null) {
			return;
		}
		pose.pushPose();
		// From the bottom middle block to the screen's left column: the sections are drawn from its block.
		Direction right = state.facing.getClockWise();
		pose.translate(-right.getStepX(), 0, -right.getStepZ());
		collector.submitCustomGeometry(pose, TYPE, (p, consumer) -> {
			fixed(OverviewScreen.KEY, frame).replay(p, consumer, state.facing, LightCoordsUtil.FULL_BRIGHT);
			fixed(page(s), page).replay(p, consumer, state.facing, LightCoordsUtil.FULL_BRIGHT);
			live(p, consumer, state, frame.getAsJsonObject("live"));
			live(p, consumer, state, page.getAsJsonObject("live"));
		});
		pose.pushPose();
		pose.translate(0.5F, 0.0F, 0.5F);
		pose.rotateDegrees(Axis.YP, -yRotation(state.facing));
		pose.translate(-0.5F, 0.0F, -0.5F);
		texts(pose, collector, state, frame.getAsJsonObject("live"));
		texts(pose, collector, state, page.getAsJsonObject("live"));
		pose.popPose();
		pose.popPose();
	}

	private static Mesh fixed(String key, JsonObject section) {
		return FIXED.computeIfAbsent(key, k -> {
			Mesh mesh = new Mesh();
			Frame f = new Frame(mesh, true);
			f.legends = true;
			ControlBoards.ops(f, section.getAsJsonArray("still"));
			ControlBoards.ops(f, section.getAsJsonArray("lit"));
			return mesh;
		});
	}

	// ------------------------------------------------------------------ the live parts

	private static void live(PoseStack.Pose pose, VertexConsumer consumer, State state, JsonObject live) {
		OverviewScreenBlockEntity s = state.screen;
		double clock = state.clock;
		Mesh mesh = new Mesh();
		Frame f = new Frame(mesh, true);
		f.legends = true;
		for (String key : new String[] {"lamps", "flows"}) {
			for (JsonElement e : array(live, key)) {
				JsonObject o = e.getAsJsonObject();
				boolean on = OverviewScreen.signal(o.get("s").getAsString(), s, clock);
				JsonArray q = o.getAsJsonArray("q");
				ControlBoards.face(f, corner(q, 0), corner(q, 3), corner(q, 6), corner(q, 9), on ? o.get("on") : o.get("off"));
			}
		}
		for (JsonElement e : array(live, "fills")) {
			JsonObject o = e.getAsJsonObject();
			int st = OverviewScreen.fill(o.get("id").getAsString(), s, clock);
			JsonArray q = o.getAsJsonArray("q"), mats = o.getAsJsonArray("m");
			ControlBoards.face(f, corner(q, 0), corner(q, 3), corner(q, 6), corner(q, 9), mats.get(Math.max(0, Math.min(st, mats.size() - 1))));
		}
		for (JsonElement e : array(live, "levels")) {
			JsonObject o = e.getAsJsonObject();
			double frac = Math.max(0, Math.min(1, (OverviewScreen.value(o.get("v").getAsString(), s, clock) - d(o, "lo")) / (d(o, "hi") - d(o, "lo"))));
			if (frac < 0.002) {
				continue;
			}
			double x0 = d(o, "x0"), y0 = d(o, "y0"), x1 = d(o, "x1"), y1 = d(o, "y1");
			float[] uv = ControlBoards.region(o.get("m"));
			if (o.has("d")) {
				LiveBoards.rect(f, x0, y1 - frac * (y1 - y0), x1, y1, d(o, "z"), uv);
			} else if (o.has("h")) {
				LiveBoards.rect(f, x0, y0, x0 + frac * (x1 - x0), y1, d(o, "z"), uv);
			} else {
				LiveBoards.rect(f, x0, y0, x1, y0 + frac * (y1 - y0), d(o, "z"), uv);
			}
		}
		// The trends: each pen's samples, five minutes ago on the left to now on the right, stepping as a recorder's does.
		int n = ControlBoard.SAMPLES;
		for (JsonElement e : array(live, "pens")) {
			JsonObject rec = e.getAsJsonObject();
			double x0 = d(rec, "x0"), x1 = d(rec, "x1"), y0 = d(rec, "y0"), y1 = d(rec, "y1"), z = d(rec, "z"), t = 0.003;
			for (JsonElement q : rec.getAsJsonArray("pens")) {
				JsonObject pen = q.getAsJsonObject();
				int i = pen.get("i").getAsInt();
				double lo = d(pen, "lo"), hi = d(pen, "hi");
				float[] uv = ControlBoards.region(pen.get("c"));
				double px = Double.NaN, py = 0;
				for (int k = n - s.filled; k < n; k++) {
					int idx = i * n + Math.floorMod(s.head + k, n);
					if (idx >= s.history.length) {
						break;
					}
					double frac = Math.max(0, Math.min(1, (s.history[idx] - lo) / (hi - lo)));
					double x = x0 + k / (double) (n - 1) * (x1 - x0), y = y0 + frac * (y1 - y0);
					if (!Double.isNaN(px)) {
						LiveBoards.rect(f, px, py - t, x, py + t, z, uv);
						if (Math.abs(y - py) > 1e-4) {
							LiveBoards.rect(f, x - t, Math.min(py, y) - t, x + t, Math.max(py, y) + t, z - 0.0005, uv);
						}
					}
					px = x;
					py = y;
				}
			}
		}
		// The alarm strip's slots: their colours by the alarms' states (flashing until acknowledged at their board).
		boolean fast = ((long) (clock / 4)) % 2 == 0, slow = ((long) (clock / 10)) % 2 == 0;
		for (JsonElement e : array(live, "alarms")) {
			JsonObject a = e.getAsJsonObject();
			JsonArray slots = a.getAsJsonArray("slots");
			JsonObject mats = a.getAsJsonObject("mats");
			for (int i = 0; i < Math.min(slots.size(), s.board.wallAlarms.length); i++) {
				JsonArray q = slots.get(i).getAsJsonArray();
				String colour = LiveBoards.wallColour(s.board.wallAlarms[i]);
				byte st = DisplayWall.state(s.board.wallAlarms[i]);
				boolean lit = st == ControlBoard.ACKED || st == ControlBoard.ALERT && fast;
				if (st == ControlBoard.RINGBACK && !slow) {
					continue;
				}
				LiveBoards.rect(f, q.get(0).getAsDouble(), q.get(1).getAsDouble(), q.get(2).getAsDouble(), q.get(3).getAsDouble(), d(a, "z"),
						ControlBoards.region(mats.get(lit ? colour : colour + "_dim")));
			}
		}
		if (mesh.vertices() > 0) {
			mesh.replay(pose, consumer, state.facing, LightCoordsUtil.FULL_BRIGHT);
		}
	}

	// ------------------------------------------------------------------ the words

	private void texts(PoseStack pose, SubmitNodeCollector collector, State state, JsonObject live) {
		OverviewScreenBlockEntity s = state.screen;
		for (JsonElement e : array(live, "texts")) {
			JsonObject t = e.getAsJsonObject();
			OverviewScreen.Text text = OverviewScreen.text(t.get("id").getAsString(), s, state.clock, state.day, state.rain, state.thunder);
			if (text.text().isEmpty()) {
				continue;
			}
			JsonArray c = t.getAsJsonArray("c");
			int colour = text.colour() != 0 ? text.colour() : 0xFF000000 | c.get(0).getAsInt() << 16 | c.get(1).getAsInt() << 8 | c.get(2).getAsInt();
			line(pose, collector, text.text(), d(t, "x"), d(t, "y"), d(t, "z"), d(t, "h"), t.get("a").getAsString(), t.has("w") ? d(t, "w") : 0, colour);
		}
		boolean fast = ((long) (state.clock / 4)) % 2 == 0;
		for (JsonElement e : array(live, "alarms")) {
			JsonObject a = e.getAsJsonObject();
			JsonArray slots = a.getAsJsonArray("slots");
			double z = d(a, "tz");
			for (int i = 0; i < Math.min(slots.size(), s.board.wallAlarms.length); i++) {
				JsonArray q = slots.get(i).getAsJsonArray();
				int packed = s.board.wallAlarms[i];
				byte st = DisplayWall.state(packed);
				boolean lit = st == ControlBoard.ACKED || st == ControlBoard.ALERT && fast;
				boolean amber = LiveBoards.wallColour(packed).equals("amber");
				int ink = lit && amber ? DisplayWall.INK : 0xFFF4F4F2;
				double x0 = q.get(0).getAsDouble(), y0 = q.get(1).getAsDouble(), x1 = q.get(2).getAsDouble(), y1 = q.get(3).getAsDouble();
				double w = x1 - x0 - 0.06, h = y1 - y0;
				line(pose, collector, DisplayWall.boardName(DisplayWall.kind(packed)), x0 + 0.03, y1 - h * 0.27, z, 0.026, "l", w, ink);
				List<String> lines = LiveBoards.wallLegend(packed);
				line(pose, collector, String.join(" ", lines), x0 + 0.03, y0 + h * 0.3, z, 0.036, "l", w, ink);
			}
		}
	}

	/** A line of words: capitals h high, their middle at y, anchored l, c or r at x, no wider than widest. */
	private void line(PoseStack pose, SubmitNodeCollector collector, String s, double x, double y, double z, double h, String anchor, double widest, int colour) {
		double k = h / 7.0;
		double width = font.width(s) * k;
		if (widest > 0 && width > widest) {
			k *= widest / width;
			width = widest;
		}
		double left = anchor.equals("l") ? x : anchor.equals("c") ? x - width / 2 : x - width;
		pose.pushPose();
		// The face is at z in the board's frame (z toward the wall): 1 - z here, a hair in front.
		pose.translate((float) left, (float) (y - 3.5 * k), (float) (1 - z + 0.003));
		pose.scale((float) k, (float) -k, (float) k);
		collector.submitText(pose, 0, -7, Texts.of(s), false, Font.DisplayMode.POLYGON_OFFSET,
				LightCoordsUtil.FULL_BRIGHT, colour, 0, 0);
		pose.popPose();
	}

	private static float yRotation(Direction facing) {
		return switch (facing) {
			case EAST -> 90;
			case SOUTH -> 180;
			case WEST -> 270;
			default -> 0;
		};
	}

	private static JsonArray array(JsonObject o, String key) {
		return o.has(key) ? o.getAsJsonArray(key) : new JsonArray();
	}

	private static Vec3 corner(JsonArray q, int at) {
		return v(q.get(at).getAsDouble(), q.get(at + 1).getAsDouble(), q.get(at + 2).getAsDouble());
	}

	private static double d(JsonObject o, String key) {
		return o.get(key).getAsDouble();
	}
}
