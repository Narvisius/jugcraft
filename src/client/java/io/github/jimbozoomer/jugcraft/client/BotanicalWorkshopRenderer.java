package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.botany.*;
import java.util.*;
import net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.*;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Ingredient offerings remain visible on their original authored workshop meshes. */
final class BotanicalWorkshopRenderer implements BlockEntityRenderer<BotanicalWorkshop,BotanicalWorkshopRenderer.State> {
    private final ItemModelResolver resolver;
    private final BotanicalPylons pylons=new BotanicalPylons();
    private final BotanicalMechanismModels mechanisms=new BotanicalMechanismModels();
    private BotanicalWorkshopRenderer(BlockEntityRendererProvider.Context context){resolver=context.itemModelResolver();}
    static void register(){
        BotanicalLuminizerParticles.register();BotanicalGaiaHeadRenderer.register();BotanicalHaloScreen.register();BotanicalSurveyClient.register();BotanicalBagScreen.register();BotanicalLensTint.register();BotanicalPotatoRenderer.register();BotanicalPylons.register();BotanicalMechanismModels.register();
        BlockEntityRenderers.register(Jugtania.WORKSHOP,BotanicalWorkshopRenderer::new);
        BlockColorRegistry.register(List.of(new BlockTintSource(){public int color(BlockState s){return s.getBlock() instanceof BotanicalBlock b&&b.kind.equals("fabulous_pool")?BotanicalLensTint.Rainbow.color():0xFFFFFFFF;}public int colorInWorld(BlockState s,BlockAndTintGetter l,BlockPos p){return s.getBlock() instanceof BotanicalBlock rainbow&&rainbow.kind.equals("fabulous_pool")?BotanicalLensTint.Rainbow.color():s.getBlock() instanceof BotanicalBlock b&&b.kind.startsWith("apothecary")?0xFF3F76E4:0xFFFFFFFF;}}),Jugtania.BLOCKS.values().stream().filter(b->b instanceof BotanicalBlock).toArray(net.minecraft.world.level.block.Block[]::new));
    }
    static final class State extends BlockEntityRenderState {
        final List<ItemStackRenderState> offerings=new ArrayList<>();final List<Integer> slots=new ArrayList<>();final ItemStackRenderState output=new ItemStackRenderState();float rotation;double height;String kind="";
        final List<net.minecraft.client.renderer.block.dispatch.BlockStateModelPart> shell=new ArrayList<>(),core=new ArrayList<>();final ItemStackRenderState spark=new ItemStackRenderState(),augment=new ItemStackRenderState();
        final ItemStackRenderState potato=new ItemStackRenderState();final boolean[] potatoBlocks=new boolean[6],potatoChildren=new boolean[6];float potatoJump;
        int[] camouflageTints=new int[0];long networkCount;boolean hideCount;String avatarRod="";
        float yaw,pitch,compression,fraction;boolean floating,filled,flip,raining,halloween;float torchAngle;int cocoonAge;int pulse,color;
    }
    @Override public State createRenderState(){return new State();}
    @Override public void extractRenderState(BotanicalWorkshop workshop,State state,float partial,Vec3 camera,ModelFeatureRenderer.CrumblingOverlay overlay){
        BlockEntityRenderState.extractBase(workshop,state,overlay);if(BotanicalLuminizer.TYPES.contains(workshop.kind()))BotanicalLuminizerParticles.trail(workshop);state.offerings.clear();state.slots.clear();state.kind=workshop.kind();state.rotation=((workshop.getLevel()==null?0:workshop.getLevel().getGameTime())%3600+partial)*2;
        var sparkStack=workshop.sparks.installed()?new net.minecraft.world.item.ItemStack(Jugtania.item("spark")):net.minecraft.world.item.ItemStack.EMPTY;
        if(!sparkStack.isEmpty())sparkStack.set(net.minecraft.core.component.DataComponents.CUSTOM_MODEL_DATA,new net.minecraft.world.item.component.CustomModelData(List.of(),List.of(),List.of(),List.of(net.minecraft.world.item.DyeColor.byId(workshop.sparks.channel()).getTextureDiffuseColor())));
        resolver.updateForTopItem(state.spark,sparkStack,ItemDisplayContext.FIXED,workshop.getLevel(),null,18);
        resolver.updateForTopItem(state.augment,workshop.sparks.installed()&&!workshop.sparks.augment().isEmpty()?new net.minecraft.world.item.ItemStack(Jugtania.item("spark_upgrade_"+workshop.sparks.augment())):net.minecraft.world.item.ItemStack.EMPTY,ItemDisplayContext.FIXED,workshop.getLevel(),null,19);
        state.raining=workshop.getLevel()!=null&&workshop.getLevel().isRaining();state.halloween=java.time.LocalDate.now().getMonthValue()==10;state.torchAngle=workshop.devices.angle(partial);state.cocoonAge=workshop.devices.age();
        state.floating=((BotanicalBlock)workshop.getBlockState().getBlock()).floating();
        state.compression=workshop.mechanisms.compression();state.fraction=workshop.mechanisms.fraction();state.filled=!workshop.getItem(0).isEmpty();state.flip=workshop.mechanisms.flipped();state.pulse=workshop.mechanisms.pulse();state.color=workshop.mechanisms.color();
        if(workshop.getBlockState().hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING))state.yaw=switch(workshop.getBlockState().getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING)){case NORTH->180;case SOUTH->0;case EAST->270;case WEST->90;default->0;};
        if(state.kind.equals("tiny_potato")){state.potatoJump=workshop.potato.jump(partial);resolver.updateForTopItem(state.potato,BotanicalPotatoRenderer.display(workshop.potato.name()),ItemDisplayContext.NONE,workshop.getLevel(),null,0);for(int i=0;i<6;i++){state.potatoBlocks[i]=workshop.getItem(i).getItem() instanceof net.minecraft.world.item.BlockItem;state.potatoChildren[i]=workshop.getItem(i).is(Jugtania.item("tiny_potato"));}}
        if(state.kind.equals("pump")){state.filled=workshop.pumpActive;var head=new net.minecraft.world.item.ItemStack(Jugtania.item("pump"));head.set(net.minecraft.core.component.DataComponents.ITEM_MODEL,io.github.jimbozoomer.jugcraft.Jugcraft.id("botanical_pump_head"));resolver.updateForTopItem(state.output,head,ItemDisplayContext.NONE,workshop.getLevel(),null,17);return;}
        if(state.kind.equals("gaia_head")||state.kind.equals("gaia_wall_head")){
            state.yaw=state.kind.equals("gaia_head")?workshop.getBlockState().getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.ROTATION_16)*22.5F:state.yaw;return;
        }
        state.avatarRod=workshop.getItem(0).isEmpty()?"":net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(workshop.getItem(0).getItem()).getPath().replaceFirst("^jugtania_","");
        state.networkCount=workshop.corporea.count();state.hideCount=workshop.corporea.hideCount();
        state.shell.clear();state.core.clear();if(state.kind.equals("fabulous_pool")){net.minecraft.client.Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(workshop.getBlockState()).collectParts(net.minecraft.util.RandomSource.create(0),state.shell);state.color=BotanicalLensTint.Rainbow.color();}
        else if(BotanicalPlatforms.TYPES.contains(state.kind)){
            var camo=workshop.platforms.disguise();state.camouflageTints=new int[0];
            if(camo==null||!camo.is(net.minecraft.world.level.block.Blocks.BARRIER)){
                var shown=camo==null?workshop.getBlockState():camo;var mc=net.minecraft.client.Minecraft.getInstance();
                mc.getModelManager().getBlockStateModelSet().get(shown).collectParts(net.minecraft.util.RandomSource.create(workshop.getBlockPos().asLong()),state.shell);
                int highest=-1;for(var part:state.shell)for(int face=-1;face<6;face++)for(var quad:part.getQuads(face<0?null:net.minecraft.core.Direction.values()[face]))highest=Math.max(highest,quad.materialInfo().tintIndex());
                state.camouflageTints=new int[Math.min(256,highest+1)];
                for(int i=0;i<state.camouflageTints.length;i++)state.camouflageTints[i]=OwnerBlockTints.color(shown,workshop.getLevel(),workshop.getBlockPos(),i);
            }
        }
        else if(state.kind.equals("corporea_crystal_cube")){net.minecraft.client.Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(workshop.getBlockState().setValue(BotanicalBlock.CORE,true)).collectParts(net.minecraft.util.RandomSource.create(0),state.core);}
        else if(state.kind.equals("cocoon")){net.minecraft.client.Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(workshop.getBlockState()).collectParts(net.minecraft.util.RandomSource.create(0),state.shell);}
        else if(state.floating){
            net.minecraft.client.Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(workshop.getBlockState()).collectParts(net.minecraft.util.RandomSource.create(workshop.getBlockPos().asLong()),state.shell);
        }else if(state.kind.contains("spreader")){
            var models=net.minecraft.client.Minecraft.getInstance().getModelManager().getBlockStateModelSet();var random=net.minecraft.util.RandomSource.create(workshop.getBlockPos().asLong());
            models.get(workshop.getBlockState().setValue(BotanicalBlock.CORE,false)).collectParts(random,state.shell);models.get(workshop.getBlockState().setValue(BotanicalBlock.CORE,true)).collectParts(random,state.core);
            var aim=workshop.aim();state.yaw=(float)Math.toDegrees(Math.atan2(-aim.x,-aim.z));state.pitch=(float)Math.toDegrees(Math.atan2(aim.y,Math.sqrt(aim.x*aim.x+aim.z*aim.z)));
        }
        state.height=workshop.kind().equals("incense_plate")?.23:workshop.kind().endsWith("pool")?.55:workshop.kind().equals("terra_plate")?.4:1.15;
        for(int i=0;i<16;i++)if(!state.kind.startsWith("corporea_")&&!workshop.getItem(i).isEmpty()){
            var item=new ItemStackRenderState();resolver.updateForTopItem(item,workshop.getItem(i),state.kind.equals("tiny_potato")?ItemDisplayContext.NONE:state.kind.equals("avatar")?ItemDisplayContext.THIRD_PERSON_RIGHT_HAND:workshop.kind().equals("incense_plate")?ItemDisplayContext.GROUND:ItemDisplayContext.FIXED,workshop.getLevel(),null,i);state.offerings.add(item);state.slots.add(i);
        }
        resolver.updateForTopItem(state.output,state.kind.equals("corporea_crystal_cube")?workshop.corporea.sample():state.kind.startsWith("corporea_")?net.minecraft.world.item.ItemStack.EMPTY:state.kind.equals("animated_torch")?new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.REDSTONE_TORCH):workshop.getItem(16),state.kind.equals("animated_torch")?ItemDisplayContext.GROUND:ItemDisplayContext.FIXED,workshop.getLevel(),null,17);
    }
    @Override public void submit(State state,PoseStack pose,SubmitNodeCollector collector,CameraRenderState camera){
        if(state.kind.equals("gaia_head")||state.kind.equals("gaia_wall_head")){
            pose.pushPose();pose.translate(.5,state.kind.equals("gaia_wall_head")?.25:0,.5);pose.rotateDegrees(Axis.YP,state.yaw);if(state.kind.equals("gaia_wall_head"))pose.translate(0,0,-.25);BotanicalGaiaHeadRenderer.draw(pose,collector,state.lightCoords,0);pose.popPose();return;
        }
        if(state.kind.equals("starfield")){
            collector.submitCustomGeometry(pose,BotanicalStarfield.TYPE,(m,v)->{
                v.addVertex(m,0,.24F,1);v.addVertex(m,1,.24F,1);v.addVertex(m,1,.24F,0);v.addVertex(m,0,.24F,0);
            });return;
        }
        if(state.kind.equals("prism")){if(!state.offerings.isEmpty()){pose.pushPose();pose.translate(.5,.5,.5);pose.rotateDegrees(Axis.YP,state.rotation);pose.scale(.7F,.7F,.7F);state.offerings.getFirst().submit(pose,collector,state.lightCoords,OverlayTexture.NO_OVERLAY,0);pose.popPose();}return;}
        if(state.kind.equals("pump")){
            float t=(state.rotation/2)%20;float stroke=state.filled?Math.max(0,Math.min(8,t<10?t*.8F:(15-t)*1.6F)):0;
            pose.pushPose();pose.translate(.5,0,.5);pose.rotateDegrees(Axis.YP,state.yaw+180);pose.translate(0,.5,stroke/14);state.output.submit(pose,collector,state.lightCoords,OverlayTexture.NO_OVERLAY,0);pose.popPose();return;
        }
        if(state.kind.equals("tiny_potato")){BotanicalPotatoRenderer.render(state.potato,state.offerings,state.slots,state.potatoBlocks,state.potatoChildren,state.yaw,state.potatoJump,pose,collector,state.lightCoords,OverlayTexture.NO_OVERLAY);return;}
        if(state.kind.equals("cacophonium_block"))return;
        if(BotanicalPlatforms.TYPES.contains(state.kind)){if(!state.shell.isEmpty())collector.submitBlockModel(pose,net.minecraft.client.renderer.rendertype.RenderTypes.entityCutout(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS),state.shell,state.camouflageTints,state.lightCoords,OverlayTexture.NO_OVERLAY,0);return;}
        if(BotanicalLuminizer.TYPES.contains(state.kind)){
            pose.pushPose();pose.translate(.5,.3,.5);pose.scale(.75F,.75F,.75F);pose.rotate(camera.orientation);pose.rotateDegrees(Axis.YP,180);pose.translate(0,.25,0);pose.rotateDegrees(Axis.ZP,state.rotation/2);pose.translate(0,-.25,0);
            collector.submitCustomGeometry(pose,net.minecraft.client.renderer.rendertype.RenderTypes.entityTranslucentEmissive(io.github.jimbozoomer.jugcraft.Jugcraft.id("textures/newest/jugtania/block/"+state.kind+".png")),(m,v)->{
                float[][] points={{-.5F,-.25F,.125F,.875F},{.5F,-.25F,.875F,.875F},{.5F,.75F,.875F,.125F},{-.5F,.75F,.125F,.125F}};
                for(var a:points)v.addVertex(m,a[0],a[1],0).setColor(-1).setUv(a[2],a[3]).setOverlay(OverlayTexture.NO_OVERLAY).setLight(net.minecraft.util.LightCoordsUtil.FULL_BRIGHT).setNormal(m,0,0,1);
            });pose.popPose();return;
        }
        if(state.kind.equals("avatar")){
            mechanisms.avatar(pose,collector,state.lightCoords,OverlayTexture.NO_OVERLAY,0,state.yaw,state.rotation/2,state.avatarRod);
            if(!state.offerings.isEmpty()){pose.pushPose();pose.translate(.5,1.6,.5);pose.scale(1,-1,-1);pose.rotateDegrees(Axis.YP,state.yaw);pose.scale(.6F,.6F,.6F);pose.translate(-.5,2,-.25);pose.rotateDegrees(Axis.XP,-70);state.offerings.getFirst().submit(pose,collector,state.lightCoords,OverlayTexture.NO_OVERLAY,0);pose.popPose();}return;
        }
        if(state.kind.equals("corporea_index")){mechanisms.index(pose,collector,state.lightCoords,OverlayTexture.NO_OVERLAY,0,state.rotation/2);return;}
        if(state.kind.equals("corporea_crystal_cube")){
            pose.pushPose();pose.translate(0,.5+(Math.sin(state.rotation/20)+1)/70,0);
            pose.pushPose();pose.translate(.5,.5,.5);pose.rotateDegrees(Axis.YP,state.rotation);pose.scale(.3F,.3F,.3F);state.output.submit(pose,collector,state.lightCoords,OverlayTexture.NO_OVERLAY,0);pose.popPose();
            collector.submitBlockModel(pose,net.minecraft.client.renderer.rendertype.RenderTypes.entityTranslucent(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS),state.core,new int[]{-1},state.lightCoords,OverlayTexture.NO_OVERLAY,0);
            if(!state.hideCount&&!state.output.isEmpty()){
                var font=net.minecraft.client.Minecraft.getInstance().font;var text=net.minecraft.network.chat.Component.literal(Long.toString(state.networkCount)).getVisualOrderText();
                pose.translate(.5,.5,.5);
                for(int side=0;side<4;side++){pose.pushPose();pose.rotateDegrees(Axis.YP,side*90);pose.translate(0,0,.258);pose.scale(.012F,-.012F,.012F);collector.submitText(pose,-font.width(text)/2F,0,text,false,net.minecraft.client.gui.Font.DisplayMode.POLYGON_OFFSET,state.lightCoords,0xFFFFFFFF,0,0);pose.popPose();}
            }pose.popPose();return;
        }
        if(state.kind.startsWith("corporea_"))return;
        if(state.kind.equals("open_crate")||state.kind.equals("crafty_crate"))return;
        if(state.kind.equals("teru_teru_bozu")){mechanisms.doll(pose,collector,state.lightCoords,OverlayTexture.NO_OVERLAY,0,state.rotation/2,state.raining,state.halloween);return;}
        if(state.kind.equals("animated_torch")){float ticks=state.rotation/2;pose.pushPose();pose.translate(.5+Math.cos(ticks*.05)*.025,.1+(Math.sin(ticks*.04)+1)*.05,.5+Math.sin(ticks*.05)*.025);pose.scale(2,2,2);pose.rotateDegrees(Axis.XP,90);pose.rotateDegrees(Axis.ZP,state.torchAngle);state.output.submit(pose,collector,state.lightCoords,OverlayTexture.NO_OVERLAY,0);pose.popPose();return;}
        if(state.kind.equals("cocoon")){pose.pushPose();pose.translate(.5,0,.5);float scale=1+.02F*(float)Math.sin(state.rotation*.15)*Math.min(1,state.cocoonAge/1200F);pose.scale(scale,1/scale,scale);pose.translate(-.5,0,-.5);collector.submitBlockModel(pose,net.minecraft.client.renderer.rendertype.RenderTypes.entityCutout(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS),state.shell,state.camouflageTints,state.lightCoords,OverlayTexture.NO_OVERLAY,0);pose.popPose();return;}
        if(!state.spark.isEmpty()){
            pose.pushPose();pose.translate(.5,1.25+Math.sin(state.rotation/25)*.05,.5);pose.rotateDegrees(Axis.YP,state.rotation);pose.scale(.65F,.65F,.65F);state.spark.submit(pose,collector,net.minecraft.util.LightCoordsUtil.FULL_BRIGHT,OverlayTexture.NO_OVERLAY,0);pose.popPose();
            if(!state.augment.isEmpty()){pose.pushPose();pose.translate(.5,1.8,.5);pose.rotateDegrees(Axis.YP,-state.rotation);pose.scale(.3F,.3F,.3F);state.augment.submit(pose,collector,net.minecraft.util.LightCoordsUtil.FULL_BRIGHT,OverlayTexture.NO_OVERLAY,0);pose.popPose();}
        }
        if(state.kind.equals("incense_plate")){
            if(!state.offerings.isEmpty()){pose.pushPose();pose.translate(.5,.15,.5);pose.rotateDegrees(Axis.YP,(state.yaw+180)%360);pose.translate(-.11,0,0);pose.scale(.6F,.6F,.6F);state.offerings.get(0).submit(pose,collector,state.lightCoords,OverlayTexture.NO_OVERLAY,0);pose.popPose();}return;
        }
        if(state.kind.equals("brewery")){
            float time=state.rotation/2;mechanisms.brewery(pose,collector,state.lightCoords,OverlayTexture.NO_OVERLAY,0,time);
            for(int i=0;i<state.offerings.size();i++){
                int slot=state.slots.get(i);pose.pushPose();double wobble=Math.sin(time/40)*.1+.05;
                if(slot==0){pose.translate(.5,1.05-wobble,.5);pose.rotateDegrees(Axis.YP,time*25/16);}
                else{double angle=time/16+(slot-1)*Math.PI/3;pose.translate(.5+Math.cos(angle)*.4375,.4375-wobble-(Math.sin(time/20+(slot-1)*40)*.2-.2),.5+Math.sin(angle)*.4375);pose.rotateDegrees(Axis.YP,-(float)Math.toDegrees(angle));pose.rotateDegrees(Axis.XP,90);}
                pose.scale(.25F,.25F,.25F);state.offerings.get(i).submit(pose,collector,state.lightCoords,OverlayTexture.NO_OVERLAY,0);pose.popPose();
            }
            if(!state.output.isEmpty()){pose.pushPose();pose.translate(.5,1.2,.5);pose.scale(.4F,.4F,.4F);state.output.submit(pose,collector,state.lightCoords,OverlayTexture.NO_OVERLAY,0);pose.popPose();}return;
        }
        if(state.kind.equals("bellows")||state.kind.equals("hourglass")){mechanisms.draw(state.kind,state.compression,state.fraction,state.filled,state.flip,state.pulse,state.color,state.rotation/2,state.yaw,pose,collector,state.lightCoords,OverlayTexture.NO_OVERLAY,0);return;}
        if(state.kind.endsWith("_pylon")){pylons.draw(state.kind.replace("_pylon", ""),state.rotation/2,false,pose,collector,state.lightCoords,OverlayTexture.NO_OVERLAY,0);return;}
        if(state.floating){
            pose.pushPose();pose.translate(.5,Math.sin(state.rotation*.025)*.1,.5);pose.rotateDegrees(Axis.YP,-state.rotation*.25F);pose.rotateDegrees(Axis.XP,4*(float)Math.sin(state.rotation*.02));pose.translate(-.5,0,-.5);
            collector.submitBlockModel(pose,net.minecraft.client.renderer.rendertype.RenderTypes.entityCutout(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS),state.shell,state.camouflageTints,state.lightCoords,OverlayTexture.NO_OVERLAY,0);pose.popPose();return;
        }
        if(state.kind.equals("fabulous_pool")){
            collector.submitBlockModel(pose,net.minecraft.client.renderer.rendertype.RenderTypes.entityCutout(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS),state.shell,new int[]{state.color},state.lightCoords,OverlayTexture.NO_OVERLAY,0);return;
        }
        if(!state.shell.isEmpty()){
            var type=net.minecraft.client.renderer.rendertype.RenderTypes.entityCutout(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS);
            pose.pushPose();pose.translate(.5,.5,.5);pose.rotateDegrees(Axis.YP,state.yaw);pose.rotateDegrees(Axis.XP,state.pitch);pose.translate(-.5,-.5,-.5);
            collector.submitBlockModel(pose,type,state.shell,state.camouflageTints,state.lightCoords,OverlayTexture.NO_OVERLAY,0);
            if(state.kind.contains("spreader")&&!state.offerings.isEmpty()){pose.pushPose();pose.translate(.5,.5,-.002);pose.scale(.75F,.75F,.75F);state.offerings.getFirst().submit(pose,collector,state.lightCoords,OverlayTexture.NO_OVERLAY,0);pose.popPose();}
            pose.translate(.5,.5,.5);pose.rotateDegrees(Axis.YP,state.rotation/2);pose.translate(-.5,-.5+Math.sin(state.rotation/40)*.05,-.5);
            collector.submitBlockModel(pose,type,state.core,new int[]{-1},state.lightCoords,OverlayTexture.NO_OVERLAY,0);pose.popPose();return;
        }
        for(int i=0;i<state.offerings.size();i++){
            double angle=(state.rotation+i*360.0/state.offerings.size())*Math.PI/180;
            pose.pushPose();pose.translate(.5+Math.sin(angle)*.26,state.height,.5+Math.cos(angle)*.26);pose.rotateDegrees(Axis.YP,state.rotation);pose.scale(.25F,.25F,.25F);state.offerings.get(i).submit(pose,collector,state.lightCoords,OverlayTexture.NO_OVERLAY,0);pose.popPose();
        }
        if(!state.output.isEmpty()){pose.pushPose();pose.translate(.5,state.height+.35,.5);pose.rotateDegrees(Axis.YP,state.rotation);pose.scale(.5F,.5F,.5F);state.output.submit(pose,collector,state.lightCoords,OverlayTexture.NO_OVERLAY,0);pose.popPose();}
    }
}
