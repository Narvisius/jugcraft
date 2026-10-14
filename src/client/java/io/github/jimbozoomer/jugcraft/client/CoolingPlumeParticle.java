package io.github.jimbozoomer.jugcraft.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

/**
 * One puff of a cooling tower's plume: a big soft white cloud that rises out of the tower, slows, drifts with the wind,
 * swells and thins out over ten seconds or so. Many together make the plume seen from across the map.
 */
public class CoolingPlumeParticle extends SingleQuadParticle {
	private final SpriteSet sprites;
	private final float startSize;

	CoolingPlumeParticle(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, SpriteSet sprites) {
		super(level, x, y, z, sprites.first());
		this.sprites = sprites;
		this.xd = dx;
		this.yd = dy;
		this.zd = dz;
		this.hasPhysics = false;
		this.gravity = 0;
		this.friction = 0.985F;
		this.lifetime = 160 + random.nextInt(100);
		this.startSize = 4.5F + random.nextFloat() * 3;
		this.quadSize = startSize;
		float shade = 0.9F + random.nextFloat() * 0.1F;
		setColor(shade, shade, shade);
		this.alpha = 0;
		setSpriteFromAge(sprites);
		this.roll = random.nextFloat() * (float) Math.PI * 2;
		this.oRoll = roll;
	}

	@Override
	public void tick() {
		super.tick();
		if (!isAlive()) {
			return;
		}
		float t = age / (float) lifetime;
		// Swells as it rises; fades in quickly and out slowly.
		quadSize = startSize * (1 + t * 2.4F);
		alpha = Math.min(1, age / 12F) * (1 - t) * 0.85F;
		// The rising column bends over with the wind.
		yd *= 0.992;
		xd += 0.0012;
		oRoll = roll;
		roll += 0.004F;
		setSpriteFromAge(sprites);
	}

	@Override
	protected SingleQuadParticle.Layer getLayer() {
		return SingleQuadParticle.Layer.TRANSLUCENT;
	}

	public static final class Provider implements ParticleProvider<SimpleParticleType> {
		private final SpriteSet sprites;

		public Provider(SpriteSet sprites) {
			this.sprites = sprites;
		}

		@Override
		public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double dx, double dy, double dz,
				RandomSource random) {
			return new CoolingPlumeParticle(level, x, y, z, dx, dy, dz, sprites);
		}
	}
}
