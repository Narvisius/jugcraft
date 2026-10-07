package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.styx.Styxhexenhammer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

public final class StyxRenderer extends HumanoidMobRenderer<Styxhexenhammer,StyxRenderer.State,StyxModel> {
	public static class State extends HumanoidRenderState { boolean ritual; }
	public StyxRenderer(EntityRendererProvider.Context context) { super(context,new StyxModel(context.bakeLayer(StyxModel.LAYER)),0.5F); }
	@Override public State createRenderState() { return new State(); }
	@Override public Identifier getTextureLocation(State state) { return Jugcraft.id("textures/entity/styxhexenhammer.png"); }
	@Override public void extractRenderState(Styxhexenhammer entity,State state,float partial) {
		super.extractRenderState(entity,state,partial);
		state.ritual=Styxhexenhammer.phase(entity.level().getOverworldClockTime())==2;
	}
}
