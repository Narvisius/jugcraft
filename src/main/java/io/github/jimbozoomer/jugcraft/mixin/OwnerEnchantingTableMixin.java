package io.github.jimbozoomer.jugcraft.mixin;

import io.github.jimbozoomer.jugcraft.enchanting.OwnerEnchanting;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.storage.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps existing table IDs/worlds; the vanilla book animation remains intact. */
@Mixin(EnchantingTableBlockEntity.class)
public abstract class OwnerEnchantingTableMixin implements OwnerEnchanting.Storage,WorldlyContainer {
    @Unique private OwnerEnchanting.Inventory jugcraft$inventory;
    @Override public OwnerEnchanting.Inventory jugcraft$enchantingInventory(){
        if(jugcraft$inventory==null)jugcraft$inventory=new OwnerEnchanting.Inventory((BlockEntity)(Object)this);
        return jugcraft$inventory;
    }
    @Inject(method="saveAdditional",at=@At("TAIL"))
    private void jugcraft$save(ValueOutput out,CallbackInfo ci){ContainerHelper.saveAllItems(out.child("JugcraftEnchanting"),jugcraft$enchantingInventory().items);}
    @Inject(method="loadAdditional",at=@At("TAIL"))
    private void jugcraft$load(ValueInput in,CallbackInfo ci){var inv=jugcraft$enchantingInventory();java.util.Collections.fill(inv.items,ItemStack.EMPTY);ContainerHelper.loadAllItems(in.childOrEmpty("JugcraftEnchanting"),inv.items);}
    public net.minecraft.nbt.CompoundTag getUpdateTag(HolderLookup.Provider lookup){return ((BlockEntity)(Object)this).saveWithoutMetadata(lookup);}
    public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create((BlockEntity)(Object)this);}
    @Override public int getContainerSize(){return 2;}
    @Override public boolean isEmpty(){return jugcraft$enchantingInventory().isEmpty();}
    @Override public ItemStack getItem(int slot){return jugcraft$enchantingInventory().getItem(slot);}
    @Override public ItemStack removeItem(int slot,int amount){return jugcraft$enchantingInventory().removeItem(slot,amount);}
    @Override public ItemStack removeItemNoUpdate(int slot){return jugcraft$enchantingInventory().removeItemNoUpdate(slot);}
    @Override public void setItem(int slot,ItemStack stack){jugcraft$enchantingInventory().setItem(slot,stack);}
    // BlockEntity supplies setChanged; inventory writes mark that entity dirty.
    @Override public boolean stillValid(Player player){return jugcraft$enchantingInventory().stillValid(player);}
    @Override public void clearContent(){jugcraft$enchantingInventory().clearContent();}
    @Override public boolean canPlaceItem(int slot,ItemStack stack){return jugcraft$enchantingInventory().canPlaceItem(slot,stack);}
    @Override public int[] getSlotsForFace(Direction face){return face==Direction.UP?new int[]{0}:face==Direction.DOWN?new int[]{0}:new int[]{1};}
    @Override public boolean canPlaceItemThroughFace(int slot,ItemStack stack,Direction face){return face!=Direction.DOWN&&canPlaceItem(slot,stack);}
    @Override public boolean canTakeItemThroughFace(int slot,ItemStack stack,Direction face){return face==Direction.DOWN&&slot==0&&!stack.isEnchantable();}
}
