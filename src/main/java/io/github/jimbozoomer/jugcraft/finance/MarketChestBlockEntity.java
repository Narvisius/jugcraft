package io.github.jimbozoomer.jugcraft.finance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * A Market Chest's stock and settings. Every two seconds (only while its chunk is loaded) it lists what it holds on the
 * Commodities market for its company, at the price set for each kind of item: the items move into the Exchange's
 * keeping, so buyers get them even when this chunk is not loaded. Goods with no price stay in the chest. "Take back"
 * returns unsold goods. Hoppers can fill it but not empty it, so its goods cannot be piped away.
 */
public class MarketChestBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer, ExtendedMenuProvider<BlockPos> {
	public static final int SIZE = 27;
	private static final int[] ALL_SLOTS;
	/** Who placed a chest that has not chosen its company yet, by {@link Market#chestRef}. */
	private static final int LIST_INTERVAL = 40;

	static {
		ALL_SLOTS = new int[SIZE];
		for (int i = 0; i < SIZE; i++) {
			ALL_SLOTS[i] = i;
		}
	}

	private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
	/** The company it sells for (0: none chosen yet). */
	private long corp;
	private boolean paused;
	/** Price per item, by item id. */
	private final Map<String, Long> prices = new LinkedHashMap<>();
	private int timer;

	public MarketChestBlockEntity(BlockPos pos, BlockState state) {
		super(ExchangeBlocks.MARKET_CHEST_ENTITY, pos, state);
	}

    private UUID placer;
    void setOwner(ServerPlayer player) {
        placer = player.getUUID();
        for (var company : Equities.get(player.level().getServer()).controlledBy(placer)) {
            if (choose(player.level().getServer(), company.id) == null) break;
        }
        setChanged();
    }

	public long corp() {
		return corp;
	}

	public boolean paused() {
		return paused;
	}

	public Map<String, Long> prices() {
		return prices;
	}

	public String ref() {
		return level == null ? "" : Market.chestRef(level, worldPosition);
	}

	// ------------------------------------------------------------------ who may use it

	/** True when {@code player} runs this chest: they control its company, or they are an operator. */
	boolean mayUse(ServerPlayer player) {
		if (FinanceService.isAdmin(player)) {
			return true;
		}
		if (corp <= 0) return player.getUUID().equals(placer);
		MinecraftServer server = player.level().getServer();
		Equities equities = Equities.get(server);
		Equities.Corporation company = equities.corp(corp);
		return company != null && player.getUUID().equals(equities.controller(company));
	}

	/** Opens the chest's screen for its runner; a new chest first takes the placer's (or opener's) company. */
	void open(ServerPlayer player) {
		MinecraftServer server = player.level().getServer();
		String refusal = null;
		if (corp <= 0) {
			if (!player.getUUID().equals(placer) && !FinanceService.isAdmin(player)) return;
			UUID chooser = placer != null ? placer : player.getUUID();
			for (Equities.Corporation company : Equities.get(server).controlledBy(chooser)) {
				refusal = choose(server, company.id);
				if (refusal == null) {
					break;
				}
			}
		}
		Equities.Corporation company = Equities.get(server).corp(corp);
		if (!mayUse(player)) {
			player.sendOverlayMessage(Component.literal(refusal != null ? refusal : company == null
					? "Only a player who controls a company can run a Market Chest."
					: "This Market Chest belongs to " + company.name + "."));
			return;
		}
		player.openMenu(this);
		FinanceService.sendChest(player, this, null);
	}

	/** Makes the chest sell for {@code corpId}; null when done, else why not (the company's chest limit). */
	@Nullable
	String choose(MinecraftServer server, long corpId) {
		if (corpId == corp) {
			return null;
		}
		Market market = Market.get(server);
		long limit = ExchangeSettings.value(Ledger.get(server).book(), ExchangeSettings.CHESTS);
		if (market.chestsOf(corpId) >= limit) {
			return "That company already runs " + limit + " Market Chests.";
		}
		// Goods listed for the old company come back before the chest changes hands.
		market.takeBack(ref(), this::insert);
        if (!market.chestListings(ref()).isEmpty()) return "Make room and take back the old company\'s remaining goods first.";
		corp = corpId;
		market.setChest(ref(), corpId);
		setChanged();
		return null;
	}

	void setPrice(String itemKey, long price) {
		if (price > 0) {
			prices.put(itemKey, price);
		} else {
			prices.remove(itemKey);
		}
		setChanged();
	}

	void setPaused(boolean paused) {
		this.paused = paused;
		setChanged();
	}

	// ------------------------------------------------------------------ selling

