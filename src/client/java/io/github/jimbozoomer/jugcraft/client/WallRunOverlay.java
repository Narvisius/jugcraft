package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.energy.CableBlock;
import io.github.jimbozoomer.jugcraft.fluid.FluidPipeBlock;
import io.github.jimbozoomer.jugcraft.logistics.BrassWrenchItem;
import io.github.jimbozoomer.jugcraft.wallrun.WallRun;
import io.github.jimbozoomer.jugcraft.wallrun.WallRunBlockEntity;
import io.github.jimbozoomer.jugcraft.wallrun.WallRuns;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;

/**
 * Like RimWorld's overlay: while a pipe, a cable or the brass wrench is in hand, the runs hidden in walls nearby show
 * through the walls as faint lines, fluid pipes blue, item tubes amber, cables orange. Also the client's half of
 * Ctrl + right-click pulling a run out of its wall (the game only tells the server about Ctrl as sprinting).
 */
public final class WallRunOverlay {
	private static final RenderType RENDER_TYPE = RenderTypes.textSeeThrough(Jugcraft.id("textures/misc/blueprint_ghost.png"));
	private static final int RADIUS_CHUNKS = 1, RANGE = 20;

	private WallRunOverlay() {
	}

	public static void register() {
		// Ctrl + right-click on a run in a wall, a block in hand: the run comes out (the server checks and does it). The
		// click goes no further, so the block in hand is not placed.
		UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
			if (!level.isClientSide() || player.isSpectator() || !Minecraft.getInstance().hasControlDown()
					|| !(level.getBlockState(hit.getBlockPos()).getBlock() instanceof WallRun) || !WallRuns.canPull(player.getItemInHand(hand))) {
				return InteractionResult.PASS;
			}
			ClientPlayNetworking.send(new WallRuns.PullPayload(hit.getBlockPos(), hand == InteractionHand.MAIN_HAND));
			return InteractionResult.FAIL;
		});
		LevelRenderEvents.COLLECT_SUBMITS.register(context -> {
			Minecraft minecraft = Minecraft.getInstance();
			Player player = minecraft.player;
			Level level = minecraft.level;
			if (player == null || level == null || !holding(player.getMainHandItem())) {
				return;
			}
			BlockPos at = player.blockPosition();
			List<float[]> boxes = new ArrayList<>();
			List<Integer> colours = new ArrayList<>();
			int cx = at.getX() >> 4, cz = at.getZ() >> 4;
			for (int x = cx - RADIUS_CHUNKS; x <= cx + RADIUS_CHUNKS; x++) {
				for (int z = cz - RADIUS_CHUNKS; z <= cz + RADIUS_CHUNKS; z++) {
					if (!(level.getChunk(x, z) instanceof LevelChunk chunk)) {
						continue;
					}
					for (BlockEntity entity : chunk.getBlockEntities().values()) {
						if (entity instanceof WallRunBlockEntity && entity.getBlockPos().closerThan(at, RANGE)) {
							run(level, entity.getBlockPos(), entity.getBlockState(), boxes, colours);
						}
					}
				}
			}
			if (boxes.isEmpty()) {
				return;
			}
			Vec3 camera = context.levelState().cameraRenderState.pos;
			PoseStack pose = context.poseStack();
			pose.pushPose();
			pose.translate(-camera.x, -camera.y, -camera.z);
			context.submitNodeCollector().submitCustomGeometry(pose, RENDER_TYPE, (p, consumer) -> {
				for (int i = 0; i < boxes.size(); i++) {
					float[] b = boxes.get(i);
					cube(p, consumer, b[0], b[1], b[2], b[3], b[4], b[5], colours.get(i));
				}
			});
			pose.popPose();
		});
	}

	private static boolean holding(ItemStack stack) {
		return stack.getItem() instanceof BrassWrenchItem
				|| stack.getItem() instanceof BlockItem item && WallRuns.WALL_OF.containsKey(item.getBlock());
	}

	/** The run's core and an arm towards each side it joins (the next run in the wall, or what it comes out to). */
	private static void run(Level level, BlockPos pos, BlockState state, List<float[]> boxes, List<Integer> colours) {
		if (!(state.getBlock() instanceof WallRun run)) {
			return;
		}
		Block carrier = run.carrier();
		int colour = carrier instanceof FluidPipeBlock ? 0x9050B4FF : carrier instanceof CableBlock ? 0x90FF7A30 : 0x90FFC840;
		float r = carrier instanceof FluidPipeBlock ? 2.2F / 16 : 2.8F / 16;
		float x = pos.getX() + 0.5F, y = pos.getY() + 0.5F, z = pos.getZ() + 0.5F;
		boxes.add(new float[] {x - r, y - r, z - r, x + r, y + r, z + r});
		colours.add(colour);
		for (Direction d : Direction.values()) {
			BlockState next = level.getBlockState(pos.relative(d));
			boolean joined = state.getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(d))
					|| next.getBlock() instanceof WallRun other && other.carrier().getClass() == carrier.getClass()
					&& run.joins(level, pos, d, next);
			if (!joined) {
				continue;
			}
			float ax = x + Math.min(0, d.getStepX()) * 0.5F, ay = y + Math.min(0, d.getStepY()) * 0.5F, az = z + Math.min(0, d.getStepZ()) * 0.5F;
			float bx = x + Math.max(0, d.getStepX()) * 0.5F, by = y + Math.max(0, d.getStepY()) * 0.5F, bz = z + Math.max(0, d.getStepZ()) * 0.5F;
			boxes.add(new float[] {d.getStepX() == 0 ? x - r : ax, d.getStepY() == 0 ? y - r : ay, d.getStepZ() == 0 ? z - r : az,
					d.getStepX() == 0 ? x + r : bx, d.getStepY() == 0 ? y + r : by, d.getStepZ() == 0 ? z + r : bz});
			colours.add(colour);
		}
	}

	private static void cube(PoseStack.Pose pose, VertexConsumer c, float ax, float ay, float az, float bx, float by, float bz, int colour) {
		quad(pose, c, colour, ax, by, az, ax, by, bz, bx, by, bz, bx, by, az);
		quad(pose, c, colour, ax, ay, bz, ax, ay, az, bx, ay, az, bx, ay, bz);
		quad(pose, c, colour, ax, ay, az, ax, by, az, bx, by, az, bx, ay, az);
		quad(pose, c, colour, bx, ay, bz, bx, by, bz, ax, by, bz, ax, ay, bz);
		quad(pose, c, colour, ax, ay, bz, ax, by, bz, ax, by, az, ax, ay, az);
		quad(pose, c, colour, bx, ay, az, bx, by, az, bx, by, bz, bx, ay, bz);
	}

	private static void quad(PoseStack.Pose pose, VertexConsumer c, int colour, float x0, float y0, float z0, float x1, float y1, float z1,
			float x2, float y2, float z2, float x3, float y3, float z3) {
		c.addVertex(pose, x0, y0, z0).setColor(colour).setUv(0, 0).setLight(LightCoordsUtil.FULL_BRIGHT);
		c.addVertex(pose, x1, y1, z1).setColor(colour).setUv(0, 1).setLight(LightCoordsUtil.FULL_BRIGHT);
		c.addVertex(pose, x2, y2, z2).setColor(colour).setUv(1, 1).setLight(LightCoordsUtil.FULL_BRIGHT);
		c.addVertex(pose, x3, y3, z3).setColor(colour).setUv(1, 0).setLight(LightCoordsUtil.FULL_BRIGHT);
	}
}
