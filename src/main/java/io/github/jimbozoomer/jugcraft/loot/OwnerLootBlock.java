package io.github.jimbozoomer.jugcraft.loot;

import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;
import net.minecraft.server.level.ServerPlayer;

public final class OwnerLootBlock extends Block implements EntityBlock,SimpleWaterloggedBlock {
    public static final EnumProperty<net.minecraft.core.Direction> FACING=BlockStateProperties.FACING;
    public static final BooleanProperty OPEN=BlockStateProperties.OPEN,WATERLOGGED=BlockStateProperties.WATERLOGGED;
    public static final BooleanProperty MODEL=BooleanProperty.create("model"),SEEN=BooleanProperty.create("seen");
    public final String kind;
    OwnerLootBlock(Properties properties,String kind){super(properties);this.kind=kind;registerDefaultState(defaultBlockState().setValue(FACING,Direction.NORTH).setValue(OPEN,false).setValue(WATERLOGGED,false).setValue(MODEL,false).setValue(SEEN,false));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(FACING,OPEN,WATERLOGGED,MODEL,SEEN);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){return defaultBlockState().setValue(FACING,kind.equals("barrel")||kind.equals("shulker_box")?c.getNearestLookingDirection().getOpposite():c.getHorizontalDirection().getOpposite()).setValue(WATERLOGGED,c.getLevel().getFluidState(c.getClickedPos()).getType()==net.minecraft.world.level.material.Fluids.WATER);}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new OwnerLootContainer(p,s);}
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){
        if(!OwnerLoot.mayOpen(player,level,pos))return InteractionResult.FAIL;
        if(player instanceof ServerPlayer p&&level.getBlockEntity(pos) instanceof OwnerLootContainer container){
            if(container.archaeology()&&!container.seen(player.getUUID())){player.sendOverlayMessage(net.minecraft.network.chat.Component.literal("Use a brush to uncover your personal archaeological reward."));return InteractionResult.SUCCESS;}
            container.open(p);
        }
        return InteractionResult.SUCCESS;
    }
    @Override protected InteractionResult useItemOn(ItemStack item,BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit){
        if(kind.startsWith("suspicious_")&&item.getItem() instanceof net.minecraft.world.item.BrushItem&&OwnerLoot.mayOpen(player,l,p)){player.startUsingItem(hand);return InteractionResult.SUCCESS;}
        return useWithoutItem(s,l,p,player,hit);
    }
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return kind.contains("chest")?Block.box(1,0,1,15,14,15):kind.equals("decorated_pot")?Block.box(1,0,1,15,16,15):Shapes.block();}
    @Override protected BlockState rotate(BlockState s,Rotation r){return s.setValue(FACING,r.rotate(s.getValue(FACING)));}
    @Override protected BlockState mirror(BlockState s,Mirror m){return s.rotate(m.getRotation(s.getValue(FACING)));}
    @Override protected net.minecraft.world.level.material.FluidState getFluidState(BlockState s){return s.getValue(WATERLOGGED)?net.minecraft.world.level.material.Fluids.WATER.getSource(false):super.getFluidState(s);}
    @Override protected BlockState updateShape(BlockState s,LevelReader l,ScheduledTickAccess ticks,BlockPos p,Direction d,BlockPos q,BlockState other,net.minecraft.util.RandomSource r){if(s.getValue(WATERLOGGED))ticks.scheduleTick(p,net.minecraft.world.level.material.Fluids.WATER,net.minecraft.world.level.material.Fluids.WATER.getTickDelay(l));return s;}
    @Override protected boolean isSignalSource(BlockState state){return kind.equals("trapped_chest");}
    @Override protected int getSignal(BlockState state,BlockGetter level,BlockPos pos,Direction direction){return kind.equals("trapped_chest")&&state.getValue(OPEN)?15:0;}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return type==OwnerLoot.TYPE?(l,p,s,be)->((OwnerLootContainer)be).tick():null;}
}
