package io.github.jimbozoomer.jugcraft.concordance;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.ember.Ember;
import io.github.jimbozoomer.jugcraft.concordance.rules.FocusPool;
import io.github.jimbozoomer.jugcraft.concordance.rules.Knowledge;
import io.github.jimbozoomer.jugcraft.concordance.trinket.Wayfaring;
import io.github.jimbozoomer.jugcraft.energy.EnergyStorage;
import java.util.List;
import java.util.function.Function;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.PackType;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Unit;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

/**
 * The Arcane Concordance (docs/ARCANE_CONCORDANCE.md, docs/features/arcane-concordance-first-light.md): Jugcraft's
 * magic, learned by observing the world rather than bought with a mana bar. This registers milestone 1, First Light:
 * the luminous specimens to examine, the Lampwright's Bench to study at, the Kindle invocation (cast through Spell
 * Engine), the Kindled mote it leaves and the Kindled Lantern.
 * <p>
 * Everything stays registered whatever the config says, so saved blocks, items and player data are never lost;
 * {@code concordance.enabled=false} stops the recipes, examination, study, workings and casts.
 */
public final class JugcraftConcordance {
	public static final String FEATURE = "concordance";
	/** What can be examined and studied: every research entry's specimens (tools/concordance.py SPECIMEN_TAGS). */
	public static final TagKey<Item> SPECIMENS = TagKey.create(Registries.ITEM, Jugcraft.id("concordance_specimens"));
	/** First Light's specimens, which hold their own light (tools/concordance.py SPECIMENS). */
	public static final TagKey<Item> LUMINOUS = TagKey.create(Registries.ITEM, Jugcraft.id("luminous_specimens"));
	/** Circle Lore's specimens, made to hold a shape or a bearing (tools/concordance_rituals.py). */
	public static final TagKey<Item> CIRCLE_SPECIMENS = TagKey.create(Registries.ITEM, Jugcraft.id("circle_specimens"));
	/** The Alembic Arts' specimens: alchemical ingredients to examine (tools/concordance_alchemy.py). */
	public static final TagKey<Item> ALCHEMY_SPECIMENS = TagKey.create(Registries.ITEM, Jugcraft.id("alchemy_specimens"));
	/** Hearthbinding's specimens: fuels and things that hold fire (tools/concordance_ember.py). */
	public static final TagKey<Item> EMBER_SPECIMENS = TagKey.create(Registries.ITEM, Jugcraft.id("ember_specimens"));
	/** What casts Concordance invocations from the main hand. */
	public static final TagKey<Item> INSTRUMENTS = TagKey.create(Registries.ITEM, Jugcraft.id("concordance_instruments"));

	/** The Radiance in a Kindled Lantern. */
	public static DataComponentType<LanternCharge> RADIANCE;
	/** Present while a Kindled Lantern is lit: the lit model and LambDynamicLights both look for it. */
	public static DataComponentType<Unit> LANTERN_LIT;
	/** What a sheet of Research Notes records (absent on a blank sheet). */
	public static DataComponentType<ResearchNotes> RESEARCH_NOTES;
	/** The spell inscribed on an instrument (absent when none is). */
	public static DataComponentType<Inscription> INSCRIPTION;
	/** The invocations tuned on an instrument and the modifier each carries (roadmap step 10). */
	public static DataComponentType<Tunings> TUNINGS;
	/** The Ley Charge a broken Ley Pylon keeps (roadmap step 12). */
	public static DataComponentType<Integer> LEY_CHARGE;
	/** Roadmap step 13: a prepared ingredient, a bottled dose, and a recorded process (canonical text). */
	public static DataComponentType<Reagent> REAGENT;
	public static DataComponentType<Brew> BREW;
	public static DataComponentType<String> FORMULA;

