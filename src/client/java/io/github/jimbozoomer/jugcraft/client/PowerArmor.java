package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.tower.crew.PlasmaBaton;
import io.github.jimbozoomer.jugcraft.tower.crew.PlasmaBlade;
import io.github.jimbozoomer.jugcraft.tower.crew.TowerCrew;
import io.github.jimbozoomer.jugcraft.tower.squad.Legionary;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

/**
 * The Narv-Tech power armour on the security detail and the drop squads' legionaries (Justin, 9 November 2026: "make the
 * full set of Narv-Tech power armor for the legionaries/security. I want it to look cool, be black and purple themed";
 * the power armour sheets, built on his "Build it"): tools/power_armor.py's mesh (assets/jugcraft/crew/armor.bin.gz, a
 * group of faceted plates for each bone, in that bone's own space) drawn on the wearer's bones as they stand, its lit
 * faces added again full bright from the glow sheet (as spider eyes glow). The helmet (the legion's helm) is the head's
 * group, worn with {@link TowerCrew#GEAR_HELMET} (the crew model hides the head under it), a squad leader's crest with
 * his rank's bit; the rest with {@link TowerCrew#GEAR_EXO}; all of it off to sleep, the pack on the back put by while
 * they sit.
 *
 * <p>The right vambrace carries the plasma blade's arm mount (tower/crew/PlasmaBlade). Its moving parts are groups of
 * their own, moved here through the deployment ({@link PlasmaBlade#DEPLOY_TICKS} ticks, timed on this client from when it
 * first sees the blade out; the same back in when it's stowed): the two shutters swing out to stand as prongs and the
 * cooling fins slide out of the breech; the carriage runs down its rails driving the spine out, three telescoping
 * segments behind a point; the plasma runs up the spine. The fins' ends glow while it's lit and a second after. Lit, it
 * hums (the baton's hum, deeper).
 */
final class PowerArmor {
	private static final Identifier GLOW = Jugcraft.id("textures/entity/power_armor/armor_glow.png");
	// the deployment's numbers (tools/power_armor.py's, in pixels and shares of the deployment)
	private static final float SEG = 3.5F, TIP = 1.3F, FULL = 3 * SEG + TIP;
	private static final float[] LEAD = {0, TIP, TIP + SEG, TIP + 2 * SEG};
	private static final float FIN_OUT = 0.42F, CARRIAGE_RUN = 0.85F, SHUTTER_SWING = 100F;
	private static final float OPEN_BY = 0.17F, SPINE_FROM = 0.17F, SPINE_BY = 0.59F, PLASMA_FROM = 0.62F, PLASMA_BY = 0.95F;
	/** Ticks the fins' ends go on glowing after the blade's stowed. */
	private static final int COOL_TICKS = 20;
	private static final float HUM_VOLUME = 0.14F;

	private PowerArmor() {
	}

	private static final class Blade {
		boolean out;
		int since;
		@Nullable Hum hum;
	}

	/** Each wearer's blade as this client last saw it (out or not, and since which of their ticks). */
	private static final Map<Entity, Blade> BLADES = new WeakHashMap<>();

	/**
	 * Notes {@code wearer}'s blade for this frame from what they're doing ({@code act}): how far it's deployed (0 stowed
	 * to 1 lit) and whether its fins still glow, into {@code state}; starts its hum once it's lit.
	 */
	static void track(Entity wearer, int act, float partialTick, TowerCrewRenderer.State state) {
		boolean out = PlasmaBlade.out(act);
		Blade b = BLADES.get(wearer);
		if (b == null) {
			b = new Blade();
			b.out = out;
			b.since = wearer.tickCount - (out ? PlasmaBlade.DEPLOY_TICKS : 1000);      // (first seen with it out: out already)
			BLADES.put(wearer, b);
		}
		if (b.out != out) {
			b.out = out;
			b.since = wearer.tickCount;
		}
		float t = Mth.clamp((wearer.tickCount - b.since + partialTick) / PlasmaBlade.DEPLOY_TICKS, 0F, 1F);
		state.blade = out ? t : 1 - t;
		state.bladeCooling = !out && wearer.tickCount - b.since < PlasmaBlade.DEPLOY_TICKS + COOL_TICKS;
		Minecraft client = Minecraft.getInstance();
		if (out && b.hum == null && wearer.tickCount - b.since >= PlasmaBlade.DEPLOY_TICKS && client.player != null
				&& client.player.distanceToSqr(wearer) < 24 * 24) {
			b.hum = new Hum(wearer, b);
			client.getSoundManager().play(b.hum);
		}
	}

