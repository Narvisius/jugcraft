package io.github.jimbozoomer.jugcraft.jimtafacts.industry;

import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.control.DataCableBlock;
import io.github.jimbozoomer.jugcraft.energy.*;
import io.github.jimbozoomer.jugcraft.fluid.FluidNetworks;
import io.github.jimbozoomer.jugcraft.logistics.ItemNetworks;
import net.fabricmc.fabric.api.transfer.v1.fluid.*;
import net.fabricmc.fabric.api.transfer.v1.item.*;
import net.fabricmc.fabric.api.transfer.v1.storage.base.InsertionOnlyStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;

/** Explicit disposal endpoints, adjustable JE limiting and the source structural control conduit. */
public final class IndustrialTransportSpecial {
    private IndustrialTransportSpecial() {}
    public static void register(){
        for(String material:new String[]{"iron","diamond"})IndustrialTransport.put(material+"_power",new Limiter(IndustrialTransport.properties(material+"_power")));
        IndustrialTransport.put("wood_power",new IndustrialTransport.RoutePower(IndustrialTransport.properties("wood_power"),256));
        IndustrialTransport.put("diamond_wood_power",new IndustrialTransport.RoutePower(IndustrialTransport.properties("diamond_wood_power"),1024));
        IndustrialTransport.put("structure",new DataCableBlock(IndustrialTransport.properties("structure")));
        var items=new VoidBlock(IndustrialTransport.properties("void_item"));IndustrialTransport.put("void_item",items);
        var fluids=new VoidBlock(IndustrialTransport.properties("void_fluid"));IndustrialTransport.put("void_fluid",fluids);
        ItemStorage.SIDED.registerForBlocks((level,pos,state,be,side)->new InsertionOnlyStorage<ItemVariant>(){
            public long insert(ItemVariant variant,long amount,TransactionContext tx){StoragePreconditions.notBlankNotNegative(variant,amount);return items.enabled(level,pos)&&be!=null&&TransportPortStorage.live(be,side)?amount:0;}
        },items);
        FluidStorage.SIDED.registerForBlocks((level,pos,state,be,side)->new InsertionOnlyStorage<FluidVariant>(){
            public long insert(FluidVariant variant,long amount,TransactionContext tx){StoragePreconditions.notBlankNotNegative(variant,amount);return fluids.enabled(level,pos)&&be!=null&&TransportPortStorage.live(be,side)?amount:0;}
        },fluids);
    }
    public static final class Limiter extends IndustrialTransport.RoutePower {
        public static final IntegerProperty LIMIT=IntegerProperty.create("limit",0,6);
        private static final long[] RATES={0,128,256,512,1024,2048,4096};
        Limiter(Properties p){super(p,4096);registerDefaultState(defaultBlockState().setValue(LIMIT,6));}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){super.createBlockStateDefinition(b);b.add(LIMIT);}
        @Override public long transferRate(BlockState state){return RATES[state.getValue(LIMIT)];}
        @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){
            if(TransportAttachments.remove(level,pos,player,hit.getDirection()))return InteractionResult.SUCCESS;
            if(level instanceof ServerLevel server){
                if(player.isSpectator()||!Authority.mayChange(server,player,pos))return InteractionResult.FAIL;
                int next=(state.getValue(LIMIT)+1)%7;level.setBlock(pos,state.setValue(LIMIT,next),Block.UPDATE_ALL);EnergyNetworks.invalidate(level);
                player.sendOverlayMessage(Component.literal("JE line limit: "+RATES[next]+" per push"));
            }
            return InteractionResult.SUCCESS;
        }
    }
    public static final class VoidBlock extends Block implements net.minecraft.world.level.block.EntityBlock {
        @Override public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(BlockPos p,BlockState s){return new TransportAttachments.Node(p,s);}
        @Override protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState s,net.minecraft.world.level.BlockGetter l,BlockPos p,net.minecraft.world.phys.shapes.CollisionContext c){return TransportEndpointShape.shape(s,l,p);}


        @Override protected BlockState updateShape(BlockState state,LevelReader level,ScheduledTickAccess ticks,BlockPos pos,Direction direction,BlockPos neighborPos,BlockState neighborState,net.minecraft.util.RandomSource random){
            return TransportEndpointShape.connected(level,pos,state);
        }
        @Override protected void neighborChanged(BlockState s,Level l,BlockPos p,Block neighbor,net.minecraft.world.level.redstone.Orientation o,boolean moved){if(!l.isClientSide())TransportEndpointShape.refresh(l,p);}
        @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext c){return TransportEndpointShape.connected(c.getLevel(),c.getClickedPos(),defaultBlockState());}
        @Override protected void tick(BlockState s,ServerLevel l,BlockPos p,net.minecraft.util.RandomSource r){TransportEndpointShape.refresh(l,p);}
        @Override protected void onPlace(BlockState s,Level l,BlockPos p,BlockState old,boolean moved){if(!l.isClientSide())l.scheduleTick(p,this,1);}
        public static final BooleanProperty ENABLED=BooleanProperty.create("enabled");
        VoidBlock(Properties p){super(p);registerDefaultState(TransportEndpointShape.empty(defaultBlockState()).setValue(ENABLED,false));}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(ENABLED);TransportEndpointShape.define(b);}
        boolean enabled(Level level,BlockPos pos){return level.isLoaded(pos)&&level.getBlockState(pos).is(this)&&level.getBlockState(pos).getValue(ENABLED)&&!level.hasNeighborSignal(pos);}
        @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){
            if(TransportAttachments.remove(level,pos,player,hit.getDirection()))return InteractionResult.SUCCESS;
            if(level instanceof ServerLevel server){
                if(player.isSpectator()||!Authority.mayChange(server,player,pos))return InteractionResult.FAIL;
                boolean enabled=!state.getValue(ENABLED);level.setBlock(pos,state.setValue(ENABLED,enabled),Block.UPDATE_ALL);ItemNetworks.invalidate(level);FluidNetworks.invalidate(level);
                player.sendOverlayMessage(Component.literal(enabled?"Disposal ENABLED: delivered contents are destroyed. Redstone pauses it.":"Disposal disabled."));
            }
            return InteractionResult.SUCCESS;
        }
    }
}
