package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.blueprint.Blueprint;
import io.github.jimbozoomer.jugcraft.blueprint.SurveyStakeBlockEntity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Draws a placed blueprint as a walk-through hologram, worked out on each client from its own copy of the
 * world: a blue ghost where a block is still missing, an amber ghost where it must be placed by hand (blocks
 * with their own block entity, like the Tower Core), and a red outline where a wrong block is in the way.
 * Blocks already in place show nothing.
 */
public class SurveyStakeRenderer implements BlockEntityRenderer<SurveyStakeBlockEntity, SurveyStakeRenderer.State> {
	private static final Identifier TEXTURE = Jugcraft.id("textures/misc/blueprint_ghost.png");
	private static final RenderType RENDER_TYPE = RenderTypes.entityTranslucentEmissive(TEXTURE);
	private static final float[] UV = {0, 0, 1, 1};
	private static final int MISSING = 0x6A4F9DFF;
	private static final int HAND_ONLY = 0x8CFFB040;
	private static final int WRONG = 0xB0FF4030;
	private static final int MAX_GHOSTS = 8192;
	private static final Map<String, List<Blueprint.Cell>> TURNED = new HashMap<>();
	/** Large builds: ghosts only within this many blocks of the camera, a few layers from the lowest unfinished one. */
	private static final int LARGE_REACH = 96;
	/** Large builds: the client's copy of each placed one, and its ghost list (worked out twice a second). */
	private static final Map<SurveyStakeBlockEntity, Large> LARGE = new java.util.WeakHashMap<>();

	private static final class Large {
		final io.github.jimbozoomer.jugcraft.blueprint.BuildSite site;
		final List<float[]> ghosts = new ArrayList<>();
		long refreshed = Long.MIN_VALUE;
		long cleared;

		Large(io.github.jimbozoomer.jugcraft.blueprint.BuildSite site) {
			this.site = site;
		}
	}

	public static class State extends BlockEntityRenderState {
		final List<float[]> ghosts = new ArrayList<>();
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
	public int getViewDistance() {
		return 384;
	}

	private static List<Blueprint.Cell> cells(SurveyStakeBlockEntity stake) {
		Blueprint blueprint = stake.blueprint();
		if (blueprint == null) {
			return List.of();
		}
		Rotation rotation = stake.rotation();
		return TURNED.computeIfAbsent(blueprint.id + "/" + rotation.ordinal(), key -> blueprint.cells(rotation));
	}

	@Override
	public void extractRenderState(SurveyStakeBlockEntity stake, State state, float partialTick, Vec3 cameraPos,
			ModelFeatureRenderer.CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(stake, state, crumbling);
		state.ghosts.clear();
		Level level = stake.getLevel();
		if (level == null) {
			return;
		}
		BlockPos origin = stake.getBlockPos();
		BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
		Blueprint blueprint = stake.blueprint();
		if (blueprint != null && blueprint.large()) {
			state.ghosts.addAll(large(stake, blueprint, level, cameraPos));
			return;
		}
		for (Blueprint.Cell cell : cells(stake)) {
			if (state.ghosts.size() >= MAX_GHOSTS) {
				break;
			}
			if (io.github.jimbozoomer.jugcraft.blueprint.BuildSite.fluid(cell.state())) {
				continue;
			}
			at.setWithOffset(origin, cell.offset());
			BlockState now = level.getBlockState(at);
			if (SurveyStakeBlockEntity.matches(now, cell.state(), cell.anyOf())) {
				continue;
			}
			int color = !now.canBeReplaced() ? WRONG : SurveyStakeBlockEntity.handOnly(cell.state()) ? HAND_ONLY : MISSING;
			state.ghosts.add(new float[] {cell.offset().getX(), cell.offset().getY(), cell.offset().getZ(), Float.intBitsToFloat(color)});
		}
	}

	/**
	 * A large build's ghosts: near the camera only, from the lowest unfinished layer there up a few layers, so a whole
	 * power plant costs no more to draw than a small blueprint.
	 */
	private static List<float[]> large(SurveyStakeBlockEntity stake, Blueprint blueprint, Level level, Vec3 camera) {
		Large large = LARGE.get(stake);
		if (large == null || large.site.blueprint != blueprint || large.site.rotation != stake.rotation()) {
			large = new Large(new io.github.jimbozoomer.jugcraft.blueprint.BuildSite(blueprint, stake.rotation(), stake.getBlockPos()));
			LARGE.put(stake, large);
		}
		long now = level.getGameTime();
		if (now - large.refreshed < 10 && now >= large.refreshed) {
			return large.ghosts;
		}
		large.refreshed = now;
		large.ghosts.clear();
		if (Math.abs(now - large.cleared) > 600) {
			// Now and then forget what was seen in place, so a block broken since shows again.
			large.site.done.clear();
			large.cleared = now;
		}
		var site = large.site;
		BlockPos origin = stake.getBlockPos();
		BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
		int lowest = -1;
		double reach = (double) LARGE_REACH * LARGE_REACH;
		for (int i = site.done.nextClearBit(0); i < site.size(); i = site.done.nextClearBit(i + 1)) {
			at.set(site.pos(i));
			double dx = at.getX() + 0.5 - camera.x, dz = at.getZ() + 0.5 - camera.z;
			if (dx * dx + dz * dz > reach) {
				continue;
			}
			int layer = site.layerOf(i);
			if (lowest >= 0 && layer > lowest + SurveyStakeBlockEntity.LAYER_WINDOW + 1) {
				break;
			}
			BlockState now2 = level.getBlockState(at);
			if (site.matches(now2, i)) {
				site.done.set(i);
				continue;
			}
			if (lowest < 0) {
				lowest = layer;
			}
			if (large.ghosts.size() >= MAX_GHOSTS) {
				break;
			}
			int color = !now2.canBeReplaced() ? WRONG : SurveyStakeBlockEntity.handOnly(site.state(i)) ? HAND_ONLY : MISSING;
			large.ghosts.add(new float[] {at.getX() - origin.getX(), at.getY() - origin.getY(), at.getZ() - origin.getZ(), Float.intBitsToFloat(color)});
		}
		return large.ghosts;
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		if (state.ghosts.isEmpty()) {
			return;
		}
		List<float[]> ghosts = List.copyOf(state.ghosts);
		collector.submitCustomGeometry(poseStack, RENDER_TYPE, (pose, consumer) -> {
			DroneDepotRenderer.Geometry g = new DroneDepotRenderer.Geometry(pose, consumer);
			g.light = LightCoordsUtil.FULL_BRIGHT;
			for (float[] ghost : ghosts) {
				int color = Float.floatToRawIntBits(ghost[3]);
				g.color = color;
				float x = ghost[0], y = ghost[1], z = ghost[2];
				if (color == WRONG) {
					// A slightly larger shell round the block in the way.
					g.box(x - 0.02, y - 0.02, z - 0.02, x + 1.02, y + 1.02, z + 1.02, UV);
				} else {
					g.box(x + 0.06, y + 0.06, z + 0.06, x + 0.94, y + 0.94, z + 0.94, UV);
				}
			}
		});
	}
}
