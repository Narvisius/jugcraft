package io.github.jimbozoomer.jugcraft.agriculture;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Trick-or-treaters at your door, all on the server, while the Halloween event runs ({@link HalloweenSeason}), between
 * dusk and midnight. Every {@value #CHECK_TICKS} ticks each Candy Bowl (not a cache) by a wooden door with a porch
 * light ({@link TrickOrTreat#porchLight}), with a player within {@value #PLAYER_RANGE} blocks, has one chance in
 * {@value #CHANCE} of a visit, at most {@value #MAX_GROUPS} a night. A group of {@value #MIN_KIDS} to
 * {@value #MAX_KIDS} village children in costume comes from {@value #MIN_DISTANCE} to {@value #MAX_DISTANCE} blocks
 * away, walks up to the bowl, knocks and waits {@value #WAIT_TICKS} ticks:
 * <ul>
 * <li>each child takes a treat from the bowl and leaves a thank-you gift (loot table {@value #GIFT_TABLE});</li>
 * <li>if the bowl runs out, they toilet-paper up to {@value #STREAMERS} spots within {@value #PRANK_REACH} blocks of
 * the door instead ({@link ToiletPaperStreamerBlock#drape}).</li>
 * </ul>
 * Then they go back the way they came and are gone. They give up after {@value #GIVE_UP_TICKS} ticks if they can't
 * get there. The children can't be hurt and drop nothing; they are never saved (any found when the world loads go
 * home at once).
 */
public final class TrickOrTreaters {
	public static final int CHECK_TICKS = 200;
	public static final int CHANCE = 4;
	public static final int MAX_GROUPS = 6;
	public static final int MIN_KIDS = 1;
	public static final int MAX_KIDS = 3;
	public static final int MIN_DISTANCE = 12;
	public static final int MAX_DISTANCE = 20;
	public static final int PLAYER_RANGE = 48;
	public static final int GIVE_UP_TICKS = 1200;
	public static final int WAIT_TICKS = 60;
	public static final int LEAVE_TICKS = 300;
	public static final int PRANK_REACH = 8;
	public static final int STREAMERS = 6;
	public static final String GIFT_TABLE = "gameplay/trick_or_treater_thanks";
	public static final ResourceKey<LootTable> GIFTS = ResourceKey.create(Registries.LOOT_TABLE, Jugcraft.id(GIFT_TABLE));
	public static final List<String> COSTUMES = List.of("minecraft:carved_pumpkin", "jugcraft:witch_hat", "jugcraft:scarecrow_hat",
			"jugcraft:ghost_sheet");
	/** Entity tag of a trick-or-treater. */
	public static final String TAG = "jugcraft.trick_or_treater";
	/** How near the bowl a child must come to knock. */
	public static final double ARRIVED = 2.5;
	private static final int STEP = 10;

	public enum Phase {
		COMING, WAITING, LEAVING
	}

	/** A group on its way, at the door or going home. */
	public static final class Visit {
		final ResourceKey<Level> dimension;
		public final BlockPos bowl;
		public final BlockPos door;
		final BlockPos from;
		final List<UUID> kids = new ArrayList<>();
		Phase phase = Phase.COMING;
		long until;
		int treated;
		boolean pranked;

		Visit(ResourceKey<Level> dimension, BlockPos bowl, BlockPos door, BlockPos from) {
			this.dimension = dimension;
			this.bowl = bowl;
			this.door = door;
			this.from = from;
		}

		public Phase phase() {
			return phase;
		}

		public List<UUID> kids() {
			return List.copyOf(kids);
		}

		/** How many children took a treat. */
		public int treated() {
			return treated;
		}

		/** Whether they found the bowl empty and papered the trees. */
		public boolean pranked() {
			return pranked;
		}
	}

	private static final List<Visit> VISITS = new ArrayList<>();

	private TrickOrTreaters() {
	}

	static void register() {
		ServerTickEvents.END_SERVER_TICK.register(TrickOrTreaters::tick);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> VISITS.clear());
		// The children can't be hurt (only what passes through invulnerability, such as the void or /kill, harms them).
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> !entity.entityTags().contains(TAG)
				|| source.is(DamageTypeTags.BYPASSES_INVULNERABILITY));
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (entity.entityTags().contains(TAG) && VISITS.stream().noneMatch(visit -> visit.kids.contains(entity.getUUID()))) {
				entity.discard();
			}
		});
	}

	public static List<Visit> visits() {
		return List.copyOf(VISITS);
	}

	private static boolean visiting(ServerLevel level, BlockPos bowl) {
		return VISITS.stream().anyMatch(visit -> visit.dimension == level.dimension() && visit.bowl.equals(bowl));
	}

	/** Whether it is trick-or-treating time: the event runs, between dusk and midnight on the overworld clock. */
	public static boolean tonight(long dayTime) {
		long hour = Math.floorMod(dayTime, TrickOrTreat.DAY);
		return HalloweenSeason.active() && hour >= TrickOrTreat.DUSK && hour < TrickOrTreat.MIDNIGHT;
	}

	/** The lower half of the nearest wooden door within the porch's reach of {@code bowl}, or null. Reads 405 blocks. */
	public static @Nullable BlockPos doorNear(Level level, BlockPos bowl) {
		int r = TrickOrTreat.PORCH_RADIUS;
		BlockPos best = null;
		for (BlockPos pos : BlockPos.betweenClosed(bowl.offset(-r, -2, -r), bowl.offset(r, 2, r))) {
			BlockState state = level.getBlockState(pos);
			if (TrickOrTreat.isWoodenDoor(state) && state.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER
					&& (best == null || pos.distSqr(bowl) < best.distSqr(bowl))) {
				best = pos.immutable();
			}
		}
		return best;
	}

	/** A Candy Bowl's regular look ({@link CandyBowlBlockEntity}): perhaps sends a group of trick-or-treaters to it. */
	static @Nullable Visit maybeVisit(ServerLevel level, BlockPos bowlPos, CandyBowlBlockEntity bowl) {
		if (!JugcraftConfig.isFeatureEnabled(JugcraftAgriculture.FEATURE) || !tonight(level.getOverworldClockTime()) || visiting(level, bowlPos)) {
			return null;
		}
		long night = TrickOrTreat.night(level.getOverworldClockTime());
		if (bowl.groups(night) >= MAX_GROUPS || !playerNear(level, bowlPos)) {
			return null;
		}
		BlockPos door = doorNear(level, bowlPos);
		if (door == null || !TrickOrTreat.porchLight(level, door) || level.getRandom().nextInt(CHANCE) != 0) {
			return null;
		}
		BlockPos from = startFrom(level, bowlPos, level.getRandom());
		if (from == null) {
			return null;
		}
		bowl.recordGroup(night);
		return send(level, bowlPos, door, from, MIN_KIDS + level.getRandom().nextInt(MAX_KIDS - MIN_KIDS + 1));
	}

	private static boolean playerNear(ServerLevel level, BlockPos pos) {
		for (ServerPlayer player : level.players()) {
			if (!player.isSpectator() && player.distanceToSqr(Vec3.atCenterOf(pos)) <= (double) PLAYER_RANGE * PLAYER_RANGE) {
				return true;
			}
		}
		return false;
	}

	/** Somewhere out of the way to come from: open ground {@value #MIN_DISTANCE} to {@value #MAX_DISTANCE} blocks off. */
	static @Nullable BlockPos startFrom(ServerLevel level, BlockPos bowl, RandomSource random) {
		for (int attempt = 0; attempt < 16; attempt++) {
			double angle = random.nextDouble() * Math.PI * 2;
			int distance = MIN_DISTANCE + random.nextInt(MAX_DISTANCE - MIN_DISTANCE + 1);
			int x = bowl.getX() + (int) Math.round(Math.cos(angle) * distance);
			int z = bowl.getZ() + (int) Math.round(Math.sin(angle) * distance);
			BlockPos column = new BlockPos(x, bowl.getY(), z);
			if (!level.isLoaded(column)) {
				continue;
			}
			BlockPos pos = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
			if (Math.abs(pos.getY() - bowl.getY()) <= 6 && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)
					&& level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
					&& level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()) {
				return pos;
			}
		}
		return null;
	}

	/** Sends {@code count} trick-or-treaters from {@code from} to the bowl at {@code bowl} by {@code door}. */
	public static Visit send(ServerLevel level, BlockPos bowl, BlockPos door, BlockPos from, int count) {
		Visit visit = new Visit(level.dimension(), bowl.immutable(), door.immutable(), from.immutable());
		visit.until = level.getGameTime() + GIVE_UP_TICKS;
		VISITS.add(visit);
		RandomSource random = level.getRandom();
		for (int i = 0; i < count; i++) {
			if (!(BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace("villager")).create(level, EntitySpawnReason.EVENT)
					instanceof Villager kid)) {
				continue;
			}
			kid.setAge(-24000);
			kid.snapTo(from.getX() + 0.5 + (i - 1) * 0.6, from.getY(), from.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
			String costume = COSTUMES.get(random.nextInt(COSTUMES.size()));
			kid.setItemSlot(EquipmentSlot.HEAD, new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(costume))));
			kid.setDropChance(EquipmentSlot.HEAD, 0.0F);
			kid.addTag(TAG);
			visit.kids.add(kid.getUUID());
			if (!level.addFreshEntity(kid)) {
				visit.kids.remove(kid.getUUID());
			}
		}
		return visit;
	}

	private static void tick(MinecraftServer server) {
		if (VISITS.isEmpty() || server.getTickCount() % STEP != 0) {
			return;
		}
		for (Iterator<Visit> it = VISITS.iterator(); it.hasNext();) {
			Visit visit = it.next();
			ServerLevel level = server.getLevel(visit.dimension);
			if (level == null || step(level, visit)) {
				if (level != null) {
					// A visiting child may have claimed a bed or work place on the way: give it back before it goes.
					kids(level, visit).forEach(kid -> {
						releasePlaces(server, kid);
						kid.discard();
					});
				}
				it.remove();
			}
		}
	}

	/** Frees the bed, work place and meeting point a child claimed (Villager's own release is private). */
	private static void releasePlaces(MinecraftServer server, Villager kid) {
		for (MemoryModuleType<GlobalPos> memory : List.of(MemoryModuleType.HOME, MemoryModuleType.JOB_SITE,
				MemoryModuleType.POTENTIAL_JOB_SITE, MemoryModuleType.MEETING_POINT)) {
			kid.getBrain().getMemory(memory).ifPresent(place -> {
				ServerLevel level = server.getLevel(place.dimension());
				// Only a place that is still there (a broken bed's claim is gone with it).
				if (level != null && level.getPoiManager().getType(place.pos()).isPresent()) {
					level.getPoiManager().release(place.pos());
				}
			});
		}
	}

	static List<Villager> kids(ServerLevel level, Visit visit) {
		List<Villager> kids = new ArrayList<>();
		visit.kids.removeIf(id -> !(level.getEntity(id) instanceof Villager kid && kid.isAlive()));
		for (UUID id : visit.kids) {
			if (level.getEntity(id) instanceof Villager kid) {
				kids.add(kid);
			}
		}
		return kids;
	}

	private static void walk(Villager kid, BlockPos to) {
		kid.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(to, 0.6F, 1));
	}

	/** Moves a visit on; returns whether it is over. */
	static boolean step(ServerLevel level, Visit visit) {
		List<Villager> kids = kids(level, visit);
		if (kids.isEmpty()) {
			return true;
		}
		long now = level.getGameTime();
		Vec3 bowl = Vec3.atCenterOf(visit.bowl);
		switch (visit.phase) {
			case COMING -> {
				kids.forEach(kid -> walk(kid, visit.bowl));
				if (kids.stream().anyMatch(kid -> kid.distanceToSqr(bowl) <= ARRIVED * ARRIVED)) {
					level.playSound(null, visit.door.above(), SoundEvents.WOOD_HIT, SoundSource.NEUTRAL, 1.0F, 0.7F);
					level.playSound(null, visit.bowl, SoundEvents.VILLAGER_AMBIENT, SoundSource.NEUTRAL, 1.0F, 1.6F);
					tell(level, visit.door, Component.translatable("message.jugcraft.trick_or_treaters.knock"));
					visit.phase = Phase.WAITING;
					visit.until = now + WAIT_TICKS;
				} else if (now >= visit.until) {
					leave(visit, now);
				}
			}
			case WAITING -> {
				if (now >= visit.until) {
					answer(level, visit, kids);
					leave(visit, now);
				}
			}
			case LEAVING -> {
				kids.forEach(kid -> walk(kid, visit.from));
				Vec3 home = Vec3.atBottomCenterOf(visit.from);
				for (Villager kid : kids) {
					if (now >= visit.until || kid.distanceToSqr(home) <= 4.0) {
						level.sendParticles(ParticleTypes.POOF, kid.getX(), kid.getY() + 0.5, kid.getZ(), 6, 0.2, 0.3, 0.2, 0.01);
						kid.discard();
					}
				}
				return kids(level, visit).isEmpty();
			}
		}
		return false;
	}

	private static void leave(Visit visit, long now) {
		visit.phase = Phase.LEAVING;
		visit.until = now + LEAVE_TICKS;
	}

	/** The door is answered: each child takes a treat and leaves a gift; if the bowl runs out, the trees get papered. */
	static void answer(ServerLevel level, Visit visit, List<Villager> kids) {
		CandyBowlBlockEntity bowl = level.getBlockEntity(visit.bowl) instanceof CandyBowlBlockEntity found ? found : null;
		LootTable gifts = level.getServer().reloadableRegistries().getLootTable(GIFTS);
		for (Villager kid : kids) {
			ItemStack treat = bowl == null ? ItemStack.EMPTY : bowl.handOut();
			if (treat.isEmpty()) {
				visit.pranked = true;
				continue;
			}
			kid.setItemInHand(InteractionHand.MAIN_HAND, treat);
			LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(visit.bowl))
					.withParameter(LootContextParams.THIS_ENTITY, kid).create(LootContextParamSets.GIFT);
			gifts.getRandomItems(params).forEach(gift -> Block.popResource(level, visit.bowl.above(), gift));
			visit.treated++;
		}
		if (visit.treated > 0) {
			level.playSound(null, visit.bowl, SoundEvents.VILLAGER_CELEBRATE, SoundSource.NEUTRAL, 1.0F, 1.5F);
			level.sendParticles(ParticleTypes.HAPPY_VILLAGER, visit.bowl.getX() + 0.5, visit.bowl.getY() + 1.0, visit.bowl.getZ() + 0.5, 8, 0.6, 0.4,
					0.6, 0.0);
			tell(level, visit.door, Component.translatable("message.jugcraft.trick_or_treaters.thanks"));
		}
		if (visit.pranked) {
			ToiletPaperStreamerBlock.drape(level, visit.door, PRANK_REACH, STREAMERS, level.getRandom());
			level.playSound(null, visit.bowl, SoundEvents.WITCH_CELEBRATE, SoundSource.NEUTRAL, 1.0F, 1.8F);
			tell(level, visit.door, Component.translatable("message.jugcraft.trick_or_treaters.prank"));
		}
	}

	private static void tell(ServerLevel level, BlockPos door, Component message) {
		for (ServerPlayer player : level.players()) {
			if (player.distanceToSqr(Vec3.atCenterOf(door)) <= (double) PLAYER_RANGE * PLAYER_RANGE) {
				player.sendOverlayMessage(message);
			}
		}
	}
}
