package io.github.jimbozoomer.jugcraft.wilds;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import net.minecraft.core.*;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;

/** Nine occupied cells, one recoverable gong, original damped swing and redstone/projectile ringing. */
public final class WildGong extends BlockEntity {
    public long struck=-1000;
    public Direction strikeDirection=Direction.NORTH;
    public WildGong(BlockPos p,BlockState s){super(OwnerWilds.GONG,p,s);}
    public static final EnumProperty<net.minecraft.core.Direction> FACING=BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty POWERED=BlockStateProperties.POWERED;
    public static final IntegerProperty SIDE=IntegerProperty.create("side",0,2),HEIGHT=IntegerProperty.create("height",0,2);
    public static BlockPos root(BlockState s,BlockPos p){return p.below(s.getValue(HEIGHT)).relative(s.getValue(FACING).getClockWise(),1-s.getValue(SIDE));}
    public void ring(Direction face){if(!(level instanceof ServerLevel l)||l.getGameTime()-struck<4)return;struck=l.getGameTime();strikeDirection=face;setChanged();l.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);l.playSound(null,worldPosition,OwnerWilds.sound("block.gong"),net.minecraft.sounds.SoundSource.BLOCKS,2,1);l.gameEvent(null,net.minecraft.world.level.gameevent.GameEvent.BLOCK_ACTIVATE,worldPosition);}
    @Override protected void saveAdditional(ValueOutput out){super.saveAdditional(out);out.putLong("struck",struck);out.putInt("direction",strikeDirection.get3DDataValue());}
    @Override protected void loadAdditional(ValueInput in){super.loadAdditional(in);struck=in.getLongOr("struck",-1000);strikeDirection=Direction.from3DDataValue(in.getIntOr("direction",2));}
    @Override public net.minecraft.nbt.CompoundTag getUpdateTag(HolderLookup.Provider lookup){return saveWithoutMetadata(lookup);}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
    public static final class GongBlock extends BaseEntityBlock {
        private final boolean controller;
        public GongBlock(boolean controller,Properties p){super(p.noOcclusion().randomTicks().pushReaction(net.minecraft.world.level.material.PushReaction.POPPED));this.controller=controller;registerDefaultState(defaultBlockState().setValue(FACING,Direction.NORTH).setValue(POWERED,false).setValue(SIDE,1).setValue(HEIGHT,0));}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING,POWERED,SIDE,HEIGHT);}
        @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return controller?new WildGong(p,s):null;}
        @Override protected BlockState rotate(BlockState state,Rotation rotation){return state.setValue(FACING,rotation.rotate(state.getValue(FACING)));}
        @Override protected BlockState mirror(BlockState state,Mirror mirror){return mirror==Mirror.NONE?state:state.setValue(FACING,mirror.mirror(state.getValue(FACING))).setValue(SIDE,2-state.getValue(SIDE));}
        private void reconcile(ServerLevel level,BlockPos pos,BlockState state){
            var base=root(state,pos);if(!level.hasChunkAt(base))return;
            var master=level.getBlockState(base);
            if(!controller){
                if(!master.is(OwnerWilds.block("gong"))||master.getValue(FACING)!=state.getValue(FACING))level.removeBlock(pos,false);
                return;
            }
            // An unloaded neighbor is unknown, not missing. Random ticks resume
            // validation when both sides of a chunk boundary are available again.
            for(int y=0;y<3;y++)for(int x=-1;x<=1;x++){
                if(x==0&&y==0)continue;var part=base.above(y).relative(state.getValue(FACING).getClockWise(),x);
                if(!level.hasChunkAt(part))return;
            }
            for(int y=0;y<3;y++)for(int x=-1;x<=1;x++){
                if(x==0&&y==0)continue;var at=base.above(y).relative(state.getValue(FACING).getClockWise(),x);var part=level.getBlockState(at);
                if(!part.is(OwnerWilds.block("gong_part"))||!root(part,at).equals(base)||part.getValue(FACING)!=state.getValue(FACING)){level.destroyBlock(base,true);return;}
            }
        }
        @Override protected void randomTick(BlockState state,ServerLevel level,BlockPos pos,net.minecraft.util.RandomSource random){reconcile(level,pos,state);}
        @Override protected RenderShape getRenderShape(BlockState s){return RenderShape.INVISIBLE;}
        @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return s.getValue(FACING).getAxis()==Direction.Axis.X?Block.box(4,0,0,12,16,16):Block.box(0,0,4,16,16,12);}
        @Override public BlockState getStateForPlacement(BlockPlaceContext c){
            if(!controller)return null;var facing=c.getHorizontalDirection();var at=c.getClickedPos();var l=c.getLevel();
            for(int y=0;y<3;y++)for(int x=-1;x<=1;x++){var p=at.above(y).relative(facing.getClockWise(),x);if(!l.isInWorldBounds(p)||!l.hasChunkAt(p)||!l.getBlockState(p).canBeReplaced(c)||l.getBlockEntity(p)!=null||!l.getWorldBorder().isWithinBounds(p)||c.getPlayer() instanceof ServerPlayer actor&&!Authority.mayChange(actor.level(),actor,p))return null;}
            return defaultBlockState().setValue(FACING,facing);
        }
        @Override public void setPlacedBy(Level l,BlockPos at,BlockState s,LivingEntity actor,ItemStack stack){super.setPlacedBy(l,at,s,actor,stack);if(!controller||l.isClientSide())return;var facing=s.getValue(FACING);for(int y=0;y<3;y++)for(int x=-1;x<=1;x++)if(x!=0||y!=0)l.setBlock(at.above(y).relative(facing.getClockWise(),x),OwnerWilds.block("gong_part").defaultBlockState().setValue(FACING,facing).setValue(SIDE,x+1).setValue(HEIGHT,y),3);}
        @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos at,Player p,BlockHitResult hit){if(hit.getDirection().getAxis()!=s.getValue(FACING).getAxis())return InteractionResult.PASS;var base=root(s,at);if(p instanceof ServerPlayer actor){if(!Authority.mayChange(actor.level(),actor,base))return InteractionResult.FAIL;if(l.getBlockEntity(base) instanceof WildGong gong)gong.ring(hit.getDirection());}return InteractionResult.SUCCESS;}
        @Override protected void onProjectileHit(Level l,BlockState s,BlockHitResult hit,Projectile projectile){if(projectile.getOwner() instanceof ServerPlayer p&&hit.getDirection().getAxis()==s.getValue(FACING).getAxis()){var base=root(s,hit.getBlockPos());if(Authority.mayChange(p.level(),p,base)&&l.getBlockEntity(base) instanceof WildGong gong)gong.ring(hit.getDirection());}}
        @Override protected void neighborChanged(BlockState s,Level l,BlockPos p,Block changed,net.minecraft.world.level.redstone.Orientation orientation,boolean moved){if(l.isClientSide())return;var base=root(s,p);if(!l.hasChunkAt(base)||!(l.getBlockEntity(base) instanceof WildGong gong))return;var state=gong.getBlockState();boolean powered=false;for(int y=0;y<3;y++)for(int x=-1;x<=1;x++){var q=base.above(y).relative(state.getValue(FACING).getClockWise(),x);if(l.hasChunkAt(q)&&l.hasNeighborSignal(q))powered=true;}if(powered!=state.getValue(POWERED)){l.setBlock(base,state.setValue(POWERED,powered),2);if(powered)gong.ring(state.getValue(FACING));}}
        @Override protected void affectNeighborsAfterRemoval(BlockState s,ServerLevel l,BlockPos p,boolean moved){super.affectNeighborsAfterRemoval(s,l,p,moved);var base=root(s,p);if(!controller){if(l.hasChunkAt(base)&&l.getBlockState(base).is(OwnerWilds.block("gong")))l.destroyBlock(base,true);return;}var facing=s.getValue(FACING);for(int y=0;y<3;y++)for(int x=-1;x<=1;x++){var q=base.above(y).relative(facing.getClockWise(),x);if(!l.hasChunkAt(q))continue;var part=l.getBlockState(q);if(part.is(OwnerWilds.block("gong_part"))&&root(part,q).equals(base))l.removeBlock(q,false);}}
        @Override public BlockState playerWillDestroy(Level l,BlockPos p,BlockState s,Player player){if(!l.isClientSide()&&player.hasInfiniteMaterials()&&!controller){var base=root(s,p);if(l.getBlockState(base).is(OwnerWilds.block("gong")))l.destroyBlock(base,false);}return super.playerWillDestroy(l,p,s,player);}
    }
}
