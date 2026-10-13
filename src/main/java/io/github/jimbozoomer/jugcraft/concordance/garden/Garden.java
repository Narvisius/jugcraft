package io.github.jimbozoomer.jugcraft.concordance.garden;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceData;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import io.github.jimbozoomer.jugcraft.concordance.JugcraftConcordance;
import io.github.jimbozoomer.jugcraft.concordance.compose.Text;
import io.github.jimbozoomer.jugcraft.concordance.ecology.Disturbance;
import io.github.jimbozoomer.jugcraft.concordance.ecology.EcologyCatalog;
import io.github.jimbozoomer.jugcraft.concordance.ecology.Mulch;
import io.github.jimbozoomer.jugcraft.concordance.ecology.Nourishment;
import io.github.jimbozoomer.jugcraft.concordance.ecology.Organism;
import io.github.jimbozoomer.jugcraft.concordance.ecology.SampleBudget;
import io.github.jimbozoomer.jugcraft.concordance.ecology.Sampler;
import io.github.jimbozoomer.jugcraft.concordance.resource.ResourceType;
import io.github.jimbozoomer.jugcraft.concordance.rules.ConcordanceRules;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Function;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;
import org.jspecify.annotations.Nullable;

/**
 * The Greenwardens' garden on the server (roadmap step 14, docs/features/arcane-concordance-ecology.md). Organisms
 * grow on vanilla's random ticks (and a sprinkler's, which gives any crop an extra one): the same scheduler as every
 * Jugcraft crop, with the habitat deciding whether a tick may grow it, how fast, and at what cost in nutrients. The
 * area round a plant is read within the bounds {@link Sampler} sets, at most {@link SampleBudget#PER_TICK} samples a
 * level a tick, and never in a chunk that is not loaded. Light is the server's (sky or block), so a dynamic light, a
 * shader or a client's render distance never changes what grows.
 * <p>
 * Registers the beds, crops and living devices; everything stays registered whatever the config says.
 */
public final class Garden {
	public static final String RESEARCH = "jugcraft:verdant_husbandry";
	/** What a player examines to begin Verdant Husbandry. */
	public static final TagKey<Item> SPECIMENS = TagKey.create(Registries.ITEM, Jugcraft.id("verdant_specimens"));
	/** The practice a crop harvested by hand records; distinct crops count for mastery. */
	public static final String ACTIVITY = "jugcraft:cultivation";
	public static final ResourceType VERDANCE = ResourceType.essence("verdance");
	/** Verdance into a Ley Pylon's charge (data: concordance/conversion). */
	public static final String CONVERSION = "jugcraft:verdance_to_ley";
	/** Nutrients one dose of bone meal (or of fertilizer) gives a bed. Keep equal to tools/concordance_ecology.py. */
	public static final int BONE_MEAL_NUTRIENTS = 2;
	/** Beds one touch of a Greenwarden wakes. */
	public static final int AWAKEN_LIMIT = 64;
	/** Water (or a wet sprinkler) this far round a bed keeps it wet. */
	public static final int WATER_REACH = 4;

	/** What a Mulch Maw eats, by worth in quarters of a nutrient (data packs add to the tags). */
	public static final List<TagKey<Item>> MULCH = List.of(
			TagKey.create(Registries.ITEM, Jugcraft.id("mulch/quarter")), TagKey.create(Registries.ITEM, Jugcraft.id("mulch/half")),
			TagKey.create(Registries.ITEM, Jugcraft.id("mulch/three_quarters")), TagKey.create(Registries.ITEM, Jugcraft.id("mulch/whole")));

	public static Block VERDANT_BED;
	public static Block SUNPETAL_CROP;
	public static Block DEWMOSS_CROP;
	public static Block GLOAMCAP_CROP;
	public static Block MENDVETCH_CROP;
	public static Block VERDANT_HEART;
	public static Block MULCH_MAW;
	public static Block HABITAT_GAUGE;
	public static Block GLEANER;
	public static Item SUNPETAL;
	public static Item DEWMOSS;
	public static Item GLOAMCAP;
	public static Item MENDVETCH;
	public static Item VERDANT_CHAFF;
	public static BlockEntityType<VerdantBedBlockEntity> BED_ENTITY;
	public static BlockEntityType<VerdantHeartBlockEntity> HEART_ENTITY;
	public static BlockEntityType<MulchMawBlockEntity> MAW_ENTITY;
	public static BlockEntityType<HabitatGaugeBlockEntity> GAUGE_ENTITY;
	public static BlockEntityType<GleanerBlockEntity> GLEANER_ENTITY;

