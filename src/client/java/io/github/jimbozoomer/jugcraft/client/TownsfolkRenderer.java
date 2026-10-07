package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.town.Townsfolk;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

/** Draws a townsperson with the skin the server names (one of the town's own; anything else gets the default). */
public class TownsfolkRenderer extends HumanoidMobRenderer<Townsfolk, TownsfolkRenderer.State, TownsfolkModel> {
	private static final Map<String, Identifier> TEXTURES = new HashMap<>();

	public static class State extends HumanoidRenderState {
		String skin = "townsfolk_1";
	}

	public TownsfolkRenderer(EntityRendererProvider.Context context) {
		super(context, new TownsfolkModel(context.bakeLayer(TownsfolkModel.LAYER)), 0.5F);
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(Townsfolk townsfolk, State state, float partialTick) {
		super.extractRenderState(townsfolk, state, partialTick);
		String skin = townsfolk.skin();
		state.skin = skin.matches("[a-z0-9_]{1,40}") ? skin : "townsfolk_1";
	}

	@Override
	public Identifier getTextureLocation(State state) {
		return TEXTURES.computeIfAbsent(state.skin, skin -> Jugcraft.id("textures/entity/townsfolk/" + skin + ".png"));
	}
}
