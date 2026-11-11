package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.tower.squad.DropPod;
import io.github.jimbozoomer.jugcraft.tower.squad.JugcraftSquads;
import io.github.jimbozoomer.jugcraft.tower.squad.LaserDesignatorItem;
import io.github.jimbozoomer.jugcraft.tower.squad.SquadDrop;
import io.github.jimbozoomer.jugcraft.tower.squad.SquadPayloads;
import java.util.List;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.event.client.player.ClientPreAttackCallback;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The LTD-7's sight (sheet 5): held to the eye (right-click held, first person) the view is through its own optic, a
 * round field in the dark housing (textures/gui/ltd_sight.png) with the reticle, the heading along the top, the range to
 * what's under the reticle, the zoom and its bar (scroll: 1x to 5x, the mouse slowed to match), the squad picked
 * (crouch and scroll) with what's on the player's account for it, and the prompts. Left-click marks the spot under the
 * reticle (no beam; only this player sees the mark): the server says what came of it (SquadDrop). A set mark is boxed in
 * green brackets with the pods' time to land, until they're down.
 */
public final class LaserDesignatorClient {
	private static final Identifier SIGHT = Jugcraft.id("textures/gui/ltd_sight.png");
	/** The mockup's frame (sheet 5 was drawn 600 high): everything is laid out in it and scaled to the screen. */
	private static final float FRAME = 600F;
	/** The field's radius (of the screen's height), the zoom's steps and limits, how long a refused mark's word stays. */
	private static final float FIELD = 0.46F, ZOOM_STEP = 0.5F, ZOOM_MIN = 1, ZOOM_MAX = 5;
	private static final int SAID_TICKS = 70;
	private static final int AMBER = 0xFFFFBA74, GREEN = 0xFF5BE38A, RED = 0xFFFF5B4D, TXT = 0xFFC3DBDC, WHITE = 0xFFECF0F0, INK = 0xE6000000;

	private static float zoom = 1, zoomShown = 1, zoomShownO = 1;
	private static int pick = 1;
	private static int[] charges = new int[JugcraftSquads.SIZES.length];
	private static boolean inField;
	/** The mark set (where, when, for which squad), and the last word on a mark (what, its colour, until when). */
	private static @Nullable Vec3 mark;
	private static long markAt;
	private static int markSize;
	private static @Nullable Component said;
	private static int saidColour;
	private static long saidUntil, lastMark;

