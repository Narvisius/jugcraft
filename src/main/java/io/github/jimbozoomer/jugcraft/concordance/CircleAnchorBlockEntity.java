package io.github.jimbozoomer.jugcraft.concordance;

import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import com.mojang.serialization.Codec;
import io.github.jimbozoomer.jugcraft.concordance.effect.Cause;
import io.github.jimbozoomer.jugcraft.concordance.effect.EffectKind;
import io.github.jimbozoomer.jugcraft.concordance.effect.EffectSpec;
import io.github.jimbozoomer.jugcraft.concordance.effect.Intent;
import io.github.jimbozoomer.jugcraft.concordance.effect.Ledger;
import io.github.jimbozoomer.jugcraft.concordance.ritual.Offerings;
import io.github.jimbozoomer.jugcraft.concordance.ritual.RitualDefinition;
import io.github.jimbozoomer.jugcraft.concordance.ritual.RitualMachine;
import io.github.jimbozoomer.jugcraft.concordance.ritual.RitualRun;
import io.github.jimbozoomer.jugcraft.concordance.ritual.StructurePattern;
import io.github.jimbozoomer.jugcraft.concordance.ritual.StructureValidator;
import io.github.jimbozoomer.jugcraft.concordance.sign.Sign;
import io.github.jimbozoomer.jugcraft.concordance.sign.Signs;
import io.github.jimbozoomer.jugcraft.concordance.rules.ConcordanceRules;
import io.github.jimbozoomer.jugcraft.concordance.rules.Evidence;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchEngine;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Prediction;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A Circle Anchor (roadmap step 12): {@value #SLOTS} offering slots, an output slot, and the ritual state of the
 * circle built round it ({@link RitualRun}). This carries out what {@link RitualMachine} decides; every change to the
 * world a ritual makes happens in {@link #apply}, in the order the run gives, in one server tick:
 * <ul>
 * <li><b>Offerings</b> are placed by using items on the anchor and taken back by sneaking with an empty hand. When a
 * ritual starts they are reserved: the slots are locked (no hopper or pipe can reach them; the anchor exposes no
 * container) until it completes or breaks off. They are consumed only at completion, together with making the
 * result.</li>
 * <li><b>Each step</b> ({@link RitualMachine#STEP_TICKS} ticks) checks the whole structure again (at most
 * {@link StructurePattern#MAX_PARTS} positions), every participant and the conditions, then draws Ley Charge from every
 * channel. Between steps nothing is scanned. An idle anchor answers status queries (the player, Jade, the command) from
 * a cached report, reused for {@value #CACHE_TICKS} ticks unless a change to a circle part nearby voids it
 * ({@link Rituals#changed}).</li>
 * <li><b>Interruptions</b>: a broken or unpowered circle, a participant gone, too much light, part of it unloaded, the
 * leader calling it off, the anchor broken, the Concordance switched off or the ritual no longer defined. A ritual
 * saved mid-run (its chunk unloaded, the server stopped) lapses when it loads, and one whose anchor missed a whole step
 * lapses too: it never catches up. The offerings are released and nothing is made.</li>
 * </ul>
 * Everything a ritual holds is saved with this block entity, in the same chunk, so a save or a crash can never keep the
 * result and the offerings both. Clients receive the phase, the participants and the linked channels (not the items),
 * which is all the anchor's animation, the participants' gesture and the channel beams show.
 */
public class CircleAnchorBlockEntity extends BlockEntity implements GeoBlockEntity {
	public static final int SLOTS = 6;
	public static final int CACHE_TICKS = 100;
	public static final String ANCHOR = "jugcraft:circle_anchor";
	/** The practice a completed ritual records ({@link Evidence.Practiced}): one per chunk counts for mastery. */
	public static final String ACTIVITY = "jugcraft:ritual";
	private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.circle_anchor.idle");
	private static final RawAnimation CHANNEL = RawAnimation.begin().thenLoop("animation.circle_anchor.channel");
	private static final Map<String, TagKey<Block>> BLOCK_TAGS = new ConcurrentHashMap<>();

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	private NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
	private ItemStack output = ItemStack.EMPTY;
	private RitualRun run = RitualRun.IDLE;
	/** Loaded from a save while a ritual held its offerings: it lapses on its next tick. */
	private boolean resumed;
	private StructureValidator.@Nullable Report report;
	/** Who last broke a block in reach, and when (not saved): a containment failing soon after is their doing (step 28). */
	private @Nullable UUID disturber;
	private long disturbedAt = Long.MIN_VALUE / 2;
	private long reportAt;
	private boolean reportStale = true;
	/** The channels linked at the last check (bit per channel), and the running ritual's steps: what clients draw. */
	private int linked;
	private int steps;
	/** Where the channels of the last checked circle are, {@code x,y,z;...} from the anchor (clients draw beams to them). */
	private String channels = "";

	public CircleAnchorBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftConcordance.ANCHOR_ENTITY, pos, state);
	}

	public RitualRun run() {
		return run;
	}

	public RitualMachine.Phase phase() {
		return run.phase();
	}

	public int linked() {
		return linked;
	}

	public int steps() {
		return steps;
	}

	/** The offsets of the channels, in the order of the linked mask's bits (empty until the circle is checked). */
	public List<StructurePattern.Offset> channelOffsets() {
		List<StructurePattern.Offset> out = new ArrayList<>();
		for (String each : channels.split(";")) {
			String[] xyz = each.split(",");
			if (xyz.length == 3) {
				try {
					out.add(new StructurePattern.Offset(Integer.parseInt(xyz[0]), Integer.parseInt(xyz[1]), Integer.parseInt(xyz[2])));
				} catch (NumberFormatException ignored) {
					return List.of();
				}
			}
		}
		return out;
	}

	public ItemStack output() {
		return output;
	}

	public List<ItemStack> offerings() {
		return List.copyOf(items);
	}

	/** Something near changed a part of a circle: the next status query checks again. */
	void invalidate() {
		reportStale = true;
	}

	// ---------------------------------------------------------------- players

	/** Places what the player holds into a slot (all of it that fits one). Not while a ritual holds the slots. */
	public void offer(ServerPlayer player, ItemStack held) {
		if (run.phase().reserving()) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.circle.locked"));
			return;
		}
		if (run.phase() == RitualMachine.Phase.COMPLETE) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.circle.take_first"));
			return;
		}
		int slot = -1;
		for (int i = 0; i < SLOTS && slot < 0; i++) {
			ItemStack in = items.get(i);
			if (!in.isEmpty() && ItemStack.isSameItemSameComponents(in, held) && in.getCount() < in.getMaxStackSize()) {
				slot = i;
			}
		}
		for (int i = 0; i < SLOTS && slot < 0; i++) {
			if (items.get(i).isEmpty()) {
				slot = i;
			}
		}
		if (slot < 0) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.circle.full"));
			return;
		}
		ItemStack in = items.get(slot);
		int moved = Math.min(held.getCount(), in.isEmpty() ? held.getMaxStackSize() : in.getMaxStackSize() - in.getCount());
		Component name = held.getHoverName();
		if (in.isEmpty()) {
			items.set(slot, held.copyWithCount(moved));
		} else {
			in.grow(moved);
		}
		held.shrink(moved);
		setChanged();
		player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.circle.offered", name));
	}

	/**
	 * An empty-handed use: begin, join, call off (the leader, sneaking), take the result, take back the offerings
	 * (sneaking, while nothing holds them) or hear how the circle stands.
	 */
	public void use(ServerPlayer player, ServerLevel level, boolean sneaking) {
		UUID id = player.getUUID();
		switch (run.phase()) {
			case IDLE -> {
				if (sneaking) {
					returnOfferings(player);
				} else if (items.stream().allMatch(ItemStack::isEmpty)) {
					status(player, level);
				} else {
					begin(player, level);
				}
			}
			case GATHERING -> {
				if (!run.joined().contains(id)) {
					if (sneaking) {
						status(player, level);
					} else {
						join(player, level);
					}
				} else if (sneaking && id.equals(run.leader())) {
					cancel(level);
				} else {
					status(player, level);
				}
			}
			case CHANNELING -> {
				if (sneaking && id.equals(run.leader())) {
					cancel(level);
				} else {
					status(player, level);
				}
			}
			case COMPLETE -> {
				if (sneaking) {
					returnOfferings(player);
				} else if (run.joined().contains(id)) {
					takeOutput(player, level);
				} else {
					status(player, level);
				}
			}
		}
	}

	private void returnOfferings(ServerPlayer player) {
		if (run.phase().reserving()) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.circle.locked"));
			return;
		}
		boolean any = false;
		for (int i = 0; i < SLOTS; i++) {
			ItemStack stack = items.get(i);
			if (!stack.isEmpty()) {
				items.set(i, ItemStack.EMPTY);
				player.getInventory().placeItemBackInInventory(stack, Prediction.SERVER_ONLY);
				any = true;
			}
		}
		if (any) {
			setChanged();
		}
		player.sendOverlayMessage(Component.translatable(any ? "message.jugcraft.concordance.circle.returned"
				: "message.jugcraft.concordance.circle.nothing"));
	}

	private void takeOutput(ServerPlayer player, ServerLevel level) {
		ItemStack made = output;
		output = ItemStack.EMPTY;
		if (!made.isEmpty()) {
			player.getInventory().placeItemBackInInventory(made, Prediction.SERVER_ONLY);
		}
		apply(level, run.collected(), null);
	}

	private void begin(ServerPlayer player, ServerLevel level) {
		if (!JugcraftConfig.isFeatureEnabled(JugcraftConcordance.FEATURE)) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.disabled"));
			return;
		}
		if (!RateGate.allow(player, "circle", 10)) {
			return;
		}
		ConcordanceRules rules = ConcordanceData.rules();
		List<Offerings.Slot> slots = slots();
		RitualDefinition chosen = null;
		Offerings.Plan plan = null;
		RitualDefinition unknown = null;
		RitualDefinition closest = null;
		Offerings.Shortfall shortfall = null;
		int closestShort = Integer.MAX_VALUE;
		for (RitualDefinition ritual : rules.ritualsAt(ANCHOR)) {
			Offerings.Match match = Offerings.match(ritual, slots, ConcordanceProgress.TAG_LOOKUP);
			if (!ResearchEngine.knowsRitual(ConcordanceProgress.knowledge(player), ritual)) {
				if (match.plan() != null && unknown == null) {
					unknown = ritual;
				}
			} else if (match.plan() != null) {
				// The most specific match wins (the most items offered); ties go to the first by id.
				if (chosen == null || ritual.offered() > chosen.offered()) {
					chosen = ritual;
					plan = match.plan();
				}
			} else {
				int missing = 0;
				for (Offerings.Shortfall each : match.missing()) {
					missing += each.offering().count() - each.have();
				}
				if (missing < closestShort) {
					closestShort = missing;
					closest = ritual;
					shortfall = match.missing().get(0);
				}
			}
		}
		if (chosen == null) {
			if (unknown != null) {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.circle.unknown", ritualName(unknown.id())));
			} else if (closest != null) {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.circle.missing", ritualName(closest.id()),
						shortfall.offering().count() - shortfall.have(), offeringName(shortfall.offering())));
			} else {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.circle.no_match"));
			}
			return;
		}
		StructurePattern pattern = rules.structure(chosen.structure());
		if (pattern == null || !resultExists(chosen)) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.circle.no_match"));
			return;
		}
		if (!near(player, pattern)) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.circle.too_far"));
			return;
		}
		if (ConcordanceProgress.currentFocus(player) < chosen.focus()) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.circle.no_focus", ritualName(chosen.id()), chosen.focus()));
			return;
		}
		long now = ConcordanceProgress.now(level);
		RitualMachine.Observation seen = observe(level, chosen, pattern, List.of(player.getUUID()));
		RitualMachine.Interruption fault = RitualMachine.fault(seen);
		if (fault != null) {
			player.sendOverlayMessage(refusal(fault, seen.report(), chosen));
			return;
		}
		if (apply(level, run.start(chosen, player.getUUID(), plan, now), chosen)) {
			RitualDefinition started = chosen;
			if (run.phase() == RitualMachine.Phase.CHANNELING) {
				tell(level, run.joined(), Component.translatable("message.jugcraft.concordance.circle.started", ritualName(started.id())));
			} else {
				tell(level, run.joined(), Component.translatable("message.jugcraft.concordance.circle.waiting", ritualName(started.id()),
						started.participants() - run.joined().size()));
			}
			sound(level, JugcraftConcordance.CIRCLE_START_SOUND, 1.0F);
			Signs.show(level, worldPosition, Sign.GATHER);
		}
	}

	private void join(ServerPlayer player, ServerLevel level) {
		RitualDefinition definition = ConcordanceData.rules().ritual(run.ritual());
		StructurePattern pattern = definition == null ? null : ConcordanceData.rules().structure(definition.structure());
		if (definition == null || pattern == null) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.circle.busy"));
			return;
		}
		if (!RateGate.allow(player, "circle", 10)) {
			return;
		}
		if (!ResearchEngine.knowsRitual(ConcordanceProgress.knowledge(player), definition)) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.circle.unknown", ritualName(definition.id())));
			return;
		}
		if (!near(player, pattern)) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.circle.too_far"));
			return;
		}
		if (ConcordanceProgress.currentFocus(player) < definition.focus()) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.circle.no_focus", ritualName(definition.id()), definition.focus()));
			return;
		}
		if (apply(level, run.join(definition, player.getUUID(), ConcordanceProgress.now(level)), definition)) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.circle.joined", ritualName(definition.id())));
			if (run.phase() == RitualMachine.Phase.CHANNELING) {
				tell(level, run.joined(), Component.translatable("message.jugcraft.concordance.circle.started", ritualName(definition.id())));
				sound(level, JugcraftConcordance.CIRCLE_START_SOUND, 1.0F);
				Signs.show(level, worldPosition, Sign.GATHER);
			}
		}
	}

	private void cancel(ServerLevel level) {
		apply(level, run.interrupt(ConcordanceData.rules().ritual(run.ritual()), RitualMachine.Interruption.CANCELLED),
				ConcordanceData.rules().ritual(run.ritual()));
	}

	/** Tells the player how the anchor and its circle stand: the phase, then every fault (or that it is complete). */
	public void status(ServerPlayer player, ServerLevel level) {
		player.sendSystemMessage(Component.translatable("message.jugcraft.concordance.circle.status",
				Component.translatable("block.jugcraft.circle_anchor"), phaseText()));
		StructureValidator.Report current = report(level);
		if (current == null) {
			return;
		}
		if (current.complete()) {
			player.sendSystemMessage(Component.translatable("message.jugcraft.concordance.circle.complete"));
			return;
		}
		player.sendSystemMessage(Component.translatable("message.jugcraft.concordance.circle.faults",
				Component.translatable("structure.jugcraft." + Identifier.parse(current.pattern()).getPath()), current.faults().size()));
		for (StructureValidator.Fault fault : current.faults()) {
			player.sendSystemMessage(faultText(fault));
		}
	}

	public Component phaseText() {
		return switch (run.phase()) {
			case IDLE -> Component.translatable("message.jugcraft.concordance.circle.phase.idle");
			case GATHERING -> Component.translatable("message.jugcraft.concordance.circle.phase.gathering", run.joined().size(), participantsNeeded());
			case CHANNELING -> Component.translatable("message.jugcraft.concordance.circle.phase.channeling", run.done() + 1, steps);
			case COMPLETE -> Component.translatable("message.jugcraft.concordance.circle.phase.complete");
		};
	}

	private int participantsNeeded() {
		RitualDefinition definition = run.ritual() == null ? null : ConcordanceData.rules().ritual(run.ritual());
		return definition == null ? run.joined().size() : definition.participants();
	}

	/** One fault, said in words, with the position it is at. */
	public Component faultText(StructureValidator.Fault fault) {
		StructurePattern.Offset offset = fault.part().offset();
		BlockPos at = worldPosition.offset(offset.x(), offset.y(), offset.z());
		Component where = Component.literal(at.getX() + ", " + at.getY() + ", " + at.getZ());
		Component role = Component.translatable("message.jugcraft.concordance.circle.role." + fault.part().role().id);
		String key = "message.jugcraft.concordance.circle.fault." + fault.problem().id;
		return switch (fault.problem()) {
			case MISSING, INCOMPATIBLE -> Component.translatable(key, role, where);
			case OBSTRUCTED, UNPOWERED, FOREIGN -> Component.translatable(key, where);
			case UNLOADED -> Component.translatable(key, role);
		};
	}

	private Component refusal(RitualMachine.Interruption fault, StructureValidator.Report seen, RitualDefinition ritual) {
		return switch (fault) {
			case CONDITIONS -> Component.translatable("message.jugcraft.concordance.circle.too_bright", ritualName(ritual.id()), ritual.maxLight());
			case PARTICIPANTS -> Component.translatable("message.jugcraft.concordance.circle.too_far");
			default -> seen.faults().isEmpty() ? Component.translatable("message.jugcraft.concordance.circle.no_match") : faultText(seen.faults().get(0));
		};
	}

	// ---------------------------------------------------------------- the ritual

	void serverTick(ServerLevel level) {
		if (!run.phase().reserving()) {
			return;
		}
		RitualDefinition definition = ConcordanceData.rules().ritual(run.ritual());
		StructurePattern pattern = definition == null ? null : ConcordanceData.rules().structure(definition.structure());
		if (resumed) {
			resumed = false;
			apply(level, run.interrupt(definition, RitualMachine.Interruption.LAPSED), definition);
			return;
		}
		if (definition == null || pattern == null) {
			apply(level, run.interrupt(definition, RitualMachine.Interruption.FORGOTTEN), definition);
			return;
		}
		if (!JugcraftConfig.isFeatureEnabled(JugcraftConcordance.FEATURE)) {
			apply(level, run.interrupt(definition, RitualMachine.Interruption.DISABLED), definition);
			return;
		}
		long now = ConcordanceProgress.now(level);
		RitualMachine.Observation seen = run.due(now) && !run.lapsed(now) ? observe(level, definition, pattern, run.joined()) : null;
		int before = run.done();
		RitualMachine.Phase phase = run.phase();
		if (apply(level, run.tick(definition, seen, now), definition) && run.phase() == RitualMachine.Phase.CHANNELING
				&& phase == RitualMachine.Phase.CHANNELING && run.done() > before) {
			sound(level, JugcraftConcordance.CIRCLE_STEP_SOUND, 0.8F + 0.1F * run.done());
		}
	}

	/** What the anchor sees now: a fresh check of the whole circle, who is present and whether the light allows it. */
	private RitualMachine.Observation observe(ServerLevel level, RitualDefinition ritual, StructurePattern pattern, List<UUID> joined) {
		StructureValidator.Report checked = check(level, pattern, ritual.ley(), joined);
		int present = 0;
		for (UUID id : joined) {
			ServerPlayer player = level.getServer().getPlayerList().getPlayer(id);
			if (player != null && near(player, pattern)) {
				present++;
			}
		}
		boolean conditions = ritual.maxLight() == null || level.getMaxLocalRawBrightness(worldPosition.above()) <= ritual.maxLight();
		return new RitualMachine.Observation(checked, joined.size(), present, conditions);
	}

	/** The report to show: the last step's while a ritual runs, else the idle check (cached, shape only). */
	public StructureValidator.@Nullable Report report(ServerLevel level) {
		if (run.phase().reserving() && report != null) {
			return report;
		}
		long now = ConcordanceProgress.now(level);
		if (report == null || reportStale || now - reportAt >= CACHE_TICKS || now < reportAt) {
			StructurePattern pattern = idlePattern();
			if (pattern == null) {
				return null;
			}
			check(level, pattern, 0, null);
		}
		return report;
	}

	/** The structure an idle anchor is judged by: the first ritual's at this anchor (all use the Lesser Circle). */
	private @Nullable StructurePattern idlePattern() {
		ConcordanceRules rules = ConcordanceData.rules();
		RitualDefinition running = run.ritual() == null ? null : rules.ritual(run.ritual());
		if (running != null && rules.structure(running.structure()) != null) {
			return rules.structure(running.structure());
		}
		for (RitualDefinition ritual : rules.ritualsAt(ANCHOR)) {
			return rules.structure(ritual.structure());
		}
		return null;
	}

	/**
	 * Checks the circle round this anchor and keeps the report: {@code ley} required in each channel, which must be
	 * lent to {@code participants} (null: anyone's will do, for an idle check of the shape).
	 */
	private StructureValidator.Report check(ServerLevel level, StructurePattern pattern, long ley, @Nullable List<UUID> participants) {
		StructureValidator.Report checked = StructureValidator.check(pattern, part -> {
			StructurePattern.Offset offset = part.offset();
			BlockPos at = worldPosition.offset(offset.x(), offset.y(), offset.z());
			if (!level.isLoaded(at)) {
				return StructureValidator.Seen.UNLOADED;
			}
			BlockState state = level.getBlockState(at);
			boolean matches = part.block() != null && matches(state, part.block());
			long held = 0L;
			boolean lent = true;
			if (part.role() == StructurePattern.Role.CHANNEL && level.getBlockEntity(at) instanceof LeyPylonBlockEntity pylon) {
				held = pylon.ley();
				lent = participants == null || pylon.lends(participants);
			}
			return new StructureValidator.Seen(true, state.isAir(), matches, held, lent);
		}, ley);
		report = checked;
		reportAt = ConcordanceProgress.now(level);
		reportStale = false;
		StringBuilder offsets = new StringBuilder();
		for (StructurePattern.Part part : pattern.channels()) {
			offsets.append(offsets.isEmpty() ? "" : ";").append(part.offset());
		}
		if (linked != checked.linked() || !channels.contentEquals(offsets)) {
			linked = checked.linked();
			channels = offsets.toString();
			sync(level);
		}
		return checked;
	}

	private static boolean matches(BlockState state, String spec) {
		if (spec.startsWith("#")) {
			Identifier tag = Identifier.tryParse(spec.substring(1));
			return tag != null && state.is(BLOCK_TAGS.computeIfAbsent(spec, unused -> TagKey.create(Registries.BLOCK, tag)));
		}
		return BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString().equals(spec);
	}

	/** Whether the player can take part from where they stand: alive, here, and close enough to the circle. */
	private boolean near(ServerPlayer player, StructurePattern pattern) {
		int range = RitualMachine.participantRange(pattern);
		return player.level() == level && player.isAlive() && !player.isSpectator()
				&& Math.abs(player.getX() - (worldPosition.getX() + 0.5)) <= range && Math.abs(player.getZ() - (worldPosition.getZ() + 0.5)) <= range
				&& Math.abs(player.getY() - worldPosition.getY()) <= RitualMachine.PARTICIPANT_HEIGHT;
	}

	/**
	 * Carries out a transition's actions in order, then takes its state. Returns false, changing nothing more, if a
	 * participant cannot pay; a draw that finds a channel short, or offerings that no longer hold at completion,
	 * interrupt the ritual instead (POWER, TAMPERED), so no action is ever half done.
	 */
	private boolean apply(ServerLevel level, RitualRun.Transition transition, @Nullable RitualDefinition definition) {
		if (transition.next() == run && transition.actions().isEmpty()) {
			return false;
		}
		RitualRun before = run;
		ItemStack source = ItemStack.EMPTY;
		for (RitualRun.Action action : transition.actions()) {
			switch (action) {
				case RitualRun.Pay pay -> {
					ServerPlayer player = level.getServer().getPlayerList().getPlayer(pay.participant());
					if (player == null || !ConcordanceProgress.spendFocus(player, pay.focus())) {
						return false;
					}
				}
				case RitualRun.Reserve reserve -> {
					// The phase it moves to locks the slots; the plan is saved with it.
				}
				case RitualRun.Draw draw -> {
					if (definition == null || !draw(level, definition, draw.ley(), before.joined())) {
						apply(level, before.interrupt(definition, RitualMachine.Interruption.POWER), definition);
						return false;
					}
				}
				case RitualRun.Consume consume -> {
					if (definition == null || !Offerings.holds(definition, consume.plan(), slots()) || !resultExists(definition)) {
						apply(level, before.interrupt(definition, RitualMachine.Interruption.TAMPERED), definition);
						return false;
					}
					source = consume(consume.plan());
				}
				case RitualRun.Commit commit -> commit(level, definition, commit, source);
				case RitualRun.Release release -> {
					if (release.where() == RitualMachine.Release.DROPPED) {
						dropAll(level);
					}
				}
				case RitualRun.Backlash backlash -> backlash(level, backlash);
			}
		}
		run = transition.next();
		if (definition != null && run.phase().reserving()) {
			steps = definition.steps();
		}
		setChanged();
		if (before.phase().reserving() && !run.phase().reserving() && run.last() != null) {
			RitualMachine.Interruption reason = run.last();
			Component name = ritualName(before.ritual());
			tell(level, before.joined(), Component.translatable("message.jugcraft.concordance.circle.interrupted." + reason.id, name));
			if (reason.consequence().offerings() == RitualMachine.Release.KEPT) {
				tell(level, before.joined(), Component.translatable("message.jugcraft.concordance.circle.kept"));
			}
			// Roadmap step 27: a shortage or a danger says which; a deliberate stop is only the circle's break.
			Sign warning = warning(reason);
			if (warning != null) {
				Signs.show(level, worldPosition, warning);
				// And where it fell short or broke: each part the last check found at fault (a dry channel, a missing stone).
				if (report != null && (reason == RitualMachine.Interruption.POWER || reason == RitualMachine.Interruption.STRUCTURE
						|| reason == RitualMachine.Interruption.CONTAINMENT)) {
					for (StructureValidator.Fault fault : report.faults()) {
						StructurePattern.Offset offset = fault.part().offset();
						Signs.show(level, worldPosition.offset(offset.x(), offset.y(), offset.z()), warning);
					}
				}
			} else {
				sound(level, JugcraftConcordance.CIRCLE_BREAK_SOUND, 1.0F);
			}
			ConcordanceProgress.log("ritual {} at {} interrupted: {}", before.ritual(), worldPosition, reason.id);
		}
		if (reasonIsRemoval()) {
			return true;
		}
		sync(level);
		return true;
	}

	/**
	 * What an interruption shows (roadmap step 27): danger for a failed containment or tampered offerings; shortage when
	 * something the ritual needs is missing (a part, Ley Charge, a participant, its conditions, a loaded chunk, a running
	 * anchor); nothing more than the circle's break for a deliberate stop (cancelled, removed, switched off, forgotten).
	 */
	public static @Nullable Sign warning(RitualMachine.Interruption reason) {
		return switch (reason) {
			case CONTAINMENT, TAMPERED -> Sign.PERIL;
			case STRUCTURE, POWER, PARTICIPANTS, CONDITIONS, UNLOADED, LAPSED -> Sign.WANT;
			case CANCELLED, REMOVED, DISABLED, FORGOTTEN -> null;
		};
	}

	private boolean reasonIsRemoval() {
		return run.last() == RitualMachine.Interruption.REMOVED;
	}

	/** Draws a step's Ley Charge from every channel, all or none. */
	private boolean draw(ServerLevel level, RitualDefinition ritual, int ley, List<UUID> participants) {
		StructurePattern pattern = ConcordanceData.rules().structure(ritual.structure());
		if (pattern == null) {
			return false;
		}
		List<LeyPylonBlockEntity> pylons = new ArrayList<>();
		for (StructurePattern.Part part : pattern.channels()) {
			StructurePattern.Offset offset = part.offset();
			BlockPos at = worldPosition.offset(offset.x(), offset.y(), offset.z());
			if (!(level.getBlockEntity(at) instanceof LeyPylonBlockEntity pylon) || pylon.ley() < ley || !pylon.lends(participants)) {
				// Roadmap step 27: the channel that fell short says so, where it stands.
				Signs.show(level, at, Sign.WANT);
				return false;
			}
			pylons.add(pylon);
		}
		for (LeyPylonBlockEntity pylon : pylons) {
			pylon.draw(level, ley);
			// The Ley Charge the step took, travelling from each channel to the anchor.
			Signs.flow(level, pylon.getBlockPos(), worldPosition);
		}
		return true;
	}

	/** Takes exactly the reserved offerings; returns one of the item that is transformed (empty if none is). */
	private ItemStack consume(Offerings.Plan plan) {
		ItemStack source = ItemStack.EMPTY;
		for (int i = 0; i < SLOTS; i++) {
			int take = plan.take().get(i);
			if (take == 0) {
				continue;
			}
			ItemStack stack = items.get(i);
			if (i == plan.source()) {
				source = stack.copyWithCount(1);
			}
			stack.shrink(take);
			if (stack.isEmpty()) {
				items.set(i, ItemStack.EMPTY);
			}
		}
		return source;
	}

	/** Makes the result, once: the transformed item into the output slot, or the effects; and records the practice. */
	private void commit(ServerLevel level, @Nullable RitualDefinition ritual, RitualRun.Commit commit, ItemStack source) {
		if (ritual == null) {
			return;
		}
		List<ServerPlayer> present = present(level, commit.participants());
		switch (ritual.result()) {
			case RitualDefinition.Transform transform -> {
				ItemStack made = new ItemStack(item(transform.into()));
				// The instrument keeps what was written on it.
				copy(source, made, JugcraftConcordance.INSCRIPTION);
				copy(source, made, JugcraftConcordance.TUNINGS);
				output = made;
			}
			case RitualDefinition.Effects effects -> grant(level, ritual, effects, present);
		}
		String chunk = Long.toString(ChunkPos.containing(worldPosition).pack());
		for (ServerPlayer player : present) {
			ConcordanceProgress.record(player, new Evidence.Practiced(ACTIVITY, chunk));
		}
		Rituals.completed(level, worldPosition, ritual.id(), present);
		tell(level, commit.participants(), Component.translatable("message.jugcraft.concordance.circle.completed", ritualName(ritual.id())));
		sound(level, JugcraftConcordance.CIRCLE_COMPLETE_SOUND, 1.0F);
		Signs.show(level, worldPosition.above(), Sign.DONE);
		ConcordanceProgress.log("ritual {} completed at {} by {}", ritual.id(), worldPosition, commit.participants());
	}

	/** A ritual's effects, through the shared effect executor: one event, one ledger, credited to its leader. */
	private void grant(ServerLevel level, RitualDefinition ritual, RitualDefinition.Effects effects, List<ServerPlayer> present) {
		int targets = 0;
		int work = 0;
		for (RitualDefinition.Grant grant : effects.grants()) {
			targets += grant.targets();
			work += grant.targets() * grant.effect().kind().work;
		}
		Cause cause = Cause.of(run.leader(), Cause.Origin.RITUAL, ritual.id(), ConcordanceEffects.nextSerial());
		Entity actor = run.leader() == null ? null : level.getEntity(run.leader());
		Ledger ledger = new Ledger(new Ledger.Limits(targets, work, 0));
		Vec3 origin = Vec3.atCenterOf(worldPosition.above());
		int index = 0;
		for (RitualDefinition.Grant grant : effects.grants()) {
			List<? extends LivingEntity> reached = grant.target() == RitualDefinition.Target.PARTICIPANTS ? present
					: creatures(level, origin, grant);
			ConcordanceEffects.Context context = new ConcordanceEffects.Context(level, cause, actor, ledger, "grant" + index, origin);
			for (LivingEntity target : reached) {
				ConcordanceEffects.apply(context, grant.effect(), target);
			}
			index++;
		}
	}

	private static List<LivingEntity> creatures(ServerLevel level, Vec3 origin, RitualDefinition.Grant grant) {
		int radius = grant.radius();
		List<LivingEntity> found = new ArrayList<>(level.getEntitiesOfClass(LivingEntity.class, new AABB(origin, origin).inflate(radius),
				entity -> entity.isAlive() && !(entity instanceof Player) && entity.distanceToSqr(origin) <= (double) radius * radius));
		found.sort(Comparator.<LivingEntity>comparingDouble(entity -> entity.distanceToSqr(origin)).thenComparing(Entity::getUUID));
		return found.size() > grant.targets() ? found.subList(0, grant.targets()) : found;
	}

	/** A player broke a block in reach (Rituals): remembered for a step, so a failed containment is theirs. */
	void disturbedBy(@Nullable UUID player, long now) {
		if (player != null) {
			disturber = player;
			disturbedAt = now;
		}
	}

	/**
	 * Containment failed: the working lashes each participant present. If a player broke the circle within the last
	 * step, the lash is theirs (roadmap step 28): it reaches only those they could harm (PvP, parties), so breaking a
	 * stranger's boundary stone is no way round the multiplayer rules. Otherwise (an explosion, a piston, time) it is
	 * sourceless magic damage.
	 */
	private void backlash(ServerLevel level, RitualRun.Backlash backlash) {
		long now = level.getGameTime();
		ServerPlayer breaker = now - disturbedAt <= RitualMachine.STEP_TICKS + 20L ? Authority.present(level, disturber) : null;
		Cause cause = Cause.of(breaker == null ? null : breaker.getUUID(), Cause.Origin.RITUAL, run.ritual() == null ? ANCHOR : run.ritual(),
				ConcordanceEffects.nextSerial());
		Ledger ledger = new Ledger(new Ledger.Limits(backlash.participants().size(), backlash.participants().size(), 0));
		EffectSpec damage = EffectSpec.of(EffectKind.DAMAGE, Intent.HARMFUL, backlash.damage(), 0);
		Vec3 origin = Vec3.atCenterOf(worldPosition.above());
		ConcordanceEffects.Context context = new ConcordanceEffects.Context(level, cause, breaker, ledger, "backlash", origin);
		for (ServerPlayer player : present(level, backlash.participants())) {
			ConcordanceEffects.apply(context, damage, player);
		}
	}

	private List<ServerPlayer> present(ServerLevel level, List<UUID> participants) {
		List<ServerPlayer> out = new ArrayList<>();
		for (UUID id : participants) {
			ServerPlayer player = level.getServer().getPlayerList().getPlayer(id);
			if (player != null && player.level() == level && player.isAlive()) {
				out.add(player);
			}
		}
		return out;
	}

	private static boolean resultExists(RitualDefinition ritual) {
		return !(ritual.result() instanceof RitualDefinition.Transform transform) || item(transform.into()) != Items.AIR;
	}

	private static Item item(String id) {
		Identifier key = Identifier.tryParse(id);
		return key == null ? Items.AIR : BuiltInRegistries.ITEM.getValue(key);
	}

	private static <T> void copy(ItemStack from, ItemStack to, DataComponentType<T> type) {
		T value = from.get(type);
		if (value != null) {
			to.set(type, value);
		}
	}

	private List<Offerings.Slot> slots() {
		List<Offerings.Slot> out = new ArrayList<>(SLOTS);
		for (ItemStack stack : items) {
			out.add(stack.isEmpty() ? Offerings.Slot.EMPTY : new Offerings.Slot(ConcordanceProgress.itemId(stack), stack.getCount()));
		}
		return out;
	}

	private void dropAll(ServerLevel level) {
		for (int i = 0; i < SLOTS; i++) {
			ItemStack stack = items.get(i);
			if (!stack.isEmpty()) {
				items.set(i, ItemStack.EMPTY);
				Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack);
			}
		}
		if (!output.isEmpty()) {
			ItemStack made = output;
			output = ItemStack.EMPTY;
			Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), made);
		}
	}

	/** Broken: a running ritual breaks off (REMOVED), and everything in the anchor drops where it stood, once. */
	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		if (level instanceof ServerLevel server) {
			if (run.phase().reserving()) {
				RitualDefinition definition = ConcordanceData.rules().ritual(run.ritual());
				apply(server, run.interrupt(definition, RitualMachine.Interruption.REMOVED), definition);
			}
			dropAll(server);
		}
	}

	private void tell(ServerLevel level, List<UUID> participants, Component message) {
		for (UUID id : participants) {
			ServerPlayer player = level.getServer().getPlayerList().getPlayer(id);
			if (player != null) {
				player.sendOverlayMessage(message);
			}
		}
	}

	private void sound(ServerLevel level, SoundEvent sound, float pitch) {
		level.playSound(null, worldPosition, sound, SoundSource.BLOCKS, 0.8F, pitch);
	}

	public static Component ritualName(@Nullable String ritual) {
		Identifier id = ritual == null ? null : Identifier.tryParse(ritual);
		return id == null ? Component.literal(String.valueOf(ritual))
				: Component.translatableWithFallback("ritual." + id.getNamespace() + "." + id.getPath(), ritual);
	}

	private static Component offeringName(RitualDefinition.Offering offering) {
		if (offering.tag()) {
			return Component.literal("#" + offering.item());
		}
		return new ItemStack(item(offering.item())).getHoverName();
	}

	// ---------------------------------------------------------------- saving and clients

	private void sync(ServerLevel level) {
		level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
		ContainerHelper.loadAllItems(input, items);
		output = input.read("output", ItemStack.CODEC).orElse(ItemStack.EMPTY);
		RitualMachine.Phase phase = RitualMachine.Phase.fromId(input.getStringOr("phase", "idle"));
		String ritual = input.getStringOr("ritual", "");
		UUID leader = input.read("leader", UUIDUtil.CODEC).orElse(null);
		List<UUID> joined = input.read("joined", UUIDUtil.CODEC.listOf()).orElse(List.of());
		Offerings.Plan plan = null;
		List<Integer> take = input.read("plan_take", Codec.INT.listOf()).orElse(List.of());
		List<String> taken = input.read("plan_items", Codec.STRING.listOf()).orElse(List.of());
		if (take.size() == SLOTS && taken.size() == SLOTS) {
			plan = new Offerings.Plan(take, taken, input.getIntOr("plan_source", -1));
		}
		String last = input.getStringOr("last", "");
		if (phase.reserving() && plan == null || phase != RitualMachine.Phase.IDLE && ritual.isEmpty()) {
			phase = RitualMachine.Phase.IDLE; // a damaged save: nothing is held
			plan = null;
		}
		run = new RitualRun(phase, ritual.isEmpty() ? null : ritual, leader, joined, input.getIntOr("done", 0),
				input.getLongOr("since", 0L), input.getLongOr("next", 0L), phase.reserving() ? plan : null,
				RitualMachine.Interruption.fromId(last), input.getBooleanOr("completed", false));
		linked = input.getIntOr("linked", 0);
		steps = input.getIntOr("steps", 0);
		channels = input.getStringOr("channels", "");
		// Saved while a ritual held its offerings: it did not run while unloaded, so it lapses on its next tick.
		resumed = run.phase().reserving();
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		Saved.stamp(output, 1);
		ContainerHelper.saveAllItems(output, items);
		if (!this.output.isEmpty()) {
			output.store("output", ItemStack.CODEC, this.output);
		}
		saveRun(output);
	}

	private void saveRun(ValueOutput output) {
		output.putString("phase", run.phase().id);
		if (run.ritual() != null) {
			output.putString("ritual", run.ritual());
		}
		if (run.leader() != null) {
			output.store("leader", UUIDUtil.CODEC, run.leader());
		}
		output.store("joined", UUIDUtil.CODEC.listOf(), run.joined());
		output.putInt("done", run.done());
		output.putLong("since", run.since());
		output.putLong("next", run.next());
		if (run.plan() != null) {
			output.store("plan_take", Codec.INT.listOf(), run.plan().take());
			output.store("plan_items", Codec.STRING.listOf(), run.plan().items());
			output.putInt("plan_source", run.plan().source());
		}
		if (run.last() != null) {
			output.putString("last", run.last().id);
		}
		output.putBoolean("completed", run.completed());
		output.putInt("linked", linked);
		output.putInt("steps", steps);
		output.putString("channels", channels);
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	/** Clients get the phase, participants, steps and linked channels; never the offerings or the reservation. */
	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		CompoundTag tag = saveWithoutMetadata(registries);
		for (String key : List.of("Items", "output", "plan_take", "plan_items", "plan_source", "leader", "since", "next", "last")) {
			tag.remove(key);
		}
		return tag;
	}

	// ---------------------------------------------------------------- GeckoLib (drawn on the client)

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<CircleAnchorBlockEntity>("main", 10,
				test -> test.setAndContinue(test.animatable().phase().reserving() ? CHANNEL : IDLE)));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}
}
