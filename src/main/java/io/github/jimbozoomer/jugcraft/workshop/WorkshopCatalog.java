package io.github.jimbozoomer.jugcraft.workshop;

import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;

/** Server-owned, bounded recipe-picker snapshot; reload invalidates already open menus. */
public record WorkshopCatalog(int station, List<Entry> entries) {
    public record Entry(Identifier id, WorkshopRecipe recipe) { }
    private static final Map<Integer, WorkshopCatalog> CACHE = new HashMap<>();
    public WorkshopCatalog { entries = List.copyOf(entries); }
    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(s -> CACHE.clear());
        ServerLifecycleEvents.SERVER_STOPPED.register(s -> CACHE.clear());
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((s, r, ok) -> CACHE.clear());
    }
    public static WorkshopCatalog get(MinecraftServer server, int station) {
        return CACHE.computeIfAbsent(station, k -> {
            var rows = new ArrayList<Entry>();
            for (var holder : server.getRecipeManager().getRecipes())
                if (holder.value() instanceof WorkshopRecipe r && r.station() == station)
                    rows.add(new Entry(holder.id().identifier(), r));
            rows.sort(Comparator.comparing(e -> e.id().toString()));
            return new WorkshopCatalog(station, rows.stream().limit(128).toList());
        });
    }
    public int index(Identifier id) {
        for (int i = 0; i < entries.size(); i++) if (entries.get(i).id().equals(id)) return i;
        return -1;
    }
    public static final StreamCodec<RegistryFriendlyByteBuf, WorkshopCatalog> STREAM_CODEC = new StreamCodec<>() {
        public WorkshopCatalog decode(RegistryFriendlyByteBuf b) {
            int station = b.readVarInt(), size = b.readVarInt();
            if (station < 0 || station > 2 || size < 0 || size > 128) throw new IllegalArgumentException("Invalid workshop catalog");
            var rows = new ArrayList<Entry>();
            for (int i = 0; i < size; i++) rows.add(new Entry(Identifier.STREAM_CODEC.decode(b), WorkshopRecipe.STREAM_CODEC.decode(b)));
            return new WorkshopCatalog(station, rows);
        }
        public void encode(RegistryFriendlyByteBuf b, WorkshopCatalog c) {
            b.writeVarInt(c.station()); b.writeVarInt(c.entries.size());
            for (var e : c.entries) { Identifier.STREAM_CODEC.encode(b, e.id()); WorkshopRecipe.STREAM_CODEC.encode(b, e.recipe()); }
        }
    };
}
