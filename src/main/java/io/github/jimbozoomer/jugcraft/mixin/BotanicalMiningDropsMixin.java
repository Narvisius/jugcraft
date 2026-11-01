package io.github.jimbozoomer.jugcraft.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.botany.Jugtania;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Block.class)
public abstract class BotanicalMiningDropsMixin {
    @ModifyReturnValue(method="getDrops(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemInstance;)Ljava/util/List;",at=@At("RETURN"))
    private static List<ItemStack> jugcraft$filterStone(List<ItemStack> drops,BlockState state,ServerLevel level,BlockPos pos,BlockEntity blockEntity,Entity breaker,ItemInstance tool){
        if(tool instanceof ItemStack glass&&glass.is(Jugtania.item("glass_pickaxe"))&&(state.is(TagKey.create(Registries.BLOCK,net.minecraft.resources.Identifier.parse("c:glass_blocks")))||state.is(TagKey.create(Registries.BLOCK,net.minecraft.resources.Identifier.parse("c:glass_panes"))))){
            var silk=level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH);
            if(net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(silk,glass)==0){var copy=glass.copy();copy.enchant(silk,1);return Block.getDrops(state,level,pos,blockEntity,breaker,copy);}
        }
        if(!(tool instanceof ItemStack stack)||!stack.is(Jugtania.item("elementium_pickaxe"))||breaker==null)return drops;
        var disposable=TagKey.create(Registries.ITEM,Jugcraft.id("jugtania_disposable"));
        var semi=TagKey.create(Registries.ITEM,Jugcraft.id("jugtania_semi_disposable"));
        return drops.stream().filter(s->!s.is(disposable)&&(breaker.isShiftKeyDown()||!s.is(semi))).toList();
    }
}
