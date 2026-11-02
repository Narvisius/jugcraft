package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** A modeled serving which retains the complete original stack, including names/effects. */
public final class FoodServingBlock extends PlacedDishBlock implements EntityBlock {
    private final net.minecraft.world.phys.shapes.VoxelShape outline;
    public FoodServingBlock(Item food, DishShape shape, net.minecraft.world.phys.shapes.VoxelShape outline, Properties properties) {
        super(food,shape,properties); this.outline=outline;
    }
    @Override protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext context) {
        var b=outline.bounds();
        return switch(state.getValue(FACING)) {
            case EAST -> net.minecraft.world.phys.shapes.Shapes.box(1-b.maxZ,b.minY,b.minX,1-b.minZ,b.maxY,b.maxX);
            case SOUTH -> net.minecraft.world.phys.shapes.Shapes.box(1-b.maxX,b.minY,1-b.maxZ,1-b.minX,b.maxY,1-b.minZ);
            case WEST -> net.minecraft.world.phys.shapes.Shapes.box(b.minZ,b.minY,1-b.maxX,b.maxZ,b.maxY,1-b.minX);
            default -> outline;
        };
    }

    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) {
        return new ShowcaseBlockEntity(FoodServings.ENTITY,pos,state);
    }

    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit) {
        if(!player.getMainHandItem().isEmpty()) return InteractionResult.PASS;
        if(!level.isClientSide() && (!io.github.jimbozoomer.jugcraft.finance.LandRegistry.mayBuild(player,level,pos)
                || io.github.jimbozoomer.jugcraft.town.TownProtection.denies(player,level,pos))) return InteractionResult.FAIL;
        if(level.getBlockEntity(pos) instanceof ShowcaseBlockEntity serving) {
            var result=serving.use(0,ItemStack.EMPTY,player,InteractionHand.MAIN_HAND,FoodServings.SLOT);
            if(!level.isClientSide() && serving.get(0).isEmpty()) level.removeBlock(pos,false);
            return result;
        }
        return InteractionResult.FAIL;
    }

    @Override protected ItemStack getCloneItemStack(LevelReader level,BlockPos pos,BlockState state,boolean includeData) {
        return level.getBlockEntity(pos) instanceof ShowcaseBlockEntity serving ? serving.get(0).copy() : new ItemStack(dish());
    }
}
