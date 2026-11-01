package io.github.jimbozoomer.jugcraft.backpack;

import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** Explicit template storage. Survival duplication is off by default for the owner's server economy. */
public final class OwnerBackpackInfinity {
    private OwnerBackpackInfinity(){}
    public static boolean admin(Player player){return player instanceof ServerPlayer server&&server.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);}
    public static boolean adminPack(ItemStack pack){return OwnerBackpacks.has(pack,"infinity_upgrade");}
    public static boolean mayEdit(Player player,ItemStack pack){return !adminPack(pack)||admin(player);}
    public static boolean active(ItemStack pack){
        return !pack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getBooleanOr("PackInfinityPaused",false)
            &&(adminPack(pack)||JugcraftConfig.option("backpacks.survival_infinity")&&OwnerBackpacks.has(pack,"survival_infinity_upgrade"));
    }
    public static boolean retained(ItemStack pack,ItemStack sample){return !sample.isEmpty()&&active(pack);}
    static boolean toggle(ServerPlayer player,ItemStack pack){
        if(!mayEdit(player,pack)||!adminPack(pack)&&!OwnerBackpacks.has(pack,"survival_infinity_upgrade"))return false;
        boolean pause=!pack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getBooleanOr("PackInfinityPaused",false);
        CustomData.update(DataComponents.CUSTOM_DATA,pack,t->t.putBoolean("PackInfinityPaused",pause));OwnerBackpacks.changed(player,pack);
        player.sendOverlayMessage(net.minecraft.network.chat.Component.literal(pause?"Infinity paused: stored templates can be removed or changed.":"Infinity active if permitted by the server."));return true;
    }
}
