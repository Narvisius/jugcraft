package io.github.jimbozoomer.jugcraft.botany;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.guide.encyclopedia.MentorOpenPayload;
import java.util.Set;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.entries.NestedLootTable;
import net.minecraft.world.level.storage.loot.providers.number.ints.ConstantValue;

public final class BotanicalTreasures {
    private BotanicalTreasures(){}
    static void register(){
        net.fabricmc.fabric.api.loot.v3.LootTableEvents.MODIFY.register((key,table,source,registries)->{
            if(!source.isBuiltin()||!key.identifier().getNamespace().equals("minecraft"))return;
            String path=key.identifier().getPath();if(!Set.of("chests/abandoned_mineshaft","chests/desert_pyramid","chests/jungle_temple","chests/stronghold_corridor").contains(path))return;
            var injected=ResourceKey.create(Registries.LOOT_TABLE,Jugcraft.id("jugtania_inject/"+path.substring(7)));
            registries.lookupOrThrow(Registries.LOOT_TABLE).get(injected).ifPresent(holder->table.withPool(LootPool.lootPool().setRolls(Holder.direct(new ConstantValue(1))).add(NestedLootTable.lootTableReference(holder))));
        });
    }
    static int lotus(ItemStack stack){return stack.is(Jugtania.item("black_lotus"))?8000:stack.is(Jugtania.item("blacker_lotus"))?100000:0;}
    static boolean dissolve(BotanicalWorkshop pool,ItemStack stack){
        int amount=lotus(stack);if(amount<=0||pool.mana()<=0||pool.freeMana()<amount)return false;
        if(pool.fill(amount)!=amount)return false;stack.shrink(1);return true;
    }
    public static final class Overgrowth extends Item {
        public Overgrowth(Properties p){super(p);}
        @Override public InteractionResult useOn(UseOnContext context){
            if(!(context.getPlayer() instanceof ServerPlayer p))return InteractionResult.SUCCESS;
            var pos=context.getClickedPos();if(!p.level().getBlockState(pos).is(Blocks.GRASS_BLOCK))return InteractionResult.PASS;
            if(!Authority.mayChange(p.level(),p,pos))return InteractionResult.FAIL;
            if(!p.level().setBlockAndUpdate(pos,Jugtania.block("enchanted_soil").defaultBlockState()))return InteractionResult.FAIL;
            if(!p.hasInfiniteMaterials())context.getItemInHand().shrink(1);p.level().levelEvent(1505,pos.above(),0);return InteractionResult.SUCCESS;
        }
    }
    public static final class Lexicon extends Item {
        public Lexicon(Properties p){super(p.stacksTo(1));}
        @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
            if(player instanceof ServerPlayer p&&ServerPlayNetworking.canSend(p,MentorOpenPayload.TYPE))ServerPlayNetworking.send(p,new MentorOpenPayload(player.isShiftKeyDown()?"botanical_flowers":"botanical"));
            return InteractionResult.SUCCESS;
        }
    }
}
