package io.github.jimbozoomer.jugcraft.backpack;

import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/** Paid conversion of an installed module; never replaces the backpack or its stored contents. */
public final class OwnerBackpackConversions {
    private OwnerBackpackConversions(){}
    public static boolean isConversion(ItemStack item){return item.getItem() instanceof OwnerBackpacks.UpgradeItem u&&u.name.startsWith("stack_upgrade_")&&u.name.endsWith("_conversion");}
    public static ItemStack replacement(ItemStack conversion,ItemStack current){
        if(!isConversion(conversion))return ItemStack.EMPTY;
        var name=((OwnerBackpacks.UpgradeItem)conversion.getItem()).name;
        var pair=name.substring(0,name.length()-"_conversion".length()).split("_to_");
        if(pair.length!=2||!current.is(OwnerBackpacks.ITEMS.get(pair[0])))return ItemStack.EMPTY;
        var target=OwnerBackpacks.ITEMS.get("stack_upgrade_"+pair[1]);
        return target==null?ItemStack.EMPTY:current.transmuteCopy(target,1);
    }
    public static boolean convert(ServerPlayer player,ItemStack pack,ItemStack conversion,Runnable changed){
        if(!OwnerBackpackInfinity.mayEdit(player,pack))return false;
        var modules=NonNullList.withSize(7,ItemStack.EMPTY);pack.getOrDefault(OwnerBackpacks.UPGRADES,ItemContainerContents.EMPTY).copyInto(modules);
        for(int i=0;i<modules.size();i++){
            var upgraded=replacement(conversion,modules.get(i));if(upgraded.isEmpty())continue;
            modules.set(i,upgraded);pack.set(OwnerBackpacks.UPGRADES,ItemContainerContents.fromItems(modules));conversion.shrink(1);changed.run();
            player.sendOverlayMessage(Component.literal("Installed ").append(upgraded.getHoverName()));return true;
        }
        player.sendOverlayMessage(Component.literal("This conversion needs its matching stack upgrade installed."));return false;
    }
}
