package io.github.jimbozoomer.jugcraft.nuclear;

import io.github.jimbozoomer.jugcraft.fluid.FluidNetworks;
import io.github.jimbozoomer.jugcraft.fluid.JugcraftFluids;
import io.github.jimbozoomer.jugcraft.fluid.StoredFluid;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.storage.base.FilteringStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A storage tank nozzle's block entity. The outlet's is the tank's controller: every two seconds it looks for the
 * tank its nozzle is on. Going in through the shell behind it, it fills the space it finds; the tank is whole when
 * that space is closed in by tank plates (walls and roof) and a solid floor, nothing else. A whole tank forms: the
 * space fills with {@link StorageTankInteriorBlock}s, every plate is painted by where it is, the inlets on its shell
 * are linked, and it holds one fluid, four buckets for every block of space inside ({@link #BUCKETS_PER_BLOCK}). A tank that comes apart unforms: the
 * interior is cleared and the plates go back to primer, and the outlet keeps the fluid until the tank is whole again.
 * The outlet also works out where its float gauge runs (up the shell above it) and the widest clear stretch of shell
 * to stencil the tank's name on, for client/StorageTankRenderer.
 */
public class StorageTankPortBlockEntity extends BlockEntity {
	private static final int CHECK_TICKS = 40, MAX_CELLS = 16000, MAX_SPAN = 48;
	/** An open outlet feeds its line: every few ticks it sends this much (a bucket a second) into whatever its pipe reaches. */
	/** What a block of space inside holds (Justin, round 14: four times a bucket, so a tank holds a useful amount). */
	public static final int BUCKETS_PER_BLOCK = 4;
	private static final int FEED_TICKS = 5;
	private static final long FEED = 250 * FluidNetworks.DROPLETS_PER_MB;
	/** Fill changes reach clients at most this often (ticks). */
	private static final int SYNC_TICKS = 10;

	/** The tank's fluid (outlets only), a bucket for each block inside. */
	public final TankFluid fluid = new TankFluid();
	/** Outlet: whether the tank is formed. Inlet: whether it is linked to a formed tank. */
	private boolean formed;
	/** An inlet's outlet (null if none). */
	private @Nullable BlockPos controller;
	/** Positions relative to the outlet, packed ({@link #pack}): the interior blocks, the plates and the linked inlets. */
	private int[] interior = new int[0], plates = new int[0], inlets = new int[0];
	/** The space inside as columns, for the client to draw the inside of the tank and the liquid in it ({@link #columns}). */
	private int[] columns = new int[0];
	/** The float gauge: how many blocks of shell above the outlet it runs up. */
	private int gaugeHeight;
	/** The stencil: the face it is painted on, its middle (relative to the outlet's corner) and its width (blocks). */
	private @Nullable Direction stencilFace;
	private Vec3 stencilCenter = Vec3.ZERO;
	private double stencilWidth, stencilHeight;
	/** The paint the tank is finished in (white for now; ready for a spray-paint system). */
	private int paint = 0xFFF0F0EC;
	/** Fill the tank (most of the way, with what its service says) when it first forms: a fuelled plant build. */
	private boolean prefill;
	private int timer;
	private boolean syncPending;
	private long lastSync;
	/** What has come in and gone out (droplets) this second, the amount at the last count, and the last second's
	 * rates (mB a second), for the tank's screen. */
	private long inCount, outCount, counted;
	private int inRate, outRate;

	/** The last liquid the tank held (its label while empty) and the title a name tag gave it. */
	private FluidVariant label = FluidVariant.blank();
	private String title = "";

	/** Client: the handwheel's turn, and the liquid's level as drawn (renderer bookkeeping). */
	public boolean animOpen;
	public long animStart = Long.MIN_VALUE;
	public float shownFill = -1;
	public long shownAt;
	public @Nullable Object lining;
	public int liningKey;

	public StorageTankPortBlockEntity(BlockPos pos, BlockState state) {
		super(StorageTanks.PORT_ENTITY, pos, state);
		animOpen = state.getValue(StorageTankPortBlock.OPEN);
	}

	public final class TankFluid extends SingleVariantStorage<FluidVariant> {
		long capacity;

		@Override
		protected FluidVariant getBlankVariant() {
			return FluidVariant.blank();
		}

		@Override
		protected long getCapacity(FluidVariant variant) {
			return capacity;
		}

		/** A tank takes only what it is rated for (TankRating): a general tank no fuel or acid, no tank a gas or lava. */
		@Override
		protected boolean canInsert(FluidVariant variant) {
			// A tank built before the ratings keeps taking what it already holds.
			return rating().takes(TankProducts.kind(variant)) || !isResourceBlank() && amount > 0 && variant.equals(this.variant);
		}

		public long capacity() {
			return capacity;
		}

		@Override
		protected void onFinalCommit() {
			// Pipes and buckets move fluid one way a transaction: the change is what came in or went out.
			long change = amount - counted;
			if (change > 0) {
				inCount += change;
			} else {
				outCount -= change;
			}
			counted = amount;
			if (!variant.isBlank() && !variant.equals(label)) {
				label = variant;
			}
			setChanged();
			syncPending = true;
		}
	}

	/** The tank's rating (its outlet's; an inlet's is its tank's). */
	public TankRating rating() {
		StorageTankPortBlockEntity tank = isOutlet() ? this : tank();
		BlockState state = tank == null ? null : tank.getBlockState();
		return state != null && state.hasProperty(StorageTankOutletBlock.RATING) ? state.getValue(StorageTankOutletBlock.RATING) : TankRating.GENERAL;
	}

	/** What the label shows: the liquid in the tank, or the last one it held (a tank keeps its label while empty). */
	public FluidVariant label() {
		return !fluid.isResourceBlank() && fluid.amount > 0 ? fluid.variant : label;
	}

	/** Where the formed tank's plates are (an outlet's; empty for an inlet or an unformed tank). */
	public List<BlockPos> platePositions() {
		List<BlockPos> out = new ArrayList<>(plates.length);
		for (int packed : plates) {
			out.add(unpack(packed));
		}
		return out;
	}

	/** The title a name tag gave the tank, or "". */
	public String title() {
		return title;
	}

	/** A name tag used on the tank: its name becomes the label's title (an empty name clears it). */
	public void setTitle(String title) {
		this.title = title == null ? "" : title.strip();
		changed();
	}

	private boolean isOutlet() {
		return getBlockState().getBlock() instanceof StorageTankOutletBlock;
	}

	public boolean formed() {
		return formed;
	}

	public int gaugeHeight() {
		return gaugeHeight;
	}

	public @Nullable Direction stencilFace() {
		return stencilFace;
	}

	public Vec3 stencilCenter() {
		return stencilCenter;
	}

	public double stencilWidth() {
		return stencilWidth;
	}

	/** The height of clear shell the stencil may take (between the foot and the band round the top). */
	public double stencilHeight() {
		return stencilHeight;
	}

	public int paint() {
		return paint;
	}

	/** What came in over the last second (mB). */
	public int inRate() {
		return inRate;
	}

	/** What went out over the last second (mB). */
	public int outRate() {
		return outRate;
	}

	/**
	 * The space inside the tank, a vertical run of it at a time: each entry packs the run's x and z and its lowest and
	 * highest y, relative to the outlet, 7 bits each offset by 64 ({@link #columnX} and the rest unpack them).
	 */
	public int[] columns() {
		return columns;
	}

	public static int columnX(int packed) {
		return (packed >> 21 & 127) - 64;
	}

	public static int columnZ(int packed) {
		return (packed >> 14 & 127) - 64;
	}

	public static int columnBottom(int packed) {
		return (packed >> 7 & 127) - 64;
	}

	public static int columnTop(int packed) {
		return (packed & 127) - 64;
	}

	/** A fuelled plant build: fill the tank when it forms. */
	public void prefill() {
		prefill = true;
		setChanged();
	}

	// ------------------------------------------------------------------ the fluid, through the nozzles

	/** The tank's outlet entity (this one, or an inlet's linked outlet), or null. */
	public @Nullable StorageTankPortBlockEntity tank() {
		if (isOutlet()) {
			return this;
		}
		if (controller == null || level == null || !level.isLoaded(controller)) {
			return null;
		}
		return level.getBlockEntity(controller) instanceof StorageTankPortBlockEntity outlet && outlet.isOutlet() && outlet.formed ? outlet : null;
	}

	private Storage<FluidVariant> backing() {
		StorageTankPortBlockEntity tank = tank();
		return tank == null ? Storage.empty() : tank.fluid;
	}

	/** What pipes and buckets see at this nozzle: the inlet only takes in, the outlet only gives out, both only while
	 * the tank is formed and the valve open. */
	public Storage<FluidVariant> portStorage() {
		return new FilteringStorage<>(this::backing) {
			@Override
			protected boolean canInsert(FluidVariant resource) {
				return !isOutlet() && usable();
			}

			@Override
			protected boolean canExtract(FluidVariant resource) {
				return isOutlet() && usable();
			}
		};
	}

	private boolean usable() {
		StorageTankPortBlockEntity tank = tank();
		return tank != null && tank.formed && StorageTankPortBlock.valveOpen(getBlockState());
	}

	public int redstone() {
		StorageTankPortBlockEntity tank = tank();
		return tank == null || !tank.formed ? 0 : StorageUtil.getRedstoneSignal(tank.fluid);
	}

	/** "Outlet valve opened. Water: 312 / 624 buckets", or why nothing moved. */
	public Component status(BlockState state, @Nullable Boolean opened) {
		StorageTankPortBlockEntity tank = tank();
		Component contents;
		if (tank == null || !tank.formed) {
			contents = Component.translatable(isOutlet() ? "message.jugcraft.storage_tank.not_formed" : "message.jugcraft.storage_tank.no_tank");
		} else {
			contents = tank.contents();
		}
		Component which = Component.translatable(isOutlet() ? "message.jugcraft.storage_tank.outlet" : "message.jugcraft.storage_tank.inlet");
		if (state.getValue(StorageTankPortBlock.POWERED)) {
			return Component.translatable("message.jugcraft.storage_tank.held_shut", which, contents);
		}
		boolean open = opened != null ? opened : state.getValue(StorageTankPortBlock.OPEN);
		return Component.translatable(open ? "message.jugcraft.storage_tank.opened" : "message.jugcraft.storage_tank.shut", which, contents);
	}

	/** A formed tank's contents: "Water: 312 / 624 buckets", or "Empty (624 buckets)". */
	public Component contents() {
		if (fluid.isResourceBlank() || fluid.amount <= 0) {
			return Component.translatable("message.jugcraft.storage_tank.empty", String.format("%,d", fluid.capacity / FluidConstants.BUCKET));
		}
		return Component.translatable("message.jugcraft.storage_tank.contents", FluidVariantAttributes.getName(fluid.variant),
				String.format("%,d", fluid.amount / FluidConstants.BUCKET), String.format("%,d", fluid.capacity / FluidConstants.BUCKET));
	}

	// ------------------------------------------------------------------ forming

	public void serverTick(ServerLevel level) {
		long now = level.getGameTime();
		if (formed && Math.floorMod(now + worldPosition.asLong(), FEED_TICKS) == 0) {
			feed(level);
		}
		if (now % 20 == 0) {
			int in = (int) (inCount / FluidNetworks.DROPLETS_PER_MB), out = (int) (outCount / FluidNetworks.DROPLETS_PER_MB);
			inCount = 0;
			outCount = 0;
			if (in != inRate || out != outRate) {
				inRate = in;
				outRate = out;
				syncPending = true;
			}
		}
		if (syncPending && now - lastSync >= SYNC_TICKS) {
			syncPending = false;
			lastSync = now;
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
		}
		if (--timer > 0) {
			return;
		}
		timer = CHECK_TICKS;
		check(level);
	}

	/**
	 * An open outlet feeds its line: its fluid runs out through the pipe on its flange into whatever takes it at the
	 * other end (another tank's inlet, a machine), a bucket a second, until that is full or this tank is empty. Shut,
	 * or held shut by redstone, nothing goes.
	 */
	private void feed(ServerLevel level) {
		if (!isOutlet() || fluid.isResourceBlank() || fluid.amount <= 0 || !StorageTankPortBlock.valveOpen(getBlockState())) {
			return;
		}
		Direction out = getBlockState().getValue(StorageTankPortBlock.FACING);
		FluidNetworks.pushToNeighbors(level, worldPosition, fluid, Math.min(FEED, fluid.amount), List.of(out));
	}

	/** Looks the tank over now (the plant builder, tests). */
	public void check(ServerLevel level) {
		if (!isOutlet()) {
			return;
		}
		Direction out = getBlockState().getValue(StorageTankPortBlock.FACING);
		Direction in = out.getOpposite();
		BlockPos seed = null;
		if (isPlate(level.getBlockState(worldPosition.relative(in)))) {
			for (int k = 2; k <= 4; k++) {
				BlockPos p = worldPosition.relative(in, k);
				if (!level.isLoaded(p)) {
					return;
				}
				BlockState state = level.getBlockState(p);
				if (isPlate(state)) {
					continue;
				}
				if (space(state)) {
					seed = p;
				}
				break;
			}
		}
		Scan scan = seed == null ? null : scan(level, seed);
		if (scan == LOADING) {
			return;
		}
		if (scan == null) {
			if (formed) {
				unform(level);
			}
			return;
		}
		form(level, scan);
	}

	private static boolean isPlate(BlockState state) {
		return state.getBlock() instanceof StorageTankPlateBlock;
	}

	/** A cell the tank can hold: empty, or soft (water, grass) with nothing in it, or the tank's own interior. */
	private static boolean space(BlockState state) {
		return state.isAir() || state.getBlock() instanceof StorageTankInteriorBlock || state.canBeReplaced() && !state.hasBlockEntity();
	}

	private record Scan(Set<Long> inside, int minY, int maxY, int minX, int maxX, int minZ, int maxZ) {
	}

	private static final Scan LOADING = new Scan(Set.of(), 0, 0, 0, 0, 0, 0);

	private @Nullable Scan scan(ServerLevel level, BlockPos seed) {
		Set<Long> inside = new HashSet<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		inside.add(seed.asLong());
		queue.add(seed);
		int minX = seed.getX(), maxX = minX, minY = seed.getY(), maxY = minY, minZ = seed.getZ(), maxZ = minZ;
		while (!queue.isEmpty()) {
			BlockPos p = queue.poll();
			for (Direction d : Direction.values()) {
				BlockPos q = p.relative(d);
				long key = q.asLong();
				if (inside.contains(key)) {
					continue;
				}
				if (!level.isLoaded(q)) {
					return LOADING;
				}
				BlockState state = level.getBlockState(q);
				if (isPlate(state)) {
					continue;
				}
				if (d == Direction.DOWN && state.isFaceSturdy(level, q, Direction.UP)) {
					continue;
				}
				if (!space(state) || level.isOutsideBuildHeight(q)) {
					return null;
				}
				inside.add(key);
				if (inside.size() > MAX_CELLS) {
					return null;
				}
				minX = Math.min(minX, q.getX());
				maxX = Math.max(maxX, q.getX());
				minY = Math.min(minY, q.getY());
				maxY = Math.max(maxY, q.getY());
				minZ = Math.min(minZ, q.getZ());
				maxZ = Math.max(maxZ, q.getZ());
				if (maxX - minX > MAX_SPAN || maxY - minY > MAX_SPAN || maxZ - minZ > MAX_SPAN) {
					return null;
				}
				queue.add(q);
			}
		}
		return new Scan(inside, minY, maxY, minX, maxX, minZ, maxZ);
	}

	private void form(ServerLevel level, Scan scan) {
		int flags = Block.UPDATE_CLIENTS;
		BlockState interiorState = StorageTanks.INTERIOR.defaultBlockState();
		List<Integer> interiorList = new ArrayList<>(scan.inside.size());
		for (long key : scan.inside) {
			BlockPos p = BlockPos.of(key);
			if (!level.getBlockState(p).is(StorageTanks.INTERIOR)) {
				level.setBlock(p, interiorState, flags);
			}
			interiorList.add(pack(p));
		}
		// The plates: those round the space, and the plates joined to them (the shell's outer layer, the roof's edge).
		Set<Long> plateSet = new HashSet<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		for (long key : scan.inside) {
			BlockPos p = BlockPos.of(key);
			for (Direction d : Direction.values()) {
				BlockPos q = p.relative(d);
				if (!scan.inside.contains(q.asLong()) && isPlate(level.getBlockState(q)) && plateSet.add(q.asLong())) {
					queue.add(q);
				}
			}
		}
		while (!queue.isEmpty()) {
			BlockPos p = queue.poll();
			for (Direction d : Direction.values()) {
				BlockPos q = p.relative(d);
				if (q.getX() < scan.minX - 3 || q.getX() > scan.maxX + 3 || q.getZ() < scan.minZ - 3 || q.getZ() > scan.maxZ + 3
						|| q.getY() < scan.minY - 1 || q.getY() > scan.maxY + 3 || plateSet.contains(q.asLong())) {
					continue;
				}
				if (level.isLoaded(q) && isPlate(level.getBlockState(q))) {
					plateSet.add(q.asLong());
					queue.add(q);
				}
			}
		}
		int roofY = scan.maxY + 1;
		TankRating rating = rating();
		// The course under the band takes the product band when there is a wall course left below it.
		boolean stripe = roofY - 2 > scan.minY;
		List<Integer> plateList = new ArrayList<>(plateSet.size());
		List<Integer> inletList = new ArrayList<>();
		for (long key : plateSet) {
			BlockPos p = BlockPos.of(key);
			StorageTankPlateBlock.Band band;
			if (p.getY() > roofY) {
				band = StorageTankPlateBlock.Band.RIM;
			} else if (p.getY() == roofY) {
				band = scan.inside.contains(p.below().asLong()) ? StorageTankPlateBlock.Band.ROOF : StorageTankPlateBlock.Band.RIM;
			} else if (p.getY() == roofY - 1) {
				band = StorageTankPlateBlock.Band.BAND;
			} else if (stripe && p.getY() == roofY - 2) {
				band = StorageTankPlateBlock.Band.STRIPE;
			} else {
				band = StorageTankPlateBlock.Band.WALL;
			}
			BlockState state = level.getBlockState(p);
			BlockState painted = state.setValue(StorageTankPlateBlock.FORMED, true).setValue(StorageTankPlateBlock.BAND, band)
					.setValue(StorageTankPlateBlock.RATING, rating);
			if (painted != state) {
				level.setBlock(p, painted, flags);
			}
			plateList.add(pack(p));
			// Inlets standing against this plate, facing out from it.
			for (Direction d : Direction.Plane.HORIZONTAL) {
				BlockPos q = p.relative(d);
				BlockState there = level.getBlockState(q);
				if (there.getBlock() instanceof StorageTankPortBlock port && !port.outlet() && there.getValue(StorageTankPortBlock.FACING) == d
						&& level.getBlockEntity(q) instanceof StorageTankPortBlockEntity inlet) {
					inlet.link(level, worldPosition);
					inletList.add(pack(q));
				}
			}
		}
		// Inlets that were linked before but are not on the tank now.
		for (int packed : inlets) {
			if (!inletList.contains(packed) && level.getBlockEntity(unpack(packed)) instanceof StorageTankPortBlockEntity old) {
				old.link(level, null);
			}
		}
		boolean was = formed;
		String before = signature();
		formed = true;
		interior = interiorList.stream().mapToInt(Integer::intValue).toArray();
		plates = plateList.stream().mapToInt(Integer::intValue).toArray();
		inlets = inletList.stream().mapToInt(Integer::intValue).toArray();
		fluid.capacity = (long) scan.inside.size() * BUCKETS_PER_BLOCK * FluidConstants.BUCKET;
		if (fluid.amount > fluid.capacity) {
			fluid.amount = fluid.capacity;
		}
		if (prefill && (fluid.isResourceBlank() || fluid.amount == 0) && getBlockState().getBlock() instanceof StorageTankOutletBlock) {
			String fill = getBlockState().getValue(StorageTankOutletBlock.SERVICE).fill;
			if (fill != null) {
				fluid.variant = FluidVariant.of(BuiltInRegistries.FLUID.getValue(Identifier.parse(fill)));
				fluid.amount = fluid.capacity * 4 / 5 / FluidConstants.BUCKET * FluidConstants.BUCKET;
				label = fluid.variant;
			}
		}
		prefill = false;
		counted = fluid.amount;
		columns = columnsOf(scan.inside);
		layout(level, scan, roofY);
		BlockState state = getBlockState();
		if (!state.getValue(StorageTankPortBlock.FORMED)) {
			level.setBlock(worldPosition, state.setValue(StorageTankPortBlock.FORMED, true), Block.UPDATE_ALL);
		}
		if (!was) {
			FluidNetworks.invalidate(level);
		}
		if (!was || !before.equals(signature())) {
			changed();
		}
	}

	/** What the clients draw from (to send it only when it changes). */
	private String signature() {
		return formed + ":" + fluid.capacity + ":" + fluid.amount + ":" + gaugeHeight + ":" + stencilFace + ":" + stencilCenter + ":" + stencilWidth
				+ ":" + stencilHeight
				+ ":" + inlets.length + ":" + java.util.Arrays.hashCode(columns) + ":" + label + ":" + title;
	}

	/** The space inside as vertical runs (see {@link #columns()}). */
	private int[] columnsOf(Set<Long> inside) {
		java.util.Map<Long, List<Integer>> byColumn = new java.util.TreeMap<>();
		for (long key : inside) {
			BlockPos p = BlockPos.of(key);
			byColumn.computeIfAbsent((long) p.getX() << 32 | (p.getZ() & 0xFFFFFFFFL), k -> new ArrayList<>()).add(p.getY());
		}
		List<Integer> out = new ArrayList<>();
		for (java.util.Map.Entry<Long, List<Integer>> e : byColumn.entrySet()) {
			int dx = (int) (e.getKey() >> 32) - worldPosition.getX(), dz = (int) (long) e.getKey() - worldPosition.getZ();
			List<Integer> ys = e.getValue();
			ys.sort(null);
			int start = ys.get(0), last = start;
			for (int i = 1; i <= ys.size(); i++) {
				if (i < ys.size() && ys.get(i) == last + 1) {
					last = ys.get(i);
					continue;
				}
				int y0 = start - worldPosition.getY(), y1 = last - worldPosition.getY();
				if (Math.abs(dx) < 64 && Math.abs(dz) < 64 && Math.abs(y0) < 64 && Math.abs(y1) < 64) {
					out.add((dx + 64) << 21 | (dz + 64) << 14 | (y0 + 64) << 7 | (y1 + 64));
				}
				if (i < ys.size()) {
					start = last = ys.get(i);
				}
			}
		}
		return out.stream().mapToInt(Integer::intValue).toArray();
	}

	/** Where the float gauge and the stencil go. */
	private void layout(ServerLevel level, Scan scan, int roofY) {
		Direction out = getBlockState().getValue(StorageTankPortBlock.FACING);
		BlockPos shell = worldPosition.relative(out.getOpposite());
		int h = 0;
		while (h < MAX_SPAN && isPlate(level.getBlockState(shell.above(h + 1))) && level.getBlockState(worldPosition.above(h + 1)).isAir()) {
			h++;
		}
		gaugeHeight = h;
		// The stencil: on the side (not the outlet's) with the widest clear stretch of flat shell, centred between the
		// foot (a manway may stand there) and the band round the top (and under a flammable or corrosive tank's
		// product stripe).
		TankRating rating = rating();
		boolean striped = roofY - 2 > scan.minY && (rating == TankRating.FLAMMABLE || rating == TankRating.CORROSIVE);
		double low = scan.minY + 1.3, high = (striped ? roofY - 2 : roofY - 1) - 0.15;
		int y = (int) Math.floor((low + high) / 2);
		stencilFace = null;
		stencilWidth = 0;
		// Sides tried from the one a quarter turn anticlockwise from the outlet (a kit's front, facing whoever placed
		// it) round, so that side wins a tie.
		Direction front = out.getCounterClockWise();
		for (Direction d : new Direction[] {front, front.getClockWise(), front.getOpposite(), front.getCounterClockWise()}) {
			if (d == out) {
				continue;
			}
			int best = Integer.MIN_VALUE;
			List<BlockPos> outer = new ArrayList<>();
			for (int packed : plates) {
				BlockPos p = unpack(packed);
				if (p.getY() != y) {
					continue;
				}
				int along = p.getX() * d.getStepX() + p.getZ() * d.getStepZ();
				if (along > best) {
					best = along;
					outer.clear();
				}
				if (along == best) {
					outer.add(p);
				}
			}
			Direction side = d.getClockWise();
			outer.sort((a, b) -> Integer.compare(a.getX() * side.getStepX() + a.getZ() * side.getStepZ(), b.getX() * side.getStepX() + b.getZ() * side.getStepZ()));
			// The longest run of neighbouring outer plates with clear air in front of them for three blocks up.
			int bestStart = 0, bestLength = 0;
			for (int i = 0; i < outer.size(); ) {
				if (!clearInFront(level, outer.get(i), d)) {
					i++;
					continue;
				}
				int j = i;
				while (j + 1 < outer.size() && clearInFront(level, outer.get(j + 1), d) && outer.get(j + 1).distManhattan(outer.get(j)) == 1) {
					j++;
				}
				if (j - i + 1 > bestLength) {
					bestLength = j - i + 1;
					bestStart = i;
				}
				i = j + 1;
			}
			if (bestLength >= 2 && bestLength > stencilWidth) {
				BlockPos first = outer.get(bestStart), last = outer.get(bestStart + bestLength - 1);
				Vec3 a = Vec3.atCenterOf(first).add(d.getStepX() * 0.5, 0, d.getStepZ() * 0.5);
				Vec3 b = Vec3.atCenterOf(last).add(d.getStepX() * 0.5, 0, d.getStepZ() * 0.5);
				stencilFace = d;
				stencilWidth = bestLength;
				Vec3 mid = a.add(b).scale(0.5);
				stencilCenter = new Vec3(mid.x, (low + high) / 2, mid.z).subtract(Vec3.atLowerCornerOf(worldPosition));
				stencilHeight = high - low;
			}
		}
	}

	private static boolean clearInFront(Level level, BlockPos plate, Direction d) {
		BlockPos front = plate.relative(d);
		for (int dy = -1; dy <= 1; dy++) {
			if (!level.getBlockState(front.above(dy)).isAir()) {
				return false;
			}
		}
		return true;
	}

	/** The tank came apart: the interior goes, the plates go back to primer, the inlets let go. */
	private void unform(ServerLevel level) {
		unform(level, false);
	}

	private void unform(ServerLevel level, boolean removing) {
		int flags = Block.UPDATE_CLIENTS;
		for (int packed : interior) {
			BlockPos p = unpack(packed);
			if (level.isLoaded(p) && level.getBlockState(p).is(StorageTanks.INTERIOR)) {
				level.setBlock(p, Blocks.AIR.defaultBlockState(), flags);
			}
		}
		for (int packed : plates) {
			BlockPos p = unpack(packed);
			BlockState state = level.isLoaded(p) ? level.getBlockState(p) : null;
			if (state != null && isPlate(state) && state.getValue(StorageTankPlateBlock.FORMED)) {
				level.setBlock(p, state.setValue(StorageTankPlateBlock.FORMED, false).setValue(StorageTankPlateBlock.BAND, StorageTankPlateBlock.Band.WALL), flags);
			}
		}
		for (int packed : inlets) {
			if (level.getBlockEntity(unpack(packed)) instanceof StorageTankPortBlockEntity inlet) {
				inlet.link(level, null);
			}
		}
		interior = new int[0];
		plates = new int[0];
		inlets = new int[0];
		columns = new int[0];
		formed = false;
		gaugeHeight = 0;
		stencilFace = null;
		FluidNetworks.invalidate(level);
		if (removing) {
			return;
		}
		BlockState state = getBlockState();
		if (state.getBlock() instanceof StorageTankPortBlock && state.getValue(StorageTankPortBlock.FORMED)) {
			level.setBlock(worldPosition, state.setValue(StorageTankPortBlock.FORMED, false), Block.UPDATE_ALL);
		}
		changed();
	}

	/** An inlet joined to (or let go by) an outlet. */
	void link(Level level, @Nullable BlockPos outlet) {
		boolean now = outlet != null;
		if (now == formed && java.util.Objects.equals(outlet, controller)) {
			return;
		}
		controller = outlet == null ? null : outlet.immutable();
		formed = now;
		BlockState state = getBlockState();
		if (state.getValue(StorageTankPortBlock.FORMED) != now) {
			level.setBlock(worldPosition, state.setValue(StorageTankPortBlock.FORMED, now), Block.UPDATE_ALL);
		}
		FluidNetworks.invalidate(level);
		changed();
	}

	/** The outlet broken: the tank comes apart (its fluid goes with the outlet's item). */
	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		super.preRemoveSideEffects(pos, state);
		if (level instanceof ServerLevel server) {
			if (isOutlet() && formed) {
				unform(server, true);
			} else if (!isOutlet() && controller != null && server.getBlockEntity(controller) instanceof StorageTankPortBlockEntity outlet) {
				outlet.timer = 1;
			}
		}
	}

	private int pack(BlockPos p) {
		int dx = p.getX() - worldPosition.getX() + 512, dy = p.getY() - worldPosition.getY() + 512, dz = p.getZ() - worldPosition.getZ() + 512;
		return dx << 20 | dy << 10 | dz;
	}

	private BlockPos unpack(int packed) {
		return worldPosition.offset((packed >> 20 & 1023) - 512, (packed >> 10 & 1023) - 512, (packed & 1023) - 512);
	}

	// ------------------------------------------------------------------ saving and syncing

	private void changed() {
		setChanged();
		if (level != null && !level.isClientSide()) {
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		fluid.variant = input.read("fluid", FluidVariant.CODEC).orElseGet(FluidVariant::blank);
		fluid.amount = Math.max(0, input.getLongOr("amount", 0L));
		fluid.capacity = Math.max(0, input.getLongOr("capacity", 0L));
		counted = fluid.amount;
		inRate = input.getIntOr("in_rate", 0);
		outRate = input.getIntOr("out_rate", 0);
		formed = input.getBooleanOr("formed", false);
		controller = input.read("controller", BlockPos.CODEC).orElse(null);
		interior = input.getIntArray("interior").orElse(interior);
		plates = input.getIntArray("plates").orElse(plates);
		inlets = input.getIntArray("inlets").orElse(inlets);
		gaugeHeight = input.getIntOr("gauge", 0);
		columns = input.getIntArray("columns").orElse(new int[0]);
		int face = input.getIntOr("stencil_face", -1);
		stencilFace = face < 0 ? null : Direction.from3DDataValue(face);
		stencilCenter = new Vec3(input.getDoubleOr("stencil_x", 0), input.getDoubleOr("stencil_y", 0), input.getDoubleOr("stencil_z", 0));
		stencilWidth = input.getDoubleOr("stencil_width", 0);
		stencilHeight = input.getDoubleOr("stencil_height", 0);
		paint = input.getIntOr("paint", paint);
		prefill = input.getBooleanOr("prefill", false);
		label = input.read("label", FluidVariant.CODEC).orElse(fluid.isResourceBlank() ? FluidVariant.blank() : fluid.variant);
		title = input.getStringOr("title", "");
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		writeSynced(output);
		output.putIntArray("interior", interior);
		output.putIntArray("plates", plates);
		output.putIntArray("inlets", inlets);
		output.putBoolean("prefill", prefill);
	}

	private void writeSynced(ValueOutput output) {
		if (!fluid.isResourceBlank()) {
			output.store("fluid", FluidVariant.CODEC, fluid.variant);
		}
		output.putLong("amount", fluid.amount);
		output.putLong("capacity", fluid.capacity);
		output.putBoolean("formed", formed);
		if (controller != null) {
			output.store("controller", BlockPos.CODEC, controller);
		}
		output.putInt("gauge", gaugeHeight);
		output.putIntArray("columns", columns);
		output.putInt("stencil_face", stencilFace == null ? -1 : stencilFace.get3DDataValue());
		output.putDouble("stencil_x", stencilCenter.x);
		output.putDouble("stencil_y", stencilCenter.y);
		output.putDouble("stencil_z", stencilCenter.z);
		output.putDouble("stencil_width", stencilWidth);
		output.putDouble("stencil_height", stencilHeight);
		output.putInt("paint", paint);
		if (!label.isBlank()) {
			output.store("label", FluidVariant.CODEC, label);
		}
		if (!title.isEmpty()) {
			output.putString("title", title);
		}
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	/** Clients get the fluid, the gauge, the stencil and the space inside (as columns), not the interior and plate lists. */
	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		CompoundTag tag = new CompoundTag();
		if (!fluid.isResourceBlank()) {
			tag.store("fluid", FluidVariant.CODEC, fluid.variant);
		}
		tag.putLong("amount", fluid.amount);
		tag.putLong("capacity", fluid.capacity);
		tag.putBoolean("formed", formed);
		tag.putInt("gauge", gaugeHeight);
		tag.putIntArray("columns", columns);
		tag.putInt("in_rate", inRate);
		tag.putInt("out_rate", outRate);
		tag.putInt("stencil_face", stencilFace == null ? -1 : stencilFace.get3DDataValue());
		tag.putDouble("stencil_x", stencilCenter.x);
		tag.putDouble("stencil_y", stencilCenter.y);
		tag.putDouble("stencil_z", stencilCenter.z);
		tag.putDouble("stencil_width", stencilWidth);
		tag.putDouble("stencil_height", stencilHeight);
		tag.putInt("paint", paint);
		if (!label.isBlank()) {
			tag.store("label", FluidVariant.CODEC, label);
		}
		tag.putString("title", title);
		return tag;
	}

	@Override
	protected void collectImplicitComponents(DataComponentMap.Builder components) {
		super.collectImplicitComponents(components);
		if (!fluid.isResourceBlank() && fluid.amount > 0) {
			components.set(JugcraftFluids.STORED_FLUID, new StoredFluid(fluid.variant, fluid.amount));
		}
	}

	@Override
	protected void applyImplicitComponents(DataComponentGetter components) {
		super.applyImplicitComponents(components);
		StoredFluid stored = components.get(JugcraftFluids.STORED_FLUID);
		if (stored != null) {
			fluid.variant = stored.variant();
			fluid.amount = stored.amount();
			counted = fluid.amount;
		}
	}

	/** What a plant rebuild keeps: the fluid. */
	public StoredFluid stored() {
		return fluid.isResourceBlank() || fluid.amount <= 0 ? null : new StoredFluid(fluid.variant, fluid.amount);
	}

	/** A rebuilt plant's tank gets its fluid back. */
	public void restore(@Nullable StoredFluid stored) {
		if (stored != null) {
			fluid.variant = stored.variant();
			fluid.amount = stored.amount();
			counted = fluid.amount;
			label = fluid.variant;
			prefill = false;
			changed();
		}
	}
}
