package io.github.jimbozoomer.jugcraft.client.jimtafacts;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.serialization.MapCodec;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.jimtafacts.network.*;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.special.*;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.joml.*;

/** Original source anchor mesh and two-pixel facade plate, sampling the actual chosen block model. */
public final class DigitalFacadeRenderer implements BlockEntityRenderer<DigitalFacades.Covers, DigitalFacadeRenderer.State> {
    private final DigitalStorageRenderer displays;
    public DigitalFacadeRenderer(BlockEntityRendererProvider.Context context) {displays=new DigitalStorageRenderer(context);}
    public static void register() { BlockEntityRenderers.register(DigitalFacades.TYPE, DigitalFacadeRenderer::new); SpecialModelRenderers.ID_MAPPER.put(Jugcraft.id("jimta_facade"), Unbaked.CODEC); }
    public static final class State extends BlockEntityRenderState {
        final java.util.List<net.minecraft.client.renderer.block.dispatch.BlockStateModelPart> parts=new java.util.ArrayList<>();
        final java.util.List<DigitalStorageRenderer.State> displays=new java.util.ArrayList<>();
        final ItemStackRenderState[] covers = new ItemStackRenderState[6], anchors = new ItemStackRenderState[6];
        State() { for (int i=0;i<6;i++) { covers[i]=new ItemStackRenderState(); anchors[i]=new ItemStackRenderState(); } }
    }
    @Override public State createRenderState() { return new State(); }
    @Override public void extractRenderState(DigitalFacades.Covers be, State state, float partial, Vec3 camera, ModelFeatureRenderer.CrumblingOverlay overlay) {
        BlockEntityRenderState.extractBase(be, state, overlay); var mc = Minecraft.getInstance(); var player = mc.player;state.parts.clear();
        state.displays.clear();
        for(var node:be.parts.nodes()){
            mc.getModelManager().getBlockStateModelSet().get(node.getBlockState()).collectParts(net.minecraft.util.RandomSource.create(0),state.parts);
            if(DigitalDisplays.monitor(node.kind())){var view=displays.createRenderState();displays.extractRenderState(node,view,partial,camera,overlay);state.displays.add(view);}
        }
        boolean hidden = player != null && (player.getMainHandItem().is(DigitalStorage.ITEMS.get("network_tool")) || player.getOffhandItem().is(DigitalStorage.ITEMS.get("network_tool")));
        for (Direction side : Direction.values()) {
            int i = side.ordinal(); state.covers[i].clear(); state.anchors[i].clear();
            if (!hidden && !be.material(side).isAir()) mc.getItemModelResolver().updateForTopItem(state.covers[i], new ItemStack(be.material(side).getBlock()), ItemDisplayContext.NONE, be.getLevel(), null, 0);
            if (be.anchored(side)) mc.getItemModelResolver().updateForTopItem(state.anchors[i], new ItemStack(DigitalStorage.ITEMS.get("cable_anchor")), ItemDisplayContext.NONE, be.getLevel(), null, 0);
        }
    }
    private static void face(PoseStack pose, Direction side) { switch (side) { case SOUTH -> pose.rotateDegrees(Axis.YP,180); case EAST -> pose.rotateDegrees(Axis.YP,-90); case WEST -> pose.rotateDegrees(Axis.YP,90); case UP -> pose.rotateDegrees(Axis.XP,90); case DOWN -> pose.rotateDegrees(Axis.XP,-90); default -> {} } }
    @Override public void submit(State s, PoseStack pose, SubmitNodeCollector out, CameraRenderState camera) {
        if(!s.parts.isEmpty())out.submitBlockModel(pose,net.minecraft.client.renderer.rendertype.RenderTypes.entityCutout(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS),s.parts,new int[0],s.lightCoords,OverlayTexture.NO_OVERLAY,0);
        for(var view:s.displays)displays.submit(view,pose,out,camera);
        for (Direction side : Direction.values()) {
            pose.pushPose(); pose.translate(.5,.5,.5); face(pose,side); s.anchors[side.ordinal()].submit(pose,out,s.lightCoords,OverlayTexture.NO_OVERLAY,0);
            pose.translate(0,0,-.4375); pose.scale(1,1,.125f); s.covers[side.ordinal()].submit(pose,out,s.lightCoords,OverlayTexture.NO_OVERLAY,0); pose.popPose();
        }
    }
    public record Unbaked() implements SpecialModelRenderer.Unbaked<ItemStackRenderState> {
        public static final MapCodec<Unbaked> CODEC = MapCodec.unit(new Unbaked());
        @Override public MapCodec<Unbaked> type() { return CODEC; }
        @Override public SpecialModelRenderer<ItemStackRenderState> bake(SpecialModelRenderer.BakingContext context) { return new Held(); }
    }
    private static final class Held implements SpecialModelRenderer<ItemStackRenderState> {
        @Override public ItemStackRenderState extractArgument(ItemStack item) {
            var result = new ItemStackRenderState(); var state = item.getOrDefault(DigitalFacades.MATERIAL, Blocks.STONE.defaultBlockState());
            if (!DigitalFacades.valid(state)) state = Blocks.STONE.defaultBlockState();
            var mc = Minecraft.getInstance(); mc.getItemModelResolver().updateForTopItem(result, new ItemStack(state.getBlock()), ItemDisplayContext.NONE, mc.level, null, 0); return result;
        }
        @Override public void submit(ItemStackRenderState item, PoseStack pose, SubmitNodeCollector out, int light, int overlay, boolean foil, int outline) {
            pose.pushPose(); pose.translate(.5,.5,.0625); pose.scale(1,1,.125f); item.submit(pose,out,light,overlay,outline); pose.popPose();
        }
        @Override public void getExtents(Consumer<Vector3fc> out) { out.accept(new Vector3f(0,0,0)); out.accept(new Vector3f(1,1,.125f)); }
    }
}
