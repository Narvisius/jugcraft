package io.github.jimbozoomer.jugcraft.concordance.spirits;

import com.mojang.brigadier.CommandDispatcher;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceData;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import io.github.jimbozoomer.jugcraft.concordance.JugcraftConcordance;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import io.github.jimbozoomer.jugcraft.concordance.sky.SkyItem;
import io.github.jimbozoomer.jugcraft.concordance.worker.WorkerCatalog;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

/**
 * Familiars, spirits and constructs on the server (roadmap step 17, docs/features/arcane-concordance-workers.md): three
 * separate models, each saved with its worker and never in its brain: the Hearthling familiar's bond, the Gathering
 * Shade's agreement (kept by its Spirit Anchor and sealed as a Bound Will) and the Clockwork Porter's body. Every worker
 * says what it is doing or why not; the {@code workers} command lists a player's workers from the saved roster,
 * loaded or not, without loading anything.
 */
public final class Workers {
	public static final String RESEARCH = "jugcraft:binding_arts";
	/** The practice a worker's service records (a familiar's mending, a spirit's delivery, a construct's trip). */
	public static final String ACTIVITY = "jugcraft:worker_service";
	public static final String FAMILIAR_SOURCE = "jugcraft:hearthling";
	/** Things that bind, which begin the Binding Arts when examined. */
	public static final TagKey<Item> SPECIMENS = TagKey.create(Registries.ITEM, Jugcraft.id("binding_specimens"));

	public static EntityType<HearthlingEntity> HEARTHLING;
	public static EntityType<GatheringShadeEntity> GATHERING_SHADE;
	public static EntityType<ClockworkPorterEntity> CLOCKWORK_PORTER;
	public static Block SPIRIT_ANCHOR;
	public static BlockEntityType<SpiritAnchorBlockEntity> ANCHOR_ENTITY;
	public static Item BONDING_CHARM;
	public static Item PORTER;
	public static Item PORTER_KEY;
	/** A Porter Key's route. */
	public static DataComponentType<ClockworkPorterEntity.Route> PORTER_ROUTE;

	private Workers() {
	}

	public static void register() {
		PORTER_ROUTE = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Jugcraft.id("porter_route"),
				DataComponentType.<ClockworkPorterEntity.Route>builder().persistent(ClockworkPorterEntity.Route.CODEC)
						.networkSynchronized(ByteBufCodecs.fromCodec(ClockworkPorterEntity.Route.CODEC)).build());
		HEARTHLING = entity("hearthling", EntityType.Builder.<HearthlingEntity>of(HearthlingEntity::new, MobCategory.MISC).sized(0.5F, 0.6F)
				.eyeHeight(0.4F).clientTrackingRange(8));
		GATHERING_SHADE = entity("gathering_shade", EntityType.Builder.<GatheringShadeEntity>of(GatheringShadeEntity::new, MobCategory.MISC)
				.sized(0.6F, 1.6F).eyeHeight(1.35F).clientTrackingRange(8));
		CLOCKWORK_PORTER = entity("clockwork_porter", EntityType.Builder.<ClockworkPorterEntity>of(ClockworkPorterEntity::new, MobCategory.MISC)
				.sized(0.8F, 1.0F).eyeHeight(0.8F).clientTrackingRange(8));
		FabricDefaultAttributeRegistry.register(HEARTHLING, HearthlingEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(GATHERING_SHADE, GatheringShadeEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(CLOCKWORK_PORTER, ClockworkPorterEntity.createAttributes());
		ResourceKey<Block> anchorKey = ResourceKey.create(Registries.BLOCK, Jugcraft.id("spirit_anchor"));
		SPIRIT_ANCHOR = Registry.register(BuiltInRegistries.BLOCK, anchorKey, new SpiritAnchorBlock(BlockBehaviour.Properties.of()
				.mapColor(MapColor.COLOR_PURPLE).strength(2.0F, 6.0F).sound(SoundType.DEEPSLATE).requiresCorrectToolForDrops()
				.lightLevel(state -> 5).pushReaction(PushReaction.IMMOVEABLE).setId(anchorKey)));
		ANCHOR_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("spirit_anchor"),
				FabricBlockEntityTypeBuilder.create(SpiritAnchorBlockEntity::new, SPIRIT_ANCHOR).build());
		// The anchor's item says what it is for, as the observatory's does.
		Item anchor = item("spirit_anchor", properties -> new SkyItem(SPIRIT_ANCHOR, properties), new Item.Properties().useBlockDescriptionPrefix());
		BONDING_CHARM = item("bonding_charm", BondingCharmItem::new, new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
		PORTER = item("clockwork_porter", ClockworkPorterItem::new, new Item.Properties().stacksTo(1));
		PORTER_KEY = item("porter_key", PorterKeyItem::new, new Item.Properties().stacksTo(1));
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> command(dispatcher));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(output -> output.accept(anchor));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(output -> {
			output.accept(BONDING_CHARM);
			output.accept(PORTER);
			output.accept(PORTER_KEY);
		});
	}

	private static <T extends net.minecraft.world.entity.Entity> EntityType<T> entity(String id, EntityType.Builder<T> builder) {
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, Jugcraft.id(id));
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
	}

	private static Item item(String id, Function<Item.Properties, Item> factory, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Jugcraft.id(id));
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
	}

	public static WorkerCatalog catalog() {
		return ConcordanceData.rules().workers();
	}

	public static boolean enabled() {
		return JugcraftConfig.isFeatureEnabled(JugcraftConcordance.FEATURE);
	}

	public static boolean knows(Player player) {
		return ConcordanceProgress.knowledge(player).state(RESEARCH).atLeast(ResearchState.UNDERSTOOD);
	}

	/** A tick of the day as a clock reads it (tick 0 is 06:00). */
	public static String clock(int tick) {
		int minutes = (tick * 60 / 1000 + 6 * 60) % (24 * 60);
		return String.format(java.util.Locale.ROOT, "%02d:%02d", minutes / 60, minutes % 60);
	}

	/** Every worker of {@code player} the roster knows: kind, last status, dimension and place, loaded or not. */
	public static void list(ServerPlayer player, java.util.function.Consumer<Component> out) {
		Map<UUID, WorkerRoster.Entry> workers = WorkerRoster.of(player.level().getServer()).workers(player.getUUID());
		if (workers.isEmpty()) {
			out.accept(Component.translatable("message.jugcraft.concordance.workers.none"));
			return;
		}
		for (WorkerRoster.Entry entry : workers.values()) {
			out.accept(Component.translatable("message.jugcraft.concordance.workers.line",
					Component.translatable("message.jugcraft.concordance.workers.kind." + entry.kind()),
					Component.translatable("compose.jugcraft.worker.status." + entry.status()), entry.dimension(),
					entry.blockPos().toShortString()));
		}
	}

	private static void command(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("jugcraft").then(Commands.literal("concordance").then(Commands.literal("workers")
				.executes(context -> {
					ServerPlayer player = context.getSource().getPlayerOrException();
					list(player, line -> context.getSource().sendSuccess(() -> line, false));
					return 1;
				}))));
	}
}
