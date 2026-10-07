package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * A hand-carved pumpkin's design ({@link PumpkinCarving}) and who carved it last. The design goes with
 * the item when the pumpkin is broken (data component {@code jugcraft:carving}, copied by its loot
 * table) and is sent to clients, which draw it. The carver is kept only in the saved world, for
 * server operators ({@code /data get block}).
 */
public class CarvedPumpkinBlockEntity extends BlockEntity {
	private PumpkinCarving carving = PumpkinCarving.BLANK;
	private @Nullable UUID carverId;
	private String carverName = "";

	public CarvedPumpkinBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftAgriculture.CARVED_PUMPKIN_ENTITY, pos, state);
	}

	public PumpkinCarving carving() {
		return carving;
	}

	public Optional<UUID> carverId() {
		return Optional.ofNullable(carverId);
	}

	public String carverName() {
		return carverName;
	}

	/** Sets the design (server side), records the carver and sends the change to clients. */
	public void setCarving(PumpkinCarving carving, @Nullable Player carver) {
		this.carving = carving;
		if (carver != null) {
			carverId = carver.getUUID();
			carverName = carver.getName().getString();
		}
		setChanged();
		if (level != null) {
			BlockState state = getBlockState();
			level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		carving = input.read("carving", PumpkinCarving.CODEC).orElse(PumpkinCarving.BLANK);
		carverId = input.read("carved_by", UUIDUtil.CODEC).orElse(null);
		carverName = input.getStringOr("carved_by_name", "");
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		if (!carving.isBlank()) {
			output.store("carving", PumpkinCarving.CODEC, carving);
		}
		if (carverId != null) {
			output.store("carved_by", UUIDUtil.CODEC, carverId);
			output.putString("carved_by_name", carverName);
		}
	}

	@Override
	protected void applyImplicitComponents(DataComponentGetter components) {
		super.applyImplicitComponents(components);
		carving = components.getOrDefault(JugcraftAgriculture.CARVING, PumpkinCarving.BLANK);
	}

	@Override
	protected void collectImplicitComponents(DataComponentMap.Builder components) {
		super.collectImplicitComponents(components);
		if (!carving.isBlank()) {
			components.set(JugcraftAgriculture.CARVING, carving);
		}
	}

	@Override
	public void removeComponentsFromTag(ValueOutput output) {
		super.removeComponentsFromTag(output);
		output.discard("carving");
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		// Clients need only the design, not who carved it.
		CompoundTag tag = saveCustomOnly(registries);
		tag.remove("carved_by");
		tag.remove("carved_by_name");
		return tag;
	}
}
