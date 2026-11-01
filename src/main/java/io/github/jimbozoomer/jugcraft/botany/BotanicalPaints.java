package io.github.jimbozoomer.jugcraft.botany;

import java.util.*;
import java.util.function.Function;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.*;

/** Explicit paint compatibility. A family never converts an arbitrary similarly named machine. */
public final class BotanicalPaints {
    private static final Map<Block,Function<DyeColor,Block>> EXTRA=new IdentityHashMap<>();
    private static final Set<String> VANILLA=Set.of("wool","carpet","terracotta","glazed_terracotta","concrete","concrete_powder","stained_glass","stained_glass_pane","candle","candle_cake");
    private BotanicalPaints(){}
    public static void register(Block source,Function<DyeColor,Block> family){Objects.requireNonNull(source);Objects.requireNonNull(family);EXTRA.put(source,family);}
    public static Block target(Block source,DyeColor color){
        var custom=EXTRA.get(source);if(custom!=null)return custom.apply(color);
        var id=BuiltInRegistries.BLOCK.getKey(source);if(!id.getNamespace().equals("minecraft"))return null;
        String name=id.getPath(),stem=null;
        if(source==Blocks.GLASS)stem="stained_glass";
        else if(source==Blocks.GLASS_PANE)stem="stained_glass_pane";
        else if(source==Blocks.TERRACOTTA)stem="terracotta";
        else if(source==Blocks.CANDLE)stem="candle";
        else if(source==Blocks.CANDLE_CAKE)stem="candle_cake";
        else for(var dye:DyeColor.values())if(name.startsWith(dye.getName()+"_")){stem=name.substring(dye.getName().length()+1);break;}
        if(stem==null||!VANILLA.contains(stem))return null;
        return BuiltInRegistries.BLOCK.getOptional(Identifier.withDefaultNamespace(color.getName()+"_"+stem)).orElse(null);
    }
}
