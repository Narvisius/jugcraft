package io.github.jimbozoomer.jugcraft.client.jimtafacts;

import com.mojang.blaze3d.vertex.*;
import io.github.jimbozoomer.jugcraft.jimtafacts.network.DigitalUtilityStorage;
import net.fabricmc.fabric.api.transfer.v1.client.fluid.FluidVariantRendering;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Real animated fluid material inside the supplied two-pixel sky-tank frame. */
public final class SkyTankRenderer implements BlockEntityRenderer<DigitalUtilityStorage.Tank, SkyTankRenderer.State> {
    public SkyTankRenderer(BlockEntityRendererProvider.Context context) {}
    public static final class State extends BlockEntityRenderState { float fill; int color; TextureAtlasSprite sprite; }
    @Override public State createRenderState() { return new State(); }
    @Override public void extractRenderState(DigitalUtilityStorage.Tank tank, State state, float partial, Vec3 camera, ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
        BlockEntityRenderState.extractBase(tank, state, crumbling);
        state.fill = tank.storage.isResourceBlank() ? 0 : (float) tank.storage.amount / tank.storage.getCapacity();
        if (state.fill > 0) {
            state.sprite = Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(tank.storage.variant.getFluid().defaultFluidState()).stillMaterial().sprite();
            state.color = 0xE0000000 | (FluidVariantRendering.getColor(tank.storage.variant) & 0xFFFFFF);
        }
    }
    @Override public void submit(State s, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (s.fill <= 0 || s.sprite == null) return;
        float lo = 2.01F / 16, hi = 1 - lo, top = lo + (hi - lo) * Math.min(1, s.fill);
        collector.submitCustomGeometry(pose, RenderTypes.entityTranslucentCull(TextureAtlas.LOCATION_BLOCKS), (matrix, buffer) -> {
            quad(buffer, matrix, s, 0, 1, 0, lo, top, lo, lo, top, hi, hi, top, hi, hi, top, lo);
            quad(buffer, matrix, s, 0, -1, 0, lo, lo, lo, hi, lo, lo, hi, lo, hi, lo, lo, hi);
            quad(buffer, matrix, s, 0, 0, -1, lo, lo, lo, lo, top, lo, hi, top, lo, hi, lo, lo);
            quad(buffer, matrix, s, 0, 0, 1, hi, lo, hi, hi, top, hi, lo, top, hi, lo, lo, hi);
            quad(buffer, matrix, s, -1, 0, 0, lo, lo, hi, lo, top, hi, lo, top, lo, lo, lo, lo);
            quad(buffer, matrix, s, 1, 0, 0, hi, lo, lo, hi, top, lo, hi, top, hi, hi, lo, hi);
        });
    }
    private static void quad(VertexConsumer out, PoseStack.Pose matrix, State s, float nx, float ny, float nz, float... p) {
        float[][] uv = {{s.sprite.getU0(), s.sprite.getV1()}, {s.sprite.getU0(), s.sprite.getV0()}, {s.sprite.getU1(), s.sprite.getV0()}, {s.sprite.getU1(), s.sprite.getV1()}};
        for (int i = 0; i < 4; i++) out.addVertex(matrix, p[i * 3], p[i * 3 + 1], p[i * 3 + 2]).setColor(s.color).setUv(uv[i][0], uv[i][1]).setOverlay(OverlayTexture.NO_OVERLAY).setLight(s.lightCoords).setNormal(matrix, nx, ny, nz);
    }
}
