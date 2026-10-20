package io.github.jimbozoomer.jugcraft.nouveau;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import java.util.*;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.phys.Vec3;

/** Searchable references, never stored copies. Every transfer re-resolves the permitted physical containers. */
public final class NouveauStorageTerminal {
    public static MenuType<Menu> TYPE;
    private NouveauStorageTerminal(){}
    public static void register(){
        TYPE=Registry.register(BuiltInRegistries.MENU,Jugcraft.id("nouveau_storage_terminal"),new MenuType<>(Menu::new,FeatureFlags.DEFAULT_FLAGS));
        PayloadTypeRegistry.serverboundPlay().register(Search.TYPE,Search.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(TabState.TYPE,TabState.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(Search.TYPE,(packet,context)->{var p=context.player();if(p.containerMenu instanceof Menu menu&&menu.containerId==packet.menu&&menu.stillValid(p)&&RateGate.allow(p,"nouveau_storage_search",2)){menu.search=packet.query.strip().toLowerCase(Locale.ROOT);menu.page=0;menu.refresh();menu.broadcastFullState();}});
    }
    public record Search(int menu,String query) implements CustomPacketPayload {
        public static final Type<Search> TYPE=new Type<>(Jugcraft.id("nouveau_storage_search"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Search> CODEC=new StreamCodec<>(){
            public Search decode(RegistryFriendlyByteBuf b){return new Search(b.readVarInt(),b.readUtf(48));}
            public void encode(RegistryFriendlyByteBuf b,Search p){b.writeVarInt(p.menu);b.writeUtf(p.query,48);}
        };public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public record TabState(int menu,String label) implements CustomPacketPayload {
        public static final Type<TabState> TYPE=new Type<>(Jugcraft.id("nouveau_storage_tab"));
        public static final StreamCodec<RegistryFriendlyByteBuf,TabState> CODEC=new StreamCodec<>(){public TabState decode(RegistryFriendlyByteBuf b){return new TabState(b.readVarInt(),b.readUtf(96));}public void encode(RegistryFriendlyByteBuf b,TabState p){b.writeVarInt(p.menu);b.writeUtf(p.label,96);}};public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    private record Ref(Container container,int slot,NouveauWorkshop controller){ItemStack stack(){return container.getItem(slot);}boolean accepts(ItemStack stack){return (controller==null||controller.accepts(stack))&&container.canPlaceItem(slot,stack);}}
    private record Key(ItemStack stack){
        @Override public int hashCode(){return ItemStack.hashItemAndComponents(stack);}
        @Override public boolean equals(Object other){return other instanceof Key key&&ItemStack.isSameItemSameComponents(stack,key.stack);}
    }
    private static final class Entry {final ItemStack item;int count;Entry(ItemStack item){this.item=item.copyWithCount(1);}}
    public static final class Menu extends AbstractContainerMenu {
        public static final int GRID=36,RESULT=45,PLAYER=46;
        private final Inventory inventory;private final StorageLectern lectern;
        private final SimpleContainer catalog=new SimpleContainer(GRID);
        private final TransientCraftingContainer grid=new TransientCraftingContainer(this,3,3);
        private final ResultContainer result=new ResultContainer();
        private final ContainerData data=new SimpleContainerData(3+GRID*2);
        private String search="",group="",sentGroup=null;public String visibleGroup="All inventories";private int page;private long refreshed=-1;
        public Menu(int id,Inventory inventory){this(id,inventory,null);}
        public Menu(int id,Inventory inventory,StorageLectern lectern){super(TYPE,id);this.inventory=inventory;this.lectern=lectern;
            for(int i=0;i<GRID;i++)addSlot(new Slot(catalog,i,18+i%9*18,50+i/9*18){public boolean mayPlace(ItemStack s){return false;}public boolean mayPickup(Player p){return false;}});
            for(int i=0;i<9;i++)addSlot(new Slot(grid,i,222+i%3*18,72+i/3*18));
            addSlot(new ResultSlot(inventory.player,grid,result,0,294,90){public boolean mayPickup(Player p){return stillValid(p);}});
            for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inventory,9+row*9+col,18+col*18,156+row*18));
            for(int i=0;i<9;i++)addSlot(new Slot(inventory,i,18+i*18,214));addDataSlots(data);refresh();
        }
        public int page(){return data.get(0);}public int pages(){return data.get(1);}public int connected(){return data.get(2);}
        public int count(int i){return (data.get(3+i*2)&65535)|((data.get(4+i*2)&65535)<<16);}
        private List<Ref> references(){
            if(!(inventory.player instanceof ServerPlayer p)||!stillValid(p))return List.of();
            var root=lectern.root(p);if(root==null)return List.of();Set<Container> containers=Collections.newSetFromMap(new IdentityHashMap<>());var ordered=new ArrayList<Container>();var controllers=new IdentityHashMap<Container,NouveauWorkshop>();
            for(int i=0;i<root.links().size()&&i<root.capacity();i++){
                var c=root.linkedContainer(p,i);if(c==null)continue;if(containers.add(c))ordered.add(c);
                if(c instanceof NouveauWorkshop b&&b.kind().equals("repository_controller"))for(var child:b.connectedRepositories())if(child.permitted(p)&&containers.add(child)){ordered.add(child);controllers.put(child,b);}
                if(ordered.size()>=128)break;
            }
            var refs=new ArrayList<Ref>();for(var c:ordered.stream().limit(128).toList())for(int i=0;i<Math.min(54,c.getContainerSize());i++)refs.add(new Ref(c,i,controllers.get(c)));return refs;
        }
        private String label(Container c){if(c instanceof NouveauWorkshop w)return w.customName();if(c instanceof net.minecraft.world.level.block.entity.BaseContainerBlockEntity b&&b.getCustomName()!=null)return b.getCustomName().getString();return "";}
        private List<String> groups(){var names=new java.util.TreeSet<String>();for(var ref:references()){String label=label(ref.container);if(!label.isBlank())names.add(label);}var groups=new ArrayList<String>();groups.add("");groups.addAll(names);return groups;}
        private void refresh(){
            if(!(inventory.player instanceof ServerPlayer))return;var refs=references();var grouped=new LinkedHashMap<Key,Entry>();
            for(var ref:refs){if(!group.isEmpty()&&!group.equals(label(ref.container)))continue;var stack=ref.stack();if(stack.isEmpty())continue;
                String name=stack.getHoverName().getString().toLowerCase(Locale.ROOT),id=BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();if(!search.isEmpty()&&!name.contains(search)&&!id.contains(search))continue;
                var key=new Key(stack.copyWithCount(1));var found=grouped.computeIfAbsent(key,k->new Entry(k.stack));found.count+=stack.getCount();
            }
            var entries=new ArrayList<>(grouped.values());entries.sort(Comparator.comparing(e->e.item.getHoverName().getString()));int pages=Math.max(1,(entries.size()+GRID-1)/GRID);page=Math.clamp(page,0,pages-1);data.set(0,page);data.set(1,pages);data.set(2,lectern==null?0:Math.min(lectern.links().size(),lectern.capacity()));
            for(int i=0;i<GRID;i++){int at=page*GRID+i,n=at<entries.size()?entries.get(at).count:0;catalog.setItem(i,at<entries.size()?entries.get(at).item.copy():ItemStack.EMPTY);data.set(3+i*2,n&65535);data.set(4+i*2,n>>>16);}refreshed=inventory.player.level().getGameTime();if(!Objects.equals(sentGroup,group)){sentGroup=group;if(inventory.player instanceof ServerPlayer p)ServerPlayNetworking.send(p,new TabState(containerId,group.isEmpty()?"All inventories":group.substring(0,Math.min(96,group.length()))));}
        }
        private ItemStack withdraw(ItemStack example,int amount){
            if(amount<=0)return ItemStack.EMPTY;ItemStack taken=ItemStack.EMPTY;for(var ref:references())if((group.isEmpty()||group.equals(label(ref.container)))&&ItemStack.isSameItemSameComponents(example,ref.stack())){int n=Math.min(amount,ref.stack().getCount());if(n>0){var actual=ref.container.removeItem(ref.slot,n);if(taken.isEmpty())taken=actual.copy();else taken.grow(actual.getCount());amount-=actual.getCount();ref.container.setChanged();}if(amount==0)break;}return taken;
        }
        private int room(ItemStack stack){int room=0;for(int i=0;i<36;i++){var s=inventory.getItem(i);if(s.isEmpty())room+=stack.getMaxStackSize();else if(ItemStack.isSameItemSameComponents(s,stack))room+=Math.max(0,s.getMaxStackSize()-s.getCount());}return room;}
        private boolean deposit(ItemStack stack){
            int old=stack.getCount();var refs=references();
            // Fill existing stacks first, then empty places. Catalog filters apply to their physical repositories.
            for(boolean empty:new boolean[]{false,true})for(var ref:refs){if(stack.isEmpty())return true;if(!group.isEmpty()&&!group.equals(label(ref.container)))continue;var current=ref.stack();if(empty!=current.isEmpty()||!ref.accepts(stack))continue;
                if(ref.container instanceof NouveauWorkshop b&&!b.accepts(stack))continue;
                if(!current.isEmpty()&&!ItemStack.isSameItemSameComponents(current,stack))continue;
                int n=Math.min(stack.getCount(),Math.min(stack.getMaxStackSize(),ref.container.getMaxStackSize())-current.getCount());if(n<=0)continue;
                if(current.isEmpty())ref.container.setItem(ref.slot,stack.split(n));else{current.grow(n);stack.shrink(n);ref.container.setChanged();}
            }return old!=stack.getCount();
        }
        @Override public void clicked(int slot,int button,ContainerInput input,Player p){
            if(!stillValid(p))return;
            if(slot>=0&&slot<54){
                if(!(p instanceof ServerPlayer server)||!RateGate.allow(server,"nouveau_storage_transfer",1))return;
                if(input==ContainerInput.QUICK_MOVE){quickMoveStack(p,slot);return;}
                if(input!=ContainerInput.PICKUP||button<0||button>1)return;
                var carried=getCarried();if(!carried.isEmpty()){deposit(carried);if(carried.isEmpty())setCarried(ItemStack.EMPTY);}
                else {var selected=catalog.getItem(slot);if(!selected.isEmpty())setCarried(withdraw(selected,button==1?1:selected.getMaxStackSize()));}
                refresh();broadcastFullState();return;
            }super.clicked(slot,button,input,p);
        }
        @Override public boolean canTakeItemForPickAll(ItemStack stack,Slot slot){return slot.container!=catalog&&slot.container!=result&&super.canTakeItemForPickAll(stack,slot);}
        @Override public void slotsChanged(Container changed){
            if(!(inventory.player instanceof ServerPlayer p))return;var input=grid.asCraftInput();var recipe=p.level().getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING,input,p.level()).orElse(null);
            result.setRecipeUsed(recipe);result.setItem(0,recipe==null?ItemStack.EMPTY:recipe.value().assemble(input));broadcastChanges();
        }
        @Override public void broadcastChanges(){if(inventory.player instanceof ServerPlayer&&inventory.player.level().getGameTime()-refreshed>=10)refresh();super.broadcastChanges();}
        @Override public boolean stillValid(Player p){return p==inventory.player&&p.isAlive()&&!p.isSpectator()&&(!(p instanceof ServerPlayer sp)||lectern!=null&&!lectern.isRemoved()&&p.level().getBlockEntity(lectern.getBlockPos())==lectern&&p.distanceToSqr(Vec3.atCenterOf(lectern.getBlockPos()))<=64&&lectern.mayUse(sp));}
        @Override public boolean clickMenuButton(Player p,int button){if(!(p instanceof ServerPlayer server)||!stillValid(p)||!getCarried().isEmpty()||!RateGate.allow(server,"nouveau_storage_page",3))return false;
            if(button==0)page--;else if(button==1)page++;else if(button==2){for(int i=0;i<36;i++){var stack=inventory.getItem(i);if(!stack.isEmpty())deposit(stack);}inventory.setChanged();}else if(button==3){var groups=groups();group=groups.get(Math.floorMod(groups.indexOf(group)+1,groups.size()));page=0;}else return false;refresh();broadcastFullState();return true;
        }
        @Override public ItemStack quickMoveStack(Player p,int index){
            if(!(p instanceof ServerPlayer)||!stillValid(p)||index<0||index>=slots.size())return ItemStack.EMPTY;
            if(index<GRID){var item=catalog.getItem(index);if(item.isEmpty())return ItemStack.EMPTY;var taken=withdraw(item,Math.min(item.getMaxStackSize(),room(item)));var copy=taken.copy();inventory.add(taken);if(!taken.isEmpty())inventory.placeItemBackInInventory(taken,net.minecraft.util.Prediction.SERVER_ONLY);refresh();broadcastFullState();return copy;}
            var slot=slots.get(index);var stack=slot.getItem();if(stack.isEmpty())return ItemStack.EMPTY;var copy=stack.copy();
            if(index>=PLAYER){if(!deposit(stack)&&!moveItemStackTo(stack,GRID,RESULT,false))return ItemStack.EMPTY;}
            else {if(index==RESULT&&room(stack)<stack.getCount())return ItemStack.EMPTY;if(!moveItemStackTo(stack,PLAYER,slots.size(),true))return ItemStack.EMPTY;}
            if(stack.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();slot.onTake(p,copy);if(index==RESULT)slotsChanged(grid);return copy;
        }
        @Override public void removed(Player p){super.removed(p);if(p instanceof ServerPlayer)clearContainer(p,grid);result.clearContent();}
    }
}
