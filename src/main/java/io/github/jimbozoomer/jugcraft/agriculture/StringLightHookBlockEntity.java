package io.github.jimbozoomer.jugcraft.agriculture;

import io.github.jimbozoomer.jugcraft.energy.SimpleEnergyStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * A String Light Hook's strand and power. A hook holds at most one strand, to another hook at most
 * {@value #MAX_LENGTH} blocks away ({@link #link}): Jack-o'-Lantern String Lights or Bat Bunting ({@link Strand});
 * the client draws it from here. Every {@value #PERIOD} ticks the
 * hook lights if it has a redstone signal; failing that, if its buffer holds enough, it draws {@value #USE} JE a tick
 * from the electric network (buffer {@value #CAPACITY}, taking up to {@value #INPUT} a tick) and lights. Every
 * {@value #CHECK_TICKS} ticks a strand whose far hook is gone comes down (dropped as an item). Breaking the hook drops
 * its strand too.
 */
public class StringLightHookBlockEntity extends BlockEntity {
	public static final int MAX_LENGTH = 16;
	public static final int USE = 1;
	public static final int CAPACITY = 200;
	public static final int INPUT = 20;
	public static final int PERIOD = 10;
	public static final int CHECK_TICKS = 100;

	/** What can be strung between hooks, and the item each is. */
	public enum Strand {
		LIGHTS("lights", "jack_o_lantern_string_lights"),
		BUNTING("bunting", "bat_bunting"),
		/** Pumpkin Night (tools/decor20.py GARLANDS): a twisting pumpkin vine and a twine of autumn leaves, both with warm bulbs. */
		PUMPKIN_VINE("pumpkin_vine", "pumpkin_vine_garland"),
		AUTUMN_LEAVES("autumn_leaves", "autumn_leaf_garland");

		public final String name;
		public final String item;

		Strand(String name, String item) {
			this.name = name;
			this.item = item;
		}

		public static Strand byName(String name) {
			for (Strand strand : values()) {
				if (strand.name.equals(name)) {
					return strand;
				}
			}
			return LIGHTS;
		}
	}

	final SimpleEnergyStorage energy = new SimpleEnergyStorage(CAPACITY, INPUT, 0, this::setChanged);
	private @Nullable BlockPos link;
	private Strand strand = Strand.LIGHTS;

	public StringLightHookBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftAgriculture.STRING_LIGHT_HOOK_ENTITY, pos, state);
	}

	public SimpleEnergyStorage energy() {
		return energy;
	}

	/** The hook this one's strand runs to, or null. */
	public @Nullable BlockPos link() {
		return link;
	}

	/** What this hook's strand is (meaningful while it has one). */
	public Strand strand() {
		return strand;
	}

	/** Strings this hook to the hook at {@code other} with string lights (the caller has checked the rules). */
	public void stringTo(BlockPos other) {
		stringTo(other, Strand.LIGHTS);
	}

	/** Strings this hook to the hook at {@code other} with {@code strand} (the caller has checked the rules). */
	public void stringTo(BlockPos other, Strand strand) {
		link = other.immutable();
		this.strand = strand;
		changed();
	}

	/** Takes this hook's strand down, dropping it as an item if {@code drop}. */
	public void unstring(boolean drop) {
		if (link == null) {
			return;
		}
		link = null;
		if (drop && level != null) {
			Block.popResource(level, worldPosition, new ItemStack(JugcraftAgriculture.item(strand.item)));
		}
		changed();
	}

	/** Whether the strand between hooks {@code a} and {@code b} glows: either hook is lit. */
	public static boolean glows(BlockGetter level, BlockPos a, BlockPos b) {
		return lit(level.getBlockState(a)) || lit(level.getBlockState(b));
	}

	private static boolean lit(BlockState state) {
		return state.getBlock() instanceof StringLightHookBlock && state.getValue(StringLightHookBlock.LIT);
	}

	void serverTick(ServerLevel level) {
		long time = level.getGameTime() + worldPosition.asLong();
		if (Math.floorMod(time, PERIOD) == 0) {
			update(level);
		}
		if (link != null && Math.floorMod(time, CHECK_TICKS) == 0 && level.isLoaded(link)
				&& !(level.getBlockState(link).getBlock() instanceof StringLightHookBlock)) {
			unstring(true);
		}
	}

	/** Works out whether the hook is lit (redstone first, else electricity for the next {@value #PERIOD} ticks). */
	public boolean update(ServerLevel level) {
		boolean lit = level.hasNeighborSignal(worldPosition);
		if (!lit && energy.getAmount() >= (long) USE * PERIOD) {
			energy.setAmount(energy.getAmount() - (long) USE * PERIOD);
			setChanged();
			lit = true;
		}
		BlockState state = getBlockState();
		if (state.getValue(StringLightHookBlock.LIT) != lit) {
			level.setBlock(worldPosition, state.setValue(StringLightHookBlock.LIT, lit), Block.UPDATE_ALL);
		}
		return lit;
	}

	private void changed() {
		setChanged();
		if (level != null) {
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
		}
	}

	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		if (link != null && level != null) {
			Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), new ItemStack(JugcraftAgriculture.item(strand.item)));
			link = null;
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		link = input.getLong("link").map(BlockPos::of).orElse(null);
		strand = Strand.byName(input.getStringOr("strand", Strand.LIGHTS.name));
		energy.setAmount(input.getLong("energy").orElse(0L));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		if (link != null) {
			output.putLong("link", link.asLong());
			output.putString("strand", strand.name);
		}
		output.putLong("energy", energy.getAmount());
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
