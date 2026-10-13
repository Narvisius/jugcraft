package io.github.jimbozoomer.jugcraft.client.guns;

import net.fabricmc.fabric.api.client.particle.v1.FabricSpriteSet;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.LightCoordsUtil;

/**
 * The Laser Sight's dot (slice 9E): the owner's red dot (the Reflex Sight's, a 2 x 2 dot in the middle of its 16 x 16
 * texture) where a laser-sighted gun points. It stays still, at full brightness, for two ticks; {@link GunLaser} draws
 * a new one each tick, so it follows the aim without a trail. Its size comes in as the first of the particle's speeds
 * (the quad's half width, in blocks), so that the dot keeps about the same size on the screen however far it is.
 */
public class GunLaserParticle extends SingleQuadParticle {
	/** How long each dot lasts, in ticks. */
	static final int LIFETIME = 2;

	GunLaserParticle(ClientLevel level, double x, double y, double z, double size, TextureAtlasSprite sprite) {
		super(level, x, y, z, 0.0, 0.0, 0.0, sprite);
		this.xd = 0.0;
		this.yd = 0.0;
		this.zd = 0.0;
		this.lifetime = LIFETIME;
		this.quadSize = (float) size;
		this.gravity = 0.0F;
		this.hasPhysics = false;
	}

	@Override
	protected int getLightCoords(float partialTick) {
		return LightCoordsUtil.FULL_BRIGHT;
	}

	@Override
	protected Layer getLayer() {
		return Layer.TRANSLUCENT;
	}

	public static ParticleProvider<SimpleParticleType> provider(FabricSpriteSet sprites) {
		return (type, level, x, y, z, xd, yd, zd, random) -> new GunLaserParticle(level, x, y, z, xd, sprites.get(random));
	}
}
