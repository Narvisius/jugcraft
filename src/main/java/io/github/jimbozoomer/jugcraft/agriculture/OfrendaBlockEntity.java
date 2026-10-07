package io.github.jimbozoomer.jugcraft.agriculture;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Containers;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The offerings on an ofrenda: up to {@value #SLOTS}, two to a tier, set out from the top tier down and taken back from the
 * last. Anything in {@code jugcraft:ofrenda/offerings} may be offered. The ofrenda is complete with one of each of the
 * five kinds ({@link Kind}: flowers, a light, bread, a sugar skull, something to drink). Every {@value #CHECK_TICKS} ticks
 * a complete ofrenda, at night, welcomes the restless spirits within {@value #WELCOME_RANGE} blocks: each comes to it and
 * shows itself there, calm, until dawn ({@link RestlessSpirit#welcome}). When a welcomed spirit is within
 * {@value #ARRIVED} blocks, every player within {@value #WITNESS_RANGE} blocks earns Remembered.
 */
public class OfrendaBlockEntity extends BlockEntity {
	public static final int SLOTS = 6;
	public static final int CHECK_TICKS = 40;
	public static final int WELCOME_RANGE = 16;
	public static final double ARRIVED = 3.0;
	public static final int WITNESS_RANGE = 8;
	public static final TagKey<Item> OFFERINGS = tag("offerings");

	/** The five kinds of offering a complete ofrenda holds, each an item tag {@code jugcraft:ofrenda/<kind>}. */
	public enum Kind {
		FLOWERS, LIGHT, BREAD, SUGAR, DRINK;

		public final TagKey<Item> items = tag(name().toLowerCase(java.util.Locale.ROOT));
	}

	private final List<ItemStack> offerings = new ArrayList<>();

	public OfrendaBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftAgriculture.OFRENDA_ENTITY, pos, state);
	}

	private static TagKey<Item> tag(String path) {
		return TagKey.create(Registries.ITEM, Jugcraft.id("ofrenda/" + path));
	}

	public List<ItemStack> offerings() {
		return List.copyOf(offerings);
	}

	public static boolean offerable(ItemStack stack) {
		return !stack.isEmpty() && stack.is(OFFERINGS);
	}

	/** Sets one of {@code stack} out on the next free place; returns whether it did. */
	public boolean offer(ItemStack stack) {
		if (offerings.size() >= SLOTS || !offerable(stack)) {
			return false;
		}
		offerings.add(stack.copyWithCount(1));
		changed();
		return true;
	}

	/** Takes back the last offering set out, or nothing. */
	public ItemStack takeLast() {
		if (offerings.isEmpty()) {
			return ItemStack.EMPTY;
		}
		ItemStack last = offerings.removeLast();
		changed();
		return last;
	}

	/** Whether {@code offerings} hold one of each kind. */
	public static boolean complete(List<ItemStack> offerings) {
		for (Kind kind : Kind.values()) {
			if (offerings.stream().noneMatch(stack -> stack.is(kind.items))) {
				return false;
			}
		}
		return true;
	}

	public boolean complete() {
		return complete(offerings);
	}

	void serverTick(ServerLevel level, BlockState state) {
		if (level.getGameTime() % CHECK_TICKS != 0) {
			return;
		}
		boolean complete = complete();
		if (state.getValue(OfrendaBlock.COMPLETE) != complete) {
			level.setBlock(worldPosition, state.setValue(OfrendaBlock.COMPLETE, complete), Block.UPDATE_ALL);
		}
		if (complete && MourningAngelBlock.night(level)) {
			welcome(level);
		}
	}

	/** Welcomes the restless spirits near; when one has come, everyone near earns Remembered. Returns how many it called. */
	public int welcome(ServerLevel level) {
		Vec3 here = Vec3.atCenterOf(worldPosition);
		int called = 0;
		boolean arrived = false;
		for (RestlessSpirit spirit : level.getEntitiesOfClass(RestlessSpirit.class, new AABB(worldPosition).inflate(WELCOME_RANGE))) {
			spirit.welcome(level, worldPosition);
			called++;
			arrived |= spirit.position().distanceTo(here) <= ARRIVED;
		}
		if (arrived) {
			level.sendParticles(ParticleTypes.END_ROD, here.x, here.y + 1.0, here.z, 6, 0.4, 0.4, 0.4, 0.01);
			for (ServerPlayer player : level.players()) {
				if (!player.isSpectator() && player.position().distanceTo(here) <= WITNESS_RANGE) {
					TrickOrTreat.award(player, "remembered");
				}
			}
		}
		return called;
	}

	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		if (level != null) {
			for (ItemStack offering : offerings) {
				Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), offering);
			}
			offerings.clear();
		}
	}

	private void changed() {
		setChanged();
		if (level != null) {
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		offerings.clear();
		input.read("offerings", ItemStack.CODEC.listOf()).ifPresent(list -> list.stream().limit(SLOTS).forEach(offerings::add));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.store("offerings", ItemStack.CODEC.listOf(), List.copyOf(offerings));
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
