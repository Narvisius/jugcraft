package io.github.jimbozoomer.jugcraft.arcane;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.*;

/** These two supplied blocks have shape/falling behavior, not a hidden crafting system. */
final class ArcaneDecor {
    static final class Mortar extends Block {
        Mortar(Properties p){super(p);}
        @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return Block.box(4,0,4,12,6,12);}
    }
    static final class Egg extends FallingBlock {
        @Override public int getDustColor(BlockState s,BlockGetter l,BlockPos p){return 0xFF473F52;}
        Egg(Properties p){super(p);}
        @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return Shapes.or(Block.box(4,0,4,12,15,12),Block.box(3,1,3,13,13,13),Block.box(2,3,2,14,11,14),Block.box(5,15,5,11,16,11));}
        @Override protected int getDelayAfterPlace(){return 5;}
        @Override protected boolean isPathfindable(BlockState s,PathComputationType t){return false;}
    }
}
