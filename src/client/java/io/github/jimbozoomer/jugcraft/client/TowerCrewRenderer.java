package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.tower.crew.PlasmaBaton;
import io.github.jimbozoomer.jugcraft.tower.crew.PlasmaBlade;
import io.github.jimbozoomer.jugcraft.tower.crew.PlasmaRifle;
import io.github.jimbozoomer.jugcraft.tower.crew.TowerCrew;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.jspecify.annotations.Nullable;

/**
 * Draws a tower crew member (tower/crew/TowerCrew) in the skin the server names, at a player's size (the player's 0.9375
 * scale), with the gear their skin wears and what they carry. Everything it needs comes from the server: the skin, the
 * gear bits, slim arms or not, the prop and what they are doing. A sleeper lying down ({@link TowerCrew#ACT_LIE}) is laid
 * on their back about the hips' back, which goes to the seat's point (on the bunk's blanket), under the blanket.
 */
public class TowerCrewRenderer extends HumanoidMobRenderer<TowerCrew, TowerCrewRenderer.State, TowerCrewModel> {
	public static final ModelLayerLocation PROPS = new ModelLayerLocation(Jugcraft.id("tower_crew"), "props");
	private static final Identifier PROPS_TEXTURE = Jugcraft.id("textures/entity/tower_crew_props.png");
	private static final Identifier PROPS_GLOW = Jugcraft.id("textures/entity/tower_crew_props_glow.png");
	private static final Map<String, Identifier> TEXTURES = new HashMap<>(), GLOWS = new HashMap<>();

	public static class State extends HumanoidRenderState {
		String skin = "controller_1";
		int gear;
		boolean slim;
		int prop;
		int act;
		/** Which of the ways to doze (from their id). */
		int variant;
		/** Greeting this client's player (tower/crew/CrewGreeting: none, wave, salute, nod, the coffee raised), and how far
		 * into it (ticks). */
		int greet;
		float greetT;
		/** Ticks since what they're doing began (the baton's draw, strike and holstering are timed by it). */
		float actTime;
		/** The baton: in hand (not in its holster), its rod thrown out (stowed, it is telescoped into the handle), and how
		 * far its plasma is out (0 to 1). */
		boolean batonInHand, batonOut;
		float batonExt;
		/** The plasma baton a gate guard carries: the item, resolved for them as it is now (stowed, out, lit). */
		final ItemStackRenderState baton = new ItemStackRenderState();
		/** Theirs alone, for what flickers (the baton's plasma), and the time it flickers by (ticks, smooth; kept small so
		 * it stays fine however long they've lived). */
		int seed;
		float flickerTime;
		/** How dazed (0 to 1): a drop squad's legionary out of the pod sways on their feet (LegionaryRenderer). */
		float daze;
		/** The power armour's plasma blade: how far it's deployed (0 stowed to 1 lit), and its fins still glowing after
		 * it's stowed (client/PowerArmor). */
		float blade;
		boolean bladeCooling;
		/** The NT-7 in their hands (security on their feet): the item, resolved for them (its charge from their shots). */
		final ItemStackRenderState rifle = new ItemStackRenderState();
	}

	/** The NT-7 drawn in a crew member's hands: a crew pixel to the rifle's (the item's pixels are the rifle's). */
	public static final float RIFLE_K = 1.0F;
	/** The item's model space (the barrel +x, up +y, its left -z) to the rifle group's (along the arm +y, its top -z). */
	private static final Quaternionf ITEM_TO_GROUP = new Quaternionf().setFromNormalized(new Matrix3f(0, 1, 0, 0, 0, -1, -1, 0, 0));
	private static @Nullable ItemStack rifleStack;
	/**
	 * The plasma baton in a guard's hand and holster: the item's model space (its length +x, its top +y; the cube's middle
	 * at the pose's origin) to its group's. In the fist: along the arm's reach ahead of it (the group's -z), its top up
	 * the arm (-y). In the holster: its length down the leg (+y), its top to the back (+z).
	 */
	private static final Quaternionf ITEM_TO_HAND = new Quaternionf().setFromNormalized(new Matrix3f(0, 0, -1, 0, -1, 0, -1, 0, 0));
	private static final Quaternionf ITEM_TO_HOLSTER = new Quaternionf().setFromNormalized(new Matrix3f(0, 1, 0, 0, 0, 1, 1, 0, 0));
	/** What of the baton sits on the holster's mouth (px along it from the grip's middle): its guard's front face (2.42,
	 * tools/plasma_baton.py GUARD) a tenth inside the clip, so the two never share a plane. */
	private static final float HOLSTER_SEAT = 2.32F;
	/** A guard's baton as the item says it (assets/jugcraft/items/plasma_baton.json): stowed, the rod out, lit. */
	private static final ItemStack[] BATONS = new ItemStack[3];
	private static final String[] BATON_STATES = {"stowed", "off", "lit"};

	/** Where a lying sleeper turns about (the hips' back, model y 12, z 2, at the crew's scale, blocks) and goes to (the
	 * seat's point: the vehicle attachment's 0.6 over their origin). */
	private static final float LIE_HIPS_Y = 12F / 16F * 0.9375F, LIE_HIPS_Z = 2F / 16F * 0.9375F, LIE_SEAT = 0.6F;

