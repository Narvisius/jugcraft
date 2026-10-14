package io.github.jimbozoomer.jugcraft.mixin;

import local.peepo.CompanionSlaughterLoot;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keep marked overflow separate from player drops so merging cannot lose its delivery provenance. */
@Mixin(ItemEntity.class)
public abstract class CompanionLootMixin {
    @Inject(method="isMergable",at=@At("HEAD"),cancellable=true)
    private void jugcraft$keepCompanionProduce(CallbackInfoReturnable<Boolean> cir){if(CompanionSlaughterLoot.marked((ItemEntity)(Object)this))cir.setReturnValue(false);}
}
