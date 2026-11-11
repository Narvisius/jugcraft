package io.github.jimbozoomer.jugcraft.warehouse;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.FilteringStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The Intake Case Packer: what is piped into it (its back ports, the Goods In panel's inlets) or dropped in (the drop-off
 * bay, the Goods In screen) waits in its 54-slot bay; the machine packs it into cases, one kind of item to a case, up to
 * four stacks (a full case at once, a part case when nothing more of it has come for a while), and pushes them onto its
 * accumulation conveyor, where they run to the stop at its right end and queue for a stacker drone.
 */
public class IntakeBlockEntity extends BlockEntity implements PieceEntity, ExtendedMenuProvider<GoodsInMenu.Opening> {
	public static final int BAY = 54, CONVEYOR = 5, PACK_TICKS = 40, PART_CASE_AFTER = 100;
	/** The conveyor's speed (px a tick) and its ends (piece px along x), from the layout. */
	public static final float SPEED = 0.6F;
	private final SimpleContainer bay = new SimpleContainer(BAY) {
		@Override
		public void setChanged() {
			super.setChanged();
			IntakeBlockEntity.this.bayChanged();
		}
	};
	private final Storage<ItemVariant> input = new FilteringStorage<>(ContainerStorage.of(bay, null)) {
		@Override
		protected boolean canInsert(ItemVariant resource) {
			return !off;
		}

		@Override
		protected boolean canExtract(ItemVariant resource) {
			return false;
		}
	};
	/** Part W5: its warehouse's INTAKE OFF is on (a Signal Box): nothing comes in (as last looked, each second). */
	private boolean off;
	private final List<Conveyed> conveyor = new ArrayList<>();
	private @Nullable CaseLoad packing;
	private long packStart, lastInsert;
	private int packed;
	private boolean dirty;

	/** A case on the conveyor: its left edge at x0 (piece px) at tick t0, running on to its place in the queue. */
	public record Conveyed(CaseLoad load, float x0, long t0) {
		public static final com.mojang.serialization.Codec<Conveyed> CODEC = com.mojang.serialization.codecs.RecordCodecBuilder.create(i -> i.group(
				CaseLoad.CODEC.fieldOf("load").forGetter(Conveyed::load),
				com.mojang.serialization.Codec.FLOAT.fieldOf("x0").forGetter(Conveyed::x0),
				com.mojang.serialization.Codec.LONG.fieldOf("t0").forGetter(Conveyed::t0)).apply(i, Conveyed::new));
	}

