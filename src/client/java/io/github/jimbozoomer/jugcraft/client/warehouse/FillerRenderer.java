package io.github.jimbozoomer.jugcraft.client.warehouse;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jimbozoomer.jugcraft.warehouse.CaseLoad;
import io.github.jimbozoomer.jugcraft.warehouse.Drums;
import io.github.jimbozoomer.jugcraft.warehouse.FillerBlockEntity;
import io.github.jimbozoomer.jugcraft.warehouse.FillerMotion;
import io.github.jimbozoomer.jugcraft.warehouse.Kinematics;
import io.github.jimbozoomer.jugcraft.warehouse.PalletLoad;
import io.github.jimbozoomer.jugcraft.warehouse.PieceBlock;
import io.github.jimbozoomer.jugcraft.warehouse.WarehouseLayout;
import io.github.jimbozoomer.jugcraft.warehouse.WarehousePiece;
import java.util.ArrayList;
import java.util.List;
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
 * The Drum Filling Station's live parts (part W4): the empty drums in its magazine and the pallets in theirs; the
 * pallet pushed out onto the scale and back; the gantry robot (bridge, trolley, Z mast, its vacuum tool, the lance)
 * at its pose on the plan's timeline (FillerMotion), the drum it carries; the drums on the pallet, strapped once it is
 * done; the buffer tank's level gauge, the scale's indicator, the HMI's page, the stack light and the earthing reel's
 * lamp (lit while a flammable liquid is filled).
 */
public class FillerRenderer implements BlockEntityRenderer<FillerBlockEntity, FillerRenderer.State> {
	private static final float WIDTH = 110;
	private final Font font;

	/** A drum to draw: its kind and where (piece pixels: its middle, its base). */
	record Drum(int kind, float x, float y, float z) {
	}

	public static class State extends BlockEntityRenderState {
		Direction facing = Direction.NORTH;
		BlockPos pos = BlockPos.ZERO;
		final List<Drum> drums = new ArrayList<>();
		int pallets, look = -1;
		/** The pallet on the bay: where it is between the magazine (0) and the bay (1); -1 none. */
		float bay = -1;
		FillerMotion.Pose head = null;
		FluidVariant liquid = FluidVariant.blank();
		float level;
		boolean earth;
		int lamp;
		@Nullable Page page, indicator;
	}

