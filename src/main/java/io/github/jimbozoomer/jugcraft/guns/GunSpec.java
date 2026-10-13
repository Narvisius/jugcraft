package io.github.jimbozoomer.jugcraft.guns;

/**
 * One gun's numbers (tools/guns.py GUNS; check_guns in tools/check_mod_data.py keeps them the same).
 *
 * @param damage      per bullet (per pellet for a shotgun), in half hearts; a grenade's burst at its centre, a burst of
 *                    flame's on each creature in it (slice 8C, {@link JugcraftGuns#SHOTS})
 * @param pellets     bullets a shot
 * @param interval    ticks between shots: the fastest a trigger can be pulled, or the automatic rate
 * @param auto        fires while the button is held
 * @param capacity    rounds the magazine (or the barrels) hold
 * @param reload      ticks to change a magazine; 0 for a gun loaded a shell at a time
 * @param shellStart  ticks to open a gun loaded a shell at a time, before the first shell
 * @param shellEach   ticks for each shell
 * @param shellFinish ticks to close it after the last
 * @param hipSpread   degrees a bullet strays from the aim, fired from the hip
 * @param aimSpread   the same, aimed down the sights
 * @param range       blocks
 * @param ammo        the round it fires: a jugcraft item path, or another mod's or vanilla's full item id
 *                    ({@link JugcraftGuns#ammo})
 */
public record GunSpec(float damage, int pellets, int interval, boolean auto, int capacity, int reload, int shellStart,
		int shellEach, int shellFinish, float hipSpread, float aimSpread, int range, String ammo) {
	/** Whether it is loaded a shell at a time (and a shot may cut the loading short). */
	public boolean byShell() {
		return reload == 0;
	}

	/** Ticks to load this many rounds. */
	public int reloadTicks(int rounds) {
		return byShell() ? shellStart + shellEach * rounds + shellFinish : reload;
	}
}
