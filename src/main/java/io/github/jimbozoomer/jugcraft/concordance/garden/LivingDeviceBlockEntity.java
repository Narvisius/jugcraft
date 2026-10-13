package io.github.jimbozoomer.jugcraft.concordance.garden;

import io.github.jimbozoomer.jugcraft.concordance.Saved;
import io.github.jimbozoomer.jugcraft.concordance.resource.Ownership;
import io.github.jimbozoomer.jugcraft.party.JugcraftParties;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * What every living device of the garden shares (roadmap step 14): who placed it (its keeper), whether a Greenwarden
 * has woken it, and its status word ({@code compose.jugcraft.ecology.status.<id>}: working, dormant, stalled, full and
 * the rest), which clients and Jade are sent so a player can see why it is idle. A device works on a staggered pulse
 * ({@link #due}), so a garden full of them spreads its work over the ticks.
 */
public abstract class LivingDeviceBlockEntity extends BlockEntity {
	protected boolean awake;
	protected @Nullable UUID keeper;
	protected String status = "dormant";

	protected LivingDeviceBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	public boolean awake() {
		return awake;
	}

	public @Nullable UUID keeper() {
		return keeper;
	}

	public String status() {
		return status;
	}

	void placedBy(ServerPlayer player) {
		keeper = player.getUUID();
		awake = Garden.knows(player);
		status = awake ? "idle" : "dormant";
		setChanged();
		sync();
	}

	/** Woken by a Greenwarden (who becomes its keeper if it had none). */
	public void awaken(UUID by) {
		awake = true;
		if (keeper == null) {
			keeper = by;
		}
		status("idle");
		setChanged();
	}

	/** Changes the status word; clients are told only when it changes. */
	protected void status(String id) {
		if (!id.equals(status)) {
			status = id;
			setChanged();
			sync();
		}
	}

	/** Whether this tick is the device's pulse: once every {@code period} ticks, staggered by its position. */
	protected boolean due(ServerLevel level, int period) {
		return Math.floorMod(level.getGameTime() + worldPosition.asLong(), period) == 0;
	}

	/** Who may draw from or change it: its keeper and their party (or anyone, if no one placed it). */
	public Ownership ownership() {
		if (keeper == null) {
			return Ownership.NONE;
		}
		Set<UUID> members = new HashSet<>(JugcraftParties.partyMembers(keeper));
		members.remove(keeper);
		return new Ownership(keeper, members, false);
	}

	/** Whether a player may change its settings: its keeper's party, an operator, or anyone if it has no keeper. */
	public boolean mayChange(ServerPlayer player) {
		return keeper == null || ownership().mayExtract(player.getUUID()) || Commands.LEVEL_GAMEMASTERS.check(player.createCommandSourceStack().permissions());
	}

	protected void sync() {
		if (level instanceof ServerLevel server) {
			server.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		awake = input.getBooleanOr("awake", false);
		keeper = input.read("keeper", UUIDUtil.CODEC).orElse(null);
		status = input.getStringOr("status", awake ? "idle" : "dormant");
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		Saved.stamp(output, 1);
		output.putBoolean("awake", awake);
		if (keeper != null) {
			output.store("keeper", UUIDUtil.CODEC, keeper);
		}
		output.putString("status", status);
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	/** Clients get whether it is awake, its status and what {@link #clientData} adds; never the keeper or its slots. */
	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		CompoundTag tag = new CompoundTag();
		tag.putBoolean("awake", awake);
		tag.putString("status", status);
		clientData(tag);
		return tag;
	}

	/** What else clients see (read back by the subclass's {@code loadAdditional}). */
	protected void clientData(CompoundTag tag) {
	}
}
