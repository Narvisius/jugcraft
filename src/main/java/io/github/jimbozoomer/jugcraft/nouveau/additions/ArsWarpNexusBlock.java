package io.github.jimbozoomer.jugcraft.nouveau.additions;

import io.github.jimbozoomer.jugcraft.nouveau.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
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
import org.jspecify.annotations.Nullable;

/** The supplied two-cell nexus: one lower entity, one collision/support cell above. */
public final class ArsWarpNexusBlock extends NouveauWorkshopBlock implements SimpleWaterloggedBlock {
    public static final EnumProperty<DoubleBlockHalf> HALF=BlockStateProperties.DOUBLE_BLOCK_HALF;
    public static final BooleanProperty WATERLOGGED=BlockStateProperties.WATERLOGGED;
    public static final BooleanProperty REQUIRES_SOURCE=BooleanProperty.create("requires_source");
    public ArsWarpNexusBlock(NouveauCatalog.Entry entry,Properties properties){super(entry,properties);registerDefaultState(defaultBlockState().setValue(HALF,DoubleBlockHalf.LOWER).setValue(WATERLOGGED,false).setValue(REQUIRES_SOURCE,true));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){super.createBlockStateDefinition(b);b.add(HALF,WATERLOGGED,REQUIRES_SOURCE);}
    @Override public @Nullable BlockState getStateForPlacement(BlockPlaceContext c){
        var pos=c.getClickedPos();if(pos.getY()>=c.getLevel().getMaxY()-1||!c.getLevel().getBlockState(pos.above()).canBeReplaced(c))return null;
        return super.getStateForPlacement(c).setValue(WATERLOGGED,c.getLevel().getFluidState(pos).is(Fluids.WATER));
    }
    @Override public void setPlacedBy(Level l,BlockPos pos,BlockState state,@Nullable LivingEntity placer,ItemStack stack){
        super.setPlacedBy(l,pos,state,placer,stack);l.setBlock(pos.above(),state.setValue(HALF,DoubleBlockHalf.UPPER).setValue(WATERLOGGED,l.getFluidState(pos.above()).is(Fluids.WATER)),Block.UPDATE_ALL);
    }
    @Override public @Nullable BlockEntity newBlockEntity(BlockPos pos,BlockState state){return state.getValue(HALF)==DoubleBlockHalf.LOWER?super.newBlockEntity(pos,state):null;}
    @Override protected FluidState getFluidState(BlockState s){return s.getValue(WATERLOGGED)?Fluids.WATER.getSource(false):super.getFluidState(s);}
    @Override protected BlockState updateShape(BlockState s,LevelReader l,ScheduledTickAccess ticks,BlockPos p,Direction d,BlockPos q,BlockState other,net.minecraft.util.RandomSource random){
        if(s.getValue(WATERLOGGED))ticks.scheduleTick(p,Fluids.WATER,Fluids.WATER.getTickDelay(l));
        boolean lower=s.getValue(HALF)==DoubleBlockHalf.LOWER;
        if(d==(lower?Direction.UP:Direction.DOWN)&&(!other.is(this)||other.getValue(HALF)==s.getValue(HALF)))return Blocks.AIR.defaultBlockState();
        return s;
    }
    @Override public BlockState playerWillDestroy(Level l,BlockPos p,BlockState s,Player player){
        if(!l.isClientSide()&&s.getValue(HALF)==DoubleBlockHalf.UPPER&&l.getBlockState(p.below()).is(this))l.destroyBlock(p.below(),!player.preventsBlockDrops(),player);
        return super.playerWillDestroy(l,p,s,player);
    }
    public static @Nullable NouveauWorkshop at(Level l,BlockPos p){
        var s=l.getBlockState(p);if(!(s.getBlock() instanceof ArsWarpNexusBlock))return null;
        if(s.getValue(HALF)==DoubleBlockHalf.UPPER)p=p.below();
        return l.getBlockEntity(p) instanceof NouveauWorkshop n&&n.kind().equals("warp_nexus")?n:null;
    }
    @Override protected InteractionResult useItemOn(ItemStack stack,BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit){
        if(stack.getItem() instanceof ArsWarpNexus.Scroll)return InteractionResult.PASS;
        return useWithoutItem(s,l,p,player,hit);
    }
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult hit){
        return player instanceof ServerPlayer server?ArsWarpNexus.open(server,at(l,p)):InteractionResult.SUCCESS;
    }
}
