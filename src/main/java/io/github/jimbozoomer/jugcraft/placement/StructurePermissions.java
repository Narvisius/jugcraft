package io.github.jimbozoomer.jugcraft.placement;

import io.github.jimbozoomer.jugcraft.town.TownProtection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Check every occupied cell before a multi-block placement or a custom packet changes the world. */
public final class StructurePermissions {
	private StructurePermissions() { }

	public static boolean mayChange(Level level, BlockPos pos, Player player) {
		return !player.isSpectator() && player.getAbilities().mayBuild
				&& level.isLoaded(pos) && !level.isOutsideBuildHeight(pos) && level.getWorldBorder().isWithinBounds(pos)
				&& level.mayInteract(player, pos) && !TownProtection.denies(player, level, pos)
				&& player.mayUseItemAt(pos, Direction.UP, player.getMainHandItem());
	}
}
