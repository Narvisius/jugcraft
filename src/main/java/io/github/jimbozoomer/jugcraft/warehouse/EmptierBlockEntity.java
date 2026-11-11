package io.github.jimbozoomer.jugcraft.warehouse;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The Drum Emptying Station (part W4), by Ready Storage: a pallet drone sets an order's pallet of drums on its bunded
 * platform (Planner PUT_EMPTIER); its jib swings the drum pump from drum to drum (EmptierMotion) and pumps what the order
 * takes into its holding tank (WORKING), the least full drums first so the pallet's drums run empty one at a time;
 * then the pallet waits on the platform (BED) for a pallet drone to take it back to its place, or, its drums all
 * empty, to a Drum Filling Station (or, none taking it, into Ready Storage as drums and a pallet). The holding tank
 * keeps one liquid; what is in it is shared out among the orders it was pumped for: the Liquids Out panel behind it
 * gives it out (an order for the fill point into buckets, one for pipes through its OUT flange).
 */
public class EmptierBlockEntity extends BlockEntity implements PieceEntity, AnalogSource {
	public static final int IDLE = 0, WORKING = 1, BED = 2;
	public static final long TANK = 80_000;
	/** How long a pallet of emptied drums waits for a filling station before Ready Storage takes them. */
	public static final int EMPTY_WAIT = 300;
	private int state = IDLE;
	private long start;
	private @Nullable PalletLoad pallet;
	private EmptierMotion.@Nullable Plan plan;
	private int order;
	private @Nullable BlockPos from;
	private long claimed;
	public final Tank tank = new Tank();
	/** The tank's liquid by the orders it was pumped for (droplets). */
	private final List<Share> shares = new ArrayList<>();

	/** Liquid in the tank for an order: its number, its owner, WINDOW (the fill point) or PIPES, how much (droplets). */
	public static final class Share {
		public final int order;
		public final @Nullable UUID owner;
		public int dest;
		public long amount;
		/** The stream it refills (TerminalBlockEntity.LIQUID_STREAM + 1 + its index), 0 none. */
		public int stream;

		Share(int order, @Nullable UUID owner, int dest, long amount, int stream) {
			this.order = order;
			this.owner = owner;
			this.dest = dest;
			this.amount = amount;
			this.stream = stream;
		}

		private record Saved(int order, Optional<UUID> owner, int dest, long amount, int stream) {
		}

		static final Codec<Share> CODEC = RecordCodecBuilder.<Saved>create(i -> i.group(Codec.INT.fieldOf("order").forGetter(Saved::order),
				UUIDUtil.CODEC.optionalFieldOf("owner").forGetter(Saved::owner), Codec.INT.fieldOf("dest").forGetter(Saved::dest),
				Codec.LONG.fieldOf("amount").forGetter(Saved::amount), Codec.INT.fieldOf("stream").forGetter(Saved::stream)).apply(i, Saved::new))
				.xmap(s -> new Share(s.order(), s.owner().orElse(null), s.dest(), s.amount(), s.stream()),
						s -> new Saved(s.order, Optional.ofNullable(s.owner), s.dest, s.amount, s.stream));
	}

	/** The holding tank: filled only by the pump; the Liquids Out panel takes from it (Liquids.out). */
	public final class Tank extends SingleVariantStorage<FluidVariant> {
		@Override
		protected FluidVariant getBlankVariant() {
			return FluidVariant.blank();
		}

		@Override
		protected long getCapacity(FluidVariant variant) {
			return Drums.droplets(TANK);
		}

		@Override
		protected boolean canInsert(FluidVariant variant) {
			return false;
		}

		@Override
		protected void onFinalCommit() {
			setChanged();
		}
	}

