package io.github.jimbozoomer.jugcraft.client.guns;

import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.LoopType;
import io.github.jimbozoomer.jugcraft.guns.GunItem;
import io.github.jimbozoomer.jugcraft.guns.GunSpec;
import io.github.jimbozoomer.jugcraft.guns.JugcraftGuns;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * The guns' GeckoLib animation controller: the owner's idle loops; draw, shoot, aim_shoot, inspect and the reloads are
 * triggered (by GunsClient for the player's own gun, by the server's GunActionPayload for others'), each played once
 * and back to idle. A gun loaded a shell at a time has one reload per shell count: reload_start, reload_loop that many
 * times, reload_stop. The animations' sound keyframes play the guns' sounds where the gun is, and their "eject_casing"
 * particle keyframes throw a spent casing ({@link GunEffects#eject}). A magazine change plays faster or slower with the
 * gun's magazine attachment, so that it ends when the reload does.
 */
public final class GunAnimations {
	public static final String CONTROLLER = "main";
	/** The animations' particle cue for a spent casing (tools/guns.py EJECT_CUE). */
	static final String EJECT_CUE = "eject_casing";
	private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
	/** Animation sound keyframes that play another event's sound (tools/guns.py EVENT_SOUNDS: "rustle" is a rustle). */
	public static final Map<String, String> SOUND_ALIASES = Map.of("rustle", "gun_rustle");
	/**
	 * The same, per gun: the shell-at-a-time guns' loops push a shell or a round, not a magazine; the Squall Rifle's
	 * canister clanks, it is no ramrod (slice 9B), and so does the Throttle's bulb as it is twisted shut (slice 9D), and
	 * the Earthmover's drum as it twists free and home; the Skylark Rifle and the Bullfrog push a round in (slice 10A); the
	 * Glowmouth is loaded a charge at a time, as the Stormlock Rifle is (slice 10B); the Mule pushes its shells in, as the
	 * Coach Gun does (slice 10C), and the Reverb a shell at a time (slice 10D); of the Nether guns, the Tusker pushes a shell
	 * in at a time and the Bastion Rifle a round (slice 11A), and the Crackling a shell (slice 11B).
	 */
	public static final Map<String, Map<String, String>> GUN_SOUND_ALIASES = Map.ofEntries(
			Map.entry("thunderpipe", Map.of("reload_mag_in", "shell_in")),
			Map.entry("haymaker", Map.of("reload_mag_in", "shell_in")),
			Map.entry("longhorn_rifle", Map.of("reload_mag_in", "shell_in")),
			Map.entry("drover_rifle", Map.of("reload_mag_in", "shell_in")),
			Map.entry("coach_gun", Map.of("reload_mag_in", "shell_in")),
			Map.entry("stormlock_rifle", Map.of("reload_mag_in", "insert")),
			Map.entry("linesman", Map.of("reload_mag_in", "insert")),
			Map.entry("squall_rifle", Map.of("metal", "clank")),
			Map.entry("sledge", Map.of("reload_mag_in", "shell_in")),
			Map.entry("highwayman", Map.of("reload_mag_in", "shell_in")),
			Map.entry("throttle", Map.of("reload_mag_in", "shell_in", "metal", "clank")),
			Map.entry("earthmover", Map.of("metal", "clank")),
			Map.entry("skylark_rifle", Map.of("reload_mag_in", "shell_in")),
			Map.entry("bullfrog", Map.of("reload_mag_in", "shell_in")),
			Map.entry("glowmouth", Map.of("reload_mag_in", "insert")),
			Map.entry("mule", Map.of("reload_mag_in", "shell_in")),
			Map.entry("reverb", Map.of("reload_mag_in", "shell_in")),
			Map.entry("tusker", Map.of("reload_mag_in", "shell_in")),
			Map.entry("bastion_rifle", Map.of("reload_mag_in", "shell_in")),
			Map.entry("crackling", Map.of("reload_mag_in", "shell_in")));

	private GunAnimations() {
	}

	public static void register(GunItem gun, AnimatableManager.ControllerRegistrar registrar) {
		AnimationController<GunItem> controller = new AnimationController<GunItem>(CONTROLLER, 1, test -> test.setAndContinue(IDLE));
		for (String name : new String[] {"draw", "shoot", "aim_shoot", "inspect"}) {
			controller.triggerableAnim(name, once(name));
		}
		controller.triggerableAnim("idle", IDLE);
		GunSpec spec = gun.spec();
		if (spec.byShell()) {
			for (int shells = 1; shells <= spec.capacity(); shells++) {
				controller.triggerableAnim(reloadName(spec, shells), RawAnimation.begin().then("reload_start", LoopType.PLAY_ONCE)
						.thenPlayXTimes("reload_loop", shells).then("reload_stop", LoopType.PLAY_ONCE).thenLoop("idle"));
			}
		} else {
			controller.triggerableAnim("reload", once("reload"));
		}
		controller.setSoundKeyframeHandler(event -> {
			Integer owner = event.renderState().getGeckolibData(GunRenderer.OWNER);
			play(gun, event.keyframeData().getSound(), owner == null ? -1 : owner);
		});
		// The owner's particle cues: a spent casing at "eject_casing" (the others mark points in a reload and show nothing).
		controller.setParticleKeyframeHandler(event -> {
			Integer owner = event.renderState().getGeckolibData(GunRenderer.OWNER);
			if (owner != null && EJECT_CUE.equals(event.keyframeData().getEffect())) {
				GunEffects.eject(gun, owner, event.renderState().getGeckolibData(GunRenderer.LEFT));
			}
		});
		registrar.add(controller);
	}

	/** The triggerable reload that loads this many rounds. */
	public static String reloadName(GunSpec spec, int rounds) {
		return spec.byShell() ? "reload_" + Math.max(1, Math.min(spec.capacity(), rounds)) : "reload";
	}

	private static RawAnimation once(String name) {
		return RawAnimation.begin().then(name, LoopType.PLAY_ONCE).thenLoop("idle");
	}

	/** Plays this gun animation on the gun in this entity's main hand (on this client only). */
	public static void trigger(LivingEntity holder, String animation) {
		trigger(holder, InteractionHand.MAIN_HAND, animation);
	}

	/** Plays this gun animation on the gun in this hand of this entity (slice 10G: a gun in each hand; this client only). */
	public static void trigger(LivingEntity holder, InteractionHand hand, String animation) {
		ItemStack stack = holder.getItemInHand(hand);
		if (stack.getItem() instanceof GunItem gun) {
			long id = GeoItem.getId(stack);
			AnimatableManager<GunItem> manager = gun.getAnimatableInstanceCache().getManagerForId(id);
			AnimationController<GunItem> controller = manager.getAnimationControllers().get(CONTROLLER);
			if (controller != null) {
				// The owner's magazine change lasts the gun's own reload; a speed or extended magazine changes that.
				controller.setAnimationSpeed(animation.equals("reload") && !gun.spec().byShell()
						? (double) gun.spec().reload() / GunItem.spec(stack).reload() : 1.0);
			}
			gun.triggerAnim(holder, id, CONTROLLER, animation);
		}
	}

	/** An animation's sound, at the entity holding the gun (the player's own when unknown). */
	static void play(GunItem gun, String event, int ownerId) {
		Minecraft client = Minecraft.getInstance();
		ClientLevel level = client.level;
		if (level == null) {
			return;
		}
		String name = GUN_SOUND_ALIASES.getOrDefault(gun.name(), Map.of()).getOrDefault(event, SOUND_ALIASES.getOrDefault(event, event));
		if (!JugcraftGuns.SOUND_EVENTS.contains(name)) {
			return;
		}
		Entity at = ownerId >= 0 ? level.getEntity(ownerId) : client.player;
		if (at == null) {
			at = client.player;
		}
		if (at == null) {
			return;
		}
		SoundEvent sound = JugcraftGuns.sound("guns." + name);
		level.playLocalSound(at.getX(), at.getEyeY(), at.getZ(), sound, SoundSource.PLAYERS, 0.8F, 1.0F, false);
	}
}
