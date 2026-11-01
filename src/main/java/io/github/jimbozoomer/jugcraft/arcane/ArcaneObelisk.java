package io.github.jimbozoomer.jugcraft.arcane;

import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.finance.ClaimTransfers;
import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.util.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.material.*;
import net.minecraft.world.phys.shapes.*;

/** Original three-block obelisk; gilded base supplies one Aureal each five seconds to the nearest forge. */
public final class ArcaneObelisk extends Block implements SimpleWaterloggedBlock {
    public enum Part implements StringRepresentable {LOWER,MIDDLE,UPPER;public String getSerializedName(){return name().toLowerCase(java.util.Locale.ROOT);}}
    public static final EnumProperty<Part> PART=EnumProperty.create("part",Part.class);
    public static final BooleanProperty ACTIVE=BooleanProperty.create("active"),WATER=BlockStateProperties.WATERLOGGED;
    public ArcaneObelisk(Properties p){super(p.pushReaction(PushReaction.IMMOVEABLE));registerDefaultState(defaultBlockState().setValue(PART,Part.LOWER).setValue(ACTIVE,false).setValue(WATER,false));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState>b){b.add(PART,ACTIVE,WATER);}
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return switch(s.getValue(PART)){case LOWER->Shapes.or(Block.box(0,0,0,16,8,16),Block.box(1,8,1,15,16,15));case MIDDLE->Block.box(2,0,2,14,16,14);case UPPER->Block.box(3,0,3,13,14,13);};}
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){var l=c.getLevel();var p=c.getClickedPos();if(p.getY()+2>=l.getMaxY())return null;for(int y=0;y<3;y++){var q=p.above(y);if(!l.hasChunkAt(q)||!l.getBlockState(q).canBeReplaced(c))return null;if(c.getPlayer() instanceof ServerPlayer player&&!Authority.mayChange(player.level(),player,q))return null;}return defaultBlockState().setValue(ACTIVE,active(l,p)).setValue(WATER,l.getFluidState(p).is(Fluids.WATER));}
    @Override public void setPlacedBy(Level l,BlockPos p,BlockState s,LivingEntity who,ItemStack stack){for(int y=1;y<3;y++)l.setBlock(p.above(y),defaultBlockState().setValue(PART,y==1?Part.MIDDLE:Part.UPPER).setValue(WATER,l.getFluidState(p.above(y)).is(Fluids.WATER)),2);l.updateNeighborsAt(p,this);l.scheduleTick(p,this,100);}
    static boolean form(ServerPlayer player,BlockPos clicked){var l=player.level();var p=clicked;for(int n=0;n<2&&l.getBlockState(p).is(ForbiddenArcane.block("arcane_crystal_block"));n++)p=p.below();if(!l.getBlockState(p).is(ForbiddenArcane.block("arcane_polished_darkstone"))||!l.getBlockState(p.above()).is(ForbiddenArcane.block("arcane_crystal_block"))||!l.getBlockState(p.above(2)).is(ForbiddenArcane.block("arcane_crystal_block")))return false;for(int y=0;y<3;y++)if(!Authority.mayChange(l,player,p.above(y))||!ClaimTransfers.allowed(l,clicked,p.above(y)))return false;var block=(ArcaneObelisk)ForbiddenArcane.block("arcane_crystal_obelisk");for(int y=0;y<3;y++)l.setBlock(p.above(y),block.defaultBlockState().setValue(PART,Part.values()[y]).setValue(ACTIVE,y==0&&active(l,p)),2);l.updateNeighborsAt(p,block);l.scheduleTick(p,block,100);return true;}
    private static boolean active(LevelReader l,BlockPos p){return l.getBlockState(p.below()).is(ForbiddenArcane.block("gilded_chiseled_polished_darkstone"));}
    @Override protected void onPlace(BlockState s,Level l,BlockPos p,BlockState old,boolean moving){super.onPlace(s,l,p,old,moving);if(!l.isClientSide()&&s.getValue(PART)==Part.LOWER)l.scheduleTick(p,this,100);}
    @Override protected void tick(BlockState s,ServerLevel l,BlockPos p,RandomSource r){if(s.getValue(PART)!=Part.LOWER)return;boolean enabled=active(l,p);if(enabled!=s.getValue(ACTIVE))l.setBlock(p,s.setValue(ACTIVE,enabled),3);if(enabled&&l.getBlockState(p.above()).is(this)&&l.getBlockState(p.above(2)).is(this)){ArcaneWorkshop nearest=null;double distance=Double.MAX_VALUE;for(var q:BlockPos.betweenClosed(p.offset(-4,-4,-4),p.offset(4,4,4))){double d=p.distSqr(q);if(d>16||d>=distance||!l.hasChunkAt(q))continue;if(l.getBlockEntity(q) instanceof ArcaneWorkshop forge&&forge.forge()&&forge.acceptsSource(p)){nearest=forge;distance=d;}}if(nearest!=null)nearest.addAureal(1);}l.scheduleTick(p,this,100);}
    @Override protected boolean canSurvive(BlockState s,LevelReader l,BlockPos p){if(s.getValue(PART)==Part.LOWER)return l.getBlockState(p.below()).isFaceSturdy(l,p.below(),Direction.UP);var below=l.getBlockState(p.below());return below.is(this)&&below.getValue(PART).ordinal()==s.getValue(PART).ordinal()-1;}
    @Override protected BlockState updateShape(BlockState s,LevelReader l,ScheduledTickAccess t,BlockPos p,Direction face,BlockPos q,BlockState other,RandomSource r){if(s.getValue(WATER))t.scheduleTick(p,Fluids.WATER,Fluids.WATER.getTickDelay(l));var part=s.getValue(PART);boolean missing=!s.canSurvive(l,p);if(face==Direction.UP&&part!=Part.UPPER)missing|=!other.is(this)||other.getValue(PART).ordinal()!=part.ordinal()+1;return missing?(s.getValue(WATER)?Blocks.WATER:Blocks.AIR).defaultBlockState():s;}
    @Override public BlockState playerWillDestroy(Level l,BlockPos p,BlockState s,Player player){if(!l.isClientSide()&&player.hasInfiniteMaterials()&&s.getValue(PART)!=Part.LOWER){var root=p.below(s.getValue(PART).ordinal());var below=l.getBlockState(root);if(below.is(this))l.setBlock(root,(below.getValue(WATER)?Blocks.WATER:Blocks.AIR).defaultBlockState(),35);}return super.playerWillDestroy(l,p,s,player);}
    @Override protected FluidState getFluidState(BlockState s){return s.getValue(WATER)?Fluids.WATER.getSource(false):super.getFluidState(s);}
}
