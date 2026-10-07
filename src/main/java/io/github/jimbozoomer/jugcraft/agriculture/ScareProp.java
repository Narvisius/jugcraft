package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * A yard prop that a passer-by sets off: the Grave Mound (a zombie hand claws up) and the Pop-Up Skeleton. A player
 * who comes within {@link #reach()} blocks, not sneaking and not a spectator, raises it ({@link #RAISED}) for
 * {@link #upTicks()} ticks; it can't go off again for {@link #cooldownTicks()} more. A redstone signal holds it up.
 * {@link ScarePropBlockEntity} does the watching, on the server, a few times a second.
 */
public interface ScareProp {
	BooleanProperty RAISED = BooleanProperty.create("raised");

	/** How near (in blocks) a player must come to set it off. */
	double reach();

	/** How long it stays up once set off. */
	int upTicks();

	/** How long after coming down before it can go off again. */
	int cooldownTicks();

	/** Its sound and dust as it goes up (server side). */
	void onRaise(ServerLevel level, BlockPos pos, BlockState state);
}
