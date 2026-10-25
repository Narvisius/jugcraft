package io.github.jimbozoomer.jugcraft.jimtafacts.industry;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.energy.*;
import java.util.*;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

/** Original cable silhouettes/skins on JE. Detector reports committed bus traffic; splitter isolates it. */
public final class IndustrialCables {
    public record Spec(int width,long rate,boolean paintable) {}
    public static final Map<String,Spec> SPECS=new LinkedHashMap<>();
    public static final Map<String,Block> BLOCKS=new LinkedHashMap<>();
    public static final IntegerProperty COLOR=IntegerProperty.create("color",0,16);
    public static final BooleanProperty ACTIVE=BlockStateProperties.POWERED;
    public static BlockEntityType<DetectorNode> TYPE;
    static {
        SPECS.put("copper",new Spec(4,128,false));SPECS.put("insulated_copper",new Spec(6,128,true));
        SPECS.put("gold",new Spec(3,512,false));SPECS.put("insulated_gold",new Spec(6,512,true));SPECS.put("double_insulated_gold",new Spec(6,512,true));
        SPECS.put("iron",new Spec(6,2048,false));SPECS.put("insulated_iron",new Spec(10,2048,true));SPECS.put("double_insulated_iron",new Spec(10,2048,true));SPECS.put("quad_insulated_iron",new Spec(12,2048,true));
        SPECS.put("glass",new Spec(4,8192,true));SPECS.put("tin",new Spec(4,32,false));SPECS.put("insulated_tin",new Spec(6,32,true));
        SPECS.put("detector",new Spec(8,8192,false));SPECS.put("splitter",new Spec(8,8192,false));
    }
    private IndustrialCables() {}
    public static void register() {
        SPECS.forEach((name,spec)->{
            var key=ResourceKey.create(Registries.BLOCK,Jugcraft.id("jimta_"+name+"_cable"));
            var p=BlockBehaviour.Properties.ofFullCopy(Blocks.COPPER_BLOCK.weathering().unaffected()).noOcclusion().setId(key);
            Block block=name.equals("detector")?new Detector(p,spec):name.equals("splitter")?new Splitter(p,spec):spec.paintable()?new Painted(p,spec):new Wire(p,spec);
            BLOCKS.put(name,Registry.register(BuiltInRegistries.BLOCK,key,block));
            var item=ResourceKey.create(Registries.ITEM,key.identifier());
            Registry.register(BuiltInRegistries.ITEM,item,new BlockItem(block,new Item.Properties().setId(item).useBlockDescriptionPrefix()));
        });
        TYPE=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,Jugcraft.id("jimta_detector_cable"),FabricBlockEntityTypeBuilder.create(DetectorNode::new,BLOCKS.get("detector")).build());
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(out->BLOCKS.values().forEach(out::accept));
    }
    static class Wire extends CableBlock {
        final Spec spec;
        Wire(Properties p,Spec spec){super(p,spec.rate());this.spec=spec;}
        @Override protected VoxelShape getShape(BlockState state,BlockGetter l,BlockPos p,CollisionContext c){
            double low=(16-spec.width())/2.,high=16-low;VoxelShape shape=Block.box(low,low,low,high,high,high);
            for(Direction side:Direction.values())if(state.getValue(PROPERTY_BY_DIRECTION.get(side)))shape=Shapes.or(shape,switch(side){
                case NORTH->Block.box(low,low,0,high,high,low);case SOUTH->Block.box(low,low,high,high,high,16);
                case WEST->Block.box(0,low,low,low,high,high);case EAST->Block.box(high,low,low,16,high,high);
                case DOWN->Block.box(low,0,low,high,low,high);case UP->Block.box(low,high,low,high,16,high);
            });return shape;
        }
        @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult hit){
            if(!l.isClientSide()&&Authority.mayChange(l,player,p))player.sendOverlayMessage(Component.literal(spec.rate()+" JE per push"+(this instanceof Splitter?" · redstone disconnects this cable":this instanceof Detector?" · redstone reports activity on this connected bus":" · uses the shared JE network")));
            return InteractionResult.SUCCESS;
        }
    }
    static final class Painted extends Wire {
        Painted(Properties p,Spec spec){super(p,spec);registerDefaultState(defaultBlockState().setValue(COLOR,0));}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){super.createBlockStateDefinition(b);b.add(COLOR);}
        @Override protected InteractionResult useItemOn(ItemStack stack,BlockState s,Level l,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
            if(!(stack.getItem() instanceof DyeItem dye))return InteractionResult.PASS;
            if(!Authority.mayChange(l,player,pos)||player.isSpectator())return InteractionResult.FAIL;
            int color=dye.components().getOrDefault(net.minecraft.core.component.DataComponents.DYE,net.minecraft.world.item.DyeColor.WHITE).getId()+1;
            if(s.getValue(COLOR)==color)return InteractionResult.SUCCESS;
            if(!l.isClientSide()){l.setBlock(pos,s.setValue(COLOR,color),Block.UPDATE_CLIENTS);stack.consume(1,player);}
            return InteractionResult.SUCCESS;
        }
    }
    static final class Splitter extends Wire {
        Splitter(Properties p,Spec spec){super(p,spec);registerDefaultState(defaultBlockState().setValue(ACTIVE,false));}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){super.createBlockStateDefinition(b);b.add(ACTIVE);}
        @Override public long transferRate(BlockState s){return s.getValue(ACTIVE)?0:super.transferRate(s);}
        @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext c){return super.getStateForPlacement(c).setValue(ACTIVE,c.getLevel().hasNeighborSignal(c.getClickedPos()));}
        @Override protected void onPlace(BlockState s,Level l,BlockPos p,BlockState old,boolean moved){super.onPlace(s,l,p,old,moved);if(!l.isClientSide())power(l,p);}
        private void power(Level l,BlockPos pos){var s=l.getBlockState(pos);if(s.getBlock()!=this)return;boolean powered=l.hasNeighborSignal(pos);if(s.getValue(ACTIVE)!=powered){l.setBlock(pos,s.setValue(ACTIVE,powered),Block.UPDATE_CLIENTS);EnergyNetworks.invalidate(l);}}
        @Override protected void neighborChanged(BlockState s,Level l,BlockPos pos,Block neighbor,Orientation orientation,boolean moved){super.neighborChanged(s,l,pos,neighbor,orientation,moved);if(!l.isClientSide())power(l,pos);}
        @Override protected void tick(BlockState s,ServerLevel l,BlockPos pos,RandomSource random){super.tick(s,l,pos,random);power(l,pos);}
    }
    static final class Detector extends Wire implements EntityBlock {
        Detector(Properties p,Spec spec){super(p,spec);registerDefaultState(defaultBlockState().setValue(ACTIVE,false));}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){super.createBlockStateDefinition(b);b.add(ACTIVE);}
        @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new DetectorNode(p,s);}
        @Override public boolean observesTransfers(){return true;}
        @Override public void onEnergyTransferred(Level level,BlockPos p,long amount){if(amount>0&&level.getBlockEntity(p) instanceof DetectorNode node){node.last=level.getGameTime();node.signal(true);}}
        @Override protected boolean isSignalSource(BlockState s){return true;}
        @Override protected int getSignal(BlockState s,BlockGetter level,BlockPos p,Direction d){return s.getValue(ACTIVE)?15:0;}
        @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> type){return !l.isClientSide()&&type==TYPE?(world,p,state,entity)->{
            var node=(DetectorNode)entity;if(state.getValue(ACTIVE)&&world.getGameTime()-node.last>=10)node.signal(false);
        }:null;}
    }
    public static final class DetectorNode extends BlockEntity {
        long last=Long.MIN_VALUE/2;
        public DetectorNode(BlockPos p,BlockState s){super(TYPE,p,s);}
        void signal(boolean active){if(level!=null&&getBlockState().getValue(ACTIVE)!=active){level.setBlock(worldPosition,getBlockState().setValue(ACTIVE,active),Block.UPDATE_CLIENTS);level.updateNeighborsAt(worldPosition,getBlockState().getBlock());}}
    }
}
