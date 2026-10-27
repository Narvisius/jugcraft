package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.creatures.herobrine.Herobrine;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.Identifier;

/**
 * Draws Herobrine (docs/features/herobrine.md) as the game's own default player, read from the player's installed game
 * rather than copied into Jugcraft, with glowing white eyes on top: the eyes texture (tools/herobrine.py) is clear but
 * for the four eye pixels, and is drawn full-bright, so they shine in the dark.
 */
public class HerobrineRenderer extends HumanoidMobRenderer<Herobrine, TownsfolkRenderer.State, TownsfolkModel> {
	public HerobrineRenderer(EntityRendererProvider.Context context) {
		super(context, new TownsfolkModel(context.bakeLayer(TownsfolkModel.LAYER)), 0.5F);
		RenderType eyes = RenderTypes.eyes(Jugcraft.id("textures/entity/herobrine_eyes.png"));
		addLayer(new EyesLayer<>(this) {
			@Override
			public RenderType renderType() {
				return eyes;
			}
		});
	}

	@Override
	public TownsfolkRenderer.State createRenderState() {
		return new TownsfolkRenderer.State();
	}

	@Override
	public Identifier getTextureLocation(TownsfolkRenderer.State state) {
		return DefaultPlayerSkin.getDefaultTexture();
	}
}
