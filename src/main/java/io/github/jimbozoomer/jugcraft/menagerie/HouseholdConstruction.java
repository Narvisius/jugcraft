package io.github.jimbozoomer.jugcraft.menagerie;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

/** Inventory-backed furniture construction. Catalogue stacks never leave the server-owned menu. */
public final class HouseholdConstruction implements Recipe<SingleRecipeInput> {
    public record Material(Ingredient ingredient,int count){
        static final Codec<Material> CODEC=RecordCodecBuilder.create(i->i.group(Ingredient.CODEC.fieldOf("ingredient").forGetter(Material::ingredient),Codec.intRange(1,64).fieldOf("count").forGetter(Material::count)).apply(i,Material::new));
        static final StreamCodec<RegistryFriendlyByteBuf,Material> STREAM=StreamCodec.composite(Ingredient.CONTENTS_STREAM_CODEC,Material::ingredient,ByteBufCodecs.VAR_INT,Material::count,Material::new);
    }
    public static RecipeType<HouseholdConstruction> TYPE;
    private static RecipeSerializer<HouseholdConstruction> SERIALIZER;
    private final Recipe.CommonInfo info;
    private final List<Material> materials;
    private final ItemStackTemplate result;
    private HouseholdConstruction(Recipe.CommonInfo i,List<Material> m,ItemStackTemplate r){info=i;materials=List.copyOf(m);result=r;}
    static void register(){
        TYPE=Registry.register(BuiltInRegistries.RECIPE_TYPE,Jugcraft.id("household_construction"),new RecipeType<HouseholdConstruction>(){});
        var codec=RecordCodecBuilder.<HouseholdConstruction>mapCodec(i->i.group(Recipe.CommonInfo.MAP_CODEC.forGetter(r->r.info),Material.CODEC.listOf().fieldOf("ingredients").forGetter(r->r.materials),ItemStackTemplate.CODEC.fieldOf("result").forGetter(r->r.result)).apply(i,HouseholdConstruction::new));
        var stream=StreamCodec.composite(Recipe.CommonInfo.STREAM_CODEC,(HouseholdConstruction r)->r.info,Material.STREAM.apply(ByteBufCodecs.list()),r->r.materials,ItemStackTemplate.STREAM_CODEC,r->r.result,HouseholdConstruction::new);
        SERIALIZER=Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,Jugcraft.id("household_construction"),new RecipeSerializer<>(codec,stream));
    }
    @Override public boolean matches(SingleRecipeInput in,Level l){return false;}
    @Override public ItemStack assemble(SingleRecipeInput in){return result.create();}
    @Override public boolean showNotification(){return info.showNotification();}
    @Override public String group(){return "";}
    @Override public RecipeSerializer<HouseholdConstruction> getSerializer(){return SERIALIZER;}
    @Override public RecipeType<HouseholdConstruction> getType(){return TYPE;}
    @Override public PlacementInfo placementInfo(){return PlacementInfo.NOT_PLACEABLE;}
    // The inventory-reserving workbench owns these recipes; vanilla grid placement cannot represent their counts.
    @Override public boolean isSpecial(){return true;}
    @Override public RecipeBookCategory recipeBookCategory(){return RecipeBookCategories.CRAFTING_MISC;}

    /** Bounded integral max-flow handles overlapping tags without double-spending an inventory stack. */
    public int[] reservation(Inventory inventory){
        if(materials.isEmpty()||materials.size()>16)return null;
        int sink=37+materials.size(),nodes=sink+1,needed=0;int[][] capacity=new int[nodes][nodes],flow=new int[nodes][nodes];
        for(int s=0;s<36;s++){var stack=inventory.getItem(s);capacity[0][1+s]=stack.getCount();for(int m=0;m<materials.size();m++)if(materials.get(m).ingredient.test(stack))capacity[1+s][37+m]=64;}
        for(int m=0;m<materials.size();m++){capacity[37+m][sink]=materials.get(m).count;needed+=materials.get(m).count;}
        int total=0;
        while(total<needed){
            int[] prev=new int[nodes];Arrays.fill(prev,-1);prev[0]=0;var queue=new ArrayDeque<Integer>();queue.add(0);
            while(!queue.isEmpty()&&prev[sink]<0){int u=queue.remove();for(int v=1;v<nodes;v++)if(prev[v]<0&&capacity[u][v]>flow[u][v]){prev[v]=u;queue.add(v);}}
            if(prev[sink]<0)return null;int amount=needed-total;
            for(int v=sink;v!=0;v=prev[v])amount=Math.min(amount,capacity[prev[v]][v]-flow[prev[v]][v]);
            for(int v=sink;v!=0;v=prev[v]){flow[prev[v]][v]+=amount;flow[v][prev[v]]-=amount;}total+=amount;
        }
        int[] take=new int[36];for(int i=0;i<36;i++)take[i]=flow[0][i+1];return take;
    }
    public boolean craft(ServerPlayer player){
        var inv=player.getInventory();var take=reservation(inv);if(take==null)return false;
        // Stage the entire transaction, including container remainders, before mutating the player's inventory.
        var staged=new SimpleContainer(36);var products=new ArrayList<ItemStack>();products.add(result.create());
        for(int i=0;i<36;i++){
            var s=inv.getItem(i);var remainder=s.getItem().getCraftingRemainder();
            if(take[i]>0&&remainder!=null&&!remainder.create().isEmpty())for(int n=0;n<take[i];n++)products.add(remainder.create());
            var copy=s.copy();copy.shrink(take[i]);staged.setItem(i,copy);
        }
        for(var product:products)if(!staged.addItem(product).isEmpty())return false;
        for(int i=0;i<36;i++)inv.setItem(i,staged.getItem(i));inv.setChanged();return true;
    }
    static void open(ServerPlayer p,BlockPos pos){p.openMenu(new SimpleMenuProvider((id,inv,player)->new Catalogue(id,inv,pos),Component.literal("Furniture workbench — click to build")));}
    private static final class Catalogue extends ChestMenu {
        private final BlockPos pos;
        private final Player owner;
        private final SimpleContainer samples;
        private final List<HouseholdConstruction> recipes;
        private int page;
        Catalogue(int id,Inventory inv,BlockPos p){this(id,inv,p,new SimpleContainer(54));}
        private Catalogue(int id,Inventory inv,BlockPos p,SimpleContainer samples){
            super(MenuType.GENERIC_9x6,id,inv,samples,6);pos=p.immutable();owner=inv.player;this.samples=samples;
            recipes=((ServerPlayer)owner).level().getServer().getRecipeManager().getRecipes().stream().map(RecipeHolder::value).filter(r->r instanceof HouseholdConstruction).map(r->(HouseholdConstruction)r).sorted(Comparator.comparing(r->r.result.create().getHoverName().getString())).toList();refresh();
        }
        private void refresh(){
            samples.clearContent();
            for(int i=0;i<45&&page*45+i<recipes.size();i++){
                var recipe=recipes.get(page*45+i);var icon=recipe.result.create();var lore=new ArrayList<Component>();
                for(var part:recipe.materials){String material=part.ingredient.items().limit(3).map(h->new ItemStack(h.value()).getHoverName().getString()).reduce((a,b)->a+" / "+b).orElse("Ingredient");lore.add(Component.literal(part.count+" × "+material));}
                lore.add(Component.literal("Click to build from your inventory"));icon.set(DataComponents.LORE,new ItemLore(lore));samples.setItem(i,icon);
            }
            samples.setItem(45,label(Items.ARROW,"Previous page"));samples.setItem(49,label(Items.CRAFTING_TABLE,"Page "+(page+1)+" / "+Math.max(1,(recipes.size()+44)/45)));samples.setItem(53,label(Items.ARROW,"Next page"));
        }
        private static ItemStack label(Item item,String text){var stack=new ItemStack(item);stack.set(DataComponents.CUSTOM_NAME,Component.literal(text));return stack;}
        @Override public boolean stillValid(Player p){return p==owner&&p.distanceToSqr(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5)<=64&&p.level().getBlockState(pos).is(Household.BLOCKS.get("household_workbench"))&&Menagerie.mayChange(p.level(),p,pos);}
        @Override public void clicked(int slot,int button,ContainerInput input,Player player){
            if(!(player instanceof ServerPlayer p)||!stillValid(p)||!RateGate.allow(p,"household_build",2))return;
            if(slot>=54){super.clicked(slot,button,input,player);return;}
            if(input!=ContainerInput.PICKUP||button!=0)return;
            if(slot==45)page=Math.max(0,page-1);else if(slot==53)page=Math.min(Math.max(0,(recipes.size()-1)/45),page+1);
            else if(slot>=0&&slot<45&&page*45+slot<recipes.size()&&!recipes.get(page*45+slot).craft(p))p.sendOverlayMessage(Component.literal("Bring the listed materials and leave room for the finished furniture."));
            refresh();broadcastChanges();
        }
        @Override public ItemStack quickMoveStack(Player p,int index){return ItemStack.EMPTY;}
        @Override public boolean canTakeItemForPickAll(ItemStack s,Slot slot){return false;}
    }
}
