package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.tower.crew.TowerCrew;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * What the tower crew carry and wear over their skins (assets/jugcraft/crew/props.json, painted on
 * textures/entity/tower_crew_props.png): the tablet and clipboard in the left hand, the radio, toolbox, mug, tray and
 * plasma rifle in the right, the facilities cart ahead of the janitor, the blanket over a sleeper lying down; the
 * NARV-TECH print on every uniform's back and the emblem on its breast (drawn finer than the skin, always shown);
 * security's helmet and power armour, with the print and emblem on the armour instead. Its body parts are empty and posed
 * exactly as the crew member's ({@link TowerCrewModel#pose}), so what hangs off them moves with them; the mug and the tray
 * are kept level whatever the arm does, and the rifle is turned back out along the look when they aim it (the rifle
 * itself, the NT-7 item, is drawn at its group by TowerCrewRenderer; so is the gate guards' plasma baton, at its group in
 * the fist and at the one on the holster's mouth).
 */
public class TowerCrewPropsModel extends HumanoidModel<TowerCrewRenderer.State> {
	private record Prop(ModelPart part, CrewBoxes.Group group, ModelPart parent) {
	}

	/** The rifle turned out from the arm (turned in to meet the other hand) while aiming: back along the look. */
	private static final float RIFLE_OUT = 0.42F;

	private final List<Prop> props = new ArrayList<>();

	public TowerCrewPropsModel(ModelPart root) {
		super(root);
		for (CrewBoxes.Group g : CrewBoxes.props()) {
			ModelPart parent = parent(g.parent());
			props.add(new Prop(parent.getChild(g.name()), g, parent));
		}
	}

	private ModelPart parent(String name) {
		return switch (name) {
			case "right_arm" -> rightArm;
			case "left_arm" -> leftArm;
			case "right_leg" -> rightLeg;
			case "left_leg" -> leftLeg;
			case "body" -> body;
			case "head" -> head;
			default -> root();
		};
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);
		head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);
		PartDefinition rightArm = root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5, 2, 0));
		PartDefinition leftArm = root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5, 2, 0));
		PartDefinition rightLeg = root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-2, 12, 0));
		PartDefinition leftLeg = root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(2, 12, 0));
		for (CrewBoxes.Group g : CrewBoxes.props()) {
			CrewBoxes.add(switch (g.parent()) {
				case "right_arm" -> rightArm;
				case "left_arm" -> leftArm;
				case "right_leg" -> rightLeg;
				case "left_leg" -> leftLeg;
				case "body" -> body;
				case "head" -> head;
				default -> root;
			}, g);
		}
		int[] size = CrewBoxes.propsTexture();
		return LayerDefinition.create(mesh, size[0], size[1]);
	}

	/**
	 * Moves {@code pose} (the crew member's model space) into the rifle's group, posed by {@link #setupAnim}: on the right
	 * forearm, the grip's top in the fist, turned out along the look while aiming, scaled by the group's half. The NT-7
	 * item is drawn there (TowerCrewRenderer).
	 */
	public void toRifle(com.mojang.blaze3d.vertex.PoseStack pose) {
		root().translateAndRotate(pose);
		rightArm.translateAndRotate(pose);
		for (Prop p : props) {
			if (p.group().prop() == TowerCrew.PROP_RIFLE) {
				p.part().translateAndRotate(pose);
				return;
			}
		}
	}

	/**
	 * Moves {@code pose} into the baton's group in the right fist (the middle of its grip there), posed by
	 * {@link #setupAnim}. The plasma baton item is drawn there (TowerCrewRenderer.BatonLayer).
	 */
	public void toBaton(com.mojang.blaze3d.vertex.PoseStack pose) {
		root().translateAndRotate(pose);
		rightArm.translateAndRotate(pose);
		for (Prop p : props) {
			if (p.group().prop() == TowerCrew.PROP_BATON) {
				p.part().translateAndRotate(pose);
				return;
			}
		}
	}

	/** As {@link #toBaton}, into the group on the holster's mouth, down the back of the right thigh. */
	public void toBatonHolstered(com.mojang.blaze3d.vertex.PoseStack pose) {
		root().translateAndRotate(pose);
		rightLeg.translateAndRotate(pose);
		for (Prop p : props) {
			if (p.group().holstered()) {
				p.part().translateAndRotate(pose);
				return;
			}
		}
	}

	/** As {@link #toRifle}, on the left forearm (its place and turn mirrored): where the rifle hangs while the armour's
	 * plasma blade is out of the right. */
	public void toRifleLeft(com.mojang.blaze3d.vertex.PoseStack pose) {
		root().translateAndRotate(pose);
		leftArm.translateAndRotate(pose);
		for (Prop p : props) {
			if (p.group().prop() == TowerCrew.PROP_RIFLE) {
				ModelPart r = p.part();
				pose.translate(-r.x / 16F, r.y / 16F, r.z / 16F);
				pose.rotate(new org.joml.Quaternionf().rotationZYX(-r.zRot, -r.yRot, r.xRot));
				pose.scale(r.xScale, r.yScale, r.zScale);
				return;
			}
		}
	}

	@Override
	public void setupAnim(TowerCrewRenderer.State state) {
		super.setupAnim(state);
		TowerCrewModel.pose(this, state);
		boolean asleep = TowerCrew.asleep(state.act);
		boolean lying = state.act == TowerCrew.ACT_LIE;
		for (Prop p : props) {
			CrewBoxes.Group g = p.group();
			if (g.holstered()) {
				// the baton in its holster: there unless it's in hand (and off with the holster to sleep). Only its place: the
				// item is drawn there (TowerCrewRenderer.BatonLayer)
				p.part().visible = (state.gear & g.bit()) != 0 && !asleep && !state.batonInHand;
			} else if (g.bit() != 0) {
				// worn kit (security's helmet and armour, the gate guards' holster): off to sleep; the armour's back frame put
				// by while they sit
				p.part().visible = (state.gear & g.bit()) != 0 && !asleep && !(g.standing() && state.isPassenger);
			} else if (g.prop() == 0) {
				// the uniform's print and emblem: not under a blanket, nor under armour that carries its own
				p.part().visible = !lying && !((state.gear & g.hide()) != 0 && !asleep);
			} else if (g.prop() == TowerCrew.PROP_BATON) {
				// the baton in hand (not while the hand's still on its way to the holster): only its place, as the holstered one's
				p.part().visible = state.prop == TowerCrew.PROP_BATON && state.batonInHand && (!g.extend() || state.batonExt > 0.01F);
			} else {
				p.part().visible = g.prop() == state.prop;
			}
			if (g.extend()) {
				p.part().zScale = g.scale() * Math.max(state.batonExt, 0.01F);
			}
			p.part().xRot = (g.level() && p.parent() != root() ? -p.parent().xRot : 0) + g.rotX();
			if (g.prop() == TowerCrew.PROP_RIFLE) {
				p.part().yRot = (state.act == TowerCrew.ACT_AIM ? RIFLE_OUT : 0) + g.rotY();
			}
		}
		// the head and the hat stay visible: the helmet hangs off this model's head (the crew model hides the real head)
		head.visible = true;
		hat.visible = true;
	}
}
