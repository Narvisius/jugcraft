package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.menagerie.OwnerAmendmentCharge;
import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;

/** Supplied charge trail frames and three original explosion sequences. */
final class OwnerAmendmentParticle extends SingleQuadParticle {
    private final SpriteSet sprites;private final boolean explosion;private final int variant;
    private OwnerAmendmentParticle(ClientLevel level,double x,double y,double z,double dx,double dy,double dz,SpriteSet sprites,boolean explosion,boolean bubble){
        super(level,x,y,z,sprites.first());this.sprites=sprites;this.explosion=explosion;variant=random.nextInt(3);xd=dx;yd=dy;zd=dz;hasPhysics=false;gravity=0;friction=.9F;lifetime=explosion?10:20;quadSize=explosion?1.2F:bubble?.07F:.18F;if(bubble)lifetime=12;roll=random.nextFloat()*6.283F;oRoll=roll;frame();
    }
    private void frame(){if(explosion)setSprite(sprites.get(variant*5+Math.min(4,age/2),14));else setSpriteFromAge(sprites);}
    @Override public void tick(){super.tick();if(isAlive()){frame();setAlpha(Math.clamp((lifetime-age)/6F,0,1));}}
    @Override protected Layer getLayer(){return Layer.TRANSLUCENT;}
    static void register(){
        for(var entry:OwnerAmendmentCharge.PARTICLES.entrySet())ParticleProviderRegistry.getInstance().register(entry.getValue(),sprites->(type,level,x,y,z,dx,dy,dz,random)->new OwnerAmendmentParticle(level,x,y,z,dx,dy,dz,sprites,entry.getKey().equals("fireball_explosion"),entry.getKey().equals("boiling_bubble")));
    }
}
