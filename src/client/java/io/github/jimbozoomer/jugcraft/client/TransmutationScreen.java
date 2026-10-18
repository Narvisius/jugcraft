package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.assay.TransmutationMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** Stone tablet with real item sockets; balances and transactions come from the server menu. */
public final class TransmutationScreen extends AbstractContainerScreen<TransmutationMenu> {
    private static final Identifier ART = Jugcraft.id("textures/gui/alchemy/transmutation.png");
    private static final int INK = 0xFFCCDADD, SILVER = 0xFFD5D5DE, VIOLET = 0xFFBD8FE3;
    private ActionButton dissolve, form;
    public TransmutationScreen(TransmutationMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 310, 238);
    }
    private static Component ui(String key, Object... args) { return Component.translatable("screen.jugcraft.alchemy." + key, args); }
    @Override protected void init() {
        super.init();
        dissolve = addRenderableWidget(new ActionButton(leftPos + 24, topPos + 114, 116, ui("dissolve"), b -> press(0)));
        form = addRenderableWidget(new ActionButton(leftPos + 170, topPos + 114, 116, ui("form"), b -> press(1)));
        addRenderableWidget(new ActionButton(leftPos+10,topPos+157,58,ui("patterns"),b->press(2)));
        addRenderableWidget(new ActionButton(leftPos+10,topPos+183,58,ui("gem_controls"),b->{if(minecraft.player!=null)minecraft.player.closeContainer();minecraft.gui.setScreen(new GemAbilitiesScreen());}));
        refresh();
    }
    private void press(int id) {
        if (minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }
    private void refresh() {
        dissolve.active = menu.canDissolve(); form.active = menu.canForm();
        dissolve.confirming = menu.confirming();
        dissolve.setMessage(ui(menu.confirming() ? "confirm" : "dissolve"));
        dissolve.setTooltip(Tooltip.create(ui(menu.canDissolve() ? "dissolve_help" : "input_help")));
        form.setTooltip(Tooltip.create(ui("status_" + menu.formStatus())));
    }
    @Override protected void containerTick() { super.containerTick(); refresh(); }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        extractBackground(g, mx, my, delta);
        super.extractRenderState(g, mx, my, delta);
        extractTooltip(g, mx, my);
    }
    @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float delta){
        int x=leftPos,y=topPos;g.fill(0,0,width,height,0xB0081018);OwnerAlchemyArt.frame(g,x,y,imageWidth,imageHeight);
        OwnerAlchemyArt.panel(g,x+14,y+36,133,75,OwnerAlchemyArt.VIOLET);
        OwnerAlchemyArt.panel(g,x+163,y+36,133,75,OwnerAlchemyArt.TEAL);
        OwnerAlchemyArt.sigil(g,x+64,y+78,22,OwnerAlchemyArt.VIOLET,menu.canDissolve(),delta);
        OwnerAlchemyArt.sigil(g,x+207,y+78,22,OwnerAlchemyArt.TEAL,menu.canForm(),delta);
        OwnerAlchemyArt.flow(g,x+91,x+144,y+78,OwnerAlchemyArt.VIOLET,menu.canDissolve(),delta);
        OwnerAlchemyArt.flow(g,x+235,x+265,y+78,OwnerAlchemyArt.TEAL,menu.canForm(),delta);
        BookArt.icon(g,"jugcraft:philosophers_stone",x+16,y+15);
        OwnerAlchemyArt.panel(g,x+170,y+11,124,21,OwnerAlchemyArt.TEAL);
        OwnerAlchemyArt.panel(g,x+69,y+147,172,81,OwnerAlchemyArt.SILVER);
        for(var slot:menu.slots){
            OwnerAlchemyArt.slot(g,"alchchest",x+slot.x,y+slot.y,11,3);
            if(slot.index<3)OwnerAlchemyArt.socket(g,x+slot.x,y+slot.y,slot.index==0?OwnerAlchemyArt.VIOLET:OwnerAlchemyArt.TEAL);
        }
    }

    @Override protected void extractLabels(GuiGraphicsExtractor g, int mx, int my) {
        text(g, ui("title"), 37, 19, 128, SILVER);
        text(g, ui("balance", menu.balance()), 191, 18, 98, 0xFFE9D2F6);
        text(g, ui("input"), 29, 43, 103, INK);
        text(g, ui("sample_kept"), 165, 43, 83, INK);
        text(g, ui("output"), 257, 51, 36, INK);
        text(g, ui("gain", menu.gain()), 28, 100, 114, INK);
        text(g, ui("cost", menu.cost()), 171, 100, 120, INK);
        text(g, ui("inventory"), 74, 137, 162, SILVER);
    }
    private void text(GuiGraphicsExtractor g, Component text, int x, int y, int width, int color) {
        g.text(font, BookArt.fit(font, text.getString(), width), x, y, color, false);
    }
    private static void arrow(GuiGraphicsExtractor g, int x, int end, int y, int color) {
        g.fill(x, y, end, y + 1, color);
        BookArt.line(g, end - 3, y - 3, end, y, color);
        BookArt.line(g, end - 3, y + 3, end, y, color);
    }
    static void slab(GuiGraphicsExtractor g, int x, int y, int w, int h, int color) {
        g.fill(x + 6, y, x + w - 6, y + h, color);
        g.fill(x + 3, y + 3, x + w - 3, y + h - 3, color);
        g.fill(x, y + 6, x + w, y + h - 6, color);
    }
    /** Native keyboard activation and narration, with a tactile inset metal key. */
    static final class ActionButton extends Button {
        boolean confirming, selected;
        ActionButton(int x, int y, int w, Component title, OnPress action) {
            super(x, y, w, 20, title, action, DEFAULT_NARRATION);
        }
        @Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {
            int x = getX(), y = getY(), w = getWidth();
            boolean lit = active && (isHovered() || isFocused());
            int accent = confirming ? OwnerAlchemyArt.GOLD : selected ? OwnerAlchemyArt.TEAL : OwnerAlchemyArt.VIOLET;
            slab(g,x,y,w,20,0xFF0B141D);
            slab(g,x+1,y+1,w-2,18,lit?0xFF718998:selected?0xFF498E8A:active?0xFF516272:0xFF34404A);
            slab(g,x+2,y+2,w-4,15,lit?0xFF354D5C:selected?0xFF244440:0xFF23313F);
            g.fill(x+6,y+2,x+w-6,y+3,active||selected?accent:0xFF4D5967);
            g.fill(x+6,y+17,x+w-6,y+18,0xFF080F18);
            if(selected||confirming){g.fill(x+3,y+8,x+5,y+12,accent);g.fill(x+w-5,y+8,x+w-3,y+12,accent);}
            if (isFocused()) g.outline(x, y, w, 20, OwnerAlchemyArt.GOLD);
            var font = Minecraft.getInstance().font;
            String label = BookArt.fit(font, getMessage().getString(), w - 10);
            g.text(font, label, x + (w - font.width(label)) / 2, y + 6, active||selected ? 0xFFE7F0F2 : 0xFF8E9BA7, false);
        }
    }
}
