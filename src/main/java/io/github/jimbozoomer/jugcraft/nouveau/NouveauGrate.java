package io.github.jimbozoomer.jugcraft.nouveau;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;

/** Thin directional grates pass items/projectiles and move a source fluid through their plane. */
public final class NouveauGrate extends Block {
    public static final EnumProperty<Direction> FACING=BlockStateProperties.FACING;
    public NouveauGrate(Properties p){super(p);registerDefaultState(defaultBlockState().setValue(FACING,Direction.UP));}
    protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING);}
    public BlockState getStateForPlacement(BlockPlaceContext c){return defaultBlockState().setValue(FACING,c.getClickedFace());}
    protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return switch(s.getValue(FACING).getAxis()){case X->Block.box(6,0,0,10,16,16);case Y->Block.box(0,0,0,16,3,16);case Z->Block.box(0,0,6,16,16,10);};}
    protected VoxelShape getCollisionShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){if(c instanceof EntityCollisionContext context&&(context.getEntity() instanceof net.minecraft.world.entity.item.ItemEntity||context.getEntity() instanceof net.minecraft.world.entity.projectile.Projectile))return Shapes.empty();return getShape(s,l,p,c);}
    protected void onPlace(BlockState s,Level l,BlockPos p,BlockState old,boolean moving){l.scheduleTick(p,this,2);}
    protected void neighborChanged(BlockState s,Level l,BlockPos p,Block neighbor,net.minecraft.world.level.redstone.Orientation orientation,boolean moving){l.scheduleTick(p,this,2);}
    protected void tick(BlockState s,ServerLevel l,BlockPos p,RandomSource random){
        var direction=s.getValue(FACING);var from=p.relative(direction);var to=p.relative(direction.getOpposite());if(!l.hasChunkAt(from)||!l.hasChunkAt(to))return;var fluid=l.getFluidState(from);var old=l.getBlockState(to);if(fluid.isEmpty()||!fluid.isSource()||!l.getBlockState(from).is(Blocks.WATER)&&!l.getBlockState(from).is(Blocks.LAVA))return;
        if(old.hasProperty(BlockStateProperties.WATERLOGGED)&&!old.getValue(BlockStateProperties.WATERLOGGED)&&fluid.is(net.minecraft.tags.FluidTags.WATER)){l.setBlock(from,Blocks.AIR.defaultBlockState(),3);l.setBlock(to,old.setValue(BlockStateProperties.WATERLOGGED,true),3);}
        else if(old.isAir()){l.setBlock(from,Blocks.AIR.defaultBlockState(),3);l.setBlock(to,fluid.createLegacyBlock(),3);}
    }
    protected InteractionResult useItemOn(ItemStack held,BlockState s,Level l,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){var below=pos.relative(s.getValue(FACING).getOpposite());if(!(player instanceof ServerPlayer p))return InteractionResult.SUCCESS;if(!l.hasChunkAt(below)||!Authority.mayChange(p.level(),p,below))return InteractionResult.FAIL;return held.useOn(new UseOnContext(p,hand,new BlockHitResult(Vec3.atCenterOf(below),s.getValue(FACING),below,false)));}
}
