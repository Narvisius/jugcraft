package io.github.jimbozoomer.jugcraft.client.warehouse;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.warehouse.AisleSignBlockEntity;
import io.github.jimbozoomer.jugcraft.warehouse.DecorBlock;
import io.github.jimbozoomer.jugcraft.warehouse.WarehouseLayout;
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

/** An aisle sign's two faces, live: the aisle (with its floor) big, its bays and its levels under it (part W3). */
public class AisleSignRenderer implements BlockEntityRenderer<AisleSignBlockEntity, AisleSignRenderer.State> {
	private static final int WIDTH = 120;
	private final Font font;

	public static class State extends BlockEntityRenderState {
		Direction facing = Direction.NORTH;
		@Nullable Page page;
	}

	public AisleSignRenderer(BlockEntityRendererProvider.Context context) {
		this.font = context.font();
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(AisleSignBlockEntity sign, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(sign, state, crumbling);
		state.facing = sign.getBlockState().getBlock() instanceof DecorBlock ? sign.getBlockState().getValue(DecorBlock.FACING) : Direction.NORTH;
		if (sign.aisle().isEmpty()) {
			state.page = null;
			return;
		}
		Page p = new Page(font);
		float big = 2.2F;
		p.add((WIDTH - font.width(sign.aisle()) * big) / 2, 4, big, sign.aisle(), 0xFFFAFAF6);
		p.add((WIDTH - font.width(sign.bays())) / 2F, 34, 1, sign.bays(), 0xFFFAFAF6);
		p.add((WIDTH - font.width(sign.levels()) * 0.8F) / 2, 46, 0.8F, sign.levels(), 0xFFC8D6EC);
		state.page = p;
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		if (state.page == null) {
			return;
		}
		JsonObject s = WarehouseLayout.get().section("aisle_sign_face");
		float x0 = s.get("x0").getAsFloat(), x1 = s.get("x1").getAsFloat(), y1 = s.get("y1").getAsFloat();
		float scale = (x1 - x0) / WIDTH;
		pose.pushPose();
		Page.blockFrame(pose, state.facing);
		state.page.draw(pose, collector, x0, y1 - 0.3F, s.get("z_front").getAsFloat(), scale);
		// The back face: turned half round about the block's middle.
		pose.translate(8, 0, 8);
		pose.rotateDegrees(Axis.YP, 180);
		pose.translate(-8, 0, -8);
		state.page.draw(pose, collector, x0, y1 - 0.3F, 16 - s.get("z_back").getAsFloat(), scale);
		pose.popPose();
	}
}
