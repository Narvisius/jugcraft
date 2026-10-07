package io.github.jimbozoomer.jugcraft.creatures.scary;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.resources.*;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;

public final class ScaryMod implements ModInitializer {
    public static Identifier id(String path) { return Identifier.fromNamespaceAndPath("jugcraft", path); }
    public static final EntityType<SpaceKookEntity> SPACE_KOOK = Registry.register(BuiltInRegistries.ENTITY_TYPE, id("space_kook"),
        EntityType.Builder.of(SpaceKookEntity::new, MobCategory.MONSTER).sized(1.0F, 2.375F).eyeHeight(2.05F)
            .clientTrackingRange(8).build(ResourceKey.create(Registries.ENTITY_TYPE, id("space_kook"))));
    public static final Item SPAWN_EGG = Registry.register(BuiltInRegistries.ITEM, id("space_kook_spawn_egg"),
        new SpawnEggItem(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id("space_kook_spawn_egg"))).spawnEgg(SPACE_KOOK)));
    public static final SoundEvent LAUGH = sound("space_kook.laugh"), HURT = sound("space_kook.hurt"),
        DEATH = sound("space_kook.death"), STEP = sound("space_kook.step");
    public static final EntityType<CaptainCutlerEntity> CAPTAIN_CUTLER = Registry.register(BuiltInRegistries.ENTITY_TYPE, id("captain_cutler"),
        EntityType.Builder.of(CaptainCutlerEntity::new, MobCategory.MONSTER).sized(.6F, 1.95F).eyeHeight(1.74F)
            .clientTrackingRange(8).build(ResourceKey.create(Registries.ENTITY_TYPE, id("captain_cutler"))));
    public static final Item CUTLER_SPAWN_EGG = Registry.register(BuiltInRegistries.ITEM, id("captain_cutler_spawn_egg"),
        new SpawnEggItem(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id("captain_cutler_spawn_egg"))).spawnEgg(CAPTAIN_CUTLER)));
    public static final SoundEvent CUTLER_AMBIENT = sound("captain_cutler.ambient"), CUTLER_CHASE = sound("captain_cutler.chase"),
        CUTLER_HURT = sound("captain_cutler.hurt"), CUTLER_DEATH = sound("captain_cutler.death"), CUTLER_STEP = sound("captain_cutler.step");
    public static final EntityType<PhantomShadowEntity> PHANTOM_SHADOW = Registry.register(BuiltInRegistries.ENTITY_TYPE, id("phantom_shadow"),
        EntityType.Builder.of(PhantomShadowEntity::new, MobCategory.MONSTER).sized(.85F,2.1F).eyeHeight(1.8F)
            .clientTrackingRange(10).build(ResourceKey.create(Registries.ENTITY_TYPE,id("phantom_shadow"))));
    public static final Item SHADOW_SPAWN_EGG=Registry.register(BuiltInRegistries.ITEM,id("phantom_shadow_spawn_egg"),
        new SpawnEggItem(new Item.Properties().setId(ResourceKey.create(Registries.ITEM,id("phantom_shadow_spawn_egg"))).spawnEgg(PHANTOM_SHADOW)));
    public static final SoundEvent SHADOW_AMBIENT=sound("phantom_shadow.ambient"),SHADOW_CHASE=sound("phantom_shadow.chase"),
        SHADOW_ATTACK=sound("phantom_shadow.attack"),SHADOW_HURT=sound("phantom_shadow.hurt"),SHADOW_DEATH=sound("phantom_shadow.death"),SHADOW_CHAIN=sound("phantom_shadow.chain");
    public static final EntityType<BlackKnightEntity> BLACK_KNIGHT=Registry.register(BuiltInRegistries.ENTITY_TYPE,id("black_knight"),
        EntityType.Builder.of(BlackKnightEntity::new,MobCategory.MONSTER).sized(.8F,2.15F).eyeHeight(1.85F)
            .clientTrackingRange(8).build(ResourceKey.create(Registries.ENTITY_TYPE,id("black_knight"))));
    public static final Item KNIGHT_SPAWN_EGG=Registry.register(BuiltInRegistries.ITEM,id("black_knight_spawn_egg"),
        new SpawnEggItem(new Item.Properties().setId(ResourceKey.create(Registries.ITEM,id("black_knight_spawn_egg"))).spawnEgg(BLACK_KNIGHT)));
    public static final SoundEvent KNIGHT_AMBIENT=sound("black_knight.ambient"),KNIGHT_CHASE=sound("black_knight.chase"),
        KNIGHT_ATTACK=sound("black_knight.attack"),KNIGHT_HURT=sound("black_knight.hurt"),KNIGHT_DEATH=sound("black_knight.death"),KNIGHT_STEP=sound("black_knight.step");
    private static SoundEvent sound(String path) {
        return Registry.register(BuiltInRegistries.SOUND_EVENT, id(path), SoundEvent.createVariableRangeEvent(id(path)));
    }
    @Override public void onInitialize() {
        ScarySpawns.initialize();
        FabricDefaultAttributeRegistry.register(BLACK_KNIGHT,BlackKnightEntity.attributes());
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS).register(entries->entries.accept(KNIGHT_SPAWN_EGG));
        FabricDefaultAttributeRegistry.register(PHANTOM_SHADOW, PhantomShadowEntity.attributes());
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS).register(entries -> entries.accept(SHADOW_SPAWN_EGG));
        FabricDefaultAttributeRegistry.register(SPACE_KOOK, SpaceKookEntity.attributes());
        FabricDefaultAttributeRegistry.register(CAPTAIN_CUTLER, CaptainCutlerEntity.attributes());
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS).register(entries -> entries.accept(CUTLER_SPAWN_EGG));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS).register(entries -> entries.accept(SPAWN_EGG));
    }
}
