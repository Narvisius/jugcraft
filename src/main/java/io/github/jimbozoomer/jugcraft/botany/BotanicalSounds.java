package io.github.jimbozoomer.jugcraft.botany;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.backpack.OwnerBackpacks;
import java.util.*;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;

/** Original supplied audio; declarations do not imply every associated system is implemented. */
final class BotanicalSounds {
    private static final Map<String,SoundEvent> SOUNDS=new HashMap<>();
    static void register(){for(var name:OwnerBackpacks.source("newest_jugtania_sounds.json").getAsJsonArray("sounds")){
        var id=Jugcraft.id("jugtania_"+name.getAsString());SOUNDS.put(name.getAsString(),Registry.register(BuiltInRegistries.SOUND_EVENT,id,SoundEvent.createVariableRangeEvent(id)));
    }}
    static SoundEvent get(String name){return Objects.requireNonNull(SOUNDS.get(name),name);}
}
