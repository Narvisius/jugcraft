package io.github.jimbozoomer.jugcraft.grave;

import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

/** Supplied headstone geometry. No block inventory: hoppers and explosions cannot steal its contents. */
public class GraveMarkerBlock extends Block {
    public GraveMarkerBlock(Properties properties) { super(properties); }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Block.box(1, 0, 3, 15, 16, 13);
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer server) {
            var grave = GraveStore.get(server.level().getServer()).at(GlobalPos.of(level.dimension(), pos));
            if (grave != null) DeathGraves.recover(server, grave.id());
        }
        return InteractionResult.SUCCESS;
    }
}
