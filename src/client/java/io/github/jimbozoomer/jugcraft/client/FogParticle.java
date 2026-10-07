package io.github.jimbozoomer.jugcraft.client;

import net.fabricmc.fabric.api.client.particle.v1.FabricSpriteSet;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * A puff of the Fog Machine's ground fog: a big soft pale quad that drifts slowly outward along the ground, fades in,
 * hangs, and fades out over four to six seconds. It never rises or falls and collides with the ground.
 */
public class FogParticle extends SingleQuadParticle {
	private static final float MAX_ALPHA = 0.42F;

	FogParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, TextureAtlasSprite sprite) {
		super(level, x, y, z, xd, yd, zd, sprite);
		this.xd = xd;
		this.yd = 0.0;
		this.zd = zd;
		this.lifetime = 80 + random.nextInt(40);
		this.quadSize = 0.7F + random.nextFloat() * 0.5F;
		this.gravity = 0.0F;
		this.friction = 0.97F;
		this.hasPhysics = true;
		this.roll = random.nextFloat() * (float) (Math.PI * 2);
		this.oRoll = roll;
		float shade = 0.9F + random.nextFloat() * 0.08F;
		setColor(shade, shade, Math.min(1.0F, shade + 0.03F));
		setAlpha(0.0F);
	}

	@Override
	public void tick() {
		super.tick();
		yd = 0.0;
		float t = age / (float) lifetime;
		setAlpha(MAX_ALPHA * Math.min(1.0F, t * 5.0F) * Math.min(1.0F, (1.0F - t) * 3.0F));
	}

	@Override
	protected Layer getLayer() {
		return Layer.TRANSLUCENT;
	}

	public static ParticleProvider<SimpleParticleType> provider(FabricSpriteSet sprites) {
		return (type, level, x, y, z, xd, yd, zd, random) -> new FogParticle(level, x, y, z, xd, yd, zd, sprites.get(random));
	}
}
