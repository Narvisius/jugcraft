package io.github.jimbozoomer.jugcraft.town;

import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import io.github.jimbozoomer.jugcraft.guide.encyclopedia.MentorOpenPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Existing residents teach the shared guide without changing identities, buildings, prices or progression. */
public final class TownMentors {
    public static final int LESSON_BUTTON = 32760;
    private TownMentors() {}
    public static String shopLesson(String shop) {
        return switch (shop) {
            case "general" -> "technology_start";
            case "florist" -> "magic_start";
            case "curios" -> "alchemy_start";
            default -> "";
        };
    }
    public static String lesson(Townsfolk npc) {
        return switch (npc.role()) {
            case "shopkeeper" -> shopLesson(npc.shop());
            case "mayor" -> "mentors";
            case "academy_radiance" -> "magic_start";
            default -> "";
        };
    }
    public static boolean consult(ServerPlayer player, Townsfolk npc) {
        String lesson = lesson(npc);
        double reach = TownShops.get().tradeRange;
        if (lesson.isEmpty() || !npc.isAlive() || npc.isSleeping() || player.isSpectator()
                || player.level() != npc.level() || npc.distanceToSqr(player) > reach * reach
                || !npc.hasLineOfSight(player) || !RateGate.allow(player, "town_mentor", 10)) return false;
        player.sendSystemMessage(Component.translatable("message.jugcraft.mentor.lesson", npc.getDisplayName(),
                Component.translatable("encyclopedia.jugcraft.page." + lesson)));
        if (ServerPlayNetworking.canSend(player, MentorOpenPayload.TYPE)) {
            player.closeContainer();
            ServerPlayNetworking.send(player, new MentorOpenPayload(lesson));
        }
        return true;
    }
}
