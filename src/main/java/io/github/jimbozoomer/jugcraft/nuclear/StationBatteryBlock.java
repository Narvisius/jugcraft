package io.github.jimbozoomer.jugcraft.nuclear;

import io.github.jimbozoomer.jugcraft.energy.EnergyStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * The station battery (round 25): placed as one piece like any panel, its placing block holds the charge
 * (StationBatteryBlockEntity), and every block of it is the battery to a cable or a charger. Right-click: its charge.
 */
public class StationBatteryBlock extends MultiFurnitureBlock implements EntityBlock {
	public StationBatteryBlock(Properties properties, int width, int rows) {
		super(properties, width, rows, true, 1, true, false, 15.6);
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return state.getValue(partProperty()) == origin() ? new StationBatteryBlockEntity(pos, state) : null;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (!(level instanceof ServerLevel) || type != PlantInterior.STATION_BATTERY_ENTITY) {
			return null;
		}
		return (BlockEntityTicker<T>) (BlockEntityTicker<StationBatteryBlockEntity>) (l, pos, s, battery) -> battery.serverTick((ServerLevel) l);
	}

	/** The battery any block of it belongs to (made if the rack was placed before batteries held charge), or null. */
	public static @Nullable StationBatteryBlockEntity battery(Level level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if (!(state.getBlock() instanceof StationBatteryBlock block)) {
			return null;
		}
		BlockPos origin = pos.subtract(block.offset(state.getValue(FACING), state.getValue(block.partProperty())));
		return level.getBlockEntity(origin) instanceof StationBatteryBlockEntity battery ? battery : null;
	}

	/** What a cable touching the battery finds: its charge, to put in or take out. */
	public static @Nullable EnergyStorage storage(Level level, BlockPos pos) {
		StationBatteryBlockEntity battery = battery(level, pos);
		return battery == null ? null : battery.energy;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		StationBatteryBlockEntity battery = battery(level, pos);
		if (battery == null) {
			return InteractionResult.PASS;
		}
		if (!level.isClientSide()) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.station_battery", Math.round(battery.charge() * 100),
					String.format("%,d", battery.energy.getAmount()), String.format("%,d", StationBatteryBlockEntity.CAPACITY),
					Component.translatable(battery.flow > 0 ? "message.jugcraft.nuclear.battery_charging"
							: battery.flow < 0 ? "message.jugcraft.nuclear.battery_discharging" : "message.jugcraft.nuclear.battery_float",
							String.format("%,d", Math.abs(battery.flow)))));
		}
		return InteractionResult.SUCCESS;
	}
}
