package io.github.jimbozoomer.jugcraft.menagerie;

import net.minecraft.core.*;
import net.minecraft.util.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;

/** A real vanilla bed, retaining native sleep/respawn/dimension rules and supplied joined frames. */
public class CraftedBedBlock extends BedBlock {
    public enum Join implements StringRepresentable {
        SINGLE, LEFT, MIDDLE, RIGHT;
        @Override public String getSerializedName(){return name().toLowerCase(java.util.Locale.ROOT);}
    }
    public static final EnumProperty<DyeColor> COLOR=EnumProperty.create("color",DyeColor.class);
    public static final EnumProperty<Join> SHAPE=EnumProperty.create("shape",Join.class);
    public CraftedBedBlock(Properties p){super(DyeColor.WHITE,p);registerDefaultState(defaultBlockState().setValue(COLOR,DyeColor.WHITE).setValue(SHAPE,Join.SINGLE));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){super.createBlockStateDefinition(b);b.add(COLOR,SHAPE);}
    private BlockState connect(BlockState s,LevelReader l,BlockPos p){
        Direction f=s.getValue(FACING);
        var a=l.getBlockState(p.relative(f.getCounterClockWise()));var b=l.getBlockState(p.relative(f.getClockWise()));
        boolean left=a.is(this)&&a.getValue(FACING)==f&&a.getValue(PART)==s.getValue(PART),right=b.is(this)&&b.getValue(FACING)==f&&b.getValue(PART)==s.getValue(PART);
        return s.setValue(SHAPE,left?(right?Join.MIDDLE:Join.RIGHT):(right?Join.LEFT:Join.SINGLE));
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){
        var s=super.getStateForPlacement(c);if(s==null)return null;
        if(c.getPlayer()!=null&&!Menagerie.mayChange(c.getLevel(),c.getPlayer(),c.getClickedPos().relative(s.getValue(FACING))))return null;
        return connect(s,c.getLevel(),c.getClickedPos());
    }
    @Override protected BlockState updateShape(BlockState s,LevelReader l,ScheduledTickAccess t,BlockPos p,Direction d,BlockPos n,BlockState other,RandomSource r){
        var next=super.updateShape(s,l,t,p,d,n,other,r);return next.is(this)?connect(next,l,p):next;
    }
    @Override protected InteractionResult useItemOn(ItemStack stack,BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit){
        String item=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        DyeColor color=stack.has(net.minecraft.core.component.DataComponents.DYE)?stack.get(net.minecraft.core.component.DataComponents.DYE):item.startsWith("crafted_")&&item.endsWith("_sheet")?DyeColor.byName(item.substring(8,item.length()-6),null):null;
        if(color==null)return InteractionResult.TRY_WITH_EMPTY_HAND;
        BlockPos partner=p.relative(getConnectedDirection(s));
        if(!Menagerie.mayChange(l,player,p)||!Menagerie.mayChange(l,player,partner))return InteractionResult.FAIL;
        if(!l.isClientSide()&&color!=s.getValue(COLOR)){
            l.setBlockAndUpdate(p,s.setValue(COLOR,color));
            var other=l.getBlockState(partner);if(other.is(this))l.setBlockAndUpdate(partner,other.setValue(COLOR,color));
            stack.consume(1,player);
        }
        return InteractionResult.SUCCESS;
    }
}