	private static final Map<ServerLevel, SampleBudget> BUDGETS = new WeakHashMap<>();
	private static @Nullable ConcordanceRules disturbanceFor;
	private static Map<Block, Integer> disturbanceByBlock = Map.of();
	private static List<Map.Entry<TagKey<Block>, Integer>> disturbanceByTag = List.of();

	private Garden() {
	}

	public static void register() {
		VERDANT_BED = block("verdant_bed", VerdantBedBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.DIRT)
				.strength(0.6F).sound(SoundType.ROOTED_DIRT).randomTicks());
		SUNPETAL_CROP = crop("sunpetal");
		DEWMOSS_CROP = crop("dewmoss");
		GLOAMCAP_CROP = crop("gloamcap");
		MENDVETCH_CROP = crop("mendvetch");
		VERDANT_HEART = block("verdant_heart", VerdantHeartBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.PLANT)
				.strength(1.0F).sound(SoundType.AZALEA).noOcclusion().pushReaction(PushReaction.IMMOVEABLE));
		MULCH_MAW = block("mulch_maw", MulchMawBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_ORANGE)
				.strength(1.0F).sound(SoundType.DECORATED_POT).noOcclusion().pushReaction(PushReaction.IMMOVEABLE));
		HABITAT_GAUGE = block("habitat_gauge", HabitatGaugeBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.PLANT)
				.strength(0.5F).sound(SoundType.AZALEA).noOcclusion().pushReaction(PushReaction.POPPED));
		GLEANER = block("gleaner", GleanerBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_ORANGE)
				.strength(1.0F).sound(SoundType.DECORATED_POT).noOcclusion().pushReaction(PushReaction.IMMOVEABLE));
		Item bed = item("verdant_bed", properties -> new GardenItem(VERDANT_BED, properties), new Item.Properties().useBlockDescriptionPrefix());
		SUNPETAL = item("sunpetal", properties -> new GardenItem(SUNPETAL_CROP, properties), new Item.Properties().useItemDescriptionPrefix()
				.compostable(ContextIntProviders.COMPOSTABLE_LOW));
		DEWMOSS = item("dewmoss", properties -> new GardenItem(DEWMOSS_CROP, properties), new Item.Properties().useItemDescriptionPrefix()
				.compostable(ContextIntProviders.COMPOSTABLE_LOW));
		GLOAMCAP = item("gloamcap", properties -> new GardenItem(GLOAMCAP_CROP, properties), new Item.Properties().useItemDescriptionPrefix()
				.compostable(ContextIntProviders.COMPOSTABLE_LOW));
		MENDVETCH = item("mendvetch", properties -> new GardenItem(MENDVETCH_CROP, properties), new Item.Properties().useItemDescriptionPrefix()
				.compostable(ContextIntProviders.COMPOSTABLE_LOW));
		VERDANT_CHAFF = item("verdant_chaff", GardenItem.Plain::new, new Item.Properties().compostable(ContextIntProviders.COMPOSTABLE_MEDIUM));
		Item heart = item("verdant_heart", properties -> new GardenItem(VERDANT_HEART, properties), new Item.Properties().useBlockDescriptionPrefix());
		Item maw = item("mulch_maw", properties -> new GardenItem(MULCH_MAW, properties), new Item.Properties().useBlockDescriptionPrefix());
		Item gauge = item("habitat_gauge", properties -> new GardenItem(HABITAT_GAUGE, properties), new Item.Properties().useBlockDescriptionPrefix());
		Item gleaner = item("gleaner", properties -> new GardenItem(GLEANER, properties), new Item.Properties().useBlockDescriptionPrefix());
		BED_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("verdant_bed"),
				FabricBlockEntityTypeBuilder.create(VerdantBedBlockEntity::new, VERDANT_BED).build());
		HEART_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("verdant_heart"),
				FabricBlockEntityTypeBuilder.create(VerdantHeartBlockEntity::new, VERDANT_HEART).build());
		MAW_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("mulch_maw"),
				FabricBlockEntityTypeBuilder.create(MulchMawBlockEntity::new, MULCH_MAW).build());
		GAUGE_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("habitat_gauge"),
				FabricBlockEntityTypeBuilder.create(HabitatGaugeBlockEntity::new, HABITAT_GAUGE).build());
		GLEANER_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("gleaner"),
				FabricBlockEntityTypeBuilder.create(GleanerBlockEntity::new, GLEANER).build());
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.NATURAL_BLOCKS).register(output -> {
			output.accept(bed);
			output.accept(SUNPETAL);
			output.accept(DEWMOSS);
			output.accept(GLOAMCAP);
			output.accept(MENDVETCH);
			output.accept(VERDANT_CHAFF);
		});
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(output -> {
			output.accept(heart);
			output.accept(maw);
			output.accept(gauge);
			output.accept(gleaner);
		});
	}

	/** A crop's block: a wheat-like plant, but not a copy of wheat's properties (wheat's colour reads its own age 0-7). */
	private static Block crop(String name) {
		return block(name + "_crop", properties -> new OrganismCropBlock(properties, Jugcraft.id(name)),
				BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).noCollision().randomTicks().instabreak().sound(SoundType.CROP)
						.pushReaction(PushReaction.POPPED));
	}

	private static Block block(String id, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties properties) {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Jugcraft.id(id));
		return Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(properties.setId(key)));
	}

	private static Item item(String id, Function<Item.Properties, Item> factory, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Jugcraft.id(id));
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
	}

	// ---------------------------------------------------------------- rules and who may work them

	public static EcologyCatalog catalog() {
		return ConcordanceData.rules().ecology();
	}

	public static boolean enabled() {
		return JugcraftConfig.isFeatureEnabled(JugcraftConcordance.FEATURE);
	}

	/** Whether the player has understood Verdant Husbandry: their touch wakes beds and devices. */
	public static boolean knows(Player player) {
		return ConcordanceProgress.knowledge(player).state(RESEARCH).atLeast(ResearchState.UNDERSTOOD);
	}

	/** The organism a block is, or null. */
	public static @Nullable Organism organism(BlockState state) {
		Identifier id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
		return catalog().byBlock(id.toString());
	}

	/** The area-sample allowance of a level (not saved: a fresh tick starts a fresh count). */
	public static SampleBudget budget(ServerLevel level) {
		return BUDGETS.computeIfAbsent(level, unused -> new SampleBudget());
	}

	// ---------------------------------------------------------------- what the sampler reads

	/** The kind of plant a block is, for diversity: any crop, flower or sapling, by block. */
	public static @Nullable String species(BlockState state) {
		if (state.isAir()) {
			return null;
		}
		if (state.getBlock() instanceof OrganismCropBlock || state.is(BlockTags.CROPS) || state.is(BlockTags.FLOWERS)
				|| state.is(BlockTags.SAPLINGS)) {
			return BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
		}
		return null;
	}

	/** What a block gives off into the habitats round it (data: concordance/disturbance). */
	public static int disturbance(BlockState state) {
		ConcordanceRules rules = ConcordanceData.rules();
		if (rules != disturbanceFor) {
			Map<Block, Integer> byBlock = new IdentityHashMap<>();
			List<Map.Entry<TagKey<Block>, Integer>> byTag = new ArrayList<>();
			for (Disturbance disturbance : rules.ecology().disturbances().values()) {
				for (String ref : disturbance.blocks()) {
					if (ref.startsWith("#")) {
						Identifier tag = Identifier.tryParse(ref.substring(1));
						if (tag != null) {
							byTag.add(Map.entry(TagKey.create(Registries.BLOCK, tag), disturbance.value()));
						}
					} else {
						Identifier id = Identifier.tryParse(ref);
						if (id != null) {
							BuiltInRegistries.BLOCK.getOptional(id).ifPresent(block -> byBlock.merge(block, disturbance.value(), Math::max));
						}
					}
				}
			}
			disturbanceByBlock = byBlock;
			disturbanceByTag = byTag;
			disturbanceFor = rules;
		}
		int value = disturbanceByBlock.getOrDefault(state.getBlock(), 0);
		for (Map.Entry<TagKey<Block>, Integer> entry : disturbanceByTag) {
			if (state.is(entry.getKey())) {
				value = Math.max(value, entry.getValue());
			}
		}
		return value;
	}

	/**
	 * Samples the area round a plant at {@code plant}, if the level's allowance has one left this tick; null when it
	 * does not. Positions in chunks that are not loaded read as empty: sampling never loads a chunk.
	 */
	public static Sampler.@Nullable Area sample(ServerLevel level, BlockPos plant) {
		if (!budget(level).take(level.getGameTime())) {
			return null;
		}
		BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
		return Sampler.sample(new Sampler.Probe() {
			@Override
			public @Nullable String species(int dx, int dz) {
				at.setWithOffset(plant, dx, 0, dz);
				return level.isLoaded(at) ? Garden.species(level.getBlockState(at)) : null;
			}

			@Override
			public int disturbance(int dx, int dy, int dz) {
				at.setWithOffset(plant, dx, dy, dz);
				return level.isLoaded(at) ? Garden.disturbance(level.getBlockState(at)) : 0;
			}
		});
	}

	/** The mulch worth of an item in quarters of a nutrient, or 0 when a Maw would not eat it. */
	public static int mulch(ItemStack stack) {
		for (int i = MULCH.size() - 1; i >= 0; i--) {
			if (stack.is(MULCH.get(i))) {
				return Math.min(Mulch.QUARTERS, i + 1);
			}
		}
		return 0;
	}

	/**
	 * Gives {@code amount} nutrients, one at a time, each to the poorest awake bed of {@code beds} with room (the first on a
	 * tie). Returns how many were given: what has nowhere to go is never made.
	 */
	public static int nourish(List<VerdantBedBlockEntity> beds, int amount) {
		int given = 0;
		for (int i = 0; i < amount; i++) {
			List<Integer> stores = new ArrayList<>();
			for (VerdantBedBlockEntity bed : beds) {
				stores.add(bed.awake() ? bed.nutrients() : VerdantBedBlockEntity.CAPACITY);
			}
			int poorest = Nourishment.poorest(stores, VerdantBedBlockEntity.CAPACITY);
			if (poorest < 0) {
				break;
			}
			beds.get(poorest).give(1);
			given++;
		}
		return given;
	}

	/**
	 * The positions within {@code reach} blocks of {@code center} across and {@code height} up or down, nearest first (the
	 * same order every time, so devices share and harvest the same way).
	 */
	public static List<BlockPos> nearest(BlockPos center, int reach, int height) {
		List<BlockPos> positions = new ArrayList<>();
		for (BlockPos pos : BlockPos.betweenClosed(center.offset(-reach, -height, -reach), center.offset(reach, height, reach))) {
			positions.add(pos.immutable());
		}
		positions.sort(Comparator.<BlockPos>comparingDouble(pos -> pos.distSqr(center)).thenComparingInt(BlockPos::getY)
				.thenComparingInt(BlockPos::getX).thenComparingInt(BlockPos::getZ));
		return positions;
	}

	/** The beds within {@code reach} blocks of {@code center} (and one above or below), nearest first, in loaded chunks. */
	public static List<VerdantBedBlockEntity> bedsAround(ServerLevel level, BlockPos center, int reach) {
		List<VerdantBedBlockEntity> beds = new ArrayList<>();
		for (BlockPos pos : nearest(center, reach, 1)) {
			if (level.isLoaded(pos) && level.getBlockEntity(pos) instanceof VerdantBedBlockEntity bed) {
				beds.add(bed);
			}
		}
		return beds;
	}

	/** The beds beside {@code bed} (north, south, east and west) and the bed itself, for a nitrogen fixer. */
	public static List<VerdantBedBlockEntity> bedAndNeighbours(ServerLevel level, BlockPos bed) {
		List<VerdantBedBlockEntity> beds = new ArrayList<>();
		if (level.getBlockEntity(bed) instanceof VerdantBedBlockEntity own) {
			beds.add(own);
		}
		for (Direction side : Direction.Plane.HORIZONTAL) {
			BlockPos next = bed.relative(side);
			if (level.isLoaded(next) && level.getBlockEntity(next) instanceof VerdantBedBlockEntity neighbour) {
				beds.add(neighbour);
			}
		}
		return beds;
	}

	// ---------------------------------------------------------------- words

	public static void tell(ServerPlayer player, String key, Object... args) {
		player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance." + key, args));
	}

	/** A status word ({@code compose.jugcraft.ecology.status.<id>}). */
	public static Component status(String id, Object... args) {
		return Component.translatable(Text.PREFIX + "ecology.status." + id, args);
	}
}
