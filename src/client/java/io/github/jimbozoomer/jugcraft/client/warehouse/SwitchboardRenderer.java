package io.github.jimbozoomer.jugcraft.client.warehouse;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.warehouse.PieceBlock;
import io.github.jimbozoomer.jugcraft.warehouse.SwitchboardBlockEntity;
import io.github.jimbozoomer.jugcraft.warehouse.WarehouseLayout;
import io.github.jimbozoomer.jugcraft.warehouse.WarehousePiece;
import io.github.jimbozoomer.jugcraft.warehouse.WarehousePower;
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
 * Part W5: the Warehouse Switchboard's live parts: the six outgoing breakers' toggles (up on, down off) and their lamps
 * (green on, amber shed, red off), the incomer's lamp, the UPS lamps; the power analyzer's and the UPS's screens; and
 * outside, the main switch's handle (up on, across off), the meter's reading and its pulse LED (faster the more is
 * drawn).
 */
public class SwitchboardRenderer implements BlockEntityRenderer<SwitchboardBlockEntity, SwitchboardRenderer.State> {
	static final int GREEN = 0xFF3CDC5A, AMBER = 0xFFFFAA14, RED = 0xFFF02818;
	private static final float WIDTH = 90;
	private final Font font;

	public static class State extends BlockEntityRenderState {
		Direction facing = Direction.NORTH;
		boolean mainOn = true, linked;
		int circuits, shed, storedPct;
		long supply, draw, time;
		@Nullable Page analyzer, ups, meter;
	}

