package io.github.jimbozoomer.jugcraft.arcane;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.backpack.OwnerBackpacks;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.tags.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.entries.NestedLootTable;
import net.minecraft.world.level.storage.loot.providers.number.ints.ConstantValue;
import net.minecraft.world.phys.AABB;

/** Source treasure routes and ore/farmland yields, with original chances and exclusions. */
public final class ArcaneSources {
    private ArcaneSources(){}
    static void register(){
        var mappings=OwnerBackpacks.source("newest_forbidden.json").getAsJsonArray("loot_injections");
        net.fabricmc.fabric.api.loot.v3.LootTableEvents.MODIFY.register((key,table,source,registries)->{
            if(!source.isBuiltin())return;
            for(var entry:mappings){var d=entry.getAsJsonObject();if(!key.identifier().toString().equals(d.get("target").getAsString()))continue;
                var injected=ResourceKey.create(Registries.LOOT_TABLE,Identifier.parse(d.get("table").getAsString()));
                registries.lookupOrThrow(Registries.LOOT_TABLE).get(injected).ifPresent(holder->table.withPool(LootPool.lootPool().setRolls(Holder.direct(new ConstantValue(1))).add(NestedLootTable.lootTableReference(holder))));
            }
        });
    }
    public static List<ItemStack> yields(BlockState state,ServerLevel l,BlockPos pos,Entity actor,ItemInstance instance,List<ItemStack> original){
        var out=new ArrayList<>(original);
        if(instance instanceof ItemStack tool&&tool.is(TagKey.create(Registries.ITEM,Jugcraft.id("forbidden_blacksmith_gavel")))&&state.is(TagKey.create(Registries.BLOCK,Identifier.fromNamespaceAndPath("c","ores")))&&!state.is(blockTag("blacksmith_gavel_unaffected"))) {
            boolean silk=l.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(Enchantments.SILK_TOUCH).map(e->EnchantmentHelper.getItemEnchantmentLevel(e,tool)>0).orElse(false);
            if(!silk&&l.getRandom().nextFloat()<.3F)for(var stack:original)out.add(stack.copy());
        }
        if(state.is(BlockTags.CROPS)&&!state.is(blockTag("magical_farmland_blacklisted"))&&l.getBlockState(pos.below()).is(ForbiddenArcane.block("magical_farmland"))&&(!(state.getBlock() instanceof CropBlock crop)||crop.isMaxAge(state))){
            var snapshot=List.copyOf(out);for(var stack:snapshot)if(!stack.is(TagKey.create(Registries.ITEM,Jugcraft.id("forbidden_magical_farmland_blacklisted"))))out.add(stack.copy());
        }
        return ArcaneModifiers.drops(l,actor,instance,out);
    }
    private static TagKey<Block> blockTag(String path){return TagKey.create(Registries.BLOCK,Jugcraft.id("forbidden_"+path));}
    public static final class Stella extends Block {
        Stella(Properties p){super(p);}
        @Override public void playerDestroy(ServerLevel raw,ServerPlayer player,BlockPos pos,BlockState state,BlockEntity be,ItemStack tool){
            if(player instanceof ServerPlayer p){ServerLevel l=raw;
                boolean silk=l.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(Enchantments.SILK_TOUCH).map(e->EnchantmentHelper.getItemEnchantmentLevel(e,tool)>0).orElse(false);
                if(!silk){boolean safe=true;for(var at:BlockPos.betweenClosed(pos.offset(-8,-8,-8),pos.offset(8,8,8)))if(!l.isInWorldBounds(at)||!l.hasChunkAt(at)||!Authority.mayChange(l,p,at)){safe=false;break;}
                    if(safe){var targets=new ArrayList<LivingEntity>();l.getEntities(net.minecraft.world.level.entity.EntityTypeTest.forClass(LivingEntity.class),new AABB(pos).inflate(8),e->e.isAlive(),targets,65);if(targets.size()>=65)safe=false;else for(var target:targets)if(target!=p&&(p.isAlliedTo(target)||target instanceof TamableAnimal pet&&p.getUUID().equals((pet.getOwnerReference()==null?null:pet.getOwnerReference().getUUID()))||!io.github.jimbozoomer.jugcraft.weapons.TwoHanded.allowed(p,l,target))){safe=false;break;}}
                    if(safe)l.explode(p,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,3,Level.ExplosionInteraction.BLOCK);
                    else l.sendParticles(ParticleTypes.EXPLOSION,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,1,0,0,0,0);
                }
            }
            super.playerDestroy(raw,player,pos,state,be,tool);
        }
    }
}
