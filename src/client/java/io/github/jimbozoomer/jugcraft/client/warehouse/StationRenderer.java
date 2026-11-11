package io.github.jimbozoomer.jugcraft.client.warehouse;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.warehouse.CaseLoad;
import io.github.jimbozoomer.jugcraft.warehouse.Kinematics;
import io.github.jimbozoomer.jugcraft.warehouse.PalletLoad;
import io.github.jimbozoomer.jugcraft.warehouse.PieceBlock;
import io.github.jimbozoomer.jugcraft.warehouse.StationBlockEntity;
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
 * The Palletizing Station's live parts: the empty pallets stacked in its magazine, the bottom one pushed out along the
 * chain conveyor onto the turntable, the pallet being built with its cases, turned on the turntable while it is
 * wrapped, then standing wrapped (or strapped) and ready.
 */
public class StationRenderer implements BlockEntityRenderer<StationBlockEntity, StationRenderer.State> {
	public static class State extends BlockEntityRenderState {
		Direction facing = Direction.NORTH;
		int magazine, look = -1;
		float slide = -1, turn;
		final List<Integer> cases = new ArrayList<>();
	}

	public StationRenderer(BlockEntityRendererProvider.Context context) {
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
	public void extractRenderState(StationBlockEntity station, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(station, state, crumbling);
		state.facing = station.getBlockState().getBlock() instanceof PieceBlock ? station.getBlockState().getValue(PieceBlock.FACING) : Direction.NORTH;
		double time = station.getLevel() == null ? 0 : station.getLevel().getGameTime() + partialTick;
		state.magazine = station.magazine();
		state.slide = -1;
		state.look = -1;
		state.turn = 0;
		state.cases.clear();
		PalletLoad build = station.build();
		switch (station.state()) {
			case StationBlockEntity.DISPENSING -> state.slide = (float) Math.min(1, (time - station.since()) / StationBlockEntity.DISPENSE_TICKS);
			case StationBlockEntity.RETURNING -> state.slide = (float) Math.max(0, 1 - (time - station.since()) / StationBlockEntity.DISPENSE_TICKS);
			case StationBlockEntity.BUILDING -> {
				state.look = 0;
				if (build != null) {
					for (CaseLoad c : build.cases()) {
						state.cases.add(c.kind().ordinal());
					}
				}
			}
			case StationBlockEntity.WRAPPING -> {
				state.look = build == null ? 0 : build.look();
				state.turn = (float) ((time - station.since()) * 6.0);
			}
			case StationBlockEntity.READY -> state.look = build == null ? 0 : build.look();
			default -> {
			}
		}
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		pose.pushPose();
		Page.pieceFrame(pose, state.facing, WarehousePiece.STATION);
		var station = WarehouseLayout.get().section("station");
		float tableY = station.get("table_y").getAsFloat(), magY = station.get("magazine_y").getAsFloat();
		float step = (float) Kinematics.PALLET_H + 0.02F;
		for (int k = 0; k < state.magazine; k++) {
			pose.pushPose();
			pose.translate(0, magY + k * step, 0);
			WhQuads.submit(collector, pose, WhQuads.loadQuads(0), state.lightCoords, WhQuads.Tint.NONE);
			pose.popPose();
		}
		if (state.slide >= 0) {
			pose.pushPose();
			pose.translate(0, tableY, 16 * state.slide);
			WhQuads.submit(collector, pose, WhQuads.loadQuads(0), state.lightCoords, WhQuads.Tint.NONE);
			pose.popPose();
		}
		if (state.look >= 0) {
			pose.pushPose();
			pose.translate(8, tableY, 24);
			pose.rotateDegrees(Axis.YP, state.turn);
			pose.translate(-8, 0, -8);
			WhQuads.submit(collector, pose, WhQuads.loadQuads(state.look), state.lightCoords, WhQuads.Tint.NONE);
			String[] kinds = {"case_carton", "case_carton_w", "case_sack", "case_crate", "case_drum", "case_ingot"};
			for (int i = 0; i < state.cases.size(); i++) {
				double[] spot = Kinematics.SPOTS[i % 4];
				pose.pushPose();
				pose.translate(spot[0], Kinematics.PALLET_H + (i / 4) * (Kinematics.CASE_H + 0.05), spot[1]);
				WhQuads.submitPart(collector, pose, kinds[state.cases.get(i)], state.lightCoords, WhQuads.Tint.NONE);
				pose.popPose();
			}
			pose.popPose();
		}
		pose.popPose();
	}
}
