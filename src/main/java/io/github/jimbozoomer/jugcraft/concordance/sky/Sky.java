package io.github.jimbozoomer.jugcraft.concordance.sky;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.HarvestMoon;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceData;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceEffects;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import io.github.jimbozoomer.jugcraft.concordance.JugcraftConcordance;
import io.github.jimbozoomer.jugcraft.concordance.Saved;
import io.github.jimbozoomer.jugcraft.concordance.celestial.Attunement;
import io.github.jimbozoomer.jugcraft.concordance.celestial.Calendar;
import io.github.jimbozoomer.jugcraft.concordance.celestial.CelestialCatalog;
import io.github.jimbozoomer.jugcraft.concordance.celestial.Pattern;
import io.github.jimbozoomer.jugcraft.concordance.effect.Cause;
import io.github.jimbozoomer.jugcraft.concordance.effect.EffectKind;
import io.github.jimbozoomer.jugcraft.concordance.effect.EffectSpec;
import io.github.jimbozoomer.jugcraft.concordance.effect.Intent;
import io.github.jimbozoomer.jugcraft.concordance.effect.Ledger;
import io.github.jimbozoomer.jugcraft.concordance.effect.Stacking;
import io.github.jimbozoomer.jugcraft.concordance.resource.ResourceKind;
import io.github.jimbozoomer.jugcraft.concordance.resource.ResourceType;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import io.github.jimbozoomer.jugcraft.season.JugcraftSeasons;
import io.github.jimbozoomer.jugcraft.season.SeasonCalendar;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import org.jspecify.annotations.Nullable;

/**
 * The Starwatchers' sky on the server (roadmap step 15, docs/features/arcane-concordance-celestial.md). The calendar is
 * the overworld's clock ({@link Level#getOverworldClockTime}), the clock vanilla's sun and moon follow, so every client
 * and every save agrees on what is up; the rendered sky, a shader's moon or a client's settings never decide anything.
 * An observatory gathers Astral Resonance from each occurrence once per keeper ({@link AstralClaims}); an astrolabe
 * carries it and attunes its holder to a pattern; attunements are worked by a pulse every {@value #PULSE_TICKS} ticks
 * over the players online, through the shared effect boundary.
 */
public final class Sky {
	public static final String RESEARCH = "jugcraft:celestial_attunement";
	/** The practice an observation records; distinct patterns count for mastery. */
	public static final String ACTIVITY = "jugcraft:observation";
	public static final ResourceType ASTRAL = ResourceType.of(ResourceKind.ASTRAL_RESONANCE);
	public static final int PULSE_TICKS = 100;
	/** How long each pulse's attunement effect lasts, a little past the next pulse. */
	public static final int EFFECT_TICKS = 140;
	public static final String SOURCE = "jugcraft:attunement";

	public static Block OBSERVATORY;
	public static Item ASTROLABE;
	public static BlockEntityType<ObservatoryBlockEntity> OBSERVATORY_ENTITY;
	/** The Astral Resonance an astrolabe carries. */
	public static DataComponentType<Integer> ASTRAL_CHARGE;
	/** On an astrolabe holding any resonance (what LambDynamicLights, if present, makes glow in hand). */
	public static DataComponentType<Unit> RESONANT;
	/** What a player examines to begin Celestial Attunement. */
	public static final TagKey<Item> SPECIMENS = TagKey.create(Registries.ITEM, Jugcraft.id("celestial_specimens"));
	/** How many days the observatory's and the command's forecast look ahead. */
	public static final int FORECAST_DAYS = 8;
	/** A player's attunement (saved with them, lost on death). */
	public static AttachmentType<Attunement> ATTUNEMENT;

