package io.github.jimbozoomer.jugcraft.jimtafacts.industry;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.energy.*;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

/** Source luminator bounds, active/dark sheets and redstone polarity on the shared JE network. */
public final class IndustrialLighting {
    public static Block LUMINATOR;
    public static BlockEntityType<Node> TYPE;
    public static final EnumProperty<Direction> FACING=BlockStateProperties.FACING;
    public static final BooleanProperty LIT=BlockStateProperties.LIT, COMPACT=BooleanProperty.create("compact");
    private IndustrialLighting() {}
    public static void register() {
        var key=ResourceKey.create(Registries.BLOCK,Jugcraft.id("jimta_luminator"));
        LUMINATOR=Registry.register(BuiltInRegistries.BLOCK,key,new Lamp(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).setId(key).strength(.5F).noOcclusion().lightLevel(s->s.getValue(LIT)?15:0)));
        var item=ResourceKey.create(Registries.ITEM,key.identifier());Registry.register(BuiltInRegistries.ITEM,item,new BlockItem(LUMINATOR,new Item.Properties().setId(item).useBlockDescriptionPrefix()));
        TYPE=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,Jugcraft.id("jimta_luminator"),FabricBlockEntityTypeBuilder.create(Node::new,LUMINATOR).build());
        EnergyStorage.SIDED.registerForBlockEntity((node,side)->side==node.getBlockState().getValue(FACING).getOpposite()?node.energy:null,TYPE);
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(out->out.accept(LUMINATOR));
    }
    public static VoxelShape shape(Direction facing,boolean compact) {
        return switch(facing) {
            case UP->Block.box(0,0,0,16,1,16);
            case DOWN->compact?Block.box(4,13,4,12,16,12):Block.box(0,15,0,16,16,16);
            case NORTH->compact?Block.box(6,3,14,10,13,16):Block.box(0,0,15,16,16,16);
            case SOUTH->compact?Block.box(6,3,0,10,13,2):Block.box(0,0,0,16,16,1);
            case WEST->compact?Block.box(14,3,6,16,13,10):Block.box(15,0,0,16,16,16);
            case EAST->compact?Block.box(0,3,6,2,13,10):Block.box(0,0,0,1,16,16);
        };
    }
    public static final class Lamp extends Block implements EntityBlock,EnergyConnectable {
        Lamp(Properties p){super(p);registerDefaultState(defaultBlockState().setValue(FACING,Direction.UP).setValue(LIT,false).setValue(COMPACT,false));}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING,LIT,COMPACT);}
        @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return shape(s.getValue(FACING),s.getValue(COMPACT));}
        @Override public BlockState getStateForPlacement(BlockPlaceContext c){var facing=c.getClickedFace();var s=defaultBlockState().setValue(FACING,facing);return canSurvive(s,c.getLevel(),c.getClickedPos())?updated(s,c.getLevel(),c.getClickedPos()):null;}
        @Override protected boolean canSurvive(BlockState s,LevelReader l,BlockPos p){var face=s.getValue(FACING);var at=p.relative(face.getOpposite());return l.hasChunkAt(at)&&(l.getBlockState(at).getBlock() instanceof CableBlock||l.getBlockState(at).isFaceSturdy(l,at,face));}
        private BlockState updated(BlockState s,LevelReader l,BlockPos p){return s.setValue(COMPACT,l.getBlockState(p.relative(s.getValue(FACING).getOpposite())).getBlock() instanceof CableBlock);}
        @Override protected BlockState updateShape(BlockState s,LevelReader l,ScheduledTickAccess ticks,BlockPos p,Direction d,BlockPos n,BlockState ns,RandomSource random){
            if(d==s.getValue(FACING).getOpposite())return canSurvive(s,l,p)?updated(s,l,p):Blocks.AIR.defaultBlockState();return s;
        }
        @Override protected void onPlace(BlockState s,Level l,BlockPos p,BlockState old,boolean moved){EnergyNetworks.invalidate(l);}
        @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new Node(p,s);}
        @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> type){return !l.isClientSide()&&type==TYPE?(world,p,state,e)->{if(world.getGameTime()%4==0)((Node)e).step();}:null;}
        @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult hit){
            if(!Authority.mayChange(l,player,p))return InteractionResult.FAIL;
            if(!l.isClientSide()&&l.getBlockEntity(p) instanceof Node node){node.inverted=!node.inverted;node.setChanged();player.sendOverlayMessage(Component.literal(node.inverted?"Luminator: redstone turns light ON":"Luminator: redstone turns light OFF"));}
            return InteractionResult.SUCCESS;
        }
    }
    public static final class Node extends BlockEntity {
        public final SimpleEnergyStorage energy=new SimpleEnergyStorage(2,2,0,this::setChanged);
        public boolean inverted;
        Node(BlockPos p,BlockState s){super(TYPE,p,s);}
        public void step(){
            if(!(level instanceof ServerLevel server))return;
            boolean lit=energy.getAmount()>=1&&(server.hasNeighborSignal(worldPosition)==inverted);
            if(lit)energy.setAmount(energy.getAmount()-1);
            if(getBlockState().getValue(LIT)!=lit)server.setBlockAndUpdate(worldPosition,getBlockState().setValue(LIT,lit));
        }
        @Override protected void saveAdditional(ValueOutput out){super.saveAdditional(out);out.putLong("energy",energy.getAmount());out.putBoolean("inverted",inverted);}
        @Override protected void loadAdditional(ValueInput in){super.loadAdditional(in);energy.setAmount(in.getLongOr("energy",0));inverted=in.getBooleanOr("inverted",false);}
    }
}
