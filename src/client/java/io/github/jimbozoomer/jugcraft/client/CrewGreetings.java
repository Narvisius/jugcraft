package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.tower.crew.CrewGreeting;
import io.github.jimbozoomer.jugcraft.tower.crew.TowerCrew;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/**
 * The tower crew greeting this client's player (tower/crew/CrewGreeting decides how): when one is due, the crew
 * member's render state gets the greeting and how far into it they are, and now and then a word appears in this
 * player's chat. Nothing goes to the server; each player sees the crew greet them.
 */
final class CrewGreetings {
	/** Per crew member: when their greeting started, when they last greeted, when they last said a word, the greeting. */
	private static final Map<UUID, long[]> SEEN = new HashMap<>();
	private static long lastWord = Long.MIN_VALUE / 2;

	private CrewGreetings() {
	}

	/** Fills {@code state}'s greeting for {@code crew} and this client's player, starting one when it is due. */
	static void update(TowerCrew crew, TowerCrewRenderer.State state, float partialTick) {
		state.greet = CrewGreeting.NONE;
		Minecraft mc = Minecraft.getInstance();
		Player me = mc.player;
		if (me == null || mc.level == null) {
			return;
		}
		long now = mc.level.getGameTime();
		if (SEEN.size() > 512) {
			SEEN.entrySet().removeIf(e -> now - e.getValue()[1] > CrewGreeting.AGAIN && now - e.getValue()[2] > CrewGreeting.LINE_AGAIN);
		}
		long[] seen = SEEN.computeIfAbsent(crew.getUUID(), k -> new long[] {Long.MIN_VALUE / 2, Long.MIN_VALUE / 2, Long.MIN_VALUE / 2, 0});
		if (now - seen[0] < CrewGreeting.TICKS) {
			state.greet = (int) seen[3];
			state.greetT = now - seen[0] + partialTick;
			return;
		}
		if (now - seen[1] < CrewGreeting.AGAIN || me.isSpectator() || crew.isInvisible()) {
			return;
		}
		double dx = me.getX() - crew.getX(), dz = me.getZ() - crew.getZ();
		if (dx * dx + dz * dz > CrewGreeting.REACH * CrewGreeting.REACH || Math.abs(me.getY() - crew.getY()) > 2.5) {
			return;
		}
		float toward = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90F;
		if (Math.abs(Mth.wrapDegrees(toward - crew.getYHeadRot())) > CrewGreeting.ANGLE) {
			return;
		}
		String job = CrewGreeting.job(crew.skin());
		int kind = CrewGreeting.kind(job, state.act, state.prop, crew.isPassenger());   // (as drawn: security with the rifle nod)
		if (kind == CrewGreeting.NONE) {
			return;
		}
		seen[1] = now;
		if (now - seen[2] >= CrewGreeting.LINE_AGAIN && now - lastWord >= CrewGreeting.ANY_LINE) {
			Random random = new Random(crew.getUUID().getLeastSignificantBits() ^ now);
			int roll = random.nextInt(CrewGreeting.RUDE);
			String key = CrewGreeting.line(mc.level.getOverworldClockTime(), roll, random.nextInt(64));
			me.sendSystemMessage(Component.translatable("message.jugcraft.tower_crew.says", crew.getDisplayName(),
					Component.translatable("crew.jugcraft.job." + job), Component.translatable(key)));
			seen[2] = now;
			lastWord = now;
			if (roll == 0) {
				return;                 // rude: no friendly gesture with it
			}
		}
		seen[0] = now;
		seen[3] = kind;
		state.greet = kind;
		state.greetT = partialTick;
	}
}
