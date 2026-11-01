package io.github.jimbozoomer.jugcraft.backpack;

import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;

public final class OwnerBackpackBlock extends Block implements EntityBlock {
    public static final EnumProperty<Direction> FACING=BlockStateProperties.HORIZONTAL_FACING;
    public final int tier;
    OwnerBackpackBlock(Properties props,int tier){super(props.pushReaction(PushReaction.IMMOVEABLE));this.tier=tier;registerDefaultState(defaultBlockState().setValue(FACING,Direction.NORTH));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){return defaultBlockState().setValue(FACING,c.getHorizontalDirection().getOpposite());}
    @Override protected BlockState rotate(BlockState s,Rotation r){return s.setValue(FACING,r.rotate(s.getValue(FACING)));}
    @Override protected BlockState mirror(BlockState s,Mirror m){return s.setValue(FACING,m.mirror(s.getValue(FACING)));}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new OwnerBackpackEntity(p,s);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return !level.isClientSide()&&type==OwnerBackpacks.ENTITY?(l,p,s,e)->((OwnerBackpackEntity)e).tick():null;}
    @Override public void setPlacedBy(Level l,BlockPos p,BlockState s,LivingEntity placer,ItemStack item){if(l.getBlockEntity(p) instanceof OwnerBackpackEntity bag){bag.setCarrier(item);if(placer instanceof Player who)bag.owner(who.getUUID());}}
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult hit){
        if(player instanceof ServerPlayer who&&l.getBlockEntity(p) instanceof OwnerBackpackEntity bag){
            if(!bag.mayUse(who))return InteractionResult.FAIL;
            if(who.isShiftKeyDown()&&who.getMainHandItem().isEmpty()){
                if(bag.viewed()||!OwnerBackpackInfinity.mayEdit(who,bag.carrier()))return InteractionResult.FAIL;
                var carrier=bag.carrier().copy();
                if(l.setBlock(p,Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL|Block.UPDATE_SUPPRESS_DROPS))who.setItemInHand(InteractionHand.MAIN_HAND,carrier);
            }else bag.open(who);
        }
        return InteractionResult.SUCCESS;
    }
    @Override protected InteractionResult useItemOn(ItemStack stack,BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit){if(player instanceof ServerPlayer who&&l.getBlockEntity(p) instanceof OwnerBackpackEntity bag&&OwnerBackpackConversions.isConversion(stack)){
            if(!bag.mayUse(who)||bag.viewed())return InteractionResult.FAIL;
            return OwnerBackpackConversions.convert(who,bag.carrier(),stack,bag::setChanged)?InteractionResult.SUCCESS:InteractionResult.FAIL;
        }
        return useWithoutItem(s,l,p,player,hit);}
    @Override protected java.util.List<ItemStack> getDrops(BlockState s,LootParams.Builder params){
        if(params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof OwnerBackpackEntity bag)return java.util.List.of(bag.carrier().copy());
        return java.util.List.of(new ItemStack(this));
    }
    @Override public BlockState playerWillDestroy(Level l,BlockPos p,BlockState s,Player player){
        if(!l.isClientSide()&&player.isCreative()&&l.getBlockEntity(p) instanceof OwnerBackpackEntity bag&&(!bag.inventory().isEmpty()||!OwnerBackpackMobs.entries(bag.carrier()).isEmpty()||bag.carrier().getOrDefault(OwnerBackpacks.UPGRADES,net.minecraft.world.item.component.ItemContainerContents.EMPTY).nonEmptyItemCopyStream().findAny().isPresent()))popResource(l,p,bag.carrier().copy());
        return super.playerWillDestroy(l,p,s,player);
    }
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return Block.box(2,0,3,14,15,13);}
}
