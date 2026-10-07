package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.Werewolf;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/**
 * Draws a werewolf ({@link WerewolfModel}) in its kind's fur (textures/entity/werewolf_&lt;kind&gt;.png) with its eyes
 * glowing in the dark (werewolf_&lt;kind&gt;_eyes.png: red for the brown and the snow werewolf, amber for the shadow
 * werewolf), hunched lower with its jaws open while it hunts. The shadow werewolf's size comes from its scale attribute.
 */
public class WerewolfRenderer extends MobRenderer<Werewolf, WerewolfRenderer.State, WerewolfModel> {
	private static final Map<Werewolf.Kind, Identifier> TEXTURES = new EnumMap<>(Werewolf.Kind.class);

	static {
		for (Werewolf.Kind kind : Werewolf.Kind.values()) {
			TEXTURES.put(kind, Jugcraft.id("textures/entity/werewolf_" + kind.id + ".png"));
		}
	}

	public static class State extends LivingEntityRenderState {
		boolean hunting;
		Werewolf.Kind kind = Werewolf.Kind.BROWN;
	}

	public WerewolfRenderer(EntityRendererProvider.Context context) {
		super(context, new WerewolfModel(context.bakeLayer(WerewolfModel.LAYER)), 0.8F);
		for (Werewolf.Kind kind : Werewolf.Kind.values()) {
			RenderType eyes = RenderTypes.eyes(Jugcraft.id("textures/entity/werewolf_" + kind.id + "_eyes.png"));
			addLayer(new EyesLayer<>(this) {
				@Override
				public RenderType renderType() {
					return eyes;
				}

				@Override
				public void submit(PoseStack pose, SubmitNodeCollector collector, int light, State state, float yRot, float xRot) {
					if (state.kind == kind) {
						super.submit(pose, collector, light, state, yRot, xRot);
					}
				}
			});
		}
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(Werewolf werewolf, State state, float partialTick) {
		super.extractRenderState(werewolf, state, partialTick);
		state.hunting = werewolf.isAggressive();
		state.kind = werewolf.kind();
	}

	@Override
	public Identifier getTextureLocation(State state) {
		return TEXTURES.get(state.kind);
	}
}
