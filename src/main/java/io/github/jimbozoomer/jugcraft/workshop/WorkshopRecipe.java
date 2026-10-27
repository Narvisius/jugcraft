package io.github.jimbozoomer.jugcraft.workshop;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.jimbozoomer.jugcraft.machine.MachineInput;
import io.github.jimbozoomer.jugcraft.machine.MultiMachineRecipe;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

/** Starter-only recipe family. Generated manual/alloy receipts reuse the powered recipe source. */
public record WorkshopRecipe(Recipe.CommonInfo commonInfo, int station, List<MultiMachineRecipe.Part> parts,
        ItemStackTemplate result, int time, Optional<ItemStackTemplate> tool, boolean heated, boolean highHeat) implements Recipe<MachineInput> {
    public WorkshopRecipe {
        parts = List.copyOf(parts);
        if (highHeat && (station != 2 || !heated)) throw new IllegalArgumentException("High heat needs the heated crucible");
        if (station < 0 || station > 2 || parts.isEmpty() || parts.size() > 3 || time < 1 || time > 32767)
            throw new IllegalArgumentException("Invalid starter workshop recipe");
    }
    public static final MapCodec<WorkshopRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        Recipe.CommonInfo.MAP_CODEC.forGetter(WorkshopRecipe::commonInfo),
        Codec.intRange(0, 2).fieldOf("station").forGetter(WorkshopRecipe::station),
        MultiMachineRecipe.Part.CODEC.listOf(1, 3).fieldOf("ingredients").forGetter(WorkshopRecipe::parts),
        ItemStackTemplate.CODEC.fieldOf("result").forGetter(WorkshopRecipe::result),
        Codec.intRange(1, 32767).fieldOf("time").forGetter(WorkshopRecipe::time),
        ItemStackTemplate.CODEC.optionalFieldOf("tool").forGetter(WorkshopRecipe::tool),
        Codec.BOOL.fieldOf("heated").forGetter(WorkshopRecipe::heated),
        Codec.BOOL.optionalFieldOf("high_heat", false).forGetter(WorkshopRecipe::highHeat)
    ).apply(i, WorkshopRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, WorkshopRecipe> STREAM_CODEC = new StreamCodec<>() {
        public WorkshopRecipe decode(RegistryFriendlyByteBuf b) {
            var info = Recipe.CommonInfo.STREAM_CODEC.decode(b);
            int station = b.readVarInt(), size = b.readVarInt();
            if (size < 1 || size > 3) throw new IllegalArgumentException("Invalid workshop ingredients");
            var parts = new java.util.ArrayList<MultiMachineRecipe.Part>();
            for (int i = 0; i < size; i++) parts.add(MultiMachineRecipe.Part.STREAM_CODEC.decode(b));
            return new WorkshopRecipe(info, station, parts, ItemStackTemplate.STREAM_CODEC.decode(b),
                b.readVarInt(), ByteBufCodecs.optional(ItemStackTemplate.STREAM_CODEC).decode(b), b.readBoolean(), b.readBoolean());
        }
        public void encode(RegistryFriendlyByteBuf b, WorkshopRecipe r) {
            Recipe.CommonInfo.STREAM_CODEC.encode(b, r.commonInfo());
            b.writeVarInt(r.station()); b.writeVarInt(r.parts().size());
            for (var part : r.parts()) MultiMachineRecipe.Part.STREAM_CODEC.encode(b, part);
            ItemStackTemplate.STREAM_CODEC.encode(b, r.result()); b.writeVarInt(r.time());
            ByteBufCodecs.optional(ItemStackTemplate.STREAM_CODEC).encode(b, r.tool()); b.writeBoolean(r.heated()); b.writeBoolean(r.highHeat());
        }
    };
    public int[] take(MachineInput input) { return MultiMachineRecipe.take(parts, input); }
    public ItemStack toolStack() { return tool.map(ItemStackTemplate::create).orElse(ItemStack.EMPTY); }
    public boolean matches(MachineInput input, Level level) { return take(input) != null; }
    public ItemStack assemble(MachineInput input) { return result.create(); }
    public boolean showNotification() { return commonInfo.showNotification(); }
    public String group() { return ""; }
    public RecipeSerializer<WorkshopRecipe> getSerializer() { return JugcraftWorkshop.SERIALIZER; }
    public RecipeType<WorkshopRecipe> getType() { return JugcraftWorkshop.RECIPES; }
    public PlacementInfo placementInfo() { return PlacementInfo.NOT_PLACEABLE; }
    public RecipeBookCategory recipeBookCategory() { return RecipeBookCategories.CRAFTING_MISC; }
}
