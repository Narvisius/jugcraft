package io.github.jimbozoomer.jugcraft.client.tv;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.tv.FlatTvBlock;
import io.github.jimbozoomer.jugcraft.tv.TvBlockEntity;
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
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws a flat screen TV's live parts over its blocks' models (tools/flat_tv.py), from its bottom-left block: the
 * picture fitted to the glass (black bars where the shapes differ), what the screen is doing while there's no picture
 * (and, on a dark band low on the picture, what holds it up: SEEKING TO, BUFFERING, a stopped stream, ENDED),
 * the NARV-TECH logo in the middle of the chin and the power light beside it (blue on, red off), and a standing
 * screen's neck, foot and the electronics housing on its back. Sizes are the model's (1 px = 6.25 cm).
 */
public class TvRenderer implements BlockEntityRenderer<TvBlockEntity, TvRenderer.State> {
	/** The bezel (px): sides, top, chin; the lip it stands in front of the glass; the glass's face hung and standing. */
	static final float BEZ_S = 0.25F, BEZ_T = 0.25F, BEZ_B = 0.55F, LIP = 0.12F, GLASS_WALL = 0.8F, GLASS_STAND = 8.4F;
	/** In front of a face by at least this (px): nothing fights. */
	private static final float OFF = 0.1F;
	private static final Identifier LED = Jugcraft.id("textures/block/tv_led.png");
	private static final Identifier METAL = Jugcraft.id("textures/block/tv_metal.png");
	private static final Identifier BACK = Jugcraft.id("textures/block/tv_back.png");
	private static final FormattedCharSequence LOGO = Component.literal("NARV-TECH").getVisualOrderText();
	private final Font font;

	public static class State extends BlockEntityRenderState {
		boolean draw;
		Direction facing = Direction.NORTH;
		int w = 1, h = 1;
		boolean stand, on;
		@Nullable Identifier video;
		float aspect = 16 / 9F;
		String status = "";
	}

