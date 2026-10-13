package io.github.jimbozoomer.jugcraft.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.client.DroneDepotRenderer.Geometry;
import io.github.jimbozoomer.jugcraft.nuclear.EquipmentBlockEntity;
import io.github.jimbozoomer.jugcraft.nuclear.EquipmentKind;
import io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlock;
import io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity;
import io.github.jimbozoomer.jugcraft.nuclear.MultiFurnitureBlock;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * What moves on the diesel generator room's panels (round 14): gauge and meter needles, pilot lamps, alarm windows,
 * the hour meter, the regulator's and relays' displays, the load sequencer's screen and step lamps, the breaker's
 * mimic; and on the DC rooms' (rounds 25 and 26), their breaker handles and the battery monitor's bars. Each panel follows the emergency diesel generator it is linked to (LivePanelBlockEntity); its readings come
 * from client/EdgTelemetry.java, laid out by tools/plant_panels.py (assets/jugcraft/live_panels.json, model pixels
 * on the piece facing north). The static drawing on the panel has every live part drawn at rest (no needle, lamps
 * dark, displays blank), so only one of each ever shows.
 */
public class LivePanelRenderer implements BlockEntityRenderer<LivePanelBlockEntity, LivePanelRenderer.State> {
	static final Identifier TEXTURE = Jugcraft.id("textures/entity/live_panel.png");
	static final Identifier LAYOUT = Jugcraft.id("live_panels.json");
	private static final RenderType SOLID = RenderTypes.entityCutoutCull(TEXTURE);
	private static final RenderType GLASS = RenderTypes.entityTranslucentCull(TEXTURE);
	static final float[] WHITE = {0.02F, 0.02F, 0.23F, 0.23F}, LENS = {0.25F, 0F, 0.5F, 0.25F}, HUB = {0.5F, 0F, 0.75F, 0.25F};
	private static Map<String, Layout> layouts;
	private final Font font;

	record Element(String type, double x, double y, double z, double r, double x0, double y0, double x1, double y1, double lo, double hi,
			double start, double sweep, int colour, int colour2, double alpha, double size, String channel, String align, String[] label,
			double[] on, double[] off, boolean open, double warn, int blades, double width) {
	}

	record Layout(int width, List<Element> elements) {
	}

	record Needle(double x, double y, double z, double angle, double length, double tail, double width, double hub, int colour) {
		Needle(double x, double y, double z, double angle, double length, double tail, double width, double hub) {
			this(x, y, z, angle, length, tail, width, hub, 0x1E1E20);
		}
	}

	record Disc(double x, double y, double z, double r, int colour, boolean glow) {
	}

	record Rect(double x0, double y0, double x1, double y1, double z, int colour, boolean opaque) {
	}

	/** A line of text: align 0 from x, 1 centred on it, 2 ending at it (round 26, 8f). */
	record Text(double x, double y, double z, double size, int colour, FormattedCharSequence text, int align, boolean glow) {
	}

	/** A small solid box in the panel's frame (a breaker's handle: round 26, 8e). */
	record Box(double[] b, int colour) {
	}

	public static final class State extends BlockEntityRenderState {
		Direction facing = Direction.NORTH;
		int column;
		final List<Needle> needles = new ArrayList<>();
		final List<Disc> discs = new ArrayList<>();
		final List<Rect> rects = new ArrayList<>();
		final List<Text> texts = new ArrayList<>();
		final List<Box> boxes = new ArrayList<>();
	}

