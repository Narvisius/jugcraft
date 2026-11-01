package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.menagerie.OwnerConfetti;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.ColorParticleOption;

/** The supplied paper sprites with drag, falling drift, spin and a landing fade. */
final class OwnerConfettiParticle extends SingleQuadParticle {
    private final float spin;private final double phase;
    private OwnerConfettiParticle(ClientLevel level,double x,double y,double z,double dx,double dy,double dz,TextureAtlasSprite sprite){
        super(level,x,y,z,dx,dy,dz,sprite);xd=dx;yd=dy;zd=dz;gravity=.2F;friction=.94F;lifetime=random.nextInt(400,700);quadSize=.125F;setSize(.001F,.001F);hasPhysics=true;
        roll=random.nextFloat()*6.283F;oRoll=roll;spin=(random.nextFloat()-.5F)*.25F;phase=random.nextDouble()*6.283;
    }
    @Override public void tick(){
        boolean landed=onGround||age>0&&x==xo&&y==yo&&z==zo;
        if(landed)age=Math.max(age,lifetime-20);
        else{xd+=Math.sin(age*.13+phase)*.004;zd+=Math.cos(age*.11+phase)*.004;oRoll=roll;roll+=spin;}
        super.tick();setAlpha(Math.clamp((lifetime-age)/20F,0,1));
    }
    @Override protected Layer getLayer(){return Layer.TRANSLUCENT;}
    static void register(){
        ParticleProviderRegistry.getInstance().register(OwnerConfetti.CONFETTI,sprites->(option,level,x,y,z,dx,dy,dz,r)->new OwnerConfettiParticle(level,x,y,z,dx,dy,dz,sprites.get(r)));
        ParticleProviderRegistry.getInstance().register(OwnerConfetti.DYED,sprites->(option,level,x,y,z,dx,dy,dz,r)->{var p=new OwnerConfettiParticle(level,x,y,z,dx,dy,dz,sprites.get(r));p.setColor(option.getRed(),option.getGreen(),option.getBlue());return p;});
        ClientPlayNetworking.registerGlobalReceiver(OwnerConfetti.Burst.TYPE,(packet,context)->{
            var level=context.client().level;if(level==null||!Double.isFinite(packet.pos().lengthSqr())||!Double.isFinite(packet.velocity().lengthSqr()))return;
            var random=level.getRandom();
            for(int i=0;i<Math.clamp(packet.count(),0,128);i++){
                double dx=random.nextGaussian()*.2,dy=random.nextDouble()*.4,dz=random.nextGaussian()*.2;
                if(!packet.radial()){dx+=packet.velocity().x*.8;dy+=packet.velocity().y*.8;dz+=packet.velocity().z*.8;}
                var particle=packet.colors().isEmpty()?OwnerConfetti.CONFETTI:ColorParticleOption.create(OwnerConfetti.DYED,packet.colors().get(i%packet.colors().size()));
                level.addParticle(particle,packet.pos().x,packet.pos().y,packet.pos().z,dx,dy,dz);
            }
        });
    }
}
