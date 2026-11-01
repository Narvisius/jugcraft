package io.github.jimbozoomer.jugcraft.botany;

import com.mojang.serialization.*;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.*;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

/** One-death protection, captured after vanilla vanishing rules and before grave storage. */
public final class BotanicalIvy {
    public static DataComponentType<Boolean> KEEP;
    private static AttachmentType<List<ItemStack>> HELD;
    static void register(){
        KEEP=Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,Jugcraft.id("botanical_resolute_ivy"),DataComponentType.<Boolean>builder().persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).build());
        HELD=AttachmentRegistry.<List<ItemStack>>builder().persistent(ItemStack.CODEC.listOf()).copyOnDeath().buildAndRegister(Jugcraft.id("botanical_ivy_kept"));
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,Jugcraft.id("botanical_resolute_ivy"),Binding.SERIALIZER);
        ServerTickEvents.END_SERVER_TICK.register(server->{for(var p:server.getPlayerList().getPlayers()){
            if(!p.isAlive()||p.isSpectator())continue;var held=p.getAttached(HELD);if(held==null||held.isEmpty())continue;
            var remaining=new ArrayList<ItemStack>();for(var saved:held){var item=saved.copy();p.getInventory().add(item);if(!item.isEmpty())remaining.add(item);}
            if(remaining.isEmpty())p.removeAttached(HELD);else p.setAttached(HELD,List.copyOf(remaining));
        }});
    }
    public static boolean capture(ServerPlayer player,ItemStack item){
        if(KEEP==null||!item.getOrDefault(KEEP,false))return false;
        // A protected boss arena / keep-inventory death must not spend the one-use binding.
        if(!io.github.jimbozoomer.jugcraft.grave.DeathGraves.eligible(player))return false;
        var saved=item.copy();saved.remove(KEEP);var list=new ArrayList<>(player.getAttachedOrElse(HELD,List.of()));list.add(saved);player.setAttached(HELD,List.copyOf(list));return true;
    }
    public static final class Binding extends CustomRecipe {
        private static final Binding INSTANCE=new Binding();public static final RecipeSerializer<Binding> SERIALIZER=new RecipeSerializer<>(MapCodec.unit(INSTANCE),StreamCodec.<RegistryFriendlyByteBuf,Binding>unit(INSTANCE));
        private ItemStack target(CraftingInput in){boolean ivy=false;ItemStack target=ItemStack.EMPTY;for(int i=0;i<in.size();i++){
            var item=in.getItem(i);if(item.isEmpty())continue;if(item.is(Jugtania.item("keep_ivy"))&&!ivy){ivy=true;continue;}
            if(!target.isEmpty()||item.is(Jugtania.item("keep_ivy"))||item.getOrDefault(KEEP,false)||item.getMaxStackSize()!=1)return ItemStack.EMPTY;target=item;
        }return ivy?target:ItemStack.EMPTY;}
        @Override public boolean matches(CraftingInput in,Level level){return !target(in).isEmpty();}
        @Override public ItemStack assemble(CraftingInput in){var target=target(in);if(target.isEmpty())return ItemStack.EMPTY;var result=target.copyWithCount(1);result.set(KEEP,true);return result;}
        @Override public RecipeSerializer<Binding> getSerializer(){return SERIALIZER;}
    }
}
