package io.github.jimbozoomer.jugcraft.client.guns;

import net.fabricmc.fabric.api.client.particle.v1.FabricSpriteSet;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * A spent case (or shotgun shell) thrown from a gun's ejection port: the owner's casing art, small, tumbling as it
 * flies out to the side, falling, and lying where it lands for a couple of seconds. Client only, and never more than
 * the particle engine's own limit.
 */
public class GunCasingParticle extends SingleQuadParticle {
	private float spin;

	GunCasingParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, TextureAtlasSprite sprite) {
		super(level, x, y, z, xd, yd, zd, sprite);
		this.xd = xd;
		this.yd = yd;
		this.zd = zd;
		this.lifetime = 40 + random.nextInt(20);
		this.quadSize = 0.075F;
		this.gravity = 1.0F;
		this.friction = 0.98F;
		this.hasPhysics = true;
		this.roll = random.nextFloat() * (float) (Math.PI * 2);
		this.oRoll = roll;
		this.spin = (0.5F + random.nextFloat() * 0.4F) * (random.nextBoolean() ? 1.0F : -1.0F);
	}

	@Override
	public void tick() {
		oRoll = roll;
		super.tick();
		if (age > 1 && y == yo) {
			spin = 0.0F; // landed: it lies still
		}
		roll += spin;
	}

	@Override
	protected Layer getLayer() {
		return Layer.OPAQUE;
	}

	public static ParticleProvider<SimpleParticleType> provider(FabricSpriteSet sprites) {
		return (type, level, x, y, z, xd, yd, zd, random) -> new GunCasingParticle(level, x, y, z, xd, yd, zd, sprites.get(random));
	}
}
