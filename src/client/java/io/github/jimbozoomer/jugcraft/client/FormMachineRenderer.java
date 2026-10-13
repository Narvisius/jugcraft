package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jimbozoomer.jugcraft.machine.Footprint;
import io.github.jimbozoomer.jugcraft.machine.MachineBlock;
import io.github.jimbozoomer.jugcraft.machine.form.FormMachineBlockEntity;
import java.util.List;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws the turning parts of industrial machine forms ({@link MachineRotors}): the Gas Burning Generator's coupling and
 * intake fan. Their block models leave these parts out; they spin up when the machine starts and run down when it stops.
 */
public class FormMachineRenderer implements BlockEntityRenderer<FormMachineBlockEntity, FormMachineRenderer.State> {
	public static final class State extends BlockEntityRenderState {
		List<MachineRotors.Spin> spins = List.of();
	}

	public FormMachineRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(FormMachineBlockEntity machine, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(machine, state, crumbling);
		state.spins = MachineRotors.extract(machine, partialTick);
		Level level = machine.getLevel();
		if (!state.spins.isEmpty() && level != null) {
			// Lit as the inside of the machine where they turn, not as the controller's block (which glows while it runs).
			float[] center = state.spins.getFirst().rotor().center();
			Direction facing = machine.getBlockState().getValue(MachineBlock.FACING);
			Vec3i cell = new Vec3i(Mth.floor(center[0]), Mth.floor(center[1]), Mth.floor(center[2]));
			state.lightCoords = LightCoordsUtil.getLightCoords(level, machine.getBlockPos().offset(Footprint.rotate(cell, facing)));
		}
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		MachineRotors.submit(state.spins, pose, collector, state.lightCoords);
	}

	@Override
	public boolean shouldRenderOffScreen() {
		return true; // A form's moving parts can be blocks away from its controller, which may be off screen.
	}

	@Override
	public int getViewDistance() {
		return 96;
	}
}
