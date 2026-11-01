package io.github.jimbozoomer.jugcraft.client;
import java.lang.Math;

import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.serialization.*;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.building.OwnerBuildingCollection;
import io.github.jimbozoomer.jugcraft.menagerie.*;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.*;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.phys.Vec3;
import org.joml.*;

/** Recovered original pivots/UVs; clocks follow the server day, not real-world time. */
final class OwnerHomeRenderer implements BlockEntityRenderer<OwnerHome.Data,OwnerHomeRenderer.State> {
    private final Map<String,ModelPart> models=new HashMap<>();
    static void register(){
        BlockEntityRenderers.register(OwnerHome.DATA,ctx->new OwnerHomeRenderer());
        SpecialModelRenderers.ID_MAPPER.put(Jugcraft.id("owner_home"),Unbaked.CODEC);
        MenuScreens.register(OwnerHomeMenu.COUNTER,OwnerHomeScreen::new);MenuScreens.register(OwnerHomeMenu.DRAWER,OwnerHomeScreen::new);
    }
    private ModelPart model(String name){return models.computeIfAbsent(name,key->{
        try(var reader=Minecraft.getInstance().getResourceManager().getResourceOrThrow(Jugcraft.id("newest_home_models/"+key+".json")).openAsReader()){
            return RecoveredWildlifeModel.bake(JsonParser.parseReader(reader).getAsJsonObject());
        }catch(java.io.IOException e){throw new IllegalStateException("Missing supplied Home model "+key,e);}
    });}
    public static final class State extends BlockEntityRenderState {
        String name="",rig="",function="",cushion="";boolean visible=true,triggered,tucked;
        int color=-1,cushionColor=-1;float rotation,yaw,pitch,time;
    }
    @Override public State createRenderState(){return new State();}
    @Override public void extractRenderState(OwnerHome.Data data,State s,float partial,Vec3 camera,ModelFeatureRenderer.CrumblingOverlay overlay){
        BlockEntityRenderState.extractBase(data,s,overlay);var state=data.getBlockState();var block=(OwnerHomeBlock)state.getBlock();
        s.name=block.name;s.rig=block.rig;s.function=block.function;s.color=data.color;s.triggered=HouseholdBlock.flag(state,"triggered");s.tucked=HouseholdBlock.flag(state,"tucked");
        s.visible=!s.function.equals("grandfather_clock")||OwnerHomeBlock.stateValue(state,"part").equals("top");
        String rotation=OwnerHomeBlock.stateValue(state,"rotation");s.rotation=rotation.isEmpty()?180-HouseholdBlock.facing(state).toYRot():Integer.parseInt(rotation)*22.5F+180;
        if(s.name.equals("quartz_grandfather_clock"))s.rotation-=180;
        s.time=data.getLevel()==null?0:data.getLevel().getDefaultClockTime()+partial;s.yaw=data.yaw;s.pitch=data.pitch;
        s.cushion=data.cushion.isEmpty()?"":BuiltInRegistries.ITEM.getKey(data.cushion.getItem()).getPath().replaceFirst("^homeworks_","");
        s.cushionColor=DyedItemColor.getOrDefault(data.cushion,-1);
    }
    @Override public void submit(State s,PoseStack pose,SubmitNodeCollector out,CameraRenderState camera){draw(s,pose,out,s.lightCoords,OverlayTexture.NO_OVERLAY,0);}
    private static Identifier texture(String path){return Jugcraft.id("textures/newest/homeworks/"+path+".png");}
    private void part(ModelPart p,String tex,PoseStack pose,SubmitNodeCollector out,int light,int overlay,int color,int outline){out.order(0).submitModelPart(p,pose,RenderTypes.entityCutout(texture(tex)),light,overlay,null,color,outline);}
    private void draw(State s,PoseStack pose,SubmitNodeCollector out,int light,int overlay,int outline){
        if(!s.visible)return;pose.pushPose();
        float height=s.function.equals("grandfather_clock")?-.5F:s.function.equals("telescope")?2.5F:1.5F;
        pose.translate(.5,height,.5);pose.rotateDegrees(Axis.XP,180);pose.rotateDegrees(Axis.YP,s.rotation);
        if(s.tucked&&s.function.equals("chair"))pose.translate(0,0,-.625);
        if(!s.rig.isEmpty()){
            ModelPart model=model(s.rig);model.getAllParts().forEach(p->{p.resetPose();p.visible=true;});
            String design=s.name.equals("quartz_chair")?"princess_chair":s.name;
            String tex=s.function.equals("telescope")?"entity/telescope_head":"block/"+s.function+"/"+design+(s.name.startsWith("ominous_")?(s.triggered?"_active":"_inactive"):s.name.equals("quartz_wall_clock")?"_hands":"");
            if(s.function.endsWith("clock")){
                float hour=(s.time+6000)/12000F*(float)(Math.PI*2),minute=s.time/1000F*(float)(Math.PI*2);
                model.getChild("hour_hand").zRot=hour;model.getChild(s.function.equals("wall_clock")?"minute_hand":"min_hand").zRot=minute;
                if(s.function.equals("grandfather_clock"))model.getChild("pendulum").zRot=(float)Math.sin(s.time*.1)*.08F;
                if(s.name.startsWith("quartz_")){
                    if(s.function.equals("grandfather_clock")){pose.translate(0,-.1875,-.078125);part(model("QuartzGrandfatherClockDialModel"),"block/grandfather_clock/quartz_grandfather_clock_dial",pose,out,light,overlay,-1,outline);}
                    model.getChild(s.function.equals("wall_clock")?"bb_main":"main").visible=false;
                    if(s.function.equals("grandfather_clock"))model.getChild("pendulum").visible=false;
                }
            }
            if(s.function.equals("telescope")){model.getChild("holder").yRot=(float)Math.toRadians(s.yaw-s.rotation+180);model.getChild("holder").getChild("head").xRot=(float)Math.toRadians(-s.pitch);}
            if(s.function.equals("sofa")){part(model.getChild("frame"),tex,pose,out,light,overlay,-1,outline);part(model.getChild("dyeable"),tex,pose,out,light,overlay,s.color,outline);}
            else part(model,tex,pose,out,light,overlay,-1,outline);
        }
        if(!s.cushion.isEmpty()){
            String rig=s.function.equals("sofa")?"SofaCushionModel":s.function.equals("couch")?"CouchCushionModel":"CushionModel";
            part(model(rig),"block/cushion/"+s.cushion,pose,out,light,overlay,s.cushionColor,outline);
        }pose.popPose();
    }
    public record Unbaked(String design) implements SpecialModelRenderer.Unbaked<State>{
        static final MapCodec<Unbaked> CODEC=Codec.STRING.fieldOf("design").xmap(Unbaked::new,Unbaked::design);
        @Override public MapCodec<Unbaked> type(){return CODEC;}
        @Override public SpecialModelRenderer<State> bake(SpecialModelRenderer.BakingContext context){return new Held(design);}
    }
    private static final class Held implements SpecialModelRenderer<State>{
        private final String design;private final OwnerHomeRenderer renderer=new OwnerHomeRenderer();
        Held(String design){this.design=design;}
        @Override public State extractArgument(ItemStack stack){var s=new State();var block=(OwnerHomeBlock)OwnerBuildingCollection.BLOCKS.get("homeworks_"+design);s.name=design;s.rig=block.rig;s.function=block.function;s.color=DyedItemColor.getOrDefault(stack,-1);return s;}
        @Override public void submit(State s,PoseStack pose,SubmitNodeCollector out,int light,int overlay,boolean foil,int outline){renderer.draw(s,pose,out,light,overlay,outline);}
        @Override public void getExtents(Consumer<Vector3fc> out){out.accept(new Vector3f(0,0,0));out.accept(new Vector3f(1,1.5F,1));}
    }
}
