package io.github.jimbozoomer.jugcraft.pets;

import com.mojang.serialization.Codec;
import java.util.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.*;
import io.github.jimbozoomer.jugcraft.Jugcraft;

/** World-owned records survive owner death and dimension changes. Entity data is never sent to clients. */
public final class PetRoster extends SavedData {
    public final Map<UUID, CompoundTag> entries = new LinkedHashMap<>();
    public static final Codec<PetRoster> CODEC = CompoundTag.CODEC.listOf().fieldOf("pets").codec().xmap(tags -> {
        var result = new PetRoster();
        for (var tag : tags) try { result.entries.put(UUID.fromString(tag.getStringOr("id", "")), tag.copy()); }
        catch (IllegalArgumentException ignored) { /* Invalid records cannot claim an entity. */ }
        return result;
    }, roster -> roster.entries.values().stream().map(CompoundTag::copy).toList());
    private static final SavedDataType<PetRoster> TYPE = new SavedDataType<>(Jugcraft.id("pet_roster"), PetRoster::new, CODEC, null);
    public static PetRoster get(MinecraftServer server) { return server.overworld().getDataStorage().computeIfAbsent(TYPE); }
    public static int level(CompoundTag entry) { return Math.min(20, 1 + entry.getIntOr("xp", 0) / 120); }
}
