package io.github.jimbozoomer.jugcraft.agriculture;

import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The master block of a {@link GiantPumpkinBlock}: how far it has grown, what it weighs, whether it was
 * watered lately, what is carved into its four sides and whether a torch is inside.
 *
 * <p>Growing. While its vine still holds it, each random tick gives it 1 growth point, +1 if the vine's
 * farmland is moist and +1 if it was watered within a day (a Gourd Canteen); bone meal gives
 * {@link #BONE_MEAL_POINTS}. At {@link #GROW_TO_TWO} points it swells from 1 to 2 blocks wide, at
 * {@link #GROW_TO_THREE} to 3, if the blocks it needs are free (air, grass, flowers; never water) on
 * ground fruit can lie on; otherwise it waits. It grows away from its vine, centred on the line the vine
 * points along. Full grown, it starts at {@link #START_WEIGHT} kg and puts on {@link #WEIGHT_PER_POINT}
 * kg per point up to {@link #MAX_WEIGHT} kg, until it is carved. Each tick reads at most 3 blocks, a
 * growth step at most 54.
 */
public class GiantPumpkinBlockEntity extends BlockEntity {
	public static final int GROW_TO_TWO = 16;
	public static final int GROW_TO_THREE = 48;
	public static final int BONE_MEAL_POINTS = 4;
	public static final int START_WEIGHT = 100;
	public static final int WEIGHT_PER_POINT = 2;
	public static final int MAX_WEIGHT = 1000;
	/** How long watering lasts: one Minecraft day. */
	public static final long WATERED_TICKS = 24000;
	/** Pixels a side of a full-grown giant pumpkin is carved in. */
	public static final int FACE_SIZE = GiantPumpkinBlock.MAX_SIZE * PumpkinCarving.SIZE;
	private static final Codec<int[]> FACE_CODEC = Codec.INT_STREAM.xmap(stream -> stream.toArray(), Arrays::stream);

	private BlockPos origin = BlockPos.ZERO;
	private Direction toward = Direction.NORTH;
	private int points;
	private int weight;
	private boolean lit;
	private boolean soul;
	private long wateredUntil;
	private UUID id = UUID.randomUUID();
	private final int[][] faces = new int[4][];
	private @Nullable UUID carverId;
	private String carverName = "";

	public GiantPumpkinBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftAgriculture.GIANT_PUMPKIN_ENTITY, pos, state);
		origin = pos;
	}

	/** Sets where the fruit first grew and the way from its vine to it (by the vine, when the fruit appears). */
	public void plant(BlockPos origin, Direction toward) {
		this.origin = origin.immutable();
		this.toward = toward;
		setChanged();
	}

	public int size() {
		return getBlockState().getValue(GiantPumpkinBlock.SIZE);
	}

	public boolean fullGrown() {
		return size() == GiantPumpkinBlock.MAX_SIZE;
	}

	public int points() {
		return points;
	}

	/** What a full-grown pumpkin weighs, in kilograms; 0 while it is still growing. */
	public int weight() {
		return fullGrown() ? weight : 0;
	}

	/** Identifies this pumpkin for the Harvest Scale's board; kept when it grows. */
	public UUID id() {
		return id;
	}

	public BlockPos vinePos() {
		return origin.relative(toward.getOpposite());
	}

	public boolean lit() {
		return lit;
	}

	/** Whether the torch inside is a soul torch. */
	public boolean soul() {
		return lit && soul;
	}

	public boolean carved() {
		for (int[] face : faces) {
			if (face != null && !CarvingFace.isBlank(face)) {
				return true;
			}
		}
		return false;
	}

	/** Whether ticks or bone meal still do anything: growing, or full grown, uncarved and below its heaviest. */
	public boolean canGrow() {
		return !fullGrown() || !carved() && weight < MAX_WEIGHT;
	}

	public boolean watered(Level level) {
		return level.getGameTime() < wateredUntil;
	}

	/** Waters the pumpkin: it grows a point faster per tick for a day. */
	public void water(Level level) {
		wateredUntil = level.getGameTime() + WATERED_TICKS;
		setChanged();
	}

	/** Whether the vine this pumpkin grew from is still there and still holds it. */
	public boolean attached(BlockGetter level) {
		BlockState vine = level.getBlockState(vinePos());
		return vine.getBlock() instanceof AttachedGiantPumpkinVineBlock && vine.getValue(HorizontalDirectionalBlock.FACING) == toward;
	}

	/** A random tick on the master: grow, if the vine still holds the pumpkin. */
	public void tick(ServerLevel level, RandomSource random) {
		if (!attached(level)) {
			return;
		}
		int gain = 1;
		BlockState soil = level.getBlockState(vinePos().below());
		if (soil.hasProperty(BlockStateProperties.MOISTURE) && soil.getValue(BlockStateProperties.MOISTURE) > 0) {
			gain++;
		}
		if (watered(level)) {
			gain++;
		}
		if (HarvestMoon.active()) {
			gain *= 2; // The Harvest Moon swells it twice as fast.
		}
		feed(level, gain, random);
	}

	/** Adds growth points: the pumpkin swells when it has enough, and full grown it gets heavier. */
	public void feed(ServerLevel level, int gain, RandomSource random) {
		int size = size();
		if (size < GiantPumpkinBlock.MAX_SIZE) {
			int need = size == 1 ? GROW_TO_TWO : GROW_TO_THREE;
			points = Math.min(need, points + gain);
			setChanged();
			if (points >= need) {
				expand(level, size + 1, random);
			}
		} else if (!carved()) {
			weight = Math.min(MAX_WEIGHT, weight + gain * WEIGHT_PER_POINT);
			setChanged();
		}
	}

	/**
	 * Grows the pumpkin to {@code size} blocks a side if there is room, moving the master to the new lowest
	 * north-west corner. Returns whether it grew.
	 */
	boolean expand(ServerLevel level, int size, RandomSource random) {
		List<BlockPos> blocks = room(level, size);
		if (blocks == null) {
			return false;
		}
		grow(level, size, blocks, random);
		return true;
	}

	/**
	 * The blocks the pumpkin would fill at {@code size} blocks a side: away from its vine, centred on the line the vine
	 * points along (2 wide: either side of it), or null if something is in the way on every side.
	 */
	private @Nullable List<BlockPos> room(BlockGetter level, int size) {
		Direction side = toward.getClockWise();
		int[][] laterals = size == 2 ? new int[][] {{0, 1}, {-1, 0}} : new int[][] {{-1, 0, 1}};
		for (int[] lateral : laterals) {
			List<BlockPos> blocks = new ArrayList<>();
			boolean room = true;
			for (int k = 0; k < size && room; k++) {
				for (int j : lateral) {
					for (int y = 0; y < size; y++) {
						BlockPos pos = origin.relative(toward, k).relative(side, j).above(y);
						if (!canGrowInto(level, pos, y == 0)) {
							room = false;
						}
						blocks.add(pos);
					}
				}
			}
			if (room) {
				return blocks;
			}
		}
		return null;
	}

	/** Whether the pumpkin has every point it needs to swell to its next size but no room to: feeding it does nothing more. */
	public boolean stuck(BlockGetter level) {
		int size = size();
		return size < GiantPumpkinBlock.MAX_SIZE && points >= (size == 1 ? GROW_TO_TWO : GROW_TO_THREE) && room(level, size + 1) == null;
	}

	private boolean canGrowInto(BlockGetter level, BlockPos pos, boolean bottom) {
		BlockState state = level.getBlockState(pos);
		boolean own = state.getBlock() instanceof GiantPumpkinBlock && GiantPumpkinBlock.masterPos(pos, state).equals(worldPosition);
		if (!own && !(state.isAir() || state.canBeReplaced() && state.getFluidState().isEmpty())) {
			return false;
		}
		if (!bottom) {
			return true;
		}
		BlockPos below = pos.below();
		BlockState ground = level.getBlockState(below);
		return ground.is(BlockTags.SUPPORTS_STEM_FRUIT) || ground.isFaceSturdy(level, below, Direction.UP);
	}

	private void grow(ServerLevel level, int size, List<BlockPos> blocks, RandomSource random) {
		int minX = Integer.MAX_VALUE;
		int minY = Integer.MAX_VALUE;
		int minZ = Integer.MAX_VALUE;
		for (BlockPos pos : blocks) {
			minX = Math.min(minX, pos.getX());
			minY = Math.min(minY, pos.getY());
			minZ = Math.min(minZ, pos.getZ());
		}
		BlockPos master = new BlockPos(minX, minY, minZ);
		if (size == GiantPumpkinBlock.MAX_SIZE) {
			weight = START_WEIGHT + random.nextInt(21);
		}
		Block block = getBlockState().getBlock();
		BlockPos oldMaster = worldPosition;
		for (BlockPos pos : blocks) {
			int part = GiantPumpkinBlock.part(size, pos.getX() - minX, pos.getY() - minY, pos.getZ() - minZ);
			level.setBlock(pos, block.defaultBlockState().setValue(GiantPumpkinBlock.SIZE, size).setValue(GiantPumpkinBlock.PART, part),
					Block.UPDATE_ALL);
		}
		if (level.getBlockEntity(master) instanceof GiantPumpkinBlockEntity moved && moved != this) {
			moved.copyFrom(this);
			level.removeBlockEntity(oldMaster);
		}
		level.playSound(null, master, SoundEvents.BIG_DRIPLEAF_TILT_UP, SoundSource.BLOCKS, 1.0F, 0.6F);
	}

	private void copyFrom(GiantPumpkinBlockEntity other) {
		origin = other.origin;
		toward = other.toward;
		points = other.points;
		weight = other.weight;
		lit = other.lit;
		soul = other.soul;
		wateredUntil = other.wateredUntil;
		id = other.id;
		for (int i = 0; i < faces.length; i++) {
			faces[i] = other.faces[i] == null ? null : other.faces[i].clone();
		}
		carverId = other.carverId;
		carverName = other.carverName;
		setChanged();
	}

	/** What is carved into one side: a 48x48 face ({@link CarvingFace}), all skin if nothing is. */
	public int[] face(Direction side) {
		int[] face = faces[side.get2DDataValue()];
		return face == null ? new int[CarvingFace.ints(FACE_SIZE)] : face.clone();
	}

	/** Carves one side (server side), records the carver, and keeps the light right if a torch is inside. */
	public void setFace(Direction side, int[] face, @Nullable Player carver) {
		faces[side.get2DDataValue()] = face.clone();
		if (carver != null) {
			carverId = carver.getUUID();
			carverName = carver.getName().getString();
		}
		setChanged();
		if (lit) {
			setLit(true);
		} else if (level != null) {
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
		}
	}

	/**
	 * How brightly a candle-lit giant pumpkin glows: nothing uncarved, then 4 plus a level for every 27
	 * holes and every 108 shaved pixels (a normal pumpkin's rule for a face 9 times as big), up to 15.
	 */
	public int glow() {
		int cut = 0;
		int shaved = 0;
		for (int[] face : faces) {
			if (face != null) {
				cut += CarvingFace.count(face, FACE_SIZE, PumpkinCarving.CUT);
				shaved += CarvingFace.count(face, FACE_SIZE, PumpkinCarving.SHAVED);
			}
		}
		return cut + shaved == 0 ? 0 : Math.min(15, 4 + cut / 27 + shaved / 108);
	}

	/** Puts a torch in (or takes it out), keeping the kind of torch already inside. */
	public void setLit(boolean lit) {
		setLit(lit, soul);
	}

	/**
	 * Puts a torch or a soul torch in (or takes it out): every block of the pumpkin gives the carving's glow (a soul
	 * torch's light at most), or none.
	 */
	public void setLit(boolean lit, boolean soul) {
		this.lit = lit;
		this.soul = lit && soul;
		setChanged();
		if (level == null) {
			return;
		}
		int light = lit ? CarvedPumpkinBlock.flameLight(glow(), this.soul) : 0;
		BlockState master = getBlockState();
		int size = master.getValue(GiantPumpkinBlock.SIZE);
		for (int part = 0; part < size * size * size; part++) {
			int[] offset = GiantPumpkinBlock.offset(size, part);
			BlockPos pos = worldPosition.offset(offset[0], offset[1], offset[2]);
			BlockState state = level.getBlockState(pos);
			if (state.getBlock() instanceof GiantPumpkinBlock && state.getValue(GiantPumpkinBlock.LIGHT) != light) {
				level.setBlock(pos, state.setValue(GiantPumpkinBlock.LIGHT, light), Block.UPDATE_ALL);
			}
		}
		level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		origin = input.read("origin", BlockPos.CODEC).orElse(worldPosition);
		toward = input.read("toward", Direction.CODEC).orElse(Direction.NORTH);
		points = input.getIntOr("points", 0);
		weight = input.getIntOr("weight", 0);
		lit = input.getBooleanOr("lit", false);
		soul = input.getBooleanOr("soul", false);
		wateredUntil = input.getLongOr("watered_until", 0L);
		id = input.read("id", UUIDUtil.CODEC).orElse(id);
		for (Direction side : Direction.Plane.HORIZONTAL) {
			faces[side.get2DDataValue()] = input.read("carving_" + side.getName(), FACE_CODEC)
					.filter(face -> CarvingFace.isValid(face, FACE_SIZE)).orElse(null);
		}
		carverId = input.read("carved_by", UUIDUtil.CODEC).orElse(null);
		carverName = input.getStringOr("carved_by_name", "");
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.store("origin", BlockPos.CODEC, origin);
		output.store("toward", Direction.CODEC, toward);
		output.putInt("points", points);
		output.putInt("weight", weight);
		output.putBoolean("lit", lit);
		if (soul) {
			output.putBoolean("soul", true);
		}
		output.putLong("watered_until", wateredUntil);
		output.store("id", UUIDUtil.CODEC, id);
		for (Direction side : Direction.Plane.HORIZONTAL) {
			int[] face = faces[side.get2DDataValue()];
			if (face != null && !CarvingFace.isBlank(face)) {
				output.store("carving_" + side.getName(), FACE_CODEC, face);
			}
		}
		if (carverId != null) {
			output.store("carved_by", UUIDUtil.CODEC, carverId);
			output.putString("carved_by_name", carverName);
		}
	}

	/** This pumpkin as an item carries it (see {@link GiantPumpkinData}). */
	public GiantPumpkinData data() {
		Map<Direction, int[]> carving = new EnumMap<>(Direction.class);
		for (Direction side : Direction.Plane.HORIZONTAL) {
			int[] face = faces[side.get2DDataValue()];
			if (face != null) {
				carving.put(side, face);
			}
		}
		return new GiantPumpkinData(size(), points, weight, Optional.of(id), lit, carving, Optional.ofNullable(carverId), carverName, soul());
	}

	@Override
	protected void collectImplicitComponents(DataComponentMap.Builder components) {
		super.collectImplicitComponents(components);
		components.set(JugcraftAgriculture.GIANT_PUMPKIN, data());
	}

	/**
	 * Takes on a pumpkin carried as an item. Its size is the block's own; lighting the blocks for a torch inside waits
	 * until every block is placed ({@link GiantPumpkinBlock#setPlacedBy}).
	 */
	@Override
	protected void applyImplicitComponents(DataComponentGetter components) {
		super.applyImplicitComponents(components);
		GiantPumpkinData data = components.get(JugcraftAgriculture.GIANT_PUMPKIN);
		if (data == null) {
			return;
		}
		points = data.points();
		weight = data.weight();
		lit = data.lit();
		soul = data.lit() && data.soul();
		data.id().ifPresent(value -> id = value);
		for (Direction side : Direction.Plane.HORIZONTAL) {
			int[] face = data.faces().get(side);
			faces[side.get2DDataValue()] = face == null ? null : face.clone();
		}
		carverId = data.carverId().orElse(null);
		carverName = data.carverName();
	}

	@Override
	public void removeComponentsFromTag(ValueOutput output) {
		super.removeComponentsFromTag(output);
		for (String key : List.of("points", "weight", "lit", "soul", "id", "carved_by", "carved_by_name")) {
			output.discard(key);
		}
		for (Direction side : Direction.Plane.HORIZONTAL) {
			output.discard("carving_" + side.getName());
		}
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		// Clients need the carving and whether it is lit, not who carved it.
		CompoundTag tag = saveCustomOnly(registries);
		tag.remove("carved_by");
		tag.remove("carved_by_name");
		return tag;
	}
}
