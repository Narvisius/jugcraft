package io.github.jimbozoomer.jugcraft.nouveau;

import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import org.jspecify.annotations.Nullable;

public final class StorageLecternBlock extends BaseEntityBlock {
    public StorageLecternBlock(Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.NORTH));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(BlockStateProperties.HORIZONTAL_FACING);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){return defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,c.getHorizontalDirection().getOpposite());}
    @Override protected BlockState rotate(BlockState s,Rotation r){return s.setValue(BlockStateProperties.HORIZONTAL_FACING,r.rotate(s.getValue(BlockStateProperties.HORIZONTAL_FACING)));}
    @Override protected BlockState mirror(BlockState s,Mirror m){return rotate(s,m.getRotation(s.getValue(BlockStateProperties.HORIZONTAL_FACING)));}
    @Override protected RenderShape getRenderShape(BlockState s){return RenderShape.INVISIBLE;}
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return Block.box(2,0,2,14,17,14);}
    @Override public @Nullable BlockEntity newBlockEntity(BlockPos p,BlockState s){return new StorageLectern(p,s);}
    @Override public <T extends net.minecraft.world.level.block.entity.BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(Level level,BlockState state,net.minecraft.world.level.block.entity.BlockEntityType<T> type){return level.isClientSide()?null:createTickerHelper(type,JugNouveau.LECTERN_ENTITY,StorageLectern::tick);}
    @Override public void setPlacedBy(Level l,BlockPos p,BlockState s,@Nullable LivingEntity by,ItemStack stack){
        super.setPlacedBy(l,p,s,by,stack);if(by instanceof ServerPlayer player&&l.getBlockEntity(p) instanceof StorageLectern be)be.placedBy(player);
    }
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult hit){
        if(player instanceof ServerPlayer server&&l.getBlockEntity(p) instanceof StorageLectern be){
            if(!be.mayUse(server)||!RateGate.allow(server,"nouveau_open",4))return InteractionResult.FAIL;
            server.openMenu(new SimpleMenuProvider((id,inv,who)->player.isShiftKeyDown()?new StorageLecternMenu(id,inv,be):new NouveauStorageTerminal.Menu(id,inv,be),Component.translatable("screen.jugcraft.bookwyrm.title")));
        }
        return InteractionResult.SUCCESS;
    }
}
