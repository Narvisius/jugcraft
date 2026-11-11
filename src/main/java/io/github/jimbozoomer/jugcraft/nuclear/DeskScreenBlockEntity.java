package io.github.jimbozoomer.jugcraft.nuclear;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;

/**
 * An office computer's screen (the administration building part 1; tools/admin_fittings.py): the plant intranet's home
 * page, read every two seconds from the nearest reactor (StatusScreenBlockEntity's reach) and sent to the players
 * nearby only when it changes; client/DeskScreenRenderer.java writes it on the screen. Nothing is saved: it reads again.
 */
public class DeskScreenBlockEntity extends BlockEntity {
	/** The rated output the intranet shows at full power (MWe). */
	public static final int RATED_MWE = 1152;
	/** 0 no reactor in reach, 1 at power, 2 shut down, 3 tripped. */
	public int status;
	/** Reactor power (percent), the generator's output (MWe, 0 off line), the alarms in (a count). */
	public int power, mwe, alarms;
	private int timer = -1;

	public DeskScreenBlockEntity(BlockPos pos, BlockState state) {
		super(PlantInterior.DESK_ENTITY, pos, state);
	}

	public void serverTick(ServerLevel level) {
		if (timer < 0) {
			// Spread the office's screens over the two seconds.
			timer = Math.floorMod(getBlockPos().hashCode(), 40);
		}
		if (timer-- > 0) {
			return;
		}
		timer = 39;
		EquipmentBlockEntity vessel = StatusScreenBlockEntity.nearestVessel(level, getBlockPos());
		int s = 0, p = 0, m = 0, a = 0;
		if (vessel != null) {
			ReactorCore core = vessel.core;
			s = core.scrammed ? 3 : core.shutDown() ? 2 : 1;
			p = (int) Math.round(Math.max(0, core.heat()) * 100);
			m = core.electric > 0 ? (int) Math.round(RATED_MWE * Math.min(1, Math.max(0, core.heat()))) : 0;
			a = Integer.bitCount(core.alarms);
		}
		if (s != status || p != power || m != mwe || a != alarms) {
			status = s;
			power = p;
			mwe = m;
			alarms = a;
			setChanged();
			level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
		}
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
		out.putInt("status", status);
		out.putInt("power", power);
		out.putInt("mwe", mwe);
		out.putInt("alarms", alarms);
		return out.buildResult();
	}

	@Override
	protected void loadAdditional(ValueInput in) {
		super.loadAdditional(in);
		status = in.getIntOr("status", 0);
		power = in.getIntOr("power", 0);
		mwe = in.getIntOr("mwe", 0);
		alarms = in.getIntOr("alarms", 0);
	}
}
