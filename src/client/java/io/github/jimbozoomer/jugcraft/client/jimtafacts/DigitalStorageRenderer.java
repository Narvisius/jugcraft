package io.github.jimbozoomer.jugcraft.client.jimtafacts;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.client.ConcordanceClientOptions;
import io.github.jimbozoomer.jugcraft.jimtafacts.network.DigitalNode;
import io.github.jimbozoomer.jugcraft.jimtafacts.network.DigitalNodeBlock;
import io.github.jimbozoomer.jugcraft.jimtafacts.network.DigitalStorage;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.*;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;

/** Source AE bay positions, chassis meshes, LED quads, press geometry/easing and charger bob, adapted to Fabric.
 * Original sources and notices: art/owner-library/originals/Oct23/Jimtafacts/applied energentics reborn by jimbo.
 * No supplied class is loaded. Only bare visible item identities and aggregate cell status reach this renderer. */
public final class DigitalStorageRenderer implements BlockEntityRenderer<DigitalNode, DigitalStorageRenderer.State> {
    private static final Identifier INSIDE = Jugcraft.id("textures/jimtafacts/ae2/block/inscriber_inside.png");
    private static final int[] CELL_COLORS = {0, 0x00FF00, 0x00AAFF, 0xFFAA00, 0xFF0000};
    // Exact CellLedRenderer face positions: front, right, left, top, bottom; slight depth offset avoids z fighting.
    private static final float[] LED_QUADS = {
        .25F,.0625F,-.0000625F, .3125F,.0625F,-.0000625F, .3125F,-.0000625F,-.0000625F, .25F,-.0000625F,-.0000625F,
        .3125F,.0625F,-.0000625F, .3125F,.0625F,.0624375F, .3125F,-.0000625F,.0624375F, .3125F,-.0000625F,-.0000625F,
        .25F,.0625F,.0624375F, .25F,.0625F,-.0000625F, .25F,-.0000625F,-.0000625F, .25F,-.0000625F,.0624375F,
        .25F,.0625F,.0624375F, .3125F,.0625F,.0624375F, .3125F,.0625F,-.0000625F, .25F,.0625F,-.0000625F,
        .25F,-.0000625F,-.0000625F, .3125F,-.0000625F,-.0000625F, .3125F,-.0000625F,.0624375F, .25F,-.0000625F,.0624375F};
    private final ItemModelResolver resolver;
    private final net.minecraft.client.gui.Font font;
    public DigitalStorageRenderer(BlockEntityRendererProvider.Context context) { resolver = context.itemModelResolver(); font = context.font(); }
    public static void register() { BlockEntityRenderers.register(DigitalStorage.NODE, DigitalStorageRenderer::new); }
    public static final class State extends BlockEntityRenderState {
        String kind = "";
        Direction facing = Direction.NORTH;
        final ItemStackRenderState[] cells = new ItemStackRenderState[10];
        final int[] colors = new int[10];
        final ItemStackRenderState top = new ItemStackRenderState(), middle = new ItemStackRenderState();
        float press = .2F, bob, crankAngle;
        String quantity = "";
        State() { for (int i = 0; i < cells.length; i++) cells[i] = new ItemStackRenderState(); }
    }
    @Override public State createRenderState() { return new State(); }
    @Override public void extractRenderState(DigitalNode node, State state, float partial, Vec3 camera, ModelFeatureRenderer.CrumblingOverlay crumbling) {
        BlockEntityRenderState.extractBase(node, state, crumbling);
        state.kind = node.kind(); state.facing = node.getBlockState().getValue(DigitalNodeBlock.FACING);
        state.top.clear(); state.middle.clear(); for (var cell : state.cells) cell.clear();
        double ticks = node.getLevel() == null ? 0 : node.getLevel().getGameTime() + partial;
        boolean reduced = ConcordanceClientOptions.reducedMotion();
        if (state.kind.equals("crank")) {
            ItemStack handle = new ItemStack(Items.PAPER); handle.set(DataComponents.ITEM_MODEL, Jugcraft.id("jimtafacts/ae2/crank_handle"));
            resolver.updateForTopItem(state.middle, handle, ItemDisplayContext.NONE, node.getLevel(), null, 0);
            float progress = reduced ? 1 : Math.clamp((float) (ticks - node.crankAt) / 5, 0, 1);
            state.crankAngle = (node.crankTurns - 1 + progress) * 90;
        }
        if (io.github.jimbozoomer.jugcraft.jimtafacts.network.DigitalDisplays.monitor(state.kind)) {
            resolver.updateForTopItem(state.middle, node.visibleResult(), ItemDisplayContext.GUI, node.getLevel(), null, 0);
            state.quantity = node.getBlockState().getValue(DigitalNodeBlock.LIT) ? Long.toString(node.visibleCount()) : "Offline";
        }
        for (int slot = 0; slot < node.cellSlots(); slot++) {
            int id = node.visibleCell(slot); if (id == 0) continue;
            int tier = (id - 1) % 5, size = new int[]{1, 4, 16, 64, 256}[tier];
            ItemStack visual = new ItemStack(Items.PAPER);
            visual.set(DataComponents.ITEM_MODEL, Jugcraft.id("jimtafacts/ae2/drive_" + size + "k_" + (id > 5 ? "fluid" : "item") + "_cell"));
            resolver.updateForTopItem(state.cells[slot], visual, ItemDisplayContext.NONE, node.getLevel(), null, slot);
            int color = node.getBlockState().getValue(DigitalNodeBlock.LIT) ? CELL_COLORS[node.visibleCellStatus(slot)] : 0;
            if (!reduced && color != 0 && node.visibleCellBlink(slot) > ticks) {
                // Original 200 ms blink phase and cubic ease towards pale red.
                float f = (float) ((ticks * 50 % 200) / 200);
                float blend = f < .5F ? 4 * f * f * f : 1 - (float) Math.pow(-2 * f + 2, 3) / 2;
                int r = Math.round((color >> 16 & 255) * (1 - blend) + 255 * blend);
                int g = Math.round((color >> 8 & 255) * (1 - blend) + 127.5F * blend);
                int b = Math.round((color & 255) * (1 - blend) + 127.5F * blend);
                color = r << 16 | g << 8 | b;
            }
            state.colors[slot] = color;
        }
        if (state.kind.equals("charger")) {
            state.bob = reduced ? 0 : (float) Math.sin(ticks / 20.0) * .02F;
            resolver.updateForTopItem(state.middle, node.visibleMiddle(), ItemDisplayContext.GROUND, node.getLevel(), null, 0);
        } else if (state.kind.equals("inscriber")) {
            float relative = (float) ((ticks - node.visibleCycle()) / 8.0);
            float progress = relative < 0 || relative >= 2 || reduced ? 0
                    : relative > 1 ? 1 - (float) Math.pow(relative - 1, 5)
                    : (float) (1 + 2.70158 * Math.pow(relative - 1, 3) + 1.70158 * Math.pow(relative - 1, 2));
            state.press = .2F - progress / 5;
            ItemStack middle = relative >= 1 && relative < 2 ? node.visibleResult() : node.visibleMiddle();
            if (middle.isEmpty()) middle = node.visibleResult();
            resolver.updateForTopItem(state.middle, middle, ItemDisplayContext.ON_SHELF, node.getLevel(), null, 0);
            resolver.updateForTopItem(state.top, node.visibleTop(), ItemDisplayContext.ON_SHELF, node.getLevel(), null, 1);
        }
    }
    @Override public void submit(State state, PoseStack pose, SubmitNodeCollector nodes, CameraRenderState camera) {
        if (state.kind.equals("crank")) {
            pose.pushPose(); pose.translate(.5, .5, .5);
            switch (state.facing) { case DOWN -> pose.rotateDegrees(Axis.XP, 180); case NORTH -> pose.rotateDegrees(Axis.XP, 270); case SOUTH -> pose.rotateDegrees(Axis.XP, 90); case WEST -> pose.rotateDegrees(Axis.ZP, 90); case EAST -> pose.rotateDegrees(Axis.ZP, 270); default -> {} }
            pose.rotateDegrees(Axis.YP, -state.crankAngle); state.middle.submit(pose, nodes, state.lightCoords, OverlayTexture.NO_OVERLAY, 0); pose.popPose(); return;
        }
        boolean monitor = io.github.jimbozoomer.jugcraft.jimtafacts.network.DigitalDisplays.monitor(state.kind);
        if (!monitor && !state.kind.equals("drive") && !state.kind.equals("me_chest") && !state.kind.equals("inscriber") && !state.kind.equals("charger")) return;
        pose.pushPose(); pose.translate(.5, .5, .5);
        if(state.facing==Direction.UP)pose.rotateDegrees(Axis.XP,90);
        else if(state.facing==Direction.DOWN)pose.rotateDegrees(Axis.XP,-90);
        else pose.rotateDegrees(Axis.YP,180-state.facing.toYRot());
        pose.translate(-.5, -.5, -.5);
        if (monitor) {
            pose.pushPose(); pose.translate(.5, .57, -.008); pose.rotateDegrees(Axis.YP, 180); pose.scale(.33F, .33F, .33F);
            state.middle.submit(pose, nodes, state.lightCoords, OverlayTexture.NO_OVERLAY, 0); pose.popPose();
            pose.pushPose(); pose.translate(.5, .28, -.009); pose.rotateDegrees(Axis.YP, 180); pose.scale(.007F, -.007F, .007F);
            var text = net.minecraft.network.chat.Component.literal(state.quantity).getVisualOrderText();
            nodes.submitText(pose, -font.width(text) / 2F, 0, text, false, net.minecraft.client.gui.Font.DisplayMode.POLYGON_OFFSET, state.lightCoords, 0xFFE8F5FA, 0, 0); pose.popPose();
        } else if (state.kind.equals("drive") || state.kind.equals("me_chest")) {
            for (int slot = 0; slot < (state.kind.equals("drive") ? 10 : 1); slot++) {
                if (state.cells[slot].isEmpty()) continue;
                pose.pushPose();
                if (state.kind.equals("drive")) pose.translate((9 - slot % 2 * 8) / 16.0, (13 - slot / 2 * 3) / 16.0, .0625);
                else pose.translate(.3125, .25, 0);
                pose.pushPose(); pose.translate(.5, .5, .5);
                state.cells[slot].submit(pose, nodes, state.lightCoords, OverlayTexture.NO_OVERLAY, 0); pose.popPose();
                int color = state.colors[slot];
                nodes.submitCustomGeometry(pose, RenderTypes.debugQuads(), (p, out) -> {
                    for (int i = 0; i < LED_QUADS.length; i += 3) out.addVertex(p, LED_QUADS[i], LED_QUADS[i + 1], LED_QUADS[i + 2]).setColor(0xFF000000 | color);
                });
                pose.popPose();
            }
        } else if (state.kind.equals("charger")) {
            pose.translate(.5, .35 + state.bob, .5); state.middle.submit(pose, nodes, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        } else {
            press(pose, nodes, state.press, state.lightCoords);
            item(pose, nodes, state.middle, 0, state.lightCoords);
            item(pose, nodes, state.top, state.press, state.lightCoords);
        }
        pose.popPose();
    }
    private static void item(PoseStack pose, SubmitNodeCollector nodes, ItemStackRenderState item, float offset, int light) {
        if (item.isEmpty()) return;
        pose.pushPose(); pose.translate(.5, .5 + offset, .5); pose.rotateDegrees(Axis.XP, 90); pose.scale(.41666665F, .41666665F, .41666665F);
        item.submit(pose, nodes, light, OverlayTexture.NO_OVERLAY, 0); pose.popPose();
    }
    private static void vertex(VertexConsumer out, PoseStack.Pose pose, float x, float y, float z, float u, float v, int light, Direction face) {
        out.addVertex(pose, x, y, z).setColor(-1).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
                .setNormal(pose, face.getStepX(), face.getStepY(), face.getStepZ());
    }
    private static void press(PoseStack stack, SubmitNodeCollector nodes, float press, int light) {
        nodes.submitCustomGeometry(stack, RenderTypes.entityTranslucent(INSIDE), (pose, out) -> {
            // Exact six supplied moving-face quads and UV formulas from InscriberRenderer.
            float m = .52F, rim = .125F - (press - .4F);
            vertex(out, pose, .125F,m+press,.125F,.875F,.125F,light,Direction.DOWN);
            vertex(out, pose, .875F,m+press,.125F,.125F,.125F,light,Direction.DOWN);
            vertex(out, pose, .875F,m+press,.875F,.125F,.875F,light,Direction.DOWN);
            vertex(out, pose, .125F,m+press,.875F,.875F,.875F,light,Direction.DOWN);
            vertex(out, pose, .125F,m+.4F,.125F,.125F,rim,light,Direction.NORTH);
            vertex(out, pose, .875F,m+.4F,.125F,.875F,rim,light,Direction.NORTH);
            vertex(out, pose, .875F,m+press,.125F,.875F,.125F,light,Direction.NORTH);
            vertex(out, pose, .125F,m+press,.125F,.125F,.125F,light,Direction.NORTH);
            vertex(out, pose, .125F,m+.4F,.875F,.125F,rim,light,Direction.SOUTH);
            vertex(out, pose, .125F,m+press,.875F,.125F,.125F,light,Direction.SOUTH);
            vertex(out, pose, .875F,m+press,.875F,.875F,.125F,light,Direction.SOUTH);
            vertex(out, pose, .875F,m+.4F,.875F,.875F,rim,light,Direction.SOUTH);
            m = .48F;
            vertex(out, pose, .875F,m-press,.125F,.875F,.125F,light,Direction.UP);
            vertex(out, pose, .125F,m-press,.125F,.125F,.125F,light,Direction.UP);
            vertex(out, pose, .125F,m-press,.875F,.125F,.875F,light,Direction.UP);
            vertex(out, pose, .875F,m-press,.875F,.875F,.875F,light,Direction.UP);
            vertex(out, pose, .875F,m-.4F,.125F,.125F,rim,light,Direction.NORTH);
            vertex(out, pose, .125F,m-.4F,.125F,.875F,rim,light,Direction.NORTH);
            vertex(out, pose, .125F,m-press,.125F,.875F,.125F,light,Direction.NORTH);
            vertex(out, pose, .875F,m-press,.125F,.125F,.125F,light,Direction.NORTH);
            vertex(out, pose, .125F,m-press,.875F,.875F,.125F,light,Direction.SOUTH);
            vertex(out, pose, .125F,m-.4F,.875F,.875F,rim,light,Direction.SOUTH);
            vertex(out, pose, .875F,m-.4F,.875F,.125F,rim,light,Direction.SOUTH);
            vertex(out, pose, .875F,m-press,.875F,.125F,.125F,light,Direction.SOUTH);
        });
    }
}
