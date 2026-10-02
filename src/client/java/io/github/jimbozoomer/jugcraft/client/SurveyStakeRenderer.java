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
 * world: a ghost in the blueprint's colour where a block is still missing (blue complete, green part, red imported),
 * a violet ghost where it must be placed by hand (blocks with their own block entity, like the Tower Core), and an
 * orange outline where a wrong block is in the way.
 * Blocks already in place show nothing.
 */
public class SurveyStakeRenderer implements BlockEntityRenderer<SurveyStakeBlockEntity, SurveyStakeRenderer.State> {
	private static final Identifier TEXTURE = Jugcraft.id("textures/misc/blueprint_ghost.png");
	private static final RenderType RENDER_TYPE = RenderTypes.entityTranslucentEmissive(TEXTURE);
	private static final float[] UV = {0, 0, 1, 1};
	/** Hand-only cells (the Tower Core, a terminal): violet. Wrong blocks in the way: orange. Missing: the blueprint's colour. */
	private static final int HAND_ONLY = 0x8CB478FF;
	private static final int WRONG = 0xB0FF8C1E;
	private static final int MAX_GHOSTS = 8192;
	private static final Map<String, List<Blueprint.Cell>> TURNED = new HashMap<>();

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
		return 160;
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
		Blueprint blueprint = stake.blueprint();
		int missing = (blueprint != null ? blueprint.kind : Blueprint.Kind.COMPLETE).argb(0x6A);
		BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
		for (Blueprint.Cell cell : cells(stake)) {
			if (state.ghosts.size() >= MAX_GHOSTS) {
				break;
			}
			at.setWithOffset(origin, cell.offset());
			BlockState now = level.getBlockState(at);
			if (SurveyStakeBlockEntity.matches(now, cell.state())) {
				continue;
			}
			int color = !now.canBeReplaced() ? WRONG : SurveyStakeBlockEntity.handOnly(cell.state()) ? HAND_ONLY : missing;
			state.ghosts.add(new float[] {cell.offset().getX(), cell.offset().getY(), cell.offset().getZ(), Float.intBitsToFloat(color)});
		}
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
