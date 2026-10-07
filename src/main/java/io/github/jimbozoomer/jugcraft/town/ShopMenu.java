package io.github.jimbozoomer.jugcraft.town;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Prediction;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

/**
 * A town shop's screen. The client lists the shop's offers from {@link TownShops} for the theme the shop opened in; the
 * player's Jugs come down as menu data. Buying and selling are menu buttons (vanilla's container button packet): button
 * {@code i} buys offer {@code i} of the shop's sales in that theme, button {@link #SELL_BASE}{@code + i} sells offer
 * {@code i} of what it buys. The server checks everything again: the shopkeeper still near ({@link TownShops#tradeRange}),
 * the offer still on sale, the price, the player's Jugs and items, and at most one trade every {@link #MIN_TICKS} ticks.
 */
public class ShopMenu extends AbstractContainerMenu {
	public static final int SELL_BASE = 64;
	public static final int MIN_TICKS = 2;
	private static final Map<UUID, Long> LAST_TRADE = new HashMap<>();

	/** What the client is told when a shop opens: which shop, its theme, and the shopkeeper. */
	public record Opening(String shop, String theme, int keeper) {
		public static final StreamCodec<RegistryFriendlyByteBuf, Opening> STREAM_CODEC = StreamCodec.composite(
				ByteBufCodecs.STRING_UTF8, Opening::shop, ByteBufCodecs.STRING_UTF8, Opening::theme, ByteBufCodecs.VAR_INT, Opening::keeper,
				Opening::new);
	}

	public final Opening opening;
	private final ContainerData data;
	private final @Nullable Townsfolk keeper;

	/** Client side. */
	public ShopMenu(int containerId, Inventory inventory, Opening opening) {
		this(containerId, inventory, opening, new SimpleContainerData(2), null);
	}

	private ShopMenu(int containerId, Inventory inventory, Opening opening, ContainerData data, @Nullable Townsfolk keeper) {
		super(JugcraftTown.SHOP_MENU, containerId);
		this.opening = opening;
		this.data = data;
		this.keeper = keeper;
		addDataSlots(data);
	}

