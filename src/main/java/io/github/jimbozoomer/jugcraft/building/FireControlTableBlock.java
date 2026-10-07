package io.github.jimbozoomer.jugcraft.building;

import io.github.jimbozoomer.jugcraft.artillery.JugcraftArtillery;
import io.github.jimbozoomer.jugcraft.artillery.Spotting;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The Fire Control Table (batch 56, docs/features/fire-control.md). Up to {@value FireControl#MAX_GUNS} guns are linked
 * to it with Fire Control Wire; it lays every one of them that has nobody at its controls, by its {@link Mode}:
 * <ul>
 * <li>Hold: the guns stand still.</li>
 * <li>Converge: every gun lays on the table's target, and a redstone pulse fires one round from each.</li>
 * <li>Parallel: as converge, but the guns lay on points {@value FireControl#SHEAF_SPACING} blocks apart across the line
 * of fire.</li>
 * <li>Creeping barrage: as converge, but after each salvo the target steps {@value FireControl#CREEP_STEP} blocks further
 * down the line of fire, {@value FireControl#CREEP_STEPS} times, then starts again from where it was set.</li>
 * <li>Sentry: each gun picks and fires on the nearest hostile mob inside the table's sector by itself.</li>
 * </ul>
 * The guns do the laying and firing themselves ({@code CrewedGun}); the table holds the orders: the mode, the target, the
 * sector and a salvo count that each pulse raises. A comparator reads how many linked guns are ready.
 *
 * <p>Use it empty-handed to change mode, sneaking to change the sector (centred on the way the table faces: the way
 * its placer looked), with a Range Finder to make your mark its target (sneaking: clear it), and with Fire Control Wire
 * to start a link (sneaking: cut every link).
 */
public class FireControlTableBlock extends HorizontalDirectionalBlock implements EntityBlock {
	public static final EnumProperty<Mode> MODE = EnumProperty.create("mode", Mode.class);
	public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

	/** What the table has its guns do. */
	public enum Mode implements StringRepresentable {
		HOLD("hold"), CONVERGE("converge"), PARALLEL("parallel"), CREEPING("creeping"), SENTRY("sentry");

		private final String name;

		Mode(String name) {
			this.name = name;
		}

		/** Whether the guns lay on the table's target and fire on a pulse. */
		public boolean mission() {
			return this == CONVERGE || this == PARALLEL || this == CREEPING;
		}

		Mode next() {
			return values()[(ordinal() + 1) % values().length];
		}

		@Override
		public String getSerializedName() {
			return name;
		}
	}

	public FireControlTableBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(MODE, Mode.HOLD).setValue(POWERED, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, MODE, POWERED);
	}

	/** The table faces the way its placer looked: toward the enemy, with its user behind it. */
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection())
				.setValue(POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos()));
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		if (!oldState.is(this)) {
			level.scheduleTick(pos, this, FireControl.TABLE_INTERVAL);
		}
	}

	/** Keeps the comparator reading current. */
	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (level.getBlockEntity(pos) instanceof Entity table) {
			int ready = table.readyCount(level.getGameTime());
			if (ready != table.lastReady) {
				table.lastReady = ready;
				level.updateNeighbourForOutputSignal(pos, this);
			}
		}
		level.scheduleTick(pos, this, FireControl.TABLE_INTERVAL);
	}

	/** A rising redstone signal fires a salvo: one round from every gun laid on the target. */
	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, @Nullable Orientation orientation,
			boolean movedByPiston) {
		boolean powered = level.hasNeighborSignal(pos);
		if (powered == state.getValue(POWERED)) {
			return;
		}
		level.setBlock(pos, state.setValue(POWERED, powered), Block.UPDATE_CLIENTS);
		if (powered && level.getBlockEntity(pos) instanceof Entity table && table.fire(state.getValue(MODE))) {
			level.playSound(null, pos, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, 1.0F, 0.6F);
		}
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		boolean wire = stack.is(FireControl.WIRE);
		if (!(wire || stack.is(JugcraftArtillery.RANGE_FINDER)) || !(level.getBlockEntity(pos) instanceof Entity table)) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (!(level instanceof ServerLevel server)) {
			return InteractionResult.SUCCESS;
		}
		if (wire) {
			if (player.isShiftKeyDown()) {
				table.links.clear();
				table.setChanged();
				player.sendOverlayMessage(Component.translatable("message.jugcraft.fire_control.cleared"));
			} else {
				FireControl.startLink(player, server, pos);
				player.sendOverlayMessage(Component.translatable("message.jugcraft.fire_control.link_started", FireControl.LINK_RANGE));
			}
			return InteractionResult.SUCCESS;
		}
		if (player.isShiftKeyDown()) {
			table.target = null;
		} else {
			BlockPos mark = Spotting.own(server, player);
			if (mark == null) {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.fire_control.no_mark"));
				return InteractionResult.SUCCESS;
			}
			table.target = mark;
			player.sendOverlayMessage(Component.translatable("message.jugcraft.fire_control.target_set",
					(int) Math.round(Math.sqrt(mark.distSqr(pos)))));
			table.setChanged();
			return InteractionResult.SUCCESS;
		}
		table.setChanged();
		status(server, pos, state, table, player);
		return InteractionResult.SUCCESS;
	}

	/** Empty-handed: the next mode; sneaking, the next sector width. */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!(level instanceof ServerLevel server) || !(level.getBlockEntity(pos) instanceof Entity table)) {
			return InteractionResult.SUCCESS;
		}
		if (player.isShiftKeyDown()) {
			table.sector = (table.sector + 1) % FireControl.SECTORS.length;
			table.setChanged();
		} else {
			state = state.setValue(MODE, state.getValue(MODE).next());
			level.setBlock(pos, state, Block.UPDATE_ALL);
		}
		level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.6F, 1.4F);
		status(server, pos, state, table, player);
		return InteractionResult.SUCCESS;
	}

	private static void status(ServerLevel level, BlockPos pos, BlockState state, Entity table, Player player) {
		Component target = table.target == null ? Component.translatable("message.jugcraft.fire_control.no_target")
				: Component.translatable("message.jugcraft.fire_control.target", (int) Math.round(Math.sqrt(table.target.distSqr(pos))));
		if (table.target != null && state.getValue(MODE) == Mode.CREEPING) {
			target = target.copy().append(", ").append(Component.translatable("message.jugcraft.fire_control.creep",
					table.creep + 1, FireControl.CREEP_STEPS));
		}
		player.sendOverlayMessage(Component.translatable("message.jugcraft.fire_control.status",
				Component.translatable("message.jugcraft.fire_control.mode." + state.getValue(MODE).getSerializedName()),
				table.readyCount(level.getGameTime()), table.links.size(), table.sectorWidth(), target));
	}

	@Override
	protected boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	/** How many linked guns are ready: laid, reloaded and with a shell to hand. */
	@Override
	protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
		return level.getBlockEntity(pos) instanceof Entity table ? Math.min(15, table.readyCount(level.getGameTime())) : 0;
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new Entity(pos, state);
	}

	/** The table's orders: its linked guns, target, sector and salvo count, and which guns last said they were ready. */
	public static class Entity extends BlockEntity {
		private final List<UUID> links = new ArrayList<>();
		private final Map<UUID, Long> readyAt = new HashMap<>();
		private @Nullable BlockPos target;
		private int sector = FireControl.SECTORS.length - 1;
		private long salvo;
		private int lastReady;
		/** A creeping barrage: how many steps down range it has walked, and whether it has fired since the target was set. */
		private int creep;
		private boolean firedHere;

		public Entity(BlockPos pos, BlockState state) {
			super(FireControl.TABLE_ENTITY, pos, state);
		}

		public Mode mode() {
			BlockState state = getBlockState();
			return state.hasProperty(MODE) ? state.getValue(MODE) : Mode.HOLD;
		}

		public List<UUID> links() {
			return List.copyOf(links);
		}

		public boolean linked(UUID gun) {
			return links.contains(gun);
		}

		/** Links a gun, if there is room; returns whether it is linked now. */
		public boolean link(UUID gun) {
			if (links.contains(gun)) {
				return true;
			}
			if (links.size() >= FireControl.MAX_GUNS) {
				return false;
			}
			links.add(gun);
			setChanged();
			return true;
		}

		public void unlink(UUID gun) {
			if (links.remove(gun)) {
				readyAt.remove(gun);
				setChanged();
			}
		}

		public @Nullable BlockPos target() {
			return target;
		}

		/** Sets the target directly (as using a Range Finder on the table does). */
		public void setTarget(@Nullable BlockPos target) {
			this.target = target == null ? null : target.immutable();
			creep = 0;
			firedHere = false;
			setChanged();
		}

		/** How many steps down range a creeping barrage has walked. */
		public int creep() {
			return creep;
		}

		public int sectorWidth() {
			return FireControl.SECTORS[Mth.clamp(sector, 0, FireControl.SECTORS.length - 1)];
		}

		public void setSectorWidth(int width) {
			for (int i = 0; i < FireControl.SECTORS.length; i++) {
				if (FireControl.SECTORS[i] == width) {
					sector = i;
					setChanged();
				}
			}
		}

		/** How many salvoes the table has ordered; a gun fires once each time this goes up. */
		public long salvo() {
			return salvo;
		}

		/** Orders a salvo, if the table is on a fire mission with a target; returns whether it did. */
		public boolean fire(Mode mode) {
			if (!mode.mission() || target == null || links.isEmpty()) {
				return false;
			}
			if (mode == Mode.CREEPING && firedHere) {
				// Each salvo after the first walks the barrage a step further down range.
				creep = (creep + 1) % FireControl.CREEP_STEPS;
			}
			firedHere = true;
			salvo++;
			setChanged();
			return true;
		}

		/** A gun saying it is ready this tick. */
		public void ready(UUID gun, long now) {
			if (links.contains(gun)) {
				readyAt.put(gun, now);
			}
		}

		public int readyCount(long now) {
			readyAt.values().removeIf(at -> now - at > 2L * FireControl.TABLE_INTERVAL);
			return readyAt.size();
		}

		/**
		 * The point the gun should lay on for a fire mission: the target block's top, or for a parallel sheaf the point
		 * its place in the battery puts it at across the line of fire. Null with no target or if the gun is not linked.
		 */
		public @Nullable Vec3 aimPoint(UUID gun) {
			int index = links.indexOf(gun);
			if (target == null || index < 0) {
				return null;
			}
			Vec3 point = Vec3.atCenterOf(target).add(0, 0.5, 0);
			Vec3 line = point.subtract(Vec3.atCenterOf(worldPosition));
			line = new Vec3(line.x, 0, line.z);
			if (line.lengthSqr() < 1.0E-4) {
				Direction facing = getBlockState().getValue(FACING);
				line = new Vec3(facing.getStepX(), 0, facing.getStepZ());
			}
			if (mode() == Mode.CREEPING) {
				return point.add(line.normalize().scale(creep * FireControl.CREEP_STEP));
			}
			if (mode() != Mode.PARALLEL || links.size() < 2) {
				return point;
			}
			Vec3 across = new Vec3(-line.z, 0, line.x).normalize();
			return point.add(across.scale((index - (links.size() - 1) / 2.0) * FireControl.SHEAF_SPACING));
		}

		/** Whether a point lies inside the table's sector, centred on the way it faces. */
		public boolean inSector(Vec3 point) {
			if (sectorWidth() >= 360) {
				return true;
			}
			Vec3 delta = point.subtract(Vec3.atCenterOf(worldPosition));
			float yaw = (float) Math.toDegrees(Math.atan2(-delta.x, delta.z));
			float facing = getBlockState().getValue(FACING).toYRot();
			return Math.abs(Mth.wrapDegrees(yaw - facing)) <= sectorWidth() / 2.0F;
		}

		@Override
		protected void loadAdditional(ValueInput input) {
			super.loadAdditional(input);
			links.clear();
			links.addAll(input.read("links", UUIDUtil.STRING_CODEC.listOf()).orElse(List.of()));
			target = input.read("target", BlockPos.CODEC).orElse(null);
			sector = Mth.clamp(input.getIntOr("sector", FireControl.SECTORS.length - 1), 0, FireControl.SECTORS.length - 1);
			salvo = input.getLongOr("salvo", 0L);
			creep = Mth.clamp(input.getIntOr("creep", 0), 0, FireControl.CREEP_STEPS - 1);
			firedHere = input.getBooleanOr("fired_here", false);
		}

		@Override
		protected void saveAdditional(ValueOutput output) {
			super.saveAdditional(output);
			output.store("links", UUIDUtil.STRING_CODEC.listOf(), List.copyOf(links));
			if (target != null) {
				output.store("target", BlockPos.CODEC, target);
			}
			output.putInt("sector", sector);
			output.putLong("salvo", salvo);
			output.putInt("creep", creep);
			output.putBoolean("fired_here", firedHere);
		}
	}
}
