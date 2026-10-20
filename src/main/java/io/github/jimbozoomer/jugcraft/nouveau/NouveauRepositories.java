package io.github.jimbozoomer.jugcraft.nouveau;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
/** Live bounded catalog: no mirrored stacks and no copied inventory contents. */
public final class NouveauRepositories {
    public static MenuType<Menu> MENU;
    private NouveauRepositories(){}
    public static void register(){MENU=Registry.register(BuiltInRegistries.MENU,Jugcraft.id("nouveau_repository_catalog"),new MenuType<>(Menu::new,FeatureFlags.DEFAULT_FLAGS));}
    static List<NouveauWorkshop> connected(NouveauWorkshop root){
        if(!(root.getLevel() instanceof ServerLevel l))return List.of();var owner=Authority.answering(l,root.owner());if(owner==null||!root.permitted(owner))return List.of();
        var found=new ArrayList<NouveauWorkshop>();var pending=new ArrayDeque<BlockPos>();var visited=new HashSet<BlockPos>();pending.add(root.getBlockPos());
        while(!pending.isEmpty()&&visited.size()<128&&found.size()<31){var at=pending.removeFirst();if(!visited.add(at)||!l.hasChunkAt(at)||!(l.getBlockEntity(at) instanceof NouveauWorkshop b)||!b.permitted(owner)||!Set.of("repository","repository_controller").contains(b.kind()))continue;
            if(b!=root&&b.kind().equals("repository"))found.add(b);
            for(var d:Direction.values())pending.add(at.relative(d));for(var link:b.links())pending.add(link);
        }return List.copyOf(found);
    }
    public static final class Menu extends AbstractContainerMenu {
        private final Inventory inv;private final NouveauWorkshop root;private int page;
        private final ContainerData data=new SimpleContainerData(2);
        private final Container view;
        public Menu(int id,Inventory inv){this(id,inv,null);}
        public Menu(int id,Inventory inv,NouveauWorkshop root){super(MENU,id);this.inv=inv;this.root=root;
            view=root==null?new SimpleContainer(54):new Container(){
                public int getContainerSize(){return 54;}private int slot(int i){return page*54+i;}
                public boolean isEmpty(){for(int i=0;i<54;i++)if(!getItem(i).isEmpty())return false;return true;}
                public ItemStack getItem(int i){return slot(i)<root.getContainerSize()?root.getItem(slot(i)):ItemStack.EMPTY;}
                public ItemStack removeItem(int i,int n){return slot(i)<root.getContainerSize()?root.removeItem(slot(i),n):ItemStack.EMPTY;}
                public ItemStack removeItemNoUpdate(int i){return removeItem(i,64);}
                public void setItem(int i,ItemStack s){if(slot(i)<root.getContainerSize())root.setItem(slot(i),s);}
                public boolean canPlaceItem(int i,ItemStack s){return slot(i)<root.getContainerSize()&&root.canPlaceItem(slot(i),s)&&NouveauFilters.allows(root.filter(),s);}
                public void setChanged(){root.setChanged();}public boolean stillValid(Player p){return root.stillValid(p);}public void clearContent(){}
            };
            for(int i=0;i<54;i++){final int slot=i;addSlot(new Slot(view,i,8+i%9*18,30+i/9*18){public boolean mayPlace(ItemStack s){return stillValid(inv.player)&&view.canPlaceItem(slot,s);}});}
            for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inv,9+row*9+col,8+col*18,164+row*18));for(int i=0;i<9;i++)addSlot(new Slot(inv,i,8+i*18,222));addDataSlots(data);refresh();
        }
        private void refresh(){if(root==null)return;int pages=(root.getContainerSize()+53)/54;page=Math.clamp(page,0,Math.max(0,pages-1));data.set(0,page);data.set(1,pages);}
        public int page(){return data.get(0);}public int pages(){return data.get(1);}
        public boolean stillValid(Player p){return p==inv.player&&p.isAlive()&&!p.isSpectator()&&(!(p instanceof ServerPlayer)||root!=null&&root.stillValid(p));}
        public void broadcastChanges(){refresh();super.broadcastChanges();}
        public boolean clickMenuButton(Player p,int button){if(!stillValid(p)||root==null||!getCarried().isEmpty()||button<0||button>1)return false;page+=button==0?-1:1;refresh();broadcastFullState();return true;}
        public ItemStack quickMoveStack(Player p,int i){if(!stillValid(p)||i<0||i>=slots.size())return ItemStack.EMPTY;var slot=slots.get(i);if(!slot.hasItem())return ItemStack.EMPTY;var held=slot.getItem();var original=held.copy();if(i<54?!moveItemStackTo(held,54,90,true):!moveItemStackTo(held,0,54,false))return ItemStack.EMPTY;if(held.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();return original;}
    }
}
