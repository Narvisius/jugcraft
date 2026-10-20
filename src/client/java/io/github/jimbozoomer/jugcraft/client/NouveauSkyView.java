package io.github.jimbozoomer.jugcraft.client;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.phys.Vec3;
/** Camera-direction sky windows share the live dimension's sky color, sun and moon phase. */
final class NouveauSkyView {
    record View(Vec3 eye,float red,float green,float blue,float sun,float stars,boolean end){}
    static View capture(Vec3 localEye){var sky=Minecraft.getInstance().gameRenderer.gameRenderState().levelRenderState.skyRenderState;var color=sky.skyColor;return new View(localEye,color==null?.4F:color.x(),color==null?.6F:color.y(),color==null?.9F:color.z(),sky.sunAngle,sky.starBrightness,sky.skybox!=null&&sky.skybox.toString().equals("END"));}
    static void draw(View sky,PoseStack pose,SubmitNodeCollector collector){
        collector.submitCustomGeometry(pose,RenderTypes.debugQuads(),(matrix,buffer)->{
            final int parts=8;for(int face=0;face<6;face++)for(int u=0;u<parts;u++)for(int v=0;v<parts;v++){
                double a=u/(double)parts,b=v/(double)parts,d=1.0/parts;double[][] corners={{a,b},{a,b+d},{a+d,b+d},{a+d,b}};
                for(var uv:corners){Vec3 point=switch(face){case 0->new Vec3(uv[0],uv[1],0);case 1->new Vec3(1-uv[0],uv[1],1);case 2->new Vec3(0,uv[1],1-uv[0]);case 3->new Vec3(1,uv[1],uv[0]);case 4->new Vec3(uv[0],1,1-uv[1]);default->new Vec3(uv[0],0,uv[1]);};
                    var ray=point.subtract(sky.eye).normalize();buffer.addVertex(matrix,(float)point.x,(float)point.y,(float)point.z).setColor(color(sky,ray));
                }
            }
        });
    }
    private static int color(View s,Vec3 ray){
        float fade=(float)Math.clamp((ray.y+.2)*.5,0,1);float r=s.red*(.8F+.2F*fade),g=s.green*(.8F+.2F*fade),b=s.blue*(.8F+.2F*fade);
        if(s.end){r=.12F;g=.08F;b=.18F;}
        else{double angle=Math.toRadians(s.sun),dot=ray.dot(new Vec3(Math.sin(angle),Math.cos(angle),0));if(dot>.998){r=1;g=.96F;b=.75F;}else if(dot<-.999&&s.stars>.05){r=.75F;g=.81F;b=.9F;}}
        if(s.stars>.1F||s.end){long x=(long)Math.floor(Math.atan2(ray.x,ray.z)*110),y=(long)Math.floor(Math.asin(ray.y)*110);long h=(x*3129871L^y*116129781L);h=h*h*42317861L+h*11;if((h&1023)<3){float star=s.end?.4F:Math.min(1,s.stars);r=Math.max(r,star);g=Math.max(g,star);b=Math.max(b,star);}}
        return 0xFF000000|Math.clamp((int)(r*255),0,255)<<16|Math.clamp((int)(g*255),0,255)<<8|Math.clamp((int)(b*255),0,255);
    }
}
