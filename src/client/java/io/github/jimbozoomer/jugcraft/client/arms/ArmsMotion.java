package io.github.jimbozoomer.jugcraft.client.arms;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.weapons.ArmItem;
import io.github.jimbozoomer.jugcraft.weapons.WeaponArtPayload;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;

/**
 * Arms motion (batch 43, docs/features/arms-motion.md): keyframed attack, guard and parry animations for the arms of
 * batch 42, in third person (the whole body) and first person (the held arm); and the weapon arts of Arms V (batch 48),
 * played when the server says one began ({@link #receive}), over everything else. Client only.
 *
 * <p>The keys are authored in tools/arms_moves.py and written to assets/jugcraft/arms_motion/&lt;kind&gt;.json; they are
 * read once into flat float arrays. Each frame:
 * <ul>
 * <li>{@link #extract} (as the entity's render state is filled) picks the clip: vanilla's own swing is the clock, read
 * with the partial tick, so the motion is smooth at any frame rate and other players' swings need no networking (they
 * already reach every client); each new swing moves the combo on. The pose is evaluated on cubic Hermite splines with
 * Catmull-Rom tangents (a key's tension flattens it), blended into the guard, the use pose and a slow breath;</li>
 * <li>{@link #apply} (after vanilla poses the model) bends the torso at the waist, carries the shoulders and head with
 * it, lays the legs over the walk cycle, and for two-handed kinds points the off arm at the grip or haft;</li>
 * <li>{@link #wrist} turns the held item in the hand; {@link #firstPersonPose} moves the
 * arm on screen.</li>
 * </ul>
 * Nothing here allocates per frame (one track and one {@link ArmsPose} per player holding an arm), and players beyond 32
 * blocks get the arms only. Players only: mobs' models pose their own arms after the shared humanoid pose.
 */
public final class ArmsMotion {
	/** The pose on an entity's render state (Fabric's render state data). */
	public static final RenderStateDataKey<ArmsPose> POSE = RenderStateDataKey.create(() -> "jugcraft:arms_pose");
	public static final List<String> BONE_NAMES = List.of("head", "body", "right_arm", "left_arm", "right_leg", "left_leg", "item");
	static final int HEAD = 0;
	static final int BODY = 1;
	static final int RIGHT_ARM = 2;
	static final int LEFT_ARM = 3;
	static final int RIGHT_LEG = 4;
	static final int LEFT_LEG = 5;
	static final int ITEM = 6;
	static final int CHANNELS = 6;
	public static final int SIZE = 7 * CHANNELS;
	/** The kinds with motion files (tools/arms_moves.py: MOVES). */
	public static final List<String> KINDS = List.of("longsword", "greatsword", "rapier", "flanged_mace", "war_hammer", "glaive",
			"halberd", "spear", "lance", "dagger", "sabre", "estoc", "battle_axe", "flail", "scythe", "quarterstaff", "pike",
			"zweihander", "maul", "executioner", "bill", "labrys", "battleblade", "war_fork", "kama", "war_pick", "twinblade", "nodachi",
			"earthbreaker", "katar", "moonblade", "kusarigama", "katana", "brazier_mace", "javelin", "francisca", "chakram", "harpoon");
	/** Ticks the guard takes to come up when an arm is taken in hand, and a parry or couch to settle. */
	private static final float GUARD_TICKS = 5.0F;
	private static final float USE_TICKS = 4.0F;
	/** After this many ticks without a swing the combo starts again from its first attack. */
	private static final float COMBO_RESET_TICKS = 30.0F;
	/** The longest a weapon art holds its last pose waiting for its next phase (the leap, in the air), in ticks. */
	private static final float ART_HOLD_TICKS = 60.0F;
	/** Beyond this distance (squared, blocks) only the arms move. */
	private static final double FULL_BODY_DISTANCE_SQ = 32.0 * 32.0;
	/** Where the right hand holds an arm, in the arm's space (pixels; ItemInHandLayer's hand point). */
	private static final float HAND_X = -1.0F;
	private static final float HAND_Y = 10.0F;
	private static final float HAND_Z = -2.0F;
	private static final float DEG = (float) (Math.PI / 180.0);
	/** How far vanilla lowers the arm on screen at a full cooldown dip (FirstPersonHandsAndItemsRenderer, blocks). */
	private static final float EQUIP_DROP = 0.6F;

	private static final Map<String, Motion> LIBRARY = new HashMap<>();
	private static final Map<LivingEntity, Track> TRACKS = new WeakHashMap<>();

