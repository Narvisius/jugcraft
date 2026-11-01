package io.github.jimbozoomer.jugcraft.client;
import io.github.jimbozoomer.jugcraft.botany.*;
import java.util.WeakHashMap;
import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.world.phys.Vec3;
/** Original ten-wisp helical trail; client-only, once per visible relay tick. */
final class BotanicalLuminizerParticles extends SingleQuadParticle {
    private static final WeakHashMap<BotanicalWorkshop,Long> TICKS=new WeakHashMap<>();
    BotanicalLuminizerParticles(ClientLevel l,double x,double y,double z,double dx,double dy,double dz,SpriteSet sprites){super(l,x,y,z,sprites.first());xd=dx;yd=dy;zd=dz;gravity=0;friction=1;hasPhysics=false;quadSize=.05F;lifetime=10;setColor(.4F,.4F,1);}
    @Override public void tick(){super.tick();setAlpha(Math.clamp((lifetime-age)/10F,0,1));}
    @Override protected Layer getLayer(){return Layer.TRANSLUCENT;}
    @Override public int getLightCoords(float partial){return 15728880;}
    static void register(){ParticleProviderRegistry.getInstance().register(Jugtania.LUMINIZER_WISP,sprites->(type,l,x,y,z,dx,dy,dz,random)->new BotanicalLuminizerParticles(l,x,y,z,dx,dy,dz,sprites));}
    static void trail(BotanicalWorkshop host){
        var level=Minecraft.getInstance().level;var player=Minecraft.getInstance().player;
        if(level==null||player==null||host.luminizer.hidden()||player.distanceToSqr(Vec3.atCenterOf(host.getBlockPos()))>4096)return;
        var to=host.luminizer.visualLink();if(to==null||!level.hasChunkAt(to))return;
        long time=level.getGameTime();if(TICKS.getOrDefault(host,Long.MIN_VALUE)==time)return;TICKS.put(host,time);
        var start=Vec3.atCenterOf(host.getBlockPos());var vector=Vec3.atCenterOf(to).subtract(start);double length=vector.length();if(length<.1||length>20)return;
        var step=vector.normalize().scale(.1);int size=Math.max(1,(int)(length/.1)),begin=(int)Math.floorMod(time,size);var axis=step.normalize();
        var cross=step.cross(new Vec3(1,1,1));if(cross.lengthSqr()<1E-8)cross=step.cross(new Vec3(0,1,0));
        double radius=.5;
        for(int i=begin;i<begin+10&&i<size;i++){
            radius=Math.min(2,radius+.4);double rad=Math.PI/16*(i+time*.4);var base=cross.scale(radius);
            var spin=base.scale(Math.cos(rad)).add(axis.cross(base).scale(Math.sin(rad))).add(axis.scale(axis.dot(base)*(1-Math.cos(rad))));
            var at=start.add(step.scale(i)).add(spin);level.addParticle(Jugtania.LUMINIZER_WISP,at.x,at.y,at.z,-step.x,-step.y,-step.z);
        }
    }
}
