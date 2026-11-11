package io.github.jimbozoomer.jugcraft.client.warehouse;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jimbozoomer.jugcraft.warehouse.GoodsInBlockEntity;
import io.github.jimbozoomer.jugcraft.warehouse.PieceBlock;
import io.github.jimbozoomer.jugcraft.warehouse.WarehouseLayout;
import io.github.jimbozoomer.jugcraft.warehouse.WarehousePiece;
import java.util.Locale;
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
 * The Goods In panel's live parts: the drop-off hatch's screen (to the intake or not, what is waiting to be packed, what
 * the warehouse has stored), each pipe inlet's flow lamp (lit while items come in, blinking when the intake can't take
 * them), and the hatch's lamp (lit when there is an intake behind the wall).
 */
public class GoodsInRenderer implements BlockEntityRenderer<GoodsInBlockEntity, GoodsInRenderer.State> {
	private static final float WIDTH = 180;
	private final Font font;

	public static class State extends BlockEntityRenderState {
		Direction facing = Direction.NORTH;
		final boolean[] lamps = new boolean[GoodsInBlockEntity.INLETS];
		boolean hatch;
		@Nullable Page page;
	}

	public GoodsInRenderer(BlockEntityRendererProvider.Context context) {
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
	public void extractRenderState(GoodsInBlockEntity panel, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(panel, state, crumbling);
		state.facing = panel.getBlockState().getBlock() instanceof PieceBlock ? panel.getBlockState().getValue(PieceBlock.FACING) : Direction.NORTH;
		long time = panel.getLevel() == null ? 0 : panel.getLevel().getGameTime();
		for (int i = 0; i < GoodsInBlockEntity.INLETS; i++) {
			state.lamps[i] = panel.rate(i) > 0 || panel.backed(i) && (time / 8) % 2 == 0;
		}
		state.hatch = panel.linked();
		Page page = new Page(font);
		page.title(WIDTH, "DROP OFF", "GOODS IN");
		page.add(0, 16, 1, "Push the flap, put items in", Page.GREY);
		page.add(0, 30, 1, "To the intake", Page.GREY).right(WIDTH, 30, 1, panel.linked() ? "OK" : "NO INTAKE", panel.linked() ? Page.GREEN : Page.RED);
		page.add(0, 44, 1, "Waiting to be packed", Page.GREY).right(WIDTH, 44, 1, String.format(Locale.ROOT, "%,d", panel.waiting()),
				panel.waiting() > 0 ? Page.AMBER : Page.WHITE);
		page.add(0, 58, 1, "Cases on their way", Page.GREY).right(WIDTH, 58, 1, String.valueOf(panel.cases()), Page.WHITE);
		page.add(0, 72, 1, "Stored", Page.GREY).right(WIDTH, 72, 1, String.format(Locale.ROOT, "%,d", panel.stored()), Page.CYAN);
		state.page = page;
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		pose.pushPose();
		Page.pieceFrame(pose, state.facing, WarehousePiece.GOODS_IN);
		JsonObject g = WarehouseLayout.get().section("goods_in");
		JsonArray inlets = g.getAsJsonArray("inlets");
		float dx = g.get("lamp_dx").getAsFloat(), lz = g.get("lamp_z").getAsFloat();
		for (int i = 0; i < inlets.size(); i++) {
			if (!state.lamps[i]) {
				continue;
			}
			JsonArray c = inlets.get(i).getAsJsonArray();
			pose.pushPose();
			pose.translate(c.get(0).getAsFloat() + dx - 8, c.get(1).getAsFloat() - 8, lz);
			WhQuads.submitPart(collector, pose, "flow_lamp", state.lightCoords, WhQuads.Tint.NONE);
			pose.popPose();
		}
		if (state.hatch) {
			JsonArray h = g.getAsJsonArray("hatch_lamp");
			pose.pushPose();
			pose.translate(h.get(0).getAsFloat() - 8, h.get(1).getAsFloat() - 8, h.get(2).getAsFloat());
			WhQuads.submitPart(collector, pose, "flow_lamp", state.lightCoords, WhQuads.Tint.NONE);
			pose.popPose();
		}
		if (state.page != null) {
			JsonObject s = g.getAsJsonObject("screen");
			float x0 = s.get("x0").getAsFloat(), x1 = s.get("x1").getAsFloat(), y1 = s.get("y1").getAsFloat();
			state.page.draw(pose, collector, x0 + 0.3F, y1 - 0.3F, s.get("z").getAsFloat(), (x1 - x0 - 0.6F) / WIDTH);
		}
		pose.popPose();
	}
}
