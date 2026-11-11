package io.github.jimbozoomer.jugcraft.nuclear;

import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

/**
 * The block entity a compiled site hangs from (towers and pump houses build): the cooling tower's controller, the
 * intake's and the circulating water pump house's. It names its site, faces the way the site is turned, and says which
 * value each of the site's states shows now (its live groups: SiteData.Group), on both sides.
 */
public interface SiteAnchor {
	/** The site's key (assets/jugcraft/sites/&lt;key&gt;.bin.gz), or null if this anchor has none. */
	@Nullable String siteKey();

	/** The way the site is turned (south: as compiled). */
	Direction siteFacing();

	/** The site's live groups by variable and value (SiteData.groupTable), or an empty table. */
	SiteData.Group[][] groups();

	/** The value variable {@code v} (its index in the site's list) shows now. */
	int value(int v);

	/** A click on one of the site's controls. */
	void onZone(ServerLevel level, Player player, String zone);

	/** The figure an LED display of the site shows now (NaN: dashes). */
	default double display(String key) {
		return Double.NaN;
	}
}
