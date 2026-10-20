package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.nouveau.NouveauScrySight;
import java.util.*;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;

/** Through-wall spectral markers reuse vanilla's see-through textured rendering pipeline. */
public final class NouveauScrySightClient {
    private static List<BlockPos> marks=List.of();private static long until;
    private NouveauScrySightClient(){}
    public static void register(){
        ClientPlayNetworking.registerGlobalReceiver(NouveauScrySight.Marks.TYPE,(packet,context)->{var c=context.client();if(c.level!=null&&c.level.dimension().identifier().equals(packet.dimension())){marks=packet.blocks();until=c.level.getGameTime()+40;}});
        ClientPlayConnectionEvents.DISCONNECT.register((h,c)->marks=List.of());
        LevelRenderEvents.COLLECT_SUBMITS.register(ctx->{var c=Minecraft.getInstance();if(c.level==null||c.player==null||c.level.getGameTime()>until||marks.isEmpty())return;
            var captured=marks;var camera=ctx.levelState().cameraRenderState.pos;var pose=ctx.poseStack();pose.pushPose();pose.translate(-camera.x,-camera.y,-camera.z);
            ctx.submitNodeCollector().submitCustomGeometry(pose,RenderTypes.textSeeThrough(Jugcraft.id("textures/misc/blueprint_ghost.png")),(p,v)->{
                for(var at:captured){double distance=c.player.blockPosition().distSqr(at);int color=distance<64?0x90E8F5FF:distance<256?0x806FE5A3:0x706D9BFF;
                    float x=at.getX(),y=at.getY(),z=at.getZ(),t=.025F;
                    for(int a=0;a<=1;a++)for(int b=0;b<=1;b++){
                        LinePreview.cube(p,v,x-t,y+a-t,z+b-t,x+1+t,y+a+t,z+b+t,color);
                        LinePreview.cube(p,v,x+a-t,y-t,z+b-t,x+a+t,y+1+t,z+b+t,color);
                        LinePreview.cube(p,v,x+a-t,y+b-t,z-t,x+a+t,y+b+t,z+1+t,color);
                    }
                }
            });pose.popPose();
        });
    }
}