	private static final Codec<Attunement> ATTUNEMENT_CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.STRING.fieldOf("pattern").forGetter(Attunement::pattern), Codec.LONG.fieldOf("until").forGetter(Attunement::until),
			Codec.BOOL.fieldOf("recalled").forGetter(Attunement::recalled)).apply(i, Attunement::new));

	private Sky() {
	}

	public static void register() {
		ASTRAL_CHARGE = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Jugcraft.id("astral_charge"),
				DataComponentType.<Integer>builder().persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT).build());
		RESONANT = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Jugcraft.id("resonant"),
				DataComponentType.<Unit>builder().persistent(MapCodec.unitCodec(Unit.INSTANCE)).networkSynchronized(Unit.STREAM_CODEC).build());
		ATTUNEMENT = AttachmentRegistry.<Attunement>builder().persistent(Saved.versioned("celestial_attunement", ATTUNEMENT_CODEC)).buildAndRegister(Jugcraft.id("celestial_attunement"));
		OBSERVATORY = block("observatory", ObservatoryBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.GOLD)
				.strength(2.5F, 6.0F).sound(SoundType.COPPER).requiresCorrectToolForDrops().noOcclusion().pushReaction(PushReaction.IMMOVEABLE));
		Item observatory = item("observatory", properties -> new SkyItem(OBSERVATORY, properties), new Item.Properties().useBlockDescriptionPrefix());
		ASTROLABE = item("astrolabe", AstrolabeItem::new, new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
		OBSERVATORY_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("observatory"),
				FabricBlockEntityTypeBuilder.create(ObservatoryBlockEntity::new, OBSERVATORY).build());
		ServerTickEvents.END_SERVER_TICK.register(Sky::tick);
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> command(dispatcher));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(output -> output.accept(observatory));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(output -> output.accept(ASTROLABE));
	}

	private static Block block(String id, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties properties) {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Jugcraft.id(id));
		return Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(properties.setId(key)));
	}

	private static Item item(String id, Function<Item.Properties, Item> factory, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Jugcraft.id(id));
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
	}

	// ---------------------------------------------------------------- the rules and the sky

	public static CelestialCatalog catalog() {
		return ConcordanceData.rules().celestial();
	}

	public static boolean enabled() {
		return JugcraftConfig.isFeatureEnabled(JugcraftConcordance.FEATURE);
	}

	public static ResearchState state(Player player) {
		return ConcordanceProgress.knowledge(player).state(RESEARCH);
	}

	/** The world's clock: the overworld's day time in every dimension (the clock vanilla's sun and moon follow). */
	public static long time(Level level) {
		return level.getOverworldClockTime();
	}

	/** The Jugcraft season today ("winter" ... "autumn", or "off" when seasons are off): the established calendar. */
	public static String season() {
		return SeasonCalendar.seasonName(JugcraftSeasons.today());
	}

	/** Whether {@code player} has understood Celestial Attunement (aligns observatories, attunes). */
	public static boolean knows(Player player) {
		return state(player).atLeast(ResearchState.UNDERSTOOD);
	}

	/**
	 * Whether a pattern's time of year allows it at world time {@code time}: no season; its Jugcraft season (any, when
	 * seasons are switched off); or, for the Harvest Moon, the Halloween event's own Harvest Moon being up
	 * ({@link HarvestMoon#rising}), so the two never disagree.
	 */
	public static boolean inSeason(Pattern pattern, long time) {
		if (pattern.season() == null) {
			return true;
		}
		if (pattern.season().equals("harvest_moon")) {
			return HarvestMoon.rising(time);
		}
		String season = season();
		return season.equals("off") || season.equals(pattern.season());
	}

	/**
	 * Why the sky over {@code pos} hides a pattern: {@code "elsewhere"} (only the Overworld has this sky),
	 * {@code "no_sky"} (a block that stops movement stands anywhere above it, glass and leaves included, by the
	 * server's heightmap, which changes the moment a block is placed) or {@code "clouded"} (rain or a storm, for a
	 * pattern that needs a clear sky); {@code null} when it can be seen. The server's level decides; never a client's
	 * rendered sky.
	 */
	public static @Nullable String obscured(ServerLevel level, BlockPos pos, Pattern pattern) {
		if (level.dimension() != Level.OVERWORLD) {
			return "elsewhere";
		}
		if (level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ()) > pos.getY() + 1) {
			return "no_sky";
		}
		return pattern.clearSky() && level.isRaining() ? "clouded" : null;
	}

	public static boolean visible(ServerLevel level, BlockPos pos, Pattern pattern) {
		return obscured(level, pos, pattern) == null;
	}

	/** The patterns up at {@code time} (their day and hours) in their season, in id order. */
	public static List<Pattern> up(long time) {
		List<Pattern> out = new ArrayList<>();
		for (Pattern pattern : catalog().patterns().values()) {
			if (Calendar.active(pattern, time) && inSeason(pattern, time)) {
				out.add(pattern);
			}
		}
		return out;
	}

	public static Component patternName(String id) {
		net.minecraft.resources.Identifier key = net.minecraft.resources.Identifier.tryParse(id);
		return key == null ? Component.literal(id) : Component.translatable("pattern." + key.getNamespace() + "." + key.getPath());
	}

	/** A tick of the day as a clock reads it (tick 0 is 06:00, 18000 midnight). */
	public static String clock(long time) {
		int tick = Calendar.tickOfDay(time);
		int minutes = (tick * 60 / 1000 + 6 * 60) % (24 * 60);
		return String.format(java.util.Locale.ROOT, "%02d:%02d", minutes / 60, minutes % 60);
	}

	private static Component seasonName(String season) {
		return Component.translatable("message.jugcraft.concordance.sky.season." + season);
	}

	/**
	 * The forecast as lines: today (the day, the moon and the season), then each window open now or opening within
	 * {@code days} days, soonest first (at most eight): when, the weather it needs and the time of year it keeps to.
	 */
	public static List<Component> forecast(long time, int days) {
		List<Component> lines = new ArrayList<>();
		lines.add(Component.translatable("message.jugcraft.concordance.sky.today", Calendar.day(time), clock(time),
				Component.translatable("message.jugcraft.concordance.sky.moon." + Calendar.moonPhase(time)), seasonName(season())));
		int shown = 0;
		for (Calendar.Window window : Calendar.forecast(catalog().patterns().values(), time, days)) {
			if (shown++ >= 8) {
				break;
			}
			Pattern pattern = window.pattern();
			Component when;
			if (window.open(time)) {
				when = Component.translatable("message.jugcraft.concordance.sky.up", clock(window.end()));
			} else {
				long ahead = Calendar.day(window.start()) - Calendar.day(time);
				when = ahead == 0 ? Component.translatable("message.jugcraft.concordance.sky.tonight", clock(window.start()))
						: Component.translatable("message.jugcraft.concordance.sky.rises", ahead, clock(window.start()));
			}
			Component weather = Component.translatable(pattern.clearSky() ? "message.jugcraft.concordance.sky.clear" : "message.jugcraft.concordance.sky.any");
			Component season = pattern.season() == null ? Component.empty()
					: Component.translatable("message.jugcraft.concordance.sky.only", seasonName(pattern.season()));
			lines.add(Component.translatable("message.jugcraft.concordance.sky.line", patternName(pattern.id()), when, weather, season));
		}
		if (shown == 0) {
			lines.add(Component.translatable("message.jugcraft.concordance.sky.none", days));
		}
		return lines;
	}

	// ---------------------------------------------------------------- attunements

	/**
	 * Every {@value #PULSE_TICKS} ticks: each online player attuned to a pattern gets its effect for
	 * {@value #EFFECT_TICKS} ticks while the pattern is up (or while a recall lasts), through the shared effect boundary.
	 * An attunement past its end is removed.
	 */
	private static void tick(MinecraftServer server) {
		if (server.getTickCount() % PULSE_TICKS != 0 || !enabled()) {
			return;
		}
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			Attunement attunement = player.getAttached(ATTUNEMENT);
			if (attunement != null) {
				pulse(player, attunement);
			}
		}
	}

	/** One pulse for one player at the level's own times. */
	public static boolean pulse(ServerPlayer player, Attunement attunement) {
		ServerLevel level = (ServerLevel) player.level();
		return pulse(player, attunement, time(level), level.getGameTime());
	}

	/**
	 * One pulse for one player at world time {@code time} and game time {@code gameTime} (tests pass their own): past its
	 * end the attunement is removed; otherwise, while its pattern is up and in season (or for a recall, whatever the sky),
	 * its effect is given through the shared effect boundary. Returns whether it was given.
	 */
	public static boolean pulse(ServerPlayer player, Attunement attunement, long time, long gameTime) {
		ServerLevel level = (ServerLevel) player.level();
		if (!attunement.active(gameTime)) {
			player.removeAttached(ATTUNEMENT);
			return false;
		}
		Pattern pattern = catalog().pattern(attunement.pattern());
		if (pattern == null || !attunement.recalled() && (!Calendar.active(pattern, time) || !inSeason(pattern, time))) {
			return false;
		}
		Cause cause = Cause.of(player.getUUID(), Cause.Origin.ITEM, SOURCE, ConcordanceEffects.nextSerial());
		Ledger ledger = new Ledger(new Ledger.Limits(1, EffectKind.STATUS.work, 0));
		EffectSpec spec = new EffectSpec(EffectKind.STATUS, Intent.HELPFUL, pattern.amplifier(), EFFECT_TICKS, pattern.effect(),
				Stacking.STRONGEST, null);
		return ConcordanceEffects.apply(new ConcordanceEffects.Context(level, cause, player, ledger, "attunement", player.position()), spec, player).applied();
	}

	// ---------------------------------------------------------------- the forecast command

	private static void command(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("jugcraft").then(Commands.literal("concordance").then(Commands.literal("sky")
				.executes(context -> {
					CommandSourceStack source = context.getSource();
					for (Component line : forecast(time(source.getLevel()), FORECAST_DAYS)) {
						source.sendSuccess(() -> line, false);
					}
					return 1;
				}))));
	}
}
