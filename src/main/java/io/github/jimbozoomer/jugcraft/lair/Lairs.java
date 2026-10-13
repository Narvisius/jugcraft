package io.github.jimbozoomer.jugcraft.lair;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The lairs (docs/features/witching-season.md, "The lairs: shared rules"; docs/features/hollow-acre.md), on the server.
 *
 * <p>Each lair is its own pocket dimension. A ritual {@link #open opens} an instance: a free slot, at most
 * {@code lairs.instances} of them, into which the lair's template is placed fresh. Players {@link #enter come in} by their
 * own choice (the ritual's gate), at most {@code lairs.party_size}; each one's {@link LairVisit} remembers exactly where
 * they stood, and {@link #leave} puts them back there. Inside they cannot build or break anything ({@link LairRules}),
 * and once a second the mist throws back anyone who flies or falls off the island. Dying there costs nothing: what they
 * drop is gathered at once as their {@link GraveGoods} and handed back when they respawn. An instance nobody has been
 * inside for {@value #EMPTY_SECONDS} seconds closes; a restart closes them all, and anyone found inside a closed one is
 * sent home. No chunk is force-loaded: placing an instance loads its chunks for that moment only.
 */
public final class Lairs {
	public static final int CHECK_TICKS = 20;
	public static final int EMPTY_SECONDS = 30;
	public static final float EDGE_DAMAGE = 4.0F;
	public static final int MAX_INSTANCES = 32;
	private static final Map<Lair, LairInstance[]> INSTANCES = new EnumMap<>(Lair.class);
	/** What each lair calls up as an instance is placed (its boss). */
	private static final Map<Lair, List<BiConsumer<ServerLevel, LairInstance>>> PLACED = new EnumMap<>(Lair.class);
	/** What each lair puts right as an instance closes (the Cursed Spindle's wheel stops spinning wild). */
	private static final Map<Lair, List<BiConsumer<MinecraftServer, LairInstance>>> CLOSED = new EnumMap<>(Lair.class);
	public static AttachmentType<LairVisit> VISIT;
	public static AttachmentType<GraveGoods> GRAVE_GOODS;

	private Lairs() {
	}

	static void register() {
		VISIT = AttachmentRegistry.<LairVisit>builder().persistent(LairVisit.CODEC).buildAndRegister(Jugcraft.id("lair_visit"));
		GRAVE_GOODS = AttachmentRegistry.<GraveGoods>builder().persistent(GraveGoods.CODEC).copyOnDeath()
				.buildAndRegister(Jugcraft.id("grave_goods"));
		ServerTickEvents.END_SERVER_TICK.register(Lairs::tick);
		ServerLifecycleEvents.SERVER_STARTED.register(server -> INSTANCES.clear());
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> INSTANCES.clear());
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> joined(handler.getPlayer()));
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity instanceof ServerPlayer player) {
				died(player);
			}
		});
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
			if (!alive) {
				handBack(newPlayer);
			}
		});
	}

	// ---------------------------------------------------------------- settings

	/** How many instances of each lair may be open at once ({@code lairs.instances}, 1 to {@value #MAX_INSTANCES}). */
	public static int instances() {
		return number("lairs.instances", 8, 1, 32);
	}

	/** How many players an instance holds ({@code lairs.party_size}). */
	public static int partySize() {
		return number("lairs.party_size", 4, 1, 16);
	}

	/** How long a ritual's gate stays open, in ticks ({@code lairs.gate_seconds}). */
	public static int gateTicks() {
		return number("lairs.gate_seconds", 60, 10, 600) * 20;
	}

	/** Whether the rituals work outside the Halloween event ({@code lairs.off_season}). */
	public static boolean offSeason() {
		return !"off".equalsIgnoreCase(JugcraftConfig.textOption("lairs.off_season").trim());
	}

	private static int number(String option, int fallback, int min, int max) {
		try {
			return Math.clamp(Integer.parseInt(JugcraftConfig.textOption(option).trim()), min, max);
		} catch (NumberFormatException e) {
			return fallback;
		}
	}

	/** A decimal setting, kept between {@code min} and {@code max} ({@code fallback} when it does not read). */
	public static double decimal(String option, double fallback, double min, double max) {
		try {
			double value = Double.parseDouble(JugcraftConfig.textOption(option).trim());
			return Double.isFinite(value) ? Math.clamp(value, min, max) : fallback;
		} catch (NumberFormatException e) {
			return fallback;
		}
	}

	/** Calls {@code hook} for each instance of {@code lair} as it is placed (a boss seats itself). */
	public static void onPlaced(Lair lair, BiConsumer<ServerLevel, LairInstance> hook) {
		PLACED.computeIfAbsent(lair, l -> new ArrayList<>()).add(hook);
	}

	/** Calls {@code hook} for each instance of {@code lair} as it closes (its ritual's gate shuts). */
	public static void onClosed(Lair lair, BiConsumer<MinecraftServer, LairInstance> hook) {
		CLOSED.computeIfAbsent(lair, l -> new ArrayList<>()).add(hook);
	}

	// ---------------------------------------------------------------- instances

	public static boolean isLair(Level level) {
		return Lair.of(level.dimension()) != null;
	}

	/** Operators in creative mode may change a lair (repairs) and stay in one without a visit. */
	public static boolean exempt(Player player) {
		return player.getAbilities().instabuild && player instanceof ServerPlayer server
				&& Commands.LEVEL_GAMEMASTERS.check(server.createCommandSourceStack().permissions());
	}

	private static LairInstance[] slots(Lair lair) {
		return INSTANCES.computeIfAbsent(lair, l -> new LairInstance[MAX_INSTANCES]);
	}

	/** The open instances of a lair. */
	public static List<LairInstance> open(Lair lair) {
		List<LairInstance> out = new ArrayList<>();
		for (LairInstance instance : slots(lair)) {
			if (instance != null && instance.open) {
				out.add(instance);
			}
		}
		return out;
	}

	public static @Nullable LairInstance instance(Lair lair, int slot) {
		LairInstance[] slots = slots(lair);
		return slot >= 0 && slot < slots.length && slots[slot] != null && slots[slot].open ? slots[slot] : null;
	}

	/** The open instance a visit belongs to, or null when it has closed (or the server restarted since). */
	public static @Nullable LairInstance instance(@Nullable LairVisit visit) {
		if (visit == null) {
			return null;
		}
		Lair lair = Lair.byId(visit.lair());
		LairInstance instance = lair == null ? null : instance(lair, visit.slot());
		return instance != null && instance.id.equals(visit.instance()) ? instance : null;
	}

	/**
	 * Opens a fresh instance of {@code lair} in its first free slot, the template placed anew, or returns null when every
	 * slot the server allows is taken (or the lair's dimension is missing).
	 */
	public static @Nullable LairInstance open(MinecraftServer server, Lair lair) {
		ServerLevel level = server.getLevel(lair.dimension);
		if (level == null) {
			Jugcraft.LOGGER.warn("[lairs] No dimension {}: the lair cannot open", lair.dimension.identifier());
			return null;
		}
		LairInstance[] slots = slots(lair);
		for (int slot = 0; slot < instances(); slot++) {
			if (slots[slot] == null || !slots[slot].open) {
				LairInstance instance = new LairInstance(lair, slot, level.getGameTime());
				if (!place(level, instance)) {
					return null;
				}
				slots[slot] = instance;
				for (BiConsumer<ServerLevel, LairInstance> hook : PLACED.getOrDefault(lair, List.of())) {
					hook.accept(level, instance);
				}
				return instance;
			}
		}
		return null;
	}

	/**
	 * The lair's template, read fresh from the data packs ({@code data/jugcraft/structure/lair/}), or null when it is
	 * missing or unreadable. The file carries this game's data version (LairGameTests checks it), so nothing needs fixing.
	 */
	public static @Nullable StructureTemplate template(ServerLevel level, Lair lair) {
		Identifier path = Identifier.fromNamespaceAndPath(lair.template.getNamespace(), "structure/" + lair.template.getPath() + ".nbt");
		try (InputStream in = level.getServer().getResourceManager().getResourceOrThrow(path).open()) {
			StructureTemplate template = new StructureTemplate();
			template.load(level.registryAccess().lookupOrThrow(Registries.BLOCK), NbtIo.readCompressed(in, NbtAccounter.unlimitedHeap()));
			return template;
		} catch (IOException e) {
			Jugcraft.LOGGER.error("[lairs] Could not read the structure template {}: {}", path, e.toString());
			return null;
		}
	}

	/**
	 * Puts the lair's template into the instance's slot, as it was made: anything left lying there (items, a straggling
	 * creature) is cleared first, and the moon hung north of the island (in a lair that has one). Shapes are taken as the
	 * template has them.
	 */
	static boolean place(ServerLevel level, LairInstance instance) {
		Lair lair = instance.lair;
		StructureTemplate template = template(level, lair);
		if (template == null) {
			return false;
		}
		BlockPos origin = instance.origin();
		for (Entity entity : level.getEntitiesOfClass(Entity.class, instance.box(), entity -> !(entity instanceof Player))) {
			entity.discard();
		}
		StructurePlaceSettings settings = new StructurePlaceSettings().setKnownShape(true);
		template.placeInWorld(level, origin, origin, settings, level.getRandom(), Block.UPDATE_CLIENTS);
		if (lair.moon == null) {
			return true;
		}
		BlockState moon = JugcraftLairs.LAIR_MOON.defaultBlockState();
		BlockPos centre = origin.offset(lair.moon);
		int r = lair.moonRadius;
		for (int dx = -r; dx <= r; dx++) {
			for (int dy = -r; dy <= r; dy++) {
				if (dx * dx + dy * dy <= r * r + r) {
					level.setBlock(centre.offset(dx, dy, 0), moon, Block.UPDATE_CLIENTS);
				}
			}
		}
		return true;
	}

	/** Closes an instance: the slot is free for the next ritual, its gate shuts and anyone still inside goes home. */
	public static void close(MinecraftServer server, LairInstance instance) {
		instance.open = false;
		LairInstance[] slots = slots(instance.lair);
		if (slots[instance.slot] == instance) {
			slots[instance.slot] = null;
		}
		ServerLevel level = server.getLevel(instance.lair.dimension);
		if (level != null) {
			for (ServerPlayer player : List.copyOf(level.players())) {
				LairVisit visit = player.getAttached(VISIT);
				if (visit != null && visit.instance().equals(instance.id)) {
					sendHome(player, visit, Component.translatable("message.jugcraft.lair.closed"));
				}
			}
		}
		for (ServerLevel any : server.getAllLevels()) {
			if (instance.gate != null && any.getEntity(instance.gate) instanceof MistGateEntity gate) {
				gate.discard();
			}
		}
		for (BiConsumer<MinecraftServer, LairInstance> hook : CLOSED.getOrDefault(instance.lair, List.of())) {
			hook.accept(server, instance);
		}
	}

	// ---------------------------------------------------------------- coming and going

	/**
	 * Takes {@code player} into {@code instance} by their own choice (the ritual, or using its gate), when there is room;
	 * their return point is where they stand now. Returns whether they went in.
	 */
	public static boolean enter(ServerPlayer player, LairInstance instance) {
		MinecraftServer server = player.level().getServer();
		ServerLevel level = server.getLevel(instance.lair.dimension);
		if (level == null || !instance.open) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.lair.gate_closed"));
			return false;
		}
		if (isLair(player.level())) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.lair.already_inside"));
			return false;
		}
		Component name = Component.translatable("lair.jugcraft." + instance.lair.id);
		if (!instance.hasRoom(player.getUUID())) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.lair.party_full", name));
			return false;
		}
		ServerLevel from = (ServerLevel) player.level();
		player.setAttached(VISIT, new LairVisit(instance.lair.id, instance.slot, instance.id, from.dimension().identifier().toString(),
				player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot()));
		instance.admit(player.getUUID());
		instance.emptySince = -1L;
		player.stopRiding();
		Vec3 at = instance.arrival();
		player.teleportTo(level, at.x, at.y, at.z, Set.of(), instance.lair.arrivalYaw, 0.0F, true);
		player.resetFallDistance();
		restrict(player);
		player.sendSystemMessage(Component.translatable("message.jugcraft.lair.enter." + instance.lair.id, name));
		return true;
	}

	/** Takes {@code player} out of the lair they are in, back to where they stood when they came in. */
	public static boolean leave(ServerPlayer player) {
		LairVisit visit = player.getAttached(VISIT);
		if (visit == null && !isLair(player.level())) {
			return false;
		}
		sendHome(player, visit, Component.translatable("message.jugcraft.lair.leave"));
		player.sendSystemMessage(Component.translatable("message.jugcraft.lair.left_behind"));
		return true;
	}

	/**
	 * Back to the visit's return point, or (when there is none, or its dimension is gone) to the player's respawn point
	 * in the Overworld; the build ability comes back.
	 */
	static void sendHome(ServerPlayer player, @Nullable LairVisit visit, Component message) {
		MinecraftServer server = player.level().getServer();
		player.removeAttached(VISIT);
		restore(player);
		player.stopRiding();
		ServerLevel level = null;
		if (visit != null) {
			Identifier dimension = Identifier.tryParse(visit.dimension());
			level = dimension == null ? null : server.getLevel(ResourceKey.create(Registries.DIMENSION, dimension));
		}
		if (level != null && !isLair(level)) {
			player.teleportTo(level, visit.x(), visit.y(), visit.z(), Set.of(), visit.yRot(), visit.xRot(), true);
		} else {
			ServerLevel overworld = server.overworld();
			BlockPos spawn = overworld.getRespawnData().pos();
			player.teleportTo(overworld, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, Set.of(), player.getYRot(), 0.0F, true);
		}
		player.resetFallDistance();
		player.sendOverlayMessage(message);
	}

	/** In a lair a player cannot build (the rule vanilla uses for Adventure mode). */
	static void restrict(ServerPlayer player) {
		if (!exempt(player) && player.getAbilities().mayBuild) {
			player.getAbilities().mayBuild = false;
			player.onUpdateAbilities();
		}
	}

	/** Out of a lair the game mode's own build ability comes back. */
	static void restore(ServerPlayer player) {
		player.gameMode.getGameModeForPlayer().updatePlayerAbilities(player.getAbilities());
		player.onUpdateAbilities();
	}

	private static void joined(ServerPlayer player) {
		LairVisit visit = player.getAttached(VISIT);
		if (!isLair(player.level())) {
			if (visit != null) {
				player.removeAttached(VISIT);
				restore(player);
			}
			return;
		}
		LairInstance instance = instance(visit);
		if (instance == null) {
			if (!exempt(player)) {
				sendHome(player, visit, Component.translatable("message.jugcraft.lair.closed"));
			}
			return;
		}
		instance.admit(player.getUUID());
		restrict(player);
	}

	// ---------------------------------------------------------------- death

	/**
	 * A player has died in a lair: what they dropped (this tick's items and experience round them) is gathered up at
	 * once as their Grave Goods, to be handed back when they respawn outside. Their visit is over.
	 */
	public static void died(ServerPlayer player) {
		if (!(player.level() instanceof ServerLevel level) || !isLair(level)) {
			return;
		}
		LairInstance instance = instance(player.getAttached(VISIT));
		if (instance != null) {
			instance.members.remove(player.getUUID());
		}
		player.removeAttached(VISIT);
		AABB around = player.getBoundingBox().inflate(3.0);
		List<ItemStack> items = new ArrayList<>();
		for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, around, item -> item.tickCount == 0)) {
			items.add(item.getItem().copy());
			item.discard();
		}
		int experience = 0;
		for (ExperienceOrb orb : level.getEntitiesOfClass(ExperienceOrb.class, around, orb -> orb.tickCount == 0)) {
			experience += orb.getValue();
			orb.discard();
		}
		if (items.isEmpty() && experience == 0) {
			return;
		}
		GraveGoods held = player.getAttached(GRAVE_GOODS);
		player.setAttached(GRAVE_GOODS, held == null ? new GraveGoods(List.copyOf(items), experience) : held.plus(items, experience));
		player.sendSystemMessage(Component.translatable("message.jugcraft.lair.grave_goods.kept"));
	}

	/** Hands a respawned player their Grave Goods: into their inventory, the rest at their feet, and the experience. */
	public static boolean handBack(ServerPlayer player) {
		GraveGoods goods = player.getAttached(GRAVE_GOODS);
		if (goods == null) {
			return false;
		}
		player.removeAttached(GRAVE_GOODS);
		for (ItemStack stack : goods.items()) {
			player.getInventory().placeItemBackInInventory(stack.copy(), Prediction.SERVER_ONLY);
		}
		if (goods.experience() > 0) {
			player.giveExperiencePoints(goods.experience());
		}
		player.sendSystemMessage(Component.translatable("message.jugcraft.lair.grave_goods.returned"));
		return true;
	}

	// ---------------------------------------------------------------- once a second

	private static void tick(MinecraftServer server) {
		if (server.getTickCount() % CHECK_TICKS != 0) {
			return;
		}
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			// Taken out of a lair some other way (a command, another mod's teleport): the visit is over.
			if (!isLair(player.level()) && player.getAttached(VISIT) != null) {
				player.removeAttached(VISIT);
				restore(player);
			}
		}
		for (Lair lair : Lair.values()) {
			ServerLevel level = server.getLevel(lair.dimension);
			if (level == null) {
				continue;
			}
			check(server, level, lair, level.getGameTime());
		}
	}

	/**
	 * The players in one lair's dimension (the edges, strays) and its instances (empty ones close), at game time
	 * {@code now} (the server's own; tests look ahead).
	 */
	public static void check(MinecraftServer server, ServerLevel level, Lair lair, long now) {
		Map<LairInstance, Integer> present = new java.util.HashMap<>();
		for (ServerPlayer player : List.copyOf(level.players())) {
			if (!player.isAlive()) {
				continue; // dead on the death screen: they respawn outside
			}
			LairVisit visit = player.getAttached(VISIT);
			LairInstance instance = instance(visit);
			if (instance == null) {
				if (!exempt(player) && !player.isSpectator()) {
					sendHome(player, visit, Component.translatable("message.jugcraft.lair.closed"));
				}
				continue;
			}
			present.merge(instance, 1, Integer::sum);
			restrict(player);
			if (!exempt(player)) {
				edges(player, instance);  // an operator in creative mode may fly round the island
			}
		}
		for (LairInstance instance : open(lair)) {
			if (present.getOrDefault(instance, 0) > 0) {
				instance.emptySince = -1L;
				continue;
			}
			if (instance.ended && now - instance.opened > CHECK_TICKS) {
				close(server, instance);
				continue;
			}
			if (instance.emptySince < 0L) {
				instance.emptySince = now;
			} else if (now - instance.emptySince >= EMPTY_SECONDS * 20L) {
				close(server, instance);
			}
		}
	}

	/**
	 * Flown or fallen off: farther than the lair's bounds from the island's centre, or below its floor. The mist throws
	 * the player back to the arrival point, for {@value #EDGE_DAMAGE} damage but never below half a heart.
	 */
	public static boolean edges(ServerPlayer player, LairInstance instance) {
		BlockPos origin = instance.origin();
		Lair lair = instance.lair;
		double dx = player.getX() - origin.getX() - lair.centreX;
		double dz = player.getZ() - origin.getZ() - lair.centreZ;
		boolean off = Math.hypot(dx, dz) > lair.bounds || player.getY() < origin.getY() + lair.floor;
		if (!off) {
			return false;
		}
		Vec3 at = instance.arrival();
		player.teleportTo((ServerLevel) player.level(), at.x, at.y, at.z, Set.of(), lair.arrivalYaw, 0.0F, true);
		player.resetFallDistance();
		// A toll, not a blow: it comes straight off health, whatever would turn a blow aside. Creative and spectator
		// players pay nothing.
		float toll = Math.min(EDGE_DAMAGE, player.getHealth() - 1.0F);
		if (toll > 0.0F && !player.getAbilities().invulnerable) {
			player.setHealth(player.getHealth() - toll);
		}
		player.sendOverlayMessage(Component.translatable("message.jugcraft.lair.edge"));
		return true;
	}

	/** Marks an instance's boss as fallen: it closes once everyone has left (the bosses call this). */
	public static void end(LairInstance instance) {
		instance.ended = true;
	}

	/** Forgets every instance (tests, and the server starting or stopping). */
	public static void reset() {
		INSTANCES.clear();
	}
}
