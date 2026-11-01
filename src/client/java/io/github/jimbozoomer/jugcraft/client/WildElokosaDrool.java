package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.wilds.WildElokosa;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.phys.Vec3;
import java.util.WeakHashMap;
import java.util.function.Supplier;

/** Source mouth anchors, twenty-tick attachment and eighty-tick pixel droplets. */
final class WildElokosaDrool extends SingleQuadParticle {
    static Supplier<TextureAtlasSprite> sprite;
    private static final WeakHashMap<WildElokosa,Mouth> MOUTHS=new WeakHashMap<>();
    static final class Mouth {final Vec3[] points=new Vec3[6];int tick=-1;}
    private final Mouth mouth;
    private final int anchor;
    private WildElokosaDrool(ClientLevel level,Mouth mouth,int anchor,TextureAtlasSprite sprite){
        super(level,mouth.points[anchor].x,mouth.points[anchor].y,mouth.points[anchor].z,0,0,0,sprite);
        this.mouth=mouth;this.anchor=anchor;xd=yd=zd=0;gravity=0;friction=1;
        lifetime=80;quadSize=0;setColor(1,1,1);setAlpha(.4F);
    }
    static Mouth capture(WildElokosa entity){
        Mouth mouth=MOUTHS.computeIfAbsent(entity,e->new Mouth());
        if(mouth.tick!=entity.tickCount){
            mouth.tick=entity.tickCount;
            // Off-screen actors do not emit; a first visible frame must have its real bone positions.
            if(entity.night()&&entity.isAlive()&&!entity.isInvisible()&&sprite!=null&&entity.getRandom().nextInt(15)==0){
                int index=entity.getRandom().nextInt(6);Vec3 point=mouth.points[index];
                if(point!=null&&point.distanceToSqr(entity.position())<16&&entity.level() instanceof ClientLevel level)
                    Minecraft.getInstance().particleEngine.add(new WildElokosaDrool(level,mouth,index,sprite.get()));
            }
        }
        return mouth;
    }
    @Override public void tick(){
        if(age<20){
            xo=x;yo=y;zo=z;
            Vec3 point=mouth.points[anchor];
            if(point==null||point.distanceToSqr(new Vec3(x,y,z))>16){remove();return;}
            setPos(point.x,point.y,point.z);age++;
        }else {if(age==20){yd=-.08;gravity=.5F;}super.tick();}
        quadSize=.2F*Math.min(1,age/10F);
    }
    @Override protected Layer getLayer(){return Layer.TRANSLUCENT;}
}