	// Scratch space for the render thread: matrices are row-major 3x3.
	private static final float[] RV = new float[9];
	private static final float[] RD = new float[9];
	private static final float[] RB = new float[9];
	private static final float[] RA = new float[9];
	private static final float[] RT = new float[9];
	private static final float[] V = new float[3];
	private static final float[] E = new float[3];
	private static final float[] FP = new float[CHANNELS];
	private static final float[] SPIN = new float[1];
	/** The time (0 to 1) of the art clip {@link #artClip} found last. */
	private static float artTime;
	private static final Quaternionf Q = new Quaternionf();

	private ArmsMotion() {
	}

	// ---------------------------------------------------------------- data

	/**
	 * One attack: key times (0 to 1), tensions and poses, and the same for its first-person track. A weapon art's clip
	 * also has its length in ticks, whether it holds its last pose until its next phase, and may turn the whole body
	 * (spin: degrees about the vertical at each key, or null).
	 */
	static final class Clip {
		final float[] times;
		final float[] tension;
		final float[] keys;
		final float[] fpTimes;
		final float[] fpTension;
		final float[] fpKeys;
		float ticks;
		boolean hold;
		float[] spin;

		Clip(float[] times, float[] tension, float[] keys, float[] fpTimes, float[] fpTension, float[] fpKeys) {
			this.times = times;
			this.tension = tension;
			this.keys = keys;
			this.fpTimes = fpTimes;
			this.fpTension = fpTension;
			this.fpKeys = fpKeys;
		}
	}

	/** One kind's motion. */
	static final class Motion {
		int bones;
		float twoHanded;
		float[] hold;
		float[] use;
		float[] fpHold;
		Clip[] attacks;
		/** The weapon art's phases (Arms V), or none. */
		Clip[] arts = new Clip[0];
	}

	/** What is remembered of an entity between frames: the swing seen last, the combo, the guard coming up, its pose. */
	static final class Track {
		final ArmsPose pose = new ArmsPose();
		Object swing;
		int combo = -1;
		float lastSwingTime = -1000.0F;
		Item item;
		float guard;
		float time;
		/** The weapon art phase playing (-1: none), when it began (the entity's ticks) and with what in hand. */
		int artPhase = -1;
		float artStart;
		Item artItem;
	}

	/** Reads every kind's motion file from the mod's resources (once, at client start). */
	public static void load() {
		for (String kind : KINDS) {
			String path = "/assets/" + Jugcraft.MOD_ID + "/arms_motion/" + kind + ".json";
			try (InputStream in = ArmsMotion.class.getResourceAsStream(path)) {
				if (in == null) {
					Jugcraft.LOGGER.warn("No arms motion for {} ({})", kind, path);
					continue;
				}
				LIBRARY.put(kind, parse(JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject()));
			} catch (Exception exception) {
				Jugcraft.LOGGER.error("Could not read the arms motion for {}", kind, exception);
			}
		}
		Jugcraft.LOGGER.info("Arms motion: {} kinds", LIBRARY.size());
	}

	/** The motion for a kind, or null. */
	static Motion motion(String kind) {
		return LIBRARY.get(kind);
	}

	/** For the client game tests: how many kinds' motion was read. */
	public static int loadedKinds() {
		return LIBRARY.size();
	}

	/** For the client game tests: an entity's combo, guard, art phase and this frame's torso turn and arm raise (degrees). */
	public static String describe(LivingEntity entity) {
		Track track = TRACKS.get(entity);
		if (track == null) {
			return "no track";
		}
		float[] v = track.pose.values;
		return "combo " + track.combo + ", guard " + track.guard + ", art phase " + track.artPhase + ", torso turn "
				+ v[BODY * CHANNELS + 1] + ", arm raise " + v[RIGHT_ARM * CHANNELS];
	}

	/** For the client game tests: the weapon art phase an entity is playing, or -1. */
	public static int artPhase(LivingEntity entity) {
		Track track = TRACKS.get(entity);
		return track == null ? -1 : track.artPhase;
	}

	/** How many phases a kind's weapon art has (0: none). */
	public static int artPhases(String kind) {
		Motion motion = LIBRARY.get(kind);
		return motion == null ? 0 : motion.arts.length;
	}

	/** A weapon art's phase began for a player this client sees (from the server): play it from now. */
	public static void receive(WeaponArtPayload payload) {
		Minecraft client = Minecraft.getInstance();
		if (client.level != null && client.level.getEntity(payload.entity()) instanceof LivingEntity entity) {
			playArt(entity, payload.phase());
		}
	}

	static void playArt(LivingEntity entity, int phase) {
		Track track = TRACKS.computeIfAbsent(entity, key -> new Track());
		track.artPhase = phase;
		track.artStart = entity.tickCount;
		track.artItem = entity.getMainHandItem().getItem();
	}