	public static Block LUMEN_MOTE;
	public static Block LAMPWRIGHT_BENCH;
	public static Item INITIATE_WAND;
	public static Item KINDLED_LANTERN;
	public static Item RESEARCH_NOTES_ITEM;
	public static Block LUMEN_SCONCE;
	public static BlockEntityType<LampwrightBenchBlockEntity> BENCH_ENTITY;
	public static BlockEntityType<LumenSconceBlockEntity> SCONCE_ENTITY;
	/** Roadmap step 12: rituals. */
	public static Block CIRCLE_ANCHOR;
	public static Block LEY_PYLON;
	public static Block WARDING_STONE;
	public static Item ADEPT_WAND;
	public static BlockEntityType<CircleAnchorBlockEntity> ANCHOR_ENTITY;
	public static BlockEntityType<LeyPylonBlockEntity> PYLON_ENTITY;
	/** Roadmap step 13: alchemy. */
	public static Block CRUCIBLE;
	public static Item MORTAR;
	public static Item STIRRING_ROD;
	public static Item SAMPLING_SPOON;
	public static Item ASSAY_GLASS;
	public static Item FORMULA_ITEM;
	public static Item REAGENT_ITEM;
	public static Item DRAUGHT;
	public static Item SALVE;
	public static BlockEntityType<CrucibleBlockEntity> CRUCIBLE_ENTITY;
	public static ExtendedMenuType<LampwrightBenchMenu, BlockPos> BENCH_MENU;

	/** What a player has learned (saved with them, kept through death, sent only to them). */
	public static AttachmentType<Knowledge> KNOWLEDGE;
	/** A player's Focus (likewise). */
	public static AttachmentType<FocusPool> FOCUS;

	public static SoundEvent KINDLE_GATHER_SOUND;
	public static SoundEvent KINDLE_SOUND;
	public static SoundEvent EXAMINE_SOUND;
	public static SoundEvent STUDY_COMPLETE_SOUND;
	public static SoundEvent LANTERN_IGNITE_SOUND;
	public static SoundEvent LANTERN_SNUFF_SOUND;
	public static SoundEvent CIRCLE_START_SOUND;
	public static SoundEvent CIRCLE_STEP_SOUND;
	public static SoundEvent CIRCLE_COMPLETE_SOUND;
	public static SoundEvent CIRCLE_BREAK_SOUND;
	public static SoundEvent CRUCIBLE_STIR_SOUND;
	public static SoundEvent CRUCIBLE_ADD_SOUND;
	public static SoundEvent CRUCIBLE_BOTTLE_SOUND;
	/** The two warnings' own sounds (roadmap step 27): something is lacking; something is dangerous. */
	public static SoundEvent SIGN_SHORTAGE_SOUND;
	public static SoundEvent SIGN_DANGER_SOUND;

	private JugcraftConcordance() {
	}