	/** The lit blade's hum: follows its wearer, stops the moment the blade's stowed (the power-down carries on from it). */
	private static final class Hum extends AbstractTickableSoundInstance {
		private final Entity wearer;
		private final Blade blade;

		Hum(Entity wearer, Blade blade) {
			super(PlasmaBaton.HUM, SoundSource.HOSTILE, RandomSource.create());
			this.wearer = wearer;
			this.blade = blade;
			looping = true;
			delay = 0;
			volume = HUM_VOLUME;
			pitch = PlasmaBlade.PITCH;
			follow();
		}

		private void follow() {
			x = wearer.getX();
			y = wearer.getY() + 1.0;
			z = wearer.getZ();
		}

		@Override
		public void tick() {
			if (wearer.isRemoved() || !blade.out) {
				blade.hum = null;
				stop();
				return;
			}
			follow();
		}
	}

	private static float share(float d, float from, float by) {
		return Mth.clamp((d - from) / (by - from), 0F, 1F);
	}

	/** The armour, drawn over the crew member's model and props. */
	static final class Layer extends RenderLayer<TowerCrewRenderer.State, TowerCrewModel> {
		private final TowerCrewPropsModel props;

		Layer(RenderLayerParent<TowerCrewRenderer.State, TowerCrewModel> parent, TowerCrewPropsModel props) {
			super(parent);
			this.props = props;
		}

		@Override
		public void submit(PoseStack pose, SubmitNodeCollector collector, int light, TowerCrewRenderer.State state, float yRot, float xRot) {
			int gear = state.gear;
			if (state.isInvisible || TowerCrew.asleep(state.act) || (gear & (TowerCrew.GEAR_HELMET | TowerCrew.GEAR_EXO)) == 0) {
				return;
			}
			CrewShipMesh mesh = CrewShipMesh.of("crew/armor.bin.gz", "power_armor");
			if (mesh == null) {
				return;
			}
			props.setupAnim(state);                     // the bones as they stand
			// where plasma has cut some of a limb off them (or a plasma blow has killed them: they've come apart), that limb's
			// plates go with it (PlasmaWoundsClient.lost: the plates aren't model boxes, so they can't be cut along the wound)
			if (PlasmaWoundsClient.vanished(state)) {
				return;
			}
			if ((gear & TowerCrew.GEAR_HELMET) != 0 && !PlasmaWoundsClient.lost(state, "/head")) {
				onBone(pose, props.head);
				part(mesh, "head", pose, collector, light);
				for (int rank = 0; rank < Legionary.CRESTS.length; rank++) {
					if ((gear & Legionary.CRESTS[rank]) != 0) {
						part(mesh, "crest_" + rank, pose, collector, light);
					}
				}
				pose.popPose();
			}
			if ((gear & TowerCrew.GEAR_EXO) == 0) {
				return;
			}
			if (!PlasmaWoundsClient.lost(state, "/body")) {
				onBone(pose, props.body);
				part(mesh, "body", pose, collector, light);
				if (!state.isPassenger) {
					part(mesh, "back", pose, collector, light);       // the pack: put by while they sit
				}
				pose.popPose();
			}
			if (!PlasmaWoundsClient.lost(state, "/right_arm")) {
				onBone(pose, props.rightArm);
				part(mesh, "right_arm", pose, collector, light);
				blade(mesh, state.blade, state.bladeCooling, pose, collector, light);
				pose.popPose();
			}
			if (!PlasmaWoundsClient.lost(state, "/left_arm")) {
				limb(mesh, "left_arm", props.leftArm, pose, collector, light);
			}
			if (!PlasmaWoundsClient.lost(state, "/right_leg")) {
				limb(mesh, "right_leg", props.rightLeg, pose, collector, light);
			}
			if (!PlasmaWoundsClient.lost(state, "/left_leg")) {
				limb(mesh, "left_leg", props.leftLeg, pose, collector, light);
			}
		}

		private void limb(CrewShipMesh mesh, String name, ModelPart bone, PoseStack pose, SubmitNodeCollector collector, int light) {
			onBone(pose, bone);
			part(mesh, name, pose, collector, light);
			pose.popPose();
		}

		/** Pushes {@code pose} into {@code bone}'s space, in pixels (the caller pops it). */
		private void onBone(PoseStack pose, ModelPart bone) {
			pose.pushPose();
			props.root().translateAndRotate(pose);
			bone.translateAndRotate(pose);
			pose.scale(1 / 16F, 1 / 16F, 1 / 16F);
		}

