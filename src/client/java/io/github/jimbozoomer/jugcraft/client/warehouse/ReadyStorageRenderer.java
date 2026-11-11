package io.github.jimbozoomer.jugcraft.client.warehouse;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jimbozoomer.jugcraft.warehouse.CaseLoad;
import io.github.jimbozoomer.jugcraft.warehouse.Kinematics;
import io.github.jimbozoomer.jugcraft.warehouse.PalletLoad;
import io.github.jimbozoomer.jugcraft.warehouse.PieceBlock;
import io.github.jimbozoomer.jugcraft.warehouse.ReadyStorageBlockEntity;
import io.github.jimbozoomer.jugcraft.warehouse.TerminalPayload;
import io.github.jimbozoomer.jugcraft.warehouse.WarehouseLayout;
import io.github.jimbozoomer.jugcraft.warehouse.WarehousePiece;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Ready Storage's live parts (part W2), from its timeline (ReadyStorageBlockEntity): the dispatch hatch's shutter rolling
 * up and down, the pallet on the roller bed rolling in and out, the pick cell's gantry lifting each case the order needs
 * off the pallet and handing it into the lift (a split case comes back lighter), the drive on top of the top module
 * (its beacon lit), and the operator screen (what the hatch is doing, the order, the slots used).
 */
public class ReadyStorageRenderer implements BlockEntityRenderer<ReadyStorageBlockEntity, ReadyStorageRenderer.State> {
	private static final float WIDTH = 150;
	private static final String[] KINDS = {"case_carton", "case_carton_w", "case_sack", "case_crate", "case_drum", "case_ingot"};
	private final Font font;

	public static class State extends BlockEntityRenderState {
		Direction facing = Direction.NORTH;
		float shutter, palletZ = -1, gx, gz, headY;
		final List<Integer> cases = new ArrayList<>();
		final List<Boolean> shown = new ArrayList<>();
		int carried = -1, modules = 1;
		@Nullable Page page;
	}

