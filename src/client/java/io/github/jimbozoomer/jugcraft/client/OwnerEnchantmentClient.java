package io.github.jimbozoomer.jugcraft.client;

import com.google.gson.*;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import io.github.jimbozoomer.jugcraft.enchanting.OwnerEnchantments;

/** The supplied enchantment reference lives in item tooltips, alongside actual enchantments. */
final class OwnerEnchantmentClient {
    private OwnerEnchantmentClient() {}
    static void register(){
        Map<String,String> descriptions=new HashMap<>();
        try(var in=OwnerEnchantmentClient.class.getResourceAsStream("/jugcraft/enchantment_reference.json")){
            if(in!=null)for(var e:JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonArray()){
                var entry=e.getAsJsonObject();descriptions.put(entry.get("id").getAsString(),entry.get("short_desc").getAsString());
            }
        }catch(java.io.IOException|RuntimeException e){io.github.jimbozoomer.jugcraft.Jugcraft.LOGGER.warn("Unable to load supplied enchantment reference",e);}
        ItemTooltipCallback.EVENT.register((stack,context,flags,lines)->{
            for(var component:List.of(DataComponents.ENCHANTMENTS,DataComponents.STORED_ENCHANTMENTS)){
                var enchantments=stack.getOrDefault(component,ItemEnchantments.EMPTY);
                for(var entry:enchantments.entrySet())entry.getKey().unwrapKey().ifPresent(key->{
                    String id=key.identifier().toString();
                    if(id.startsWith("jugcraft:reforged/"))lines.add(Component.translatable("enchantment.jugcraft.reforged."+key.identifier().getPath().substring("reforged/".length())+".desc").withStyle(ChatFormatting.DARK_AQUA));
                    else if(descriptions.containsKey(id))lines.add(Component.literal(descriptions.get(id)).withStyle(ChatFormatting.DARK_GRAY));
                });
            }
        });
        // Local prediction mirrors the server movement rules; never edits equipment or awards effects.
        ClientTickEvents.END_CLIENT_TICK.register(mc->{
            var p=mc.player;if(p==null||!p.isAlive()||p.isSpectator())return;
            var v=p.getDeltaMovement();int glider=OwnerEnchantments.armor(p,"glider");
            if(glider>0&&p.isFallFlying()&&v.y<0)p.setDeltaMovement(v.x,Math.min(0,v.y+Math.min(.018,.006*glider)),v.z);
            v=p.getDeltaMovement();if(OwnerEnchantments.armor(p,"wall_grip")>0&&p.horizontalCollision&&!p.onGround()&&v.y<-.15)p.setDeltaMovement(v.x,-.15,v.z);
        });
    }
}
