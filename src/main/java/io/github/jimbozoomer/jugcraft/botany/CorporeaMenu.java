package io.github.jimbozoomer.jugcraft.botany;

import java.util.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemLore;

/** Server-owned catalogue ghosts: requests move real items into the player's inventory. */
final class CorporeaMenu extends ChestMenu {
    private final BotanicalWorkshop host;
    private final ServerPlayer owner;
    private List<CorporeaInventory.Entry> entries=List.of();
    private int page;
    private long refreshed=Long.MIN_VALUE;
    static void open(ServerPlayer p,BotanicalWorkshop host){
        if(host.permitted(p)&&!p.isSpectator())p.openMenu(new SimpleMenuProvider((id,inventory,player)->new CorporeaMenu(id,inventory,host,p),Component.literal("Corporea Index · Stored items")));
    }
    private CorporeaMenu(int id,Inventory inventory,BotanicalWorkshop host,ServerPlayer owner){
        super(MenuType.GENERIC_9x6,id,inventory,new SimpleContainer(54),6);this.host=host;this.owner=owner;refresh();
    }
    private void button(int slot,Item item,String label,String hint){var stack=new ItemStack(item);stack.set(DataComponents.CUSTOM_NAME,Component.literal(label));stack.set(DataComponents.LORE,new ItemLore(List.of(Component.literal(hint))));getContainer().setItem(slot,stack);}
    private void refresh(){
        refreshed=owner.level().getGameTime();entries=CorporeaInventory.catalog(owner,CorporeaSpark.at(owner.level(),host.getBlockPos()));page=Math.clamp(page,0,Math.max(0,(entries.size()-1)/45));getContainer().clearContent();
        for(int slot=0;slot<45&&page*45+slot<entries.size();slot++){
            var entry=entries.get(page*45+slot);var ghost=entry.variant().toStack();ghost.setCount((int)Math.min(ghost.getMaxStackSize(),entry.amount()));
            var lore=new ArrayList<Component>(ghost.getOrDefault(DataComponents.LORE,ItemLore.EMPTY).lines());lore.add(Component.literal("Stored: "+entry.amount()));lore.add(Component.literal("Left: 1 · Right or shift: a stack"));ghost.set(DataComponents.LORE,new ItemLore(lore));getContainer().setItem(slot,ghost);
        }
        button(45,Items.ARROW,"Previous page","Browse stored items.");button(49,Items.BOOK,"Page "+(page+1)+" / "+Math.max(1,(entries.size()+44)/45),entries.isEmpty()?"Attach matching sparks and exactly one master to loaded inventories.":"Requests use real inventory; full pockets leave items in storage.");button(50,Items.CLOCK,"Refresh","Recount available items.");button(53,Items.ARROW,"Next page","Browse stored items.");super.broadcastChanges();
    }
    @Override public void broadcastChanges(){if(owner!=null&&owner.level().getGameTime()-refreshed>=20&&stillValid(owner))refresh();else super.broadcastChanges();}
    @Override public boolean stillValid(Player p){return p==owner&&p.isAlive()&&!p.isSpectator()&&!host.isRemoved()&&host.getLevel()==p.level()&&p.distanceToSqr(host.getBlockPos().getX()+.5,host.getBlockPos().getY()+.5,host.getBlockPos().getZ()+.5)<=64&&host.permitted(owner);}
    @Override public void clicked(int slot,int button,ContainerInput input,Player player){
        if(!stillValid(player)||(input!=ContainerInput.PICKUP&&input!=ContainerInput.QUICK_MOVE))return;
        // Even creative clone, drag, throw and number-key packets cannot remove catalogue ghosts.
        if(slot>=0&&slot<45&&page*45+slot<entries.size()){
            var selected=entries.get(page*45+slot).variant().toStack();int amount=button==1||input==ContainerInput.QUICK_MOVE?selected.getMaxStackSize():1;
            int moved=host.corporea.request(owner,selected,amount,true);owner.sendOverlayMessage(Component.literal("Retrieved "+moved+" "+selected.getHoverName().getString()));
        }else if(slot==45)page=Math.max(0,page-1);else if(slot==53)page=Math.min(Math.max(0,(entries.size()-1)/45),page+1);
        refresh();
    }
    @Override public ItemStack quickMoveStack(Player p,int slot){return ItemStack.EMPTY;}
}
