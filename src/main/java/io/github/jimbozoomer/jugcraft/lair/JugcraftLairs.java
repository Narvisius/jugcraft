package io.github.jimbozoomer.jugcraft.lair;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.function.Function;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

/**
 * The lairs' registrations (docs/features/hollow-acre.md, docs/features/spindle-loft.md, docs/features/glacier-hall.md,
 * docs/features/cinder-kiln.md): the lair-only blocks (unbreakable, no items, no drops), the Mourning Wreath and the Death
 * Knell of the Last Rites, the Cursed Spindle, the Frost Horn, the Kiln Seal, the Mist Gate, and the lairs' events, rules,
 * rituals and commands. Registered whatever the feature switch says, so saved worlds keep them; the switch
 * ({@value #FEATURE}) stops the rituals.
 */
public final class JugcraftLairs {
	public static final String FEATURE = "agriculture";
	/** Glare ice is as slick as blue ice (tools/lairs.py GLARE_FRICTION). */
	public static final float GLARE_FRICTION = 0.989F;
	public static Block BLIGHTED_SOIL;
	public static Block BLACK_WHEAT;
	public static Block MOWN_STUBBLE;
	public static Block LAIR_BRAZIER;
	public static Block LAIR_MOON;
	public static Block LAIR_EXIT;
	public static Block DOILY_LACE;
	public static Block SPOOL_WOOD;
	public static Block SPOOL_THREAD;
	public static Block PINCUSHION;
	public static Block PINCUSHION_SEAM;
	public static Block PINCUSHION_LEAF;
	public static Block NEEDLE_STEEL;
	public static Block PIN_SHAFT;
	public static Block MEASURING_TAPE;
	public static Block THIMBLE_METAL;
	public static Block TAUT_THREAD;
	public static Block GRIMY_SKYLIGHT;
	public static Block DRIFT_SNOW;
	public static Block TRAMPLED_SNOW;
	public static Block GLARE_ICE;
	public static Block GIANT_ICICLE;
	public static Block MAMMOTH_TUSK;
	public static Block FROZEN_HOARD;
	public static Block KILN_BRICK;
	public static Block CRACKED_BASALT;
	public static Block MOLTEN_SLAG;
	public static Block TROUGH_STONE;
	public static Block SLUICE_GATE;
	public static Block MOURNING_WREATH;
	public static Item MOURNING_WREATH_ITEM;
	public static Item DEATH_KNELL;
	public static Item CURSED_SPINDLE;
	public static Item FROST_HORN;
	public static Item KILN_SEAL;
	public static EntityType<MistGateEntity> MIST_GATE;

	private JugcraftLairs() {
	}