	private LaserDesignatorClient() {
	}

	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(LaserDesignatorClient::tick);
		ClientPreAttackCallback.EVENT.register((client, player, clicks) -> attack(client, player));
		HudElementRegistry.attachElementAfter(VanillaHudElements.MISC_OVERLAYS, Jugcraft.id("ltd_sight"), LaserDesignatorClient::hud);
		ClientPlayNetworking.registerGlobalReceiver(SquadPayloads.Account.TYPE, (payload, context) -> {
			List<Integer> c = payload.charges();
			for (int i = 0; i < charges.length; i++) {
				charges[i] = i < c.size() ? c.get(i) : 0;
			}
			inField = payload.inField();
		});
		ClientPlayNetworking.registerGlobalReceiver(SquadPayloads.Marked.TYPE, (payload, context) -> marked(payload));
	}

	/** Whether the player is looking through a designator now (held to the eye, first person, no screen open). */
	public static boolean sighted() {
		Minecraft client = Minecraft.getInstance();
		return client.player != null && client.gui.screen() == null && client.options.getCameraType().isFirstPerson() && LaserDesignatorItem.sighted(client.player);
	}

	private static void tick(Minecraft client) {
		zoomShownO = zoomShown;
		if (!sighted()) {
			zoom = 1;
			zoomShown = zoomShownO = 1;
			return;
		}
		zoomShown += (zoom - zoomShown) * 0.45F;
		if (Math.abs(zoom - zoomShown) < 0.01F) {
			zoomShown = zoom;
		}
	}

	private static float zoomNow(float partialTick) {
		return Mth.lerp(partialTick, zoomShownO, zoomShown);
	}

	/** The view through the sight: narrowed by its zoom (GunView.fov hands it here). */
	public static float fov(float modifier) {
		return sighted() ? modifier / zoomNow(1.0F) : modifier;
	}

	/** The mouse slowed by the zoom (GunView.turnScale). */
	public static double turnScale() {
		return sighted() ? 1.0 / zoomNow(1.0F) : 1.0;
	}

	/** The scroll wheel through the sight (DesignatorScrollMixin): zoom, or (crouching) the squad picked; true if taken. */
	public static boolean scroll(double amount) {
		if (!sighted() || amount == 0) {
			return false;
		}
		int dir = amount > 0 ? 1 : -1;
		LocalPlayer player = Minecraft.getInstance().player;
		if (player != null && player.isShiftKeyDown()) {
			pick = Math.floorMod(pick - dir, JugcraftSquads.SIZES.length);
		} else {
			zoom = Mth.clamp(zoom + dir * ZOOM_STEP, ZOOM_MIN, ZOOM_MAX);
		}
		return true;
	}

	/** What's under the reticle, as far as a mark reaches (null: nothing). */
	private static @Nullable Vec3 aimed(Minecraft client, float partialTick) {
		if (client.level == null || client.player == null) {
			return null;
		}
		Vec3 eye = client.player.getEyePosition(partialTick);
		Vec3 end = eye.add(client.player.getViewVector(partialTick).scale(JugcraftSquads.MARK_RANGE));
		BlockHitResult hit = client.level.clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.ANY, client.player));
		return hit.getType() == HitResult.Type.MISS ? null : hit.getLocation();
	}

	/** Left-click through the sight: mark the spot under the reticle (the server checks it); never a punch. */
	private static boolean attack(Minecraft client, LocalPlayer player) {
		if (!sighted()) {
			return false;
		}
		long now = client.level == null ? 0 : client.level.getGameTime();
		if (now - lastMark < 10) {
			return true;
		}
		lastMark = now;
		Vec3 at = aimed(client, 1.0F);
		if (at == null) {
			say(Component.translatable("message.jugcraft.squad.mark.nothing"), RED);
			return true;
		}
		ClientPlayNetworking.send(new SquadPayloads.Mark(at, JugcraftSquads.SIZES[pick]));
		return true;
	}

	private static void marked(SquadPayloads.Marked m) {
		Minecraft client = Minecraft.getInstance();
		long now = client.level == null ? 0 : client.level.getGameTime();
		if (m.result() == SquadDrop.OK) {
			mark = m.at();
			markAt = now;
			markSize = m.size();
			said = null;
			return;
		}
		String why = switch (m.result()) {
			case SquadDrop.NO_CHARGE -> "no_charge";
			case SquadDrop.IN_FIELD -> "in_field";
			case SquadDrop.NO_SKY -> "no_sky";
			case SquadDrop.TOO_FAR -> "too_far";
			case SquadDrop.NO_ROOM -> "no_room";
			default -> "not_sighted";
		};
		say(Component.translatable("message.jugcraft.squad.mark." + why, m.size()), RED);
	}

	private static void say(Component what, int colour) {
		Minecraft client = Minecraft.getInstance();
		said = what;
		saidColour = colour;
		saidUntil = (client.level == null ? 0 : client.level.getGameTime()) + SAID_TICKS;
	}

	// ---------------------------------------------------------------- the view through it

	private static void hud(GuiGraphicsExtractor g, DeltaTracker delta) {
		Minecraft client = Minecraft.getInstance();
		if (!sighted() || client.level == null || client.player == null) {
			return;
		}
		float partial = delta.getGameTimeDeltaPartialTick(false);
		int w = g.guiWidth(), h = g.guiHeight();
		float k = h / FRAME;
		int cx = w / 2, cy = h / 2, r = Math.round(h * FIELD);
		// the housing round the field: the optic's square as tall as the screen, its housing out to the sides
		int side = h, left = (w - side) / 2;
		g.blit(RenderPipelines.GUI_TEXTURED, SIGHT, left, 0, 0.0F, 0.0F, side, side, side, side);
		g.fill(0, 0, left, h, 0xFF0E0F11);
		g.fill(left + side, 0, w, h, 0xFF0E0F11);
		reticle(g, cx, cy, r, k);
		heading(g, client, cx, cy, r, k, partial);
		Font font = client.font;
		long now = client.level.getGameTime();
		// range, zoom, the squad, the prompts: each on a dark plate (so it reads over anything), clear of the reticle
		Vec3 at = aimed(client, partial);
		String rng = at == null ? "RNG ---" : "RNG " + Math.round(client.player.getEyePosition(partial).distanceTo(at)) + " m";
		plate(g, font, Component.literal(rng), cx + 68 * k, cy - 30 * k, 15 * k, WHITE);
		float z = zoomNow(partial);
		zoomBar(g, font, cx + r - 70 * k, cy, k, z);
		int n = JugcraftSquads.SIZES[pick];
		plate(g, font, Component.translatable("hud.jugcraft.ltd.squad", n, charges[pick]), cx - 150 * k, cy + r - 74 * k, 14 * k,
				charges[pick] > 0 ? AMBER : TXT);
		plate(g, font, Component.translatable("hud.jugcraft.ltd.prompts"), cx - 150 * k, cy + r - 52 * k, 11 * k, TXT);
		// the mark: brackets round it while the pods are on their way, its word under the reticle
		long land = DropPod.FLIGHT;                                // (the first pod down; the rest within SPREAD_TICKS)
		if (mark != null && now - markAt > land + DropPod.SPREAD_TICKS + 60) {
			mark = null;
		}
		if (mark != null) {
			float[] s = screen(client, mark, w, h, partial);
			if (s != null) {
				brackets(g, s[0], s[1], 46 * k, 16 * k, Math.max(1, Math.round(2 * k)));
			}
			long left_ = Math.max(0, land - (now - markAt));
			Component head = left_ > 0 ? Component.translatable("hud.jugcraft.ltd.marked") : Component.translatable("hud.jugcraft.ltd.down");
			plate(g, font, head, cx - 150 * k, cy + 64 * k, 16 * k, GREEN);
			int secs = (int) Math.ceil(left_ / 20.0);
			plate(g, font, Component.translatable("hud.jugcraft.ltd.eta", secs / 60, String.format("%02d", secs % 60), markSize), cx - 150 * k,
					cy + 88 * k, 11 * k, TXT);
		} else if (inField) {
			plate(g, font, Component.translatable("hud.jugcraft.ltd.in_field"), cx - 150 * k, cy + 64 * k, 12 * k, AMBER);
		}
		if (said != null && now < saidUntil) {
			plate(g, font, said, cx - 150 * k, cy + 112 * k, 12 * k, saidColour);
		}
	}

	/** The reticle: a cross broken round the middle with range ticks along it, the amber box at its centre. */
	private static void reticle(GuiGraphicsExtractor g, int cx, int cy, int r, float k) {
		int t = Math.max(1, Math.round(2 * k)), gap = Math.round(18 * k), tick = Math.max(2, Math.round(5 * k));
		g.fill(cx - r, cy - t / 2, cx - gap, cy - t / 2 + t, INK);
		g.fill(cx + gap, cy - t / 2, cx + r, cy - t / 2 + t, INK);
		g.fill(cx - t / 2, cy - r, cx - t / 2 + t, cy - gap, INK);
		g.fill(cx - t / 2, cy + gap, cx - t / 2 + t, cy + r, INK);
		for (int i = 1; i < 8; i++) {
			int o = gap + Math.round(i * 22 * k);
			g.fill(cx - o - t / 2, cy - tick, cx - o - t / 2 + t, cy + tick, INK);
			g.fill(cx + o - t / 2, cy - tick, cx + o - t / 2 + t, cy + tick, INK);
			g.fill(cx - tick, cy - o - t / 2, cx + tick, cy - o - t / 2 + t, INK);
			g.fill(cx - tick, cy + o - t / 2, cx + tick, cy + o - t / 2 + t, INK);
		}
		int b = Math.max(2, Math.round(4 * k));
		box(g, cx - b, cy - b, cx + b, cy + b, Math.max(1, Math.round(2 * k)), AMBER);
	}

	/** The heading strip along the top of the field: a tick every 5 degrees, the figures every 15, the amber pointer. */
	private static void heading(GuiGraphicsExtractor g, Minecraft client, int cx, int cy, int r, float k, float partial) {
		float yaw = Mth.wrapDegrees(client.player.getViewYRot(partial) + 180);              // the way they face, north 0
		float hdg = (yaw + 360) % 360;
		int top = cy - r + Math.round(18 * k), bottom = cy - r + Math.round(42 * k);
		g.fill(cx - Math.round(150 * k), top, cx + Math.round(150 * k), bottom, 0x78000000);
		Font font = client.font;
		float step = 24 * k;                                   // a 5-degree step on the strip
		float first = (float) Math.ceil((hdg - 30) / 5) * 5;
		for (float d = first; d <= hdg + 30; d += 5) {
			float x = cx + (d - hdg) / 5 * step;
			if (Math.abs(x - cx) > 150 * k) {
				continue;
			}
			g.fill(Math.round(x), bottom - Math.round(6 * k), Math.round(x) + 1, bottom, TXT);
			int whole = Math.floorMod(Math.round(d), 360);
			if (whole % 15 == 0) {
				text(g, font, Component.literal(String.format("%03d", whole)), x, top + 2 * k, 11 * k, TXT, true);
			}
		}
		int p = Math.max(2, Math.round(5 * k));
		for (int i = 0; i < p; i++) {
			g.fill(cx - p + i, bottom + Math.round(4 * k) - i, cx + p - i, bottom + Math.round(4 * k) - i + 1, AMBER);
		}
	}

	/** The zoom: its figure on a plate, the bar filled to it, 1 to 5 marked up its side. */
	private static void zoomBar(GuiGraphicsExtractor g, Font font, float zx, int cy, float k, float z) {
		plate(g, font, Component.literal(String.format("%.1fx", z)), zx - 12 * k, cy - 122 * k, 16 * k, AMBER);
		g.fill(Math.round(zx - 4 * k), Math.round(cy - 96 * k), Math.round(zx + 30 * k), Math.round(cy + 96 * k), 0x5A000000);
		box(g, Math.round(zx), Math.round(cy - 92 * k), Math.round(zx + 10 * k), Math.round(cy + 92 * k), 1, TXT);
		int full = Math.round(184 * k * (z - 1) / 4);
		g.fill(Math.round(zx + 2 * k), Math.round(cy + 90 * k) - full, Math.round(zx + 8 * k), Math.round(cy + 90 * k), AMBER);
		for (int s = 1; s <= 5; s++) {
			float y = cy + 90 * k - 180 * k * (s - 1) / 4;
			text(g, font, Component.literal(String.valueOf(s)), zx + 16 * k, y - 4 * k, 11 * k, TXT, false);
		}
	}

	/** Where a point in the world is on the screen (gui pixels), or null behind the view. */
	private static float @Nullable [] screen(Minecraft client, Vec3 p, int w, int h, float partial) {
		Camera camera = client.gameRenderer.mainCamera();
		Vec3 c = camera.position();
		Matrix4f m = camera.getViewRotationProjectionMatrix(new Matrix4f());
		Vector4f v = m.transform(new Vector4f((float) (p.x - c.x), (float) (p.y - c.y), (float) (p.z - c.z), 1));
		if (v.w <= 0.05F) {
			return null;
		}
		return new float[] {(v.x / v.w + 1) / 2 * w, (1 - v.y / v.w) / 2 * h};
	}

	/** The mark's green corner brackets round (x, y): half size {@code b}, arms {@code arm} long. */
	private static void brackets(GuiGraphicsExtractor g, float x, float y, float b, float arm, int t) {
		for (int sx = -1; sx <= 1; sx += 2) {
			for (int sy = -1; sy <= 1; sy += 2) {
				int px = Math.round(x + sx * b), py = Math.round(y + sy * b), a = Math.round(arm);
				int x0 = sx < 0 ? px : px - a, x1 = sx < 0 ? px + a : px;
				int y0 = sy < 0 ? py : py - a, y1 = sy < 0 ? py + a : py;
				g.fill(x0, sy < 0 ? py : py - t, x1, sy < 0 ? py + t : py, GREEN);
				g.fill(sx < 0 ? px : px - t, y0, sx < 0 ? px + t : px, y1, GREEN);
			}
		}
	}

	private static void box(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, int t, int colour) {
		g.fill(x0, y0, x1, y0 + t, colour);
		g.fill(x0, y1 - t, x1, y1, colour);
		g.fill(x0, y0, x0 + t, y1, colour);
		g.fill(x1 - t, y0, x1, y1, colour);
	}

	/** A readout on its dark plate at (x, y), its letters {@code size} gui pixels tall (the mockup's point size, scaled). */
	private static void plate(GuiGraphicsExtractor g, Font font, Component text, float x, float y, float size, int colour) {
		float s = size / 11F;
		int tw = font.width(text);
		g.fill(Math.round(x - 5 * s), Math.round(y - 3 * s), Math.round(x + tw * s + 5 * s), Math.round(y + 9 * s + 4 * s), 0x8C000000);
		text(g, font, text, x, y, size, colour, false);
	}

	private static void text(GuiGraphicsExtractor g, Font font, Component text, float x, float y, float size, int colour, boolean centred) {
		float s = size / 11F;
		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.pose().scale(s, s);
		if (centred) {
			g.text(font, text, -font.width(text) / 2, 0, colour, false);
		} else {
			g.text(font, text, 0, 0, colour, false);
		}
		g.pose().popMatrix();
	}
}
