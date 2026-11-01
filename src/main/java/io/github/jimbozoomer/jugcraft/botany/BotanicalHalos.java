package io.github.jimbozoomer.jugcraft.botany;

import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.crafting.CraftingChoices;
import io.github.jimbozoomer.jugcraft.jimtafacts.network.DigitalPatterns.Pattern;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;

/** Eleven actual recipe bookmarks and material-conserving portable crafting. */
public final class BotanicalHalos extends Item {
    public static MenuType<Menu> MENU;
    public static DataComponentType<List<Bookmark>> BOOKMARKS;
    public static DataComponentType<Pattern> LAST;
    public static DataComponentType<Boolean> ACTIVE;
    private final boolean automatic;
    BotanicalHalos(boolean automatic,Properties p){super(p.stacksTo(1));this.automatic=automatic;}
    public record Bookmark(int slot,Pattern pattern){public static final Codec<Bookmark> CODEC=RecordCodecBuilder.create(i->i.group(Codec.intRange(0,10).fieldOf("slot").forGetter(Bookmark::slot),Pattern.CODEC.fieldOf("pattern").forGetter(Bookmark::pattern)).apply(i,Bookmark::new));}
    private static <T> DataComponentType<T> component(String name,Codec<T> codec){return Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,Jugcraft.id(name),DataComponentType.<T>builder().persistent(codec).networkSynchronized(ByteBufCodecs.fromCodecWithRegistries(codec)).build());}
    static void register(){BOOKMARKS=component("botanical_halo_bookmarks",Bookmark.CODEC.listOf(0,11));LAST=component("botanical_halo_last",Pattern.CODEC);ACTIVE=component("botanical_halo_active",Codec.BOOL);MENU=Registry.register(BuiltInRegistries.MENU,Jugcraft.id("botanical_assembly_halo"),new MenuType<>(Menu::new,FeatureFlags.VANILLA_SET));}
    @Override public InteractionResult use(Level level,Player user,InteractionHand hand){
        if(!(user instanceof ServerPlayer p))return InteractionResult.SUCCESS;if(p.isSpectator())return InteractionResult.FAIL;var halo=p.getItemInHand(hand);
        if(automatic&&p.isShiftKeyDown()){boolean enabled=!halo.getOrDefault(ACTIVE,false);if(enabled)halo.set(ACTIVE,true);else halo.remove(ACTIVE);p.sendOverlayMessage(Component.literal("Passive crafting "+(enabled?"on":"off")));return InteractionResult.SUCCESS;}
        p.openMenu(new SimpleMenuProvider((id,inventory,player)->new Menu(id,inventory,p,hand,halo),halo.getHoverName()));return InteractionResult.SUCCESS;
    }
    static void tick(ServerPlayer p,ItemStack halo){
        if(!(halo.getItem() instanceof BotanicalHalos h)||!h.automatic||!halo.getOrDefault(ACTIVE,false)||p.tickCount%20!=0||p.containerMenu!=p.inventoryMenu||p.getMainHandItem()==halo||p.getOffhandItem()==halo)return;
        // One bookmark attempt per second, rotating through all eleven; no recursive crafting loop.
        int slot=(p.tickCount/20)%11;for(var bookmark:halo.getOrDefault(BOOKMARKS,List.<Bookmark>of()))if(bookmark.slot==slot){craft(p,bookmark.pattern);break;}
    }
    private static boolean insert(List<ItemStack> inventory,ItemStack input){
        var pending=input.copy();for(var s:inventory)if(!s.isEmpty()&&ItemStack.isSameItemSameComponents(s,pending)){int n=Math.min(pending.getCount(),s.getMaxStackSize()-s.getCount());s.grow(n);pending.shrink(n);if(pending.isEmpty())return true;}
        for(int i=0;i<inventory.size();i++)if(inventory.get(i).isEmpty()){int n=Math.min(pending.getCount(),pending.getMaxStackSize());inventory.set(i,pending.copyWithCount(n));pending.shrink(n);if(pending.isEmpty())return true;}return pending.isEmpty();
    }
    private static boolean craft(ServerPlayer p,Pattern pattern){
        if(!p.isAlive()||p.isSpectator())return false;var recipe=pattern.recipe(p.level());if(recipe==null)return false;
        if(p.level().getGameRules().get(GameRules.LIMITED_CRAFTING)&&!recipe.value().isSpecial()&&!p.getRecipeBook().contains(recipe.id()))return false;
        var contents=new ArrayList<ItemStack>();for(int i=0;i<36;i++)contents.add(p.getInventory().getItem(i).copy());
        var input=selectInputs(p,recipe.value(),pattern,contents);if(input==null)return false;
        var output=recipe.value().assemble(input);if(output.isEmpty()||!output.isItemEnabled(p.level().enabledFeatures()))return false;
        if(!insert(contents,output))return false;for(var remainder:recipe.value().getRemainingItems(input))if(!remainder.isEmpty()&&!insert(contents,remainder))return false;
        // Only after all input/output/remainder checks pass do physical inventory slots change.
        for(int i=0;i<36;i++)if(!ItemStack.matches(contents.get(i),p.getInventory().getItem(i)))p.getInventory().setItem(i,contents.get(i));p.getInventory().setChanged();output.onCraftedBy(p,output.getCount());if(!recipe.value().isSpecial())p.awardRecipes(List.of(recipe));p.triggerRecipeCrafted(recipe,input.items());return true;
    }
    private record Need(int cell,Ingredient ingredient){}
    /** Only ordinary recipes allow tag substitutions. Special component-sensitive
     * recipes retain their exact saved inputs. All remainders come from real inputs. */
    private static CraftingInput selectInputs(ServerPlayer player,CraftingRecipe recipe,Pattern pattern,List<ItemStack> inventory){
        var needs=new ArrayList<Need>();var grid=new ArrayList<ItemStack>(Collections.nCopies(9,ItemStack.EMPTY));
        if(recipe instanceof ShapedRecipe shaped){
            var ingredients=shaped.getIngredients();
            for(int y=0;y<shaped.getHeight();y++)for(int x=0;x<shaped.getWidth();x++){
                var ingredient=ingredients.get(x+y*shaped.getWidth());if(ingredient.isPresent())needs.add(new Need(x+y*3,ingredient.get()));
            }
        }else if(recipe instanceof ShapelessRecipe){
            var ingredients=recipe.placementInfo().ingredients();if(ingredients.size()>9)return null;
            for(int i=0;i<ingredients.size();i++)needs.add(new Need(i,ingredients.get(i)));
        }else{
            var saved=pattern.grid();
            for(int cell=0;cell<9;cell++){
                var required=saved.get(cell);if(required.isEmpty())continue;if(required.getItem() instanceof BotanicalHalos)return null;
                boolean found=false;for(var available:inventory)if(!available.isEmpty()&&ItemStack.isSameItemSameComponents(available,required)){grid.set(cell,available.copyWithCount(1));available.shrink(1);found=true;break;}if(!found)return null;
            }
            var input=CraftingInput.of(3,3,grid);return recipe.matches(input,player.level())?input:null;
        }
        // Match scarce ingredients first; bounded backtracking handles overlapping
        // tags (e.g. one specific log plus any log) without consuming live items.
        needs.sort(Comparator.comparingInt(n->(int)inventory.stream().filter(s->!s.isEmpty()&&!(s.getItem() instanceof BotanicalHalos)&&n.ingredient.test(s)).count()));
        if(!assign(needs,0,inventory,grid,new int[]{1024}))return null;
        var input=CraftingInput.of(3,3,grid);
        return recipe.matches(input,player.level())&&ItemStack.isSameItemSameComponents(recipe.assemble(input),recipe.assemble(pattern.input()))?input:null;
    }
    private static boolean assign(List<Need> needs,int index,List<ItemStack> inventory,List<ItemStack> grid,int[] budget){
        if(index>=needs.size())return true;if(--budget[0]<0)return false;var need=needs.get(index);
        var seen=new ArrayList<ItemStack>();
        for(var available:inventory){
            if(available.isEmpty()||available.getItem() instanceof BotanicalHalos||!need.ingredient.test(available)||seen.stream().anyMatch(s->ItemStack.isSameItemSameComponents(s,available)))continue;
            var one=available.copyWithCount(1);seen.add(one);grid.set(need.cell,one);available.shrink(1);
            if(assign(needs,index+1,inventory,grid,budget))return true;
            available.grow(1);grid.set(need.cell,ItemStack.EMPTY);
        }return false;
    }
    public static final class Menu extends AbstractCraftingMenu {
        private final Player player;private final InteractionHand hand;private final ItemStack halo;private final SimpleContainer bookmarks=new SimpleContainer(11);
        public Menu(int id,Inventory inventory){this(id,inventory,inventory.player,null,ItemStack.EMPTY);}
        Menu(int id,Inventory inventory,Player player,InteractionHand hand,ItemStack halo){
            super(MENU,id,3,3);this.player=player;this.hand=hand;this.halo=halo;
            addSlot(new ResultSlot(player,craftSlots,resultSlots,0,124,35){@Override public void onTake(Player p,ItemStack output){remember();super.onTake(p,output);}});
            addCraftingGridSlots(30,17);
            for(int i=0;i<11;i++)addSlot(new Slot(bookmarks,i,190+i%2*20,18+i/2*20){@Override public boolean mayPlace(ItemStack s){return false;}@Override public boolean mayPickup(Player p){return false;}});
            for(int row=0;row<3;row++)for(int col=0;col<9;col++)inventorySlot(inventory,9+row*9+col,8+col*18,112+row*18);
            for(int col=0;col<9;col++)inventorySlot(inventory,col,8+col*18,170);
            refreshBookmarks();
        }
        private void inventorySlot(Inventory inventory,int i,int x,int y){addSlot(new Slot(inventory,i,x,y){@Override public boolean mayPickup(Player p){return hand==null||getItem()!=halo;}@Override public boolean mayPlace(ItemStack s){return hand==null||getItem()!=halo&&s!=halo;}});}
        private void remember(){if(!(player instanceof ServerPlayer p)||hand==null||!stillValid(p))return;
            var chosen=CraftingChoices.state(resultSlots).selected();if(chosen==null)return;var inputs=new ArrayList<ItemStack>();for(int i=0;i<9;i++){var item=craftSlots.getItem(i);if(item.getItem() instanceof BotanicalHalos)return;inputs.add(item.copyWithCount(item.isEmpty()?0:1));}
            halo.set(LAST,new Pattern(inputs,0,chosen.id().identifier().toString()));p.getInventory().setChanged();
        }
        private void refreshBookmarks(){if(!(player instanceof ServerPlayer p)||hand==null)return;bookmarks.clearContent();for(var b:halo.getOrDefault(BOOKMARKS,List.<Bookmark>of()))bookmarks.setItem(b.slot,b.pattern.output(p.level()));}
        @Override public void slotsChanged(Container c){if(player instanceof ServerPlayer p)CraftingChoices.update(this,p,craftSlots,resultSlots,null);}
        @Override protected void finishPlacingRecipe(net.minecraft.server.level.ServerLevel level,RecipeHolder<CraftingRecipe> r){if(player instanceof ServerPlayer p)CraftingChoices.update(this,p,craftSlots,resultSlots,r);}
        @Override public boolean stillValid(Player p){return p==player&&p.isAlive()&&(hand==null||p.getItemInHand(hand)==halo);}
        @Override public void clicked(int slot,int button,ContainerInput type,Player p){
            if(!stillValid(p))return;
            if(slot>=10&&slot<21){if(p instanceof ServerPlayer server){int index=slot-10;var entries=new ArrayList<>(halo.getOrDefault(BOOKMARKS,List.<Bookmark>of()));var saved=entries.stream().filter(b->b.slot==index).findFirst().orElse(null);
                if(type==ContainerInput.PICKUP&&button==1){entries.removeIf(b->b.slot==index);halo.set(BOOKMARKS,List.copyOf(entries));}
                else if(type==ContainerInput.QUICK_MOVE||type==ContainerInput.PICKUP&&button==0&&saved==null){var last=halo.get(LAST);if(last==null)server.sendOverlayMessage(Component.literal("Craft something in this halo first, then save it here."));else{entries.removeIf(b->b.slot==index);entries.add(new Bookmark(index,last));halo.set(BOOKMARKS,List.copyOf(entries));}}
                else if(type==ContainerInput.PICKUP&&button==0&&saved!=null&&!craft(server,saved.pattern))server.sendOverlayMessage(Component.literal("Needs recipe ingredients and room for the output and returned containers."));
                p.getInventory().setChanged();refreshBookmarks();broadcastChanges();}return;}
            if(hand!=null&&slot>=0&&slot<slots.size()&&slots.get(slot).getItem()==halo)return;
            if(hand!=null&&type==ContainerInput.SWAP&&(button==40&&hand==InteractionHand.OFF_HAND||button>=0&&button<9&&p.getInventory().getItem(button)==halo))return;
            super.clicked(slot,button,type,p);
        }
        @Override public ItemStack quickMoveStack(Player p,int index){
            if(!stillValid(p)||index<0||index>=slots.size()||index>=10&&index<21)return ItemStack.EMPTY;var slot=slots.get(index);if(!slot.hasItem()||!slot.mayPickup(p))return ItemStack.EMPTY;var stack=slot.getItem();var before=stack.copy();
            if(index==0){remember();if(!moveItemStackTo(stack,21,slots.size(),true))return ItemStack.EMPTY;slot.onQuickCraft(stack,before);}
            else if(index<10){if(!moveItemStackTo(stack,21,slots.size(),false))return ItemStack.EMPTY;}else if(!moveItemStackTo(stack,1,10,false))return ItemStack.EMPTY;
            if(stack.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();if(stack.getCount()==before.getCount())return ItemStack.EMPTY;slot.onTake(p,stack);return before;
        }
        @Override public void removed(Player p){super.removed(p);if(!p.level().isClientSide())clearContainer(p,craftSlots);}
        @Override public boolean canTakeItemForPickAll(ItemStack s,Slot slot){return slot.container!=bookmarks&&slot.container!=resultSlots&&super.canTakeItemForPickAll(s,slot);}
        @Override public Slot getResultSlot(){return slots.getFirst();}
        @Override public List<Slot> getInputGridSlots(){return slots.subList(1,10);}
        @Override public RecipeBookType getRecipeBookType(){return RecipeBookType.CRAFTING;}
        @Override protected Player owner(){return player;}
    }
    @Override public void appendHoverText(ItemStack s,TooltipContext c,net.minecraft.world.item.component.TooltipDisplay d,java.util.function.Consumer<Component> text,TooltipFlag f){text.accept(Component.literal("Portable crafting · 11 saved recipes"));text.accept(Component.literal("Accepts each recipe's material alternatives; special recipes keep exact saved ingredients."));if(automatic)text.accept(Component.literal("Sneak-use: passive crafting "+(s.getOrDefault(ACTIVE,false)?"on":"off")));}
}
