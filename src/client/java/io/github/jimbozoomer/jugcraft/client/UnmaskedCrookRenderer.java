package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.creatures.scary.UnmaskedCrook;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.Identifier;

/** Draws an unmasked crook as a townsperson, in the town skin the server names (docs/features/costumed-villains-unmasking.md). */
public class UnmaskedCrookRenderer extends HumanoidMobRenderer<UnmaskedCrook, TownsfolkRenderer.State, TownsfolkModel> {
	public UnmaskedCrookRenderer(EntityRendererProvider.Context context) {
		super(context, new TownsfolkModel(context.bakeLayer(TownsfolkModel.LAYER)), 0.5F);
	}

	@Override
	public TownsfolkRenderer.State createRenderState() {
		return new TownsfolkRenderer.State();
	}

	@Override
	public void extractRenderState(UnmaskedCrook crook, TownsfolkRenderer.State state, float partialTick) {
		super.extractRenderState(crook, state, partialTick);
		String skin = crook.skin();
		state.skin = skin.matches("[a-z0-9_]{1,40}") ? skin : "townsfolk_1";
	}

	@Override
	public Identifier getTextureLocation(TownsfolkRenderer.State state) {
		return Jugcraft.id("textures/entity/townsfolk/" + state.skin + ".png");
	}
}
