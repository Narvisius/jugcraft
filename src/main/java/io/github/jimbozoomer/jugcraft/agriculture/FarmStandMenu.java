package io.github.jimbozoomer.jugcraft.agriculture;

import io.github.jimbozoomer.jugcraft.town.Jugs;
import io.github.jimbozoomer.jugcraft.town.TownShops;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Prediction;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The Farm Stand's screen: its {@value FarmStandBlockEntity#CRATES} crates in a row, each with its price in Jugs, and the
 * player's inventory. Only the owner can put goods in or take them out (the crate slots refuse everyone else, on both
 * sides). Everything else is a menu button (vanilla's container button packet): button {@code i} (0 to 5) buys one item
 * from crate {@code i}; the owner's button {@link #PRICE_BASE}{@code + crate * 8 + step * 2 + (up ? 1 : 0)} moves that
 * crate's price down or up by {@link #PRICE_STEPS}{@code [step]}. The server checks every button again: the stand still
 * there and within {@value #REACH} blocks of the player's eyes, the owner's buttons pressed by the owner, the crate
 * stocked, the buyer not the owner and holding the price, the owner's purse not past the town's maximum, and at most one
 * purchase every {@value #RATE_TICKS} ticks; then {@link Jugs#transfer} moves the Jugs.
 */
public class FarmStandMenu extends AbstractContainerMenu {
	public static final double REACH = 6.0;
	public static final int RATE_TICKS = 4;
	public static final long[] PRICE_STEPS = {1, 10, 100, 1000};
	public static final int PRICE_BASE = 8;
	public static final int CRATE_X = 14;
	public static final int CRATE_STEP = 28;
	public static final int CRATE_Y = 22;
	public static final int INVENTORY_Y = 112;
	private static final int CRATES = FarmStandBlockEntity.CRATES;
	private static final int DATA_COUNT = 2 + CRATES * 2;
	private static final String MESSAGES = "message.jugcraft.farm_stand.";
	private static final Map<UUID, Long> LAST_BUY = new HashMap<>();

	/** What the client is told when the stand opens: where its first block is, whose it is, and whether it is the viewer's. */
	public record Opening(BlockPos stand, String ownerName, boolean owner) {
		public static final StreamCodec<RegistryFriendlyByteBuf, Opening> STREAM_CODEC = StreamCodec.composite(
				BlockPos.STREAM_CODEC, Opening::stand, ByteBufCodecs.STRING_UTF8, Opening::ownerName, ByteBufCodecs.BOOL, Opening::owner,
				Opening::new);
	}

	public final Opening opening;
	private final ContainerData data;
	private final @Nullable FarmStandBlockEntity stand;

	/** Client side: the crates arrive through slot syncing, the prices and the player's Jugs as menu data. */
	public FarmStandMenu(int containerId, Inventory inventory, Opening opening) {
		this(containerId, inventory, opening, new SimpleContainer(CRATES), new SimpleContainerData(DATA_COUNT), null);
	}

	private FarmStandMenu(int containerId, Inventory inventory, Opening opening, Container crates, ContainerData data,
			@Nullable FarmStandBlockEntity stand) {
		super(JugcraftAgriculture.FARM_STAND_MENU, containerId);
		checkContainerSize(crates, CRATES);
		checkContainerDataCount(data, DATA_COUNT);
		this.opening = opening;
		this.data = data;
		this.stand = stand;
		for (int crate = 0; crate < CRATES; crate++) {
			addSlot(new Slot(crates, crate, CRATE_X + crate * CRATE_STEP, CRATE_Y) {
				@Override
				public boolean mayPlace(ItemStack stack) {
					return opening.owner();
				}

				@Override
				public boolean mayPickup(Player player) {
					return opening.owner();
				}
			});
		}
		addStandardInventorySlots(inventory, 8, INVENTORY_Y);
		addDataSlots(data);
	}

	/** Opens a stand for a player (server side). */
	public static void open(ServerPlayer player, FarmStandBlockEntity stand) {
		MinecraftServer server = player.level().getServer();
		Opening opening = new Opening(stand.getBlockPos().immutable(), stand.ownerName(), stand.isOwner(player));
		player.openMenu(new ExtendedMenuProvider<Opening>() {
			@Override
			public Opening getScreenOpeningData(ServerPlayer p) {
				return opening;
			}

			@Override
			public Component getDisplayName() {
				return Component.translatable("container.jugcraft.farm_stand");
			}

			@Override
			public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player p) {
				return new FarmStandMenu(containerId, inventory, opening, stand.crates(), data(server, p.getUUID(), stand), stand);
			}
		});
	}

	/** The viewer's Jugs, then each crate's price, as pairs of 16-bit menu values (menu data travels as shorts). */
	private static ContainerData data(MinecraftServer server, UUID player, FarmStandBlockEntity stand) {
		return new ContainerData() {
			@Override
			public int get(int index) {
				long value = index < 2 ? Jugs.balance(server, player) : stand.price((index - 2) / 2);
				return (int) ((index % 2 == 0 ? value : value >>> 16) & 0xFFFF);
			}

			@Override
			public void set(int index, int value) {
			}

			@Override
			public int getCount() {
				return DATA_COUNT;
			}
		};
	}

	private long pair(int index) {
		return (data.get(index) & 0xFFFFL) | ((data.get(index + 1) & 0xFFFFL) << 16);
	}

	/** The player's Jugs as the client last heard. */
	public long balance() {
		return pair(0);
	}

	/** The price chalked on a crate as the client last heard. */
	public long price(int crate) {
		return pair(2 + crate * 2);
	}

	/** The button that moves a crate's price by step {@code step} of {@link #PRICE_STEPS}, up or down. */
	public static int priceButton(int crate, int step, boolean up) {
		return PRICE_BASE + crate * PRICE_STEPS.length * 2 + step * 2 + (up ? 1 : 0);
	}

	/** Whether {@code player}'s eyes are within {@link #REACH} of the middle of either block of the stand. */
	public static boolean inReach(Player player, FarmStandBlockEntity stand) {
		BlockState state = stand.getBlockState();
		if (!(state.getBlock() instanceof FarmStandBlock block)) {
			return false;
		}
		Vec3 eyes = player.getEyePosition();
		for (int part = 0; part < block.cells().length; part++) {
			BlockPos pos = block.partPos(stand.getBlockPos(), state.getValue(FarmStandBlock.FACING), part);
			if (eyes.distanceToSqr(Vec3.atCenterOf(pos)) <= REACH * REACH) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean stillValid(Player player) {
		if (stand == null) {
			return true;
		}
		return !stand.isRemoved() && stand.getLevel() == player.level() && stand.getLevel().getBlockEntity(stand.getBlockPos()) == stand
				&& inReach(player, stand);
	}

	@Override
	public boolean clickMenuButton(Player player, int id) {
		if (!(player instanceof ServerPlayer buyer) || stand == null || !stillValid(player)) {
			return false;
		}
		if (id >= 0 && id < CRATES) {
			long now = buyer.level().getGameTime();
			Long last = LAST_BUY.get(player.getUUID());
			if (last != null && now - last < RATE_TICKS && now >= last) {
				return false;
			}
			LAST_BUY.put(player.getUUID(), now);
			return buy(buyer, stand, id);
		}
		int index = id - PRICE_BASE;
		if (index < 0 || index >= CRATES * PRICE_STEPS.length * 2 || !stand.isOwner(player)) {
			return false;
		}
		int crate = index / (PRICE_STEPS.length * 2);
		long step = PRICE_STEPS[index % (PRICE_STEPS.length * 2) / 2];
		stand.setPrice(crate, stand.price(crate) + (index % 2 == 1 ? step : -step));
		return true;
	}

	/**
	 * {@code buyer} buys one item from a crate for its price: the crate must hold something, the buyer must not own the
	 * stand and must have the Jugs, and the owner's purse must have room for them.
	 */
	public static boolean buy(ServerPlayer buyer, FarmStandBlockEntity stand, int crate) {
		MinecraftServer server = buyer.level().getServer();
		UUID owner = stand.owner();
		if (owner == null || crate < 0 || crate >= CRATES) {
			return false;
		}
		if (owner.equals(buyer.getUUID())) {
			buyer.sendOverlayMessage(Component.translatable(MESSAGES + "own"));
			return false;
		}
		ItemStack goods = stand.crate(crate);
		if (goods.isEmpty()) {
			buyer.sendOverlayMessage(Component.translatable(MESSAGES + "empty"));
			return false;
		}
		long price = stand.price(crate);
		if (Jugs.balance(server, buyer.getUUID()) < price) {
			buyer.sendOverlayMessage(Component.translatable(MESSAGES + "poor", price));
			return false;
		}
		if (Jugs.balance(server, owner) + price > TownShops.get().maxBalance) {
			buyer.sendOverlayMessage(Component.translatable(MESSAGES + "owner_full", stand.ownerName()));
			return false;
		}
		if (!Jugs.transfer(server, buyer.getUUID(), owner, price)) {
			return false;
		}
		ItemStack bought = stand.crates().removeItem(crate, 1);
		Component name = bought.getHoverName();
		buyer.getInventory().placeItemBackInInventory(bought, Prediction.SERVER_ONLY);
		buyer.level().playSound(null, stand.getBlockPos(), SoundEvents.VILLAGER_YES, SoundSource.BLOCKS, 0.5F, 1.2F);
		buyer.sendOverlayMessage(Component.translatable(MESSAGES + "bought", name, price));
		return true;
	}

	/** Only the owner shift-clicks goods between the crates and their inventory. */
	@Override
	public ItemStack quickMoveStack(Player player, int slotIndex) {
		Slot slot = slots.get(slotIndex);
		if (!opening.owner() || slot == null || !slot.hasItem()) {
			return ItemStack.EMPTY;
		}
		ItemStack stack = slot.getItem();
		ItemStack original = stack.copy();
		if (slotIndex < CRATES) {
			if (!moveItemStackTo(stack, CRATES, CRATES + 36, true)) {
				return ItemStack.EMPTY;
			}
		} else if (!moveItemStackTo(stack, 0, CRATES, false)) {
			return ItemStack.EMPTY;
		}
		if (stack.isEmpty()) {
			slot.setByPlayer(ItemStack.EMPTY);
		} else {
			slot.setChanged();
		}
		if (stack.getCount() == original.getCount()) {
			return ItemStack.EMPTY;
		}
		slot.onTake(player, stack);
		return original;
	}

	/** Forgets rate-limit records (when the server stops). */
	public static void clear() {
		LAST_BUY.clear();
	}
}
