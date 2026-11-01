package io.github.jimbozoomer.jugcraft.backpack;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/** Fixed 45-slot page, seven upgrade sockets and ghost filters; no text commands or hidden inventory rewrites. */
public final class OwnerBackpackMenu extends AbstractContainerMenu {
    public static final int PAGE_SIZE=45,UPGRADES=7,FILTERS=16,PLAYER_START=68;
    private final Player owner;
    private final ItemStack carrier;
    private final OwnerBackpackEntity placed;
    private final Container inventory;
    private final SimpleContainerData data=new SimpleContainerData(8+PAGE_SIZE*2);
    private boolean loading=true,handlingClick;
    private java.util.List<net.minecraft.world.item.component.CustomData> sentCreatures;
    private java.util.List<net.minecraft.world.item.component.CustomData> previewCreatures=java.util.List.of();
    public java.util.List<net.minecraft.world.item.component.CustomData> previewCreatures(){return previewCreatures;}
    public void acceptPreviews(java.util.List<net.minecraft.world.item.component.CustomData> value){
        if(!(owner instanceof ServerPlayer))previewCreatures=java.util.List.copyOf(value);
    }
    private final SimpleContainer view=new SimpleContainer(PAGE_SIZE);
    private final ItemStack[] baseline=new ItemStack[PAGE_SIZE];
    private final SimpleContainer upgrades,filters;
    public OwnerBackpackMenu(int id,Inventory inv){this(id,inv,ItemStack.EMPTY,-1,null);}
    public OwnerBackpackMenu(int id,Inventory inv,ItemStack carrier,int locked,OwnerBackpackEntity placed){
        super(OwnerBackpacks.MENU,id);owner=inv.player;this.carrier=carrier;this.placed=placed;
        int tier=OwnerBackpacks.tier(carrier);data.set(0,locked);data.set(1,Math.max(0,tier));data.set(3,tier<0?27:OwnerBackpacks.CAPACITY[tier]);data.set(4,tier<0?1:OwnerBackpacks.UPGRADE_SLOTS[tier]);
        inventory=placed!=null?placed.inventory():tier>=0?new OwnerBackpackInventory(carrier,this::changed):new SimpleContainer(120);
        upgrades=new SimpleContainer(UPGRADES){@Override public void setChanged(){super.setChanged();saveUpgrades();}};
        filters=new SimpleContainer(FILTERS){@Override public void setChanged(){super.setChanged();saveFilters();}};
        if(tier>=0){load(upgrades,carrier.getOrDefault(OwnerBackpacks.UPGRADES,ItemContainerContents.EMPTY));load(filters,carrier.getOrDefault(OwnerBackpacks.FILTERS,ItemContainerContents.EMPTY));}
        for(int i=0;i<PAGE_SIZE;i++){
            final int pageSlot=i;
            addSlot(new Slot(view,i,20+(i%9)*18,32+(i/9)*18){
                @Override public boolean isActive(){return page()*PAGE_SIZE+pageSlot<capacity()&&storedCount(pageSlot)>=0;}
                @Override public boolean mayPlace(ItemStack stack){return isActive()&&OwnerBackpacks.allows(carrier,stack)&&mayPut(pageSlot,stack);}
                @Override public boolean mayPickup(Player p){return isActive()&&stillValid(p);}
                @Override public int getMaxStackSize(ItemStack stack){
                    if(!(owner instanceof ServerPlayer))return stack.getMaxStackSize();
                    var full=inventory.getItem(page()*PAGE_SIZE+pageSlot);
                    int reserved=full.isEmpty()?0:Math.max(0,full.getCount()-view.getItem(pageSlot).getCount());
                    return Math.min(stack.getMaxStackSize(),Math.max(0,OwnerBackpackStacks.limit(carrier,stack)-reserved));
                }
            });
        }
        for(int i=0;i<UPGRADES;i++){final int index=i;addSlot(new Slot(upgrades,i,201,32+i*18){
            @Override public int getMaxStackSize(){return 1;}
            @Override public boolean isActive(){return index<data.get(4);}
            @Override public boolean mayPlace(ItemStack item){
                if(owner instanceof ServerPlayer&&(!OwnerBackpackInfinity.mayEdit(owner,carrier)||item.is(OwnerBackpacks.ITEMS.get("infinity_upgrade"))&&!OwnerBackpackInfinity.admin(owner)))return false;
                if(!isActive()||!(item.getItem() instanceof OwnerBackpacks.UpgradeItem upgrade)||!OwnerBackpacks.UPGRADE_NAMES.contains(upgrade.name))return false;
                int count=0;boolean stackModule=OwnerBackpackStacks.module(item);
                for(int n=0;n<UPGRADES;n++)if(n!=index){var other=upgrades.getItem(n);
                    if(stackModule&&OwnerBackpackStacks.module(other))count++;
                    else if(!stackModule&&!other.isEmpty()&&group(other).equals(group(item)))return false;
                }
                if(stackModule&&count>=3)return false;
                return fitsReplacement(index,item);

            }
            @Override public boolean mayPickup(Player p){return stillValid(p)&&(!(p instanceof ServerPlayer)||OwnerBackpackInfinity.mayEdit(p,carrier))&&fitsReplacement(index,ItemStack.EMPTY)&&(!getItem().is(OwnerBackpacks.ITEMS.get("inception_upgrade"))||java.util.stream.IntStream.range(0,capacity()).noneMatch(n->inventory.getItem(n).getItem() instanceof OwnerBackpacks.PackItem));}
        });}
        for(int i=0;i<FILTERS;i++){final int filterIndex=i;addSlot(new Slot(filters,i,20+(i%9)*18,132+(i/9)*18){@Override public boolean isActive(){return filterIndex<OwnerBackpackFilters.slots(upgrades.getItem(selectedUpgrade()));}@Override public boolean mayPlace(ItemStack s){return false;}@Override public boolean mayPickup(Player p){return false;}});}
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)playerSlot(inv,9+row*9+col,20+col*18,201+row*18);
        for(int col=0;col<9;col++)playerSlot(inv,col,20+col*18,259);
        loading=false;readFilters();refreshView();addDataSlots(data);if(placed!=null&&owner instanceof ServerPlayer p)placed.start(p);
    }
    private static String group(ItemStack item){
        if(!(item.getItem() instanceof OwnerBackpacks.UpgradeItem upgrade))return "";
        String name=upgrade.name.replace("advanced_","");
        if(name.contains("smelting")||name.contains("smoking")||name.contains("blasting"))return "cooking";
        return name;
    }
    private boolean fitsReplacement(int slot,ItemStack item){
        if(!(owner instanceof ServerPlayer))return true;
        if(group(upgrades.getItem(slot)).equals("mob_catcher_upgrade")&&!OwnerBackpackMobs.entries(carrier).isEmpty())return false;
        var proposed=NonNullList.withSize(UPGRADES,ItemStack.EMPTY);
        for(int i=0;i<UPGRADES;i++)proposed.set(i,i==slot?item:upgrades.getItem(i));
        return OwnerBackpackStacks.fits(inventory,ItemContainerContents.fromItems(proposed));
    }
    private boolean mayPut(int pageSlot,ItemStack item){
        if(!(owner instanceof ServerPlayer))return true;
        if(!OwnerBackpackInfinity.mayEdit(owner,carrier))return false;
        var full=inventory.getItem(page()*PAGE_SIZE+pageSlot);
        return (full.isEmpty()||full.getCount()<=full.getMaxStackSize()||ItemStack.isSameItemSameComponents(full,item))
            &&(full.isEmpty()||!ItemStack.isSameItemSameComponents(full,item)||full.getCount()<OwnerBackpackStacks.limit(carrier,item));
    }
    private void refreshView(){
        if(!(owner instanceof ServerPlayer))return;
        data.set(97,OwnerBackpackFluids.flags(carrier));
        for(int i=0;i<PAGE_SIZE;i++){
            int slot=page()*PAGE_SIZE+i;var full=slot<capacity()?inventory.getItem(slot):ItemStack.EMPTY;
            var visible=full.isEmpty()?ItemStack.EMPTY:full.copyWithCount(OwnerBackpackInfinity.active(carrier)?full.getMaxStackSize():Math.min(full.getCount(),full.getMaxStackSize()));
            view.setItem(i,visible);baseline[i]=visible.copy();
            int count=OwnerBackpackMobs.occupied(carrier,slot)?-1:full.getCount();
            data.set(5+i*2,count&65535);data.set(6+i*2,(count>>>16)&65535);
        }
    }
    private void flushView(){
        if(!(owner instanceof ServerPlayer))return;
        for(int i=0;i<PAGE_SIZE;i++){
            int slot=page()*PAGE_SIZE+i;if(slot>=capacity())continue;
            var before=baseline[i];var after=view.getItem(i);var full=inventory.getItem(slot);
            if(ItemStack.matches(before,after))continue;
            if(!before.isEmpty()&&(after.isEmpty()||ItemStack.isSameItemSameComponents(before,after))){
                long total=(long)full.getCount()+after.getCount()-before.getCount();
                if(total<full.getCount()&&OwnerBackpackInfinity.retained(carrier,full))total=full.getCount();
                inventory.setItem(slot,total<=0?ItemStack.EMPTY:full.copyWithCount((int)Math.min(Integer.MAX_VALUE,total)));
            }else inventory.setItem(slot,after.copy());
        }
    }
    public int pumpFlags(){return data.get(97);}
    public int storedCount(int pageSlot){return (data.get(5+pageSlot*2)&65535)|((data.get(6+pageSlot*2)&65535)<<16);}
    @Override public void broadcastChanges(){
        if(!handlingClick&&!loading)refreshView();super.broadcastChanges();
        if(!loading&&owner instanceof ServerPlayer who&&who.containerMenu==this&&stillValid(who)){
            var creatures=OwnerBackpackMobs.entries(carrier);
            if(creatures!=sentCreatures){
                net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(who,new OwnerBackpackPreview(containerId,creatures));
                sentCreatures=creatures;
            }
        }
    }
    private void playerSlot(Inventory inv,int index,int x,int y){addSlot(new Slot(inv,index,x,y){@Override public boolean mayPickup(Player p){return index!=lockedSlot()&&stillValid(p);}@Override public boolean mayPlace(ItemStack stack){return index!=lockedSlot();}});}
    private static void load(Container target,ItemContainerContents contents){var items=NonNullList.withSize(target.getContainerSize(),ItemStack.EMPTY);contents.copyInto(items);for(int i=0;i<items.size();i++)target.setItem(i,items.get(i));}
    private ItemContainerContents stored(Container contents){var list=NonNullList.withSize(contents.getContainerSize(),ItemStack.EMPTY);for(int i=0;i<list.size();i++)list.set(i,contents.getItem(i));return ItemContainerContents.fromItems(list);}
    private void saveUpgrades(){if(!loading&&owner instanceof ServerPlayer&&stillValid(owner)){carrier.set(OwnerBackpacks.UPGRADES,stored(upgrades));changed();}}
    private void saveFilters(){if(!loading&&owner instanceof ServerPlayer&&stillValid(owner)&&OwnerBackpackFilters.configurable(upgrades.getItem(selectedUpgrade()))){upgrades.getItem(selectedUpgrade()).set(OwnerBackpacks.FILTERS,stored(filters));saveUpgrades();}}
    public int selectedUpgrade(){return Math.clamp(data.get(95),0,UPGRADES-1);}
    public int filterFlags(){return data.get(96);}
    public String selectedUpgradeName(){var item=upgrades.getItem(selectedUpgrade());return item.isEmpty()?"Choose an upgrade":item.getHoverName().getString();}
    private void readFilters(){if(!(owner instanceof ServerPlayer))return;boolean was=loading;loading=true;var selected=upgrades.getItem(selectedUpgrade());load(filters,OwnerBackpackFilters.filters(carrier,selected));data.set(96,OwnerBackpackFilters.flags(selected));loading=was;}
    private void changed(){if(placed!=null)placed.setChanged();else OwnerBackpacks.changed(owner,carrier);}
    public int capacity(){return data.get(3);}public int page(){return data.get(2);}public int pages(){return (capacity()+PAGE_SIZE-1)/PAGE_SIZE;}public int lockedSlot(){return data.get(0);}
    public boolean uses(ItemStack stack){return carrier==stack;}
    public boolean hasUpgrade(String name){for(int i=0;i<upgrades.getContainerSize();i++)if(upgrades.getItem(i).is(OwnerBackpacks.ITEMS.get(name+"_upgrade")))return true;return false;}
    @Override public boolean stillValid(Player player){
        if(owner!=player||!player.isAlive()||player.isSpectator())return false;
        if(!(player instanceof ServerPlayer p))return true;
        return placed!=null?placed.mayUse(p):OwnerBackpacks.heldBy(player,carrier,lockedSlot())&&OwnerBackpacks.tier(carrier)>=0;
    }
    @Override public void clicked(int slot,int button,ContainerInput input,Player player){
        if(!stillValid(player)||input==ContainerInput.SWAP&&button==lockedSlot())return;
        if(slot>=0&&slot<PAGE_SIZE&&storedCount(slot)<0){
            if(player instanceof ServerPlayer p&&input==ContainerInput.PICKUP&&getCarried().isEmpty()&&io.github.jimbozoomer.jugcraft.concordance.RateGate.allow(p,"backpack_release",10)){
                OwnerBackpackMobs.release(p,carrier,p.blockPosition().relative(p.getDirection(),2),page()*PAGE_SIZE+slot,this::changed);refreshView();broadcastChanges();
            }return;
        }
        if(player instanceof ServerPlayer&&input==ContainerInput.PICKUP&&slot>=PAGE_SIZE&&slot<PAGE_SIZE+UPGRADES){
            int upgradeSlot=slot-PAGE_SIZE;var replacement=OwnerBackpackConversions.replacement(getCarried(),upgrades.getItem(upgradeSlot));
            if(OwnerBackpackInfinity.mayEdit(player,carrier)&&!replacement.isEmpty()&&fitsReplacement(upgradeSlot,replacement)){
                upgrades.setItem(upgradeSlot,replacement);getCarried().shrink(1);broadcastChanges();return;
            }
        }
        if(slot>=PAGE_SIZE+UPGRADES&&slot<PLAYER_START){
            if(input==ContainerInput.PICKUP&&slots.get(slot).isActive()){filters.setItem(slot-PAGE_SIZE-UPGRADES,button==1?ItemStack.EMPTY:getCarried().copyWithCount(1));broadcastChanges();}return;
        }
        if(slot>=0&&slot<slots.size()&&slots.get(slot).container==player.getInventory()&&slots.get(slot).getContainerSlot()==lockedSlot())return;
        if(slot>=0&&slot<PAGE_SIZE&&page()*PAGE_SIZE+slot<capacity()&&owner instanceof ServerPlayer){
            var full=inventory.getItem(page()*PAGE_SIZE+slot);
            if(full.getCount()>full.getMaxStackSize()){
                if(input==ContainerInput.SWAP)return;
                if(input==ContainerInput.PICKUP&&!getCarried().isEmpty()&&!ItemStack.isSameItemSameComponents(full,getCarried()))return;
            }
        }
        handlingClick=true;
        try{super.clicked(slot,button,input,player);}finally{flushView();handlingClick=false;readFilters();refreshView();broadcastChanges();}
    }
    @Override public boolean clickMenuButton(Player player,int action){
        if(!(player instanceof ServerPlayer p)||!stillValid(player)||!getCarried().isEmpty()||!io.github.jimbozoomer.jugcraft.concordance.RateGate.allow(p,"backpack_menu",2))return false;
        if(action==0||action==1){data.set(2,Math.clamp(page()+(action==0?-1:1),0,pages()-1));broadcastChanges();return true;}
        if(action==2){
            if(!OwnerBackpackInfinity.mayEdit(player,carrier))return false;
            var sorted=new java.util.ArrayList<ItemStack>();for(int i=0;i<capacity();i++)if(!inventory.getItem(i).isEmpty())sorted.add(inventory.getItem(i).copy());
            sorted.sort(java.util.Comparator.comparing(s->net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(s.getItem()).toString()));
            int next=0;for(int i=0;i<capacity();i++){if(OwnerBackpackMobs.occupied(carrier,i))continue;inventory.setItem(i,next<sorted.size()?sorted.get(next++):ItemStack.EMPTY);}broadcastChanges();return true;
        }
        if(action==62){boolean result=OwnerBackpackInfinity.toggle(p,carrier);if(result){changed();broadcastChanges();}return result;}
        if(action==60||action==61){boolean result=OwnerBackpackFluids.settings(p,carrier,action);if(result){changed();broadcastChanges();}return result;}
        if(action==50||action==51){
            for(int step=1;step<=UPGRADES;step++){int candidate=Math.floorMod(selectedUpgrade()+(action==50?-step:step),UPGRADES);if(OwnerBackpackFilters.configurable(upgrades.getItem(candidate))){data.set(95,candidate);break;}}
            readFilters();broadcastChanges();return true;
        }
        if(action>=52&&action<=54){
            var selected=upgrades.getItem(selectedUpgrade());if(!OwnerBackpackFilters.configurable(selected))return false;
            int flags=OwnerBackpackFilters.flags(selected);flags=action==52?flags^1:action==53?flags^2:(flags&3)|((((flags>>>2)+1)%3)<<2);
            int next=flags;net.minecraft.world.item.component.CustomData.update(DataComponents.CUSTOM_DATA,selected,t->t.putInt("PackFilterFlags",next));saveUpgrades();readFilters();broadcastChanges();return true;
        }
        if(action>=40&&action<=42){boolean result=OwnerBackpackMusic.button(p,carrier,action);if(result)changed();return result;}
        if(action>=10&&action<=13)return OwnerBackpackWorkstations.open(p,carrier,lockedSlot(),placed,action);
        if(action>=20&&action<=22)return OwnerBackpackCooking.open(p,carrier,lockedSlot(),placed,action);
        if(action==30||action==31){
            for(int i=0;i<upgrades.getContainerSize();i++)if(upgrades.getItem(i).is(OwnerBackpacks.ITEMS.get("xp_pump_upgrade"))){
                var item=upgrades.getItem(i);var component=io.github.jimbozoomer.jugcraft.concordance.assay.AlchemyStorage.EXPERIENCE;
                int stored=item.getOrDefault(component,0),actual=io.github.jimbozoomer.jugcraft.concordance.assay.AlchemyStorage.currentXp(p);
                int amount=action==30?Math.min(100,Math.min(actual,1_000_000-stored)):Math.min(100,Math.min(stored,Integer.MAX_VALUE-actual));
                if(amount<=0)return false;
                p.experienceLevel=0;p.experienceProgress=0;p.totalExperience=0;p.giveExperiencePoints(actual+(action==30?-amount:amount));
                item.set(component,stored+(action==30?amount:-amount));upgrades.setChanged();p.sendOverlayMessage(net.minecraft.network.chat.Component.literal(item.getOrDefault(component,0)+" stored experience points"));return true;
            }
        }
        return false;
    }
    @Override public ItemStack quickMoveStack(Player player,int index){
        if(!stillValid(player)||index<0||index>=slots.size())return ItemStack.EMPTY;var slot=slots.get(index);if(!slot.hasItem()||!slot.mayPickup(player))return ItemStack.EMPTY;
        var stack=slot.getItem();var original=stack.copy();
        if(index>=PLAYER_START&&owner instanceof ServerPlayer&&!OwnerBackpackInfinity.mayEdit(owner,carrier))return ItemStack.EMPTY;
        if(index>=PLAYER_START&&inventory instanceof OwnerBackpackInventory bag&&!(stack.getItem() instanceof OwnerBackpacks.UpgradeItem)){
            var left=bag.insert(stack);if(left.getCount()==stack.getCount())return ItemStack.EMPTY;
            slot.setByPlayer(left);slot.onTake(player,original.copyWithCount(original.getCount()-left.getCount()));refreshView();return original;
        }
        if(index<PLAYER_START){if(!moveItemStackTo(stack,PLAYER_START,slots.size(),true))return ItemStack.EMPTY;}
        else if(stack.getItem() instanceof OwnerBackpacks.UpgradeItem&&moveItemStackTo(stack,PAGE_SIZE,PAGE_SIZE+UPGRADES,false)){}
        else if(!OwnerBackpacks.allows(carrier,stack)||!moveItemStackTo(stack,0,PAGE_SIZE,false))return ItemStack.EMPTY;
        if(stack.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();slot.onTake(player,stack);inventory.setChanged();return original;
    }
    @Override public void removed(Player player){if(placed!=null&&player instanceof ServerPlayer p)placed.stop(p);super.removed(player);}
}
