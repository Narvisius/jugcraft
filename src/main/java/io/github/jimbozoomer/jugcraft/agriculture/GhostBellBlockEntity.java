package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * A round of Ghost Tag, run on the server by a {@link GhostBellBlock}. Ringing the bell starts a round of
 * {@value #ROUND_TICKS} ticks with everyone within {@value #RANGE} blocks (at least two, at most {@value #MAX_PLAYERS});
 * one of them, at random, is the ghost: "it", glowing. "It" tags someone by hitting them ({@link #onAttack}; no harm is
 * done, and players in the round can't hurt each other while it lasts), and then they are "it" and glow, but they
 * can't tag back whoever tagged them for {@value #TAG_BACK_TICKS} ticks. Players who leave, or go more than
 * {@value #LEAVE_RANGE} blocks away, drop out (if "it" goes, someone else is "it"). When time is up, whoever is "it"
 * loses. The round is saved.
 */
public class GhostBellBlockEntity extends BlockEntity {
	public static final int ROUND_TICKS = 2400;
	public static final int RANGE = 16;
	public static final int LEAVE_RANGE = 48;
	public static final int MAX_PLAYERS = 32;
	public static final int TAG_BACK_TICKS = 40;
	public static final int PERIOD = 20;
	private static final Map<Level, Set<BlockPos>> RINGING = new WeakHashMap<>();

	private long endsAt;
	private final Set<UUID> players = new LinkedHashSet<>();
	private UUID it;
	private UUID taggedBy;
	private long taggedAt;

	public GhostBellBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftAgriculture.GHOST_BELL_ENTITY, pos, state);
	}

	public boolean running() {
		return endsAt > 0;
	}

	public Set<UUID> players() {
		return Set.copyOf(players);
	}

	public UUID it() {
		return it;
	}

	/** Rings the bell: starts a round with everyone near, or tells {@code player} who is "it". */
	public void ring(ServerLevel level, ServerPlayer player) {
		if (running()) {
			ServerPlayer ghost = it == null ? null : level.getServer().getPlayerList().getPlayer(it);
			player.sendOverlayMessage(Component.translatable("message.jugcraft.ghost_bell.running", ghost == null ? "?" : ghost.getName().getString(),
					Math.max(0, (endsAt - level.getGameTime()) / 20)));
			return;
		}
		List<ServerPlayer> near = new ArrayList<>();
		for (ServerPlayer other : level.players()) {
			if (!other.isSpectator() && other.distanceToSqr(Vec3.atCenterOf(worldPosition)) <= (double) RANGE * RANGE && near.size() < MAX_PLAYERS) {
				near.add(other);
			}
		}
		if (near.size() < 2) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.ghost_bell.alone"));
			return;
		}
		players.clear();
		near.forEach(other -> players.add(other.getUUID()));
		ServerPlayer ghost = near.get(level.getRandom().nextInt(near.size()));
		it = ghost.getUUID();
		taggedBy = null;
		endsAt = level.getGameTime() + ROUND_TICKS;
		setChanged();
		level.setBlock(worldPosition, getBlockState().setValue(GhostBellBlock.RINGING, true), Block.UPDATE_ALL);
		RINGING.computeIfAbsent(level, l -> new HashSet<>()).add(worldPosition);
		glow(ghost);
		level.playSound(null, worldPosition, SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 1.0F, 0.7F);
		tell(level, Component.translatable("message.jugcraft.ghost_bell.start", ghost.getName(), ROUND_TICKS / 20));
	}

	private static void glow(ServerPlayer player) {
		player.addEffect(new MobEffectInstance(MobEffects.GLOWING, PERIOD * 2 + 5, 0, false, false));
	}

	void serverTick(ServerLevel level) {
		RINGING.computeIfAbsent(level, l -> new HashSet<>()).add(worldPosition);
		if (Math.floorMod(level.getGameTime() + worldPosition.asLong(), PERIOD) != 0) {
			return;
		}
		players.removeIf(id -> {
			ServerPlayer player = level.getServer().getPlayerList().getPlayer(id);
			return player == null || player.level() != level || player.distanceToSqr(Vec3.atCenterOf(worldPosition)) > (double) LEAVE_RANGE * LEAVE_RANGE;
		});
		if (players.size() < 2 || level.getGameTime() >= endsAt) {
			end(level);
			return;
		}
		if (it == null || !players.contains(it)) {
			List<UUID> left = new ArrayList<>(players);
			it = left.get(level.getRandom().nextInt(left.size()));
			taggedBy = null;
			ServerPlayer ghost = level.getServer().getPlayerList().getPlayer(it);
			if (ghost != null) {
				tell(level, Component.translatable("message.jugcraft.ghost_bell.tagged", ghost.getName()));
			}
		}
		ServerPlayer ghost = level.getServer().getPlayerList().getPlayer(it);
		if (ghost != null) {
			glow(ghost);
		}
		setChanged();
	}

	/** Tags {@code target} if {@code tagger} is "it" and may tag them; returns whether they did. */
	public boolean tag(ServerLevel level, ServerPlayer tagger, ServerPlayer target) {
		if (!running() || !tagger.getUUID().equals(it) || !players.contains(target.getUUID())) {
			return false;
		}
		if (target.getUUID().equals(taggedBy) && level.getGameTime() - taggedAt < TAG_BACK_TICKS) {
			tagger.sendOverlayMessage(Component.translatable("message.jugcraft.ghost_bell.no_tag_back"));
			return false;
		}
		tagger.removeEffect(MobEffects.GLOWING);
		it = target.getUUID();
		taggedBy = tagger.getUUID();
		taggedAt = level.getGameTime();
		setChanged();
		glow(target);
		level.playSound(null, target.blockPosition(), SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 0.5F, 1.8F);
		tell(level, Component.translatable("message.jugcraft.ghost_bell.tagged", target.getName()));
		return true;
	}

	/** Ends the round: whoever is "it" loses. */
	public void end(ServerLevel level) {
		ServerPlayer ghost = it == null ? null : level.getServer().getPlayerList().getPlayer(it);
		if (ghost != null) {
			ghost.removeEffect(MobEffects.GLOWING);
			tell(level, Component.translatable("message.jugcraft.ghost_bell.end", ghost.getName()));
		}
		players.clear();
		it = null;
		taggedBy = null;
		endsAt = 0;
		setChanged();
		Set<BlockPos> ringing = RINGING.get(level);
		if (ringing != null) {
			ringing.remove(worldPosition);
		}
		if (getBlockState().getValue(GhostBellBlock.RINGING)) {
			level.setBlock(worldPosition, getBlockState().setValue(GhostBellBlock.RINGING, false), Block.UPDATE_ALL);
		}
		level.playSound(null, worldPosition, SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 1.0F, 0.5F);
	}

	private void tell(ServerLevel level, Component message) {
		for (UUID id : players) {
			ServerPlayer player = level.getServer().getPlayerList().getPlayer(id);
			if (player != null) {
				player.sendSystemMessage(message);
			}
		}
	}

	/**
	 * One player hitting another while both are in a round: a tag if the hitter is "it", and never any harm (Fabric's
	 * AttackEntityCallback, on the server). Anything else is left alone.
	 */
	public static InteractionResult onAttack(Player player, Level level, InteractionHand hand, Entity entity) {
		if (level.isClientSide() || !(player instanceof ServerPlayer tagger) || !(entity instanceof ServerPlayer target)) {
			return InteractionResult.PASS;
		}
		Set<BlockPos> ringing = RINGING.get(level);
		if (ringing == null) {
			return InteractionResult.PASS;
		}
		for (BlockPos pos : List.copyOf(ringing)) {
			if (level.getBlockEntity(pos) instanceof GhostBellBlockEntity bell && bell.running() && bell.players.contains(tagger.getUUID())
					&& bell.players.contains(target.getUUID())) {
				bell.tag((ServerLevel) level, tagger, target);
				return InteractionResult.SUCCESS;
			}
		}
		return InteractionResult.PASS;
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		endsAt = input.getLongOr("ends_at", 0L);
		players.clear();
		players.addAll(input.read("players", UUIDUtil.CODEC.listOf()).orElse(List.of()));
		it = input.read("it", UUIDUtil.CODEC).orElse(null);
		taggedBy = input.read("tagged_by", UUIDUtil.CODEC).orElse(null);
		taggedAt = input.getLongOr("tagged_at", 0L);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putLong("ends_at", endsAt);
		output.store("players", UUIDUtil.CODEC.listOf(), List.copyOf(players));
		Optional.ofNullable(it).ifPresent(id -> output.store("it", UUIDUtil.CODEC, id));
		Optional.ofNullable(taggedBy).ifPresent(id -> output.store("tagged_by", UUIDUtil.CODEC, id));
		output.putLong("tagged_at", taggedAt);
	}
}
