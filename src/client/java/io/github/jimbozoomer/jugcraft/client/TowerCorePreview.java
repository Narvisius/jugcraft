package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.tower.JugcraftTower;
import io.github.jimbozoomer.jugcraft.tower.TowerCoreBlock;
import io.github.jimbozoomer.jugcraft.tower.TowerData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.fabric.api.event.client.player.ClientHotbarScrollEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * While a Drone Tower Core is held and aimed at the ground, shows in amber the space the tower will take, measured from
 * where the core would end up (set into the ground when placed on top of it): first the foundation (the control
 * building and its yard); shift+scroll steps through the tiers up to the finished tower. It replaces the old Drone Tower
 * Foundation blueprint's preview, so players can still see how much room to leave before they place the core.
 */
public final class TowerCorePreview {
	private static final RenderType RENDER_TYPE = RenderTypes.entityTranslucentEmissive(Jugcraft.id("textures/misc/blueprint_ghost.png"));
	private static final int TOP = 0x50FFB84A;
	private static final int SIDE = 0x30FFB84A;
	private static final int CORE = 0xC0FFC04A;
	/** How far (blocks) each side of a flat face stands off its plane, so the two sides never share a plane. */
	private static final float LIFT = 0.05F / 16.0F;
	/** 0: the foundation; 1-9: the tower up to that tier. */
	private static int stage;
	/** How far away the ground the core is aimed at may be (well past reach: the preview is for picking a spot). */
	private static final double RANGE = 128;
	private static final Map<Integer, List<float[]>> FACES = new HashMap<>();

	private TowerCorePreview() {
	}

	/** Shows the tower up to {@code tier} (0: the foundation), as shift+scroll does (client tests). */
	public static void setStage(int tier) {
		stage = Math.max(0, Math.min(TowerData.TIERS, tier));
	}

	private static boolean holding(Player player) {
		return player.getMainHandItem().is(JugcraftTower.CORE.asItem()) || player.getOffhandItem().is(JugcraftTower.CORE.asItem());
	}