	public EmptierBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftWarehouse.EMPTIER_ENTITY, pos, state);
	}

	public int state() {
		return state;
	}

	public long start() {
		return start;
	}

	public @Nullable PalletLoad pallet() {
		return pallet;
	}

	public EmptierMotion.@Nullable Plan plan() {
		return plan;
	}

	public int orderId() {
		return order;
	}

	public @Nullable BlockPos from() {
		return from;
	}

	public List<Share> shares() {
		return shares;
	}

	public FluidVariant liquid() {
		return tank.variant;
	}

	/** The tank (mB). */
	public long level() {
		return Drums.mb(tank.amount);
	}

	public Direction facing() {
		BlockState s = getBlockState();
		return s.getBlock() instanceof PieceBlock ? s.getValue(PieceBlock.FACING) : Direction.NORTH;
	}

	/** Its platform is free for the next pallet. */
	public boolean free() {
		return state == IDLE;
	}

	/** It could pump a liquid now: free, its tank empty or holding that liquid with room. */
	public boolean takes(FluidVariant liquid) {
		return free() && room(liquid) > 0;
	}

	/** Room in the tank for a liquid (mB): none for another liquid than the one in it. */
	public long room(FluidVariant liquid) {
		if (!tank.isResourceBlank() && !tank.variant.equals(liquid)) {
			return 0;
		}
		return TANK - level();
	}

	public boolean waiting() {
		return state == BED && pallet != null;
	}

	/** Whether its pallet's drums are all empty. */
	public boolean emptied() {
		if (pallet == null) {
			return false;
		}
		for (CaseLoad c : pallet.cases()) {
			if (c.count() > 0) {
				return false;
			}
		}
		return true;
	}

	void claim(long until) {
		claimed = until;
	}

	/** How much is in the tank for an order (mB). */
	public long shareOf(int orderId) {
		for (Share s : shares) {
			if (s.order == orderId) {
				return Drums.mb(s.amount);
			}
		}
		return 0;
	}

	public boolean hasOrder(int orderId) {
		for (Share s : shares) {
			if (s.order == orderId && s.amount > 0) {
				return true;
			}
		}
		return false;
	}

	/** Sends an order's share to pipes instead of the fill point. */
	void toPipes(int orderId) {
		for (Share s : shares) {
			if (s.order == orderId) {
				s.dest = Order.PIPES;
			}
		}
		sync();
	}

	// ------------------------------------------------------------------ the server

	/**
	 * A pallet drone set an order's pallet on the platform (Planner PUT_EMPTIER): works out what the pump takes from
	 * each drum (the order's liquid, up to what it still wants and what the tank has room for, the least full drums
	 * first) and starts the run.
	 */
	void begin(PalletLoad load, Order o, @Nullable BlockPos place, long now) {
		pallet = load.copy();
		FluidVariant liquid = Drums.liquid(o.item);
		long need = liquid == null ? 0 : Math.min(o.remaining(), room(liquid));
		List<Integer> order = new ArrayList<>();
		for (int i = 0; i < pallet.size(); i++) {
			order.add(i);
		}
		order.sort((a, b) -> Long.compare(pallet.cases().get(a).count(), pallet.cases().get(b).count()));
		List<EmptierMotion.Step> steps = new ArrayList<>();
		for (int i : order) {
			CaseLoad c = pallet.cases().get(i);
			if (need <= 0 || c.count() <= 0 || !c.sameItem(o.item)) {
				continue;
			}
			long n = Math.min(c.count(), need);
			steps.add(new EmptierMotion.Step(i, n));
			need -= n;
		}
		Drums.Kind kind = Drums.kindOf(o.item);
		plan = new EmptierMotion.Plan(now, kind == null ? 0 : kind.ordinal(), steps);
		this.order = o.id;
		from = place;
		state = WORKING;
		start = now;
		if (level != null) {
			level.playSound(null, worldPosition, SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 0.5F, 0.7F);
		}
		sync();
	}

	/** Part W5: a comparator by it: 15 pumping, 8 a pallet on its platform, 0 idle. */
	@Override
	public int analog() {
		return state == WORKING ? 15 : state == BED ? 8 : 0;
	}

	private int lastAnalog = -1;

	public void serverTick(ServerLevel level) {
		long now = level.getGameTime();
		if (now % 10 == 0 && analog() != lastAnalog) {
			lastAnalog = analog();
			PieceBlock.analogChanged(level, worldPosition, getBlockState());
		}
		if (state == WORKING && plan != null && now >= plan.end()) {
			finish(level, now);
		} else if (state == WORKING && plan == null) {
			state = pallet == null ? IDLE : BED;
			start = now;
			sync();
		} else if (state == BED && emptied() && now - start >= EMPTY_WAIT && now >= claimed) {
			// No filling station took the emptied drums back: they go into Ready Storage, with the pallet (or, with no
			// room there for a minute more, off the platform as items, so the station isn't held up for ever).
			TerminalBlockEntity t = Warehouses.terminalOf(level, worldPosition);
			if (now - start >= EMPTY_WAIT * 5 && (t == null || t.pool().room(new ItemStack(JugcraftWarehouse.PALLET), 0) <= 0)) {
				double x = worldPosition.getX() + 0.5, y = worldPosition.getY() + 1.0, z = worldPosition.getZ() + 0.5;
				for (CaseLoad c : pallet.cases()) {
					Containers.dropItemStack(level, x, y, z, Drums.dropped(c));
				}
				Containers.dropItemStack(level, x, y, z, new ItemStack(JugcraftWarehouse.PALLET));
				if (t != null) {
					t.unreserve(from);
					t.setChanged();
				}
				pallet = null;
				from = null;
				state = IDLE;
				sync();
			} else if (t != null && t.pool().room(new ItemStack(JugcraftWarehouse.PALLET), 0) > 0) {
				for (CaseLoad c : pallet.cases()) {
					ItemStack drum = Drums.dropped(c);
					if (t.pool().add(drum, 1, null, 0) < 1) {
						Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5, drum);
					}
				}
				t.pool().add(new ItemStack(JugcraftWarehouse.PALLET), 1, null, 0);
				t.unreserve(from);
				pallet = null;
				from = null;
				state = IDLE;
				t.setChanged();
				sync();
			}
		}
	}

	/** The run's end: the drums are lighter, the tank fuller, the order has what was pumped for it. */
	private void finish(ServerLevel level, long now) {
		TerminalBlockEntity t = Warehouses.terminalOf(level, worldPosition);
		Order o = t == null ? null : t.order(order);
		long total = 0;
		FluidVariant liquid = pallet == null || pallet.isEmpty() ? null : Drums.liquid(pallet.cases().get(0).item());
		if (pallet != null && plan != null) {
			for (EmptierMotion.Step s : plan.steps()) {
				CaseLoad c = pallet.cases().get(s.drum());
				long n = Math.min(c.count(), s.amount());
				pallet.cases().set(s.drum(), new CaseLoad(c.item(), (int) (c.count() - n)));
				total += n;
			}
		}
		if (total > 0 && liquid != null) {
			if (tank.isResourceBlank()) {
				tank.variant = liquid;
			}
			tank.amount += Drums.droplets(total);
			// (An order cancelled meanwhile: what was pumped for it is anyone's at the fill point.)
			int id = o == null ? 0 : o.id;
			Share share = null;
			for (Share s : shares) {
				if (s.order == id) {
					share = s;
				}
			}
			if (share == null) {
				share = o == null ? new Share(0, null, Order.WINDOW, 0, 0) : new Share(o.id, o.owner, o.dest, 0, o.stream);
				shares.add(share);
			}
			share.amount += Drums.droplets(total);
		}
		if (t != null) {
			t.pumped(this, order, total, now);
		}
		plan = null;
		state = BED;
		start = now;
		level.playSound(null, worldPosition, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 0.5F, 0.8F);
		sync();
	}

	/** A pallet drone takes the pallet off the platform (Planner TAKE_EMPTIER). */
	@Nullable PalletLoad takeBed() {
		if (state != BED || pallet == null) {
			return null;
		}
		PalletLoad out = pallet;
		pallet = null;
		state = IDLE;
		from = null;
		sync();
		return out;
	}

	/**
	 * Takes liquid out of the tank for orders (the fill point: WINDOW; the OUT flange: PIPES, or a stream's), up to
	 * max droplets: the shares are let go of as it goes (an owner's own first when given). Returns what may go and
	 * who gets it; the caller moves it and then calls taken().
	 */
	List<Share> sharesFor(int dest, @Nullable UUID owner, int stream) {
		List<Share> out = new ArrayList<>();
		for (Share s : shares) {
			if (s.amount > 0 && s.dest == dest && (stream < 0 || s.stream == stream) && owner != null && owner.equals(s.owner)) {
				out.add(s);
			}
		}
		for (Share s : shares) {
			if (s.amount > 0 && s.dest == dest && (stream < 0 || s.stream == stream) && !out.contains(s)) {
				out.add(s);
			}
		}
		return out;
	}

	/**
	 * A liquid stream was removed (its tag): what was pumped for it goes out of the OUT flange as a plain pipe order's;
	 * the streams after it moved up a place, their tags with them.
	 */
	void streamRemoved(int tag) {
		boolean changed = false;
		for (Share s : shares) {
			if (s.stream == tag) {
				s.stream = 0;
				changed = true;
			} else if (s.stream > tag) {
				s.stream--;
				changed = true;
			}
		}
		if (changed) {
			sync();
		}
	}

	/** droplets came out of the tank for these shares (in their order). */
	void taken(List<Share> from, long droplets) {
		long left = droplets;
		for (Share s : from) {
			long n = Math.min(left, s.amount);
			s.amount -= n;
			left -= n;
		}
		shares.removeIf(s -> s.amount <= 0);
		if (tank.amount <= 0) {
			tank.amount = 0;
			tank.variant = FluidVariant.blank();
			shares.clear();
		}
		sync();
	}

	void sync() {
		setChanged();
		if (level != null) {
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
		}
	}

	@Override
	public InteractionResult use(Level level, BlockPos clicked, Player player, ItemStack stack, InteractionHand hand) {
		if (!level.isClientSide()) {
			player.sendOverlayMessage(Component.literal(tank.isResourceBlank() ? "Holding tank: empty"
					: "Holding tank: " + Drums.buckets(level()) + " " + Drums.name(tank.variant).getString() + " of " + Drums.buckets(TANK)));
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void dropContents() {
		if (level == null || pallet == null) {
			return;
		}
		TerminalBlockEntity t = Warehouses.terminalOf(level, worldPosition);
		if (t != null) {
			t.unreserve(from);
		}
		double x = worldPosition.getX() + 0.5, y = worldPosition.getY() + 0.5, z = worldPosition.getZ() + 0.5;
		for (ItemStack s : pallet.stacks()) {
			Containers.dropItemStack(level, x, y, z, s);
		}
		Containers.dropItemStack(level, x, y, z, new ItemStack(JugcraftWarehouse.PALLET));
		pallet = null;
		plan = null;
		state = IDLE;
	}

	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		dropContents();
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return saveWithoutMetadata(registries);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		state = input.getIntOr("state", IDLE);
		start = input.getLongOr("start", 0);
		pallet = input.read("pallet", PalletLoad.CODEC).orElse(null);
		plan = input.read("plan", EmptierMotion.Plan.CODEC).orElse(null);
		order = input.getIntOr("order", 0);
		from = input.read("from", BlockPos.CODEC).orElse(null);
		tank.variant = input.read("liquid", FluidVariant.CODEC).orElseGet(FluidVariant::blank);
		tank.amount = Math.max(0, input.getLongOr("amount", 0));
		shares.clear();
		shares.addAll(input.read("shares", Share.CODEC.listOf()).orElse(List.of()));
		if (pallet == null && state != IDLE) {
			state = IDLE;
		}
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putInt("state", state);
		output.putLong("start", start);
		if (pallet != null) {
			output.store("pallet", PalletLoad.CODEC, pallet);
		}
		if (plan != null) {
			output.store("plan", EmptierMotion.Plan.CODEC, plan);
		}
		output.putInt("order", order);
		if (from != null) {
			output.store("from", BlockPos.CODEC, from);
		}
		if (!tank.variant.isBlank()) {
			output.store("liquid", FluidVariant.CODEC, tank.variant);
		}
		output.putLong("amount", tank.amount);
		output.store("shares", Share.CODEC.listOf(), shares);
	}
}
