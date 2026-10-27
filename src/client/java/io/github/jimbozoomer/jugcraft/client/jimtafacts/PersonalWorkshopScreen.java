package io.github.jimbozoomer.jugcraft.client.jimtafacts;

import io.github.jimbozoomer.jugcraft.client.ExchangeScreen.Plate;
import io.github.jimbozoomer.jugcraft.client.FittedContainerScreen;
import io.github.jimbozoomer.jugcraft.client.TradeLook;
import io.github.jimbozoomer.jugcraft.jimtafacts.industry.PersonalWorkshops;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * The personal safe and the personal shops (the barter station and the energy vendor), in the trade look
 * (docs/features/trade-look.md). A shop shows what you pay and what you get as samples on slate, the stock or charge
 * and its trades on a second slate, a brass key to trade, its stock and payments, and its state, over the inventory;
 * its owner also has the publish and charge keys. Laid out at 176 x 222 (the safe) or 236 (a shop) and scaled down to
 * fit a smaller window.
 */
public final class PersonalWorkshopScreen extends FittedContainerScreen<PersonalWorkshops.Menu> {
    private static final String[] STATUS = {"Ready", "Private / paused", "Not enough payment", "Payments full", "Out of stock", "Inventory full",
            "Needs power / held item"};
    private Plate buy, publish, charge;

    public PersonalWorkshopScreen(PersonalWorkshops.Menu menu, Inventory inv, Component title) { super(menu, inv, title, 176, menu.kind.equals("personal_safe") ? 222 : 236); }

    private boolean safe() { return menu.kind.equals("personal_safe"); }

    private void action(int id) { if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id); }

    @Override protected void init() {
        super.init(); titleLabelY = inventoryLabelY = 10000;
        if (safe()) return;
        buy = addRenderableWidget(new Plate(leftPos + 82, topPos + 45, 84, 19, "Trade", Plate.Kind.BRASS, b -> action(0)));
        buy.setTooltip(Tooltip.create(Component.literal("One exchange. Payment, stock and space are checked before anything moves. Charging fills your selected hotbar item.")));
        publish = addRenderableWidget(new Plate(leftPos + 8, topPos + 213, 78, 18, "", Plate.Kind.WOOD, b -> action(1)));
        publish.setTooltip(Tooltip.create(Component.literal("Publish to let visitors buy, including inside your claim. Stock remains private; hoppers cannot extract it.")));
        charge = addRenderableWidget(new Plate(leftPos + 90, topPos + 213, 78, 18, "", Plate.Kind.WOOD, b -> action(2)));
        charge.setTooltip(Tooltip.create(Component.literal("JE delivered for one full payment. Requires that much stored energy and free space in the customer's held item.")));
        refresh();
    }

    @Override protected void containerTick() { super.containerTick(); refresh(); }

    private void refresh() {
        if (buy == null) return;
        buy.active = menu.data.get(1) == 1;
        buy.setMessage(Component.literal(menu.kind.equals("energy_vendor") ? "Buy charge" : "Trade"));
        publish.visible = menu.data.get(0) == 1;
        publish.setMessage(Component.literal(menu.data.get(1) == 1 ? "Published" : "Private"));
        charge.visible = menu.data.get(0) == 1 && menu.kind.equals("energy_vendor");
        charge.setMessage(Component.literal(PersonalWorkshops.CHARGES[Math.clamp(menu.data.get(2), 0, 2)] + " JE"));
    }

    @Override protected void background(GuiGraphicsExtractor g, int mx, int my, float dt) {
        int x = leftPos, y = topPos;
        TradeLook.frame(g, x, y, x + imageWidth, y + imageHeight);
        g.text(font, title, x + 8, y + 5, TradeLook.GOLD, false);
        TradeLook.rule(g, x + 6, y + 15, x + imageWidth - 6);
        if (safe()) {
            // The safe's 54 slots, and the inventory.
            TradeLook.tray(g, x + 6, y + 16, x + 170, y + 127);
            TradeLook.tray(g, x + 6, y + 137, x + 170, y + 217);
            TradeLook.slots(g, x, y, menu);
            return;
        }
        // What you pay and what you get, the stock or charge and its trades, the stock and payments, the inventory.
        TradeLook.tray(g, x + 6, y + 16, x + 72, y + 71);
        TradeLook.tray(g, x + 74, y + 16, x + 170, y + 42);
        TradeLook.tray(g, x + 6, y + 72, x + 170, y + 111);
        TradeLook.tray(g, x + 6, y + 125, x + 170, y + 205);
        TradeLook.slots(g, x, y, menu);
        g.text(font, "Pay", x + 12, y + 23, TradeLook.CREAM, false);
        g.text(font, "Get", x + 12, y + 57, TradeLook.CREAM, false);
        if (menu.kind.equals("energy_vendor")) {
            g.text(font, PersonalWorkshops.CHARGES[Math.clamp(menu.data.get(2), 0, 2)] + " JE", x + 79, y + 20, TradeLook.GOLD, false);
            g.text(font, (menu.data.get(3) * 100) + " stored", x + 79, y + 31, TradeLook.CREAM, false);
        } else {
            g.text(font, "Stock / payments", x + 79, y + 20, TradeLook.CREAM, false);
            g.text(font, "Trades: " + menu.data.get(5), x + 79, y + 31, TradeLook.GOLD, false);
        }
        int status = Math.clamp(menu.data.get(4), 0, STATUS.length - 1);
        boolean sampling = menu.data.get(0) == 1 && mx >= x + 49 && mx < x + 67 && my >= y + 18 && my < y + 70;
        g.text(font, sampling ? "Cursor stack sets sample" : STATUS[status], x + 8, y + 114,
                sampling ? TradeLook.TAN : status == 0 ? TradeLook.GAIN : status == 1 ? TradeLook.DIM : TradeLook.LOSS, false);
    }

    @Override protected void extractLabels(GuiGraphicsExtractor g, int x, int y) {}
}
