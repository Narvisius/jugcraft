package io.github.jimbozoomer.jugcraft.concordance.reliquary;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.trinkets.api.TrinketsApi;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceData;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceEffects;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import io.github.jimbozoomer.jugcraft.concordance.JugcraftConcordance;
import io.github.jimbozoomer.jugcraft.concordance.LeyPylonBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.effect.Cause;
import io.github.jimbozoomer.jugcraft.concordance.effect.EffectKind;
import io.github.jimbozoomer.jugcraft.concordance.effect.EffectSpec;
import io.github.jimbozoomer.jugcraft.concordance.effect.Intent;
import io.github.jimbozoomer.jugcraft.concordance.effect.Ledger;
import io.github.jimbozoomer.jugcraft.concordance.effect.Stacking;
import io.github.jimbozoomer.jugcraft.concordance.relic.Context;
import io.github.jimbozoomer.jugcraft.concordance.relic.Mode;
import io.github.jimbozoomer.jugcraft.concordance.relic.RelicCatalog;
import io.github.jimbozoomer.jugcraft.concordance.relic.RelicDefinition;
import io.github.jimbozoomer.jugcraft.concordance.relic.RelicState;
import io.github.jimbozoomer.jugcraft.concordance.relic.Relics;
import io.github.jimbozoomer.jugcraft.concordance.rules.Evidence;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import io.github.jimbozoomer.jugcraft.concordance.sky.SkyItem;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import io.github.jimbozoomer.jugcraft.party.JugcraftParties;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Relics on the server (roadmap step 20, docs/features/arcane-concordance-relics.md). Every {@value #CHECK_TICKS} ticks
 * each online player's relics are looked for in their explicit contexts, in this order: the main hand, the off hand, the
 * four armour slots, then the Trinkets slots (a cosmetic slot is "worn for show" and never works). Relics carried loose
 * in the inventory are found only to say why they do nothing; a chest, a shulker box or a bundle is never looked into.
 * <p>
 * A relic found in a context one of its modes names is checked by {@link Relics#check}. One that may pulse gives its
 * mode's status to its targets through the shared effect boundary and only then spends its charge and records when,
 * both saved on the relic itself ({@link #RELIC}); a pulse that would change nothing (no one in range, or a stronger
 * effect already holds) spends nothing. One player's relics share a budget ({@link Relics#BUDGET} pulses a second);
 * installed relics are worked by their {@link ReliquaryShrineBlockEntity} under their owner's own budget.
 */
public final class Reliquary {
	public static final String RESEARCH = "jugcraft:relic_lore";
	/** The practice a pulse records, with its context as the detail: three different contexts master Relic Lore. */
	public static final String ACTIVITY = "jugcraft:relic_pulse";
	public static final String SOURCE = "jugcraft:relic";
	/** How often each player's relics (and each shrine) are checked. */
	public static final int CHECK_TICKS = 20;
	/** The charge one Ley Charge gives a relic. */
	public static final int CHARGE_PER_LEY = 4;
	/** How fast a shrine charges the relic installed in it from its pylons, each second. */
	public static final int RECHARGE_PER_SECOND = 8;
	/** How far from a shrine (in each direction) a Ley Pylon may stand to serve it. */
	public static final int PYLON_REACH = 2;
	/** The most creatures one pulse reaches, nearest first. */
	public static final int MAX_TARGETS = 12;
	/** The relic items, in a fixed order (their ids are stable; what each does is data). */
	public static final List<String> RELIC_IDS = List.of("wardlight", "hearthstone", "stormglass", "owlsight_circlet");
	/** What a player examines to begin Relic Lore. */
	public static final TagKey<Item> SPECIMENS = TagKey.create(Registries.ITEM, Jugcraft.id("relic_specimens"));

	public static final Codec<RelicState> STATE_CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.INT.fieldOf("charge").forGetter(RelicState::charge),
			UUIDUtil.CODEC.optionalFieldOf("owner").forGetter(state -> Optional.ofNullable(state.owner())),
			Codec.LONG.optionalFieldOf("last_pulse", RelicState.FRESH.lastPulse()).forGetter(RelicState::lastPulse))
			.apply(i, (charge, owner, lastPulse) -> new RelicState(Math.max(0, charge), owner.orElse(null), lastPulse)));

	/** A relic's own state: its charge, the player it is bound to and its last pulse. */
	public static DataComponentType<RelicState> RELIC;
	public static Block SHRINE;
	public static BlockEntityType<ReliquaryShrineBlockEntity> SHRINE_ENTITY;
	public static final Map<String, Item> ITEMS = new LinkedHashMap<>();

	/** One player's carried and worn relics together. */
	private static final Relics.Budget CARRIED = new Relics.Budget(Relics.BUDGET);
	/** One owner's installed relics together, however many shrines. */
	static final Relics.Budget INSTALLED = new Relics.Budget(Relics.SHRINE_BUDGET);
	/** When each player was last hurt, in game time. Not saved: after a restart everyone is calm. */
	private static final Map<UUID, Long> HURT = new HashMap<>();

	private Reliquary() {
	}

	public static void register() {
		RELIC = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Jugcraft.id("relic"),
				DataComponentType.<RelicState>builder().persistent(STATE_CODEC).networkSynchronized(ByteBufCodecs.fromCodec(STATE_CODEC)).build());
		ResourceKey<Block> shrineKey = ResourceKey.create(Registries.BLOCK, Jugcraft.id("reliquary_shrine"));
		SHRINE = Registry.register(BuiltInRegistries.BLOCK, shrineKey, new ReliquaryShrineBlock(BlockBehaviour.Properties.of()
				.mapColor(MapColor.COLOR_BLACK).strength(3.0F, 9.0F).sound(SoundType.STONE).requiresCorrectToolForDrops().noOcclusion()
				.lightLevel(state -> state.getValue(ReliquaryShrineBlock.LIT) ? 7 : 0).pushReaction(PushReaction.IMMOVEABLE).setId(shrineKey)));
		ResourceKey<Item> shrineItemKey = ResourceKey.create(Registries.ITEM, Jugcraft.id("reliquary_shrine"));
		Item shrine = Registry.register(BuiltInRegistries.ITEM, shrineItemKey,
				new SkyItem(SHRINE, new Item.Properties().useBlockDescriptionPrefix().setId(shrineItemKey)));
		SHRINE_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("reliquary_shrine"),
				FabricBlockEntityTypeBuilder.create(ReliquaryShrineBlockEntity::new, SHRINE).build());
		for (String id : RELIC_IDS) {
			ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Jugcraft.id(id));
			Item.Properties properties = new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON).setId(key);
			if (id.equals("owlsight_circlet")) {
				// A circlet is worn on the head like a helmet (and drawn there), so its head context is the vanilla slot.
				properties.component(DataComponents.EQUIPPABLE, Equippable.builder(EquipmentSlot.HEAD).setEquipSound(SoundEvents.ARMOR_EQUIP_GOLD)
						.setAsset(ResourceKey.create(EquipmentAssets.ROOT_ID, Jugcraft.id(id))).build());
			}
			ITEMS.put(id, Registry.register(BuiltInRegistries.ITEM, key, new RelicItem(properties)));
		}
		ServerTickEvents.END_SERVER_TICK.register(Reliquary::tick);
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damage, blocked) -> {
			if (damage > 0.0F && entity instanceof ServerPlayer player) {
				hurt(player.getUUID(), player.level().getGameTime());
			}
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			HURT.remove(handler.getPlayer().getUUID());
			CARRIED.forget(handler.getPlayer().getUUID());
		});
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> command(dispatcher));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(output -> output.accept(shrine));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(output -> ITEMS.values().forEach(output::accept));
	}

	public static RelicCatalog catalog() {
		return ConcordanceData.rules().relics();
	}

	public static boolean enabled() {
		return JugcraftConfig.isFeatureEnabled(JugcraftConcordance.FEATURE);
	}

	public static boolean knows(Player player) {
		return ConcordanceProgress.knowledge(player).state(RESEARCH).atLeast(ResearchState.UNDERSTOOD);
	}

	public static @Nullable RelicDefinition definition(ItemStack stack) {
		return stack.isEmpty() || !(stack.getItem() instanceof RelicItem) ? null
				: catalog().byItem(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
	}

	public static RelicState state(ItemStack stack) {
		return stack.getOrDefault(RELIC, RelicState.FRESH);
	}

	/** Operators and tests: what a player was last hurt at. */
	public static void hurt(UUID player, long gameTime) {
		HURT.put(player, gameTime);
	}

	// ---------------------------------------------------------------- where a player's relics are

	/** A relic found on a player: its context, the slot it is in (for messages) and its stack (the real one, not a copy). */
	public record Found(Context context, String slot, ItemStack stack) {
	}

	/**
	 * Every relic on {@code player}, in the order they are worked: the hands, the armour, the Trinkets slots (worn or
	 * worn for show), then loose in the inventory. Nothing inside a container item is ever found.
	 */
	public static List<Found> find(ServerPlayer player) {
		List<Found> found = new ArrayList<>();
		Set<ItemStack> seen = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
		add(found, seen, Context.MAIN_HAND, "main_hand", player.getMainHandItem());
		add(found, seen, Context.OFF_HAND, "off_hand", player.getOffhandItem());
		add(found, seen, Context.HEAD, "head", player.getItemBySlot(EquipmentSlot.HEAD));
		add(found, seen, Context.CHEST, "chest", player.getItemBySlot(EquipmentSlot.CHEST));
		add(found, seen, Context.LEGS, "legs", player.getItemBySlot(EquipmentSlot.LEGS));
		add(found, seen, Context.FEET, "feet", player.getItemBySlot(EquipmentSlot.FEET));
		TrinketsApi.getAttachment(player).forEach((access, stack) -> add(found, seen,
				access.cosmetic() || !access.canApplyEffects() ? Context.COSMETIC : Context.TRINKET, access.slotType().getId(), stack));
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			add(found, seen, Context.INVENTORY, "inventory", player.getInventory().getItem(slot));
		}
		return found;
	}

	private static void add(List<Found> found, Set<ItemStack> seen, Context context, String slot, ItemStack stack) {
		if (!stack.isEmpty() && stack.getItem() instanceof RelicItem && seen.add(stack)) {
			found.add(new Found(context, slot, stack));
		}
	}

	// ---------------------------------------------------------------- pulses

	/**
	 * What one relic did, or why it did nothing: {@code reason} is empty when it pulsed, else one of {@link Relics#check}'s
	 * reasons, {@code "budget"} (its holder's other relics used this second's pulses), {@code "no_target"} (nothing in
	 * range it serves), {@code "kept"} (a stronger effect already holds everywhere it reaches) or {@code "unknown"} (no
	 * definition: the data has none).
	 */
	public record Outcome(@Nullable RelicDefinition relic, Context context, @Nullable Mode mode, String reason, int reached) {
		public boolean pulsed() {
			return reason.isEmpty();
		}
	}

	private static void tick(MinecraftServer server) {
		if (!enabled()) {
			return;
		}
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if ((server.getTickCount() + player.getId()) % CHECK_TICKS == 0 && player.isAlive() && !player.isSpectator()) {
				check(player, true);
			}
		}
	}

	/** One check of all of {@code player}'s relics; with {@code act} false nothing pulses (the command's report). */
	public static List<Outcome> check(ServerPlayer player, boolean act) {
		List<Outcome> outcomes = new ArrayList<>();
		for (Found found : find(player)) {
			outcomes.add(work(player, found, act));
		}
		return outcomes;
	}

	/** Works one relic found on {@code player} (tests call this with the stack they placed). */
	public static Outcome work(ServerPlayer player, Found found, boolean act) {
		ServerLevel level = (ServerLevel) player.level();
		RelicDefinition relic = definition(found.stack());
		if (relic == null) {
			return new Outcome(null, found.context(), null, "unknown", 0);
		}
		RelicState state = state(found.stack());
		long now = level.getGameTime();
		BlockPos head = BlockPos.containing(player.getEyePosition());
		Relics.Situation situation = new Relics.Situation(player.getUUID(), level.dimension().identifier().toString(), openSky(level, head),
				night(level), HURT.getOrDefault(player.getUUID(), RelicState.FRESH.lastPulse()), player.getHealth() < player.getMaxHealth(), now);
		Relics.Verdict verdict = Relics.check(relic, found.context(), state, situation);
		if (!verdict.allowed()) {
			return new Outcome(relic, found.context(), verdict.mode(), verdict.reason(), 0);
		}
		Mode mode = verdict.mode();
		if (!act) {
			return new Outcome(relic, found.context(), mode, CARRIED.allows(player.getUUID(), now) ? "" : "budget", 0);
		}
		if (!CARRIED.allows(player.getUUID(), now)) {
			return new Outcome(relic, found.context(), mode, "budget", 0);
		}
		List<LivingEntity> targets = targets(level, mode, player.position(), player.getUUID(), player);
		if (targets.isEmpty()) {
			return new Outcome(relic, found.context(), mode, "no_target", 0);
		}
		int reached = give(level, mode, targets, Cause.Origin.ITEM, player.getUUID(), player, player.position());
		if (reached == 0) {
			return new Outcome(relic, found.context(), mode, "kept", 0);
		}
		CARRIED.take(player.getUUID(), now);
		RelicState next = state.spent(mode.cost(), now);
		if (relic.owned() && next.owner() == null) {
			next = next.bound(player.getUUID());
		}
		found.stack().set(RELIC, next);
		ConcordanceProgress.record(player, new Evidence.Practiced(ACTIVITY, found.context().id));
		return new Outcome(relic, found.context(), mode, "", reached);
	}

	/**
	 * Who a pulse of {@code mode} at {@code center} serves: its holder (self); its holder's party within range, the
	 * holder included (allies); or hostile creatures within range (hostiles). Only hurt creatures, if the mode mends.
	 * At most {@value #MAX_TARGETS}, nearest first. {@code holder} is null for a shrine (it has no self).
	 */
	static List<LivingEntity> targets(ServerLevel level, Mode mode, Vec3 center, UUID user, @Nullable ServerPlayer holder) {
		List<LivingEntity> targets = new ArrayList<>();
		boolean wounded = mode.conditions().wounded();
		switch (mode.target()) {
			case SELF -> {
				if (holder != null && (!wounded || holder.getHealth() < holder.getMaxHealth())) {
					targets.add(holder);
				}
			}
			case ALLIES -> {
				double range = mode.range();
				for (ServerPlayer player : level.players()) {
					if (player.isAlive() && !player.isSpectator() && player.position().distanceToSqr(center) <= range * range
							&& (player.getUUID().equals(user) || JugcraftParties.sameParty(user, player.getUUID()))
							&& (!wounded || player.getHealth() < player.getMaxHealth())) {
						targets.add(player);
					}
				}
			}
			case HOSTILES -> {
				double range = mode.range();
				for (LivingEntity creature : level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(range),
						creature -> creature instanceof Enemy && creature.isAlive() && creature.position().distanceToSqr(center) <= range * range)) {
					if (!wounded || creature.getHealth() < creature.getMaxHealth()) {
						targets.add(creature);
					}
				}
			}
		}
		targets.sort(Comparator.comparingDouble(creature -> creature.position().distanceToSqr(center)));
		return targets.size() > MAX_TARGETS ? new ArrayList<>(targets.subList(0, MAX_TARGETS)) : targets;
	}

	/**
	 * Gives {@code mode}'s status to each target through the shared effect boundary (one event, one ledger); returns how
	 * many it changed. Helpful to allies and self; to hostile creatures it is harmful, so their tolerance applies.
	 */
	static int give(ServerLevel level, Mode mode, List<LivingEntity> targets, Cause.Origin origin, UUID user, @Nullable ServerPlayer actor,
			Vec3 origin3) {
		Intent intent = mode.target() == Mode.Target.HOSTILES ? Intent.HARMFUL : Intent.HELPFUL;
		EffectSpec spec = new EffectSpec(EffectKind.STATUS, intent, mode.amplifier(), mode.duration(), mode.status(), Stacking.STRONGEST, null);
		Cause cause = Cause.of(user, origin, SOURCE, ConcordanceEffects.nextSerial());
		Ledger ledger = new Ledger(new Ledger.Limits(MAX_TARGETS, MAX_TARGETS * EffectKind.STATUS.work, 0));
		ConcordanceEffects.Context context = new ConcordanceEffects.Context(level, cause, actor, ledger, "relic." + mode.id(), origin3);
		int reached = 0;
		for (LivingEntity target : targets) {
			if (ConcordanceEffects.apply(context, spec, target).applied()) {
				reached++;
			}
		}
		return reached;
	}

	/**
	 * Whether nothing that stops movement stands above {@code pos}, glass and leaves included (the server's heightmap,
	 * which changes the moment a block is placed; the Nether's roof is above everything there).
	 */
	public static boolean openSky(ServerLevel level, BlockPos pos) {
		return level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ()) <= pos.getY() + 1;
	}

	/** Whether it is dark outside: night, or a dimension with no day. */
	public static boolean night(ServerLevel level) {
		return !level.isBrightOutside();
	}

	// ---------------------------------------------------------------- charging from Ley Pylons

	/**
	 * The Ley Pylons within {@value #PYLON_REACH} blocks of {@code pos} that lend to {@code user}'s party (or to anyone,
	 * if unowned), loaded ones only.
	 */
	static List<LeyPylonBlockEntity> pylons(ServerLevel level, BlockPos pos, UUID user) {
		Set<UUID> party = new HashSet<>(JugcraftParties.partyMembers(user));
		party.add(user);
		List<LeyPylonBlockEntity> pylons = new ArrayList<>();
		for (BlockPos at : BlockPos.betweenClosed(pos.offset(-PYLON_REACH, -PYLON_REACH, -PYLON_REACH), pos.offset(PYLON_REACH, PYLON_REACH, PYLON_REACH))) {
			if (level.isLoaded(at) && level.getBlockEntity(at) instanceof LeyPylonBlockEntity pylon && pylon.lends(party) && pylon.ley() > 0) {
				pylons.add(pylon);
			}
		}
		return pylons;
	}

	/**
	 * Draws up to {@code ley} Ley Charge from {@code pylons}, first to last, each drawing what it has; returns how much
	 * it drew.
	 */
	static long draw(ServerLevel level, List<LeyPylonBlockEntity> pylons, long ley) {
		long drawn = 0;
		for (LeyPylonBlockEntity pylon : pylons) {
			long take = Math.min(ley - drawn, pylon.ley());
			if (take > 0 && pylon.draw(level, take)) {
				drawn += take;
			}
			if (drawn >= ley) {
				break;
			}
		}
		return drawn;
	}

	/**
	 * Recharges a held relic at a shrine from the pylons beside it, for {@code player}'s party: whole Ley Charges, each
	 * {@value #CHARGE_PER_LEY} charge, until it is full (the last may fill it only in part). Returns the charge added.
	 */
	public static int recharge(ServerPlayer player, ServerLevel level, BlockPos shrine, ItemStack stack, RelicDefinition relic) {
		RelicState state = state(stack);
		int room = relic.capacity() - state.charge();
		if (room <= 0) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.relic.full", stack.getHoverName()));
			return 0;
		}
		List<LeyPylonBlockEntity> pylons = pylons(level, shrine, player.getUUID());
		long drawn = draw(level, pylons, (room + CHARGE_PER_LEY - 1) / CHARGE_PER_LEY);
		if (drawn == 0) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.relic.no_pylon"));
			return 0;
		}
		RelicState next = state.charged((int) drawn * CHARGE_PER_LEY, relic.capacity());
		stack.set(RELIC, next);
		player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.relic.recharged", stack.getHoverName(), next.charge(),
				relic.capacity()));
		return next.charge() - state.charge();
	}

	// ---------------------------------------------------------------- describing

	/** Where it works, as words: "held in the off hand or installed in a Reliquary Shrine". */
	public static Component contexts(List<Context> contexts) {
		Component words = Component.empty();
		for (int i = 0; i < contexts.size(); i++) {
			if (i > 0) {
				words = Component.translatable("compose.jugcraft.relic.or", words, context(contexts.get(i)));
			} else {
				words = context(contexts.get(i));
			}
		}
		return words;
	}

	public static Component context(Context context) {
		return Component.translatable("compose.jugcraft.relic.context." + context.id);
	}

	/** Why it does nothing, in words (the supported contexts fill the wrong-context reasons). */
	public static Component reason(Outcome outcome) {
		String reason = outcome.reason();
		if (outcome.relic() != null && (reason.equals("wrong_context") || reason.equals("cannot_install"))) {
			return Component.translatable("compose.jugcraft.relic.reason." + reason, contexts(outcome.relic().contexts()));
		}
		return Component.translatable("compose.jugcraft.relic.reason." + reason);
	}

	/** One line for a relic: its name and where it is, then working (mode, charge) or why not. */
	public static Component line(ItemStack stack, Outcome outcome) {
		Component where = context(outcome.context());
		if (outcome.pulsed() || outcome.reason().equals("resting")) {
			RelicDefinition relic = outcome.relic();
			return Component.translatable("message.jugcraft.concordance.relic.status", stack.getHoverName(), where,
					Component.translatable("message.jugcraft.concordance.relic.working",
							Component.translatable("compose.jugcraft.relic.mode." + outcome.mode().id()), state(stack).charge(),
							relic == null ? 0 : relic.capacity()));
		}
		return Component.translatable("message.jugcraft.concordance.relic.status", stack.getHoverName(), where,
				Component.translatable("message.jugcraft.concordance.relic.cannot", reason(outcome)));
	}

	private static void command(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("jugcraft").then(Commands.literal("concordance").then(Commands.literal("relics")
				.executes(context -> report(context.getSource().getPlayerOrException(), line -> context.getSource().sendSuccess(() -> line, false))))));
	}

	/**
	 * Every relic {@code player} carries or has installed, each with whether it works where it is and why not (its
	 * check only: nothing pulses), for the relics command and the Concordance Journal; returns how many.
	 */
	public static int report(ServerPlayer player, java.util.function.Consumer<Component> out) {
		List<Found> found = find(player);
		if (found.isEmpty()) {
			out.accept(Component.translatable("message.jugcraft.concordance.relic.none"));
			return 0;
		}
		for (Found relic : found) {
			out.accept(line(relic.stack(), work(player, relic, false)));
		}
		return found.size();
	}
}
