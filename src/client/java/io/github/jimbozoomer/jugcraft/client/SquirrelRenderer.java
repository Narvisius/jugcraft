package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.Squirrel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;

/** Draws a squirrel ({@link SquirrelModel}), red or grey, a kit at half size, and the acorn it carries at its chin. */
public class SquirrelRenderer extends MobRenderer<Squirrel, SquirrelRenderer.State, SquirrelModel> {
	private static final Identifier RED = Jugcraft.id("textures/entity/squirrel_red.png");
	private static final Identifier GREY = Jugcraft.id("textures/entity/squirrel_grey.png");

	private final ItemModelResolver itemModels;

	public static class State extends LivingEntityRenderState {
		boolean grey;
		boolean carrying;
		final ItemStackRenderState acorn = new ItemStackRenderState();
	}

	public SquirrelRenderer(EntityRendererProvider.Context context) {
		super(context, new SquirrelModel(context.bakeLayer(SquirrelModel.LAYER)), 0.25F);
		itemModels = context.getItemModelResolver();
		addLayer(new AcornLayer(this));
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(Squirrel squirrel, State state, float partialTick) {
		super.extractRenderState(squirrel, state, partialTick);
		state.grey = squirrel.isGrey();
		state.carrying = squirrel.carrying();
		itemModels.updateForTopItem(state.acorn, squirrel.getMainHandItem(), ItemDisplayContext.GROUND, squirrel.level(), null, squirrel.getId());
	}

	@Override
	protected void scale(State state, PoseStack pose) {
		if (state.isBaby) {
			pose.scale(0.5F, 0.5F, 0.5F);
		}
	}

	@Override
	public Identifier getTextureLocation(State state) {
		return state.grey ? GREY : RED;
	}

	/** The acorn in its forepaws, just under its chin. */
	static class AcornLayer extends RenderLayer<State, SquirrelModel> {
		AcornLayer(RenderLayerParent<State, SquirrelModel> parent) {
			super(parent);
		}

		@Override
		public void submit(PoseStack pose, SubmitNodeCollector collector, int light, State state, float yRot, float xRot) {
			if (!state.carrying || state.acorn.isEmpty()) {
				return;
			}
			pose.pushPose();
			getParentModel().head().translateAndRotate(pose);
			pose.translate(0.0F, 0.12F, -0.3F);
			pose.scale(0.6F, 0.6F, 0.6F);
			state.acorn.submit(pose, collector, light, OverlayTexture.NO_OVERLAY, 0);
			pose.popPose();
		}
	}
}