	public LivePanelRenderer(BlockEntityRendererProvider.Context context) {
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
	public void extractRenderState(LivePanelBlockEntity panel, State state, float partialTick, Vec3 cameraPos,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(panel, state, crumbling);
		state.needles.clear();
		state.discs.clear();
		state.rects.clear();
		state.texts.clear();
		state.boxes.clear();
		Level level = panel.getLevel();
		if (level == null || !(panel.getBlockState().getBlock() instanceof LivePanelBlock block)) {
			return;
		}
		Layout layout = layouts().get(BuiltInRegistries.BLOCK.getKey(block).getPath());
		if (layout == null) {
			return;
		}
		state.facing = panel.getBlockState().getValue(MultiFurnitureBlock.FACING);
		state.column = block.origin() % layout.width();
		double now = level.getGameTime() + partialTick;
		EquipmentBlockEntity edg = generator(panel, level);
		EdgTelemetry.Snapshot snap = EdgTelemetry.of(panel, edg, now);
		if (edg == null) {
			EdgTelemetry.battery(snap, panel, level);
			if (io.github.jimbozoomer.jugcraft.nuclear.RelayRoom.kind(panel.getBlockState()) >= 0) {
				// A relay room cabinet (round 26, #10): the reactor, or its name.
				EdgTelemetry.relayRoom(snap, panel, now);
			} else if (io.github.jimbozoomer.jugcraft.nuclear.HvacRoom.kind(panel.getBlockState()) >= 0) {
				// The control room HVAC's panel and monitors (round 26, #11): the room as its panel found it.
				EdgTelemetry.hvac(snap, panel, now);
			} else if (io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity.isInverter(panel.getBlockState())) {
				EdgTelemetry.inverter(snap, panel, level, now);
			} else if (io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity.isTransformer(panel.getBlockState())) {
				// A regulating transformer (round 26, 8d): its MCC breaker and its inverter's load.
				EdgTelemetry.transformer(snap, panel, level, now);
			} else if (io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity.isVitalPanel(panel.getBlockState())) {
				// A vital panel (round 26, 8e): its inverter's output, its breakers.
				EdgTelemetry.vitalPanel(snap, panel, level, now);
			} else if (io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity.isGroundDetector(panel.getBlockState())) {
				// A ground detector and battery monitor (round 26, 8f): its battery, its test.
				EdgTelemetry.groundDetector(snap, panel, level, now);
			} else if (io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity.isH2Monitor(panel.getBlockState())) {
				// A battery room's hydrogen monitor (round 26, #9): its battery's gassing, its fan.
				EdgTelemetry.hydrogen(snap, panel, level, now);
			} else if (io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity.isExhaustFan(panel.getBlockState())) {
				// A battery room's exhaust fan (round 26, #9): its selector, its blades.
				EdgTelemetry.exhaustFan(snap, panel, level, now);
			}
		}
		// What shows only with a panel's door open (a vital panel's breaker handles).
		boolean open = panel.getBlockState().hasProperty(io.github.jimbozoomer.jugcraft.nuclear.VitalPanelBlock.OPEN)
				&& panel.getBlockState().getValue(io.github.jimbozoomer.jugcraft.nuclear.VitalPanelBlock.OPEN);
		boolean blink = ((long) (now / 5)) % 2 == 0;
		for (Element e : layout.elements()) {
			if (e.open() && !open) {
				continue;
			}
			switch (e.type()) {
				case "toggle" -> {
					// A switch's handle (8e), a selector's pointer (#9: one of several, the rest with nothing to show off).
					double[] b = snap.lit(e.channel()) > 0 ? e.on() : e.off();
					if (b.length == 6) {
						state.boxes.add(new Box(b, e.colour()));
					}
				}
				case "fan" -> {
					// A fan's blades (round 26, #9), turned to the channel's angle (degrees).
					double a0 = Math.toRadians(snap.number(e.channel()));
					for (int k = 0; k < e.blades(); k++) {
						state.needles.add(new Needle(e.x(), e.y(), e.z(), a0 + 2 * Math.PI * k / e.blades(), e.r(), -0.4, e.width(), 0, e.colour()));
					}
				}
				case "dial" -> {
					double v = clamp(snap.number(e.channel()), e.lo(), e.hi());
					double a = Math.toRadians(e.start() - e.sweep() * (v - e.lo()) / (e.hi() - e.lo()));
					state.needles.add(new Needle(e.x(), e.y(), e.z(), a, e.r() * 0.82, e.r() * 0.14, 0.2, e.r() * 0.11));
				}
				case "meter" -> {
					double v = clamp(snap.number(e.channel()), e.lo(), e.hi());
					double a = Math.toRadians(90 - 90 * (v - e.lo()) / (e.hi() - e.lo()));
					state.needles.add(new Needle(e.x(), e.y(), e.z(), a, e.r(), 0, 0.15, 0.17));
				}
				case "lamp" -> {
					if (snap.lit(e.channel()) > 0) {
						state.discs.add(new Disc(e.x(), e.y(), e.z(), e.r(), e.colour(), true));
					}
				}
				case "window" -> {
					int lit = snap.lit(e.channel());
					if (lit == 1 || lit == 2 && (e.colour2() != 0 || blink)) {
						int rgb = lit == 2 && e.colour2() != 0 ? e.colour2() : e.colour();
						int alpha = (int) Math.round(e.alpha() * 255);
						state.rects.add(new Rect(e.x0(), e.y0(), e.x1(), e.y1(), e.z(), alpha << 24 | (rgb & 0xFFFFFF), e.alpha() >= 0.9));
						// An alarm window's legend, back-lit.
						String[] label = e.label();
						for (int k = 0; k < label.length; k++) {
							double cy = (e.y0() + e.y1()) / 2 + (label.length - 1) * 0.28 - k * 0.56;
							state.texts.add(new Text((e.x0() + e.x1()) / 2, cy, e.z() + 0.05, 0.34, 0x18181A,
									Component.literal(label[k]).getVisualOrderText(), 1, true));
						}
					}
				}
				case "text" -> {
					String s = snap.text(e.channel());
					if (!s.isEmpty()) {
						// Digital displays light themselves; the hour meter's printed wheels do not.
						boolean glow = !e.channel().startsWith("hours");
						state.texts.add(new Text(e.x(), e.y(), e.z(), e.size(), e.colour(), Component.literal(s).getVisualOrderText(),
								"center".equals(e.align()) ? 1 : "right".equals(e.align()) ? 2 : 0, glow));
					}
				}
				case "bar" -> {
					// A screen's bar (round 26, 8f: a battery monitor's cell volts): filled up from its bottom to the
					// reading on its scale, in its second colour below the warn level; lit.
					double v = snap.number(e.channel());
					double f = Math.max(0, Math.min(1, (v - e.lo()) / (e.hi() - e.lo())));
					if (f > 0) {
						int rgb = v < e.warn() && e.colour2() != 0 ? e.colour2() : e.colour();
						state.rects.add(new Rect(e.x0(), e.y0(), e.x1(), e.y0() + (e.y1() - e.y0()) * f, e.z(), rgb, true));
					}
				}
				default -> {
				}
			}
		}
	}

	private static double clamp(double v, double lo, double hi) {
		// The needle rests on its stop pins a little past each end of the scale.
		double pad = (hi - lo) * 0.02;
		return Math.max(lo - pad, Math.min(hi + pad, v));
	}

	/** The generator a panel is linked to (the server links it: LivePanelBlockEntity), or null. */
	static @Nullable EquipmentBlockEntity generator(LivePanelBlockEntity panel, Level level) {
		return panel.generator != null && level.getBlockEntity(panel.generator) instanceof EquipmentBlockEntity e
				&& e.kind() == EquipmentKind.EMERGENCY_DIESEL_GENERATOR ? e : null;
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		if (state.needles.isEmpty() && state.discs.isEmpty() && state.rects.isEmpty() && state.texts.isEmpty() && state.boxes.isEmpty()) {
			return;
		}
		int light = state.lightCoords;
		pose.pushPose();
		pose.translate(0.5F, 0.0F, 0.5F);
		pose.rotateDegrees(Axis.YP, -yRotation(state.facing));
		pose.translate(-0.5F, 0.0F, -0.5F);
		pose.scale(1 / 16F, 1 / 16F, 1 / 16F);
		// The layout is across the whole piece; this block is its column {@code column}.
		pose.translate(-16F * state.column, 0, 0);
		collector.submitCustomGeometry(pose, SOLID, (p, consumer) -> {
			Geometry g = new Geometry(p, consumer);
			for (Needle n : state.needles) {
				g.light = light;
				g.color = 0xFF000000 | n.colour();
				double dx = Math.cos(n.angle()), dy = Math.sin(n.angle());
				double nx = -dy * n.width() / 2, ny = dx * n.width() / 2;
				double tx = n.x() - dx * n.tail(), ty = n.y() - dy * n.tail();
				double hx = n.x() + dx * n.length(), hy = n.y() + dy * n.length();
				float z = (float) (n.z() + 0.06);
				g.quad((float) (tx - nx), (float) (ty - ny), z, (float) (hx - nx), (float) (hy - ny), z, (float) (hx + nx), (float) (hy + ny), z,
						(float) (tx + nx), (float) (ty + ny), z, 0, 0, 1, WHITE);
				if (n.hub() > 0) {
					square(g, n.x(), n.y(), n.z() + 0.09, n.hub(), HUB);
				}
			}
			for (Disc d : state.discs) {
				g.light = d.glow() ? LightCoordsUtil.FULL_BRIGHT : light;
				g.color = 0xFF000000 | d.colour();
				square(g, d.x(), d.y(), d.z() + 0.05, d.r(), LENS);
			}
			g.light = light;
			for (Box b : state.boxes) {
				box(g, b.b(), b.colour());
			}
			g.light = LightCoordsUtil.FULL_BRIGHT;
			for (Rect r : state.rects) {
				if (r.opaque()) {
					g.color = 0xFF000000 | r.colour();
					float z = (float) (r.z() + 0.04);
					g.quad((float) r.x1(), (float) r.y0(), z, (float) r.x1(), (float) r.y1(), z, (float) r.x0(), (float) r.y1(), z,
							(float) r.x0(), (float) r.y0(), z, 0, 0, 1, WHITE);
				}
			}
			g.color = 0xFFFFFFFF;
		});
		if (state.rects.stream().anyMatch(r -> !r.opaque())) {
			collector.submitCustomGeometry(pose, GLASS, (p, consumer) -> {
				Geometry g = new Geometry(p, consumer);
				g.light = LightCoordsUtil.FULL_BRIGHT;
				for (Rect r : state.rects) {
					if (r.opaque()) {
						continue;
					}
					g.color = r.colour();
					float z = (float) (r.z() + 0.04);
					g.quad((float) r.x1(), (float) r.y0(), z, (float) r.x1(), (float) r.y1(), z, (float) r.x0(), (float) r.y1(), z,
							(float) r.x0(), (float) r.y0(), z, 0, 0, 1, WHITE);
				}
				g.color = 0xFFFFFFFF;
			});
		}
		for (Text t : state.texts) {
			pose.pushPose();
			float k = (float) (t.size() / 7.0);
			pose.translate((float) t.x(), (float) t.y(), (float) (t.z() + 0.02));
			pose.scale(k, -k, k);
			float w = font.width(t.text());
			float x = t.align() == 1 ? -w / 2 : t.align() == 2 ? -w : 0;
			collector.submitText(pose, x, -4.5F, t.text(), false, Font.DisplayMode.POLYGON_OFFSET, t.glow() ? LightCoordsUtil.FULL_BRIGHT : light,
					0xFF000000 | t.colour(), 0, 0);
			pose.popPose();
		}
		pose.popPose();
	}

	/**
	 * A solid box (x0, y0, z0, x1, y1, z1): its six faces, each wound anticlockwise seen from outside, shaded as a block
	 * is (the top full, the sides less, the bottom least).
	 */
	private static void box(Geometry g, double[] b, int rgb) {
		float x0 = (float) b[0], y0 = (float) b[1], z0 = (float) b[2], x1 = (float) b[3], y1 = (float) b[4], z1 = (float) b[5];
		g.color = shade(rgb, 1.0);
		g.quad(x1, y0, z1, x1, y1, z1, x0, y1, z1, x0, y0, z1, 0, 0, 1, WHITE);
		g.quad(x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0, 0, 0, -1, WHITE);
		g.color = shade(rgb, 0.8);
		g.quad(x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1, 1, 0, 0, WHITE);
		g.quad(x0, y0, z1, x0, y1, z1, x0, y1, z0, x0, y0, z0, -1, 0, 0, WHITE);
		g.color = shade(rgb, 1.0);
		g.quad(x1, y1, z1, x1, y1, z0, x0, y1, z0, x0, y1, z1, 0, 1, 0, WHITE);
		g.color = shade(rgb, 0.55);
		g.quad(x1, y0, z0, x1, y0, z1, x0, y0, z1, x0, y0, z0, 0, -1, 0, WHITE);
		g.color = 0xFFFFFFFF;
	}

	private static int shade(int rgb, double k) {
		int r = (int) Math.round((rgb >> 16 & 0xFF) * k), gr = (int) Math.round((rgb >> 8 & 0xFF) * k), b = (int) Math.round((rgb & 0xFF) * k);
		return 0xFF000000 | r << 16 | gr << 8 | b;
	}

	/** A small square facing the front (+z), centred on (x, y), half-size r, showing texture region uv. */
	private static void square(Geometry g, double x, double y, double z, double r, float[] uv) {
		float x0 = (float) (x - r), x1 = (float) (x + r), y0 = (float) (y - r), y1 = (float) (y + r), fz = (float) z;
		g.quad(x1, y0, fz, x1, y1, fz, x0, y1, fz, x0, y0, fz, 0, 0, 1, uv);
	}

	private static float yRotation(Direction facing) {
		return switch (facing) {
			case EAST -> 90;
			case SOUTH -> 180;
			case WEST -> 270;
			default -> 0;
		};
	}

	/** The panels' layouts, read once from the mod's assets. */
	static Map<String, Layout> layouts() {
		if (layouts == null) {
			Map<String, Layout> out = new HashMap<>();
			Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(LAYOUT);
			if (resource.isPresent()) {
				try (Reader reader = resource.get().openAsReader()) {
					JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
					for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
						JsonObject panel = entry.getValue().getAsJsonObject();
						List<Element> elements = new ArrayList<>();
						for (JsonElement el : panel.getAsJsonArray("elements")) {
							elements.add(element(el.getAsJsonObject()));
						}
						out.put(entry.getKey(), new Layout(panel.get("width").getAsInt(), elements));
					}
				} catch (Exception e) {
					Jugcraft.LOGGER.warn("Could not read {}", LAYOUT, e);
				}
			}
			layouts = out;
		}
		return layouts;
	}

