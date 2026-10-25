package io.github.jimbozoomer.jugcraft.client.jimtafacts;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.*;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks.*;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/** Original source mage meshes, skins, casting clips and native procedural walking/head movement. */
public final class JimtaWizardClient {
    private static final DataTicket<Float> WALK=DataTicket.create("jimta_mage_walk",Float.class),SPEED=DataTicket.create("jimta_mage_speed",Float.class),TIME=DataTicket.create("jimta_mage_time",Float.class),PITCH=DataTicket.create("jimta_mage_pitch",Float.class);
    private static final DataTicket<Integer> PHASE=DataTicket.create("jimta_mage_phase",Integer.class),CAST=DataTicket.create("jimta_mage_cast",Integer.class);
    public static void register(){JimtaWizard.TYPES.forEach((kind,type)->EntityRendererRegistry.register(type,c->new GeoEntityRenderer<JimtaWizard,EntityRenderState>(c,new GeoModel<>(){
        @Override public Identifier getModelResource(GeoRenderState s){return Jugcraft.id("entity/jimtafacts/mage_"+kind);}
        @Override public Identifier getTextureResource(GeoRenderState s){return kind.equals("cursed_armor_stand")?Identifier.withDefaultNamespace("textures/entity/armorstand/wood.png"):tex(switch(kind){case "dead_king"->"dead_king/dead_king"+(s.getOrDefaultGeckolibData(PHASE,0)==2?"_enraged":"");case "fire_boss"->"fire_boss/tyros"+(s.getOrDefaultGeckolibData(PHASE,0)==2?"_soul_mode":"");case "priest"->"priest/priest";case "dead_king_corpse"->"dead_king/dead_king_resting";default->kind;});}
        @Override public Identifier getAnimationResource(JimtaWizard mage){return Jugcraft.id("entity/jimtafacts/mage_casting");}
    }){
        @Override public void captureDefaultRenderState(JimtaWizard e,Void data,EntityRenderState s,float partial){super.captureDefaultRenderState(e,data,s,partial);s.addGeckolibData(WALK,e.walkAnimation.position(partial));s.addGeckolibData(SPEED,Math.min(1,e.walkAnimation.speed(partial)));s.addGeckolibData(TIME,e.tickCount+partial);s.addGeckolibData(PITCH,e.getXRot());s.addGeckolibData(PHASE,e.phase());s.addGeckolibData(CAST,e.castPose());}
        @Override public void adjustModelBonesForRender(RenderPassInfo<EntityRenderState> info,BoneSnapshots bones){super.adjustModelBonesForRender(info,bones);float w=info.getOrDefaultGeckolibData(WALK,0F),v=info.getOrDefaultGeckolibData(SPEED,0F),time=info.getOrDefaultGeckolibData(TIME,0F);float a=(float)Math.cos(w*.6662)*1.4F*v,b=(float)Math.cos(w*.6662+Math.PI)*1.4F*v;boolean casting=info.getOrDefaultGeckolibData(CAST,0)>0;bones.ifPresent("right_leg",p->p.setRotX(p.getRotX()+a));bones.ifPresent("left_leg",p->p.setRotX(p.getRotX()+b));if(!casting){bones.ifPresent("right_arm",p->p.setRotX(p.getRotX()+b*.6F));bones.ifPresent("left_arm",p->p.setRotX(p.getRotX()+a*.6F));}bones.ifPresent("head",p->p.setRotX(p.getRotX()+info.getOrDefaultGeckolibData(PITCH,0F)*(float)Math.PI/180));if(kind.equals("dead_king")){bones.ifPresent("jaw",p->p.setRotX((float)Math.sin(time*.05)*.0873F));bones.ifPresent("hair",p->p.setRotX(((float)Math.sin(time*.1)*10-30)*(float)Math.PI/180));bones.ifPresent("hair2",p->p.setRotX((float)Math.sin(time*.15)*.2618F));}}
    }.withRenderLayer(renderer->new com.geckolib.renderer.layer.builtin.ItemInHandGeoLayer<>(c,renderer,"rightItem","leftItem"))
      .withRenderLayer(renderer->new com.geckolib.renderer.layer.builtin.ItemArmorGeoLayer<JimtaWizard,Void,EntityRenderState>(renderer,c){
          @Override protected java.util.List<RenderData> getRelevantBones(RenderPassInfo<EntityRenderState> pass){return java.util.List.of(RenderData.head("armorBipedHead"),RenderData.body("armorBipedBody"),RenderData.leftArm("armorBipedLeftArm"),RenderData.rightArm("armorBipedRightArm"),RenderData.leftLeg("armorBipedLeftLeg"),RenderData.rightLeg("armorBipedRightLeg"),RenderData.leftFoot("armorBipedLeftFoot"),RenderData.rightFoot("armorBipedRightFoot"));}
      })));EntityRendererRegistry.register(JimtaWizardBolt.TYPE,BoltRenderer::new);}
    private static Identifier tex(String path){return Jugcraft.id("textures/jimtafacts/spellbooks/entity/"+path+".png");}
    private static final class BoltState extends EntityRenderState{String school;}
    private static final class BoltRenderer extends EntityRenderer<JimtaWizardBolt,BoltState>{
        BoltRenderer(EntityRendererProvider.Context c){super(c);}@Override public BoltState createRenderState(){return new BoltState();}@Override public void extractRenderState(JimtaWizardBolt e,BoltState s,float partial){super.extractRenderState(e,s,partial);s.school=e.school();}
        @Override public void submit(BoltState s,PoseStack pose,SubmitNodeCollector collector,CameraRenderState camera){pose.pushPose();pose.rotate(camera.orientation);int color=switch(s.school){case "fire"->0xffff8833;case "ice"->0xffaaddff;case "blood"->0xffbb3344;case "poison"->0xff88bb33;case "holy"->0xffffee99;default->0xffaa77dd;};collector.submitCustomGeometry(pose,RenderTypes.entityTranslucentEmissive(tex("magic_arrow")),(m,v)->{float[][] p={{-.2F,-.2F,0,1},{.2F,-.2F,1,1},{.2F,.2F,1,0},{-.2F,.2F,0,0}};for(var a:p)v.addVertex(m,a[0],a[1],0).setColor(color).setUv(a[2],a[3]).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xf000f0).setNormal(m,0,0,1);});pose.popPose();super.submit(s,pose,collector,camera);}
    }
}
