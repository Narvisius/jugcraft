package io.github.jimbozoomer.jugcraft.concordance.garden;

import io.github.jimbozoomer.jugcraft.concordance.ComposeText;
import io.github.jimbozoomer.jugcraft.concordance.Saved;
import io.github.jimbozoomer.jugcraft.concordance.compose.Text;
import io.github.jimbozoomer.jugcraft.concordance.ecology.Graft;
import io.github.jimbozoomer.jugcraft.concordance.ecology.Habitat;
import io.github.jimbozoomer.jugcraft.concordance.ecology.HabitatInspection;
import io.github.jimbozoomer.jugcraft.concordance.ecology.Niche;
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
import net.minecraft.world.level.LightLayer;
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
 * (growth and the factors holding it back), so a crop's tooltip can say why; the verdict is forgotten on the bed's next
 * random tick once nothing that notes it stands on it (Concordance Chunk C, C-07); nothing a client sends changes it.
 * <p>
 * Nutrients only change through {@link #take} (a step of growth, a Heart's beat), {@link #give} (bone meal, fertilizer,
 * a Maw, a fixer) and nothing else. A broken bed loses them.
 * <p>
 * Concordance Chunk C (C-08, C-P59 to C-P61): harvest strain. Growth notes when its crop ripens ({@link #ripen}); the next
 * time it sees that crop below ripe or gone (a growth attempt, its random tick, compost or tending) it counts one harvest,
 * up to {@link Habitat#MAX_STRAIN} ({@link #settle}), whatever took the crop. It rests away one a random tick while no crop
 * grows on it, one a dose of compost, one a Greenwarden's tending; {@link #give} never eases it. Saved as optional keys
 * (stamp 1) and sent to clients.
 * <p>
 * Since Concordance Chunk C (C-09, C-P62 to C-P65) it may carry a graft for its host's crop, kept through every harvest and
 * replanting until another organism grows here; clients are told it and the organism it is for. Every reader of the niche
 * a plant grows by here asks {@link #niche}.
 */
public class VerdantBedBlockEntity extends BlockEntity {
	public static final int CAPACITY = Habitat.MAX_NUTRIENTS;
	/** Saved and sent keys (Concordance Chunk C, C-08): in a save only when set; absent in older saves (read as 0 and false). */
	public static final String STRAIN = "strain";
	public static final String RIPENED = "ripened";
	/** Saved and sent key (C-09, C-P65): the graft it carries, in a save only when it carries one; clients are also told its host. */
	public static final String GRAFT = "graft";
	public static final String GRAFT_HOST = "graft_host";

	private int nutrients;
	private boolean awake;
	private @Nullable UUID keeper;
	/** The area's last sample, and when (game time); not saved: the first reading after a load samples again. */
	private Sampler.@Nullable Area area;
	private long sampledAt = Long.MIN_VALUE;
	/** The crop above's last verdict, for clients: its growth id and its reasons as "key|factor|value|a|b". */
	private String growth = "";
	private List<String> reasons = List.of();
	/**
	 * C-08: what its harvests have strained it (0 to Habitat.MAX_STRAIN), and whether its crop has ripened since the last
	 * harvest it counted.
	 */
	private int strain;
	private boolean ripened;
	/** When a Greenwarden last tended it (game time); not saved. */
	private long tendedAt = Long.MIN_VALUE;
	/** The graft it carries for its host's crop (Concordance Chunk C, C-09), by id; "" for none. */
	private String graft = "";
	/** Clients only: the organism that graft is for, as the server sent it ("" for none). */
	private String graftFor = "";

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
		return Sampler.habitat(moisture(), level.getRawBrightness(plant, 0), nutrients, area, strainNow());
	}

	/** Forgets the area's sample, so the next reading takes a fresh one (a test changes the area round it). */
	public void forgetSample() {
		area = null;
		sampledAt = Long.MIN_VALUE;
	}

	/**
	 * The reading the encyclopedia's Inspect page shows (Concordance Chunk C, C-07, C-P14, C-P54): the habitat growth would
	 * read now (the cached sample, taken again only as a plant would take it) and the light at the plant as its two sources,
	 * sky and lamps (the habitat's light is the greater). Null when there is no reading this tick. It only reads.
	 */
	public HabitatInspection.@Nullable State inspect(ServerLevel level) {
		Habitat habitat = habitat(level);
		if (habitat == null) {
			return null;
		}
		BlockPos plant = worldPosition.above();
		return new HabitatInspection.State(habitat, level.getBrightness(LightLayer.SKY, plant), level.getBrightness(LightLayer.BLOCK, plant),
				Garden.WATER_REACH);
	}

	/**
	 * Records what the crop or device above makes of the habitat, for its tooltip; sends it only when it changes. A mixed
	 * bed's line (Concordance Chunk C, C-08) follows the verdict's reasons.
	 */
	public void note(Verdict verdict, Habitat habitat, Organism organism) {
		List<String> encoded = new ArrayList<>();
		for (Text line : verdict.reasons(niche(organism), habitat)) {
			encoded.add(encode(line));
		}
		Text mixed = mixedLine(organism);
		if (mixed != null) {
			encoded.add(encode(mixed));
		}
		note(verdict.growth().id, encoded);
	}

	/** A line as a client reads it back: "key|arg|arg", a named thing by its id. */
	private static String encode(Text line) {
		StringBuilder out = new StringBuilder(line.key());
		for (Object arg : line.args()) {
			out.append('|').append(arg instanceof Text.Ref ref ? ref.id() : String.valueOf(arg));
		}
		return out.toString();
	}

	/** Records a growth word with no reasons (a dormant bed, a crop waiting for a reading). */
	public void note(String growth, List<String> reasons) {
		if (!growth.equals(this.growth) || !reasons.equals(this.reasons)) {
			this.growth = growth;
			this.reasons = List.copyOf(reasons);
			sync();
		}
	}

	/**
	 * Forgets the last verdict once nothing that notes it stands on the bed (Concordance Chunk C, C-07, C-P55: the bed's
	 * random tick asks), and at once when another crop is set on it or it is grafted (OrganismCropBlock: the unit review),
	 * so a crop's tooltip never speaks for a plant that is gone or for its niche before a graft; sent to clients and saved.
	 * Only the verdict: the nutrients and whether it is awake stay.
	 */
	public void forgetVerdict() {
		if (!growth.isEmpty() || !reasons.isEmpty()) {
			note("", List.of());
			setChanged();
		}
	}

	/** Tells a player how the bed stands: its moisture and nutrients, and its last reading. */
	public void report(ServerLevel level, ServerPlayer player) {
		player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.jugcraft.concordance.garden.bed",
				moisture(), nutrients, CAPACITY));
		// Concordance Chunk C (C-09): the graft it carries, named.
		List<Text> grafted = Garden.catalog().graftLines(graft);
		if (!grafted.isEmpty()) {
			player.sendSystemMessage(ComposeText.show(grafted.getFirst()));
		}
		if (!awake) {
			player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.jugcraft.concordance.garden.dormant"));
			return;
		}
		Habitat habitat = habitat(level);
		if (habitat != null) {
			player.sendSystemMessage(ComposeText.show(Text.of("ecology.habitat", habitat.moisture(), habitat.light(),
					habitat.nutrients(), habitat.diversity(), habitat.disturbance())));
		}
		// Concordance Chunk C (C-08): its strain, as growth will count it.
		for (Text line : notes(null)) {
			player.sendSystemMessage(ComposeText.show(line));
		}
	}

	// ---------------------------------------------------------------- harvest strain (Concordance Chunk C, C-08, C-P59)

	/** The strain its harvests left, as saved: 0 to Habitat.MAX_STRAIN. */
	public int strain() {
		return strain;
	}

	/** Whether its crop has ripened by growth since the last harvest it counted. */
	public boolean ripened() {
		return ripened;
	}

	/**
	 * The strain the next reading of its plant counts: the saved strain, and one more (up to Habitat.MAX_STRAIN) for a
	 * harvest not yet counted. It only reads, on either side, so the Inspect page, Jade and a client show what growth
	 * counts.
	 */
	public int strainNow() {
		return harvestSeen() ? Math.min(Habitat.MAX_STRAIN, strain + 1) : strain;
	}

	/** Growth has taken its crop to its last step: the next time it stands below ripe (or is gone), it was harvested. */
	void ripen() {
		if (!ripened) {
			ripened = true;
			changed();
		}
	}

	/**
	 * Counts a harvest it saw: its crop ripened and no longer stands ripe on it, whatever took it (a hand, a Gleaner, a Peepo,
	 * a Crop Harvester, a Hay Golem, a sickle, a reap, a crow, breaking). The strain rises by one, at most to
	 * Habitat.MAX_STRAIN, and the ripening is forgotten. Returns whether it counted one.
	 */
	public boolean settle() {
		if (!harvestSeen()) {
			return false;
		}
		ripened = false;
		strain = Math.min(Habitat.MAX_STRAIN, strain + 1);
		changed();
		return true;
	}

	/**
	 * Its random tick (C-P60): a harvest it saw is counted (and it does not rest that tick); otherwise, while no crop grows on
	 * it (fallow, its crop ripe, or its crop stalled or dormant), it rests a strain away. Nothing changes while the
	 * Concordance is switched off.
	 */
	public void rest() {
		if (!Garden.enabled()) {
			return;
		}
		if (!settle() && strain > 0 && !growing()) {
			ease();
		}
	}

	/**
	 * A dose of compost has fed it (C-P61: bone meal or fertilizer on it or its crop, a Mulch Maw's nutrient): a harvest it
	 * saw is counted, then one strain eased. Never called from {@link #give}: a nitrogen fixer's gift is not compost.
	 */
	public void compost() {
		if (!Garden.enabled()) {
			return;
		}
		settle();
		if (strain > 0) {
			ease();
		}
	}

	/**
	 * A Greenwarden tends it at game time {@code now} (C-P61): a harvest it saw is counted, then one strain eased, at most
	 * once every {@value Garden#TEND_TICKS} ticks. Returns whether it eased one. Who may tend is VerdantBedBlock's business.
	 */
	public boolean tend(long now) {
		if (!Garden.enabled()) {
			return false;
		}
		settle();
		if (strain == 0 || tendedAt != Long.MIN_VALUE && now >= tendedAt && now - tendedAt < Garden.TEND_TICKS) {
			return false;
		}
		tendedAt = now;
		ease();
		return true;
	}

	/** Tests only (no game code calls it): sets the strain (clamped) and forgets a ripening not yet counted. */
	public void setStrain(int amount) {
		strain = Math.clamp(amount, 0, Habitat.MAX_STRAIN);
		ripened = false;
		changed();
	}

	private void ease() {
		strain--;
		changed();
	}

	private void changed() {
		setChanged();
		sync();
	}

	/** Its crop ripened and no longer stands ripe on it (gone, or below ripe). Reads one block, only while ripened. */
	private boolean harvestSeen() {
		if (!ripened || level == null) {
			return false;
		}
		BlockState above = level.getBlockState(worldPosition.above());
		return !(above.getBlock() instanceof OrganismCropBlock crop && crop.isMaxAge(above));
	}

	/** An organism crop grows on it: below ripe, and neither stalled nor dormant by its last note. */
	private boolean growing() {
		if (level == null) {
			return false;
		}
		BlockState above = level.getBlockState(worldPosition.above());
		return above.getBlock() instanceof OrganismCropBlock crop && !crop.isMaxAge(above)
				&& !growth.equals(Verdict.Growth.STALLED.id) && !growth.equals("dormant");
	}

	// ---------------------------------------------------------------- a mixed bed (Concordance Chunk C, C-08, C-P58)

	/**
	 * Whether another crop that speeds {@code organism} grows in the area round its plant, from the last sample (no new
	 * read). Server only.
	 */
	public boolean mixed(Organism organism) {
		return area != null && organism.neighbours().any() && Garden.catalog().mixed(organism, area.kinds());
	}

	/** A mixed bed's line for {@code organism} ("Mixed bed: +25%"), or null. Server only; it only reads. */
	public @Nullable Text mixedLine(Organism organism) {
		return mixed(organism) ? organism.neighbours().line() : null;
	}

	/** Its strain as the next reading counts it ("Recently harvested: strain n of 3"), or null when none. It only reads. */
	public @Nullable Text strainLine() {
		int shown = strainNow();
		return shown > 0 ? Text.of("ecology.strain", shown, Habitat.MAX_STRAIN) : null;
	}

	/**
	 * What the reports add after a reading of its plant: the mixed bed's line for {@code organism} (none for null), then its
	 * strain.
	 */
	public List<Text> notes(@Nullable Organism organism) {
		List<Text> lines = new ArrayList<>();
		Text mixed = organism == null ? null : mixedLine(organism);
		if (mixed != null) {
			lines.add(mixed);
		}
		Text strained = strainLine();
		if (strained != null) {
			lines.add(strained);
		}
		return lines;
	}

	// ---------------------------------------------------------------- a graft (Concordance Chunk C, C-09, C-P62 to C-P65)

	/** The graft it carries, by id ("" for none). */
	public String graft() {
		return graft;
	}

	/** On a client, the organism its graft is for, as the server sent it ("" for none): a crop names the graft only if it is that. */
	public String graftFor() {
		return graftFor;
	}

	/** Grafts it (OrganismCropBlock's gesture, and tests), or clears it with "". */
	public void setGraft(String graft) {
		if (!graft.equals(this.graft)) {
			this.graft = graft;
			setChanged();
			sync();
		}
	}

	/**
	 * A crop of {@code organism} grows here (OrganismCropBlock.grow, C-P64): a graft the rules know for another organism is
	 * cleared. One the rules no longer know is kept, read by nothing (the next graft replaces it). Reading never calls this.
	 */
	public void clearGraftUnlessFor(String organism) {
		Graft held = graft.isEmpty() ? null : Garden.catalog().graft(graft);
		if (held != null && !held.host().equals(organism)) {
			setGraft("");
		}
	}

	/**
	 * The niche {@code organism} grows by on this bed: its graft's when it carries one for that organism, otherwise the
	 * organism's own (EcologyCatalog.niche). Growth, both reports, the note, the Habitat Gauge and the Inspect page ask here;
	 * the Verdant Heart reads its own.
	 */
	public Niche niche(Organism organism) {
		return Garden.catalog().niche(organism, graft);
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
		// Concordance Chunk C (C-08): optional; a bed saved before them reads unstrained and unripened (the stamp stays 1).
		strain = Math.clamp(input.getIntOr(STRAIN, 0), 0, Habitat.MAX_STRAIN);
		ripened = input.getBooleanOr(RIPENED, false);
		// Concordance Chunk C (C-09): optional; a bed saved before grafts reads ungrafted. Only an update tag holds the host.
		graft = input.getStringOr(GRAFT, "");
		graftFor = input.getStringOr(GRAFT_HOST, "");
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
		if (strain > 0) {
			output.putInt(STRAIN, strain);
		}
		if (ripened) {
			output.putBoolean(RIPENED, true);
		}
		if (!graft.isEmpty()) {
			output.putString(GRAFT, graft);
		}
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	/**
	 * Clients get the nutrients, whether it is awake, the crop's last verdict (its reasons with a mixed bed's line), and
	 * (C-08) its strain and whether its crop has ripened since (so a client shows a harvest not yet counted as the server
	 * counts it), and (C-09) its graft with the organism it is for, only while it carries one; never the keeper.
	 */
	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		CompoundTag tag = new CompoundTag();
		tag.putInt("nutrients", nutrients);
		tag.putBoolean("awake", awake);
		tag.putString("growth", growth);
		tag.putString("reasons", String.join("\n", reasons));
		tag.putInt(STRAIN, strain);
		tag.putBoolean(RIPENED, ripened);
		if (!graft.isEmpty()) {
			Graft held = Garden.catalog().graft(graft);
			tag.putString(GRAFT, graft);
			tag.putString(GRAFT_HOST, held == null ? "" : held.host());
		}
		return tag;
	}
}
