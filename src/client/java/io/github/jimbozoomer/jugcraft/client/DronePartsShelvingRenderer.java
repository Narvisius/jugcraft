package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.drone.DepotDisplayBlockEntity;
import io.github.jimbozoomer.jugcraft.tower.DronePartsShelvingBlock;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * The Drone Parts Shelving (tower/DronePartsShelvingBlock; work list item 9, approved 22 October 2026): its mesh from
 * tools/drone_parts_shelving.py for its stock, turned to its facing, and what on it is live. The eight smart packs charge
 * in their station towards storage level, each gauge filling a LED at a time with the next LED blinking, the slot LEDs
 * amber while a pack charges and green when it is ready, the station's screen counting the ready packs and the time to
 * the next; the balance charger's four channels fill their bars and show their percentages, each starting over with a
 * fresh pack when it is done.
 *
 * <p>Live faces (client/ConsoleMesh): kind 8 a gauge LED (id pack * 4 + LED), 9 a slot LED, 10 the station's screen, 11
 * the charger's screen; drawn from the mesh's white patch (its first edge tile) in their colour, full bright.
 */
public final class DronePartsShelvingRenderer implements BlockEntityRenderer<DepotDisplayBlockEntity, DronePartsShelvingRenderer.State> {
	static final Identifier TEXTURE = Jugcraft.id("textures/entity/drone_parts_shelving.png");
	private static final RenderType MESH = RenderTypes.entityCutout(TEXTURE);
	private static final RenderType GLASS = RenderTypes.entityTranslucentCull(TEXTURE);
	static final int GAUGE = 8, SLOT = 9, STATION = 10, CHARGER = 11;
	static final int GREEN = 0xFF5CFF7A, AMBER = 0xFFFFB84A, DARK = 0xFF18201A, WHITE = 0xFFDCE6EA, TITLE = 0xFFFFBA5A;
	/** How long a pack takes from one LED to full (seconds), and a charger channel from empty to full. */
	static final double PACK_CYCLE = 900, CHANNEL_CYCLE = 420;

	private final Font font;

	public DronePartsShelvingRenderer(BlockEntityRendererProvider.Context context) {
		this.font = context == null ? null : context.font();
	}

	public static final class State extends BlockEntityRenderState {
		boolean present;
		float facing;
		String stock = "a";
		double seconds;
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public int getViewDistance() {
		return 64;
	}

	/** The shelving reaches a block either side of its master and two over it. */
	@Override
	public boolean shouldRenderOffScreen() {
		return true;
	}

	static String meshName(String stock) {
		return "drone_parts_shelving_" + stock;
	}

	/** The way the shelving faces, as a turn of its mesh (front to +z). */
	static float facingTurn(net.minecraft.core.Direction facing) {
		return switch (facing) {
			case NORTH -> (float) Math.PI;
			case EAST -> (float) Math.PI / 2;
			case WEST -> -(float) Math.PI / 2;
			default -> 0;
		};
	}

	@Override
	public void extractRenderState(DepotDisplayBlockEntity be, State state, float partialTick, Vec3 cameraPos,
			ModelFeatureRenderer.CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(be, state, crumbling);
		BlockState bs = be.getBlockState();
		Level level = be.getLevel();
		state.present = level != null && bs.getBlock() instanceof DronePartsShelvingBlock;
		if (!state.present) {
			return;
		}
		state.facing = facingTurn(bs.getValue(DronePartsShelvingBlock.FACING));
		state.stock = bs.getValue(DronePartsShelvingBlock.STOCK).getSerializedName();
		state.seconds = (level.getGameTime() + partialTick) / 20.0 + (be.getBlockPos().asLong() & 1023);
	}

	// ------------------------------------------------------------------ what the batteries are doing

	/** A pack's progress through its charge (0 to 1): each pack started at its own time. */
	static double packProgress(int pack, double seconds) {
		double phase = (pack * 0.37 + 0.11) % 1.0;
		return (seconds / PACK_CYCLE + phase) % 1.0;
	}

	/** LEDs lit on a pack's gauge (1 to 4); full when the fourth is steady. */
	static int packLevel(int pack, double seconds) {
		return Math.min(4, 1 + (int) (packProgress(pack, seconds) * 4.0));
	}

	/** Ready: at storage level (three LEDs) or more. */
	static boolean packReady(int pack, double seconds) {
		return packLevel(pack, seconds) >= 3;
	}

	/** The colour of a pack's gauge LED: lit ones green, the one charging blinking, the rest dark. */
	static int gaugeColour(int pack, int led, double seconds) {
		int level = packLevel(pack, seconds);
		if (led < level) {
			return GREEN;
		}
		if (led == level && level < 4) {
			return (seconds % 1.0) < 0.5 ? GREEN : DARK;
		}
		return DARK;
	}

