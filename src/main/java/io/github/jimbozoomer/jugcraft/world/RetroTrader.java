package io.github.jimbozoomer.jugcraft.world;

import com.google.common.collect.ImmutableSet;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import io.github.jimbozoomer.jugcraft.mixin.StructureTemplatePoolAccessor;
import java.util.Collections;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.object.builder.v1.world.poi.PoiHelper;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;

/**
 * The Retro Trader: a villager profession whose job site is the arcade cabinet, and the Retro Game Shop that
 * brings one to every new village (one shop per village; see {@link RetroShopPlacement}). He always offers a Pixel
 * Hollows map, sells a little of the cave's palette and buys pixel shards back. His trades are data (villager_trade
 * and trade_set files generated from TRADES in tools/pixel_hollows.py).
 *
 * <p>The {@code retro_trader} switch stops new shops, the map trade and the cabinet recipe. The profession, the
 * cabinet, the map item and existing traders and their offers stay.
 */
public final class RetroTrader {
	public static final String FEATURE = "retro_trader";
	public static final ResourceKey<VillagerProfession> PROFESSION =
			ResourceKey.create(Registries.VILLAGER_PROFESSION, Jugcraft.id("retro_trader"));
	public static final ResourceKey<PoiType> JOB_SITE = ResourceKey.create(Registries.POINT_OF_INTEREST_TYPE, Jugcraft.id("arcade_cabinet"));
	public static final ResourceKey<StructureTemplatePool> PLAINS_HOUSES = houses("plains");
	/** The houses pools of the five village types; each new village of any of them gets one shop. Zombie villages do not. */
	public static final List<ResourceKey<StructureTemplatePool>> VILLAGE_HOUSES =
			List.of(PLAINS_HOUSES, houses("desert"), houses("savanna"), houses("snowy"), houses("taiga"));
	/** The shop's template (data/jugcraft/structure/village/plains/retro_game_shop.nbt, from tools/retro_game_shop.py). */
	public static final Identifier SHOP = Jugcraft.id("village/plains/retro_game_shop");
	/**
	 * The shop's weight in each houses pool; keep in sync with tools/pixel_hollows.py. It only puts the shop among the
	 * candidates: {@link RetroShopPlacement} makes it the first choice until one is placed, then removes it.
	 */
	public static final int SHOP_WEIGHT = 1;

	public static Block ARCADE_CABINET;
	public static Item PIXEL_HOLLOWS_MAP;
	public static SoundEvent WORK_SOUND;
	public static Holder<MapDecorationType> MAP_MARKER;
	private static final Set<StructureTemplatePool> EXTENDED = Collections.newSetFromMap(new WeakHashMap<>());
	/** The pool element added to this server's villages, or null while the switch is off (read by worldgen threads). */
	private static volatile StructurePoolElement shop;

	private RetroTrader() {
	}

	public static void register() {
		ResourceKey<Block> cabinetKey = ResourceKey.create(Registries.BLOCK, Jugcraft.id("arcade_cabinet"));
		ARCADE_CABINET = Registry.register(BuiltInRegistries.BLOCK, cabinetKey, new ArcadeCabinetBlock(
				// Immovable: a piston moving one half of a two-block cabinet would break the other and drop a cabinet.
				BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).strength(2.5F).noOcclusion().pushReaction(PushReaction.IMMOVEABLE)
						.lightLevel(state -> state.getValue(ArcadeCabinetBlock.HALF) == DoubleBlockHalf.UPPER ? 6 : 0)
						.setId(cabinetKey)));
		ResourceKey<Item> cabinetItem = ResourceKey.create(Registries.ITEM, Jugcraft.id("arcade_cabinet"));
		Registry.register(BuiltInRegistries.ITEM, cabinetItem,
				new BlockItem(ARCADE_CABINET, new Item.Properties().setId(cabinetItem).useBlockDescriptionPrefix()));

		Identifier workSound = Jugcraft.id("entity.villager.work_retro_trader");
		WORK_SOUND = Registry.register(BuiltInRegistries.SOUND_EVENT, workSound, SoundEvent.createVariableRangeEvent(workSound));
		MAP_MARKER = Registry.registerForHolder(BuiltInRegistries.MAP_DECORATION_TYPE, Jugcraft.id("pixel_hollows"),
				new MapDecorationType(Jugcraft.id("pixel_hollows"), true, false));

