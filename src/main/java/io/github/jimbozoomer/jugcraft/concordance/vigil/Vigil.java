package io.github.jimbozoomer.jugcraft.concordance.vigil;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceData;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import io.github.jimbozoomer.jugcraft.concordance.JugcraftConcordance;
import io.github.jimbozoomer.jugcraft.concordance.Saved;
import io.github.jimbozoomer.jugcraft.concordance.crimson.CrimsonCatalog;
import io.github.jimbozoomer.jugcraft.concordance.crimson.Exhaustion;
import io.github.jimbozoomer.jugcraft.concordance.crimson.Growth;
import io.github.jimbozoomer.jugcraft.concordance.crimson.OfferingState;
import io.github.jimbozoomer.jugcraft.concordance.crimson.Offerings;
import io.github.jimbozoomer.jugcraft.concordance.crimson.Rite;
import io.github.jimbozoomer.jugcraft.concordance.resource.ResourceKind;
import io.github.jimbozoomer.jugcraft.concordance.resource.ResourceType;
import io.github.jimbozoomer.jugcraft.concordance.rules.Evidence;
import io.github.jimbozoomer.jugcraft.concordance.rules.FocusPool;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import io.netty.buffer.ByteBuf;
import java.util.function.Function;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ToolMaterial;

/**
 * The Crimson Vigil on the server (roadmap step 16, docs/features/arcane-concordance-vitae.md). Three things are kept
 * apart, as the brief asks: <b>health</b> (vanilla's, healed by anything), <b>Vitae</b> (what an offering turns health
 * into, held in a Crimson Chalice) and <b>offering exhaustion</b> (what offering leaves behind, cleared only by time;
 * {@link Exhaustion}). An offering is the only way health becomes Vitae ({@link Offerings}); no food, potion, spell or
 * regeneration touches exhaustion, so healing never resets the loop. Vitae buys two things: a Crimson Surge (Focus in
 * an emergency, once a minute) and vigor for living equipment, the Thornheart Blade, which develops through varied deeds
 * ({@link Growth}).
 */
public final class Vigil {
	public static final String RESEARCH = "jugcraft:crimson_rites";
	/** The rite the Crimson Chalice performs (data: concordance/offering). */
	public static final String RITE = "jugcraft:offering";
	/** The practice a living blade records as it reaches each stage; distinct stages count for mastery. */
	public static final String ACTIVITY = "jugcraft:living_growth";
	public static final ResourceType VITAE = ResourceType.of(ResourceKind.VITAE);
	public static final int CHALICE_CAPACITY = 32;
	/** A Crimson Surge: this much Vitae for this much Focus, at most once per {@value #SURGE_COOLDOWN} ticks. */
	public static final int SURGE_VITAE = 6;
	public static final int SURGE_FOCUS = 6;
	public static final int SURGE_COOLDOWN = 1200;
	/** Vitae one feeding gives a living blade. */
	public static final int NOURISH_VITAE = 2;
	/** Vigor a blow on a living creature costs a blade whose powers are awake. */
	public static final int BLOW_VIGOR = 1;
	/** Attack damage each stage adds while the blade has vigor. */
	public static final double STAGE_DAMAGE = 1.0;

	public static Item CRIMSON_CHALICE;
	public static Item THORNHEART_BLADE;
	/** The Vitae a chalice holds. */
	public static DataComponentType<Integer> VITAE_HELD;
	/** A living blade's development and vigor. */
	public static DataComponentType<Growth> GROWTH;
	/** A player's offering state (synced to them alone, kept through death). */
	public static AttachmentType<OfferingState> OFFERING;
	public static final TagKey<Item> SPECIMENS = TagKey.create(Registries.ITEM, Jugcraft.id("crimson_specimens"));