	static int slotColour(int slot, double seconds) {
		if (packReady(slot, seconds)) {
			return GREEN;
		}
		return (seconds % 2.0) < 1.2 ? AMBER : 0xFF3A2A12;
	}

	/** A charger channel's fill (0 to 1); it holds at 100% for a while, then a fresh pack goes on. */
	static double channelFill(int channel, double seconds) {
		double period = CHANNEL_CYCLE * (1 + 0.23 * channel);
		double x = (seconds / period + channel * 0.29) % 1.0;
		return Math.min(1.0, x * 1.15);
	}

	// ------------------------------------------------------------------ drawing

	@Override
	public void submit(State state, PoseStack poses, SubmitNodeCollector collector, CameraRenderState camera) {
		if (!state.present) {
			return;
		}
		ConsoleMesh mesh = ConsoleMesh.get(meshName(state.stock));
		if (mesh == null) {
			return;
		}
		poses.pushPose();
		poses.translate(0.5, 0, 0.5);
		poses.mulPose(new Matrix4f().rotationY(state.facing));
		int light = state.lightCoords;
		double t = state.seconds;
		float[] white = mesh.edgeTiles[0];
		collector.submitCustomGeometry(poses, MESH, (pose, consumer) -> {
			DroneDepotRenderer.Geometry g = new DroneDepotRenderer.Geometry(pose, consumer);
			if (!BakedFixtures.baked(io.github.jimbozoomer.jugcraft.tower.JugcraftTower.DRONE_PARTS_SHELVING)) {          // else in the chunk's mesh
				ConsoleMesh.emit(g, mesh.quads[ConsoleMesh.SOLID], light, false);
				ConsoleMesh.emit(g, mesh.quads[ConsoleMesh.LIT], LightCoordsUtil.FULL_BRIGHT, true);
			}
			for (int i = 0; i < mesh.liveKind.length; i++) {
				int kind = mesh.liveKind[i], id = mesh.liveId[i];
				int colour = switch (kind) {
					case GAUGE -> gaugeColour(id / 4, id % 4, t);
					case SLOT -> slotColour(id, t);
					default -> 0;
				};
				if (colour != 0) {
					face(g, mesh.live[i], 0, 0, 1, 1, white, colour);
				} else if (kind == CHARGER) {
					for (int c = 0; c < 4; c++) {
						double fill = channelFill(c, t);
						float y0 = 0.2f * (c + 1) + 0.05f, y1 = y0 + 0.10f;
						face(g, mesh.live[i], 0.22f, y0, 0.70f, y1, white, 0xFF26343A);
						face(g, mesh.live[i], 0.225f, y0 + 0.012f, 0.225f + (float) (0.47 * fill), y1 - 0.012f, white, fill >= 0.5 ? GREEN : AMBER);
					}
				}
			}
		});
		collector.submitCustomGeometry(poses, GLASS, (pose, consumer) -> ConsoleMesh.emit(new DroneDepotRenderer.Geometry(pose, consumer),
				mesh.quads[ConsoleMesh.GLASS], light, false));
		if (font != null) {
			for (int i = 0; i < mesh.liveKind.length; i++) {
				if (mesh.liveKind[i] == STATION) {
					int ready = 0, next = -1;
					double best = 2;
					for (int p = 0; p < 8; p++) {
						if (packReady(p, t)) {
							ready++;
						} else {
							double left = 0.5 - packProgress(p, t);
							if (left >= 0 && left < best) {
								best = left;
								next = p;
							}
						}
					}
					String[] lines = {"BATTERY STATION", ready + "/8 READY  STORAGE", next < 0 ? "ALL PACKS READY"
							: "NEXT T4-0" + (next + 1) + "  " + Math.max(1, (int) Math.ceil(best * PACK_CYCLE / 60)) + " MIN"};
					int[] colours = {TITLE, GREEN, WHITE};
					text(poses, collector, mesh.live[i], lines, new String[3], colours, 120);
				} else if (mesh.liveKind[i] == CHARGER) {
					String[] lines = new String[5], values = new String[5];
					int[] colours = new int[5];
					lines[0] = "QUAD BALANCE CHARGER";
					colours[0] = TITLE;
					for (int c = 0; c < 4; c++) {
						double fill = channelFill(c, t);
						lines[c + 1] = "CH" + (c + 1);
						values[c + 1] = fill >= 1 ? "DONE" : (int) (fill * 100) + "%";
						colours[c + 1] = fill >= 1 ? GREEN : WHITE;
					}
					text(poses, collector, mesh.live[i], lines, values, colours, 160);
				}
			}
		}
		poses.popPose();
	}

