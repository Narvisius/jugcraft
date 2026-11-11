package io.github.jimbozoomer.jugcraft.nuclear;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The entrance hall's security post (older rooms round 6; Justin: "Lobby and entrance"; tools/entrance_hall.py draws it).
 * Each speed gate that opens for someone writes the passage to the nearest post within {@link #REACH} blocks: the
 * time of day, IN through a lane facing north (into the building) or OUT through one facing south, and who. The post
 * keeps the last {@link #KEEP}; its screens show the newest and who is inside (whose last passage was IN), its gate
 * panel lights each lane while it is open (client/EdgTelemetry.java acStation reads the gates), and its duress button
 * sets off the alarm for {@link #DURESS_TICKS}.
 */
public final class AccessControl {
	public static final String STATION = "ac_station", HAND_READER = "ac_hand_reader";
	public static final int REACH = 16, KEEP = 40, DURESS_TICKS = 200;
	/** The duress button on the gate panel (model pixels across the post, up; tools/entrance_hall.py DURESS). */
	static final double DURESS_X = 43.6, DURESS_Y = 13.4, DURESS_R = 1.6;

	private AccessControl() {
	}

	public static boolean isStation(BlockState state) {
		return DcRoom.is(state, STATION);
	}

	public static boolean duressAt(double x, double y) {
		return Math.abs(x - DURESS_X) <= DURESS_R && Math.abs(y - DURESS_Y) <= DURESS_R;
	}

	/** A gate opened for a player: written to the nearest post in reach. */
	public static void passed(ServerLevel level, BlockPos gate, Direction facing, Player player) {
		List<LivePanelBlockEntity> posts = ControlBoard.panels(level, gate, PlantInterior.BLOCKS.get(STATION), REACH);
		if (posts.isEmpty()) {
			return;
		}
		posts.sort(java.util.Comparator.comparingDouble(p -> p.getBlockPos().distSqr(gate)));
		LivePanelBlockEntity post = posts.get(0);
		String dir = facing == Direction.SOUTH ? "OUT" : "IN";
		post.acLog.add(level.getOverworldClockTime() + "|" + dir + "|" + player.getName().getString());
		while (post.acLog.size() > KEEP) {
			post.acLog.remove(0);
		}
		post.changedAndSync(level);
	}

	/** A logged passage: its time of day, IN or OUT, who. */
	public record Entry(long dayTime, String dir, String name) {
		static Entry of(String s) {
			String[] p = s.split("\\|", 3);
			long t = 0;
			try {
				t = Long.parseLong(p[0]);
			} catch (NumberFormatException ignored) {
			}
			return new Entry(t, p.length > 1 ? p[1] : "IN", p.length > 2 ? p[2] : "?");
		}
	}

	public static List<Entry> entries(LivePanelBlockEntity post) {
		List<Entry> out = new ArrayList<>();
		for (String s : post.acLog) {
			out.add(Entry.of(s));
		}
		return out;
	}

	/** Who is inside: each name whose last passage was IN, with when (oldest first). */
	public static List<Entry> inside(LivePanelBlockEntity post) {
		Map<String, Entry> last = new LinkedHashMap<>();
		for (Entry e : entries(post)) {
			last.remove(e.name());
			last.put(e.name(), e);
		}
		List<Entry> out = new ArrayList<>();
		for (Entry e : last.values()) {
			if (e.dir().equals("IN")) {
				out.add(e);
			}
		}
		return out;
	}

	/** The duress button: the alarm for ten seconds, its bell, the word to everyone near. */
	static void duress(ServerLevel level, LivePanelBlockEntity post, Player who) {
		post.acDuressUntil = level.getGameTime() + DURESS_TICKS;
		post.changedAndSync(level);
		level.playSound(null, post.getBlockPos(), SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 1.6F, 0.6F);
		Component msg = Component.translatable("message.jugcraft.nuclear.ac_duress_alarm", who.getName()).withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
		for (ServerPlayer p : level.players()) {
			if (p.blockPosition().distSqr(post.getBlockPos()) <= 48 * 48) {
				p.sendSystemMessage(msg);
			}
		}
	}

	/** What a click on the post says: how many are inside, the last passage. */
	static Component status(LivePanelBlockEntity post) {
		List<Entry> all = entries(post);
		String last = all.isEmpty() ? "none" : TscRoom.clock(all.get(all.size() - 1).dayTime()) + " " + all.get(all.size() - 1).dir() + " "
				+ all.get(all.size() - 1).name();
		return Component.translatable("message.jugcraft.nuclear.ac_status", inside(post).size(), last);
	}
}