	/** Opens a shopkeeper's shop for a player (server side). */
	public static void open(ServerPlayer player, Townsfolk keeper) {
		TownShops.Shop shop = TownShops.get().shop(keeper.shop());
		if (shop == null) {
			return;
		}
		Opening opening = new Opening(shop.id(), TownDecor.theme(), keeper.getId());
		MinecraftServer server = player.level().getServer();
		player.openMenu(new ExtendedMenuProvider<Opening>() {
			@Override
			public Opening getScreenOpeningData(ServerPlayer p) {
				return opening;
			}

			@Override
			public Component getDisplayName() {
				return Component.translatable("shop.jugcraft." + shop.id());
			}

			@Override
			public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player p) {
				return new ShopMenu(containerId, inventory, opening, balanceData(server, p.getUUID()), keeper);
			}
		});
	}

	/** A player's Jugs as two 16-bit menu values (menu data travels as shorts). */
	static ContainerData balanceData(MinecraftServer server, UUID player) {
		return new ContainerData() {
			@Override
			public int get(int index) {
				long balance = Jugs.balance(server, player);
				return (int) ((index == 0 ? balance : balance >>> 16) & 0xFFFF);
			}

			@Override
			public void set(int index, int value) {
			}

			@Override
			public int getCount() {
				return 2;
			}
		};
	}

	/** The player's Jugs as the client last heard. */
	public long balance() {
		return (data.get(0) & 0xFFFFL) | ((data.get(1) & 0xFFFFL) << 16);
	}

	public TownShops.@Nullable Shop shop() {
		return TownShops.get().shop(opening.shop());
	}

	/** The offers on sale, as listed (in the theme the shop opened in). */
	public List<TownShops.Offer> sales() {
		TownShops.Shop shop = shop();
		return shop == null ? List.of() : shop.sellsIn(opening.theme());
	}

	public List<TownShops.Offer> purchases() {
		TownShops.Shop shop = shop();
		return shop == null ? List.of() : shop.buys();
	}

	@Override
	public boolean stillValid(Player player) {
		if (keeper == null) {
			return true;
		}
		double range = TownShops.get().tradeRange;
		return keeper.isAlive() && keeper.distanceToSqr(player) <= range * range;
	}

	@Override
	public ItemStack quickMoveStack(Player player, int slot) {
		return ItemStack.EMPTY;
	}

	@Override
	public boolean clickMenuButton(Player player, int id) {
		if (!(player instanceof ServerPlayer server) || keeper == null || !stillValid(player)) {
			return false;
		}
		long now = server.level().getGameTime();
		Long last = LAST_TRADE.get(player.getUUID());
		if (last != null && now - last < MIN_TICKS && now >= last) {
			return false;
		}
		LAST_TRADE.put(player.getUUID(), now);
		if (id >= 0 && id < SELL_BASE) {
			List<TownShops.Offer> sales = sales();
			return id < sales.size() && buy(server, sales.get(id));
		}
		List<TownShops.Offer> purchases = purchases();
		int index = id - SELL_BASE;
		return index >= 0 && index < purchases.size() && sell(server, purchases.get(index));
	}

	/** The player buys an offer: the shop must still have it on sale today, and the player must have the Jugs. */
	public static boolean buy(ServerPlayer player, TownShops.Offer offer) {
		MinecraftServer server = player.level().getServer();
		if (!offer.onSale(TownDecor.theme())) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.shop.out_of_season"));
			return false;
		}
		Item item = offer.itemType();
		if (item == Items.AIR) {
			return false;
		}
		if (!Jugs.take(server, player.getUUID(), offer.price())) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.shop.too_few_jugs", offer.price()));
			return false;
		}
		int left = offer.count();
		int max = new ItemStack(item).getMaxStackSize();
		while (left > 0) {
			ItemStack stack = new ItemStack(item, Math.min(left, max));
			left -= stack.getCount();
			// Into the inventory, or dropped at the player's feet if it is full.
			player.getInventory().placeItemBackInInventory(stack, Prediction.SERVER_ONLY);
		}
		player.level().playSound(null, player.blockPosition(), SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 0.6F, 1.0F);
		player.sendOverlayMessage(Component.translatable("message.jugcraft.shop.bought", offer.count(), new ItemStack(item).getHoverName(),
				offer.price()));
		return true;
	}

	/** The player sells to the shop: they must carry the whole count, and their Jugs must not pass the maximum. */
	public static boolean sell(ServerPlayer player, TownShops.Offer offer) {
		MinecraftServer server = player.level().getServer();
		Item item = offer.itemType();
		if (item == Items.AIR) {
			return false;
		}
		Inventory inventory = player.getInventory();
		int have = 0;
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			if (inventory.getItem(slot).is(item)) {
				have += inventory.getItem(slot).getCount();
			}
		}
		if (have < offer.count()) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.shop.too_few_items", offer.count(), new ItemStack(item).getHoverName()));
			return false;
		}
		if (Jugs.balance(server, player.getUUID()) + offer.price() > TownShops.get().maxBalance) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.shop.purse_full"));
			return false;
		}
		int left = offer.count();
		for (int slot = 0; slot < inventory.getContainerSize() && left > 0; slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (stack.is(item)) {
				int take = Math.min(left, stack.getCount());
				stack.shrink(take);
				left -= take;
			}
		}
		inventory.setChanged();
		Jugs.add(server, player.getUUID(), offer.price());
		player.level().playSound(null, player.blockPosition(), SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 0.6F, 1.1F);
		player.sendOverlayMessage(Component.translatable("message.jugcraft.shop.sold", offer.count(), new ItemStack(item).getHoverName(),
				offer.price()));
		return true;
	}

	/** Forgets rate-limit records (when the server stops). */
	public static void clear() {
		LAST_TRADE.clear();
	}
}
