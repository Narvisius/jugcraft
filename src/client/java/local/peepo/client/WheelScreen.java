package local.peepo.client;

import io.github.jimbozoomer.jugcraft.client.MachineScreenThemes;
import io.github.jimbozoomer.jugcraft.machine.MachineKind;
import local.peepo.WheelMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Uses the same bay, energy gauge, inventory and terminal as the Jugcraft generators. */
public final class WheelScreen extends AbstractContainerScreen<WheelMenu> {
    private final MachineScreenThemes.Theme theme = MachineScreenThemes.of(MachineKind.COAL_GENERATOR);

    public WheelScreen(WheelMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 268, 166);
    }

    @Override protected void init() {
        super.init();
        titleLabelX = (176 - font.width(title)) / 2;
    }

    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        extractBackground(graphics, mouseX, mouseY, delta);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        extractTooltip(graphics, mouseX, mouseY);
        if (mouseX >= leftPos + 9 && mouseX <= leftPos + 23 && mouseY >= topPos + 16 && mouseY <= topPos + 70)
            graphics.setTooltipForNextFrame(font, Component.literal(String.format("%,d / 32,000 JE", menu.energy())), mouseX, mouseY);
        if(mouseX>=leftPos+124 && mouseX<leftPos+160 && mouseY>=topPos+18 && mouseY<topPos+68)
            graphics.setTooltipForNextFrame(font,Component.translatable("container.peepo_companion.wheel.reserve",menu.reservePercent()),mouseX,mouseY);
        else if(menu.occupantKind()!=0 && mouseX>=leftPos+36 && mouseX<leftPos+119 && mouseY>=topPos+23 && mouseY<topPos+34)
            graphics.setTooltipForNextFrame(font,workerName(),mouseX,mouseY);
    }

    @Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        int x = leftPos, y = topPos;
        graphics.blit(RenderPipelines.GUI_TEXTURED, theme.texture(), x, y, 0.0F, 0.0F, imageWidth, imageHeight, 512, 256);
        graphics.fill(x + 9, y + 16, x + 23, y + 70, theme.slotDark());
        graphics.fill(x + 10, y + 17, x + 22, y + 69, theme.slotFace());
        int fill = menu.energy() * 52 / 32000;
        graphics.fill(x + 10, y + 69 - fill, x + 22, y + 69, theme.energy());
        for (int tick = 1; tick < 4; tick++)
            graphics.fill(x + 19, y + 69 - tick * 13, x + 22, y + 70 - tick * 13, theme.slotLight());
        drawReserve(graphics,x+124,y+18);

    }

    private Component workerName(){
        if(minecraft!=null && minecraft.level!=null && menu.occupantKind()!=0
                && minecraft.level.getEntity(menu.occupantId()) instanceof local.peepo.PeepoEntity npc)
            return npc.getDisplayName();
        return Component.literal(menu.occupantKind()==2?"Jughead":"Peepo");
    }

    // Compact pixel silhouette: eye bumps, broad head/body, thin arms and two feet.
    private static final String[] FIGURE={
        "....####...####....",
        "...############...",
        "..##############..",
        ".################.",
        "##################",
        "##################",
        "##################",
        "##################",
        ".################.",
        "..##############..",
        "..##############..",
        ".################.",
        "##################",
        "##################",
        "##.############.##",
        "##.############.##",
        "##.############.##",
        "...############...",
        "...############...",
        "....##########....",
        "....###....###....",
        "....###....###....",
        "....###....###....",
        "...####....####...",
        "...####....####..."
    };
    private record Span(int x,int y,int width){}
    private static boolean pixel(int x,int y){return y>=0 && y<FIGURE.length && x>=0 && x<18 && FIGURE[y].charAt(x)=='#';}
    private static final java.util.List<Span> OUTLINE=spans(false),INTERIOR=spans(true);
    private static java.util.List<Span> spans(boolean inside){
        var result=new java.util.ArrayList<Span>();
        for(int y=0;y<FIGURE.length;y++){
            int start=-1;
            for(int x=0;x<=18;x++){
                boolean on=pixel(x,y) && (!inside || pixel(x-1,y)&&pixel(x+1,y)&&pixel(x,y-1)&&pixel(x,y+1));
                if(on && start<0)start=x;
                if(!on && start>=0){result.add(new Span(start*2,y*2,(x-start)*2));start=-1;}
            }
        }
        return java.util.List.copyOf(result);
    }
    private void drawReserve(GuiGraphicsExtractor g,int x,int y){
        for(var r:OUTLINE)g.fill(x+r.x,y+r.y,x+r.x+r.width,y+r.y+2,theme.slotLight());
        int level=50-(menu.occupantKind()==0?0:Math.clamp(menu.reservePercent(),0,100))*50/100;
        for(var r:INTERIOR){
            g.fill(x+r.x,y+r.y,x+r.x+r.width,y+r.y+2,theme.slotDark());
            int top=Math.max(r.y,level);
            if(top<r.y+2)g.fill(x+r.x,y+top,x+r.x+r.width,y+r.y+2,0xFF73BA52);
        }
        // Minimal facial marks keep the empty gauge recognizable as Peepo.
        g.fill(x+10,y+10,x+12,y+14,theme.slotLight());
        g.fill(x+24,y+10,x+26,y+14,theme.slotLight());
        g.fill(x+12,y+16,x+24,y+18,theme.slotLight());
    }

    private void line(GuiGraphicsExtractor graphics, String key, int x, int y, int color) {
        graphics.text(font, Component.translatable("container.peepo_companion.wheel." + key), x, y, color, false);
    }

    @Override protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, title, titleLabelX, titleLabelY, theme.label(), false);
        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, theme.label(), false);
        if (menu.occupantKind()==0) line(graphics, "no_worker", 36, 24, theme.label());
        else {
            String name=workerName().getString();
            String shown=font.width(name)>82?font.plainSubstrByWidth(name,73)+"...":name;
            graphics.text(font,shown,36,24,theme.label(),false);
        }
        graphics.text(font, Component.translatable("container.peepo_companion.wheel.reserve", menu.reservePercent()), 36, 37, theme.label(), false);
        line(graphics, "max_rate", 36, 61, theme.label());

        line(graphics, "terminal", 185, 10, theme.dim());
        graphics.fill(185, 23, 259, 24, theme.dim());
        line(graphics, menu.energy() >= 32000 ? "full" : menu.running() ? "running" : "waiting", 185, 29, theme.text());
        line(graphics, "stored", 185, 43, theme.dim());
        graphics.text(font, String.format("%,d JE", menu.energy()), 185, 54, theme.text(), false);
        line(graphics, "push", 185, 68, theme.dim());
        graphics.text(font, menu.outputRate() + " JE/t", 185, 79, theme.text(), false);
    }
}
