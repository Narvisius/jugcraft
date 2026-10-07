package io.github.jimbozoomer.jugcraft.mixin;

import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Vanilla's rule for raising a wall's post, for walls that join diagonally (DiagonalConnections): the rule is private
 * to WallBlock and no API offers it, and calling it keeps diagonal walls' posts in step with vanilla's.
 */
@Mixin(WallBlock.class)
public interface WallBlockInvoker {
	@Invoker("shouldRaisePost")
	boolean jugcraft$shouldRaisePost(BlockState state, BlockState above, VoxelShape aboveShape);
}
