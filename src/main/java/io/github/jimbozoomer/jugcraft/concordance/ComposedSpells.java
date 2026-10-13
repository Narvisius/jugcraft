package io.github.jimbozoomer.jugcraft.concordance;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.compose.Component;
import io.github.jimbozoomer.jugcraft.concordance.compose.Compiler;
import io.github.jimbozoomer.jugcraft.concordance.compose.Instrument;
import io.github.jimbozoomer.jugcraft.concordance.compose.Plan;
import io.github.jimbozoomer.jugcraft.concordance.effect.Cause;
import io.github.jimbozoomer.jugcraft.concordance.effect.EffectKind;
import io.github.jimbozoomer.jugcraft.concordance.effect.EffectSpec;
import io.github.jimbozoomer.jugcraft.concordance.effect.Intent;
import io.github.jimbozoomer.jugcraft.concordance.effect.Ledger;
import io.github.jimbozoomer.jugcraft.concordance.rules.Knowledge;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import io.github.jimbozoomer.jugcraft.party.JugcraftParties;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.api.spell.event.SpellHandlers;
import net.spell_engine.internals.SpellExecution;
import net.spell_power.api.SpellPower;
import org.jspecify.annotations.Nullable;

/**
 * Inscribed spells: compositions written on an instrument (roadmap step 8) and cast through Spell Engine's carrier
 * spell {@code jugcraft:composed}. The division of authority is Kindle's ({@link ConcordanceSpells}):
 * <ul>
 * <li>Spell Engine owns the cast (its timeline, gestures, HUD and the brief base cooldown).</li>
 * <li>The {@code CUSTOM} impact ({@link #impact}, server only) compiles the inscription again, from its text, against
 * the current rules and the caster's current research and instrument, so a stale or forged inscription is refused. It
 * then runs the plan: the delivery is traced on the server from the caster's own view (a client's aim is never
 * trusted), and every effect goes through {@link ConcordanceEffects} under one {@link Ledger} built from the plan's
 * limits, shared by its pulses and its branch. A plan therefore cannot reach more targets, do more work or take more
 * branches than it compiled to.</li>
 * <li>If nothing took effect the impact fails: Spell Engine applies no cooldown, no Focus is taken and nothing
 * lingers. Otherwise the Focus the plan costs is taken once and the plan's own cooldown set, when Spell Engine consumes
 * the cast's cost ({@link ConcordanceSpells#owe}). A spell that has started is not refunded if its later pulses find
 * nothing.</li>
 * </ul>
 * Authored invocations ({@link Invocations}) run their compiled plans through the same {@link #perform}.
 * Pulses after the first are kept here (at most {@value #MAX_LINGERING} at once on the server, {@value #MAX_PER_PLAYER}
 * per player) and end early if the caster leaves, dies, changes dimension or the place is no longer loaded; they never
 * load chunks and are not saved.
 */
public final class ComposedSpells {
	public static final String SPELL = "jugcraft:composed";
	public static final int MAX_LINGERING = 64;
	public static final int MAX_PER_PLAYER = 2;
	private static final SpellHandlers.ImpactResult FAILED = new SpellHandlers.ImpactResult(false, false);
	private static final List<Lingering> LINGERING = new ArrayList<>();

	private ComposedSpells() {
	}

	/**
	 * What running a plan did: whether anything took effect, and whether something was refused because the caster may
	 * not change a block there (so the caster can be told why).
	 */
	public record Outcome(boolean applied, boolean notAllowed) {
	}

	/**
	 * Where a delivery landed: the point, the creature it struck (or null), the open block before the surface it met
	 * (where light goes) and the block it met (what is used, gathered or put out), and the point it came from.
	 */
	public record Impact(Vec3 point, @Nullable UUID struck, BlockPos open, BlockPos block, Vec3 origin) {
	}

	private record Lingering(UUID caster, ResourceKey<Level> dimension, Plan.Node node, Impact impact, Cause cause, Ledger ledger,
			int pulse, long due) {
	}

