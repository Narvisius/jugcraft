package io.github.jimbozoomer.jugcraft.arcane;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.*;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.vehicle.boat.*;
import net.minecraft.world.item.*;

/** Native steering, paddle animation, passengers and persistent chest cargo. */
public final class ArcaneBoats {
    public static final Map<String,EntityType<Boat>> BOATS=new LinkedHashMap<>();
    public static final Map<String,EntityType<ChestBoat>> CHESTS=new LinkedHashMap<>();
    static Item item(String name,Item.Properties properties){
        var key=ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id("forbidden_"+name));
        if(name.contains("chest")){var type=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.<ChestBoat>of((t,l)->new ChestBoat(t,l,()->ForbiddenArcane.item(name)),MobCategory.MISC).sized(1.375F,.5625F).clientTrackingRange(10).build(key));CHESTS.put(name.substring(0,name.indexOf('_')),type);return new BoatItem(type,properties.stacksTo(1));}
        var type=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.<Boat>of((t,l)->new Boat(t,l,()->ForbiddenArcane.item(name)),MobCategory.MISC).sized(1.375F,.5625F).clientTrackingRange(10).build(key));BOATS.put(name.substring(0,name.indexOf('_')),type);return new BoatItem(type,properties.stacksTo(1));
    }
}
