package io.github.jimbozoomer.jugcraft.client;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
/** Spherified cube with the supplied cube-map UV layout; ordinary entity pipeline remains shader compatible. */
final class OwnerRoundGlobe {
 private record Face(Vec3 normal,Vec3 u,Vec3 v,int x,int y){}
 private static final Face[] FACES={
  new Face(new Vec3(-1,0,0),new Vec3(0,0,1),new Vec3(0,1,0),0,8),
  new Face(new Vec3(0,0,-1),new Vec3(-1,0,0),new Vec3(0,1,0),8,8),
  new Face(new Vec3(1,0,0),new Vec3(0,0,-1),new Vec3(0,1,0),16,8),
  new Face(new Vec3(0,0,1),new Vec3(1,0,0),new Vec3(0,1,0),24,8),
  new Face(new Vec3(0,-1,0),new Vec3(1,0,0),new Vec3(0,0,1),8,0),
  new Face(new Vec3(0,1,0),new Vec3(1,0,0),new Vec3(0,0,-1),16,0)
 };
 static void draw(Identifier texture,int light,PoseStack pose,SubmitNodeCollector out){
  out.submitCustomGeometry(pose,RenderTypes.entityCutout(texture),(matrix,consumer)->{
   for(var face:FACES)for(int y=0;y<8;y++)for(int x=0;x<8;x++){
    vertex(matrix,consumer,face,x,y,light);vertex(matrix,consumer,face,x+1,y,light);vertex(matrix,consumer,face,x+1,y+1,light);vertex(matrix,consumer,face,x,y+1,light);
   }
  });
 }
 private static void vertex(PoseStack.Pose pose,VertexConsumer out,Face f,int x,int y,int light){
  var normal=f.normal.add(f.u.scale(x/4D-1)).add(f.v.scale(y/4D-1)).normalize();
  out.addVertex(pose,(float)normal.x*.25F,(float)normal.y*.25F,(float)normal.z*.25F).setColor(-1).setUv((f.x+x)/32F,(f.y+y)/16F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose,(float)normal.x,(float)normal.y,(float)normal.z);
 }
}
