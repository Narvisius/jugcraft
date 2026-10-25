package io.github.jimbozoomer.jugcraft.client.jimtafacts;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.*;
import com.geckolib.renderer.layer.builtin.*;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks.*;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.*;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Original meshes and clips; procedural gait equations recovered from the supplied model classes. */
public final class JimtaEncounterClient {
    private static final DataTicket<Float> WALK=DataTicket.create("jimta_enc_walk",Float.class),SPEED=DataTicket.create("jimta_enc_speed",Float.class),YAW=DataTicket.create("jimta_enc_yaw",Float.class),PITCH=DataTicket.create("jimta_enc_pitch",Float.class),CROUCH=DataTicket.create("jimta_enc_crouch",Float.class),AGE=DataTicket.create("jimta_enc_age",Float.class),FORWARD=DataTicket.create("jimta_enc_forward",Float.class),SIDE=DataTicket.create("jimta_enc_side",Float.class),HURT=DataTicket.create("jimta_enc_hurt",Float.class);
    private static final DataTicket<Boolean> RESTORED=DataTicket.create("jimta_enc_restored",Boolean.class),ACTING=DataTicket.create("jimta_enc_acting",Boolean.class);
    private static final DataTicket<Vec3> NORMAL=DataTicket.create("jimta_enc_normal",Vec3.class);
    public static void register(){EntityRendererRegistry.register(JimtaEncounterCreatures.ICE_SPIDER,c->new CreatureRenderer(c,true));EntityRendererRegistry.register(JimtaEncounterCreatures.KEEPER,c->new CreatureRenderer(c,false));EntityRendererRegistry.register(JimtaSpiderTomb.TOMB,c->new JimtaSpiderHazardRenderer(c,false));EntityRendererRegistry.register(JimtaSpiderTomb.SPIKE,c->new JimtaSpiderHazardRenderer(c,true));}
    private static Identifier tex(String path){return Jugcraft.id("textures/jimtafacts/spellbooks/"+path+".png");}
    private static final class CreatureRenderer extends GeoEntityRenderer<JimtaEncounterCreatures.Encounter,EntityRenderState> {
        private final boolean spider;
        CreatureRenderer(EntityRendererProvider.Context c,boolean spider){
            super(c,new GeoModel<>(){
                @Override public Identifier getModelResource(GeoRenderState s){return Jugcraft.id("entity/jimtafacts/"+(spider?"ice_spider":"citadel_keeper"));}
                @Override public Identifier getTextureResource(GeoRenderState s){return tex(spider?"entity/ice_spider/ice_spider":"entity/keeper/"+(s.getOrDefaultGeckolibData(RESTORED,false)?"keeper_restored":"keeper"));}
                @Override public Identifier getAnimationResource(JimtaEncounterCreatures.Encounter e){return Jugcraft.id("entity/jimtafacts/"+(spider?"ice_spider":"citadel_keeper"));}
            });this.spider=spider;shadowRadius=spider?1.2F:.65F;
            if(!spider){withScale(1.3F);withRenderLayer(new ItemInHandGeoLayer<>(c,this,"rightItem","leftItem"));withRenderLayer(new TextureLayerGeoLayer<>(this,tex("entity/keeper/keeper_ghost")){
                @Override protected RenderType getRenderType(EntityRenderState s){if(s.getOrDefaultGeckolibData(HURT,0F)<=0)return null;float time=s.getOrDefaultGeckolibData(AGE,0F);return RenderTypes.energySwirl(tex("entity/keeper/keeper_ghost"),time*.02F%1,time*.01F%1);}
            });}
        }
        @Override public RenderType getRenderType(EntityRenderState s,Identifier texture){return RenderTypes.entityTranslucent(texture);}
        @Override public void captureDefaultRenderState(JimtaEncounterCreatures.Encounter e,Void unused,EntityRenderState s,float partial){
            super.captureDefaultRenderState(e,unused,s,partial);s.addGeckolibData(WALK,e.walkAnimation.position(partial));s.addGeckolibData(SPEED,Math.min(1,e.walkAnimation.speed(partial)));s.addGeckolibData(YAW,Mth.rotLerp(partial,e.yHeadRotO-e.yBodyRotO,e.yHeadRot-e.yBodyRot)*Mth.DEG_TO_RAD);s.addGeckolibData(PITCH,Mth.lerp(partial,e.xRotO,e.getXRot())*Mth.DEG_TO_RAD);s.addGeckolibData(AGE,e.tickCount+partial);s.addGeckolibData(ACTING,e.animating());s.addGeckolibData(HURT,(float)e.hurtTime);
            var facing=e.getForward().multiply(1,0,1).normalize();var momentum=e.getDeltaMovement().multiply(1,0,1).normalize();s.addGeckolibData(FORWARD,(float)facing.dot(momentum));s.addGeckolibData(SIDE,(float)new Vec3(-facing.z,0,facing.x).dot(momentum)*.35F);
            if(e instanceof JimtaEncounterCreatures.IceSpider ice){s.addGeckolibData(CROUCH,ice.crouch(partial));s.addGeckolibData(NORMAL,ice.lastNormal.lerp(ice.normal,partial));}else if(e instanceof JimtaEncounterCreatures.Keeper k)s.addGeckolibData(RESTORED,k.restored());
        }
        @Override public void adjustRenderPose(RenderPassInfo<EntityRenderState> info){super.adjustRenderPose(info);if(spider){var n=info.getOrDefaultGeckolibData(NORMAL,new Vec3(0,1,0));info.poseStack().rotate(new org.joml.Quaternionf().rotationTo(0,1,0,(float)n.x,(float)n.y,(float)n.z));}}
        @Override public void adjustModelBonesForRender(RenderPassInfo<EntityRenderState> info,BoneSnapshots bones){
            super.adjustModelBonesForRender(info,bones);float swing=info.getOrDefaultGeckolibData(WALK,0F),speed=info.getOrDefaultGeckolibData(SPEED,0F),yaw=info.getOrDefaultGeckolibData(YAW,0F),pitch=info.getOrDefaultGeckolibData(PITCH,0F),age=info.getOrDefaultGeckolibData(AGE,0F);
            bones.ifPresent("head",p->{p.setRotX(p.getRotX()-pitch);p.setRotY(p.getRotY()-yaw);});
            if(spider){
                float crouch=info.getOrDefaultGeckolibData(CROUCH,1F);bones.ifPresent("torso",p->p.setTranslateY(p.getTranslateY()+18*(.5F+.5F*crouch)));
                float primaryY=legY(swing,.2F,0)*.17453292F*speed,secondaryY=legY(swing,.2F,(float)Math.PI)*.17453292F*speed,primaryZ=legZ(swing,.2F,(float)Math.PI)*.10471976F*speed,secondaryZ=legZ(swing,.2F,0)*.10471976F*speed;
                String[] sides={"right","left"},legs={"Fore","ForeMiddle","BackMiddle","Back"};
                for(int i=0;i<2;i++)for(int j=0;j<4;j++){int sign=i==0?-1:1;float y=(j%2==0?primaryY:secondaryY)*sign,z=((j%2==0?primaryZ:secondaryZ)+(1-crouch)*10*Mth.DEG_TO_RAD)*-sign;String base=sides[i]+legs[j];bones.ifPresent(base+"Shoulder",p->p.setRotY(p.getRotY()+y));bones.ifPresent(base+"Leg",p->p.setRotZ(p.getRotZ()+z));}
            }else{
                float forward=info.getOrDefaultGeckolibData(FORWARD,0F),side=info.getOrDefaultGeckolibData(SIDE,0F);float right=(float)Math.cos(swing*.6662),left=(float)Math.cos(swing*.6662+Math.PI),rSide=-(float)Math.sin(swing*.6662)*4*speed,lSide=-(float)Math.sin(swing*.6662-Math.PI)*4*speed;
                bones.ifPresent("right_leg",p->{p.setTranslateX(p.getTranslateX()+rSide*side);p.setTranslateY(p.getTranslateY()+right*3*speed);p.setTranslateZ(p.getTranslateZ()+rSide*forward);p.setRotX(p.getRotX()+right*1.05F*speed);});bones.ifPresent("left_leg",p->{p.setTranslateX(p.getTranslateX()+lSide*side);p.setTranslateY(p.getTranslateY()+left*3*speed);p.setTranslateZ(p.getTranslateZ()+lSide*forward);p.setRotX(p.getRotX()+left*1.05F*speed);});
                bones.ifPresent("body",p->p.setTranslateY(p.getTranslateY()+Math.abs((float)Math.cos((swing*1.2662-Math.PI/2)*.5))*1.5F*speed));
                if(!info.getOrDefaultGeckolibData(ACTING,false)){float bobZ=(float)Math.cos(age*.09)*.05F+.05F,bobX=(float)Math.sin(age*.067)*.05F;bones.ifPresent("right_arm",p->{p.setRotX(p.getRotX()+left*speed+bobX);p.setRotZ(p.getRotZ()+bobZ);});bones.ifPresent("left_arm",p->{p.setRotX(p.getRotX()+right*speed-bobX);p.setRotZ(p.getRotZ()-bobZ);});}
            }
        }
    }
    private static float legY(float limb,float speed,float offset){float f=offset-(float)Math.PI/2,t=limb*(float)Math.PI*2*speed+f;return (float)Math.sin(t+Math.sin(t)*.5);}
    private static float legZ(float limb,float speed,float offset){float f=(float)Math.sin(limb*(float)Math.PI*2*speed+offset);return Math.max(f*f*f,0);}
}
