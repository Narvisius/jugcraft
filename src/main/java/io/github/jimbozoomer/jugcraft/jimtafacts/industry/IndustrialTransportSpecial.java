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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;

/** Explicit disposal endpoints, adjustable JE limiting and the source structural control conduit. */
public final class IndustrialTransportSpecial {
    private IndustrialTransportSpecial() {}
    public static void register(){
        for(String material:new String[]{"iron","diamond"})IndustrialTransport.put(material+"_power",new Limiter(IndustrialTransport.properties(material+"_power")));
        IndustrialTransport.put("wood_power",new CableBlock(IndustrialTransport.properties("wood_power"),256));
        IndustrialTransport.put("diamond_wood_power",new CableBlock(IndustrialTransport.properties("diamond_wood_power"),1024));
        IndustrialTransport.put("structure",new DataCableBlock(IndustrialTransport.properties("structure")));
        var items=new VoidBlock(IndustrialTransport.properties("void_item"));IndustrialTransport.put("void_item",items);
        var fluids=new VoidBlock(IndustrialTransport.properties("void_fluid"));IndustrialTransport.put("void_fluid",fluids);
        ItemStorage.SIDED.registerForBlocks((level,pos,state,be,side)->new InsertionOnlyStorage<ItemVariant>(){
            public long insert(ItemVariant variant,long amount,TransactionContext tx){StoragePreconditions.notBlankNotNegative(variant,amount);return items.enabled(level,pos)?amount:0;}
        },items);
        FluidStorage.SIDED.registerForBlocks((level,pos,state,be,side)->new InsertionOnlyStorage<FluidVariant>(){
            public long insert(FluidVariant variant,long amount,TransactionContext tx){StoragePreconditions.notBlankNotNegative(variant,amount);return fluids.enabled(level,pos)?amount:0;}
        },fluids);
    }
    public static final class Limiter extends CableBlock {
        public static final IntegerProperty LIMIT=IntegerProperty.create("limit",0,6);
        private static final long[] RATES={0,128,256,512,1024,2048,4096};
        Limiter(Properties p){super(p,4096);registerDefaultState(defaultBlockState().setValue(LIMIT,6));}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){super.createBlockStateDefinition(b);b.add(LIMIT);}
        @Override public long transferRate(BlockState state){return RATES[state.getValue(LIMIT)];}
        @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){
            if(level instanceof ServerLevel server){
                if(player.isSpectator()||!Authority.mayChange(server,player,pos))return InteractionResult.FAIL;
                int next=(state.getValue(LIMIT)+1)%7;level.setBlock(pos,state.setValue(LIMIT,next),Block.UPDATE_ALL);EnergyNetworks.invalidate(level);
                player.sendOverlayMessage(Component.literal("JE line limit: "+RATES[next]+" per push"));
            }
            return InteractionResult.SUCCESS;
        }
    }
    public static final class VoidBlock extends Block {
        public static final BooleanProperty ENABLED=BooleanProperty.create("enabled");
        VoidBlock(Properties p){super(p);registerDefaultState(defaultBlockState().setValue(ENABLED,false));}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(ENABLED);}
        boolean enabled(Level level,BlockPos pos){return level.isLoaded(pos)&&level.getBlockState(pos).is(this)&&level.getBlockState(pos).getValue(ENABLED)&&!level.hasNeighborSignal(pos);}
        @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){
            if(level instanceof ServerLevel server){
                if(player.isSpectator()||!Authority.mayChange(server,player,pos))return InteractionResult.FAIL;
                boolean enabled=!state.getValue(ENABLED);level.setBlock(pos,state.setValue(ENABLED,enabled),Block.UPDATE_ALL);ItemNetworks.invalidate(level);FluidNetworks.invalidate(level);
                player.sendOverlayMessage(Component.literal(enabled?"Disposal ENABLED: delivered contents are destroyed. Redstone pauses it.":"Disposal disabled."));
            }
            return InteractionResult.SUCCESS;
        }
    }
}
