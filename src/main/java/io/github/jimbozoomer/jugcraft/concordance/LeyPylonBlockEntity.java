package io.github.jimbozoomer.jugcraft.concordance;

import io.github.jimbozoomer.jugcraft.concordance.resource.Conversion;
import io.github.jimbozoomer.jugcraft.concordance.resource.Reservoir;
import io.github.jimbozoomer.jugcraft.concordance.resource.ResourceKind;
import io.github.jimbozoomer.jugcraft.concordance.resource.ResourceType;
import io.github.jimbozoomer.jugcraft.energy.SimpleEnergyStorage;
import java.util.Collection;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * A Ley Pylon's Ley Charge: a {@link Reservoir} of {@code ley_charge} holding up to {@value #CAPACITY}, which a ritual
 * draws from at every step (roadmap step 12). Two ways in, neither of which comes back out:
 * <ul>
 * <li>Radiance poured from a Kindled Lantern, through the data conversion {@code jugcraft:radiance_to_ley} in whole
 * batches, at most {@value #POUR} Radiance a pour (a third is lost);</li>
 * <li>Jugcraft Energy through the shared energy interface ({@link #energy}), {@value #JE_PER_LEY} JE for one Ley
 * Charge, accepted at up to {@value #JE_RATE} JE a tick while there is room.</li>
 * </ul>
 * The player who placed it owns it: a ritual draws only from pylons owned by a participant or by no one. Broken, it
 * keeps its charge on the item ({@code jugcraft:ley_charge}). Keep the numbers equal to tools/concordance_rituals.py.
 */
public class LeyPylonBlockEntity extends BlockEntity {
	public static final int CAPACITY = 64;
	public static final int POUR = 15;
	public static final int JE_PER_LEY = 1000;
	public static final int JE_RATE = 64;
	public static final ResourceType LEY = ResourceType.of(ResourceKind.LEY_CHARGE);
	public static final String CONVERSION = "jugcraft:radiance_to_ley";

	private long ley;
	private @Nullable UUID owner;
	/** Electricity waiting to become Ley Charge: it takes no more once the pylon is full. */
	public final SimpleEnergyStorage energy = new SimpleEnergyStorage(JE_PER_LEY, JE_RATE, 0, this::setChanged,
			() -> ley < CAPACITY ? JE_PER_LEY : 0L);

	public LeyPylonBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftConcordance.PYLON_ENTITY, pos, state);
	}

	public long ley() {
		return ley;
	}

	public Reservoir reservoir() {
		return new Reservoir(LEY, ley, CAPACITY);
	}

	public @Nullable UUID owner() {
		return owner;
	}

	/** Placement, operators and tests: who owns the pylon (null: no one, so any ritual may draw on it). */
	public void setOwner(@Nullable UUID owner) {
		this.owner = owner;
		setChanged();
	}

	/** Whether a ritual whose participants are {@code participants} may draw on this pylon. */
	public boolean lends(Collection<UUID> participants) {
		return owner == null || participants.contains(owner);
	}

	/** The result of pouring a lantern in: Radiance taken, Ley Charge made, or why nothing moved. */
	public record Pour(int radiance, int ley, @Nullable String refusal) {
	}

	/**
	 * Pours from a Kindled Lantern in whole batches of the conversion: as many as the lantern, the pylon's room and
	 * {@value #POUR} Radiance allow. What does not make a whole batch stays in the lantern.
	 */
	public Pour pour(ServerLevel level, ItemStack lantern, @Nullable Conversion conversion) {
		if (conversion == null || !conversion.from().equals(ResourceType.RADIANCE) || !conversion.to().equals(LEY)) {
			return new Pour(0, 0, "disabled");
		}
		long now = ConcordanceProgress.now(level);
		int held = KindledLanternItem.remaining(lantern, now);
		if (held == 0) {
			return new Pour(0, 0, "lantern_empty");
		}
		long batches = conversion.batches(held, CAPACITY - ley, POUR / conversion.fromAmount());
		if (batches == 0) {
			return new Pour(0, 0, held < conversion.fromAmount() ? "too_little" : "full");
		}
		int radiance = (int) (batches * conversion.fromAmount());
		int made = (int) (batches * conversion.toAmount());
		KindledLanternItem.set(lantern, held - radiance, now, KindledLanternItem.lit(lantern));
		change(level, ley + made);
		return new Pour(radiance, made, null);
	}

	/**
	 * Draws {@code amount} for a ritual step, all or none; returns whether it did. The anchor checks every channel
	 * first and draws in the same tick, so a step draws from all its pylons or from none.
	 */
	public boolean draw(ServerLevel level, long amount) {
		Reservoir.Extraction extraction = reservoir().extract(LEY, amount, true);
		if (extraction.outcome() != Reservoir.Outcome.DONE && amount > 0) {
			return false;
		}
		change(level, extraction.reservoir().amount());
		return true;
	}

	/**
	 * Takes up to {@code amount} Ley Charge made elsewhere by a conversion (roadmap step 14: a Verdant Heart's pour);
	 * returns how much it took. The caller has already spent what made it.
	 */
	public long fill(ServerLevel level, long amount) {
		long taken = Math.clamp(amount, 0L, CAPACITY - ley);
		if (taken > 0) {
			change(level, ley + taken);
		}
		return taken;
	}

	/** Operators and tests: sets the charge. */
	public void setLey(ServerLevel level, long amount) {
		change(level, Math.clamp(amount, 0L, CAPACITY));
	}

	private void change(ServerLevel level, long amount) {
		ley = amount;
		setChanged();
		BlockState state = getBlockState();
		boolean charged = ley > 0;
		if (state.getValue(LeyPylonBlock.CHARGED) != charged) {
			level.setBlock(worldPosition, state.setValue(LeyPylonBlock.CHARGED, charged), Block.UPDATE_ALL);
		}
		// A change of charge is a change to every circle drawing on this pylon.
		Rituals.changed(level, worldPosition);
	}

	/** Called once placed (the level is set): the block shows the charge the item carried. */
	void placed(ServerLevel level) {
		change(level, ley);
	}

	/** Turns stored electricity into Ley Charge, one whole unit at a time. */
	void serverTick(ServerLevel level) {
		if (energy.getAmount() >= JE_PER_LEY && ley < CAPACITY) {
			energy.setAmount(energy.getAmount() - JE_PER_LEY);
			change(level, ley + 1);
		}
	}

	// ---------------------------------------------------------------- saving and the item form

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		ley = Math.clamp(input.getLongOr("ley", 0L), 0L, CAPACITY);
		energy.setAmount(input.getLongOr("energy", 0L));
		owner = input.read("owner", UUIDUtil.CODEC).orElse(null);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		Saved.stamp(output, 1);
		output.putLong("ley", ley);
		output.putLong("energy", energy.getAmount());
		if (owner != null) {
			output.store("owner", UUIDUtil.CODEC, owner);
		}
	}

	/** Broken, the pylon keeps its Ley Charge on the item (electricity not yet turned is lost). */
	@Override
	protected void collectImplicitComponents(DataComponentMap.Builder components) {
		super.collectImplicitComponents(components);
		if (ley > 0) {
			components.set(JugcraftConcordance.LEY_CHARGE, (int) ley);
		}
	}

	@Override
	protected void applyImplicitComponents(DataComponentGetter components) {
		super.applyImplicitComponents(components);
		Integer charge = components.get(JugcraftConcordance.LEY_CHARGE);
		if (charge != null) {
			ley = Math.clamp(charge, 0, CAPACITY);
		}
	}
}
