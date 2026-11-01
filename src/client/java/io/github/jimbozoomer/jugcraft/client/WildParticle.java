package io.github.jimbozoomer.jugcraft.client;
import io.github.jimbozoomer.jugcraft.wilds.WildParticles;
import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
/** Supplied sprites, bounded native animation; no dependency on the old particle engine. */
final class WildParticle extends SingleQuadParticle {
    private final String kind;private final float initialSize;
    WildParticle(ClientLevel l,double x,double y,double z,double dx,double dy,double dz,TextureAtlasSprite sprite,String kind){super(l,x,y,z,dx,dy,dz,sprite);this.kind=kind;xd=dx;yd=dy;zd=dz;hasPhysics=false;friction=.94F;lifetime=kind.startsWith("moon_")?25:kind.equals("snowflake")?30:20;initialSize=kind.startsWith("moon_")?.6F:kind.equals("glow")?.25F:.045F;quadSize=initialSize;if(kind.equals("pixel"))setColor(.85F,.65F,.99F);if(kind.equals("snowflake")){gravity=.015F;roll=random.nextFloat()*6.28F;oRoll=roll;}setAlpha(0);}
    @Override public void tick(){super.tick();float t=age/(float)lifetime;if(kind.startsWith("moon_")){setAlpha(Math.clamp(Math.min(t/.15F,(1-t)/.5F),0,1));quadSize=initialSize*(1+(1-(float)Math.pow(t,.6))*(float)Math.sqrt(t)*7);}else {setAlpha(Math.clamp(Math.min(age/3F,(lifetime-age)/7F),0,1));quadSize=initialSize*(1-t*t);if(kind.equals("snowflake")){oRoll=roll;roll+=.045F;}}}
    @Override protected Layer getLayer(){return Layer.TRANSLUCENT;}
    @Override protected int getLightCoords(float partial){return 0xF000F0;}
    static void register(){WildParticles.TYPES.forEach((kind,type)->ParticleProviderRegistry.getInstance().register(type,sprites->{if(kind.equals("pixel"))WildElokosaDrool.sprite=()->sprites.get(net.minecraft.util.RandomSource.create());return (options,level,x,y,z,dx,dy,dz,random)->new WildParticle(level,x,y,z,dx,dy,dz,sprites.get(random),kind);}));}
}
