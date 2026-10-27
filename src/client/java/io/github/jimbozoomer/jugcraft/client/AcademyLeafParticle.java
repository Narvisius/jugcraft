package io.github.jimbozoomer.jugcraft.client;

import net.fabricmc.fabric.api.client.particle.v1.FabricSpriteSet;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.SimpleParticleType;

/** A slow, drifting local leaf using Jugcraft's existing small leaf silhouette. */
public final class AcademyLeafParticle extends SingleQuadParticle {
	private final double drift;
	private final float spin;

	private AcademyLeafParticle(ClientLevel level, double x, double y, double z,
			double xd, double yd, double zd, TextureAtlasSprite sprite) {
		super(level, x, y, z, xd, yd, zd, sprite);
		this.xd = xd;
		this.yd = -0.02 - random.nextDouble() * 0.018;
		this.zd = zd;
		this.lifetime = 90 + random.nextInt(60);
		this.quadSize = 0.6F + random.nextFloat() * 0.18F;
		this.gravity = 0.001F;
		this.friction = 0.99F;
		this.hasPhysics = false;
		this.drift = random.nextDouble() * Math.PI * 2;
		this.spin = (random.nextBoolean() ? 1 : -1) * (0.012F + random.nextFloat() * 0.02F);
		int colour = switch (random.nextInt(3)) {
			case 0 -> 0xB94A34;
			case 1 -> 0xD08035;
			default -> 0xD5AE58;
		};
		setColor((colour >> 16 & 255) / 255.0F, (colour >> 8 & 255) / 255.0F, (colour & 255) / 255.0F);
		this.roll = random.nextFloat() * (float) (Math.PI * 2);
		this.oRoll = roll;
	}

	@Override
	public void tick() {
		super.tick();
		xd += Math.sin(age * 0.09 + drift) * 0.0009;
		zd += Math.cos(age * 0.07 + drift) * 0.0009;
		if (yd < -0.045) yd = -0.045;
		oRoll = roll;
		roll += spin;
		setAlpha(Math.max(0.0F, Math.min(1.0F, (lifetime - age) / 18.0F)));
	}

	@Override
	protected Layer getLayer() {
		return Layer.TRANSLUCENT;
	}

	public static ParticleProvider<SimpleParticleType> provider(FabricSpriteSet sprites) {
		return (type, level, x, y, z, xd, yd, zd, random) ->
				new AcademyLeafParticle(level, x, y, z, xd, yd, zd, sprites.get(random));
	}
}
