package io.github.jimbozoomer.jugcraft.client.guns;

import io.github.jimbozoomer.jugcraft.guns.GunItem;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * How each gun looks in use, on the client only (tools/guns.py: ZOOM, two_handed(), FLASH_SIZE, the attachments'
 * "hides_flash" and the scopes' "zoom" and "view"; check_guns in tools/check_mod_data.py keeps them the same).
 */
public final class GunLooks {
	/** Each gun's look, by name. */
	static final Map<String, Look> LOOKS = new HashMap<>();
	/** How big a shot's muzzle flash is, across, in the gun model's pixels, by the round it fires (or grenade or fuel). */
	static final Map<String, Float> FLASH_SIZES = Map.of("light_round", 5.0F, "rifle_round", 7.0F, "buckshot_shell", 8.0F,
			"paper_cartridge", 10.0F, "grenade", 9.0F, "minecraft:blaze_powder", 8.0F, "energy_cell", 6.0F, "he_rocket", 10.0F);
	/**
	 * The flash's tint, RGB, multiplied into the owner's white-gold frames, by the round, where it has one (tools/guns.py
	 * FLASH_TINT): an energy weapon's discharge is cyan-white (slice 8D).
	 */
	static final Map<String, Integer> FLASH_TINTS = Map.of("energy_cell", 0x9FF4FF);
	/** The attachments that hide the flash: cans over the muzzle. */
	static final List<String> HIDE_FLASH = List.of("silencer", "baffled_silencer");
	/** The scopes (slice 7), by name: how far each narrows the view and what aiming through it shows. */
	static final Map<String, Optic> OPTICS = new HashMap<>();
	/**
	 * How much further from the eye a gun is held aimed than at the hip, in sixteenths of a block (tools/guns.py BUILDS
	 * "eye_relief"); a gun not listed is aimed at its hip's depth. The Garrison Rifle's bolt slides back along its line
	 * of sight as it fires, and at the hip's depth it came past the eye: aimed, each shot filled the screen. The Beam
	 * Pistol's coil (slice 8D) stands out either side of its back, between its sight and the eye. The marksman rifles
	 * (slice 9A) kick back toward the eye with each shot, and the Ranger's handle slides back beside its sights: at the
	 * hip's depth their backs came within two pixels of the eye and filled the bottom of the view. The automatic weapons
	 * (slice 9B) kick back toward the eye with every shot: at the hip's depth the Rattler's slide and the Bronco's
	 * receiver reached the near plane, and the Squall's back came within two pixels. So did the Spikedriver's (slice 9C),
	 * kicking back with each shot. The pump shotguns (slice 9D) kick hardest: at the hip's depth the tops of the Sledge's
	 * and the Highwayman's grips came two pixels past the eye, and the Throttle's within one and a quarter. The Skylark
	 * Rifle's kick (slice 10A) brought the back of its receiver to the near plane. Of coil and plasma (slice 10B), the
	 * Solenoid Rifle's kick brought its back within a pixel of the eye, the Votive Rifle's the end of its long grip to the
	 * eye, and the Glowmouth's past it. Of the double-barrels (slice 10C), the Mule's kick brought the back of its grip
	 * past the eye, and the Fowler's its butt plate. Of the sculk guns (slice 10D), the Undertone Rifle's bolt, driven
	 * back with each shot, came to the near plane, and the Murmur SMG's and the Reverb's backs within two pixels. Of the
	 * Nether guns (slice 11A), the Tusker's pump stroke brought the top of its grip within the near plane, the Cinder
	 * Repeater's rib within two pixels, and the Bastion Rifle's kick the back of its grip two and a half pixels past
	 * the eye. Of the rest (slice 11B), the Goldbristle Carbine's top plate, driven back with each shot, comes within
	 * three pixels, as the Cinder Repeater's rib does, and the Crackling's kick the top of its grip two pixels past the
	 * eye, as the Sledge's, whose receiver and grip it shares; the Ashfall Pistol stays nearly four pixels off.
	 */
	static final Map<String, Float> EYE_RELIEF = Map.ofEntries(Map.entry("garrison_rifle", 4.0F), Map.entry("beam_pistol", 4.0F), Map.entry("picket_rifle", 2.0F), Map.entry("ranger_rifle", 2.0F), Map.entry("kestrel_rifle", 2.0F), Map.entry("rattler_pistol", 4.0F), Map.entry("bronco_smg", 4.0F), Map.entry("squall_rifle", 3.0F), Map.entry("spikedriver", 2.0F), Map.entry("sledge", 6.0F), Map.entry("highwayman", 6.0F), Map.entry("throttle", 2.0F), Map.entry("skylark_rifle", 3.0F), Map.entry("solenoid_rifle", 1.0F), Map.entry("votive_rifle", 2.0F), Map.entry("glowmouth", 3.0F), Map.entry("mule", 2.5F), Map.entry("fowler", 3.0F), Map.entry("undertone_rifle", 2.0F), Map.entry("murmur_smg", 1.0F), Map.entry("reverb", 1.0F), Map.entry("tusker", 1.0F), Map.entry("cinder_repeater", 1.0F), Map.entry("bastion_rifle", 6.0F), Map.entry("goldbristle_carbine", 1.0F), Map.entry("crackling", 6.0F));
	/**
	 * How far the owner's third-person transform tilts a gun up off the arm that holds it, in degrees (tools/guns.py
	 * tilt(): the x rotation of its "thirdperson_righthand"); a gun not listed has none. The Gattaler's is made for an
	 * arm hanging at the hip, so seen from outside its holder's arms hang that much lower ({@link GunPose}) and the gun
	 * still points along the look: raised like a rifle, its barrels pointed at the sky. The CR4K Mining Laser's (the Seam
	 * Cutter, slice 9C) is made the same way.
	 */
	static final Map<String, Float> TILT = Map.of("thresher", 68.25F, "seam_cutter", 72.75F);
	private static final Look DEFAULT = new Look(true, 1.0F);

	static {
		LOOKS.put("rust_midge", new Look(true, 0.9F));
		LOOKS.put("patchwork_carbine", new Look(true, 0.82F));
		LOOKS.put("thunderpipe", new Look(true, 0.92F));
		LOOKS.put("warden_pistol", new Look(false, 0.9F));
		LOOKS.put("riveter_smg", new Look(true, 0.88F));
		LOOKS.put("haymaker", new Look(false, 0.92F));
		LOOKS.put("longhorn_rifle", new Look(true, 0.75F));
		LOOKS.put("drover_rifle", new Look(true, 0.8F));
		LOOKS.put("coach_gun", new Look(true, 0.9F));
		LOOKS.put("duelling_pistol", new Look(false, 0.9F));
		LOOKS.put("line_musket", new Look(true, 0.82F));
		LOOKS.put("bellmouth", new Look(true, 0.92F));
		LOOKS.put("bulldog_pistol", new Look(false, 0.9F));
		LOOKS.put("marshal_revolver", new Look(false, 0.85F));
		LOOKS.put("sapper_revolver", new Look(false, 0.9F));
		LOOKS.put("sentry_pistol", new Look(false, 0.9F));
		LOOKS.put("garrison_rifle", new Look(true, 0.85F));
		LOOKS.put("breacher", new Look(true, 0.92F));
		LOOKS.put("trench_lobber", new Look(true, 0.9F));
		LOOKS.put("thresher", new Look(true, 0.95F));
		LOOKS.put("stoker", new Look(true, 0.95F));
		LOOKS.put("beam_pistol", new Look(false, 0.9F));
		LOOKS.put("stormlock_rifle", new Look(true, 0.8F));
		LOOKS.put("linesman", new Look(true, 0.95F));
		LOOKS.put("picket_rifle", new Look(true, 0.7F));
		LOOKS.put("ranger_rifle", new Look(true, 0.75F));
		LOOKS.put("kestrel_rifle", new Look(true, 0.7F));
		LOOKS.put("rattler_pistol", new Look(false, 0.9F));
		LOOKS.put("bronco_smg", new Look(false, 0.9F));
		LOOKS.put("squall_rifle", new Look(true, 0.85F));
		LOOKS.put("spikedriver", new Look(false, 0.85F));
		LOOKS.put("seam_cutter", new Look(true, 0.95F));
		LOOKS.put("caisson_pistol", new Look(false, 0.9F));
		LOOKS.put("sledge", new Look(true, 0.9F));
		LOOKS.put("highwayman", new Look(true, 0.8F));
		LOOKS.put("throttle", new Look(true, 0.88F));
		LOOKS.put("earthmover", new Look(true, 0.85F));
		LOOKS.put("skylark_rifle", new Look(true, 0.8F));
		LOOKS.put("bullfrog", new Look(true, 0.92F));
		LOOKS.put("solenoid_rifle", new Look(true, 0.75F));
		LOOKS.put("votive_rifle", new Look(true, 0.85F));
		LOOKS.put("glowmouth", new Look(true, 0.95F));
		LOOKS.put("mule", new Look(true, 0.92F));
		LOOKS.put("fowler", new Look(true, 0.88F));
		LOOKS.put("culverin", new Look(false, 0.94F));
		LOOKS.put("undertone_rifle", new Look(true, 0.82F));
		LOOKS.put("murmur_smg", new Look(true, 0.9F));
		LOOKS.put("reverb", new Look(true, 0.9F));
		LOOKS.put("tusker", new Look(true, 0.9F));
		LOOKS.put("cinder_repeater", new Look(true, 0.88F));
		LOOKS.put("bastion_rifle", new Look(true, 0.8F));
		LOOKS.put("ashfall_pistol", new Look(false, 0.9F));
		LOOKS.put("goldbristle_carbine", new Look(true, 0.85F));
		LOOKS.put("crackling", new Look(true, 0.9F));
		OPTICS.put("long_scope", new Optic(0.3F, "long_scope_reticle2", "scope_vignette", null));
		OPTICS.put("medium_scope", new Optic(0.5F, "long_scope_reticle2", "scope_vignette", null));
		OPTICS.put("reflex_sight", new Optic(0.85F, null, null, "red_dot_reticle"));
		OPTICS.put("laser_sight", new Optic(0.9F, null, null, "red_dot_reticle"));
	}

	private GunLooks() {
	}

	/** The gun's look; a gun not listed is held in both hands and does not zoom. */
	public static Look of(String gun) {
		return LOOKS.getOrDefault(gun, DEFAULT);
	}

	/** How far the gun's third-person transform tilts it up off the arm, in degrees ({@link #TILT}), or null for none. */
	public static @Nullable Float tilt(String gun) {
		return TILT.get(gun);
	}

	/** The scope fitted to this gun, or null. */
	public static @Nullable Optic optic(ItemStack stack) {
		String fitted = GunItem.inSlot(stack, "optic");
		return fitted == null ? null : OPTICS.get(fitted);
	}

	/**
	 * @param twoHanded held in both hands: seen from outside, both arms come up to it ({@link GunPose})
	 * @param zoom      the field of view is multiplied by this aimed down the sights (GunFovMixin)
	 */
	public record Look(boolean twoHanded, float zoom) {
	}

	/**
	 * A scope's look ({@link GunScope}); its textures are the owner's, in textures/item/guns/optics/.
	 *
	 * @param zoom     aimed through it in first person, the field of view is multiplied by this, in place of the gun's own
	 * @param reticle  the reticle that fills the screen aimed through it, the gun put away; or null
	 * @param vignette the dark rim of the lens, drawn over the reticle; or null
	 * @param dot      a reflex sight's dot, on the middle of the screen over the gun; or null
	 */
	public record Optic(float zoom, @Nullable String reticle, @Nullable String vignette, @Nullable String dot) {
		/** Whether aiming through it fills the screen with the view through the scope (and puts the gun away). */
		public boolean fillsView() {
			return reticle != null;
		}
	}
}
