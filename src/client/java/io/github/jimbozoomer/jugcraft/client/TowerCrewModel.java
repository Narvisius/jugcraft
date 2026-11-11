package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.tower.crew.CrewGreeting;
import io.github.jimbozoomer.jugcraft.tower.crew.PlasmaBaton;
import io.github.jimbozoomer.jugcraft.tower.crew.TowerCrew;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * A tower crew member's body (tools/crew_skins.py paints their 128 x 64 skin): the player's boxes, with arms four
 * pixels wide or (the janitors) three, the outer layer on every part, and their gear (headset, cap peak, hard hat, bun,
 * ponytail) from assets/jugcraft/crew/gear.json, shown as the skin says. It moves as any humanoid does, and holds the
 * pose of what it is doing ({@link #pose}): typing at a console, reading a tablet, on the radio, cooking, pushing the
 * cart; and asleep (the night shift and breaks sheet): dozing sitting up, each their own way, laid back in a recliner,
 * or lying under a blanket (the renderer lays the body down), the eyelids shut and the headset and hard hat off. Over
 * whatever they are doing, a greeting to this client's player (client/CrewGreetings): a wave, a salute, a nod, the
 * coffee raised.
 *
 * <p>No coplanar faces (Justin's rule): each outer layer stands off its base box, and where two layers could meet they
 * stand off each other by at least 0.1 px: the jacket 0.25 (its hem level with the trousers' top, not over it), the
 * sleeves 0.35 (0.1 short of the arm on the body's side), the right trouser leg 0.25 and the left 0.15, the legs side
 * by side on their pivots two pixels out (the player's 1.9 would overlap them).
 */
public class TowerCrewModel extends HumanoidModel<TowerCrewRenderer.State> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Jugcraft.id("tower_crew"), "main");

	private record Gear(ModelPart part, int bit) {
	}

	private final ModelPart rightArmSlim, leftArmSlim;
	private final List<Gear> gear = new ArrayList<>();

	public TowerCrewModel(ModelPart root) {
		super(root);
		rightArmSlim = root.getChild("right_arm_slim");
		leftArmSlim = root.getChild("left_arm_slim");
		for (CrewBoxes.Group g : CrewBoxes.gear()) {
			ModelPart parent = switch (g.parent()) {
				case "head" -> head;
				case "body" -> body;
				default -> root;
			};
			gear.add(new Gear(parent.getChild(g.name()), g.bit()));
		}
	}

	/** A box grown by lo on each axis' low side and hi on its high side, its texture left on the base box's unwrap. */
	private static CubeListBuilder layer(int u, int v, float x, float y, float z, float w, float h, float d, float[] lo, float[] hi) {
		return CubeListBuilder.create().texOffs(u, v).addBox(x + (hi[0] - lo[0]) / 2, y + (hi[1] - lo[1]) / 2, z + (hi[2] - lo[2]) / 2,
				w, h, d, new CubeDeformation((lo[0] + hi[0]) / 2, (lo[1] + hi[1]) / 2, (lo[2] + hi[2]) / 2));
	}

	private static CubeListBuilder box(int u, int v, float x, float y, float z, float w, float h, float d) {
		return CubeListBuilder.create().texOffs(u, v).addBox(x, y, z, w, h, d);
	}

	private static void arm(PartDefinition root, String name, String sleeve, int u, int v, int su, int sv, float pivotX, float pivotY,
			float x, float width, boolean right) {
		// sleeves 0.35 out all round but 0.1 short on the side towards the body
		float[] lo = right ? new float[] {0.35F, 0.35F, 0.35F} : new float[] {-0.1F, 0.35F, 0.35F};
		float[] hi = right ? new float[] {-0.1F, 0.35F, 0.35F} : new float[] {0.35F, 0.35F, 0.35F};
		PartDefinition arm = root.addOrReplaceChild(name, box(u, v, x, -2, -2, width, 12, 4), PartPose.offset(pivotX, pivotY, 0));
		arm.addOrReplaceChild(sleeve, layer(su, sv, x, -2, -2, width, 12, 4, lo, hi), PartPose.ZERO);
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition head = root.addOrReplaceChild("head", box(0, 0, -4, -8, -4, 8, 8, 8), PartPose.ZERO);
		head.addOrReplaceChild("hat", CubeListBuilder.create().texOffs(32, 0).addBox(-4, -8, -4, 8, 8, 8, new CubeDeformation(0.5F)), PartPose.ZERO);
		PartDefinition body = root.addOrReplaceChild("body", box(16, 16, -4, 0, -2, 8, 12, 4), PartPose.ZERO);
		body.addOrReplaceChild("jacket", layer(16, 32, -4, 0, -2, 8, 12, 4, new float[] {0.25F, 0.25F, 0.25F}, new float[] {0.25F, 0F, 0.25F}), PartPose.ZERO);
		arm(root, "right_arm", "right_sleeve", 40, 16, 40, 32, -5, 2, -3, 4, true);
		arm(root, "left_arm", "left_sleeve", 32, 48, 48, 48, 5, 2, -1, 4, false);
		arm(root, "right_arm_slim", "right_sleeve", 40, 16, 40, 32, -5, 2.5F, -2, 3, true);
		arm(root, "left_arm_slim", "left_sleeve", 32, 48, 48, 48, 5, 2.5F, -1, 3, false);
		PartDefinition rightLeg = root.addOrReplaceChild("right_leg", box(0, 16, -2, 0, -2, 4, 12, 4), PartPose.offset(-2, 12, 0));
		rightLeg.addOrReplaceChild("right_pants", layer(0, 32, -2, 0, -2, 4, 12, 4, new float[] {0.25F, 0F, 0.25F}, new float[] {0.1F, 0.25F, 0.25F}), PartPose.ZERO);
		PartDefinition leftLeg = root.addOrReplaceChild("left_leg", box(16, 48, -2, 0, -2, 4, 12, 4), PartPose.offset(2, 12, 0));
		leftLeg.addOrReplaceChild("left_pants", layer(0, 48, -2, 0, -2, 4, 12, 4, new float[] {0.1F, 0F, 0.15F}, new float[] {0.15F, 0.35F, 0.15F}), PartPose.ZERO);
		for (CrewBoxes.Group g : CrewBoxes.gear()) {
			CrewBoxes.add(switch (g.parent()) {
				case "head" -> head;
				case "body" -> body;
				default -> root;
			}, g);
		}
		return LayerDefinition.create(mesh, 128, 64);
	}

	@Override
	public void setupAnim(TowerCrewRenderer.State state) {
		super.setupAnim(state);
		pose(this, state);
		copyRotation(rightArm, rightArmSlim);
		copyRotation(leftArm, leftArmSlim);
		rightArmSlim.z = rightArm.z;               // (a pilot's recline moves the arms back; the slim arms keep their own height)
		leftArmSlim.z = leftArm.z;
		// lying under the blanket, the arms and legs are under it
		boolean limbs = state.act != TowerCrew.ACT_LIE;
		rightArm.visible = !state.slim && limbs;
		leftArm.visible = !state.slim && limbs;
		rightArmSlim.visible = state.slim && limbs;
		leftArmSlim.visible = state.slim && limbs;
		rightLeg.visible = limbs;
		leftLeg.visible = limbs;
		boolean asleep = TowerCrew.asleep(state.act);
		int worn = asleep ? state.gear & ~(TowerCrew.GEAR_HEADSET | TowerCrew.GEAR_HARDHAT | TowerCrew.GEAR_HELMET) : state.gear;
		for (Gear g : gear) {
			g.part().visible = g.bit() == TowerCrew.GEAR_EYELIDS ? asleep : (worn & g.bit()) != 0;
		}
		// security's full helmet (the props layer) takes the head's place while it is on
		head.visible = (worn & TowerCrew.GEAR_HELMET) == 0;
	}

	private static void copyRotation(ModelPart from, ModelPart to) {
		to.xRot = from.xRot;
		to.yRot = from.yRot;
		to.zRot = from.zRot;
	}

	/**
	 * The pose of what a crew member is doing, over the walk and the seat HumanoidModel gives (the props model takes it
	 * too, so what they carry stays in their hands). Arms forward are negative x rotations; an arm turned in towards the
	 * middle is a negative y rotation on the right, positive on the left; the head looks down with a positive x rotation.
	 */
	static void pose(HumanoidModel<?> m, TowerCrewRenderer.State s) {
		if (TowerCrew.asleep(s.act)) {
			m.head.xRot = 0;                       // asleep, they look nowhere
			m.head.yRot = 0;
		}
		switch (s.act) {
			case TowerCrew.ACT_TYPE -> {
				arm(m.rightArm, -1.25F, -0.22F);
				arm(m.leftArm, -1.25F, 0.22F);
				m.head.xRot = 0.22F;
			}
			case TowerCrew.ACT_READ -> {
				arm(m.leftArm, -1.0F, 0.30F);
				m.head.xRot = 0.42F;
				m.head.yRot *= 0.3F;
			}
			case TowerCrew.ACT_RADIO -> arm(m.rightArm, -1.95F, -0.30F);
			case TowerCrew.ACT_COOK -> {
				arm(m.rightArm, -0.85F, -0.15F);
				arm(m.leftArm, -0.75F, 0.20F);
				m.head.xRot = 0.40F;
			}
			case TowerCrew.ACT_POINT -> {
				arm(m.rightArm, -1.0F, -0.25F);
				m.head.xRot = 0.45F;
			}
			case TowerCrew.ACT_PUSH -> {
				arm(m.rightArm, -1.15F, -0.12F);
				arm(m.leftArm, -1.15F, 0.12F);
			}
			case TowerCrew.ACT_TRAY -> arm(m.rightArm, -1.2F, -0.10F);
			case TowerCrew.ACT_MUG -> {
				arm(m.rightArm, -0.95F, -0.18F);
				arm(m.leftArm, -0.85F, 0.12F);
				m.head.xRot = 0.10F;
			}
			case TowerCrew.ACT_CARRY -> arm(m.rightArm, 0.0F, 0.0F);
			case TowerCrew.ACT_BENCH -> {
				arm(m.rightArm, -0.85F, -0.15F);
				arm(m.leftArm, -0.75F, 0.20F);
				m.head.xRot = 0.40F;
			}
			case TowerCrew.ACT_PILOT -> {
				// in a Pilot Simulator: leaned back with its seat (22.5 degrees), the right hand down on the stick, the left
				// on the throttle, looking ahead at the screens
				recline(m, RECLINE);
				m.rightArm.xRot -= 0.55F;
				m.rightArm.yRot = -0.10F;
				m.rightArm.zRot = 0.30F;
				m.leftArm.xRot -= 0.75F;
				m.leftArm.yRot = 0.10F;
				m.leftArm.zRot = -0.30F;
				m.head.xRot += 0.32F;
			}
			case TowerCrew.ACT_DOZE -> {
				// asleep sitting up: leant back into the seat, the head nodded and over to one side (each their own way),
				// the hands in the lap
				float[] d = DOZE[s.variant & 3];
				recline(m, -0.20F);
				m.rightArm.xRot += d[2];
				m.rightArm.yRot = -0.28F;
				m.leftArm.xRot += d[3];
				m.leftArm.yRot = 0.30F;
				m.head.xRot += d[0];
				m.head.zRot = d[1];
			}
			case TowerCrew.ACT_RECLINE -> {
				// asleep in a recliner, laid back with it (22.5 degrees), the legs out on its footrest, the forearms on its
				// arms, the head over to one side
				recline(m, RECLINE);
				leg(m.rightLeg, -1.50F, Mth.PI / 14, 0.06F);
				leg(m.leftLeg, -1.50F, -Mth.PI / 14, -0.06F);
				m.rightArm.xRot = -1.0F;
				m.rightArm.yRot = 0;
				m.rightArm.zRot = 0.04F;
				m.leftArm.xRot = -1.0F;
				m.leftArm.yRot = 0;
				m.leftArm.zRot = -0.04F;
				m.head.xRot += 0.30F;
				m.head.zRot = (s.variant & 1) != 0 ? -0.26F : 0.26F;
			}
			case TowerCrew.ACT_READY -> {
				// the plasma rifle at the low ready: across the body, the muzzle down and in, the left hand on it
				arm(m.rightArm, -0.90F, -0.60F);
				arm(m.leftArm, -0.95F, 0.62F);
			}
			case TowerCrew.ACT_AIM -> {
				// aimed: both arms up along the look, the right turned in to meet the left (the props model turns the rifle
				// back out along the look)
				m.rightArm.xRot = -Mth.PI / 2 + m.head.xRot;
				m.rightArm.yRot = -0.42F + m.head.yRot;
				m.rightArm.zRot = 0;
				m.leftArm.xRot = -Mth.PI / 2 + m.head.xRot + 0.05F;
				m.leftArm.yRot = 0.52F + m.head.yRot;
				m.leftArm.zRot = 0;
			}
			case TowerCrew.ACT_DRAW, TowerCrew.ACT_BATON, TowerCrew.ACT_STRIKE, TowerCrew.ACT_HOLSTER -> batonPose(m, s);
			case TowerCrew.ACT_BLADE, TowerCrew.ACT_THRUST, TowerCrew.ACT_SLASH -> bladePose(m, s);
			case TowerCrew.ACT_LIE -> {
				// on their back under the blanket (the renderer lays the body down): the head raised onto the pillow and
				// tipped a little
				m.head.z = LIE_HEAD;
				m.head.zRot = 0.16F;
			}
			default -> {
			}
		}
		greet(m, s);
	}

	/**
	 * A greeting over the pose (the greetings sheet): the arm up over its first 6 ticks, held, down over the last 6 of
	 * CrewGreeting.TICKS; a wave (the raised hand rocking out and back), a salute (the hand to the brow), a nod (twice), the
	 * coffee raised.
	 */
	private static void greet(HumanoidModel<?> m, TowerCrewRenderer.State st) {
		if (st.greet == CrewGreeting.NONE) {
			return;
		}
		float t = st.greetT;
		float s = Mth.clamp(Math.min(1F, Math.min(t / 6F, (CrewGreeting.TICKS - t) / 6F)), 0F, 1F);
		ModelPart ra = m.rightArm;
		switch (st.greet) {
			case CrewGreeting.WAVE -> {
				ra.xRot = (1 - s) * ra.xRot + s * -2.65F;
				ra.yRot = (1 - s) * ra.yRot;
				ra.zRot = -s * (0.55F + 0.25F * Mth.sin(t * 0.9F));
				m.head.xRot -= 0.08F * s;
			}
			case CrewGreeting.SALUTE -> {
				ra.xRot = (1 - s) * ra.xRot + s * -2.35F;
				ra.yRot = (1 - s) * ra.yRot + s * -0.62F;
				ra.zRot = s * -0.28F;
				m.head.xRot -= 0.05F * s;
			}
			case CrewGreeting.NOD -> {
				if (t < 24) {
					m.head.xRot += 0.38F * Mth.sin(Mth.PI * (t < 12 ? t : t - 12) / 12F);
				}
			}
			case CrewGreeting.CHEERS -> {
				ra.xRot -= 0.55F * s;
				ra.yRot -= 0.10F * s;
				ra.zRot = 0;
				m.head.xRot -= 0.06F * s;
			}
			default -> {
			}
		}
	}

	/** The baton on guard: the right arm up and ahead, the baton above the line of the shoulders, the left hand out. */
	private static final float[] GUARD_R = {-1.15F, -0.15F}, GUARD_L = {-0.55F, 0.30F};
	/** The right hand down to the holster on the back of the thigh. */
	private static final float[] HOLSTER_R = {0.38F, 0.0F, 0.06F};

	/** The plasma blade (the power armour sheets' blade pictures): the rifle held low in the left hand; a thrust's reach;
	 * a slash's wind-up over the shoulder and its end down across the body. */
	// (r196: the thrust turned in and the slash brought down across the body, from over the right shoulder to the left hip,
	// so that the blade goes through what's straight ahead of them: before, a slash came down 0.4 to 1 block to their right
	// of it; the cut now follows the blade, PlasmaWounds.Swing, so it has to pass through what it hits)
	private static final float[] BLADE_L = {-0.30F, 0.22F}, THRUST_R = {-1.5F, -0.44F}, SLASH_UP = {-2.6F, 0.2F}, SLASH_DOWN = {-0.75F, -1.1F};

	/**
	 * The armour's plasma blade out of the right forearm (tower/crew/PlasmaBlade): on guard as the baton's; a thrust out
	 * and back; a slash up over the shoulder, down across and back to the guard. Each move takes
	 * {@link io.github.jimbozoomer.jugcraft.tower.crew.PlasmaBlade#MOVE_TICKS} ticks, its blow landing halfway.
	 */
	private static void bladePose(HumanoidModel<?> m, TowerCrewRenderer.State s) {
		set(m.leftArm, BLADE_L[0], BLADE_L[1], 0);
		float p = Mth.clamp(s.actTime / io.github.jimbozoomer.jugcraft.tower.crew.PlasmaBlade.MOVE_TICKS, 0F, 1F);
		// a move is aimed where they look: up at a tall thing, down at a low one
		float aim = Mth.clamp(s.xRot * Mth.DEG_TO_RAD, -0.9F, 0.9F);
		switch (s.act) {
			case TowerCrew.ACT_THRUST -> {
				float k = p < 0.5F ? ease(p / 0.5F) : 1 - ease((p - 0.5F) / 0.5F);
				set(m.rightArm, lerp(GUARD_R[0], THRUST_R[0] + aim, k), lerp(GUARD_R[1], THRUST_R[1], k), 0);
			}
			case TowerCrew.ACT_SLASH -> {
				if (p < 0.3F) {
					float k = ease(p / 0.3F);
					set(m.rightArm, lerp(GUARD_R[0], SLASH_UP[0] + aim, k), lerp(GUARD_R[1], SLASH_UP[1], k), 0);
				} else if (p < 0.7F) {
					float k = ease((p - 0.3F) / 0.4F);
					set(m.rightArm, lerp(SLASH_UP[0], SLASH_DOWN[0], k) + aim, lerp(SLASH_UP[1], SLASH_DOWN[1], k), 0);
				} else {
					float k = ease((p - 0.7F) / 0.3F);
					set(m.rightArm, lerp(SLASH_DOWN[0] + aim, GUARD_R[0], k), lerp(SLASH_DOWN[1], GUARD_R[1], k), 0);
				}
			}
			default -> set(m.rightArm, GUARD_R[0], GUARD_R[1], 0);
		}
	}

	private static float ease(float k) {
		k = Mth.clamp(k, 0F, 1F);
		return k * k * (3 - 2 * k);
	}

	private static float lerp(float a, float b, float k) {
		return a + (b - a) * k;
	}

	/**
	 * Whether the baton is in hand and how far its plasma is out, from what they're doing and how long they've been at it
	 * (the baton sheet): the draw (PlasmaBaton.DRAW_TICKS): the hand to the holster by 4 ticks, the baton in hand from
	 * there, up to the guard by 9, the plasma growing up the rod over ticks 8-13; on guard and striking it's out; putting it
	 * away (PlasmaBaton.HOLSTER_TICKS): the plasma back over 4 ticks, the hand to the holster by 8, in.
	 */
	static void baton(TowerCrewRenderer.State s) {
		float t = s.actTime;
		switch (s.act) {
			case TowerCrew.ACT_DRAW -> {
				s.batonInHand = t >= PlasmaBaton.DRAW_CATCH;
				s.batonOut = t >= PlasmaBaton.DRAW_OUT;
				s.batonExt = ease((t - 8) / 5F);
			}
			case TowerCrew.ACT_BATON, TowerCrew.ACT_STRIKE -> {
				s.batonInHand = true;
				s.batonOut = true;
				s.batonExt = 1;
			}
			case TowerCrew.ACT_HOLSTER -> {
				s.batonInHand = t < 8;
				s.batonOut = t < PlasmaBaton.HOLSTER_IN;
				s.batonExt = 1 - ease(t / 4F);
			}
			default -> {
				s.batonInHand = false;
				s.batonOut = false;
				s.batonExt = 0;
			}
		}
	}

	/** The baton's arms (the baton sheet): drawing, on guard, a strike (up over the shoulder, down hard, back), away. */
	private static void batonPose(HumanoidModel<?> m, TowerCrewRenderer.State s) {
		float t = s.actTime;
		ModelPart ra = m.rightArm, la = m.leftArm;
		switch (s.act) {
			case TowerCrew.ACT_DRAW -> {
				if (t < 4) {
					float k = ease(t / 4F);
					set(ra, lerp(ra.xRot, HOLSTER_R[0], k), lerp(ra.yRot, HOLSTER_R[1], k), HOLSTER_R[2] * k);
				} else {
					float k = ease((t - 4) / 5F);
					set(ra, lerp(HOLSTER_R[0], GUARD_R[0], k), lerp(HOLSTER_R[1], GUARD_R[1], k), HOLSTER_R[2] * (1 - k));
				}
				float k = ease((t - 4) / 5F);
				set(la, lerp(la.xRot, GUARD_L[0], k), lerp(la.yRot, GUARD_L[1], k), 0);
			}
			case TowerCrew.ACT_BATON -> {
				set(ra, GUARD_R[0], GUARD_R[1], 0);
				set(la, GUARD_L[0], GUARD_L[1], 0);
			}
			case TowerCrew.ACT_STRIKE -> {
				float p = Mth.clamp(t / 8F, 0F, 1F);              // PlasmaBaton.STRIKE_TICKS; the blow lands halfway
				float x;
				if (p < 0.35F) {
					x = lerp(GUARD_R[0], -2.75F, ease(p / 0.35F));
				} else if (p < 0.65F) {
					x = lerp(-2.75F, -0.30F, ease((p - 0.35F) / 0.3F));
				} else {
					x = lerp(-0.30F, GUARD_R[0], ease((p - 0.65F) / 0.35F));
				}
				set(ra, x, GUARD_R[1], 0.12F * Mth.sin(p * Mth.PI));
				set(la, GUARD_L[0], GUARD_L[1], 0);
			}
			case TowerCrew.ACT_HOLSTER -> {
				if (t < 4) {
					set(ra, GUARD_R[0], GUARD_R[1], 0);
					set(la, GUARD_L[0], GUARD_L[1], 0);
				} else if (t < 8) {
					float k = ease((t - 4) / 4F);
					set(ra, lerp(GUARD_R[0], HOLSTER_R[0], k), lerp(GUARD_R[1], HOLSTER_R[1], k), HOLSTER_R[2] * k);
					set(la, lerp(GUARD_L[0], la.xRot, k), lerp(GUARD_L[1], la.yRot, k), 0);
				} else {
					float k = ease((t - 8) / 4F);
					set(ra, lerp(HOLSTER_R[0], ra.xRot, k), lerp(HOLSTER_R[1], ra.yRot, k), HOLSTER_R[2] * (1 - k));
				}
			}
			default -> {
			}
		}
	}

	private static void set(ModelPart p, float x, float y, float z) {
		p.xRot = x;
		p.yRot = y;
		p.zRot = z;
	}

	/** How each one dozes (the renderer picks one from their id): the head's nod and tilt, the arms' rest in the lap. */
	private static final float[][] DOZE = {{0.52F, 0.30F, 0.12F, 0.06F}, {0.40F, -0.34F, 0.04F, 0.14F}, {0.72F, 0.08F, 0.10F, 0.10F},
			{-0.15F, 0.20F, 0.08F, 0.04F}};
	/** A sleeper lying down: the head raised (towards the front, up as they lie) onto the pillow, model px. */
	private static final float LIE_HEAD = -3.1F;

	/** How far a pilot leans back in a Pilot Simulator (its seat's 22.5 degrees). */
	private static final float RECLINE = -0.3927F;

	/** Leans the head, body and arms back about the hips (model y 12) by {@code angle} (negative: back). */
	private static void recline(HumanoidModel<?> m, float angle) {
		float c = net.minecraft.util.Mth.cos(angle), s = net.minecraft.util.Mth.sin(angle);
		for (ModelPart p : new ModelPart[] {m.head, m.body, m.rightArm, m.leftArm}) {
			float y = p.y - 12, z = p.z;
			p.y = 12 + y * c - z * s;
			p.z = y * s + z * c;
			p.xRot += angle;
		}
	}

	private static void arm(ModelPart arm, float xRot, float yRot) {
		arm.xRot = xRot;
		arm.yRot = yRot;
		arm.zRot = 0;
	}

	private static void leg(ModelPart leg, float xRot, float yRot, float zRot) {
		leg.xRot = xRot;
		leg.yRot = yRot;
		leg.zRot = zRot;
	}
}
