package io.github.jimbozoomer.jugcraft.concordance.spire;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceData;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceEffects;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import io.github.jimbozoomer.jugcraft.concordance.Illumination;
import io.github.jimbozoomer.jugcraft.concordance.JugcraftConcordance;
import io.github.jimbozoomer.jugcraft.concordance.LeyPylonBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.Rituals;
import io.github.jimbozoomer.jugcraft.concordance.courier.CourierLedger;
import io.github.jimbozoomer.jugcraft.concordance.courier.CourierPostBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.courier.Couriers;
import io.github.jimbozoomer.jugcraft.concordance.effect.Cause;
import io.github.jimbozoomer.jugcraft.concordance.effect.EffectKind;
import io.github.jimbozoomer.jugcraft.concordance.effect.EffectSpec;
import io.github.jimbozoomer.jugcraft.concordance.effect.Intent;
import io.github.jimbozoomer.jugcraft.concordance.effect.Ledger;
import io.github.jimbozoomer.jugcraft.concordance.garden.OrganismCropBlock;
import io.github.jimbozoomer.jugcraft.concordance.logistics.Logistics;
import io.github.jimbozoomer.jugcraft.concordance.progression.StageDefinition;
import io.github.jimbozoomer.jugcraft.concordance.ritual.StructurePattern;
import io.github.jimbozoomer.jugcraft.concordance.ritual.StructureValidator;
import io.github.jimbozoomer.jugcraft.concordance.rules.Definitions;
import io.github.jimbozoomer.jugcraft.concordance.rules.Evidence;
import io.github.jimbozoomer.jugcraft.concordance.rules.FocusPool;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchEngine;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import io.github.jimbozoomer.jugcraft.concordance.sign.Sign;
import io.github.jimbozoomer.jugcraft.concordance.sign.Signs;
import io.github.jimbozoomer.jugcraft.concordance.sky.SkyItem;
import io.github.jimbozoomer.jugcraft.concordance.stages.StageProgress;
import io.github.jimbozoomer.jugcraft.concordance.wonder.SpireConfiguration;
import io.github.jimbozoomer.jugcraft.concordance.wonder.SpireDefinition;
import io.github.jimbozoomer.jugcraft.concordance.wonder.SpireState;
import io.github.jimbozoomer.jugcraft.concordance.wonder.Spires;
import io.github.jimbozoomer.jugcraft.concordance.wonder.WonderCatalog;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import io.github.jimbozoomer.jugcraft.party.JugcraftParties;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
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
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The Concord Spire on the server (roadmap step 25, docs/features/arcane-concordance-spire.md): a wonder founded at a
 * Spire Heart by someone at the Master stage who knows its configuration's research, raised through phases (structures
 * built round the heart, the configuration's practices carried through, the Kindling rite at a Lesser Circle nearby,
 * days of upkeep held in a row) and then kept: each day it takes its configuration's item from its store (which couriers
 * fill) and Ley Charge from its own pylons, and while it is whole, attended and supplied its field works. Its progress is
 * the world's ({@link SpireRecord}); the pure rules ({@link Spires}) decide everything and this class only looks and
 * applies. Nothing here ever breaks or replaces a block: structures are checked, light goes only into open air, crops
 * only grow.
 */
public final class ConcordSpire {
	public static final String WONDER = "jugcraft:concord_spire";
	/** The milestone a raised spire records for its keeper and contributors (an Architect route reads it). */
	public static final String MILESTONE = "jugcraft:spire_raised";
	/** How far from the heart a courier post may be for its upkeep requests, and how far commands look for a heart. */
	public static final int REACH = 16;
	/** How long a pulse's light or reveal lasts (ticks): longer than the pulse, so a kept field never goes dark. */
	public static final int HOLD_TICKS = 400;

	public static Block HEART;
	public static BlockEntityType<SpireHeartBlockEntity> HEART_ENTITY;

	private static final Map<String, TagKey<Block>> BLOCK_TAGS = new HashMap<>();

	private ConcordSpire() {
	}

