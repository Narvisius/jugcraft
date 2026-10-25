package io.github.jimbozoomer.jugcraft.client.jimtafacts;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.*;
import net.minecraft.world.phys.Vec3;

/** Recovered source shield meshes/swirl formula, ten-frame portals and scapegoat procedural gait. */
public final class JimtaWardClient {
    private static final ModelLayerLocation SHIELD=new ModelLayerLocation(Jugcraft.id("jimtafacts_shield"),"surface"),TRIM=new ModelLayerLocation(Jugcraft.id("jimtafacts_shield"),"trim");
    private static final DataTicket<Float> WALK=DataTicket.create("jimta_scapegoat_walk",Float.class),SPEED=DataTicket.create("jimta_scapegoat_speed",Float.class);
    public static void register(){
        net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(JimtaPortalFrames.NODE,JimtaPortalFrameRenderer::new);
        ModelLayerRegistry.registerModelLayer(SHIELD,()->shield(false));ModelLayerRegistry.registerModelLayer(TRIM,()->shield(true));
        JimtaWardEntity.TYPES.values().forEach(type->EntityRendererRegistry.register(type,WardRenderer::new));
        EntityRendererRegistry.register(JimtaScapegoat.TYPE,c->new GeoEntityRenderer<JimtaScapegoat,EntityRenderState>(c,new GeoModel<>(){
            @Override public Identifier getModelResource(GeoRenderState s){return Jugcraft.id("entity/jimtafacts/scapegoat");}
            @Override public Identifier getTextureResource(GeoRenderState s){return tex("entity/scapegoat");}
            @Override public Identifier getAnimationResource(JimtaScapegoat e){return Jugcraft.id("entity/jimtafacts/scapegoat");}
        }){
            @Override public void captureDefaultRenderState(JimtaScapegoat e,Void data,EntityRenderState s,float partial){super.captureDefaultRenderState(e,data,s,partial);s.addGeckolibData(WALK,e.walkAnimation.position(partial));s.addGeckolibData(SPEED,Math.min(1,e.walkAnimation.speed(partial)));}
            @Override public void adjustModelBonesForRender(RenderPassInfo<EntityRenderState> info,BoneSnapshots bones){super.adjustModelBonesForRender(info,bones);float w=info.getOrDefaultGeckolibData(WALK,0F),speed=info.getOrDefaultGeckolibData(SPEED,0F);float a=(float)Math.cos(w*.6662)*1.4F*speed,b=(float)Math.cos(w*.6662+Math.PI)*1.4F*speed;
                bones.ifPresent("rightHindLeg",p->p.setRotX(p.getRotX()+a));bones.ifPresent("leftFrontLeg",p->p.setRotX(p.getRotX()+a));bones.ifPresent("leftHindLeg",p->p.setRotX(p.getRotX()+b));bones.ifPresent("rightFrontLeg",p->p.setRotX(p.getRotX()+b));
            }
        });
        ClientTickEvents.END_CLIENT_TICK.register(client->{var p=client.player;if(p==null||!JimtaWardSpells.has(p,"planar_sight"))return;for(int i=0;i<3;i++)p.level().addParticle(ParticleTypes.WHITE_ASH,p.getX()+(p.getRandom().nextDouble()-.5)*32,p.getY()+p.getRandom().nextDouble()*10,p.getZ()+(p.getRandom().nextDouble()-.5)*32,0,0,0);});
    }
    public static boolean sees(Entity e){var p=Minecraft.getInstance().player;return p!=null&&e instanceof LivingEntity&&e!=p&&e.isAlive()&&p.distanceToSqr(e)<=32*32&&JimtaWardSpells.has(p,"planar_sight");}
    public static boolean hidden(Entity e){return e instanceof LivingEntity living&&JimtaWardSpells.concealed(living)&&!sees(e);}
    private static Identifier tex(String path){return Jugcraft.id("textures/jimtafacts/spellbooks/"+path+".png");}
    public static final class WardState extends EntityRenderState {String kind;float time,yaw,pitch;Vec3 chain=Vec3.ZERO;}
    private static final class WardRenderer extends EntityRenderer<JimtaWardEntity,WardState>{
        final ModelPart shield,trim;
        WardRenderer(EntityRendererProvider.Context c){super(c);shield=c.bakeLayer(SHIELD);trim=c.bakeLayer(TRIM);}
        @Override public WardState createRenderState(){return new WardState();}
        @Override public void extractRenderState(JimtaWardEntity e,WardState s,float partial){super.extractRenderState(e,s,partial);s.kind=e.kind();s.time=e.tickCount+partial;s.yaw=e.getYRot();s.pitch=e.getXRot();var victim=e.victim();s.chain=victim==null?e.getDeltaMovement().scale(-1):victim.getPosition(partial).add(0,victim.getBbHeight()*.5,0).subtract(e.getPosition(partial)).scale(8D/9);}
        @Override public void submit(WardState s,PoseStack pose,SubmitNodeCollector collector,CameraRenderState camera){
            pose.pushPose();
            if(s.kind.equals("arcane_shackle")){chain(s,pose,collector);}
            else{
                pose.rotateDegrees(Axis.YP,-s.yaw);
                if(s.kind.equals("shield")){
                    pose.rotateDegrees(Axis.XP,s.pitch);pose.scale(3.25F,3.25F,3.25F);float f=s.time*.02F;
                    collector.order(0).submitModelPart(shield,pose,RenderTypes.energySwirl(tex("entity/shield/shield_overlay"),noise(1.2F*f),noise(f+456)),0xf000f0,OverlayTexture.NO_OVERLAY,null);
                    collector.order(1).submitModelPart(trim,pose,RenderTypes.energySwirl(tex("entity/shield/shield_trim"),0,0),0xf000f0,OverlayTexture.NO_OVERLAY,null);
                }else{
                    float u=((int)s.time/2%10)/10F;
                    collector.submitCustomGeometry(pose,RenderTypes.entityTranslucentEmissive(tex("entity/portal/"+(s.kind.equals("pocket_portal")?"pocket_dimension_portal_round":"portal_round"))),(matrix,v)->{
                        float[][] p={{-.5F,0,u,0},{.5F,0,u+.1F,0},{.5F,2,u+.1F,1},{-.5F,2,u,1}};
                        for(int side=0;side<2;side++)for(int i=0;i<4;i++){float[] a=p[side==0?i:3-i];v.addVertex(matrix,a[0],a[1],0).setColor(0x64ffffff).setUv(a[2],a[3]).setLight(0xf000f0).setOverlay(OverlayTexture.NO_OVERLAY).setNormal(matrix,0,0,side==0?1:-1);}
                    });
                }
            }pose.popPose();super.submit(s,pose,collector,camera);
        }
        private static void chain(WardState s,PoseStack pose,SubmitNodeCollector collector){
            double length=s.chain.length();if(length<1e-4)return;Vec3 direction=s.chain.normalize();pose.rotate(new org.joml.Quaternionf().rotationTo(0,0,1,(float)direction.x,(float)direction.y,(float)direction.z));
            float start=(float)(length*(1-Math.clamp(s.time/3,0,1))),end=(float)length;
            collector.submitCustomGeometry(pose,RenderTypes.entityTranslucentEmissive(tex("entity/ender_chain")),(matrix,v)->{
                for(int plane=0;plane<2;plane++)for(int side=0;side<2;side++){
                    float[][] a={{-.5F,start,0,0},{.5F,start,1,0},{.5F,end,1,end},{-.5F,end,0,end}};
                    for(int n=0;n<4;n++){var p=a[side==0?n:3-n];v.addVertex(matrix,plane==0?p[0]:0,plane==1?p[0]:0,p[1]).setColor(-1).setUv(p[2],p[3]).setLight(0xf000f0).setOverlay(OverlayTexture.NO_OVERLAY).setNormal(matrix,0,1,0);}
                }
            });
        }
    }
    private static float noise(float f){return (float)(Math.sin(f/4)+2*Math.sin(f/3)+3*Math.sin(f/2)+4*Math.sin(f))*.25F;}
    public static LayerDefinition shield(boolean trim){
        var mesh=new MeshDefinition();var cubes=CubeListBuilder.create();
        if(!trim){
            cubes.texOffs(4,8).addBox(-6,-3,0,12,8,.1F).texOffs(7,7).addBox(-3,5,0,6,1,.1F).texOffs(9,6).addBox(-1,6,0,2,1,.1F).texOffs(5,16).addBox(-5,-4,0,10,1,.1F).texOffs(7,18).addBox(-3,-6,0,6,1,.1F).texOffs(6,17).addBox(-4,-5,0,8,1,.1F).texOffs(8,19).addBox(-2,-7,0,4,1,.1F);
        }else{
            float[][] boxes={{-7,-3,1,9},{6,-3,1,9},{-6,-4,1,1},{-5,-5,1,1},{-4,-6,1,1},{-2,-8,4,1},{2,-7,1,1},{3,-6,1,1},{5,-4,1,1},{4,-5,1,1},{-3,-7,1,1},{-6,5,3,1},{3,5,3,1},{1,6,2,1},{-3,6,2,1},{-1,7,2,1}};
            for(var b:boxes)cubes.texOffs(0,0).addBox(b[0],b[1],-.5F,b[2],b[3],1);
        }
        mesh.getRoot().addOrReplaceChild("bb_main",cubes,PartPose.ZERO);return LayerDefinition.create(mesh,64,32);
    }
}