		// The lower half is the job site (unemployed villagers claim it through #minecraft:acquirable_job_site).
		List<BlockState> jobSite = ARCADE_CABINET.getStateDefinition().getPossibleStates().stream()
				.filter(state -> state.getValue(ArcadeCabinetBlock.HALF) == DoubleBlockHalf.LOWER).toList();
		PoiHelper.register(Jugcraft.id("arcade_cabinet"), 1, 1, jobSite);
		Registry.register(BuiltInRegistries.VILLAGER_PROFESSION, PROFESSION, new VillagerProfession(
				Component.translatable("entity.jugcraft.villager.retro_trader"),
				poi -> poi.is(JOB_SITE), poi -> poi.is(JOB_SITE), ImmutableSet.of(), ImmutableSet.of(), WORK_SOUND,
				// Trades are data: data/jugcraft/trade_set/retro_trader/level_<n>.json (from TRADES in tools/pixel_hollows.py).
				new Int2ObjectOpenHashMap<>(Map.of(1, key(Registries.TRADE_SET, "retro_trader/level_1"),
						2, key(Registries.TRADE_SET, "retro_trader/level_2"), 3, key(Registries.TRADE_SET, "retro_trader/level_3")))));
		ResourceKey<Item> mapKey = ResourceKey.create(Registries.ITEM, Jugcraft.id("pixel_hollows_map"));
		PIXEL_HOLLOWS_MAP = Registry.register(BuiltInRegistries.ITEM, mapKey, new PixelHollowsMapItem(new Item.Properties().setId(mapKey)));

		ServerLifecycleEvents.SERVER_STARTING.register(server -> addShopToVillages(server.registryAccess()));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(output -> output.accept(ARCADE_CABINET));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(output -> output.accept(PIXEL_HOLLOWS_MAP));
	}

	private static <T> ResourceKey<T> key(ResourceKey<? extends Registry<T>> registry, String path) {
		return ResourceKey.create(registry, Jugcraft.id(path));
	}

	private static ResourceKey<StructureTemplatePool> houses(String village) {
		return ResourceKey.create(Registries.TEMPLATE_POOL, Identifier.withDefaultNamespace("village/" + village + "/houses"));
	}

	/** Adds the shop to every village type's houses once per server (village pools are fixed while a world runs). */
	public static void addShopToVillages(RegistryAccess registries) {
		// The trader's trades are Pixel Hollows goods and load only with that switch on: no shop without them.
		if (!JugcraftConfig.isFeatureEnabled(FEATURE) || !JugcraftConfig.isFeatureEnabled(PixelHollows.FEATURE)) {
			shop = null;
			return;
		}
		StructurePoolElement element = StructurePoolElement.single(SHOP.toString()).apply(StructureTemplatePool.Projection.RIGID);
		int added = 0;
		for (ResourceKey<StructureTemplatePool> key : VILLAGE_HOUSES) {
			StructureTemplatePool pool = registries.lookupOrThrow(Registries.TEMPLATE_POOL).getValue(key);
			if (pool == null || !EXTENDED.add(pool)) {
				continue;
			}
			for (int i = 0; i < SHOP_WEIGHT; i++) {
				((StructureTemplatePoolAccessor) pool).jugcraft$templates().add(element);
			}
			added++;
		}
		if (added > 0) {
			shop = element;
			Jugcraft.LOGGER.info("Retro Game Shop added to {} village houses pools: one shop per new village", added);
		}
	}

	/** Whether the shop was added to this pool (only those pools are reordered; see RetroShopPlacement). */
	public static boolean isExtended(StructureTemplatePool pool) {
		return shop != null && EXTENDED.contains(pool);
	}

	/** The shop's pool element in this server's villages, or null when there is none. */
	public static StructurePoolElement shop() {
		return shop;
	}

	/** The shop's pool element in one houses pool, if it is there (for tests). */
	public static Optional<StructurePoolElement> shopElement(RegistryAccess registries, ResourceKey<StructureTemplatePool> key) {
		StructureTemplatePool pool = registries.lookupOrThrow(Registries.TEMPLATE_POOL).getValue(key);
		if (pool == null) {
			return Optional.empty();
		}
		return ((StructureTemplatePoolAccessor) pool).jugcraft$templates().stream()
				.filter(element -> element.toString().contains(SHOP.toString())).findFirst();
	}

	/** The shop's pool element in the plains houses, if it is there (for tests). */
	public static Optional<StructurePoolElement> shopElement(RegistryAccess registries) {
		return shopElement(registries, PLAINS_HOUSES);
	}
}
