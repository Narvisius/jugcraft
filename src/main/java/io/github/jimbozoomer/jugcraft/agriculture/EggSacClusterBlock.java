package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The Egg Sac Cluster (Halloween decorations batch 19, the Spider's Larder): glistening white sacs on the floors, walls
 * or ceilings of the blocks beside it, any or all at once, like glow lichen. They pulse slowly (an animated texture),
 * and at night, now and then, a few tiny spiderlings skitter out across the face and vanish. The spiderlings are drawn by
 * the client (client/EggSacRenderer.java): no mobs are spawned.
 */
public class EggSacClusterBlock extends MultifaceBlock implements EntityBlock {
	public EggSacClusterBlock(Properties properties) {
		super(properties);
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new DecorationBlockEntity(JugcraftAgriculture.EGG_SAC_ENTITY, pos, state);
	}
}
