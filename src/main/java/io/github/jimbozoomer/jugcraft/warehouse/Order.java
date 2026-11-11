package io.github.jimbozoomer.jugcraft.warehouse;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * An order out of the warehouse (part W2): an amount of one item for a player, to the Goods Out window (Ready Storage)
 * or to pipes (the Output Port). The terminal keeps the line; the planner fetches a pallet holding the item to Ready
 * Storage's dispatch hatch, its pick cell takes off what is needed (as many pallets as it takes), and the order is
 * READY (in Ready Storage) or SENT (in the port's buffer) until it has been collected or has gone out (DONE).
 */
public final class Order {
	public static final int WINDOW = 0, PIPES = 1;
	public static final int QUEUED = 0, FETCHING = 1, PICKING = 2, READY = 3, SENT = 4, DONE = 5, CANCELLED = 6;
	public static final String[] STATES = {"QUEUED", "FETCHING", "PICKING", "READY", "SENT", "DONE", "CANCELLED"};

	public final int id;
	public final @Nullable UUID owner;
	public final String who;
	/** The item (count 1: the kind with its components). */
	public final ItemStack item;
	public final long amount;
	public int dest;
	public int state = QUEUED;
	/** How many have come off pallets for it so far. */
	public long filled;
	/** Game ticks: placed, its pallet fetched, at the hatch, ready (0: not yet). */
	public final long placed;
	public long fetched, atHatch, readyAt;
	/** Where it was placed (KIOSK, TERMINAL) or, for a stream's refill, the stream's number + 1 (else 0). */
	public final String from;
	public int stream;
	/** The pallet place being fetched for it now (null: none). */
	public @Nullable BlockPos pallet;
	/** Part W5: RUSH: it goes ahead of everything. */
	public boolean rush;

	public Order(int id, @Nullable UUID owner, String who, ItemStack item, long amount, int dest, long placed, String from) {
		this.id = id;
		this.owner = owner;
		this.who = who;
		this.item = item.copyWithCount(1);
		this.amount = amount;
		this.dest = dest;
		this.placed = placed;
		this.from = from;
	}

	public long remaining() {
		return Math.max(0, amount - filled);
	}

	/** Still to be picked (queued, fetching or picking). */
	public boolean open() {
		return state == QUEUED || state == FETCHING || state == PICKING;
	}

	/** Not finished with: open, or waiting to be collected or sent. */
	public boolean live() {
		return state < DONE;
	}

	public boolean sameItem(ItemStack other) {
		return ItemStack.isSameItemSameComponents(item, other);
	}

	private record Saved(int id, Optional<UUID> owner, String who, ItemStack item, long amount, int dest, int state, long filled, long placed,
			long fetched, long atHatch, long readyAt, String from, int stream, Optional<BlockPos> pallet, boolean rush) {
	}

	public static final Codec<Order> CODEC = RecordCodecBuilder.<Saved>create(i -> i.group(
			Codec.INT.fieldOf("id").forGetter(Saved::id), UUIDUtil.CODEC.optionalFieldOf("owner").forGetter(Saved::owner),
			Codec.STRING.fieldOf("who").forGetter(Saved::who), ItemStack.CODEC.fieldOf("item").forGetter(Saved::item),
			Codec.LONG.fieldOf("amount").forGetter(Saved::amount), Codec.INT.fieldOf("dest").forGetter(Saved::dest),
			Codec.INT.fieldOf("state").forGetter(Saved::state), Codec.LONG.fieldOf("filled").forGetter(Saved::filled),
			Codec.LONG.fieldOf("placed").forGetter(Saved::placed), Codec.LONG.fieldOf("fetched").forGetter(Saved::fetched),
			Codec.LONG.fieldOf("at_hatch").forGetter(Saved::atHatch), Codec.LONG.fieldOf("ready_at").forGetter(Saved::readyAt),
			Codec.STRING.fieldOf("from").forGetter(Saved::from), Codec.INT.fieldOf("stream").forGetter(Saved::stream),
			BlockPos.CODEC.optionalFieldOf("pallet").forGetter(Saved::pallet), Codec.BOOL.optionalFieldOf("rush", false).forGetter(Saved::rush))
			.apply(i, Saved::new))
			.xmap(s -> {
				Order o = new Order(s.id(), s.owner().orElse(null), s.who(), s.item(), s.amount(), s.dest(), s.placed(), s.from());
				o.state = s.state();
				o.filled = s.filled();
				o.fetched = s.fetched();
				o.atHatch = s.atHatch();
				o.readyAt = s.readyAt();
				o.stream = s.stream();
				o.pallet = s.pallet().orElse(null);
				o.rush = s.rush();
				return o;
			}, o -> new Saved(o.id, Optional.ofNullable(o.owner), o.who, o.item, o.amount, o.dest, o.state, o.filled, o.placed, o.fetched, o.atHatch,
					o.readyAt, o.from, o.stream, Optional.ofNullable(o.pallet), o.rush));

	/**
	 * A stream (the Output Port): an item sent out of the port all the time, up to rate a second, while the warehouse
	 * keeps at least reserve of it (HELD when it is down to that).
	 */
	public static final class Stream {
		public final ItemStack item;
		public int rate;
		public long reserve;
		public final @Nullable UUID owner;
		public final String who;
		/** Items it may still send this second (the port's allowance). */
		public double allowance;
		public boolean held;

		public Stream(ItemStack item, int rate, long reserve, @Nullable UUID owner, String who) {
			this.item = item.copyWithCount(1);
			this.rate = rate;
			this.reserve = reserve;
			this.owner = owner;
			this.who = who;
		}

		private record SavedStream(ItemStack item, int rate, long reserve, Optional<UUID> owner, String who) {
		}

		public static final Codec<Stream> CODEC = RecordCodecBuilder.<SavedStream>create(i -> i.group(
				ItemStack.CODEC.fieldOf("item").forGetter(SavedStream::item), Codec.INT.fieldOf("rate").forGetter(SavedStream::rate),
				Codec.LONG.fieldOf("reserve").forGetter(SavedStream::reserve), UUIDUtil.CODEC.optionalFieldOf("owner").forGetter(SavedStream::owner),
				Codec.STRING.fieldOf("who").forGetter(SavedStream::who)).apply(i, SavedStream::new))
				.xmap(s -> new Stream(s.item(), s.rate(), s.reserve(), s.owner().orElse(null), s.who()),
						s -> new SavedStream(s.item, s.rate, s.reserve, Optional.ofNullable(s.owner), s.who));
	}
}