	private static Element element(JsonObject o) {
		return new Element(o.get("t").getAsString(), num(o, "x"), num(o, "y"), num(o, "z"), num(o, "r"), num(o, "x0"), num(o, "y0"), num(o, "x1"),
				num(o, "y1"), num(o, "lo"), num(o, "hi"), num(o, "start"), num(o, "sweep"), rgb(o, "colour"), rgb(o, "colour2"),
				o.has("alpha") ? num(o, "alpha") : 0.6, num(o, "size"), o.get("ch").getAsString(), o.has("align") ? o.get("align").getAsString() : "left",
				label(o), nums(o, "on"), nums(o, "off"), o.has("open") && o.get("open").getAsBoolean(), num(o, "warn"), (int) num(o, "blades"),
				num(o, "width"));
	}

	private static double[] nums(JsonObject o, String key) {
		if (!o.has(key)) {
			return new double[0];
		}
		JsonArray a = o.getAsJsonArray(key);
		double[] out = new double[a.size()];
		for (int i = 0; i < out.length; i++) {
			out[i] = a.get(i).getAsDouble();
		}
		return out;
	}

	private static String[] label(JsonObject o) {
		if (!o.has("label")) {
			return new String[0];
		}
		JsonArray a = o.getAsJsonArray("label");
		String[] out = new String[a.size()];
		for (int i = 0; i < out.length; i++) {
			out[i] = a.get(i).getAsString();
		}
		return out;
	}

	private static double num(JsonObject o, String key) {
		return o.has(key) ? o.get(key).getAsDouble() : 0;
	}

	private static int rgb(JsonObject o, String key) {
		if (!o.has(key)) {
			return 0;
		}
		JsonArray a = o.getAsJsonArray(key);
		return a.get(0).getAsInt() << 16 | a.get(1).getAsInt() << 8 | a.get(2).getAsInt();
	}
}
