package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A Spirit Board's séance ({@link SpiritBoard}): whose fingers are on the planchette, what it is spelling and how far it
 * has got, and where the planchette is going (sent to clients, which slide it there). Only the server decides what is
 * spelled; the players' part is to touch the planchette and stay near it.
 */
public class SpiritBoardBlockEntity extends BlockEntity {
	private final List<UUID> hands = new ArrayList<>();
	/** The stops still to come are {@code spelling} from {@code step} on; empty when no séance is going on. */
	private String spelling = "";
	private int step;
	private long nextAt;
	private long restUntil;
	/** What has been spelled so far, as the players are shown it. */
	private String spelled = "";
	/** The spirit answering, by its name and wish (-1 for none). */
	private int name = -1;
	private int wish = -1;
	/** The planchette: sliding from one stop to another, starting at {@code moveStart} and taking {@code moveTicks}. */
	private char from = SpiritBoard.REST;
	private char to = SpiritBoard.REST;
	private long moveStart;
	private int moveTicks = 1;

	public SpiritBoardBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftAgriculture.SPIRIT_BOARD_ENTITY, pos, state);
	}

	public boolean active() {
		return !spelling.isEmpty();
	}

	public List<UUID> hands() {
		return List.copyOf(hands);
	}

	public String spelled() {
		return spelled;
	}

	public char from() {
		return from;
	}

	public char to() {
		return to;
	}

	public long moveStart() {
		return moveStart;
	}

	public int moveTicks() {
		return moveTicks;
	}

	/** Ticks between stops with {@code hands} players' fingers on the planchette. */
	public static int interval(int hands) {
		return hands >= 2 ? SpiritBoard.FAST_LETTER_TICKS : SpiritBoard.LETTER_TICKS;
	}

	private Vec3 centre() {
		return Vec3.atCenterOf(worldPosition);
	}

	private boolean near(Player player, double range) {
		return player.isAlive() && !player.isSpectator() && player.level() == level && player.position().distanceToSqr(centre()) <= range * range;
	}

	/**
	 * {@code player} rests their fingers on the planchette: joins the séance going on, or starts one if a candle is lit near
	 * and the board isn't resting from the last. Returns whether their fingers are on it now.
	 */
	public boolean touch(ServerPlayer player) {
		if (!(level instanceof ServerLevel server) || !near(player, SpiritBoard.HAND_RANGE)) {
			message(player, "too_far");
			return false;
		}
		if (hands.contains(player.getUUID())) {
			message(player, "already");
			return true;
		}
		if (active()) {
			if (hands.size() >= SpiritBoard.MAX_HANDS) {
				message(player, "crowded");
				return false;
			}
			hands.add(player.getUUID());
			message(player, "joined");
			changed();
			return true;
		}
		long now = server.getGameTime();
		if (now < restUntil) {
			message(player, "resting");
			return false;
		}
		if (!SpiritBoard.candlelit(server, worldPosition)) {
			message(player, "dark");
			return false;
		}
		hands.add(player.getUUID());
		RestlessSpirit spirit = server.getEntitiesOfClass(RestlessSpirit.class, new AABB(worldPosition).inflate(SpiritBoard.SPIRIT_RANGE)).stream()
				.min(Comparator.comparingDouble(found -> found.distanceToSqr(centre()))).orElse(null);
		if (spirit != null) {
			spirit.promise(server.getRandom());
			name = spirit.spiritName();
			wish = spirit.wish() == null ? -1 : spirit.wish().ordinal();
		} else {
			name = -1;
			wish = -1;
		}
		spelling = name >= 0 && wish >= 0 ? SpiritBoard.message(name, SpiritBoard.Wish.values()[wish]) : SpiritBoard.nobody();
		step = 0;
		spelled = "";
		nextAt = now + 10;
		server.playSound(null, worldPosition, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.6F, 0.5F);
		changed();
		return true;
	}

	/** Comparators: 0 at rest; 15 on YES, 1 on NO, 4 on GOODBYE, 8 on a letter or number. */
	public int signal() {
		if (!active()) {
			return 0;
		}
		return switch (to) {
			case SpiritBoard.YES -> 15;
			case SpiritBoard.NO -> 1;
			case SpiritBoard.GOODBYE -> 4;
			case SpiritBoard.REST -> 0;
			default -> 8;
		};
	}

	void serverTick(ServerLevel server) {
		if (!active()) {
			return;
		}
		long now = server.getGameTime();
		// Fingers come off the planchette when their player wanders off (or leaves, or dies).
		hands.removeIf(id -> !(server.getPlayerByUUID(id) instanceof ServerPlayer player) || !near(player, SpiritBoard.HAND_RANGE + 1));
		if (hands.isEmpty()) {
			show(server, Component.translatable("message.jugcraft.spirit_board.broken"));
			finish(server, now);
			return;
		}
		if (now < nextAt) {
			return;
		}
		if (step >= spelling.length()) {
			if (name >= 0 && wish >= 0) {
				SpiritBoard.Wish wished = SpiritBoard.Wish.values()[wish];
				show(server, Component.translatable("message.jugcraft.spirit_board.wish", spiritName(name),
						Component.translatable("spirit_wish.jugcraft." + wished.key())));
			}
			finish(server, now);
			return;
		}
		char stop = spelling.charAt(step++);
		int interval = interval(hands.size());
		slide(stop, now, Math.max(4, interval - 6));
		spelled = switch (stop) {
			case SpiritBoard.YES -> "YES";
			case SpiritBoard.NO -> "NO";
			case SpiritBoard.GOODBYE -> spelled + " … GOODBYE";
			case SpiritBoard.REST -> spelled.endsWith(" ") ? spelled : spelled + " ";
			default -> spelled + stop;
		};
		if (stop == SpiritBoard.YES) {
			for (UUID id : hands) {
				if (server.getPlayerByUUID(id) instanceof ServerPlayer player) {
					TrickOrTreat.award(player, "is_anybody_there");
				}
			}
		}
		if (stop != SpiritBoard.REST) {
			server.playSound(null, worldPosition, SoundEvents.WOOD_HIT, SoundSource.BLOCKS, 0.3F, 1.6F);
		}
		show(server, Component.translatable("message.jugcraft.spirit_board.spells", spelled));
		nextAt = now + interval;
		changed();
	}

	/** A spirit's name as a person would write it ("Mabel"). */
	public static String spiritName(int name) {
		String upper = SpiritBoard.NAMES[name];
		return upper.charAt(0) + upper.substring(1).toLowerCase(java.util.Locale.ROOT);
	}

	private void finish(ServerLevel server, long now) {
		hands.clear();
		spelling = "";
		step = 0;
		restUntil = now + SpiritBoard.COOLDOWN_TICKS;
		slide(SpiritBoard.REST, now, 20);
		changed();
	}

	private void slide(char stop, long now, int ticks) {
		from = to;
		to = stop;
		moveStart = now;
		moveTicks = Math.max(1, ticks);
	}

	/** Shows {@code text} to everyone within {@value SpiritBoard#WATCH_RANGE} blocks (and to fingers on the planchette). */
	private void show(ServerLevel server, Component text) {
		for (ServerPlayer player : server.players()) {
			if (hands.contains(player.getUUID()) || near(player, SpiritBoard.WATCH_RANGE)) {
				player.sendOverlayMessage(text);
			}
		}
	}

	private static void message(ServerPlayer player, String key) {
		player.sendOverlayMessage(Component.translatable("message.jugcraft.spirit_board." + key));
	}

	private void changed() {
		setChanged();
		if (level != null) {
			BlockState state = getBlockState();
			level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
			level.updateNeighbourForOutputSignal(worldPosition, state.getBlock());
		}
	}

	private static char stop(String saved) {
		return saved.length() == 1 && SpiritBoard.place(saved.charAt(0)) != null ? saved.charAt(0) : SpiritBoard.REST;
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		hands.clear();
		input.read("hands", UUIDUtil.STRING_CODEC.listOf()).ifPresent(list -> hands.addAll(list.subList(0, Math.min(list.size(), SpiritBoard.MAX_HANDS))));
		spelling = input.getStringOr("spelling", "");
		step = Math.clamp(input.getIntOr("step", 0), 0, spelling.length());
		nextAt = input.getLongOr("next_at", 0L);
		restUntil = input.getLongOr("rest_until", 0L);
		spelled = input.getStringOr("spelled", "");
		name = input.getIntOr("name", -1);
		wish = input.getIntOr("wish", -1);
		if (name < -1 || name >= SpiritBoard.NAMES.length || wish < -1 || wish >= SpiritBoard.Wish.values().length) {
			name = -1;
			wish = -1;
		}
		from = stop(input.getStringOr("from", " "));
		to = stop(input.getStringOr("to", " "));
		moveStart = input.getLongOr("move_start", 0L);
		moveTicks = Math.max(1, input.getIntOr("move_ticks", 1));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.store("hands", UUIDUtil.STRING_CODEC.listOf(), List.copyOf(hands));
		output.putString("spelling", spelling);
		output.putInt("step", step);
		output.putLong("next_at", nextAt);
		output.putLong("rest_until", restUntil);
		output.putString("spelled", spelled);
		output.putInt("name", name);
		output.putInt("wish", wish);
		output.putString("from", String.valueOf(from));
		output.putString("to", String.valueOf(to));
		output.putLong("move_start", moveStart);
		output.putInt("move_ticks", moveTicks);
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return saveCustomOnly(registries);
	}

	/** For tests and screenshots: the planchette sliding from {@code from} to {@code to}, starting at {@code moveStart}. */
	public void setPlanchette(char from, char to, long moveStart, int moveTicks) {
		this.from = from;
		this.to = to;
		this.moveStart = moveStart;
		this.moveTicks = Math.max(1, moveTicks);
		changed();
	}
}
