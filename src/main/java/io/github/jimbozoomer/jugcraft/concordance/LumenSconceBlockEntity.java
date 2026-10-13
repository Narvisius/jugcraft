package io.github.jimbozoomer.jugcraft.concordance;

import io.github.jimbozoomer.jugcraft.concordance.resource.Overflow;
import io.github.jimbozoomer.jugcraft.concordance.resource.Ownership;
import io.github.jimbozoomer.jugcraft.concordance.resource.RateBudget;
import io.github.jimbozoomer.jugcraft.concordance.resource.Reservoir;
import io.github.jimbozoomer.jugcraft.concordance.resource.ResourceType;
import io.github.jimbozoomer.jugcraft.concordance.resource.Transfers;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * A Lumen Sconce's Radiance: a {@link Reservoir} of {@code essence/radiance} holding up to {@value #CAPACITY} measures,
 * burning one every {@value #BURN_TICKS} ticks while it has any (worked out from the game time, like a lantern), and
 * giving light 15 while it burns.
 * <p>
 * Radiance moves in and out only through {@link Transfers}: anyone may pour from a Kindled Lantern into it (the sconce
 * is open for offerings, so a Lampwright can keep a town's lamps lit), at most {@value #POUR} per pour and
 * {@value #RATE_LIMIT} per {@value #RATE_WINDOW} ticks; what does not fit stays in the lantern. Only the player who
 * placed it may draw Radiance back out into a lantern. Broken, it keeps its Radiance as the item's
 * {@code jugcraft:radiance}, less the measure it was burning. Keep the numbers equal to tools/concordance.py.
 */
public class LumenSconceBlockEntity extends BlockEntity {
	public static final int CAPACITY = KindledLanternItem.CAPACITY;
	public static final int BURN_TICKS = 1200;
	public static final int POUR = 16;
	public static final int RATE_LIMIT = 32;
	public static final int RATE_WINDOW = 20;

	private long stored;
	private long since;
	private @Nullable UUID owner;
	private RateBudget budget = RateBudget.of(RATE_LIMIT, RATE_WINDOW);

	public LumenSconceBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftConcordance.SCONCE_ENTITY, pos, state);
	}

	private long now() {
		return level == null ? 0L : ConcordanceProgress.now(level);
	}

	/** The Radiance left at {@code now}. */
	public long remaining(long now) {
		if (stored == 0 || now <= since) {
			return stored;
		}
		return Math.max(0L, stored - (now - since) / BURN_TICKS);
	}

	/** Takes off what has burnt, keeping the part of the measure now burning, so nothing is lost or gained. */
	private void settle(long now) {
		if (stored == 0 || now <= since) {
			since = Math.max(since, now);
			return;
		}
		long burnt = (now - since) / BURN_TICKS;
		stored = Math.max(0L, stored - burnt);
		since = stored == 0 ? now : since + burnt * BURN_TICKS;
	}

	/** The sconce as a typed container at {@code now}. */
	public Reservoir reservoir(long now) {
		return new Reservoir(ResourceType.RADIANCE, remaining(now), CAPACITY);
	}

	public Ownership ownership() {
		return owner == null ? Ownership.NONE : Ownership.of(owner, true);
	}

	public @Nullable UUID owner() {
		return owner;
	}

	void setOwner(@Nullable UUID owner) {
		this.owner = owner;
		setChanged();
	}

	/** Pours from the Kindled Lantern {@code lantern} (held by {@code actor}) into the sconce. */
	public Transfers.Transfer pour(ServerLevel level, UUID actor, ItemStack lantern) {
		long now = ConcordanceProgress.now(level);
		settle(now);
		Transfers.Side from = new Transfers.Side(lanternReservoir(lantern, now), Ownership.NONE);
		Transfers.Side to = new Transfers.Side(reservoir(now), ownership());
		Transfers.Transfer transfer = Transfers.move(from, to, actor, POUR, budget, now, Overflow.FILL);
		apply(level, lantern, now, transfer.source(), transfer.target(), transfer);
		return transfer;
	}

	/** Draws Radiance back into the Kindled Lantern {@code lantern}; only the sconce's owner may. */
	public Transfers.Transfer draw(ServerLevel level, UUID actor, ItemStack lantern) {
		long now = ConcordanceProgress.now(level);
		settle(now);
		Transfers.Side from = new Transfers.Side(reservoir(now), ownership());
		Transfers.Side to = new Transfers.Side(lanternReservoir(lantern, now), Ownership.NONE);
		Transfers.Transfer transfer = Transfers.move(from, to, actor, POUR, budget, now, Overflow.FILL);
		apply(level, lantern, now, transfer.target(), transfer.source(), transfer);
		return transfer;
	}

	private static Reservoir lanternReservoir(ItemStack lantern, long now) {
		return new Reservoir(ResourceType.RADIANCE, KindledLanternItem.remaining(lantern, now), KindledLanternItem.CAPACITY);
	}

	private void apply(ServerLevel level, ItemStack lantern, long now, Reservoir lanternAfter, Reservoir sconceAfter,
			Transfers.Transfer transfer) {
		if (!transfer.outcome().moved()) {
			return;
		}
		KindledLanternItem.set(lantern, (int) lanternAfter.amount(), now, KindledLanternItem.lit(lantern));
		stored = sconceAfter.amount();
		budget = transfer.budget();
		refresh(level, now);
		setChanged();
	}

	/** Brings the block's light into line with what is left, and wakes it when the last measure burns out. */
	public void refresh(ServerLevel level, long now) {
		settle(now);
		boolean lit = stored > 0;
		BlockState state = getBlockState();
		if (state.getValue(LumenSconceBlock.LIT) != lit) {
			level.setBlock(worldPosition, state.setValue(LumenSconceBlock.LIT, lit), Block.UPDATE_ALL);
		}
		if (lit) {
			// The level keeps one pending tick per block and ignores a second, so a draw that brings the end forward
			// cannot move an earlier-scheduled tick: checking at least once per burnt measure keeps the light at most
			// one measure late going out.
			long untilOut = since + stored * BURN_TICKS - now;
			level.scheduleTick(worldPosition, state.getBlock(), (int) Math.clamp(untilOut, 1L, (long) BURN_TICKS));
		}
	}

	// ---------------------------------------------------------------- saving and the item form

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		stored = Math.clamp(input.getLongOr("stored", 0L), 0L, CAPACITY);
		since = input.getLongOr("since", 0L);
		owner = input.read("owner", UUIDUtil.CODEC).orElse(null);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		Saved.stamp(output, 1);
		output.putLong("stored", stored);
		output.putLong("since", since);
		if (owner != null) {
			output.store("owner", UUIDUtil.CODEC, owner);
		}
	}

	/**
	 * The Radiance kept if it is taken down at {@code now}: a measure begun is a measure spent (roadmap step 29), so
	 * breaking a sconce and placing it again never gives light for nothing.
	 */
	public long kept(long now) {
		if (stored == 0 || now <= since) {
			return stored;
		}
		return Math.max(0L, stored - Math.ceilDiv(now - since, (long) BURN_TICKS));
	}

	/** Broken, the sconce keeps what is left as an unlit charge on the item, less the measure it was burning. */
	@Override
	protected void collectImplicitComponents(DataComponentMap.Builder components) {
		super.collectImplicitComponents(components);
		long now = now();
		long left = kept(now);
		if (left > 0) {
			components.set(JugcraftConcordance.RADIANCE, new LanternCharge((int) left, now));
		}
	}

	/** Placed again, it starts burning what the item held from the moment it is placed. */
	@Override
	protected void applyImplicitComponents(DataComponentGetter components) {
		super.applyImplicitComponents(components);
		LanternCharge charge = components.get(JugcraftConcordance.RADIANCE);
		if (charge != null) {
			stored = Math.clamp(charge.stored(), 0, CAPACITY);
			since = now();
		}
	}

	/** Called once placed (the level is set): light and schedule from the charge it was placed with. */
	void placed(Level level) {
		if (level instanceof ServerLevel server) {
			since = ConcordanceProgress.now(server);
			refresh(server, since);
		}
	}
}
