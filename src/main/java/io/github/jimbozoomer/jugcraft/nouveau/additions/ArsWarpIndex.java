package io.github.jimbozoomer.jugcraft.nouveau.additions;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.trinket.WornTrinketItem;
import io.github.jimbozoomer.jugcraft.nouveau.*;
import java.util.Map;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/** The bound item remains the live authorization token for a remote native storage/crafting menu. */
public final class ArsWarpIndex extends WornTrinketItem {
    private final boolean stabilized;
    private ArsWarpIndex(Properties props,boolean stabilized){super(props,"ars_warp_index",Map.of());this.stabilized=stabilized;}
    public static void register(){
        for(boolean stable:new boolean[]{false,true}){
            String name=stable?"stabilized_warp_index":"warp_index";var key=ResourceKey.create(Registries.ITEM,Jugcraft.id("ars_additions_"+name));
            var item=Registry.register(BuiltInRegistries.ITEM,key,new ArsWarpIndex(new Properties().setId(key).stacksTo(1),stable));
            CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(e->e.accept(item));
        }
        // A lectern ordinarily consumes use before Item.useOn can bind it.
        net.fabricmc.fabric.api.event.player.UseBlockCallback.EVENT.register((p,l,h,hit)->p.getItemInHand(h).getItem() instanceof ArsWarpIndex index&&p.isShiftKeyDown()?index.useOn(new UseOnContext(p,h,hit)):InteractionResult.PASS);
    }
    @Override public InteractionResult useOn(UseOnContext context){
        if(!context.isSecondaryUseActive())return InteractionResult.PASS;
        if(!(context.getPlayer() instanceof ServerPlayer p))return InteractionResult.SUCCESS;
        if(!(p.level().getBlockEntity(context.getClickedPos()) instanceof StorageLectern lectern)||!lectern.mayUse(p))return InteractionResult.FAIL;
        context.getItemInHand().set(NouveauUtilities.DESTINATION,GlobalPos.of(p.level().dimension(),context.getClickedPos()));
        p.sendOverlayMessage(Component.literal("Warp Index bound. Use it to open this loaded storage network."));return InteractionResult.SUCCESS;
    }
    @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
        if(!(player instanceof ServerPlayer p))return InteractionResult.SUCCESS;
        var stack=p.getItemInHand(hand);var destination=stack.get(NouveauUtilities.DESTINATION);var lectern=resolve(p,stack);
        if(lectern==null){p.sendOverlayMessage(Component.literal("Bound lectern unavailable: it must be loaded and permitted; ordinary indexes require the same dimension."));return InteractionResult.FAIL;}
        p.openMenu(new SimpleMenuProvider((id,inventory,who)->new NouveauStorageTerminal.Menu(id,inventory,lectern,
            ()->p.getItemInHand(hand)==stack&&java.util.Objects.equals(destination,stack.get(NouveauUtilities.DESTINATION))&&resolve(p,stack)==lectern),Component.literal("Warp Index · Storage & Crafting")));
        return InteractionResult.SUCCESS;
    }
    private StorageLectern resolve(ServerPlayer p,ItemStack stack){
        if(!JugNouveau.enabled()||!p.isAlive()||p.isSpectator()||stack.getItem()!=this)return null;
        var destination=stack.get(NouveauUtilities.DESTINATION);if(destination==null||!stabilized&&!destination.dimension().equals(p.level().dimension()))return null;
        var level=p.level().getServer().getLevel(destination.dimension());
        return level!=null&&level.hasChunkAt(destination.pos())&&level.getBlockEntity(destination.pos()) instanceof StorageLectern lectern&&lectern.mayUseRemote(p)?lectern:null;
    }
}
