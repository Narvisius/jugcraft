package io.github.jimbozoomer.jugcraft.nouveau;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
/** Choose an existing inventory dose. Preview slots are never a source of real items. */
public final class NouveauPotionMenu extends AbstractContainerMenu {
    public static MenuType<NouveauPotionMenu> TYPE;
    private final Inventory inventory;private final ItemStack cannon;private final InteractionHand hand;private final boolean crown;
    private final SimpleContainer previews=new SimpleContainer(36);private final ContainerData data=new SimpleContainerData(2);
    public static void register(){TYPE=Registry.register(BuiltInRegistries.MENU,Jugcraft.id("nouveau_potion_selector"),new MenuType<>(NouveauPotionMenu::new,FeatureFlags.DEFAULT_FLAGS));}
    public NouveauPotionMenu(int id,Inventory inv){this(id,inv,InteractionHand.MAIN_HAND);}
    public NouveauPotionMenu(int id,Inventory inv,InteractionHand hand){this(id,inv,hand,false);}
    public NouveauPotionMenu(int id,Inventory inv,InteractionHand hand,boolean crown){super(TYPE,id);this.crown=crown;inventory=inv;this.hand=hand;cannon=inv.player.getItemInHand(hand);
        for(int i=0;i<36;i++)addSlot(new Slot(previews,i,18+i%9*24,42+i/9*24){public boolean mayPickup(Player p){return false;}public boolean mayPlace(ItemStack s){return false;}});addDataSlots(data);refresh();}
    private void refresh(){if(!(inventory.player instanceof ServerPlayer))return;for(int i=0;i<36;i++){var stack=inventory.getItem(i);previews.setItem(i,NouveauPotions.ammo(stack)?stack.copy():ItemStack.EMPTY);}data.set(0,cannon.getOrDefault(NouveauPotions.SELECTED,0));data.set(1,crown?1:0);}
    public boolean crown(){return data.get(1)>0;}
    public int selected(){return data.get(0);}
    public boolean stillValid(Player p){return p==inventory.player&&p.isAlive()&&!p.isSpectator()&&(!(p instanceof ServerPlayer)||(crown?NouveauGear.worn(p,"alchemists_crown"):p.getItemInHand(hand)==cannon)&&JugNouveau.enabled());}
    public ItemStack quickMoveStack(Player p,int i){return ItemStack.EMPTY;}
    public void broadcastChanges(){refresh();super.broadcastChanges();}
    @Override public void clicked(int slot,int button,ContainerInput type,Player p){if(p instanceof ServerPlayer&&stillValid(p)&&slot>=0&&slot<36&&NouveauPotions.ammo(inventory.getItem(slot))){if(crown){var stack=inventory.getItem(slot);var contents=stack.get(net.minecraft.core.component.DataComponents.POTION_CONTENTS);if(contents==null)return;if(stack.getItem() instanceof NouveauPotions.Flask){if(!NouveauPotions.consumeDose(stack))return;}else{stack.shrink(1);p.getInventory().placeItemBackInInventory(new ItemStack(net.minecraft.world.item.Items.GLASS_BOTTLE),net.minecraft.util.Prediction.SERVER_ONLY);}NouveauPotions.apply((ServerPlayer)p,contents,p,1);}else cannon.set(NouveauPotions.SELECTED,slot);((ServerPlayer)p).closeContainer();}}
}
