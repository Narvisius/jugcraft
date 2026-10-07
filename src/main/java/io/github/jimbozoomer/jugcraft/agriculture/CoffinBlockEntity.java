package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The Coffin's {@value #SLOTS} slots, kept in its head half and shown as a chest. While anyone has it open the lid is
 * up on both halves (it creaks open and thuds shut); like a chest, it is re-checked when an opener walks away. Hoppers
 * and pipes reach it like a chest; breaking it spills what is inside. The Iron-Bound Coffin's chest
 * ({@link IronBoundCoffinBlockEntity}) is one of these with more slots.
 */
public class CoffinBlockEntity extends BaseContainerBlockEntity {
	public static final int SLOTS = 27;

	private final int slots;
	private NonNullList<ItemStack> items;
	private final ContainerOpenersCounter openers = new ContainerOpenersCounter() {
		@Override
		protected void onOpen(Level level, BlockPos pos, BlockState state) {
			level.playSound(null, pos, SoundEvents.WOODEN_DOOR_OPEN, SoundSource.BLOCKS, 0.8F, 0.5F);
		}

		@Override
		protected void onClose(Level level, BlockPos pos, BlockState state) {
			level.playSound(null, pos, SoundEvents.WOODEN_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.8F, 0.5F);
		}

		@Override
		protected void openerCountChanged(Level level, BlockPos pos, BlockState state, int before, int now) {
			setLid(level, pos, state, now > 0);
		}

		@Override
		public boolean isOwnContainer(Player player) {
			return player.containerMenu instanceof ChestMenu menu && menu.getContainer() == CoffinBlockEntity.this;
		}
	};

	public CoffinBlockEntity(BlockPos pos, BlockState state) {
		this(JugcraftAgriculture.COFFIN_ENTITY, pos, state, SLOTS);
	}

	protected CoffinBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int slots) {
		super(type, pos, state);
		this.slots = slots;
		this.items = NonNullList.withSize(slots, ItemStack.EMPTY);
	}

	/** Lifts or lowers the lid on both halves. */
	private static void setLid(Level level, BlockPos head, BlockState state, boolean open) {
		if (state.getBlock() instanceof CoffinBlock && state.getValue(CoffinBlock.OPEN) != open) {
			level.setBlock(head, state.setValue(CoffinBlock.OPEN, open), Block.UPDATE_ALL);
		}
		BlockPos foot = head.relative(state.getValue(CoffinBlock.FACING).getOpposite());
		BlockState footState = level.getBlockState(foot);
		if (footState.getBlock() instanceof CoffinBlock && footState.getValue(CoffinBlock.OPEN) != open) {
			level.setBlock(foot, footState.setValue(CoffinBlock.OPEN, open), Block.UPDATE_ALL);
		}
	}

	@Override
	public void startOpen(ContainerUser user) {
		if (!remove && !user.getLivingEntity().isSpectator() && level != null) {
			openers.incrementOpeners(user.getLivingEntity(), level, worldPosition, getBlockState(), user.getContainerInteractionRange());
		}
	}

	@Override
	public void stopOpen(ContainerUser user) {
		if (!remove && !user.getLivingEntity().isSpectator() && level != null) {
			openers.decrementOpeners(user.getLivingEntity(), level, worldPosition, getBlockState());
		}
	}

	@Override
	public List<ContainerUser> getEntitiesWithContainerOpen() {
		return level == null ? List.of() : openers.getEntitiesWithContainerOpen(level, worldPosition);
	}

	public void recheckOpen() {
		if (!remove && level != null) {
			openers.recheckOpeners(level, worldPosition, getBlockState());
		}
	}

	public int openers() {
		return openers.getOpenerCount();
	}

	@Override
	protected Component getDefaultName() {
		return Component.translatable("container.jugcraft.coffin");
	}

	@Override
	protected NonNullList<ItemStack> getItems() {
		return items;
	}

	@Override
	protected void setItems(NonNullList<ItemStack> items) {
		this.items = items;
	}

	@Override
	public int getContainerSize() {
		return slots;
	}

	@Override
	protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
		return slots > SLOTS ? ChestMenu.sixRows(containerId, inventory, this) : ChestMenu.threeRows(containerId, inventory, this);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		items = NonNullList.withSize(slots, ItemStack.EMPTY);
		ContainerHelper.loadAllItems(input, items);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, items);
	}
}