	/**
	 * The weapon art clip playing on this track now (its index; its time in {@link #artTime}), or -1. A clip ends after
	 * its ticks, or holds its last pose (for at most ART_HOLD_TICKS) until its next phase; putting the arm away ends it.
	 */
	private static int artClip(Track track, LivingEntity entity, Motion motion, float now) {
		int phase = track.artPhase;
		if (phase < 0) {
			return -1;
		}
		if (phase >= motion.arts.length || entity.getMainHandItem().getItem() != track.artItem) {
			track.artPhase = -1;
			return -1;
		}
		Clip clip = motion.arts[phase];
		float elapsed = Math.max(0.0F, now - track.artStart);
		if (elapsed < clip.ticks) {
			artTime = elapsed / clip.ticks;
			return phase;
		}
		if (clip.hold && elapsed < clip.ticks + ART_HOLD_TICKS) {
			artTime = 1.0F;
			return phase;
		}
		track.artPhase = -1;
		return -1;
	}

	private static Motion parse(JsonObject json) {
		Motion motion = new Motion();
		motion.bones = json.get("bones").getAsInt();
		motion.twoHanded = json.get("two_handed").getAsFloat();
		motion.hold = floats(json.getAsJsonArray("hold"));
		motion.use = json.has("use") && json.get("use").isJsonArray() ? floats(json.getAsJsonArray("use")) : null;
		motion.fpHold = floats(json.getAsJsonArray("fp_hold"));
		JsonArray attacks = json.getAsJsonArray("attacks");
		motion.attacks = new Clip[attacks.size()];
		for (int i = 0; i < attacks.size(); i++) {
			motion.attacks[i] = clip(attacks.get(i).getAsJsonObject());
		}
		if (json.has("arts")) {
			JsonArray arts = json.getAsJsonArray("arts");
			motion.arts = new Clip[arts.size()];
			for (int i = 0; i < arts.size(); i++) {
				JsonObject art = arts.get(i).getAsJsonObject();
				Clip clip = clip(art);
				clip.ticks = art.get("ticks").getAsFloat();
				clip.hold = art.get("hold").getAsBoolean();
				clip.spin = art.has("spin") && art.get("spin").isJsonArray() ? floats(art.getAsJsonArray("spin")) : null;
				motion.arts[i] = clip;
			}
		}
		return motion;
	}

	private static Clip clip(JsonObject attack) {
		return new Clip(floats(attack.getAsJsonArray("times")), floats(attack.getAsJsonArray("tension")),
				flatten(attack.getAsJsonArray("keys")), floats(attack.getAsJsonArray("fp_times")),
				floats(attack.getAsJsonArray("fp_tension")), flatten(attack.getAsJsonArray("fp_keys")));
	}

	private static float[] floats(JsonArray array) {
		float[] out = new float[array.size()];
		for (int i = 0; i < out.length; i++) {
			out[i] = array.get(i).getAsFloat();
		}
		return out;
	}

	private static float[] flatten(JsonArray rows) {
		int width = rows.get(0).getAsJsonArray().size();
		float[] out = new float[rows.size() * width];
		int i = 0;
		for (JsonElement row : rows) {
			for (JsonElement value : row.getAsJsonArray()) {
				out[i++] = value.getAsFloat();
			}
		}
		return out;
	}

	// ---------------------------------------------------------------- splines

	/**
	 * The values at time t through keys of `stride` channels: cubic Hermite between neighbouring keys, with Catmull-Rom
	 * tangents scaled by (1 - tension) and flat at the ends, so speed carries through a key unless it is meant to stop.
	 */
	static void evaluate(float[] times, float[] tension, float[] keys, int stride, float t, float[] out) {
		int last = times.length - 1;
		if (t <= times[0] || last == 0) {
			System.arraycopy(keys, 0, out, 0, stride);
			return;
		}
		if (t >= times[last]) {
			System.arraycopy(keys, last * stride, out, 0, stride);
			return;
		}
		int i = 0;
		while (i < last - 1 && t > times[i + 1]) {
			i++;
		}
		float span = Math.max(times[i + 1] - times[i], 1.0E-6F);
		float u = (t - times[i]) / span;
		float u2 = u * u;
		float u3 = u2 * u;
		float h00 = 2 * u3 - 3 * u2 + 1;
		float h10 = u3 - 2 * u2 + u;
		float h01 = -2 * u3 + 3 * u2;
		float h11 = u3 - u2;
		for (int c = 0; c < stride; c++) {
			float p0 = keys[i * stride + c];
			float p1 = keys[(i + 1) * stride + c];
			float m0 = tangent(times, tension, keys, stride, i, c) * span;
			float m1 = tangent(times, tension, keys, stride, i + 1, c) * span;
			out[c] = h00 * p0 + h10 * m0 + h01 * p1 + h11 * m1;
		}
	}

