package io.github.jimbozoomer.jugcraft.client.jimtafacts;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.renderpearl.api.buffers.*;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;

/** Supplied PocketDimensionEffects geometry/motion, adapted to 26.3's sky render pass.
 * Original pixels and seeded layout are retained. Meshes are allocated once and closed with SkyRenderer.
 * No legacy GL state, global world reads during draw, or per-frame mesh allocation. */
public final class JimtaRealmSky implements AutoCloseable {
    private static Identifier texture(String name){return Jugcraft.id("textures/jimtafacts/spellbooks/environment/"+name+".png");}
    private static final Identifier SKY=texture("pocket_dimension_sky"),CLOUD=texture("pocket_clouds"),WISP=texture("single_cloud");
    private record Layer(GpuBuffer vertices,int indices,Identifier texture,double x,double y,double z,double dx,double dy,double dz){}
    private final List<Layer> layers=new ArrayList<>();
    private final List<GpuBuffer> borderFrames=new ArrayList<>();

    public JimtaRealmSky(){
        try{
            // Original scrolling noise aura: 48 fixed UV phases, no per-frame mesh allocation.
            // Native rooms are 16 blocks wide at y=32, rather than the source's hard-coded 7-block room.
            for(int frame=0;frame<48;frame++){
                float uv=frame/48F;
                borderFrames.add(mesh(16,b->{for(int side=0;side<4;side++){
                    var pose=new Matrix4f().rotationY((float)Math.toRadians(side*90));
                    b.addVertex(pose,-8,0,8).setUv(0,uv+1F/48).setColor(-6745686);
                    b.addVertex(pose,-8,3,8).setUv(0,uv).setColor(0xff000000);
                    b.addVertex(pose,8,3,8).setUv(5,uv).setColor(0xff000000);
                    b.addVertex(pose,8,0,8).setUv(5,uv+1F/48).setColor(-6745686);
                }}));
            }
            layers.add(cube(100,1,0xff454545,SKY,0,0,0,0,0,0));
            var random=new Random(431);
            float scale=.8f;
            for(int i=0;i<6;i++,scale-=.04f){
                float speed=(.01f+i*i*.09f)*.015f;
                double dx=speed*(random.nextFloat()-.5f),dy=speed*(random.nextFloat()-.5f),dz=speed*(random.nextFloat()-.5f);
                float intensity=.25f+(5-i)/6f*(.8f-.25f);
                int color=color((random.nextFloat()*.5f+.5f)*intensity*.5f,(random.nextFloat()*.5f+.5f)*intensity*.5f,(random.nextFloat()*.5f+.5f)*intensity*.5f);
                layers.add(cube(100*scale,4+2*scale,color,CLOUD,i*68731d,i*74869d,i*98744d,dx,dy,dz));
            }
            float[][] colors={{.1f*.075f,.4f*.075f,.6f*.075f},{.6f*.125f,.1f*.125f,.5f*.125f},{.3f*.125f,.3f*.125f,.3f*.125f}};
            float zoff=0;
            for(float[] rgb:colors)for(int i=0;i<15;i++){
                int count=i+1;float width=100*scale*(.15f+i*.003f),distance=100*scale;
                int x=random.nextInt(360),y=random.nextInt(360),z=random.nextInt(360);
                var poses=new ArrayList<Matrix4f>(count);
                for(int j=0;j<count;j++){
                    float offset=25*(1+j*.025f);
                    poses.add(new Matrix4f().translation((random.nextFloat()-.5f)*offset,zoff,(random.nextFloat()-.5f)*offset));
                    zoff+=.03f;
                }
                int color=color(rgb[0]*.37647f,rgb[1]*.37647f,rgb[2]*.37647f);
                var mesh=mesh(count*4,b->{for(var pose:poses)quad(b,pose,width,distance,1,color);});
                layers.add(new Layer(mesh,count*6,WISP,x,y,z,.005,.005,.005));
            }
        }catch(RuntimeException|Error e){close();throw e;}
    }
    private static int color(float r,float g,float b){return 0xff000000|(Math.clamp(Math.round(r*255),0,255)<<16)|(Math.clamp(Math.round(g*255),0,255)<<8)|Math.clamp(Math.round(b*255),0,255);}
    private static GpuBuffer mesh(int vertices,Consumer<BufferBuilder> writer){
        try(var bytes=ByteBufferBuilder.exactlySized(vertices*DefaultVertexFormat.POSITION_TEX_COLOR.getVertexSize())){
            var builder=new BufferBuilder(bytes,PrimitiveTopology.QUADS,DefaultVertexFormat.POSITION_TEX_COLOR);writer.accept(builder);
            try(var mesh=builder.buildOrThrow()){return RenderSystem.getDevice().createBuffer(()->"Jugcraft realm sky",40,mesh.vertexBuffer());}
        }
    }
    private static Layer cube(float size,float uv,int color,Identifier texture,double x,double y,double z,double dx,double dy,double dz){
        var buffer=mesh(24,b->{for(int face=0;face<6;face++){
            var pose=new Matrix4f();switch(face){case 1->pose.rotationX((float)Math.PI/2);case 2->pose.rotationX(-(float)Math.PI/2);case 3->pose.rotationX((float)Math.PI);case 4->pose.rotationZ((float)Math.PI/2);case 5->pose.rotationZ(-(float)Math.PI/2);}
            quad(b,pose,size,size,uv,color);
        }});
        return new Layer(buffer,36,texture,x,y,z,dx,dy,dz);
    }
    private static void quad(BufferBuilder b,Matrix4f pose,float width,float distance,float uv,int color){
        b.addVertex(pose,-width,-distance,-width).setUv(0,0).setColor(color);
        b.addVertex(pose,-width,-distance,width).setUv(0,uv).setColor(color);
        b.addVertex(pose,width,-distance,width).setUv(uv,uv).setColor(color);
        b.addVertex(pose,width,-distance,-width).setUv(uv,0).setColor(color);
    }
    public void render(RenderTarget target,GpuBufferSlice fog,double ticks,net.minecraft.world.phys.Vec3 borderOffset){
        RenderSystem.setShaderFog(fog);
        var indices=RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS);
        var index=indices.getBuffer(90);
        try(var pass=RenderSystem.getDevice().createCommandEncoder().createRenderPass(()->"Jugcraft pocket realm",target.getColorTextureView(),Optional.empty(),target.getDepthTextureView(),OptionalDouble.empty())){
            RenderSystem.bindDefaultUniforms(pass);
            for(int i=0;i<layers.size();i++){
                var l=layers.get(i);var matrix=new Matrix4f(RenderSystem.getModelViewMatrixCopy());
                matrix.rotateX((float)Math.toRadians((l.x+ticks*l.dx)%360)).rotateY((float)Math.toRadians((l.y+ticks*l.dy)%360)).rotateZ((float)Math.toRadians((l.z+ticks*l.dz)%360));
                // Same native position/UV/color shader; additive pipeline has no depth writes.
                pass.setPipeline(RenderSystem.getCompiledPipeline(i==0?RenderPipelines.END_SKY:RenderPipelines.GUI_NAUSEA_OVERLAY));
                pass.setUniform("DynamicTransforms",RenderSystem.getDynamicUniforms().writeTransform(matrix));
                var texture=Minecraft.getInstance().getTextureManager().getTexture(l.texture);
                pass.setUniform("Sampler0",texture.getTextureView(),texture.getSampler());
                pass.setVertexBuffer(0,l.vertices.slice());pass.setIndexBuffer(index,indices.type());pass.drawIndexed(l.indices,1,0,0,0);
            }
            var matrix=new Matrix4f(RenderSystem.getModelViewMatrixCopy()).translate((float)borderOffset.x,(float)borderOffset.y,(float)borderOffset.z);
            pass.setPipeline(RenderSystem.getCompiledPipeline(RenderPipelines.GUI_NAUSEA_OVERLAY));
            pass.setUniform("DynamicTransforms",RenderSystem.getDynamicUniforms().writeTransform(matrix));
            var noise=Minecraft.getInstance().getTextureManager().getTexture(texture("noise_tile"));
            pass.setUniform("Sampler0",noise.getTextureView(),noise.getSampler());
            pass.setVertexBuffer(0,borderFrames.get(Math.floorMod((int)(ticks/5),48)).slice());pass.setIndexBuffer(index,indices.type());pass.drawIndexed(24,1,0,0,0);
        }
    }
    @Override public void close(){for(var l:layers)l.vertices.close();layers.clear();for(var frame:borderFrames)frame.close();borderFrames.clear();}
}