	public static void register() {
		RADIANCE = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Jugcraft.id("radiance"),
				DataComponentType.<LanternCharge>builder().persistent(LanternCharge.CODEC).networkSynchronized(LanternCharge.STREAM_CODEC).build());
		LANTERN_LIT = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Jugcraft.id("lantern_lit"),
				DataComponentType.<Unit>builder().persistent(MapCodec.unitCodec(Unit.INSTANCE)).networkSynchronized(Unit.STREAM_CODEC).build());
		RESEARCH_NOTES = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Jugcraft.id("research_notes"),
				DataComponentType.<ResearchNotes>builder().persistent(ResearchNotes.CODEC).networkSynchronized(ResearchNotes.STREAM_CODEC).build());
		INSCRIPTION = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Jugcraft.id("inscription"),
				DataComponentType.<Inscription>builder().persistent(Inscription.CODEC).networkSynchronized(Inscription.STREAM_CODEC).build());
		TUNINGS = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Jugcraft.id("tunings"),
				DataComponentType.<Tunings>builder().persistent(Tunings.CODEC).networkSynchronized(Tunings.STREAM_CODEC).build());
		LEY_CHARGE = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Jugcraft.id("ley_charge"),
				DataComponentType.<Integer>builder().persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT).build());
		REAGENT = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Jugcraft.id("reagent"),
				DataComponentType.<Reagent>builder().persistent(Reagent.CODEC).networkSynchronized(Reagent.STREAM_CODEC).build());
		BREW = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Jugcraft.id("brew"),
				DataComponentType.<Brew>builder().persistent(Brew.CODEC).networkSynchronized(Brew.STREAM_CODEC).build());
		FORMULA = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Jugcraft.id("formula"),
				DataComponentType.<String>builder().persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8).build());

		KNOWLEDGE = AttachmentRegistry.<Knowledge>builder().persistent(Saved.versioned("concordance_knowledge", ConcordanceCodecs.KNOWLEDGE)).copyOnDeath()
				.syncWith(ConcordanceCodecs.KNOWLEDGE_STREAM, AttachmentSyncPredicate.targetOnly())
				.buildAndRegister(Jugcraft.id("concordance_knowledge"));
		FOCUS = AttachmentRegistry.<FocusPool>builder().persistent(Saved.versioned("concordance_focus", ConcordanceCodecs.FOCUS)).copyOnDeath()
				.syncWith(ConcordanceCodecs.FOCUS_STREAM, AttachmentSyncPredicate.targetOnly())
				.buildAndRegister(Jugcraft.id("concordance_focus"));

		KINDLE_GATHER_SOUND = sound("concordance.kindle_gather");
		KINDLE_SOUND = sound("concordance.kindle");
		EXAMINE_SOUND = sound("concordance.examine");
		STUDY_COMPLETE_SOUND = sound("concordance.study_complete");
		LANTERN_IGNITE_SOUND = sound("concordance.lantern_ignite");
		LANTERN_SNUFF_SOUND = sound("concordance.lantern_snuff");
		// The invocations' release sounds, and Ember's cast sound (tools/concordance_ember.py): Spell Engine looks each up
		// in the sound registry when a cast starts or is released.
		for (String release : List.of("aegis", "revelation", "lance", "flash", "lanternward", "ember_gather", "hearthspark",
				"hearthguard", "cinderbolt", "hearthflare")) {
			sound("concordance." + release);
		}
		CIRCLE_START_SOUND = sound("concordance.circle_start");
		CIRCLE_STEP_SOUND = sound("concordance.circle_step");
		CIRCLE_COMPLETE_SOUND = sound("concordance.circle_complete");
		CIRCLE_BREAK_SOUND = sound("concordance.circle_break");
		CRUCIBLE_STIR_SOUND = sound("concordance.crucible_stir");
		CRUCIBLE_ADD_SOUND = sound("concordance.crucible_add");
		CRUCIBLE_BOTTLE_SOUND = sound("concordance.crucible_bottle");
		SIGN_SHORTAGE_SOUND = sound("concordance.sign_shortage");
		SIGN_DANGER_SOUND = sound("concordance.sign_danger");

		// The Kindled mote is light in the air: nothing to see, hit, break or hold.
		LUMEN_MOTE = block("lumen_mote", LumenMoteBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.NONE)
				.replaceable().noCollision().noOcclusion().instabreak().noLootTable().pushReaction(PushReaction.POPPED)
				.lightLevel(LumenMoteBlock::light));
		LAMPWRIGHT_BENCH = block("lampwright_bench", LampwrightBenchBlock::new, BlockBehaviour.Properties.of()
				.mapColor(MapColor.WOOD).strength(2.0F).sound(SoundType.WOOD).noOcclusion()
				.lightLevel(state -> state.getValue(LampwrightBenchBlock.WORKING) ? 7 : 0));
		Item bench = item("lampwright_bench", properties -> new BlockItem(LAMPWRIGHT_BENCH, properties),
				new Item.Properties().useBlockDescriptionPrefix());
		INITIATE_WAND = item("initiate_wand", InitiateWandItem::new, new Item.Properties().stacksTo(1));
		KINDLED_LANTERN = item("kindled_lantern", KindledLanternItem::new, new Item.Properties().stacksTo(1)
				.rarity(Rarity.UNCOMMON).component(RADIANCE, LanternCharge.EMPTY));

		RESEARCH_NOTES_ITEM = item("research_notes", ResearchNotesItem::new, new Item.Properties().stacksTo(16));
		LUMEN_SCONCE = block("lumen_sconce", LumenSconceBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.GOLD)
				.strength(1.5F).sound(SoundType.LANTERN).noOcclusion().pushReaction(PushReaction.POPPED)
				.lightLevel(state -> state.getValue(LumenSconceBlock.LIT) ? 15 : 0));
		Item sconce = item("lumen_sconce", properties -> new BlockItem(LUMEN_SCONCE, properties),
				new Item.Properties().useBlockDescriptionPrefix());

		BENCH_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("lampwright_bench"),
				FabricBlockEntityTypeBuilder.create(LampwrightBenchBlockEntity::new, LAMPWRIGHT_BENCH).build());
		SCONCE_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("lumen_sconce"),
				FabricBlockEntityTypeBuilder.create(LumenSconceBlockEntity::new, LUMEN_SCONCE).build());
		// Roadmap step 12: the anchor rituals are worked at, the pylons that feed them and the stones that contain them.
		CIRCLE_ANCHOR = block("circle_anchor", CircleAnchorBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.STONE)
				.strength(3.0F, 6.0F).sound(SoundType.STONE).requiresCorrectToolForDrops().noOcclusion().pushReaction(PushReaction.IMMOVEABLE)
				.lightLevel(state -> 4));
		Item anchor = item("circle_anchor", properties -> new BlockItem(CIRCLE_ANCHOR, properties), new Item.Properties().useBlockDescriptionPrefix());
		LEY_PYLON = block("ley_pylon", LeyPylonBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLUE)
				.strength(2.5F, 6.0F).sound(SoundType.COPPER).requiresCorrectToolForDrops().noOcclusion().pushReaction(PushReaction.IMMOVEABLE)
				.lightLevel(state -> state.getValue(LeyPylonBlock.CHARGED) ? 7 : 0));
		Item pylon = item("ley_pylon", properties -> new BlockItem(LEY_PYLON, properties), new Item.Properties().useBlockDescriptionPrefix());
		WARDING_STONE = block("warding_stone", WardingStoneBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.STONE)
				.strength(2.0F, 6.0F).sound(SoundType.STONE).requiresCorrectToolForDrops().pushReaction(PushReaction.IMMOVEABLE));
		Item stone = item("warding_stone", properties -> new BlockItem(WARDING_STONE, properties), new Item.Properties().useBlockDescriptionPrefix());
		ADEPT_WAND = item("adept_wand", InitiateWandItem::new, new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
		ANCHOR_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("circle_anchor"),
				FabricBlockEntityTypeBuilder.create(CircleAnchorBlockEntity::new, CIRCLE_ANCHOR).build());
		PYLON_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("ley_pylon"),
				FabricBlockEntityTypeBuilder.create(LeyPylonBlockEntity::new, LEY_PYLON).build());
		// A pylon takes electricity through Jugcraft's one energy interface; it never gives any back.
		EnergyStorage.SIDED.registerForBlockEntity((entity, side) -> entity.energy, PYLON_ENTITY);
		// Roadmap step 13: the crucible, its tools, and the brews it bottles.
		CRUCIBLE = block("crucible", CrucibleBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
				.strength(2.5F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion());
		Item crucible = item("crucible", properties -> new BlockItem(CRUCIBLE, properties), new Item.Properties().useBlockDescriptionPrefix());
		MORTAR = item("mortar", MortarItem::new, new Item.Properties().stacksTo(1));
		STIRRING_ROD = item("stirring_rod", AlchemyItem::new, new Item.Properties().stacksTo(1));
		SAMPLING_SPOON = item("sampling_spoon", AlchemyItem::new, new Item.Properties().stacksTo(1));
		ASSAY_GLASS = item("assay_glass", AlchemyItem::new, new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
		FORMULA_ITEM = item("formula", AlchemyItem::new, new Item.Properties().stacksTo(16));
		REAGENT_ITEM = item("reagent", AlchemyItem::new, new Item.Properties().stacksTo(64));
		DRAUGHT = item("draught", properties -> new BrewItem(properties, false), new Item.Properties().stacksTo(16)
				.usingConvertsTo(Items.GLASS_BOTTLE).component(DataComponents.CONSUMABLE, Consumables.defaultDrink().build()));
		SALVE = item("salve", properties -> new BrewItem(properties, true), new Item.Properties().stacksTo(16));
		CRUCIBLE_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("crucible"),
				FabricBlockEntityTypeBuilder.create(CrucibleBlockEntity::new, CRUCIBLE).build());
		// Water by pipe into the crucible's tank (a formula's water steps draw on it); items through its container.
		FluidStorage.SIDED.registerForBlockEntity((entity, side) -> entity.water, CRUCIBLE_ENTITY);
		// Roadmap step 14: the Greenwardens' beds, crops and living devices.
		io.github.jimbozoomer.jugcraft.concordance.garden.Garden.register();
		// Roadmap step 15: the Starwatchers' observatory, astrolabe and attunements.
		io.github.jimbozoomer.jugcraft.concordance.sky.Sky.register();
		// Roadmap step 16: the Crimson Vigil: offerings, Vitae, exhaustion and the living Thornheart Blade.
		io.github.jimbozoomer.jugcraft.concordance.vigil.Vigil.register();
		// Roadmap step 17: familiars, spirits and constructs.
		io.github.jimbozoomer.jugcraft.concordance.spirits.Workers.register();
		// Roadmap step 18: Courier Posts, the logistics ledger and porters as couriers.
		io.github.jimbozoomer.jugcraft.concordance.courier.Couriers.register();
		// Roadmap step 19: Runesmithing: the Artificer's Bench and Resonant Rings.
		io.github.jimbozoomer.jugcraft.concordance.smithy.Artificery.register();
		// Roadmap step 20: relics in their explicit contexts, and the Reliquary Shrine.
		io.github.jimbozoomer.jugcraft.concordance.reliquary.Reliquary.register();
		// Roadmap step 21: bounded material equivalence, the Assayer's Scale and its Prima Materia ledger.
		io.github.jimbozoomer.jugcraft.concordance.assay.Assaying.register();
		// Roadmap step 22: sympathetic links, curses and wards; dream expeditions at the Oneiric Censer.
		io.github.jimbozoomer.jugcraft.concordance.sympathy.Sympathy.register();
		io.github.jimbozoomer.jugcraft.concordance.dreaming.Dreaming.register();
		// Roadmap step 23: the Starbound Conclave: renown, ranks, commissions, shared projects and the Conclave Lectern.
		io.github.jimbozoomer.jugcraft.concordance.starbound.Starbound.register();
		// Roadmap step 24: each player's stage of the Concordance, read from the progression graph's stages.
		io.github.jimbozoomer.jugcraft.concordance.stages.StageProgress.register();
		// Roadmap step 25: the Concord Spire, the endgame wonder raised round a Spire Heart and kept.
		io.github.jimbozoomer.jugcraft.concordance.spire.ConcordSpire.register();
		// Roadmap step 26: the Concordance Journal, every report in one place for the screens.
		io.github.jimbozoomer.jugcraft.concordance.journal.Journal.register();
		io.github.jimbozoomer.jugcraft.concordance.sign.Signs.register();
		BENCH_MENU = Registry.register(BuiltInRegistries.MENU, Jugcraft.id("lampwright_bench"),
				new ExtendedMenuType<>((containerId, inventory, pos) -> new LampwrightBenchMenu(containerId, inventory), BlockPos.STREAM_CODEC.cast()));

		ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(ConcordanceData.ID, ConcordanceData.INSTANCE);
		// New rules can teach or take away invocations: work them out again for everyone online, and for each joiner.
		ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resources, success) -> ConcordanceProgress.relearnAll(server));
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> ConcordanceProgress.relearn(handler.player));

		Examination.register();
		Ember.register();
		Wayfaring.register();
		RateGate.register();
		ConcordanceSpells.register();
		Invocations.register();
		ComposedSpells.register();
		Rituals.register();
		ConcordanceCommand.register();

		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(output -> {
			output.accept(INITIATE_WAND);
			output.accept(KINDLED_LANTERN);
			ItemStack full = new ItemStack(KINDLED_LANTERN);
			KindledLanternItem.set(full, KindledLanternItem.CAPACITY, 0L, false);
			output.accept(full);
			output.accept(RESEARCH_NOTES_ITEM);
			output.accept(ADEPT_WAND);
			output.accept(MORTAR);
			output.accept(STIRRING_ROD);
			output.accept(SAMPLING_SPOON);
			output.accept(ASSAY_GLASS);
			output.accept(FORMULA_ITEM);
		});
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(output -> {
			output.accept(bench);
			output.accept(sconce);
			output.accept(anchor);
			output.accept(pylon);
			output.accept(stone);
			output.accept(crucible);
		});
	}

	private static SoundEvent sound(String id) {
		Identifier key = Jugcraft.id(id);
		return Registry.register(BuiltInRegistries.SOUND_EVENT, key, SoundEvent.createVariableRangeEvent(key));
	}

	private static Block block(String id, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties properties) {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Jugcraft.id(id));
		return Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(properties.setId(key)));
	}

	private static Item item(String id, Function<Item.Properties, Item> factory, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Jugcraft.id(id));
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
	}
}