	public ReadyStorageRenderer(BlockEntityRendererProvider.Context context) {
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
	public int getViewDistance() {
		return 64;
	}

	private static float lerp(float a, float b, float f) {
		return a + (b - a) * Math.max(0, Math.min(1, f));
	}

	@Override
	public void extractRenderState(ReadyStorageBlockEntity rs, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(rs, state, crumbling);
		state.facing = rs.getBlockState().getBlock() instanceof PieceBlock ? rs.getBlockState().getValue(PieceBlock.FACING) : Direction.NORTH;
		JsonObject l = WarehouseLayout.get().section("ready_storage");
		float bedZ = l.get("bed_z").getAsFloat(), cellZ = l.get("cell_z").getAsFloat(), carry = l.get("carry_y").getAsFloat();
		JsonArray park = l.getAsJsonArray("park"), drop = l.getAsJsonArray("drop");
		float px = park.get(0).getAsFloat(), pz = park.get(1).getAsFloat(), py = park.get(2).getAsFloat();
		float dx = drop.get(0).getAsFloat(), dz = drop.get(1).getAsFloat();
		float bedY = l.get("bed_y").getAsFloat();
		double time = rs.getLevel() == null ? 0 : rs.getLevel().getGameTime() + partialTick;
		state.shutter = 0;
		state.palletZ = -1;
		state.gx = px;
		state.gz = pz;
		state.headY = py;
		state.carried = -1;
		state.cases.clear();
		state.shown.clear();
		PalletLoad pallet = rs.pallet();
		if (pallet != null) {
			for (CaseLoad c : pallet.cases()) {
				state.cases.add(c.kind().ordinal());
				state.shown.add(true);
			}
		}
		String doing = "READY";
		if (rs.state() == ReadyStorageBlockEntity.BED) {
			state.palletZ = bedZ;
			doing = "PALLET OUT";
		} else if (rs.state() == ReadyStorageBlockEntity.WORKING && pallet != null) {
			float t = (float) (time - rs.start());
			int s = ReadyStorageBlockEntity.SHUTTER, roll = ReadyStorageBlockEntity.ROLL;
			int pick0 = s + roll + s, pick1 = pick0 + rs.pickTicks();
			if (t < s) {
				state.shutter = t / s;
				state.palletZ = bedZ;
				doing = "OPENING";
			} else if (t < s + roll) {
				state.shutter = 1;
				state.palletZ = lerp(bedZ, cellZ, (t - s) / roll);
				doing = "PALLET IN";
			} else if (t < pick0) {
				state.shutter = 1 - (t - s - roll) / s;
				state.palletZ = cellZ;
				doing = "CLOSING";
			} else if (t < pick1) {
				state.palletZ = cellZ;
				doing = "PICKING";
				gantry(state, rs, t - pick0, bedY, cellZ, px, pz, py, dx, dz, carry);
			} else if (rs.keep()) {
				// What is left rolls back out: the cases taken are gone.
				List<int[]> picks = rs.picks();
				for (int[] p : picks) {
					if (p[1] == 0) {
						state.shown.set(p[0], false);
					}
				}
				float u = t - pick1;
				doing = u < s ? "OPENING" : u < s + roll ? "PALLET OUT" : "CLOSING";
				state.shutter = u < s ? u / s : u < s + roll ? 1 : Math.max(0, 1 - (u - s - roll) / s);
				state.palletZ = u < s ? cellZ : lerp(cellZ, bedZ, (u - s) / roll);
			} else {
				state.palletZ = -1;
				doing = "TO STORAGE";
			}
		}
		// The modules standing on it (the drive goes on the top one).
		int m = 1;
		if (rs.getLevel() != null) {
			for (int k = 1; k < 8; k++) {
				BlockState above = rs.getLevel().getBlockState(rs.getBlockPos().above(4 * k));
				if (above.getBlock() instanceof PieceBlock piece && piece.piece == WarehousePiece.READY_STORAGE_EXT && piece.isOrigin(above)) {
					m++;
				} else {
					break;
				}
			}
		}
		state.modules = m;
		// The operator screen.
		TerminalPayload info = rs.terminalPos() == null ? null : WarehouseClient.snapshot(rs.terminalPos());
		Page page = new Page(font);
		page.title(WIDTH, "READY STORAGE", m + (m == 1 ? " MODULE" : " MODULES"));
		page.add(0, 16, 1, "Dispatch hatch", Page.GREY).right(WIDTH, 16, 1, doing, doing.equals("READY") ? Page.GREEN : Page.AMBER);
		if (rs.state() == ReadyStorageBlockEntity.WORKING && !rs.orderItem().isEmpty()) {
			String name = rs.orderItem().getHoverName().getString();
			if (name.length() > 14) {
				name = name.substring(0, 13) + ".";
			}
			page.add(0, 30, 1, "Order #" + rs.orderId(), Page.GREY).right(WIDTH, 30, 1, String.format(Locale.ROOT, "%s x%,d", name, rs.orderAmount()), Page.WHITE);
		} else {
			page.add(0, 30, 1, "Order", Page.GREY).right(WIDTH, 30, 1, "-", Page.DIM);
		}
		if (info != null && info.out().poolSlots() > 0) {
			TerminalPayload.Outbound out = info.out();
			page.add(0, 44, 1, "Slots used", Page.GREY).right(WIDTH, 44, 1, out.poolUsed() + " / " + out.poolSlots(), Page.CYAN);
			page.bar(0, 57, 1, 20, out.poolUsed() / (double) out.poolSlots(), Page.CYAN);
		} else {
			page.add(0, 44, 1, info == null ? "NO TERMINAL" : "CONNECTING", Page.AMBER);
		}
		state.page = page;
	}

	/** The gantry over its picks: each case lifted off, carried to the lift and handed in (a split one brought back). */
	private static void gantry(State state, ReadyStorageBlockEntity rs, float t, float bedY, float cellZ, float px, float pz, float py, float dx,
			float dz, float carry) {
		List<int[]> picks = rs.picks();
		for (int[] p : picks) {
			int len = p[1] == 1 ? ReadyStorageBlockEntity.SPLIT : ReadyStorageBlockEntity.CYCLE;
			int i = p[0];
			int layer = Math.min(1, i / 4);
			double[] spot = Kinematics.SPOTS[i % 4];
			float cx = (float) (16 + spot[0] + Kinematics.CASE / 2), cz = (float) (cellZ - 8 + spot[1] + Kinematics.CASE / 2);
			float top = (float) (bedY + Kinematics.PALLET_H + layer * (Kinematics.CASE_H + 0.05) + Kinematics.CASE_H);
			if (t >= len) {
				// Done: a whole case is gone from the pallet (a split one is back on it).
				if (p[1] == 0) {
					state.shown.set(i, false);
				}
				t -= len;
				continue;
			}
			boolean holding;
			if (t < 10) {
				state.gx = lerp(px, cx, t / 10);
				state.gz = lerp(pz, cz, t / 10);
				state.headY = lerp(py, top + 3, t / 10);
				holding = false;
			} else if (t < 16) {
				state.gx = cx;
				state.gz = cz;
				state.headY = lerp(top + 3, top, (t - 10) / 6);
				holding = false;
			} else if (t < 22) {
				state.gx = cx;
				state.gz = cz;
				state.headY = lerp(top, carry, (t - 16) / 6);
				holding = true;
			} else if (t < 32) {
				state.gx = lerp(cx, dx, (t - 22) / 10);
				state.gz = lerp(cz, dz, (t - 22) / 10);
				state.headY = carry;
				holding = true;
			} else if (p[1] == 0) {
				// Handed into the lift; back to where it waits.
				state.gx = lerp(dx, px, (t - 32) / 8);
				state.gz = lerp(dz, pz, (t - 32) / 8);
				state.headY = lerp(carry, py, (t - 32) / 8);
				holding = false;
			} else if (t < 52) {
				state.gx = dx;
				state.gz = dz;
				state.headY = carry;
				holding = true;
			} else if (t < 62) {
				state.gx = lerp(dx, cx, (t - 52) / 10);
				state.gz = lerp(dz, cz, (t - 52) / 10);
				state.headY = carry;
				holding = true;
			} else if (t < 68) {
				state.gx = cx;
				state.gz = cz;
				state.headY = lerp(carry, top, (t - 62) / 6);
				holding = true;
			} else {
				state.gx = lerp(cx, px, (t - 68) / 12);
				state.gz = lerp(cz, pz, (t - 68) / 12);
				state.headY = lerp(top, py, (t - 68) / 12);
				holding = false;
			}
			if (holding) {
				state.shown.set(i, false);
				state.carried = state.cases.get(i);
			}
			return;
		}
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		JsonObject l = WarehouseLayout.get().section("ready_storage");
		float top = l.get("hatch_top").getAsFloat(), step = l.get("slat_step").getAsFloat(), edge = l.get("edge").getAsFloat();
		float bedY = l.get("bed_y").getAsFloat(), palletX = l.get("pallet_x").getAsFloat();
		int light = state.lightCoords;
		pose.pushPose();
		Page.pieceFrame(pose, state.facing, WarehousePiece.READY_STORAGE);
		// The shutter: its edge where it is, the slats from there to the coil box.
		float yb = (top - 1.6F) * state.shutter;
		part(collector, pose, "rs_edge", 0, yb, 0, light);
		for (float y = yb + edge; y + step <= top + 0.01F; y += step) {
			part(collector, pose, "rs_slat", 0, y, 0, light);
		}
		// The gantry: the bridge at its z, the carriage on it, the tube down to the vacuum head.
		part(collector, pose, "rs_bridge", 0, 0, state.gz, light);
		part(collector, pose, "rs_carriage", state.gx, 0, state.gz, light);
		float tube0 = state.headY + 1.6F, tube1 = top - 3.2F;
		if (tube1 > tube0) {
			pose.pushPose();
			pose.translate(state.gx, tube0, state.gz);
			pose.scale(1, (tube1 - tube0) / 16F, 1);
			WhQuads.submitPart(collector, pose, "rs_tube", light, WhQuads.Tint.NONE);
			pose.popPose();
		}
		part(collector, pose, "rs_head", state.gx, state.headY, state.gz, light);
		if (state.carried >= 0) {
			pose.pushPose();
			pose.translate(state.gx - Kinematics.CASE / 2, state.headY - Kinematics.CASE_H, state.gz - Kinematics.CASE / 2);
			WhQuads.submitPart(collector, pose, KINDS[state.carried], light, WhQuads.Tint.NONE);
			pose.popPose();
		}
		// The pallet and its cases (on the bed, rolling, in the cell).
		if (state.palletZ >= 0) {
			pose.pushPose();
			pose.translate(palletX - 8, bedY, state.palletZ - 8);
			WhQuads.submit(collector, pose, WhQuads.loadQuads(0), light, WhQuads.Tint.NONE);
			for (int i = 0; i < state.cases.size(); i++) {
				if (!state.shown.get(i)) {
					continue;
				}
				double[] spot = Kinematics.SPOTS[i % 4];
				pose.pushPose();
				pose.translate(spot[0], Kinematics.PALLET_H + Math.min(1, i / 4) * (Kinematics.CASE_H + 0.05), spot[1]);
				WhQuads.submitPart(collector, pose, KINDS[Math.max(0, Math.min(KINDS.length - 1, state.cases.get(i)))], light, WhQuads.Tint.NONE);
				pose.popPose();
			}
			pose.popPose();
		}
		// The drive on the top module.
		part(collector, pose, "rs_drive", 0, l.get("drive_y").getAsFloat() + 64F * (state.modules - 1), 0, light);
		// The operator screen.
		if (state.page != null) {
			JsonObject s = l.getAsJsonObject("screen");
			float x0 = s.get("x0").getAsFloat(), x1 = s.get("x1").getAsFloat(), y1 = s.get("y1").getAsFloat();
			state.page.draw(pose, collector, x0 + 0.3F, y1 - 0.3F, s.get("z").getAsFloat(), (x1 - x0 - 0.6F) / WIDTH);
		}
		pose.popPose();
	}

	private static void part(SubmitNodeCollector collector, PoseStack pose, String name, float x, float y, float z, int light) {
		pose.pushPose();
		pose.translate(x, y, z);
		WhQuads.submitPart(collector, pose, name, light, WhQuads.Tint.NONE);
		pose.popPose();
	}
}
