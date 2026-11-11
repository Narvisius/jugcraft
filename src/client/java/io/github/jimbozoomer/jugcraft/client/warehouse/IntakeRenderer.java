package io.github.jimbozoomer.jugcraft.client.warehouse;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jimbozoomer.jugcraft.warehouse.CaseLoad;
import io.github.jimbozoomer.jugcraft.warehouse.IntakeBlockEntity;
import io.github.jimbozoomer.jugcraft.warehouse.Kinematics;
import io.github.jimbozoomer.jugcraft.warehouse.PieceBlock;
import io.github.jimbozoomer.jugcraft.warehouse.WarehouseLayout;
import io.github.jimbozoomer.jugcraft.warehouse.WarehousePiece;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The Intake Case Packer's live parts: the cases running along its accumulation conveyor to the stop (where they queue),
 * its HMI's page (status, cases packed, on the conveyor), and its stack light (green packing, amber the conveyor full,
 * blue idle).
 */
public class IntakeRenderer implements BlockEntityRenderer<IntakeBlockEntity, IntakeRenderer.State> {
	private final net.minecraft.client.gui.Font font;

	public static class State extends BlockEntityRenderState {
		Direction facing = Direction.NORTH;
		final List<float[]> cases = new ArrayList<>();
		int lamp;
		@Nullable Page hmi;
	}

	public IntakeRenderer(BlockEntityRendererProvider.Context context) {
		this.font = context.font();
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
	public void extractRenderState(IntakeBlockEntity intake, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(intake, state, crumbling);
		state.facing = intake.getBlockState().getBlock() instanceof PieceBlock ? intake.getBlockState().getValue(PieceBlock.FACING) : Direction.NORTH;
		state.cases.clear();
		double time = intake.getLevel() == null ? 0 : intake.getLevel().getGameTime() + partialTick;
		List<IntakeBlockEntity.Conveyed> conveyor = intake.conveyor();
		boolean moving = false;
		for (int i = 0; i < conveyor.size(); i++) {
			float x = intake.position(i, time);
			CaseLoad c = conveyor.get(i).load();
			state.cases.add(new float[] {x, c.kind().ordinal()});
			moving |= x < IntakeBlockEntity.slotX(i) - 0.01F;
		}
		boolean packing = intake.packing() != null;
		boolean full = conveyor.size() >= IntakeBlockEntity.CONVEYOR;
		state.lamp = full ? 2 : packing || moving ? 1 : 0;
		Page page = new Page(font);
		page.title(150, "INTAKE", "WH-1");
		page.add(0, 16, 1, "STATUS", Page.GREY).right(150, 16, 1, full ? "CONVEYOR FULL" : packing ? "PACKING" : moving ? "RUNNING" : "IDLE",
				full ? Page.AMBER : packing || moving ? Page.GREEN : Page.CYAN);
		page.add(0, 30, 1, "PACKING", Page.GREY).right(150, 30, 1, packing ? intake.packing().count() + " " + shortName(intake.packing()) : "-", Page.WHITE);
		page.add(0, 44, 1, "ON CONVEYOR", Page.GREY).right(150, 44, 1, conveyor.size() + " / " + IntakeBlockEntity.CONVEYOR, Page.WHITE);
		page.add(0, 58, 1, "CASES PACKED", Page.GREY).right(150, 58, 1, String.valueOf(intake.packed()), Page.WHITE);
		if (packing && intake.getLevel() != null) {
			double f = Math.min(1, (time - intake.packStart()) / IntakeBlockEntity.PACK_TICKS);
			page.bar(0, 74, 1, 18, f, Page.GREEN);
		}
		state.hmi = page;
	}

	private static String shortName(CaseLoad c) {
		String name = c.item().getHoverName().getString();
		return name.length() > 12 ? name.substring(0, 11) + "." : name;
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		pose.pushPose();
		Page.pieceFrame(pose, state.facing, WarehousePiece.INTAKE);
		WarehouseLayout layout = WarehouseLayout.get();
		float y = (float) layout.number("intake", "conveyor_y"), z = (float) layout.number("intake", "conveyor_z") - (float) Kinematics.CASE / 2;
		String[] kinds = {"case_carton", "case_carton_w", "case_sack", "case_crate", "case_drum", "case_ingot"};
		for (float[] c : state.cases) {
			pose.pushPose();
			pose.translate(c[0], y, z);
			WhQuads.submitPart(collector, pose, kinds[(int) c[1]], state.lightCoords, WhQuads.Tint.NONE);
			pose.popPose();
		}
		// The stack light: k 0 blue (bottom), 1 green, 2 amber, 3 red.
		var stack = layout.section("intake").getAsJsonObject("stack");
		int k = state.lamp == 2 ? 2 : state.lamp == 1 ? 1 : 0;
		String[] lamps = {"lamp_blue", "lamp_green", "lamp_amber", "lamp_red"};
		pose.pushPose();
		pose.translate(stack.get("x").getAsFloat() - 8, stack.get("y0").getAsFloat() + k * stack.get("step").getAsFloat(), stack.get("z").getAsFloat() - 8);
		WhQuads.submitPart(collector, pose, lamps[k], state.lightCoords, WhQuads.Tint.NONE);
		pose.popPose();
		if (state.hmi != null) {
			var hmi = layout.section("intake").getAsJsonObject("hmi");
			float x0 = hmi.get("x0").getAsFloat(), x1 = hmi.get("x1").getAsFloat(), y1 = hmi.get("y1").getAsFloat();
			float scale = (x1 - x0 - 0.6F) / 150F;
			state.hmi.draw(pose, collector, x0 + 0.3F, y1 - 0.3F, hmi.get("z").getAsFloat(), scale);
		}
		pose.popPose();
	}
}
