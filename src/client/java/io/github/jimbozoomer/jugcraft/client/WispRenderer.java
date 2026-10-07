package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.WillOWisp;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

/** Draws a will-o'-wisp at full brightness, however dark it is around it; it casts no shadow. */
public class WispRenderer extends MobRenderer<WillOWisp, LivingEntityRenderState, WispModel> {
	private static final Identifier TEXTURE = Jugcraft.id("textures/entity/will_o_wisp.png");

	public WispRenderer(EntityRendererProvider.Context context) {
		super(context, new WispModel(context.bakeLayer(WispModel.LAYER)), 0.0F);
	}

	@Override
	public LivingEntityRenderState createRenderState() {
		return new LivingEntityRenderState();
	}

	@Override
	public Identifier getTextureLocation(LivingEntityRenderState state) {
		return TEXTURE;
	}

	@Override
	protected int getBlockLightLevel(WillOWisp wisp, BlockPos pos) {
		return 15;
	}
}
