package io.github.jimbozoomer.jugcraft.menagerie;

import com.google.gson.*;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.*;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.*;

/** Saved, hopper-compatible capsule conversion chamber. One amethyst powers 600 ticks. */
public final class BeastCapsid extends BaseContainerBlockEntity implements WorldlyContainer {
    public static BlockEntityType<BeastCapsid> TYPE;
    private record Conversion(String input,Item output,int count,int ticks){
        boolean matches(ItemStack s){return input.startsWith("#")?s.is(TagKey.create(Registries.ITEM,Identifier.parse(input.substring(1)))):s.is(BuiltInRegistries.ITEM.getValue(Identifier.parse(input)));}
    }
    private static final List<Conversion> RECIPES=new ArrayList<>();
    private NonNullList<ItemStack> items=NonNullList.withSize(3,ItemStack.EMPTY);
    private int fuel,progress,total=120;
    private String active="";
    private final ContainerData data=new ContainerData(){
        public int get(int i){return switch(i){case 0->fuel;case 1->600;case 2->progress;default->total;};}
        public void set(int i,int v){}public int getCount(){return 4;}
    };
    static void register(JsonArray recipes){
        for(var value:recipes){var d=value.getAsJsonObject();var ingredients=d.getAsJsonArray("ingredients");if(ingredients.size()!=1)throw new IllegalStateException("Capsid requires one input");
            var result=d.getAsJsonObject("result");RECIPES.add(new Conversion(ingredients.get(0).getAsString(),BuiltInRegistries.ITEM.getValue(Identifier.parse(result.get("id").getAsString())),result.has("count")?result.get("count").getAsInt():1,d.get("time").getAsInt()));
        }
        TYPE=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,Jugcraft.id("beast_capsid"),FabricBlockEntityTypeBuilder.create(BeastCapsid::new,Menagerie.BLOCKS.get("beast_capsid")).build());
    }
    BeastCapsid(BlockPos p,BlockState s){super(TYPE,p,s);}
    public static void tick(ServerLevel l,BlockPos p,BlockState s,BeastCapsid be){
        var recipe=RECIPES.stream().filter(r->r.matches(be.items.get(0))).findFirst().orElse(null);
        String id=recipe==null?"":recipe.input;boolean changed=false;
        if(!id.equals(be.active)){be.active=id;be.progress=0;changed=true;}
        if(recipe==null){if(changed)be.setChanged();return;}
        be.total=recipe.ticks;var output=be.items.get(2);var product=new ItemStack(recipe.output,recipe.count);
        if(!output.isEmpty()&&(!ItemStack.isSameItemSameComponents(output,product)||output.getCount()+product.getCount()>output.getMaxStackSize()))return;
        if(be.fuel==0&&be.items.get(1).is(Items.AMETHYST_SHARD)){be.items.get(1).shrink(1);be.fuel=600;changed=true;}
        if(be.fuel>0){be.fuel--;be.progress++;changed=true;
            if(be.progress>=be.total){be.progress=0;be.items.get(0).shrink(1);if(output.isEmpty())be.items.set(2,product);else output.grow(product.getCount());}
        }
        if(changed)be.setChanged();
    }
    @Override protected Component getDefaultName(){return Component.literal("Capsid conversion chamber");}
    @Override protected AbstractContainerMenu createMenu(int id,Inventory inv){return new HouseholdMenu(id,inv,this,data,2);}
    @Override protected NonNullList<ItemStack> getItems(){return items;}
    @Override protected void setItems(NonNullList<ItemStack> items){this.items=items;}
    @Override public int getContainerSize(){return 3;}
    @Override public boolean canPlaceItem(int i,ItemStack s){return i==0?RECIPES.stream().anyMatch(r->r.matches(s)):i==1&&s.is(Items.AMETHYST_SHARD);}
    @Override public int[] getSlotsForFace(Direction d){return d==Direction.DOWN?new int[]{2}:d==Direction.UP?new int[]{0}:new int[]{1};}
    @Override public boolean canPlaceItemThroughFace(int i,ItemStack s,Direction d){return canPlaceItem(i,s);}
    @Override public boolean canTakeItemThroughFace(int i,ItemStack s,Direction d){return i==2;}
    @Override protected void saveAdditional(ValueOutput o){super.saveAdditional(o);ContainerHelper.saveAllItems(o,items);o.putInt("Fuel",fuel);o.putInt("Progress",progress);o.putString("Active",active);}
    @Override protected void loadAdditional(ValueInput i){super.loadAdditional(i);items=NonNullList.withSize(3,ItemStack.EMPTY);ContainerHelper.loadAllItems(i,items);fuel=Math.clamp(i.getIntOr("Fuel",0),0,600);progress=Math.clamp(i.getIntOr("Progress",0),0,1200);active=i.getStringOr("Active","");}
}
