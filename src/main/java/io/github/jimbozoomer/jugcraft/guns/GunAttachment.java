package io.github.jimbozoomer.jugcraft.guns;

/**
 * One attachment's slot and what it does to a gun (tools/guns.py ATTACHMENTS; check_guns in tools/check_mod_data.py
 * keeps them the same). Each number multiplies the gun's own; 1 leaves it be.
 *
 * @param slot      barrel, magazine, stock, grip or optic (a scope): a gun takes one attachment a slot
 * @param replaces  it takes the place of the slot's standard part on the model (GunRenderer hides that part)
 * @param damage    per bullet
 * @param range     blocks
 * @param hipSpread degrees a bullet strays fired from the hip
 * @param aimSpread the same, aimed down the sights
 * @param capacity  rounds the magazine holds
 * @param reload    ticks to change the magazine
 * @param kick      how far the view jumps with each shot
 * @param volume    how loud the shot is
 * @param stab      a bayonet's stab, in half hearts (slice 7); 0 for every other attachment
 */
public record GunAttachment(String slot, boolean replaces, float damage, float range, float hipSpread, float aimSpread,
		float capacity, float reload, float kick, float volume, float stab) {
	/** Whether it is a bayonet: the gun can stab with it ({@link GunShots#stab}). */
	public boolean bayonet() {
		return stab > 0.0F;
	}
}
