package io.github.jimbozoomer.jugcraft.concordance.garden;

import io.github.jimbozoomer.jugcraft.concordance.ComposeText;
import io.github.jimbozoomer.jugcraft.concordance.Saved;
import io.github.jimbozoomer.jugcraft.concordance.compose.Text;
import io.github.jimbozoomer.jugcraft.concordance.ecology.Habitat;
import io.github.jimbozoomer.jugcraft.concordance.ecology.Organism;
import io.github.jimbozoomer.jugcraft.concordance.ecology.Sampler;
import io.github.jimbozoomer.jugcraft.concordance.ecology.Verdict;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * What a Verdant Bed holds (roadmap step 14): its nutrients (0 to {@value #CAPACITY}), whether a Greenwarden has woken
 * it and who placed it, and its last sample of the area round the plant above it, reused for
 * {@value Sampler#FRESH_TICKS} ticks. Clients are sent the nutrients, whether it is awake and its crop's last verdict
 * (growth and the factors holding it back), so a crop's tooltip can say why; nothing a client sends changes it.
 * <p>
 * Nutrients only change through {@link #take} (a step of growth, a Heart's beat), {@link #give} (bone meal, fertilizer,
 * a Maw, a fixer) and nothing else. A broken bed loses them.
 */
public class VerdantBedBlockEntity extends BlockEntity {
	public static final int CAPACITY = Habitat.MAX_NUTRIENTS;

	private int nutrients;
	private boolean awake;
	private @Nullable UUID keeper;
	/** The area's last sample, and when (game time); not saved: the first reading after a load samples again. */
	private Sampler.@Nullable Area area;
	private long sampledAt = Long.MIN_VALUE;
	/** The crop above's last verdict, for clients: its growth id and its reasons as "key|factor|value|a|b". */
	private String growth = "";
	private List<String> reasons = List.of();

	public VerdantBedBlockEntity(BlockPos pos, BlockState state) {
		super(Garden.BED_ENTITY, pos, state);
	}

	public int nutrients() {
		return nutrients;
	}

	public boolean awake() {
		return awake;
	}

	public @Nullable UUID keeper() {
		return keeper;
	}

	public String growth() {
		return growth;
	}

	public List<String> reasons() {
		return reasons;
	}

	public int moisture() {
		return getBlockState().getValue(VerdantBedBlock.MOISTURE);
	}

	// ---------------------------------------------------------------- nutrients

	/** Takes exactly {@code amount} nutrients, or none; returns whether it did. */
	public boolean take(int amount) {
		if (amount < 0 || amount > nutrients) {
			return false;
		}
		set(nutrients - amount);
		return true;
	}

	/** Gives up to {@code amount} nutrients (what does not fit is never made); returns how many it took. */
	public int give(int amount) {
		int taken = Math.clamp(amount, 0, CAPACITY - nutrients);
		if (taken > 0) {
			set(nutrients + taken);
		}
		return taken;
	}

	/** Operators and tests: sets the nutrients. */
	public void setNutrients(int amount) {
		set(Math.clamp(amount, 0, CAPACITY));
	}

	private void set(int amount) {
		nutrients = amount;
		setChanged();
		sync();
	}

	/** A step of growth dries the bed by one. */
	public void dry(ServerLevel level) {
		BlockState state = getBlockState();
		int moisture = state.getValue(VerdantBedBlock.MOISTURE);
		if (moisture > 0) {
			level.setBlock(worldPosition, state.setValue(VerdantBedBlock.MOISTURE, moisture - 1), Block.UPDATE_CLIENTS);
		}
	}

	// ---------------------------------------------------------------- waking

	void placedBy(ServerPlayer player, boolean greenwarden) {
		keeper = player.getUUID();
		awake = greenwarden;
		setChanged();
		sync();
	}

	/** Woken by a Greenwarden (who becomes its keeper if it had none). */
	public void awaken(UUID by) {
		awake = true;
		if (keeper == null) {
			keeper = by;
		}
		setChanged();
		sync();
	}

	// ---------------------------------------------------------------- the habitat

	/**
	 * The habitat for a plant on this bed: the bed's moisture and nutrients, the server's light at the plant (sky or
	 * block, never the time of day) and the area's sample, taken again when older than {@value Sampler#FRESH_TICKS}
	 * ticks if the level's allowance permits. Null only when there has never been a sample and none can be taken this
	 * tick (the plant waits for a reading).
	 */
	public @Nullable Habitat habitat(ServerLevel level) {
		BlockPos plant = worldPosition.above();
		long now = level.getGameTime();
		if (area == null || now - sampledAt >= Sampler.FRESH_TICKS || now < sampledAt) {
			Sampler.Area fresh = Garden.sample(level, plant);
			if (fresh != null) {
				area = fresh;
				sampledAt = now;
			}
		}
		if (area == null) {
			return null;
		}
		return Sampler.habitat(moisture(), level.getRawBrightness(plant, 0), nutrients, area);
	}

	/** Forgets the area's sample, so the next reading takes a fresh one (a test changes the area round it). */
	public void forgetSample() {
		area = null;
		sampledAt = Long.MIN_VALUE;
	}

	/** Records what the crop or device above makes of the habitat, for its tooltip; sends it only when it changes. */
	public void note(Verdict verdict, Habitat habitat, Organism organism) {
		List<String> encoded = new ArrayList<>();
		for (Text line : verdict.reasons(organism.niche(), habitat)) {
			StringBuilder out = new StringBuilder(line.key());
			for (Object arg : line.args()) {
				out.append('|').append(arg instanceof Text.Ref ref ? ref.id() : String.valueOf(arg));
			}
			encoded.add(out.toString());
		}
		note(verdict.growth().id, encoded);
	}

	/** Records a growth word with no reasons (a dormant bed, a crop waiting for a reading). */
	public void note(String growth, List<String> reasons) {
		if (!growth.equals(this.growth) || !reasons.equals(this.reasons)) {
			this.growth = growth;
			this.reasons = List.copyOf(reasons);
			sync();
		}
	}

	/** Tells a player how the bed stands: its moisture and nutrients, and its last reading. */
	public void report(ServerLevel level, ServerPlayer player) {
		player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.jugcraft.concordance.garden.bed",
				moisture(), nutrients, CAPACITY));
		if (!awake) {
			player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.jugcraft.concordance.garden.dormant"));
			return;
		}
		Habitat habitat = habitat(level);
		if (habitat != null) {
			player.sendSystemMessage(ComposeText.show(Text.of("ecology.habitat", habitat.moisture(), habitat.light(),
					habitat.nutrients(), habitat.diversity(), habitat.disturbance())));
		}
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
		nutrients = Math.clamp(input.getIntOr("nutrients", 0), 0, CAPACITY);
		awake = input.getBooleanOr("awake", false);
		keeper = input.read("keeper", UUIDUtil.CODEC).orElse(null);
		growth = input.getStringOr("growth", "");
		String joined = input.getStringOr("reasons", "");
		reasons = joined.isEmpty() ? List.of() : List.of(joined.split("\n"));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		Saved.stamp(output, 1);
		output.putInt("nutrients", nutrients);
		output.putBoolean("awake", awake);
		if (keeper != null) {
			output.store("keeper", UUIDUtil.CODEC, keeper);
		}
		output.putString("growth", growth);
		output.putString("reasons", String.join("\n", reasons));
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	/** Clients get the nutrients, whether it is awake and the crop's last verdict; never the keeper. */
	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		CompoundTag tag = new CompoundTag();
		tag.putInt("nutrients", nutrients);
		tag.putBoolean("awake", awake);
		tag.putString("growth", growth);
		tag.putString("reasons", String.join("\n", reasons));
		return tag;
	}
}
