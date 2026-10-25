package io.github.jimbozoomer.jugcraft.jimtafacts.mixin;
import io.github.jimbozoomer.jugcraft.jimtafacts.JimtaAbilities;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(Block.class)
public abstract class JimtaSmeltingMixin {
    @ModifyReturnValue(method="getDrops(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemInstance;)Ljava/util/List;",at=@At("RETURN"))
    private static List<ItemStack> jimta$smelt(List<ItemStack> drops,BlockState state,ServerLevel level,BlockPos pos,BlockEntity blockEntity,Entity breaker,ItemInstance tool){
        if(!(breaker instanceof ServerPlayer p)||!JimtaAbilities.wearing(p,"pickaxe_heater"))return drops;
        List<ItemStack> cooked=new ArrayList<>();
        for(ItemStack stack:drops){
            var input=new SingleRecipeInput(stack);var recipe=level.getServer().getRecipeManager().getRecipeFor(RecipeType.SMELTING,input,level);
            if(recipe.isEmpty()){cooked.add(stack);continue;}
            ItemStack output=recipe.get().value().assemble(input);if(output.isEmpty()){cooked.add(stack);continue;}
            long count=(long)output.getCount()*stack.getCount();if(count>4096){cooked.add(stack);continue;}
            while(count>0){int take=(int)Math.min(count,output.getMaxStackSize());cooked.add(output.copyWithCount(take));count-=take;}
        }
        return cooked;
    }
}
