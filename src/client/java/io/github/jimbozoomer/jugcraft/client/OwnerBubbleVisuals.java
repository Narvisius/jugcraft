package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.menagerie.OwnerBubbles;
import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;

/** Original eight-corner bubble wobble, palette strip and seven-frame pop artwork. */
final class OwnerBubbleVisuals {
    private static final int[] PALETTE={0xD3A4F7,0xF3C1F0,0xD3A4F7,0xA2C0F8,0xA2F8DF,0xA2C0F8};
    private static int color(float time){float v=(time-(float)Math.floor(time))*6;int i=(int)v;float f=v-i;int a=PALETTE[i%6],b=PALETTE[(i+1)%6];return 0xFF000000|((int)(((a>>16)&255)*(1-f)+((b>>16)&255)*f)<<16)|((int)(((a>>8)&255)*(1-f)+((b>>8)&255)*f)<<8)|(int)((a&255)*(1-f)+(b&255)*f);}
    static void draw(OwnerSupplementRenderer.State s,PoseStack pose,SubmitNodeCollector out){
        float time=s.time/100+s.bubblePhase,scale=s.bubbleScale;
        // NW, NE, SE, SW on upper and lower planes, matching the supplied mesh.
        float[][] signs={{-1,1,-1},{1,1,-1},{1,1,1},{-1,1,1},{-1,-1,-1},{1,-1,-1},{1,-1,1},{-1,-1,1}};
        float[] phase={0,.25F,.5F,.75F,.5F,.75F,0,.25F},colors={0,.15F,.55F,.35F,.45F,.85F,1,.65F};
        float[][] corners=new float[8][3];int[] tint=new int[8];for(int i=0;i<8;i++){float half=.48F+.02F*(float)Math.cos(2*Math.PI*(time+phase[i]));for(int j=0;j<3;j++)corners[i][j]=signs[i][j]*half;tint[i]=color(time+colors[i]);}
        int[][] faces={{3,2,1,0},{4,5,6,7},{5,4,0,1},{4,7,3,0},{7,6,2,3},{6,5,1,2}};
        float[][] normals={{0,1,0},{0,-1,0},{0,0,-1},{-1,0,0},{0,0,1},{1,0,0}},uv={{0,1},{1,1},{1,0},{0,0}};
        pose.pushPose();pose.translate(.5,.5,.5);pose.scale(scale,scale,scale);
        out.submitCustomGeometry(pose,RenderTypes.entityTranslucentCull(Jugcraft.id("textures/newest/supps/block/bubble_block.png")),(matrix,buffer)->{
            for(int face=0;face<6;face++)for(int v=0;v<4;v++){int i=faces[face][v];buffer.addVertex(matrix,corners[i][0],corners[i][1],corners[i][2]).setColor(tint[i]).setUv(uv[v][0],uv[v][1]).setOverlay(OverlayTexture.NO_OVERLAY).setLight(s.lightCoords).setNormal(matrix,normals[face][0],normals[face][1],normals[face][2]);}
        });pose.popPose();
    }
    private static final class BubbleParticle extends SingleQuadParticle {
        final SpriteSet sprites;final boolean pop;
        BubbleParticle(ClientLevel level,double x,double y,double z,double dx,double dy,double dz,SpriteSet sprites,boolean pop){super(level,x,y,z,sprites.first());this.sprites=sprites;this.pop=pop;xd=dx;yd=dy;zd=dz;hasPhysics=!pop;gravity=pop?0:-.015F;friction=.96F;lifetime=pop?14:40+random.nextInt(40);quadSize=pop?.72F:.06F+random.nextFloat()*.06F;if(pop)setSpriteFromAge(sprites);else setSprite(sprites.get(random));}
        @Override public void tick(){super.tick();if(pop)setSpriteFromAge(sprites);else setAlpha(Math.clamp((lifetime-age)/10F,0,1));}
        @Override protected Layer getLayer(){return Layer.TRANSLUCENT;}
    }
    static void register(){ParticleProviderRegistry.getInstance().register(OwnerBubbles.SUDS,sprites->(type,level,x,y,z,dx,dy,dz,r)->new BubbleParticle(level,x,y,z,dx,dy,dz,sprites,false));ParticleProviderRegistry.getInstance().register(OwnerBubbles.POP,sprites->(type,level,x,y,z,dx,dy,dz,r)->new BubbleParticle(level,x,y,z,dx,dy,dz,sprites,true));}
}
