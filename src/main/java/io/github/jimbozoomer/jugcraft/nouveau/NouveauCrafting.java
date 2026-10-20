package io.github.jimbozoomer.jugcraft.nouveau;

import java.util.*;
import net.minecraft.server.level.*;
import net.minecraft.world.Container;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;

/** Wixies reserve real inventory ingredients and return container remainders with the crafted result. */
public final class NouveauCrafting {
    private record Slot(Container container,int slot){}
    private static final Map<NouveauWorkshop,Integer> CURSORS=Collections.synchronizedMap(new WeakHashMap<>());
    private static List<RecipeHolder<?>> cached=List.of();
    public static void invalidate(){cached=List.of();CURSORS.clear();SAMPLES.clear();}
    private static final Map<NouveauWorkshop,Integer> SAMPLES=new WeakHashMap<>();
    private NouveauCrafting(){}
    private static boolean assign(List<Optional<Ingredient>> needs,int index,List<Slot> slots,int[] used,List<ItemStack> grid,int[] budget){
        if(--budget[0]<0)return false;if(index>=needs.size())return true;
        if(needs.get(index).isEmpty()){grid.set(index,ItemStack.EMPTY);return assign(needs,index+1,slots,used,grid,budget);}
        for(int s=0;s<slots.size();s++){var ref=slots.get(s);var stack=ref.container.getItem(ref.slot);if(stack.getCount()<=used[s]||!needs.get(index).get().test(stack))continue;used[s]++;grid.set(index,stack.copyWithCount(1));if(assign(needs,index+1,slots,used,grid,budget))return true;used[s]--;}
        return false;
    }
    public static boolean craftOne(ServerLevel l,ServerPlayer p,NouveauWorkshop home){
        List<ItemStack> samples=new ArrayList<>();if(!home.sample().isEmpty())samples.add(home.sample());
        for(var at:net.minecraft.core.BlockPos.betweenClosed(home.getBlockPos().offset(-3,0,-3),home.getBlockPos().offset(3,0,3)))if(l.hasChunkAt(at)&&l.getBlockEntity(at) instanceof NouveauWorkshop pedestal&&pedestal.pedestal()&&!l.hasNeighborSignal(at)&&pedestal.permitted(p)&&!pedestal.getItem(0).isEmpty()&&samples.size()<9)samples.add(pedestal.getItem(0));
        if(samples.isEmpty())return false;int sampleIndex=Math.floorMod(SAMPLES.getOrDefault(home,0),samples.size());ItemStack sample=samples.get(sampleIndex);
        List<Slot> slots=new ArrayList<>();for(int i=1;i<9;i++)slots.add(new Slot(home,i));
        for(var c:NouveauDevices.inventories(l,p,home))for(int i=0;i<Math.min(54,c.getContainerSize());i++)slots.add(new Slot(c,i));
        if(cached.isEmpty())cached=l.getServer().getRecipeManager().getRecipes().stream().filter(r->r.value() instanceof CraftingRecipe).toList();var recipes=cached;
        if(recipes.isEmpty())return false;int cursor=CURSORS.getOrDefault(home,0);
        // Bound expensive matching even on packs with thousands of recipes; resume next decision.
        for(int attempt=0;attempt<Math.min(64,recipes.size());attempt++){
            CraftingRecipe recipe=(CraftingRecipe)recipes.get(Math.floorMod(cursor++,recipes.size())).value();
            CURSORS.put(home,cursor%recipes.size());var placement=recipe.placementInfo();
            if(placement.isImpossibleToPlace()||placement.ingredients().size()>9)continue;
            List<Optional<Ingredient>> needs;
            int width,height;
            if(recipe instanceof ShapedRecipe shaped){needs=shaped.getIngredients();width=shaped.getWidth();height=shaped.getHeight();}
            else {needs=placement.ingredients().stream().map(Optional::of).toList();width=3;height=(needs.size()+2)/3;}
            if(width<1||height<1||width>3||height>3)continue;
            int[] used=new int[slots.size()];List<ItemStack> grid=new ArrayList<>(Collections.nCopies(width*height,ItemStack.EMPTY));
            if(!assign(needs,0,slots,used,grid,new int[]{4096}))continue;CraftingInput input=CraftingInput.of(width,height,grid);if(!recipe.matches(input,l))continue;
            ItemStack output=recipe.assemble(input);if(!ItemStack.isSameItemSameComponents(output,sample))continue;
            List<ItemStack> results=new ArrayList<>();results.add(output);results.addAll(recipe.getRemainingItems(input));
            List<ItemStack> destination=new ArrayList<>();for(int i=9;i<18;i++)destination.add(home.getItem(i).copy());
            if(!insertAll(destination,results))return false;
            for(int i=0;i<slots.size();i++)if(used[i]>0){var ref=slots.get(i);ref.container.removeItem(ref.slot,used[i]);ref.container.setChanged();}
            for(int i=0;i<9;i++)home.setItem(i+9,destination.get(i));SAMPLES.put(home,(sampleIndex+1)%samples.size());return true;
        }SAMPLES.put(home,(sampleIndex+1)%samples.size());return false;
    }
    /** Reserve an entire deposit without bypassing slot rules or mutating a rejected destination. */
    public static boolean deposit(Container container,ItemStack value,boolean simulate){
        if(container instanceof NouveauWorkshop workshop&&!workshop.accepts(value))return false;
        var slots=new ArrayList<Integer>();var copies=new ArrayList<ItemStack>();
        int[] available=container instanceof net.minecraft.world.WorldlyContainer sided?sided.getSlotsForFace(net.minecraft.core.Direction.UP):java.util.stream.IntStream.range(0,Math.min(1728,container.getContainerSize())).toArray();
        for(int i:available)if(i>=0&&i<container.getContainerSize()&&container.canPlaceItem(i,value)&&(!(container instanceof net.minecraft.world.WorldlyContainer sided)||sided.canPlaceItemThroughFace(i,value,net.minecraft.core.Direction.UP))){slots.add(i);copies.add(container.getItem(i).copy());}
        if(!insertAll(copies,List.of(value)))return false;
        if(!simulate){for(int i=0;i<slots.size();i++)container.setItem(slots.get(i),copies.get(i));container.setChanged();}return true;
    }
    public static boolean insertAll(List<ItemStack> target,List<ItemStack> additions){
        for(var addition:additions){var rest=addition.copy();
            for(var stack:target)if(!rest.isEmpty()&&ItemStack.isSameItemSameComponents(stack,rest)){
                int amount=Math.min(rest.getCount(),stack.getMaxStackSize()-stack.getCount());stack.grow(amount);rest.shrink(amount);
            }
            for(int i=0;i<target.size()&&!rest.isEmpty();i++)if(target.get(i).isEmpty())target.set(i,rest.split(Math.min(rest.getCount(),rest.getMaxStackSize())));
            if(!rest.isEmpty())return false;
        }return true;
    }
}
