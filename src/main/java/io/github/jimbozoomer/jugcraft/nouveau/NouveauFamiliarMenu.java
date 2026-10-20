package io.github.jimbozoomer.jugcraft.nouveau;

import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

/** The client sends a bounded species button, never a creature id or an ownership claim. */
public final class NouveauFamiliarMenu extends AbstractContainerMenu {
    private final Inventory inventory;private final ContainerData data=new SimpleContainerData(2);
    public NouveauFamiliarMenu(int id,Inventory inventory){super(NouveauFamiliars.MENU,id);this.inventory=inventory;addDataSlots(data);refresh();}
    private void refresh(){if(!(inventory.player instanceof ServerPlayer p))return;var known=p.getAttachedOrElse(NouveauFamiliars.KNOWN,List.of());int flags=0;for(int i=0;i<6;i++)if(known.contains(NouveauFamiliars.SPECIES.get(i)))flags|=1<<i;data.set(0,flags);data.set(1,NouveauFamiliars.SPECIES.indexOf(NouveauFamiliars.active(p))+1);}
    public boolean known(int index){return (data.get(0)&1<<index)!=0;}
    public int active(){return data.get(1)-1;}
    @Override public void broadcastChanges(){refresh();super.broadcastChanges();}
    @Override public boolean stillValid(Player p){return p==inventory.player&&p.isAlive()&&!p.isSpectator()&&JugNouveau.enabled();}
    @Override public ItemStack quickMoveStack(Player p,int slot){return ItemStack.EMPTY;}
    @Override public boolean clickMenuButton(Player p,int button){if(!(p instanceof ServerPlayer server)||!stillValid(p)||button<0||button>6||button<6&&!known(button))return false;NouveauFamiliars.choose(server,button==6?"":NouveauFamiliars.SPECIES.get(button));refresh();return true;}
}
