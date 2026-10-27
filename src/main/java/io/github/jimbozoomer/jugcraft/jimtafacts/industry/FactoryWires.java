package io.github.jimbozoomer.jugcraft.jimtafacts.industry;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.control.*;
import io.github.jimbozoomer.jugcraft.finance.ClaimTransfers;
import java.util.*;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.*;

/** Sixteen paid, removable signal conductors sharing the pipe's saved fittings. */
public final class FactoryWires {
    private FactoryWires(){}
    public static final Map<DyeColor,Item> ITEMS=new EnumMap<>(DyeColor.class);
    public static void register(){
        for(var colour:DyeColor.values()){
            var key=ResourceKey.create(Registries.ITEM,Jugcraft.id("jimta_pipe_wire_"+colour.getName()));
            ITEMS.put(colour,Registry.register(BuiltInRegistries.ITEM,key,new Item(new Item.Properties().setId(key)){
                @Override public InteractionResult useOn(UseOnContext c){
                    var fitting=TransportAttachments.at(c.getLevel(),c.getClickedPos());if(fitting==null)return InteractionResult.PASS;
                    if(!(c.getPlayer() instanceof ServerPlayer player))return InteractionResult.SUCCESS;
                    if(player.isSpectator()||!Authority.mayChange(player.level(),player,c.getClickedPos()))return InteractionResult.FAIL;
                    int bit=1<<colour.ordinal();boolean installed=(fitting.wires&bit)!=0;
                    if(player.isShiftKeyDown()){
                        if(!installed)return InteractionResult.FAIL;
                        fitting.wires&=~bit;fitting.changed();player.getInventory().placeItemBackInInventory(new ItemStack(this),net.minecraft.util.Prediction.SERVER_ONLY);
                    }else{
                        if(installed){player.sendOverlayMessage(Component.literal("Wire already fitted. Sneak-use to recover it."));return InteractionResult.SUCCESS;}
                        fitting.wires|=bit;c.getItemInHand().consume(1,player);fitting.changed();
                    }
                    for(var direction:Direction.values()){var pos=c.getClickedPos().relative(direction);if(player.level().hasChunkAt(pos)){var state=player.level().getBlockState(pos);if(state.getBlock() instanceof DataCableBlock)player.level().setBlockAndUpdate(pos,net.minecraft.world.level.block.Block.updateFromNeighbourShapes(state,player.level(),pos));}}
                    return InteractionResult.SUCCESS;
                }
                @Override public void appendHoverText(ItemStack s,TooltipContext c,net.minecraft.world.item.component.TooltipDisplay d,java.util.function.Consumer<Component> out,TooltipFlag f){out.accept(Component.literal("Install on factory pipes to carry this control channel. Sneak-use recovers it."));out.accept(Component.literal("Connect to data cables, sensors, gates and relays. Carries no power or cargo."));}
            }));
        }
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.REDSTONE_BLOCKS).register(o->ITEMS.values().forEach(o::accept));
    }
    public static int mask(BlockGetter world,BlockPos pos){var fitting=TransportAttachments.at(world,pos);return fitting==null?0:fitting.wires;}
    public static boolean connects(Level level,BlockPos from,Direction direction,int colour){
        var to=from.relative(direction);if(!level.hasChunkAt(to)||!ClaimTransfers.allowed(level,from,to))return false;
        int bit=1<<colour;if((mask(level,from)&bit)==0)return false;
        var fitting=TransportAttachments.at(level,to);
        if(fitting!=null)return (fitting.wires&bit)!=0;
        var state=level.getBlockState(to);
        return state.getBlock() instanceof DataConnectable&&(!state.hasProperty(Channels.CHANNEL)||state.getValue(Channels.CHANNEL).ordinal()==colour);
    }
}
