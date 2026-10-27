package io.github.jimbozoomer.jugcraft.client.jimtafacts;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.jimtafacts.network.DigitalPressMenu;
import io.github.jimbozoomer.jugcraft.jimtafacts.network.DigitalPaint;
import io.github.jimbozoomer.jugcraft.jimtafacts.network.DigitalMatterAmmo;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class DigitalEquipmentClient {
    private DigitalEquipmentClient() {}
    public static void register() {
        io.github.jimbozoomer.jugcraft.jimtafacts.network.DigitalGuide.open = () -> io.github.jimbozoomer.jugcraft.client.EncyclopediaClient.open(net.minecraft.client.Minecraft.getInstance(), io.github.jimbozoomer.jugcraft.client.EncyclopediaScreen.Back.GAME);
        MenuScreens.register(DigitalPressMenu.TYPE, PressScreen::new);
        DigitalUtilityClient.register();
        DigitalFacadeRenderer.register();
        TransportAttachmentRenderer.register();
        net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry.register(java.util.List.of(new net.minecraft.client.color.block.BlockTintSource() {
            public int color(net.minecraft.world.level.block.state.BlockState state) { return 0xFF000000 | DigitalMatterAmmo.colors(state.getValue(DigitalPaint.COLOR)).get(state.getValue(DigitalPaint.LUMEN) ? 2 : 1); }
            public java.util.Set<net.minecraft.world.level.block.state.properties.Property<?>> relevantProperties() { return java.util.Set.of(DigitalPaint.COLOR, DigitalPaint.LUMEN); }
        }), DigitalPaint.PAINT);
    }
    public static final class PressScreen extends AbstractContainerScreen<DigitalPressMenu.Menu> {
        private EditBox name; private int debounce;
        public PressScreen(DigitalPressMenu.Menu menu, Inventory inv, Component title) { super(menu, inv, title, 176, 176); }
        @Override protected void init() {
            super.init(); titleLabelY = 10000; inventoryLabelY = 10000;
            name = addRenderableWidget(new EditBox(font, leftPos + 12, topPos + 24, 152, 18, Component.literal("Name to engrave")));
            name.setMaxLength(256); name.setHint(Component.literal("Name to engrave")); name.setResponder(value -> debounce = 4);
        }
        @Override protected void containerTick() { super.containerTick(); if (debounce > 0 && --debounce == 0) ClientPlayNetworking.send(new DigitalPressMenu.Name(menu.containerId, name.getValue())); }
        @Override public void extractRenderState(GuiGraphicsExtractor g, int x, int y, float delta) { extractBackground(g, x, y, delta); super.extractRenderState(g, x, y, delta); extractTooltip(g, x, y); }
        @Override public void extractBackground(GuiGraphicsExtractor g, int x, int y, float delta) {
            g.fill(0, 0, width, height, 0xB0091017);
            g.blit(RenderPipelines.GUI_TEXTURED, Jugcraft.id("textures/jimtafacts/ae2/guis/quartzknife.png"), leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
            g.text(font, "Engrave a name press", leftPos + 10, topPos + 7, 0xFF202631, false);
            g.text(font, "1 ingot + knife durability", leftPos + 10, topPos + 77, 0xFF202631, false);
            // Slot positions stay clear of source labels even when the font/resource pack changes.
            for (var s : menu.slots) { g.fill(leftPos + s.x - 1, topPos + s.y - 1, leftPos + s.x + 17, topPos + s.y + 17, 0xFF6D737B); g.fill(leftPos + s.x, topPos + s.y, leftPos + s.x + 16, topPos + s.y + 16, 0xFF262D35); }
        }
        @Override protected void extractLabels(GuiGraphicsExtractor g, int x, int y) {}
    }
}
