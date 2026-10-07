package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A Spooky Sign's own words, if it has any: plain text of at most {@link GravestoneBlockEntity#MAX_LENGTH} characters,
 * painted with a named Name Tag or carried by a sign renamed in an anvil, as a gravestone's engraving is. They go with
 * the item when the sign is broken (as its custom name) and are sent to clients, which paint them on the board; without
 * them the board shows its painted {@link SpookySignBlock#WORDS}.
 */
public class SpookySignBlockEntity extends BlockEntity {
	private String text = "";

	public SpookySignBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftAgriculture.SPOOKY_SIGN_ENTITY, pos, state);
	}

	public String text() {
		return text;
	}

	/** Paints {@code words} (cut to length, control characters dropped; blank wipes it) and sends them to clients. */
	public void paint(String words) {
		text = GravestoneBlockEntity.clean(words);
		setChanged();
		if (level != null) {
			BlockState state = getBlockState();
			level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		text = GravestoneBlockEntity.clean(input.getStringOr("words", ""));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		if (!text.isEmpty()) {
			output.putString("words", text);
		}
	}

	@Override
	protected void applyImplicitComponents(DataComponentGetter components) {
		super.applyImplicitComponents(components);
		Component name = components.get(DataComponents.CUSTOM_NAME);
		text = name == null ? "" : GravestoneBlockEntity.clean(name.getString());
	}

	@Override
	protected void collectImplicitComponents(DataComponentMap.Builder components) {
		super.collectImplicitComponents(components);
		if (!text.isEmpty()) {
			components.set(DataComponents.CUSTOM_NAME, Component.literal(text));
		}
	}

	@Override
	public void removeComponentsFromTag(ValueOutput output) {
		super.removeComponentsFromTag(output);
		output.discard("words");
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return saveCustomOnly(registries);
	}
}
