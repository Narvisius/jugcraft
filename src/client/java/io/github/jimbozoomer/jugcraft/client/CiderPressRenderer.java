package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.CiderPressBlock;
import io.github.jimbozoomer.jugcraft.agriculture.CiderPressBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * What moves in a Cider Press (the block model is its frame), drawn for a press facing north and turned to its facing:
 * the apples waiting in the grinder's hopper, the ground pulp in the basket (squashed lower as the screw goes down), the
 * pressing plate and screw (resting up under the beam until pressing starts, then down on the pulp), the capstan bar on
 * the beam (a quarter turn for each turn of the screw), and the juice in the trough.
 */
public class CiderPressRenderer implements BlockEntityRenderer<CiderPressBlockEntity, CiderPressRenderer.State> {
	private static final RenderType APPLE = RenderTypes.entityCutout(Jugcraft.id("textures/entity/cider_apple.png"));
	private static final RenderType PULP = RenderTypes.entityCutout(Jugcraft.id("textures/entity/cider_pulp.png"));
	private static final RenderType JUICE = RenderTypes.entityCutout(Jugcraft.id("textures/entity/cider_juice.png"));
	private static final RenderType WOOD = RenderTypes.entityCutout(Jugcraft.id("textures/block/cider_press_wood.png"));
	private static final RenderType IRON = RenderTypes.entityCutout(Jugcraft.id("textures/block/cider_press_iron.png"));
	private static final int WHITE = 0xFFFFFFFF;
	/** The basket's inside, in pixels: its floor, its rim, and how high a full basket of pulp stands. */
	private static final float BASKET_FLOOR = 2.0F;
	private static final float FULL_PULP = 6.5F;
	/** How far down a fully pressed cheese is squashed. */
	private static final float SQUASH = 0.55F;
	private static final float PLATE_REST = 11.0F;

	public static final class State extends BlockEntityRenderState {
		int apples;
		int pulp;
		int turns;
		int juice;
		Direction facing = Direction.NORTH;
	}

	public CiderPressRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(CiderPressBlockEntity press, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(press, state, crumbling);
		state.apples = press.apples();
		state.pulp = press.pulp();
		state.turns = press.turns();
		state.juice = press.juice();
		state.facing = press.getBlockState().hasProperty(CiderPressBlock.FACING) ? press.getBlockState().getValue(CiderPressBlock.FACING)
				: Direction.NORTH;
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		int light = state.lightCoords;
		pose.pushPose();
		pose.translate(0.5F, 0.0F, 0.5F);
		pose.rotateDegrees(Axis.YP, -RockingChairRenderer.yRotation(state.facing));
		pose.translate(-0.5F, 0.0F, -0.5F);

		// Apples waiting in the hopper, four to a row, two rows deep.
		if (state.apples > 0) {
			collector.submitCustomGeometry(pose, APPLE, (matrix, buffer) -> {
				for (int i = 0; i < state.apples; i++) {
					float x = 4.2F + (i % 4) * 1.95F;
					float y = 12.5F + (i / 4) * 1.4F;
					float z = 13.3F + (i / 4) * 0.3F;
					TintedBoxes.box(buffer, matrix, x, y, z, x + 1.8F, y + 1.8F, z + 1.8F, WHITE, light);
				}
			});
		}

		// The pulp in the basket, squashed as the screw goes down; the plate rests on it while pressing.
		float pulpTop = BASKET_FLOOR + FULL_PULP * state.pulp / CiderPressBlockEntity.CAPACITY
				* (1.0F - SQUASH * state.turns / CiderPressBlockEntity.TURNS);
		if (state.pulp > 0) {
			collector.submitCustomGeometry(pose, PULP, (matrix, buffer) ->
					TintedBoxes.box(buffer, matrix, 4, BASKET_FLOOR, 4, 12, pulpTop, 12, WHITE, light));
		}
		float plate = state.turns > 0 ? pulpTop : PLATE_REST;
		collector.submitCustomGeometry(pose, WOOD, (matrix, buffer) ->
				TintedBoxes.box(buffer, matrix, 4.25F, plate, 4.25F, 11.75F, plate + 1, 11.75F, WHITE, light));
		collector.submitCustomGeometry(pose, IRON, (matrix, buffer) ->
				TintedBoxes.box(buffer, matrix, 7.25F, plate + 1, 7.25F, 8.75F, 15.0F, 8.75F, WHITE, light));

		// The capstan bar on the nut, a quarter turn a turn of the screw.
		pose.pushPose();
		pose.translate(0.5F, 0.0F, 0.5F);
		pose.rotateDegrees(Axis.YP, 90.0F * state.turns);
		pose.translate(-0.5F, 0.0F, -0.5F);
		collector.submitCustomGeometry(pose, IRON, (matrix, buffer) ->
				TintedBoxes.box(buffer, matrix, 2.5F, 16, 7.5F, 13.5F, 17, 8.5F, WHITE, light));
		pose.popPose();

		// The juice in the trough.
		if (state.juice > 0) {
			float level = 1.0F + 1.4F * state.juice / CiderPressBlockEntity.TROUGH;
			collector.submitCustomGeometry(pose, JUICE, (matrix, buffer) -> TintedBoxes.top(buffer, matrix, 1, 1, 15, 15, level, WHITE, light));
		}
		pose.popPose();
	}
}