	public TowerCrewRenderer(EntityRendererProvider.Context context) {
		super(context, new TowerCrewModel(context.bakeLayer(TowerCrewModel.LAYER)), 0.5F);
		TowerCrewPropsModel props = new TowerCrewPropsModel(context.bakeLayer(PROPS));
		addLayer(new PropsLayer(this, props));
		addLayer(new GlowLayer(this, props));
		addLayer(new PowerArmor.Layer(this, props));
		addLayer(new RifleLayer(this, props));
		addLayer(new BatonLayer(this, props));
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(TowerCrew crew, State state, float partialTick) {
		super.extractRenderState(crew, state, partialTick);
		String skin = crew.skin();
		state.skin = skin.matches("[a-z0-9_]{1,40}") ? skin : "controller_1";
		state.gear = crew.gear();
		state.slim = crew.slim();
		state.act = crew.act();
		state.prop = state.act == TowerCrew.ACT_LIE ? TowerCrew.PROP_BLANKET : crew.heldProp();
		if (crew.armed() && state.act == TowerCrew.ACT_IDLE && state.prop == TowerCrew.PROP_NONE && !crew.isPassenger()) {
			// security on their feet: the plasma rifle at the low ready (aiming, the server says ACT_AIM)
			state.act = TowerCrew.ACT_READY;
			state.prop = TowerCrew.PROP_RIFLE;
		}
		if (state.prop == TowerCrew.PROP_RIFLE) {
			if (rifleStack == null) {
				rifleStack = new ItemStack(PlasmaRifle.RIFLE);
			}
			itemModelResolver.updateForLiving(state.rifle, rifleStack, ItemDisplayContext.NONE, crew);
		} else {
			state.rifle.clear();
		}
		BatonHum.track(crew);
		PowerArmor.track(crew, state.act, partialTick, state);
		state.actTime = crew.tickCount - crew.clientActSince + partialTick;
		TowerCrewModel.baton(state);
		state.seed = crew.getId();
		state.flickerTime = crew.tickCount % 24000 + partialTick;
		if ((state.gear & TowerCrew.GEAR_BATON) != 0 && !TowerCrew.asleep(state.act)) {
			// stowed in the holster and as the hand takes it; thrown out; lit once the plasma is half out
			int now = !state.batonInHand || !state.batonOut ? 0 : state.batonExt > 0.5F ? 2 : 1;
			itemModelResolver.updateForLiving(state.baton, batonStack(now), ItemDisplayContext.NONE, crew);
		} else {
			state.baton.clear();
		}
		state.variant = Math.floorMod(crew.getUUID().hashCode(), 4);
		CrewGreetings.update(crew, state, partialTick);
		if (TowerCrew.asleep(state.act)) {
			// asleep they lie or sit square to the seat, looking nowhere
			state.bodyRot = crew.getYRot();
			state.yRot = 0;
			state.xRot = 0;
		}
	}

	private static ItemStack batonStack(int now) {
		if (BATONS[now] == null) {
			ItemStack stack = new ItemStack(PlasmaBaton.BATON);
			stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(), List.of(), List.of(BATON_STATES[now]), List.of()));
			BATONS[now] = stack;
		}
		return BATONS[now];
	}

	@Override
	protected void setupRotations(State state, PoseStack pose, float bodyRot, float scale) {
		super.setupRotations(state, pose, bodyRot, scale);
		if (state.act == TowerCrew.ACT_LIE) {
			// on their back: the front up, the head towards the seat's back (the feet the way the seat faces)
			pose.translate(0, LIE_SEAT, 0);
			pose.rotateDegrees(Axis.XP, 90F);
			pose.translate(0, -LIE_HIPS_Y, -LIE_HIPS_Z);
		}
	}

	@Override
	public Identifier getTextureLocation(State state) {
		return skinTexture(state.skin);
	}

	/** A skin's texture (tools/crew_skins.py): the crew's, and the drop squads' legionaries' (LegionaryRenderer). */
	static Identifier skinTexture(String skin) {
		return TEXTURES.computeIfAbsent(skin, s -> Jugcraft.id("textures/entity/tower_crew/" + s + ".png"));
	}

	@Override
	protected void scale(State state, PoseStack pose) {
		pose.scale(0.9375F, 0.9375F, 0.9375F);
	}

	/** The glow layer: each skin's _glow.png and the props' glow, full bright over the faces (tools/crew_skins.py). */
	private static Identifier glow(String skin) {
		return GLOWS.computeIfAbsent(skin, s -> Jugcraft.id("textures/entity/tower_crew/" + s + "_glow.png"));
	}

	/**
	 * The uniforms' glow (the Narv-Tech uniforms sheet): the lit piping, cuffs, soles and cap bands on the skin, and the
	 * print, emblem and armour's actuators on the props, drawn full bright and added over the faces (the
	 * eyes layer, as spider eyes glow): violet by day, standing out at night. Drawn on the client only.
	 */
	static final class GlowLayer extends RenderLayer<State, TowerCrewModel> {
		private final TowerCrewPropsModel props;

		GlowLayer(RenderLayerParent<State, TowerCrewModel> parent, TowerCrewPropsModel props) {
			super(parent);
			this.props = props;
		}

		@Override
		public void submit(PoseStack pose, SubmitNodeCollector collector, int light, State state, float yRot, float xRot) {
			if (state.isInvisible) {
				return;
			}
			collector.order(2).submitModel(getParentModel(), state, pose, RenderTypes.eyes(glow(state.skin)), light, OverlayTexture.NO_OVERLAY,
					state.outlineColor);
			collector.order(3).submitModel(props, state, pose, RenderTypes.eyes(PROPS_GLOW), light, OverlayTexture.NO_OVERLAY, state.outlineColor);
		}
	}

	/**
	 * The NT-7 in their hands (P6): the item itself, at the props' rifle group (on the right forearm, the grip in the fist,
	 * turned out along the look while aiming), so the crew's rifle is the players' to the pixel, its accelerator flaring
	 * and its cell's bars refilling after each of their shots (client/PlasmaCharge). With the armour's plasma blade out
	 * (tower/crew/PlasmaBlade) it hangs in the left hand instead.
	 */
	static final class RifleLayer extends RenderLayer<State, TowerCrewModel> {
		private final TowerCrewPropsModel props;

		RifleLayer(RenderLayerParent<State, TowerCrewModel> parent, TowerCrewPropsModel props) {
			super(parent);
			this.props = props;
		}

		@Override
		public void submit(PoseStack pose, SubmitNodeCollector collector, int light, State state, float yRot, float xRot) {
			if (state.isInvisible || state.prop != TowerCrew.PROP_RIFLE || state.rifle.isEmpty()) {
				return;
			}
			props.setupAnim(state);
			pose.pushPose();
			if (PlasmaBlade.out(state.act)) {
				props.toRifleLeft(pose);                // the blade's out: the rifle's dropped to the left hand
			} else {
				props.toRifle(pose);
			}
			pose.rotate(ITEM_TO_GROUP);
			// the group is scaled by a half: twice the crew's pixel to the rifle's
			float k = RIFLE_K * 2.0F;
			pose.scale(k, k, k);
			state.rifle.submit(pose, collector, light, OverlayTexture.NO_OVERLAY, state.outlineColor);
			pose.popPose();
		}
	}

	/**
	 * The gate guards' plasma baton (Justin, 10 November 2026: A, "Patrol"): the item itself, a crew pixel to its own, so
	 * a guard's baton is the players' to the pixel. In the holster down the back of the right thigh it is stowed (the rod
	 * telescoped into the handle), its guard on the holster's mouth, the handle up; drawn, in the right fist, the rod
	 * thrown out as the arm comes up and then its plasma growing out of the emitter ({@link BatonPlasma}).
	 */
	static final class BatonLayer extends RenderLayer<State, TowerCrewModel> {
		private final TowerCrewPropsModel props;

		BatonLayer(RenderLayerParent<State, TowerCrewModel> parent, TowerCrewPropsModel props) {
			super(parent);
			this.props = props;
		}

		@Override
		public void submit(PoseStack pose, SubmitNodeCollector collector, int light, State state, float yRot, float xRot) {
			boolean inHand = state.batonInHand && state.prop == TowerCrew.PROP_BATON;
			if (state.isInvisible || state.baton.isEmpty() || state.batonInHand != inHand) {
				return;
			}
			props.setupAnim(state);
			pose.pushPose();
			if (inHand) {
				props.toBaton(pose);
				pose.rotate(ITEM_TO_HAND);
				pose.translate((8 - BatonPlasma.GX) / 16F, 0, 0);            // the grip's middle in the fist
			} else {
				props.toBatonHolstered(pose);
				pose.rotate(ITEM_TO_HOLSTER);
				pose.translate((8 - BatonPlasma.GX - HOLSTER_SEAT) / 16F, 0, 0);
			}
			state.baton.submit(pose, collector, light, OverlayTexture.NO_OVERLAY, state.outlineColor);
			if (inHand && state.batonOut && state.batonExt > 0.01F) {
				pose.translate(-0.5F, -0.5F, -0.5F);                       // the model's cube, as the item's own plasma is drawn in it
				BatonPlasma.submit(pose, collector, state.batonExt, false, state.seed, state.flickerTime);
			}
			pose.popPose();
		}
	}

	/** The props, posed with the body, from the shared props texture (and the prints and worn kit: always drawn). */
	static final class PropsLayer extends RenderLayer<State, TowerCrewModel> {
		private final TowerCrewPropsModel props;

		PropsLayer(RenderLayerParent<State, TowerCrewModel> parent, TowerCrewPropsModel props) {
			super(parent);
			this.props = props;
		}

		@Override
		public void submit(PoseStack pose, SubmitNodeCollector collector, int light, State state, float yRot, float xRot) {
			if (state.isInvisible) {
				return;
			}
			collector.order(1).submitModel(props, state, pose, RenderTypes.entityCutout(PROPS_TEXTURE), light, OverlayTexture.NO_OVERLAY,
					state.outlineColor);
		}
	}
}
