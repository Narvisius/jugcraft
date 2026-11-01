package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.guide.encyclopedia.*;
import io.github.jimbozoomer.jugcraft.town.*;
import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;

public final class StarterGuidanceGameTests {
    @GameTest public void fullLibraryAndLiveRecipesSurviveTheWire(GameTestHelper h) {
        var player = TestPlayers.mock(h);
        var request = new EncyclopediaRequestPayload(17, Optional.empty());
        var sent = Encyclopedia.build(player, request, PageWriter::of);
        var ids = sent.pages().stream().map(EncyclopediaPayload.Page::id).toList();
        h.assertTrue(ids.containsAll(StarterGuides.IDS), "Every starter route is available to a newcomer");
        h.assertTrue(ids.containsAll(List.of("nouveau", "jimta_network", "jimta_guides", "caverns")), "Late registered guides are not truncated");
        var tech = sent.pages().stream().filter(p -> p.id().equals("technology_start")).findFirst().orElseThrow();
        h.assertTrue(tech.rows().stream().filter(r -> r.grid().isPresent()).count() >= 5, "Starter recipes are read from the actual server");
        var buf = new RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), h.getLevel().registryAccess());
        try {
            EncyclopediaPayload.CODEC.encode(buf, sent);
            var received = EncyclopediaPayload.CODEC.decode(buf);
            h.assertValueEqual(received.pages().stream().map(EncyclopediaPayload.Page::id).toList(), ids, "Every page round trips");
        } finally { buf.release(); }
        h.succeed();
    }
    @GameTest public void removedRecipeIsUnavailableAndCountsArePersonal(GameTestHelper h) {
        var player = TestPlayers.mock(h);
        player.getInventory().add(new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(io.github.jimbozoomer.jugcraft.Jugcraft.id("bronze_ingot")), 2));
        var out = new PageWriter(player, id -> Optional.empty());
        StarterGuides.write("technology_start", player, out);
        h.assertTrue(out.rows().stream().noneMatch(r -> r.grid().isPresent()), "No stale baked crafting grids");
        h.assertTrue(out.rows().stream().anyMatch(r -> r.stateOf() == EncyclopediaPayload.StepState.BLOCKED), "Missing recipes are called unavailable");
        var other = TestPlayers.mock(h);
        var otherOut = new PageWriter(other, id -> Optional.empty());
        StarterGuides.write("technology_start", other, otherOut);
        java.util.function.Function<PageWriter, Object> bronzeCount = writer -> writer.rows().stream()
            .filter(r -> r.text().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t
                && t.getKey().equals("encyclopedia.jugcraft.have_now"))
            .map(r -> ((net.minecraft.network.chat.contents.TranslatableContents)r.text().getContents()).getArgs()[1]).findFirst().orElseThrow();
        h.assertValueEqual(bronzeCount.apply(out), 2, "Reader sees their carried bronze");
        h.assertValueEqual(bronzeCount.apply(otherOut), 0, "Other player does not inherit the inventory checklist");
        h.succeed();
    }
    @GameTest public void mentorIsFreeAndRejectsRemoteAndRepeatedRequests(GameTestHelper h) {
        var player = TestPlayers.mock(h); player.setGameMode(GameType.SURVIVAL);
        var npc = h.spawn(JugcraftTown.TOWNSFOLK, new BlockPos(1, 1, 1));
        npc.setNoAi(true);
        var spot = TownData.get().spots.stream().filter(s -> "general".equals(s.shop())).findFirst().orElseThrow();
        npc.setUp(spot, npc.blockPosition());
        player.setPos(npc.position().add(1, 0, 0));
        player.getInventory().add(new ItemStack(Items.DIAMOND, 3));
        long balance = Jugs.balance(h.getLevel().getServer(), player.getUUID());
        var menu = ShopMenu.serve(2, player, npc, false);
        h.assertTrue(menu.clickMenuButton(player, TownMentors.LESSON_BUTTON), "Shop mentor button opens a lesson");
        h.assertFalse(menu.clickMenuButton(player, TownMentors.LESSON_BUTTON), "Spam is bounded");
        h.assertValueEqual(player.getInventory().countItem(Items.DIAMOND), 3, "Lesson consumes no items");
        h.assertTrue(balance == Jugs.balance(h.getLevel().getServer(), player.getUUID()), "Lesson changes no Jugs");
        RateGate.forget(player.getUUID()); player.setPos(npc.position().add(100, 0, 0));
        h.assertFalse(menu.clickMenuButton(player, TownMentors.LESSON_BUTTON), "Remote menu cannot consult");
        npc.discard(); h.succeed();
    }
    @GameTest public void mentorsReuseExistingResidentsAndNeverTurnGuardsIntoTeachers(GameTestHelper h) {
        for (String shop : List.of("general", "florist", "curios"))
            h.assertTrue(TownData.get().spots.stream().anyMatch(s -> shop.equals(s.shop())), "Existing mentor resident: " + shop);
        h.assertTrue(TownMentors.shopLesson("weapons").isEmpty(), "Unassigned shops keep their normal behavior");
        h.succeed();
    }
}
