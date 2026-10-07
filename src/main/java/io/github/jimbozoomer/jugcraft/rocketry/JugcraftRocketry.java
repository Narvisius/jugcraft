package io.github.jimbozoomer.jugcraft.rocketry;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.prospecting.OreSurvey;
import io.github.jimbozoomer.jugcraft.prospecting.SurveyPayload;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.saveddata.WeatherData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Rocketry (batch 38, docs/features/rocketry.md): its items, and the rockets in flight. A fired rocket is booked here
 * and does its work {@link #LAUNCH_DELAY} ticks later, {@link #ARRIVAL_HEIGHT} blocks above where it went up. Flights
 * are not saved: one in the air when the server stops is lost, like a firework. Numbers: tools/rocketry.py.
 */
public final class JugcraftRocketry {
	public static final int LAUNCH_DELAY = 40;
	public static final int ARRIVAL_HEIGHT = 30;
	public static final int SURVEY_RADIUS = 3;
	public static final int SURVEY_STRIDE = 4;
	public static final int WEATHER_TICKS = 6_000;
	public static final int WEATHER_COOLDOWN = 2_400;
	public static final int FLARE_RADIUS = 48;
	public static final int GLOW_TICKS = 600;
	public static final int SIGNAL_RANGE = 512;
	public static final int COOLDOWN = 20;

	/** Materials and parts (no behaviour of their own), with a grey tooltip line where they have one. */
	private static final String[][] PARTS = {{"iodine", "t"}, {"ammonium_perchlorate", "t"}, {"silver_iodide", "t"},
			{"solid_propellant", "t"}, {"rocket_casing", ""}, {"rocket_nozzle", ""}, {"guidance_unit", ""}, {"rocket_motor", "t"}};
	public static final List<Item> ITEMS = new ArrayList<>();
	public static Item SURVEY_ROCKET;
	public static Item CLOUD_SEEDING_ROCKET;
	public static Item CLEAR_SKY_ROCKET;
	public static Item SIGNAL_FLARE;
	public static Item ILLUMINATION_FLARE;
	/** Batch 39: the rocket post. */
	public static Item DELIVERY_ROCKET;
	public static Item FLIGHT_PLAN;
	public static Block ROCKET_PAD;
	public static BlockEntityType<RocketPadBlockEntity> ROCKET_PAD_ENTITY;
	public static ExtendedMenuType<RocketPadMenu, BlockPos> ROCKET_PAD_MENU;
	public static DataComponentType<GlobalPos> FLIGHT_TARGET;
	/** Batch 40: the zipline. */
	public static Item LINE_ROCKET;
	public static Block ZIPLINE_ANCHOR;
	public static BlockEntityType<ZiplineAnchorBlockEntity> ZIPLINE_ANCHOR_ENTITY;
	public static EntityType<ZiplineRider> ZIPLINE_RIDER;
	/** Batch 41: the rocket launcher. */
	public static Item ROCKET_LAUNCHER;
	public static Item HE_ROCKET;
	public static Item HOMING_ROCKET;
	public static EntityType<CombatRocket> COMBAT_ROCKET;
	/** Batch 42: booster rails, fuelled with solid propellant. */
	public static Item SOLID_PROPELLANT;
	public static Block BOOSTER_RAIL;
	public static BlockEntityType<BoosterRailBlockEntity> BOOSTER_RAIL_ENTITY;

	private record Flight(ServerLevel level, UUID player, String name, RocketItem.Kind kind, Vec3 at, long due) {
	}

	private static final List<Flight> FLIGHTS = new ArrayList<>();
	private static long nextWeather;

	private JugcraftRocketry() {
	}

	public static void register() {
		for (String[] part : PARTS) {
			boolean described = !part[1].isEmpty();
			String path = part[0];
			Item registered = item(path, properties -> new Item(properties) {
				@Override
				public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
						Consumer<Component> tooltip, TooltipFlag flag) {
					if (described) {
						tooltip.accept(Component.translatable("tooltip.jugcraft." + path).withStyle(ChatFormatting.GRAY));
					}
				}
			});
			if (path.equals("solid_propellant")) {
				SOLID_PROPELLANT = registered;
			}
		}
		SURVEY_ROCKET = rocket("survey_rocket", RocketItem.Kind.SURVEY);
		CLOUD_SEEDING_ROCKET = rocket("cloud_seeding_rocket", RocketItem.Kind.RAIN);
		CLEAR_SKY_ROCKET = rocket("clear_sky_rocket", RocketItem.Kind.CLEAR);
		SIGNAL_FLARE = rocket("signal_flare", RocketItem.Kind.SIGNAL);
		ILLUMINATION_FLARE = rocket("illumination_flare", RocketItem.Kind.ILLUMINATION);
		registerPost();
		registerZipline();
		registerLauncher();
		registerBoosterRail();
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(output -> ITEMS.forEach(output::accept));
		ServerTickEvents.END_SERVER_TICK.register(JugcraftRocketry::tick);
		ServerTickEvents.END_SERVER_TICK.register(RocketPost::tick);
		ServerTickEvents.END_SERVER_TICK.register(BoosterRails::tick);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			FLIGHTS.clear();
			BoosterRails.clear();
			nextWeather = 0;
		});
	}

	/** Batch 39: the rocket pad, delivery rockets and flight plans. */
	private static void registerPost() {
		FLIGHT_TARGET = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Jugcraft.id("flight_target"),
				DataComponentType.<GlobalPos>builder().persistent(GlobalPos.CODEC).networkSynchronized(GlobalPos.STREAM_CODEC).build());
		DELIVERY_ROCKET = item("delivery_rocket", properties -> new Item(properties.stacksTo(16)) {
			@Override
			public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
					Consumer<Component> tooltip, TooltipFlag flag) {
				tooltip.accept(Component.translatable("tooltip.jugcraft.delivery_rocket").withStyle(ChatFormatting.GRAY));
			}
		});
		FLIGHT_PLAN = item("flight_plan", properties -> new FlightPlanItem(properties.stacksTo(1)));
		ResourceKey<Block> padKey = ResourceKey.create(Registries.BLOCK, Jugcraft.id("rocket_pad"));
		ROCKET_PAD = Registry.register(BuiltInRegistries.BLOCK, padKey, new RocketPadBlock(
				BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(2.0F).noOcclusion().setId(padKey)));
		item("rocket_pad", properties -> new BlockItem(ROCKET_PAD, properties.useBlockDescriptionPrefix()));
		ROCKET_PAD_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("rocket_pad"),
				FabricBlockEntityTypeBuilder.create(RocketPadBlockEntity::new, ROCKET_PAD).build());
		ROCKET_PAD_MENU = Registry.register(BuiltInRegistries.MENU, Jugcraft.id("rocket_pad"),
				new ExtendedMenuType<>((containerId, inventory, pos) -> new RocketPadMenu(containerId, inventory), BlockPos.STREAM_CODEC.cast()));
	}

	/** Batch 40: zipline anchors, the line-throwing rocket and the trolley riders hang from. */
	private static void registerZipline() {
		LINE_ROCKET = item("line_rocket", properties -> new LineRocketItem(properties.stacksTo(16)));
		ResourceKey<Block> anchorKey = ResourceKey.create(Registries.BLOCK, Jugcraft.id("zipline_anchor"));
		ZIPLINE_ANCHOR = Registry.register(BuiltInRegistries.BLOCK, anchorKey, new ZiplineAnchorBlock(
				BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(3.0F).noOcclusion().setId(anchorKey)));
		item("zipline_anchor", properties -> new BlockItem(ZIPLINE_ANCHOR, properties.useBlockDescriptionPrefix()));
		ZIPLINE_ANCHOR_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("zipline_anchor"),
				FabricBlockEntityTypeBuilder.create(ZiplineAnchorBlockEntity::new, ZIPLINE_ANCHOR).build());
		ResourceKey<EntityType<?>> riderKey = ResourceKey.create(Registries.ENTITY_TYPE, Jugcraft.id("zipline_rider"));
		ZIPLINE_RIDER = Registry.register(BuiltInRegistries.ENTITY_TYPE, riderKey, EntityType.Builder
				.<ZiplineRider>of(ZiplineRider::new, MobCategory.MISC).sized(0.001F, 0.001F).noSummon()
				.clientTrackingRange(10).updateInterval(1).build(riderKey));
	}

	/** Batch 41: the rocket launcher, its two rockets and the rocket in flight. */
	private static void registerLauncher() {
		ROCKET_LAUNCHER = item("rocket_launcher", properties -> new RocketLauncherItem(properties.stacksTo(1)));
		HE_ROCKET = described("he_rocket");
		HOMING_ROCKET = described("homing_rocket");
		// Batch 43: propellant tanks for liquid rocket motors (filled in the chemical reactor).
		described("kerosene_tank");
		described("lox_tank");
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, Jugcraft.id("combat_rocket"));
		COMBAT_ROCKET = Registry.register(BuiltInRegistries.ENTITY_TYPE, key, EntityType.Builder
				.<CombatRocket>of(CombatRocket::new, MobCategory.MISC).sized(0.3F, 0.3F)
				.clientTrackingRange(8).updateInterval(2).build(key));
	}

	/** Batch 42: the booster rail. */
	private static void registerBoosterRail() {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Jugcraft.id("booster_rail"));
		BOOSTER_RAIL = Registry.register(BuiltInRegistries.BLOCK, key,
				new BoosterRailBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.POWERED_RAIL).setId(key)));
		item("booster_rail", properties -> new BlockItem(BOOSTER_RAIL, properties.useBlockDescriptionPrefix()) {
			@Override
			public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
					Consumer<Component> tooltip, TooltipFlag flag) {
				tooltip.accept(Component.translatable("tooltip.jugcraft.booster_rail").withStyle(ChatFormatting.GRAY));
			}
		});
		BOOSTER_RAIL_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("booster_rail"),
				FabricBlockEntityTypeBuilder.create(BoosterRailBlockEntity::new, BOOSTER_RAIL).build());
	}

	/** A plain item with a grey tooltip line, {@code tooltip.jugcraft.<path>}. */
	private static Item described(String path) {
		return item(path, properties -> new Item(properties.stacksTo(16)) {
			@Override
			public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
					Consumer<Component> tooltip, TooltipFlag flag) {
				tooltip.accept(Component.translatable("tooltip.jugcraft." + path).withStyle(ChatFormatting.GRAY));
			}
		});
	}

	private static Item rocket(String path, RocketItem.Kind kind) {
		return item(path, properties -> new RocketItem(properties.stacksTo(16), kind));
	}

	private static Item item(String path, java.util.function.Function<Item.Properties, Item> factory) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Jugcraft.id(path));
		Item item = Registry.register(BuiltInRegistries.ITEM, key, factory.apply(new Item.Properties().setId(key)));
		ITEMS.add(item);
		return item;
	}

	static String id(Item item) {
		return BuiltInRegistries.ITEM.getKey(item).getPath();
	}

	/** Whether rain can fall in this dimension (it has a sky and no ceiling). */
	static boolean hasWeather(ServerLevel level) {
		return level.dimensionType().hasSkyLight() && !level.dimensionType().hasCeiling();
	}

	/** Ticks until another weather rocket may change the weather (0: now). Shared by every player. */
	static long weatherWait(ServerLevel level) {
		return Math.max(0, nextWeather - level.getGameTime());
	}

	static void book(ServerLevel level, Player player, RocketItem.Kind kind) {
		if (kind.weather()) {
			nextWeather = level.getGameTime() + WEATHER_COOLDOWN;
		}
		FLIGHTS.add(new Flight(level, player.getUUID(), player.getName().getString(), kind,
				player.position().add(0, ARRIVAL_HEIGHT, 0), level.getGameTime() + LAUNCH_DELAY));
	}

	private static void tick(MinecraftServer server) {
		if (FLIGHTS.isEmpty()) {
			return;
		}
		for (Iterator<Flight> it = FLIGHTS.iterator(); it.hasNext(); ) {
			Flight flight = it.next();
			if (flight.level().getGameTime() >= flight.due()) {
				it.remove();
				arrive(flight.level(), flight.level().getServer().getPlayerList().getPlayer(flight.player()), flight.name(),
						flight.kind(), flight.at());
			}
		}
	}

	/** What a rocket of {@code kind} does on reaching {@code at}; {@code player} (who fired it) may have left. */
	public static void arrive(ServerLevel level, @Nullable ServerPlayer player, String name, RocketItem.Kind kind, Vec3 at) {
		BlockPos pos = BlockPos.containing(at);
		switch (kind) {
			case SURVEY -> {
				if (player != null) {
					ServerPlayNetworking.send(player, new SurveyPayload(OreSurvey.survey(level, pos, level.getRandom(), SURVEY_RADIUS, SURVEY_STRIDE)));
				}
			}
			case RAIN -> {
				setWeather(level, true);
				tell(player, Component.translatable("message.jugcraft.rocket.rain"));
			}
			case CLEAR -> {
				setWeather(level, false);
				tell(player, Component.translatable("message.jugcraft.rocket.clear"));
			}
			case SIGNAL -> {
				Component message = Component.translatable("message.jugcraft.rocket.signal", name, pos.getX(), pos.getY() - ARRIVAL_HEIGHT, pos.getZ())
						.withStyle(ChatFormatting.RED);
				for (ServerPlayer other : level.players()) {
					if (other.position().distanceToSqr(at) <= (double) SIGNAL_RANGE * SIGNAL_RANGE) {
						other.sendSystemMessage(message);
					}
				}
			}
			case ILLUMINATION -> {
				AABB area = new AABB(at.subtract(FLARE_RADIUS, FLARE_RADIUS + ARRIVAL_HEIGHT, FLARE_RADIUS), at.add(FLARE_RADIUS, FLARE_RADIUS, FLARE_RADIUS));
				List<LivingEntity> lit = level.getEntitiesOfClass(LivingEntity.class, area, entity -> entity instanceof Enemy && entity.isAlive());
				for (LivingEntity mob : lit) {
					mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, GLOW_TICKS));
				}
				tell(player, Component.translatable("message.jugcraft.rocket.illuminated", lit.size()));
			}
		}
	}

	/** Rain (without thunder) or a clear sky for {@link #WEATHER_TICKS}, as the /weather command sets it. */
	private static void setWeather(ServerLevel level, boolean rain) {
		WeatherData weather = level.getWeatherData();
		weather.setClearWeatherTime(rain ? 0 : WEATHER_TICKS);
		weather.setRainTime(rain ? WEATHER_TICKS : 0);
		weather.setThunderTime(rain ? WEATHER_TICKS : 0);
		weather.setRaining(rain);
		weather.setThundering(false);
		weather.setDirty();
	}

	private static void tell(@Nullable ServerPlayer player, Component message) {
		if (player != null) {
			player.sendOverlayMessage(message);
		}
	}
}
