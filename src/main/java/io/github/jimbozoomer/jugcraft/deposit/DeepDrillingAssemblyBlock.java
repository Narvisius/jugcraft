package io.github.jimbozoomer.jugcraft.deposit;

import io.github.jimbozoomer.jugcraft.machine.MachineBlock;
import io.github.jimbozoomer.jugcraft.machine.MachineKind;
import io.github.jimbozoomer.jugcraft.town.TownProtection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/** A removable mast mounted at the centre of the existing rig's roof. No second machine or inventory. */
public final class DeepDrillingAssemblyBlock extends DoublePlantBlock {
	public static final int DEPTH = 128, RADIUS = 4, BORE_TICKS = 20, USE = 32;
	public DeepDrillingAssemblyBlock(Properties properties) { super(properties); }

	public static BlockPos center(BlockPos master, BlockState state) {
		MachineBlock block = (MachineBlock) state.getBlock();
		var offsets = block.footprint(state);
		// The large rig's centre is part 4; support compact legacy rigs too.
		return offsets.size() > 1 ? offsets.partPos(master, state.getValue(MachineBlock.FACING), 4) : master;
	}

	public static BlockPos mount(BlockPos master, BlockState state) {
		return center(master, state).above(((MachineBlock) state.getBlock()).height(state));
	}

	public static boolean installed(Level level, BlockPos master, BlockState state) {
		BlockPos mount = mount(master, state);
		return level.isLoaded(mount.above()) && isHalf(level.getBlockState(mount), DoubleBlockHalf.LOWER)
				&& isHalf(level.getBlockState(mount.above()), DoubleBlockHalf.UPPER);
	}

	private static boolean isHalf(BlockState state, DoubleBlockHalf half) {
		return state.is(JugcraftDeposits.DEEP_DRILLING_ASSEMBLY) && state.getValue(HALF) == half;
	}

	private static boolean mayPlace(Level level, BlockPos pos, Player player, ItemStack stack) {
		return level.isLoaded(pos) && level.getBlockState(pos).isAir() && level.isInWorldBounds(pos)
				&& level.getWorldBorder().isWithinBounds(pos) && level.mayInteract(player, pos)
				&& player.mayUseItemAt(pos, Direction.UP, stack) && !TownProtection.denies(player, level, pos)
				&& (!(level instanceof net.minecraft.server.level.ServerLevel server)
					|| io.github.jimbozoomer.jugcraft.concordance.Authority.mayChange(server, player, pos));
	}

	@Override public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		Player player = context.getPlayer();
		BlockPos pos = context.getClickedPos();
		if (player == null || !mayPlace(context.getLevel(), pos, player, context.getItemInHand())
				|| !mayPlace(context.getLevel(), pos.above(), player, context.getItemInHand())) return null;
		BlockState state = super.getStateForPlacement(context);
		return state != null && state.canSurvive(context.getLevel(), pos) ? state : null;
	}

	@Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		var machine = MachineBlock.machineAt(level, pos.below(), level.getBlockState(pos.below()));
		if (placer instanceof Player player && machine != null) machine.claimDepositDrill(player);
	}

	public static InteractionResult install(ItemStack stack, Level level, BlockPos part, BlockState state, Player player) {
		BlockPos master = ((MachineBlock) state.getBlock()).masterPos(part, state);
		BlockPos mount = mount(master, state);
		if (!mayPlace(level, mount, player, stack) || !mayPlace(level, mount.above(), player, stack)) return InteractionResult.FAIL;
		if (!level.isClientSide()) {
			DoublePlantBlock.placeAt(level, JugcraftDeposits.DEEP_DRILLING_ASSEMBLY.defaultBlockState(), mount, 3);
			var machine = MachineBlock.machineAt(level, part, state);
			if (machine != null) machine.claimDepositDrill(player);
			stack.consume(1, player);
		}
		return InteractionResult.SUCCESS;
	}

	@Override protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		BlockState below = level.getBlockState(pos.below());
		if (state.getValue(HALF) == DoubleBlockHalf.UPPER) return isHalf(below, DoubleBlockHalf.LOWER);
		if (!(below.getBlock() instanceof MachineBlock machine) || machine.kind() != MachineKind.DEPOSIT_DRILL) return false;
		BlockPos master = machine.masterPos(pos.below(), below);
		return mount(master, below).equals(pos);
	}

	@Override protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
			Direction direction, BlockPos neighbor, BlockState neighborState, net.minecraft.util.RandomSource random) {
		return canSurvive(state, level, pos)
				? super.updateShape(state, level, ticks, pos, direction, neighbor, neighborState, random) : Blocks.AIR.defaultBlockState();
	}

	@Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return state.getValue(HALF) == DoubleBlockHalf.LOWER ? Block.box(1, -11, 1, 15, 16, 15)
				: Block.box(2, 0, 2, 14, 16, 14);
	}

	@Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		BlockPos lower = state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
		var machine = MachineBlock.machineAt(level, lower.below(), level.getBlockState(lower.below()));
		if (!level.isClientSide() && machine != null) player.sendOverlayMessage(Component.translatable(
				"message.jugcraft.deep_drilling", machine.depositDepth(), DEPTH, machine.depositBoreProgress() * 100 / BORE_TICKS,
				machine.processingProgress(), machine.processingDuration()));
		return InteractionResult.SUCCESS;
	}
}
