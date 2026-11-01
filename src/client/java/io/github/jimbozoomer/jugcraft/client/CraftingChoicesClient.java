package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.crafting.CraftingChoices;
import io.github.jimbozoomer.jugcraft.mixin.client.CraftingScreenAccess;
import java.util.*;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.fabricmc.fabric.api.client.screen.v1.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Small result-slot button; choosing only asks the server to change the normal crafting preview. */
public final class CraftingChoicesClient {
    private static final Map<AbstractContainerMenu, CraftingChoices.Options> OPTIONS = new WeakHashMap<>();
    private static final Map<Screen, Button> BUTTONS = new WeakHashMap<>();
    private CraftingChoicesClient() { }
    public static CraftingChoices.Options options(AbstractContainerMenu menu) { return OPTIONS.get(menu); }
    public static Button button(Screen screen) { return BUTTONS.get(screen); }
    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(CraftingChoices.Options.TYPE, (packet, context) -> {
            var player = context.client().player;
            if (player != null && player.containerMenu.containerId == packet.menu()) OPTIONS.put(player.containerMenu, packet);
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> { OPTIONS.clear(); BUTTONS.clear(); });
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (!(screen instanceof AbstractContainerScreen<?> container) || screen instanceof CreativeModeInventoryScreen
                    || !CraftingChoices.supported(container.getMenu())) return;
            var grid = CraftingChoices.grid(container.getMenu());
            if (grid == null) return;
            var button = Button.builder(Component.translatable("screen.jugcraft.crafting.choose"), pressed -> {
                var options = OPTIONS.get(container.getMenu());
                if (options != null && options.entries().size() > 1) client.gui.setScreen(new Picker(container, options));
            }).bounds(0, 0, 28, 12).build();
            button.setTooltip(Tooltip.create(Component.translatable("screen.jugcraft.crafting.choose")));
            button.visible = false; Screens.getWidgets(screen).add(button); BUTTONS.put(screen, button);
            ScreenEvents.remove(screen).register(shown -> BUTTONS.remove(shown));
            ScreenEvents.beforeExtract(screen).register((shown, graphics, x, y, delta) -> {
                var slot = container.getMenu().slots.get(grid.slot());
                var origin = (CraftingScreenAccess)container;
                button.setPosition(origin.jugcraft$left() + slot.x - 6, origin.jugcraft$top() + slot.y + 20);
                var options = OPTIONS.get(container.getMenu());
                button.visible = slot.isActive() && options != null && options.entries().size() > 1;
                button.active = button.visible && container.getMenu().getCarried().isEmpty();
                if (options != null) button.setMessage(Component.literal(options.entries().size() + " >"));
            });
            if (ClientPlayNetworking.canSend(CraftingChoices.Request.TYPE))
                ClientPlayNetworking.send(new CraftingChoices.Request(container.getMenu().containerId, 0, ""));
        });
    }
    public static final class Picker extends Screen {
        private final AbstractContainerScreen<?> parent;
        private final CraftingChoices.Options options;
        private int page, rows, left, top;
        private final List<CraftingChoices.Option> visible = new ArrayList<>();
        private Picker(AbstractContainerScreen<?> parent, CraftingChoices.Options options) {
            super(Component.translatable("screen.jugcraft.crafting.choose")); this.parent = parent; this.options = options;
        }
        @Override protected void init() {
            rows = Math.max(1, Math.min(options.entries().size(), Math.min(6, (height - 100) / 26)));
            left = (width - 294) / 2; top = (height - (rows * 26 + 72)) / 2;
            page = Math.clamp(page, 0, (options.entries().size() - 1) / rows);
            visible.clear();
            for (int i = 0; i < rows && page * rows + i < options.entries().size(); i++) {
                var option = options.entries().get(page * rows + i); visible.add(option);
                var name = option.output().getHoverName().copy().append(" x" + option.output().getCount());
                var button = Button.builder(Component.literal("     " + ScreenKit.fit(font, name.getString(), 235)), pressed -> {
                    ClientPlayNetworking.send(new CraftingChoices.Request(options.menu(), options.revision(), option.recipe().toString()));
                    onClose();
                }).bounds(left + 8, top + 29 + i * 26, 278, 24).build();
                button.setTooltip(Tooltip.create(name.copy().append("\n" + option.recipe())));
                addRenderableWidget(button);
            }
            addRenderableWidget(Button.builder(Component.literal("<"), b -> { page--; rebuildWidgets(); })
                .bounds(left + 8, top + rows * 26 + 38, 24, 20).build()).active = page > 0;
            addRenderableWidget(Button.builder(Component.translatable("gui.back"), b -> onClose())
                .bounds(left + 97, top + rows * 26 + 38, 100, 20).build());
            addRenderableWidget(Button.builder(Component.literal(">"), b -> { page++; rebuildWidgets(); })
                .bounds(left + 262, top + rows * 26 + 38, 24, 20).build()).active = (page + 1) * rows < options.entries().size();
        }
        @Override public boolean isPauseScreen() { return false; }
        @Override public void onClose() { minecraft.gui.setScreen(parent); }
        @Override public void tick() {
            if (minecraft.player == null || minecraft.player.containerMenu != parent.getMenu()) minecraft.gui.setScreen(null);
        }
        @Override public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
            super.extractBackground(g, mouseX, mouseY, delta);
            TradeLook.frame(g, left, top, left + 294, top + rows * 26 + 66);
        }
        @Override public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
            super.extractRenderState(g, mouseX, mouseY, delta);
            g.text(font, title, left + 9, top + 10, TradeLook.CREAM, false);
            for (int i = 0; i < visible.size(); i++) {
                var item = visible.get(i).output();
                g.item(item, left + 12, top + 33 + i * 26);
                g.itemDecorations(font, item, left + 12, top + 33 + i * 26);
            }
        }
    }
}