	public TvRenderer(BlockEntityRendererProvider.Context context) {
		this.font = context.font();
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(TvBlockEntity tv, State state, float partialTick, Vec3 camera, ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(tv, state, crumbling);
		BlockState bs = tv.getBlockState();
		state.draw = false;
		state.video = null;
		state.status = "";
		if (!(bs.getBlock() instanceof FlatTvBlock) || bs.getValue(FlatTvBlock.LEFT) || bs.getValue(FlatTvBlock.DOWN) || tv.getLevel() == null) {
			return;
		}
		int[] size = FlatTvBlock.sizeFrom(tv.getLevel(), tv.getBlockPos(), bs);
		state.draw = true;
		state.facing = bs.getValue(FlatTvBlock.FACING);
		state.w = size[0];
		state.h = size[1];
		state.stand = bs.getValue(FlatTvBlock.STAND);
		state.on = !tv.url().isEmpty();
		if (!state.on) {
			TvPlayers.stop(tv.getBlockPos());
			return;
		}
		TvPlayers.Slot slot = TvPlayers.frame(tv, size[0], size[1]);
		TvPlayer p = slot == null ? null : slot.player;
		if (slot == null) {
			state.status = "TOO MANY SCREENS PLAYING NEARBY";
		} else if (p == null) {
			state.status = slot.state;
		} else if (p.hasPicture()) {
			state.video = p.textureId;
			state.aspect = p.aspect();
			state.status = slot.ended ? "ENDED" : p.status();
		} else {
			state.status = p.state;
		}
	}

	@Override
	public void submit(State s, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		if (!s.draw) {
			return;
		}
		pose.pushPose();
		pose.translate(0.5F, 0, 0.5F);
		pose.rotateDegrees(Axis.YP, -s.facing.toYRot() + 180);
		pose.translate(-0.5F, 0, -0.5F);
		pose.scale(1 / 16F, 1 / 16F, 1 / 16F);
		float tw = 16 * s.w, th = 16 * s.h, y0 = s.stand ? (float) FlatTvBlock.LIFT : 0;
		float glass = s.stand ? GLASS_STAND : GLASS_WALL, front = glass + LIP;
		float gx0 = BEZ_S, gx1 = tw - BEZ_S, gy0 = y0 + BEZ_B, gy1 = th - BEZ_T;
		float z = glass + OFF * 0.9F;
		if (s.video != null) {
			float gw = gx1 - gx0, gh = gy1 - gy0;
			float vw = gw, vh = gw / s.aspect;
			if (vh > gh) {
				vh = gh;
				vw = gh * s.aspect;
			}
			float vx0 = gx0 + (gw - vw) / 2, vy0 = gy0 + (gh - vh) / 2;
			Identifier video = s.video;
			final float fx0 = vx0, fy0 = vy0, fx1 = vx0 + vw, fy1 = vy0 + vh;
			collector.submitCustomGeometry(pose, RenderTypes.text(video), (p, c) -> {
				quad(p, c, fx0, fy0, fx1, fy1, z, 0, 0, 1, 1, 0xFFFFFFFF, LightCoordsUtil.FULL_BRIGHT);
			});
			// The bars, black, where the picture's shape leaves glass.
			collector.submitCustomGeometry(pose, RenderTypes.text(LED), (p, c) -> {
				if (fx0 - gx0 > 0.01F) {
					quad(p, c, gx0, gy0, fx0, gy1, z, 0.5F, 0.5F, 0.5F, 0.5F, 0xFF000000, LightCoordsUtil.FULL_BRIGHT);
					quad(p, c, fx1, gy0, gx1, gy1, z, 0.5F, 0.5F, 0.5F, 0.5F, 0xFF000000, LightCoordsUtil.FULL_BRIGHT);
				}
				if (fy0 - gy0 > 0.01F) {
					quad(p, c, gx0, gy0, gx1, fy0, z, 0.5F, 0.5F, 0.5F, 0.5F, 0xFF000000, LightCoordsUtil.FULL_BRIGHT);
					quad(p, c, gx0, fy1, gx1, gy1, z, 0.5F, 0.5F, 0.5F, 0.5F, 0xFF000000, LightCoordsUtil.FULL_BRIGHT);
				}
			});
		}
		if (!s.status.isEmpty() && s.video != null) {
			// Over the picture: on a dark band low on it, amber while it's held up, red when the stream stopped.
			FormattedCharSequence text = Component.literal(s.status).getVisualOrderText();
			float gw = gx1 - gx0, gh = gy1 - gy0;
			float scale = Math.min(gw * 0.8F / Math.max(1, font.width(text)), gh * 0.055F / 8);
			float mx = (gx0 + gx1) / 2, cy = gy0 + gh * 0.12F;
			float hw = font.width(text) * scale / 2 + gh * 0.03F, hh = 4 * scale + gh * 0.022F;
			collector.submitCustomGeometry(pose, RenderTypes.text(LED), (p, c) -> quad(p, c, mx - hw, cy - hh, mx + hw, cy + hh, z + OFF, 0.5F, 0.5F,
					0.5F, 0.5F, 0xC8000000, LightCoordsUtil.FULL_BRIGHT));
			int colour = s.status.startsWith("STREAM STOPPED") || s.status.startsWith("CAN'T") ? 0xFFE0705A
					: s.status.equals("ENDED") ? 0xFF8FB4C8 : 0xFFE8C46A;
			text(pose, collector, text, mx, cy, z + 2 * OFF, scale, colour, LightCoordsUtil.FULL_BRIGHT);
		} else if (!s.status.isEmpty()) {
			FormattedCharSequence text = Component.literal(s.status).getVisualOrderText();
			float scale = Math.min((gx1 - gx0) * 0.86F / Math.max(1, font.width(text)), (gy1 - gy0) * 0.07F / 8);
			text(pose, collector, text, (gx0 + gx1) / 2, (gy0 + gy1) / 2, z + OFF, scale, 0xFF8FB4C8, LightCoordsUtil.FULL_BRIGHT);
		}
		// The chin: the logo in the middle, the power light under its right end.
		float cy = y0 + BEZ_B / 2;
		text(pose, collector, LOGO, tw / 2, cy, front + OFF, 0.30F / 7, 0xFF969AA0, s.lightCoords);
		float lx = tw / 2 + 1.9F, r = 0.07F;
		int led = s.on ? 0xFFA0DCFF : 0xFF961E18;
		collector.submitCustomGeometry(pose, RenderTypes.text(LED), (p, c) -> quad(p, c, lx - r, cy - r, lx + r, cy + r, front + OFF, 0, 0, 1, 1, led,
				LightCoordsUtil.FULL_BRIGHT));
		if (s.stand) {
			stand(pose, collector, s, tw, th, y0);
		}
		pose.popPose();
	}

	/** A standing screen's housing on its back, its neck and its foot (tools/flat_tv.py's mock, r22b/tv). */
	private static void stand(PoseStack pose, SubmitNodeCollector collector, State s, float tw, float th, float y0) {
		float back = (float) FlatTvBlock.STAND_BACK, panelFront = GLASS_STAND, cx = tw / 2;
		int light = s.lightCoords;
		collector.submitCustomGeometry(pose, RenderTypes.entitySolid(BACK), (p, c) ->
				box(p, c, tw * 0.22F, y0 + th * 0.22F, back - 1.1F, tw * 0.78F, th * 0.86F, back, light, ~(1 << SOUTH)));
		float half = Math.min(tw * 0.2F, 9);
		collector.submitCustomGeometry(pose, RenderTypes.entitySolid(METAL), (p, c) -> {
			box(p, c, cx - 1.4F, 0.35F, back - 0.6F, cx + 1.4F, y0 + th * 0.3F, back - 0.1F, light, ~((1 << DOWN) | (1 << SOUTH)));
			box(p, c, cx - half, 0, back - 3.0F, cx + half, 0.35F, panelFront + 3.0F, light, ~(1 << DOWN));
		});
	}

	private static final int SOUTH = 0, NORTH = 1, EAST = 2, WEST = 3, UP = 4, DOWN = 5;

	/** A box (px) with the faces in {@code faces} (bits by SOUTH..DOWN), each its texture once over, lit by the block. */
	private static void box(PoseStack.Pose p, VertexConsumer c, float x0, float y0, float z0, float x1, float y1, float z1, int light, int faces) {
		float[][][] q = {
				{{x0, y0, z1}, {x1, y0, z1}, {x1, y1, z1}, {x0, y1, z1}},
				{{x1, y0, z0}, {x0, y0, z0}, {x0, y1, z0}, {x1, y1, z0}},
				{{x1, y0, z1}, {x1, y0, z0}, {x1, y1, z0}, {x1, y1, z1}},
				{{x0, y0, z0}, {x0, y0, z1}, {x0, y1, z1}, {x0, y1, z0}},
				{{x0, y1, z1}, {x1, y1, z1}, {x1, y1, z0}, {x0, y1, z0}},
				{{x0, y0, z0}, {x1, y0, z0}, {x1, y0, z1}, {x0, y0, z1}}};
		float[][] n = {{0, 0, 1}, {0, 0, -1}, {1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}};
		float[][] uv = {{0, 1}, {1, 1}, {1, 0}, {0, 0}};
		for (int f = 0; f < 6; f++) {
			if ((faces & (1 << f)) == 0) {
				continue;
			}
			for (int i = 0; i < 4; i++) {
				c.addVertex(p, q[f][i][0], q[f][i][1], q[f][i][2]).setColor(0xFFFFFFFF).setUv(uv[i][0], uv[i][1]).setOverlay(OverlayTexture.NO_OVERLAY)
						.setLight(light).setNormal(p, n[f][0], n[f][1], n[f][2]);
			}
		}
	}

	/** A flat rectangle facing out of the screen (px), its texture's (u0, v0) at the top left. */
	private static void quad(PoseStack.Pose p, VertexConsumer c, float x0, float y0, float x1, float y1, float z, float u0, float v0, float u1, float v1,
			int colour, int light) {
		c.addVertex(p, x0, y0, z).setColor(colour).setUv(u0, v1).setLight(light);
		c.addVertex(p, x1, y0, z).setColor(colour).setUv(u1, v1).setLight(light);
		c.addVertex(p, x1, y1, z).setColor(colour).setUv(u1, v0).setLight(light);
		c.addVertex(p, x0, y1, z).setColor(colour).setUv(u0, v0).setLight(light);
	}

	/** Words centred at (x, y) on the screen's face (px), {@code scale} px a font pixel. */
	private void text(PoseStack pose, SubmitNodeCollector collector, FormattedCharSequence text, float x, float y, float z, float scale, int colour,
			int light) {
		pose.pushPose();
		pose.translate(x, y, z);
		pose.scale(scale, -scale, scale);
		collector.submitText(pose, -font.width(text) / 2F, -3.5F, text, false, Font.DisplayMode.POLYGON_OFFSET, light, colour, 0, 0);
		pose.popPose();
	}

	@Override
	public boolean shouldRenderOffScreen() {
		// One block draws the whole screen (up to 48 x 27): keep drawing it while that block is out of view.
		return true;
	}

	@Override
	public int getViewDistance() {
		return 96;
	}
}