	private static final Codec<Exhaustion> EXHAUSTION_CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.INT.fieldOf("points").forGetter(Exhaustion::points), Codec.LONG.fieldOf("stamp").forGetter(Exhaustion::stamp))
			.apply(i, Exhaustion::new));
	public static final Codec<OfferingState> OFFERING_CODEC = RecordCodecBuilder.create(i -> i.group(
			EXHAUSTION_CODEC.fieldOf("exhaustion").forGetter(OfferingState::exhaustion),
			Codec.LONG.fieldOf("last_offering").forGetter(OfferingState::lastOffering),
			Codec.LONG.fieldOf("last_surge").forGetter(OfferingState::lastSurge)).apply(i, OfferingState::new));
	public static final StreamCodec<ByteBuf, OfferingState> OFFERING_STREAM = ByteBufCodecs.fromCodec(OFFERING_CODEC);
	public static final Codec<Growth> GROWTH_CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.unboundedMap(Codec.STRING, Codec.INT).fieldOf("points").forGetter(Growth::points),
			Codec.LONG.fieldOf("window").forGetter(Growth::window),
			Codec.unboundedMap(Codec.STRING, Codec.INT).fieldOf("today").forGetter(Growth::today),
			Codec.INT.fieldOf("vigor").forGetter(Growth::vigor)).apply(i, Growth::new));

	private Vigil() {
	}

	public static void register() {
		VITAE_HELD = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Jugcraft.id("vitae"),
				DataComponentType.<Integer>builder().persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT).build());
		GROWTH = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Jugcraft.id("growth"),
				DataComponentType.<Growth>builder().persistent(GROWTH_CODEC).networkSynchronized(ByteBufCodecs.fromCodec(GROWTH_CODEC)).build());
		OFFERING = AttachmentRegistry.<OfferingState>builder().persistent(Saved.versioned("offering_state", OFFERING_CODEC)).copyOnDeath()
				.syncWith(OFFERING_STREAM, AttachmentSyncPredicate.targetOnly()).buildAndRegister(Jugcraft.id("offering_state"));
		CRIMSON_CHALICE = item("crimson_chalice", CrimsonChaliceItem::new, new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
		THORNHEART_BLADE = item("thornheart_blade", ThornheartBladeItem::new,
				new Item.Properties().sword(ToolMaterial.IRON, 3.0F, -2.4F).rarity(Rarity.UNCOMMON));
		PayloadTypeRegistry.clientboundPlay().register(VigilGesturePayload.TYPE, VigilGesturePayload.CODEC);
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (enabled() && entity instanceof Enemy && source.getEntity() instanceof ServerPlayer killer && killer.getMainHandItem().is(THORNHEART_BLADE)) {
				deed(killer, killer.getMainHandItem(), Growth.Deed.SLAY, BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString());
			}
		});
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damage, blocked) -> {
			if (enabled() && damage > 0.0F && entity instanceof ServerPlayer player && player.isAlive() && player.getMainHandItem().is(THORNHEART_BLADE)) {
				String harm = source.typeHolder().unwrapKey().map(key -> key.identifier().toString()).orElse("unknown");
				deed(player, player.getMainHandItem(), Growth.Deed.ENDURE, harm);
			}
		});
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> command(dispatcher));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(output -> output.accept(CRIMSON_CHALICE));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(output -> output.accept(THORNHEART_BLADE));
	}

	private static Item item(String id, Function<Item.Properties, Item> factory, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Jugcraft.id(id));
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
	}

	// ---------------------------------------------------------------- the rules

	public static CrimsonCatalog catalog() {
		return ConcordanceData.rules().crimson();
	}

	public static boolean enabled() {
		return JugcraftConfig.isFeatureEnabled(JugcraftConcordance.FEATURE);
	}

	public static ResearchState state(Player player) {
		return ConcordanceProgress.knowledge(player).state(RESEARCH);
	}

	public static boolean knows(Player player) {
		return state(player).atLeast(ResearchState.UNDERSTOOD);
	}

	public static OfferingState offering(Player player) {
		return player.getAttachedOrElse(OFFERING, OfferingState.NONE);
	}

	// ---------------------------------------------------------------- offerings

	/**
	 * Makes an offering for {@code player} into {@code chalice} at game time {@code now} (tests pass their own): the
	 * health is taken only when the offering succeeds, never below the rite's floor and never through armour or death.
	 */
	public static Offerings.Attempt offer(ServerPlayer player, ItemStack chalice, long now) {
		Rite rite = catalog().rite(RITE);
		if (rite == null || !enabled()) {
			return new Offerings.Attempt(Offerings.Outcome.EXHAUSTED, 0, 0, offering(player).exhaustion());
		}
		OfferingState state = offering(player);
		int held = CrimsonChaliceItem.vitae(chalice);
		Offerings.Attempt attempt = Offerings.offer(rite, player.getHealth(), state.exhaustion(), state.lastOffering(), now, CHALICE_CAPACITY - held);
		if (attempt.outcome() == Offerings.Outcome.OFFERED) {
			player.setHealth(player.getHealth() - attempt.health());
			CrimsonChaliceItem.setVitae(chalice, held + attempt.vitae());
			player.setAttached(OFFERING, state.withOffering(attempt.exhaustion(), now));
			player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_HURT_SWEET_BERRY_BUSH, SoundSource.PLAYERS, 0.8F, 0.8F);
			gesture(player);
		}
		return attempt;
	}

	/** A Crimson Surge: {@value #SURGE_VITAE} Vitae for {@value #SURGE_FOCUS} Focus, once per {@value #SURGE_COOLDOWN} ticks. */
	public static String surge(ServerPlayer player, ItemStack chalice, long now) {
		if (!enabled()) {
			return "disabled";
		}
		OfferingState state = offering(player);
		if (now - state.lastSurge() < SURGE_COOLDOWN) {
			return "surge_too_soon";
		}
		int held = CrimsonChaliceItem.vitae(chalice);
		if (held < SURGE_VITAE) {
			return "surge_too_little";
		}
		FocusPool focus = ConcordanceProgress.focus(player);
		if (focus.current(now) >= FocusPool.MAX) {
			return "surge_full";
		}
		CrimsonChaliceItem.setVitae(chalice, held - SURGE_VITAE);
		player.setAttached(JugcraftConcordance.FOCUS, focus.gain(now, SURGE_FOCUS));
		player.setAttached(OFFERING, state.withSurge(now));
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BREWING_STAND_BREW, SoundSource.PLAYERS, 0.6F, 0.6F);
		return "surged";
	}

	/** Tells every client that sees {@code player}, and theirs, to play the offering gesture. */
	private static void gesture(ServerPlayer player) {
		VigilGesturePayload payload = new VigilGesturePayload(player.getId());
		for (ServerPlayer watcher : PlayerLookup.tracking(player)) {
			if (ServerPlayNetworking.canSend(watcher, VigilGesturePayload.TYPE)) {
				ServerPlayNetworking.send(watcher, payload);
			}
		}
		if (ServerPlayNetworking.canSend(player, VigilGesturePayload.TYPE)) {
			ServerPlayNetworking.send(player, payload);
		}
	}

	// ---------------------------------------------------------------- living equipment

	public static Growth growth(ItemStack blade) {
		Growth growth = blade.get(GROWTH);
		return growth == null ? Growth.NEW : growth;
	}

	/**
	 * Records a deed for the blade {@code player} wields: the growth after it is stored on the blade, its powers refreshed,
	 * and each stage newly reached is practice for Crimson Rites. Returns the points it paid.
	 */
	public static int deed(ServerPlayer player, ItemStack blade, Growth.Deed deed, String subject) {
		return deed(player, blade, deed, subject, player.level().getGameTime());
	}

	public static int deed(ServerPlayer player, ItemStack blade, Growth.Deed deed, String subject, long gameTime) {
		Growth before = growth(blade);
		Growth.Result result = before.record(deed, subject, gameTime);
		store(player, blade, before, result.growth());
		return result.gained();
	}

	/** Feeds the blade from the chalice; returns the Vitae it took (0 when it was not hungry or the chalice too low). */
	public static int nourish(ServerPlayer player, ItemStack blade, ItemStack chalice, long gameTime) {
		Growth before = growth(blade);
		int held = CrimsonChaliceItem.vitae(chalice);
		if (held < NOURISH_VITAE || before.hunger() < NOURISH_VITAE) {
			return 0;
		}
		CrimsonChaliceItem.setVitae(chalice, held - NOURISH_VITAE);
		store(player, blade, before, before.nourish(NOURISH_VITAE, gameTime).growth());
		return NOURISH_VITAE;
	}

	static void store(ServerPlayer player, ItemStack blade, Growth before, Growth after) {
		blade.set(GROWTH, after);
		ThornheartBladeItem.refresh(blade);
		for (int stage = before.stage() + 1; stage <= after.stage(); stage++) {
			ConcordanceProgress.record(player, new Evidence.Practiced(ACTIVITY, "stage_" + stage));
			player.sendSystemMessage(Component.translatable("message.jugcraft.concordance.vigil.grew",
					Component.translatable("message.jugcraft.concordance.vigil.stage." + stage)));
		}
	}

	// ---------------------------------------------------------------- the status command

	/** Health, Vitae, exhaustion and recovery, each said apart (never one shared bar). */
	public static void status(ServerPlayer player, java.util.function.Consumer<Component> out) {
		long now = player.level().getGameTime();
		OfferingState state = offering(player);
		Exhaustion exhaustion = state.exhaustion();
		out.accept(Component.translatable("message.jugcraft.concordance.vigil.health", Math.round(player.getHealth()), Math.round(player.getMaxHealth())));
		ItemStack chalice = player.getMainHandItem().is(CRIMSON_CHALICE) ? player.getMainHandItem() : player.getOffhandItem();
		if (chalice.is(CRIMSON_CHALICE)) {
			out.accept(Component.translatable("message.jugcraft.concordance.vigil.vitae", CrimsonChaliceItem.vitae(chalice), CHALICE_CAPACITY));
		}
		out.accept(Component.translatable("message.jugcraft.concordance.vigil.exhaustion", exhaustion.current(now), Exhaustion.MAX,
				Offerings.efficiency(exhaustion.current(now)), (exhaustion.ticksToClear(now) + 19) / 20));
		ItemStack blade = player.getMainHandItem().is(THORNHEART_BLADE) ? player.getMainHandItem() : player.getOffhandItem();
		if (blade.is(THORNHEART_BLADE)) {
			Growth growth = growth(blade);
			out.accept(Component.translatable("message.jugcraft.concordance.vigil.blade",
					Component.translatable("message.jugcraft.concordance.vigil.stage." + growth.stage()), growth.points(Growth.Deed.SLAY),
					growth.points(Growth.Deed.ENDURE), growth.points(Growth.Deed.NOURISH), growth.vigor(), Growth.MAX_VIGOR));
		}
	}

	private static void command(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("jugcraft").then(Commands.literal("concordance").then(Commands.literal("vitae")
				.executes(context -> {
					ServerPlayer player = context.getSource().getPlayerOrException();
					status(player, line -> context.getSource().sendSuccess(() -> line, false));
					return 1;
				}))));
	}
}