	private static float tangent(float[] times, float[] tension, float[] keys, int stride, int i, int c) {
		if (i == 0 || i == times.length - 1) {
			return 0.0F;
		}
		float dt = Math.max(times[i + 1] - times[i - 1], 1.0E-6F);
		return (1.0F - tension[i]) * (keys[(i + 1) * stride + c] - keys[(i - 1) * stride + c]) / dt;
	}

	private static float smooth(float x) {
		x = Math.max(0.0F, Math.min(1.0F, x));
		return x * x * (3.0F - 2.0F * x);
	}

	// ---------------------------------------------------------------- per entity

	/** The entity's track, its combo moved on if a new swing has begun. */
	static Track track(LivingEntity entity, float partialTick) {
		Track track = TRACKS.get(entity);
		if (track == null) {
			track = new Track();
			TRACKS.put(entity, track);
		}
		LivingEntity.SwingDescription swing = entity.getCurrentSwing();
		float now = entity.tickCount + partialTick;
		if (swing != null && swing != track.swing) {
			track.swing = swing;
			track.combo = now - track.lastSwingTime > COMBO_RESET_TICKS ? 0 : track.combo + 1;
			track.lastSwingTime = now;
		}
		return track;
	}

	/** The arm kind an entity's main hand holds, or null. */
	static String kind(ItemStack stack) {
		return stack.getItem() instanceof ArmItem arm ? arm.kind() : null;
	}

	/**
	 * Gives the entity's render state its {@link ArmsPose} for this frame (from the render state extraction of every armed
	 * entity: mixin/client/ArmsRenderStateMixin). Players holding an arm only, and not while swimming, crawling, gliding,
	 * sleeping or spinning (those poses stay vanilla's).
	 */
	public static void extract(LivingEntity entity, ArmedEntityRenderState state, float partialTick) {
		ItemStack main = entity.getMainHandItem();
		String kind = entity instanceof Player ? kind(main) : null;
		Motion motion = kind == null ? null : LIBRARY.get(kind);
		if (motion == null || entity.isVisuallySwimming() || entity.isFallFlying() || entity.isSleeping() || entity.isAutoSpinAttack()) {
			if (state.getData(POSE) != null) {
				state.setData(POSE, null);
			}
			return;
		}
		Track track = track(entity, partialTick);
		ArmsPose pose = track.pose;
		float now = entity.tickCount + partialTick;
		if (track.item != main.getItem()) {
			track.item = main.getItem();
			track.guard = 0.0F;
		} else {
			track.guard = Math.min(1.0F, track.guard + Math.max(0.0F, Math.min(2.0F, now - track.time)) / GUARD_TICKS);
		}
		track.time = now;

		float[] v = pose.values;
		LivingEntity.SwingDescription swing = entity.getCurrentSwing();
		float t = entity.getSwingAnimation(partialTick);
		// A weapon art plays over everything else; its spin turns the whole body (the renderer's body yaw).
		int art = artClip(track, entity, motion, now);
		boolean attacking = art >= 0 || swing != null && t > 0.0F && swing.hand() == InteractionHand.MAIN_HAND && motion.attacks.length > 0;
		if (art >= 0) {
			Clip clip = motion.arts[art];
			evaluate(clip.times, clip.tension, clip.keys, SIZE, artTime, v);
			if (clip.spin != null) {
				evaluate(clip.times, clip.tension, clip.spin, 1, artTime, SPIN);
				state.bodyRot += entity.getMainArm() == HumanoidArm.LEFT ? -SPIN[0] : SPIN[0];
			}
			t = artTime;
		} else if (attacking) {
			Clip clip = motion.attacks[Math.floorMod(track.combo, motion.attacks.length)];
			evaluate(clip.times, clip.tension, clip.keys, SIZE, t, v);
		} else {
			System.arraycopy(motion.hold, 0, v, 0, SIZE);
		}
		if (motion.use != null && entity.isUsingItem() && entity.getUsedItemHand() == InteractionHand.MAIN_HAND) {
			float u = smooth((entity.getTicksUsingItem() + partialTick) / USE_TICKS);
			for (int i = 0; i < SIZE; i++) {
				v[i] += (motion.use[i] - v[i]) * u;
			}
		}
		// A slow breath when not striking: the arms and torso rise and settle, each entity at its own phase.
		float calm = attacking ? 1.0F - smooth(Math.min(1.0F, t * 6.0F)) * smooth(Math.min(1.0F, (1.0F - t) * 6.0F)) : 1.0F;
		float breath = (float) Math.sin(now * 0.075F + entity.getId() * 2.39996F) * calm;
		v[BODY * CHANNELS] += breath * 0.8F;
		v[RIGHT_ARM * CHANNELS] += breath * 1.8F;
		v[LEFT_ARM * CHANNELS] += breath * 1.4F;
		v[ITEM * CHANNELS] += breath * 1.2F;

		// Something in the off hand keeps that arm vanilla's (a shield raised, a torch held); riding keeps the legs.
		int bones = motion.bones;
		float twoHanded = motion.twoHanded;
		if (!entity.getOffhandItem().isEmpty()) {
			bones &= ~(1 << LEFT_ARM);
			twoHanded = 0.0F;
		}
		if (entity.isPassenger()) {
			bones &= ~((1 << RIGHT_LEG) | (1 << LEFT_LEG));
		}
		pose.bones = bones;
		pose.twoHanded = twoHanded;
		pose.leftHanded = entity.getMainArm() == HumanoidArm.LEFT;
		pose.weight = smooth(track.guard);
		pose.active = true;
		state.setData(POSE, pose);
	}

