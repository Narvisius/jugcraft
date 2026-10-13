package local.peepo;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.minecraft.core.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
public final class LunchBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACE=BlockStateProperties.FACING;
    public final boolean cover;
    public LunchBlock(Properties p,boolean cover){super(p);this.cover=cover;registerDefaultState(stateDefinition.any().setValue(FACE,Direction.UP));}
    protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACE);}
    public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new LunchBlockEntity(pos,state);}
    protected RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
    protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){
        if(!cover)return Block.box(1,0,1,15,12,15);
        return switch(s.getValue(FACE)){case UP->Block.box(1,0,1,15,1,15);case DOWN->Block.box(1,15,1,15,16,15);case NORTH->Block.box(1,1,15,15,15,16);case SOUTH->Block.box(1,1,0,15,15,1);case WEST->Block.box(15,1,1,16,15,15);case EAST->Block.box(0,1,1,1,15,15);};
    }
    protected VoxelShape getCollisionShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return cover?Shapes.empty():getShape(s,l,p,c);}
    public BlockState getStateForPlacement(BlockPlaceContext c){
        var state=defaultBlockState().setValue(FACE,cover?c.getClickedFace():Direction.UP);
        if(cover){var target=c.getClickedPos().relative(state.getValue(FACE).getOpposite());if(ItemStorage.SIDED.find(c.getLevel(),target,state.getValue(FACE))==null)return null;}
        return state;
    }
    protected BlockState updateShape(BlockState s,LevelReader level,ScheduledTickAccess ticks,BlockPos pos,Direction direction,BlockPos other,BlockState neighbor,RandomSource random){
        if(cover && direction==s.getValue(FACE).getOpposite() && neighbor.isAir())return Blocks.AIR.defaultBlockState();return s;
    }
    public void setPlacedBy(Level level,BlockPos pos,BlockState state,LivingEntity placer,ItemStack stack){
        if(!level.isClientSide() && placer instanceof Player p && level.getBlockEntity(pos) instanceof LunchBlockEntity lunch)lunch.setOwner(p.getUUID());
    }
    protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){
        if(!level.isClientSide() && level.getBlockEntity(pos) instanceof LunchBlockEntity lunch){
            if(!lunch.allowed(player)){player.sendOverlayMessage(net.minecraft.network.chat.Component.literal("This lunch source belongs to another player."));return InteractionResult.SUCCESS;}
            if(player.isShiftKeyDown()){if(lunch.isOwner(player))lunch.toggleParty();player.sendOverlayMessage(net.minecraft.network.chat.Component.literal(lunch.party()?"Lunch access: owner and party":"Lunch access: owner only"));}
            else if(!cover)player.openMenu(lunch);
            else player.sendOverlayMessage(net.minecraft.network.chat.Component.literal("Lunch cover active. Open the container from another face. Shift-click to change sharing."));
        }
        return InteractionResult.SUCCESS;
    }
}