	static void register() {
		SpellHandlers.registerCustomImpact(Jugcraft.id("composed"), ComposedSpells::impact);
		ServerTickEvents.END_SERVER_TICK.register(ComposedSpells::tick);
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> forget(handler.getPlayer().getUUID()));
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> LINGERING.clear());
	}

	public static @Nullable Inscription inscription(ItemStack stack) {
		return stack.get(JugcraftConcordance.INSCRIPTION);
	}

	/** The instrument definition for what the player holds in their main hand, or null if it cannot hold spells. */
	public static @Nullable Instrument instrument(Player player) {
		ItemStack held = player.getMainHandItem();
		if (!held.is(JugcraftConcordance.INSTRUMENTS)) {
			return null;
		}
		return ConcordanceData.rules().catalog().instrumentFor(BuiltInRegistries.ITEM.getKey(held.getItem()).toString());
	}

	/** Compiles {@code text} for this player and the instrument they hold (server rules and research). */
	public static Compiler.Compilation compile(ServerPlayer player, Instrument instrument, String text) {
		Knowledge knowledge = ConcordanceProgress.knowledge(player);
		return Compiler.compile(text, ConcordanceData.rules().catalog(), knowledge::state, instrument);
	}

	/** Spell Engine's {@code CUSTOM} impact {@code jugcraft:composed}. */
	static SpellHandlers.ImpactResult impact(Holder<Spell> spell, SpellPower.Result power, LivingEntity caster, @Nullable Entity target,
			SpellExecution.ImpactContext context) {
		try {
			if (!(caster instanceof ServerPlayer player) || ConcordanceSpells.refusal(player, spell) != null) {
				return FAILED;
			}
			Inscription inscription = inscription(player.getMainHandItem());
			Instrument instrument = instrument(player);
			if (inscription == null || instrument == null) {
				return FAILED;
			}
			Compiler.Compilation compiled = compile(player, instrument, inscription.text());
			if (compiled.plan() == null) {
				player.sendOverlayMessage(compiled.problems().isEmpty()
						? net.minecraft.network.chat.Component.translatable("compose.jugcraft.invalid", inscription.text())
						: ComposeText.show(compiled.problems().getFirst()));
				return FAILED;
			}
			Plan plan = compiled.plan();
			if (ConcordanceProgress.currentFocus(player) < plan.focus()) {
				return FAILED;
			}
			Cause cause = Cause.of(player.getUUID(), Cause.Origin.SPELL, SPELL, ConcordanceEffects.nextSerial());
			Outcome outcome = perform(player.level(), player, plan, cause, new Ledger(plan.limits()));
			if (outcome.applied()) {
				ConcordanceSpells.owe(player, SPELL, plan.focus(), plan.cooldown(), true);
			} else if (outcome.notAllowed()) {
				player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("message.jugcraft.concordance.invocation.not_allowed"));
			}
			return new SpellHandlers.ImpactResult(outcome.applied(), false);
		} catch (RuntimeException problem) {
			Jugcraft.LOGGER.error("Arcane Concordance: an inscribed spell failed", problem);
			return FAILED;
		}
	}

	/**
	 * Runs a compiled plan for {@code caster} now: the first pulse of the spell and, if that took effect, its branch;
	 * later pulses are scheduled. Returns whether anything took effect (nothing is scheduled otherwise). Game tests call
	 * this directly to check the limits.
	 */
	public static boolean cast(ServerLevel level, ServerPlayer caster, Plan plan, Cause cause) {
		return cast(level, caster, plan, cause, new Ledger(plan.limits()));
	}

	/** As {@link #cast(ServerLevel, ServerPlayer, Plan, Cause)}, with the caller's ledger (to inspect afterwards). */
	public static boolean cast(ServerLevel level, ServerPlayer caster, Plan plan, Cause cause, Ledger ledger) {
		return perform(level, caster, plan, cause, ledger).applied();
	}

	/** As {@link #cast(ServerLevel, ServerPlayer, Plan, Cause, Ledger)}, reporting why nothing took effect. */
	public static Outcome perform(ServerLevel level, ServerPlayer caster, Plan plan, Cause cause, Ledger ledger) {
		Set<ConcordanceEffects.Result> seen = EnumSet.noneOf(ConcordanceEffects.Result.class);
		boolean applied = run(level, caster, plan.root(), null, cause, ledger, seen);
		return new Outcome(applied, seen.contains(ConcordanceEffects.Result.NOT_ALLOWED));
	}

	private static boolean run(ServerLevel level, ServerPlayer caster, Plan.Node node, @Nullable Impact parent, Cause cause, Ledger ledger,
			Set<ConcordanceEffects.Result> seen) {
		Impact impact = deliver(level, caster, node, parent);
		boolean applied = pulse(level, caster, node, impact, cause, ledger, 0, seen);
		if (!applied) {
			return false;
		}
		if (node.pulses() > 1) {
			linger(caster, level, node, impact, cause, ledger);
		}
		Cause triggered = cause.triggered();
		if (node.then() != null && triggered != null && ledger.branch()) {
			run(level, caster, node.then(), impact, triggered, ledger, seen);
		}
		return true;
	}

	/** Where the node's delivery lands, traced on the server. */
	public static Impact deliver(ServerLevel level, ServerPlayer caster, Plan.Node node, @Nullable Impact parent) {
		if (node.form() == Component.Form.HERE) {
			if (parent != null) {
				return parent;
			}
			BlockPos feet = caster.blockPosition();
			return new Impact(caster.position(), caster.getUUID(), feet.above(), feet, caster.position());
		}
		Vec3 eye = caster.getEyePosition();
		Vec3 reach = caster.getViewVector(1.0F).scale(node.range());
		Vec3 end = eye.add(reach);
		BlockHitResult block = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, caster));
		Vec3 stop = block.getType() == HitResult.Type.BLOCK ? block.getLocation() : end;
		AABB search = caster.getBoundingBox().expandTowards(reach).inflate(1.0);
		EntityHitResult creature = ProjectileUtil.getEntityHitResult(caster, eye, stop, search,
				entity -> entity instanceof LivingEntity && entity.isAlive() && !entity.isSpectator() && entity.isPickable(),
				eye.distanceToSqr(stop));
		if (creature != null) {
			BlockPos at = creature.getEntity().blockPosition();
			return new Impact(creature.getLocation(), creature.getEntity().getUUID(), at, at, eye);
		}
		if (block.getType() == HitResult.Type.BLOCK) {
			return new Impact(block.getLocation(), null, block.getBlockPos().relative(block.getDirection()), block.getBlockPos(), eye);
		}
		BlockPos at = BlockPos.containing(end);
		return new Impact(end, null, at, at, eye);
	}

	/**
	 * One pulse of a node: each operation on what its selection chooses. Returns whether anything took effect, and adds
	 * each application's result to {@code seen}.
	 */
	private static boolean pulse(ServerLevel level, ServerPlayer caster, Plan.Node node, Impact impact, Cause cause, Ledger ledger, int pulse,
			Set<ConcordanceEffects.Result> seen) {
		boolean applied = false;
		for (Plan.Step step : node.steps()) {
			ConcordanceEffects.Context context = new ConcordanceEffects.Context(level, cause, caster, ledger,
					node.depth() + "/" + pulse + "/" + step.index(), impact.origin());
			if (step.effect().kind().on == EffectKind.On.CREATURE) {
				for (LivingEntity target : creatures(level, caster, node, impact, step.effect().intent())) {
					ConcordanceEffects.Result result = ConcordanceEffects.apply(context, step.effect(), target);
					seen.add(result);
					applied |= result.applied();
				}
			} else {
				for (BlockPos pos : blocks(level, node, impact, step.effect())) {
					ConcordanceEffects.Result result = ConcordanceEffects.apply(context, step.effect(), pos);
					seen.add(result);
					applied |= result.applied();
				}
			}
		}
		return applied;
	}

	/**
	 * The creatures a node's selection chooses, nearest first (ties by UUID): never the caster for a harmful effect,
	 * and for allies only the caster and the players in the caster's party.
	 */
	static List<LivingEntity> creatures(ServerLevel level, ServerPlayer caster, Plan.Node node, Impact impact, Intent intent) {
		if (node.pick() == Component.Pick.STRUCK) {
			Entity struck = impact.struck() == null ? null : level.getEntity(impact.struck());
			return struck instanceof LivingEntity living && living.isAlive() ? List.of(living) : List.of();
		}
		double radius = node.radius();
		List<LivingEntity> found = new ArrayList<>();
		if (node.pick() == Component.Pick.ALLIES) {
			// Players only (a bounded list): the caster and their party, never a stranger or a creature.
			for (ServerPlayer other : level.players()) {
				if (other.isAlive() && !other.isSpectator() && other.distanceToSqr(impact.point()) <= radius * radius
						&& (other == caster || JugcraftParties.sameParty(caster.getUUID(), other.getUUID()))) {
					found.add(other);
				}
			}
		} else if (node.pick() == Component.Pick.CREATURES) {
			found.addAll(level.getEntitiesOfClass(LivingEntity.class, new AABB(impact.point(), impact.point()).inflate(radius),
					entity -> entity.isAlive() && !entity.isSpectator() && entity.distanceToSqr(impact.point()) <= radius * radius
							&& (intent == Intent.HELPFUL || entity != caster)));
		} else {
			return List.of();
		}
		found.sort(Comparator.<LivingEntity>comparingDouble(entity -> entity.distanceToSqr(impact.point())).thenComparing(Entity::getUUID));
		return found.size() > node.targets() ? found.subList(0, node.targets()) : found;
	}

	/**
	 * The blocks a node's selection chooses for an operation: the one it struck, or the nearest ones (ties by
	 * position) the operation can act on, lights at least two blocks apart.
	 */
	static List<BlockPos> blocks(ServerLevel level, Plan.Node node, Impact impact, EffectSpec effect) {
		EffectKind kind = effect.kind();
		if (node.pick() == Component.Pick.STRUCK) {
			return List.of(kind == EffectKind.ILLUMINATION ? impact.open() : impact.block());
		}
		if (node.pick() != Component.Pick.BLOCKS) {
			return List.of();
		}
		BlockPos center = BlockPos.containing(impact.point());
		int r = node.radius();
		List<BlockPos> candidates = new ArrayList<>();
		for (BlockPos pos : BlockPos.betweenClosed(center.offset(-r, -r, -r), center.offset(r, r, r))) {
			if (pos.distSqr(center) <= (double) r * r && level.isLoaded(pos) && ConcordanceEffects.accepts(level, effect, pos)) {
				candidates.add(pos.immutable());
			}
		}
		candidates.sort(Comparator.<BlockPos>comparingDouble(pos -> pos.distSqr(center)).thenComparingInt(BlockPos::getY)
				.thenComparingInt(BlockPos::getX).thenComparingInt(BlockPos::getZ));
		List<BlockPos> chosen = new ArrayList<>();
		for (BlockPos pos : candidates) {
			if (chosen.size() >= node.targets()) {
				break;
			}
			if (kind == EffectKind.ILLUMINATION && chosen.stream().anyMatch(other -> other.closerThan(pos, 2.0))) {
				continue;
			}
			chosen.add(pos);
		}
		return chosen;
	}

	private static void linger(ServerPlayer caster, ServerLevel level, Plan.Node node, Impact impact, Cause cause, Ledger ledger) {
		long mine = LINGERING.stream().filter(entry -> entry.caster().equals(caster.getUUID())).count();
		if (LINGERING.size() >= MAX_LINGERING || mine >= MAX_PER_PLAYER) {
			return;
		}
		LINGERING.add(new Lingering(caster.getUUID(), level.dimension(), node, impact, cause, ledger, 1, level.getGameTime() + node.interval()));
	}

	private static void tick(MinecraftServer server) {
		if (LINGERING.isEmpty()) {
			return;
		}
		List<Lingering> next = new ArrayList<>();
		for (Iterator<Lingering> it = LINGERING.iterator(); it.hasNext();) {
			Lingering entry = it.next();
			ServerLevel level = server.getLevel(entry.dimension());
			if (level == null || level.getGameTime() < entry.due()) {
				continue;
			}
			it.remove();
			ServerPlayer caster = server.getPlayerList().getPlayer(entry.caster());
			if (caster == null || !caster.isAlive() || caster.level() != level || !JugcraftConfig.isFeatureEnabled(JugcraftConcordance.FEATURE)
					|| !level.isLoaded(BlockPos.containing(entry.impact().point()))) {
				continue;
			}
			try {
				pulse(level, caster, entry.node(), entry.impact(), entry.cause(), entry.ledger(), entry.pulse(),
						EnumSet.noneOf(ConcordanceEffects.Result.class));
			} catch (RuntimeException problem) {
				Jugcraft.LOGGER.error("Arcane Concordance: a pulse of an inscribed spell failed", problem);
				continue;
			}
			if (entry.pulse() + 1 < entry.node().pulses()) {
				next.add(new Lingering(entry.caster(), entry.dimension(), entry.node(), entry.impact(), entry.cause(), entry.ledger(),
						entry.pulse() + 1, level.getGameTime() + entry.node().interval()));
			}
		}
		LINGERING.addAll(next);
	}

	/** Pulses waiting for {@code player} (game tests and diagnostics). */
	public static int lingering(UUID player) {
		return (int) LINGERING.stream().filter(entry -> entry.caster().equals(player)).count();
	}

	private static void forget(UUID player) {
		LINGERING.removeIf(entry -> entry.caster().equals(player));
	}
}