	/** Whether the pose replaces vanilla's arms (the spear's and the lance's keep them). */
	public static boolean overridesArms(ArmedEntityRenderState state) {
		ArmsPose pose = state.getData(POSE);
		return pose != null && pose.active && (pose.bones & (1 << RIGHT_ARM)) != 0;
	}

	// ---------------------------------------------------------------- the model

	/**
	 * Poses the model after vanilla has (mixin/client/ArmsHumanoidModelMixin): the torso turns at the waist by the pose's
	 * body, carrying the head and the shoulders; the arms take the pose (riding on the torso), the legs add theirs to the
	 * walk; a two-handed kind's off arm reaches for the grip. All blended over vanilla's pose by the pose's weight.
	 */
	public static void apply(HumanoidModel<?> model, HumanoidRenderState state) {
		ArmsPose pose = state.getData(POSE);
		if (pose == null || !pose.active || pose.weight <= 0.0F) {
			return;
		}
		float w = pose.weight;
		float[] v = pose.values;
		int bones = pose.bones;
		boolean mirror = pose.leftHanded;
		float s = mirror ? -1.0F : 1.0F;
		boolean full = state.distanceToCameraSq < FULL_BODY_DISTANCE_SQ;
		ModelPart body = model.body;
		ModelPart head = model.head;
		ModelPart main = mirror ? model.leftArm : model.rightArm;
		ModelPart off = mirror ? model.rightArm : model.leftArm;
		ModelPart mainLeg = mirror ? model.leftLeg : model.rightLeg;
		ModelPart offLeg = mirror ? model.rightLeg : model.leftLeg;

		// Vanilla's attack twist of the torso (and of the shoulders with it), faded out as ours comes in.
		float twist = body.yRot * w;
		// The torso: our turn (RD) over vanilla's crouch lean (RV, its attack twist faded out), bent at the waist.
		boolean torso = full && (bones & (1 << BODY)) != 0;
		float bx = torso ? v[BODY * CHANNELS] * DEG * w : 0.0F;
		float by = torso ? s * v[BODY * CHANNELS + 1] * DEG * w : 0.0F;
		float bz = torso ? s * v[BODY * CHANNELS + 2] * DEG * w : 0.0F;
		zyx(RD, bx, by, bz);
		zyx(RV, body.xRot, body.yRot * (1.0F - w), body.zRot);
		mul(RB, RD, RV);
		float neckX = body.x;
		float neckY = body.y;
		float neckZ = body.z;
		// The neck moves as the torso turns about the waist, 12 pixels below it.
		V[0] = 0.0F;
		V[1] = -12.0F;
		V[2] = 0.0F;
		mv(RD, V);
		float nx = neckX + V[0] + (torso ? s * v[BODY * CHANNELS + 3] * w : 0.0F);
		float ny = neckY + 12.0F + V[1] + (torso ? v[BODY * CHANNELS + 4] * w : 0.0F);
		float nz = neckZ + V[2] + (torso ? v[BODY * CHANNELS + 5] * w : 0.0F);
		euler(RB, E);
		body.xRot = E[0];
		body.yRot = E[1];
		body.zRot = E[2];
		body.x = nx;
		body.y = ny;
		body.z = nz;

		// The head rides on the torso's turn and adds its own (a counter-turn keeps the eyes on the target).
		if (full) {
			boolean ownHead = (bones & (1 << HEAD)) != 0;
			zyx(RT, ownHead ? v[0] * DEG * w : 0.0F, ownHead ? s * v[1] * DEG * w : 0.0F, ownHead ? s * v[2] * DEG * w : 0.0F);
			zyx(RA, head.xRot, head.yRot, head.zRot);
			mul(RV, RT, RA);
			mul(RA, RD, RV);
			euler(RA, E);
			head.xRot = E[0];
			head.yRot = E[1];
			head.zRot = E[2];
			head.x += nx - neckX;
			head.y += ny - neckY;
			head.z += nz - neckZ;
		}

		// The arms: their shoulders ride on the torso; their own turn is the pose's (or vanilla's, for spear and lance).
		placeArm(main, RIGHT_ARM, v, bones, w, s, twist, nx, ny, nz, neckX, neckY, neckZ);
		placeArm(off, LEFT_ARM, v, bones, w, s, twist, nx, ny, nz, neckX, neckY, neckZ);

		// The legs add the pose's step to the walk.
		if (full) {
			addLeg(mainLeg, RIGHT_LEG, v, bones, w, s);
			addLeg(offLeg, LEFT_LEG, v, bones, w, s);
		}

		// Two hands on the weapon: the off arm points from its shoulder at the grip below the main hand.
		if (pose.twoHanded > 0.0F && (bones & (1 << RIGHT_ARM)) != 0) {
			reach(main, off, v, s, pose.twoHanded, w);
		}
	}

