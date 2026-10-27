package io.github.jimbozoomer.jugcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks.JimtaEncounterCreatures;
import io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks.JimtaSpellbooks;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Source feeding consumes only food from the animal killed by this spider, never nearby player drops. */
@Mixin(LivingEntity.class)
public abstract class JimtaSpiderPreyLootMixin implements io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks.JimtaSpiderMealAccess {
    @Override public boolean jugcraft$isSpiderMeal(){return jugcraft$spiderMeal;}
    @Unique private boolean jugcraft$spiderMeal;
    @WrapMethod(method="dropAllDeathLoot")
    private void jugcraft$preyDeath(ServerLevel level,DamageSource source,Operation<Void> original){
        boolean previous=jugcraft$spiderMeal;
        jugcraft$spiderMeal=JimtaSpellbooks.enabled()&&(Object)this instanceof Animal
                &&source.getEntity() instanceof JimtaEncounterCreatures.IceSpider;
        try {
            if(jugcraft$spiderMeal){var prey=(LivingEntity)(Object)this;((JimtaEncounterCreatures.IceSpider)source.getEntity()).addSatiation((int)Math.ceil(prey.getMaxHealth()));}
            original.call(level,source);
        } finally {jugcraft$spiderMeal=previous;}
    }
    @Inject(method="createItemStackToDrop",at=@At("HEAD"),cancellable=true)
    private void jugcraft$consumeFood(ItemStack stack,boolean randomly,boolean thrown,CallbackInfoReturnable<ItemEntity> cir){
        if(jugcraft$spiderMeal&&stack.has(DataComponents.FOOD))cir.setReturnValue(null);
    }
}
