package io.github.jimbozoomer.jugcraft.agriculture;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Containers;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * A Pumpkin Chunkin' Trebuchet's state: the pumpkin in its sling, its release angle, and its board of the
 * {@link #BOARD} longest throws (each player once, at their best), plus the players it has given a ribbon to.
 *
 * <p>A throw's speed is {@link #BASE_SPEED} blocks a tick, times the pumpkin's {@link #FACTORS factor} (hollow carved
 * pumpkins are lighter and fly farther, squat heavy ones less far), times a gust of up to {@link #GUST} either way.
 * Its distance is measured on the server, across the ground, from where it left the sling to where it came down
 * ({@link FlyingPumpkin}). Ribbons are the Harvest Scale's: the first time a player places on this board they get
 * that place's ribbon, never again here (the last {@link #REMEMBERED} players are remembered).
 */
public class TrebuchetBlockEntity extends BlockEntity {
	public static final TagKey<Item> AMMO = TagKey.create(Registries.ITEM, Jugcraft.id("trebuchet_ammo"));
	public static final int BOARD = HarvestScaleBlockEntity.BOARD;
	public static final int REMEMBERED = 64;
	public static final double BASE_SPEED = 1.5;
	public static final double GUST = 0.04;
	public static final int MIN_ANGLE = 30;
	public static final int MAX_ANGLE = 60;
	public static final int ANGLE_STEP = 5;
	public static final int DEFAULT_ANGLE = 45;
	/** Throws of at least this many blocks earn the Pumpkin Chunkin' advancement. */
	public static final double ADVANCEMENT_DISTANCE = 50.0;
	/** Speed factors by pumpkin; anything else in {@link #AMMO} flies at 1. */
	public static final Map<String, Double> FACTORS = Map.ofEntries(
			Map.entry("minecraft:pumpkin", 1.0), Map.entry("minecraft:carved_pumpkin", 1.06), Map.entry("minecraft:jack_o_lantern", 1.03),
			Map.entry("jugcraft:white_pumpkin", 1.02), Map.entry("jugcraft:jarrahdale_pumpkin", 0.97), Map.entry("jugcraft:cinderella_pumpkin", 0.95),
			Map.entry("jugcraft:hand_carved_pumpkin", 1.06), Map.entry("jugcraft:hand_carved_white_pumpkin", 1.08),
			Map.entry("jugcraft:hand_carved_jarrahdale_pumpkin", 1.03), Map.entry("jugcraft:hand_carved_cinderella_pumpkin", 1.01),
			Map.entry("jugcraft:red_kuri_pumpkin", 0.99), Map.entry("jugcraft:kabocha_pumpkin", 0.96),
			Map.entry("jugcraft:hand_carved_red_kuri_pumpkin", 1.05), Map.entry("jugcraft:hand_carved_kabocha_pumpkin", 1.02));

	/** One place on the board: a player's longest throw, in tenths of a block. */
	public record Entry(UUID thrower, String name, int tenths) {
		static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
				UUIDUtil.CODEC.fieldOf("thrower").forGetter(Entry::thrower),
				Codec.STRING.fieldOf("name").forGetter(Entry::name),
				Codec.INT.fieldOf("tenths").forGetter(Entry::tenths)).apply(i, Entry::new));
	}

	private ItemStack loaded = ItemStack.EMPTY;
	private int angle = DEFAULT_ANGLE;
	private final List<Entry> board = new ArrayList<>();
	private final Deque<UUID> awarded = new ArrayDeque<>();

	public TrebuchetBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftAgriculture.TREBUCHET_ENTITY, pos, state);
	}

	public ItemStack loaded() {
		return loaded;
	}

	public int angle() {
		return angle;
	}

	public List<Entry> board() {
		return List.copyOf(board);
	}

	public static double factor(ItemStack pumpkin) {
		return FACTORS.getOrDefault(BuiltInRegistries.ITEM.getKey(pumpkin.getItem()).toString(), 1.0);
	}

	/** Puts one pumpkin in the sling. */
	public void load(ItemStack pumpkin) {
		loaded = pumpkin.copyWithCount(1);
		setChanged();
	}

	/** Steps the release angle up by {@link #ANGLE_STEP}, back to {@link #MIN_ANGLE} past the top. Returns the new angle. */
	public int nextAngle() {
		angle = angle + ANGLE_STEP > MAX_ANGLE ? MIN_ANGLE : angle + ANGLE_STEP;
		setChanged();
		return angle;
	}

	/** Where a throw leaves the sling: above the trebuchet, at the front. */
	public Vec3 launchPoint(Direction facing) {
		return Vec3.atBottomCenterOf(worldPosition).add(facing.getStepX() * 0.5, 1.75, facing.getStepZ() * 0.5);
	}

	/** The launch velocity for this angle and pumpkin, with a gust from {@code -1} to {@code 1} (a fraction of {@link #GUST}). */
	public static Vec3 velocity(Direction facing, int angle, ItemStack pumpkin, double gust) {
		double speed = BASE_SPEED * factor(pumpkin) * (1.0 + GUST * Math.max(-1.0, Math.min(1.0, gust)));
		double radians = Math.toRadians(angle);
		return new Vec3(facing.getStepX() * Math.cos(radians), Math.sin(radians), facing.getStepZ() * Math.cos(radians)).scale(speed);
	}

	/** Throws the loaded pumpkin for {@code player}, emptying the sling. Returns the pumpkin in flight, or null if none was loaded. */
	public FlyingPumpkin fire(ServerPlayer player) {
		if (loaded.isEmpty() || !(level instanceof ServerLevel server)) {
			return null;
		}
		Direction facing = getBlockState().getValue(TrebuchetBlock.FACING);
		Vec3 start = launchPoint(facing);
		FlyingPumpkin pumpkin = new FlyingPumpkin(server, start, loaded, worldPosition);
		pumpkin.setOwner(player);
		pumpkin.setDeltaMovement(velocity(facing, angle, loaded, server.getRandom().nextDouble() * 2.0 - 1.0));
		server.addFreshEntity(pumpkin);
		loaded = ItemStack.EMPTY;
		setChanged();
		return pumpkin;
	}

	/**
	 * Records a landed throw of {@code distance} blocks for {@code player}: keeps it on the board if it is one of the
	 * longest (each player once, at their best), hands out a ribbon the first time they place, and tells them.
	 * Returns their place (0 for first) or -1.
	 */
	public int record(ServerPlayer player, double distance) {
		int tenths = (int) Math.round(distance * 10.0);
		UUID id = player.getUUID();
		Entry best = board.stream().filter(entry -> entry.thrower().equals(id)).findFirst().orElse(null);
		if (best == null || tenths > best.tenths()) {
			board.removeIf(entry -> entry.thrower().equals(id));
			board.add(new Entry(id, player.getName().getString(), tenths));
			board.sort(Comparator.comparingInt(Entry::tenths).reversed());
			while (board.size() > BOARD) {
				board.removeLast();
			}
		}
		int place = -1;
		for (int i = 0; i < board.size(); i++) {
			if (board.get(i).thrower().equals(id)) {
				place = i;
			}
		}
		if (place >= 0 && !awarded.contains(id)) {
			awarded.addLast(id);
			while (awarded.size() > REMEMBERED) {
				awarded.removeFirst();
			}
			ItemStack ribbon = new ItemStack(JugcraftAgriculture.item(HarvestScaleBlockEntity.RIBBONS.get(place)));
			player.sendSystemMessage(Component.translatable("message.jugcraft.harvest_scale.ribbon", ribbon.getHoverName()));
			if (!player.getInventory().add(ribbon)) {
				Block.popResource(player.level(), worldPosition.above(), ribbon);
			}
		}
		setChanged();
		return place;
	}

	/** Tells a player the angle and the board. */
	public void show(ServerPlayer player) {
		player.sendSystemMessage(Component.translatable("message.jugcraft.trebuchet.board", angle));
		for (int i = 0; i < board.size(); i++) {
			Entry entry = board.get(i);
			player.sendSystemMessage(Component.translatable("message.jugcraft.trebuchet.place", i + 1, entry.name(), blocks(entry.tenths())));
		}
	}

	/** Tenths of a block as text, such as {@code 54.3}. */
	public static String blocks(int tenths) {
		return tenths / 10 + "." + tenths % 10;
	}

	/** Breaking the trebuchet drops the pumpkin in its sling. */
	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		if (level != null && !loaded.isEmpty()) {
			Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), loaded);
			loaded = ItemStack.EMPTY;
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		loaded = input.read("loaded", ItemStack.CODEC).orElse(ItemStack.EMPTY);
		angle = Math.max(MIN_ANGLE, Math.min(MAX_ANGLE, input.getIntOr("angle", DEFAULT_ANGLE)));
		board.clear();
		input.read("board", Entry.CODEC.listOf()).ifPresent(entries -> board.addAll(entries.stream().limit(BOARD).toList()));
		awarded.clear();
		input.read("awarded", UUIDUtil.CODEC.listOf()).ifPresent(ids -> ids.stream().limit(REMEMBERED).forEach(awarded::addLast));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		if (!loaded.isEmpty()) {
			output.store("loaded", ItemStack.CODEC, loaded);
		}
		output.putInt("angle", angle);
		output.store("board", Entry.CODEC.listOf(), List.copyOf(board));
		output.store("awarded", UUIDUtil.CODEC.listOf(), List.copyOf(awarded));
	}
}
