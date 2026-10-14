package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.powerline.LineAnchorBlockEntity;
import io.github.jimbozoomer.jugcraft.powerline.LineItem;
import io.github.jimbozoomer.jugcraft.powerline.LineKind;
import io.github.jimbozoomer.jugcraft.powerline.LinePlacement;
import io.github.jimbozoomer.jugcraft.powerline.LineRegistry;
import io.github.jimbozoomer.jugcraft.powerline.LineWires;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * While a power line structure is held and aimed at the ground, shows where it would stand (its footprint as faint
 * blue blocks, red if something is in the way or the ground is too steep) and the wire it would hang to the
 * structure it would be wired to (green), the same one the server picks.
 */
public final class LinePreview {
	private static final RenderType RENDER_TYPE = RenderTypes.entityTranslucentEmissive(Jugcraft.id("textures/misc/blueprint_ghost.png"));
	private static final int CLEAR = 0x405AB4FF;
	private static final int BLOCKED = 0x90FF4030;
	private static final int WIRE = 0xA040FF60;

	private LinePreview() {
	}

	public static void register() {
		LevelRenderEvents.COLLECT_SUBMITS.register(context -> {
			Minecraft minecraft = Minecraft.getInstance();
			Player player = minecraft.player;
			Level level = minecraft.level;
			if (player == null || level == null || !(minecraft.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
				return;
			}
			ItemStack held = player.getMainHandItem();
			if (!(held.getItem() instanceof LineItem item)) {
				return;
			}
			LineKind kind = item.kind;
			BlockPos clicked = hit.getBlockPos();
			if (LineRegistry.of(level).anchorOf(clicked) != null) {
				return;   // aiming at a structure: using the item there upgrades it instead
			}
			BlockPos at = level.getBlockState(clicked).canBeReplaced() ? clicked : clicked.relative(hit.getDirection());
			BlockPos anchor = LinePlacement.footing(level, at, kind);
			boolean fits = anchor != null && LinePlacement.fits(level, anchor, kind, null);
			if (anchor == null) {
				anchor = at;
			}
			List<float[]> boxes = new ArrayList<>();
			int colour = fits ? CLEAR : BLOCKED;
			boxes.add(new float[] {0, 0, 0});
			for (BlockPos frame : kind.frames()) {
				boxes.add(new float[] {frame.getX(), frame.getY(), frame.getZ()});
			}
			List<float[]> wire = new ArrayList<>();
			if (fits) {
				BlockPos partner = LinePlacement.autoPartner(level, anchor, kind);
				if (partner != null && level.getBlockEntity(partner) instanceof LineAnchorBlockEntity other) {
					List<BlockPos> withPartner = List.of(partner);
					List<BlockPos> otherConnections = new ArrayList<>(other.connections());
					otherConnections.add(anchor);
					for (LineWires.Wire w : LineWires.between(anchor, kind, withPartner, partner, other.kind(), otherConnections)) {
						for (int i = 0; i <= 24; i++) {
							Vec3 p = w.at(i / 24.0);
							wire.add(new float[] {(float) (p.x - anchor.getX()), (float) (p.y - anchor.getY()), (float) (p.z - anchor.getZ())});
						}
					}
				}
			}
			BlockPos origin = anchor;
			Vec3 camera = context.levelState().cameraRenderState.pos;
			PoseStack pose = context.poseStack();
			pose.pushPose();
			pose.translate(origin.getX() - camera.x, origin.getY() - camera.y, origin.getZ() - camera.z);
			context.submitNodeCollector().submitCustomGeometry(pose, RENDER_TYPE, (p, consumer) -> {
				for (float[] b : boxes) {
					cube(p, consumer, b[0] + 0.15f, b[1] + 0.02f, b[2] + 0.15f, b[0] + 0.85f, b[1] + 0.98f, b[2] + 0.85f, colour);
				}
				for (float[] w : wire) {
					cube(p, consumer, w[0] - 0.06f, w[1] - 0.06f, w[2] - 0.06f, w[0] + 0.06f, w[1] + 0.06f, w[2] + 0.06f, WIRE);
				}
			});
			pose.popPose();
		});
	}

	private static void cube(PoseStack.Pose pose, VertexConsumer c, float ax, float ay, float az, float bx, float by, float bz, int color) {
		quad(pose, c, color, ax, by, az, ax, by, bz, bx, by, bz, bx, by, az, 0, 1, 0);
		quad(pose, c, color, ax, ay, bz, ax, ay, az, bx, ay, az, bx, ay, bz, 0, -1, 0);
		quad(pose, c, color, ax, ay, az, ax, by, az, bx, by, az, bx, ay, az, 0, 0, -1);
		quad(pose, c, color, bx, ay, bz, bx, by, bz, ax, by, bz, ax, ay, bz, 0, 0, 1);
		quad(pose, c, color, ax, ay, bz, ax, by, bz, ax, by, az, ax, ay, az, -1, 0, 0);
		quad(pose, c, color, bx, ay, az, bx, by, az, bx, by, bz, bx, ay, bz, 1, 0, 0);
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
