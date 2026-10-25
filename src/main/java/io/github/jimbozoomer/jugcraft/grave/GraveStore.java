package io.github.jimbozoomer.jugcraft.grave;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.*;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.*;

/** Contents live in world saved data, never in a breakable marker or an entity that can despawn. */
public final class GraveStore extends SavedData {
    public record Grave(UUID id, UUID owner, GlobalPos location, List<ItemStack> items, boolean marked) {
        public static final Codec<Grave> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.CODEC.fieldOf("id").forGetter(Grave::id),
            UUIDUtil.CODEC.fieldOf("owner").forGetter(Grave::owner),
            GlobalPos.CODEC.fieldOf("location").forGetter(Grave::location),
            ItemStack.CODEC.listOf().fieldOf("items").forGetter(Grave::items),
            Codec.BOOL.optionalFieldOf("marked", false).forGetter(Grave::marked)).apply(i, Grave::new));
        public int count() { return items.stream().mapToInt(ItemStack::getCount).sum(); }
    }
    public static final Codec<GraveStore> CODEC = Grave.CODEC.listOf().fieldOf("graves")
        .xmap(GraveStore::new, s -> new ArrayList<>(s.graves.values())).codec();
    private static final SavedDataType<GraveStore> TYPE = new SavedDataType<>(Jugcraft.id("death_graves"), GraveStore::new, CODEC, null);
    private final Map<UUID, Grave> graves = new LinkedHashMap<>();
    public GraveStore() {}
    private GraveStore(List<Grave> records) { for (var record : records) graves.put(record.id(), record); }
    public static GraveStore get(MinecraftServer server) { return server.overworld().getDataStorage().computeIfAbsent(TYPE); }
    public List<Grave> owned(UUID owner) { return graves.values().stream().filter(g -> g.owner().equals(owner)).toList(); }
    public Grave at(GlobalPos location) { return graves.values().stream().filter(g -> g.location().equals(location)).findFirst().orElse(null); }
    public Grave get(UUID id) { return graves.get(id); }
    public void put(Grave grave) { graves.put(grave.id(), grave); setDirty(); }
    public void remove(UUID id) { if (graves.remove(id) != null) setDirty(); }
}
