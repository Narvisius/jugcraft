package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.assay.AlchemicalWorkshopMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** A native inventory panel in the existing essence tablet style, with explicit feed/target/output roles. */
public final class AlchemicalWorkshopScreen extends AbstractContainerScreen<AlchemicalWorkshopMenu> {
    private static final Identifier ART = Jugcraft.id("textures/gui/alchemy/transmutation.png");
    private TransmutationScreen.ActionButton fuel;
    private final TransmutationScreen.ActionButton[] buttons = new TransmutationScreen.ActionButton[3];
    public AlchemicalWorkshopScreen(AlchemicalWorkshopMenu menu, Inventory inv, Component title) { super(menu, inv, title, 348, 240); }
    private static Component ui(String key, Object... args) { return Component.translatable("screen.jugcraft.workshop." + key, args); }
    private static String n(long value) { return String.format("%,d", value); }
    @Override protected void init() {
        super.init();
        for (int i = 0; i < 3; i++) {
            final int action = i;
            buttons[i] = addRenderableWidget(new TransmutationScreen.ActionButton(leftPos+21+i*103, topPos+124, 100,
                    ui(i == 0 ? "pause" : i == 1 ? "deposit" : "withdraw"), b -> { if (minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, action); }));
            buttons[i].setTooltip(Tooltip.create(ui(i == 0 ? "pause_help" : i == 1 ? "deposit_help" : "withdraw_help")));
        }
        if(menu.collector){fuel=addRenderableWidget(new TransmutationScreen.ActionButton(leftPos+116,topPos+65,124,ui("fuel_0"),b->{if(minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,3);}));fuel.setTooltip(Tooltip.create(ui("fuel_help")));}
        refresh();
    }
    private void refresh() {
        if(fuel!=null)fuel.setMessage(ui("fuel_"+menu.fuelTarget()));
        buttons[0].setMessage(ui(menu.paused() ? "resume" : "pause")); buttons[0].active = menu.canOperate();
        buttons[1].active = menu.canOperate() && menu.wallet() > 0 && menu.essence() < menu.capacity();
        buttons[2].active = menu.canOperate() && menu.essence() > 0 && menu.wallet() < io.github.jimbozoomer.jugcraft.concordance.equivalence.Assay.MAX_BALANCE;
    }
    @Override protected void containerTick() { super.containerTick(); refresh(); }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) { extractBackground(g,mx,my,delta); super.extractRenderState(g,mx,my,delta); extractTooltip(g,mx,my); }
    @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float delta){
        int x=leftPos,y=topPos;
        g.fill(0,0,width,height,0xB0081018);
        OwnerAlchemyArt.frame(g,x,y,imageWidth,imageHeight);

        OwnerAlchemyArt.panel(g,x+16,y+34,316,88,OwnerAlchemyArt.TEAL);
        OwnerAlchemyArt.meter(g,x+23,y+38,302,menu.essence(),menu.capacity(),OwnerAlchemyArt.TEAL);
        OwnerAlchemyArt.panel(g,x+89,y+156,170,81,OwnerAlchemyArt.SILVER);

        if(menu.collector){
            OwnerAlchemyArt.crop(g,"collector"+menu.tier(),x+143,y+88,58+(menu.tier()-1)*20,56,58,18);
            OwnerAlchemyArt.flow(g,x+243,x+248,y+73,OwnerAlchemyArt.GOLD,false,delta);
        }else OwnerAlchemyArt.flow(g,x+174,x+235,y+75,OwnerAlchemyArt.TEAL,menu.status()==14,delta);
        for(var slot:menu.slots){
            if(!slot.isActive())continue;
            OwnerAlchemyArt.slot(g,"alchchest",x+slot.x,y+slot.y,11,3);
            if(slot.index==0||slot.index==1)OwnerAlchemyArt.socket(g,x+slot.x,y+slot.y,slot.index==0?OwnerAlchemyArt.VIOLET:OwnerAlchemyArt.GOLD);
        }
        OwnerAlchemyArt.sigil(g,x+44,y+190,20,menu.collector?OwnerAlchemyArt.GOLD:OwnerAlchemyArt.TEAL,menu.status()==9||menu.status()==14,delta);
        BookArt.icon(g,"jugcraft:"+(menu.collector?"collector_mk":"condenser_mk")+menu.tier(),x+36,y+182);

    }
    private void line(GuiGraphicsExtractor g, Component text, int x, int y, int width, int color) { g.text(font,BookArt.fit(font,text.getString(),width),x,y,color,false); }
    @Override protected void extractLabels(GuiGraphicsExtractor g, int mx, int my) {
        line(g,title,19,13,210,0xFFE4DCEC);
        line(g,ui("buffer",n(menu.essence()),n(menu.capacity())),19,25,312,0xFFB5DFE2);
        line(g,ui(menu.collector?"star_charge":"star_supply"),24,52,91,0xFFBDAED3);
        if(menu.collector){
            line(g,ui("sunlight",menu.sunlight()),119,51,122,0xFFE3CE9A);
            line(g,ui("fuel_input"),24,84,88,0xFFE1C39F);
            line(g,ui("output"),251,48,78,0xFFB6E4D0);
        } else {
            line(g,ui("sample"),119,52,105,0xFFE4C786);line(g,ui("output"),251,48,78,0xFFB6E4D0);
            line(g,ui("feed"),24,84,88,0xFFE1C39F);line(g,ui("cost",n(menu.cost())),118,94,207,0xFFC8CEDD);
            line(g,ui("speed",menu.rate()),118,104,207,0xFF9FAEBE);
        }
        line(g,ui("status_"+menu.status()),19,112,311,menu.status()==9||menu.status()==14?0xFF9CE2BC:0xFFE2C99D);
        line(g,ui("wallet",n(menu.wallet())),93,146,237,0xFFCEC5DF);
    }
}