	void serverTick(ServerLevel level) {
		if (++timer % LIST_INTERVAL != 0 || corp <= 0 || paused || prices.isEmpty()) {
			return;
		}
		MinecraftServer server = level.getServer();
		Equities.Corporation company = Equities.get(server).corp(corp);
		if (company == null) {
			return;
		}
		Ledger ledger = Ledger.get(server);
		Market market = Market.get(server);
		long day = FinanceService.day(server);
		boolean changed = false;
		for (ItemStack stack : items) {
			if (stack.isEmpty()) {
				continue;
			}
			Long price = prices.get(Market.itemKey(stack));
			if (price == null) {
				continue;
			}
			int listed = market.listFromChest(server, ledger.book(), company, ref(), stack, price, day);
			if (listed > 0) {
				stack.shrink(listed);
				changed = true;
			}
		}
		if (changed) {
			setChanged();
			ledger.setDirty();
			FinanceService.announce(server, ledger);
		}
	}

	/** Puts as much of {@code offer} as fits into the chest; returns how many went in. */
	int insert(ItemStack offer) {
		int left = offer.getCount();
		int max = Math.max(1, offer.getMaxStackSize());
		for (int pass = 0; pass < 2 && left > 0; pass++) {
			for (int slot = 0; slot < SIZE && left > 0; slot++) {
				ItemStack held = items.get(slot);
				if (pass == 0 && !held.isEmpty() && ItemStack.isSameItemSameComponents(held, offer)) {
					int put = Math.min(left, max - held.getCount());
					if (put > 0) {
						held.grow(put);
						left -= put;
					}
				} else if (pass == 1 && held.isEmpty()) {
					int put = Math.min(left, max);
					items.set(slot, offer.copyWithCount(put));
					left -= put;
				}
			}
		}
		setChanged();
		return offer.getCount() - left;
	}

	/** Item kinds this chest deals in: what it holds, what it has listed, and what has a price. */
	List<String> kinds(Market market) {
		List<String> out = new ArrayList<>();
		for (ItemStack stack : items) {
			if (!stack.isEmpty() && !out.contains(Market.itemKey(stack))) {
				out.add(Market.itemKey(stack));
			}
		}
		for (Market.Listing listing : market.chestListings(ref())) {
			if (!out.contains(Market.itemKey(listing.item))) {
				out.add(Market.itemKey(listing.item));
			}
		}
		for (String key : prices.keySet()) {
			if (!out.contains(key)) {
				out.add(key);
			}
		}
		return out;
	}

	int inChest(String itemKey) {
		int count = 0;
		for (ItemStack stack : items) {
			if (!stack.isEmpty() && Market.itemKey(stack).equals(itemKey)) {
				count += stack.getCount();
			}
		}
		return count;
	}

	ItemStack sample(String itemKey, Market market) {
		for (ItemStack stack : items) {
			if (!stack.isEmpty() && Market.itemKey(stack).equals(itemKey)) {
				return stack.copyWithCount(1);
			}
		}
		for (Market.Listing listing : market.chestListings(ref())) {
			if (Market.itemKey(listing.item).equals(itemKey)) {
				return listing.item.copyWithCount(1);
			}
		}
		return ItemStack.EMPTY;
	}

	// ------------------------------------------------------------------ breaking

	/** Breaking the chest takes its goods off the market and drops them with what it holds. */
	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		if (level instanceof ServerLevel server) {
			Market market = Market.get(server.getServer());
			List<ItemStack> drops = new ArrayList<>();
			market.takeBack(ref(), offer -> {
				int max = Math.max(1, offer.getMaxStackSize());
				for (int left = offer.getCount(); left > 0; left -= max) {
					drops.add(offer.copyWithCount(Math.min(max, left)));
				}
				return offer.getCount();
			});
			market.setChest(ref(), 0);
			for (ItemStack stack : drops) {
				Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
			}
		}
		super.preRemoveSideEffects(pos, state);
	}

	// ------------------------------------------------------------------ container

	@Override
	protected Component getDefaultName() {
		return Component.translatable("container.jugcraft.market_chest");
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
		return SIZE;
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		return ALL_SLOTS;
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
		return true;
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return false;
	}

	@Override
	protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
		return new MarketChestMenu(containerId, inventory, this, worldPosition);
	}

	@Override
	public BlockPos getScreenOpeningData(ServerPlayer player) {
		return worldPosition;
	}

	// ------------------------------------------------------------------ saving

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
		ContainerHelper.loadAllItems(input, items);
		corp = input.getLongOr("corp", 0L);
        try { placer = UUID.fromString(input.getStringOr("placer", "")); } catch (IllegalArgumentException ignored) { placer = null; }
		paused = input.getBooleanOr("paused", false);
		prices.clear();
		for (String entry : input.getStringOr("prices", "").split(";")) {
			int eq = entry.lastIndexOf('=');
			if (eq > 0) {
				try {
					prices.put(entry.substring(0, eq), Long.parseLong(entry.substring(eq + 1)));
				} catch (NumberFormatException ignored) {
					// skip a damaged entry
				}
			}
		}
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, items);
		output.putLong("corp", corp);
        output.putString("placer", placer == null ? "" : placer.toString());
		output.putBoolean("paused", paused);
		StringBuilder text = new StringBuilder();
		prices.forEach((key, price) -> text.append(key).append('=').append(price).append(';'));
		output.putString("prices", text.toString());
	}
}
