package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.Turkey;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

/**
 * Draws a wild turkey ({@link TurkeyModel}): a bronze tom, a brown hen or a buff poult (half size), and a tom's tail as
 * far fanned as his strut has opened it.
 */
public class TurkeyRenderer extends MobRenderer<Turkey, TurkeyRenderer.State, TurkeyModel> {
	private static final Identifier TOM = Jugcraft.id("textures/entity/turkey_tom.png");
	private static final Identifier HEN = Jugcraft.id("textures/entity/turkey_hen.png");
	private static final Identifier POULT = Jugcraft.id("textures/entity/turkey_poult.png");

	public static class State extends LivingEntityRenderState {
		boolean tom;
		float fan;
	}

	public TurkeyRenderer(EntityRendererProvider.Context context) {
		super(context, new TurkeyModel(context.bakeLayer(TurkeyModel.LAYER)), 0.4F);
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(Turkey turkey, State state, float partialTick) {
		super.extractRenderState(turkey, state, partialTick);
		state.tom = turkey.isTom() && !turkey.isBaby();
		state.fan = turkey.fan(partialTick);
	}

	@Override
	protected void scale(State state, PoseStack pose) {
		if (state.isBaby) {
			pose.scale(0.5F, 0.5F, 0.5F);
		}
	}

	@Override
	public Identifier getTextureLocation(State state) {
		return state.isBaby ? POULT : state.tom ? TOM : HEN;
	}
}
