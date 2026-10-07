package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.Crow;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

/** Draws a crow ({@link CrowModel}), its wings beating in flight and folded while it pecks. */
public class CrowRenderer extends MobRenderer<Crow, CrowRenderer.State, CrowModel> {
	private static final Identifier TEXTURE = Jugcraft.id("textures/entity/crow.png");

	public static class State extends LivingEntityRenderState {
		boolean pecking;
	}

	public CrowRenderer(EntityRendererProvider.Context context) {
		super(context, new CrowModel(context.bakeLayer(CrowModel.LAYER)), 0.25F);
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(Crow crow, State state, float partialTick) {
		super.extractRenderState(crow, state, partialTick);
		state.pecking = crow.pecking();
	}

	@Override
	public Identifier getTextureLocation(State state) {
		return TEXTURE;
	}
}