		/** The blade's moving parts at {@code d} of its deployment (the pose in the right arm's space). */
		private static void blade(CrewShipMesh mesh, float d, boolean cooling, PoseStack pose, SubmitNodeCollector collector, int light) {
			float open = share(d, 0, OPEN_BY), ext = share(d, SPINE_FROM, SPINE_BY), run = share(d, PLASMA_FROM, PLASMA_BY);
			boolean hot = run > 0;
			for (int side = 0; side < 2; side++) {
				String ab = side == 0 ? "a" : "b";
				float sign = side == 0 ? 1 : -1;
				pivoted(mesh, "shutter_" + ab, pose);
				pose.rotateDegrees(Axis.XP, sign * SHUTTER_SWING * open);
				draw(mesh, "shutter_" + ab, pose, collector, light);
				if (hot) {
					draw(mesh, "shutter_" + ab + "_hot", pose, collector, light);
				}
				pose.popPose();
				pose.pushPose();
				pose.translate(0, 0, -sign * FIN_OUT * (1 - open));
				part(mesh, "fins_" + ab, pose, collector, light);
				if (hot || cooling) {
					part(mesh, "fins_" + ab + "_hot", pose, collector, light);
				}
				pose.popPose();
			}
			pose.pushPose();
			pose.translate(0, CARRIAGE_RUN * ext, 0);
			part(mesh, "carriage", pose, collector, light);
			pose.popPose();
			if (ext > 0.02F) {
				part(mesh, "slot_lit", pose, collector, light);
			}
			float s = ext * FULL;
			for (int i = 0; i < LEAD.length; i++) {
				if (s > LEAD[i]) {                           // (not yet started, it's nested in the mount)
					pose.pushPose();
					pose.translate(0, s - FULL, 0);
					part(mesh, "spine_" + i, pose, collector, light);
					pose.popPose();
				}
			}
			if (run > 0.01F) {
				pivoted(mesh, "plasma", pose);
				pose.scale(1, run, 1);
				draw(mesh, "plasma", pose, collector, light);
				pose.popPose();
			}
		}

		/** Pushes {@code pose} to a group's pivot (the caller turns or scales it there, draws and pops). */
		private static void pivoted(CrewShipMesh mesh, String name, PoseStack pose) {
			pose.pushPose();
			CrewShipMesh.Part part = mesh.parts.get(name);
			if (part != null) {
				pose.translate(part.px(), part.py(), part.pz());
			}
		}

		/** A group, about its pivot. */
		private static void part(CrewShipMesh mesh, String name, PoseStack pose, SubmitNodeCollector collector, int light) {
			pivoted(mesh, name, pose);
			draw(mesh, name, pose, collector, light);
			pose.popPose();
		}

		/** A group's faces as the pose stands (at its pivot): lit by the world, the lit ones again full bright from the
		 * glow sheet. */
		private static void draw(CrewShipMesh mesh, String name, PoseStack pose, SubmitNodeCollector collector, int light) {
			CrewShipMesh.Part part = mesh.parts.get(name);
			if (part == null) {
				return;
			}
			for (CrewShipMesh.Batch b : part.batches()) {
				quads(b, pose, collector.order(1), mesh.types[b.material()], light);
				if (b.glow()) {
					quads(b, pose, collector.order(2), RenderTypes.eyes(GLOW), light);
				}
			}
		}

		private static void quads(CrewShipMesh.Batch b, PoseStack pose, net.minecraft.client.renderer.OrderedSubmitNodeCollector collector, RenderType type,
				int light) {
			collector.submitCustomGeometry(pose, type, (matrix, buffer) -> {
				float[] pos = b.pos(), uv = b.uv(), n = b.normal();
				Vector3f at = new Vector3f(), nn = new Vector3f();
				for (int v = 0, nv = pos.length / 3; v < nv; v++) {
					matrix.normal().transform(n[v * 3], n[v * 3 + 1], n[v * 3 + 2], nn).normalize();
					matrix.pose().transformPosition(pos[v * 3], pos[v * 3 + 1], pos[v * 3 + 2], at);
					buffer.addVertex(at.x, at.y, at.z, 0xFFFFFFFF, uv[v * 2], uv[v * 2 + 1], OverlayTexture.NO_OVERLAY, light, nn.x, nn.y, nn.z);
				}
			});
		}
	}
}
