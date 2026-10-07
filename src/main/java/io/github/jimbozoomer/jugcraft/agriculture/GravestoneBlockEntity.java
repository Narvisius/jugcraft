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
 * A gravestone's engraving: plain text of at most {@link #MAX_LENGTH} characters, cut in with a named Name Tag
 * or carried by a gravestone renamed in an anvil. It goes with the item when the gravestone is broken (as its
 * custom name) and is sent to clients, which draw it on the stone's face.
 */
public class GravestoneBlockEntity extends BlockEntity {
	/** As long as an anvil lets a name be. */
	public static final int MAX_LENGTH = 50;

	private String text = "";

	public GravestoneBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftAgriculture.GRAVESTONE_ENTITY, pos, state);
	}

	public String text() {
		return text;
	}

	/** Engraves {@code engraving} (cut to {@link #MAX_LENGTH}, control characters dropped) and sends it to clients. */
	public void engrave(String engraving) {
		text = clean(engraving);
		setChanged();
		if (level != null) {
			BlockState state = getBlockState();
			level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
		}
	}

	static String clean(String raw) {
		StringBuilder out = new StringBuilder();
		raw.codePoints().filter(c -> !Character.isISOControl(c)).forEach(out::appendCodePoint);
		String text = out.toString().strip();
		return text.codePointCount(0, text.length()) > MAX_LENGTH ? text.substring(0, text.offsetByCodePoints(0, MAX_LENGTH)) : text;
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		text = clean(input.getStringOr("engraving", ""));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		if (!text.isEmpty()) {
			output.putString("engraving", text);
		}
	}

	@Override
	protected void applyImplicitComponents(DataComponentGetter components) {
		super.applyImplicitComponents(components);
		Component name = components.get(DataComponents.CUSTOM_NAME);
		text = name == null ? "" : clean(name.getString());
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
		output.discard("engraving");
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
