package io.github.jimbozoomer.jugcraft.client;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.nouveau.NouveauBubble;
import java.util.*;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;
/** The supplied bubble and five pop frames surround the actual moving target. */
public final class NouveauBubbleClient {
    private record Shell(NouveauBubble.Shell packet,long until){}
    private static final Map<UUID,Shell> SHELLS=new LinkedHashMap<>();
    private NouveauBubbleClient(){}
    public static void register(){
        ClientPlayConnectionEvents.DISCONNECT.register((h,c)->SHELLS.clear());
        ClientPlayNetworking.registerGlobalReceiver(NouveauBubble.Shell.TYPE,(packet,context)->{var level=context.client().level;if(level==null)return;if(SHELLS.size()>=128&&!SHELLS.containsKey(packet.uuid()))SHELLS.remove(SHELLS.keySet().iterator().next());SHELLS.put(packet.uuid(),new Shell(packet,level.getGameTime()+packet.duration()));});
        LevelRenderEvents.COLLECT_SUBMITS.register(ctx->{var client=Minecraft.getInstance();if(client.level==null)return;long now=client.level.getGameTime();SHELLS.entrySet().removeIf(e->e.getValue().until+5<now);var camera=ctx.levelState().cameraRenderState.pos;
            for(var shell:List.copyOf(SHELLS.values())){var entity=client.level.getEntity(shell.packet.entity());if(entity==null||!entity.getUUID().equals(shell.packet.uuid())||entity.distanceToSqr(camera)>4096)continue;
                Vec3 center=entity.position().add(0,entity.getBbHeight()/2,0),toward=camera.subtract(center).normalize();var right=toward.cross(new Vec3(0,1,0)).normalize();if(right.lengthSqr()<.1)right=new Vec3(1,0,0);var up=right.cross(toward).normalize();double size=Math.max(entity.getBbHeight(),entity.getBbWidth())*.65+.15;
                Vec3 r=right.scale(size),u=up.scale(size);var corners=List.of(center.subtract(r).subtract(u),center.add(r).subtract(u),center.add(r).add(u),center.subtract(r).add(u));
                int frame=shell.packet.popping()?(int)Math.clamp(6-(shell.until-now),1,5):now>=shell.until?(int)Math.clamp(now-shell.until+1,1,5):0;
                var pose=ctx.poseStack();pose.pushPose();pose.translate(-camera.x,-camera.y,-camera.z);String texture="textures/nouveau/entity/bubble"+(frame==0?"":"_pop"+frame)+".png";
                ctx.submitNodeCollector().submitCustomGeometry(pose,RenderTypes.entityTranslucentEmissive(Jugcraft.id(texture)),(matrix,buffer)->{for(int i=0;i<4;i++){var point=corners.get(i);buffer.addVertex(matrix,(float)point.x,(float)point.y,(float)point.z).setColor(0xDFFFFFFF).setUv(i==0||i==3?0:1,i<2?1:0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(matrix,0,0,1);}});pose.popPose();
            }
        });
    }
}
