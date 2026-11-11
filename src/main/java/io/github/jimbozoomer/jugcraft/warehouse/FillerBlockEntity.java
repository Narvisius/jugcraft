package io.github.jimbozoomer.jugcraft.warehouse;

import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
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
 * The Drum Filling Station (part W4): liquids piped into the Liquids In panel in front of it (or tipped into its funnel)
 * gather in its buffer tank, one liquid at a time. When there is a drum's worth (or the inflow has stopped) and it has
 * an empty drum of the liquid's kind and an empty pallet, it pushes a pallet onto its fill bay's scale (DISPENSING),
 * and its gantry robot sets a drum on it and fills it, drum after drum (BUILDING; FillerMotion's trips), up to four;
 * then it straps them (STRAPPING) and the pallet is READY for a pallet drone to take to the racks. A pallet of emptied
 * drums brought back from a Drum Emptying Station is set on the bay: the robot puts the drums back in the magazine and
 * the pallet slides back into its magazine (RETURNING). Players load empty drums and pallets by using them on it.
 */
public class FillerBlockEntity extends BlockEntity implements PieceEntity, AnalogSource {
	public static final int EMPTY = 0, DISPENSING = 1, BUILDING = 2, STRAPPING = 3, READY = 4, RETURNING = 5, STOWING = 6;
	public static final int PALLETS = 8;
	/** The buffer tank (mB), a drum's worth, and the ticks: a pallet pushed out (or back), the straps, the wait for more. */
	public static final long TANK = 80_000;
	public static final int SLIDE = 30, STRAP = 40;
	/** No more liquid this long: what is in the tank goes in a part drum, a part pallet is strapped as it is. */
	public static final int IDLE = 100;
	private final int[] magazine = new int[8];
	private int pallets;
	private int state = EMPTY;
	private long since, until;
	private @Nullable PalletLoad build;
	private FluidVariant buildLiquid = FluidVariant.blank();
	private FillerMotion.@Nullable Plan plan;
	/** A pallet of emptied drums brought back (its drums' kinds, the ones still on it). */
	private final List<Integer> returned = new ArrayList<>();
	private long lastInflow, lastWork, incoming;
	/** The liquid that came in the last second (mB), for the screens. */
	private long inflow, inflowShown;
	/** The buffer tank as the clients last saw it (mB, liquid): sent again once a second when it changes. */
	private long shownTank = -1;
	private FluidVariant shownLiquid = FluidVariant.blank();
	public final Buffer buffer = new Buffer();

	/** The buffer tank: one liquid the warehouse keeps (not gases, not lava). */
	public final class Buffer extends SingleVariantStorage<FluidVariant> {
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
			return Drums.kindFor(variant) != null;
		}

		@Override
		protected boolean canExtract(FluidVariant variant) {
			return false;
		}

		@Override
		protected void onFinalCommit() {
			long now = level == null ? 0 : level.getGameTime();
			lastInflow = now;
			setChanged();
		}
	}

	public FillerBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftWarehouse.FILLER_ENTITY, pos, state);
		Arrays.fill(magazine, -1);
	}

	// ------------------------------------------------------------------ what the screens and the renderer read

	public int state() {
		return state;
	}

	public long since() {
		return since;
	}

	public long until() {
		return until;
	}

	public int[] magazine() {
		return magazine;
	}

	public int pallets() {
		return pallets;
	}

	public @Nullable PalletLoad build() {
		return build;
	}

	public FluidVariant buildLiquid() {
		return buildLiquid;
	}

	public FillerMotion.@Nullable Plan plan() {
		return plan;
	}

	public List<Integer> returned() {
		return returned;
	}

	/** The buffer tank's liquid and how much (mB). */
	public FluidVariant liquid() {
		return buffer.variant;
	}

	public long tank() {
		return Drums.mb(buffer.amount);
	}

	/** Part W5: a comparator by it reads its buffer tank (0 empty, 15 full). */
	@Override
	public int analog() {
		long mb = tank();
		return mb <= 0 ? 0 : (int) Math.min(15, 1 + 14 * mb / TANK);
	}

	private int lastAnalog = -1;

	public long inflow() {
		return inflowShown;
	}

	public int drums(Drums.Kind kind) {
		int n = 0;
		for (int k : magazine) {
			if (k == kind.ordinal()) {
				n++;
			}
		}
		return n;
	}

	public int freePlaces() {
		int n = 0;
		for (int i = 0; i < magazine.length; i++) {
			if (magazine[i] < 0 && !(plan != null && plan.place() == i)) {
				n++;
			}
		}
		return n;
	}

	public Direction facing() {
		BlockState s = getBlockState();
		return s.getBlock() instanceof PieceBlock ? s.getValue(PieceBlock.FACING) : Direction.NORTH;
	}

	/** Its pallet is strapped and waits for a pallet drone. */
	public boolean ready() {
		return state == READY && build != null;
	}

	/** It could take a pallet of n emptied drums back now (its bay clear, room in its magazines). */
	public boolean takesBack(int drums) {
		return state == EMPTY && pallets < PALLETS && freePlaces() >= drums;
	}

	/** Loads pallets (as using pallets on it does; the demo, the tests). */
	public void setPallets(int n) {
		pallets = Math.max(0, Math.min(PALLETS, n));
		sync();
	}

	void setIncoming(long until) {
		this.incoming = until;
	}

	// ------------------------------------------------------------------ the server

	private int placeOf(int kind) {
		for (int i = 0; i < magazine.length; i++) {
			if (magazine[i] == kind) {
				return i;
			}
		}
		return -1;
	}

	/** A free magazine place (not the one the robot is taking a drum from or bringing one back to). */
	private int freePlace() {
		for (int i = 0; i < magazine.length; i++) {
			if (magazine[i] < 0 && !(plan != null && plan.place() == i)) {
				return i;
			}
		}
		return -1;
	}

	/** Enough liquid for a drum: a drum's worth, or what there is once the inflow has stopped. */
	private boolean enough(long now) {
		long mb = tank();
		return mb >= Drums.CAPACITY || mb > 0 && now - lastInflow >= IDLE;
	}

	private Drums.@Nullable Kind kindNow() {
		return buffer.isResourceBlank() ? null : Drums.kindFor(buffer.variant);
	}

	public void serverTick(ServerLevel level) {
		long now = level.getGameTime();
		if (now % 20 == 0) {
			inflowShown = Drums.mb(inflow);
			inflow = 0;
			// Less than a millibucket left over (pipes split their flow in droplets) would hold the tank to its liquid
			// for ever: let it go once nothing more comes.
			if (buffer.amount > 0 && buffer.amount < Drums.droplets(1) && now - lastInflow >= IDLE) {
				buffer.amount = 0;
				buffer.variant = FluidVariant.blank();
				setChanged();
			}
			int a = analog();
			if (a != lastAnalog) {
				lastAnalog = a;
				PieceBlock.analogChanged(level, worldPosition, getBlockState());
			}
			// The tank's gauge, the HMI and the panel's screen live.
			if (tank() != shownTank || !buffer.variant.equals(shownLiquid)) {
				shownTank = tank();
				shownLiquid = buffer.variant;
				sync();
			}
		}
		Drums.Kind kind = kindNow();
		switch (state) {
			case EMPTY -> {
				if (now >= incoming && pallets > 0 && kind != null && placeOf(kind.ordinal()) >= 0 && enough(now)
						&& WarehousePower.ok(level, worldPosition, WarehousePower.Circuit.LIQUIDS)) {
					pallets--;
					state = DISPENSING;
					since = now;
					until = now + SLIDE;
					level.playSound(null, worldPosition, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 0.5F, 0.8F);
					sync();
				}
			}
			case DISPENSING -> {
				if (now >= until) {
					state = BUILDING;
					build = new PalletLoad();
					buildLiquid = buffer.variant;
					since = now;
					lastWork = now;
					sync();
				}
			}
			case BUILDING -> building(level, now, kind);
			case STRAPPING -> {
				if (now >= until) {
					state = READY;
					since = now;
					level.playSound(null, worldPosition, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 0.6F, 1.2F);
					sync();
				}
			}
			case RETURNING -> returning(level, now);
			case STOWING -> {
				if (now >= until) {
					if (pallets < PALLETS) {
						pallets++;
					} else {
						// (Its pallet magazine was filled up by hand meanwhile: this one comes out.)
						Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5,
								new ItemStack(JugcraftWarehouse.PALLET));
					}
					state = EMPTY;
					since = now;
					sync();
				}
			}
			default -> {
			}
		}
	}

	private void building(ServerLevel level, long now, Drums.@Nullable Kind kind) {
		if (build == null) {
			state = EMPTY;
			sync();
			return;
		}
		if (plan != null) {
			if (now >= plan.end()) {
				build.add(new CaseLoad(Drums.book(buildLiquid), (int) plan.amount()));
				plan = null;
				lastWork = now;
				if (build.size() >= Drums.PER_PALLET) {
					strap(now);
				}
				sync();
			}
			return;
		}
		boolean same = kind != null && buffer.variant.equals(buildLiquid);
		int place = kind == null ? -1 : placeOf(kind.ordinal());
		if (same && place >= 0 && enough(now)) {
			long amount = Math.min(Drums.CAPACITY, tank());
			buffer.amount -= Drums.droplets(amount);
			// (What is left under a millibucket goes with the drum.)
			if (buffer.amount < Drums.droplets(1)) {
				buffer.amount = 0;
				buffer.variant = FluidVariant.blank();
			}
			magazine[place] = -1;
			plan = new FillerMotion.Plan(FillerMotion.FILL, now, place, build.size(), kind.ordinal(), amount);
			level.playSound(null, worldPosition, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.4F, 1.3F);
			sync();
			return;
		}
		// Nothing to fill now: a part pallet is strapped once the liquid has stopped coming (or another liquid came); an
		// empty one slides back.
		boolean other = !buffer.isResourceBlank() && !buffer.variant.equals(buildLiquid);
		if (other || now - Math.max(lastWork, lastInflow) >= IDLE * 2) {
			if (build.isEmpty()) {
				state = STOWING;
				since = now;
				until = now + SLIDE;
				build = null;
				sync();
			} else {
				strap(now);
				sync();
			}
		}
	}

	private void strap(long now) {
		state = STRAPPING;
		since = now;
		until = now + STRAP;
	}

	private void returning(ServerLevel level, long now) {
		if (plan != null) {
			if (now >= plan.end()) {
				magazine[plan.place()] = plan.kind();
				plan = null;
				sync();
			}
			return;
		}
		if (returned.isEmpty()) {
			state = STOWING;
			since = now;
			until = now + SLIDE;
			sync();
			return;
		}
		int place = freePlace();
		if (place < 0) {
			// (The magazine filled up meanwhile: what is left goes out as drums.)
			for (int k : returned) {
				Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5,
						new ItemStack(Drums.item(Drums.Kind.values()[k])));
			}
			returned.clear();
			sync();
			return;
		}
		int spot = returned.size() - 1;
		int k = returned.remove(spot);
		plan = new FillerMotion.Plan(FillerMotion.RETURN, now, place, spot, k, 0);
		sync();
	}

	/** Liquid came in through the Liquids In panel (mB), for the screens. */
	void counted(long droplets) {
		inflow += droplets;
	}

	/** A pallet drone lifts the strapped pallet off (Planner PICK_PALLET). */
	public @Nullable PalletLoad takePallet(long now) {
		if (state != READY || build == null) {
			return null;
		}
		PalletLoad out = build;
		build = null;
		buildLiquid = FluidVariant.blank();
		state = EMPTY;
		since = now;
		sync();
		return out;
	}

	/** A pallet drone set a pallet of emptied drums on the bay (Planner PUT_FILLER): the robot stows them. */
	boolean receivePallet(@Nullable PalletLoad load, long now) {
		incoming = 0;
		if (load == null || !takesBack(load.size())) {
			return false;
		}
		returned.clear();
		for (CaseLoad c : load.cases()) {
			Drums.Kind k = Drums.kindOf(c.item());
			returned.add(k == null ? 0 : k.ordinal());
		}
		state = RETURNING;
		since = now;
		sync();
		return true;
	}

	private void sync() {
		setChanged();
		if (level != null) {
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
		}
	}

	/** Loads empty drums (any kind, a place each) or pallets; used empty-handed, says what it holds. */
	@Override
	public InteractionResult use(Level level, BlockPos clicked, Player player, ItemStack stack, InteractionHand hand) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		Drums.Kind kind = Drums.kindOf(stack);
		if (kind != null) {
			if (DrumItem.contents(stack) != null) {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.warehouse.drum_full"));
				return InteractionResult.SUCCESS;
			}
			int n = player.isSecondaryUseActive() ? stack.getCount() : 1, in = 0;
			for (int i = 0; i < n; i++) {
				int place = freePlace();
				if (place < 0) {
					break;
				}
				magazine[place] = kind.ordinal();
				in++;
			}
			if (in == 0) {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.warehouse.drum_magazine_full"));
				return InteractionResult.SUCCESS;
			}
			if (!player.getAbilities().instabuild) {
				stack.shrink(in);
			}
			level.playSound(null, worldPosition, SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.BLOCKS, 0.7F, 0.8F);
			sync();
		} else if (stack.is(JugcraftWarehouse.PALLET)) {
			int room = PALLETS - pallets;
			if (room <= 0) {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.warehouse.magazine_full"));
				return InteractionResult.SUCCESS;
			}
			int n = Math.min(room, player.isSecondaryUseActive() ? stack.getCount() : 1);
			pallets += n;
			if (!player.getAbilities().instabuild) {
				stack.shrink(n);
			}
			level.playSound(null, worldPosition, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 0.8F, 1.0F);
			sync();
		}
		player.sendOverlayMessage(Component.translatable("message.jugcraft.warehouse.drum_magazine", drums(Drums.Kind.STEEL), drums(Drums.Kind.RED),
				drums(Drums.Kind.POLY), drums(Drums.Kind.DEWAR), freePlaces(), pallets));
		return InteractionResult.SUCCESS;
	}

	@Override
	public void dropContents() {
		if (level == null) {
			return;
		}
		double x = worldPosition.getX() + 0.5, y = worldPosition.getY() + 0.5, z = worldPosition.getZ() + 0.5;
		List<ItemStack> out = new ArrayList<>();
		for (int k : magazine) {
			if (k >= 0) {
				out.add(new ItemStack(Drums.item(Drums.Kind.values()[k])));
			}
		}
		for (int k : returned) {
			out.add(new ItemStack(Drums.item(Drums.Kind.values()[k])));
		}
		if (build != null) {
			out.addAll(build.stacks());
		}
		if (plan != null) {
			Drums.Kind k = Drums.Kind.values()[plan.kind()];
			out.add(plan.type() == FillerMotion.FILL ? Drums.dropped(new CaseLoad(Drums.book(buildLiquid.isBlank() ? FluidVariant.blank() : buildLiquid),
					(int) plan.amount())) : new ItemStack(Drums.item(k)));
		}
		int p = pallets + (build != null || state == DISPENSING || state == STOWING || state == RETURNING ? 1 : 0);
		if (p > 0) {
			out.add(new ItemStack(JugcraftWarehouse.PALLET, p));
		}
		for (ItemStack s : out) {
			Containers.dropItemStack(level, x, y, z, s);
		}
		Arrays.fill(magazine, -1);
		returned.clear();
		build = null;
		plan = null;
		pallets = 0;
		state = EMPTY;
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
		int[] mag = input.read("magazine", Codec.INT.listOf()).orElse(List.of()).stream().mapToInt(Integer::intValue).toArray();
		Arrays.fill(magazine, -1);
		System.arraycopy(mag, 0, magazine, 0, Math.min(mag.length, magazine.length));
		pallets = input.getIntOr("pallets", 0);
		state = input.getIntOr("state", EMPTY);
		since = input.getLongOr("since", 0);
		until = input.getLongOr("until", 0);
		build = input.read("build", PalletLoad.CODEC).orElse(null);
		buildLiquid = input.read("build_liquid", FluidVariant.CODEC).orElseGet(FluidVariant::blank);
		plan = input.read("plan", FillerMotion.Plan.CODEC).orElse(null);
		returned.clear();
		returned.addAll(input.read("returned", Codec.INT.listOf()).orElse(List.of()));
		buffer.variant = input.read("liquid", FluidVariant.CODEC).orElseGet(FluidVariant::blank);
		buffer.amount = Math.max(0, input.getLongOr("amount", 0));
		lastInflow = input.getLongOr("last_inflow", 0);
		lastWork = input.getLongOr("last_work", 0);
		inflowShown = input.getLongOr("inflow", 0);
		if (build == null && (state == BUILDING || state == STRAPPING || state == READY)) {
			state = EMPTY;
		}
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		List<Integer> mag = new ArrayList<>();
		for (int k : magazine) {
			mag.add(k);
		}
		output.store("magazine", Codec.INT.listOf(), mag);
		output.putInt("pallets", pallets);
		output.putInt("state", state);
		output.putLong("since", since);
		output.putLong("until", until);
		if (build != null) {
			output.store("build", PalletLoad.CODEC, build);
		}
		if (!buildLiquid.isBlank()) {
			output.store("build_liquid", FluidVariant.CODEC, buildLiquid);
		}
		if (plan != null) {
			output.store("plan", FillerMotion.Plan.CODEC, plan);
		}
		output.store("returned", Codec.INT.listOf(), returned);
		if (!buffer.variant.isBlank()) {
			output.store("liquid", FluidVariant.CODEC, buffer.variant);
		}
		output.putLong("amount", buffer.amount);
		output.putLong("last_inflow", lastInflow);
		output.putLong("last_work", lastWork);
		output.putLong("inflow", inflowShown);
	}
}
