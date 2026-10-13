package local.peepo;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

public final class PeepoMod implements ModInitializer {
    public static Identifier id(String path) { return Identifier.fromNamespaceAndPath("peepo_companion", path); }
    public static final EntityType<PeepoEntity> PEEPO = Registry.register(BuiltInRegistries.ENTITY_TYPE, id("peepo"),
        EntityType.Builder.of(PeepoEntity::new, MobCategory.CREATURE).sized(.39F, .6F)
            .clientTrackingRange(8).build(ResourceKey.create(Registries.ENTITY_TYPE, id("peepo"))));
    public static final EntityType<PeepoEntity> JUGHEAD = Registry.register(BuiltInRegistries.ENTITY_TYPE, id("jughead"),
        EntityType.Builder.of(PeepoEntity::new, MobCategory.CREATURE).sized(.39F, .96F).eyeHeight(.51F)
            .clientTrackingRange(8).build(ResourceKey.create(Registries.ENTITY_TYPE, id("jughead"))));
    public static final Item JUGHEAD_SUMMONER = Registry.register(BuiltInRegistries.ITEM, id("jughead_summoner"),
        new PeepoSummoner(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id("jughead_summoner"))).stacksTo(16), true));
    // Read compatibility for worlds saved before the Jughead rename; omitted from Creative.
    public static final EntityType<PeepoEntity> LEGACY_JUGHEAD = Registry.register(BuiltInRegistries.ENTITY_TYPE, id("pink_peepo"),
        EntityType.Builder.of(PeepoEntity::new, MobCategory.CREATURE).sized(.39F, .96F).eyeHeight(.51F)
            .clientTrackingRange(8).build(ResourceKey.create(Registries.ENTITY_TYPE, id("pink_peepo"))));
    public static final Item LEGACY_JUGHEAD_SUMMONER = Registry.register(BuiltInRegistries.ITEM, id("pink_peepo_summoner"),
        new PeepoSummoner(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id("pink_peepo_summoner"))).stacksTo(16), true));
    public static final Item SUMMONER = Registry.register(BuiltInRegistries.ITEM, id("peepo_summoner"),
        new PeepoSummoner(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id("peepo_summoner"))).stacksTo(16)));
    @Override public void onInitialize() {
        CompanionBudget.initialize();
        CompanionLivestock.initialize();
        GeneratorWheel.initialize();
        CompanionBeds.initialize();
        CompanionStool.initialize();
        CompanionLunch.initialize();
        TikiTorch.initialize();
        CompanionMenu.initialize();
        AssignmentTool.initialize();
        MobTransportCrate.initialize();
        PeepoSpawns.initialize();
        FabricDefaultAttributeRegistry.register(PEEPO, PeepoEntity.attributes());
        FabricDefaultAttributeRegistry.register(JUGHEAD, PeepoEntity.attributes());
        FabricDefaultAttributeRegistry.register(LEGACY_JUGHEAD, PeepoEntity.attributes());
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS).register(entries -> { entries.accept(SUMMONER); entries.accept(JUGHEAD_SUMMONER); });
    }
}
