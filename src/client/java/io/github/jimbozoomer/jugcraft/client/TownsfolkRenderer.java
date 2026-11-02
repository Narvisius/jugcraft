package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.town.CustomNpcs;
import io.github.jimbozoomer.jugcraft.town.TownRoutine;
import io.github.jimbozoomer.jugcraft.town.Townsfolk;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * Draws a townsperson with the skin the server names (one of the town's own; anything else gets the default), posed
 * for what they are doing at their place.
 */
public class TownsfolkRenderer extends HumanoidMobRenderer<Townsfolk, TownsfolkRenderer.State, TownsfolkModel> {
	private static final Map<String, Identifier> TEXTURES = new HashMap<>();

	public static class State extends HumanoidRenderState {
		String skin = "townsfolk_1";
		/** What they are doing at their place (TownRoutine), posed by TownsfolkModel. */
		TownRoutine.Activity activity = TownRoutine.Activity.NONE;
		/** Each person's own offset into their gestures, so a crowd does not move in step. */
		float phase;
		/** A custom NPC's player skin once loaded (NpcPlayerSkins), drawn instead of the town skin; slim arms if it has them. */
		@Nullable Identifier playerSkin;
		boolean slim;
        boolean seasonalTownResident;
	}

	public TownsfolkRenderer(EntityRendererProvider.Context context) {
		super(context, new TownsfolkModel(context.bakeLayer(TownsfolkModel.SKINNABLE_LAYER)), 0.5F);
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(Townsfolk townsfolk, State state, float partialTick) {
		super.extractRenderState(townsfolk, state, partialTick);
		String skin = townsfolk.skin();
        state.seasonalTownResident = !townsfolk.isCustomNpc();
		state.playerSkin = null;
		state.slim = false;
		if (skin.equals(CustomNpcs.PLAYER_SKIN)) {
			// A custom NPC in a player's skin: the town's default until (or unless) it loads.
			NpcPlayerSkins.extract(townsfolk, state);
			skin = "townsfolk_1";
		}
		state.skin = skin.matches("[a-z0-9_]{1,40}") ? skin : "townsfolk_1";
		state.activity = townsfolk.activity();
		state.phase = Math.floorMod(townsfolk.getId() * 37, 100) * 0.63F;
		if (townsfolk.isSleeping()) {
			// Lying in bed: no leg swing left over from the last steps to it.
			state.walkAnimationPos = 0.0F;
			state.walkAnimationSpeed = 0.0F;
			state.activity = TownRoutine.Activity.NONE;
		}
		// Gardeners and the florist kneel to their flowers.
		if (state.activity == TownRoutine.Activity.TEND) {
			state.isCrouching = true;
		}
	}

	@Override
	public Identifier getTextureLocation(State state) {
		if (state.playerSkin != null) {
			return state.playerSkin;
		}
		return TEXTURES.computeIfAbsent(state.skin, skin -> Jugcraft.id("textures/entity/townsfolk/" + skin + ".png"));
	}
}