	public static void register() {
		// The lair-only blocks: like bedrock, they cannot be broken or blown up; their loot tables are empty.
		BLIGHTED_SOIL = fixture("blighted_soil", Block::new, MapColor.COLOR_BLACK, SoundType.ROOTED_DIRT, 0, Fit.SOLID);
		BLACK_WHEAT = fixture("black_wheat", p -> new LairPlantBlock(14, p), MapColor.COLOR_BLACK, SoundType.CROP, 0, Fit.PASSABLE);
		MOWN_STUBBLE = fixture("mown_stubble", p -> new LairPlantBlock(4, p), MapColor.TERRACOTTA_BROWN, SoundType.CROP, 0, Fit.PASSABLE);
		LAIR_BRAZIER = fixture("lair_brazier", LairBrazierBlock::new, MapColor.COLOR_BLACK, SoundType.LANTERN, -1, Fit.SOLID);
		LAIR_MOON = fixture("lair_moon", LairMoonBlock::new, MapColor.GOLD, SoundType.STONE, 15, Fit.SOLID);
		LAIR_EXIT = fixture("lair_exit", LairExitBlock::new, MapColor.COLOR_LIGHT_GRAY, SoundType.WOOL, 6, Fit.PASSABLE);
		// The Spindle Loft's (docs/features/spindle-loft.md).
		DOILY_LACE = fixture("doily_lace", DoilyLaceBlock::new, MapColor.WOOL, SoundType.WOOL, 0, Fit.SEE_THROUGH);
		SPOOL_WOOD = fixture("spool_wood", Block::new, MapColor.WOOD, SoundType.WOOD, 0, Fit.SOLID);
		SPOOL_THREAD = fixture("spool_thread", SpoolThreadBlock::new, MapColor.WOOL, SoundType.WOOL, 0, Fit.SOLID);
		PINCUSHION = fixture("pincushion", Block::new, MapColor.COLOR_RED, SoundType.WOOL, 0, Fit.SOLID);
		PINCUSHION_SEAM = fixture("pincushion_seam", Block::new, MapColor.COLOR_RED, SoundType.WOOL, 0, Fit.SOLID);
		PINCUSHION_LEAF = fixture("pincushion_leaf", Block::new, MapColor.COLOR_GREEN, SoundType.WOOL, 0, Fit.SOLID);
		NEEDLE_STEEL = fixture("needle_steel", Block::new, MapColor.METAL, SoundType.METAL, 0, Fit.SOLID);
		PIN_SHAFT = fixture("pin_shaft", PinShaftBlock::new, MapColor.METAL, SoundType.METAL, 0, Fit.SEE_THROUGH);
		MEASURING_TAPE = fixture("measuring_tape", MeasuringTapeBlock::new, MapColor.COLOR_YELLOW, SoundType.WOOL, 0, Fit.SEE_THROUGH);
		THIMBLE_METAL = fixture("thimble_metal", Block::new, MapColor.GOLD, SoundType.METAL, 0, Fit.SOLID);
		TAUT_THREAD = fixture("taut_thread", TautThreadBlock::new, MapColor.WOOL, SoundType.WOOL, 0, Fit.PASSABLE);
		GRIMY_SKYLIGHT = fixture("grimy_skylight", p -> new TransparentBlock(p) {
		}, MapColor.NONE, SoundType.GLASS, 0, Fit.SEE_THROUGH);
		// The Glacier Hall's (docs/features/glacier-hall.md).
		DRIFT_SNOW = fixture("drift_snow", Block::new, MapColor.SNOW, SoundType.SNOW, 0, Fit.SOLID);
		TRAMPLED_SNOW = fixture("trampled_snow", TrampledSnowBlock::new, MapColor.SNOW, SoundType.SNOW, 0, Fit.SOLID);
		GLARE_ICE = fixture("glare_ice", p -> new Block(p.friction(GLARE_FRICTION)), MapColor.ICE, SoundType.GLASS, 0, Fit.SOLID);
		GIANT_ICICLE = fixture("giant_icicle", GiantIcicleBlock::new, MapColor.ICE, SoundType.GLASS, 0, Fit.SEE_THROUGH);
		MAMMOTH_TUSK = fixture("mammoth_tusk", Block::new, MapColor.SAND, SoundType.BONE_BLOCK, 0, Fit.SOLID);
		FROZEN_HOARD = fixture("frozen_hoard", Block::new, MapColor.ICE, SoundType.GLASS, 0, Fit.SOLID);
		// The Cinder Kiln's (docs/features/cinder-kiln.md): the slag glows; the troughs and the sluice gates work.
		KILN_BRICK = fixture("kiln_brick", Block::new, MapColor.TERRACOTTA_YELLOW, SoundType.STONE, 0, Fit.SOLID);
		CRACKED_BASALT = fixture("cracked_basalt", Block::new, MapColor.COLOR_BLACK, SoundType.DEEPSLATE, 0, Fit.SOLID);
		MOLTEN_SLAG = fixture("molten_slag", MoltenSlagBlock::new, MapColor.COLOR_ORANGE, SoundType.DEEPSLATE, 10, Fit.SOLID);
		TROUGH_STONE = fixture("trough_stone", TroughStoneBlock::new, MapColor.STONE, SoundType.STONE, 0, Fit.SOLID);
		SLUICE_GATE = fixture("sluice_gate", SluiceGateBlock::new, MapColor.METAL, SoundType.METAL, 0, Fit.SEE_THROUGH);

		ResourceKey<Block> wreathKey = ResourceKey.create(Registries.BLOCK, Jugcraft.id("mourning_wreath"));
		MOURNING_WREATH = Registry.register(BuiltInRegistries.BLOCK, wreathKey, new MourningWreathBlock(BlockBehaviour.Properties.of()
				.mapColor(MapColor.COLOR_GREEN).strength(0.2F).sound(SoundType.AZALEA_LEAVES).noOcclusion().noCollision()
				.pushReaction(PushReaction.POPPED).setId(wreathKey)));
		ResourceKey<Item> wreathItemKey = ResourceKey.create(Registries.ITEM, Jugcraft.id("mourning_wreath"));
		MOURNING_WREATH_ITEM = Registry.register(BuiltInRegistries.ITEM, wreathItemKey,
				new BlockItem(MOURNING_WREATH, new Item.Properties().useBlockDescriptionPrefix().setId(wreathItemKey)));
		ResourceKey<Item> knellKey = ResourceKey.create(Registries.ITEM, Jugcraft.id("death_knell"));
		DEATH_KNELL = Registry.register(BuiltInRegistries.ITEM, knellKey, new DeathKnellItem(new Item.Properties().stacksTo(1).setId(knellKey)));
		ResourceKey<Item> spindleKey = ResourceKey.create(Registries.ITEM, Jugcraft.id("cursed_spindle"));
		CURSED_SPINDLE = Registry.register(BuiltInRegistries.ITEM, spindleKey, new CursedSpindleItem(new Item.Properties().stacksTo(16)
				.setId(spindleKey)));
		ResourceKey<Item> hornKey = ResourceKey.create(Registries.ITEM, Jugcraft.id("frost_horn"));
		FROST_HORN = Registry.register(BuiltInRegistries.ITEM, hornKey, new FrostHornItem(new Item.Properties().stacksTo(16).setId(hornKey)));
		ResourceKey<Item> sealKey = ResourceKey.create(Registries.ITEM, Jugcraft.id("kiln_seal"));
		KILN_SEAL = Registry.register(BuiltInRegistries.ITEM, sealKey, new KilnSealItem(new Item.Properties().stacksTo(16).setId(sealKey)));

		ResourceKey<EntityType<?>> gateKey = ResourceKey.create(Registries.ENTITY_TYPE, Jugcraft.id("mist_gate"));
		MIST_GATE = Registry.register(BuiltInRegistries.ENTITY_TYPE, gateKey, EntityType.Builder.<MistGateEntity>of(MistGateEntity::new,
				MobCategory.MISC).sized(1.6F, 2.6F).noSummon().clientTrackingRange(8).updateInterval(20).build(gateKey));
		UseEntityCallback.EVENT.register((player, level, hand, entity, hit) ->
				entity instanceof MistGateEntity gate ? gate.use(player, hand) : InteractionResult.PASS);

		Lairs.register();
		LairRules.register();
		SpindleRite.register();
		LairCommands.register();
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(output -> {
			output.accept(DEATH_KNELL);
			output.accept(CURSED_SPINDLE);
			output.accept(FROST_HORN);
			output.accept(KILN_SEAL);
		});
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(output -> output.accept(MOURNING_WREATH_ITEM));
	}

