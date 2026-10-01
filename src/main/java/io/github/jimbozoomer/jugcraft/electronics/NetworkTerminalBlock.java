package io.github.jimbozoomer.jugcraft.electronics;

import io.github.jimbozoomer.jugcraft.energy.CableBlock;
import io.github.jimbozoomer.jugcraft.energy.EnergyConnectable;
import io.github.jimbozoomer.jugcraft.energy.EnergyNetworks;
import io.github.jimbozoomer.jugcraft.energy.EnergyStorage;
import io.github.jimbozoomer.jugcraft.machine.MachineBlock;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * The network terminal: a beige retro computer that reads out the power network it is cabled to. Right-click it and
 * it shows how many cables the network has, the rate its slowest cable sets, how many devices it reaches and how much
 * energy they hold. It uses no power and stores none; cables connect to it on every side.
 */
public class NetworkTerminalBlock extends Block implements EnergyConnectable {
	public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;

	public NetworkTerminalBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide()) {
			Reading reading = read(level, pos);
			player.sendOverlayMessage(reading == null
					? Component.translatable("message.jugcraft.network_terminal.none")
					: Component.translatable("message.jugcraft.network_terminal", String.format("%,d", reading.cables()),
							String.format("%,d", reading.rate()), reading.devices(), String.format("%,d", reading.stored()),
							String.format("%,d", reading.capacity()), reading.percent()));
		}
		return InteractionResult.SUCCESS;
	}

	/** What the terminal shows: the network through the first cable beside it (null when no cable touches it). */
	public record Reading(int cables, long rate, int devices, long stored, long capacity) {
		public int percent() {
			return capacity <= 0 ? 0 : (int) (stored * 100 / capacity);
		}
	}

	/**
	 * Reads the network beside the terminal. Each device counts once, however many of its faces (or, for a multi-block
	 * machine, of its blocks) touch the network.
	 */
	public static @Nullable Reading read(Level level, BlockPos pos) {
		for (Direction direction : Direction.values()) {
			BlockPos cable = pos.relative(direction);
			if (!(level.getBlockState(cable).getBlock() instanceof CableBlock)) {
				continue;
			}
			EnergyNetworks.View view = EnergyNetworks.view(level, cable);
			List<Object> seen = new ArrayList<>();
			long stored = 0;
			long capacity = 0;
			for (EnergyNetworks.Endpoint endpoint : view.endpoints()) {
				EnergyStorage storage = EnergyStorage.SIDED.find(level, endpoint.pos(), endpoint.side());
				if (storage == null) {
					continue;
				}
				Object owner = owner(level, endpoint.pos(), storage);
				if (seen.stream().anyMatch(o -> o == owner)) {
					continue;
				}
				seen.add(owner);
				stored += storage.getAmount();
				capacity += storage.getCapacity();
			}
			return new Reading(view.cables(), view.rate(), seen.size(), stored, capacity);
		}
		return null;
	}

	/** The thing a storage belongs to: a machine's master block entity, another block entity, or the storage itself. */
	private static Object owner(Level level, BlockPos pos, EnergyStorage storage) {
		BlockState state = level.getBlockState(pos);
		Object machine = MachineBlock.machineAt(level, pos, state);
		if (machine != null) {
			return machine;
		}
		Object entity = level.getBlockEntity(pos);
		return entity != null ? entity : storage;
	}
}
