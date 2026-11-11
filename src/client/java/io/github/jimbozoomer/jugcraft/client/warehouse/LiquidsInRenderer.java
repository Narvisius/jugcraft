package io.github.jimbozoomer.jugcraft.client.warehouse;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jimbozoomer.jugcraft.warehouse.Drums;
import io.github.jimbozoomer.jugcraft.warehouse.FillerBlockEntity;
import io.github.jimbozoomer.jugcraft.warehouse.LiquidsInBlockEntity;
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
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The Liquids In panel's live parts (part W4): each inlet's flow lamp (lit while liquid comes in) and its screen
 * (LIQUIDS IN: the inlets, the liquid going into drums now, the filling station's buffer tank).
 */
public class LiquidsInRenderer implements BlockEntityRenderer<LiquidsInBlockEntity, LiquidsInRenderer.State> {
	private static final float WIDTH = 110;
	private final Font font;

	public static class State extends BlockEntityRenderState {
		Direction facing = Direction.NORTH;
		final boolean[] lit = new boolean[4];
		@Nullable Page page;
	}

	public LiquidsInRenderer(BlockEntityRendererProvider.Context context) {
		this.font = context.font();
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(LiquidsInBlockEntity panel, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(panel, state, crumbling);
		state.facing = panel.getBlockState().getBlock() instanceof PieceBlock ? panel.getBlockState().getValue(PieceBlock.FACING) : Direction.NORTH;
		for (int i = 0; i < 4; i++) {
			state.lit[i] = panel.rate(i) > 0;
		}
		Page page = new Page(font);
		page.title(WIDTH, "LIQUIDS IN", "DRUM FILLING");
		if (!panel.linked()) {
			page.add(0, 18, 1, "NO FILLING STATION", Page.AMBER);
		} else {
			// Four inlets in two columns, the tanker coupling, what goes into drums now, the buffer tank.
			for (int i = 0; i < LiquidsInBlockEntity.INLETS; i++) {
				String label = i == LiquidsInBlockEntity.TANKER ? "TANKER" : "LIQ" + (i + 1);
				String what = panel.rate(i) > 0 ? Drums.buckets(panel.rate(i)) + "/s" : panel.waits(i) ? "WAIT" : "-";
				int colour = panel.rate(i) > 0 ? Page.GREEN : panel.waits(i) ? Page.AMBER : Page.DIM;
				float x = i % 2 == 0 || i == LiquidsInBlockEntity.TANKER ? 0 : WIDTH / 2 + 3, y = 14 + (i / 2) * 9;
				page.add(x, y, 0.75F, label, Page.GREY).right(x + WIDTH / 2 - 3, y, 0.75F, what, colour);
			}
			FillerBlockEntity f = panel.getLevel() != null && panel.filler() != null ? panel.filler() : null;
			if (f != null) {
				page.right(WIDTH, 32, 0.75F, f.liquid().isBlank() ? "-" : name(f.liquid()), Page.WHITE);
				page.add(0, 41, 0.75F, "TANK", Page.GREY).right(WIDTH, 41, 0.75F, Drums.buckets(f.tank()) + " / " + Drums.buckets(FillerBlockEntity.TANK),
						Page.CYAN);
			}
		}
		state.page = page;
	}

	private static String name(FluidVariant v) {
		String n = v.isBlank() ? "?" : Drums.name(v).getString().toUpperCase(Locale.ROOT);
		return n.length() > 10 ? n.substring(0, 9) + "." : n;
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		JsonObject g = WarehouseLayout.get().section("liquids_in");
		int light = state.lightCoords;
		pose.pushPose();
		Page.pieceFrame(pose, state.facing, WarehousePiece.LIQUIDS_IN);
		JsonArray inlets = g.getAsJsonArray("inlets");
		float dx = g.get("lamp_dx").getAsFloat(), lz = g.get("lamp_z").getAsFloat();
		for (int i = 0; i < 4; i++) {
			if (!state.lit[i]) {
				continue;
			}
			JsonArray p = inlets.get(i).getAsJsonArray();
			pose.pushPose();
			pose.translate(p.get(0).getAsFloat() + dx - 8, p.get(1).getAsFloat() - 8, lz);
			WhQuads.submitPart(collector, pose, "flow_lamp", light, WhQuads.Tint.NONE);
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
