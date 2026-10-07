package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.agriculture.SpookySparkOptions;
import net.fabricmc.fabric.api.client.particle.v1.FabricSpriteSet;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.LightCoordsUtil;

/**
 * One spark of a spooky firework's picture: a bright point in its colour that flies out with the rest of the picture,
 * slows, droops a little, and fades; a twinkling spark flickers as it fades. Drawn at full brightness, as firework
 * sparks are.
 */
public class SpookySparkParticle extends SingleQuadParticle {
	private final boolean twinkle;
	private final float size;

	SpookySparkParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpookySparkOptions options,
			TextureAtlasSprite sprite) {
		super(level, x, y, z, xd, yd, zd, sprite);
		this.xd = xd;
		this.yd = yd;
		this.zd = zd;
		this.lifetime = 44 + random.nextInt(12);
		this.gravity = 0.012F;
		this.friction = 0.9F;
		this.hasPhysics = false;
		this.size = 0.16F + random.nextFloat() * 0.04F;
		this.quadSize = size;
		this.twinkle = options.twinkle();
		int colour = options.colour();
		setColor((colour >> 16 & 0xFF) / 255.0F, (colour >> 8 & 0xFF) / 255.0F, (colour & 0xFF) / 255.0F);
	}

	@Override
	public void tick() {
		super.tick();
		float t = age / (float) lifetime;
		float alpha = t < 0.65F ? 1.0F : 1.0F - (t - 0.65F) / 0.35F;
		if (twinkle && t > 0.3F && random.nextInt(3) == 0) {
			alpha *= 0.1F;
		}
		setAlpha(Math.max(0.0F, alpha));
		quadSize = size * (1.0F - 0.4F * t);
	}

	@Override
	protected int getLightCoords(float partialTick) {
		return LightCoordsUtil.FULL_BRIGHT;
	}

	@Override
	protected Layer getLayer() {
		return Layer.TRANSLUCENT;
	}

	public static ParticleProvider<SpookySparkOptions> provider(FabricSpriteSet sprites) {
		return (options, level, x, y, z, xd, yd, zd, random) -> new SpookySparkParticle(level, x, y, z, xd, yd, zd, options, sprites.get(random));
	}
}
