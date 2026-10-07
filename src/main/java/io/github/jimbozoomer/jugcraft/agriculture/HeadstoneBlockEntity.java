package io.github.jimbozoomer.jugcraft.agriculture;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A memorial's inscriptions, on the block of its part 0 (a {@link HeadstoneBlock}): its {@link Epitaph}, and on a
 * building of pack 3 the rest of them (a mausoleum's crypt fronts, a columbarium's niches), each by its slot in
 * {@link HeadstoneBlock.Layout#texts()}. They go with the item when it is broken (data components
 * {@code jugcraft:epitaph} and {@code jugcraft:inscriptions}) and come back when it is placed again; one renamed in an
 * anvil is placed with its name as the epitaph's first line. Clients get them to draw.
 */
public class HeadstoneBlockEntity extends BlockEntity {
	/** The most inscriptions after the epitaph that one memorial may keep. */
	public static final int MORE = 15;
	public static final Codec<List<Epitaph>> MORE_CODEC = Epitaph.CODEC.sizeLimitedListOf(MORE);
	public static final StreamCodec<ByteBuf, List<Epitaph>> MORE_STREAM_CODEC = Epitaph.STREAM_CODEC.apply(ByteBufCodecs.list(MORE));

	private Epitaph epitaph = Epitaph.BLANK;
	/** Inscriptions 1 and on, without blank ones at the end. */
	private List<Epitaph> more = List.of();

	public HeadstoneBlockEntity(BlockPos pos, BlockState state) {
		this(JugcraftAgriculture.HEADSTONE_ENTITY, pos, state);
	}

	public HeadstoneBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	public Epitaph epitaph() {
		return epitaph;
	}

	/** Inscription {@code slot}: 0 is the epitaph; blank if none is cut there. */
	public Epitaph inscription(int slot) {
		if (slot == 0) {
			return epitaph;
		}
		return slot > 0 && slot <= more.size() ? more.get(slot - 1) : Epitaph.BLANK;
	}

	/** Every inscription after the epitaph, by slot from 1. */
	public List<Epitaph> more() {
		return more;
	}

	/** Cuts {@code cut} as the epitaph, replacing what was there, and sends it to clients. */
	public void engrave(Epitaph cut) {
		engrave(0, cut);
	}

	/** Cuts {@code cut} as inscription {@code slot} (0 to {@value #MORE}), replacing what was there, and sends it to clients. */
	public void engrave(int slot, Epitaph cut) {
		if (slot == 0) {
			epitaph = cut;
		} else if (slot > 0 && slot <= MORE) {
			List<Epitaph> out = new ArrayList<>(more);
			while (out.size() < slot) {
				out.add(Epitaph.BLANK);
			}
			out.set(slot - 1, cut);
			more = trimmed(out);
		} else {
			return;
		}
		setChanged();
		if (level != null) {
			BlockState state = getBlockState();
			level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
		}
	}

	private static List<Epitaph> trimmed(List<Epitaph> list) {
		int end = Math.min(list.size(), MORE);
		while (end > 0 && list.get(end - 1).isBlank()) {
			end--;
		}
		return List.copyOf(list.subList(0, end));
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		epitaph = input.read("epitaph", Epitaph.CODEC).orElse(Epitaph.BLANK);
		more = trimmed(input.read("inscriptions", MORE_CODEC).orElse(List.of()));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		if (!epitaph.isBlank()) {
			output.store("epitaph", Epitaph.CODEC, epitaph);
		}
		if (!more.isEmpty()) {
			output.store("inscriptions", MORE_CODEC, more);
		}
	}

	@Override
	protected void applyImplicitComponents(DataComponentGetter components) {
		super.applyImplicitComponents(components);
		Epitaph carried = components.get(JugcraftAgriculture.EPITAPH);
		Component name = components.get(DataComponents.CUSTOM_NAME);
		if (carried != null) {
			epitaph = carried;
		} else if (name != null) {
			epitaph = Epitaph.BLANK.withFirstLine(name.getString());
		}
		List<Epitaph> carriedMore = components.get(JugcraftAgriculture.INSCRIPTIONS);
		if (carriedMore != null) {
			more = trimmed(carriedMore);
		}
	}

	@Override
	protected void collectImplicitComponents(DataComponentMap.Builder components) {
		super.collectImplicitComponents(components);
		if (!epitaph.isBlank()) {
			components.set(JugcraftAgriculture.EPITAPH, epitaph);
		}
		if (!more.isEmpty()) {
			components.set(JugcraftAgriculture.INSCRIPTIONS, more);
		}
	}

	@Override
	public void removeComponentsFromTag(ValueOutput output) {
		super.removeComponentsFromTag(output);
		output.discard("epitaph");
		output.discard("inscriptions");
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