	public static void register() {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Jugcraft.id("spire_heart"));
		HEART = Registry.register(BuiltInRegistries.BLOCK, key, new SpireHeartBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK)
				.strength(5.0F, 1200.0F).sound(SoundType.DEEPSLATE).lightLevel(state -> state.getValue(SpireHeartBlock.LIT) ? 12 : 3).noOcclusion()
				.setId(key)));
		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Jugcraft.id("spire_heart"));
		Item item = Registry.register(BuiltInRegistries.ITEM, itemKey, new SkyItem(HEART, new Item.Properties().useBlockDescriptionPrefix().setId(itemKey)));
		HEART_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("spire_heart"),
				FabricBlockEntityTypeBuilder.create(SpireHeartBlockEntity::new, HEART).build());
		ConcordanceProgress.listen(ConcordSpire::recorded);
		Rituals.listen(ConcordSpire::ritual);
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> join(handler.getPlayer()));
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> command(dispatcher));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(output -> output.accept(item));
	}

	public static boolean enabled() {
		return JugcraftConfig.isFeatureEnabled(JugcraftConcordance.FEATURE);
	}

	public static WonderCatalog catalog() {
		return ConcordanceData.rules().wonders();
	}

	public static @Nullable SpireDefinition definition() {
		return catalog().wonder(WONDER);
	}

	/** A spire's id: where its heart stands ({@code minecraft:overworld@12,64,-30}). */
	public static String id(ServerLevel level, BlockPos pos) {
		return level.dimension().identifier() + "@" + pos.getX() + "," + pos.getY() + "," + pos.getZ();
	}

	/** Where a spire's heart stands, if {@code id} is in {@code level}'s dimension. */
	public static @Nullable BlockPos pos(ServerLevel level, String id) {
		String prefix = level.dimension().identifier() + "@";
		if (!id.startsWith(prefix)) {
			return null;
		}
		String[] parts = id.substring(prefix.length()).split(",");
		try {
			return new BlockPos(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
		} catch (RuntimeException malformed) {
			return null;
		}
	}

	/** Whether {@code player} works on {@code state}: its keeper, or someone in the keeper's party when it is shared. */
	public static boolean belongs(SpireState state, UUID player) {
		return state.keeper().equals(player) || state.communal() && JugcraftParties.sameParty(state.keeper(), player);
	}

	/** The keeper's side: the keeper, and their party when the spire is shared (whose pylons it may draw on). */
	static Set<UUID> side(SpireState state) {
		Set<UUID> side = new HashSet<>();
		side.add(state.keeper());
		if (state.communal()) {
			side.addAll(JugcraftParties.partyMembers(state.keeper()));
		}
		return side;
	}

	private static void say(ServerPlayer player, String key, Object... args) {
		player.sendSystemMessage(Component.translatable("message.jugcraft.concordance.spire." + key, args));
	}

	private static void say(Consumer<Component> out, String key, Object... args) {
		out.accept(Component.translatable("message.jugcraft.concordance.spire." + key, args));
	}

	private static void refuse(ServerPlayer player, String reason) {
		player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.spire.refused",
				Component.translatable("compose.jugcraft.spire.reason." + reason)));
	}

	public static Component configurationName(String id) {
		return Component.translatable("compose.jugcraft.spire.configuration." + id.substring(id.indexOf(':') + 1));
	}

	public static Component phaseName(String id) {
		return Component.translatable("compose.jugcraft.spire.phase." + id);
	}

	private static Map<String, ResearchState> research(ServerPlayer player) {
		Map<String, ResearchState> states = new HashMap<>();
		for (Definitions.Research entry : ConcordanceData.rules().research().values()) {
			states.put(entry.id(), ConcordanceProgress.knowledge(player).state(entry.id()));
		}
		return states;
	}

	// ---------------------------------------------------------------- founding

	/** Founds a spire of {@code configurationId} at {@code heart} for {@code player}. Returns why not, or "" (tests call this). */
	public static String found(ServerPlayer player, ServerLevel level, BlockPos heart, String configurationId) {
		if (!enabled()) {
			return "disabled";
		}
		SpireDefinition definition = definition();
		SpireConfiguration configuration = catalog().configuration(configurationId);
		if (definition == null || configuration == null || !configuration.wonder().equals(definition.id())) {
			return "unknown";
		}
		SpireRecord record = SpireRecord.of(level.getServer());
		String id = id(level, heart);
		if (record.spire(id) != null) {
			return "founded";
		}
		StageDefinition needed = StageProgress.catalog().stages().get(definition.stage());
		StageDefinition held = StageProgress.stage(player);
		String reason = Spires.mayFound(configuration, held == null ? 0 : held.order(), needed == null ? Integer.MAX_VALUE : needed.order(),
				research(player));
		if (!reason.isEmpty()) {
			return reason;
		}
		record.put(Spires.found(id, definition, configuration, player.getUUID(), false, level.getGameTime()));
		ConcordanceProgress.award(player, Jugcraft.id("concord_spire_founded"));
		say(player, "founded", configurationName(configuration.id()));
		return "";
	}

	/** A heart placed where a spire is recorded: it answers again, for the keeper's side. */
	static void placed(ServerPlayer player, ServerLevel level, BlockPos pos) {
		SpireState state = SpireRecord.of(level.getServer()).spire(id(level, pos));
		if (state != null) {
			say(player, belongs(state, player.getUUID()) ? "returned" : "kept_by_another");
		}
	}

	// ---------------------------------------------------------------- the heart's uses

	/**
	 * An empty hand on a heart. Not founded: sneaking chooses the next configuration (saying what it needs), otherwise it
	 * founds the chosen one. Founded: its keeper sneaking shares it with their party or takes it back; anyone else (or not
	 * sneaking) is shown how it stands.
	 */
	static void use(ServerPlayer player, ServerLevel level, SpireHeartBlockEntity heart, boolean sneaking) {
		if (!enabled()) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.disabled"));
			return;
		}
		SpireDefinition definition = definition();
		if (definition == null) {
			refuse(player, "unknown");
			return;
		}
		SpireRecord record = SpireRecord.of(level.getServer());
		SpireState state = record.spire(id(level, heart.getBlockPos()));
		if (state == null) {
			if (heart.owner() != null && !heart.owner().equals(player.getUUID()) && !JugcraftParties.sameParty(heart.owner(), player.getUUID())) {
				refuse(player, "not_yours");
				return;
			}
			List<SpireConfiguration> configurations = catalog().configurations(definition.id());
			if (configurations.isEmpty()) {
				refuse(player, "unknown");
				return;
			}
			int index = 0;
			for (int i = 0; i < configurations.size(); i++) {
				if (configurations.get(i).id().equals(heart.selected())) {
					index = i;
				}
			}
			if (sneaking || heart.selected().isEmpty()) {
				SpireConfiguration next = configurations.get(sneaking && !heart.selected().isEmpty() ? (index + 1) % configurations.size() : index);
				heart.select(next.id());
				say(player, "selected", configurationName(next.id()), Component.translatable("compose.jugcraft.spire.needs."
						+ next.id().substring(next.id().indexOf(':') + 1)));
				return;
			}
			String reason = found(player, level, heart.getBlockPos(), configurations.get(index).id());
			if (!reason.isEmpty()) {
				refuse(player, reason);
			}
			return;
		}
		if (sneaking && state.keeper().equals(player.getUUID())) {
			SpireState next = state.withCommunal(!state.communal());
			record.put(next);
			say(player, next.communal() ? "shared" : "kept");
			return;
		}
		describe(player::sendSystemMessage, level, heart.getBlockPos(), state);
	}

	/** The configuration's upkeep item used on a heart goes into its store (the keeper's side only). */
	static boolean offer(ServerPlayer player, ServerLevel level, SpireHeartBlockEntity heart, ItemStack stack) {
		SpireState state = SpireRecord.of(level.getServer()).spire(id(level, heart.getBlockPos()));
		SpireConfiguration configuration = state == null ? null : catalog().configuration(state.configuration());
		if (configuration == null || !BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(configuration.upkeepItem())) {
			return false;
		}
		if (!belongs(state, player.getUUID())) {
			refuse(player, "not_yours");
			return true;
		}
		int stored = heart.store(stack);
		player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.spire.stored", stored, stack.getHoverName()));
		return true;
	}

	// ---------------------------------------------------------------- what the research engine and the circles tell it

	/** A practice the keeper's side carried through: it attends their spires and counts towards their phases. */
	private static void recorded(ServerPlayer player, Evidence evidence, ResearchEngine.Result result) {
		if (!enabled() || !(evidence instanceof Evidence.Practiced practiced)) {
			return;
		}
		SpireDefinition definition = definition();
		if (definition == null) {
			return;
		}
		SpireRecord record = SpireRecord.of(((ServerLevel) player.level()).getServer());
		long now = player.level().getGameTime();
		for (SpireState state : record.all()) {
			SpireConfiguration configuration = catalog().configuration(state.configuration());
			if (configuration == null || !belongs(state, player.getUUID())) {
				continue;
			}
			SpireState next = Spires.practiced(definition, configuration, state, practiced.activity(), player.getUUID(), now);
			if (!next.equals(state)) {
				record.put(next);
			}
		}
	}

	/** A ritual completed: the wonder's rite near a spire's heart counts for it when its keeper's side took part. */
	private static void ritual(ServerLevel level, BlockPos anchor, String ritual, List<ServerPlayer> present) {
		SpireDefinition definition = definition();
		if (!enabled() || definition == null || !ritual.equals(definition.rite())) {
			return;
		}
		SpireRecord record = SpireRecord.of(level.getServer());
		for (SpireState state : record.all()) {
			BlockPos heart = pos(level, state.id());
			if (heart == null || heart.distSqr(anchor) > (double) definition.riteRange() * definition.riteRange()) {
				continue;
			}
			List<UUID> theirs = present.stream().map(Entity::getUUID).filter(uuid -> belongs(state, uuid)).toList();
			SpireState next = theirs.isEmpty() ? state : Spires.rite(definition, state, theirs);
			if (!next.equals(state)) {
				record.put(next);
				for (ServerPlayer player : present) {
					if (theirs.contains(player.getUUID())) {
						say(player, "rite", heart.getX(), heart.getY(), heart.getZ());
					}
				}
			}
		}
	}

	/** A player joining: the milestone of every raised spire they kept or helped raise (given once). */
	private static void join(ServerPlayer player) {
		SpireDefinition definition = definition();
		if (!enabled() || definition == null) {
			return;
		}
		for (SpireState state : SpireRecord.of(((ServerLevel) player.level()).getServer()).all()) {
			if (state.raised(definition) && (state.keeper().equals(player.getUUID()) || state.contributors().contains(player.getUUID()))) {
				raisedFor(player);
			}
		}
	}

	private static void raisedFor(ServerPlayer player) {
		ConcordanceProgress.award(player, Jugcraft.id("concord_spire_raised"));
		StageProgress.milestone(player, MILESTONE);
	}

	// ---------------------------------------------------------------- the heart's look at its spire

	/** What stands round a heart: whole or not, unloaded, the Ley its pylons hold for the keeper's side, its faults. */
	record Standing(boolean intact, boolean unloaded, long ley, List<LeyPylonBlockEntity> pylons, List<String> faults) {
	}

	/** Checks every structure the spire's phases have raised so far (all of them once raised) round {@code heart}. */
	static Standing stand(ServerLevel level, BlockPos heart, SpireDefinition definition, SpireConfiguration configuration, SpireState state) {
		Set<UUID> lenders = side(state);
		List<String> faults = new ArrayList<>();
		List<LeyPylonBlockEntity> pylons = new ArrayList<>();
		boolean unloaded = false;
		long ley = 0;
		for (String structure : definition.structuresThrough(Math.min(state.phase(), definition.phases().size() - 1))) {
			String id = structure.equals(SpireDefinition.CROWN) ? configuration.crown() : structure;
			StructurePattern pattern = ConcordanceData.rules().structure(id);
			if (pattern == null) {
				faults.add("unknown " + id);
				continue;
			}
			StructureValidator.Report report = StructureValidator.check(pattern, part -> {
				BlockPos at = heart.offset(part.offset().x(), part.offset().y(), part.offset().z());
				if (!level.isLoaded(at)) {
					return StructureValidator.Seen.UNLOADED;
				}
				BlockState found = level.getBlockState(at);
				long held = 0L;
				boolean lent = true;
				if (part.role() == StructurePattern.Role.CHANNEL && level.getBlockEntity(at) instanceof LeyPylonBlockEntity pylon) {
					held = pylon.ley();
					lent = pylon.lends(lenders);
				}
				return new StructureValidator.Seen(true, found.isAir(), part.block() != null && matches(found, part.block()), held, lent);
			}, 0L);
			unloaded |= report.has(StructureValidator.Problem.UNLOADED);
			for (StructureValidator.Fault fault : report.faults()) {
				StructurePattern.Offset offset = fault.part().offset();
				faults.add(fault.problem().id + " " + (fault.part().block() == null ? "air" : fault.part().block()) + " " + offset.x() + " " + offset.y()
						+ " " + offset.z());
			}
			for (StructurePattern.Part part : pattern.channels()) {
				BlockPos at = heart.offset(part.offset().x(), part.offset().y(), part.offset().z());
				if (level.isLoaded(at) && level.getBlockEntity(at) instanceof LeyPylonBlockEntity pylon && pylon.lends(lenders)) {
					pylons.add(pylon);
					ley += pylon.ley();
				}
			}
		}
		return new Standing(faults.isEmpty(), unloaded, ley, pylons, faults);
	}

	private static boolean matches(BlockState state, String spec) {
		if (spec.startsWith("#")) {
			Identifier tag = Identifier.tryParse(spec.substring(1));
			return tag != null && state.is(BLOCK_TAGS.computeIfAbsent(spec, unused -> TagKey.create(Registries.BLOCK, tag)));
		}
		return BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString().equals(spec);
	}

	/**
	 * One look at the spire at {@code heart}'s place: the day's upkeep when it falls due and the next phase when nothing
	 * is missing. Returns what the heart shows: "unfounded", "raising", "active", or why its field rests ("damaged",
	 * "unattended", "unsupplied"); a spire partly unloaded changes nothing and shows what it showed.
	 */
	static String work(ServerLevel level, SpireHeartBlockEntity heart, long now) {
		if (!enabled()) {
			return "disabled";
		}
		SpireDefinition definition = definition();
		SpireRecord record = SpireRecord.of(level.getServer());
		SpireState state = record.spire(id(level, heart.getBlockPos()));
		if (definition == null) {
			return "unknown";
		}
		if (state == null) {
			return "unfounded";
		}
		SpireConfiguration configuration = catalog().configuration(state.configuration());
		if (configuration == null) {
			return "unknown";
		}
		Standing standing = stand(level, heart.getBlockPos(), definition, configuration, state);
		if (standing.unloaded()) {
			return heart.status();
		}
		SpireState next = state;
		Spires.Day day = Spires.day(definition, configuration, next, standing.ley(), heart.count(configuration.upkeepItem()), standing.intact(), now);
		if (day.due()) {
			if (day.met()) {
				// Roadmap step 27: the day's Ley Charge travels from each pylon it was drawn from.
				for (BlockPos pylon : draw(level, standing.pylons(), day.ley())) {
					Signs.flow(level, pylon, heart.getBlockPos());
				}
				heart.take(configuration.upkeepItem(), day.items());
			} else {
				Signs.show(level, heart.getBlockPos(), Sign.WANT);
			}
			next = day.next();
		}
		if (Spires.upkeepRuns(definition, next)) {
			restock(level, heart, next, configuration, now);
		}
		if (!next.raised(definition) && Spires.missing(definition, next, standing.intact()).isEmpty()) {
			SpireDefinition.Phase finished = Spires.phase(definition, next);
			next = Spires.advance(definition, next, now);
			tell(level, next, next.raised(definition) ? Component.translatable("message.jugcraft.concordance.spire.raised")
					: Component.translatable("message.jugcraft.concordance.spire.advanced", phaseName(finished.id()),
							phaseName(definition.phases().get(next.phase()).id())));
			if (next.raised(definition)) {
				raise(level, next);
			}
			Signs.show(level, heart.getBlockPos(), Sign.DONE);
		}
		if (!next.equals(state)) {
			record.put(next);
		}
		String dormant = Spires.dormant(definition, next, standing.intact(), now);
		return dormant.isEmpty() ? "active" : dormant;
	}

	/** A working spire's heart, on the server's own tick: its field pulses every pulse ticks. */
	static void pulse(ServerLevel level, SpireHeartBlockEntity heart, long now) {
		SpireDefinition definition = definition();
		SpireState state = SpireRecord.of(level.getServer()).spire(id(level, heart.getBlockPos()));
		SpireConfiguration configuration = state == null ? null : catalog().configuration(state.configuration());
		if (definition != null && configuration != null && heart.pulse(now, definition.pulseTicks())) {
			field(level, heart.getBlockPos(), state, configuration);
		}
	}

	/** Tells everyone on the keeper's side who is online. */
	private static void tell(ServerLevel level, SpireState state, Component message) {
		for (UUID uuid : side(state)) {
			ServerPlayer player = level.getServer().getPlayerList().getPlayer(uuid);
			if (player != null) {
				player.sendSystemMessage(message);
			}
		}
	}

	/** Raised: its keeper and contributors online now record the milestone; the others when they next join. */
	private static void raise(ServerLevel level, SpireState state) {
		Set<UUID> everyone = new HashSet<>(state.contributors());
		everyone.add(state.keeper());
		for (UUID uuid : everyone) {
			ServerPlayer player = level.getServer().getPlayerList().getPlayer(uuid);
			if (player != null) {
				raisedFor(player);
			}
		}
	}

	/** Draws {@code ley} from {@code pylons}, first to last, each what it holds; returns where it was drawn from. */
	private static List<BlockPos> draw(ServerLevel level, List<LeyPylonBlockEntity> pylons, long ley) {
		List<BlockPos> from = new ArrayList<>();
		long drawn = 0;
		for (LeyPylonBlockEntity pylon : pylons) {
			long take = Math.min(ley - drawn, pylon.ley());
			if (take > 0 && pylon.draw(level, take)) {
				drawn += take;
				from.add(pylon.getBlockPos());
			}
			if (drawn >= ley) {
				break;
			}
		}
		return from;
	}

	/**
	 * Keeps two days of upkeep coming: while its store holds less, it files one courier request (its keeper's) at the
	 * nearest Courier Post within {@value #REACH} blocks the keeper may use, for what is short, and files no other until
	 * that one closes.
	 */
	private static void restock(ServerLevel level, SpireHeartBlockEntity heart, SpireState state, SpireConfiguration configuration, long now) {
		CourierLedger ledger = CourierLedger.of(level.getServer());
		if (heart.request() >= 0 && ledger.ledger().get(heart.request()) != null) {
			return;
		}
		int want = configuration.upkeepCount() * 2 - heart.count(configuration.upkeepItem());
		if (want <= 0) {
			heart.setRequest(-1L);
			return;
		}
		CourierPostBlockEntity post = nearestPost(level, heart.getBlockPos(), state.keeper());
		Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(configuration.upkeepItem()));
		if (post == null) {
			heart.setRequest(-1L);
			return;
		}
		Logistics.Filed filed = ledger.file(state.keeper(), Couriers.place(level, post.getBlockPos()), Couriers.place(level, heart.getBlockPos()),
				new ItemStack(item), want, level.registryAccess(), now);
		heart.setRequest(filed.outcome() == Logistics.Outcome.DONE ? filed.id() : -1L);
	}

	private static @Nullable CourierPostBlockEntity nearestPost(ServerLevel level, BlockPos heart, UUID keeper) {
		CourierPostBlockEntity nearest = null;
		double best = Double.MAX_VALUE;
		int cx = heart.getX() >> 4;
		int cz = heart.getZ() >> 4;
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				if (!level.hasChunk(cx + dx, cz + dz)) {
					continue;
				}
				for (BlockEntity entity : List.copyOf(level.getChunk(cx + dx, cz + dz).getBlockEntities().values())) {
					double distance = entity.getBlockPos().distSqr(heart);
					if (entity instanceof CourierPostBlockEntity post && distance <= (double) REACH * REACH && distance < best && post.mayUse(keeper)) {
						nearest = post;
						best = distance;
					}
				}
			}
		}
		return nearest;
	}

	// ---------------------------------------------------------------- the field

	/** One pulse of the field, bounded: at most the configuration's count of things within its radius (tests call this). */
	public static int field(ServerLevel level, BlockPos heart, SpireState state, SpireConfiguration configuration) {
		// The keeper, if here; its light and growth change blocks only as they could by hand (Authority, step 28).
		ServerPlayer keeper = Authority.present(level, state.keeper());
		Cause cause = Cause.of(state.keeper(), Cause.Origin.SHRINE, WONDER, ConcordanceEffects.nextSerial());
		return switch (configuration.field()) {
			case ILLUMINATION -> illuminate(level, heart, configuration, cause, keeper);
			case GROWTH -> grow(level, heart, state, configuration);
			case FOCUS -> focus(level, heart, state, configuration, cause, keeper);
		};
	}

	/** Kindled light in dark open air near the ground, so nothing hostile spawns there while the spire holds. */
	private static int illuminate(ServerLevel level, BlockPos heart, SpireConfiguration configuration, Cause cause, @Nullable ServerPlayer keeper) {
		int count = configuration.count();
		Ledger ledger = new Ledger(new Ledger.Limits(count, count * EffectKind.ILLUMINATION.work, 0));
		ConcordanceEffects.Context context = new ConcordanceEffects.Context(level, cause, keeper, ledger, "spire.light", Vec3.atCenterOf(heart));
		EffectSpec light = EffectSpec.of(EffectKind.ILLUMINATION, Intent.HELPFUL, 0, HOLD_TICKS);
		RandomSource random = level.getRandom();
		int lit = 0;
		for (int attempt = 0; attempt < count * 6 && lit < count; attempt++) {
			BlockPos column = sample(heart, configuration.radius(), random);
			for (int dy = 4; dy >= -6; dy--) {
				BlockPos at = column.above(dy);
				if (level.isLoaded(at) && Illumination.open(level.getBlockState(at)) && level.getBlockState(at.below()).isFaceSturdy(level, at.below(), Direction.UP)
						&& level.getBrightness(LightLayer.BLOCK, at) < 8) {
					if (ConcordanceEffects.apply(context, light, at).applied()) {
						lit++;
					}
					break;
				}
			}
		}
		return lit;
	}

	/**
	 * Crops in the field grow one step each (once a pulse); nothing ripe is touched and nothing is harvested or replaced.
	 * Only crops its keeper could tend by hand grow (roadmap step 28), and only while they are here, unless the server
	 * lets a stand-in answer for absent owners. A Concordance crop grows by its own rules, paying its bed and fixing
	 * nothing ({@link OrganismCropBlock#hasten}, roadmap step 29); a vanilla crop simply ages, as bone meal ages it.
	 */
	private static int grow(ServerLevel level, BlockPos heart, SpireState state, SpireConfiguration configuration) {
		ServerPlayer keeper = Authority.answering(level, state.keeper());
		if (keeper == null) {
			return 0;
		}
		RandomSource random = level.getRandom();
		Set<BlockPos> grown = new HashSet<>();
		for (int attempt = 0; attempt < configuration.count() * 6 && grown.size() < configuration.count(); attempt++) {
			BlockPos column = sample(heart, configuration.radius(), random);
			for (int dy = 4; dy >= -6; dy--) {
				BlockPos at = column.above(dy);
				if (grown.contains(at)) {
					break;
				}
				BlockState crop = level.isLoaded(at) ? level.getBlockState(at) : null;
				if (crop != null && crop.getBlock() instanceof OrganismCropBlock organism && !organism.isMaxAge(crop)) {
					if (Authority.mayChange(level, keeper, at) && organism.hasten(level, at, crop, random) == OrganismCropBlock.Growth.GREW) {
						grown.add(at);
						Signs.show(level, at, Sign.WORK);
					}
					break;
				}
				BlockState older = crop != null && crop.getBlock() instanceof CropBlock growing && !growing.isMaxAge(crop) ? older(crop) : null;
				if (older != null && Authority.mayChange(level, keeper, at)) {
					level.setBlock(at, older, Block.UPDATE_CLIENTS);
					grown.add(at);
					Signs.show(level, at, Sign.WORK);
					break;
				}
			}
		}
		return grown.size();
	}

	/** A crop one step older (its "age" property one higher), or null if it has none to give. */
	private static @Nullable BlockState older(BlockState state) {
		for (Property<?> property : state.getProperties()) {
			if (property instanceof IntegerProperty age && age.getName().equals("age")) {
				int value = state.getValue(age);
				return age.getPossibleValues().contains(value + 1) ? state.setValue(age, value + 1) : null;
			}
		}
		return null;
	}

	/** The keeper's side in the field regain a Focus each; hostile creatures in it are revealed. */
	private static int focus(ServerLevel level, BlockPos heart, SpireState state, SpireConfiguration configuration, Cause cause,
			@Nullable ServerPlayer keeper) {
		Vec3 center = Vec3.atCenterOf(heart);
		double reach = (double) configuration.radius() * configuration.radius();
		int helped = 0;
		for (ServerPlayer player : level.players()) {
			if (helped < configuration.count() && belongs(state, player.getUUID()) && player.distanceToSqr(center) <= reach) {
				int held = ConcordanceProgress.currentFocus(player);
				if (held < FocusPool.MAX) {
					ConcordanceProgress.setFocus(player, held + 1);
					helped++;
					// The Focus given, travelling from the heart to the player.
					Signs.show(level, player.position().add(0.0, 1.0, 0.0), Sign.FLOW, center);
				}
			}
		}
		List<LivingEntity> hostile = new ArrayList<>(level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(configuration.radius()),
				entity -> entity instanceof Enemy && entity.isAlive() && entity.distanceToSqr(center) <= reach));
		hostile.sort(Comparator.<LivingEntity>comparingDouble(entity -> entity.distanceToSqr(center)).thenComparing(Entity::getUUID));
		int count = Math.min(hostile.size(), configuration.count());
		if (count > 0) {
			Ledger ledger = new Ledger(new Ledger.Limits(count, count * EffectKind.DETECTION.work, 0));
			ConcordanceEffects.Context context = new ConcordanceEffects.Context(level, cause, keeper, ledger, "spire.reveal", center);
			EffectSpec reveal = EffectSpec.of(EffectKind.DETECTION, Intent.HARMFUL, 0, HOLD_TICKS);
			for (LivingEntity creature : hostile.subList(0, count)) {
				if (ConcordanceEffects.apply(context, reveal, creature).applied()) {
					helped++;
				}
			}
		}
		return helped;
	}

	/** A column in the field's disc, around {@code heart}. */
	private static BlockPos sample(BlockPos heart, int radius, RandomSource random) {
		while (true) {
			int dx = random.nextInt(radius * 2 + 1) - radius;
			int dz = random.nextInt(radius * 2 + 1) - radius;
			if (dx * dx + dz * dz <= radius * radius) {
				return heart.offset(dx, 0, dz);
			}
		}
	}

	// ---------------------------------------------------------------- inspecting

	/** How a spire stands, line by line: what it is, its phase and what it needs, its upkeep, its field, its attendance. */
	public static void describe(Consumer<Component> out, ServerLevel level, BlockPos heart, SpireState state) {
		SpireDefinition definition = definition();
		SpireConfiguration configuration = catalog().configuration(state.configuration());
		if (definition == null || configuration == null) {
			out.accept(Component.translatable("message.jugcraft.concordance.spire.refused", Component.translatable("compose.jugcraft.spire.reason.unknown")));
			return;
		}
		ServerPlayer keeper = level.getServer().getPlayerList().getPlayer(state.keeper());
		say(out, state.communal() ? "title_shared" : "title", configurationName(configuration.id()),
				keeper == null ? Component.translatable("message.jugcraft.concordance.spire.away") : keeper.getDisplayName());
		long now = level.getGameTime();
		Standing standing = stand(level, heart, definition, configuration, state);
		SpireDefinition.Phase phase = Spires.phase(definition, state);
		if (phase != null) {
			String missing = Spires.missing(definition, state, standing.intact());
			say(out, "phase", state.phase() + 1, definition.phases().size(), phaseName(phase.id()), needs(definition, configuration, state, phase,
					missing, standing));
		}
		if (Spires.upkeepRuns(definition, state)) {
			say(out, "upkeep", configuration.upkeepCount(), itemName(configuration.upkeepItem()), configuration.upkeepLey(),
					heart(level, heart) == null ? 0 : heart(level, heart).count(configuration.upkeepItem()), standing.ley(),
					Math.max(0L, (state.nextDay() - now) / 1200L));
		}
		String dormant = Spires.dormant(definition, state, standing.intact(), now);
		say(out, dormant.isEmpty() ? "field_working" : "field_resting",
				Component.translatable("compose.jugcraft.spire.field." + configuration.field().id, configuration.radius()),
				Component.translatable("compose.jugcraft.spire.dormant." + (dormant.isEmpty() ? "raising" : dormant)));
		long left = definition.attendanceDays() - (now - state.lastAttended()) / Spires.DAY;
		say(out, Spires.attended(definition, state, now) ? "attended" : "unattended", Math.max(0L, left),
				Component.translatable("compose.jugcraft.conclave.activity." + configuration.practice().substring(configuration.practice().indexOf(':') + 1)));
	}

	private static Component needs(SpireDefinition definition, SpireConfiguration configuration, SpireState state, SpireDefinition.Phase phase,
			String missing, Standing standing) {
		return switch (missing) {
			case "" -> Component.translatable("compose.jugcraft.spire.missing.nothing");
			case "structure" -> Component.translatable("compose.jugcraft.spire.missing.structure",
					String.join("; ", standing.faults().subList(0, Math.min(3, standing.faults().size()))));
			case "practices" -> Component.translatable("compose.jugcraft.spire.missing.practices", phase.practices() - state.practiced(),
					Component.translatable("compose.jugcraft.conclave.activity." + configuration.practice().substring(configuration.practice().indexOf(':') + 1)));
			case "rite" -> Component.translatable("compose.jugcraft.spire.missing.rite", definition.riteRange());
			case "sustain" -> Component.translatable("compose.jugcraft.spire.missing.sustain", state.sustained(), phase.sustain());
			default -> Component.translatable("compose.jugcraft.spire.missing." + missing);
		};
	}

	private static Component itemName(String id) {
		return new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(id))).getHoverName();
	}

	private static @Nullable SpireHeartBlockEntity heart(ServerLevel level, BlockPos pos) {
		return level.isLoaded(pos) && level.getBlockEntity(pos) instanceof SpireHeartBlockEntity heart ? heart : null;
	}

	/** The spire whose heart is nearest {@code player} within {@value #REACH} blocks and that they work on, if any. */
	private static @Nullable SpireState nearest(ServerPlayer player, boolean keeperOnly) {
		ServerLevel level = (ServerLevel) player.level();
		SpireState nearest = null;
		double best = Double.MAX_VALUE;
		for (SpireState state : SpireRecord.of(level.getServer()).all()) {
			BlockPos heart = pos(level, state.id());
			double distance = heart == null ? Double.MAX_VALUE : heart.distSqr(player.blockPosition());
			if (heart != null && distance <= (double) REACH * REACH && distance < best
					&& (keeperOnly ? state.keeper().equals(player.getUUID()) : belongs(state, player.getUUID()))) {
				nearest = state;
				best = distance;
			}
		}
		return nearest;
	}

	/** Changes the configuration of the spire {@code player} keeps nearest them. Returns why not, or "" (tests call this). */
	public static String realign(ServerPlayer player, String configurationId) {
		SpireDefinition definition = definition();
		SpireState state = nearest(player, true);
		SpireConfiguration next = catalog().configuration(configurationId);
		if (!enabled() || definition == null || next == null || !next.wonder().equals(definition.id())) {
			return "unknown";
		}
		if (state == null) {
			return "no_spire";
		}
		StageDefinition needed = StageProgress.catalog().stages().get(definition.stage());
		StageDefinition held = StageProgress.stage(player);
		String reason = Spires.mayFound(next, held == null ? 0 : held.order(), needed == null ? Integer.MAX_VALUE : needed.order(), research(player));
		if (!reason.isEmpty()) {
			return reason;
		}
		SpireRecord.of(((ServerLevel) player.level()).getServer()).put(Spires.realign(definition, state, next, player.level().getGameTime()));
		say(player, "realigned", configurationName(next.id()));
		return "";
	}

	/**
	 * Every spire in {@code player}'s dimension that they keep or share, each as {@link #describe} shows it, or that they
	 * keep none (the command and the Concordance Journal); returns how many.
	 */
	public static int report(ServerPlayer player, Consumer<Component> out) {
		ServerLevel level = (ServerLevel) player.level();
		int shown = 0;
		for (SpireState state : SpireRecord.of(level.getServer()).all()) {
			BlockPos heart = pos(level, state.id());
			if (heart != null && belongs(state, player.getUUID())) {
				describe(out, level, heart, state);
				shown++;
			}
		}
		if (shown == 0) {
			say(out, "none");
		}
		return shown;
	}

	private static void command(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("jugcraft").then(Commands.literal("concordance").then(Commands.literal("spire")
				.executes(context -> {
					ServerPlayer player = context.getSource().getPlayerOrException();
					return report(player, player::sendSystemMessage);
				})
				.then(Commands.literal("realign").then(Commands.argument("configuration", StringArgumentType.word()).executes(context -> {
					ServerPlayer player = context.getSource().getPlayerOrException();
					String reason = realign(player, "jugcraft:" + StringArgumentType.getString(context, "configuration"));
					if (!reason.isEmpty()) {
						refuse(player, reason);
					}
					return reason.isEmpty() ? 1 : 0;
				})))
				.then(Commands.literal("abandon").then(Commands.literal("confirm").executes(context -> {
					ServerPlayer player = context.getSource().getPlayerOrException();
					SpireState state = nearest(player, true);
					if (state == null) {
						refuse(player, "no_spire");
						return 0;
					}
					SpireRecord.of(((ServerLevel) player.level()).getServer()).remove(state.id());
					say(player, "abandoned");
					return 1;
				}))))));
	}
}
