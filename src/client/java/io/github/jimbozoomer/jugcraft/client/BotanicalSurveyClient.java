package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.botany.BotanicalBuilding;
import java.util.*;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

/** A local construction overlay, not packets or thousands of persistent entities. */
final class BotanicalSurveyClient {
    private static List<BlockPos> visible=List.of();private static long refreshed=Long.MIN_VALUE;private static Object previousAnchor;private static int previousRadius;private static boolean previousSphere;
    static void register(){
        LevelRenderEvents.COLLECT_SUBMITS.register(ctx->{
            var c=Minecraft.getInstance();if(c.level==null||c.player==null)return;
            var ring=io.github.jimbozoomer.jugcraft.botany.BotanicalRelics.loki(c.player);if(ring.isEmpty())return;
            var origin=ring.get(io.github.jimbozoomer.jugcraft.botany.BotanicalRelics.ORIGIN);
            BlockPos center;
            if(origin!=null){if(!origin.dimension().equals(c.level.dimension()))return;center=origin.pos();}
            else {if(!c.player.isShiftKeyDown()||!(c.hitResult instanceof net.minecraft.world.phys.BlockHitResult hit))return;center=hit.getBlockPos();}
            var points=new ArrayList<BlockPos>();points.add(center);
            for(var offset:ring.getOrDefault(io.github.jimbozoomer.jugcraft.botany.BotanicalRelics.CURSORS,List.<BlockPos>of())){var at=center.offset(offset);if(at.distSqr(c.player.blockPosition())<=4096&&c.level.hasChunkAt(at))points.add(at);}
            var camera=ctx.levelState().cameraRenderState.pos;var pose=ctx.poseStack();pose.pushPose();pose.translate(-camera.x,-camera.y,-camera.z);
            ctx.submitNodeCollector().submitCustomGeometry(pose,RenderTypes.textSeeThrough(Jugcraft.id("textures/misc/blueprint_ghost.png")),(p,v)->{
                for(var at:points){float x=at.getX(),y=at.getY(),z=at.getZ(),t=.015F;int color=at.equals(center)?0xB0FFCF54:0x909C6DF2;
                    for(int a=0;a<=1;a++)for(int b=0;b<=1;b++){
                        LinePreview.cube(p,v,x-t,y+a-t,z+b-t,x+1+t,y+a+t,z+b+t,color);
                        LinePreview.cube(p,v,x+a-t,y-t,z+b-t,x+a+t,y+1+t,z+b+t,color);
                        LinePreview.cube(p,v,x+a-t,y+b-t,z-t,x+a+t,y+b+t,z+1+t,color);
                    }
                }
            });pose.popPose();
        });
        net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback.EVENT.register((stack,context,type,lines)->{if(stack.getOrDefault(io.github.jimbozoomer.jugcraft.botany.BotanicalIvy.KEEP,false))lines.add(net.minecraft.network.chat.Component.literal("Resolute Ivy · retained through one ordinary death"));});
        LevelRenderEvents.COLLECT_SUBMITS.register(ctx->{
        var c=Minecraft.getInstance();if(c.level==null||c.player==null){visible=List.of();return;}
        ItemStack tool=c.player.getMainHandItem().getItem() instanceof BotanicalBuilding.Sextant?c.player.getMainHandItem():c.player.getOffhandItem();
        if(!(tool.getItem() instanceof BotanicalBuilding.Sextant)){visible=List.of();return;}
        var anchor=tool.get(BotanicalBuilding.CENTER);if(anchor==null||!anchor.dimension().equals(c.level.dimension())){visible=List.of();return;}
        int radius=c.player.isUsingItem()&&c.player.getUseItem()==tool?BotanicalBuilding.Sextant.radius(tool,c.player):tool.getOrDefault(BotanicalBuilding.RADIUS,0);
        if(radius<=0){visible=List.of();return;}
        long now=c.level.getGameTime();boolean sphere=tool.getOrDefault(BotanicalBuilding.SPHERE,false),changed=!Objects.equals(previousAnchor,anchor)||previousRadius!=radius||previousSphere!=sphere;
        if(changed||now!=refreshed&& (now%4==0||visible.isEmpty()||now<refreshed)){
            previousAnchor=anchor;previousRadius=radius;previousSphere=sphere;
            refreshed=now;var positions=new HashSet<BlockPos>();var center=anchor.pos();var eye=c.player.blockPosition();
            if(!tool.getOrDefault(BotanicalBuilding.SPHERE,false)){
                for(int x=-radius;x<=radius;x++){int z=(int)Math.round(Math.sqrt((double)radius*radius-(double)x*x));positions.add(center.offset(x,0,z));positions.add(center.offset(x,0,-z));positions.add(center.offset(z,0,x));positions.add(center.offset(-z,0,x));}
            }else{
                // Project the shell onto three bounded planes around the viewer, rather than allocating an entire radius-256 sphere.
                for(int axis=0;axis<3;axis++)for(int a=-20;a<=20;a++)for(int b=-20;b<=20;b++){
                    int x=(axis==0?eye.getY()-center.getY():eye.getX()-center.getX())+a;
                    int y=(axis==2?eye.getY()-center.getY():eye.getZ()-center.getZ())+b;
                    double square=(double)radius*radius-(double)x*x-(double)y*y;if(square<0)continue;int z=(int)Math.round(Math.sqrt(square));
                    for(int sign:new int[]{-1,1})positions.add(axis==0?center.offset(z*sign,x,y):axis==1?center.offset(x,z*sign,y):center.offset(x,y,z*sign));
                }
            }
            visible=positions.stream().filter(at->at.distSqr(eye)<=900&&c.level.hasChunkAt(at)&&c.level.getBlockState(at).isAir()).sorted(Comparator.comparingDouble(eye::distSqr)).limit(768).toList();
        }
        if(visible.isEmpty())return;var captured=visible;var camera=ctx.levelState().cameraRenderState.pos;var pose=ctx.poseStack();pose.pushPose();pose.translate(-camera.x,-camera.y,-camera.z);
        ctx.submitNodeCollector().submitCustomGeometry(pose,RenderTypes.textSeeThrough(Jugcraft.id("textures/misc/blueprint_ghost.png")),(p,v)->{
            for(var at:captured){float x=at.getX(),y=at.getY(),z=at.getZ(),t=.012F;int color=0x704FE5DD;
                for(int a=0;a<=1;a++)for(int b=0;b<=1;b++){
                    LinePreview.cube(p,v,x-t,y+a-t,z+b-t,x+1+t,y+a+t,z+b+t,color);
                    LinePreview.cube(p,v,x+a-t,y-t,z+b-t,x+a+t,y+1+t,z+b+t,color);
                    LinePreview.cube(p,v,x+a-t,y+b-t,z-t,x+a+t,y+b+t,z+1+t,color);
                }
            }
        });pose.popPose();
    });}
}