	private static void placeArm(ModelPart arm, int bone, float[] v, int bones, float w, float s, float twist, float nx, float ny,
			float nz, float neckX, float neckY, float neckZ) {
		// The shoulder: vanilla's place relative to the neck, its attack twist taken out, turned with the torso.
		float ax = arm.x - neckX;
		float az = arm.z - neckZ;
		float c = (float) Math.cos(twist);
		float sn = (float) Math.sin(twist);
		V[0] = ax * c - az * sn;
		V[1] = arm.y - neckY;
		V[2] = ax * sn + az * c;
		mv(RD, V);
		float px = nx + V[0];
		float py = ny + V[1];
		float pz = nz + V[2];
		int o = bone * CHANNELS;
		if ((bones & (1 << bone)) != 0) {
			zyx(RT, v[o] * DEG, s * v[o + 1] * DEG, s * v[o + 2] * DEG);
			mul(RA, RD, RT);
			euler(RA, E);
			arm.xRot += (E[0] - arm.xRot) * w;
			arm.yRot += (E[1] - arm.yRot) * w;
			arm.zRot += (E[2] - arm.zRot) * w;
			px += s * v[o + 3] * w;
			py += v[o + 4] * w;
			pz += v[o + 5] * w;
		} else {
			zyx(RT, arm.xRot, arm.yRot, arm.zRot);
			mul(RA, RD, RT);
			euler(RA, E);
			arm.xRot = E[0];
			arm.yRot = E[1];
			arm.zRot = E[2];
		}
		arm.x = px;
		arm.y = py;
		arm.z = pz;
	}

	private static void addLeg(ModelPart leg, int bone, float[] v, int bones, float w, float s) {
		if ((bones & (1 << bone)) == 0) {
			return;
		}
		int o = bone * CHANNELS;
		leg.xRot += v[o] * DEG * w;
		leg.yRot += s * v[o + 1] * DEG * w;
		leg.zRot += s * v[o + 2] * DEG * w;
		leg.x += s * v[o + 3] * w;
		leg.y += v[o + 4] * w;
		leg.z += v[o + 5] * w;
	}

	/**
	 * Points the off arm at the weapon `spread` pixels below the main hand. The blade's direction in the main arm comes
	 * from the item bone's wrist turn, applied in ItemInHandLayer's hand frame as {@link #wrist} does.
	 */
	private static void reach(ModelPart main, ModelPart off, float[] v, float s, float spread, float w) {
		int o = ITEM * CHANNELS;
		bladeInArm(v[o] * DEG, s * v[o + 1] * DEG, s * v[o + 2] * DEG, V);
		float dx = V[0];
		float dy = V[1];
		float dz = V[2];
		// The grip in the main arm's space: the hand point, a pixel and a half along the blade, then back down it.
		float gx = s * (HAND_X + v[o + 3]) + dx * (1.5F - spread);
		float gy = HAND_Y + v[o + 4] + dy * (1.5F - spread);
		float gz = HAND_Z + v[o + 5] + dz * (1.5F - spread);
		zyx(RA, main.xRot, main.yRot, main.zRot);
		V[0] = gx;
		V[1] = gy;
		V[2] = gz;
		mv(RA, V);
		float tx = main.x + V[0] - off.x;
		float ty = main.y + V[1] - off.y;
		float tz = main.z + V[2] - off.z;
		float n = (float) Math.sqrt(tx * tx + ty * ty + tz * tz);
		if (n < 1.0E-4F) {
			return;
		}
		tx /= n;
		ty /= n;
		tz /= n;
		float sin = -(float) Math.sqrt(Math.max(0.0F, 1.0F - ty * ty));
		float rx = (float) Math.atan2(sin, ty);
		float ry = sin < -1.0E-4F ? (float) Math.atan2(-tx, -tz) : off.yRot;
		off.xRot += (rx - off.xRot) * w;
		off.yRot += (wrapNear(ry, off.yRot) - off.yRot) * w;
		off.zRot += (0.0F - off.zRot) * w;
	}

