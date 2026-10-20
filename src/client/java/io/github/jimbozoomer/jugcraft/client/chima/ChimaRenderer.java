package io.github.jimbozoomer.jugcraft.client.chima;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.creatures.chima.Chima;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

/**
 * Draws Chima ({@link ChimaModel}) in its look (its own, pumpkin, zombie, skeleton or shark), wild or tamed with its red
 * collar on, sitting when told to.
 */
public class ChimaRenderer extends MobRenderer<Chima, ChimaRenderer.State, ChimaModel> {
	public static final float MODEL_SCALE = 0.6F;
	private static final Identifier[] WILD = textures("");
	private static final Identifier[] TAME = textures("_tame");

	public static class State extends LivingEntityRenderState {
		boolean tame;
		boolean sitting;
		Chima.Skin skin = Chima.Skin.NORMAL;
	}

	public ChimaRenderer(EntityRendererProvider.Context context) {
		super(context, new ChimaModel(context.bakeLayer(ChimaModel.LAYER)), 0.4F);
	}

	private static Identifier[] textures(String suffix) {
		Chima.Skin[] skins = Chima.Skin.values();
		Identifier[] out = new Identifier[skins.length];
		for (Chima.Skin skin : skins) {
			out[skin.ordinal()] = Jugcraft.id("textures/entity/chima_" + skin.id() + suffix + ".png");
		}
		return out;
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(Chima chima, State state, float partialTick) {
		super.extractRenderState(chima, state, partialTick);
		state.tame = chima.tame();
		state.sitting = chima.sitting();
		state.skin = chima.skin();
	}

	/** The model is drawn at three fifths: with its great head, a cub not quite a block tall. */
	@Override
	protected void scale(State state, PoseStack pose) {
		pose.scale(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE);
	}

	@Override
	public Identifier getTextureLocation(State state) {
		return (state.tame ? TAME : WILD)[state.skin.ordinal()];
	}
}
