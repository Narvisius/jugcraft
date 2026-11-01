package io.github.jimbozoomer.jugcraft.backpack;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;

/** Native recipe/result menus keep ordinary material, XP, remainder and recipe-selection rules. */
final class OwnerBackpackWorkstations {
    private OwnerBackpackWorkstations(){}
    static boolean open(ServerPlayer p,ItemStack carrier,int locked,OwnerBackpackEntity placed,int action){
        String feature=switch(action){case 10->"crafting";case 11->"stonecutter";case 12->"anvil";case 13->"smithing";default->"";};
        if(feature.isEmpty()||!OwnerBackpacks.has(carrier,feature+"_upgrade"))return false;
        var guard=new Guard(p,carrier,locked,placed,feature);
        p.openMenu(new SimpleMenuProvider((id,inv,player)->{
            // Real level access is needed for recipes and returning input stacks on close.
            // A portable anvil must not wear an unrelated anvil in the world.
            AbstractContainerMenu menu=switch(action){
                case 10->new CraftingMenu(id,inv,ContainerLevelAccess.create(p.level(),p.blockPosition())){
                    @Override public boolean stillValid(Player who){return guard.valid(who);}
                    @Override public void clicked(int s,int b,ContainerInput type,Player who){if(guard.click(this,s,b,type,who))super.clicked(s,b,type,who);}
                };
                case 11->new StonecutterMenu(id,inv,ContainerLevelAccess.create(p.level(),p.blockPosition())){
                    @Override public boolean stillValid(Player who){return guard.valid(who);}
                    @Override public void clicked(int s,int b,ContainerInput type,Player who){if(guard.click(this,s,b,type,who))super.clicked(s,b,type,who);}
                };
                case 12->new AnvilMenu(id,inv,ContainerLevelAccess.NULL){
                    @Override public boolean stillValid(Player who){return guard.valid(who);}
                    @Override public void clicked(int s,int b,ContainerInput type,Player who){if(guard.click(this,s,b,type,who))super.clicked(s,b,type,who);}
                    @Override public void removed(Player who){super.removed(who);if(who instanceof ServerPlayer)clearContainer(who,inputSlots);}
                };
                default->new SmithingMenu(id,inv,ContainerLevelAccess.create(p.level(),p.blockPosition())){
                    @Override public boolean stillValid(Player who){return guard.valid(who);}
                    @Override public void clicked(int s,int b,ContainerInput type,Player who){if(guard.click(this,s,b,type,who))super.clicked(s,b,type,who);}
                };
            };
            for(int i=0;i<menu.slots.size();i++){
                var previous=menu.slots.get(i);if(previous.container!=inv||previous.getContainerSlot()!=locked)continue;
                var fixed=new Slot(inv,locked,previous.x,previous.y){@Override public boolean mayPickup(Player who){return false;}@Override public boolean mayPlace(ItemStack stack){return false;}};
                fixed.index=i;menu.slots.set(i,fixed);
            }
            ACTIVE.put(menu,guard);return menu;
        },Component.literal("Backpack "+feature)));return true;
    }
    private static final java.util.Map<AbstractContainerMenu,Guard> ACTIVE=new java.util.WeakHashMap<>();
    static void clean(){ACTIVE.entrySet().removeIf(e->e.getValue().owner.containerMenu!=e.getKey());}
    static void clear(){ACTIVE.clear();}
    static boolean uses(ServerPlayer p,ItemStack carrier){var g=ACTIVE.get(p.containerMenu);return g!=null&&g.carrier==carrier;}
    private record Guard(ServerPlayer owner,ItemStack carrier,int locked,OwnerBackpackEntity placed,String upgrade){
        boolean valid(Player p){return owner==p&&p.isAlive()&&!p.isSpectator()&&OwnerBackpacks.has(carrier,upgrade+"_upgrade")&&(placed!=null?placed.mayUse(owner):OwnerBackpacks.heldBy(p,carrier,locked));}
        boolean click(AbstractContainerMenu menu,int slot,int button,ContainerInput input,Player player){return valid(player)&&!(input==ContainerInput.SWAP&&button==locked)&&!(slot>=0&&slot<menu.slots.size()&&menu.slots.get(slot).container==player.getInventory()&&menu.slots.get(slot).getContainerSlot()==locked);}
    }
}
