package io.github.jimbozoomer.jugcraft.concordance.assay;

import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import io.github.jimbozoomer.jugcraft.concordance.equivalence.Assay;
import java.util.*;
import java.util.function.BooleanSupplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

/** Two real transaction slots and 24 read-only, server-synchronized learned patterns. */
public final class AlchemyPatternMenu extends AbstractContainerMenu {
    public static final int GHOST_START=38, PAGE_SIZE=24;
    private final Player owner;
    private final BooleanSupplier access;
    private final ItemStack eye;
    private final SimpleContainer real=new SimpleContainer(2),ghost=new SimpleContainer(PAGE_SIZE);
    private final SimpleContainerData data=new SimpleContainerData(25);
    private String query="",selected="";
    private int page, batch=1;
    private ItemStack confirmation=ItemStack.EMPTY;
    private long confirmUntil;
    private boolean favorites;
    private List<String> cachedIds=List.of(),lastKnown=List.of(),lastFavorites=List.of();
    private Object lastCatalogue;
    private String lastQuery;
    private boolean lastFilter;
    private int lastPage=-1;
    public AlchemyPatternMenu(int id,Inventory inv){this(id,inv,()->true,ItemStack.EMPTY);}
    public AlchemyPatternMenu(int id,Inventory inv,BooleanSupplier access,ItemStack eye){
        super(AlchemyPatterns.MENU,id);owner=inv.player;this.access=access;this.eye=eye;
        if(!eye.isEmpty())selected=eye.getOrDefault(AlchemyPatterns.TARGET,"");
        addSlot(new Slot(real,0,31,63){@Override public boolean isActive(){return !eye();}@Override public boolean mayPlace(ItemStack s){return !eye();}});
        addSlot(new Slot(real,1,113,63){@Override public boolean isActive(){return !eye();}@Override public boolean mayPlace(ItemStack s){return false;}});
        addStandardInventorySlots(inv,181,151);
        for(int i=0;i<PAGE_SIZE;i++)addSlot(new Slot(ghost,i,205+(i%6)*22,52+(i/6)*20){
            @Override public boolean mayPlace(ItemStack s){return false;}
            @Override public boolean mayPickup(Player p){return false;}
        });
        addDataSlots(data);refresh();
    }
    public void search(String s){query=s.substring(0,Math.min(48,s.length())).toLowerCase(Locale.ROOT);page=0;broadcastChanges();}
    private ItemStack target(){var id=Identifier.tryParse(selected);return id==null?ItemStack.EMPTY:new ItemStack(BuiltInRegistries.ITEM.getValue(id));}
    public int page(){return data.get(0);} public int pages(){return Math.max(1,data.get(1));}
    public boolean eye(){return data.get(2)!=0;} public boolean favorites(){return data.get(3)!=0;}
    public long price(){return (data.get(4)&65535L)|((data.get(5)&65535L)<<16);}
    public long balance(){return (data.get(6)&65535L)|((data.get(7)&65535L)<<16);}
    public boolean canMake(){return (data.get(8)&1)!=0;} public boolean isFavorite(){return (data.get(8)&2)!=0;}
    public ItemStack selection(){return new ItemStack(BuiltInRegistries.ITEM.byId(data.get(9)));}
    private long number(int i){return (data.get(i)&65535L)|((data.get(i+1)&65535L)<<16);}
    private void put(int i,long n){data.set(i,(int)n&65535);data.set(i+1,(int)(n>>>16));}
    private void putWide(int low,int high,long n){put(low,n);put(high,n>>>32);}
    private long wide(int low,int high){return number(low)|(number(high)<<32);}
    public long gain(){return wide(11,21);} public long stars(){return number(13);}
    public int area(){return Math.clamp(data.get(20),1,5);}
    public int inputStatus(){return data.get(10);} public boolean confirming(){return data.get(15)!=0;}
    public int batch(){return data.get(16);} public long batchCost(){return wide(17,23);} public int formStatus(){return data.get(19);}
    public boolean canDissolve(){return !eye()&&inputStatus()==0;}
    private int quantity(ServerPlayer p){
        var m=AlchemicalWorkshopBlockEntity.material(target());if(m==null)return 0;
        var out=real.getItem(1);int room=out.isEmpty()?target().getMaxStackSize():ItemStack.isSameItemSameComponents(out,target())?out.getMaxStackSize()-out.getCount():0;
        int n=Math.min(room,batch==0?64:batch);
        if(batch==0)while(n>0&&Assay.form(m,n)>AlchemyPower.available(p))n--;
        return n;
    }
    private boolean allowed(ItemStack stack){var m=AlchemicalWorkshopBlockEntity.material(stack);return m!=null&&m.formable()&&(eye.isEmpty()||AlchemyPatterns.building(stack));}
    private void refresh(){
        if(!(owner instanceof ServerPlayer p))return;
        var known=AlchemyPatterns.known(p);var starred=AlchemyPatterns.favorites(p);var catalogue=Assaying.catalog();
        boolean rebuild=known!=lastKnown||starred!=lastFavorites||catalogue!=lastCatalogue||!query.equals(lastQuery)||favorites!=lastFilter;
        if(rebuild){cachedIds=known.stream().filter(id->{
            var key=Identifier.tryParse(id);if(key==null)return false;
            var stack=new ItemStack(BuiltInRegistries.ITEM.getValue(key));
            return allowed(stack)&&(!favorites||AlchemyPatterns.favorites(p).contains(id))
                    &&(id.contains(query)||stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains(query));
        }).sorted().toList();lastKnown=known;lastFavorites=starred;lastCatalogue=catalogue;lastQuery=query;lastFilter=favorites;}
        var ids=cachedIds;
        int pages=Math.max(1,(ids.size()+PAGE_SIZE-1)/PAGE_SIZE);page=Math.clamp(page,0,pages-1);
        if(rebuild||lastPage!=page)for(int i=0;i<PAGE_SIZE;i++)ghost.setItem(i,page*PAGE_SIZE+i<ids.size()?new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(ids.get(page*PAGE_SIZE+i)))):ItemStack.EMPTY);
        lastPage=page;
        var stack=target();var m=AlchemicalWorkshopBlockEntity.material(stack);
        long cost=m!=null&&allowed(stack)?Assay.form(m,1):0;
        var out=real.getItem(1);
        boolean valid=stillValid(p)&&AlchemyPatterns.known(p).contains(selected)&&cost>0;
        boolean fits=out.isEmpty()||ItemStack.isSameItemSameComponents(out,stack)&&out.getCount()<out.getMaxStackSize();
        data.set(0,page);data.set(1,pages);data.set(2,eye.isEmpty()?0:1);data.set(3,favorites?1:0);
        data.set(4,(int)cost&65535);data.set(5,(int)(cost>>>16));long balance=AlchemyPower.available(p);
        data.set(6,(int)balance&65535);data.set(7,(int)(balance>>>16));
        data.set(8,(valid&&(!eye.isEmpty()||balance>=cost&&fits)?1:0)|(AlchemyPatterns.favorites(p).contains(selected)?2:0));
        data.set(9,BuiltInRegistries.ITEM.getId(stack.getItem()));
        var input=real.getItem(0);var inputMaterial=AlchemicalWorkshopBlockEntity.material(input);
        long gain=inputMaterial==null?0:Assay.dissolve(inputMaterial,Math.min(64,input.getCount()));
        int reason=input.isEmpty()?1:inputMaterial==null?2:gain>Assay.MAX_BALANCE-Assaying.balance(p)?3:0;
        if(!ItemStack.matches(input,confirmation)||p.level().getGameTime()>confirmUntil)confirmation=ItemStack.EMPTY;
        data.set(20,eye.getOrDefault(AlchemyPatterns.AREA,3));
        data.set(10,reason);putWide(11,21,gain);put(13,balance-Assaying.balance(p));data.set(15,confirmation.isEmpty()?0:1);data.set(16,batch);
        int n=quantity(p);long total=m==null?0:Assay.form(m,n);putWide(17,23,total);
        int state=!valid?1:!fits?3:n<=0||total>balance?2:0;data.set(19,state);
        data.set(8,((valid&&(!eye.isEmpty()||state==0))?1:0)|(AlchemyPatterns.favorites(p).contains(selected)?2:0));
    }
    @Override public void broadcastChanges(){refresh();super.broadcastChanges();}
    @Override public boolean clickMenuButton(Player player,int action){
        if(player!=owner||!(player instanceof ServerPlayer p)||!stillValid(p)||action<0||action>10||!RateGate.allow(p,"alchemy_patterns",2))return false;
        refresh();
        switch(action){
            case 0->{if(canDissolve()){
                if(!confirming()){confirmation=real.getItem(0).copy();confirmUntil=p.level().getGameTime()+200;}
                else {var input=real.getItem(0);var specimen=input.copy();
                    var material=AlchemicalWorkshopBlockEntity.material(input);
                    Assaying.dissolve(p,input,material);
                    if(input.getCount()<specimen.getCount()){
                        AlchemyPatterns.learn(p,specimen);selected=BuiltInRegistries.ITEM.getKey(specimen.getItem()).toString();
                    }
                    confirmation=ItemStack.EMPTY;}
            }}
            case 1->{if(canMake()){
                if(!eye.isEmpty()){eye.set(AlchemyPatterns.TARGET,selected);p.getInventory().setChanged();}
                else {int n=quantity(p);var material=AlchemicalWorkshopBlockEntity.material(target());
                    if(n>0&&material!=null&&AlchemyPower.pay(p,Assay.form(material,n))){var out=real.getItem(1);if(out.isEmpty())real.setItem(1,target().copyWithCount(n));else out.grow(n);}}
            }}
            case 2->page--;
            case 3->page++;
            case 4->{AlchemyPatterns.favorite(p,selected);}
            case 5->{favorites=!favorites;page=0;}
            case 6->batch=1;
            case 7->batch=16;
            case 8->batch=64;
            case 9->batch=0;
            case 10->{if(!eye.isEmpty()){int size=eye.getOrDefault(AlchemyPatterns.AREA,3);eye.set(AlchemyPatterns.AREA,size>=5?1:size+2);p.getInventory().setChanged();}}
        }
        broadcastChanges();return true;
    }
    @Override public void clicked(int slot,int button,ContainerInput input,Player player){
        if(!stillValid(player))return;
        if(slot>=GHOST_START&&slot<GHOST_START+PAGE_SIZE){
            if(player instanceof ServerPlayer p&&input==ContainerInput.PICKUP&&RateGate.allow(p,"alchemy_pattern_select",1)){
                var s=ghost.getItem(slot-GHOST_START);if(!s.isEmpty()){selected=BuiltInRegistries.ITEM.getKey(s.getItem()).toString();broadcastChanges();}
            }return;
        }
        super.clicked(slot,button,input,player);
    }
    @Override public boolean stillValid(Player p){return p==owner&&(!(p instanceof ServerPlayer s)||AlchemicalGear.ready(s)&&access.getAsBoolean());}
    @Override public ItemStack quickMoveStack(Player p,int index){
        if(!stillValid(p)||index<0||index>=GHOST_START)return ItemStack.EMPTY;
        var slot=slots.get(index);if(!slot.hasItem())return ItemStack.EMPTY;var s=slot.getItem();var copy=s.copy();
        if(index<2){if(!moveItemStackTo(s,2,GHOST_START,true))return ItemStack.EMPTY;}
        else if(!moveItemStackTo(s,0,1,false))return ItemStack.EMPTY;
        if(s.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();return copy;
    }
    @Override public void removed(Player p){super.removed(p);if(p instanceof ServerPlayer)clearContainer(p,real);}
}
