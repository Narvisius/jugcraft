package io.github.jimbozoomer.jugcraft.client.warehouse;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jimbozoomer.jugcraft.warehouse.LiftBlockEntity;
import io.github.jimbozoomer.jugcraft.warehouse.PieceBlock;
import io.github.jimbozoomer.jugcraft.warehouse.WarehouseLayout;
import io.github.jimbozoomer.jugcraft.warehouse.WarehousePiece;
import java.util.List;
import net.minecraft.client.gui.Font;
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
 * A Pallet Lift landing, live (part W3; LiftBlockEntity tells the story): its telescoping gate at how far open it is,
 * the pallet on its transfer conveyor (rolling on or off the carriage), the call station's screen (the carriage's floor
 * and what it is doing, the pallet, the gate) and its beacon (flashing while the lift moves), the name plate over the
 * opening (the lift and the floor), the position indicator's arrows. The lowest landing draws the carriage (and its
 * pallet), the counterweight going the other way and the chains up to the drive's sprockets, the whole height of the lift.
 */
public class LiftRenderer implements BlockEntityRenderer<LiftBlockEntity, LiftRenderer.State> {
	private static final int WIDTH = 130;
	private final Font font;

	public static class State extends BlockEntityRenderState {
		Direction facing = Direction.NORTH;
		float gate;
		int bedLook = -1;
		boolean beacon, up, down;
		@Nullable Page screen, plate;
		// The controller's: the carriage's height over this landing (px), its pallet, the pallet rolling (its z, or -1),
		// the lift's top (the drive's base, px over this landing).
		boolean controller;
		float car, rollZ = -1;
		int carLook = -1;
		float top;
	}

	public LiftRenderer(BlockEntityRendererProvider.Context context) {
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
		return 128;
	}

	@Override
	public void extractRenderState(LiftBlockEntity lift, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(lift, state, crumbling);
		state.facing = lift.getBlockState().getBlock() instanceof PieceBlock ? lift.getBlockState().getValue(PieceBlock.FACING) : Direction.NORTH;
		long now = lift.getLevel() == null ? 0 : lift.getLevel().getGameTime();
		LiftBlockEntity c = lift.controller();
		state.gate = lift.gate(now, partialTick);
		JsonObject l = WarehouseLayout.get().section("lift");
		int floor = lift.floorNumber();
		List<Integer> floors = c == null ? List.of() : c.floors();
		int at = c == null ? 0 : Math.min(c.at(), Math.max(0, floors.size() - 1));
		int phase = c == null ? LiftBlockEntity.IDLE : c.phase();
		boolean rollingHere = c != null && (phase == LiftBlockEntity.LOADING || phase == LiftBlockEntity.UNLOADING)
				&& c.landingIndex(lift.getBlockPos().getY()) == c.at();
		state.bedLook = rollingHere ? -1 : lift.bedLook();
		boolean moving = phase == LiftBlockEntity.MOVING;
		state.beacon = moving && (now / 8) % 2 == 0;
		state.up = moving && c.direction() > 0;
		state.down = moving && c.direction() < 0;
		// The call station's screen.
		Page s = new Page(font);
		s.title(WIDTH, "PALLET LIFT", "FLOOR " + floor);
		String where;
		int colour = Page.GREEN;
		if (c == null) {
			where = "NO LIFT";
			colour = Page.GREY;
		} else if (moving) {
			int to = at + 1;
			where = (c.direction() > 0 ? "UP TO " : "DOWN TO ") + "FLOOR " + to;
			colour = Page.AMBER;
		} else {
			where = "AT FLOOR " + (at + 1);
		}
		s.add(0, 14, 1, "CARRIAGE", Page.GREY).right(WIDTH, 14, 1, where, colour);
		s.add(0, 26, 1, "PALLET", Page.GREY).right(WIDTH, 26, 1, c == null || c.carLook() < 0 ? "EMPTY" : "ON BOARD", Page.WHITE);
		String gate = state.gate > 0.99F ? "OPEN" : state.gate > 0.01F ? "MOVING" : "SHUT - LOCKED";
		s.add(0, 38, 1, "GATE", Page.GREY).right(WIDTH, 38, 1, gate, state.gate > 0.01F ? Page.AMBER : Page.WHITE);
		s.add(0, 50, 1, "BED", Page.GREY).right(WIDTH, 50, 1, lift.bedLook() >= 0 ? "PALLET WAITING" : "CLEAR", Page.CYAN);
		state.screen = s;
		// The name plate: the floor big in its black field, the lift's name.
		Page p = new Page(font);
		p.add(0, 0, 1, String.valueOf(floor), 0xFFECB814);
		state.plate = p;
		// The controller: the carriage, the counterweight, the chains, a pallet rolling.
		state.controller = c == lift && !floors.isEmpty();
		if (state.controller) {
			double carY = c.carY(now) + (phase == LiftBlockEntity.MOVING ? partialTick * LiftBlockEntity.SPEED * c.direction() : 0);
			carY = Math.max(floors.get(0), Math.min(floors.get(floors.size() - 1), carY));
			state.car = (float) ((carY - lift.getBlockPos().getY()) * 16);
			state.carLook = c.carLook();
			state.top = (floors.get(floors.size() - 1) + 3 - lift.getBlockPos().getY()) * 16F;
			state.rollZ = -1;
			if (phase == LiftBlockEntity.LOADING || phase == LiftBlockEntity.UNLOADING) {
				float f = Math.min(1, (now - c.phaseStart() + partialTick) / LiftBlockEntity.ROLL);
				float bedZ = l.get("bed_z").getAsFloat(), carZ = l.get("carriage_z").getAsFloat();
				float from = phase == LiftBlockEntity.LOADING ? bedZ : carZ, to = phase == LiftBlockEntity.LOADING ? carZ : bedZ;
				state.rollZ = from + (to - from) * f;
				int look = phase == LiftBlockEntity.LOADING ? landingBedLook(c, at) : c.carLook();
				state.carLook = look;
			}
		}
	}

