package io.github.jimbozoomer.jugcraft.botany;

import eu.pb4.trinkets.api.callback.TrinketCallback;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.mixin.AlchemicalItemAccess;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;

/** Component-backed source bags. Held carrier slots and hotbar swaps are locked while their menu is open. */
public final class BotanicalBags extends Item {
    public static MenuType<Menu> FLOWER_MENU,BOX_MENU;
    static void register(){
        FLOWER_MENU=Registry.register(net.minecraft.core.registries.BuiltInRegistries.MENU,io.github.jimbozoomer.jugcraft.Jugcraft.id("botanical_flower_bag"),new MenuType<>((id,inv)->new Menu(id,inv,true),net.minecraft.world.flag.FeatureFlags.VANILLA_SET));
        BOX_MENU=Registry.register(net.minecraft.core.registries.BuiltInRegistries.MENU,io.github.jimbozoomer.jugcraft.Jugcraft.id("botanical_bauble_box"),new MenuType<>((id,inv)->new Menu(id,inv,false),net.minecraft.world.flag.FeatureFlags.VANILLA_SET));
    }
    private final boolean flowers;
    BotanicalBags(boolean flowers,Properties p){super(p.stacksTo(1));this.flowers=flowers;}
    private int size(){return flowers?32:24;}
    private int flowerSlot(ItemStack stack){
        for(var color:DyeColor.values()){String name=color.getName();if(stack.is(Jugtania.item(name+"_mystical_flower")))return color.getId();if(stack.is(Jugtania.item(name+"_double_flower")))return color.getId()+16;}return -1;
    }
    private boolean accepts(int slot,ItemStack stack){return slot<size()&&(flowers?flowerSlot(stack)==slot:stack.getItem() instanceof TrinketCallback&&!stack.has(DataComponents.CONTAINER)&&!(stack.getItem() instanceof BotanicalBags));}
    private NonNullList<ItemStack> contents(ItemStack carrier){var items=NonNullList.withSize(size(),ItemStack.EMPTY);carrier.getOrDefault(DataComponents.CONTAINER,ItemContainerContents.EMPTY).copyInto(items);return items;}
    private int deposit(ItemStack bag,ItemStack input,int limit){
        var entries=contents(bag);int moved=0;
        for(int i=0;i<entries.size()&&moved<limit&&!input.isEmpty();i++){
            if(!accepts(i,input))continue;var old=entries.get(i);
            if(!old.isEmpty()&&!ItemStack.isSameItemSameComponents(old,input))continue;
            int n=Math.min(Math.min(input.getCount(),limit-moved),input.getMaxStackSize()-old.getCount());if(n<=0)continue;
            entries.set(i,input.copyWithCount(old.getCount()+n));input.shrink(n);moved+=n;
        }
        if(moved>0)bag.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(entries));return moved;
    }
    @Override public boolean overrideOtherStackedOnMe(ItemStack bag,ItemStack cursor,Slot slot,ClickAction action,Player player,net.minecraft.world.entity.SlotAccess cursorAccess){
        if(action!=ClickAction.SECONDARY||!slot.allowModification(player))return false;
        if(!cursor.isEmpty()){if(deposit(bag,cursor,cursor.getCount())==0)return false;cursorAccess.set(cursor);slot.setChanged();return true;}
        var entries=contents(bag);for(int i=entries.size()-1;i>=0;i--)if(!entries.get(i).isEmpty()){
            var item=entries.get(i);if(!cursorAccess.set(item.copy()))return false;entries.set(i,ItemStack.EMPTY);bag.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(entries));slot.setChanged();return true;
        }return false;
    }
    @Override public boolean overrideStackedOnOther(ItemStack bag,Slot slot,ClickAction action,Player player){
        if(action!=ClickAction.SECONDARY||!slot.allowModification(player)||!slot.mayPickup(player))return false;
        if(!slot.getItem().isEmpty()){
            var simulated=bag.copy();var trial=slot.getItem().copy();int n=deposit(simulated,trial,trial.getCount());if(n==0)return false;
            var taken=slot.safeTake(n,n,player);if(taken.isEmpty())return false;deposit(bag,taken,taken.getCount());slot.setChanged();return true;
        }
        var entries=contents(bag);for(int i=entries.size()-1;i>=0;i--)if(!entries.get(i).isEmpty()&&slot.mayPlace(entries.get(i))){
            var old=entries.get(i);var rest=slot.safeInsert(old.copy());if(rest.getCount()==old.getCount())return false;entries.set(i,rest);bag.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(entries));slot.setChanged();return true;
        }return false;
    }
    @Override public boolean canFitInsideContainerItems(){return false;}
    @Override public InteractionResult use(Level level,Player user,InteractionHand hand){
        if(!(user instanceof ServerPlayer p))return InteractionResult.SUCCESS;
        if(p.isSpectator())return InteractionResult.FAIL;var carrier=p.getItemInHand(hand);
        p.openMenu(new SimpleMenuProvider((id,inv,who)->new Menu(id,inv,p,hand,carrier,this),carrier.getHoverName()));return InteractionResult.SUCCESS;
    }
    @Override public InteractionResult useOn(net.minecraft.world.item.context.UseOnContext c){
        if(!flowers||c.getPlayer()==null||!c.getPlayer().isShiftKeyDown())return InteractionResult.PASS;
        if(!(c.getPlayer() instanceof ServerPlayer p))return InteractionResult.SUCCESS;
        var at=c.getClickedPos();if(!p.level().hasChunkAt(at)||!Authority.mayChange(p.level(),p,at))return InteractionResult.FAIL;
        var be=p.level().getBlockEntity(at);if(be instanceof net.minecraft.world.level.block.entity.BaseContainerBlockEntity locked&&locked.isLocked()||be instanceof net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity loot&&loot.getLootTable()!=null)return InteractionResult.FAIL;
        // A direct chest adapter deliberately addresses only the permitted half, not its neighboring inventory.
        var storage=be instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chest?net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage.of(chest,c.getClickedFace()):net.fabricmc.fabric.api.transfer.v1.item.ItemStorage.SIDED.find(p.level(),at,c.getClickedFace());
        if(storage==null)return InteractionResult.PASS;var bag=c.getItemInHand();var slots=contents(bag);boolean moved=false;
        try(var transaction=net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()){
            for(int i=0;i<slots.size();i++){var item=slots.get(i);if(item.isEmpty())continue;long n=storage.insert(net.fabricmc.fabric.api.transfer.v1.item.ItemVariant.of(item),item.getCount(),transaction);if(n>0){item.shrink((int)n);moved=true;}}
            transaction.commit();
        }
        if(moved){bag.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(slots));p.getInventory().setChanged();}return InteractionResult.SUCCESS;
    }
    /** Only ordinary eligible contact pickups: never attracts items or bypasses a reservation. */
    public static boolean pickup(ServerPlayer p,ItemEntity entity){
        if(p.isSpectator()||!p.isAlive()||entity.isRemoved()||entity.hasPickUpDelay()||!Authority.mayChange(p.level(),p,entity.blockPosition()))return false;
        var target=((AlchemicalItemAccess)entity).jugcraft$pickupTarget();if(target!=null&&!target.equals(p.getUUID()))return false;
        var incoming=entity.getItem();int before=incoming.getCount();
        for(int i=0;i<36&&!incoming.isEmpty();i++){
            var carrier=p.getInventory().getItem(i);if(!(carrier.getItem() instanceof BotanicalBags bag)||!bag.flowers||carrier==p.getMainHandItem()||p.containerMenu instanceof Menu menu&&menu.carrier==carrier)continue;
            int slot=bag.flowerSlot(incoming);if(slot<0)continue;var slots=bag.contents(carrier);var existing=slots.get(slot);
            if(!existing.isEmpty()&&!ItemStack.isSameItemSameComponents(existing,incoming))continue;
            int count=Math.min(incoming.getCount(),incoming.getMaxStackSize()-existing.getCount());if(count<=0)continue;
            slots.set(slot,incoming.copyWithCount(existing.getCount()+count));incoming.shrink(count);carrier.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(slots));p.getInventory().setChanged();
        }
        int picked=before-incoming.getCount();if(picked==0)return false;p.take(entity,picked);if(incoming.isEmpty()){entity.discard();return true;}entity.setItem(incoming);return false;
    }
    public static final class Menu extends AbstractContainerMenu {
        private final Player owner;private final InteractionHand hand;private final ItemStack carrier;private final Container contents;public final boolean flowers;private final int capacity;
        private Menu(int id,Inventory inventory,boolean flowers){this(id,inventory,inventory.player,null,ItemStack.EMPTY,(BotanicalBags)Jugtania.item(flowers?"flower_bag":"bauble_box"),new SimpleContainer(flowers?32:24));}
        Menu(int id,Inventory inventory,ServerPlayer owner,InteractionHand hand,ItemStack carrier,BotanicalBags bag){this(id,inventory,owner,hand,carrier,bag,new Contents(owner,hand,carrier,bag));}
        private Menu(int id,Inventory inventory,Player owner,InteractionHand hand,ItemStack carrier,BotanicalBags bag,Container contents){
            super(bag.flowers?FLOWER_MENU:BOX_MENU,id);this.owner=owner;this.hand=hand;this.carrier=carrier;this.contents=contents;flowers=bag.flowers;capacity=bag.size();
            int columns=flowers?8:6,x=flowers?17:62,y=flowers?26:8;
            for(int i=0;i<capacity;i++){final int index=i;addSlot(new Slot(contents,i,x+i%columns*18,y+i/columns*18){@Override public boolean mayPlace(ItemStack stack){return stillValid(owner)&&bag.accepts(index,stack);}@Override public boolean mayPickup(Player p){return stillValid(p);}});}
            int inventoryY=flowers?120:84;
            for(int row=0;row<3;row++)for(int col=0;col<9;col++)playerSlot(inventory,9+row*9+col,8+col*18,inventoryY+row*18);
            for(int col=0;col<9;col++)playerSlot(inventory,col,8+col*18,inventoryY+58);
        }
        private void playerSlot(Inventory inventory,int index,int x,int y){addSlot(new Slot(inventory,index,x,y){@Override public boolean mayPickup(Player p){return (hand==null||getItem()!=carrier)&&stillValid(p);}@Override public boolean mayPlace(ItemStack item){return (hand==null||getItem()!=carrier&&item!=carrier)&&stillValid(owner);}});}
        @Override public boolean stillValid(Player p){return p==owner&&p.isAlive()&&(hand==null||p.getItemInHand(hand)==carrier);}
        @Override public void clicked(int slot,int button,ContainerInput type,Player p){
            if(!stillValid(p)||hand!=null&&slot>=0&&slot<slots.size()&&slots.get(slot).getItem()==carrier)return;
            if(type==ContainerInput.SWAP&&(button==40&&hand==InteractionHand.OFF_HAND||button>=0&&button<9&&p.getInventory().getItem(button)==carrier))return;
            super.clicked(slot,button,type,p);
        }
        @Override public ItemStack quickMoveStack(Player p,int index){
            if(!stillValid(p)||index<0||index>=slots.size())return ItemStack.EMPTY;var slot=slots.get(index);if(!slot.hasItem()||!slot.mayPickup(p))return ItemStack.EMPTY;var item=slot.getItem();var before=item.copy();
            if(!(index<capacity?moveItemStackTo(item,capacity,slots.size(),true):moveItemStackTo(item,0,capacity,false)))return ItemStack.EMPTY;
            if(item.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();slot.onTake(p,item);return before;
        }
        @Override public void removed(Player p){contents.setChanged();super.removed(p);}
    }
    private static final class Contents extends SimpleContainer {
        private final ServerPlayer owner;private final InteractionHand hand;private final ItemStack carrier;private final BotanicalBags bag;private boolean loading=true;
        Contents(ServerPlayer owner,InteractionHand hand,ItemStack carrier,BotanicalBags bag){super(bag.size());this.owner=owner;this.hand=hand;this.carrier=carrier;this.bag=bag;var items=bag.contents(carrier);for(int i=0;i<items.size();i++)setItem(i,items.get(i));loading=false;}
        @Override public boolean canPlaceItem(int slot,ItemStack stack){return bag.accepts(slot,stack);}
        @Override public void setChanged(){super.setChanged();if(loading||owner.getItemInHand(hand)!=carrier)return;var items=NonNullList.withSize(bag.size(),ItemStack.EMPTY);for(int i=0;i<items.size();i++)items.set(i,getItem(i).copy());carrier.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(items));owner.getInventory().setChanged();}
    }
}
