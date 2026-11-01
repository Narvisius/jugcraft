package io.github.jimbozoomer.jugcraft.menagerie;

import com.google.gson.JsonObject;
import java.util.List;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;
import net.minecraft.world.level.storage.loot.LootParams;

/** The supplied attached meshes with native storage, support and food interactions. */
public final class OwnerAmendmentBlock extends HouseholdBlock implements EntityBlock,SimpleWaterloggedBlock {
    OwnerAmendmentBlock(Properties p,JsonObject d){super(p,d);}
    @Override public void animateTick(BlockState state,Level level,BlockPos pos,net.minecraft.util.RandomSource random){super.animateTick(state,level,pos,random);if(name.endsWith("cauldron"))OwnerCauldronHeat.animate(state,level,pos,random);}
    @Override protected void entityInside(BlockState state,Level level,BlockPos pos,net.minecraft.world.entity.Entity entity,net.minecraft.world.entity.InsideBlockEffectApplier effects,boolean pastEdges){super.entityInside(state,level,pos,entity,effects,pastEdges);if(name.endsWith("cauldron"))OwnerCauldronHeat.inside(state,level,pos,entity);}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return name.contains("cake")||name.equals("water_lily_pad")?null:new OwnerAmendmentData(p,s);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){
        if(level.isClientSide()||type!=OwnerAmendments.DATA||!name.endsWith("cauldron"))return null;
        return (l,p,s,be)->OwnerAmendmentCauldrons.tick((ServerLevel)l,p,(OwnerAmendmentData)be);
    }
    @Override protected FluidState getFluidState(BlockState s){return name.equals("water_lily_pad")||flag(s,"waterlogged")?Fluids.WATER.getSource(false):super.getFluidState(s);}
    @Override protected boolean canSurvive(BlockState s,LevelReader l,BlockPos p){
        if(name.equals("water_lily_pad")||name.endsWith("cauldron"))return true;
        if(name.startsWith("ceiling_banner"))return l.getBlockState(p.above()).isFaceSturdy(l,p.above(),Direction.DOWN);
        if(name.startsWith("skull"))return !name.endsWith("wall")||l.getBlockState(p.relative(facing(s).getOpposite())).isSolid();
        if(name.contains("cake"))return l.getBlockState(p.below()).isSolid();
        var side=name.equals("hanging_flower_pot")?Direction.UP:facing(s).getOpposite();var support=p.relative(side);if(!l.hasChunkAt(support))return false;var b=l.getBlockState(support);
        return b.isFaceSturdy(l,support,side.getOpposite())||name.contains("lantern")&&(b.getBlock() instanceof FenceBlock||b.getBlock() instanceof WallBlock||b.getBlock() instanceof IronBarsBlock)||name.equals("hanging_flower_pot")&&b.is(Blocks.IRON_CHAIN);
    }
    @Override protected BlockState updateShape(BlockState s,LevelReader l,ScheduledTickAccess ticks,BlockPos p,Direction dir,BlockPos q,BlockState other,net.minecraft.util.RandomSource random){
        if(name.equals("water_lily_pad")){ticks.scheduleTick(p,Fluids.WATER,Fluids.WATER.getTickDelay(l));if(dir==Direction.UP)ticks.scheduleTick(p,this,1);return s;}
        if(flag(s,"waterlogged")&&name.startsWith("skull_candle"))s=set(s,"lit","false");
        if(flag(s,"waterlogged"))ticks.scheduleTick(p,Fluids.WATER,Fluids.WATER.getTickDelay(l));
        if(name.contains("lantern")&&!canSurvive(s,l,p)){ticks.scheduleTick(p,this,1);return s;}
        return canSurvive(s,l,p)?s:flag(s,"waterlogged")?Blocks.WATER.defaultBlockState():Blocks.AIR.defaultBlockState();
    }
    @Override protected void tick(BlockState state,ServerLevel level,BlockPos pos,net.minecraft.util.RandomSource random){
        if(name.equals("water_lily_pad"))OwnerLilySupport.restore(level,pos);
        else if(name.contains("lantern")&&!canSurvive(state,level,pos)){
            if(level.hasChunkAt(pos.below())&&FallingBlock.isFree(level.getBlockState(pos.below()))&&level.getBlockEntity(pos) instanceof OwnerAmendmentData data&&data.base.getBlock() instanceof LanternBlock){
                var falling=data.base.setValue(LanternBlock.HANGING,false).setValue(LanternBlock.WATERLOGGED,flag(state,"waterlogged"));
                net.minecraft.world.entity.item.FallingBlockEntity.fall(level,pos,falling);
            }else level.destroyBlock(pos,true);
        }
    }
    @Override protected VoxelShape getBlockSupportShape(BlockState s,BlockGetter l,BlockPos p){return name.equals("water_lily_pad")?Block.box(0,15,0,16,16,16):super.getBlockSupportShape(s,l,p);}
    @Override protected InteractionResult useItemOn(ItemStack stack,BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit){
        if(!Menagerie.mayChange(l,player,p))return InteractionResult.FAIL;
        if(!(l.getBlockEntity(p) instanceof OwnerAmendmentData d))return InteractionResult.PASS;
        if(name.startsWith("skull_candle")&&d.decoration.getBlock() instanceof CandleBlock){
            if(stack.getItem() instanceof BlockItem candle&&candle.getBlock()==d.decoration.getBlock()&&d.decoration.getValue(CandleBlock.CANDLES)<4){
                if(!l.isClientSide()){d.decoration=d.decoration.cycle(CandleBlock.CANDLES);l.setBlockAndUpdate(p,set(s,"candles",Integer.toString(d.decoration.getValue(CandleBlock.CANDLES))));stack.consume(1,player);d.changed();}return InteractionResult.SUCCESS;
            }
            if((stack.is(Items.FLINT_AND_STEEL)||stack.is(Items.FIRE_CHARGE))&&!flag(s,"lit")&&!flag(s,"waterlogged")){
                if(!l.isClientSide()){l.setBlockAndUpdate(p,set(s,"lit","true"));d.decoration=d.decoration.setValue(CandleBlock.LIT,true);d.changed();if(stack.is(Items.FLINT_AND_STEEL))stack.hurtAndBreak(1,player,hand.asEquipmentSlot());else stack.consume(1,player);}return InteractionResult.SUCCESS;
            }
        }
        if(name.equals("hanging_flower_pot")&&d.decoration.isAir()){
            var plant=OwnerAmendments.potted(stack);if(plant==null)return InteractionResult.PASS;
            if(!l.isClientSide()){d.decoration=((BlockItem)stack.getItem()).getBlock().defaultBlockState();stack.consume(1,player);d.changed();}return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult hit){
        if(!Menagerie.mayChange(l,player,p))return InteractionResult.FAIL;
        if(name.contains("cake")){
            if(!player.canEat(false))return InteractionResult.PASS;
            if(!l.isClientSide()){
                player.getFoodData().eat(2,.1F);player.awardStat(net.minecraft.stats.Stats.EAT_CAKE_SLICE);int bites=Integer.parseInt(value(s,"bites"));
                if(bites<6){s=set(s,"bites",Integer.toString(bites+1));if(bites==0)s=set(s,"facing",player.getDirection().getOpposite().getName());l.setBlockAndUpdate(p,s);}
                else l.setBlockAndUpdate(p,name.equals("double_cake")?Blocks.CAKE.defaultBlockState():Blocks.AIR.defaultBlockState());
                l.gameEvent(player,net.minecraft.world.level.gameevent.GameEvent.EAT,p);
            }return InteractionResult.SUCCESS;
        }
        if(!(l.getBlockEntity(p) instanceof OwnerAmendmentData d))return InteractionResult.PASS;
        if(name.startsWith("skull_candle")&&flag(s,"lit")&&!player.isShiftKeyDown()){
            if(!l.isClientSide()){l.setBlockAndUpdate(p,set(s,"lit","false"));d.decoration=d.decoration.setValue(CandleBlock.LIT,false);d.changed();l.playSound(null,p,net.minecraft.sounds.SoundEvents.FIRE_EXTINGUISH,net.minecraft.sounds.SoundSource.BLOCKS,.5F,1);}return InteractionResult.SUCCESS;
        }
        if(name.equals("skull_pile")||name.startsWith("skull_candle")&&player.isShiftKeyDown()){if(!l.isClientSide())d.restore(player);return InteractionResult.SUCCESS;}
        if(name.equals("tool_hook")){if(!l.isClientSide())d.restore(player);return InteractionResult.SUCCESS;}
        if(name.equals("hanging_flower_pot")&&!d.decoration.isAir()){
            if(!l.isClientSide()){var plant=new ItemStack(d.decoration.getBlock());d.decoration=Blocks.AIR.defaultBlockState();d.changed();player.getInventory().placeItemBackInInventory(plant,net.minecraft.util.Prediction.SERVER_ONLY);}return InteractionResult.SUCCESS;
        }return InteractionResult.PASS;
    }
    @Override protected List<ItemStack> getDrops(BlockState s,LootParams.Builder b){return name.equals("water_lily_pad")?List.of(new ItemStack(Items.LILY_PAD)):name.contains("cake")?List.of():OwnerAmendmentData.drops(b);}
    @Override protected ItemStack getCloneItemStack(LevelReader l,BlockPos p,BlockState s,boolean data){return name.contains("cake")?new ItemStack(Items.CAKE):l.getBlockEntity(p) instanceof OwnerAmendmentData d?new ItemStack(d.base.getBlock()):ItemStack.EMPTY;}
    @Override protected boolean hasAnalogOutputSignal(BlockState s){return name.contains("cake")||name.endsWith("cauldron");}
    @Override protected int getAnalogOutputSignal(BlockState s,Level l,BlockPos p,Direction dir){return name.contains("cake")?(7-Integer.parseInt(value(s,"bites")))*2:name.endsWith("cauldron")?Integer.parseInt(value(s,"level")):0;}
    private static VoxelShape rotateShape(VoxelShape shape,Direction facing){int turns=switch(facing){case EAST->1;case SOUTH->2;case WEST->3;default->0;};for(int i=0;i<turns;i++){var boxes=shape.toAabbs();shape=Shapes.empty();for(var b:boxes)shape=Shapes.or(shape,Shapes.box(1-b.maxZ,b.minY,b.minX,1-b.minZ,b.maxY,b.maxX));}return shape;}
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){
        if(name.equals("water_lily_pad"))return Block.box(1,15,1,15,16,15);
        if(name.startsWith("ceiling_banner"))return Block.box(3,-16,7,13,16,9);
        if(name.startsWith("skull"))return name.equals("skull_pile")?Block.box(4,0,4,12,16,12):name.endsWith("wall")?Block.box(4,0,4,12,12,12):Block.box(4,0,4,12,12,12);
        if(name.endsWith("cauldron"))return Blocks.CAULDRON.defaultBlockState().getShape(l,p,c);
        if(name.contains("cake")){int bite=Integer.parseInt(value(s,"bites"));VoxelShape shape=name.equals("double_cake")?Shapes.or(Block.box(1,0,1,15,8,15),Block.box(2,8,2+Math.max(0,bite*2-1),14,16,14)):Block.box(1,0,1+bite*2,15,8,15);return rotateShape(shape,facing(s));}
        if(name.equals("hanging_flower_pot"))return Block.box(4,0,4,12,16,12);
        if(name.equals("tool_hook")){return switch(facing(s)){case NORTH->Block.box(5,5,10,11,15,16);case SOUTH->Block.box(5,5,0,11,15,6);case WEST->Block.box(10,5,5,16,15,11);default->Block.box(0,5,5,6,15,11);};}
        return super.getShape(s,l,p,c);
    }
}
