package io.github.jimbozoomer.jugcraft.client;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.*;
import com.geckolib.renderer.layer.builtin.BlockAndItemGeoLayer;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.wilds.*;
import java.util.*;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Original Gecko meshes/animation clips, orbit controllers, block sockets and crack sprites. */
final class WildBluffRenderer extends GeoEntityRenderer<WildBluff,EntityRenderState> {
    private static final DataTicket<Float> AGE=DataTicket.create("wilds_bluff_age",Float.class),YAW=DataTicket.create("wilds_bluff_yaw",Float.class),PITCH=DataTicket.create("wilds_bluff_pitch",Float.class);
    private static final DataTicket<Boolean> ALIVE=DataTicket.create("wilds_bluff_alive",Boolean.class);
    WildBluffRenderer(EntityRendererProvider.Context c){super(c,new Model<WildBluff>("bluff"));shadowRadius=.5F;}
    private static class Model<T extends GeoAnimatable> extends GeoModel<T>{
        final String kind;Model(String kind){this.kind=kind;}
        @Override public Identifier getModelResource(GeoRenderState s){return Jugcraft.id("entity/wilds/"+kind);}
        @Override public Identifier getTextureResource(GeoRenderState s){return kind.equals("bluff")?Jugcraft.id("textures/newest/wilds/entity/bluff.png"):Identifier.withDefaultNamespace("textures/block/dirt.png");}
        @Override public Identifier getAnimationResource(T e){return Jugcraft.id("entity/wilds/"+kind);}
    }
    static void register(){
        WildGeomancy.rumble=e->net.minecraft.client.Minecraft.getInstance().getSoundManager().play(new Rumble(e));
        EntityRendererRegistry.register(WildBluff.TYPE,WildBluffRenderer::new);
        EntityRendererRegistry.register(WildGeomancy.FISSURE,NoopRenderer::new);
        EntityRendererRegistry.register(WildGeomancy.CRACK,CrackRenderer::new);
        EntityRendererRegistry.register(WildGeomancy.SPIKE,c->new GeoEntityRenderer<WildGeomancy.Spike,EntityRenderState>(c,new Model<WildGeomancy.Spike>("earth_spike"))
            .withRenderLayer(r->new BlockAndItemGeoLayer<WildGeomancy.Spike,Void,EntityRenderState>(c,r){
                @Override protected List<RenderData> getRelevantBones(WildGeomancy.Spike e,Void unused,EntityRenderState s,float partial){var block=new BlockModelRenderState();blockModelResolver.update(block,e.material(),BlockDisplayContext.create());var result=new ArrayList<RenderData>();for(int i=1;i<=6;i++)result.add(RenderData.block("t1_block"+i,block));return result;}
                @Override public void addRenderData(WildGeomancy.Spike e,Void unused,EntityRenderState s,float partial){s.addGeckolibData(CONTENTS,getRelevantBones(e,unused,s,partial));}
                @Override protected void submitBlockRender(PoseStack pose,com.geckolib.cache.model.GeoBone bone,BlockModelRenderState block,EntityRenderState s,SubmitNodeCollector collector,int light){pose.pushPose();pose.translate(-.5,0,-.5);block.submit(pose,collector,light,OverlayTexture.NO_OVERLAY,s.outlineColor);pose.popPose();}
            }));
    }
    @Override public void captureDefaultRenderState(WildBluff e,Void unused,EntityRenderState s,float partial){super.captureDefaultRenderState(e,unused,s,partial);s.addGeckolibData(AGE,e.tickCount+partial);s.addGeckolibData(ALIVE,e.isAlive());s.addGeckolibData(YAW,Mth.rotLerp(partial,e.yHeadRotO-e.yBodyRotO,e.yHeadRot-e.yBodyRot)*Mth.DEG_TO_RAD);s.addGeckolibData(PITCH,Mth.lerp(partial,e.xRotO,e.getXRot())*Mth.DEG_TO_RAD);}
    @Override public void adjustModelBonesForRender(RenderPassInfo<EntityRenderState> info,BoneSnapshots bones){super.adjustModelBonesForRender(info,bones);float frame=info.getOrDefaultGeckolibData(AGE,0F);if(info.getOrDefaultGeckolibData(ALIVE,true)){for(String name:List.of("rotation1","rotation2","rotation3"))bones.ifPresent(name,b->b.setRotY(b.getRotY()+frame%360/4));bones.ifPresent("core",b->{b.setRotY(b.getRotY()-frame%360/4);b.setTranslateY(b.getTranslateY()+(float)Math.sin(frame/5)*.8F);b.setRotX(b.getRotX()+(float)Math.sin(frame/9));});}bones.ifPresent("head",b->b.setRotX(b.getRotX()+info.getOrDefaultGeckolibData(PITCH,0F)));bones.ifPresent("root",b->b.setRotY(b.getRotY()+info.getOrDefaultGeckolibData(YAW,0F)));}
    private static final class Rumble extends net.minecraft.client.resources.sounds.AbstractTickableSoundInstance {
        private final WildGeomancy.Fissure fissure;
        Rumble(WildGeomancy.Fissure e){super(OwnerWilds.sound("geomancy.rumble_loop"),net.minecraft.sounds.SoundSource.HOSTILE,net.minecraft.client.resources.sounds.SoundInstance.createUnseededRandom());fissure=e;looping=true;delay=0;volume=.8F;pitch=1;position();}
        private void position(){x=fissure.getX();y=fissure.getY();z=fissure.getZ();}
        @Override public void tick(){if(fissure.isRemoved()||fissure.level()!=net.minecraft.client.Minecraft.getInstance().level){stop();return;}position();}
    }
    private static final class CrackRenderer extends EntityRenderer<WildGeomancy.Crack,CrackState>{
        CrackRenderer(EntityRendererProvider.Context c){super(c);}
        @Override public CrackState createRenderState(){return new CrackState();}
        @Override public void extractRenderState(WildGeomancy.Crack e,CrackState s,float partial){super.extractRenderState(e,s,partial);s.frame=Math.min(5,e.tickCount);s.quads.clear();float angle=(float)Math.toRadians(-(e.getYRot()+180)),cos=(float)Math.cos(angle),sin=(float)Math.sin(angle);
            for(var p:BlockPos.betweenClosed(e.blockPosition().offset(-2,-2,-2),e.blockPosition().offset(2,1,2))){if(!e.level().hasChunkAt(p))continue;var block=e.level().getBlockState(p);if(!block.isSolidRender()||!e.level().getBlockState(p.above()).getCollisionShape(e.level(),p.above()).isEmpty())continue;var shape=block.getCollisionShape(e.level(),p);if(shape.isEmpty())continue;var box=shape.bounds();float x0=(float)Math.max(-2,p.getX()+box.minX-e.getX()),x1=(float)Math.min(2,p.getX()+box.maxX-e.getX()),z0=(float)Math.max(-2,p.getZ()+box.minZ-e.getZ()),z1=(float)Math.min(2,p.getZ()+box.maxZ-e.getZ()),y=(float)(p.getY()+box.maxY-e.getY()+.002);if(x1<=x0||z1<=z0)continue;float[][] quad=new float[4][];float[][] xy={{x0,z0},{x0,z1},{x1,z1},{x1,z0}};for(int i=0;i<4;i++){float x=xy[i][0],z=xy[i][1];quad[i]=new float[]{x,y,z,(x*cos-z*sin)/4+.5F,(x*sin+z*cos)/4+.5F};}s.quads.add(quad);
                // Carry the supplied crack decal over one-block exposed ledges.
                var directions=new net.minecraft.core.Direction[]{net.minecraft.core.Direction.WEST,net.minecraft.core.Direction.SOUTH,net.minecraft.core.Direction.EAST,net.minecraft.core.Direction.NORTH};
                for(int i=0;i<4;i++){var direction=directions[i];var neighbor=p.relative(direction);if(!e.level().hasChunkAt(neighbor)||e.level().getBlockState(neighbor).isSolidRender()||!e.level().getBlockState(p.below().relative(direction)).isSolidRender())continue;
                    int j=(i+1)%4;var a=quad[i];var b=quad[j];float dx=direction.getStepX()*.002F,dz=direction.getStepZ()*.002F;
                    s.quads.add(new float[][]{{a[0]+dx,y,a[2]+dz,a[3],a[4]},{b[0]+dx,y,b[2]+dz,b[3],b[4]},{b[0]+dx,y-1,b[2]+dz,b[3],b[4]},{a[0]+dx,y-1,a[2]+dz,a[3],a[4]}});
                }
            }
        }
        @Override public void submit(CrackState s,PoseStack pose,SubmitNodeCollector collector,CameraRenderState camera){var quads=List.copyOf(s.quads);int light=s.lightCoords;collector.submitCustomGeometry(pose,RenderTypes.entityTranslucent(Jugcraft.id("textures/newest/wilds/particle/crack_"+s.frame+".png")),(matrix,buffer)->{for(var quad:quads)for(var v:quad)buffer.addVertex(matrix,v[0],v[1],v[2]).setColor(0xA6000000).setUv(v[3],v[4]).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(matrix,0,1,0);});super.submit(s,pose,collector,camera);}
    }
    private static final class CrackState extends EntityRenderState{int frame;final List<float[][]> quads=new ArrayList<>();}
}
