package io.github.jimbozoomer.jugcraft.concordance.assay;

import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import io.github.jimbozoomer.jugcraft.concordance.equivalence.Assay;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

/** Read-only, server-authored catalogue: display stacks can never be picked up or returned to inventory. */
public final class AlchemyStorageCatalogueMenu extends AbstractContainerMenu {
    private final Player owner;
    private final SimpleContainer samples=new SimpleContainer(36);
    private final SimpleContainerData data=new SimpleContainerData(8);
    private int page,selected,lastPage=-1,lastSelected=-1;
    private io.github.jimbozoomer.jugcraft.concordance.equivalence.EquivalenceCatalog lastCatalogue;
    private List<String> catalogueItems=List.of();
    public AlchemyStorageCatalogueMenu(int id,Inventory inventory){
        super(AlchemyStorage.CATALOGUE_MENU,id);owner=inventory.player;
        for(int row=0;row<4;row++)for(int col=0;col<9;col++)addSlot(new Slot(samples,row*9+col,29+18*col,38+18*row){
            @Override public boolean mayPlace(ItemStack stack){return false;}
            @Override public boolean mayPickup(Player player){return false;}
        });addDataSlots(data);refresh();
    }
    private void put(int offset,long value){data.set(offset,(int)value&0xffff);data.set(offset+1,(int)(value>>>16));}
    private long number(int offset){return (data.get(offset)&0xffffL)|((data.get(offset+1)&0xffffL)<<16);}
    public int source(){return data.get(7);}
    public long dissolve(){return number(3);}public long form(){return number(5);}
    public int page(){return data.get(0);}public int pages(){return Math.max(1,data.get(1));}public int selected(){return data.get(2);}
    private void refresh(){
        if(!(owner instanceof ServerPlayer))return;
        var catalogue=Assaying.catalog();
        boolean reloaded=catalogue!=lastCatalogue;
        if(reloaded){lastCatalogue=catalogue;catalogueItems=catalogue.materials().keySet().stream().sorted().toList();}
        int pages=Math.max(1,(catalogueItems.size()+35)/36);page=Math.clamp(page,0,pages-1);selected=Math.clamp(selected,0,35);
        if(!reloaded&&page==lastPage&&selected==lastSelected)return;
        if(reloaded||page!=lastPage)for(int i=0;i<36;i++)samples.setItem(i,page*36+i<catalogueItems.size()?new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(catalogueItems.get(page*36+i)))):ItemStack.EMPTY);
        lastPage=page;lastSelected=selected;
        var material=AlchemicalWorkshopBlockEntity.material(samples.getItem(selected));
        put(3,material!=null&&material.dissolvable()?Assay.dissolve(material,1):0);
        put(5,material!=null&&material.formable()?Assay.form(material,1):0);
        String origin=material==null?"":AlchemyValues.source(material.item());data.set(7,origin.startsWith("reference")?0:origin.startsWith("recipe")?1:2);
        data.set(0,page);data.set(1,pages);data.set(2,selected);
    }
    @Override public boolean clickMenuButton(Player player,int action){
        if(!(player instanceof ServerPlayer p)||!stillValid(player)||action<0||action>1||!RateGate.allow(p,"alchemy_catalogue",2))return false;
        page+=action==0?-1:1;refresh();broadcastChanges();return true;
    }
    @Override public void clicked(int slot,int button,ContainerInput input,Player player){
        if(player instanceof ServerPlayer p&&stillValid(player)&&slot>=0&&slot<36&&RateGate.allow(p,"alchemy_catalogue_select",2)){selected=slot;refresh();broadcastChanges();}
    }
    @Override public boolean stillValid(Player player){return player==owner&&(!(player instanceof ServerPlayer p)||AlchemicalGear.ready(p)
            &&(p.getMainHandItem().is(AlchemyStorage.ITEMS.get("alchemy_tome"))||p.getOffhandItem().is(AlchemyStorage.ITEMS.get("alchemy_tome"))));}
    @Override public ItemStack quickMoveStack(Player player,int index){return ItemStack.EMPTY;}
    @Override public void broadcastChanges(){refresh();super.broadcastChanges();}
}
