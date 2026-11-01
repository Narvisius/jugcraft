package io.github.jimbozoomer.jugcraft.aquatic;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;

public final class OwnerAquaticRewards {
    private OwnerAquaticRewards(){}
    private static java.util.List<String> messages=java.util.List.of();
    public static void register(){
        messages=java.util.stream.StreamSupport.stream(io.github.jimbozoomer.jugcraft.backpack.OwnerBackpacks.source("newest_aquatic_messages.json").getAsJsonArray("messages").spliterator(),false).map(com.google.gson.JsonElement::getAsString).toList();
        for(String name:new String[]{"box","lockbox","treasure_chest"})OwnerAquaculture.item(name,p->new Reward(p,name));
        OwnerAquaculture.item("message_in_a_bottle",Bottle::new);
    }
    private static class Reward extends Item {
        private final ResourceKey<LootTable> table;
        Reward(Properties p,String name){super(p);table=ResourceKey.create(Registries.LOOT_TABLE,Jugcraft.id("aquatic_box/"+name));}
        @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
            if(player instanceof ServerPlayer p){var results=p.level().getServer().reloadableRegistries().getLootTable(table).getRandomItems(new LootParams.Builder(p.level()).create(LootContextParamSets.EMPTY));
                if(results.isEmpty())return InteractionResult.FAIL;
                player.getItemInHand(hand).shrink(1);for(var stack:results)if(!p.getInventory().add(stack))p.drop(stack,false,net.minecraft.util.Prediction.SERVER_ONLY);p.getInventory().setChanged();
            }return InteractionResult.SUCCESS;
        }
    }
    private static class Bottle extends Item {
        Bottle(Properties p){super(p);}
        @Override public InteractionResult use(Level level,Player player,InteractionHand hand){if(player instanceof ServerPlayer p){if(!messages.isEmpty())p.sendSystemMessage(Component.literal(messages.get(p.getRandom().nextInt(messages.size()))));p.level().playSound(null,p.blockPosition(),OwnerAquaticSounds.sound("bottle_open"),net.minecraft.sounds.SoundSource.PLAYERS,.5f,1);player.getItemInHand(hand).shrink(1);if(!p.getInventory().add(new ItemStack(Items.GLASS_BOTTLE)))p.drop(new ItemStack(Items.GLASS_BOTTLE),false,net.minecraft.util.Prediction.SERVER_ONLY);}return InteractionResult.SUCCESS;}
    }
}