	/** The blade's direction in the arm's space after a wrist turn (rx, ry, rz) in ItemInHandLayer's hand frame. */
	static void bladeInArm(float rx, float ry, float rz, float[] out) {
		// In the hand frame (after XP -90, YP 180) the blade runs along +y, a little towards -z.
		out[0] = 0.0F;
		out[1] = 0.9842F;
		out[2] = -0.1772F;
		zyx(RT, rx, ry, rz);
		mv(RT, out);
		// Back to the arm's space: XP -90 then YP 180 (x -> -x, z -> -z), then the X turn (y, z) -> (z, -y).
		float x = -out[0];
		float y = out[1];
		float z = -out[2];
		out[0] = x;
		out[1] = z;
		out[2] = -y;
	}

	private static float wrapNear(float angle, float near) {
		float twoPi = (float) (Math.PI * 2.0);
		while (angle - near > Math.PI) {
			angle -= twoPi;
		}
		while (angle - near < -Math.PI) {
			angle += twoPi;
		}
		return angle;
	}

	// ---------------------------------------------------------------- the held item

	/** Turns the held item in the main hand by the pose's item bone (mixin/client/ArmsItemInHandLayerMixin). */
	public static void wrist(ArmedEntityRenderState state, HumanoidArm arm, PoseStack poseStack) {
		ArmsPose pose = state.getData(POSE);
		if (pose == null || !pose.active || (pose.bones & (1 << ITEM)) == 0 || arm != state.mainArm) {
			return;
		}
		float[] v = pose.values;
		float s = pose.leftHanded ? -1.0F : 1.0F;
		int o = ITEM * CHANNELS;
		float w = pose.weight;
		// The bone's offset is in the arm's space; the hand's frame here is that turned by XP -90 then YP 180.
		poseStack.translate(-s * v[o + 3] * w / 16.0F, -v[o + 5] * w / 16.0F, -v[o + 4] * w / 16.0F);
		poseStack.rotate(Q.rotationZYX(s * v[o + 2] * DEG * w, s * v[o + 1] * DEG * w, v[o] * DEG * w));
	}

	// ---------------------------------------------------------------- first person
	//
	// Vanilla draws no arm on screen while the hand holds an item, so first person moves the held arm alone: after
	// FirstPersonHandsAndItemsRenderer places the hand (applyItemArmTransform), the guard or the attack's key pose is
	// applied there, and vanilla's own swing for the hand is skipped (mixin/client/ArmsFirstPersonMixin).

	private static Motion firstPerson;
	private static int firstPersonArt = -1;
	private static float firstPersonArtTime;
	private static float firstPersonProgress;
	private static float firstPersonUse;
	private static LivingEntity firstPersonPlayer;
	private static float firstPersonPartial;

	/** Before the main hand is drawn on screen: whether it holds an arm whose swing this replaces, and how far it is. */
	public static void beginFirstPerson(LivingEntity player, ItemStack stack, InteractionHand hand, float swingProgress, float partialTick) {
		firstPerson = null;
		if (player == null || hand != InteractionHand.MAIN_HAND) {
			return;
		}
		String kind = kind(stack);
		Motion motion = kind == null ? null : LIBRARY.get(kind);
		if (motion == null || (motion.bones & (1 << RIGHT_ARM)) == 0 || motion.attacks.length == 0) {
			return;
		}
		firstPerson = motion;
		firstPersonProgress = swingProgress;
		firstPersonPlayer = player;
		firstPersonPartial = partialTick;
		firstPersonArt = artClip(track(player, partialTick), player, motion, player.tickCount + partialTick);
		firstPersonArtTime = artTime;
		boolean using = player.isUsingItem() && player.getUsedItemHand() == InteractionHand.MAIN_HAND;
		firstPersonUse = using ? smooth((player.getTicksUsingItem() + partialTick) / USE_TICKS) : 0.0F;
	}

