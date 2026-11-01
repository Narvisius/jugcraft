package io.github.jimbozoomer.jugcraft.aquatic;
import java.util.*;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.backpack.OwnerBackpacks;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
public final class OwnerAquaticSounds {
    private OwnerAquaticSounds(){}
    private static final Map<String,SoundEvent> SOUNDS=new HashMap<>();
    public static void register(){for(var e:OwnerBackpacks.source("newest_aquatic_sounds.json").getAsJsonArray("sounds")){String name=e.getAsString();var id=Jugcraft.id("aquatic_"+name);SOUNDS.put(name,Registry.register(BuiltInRegistries.SOUND_EVENT,id,SoundEvent.createVariableRangeEvent(id)));}}
    public static SoundEvent sound(String name){return SOUNDS.get(name);}
}
