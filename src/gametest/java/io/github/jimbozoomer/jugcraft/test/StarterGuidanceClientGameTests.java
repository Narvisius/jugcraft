package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.client.*;
import io.github.jimbozoomer.jugcraft.town.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntitySpawnReason;

/** Real mentor menu -> packet -> fresh server pages -> same Encyclopedia, then both route directories. */
public final class StarterGuidanceClientGameTests implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext c) {
        try (var world = c.worldBuilder().create()) {
            world.getConnection().waitForChunksRender();
            c.getInput().pressKey(EncyclopediaClient.key());
            c.waitForScreen(EncyclopediaScreen.class);
            c.waitFor(mc -> EncyclopediaClient.navigator().pages().stream().anyMatch(p -> p.id().equals("jimta_guides")));
            c.takeScreenshot("encyclopedia_unified_home");
            c.clickScreenButton("ui.jugcraft.book.technology");
            c.waitForScreen(EncyclopediaRoutesScreen.class);
            c.waitTicks(3); c.takeScreenshot("encyclopedia_technology_start");
            c.clickScreenButton("ui.jugcraft.book.read");
            c.waitForScreen(EncyclopediaScreen.class);
            c.waitFor(mc -> ((EncyclopediaScreen)mc.gui.screen()).page().equals("technology_start"));
            c.waitTicks(3); c.takeScreenshot("encyclopedia_first_machine_lesson");
            c.runOnClient(mc -> ((EncyclopediaScreen)mc.gui.screen()).showSectionContaining(
                net.minecraft.network.chat.Component.literal("1. Prepare bronze and the casing")));
            c.waitTicks(3); c.takeScreenshot("encyclopedia_starter_recipe");
            c.runOnClient(mc -> {
                var screen = (EncyclopediaScreen)mc.gui.screen();
                var body = screen.children().stream()
                    .filter(net.minecraft.client.gui.components.AbstractContainerWidget.class::isInstance)
                    .map(net.minecraft.client.gui.components.AbstractContainerWidget.class::cast)
                    .filter(area -> area.children().stream().anyMatch(child ->
                        child instanceof net.minecraft.client.gui.components.AbstractWidget widget
                            && widget.getMessage().getString().contains("bronze blend")))
                    .findFirst().orElseThrow(() -> new AssertionError("Starter recipe is not in the scrolling lesson"));
                var row = body.children().stream()
                    .filter(net.minecraft.client.gui.components.AbstractWidget.class::isInstance)
                    .map(net.minecraft.client.gui.components.AbstractWidget.class::cast)
                    .filter(widget -> widget.getMessage().getString().contains("bronze blend"))
                    .findFirst().orElseThrow();
                body.setScrollAmount(body.scrollAmount() + row.getY() - body.getY());
            });
            c.waitTicks(3); c.takeScreenshot("encyclopedia_starter_recipe_grids");
            c.runOnClient(mc -> ((EncyclopediaScreen)mc.gui.screen()).showContents());
            c.waitTicks(2); c.clickScreenButton("ui.jugcraft.book.magic");
            c.waitForScreen(EncyclopediaRoutesScreen.class);
            c.waitTicks(3); c.takeScreenshot("encyclopedia_magic_start");
            c.clickScreenButton("ui.jugcraft.book.read");
            c.waitForScreen(EncyclopediaScreen.class);
            c.waitFor(mc -> ((EncyclopediaScreen)mc.gui.screen()).page().equals("magic_start"));
            c.waitTicks(3); c.takeScreenshot("encyclopedia_magic_lesson");
            c.setScreen(() -> null);
            for (String shop : java.util.List.of("general", "florist", "curios")) {
                world.getServer().runOnServer(server -> {
                    var player = server.getPlayerList().getPlayers().getFirst();
                    var npc = JugcraftTown.TOWNSFOLK.create(player.level(), EntitySpawnReason.COMMAND);
                    var spot = TownData.get().spots.stream().filter(s -> shop.equals(s.shop())).findFirst().orElseThrow();
                    npc.setUp(spot, player.blockPosition()); npc.setNoAi(true);
                    npc.setPos(player.position().add(1, 0, 0)); player.level().addFreshEntity(npc);
                    ShopMenu.open(player, npc);
                });
                c.waitForScreen(ShopScreen.class); c.waitTicks(3);
                c.takeScreenshot("mentor_shop_" + shop);
                c.clickScreenButton("screen.jugcraft.shop.mentor");
                c.waitForScreen(EncyclopediaScreen.class);
                c.waitFor(mc -> ((EncyclopediaScreen)mc.gui.screen()).page().equals(TownMentors.shopLesson(shop)));
                c.waitTicks(3); c.takeScreenshot("mentor_lesson_" + shop);
                c.setScreen(() -> null);
            }
        }
    }
}
