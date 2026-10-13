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
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A kitchen cabinet's {@value #SLOTS} slots, shown as a chest. While anyone has it open its doors stand open (they
 * creak open and click shut); like a chest, it is re-checked when an opener walks away. Hoppers and pipes reach it like
 * a chest; breaking it spills what is inside.
 */
public class KitchenCabinetBlockEntity extends BaseContainerBlockEntity {
	public static final int SLOTS = 27;

	private NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
	private final ContainerOpenersCounter openers = new ContainerOpenersCounter() {
		@Override
		protected void onOpen(Level level, BlockPos pos, BlockState state) {
			level.playSound(null, pos, SoundEvents.WOODEN_DOOR_OPEN, SoundSource.BLOCKS, 0.5F, 1.2F);
		}

		@Override
		protected void onClose(Level level, BlockPos pos, BlockState state) {
			level.playSound(null, pos, SoundEvents.WOODEN_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.5F, 1.2F);
		}

		@Override
		protected void openerCountChanged(Level level, BlockPos pos, BlockState state, int before, int now) {
			if (state.getBlock() instanceof KitchenCabinetBlock && state.getValue(KitchenCabinetBlock.OPEN) != now > 0) {
				level.setBlock(pos, state.setValue(KitchenCabinetBlock.OPEN, now > 0), Block.UPDATE_ALL);
			}
		}

		@Override
		public boolean isOwnContainer(Player player) {
			return player.containerMenu instanceof ChestMenu menu && menu.getContainer() == KitchenCabinetBlockEntity.this;
		}
	};

	public KitchenCabinetBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftAgriculture.KITCHEN_CABINET_ENTITY, pos, state);
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

	@Override
	protected Component getDefaultName() {
		return Component.translatable("container.jugcraft.kitchen_cabinet");
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
		return SLOTS;
	}

	@Override
	protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
		return ChestMenu.threeRows(containerId, inventory, this);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
		ContainerHelper.loadAllItems(input, items);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, items);
	}
}