	/** Part of a live face (fractions across and down it), filled with one colour, full bright. */
	private static void face(DroneDepotRenderer.Geometry g, float[][] f, float a0, float b0, float a1, float b1, float[] white, int argb) {
		float[] p00 = at(f, a0, b0), p10 = at(f, a1, b0), p11 = at(f, a1, b1), p01 = at(f, a0, b1);
		Vector3f n = normal(f);
		float u = (white[0] + white[2]) / 2, v = (white[1] + white[3]) / 2;
		// tl, bl, br, tr: counter-clockwise as the face's viewer sees it, a hair in front of the face
		float lift = 0.0006f;
		g.meshVertex(p00[0] + n.x * lift, p00[1] + n.y * lift, p00[2] + n.z * lift, u, v, n.x, n.y, n.z, argb, LightCoordsUtil.FULL_BRIGHT);
		g.meshVertex(p01[0] + n.x * lift, p01[1] + n.y * lift, p01[2] + n.z * lift, u, v, n.x, n.y, n.z, argb, LightCoordsUtil.FULL_BRIGHT);
		g.meshVertex(p11[0] + n.x * lift, p11[1] + n.y * lift, p11[2] + n.z * lift, u, v, n.x, n.y, n.z, argb, LightCoordsUtil.FULL_BRIGHT);
		g.meshVertex(p10[0] + n.x * lift, p10[1] + n.y * lift, p10[2] + n.z * lift, u, v, n.x, n.y, n.z, argb, LightCoordsUtil.FULL_BRIGHT);
	}

	/** A point of a live face (tl tr br bl) at fractions across (a) and down (b). */
	static float[] at(float[][] f, float a, float b) {
		float[] out = new float[3];
		for (int k = 0; k < 3; k++) {
			float top = f[0][k] + (f[1][k] - f[0][k]) * a;
			float bottom = f[3][k] + (f[2][k] - f[3][k]) * a;
			out[k] = top + (bottom - top) * b;
		}
		return out;
	}

	static Vector3f normal(float[][] f) {
		Vector3f right = new Vector3f(f[1][0] - f[0][0], f[1][1] - f[0][1], f[1][2] - f[0][2]);
		Vector3f down = new Vector3f(f[3][0] - f[0][0], f[3][1] - f[0][1], f[3][2] - f[0][2]);
		return new Vector3f(down).cross(right).normalize();
	}

	/** Lines of text on a live face, `pixels` across its width; a line's value (if any) set flush right. */
	private void text(PoseStack poses, SubmitNodeCollector collector, float[][] f, String[] lines, String[] values, int[] colours, int pixels) {
		Vector3f right = new Vector3f(f[1][0] - f[0][0], f[1][1] - f[0][1], f[1][2] - f[0][2]);
		Vector3f down = new Vector3f(f[3][0] - f[0][0], f[3][1] - f[0][1], f[3][2] - f[0][2]);
		float width = right.length(), height = down.length();
		right.normalize();
		down.normalize();
		Vector3f out = new Vector3f(down).cross(right).normalize();
		float scale = width / pixels;
		int lineHeight = Math.max(1, (int) Math.floor(height / scale / lines.length));
		poses.pushPose();
		poses.translate(f[0][0] + out.x * 0.0012f, f[0][1] + out.y * 0.0012f, f[0][2] + out.z * 0.0012f);
		// columns: +x along the face's top edge, +y up it (text runs down it), +z out of it
		Matrix4f basis = new Matrix4f(right.x, right.y, right.z, 0, -down.x, -down.y, -down.z, 0, out.x, out.y, out.z, 0, 0, 0, 0, 1);
		poses.mulPose(basis);
		poses.scale(scale, -scale, scale);
		for (int i = 0; i < lines.length; i++) {
			String s = font.plainSubstrByWidth(lines[i], pixels - 4);
			collector.submitText(poses, 2, 1 + i * lineHeight, Texts.of(s), false, Font.DisplayMode.POLYGON_OFFSET,
					LightCoordsUtil.FULL_BRIGHT, colours[i], 0, 0);
			if (values[i] != null) {
				collector.submitText(poses, pixels - 2 - font.width(values[i]), 1 + i * lineHeight, Texts.of(values[i]),
						false, Font.DisplayMode.POLYGON_OFFSET, LightCoordsUtil.FULL_BRIGHT, colours[i], 0, 0);
			}
		}
		poses.popPose();
	}
}
