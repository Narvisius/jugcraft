package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.menagerie.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.*;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Original attachment meshes surround real native block/item models, preserving resource-pack skins. */
final class OwnerAmendmentRenderer implements BlockEntityRenderer<OwnerAmendmentData,OwnerAmendmentRenderer.State> {
    private final ItemModelResolver items;
    private final SkullBlockRenderer skulls;
    private final net.minecraft.client.model.object.banner.BannerModel bannerBar;
    private final net.minecraft.client.model.object.banner.BannerFlagModel bannerCloth;
    private final net.minecraft.client.resources.model.sprite.SpriteGetter sprites;
    private OwnerAmendmentRenderer(BlockEntityRendererProvider.Context context){items=context.itemModelResolver();skulls=new SkullBlockRenderer(context);sprites=context.sprites();
        var bar=context.entityModelSet().bakeLayer(net.minecraft.client.model.geom.ModelLayers.STANDING_BANNER);bar.getChild("pole").visible=false;bar.getChild("bar").y=12;bar.getChild("bar").setInitialPose(bar.getChild("bar").storePose());bannerBar=new net.minecraft.client.model.object.banner.BannerModel(bar);
        var cloth=context.entityModelSet().bakeLayer(net.minecraft.client.model.geom.ModelLayers.STANDING_BANNER_FLAG);cloth.getChild("flag").y=-32;cloth.getChild("flag").setInitialPose(cloth.getChild("flag").storePose());bannerCloth=new net.minecraft.client.model.object.banner.BannerFlagModel(cloth);
    }
    static void register(){net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry.register(List.of(new net.minecraft.client.color.block.BlockTintSource(){
        public int color(BlockState state){return 0xFF208030;}
        public int colorInWorld(BlockState state,net.minecraft.client.renderer.block.BlockAndTintGetter level,net.minecraft.core.BlockPos pos){return OwnerBlockTints.color(Blocks.LILY_PAD.defaultBlockState(),level,pos,0);}
    }),OwnerAmendments.BLOCKS.get("water_lily_pad"));OwnerAmendmentParticle.register();net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(OwnerAmendmentCharge.TYPE,OwnerAmendmentChargeRenderer::new);net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry.register(List.of(new net.minecraft.client.color.block.BlockTintSource(){
        public int color(BlockState state){return 0xFFFFFFFF;}
        public int colorInWorld(BlockState state,net.minecraft.client.renderer.block.BlockAndTintGetter level,net.minecraft.core.BlockPos pos){return level.getBlockEntity(pos) instanceof OwnerAmendmentData data?0xFF000000|data.liquidColor:0xFFFFFFFF;}
    }),OwnerAmendments.BLOCKS.get("dye_cauldron"),OwnerAmendments.BLOCKS.get("liquid_cauldron"));BlockEntityRenderers.register(OwnerAmendments.DATA,OwnerAmendmentRenderer::new);}
    static final class State extends BlockEntityRenderState {
        final List<BlockStateModelPart> parts=new ArrayList<>();final ItemStackRenderState item=new ItemStackRenderState();
        final List<net.minecraft.client.renderer.blockentity.state.SkullBlockRenderState> heads=new ArrayList<>();
        net.minecraft.world.level.block.entity.BannerPatternLayers bannerPatterns;net.minecraft.world.item.DyeColor bannerColor;float phase,rotation,sway;
        int[] tint=new int[0];String name="",attachment="";Direction facing=Direction.NORTH;
    }
    @Override public State createRenderState(){return new State();}
    @Override public boolean shouldRenderOffScreen(){return true;}
    @Override public int getViewDistance(){return 64;}
    @Override public void extractRenderState(OwnerAmendmentData data,State state,float partial,Vec3 camera,ModelFeatureRenderer.CrumblingOverlay overlay){
        BlockEntityRenderState.extractBase(data,state,overlay);state.name=data.getBlockState().getBlock() instanceof OwnerAmendmentBlock b?b.name:"carpet";state.facing=HouseholdBlock.facing(data.getBlockState());state.attachment=HouseholdBlock.value(data.getBlockState(),"attachment");
        state.parts.clear();state.heads.clear();state.bannerPatterns=null;BlockState base=data.actualBase();
        if(state.name.startsWith("skull")){
            var stack=data.displayed;for(int i=0;i<2;i++){
                if(i==1)stack=data.secondary;if(stack.isEmpty()||!(stack.getItem() instanceof net.minecraft.world.item.BlockItem head)||!(head.getBlock() instanceof AbstractSkullBlock))continue;
                var shape=i==0?base:head.getBlock().defaultBlockState();shape=OwnerAmendments.copy(data.getBlockState(),shape);
                var visual=new net.minecraft.world.level.block.entity.SkullBlockEntity(data.getBlockPos(),shape);visual.setLevel(data.getLevel());visual.applyComponentsFromItemStack(stack);
                var part=skulls.createRenderState();skulls.extractRenderState(visual,part,partial,camera,overlay);state.heads.add(part);
            }
            base=data.decoration;if(base.getBlock() instanceof CandleBlock)base=base.setValue(CandleBlock.LIT,HouseholdBlock.flag(data.getBlockState(),"lit")&&!HouseholdBlock.flag(data.getBlockState(),"waterlogged"));
        }
        if(state.name.startsWith("ceiling_banner")&&data.displayed.getItem() instanceof net.minecraft.world.item.BannerItem banner){
            state.bannerPatterns=OwnerSupplementFlags.bounded(data.displayed.getOrDefault(net.minecraft.core.component.DataComponents.BANNER_PATTERNS,net.minecraft.world.level.block.entity.BannerPatternLayers.EMPTY));state.bannerColor=banner.getColor();
            state.rotation=Float.parseFloat(HouseholdBlock.value(data.getBlockState(),"rotation"))*22.5F;state.phase=((data.getBlockPos().asLong()+(data.getLevel()==null?0:data.getLevel().getGameTime()))%100+partial)/100F;base=Blocks.AIR.defaultBlockState();
        }
        if(state.name.equals("tool_hook")||state.name.endsWith("cauldron"))base=Blocks.AIR.defaultBlockState();
        if(state.name.contains("lantern")){base=base.setValue(LanternBlock.HANGING,true);var side=state.facing.getClockWise();state.sway=OwnerHangingMotion.angle(data,partial,new Vec3(side.getStepX(),0,side.getStepZ()),true);}
        if(state.name.equals("hanging_flower_pot")&&!data.decoration.isAir()){
            for(var block:net.minecraft.core.registries.BuiltInRegistries.BLOCK)if(block instanceof FlowerPotBlock pot&&pot.getPotted()==data.decoration.getBlock()){base=block.defaultBlockState();break;}
        }
        if(!base.isAir()){
            var mc=Minecraft.getInstance();mc.getModelManager().getBlockStateModelSet().get(base).collectParts(net.minecraft.util.RandomSource.create(data.getBlockPos().asLong()),state.parts);
            state.tint=new int[]{OwnerBlockTints.color(base,data.getLevel(),data.getBlockPos(),0)};
        }
        items.updateForTopItem(state.item,state.name.equals("tool_hook")?data.displayed:ItemStack.EMPTY,ItemDisplayContext.FIXED,data.getLevel(),null,0);
    }
    @Override public void submit(State state,PoseStack pose,SubmitNodeCollector collector,CameraRenderState camera){
        for(int i=0;i<state.heads.size();i++){pose.pushPose();pose.translate(0,i*.5,0);skulls.submit(state.heads.get(i),pose,collector,camera);pose.popPose();}
        if(state.bannerPatterns!=null){
            pose.pushPose();pose.translate(.5,-1/3.0,.5);pose.rotateDegrees(Axis.YP,state.rotation);pose.scale(-2/3F,-2/3F,2/3F);
            collector.submitModel(bannerBar,net.minecraft.util.Unit.INSTANCE,pose,state.lightCoords,OverlayTexture.NO_OVERLAY,-1,Sheets.BANNER_BASE,sprites,0);
            collector.submitModel(bannerCloth,state.phase,pose,state.lightCoords,OverlayTexture.NO_OVERLAY,-1,Sheets.BANNER_BASE,sprites,0);
            BannerRenderer.submitPatterns(sprites,pose,collector,state.lightCoords,OverlayTexture.NO_OVERLAY,bannerCloth,state.phase,true,state.bannerColor,state.bannerPatterns);pose.popPose();
        }
        if(!state.parts.isEmpty()){
            pose.pushPose();
            if(state.name.startsWith("skull_candle"))pose.translate(0,.5,0);
            if(state.name.contains("lantern")){
                pose.translate(.5,.875,.5);pose.rotateDegrees(Axis.YP,180-state.facing.toYRot());pose.rotateDegrees(Axis.ZP,state.sway);
                double offset=switch(state.attachment){case "beam"->1/16.0;case "wall"->2/16.0;case "palisade"->3/16.0;case "post"->4/16.0;case "stick"->5/16.0;default->0;};
                pose.translate(-.5,-.75-offset,-.375);
            }
            collector.submitBlockModel(pose,RenderTypes.entityCutout(TextureAtlas.LOCATION_BLOCKS),state.parts,state.tint,state.lightCoords,OverlayTexture.NO_OVERLAY,0);pose.popPose();
        }
        if(!state.item.isEmpty()){
            pose.pushPose();pose.translate(.5,.5,.5);pose.rotateDegrees(Axis.YP,180-state.facing.toYRot());pose.translate(0,.12,.23);pose.rotateDegrees(Axis.ZP,180);pose.scale(.85F,.85F,.85F);
            state.item.submit(pose,collector,state.lightCoords,OverlayTexture.NO_OVERLAY,0);pose.popPose();
        }
    }
}
