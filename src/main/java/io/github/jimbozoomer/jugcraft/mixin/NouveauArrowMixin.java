package io.github.jimbozoomer.jugcraft.mixin;
import io.github.jimbozoomer.jugcraft.nouveau.*;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.phys.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(AbstractArrow.class)
public abstract class NouveauArrowMixin implements io.github.jimbozoomer.jugcraft.nouveau.NouveauArrowAccess {
    @org.spongepowered.asm.mixin.Shadow private double baseDamage;
    @Override public double jugcraft$baseDamage(){return baseDamage;}
    @Inject(method="tick",at=@At("HEAD")) private void jugcraft$arrowSetup(CallbackInfo ci){NouveauEquipment.arrowTick((AbstractArrow)(Object)this);}
    @Inject(method="onHitEntity",at=@At("HEAD")) private void jugcraft$spellArrowEntity(EntityHitResult hit,CallbackInfo ci){NouveauEquipment.arrowHit((AbstractArrow)(Object)this,hit);}
    @Inject(method="onHitBlock",at=@At("HEAD")) private void jugcraft$spellArrowBlock(BlockHitResult hit,CallbackInfo ci){NouveauEquipment.arrowHit((AbstractArrow)(Object)this,hit);}
    @Inject(method="getPierceLevel",at=@At("RETURN"),cancellable=true) private void jugcraft$glyphPiercing(CallbackInfoReturnable<Byte> cir){if(((AbstractArrow)(Object)this).getPickupItemStackOrigin().is(NouveauCatalog.item("pierce_arrow")))cir.setReturnValue((byte)Math.max(2,cir.getReturnValue()));}
}
