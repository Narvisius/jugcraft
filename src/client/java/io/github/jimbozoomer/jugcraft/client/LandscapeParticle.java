package io.github.jimbozoomer.jugcraft.client;

import net.fabricmc.fabric.api.client.particle.v1.FabricSpriteSet;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.SimpleParticleType;

/** Gentle drifting particles using the supplied landscape sprites. */
public final class LandscapeParticle extends SingleQuadParticle {
    private LandscapeParticle(ClientLevel l,double x,double y,double z,double dx,double dy,double dz,TextureAtlasSprite sprite){
        super(l,x,y,z,dx,dy,dz,sprite);quadSize=.04F+random.nextFloat()*.04F;lifetime=60+random.nextInt(60);gravity=0;friction=.98F;hasPhysics=false;yd=.008+dy;
    }
    @Override public void tick(){super.tick();setAlpha(Math.min(1,(lifetime-age)/20F));}
    @Override protected Layer getLayer(){return Layer.TRANSLUCENT;}
    public static ParticleProvider<SimpleParticleType> provider(FabricSpriteSet sprites){return (type,l,x,y,z,dx,dy,dz,r)->new LandscapeParticle(l,x,y,z,dx,dy,dz,sprites.get(r));}
}
