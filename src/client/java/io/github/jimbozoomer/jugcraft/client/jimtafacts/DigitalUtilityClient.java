package io.github.jimbozoomer.jugcraft.client.jimtafacts;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.jimtafacts.network.*;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.model.object.chest.ChestModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.blockentity.state.ChestRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Original sky-chest cube dimensions, UVs, hinge pivots and cubic lid easing. */
public final class DigitalUtilityClient {
    private DigitalUtilityClient() {}
    public static void register() {
        BlockEntityRenderers.register(DigitalUtilityStorage.CHEST, SkyChest::new);
        BlockEntityRenderers.register(DigitalUtilityStorage.TANK, SkyTankRenderer::new);
    }
    private static final class SkyState extends ChestRenderState { boolean smooth; }
    public static final class SkyChest extends ChestRenderer<DigitalUtilityStorage.Chest> {
        private final ChestModel source = new ChestModel(layer().bakeRoot());
        public SkyChest(BlockEntityRendererProvider.Context context) { super(context); }
        @Override public ChestRenderState createRenderState() { return new SkyState(); }
        @Override public void extractRenderState(DigitalUtilityStorage.Chest chest, ChestRenderState state, float partial, Vec3 camera, ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
            super.extractRenderState(chest, state, partial, camera, crumbling);
            ((SkyState) state).smooth = chest.getBlockState().is(DigitalStorage.BLOCKS.get("smooth_sky_stone_chest"));
        }
        @Override public void submit(ChestRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
            pose.pushPose(); pose.mulPose(modelTransformation(state.facing));
            float open = 1 - (float) Math.pow(1 - state.open, 3);
            collector.submitModel(source, open, pose, Jugcraft.id("textures/jimtafacts/ae2/block/" + (((SkyState) state).smooth ? "skyblockchest" : "skychest") + ".png"), state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
        }
        private static LayerDefinition layer() {
            var mesh = new MeshDefinition(); var root = mesh.getRoot();
            root.addOrReplaceChild("bottom", CubeListBuilder.create().texOffs(0, 19).addBox(1, 0, 1, 14, 10, 14), PartPose.ZERO);
            root.addOrReplaceChild("lid", CubeListBuilder.create().texOffs(0, 0).addBox(1, 0, 0, 14, 5, 14), PartPose.offset(0, 10, 1));
            root.addOrReplaceChild("lock", CubeListBuilder.create().texOffs(0, 0).addBox(7, -1, 15, 2, 4, 1), PartPose.offset(0, 9, 0));
            return LayerDefinition.create(mesh, 64, 64);
        }
    }
}
