package io.github.jimbozoomer.jugcraft.machine.form;

import io.github.jimbozoomer.jugcraft.chemistry.FluidRecipes;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

/** A bounded menu-opening snapshot of the server's actual recipes, including datapacks. */
public record FormRecipeCatalog(Identifier form, List<FluidRecipes.Entry> entries) {
    public static final int MAX_RECIPES = 256;
    public FormRecipeCatalog {
        entries = List.copyOf(entries);
        if (entries.size() > MAX_RECIPES) throw new IllegalArgumentException("Too many machine recipes");
    }
    public static final StreamCodec<RegistryFriendlyByteBuf, FormRecipeCatalog> STREAM_CODEC = new StreamCodec<>() {
        public FormRecipeCatalog decode(RegistryFriendlyByteBuf buf) {
            var id = Identifier.STREAM_CODEC.decode(buf);
            var form = JugcraftForms.form(id);
            if (form == null) throw new IllegalArgumentException("Unknown machine form");
            int size = buf.readVarInt();
            if (size < 0 || size > MAX_RECIPES) throw new IllegalArgumentException("Invalid recipe count");
            var entries = new ArrayList<FluidRecipes.Entry>();
            for (int i = 0; i < size; i++) {
                var recipeId = Identifier.STREAM_CODEC.decode(buf);
                var recipe = FluidRecipes.serializer(form.family()).streamCodec().decode(buf);
                entries.add(new FluidRecipes.Entry(recipeId, recipe));
            }
            return new FormRecipeCatalog(id, entries);
        }
        public void encode(RegistryFriendlyByteBuf buf, FormRecipeCatalog catalog) {
            Identifier.STREAM_CODEC.encode(buf, catalog.form());
            buf.writeVarInt(catalog.entries().size());
            for (var entry : catalog.entries()) {
                Identifier.STREAM_CODEC.encode(buf, entry.id());
                entry.recipe().getSerializer().streamCodec().encode(buf, entry.recipe());
            }
        }
    };
}
