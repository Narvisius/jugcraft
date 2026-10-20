package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.nouveau.NouveauResources;
import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

/** Bounded, fading particles using the owner's exact particle sprites. */
public final class NouveauParticle extends SingleQuadParticle {
    private final String kind;
    NouveauParticle(ClientLevel l,double x,double y,double z,double dx,double dy,double dz,TextureAtlasSprite sprite,String kind){
        super(l,x,y,z,dx,dy,dz,sprite);this.kind=kind;this.xd=dx;this.yd=dy;this.zd=dz;hasPhysics=false;friction=.96F;
        lifetime=kind.equals("smoke")?50:kind.equals("leaf")?70:24;quadSize=kind.contains("circle")?.4F:kind.equals("smoke")?.25F:.09F;
        if(kind.equals("leaf")||kind.equals("snow")){gravity=.03F;roll=random.nextFloat()*6.28F;oRoll=roll;}
        else if(!kind.equals("smoke"))setColor(.72F,.55F,1);
    }
    @Override public void tick(){super.tick();setAlpha(Math.min(1F,(lifetime-age)/8F));if(kind.equals("helix")){double a=age*.4;xd+=Math.cos(a)*.003;zd+=Math.sin(a)*.003;}if(kind.equals("leaf")){oRoll=roll;roll+=.04F;}}
    @Override protected Layer getLayer(){return Layer.TRANSLUCENT;}
    @Override protected int getLightCoords(float partial){return kind.equals("leaf")||kind.equals("smoke")?super.getLightCoords(partial):0xf000f0;}
    public static void register(){NouveauResources.PARTICLES.forEach((kind,type)->ParticleProviderRegistry.getInstance().register(type,sprites->(options,level,x,y,z,dx,dy,dz,random)->new NouveauParticle(level,x,y,z,dx,dy,dz,sprites.get(random),kind)));}
}
