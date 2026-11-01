package io.github.jimbozoomer.jugcraft.client;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.constant.DataTickets;
import io.github.jimbozoomer.jugcraft.concordance.celestial.Calendar;
import net.minecraft.client.renderer.SubmitNodeCollector;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.*;
import com.geckolib.renderer.layer.builtin.TextureLayerGeoLayer;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.wilds.WildElokosa;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.*;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Original day/night/elite geometry, clips, glow masks and source locomotion equations. */
final class WildElokosaRenderer extends GeoEntityRenderer<WildElokosa,EntityRenderState>{
    private static final DataTicket<Boolean> NIGHT=DataTicket.create("wilds_elokosa_night",Boolean.class),ELITE=DataTicket.create("wilds_elokosa_elite",Boolean.class),IDLE=DataTicket.create("wilds_elokosa_idle",Boolean.class);
    private static final DataTicket<Float> WALK=DataTicket.create("wilds_elokosa_walk",Float.class),SPEED=DataTicket.create("wilds_elokosa_speed",Float.class),FORWARD=DataTicket.create("wilds_elokosa_forward",Float.class),SIDE=DataTicket.create("wilds_elokosa_side",Float.class),YAW=DataTicket.create("wilds_elokosa_yaw",Float.class),PITCH=DataTicket.create("wilds_elokosa_pitch",Float.class);
    private static final DataTicket<WildElokosaDrool.Mouth> MOUTH=DataTicket.create("wilds_elokosa_mouth",WildElokosaDrool.Mouth.class);
    private static final DataTicket<WildSpringChain.Frame> TAIL=DataTicket.create("wilds_elokosa_tail",WildSpringChain.Frame.class);
    private static final DataTicket<Integer> MOON=DataTicket.create("wilds_elokosa_moon",Integer.class);
    private static final DataTicket<Float> TRANSFORM=DataTicket.create("wilds_elokosa_transform",Float.class);
    WildElokosaRenderer(EntityRendererProvider.Context c){super(c,new GeoModel<>(){
        @Override public Identifier getModelResource(GeoRenderState s){return Jugcraft.id("entity/wilds/elokosa");}
        @Override public Identifier getTextureResource(GeoRenderState s){return texture(s.getOrDefaultGeckolibData(ELITE,false),false);}
        @Override public Identifier getAnimationResource(WildElokosa e){return Jugcraft.id("entity/wilds/elokosa");}
    });shadowRadius=.7F;withRenderLayer(new TextureLayerGeoLayer<>(this,texture(false,true)){
        @Override protected RenderType getRenderType(EntityRenderState s){return RenderTypes.eyes(texture(s.getOrDefaultGeckolibData(ELITE,false),true));}
    });
        withRenderLayer(new TextureLayerGeoLayer<>(this,Jugcraft.id("textures/newest/wilds/entity/elokosa_transforming.png"),RenderTypes::entityTranslucent){
            @Override public void submitRenderTask(RenderPassInfo<EntityRenderState> info,SubmitNodeCollector collector){
                if(!info.willRender())return;
                // Resolve this entity's deferred animation before reading its authored controller.
                info.renderPosed(()->{});
                float alpha=info.getOrDefaultGeckolibData(TRANSFORM,0F);
                if(alpha<=0)return;
                int original=info.renderColor();
                info.renderState().addGeckolibData(DataTickets.RENDER_COLOR,((int)((original>>>24)*alpha)<<24)|(original&0xFFFFFF));
                try {super.submitRenderTask(info,collector);} finally {info.renderState().addGeckolibData(DataTickets.RENDER_COLOR,original);}
            }
        });
        withRenderLayer(new TextureLayerGeoLayer<>(this,Jugcraft.id("textures/newest/wilds/entity/elokosa_paw_full.png"),RenderTypes::entityTranslucent){
            @Override protected Identifier getTextureResource(EntityRenderState s){
                String phase=switch(s.getOrDefaultGeckolibData(MOON,0)){case 0->"full";case 1,7->"gibbous";case 2,6->"half";default->"crescent";};
                return Jugcraft.id("textures/newest/wilds/entity/elokosa_paw_"+phase+".png");
            }
        });
    }
    private static Identifier texture(boolean elite,boolean glow){return Jugcraft.id("textures/newest/wilds/entity/elokosa"+(elite?"_elite":"")+(glow?"_glowmask":"")+".png");}
    static void register(){EntityRendererRegistry.register(WildElokosa.HOWLER,WildElokosaRenderer::new);EntityRendererRegistry.register(WildElokosa.FOLLOWER,WildElokosaRenderer::new);}
    @Override public void captureDefaultRenderState(WildElokosa e,Void unused,EntityRenderState s,float partial){super.captureDefaultRenderState(e,unused,s,partial);s.addGeckolibData(TAIL,WildSpringChain.capture(e,partial));s.addGeckolibData(MOON,Calendar.moonPhase(e.level().getOverworldClockTime()));s.addGeckolibData(MOUTH,WildElokosaDrool.capture(e));s.addGeckolibData(NIGHT,e.night());s.addGeckolibData(ELITE,e.howler());s.addGeckolibData(IDLE,e.action()==WildElokosa.IDLE&&e.cling()==Direction.DOWN&&e.isAlive());s.addGeckolibData(WALK,e.walkAnimation.position(partial));s.addGeckolibData(SPEED,e.walkAnimation.speed(partial));s.addGeckolibData(YAW,Mth.rotLerp(partial,e.yHeadRotO-e.yBodyRotO,e.yHeadRot-e.yBodyRot)*Mth.DEG_TO_RAD);s.addGeckolibData(PITCH,Mth.lerp(partial,e.xRotO,e.getXRot())*Mth.DEG_TO_RAD);var look=e.getLookAngle().multiply(1,0,1).normalize();var v=e.getDeltaMovement().multiply(1,0,1).normalize();s.addGeckolibData(FORWARD,(float)look.dot(v));s.addGeckolibData(SIDE,(float)(look.x*v.z-look.z*v.x));}
    @Override public void preRenderPass(RenderPassInfo<EntityRenderState> info,SubmitNodeCollector collector){
        super.preRenderPass(info,collector);
        var mouth=info.getGeckolibData(MOUTH);
        if(mouth!=null)for(int i=0;i<6;i++){final int index=i;info.addBonePositionListener("droolPos"+(i+1),(world,model,local)->mouth.points[index]=world);}
    }
    @Override public void adjustModelBonesForRender(RenderPassInfo<EntityRenderState> info,BoneSnapshots bones){super.adjustModelBonesForRender(info,bones);info.renderState().addGeckolibData(TRANSFORM,Math.clamp(-bones.get("transformTextureController").map(b->b.getTranslateX()).orElse(0F),0,1));boolean night=info.getOrDefaultGeckolibData(NIGHT,false),elite=info.getOrDefaultGeckolibData(ELITE,false);float morph=-bones.get("whichFormController").map(b->b.getTranslateX()).orElse(0F);boolean dayVisible=morph<=.1F?!night:morph<1,nightVisible=morph<=.1F?night:true;
        bones.ifPresent("root_day",b->b.skipRender(!dayVisible).skipChildrenRender(!dayVisible));bones.ifPresent("root",b->b.skipRender(!nightVisible).skipChildrenRender(!nightVisible));
        bones.ifPresent("root_shared",b->{float scale=elite?1.185F:1.085F;b.setScale(b.getScaleX()*scale,b.getScaleY()*scale,b.getScaleZ()*scale);});
        for(String name:new String[]{"scythe","beard"})bones.ifPresent(name,b->b.skipRender(!elite).skipChildrenRender(!elite));bones.ifPresent("tailTuft",b->b.skipRender(elite).skipChildrenRender(elite));
        if(elite){bones.ifPresent("tail1",b->b.setScale(b.getScaleX()*.91561186F,b.getScaleY()*.91561186F,b.getScaleZ()*.91561186F));for(int i=1;i<=4;i++)bones.ifPresent("tuft"+i,b->b.setScaleY(b.getScaleY()*1.18F));}
        for(String name:new String[]{"headLook","head_day"})bones.ifPresent(name,b->{b.setRotX(b.getRotX()+info.getOrDefaultGeckolibData(PITCH,0F));b.setRotY(b.getRotY()+info.getOrDefaultGeckolibData(YAW,0F));});
        if(info.getOrDefaultGeckolibData(IDLE,false)){float f=info.getOrDefaultGeckolibData(FORWARD,0F),side=info.getOrDefaultGeckolibData(SIDE,0F),speed=info.getOrDefaultGeckolibData(SPEED,0F);new WildElokosaPose(bones).apply(info.getOrDefaultGeckolibData(WALK,0F),speed,Math.max(0,f),Math.max(0,-f),Math.max(0,side),Math.max(0,-side),Math.clamp((speed-.12)/.25,0,1),1);}
        var tail=info.getGeckolibData(TAIL);if(nightVisible&&tail!=null)tail.chain().apply(tail,bones);
    }
}
