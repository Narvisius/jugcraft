package io.github.jimbozoomer.jugcraft.concordance;

import io.github.jimbozoomer.jugcraft.concordance.alchemy.Band;
import io.github.jimbozoomer.jugcraft.concordance.alchemy.Heat;
import io.github.jimbozoomer.jugcraft.concordance.compose.Text;
import io.github.jimbozoomer.jugcraft.concordance.diagnostic.Code;
import io.github.jimbozoomer.jugcraft.concordance.diagnostic.Diagnostic;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import io.github.jimbozoomer.jugcraft.energy.SimpleEnergyStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * A Crucible Stand (Concordance Chunk C, C-05; C-D3, C-P12, C-P43 to C-P46): JE turned into heat for the Alembic Crucible
 * standing on it, and nothing else.
 * <ul>
 * <li><b>JE in</b> through Jugcraft's one energy interface on every face but its seat ({@link #energy}: at most
 * {@value #INTAKE} JE a tick, {@value #CAPACITY} held, none ever given back). With the Concordance switched off it takes
 * none.</li>
 * <li><b>Heat out</b>, only to the crucible directly above (one block entity read, no scan). Each tick it works out the band
 * that crucible needs ({@link Heat#standBand}): with a formula, its next stir's, but only while the crucible waits for
 * nothing or for heat alone; with none, its own setting. It pays that band's rate ({@value #JE_WARM}, {@value #JE_HOT} or
 * {@value #JE_SEARING} JE a tick) only on a tick the crucible is no hotter than the band's target, and lights; the
 * crucible then heads for the target ({@link CrucibleBlockEntity#below}). Otherwise it pays nothing and goes dark, and the
 * crucible cools by the usual rule. Preserve's hold wins. Fed less than its rate (JE came in within {@value #FED_TICKS}
 * ticks), a tick it cannot pay holds the crucible where it is while the crucible is below the band (and, following a
 * formula, at the band's lowest degree), so a trickle still brings it to the band for the same JE a degree; inside the
 * band such a tick cools as before. Short of JE, it goes dark only after {@value #DARK_TICKS} such ticks in a row.</li>
 * <li><b>Setting</b>: with no formula above, a plain empty-hand use by someone who may change the block cycles off, warm,
 * hot, searing, at most once every {@value #SET_TICKS} ticks a player. There is no client slider; the server chooses.</li>
 * </ul>
 * No wear and no upkeep (D-004). Broken, it drops itself; the JE it held is lost, never carried. Keep the numbers equal to
 * tools/concordance_alchemy.py (STAND_*) and tools/concordance_economy.py (STAND_JE).
 */
public class CrucibleStandBlockEntity extends BlockEntity {
	public static final long JE_WARM = 8;
	public static final long JE_HOT = 16;
	public static final long JE_SEARING = 32;
	public static final long CAPACITY = 640;
	public static final long INTAKE = 64;
	public static final int SET_TICKS = 10;
	/** C-P43 (the unit review): how recently JE must have come in for a tick it cannot pay to hold the crucible. */
	public static final int FED_TICKS = 20;
	/** C-P43 (the unit review): ticks in a row short of JE before it goes dark, so a trickle does not flicker its light. */
	public static final int DARK_TICKS = 10;

	/** Electricity waiting to be paid as heat. Never extracted; switched off, it takes nothing. */
	public final SimpleEnergyStorage energy = new SimpleEnergyStorage(CAPACITY, INTAKE, 0, this::setChanged) {
		@Override
		public long insert(long maxAmount, TransactionContext transaction) {
			return JugcraftConfig.isFeatureEnabled(JugcraftConcordance.FEATURE) ? super.insert(maxAmount, transaction) : 0L;
		}
	};
	/** Its own heat when no formula decides it: COLD is off. Saved. */
	private Band setting = Band.COLD;
	/**
	 * The target it paid for on its last tick, the crucible's own temperature on a tick it held it, or AMBIENT. Not saved:
	 * worked out again at its next tick.
	 */
	private int given = Heat.AMBIENT;
	/** The JE in its store after its last tick, the game time JE last came in, and its ticks in a row short of JE. Not saved. */
	private long left;
	private long fedAt = Long.MIN_VALUE;
	private int starvedTicks;

	/**
	 * What it does for the crucible on it now (C-05): the band it aims at (null: none); whether a formula decides it;
	 * whether it heats (would pay on this tick); and, when it gives no heat for a reason, why: the crucible's own reason or
	 * Preserve's hold ({@code waits}), nothing on it, or too little JE; and the crucible's temperature (AMBIENT with none),
	 * read in the same block entity read. Read only.
	 */
	public record Plan(@Nullable Band band, boolean following, boolean heats, @Nullable Diagnostic reason, boolean waits, int temperature) {
		static final Plan DISABLED = new Plan(null, false, false, null, false, Heat.AMBIENT);
	}

	public CrucibleStandBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftConcordance.CRUCIBLE_STAND_ENTITY, pos, state);
	}

	public Band setting() {
		return setting;
	}

	/** What it gave on its last tick (what the crucible above heads for): its target paid for, the crucible's temperature held, or AMBIENT. */
	public int given() {
		return given;
	}

	/** The JE a tick it pays to hold a crucible at {@code band}'s target (C-D3's starting rates); none for cold, which is off. */
	public static long rate(Band band) {
		return switch (band) {
			case COLD -> 0L;
			case WARM -> JE_WARM;
			case HOT -> JE_HOT;
			case SEARING -> JE_SEARING;
		};
	}

	/** What it would do now (heat() pays on it; the status line and Jade show it). One block entity read, no scan. */
	public Plan plan(ServerLevel level) {
		if (!JugcraftConfig.isFeatureEnabled(JugcraftConcordance.FEATURE)) {
			return Plan.DISABLED;
		}
		if (!(level.getBlockEntity(worldPosition.above()) instanceof CrucibleBlockEntity crucible)) {
			return new Plan(setting == Band.COLD ? null : setting, false, false, Diagnostic.of(Code.NO_CRUCIBLE), false, Heat.AMBIENT);
		}
		int temperature = crucible.temperature();
		int held = crucible.held(ConcordanceProgress.now(level));
		Diagnostic waiting = crucible.waiting();
		boolean following = crucible.program() != null || !crucible.unreadableProgram().isEmpty();
		Band band = Heat.standBand(crucible.program(), crucible.step(), waiting, held > 0, setting);
		if (held > 0) {
			return new Plan(band, following, false, Diagnostic.held(crucible.band().id, (held + 19) / 20), true, temperature);
		}
		if (!Heat.waitsForHeat(waiting)) {
			return new Plan(band, following, false, waiting, true, temperature);
		}
		if (band == null || crucible.temperature() > Heat.standTarget(band)) {
			return new Plan(band, following, false, null, false, temperature);
		}
		long rate = rate(band);
		if (energy.getAmount() < rate) {
			return new Plan(band, following, false, Diagnostic.of(Code.UNAVAILABLE_POWER, Text.of("alchemy.stand.it"),
					Text.of("alchemy.stand.je", (int) energy.getAmount()), Text.of("alchemy.stand.je_tick", (int) rate)), false, temperature);
		}
		return new Plan(band, following, true, null, false, temperature);
	}

	/**
	 * One tick of the stand (its ticker, or a test's at a known moment): pays its plan's rate from its own store if the
	 * plan heats, and lights; otherwise pays nothing and goes dark (short of JE, only after DARK_TICKS such ticks in a row).
	 * Short of JE but fed within FED_TICKS, it holds the crucible where it is while {@link #holds} says so (C-P43, the unit
	 * review). Returns whether it paid. Its light changes only how it looks (clients read it): no neighbour, cable-shape or
	 * light update.
	 */
	public boolean heat(ServerLevel level) {
		Plan plan = plan(level);
		long now = level.getGameTime();
		if (energy.getAmount() > left) {
			fedAt = now;
		}
		boolean paid = false;
		if (plan.heats()) {
			energy.setAmount(energy.getAmount() - rate(plan.band()));
			setChanged();
			paid = true;
		}
		boolean starving = !paid && plan.reason() != null && plan.reason().code() == Code.UNAVAILABLE_POWER;
		starvedTicks = starving ? starvedTicks + 1 : 0;
		given = paid ? Heat.standTarget(plan.band()) : starving && fedAt >= now - FED_TICKS && holds(plan) ? plan.temperature() : Heat.AMBIENT;
		left = energy.getAmount();
		BlockState state = getBlockState();
		boolean lit = paid || starving && starvedTicks < DARK_TICKS && state.getValue(CrucibleStandBlock.LIT);
		if (state.getValue(CrucibleStandBlock.LIT) != lit) {
			level.setBlock(worldPosition, state.setValue(CrucibleStandBlock.LIT, lit), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
		}
		return paid;
	}

	/**
	 * On a tick it is short of JE but fed lately (C-P43, the unit review): whether it holds the crucible where it is. Below
	 * the band, always, so a trickle brings it there degree by degree for the same JE a degree. Following a formula, also at
	 * the band's lowest degree, so a pulse finds it in the band whatever the pattern of the ticks it pays (a stir waits at
	 * most one pulse more). Otherwise, inside the band, it cools as before, and holding the band costs C-D3's rates.
	 */
	static boolean holds(Plan plan) {
		Band band = plan.band();
		int temperature = plan.temperature();
		return band != null && temperature < Heat.standTarget(band)
				&& (Band.of(temperature) != band || plan.following() && Band.of(temperature - Heat.RATE) != band);
	}

	/**
	 * Empty-handed (C-05, C-P46): a plain use with no formula above, by someone who may change the block, sets the next
	 * heat (off, warm, hot, searing), at most once every SET_TICKS ticks a player; otherwise, or sneaking, it says how it
	 * stands. Server only: a client sends nothing but the use.
	 */
	public void useEmpty(ServerPlayer player, boolean sneaking) {
		ServerLevel level = player.level();
		if (!JugcraftConfig.isFeatureEnabled(JugcraftConcordance.FEATURE)) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.disabled"));
			return;
		}
		Plan plan = plan(level);
		if (!sneaking && !plan.following()) {
			// C-05 (C-P46): only someone who could change the block by hand sets its heat; reading stays open.
			if (!Authority.mayChange(level, player, worldPosition)) {
				player.sendOverlayMessage(ComposeText.show(Diagnostic.of(Code.MISSING_PERMISSION).text()));
				return;
			}
			if (!RateGate.allow(player, "crucible_stand", SET_TICKS)) {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.stand.settling"));
				return;
			}
			setting = next(setting);
			setChanged();
			player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.stand.set", heatWords(setting == Band.COLD ? null : setting)));
			return;
		}
		player.sendOverlayMessage(statusLine(level));
	}

	/** The next setting in turn: off, warm, hot, searing, then off again. */
	static Band next(Band setting) {
		Band[] bands = Band.values();
		return bands[(setting.ordinal() + 1) % bands.length];
	}

	/** The empty-hand status line (one overlay): what decides its heat and at what, its JE, then what it pays or why not. */
	public Component statusLine(ServerLevel level) {
		Plan plan = plan(level);
		Component line = Component.translatable("message.jugcraft.concordance.stand.status",
				Component.translatable(plan.following() ? "message.jugcraft.concordance.stand.following" : "message.jugcraft.concordance.stand.setting",
						heatWords(plan.band())), energy.getAmount(), CAPACITY);
		if (plan.heats()) {
			return Component.translatable("message.jugcraft.concordance.stand.status_more", line,
					Component.translatable("message.jugcraft.concordance.stand.heating", rate(plan.band())));
		}
		return plan.reason() == null ? line
				: Component.translatable("message.jugcraft.concordance.stand.status_more", line, ComposeText.show(plan.reason().text()));
	}

	private static Component heatWords(@Nullable Band band) {
		return band == null ? Component.translatable("message.jugcraft.concordance.stand.off")
				: Component.translatable("message.jugcraft.concordance.stand.heat", ComposeText.name(new Text.Ref("band", band.id)), Heat.standTarget(band));
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		energy.setAmount(input.getLongOr("energy", 0L));
		Band saved = Band.fromId(input.getStringOr("setting", Band.COLD.id));
		setting = saved == null ? Band.COLD : saved;
		given = Heat.AMBIENT;
		left = energy.getAmount();
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		Saved.stamp(output, 1);
		output.putLong("energy", energy.getAmount());
		output.putString("setting", setting.id);
	}
}
