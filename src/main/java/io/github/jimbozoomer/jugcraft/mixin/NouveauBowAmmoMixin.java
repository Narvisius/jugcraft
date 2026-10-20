package io.github.jimbozoomer.jugcraft.mixin;
import io.github.jimbozoomer.jugcraft.nouveau.*;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(Player.class)
public abstract class NouveauBowAmmoMixin {
    @Inject(method="getProjectile",at=@At("RETURN"),cancellable=true)
    private void jugcraft$spellAmmo(ItemStack weapon,CallbackInfoReturnable<ItemStack> cir){
        if(!cir.getReturnValue().isEmpty()||!JugNouveau.enabled()||NouveauGear.CONJURED_ARROW==null)return;
        if(weapon.getItem()!=NouveauCatalog.ITEMS.get("spell_bow")&&weapon.getItem()!=NouveauCatalog.ITEMS.get("spell_crossbow"))return;
        if((Object)this instanceof ServerPlayer p&&ConcordanceProgress.currentFocus(p)<1)return;
        var arrow=new ItemStack(Items.ARROW);arrow.set(NouveauGear.CONJURED_ARROW,true);cir.setReturnValue(arrow);
    }
}
