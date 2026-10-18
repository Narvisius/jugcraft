package io.github.jimbozoomer.jugcraft.concordance.assay;

import com.mojang.serialization.Codec;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/** Portable escrow for the existing essence ledger. No passive generation or duplicate balance. */
public final class KleinStars {
    public static final int[] CAPACITIES = {4096,16384,65536,262144,1048576,4194304};
    public static final Map<String, Item> ITEMS = new LinkedHashMap<>();
    public static DataComponentType<Integer> ESSENCE;
    public static MenuType<KleinStarMenu> MENU;
    private KleinStars() { }

    public static void register() {
        ESSENCE=Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,Jugcraft.id("star_essence"),
                DataComponentType.<Integer>builder().persistent(Codec.intRange(0,CAPACITIES[5])).networkSynchronized(ByteBufCodecs.VAR_INT).build());
        MENU=Registry.register(BuiltInRegistries.MENU,Jugcraft.id("klein_star"),new MenuType<>(KleinStarMenu::new,FeatureFlags.DEFAULT_FLAGS));
        for(int tier=0;tier<6;tier++){
            String id="klein_star_"+(tier+1);var key=ResourceKey.create(Registries.ITEM,Jugcraft.id(id));
            ITEMS.put(id,Registry.register(BuiltInRegistries.ITEM,key,new StarItem(new Item.Properties().setId(key).stacksTo(1)
                    .rarity(Rarity.RARE).component(ESSENCE,0),CAPACITIES[tier])));
        }
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(e->ITEMS.values().forEach(e::accept));
    }
    public static int capacity(ItemStack stack) { return stack.getItem() instanceof StarItem star?star.capacity:0; }
    public static int stored(ItemStack stack) { return Math.clamp(stack.getOrDefault(ESSENCE,0),0,capacity(stack)); }
    public static void setStored(ItemStack stack,int amount) { if(capacity(stack)>0)stack.set(ESSENCE,Math.clamp(amount,0,capacity(stack))); }

    private static final class StarItem extends Item {
        private final int capacity;
        StarItem(Properties properties,int capacity){super(properties);this.capacity=capacity;}
        @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
            if(player instanceof ServerPlayer server && RateGate.allow(server,"klein_star_open",4)){
                if(!AlchemicalGear.ready(server)){
                    server.sendOverlayMessage(Component.translatable("message.jugcraft.alchemy.unavailable"));return InteractionResult.FAIL;
                }
                server.openMenu(new SimpleMenuProvider((id,inv,p)->{
                    var menu=new KleinStarMenu(id,inv);
                    // The menu owns this one real stack until close, when vanilla returns it to the player.
                    menu.getSlot(0).set(player.getItemInHand(hand).split(1));
                    return menu;
                },Component.translatable("screen.jugcraft.star.title")));
            }
            return InteractionResult.SUCCESS;
        }
        @Override public boolean isBarVisible(ItemStack stack){return true;}
        @Override public int getBarWidth(ItemStack stack){return (int)(13L*stored(stack)/capacity);}
        @Override public int getBarColor(ItemStack stack){return 0x7EDCE0;}
        @Override public boolean isFoil(ItemStack stack){return stored(stack)>0||super.isFoil(stack);}
        @Override public void appendHoverText(ItemStack stack,TooltipContext context,TooltipDisplay display,Consumer<Component> text,TooltipFlag flag){
            text.accept(Component.translatable("tooltip.jugcraft.star.charge",stored(stack),capacity).withStyle(ChatFormatting.AQUA));
            text.accept(Component.translatable("tooltip.jugcraft.star.use").withStyle(ChatFormatting.GRAY));
        }
    }
}
