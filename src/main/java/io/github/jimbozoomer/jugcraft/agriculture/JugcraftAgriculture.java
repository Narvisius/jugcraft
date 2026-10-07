package io.github.jimbozoomer.jugcraft.agriculture;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.chemistry.FertilizerItem;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import io.github.jimbozoomer.jugcraft.energy.EnergyStorage;
import io.github.jimbozoomer.jugcraft.tools.JugcraftTools;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectionContext;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityType;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBiomeTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MinecartItem;
import net.minecraft.world.item.PlaceOnWaterBlockItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.GlowLichenBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.TallFlowerBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TintedParticleLeavesBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProvider;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Agriculture branch: crops, seeds, foods and farm tools (docs/branches/AGRICULTURE.md).
 * Keep the IDs and numbers here in sync with tools/agriculture.py; the checker compares them.
 * Everything is always registered; the {@value #FEATURE} switch only turns off recipes, wild
 * plants in new chunks and seeds from grass, so saved crops and items survive.
 */
public final class JugcraftAgriculture {
	public static final String FEATURE = "agriculture";
	/** Chance per short grass broken to drop one Jugcraft seed, chosen evenly: as often as vanilla wheat seeds. */
	public static final float GRASS_SEED_CHANCE = 0.125F;
	private static final List<String> GRASS_SEEDS = List.of("corn_kernels", "sunflower_seeds", "beans", "sweet_potato", "flax_seeds",
			"tomato_seeds", "pepper_seeds", "onion", "garlic", "cabbage_seeds", "oat_seeds", "barley_seeds",
			"butternut_squash_seeds", "acorn_squash_seeds", "warty_gourd_seeds", "turnip", "cranberries", "chestnut",
			"giant_pumpkin_seeds", "white_pumpkin_seeds", "jarrahdale_pumpkin_seeds", "cinderella_pumpkin_seeds", "red_kuri_pumpkin_seeds",
			"kabocha_pumpkin_seeds", "bottle_gourd_seeds",
			"ornamental_corn_kernels", "mandrake_root");
	/** The chestnut tree's feature (data/jugcraft/worldgen/feature/chestnut.json), grown by its sapling. */
	public static final ResourceKey<Feature> CHESTNUT_TREE = ResourceKey.create(Registries.FEATURE, Jugcraft.id("chestnut"));
	public static final TreeGrower CHESTNUT_GROWER = new TreeGrower(Jugcraft.MOD_ID + "_chestnut", WeightedList.of(CHESTNUT_TREE),
			WeightedList.of(), WeightedList.of(), CHESTNUT_TREE);
	/** The apple tree's feature (data/jugcraft/worldgen/feature/apple_tree.json), grown by its sapling. */
	public static final ResourceKey<Feature> APPLE_TREE = ResourceKey.create(Registries.FEATURE, Jugcraft.id("apple_tree"));
	public static final TreeGrower APPLE_GROWER = new TreeGrower(Jugcraft.MOD_ID + "_apple", WeightedList.of(APPLE_TREE),
			WeightedList.of(), WeightedList.of(), APPLE_TREE);
	/** Trees grown from their saplings (data/jugcraft/worldgen/feature/<tree>.json, tools/trees.py). */
	public static final TreeGrower LARCH_GROWER = grower("larch");
	public static final TreeGrower MAPLE_GROWER = grower("maple");
	public static final TreeGrower ASPEN_GROWER = grower("aspen");
	public static final TreeGrower FIR_GROWER = grower("fir");
	public static final TreeGrower JACARANDA_GROWER = grower("jacaranda");
	public static final TreeGrower WILLOW_GROWER = grower("willow");
	public static final TreeGrower PALM_GROWER = grower("palm");
	public static final TreeGrower CYPRESS_GROWER = grower("cypress");
	public static final TreeGrower REDWOOD_GROWER = grower("redwood");
	public static final TreeGrower EUCALYPTUS_GROWER = grower("eucalyptus");
	public static final TreeGrower MAHOGANY_GROWER = grower("mahogany");
	/** The tree roster's batch 1: the swamp cedar, on its own wood (tools/trees.py "cedar"). */
	public static final TreeGrower CEDAR_GROWER = grower("cedar");
	/** Giant trees, which four saplings in a square grow ({@link GiantSaplingBlock}; agriculture.TREES "giant"). */
	public static final TreeGrower GIANT_REDWOOD_GROWER = grower("giant_redwood");
	public static final TreeGrower GIANT_MAHOGANY_GROWER = grower("giant_mahogany");
	private static final Map<String, TreeGrower> GIANT_GROWERS = Map.of("redwood", GIANT_REDWOOD_GROWER, "mahogany", GIANT_MAHOGANY_GROWER);
	/** The dead tree, which no sapling grows; it stands in the Dead Forest (and game tests grow it). */
	public static final TreeGrower DEAD_TREE_GROWER = grower("dead_tree");
	/** Seasonal trees' leaf schedules, in season days. Keep in sync with TREES in tools/agriculture.py. */
	public static final SeasonalLeavesBlock.Schedule LARCH_LEAVES = new SeasonalLeavesBlock.Schedule(91, 268, 318);
	public static final SeasonalLeavesBlock.Schedule MAPLE_LEAVES = new SeasonalLeavesBlock.Schedule(95, 265, 310);
	public static final SeasonalLeavesBlock.Schedule ASPEN_LEAVES = new SeasonalLeavesBlock.Schedule(96, 258, 302);
	public static final SeasonalLeavesBlock.Schedule WILLOW_LEAVES = new SeasonalLeavesBlock.Schedule(88, 283, 328);

	private static final ResourceKey<ContextIntProvider> COMPOST_LOW = ContextIntProviders.COMPOSTABLE_LOW;
	private static final ResourceKey<ContextIntProvider> COMPOST_MEDIUM = ContextIntProviders.COMPOSTABLE_MEDIUM;
	private static final ResourceKey<ContextIntProvider> COMPOST_MEDIUM_HIGH = ContextIntProviders.COMPOSTABLE_MEDIUM_HIGH;

	public static final Map<TallCrop, TallCropBlock> TALL_CROPS = new EnumMap<>(TallCrop.class);
	private static final Map<String, Block> BLOCKS = new LinkedHashMap<>();
	private static final Map<String, Item> ITEMS = new LinkedHashMap<>();
	private static final List<Item> SEEDS_TAB = new ArrayList<>();
	private static final List<Item> FOOD_TAB = new ArrayList<>();
	private static final List<Item> INGREDIENT_TAB = new ArrayList<>();
	private static final List<Item> TOOL_TAB = new ArrayList<>();
	private static final List<Item> EQUIPMENT_TAB = new ArrayList<>();
	private static final List<Item> BUILDING_TAB = new ArrayList<>();

	public static RecipeType<CookingPotRecipe> POT_COOKING;
	public static RecipeSerializer<CookingPotRecipe> POT_SERIALIZER;
	public static BlockEntityType<CookingPotBlockEntity> COOKING_POT_ENTITY;
	public static BlockEntityType<CarvedPumpkinBlockEntity> CARVED_PUMPKIN_ENTITY;
	/** A hand-carved pumpkin's design, on its item (copied from and to the block entity). */
	public static DataComponentType<PumpkinCarving> CARVING;
	/** A Pumpkin Stencil's traced design (face 0). */
	public static DataComponentType<PumpkinCarving> STENCIL;
	/** Sips of water in a Gourd Canteen. */
	public static DataComponentType<Integer> CANTEEN_WATER;
	public static BlockEntityType<GiantPumpkinBlockEntity> GIANT_PUMPKIN_ENTITY;
	public static BlockEntityType<ScarecrowBlockEntity> SCARECROW_ENTITY;
	/** What a scarecrow wears for a head. */
	public static final TagKey<Item> SCARECROW_HEADS = TagKey.create(Registries.ITEM, Jugcraft.id("scarecrow_heads"));
	/** A giant pumpkin carried as an item: see {@link GiantPumpkinData}. */
	public static DataComponentType<GiantPumpkinData> GIANT_PUMPKIN;
	public static BlockEntityType<HarvestScaleBlockEntity> HARVEST_SCALE_ENTITY;
	/** A pumpkin boat's weight, torch and carving, on its item and its entity. */
	public static DataComponentType<PumpkinBoatData> PUMPKIN_BOAT;
	public static EntityType<PumpkinBoat> PUMPKIN_BARGE;
	public static EntityType<PumpkinBoat> PUMPKIN_RACER;
	public static BlockEntityType<RegattaFlagBlockEntity> REGATTA_FLAG_ENTITY;
	public static BlockEntityType<RegattaBuoyBlockEntity> REGATTA_BUOY_ENTITY;
	public static BlockEntityType<JudgingStandBlockEntity> JUDGING_STAND_ENTITY;
	public static BlockEntityType<GravestoneBlockEntity> GRAVESTONE_ENTITY;
	/** The graveyard pack: every headstone's block entity, the epitaph it carries as an item, and the chisel's wear. */
	public static BlockEntityType<HeadstoneBlockEntity> HEADSTONE_ENTITY;
	public static DataComponentType<Epitaph> EPITAPH;
	public static DataComponentType<List<Epitaph>> INSCRIPTIONS;
	public static BlockEntityType<HeadstoneBlockEntity> GRAVEYARD_BUILDING_ENTITY;
	public static final int CHISEL_USES = 250;
	public static DataComponentType<CandyBagItem.Night> CANDY_BAG_NIGHT;
	public static EntityType<WillOWisp> WILL_O_WISP;
	public static EntityType<FlyingPumpkin> FLYING_PUMPKIN;
	public static EntityType<BowlingPumpkin> BOWLING_PUMPKIN;
	public static EntityType<ToiletPaperRoll> TOILET_PAPER_ROLL;
	public static EntityType<HauntedHayride> HAUNTED_HAYRIDE;
	public static EntityType<ThrowMarker> THROW_MARKER;
	public static EntityType<HeadlessHorseman> HEADLESS_HORSEMAN;
	public static EntityType<FlamingPumpkin> FLAMING_PUMPKIN;
	public static BlockEntityType<TrebuchetBlockEntity> TREBUCHET_ENTITY;
	public static BlockEntityType<StringLightHookBlockEntity> STRING_LIGHT_HOOK_ENTITY;
	public static BlockEntityType<CandyBowlBlockEntity> CANDY_BOWL_ENTITY;
	public static BlockEntityType<CoffinBlockEntity> COFFIN_ENTITY;
	public static BlockEntityType<HauntedPortraitBlockEntity> HAUNTED_PORTRAIT_ENTITY;
	public static BlockEntityType<FogMachineBlockEntity> FOG_MACHINE_ENTITY;
	public static BlockEntityType<FloatingCandleBlockEntity> FLOATING_CANDLE_ENTITY;
	public static BlockEntityType<PumpkinCrateBlockEntity> PUMPKIN_CRATE_ENTITY;
	public static BlockEntityType<DecorationBlockEntity> ROCKING_CHAIR_ENTITY;
	public static BlockEntityType<DecorationBlockEntity> LURKING_EYES_ENTITY;
	public static BlockEntityType<DecorationBlockEntity> SILHOUETTE_WINDOW_ENTITY;
	public static BlockEntityType<MusicBoxBlockEntity> MUSIC_BOX_ENTITY;
	public static BlockEntityType<DecorationBlockEntity> GIANT_FAKE_SPIDER_ENTITY;
	public static BlockEntityType<DecorationBlockEntity> HAUNTED_CHANDELIER_ENTITY;
	public static BlockEntityType<PipeOrganBlockEntity> PIPE_ORGAN_ENTITY;
	public static BlockEntityType<DecorationBlockEntity> SUIT_OF_ARMOR_ENTITY;
	public static BlockEntityType<DustSheetBlockEntity> DUST_SHEET_ENTITY;
	public static BlockEntityType<DecorationBlockEntity> SPIRIT_MIRROR_ENTITY;
	public static BlockEntityType<DecorationBlockEntity> TATTERED_CURTAINS_ENTITY;
	public static BlockEntityType<DecorationBlockEntity> CREEPY_DOLL_ENTITY;
	public static BlockEntityType<TeslaCoilBlockEntity> TESLA_COIL_ENTITY;
	public static BlockEntityType<DecorationBlockEntity> LAB_TABLE_ENTITY;
	public static BlockEntityType<DecorationBlockEntity> SPECIMEN_JAR_ENTITY;
	public static BlockEntityType<DecorationBlockEntity> SARCOPHAGUS_ENTITY;
	public static BlockEntityType<DecorationBlockEntity> RAVEN_PERCH_ENTITY;
	public static BlockEntityType<BlackCatBlockEntity> BLACK_CAT_ENTITY;
	public static BlockEntityType<DecorationBlockEntity> INFLATABLE_ENTITY;
	public static BlockEntityType<PorchWitchBlockEntity> PORCH_WITCH_ENTITY;
	public static BlockEntityType<DecorationBlockEntity> WIND_CHIMES_ENTITY;
	public static BlockEntityType<DecorationBlockEntity> WEATHERVANE_ENTITY;
	public static BlockEntityType<SpookySignBlockEntity> SPOOKY_SIGN_ENTITY;
	public static BlockEntityType<BlackLightBlockEntity> BLACK_LIGHT_ENTITY;
	public static BlockEntityType<DecorationBlockEntity> GLOW_PAINT_ENTITY;
	public static BlockEntityType<DecorationBlockEntity> FLYING_EYEBALL_ENTITY;
	public static BlockEntityType<HornedSkullCauldronBlockEntity> HORNED_SKULL_CAULDRON_ENTITY;
	public static BlockEntityType<DecorationBlockEntity> CANDELABRUM_ENTITY;
	public static BlockEntityType<EnchantedBroomBlockEntity> ENCHANTED_BROOM_ENTITY;
	public static BlockEntityType<DustpanBlockEntity> DUSTPAN_ENTITY;
	public static BlockEntityType<ShowcaseBlockEntity> SHOWCASE_ENTITY;
	public static BlockEntityType<DecorationBlockEntity> MOTH_CASE_ENTITY;
	public static BlockEntityType<DecorationBlockEntity> ODDITY_JAR_ENTITY;
	public static BlockEntityType<DecorationBlockEntity> GIANT_HEART_ENTITY;
	public static BlockEntityType<DecorationBlockEntity> SPECIMEN_VESSEL_ENTITY;
	public static BlockEntityType<IronBoundCoffinBlockEntity> IRON_BOUND_COFFIN_ENTITY;
	public static BlockEntityType<CoffinWardrobeBlockEntity> COFFIN_WARDROBE_ENTITY;
	public static BlockEntityType<SarcophagusBlockEntity> SARCOPHAGUS_TOMB_ENTITY;
	public static BlockEntityType<ColossalSkullBlockEntity> COLOSSAL_SKULL_ENTITY;
	public static BlockEntityType<GargoyleSentinelBlockEntity> GARGOYLE_SENTINEL_ENTITY;
	public static BlockEntityType<GargoyleRainspoutBlockEntity> GARGOYLE_RAINSPOUT_ENTITY;
	public static BlockEntityType<LightningHarnessBlockEntity> LIGHTNING_HARNESS_ENTITY;
	public static BlockEntityType<DecorationBlockEntity> CRAWLING_HAND_ENTITY;
	public static BlockEntityType<SilkCocoonBlockEntity> SILK_COCOON_ENTITY;
	public static BlockEntityType<DecorationBlockEntity> EGG_SAC_ENTITY;
	public static BlockEntityType<SilkSpoolStackBlockEntity> SILK_SPOOL_ENTITY;
	public static BlockEntityType<HauntedDiningChairBlockEntity> DINING_CHAIR_ENTITY;
	public static BlockEntityType<DecorationBlockEntity> TABLE_SETTING_ENTITY;
	public static BlockEntityType<GrandfatherClockBlockEntity> GRANDFATHER_CLOCK_ENTITY;
	public static BlockEntityType<WitchlightBlockEntity> WITCHLIGHT_ENTITY;
	public static BlockEntityType<DecorationBlockEntity> SILHOUETTE_ENTITY;
	public static BlockEntityType<DecorationBlockEntity> MOON_LAMP_ENTITY;
	public static BlockEntityType<DecorationBlockEntity> SINGING_PUMPKIN_ENTITY;
	public static BlockEntityType<HarvestEffigyBlockEntity> HARVEST_EFFIGY_ENTITY;
	public static BlockEntityType<FarmStandBlockEntity> FARM_STAND_ENTITY;
	public static ExtendedMenuType<FarmStandMenu, FarmStandMenu.Opening> FARM_STAND_MENU;
	/** The Harvest Effigy catching (assets/jugcraft/sounds/effigy, made by tools/choir_sounds.py). */
	public static SoundEvent EFFIGY_BURN;
	private static final Map<SingingPumpkinBlock.Voice, SoundEvent> SINGING_VOICES = new EnumMap<>(SingingPumpkinBlock.Voice.class);
	/** A Skeleton Key's wards ({@link SkeletonKeyItem}). */
	public static DataComponentType<Integer> KEY_WARDS;
	public static RecipeSerializer<KeyCopyingRecipe> KEY_COPYING_SERIALIZER;
	public static BlockEntityType<DecorationBlockEntity> BRAZIER_ENTITY;
	public static BlockEntityType<DecorationBlockEntity> SHADOW_LAMP_ENTITY;
	public static BlockEntityType<DecorationBlockEntity> FLOATING_HAT_ENTITY;
	public static BlockEntityType<JumpScareTrapBlockEntity> JUMP_SCARE_ENTITY;
	public static BlockEntityType<JudgesTableBlockEntity> JUDGES_TABLE_ENTITY;
	public static BlockEntityType<BowlingScoreboardBlockEntity> BOWLING_SCOREBOARD_ENTITY;
	public static BlockEntityType<DecorationBlockEntity> DANCE_FLOOR_ENTITY;
	public static BlockEntityType<GhostBellBlockEntity> GHOST_BELL_ENTITY;
	public static BlockEntityType<FortuneTellerTableBlockEntity> FORTUNE_TABLE_ENTITY;
	public static BlockEntityType<HalloweenBonfireBlockEntity> BONFIRE_ENTITY;
	public static BlockEntityType<BarmbrackBlockEntity> BARMBRACK_ENTITY;
	public static BlockEntityType<CostumeTrunkBlockEntity> COSTUME_TRUNK_ENTITY;
	public static BlockEntityType<WaxPotBlockEntity> WAX_POT_ENTITY;
	public static BlockEntityType<AuraCandleBlockEntity> AURA_CANDLE_ENTITY;
	/** What an Aura Candle is made of (its wax, layers, colour, scents, strength and burn). */
	public static DataComponentType<CandleMix> CANDLE_MIX;
	public static BlockEntityType<CiderPressBlockEntity> CIDER_PRESS_ENTITY;
	public static BlockEntityType<CiderBarrelBlockEntity> CIDER_BARREL_ENTITY;
	/** The cider a broken Cider Barrel keeps (its servings and when its batch started ageing). */
	public static DataComponentType<BarrelCider> BARREL_CIDER;
	public static BlockEntityType<CanningKettleBlockEntity> CANNING_KETTLE_ENTITY;
	/** Crows: they come to fields by day and peck ripe crops, unless a scarecrow guards them. */
	public static EntityType<Crow> CROW;
	/** Restless spirits: risen from graves at night, seen only by a Spirit Lantern's light (or a Revealing candle's). */
	public static EntityType<RestlessSpirit> RESTLESS_SPIRIT;
	/** Spooky fireworks in flight: they burst into a picture ({@link FireworkShape}). */
	public static EntityType<SpookyRocket> SPOOKY_ROCKET;
	/** Sky lanterns let go: they rise, drift with the wind and burn out. */
	public static EntityType<SkyLantern> SKY_LANTERN;
	public static EntityType<Broomstick> FLYING_BROOMSTICK;
	public static EntityType<Werewolf> WEREWOLF;
	public static EntityType<Squirrel> SQUIRREL;
	public static EntityType<Pumpkling> PUMPKLING;
	public static EntityType<TossRing> TOSS_RING;
	public static BlockEntityType<HighStrikerBlockEntity> HIGH_STRIKER_ENTITY;
	public static EntityType<FerrisWheel> FERRIS_WHEEL;
	public static BlockEntityType<FerrisWheelBlockEntity> FERRIS_WHEEL_BOOTH;
	public static EntityType<Pinata> PINATA;
	public static EntityType<HotAirBalloon> HOT_AIR_BALLOON;
	public static EntityType<Pibal> PIBAL;
	public static DataComponentType<Integer> BALLOON_FUEL;
	public static DataComponentType<Integer> BROOM_CHARGE;
	public static BlockEntityType<FeastTableBlockEntity> FEAST_TABLE_ENTITY;
	public static BlockEntityType<CornMazeGateBlockEntity> CORN_MAZE_GATE_ENTITY;
	/** The mooncakes, baked in the Cooking Pot. */
	public static final List<String> MOONCAKES = List.of("red_bean_mooncake", "chestnut_mooncake", "pumpkin_mooncake");
	public static BlockEntityType<ShowLauncherBlockEntity> SHOW_LAUNCHER_ENTITY;
	/** A spooky firework made with glowstone dust: its sparks twinkle. */
	public static DataComponentType<Boolean> TWINKLE;
	/** The design a Face Paint Kit's dial is set to. */
	public static DataComponentType<FacePaint.Design> FACE_PAINT_DESIGN;
	/** The candy poured onto a Candy Tray. */
	public static DataComponentType<CandyBatch> CANDY_BATCH;
	public static BlockEntityType<CandyKettleBlockEntity> CANDY_KETTLE_ENTITY;
	public static BlockEntityType<BatHouseBlockEntity> BAT_HOUSE_ENTITY;
	public static EntityType<HayGolem> HAY_GOLEM;
	public static EntityType<Turkey> TURKEY;
	public static DataComponentType<KnittingWork> KNITTING;
	public static BlockEntityType<SpinningWheelBlockEntity> SPINNING_WHEEL_ENTITY;
	public static BlockEntityType<HearthOvenBlockEntity> HEARTH_OVEN_ENTITY;
	public static BlockEntityType<SpiritBoardBlockEntity> SPIRIT_BOARD_ENTITY;
	public static BlockEntityType<ThereminBlockEntity> THEREMIN_ENTITY;
	public static BlockEntityType<OfrendaBlockEntity> OFRENDA_ENTITY;
	/** How many uses Knitting Needles have. */
	public static final int NEEDLES_DURABILITY = 128;
	/** Bat guano fertilizes the crops this far round where it is used (a 3x3 patch), with this many doses of bone meal each. */
	public static final int GUANO_RADIUS = 1;
	public static final int GUANO_DOSES = 1;
	/** The five wild autumn mushrooms. */
	public static final List<String> WILD_MUSHROOMS = List.of("chanterelle", "porcini", "puffball", "fly_agaric", "jack_o_lantern_mushroom");
	/** The Candy Kettle's own candies (it also makes candy corn and caramel). */
	public static final List<String> CANDIES = List.of("rock_candy", "salt_water_taffy", "hard_candy", "lollipop", "fudge", "cream_caramel",
			"toffee", "burnt_sugar");
	/** A spooky firework's coloured spark, drawn by the client's SpookySparkParticle. */
	public static final ParticleType<SpookySparkOptions> SPOOKY_SPARK = FabricParticleTypes.complex(true, SpookySparkOptions.CODEC,
			SpookySparkOptions.STREAM_CODEC);
	public static BlockEntityType<PantryShelfBlockEntity> PANTRY_SHELF_ENTITY;
	/** A jar of preserves sealed in a Canning Kettle: it keeps for ever (its item model shows a cloth cap). */
	public static DataComponentType<Boolean> SEALED;
	/** What is left in an unsealed jar of preserves, and when it was cooked or opened. */
	public static DataComponentType<JarContents> JAR_CONTENTS;
	/** The outfits of decorations batch 14, worn on the head and drawn over the whole body (the client's CostumeLayer). */
	public static final List<String> OUTFITS = List.of("vampire_cape", "mummy_wraps", "skeleton_suit", "werewolf_mask", "cat_ears_and_tail",
			"bat_wings");
	/** The Dust Sheet: its item, and the block it becomes over what it covers. */
	public static final String DUST_SHEET = "dust_sheet";
	/** What a Dust Sheet may cover (block tag). */
	public static final TagKey<Block> DUST_SHEET_COVERABLE = TagKey.create(Registries.BLOCK, Jugcraft.id("dust_sheet_coverable"));
	public static EntityType<Seat> SEAT;
	public static BlockEntityType<ScarePropBlockEntity> SCARE_PROP_ENTITY;
	/** Low ground fog from the Fog Machine (drawn by the client: client/FogParticle.java). */
	public static final SimpleParticleType FOG = FabricParticleTypes.simple();
	/** What each kind of pumpkin becomes when first carved by hand, and the loot table its seeds come from. */
	private static final Map<Block, Block> CARVED_FROM = new HashMap<>();
	private static final Map<Block, ResourceKey<LootTable>> CARVE_LOOT = new HashMap<>();
	public static ExtendedMenuType<CookingPotMenu, BlockPos> COOKING_POT_MENU;

	private JugcraftAgriculture() {
	}

	/** A registered agriculture item by ID, such as {@code "corn"}. */
	public static Item item(String id) {
		Item item = ITEMS.get(id);
		if (item == null) {
			throw new IllegalArgumentException("No Jugcraft agriculture item " + id);
		}
		return item;
	}

	/** A registered agriculture block by ID, such as {@code "corn_crop"}. */
	public static Block block(String id) {
		Block block = BLOCKS.get(id);
		if (block == null) {
			throw new IllegalArgumentException("No Jugcraft agriculture block " + id);
		}
		return block;
	}

	public static void register() {
		// Crops. None has a block item: its seed places it.
		for (TallCrop crop : TallCrop.values()) {
			// Wheat's properties (plant colour, crop sounds, broken by pistons, random ticks). Tall and climbing
			// plants are not instant to break, so a sweep of the hand does not flatten a maze wall or a trellis row.
			BlockBehaviour.Properties properties = BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT);
			if (crop.trellis || crop.height(TallCropBlock.MAX_AGE) > 1) {
				properties = properties.strength(0.2F);
			}
			TALL_CROPS.put(crop, (TallCropBlock) registerBlock(crop.blockId, props -> new TallCropBlock(props, crop), properties));
		}
		crop("bean_crop", "beans", true);
		crop("sweet_potato_crop", "sweet_potato", false);
		crop("flax_crop", "flax_seeds", false);
		crop("onion_crop", "onion", false);
		crop("garlic_crop", "garlic", false);
		crop("cabbage_crop", "cabbage_seeds", false);
		crop("oat_crop", "oat_seeds", false);
		crop("barley_crop", "barley_seeds", false);
		// Festival crops: turnips, gourds on stems, the cranberry bog bush and the chestnut tree.
		crop("turnip_crop", "turnip", false);
		// The graveyard flora's mandrake: planted from its root; it screams when pulled up ripe (Mandrakes).
		crop("mandrake_crop", "mandrake_root", false);
		gourd("butternut_squash", "butternut_squash_seeds", 1.0F, MapColor.TERRACOTTA_ORANGE);
		gourd("acorn_squash", "acorn_squash_seeds", 1.0F, MapColor.COLOR_GREEN);
		gourd("warty_gourd", "warty_gourd_seeds", 1.0F, MapColor.COLOR_YELLOW);
		// Halloween harvest: heirloom pumpkins (all carvable), the bottle gourd, and the giant pumpkin.
		gourd("white_pumpkin", "white_pumpkin_seeds", 1.0F, MapColor.SNOW);
		gourd("jarrahdale_pumpkin", "jarrahdale_pumpkin_seeds", 1.0F, MapColor.TERRACOTTA_CYAN);
		gourd("cinderella_pumpkin", "cinderella_pumpkin_seeds", 1.0F, MapColor.COLOR_RED);
		// Pumpkin Night (tools/decor20.py): the Red Kuri and the Kabocha.
		gourd("red_kuri_pumpkin", "red_kuri_pumpkin_seeds", 1.0F, MapColor.COLOR_ORANGE);
		gourd("kabocha_pumpkin", "kabocha_pumpkin_seeds", 1.0F, MapColor.COLOR_GREEN);
		gourd("bottle_gourd", "bottle_gourd_seeds", 1.0F, MapColor.COLOR_LIGHT_GREEN);
		registerGiantPumpkin();
		registerBlock("cranberry_bush", CranberryBushBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.SWEET_BERRY_BUSH)
				.sound(SoundType.WET_GRASS));
		registerChestnutTree();
		registerAppleTree();
		// Generated trees' seasonal leaves start in today's look (tools/trees.py DECORATOR).
		Registry.register(BuiltInRegistries.TREE_DECORATOR_TYPE, Jugcraft.id("seasonal_leaves"), SeasonalLeavesDecorator.TYPE);
		registerTree("larch", "larch_needles", LARCH_GROWER, LARCH_LEAVES, Blocks.SPRUCE_SAPLING, Blocks.SPRUCE_LEAVES,
				MapColor.TERRACOTTA_RED, MapColor.TERRACOTTA_ORANGE);
		registerTree("maple", "maple_leaves", MAPLE_GROWER, MAPLE_LEAVES, Blocks.OAK_SAPLING, Blocks.OAK_LEAVES,
				MapColor.COLOR_GRAY, MapColor.TERRACOTTA_PINK);
		registerTree("aspen", "aspen_leaves", ASPEN_GROWER, ASPEN_LEAVES, Blocks.BIRCH_SAPLING, Blocks.BIRCH_LEAVES,
				MapColor.QUARTZ, MapColor.SAND);
		registerTree("fir", "fir_needles", FIR_GROWER, null, Blocks.SPRUCE_SAPLING, Blocks.SPRUCE_LEAVES,
				MapColor.PODZOL, MapColor.WOOD);
		registerTree("jacaranda", "jacaranda_leaves", JACARANDA_GROWER, null, Blocks.CHERRY_SAPLING, Blocks.CHERRY_LEAVES,
				MapColor.TERRACOTTA_GRAY, MapColor.TERRACOTTA_PINK);
		registerTree("willow", "willow_leaves", WILLOW_GROWER, WILLOW_LEAVES, Blocks.OAK_SAPLING, Blocks.OAK_LEAVES,
				MapColor.TERRACOTTA_BROWN, MapColor.TERRACOTTA_YELLOW);
		registerTree("palm", "palm_fronds", PALM_GROWER, null, Blocks.JUNGLE_SAPLING, Blocks.JUNGLE_LEAVES,
				MapColor.TERRACOTTA_LIGHT_GRAY, MapColor.SAND);
		registerTree("cypress", "cypress_leaves", CYPRESS_GROWER, null, Blocks.SPRUCE_SAPLING, Blocks.SPRUCE_LEAVES,
				MapColor.TERRACOTTA_RED, MapColor.TERRACOTTA_ORANGE);
		registerTree("redwood", "redwood_needles", REDWOOD_GROWER, null, Blocks.SPRUCE_SAPLING, Blocks.SPRUCE_LEAVES,
				MapColor.TERRACOTTA_BROWN, MapColor.TERRACOTTA_RED);
		registerTree("eucalyptus", "eucalyptus_leaves", EUCALYPTUS_GROWER, null, Blocks.JUNGLE_SAPLING, Blocks.JUNGLE_LEAVES,
				MapColor.TERRACOTTA_GREEN, MapColor.TERRACOTTA_WHITE);
		registerTree("mahogany", "mahogany_leaves", MAHOGANY_GROWER, null, Blocks.JUNGLE_SAPLING, Blocks.JUNGLE_LEAVES,
				MapColor.TERRACOTTA_GRAY, MapColor.CRIMSON_STEM);
		registerTree("cedar", "cedar_leaves", CEDAR_GROWER, null, Blocks.SPRUCE_SAPLING, Blocks.SPRUCE_LEAVES,
				MapColor.TERRACOTTA_BROWN, MapColor.COLOR_BROWN);
		registerWoodSet("dead", MapColor.COLOR_LIGHT_GRAY, MapColor.TERRACOTTA_LIGHT_GRAY);
		registerWildPlants();

		// Seeds, produce and food.
		food("corn", 3, 0.6F, COMPOST_MEDIUM);
		seeds("corn_kernels", "corn_crop", COMPOST_LOW);
		food("roasted_corn", 5, 0.6F, COMPOST_MEDIUM_HIGH);
		food("popcorn", 2, 0.3F, COMPOST_MEDIUM_HIGH);
		seeds("sunflower_seeds", "sunflower_crop", COMPOST_LOW);
		food("roasted_sunflower_seeds", 2, 0.3F, COMPOST_MEDIUM_HIGH);
		seeds("beans", "bean_crop", COMPOST_MEDIUM);
		edibleSeeds("sweet_potato", "sweet_potato_crop", 2, 0.3F, COMPOST_MEDIUM);
		food("baked_sweet_potato", 5, 0.6F, COMPOST_MEDIUM_HIGH);
		plain("flax", COMPOST_MEDIUM);
		seeds("flax_seeds", "flax_crop", COMPOST_LOW);
		stew("three_sisters_stew", 10, 0.6F);
		// Kitchen Garden.
		food("tomato", 3, 0.3F, COMPOST_MEDIUM);
		trellisSeeds("tomato_seeds", TallCrop.TOMATO, COMPOST_LOW);
		food("pepper", 2, 0.3F, COMPOST_MEDIUM);
		seeds("pepper_seeds", "pepper_crop", COMPOST_LOW);
		seeds("onion", "onion_crop", COMPOST_MEDIUM);
		seeds("garlic", "garlic_crop", COMPOST_MEDIUM);
		food("cabbage", 3, 0.6F, COMPOST_MEDIUM);
		seeds("cabbage_seeds", "cabbage_crop", COMPOST_LOW);
		plain("oats", COMPOST_MEDIUM);
		seeds("oat_seeds", "oat_crop", COMPOST_LOW);
		plain("barley", COMPOST_MEDIUM);
		seeds("barley_seeds", "barley_crop", COMPOST_LOW);
		food("barley_bread", 5, 0.6F, COMPOST_MEDIUM_HIGH);
		food("sauerkraut", 4, 0.6F, COMPOST_MEDIUM_HIGH);
		stew("garden_salad", 7, 0.6F);
		stew("tomato_soup", 8, 0.6F);
		stew("onion_soup", 8, 0.6F);
		stew("vegetable_soup", 10, 0.6F);
		stew("mushroom_barley_soup", 8, 0.6F);
		stew("oat_porridge", 6, 0.6F);
		stew("chili", 10, 0.8F);
		meal("cabbage_rolls", 6, 0.8F);
		// Festival crops.
		seeds("butternut_squash_seeds", "butternut_squash_stem", COMPOST_LOW);
		seeds("acorn_squash_seeds", "acorn_squash_stem", COMPOST_LOW);
		seeds("warty_gourd_seeds", "warty_gourd_stem", COMPOST_LOW);
		edibleSeeds("turnip", "turnip_crop", 3, 0.6F, COMPOST_MEDIUM);
		seeds("mandrake_root", "mandrake_crop", COMPOST_MEDIUM);
		edibleSeeds("cranberries", "cranberry_bush", 2, 0.1F, COMPOST_LOW);
		seeds("chestnut", "chestnut_sapling", COMPOST_LOW);
		food("roasted_chestnuts", 4, 0.6F, COMPOST_MEDIUM_HIGH);
		food("baked_acorn_squash", 6, 0.6F, COMPOST_MEDIUM_HIGH);
		food("squash_pie", 8, 0.3F, COMPOST_MEDIUM_HIGH);
		food("candy_corn", 2, 0.1F, COMPOST_MEDIUM_HIGH);
		stew("butternut_squash_soup", 8, 0.6F);
		stew("harvest_stew", 10, 0.6F);
		stew("cranberry_sauce", 5, 0.6F);
		food("roasted_pumpkin_seeds", 2, 0.3F, COMPOST_MEDIUM_HIGH);
		// Halloween harvest.
		seeds("giant_pumpkin_seeds", "giant_pumpkin_vine", COMPOST_LOW);
		plain("pumpkin_guts", COMPOST_MEDIUM);
		stew("pumpkin_soup", 8, 0.6F);
		seeds("white_pumpkin_seeds", "white_pumpkin_stem", COMPOST_LOW);
		seeds("jarrahdale_pumpkin_seeds", "jarrahdale_pumpkin_stem", COMPOST_LOW);
		seeds("cinderella_pumpkin_seeds", "cinderella_pumpkin_stem", COMPOST_LOW);
		seeds("red_kuri_pumpkin_seeds", "red_kuri_pumpkin_stem", COMPOST_LOW);
		seeds("kabocha_pumpkin_seeds", "kabocha_pumpkin_stem", COMPOST_LOW);
		stew("red_kuri_soup", 9, 0.7F);
		food("kabocha_tempura", 6, 0.8F, COMPOST_MEDIUM_HIGH);
		seeds("bottle_gourd_seeds", "bottle_gourd_stem", COMPOST_LOW);
		plain("dried_bottle_gourd", COMPOST_MEDIUM);
		plain("ornamental_corn", COMPOST_MEDIUM);
		seeds("ornamental_corn_kernels", "ornamental_corn_crop", COMPOST_LOW);
		plain("corn_stalks", COMPOST_MEDIUM);
		food("caramel", 2, 0.1F, COMPOST_MEDIUM_HIGH);
		treat("caramel_apple", 6, 0.6F);
		food("popcorn_ball", 5, 0.6F, COMPOST_MEDIUM_HIGH);
		food("marshmallow", 1, 0.1F, COMPOST_MEDIUM_HIGH);
		treat("toasted_marshmallow", 4, 0.5F);
		treat("burnt_marshmallow", 2, 0.1F);
		// Trick-or-treating's rare prize (only villagers hand it out).
		food("king_size_candy_bar", 8, 0.4F, COMPOST_MEDIUM_HIGH);
		// Halloween treats: soul cakes, pumpkin bread, spiderweb cupcakes and bat-wing cookies; two drinks in a bottle.
		food("soul_cake", 4, 0.4F, COMPOST_MEDIUM_HIGH);
		food("pumpkin_bread", 6, 0.8F, COMPOST_MEDIUM_HIGH);
		food("spiderweb_cupcake", 3, 0.4F, COMPOST_MEDIUM_HIGH);
		food("bat_wing_cookie", 2, 0.1F, COMPOST_MEDIUM_HIGH);
		drink("pumpkin_spice_latte", 3, 0.3F, MobEffects.SPEED, 30);
		drink("witchs_brew_punch", 2, 0.2F, MobEffects.GLOWING, 10);
		// Spooky sweets from the Cooking Pot: a moment of magic each.
		sweet("glow_gum", 1, 0.1F, MobEffects.GLOWING, 30);
		sweet("ghost_taffy", 1, 0.1F, MobEffects.INVISIBILITY, 3);
		sweet("fizz_rocks", 1, 0.1F, MobEffects.JUMP_BOOST, 20);
		sweet("witchs_licorice", 1, 0.1F, MobEffects.NIGHT_VISION, 45);
		// Fall additions 2, the cider mill: the apple tree's seeds, the press's pomace, cider at each stage and what is made with it.
		seeds("apple_seeds", "apple_sapling", COMPOST_LOW);
		plain("apple_pomace", COMPOST_MEDIUM);
		drink("sweet_cider", 3, 0.3F, MobEffects.HASTE, 30, true);
		drink("sparkling_cider", 3, 0.4F, MobEffects.JUMP_BOOST, 60);
		drink("aged_cider", 4, 0.6F, MobEffects.ABSORPTION, 120);
		drink("mulled_cider", 6, 0.8F, MobEffects.REGENERATION, 15);
		plain("mulling_spices", COMPOST_MEDIUM);
		food("apple_cider_donut", 3, 0.4F, COMPOST_MEDIUM_HIGH);

		// Farm tools.
		sickle("flint_sickle", 1, 131);
		sickle("bronze_sickle", 2, 350);

		// Wild plants: natural seed sources.
		wild("wild_corn");
		wild("wild_sunflower");
		wild("wild_beans");
		wild("wild_sweet_potato");
		wild("wild_flax");
		wild("wild_tomato");
		wild("wild_pepper");
		wild("wild_onion");
		wild("wild_garlic");
		wild("wild_cabbage");
		wild("wild_oats");
		wild("wild_barley");
		wild("wild_turnip");
		wild("wild_mandrake");
		Mandrakes.register();

		registerEquipment();
		registerDecorations();
		registerCarving();
		registerHalloween();
		registerRegatta();
		registerTrickOrTreat();
		registerFestivities();
		registerNight();
		registerHalloweenDecorations();

		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.NATURAL_BLOCKS).register(output -> SEEDS_TAB.forEach(output::accept));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FOOD_AND_DRINKS).register(output -> FOOD_TAB.forEach(output::accept));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(output -> INGREDIENT_TAB.forEach(output::accept));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(output -> TOOL_TAB.forEach(output::accept));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(output -> EQUIPMENT_TAB.forEach(output::accept));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS).register(output -> BUILDING_TAB.forEach(output::accept));

		registerGrassSeeds();
		registerWorldgen();
	}

	// ---------------------------------------------------------------- acquisition

	/**
	 * Short grass sometimes drops a Jugcraft seed, like wheat seeds: every crop is reachable in any world.
	 * One pool picks one seed evenly, so adding crops never makes grass drop more seeds overall.
	 */
	private static void registerGrassSeeds() {
		ResourceKey<LootTable> shortGrass = ResourceKey.create(Registries.LOOT_TABLE,
				Identifier.fromNamespaceAndPath("minecraft", "blocks/short_grass"));
		LootTableEvents.MODIFY.register((key, table, source, registries) -> {
			if (!source.isBuiltin() || !key.equals(shortGrass) || !JugcraftConfig.isFeatureEnabled(FEATURE)) {
				return;
			}
			LootPool.Builder pool = LootPool.lootPool().when(LootItemRandomChanceCondition.randomChance(GRASS_SEED_CHANCE));
			for (String seed : GRASS_SEEDS) {
				pool.add(LootItem.lootTableItem(item(seed)));
			}
			table.withPool(pool);
		});
	}

	/** The trellis for climbing crops and the Cooking Pot, with its block entity, screen and recipe type. */
	private static void registerEquipment() {
		Block trellis = registerBlock("trellis", TrellisBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.WOOD)
				.strength(1.0F).sound(SoundType.SCAFFOLDING).noOcclusion());
		registerItem("trellis", props -> new BlockItem(trellis, props), new Item.Properties().useBlockDescriptionPrefix(), EQUIPMENT_TAB);
		Block pot = registerBlock("cooking_pot", CookingPotBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
				.strength(2.0F).requiresCorrectToolForDrops().sound(SoundType.LANTERN).noOcclusion());
		registerItem("cooking_pot", props -> new BlockItem(pot, props), new Item.Properties().useBlockDescriptionPrefix(), EQUIPMENT_TAB);

		COOKING_POT_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("cooking_pot"),
				FabricBlockEntityTypeBuilder.create(CookingPotBlockEntity::new, pot).build());
		COOKING_POT_MENU = Registry.register(BuiltInRegistries.MENU, Jugcraft.id("cooking_pot"),
				new ExtendedMenuType<>((containerId, inventory, pos) -> new CookingPotMenu(containerId, inventory), BlockPos.STREAM_CODEC.cast()));
		POT_COOKING = Registry.register(BuiltInRegistries.RECIPE_TYPE, Jugcraft.id("pot_cooking"), new RecipeType<CookingPotRecipe>() {
			@Override
			public String toString() {
				return Jugcraft.MOD_ID + ":pot_cooking";
			}
		});
		POT_SERIALIZER = Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, Jugcraft.id("pot_cooking"),
				new RecipeSerializer<>(CookingPotRecipe.CODEC, CookingPotRecipe.STREAM_CODEC));
		CookingPotRecipe.registerReloadListener();
	}

	/** The Turnip Lantern: a carved turnip that gives light, the original jack-o'-lantern. */
	private static void registerDecorations() {
		Block lantern = registerBlock("turnip_lantern", TurnipLanternBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE)
				.strength(0.5F).sound(SoundType.WOOD).lightLevel(state -> 13).noOcclusion());
		registerItem("turnip_lantern", props -> new BlockItem(lantern, props), new Item.Properties().useBlockDescriptionPrefix(), EQUIPMENT_TAB);
	}

	/**
	 * Pumpkin carving: the Carving Knife, and the hand-carved pumpkin it makes (any face on any side,
	 * lit by a torch). See {@link PumpkinCarvings}.
	 */
	private static void registerCarving() {
		CARVING = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Jugcraft.id("carving"),
				DataComponentType.<PumpkinCarving>builder().persistent(PumpkinCarving.CODEC).networkSynchronized(PumpkinCarving.STREAM_CODEC).build());
		List<Block> carved = new ArrayList<>();
		carved.add(carvedPumpkin("hand_carved_pumpkin", Blocks.PUMPKIN, BuiltInLootTables.CARVE_PUMPKIN, MapColor.COLOR_ORANGE));
		for (String variety : List.of("white_pumpkin", "jarrahdale_pumpkin", "cinderella_pumpkin", "red_kuri_pumpkin", "kabocha_pumpkin")) {
			carved.add(carvedPumpkin("hand_carved_" + variety, block(variety),
					ResourceKey.create(Registries.LOOT_TABLE, Jugcraft.id("carve/" + variety)), block(variety).defaultMapColor()));
		}
		CARVED_PUMPKIN_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("hand_carved_pumpkin"),
				FabricBlockEntityTypeBuilder.create(CarvedPumpkinBlockEntity::new, carved.toArray(Block[]::new)).build());
		registerItem("carving_knife", CarvingKnifeItem::new, new Item.Properties().durability(CARVING_KNIFE_DURABILITY), TOOL_TAB);
		PumpkinCarvings.register();
	}

	/** A hand-carved pumpkin of one kind (with its item), carved from {@code pumpkin}, whose seeds come from {@code seeds}. */
	private static Block carvedPumpkin(String id, Block pumpkin, ResourceKey<LootTable> seeds, MapColor color) {
		Block carved = registerBlock(id, CarvedPumpkinBlock::new,
				BlockBehaviour.Properties.ofFullCopy(Blocks.CARVED_PUMPKIN).mapColor(color).lightLevel(CarvedPumpkinBlock::light));
		// Worn like a vanilla carved pumpkin: on the head, seen through, never swapped on by a right click (that places it).
		Equippable worn = Equippable.builder(EquipmentSlot.HEAD).setSwappable(false)
				.setCameraOverlay(Identifier.withDefaultNamespace("misc/pumpkinblur")).build();
		registerItem(id, props -> new BlockItem(carved, props), new Item.Properties().useBlockDescriptionPrefix()
				.component(DataComponents.EQUIPPABLE, worn), EQUIPMENT_TAB);
		CARVED_FROM.put(pumpkin, carved);
		CARVE_LOOT.put(pumpkin, seeds);
		return carved;
	}

	/** The plain pumpkin a hand-carved pumpkin of this kind was carved from, or null if it is not a hand-carved pumpkin. */
	public static @Nullable Block plainPumpkin(Block carved) {
		for (Map.Entry<Block, Block> entry : CARVED_FROM.entrySet()) {
			if (entry.getValue() == carved) {
				return entry.getKey();
			}
		}
		return null;
	}

	/** The hand-carved pumpkin a plain pumpkin of this kind becomes when first carved, or null if it is not a carvable pumpkin. */
	public static @Nullable Block carvedFrom(Block pumpkin) {
		return CARVED_FROM.get(pumpkin);
	}

	/** The loot table of the seeds that come out of a pumpkin of this kind when it is first carved. */
	public static ResourceKey<LootTable> carveLoot(Block pumpkin) {
		return CARVE_LOOT.getOrDefault(pumpkin, BuiltInLootTables.CARVE_PUMPKIN);
	}

	/** Uses of a Carving Knife: one per finished carving (shears' durability). */
	public static final int CARVING_KNIFE_DURABILITY = 238;

	/**
	 * The giant pumpkin: its vine (planted from Giant Pumpkin Seeds), the vine holding a fruit, and the fruit,
	 * a block of up to 3x3x3 whose master block keeps its growth, weight and carvings. Pistons cannot move it;
	 * breaking it picks it up whole as a Giant Pumpkin item, which places it again.
	 */
	private static void registerGiantPumpkin() {
		GIANT_PUMPKIN = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Jugcraft.id("giant_pumpkin"),
				DataComponentType.<GiantPumpkinData>builder().persistent(GiantPumpkinData.CODEC)
						.networkSynchronized(ByteBufCodecs.fromCodecWithRegistries(GiantPumpkinData.CODEC)).build());
		Block giant = registerBlock("giant_pumpkin", GiantPumpkinBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.PUMPKIN)
				.strength(3.0F).lightLevel(GiantPumpkinBlock::light).pushReaction(PushReaction.IMMOVEABLE));
		registerItem("giant_pumpkin", props -> new GiantPumpkinItem(giant, props), new Item.Properties().stacksTo(1).useBlockDescriptionPrefix()
				.component(GIANT_PUMPKIN, GiantPumpkinData.FULL_GROWN), EQUIPMENT_TAB);
		registerBlock("giant_pumpkin_vine", GiantPumpkinVineBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.PUMPKIN_STEM));
		registerBlock("attached_giant_pumpkin_vine", AttachedGiantPumpkinVineBlock::new,
				BlockBehaviour.Properties.ofFullCopy(Blocks.ATTACHED_PUMPKIN_STEM));
		GIANT_PUMPKIN_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("giant_pumpkin"),
				FabricBlockEntityTypeBuilder.create(GiantPumpkinBlockEntity::new, giant).build());
	}

	/**
	 * The rest of the Halloween harvest: the Harvest Scale and its ribbons, pumpkin stencils, the Scarecrow,
	 * the Corn Shock and Ornamental Corn Bundle, the Gourd Birdhouse and Gourd Canteen, and mums with their pots.
	 */
	private static void registerHalloween() {
		STENCIL = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Jugcraft.id("stencil"),
				DataComponentType.<PumpkinCarving>builder().persistent(PumpkinCarving.CODEC).networkSynchronized(PumpkinCarving.STREAM_CODEC).build());
		CANTEEN_WATER = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Jugcraft.id("canteen_water"),
				DataComponentType.<Integer>builder().persistent(Codec.intRange(0, GourdCanteenItem.CAPACITY))
						.networkSynchronized(ByteBufCodecs.VAR_INT).build());

		Block scale = registerBlock("harvest_scale", HarvestScaleBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.WOOD)
				.strength(2.0F).sound(SoundType.WOOD).noOcclusion());
		registerItem("harvest_scale", props -> new BlockItem(scale, props), new Item.Properties().useBlockDescriptionPrefix(), EQUIPMENT_TAB);
		HARVEST_SCALE_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("harvest_scale"),
				FabricBlockEntityTypeBuilder.create(HarvestScaleBlockEntity::new, scale).build());
		for (String ribbon : HarvestScaleBlockEntity.RIBBONS) {
			registerItem(ribbon, Item::new, new Item.Properties().rarity(Rarity.UNCOMMON), TOOL_TAB);
		}

		registerItem("blank_stencil", BlankStencilItem::new, new Item.Properties(), TOOL_TAB);
		registerItem("pumpkin_stencil", PumpkinStencilItem::new, new Item.Properties().stacksTo(1), TOOL_TAB);
		registerItem("gourd_canteen", GourdCanteenItem::new, new Item.Properties().stacksTo(1).component(CANTEEN_WATER, 0), TOOL_TAB);

		Block scarecrow = registerBlock("scarecrow", ScarecrowBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW)
				.strength(0.8F).sound(SoundType.GRASS).noOcclusion().ignitedByLava().pushReaction(PushReaction.POPPED)
				.lightLevel(ScarecrowBlock::light));
		SCARECROW_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("scarecrow"),
				FabricBlockEntityTypeBuilder.create(ScarecrowBlockEntity::new, scarecrow).build());
		Block shock = registerBlock("corn_shock", props -> new TallDecorationBlock(props, Block.box(1.0, 0.0, 1.0, 15.0, 16.0, 15.0),
				Block.box(4.0, 0.0, 4.0, 12.0, 14.0, 12.0)), BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW)
				.strength(0.5F).sound(SoundType.GRASS).noOcclusion().ignitedByLava().pushReaction(PushReaction.POPPED));
		Block bundle = registerBlock("ornamental_corn_bundle", props -> new WallDecorationBlock(props, 3.0, 2.0, 14.0),
				BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(0.3F).sound(SoundType.GRASS).noCollision()
						.noOcclusion().ignitedByLava().pushReaction(PushReaction.POPPED));
		Block birdhouse = registerBlock("gourd_birdhouse", LanternBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_YELLOW)
				.strength(0.5F).sound(SoundType.WOOD).noOcclusion().pushReaction(PushReaction.POPPED));
		for (Block block : List.of(scarecrow, shock, bundle, birdhouse)) {
			String id = BuiltInRegistries.BLOCK.getKey(block).getPath();
			registerItem(id, props -> new BlockItem(block, props), new Item.Properties().useBlockDescriptionPrefix(), EQUIPMENT_TAB);
		}
		// Straw and dry stalks burn like a hay bale.
		FlammableBlockRegistry fire = FlammableBlockRegistry.getDefaultInstance();
		for (Block block : List.of(scarecrow, shock, bundle)) {
			fire.add(block, 60, 20);
		}

		mum("yellow_mum", MobEffects.SATURATION, 0.35F);
		mum("orange_mum", MobEffects.FIRE_RESISTANCE, 4.0F);
		mum("red_mum", MobEffects.REGENERATION, 8.0F);
		mum("purple_mum", MobEffects.NIGHT_VISION, 5.0F);
		// Fall additions 20: the cempasúchil marigold, the flower of Día de Muertos.
		mum("marigold", MobEffects.LUCK, 6.0F);
	}

	/**
	 * The pumpkin regatta: Pumpkin Barge and Pumpkin Racer (entities and their items, hollowed from giant
	 * pumpkins), the Regatta Flag that starts and times a run, and the numbered Regatta Buoys of its course.
	 */
	private static void registerRegatta() {
		PUMPKIN_BOAT = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Jugcraft.id("pumpkin_boat"),
				DataComponentType.<PumpkinBoatData>builder().persistent(PumpkinBoatData.CODEC).networkSynchronized(PumpkinBoatData.STREAM_CODEC)
						.build());
		PUMPKIN_BARGE = boat(PumpkinBoat.Kind.BARGE, 2.75F, 1.125F);
		PUMPKIN_RACER = boat(PumpkinBoat.Kind.RACER, 1.75F, 0.9F);
		for (PumpkinBoat.Kind kind : PumpkinBoat.Kind.values()) {
			registerItem(kind.id, props -> new PumpkinBoatItem(kind, props), new Item.Properties().stacksTo(1)
					.component(PUMPKIN_BOAT, kind.defaultData()), TOOL_TAB);
		}
		Block flag = registerBlock("regatta_flag", RegattaFlagBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.SNOW)
				.strength(1.0F).sound(SoundType.WOOD).noOcclusion().ignitedByLava());
		registerItem("regatta_flag", props -> new BlockItem(flag, props), new Item.Properties().useBlockDescriptionPrefix(), EQUIPMENT_TAB);
		Block buoy = registerBlock("regatta_buoy", RegattaBuoyBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED)
				.strength(0.5F).sound(SoundType.WOOD).noOcclusion().pushReaction(PushReaction.POPPED));
		registerItem("regatta_buoy", props -> new PlaceOnWaterBlockItem(buoy, props), new Item.Properties().useBlockDescriptionPrefix(),
				EQUIPMENT_TAB);
		REGATTA_FLAG_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("regatta_flag"),
				FabricBlockEntityTypeBuilder.create(RegattaFlagBlockEntity::new, flag).build());
		REGATTA_BUOY_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("regatta_buoy"),
				FabricBlockEntityTypeBuilder.create(RegattaBuoyBlockEntity::new, buoy).build());
	}

	private static EntityType<PumpkinBoat> boat(PumpkinBoat.Kind kind, float width, float height) {
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, Jugcraft.id(kind.id));
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, EntityType.Builder.<PumpkinBoat>of(
				(type, level) -> new PumpkinBoat(type, level, kind), MobCategory.MISC)
				.noLootTable().sized(width, height).eyeHeight(height).clientTrackingRange(10).build(key));
	}

	/** The entity type of a kind of pumpkin boat. */
	public static EntityType<PumpkinBoat> boatType(PumpkinBoat.Kind kind) {
		return kind == PumpkinBoat.Kind.BARGE ? PUMPKIN_BARGE : PUMPKIN_RACER;
	}

	/** Costumes (worn on the head, nothing else) and the Candy Bag that knocks on villagers' doors. */
	private static void registerTrickOrTreat() {
		for (String costume : COSTUMES) {
			Equippable.Builder worn = Equippable.builder(EquipmentSlot.HEAD).setEquipSound(SoundEvents.ARMOR_EQUIP_LEATHER);
			if (costume.equals("ghost_sheet")) {
				// Its equipment asset has no layers: nothing is drawn by the armor layer or as a block on the head, and the
				// client's GhostSheetLayer drapes the whole sheet over the wearer (it reads equipment that has an asset).
				worn.setCameraOverlay(Jugcraft.id("misc/ghost_sheet")).setAsset(ResourceKey.create(EquipmentAssets.ROOT_ID, Jugcraft.id("ghost_sheet")));
			}
			registerItem(costume, Item::new, new Item.Properties().stacksTo(1).component(DataComponents.EQUIPPABLE, worn.build()), TOOL_TAB);
		}
		CANDY_BAG_NIGHT = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Jugcraft.id("candy_bag_night"),
				DataComponentType.<CandyBagItem.Night>builder().persistent(CandyBagItem.Night.CODEC)
						.networkSynchronized(CandyBagItem.Night.STREAM_CODEC).build());
		registerItem("candy_bag", CandyBagItem::new, new Item.Properties().stacksTo(1)
				.component(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY), TOOL_TAB);
		TrickOrTreat.register();
		TrickOrTreaters.register();
	}

	/**
	 * Halloween festivities: the Judging Stand of the carving contest, costumed mobs and the Halloween Peddler
	 * (both only while the event runs), and decorations for any time of year: three gravestones to engrave,
	 * the Spun Cobweb (no sticking), the Hanging Ghost and the Candle Skull.
	 */
	private static void registerFestivities() {
		Block stand = registerBlock("judging_stand", JudgingStandBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE)
				.strength(1.5F).sound(SoundType.WOOD).noOcclusion().ignitedByLava());
		registerItem("judging_stand", props -> new BlockItem(stand, props), new Item.Properties().useBlockDescriptionPrefix(), EQUIPMENT_TAB);
		JUDGING_STAND_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("judging_stand"),
				FabricBlockEntityTypeBuilder.create(JudgingStandBlockEntity::new, stand).build());
		CarvingContest.register();
		CostumedMobs.register();
		HalloweenPeddler.register();

		List<Block> gravestones = new ArrayList<>();
		for (GravestoneBlock.Style style : GravestoneBlock.Style.values()) {
			// Random ticks: graves stir at night (fall additions 9, ghost hunting).
			Block stone = registerBlock(style.id, props -> new GravestoneBlock(props, style), BlockBehaviour.Properties.of()
					.mapColor(MapColor.STONE).requiresCorrectToolForDrops().strength(1.5F, 6.0F).sound(SoundType.STONE).noOcclusion().randomTicks());
			registerItem(style.id, props -> new BlockItem(stone, props), new Item.Properties().useBlockDescriptionPrefix(), EQUIPMENT_TAB);
			gravestones.add(stone);
		}
		GRAVESTONE_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("gravestone"),
				FabricBlockEntityTypeBuilder.create(GravestoneBlockEntity::new, gravestones.toArray(Block[]::new)).build());

		Block cobweb = registerBlock("spun_cobweb", Block::new, BlockBehaviour.Properties.of().mapColor(MapColor.WOOL).noCollision()
				.strength(0.2F).sound(SoundType.COBWEB).noOcclusion().ignitedByLava().pushReaction(PushReaction.POPPED));
		Block ghost = registerBlock("hanging_ghost", HangingGhostBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.SNOW)
				.noCollision().strength(0.3F).sound(SoundType.WOOL).noOcclusion().ignitedByLava().pushReaction(PushReaction.POPPED));
		Block skull = registerBlock("candle_skull", CandleSkullBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.SAND)
				.strength(1.0F).sound(SoundType.BONE_BLOCK).noOcclusion().lightLevel(CandleSkullBlock::light).pushReaction(PushReaction.POPPED));
		for (Block block : List.of(cobweb, ghost, skull)) {
			String id = BuiltInRegistries.BLOCK.getKey(block).getPath();
			registerItem(id, props -> new BlockItem(block, props), new Item.Properties().useBlockDescriptionPrefix(), EQUIPMENT_TAB);
		}
		// String and cloth burn like wool.
		FlammableBlockRegistry fire = FlammableBlockRegistry.getDefaultInstance();
		fire.add(cobweb, 30, 60);
		fire.add(ghost, 30, 60);
	}

	/**
	 * Halloween nights: will-o'-wisps (and the Wisp in a Jar they are caught into), the Pumpkin Chunkin' Trebuchet
	 * with its flying pumpkins and landing markers, the Headless Horseman with his flaming pumpkins, lantern and
	 * cloak, and the Harvest Moon.
	 */
	private static void registerNight() {
		WILL_O_WISP = entity("will_o_wisp", EntityType.Builder.<WillOWisp>of(WillOWisp::new, MobCategory.AMBIENT).noLootTable()
				.sized(0.4F, 0.4F).eyeHeight(0.2F).clientTrackingRange(8));
		FabricDefaultAttributeRegistry.register(WILL_O_WISP, WillOWisp.createAttributes());
		Block jar = registerBlock("wisp_in_a_jar", LanternBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE)
				.strength(0.3F).sound(SoundType.GLASS).lightLevel(state -> WISP_JAR_LIGHT).noOcclusion().pushReaction(PushReaction.POPPED));
		registerItem("wisp_in_a_jar", props -> new BlockItem(jar, props), new Item.Properties().useBlockDescriptionPrefix(), EQUIPMENT_TAB);
		Wisps.register();

		Block trebuchet = registerBlock("trebuchet", TrebuchetBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.WOOD)
				.strength(2.0F).sound(SoundType.WOOD).noOcclusion().ignitedByLava());
		registerItem("trebuchet", props -> new BlockItem(trebuchet, props), new Item.Properties().useBlockDescriptionPrefix(), EQUIPMENT_TAB);
		TREBUCHET_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("trebuchet"),
				FabricBlockEntityTypeBuilder.create(TrebuchetBlockEntity::new, trebuchet).build());
		FLYING_PUMPKIN = entity("flying_pumpkin", EntityType.Builder.<FlyingPumpkin>of(FlyingPumpkin::new, MobCategory.MISC).noLootTable()
				.sized(0.5F, 0.5F).clientTrackingRange(10).updateInterval(5));
		THROW_MARKER = entity("throw_marker", EntityType.Builder.<ThrowMarker>of(ThrowMarker::new, MobCategory.MISC).noLootTable().noSave()
				.sized(0.4F, 1.2F).clientTrackingRange(8).updateInterval(40));

		HEADLESS_HORSEMAN = entity("headless_horseman", EntityType.Builder.<HeadlessHorseman>of(HeadlessHorseman::new, MobCategory.MONSTER)
				.sized(1.4F, 2.9F).eyeHeight(2.5F).fireImmune().notInPeaceful().clientTrackingRange(10));
		FabricDefaultAttributeRegistry.register(HEADLESS_HORSEMAN, HeadlessHorseman.createAttributes());
		FLAMING_PUMPKIN = entity("flaming_pumpkin", EntityType.Builder.<FlamingPumpkin>of(FlamingPumpkin::new, MobCategory.MISC).noLootTable()
				.sized(0.5F, 0.5F).fireImmune().clientTrackingRange(8).updateInterval(5));
		Block lantern = registerBlock("horseman_lantern", LanternBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE)
				.strength(1.0F).sound(SoundType.LANTERN).lightLevel(state -> 15).noOcclusion().pushReaction(PushReaction.POPPED));
		registerItem("horseman_lantern", props -> new BlockItem(lantern, props), new Item.Properties().useBlockDescriptionPrefix()
				.rarity(Rarity.EPIC), EQUIPMENT_TAB);
		Equippable cloak = Equippable.builder(EquipmentSlot.CHEST).setEquipSound(SoundEvents.ARMOR_EQUIP_LEATHER)
				.setAsset(ResourceKey.create(EquipmentAssets.ROOT_ID, Jugcraft.id("horseman_cloak"))).build();
		registerItem("horseman_cloak", Item::new, new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).component(DataComponents.EQUIPPABLE, cloak),
				TOOL_TAB);
		HarvestMoon.register();
	}

	/**
	 * Halloween decorations, the first five: String Light Hooks and the Jack-o'-Lantern String Lights strung between
	 * them, the Candy Bowl, the Coffin, the Haunted Portrait and the Fog Machine. The hooks and the fog machine take
	 * electricity through the shared energy interface.
	 */
	private static void registerHalloweenDecorations() {
		Block hook = registerBlock("string_light_hook", StringLightHookBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
				.strength(0.5F).sound(SoundType.LANTERN).noCollision().lightLevel(StringLightHookBlock::light).pushReaction(PushReaction.POPPED));
		registerItem("string_light_hook", props -> new BlockItem(hook, props), new Item.Properties().useBlockDescriptionPrefix(), BUILDING_TAB);
		STRING_LIGHT_HOOK_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("string_light_hook"),
				FabricBlockEntityTypeBuilder.create(StringLightHookBlockEntity::new, hook).build());
		EnergyStorage.SIDED.registerForBlockEntity((entity, side) -> entity.energy(), STRING_LIGHT_HOOK_ENTITY);
		registerItem("jack_o_lantern_string_lights", props -> new StringLightsItem(props, StringLightHookBlockEntity.Strand.LIGHTS),
				new Item.Properties(), BUILDING_TAB);

		Block bowl = registerBlock("candy_bowl", CandyBowlBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE)
				.strength(0.8F).sound(SoundType.DECORATED_POT).noOcclusion().pushReaction(PushReaction.POPPED));
		registerItem("candy_bowl", props -> new BlockItem(bowl, props), new Item.Properties().useBlockDescriptionPrefix(), EQUIPMENT_TAB);
		// The Candy Cache (Halloween batch 11) is a hidden candy bowl: the same block entity, under its own block.
		Block cache = registerBlock("candy_cache", CandyCacheBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.WOOD)
				.strength(1.0F).sound(SoundType.WOOD).noOcclusion().ignitedByLava().pushReaction(PushReaction.POPPED));
		registerItem("candy_cache", props -> new BlockItem(cache, props), new Item.Properties().useBlockDescriptionPrefix(), EQUIPMENT_TAB);
		CANDY_BOWL_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("candy_bowl"),
				FabricBlockEntityTypeBuilder.create(CandyBowlBlockEntity::new, bowl, cache).build());

		Block coffin = registerBlock("coffin", CoffinBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN)
				.strength(2.5F).sound(SoundType.WOOD).noOcclusion().ignitedByLava().pushReaction(PushReaction.POPPED));
		registerItem("coffin", props -> new BlockItem(coffin, props), new Item.Properties().useBlockDescriptionPrefix().stacksTo(1), EQUIPMENT_TAB);
		COFFIN_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("coffin"),
				FabricBlockEntityTypeBuilder.create(CoffinBlockEntity::new, coffin).build());

		Block portrait = registerBlock("haunted_portrait", HauntedPortraitBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.WOOD)
				.strength(1.0F).sound(SoundType.WOOD).noCollision().ignitedByLava().pushReaction(PushReaction.POPPED));
		registerItem("haunted_portrait", props -> new BlockItem(portrait, props), new Item.Properties().useBlockDescriptionPrefix(), BUILDING_TAB);
		HAUNTED_PORTRAIT_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("haunted_portrait"),
				FabricBlockEntityTypeBuilder.create(HauntedPortraitBlockEntity::new, portrait).build());

		Block fog = registerBlock("fog_machine", FogMachineBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
				.strength(3.5F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion().lightLevel(FogMachineBlock::light));
		registerItem("fog_machine", props -> new BlockItem(fog, props), new Item.Properties().useBlockDescriptionPrefix(), EQUIPMENT_TAB);
		FOG_MACHINE_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("fog_machine"),
				FabricBlockEntityTypeBuilder.create(FogMachineBlockEntity::new, fog).build());
		EnergyStorage.SIDED.registerForBlockEntity((entity, side) -> entity.energy(), FOG_MACHINE_ENTITY);
		Registry.register(BuiltInRegistries.PARTICLE_TYPE, Jugcraft.id("fog"), FOG);

		// Batch 2: the Luminaria, Floating Candles, the Skeleton Hand Sconce and Bat Bunting (soul-flame carvings are
		// the hand-carved and giant pumpkins' own).
		Block luminaria = registerBlock("luminaria", LuminariaBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_WHITE)
				.instabreak().sound(SoundType.WOOL).noOcclusion().lightLevel(LuminariaBlock::light).pushReaction(PushReaction.POPPED));
		registerItem("luminaria", props -> new BlockItem(luminaria, props), new Item.Properties().useBlockDescriptionPrefix(), BUILDING_TAB);
		Block candles = registerBlock("floating_candle", FloatingCandleBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.SAND)
				.strength(0.1F).sound(SoundType.CANDLE).noCollision().noOcclusion().lightLevel(FloatingCandleBlock::light)
				.pushReaction(PushReaction.POPPED));
		registerItem("floating_candle", props -> new BlockItem(candles, props), new Item.Properties().useBlockDescriptionPrefix(), BUILDING_TAB);
		FLOATING_CANDLE_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("floating_candle"),
				FabricBlockEntityTypeBuilder.create(FloatingCandleBlockEntity::new, candles).build());
		Block sconce = registerBlock("skeleton_hand_sconce", SkeletonHandSconceBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.SAND)
				.strength(0.5F).sound(SoundType.BONE_BLOCK).noCollision().lightLevel(SkeletonHandSconceBlock::light).pushReaction(PushReaction.POPPED));
		registerItem("skeleton_hand_sconce", props -> new BlockItem(sconce, props), new Item.Properties().useBlockDescriptionPrefix(), BUILDING_TAB);
		registerItem("bat_bunting", props -> new StringLightsItem(props, StringLightHookBlockEntity.Strand.BUNTING), new Item.Properties(),
				BUILDING_TAB);

		// Batch 3, the graveyard: a wrought-iron fence and gate, the crypt set, the Grave Mound, the Mourning Angel and
		// the Pop-Up Skeleton.
		BlockBehaviour.Properties iron = BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).forceSolidOn().strength(5.0F, 6.0F)
				.sound(SoundType.METAL).requiresCorrectToolForDrops();
		Block fence = registerBlock("cemetery_fence", FenceBlock::new, iron);
		Block gate = registerBlock("cemetery_gate", props -> new FenceGateBlock(WoodType.OAK, props), iron);
		BlockBehaviour.Properties crypt = BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BRICKS).mapColor(MapColor.DEEPSLATE);
		Block cryptStone = registerBlock("crypt_stone", Block::new, crypt);
		Block chiseled = registerBlock("chiseled_crypt_stone", Block::new, crypt);
		Block pillar = registerBlock("crypt_stone_pillar", RotatedPillarBlock::new, crypt);
		Block door = registerBlock("crypt_door", props -> new DoorBlock(BlockSetType.STONE, props), BlockBehaviour.Properties.of()
				.mapColor(MapColor.DEEPSLATE).strength(3.0F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.STONE).noOcclusion()
				.pushReaction(PushReaction.POPPED));
		Block mound = registerBlock("grave_mound", GraveMoundBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.DIRT).strength(0.6F)
				.sound(SoundType.ROOTED_DIRT).noOcclusion().randomTicks());
		Block angel = registerBlock("mourning_angel", MourningAngelBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.QUARTZ)
				.strength(2.0F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.CALCITE).noOcclusion().pushReaction(PushReaction.POPPED));
		Block skeleton = registerBlock("pop_up_skeleton", PopUpSkeletonBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.WOOD)
				.strength(1.5F).sound(SoundType.WOOD).noOcclusion().ignitedByLava());
		// The door places its upper half itself (DoorBlock.setPlacedBy), like the Scarecrow.
		for (Block block : List.of(fence, gate, cryptStone, chiseled, pillar, door, mound, angel, skeleton)) {
			String id = BuiltInRegistries.BLOCK.getKey(block).getPath();
			registerItem(id, props -> new BlockItem(block, props), new Item.Properties().useBlockDescriptionPrefix(), BUILDING_TAB);
		}
		SCARE_PROP_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("scare_prop"),
				FabricBlockEntityTypeBuilder.create(ScarePropBlockEntity::new, mound, skeleton).build());

		// Batch 4, the witch's cottage: the Bubbling Cauldron, the Apothecary Shelf, the Crystal Ball, the Grimoire Stand
		// and the Witch's Broom.
		Block cauldron = registerBlock("bubbling_cauldron", BubblingCauldronBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY)
				.strength(2.0F).requiresCorrectToolForDrops().sound(SoundType.METAL).noOcclusion().lightLevel(BubblingCauldronBlock::light));
		Block shelf = registerBlock("apothecary_shelf", ApothecaryShelfBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.WOOD)
				.strength(1.0F).sound(SoundType.WOOD).noOcclusion().ignitedByLava().pushReaction(PushReaction.POPPED));
		Block ball = registerBlock("crystal_ball", CrystalBallBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE)
				.strength(0.6F).sound(SoundType.AMETHYST).noOcclusion().lightLevel(CrystalBallBlock::light).pushReaction(PushReaction.POPPED));
		Block grimoire = registerBlock("grimoire_stand", GrimoireStandBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.WOOD)
				.strength(1.5F).sound(SoundType.WOOD).noOcclusion().ignitedByLava().lightLevel(state -> GrimoireStandBlock.LIGHT));
		Block broom = registerBlock("witchs_broom", WitchsBroomBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN)
				.strength(0.2F).sound(SoundType.WOOD).noCollision().noOcclusion().ignitedByLava().pushReaction(PushReaction.POPPED));
		for (Block block : List.of(cauldron, shelf, ball, grimoire, broom)) {
			String id = BuiltInRegistries.BLOCK.getKey(block).getPath();
			registerItem(id, props -> new BlockItem(block, props), new Item.Properties().useBlockDescriptionPrefix(), BUILDING_TAB);
		}
		// Fall addition 21: the hex brews' effects and their draughts, drunk like potions (the glass bottle back).
		Hexes.register();
		for (BubblingCauldronBlock.Brew hex : List.of(BubblingCauldronBlock.Brew.SHRINKING, BubblingCauldronBlock.Brew.GIANT,
				BubblingCauldronBlock.Brew.FLYING)) {
			registerItem(Hexes.draughtId(hex), props -> new HexDraughtItem(props, hex), new Item.Properties().component(DataComponents.CONSUMABLE,
					Consumables.defaultDrink().build()).usingConvertsTo(Items.GLASS_BOTTLE).stacksTo(16), FOOD_TAB);
		}
		// Fall additions 22, the flying broomstick: a witch's broom anointed with Flying Ointment, ridden and steered by
		// looking; the ointment is its fuel, kept on the item.
		BROOM_CHARGE = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Jugcraft.id("broom_charge"),
				DataComponentType.<Integer>builder().persistent(Codec.intRange(0, Broomstick.MAX_CHARGE)).networkSynchronized(ByteBufCodecs.VAR_INT)
						.build());
		registerItem(Broomstick.ITEM, FlyingBroomstickItem::new, new Item.Properties().stacksTo(1).component(BROOM_CHARGE,
				Broomstick.CHARGE_PER_OINTMENT), EQUIPMENT_TAB);
		FLYING_BROOMSTICK = entity(Broomstick.ITEM, EntityType.Builder.<Broomstick>of(Broomstick::new, MobCategory.MISC).noLootTable()
				.sized(0.9F, 0.6F).clientTrackingRange(10).updateInterval(1));
		UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> Broomstick.use(player, level, hand, entity));
		// Fall additions 23, full-moon werewolves: the werewolf (brown, snow and shadow) and where it comes from, wolfsbane
		// to ward it off (a wild flower, potted too), silver to hurt it (a dagger and arrows), and its pelts and rugs.
		WEREWOLF = entity("werewolf", EntityType.Builder.<Werewolf>of(Werewolf::new, MobCategory.MONSTER).sized(0.9F, 2.4F).eyeHeight(2.1F)
				.notInPeaceful().clientTrackingRange(10));
		FabricDefaultAttributeRegistry.register(WEREWOLF, Werewolf.createAttributes());
		Werewolves.register();
		Block wolfsbane = registerBlock(Werewolves.WOLFSBANE, props -> new FlowerBlock(MobEffects.POISON, Werewolves.STEW_SECONDS, props),
				BlockBehaviour.Properties.ofFullCopy(Blocks.DANDELION));
		registerItem(Werewolves.WOLFSBANE, props -> new BlockItem(wolfsbane, props), new Item.Properties().useBlockDescriptionPrefix()
				.compostable(COMPOST_MEDIUM), SEEDS_TAB);
		registerBlock("potted_" + Werewolves.WOLFSBANE, props -> new FlowerPotBlock(wolfsbane, props),
				BlockBehaviour.Properties.ofFullCopy(Blocks.POTTED_DANDELION));
		wildPatch("wolfsbane", ConventionalBiomeTags.IS_TAIGA, ConventionalBiomeTags.IS_FOREST);
		registerItem("silver_dagger", props -> new Item(props.sword(Werewolf.SILVER, 2.5F, -1.8F)), new Item.Properties(), TOOL_TAB);
		registerItem(Werewolves.SILVER_ARROW, ArrowItem::new, new Item.Properties(), TOOL_TAB);
		// Each kind of werewolf drops its own pelt, and two make its rug.
		for (Werewolf.Kind kind : Werewolf.Kind.values()) {
			registerItem(kind.pelt, Item::new, new Item.Properties(), INGREDIENT_TAB);
			MapColor colour = kind == Werewolf.Kind.SNOW ? MapColor.SNOW : kind == Werewolf.Kind.SHADOW ? MapColor.COLOR_BLACK : MapColor.COLOR_BROWN;
			Block rug = registerBlock(kind.rug, WerewolfRugBlock::new, BlockBehaviour.Properties.of().mapColor(colour)
					.strength(0.1F).sound(SoundType.WOOL).noOcclusion().ignitedByLava().pushReaction(PushReaction.POPPED));
			registerItem(kind.rug, props -> new BlockItem(rug, props), new Item.Properties().useBlockDescriptionPrefix(), BUILDING_TAB);
		}
		// Fall additions 24, squirrels and acorns: squirrels in the woods by day, which gather acorns and plant oaks;
		// acorns from oak leaves, planted as oak saplings or roasted.
		SQUIRREL = entity("squirrel", EntityType.Builder.<Squirrel>of(Squirrel::new, MobCategory.CREATURE).noLootTable().sized(0.4F, 0.5F).eyeHeight(0.35F)
				.clientTrackingRange(8));
		FabricDefaultAttributeRegistry.register(SQUIRREL, Squirrel.createAttributes());
		registerItem(Squirrel.ACORN, AcornItem::new, new Item.Properties().compostable(COMPOST_LOW), SEEDS_TAB);
		food("roasted_acorns", 3, 0.4F, COMPOST_MEDIUM);
		Squirrels.register();
		// Fall additions 25, the Pumpkling: a hand-carved pumpkin woken by a wisp or ectoplasm into a pet wearing its face.
		PUMPKLING = entity("pumpkling", EntityType.Builder.<Pumpkling>of(Pumpkling::new, MobCategory.MISC).noLootTable().sized(0.6F, 0.9F)
				.eyeHeight(0.6F).clientTrackingRange(10));
		FabricDefaultAttributeRegistry.register(PUMPKLING, Pumpkling.createAttributes());
		// Fall additions 26, the fall fair midway: the High Striker and its Carnival Mallet, Ring Toss and its rings, and
		// the plush prizes they give.
		Block striker = registerBlock("high_striker", HighStrikerBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED)
				.strength(2.0F).sound(SoundType.WOOD).noOcclusion().lightLevel(HighStrikerBlock::light).pushReaction(PushReaction.IMMOVEABLE)
				.ignitedByLava());
		registerItem("high_striker", props -> new BlockItem(striker, props), new Item.Properties().useBlockDescriptionPrefix(), BUILDING_TAB);
		HIGH_STRIKER_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("high_striker"),
				FabricBlockEntityTypeBuilder.create(HighStrikerBlockEntity::new, striker).build());
		registerItem(Midway.MALLET, props -> new Item(props.sword(ToolMaterial.WOOD, 1.0F, -3.4F)), new Item.Properties(), TOOL_TAB);
		Block ringToss = registerBlock("ring_toss", RingTossBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.WOOD)
				.strength(1.0F).sound(SoundType.WOOD).noOcclusion());
		registerItem("ring_toss", props -> new BlockItem(ringToss, props), new Item.Properties().useBlockDescriptionPrefix(), BUILDING_TAB);
		registerItem(TossRingItem.ID, TossRingItem::new, new Item.Properties().stacksTo(16), TOOL_TAB);
		TOSS_RING = entity(TossRingItem.ID, EntityType.Builder.<TossRing>of(TossRing::new, MobCategory.MISC).noLootTable()
				.sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(10));
		for (Midway.Plush plush : Midway.PLUSHES) {
			Block block = registerBlock(plush.id(), props -> new PlushBlock(props, plush), BlockBehaviour.Properties.of().mapColor(MapColor.WOOL)
					.strength(0.2F).sound(SoundType.WOOL).noOcclusion().ignitedByLava().pushReaction(PushReaction.POPPED));
			registerItem(plush.id(), props -> new BlockItem(block, props), new Item.Properties().useBlockDescriptionPrefix(), BUILDING_TAB);
		}
		Midway.register();
		// Fall additions 27, the Ferris wheel: its booth (the block, which raises the wheel, boards riders and takes kinetic
		// power) and the wheel itself.
		Block booth = registerBlock(FerrisWheelBlock.ID, FerrisWheelBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED)
				.strength(3.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().pushReaction(PushReaction.IMMOVEABLE));
		registerItem(FerrisWheelBlock.ID, props -> new BlockItem(booth, props), new Item.Properties().useBlockDescriptionPrefix(), BUILDING_TAB);
		FERRIS_WHEEL_BOOTH = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id(FerrisWheelBlock.ID),
				FabricBlockEntityTypeBuilder.create(FerrisWheelBlockEntity::new, booth).build());
		FERRIS_WHEEL = entity("ferris_wheel", EntityType.Builder.<FerrisWheel>of(FerrisWheel::new, MobCategory.MISC).noLootTable().noSummon()
				.sized(1.0F, 1.0F).clientTrackingRange(10).updateInterval(20));
		// Fall additions 28, the piñata party: the three piñatas (items that hang the piñata), the Piñata Stick and the
		// Blindfold, worn on the head and seen through as a dark cloth.
		for (Pinata.Kind kind : Pinata.Kind.values()) {
			registerItem(kind.item, props -> new PinataItem(props, kind), new Item.Properties().stacksTo(16), TOOL_TAB);
		}
		registerItem(Pinatas.STICK, props -> new Item(props.sword(ToolMaterial.WOOD, 1.0F, -2.8F)), new Item.Properties(), TOOL_TAB);
		Equippable blindfold = Equippable.builder(EquipmentSlot.HEAD).setEquipSound(SoundEvents.ARMOR_EQUIP_LEATHER)
				.setCameraOverlay(Jugcraft.id("misc/blindfold")).setAsset(ResourceKey.create(EquipmentAssets.ROOT_ID, Jugcraft.id("blindfold"))).build();
		registerItem(Pinatas.BLINDFOLD, Item::new, new Item.Properties().stacksTo(1).component(DataComponents.EQUIPPABLE, blindfold), EQUIPMENT_TAB);
		PINATA = entity("pinata", EntityType.Builder.<Pinata>of(Pinata::new, MobCategory.MISC).noLootTable().noSummon().sized(0.9F, 0.9F)
				.clientTrackingRange(10).updateInterval(20));
		Pinatas.register();
		// Fall additions 29, the hot-air balloon fiesta: three balloons (items that set one up, keeping its fuel), the
		// burner they are made with, pibals to read the winds aloft, and the Mooring Post to tether a balloon.
		BALLOON_FUEL = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Jugcraft.id("balloon_fuel"),
				DataComponentType.<Integer>builder().persistent(Codec.intRange(0, HotAirBalloon.MAX_FUEL)).networkSynchronized(ByteBufCodecs.VAR_INT)
						.build());
		for (HotAirBalloon.Kind kind : HotAirBalloon.Kind.values()) {
			registerItem(kind.item, props -> new HotAirBalloonItem(props, kind), new Item.Properties().stacksTo(1).component(BALLOON_FUEL, 0),
					TOOL_TAB);
		}
		registerItem("balloon_burner", Item::new, new Item.Properties(), INGREDIENT_TAB);
		registerItem("pibal", PibalItem::new, new Item.Properties().stacksTo(16), TOOL_TAB);
		Block mooringPost = registerBlock(MooringPostBlock.ID, MooringPostBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK)
				.strength(3.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion());
		registerItem(MooringPostBlock.ID, props -> new BlockItem(mooringPost, props), new Item.Properties().useBlockDescriptionPrefix(), BUILDING_TAB);
		HOT_AIR_BALLOON = entity("hot_air_balloon", EntityType.Builder.<HotAirBalloon>of(HotAirBalloon::new, MobCategory.MISC).noLootTable()
				.noSummon().sized((float) HotAirBalloon.BASKET, (float) HotAirBalloon.BASKET_HEIGHT).clientTrackingRange(16).updateInterval(1));
		PIBAL = entity("pibal", EntityType.Builder.<Pibal>of(Pibal::new, MobCategory.MISC).noLootTable().noSummon().sized(0.6F, 0.6F)
				.clientTrackingRange(16).updateInterval(1));
		Balloons.register();
		// Fall additions 30, the leaf blower: a dieselpunk electric leaf blower, charged at the Charging Station.
		registerItem(LeafBlowerItem.ID, LeafBlowerItem::new, new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)
				.component(JugcraftTools.ENERGY, 0L), TOOL_TAB);

		// Batch 5, the harvest party: the Bobbing for Apples Tub, the Pumpkin Crate, the Hay Bale Seat (and the seat
		// entity players sit on), the Autumn Wreath and the Leaf Piles.
		Block tub = registerBlock("bobbing_tub", BobbingTubBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.WOOD)
				.strength(1.5F).sound(SoundType.WOOD).noOcclusion().ignitedByLava());
		Block crate = registerBlock("pumpkin_crate", PumpkinCrateBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.WOOD)
				.strength(1.5F).sound(SoundType.WOOD).noOcclusion().ignitedByLava());
		PUMPKIN_CRATE_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("pumpkin_crate"),
				FabricBlockEntityTypeBuilder.create(PumpkinCrateBlockEntity::new, crate).build());
		Block bale = registerBlock("hay_bale_seat", HayBaleSeatBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW)
				.strength(0.5F).sound(SoundType.GRASS).noOcclusion().ignitedByLava());
		SEAT = entity("chair_seat", EntityType.Builder.<Seat>of(Seat::new, MobCategory.MISC).noLootTable().noSummon()
				.sized(0.5F, 0.1F).passengerAttachments(0.0F).clientTrackingRange(10).updateInterval(20));
		Block wreath = registerBlock("autumn_wreath", AutumnWreathBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE)
				.strength(0.2F).sound(SoundType.GRASS).noCollision().noOcclusion().ignitedByLava().pushReaction(PushReaction.POPPED));
		List<Block> piles = new ArrayList<>();
		for (String colour : LEAF_PILE_COLOURS) {
			piles.add(registerBlock(colour + "_leaf_pile", LeafPileBlock::new, BlockBehaviour.Properties.of().mapColor(
					colour.equals("red") ? MapColor.COLOR_RED : colour.equals("orange") ? MapColor.COLOR_ORANGE : MapColor.COLOR_YELLOW)
					.strength(0.1F).sound(SoundType.GRASS).noOcclusion().ignitedByLava().pushReaction(PushReaction.POPPED)));
		}
		for (Block block : List.of(tub, crate, bale, wreath)) {
			String id = BuiltInRegistries.BLOCK.getKey(block).getPath();
			registerItem(id, props -> new BlockItem(block, props), new Item.Properties().useBlockDescriptionPrefix(), BUILDING_TAB);
		}
		FlammableBlockRegistry fire = FlammableBlockRegistry.getDefaultInstance();
		fire.add(bale, 60, 20);
		fire.add(wreath, 30, 60);
		for (Block pile : piles) {
			String id = BuiltInRegistries.BLOCK.getKey(pile).getPath();
			registerItem(id, props -> new BlockItem(pile, props), new Item.Properties().useBlockDescriptionPrefix().compostable(COMPOST_LOW), BUILDING_TAB);
			fire.add(pile, 30, 60);
		}

		// Batch 6, the haunted house and yard: the Rocking Chair, the Lurking Eyes, the Silhouette Window, the Spooky Music
		// Box and the Giant Fake Spider.
		Block chair = registerBlock("rocking_chair", RockingChairBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN)
				.strength(1.5F).sound(SoundType.WOOD).noOcclusion().ignitedByLava());
		ROCKING_CHAIR_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("rocking_chair"),
				FabricBlockEntityTypeBuilder.<DecorationBlockEntity>create((pos, state) -> new DecorationBlockEntity(ROCKING_CHAIR_ENTITY, pos, state), chair).build());
		Block eyes = registerBlock("lurking_eyes", LurkingEyesBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.NONE)
				.strength(0.1F).sound(SoundType.GRASS).noCollision().noOcclusion().pushReaction(PushReaction.POPPED));
		LURKING_EYES_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("lurking_eyes"),
				FabricBlockEntityTypeBuilder.<DecorationBlockEntity>create((pos, state) -> new DecorationBlockEntity(LURKING_EYES_ENTITY, pos, state), eyes).build());
		Block window = registerBlock("silhouette_window", SilhouetteWindowBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE)
				.strength(0.5F).sound(SoundType.WOOD).noOcclusion().ignitedByLava());
		SILHOUETTE_WINDOW_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("silhouette_window"),
				FabricBlockEntityTypeBuilder.<DecorationBlockEntity>create((pos, state) -> new DecorationBlockEntity(SILHOUETTE_WINDOW_ENTITY, pos, state), window).build());
		Block box = registerBlock("music_box", MusicBoxBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK)
				.strength(1.0F).sound(SoundType.WOOD).noOcclusion().ignitedByLava().pushReaction(PushReaction.POPPED));
		MUSIC_BOX_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("music_box"),
				FabricBlockEntityTypeBuilder.create(MusicBoxBlockEntity::new, box).build());
		Block spider = registerBlock("giant_fake_spider", GiantFakeSpiderBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK)
				.strength(0.2F).sound(SoundType.WOOL).noCollision().noOcclusion().ignitedByLava().pushReaction(PushReaction.POPPED));
		GIANT_FAKE_SPIDER_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("giant_fake_spider"),
				FabricBlockEntityTypeBuilder.<DecorationBlockEntity>create((pos, state) -> new DecorationBlockEntity(GIANT_FAKE_SPIDER_ENTITY, pos, state), spider).build());
		for (Block block : List.of(chair, eyes, window, box, spider)) {
			String id = BuiltInRegistries.BLOCK.getKey(block).getPath();
			registerItem(id, props -> new BlockItem(block, props), new Item.Properties().useBlockDescriptionPrefix(), BUILDING_TAB);
		}

		// Batch 7, the haunted house inside: the Haunted Chandelier, the Phantom Pipe Organ, the Suit of Armor, the Dust
		// Sheet, the Spirit Mirror, Tattered Curtains and the Creepy Doll.
		Block chandelier = registerBlock("haunted_chandelier", HauntedChandelierBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
				.strength(1.0F).sound(SoundType.CHAIN).noOcclusion().lightLevel(HauntedChandelierBlock::light).pushReaction(PushReaction.POPPED));
		HAUNTED_CHANDELIER_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("haunted_chandelier"),
				FabricBlockEntityTypeBuilder.<DecorationBlockEntity>create((pos, state) -> new DecorationBlockEntity(HAUNTED_CHANDELIER_ENTITY, pos, state), chandelier).build());
		Block organ = registerBlock("phantom_pipe_organ", PipeOrganBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN)
				.strength(2.5F).sound(SoundType.WOOD).noOcclusion().ignitedByLava().pushReaction(PushReaction.IMMOVEABLE));
		PIPE_ORGAN_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("phantom_pipe_organ"),
				FabricBlockEntityTypeBuilder.create(PipeOrganBlockEntity::new, organ).build());
		Block armor = registerBlock("suit_of_armor", SuitOfArmorBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
				.strength(2.0F).sound(SoundType.METAL).noOcclusion().pushReaction(PushReaction.POPPED));
		SUIT_OF_ARMOR_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("suit_of_armor"),
				FabricBlockEntityTypeBuilder.<DecorationBlockEntity>create((pos, state) -> new DecorationBlockEntity(SUIT_OF_ARMOR_ENTITY, pos, state), armor).build());
		// The sheet's shape is whatever it covers (block entity data), so it must not be cached per block state.
		Block sheet = registerBlock(DUST_SHEET, DustSheetBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.SNOW)
				.strength(0.2F).sound(SoundType.WOOL).noOcclusion().dynamicShape().ignitedByLava().pushReaction(PushReaction.IMMOVEABLE));
		DUST_SHEET_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id(DUST_SHEET),
				FabricBlockEntityTypeBuilder.create(DustSheetBlockEntity::new, sheet).build());
		registerItem(DUST_SHEET, DustSheetItem::new, new Item.Properties(), BUILDING_TAB);
		// A sheet in hand goes over a chair or chest rather than sitting in it or opening it.
		UseBlockCallback.EVENT.register((player, level, hand, hit) -> player.getItemInHand(hand).is(item(DUST_SHEET))
				? DustSheetItem.tryCover(player, level, hand, hit.getBlockPos(), hit.getDirection()) : InteractionResult.PASS);
		Block mirror = registerBlock("spirit_mirror", SpiritMirrorBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.GOLD)
				.strength(0.6F).sound(SoundType.GLASS).noOcclusion().noCollision().pushReaction(PushReaction.POPPED));
		SPIRIT_MIRROR_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("spirit_mirror"),
				FabricBlockEntityTypeBuilder.<DecorationBlockEntity>create((pos, state) -> new DecorationBlockEntity(SPIRIT_MIRROR_ENTITY, pos, state), mirror).build());
		Block curtains = registerBlock("tattered_curtains", TatteredCurtainsBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.WOOL)
				.strength(0.2F).sound(SoundType.WOOL).noOcclusion().noCollision().ignitedByLava().pushReaction(PushReaction.POPPED));
		TATTERED_CURTAINS_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("tattered_curtains"),
				FabricBlockEntityTypeBuilder.<DecorationBlockEntity>create((pos, state) -> new DecorationBlockEntity(TATTERED_CURTAINS_ENTITY, pos, state), curtains).build());
		Block doll = registerBlock("creepy_doll", CreepyDollBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE)
				.strength(0.3F).sound(SoundType.WOOL).noOcclusion().ignitedByLava().pushReaction(PushReaction.POPPED));
		CREEPY_DOLL_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("creepy_doll"),
				FabricBlockEntityTypeBuilder.<DecorationBlockEntity>create((pos, state) -> new DecorationBlockEntity(CREEPY_DOLL_ENTITY, pos, state), doll).build());
		for (Block block : List.of(chandelier, organ, armor, mirror, curtains, doll)) {
			String id = BuiltInRegistries.BLOCK.getKey(block).getPath();
			registerItem(id, props -> new BlockItem(block, props), new Item.Properties().useBlockDescriptionPrefix(), BUILDING_TAB);
		}
		fire.add(curtains, 60, 100);
		fire.add(sheet, 30, 60);

		// Batch 8, the mad scientist and monsters: the Tesla Coil, the Lab Table, Specimen Jars, the Mummy Sarcophagus, the
		// Raven on a Perch and the Black Cat Figure.
		Block coil = registerBlock("tesla_coil", TeslaCoilBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE)
				.strength(2.0F).sound(SoundType.COPPER).noOcclusion().lightLevel(TeslaCoilBlock::light).pushReaction(PushReaction.IMMOVEABLE));
		TESLA_COIL_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("tesla_coil"),
				FabricBlockEntityTypeBuilder.create(TeslaCoilBlockEntity::new, coil).build());
		EnergyStorage.SIDED.registerForBlockEntity((entity, side) -> entity.energy(), TESLA_COIL_ENTITY);
		Block table = registerBlock("lab_table", LabTableBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
				.strength(2.0F).sound(SoundType.METAL).noOcclusion().pushReaction(PushReaction.IMMOVEABLE));
		LAB_TABLE_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("lab_table"),
				FabricBlockEntityTypeBuilder.<DecorationBlockEntity>create((pos, state) -> new DecorationBlockEntity(LAB_TABLE_ENTITY, pos, state), table).build());
		Block jar = registerBlock("specimen_jar", SpecimenJarBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GREEN)
				.strength(0.4F).sound(SoundType.GLASS).noOcclusion().lightLevel(state -> SpecimenJarBlock.LIGHT).pushReaction(PushReaction.POPPED));
		SPECIMEN_JAR_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("specimen_jar"),
				FabricBlockEntityTypeBuilder.<DecorationBlockEntity>create((pos, state) -> new DecorationBlockEntity(SPECIMEN_JAR_ENTITY, pos, state), jar).build());
		Block sarcophagus = registerBlock("mummy_sarcophagus", MummySarcophagusBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.SAND)
				.strength(2.0F).sound(SoundType.STONE).noOcclusion().pushReaction(PushReaction.POPPED));
		SARCOPHAGUS_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("mummy_sarcophagus"),
				FabricBlockEntityTypeBuilder.<DecorationBlockEntity>create((pos, state) -> new DecorationBlockEntity(SARCOPHAGUS_ENTITY, pos, state), sarcophagus).build());
		Block raven = registerBlock("raven_perch", RavenPerchBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK)
				.strength(0.5F).sound(SoundType.WOOD).noOcclusion().ignitedByLava().pushReaction(PushReaction.POPPED));
		RAVEN_PERCH_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("raven_perch"),
				FabricBlockEntityTypeBuilder.<DecorationBlockEntity>create((pos, state) -> new DecorationBlockEntity(RAVEN_PERCH_ENTITY, pos, state), raven).build());
		Block cat = registerBlock("black_cat_figure", BlackCatBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK)
				.strength(0.8F).sound(SoundType.DECORATED_POT).noOcclusion().pushReaction(PushReaction.POPPED));
		BLACK_CAT_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("black_cat_figure"),
				FabricBlockEntityTypeBuilder.create(BlackCatBlockEntity::new, cat).build());
		for (Block block : List.of(coil, table, jar, sarcophagus, raven, cat)) {
			String id = BuiltInRegistries.BLOCK.getKey(block).getPath();
			registerItem(id, props -> new BlockItem(block, props), new Item.Properties().useBlockDescriptionPrefix(), BUILDING_TAB);
		}

		// Batch 9, the yard and porch: the Yard Inflatables, the Porch Witch, Grasping Hands, the Poseable Skeleton, Bone
		// Wind Chimes, the Weathervanes, the Spooky Sign, the Haunted Archway and the Dead Hollow Tree.
		List<Block> yard = new ArrayList<>();
		List<Block> inflatables = new ArrayList<>();
		for (String design : INFLATABLE_DESIGNS) {
			inflatables.add(registerBlock("inflatable_" + design, props -> new InflatableBlock(props, design), BlockBehaviour.Properties.of()
					.mapColor(MapColor.WOOL).strength(0.5F).sound(SoundType.WOOL).noOcclusion().lightLevel(InflatableBlock::light)
					.pushReaction(PushReaction.POPPED)));
		}
		yard.addAll(inflatables);
		INFLATABLE_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("inflatable"),
				FabricBlockEntityTypeBuilder.<DecorationBlockEntity>create((pos, state) -> new DecorationBlockEntity(INFLATABLE_ENTITY, pos, state),
						inflatables.toArray(Block[]::new)).build());
		Block witch = registerBlock("porch_witch", PorchWitchBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE)
				.strength(1.0F).sound(SoundType.WOOD).noOcclusion().lightLevel(state -> state.getValue(PorchWitchBlock.HALF) == DoubleBlockHalf.LOWER ? 6 : 0)
				.pushReaction(PushReaction.POPPED));
		PORCH_WITCH_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("porch_witch"),
				FabricBlockEntityTypeBuilder.create(PorchWitchBlockEntity::new, witch).build());
		Block hands = registerBlock("grasping_hands", GraspingHandsBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.DIRT)
				.strength(0.5F).sound(SoundType.ROOTED_DIRT).noOcclusion().pushReaction(PushReaction.POPPED));
		Block poseable = registerBlock("poseable_skeleton", PoseableSkeletonBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.SAND)
				.strength(1.0F).sound(SoundType.BONE_BLOCK).noOcclusion().pushReaction(PushReaction.POPPED));
		Block chimes = registerBlock("bone_wind_chimes", BoneWindChimesBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.SAND)
				.strength(0.3F).sound(SoundType.BONE_BLOCK).noOcclusion().noCollision().pushReaction(PushReaction.POPPED));
		WIND_CHIMES_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("bone_wind_chimes"),
				FabricBlockEntityTypeBuilder.<DecorationBlockEntity>create((pos, state) -> new DecorationBlockEntity(WIND_CHIMES_ENTITY, pos, state), chimes).build());
		yard.addAll(List.of(witch, hands, poseable, chimes));
		List<Block> vanes = new ArrayList<>();
		for (String design : WEATHERVANE_DESIGNS) {
			vanes.add(registerBlock(design + "_weathervane", props -> new WeathervaneBlock(props, design), BlockBehaviour.Properties.of()
					.mapColor(MapColor.COLOR_BLACK).strength(2.0F).sound(SoundType.METAL).noOcclusion().pushReaction(PushReaction.POPPED)));
		}
		yard.addAll(vanes);
		WEATHERVANE_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("weathervane"),
				FabricBlockEntityTypeBuilder.<DecorationBlockEntity>create((pos, state) -> new DecorationBlockEntity(WEATHERVANE_ENTITY, pos, state),
						vanes.toArray(Block[]::new)).build());
		Block sign = registerBlock("spooky_sign", SpookySignBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.WOOD)
				.strength(1.0F).sound(SoundType.WOOD).noOcclusion().ignitedByLava().pushReaction(PushReaction.POPPED));
		SPOOKY_SIGN_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("spooky_sign"),
				FabricBlockEntityTypeBuilder.create(SpookySignBlockEntity::new, sign).build());
		Block archway = registerBlock("haunted_archway", HauntedArchwayBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.STONE)
				.strength(2.0F, 6.0F).sound(SoundType.STONE).noOcclusion().lightLevel(HauntedArchwayBlock::light).pushReaction(PushReaction.IMMOVEABLE));
		Block tree = registerBlock("dead_hollow_tree", DeadHollowTreeBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN)
				.strength(2.0F).sound(SoundType.WOOD).noOcclusion().ignitedByLava().lightLevel(DeadHollowTreeBlock::light)
				.pushReaction(PushReaction.IMMOVEABLE));
		yard.addAll(List.of(sign, archway, tree));
		for (Block block : yard) {
			String id = BuiltInRegistries.BLOCK.getKey(block).getPath();
			registerItem(id, props -> new BlockItem(block, props), new Item.Properties().useBlockDescriptionPrefix(), BUILDING_TAB);
		}
		fire.add(sign, 5, 20);
		fire.add(tree, 5, 5);

		// Batch 10, lighting and glow: the Black Light and Glow Paint, the Witch Fire Brazier, the Shadow Puppet Lamp, the
		// Mini Pumpkin Stack and the Floating Witch Hat.
		Block blackLight = registerBlock("black_light", BlackLightBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE)
				.strength(0.5F).sound(SoundType.GLASS).noOcclusion().noCollision().lightLevel(BlackLightBlock::light).pushReaction(PushReaction.POPPED));
		BLACK_LIGHT_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("black_light"),
				FabricBlockEntityTypeBuilder.create(BlackLightBlockEntity::new, blackLight).build());
		Block glowPaint = registerBlock("glow_paint", GlowPaintBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.NONE)
				.strength(0.1F).sound(SoundType.SLIME_BLOCK).noOcclusion().noCollision().lightLevel(state -> 1).pushReaction(PushReaction.POPPED));
		GLOW_PAINT_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("glow_paint"),
				FabricBlockEntityTypeBuilder.<DecorationBlockEntity>create((pos, state) -> new DecorationBlockEntity(GLOW_PAINT_ENTITY, pos, state), glowPaint).build());
		Block brazier = registerBlock("witch_fire_brazier", WitchFireBrazierBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
				.strength(2.0F).sound(SoundType.METAL).noOcclusion().lightLevel(WitchFireBrazierBlock::light).pushReaction(PushReaction.POPPED));
		BRAZIER_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("witch_fire_brazier"),
				FabricBlockEntityTypeBuilder.<DecorationBlockEntity>create((pos, state) -> new DecorationBlockEntity(BRAZIER_ENTITY, pos, state), brazier).build());
		Block lamp = registerBlock("shadow_puppet_lamp", ShadowPuppetLampBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.WOOD)
				.strength(0.5F).sound(SoundType.WOOD).noOcclusion().lightLevel(ShadowPuppetLampBlock::light).pushReaction(PushReaction.POPPED));
		SHADOW_LAMP_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("shadow_puppet_lamp"),
				FabricBlockEntityTypeBuilder.<DecorationBlockEntity>create((pos, state) -> new DecorationBlockEntity(SHADOW_LAMP_ENTITY, pos, state), lamp).build());
		Block miniPumpkins = registerBlock("mini_pumpkin_stack", MiniPumpkinStackBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE)
				.strength(1.0F).sound(SoundType.WOOD).noOcclusion().lightLevel(MiniPumpkinStackBlock::light).pushReaction(PushReaction.POPPED));
		Block hat = registerBlock("floating_witch_hat", FloatingWitchHatBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK)
				.strength(0.2F).sound(SoundType.WOOL).noOcclusion().noCollision().lightLevel(FloatingWitchHatBlock::light).pushReaction(PushReaction.POPPED));
		FLOATING_HAT_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("floating_witch_hat"),
				FabricBlockEntityTypeBuilder.<DecorationBlockEntity>create((pos, state) -> new DecorationBlockEntity(FLOATING_HAT_ENTITY, pos, state), hat).build());
		for (Block block : List.of(blackLight, glowPaint, brazier, lamp, miniPumpkins, hat)) {
			String id = BuiltInRegistries.BLOCK.getKey(block).getPath();
			registerItem(id, props -> new BlockItem(block, props), new Item.Properties().useBlockDescriptionPrefix(), BUILDING_TAB);
		}
		fire.add(lamp, 5, 20);
		fire.add(hat, 30, 60);

		// Batch 11, party games: the Jump-Scare Trap, the Costume Runway and Judges' Table, Pumpkin Bowling, the Monster
		// Mash Dance Floor, the Ghost Bell and the Fortune Teller's Table (the Candy Cache is with the Candy Bowl).
		Block scare = registerBlock("jump_scare_trap", JumpScareTrapBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.WOOD)
				.strength(1.5F).sound(SoundType.WOOD).noOcclusion().ignitedByLava().pushReaction(PushReaction.POPPED));
		JUMP_SCARE_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("jump_scare_trap"),
				FabricBlockEntityTypeBuilder.create(JumpScareTrapBlockEntity::new, scare).build());
		Block runway = registerBlock("costume_runway", CostumeRunwayBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED)
				.strength(0.1F).sound(SoundType.WOOL).noOcclusion().ignitedByLava().pushReaction(PushReaction.POPPED));
		Block judges = registerBlock("judges_table", JudgesTableBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED)
				.strength(1.5F).sound(SoundType.WOOD).noOcclusion().ignitedByLava().pushReaction(PushReaction.POPPED));
		JUDGES_TABLE_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("judges_table"),
				FabricBlockEntityTypeBuilder.create(JudgesTableBlockEntity::new, judges).build());
		UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> JudgesTableBlockEntity.onUseEntity(player, level, hand, entity));
		Block pin = registerBlock("skeleton_pin", SkeletonPinBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.SAND)
				.strength(0.3F).sound(SoundType.BONE_BLOCK).noOcclusion().noCollision().pushReaction(PushReaction.POPPED));
		Block scoreboard = registerBlock("bowling_scoreboard", BowlingScoreboardBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK)
				.strength(1.0F).sound(SoundType.WOOD).noOcclusion().ignitedByLava().pushReaction(PushReaction.POPPED));
		BOWLING_SCOREBOARD_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("bowling_scoreboard"),
				FabricBlockEntityTypeBuilder.create(BowlingScoreboardBlockEntity::new, scoreboard).build());
		BOWLING_PUMPKIN = entity("bowling_pumpkin", EntityType.Builder.<BowlingPumpkin>of(BowlingPumpkin::new, MobCategory.MISC).noLootTable()
				.sized(0.5F, 0.5F).clientTrackingRange(8).updateInterval(2));
		Block dance = registerBlock("dance_floor", DanceFloorBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK)
				.strength(0.6F).sound(SoundType.GLASS).lightLevel(DanceFloorBlock::light));
		DANCE_FLOOR_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("dance_floor"),
				FabricBlockEntityTypeBuilder.<DecorationBlockEntity>create((pos, state) -> new DecorationBlockEntity(DANCE_FLOOR_ENTITY, pos, state), dance).build());
		Block ghostBell = registerBlock("ghost_bell", GhostBellBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
				.strength(2.0F).sound(SoundType.METAL).noOcclusion().pushReaction(PushReaction.POPPED));
		GHOST_BELL_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("ghost_bell"),
				FabricBlockEntityTypeBuilder.create(GhostBellBlockEntity::new, ghostBell).build());
		AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) -> GhostBellBlockEntity.onAttack(player, level, hand, entity));
		Block fortune = registerBlock("fortune_teller_table", FortuneTellerTableBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE)
				.strength(1.5F).sound(SoundType.WOOD).noOcclusion().ignitedByLava().pushReaction(PushReaction.POPPED));
		FORTUNE_TABLE_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("fortune_teller_table"),
				FabricBlockEntityTypeBuilder.create(FortuneTellerTableBlockEntity::new, fortune).build());
		for (Block block : List.of(scare, runway, judges, pin, scoreboard, dance, ghostBell, fortune)) {
			String id = BuiltInRegistries.BLOCK.getKey(block).getPath();
			registerItem(id, props -> new BlockItem(block, props), new Item.Properties().useBlockDescriptionPrefix(), BUILDING_TAB);
		}
		registerItem("best_costume_ribbon", Item::new, new Item.Properties().rarity(Rarity.UNCOMMON), TOOL_TAB);
		registerItem("bowling_pumpkin", BowlingPumpkinItem::new, new Item.Properties().stacksTo(16), TOOL_TAB);
		fire.add(runway, 30, 60);

		// Batch 12, night events: Toilet Paper Rolls and their streamers, the Haunted Hayride and the Halloween Bonfire
		// with marshmallows to toast (the trick-or-treaters come to Candy Bowls, TrickOrTreaters).
		Block streamer = registerBlock("toilet_paper_streamer", ToiletPaperStreamerBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.SNOW)
				.instabreak().noCollision().noOcclusion().replaceable().sound(SoundType.WOOL).pushReaction(PushReaction.POPPED));
		registerItem("toilet_paper_streamer", props -> new BlockItem(streamer, props), new Item.Properties().useBlockDescriptionPrefix(), BUILDING_TAB);
		registerItem("toilet_paper_roll", ToiletPaperRollItem::new, new Item.Properties().stacksTo(16), TOOL_TAB);
		TOILET_PAPER_ROLL = entity("toilet_paper_roll", EntityType.Builder.<ToiletPaperRoll>of(ToiletPaperRoll::new, MobCategory.MISC).noLootTable()
				.sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(10));
		HAUNTED_HAYRIDE = entity("haunted_hayride", EntityType.Builder.<HauntedHayride>of(HauntedHayride::new, MobCategory.MISC).noLootTable()
				.sized(0.98F, 0.7F).passengerAttachments(0.1875F).clientTrackingRange(8));
		registerItem("haunted_hayride", props -> new MinecartItem(HAUNTED_HAYRIDE, props), new Item.Properties().stacksTo(1), TOOL_TAB);
		UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> HauntedHayride.board(player, level, hand, entity));
		Block bonfire = registerBlock("halloween_bonfire", HalloweenBonfireBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.PODZOL)
				.strength(2.0F).sound(SoundType.WOOD).noOcclusion().lightLevel(HalloweenBonfireBlock::light).pushReaction(PushReaction.IMMOVEABLE));
		registerItem("halloween_bonfire", props -> new BlockItem(bonfire, props), new Item.Properties().useBlockDescriptionPrefix(), BUILDING_TAB);
		BONFIRE_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("halloween_bonfire"),
				FabricBlockEntityTypeBuilder.create(HalloweenBonfireBlockEntity::new, bonfire).build());
		registerItem("marshmallow_on_a_stick", MarshmallowStickItem::new, new Item.Properties().stacksTo(16), FOOD_TAB);

		// Batch 13, treats: the Witch's Brew Punch Bowl, the Barmbrack with its hidden ring, and Giant Candy props (the
		// treats themselves are foods, registered with the others).
		Block punch = registerBlock("witchs_brew_punch_bowl", PunchBowlBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GREEN)
				.strength(0.5F).sound(SoundType.GLASS).noOcclusion().lightLevel(PunchBowlBlock::light).pushReaction(PushReaction.POPPED));
		Block brack = registerBlock("barmbrack", BarmbrackBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN)
				.strength(0.5F).sound(SoundType.WOOL).noOcclusion().pushReaction(PushReaction.POPPED));
		BARMBRACK_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("barmbrack"),
				FabricBlockEntityTypeBuilder.create(BarmbrackBlockEntity::new, brack).build());
		Block candy = registerBlock("giant_candy", GiantCandyBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE)
				.strength(0.8F).sound(SoundType.STONE).noOcclusion());
		for (Block block : List.of(punch, candy)) {
			String id = BuiltInRegistries.BLOCK.getKey(block).getPath();
			registerItem(id, props -> new BlockItem(block, props), new Item.Properties().useBlockDescriptionPrefix(), BUILDING_TAB);
		}
		registerItem("barmbrack", props -> new BlockItem(brack, props), new Item.Properties().useBlockDescriptionPrefix().stacksTo(1), FOOD_TAB);
		registerItem("barmbrack_ring", Item::new, new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON), TOOL_TAB);

		// Batch 14, costumes: outfits worn on the head like the Ghost Sheet (their equipment assets have no layers, so
		// nothing is drawn by the armor layer or on the head; the client's CostumeLayer dresses the whole wearer), and the
		// Costume Trunk to keep them in.
		for (String outfit : OUTFITS) {
			Equippable worn = Equippable.builder(EquipmentSlot.HEAD).setEquipSound(SoundEvents.ARMOR_EQUIP_LEATHER)
					.setAsset(ResourceKey.create(EquipmentAssets.ROOT_ID, Jugcraft.id(outfit))).build();
			registerItem(outfit, Item::new, new Item.Properties().stacksTo(1).component(DataComponents.EQUIPPABLE, worn), TOOL_TAB);
		}
		Block trunk = registerBlock("costume_trunk", CostumeTrunkBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN)
				.strength(2.5F).sound(SoundType.WOOD).noOcclusion().ignitedByLava());
		COSTUME_TRUNK_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("costume_trunk"),
				FabricBlockEntityTypeBuilder.create(CostumeTrunkBlockEntity::new, trunk).build());
		registerItem("costume_trunk", props -> new BlockItem(trunk, props), new Item.Properties().useBlockDescriptionPrefix(), BUILDING_TAB);

		// Fall additions 1, the chandlery: the Wax Melting Pot and the Aura Candles dipped from it.
		CANDLE_MIX = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Jugcraft.id("candle"),
				DataComponentType.<CandleMix>builder().persistent(CandleMix.CODEC).networkSynchronized(CandleMix.STREAM_CODEC).build());
		Block waxPot = registerBlock("wax_melting_pot", WaxPotBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE)
				.strength(2.5F).sound(SoundType.COPPER).noOcclusion());
		WAX_POT_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("wax_melting_pot"),
				FabricBlockEntityTypeBuilder.create(WaxPotBlockEntity::new, waxPot).build());
		registerItem("wax_melting_pot", props -> new BlockItem(waxPot, props), new Item.Properties().useBlockDescriptionPrefix(), BUILDING_TAB);
		Block candle = registerBlock("aura_candle", AuraCandleBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.SAND).strength(0.1F)
				.sound(SoundType.CANDLE).noOcclusion().lightLevel(AuraCandleBlock::light).pushReaction(PushReaction.POPPED));
		AURA_CANDLE_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("aura_candle"),
				FabricBlockEntityTypeBuilder.create(AuraCandleBlockEntity::new, candle).build());
		registerItem("aura_candle", props -> new AuraCandleItem(candle, props), new Item.Properties().useBlockDescriptionPrefix().stacksTo(16),
				TOOL_TAB);

		// Fall additions 2, the cider mill: the Cider Press and the Cider Barrel the cider ages in.
		BARREL_CIDER = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Jugcraft.id("barrel_cider"),
				DataComponentType.<BarrelCider>builder().persistent(BarrelCider.CODEC).networkSynchronized(BarrelCider.STREAM_CODEC).build());
		Block press = registerBlock("cider_press", CiderPressBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5F)
				.sound(SoundType.WOOD).noOcclusion().ignitedByLava());
		CIDER_PRESS_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("cider_press"),
				FabricBlockEntityTypeBuilder.create(CiderPressBlockEntity::new, press).build());
		registerItem("cider_press", props -> new BlockItem(press, props), new Item.Properties().useBlockDescriptionPrefix(), BUILDING_TAB);
		Block barrel = registerBlock("cider_barrel", CiderBarrelBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5F)
				.sound(SoundType.WOOD).noOcclusion().ignitedByLava());
		CIDER_BARREL_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("cider_barrel"),
				FabricBlockEntityTypeBuilder.create(CiderBarrelBlockEntity::new, barrel).build());
		registerItem("cider_barrel", props -> new CiderBarrelItem(barrel, props), new Item.Properties().useBlockDescriptionPrefix(), BUILDING_TAB);

		// Fall additions 3, the preserves pantry: Mason Jars, cider vinegar, preserves cooked into jars in the Cooking Pot, the
		// Canning Kettle that seals them and the Pantry Shelf that shows them off.
		SEALED = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Jugcraft.id("sealed"),
				DataComponentType.<Boolean>builder().persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).build());
		JAR_CONTENTS = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Jugcraft.id("jar_contents"),
				DataComponentType.<JarContents>builder().persistent(JarContents.CODEC).networkSynchronized(JarContents.STREAM_CODEC).build());
		registerItem("mason_jar", Item::new, new Item.Properties().stacksTo(16), INGREDIENT_TAB);
		registerItem("cider_vinegar", Item::new, new Item.Properties().craftRemainder(Items.GLASS_BOTTLE).stacksTo(16), INGREDIENT_TAB);
		preserve("sweet_berry_jam", 3, 0.4F, null, 0, 0x9A1E3A);
		preserve("apple_butter", 4, 0.5F, null, 0, 0x7A3A14);
		preserve("pumpkin_butter", 4, 0.5F, null, 0, 0xC8701E);
		preserve("cranberry_preserves", 3, 0.4F, null, 0, 0xB0122E);
		preserve("glow_berry_jelly", 2, 0.3F, MobEffects.NIGHT_VISION, 30, 0xF0B030);
		preserve("pickled_beets", 2, 0.4F, null, 0, 0x7A1040);
		preserve("pickled_peppers", 2, 0.4F, MobEffects.FIRE_RESISTANCE, 15, 0x4A8A2A);
		preserve("corn_relish", 3, 0.5F, null, 0, 0xE0B828);
		Block kettle = registerBlock("canning_kettle", CanningKettleBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLUE)
				.strength(2.0F).sound(SoundType.LANTERN).noOcclusion());
		CANNING_KETTLE_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("canning_kettle"),
				FabricBlockEntityTypeBuilder.create(CanningKettleBlockEntity::new, kettle).build());
		registerItem("canning_kettle", props -> new BlockItem(kettle, props), new Item.Properties().useBlockDescriptionPrefix(), BUILDING_TAB);
		Block pantryShelf = registerBlock("pantry_shelf", PantryShelfBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.WOOD)
				.strength(2.0F).sound(SoundType.WOOD).noOcclusion().ignitedByLava());
		PANTRY_SHELF_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("pantry_shelf"),
				FabricBlockEntityTypeBuilder.create(PantryShelfBlockEntity::new, pantryShelf).build());
		registerItem("pantry_shelf", props -> new BlockItem(pantryShelf, props), new Item.Properties().useBlockDescriptionPrefix(), BUILDING_TAB);

		// Fall additions 4, crows and working scarecrows: crows come to fields by day; scarecrows keep them off.
		CROW = entity("crow", EntityType.Builder.<Crow>of(Crow::new, MobCategory.AMBIENT).sized(0.5F, 0.6F).eyeHeight(0.45F)
				.clientTrackingRange(8));
		FabricDefaultAttributeRegistry.register(CROW, Crow.createAttributes());
		Crows.register();

		// Fall additions 5, spooky fireworks: rockets that burst into pictures in sparks, and the Show Launcher.
		TWINKLE = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Jugcraft.id("twinkle"),
				DataComponentType.<Boolean>builder().persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).build());
		for (FireworkShape shape : FireworkShape.values()) {
			registerItem(shape.item(), props -> new SpookyFireworkItem(shape, props),
					new Item.Properties().component(DataComponents.FIREWORKS, new Fireworks(1, List.of())), EQUIPMENT_TAB);
		}
		SPOOKY_ROCKET = entity("spooky_rocket", EntityType.Builder.<SpookyRocket>of(SpookyRocket::new, MobCategory.MISC).noLootTable()
				.sized(0.25F, 0.25F).clientTrackingRange(8).updateInterval(10));
		Registry.register(BuiltInRegistries.PARTICLE_TYPE, Jugcraft.id("spooky_spark"), SPOOKY_SPARK);
		PayloadTypeRegistry.clientboundPlay().register(SpookyBurstPayload.TYPE, SpookyBurstPayload.CODEC);
		Block launcher = registerBlock("show_launcher", ShowLauncherBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE)
				.strength(2.0F).sound(SoundType.WOOD).noOcclusion().ignitedByLava());
		SHOW_LAUNCHER_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("show_launcher"),
				FabricBlockEntityTypeBuilder.create(ShowLauncherBlockEntity::new, launcher).build());
		registerItem("show_launcher", props -> new BlockItem(launcher, props), new Item.Properties().useBlockDescriptionPrefix(), EQUIPMENT_TAB);
		SpookyFireworkItem.registerDispensing();

		// Fall additions 6, the sky lantern festival: sky lanterns to let go together, and mooncakes for a full moon.
		registerItem("sky_lantern", SkyLanternItem::new, new Item.Properties().stacksTo(16), EQUIPMENT_TAB);
		SKY_LANTERN = entity("sky_lantern", EntityType.Builder.<SkyLantern>of(SkyLantern::new, MobCategory.MISC).noLootTable()
				.sized(0.5F, 0.75F).clientTrackingRange(10).updateInterval(10));
		SkyLanterns.register();
		for (String mooncake : MOONCAKES) {
			registerItem(mooncake, MooncakeItem::new, new Item.Properties().food(nourishment(3, 0.6F)).stacksTo(16), FOOD_TAB);
		}

		// Fall additions 7, the harvest feast: a long table to serve dishes on, blessing those who eat at it together.
		Block feastTable = registerBlock("feast_table", FeastTableBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.WOOD)
				.strength(2.0F).sound(SoundType.WOOD).noOcclusion().ignitedByLava());
		FEAST_TABLE_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("feast_table"),
				FabricBlockEntityTypeBuilder.create(FeastTableBlockEntity::new, feastTable).build());
		registerItem("feast_table", props -> new BlockItem(feastTable, props), new Item.Properties().useBlockDescriptionPrefix(), BUILDING_TAB);

		// Fall additions 8, the corn maze: a gate that plants a maze of corn, times runners and keeps a board.
		registerBlock("maze_corn", MazeCornBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).strength(0.3F)
				.sound(SoundType.CROP).noOcclusion().pushReaction(PushReaction.POPPED).ignitedByLava());
		Block mazeGate = registerBlock("corn_maze_gate", CornMazeGateBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.WOOD)
				.strength(1.5F).sound(SoundType.WOOD).noOcclusion().ignitedByLava());
		CORN_MAZE_GATE_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("corn_maze_gate"),
				FabricBlockEntityTypeBuilder.create(CornMazeGateBlockEntity::new, mazeGate).build());
		registerItem("corn_maze_gate", props -> new BlockItem(mazeGate, props), new Item.Properties().useBlockDescriptionPrefix(), EQUIPMENT_TAB);
		registerBlock("corn_maze_finish", CornMazeFinishBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(1.5F)
				.sound(SoundType.WOOD).noOcclusion().ignitedByLava());

		// Fall additions 9, ghost hunting: restless spirits rise from graves at night; a Spirit Lantern reveals them, and a
		// glass bottle catches one as Ectoplasm, the Ghostly candle scent.
		registerItem("spirit_lantern", SpiritLanternItem::new, new Item.Properties().stacksTo(1), EQUIPMENT_TAB);
		registerItem("ectoplasm", Item::new, new Item.Properties().craftRemainder(Items.GLASS_BOTTLE).stacksTo(16), INGREDIENT_TAB);
		RESTLESS_SPIRIT = entity("restless_spirit", EntityType.Builder.<RestlessSpirit>of(RestlessSpirit::new, MobCategory.AMBIENT).noLootTable()
				.sized(0.6F, 1.4F).eyeHeight(1.15F).fireImmune().clientTrackingRange(8));
		FabricDefaultAttributeRegistry.register(RESTLESS_SPIRIT, RestlessSpirit.createAttributes());

		// Fall additions 10, face paint: a kit that paints a design on a player's face, which counts as a costume.
		FACE_PAINT_DESIGN = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Jugcraft.id("face_paint_design"),
				DataComponentType.<FacePaint.Design>builder().persistent(FacePaint.Design.CODEC).networkSynchronized(FacePaint.Design.STREAM_CODEC).build());
		registerItem("face_paint_kit", FacePaintKitItem::new, new Item.Properties().durability(FacePaintKitItem.USES)
				.component(FACE_PAINT_DESIGN, FacePaint.Design.SKULL), EQUIPMENT_TAB);
		FacePaint.register();

		// Fall additions 11, the candy kitchen: the Candy Kettle, the Candy Tray its candy is poured onto, and its candies.
		CANDY_BATCH = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Jugcraft.id("candy_batch"),
				DataComponentType.<CandyBatch>builder().persistent(CandyBatch.CODEC).networkSynchronized(CandyBatch.STREAM_CODEC).build());
		Block candyKettle = registerBlock("candy_kettle", CandyKettleBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE)
				.strength(2.5F).sound(SoundType.COPPER).noOcclusion());
		CANDY_KETTLE_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("candy_kettle"),
				FabricBlockEntityTypeBuilder.create(CandyKettleBlockEntity::new, candyKettle).build());
		registerItem("candy_kettle", props -> new BlockItem(candyKettle, props), new Item.Properties().useBlockDescriptionPrefix(), BUILDING_TAB);
		registerItem("candy_tray", CandyTrayItem::new, new Item.Properties().stacksTo(16), TOOL_TAB);
		candy("rock_candy", 2, 0.1F);
		candy("salt_water_taffy", 2, 0.2F);
		candy("hard_candy", 1, 0.1F);
		candy("lollipop", 2, 0.1F);
		candy("fudge", 3, 0.3F);
		candy("cream_caramel", 2, 0.2F);
		candy("toffee", 2, 0.2F);
		candy("burnt_sugar", 1, 0.0F);

		// Fall additions 12, autumn foraging: five wild mushrooms that spread in the shade and sprout fairy rings under a
		// full moon (the jack o'lantern mushroom glows), the Foraging Basket, and what they cook into.
		for (String id : WILD_MUSHROOMS) {
			int light = id.equals("jack_o_lantern_mushroom") ? 9 : 0;
			Block mushroom = registerBlock(id, WildMushroomBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.BROWN_MUSHROOM)
					.lightLevel(state -> light).randomTicks());
			registerItem(id, props -> new BlockItem(mushroom, props), new Item.Properties().useBlockDescriptionPrefix().compostable(COMPOST_MEDIUM),
					INGREDIENT_TAB);
		}
		registerItem("foraging_basket", ForagingBasketItem::new, new Item.Properties().stacksTo(1)
				.component(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY), TOOL_TAB);
		food("sauteed_chanterelles", 5, 0.6F, COMPOST_MEDIUM_HIGH);
		food("roasted_porcini", 6, 0.6F, COMPOST_MEDIUM_HIGH);
		food("fried_puffball", 4, 0.5F, COMPOST_MEDIUM_HIGH);
		stew("foragers_stew", 10, 0.8F);
		FairyRings.register();
		// Bunker interiors (batch 59): Trench Stew, cooked in a Cooking Pot (on a Field Kitchen or any heat source).
		stew("trench_stew", 10, 0.8F, MobEffects.REGENERATION, 5);

		// Fall additions 13, the Bat House: a roost that lets bats out at dusk and takes them in at dawn, and the guano they
		// leave, a fertilizer (superphosphate's rule over a 3x3 patch, one dose) and a source of phosphate.
		Block batHouse = registerBlock("bat_house", BatHouseBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0F)
				.sound(SoundType.WOOD).noOcclusion().ignitedByLava());
		BAT_HOUSE_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("bat_house"),
				FabricBlockEntityTypeBuilder.create(BatHouseBlockEntity::new, batHouse).build());
		registerItem("bat_house", props -> new BlockItem(batHouse, props), new Item.Properties().useBlockDescriptionPrefix(), BUILDING_TAB);
		registerItem("bat_guano", props -> new FertilizerItem(props, GUANO_RADIUS, GUANO_DOSES), new Item.Properties()
				.compostable(COMPOST_MEDIUM_HIGH), INGREDIENT_TAB);

		// Fall additions 14, the Hay Golem: built from four hay bales and a carved head; a walking scarecrow that tends the
		// crops round its post and carries the harvest home.
		HAY_GOLEM = entity("hay_golem", EntityType.Builder.<HayGolem>of(HayGolem::new, MobCategory.MISC).sized(0.9F, 2.5F).eyeHeight(2.2F)
				.clientTrackingRange(10));
		FabricDefaultAttributeRegistry.register(HAY_GOLEM, HayGolem.createAttributes());
		UseBlockCallback.EVENT.register(HayGolem::onUseBlock);

		// Fall additions 15, knitting: the Spinning Wheel spins wool into yarn; Knitting Needles knit yarn into beanies,
		// socks and sweaters, worn (dyed the yarn's colour) to keep warm by a campfire.
		KNITTING = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Jugcraft.id("knitting"),
				DataComponentType.<KnittingWork>builder().persistent(KnittingWork.CODEC).networkSynchronized(KnittingWork.STREAM_CODEC).build());
		Block spinningWheel = registerBlock("spinning_wheel", SpinningWheelBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.WOOD)
				.strength(2.0F).sound(SoundType.WOOD).noOcclusion().ignitedByLava());
		SPINNING_WHEEL_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("spinning_wheel"),
				FabricBlockEntityTypeBuilder.create(SpinningWheelBlockEntity::new, spinningWheel).build());
		registerItem("spinning_wheel", props -> new BlockItem(spinningWheel, props), new Item.Properties().useBlockDescriptionPrefix(), BUILDING_TAB);
		registerItem("yarn", Item::new, new Item.Properties().component(DataComponents.DYED_COLOR, new DyedItemColor(Knitting.UNDYED)),
				INGREDIENT_TAB);
		registerItem("knitting_needles", KnittingNeedlesItem::new, new Item.Properties().durability(NEEDLES_DURABILITY)
				.component(KNITTING, KnittingWork.NONE), TOOL_TAB);
		for (Knitwear knit : Knitwear.values()) {
			Equippable worn = Equippable.builder(knit.slot).setEquipSound(SoundEvents.ARMOR_EQUIP_LEATHER)
					.setAsset(ResourceKey.create(EquipmentAssets.ROOT_ID, Jugcraft.id(knit.asset))).build();
			registerItem(knit.item, Item::new, new Item.Properties().stacksTo(1).component(DataComponents.EQUIPPABLE, worn)
					.component(DataComponents.DYED_COLOR, new DyedItemColor(Knitting.UNDYED)), EQUIPMENT_TAB);
		}
		Knitting.register();

		// Fall additions 16, pie baking: a brick Hearth Oven fed with fuel bakes raw pies (pastry, a filling and sugar) into
		// pies placed like cakes, eaten or cut a slice at a time; left in too long they burn.
		Block oven = registerBlock("hearth_oven", HearthOvenBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED)
				.strength(2.0F, 6.0F).sound(SoundType.STONE).requiresCorrectToolForDrops().noOcclusion()
				.lightLevel(state -> state.getValue(HearthOvenBlock.LIT) ? HearthOvenBlock.LIGHT : 0));
		HEARTH_OVEN_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("hearth_oven"),
				FabricBlockEntityTypeBuilder.create(HearthOvenBlockEntity::new, oven).build());
		registerItem("hearth_oven", props -> new BlockItem(oven, props), new Item.Properties().useBlockDescriptionPrefix(), BUILDING_TAB);
		registerItem("pastry_dough", Item::new, new Item.Properties().compostable(COMPOST_MEDIUM), INGREDIENT_TAB);
		for (PieFilling filling : PieFilling.values()) {
			registerItem(filling.rawPie(), Item::new, new Item.Properties().stacksTo(16).compostable(COMPOST_MEDIUM_HIGH), FOOD_TAB);
			Block pie = registerBlock(filling.pie(), props -> new PieBlock(filling, props), BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE)
					.strength(0.5F).sound(SoundType.WOOL).noOcclusion().pushReaction(PushReaction.POPPED));
			registerItem(filling.pie(), props -> new BlockItem(pie, props), new Item.Properties().useBlockDescriptionPrefix().stacksTo(1), FOOD_TAB);
			food(filling.slice(), filling.nutrition, filling.saturation, COMPOST_MEDIUM_HIGH);
		}
		Block burnt = registerBlock("burnt_pie", props -> new PieBlock(null, props), BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK)
				.strength(0.5F).sound(SoundType.WOOL).noOcclusion().pushReaction(PushReaction.POPPED));
		registerItem("burnt_pie", props -> new BlockItem(burnt, props), new Item.Properties().useBlockDescriptionPrefix().stacksTo(1), FOOD_TAB);

		// Fall additions 17, the Spirit Board: a candlelit séance spells out a restless spirit's name and the one thing it
		// wishes for; given it, the spirit is laid to rest.
		Block spiritBoard = registerBlock("spirit_board", SpiritBoardBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.SAND)
				.strength(0.8F).sound(SoundType.WOOD).noOcclusion().ignitedByLava().pushReaction(PushReaction.POPPED));
		SPIRIT_BOARD_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("spirit_board"),
				FabricBlockEntityTypeBuilder.create(SpiritBoardBlockEntity::new, spiritBoard).build());
		registerItem("spirit_board", props -> new BlockItem(spiritBoard, props), new Item.Properties().useBlockDescriptionPrefix(), BUILDING_TAB);

		// Fall additions 18, wild turkeys: flocks in woods and meadows; toms strut, hens lay eggs; a roast turkey is set on the
		// table and carved a serving at a time.
		TURKEY = entity("turkey", EntityType.Builder.<Turkey>of(Turkey::new, MobCategory.CREATURE).sized(0.6F, 0.95F).eyeHeight(0.8F)
				.clientTrackingRange(10));
		FabricDefaultAttributeRegistry.register(TURKEY, Turkey.createAttributes());
		Turkeys.register();
		meal("raw_turkey", 3, 0.3F);
		Block roastTurkey = registerBlock("roast_turkey", RoastTurkeyBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN)
				.strength(0.5F).sound(SoundType.WOOL).noOcclusion().pushReaction(PushReaction.POPPED));
		registerItem("roast_turkey", props -> new BlockItem(roastTurkey, props), new Item.Properties().useBlockDescriptionPrefix().stacksTo(1),
				FOOD_TAB);
		meal("turkey_slice", 3, 0.6F);

		// Fall additions 19, the Theremin: an eerie electronic instrument played without touching, singing higher the nearer
		// someone stands; comparators read how near, so it doubles as a proximity sensor.
		Block theremin = registerBlock("theremin", ThereminBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(1.5F)
				.sound(SoundType.WOOD).noOcclusion().ignitedByLava().lightLevel(state -> ThereminBlock.playing(state) ? ThereminBlock.LIGHT : 0));
		THEREMIN_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("theremin"),
				FabricBlockEntityTypeBuilder.create(ThereminBlockEntity::new, theremin).build());
		registerItem("theremin", props -> new BlockItem(theremin, props), new Item.Properties().useBlockDescriptionPrefix(), BUILDING_TAB);

		// Fall additions 20, the Día de Muertos ofrenda: an altar of three tiers for offerings to those remembered, with
		// marigold petals, papel picado, sugar skulls and pan de muerto; complete, it welcomes the restless spirits near.
		Block ofrenda = registerBlock("ofrenda", OfrendaBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.WOOL).strength(1.5F)
				.sound(SoundType.WOOD).noOcclusion().ignitedByLava().lightLevel(state -> state.getValue(OfrendaBlock.COMPLETE) ? OfrendaBlock.LIGHT : 0));
		OFRENDA_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("ofrenda"),
				FabricBlockEntityTypeBuilder.create(OfrendaBlockEntity::new, ofrenda).build());
		registerItem("ofrenda", props -> new BlockItem(ofrenda, props), new Item.Properties().useBlockDescriptionPrefix(), BUILDING_TAB);
		Block petals = registerBlock("marigold_petals", CarpetBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.PINK_PETALS)
				.mapColor(MapColor.COLOR_ORANGE));
		registerItem("marigold_petals", props -> new BlockItem(petals, props), new Item.Properties().useBlockDescriptionPrefix()
				.compostable(COMPOST_LOW), BUILDING_TAB);
		Block papel = registerBlock("papel_picado", props -> new WallDecorationBlock(props, 1.0, 1.0, 15.0), BlockBehaviour.Properties.of()
				.mapColor(MapColor.COLOR_PINK).instabreak().noCollision().sound(SoundType.WOOL).ignitedByLava().pushReaction(PushReaction.POPPED));
		registerItem("papel_picado", props -> new BlockItem(papel, props), new Item.Properties().useBlockDescriptionPrefix(), BUILDING_TAB);
		Block skull = registerBlock("sugar_skull", SugarSkullBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(0.3F)
				.sound(SoundType.STONE).noOcclusion().pushReaction(PushReaction.POPPED));
		registerItem("sugar_skull", props -> new BlockItem(skull, props), new Item.Properties().useBlockDescriptionPrefix(), BUILDING_TAB);
		plain("pan_de_muerto_dough", COMPOST_MEDIUM);
		food("pan_de_muerto", 6, 0.7F, COMPOST_MEDIUM_HIGH);

		registerGraveyard();
	}

	/**
	 * The graveyard pack (tools/graveyard.py): headstones that weather, one to three blocks each, each part of one block
	 * entity type; the epitaph's data component; the Stonemason's Chisel and its epitaph screen.
	 */
	private static void registerGraveyard() {
		EPITAPH = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Jugcraft.id("epitaph"),
				DataComponentType.<Epitaph>builder().persistent(Epitaph.CODEC).networkSynchronized(Epitaph.STREAM_CODEC).build());
		List<Block> headstones = new ArrayList<>();
		for (HeadstoneBlock.Style style : HeadstoneBlock.Style.values()) {
			MapColor colour = switch (style.stone) {
				case MARBLE -> MapColor.QUARTZ;
				case SLATE -> MapColor.DEEPSLATE;
				case GRANITE -> MapColor.DIRT;
				case SANDSTONE -> MapColor.SAND;
				case IRON -> MapColor.METAL;
			};
			// Random ticks: headstones weather, and graves stir at night.
			Block stone = registerBlock(style.id, props -> style == HeadstoneBlock.Style.MEMORIAL_BENCH ? new MemorialBenchBlock(props, style)
					: new HeadstoneBlock(props, style), BlockBehaviour.Properties.of().mapColor(colour)
					.requiresCorrectToolForDrops().strength(2.0F, 6.0F).sound(style.stone == HeadstoneBlock.Stone.IRON ? SoundType.METAL : SoundType.STONE)
					.noOcclusion().randomTicks());
			registerItem(style.id, props -> new HeadstoneItem(stone, props), new Item.Properties().useBlockDescriptionPrefix(), BUILDING_TAB);
			headstones.add(stone);
		}
		HEADSTONE_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("headstone"),
				FabricBlockEntityTypeBuilder.create(HeadstoneBlockEntity::new, headstones.toArray(Block[]::new)).build());
		registerItem(Epitaphs.CHISEL, StonemasonsChiselItem::new, new Item.Properties().durability(CHISEL_USES), EQUIPMENT_TAB);
		Epitaphs.register();
		registerGraveyardBuildings();
		registerGraveyardGrounds();
	}

	/**
	 * The graveyard pack's grounds (pack 4) beyond its headstone styles: the Grave Vase, whose fresh flowers calm the
	 * graves about it, and the Cemetery Lamp Post, lit at night.
	 */
	private static void registerGraveyardGrounds() {
		Block vase = registerBlock(GRAVE_VASE, GraveVaseBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_ORANGE)
				.strength(1.5F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.METAL).noOcclusion().randomTicks()
				.pushReaction(PushReaction.POPPED));
		registerItem(GRAVE_VASE, props -> new BlockItem(vase, props), new Item.Properties().useBlockDescriptionPrefix(), BUILDING_TAB);
		Block post = registerBlock(LAMP_POST, LampPostBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK)
				.strength(3.0F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.METAL).noOcclusion().lightLevel(LampPostBlock::light)
				.pushReaction(PushReaction.POPPED));
		registerItem(LAMP_POST, props -> new BlockItem(post, props), new Item.Properties().useBlockDescriptionPrefix(), BUILDING_TAB);
		registerChurchyardOrnaments();
		registerHauntedHouseProps();
		registerWitchsWorkshop();
		registerCryptAndOssuary();
		registerLaboratoryLarderDining();
		registerPumpkinNight();
	}

	/**
	 * Halloween decorations batch 15, the churchyard's ornaments (tools/decor15.py): the Bone Pile, the Ossuary Wall, the
	 * Giant Bone Hand and the Witch's Lantern. (The Gargoyle is a headstone style.)
	 */
	private static void registerChurchyardOrnaments() {
		Block pile = registerBlock(BONE_PILE, BonePileBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.SAND).strength(0.4F)
				.sound(SoundType.BONE_BLOCK).noOcclusion().pushReaction(PushReaction.POPPED));
		Block wall = registerBlock(OSSUARY_WALL, OssuaryWallBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.BONE_BLOCK));
		Block hand = registerBlock(BONE_HAND, props -> new GiantBoneHandBlock(props, Block.box(2.5, 0.0, 2.5, 13.5, 16.0, 13.5),
				Block.box(4.0, 0.0, 4.0, 12.0, 10.0, 12.0)), BlockBehaviour.Properties.of().mapColor(MapColor.SAND).strength(1.5F)
				.sound(SoundType.BONE_BLOCK).noOcclusion().pushReaction(PushReaction.IMMOVEABLE));
		Block lantern = registerBlock(WITCHS_LANTERN, LanternBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.LANTERN)
				.mapColor(MapColor.COLOR_PURPLE).lightLevel(state -> WITCHS_LANTERN_LIGHT));
		for (Block block : List.of(pile, wall, lantern)) {
			String id = BuiltInRegistries.BLOCK.getKey(block).getPath();
			registerItem(id, props -> new BlockItem(block, props), new Item.Properties().useBlockDescriptionPrefix(), BUILDING_TAB);
		}
		registerItem(BONE_HAND, props -> new DoubleHighBlockItem(hand, props), new Item.Properties().useBlockDescriptionPrefix(), BUILDING_TAB);
	}

	/**
	 * Halloween decorations batch 16, the haunted house's props (tools/decor16.py): the Flying Eyeball (drawn by the
	 * client), the ivory and black Pillar Candles (vanilla candles), the Spider Web (vanilla's multiface block) and the
	 * Monster's Head. (The harvest plushes are the midway's.)
	 */
	private static void registerHauntedHouseProps() {
		Block eyeball = registerBlock(FLYING_EYEBALL, FlyingEyeballBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED)
				.strength(0.3F).sound(SoundType.SLIME_BLOCK).noCollision().noOcclusion().pushReaction(PushReaction.POPPED));
		FLYING_EYEBALL_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id(FLYING_EYEBALL),
				FabricBlockEntityTypeBuilder.<DecorationBlockEntity>create((pos, state) -> new DecorationBlockEntity(FLYING_EYEBALL_ENTITY, pos, state), eyeball)
						.build());
		Block ivory = registerBlock(IVORY_PILLAR_CANDLE, PillarCandleBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.CANDLE).mapColor(MapColor.SAND));
		Block black = registerBlock(BLACK_PILLAR_CANDLE, PillarCandleBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.CANDLE).mapColor(MapColor.COLOR_BLACK));
		Block web = registerBlock(SPIDER_WEB, MultifaceBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.WOOL).noCollision()
				.strength(0.2F).sound(SoundType.COBWEB).noOcclusion().pushReaction(PushReaction.POPPED));
		Block head = registerBlock(MONSTER_HEAD, MonsterHeadBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN)
				.strength(1.0F).sound(SoundType.DECORATED_POT).noOcclusion().lightLevel(MonsterHeadBlock::light).pushReaction(PushReaction.POPPED));
		for (Block block : List.of(eyeball, ivory, black, web, head)) {
			String id = BuiltInRegistries.BLOCK.getKey(block).getPath();
			registerItem(id, props -> new BlockItem(block, props), new Item.Properties().useBlockDescriptionPrefix(), BUILDING_TAB);
		}
	}

	/**
	 * Halloween decorations batch 17, the Witch's Workshop (tools/decor17.py): the Horned Skull Cauldron with its Ember
	 * Bed and Brew Ladle; the four wrought-iron candelabra; the Enchanted Broom, Dustpan and Broom Rack; the Curiosity
	 * Cabinet, Bell Jar and Moth Display Case; the five Oddity Jars; and the bigger jars: the Giant's Beating Heart, the
	 * Tall Specimen Jar and the Specimen Tank.
	 */
	private static void registerWitchsWorkshop() {
		List<Block> blocks = new ArrayList<>();
		Block cauldron = registerBlock(HORNED_SKULL_CAULDRON, HornedSkullCauldronBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK)
				.strength(2.0F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.METAL).noOcclusion().lightLevel(HornedSkullCauldronBlock::light));
		HORNED_SKULL_CAULDRON_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id(HORNED_SKULL_CAULDRON),
				FabricBlockEntityTypeBuilder.create(HornedSkullCauldronBlockEntity::new, cauldron).build());
		blocks.add(cauldron);
		blocks.add(registerBlock(EMBER_BED, EmberBedBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(2.0F, 6.0F)
				.requiresCorrectToolForDrops().sound(SoundType.STONE).noOcclusion().lightLevel(state -> EmberBedBlock.LIGHT)));
		registerItem(BREW_LADLE, Item::new, new Item.Properties().stacksTo(1), TOOL_TAB);

		Block floor = registerBlock(FloorCandelabrumBlock.KIND, FloorCandelabrumBlock::new, candelabrum().lightLevel(state -> Candelabra.light(state, 14)));
		Block table = registerBlock("table_candelabrum", props -> new CandelabrumBlock(props, "table_candelabrum", CandelabrumBlock.Mount.TABLE),
				candelabrum().lightLevel(state -> Candelabra.light(state, 9)));
		Block girandole = registerBlock("wall_girandole", props -> new CandelabrumBlock(props, "wall_girandole", CandelabrumBlock.Mount.WALL),
				candelabrum().lightLevel(state -> Candelabra.light(state, 9)));
		Block chandelier = registerBlock("branching_chandelier", props -> new CandelabrumBlock(props, "branching_chandelier", CandelabrumBlock.Mount.HANGING),
				candelabrum().lightLevel(state -> Candelabra.light(state, 15)));
		CANDELABRUM_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("candelabrum"),
				FabricBlockEntityTypeBuilder.<DecorationBlockEntity>create((pos, state) -> new DecorationBlockEntity(CANDELABRUM_ENTITY, pos, state),
						floor, table, girandole, chandelier).build());
		registerItem(FloorCandelabrumBlock.KIND, props -> new DoubleHighBlockItem(floor, props), new Item.Properties().useBlockDescriptionPrefix(), BUILDING_TAB);
		blocks.addAll(List.of(table, girandole, chandelier));

		Block broom = registerBlock(ENCHANTED_BROOM, EnchantedBroomBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(0.5F)
				.sound(SoundType.WOOD).noOcclusion().noCollision().pushReaction(PushReaction.POPPED));
		ENCHANTED_BROOM_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id(ENCHANTED_BROOM),
				FabricBlockEntityTypeBuilder.create(EnchantedBroomBlockEntity::new, broom).build());
		Block dustpan = registerBlock(DUSTPAN, DustpanBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(0.8F)
				.sound(SoundType.LANTERN).noOcclusion().pushReaction(PushReaction.POPPED));
		DUSTPAN_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id(DUSTPAN),
				FabricBlockEntityTypeBuilder.create(DustpanBlockEntity::new, dustpan).build());
		Block rack = registerBlock(BROOM_RACK, BroomRackBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(1.0F)
				.sound(SoundType.WOOD).noOcclusion().pushReaction(PushReaction.POPPED));
		blocks.addAll(List.of(broom, dustpan, rack));

		Block cabinet = registerBlock(CURIOSITY_CABINET, CuriosityCabinetBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN)
				.strength(2.0F, 3.0F).sound(SoundType.WOOD).noOcclusion().pushReaction(PushReaction.POPPED));
		Block bellJar = registerBlock(BELL_JAR, BellJarBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.NONE).strength(0.6F)
				.sound(SoundType.GLASS).noOcclusion().pushReaction(PushReaction.POPPED));
		SHOWCASE_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("showcase"),
				FabricBlockEntityTypeBuilder.create(ShowcaseBlockEntity::new, cabinet, bellJar, rack).build());
		registerItem(CURIOSITY_CABINET, props -> new DoubleHighBlockItem(cabinet, props), new Item.Properties().useBlockDescriptionPrefix(), BUILDING_TAB);
		Block mothCase = registerBlock(MOTH_CASE, MothCaseBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(0.5F)
				.sound(SoundType.WOOD).noOcclusion().pushReaction(PushReaction.POPPED));
		MOTH_CASE_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id(MOTH_CASE),
				FabricBlockEntityTypeBuilder.<DecorationBlockEntity>create((pos, state) -> new DecorationBlockEntity(MOTH_CASE_ENTITY, pos, state), mothCase)
						.build());
		blocks.addAll(List.of(bellJar, mothCase));

		Block eyeballs = registerBlock("jar_of_eyeballs", props -> new OddityJarBlock(props, OddityJarBlock.Kind.EYEBALLS), jar());
		Block heart = registerBlock("beating_heart_jar", BeatingHeartJarBlock::new, jar());
		Block bat = registerBlock("bat_in_a_jar", BatJarBlock::new, jar());
		Block snake = registerBlock("two_headed_snake_jar", props -> new OddityJarBlock(props, OddityJarBlock.Kind.SNAKE), jar());
		Block hand = registerBlock("hand_in_a_jar", HandJarBlock::new, jar());
		ODDITY_JAR_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("oddity_jar"),
				FabricBlockEntityTypeBuilder.<DecorationBlockEntity>create((pos, state) -> new DecorationBlockEntity(ODDITY_JAR_ENTITY, pos, state),
						eyeballs, heart, bat, snake, hand).build());
		blocks.addAll(List.of(eyeballs, heart, bat, snake, hand));

		// The bigger jars, each its small jar made bigger, placed and broken as one: the Giant's Beating Heart (3 x 3 x 3), the
		// Tall Specimen Jar (1 x 2 x 1) and the Specimen Tank (3 x 3 x 3).
		Block giantHeart = registerBlock(GIANT_HEART, GiantBeatingHeartBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED)
				.strength(1.5F).sound(SoundType.GLASS).noOcclusion().pushReaction(PushReaction.IMMOVEABLE));
		GIANT_HEART_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id(GIANT_HEART),
				FabricBlockEntityTypeBuilder.<DecorationBlockEntity>create((pos, state) -> new DecorationBlockEntity(GIANT_HEART_ENTITY, pos, state),
						giantHeart).build());
		Block tallJar = registerBlock(TALL_SPECIMEN_JAR, TallSpecimenJarBlock::new, vessel().lightLevel(state -> TallSpecimenJarBlock.LIGHT));
		Block tank = registerBlock(SPECIMEN_TANK, SpecimenTankBlock::new, vessel().lightLevel(state -> SpecimenTankBlock.LIGHT));
		SPECIMEN_VESSEL_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("specimen_vessel"),
				FabricBlockEntityTypeBuilder.<DecorationBlockEntity>create((pos, state) -> new DecorationBlockEntity(SPECIMEN_VESSEL_ENTITY, pos, state),
						tallJar, tank).build());
		blocks.addAll(List.of(giantHeart, tallJar, tank));
		for (Block block : blocks) {
			String id = BuiltInRegistries.BLOCK.getKey(block).getPath();
			registerItem(id, props -> new BlockItem(block, props), new Item.Properties().useBlockDescriptionPrefix(), BUILDING_TAB);
		}
	}

	private static BlockBehaviour.Properties candelabrum() {
		return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(1.0F).sound(SoundType.METAL).noOcclusion()
				.pushReaction(PushReaction.POPPED).randomTicks();
	}

	private static BlockBehaviour.Properties jar() {
		return BlockBehaviour.Properties.of().mapColor(MapColor.NONE).strength(0.5F).sound(SoundType.GLASS).noOcclusion().pushReaction(PushReaction.POPPED);
	}

	private static BlockBehaviour.Properties vessel() {
		return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GREEN).strength(1.0F).sound(SoundType.GLASS).noOcclusion()
				.pushReaction(PushReaction.IMMOVEABLE);
	}

	/**
	 * Halloween decorations batch 18, the Crypt and the Ossuary (docs/features/crypt-and-ossuary.md): the Iron-Bound Coffin,
	 * the Skeleton Key and the Coffin Wardrobe; the Stone Sarcophagi; the Bone Throne, Ribcage Bookcase, Skull Footstool
	 * and Vertebra Floor Lamp; the Buried Colossus; and the Gargoyle Sentinel, Gargoyle Rainspout and Chimera Finial.
	 */
	private static void registerCryptAndOssuary() {
		List<Block> blocks = new ArrayList<>();
		KEY_WARDS = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Jugcraft.id("key_wards"),
				DataComponentType.<Integer>builder().persistent(Codec.intRange(1, SkeletonKeyItem.PATTERNS - 1)).networkSynchronized(ByteBufCodecs.VAR_INT)
						.build());
		KEY_COPYING_SERIALIZER = Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, Jugcraft.id(KEY_COPYING), KeyCopyingRecipe.SERIALIZER);
		registerItem(KEY_BLANK, props -> new SkeletonKeyItem(props, true), new Item.Properties(), TOOL_TAB);
		registerItem(SKELETON_KEY, props -> new SkeletonKeyItem(props, false), new Item.Properties().stacksTo(16), TOOL_TAB);

		Block coffin = registerBlock(IRON_BOUND_COFFIN, IronBoundCoffinBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK)
				.strength(3.0F, 6.0F).sound(SoundType.WOOD).noOcclusion().pushReaction(PushReaction.IMMOVEABLE));
		registerItem(IRON_BOUND_COFFIN, props -> new BlockItem(coffin, props), new Item.Properties().useBlockDescriptionPrefix().stacksTo(1), EQUIPMENT_TAB);
		IRON_BOUND_COFFIN_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id(IRON_BOUND_COFFIN),
				FabricBlockEntityTypeBuilder.create(IronBoundCoffinBlockEntity::new, coffin).build());
		Block wardrobe = registerBlock(COFFIN_WARDROBE, CoffinWardrobeBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN)
				.strength(2.5F).sound(SoundType.WOOD).noOcclusion().ignitedByLava().pushReaction(PushReaction.IMMOVEABLE));
		COFFIN_WARDROBE_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id(COFFIN_WARDROBE),
				FabricBlockEntityTypeBuilder.create(CoffinWardrobeBlockEntity::new, wardrobe).build());
		registerItem(COFFIN_WARDROBE, props -> new DoubleHighBlockItem(wardrobe, props), new Item.Properties().useBlockDescriptionPrefix(), BUILDING_TAB);

		List<Block> tombs = new ArrayList<>();
		for (String stone : SARCOPHAGUS_STONES) {
			Block tomb = registerBlock(stone + "_sarcophagus", SarcophagusBlock::new, BlockBehaviour.Properties.of()
					.mapColor(stone.equals("stone_brick") ? MapColor.STONE : MapColor.DEEPSLATE).strength(3.0F, 6.0F).requiresCorrectToolForDrops()
					.sound(stone.equals("deepslate") ? SoundType.POLISHED_DEEPSLATE : SoundType.STONE).noOcclusion().pushReaction(PushReaction.IMMOVEABLE));
			tombs.add(tomb);
		}
		SARCOPHAGUS_TOMB_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("sarcophagus"),
				FabricBlockEntityTypeBuilder.create(SarcophagusBlockEntity::new, tombs.toArray(Block[]::new)).build());
		blocks.addAll(tombs);

		Block throne = registerBlock(BONE_THRONE, BoneThroneBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.SAND).strength(2.0F)
				.sound(SoundType.BONE_BLOCK).noOcclusion().pushReaction(PushReaction.IMMOVEABLE).lightLevel(BoneThroneBlock::light));
		registerItem(BONE_THRONE, props -> new DoubleHighBlockItem(throne, props), new Item.Properties().useBlockDescriptionPrefix(), BUILDING_TAB);
		Block bookcase = registerBlock(RIBCAGE_BOOKCASE, RibcageBookcaseBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.SAND)
				.strength(1.5F).sound(SoundType.BONE_BLOCK).noOcclusion());
		// The bookcase keeps its books in vanilla's chiseled bookshelf block entity (26.3 names no constant for that type).
		BlockEntityType<?> shelves = BuiltInRegistries.BLOCK_ENTITY_TYPE.getValue(Identifier.withDefaultNamespace("chiseled_bookshelf"));
		if (shelves == null) {
			throw new IllegalStateException("No minecraft:chiseled_bookshelf block entity type to add the Ribcage Bookcase to");
		}
		((FabricBlockEntityType) shelves).addValidBlock(bookcase);
		Block footstool = registerBlock(SKULL_FOOTSTOOL, SkullFootstoolBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.SAND)
				.strength(1.0F).sound(SoundType.BONE_BLOCK).noOcclusion().pushReaction(PushReaction.POPPED));
		Block lamp = registerBlock(VERTEBRA_LAMP, VertebraFloorLampBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.SAND).strength(1.0F)
				.sound(SoundType.BONE_BLOCK).noOcclusion().pushReaction(PushReaction.POPPED).lightLevel(VertebraFloorLampBlock::light));
		registerItem(VERTEBRA_LAMP, props -> new DoubleHighBlockItem(lamp, props), new Item.Properties().useBlockDescriptionPrefix(), BUILDING_TAB);
		blocks.addAll(List.of(bookcase, footstool));

		Block skull = registerBlock(COLOSSAL_SKULL, ColossalSkullBlock::new, colossal());
		COLOSSAL_SKULL_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id(COLOSSAL_SKULL),
				FabricBlockEntityTypeBuilder.create(ColossalSkullBlockEntity::new, skull).build());
		Block rib = registerBlock(COLOSSAL_RIB, ColossalRibBlock::new, colossal());
		registerItem(COLOSSAL_RIB, props -> new DoubleHighBlockItem(rib, props), new Item.Properties().useBlockDescriptionPrefix(), BUILDING_TAB);
		Block vertebra = registerBlock(COLOSSAL_VERTEBRA, ColossalVertebraBlock::new, colossal());
		Block femur = registerBlock(COLOSSAL_FEMUR, props -> new LongDecorationBlock(props, FEMUR_HEAD, FEMUR_FOOT), colossal());
		blocks.addAll(List.of(skull, vertebra, femur));

		Block sentinel = registerBlock(GARGOYLE_SENTINEL, GargoyleSentinelBlock::new, gargoyle());
		GARGOYLE_SENTINEL_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id(GARGOYLE_SENTINEL),
				FabricBlockEntityTypeBuilder.create(GargoyleSentinelBlockEntity::new, sentinel).build());
		Block spout = registerBlock(GARGOYLE_RAINSPOUT, GargoyleRainspoutBlock::new, gargoyle().pushReaction(PushReaction.POPPED));
		GARGOYLE_RAINSPOUT_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id(GARGOYLE_RAINSPOUT),
				FabricBlockEntityTypeBuilder.create(GargoyleRainspoutBlockEntity::new, spout).build());
		Block finial = registerBlock(CHIMERA_FINIAL, ChimeraFinialBlock::new, gargoyle());
		blocks.addAll(List.of(sentinel, spout, finial));
		for (Block block : blocks) {
			String id = BuiltInRegistries.BLOCK.getKey(block).getPath();
			registerItem(id, props -> new BlockItem(block, props), new Item.Properties().useBlockDescriptionPrefix(), BUILDING_TAB);
		}
	}

	private static BlockBehaviour.Properties colossal() {
		return BlockBehaviour.Properties.of().mapColor(MapColor.SAND).strength(2.0F, 4.0F).requiresCorrectToolForDrops().sound(SoundType.BONE_BLOCK)
				.noOcclusion().pushReaction(PushReaction.IMMOVEABLE);
	}

	/**
	 * Halloween decorations batch 19, the Laboratory, the Larder and the Dining Room (tools/decor19.py): the Lightning
	 * Harness, Brain-Vat Console and Crawling Hand; the Silk Cocoon, Egg Sac Cluster, Web Drape and Silk Spool Stack; the
	 * Haunted Dining Chair, Floating Table Setting and Grandfather Clock; the three witchlights; the Yard Silhouette and
	 * the Harvest Moon Lamp.
	 */
	private static void registerLaboratoryLarderDining() {
		List<Block> blocks = new ArrayList<>();
		Block harness = registerBlock(LIGHTNING_HARNESS, LightningHarnessBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE)
				.strength(2.0F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.COPPER).noOcclusion().pushReaction(PushReaction.POPPED));
		LIGHTNING_HARNESS_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id(LIGHTNING_HARNESS),
				FabricBlockEntityTypeBuilder.create(LightningHarnessBlockEntity::new, harness).build());
		Block console = registerBlock(BRAIN_VAT_CONSOLE, BrainVatConsoleBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.GOLD)
				.strength(2.0F, 6.0F).sound(SoundType.METAL).noOcclusion().lightLevel(BrainVatConsoleBlock::light));
		Block hand = registerBlock(CRAWLING_HAND, CrawlingHandBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN)
				.strength(0.3F).sound(SoundType.SLIME_BLOCK).noOcclusion().pushReaction(PushReaction.POPPED));
		CRAWLING_HAND_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id(CRAWLING_HAND),
				FabricBlockEntityTypeBuilder.<DecorationBlockEntity>create((pos, state) -> new DecorationBlockEntity(CRAWLING_HAND_ENTITY, pos, state), hand)
						.build());
		blocks.addAll(List.of(harness, console, hand));

		Block cocoon = registerBlock(SILK_COCOON, SilkCocoonBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.WOOL).strength(0.8F)
				.sound(SoundType.WOOL).noOcclusion().pushReaction(PushReaction.POPPED));
		SILK_COCOON_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id(SILK_COCOON),
				FabricBlockEntityTypeBuilder.create(SilkCocoonBlockEntity::new, cocoon).build());
		Block sacs = registerBlock(EGG_SAC_CLUSTER, EggSacClusterBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.WOOL).noCollision()
				.strength(0.2F).sound(SoundType.FROGSPAWN).noOcclusion().pushReaction(PushReaction.POPPED));
		EGG_SAC_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id(EGG_SAC_CLUSTER),
				FabricBlockEntityTypeBuilder.<DecorationBlockEntity>create((pos, state) -> new DecorationBlockEntity(EGG_SAC_ENTITY, pos, state), sacs)
						.build());
		Block drape = registerBlock(WEB_DRAPE, WebDrapeBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.WOOL).noCollision().strength(0.6F)
				.sound(SoundType.COBWEB).noOcclusion().pushReaction(PushReaction.IMMOVEABLE));
		Block spools = registerBlock(SILK_SPOOL_STACK, SilkSpoolStackBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(1.0F)
				.sound(SoundType.WOOD).noOcclusion().ignitedByLava());
		SILK_SPOOL_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id(SILK_SPOOL_STACK),
				FabricBlockEntityTypeBuilder.create(SilkSpoolStackBlockEntity::new, spools).build());
		blocks.addAll(List.of(cocoon, sacs, drape, spools));

		Block chair = registerBlock(DINING_CHAIR, HauntedDiningChairBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN)
				.strength(1.5F).sound(SoundType.WOOD).noOcclusion().ignitedByLava());
		DINING_CHAIR_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id(DINING_CHAIR),
				FabricBlockEntityTypeBuilder.create(HauntedDiningChairBlockEntity::new, chair).build());
		Block setting = registerBlock(TABLE_SETTING, FloatingTableSettingBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.NONE)
				.strength(0.3F).sound(SoundType.DECORATED_POT).noOcclusion().pushReaction(PushReaction.POPPED).lightLevel(FloatingTableSettingBlock::light));
		TABLE_SETTING_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id(TABLE_SETTING),
				FabricBlockEntityTypeBuilder.<DecorationBlockEntity>create((pos, state) -> new DecorationBlockEntity(TABLE_SETTING_ENTITY, pos, state), setting)
						.build());
		Block clock = registerBlock(GRANDFATHER_CLOCK, GrandfatherClockBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN)
				.strength(2.0F).sound(SoundType.WOOD).noOcclusion().ignitedByLava().pushReaction(PushReaction.IMMOVEABLE));
		GRANDFATHER_CLOCK_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id(GRANDFATHER_CLOCK),
				FabricBlockEntityTypeBuilder.create(GrandfatherClockBlockEntity::new, clock).build());
		registerItem(GRANDFATHER_CLOCK, props -> new DoubleHighBlockItem(clock, props), new Item.Properties().useBlockDescriptionPrefix(), BUILDING_TAB);
		blocks.addAll(List.of(chair, setting));

		Block post = registerBlock(WITCHLIGHT_POST, WitchlightLampPostBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(1.5F)
				.sound(SoundType.LANTERN).noOcclusion().pushReaction(PushReaction.IMMOVEABLE).lightLevel(WitchlightLampPostBlock::light));
		registerItem(WITCHLIGHT_POST, props -> new DoubleHighBlockItem(post, props), new Item.Properties().useBlockDescriptionPrefix(), BUILDING_TAB);
		Block stake = registerBlock(WITCHLIGHT_STAKE, props -> new WitchlightBlock(props, WitchlightBlock.Mount.STAKE), BlockBehaviour.Properties.of()
				.mapColor(MapColor.METAL).strength(1.0F).sound(SoundType.LANTERN).noOcclusion().pushReaction(PushReaction.POPPED).lightLevel(Witchlights::light));
		Block hanging = registerBlock(HANGING_WITCHLIGHT, props -> new WitchlightBlock(props, WitchlightBlock.Mount.HANGING), BlockBehaviour.Properties.of()
				.mapColor(MapColor.METAL).strength(1.0F).sound(SoundType.LANTERN).noOcclusion().pushReaction(PushReaction.POPPED).lightLevel(Witchlights::light));
		WITCHLIGHT_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("witchlight"),
				FabricBlockEntityTypeBuilder.create(WitchlightBlockEntity::new, post, stake, hanging).build());
		blocks.addAll(List.of(stake, hanging));

		Block silhouette = registerBlock(YARD_SILHOUETTE, YardSilhouetteBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK)
				.strength(1.0F).sound(SoundType.WOOD).noOcclusion().ignitedByLava().pushReaction(PushReaction.POPPED));
		SILHOUETTE_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id(YARD_SILHOUETTE),
				FabricBlockEntityTypeBuilder.<DecorationBlockEntity>create((pos, state) -> new DecorationBlockEntity(SILHOUETTE_ENTITY, pos, state), silhouette)
						.build());
		Block moon = registerBlock(MOON_LAMP, HarvestMoonLampBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.SAND).strength(1.5F)
				.sound(SoundType.GLASS).noOcclusion().pushReaction(PushReaction.IMMOVEABLE).lightLevel(HarvestMoonLampBlock::light));
		MOON_LAMP_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id(MOON_LAMP),
				FabricBlockEntityTypeBuilder.<DecorationBlockEntity>create((pos, state) -> new DecorationBlockEntity(MOON_LAMP_ENTITY, pos, state), moon)
						.build());
		blocks.addAll(List.of(silhouette, moon));
		for (Block block : blocks) {
			String id = BuiltInRegistries.BLOCK.getKey(block).getPath();
			registerItem(id, props -> new BlockItem(block, props), new Item.Properties().useBlockDescriptionPrefix(), BUILDING_TAB);
		}
	}

	public static final String LIGHTNING_HARNESS = "lightning_harness";
	public static final String BRAIN_VAT_CONSOLE = "brain_vat_console";
	public static final String CRAWLING_HAND = "crawling_hand";
	public static final String SILK_COCOON = "silk_cocoon";
	public static final String EGG_SAC_CLUSTER = "egg_sac_cluster";
	public static final String WEB_DRAPE = "web_drape";
	public static final String SILK_SPOOL_STACK = "silk_spool_stack";
	public static final String DINING_CHAIR = "haunted_dining_chair";
	public static final String TABLE_SETTING = "floating_table_setting";
	public static final String GRANDFATHER_CLOCK = "grandfather_clock";
	public static final String WITCHLIGHT_POST = "witchlight_lamp_post";
	public static final String WITCHLIGHT_STAKE = "witchlight_path_stake";
	public static final String HANGING_WITCHLIGHT = "hanging_witchlight";
	public static final String YARD_SILHOUETTE = "yard_silhouette";
	public static final String MOON_LAMP = "harvest_moon_lamp";

	/** A Singing Pumpkin voice's sound (assets/jugcraft/sounds/choir, made by tools/choir_sounds.py). */
	public static SoundEvent singingVoice(SingingPumpkinBlock.Voice voice) {
		return SINGING_VOICES.get(voice);
	}

	private static SoundEvent sound(String id) {
		Identifier key = Jugcraft.id(id);
		return Registry.register(BuiltInRegistries.SOUND_EVENT, key, SoundEvent.createVariableRangeEvent(key));
	}

	/**
	 * Halloween decorations batch 20, Pumpkin Night (tools/decor20.py): the Farm Stand, the Pumpkin Vine and Autumn Leaf
	 * Garlands, the Harvest Effigy with its Effigy Ashes and Hearth Ash, and the four Singing Pumpkins. (The Red Kuri and
	 * Kabocha pumpkins are registered with the other heirlooms.)
	 */
	private static void registerPumpkinNight() {
		Block stand = registerBlock(FARM_STAND, FarmStandBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5F, 1200.0F)
				.sound(SoundType.WOOD).noOcclusion().pushReaction(PushReaction.IMMOVEABLE));
		FARM_STAND_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id(FARM_STAND),
				FabricBlockEntityTypeBuilder.create(FarmStandBlockEntity::new, stand).build());
		FARM_STAND_MENU = Registry.register(BuiltInRegistries.MENU, Jugcraft.id(FARM_STAND),
				new ExtendedMenuType<>(FarmStandMenu::new, FarmStandMenu.Opening.STREAM_CODEC));
		registerItem(FARM_STAND, props -> new BlockItem(stand, props), new Item.Properties().useBlockDescriptionPrefix(), EQUIPMENT_TAB);
		FarmStandBlock.register();
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> FarmStandMenu.clear());

		registerItem(PUMPKIN_VINE_GARLAND, props -> new StringLightsItem(props, StringLightHookBlockEntity.Strand.PUMPKIN_VINE), new Item.Properties(),
				BUILDING_TAB);
		registerItem(AUTUMN_LEAF_GARLAND, props -> new StringLightsItem(props, StringLightHookBlockEntity.Strand.AUTUMN_LEAVES), new Item.Properties(),
				BUILDING_TAB);

		EFFIGY_BURN = sound("effigy.burn");
		Block effigy = registerBlock(HARVEST_EFFIGY, HarvestEffigyBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW)
				.strength(0.8F).sound(SoundType.GRASS).noOcclusion().pushReaction(PushReaction.IMMOVEABLE).lightLevel(HarvestEffigyBlock::light));
		HARVEST_EFFIGY_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id(HARVEST_EFFIGY),
				FabricBlockEntityTypeBuilder.create(HarvestEffigyBlockEntity::new, effigy).build());
		registerItem(HARVEST_EFFIGY, props -> new BlockItem(effigy, props), new Item.Properties().useBlockDescriptionPrefix(), BUILDING_TAB);
		Block ashes = registerBlock(EFFIGY_ASHES, EffigyAshesBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY)
				.strength(0.3F).sound(SoundType.SAND).noOcclusion().pushReaction(PushReaction.POPPED));
		registerItem(EFFIGY_ASHES, props -> new BlockItem(ashes, props), new Item.Properties().useBlockDescriptionPrefix(), BUILDING_TAB);
		registerItem(HEARTH_ASH, props -> new FertilizerItem(props, HEARTH_ASH_RADIUS, HEARTH_ASH_DOSES), new Item.Properties(), INGREDIENT_TAB);

		List<Block> pumpkins = new ArrayList<>();
		for (SingingPumpkinBlock.Voice voice : SingingPumpkinBlock.Voice.values()) {
			SINGING_VOICES.put(voice, sound("singing_pumpkin." + voice.id()));
			String id = SINGING_PUMPKIN + voice.id();
			Block pumpkin = registerBlock(id, props -> new SingingPumpkinBlock(props, voice), BlockBehaviour.Properties.of()
					.mapColor(MapColor.COLOR_ORANGE).strength(1.0F).sound(SoundType.WOOD).lightLevel(state -> SingingPumpkinBlock.LIGHT)
					.pushReaction(PushReaction.POPPED));
			registerItem(id, props -> new BlockItem(pumpkin, props), new Item.Properties().useBlockDescriptionPrefix(), BUILDING_TAB);
			pumpkins.add(pumpkin);
		}
		SINGING_PUMPKIN_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("singing_pumpkin"),
				FabricBlockEntityTypeBuilder.<DecorationBlockEntity>create((pos, state) -> new DecorationBlockEntity(SINGING_PUMPKIN_ENTITY, pos, state),
						pumpkins.toArray(Block[]::new)).build());
	}

	public static final String FARM_STAND = "farm_stand";
	public static final String PUMPKIN_VINE_GARLAND = "pumpkin_vine_garland";
	public static final String AUTUMN_LEAF_GARLAND = "autumn_leaf_garland";
	public static final String HARVEST_EFFIGY = "harvest_effigy";
	public static final String EFFIGY_ASHES = "effigy_ashes";
	public static final String HEARTH_ASH = "hearth_ash";
	/** Hearth Ash fertilizes this far round where it is used (a 3x3 patch), one dose of bone meal each: weaker than bone meal. */
	public static final int HEARTH_ASH_RADIUS = 1;
	public static final int HEARTH_ASH_DOSES = 1;
	/** Each Singing Pumpkin's ID is this and its voice: singing_pumpkin_bass and so on. */
	public static final String SINGING_PUMPKIN = "singing_pumpkin_";

	private static BlockBehaviour.Properties gargoyle() {
		return BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(2.0F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.STONE)
				.noOcclusion();
	}

	public static final String IRON_BOUND_COFFIN = "iron_bound_coffin";
	public static final String KEY_BLANK = "key_blank";
	public static final String SKELETON_KEY = "skeleton_key";
	public static final String KEY_COPYING = "key_copying";
	public static final String COFFIN_WARDROBE = "coffin_wardrobe";
	public static final List<String> SARCOPHAGUS_STONES = List.of("stone_brick", "deepslate", "blackstone");
	public static final String BONE_THRONE = "bone_throne";
	public static final String RIBCAGE_BOOKCASE = "ribcage_bookcase";
	public static final String SKULL_FOOTSTOOL = "skull_footstool";
	public static final String VERTEBRA_LAMP = "vertebra_floor_lamp";
	public static final String COLOSSAL_SKULL = "colossal_skull";
	public static final String COLOSSAL_RIB = "colossal_rib";
	public static final String COLOSSAL_VERTEBRA = "colossal_vertebra";
	public static final String COLOSSAL_FEMUR = "colossal_femur";
	public static final String GARGOYLE_SENTINEL = "gargoyle_sentinel";
	public static final String GARGOYLE_RAINSPOUT = "gargoyle_rainspout";
	public static final String CHIMERA_FINIAL = "chimera_finial";
	/** The Colossal Femur's hitboxes, in pixels for its head facing north (tools/decor18_data.py draws it). */
	private static final double[][] FEMUR_HEAD = {{3.0, 0.0, 2.0, 13.0, 8.0, 16.0}};
	private static final double[][] FEMUR_FOOT = {{4.0, 0.0, 0.0, 12.0, 7.0, 14.0}};

	public static final String HORNED_SKULL_CAULDRON = "horned_skull_cauldron";
	public static final String EMBER_BED = "ember_bed";
	public static final String BREW_LADLE = "brew_ladle";
	public static final String ENCHANTED_BROOM = "enchanted_broom";
	public static final String DUSTPAN = "dustpan";
	public static final String BROOM_RACK = "broom_rack";
	public static final String CURIOSITY_CABINET = "curiosity_cabinet";
	public static final String BELL_JAR = "bell_jar";
	public static final String MOTH_CASE = "moth_display_case";
	public static final String GIANT_HEART = "giant_beating_heart";
	public static final String TALL_SPECIMEN_JAR = "tall_specimen_jar";
	public static final String SPECIMEN_TANK = "specimen_tank";

	public static final String FLYING_EYEBALL = "flying_eyeball";
	public static final String IVORY_PILLAR_CANDLE = "ivory_pillar_candle";
	public static final String BLACK_PILLAR_CANDLE = "black_pillar_candle";
	public static final String SPIDER_WEB = "spider_web";
	public static final String MONSTER_HEAD = "monster_head";

	public static final String BONE_PILE = "bone_pile";
	public static final String OSSUARY_WALL = "ossuary_wall";
	public static final String BONE_HAND = "giant_bone_hand";
	public static final String WITCHS_LANTERN = "witchs_lantern";
	public static final int WITCHS_LANTERN_LIGHT = 13;

	/** The Grave Vase and the Cemetery Lamp Post. */
	public static final String GRAVE_VASE = "grave_vase";
	public static final String LAMP_POST = "cemetery_lamp_post";

	/**
	 * The graveyard pack's buildings (pack 3), from the generated {@code /jugcraft/graveyard_buildings.json}: each one a
	 * {@link GraveyardBuildingBlock} of many parts with its item, all sharing one block entity type for their
	 * inscriptions (data component {@code jugcraft:inscriptions} for those after the epitaph); and the Bronze Mausoleum
	 * Door, a door opened by hand that fits a mausoleum's doorway.
	 */
	private static void registerGraveyardBuildings() {
		INSCRIPTIONS = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Jugcraft.id("inscriptions"),
				DataComponentType.<List<Epitaph>>builder().persistent(HeadstoneBlockEntity.MORE_CODEC)
						.networkSynchronized(HeadstoneBlockEntity.MORE_STREAM_CODEC).build());
		JsonArray list;
		try (InputStream stream = JugcraftAgriculture.class.getResourceAsStream("/jugcraft/graveyard_buildings.json")) {
			if (stream == null) {
				throw new IOException("missing");
			}
			list = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonArray();
		} catch (IOException | RuntimeException e) {
			throw new IllegalStateException("Could not read /jugcraft/graveyard_buildings.json", e);
		}
		List<Block> buildings = new ArrayList<>();
		for (JsonElement element : list) {
			GraveyardBuildingBlock.Building building = GraveyardBuildingBlock.Building.read(element.getAsJsonObject());
			MapColor colour = switch (building.stone()) {
				case MARBLE -> MapColor.QUARTZ;
				case SLATE -> MapColor.DEEPSLATE;
				case GRANITE -> MapColor.DIRT;
				case SANDSTONE -> MapColor.SAND;
				case IRON -> MapColor.METAL;
			};
			// Random ticks: buildings weather as the headstones do, and stir spirits at night.
			Block block = registerBlock(building.id(), props -> GraveyardBuildingBlock.create(props, building), BlockBehaviour.Properties.of()
					.mapColor(building.wooden() ? MapColor.WOOD : colour).requiresCorrectToolForDrops().strength(3.0F, 6.0F)
					.sound(building.wooden() ? SoundType.WOOD : SoundType.STONE).noOcclusion().randomTicks()
					.lightLevel(state -> building.light(state.getValue(building.part()))));
			registerItem(building.id(), props -> new HeadstoneItem(block, props), new Item.Properties().useBlockDescriptionPrefix(), BUILDING_TAB);
			buildings.add(block);
		}
		GRAVEYARD_BUILDING_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("graveyard_building"),
				FabricBlockEntityTypeBuilder.create((pos, state) -> new HeadstoneBlockEntity(GRAVEYARD_BUILDING_ENTITY, pos, state),
						buildings.toArray(Block[]::new)).build());
		// Opened by hand, like a copper door; the mausoleum places nothing in its doorway itself.
		Block door = registerBlock(MAUSOLEUM_DOOR, props -> new DoorBlock(BlockSetType.COPPER, props), BlockBehaviour.Properties.of()
				.mapColor(MapColor.TERRACOTTA_ORANGE).strength(4.0F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.COPPER)
				.noOcclusion().pushReaction(PushReaction.POPPED));
		registerItem(MAUSOLEUM_DOOR, props -> new BlockItem(door, props), new Item.Properties().useBlockDescriptionPrefix(), BUILDING_TAB);
	}

	/** The Bronze Mausoleum Door. */
	public static final String MAUSOLEUM_DOOR = "bronze_mausoleum_door";

	/** The Yard Inflatables' designs, one block each ({@code inflatable_<design>}). */
	public static final List<String> INFLATABLE_DESIGNS = List.of("ghost", "cat", "pumpkin", "spider");

	/** The Weathervanes' designs, one block each ({@code <design>_weathervane}). */
	public static final List<String> WEATHERVANE_DESIGNS = List.of("bat", "witch");

	/** The Leaf Piles' colours, one block each ({@code <colour>_leaf_pile}). */
	public static final List<String> LEAF_PILE_COLOURS = List.of("red", "orange", "yellow");

	/** How brightly a Wisp in a Jar glows. */
	public static final int WISP_JAR_LIGHT = 13;

	private static <T extends net.minecraft.world.entity.Entity> EntityType<T> entity(String id, EntityType.Builder<T> builder) {
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, Jugcraft.id(id));
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
	}

	/** The costume hats, in the order of their tag. */
	public static final List<String> COSTUMES = List.of("witch_hat", "ghost_sheet", "scarecrow_hat");

	/** A mum (garden chrysanthemum): a small flower with its item, and its potted form (no item, like vanilla's). */
	private static void mum(String id, Holder<MobEffect> stewEffect, float seconds) {
		Block flower = registerBlock(id, props -> new FlowerBlock(stewEffect, seconds, props), BlockBehaviour.Properties.ofFullCopy(Blocks.DANDELION));
		registerItem(id, props -> new BlockItem(flower, props), new Item.Properties().useBlockDescriptionPrefix().compostable(COMPOST_MEDIUM),
				SEEDS_TAB);
		registerBlock("potted_" + id, props -> new FlowerPotBlock(flower, props), BlockBehaviour.Properties.ofFullCopy(Blocks.POTTED_DANDELION));
	}

	/**
	 * The biomes branch's wild plants (tools/plants.py), from the generated {@code /jugcraft/plants.json}: small flowers
	 * (with their potted forms), tall flowers and tall plants, flowerbeds (ground cover, {@link GroundCoverBlock}), water
	 * plants ({@link WaterPlantBlock}) and plants floating on water ({@link FloatingPlantBlock}), copying a vanilla plant's
	 * properties. They compost and burn like vanilla's flowers.
	 */
	private static void registerWildPlants() {
		JsonArray plants;
		try (InputStream stream = JugcraftAgriculture.class.getResourceAsStream("/jugcraft/plants.json")) {
			if (stream == null) {
				throw new IOException("missing");
			}
			plants = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonArray();
		} catch (IOException | RuntimeException e) {
			throw new IllegalStateException("Could not read /jugcraft/plants.json", e);
		}
		FlammableBlockRegistry fire = FlammableBlockRegistry.getDefaultInstance();
		for (JsonElement element : plants) {
			JsonObject plant = element.getAsJsonObject();
			String id = plant.get("id").getAsString();
			Block block = switch (plant.get("kind").getAsString()) {
				case "flower" -> {
					Holder<MobEffect> effect = BuiltInRegistries.MOB_EFFECT.getOrThrow(ResourceKey.create(Registries.MOB_EFFECT,
							Identifier.parse(plant.get("effect").getAsString())));
					float seconds = plant.get("seconds").getAsFloat();
					int light = plant.has("light") ? plant.get("light").getAsInt() : 0;
					Block flower = registerBlock(id, props -> new FlowerBlock(effect, seconds, props), BlockBehaviour.Properties.ofFullCopy(Blocks.DANDELION)
							.lightLevel(state -> light));
					registerItem(id, props -> new BlockItem(flower, props), new Item.Properties().useBlockDescriptionPrefix()
							.compostable(COMPOST_MEDIUM), SEEDS_TAB);
					registerBlock("potted_" + id, props -> new FlowerPotBlock(flower, props), BlockBehaviour.Properties.ofFullCopy(Blocks.POTTED_DANDELION));
					yield flower;
				}
				case "tall_flower" -> {
					Block tall = registerBlock(id, TallFlowerBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.LILAC));
					registerItem(id, props -> new DoubleHighBlockItem(tall, props), new Item.Properties().useBlockDescriptionPrefix()
							.compostable(COMPOST_MEDIUM), SEEDS_TAB);
					yield tall;
				}
				case "flowerbed" -> {
					Block bed = registerBlock(id, GroundCoverBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.PINK_PETALS));
					registerItem(id, props -> new BlockItem(bed, props), new Item.Properties().useBlockDescriptionPrefix()
							.compostable(COMPOST_LOW), SEEDS_TAB);
					yield bed;
				}
				case "tall_plant" -> {
					Block tall = registerBlock(id, DoublePlantBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.LILAC));
					registerItem(id, props -> new DoubleHighBlockItem(tall, props), new Item.Properties().useBlockDescriptionPrefix()
							.compostable(COMPOST_MEDIUM), SEEDS_TAB);
					yield tall;
				}
				case "floor_plant" -> {
					int light = plant.has("light") ? plant.get("light").getAsInt() : 0;
					Block glow = registerBlock(id, FloorPlantBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.BROWN_MUSHROOM)
							.lightLevel(state -> light));
					registerItem(id, props -> new BlockItem(glow, props), new Item.Properties().useBlockDescriptionPrefix()
							.compostable(COMPOST_MEDIUM), SEEDS_TAB);
					yield glow;
				}
				case "dune_plant" -> {
					Block tall = registerBlock(id, DunePlantBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.TALL_GRASS));
					registerItem(id, props -> new DoubleHighBlockItem(tall, props), new Item.Properties().useBlockDescriptionPrefix()
							.compostable(COMPOST_MEDIUM), SEEDS_TAB);
					yield tall;
				}
				case "water_plant" -> {
					Block water = registerBlock(id, WaterPlantBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.SEAGRASS));
					registerItem(id, props -> new BlockItem(water, props), new Item.Properties().useBlockDescriptionPrefix()
							.compostable(COMPOST_LOW), SEEDS_TAB);
					yield water;
				}
				case "surface" -> {
					Block floating = registerBlock(id, FloatingPlantBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.LILY_PAD));
					registerItem(id, props -> new PlaceOnWaterBlockItem(floating, props), new Item.Properties().useBlockDescriptionPrefix()
							.compostable(COMPOST_MEDIUM), SEEDS_TAB);
					yield floating;
				}
				case "grass" -> {
					String tall = plant.get("tall").getAsString();
					Block grass = registerBlock(id, props -> new WildGrassBlock(props, tall), BlockBehaviour.Properties.ofFullCopy(Blocks.SHORT_GRASS));
					registerItem(id, props -> new BlockItem(grass, props), new Item.Properties().useBlockDescriptionPrefix()
							.compostable(COMPOST_LOW), SEEDS_TAB);
					yield grass;
				}
				case "tall_grass" -> {
					Block tall = registerBlock(id, DoublePlantBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.TALL_GRASS));
					registerItem(id, props -> new DoubleHighBlockItem(tall, props), new Item.Properties().useBlockDescriptionPrefix()
							.compostable(COMPOST_MEDIUM), SEEDS_TAB);
					yield tall;
				}
				case "hanging" -> {
					int maxLength = plant.get("max_length").getAsInt();
					Block hanging = registerBlock(id, props -> new HangingPlantBlock(props, maxLength), BlockBehaviour.Properties.ofFullCopy(Blocks.VINE));
					registerItem(id, props -> new BlockItem(hanging, props), new Item.Properties().useBlockDescriptionPrefix()
							.compostable(COMPOST_LOW), SEEDS_TAB);
					yield hanging;
				}
				case "vine" -> {
					// Glow lichen's block without its glow: on any faces, spread by bone meal.
					Block vine = registerBlock(id, GlowLichenBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.VINE));
					registerItem(id, props -> new BlockItem(vine, props), new Item.Properties().useBlockDescriptionPrefix()
							.compostable(COMPOST_LOW), SEEDS_TAB);
					yield vine;
				}
				default -> throw new IllegalStateException("Unknown wild plant kind in /jugcraft/plants.json: " + plant);
			};
			if (!(block instanceof WaterPlantBlock) && !(block instanceof FloatingPlantBlock)) {
				fire.add(block, 60, 100);
			}
			// Scattered in vanilla's biomes too ("patch": conventional biome tags; placed feature patch_<id>).
			if (plant.has("patch")) {
				Predicate<BiomeSelectionContext> selector = context -> false;
				for (JsonElement tag : plant.getAsJsonArray("patch")) {
					String field = tag.getAsString().toLowerCase(Locale.ROOT);
					TagKey<Biome> biomes = TagKey.create(Registries.BIOME, Identifier.parse("c:" + field));
					selector = selector.or(BiomeSelectors.tag(biomes));
				}
				BiomeModifications.addFeature(BiomeSelectors.foundInOverworld().and(selector), GenerationStep.Decoration.VEGETAL_DECORATION,
						ResourceKey.create(Registries.PLACED_FEATURE, Jugcraft.id("patch_" + id)));
			}
		}
	}

	/** The chestnut tree: its sapling (planted from a chestnut), fruiting leaves and a wood set. */
	private static void registerChestnutTree() {
		registerBlock("chestnut_sapling", props -> new SaplingBlock(CHESTNUT_GROWER, props) {
		}, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SAPLING));
		Block leaves = registerBlock("chestnut_leaves", ChestnutLeavesBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LEAVES)
				.mapColor(MapColor.PLANT));
		registerItem("chestnut_leaves", props -> new BlockItem(leaves, props), new Item.Properties().useBlockDescriptionPrefix(), SEEDS_TAB);
		registerWoodSet("chestnut", MapColor.TERRACOTTA_BROWN, MapColor.COLOR_BROWN);
		FlammableBlockRegistry.getDefaultInstance().add(leaves, 30, 60);
	}

	private static TreeGrower grower(String shape) {
		ResourceKey<Feature> tree = ResourceKey.create(Registries.FEATURE, Jugcraft.id(shape));
		return new TreeGrower(Jugcraft.MOD_ID + "_" + shape, WeightedList.of(tree), WeightedList.of(), WeightedList.of(), tree);
	}

	/**
	 * A tree with its own sapling and leaves, and its wood set (agriculture.TREES and WOOD_SETS in tools). Leaves with a
	 * {@code schedule} follow the seasons ({@link SeasonalLeavesBlock}); null: evergreen. The sapling and leaves copy
	 * {@code saplingLike} and {@code leavesLike} (sound, strength); leaves burn like vanilla leaves. A tree in
	 * GIANT_GROWERS has a {@link GiantSaplingBlock}: four in a square grow its giant.
	 */
	private static void registerTree(String tree, String leavesId, TreeGrower grower, SeasonalLeavesBlock.Schedule schedule,
			Block saplingLike, Block leavesLike, MapColor bark, MapColor inner) {
		TreeGrower giant = GIANT_GROWERS.get(tree);
		Block sapling = registerBlock(tree + "_sapling", giant == null ? props -> new SaplingBlock(grower, props) {
		} : props -> new GiantSaplingBlock(grower, giant, props), BlockBehaviour.Properties.ofFullCopy(saplingLike));
		registerItem(tree + "_sapling", props -> new BlockItem(sapling, props), new Item.Properties().useBlockDescriptionPrefix()
				.compostable(COMPOST_LOW), SEEDS_TAB);
		Function<BlockBehaviour.Properties, Block> leavesFactory = schedule == null
				? props -> new TintedParticleLeavesBlock(0.01F, props)
				: props -> new SeasonalLeavesBlock(schedule, props);
		Block leaves = registerBlock(leavesId, leavesFactory, BlockBehaviour.Properties.ofFullCopy(leavesLike).mapColor(MapColor.PLANT));
		registerItem(leavesId, props -> new BlockItem(leaves, props), new Item.Properties().useBlockDescriptionPrefix(), SEEDS_TAB);
		registerWoodSet(tree, bark, inner);
		FlammableBlockRegistry.getDefaultInstance().add(leaves, 30, 60);
	}

	/**
	 * A wood set like oak's: log, wood and their stripped forms, planks, stairs, slab, fence and fence gate, with block
	 * items in the building tab. Logs and wood strip with any axe; everything wooden burns like oak. {@code bark} is the
	 * map colour of the log and wood, {@code inner} of the stripped forms and everything made from planks.
	 */
	private static void registerWoodSet(String wood, MapColor bark, MapColor inner) {
		Block log = registerBlock(wood + "_log", props -> new StrippableLogBlock(props, "stripped_" + wood + "_log"),
				BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LOG).mapColor(bark));
		Block woodBlock = registerBlock(wood + "_wood", props -> new StrippableLogBlock(props, "stripped_" + wood + "_wood"),
				BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_WOOD).mapColor(bark));
		Block strippedLog = registerBlock("stripped_" + wood + "_log", RotatedPillarBlock::new,
				BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_OAK_LOG).mapColor(inner));
		Block strippedWood = registerBlock("stripped_" + wood + "_wood", RotatedPillarBlock::new,
				BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_OAK_WOOD).mapColor(inner));
		Block planks = registerBlock(wood + "_planks", Block::new, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).mapColor(inner));
		Block stairs = registerBlock(wood + "_stairs", props -> new StairBlock(planks.defaultBlockState(), props) {
		}, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_STAIRS).mapColor(inner));
		Block slab = registerBlock(wood + "_slab", SlabBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SLAB).mapColor(inner));
		Block fence = registerBlock(wood + "_fence", FenceBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_FENCE).mapColor(inner));
		Block gate = registerBlock(wood + "_fence_gate", props -> new FenceGateBlock(WoodType.OAK, props),
				BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_FENCE_GATE).mapColor(inner));
		// Furnace fuel like oak: a slab burns half as long as a block.
		for (Block block : List.of(log, woodBlock, strippedLog, strippedWood, planks, stairs, slab, fence, gate)) {
			String id = BuiltInRegistries.BLOCK.getKey(block).getPath();
			registerItem(id, props -> new BlockItem(block, props), new Item.Properties().useBlockDescriptionPrefix()
					.cookingFuel(block == slab ? ContextIntProviders.COOKING_TIME_WOOD_SLABS : ContextIntProviders.COOKING_TIME_WOOD_BLOCKS),
					BUILDING_TAB);
		}

		// Vanilla oak's fire behaviour: logs catch slowly, planks and their shapes faster.
		FlammableBlockRegistry fire = FlammableBlockRegistry.getDefaultInstance();
		for (Block block : List.of(log, woodBlock, strippedLog, strippedWood)) {
			fire.add(block, 5, 5);
		}
		for (Block block : List.of(planks, stairs, slab, fence, gate)) {
			fire.add(block, 5, 20);
		}
	}

	/**
	 * The apple tree: its sapling (planted from apple seeds) and leaves that blossom and fruit. Its trunk is vanilla oak, so
	 * it needs no wood of its own.
	 */
	private static void registerAppleTree() {
		registerBlock("apple_sapling", props -> new SaplingBlock(APPLE_GROWER, props) {
		}, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SAPLING));
		Block leaves = registerBlock("apple_leaves", AppleLeavesBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LEAVES)
				.mapColor(MapColor.PLANT));
		registerItem("apple_leaves", props -> new BlockItem(leaves, props), new Item.Properties().useBlockDescriptionPrefix(), SEEDS_TAB);
		FlammableBlockRegistry.getDefaultInstance().add(leaves, 30, 60);
	}

	/** Wild plant patches (data/jugcraft/worldgen) in the biomes each crop comes from. New chunks only. */
	private static void registerWorldgen() {
		if (!JugcraftConfig.isFeatureEnabled(FEATURE)) {
			Jugcraft.LOGGER.info("Wild crop worldgen disabled by config");
			return;
		}
		wildPatch("wild_corn", ConventionalBiomeTags.IS_PLAINS, ConventionalBiomeTags.IS_SAVANNA);
		wildPatch("wild_sunflower", ConventionalBiomeTags.IS_PLAINS);
		wildPatch("wild_beans", ConventionalBiomeTags.IS_FOREST, ConventionalBiomeTags.IS_JUNGLE);
		wildPatch("wild_sweet_potato", ConventionalBiomeTags.IS_SAVANNA, ConventionalBiomeTags.IS_JUNGLE);
		wildPatch("wild_flax", ConventionalBiomeTags.IS_PLAINS, ConventionalBiomeTags.IS_FLORAL);
		wildPatch("wild_tomato", ConventionalBiomeTags.IS_JUNGLE, ConventionalBiomeTags.IS_SAVANNA);
		wildPatch("wild_pepper", ConventionalBiomeTags.IS_SAVANNA, ConventionalBiomeTags.IS_BADLANDS);
		wildPatch("wild_onion", ConventionalBiomeTags.IS_PLAINS, ConventionalBiomeTags.IS_HILL);
		wildPatch("wild_garlic", ConventionalBiomeTags.IS_FOREST, ConventionalBiomeTags.IS_TAIGA);
		wildPatch("wild_cabbage", ConventionalBiomeTags.IS_WINDSWEPT, ConventionalBiomeTags.IS_HILL);
		wildPatch("wild_oats", ConventionalBiomeTags.IS_PLAINS, ConventionalBiomeTags.IS_TAIGA);
		wildPatch("wild_barley", ConventionalBiomeTags.IS_SAVANNA, ConventionalBiomeTags.IS_HILL);
		wildPatch("wild_turnip", ConventionalBiomeTags.IS_TAIGA, ConventionalBiomeTags.IS_BIRCH_FOREST);
		wildPatch("wild_mandrake", ConventionalBiomeTags.IS_SPOOKY);
		// Festival crops found as themselves: gourds on grass, ripe cranberries in swamp shallows, chestnut trees.
		wildPatch("butternut_squash", ConventionalBiomeTags.IS_PLAINS, ConventionalBiomeTags.IS_SAVANNA);
		wildPatch("acorn_squash", ConventionalBiomeTags.IS_FOREST, ConventionalBiomeTags.IS_TAIGA);
		wildPatch("warty_gourd", ConventionalBiomeTags.IS_SWAMP, ConventionalBiomeTags.IS_SPOOKY);
		wildPatch("cranberry_bush", ConventionalBiomeTags.IS_SWAMP);
		wildPatch("chestnut_tree", ConventionalBiomeTags.IS_FOREST);
		wildPatch("apple_tree", ConventionalBiomeTags.IS_PLAINS, ConventionalBiomeTags.IS_FLORAL);
		// Halloween harvest: heirloom pumpkins and bottle gourds on grass, and mums in flower-rich places.
		wildPatch("white_pumpkin", ConventionalBiomeTags.IS_BIRCH_FOREST, ConventionalBiomeTags.IS_SNOWY);
		wildPatch("jarrahdale_pumpkin", ConventionalBiomeTags.IS_SAVANNA, ConventionalBiomeTags.IS_WINDSWEPT);
		wildPatch("cinderella_pumpkin", ConventionalBiomeTags.IS_PLAINS, ConventionalBiomeTags.IS_FLORAL);
		wildPatch("red_kuri_pumpkin", ConventionalBiomeTags.IS_TAIGA, ConventionalBiomeTags.IS_SPOOKY);
		wildPatch("kabocha_pumpkin", ConventionalBiomeTags.IS_FOREST, ConventionalBiomeTags.IS_JUNGLE);
		wildPatch("bottle_gourd", ConventionalBiomeTags.IS_JUNGLE, ConventionalBiomeTags.IS_SAVANNA);
		wildPatch("mums", ConventionalBiomeTags.IS_FLORAL, ConventionalBiomeTags.IS_FOREST);
		// Autumn foraging: wild mushrooms on forest floors.
		wildPatch("chanterelle", ConventionalBiomeTags.IS_FOREST, ConventionalBiomeTags.IS_BIRCH_FOREST);
		wildPatch("porcini", ConventionalBiomeTags.IS_TAIGA, ConventionalBiomeTags.IS_FOREST);
		wildPatch("puffball", ConventionalBiomeTags.IS_PLAINS, ConventionalBiomeTags.IS_FOREST);
		wildPatch("fly_agaric", ConventionalBiomeTags.IS_BIRCH_FOREST, ConventionalBiomeTags.IS_TAIGA);
		wildPatch("jack_o_lantern_mushroom", ConventionalBiomeTags.IS_SPOOKY, ConventionalBiomeTags.IS_FOREST);
	}

	@SafeVarargs
	private static void wildPatch(String name, TagKey<Biome>... biomes) {
		Predicate<BiomeSelectionContext> selector = BiomeSelectors.tag(biomes[0]);
		for (int i = 1; i < biomes.length; i++) {
			selector = selector.or(BiomeSelectors.tag(biomes[i]));
		}
		ResourceKey<PlacedFeature> key = ResourceKey.create(Registries.PLACED_FEATURE, Jugcraft.id("patch_" + name));
		BiomeModifications.addFeature(BiomeSelectors.foundInOverworld().and(selector), GenerationStep.Decoration.VEGETAL_DECORATION, key);
	}

	// ---------------------------------------------------------------- registration helpers

	private static Block registerBlock(String id, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties properties) {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Jugcraft.id(id));
		Block block = Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(properties.setId(key)));
		BLOCKS.put(id, block);
		return block;
	}

	private static Item registerItem(String id, Function<Item.Properties, Item> factory, Item.Properties properties, List<Item> tab) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Jugcraft.id(id));
		Item item = Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
		ITEMS.put(id, item);
		tab.add(item);
		return item;
	}

	private static void crop(String id, String seedId, boolean legume) {
		registerBlock(id, props -> new JugcraftCropBlock(props, seedId, legume), BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT));
	}

	/** Shapes of each gourd facing north or south, then east or west (models: tools/festival_data.py). */
	private static final Map<String, VoxelShape[]> GOURD_SHAPES = Map.of(
			"butternut_squash", new VoxelShape[] {Block.box(4.0, 0.0, 1.0, 12.0, 8.0, 15.0), Block.box(1.0, 0.0, 4.0, 15.0, 8.0, 12.0)},
			"acorn_squash", new VoxelShape[] {Block.box(3.0, 0.0, 3.0, 13.0, 11.0, 13.0), Block.box(3.0, 0.0, 3.0, 13.0, 11.0, 13.0)},
			"warty_gourd", new VoxelShape[] {Block.box(4.0, 0.0, 4.0, 12.0, 14.0, 12.0), Block.box(4.0, 0.0, 4.0, 12.0, 14.0, 12.0)},
			"white_pumpkin", new VoxelShape[] {Shapes.block(), Shapes.block()},
			"jarrahdale_pumpkin", new VoxelShape[] {Shapes.block(), Shapes.block()},
			"cinderella_pumpkin", new VoxelShape[] {Shapes.block(), Shapes.block()},
			"red_kuri_pumpkin", new VoxelShape[] {Shapes.block(), Shapes.block()},
			"kabocha_pumpkin", new VoxelShape[] {Shapes.block(), Shapes.block()},
			"bottle_gourd", new VoxelShape[] {Block.box(4.0, 0.0, 4.0, 12.0, 15.0, 12.0), Block.box(4.0, 0.0, 4.0, 12.0, 15.0, 12.0)});

	/** A squash or gourd block (with its item), its stem and its attached stem. The seeds are registered with the other items. */
	private static void gourd(String id, String seedId, float growthTime, MapColor color) {
		VoxelShape[] shapes = GOURD_SHAPES.get(id);
		// Pumpkins are whole blocks, like vanilla's; the smaller gourds let light and faces past them.
		BlockBehaviour.Properties properties = BlockBehaviour.Properties.ofFullCopy(Blocks.PUMPKIN).mapColor(color);
		Block gourd = registerBlock(id, props -> new GourdBlock(props, shapes[0], shapes[1]),
				shapes[0] == Shapes.block() ? properties : properties.noOcclusion());
		registerItem(id, props -> new BlockItem(gourd, props), new Item.Properties().useBlockDescriptionPrefix().compostable(COMPOST_MEDIUM),
				SEEDS_TAB);
		registerBlock(id + "_stem", props -> new GourdStemBlock(props, id, seedId, growthTime), BlockBehaviour.Properties.ofFullCopy(Blocks.PUMPKIN_STEM));
		registerBlock("attached_" + id + "_stem", props -> new AttachedGourdStemBlock(props, id, seedId),
				BlockBehaviour.Properties.ofFullCopy(Blocks.ATTACHED_PUMPKIN_STEM));
	}

	private static FoodProperties nourishment(int nutrition, float saturation) {
		return new FoodProperties.Builder().nutrition(nutrition).saturationModifier(saturation).build();
	}

	private static void food(String id, int nutrition, float saturation, ResourceKey<ContextIntProvider> compost) {
		registerItem(id, Item::new, new Item.Properties().food(nourishment(nutrition, saturation)).compostable(compost), FOOD_TAB);
	}

	/** A food that composters refuse, like vanilla cooked meat. */
	private static void meal(String id, int nutrition, float saturation) {
		registerItem(id, Item::new, new Item.Properties().food(nourishment(nutrition, saturation)), FOOD_TAB);
	}

	private static void plain(String id, ResourceKey<ContextIntProvider> compost) {
		registerItem(id, Item::new, new Item.Properties().compostable(compost), INGREDIENT_TAB);
	}

	private static void seeds(String id, String crop, ResourceKey<ContextIntProvider> compost) {
		Block block = block(crop);
		registerItem(id, props -> new BlockItem(block, props), new Item.Properties().useItemDescriptionPrefix().compostable(compost), SEEDS_TAB);
	}

	/** Seeds of a climbing crop: planted on a trellis standing on farmland. */
	private static void trellisSeeds(String id, TallCrop crop, ResourceKey<ContextIntProvider> compost) {
		registerItem(id, props -> new TrellisSeedItem(props, crop), new Item.Properties().compostable(compost), SEEDS_TAB);
	}

	/** A crop planted from its own produce that can also be eaten raw, like a potato. */
	private static void edibleSeeds(String id, String crop, int nutrition, float saturation, ResourceKey<ContextIntProvider> compost) {
		Block block = block(crop);
		registerItem(id, props -> new BlockItem(block, props), new Item.Properties().useItemDescriptionPrefix()
				.food(nourishment(nutrition, saturation)).compostable(compost), FOOD_TAB);
	}

	/** A treat on a stick, like a caramel apple: eating it leaves the stick. */
	private static void treat(String id, int nutrition, float saturation) {
		registerItem(id, Item::new, new Item.Properties().food(nourishment(nutrition, saturation)).usingConvertsTo(Items.STICK), FOOD_TAB);
	}

	/** A spooky sweet: a bite of sugar with a short effect, eaten even on a full stomach. */
	private static void sweet(String id, int nutrition, float saturation, Holder<MobEffect> effect, int seconds) {
		FoodProperties food = new FoodProperties.Builder().nutrition(nutrition).saturationModifier(saturation).alwaysEdible().build();
		Consumable eaten = Consumables.defaultFood().onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(effect, seconds * 20)))
				.build();
		registerItem(id, Item::new, new Item.Properties().food(food, eaten), FOOD_TAB);
	}

	/** A piece of the Candy Kettle's candy: quick to eat, even on a full stomach (a flavoured piece carries its effects). */
	private static void candy(String id, int nutrition, float saturation) {
		FoodProperties food = new FoodProperties.Builder().nutrition(nutrition).saturationModifier(saturation).alwaysEdible().build();
		registerItem(id, Item::new, new Item.Properties().food(food, Consumables.defaultFood().consumeSeconds(Candies.EAT_SECONDS).build())
				.compostable(COMPOST_MEDIUM_HIGH), FOOD_TAB);
	}

	/** A jar of preserves ({@link PreserveJarItem}): four servings of {@code nutrition} each, and an effect (or none). */
	private static void preserve(String id, int nutrition, float saturation, @Nullable Holder<MobEffect> effect, int seconds, int color) {
		registerItem(id, props -> new PreserveJarItem(props, nutrition, saturation, effect, seconds, color), new Item.Properties().stacksTo(16),
				FOOD_TAB);
	}

	/** A drink in a glass bottle, like a potion: drunk even on a full stomach for a short effect, leaving the bottle. */
	private static void drink(String id, int nutrition, float saturation, Holder<MobEffect> effect, int seconds) {
		drink(id, nutrition, saturation, effect, seconds, false);
	}

	/**
	 * As {@link #drink(String, int, float, Holder, int)}; {@code bottleBack}: crafting with it gives the bottle back (as
	 * vanilla's honey bottle does). Only for drinks no Cooking Pot recipe cooks into another bottled drink, since the pot
	 * hands remainders back too and the bottle would be doubled.
	 */
	private static void drink(String id, int nutrition, float saturation, Holder<MobEffect> effect, int seconds, boolean bottleBack) {
		FoodProperties food = new FoodProperties.Builder().nutrition(nutrition).saturationModifier(saturation).alwaysEdible().build();
		Consumable drunk = Consumables.defaultDrink().onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(effect, seconds * 20)))
				.build();
		Item.Properties properties = new Item.Properties().food(food, drunk).usingConvertsTo(Items.GLASS_BOTTLE).stacksTo(16);
		registerItem(id, Item::new, bottleBack ? properties.craftRemainder(Items.GLASS_BOTTLE) : properties, FOOD_TAB);
	}

	private static void stew(String id, int nutrition, float saturation) {
		registerItem(id, Item::new, new Item.Properties().food(nourishment(nutrition, saturation)).usingConvertsTo(Items.BOWL).stacksTo(1),
				FOOD_TAB);
	}

	/** A stew that also gives a short effect when eaten, leaving the bowl. */
	private static void stew(String id, int nutrition, float saturation, Holder<MobEffect> effect, int seconds) {
		Consumable eaten = Consumables.defaultFood().onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(effect, seconds * 20)))
				.build();
		registerItem(id, Item::new, new Item.Properties().food(nourishment(nutrition, saturation), eaten).usingConvertsTo(Items.BOWL)
				.stacksTo(1), FOOD_TAB);
	}

	private static void sickle(String id, int radius, int durability) {
		registerItem(id, props -> new SickleItem(props, radius), new Item.Properties().durability(durability), TOOL_TAB);
	}

	private static void wild(String id) {
		// Sweet berry bush properties: plant colour, no collision, broken by pistons; wheat's colour depends on its age.
		BlockBehaviour.Properties properties = BlockBehaviour.Properties.ofFullCopy(Blocks.SWEET_BERRY_BUSH).sound(SoundType.GRASS);
		Block block = registerBlock(id, WildCropBlock::new, properties);
		registerItem(id, props -> new BlockItem(block, props), new Item.Properties().useBlockDescriptionPrefix().compostable(COMPOST_MEDIUM),
				SEEDS_TAB);
	}
}
