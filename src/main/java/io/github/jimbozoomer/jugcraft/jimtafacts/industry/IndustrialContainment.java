package io.github.jimbozoomer.jugcraft.jimtafacts.industry;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.jimtafacts.JimtaContainer;
import io.github.jimbozoomer.jugcraft.nuclear.*;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;
import net.minecraft.world.level.Level;

/** Original twelve-slot containment case connected to Jugcraft's real radiation accounting. */
public final class IndustrialContainment {
    public static Item ITEM;
    public static MenuType<Menu> MENU;
    public static final double LEAKAGE=.01;
    private IndustrialContainment(){}
    public static void register(){
        var id=Jugcraft.id("jimta_containment_box");var key=ResourceKey.create(Registries.ITEM,id);
        ITEM=Registry.register(BuiltInRegistries.ITEM,key,new Case(new Item.Properties().setId(key).stacksTo(1)));
        IndustrialUtilityItems.ITEMS.put("containment_box",ITEM);
        MENU=Registry.register(BuiltInRegistries.MENU,id,new MenuType<>(Menu::new,FeatureFlags.DEFAULT_FLAGS));
    }
    public static boolean accepts(ItemStack s){return !s.isEmpty()&&s.getItem()!=ITEM&&!s.has(DataComponents.CONTAINER)&&!s.has(DataComponents.BUNDLE_CONTENTS)
        &&(s.getOrDefault(JugcraftNuclear.RADIOACTIVITY,0F)>0||RadioactiveMaterials.item(s.getItem())>0);}
    private static final class Case extends Item {
        Case(Properties p){super(p);}
        @Override public InteractionResult use(Level l,Player p,InteractionHand hand){
            if(!p.isAlive()||p.isSpectator())return InteractionResult.FAIL;
            if(p instanceof ServerPlayer sp){var held=p.getItemInHand(hand);sp.openMenu(new SimpleMenuProvider((id,inv,who)->new Menu(id,inv,hand,held),Component.literal("Containment Box")));}
            return InteractionResult.SUCCESS;
        }
        @Override public void appendHoverText(ItemStack s,TooltipContext c,TooltipDisplay d,java.util.function.Consumer<Component> out,TooltipFlag f){out.accept(Component.literal("12 slots for radioactive materials · blocks 99% of stored radiation."));out.accept(Component.literal("Open in either hand. No nested containers."));}
    }
    public static final class Menu extends AbstractContainerMenu {
        private final InteractionHand hand;
        private final ItemStack held;
        private final JimtaContainer contents=new JimtaContainer(12);
        public Menu(int id,Inventory inv){this(id,inv,null,ItemStack.EMPTY);}
        Menu(int id,Inventory inv,InteractionHand hand,ItemStack held){
            super(MENU,id);this.hand=hand;this.held=held;
            if(hand!=null){held.getOrDefault(DataComponents.CONTAINER,ItemContainerContents.EMPTY).copyInto(contents.getItems());
                contents.addListener(c->{held.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(contents.getItems()));inv.setChanged();});}
            for(int row=0;row<3;row++)for(int col=0;col<4;col++)addSlot(new Slot(contents,row*4+col,53+col*18,19+row*18){@Override public boolean mayPlace(ItemStack s){return accepts(s);}});
            for(int row=0;row<3;row++)for(int col=0;col<9;col++)playerSlot(inv,9+row*9+col,8+col*18,84+row*18);
            for(int col=0;col<9;col++)playerSlot(inv,col,8+col*18,142);
        }
        private void playerSlot(Inventory inv,int id,int x,int y){addSlot(new Slot(inv,id,x,y){@Override public boolean mayPickup(Player p){return held.isEmpty()||getItem()!=held;}@Override public boolean mayPlace(ItemStack s){return held.isEmpty()||getItem()!=held;}});}
        @Override public boolean stillValid(Player p){return hand==null||p.isAlive()&&!p.isSpectator()&&!held.isEmpty()&&p.getItemInHand(hand)==held;}
        @Override public void clicked(int slot,int button,ContainerInput input,Player p){
            if(!stillValid(p)||input==ContainerInput.SWAP&&(button==40&&hand==InteractionHand.OFF_HAND||button==p.getInventory().getSelectedSlot()&&hand==InteractionHand.MAIN_HAND))return;
            super.clicked(slot,button,input,p);
        }
        @Override public ItemStack quickMoveStack(Player p,int i){
            if(!stillValid(p)||i<0||i>=slots.size())return ItemStack.EMPTY;
            var slot=slots.get(i);var s=slot.getItem();if(s.isEmpty()||s==held)return ItemStack.EMPTY;var original=s.copy();
            if(i<12?!moveItemStackTo(s,12,slots.size(),true):!accepts(s)||!moveItemStackTo(s,0,12,false))return ItemStack.EMPTY;
            slot.setByPlayer(s.isEmpty()?ItemStack.EMPTY:s);slot.setChanged();return original;
        }
    }
}
