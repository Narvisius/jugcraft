package io.github.jimbozoomer.jugcraft.client.blueprint;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.blueprint.Blueprint;
import io.github.jimbozoomer.jugcraft.blueprint.BlueprintItem;
import io.github.jimbozoomer.jugcraft.blueprint.JugcraftBlueprints;
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
import net.minecraft.world.phys.Vec3;

/**
 * While a Blueprint is held, shows where it would go (up to five chunks away): every block of the structure as
 * a faint ghost, red where something is in the way, and the stake's spot marked. Right-click places it there.
 * Shift+scroll on a blueprint with growth stages ({@link BlueprintStages}) also shows, in amber, the space the
 * finished build will take.
 */
public final class BlueprintPlacementPreview {
	private static final RenderType RENDER_TYPE = RenderTypes.entityTranslucentEmissive(Jugcraft.id("textures/misc/blueprint_ghost.png"));
	private static final int CLEAR = 0x405AB4FF;
	private static final int BLOCKED = 0x90FF4030;
	private static final int STAKE = 0xC0FFC04A;
	/** The finished build's space (a later stage): tops a little brighter than sides. */
	private static final int STAGE_TOP = 0x50FFB84A;
	private static final int STAGE_SIDE = 0x30FFB84A;

	private BlueprintPlacementPreview() {
	}

	public static void register() {
		LevelRenderEvents.COLLECT_SUBMITS.register(context -> {
			Minecraft minecraft = Minecraft.getInstance();
			Player player = minecraft.player;
			Level level = minecraft.level;
			if (player == null || level == null) {
				return;
			}
			ItemStack held = player.getMainHandItem().is(JugcraftBlueprints.BLUEPRINT) ? player.getMainHandItem() : player.getOffhandItem();
			if (!held.is(JugcraftBlueprints.BLUEPRINT)) {
				return;
			}
			Blueprint blueprint = Blueprint.get(BlueprintItem.idOf(held), true);
			BlueprintItem.Target target = BlueprintItem.target(player, minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false));
			if (blueprint == null || target == null) {
				return;
			}
			BlockPos stake = target.stake();
			List<float[]> boxes = new ArrayList<>();
			boxes.add(new float[] {0, 0, 0, Float.intBitsToFloat(STAKE)});
			BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
			for (Blueprint.Cell cell : blueprint.cells(target.rotation())) {
				at.setWithOffset(stake, cell.offset());
				boolean clear = level.getBlockState(at).canBeReplaced() || level.getBlockState(at).is(cell.state().getBlock());
				boxes.add(new float[] {cell.offset().getX(), cell.offset().getY(), cell.offset().getZ(), Float.intBitsToFloat(clear ? CLEAR : BLOCKED)});
			}
			// A later growth stage (shift+scroll): the space the finished build takes, measured from its anchor block.
			String id = BlueprintItem.idOf(held);
			BlueprintStages.Set set = BlueprintStages.stages(id);
			int stageIndex = set == null ? 0 : Math.min(BlueprintStages.stage(id), set.stages().size());
			BlockPos anchor = null;
			if (stageIndex > 0) {
				for (Blueprint.Cell cell : blueprint.cells(target.rotation())) {
					if (net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(cell.state().getBlock()).toString().equals(set.anchor())) {
						anchor = cell.offset();
					}
				}
			}
			List<float[]> faces = anchor == null ? List.of() : set.stages().get(stageIndex - 1).faces();
			BlockPos anchorAt = anchor;
			Vec3 camera = context.levelState().cameraRenderState.pos;
			PoseStack pose = context.poseStack();
			pose.pushPose();
			pose.translate(stake.getX() - camera.x, stake.getY() - camera.y, stake.getZ() - camera.z);
			context.submitNodeCollector().submitCustomGeometry(pose, RENDER_TYPE, (p, consumer) -> {
				for (float[] box : boxes) {
					cube(p, consumer, box[0] + 0.1f, box[1] + 0.1f, box[2] + 0.1f, box[0] + 0.9f, box[1] + 0.9f, box[2] + 0.9f,
							Float.floatToRawIntBits(box[3]));
				}
				if (anchorAt != null) {
					float ax = anchorAt.getX(), ay = anchorAt.getY(), az = anchorAt.getZ();
					for (float[] f : faces) {
						face(p, consumer, f[0] + ax, f[1] + ay, f[2] + az, f[3] + ax, f[4] + ay, f[5] + az, f[6] == 0 ? STAGE_TOP : STAGE_SIDE);
					}
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

	/** One flat face, both sides visible: horizontal when y0 == y1, otherwise a vertical wall along x or z. */
	private static void face(PoseStack.Pose pose, VertexConsumer c, float x0, float y0, float z0, float x1, float y1, float z1, int color) {
		if (y0 == y1) {
			quad(pose, c, color, x0, y0, z0, x0, y0, z1, x1, y0, z1, x1, y0, z0, 0, 1, 0);
			quad(pose, c, color, x0, y0, z1, x0, y0, z0, x1, y0, z0, x1, y0, z1, 0, -1, 0);
		} else if (x0 == x1) {
			quad(pose, c, color, x0, y0, z0, x0, y1, z0, x0, y1, z1, x0, y0, z1, 1, 0, 0);
			quad(pose, c, color, x0, y0, z1, x0, y1, z1, x0, y1, z0, x0, y0, z0, -1, 0, 0);
		} else {
			quad(pose, c, color, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0, 0, 0, -1);
			quad(pose, c, color, x1, y0, z0, x1, y1, z0, x0, y1, z0, x0, y0, z0, 0, 0, 1);
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