	/** How a lair-only block takes up its space: a solid block, one you see through but stand on, or one you pass through. */
	private enum Fit {
		SOLID, SEE_THROUGH, PASSABLE
	}

	private static Block fixture(String id, Function<BlockBehaviour.Properties, Block> make, MapColor colour, SoundType sound, int light,
			Fit fit) {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Jugcraft.id(id));
		BlockBehaviour.Properties properties = BlockBehaviour.Properties.of().mapColor(colour).sound(sound).strength(-1.0F, 3600000.0F)
				.pushReaction(PushReaction.IMMOVEABLE).setId(key);
		if (light > 0) {
			properties = properties.lightLevel(state -> light);
		} else if (light < 0) {
			properties = properties.lightLevel(state -> state.getValue(LairBrazierBlock.LIT) ? 12 : 0);
		}
		if (fit == Fit.PASSABLE) {
			properties = properties.noCollision().noOcclusion();
		} else if (fit == Fit.SEE_THROUGH) {
			// As glass: nothing behind it is hidden, it never suffocates, and nothing spawns on it.
			properties = properties.noOcclusion().isValidSpawn((state, level, pos, type) -> false).isRedstoneConductor((state, level, pos) -> false)
					.isSuffocating((state, level, pos) -> false);
		}
		return Registry.register(BuiltInRegistries.BLOCK, key, make.apply(properties));
	}
}
