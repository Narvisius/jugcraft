package io.github.jimbozoomer.jugcraft.mixin;

import io.github.jimbozoomer.jugcraft.enchanting.OwnerEnchanting;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(EnchantmentMenu.class)
public abstract class OwnerEnchantingMenuMixin extends AbstractContainerMenu {
    @Shadow @Final @Mutable private Container enchantSlots;
    @Shadow @Final private ContainerLevelAccess access;
    @Shadow @Final private DataSlot enchantmentSeed;
    @Unique private OwnerEnchanting.Inventory jugcraft$persistent;
    @Unique private boolean jugcraft$publicTable;
    protected OwnerEnchantingMenuMixin(MenuType<?> type,int id){super(type,id);}
    @Inject(method="<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V",at=@At("TAIL"))
    private void jugcraft$attach(int id,Inventory inventory,ContainerLevelAccess access,CallbackInfo ci){
        access.execute((level,pos)->{
            if(io.github.jimbozoomer.jugcraft.town.TownProtection.denies(inventory.player,level,pos)){
                jugcraft$publicTable=io.github.jimbozoomer.jugcraft.town.TownProtection.allowsPublicUse(inventory.player,level,pos);
                return; // Keep vanilla per-viewer slots; never expose stored town items.
            }
            if(level.getBlockEntity(pos) instanceof OwnerEnchanting.Storage table){
                jugcraft$persistent=table.jugcraft$enchantingInventory();enchantSlots=jugcraft$persistent;
                ((OwnerEnchantingSlotAccess)slots.get(0)).jugcraft$setContainer(enchantSlots);
                ((OwnerEnchantingSlotAccess)slots.get(1)).jugcraft$setContainer(enchantSlots);
                jugcraft$persistent.watch((EnchantmentMenu)(Object)this);
            }
        });
    }
    @Inject(method="removed",at=@At("HEAD"),cancellable=true)
    private void jugcraft$keepContents(Player player,CallbackInfo ci){
        if(jugcraft$persistent!=null){jugcraft$persistent.unwatch((EnchantmentMenu)(Object)this);super.removed(player);ci.cancel();}
    }
    @Inject(method="stillValid",at=@At("RETURN"),cancellable=true)
    private void jugcraft$permissions(Player player,CallbackInfoReturnable<Boolean> cir){
        if(jugcraft$persistent!=null)cir.setReturnValue(cir.getReturnValue()&&jugcraft$persistent.stillValid(player));
        else if(jugcraft$publicTable)cir.setReturnValue(cir.getReturnValue()&&access.evaluate((level,pos)->io.github.jimbozoomer.jugcraft.town.TownProtection.allowsPublicUse(player,level,pos),false));
    }
    @Inject(method="clickMenuButton",at=@At("HEAD"),cancellable=true)
    private void jugcraft$reroll(Player player,int button,CallbackInfoReturnable<Boolean> cir){
        if(jugcraft$persistent!=null&&!jugcraft$persistent.stillValid(player)){cir.setReturnValue(false);return;}
        if(button!=OwnerEnchanting.REROLL_BUTTON)return;
        cir.setReturnValue(false);
        if(player.level().isClientSide()||(jugcraft$persistent==null&&!jugcraft$publicTable)||!((EnchantmentMenu)(Object)this).stillValid(player)||!enchantSlots.getItem(0).isEnchantable())return;
        var catalyst=enchantSlots.getItem(1);
        if(!player.hasInfiniteMaterials()&&(!catalyst.is(Items.LAPIS_LAZULI)||catalyst.getCount()<OwnerEnchanting.REROLL_LAPIS||OwnerEnchanting.experience(player)<OwnerEnchanting.REROLL_XP))return;
        if(!player.hasInfiniteMaterials()){catalyst.shrink(OwnerEnchanting.REROLL_LAPIS);player.giveExperiencePoints(-OwnerEnchanting.REROLL_XP);}
        player.onEnchantmentPerformed(enchantSlots.getItem(0),0);enchantmentSeed.set(player.getEnchantmentSeed());enchantSlots.setChanged();
        cir.setReturnValue(true);
    }
    @ModifyArg(method="lambda$slotsChanged$0",at=@At(value="INVOKE",target="Lnet/minecraft/world/item/enchantment/EnchantmentHelper;getEnchantmentCost(Lnet/minecraft/util/RandomSource;IILnet/minecraft/world/item/ItemStack;)I"),index=2)
    private int jugcraft$bookPower(int original){return access.evaluate((level,pos)->OwnerEnchanting.bookshelfPower(level,pos),original);}
}