	private static int landingBedLook(LiftBlockEntity c, int at) {
		if (c.getLevel() == null || at >= c.floors().size()) {
			return -1;
		}
		var pos = new net.minecraft.core.BlockPos(c.getBlockPos().getX(), c.floors().get(at), c.getBlockPos().getZ());
		return c.getLevel().getBlockEntity(pos) instanceof LiftBlockEntity l ? l.bedLook() : -1;
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		JsonObject l = WarehouseLayout.get().section("lift");
		float top = l.get("top").getAsFloat(), lo0 = l.get("gate_lo").getAsFloat(), hi0 = l.get("gate_hi").getAsFloat();
		float palletX = l.get("pallet_x").getAsFloat(), bedZ = l.get("bed_z").getAsFloat(), bedY = l.get("bed_y").getAsFloat();
		float deckY = l.get("deck_y").getAsFloat(), carZ = l.get("carriage_z").getAsFloat();
		int light = state.lightCoords;
		pose.pushPose();
		Page.pieceFrame(pose, state.facing, WarehousePiece.LIFT_LANDING);
		// The gate: the lower panel rises twice as fast as the upper; open, they stand together in the housing.
		part(collector, pose, "lift_gate_lo", 0, lo0 + state.gate * (top - 1.2F), 0, light);
		part(collector, pose, "lift_gate_hi", 0, hi0 + state.gate * (top / 2 - 0.4F), 0, light);
		// The pallet waiting on the transfer conveyor.
		if (state.bedLook >= 0) {
			load(collector, pose, state.bedLook, palletX, bedY, bedZ, light);
		}
		// The call station's beacon and the indicator's arrows, lit.
		JsonArray b = l.getAsJsonArray("beacon");
		if (state.beacon) {
			part(collector, pose, "lift_beacon", b.get(0).getAsFloat() - 8, b.get(1).getAsFloat(), b.get(2).getAsFloat() - 8, light);
		}
		if (state.up) {
			JsonArray a = l.getAsJsonArray("arrow_up");
			part(collector, pose, "lift_arrow_up", a.get(0).getAsFloat(), a.get(1).getAsFloat(), a.get(2).getAsFloat(), light);
		}
		if (state.down) {
			JsonArray a = l.getAsJsonArray("arrow_dn");
			part(collector, pose, "lift_arrow_dn", a.get(0).getAsFloat(), a.get(1).getAsFloat(), a.get(2).getAsFloat(), light);
		}
		// The screens: the call station's page, the name plate's floor.
		JsonObject sc = l.getAsJsonObject("screen");
		if (state.screen != null) {
			float x0 = sc.get("x0").getAsFloat(), x1 = sc.get("x1").getAsFloat(), y1 = sc.get("y1").getAsFloat();
			state.screen.draw(pose, collector, x0 + 0.25F, y1 - 0.25F, sc.get("z").getAsFloat(), (x1 - x0 - 0.5F) / WIDTH);
		}
		JsonObject pl = l.getAsJsonObject("plate");
		if (state.plate != null) {
			float x0 = pl.get("x0").getAsFloat(), y1 = pl.get("y1").getAsFloat(), field = pl.get("field").getAsFloat();
			state.plate.draw(pose, collector, x0 + field * 0.25F, y1 - 1.2F, pl.get("z").getAsFloat(), field * 0.55F / 6F);
		}
		if (state.controller) {
			// The carriage at its height, its pallet on its rollers (or rolling on or off).
			part(collector, pose, "lift_carriage", 0, state.car, 0, light);
			if (state.rollZ >= 0 && state.carLook >= 0) {
				float y = state.car + deckY + (bedY - deckY) * Math.max(0, Math.min(1, (state.rollZ - carZ) / (bedZ - carZ)));
				load(collector, pose, state.carLook, palletX, y, state.rollZ, light);
			} else if (state.carLook >= 0) {
				load(collector, pose, state.carLook, palletX, state.car + deckY, carZ, light);
			}
			// The counterweight the other way (its travel the carriage's), the chains up to the drive's sprockets.
			float travel = state.top - 48F;
			float cw = travel - state.car + 16F;
			part(collector, pose, "lift_cw", 0, cw, 0, light);
			JsonArray chains = l.getAsJsonArray("chains");
			float sprocket = state.top + l.get("sprocket_y").getAsFloat();
			float yoke = state.car + l.get("yoke_y").getAsFloat();
			for (int i = 0; i < chains.size(); i++) {
				JsonArray c = chains.get(i).getAsJsonArray();
				chain(collector, pose, c.get(0).getAsFloat(), yoke, sprocket, c.get(1).getAsFloat(), light);
			}
			JsonArray cc = l.getAsJsonArray("cw_chain");
			chain(collector, pose, cc.get(0).getAsFloat(), cw + l.get("cw_eye").getAsFloat(), sprocket, cc.get(1).getAsFloat(), light);
		}
		pose.popPose();
	}

	private static void chain(SubmitNodeCollector collector, PoseStack pose, float x, float y0, float y1, float z, int light) {
		if (y1 <= y0 + 0.5F) {
			return;
		}
		pose.pushPose();
		pose.translate(x, y0, z);
		pose.scale(1, (y1 - y0) / 16F, 1);
		WhQuads.submitPart(collector, pose, "lift_chain", light, WhQuads.Tint.NONE);
		pose.popPose();
	}

	private static void load(SubmitNodeCollector collector, PoseStack pose, int look, float x, float y, float z, int light) {
		pose.pushPose();
		pose.translate(x - 8, y, z - 8);
		WhQuads.submit(collector, pose, WhQuads.loadQuads(look), light, WhQuads.Tint.NONE);
		pose.popPose();
	}

	private static void part(SubmitNodeCollector collector, PoseStack pose, String name, float x, float y, float z, int light) {
		pose.pushPose();
		pose.translate(x, y, z);
		WhQuads.submitPart(collector, pose, name, light, WhQuads.Tint.NONE);
		pose.popPose();
	}
}
