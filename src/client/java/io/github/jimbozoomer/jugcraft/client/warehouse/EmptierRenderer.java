package io.github.jimbozoomer.jugcraft.client.warehouse;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.warehouse.Drums;
import io.github.jimbozoomer.jugcraft.warehouse.EmptierBlockEntity;
import io.github.jimbozoomer.jugcraft.warehouse.EmptierMotion;
import io.github.jimbozoomer.jugcraft.warehouse.PalletLoad;
import io.github.jimbozoomer.jugcraft.warehouse.PieceBlock;
import io.github.jimbozoomer.jugcraft.warehouse.WarehouseLayout;
import io.github.jimbozoomer.jugcraft.warehouse.WarehousePiece;
import java.util.Locale;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The Drum Emptying Station's live parts (part W4): the pallet of drums on its platform; the jib swinging the drum pump
 * from drum to drum on the run's timeline (EmptierMotion), its balancer running out along the arm, the cable, the pump
 * dipping in and lifting out; the holding tank's level gauge rising as it pumps; the HMI's page and the stack light.
 */
public class EmptierRenderer implements BlockEntityRenderer<EmptierBlockEntity, EmptierRenderer.State> {
	private static final float WIDTH = 110;
	private final Font font;

	public static class State extends BlockEntityRenderState {
		Direction facing = Direction.NORTH;
		BlockPos pos = BlockPos.ZERO;
		int look = -1;
		EmptierMotion.Pose jib;
		FluidVariant liquid = FluidVariant.blank();
		float level;
		int lamp;
		@Nullable Page page;
	}

