package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;

/** A 3x2x3 roast with one authoritative remaining-portion count at its master. */
public final class GiantTurkeyFeastBlock extends MultiDecorationBlock {
    public static final int PORTIONS=24;
    private static final int[][] CELLS=box(3,2,3);
    public static final IntegerProperty PART=IntegerProperty.create("part",0,17);
    public static final IntegerProperty REMAINING=IntegerProperty.create("remaining",0,PORTIONS);
    private static final java.util.Map<Direction,VoxelShape[]> SHAPES=shapesOf(CELLS,
        new double[]{3,0,4.5,45,1.5,43.5},new double[]{12,1.5,12,36,15,39},
        new double[]{15,15,15,33,19.5,36},new double[]{9,4.5,18,39,12,33},new double[]{13.5,3,2.25,34.5,12,13.5});
    public GiantTurkeyFeastBlock(Properties p){super(p);registerDefaultState(defaultBlockState().setValue(REMAINING,PORTIONS));}
    public int[][] cells(){return CELLS;}
    public IntegerProperty partProperty(){return PART;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){super.createBlockStateDefinition(b);b.add(REMAINING);}
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return SHAPES.get(s.getValue(FACING))[part(s)];}
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){
        var s=super.getStateForPlacement(c);if(s==null)return null;
        var player=c.getPlayer();var level=c.getLevel();
        for(int i=0;i<CELLS.length;i++){
            var p=partPos(c.getClickedPos(),s.getValue(FACING),i);
            if(!level.hasChunkAt(p)||!level.isUnobstructed(s.setValue(PART,i),p,CollisionContext.empty())
                ||(player!=null&&(!player.mayUseItemAt(p,Direction.UP,c.getItemInHand())
                ||!level.isClientSide()&&(!io.github.jimbozoomer.jugcraft.finance.LandRegistry.mayBuild(player,level,p)||io.github.jimbozoomer.jugcraft.town.TownProtection.denies(player,level,p)))))return null;
            if(CELLS[i][1]==0&&!level.getBlockState(p.below()).isSolid())return null;
        }
        return s;
    }
    private boolean usable(Level l,BlockPos master,BlockState s,Player player,boolean packing){
        if(!s.is(this))return false;
        for(int i=0;i<CELLS.length;i++){
            var p=partPos(master,s.getValue(FACING),i);if(!l.hasChunkAt(p))return false;
            var other=l.getBlockState(p);
            if(!other.is(this)||other.getValue(PART)!=i||other.getValue(FACING)!=s.getValue(FACING)
                ||!io.github.jimbozoomer.jugcraft.finance.LandRegistry.mayBuild(player,l,p)||(io.github.jimbozoomer.jugcraft.town.TownProtection.denies(player,l,p) && (packing || !io.github.jimbozoomer.jugcraft.town.TownProtection.allowsPublicUse(player,l,p))))return false;
        }
        return true;
    }
    @Override protected InteractionResult useItemOn(ItemStack held,BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit){
        return held.is(JugcraftAgriculture.KNIVES)?serve(s,l,p,player,true):InteractionResult.TRY_WITH_EMPTY_HAND;
    }
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult hit){
        if(player.isShiftKeyDown()){
            if(l.isClientSide())return InteractionResult.SUCCESS;
            var master=masterPos(p,s);var ms=l.getBlockState(master);
            if(!usable(l,master,ms,player,true))return InteractionResult.FAIL;
            var packed=new ItemStack(this);packed.set(net.minecraft.core.component.DataComponents.BLOCK_STATE,
                net.minecraft.world.item.component.BlockItemStateProperties.EMPTY.with(REMAINING,ms.getValue(REMAINING)));
            l.removeBlock(master,false); // shared multiblock removal clears every matching part, without loot
            if(!player.getInventory().add(packed))Block.popResource(l,master,packed);
            return InteractionResult.SUCCESS;
        }
        return serve(s,l,p,player,false);
    }
    private InteractionResult serve(BlockState s,Level l,BlockPos p,Player player,boolean carve){
        if(l.isClientSide())return InteractionResult.SUCCESS;
        var master=masterPos(p,s);var ms=l.getBlockState(master);
        if(!usable(l,master,ms,player,false))return InteractionResult.FAIL;
        int left=ms.getValue(REMAINING);
        if(left==0||!carve&&!player.canEat(false)){
            player.sendOverlayMessage(net.minecraft.network.chat.Component.literal(left+" / 24 turkey portions remaining"));
            return InteractionResult.SUCCESS;
        }
        for(int i=0;i<CELLS.length;i++)l.setBlock(partPos(master,ms.getValue(FACING),i),ms.setValue(PART,i).setValue(REMAINING,left-1),Block.UPDATE_ALL);
        if(carve){var slice=new ItemStack(JugcraftAgriculture.item("turkey_slice"));if(!player.getInventory().add(slice))Block.popResource(l,p,slice);}
        else player.getFoodData().eat(RoastTurkeyBlock.NUTRITION,RoastTurkeyBlock.SATURATION);
        player.sendOverlayMessage(net.minecraft.network.chat.Component.literal((left-1)+" / 24 turkey portions remaining"));
        l.playSound(null,p,net.minecraft.sounds.SoundEvents.GENERIC_EAT.value(),net.minecraft.sounds.SoundSource.BLOCKS,.8F,1);
        return InteractionResult.SUCCESS;
    }
    @Override protected boolean hasAnalogOutputSignal(BlockState s){return true;}
    @Override protected int getAnalogOutputSignal(BlockState s,Level l,BlockPos p,Direction d){return s.getValue(REMAINING)*15/PORTIONS;}
}
