package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jimbozoomer.jugcraft.tower.crew.PlasmaBlade;
import io.github.jimbozoomer.jugcraft.tower.crew.PlasmaRifle;
import io.github.jimbozoomer.jugcraft.tower.crew.TowerCrew;
import io.github.jimbozoomer.jugcraft.tower.squad.Legionary;
import org.jspecify.annotations.Nullable;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Draws a drop squad's legionary (tower/squad/Legionary) as the security detail are drawn (the crew's model, skins,
 * power armour, helmet and the NT-7 in their hands: client/TowerCrewRenderer's layers and client/PowerArmor), always with
 * the rifle: at the low ready, raised to aim while they fire, in the left hand while the armour's plasma blade is out. The leader's helmet carries his rank's crest (the props' crest groups, its gear
 * bit). Dazed out of the pod they sway on their feet.
 */
public class LegionaryRenderer extends HumanoidMobRenderer<Legionary, TowerCrewRenderer.State, TowerCrewModel> {
	private static @Nullable ItemStack rifleStack;

	public LegionaryRenderer(EntityRendererProvider.Context context) {
		super(context, new TowerCrewModel(context.bakeLayer(TowerCrewModel.LAYER)), 0.5F);
		TowerCrewPropsModel props = new TowerCrewPropsModel(context.bakeLayer(TowerCrewRenderer.PROPS));
		addLayer(new TowerCrewRenderer.PropsLayer(this, props));
		addLayer(new TowerCrewRenderer.GlowLayer(this, props));
		addLayer(new PowerArmor.Layer(this, props));
		addLayer(new TowerCrewRenderer.RifleLayer(this, props));
	}

	@Override
	public TowerCrewRenderer.State createRenderState() {
		return new TowerCrewRenderer.State();
	}

	@Override
	public void extractRenderState(Legionary legionary, TowerCrewRenderer.State state, float partialTick) {
		super.extractRenderState(legionary, state, partialTick);
		String skin = legionary.skin();
		state.skin = skin.matches("[a-z0-9_]{1,40}") ? skin : "security_1";
		state.gear = legionary.gear();
		state.slim = legionary.slim();
		int act = legionary.act();
		state.act = act == TowerCrew.ACT_AIM || PlasmaBlade.out(act) ? act : TowerCrew.ACT_READY;
		PowerArmor.track(legionary, act, partialTick, state);
		state.prop = TowerCrew.PROP_RIFLE;
		if (rifleStack == null) {
			rifleStack = new ItemStack(PlasmaRifle.RIFLE);
		}
		itemModelResolver.updateForLiving(state.rifle, rifleStack, ItemDisplayContext.NONE, legionary);
		if (legionary.clientAct != act) {
			legionary.clientAct = act;
			legionary.clientActSince = legionary.tickCount;
		}
		state.actTime = legionary.tickCount - legionary.clientActSince + partialTick;
		state.batonInHand = false;
		state.batonExt = 0;
		state.variant = Math.floorMod(legionary.getUUID().hashCode(), 4);
		state.greet = 0;
		if (act == TowerCrew.ACT_IDLE) {
			legionary.clientDazed = legionary.tickCount;          // (dazed out of the pod: the server holds them idle)
		}
		state.daze = Mth.clamp(1 - (legionary.tickCount - legionary.clientDazed + partialTick) / 15F, 0, 1);
	}

	@Override
	protected void setupRotations(TowerCrewRenderer.State state, PoseStack pose, float bodyRot, float scale) {
		super.setupRotations(state, pose, bodyRot, scale);
		if (state.daze > 0) {
			// dazed out of the pod: a slow sway about their feet, side to side and a little forward, settling as it passes
			float t = state.ageInTicks;
			pose.rotateDegrees(com.mojang.math.Axis.ZP, state.daze * 6 * Mth.sin(t * 0.21F));
			pose.rotateDegrees(com.mojang.math.Axis.XP, state.daze * 3 * Mth.sin(t * 0.13F + 1));
		}
	}

	@Override
	public Identifier getTextureLocation(TowerCrewRenderer.State state) {
		return TowerCrewRenderer.skinTexture(state.skin);
	}

	@Override
	protected void scale(TowerCrewRenderer.State state, PoseStack pose) {
		pose.scale(0.9375F, 0.9375F, 0.9375F);
	}
}