	public static void endFirstPerson() {
		firstPerson = null;
		firstPersonPlayer = null;
	}

	/** Whether the hand being drawn on screen holds an arm with motion (vanilla's swing for it is then skipped). */
	public static boolean firstPersonActive() {
		return firstPerson != null;
	}

	/**
	 * After vanilla places the hand on screen (lowered by {@code equip} while the attack cooldown recovers): the guard, or
	 * the attack at the swing's progress (the combo's clip). Through a stroke the arm is held up against vanilla's dip,
	 * which comes back as the stroke settles, so a slow arm's blow is seen and its cooldown still shows after it.
	 */
	public static void firstPersonPose(PoseStack poseStack, HumanoidArm arm, float equip) {
		Motion motion = firstPerson;
		if (motion == null) {
			return;
		}
		if (firstPersonArt >= 0) {
			// A weapon art holds the arm up against vanilla's dip all through.
			poseStack.translate(0.0F, EQUIP_DROP * equip, 0.0F);
			Clip clip = motion.arts[firstPersonArt];
			evaluate(clip.fpTimes, clip.fpTension, clip.fpKeys, CHANNELS, firstPersonArtTime, FP);
		} else if (firstPersonProgress > 0.0F) {
			float t = firstPersonProgress;
			float held = smooth(t / 0.1F) * (1.0F - smooth((t - 0.6F) / 0.4F));
			poseStack.translate(0.0F, EQUIP_DROP * equip * held, 0.0F);
			Track track = track(firstPersonPlayer, firstPersonPartial);
			Clip clip = motion.attacks[Math.floorMod(track.combo, motion.attacks.length)];
			evaluate(clip.fpTimes, clip.fpTension, clip.fpKeys, CHANNELS, firstPersonProgress, FP);
		} else {
			// The guard eases out as a parry comes up (vanilla's blocking pose is drawn on top).
			float keep = 1.0F - firstPersonUse;
			for (int i = 0; i < CHANNELS; i++) {
				FP[i] = motion.fpHold[i] * keep;
			}
		}
		float s = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
		poseStack.translate(s * FP[3] / 16.0F, FP[4] / 16.0F, FP[5] / 16.0F);
		poseStack.rotate(Q.rotationZYX(s * FP[2] * DEG, s * FP[1] * DEG, FP[0] * DEG));
	}

	// ---------------------------------------------------------------- 3x3 maths (row-major, ModelPart's Z*Y*X order)

	static void zyx(float[] m, float x, float y, float z) {
		float cx = (float) Math.cos(x);
		float sx = (float) Math.sin(x);
		float cy = (float) Math.cos(y);
		float sy = (float) Math.sin(y);
		float cz = (float) Math.cos(z);
		float sz = (float) Math.sin(z);
		m[0] = cz * cy;
		m[1] = cz * sy * sx - sz * cx;
		m[2] = cz * sy * cx + sz * sx;
		m[3] = sz * cy;
		m[4] = sz * sy * sx + cz * cx;
		m[5] = sz * sy * cx - cz * sx;
		m[6] = -sy;
		m[7] = cy * sx;
		m[8] = cy * cx;
	}

	static void mul(float[] out, float[] a, float[] b) {
		for (int i = 0; i < 3; i++) {
			float a0 = a[i * 3];
			float a1 = a[i * 3 + 1];
			float a2 = a[i * 3 + 2];
			out[i * 3] = a0 * b[0] + a1 * b[3] + a2 * b[6];
			out[i * 3 + 1] = a0 * b[1] + a1 * b[4] + a2 * b[7];
			out[i * 3 + 2] = a0 * b[2] + a1 * b[5] + a2 * b[8];
		}
	}

	static void mv(float[] m, float[] v) {
		float x = v[0];
		float y = v[1];
		float z = v[2];
		v[0] = m[0] * x + m[1] * y + m[2] * z;
		v[1] = m[3] * x + m[4] * y + m[5] * z;
		v[2] = m[6] * x + m[7] * y + m[8] * z;
	}

	/** Back from a matrix to ModelPart's (xRot, yRot, zRot). */
	static void euler(float[] m, float[] out) {
		float sy = Math.max(-1.0F, Math.min(1.0F, -m[6]));
		out[1] = (float) Math.asin(sy);
		if (Math.abs(sy) < 0.9999F) {
			out[0] = (float) Math.atan2(m[7], m[8]);
			out[2] = (float) Math.atan2(m[3], m[0]);
		} else {
			out[0] = (float) Math.atan2(-m[5], m[4]);
			out[2] = 0.0F;
		}
	}
}
