package io.github.jimbozoomer.jugcraft.concordance.spire;

import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import io.github.jimbozoomer.jugcraft.concordance.Saved;
import io.github.jimbozoomer.jugcraft.concordance.courier.CourierLedger;
import io.github.jimbozoomer.jugcraft.concordance.courier.Couriers;
import io.github.jimbozoomer.jugcraft.concordance.sign.Sign;
import io.github.jimbozoomer.jugcraft.concordance.sign.Signs;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * A Concord Spire's heart (roadmap step 25). The spire itself lives in the world's record ({@link SpireRecord}); the
 * heart holds only its store (nine slots for the upkeep item: couriers deliver here, hoppers and hands put things in),
 * who placed it, the configuration chosen before founding, the courier request it filed for its upkeep, and what it last
 * showed. Every {@value #CHECK_TICKS} ticks it looks at its spire ({@link ConcordSpire#work}). Broken, its store drops
 * and the requests bound for it are cancelled; the spire keeps its phase and answers again when a heart is put back.
 * Its store is the keeper's upkeep: anything may put into it through any face, but nothing takes out through one, so no
 * hopper, pipe, porter or courier can drain another player's spire (roadmap step 28); only its upkeep spends it.
 */
public class SpireHeartBlockEntity extends BlockEntity implements WorldlyContainer, GeoBlockEntity {
	public static final int SLOTS = 9;
	private static final int[] ALL_SLOTS = {0, 1, 2, 3, 4, 5, 6, 7, 8};
	/** How often the heart looks at its spire (ticks). */
	public static final int CHECK_TICKS = 20;
	private static final RawAnimation DORMANT = RawAnimation.begin().thenLoop("animation.spire_heart.dormant");
	private static final RawAnimation RAISING = RawAnimation.begin().thenLoop("animation.spire_heart.raising");
	private static final RawAnimation ACTIVE = RawAnimation.begin().thenLoop("animation.spire_heart.active");

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	private NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
	private @Nullable UUID owner;
	/** The configuration chosen before founding ("" the first). */
	private String selected = "";
	/** The open courier request for its upkeep (-1 none). */
	private long request = -1L;
	/** When its field last pulsed (not saved: the first pulse comes a pulse after the heart loads). */
	private long lastPulse = Long.MIN_VALUE;
	/** What it last showed: "unfounded", "raising", "active" or why its field rests; on clients, what the server sent. */
	private String status = "unfounded";

	public SpireHeartBlockEntity(BlockPos pos, BlockState state) {
		super(ConcordSpire.HEART_ENTITY, pos, state);
	}

	public @Nullable UUID owner() {
		return owner;
	}

	public String status() {
		return status;
	}

	public String selected() {
		return selected;
	}

	void select(String configuration) {
		selected = configuration;
		setChanged();
	}

	/** Placed: its placer owns it, and a spire already recorded here answers again. */
	void placed(ServerPlayer player, ServerLevel level) {
		owner = player.getUUID();
		setChanged();
		ConcordSpire.placed(player, level, worldPosition);
	}

	void serverTick(ServerLevel level) {
		long now = level.getGameTime();
		if (lastPulse == Long.MIN_VALUE) {
			lastPulse = now;
		}
		if (Math.floorMod(now + worldPosition.asLong(), CHECK_TICKS) == 0 && work(level, now).equals("active")) {
			ConcordSpire.pulse(level, this, now);
		}
	}

	/**
	 * One look at its spire at {@code now} (tests pass the time to stand for): its upkeep, its phases, its state; returns
	 * what it shows. Its field pulses only on the server's own ticks ({@link #serverTick}), never from here.
	 */
	public String work(ServerLevel level, long now) {
		String next = ConcordSpire.work(level, this, now);
		if (!next.equals(status)) {
			Sign sign = sign(status, next);
			if (sign != null) {
				Signs.show(level, worldPosition, sign);
			}
			status = next;
			setChanged();
			BlockState state = getBlockState();
			boolean lit = next.equals("active");
			if (state.getValue(SpireHeartBlock.LIT) != lit) {
				level.setBlock(worldPosition, state.setValue(SpireHeartBlock.LIT, lit), Block.UPDATE_ALL);
			}
			sync();
		}
		return status;
	}

	/**
	 * What a change of state shows (roadmap step 27), once, when it happens: danger when the spire is damaged, shortage
	 * when its upkeep or attendance lapses, success when its field works again (its raising shows at the phase).
	 */
	static @Nullable Sign sign(String before, String after) {
		return switch (after) {
			case "damaged" -> Sign.PERIL;
			case "unattended", "unsupplied" -> Sign.WANT;
			case "active" -> before.equals("raising") || before.equals("unfounded") ? null : Sign.DONE;
			default -> null;
		};
	}

	/** Whether its field may pulse at {@code now} (every {@code pulse} ticks), noting that it does. */
	boolean pulse(long now, int pulse) {
		if (now - lastPulse < pulse && now >= lastPulse) {
			return false;
		}
		lastPulse = now;
		return true;
	}

	public void use(ServerPlayer player, ServerLevel level, boolean sneaking) {
		ConcordSpire.use(player, level, this, sneaking);
	}

	/** Something used on it: the configuration's upkeep item goes into its store. Returns whether it was taken. */
	public boolean offer(ServerPlayer player, ServerLevel level, ItemStack stack) {
		return ConcordSpire.offer(player, level, this, stack);
	}

	// ---------------------------------------------------------------- the store

	/** How many of {@code item} its store holds. */
	public int count(String item) {
		int count = 0;
		for (ItemStack stack : items) {
			if (!stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(item)) {
				count += stack.getCount();
			}
		}
		return count;
	}

	/** Takes {@code amount} of {@code item} from its store (the day's upkeep, which was counted first). */
	void take(String item, int amount) {
		int left = amount;
		for (ItemStack stack : items) {
			if (left > 0 && !stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(item)) {
				int taken = Math.min(left, stack.getCount());
				stack.shrink(taken);
				left -= taken;
			}
		}
		setChanged();
	}

	/** Puts as much of {@code stack} as fits into its store; returns how much went in. */
	int store(ItemStack stack) {
		int before = stack.getCount();
		for (int slot = 0; slot < SLOTS && !stack.isEmpty(); slot++) {
			ItemStack held = items.get(slot);
			if (held.isEmpty()) {
				items.set(slot, stack.split(Math.min(stack.getCount(), stack.getMaxStackSize())));
			} else if (ItemStack.isSameItemSameComponents(held, stack) && held.getCount() < held.getMaxStackSize()) {
				int moved = Math.min(stack.getCount(), held.getMaxStackSize() - held.getCount());
				held.grow(moved);
				stack.shrink(moved);
			}
		}
		if (stack.getCount() != before) {
			setChanged();
		}
		return before - stack.getCount();
	}

	/** The open courier request for its upkeep (-1 none). */
	public long request() {
		return request;
	}

	void setRequest(long id) {
		request = id;
		setChanged();
	}

	/** Broken: its store drops once, and the courier requests bound for it are cancelled (their cargo goes back). */
	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		if (level instanceof ServerLevel server) {
			CourierLedger.of(server.getServer()).destinationGone(Couriers.place(server, pos), server.getGameTime());
			Containers.dropContents(server, pos, this);
		}
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		return ALL_SLOTS;
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
		return canPlaceItem(slot, stack);
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return false;
	}

	@Override
	public int getContainerSize() {
		return SLOTS;
	}

	@Override
	public boolean isEmpty() {
		return items.stream().allMatch(ItemStack::isEmpty);
	}

	@Override
	public ItemStack getItem(int slot) {
		return items.get(slot);
	}

	@Override
	public ItemStack removeItem(int slot, int count) {
		ItemStack taken = ContainerHelper.removeItem(items, slot, count);
		if (!taken.isEmpty()) {
			setChanged();
		}
		return taken;
	}

	@Override
	public ItemStack removeItemNoUpdate(int slot) {
		return ContainerHelper.takeItem(items, slot);
	}

	@Override
	public void setItem(int slot, ItemStack stack) {
		items.set(slot, stack);
		setChanged();
	}

	@Override
	public boolean stillValid(Player player) {
		return false;
	}

	@Override
	public void clearContent() {
		items.clear();
	}

	// ---------------------------------------------------------------- saving and clients

	private void sync() {
		if (level instanceof ServerLevel server) {
			server.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
		ContainerHelper.loadAllItems(input, items);
		owner = input.read("owner", UUIDUtil.CODEC).orElse(null);
		selected = input.getStringOr("selected", "");
		request = input.getLongOr("request", -1L);
		// Clients read the update tag here too: it carries only the status.
		status = input.getStringOr("status", "unfounded");
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		Saved.stamp(output, 1);
		ContainerHelper.saveAllItems(output, items);
		if (owner != null) {
			output.store("owner", UUIDUtil.CODEC, owner);
		}
		output.putString("selected", selected);
		output.putLong("request", request);
		output.putString("status", status);
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	/** Clients get what it shows (for its animation); never its owner or its store. */
	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		CompoundTag tag = new CompoundTag();
		tag.putString("status", status);
		return tag;
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<SpireHeartBlockEntity>("main", 10, test -> {
			String shown = test.animatable().status;
			return test.setAndContinue(shown.equals("active") ? ACTIVE : shown.equals("raising") ? RAISING : DORMANT);
		}));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}
}
