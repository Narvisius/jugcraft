package io.github.jimbozoomer.jugcraft.client.warehouse;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jimbozoomer.jugcraft.warehouse.Drums;
import io.github.jimbozoomer.jugcraft.warehouse.EmptierBlockEntity;
import io.github.jimbozoomer.jugcraft.warehouse.LiquidsOutBlockEntity;
import io.github.jimbozoomer.jugcraft.warehouse.Order;
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
 * The Liquids Out panel's live parts (part W4): the OUT flange's flow lamp and the liquid seen running through its
 * sight flow glass while the flange sends, and its screen (LIQUIDS OUT: the holding tank, what is ready at the fill
 * point, what is going to pipes, the flange's flow).
 */
public class LiquidsOutRenderer implements BlockEntityRenderer<LiquidsOutBlockEntity, LiquidsOutRenderer.State> {
	private static final float WIDTH = 110;
	private final Font font;

	public static class State extends BlockEntityRenderState {
		Direction facing = Direction.NORTH;
		BlockPos pos = BlockPos.ZERO;
		boolean flowing;
		FluidVariant liquid = FluidVariant.blank();
		@Nullable Page page;
	}

	public LiquidsOutRenderer(BlockEntityRendererProvider.Context context) {
		this.font = context.font();
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(LiquidsOutBlockEntity panel, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(panel, state, crumbling);
		state.facing = panel.getBlockState().getBlock() instanceof PieceBlock ? panel.getBlockState().getValue(PieceBlock.FACING) : Direction.NORTH;
		state.pos = panel.getBlockPos();
		state.flowing = panel.flowing();
		state.liquid = panel.flowLiquid();
		Page page = new Page(font);
		page.title(WIDTH, "LIQUIDS OUT", "DRUM EMPTYING");
		EmptierBlockEntity e = panel.emptier();
		if (!panel.linked() || e == null) {
			page.add(0, 18, 1, "NO EMPTYING STATION", Page.AMBER);
		} else {
			long window = 0, pipes = 0;
			for (EmptierBlockEntity.Share s : e.shares()) {
				if (s.dest == Order.WINDOW) {
					window += Drums.mb(s.amount);
				} else {
					pipes += Drums.mb(s.amount);
				}
			}
			String liquid = e.liquid().isBlank() ? "EMPTY" : Drums.name(e.liquid()).getString().toUpperCase(Locale.ROOT);
			page.add(0, 16, 0.85F, "Tank", Page.GREY).right(WIDTH, 16, 0.85F, Drums.buckets(e.level()) + "  " + (liquid.length() > 10 ? liquid.substring(0, 9)
					+ "." : liquid), Page.CYAN);
			page.add(0, 28, 0.85F, "Fill point", Page.GREY).right(WIDTH, 28, 0.85F, window > 0 ? Drums.buckets(window) + " READY" : "-",
					window > 0 ? Page.GREEN : Page.DIM);
			page.add(0, 40, 0.85F, "To pipes", Page.GREY).right(WIDTH, 40, 0.85F, pipes > 0 ? Drums.buckets(pipes) : "-", pipes > 0 ? Page.CYAN : Page.DIM);
			page.add(0, 52, 0.85F, "OUT flange", Page.GREY).right(WIDTH, 52, 0.85F, panel.flowing() ? Drums.buckets(panel.flow()) + "/s" : "-",
					panel.flowing() ? Page.GREEN : Page.DIM);
		}
		state.page = page;
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		JsonObject g = WarehouseLayout.get().section("liquids_out");
		int light = state.lightCoords;
		pose.pushPose();
		Page.pieceFrame(pose, state.facing, WarehousePiece.LIQUIDS_OUT);
		if (state.flowing) {
			JsonArray o = g.getAsJsonArray("out");
			pose.pushPose();
			pose.translate(o.get(0).getAsFloat() + g.get("lamp_dx").getAsFloat() - 8, o.get(1).getAsFloat() - 8, g.get("lamp_z").getAsFloat());
			WhQuads.submitPart(collector, pose, "flow_lamp", light, WhQuads.Tint.NONE);
			pose.popPose();
			JsonArray d = g.getAsJsonArray("disc");
			float cx = d.get(0).getAsFloat(), cy = d.get(1).getAsFloat(), z = d.get(2).getAsFloat();
			WhLiquid.face(collector, pose, cx - 0.75F, cy - 0.75F, cx + 0.75F, cy + 0.75F, z, state.liquid, light, state.pos);
		}
		if (state.page != null) {
			JsonObject s = g.getAsJsonObject("screen");
			float x0 = s.get("x0").getAsFloat(), x1 = s.get("x1").getAsFloat(), y1 = s.get("y1").getAsFloat();
			state.page.draw(pose, collector, x0 + 0.3F, y1 - 0.3F, s.get("z").getAsFloat(), (x1 - x0 - 0.6F) / WIDTH);
		}
		pose.popPose();
	}
}
