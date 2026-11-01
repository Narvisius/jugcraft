package io.github.jimbozoomer.jugcraft.backpack;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.SimpleMenuProvider;

/** Client requests an opening, never chooses an arbitrary inventory or supplies a carrier. */
public record OwnerBackpackOpen() implements CustomPacketPayload {
    public static final Type<OwnerBackpackOpen> TYPE=new Type<>(Jugcraft.id("open_owner_backpack"));
    public static final StreamCodec<RegistryFriendlyByteBuf,OwnerBackpackOpen> CODEC=StreamCodec.unit(new OwnerBackpackOpen());
    public Type<OwnerBackpackOpen> type(){return TYPE;}
    static void register(){
        PayloadTypeRegistry.serverboundPlay().register(TYPE,CODEC);
        ServerPlayNetworking.registerGlobalReceiver(TYPE,(payload,context)->{
            var player=context.player();if(!player.isAlive()||player.isSpectator()||player.containerMenu!=player.inventoryMenu||!RateGate.allow(player,"backpack_open",5))return;
            var worn=OwnerBackpacks.worn(player);
            if(worn!=null){var pack=worn.get();player.openMenu(new SimpleMenuProvider((id,inv,p)->new OwnerBackpackMenu(id,inv,pack,-2,null),pack.getHoverName()));return;}
            for(int i=0;i<player.getInventory().getContainerSize();i++){var pack=player.getInventory().getItem(i);if(OwnerBackpacks.tier(pack)<0)continue;int slot=i;player.openMenu(new SimpleMenuProvider((id,inv,p)->new OwnerBackpackMenu(id,inv,pack,slot,null),pack.getHoverName()));return;}
        });
    }
}