	public static void register() {
		ClientHotbarScrollEvents.ALLOW.register((inventory, current, next, dx, dy) -> {
			Minecraft minecraft = Minecraft.getInstance();
			if (minecraft.player == null || !minecraft.hasShiftDown() || dy == 0 || !holding(minecraft.player)) {
				return true;
			}
			stage = Math.floorMod(stage + (dy > 0 ? 1 : -1), TowerData.TIERS + 1);
			String name = Component.translatable(stage == 0 ? "tower.jugcraft.foundation" : "tower.jugcraft.tier." + stage).getString();
			minecraft.player.sendOverlayMessage(Component.translatable("message.jugcraft.tower.preview", stage,
					name.toUpperCase(Locale.ROOT)));
			return false;
		});
		LevelRenderEvents.COLLECT_SUBMITS.register(context -> {
			Minecraft minecraft = Minecraft.getInstance();
			Player player = minecraft.player;
			Level level = minecraft.level;
			if (player == null || level == null || !holding(player)) {
				return;
			}
			BlockPos core = where(player, level, minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false));
			if (core == null) {
				return;
			}
			List<float[]> faces = FACES.computeIfAbsent(stage, TowerCorePreview::faces);
			Vec3 camera = context.levelState().cameraRenderState.pos;
			PoseStack pose = context.poseStack();
			pose.pushPose();
			pose.translate(core.getX() - camera.x, core.getY() - camera.y, core.getZ() - camera.z);
			context.submitNodeCollector().submitCustomGeometry(pose, RENDER_TYPE, (p, consumer) -> {
				cube(p, consumer, 0.1f, 0.1f, 0.1f, 0.9f, 0.9f, 0.9f, CORE);
				for (float[] f : faces) {
					face(p, consumer, f[0], f[1], f[2], f[3], f[4], f[5], f[6] == 0 ? TOP : SIDE);
				}
			});
			pose.popPose();
		});
	}

	/** Where a core placed on the block the player is aiming at would end up, or null when they aren't aiming at one. */
	private static BlockPos where(Player player, Level level, float partialTick) {
		if (!(player.pick(RANGE, partialTick, false) instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
			return null;
		}
		BlockPos clicked = hit.getBlockPos();
		BlockPos pos = level.getBlockState(clicked).canBeReplaced() ? clicked : clicked.relative(hit.getDirection());
		return TowerCoreBlock.sinks(level, pos) ? pos.below() : pos;
	}

	/** The outer faces of the tower up to {@code stage}, as columns: each column's top, and its sides where the next is lower. */
	private static List<float[]> faces(int stage) {
		TowerData data = TowerData.get(TowerData.CURRENT_LAYOUT);
		Map<Long, int[]> columns = new HashMap<>();
		List<TowerData.Tier> steps = new ArrayList<>();
		if (data.foundation != null) {
			steps.add(data.foundation);
		}
		for (int t = 1; t <= stage; t++) {
			steps.add(data.tier(t));
		}
		for (TowerData.Tier step : steps) {
			for (int[] b : step.place) {
				int[] col = columns.computeIfAbsent(key(b[0], b[2]), k -> new int[] {b[0], b[2], b[1], b[1]});
				col[2] = Math.min(col[2], b[1]);
				col[3] = Math.max(col[3], b[1]);
			}
		}
		List<float[]> faces = new ArrayList<>();
		int[][] sides = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
		for (int[] col : columns.values()) {
			int x = col[0], z = col[1], lo = Math.max(0, col[2]), hi = col[3] + 1;
			faces.add(new float[] {x, hi, z, x + 1, hi, z + 1, 0});
			for (int[] s : sides) {
				int[] n = columns.get(key(x + s[0], z + s[1]));
				int from = n == null ? lo : Math.max(lo, n[3] + 1);
				if (from >= hi) {
					continue;
				}
				float fx = s[0] > 0 ? x + 1 : x, fz = s[1] > 0 ? z + 1 : z;
				faces.add(s[0] != 0 ? new float[] {fx, from, z, fx, hi, z + 1, 1} : new float[] {x, from, fz, x + 1, hi, fz, 1});
			}
		}
		return faces;
	}

	private static long key(int x, int z) {
		return ((long) x << 32) ^ (z & 0xFFFFFFFFL);
	}

	private static void cube(PoseStack.Pose pose, VertexConsumer c, float ax, float ay, float az, float bx, float by, float bz, int color) {
		quad(pose, c, color, ax, by, az, ax, by, bz, bx, by, bz, bx, by, az, 0, 1, 0);
		quad(pose, c, color, ax, ay, bz, ax, ay, az, bx, ay, az, bx, ay, bz, 0, -1, 0);
		quad(pose, c, color, ax, ay, az, ax, by, az, bx, by, az, bx, ay, az, 0, 0, -1);
		quad(pose, c, color, bx, ay, bz, bx, by, bz, ax, by, bz, ax, ay, bz, 0, 0, 1);
		quad(pose, c, color, ax, ay, bz, ax, by, bz, ax, by, az, ax, ay, az, -1, 0, 0);
		quad(pose, c, color, bx, ay, az, bx, by, az, bx, by, bz, bx, ay, bz, 1, 0, 0);
	}

	/** One flat face, both sides visible, each side standing {@link #LIFT} off the plane on its own side. */
	private static void face(PoseStack.Pose pose, VertexConsumer c, float x0, float y0, float z0, float x1, float y1, float z1, int color) {
		if (y0 == y1) {
			quad(pose, c, color, x0, y0 + LIFT, z0, x0, y0 + LIFT, z1, x1, y0 + LIFT, z1, x1, y0 + LIFT, z0, 0, 1, 0);
			quad(pose, c, color, x0, y0 - LIFT, z1, x0, y0 - LIFT, z0, x1, y0 - LIFT, z0, x1, y0 - LIFT, z1, 0, -1, 0);
		} else if (x0 == x1) {
			quad(pose, c, color, x0 + LIFT, y0, z0, x0 + LIFT, y1, z0, x0 + LIFT, y1, z1, x0 + LIFT, y0, z1, 1, 0, 0);
			quad(pose, c, color, x0 - LIFT, y0, z1, x0 - LIFT, y1, z1, x0 - LIFT, y1, z0, x0 - LIFT, y0, z0, -1, 0, 0);
		} else {
			quad(pose, c, color, x0, y0, z0 - LIFT, x0, y1, z0 - LIFT, x1, y1, z0 - LIFT, x1, y0, z0 - LIFT, 0, 0, -1);
			quad(pose, c, color, x1, y0, z0 + LIFT, x1, y1, z0 + LIFT, x0, y1, z0 + LIFT, x0, y0, z0 + LIFT, 0, 0, 1);
		}
	}

	private static void quad(PoseStack.Pose pose, VertexConsumer c, int color, float x0, float y0, float z0, float x1, float y1, float z1,
			float x2, float y2, float z2, float x3, float y3, float z3, float nx, float ny, float nz) {
		vertex(pose, c, color, x0, y0, z0, 0, 0, nx, ny, nz);
		vertex(pose, c, color, x1, y1, z1, 0, 1, nx, ny, nz);
		vertex(pose, c, color, x2, y2, z2, 1, 1, nx, ny, nz);
		vertex(pose, c, color, x3, y3, z3, 1, 0, nx, ny, nz);
	}

	private static void vertex(PoseStack.Pose pose, VertexConsumer c, int color, float x, float y, float z, float u, float v, float nx, float ny, float nz) {
		c.addVertex(pose, x, y, z).setColor(color).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightCoordsUtil.FULL_BRIGHT)
				.setNormal(pose, nx, ny, nz);
	}
}
