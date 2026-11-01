package io.github.jimbozoomer.jugcraft.mixin.client;

import io.github.jimbozoomer.jugcraft.enchanting.OwnerEnchanting;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.inventory.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.EnchantmentMenu;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EnchantmentScreen.class)
public abstract class OwnerEnchantingScreenMixin extends AbstractContainerScreen<EnchantmentMenu> {
    @Unique private Button jugcraft$reroll;
    protected OwnerEnchantingScreenMixin(EnchantmentMenu menu,Inventory inv,Component title){super(menu,inv,title);}
    @Inject(method="init",at=@At("TAIL"))
    private void jugcraft$button(CallbackInfo ci){
        jugcraft$reroll=addRenderableWidget(Button.builder(Component.literal("Reroll · 1 lapis + 5 XP"),button->{
            if(minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,OwnerEnchanting.REROLL_BUTTON);
        }).bounds(leftPos+5,topPos-22,166,18).build());
        jugcraft$reroll.setTooltip(Tooltip.create(Component.literal("Changes the three offers. Costs 1 lapis and 5 experience points, not levels. Items stay in this table when you close it.")));
        jugcraft$refresh();
    }
    @Inject(method="containerTick",at=@At("TAIL"))
    private void jugcraft$state(CallbackInfo ci){jugcraft$refresh();}
    @Unique private void jugcraft$refresh(){
        if(jugcraft$reroll!=null)jugcraft$reroll.active=minecraft.player!=null&&menu.getSlot(0).getItem().isEnchantable()
            &&(minecraft.player.hasInfiniteMaterials()||menu.getGoldCount()>=OwnerEnchanting.REROLL_LAPIS&&OwnerEnchanting.experience(minecraft.player)>=OwnerEnchanting.REROLL_XP);
    }
}
