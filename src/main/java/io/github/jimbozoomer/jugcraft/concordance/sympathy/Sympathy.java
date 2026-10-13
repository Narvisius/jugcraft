package io.github.jimbozoomer.jugcraft.concordance.sympathy;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceData;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceEffects;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import io.github.jimbozoomer.jugcraft.concordance.JugcraftConcordance;
import io.github.jimbozoomer.jugcraft.concordance.Saved;
import io.github.jimbozoomer.jugcraft.concordance.effect.Cause;
import io.github.jimbozoomer.jugcraft.concordance.effect.EffectKind;
import io.github.jimbozoomer.jugcraft.concordance.effect.EffectSpec;
import io.github.jimbozoomer.jugcraft.concordance.effect.Intent;
import io.github.jimbozoomer.jugcraft.concordance.effect.Ledger;
import io.github.jimbozoomer.jugcraft.concordance.effect.Stacking;
import io.github.jimbozoomer.jugcraft.concordance.hex.Curse;
import io.github.jimbozoomer.jugcraft.concordance.hex.CurseDefinition;
import io.github.jimbozoomer.jugcraft.concordance.hex.HexCatalog;
import io.github.jimbozoomer.jugcraft.concordance.hex.Hexes;
import io.github.jimbozoomer.jugcraft.concordance.hex.Link;
import io.github.jimbozoomer.jugcraft.concordance.hex.Ward;
import io.github.jimbozoomer.jugcraft.concordance.hex.WardCategory;
import io.github.jimbozoomer.jugcraft.concordance.rules.Evidence;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Registry;
import net.minecraft.core.UUIDUtil;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * Sympathetic links, curses and wards on the server (roadmap step 22, docs/features/arcane-concordance-hexes.md). A
 * taglock touched to a creature holds a {@link Link} to it; held with a curse's reagent it casts that curse through the
 * link, once the rules ({@link Hexes#cast}) allow: the link alive and strong enough, the target found in the caster's
 * dimension and within range, the multiplayer rules letting the caster harm it, no curse ward, Focus enough. A curse
 * lives on its bearer ({@link #CURSES}) and pulses through the shared effect boundary, revalidated every pulse
 * ({@link Hexes#pulse}): its caster online in the same dimension and in range, its bearer unwarded, the rules still
 * allowing it. A scrying glass investigates curses (their names and remedies, then their caster) and lifts one with its
 * remedy; a ward sigil wards its user against one category of operation for twenty minutes.
 */
public final class Sympathy {
	public static final String RESEARCH = "jugcraft:sympathy";
	/** The practice a cast curse records, with the curse as its detail: three different curses master Sympathy. */
	public static final String ACTIVITY = "jugcraft:sympathy";
	public static final String SOURCE = "jugcraft:curse";
	/** How often curses are checked. */
	public static final int CHECK_TICKS = 20;
	public static final TagKey<Item> SPECIMENS = TagKey.create(Registries.ITEM, Jugcraft.id("sympathy_specimens"));

	private static final Codec<Link> LINK_CODEC = RecordCodecBuilder.create(i -> i.group(
			UUIDUtil.CODEC.fieldOf("linker").forGetter(Link::linker), UUIDUtil.CODEC.fieldOf("target").forGetter(Link::target),
			Codec.STRING.fieldOf("kind").forGetter(Link::kind), Codec.STRING.fieldOf("dimension").forGetter(Link::dimension),
			Codec.LONG.fieldOf("created").forGetter(Link::created), Codec.INT.fieldOf("strength").forGetter(Link::strength))
			.apply(i, Link::new));
	private static final Codec<Curse> CURSE_CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.STRING.fieldOf("curse").forGetter(Curse::curse), UUIDUtil.CODEC.fieldOf("caster").forGetter(Curse::caster),
			Codec.STRING.fieldOf("caster_name").forGetter(Curse::casterName), Codec.LONG.fieldOf("started").forGetter(Curse::started),
			Codec.LONG.fieldOf("until").forGetter(Curse::until), Codec.LONG.fieldOf("last_pulse").forGetter(Curse::lastPulse),
			Codec.INT.fieldOf("insight").forGetter(Curse::insight)).apply(i, Curse::new));
	private static final Codec<Ward> WARD_CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.STRING.xmap(id -> {
				WardCategory category = WardCategory.fromId(id);
				return category == null ? WardCategory.CURSING : category;
			}, category -> category.id).fieldOf("category").forGetter(Ward::category),
			Codec.LONG.fieldOf("until").forGetter(Ward::until)).apply(i, Ward::new));

	/** The link a taglock holds. */
	public static DataComponentType<Link> LINK;
	/** The category a ward sigil wards against. */
	public static DataComponentType<String> WARD;
	/** The curses on a creature (lost on death). */
	public static AttachmentType<List<Curse>> CURSES;
	/** A player's wards (lost on death). */
	public static AttachmentType<List<Ward>> WARDS;
	public static Item TAGLOCK;
	public static Item SCRYING_GLASS;
	public static Item WARD_SIGIL;

	/** Creatures carrying curses, and their level (filled when cast and when a cursed creature loads). */
	private static final Map<UUID, ResourceKey<Level>> CURSED = new HashMap<>();

	private Sympathy() {
	}

	public static void register() {
		LINK = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Jugcraft.id("taglock"),
				DataComponentType.<Link>builder().persistent(LINK_CODEC).networkSynchronized(ByteBufCodecs.fromCodec(LINK_CODEC)).build());
		WARD = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Jugcraft.id("ward"),
				DataComponentType.<String>builder().persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8).build());
		CURSES = AttachmentRegistry.<List<Curse>>builder().persistent(Saved.versioned("curses", CURSE_CODEC.listOf())).buildAndRegister(Jugcraft.id("curses"));
		WARDS = AttachmentRegistry.<List<Ward>>builder().persistent(Saved.versioned("wards", WARD_CODEC.listOf())).buildAndRegister(Jugcraft.id("wards"));
		TAGLOCK = item("taglock", TaglockItem::new, new Item.Properties().stacksTo(1));
		SCRYING_GLASS = item("scrying_glass", ScryingGlassItem::new, new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
		WARD_SIGIL = item("ward_sigil", WardSigilItem::new, new Item.Properties().stacksTo(16));
		ServerTickEvents.END_SERVER_TICK.register(Sympathy::tick);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> CURSED.clear());
		// A moving ward: no one else's Concordance operation pushes its bearer (their own Flashstep still does).
		ConcordanceEffects.guard((context, spec, target) -> spec.kind() == EffectKind.MOVEMENT && context.actor() != target
				&& warded(target, WardCategory.MOVING));
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			List<Curse> curses = entity.getAttached(CURSES);
			if (curses != null && !curses.isEmpty()) {
				CURSED.put(entity.getUUID(), level.dimension());
			}
		});
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> command(dispatcher));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(output -> {
			output.accept(TAGLOCK);
			output.accept(SCRYING_GLASS);
			output.accept(WARD_SIGIL);
		});
	}

	private static Item item(String id, Function<Item.Properties, Item> factory, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Jugcraft.id(id));
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
	}

	public static HexCatalog catalog() {
		return ConcordanceData.rules().hexes();
	}

	public static boolean enabled() {
		return JugcraftConfig.isFeatureEnabled(JugcraftConcordance.FEATURE);
	}

	public static boolean knows(Player player) {
		return ConcordanceProgress.knowledge(player).state(RESEARCH).atLeast(ResearchState.UNDERSTOOD);
	}

	// ---------------------------------------------------------------- wards

	public static List<Ward> wards(Entity entity) {
		List<Ward> wards = entity.getAttached(WARDS);
		return wards == null ? List.of() : wards;
	}

	/** Whether {@code entity} is warded against {@code category} now. Creatures other than players bear no wards. */
	public static boolean warded(Entity entity, WardCategory category) {
		return entity instanceof Player && Ward.guards(wards(entity), category, entity.level().getGameTime());
	}

	public static void ward(Player player, WardCategory category) {
		long now = player.level().getGameTime();
		player.setAttached(WARDS, Ward.with(wards(player), new Ward(category, now + Ward.TICKS), now));
	}

	// ---------------------------------------------------------------- the multiplayer rules

	/**
	 * Whether {@code caster} may act against {@code target} by the configured multiplayer rules: the shared boundary's
	 * friendly-fire rule (the server's PvP setting and parties; never oneself) for players, always for other creatures.
	 */
	public static boolean allowed(@Nullable ServerPlayer caster, LivingEntity target) {
		if (target instanceof Player) {
			return caster != null && ConcordanceEffects.mayHarm(caster, target);
		}
		// Other creatures: only those the caster could strike by hand (claims and protected creatures refuse, step 28).
		return caster == null || caster != target && Authority.mayStrike(caster, target);
	}

	// ---------------------------------------------------------------- curses

	public static List<Curse> curses(Entity entity) {
		List<Curse> curses = entity.getAttached(CURSES);
		return curses == null ? List.of() : curses;
	}

	private static void setCurses(LivingEntity entity, List<Curse> curses) {
		if (curses.isEmpty()) {
			entity.removeAttached(CURSES);
			CURSED.remove(entity.getUUID());
		} else {
			entity.setAttached(CURSES, List.copyOf(curses));
			CURSED.put(entity.getUUID(), entity.level().dimension());
		}
	}

	/** Finds a link's target in {@code level}: the player if online there, or the creature if loaded there. */
	public static @Nullable LivingEntity find(ServerLevel level, Link link) {
		Entity entity = link.player() ? level.getServer().getPlayerList().getPlayer(link.target()) : level.getEntity(link.target());
		return entity instanceof LivingEntity living && living.isAlive() && living.level() == level ? living : null;
	}

	/**
	 * Casts {@code curse} through {@code link} for {@code caster}, revalidating everything now; returns the reason it
	 * cannot ("" when cast). The reagent and Focus are the caller's to take once this succeeds.
	 */
	public static String cast(ServerPlayer caster, ServerLevel level, @Nullable Link link, CurseDefinition curse) {
		if (link == null) {
			return "no_link";
		}
		// Roadmap step 28: a link is its maker's; a taglock handed to someone else carries nothing they may use.
		if (!link.linker().equals(caster.getUUID())) {
			return "not_yours";
		}
		long now = level.getGameTime();
		boolean sameDimension = link.dimension().equals(level.dimension().identifier().toString());
		LivingEntity target = sameDimension ? find(level, link) : null;
		Hexes.Situation situation = new Hexes.Situation(target != null, sameDimension, target == null ? 0.0 : Math.sqrt(caster.distanceToSqr(target)),
				target != null && warded(target, WardCategory.CURSING), target != null && allowed(caster, target), now);
		String reason = Hexes.cast(link, curse, situation, target == null ? List.of() : curses(target), ConcordanceProgress.currentFocus(caster));
		if (!reason.isEmpty()) {
			return reason;
		}
		List<Curse> next = new ArrayList<>(curses(target));
		next.removeIf(held -> !held.active(now));
		next.add(new Curse(curse.id(), caster.getUUID(), caster.getName().getString(), now, now + curse.totalTicks(), now - curse.pulseTicks(), 0));
		setCurses(target, next);
		ConcordanceProgress.record(caster, new Evidence.Practiced(ACTIVITY, curse.id()));
		if (target instanceof ServerPlayer bearer) {
			bearer.sendSystemMessage(Component.translatable("message.jugcraft.concordance.hex.symptom"));
		}
		return "";
	}

	private static void tick(MinecraftServer server) {
		if (server.getTickCount() % CHECK_TICKS != 0 || CURSED.isEmpty() || !enabled()) {
			return;
		}
		for (Map.Entry<UUID, ResourceKey<Level>> entry : new ArrayList<>(CURSED.entrySet())) {
			ServerLevel level = server.getLevel(entry.getValue());
			Entity entity = level == null ? null : level.getEntity(entry.getKey());
			if (!(entity instanceof LivingEntity bearer) || !bearer.isAlive()) {
				// Unloaded or gone: it comes back into the index when it loads again (its curses are saved on it).
				CURSED.remove(entry.getKey());
				continue;
			}
			pulse(bearer, (ServerLevel) bearer.level());
		}
	}

	/** One check of every curse on {@code bearer}: each pulses, skips (and says nothing) or lifts. Tests call this. */
	public static void pulse(LivingEntity bearer, ServerLevel level) {
		long now = level.getGameTime();
		List<Curse> next = new ArrayList<>();
		boolean changed = false;
		for (Curse curse : curses(bearer)) {
			CurseDefinition definition = catalog().curse(curse.curse());
			ServerPlayer caster = level.getServer().getPlayerList().getPlayer(curse.caster());
			boolean together = caster != null && caster.level() == level;
			Hexes.Situation situation = new Hexes.Situation(true, together, together ? Math.sqrt(caster.distanceToSqr(bearer)) : 0.0,
					warded(bearer, WardCategory.CURSING), caster == null || allowed(caster, bearer), now);
			String reason = definition == null ? "ended" : Hexes.pulse(curse, definition, situation);
			if (Hexes.lifts(reason)) {
				changed = true;
				if (bearer instanceof ServerPlayer player && definition != null) {
					player.sendSystemMessage(Component.translatable("message.jugcraft.concordance.hex.lifted", curseName(definition.id())));
				}
				continue;
			}
			if (reason.isEmpty()) {
				give(level, definition, curse, caster, bearer);
				next.add(curse.pulsed(now));
				changed = true;
			} else {
				next.add(curse);
			}
		}
		if (changed) {
			setCurses(bearer, next);
		}
	}

	private static void give(ServerLevel level, CurseDefinition definition, Curse curse, @Nullable ServerPlayer caster, LivingEntity bearer) {
		Cause cause = Cause.of(curse.caster(), Cause.Origin.ITEM, SOURCE, ConcordanceEffects.nextSerial());
		Ledger ledger = new Ledger(new Ledger.Limits(1, EffectKind.STATUS.work, 0));
		EffectSpec spec = new EffectSpec(EffectKind.STATUS, Intent.HARMFUL, definition.amplifier(), definition.pulseDuration(), definition.status(),
				Stacking.STRONGEST, null);
		ConcordanceEffects.apply(new ConcordanceEffects.Context(level, cause, caster, ledger, "curse." + definition.id(), bearer.position()),
				spec, bearer);
	}

	/** Lifts every active curse on {@code bearer} whose remedy is {@code item}; returns how many. */
	public static int remedy(LivingEntity bearer, String item) {
		long now = bearer.level().getGameTime();
		List<Curse> next = new ArrayList<>();
		int lifted = 0;
		for (Curse curse : curses(bearer)) {
			CurseDefinition definition = catalog().curse(curse.curse());
			if (curse.active(now) && definition != null && Hexes.remedies(definition, item)) {
				lifted++;
			} else {
				next.add(curse);
			}
		}
		if (lifted > 0) {
			setCurses(bearer, next);
		}
		return lifted;
	}

	/**
	 * One look at {@code bearer}'s curses through a scrying glass: each active one is understood one step further
	 * (its name and remedy, then its caster unless the caster's scrying ward hides them). Returns the curses as now known.
	 */
	public static List<Curse> investigate(ServerPlayer bearer, ServerLevel level) {
		long now = level.getGameTime();
		List<Curse> next = new ArrayList<>();
		for (Curse curse : curses(bearer)) {
			if (!curse.active(now)) {
				continue;
			}
			ServerPlayer caster = level.getServer().getPlayerList().getPlayer(curse.caster());
			boolean hidden = caster != null && warded(caster, WardCategory.SCRYING);
			next.add(curse.withInsight(Hexes.investigate(curse, hidden)));
		}
		setCurses(bearer, next);
		return next;
	}

	public static Component curseName(String id) {
		return Component.translatable("compose.jugcraft.hex.curse." + id.substring(id.indexOf(':') + 1));
	}

	/** What its bearer knows of a curse, in a line. */
	public static Component describe(Curse curse, long now) {
		long seconds = Math.max(0L, curse.until() - now) / 20;
		if (curse.insight() <= 0) {
			return Component.translatable("message.jugcraft.concordance.hex.unknown_curse", seconds);
		}
		CurseDefinition definition = catalog().curse(curse.curse());
		Component remedy = definition == null ? Component.literal("?")
				: new ItemStack(BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.parse(definition.remedy()))).getHoverName();
		if (curse.insight() < Curse.TRACED) {
			return Component.translatable("message.jugcraft.concordance.hex.named_curse", curseName(curse.curse()), remedy, seconds);
		}
		return Component.translatable("message.jugcraft.concordance.hex.traced_curse", curseName(curse.curse()), remedy, curse.casterName(), seconds);
	}

	/**
	 * The curses on {@code player} (each with its remedy and who cast it) and the wards they hold, one line each, for
	 * the hexes command and the Concordance Journal; returns how many curses are on them.
	 */
	public static int report(ServerPlayer player, Consumer<Component> out) {
		long now = player.level().getGameTime();
		List<Curse> curses = curses(player).stream().filter(curse -> curse.active(now)).toList();
		if (curses.isEmpty()) {
			out.accept(Component.translatable("message.jugcraft.concordance.hex.clean"));
		}
		for (Curse curse : curses) {
			out.accept(describe(curse, now));
		}
		for (Ward ward : wards(player)) {
			if (ward.active(now)) {
				out.accept(Component.translatable("message.jugcraft.concordance.hex.ward_held",
						Component.translatable("compose.jugcraft.hex.ward." + ward.category().id), (ward.until() - now) / 20));
			}
		}
		return curses.size();
	}

	/** Whether anything sympathetic touches {@code player} now: a curse on them or a ward they hold. */
	public static boolean touched(ServerPlayer player) {
		long now = player.level().getGameTime();
		return curses(player).stream().anyMatch(curse -> curse.active(now)) || wards(player).stream().anyMatch(ward -> ward.active(now));
	}

	private static void command(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("jugcraft").then(Commands.literal("concordance").then(Commands.literal("hexes")
				.executes(context -> report(context.getSource().getPlayerOrException(), line -> context.getSource().sendSuccess(() -> line, false))))));
	}
}
