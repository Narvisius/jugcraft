package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.raiders.RaiderInfantry;
import java.util.Map;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

/** Draws raider infantry (batch 57) in their uniforms: textures/entity/raider/<kind>.png, by role. */
public class RaiderRenderer extends HumanoidMobRenderer<RaiderInfantry, RaiderRenderer.State, RaiderModel> {
	private static final Map<RaiderInfantry.Role, Identifier> TEXTURES = Map.of(
			RaiderInfantry.Role.GRUNT, Jugcraft.id("textures/entity/raider/raider_grunt.png"),
			RaiderInfantry.Role.GRENADIER, Jugcraft.id("textures/entity/raider/raider_grenadier.png"),
			RaiderInfantry.Role.OFFICER, Jugcraft.id("textures/entity/raider/raider_officer.png"),
			RaiderInfantry.Role.GUNNER, Jugcraft.id("textures/entity/raider/raider_gunner.png"));

	public static class State extends HumanoidRenderState {
		RaiderInfantry.Role role = RaiderInfantry.Role.GRUNT;
	}

	public RaiderRenderer(EntityRendererProvider.Context context) {
		super(context, new RaiderModel(context.bakeLayer(RaiderModel.LAYER)), 0.5F);
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(RaiderInfantry raider, State state, float partialTick) {
		super.extractRenderState(raider, state, partialTick);
		state.role = raider.role();
	}

	@Override
	public Identifier getTextureLocation(State state) {
		return TEXTURES.get(state.role);
	}
}
