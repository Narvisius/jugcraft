package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.agriculture.DecorationBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.LabTableBlock;
import io.github.jimbozoomer.jugcraft.agriculture.MourningAngelBlock;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws the Lab Table's patient on the foot block: legs under the sheet, and the torso and head, which sit up about the
 * hips while the table is powered ({@link LabTableBlock#lean}) and twitch at night. Woken by a Lightning Harness
 * ({@link LabTableBlock#awake}), it sits bolt upright with its arms out and its eyes flashing (decor19_quads.json:
 * {@code lab_table_arms}, {@code lab_table_eyes}). Coordinates are in pixels of the foot block, for a table whose head
 * lies to the north.
 */
public class LabTableRenderer implements BlockEntityRenderer<DecorationBlockEntity, LabTableRenderer.State> {
	/** The hips, about which the patient sits up. */
	private static final float HIP_Y = 13.5F;
	private static final float HIP_Z = 2.0F;
	/** Each table's patient as this client last left it: {lean, game time}. */
	private final Map<DecorationBlockEntity, double[]> patients = new WeakHashMap<>();

	public static final class State extends BlockEntityRenderState {
		Direction facing = Direction.NORTH;
		float lean;
		float twitch;
		boolean woken;
		boolean flash;
	}

	public LabTableRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(DecorationBlockEntity table, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(table, state, crumbling);
		BlockState block = table.getBlockState();
		Level level = table.getLevel();
		if (!(block.getBlock() instanceof LabTableBlock) || level == null) {
			return;
		}
		state.facing = block.getValue(LabTableBlock.FACING);
		state.woken = LabTableBlock.awake(table.marked(), level.getGameTime());
		state.flash = state.woken && Math.floorMod(level.getGameTime(), 6) < 3;
		boolean powered = block.getValue(LabTableBlock.POWERED) || state.woken;
		double now = level.getGameTime() + (double) partialTick;
		double[] patient = patients.computeIfAbsent(table, t -> new double[] {powered ? LabTableBlock.SIT_DEGREES : 0.0, now});
		float elapsed = (float) Math.max(0.0, Math.min(20.0, now - patient[1]));
		patient[0] = LabTableBlock.lean((float) patient[0], powered, elapsed);
		patient[1] = now;
		state.lean = (float) patient[0];
		state.twitch = LabTableBlock.twitching(table.getBlockPos(), MourningAngelBlock.night(level), level.getGameTime())
				? 4.0F * Mth.sin((float) now * 2.7F) : 0.0F;
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		QuadModel legs = DecorQuads.get("lab_table_legs");
		QuadModel torso = DecorQuads.get("lab_table_torso");
		pose.pushPose();
		pose.translate(0.5F, 0.0F, 0.5F);
		pose.rotateDegrees(Axis.YP, -RockingChairRenderer.yRotation(state.facing));
		pose.translate(-0.5F, 0.0F, -0.5F);
		if (legs != null) {
			legs.submit(pose, collector, state.lightCoords);
		}
		if (torso != null) {
			pose.translate(0.0F, HIP_Y / 16, HIP_Z / 16);
			pose.rotateDegrees(Axis.XP, state.lean + state.twitch);
			pose.translate(0.0F, -HIP_Y / 16, -HIP_Z / 16);
			torso.submit(pose, collector, state.lightCoords);
			QuadModel arms = state.woken ? DecorQuads.get("lab_table_arms") : null;
			if (arms != null) {
				arms.submit(pose, collector, state.lightCoords);
			}
			QuadModel eyes = state.flash ? DecorQuads.get("lab_table_eyes") : null;
			if (eyes != null) {
				eyes.submit(pose, collector, 0xF000F0);
			}
		}
		pose.popPose();
	}

	@Override
	public boolean shouldRenderOffScreen() {
		return true; // The patient lies across both blocks.
	}
}
