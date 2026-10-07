package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.HeadlessHorseman;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/** Draws the Headless Horseman, with his horse's eyes, the embers in his collar and his lantern glowing in the dark. */
public class HorsemanRenderer extends MobRenderer<HeadlessHorseman, LivingEntityRenderState, HorsemanModel> {
	private static final Identifier TEXTURE = Jugcraft.id("textures/entity/headless_horseman.png");
	private static final Identifier GLOW = Jugcraft.id("textures/entity/headless_horseman_glow.png");
	private static final RenderType GLOW_TYPE = RenderTypes.eyes(GLOW);

	public HorsemanRenderer(EntityRendererProvider.Context context) {
		super(context, new HorsemanModel(context.bakeLayer(HorsemanModel.LAYER)), 1.0F);
		addLayer(new EyesLayer<>(this) {
			@Override
			public RenderType renderType() {
				return GLOW_TYPE;
			}
		});
	}

	@Override
	public LivingEntityRenderState createRenderState() {
		return new LivingEntityRenderState();
	}

	@Override
	public Identifier getTextureLocation(LivingEntityRenderState state) {
		return TEXTURE;
	}
}
