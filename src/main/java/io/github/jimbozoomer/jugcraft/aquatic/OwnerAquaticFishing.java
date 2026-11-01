package io.github.jimbozoomer.jugcraft.aquatic;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.advancements.predicates.entity.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.entries.NestedLootTable;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.minecraft.world.level.storage.loot.predicates.LootItemEntityPropertyCondition;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

/** Supplied fishing weights join the native pool; rolls, luck and open-water checks remain native. */
public final class OwnerAquaticFishing {
    private OwnerAquaticFishing(){}
    public static void register(){
        LootTableEvents.MODIFY.register((key,table,source,registries)->{
            if(!source.isBuiltin()||!key.equals(BuiltInLootTables.FISHING))return;
            var lookup=registries.lookupOrThrow(Registries.LOOT_TABLE);
            String[] names={"fish","junk","neptunium"};int[] weight={85,10,1},quality={-1,-2,2};
            for(int i=0;i<names.length;i++){int index=i;
                var resource=ResourceKey.create(Registries.LOOT_TABLE,Jugcraft.id("aquatic_gameplay/fishing/"+names[i]));
                lookup.get(resource).ifPresent(holder->{int[] poolIndex={0};table.modifyPools(pool->{
                    if(poolIndex[0]++!=0)return;
                    var entry=NestedLootTable.lootTableReference(holder).setWeight(weight[index]).setQuality(quality[index]);
                    if(index==2)entry.when(LootItemEntityPropertyCondition.hasProperties(LootContext.EntityTarget.THIS,EntityPredicate.Builder.entity().put(FishingHookPredicate.CODEC,FishingHookPredicate.inOpenWater(true))));
                    pool.add(entry);
                });});
            }
        });
    }
    public static ObjectArrayList<ItemStack> caught(ObjectArrayList<ItemStack> original,FishingHook hook,ItemStack rod,String tackle,int luck){
        if(!(hook.getPlayerOwner() instanceof ServerPlayer p)||original.isEmpty())return original;
        if(!(rod.getItem() instanceof OwnerAquaticGear.Rod)){OwnerFishWeights.assign(original,p.getRandom());return original;}
        var result=new ObjectArrayList<ItemStack>(original);
        if(tackle.equals("double")&&p.getRandom().nextFloat()<.1f){
            var params=new LootParams.Builder(p.level()).withParameter(LootContextParams.ORIGIN,hook.position()).withParameter(LootContextParams.TOOL,rod).withParameter(LootContextParams.THIS_ENTITY,hook).withLuck(luck+p.getLuck()).create(LootContextParamSets.FISHING);
            result.addAll(p.level().getServer().reloadableRegistries().getLootTable(BuiltInLootTables.FISHING).getRandomItems(params));
        }
        OwnerFishWeights.assign(result,p.getRandom());OwnerAquaticGear.payBait(p,rod);return result;
    }
}
