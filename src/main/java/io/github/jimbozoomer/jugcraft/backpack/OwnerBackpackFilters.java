package io.github.jimbozoomer.jugcraft.backpack;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.*;

/** Each installed module owns its filters; matching supports exact components for potion/tool variants. */
public final class OwnerBackpackFilters {
    private OwnerBackpackFilters(){}
    public static ItemStack module(ItemStack pack,String feature){
        return pack.getOrDefault(OwnerBackpacks.UPGRADES,ItemContainerContents.EMPTY).nonEmptyItemCopyStream().filter(s->s.getItem() instanceof OwnerBackpacks.UpgradeItem u&&(u.name.equals(feature+"_upgrade")||u.name.equals("advanced_"+feature+"_upgrade"))).findFirst().orElse(ItemStack.EMPTY);
    }
    public static boolean configurable(ItemStack item){
        if(!(item.getItem() instanceof OwnerBackpacks.UpgradeItem u))return false;
        return java.util.Set.of("pickup","magnet","filter","feeding","refill","deposit","restock","tool_swapper","void","compacting","alchemy").contains(u.name.replace("advanced_","").replace("_upgrade",""));
    }
    public static int slots(ItemStack module){return configurable(module)?((OwnerBackpacks.UpgradeItem)module.getItem()).name.startsWith("advanced_")?16:9:0;}
    public static int flags(ItemStack module){return module.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getIntOr("PackFilterFlags",0);}
    public static ItemContainerContents filters(ItemStack pack,ItemStack module){return module.getOrDefault(OwnerBackpacks.FILTERS,pack.getOrDefault(OwnerBackpacks.FILTERS,ItemContainerContents.EMPTY));}
    public static boolean hasSamples(ItemStack pack,String feature){return filters(pack,module(pack,feature)).nonEmptyItemCopyStream().findAny().isPresent();}
    public static boolean matches(ItemStack pack,ItemStack stack,String feature){return matches(pack,stack,feature,0);}
    /** Flow 1 is pipe input, 2 is pipe output; zero is the module's own operation. */
    public static boolean matches(ItemStack pack,ItemStack stack,String feature,int flow){
        var module=module(pack,feature);if(module.isEmpty())return true;
        int flags=flags(module),direction=(flags>>>2)&3;if(flow!=0&&direction!=0&&direction!=flow)return true;
        var samples=filters(pack,module).nonEmptyItemCopyStream().toList();if(samples.isEmpty())return true;
        boolean matched=samples.stream().anyMatch(s->(flags&2)!=0?ItemStack.isSameItemSameComponents(s,stack):s.is(stack.getItem()));
        return (flags&1)!=0?!matched:matched;
    }
}
