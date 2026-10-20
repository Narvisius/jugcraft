package io.github.jimbozoomer.jugcraft.menagerie;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.*;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;

/** Distinct data-pack cooking types retain the supplied times and prevent freezer/oven recipe crossover. */
public final class HouseholdCooking extends AbstractCookingRecipe {
    public static final Set<String> KINDS=Set.of("oven","freezer","pan","toaster","microwave");
    static final Map<String,RecipeType<HouseholdCooking>> TYPES=new LinkedHashMap<>();
    private static final Map<String,RecipeSerializer<HouseholdCooking>> SERIALIZERS=new LinkedHashMap<>();
    private final String appliance;
    private HouseholdCooking(String kind,Recipe.CommonInfo info,CookingBookInfo book,Ingredient ingredient,ItemStackTemplate result,float xp,int time){super(info,book,ingredient,result,xp,time);appliance=kind;}
    static void register(){
        for(String kind:KINDS){
            TYPES.put(kind,Registry.register(BuiltInRegistries.RECIPE_TYPE,Jugcraft.id("household_"+kind),new RecipeType<HouseholdCooking>(){}));
            AbstractCookingRecipe.Factory<HouseholdCooking> factory=(info,book,ingredient,result,xp,time)->new HouseholdCooking(kind,info,book,ingredient,result,xp,time);
            SERIALIZERS.put(kind,Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,Jugcraft.id("household_"+kind),new RecipeSerializer<>(cookingMapCodec(factory),cookingStreamCodec(factory))));
        }
    }
    @Override protected Item furnaceIcon(){return Items.FURNACE;}
    @Override public RecipeSerializer<HouseholdCooking> getSerializer(){return SERIALIZERS.get(appliance);}
    @Override public RecipeType<HouseholdCooking> getType(){return TYPES.get(appliance);}
    @Override public RecipeBookCategory recipeBookCategory(){return RecipeBookCategories.CRAFTING_MISC;}
}