	public SwitchboardRenderer(BlockEntityRendererProvider.Context context) {
		this.font = context.font();
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(SwitchboardBlockEntity sb, State state, float partialTick, Vec3 camera, ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(sb, state, crumbling);
		state.facing = sb.getBlockState().getBlock() instanceof PieceBlock ? sb.getBlockState().getValue(PieceBlock.FACING) : Direction.NORTH;
		state.mainOn = sb.mainOn();
		state.linked = sb.linked();
		state.circuits = sb.circuits();
		state.shed = sb.shed();
		state.storedPct = sb.storedPct();
		state.supply = sb.supply();
		state.draw = sb.draw();
		state.time = sb.getLevel() == null ? 0 : sb.getLevel().getGameTime();
		Page a = new Page(font);
		a.title(WIDTH, "POWER", "MDB-1");
		a.add(0, 14, 0.8F, "Supply", Page.GREY).right(WIDTH, 14, 0.8F, String.format(Locale.ROOT, "%,d JE/t", sb.supply()), Page.CYAN);
		a.add(0, 24, 0.8F, "Draw", Page.GREY).right(WIDTH, 24, 0.8F, String.format(Locale.ROOT, "%,d JE/t", sb.draw()), Page.WHITE);
		a.add(0, 34, 0.8F, "Stored", Page.GREY).right(WIDTH, 34, 0.8F, sb.storedPct() + " %", sb.storedPct() < 10 ? Page.AMBER : Page.GREEN);
		int on = Integer.bitCount(sb.circuits() & 0x3F), shed = Integer.bitCount(sb.shed() & sb.circuits() & 0x3F);
		a.add(0, 44, 0.8F, "Circuits", Page.GREY).right(WIDTH, 44, 0.8F, on + " / 6 on" + (shed > 0 ? "  " + shed + " SHED" : ""), shed > 0 ? Page.AMBER : Page.WHITE);
		a.add(0, 54, 0.8F, "Main", Page.GREY).right(WIDTH, 54, 0.8F, !sb.mainOn() ? "OFF" : sb.linked() ? "ON" : "NO WAREHOUSE", !sb.mainOn() ? Page.RED
				: sb.linked() ? Page.GREEN : Page.AMBER);
		state.analyzer = a;
		boolean mains = sb.supply() > 0;
		Page u = new Page(font);
		u.title(WIDTH, "UPS", "CONTROL");
		u.add(0, 14, 0.8F, "Mains", Page.GREY).right(WIDTH, 14, 0.8F, mains ? "OK" : "NO SUPPLY", mains ? Page.GREEN : Page.AMBER);
		u.add(0, 25, 0.8F, "Battery", Page.GREY).right(WIDTH, 25, 0.8F, "100 %", Page.GREEN);
		u.add(0, 36, 0.8F, "Runs", Page.GREY).right(WIDTH, 36, 0.8F, "terminal, screens", Page.WHITE);
		state.ups = u;
		Page m = new Page(font);
		m.right(60, 1, 1.4F, String.format(Locale.ROOT, "%d JE/t", sb.draw()), 0xFF1C2814);
		state.meter = m;
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		JsonObject g = WarehouseLayout.get().section("switchboard");
		int light = state.lightCoords;
		pose.pushPose();
		Page.pieceFrame(pose, state.facing, WarehousePiece.SWITCHBOARD);
		// The breakers' toggles and lamps.
		JsonArray toggles = g.getAsJsonArray("toggles"), lamps = g.getAsJsonArray("lamps");
		for (int k = 0; k < WarehousePower.Circuit.values().length && k < toggles.size(); k++) {
			boolean on = (state.circuits >> k & 1) != 0, shed = (state.shed >> k & 1) != 0;
			JsonArray t = toggles.get(k).getAsJsonArray();
			pose.pushPose();
			pose.translate(t.get(0).getAsFloat(), on ? t.get(1).getAsFloat() : t.get(2).getAsFloat(), t.get(3).getAsFloat());
			WhQuads.submitPart(collector, pose, "w5_toggle", light, WhQuads.Tint.NONE);
			pose.popPose();
			if (state.linked && state.mainOn) {
				JsonArray l = lamps.get(k).getAsJsonArray();
				WhQuads.lamp(collector, pose, l.get(0).getAsFloat(), l.get(1).getAsFloat(), l.get(2).getAsFloat(), l.get(3).getAsFloat(),
						!on ? RED : shed ? AMBER : GREEN);
			}
		}
		boolean live = state.supply > 0 || state.storedPct > 0;
		JsonArray main = g.getAsJsonArray("main_lamp");
		WhQuads.lamp(collector, pose, main.get(0).getAsFloat(), main.get(1).getAsFloat(), main.get(2).getAsFloat(), main.get(3).getAsFloat(),
				!state.mainOn ? RED : live ? GREEN : AMBER);
		JsonArray ups = g.getAsJsonArray("ups_lamps");
		for (int k = 0; k < ups.size(); k++) {
			JsonArray l = ups.get(k).getAsJsonArray();
			WhQuads.lamp(collector, pose, l.get(0).getAsFloat(), l.get(1).getAsFloat(), l.get(2).getAsFloat(), l.get(3).getAsFloat(),
					k == 0 && state.supply <= 0 ? AMBER : GREEN);
		}
		page(pose, collector, g.getAsJsonObject("analyzer"), state.analyzer, WIDTH);
		page(pose, collector, g.getAsJsonObject("ups"), state.ups, WIDTH);
		// Outside (its faces to -z): the main switch's handle, the meter's reading and its pulse.
		JsonArray h = g.getAsJsonArray("handle");
		pose.pushPose();
		pose.translate(h.get(0).getAsFloat(), h.get(1).getAsFloat(), h.get(2).getAsFloat());
		if (!state.mainOn) {
			pose.rotateDegrees(Axis.ZP, 90);
		}
		WhQuads.submitPart(collector, pose, "w5_handle", light, WhQuads.Tint.NONE);
		pose.popPose();
		JsonObject m = g.getAsJsonObject("meter");
		float x0 = m.get("x0").getAsFloat(), x1 = m.get("x1").getAsFloat(), y1 = m.get("y1").getAsFloat(), z = m.get("z").getAsFloat();
		if (state.meter != null) {
			pose.pushPose();
			pose.translate(x1, 0, z);
			pose.rotateDegrees(Axis.YP, 180);
			state.meter.draw(pose, collector, 0.25F, y1 - 0.5F, 0, (x1 - x0 - 0.5F) / 60);
			pose.popPose();
		}
		long period = state.draw <= 0 ? 0 : Math.max(4, 4000 / state.draw);
		if (period > 0 && state.time % period < 2) {
			JsonArray p = g.getAsJsonArray("pulse");
			pose.pushPose();
			pose.translate(p.get(0).getAsFloat(), 0, p.get(2).getAsFloat());
			pose.rotateDegrees(Axis.YP, 180);
			WhQuads.lamp(collector, pose, 0, p.get(1).getAsFloat(), 0.02F, p.get(3).getAsFloat(), RED);
			pose.popPose();
		}
		pose.popPose();
	}

	private static void page(PoseStack pose, SubmitNodeCollector collector, JsonObject s, @Nullable Page page, float width) {
		if (page == null) {
			return;
		}
		float x0 = s.get("x0").getAsFloat(), x1 = s.get("x1").getAsFloat(), y1 = s.get("y1").getAsFloat();
		page.draw(pose, collector, x0 + 0.3F, y1 - 0.3F, s.get("z").getAsFloat(), (x1 - x0 - 0.6F) / width);
	}

	@Override
	public int getViewDistance() {
		return 48;
	}

	@Override
	public boolean shouldRenderOffScreen() {
		return true;
	}
}
