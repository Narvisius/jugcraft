package io.github.jimbozoomer.jugcraft.jimtafacts.network;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.Set;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

/** Changing a supported device's casing preserves its saved item components, including stored JE. */
public final class DigitalPartConversionRecipe extends CustomRecipe {
    private static final Set<String> KINDS=Set.of("energy_acceptor","interface","pattern_provider");
    private final String input,output;
    private static final MapCodec<DigitalPartConversionRecipe> CODEC=RecordCodecBuilder.mapCodec(i->i.group(Codec.STRING.fieldOf("input").forGetter(r->r.input),Codec.STRING.fieldOf("output").forGetter(r->r.output)).apply(i,DigitalPartConversionRecipe::new));
    public static final RecipeSerializer<DigitalPartConversionRecipe> SERIALIZER=new RecipeSerializer<>(CODEC,new StreamCodec<RegistryFriendlyByteBuf,DigitalPartConversionRecipe>(){
        public DigitalPartConversionRecipe decode(RegistryFriendlyByteBuf b){return new DigitalPartConversionRecipe(b.readUtf(),b.readUtf());}
        public void encode(RegistryFriendlyByteBuf b,DigitalPartConversionRecipe r){b.writeUtf(r.input);b.writeUtf(r.output);}
    });
    public DigitalPartConversionRecipe(String input,String output){
        if(!KINDS.stream().anyMatch(k->input.equals(k)&&output.equals("cable_"+k)||output.equals(k)&&input.equals("cable_"+k)))throw new IllegalArgumentException("Unsupported compact device conversion");
        this.input=input;this.output=output;
    }
    private ItemStack sample(CraftingInput in){
        ItemStack found=ItemStack.EMPTY;
        for(var stack:in.items())if(!stack.isEmpty()){
            if(!found.isEmpty()||!stack.is(BuiltInRegistries.ITEM.getValue(Jugcraft.id("jimta_"+input))))return ItemStack.EMPTY;
            found=stack;
        }
        return found;
    }
    @Override public boolean matches(CraftingInput in,Level level){return !sample(in).isEmpty();}
    @Override public ItemStack assemble(CraftingInput in){var source=sample(in);return source.isEmpty()?ItemStack.EMPTY:source.transmuteCopy(BuiltInRegistries.ITEM.getValue(Jugcraft.id("jimta_"+output)),1);}
    @Override public RecipeSerializer<DigitalPartConversionRecipe> getSerializer(){return SERIALIZER;}
}