	public IntakeBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftWarehouse.INTAKE_ENTITY, pos, state);
	}

	public SimpleContainer bay() {
		return bay;
	}

	/** Where piped and dropped-in items go (insert only). */
	public Storage<ItemVariant> input() {
		return input;
	}

	public List<Conveyed> conveyor() {
		return conveyor;
	}

	public @Nullable CaseLoad packing() {
		return packing;
	}

	public long packStart() {
		return packStart;
	}

	public int packed() {
		return packed;
	}

	private void bayChanged() {
		setChanged();
		if (level != null) {
			lastInsert = level.getGameTime();
		}
	}

	static float stopX() {
		return (float) WarehouseLayout.get().number("intake", "stop_x");
	}

	static float startX() {
		return (float) WarehouseLayout.get().number("intake", "start_x");
	}

	/** Queue place i's left edge (0 at the stop). */
	public static float slotX(int i) {
		return stopX() - (float) Kinematics.CASE - i * ((float) Kinematics.CASE + 0.4F);
	}

	/** Where case i on the conveyor is now (its left edge, piece px). */
	public float position(int i, double time) {
		Conveyed c = conveyor.get(i);
		return (float) Math.min(slotX(i), c.x0 + SPEED * (time - c.t0));
	}

	public Direction facing() {
		BlockState s = getBlockState();
		return s.getBlock() instanceof PieceBlock ? s.getValue(PieceBlock.FACING) : Direction.NORTH;
	}

	/** The case at the stop, if it has got there. */
	public @Nullable CaseLoad ready(long now) {
		return !conveyor.isEmpty() && position(0, now) >= slotX(0) - 1e-3 ? conveyor.get(0).load() : null;
	}

	/** The top middle of the case at the stop (world blocks). */
	public double[] headTop() {
		double y = WarehouseLayout.get().number("intake", "conveyor_y") + Kinematics.CASE_H;
		double z = WarehouseLayout.get().number("intake", "conveyor_z");
		return WarehousePiece.toWorld(WarehousePiece.INTAKE, facing(), worldPosition, slotX(0) + Kinematics.CASE / 2, y, z);
	}

	/** A stacker takes the case at the stop (Planner PICK_CASE): the rest run on up the queue. */
	public @Nullable CaseLoad takeHead(long now) {
		if (ready(now) == null) {
			return null;
		}
		List<Float> at = new ArrayList<>();
		for (int i = 1; i < conveyor.size(); i++) {
			at.add(position(i, now));
		}
		CaseLoad head = conveyor.remove(0).load();
		for (int i = 0; i < conveyor.size(); i++) {
			conveyor.set(i, new Conveyed(conveyor.get(i).load(), at.get(i), now));
		}
		dirty = true;
		return head;
	}

	public void serverTick(ServerLevel level) {
		long now = level.getGameTime();
		if (packing != null && now >= packStart + PACK_TICKS && conveyor.size() < CONVEYOR) {
			// The pusher puts the case out onto the conveyor's start (clear of the case before it).
			float x0 = startX();
			if (!conveyor.isEmpty() && position(conveyor.size() - 1, now) < x0 + Kinematics.CASE + 0.4F) {
				return;
			}
			conveyor.add(new Conveyed(packing, x0, now));
			packing = null;
			packed++;
			dirty = true;
			level.playSound(null, worldPosition, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 0.35F, 1.4F);
		}
		if (now % 20 == 0) {
			TerminalBlockEntity t = Warehouses.terminalOf(level, worldPosition);
			off = t != null && t.intakeOff();
		}
		// (Part W5: no new case while the STATIONS circuit is off or shed.)
		if (packing == null && conveyor.size() < CONVEYOR && now % 5 == 0 && WarehousePower.ok(level, worldPosition, WarehousePower.Circuit.STATIONS)) {
			pack(now);
		}
		if (dirty) {
			dirty = false;
			setChanged();
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
		}
	}

	/** Starts packing a case of the first kind in the bay: a full case, or what there is once nothing more has come for a while. */
	private void pack(long now) {
		for (int slot = 0; slot < BAY; slot++) {
			ItemStack first = bay.getItem(slot);
			if (first.isEmpty()) {
				continue;
			}
			int cap = CaseLoad.capacity(first), have = 0;
			for (int s = 0; s < BAY; s++) {
				if (ItemStack.isSameItemSameComponents(first, bay.getItem(s))) {
					have += bay.getItem(s).getCount();
				}
			}
			if (have < cap && now - lastInsert < PART_CASE_AFTER) {
				continue;
			}
			int take = Math.min(cap, have), left = take;
			ItemStack kind = first.copyWithCount(1);
			for (int s = BAY - 1; s >= 0 && left > 0; s--) {
				ItemStack st = bay.getItem(s);
				if (ItemStack.isSameItemSameComponents(kind, st)) {
					int n = Math.min(left, st.getCount());
					st.shrink(n);
					left -= n;
				}
			}
			long keep = lastInsert;
			bay.setChanged();
			lastInsert = keep;
			packing = new CaseLoad(kind, take);
			packStart = now;
			dirty = true;
			return;
		}
	}

	// ------------------------------------------------------------------ players

	@Override
	public InteractionResult use(Level level, BlockPos clicked, Player player, ItemStack stack, InteractionHand hand) {
		if (!level.isClientSide()) {
			player.openMenu(this);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable("container.jugcraft.goods_in");
	}

	@Override
	public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
		return new GoodsInMenu(id, inventory, bay, worldPosition, null);
	}

	@Override
	public GoodsInMenu.Opening getScreenOpeningData(ServerPlayer player) {
		return new GoodsInMenu.Opening(worldPosition, worldPosition);
	}

	@Override
	public void dropContents() {
		if (level == null) {
			return;
		}
		double x = worldPosition.getX() + 0.5, y = worldPosition.getY() + 0.5, z = worldPosition.getZ() + 0.5;
		Containers.dropContents(level, worldPosition, bay);
		List<CaseLoad> cases = new ArrayList<>();
		if (packing != null) {
			cases.add(packing);
		}
		conveyor.forEach(c -> cases.add(c.load()));
		for (CaseLoad c : cases) {
			for (ItemStack s : c.stacks()) {
				Containers.dropItemStack(level, x, y, z, s);
			}
		}
		packing = null;
		conveyor.clear();
	}

	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		dropContents();
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	/** The clients get the conveyor and what is being packed (to draw), not the bay. */
	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		CompoundTag tag = new CompoundTag();
		var ops = registries.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE);
		tag.put("conveyor", Conveyed.CODEC.listOf().encodeStart(ops, conveyor).getOrThrow());
		if (packing != null) {
			tag.put("packing", CaseLoad.CODEC.encodeStart(ops, packing).getOrThrow());
		}
		tag.putLong("pack_start", packStart);
		tag.putInt("packed", packed);
		return tag;
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		bay.clearContent();
		net.minecraft.core.NonNullList<ItemStack> items = net.minecraft.core.NonNullList.withSize(BAY, ItemStack.EMPTY);
		ContainerHelper.loadAllItems(input, items);
		for (int i = 0; i < BAY; i++) {
			bay.setItem(i, items.get(i));
		}
		conveyor.clear();
		conveyor.addAll(input.read("conveyor", Conveyed.CODEC.listOf()).orElse(List.of()));
		packing = input.read("packing", CaseLoad.CODEC).orElse(null);
		packStart = input.getLongOr("pack_start", 0);
		packed = input.getIntOr("packed", 0);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		net.minecraft.core.NonNullList<ItemStack> items = net.minecraft.core.NonNullList.withSize(BAY, ItemStack.EMPTY);
		for (int i = 0; i < BAY; i++) {
			items.set(i, bay.getItem(i));
		}
		ContainerHelper.saveAllItems(output, items);
		output.store("conveyor", Conveyed.CODEC.listOf(), conveyor);
		if (packing != null) {
			output.store("packing", CaseLoad.CODEC, packing);
		}
		output.putLong("pack_start", packStart);
		output.putInt("packed", packed);
	}
}
