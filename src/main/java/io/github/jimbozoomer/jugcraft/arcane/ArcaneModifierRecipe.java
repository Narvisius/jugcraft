package io.github.jimbozoomer.jugcraft.arcane;

import com.mojang.serialization.*;
import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

/** Uses the normal smithing table, consuming template and source modifier exactly once. */
public final class ArcaneModifierRecipe extends SimpleSmithingRecipe {
    public static final MapCodec<ArcaneModifierRecipe> CODEC=Codec.STRING.fieldOf("modifier").xmap(ArcaneModifierRecipe::new,r->r.modifier);
    public static final StreamCodec<RegistryFriendlyByteBuf,ArcaneModifierRecipe> STREAM=ByteBufCodecs.STRING_UTF8.<RegistryFriendlyByteBuf>cast().map(ArcaneModifierRecipe::new,r->r.modifier);
    public static final RecipeSerializer<ArcaneModifierRecipe> SERIALIZER=new RecipeSerializer<>(CODEC,STREAM);
    private final String modifier;
    public ArcaneModifierRecipe(String modifier){super(new Recipe.CommonInfo(true));if(!ArcaneModifiers.ADDITIONS.containsKey(modifier))throw new IllegalArgumentException("Unknown arcane modifier");this.modifier=modifier;}
    @Override public Optional<Ingredient> templateIngredient(){return Optional.of(Ingredient.of(ForbiddenArcane.item("apply_modifier_smithing_template")));}
    @Override public Ingredient baseIngredient(){return Ingredient.of(BuiltInRegistries.ITEM.stream().filter(i->ArcaneModifiers.applicable(new ItemStack(i),modifier)));}
    @Override public Optional<Ingredient> additionIngredient(){return Optional.of(Ingredient.of(ForbiddenArcane.item(ArcaneModifiers.ADDITIONS.get(modifier))));}
    @Override public boolean matches(SmithingRecipeInput input,Level l){return templateIngredient().orElseThrow().test(input.template())&&additionIngredient().orElseThrow().test(input.addition())&&ArcaneModifiers.applicable(input.base(),modifier);}
    @Override public ItemStack assemble(SmithingRecipeInput input){return ArcaneModifiers.apply(input.base(),modifier);}
    @Override protected PlacementInfo createPlacementInfo(){return PlacementInfo.createFromOptionals(List.of(templateIngredient(),Optional.of(baseIngredient()),additionIngredient()));}
    @Override public RecipeSerializer<ArcaneModifierRecipe> getSerializer(){return SERIALIZER;}
}
