package io.github.jimbozoomer.jugcraft.arcane;

import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.material.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

public final class ArcaneWorkshopBlock extends BaseEntityBlock implements SimpleWaterloggedBlock {
    public static final BooleanProperty ACTIVE=BooleanProperty.create("activated"),WATERLOGGED=BlockStateProperties.WATERLOGGED;
    public final String kind;
    public ArcaneWorkshopBlock(String kind,Properties p){super(p);this.kind=kind;registerDefaultState(defaultBlockState().setValue(ACTIVE,false).setValue(WATERLOGGED,false));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(ACTIVE,WATERLOGGED);}
    @Override protected RenderShape getRenderShape(BlockState s){return RenderShape.MODEL;}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new ArcaneWorkshop(p,s);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> type){return l.isClientSide()?null:createTickerHelper(type,ForbiddenArcane.WORKSHOP,ArcaneWorkshop::tick);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){return defaultBlockState().setValue(WATERLOGGED,c.getLevel().getFluidState(c.getClickedPos()).is(Fluids.WATER));}
    @Override protected FluidState getFluidState(BlockState s){return s.getValue(WATERLOGGED)?Fluids.WATER.getSource(false):super.getFluidState(s);}
    @Override protected BlockState updateShape(BlockState s,LevelReader l,ScheduledTickAccess ticks,BlockPos p,Direction face,BlockPos other,BlockState n,RandomSource random){if(s.getValue(WATERLOGGED))ticks.scheduleTick(p,Fluids.WATER,Fluids.WATER.getTickDelay(l));return super.updateShape(s,l,ticks,p,face,other,n,random);}
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return kind.contains("pedestal")?Shapes.or(Block.box(1,0,1,15,4,15),Block.box(3,4,3,13,6,13),Block.box(4,6,4,12,11,12),Block.box(2,11,2,14,14,14)):Shapes.or(Block.box(1,0,1,15,3,15),Block.box(2,3,2,14,4,14),Block.box(4,4,4,12,8,12),Block.box(0,8,0,16,15,16));}
    @Override public void setPlacedBy(Level l,BlockPos p,BlockState s,LivingEntity entity,ItemStack stack){super.setPlacedBy(l,p,s,entity,stack);if(entity instanceof ServerPlayer player&&l.getBlockEntity(p) instanceof ArcaneWorkshop w)w.owner(player.getUUID());}
    @Override protected InteractionResult useItemOn(ItemStack stack,BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit){return player instanceof ServerPlayer sp&&l.getBlockEntity(p) instanceof ArcaneWorkshop w?w.use(sp,hand):InteractionResult.SUCCESS;}
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult hit){return player instanceof ServerPlayer sp&&l.getBlockEntity(p) instanceof ArcaneWorkshop w?w.use(sp,InteractionHand.MAIN_HAND):InteractionResult.SUCCESS;}
}
