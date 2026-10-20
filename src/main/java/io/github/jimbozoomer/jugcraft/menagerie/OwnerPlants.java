package io.github.jimbozoomer.jugcraft.menagerie;

import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.effect.MobEffects;

/** Native plant placement, paired halves and cardinal model states. */
final class OwnerPlants {
    private OwnerPlants(){}
    static final class FlytrapPot extends FlowerPotBlock {
        FlytrapPot(Block flower,Properties p){super(flower,p);registerDefaultState(defaultBlockState().setValue(BlockStateProperties.OPEN,true));}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){super.createBlockStateDefinition(b);b.add(BlockStateProperties.OPEN);}
        @Override protected net.minecraft.world.InteractionResult useWithoutItem(BlockState s,Level l,BlockPos at,net.minecraft.world.entity.player.Player player,net.minecraft.world.phys.BlockHitResult hit){
            if(!player.isShiftKeyDown())return super.useWithoutItem(s,l,at,player,hit);
            if(!Menagerie.mayChange(l,player,at))return net.minecraft.world.InteractionResult.FAIL;
            if(!l.isClientSide())l.setBlock(at,s.cycle(BlockStateProperties.OPEN),Block.UPDATE_ALL);
            return net.minecraft.world.InteractionResult.SUCCESS;
        }
    }
    static Block create(BlockBehaviour.Properties p,JsonObject definition){
        var properties=definition.getAsJsonObject("properties");
        if(properties.has("half"))return new Tall(p);
        if(properties.has("flower_amount"))return new FlowerBedBlock(p,1){};
        if(!properties.isEmpty())return new Directional(p);
        return new FlowerBlock(MobEffects.REGENERATION,5,p);
    }
    private static final class Tall extends DoublePlantBlock {
        Tall(Properties p){super(p);registerDefaultState(defaultBlockState().setValue(HorizontalDirectionalBlock.FACING,Direction.NORTH));}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){super.createBlockStateDefinition(b);b.add(HorizontalDirectionalBlock.FACING);}
        @Override public BlockState getStateForPlacement(BlockPlaceContext c){var s=super.getStateForPlacement(c);return s==null?null:s.setValue(HorizontalDirectionalBlock.FACING,c.getHorizontalDirection().getOpposite());}
        @Override public void setPlacedBy(Level l,BlockPos p,BlockState s,LivingEntity entity,ItemStack stack){super.setPlacedBy(l,p,s,entity,stack);var top=l.getBlockState(p.above());if(top.is(this))l.setBlock(p.above(),top.setValue(HorizontalDirectionalBlock.FACING,s.getValue(HorizontalDirectionalBlock.FACING)),UPDATE_ALL);}
        @Override protected BlockState rotate(BlockState s,Rotation r){return s.setValue(HorizontalDirectionalBlock.FACING,r.rotate(s.getValue(HorizontalDirectionalBlock.FACING)));}
        @Override protected BlockState mirror(BlockState s,Mirror m){return rotate(s,m.getRotation(s.getValue(HorizontalDirectionalBlock.FACING)));}
    }
    private static final class Directional extends FlowerBlock {
        static final BooleanProperty TOP=BooleanProperty.create("top");
        Directional(Properties p){super(MobEffects.REGENERATION,5,p);registerDefaultState(defaultBlockState().setValue(BlockStateProperties.FACING,Direction.UP).setValue(BlockStateProperties.OPEN,true).setValue(TOP,true));}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){super.createBlockStateDefinition(b);b.add(BlockStateProperties.FACING,BlockStateProperties.OPEN,TOP);}
        @Override public BlockState getStateForPlacement(BlockPlaceContext c){return defaultBlockState().setValue(BlockStateProperties.FACING,c.getClickedFace());}
        @Override protected BlockState rotate(BlockState s,Rotation r){return s.setValue(BlockStateProperties.FACING,r.rotate(s.getValue(BlockStateProperties.FACING)));}
        @Override protected BlockState mirror(BlockState s,Mirror m){return s.setValue(BlockStateProperties.FACING,m.mirror(s.getValue(BlockStateProperties.FACING)));}
    }
}
