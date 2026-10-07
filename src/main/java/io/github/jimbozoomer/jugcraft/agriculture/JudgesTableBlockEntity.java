package io.github.jimbozoomer.jugcraft.agriculture;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * A round of the costume contest, run on the server by a {@link JudgesTableBlock}. Ringing the bell opens a round of
 * {@value #ROUND_TICKS} ticks. Every {@value #PERIOD} ticks the table looks at the level's players within
 * {@value #RANGE} blocks: one standing on a {@link CostumeRunwayBlock} with a costume on their head (the
 * trick-or-treat costume tag, {@link TrickOrTreat#COSTUMES}) or a painted face ({@link FacePaint}) is entered (at most
 * {@value #MAX_CONTESTANTS}). Anyone else within range votes by using the contestant they like best with an empty hand
 * ({@link #onUseEntity}): one vote each, which they may move, never for themselves. When the round ends, the contestants
 * with the most votes (at least one) get a Best Costume Ribbon, if they are still on the server. The round, its
 * contestants and its votes are saved.
 */
public class JudgesTableBlockEntity extends BlockEntity {
	public static final int ROUND_TICKS = 1200;
	public static final int RANGE = 16;
	public static final int MAX_CONTESTANTS = 16;
	public static final int PERIOD = 10;
	/** How far away players hear the contest open and close. */
	public static final int ANNOUNCE_RANGE = 32;
	/** The contestants in the order they walked the runway (a map's order isn't kept). */
	private static final Codec<List<Entrant>> CONTESTANTS = Entrant.CODEC.listOf();
	private static final Codec<Map<UUID, UUID>> VOTES = Codec.unboundedMap(UUIDUtil.STRING_CODEC, UUIDUtil.CODEC);
	/** The tables with a round open, per level, for votes to find. */
	private static final Map<Level, Set<BlockPos>> OPEN = new WeakHashMap<>();

	public enum Vote {
		VOTED, MOVED, SAME, OWN, NOT_IN_ROUND
	}

	private record Entrant(UUID id, String name) {
		static final Codec<Entrant> CODEC = RecordCodecBuilder.create(i -> i.group(
				UUIDUtil.CODEC.fieldOf("id").forGetter(Entrant::id),
				Codec.STRING.fieldOf("name").forGetter(Entrant::name)).apply(i, Entrant::new));
	}

	private long endsAt;
	private final Map<UUID, String> contestants = new LinkedHashMap<>();
	private final Map<UUID, UUID> votes = new HashMap<>();

	public JudgesTableBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftAgriculture.JUDGES_TABLE_ENTITY, pos, state);
	}

	public boolean open() {
		return endsAt > 0;
	}

	/** The contestants, in the order they walked the runway. */
	public List<UUID> contestants() {
		return List.copyOf(contestants.keySet());
	}

	public int votesFor(UUID contestant) {
		int count = 0;
		for (UUID vote : votes.values()) {
			count += vote.equals(contestant) ? 1 : 0;
		}
		return count;
	}

	void serverTick(ServerLevel level) {
		OPEN.computeIfAbsent(level, l -> new HashSet<>()).add(worldPosition);
		if (Math.floorMod(level.getGameTime() + worldPosition.asLong(), PERIOD) != 0) {
			return;
		}
		enterWalkers(level);
		if (level.getGameTime() >= endsAt) {
			finish(level);
		}
	}

	/** Enters everyone in costume standing on a runway within range. */
	public void enterWalkers(ServerLevel level) {
		Block runway = JugcraftAgriculture.block("costume_runway");
		for (ServerPlayer player : level.players()) {
			if (player.isSpectator() || contestants.containsKey(player.getUUID()) || contestants.size() >= MAX_CONTESTANTS || !inRange(player)) {
				continue;
			}
			boolean onRunway = level.getBlockState(player.blockPosition()).is(runway) || level.getBlockState(player.getOnPos()).is(runway);
			if (onRunway && FacePaint.inCostume(player)) {
				contestants.put(player.getUUID(), player.getName().getString());
				setChanged();
				player.sendOverlayMessage(Component.translatable("message.jugcraft.judges_table.entered"));
				level.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, 0.8F, 1.4F);
			}
		}
	}

	private boolean inRange(Player player) {
		return player.distanceToSqr(Vec3.atCenterOf(worldPosition)) <= (double) RANGE * RANGE;
	}

	/** Rings the bell to open a round, or tells {@code player} how the round stands. */
	public void use(ServerLevel level, ServerPlayer player) {
		if (!open()) {
			endsAt = level.getGameTime() + ROUND_TICKS;
			contestants.clear();
			votes.clear();
			setChanged();
			level.setBlock(worldPosition, getBlockState().setValue(JudgesTableBlock.OPEN, true), Block.UPDATE_ALL);
			OPEN.computeIfAbsent(level, l -> new HashSet<>()).add(worldPosition);
			level.playSound(null, worldPosition, SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 1.0F, 1.2F);
			announce(level, Component.translatable("message.jugcraft.judges_table.open", ROUND_TICKS / 20));
			return;
		}
		if (contestants.isEmpty()) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.judges_table.nobody", seconds(level)));
			return;
		}
		List<String> standings = new ArrayList<>();
		contestants.forEach((id, name) -> standings.add(name + " " + votesFor(id)));
		player.sendOverlayMessage(Component.translatable("message.jugcraft.judges_table.standings", String.join(", ", standings), seconds(level)));
	}

	private long seconds(ServerLevel level) {
		return Math.max(0, (endsAt - level.getGameTime()) / 20);
	}

	/** Records {@code voter}'s vote for {@code contestant}. */
	public Vote vote(UUID voter, UUID contestant) {
		if (!open() || !contestants.containsKey(contestant)) {
			return Vote.NOT_IN_ROUND;
		}
		if (voter.equals(contestant)) {
			return Vote.OWN;
		}
		UUID before = votes.put(voter, contestant);
		setChanged();
		return before == null ? Vote.VOTED : before.equals(contestant) ? Vote.SAME : Vote.MOVED;
	}

	/** Closes the round: the contestants with the most votes (at least one) who are still here get a ribbon. */
	public void finish(ServerLevel level) {
		int best = 0;
		for (UUID contestant : contestants.keySet()) {
			best = Math.max(best, votesFor(contestant));
		}
		List<String> winners = new ArrayList<>();
		if (best > 0) {
			for (Map.Entry<UUID, String> contestant : contestants.entrySet()) {
				if (votesFor(contestant.getKey()) != best) {
					continue;
				}
				winners.add(contestant.getValue());
				ServerPlayer winner = level.getServer().getPlayerList().getPlayer(contestant.getKey());
				if (winner != null) {
					ItemStack ribbon = new ItemStack(JugcraftAgriculture.item("best_costume_ribbon"));
					if (!winner.getInventory().add(ribbon)) {
						winner.spawnAtLocation(winner.level(), ribbon);
					}
				}
			}
		}
		endsAt = 0;
		contestants.clear();
		votes.clear();
		setChanged();
		Set<BlockPos> open = OPEN.get(level);
		if (open != null) {
			open.remove(worldPosition);
		}
		if (getBlockState().getValue(JudgesTableBlock.OPEN)) {
			level.setBlock(worldPosition, getBlockState().setValue(JudgesTableBlock.OPEN, false), Block.UPDATE_ALL);
		}
		level.playSound(null, worldPosition, SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 1.0F, 0.9F);
		announce(level, winners.isEmpty() ? Component.translatable("message.jugcraft.judges_table.no_winner")
				: Component.translatable("message.jugcraft.judges_table.winner", String.join(", ", winners)));
	}

	private void announce(ServerLevel level, Component message) {
		for (ServerPlayer player : level.players()) {
			if (player.distanceToSqr(Vec3.atCenterOf(worldPosition)) <= (double) ANNOUNCE_RANGE * ANNOUNCE_RANGE) {
				player.sendSystemMessage(message);
			}
		}
	}

	/**
	 * A player using another with an empty hand votes for them, if they are a contestant at an open table within
	 * range; anything else is left alone (Fabric's UseEntityCallback, on the server).
	 */
	public static InteractionResult onUseEntity(Player player, Level level, InteractionHand hand, Entity entity) {
		if (level.isClientSide() || hand != InteractionHand.MAIN_HAND || !(entity instanceof ServerPlayer contestant)
				|| !(player instanceof ServerPlayer voter) || !voter.getMainHandItem().isEmpty()) {
			return InteractionResult.PASS;
		}
		Set<BlockPos> open = OPEN.get(level);
		if (open == null) {
			return InteractionResult.PASS;
		}
		for (BlockPos pos : List.copyOf(open)) {
			if (level.getBlockEntity(pos) instanceof JudgesTableBlockEntity table && table.open() && table.inRange(voter)
					&& table.contestants.containsKey(contestant.getUUID())) {
				Vote vote = table.vote(voter.getUUID(), contestant.getUUID());
				voter.sendOverlayMessage(Component.translatable("message.jugcraft.judges_table." + vote.name().toLowerCase(Locale.ROOT),
						contestant.getName()));
				return InteractionResult.SUCCESS;
			}
		}
		return InteractionResult.PASS;
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		endsAt = input.getLongOr("ends_at", 0L);
		contestants.clear();
		input.read("contestants", CONTESTANTS).orElse(List.of()).forEach(entrant -> contestants.put(entrant.id(), entrant.name()));
		votes.clear();
		votes.putAll(input.read("votes", VOTES).orElse(Map.of()));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putLong("ends_at", endsAt);
		List<Entrant> entrants = new ArrayList<>();
		contestants.forEach((id, name) -> entrants.add(new Entrant(id, name)));
		output.store("contestants", CONTESTANTS, entrants);
		output.store("votes", VOTES, votes);
	}
}
