package io.github.jimbozoomer.jugcraft.nuclear;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.tower.FurnitureBlock;
import io.github.jimbozoomer.jugcraft.tower.SeatEntity;
import io.github.jimbozoomer.jugcraft.tower.Sittable;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The control building's restroom (round 26, #14; Justin: "2 stalls, urinal, 2 sinks", every fixture working, the
 * toilet paper and the trash can working, ceramic tile). The fixtures with states of their own; their models and
 * blockstates are written by tools/restroom.py (the pieces facing north, the wall at z 0), their sounds by
 * tools/restroom_sounds.py.
 *
 * <ul>
 * <li>Toilet: right-click the flush valve to flush it (the water churns, the handle goes down, for three seconds);
 * right-click the paper dispenser for a Toilet Paper Roll (eight in it, one back each random tick, about a minute);
 * anywhere else to sit down.</li>
 * <li>Urinal: right-click to flush.</li>
 * <li>Sink and hand dryer: right-click to run them (five and four seconds), the water or the air, the sensor's lamp.</li>
 * <li>Trash can: right-click to open it; whatever is in it when you close it is gone. It fills up (paper towels to a
 * third, two thirds, the top) and is emptied on the custodian's morning round (06:00 to 10:00 game time).</li>
 * <li>Stall doors: open and close by hand; closing one from inside the stall latches it (OCCUPIED: it will not open
 * from outside); right-click from inside to unlatch and open. A latched door with nobody inside unlatches itself.</li>
 * <li>Shower stall (work list item 22, the Tri-Pillar's locker room; tools/shower_stall.py): right-click to turn it on
 * (the lever swings up, the water falls and churns on the tray, the spray's sound), again to turn it off; it shuts itself
 * off after a minute. Standing under it puts out a fire.</li>
 * </ul>
 */
public final class RestroomBlocks {
	public static SoundEvent TOILET_FLUSH, URINAL_FLUSH, FAUCET, HAND_DRYER, SHOWER;

	private RestroomBlocks() {
	}

	public static void register() {
		TOILET_FLUSH = sound("restroom.toilet_flush", 16);
		URINAL_FLUSH = sound("restroom.urinal_flush", 16);
		FAUCET = sound("restroom.faucet", 12);
		HAND_DRYER = sound("restroom.hand_dryer", 16);
		SHOWER = sound("restroom.shower", 12);
		BlockBehaviour.Properties porcelain = BlockBehaviour.Properties.ofFullCopy(Blocks.CALCITE).strength(1.5F).noOcclusion();
		BlockBehaviour.Properties metal = BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(1.5F).noOcclusion();
		block("restroom_toilet", Toilet::new, porcelain);
		block("restroom_urinal", Urinal::new, porcelain);
		block("restroom_sink", p -> new Running(p, Running.SINK), porcelain);
		block("restroom_hand_dryer", p -> new Running(p, Running.DRYER), metal);
		block("restroom_trash_can", TrashCan::new, metal);
		block("shower_stall", Shower::new, BlockBehaviour.Properties.ofFullCopy(Blocks.STONE).strength(1.5F, 6F).noOcclusion());
		for (String id : new String[] {"restroom_stall_door", "restroom_stall_door_accessible"}) {
			ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Jugcraft.id(id));
			Block door = net.minecraft.core.Registry.register(BuiltInRegistries.BLOCK, key,
					new StallDoor(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_DOOR).strength(2.0F).noOcclusion().setId(key)));
			ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Jugcraft.id(id));
			net.minecraft.core.Registry.register(BuiltInRegistries.ITEM, itemKey,
					new DoubleHighBlockItem(door, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));
			PlantInterior.BLOCKS.put(id, door);
		}
		BlockBehaviour.Properties tile = BlockBehaviour.Properties.ofFullCopy(Blocks.STONE).strength(1.5F, 6F);
		block("restroom_wall_tile", TileWall::new, tile);
		block("restroom_wall_tile_cap", TileWall::new, tile);
		block("restroom_floor_tile", Block::new, tile);
	}

	private static SoundEvent sound(String name, float range) {
		return net.minecraft.core.Registry.register(BuiltInRegistries.SOUND_EVENT, Jugcraft.id(name),
				SoundEvent.createFixedRangeEvent(Jugcraft.id(name), range));
	}

	private static void block(String name, java.util.function.Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties properties) {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Jugcraft.id(name));
		Block block = net.minecraft.core.Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(properties.setId(key)));
		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Jugcraft.id(name));
		net.minecraft.core.Registry.register(BuiltInRegistries.ITEM, itemKey, new BlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));
		PlantInterior.BLOCKS.put(name, block);
	}

	/** A point of the block (pixels) turned back to the model's frame (facing north): the inverse of the blockstate's turn. */
	static double[] model(BlockPos pos, Vec3 at, Direction facing) {
		double x = (at.x - pos.getX()) * 16, y = (at.y - pos.getY()) * 16, z = (at.z - pos.getZ()) * 16;
		int turns = switch (facing) {
			case EAST -> 1;
			case SOUTH -> 2;
			case WEST -> 3;
			default -> 0;
		};
		for (int i = 0; i < turns; i++) {
			double nx = z, nz = 16 - x;
			x = nx;
			z = nz;
		}
		return new double[] {x, y, z};
	}

	/** Particles at a point of the model (pixels, facing north), as the blockstate turns it. */
	static void particles(ServerLevel level, BlockPos pos, Direction facing, double x, double y, double z, ParticleOptions type, int count,
			double spread, double speed) {
		double[] w = EyewashBlock.world(new double[] {x, y, z}, facing);
		level.sendParticles(type, pos.getX() + w[0], pos.getY() + w[1], pos.getZ() + w[2], count, spread, spread, spread, speed);
	}

	/** Each level's fixtures running or flushing: until when (game time). */
	private static final Map<Level, Map<BlockPos, Long>> UNTIL = new WeakHashMap<>();

	static long until(Level level, BlockPos pos) {
		Long end = UNTIL.getOrDefault(level, Map.of()).get(pos);
		return end == null ? -1 : end;
	}

	public static void runFor(Level level, BlockPos pos, int ticks) {
		UNTIL.computeIfAbsent(level, k -> new HashMap<>()).put(pos.immutable(), level.getGameTime() + ticks);
	}

	static void done(Level level, BlockPos pos) {
		UNTIL.getOrDefault(level, new HashMap<>()).remove(pos);
	}

	// ------------------------------------------------------------------------------------------------ the toilet
	public static final IntegerProperty VARIANT = IntegerProperty.create("variant", 0, 1);
	public static final int PAPER_MAX = 8;
	public static final IntegerProperty PAPER = IntegerProperty.create("paper", 0, PAPER_MAX);
	public static final BooleanProperty FLUSHING = BooleanProperty.create("flushing");
	public static final BooleanProperty RUNNING = BooleanProperty.create("running");
	public static final IntegerProperty FILL = IntegerProperty.create("fill", 0, 3);
	/**
	 * The stall partition panel standing along the toilet's left side as you face it (two stalls side by side, the panel
	 * between them running from the back wall through the toilet's block): restroom_stall_partition's wall-end variant.
	 */
	public static final BooleanProperty PARTITION = BooleanProperty.create("partition");

	/** A commercial toilet with its flush valve and toilet paper dispenser (variant 1: the accessible one). */
	public static class Toilet extends FurnitureBlock implements Sittable {
		public static final int FLUSH = 60;
		/** The partition panel's box, facing north (along the west edge: restroom_stall_partition's turned a quarter). */
		static final double[] PANEL = {0.3, 0, 0, 0.8, 24, 16};
		private final Map<Direction, VoxelShape> withPanel = new java.util.EnumMap<>(Direction.class);

		public Toilet(Properties properties) {
			super(properties, new double[] {5, 0, 1, 12.1, 7.7, 13.4}, new double[] {4.4, 9.5, 0, 10.2, 14.4, 3}, new double[] {13.5, 8.4, 3.4, 16, 12.8, 12.2});
			registerDefaultState(defaultBlockState().setValue(VARIANT, 0).setValue(PAPER, PAPER_MAX).setValue(FLUSHING, false).setValue(PARTITION, false));
			for (Direction facing : Direction.Plane.HORIZONTAL) {
				BlockState state = defaultBlockState().setValue(FACING, facing);
				withPanel.put(facing, Shapes.or(super.getShape(state, null, BlockPos.ZERO, null), FurnitureBlock.turned(PANEL, facing)).optimize());
			}
		}

		@Override
		protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
			super.createBlockStateDefinition(builder);
			builder.add(VARIANT, PAPER, FLUSHING, PARTITION);
		}

		@Override
		protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext context) {
			return state.getValue(PARTITION) ? withPanel.get(state.getValue(FACING)) : super.getShape(state, level, pos, context);
		}

		/** A toilet partition used on a toilet stands it along the toilet's left side (it drops again with the toilet). */
		@Override
		protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, net.minecraft.world.InteractionHand hand,
				BlockHitResult hit) {
			if (state.getValue(PARTITION) || !stack.is(BuiltInRegistries.ITEM.getValue(Jugcraft.id("restroom_stall_partition")))) {
				return InteractionResult.TRY_WITH_EMPTY_HAND;
			}
			if (!level.isClientSide()) {
				level.setBlock(pos, state.setValue(PARTITION, true), Block.UPDATE_ALL);
				stack.consume(1, player);
				level.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.8F, 0.9F);
			}
			return InteractionResult.SUCCESS;
		}

		@Override
		public double seatHeight() {
			return 7.1 / 16;
		}

		/** What a click at this point of the toilet does: "valve", "paper" or "seat". */
		public static String part(BlockState state, BlockPos pos, Vec3 at) {
			double[] m = model(pos, at, state.getValue(FACING));
			double up = state.getValue(VARIANT) == 1 ? 0.6 : 0;
			double cx = state.getValue(VARIANT) == 1 ? 9 : 8;
			if (m[0] > cx - 4 && m[0] < cx + 1.3 && m[1] > 8.6 + up && m[1] < 14.2 + up && m[2] < 6) {
				return "valve";
			}
			if (m[0] > 13.2 && m[1] > 7.0 && m[1] < 12.9 && m[2] > 3.0 && m[2] < 12.6) {
				return "paper";
			}
			return "seat";
		}

		@Override
		protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
			if (level.isClientSide()) {
				return InteractionResult.SUCCESS;
			}
			switch (part(state, pos, hit.getLocation())) {
				case "valve" -> flush((ServerLevel) level, pos, state);
				case "paper" -> takePaper((ServerLevel) level, pos, state, player);
				default -> SeatEntity.sit(level, pos, player, state.getValue(FACING).getOpposite(),
						state.getValue(VARIANT) == 1 ? 7.7 / 16 : 7.1 / 16);
			}
			return InteractionResult.SUCCESS_SERVER;
		}

		public static void flush(ServerLevel level, BlockPos pos, BlockState state) {
			if (state.getValue(FLUSHING)) {
				return;
			}
			level.setBlock(pos, state.setValue(FLUSHING, true), Block.UPDATE_ALL);
			level.playSound(null, pos, TOILET_FLUSH, SoundSource.BLOCKS, 1.0F, 1.0F);
			runFor(level, pos, FLUSH);
			level.scheduleTick(pos, state.getBlock(), 2);
		}

		/** A roll off the dispenser into the player's hands (or at their feet), if there is one left. */
		public static boolean takePaper(ServerLevel level, BlockPos pos, BlockState state, Player player) {
			int left = state.getValue(PAPER);
			if (left == 0) {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.restroom.paper_empty"));
				return false;
			}
			level.setBlock(pos, state.setValue(PAPER, left - 1), Block.UPDATE_ALL);
			ItemStack roll = new ItemStack(BuiltInRegistries.ITEM.getValue(Jugcraft.id("toilet_paper_roll")));
			player.getInventory().placeItemBackInInventory(roll, net.minecraft.util.Prediction.SERVER_ONLY);
			level.playSound(null, pos, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 0.8F, 0.7F);
			return true;
		}

		@Override
		protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
			long end = until(level, pos), now = level.getGameTime();
			if (!state.getValue(FLUSHING)) {
				return;
			}
			if (end < 0 || now >= end) {
				done(level, pos);
				level.setBlock(pos, state.setValue(FLUSHING, false), Block.UPDATE_ALL);
				return;
			}
			double cx = state.getValue(VARIANT) == 1 ? 9 : 8, y = state.getValue(VARIANT) == 1 ? 5.3 : 4.7;
			if (end - now > FLUSH / 3) {
				particles(level, pos, state.getValue(FACING), cx, y, 7.6, ParticleTypes.SPLASH, 6, 0.08, 0.2);
				particles(level, pos, state.getValue(FACING), cx, y, 7.6, ParticleTypes.BUBBLE_POP, 2, 0.08, 0.02);
			}
			level.scheduleTick(pos, this, 3);
		}

		@Override
		protected boolean isRandomlyTicking(BlockState state) {
			return state.getValue(PAPER) < PAPER_MAX;
		}

		@Override
		protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
			// The dispenser is refilled a roll at a time (about one a minute).
			level.setBlock(pos, state.setValue(PAPER, Math.min(PAPER_MAX, state.getValue(PAPER) + 1)), Block.UPDATE_ALL);
		}
	}

	// ------------------------------------------------------------------------------------------------ the urinal
	/** A wall-hung urinal with its flush valve: right-click to flush. */
	public static class Urinal extends FurnitureBlock {
		public static final int FLUSH = 50;

		public Urinal(Properties properties) {
			super(properties, new double[] {4.8, 6.4, 0, 11.2, 16, 5.4});
			registerDefaultState(defaultBlockState().setValue(FLUSHING, false));
		}

		@Override
		protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
			super.createBlockStateDefinition(builder);
			builder.add(FLUSHING);
		}

		@Override
		protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
			if (level.isClientSide()) {
				return InteractionResult.SUCCESS;
			}
			flush((ServerLevel) level, pos, state);
			return InteractionResult.SUCCESS_SERVER;
		}

		public static void flush(ServerLevel level, BlockPos pos, BlockState state) {
			if (state.getValue(FLUSHING)) {
				return;
			}
			level.setBlock(pos, state.setValue(FLUSHING, true), Block.UPDATE_ALL);
			level.playSound(null, pos, URINAL_FLUSH, SoundSource.BLOCKS, 0.9F, 1.0F);
			runFor(level, pos, FLUSH);
			level.scheduleTick(pos, state.getBlock(), 2);
		}

		@Override
		protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
			long end = until(level, pos), now = level.getGameTime();
			if (!state.getValue(FLUSHING)) {
				return;
			}
			if (end < 0 || now >= end) {
				done(level, pos);
				level.setBlock(pos, state.setValue(FLUSHING, false), Block.UPDATE_ALL);
				return;
			}
			particles(level, pos, state.getValue(FACING), 8, 14.5, 1.4, ParticleTypes.FALLING_WATER, 2, 0.12, 0);
			particles(level, pos, state.getValue(FACING), 8, 7.4, 2.4, ParticleTypes.SPLASH, 3, 0.08, 0.1);
			level.scheduleTick(pos, this, 3);
		}
	}

	// ------------------------------------------------------------------------------------------------ sink, dryer
	/** The sink's sensor faucet and the hand dryer: right-click and they run a while (the water, the air, the lamp). */
	public static class Running extends FurnitureBlock {
		static final int SINK = 0, DRYER = 1;
		private final int kind;

		public Running(Properties properties, int kind) {
			super(properties, kind == SINK ? new double[] {0, 8, 0, 16, 15.4, 9.3} : new double[] {5.2, 1.4, 0, 10.8, 7.4, 2.8});
			this.kind = kind;
			registerDefaultState(defaultBlockState().setValue(RUNNING, false));
		}

		@Override
		protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
			super.createBlockStateDefinition(builder);
			builder.add(RUNNING);
		}

		public int runTicks() {
			return kind == SINK ? 100 : 84;
		}

		@Override
		protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
			if (level.isClientSide()) {
				return InteractionResult.SUCCESS;
			}
			start((ServerLevel) level, pos, state);
			return InteractionResult.SUCCESS_SERVER;
		}

		public void start(ServerLevel level, BlockPos pos, BlockState state) {
			boolean was = state.getValue(RUNNING);
			runFor(level, pos, runTicks());
			if (!was) {
				level.setBlock(pos, state.setValue(RUNNING, true), Block.UPDATE_ALL);
				level.playSound(null, pos, kind == SINK ? FAUCET : HAND_DRYER, SoundSource.BLOCKS, 0.8F, 1.0F);
				level.scheduleTick(pos, this, 2);
			}
		}

		@Override
		protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
			long end = until(level, pos), now = level.getGameTime();
			if (!state.getValue(RUNNING)) {
				return;
			}
			if (end < 0 || now >= end) {
				done(level, pos);
				level.setBlock(pos, state.setValue(RUNNING, false), Block.UPDATE_ALL);
				return;
			}
			Direction f = state.getValue(FACING);
			if (kind == SINK) {
				particles(level, pos, f, 8, 15.2, 3.4, ParticleTypes.FALLING_WATER, 1, 0.01, 0);
				particles(level, pos, f, 8, 11.6, 3.8, ParticleTypes.SPLASH, 3, 0.05, 0.1);
			} else {
				particles(level, pos, f, 8, 1.2, 1.6, ParticleTypes.CLOUD, 1, 0.05, 0.01);
			}
			level.scheduleTick(pos, this, 3);
		}
	}

	// ------------------------------------------------------------------------------------------------ the shower stall
	/**
	 * A shower stall (tools/shower_stall.py): its partitions down both sides (walk in between them), the tiled back, the
	 * tray. Right-click turns it on or off; it runs a minute at most. While it runs the water falls from the head
	 * (particles over the model's streams), splashes on the tray, the spray's sound repeats (six seconds, seamless), and
	 * whatever stands in it stops burning.
	 */
	public static class Shower extends FurnitureBlock {
		/** How long it runs before it shuts itself off (ticks), and how often its sound repeats. */
		static final int RUN = 1200, SOUND_EVERY = 118;
		/** The shower head's nozzle and the tray's top (pixels, facing north; tools/shower_stall.py HEAD, TRAY_TOP). */
		static final double HEAD_X = 8.0, HEAD_Y = 22.6, HEAD_Z = 6.0, TRAY = 0.6;
		private static final double[][] BODY = {{0.3, 0, 0, 0.8, 16, 16}, {15.2, 0, 0, 15.7, 16, 16}, {0.8, 0, 0, 15.2, 16, 0.9},
				{0.8, 0, 0.9, 15.2, 1.4, 16}};
		private final Map<Direction, VoxelShape> collision;

		public Shower(Properties properties) {
			super(properties, BODY);
			this.collision = tall(new double[][] {{0.3, 1.6, 0, 0.8, 28.5, 16}, {15.2, 1.6, 0, 15.7, 28.5, 16}, {0.8, 0, 0, 15.2, 24, 0.9},
					{0.8, 0, 0.9, 15.2, 0.6, 16}});
			registerDefaultState(defaultBlockState().setValue(RUNNING, false));
		}

		@Override
		protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
			super.createBlockStateDefinition(builder);
			builder.add(RUNNING);
		}

		@Override
		protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
			return collision.get(state.getValue(FACING));
		}

		@Override
		protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
			if (level.isClientSide()) {
				return InteractionResult.SUCCESS;
			}
			ServerLevel server = (ServerLevel) level;
			if (state.getValue(RUNNING)) {
				stop(server, pos, state);
			} else {
				start(server, pos, state);
			}
			return InteractionResult.SUCCESS_SERVER;
		}

		public void start(ServerLevel level, BlockPos pos, BlockState state) {
			runFor(level, pos, RUN);
			level.setBlock(pos, state.setValue(RUNNING, true), Block.UPDATE_ALL);
			level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.4F, 1.4F);
			level.playSound(null, pos, SHOWER, SoundSource.BLOCKS, 0.8F, 1.0F);
			level.scheduleTick(pos, this, 2);
		}

		public void stop(ServerLevel level, BlockPos pos, BlockState state) {
			done(level, pos);
			level.setBlock(pos, state.setValue(RUNNING, false), Block.UPDATE_ALL);
			level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.4F, 1.2F);
			// The spray stops with the water (its sound would play on to its six seconds' end). It stops every shower's
			// sound for those near: another still running picks its sound up again at its next repeat.
			net.minecraft.network.protocol.game.ClientboundStopSoundPacket quiet =
					new net.minecraft.network.protocol.game.ClientboundStopSoundPacket(SHOWER.location(), SoundSource.BLOCKS);
			for (net.minecraft.server.level.ServerPlayer p : level.players()) {
				if (p.blockPosition().distSqr(pos) < 20 * 20) {
					p.connection.send(quiet);
				}
			}
		}

		@Override
		protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
			if (!state.getValue(RUNNING)) {
				return;
			}
			long end = until(level, pos), now = level.getGameTime();
			if (end < 0 || now >= end) {
				// Run out (or the world was reloaded while it ran: the timers are not saved): off.
				stop(level, pos, state);
				return;
			}
			Direction f = state.getValue(FACING);
			particles(level, pos, f, HEAD_X, HEAD_Y - 0.6, HEAD_Z + 0.8, ParticleTypes.FALLING_WATER, 4, 0.09, 0);
			particles(level, pos, f, HEAD_X, TRAY + 0.6, HEAD_Z + 2.0, ParticleTypes.SPLASH, 3, 0.16, 0.12);
			long ran = RUN - (end - now);
			if (ran >= SOUND_EVERY && ran % SOUND_EVERY < 3) {
				level.playSound(null, pos, SHOWER, SoundSource.BLOCKS, 0.8F, 1.0F);
			}
			AABB inside = new AABB(pos).expandTowards(0, 1, 0);
			for (net.minecraft.world.entity.Entity e : level.getEntities((net.minecraft.world.entity.Entity) null, inside, e -> e.isOnFire())) {
				e.clearFire();
			}
			level.scheduleTick(pos, this, 3);
		}
	}

	// ------------------------------------------------------------------------------------------------ the trash can
	/** A trash can: what you put in it is gone when you close it; it fills up and is emptied each morning. */
	public static class TrashCan extends FurnitureBlock {
		public TrashCan(Properties properties) {
			super(properties, new double[] {5, 0, 5, 11, 8.3, 11});
			registerDefaultState(defaultBlockState().setValue(FILL, 0));
		}

		@Override
		protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
			super.createBlockStateDefinition(builder);
			builder.add(FILL);
		}

		@Override
		protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
			if (level.isClientSide()) {
				return InteractionResult.SUCCESS;
			}
			ServerLevel server = (ServerLevel) level;
			player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new ChestMenu(MenuType.GENERIC_9x1, id, inventory, bin(server, pos), 1),
					Component.translatable("container.jugcraft.restroom_trash_can")));
			return InteractionResult.SUCCESS_SERVER;
		}

		/** The can's inside: whatever is in it when it is closed is thrown away. */
		public static SimpleContainer bin(ServerLevel level, BlockPos pos) {
			return new SimpleContainer(9) {
				@Override
				public void stopOpen(ContainerUser user) {
					throwAway(level, pos, this);
				}
			};
		}

		/** Empties the container into the can: the items are gone, the can a bit fuller. */
		public static boolean throwAway(ServerLevel level, BlockPos pos, SimpleContainer bin) {
			if (bin.isEmpty()) {
				return false;
			}
			bin.clearContent();
			BlockState state = level.getBlockState(pos);
			if (state.getBlock() instanceof TrashCan) {
				level.setBlock(pos, state.setValue(FILL, Math.min(3, state.getValue(FILL) + 1)), Block.UPDATE_ALL);
			}
			level.playSound(null, pos, SoundEvents.BUNDLE_INSERT, SoundSource.BLOCKS, 0.8F, 0.8F);
			return true;
		}

		@Override
		protected boolean isRandomlyTicking(BlockState state) {
			return state.getValue(FILL) > 0;
		}

		@Override
		protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
			// The custodian's morning round: 06:00 to 10:00 game time.
			if (Math.floorMod(level.getOverworldClockTime(), 24000L) < 4000) {
				level.setBlock(pos, state.setValue(FILL, 0), Block.UPDATE_ALL);
			}
		}
	}

	// ------------------------------------------------------------------------------------------------ the stall doors
	public static final BooleanProperty LATCHED = BooleanProperty.create("latched");

	/**
	 * A toilet partition door (its pilasters in its blocks): a vanilla door that latches from inside the stall. The
	 * stall is on the side its leaf closes against (the facing's opposite).
	 */
	public static class StallDoor extends PlantDoorBlock {
		public StallDoor(Properties properties) {
			super(PlantInterior.STEEL, properties, false);
			registerDefaultState(defaultBlockState().setValue(LATCHED, false));
		}

		@Override
		protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
			super.createBlockStateDefinition(builder);
			builder.add(LATCHED);
		}

		/** How far into the stall a point is past the door's middle (the leaf at 0.5), and how far across. */
		static double[] intoStall(BlockState state, BlockPos pos, Vec3 at) {
			Direction in = state.getValue(FACING).getOpposite();
			Direction across = in.getClockWise();
			double dx = at.x - (pos.getX() + 0.5), dz = at.z - (pos.getZ() + 0.5);
			return new double[] {dx * in.getStepX() + dz * in.getStepZ(), dx * across.getStepX() + dz * across.getStepZ()};
		}

		public static boolean inside(BlockState state, BlockPos pos, Player player) {
			return intoStall(state, pos, player.position())[0] > 0.45;
		}

		/** Anyone in the stall behind the door (lower half's position). */
		public static boolean occupied(Level level, BlockState state, BlockPos lower) {
			AABB near = new AABB(lower).inflate(3, 1, 3);
			for (Player p : level.getEntitiesOfClass(Player.class, near)) {
				double[] d = intoStall(state, lower, p.position());
				if (d[0] > 0.45 && d[0] < 2.8 && Math.abs(d[1]) < 2.3 && p.getY() > lower.getY() - 0.6 && p.getY() < lower.getY() + 1.5) {
					return true;
				}
			}
			return false;
		}

		static BlockPos lower(BlockState state, BlockPos pos) {
			return state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
		}

		/** Both halves to this open and latched state (swinging, if it opens or shuts: PlantDoorBlock). */
		public static void set(Level level, BlockPos lower, boolean open, boolean latched) {
			BlockState low = level.getBlockState(lower);
			boolean swing = low.getBlock() instanceof StallDoor && low.getValue(OPEN) != open;
			for (BlockPos p : new BlockPos[] {lower, lower.above()}) {
				BlockState s = level.getBlockState(p);
				if (s.getBlock() instanceof StallDoor) {
					s = s.setValue(OPEN, open).setValue(LATCHED, latched);
					level.setBlock(p, swing ? s.setValue(MOVING, true) : s, Block.UPDATE_CLIENTS | Block.UPDATE_IMMEDIATE);
				}
			}
			if (swing) {
				level.scheduleTick(lower, low.getBlock(), open ? SWING_OPEN : SWING_SHUT);
			}
		}

		/** A click on the door by a player inside or outside the stall: what the door does. */
		public static void use(Level level, BlockPos pos, BlockState state, boolean inside, Player player) {
			BlockPos lower = lower(state, pos);
			boolean open = state.getValue(OPEN), latched = state.getValue(LATCHED);
			StallDoor door = (StallDoor) state.getBlock();
			if (latched) {
				if (inside) {
					set(level, lower, true, false);
					level.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, 0.6F, 1.6F);
					level.playSound(null, pos, door.type().doorOpen(), SoundSource.BLOCKS, 1.0F, 1.1F);
					level.gameEvent(player, GameEvent.BLOCK_OPEN, pos);
				} else {
					if (player != null) {
						player.sendOverlayMessage(Component.translatable("message.jugcraft.restroom.occupied"));
					}
					level.playSound(null, pos, SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 0.4F, 1.8F);
				}
				return;
			}
			if (open) {
				set(level, lower, false, inside);
				level.playSound(null, pos, door.type().doorClose(), SoundSource.BLOCKS, 1.0F, 1.1F);
				if (inside) {
					level.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.6F, 1.6F);
					level.scheduleTick(lower, door, 40);
				}
				level.gameEvent(player, GameEvent.BLOCK_CLOSE, pos);
			} else {
				set(level, lower, true, false);
				level.playSound(null, pos, door.type().doorOpen(), SoundSource.BLOCKS, 1.0F, 1.1F);
				level.gameEvent(player, GameEvent.BLOCK_OPEN, pos);
			}
		}

		@Override
		protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
			if (!level.isClientSide() && !state.getValue(MOVING)) {
				use(level, pos, state, inside(state, lower(state, pos), player), player);
			}
			return InteractionResult.SUCCESS;
		}

		@Override
		protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
			super.tick(state, level, pos, random);              // the end of a swing
			// A latched door with nobody in the stall unlatches (someone left over the top, or logged out).
			BlockState now = level.getBlockState(pos);
			if (now.getBlock() != this || now.getValue(HALF) != DoubleBlockHalf.LOWER || !now.getValue(LATCHED)) {
				return;
			}
			if (occupied(level, now, pos)) {
				level.scheduleTick(pos, this, 40);
			} else {
				set(level, pos, false, false);
			}
		}
	}

	// ------------------------------------------------------------------------------------------------ the tile
	public enum Back implements StringRepresentable {
		/** Painted plaster, bare concrete, or the tower's carbon composite panel (a tiled face on the tower's walls). */
		PAINTED, CONCRETE, CARBON;

		@Override
		public String getSerializedName() {
			return name().toLowerCase(java.util.Locale.ROOT);
		}
	}

	public static final EnumProperty<Back> BACK = EnumProperty.create("back", Back.class);

	/** A wall block tiled on its facing side, painted (or concrete) on the others. */
	public static class TileWall extends HorizontalDirectionalBlock {
		public TileWall(Properties properties) {
			super(properties);
			registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(BACK, Back.PAINTED));
		}

		@Override
		protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
			builder.add(FACING, BACK);
		}

		@Override
		public BlockState getStateForPlacement(BlockPlaceContext context) {
			return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
		}
	}

	// ------------------------------------------------------------------------------------------------ the stalls' parts
	/** A collision box per facing, its top as given (a partition's 1.5 blocks: no jumping over it). */
	static Map<Direction, VoxelShape> tall(double[]... boxes) {
		Map<Direction, VoxelShape> out = new java.util.EnumMap<>(Direction.class);
		for (Direction facing : Direction.Plane.HORIZONTAL) {
			VoxelShape shape = Shapes.empty();
			for (double[] b : boxes) {
				double x0 = b[0], z0 = b[2], x1 = b[3], z1 = b[5];
				int turns = switch (facing) {
					case EAST -> 1;
					case SOUTH -> 2;
					case WEST -> 3;
					default -> 0;
				};
				for (int i = 0; i < turns; i++) {
					double nx0 = 16 - z1, nx1 = 16 - z0, nz0 = x0, nz1 = x1;
					x0 = nx0;
					x1 = nx1;
					z0 = nz0;
					z1 = nz1;
				}
				shape = Shapes.or(shape, Block.box(x0, b[1], z0, x1, Math.min(24, b[4]), z1));
			}
			out.put(facing, shape.optimize());
		}
		return out;
	}

	/**
	 * A partition, the headrail: their brackets as variants (0 plain), placed plain by hand; sneak and right-click with
	 * an empty hand for the next. Collision as tall as the panel (up to 1.5 blocks).
	 */
	public static class Part extends VariantFurnitureBlock {
		private final Map<Direction, VoxelShape> collision;
		private final IntegerProperty variant;
		private final int count;

		public static Part create(Properties properties, int count, double[]... boxes) {
			preparing(count);
			return new Part(properties, count, boxes);
		}

		protected Part(Properties properties, int count, double[]... boxes) {
			super(properties, count, boxes);
			this.collision = tall(boxes);
			this.variant = property(count);
			this.count = count;
		}

		@Override
		public BlockState getStateForPlacement(BlockPlaceContext context) {
			return super.getStateForPlacement(context).setValue(variant, 0);
		}

		@Override
		protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
			if (!player.isSecondaryUseActive() || !player.getMainHandItem().isEmpty()) {
				return InteractionResult.PASS;
			}
			if (!level.isClientSide()) {
				level.setBlock(pos, state.setValue(variant, (state.getValue(variant) + 1) % count), Block.UPDATE_ALL);
			}
			return InteractionResult.SUCCESS;
		}

		@Override
		protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
			return collision.get(state.getValue(FACING));
		}
	}

	/** A plain piece whose collision is as tall as it (the fixed stall panel, the urinal screen). */
	public static class Tall extends FurnitureBlock {
		private final Map<Direction, VoxelShape> collision;

		public Tall(Properties properties, double[]... boxes) {
			super(properties, boxes);
			this.collision = tall(boxes);
		}

		@Override
		protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
			return collision.get(state.getValue(FACING));
		}
	}
}
