package io.github.jimbozoomer.jugcraft.concordance.sky;

import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import io.github.jimbozoomer.jugcraft.concordance.Saved;
import io.github.jimbozoomer.jugcraft.concordance.celestial.Calendar;
import io.github.jimbozoomer.jugcraft.concordance.celestial.Pattern;
import io.github.jimbozoomer.jugcraft.concordance.resource.Overflow;
import io.github.jimbozoomer.jugcraft.concordance.resource.Ownership;
import io.github.jimbozoomer.jugcraft.concordance.resource.RateBudget;
import io.github.jimbozoomer.jugcraft.concordance.resource.Reservoir;
import io.github.jimbozoomer.jugcraft.concordance.resource.Transfers;
import io.github.jimbozoomer.jugcraft.concordance.rules.Evidence;
import io.github.jimbozoomer.jugcraft.party.JugcraftParties;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The Orrery Observatory (roadmap step 15). Aligned by a Starwatcher (placed by one, or touched by one's empty hand),
 * every {@value #GATHER_TICKS} ticks it looks at what is up by the world's clock; for each pattern up and visible from
 * it (the open sky above it, in the overworld, clear weather if the pattern needs it, in its season) it gathers the
 * pattern's Astral Resonance, up to {@value #CAPACITY}, once per occurrence for its keeper ({@link AstralClaims}): a
 * second observatory of the same keeper, a restart or a clock moved back or forward pays nothing more. It says why it
 * is idle (unaligned, waiting, clouded, no sky, elsewhere, full, already gathered, too soon). Its resonance leaves only
 * into an astrolabe, through the shared transfer rules, for its keeper's party.
 */
public class ObservatoryBlockEntity extends BlockEntity implements GeoBlockEntity {
	public static final int CAPACITY = 32;
	public static final int GATHER_TICKS = 100;
	private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.observatory.idle");
	private static final RawAnimation TRACKING = RawAnimation.begin().thenLoop("animation.observatory.tracking");

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	private long resonance;
	private @Nullable UUID keeper;
	private String status = "waiting";
	/** The pattern it last looked at (for its animation and Jade); empty when nothing is up. */
	private String tracking = "";

	public ObservatoryBlockEntity(BlockPos pos, BlockState state) {
		super(Sky.OBSERVATORY_ENTITY, pos, state);
	}

	public long resonance() {
		return resonance;
	}

	public String status() {
		return status;
	}

	public String tracking() {
		return tracking;
	}

	public @Nullable UUID keeper() {
		return keeper;
	}

	public void setKeeper(@Nullable UUID keeper) {
		this.keeper = keeper;
		setChanged();
	}

	/** Operators and tests: sets its resonance. */
	public void setResonance(long amount) {
		resonance = Math.clamp(amount, 0L, CAPACITY);
		setChanged();
		sync();
	}

	/** Whether a Starwatcher has aligned it (its keeper, whose claims its gathering counts against). */
	public boolean aligned() {
		return keeper != null;
	}

	/** Placed by a Starwatcher, it is aligned to them at once; otherwise it waits for one's touch. */
	public void placedBy(ServerPlayer player) {
		if (Sky.knows(player)) {
			setKeeper(player.getUUID());
		}
	}

	public Ownership ownership() {
		if (keeper == null) {
			return Ownership.NONE;
		}
		Set<UUID> members = new HashSet<>(JugcraftParties.partyMembers(keeper));
		members.remove(keeper);
		return new Ownership(keeper, members, false);
	}

	void serverTick(ServerLevel level) {
		if (Math.floorMod(level.getGameTime() + worldPosition.asLong(), GATHER_TICKS) == 0) {
			gather(level, Sky.time(level), level.getGameTime());
		}
	}

	/** One look at the real sky over it (its pulse); returns the status it leaves. */
	public String gather(ServerLevel level, long time, long gameTime) {
		return gather(level, time, gameTime, pattern -> Sky.obscured(level, worldPosition, pattern));
	}

	/** What it says when several patterns are up, the most useful first. */
	private static final List<String> PRIORITY = List.of("gathering", "full", "too_soon", "gathered", "clouded", "no_sky", "elsewhere", "waiting");

	/**
	 * One look at the sky at world time {@code time} and game time {@code gameTime}, with {@code sky} saying why a
	 * pattern is hidden from here (null when it can be seen). Its pulse passes the level's own; tests pass theirs, so they
	 * never move the world's clock or wait on the weather. Returns the status it leaves.
	 */
	public String gather(ServerLevel level, long time, long gameTime, Function<Pattern, @Nullable String> sky) {
		String next = "waiting";
		String looking = "";
		long before = resonance;
		if (!Sky.enabled()) {
			next = "disabled";
		} else if (keeper == null) {
			next = "unaligned";
		} else {
			AstralClaims claims = AstralClaims.of(level.getServer());
			for (Pattern pattern : Sky.up(time)) {
				String hidden = sky.apply(pattern);
				String outcome;
				if (hidden != null) {
					outcome = hidden;
				} else {
					if (looking.isEmpty()) {
						looking = pattern.id();
					}
					if (resonance + pattern.resonance() > CAPACITY) {
						outcome = "full";
					} else {
						outcome = switch (claims.claim(keeper, pattern.id(), Calendar.occurrence(pattern, time), gameTime, Calendar.minGap(pattern))) {
							case GRANTED -> {
								resonance += pattern.resonance();
								yield "gathering";
							}
							case ALREADY_CLAIMED -> "gathered";
							case TOO_SOON -> "too_soon";
						};
					}
				}
				if (PRIORITY.indexOf(outcome) < PRIORITY.indexOf(next)) {
					next = outcome;
				}
			}
		}
		if (!next.equals(status) || !looking.equals(tracking) || resonance != before) {
			status = next;
			tracking = looking;
			setChanged();
			sync();
		}
		return status;
	}

	/**
	 * An empty hand: a Starwatcher aligns it if no one has; then the forecast and its store; and while a pattern is up
	 * and visible, the player observes it.
	 */
	public void use(ServerPlayer player, ServerLevel level) {
		if (keeper == null && Sky.enabled() && Sky.knows(player)) {
			setKeeper(player.getUUID());
			player.sendSystemMessage(Component.translatable("message.jugcraft.concordance.sky.aligned", player.getDisplayName()));
		}
		long time = Sky.time(level);
		for (Component line : Sky.forecast(time, Sky.FORECAST_DAYS)) {
			player.sendSystemMessage(line);
		}
		player.sendSystemMessage(Component.translatable("message.jugcraft.concordance.sky.observatory", resonance, CAPACITY,
				Component.translatable("compose.jugcraft.sky.status." + status)));
		observe(player, level, time, level.getGameTime(), pattern -> Sky.obscured(level, worldPosition, pattern));
	}

	/**
	 * Records the patterns up at {@code time} and visible here ({@code sky} as in {@link #gather}) as observed by
	 * {@code player}: practice for the research, and what a master may later recall. Returns how many.
	 */
	public int observe(ServerPlayer player, ServerLevel level, long time, long gameTime, Function<Pattern, @Nullable String> sky) {
		if (!Sky.enabled()) {
			return 0;
		}
		int seen = 0;
		for (Pattern pattern : Sky.up(time)) {
			if (sky.apply(pattern) != null) {
				continue;
			}
			AstralClaims.of(level.getServer()).observe(player.getUUID(), pattern.id(), gameTime);
			ConcordanceProgress.record(player, new Evidence.Practiced(Sky.ACTIVITY, pattern.id()));
			player.sendSystemMessage(Component.translatable("message.jugcraft.concordance.sky.observed", Sky.patternName(pattern.id())));
			seen++;
		}
		return seen;
	}

	/** Moves resonance into a held astrolabe through the shared transfer rules (its keeper's party only). */
	public long fill(ServerPlayer player, ServerLevel level, ItemStack astrolabe) {
		int held = AstrolabeItem.charge(astrolabe);
		long want = AstrolabeItem.CAPACITY - held;
		if (want <= 0 || resonance == 0) {
			player.sendOverlayMessage(Component.translatable(want <= 0 ? "message.jugcraft.concordance.sky.astrolabe_full"
					: "message.jugcraft.concordance.sky.nothing_to_draw"));
			return 0;
		}
		Transfers.Transfer transfer = Transfers.move(new Transfers.Side(new Reservoir(Sky.ASTRAL, resonance, CAPACITY), ownership()),
				new Transfers.Side(new Reservoir(Sky.ASTRAL, held, AstrolabeItem.CAPACITY), Ownership.NONE), player.getUUID(), want,
				RateBudget.of(want, 1), ConcordanceProgress.now(level), Overflow.FILL);
		if (!transfer.outcome().moved()) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.sky.not_yours"));
			return 0;
		}
		resonance = transfer.source().amount();
		AstrolabeItem.setCharge(astrolabe, (int) transfer.target().amount());
		setChanged();
		sync();
		player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.sky.drawn", transfer.received(),
				AstrolabeItem.charge(astrolabe), AstrolabeItem.CAPACITY));
		return transfer.received();
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
		resonance = Math.clamp(input.getLongOr("resonance", 0L), 0L, CAPACITY);
		keeper = input.read("keeper", UUIDUtil.CODEC).orElse(null);
		status = input.getStringOr("status", "waiting");
		tracking = input.getStringOr("tracking", "");
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		Saved.stamp(output, 1);
		output.putLong("resonance", resonance);
		if (keeper != null) {
			output.store("keeper", UUIDUtil.CODEC, keeper);
		}
		output.putString("status", status);
		output.putString("tracking", tracking);
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	/** Clients get its resonance, status and the pattern it tracks; never its keeper. */
	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		CompoundTag tag = new CompoundTag();
		tag.putLong("resonance", resonance);
		tag.putString("status", status);
		tag.putString("tracking", tracking);
		return tag;
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<ObservatoryBlockEntity>("main", 10,
				test -> test.setAndContinue(test.animatable().tracking().isEmpty() ? IDLE : TRACKING)));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}

	/** The patterns this observatory could see now (for Jade): up, in season and visible from here. */
	public List<Pattern> visible(ServerLevel level) {
		return Sky.up(Sky.time(level)).stream().filter(pattern -> Sky.visible(level, worldPosition, pattern)).toList();
	}
}
