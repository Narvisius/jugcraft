package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.machine.MachineBlock;
import io.github.jimbozoomer.jugcraft.machine.MachineBlockEntity;
import io.github.jimbozoomer.jugcraft.machine.MachineKind;
import java.util.List;
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
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws the wind turbine's rotor: eight wooden vanes on a brass hub, about seven blocks across, in
 * front of the nacelle on top of the nine-block tower. It turns while the turbine generates (the
 * master block's LIT state). The rotor is too big for a block model, so it is drawn here. It also
 * draws the turning parts of other multi-block machines ({@link MachineRotors}: the sawmill's blade,
 * the sieve's weights); the rest of every machine is a normal block model.
 */
public class WindTurbineRenderer implements BlockEntityRenderer<MachineBlockEntity, WindTurbineRenderer.State> {
	private static final RenderType VANE = RenderTypes.entitySolid(Jugcraft.id("textures/block/sp_vane.png"));
	private static final RenderType HUB = RenderTypes.entitySolid(Jugcraft.id("textures/block/sp_brass_plate.png"));
	private static final RenderType SPAR = RenderTypes.entitySolid(Jugcraft.id("textures/block/sp_iron.png"));
	/** Hub center relative to the master block (blocks), for a north-facing turbine: see steampunk_models.wind_turbine. */
	private static final float HUB_X = 0.5F;
	private static final float HUB_Y = 136 / 16.0F;
	private static final float HUB_Z = -0.1F;
	/** Rotor speed while running, in degrees per tick. */
	private static final float DEGREES_PER_TICK = 4.0F;
	private static final int VANES = 8;
	/** Vane span from the hub (blocks). */
	private static final float VANE_INNER = 0.45F;
	private static final float VANE_OUTER = 3.5F;

	public static final class State extends BlockEntityRenderState {
		boolean turbine;
		List<MachineRotors.Spin> spins = List.of();
		Direction facing = Direction.NORTH;
		float angle;
	}

	public WindTurbineRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(MachineBlockEntity machine, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(machine, state, crumbling);
		state.spins = MachineRotors.extract(machine, partialTick);
		state.turbine = machine.kind() == MachineKind.WIND_TURBINE;
		if (!state.turbine) {
			return;
		}
		BlockState block = machine.getBlockState();
		state.facing = block.getValue(MachineBlock.FACING);
		long time = machine.getLevel() == null ? 0 : machine.getLevel().getGameTime();
		state.angle = block.getValue(MachineBlock.LIT) ? ((time % 360) + partialTick) * DEGREES_PER_TICK % 360 : 0;
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		MachineRotors.submit(state.spins, pose, collector, state.lightCoords);
		if (!state.turbine) {
			return;
		}
		int light = state.lightCoords;
		pose.pushPose();
		// Turn the north-facing layout to the machine's facing, about the master block's center.
		pose.translate(0.5F, 0.0F, 0.5F);
		pose.rotateDegrees(Axis.YP, -yRotation(state.facing));
		pose.translate(-0.5F, 0.0F, -0.5F);
		pose.translate(HUB_X, HUB_Y, HUB_Z);
		pose.rotateDegrees(Axis.ZP, state.angle);
		// Hub.
		cuboid(collector, pose, HUB, -0.28F, -0.28F, -0.2F, 0.28F, 0.28F, 0.0F, light);
		for (int vane = 0; vane < VANES; vane++) {
			pose.pushPose();
			pose.rotateDegrees(Axis.ZP, vane * 360.0F / VANES);
			// Iron spar from the hub to the tip, and the wooden vane on it, widening outwards.
			cuboid(collector, pose, SPAR, -0.04F, 0.2F, -0.14F, 0.04F, VANE_OUTER, -0.06F, light);
			cuboid(collector, pose, VANE, -0.18F, VANE_INNER, -0.12F, 0.18F, 1.6F, -0.08F, light);
			cuboid(collector, pose, VANE, -0.3F, 1.6F, -0.12F, 0.3F, VANE_OUTER, -0.08F, light);
			pose.popPose();
		}
		pose.popPose();
	}

	/** Degrees the blockstate turns a north-facing model (clockwise seen from above), as in the blockstates. */
	private static float yRotation(Direction facing) {
		return switch (facing) {
			case EAST -> 90;
			case SOUTH -> 180;
			case WEST -> 270;
			default -> 0;
		};
	}

	/** A textured box from (x0, y0, z0) to (x1, y1, z1) in the current pose; each face shows the whole texture. */
	private static void cuboid(SubmitNodeCollector collector, PoseStack pose, RenderType type,
			float x0, float y0, float z0, float x1, float y1, float z1, int light) {
		collector.submitCustomGeometry(pose, type, (matrix, buffer) -> {
			quad(buffer, matrix, light, 0, 0, -1, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0);
			quad(buffer, matrix, light, 0, 0, 1, x1, y0, z1, x1, y1, z1, x0, y1, z1, x0, y0, z1);
			quad(buffer, matrix, light, -1, 0, 0, x0, y0, z1, x0, y1, z1, x0, y1, z0, x0, y0, z0);
			quad(buffer, matrix, light, 1, 0, 0, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1);
			quad(buffer, matrix, light, 0, 1, 0, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0);
			quad(buffer, matrix, light, 0, -1, 0, x0, y0, z1, x0, y0, z0, x1, y0, z0, x1, y0, z1);
		});
	}

	private static void quad(VertexConsumer buffer, PoseStack.Pose matrix, int light, float nx, float ny, float nz,
			float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz, float dx, float dy, float dz) {
		vertex(buffer, matrix, light, nx, ny, nz, ax, ay, az, 0, 1);
		vertex(buffer, matrix, light, nx, ny, nz, bx, by, bz, 0, 0);
		vertex(buffer, matrix, light, nx, ny, nz, cx, cy, cz, 1, 0);
		vertex(buffer, matrix, light, nx, ny, nz, dx, dy, dz, 1, 1);
	}

	private static void vertex(VertexConsumer buffer, PoseStack.Pose matrix, int light, float nx, float ny, float nz,
			float x, float y, float z, float u, float v) {
		buffer.addVertex(matrix, x, y, z).setColor(0xFFFFFFFF).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
				.setLight(light).setNormal(matrix, nx, ny, nz);
	}

	@Override
	public boolean shouldRenderOffScreen() {
		return true; // The rotors are far from the master block (the turbine's nine blocks up), which may be off screen.
	}

	@Override
	public int getViewDistance() {
		return 160;
	}
}