	public EmptierRenderer(BlockEntityRendererProvider.Context context) {
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
	public void extractRenderState(EmptierBlockEntity e, State state, float partialTick, Vec3 camera, ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(e, state, crumbling);
		state.facing = e.getBlockState().getBlock() instanceof PieceBlock ? e.getBlockState().getValue(PieceBlock.FACING) : Direction.NORTH;
		state.pos = e.getBlockPos();
		double time = e.getLevel() == null ? 0 : e.getLevel().getGameTime() + partialTick;
		PalletLoad pallet = e.pallet();
		state.look = pallet == null ? -1 : pallet.look();
		EmptierMotion.Plan plan = e.state() == EmptierBlockEntity.WORKING ? e.plan() : null;
		Drums.Kind kind = pallet == null || pallet.isEmpty() ? null : Drums.kindOf(pallet.cases().get(0).item());
		state.jib = plan == null ? EmptierMotion.parked(kind == null ? 0 : kind.ordinal()) : EmptierMotion.at(plan, time - plan.start());
		long tank = e.level() + (plan == null ? 0 : state.jib.pumped());
		state.liquid = e.liquid();
		if (state.liquid.isBlank() && pallet != null && !pallet.isEmpty()) {
			FluidVariant v = Drums.liquid(pallet.cases().get(0).item());
			state.liquid = v == null || plan == null ? FluidVariant.blank() : v;
		}
		state.level = (float) Math.min(1, tank / (double) EmptierBlockEntity.TANK);
		state.lamp = plan != null ? 1 : e.state() == EmptierBlockEntity.BED ? 2 : 0;
		Page page = new Page(font);
		page.title(WIDTH, "DRUM EMPTYING", "DE-1");
		FluidVariant liquid = pallet == null || pallet.isEmpty() ? null : Drums.liquid(pallet.cases().get(0).item());
		String what = liquid == null ? "-" : Drums.name(liquid).getString().toUpperCase(Locale.ROOT);
		page.add(0, 16, 1, "Order", Page.GREY).right(WIDTH, 16, 1, (e.orderId() > 0 && pallet != null ? "#" + e.orderId() + "  " : "")
				+ (what.length() > 11 ? what.substring(0, 10) + "." : what), Page.WHITE);
		int drums = pallet == null ? 0 : pallet.size(), drum = 0;
		if (plan != null) {
			int k = 0;
			for (EmptierMotion.Step s : plan.steps()) {
				k++;
				if (state.jib.pumped() < sumTo(plan, k)) {
					drum = s.drum() + 1;
					break;
				}
			}
		}
		page.add(0, 28, 1, "Drum", Page.GREY).right(WIDTH, 28, 1, drum > 0 ? drum + " OF " + drums : e.state() == EmptierBlockEntity.BED ? "DONE" : "-",
				drum > 0 ? Page.AMBER : Page.WHITE);
		page.add(0, 40, 1, "Pumped", Page.GREY).right(WIDTH, 40, 1, plan == null ? "-" : Drums.buckets(state.jib.pumped()) + " / " + Drums.buckets(plan.total()),
				Page.CYAN);
		page.add(0, 52, 1, "Flow", Page.GREY).right(WIDTH, 52, 1, state.jib.pumping() ? Drums.buckets(EmptierMotion.RATE * 20L) + "/s" : "-", Page.CYAN);
		page.add(0, 64, 1, "Tank", Page.GREY).right(WIDTH, 64, 1, Drums.buckets(tank) + " / " + Drums.buckets(EmptierBlockEntity.TANK), Page.CYAN);
		page.add(0, 76, 1, "Pallet", Page.GREY).right(WIDTH, 76, 1, e.state() == EmptierBlockEntity.BED ? (e.emptied() ? "EMPTIES TO DF" : "BACK TO RACK") : "-",
				Page.GREEN);
		state.page = page;
	}

	private static long sumTo(EmptierMotion.Plan plan, int k) {
		long n = 0;
		for (int i = 0; i < k && i < plan.steps().size(); i++) {
			n += plan.steps().get(i).amount();
		}
		return n;
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		JsonObject g = WarehouseLayout.get().section("drum_emptier");
		int light = state.lightCoords;
		pose.pushPose();
		Page.pieceFrame(pose, state.facing, WarehousePiece.DRUM_EMPTIER);
		JsonArray bay = g.getAsJsonArray("bay");
		if (state.look >= 0) {
			pose.pushPose();
			pose.translate(bay.get(0).getAsFloat(), g.get("deck").getAsFloat(), bay.get(1).getAsFloat());
			WhQuads.submit(collector, pose, WhQuads.loadQuads(state.look), light, WhQuads.Tint.NONE);
			pose.popPose();
		}
		// The jib: its arm turned about the pillar, the balancer out along it, the cable down to the pump.
		JsonArray jib = g.getAsJsonArray("jib");
		float jx = jib.get(0).getAsFloat(), jz = jib.get(1).getAsFloat(), jy = jib.get(2).getAsFloat();
		EmptierMotion.Pose p = state.jib;
		pose.pushPose();
		pose.translate(jx, jy, jz);
		pose.rotateDegrees(Axis.YP, p.angle());
		WhQuads.submitPart(collector, pose, "lq_jib", light, WhQuads.Tint.NONE);
		pose.translate(0, 0, p.radius());
		WhQuads.submitPart(collector, pose, "lq_balancer", light, WhQuads.Tint.NONE);
		pose.popPose();
		double a = Math.toRadians(p.angle());
		float px = (float) (jx + p.radius() * Math.sin(a)), pz = (float) (jz + p.radius() * Math.cos(a));
		float lift = g.get("pump_lift").getAsFloat();
		float py = p.pumpY() + lift * (1 - p.dip());
		pose.pushPose();
		pose.translate(px, py, pz);
		WhQuads.submitPart(collector, pose, "lq_pump", light, WhQuads.Tint.NONE);
		pose.popPose();
		float top = py + g.get("pump_top").getAsFloat(), bottom = jy - g.get("balancer_drop").getAsFloat();
		if (bottom > top) {
			pose.pushPose();
			pose.translate(px, top, pz);
			pose.scale(1, (bottom - top) / 16F, 1);
			WhQuads.submitPart(collector, pose, "lq_cable", light, WhQuads.Tint.NONE);
			pose.popPose();
		}
		// The holding tank's level gauge.
		JsonObject s = g.getAsJsonObject("sight");
		float sx = s.get("x").getAsFloat(), y0 = s.get("y0").getAsFloat(), y1 = s.get("y1").getAsFloat(), sz = s.get("z").getAsFloat();
		if (state.level > 0.001F) {
			WhLiquid.face(collector, pose, sx + 0.05F, y0, sx + 1.15F, y0 + (y1 - y0) * state.level, sz + 0.02F, state.liquid, light, state.pos);
		}
		if (state.lamp > 0) {
			JsonArray st = g.getAsJsonArray("stack");
			int k = state.lamp == 1 ? 0 : 1;
			pose.pushPose();
			pose.translate(st.get(0).getAsFloat() - 8, st.get(2).getAsFloat() + k * st.get(3).getAsFloat() - 0.03F, st.get(1).getAsFloat() - 8);
			WhQuads.submitPart(collector, pose, k == 0 ? "lamp_green" : "lamp_amber", light, WhQuads.Tint.NONE);
			pose.popPose();
		}
		if (state.page != null) {
			JsonObject m = g.getAsJsonObject("hmi");
			float x0 = m.get("x0").getAsFloat(), x1 = m.get("x1").getAsFloat(), my1 = m.get("y1").getAsFloat();
			state.page.draw(pose, collector, x0 + 0.3F, my1 - 0.3F, m.get("z").getAsFloat(), (x1 - x0 - 0.6F) / WIDTH);
		}
		pose.popPose();
	}
}
