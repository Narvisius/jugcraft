package io.github.jimbozoomer.jugcraft.client;

import com.google.gson.*;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.*;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.*;
import net.minecraft.tags.TagKey;

/** Resource-selected original frame atlases. Reloaded once, not parsed on every hover. */
final class OwnerTooltipStyles {
    record Style(Identifier image,int index,int start,int end,int priority,int width,int height,List<String> selectors){}
    private static List<Style> styles=List.of();
    static void register(){
        net.fabricmc.fabric.api.resource.v1.ResourceLoader.get(net.minecraft.server.packs.PackType.CLIENT_RESOURCES).registerReloadListener(
            Jugcraft.id("owner_tooltip_frames"),(net.minecraft.server.packs.resources.ResourceManagerReloadListener)resources->{
                var result=new ArrayList<Style>();
                for(var location:List.of(Identifier.fromNamespaceAndPath("legendarytooltips","frame_definitions.json"),Jugcraft.id("tooltip_frames.json"))){
                    for(var resource:resources.getResourceStack(location))try(var reader=resource.openAsReader()){
                        var root=JsonParser.parseReader(reader).getAsJsonObject();
                        if(!root.has("definitions"))continue;
                        for(var value:root.getAsJsonArray("definitions")){
                            if(result.size()>=256)break;
                            try{
                                var row=value.getAsJsonObject();var image=Identifier.parse(row.has("image")?row.get("image").getAsString():"jugcraft:textures/gui/newest/tooltip_borders.png");
                                if(image.equals(Identifier.parse("legendarytooltips:textures/gui/tooltip_borders.png")))image=Jugcraft.id("textures/gui/newest/tooltip_borders.png");
                                var selectors=new ArrayList<String>();for(var selector:row.getAsJsonArray("selectors"))if(selectors.size()<64&&selector.getAsString().length()<=256)selectors.add(selector.getAsString());
                                int width=128,height=128;var texture=resources.getResource(image).orElseThrow();
                                try(var stream=texture.open();var pixels=com.mojang.blaze3d.platform.NativeImage.read(stream)){width=pixels.getWidth();height=pixels.getHeight();}
                                int index=row.has("index")?row.get("index").getAsInt():0;
                                if(index<0||width<64||height<16||index>=width/64*(height/16))continue;
                                result.add(new Style(image,index,color(row,"startColor",0xFF996922),color(row,"endColor",0xFF5A3A1D),row.has("priority")?row.get("priority").getAsInt():0,width,height,List.copyOf(selectors)));
                            }catch(Exception invalid){Jugcraft.LOGGER.warn("Skipping invalid tooltip frame in {}",location,invalid);}
                        }
                    }catch(Exception invalid){Jugcraft.LOGGER.warn("Could not load tooltip frame definitions {}",location,invalid);}
                }
                result.sort(Comparator.comparingInt(Style::priority).reversed());styles=List.copyOf(result);
            });
    }
    static Style of(ItemStack stack){for(var style:styles)for(String selector:style.selectors())if(matches(stack,selector))return style;return null;}
    private static boolean matches(ItemStack stack,String selector){
        var id=BuiltInRegistries.ITEM.getKey(stack.getItem());
        if(selector.equals("*"))return true;
        if(selector.startsWith("@"))return id.getNamespace().equals(selector.substring(1));
        if(selector.startsWith("%"))return stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains(selector.substring(1).toLowerCase(Locale.ROOT));
        if(selector.startsWith("rarity:"))return stack.getRarity().name().equalsIgnoreCase(selector.substring(7));
        String value=selector.startsWith("#")?selector.substring(1):selector;Identifier parsed=Identifier.tryParse(value);if(parsed==null)return false;
        return selector.startsWith("#")?stack.is(TagKey.create(Registries.ITEM,parsed)):id.equals(parsed);
    }
    private static int color(JsonObject row,String key,int fallback){
        if(!row.has(key))return fallback;String value=row.get(key).getAsString();
        if(value.startsWith("#"))return (int)Long.parseLong(value.substring(1),16)|(value.length()<=7?0xFF000000:0);
        if(value.startsWith("0x")||value.startsWith("0X"))return (int)Long.parseLong(value.substring(2),16);
        var format=net.minecraft.network.chat.TextColor.parseColor(value).result().orElse(null);if(format!=null)return 0xFF000000|format.getValue();
        return (int)Long.parseLong(value);
    }
}