	public FillerRenderer(BlockEntityRendererProvider.Context context) {
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

	private static float spot(int i, int axis) {
		return FillerMotion.spot(i)[axis];
	}

	@Override
	public void extractRenderState(FillerBlockEntity f, State state, float partialTick, Vec3 camera, ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(f, state, crumbling);
		state.facing = f.getBlockState().getBlock() instanceof PieceBlock ? f.getBlockState().getValue(PieceBlock.FACING) : Direction.NORTH;
		state.pos = f.getBlockPos();
		double time = f.getLevel() == null ? 0 : f.getLevel().getGameTime() + partialTick;
		state.drums.clear();
		float magY = FillerMotion.magY(), top = FillerMotion.palletTop();
		int[] mag = f.magazine();
		for (int i = 0; i < mag.length && i < FillerMotion.places(); i++) {
			if (mag[i] >= 0) {
				float[] p = FillerMotion.place(i);
				state.drums.add(new Drum(mag[i], p[0], magY, p[1]));
			}
		}
		state.pallets = Math.min(f.pallets(), 4);
		state.look = -1;
		state.bay = -1;
		FillerMotion.Plan plan = f.plan();
		state.head = plan == null ? FillerMotion.parked() : FillerMotion.at(plan, time - plan.start());
		if (plan != null) {
			state.drums.add(new Drum(plan.kind(), state.head.drumX(), state.head.drumY(), state.head.drumZ()));
		}
		PalletLoad build = f.build();
		switch (f.state()) {
			case FillerBlockEntity.DISPENSING -> state.bay = (float) Math.min(1, (time - f.since()) / FillerBlockEntity.SLIDE);
			case FillerBlockEntity.STOWING -> state.bay = (float) Math.max(0, 1 - (time - f.since()) / FillerBlockEntity.SLIDE);
			case FillerBlockEntity.BUILDING, FillerBlockEntity.STRAPPING -> {
				state.bay = 1;
				if (build != null) {
					for (int i = 0; i < build.size(); i++) {
						CaseLoad c = build.cases().get(i);
						Drums.Kind k = Drums.kindOf(c.item());
						state.drums.add(new Drum(k == null ? 0 : k.ordinal(), spot(i, 0), top, spot(i, 1)));
					}
				}
			}
			case FillerBlockEntity.READY -> state.look = build == null ? 0 : build.look();
			case FillerBlockEntity.RETURNING -> {
				state.bay = 1;
				List<Integer> back = f.returned();
				for (int i = 0; i < back.size(); i++) {
					state.drums.add(new Drum(back.get(i), spot(i, 0), top, spot(i, 1)));
				}
			}
			default -> {
			}
		}
		// The buffer tank: what is in it, and the drum being filled's share till it has run in.
		long tank = f.tank();
		state.liquid = f.liquid();
		boolean filling = plan != null && plan.type() == FillerMotion.FILL;
		if (filling) {
			tank += (long) (plan.amount() * (1 - state.head.filled()));
			if (state.liquid.isBlank()) {
				state.liquid = f.buildLiquid();
			}
		}
		state.level = (float) Math.min(1, tank / (double) FillerBlockEntity.TANK);
		Drums.Kind kind = state.liquid.isBlank() ? null : Drums.kindFor(state.liquid);
		state.earth = filling && FillerMotion.filling(plan, time - plan.start()) && plan.kind() == Drums.Kind.RED.ordinal();
		boolean waiting = !f.liquid().isBlank() && plan == null && f.state() == FillerBlockEntity.EMPTY;
		state.lamp = plan != null || f.state() == FillerBlockEntity.DISPENSING || f.state() == FillerBlockEntity.STRAPPING
				|| f.state() == FillerBlockEntity.STOWING ? 1 : f.state() == FillerBlockEntity.READY || waiting ? 2 : 0;
		// The HMI's page.
		Page page = new Page(font);
		page.title(WIDTH, "DRUM FILLING", "DF-1");
		String name = state.liquid.isBlank() ? "-" : Drums.name(state.liquid).getString().toUpperCase(Locale.ROOT);
		page.add(0, 16, 1, "Liquid", Page.GREY).right(WIDTH, 16, 1, name.length() > 14 ? name.substring(0, 13) + "." : name, Page.WHITE);
		String drum = filling ? (plan.spot() + 1) + " OF 4  " + kindName(plan.kind()) : f.state() == FillerBlockEntity.READY ? "STRAPPED" : "-";
		page.add(0, 28, 1, "Drum", Page.GREY).right(WIDTH, 28, 1, drum, filling ? Page.AMBER : Page.WHITE);
		String filled = filling ? Drums.buckets((long) (plan.amount() * state.head.filled())) + " / " + Drums.buckets(Drums.CAPACITY) : "-";
		page.add(0, 40, 1, "Filled", Page.GREY).right(WIDTH, 40, 1, filled, Page.CYAN);
		page.add(0, 52, 1, "Earth", Page.GREY).right(WIDTH, 52, 1, kind == Drums.Kind.RED ? (state.earth ? "CLAMPED" : "READY") : "-",
				state.earth ? Page.GREEN : Page.WHITE);
		int empties = 0;
		for (int k : mag) {
			if (k >= 0) {
				empties++;
			}
		}
		boolean none = kind != null && f.drums(kind) == 0;
		page.add(0, 64, 1, "Magazine", Page.GREY).right(WIDTH, 64, 1, none ? "NO " + kindName(kind.ordinal()) + " DRUMS" : empties + " DRUMS  " + f.pallets()
				+ " PAL", none ? Page.AMBER : Page.WHITE);
		page.add(0, 76, 1, "Tank", Page.GREY).right(WIDTH, 76, 1, Drums.buckets(tank) + " / " + Drums.buckets(FillerBlockEntity.TANK), Page.CYAN);
		state.page = page;
		// The scale's indicator: the drum's net weight as it fills (in buckets).
		Page ind = new Page(font);
		String digits = filling && state.head.placed() ? String.format(Locale.ROOT, "%5.1f B", plan.amount() * state.head.filled() / 1000.0) : "  0.0 B";
		ind.add(4, 3, 1.6F, digits, 0xFF78FA96);
		ind.add(4, 20, 0.8F, filling && FillerMotion.filling(plan, time - plan.start()) ? "NET  FILLING" : "NET  STABLE", 0xFFAAB4B0);
		state.indicator = ind;
	}

	private static String kindName(int kind) {
		return switch (Drums.Kind.values()[kind]) {
			case STEEL -> "STEEL";
			case RED -> "RED";
			case POLY -> "HDPE";
			case DEWAR -> "DEWAR";
		};
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		JsonObject g = WarehouseLayout.get().section("drum_filler");
		int light = state.lightCoords;
		pose.pushPose();
		Page.pieceFrame(pose, state.facing, WarehousePiece.DRUM_FILLER);
		// The pallets in their magazine, the one on (or going to or from) the bay.
		JsonObject pal = g.getAsJsonObject("pallets");
		float px = pal.get("x").getAsFloat(), pz = pal.get("z").getAsFloat(), py = pal.get("y0").getAsFloat(), step = pal.get("step").getAsFloat();
		for (int k = 0; k < state.pallets; k++) {
			pose.pushPose();
			pose.translate(px, py + k * step, pz);
			WhQuads.submit(collector, pose, WhQuads.loadQuads(0), light, WhQuads.Tint.NONE);
			pose.popPose();
		}
		JsonArray bay = g.getAsJsonArray("bay");
		float bx = bay.get(0).getAsFloat(), bz = bay.get(1).getAsFloat(), deck = g.get("fill_deck").getAsFloat();
		if (state.look >= 0) {
			pose.pushPose();
			pose.translate(bx, deck, bz);
			WhQuads.submit(collector, pose, WhQuads.loadQuads(state.look), light, WhQuads.Tint.NONE);
			pose.popPose();
		} else if (state.bay >= 0) {
			pose.pushPose();
			pose.translate(bx, py + (deck - py) * state.bay, pz + (bz - pz) * state.bay);
			WhQuads.submit(collector, pose, WhQuads.loadQuads(0), light, WhQuads.Tint.NONE);
			pose.popPose();
		}
		for (Drum d : state.drums) {
			pose.pushPose();
			pose.translate(d.x(), d.y(), d.z());
			WhQuads.submitPart(collector, pose, Drums.Kind.values()[d.kind()].part(), light, WhQuads.Tint.NONE);
			pose.popPose();
		}
		// The gantry robot.
		FillerMotion.Pose h = state.head;
		float headY = g.get("head_y").getAsFloat(), lance = g.get("lance").getAsFloat();
		pose.pushPose();
		pose.translate(h.x(), 0, 0);
		WhQuads.submitPart(collector, pose, "lq_bridge", light, WhQuads.Tint.NONE);
		pose.popPose();
		pose.pushPose();
		pose.translate(h.x(), headY, h.z());
		WhQuads.submitPart(collector, pose, "lq_trolley", light, WhQuads.Tint.NONE);
		pose.translate(0, -h.drop(), 0);
		WhQuads.submitPart(collector, pose, "lq_mast", light, WhQuads.Tint.NONE);
		pose.translate(0, lance * (1 - h.lance()), 0);
		WhQuads.submitPart(collector, pose, "lq_lance", light, WhQuads.Tint.NONE);
		pose.popPose();
		// The buffer tank's level gauge.
		JsonObject s = g.getAsJsonObject("sight");
		float sx = s.get("x").getAsFloat(), y0 = s.get("y0").getAsFloat(), y1 = s.get("y1").getAsFloat(), sz = s.get("z").getAsFloat();
		if (state.level > 0.001F) {
			WhLiquid.face(collector, pose, sx + 0.05F, y0, sx + 1.15F, y0 + (y1 - y0) * state.level, sz + 0.02F, state.liquid, light, state.pos);
		}
		// The stack light (green working, amber waiting), the earthing reel's lamp.
		if (state.lamp > 0) {
			JsonArray st = g.getAsJsonArray("stack");
			int k = state.lamp == 1 ? 0 : 1;
			pose.pushPose();
			pose.translate(st.get(0).getAsFloat() - 8, st.get(2).getAsFloat() + k * st.get(3).getAsFloat() - 0.03F, st.get(1).getAsFloat() - 8);
			pose.scale(1.0F, 0.99F, 1.0F);
			WhQuads.submitPart(collector, pose, k == 0 ? "lamp_green" : "lamp_amber", light, WhQuads.Tint.NONE);
			pose.popPose();
		}
		if (state.earth) {
			JsonArray r = g.getAsJsonArray("reel_lamp");
			pose.pushPose();
			pose.translate(r.get(0).getAsFloat(), r.get(1).getAsFloat(), r.get(2).getAsFloat());
			WhQuads.submitPart(collector, pose, "lq_reel_lamp", light, WhQuads.Tint.NONE);
			pose.popPose();
		}
		if (state.page != null) {
			JsonObject m = g.getAsJsonObject("hmi");
			float x0 = m.get("x0").getAsFloat(), x1 = m.get("x1").getAsFloat(), my1 = m.get("y1").getAsFloat();
			state.page.draw(pose, collector, x0 + 0.3F, my1 - 0.3F, m.get("z").getAsFloat(), (x1 - x0 - 0.6F) / WIDTH);
		}
		if (state.indicator != null) {
			JsonObject m = g.getAsJsonObject("indicator");
			float x0 = m.get("x0").getAsFloat(), x1 = m.get("x1").getAsFloat(), my1 = m.get("y1").getAsFloat();
			state.indicator.draw(pose, collector, x0 + 0.4F, my1 - 0.5F, m.get("z").getAsFloat(), (x1 - x0 - 0.8F) / 64F);
		}
		pose.popPose();
	}
}
