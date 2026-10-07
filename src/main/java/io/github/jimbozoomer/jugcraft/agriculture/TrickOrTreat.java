package io.github.jimbozoomer.jugcraft.agriculture;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Trick-or-treating, all decided on the server. While the Halloween event runs ({@link HalloweenSeason}), a
 * player using a Candy Bag on a wooden door knocks; {@link #ANSWER_TICKS} later the door is answered if:
 * <ul>
 * <li>it is between dusk and midnight ({@link #DUSK} to {@link #MIDNIGHT} of the overworld day);</li>
 * <li>the player wears a costume on their head (item tag {@code jugcraft:trick_or_treat_costumes}: a carved
 * pumpkin or one of the costume hats), or has a painted face ({@link FacePaint});</li>
 * <li>a porch light burns within {@link #PORCH_RADIUS} blocks of the door (block tag {@code jugcraft:porch_lights},
 * or any lit hand-carved or giant pumpkin);</li>
 * <li>a villager whose home bed is within {@link #HOME_RADIUS} blocks of the door lives there.</li>
 * </ul>
 * Each villager's home hands each player one treat a night (loot table {@value #TREAT_TABLE}; a costume hat
 * sometimes earns a second roll); knocking again there gets a harmless trick instead. Who was given what tonight
 * is saved with the dimension, so a restart, or the event ending and starting again, never gives a treat twice.
 * Ten homes in one night earns the Full Bag advancement. Ending the event only stops answers: treats, costumes
 * and advancements stay.
 */
public final class TrickOrTreat {
	public static final String TREAT_TABLE = "gameplay/trick_or_treat";
	public static final ResourceKey<LootTable> TREATS = ResourceKey.create(Registries.LOOT_TABLE, Jugcraft.id(TREAT_TABLE));
	public static final TagKey<Item> COSTUMES = TagKey.create(Registries.ITEM, Jugcraft.id("trick_or_treat_costumes"));
	public static final TagKey<Item> COSTUME_HATS = TagKey.create(Registries.ITEM, Jugcraft.id("costume_hats"));
	public static final TagKey<Block> PORCH_LIGHTS = TagKey.create(Registries.BLOCK, Jugcraft.id("porch_lights"));
	public static final long DAY = 24000;
	public static final long DUSK = 12000;
	public static final long MIDNIGHT = 18000;
	public static final int ANSWER_TICKS = 30;
	public static final int DOOR_OPEN_TICKS = 40;
	public static final int KNOCK_COOLDOWN = 40;
	public static final int PORCH_RADIUS = 4;
	public static final int HOME_RADIUS = 12;
	public static final int FULL_BAG = 10;
	public static final float COSTUME_BONUS_CHANCE = 0.25F;
	/** At most this many homes are remembered per player and night, and this many players. */
	public static final int MAX_HOMES = 256;
	public static final int MAX_PLAYERS = 1024;

	public enum Result {
		KNOCKED, TREAT, TRICK, OUT_OF_SEASON, WRONG_HOUR, NO_COSTUME, NO_PORCH_LIGHT, NOBODY_HOME, NOT_A_DOOR, BUSY, GONE
	}

	private record Knock(ResourceKey<Level> dimension, BlockPos door, long due) {
	}

	private record OpenDoor(ResourceKey<Level> dimension, BlockPos door, long closeAt) {
	}

	/** One knock waiting for an answer per player, and the doors to close (one per answered knock, so a quick second knock never leaves the first door open). */
	private static final Map<UUID, Knock> KNOCKS = new HashMap<>();
	private static final List<OpenDoor> OPEN_DOORS = new ArrayList<>();

	private TrickOrTreat() {
	}

	static void register() {
		HalloweenSeason.load();
		UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
			ItemStack held = player.getItemInHand(hand);
			if (player.isSpectator() || !held.is(JugcraftAgriculture.item("candy_bag")) || !isWoodenDoor(level.getBlockState(hit.getBlockPos()))) {
				return InteractionResult.PASS;
			}
			if (player.getCooldowns().isOnCooldown(held)) {
				return InteractionResult.FAIL;
			}
			if (player instanceof ServerPlayer server) {
				Result result = knock(server, hit.getBlockPos());
				if (result == Result.KNOCKED) {
					player.getCooldowns().addCooldown(held, KNOCK_COOLDOWN);
				} else {
					message(server, result);
				}
			}
			return InteractionResult.SUCCESS;
		});
		ServerTickEvents.END_SERVER_TICK.register(TrickOrTreat::tick);
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> KNOCKS.remove(handler.getPlayer().getUUID()));
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			KNOCKS.clear();
			OPEN_DOORS.clear();
		});
	}

	static boolean isWoodenDoor(BlockState state) {
		return state.getBlock() instanceof DoorBlock && DoorBlock.isWoodenDoor(state);
	}

	/** Knocks on the wooden door at {@code pos}; the answer comes {@link #ANSWER_TICKS} later. One knock waits per player. */
	public static Result knock(ServerPlayer player, BlockPos pos) {
		ServerLevel level = player.level();
		BlockState state = level.getBlockState(pos);
		if (!isWoodenDoor(state)) {
			return Result.NOT_A_DOOR;
		}
		if (KNOCKS.containsKey(player.getUUID())) {
			return Result.BUSY;
		}
		BlockPos door = state.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos.immutable();
		level.playSound(null, door.above(), SoundEvents.WOOD_HIT, SoundSource.PLAYERS, 1.0F, 0.7F);
		KNOCKS.put(player.getUUID(), new Knock(level.dimension(), door, level.getGameTime() + ANSWER_TICKS));
		return Result.KNOCKED;
	}

	private static void tick(MinecraftServer server) {
		if (KNOCKS.isEmpty() && OPEN_DOORS.isEmpty()) {
			return;
		}
		for (Iterator<Map.Entry<UUID, Knock>> it = KNOCKS.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<UUID, Knock> entry = it.next();
			Knock knock = entry.getValue();
			ServerLevel level = server.getLevel(knock.dimension());
			if (level == null) {
				it.remove();
				continue;
			}
			if (level.getGameTime() < knock.due()) {
				continue;
			}
			it.remove();
			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
			if (player != null) {
				boolean near = player.level() == level && player.distanceToSqr(Vec3.atCenterOf(knock.door())) <= 8.0 * 8.0;
				message(player, near ? answer(player, knock.door(), level.getOverworldClockTime()) : Result.GONE);
			}
		}
		for (Iterator<OpenDoor> it = OPEN_DOORS.iterator(); it.hasNext(); ) {
			OpenDoor open = it.next();
			ServerLevel level = server.getLevel(open.dimension());
			if (level == null || level.getGameTime() >= open.closeAt()) {
				it.remove();
				if (level != null) {
					BlockState state = level.getBlockState(open.door());
					if (isWoodenDoor(state) && state.getValue(DoorBlock.OPEN) && state.getBlock() instanceof DoorBlock door) {
						door.setOpen(null, level, state, open.door(), false);
					}
				}
			}
		}
	}

	private static void message(ServerPlayer player, Result result) {
		if (result != Result.TREAT && result != Result.KNOCKED) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.trick_or_treat." + result.name().toLowerCase()));
		}
	}

	/**
	 * Answers a knock on the door whose lower half is at {@code door}, at {@code dayTime} on the overworld
	 * clock: checks the event, the hour, the costume, the porch light and who lives there, then hands out a
	 * treat or plays a trick. Returns what happened.
	 */
	public static Result answer(ServerPlayer player, BlockPos door, long dayTime) {
		ServerLevel level = player.level();
		// With the agriculture switch off there are no treats, as for every other part of Halloween.
		if (!JugcraftConfig.isFeatureEnabled(JugcraftAgriculture.FEATURE) || !HalloweenSeason.active()) {
			return Result.OUT_OF_SEASON;
		}
		long hour = Math.floorMod(dayTime, DAY);
		if (hour < DUSK || hour >= MIDNIGHT) {
			return Result.WRONG_HOUR;
		}
		BlockState state = level.getBlockState(door);
		if (!isWoodenDoor(state)) {
			return Result.NOT_A_DOOR;
		}
		// A costume on the head, or a painted face (FacePaint).
		if (!FacePaint.inCostume(player)) {
			return Result.NO_COSTUME;
		}
		ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
		if (!porchLight(level, door)) {
			return Result.NO_PORCH_LIGHT;
		}
		long night = night(dayTime);
		Data data = data(level);
		List<Villager> residents = residents(level, door);
		if (residents.isEmpty()) {
			return Result.NOBODY_HOME;
		}
		Villager host = null;
		BlockPos home = null;
		for (Villager villager : residents) {
			BlockPos bed = home(villager, level);
			if (bed != null && !data.visited(night, player.getUUID(), bed)) {
				host = villager;
				home = bed;
				break;
			}
		}
		if (host == null || home == null || !data.record(night, player.getUUID(), home)) {
			trick(player, level, door, residents.getFirst());
			return Result.TRICK;
		}
		treat(player, level, door, host, head);
		CandyBagItem.recordHomes(candyBag(player), night, data.count(night, player.getUUID()));
		if (data.count(night, player.getUUID()) == FULL_BAG) {
			award(player, "full_bag");
		}
		return Result.TREAT;
	}

	/** The number of the night a time on the overworld clock belongs to (a day runs from one sunrise to the next). */
	public static long night(long dayTime) {
		return Math.floorDiv(dayTime, DAY);
	}

	/** Whether a porch light burns near the door: a block in {@link #PORCH_LIGHTS}, or a lit hand-carved or giant pumpkin. Reads at most 486 blocks. */
	public static boolean porchLight(Level level, BlockPos door) {
		for (BlockPos pos : BlockPos.betweenClosed(door.offset(-PORCH_RADIUS, -2, -PORCH_RADIUS), door.offset(PORCH_RADIUS, 3, PORCH_RADIUS))) {
			BlockState state = level.getBlockState(pos);
			if (state.is(PORCH_LIGHTS)
					|| state.getBlock() instanceof CarvedPumpkinBlock && state.getValue(CarvedPumpkinBlock.LIT)
					|| state.getBlock() instanceof GiantPumpkinBlock && state.getValue(GiantPumpkinBlock.LIGHT) > 0) {
				return true;
			}
		}
		return false;
	}

	/** The villagers whose home bed is within {@link #HOME_RADIUS} of the door, nearest bed first. */
	static List<Villager> residents(ServerLevel level, BlockPos door) {
		List<Villager> found = new ArrayList<>(level.getEntitiesOfClass(Villager.class, new AABB(door).inflate(HOME_RADIUS * 2.0), villager -> {
			BlockPos bed = home(villager, level);
			return bed != null && bed.closerThan(door, HOME_RADIUS);
		}));
		found.sort(Comparator.comparingDouble(villager -> home(villager, level).distSqr(door)));
		return found;
	}

	private static @Nullable BlockPos home(Villager villager, ServerLevel level) {
		return villager.getBrain().getMemory(MemoryModuleType.HOME).filter(pos -> pos.dimension() == level.dimension())
				.map(GlobalPos::pos).orElse(null);
	}

	private static void treat(ServerPlayer player, ServerLevel level, BlockPos door, Villager host, ItemStack head) {
		LootTable table = level.getServer().reloadableRegistries().getLootTable(TREATS);
		LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(door))
				.withParameter(LootContextParams.THIS_ENTITY, player).create(LootContextParamSets.GIFT);
		List<ItemStack> treats = new ArrayList<>(table.getRandomItems(params));
		if (head.is(COSTUME_HATS) && level.getRandom().nextFloat() < COSTUME_BONUS_CHANCE) {
			treats.addAll(table.getRandomItems(params));
		}
		ItemStack bag = candyBag(player);
		for (ItemStack stack : treats) {
			player.sendSystemMessage(Component.translatable("message.jugcraft.trick_or_treat.treat", stack.getHoverName(), stack.getCount()));
			CandyBagItem.fill(bag, stack); // Into the bag first; what does not fit goes to the inventory.
			if (!stack.isEmpty() && !player.getInventory().add(stack)) {
				Block.popResource(level, door, stack);
			}
		}
		BlockState state = level.getBlockState(door);
		if (state.getBlock() instanceof DoorBlock doorBlock && !state.getValue(DoorBlock.OPEN)) {
			doorBlock.setOpen(host, level, state, door, true);
			OPEN_DOORS.add(new OpenDoor(level.dimension(), door, level.getGameTime() + DOOR_OPEN_TICKS));
		}
		level.playSound(null, door, SoundEvents.VILLAGER_CELEBRATE, SoundSource.NEUTRAL, 1.0F, 1.0F);
		level.sendParticles(ParticleTypes.HAPPY_VILLAGER, door.getX() + 0.5, door.getY() + 1.5, door.getZ() + 0.5, 8, 0.4, 0.4, 0.4, 0.0);
	}

	/** A harmless prank: a cackle and a hop, bats and a moment of darkness, or the door slammed with a "no". Nothing is taken. */
	private static void trick(ServerPlayer player, ServerLevel level, BlockPos door, Villager host) {
		double x = player.getX();
		double y = player.getY() + 1.0;
		double z = player.getZ();
		switch (level.getRandom().nextInt(3)) {
			case 0 -> {
				level.playSound(null, door, SoundEvents.WITCH_CELEBRATE, SoundSource.NEUTRAL, 1.0F, 1.0F);
				level.sendParticles(ParticleTypes.WITCH, x, y, z, 16, 0.4, 0.6, 0.4, 0.0);
				player.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 10, 1));
			}
			case 1 -> {
				level.playSound(null, door, SoundEvents.BAT_TAKEOFF, SoundSource.NEUTRAL, 1.0F, 1.0F);
				level.sendParticles(ParticleTypes.LARGE_SMOKE, x, y, z, 12, 0.4, 0.6, 0.4, 0.01);
				player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0));
			}
			default -> {
				level.playSound(null, door, SoundEvents.VILLAGER_NO, SoundSource.NEUTRAL, 1.0F, 1.0F);
				level.playSound(null, door, SoundEvents.WOODEN_DOOR_CLOSE, SoundSource.BLOCKS, 1.5F, 0.8F);
				level.sendParticles(ParticleTypes.SMOKE, door.getX() + 0.5, door.getY() + 1.0, door.getZ() + 0.5, 10, 0.3, 0.5, 0.3, 0.01);
			}
		}
	}

	/** Grants a Jugcraft advancement whose one criterion, {@code done}, is only ever granted from code. */
	/** The Candy Bag the player is using: in a hand, else the first in their inventory, else none (an empty stack). */
	public static ItemStack candyBag(ServerPlayer player) {
		Item bag = JugcraftAgriculture.item("candy_bag");
		for (InteractionHand hand : InteractionHand.values()) {
			if (player.getItemInHand(hand).is(bag)) {
				return player.getItemInHand(hand);
			}
		}
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			if (player.getInventory().getItem(slot).is(bag)) {
				return player.getInventory().getItem(slot);
			}
		}
		return ItemStack.EMPTY;
	}

	public static void award(ServerPlayer player, String id) {
		AdvancementHolder advancement = player.level().getServer().getAdvancements().get(Jugcraft.id(id));
		if (advancement != null) {
			player.getAdvancements().award(advancement, "done");
		}
	}

	public static Data data(ServerLevel level) {
		return level.getDataStorage().computeIfAbsent(Data.TYPE);
	}

	/**
	 * Tonight's record (data/jugcraft_trick_or_treat.dat in the dimension): the night number and, for each
	 * player, the home beds that gave them a treat. A new night replaces it.
	 */
	public static final class Data extends SavedData {
		public static final Codec<Data> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.LONG.fieldOf("night").forGetter(data -> data.night),
				Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.LONG.listOf()).fieldOf("visits").forGetter(Data::visits))
				.apply(i, Data::new));
		static final SavedDataType<Data> TYPE = new SavedDataType<>(Jugcraft.id("trick_or_treat"), Data::new, CODEC, null);

		private long night;
		private final Map<UUID, Set<Long>> visits = new HashMap<>();

		Data() {
			this(Long.MIN_VALUE, Map.of());
		}

		Data(long night, Map<UUID, List<Long>> saved) {
			this.night = night;
			saved.entrySet().stream().limit(MAX_PLAYERS)
					.forEach(entry -> visits.put(entry.getKey(), new LinkedHashSet<>(entry.getValue().stream().limit(MAX_HOMES).toList())));
		}

		private Map<UUID, List<Long>> visits() {
			Map<UUID, List<Long>> out = new HashMap<>();
			visits.forEach((player, homes) -> out.put(player, List.copyOf(homes)));
			return out;
		}

		private void turnTo(long tonight) {
			if (tonight != night) {
				night = tonight;
				visits.clear();
				setDirty();
			}
		}

		public boolean visited(long tonight, UUID player, BlockPos home) {
			return tonight == night && visits.getOrDefault(player, Set.of()).contains(home.asLong());
		}

		/** Records a treat from {@code home} for {@code player} tonight; false if already given or the record is full. */
		public boolean record(long tonight, UUID player, BlockPos home) {
			turnTo(tonight);
			Set<Long> homes = visits.get(player);
			if (homes == null) {
				if (visits.size() >= MAX_PLAYERS) {
					return false;
				}
				homes = new LinkedHashSet<>();
				visits.put(player, homes);
			}
			if (homes.size() >= MAX_HOMES || !homes.add(home.asLong())) {
				return false;
			}
			setDirty();
			return true;
		}

		public int count(long tonight, UUID player) {
			return tonight == night ? visits.getOrDefault(player, Set.of()).size() : 0;
		}
	}
}
