package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A Stone Sarcophagus's {@value #SLOTS} slots, kept in its head half and shown as a chest; the lid is off while anyone
 * has it open. Shut, at night, it knocks: once every {@value #CHECK_TICKS} ticks, with a player (not a spectator) within
 * {@value #RANGE} blocks, one chance in {@value #CHANCE}, and at most once every {@value #COOLDOWN_TICKS} ticks, it knocks
 * {@value #KNOCKS} times, {@value #GAP_TICKS} ticks apart, and the lid shudders on clients at each. The last knock's time
 * is saved, so reloading the chunk doesn't reset it.
 */
public class SarcophagusBlockEntity extends BaseContainerBlockEntity {
	public static final int SLOTS = 27;
	public static final double RANGE = 6.0;
	public static final int CHECK_TICKS = 20;
	public static final int CHANCE = 30;
	public static final int COOLDOWN_TICKS = 1200;
	public static final int KNOCKS = 3;
	public static final int GAP_TICKS = 10;

	private NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
	private long lastKnock = Long.MIN_VALUE / 2;
	private int knocksLeft;
	private long marked = Long.MIN_VALUE;
	/** The client's lid: how far it is pushed aside (0 shut, 1 open), and the time it was last moved toward its state. */
	public float lid;
	public float lidTime = -1.0F;
	private final ContainerOpenersCounter openers = new ContainerOpenersCounter() {
		@Override
		protected void onOpen(Level level, BlockPos pos, BlockState state) {
			level.playSound(null, pos, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.7F, 0.5F);
		}

		@Override
		protected void onClose(Level level, BlockPos pos, BlockState state) {
			level.playSound(null, pos, SoundEvents.DEEPSLATE_BRICKS_PLACE, SoundSource.BLOCKS, 1.0F, 0.6F);
		}

		@Override
		protected void openerCountChanged(Level level, BlockPos pos, BlockState state, int before, int now) {
			if (state.getBlock() instanceof SarcophagusBlock && state.getValue(SarcophagusBlock.OPEN) != now > 0) {
				SarcophagusBlock.setBoth(level, pos, state.setValue(SarcophagusBlock.OPEN, now > 0));
			}
		}

		@Override
		public boolean isOwnContainer(Player player) {
			return player.containerMenu instanceof ChestMenu menu && menu.getContainer() == SarcophagusBlockEntity.this;
		}
	};

	public SarcophagusBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftAgriculture.SARCOPHAGUS_TOMB_ENTITY, pos, state);
	}

	/** Whether a shut sarcophagus may start knocking now: at night, a player near, and none for a minute. */
	public static boolean mayKnock(boolean shut, boolean night, boolean playerNear, long now, long lastKnock) {
		return shut && night && playerNear && now - lastKnock >= COOLDOWN_TICKS;
	}

	public void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
		long now = level.getGameTime();
		if (knocksLeft > 0) {
			if ((now - lastKnock) % GAP_TICKS == 0) {
				knock(level, pos);
			}
			return;
		}
		if (now % CHECK_TICKS != Math.floorMod(pos.asLong(), CHECK_TICKS)) {
			return;
		}
		boolean near = level.getNearestPlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, RANGE, false) != null;
		if (mayKnock(!state.getValue(SarcophagusBlock.OPEN), MourningAngelBlock.night(level), near, now, lastKnock)
				&& level.getRandom().nextInt(CHANCE) == 0) {
			startKnocking(level, pos);
		}
	}

	/** Starts the three knocks now (for tests and the tick above). */
	public void startKnocking(ServerLevel level, BlockPos pos) {
		lastKnock = level.getGameTime();
		knocksLeft = KNOCKS;
		setChanged();
		knock(level, pos);
	}

	private void knock(ServerLevel level, BlockPos pos) {
		knocksLeft--;
		level.playSound(null, pos, SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, SoundSource.BLOCKS, 0.5F, 0.5F + level.getRandom().nextFloat() * 0.1F);
		level.blockEvent(pos, getBlockState().getBlock(), 1, 0);
	}

	public int knocksLeft() {
		return knocksLeft;
	}

	public long lastKnock() {
		return lastKnock;
	}

	/** Marks a knock on clients, for the lid to shudder from. */
	public void mark(long time) {
		marked = time;
	}

	public long marked() {
		return marked;
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
		return Component.translatable("container.jugcraft.sarcophagus");
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
		lastKnock = input.getLongOr("last_knock", Long.MIN_VALUE / 2);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, items);
		output.putLong("last_knock", lastKnock);
	}
}
