package io.github.jimbozoomer.jugcraft.nouveau;

import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import java.util.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import org.jspecify.annotations.Nullable;

/** Buttons select server recipe IDs; preview slots can never be taken or shift-clicked. */
public final class NouveauWorkshopMenu extends AbstractContainerMenu {
    public static final int MACHINE_SLOTS=54,PREVIEW=54,PREVIEW_COUNT=11,PLAYER=65;
    private final Player viewer;
    private final @Nullable NouveauWorkshop workshop;
    private final Container inventory;
    private final SimpleContainer preview=new SimpleContainer(PREVIEW_COUNT);
    private final ContainerData data=new SimpleContainerData(11);
    private int cursor;
    private String query="";
    private List<NouveauRecipes.Working> available(){return workshop==null?List.of():NouveauRecipes.forStation(workshop.kind()).stream().filter(r->query.isEmpty()||r.label().toLowerCase(Locale.ROOT).contains(query)||r.id().contains(query)).toList();}
    public void search(ServerPlayer p,String text){if(!stillValid(p)||!RateGate.allow(p,"nouveau_recipe_search",2))return;query=text.strip().toLowerCase(Locale.ROOT);cursor=0;shown=null;refresh();broadcastFullState();}
    private NouveauRecipes.@Nullable Working shown;
    public NouveauWorkshopMenu(int id,Inventory inv){this(id,inv,null);}
    public NouveauWorkshopMenu(int id,Inventory inv,@Nullable NouveauWorkshop workshop){
        super(NouveauWorkshops.MENU,id);this.workshop=workshop;viewer=inv.player;inventory=workshop==null?new SimpleContainer(MACHINE_SLOTS):workshop;
        for(int i=0;i<MACHINE_SLOTS;i++){
            final int slot=i;addSlot(new Slot(inventory,i,18+(i%9)*18,42+(i/9)*18){
                @Override public boolean isActive(){return storage()||slot<18;}
                @Override public boolean mayPlace(ItemStack s){return stillValid(viewer)&&(storage()||slot<9)&&inventory.canPlaceItem(slot,s);}
                @Override public boolean mayPickup(Player p){return stillValid(p)&&isActive();}
            });
        }
        for(int i=0;i<PREVIEW_COUNT;i++)addSlot(new Slot(preview,i,i<9?220+i%3*18:292,i<9?85+i/3*18:i==9?103:-1000){
            @Override public boolean mayPlace(ItemStack s){return false;}
            @Override public boolean mayPickup(Player p){return false;}
        });
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inv,9+row*9+col,18+col*18,154+row*18));
        for(int col=0;col<9;col++)addSlot(new Slot(inv,col,18+col*18,214));
        refresh();addDataSlots(data);
    }
    public int controls(){return data.get(8);}
    public int threshold(){return data.get(9);}
    public boolean inverted(){return data.get(10)>0;}
    public boolean storage(){return data.get(0)==1;}
    public int ley(){return data.get(1);}
    public int progress(){return data.get(2);}
    public boolean paused(){return data.get(3)>0;}
    public boolean locked(){return data.get(4)>0;}
    public int cursor(){return data.get(5);}
    public int recipes(){return data.get(6);}
    public boolean automatic(){return data.get(7)>0;}
    public Component details(){return preview.getItem(10).getHoverName();}
    public ItemStack result(){return preview.getItem(9);}
    private void refresh(){
        if(workshop==null)return;var recipes=available();cursor=Math.clamp(cursor,0,Math.max(0,recipes.size()-1));
        data.set(0,workshop.storage()?1:0);data.set(1,(int)Math.min(32767,workshop.ley()));data.set(2,workshop.progress());data.set(3,workshop.paused()?1:0);
        data.set(4,workshop.locked()?1:0);data.set(5,cursor);data.set(6,recipes.size());data.set(7,workshop.selected().isEmpty()?1:0);
        data.set(8,switch(workshop.kind()){case "item_detector"->1;case "timer_spell_turret"->2;case "spell_sensor"->3;default->0;});data.set(9,workshop.threshold());data.set(10,workshop.inverted()?1:0);
        String title=workshop.status();
        if(recipes.isEmpty()){for(int i=0;i<PREVIEW_COUNT;i++)preview.setItem(i,ItemStack.EMPTY);shown=null;}
        if(!recipes.isEmpty()){
            var r=recipes.get(cursor);title=r.label()+" · "+r.ley()+" Ley · "+r.experience()+" XP";
            if(shown!=r){
            for(int i=0;i<PREVIEW_COUNT;i++)preview.setItem(i,ItemStack.EMPTY);
            preview.setItem(9,NouveauRecipes.preview(r,viewer.registryAccess()));
            for(int i=0;i<Math.min(9,r.ingredients().size());i++){
                var need=r.ingredients().get(i);for(Item item:net.minecraft.core.registries.BuiltInRegistries.ITEM){
                    var example=new ItemStack(item);if(NouveauRecipes.ingredient(need,example)){preview.setItem(i,example);break;}
                }
            }shown=r;}
        }
        var label=new ItemStack(Items.PAPER);label.set(DataComponents.CUSTOM_NAME,Component.literal(title));preview.setItem(10,label);
    }
    @Override public void broadcastChanges(){refresh();super.broadcastChanges();}
    @Override public boolean stillValid(Player p){return p==viewer&&p.isAlive()&&!p.isSpectator()&&(!(p instanceof ServerPlayer)||(workshop!=null&&workshop.stillValid(p)));}
    @Override public boolean clickMenuButton(Player p,int action){
        if(!(p instanceof ServerPlayer sp)||!stillValid(p)||workshop==null||!getCarried().isEmpty()||!RateGate.allow(sp,"nouveau_workshop_menu",3))return false;
        var recipes=available();
        switch(action){
            case 0 -> cursor=Math.max(0,cursor-1);
            case 1 -> cursor=Math.min(Math.max(0,recipes.size()-1),cursor+1);
            case 2 -> {if(!recipes.isEmpty())workshop.select(recipes.get(cursor).id());}
            case 3 -> workshop.toggleLock();
            case 4 -> workshop.togglePause();
            case 5 -> workshop.select("");
            case 6 -> workshop.threshold(workshop.threshold()-1);
            case 7 -> workshop.threshold(workshop.threshold()+1);
            case 8 -> workshop.invert();
            case 9 -> workshop.filter(workshop.getItem(0));
            case 10 -> workshop.filter(ItemStack.EMPTY);
            default -> {return false;}
        }refresh();broadcastFullState();return true;
    }
    @Override public void clicked(int slot,int button,ContainerInput input,Player p){if(slot>=PREVIEW&&slot<PLAYER)return;if(stillValid(p))super.clicked(slot,button,input,p);}
    @Override public ItemStack quickMoveStack(Player p,int i){
        if(!stillValid(p)||i<0||i>=slots.size()||i>=PREVIEW&&i<PLAYER)return ItemStack.EMPTY;var slot=slots.get(i);if(!slot.hasItem()||!slot.mayPickup(p))return ItemStack.EMPTY;
        var stack=slot.getItem();var copy=stack.copy();
        if(i<MACHINE_SLOTS){if(!moveItemStackTo(stack,PLAYER,slots.size(),true))return ItemStack.EMPTY;}
        else if(!moveItemStackTo(stack,0,storage()?MACHINE_SLOTS:9,false))return ItemStack.EMPTY;
        if(stack.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();return copy;
    }
}
