package io.github.jimbozoomer.jugcraft.client.jimtafacts;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.client.ConcordanceClientOptions;
import io.github.jimbozoomer.jugcraft.jimtafacts.computers.ComputerBlock;
import io.github.jimbozoomer.jugcraft.jimtafacts.computers.ComputerNode;
import io.github.jimbozoomer.jugcraft.jimtafacts.computers.ComputerCanvas;
import io.github.jimbozoomer.jugcraft.jimtafacts.computers.WorkshopComputers;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.item.*;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;

/** Native text over the original supplied monitor face. Only public monitor output is synced. */
public final class ComputerMonitorRenderer implements BlockEntityRenderer<ComputerNode, ComputerMonitorRenderer.State> {
    private final Font font;
    private final ItemModelResolver models;
    private static final net.minecraft.resources.Identifier CELL_FONT=Jugcraft.id("textures/jimtafacts/computers/gui/term_font.png");
    public ComputerMonitorRenderer(BlockEntityRendererProvider.Context context) { font = context.font(); models = context.itemModelResolver(); }
    public static final class State extends BlockEntityRenderState {
        Direction facing = Direction.NORTH;
        final List<FormattedCharSequence> lines = new ArrayList<>();
        final List<BlockStateModelPart> body = new ArrayList<>();
        final ItemStackRenderState[] upgrades = {new ItemStackRenderState(), new ItemStackRenderState()};
        final boolean[] tool = new boolean[2];
        final float[] swing = new float[2];
        int width = 1;
        int colour=0xFFE5F4DF;
        String cells="",front="",back="";
        int height=1;
        float yaw;
        Vec3 offset = Vec3.ZERO;
    }
    @Override public State createRenderState() { return new State(); }
    @Override public void extractRenderState(ComputerNode node, State state, float delta, Vec3 camera,
            ModelFeatureRenderer.CrumblingOverlay crumbling) {
        BlockEntityRenderState.extractBase(node, state, crumbling); state.lines.clear(); state.body.clear();state.cells="";
        for (var upgrade : state.upgrades) upgrade.clear();
        state.facing = node.getBlockState().getValue(ComputerBlock.FACING);
        if (node.turtle()) { turtle(node, state, delta); return; }
        if (!node.kind().startsWith("monitor") || !node.monitorOrigin()) return;
        state.width = node.monitorWidth(); state.colour=0xFF000000|node.displayColour(); int height = node.monitorHeight();
        state.height=height;
        if(node.canvas().active()){state.cells=node.canvas().text();state.front=node.canvas().foreground();state.back=node.canvas().background();return;}
        for (String line : node.output().split("\\R", -1)) {
            for (FormattedCharSequence wrapped : font.split(Component.literal(line), state.width * 100 - 18)) {
                if (state.lines.size() >= height * 10 - 2) break;
                state.lines.add(wrapped);
            }
            if (state.lines.size() >= height * 10 - 2) break;
        }
    }
    private void turtle(ComputerNode node, State state, float delta) {
        Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(node.getBlockState().setValue(ComputerBlock.FACING, Direction.NORTH))
                .collectParts(RandomSource.create(0), state.body);
        // The supplied TurtleBrain uses eight ticks, linear movement/turns and a 45-degree sine tool swing.
        float fraction = ConcordanceClientOptions.reducedMotion() ? 1 : node.animationFraction(delta);
        int animation = node.animation(); state.offset = Vec3.ZERO;
        if (animation >= 1 && animation <= 6) {
            Direction motion = Direction.values()[animation - 1];
            state.offset = new Vec3(motion.getStepX() * (fraction - 1), motion.getStepY() * (fraction - 1), motion.getStepZ() * (fraction - 1));
        }
        state.yaw = state.facing.toYRot() + (animation == 7 ? 90 * (1 - fraction) : animation == 8 ? -90 * (1 - fraction) : 0);
        for (int side = 0; side < 2; side++) {
            ItemStack upgrade = node.upgrade(side).copy(); state.tool[side] = upgrade.isDamageableItem();
            state.swing[side] = animation == side + 9 ? 45 * (float) Math.sin(fraction * Math.PI) : 0;
            if (upgrade.isEmpty()) continue;
            if (!state.tool[side]) {
                String part = upgrade.is(Items.CRAFTING_TABLE) ? "crafting_table"
                        : upgrade.is(WorkshopComputers.BLOCKS.get("speaker").asItem()) ? "speaker"
                        : (upgrade.is(WorkshopComputers.BLOCKS.get("wireless_modem_advanced").asItem()) ? "modem_advanced" : "modem_normal")
                            + (node.modemActive() ? "_on" : "_off");
                upgrade.set(DataComponents.ITEM_MODEL, Jugcraft.id("jimtafacts/turtle_" + part + (side == 0 ? "_left" : "_right")));
            }
            models.updateForTopItem(state.upgrades[side], upgrade, ItemDisplayContext.NONE, node.getLevel(), null, side);
        }
    }
    @Override public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.body.isEmpty()) { submitTurtle(state, pose, collector); return; }
        if (state.lines.isEmpty()&&state.cells.isEmpty()) return;
        pose.pushPose(); pose.translate(.5, .5, .5);
        pose.mulPose(new org.joml.Matrix4f().rotationY((float) Math.toRadians(-state.facing.toYRot())));
        pose.translate(-.42, .36, .501);
        if(!state.cells.isEmpty()){
            float scale=Math.min((state.width-.16F)/(ComputerCanvas.WIDTH*6),(state.height-.28F)/(ComputerCanvas.HEIGHT*9));
            pose.scale(scale,-scale,scale);
            collector.submitCustomGeometry(pose,RenderTypes.text(CELL_FONT),(matrix,vertices)->{
                // Supplied term_font.png: 16 columns, 8x11 atlas pitch, 6x9 visible glyphs.
                // The source's white patch at 250..252 supplies solid backgrounds without stretched glyph gaps.
                for(int i=0;i<state.cells.length();i++){
                    int x=i%ComputerCanvas.WIDTH*6,y=i/ComputerCanvas.WIDTH*9;
                    int fg=0xFF000000|DyeColor.byId(Character.digit(state.front.charAt(i),16)).getTextureDiffuseColor();
                    int bg=0xFF000000|DyeColor.byId(Character.digit(state.back.charAt(i),16)).getTextureDiffuseColor();
                    cellQuad(matrix,vertices,x,y,0,bg,250/256F,250/256F,252/256F,252/256F);
                    int c=state.cells.charAt(i);if(c==32)continue;int u=1+c%16*8,v=1+c/16*11;
                    cellQuad(matrix,vertices,x,y,.01F,fg,u/256F,v/256F,(u+6)/256F,(v+9)/256F);
                }
            });pose.popPose();return;
        }
        pose.scale(.01F, -.01F, .01F);
        for (int i = 0; i < state.lines.size(); i++) collector.submitText(pose, 0, i * 10, state.lines.get(i), false,
                Font.DisplayMode.POLYGON_OFFSET, LightCoordsUtil.FULL_BRIGHT, state.colour, 0, 0);
        pose.popPose();
    }
    private static void cellQuad(PoseStack.Pose matrix,com.mojang.blaze3d.vertex.VertexConsumer out,float x,float y,float z,int color,float u0,float v0,float u1,float v1){
        out.addVertex(matrix,x,y,z).setColor(color).setUv(u0,v0).setLight(LightCoordsUtil.FULL_BRIGHT);
        out.addVertex(matrix,x,y+9,z).setColor(color).setUv(u0,v1).setLight(LightCoordsUtil.FULL_BRIGHT);
        out.addVertex(matrix,x+6,y+9,z).setColor(color).setUv(u1,v1).setLight(LightCoordsUtil.FULL_BRIGHT);
        out.addVertex(matrix,x+6,y,z).setColor(color).setUv(u1,v0).setLight(LightCoordsUtil.FULL_BRIGHT);
    }
    private void submitTurtle(State state, PoseStack pose, SubmitNodeCollector collector) {
        pose.pushPose(); pose.translate(state.offset); pose.translate(.5, .5, .5);
        pose.rotateDegrees(Axis.YP, 180 - state.yaw); pose.translate(-.5, -.5, -.5);
        collector.submitBlockModel(pose, RenderTypes.entityCutout(TextureAtlas.LOCATION_BLOCKS), state.body,
                new int[0], state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        for (int side = 0; side < 2; side++) {
            if (state.upgrades[side].isEmpty()) continue;
            pose.pushPose(); pose.translate(0, .5, .5); pose.rotateDegrees(Axis.XN, state.swing[side]); pose.translate(0, -.5, -.5);
            pose.translate(.5, .5, .5);
            if (state.tool[side]) {
                // Original ItemUpgradeModel: Y -90, Z +90 and +/-0.4065 side offset.
                pose.rotateDegrees(Axis.YN, 90); pose.rotateDegrees(Axis.ZP, 90); pose.translate(0, 0, side == 1 ? -.4065 : .4065);
            }
            state.upgrades[side].submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0); pose.popPose();
        }
        pose.popPose();
    }
}
