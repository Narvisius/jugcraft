package io.github.jimbozoomer.jugcraft.nouveau;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.item.BoatItem;
import net.minecraft.world.level.Level;

/** Native boat physics, inventory identity and paddle animation with the supplied archwood texture. */
public final class NouveauBoat extends Boat {
    public static EntityType<NouveauBoat> TYPE;
    public NouveauBoat(EntityType<? extends Boat> t,Level l){super(t,l,()->NouveauCatalog.item("archwood_boat"));}
    public static void register(){var key=ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id("nouveau_archwood_boat"));TYPE=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.<NouveauBoat>of(NouveauBoat::new,MobCategory.MISC).sized(1.375F,.5625F).clientTrackingRange(10).build(key));NouveauCatalog.registerItem(NouveauCatalog.ITEM_ENTRIES.get("archwood_boat"),p->new BoatItem(TYPE,p.stacksTo(1)));}
}
