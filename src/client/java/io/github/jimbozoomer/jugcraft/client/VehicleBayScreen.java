package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.vehiclebay.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import java.util.ArrayList;
import java.util.List;

/** Physical workshop console: vehicle selector, chassis options, exact material bill, and inventory. No typing. */
public class VehicleBayScreen extends AbstractContainerScreen<VehicleBayMenu> {
    private final MachineScreenThemes.Theme theme = new MachineScreenThemes.Theme("advanced",
        MachineScreenThemes.FALLBACK.texture(), 0xFFC5E3E7, 0xFF64828F, 0xFFE0A254, 0xFF67DDCC,
        0xFFCF9440, 0xFF47C9CA, 0xFFC4D0D7, 0xFFB9E1E5, 0xFF091218, 0xFF14232B, 0xFF49616F,
        0xFF253B49, 0xFF101D27, 0xFF4F8998, 0xFFC7EAEF, true, "advanced",
        new MachineScreenThemes.Frame(new int[]{0xFF101B23,0xFF202F3A,0xFF263640,0xFF49616F,0xFF728B99},
            0xFF091218,0xFF101D26,0xFF253C46,0xFF607E8D,0,0xFF14232B));
    private record Choice(int id, ScreenKit.TextButton button, String label) { }
    private final List<Choice> choices = new ArrayList<>();
    public VehicleBayScreen(VehicleBayMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, 318, 238);
    }
    private void button(int id, int x, int y, int w, String key, String help) {
        var button = addRenderableWidget(new ScreenKit.TextButton(leftPos+x, topPos+y, w, 17, theme,
            Component.translatable(key), b -> { if (minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id); }));
        button.setTooltip(Tooltip.create(Component.translatable(help))); choices.add(new Choice(id, button, key));
    }
    @Override protected void init() {
        super.init(); choices.clear(); titleLabelX = 12; titleLabelY = 5; inventoryLabelY = 10000;
        for (int i = 0; i < 4; i++) button(i, 10, 23 + (i == 3 ? 0 : i)*20, 92, "screen.jugcraft.bay.plan."+i, "screen.jugcraft.bay.plan_help");
        for (int i = 0; i < 2; i++) button(10+i, 202, 23+i*20, 106, "screen.jugcraft.bay.armor."+i, "screen.jugcraft.bay.armor_help");
        for (int i = 0; i < 3; i++) button(20+i, 202+i*36, 66, 34, "screen.jugcraft.bay.drive_short."+i, "screen.jugcraft.bay.drive_help."+i);
        button(30, 110, 104, 80, "screen.jugcraft.bay.start", "screen.jugcraft.bay.start_help");
        button(31, 196, 104, 70, "screen.jugcraft.bay.cancel", "screen.jugcraft.bay.cancel_help");
        refresh();
    }
    @Override protected void containerTick() { super.containerTick(); refresh(); }
    private void refresh() {
        for (var c : choices) {
            int id = c.id(); boolean selected = id < 4 ? id == menu.plan() : id < 20 ? id == 10+menu.data.get(1) : id < 30 && id == 20+menu.data.get(2);
            c.button().visible = id >= 4 || (menu.data.get(8) == 1 ? id == 3 : id < 3);
            c.button().active = id == 31 ? menu.busy() : !menu.busy() && (id >= 4 || menu.data.get(9) == 0);
            c.button().setSelected(selected);
            c.button().setMessage(Component.translatable(c.label()));
        }
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float dt) {
        extractBackground(g, mx, my, dt); super.extractRenderState(g, mx, my, dt); extractTooltip(g, mx, my);
        var needs = VehiclePlans.needs(menu.plan(), menu.data.get(1), menu.data.get(2), menu.data.get(9)==1, VehicleFitout.read(menu.slots.get(9).getItem()));
        for (int i=0;i<needs.size();i++) if (mx>=leftPos+110+i*18 && mx<leftPos+128+i*18 && my>=topPos+136 && my<topPos+153)
            g.setTooltipForNextFrame(font, Component.literal(needs.get(i).count()+" × ").append(needs.get(i).preview().getHoverName()), mx, my);
    }
    @Override public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float dt) {
        int x=leftPos,y=topPos;
        g.fill(0,0,width,height,0xA0081018);
        ScreenKit.body(g,theme,x,y,imageWidth,158); ScreenKit.pane(g,theme,x+5,y+18,101,69);
        ScreenKit.pane(g,theme,x+109,y+18,88,69); ScreenKit.pane(g,theme,x+199,y+18,114,69);
        ScreenKit.tray(g,theme,x,y+157,imageWidth,81); ScreenKit.inventory(g,theme,x+77,y+158);
        // Blueprint grid surrounds the actual vehicle's inventory model, with a subtle scanning light.
        for(int n=0;n<8;n++) g.fill(x+113+n*10,y+22,x+114+n*10,y+83,0xFF253C46);
        for(int n=0;n<6;n++) g.fill(x+113,y+22+n*10,x+193,y+23+n*10,0xFF253C46);
        g.pose().pushMatrix(); g.pose().translate(x+125,y+28); g.pose().scale(3,3);
        g.item(VehiclePlans.product(menu.plan()),0,0); g.pose().popMatrix();
        int scan=(int)((System.currentTimeMillis()/60)%58); g.fill(x+113,y+23+scan,x+193,y+24+scan,0x553BCBCD);
        for(int i=0;i<9;i++) MachineSkin.slot(g,theme,x+12+i%3*18,y+100+i/3*18);
        MachineSkin.slot(g,theme,x+83,y+118); MachineSkin.slot(g,theme,x+286,y+118);
        int progress=menu.data.get(3),total=menu.data.get(4);
        g.fill(x+110,y+126,x+266,y+132,0xFF111B23);
        g.fill(x+111,y+127,x+111+(total==0?0:154*progress/total),y+131,0xFF47C9CA);
        int energy=(menu.data.get(5)&65535)|((menu.data.get(6)&65535)<<16);
        g.fill(x+10,y+154,x+308,y+156,0xFF17242B); g.fill(x+10,y+154,x+10+298*Math.min(64000,energy)/64000,y+156,0xFFCF9440);
        var needs=VehiclePlans.needs(menu.plan(),menu.data.get(1),menu.data.get(2),menu.data.get(9)==1,VehicleFitout.read(menu.slots.get(9).getItem()));
        var available = new ArrayList<net.minecraft.world.item.ItemStack>();
        for(int i=0;i<9;i++) available.add(menu.slots.get(i).getItem().copy());
        for(int i=0;i<needs.size();i++) {
            var need=needs.get(i);int missing=need.count();
            for(var held:available) if(need.matches(held)){int take=Math.min(missing,held.getCount());held.shrink(take);missing-=take;}
            g.fill(x+110+i*18,y+135,x+126+i*18,y+136,missing==0?0xFF67DDCC:0xFFD08358);
            var stack=need.preview();g.item(stack,x+110+i*18,y+136);g.itemDecorations(font,stack,x+110+i*18,y+136);
        }
    }
    @Override protected void extractLabels(GuiGraphicsExtractor g,int mx,int my) {
        g.text(font,title,12,5,0xFFE3E8E8,false);
        g.text(font,Component.translatable("screen.jugcraft.bay.materials"),12,90,theme.label(),false);
        g.text(font,Component.translatable("screen.jugcraft.bay.refit"),78,139,theme.label(),false);
        g.text(font,Component.translatable("screen.jugcraft.bay.output"),277,139,theme.label(),false);
        g.text(font,Component.translatable("screen.jugcraft.bay.status."+menu.data.get(7)),110,91,menu.data.get(7)==4?0xFF67DDCC:theme.label(),false);
        int energy=(menu.data.get(5)&65535)|((menu.data.get(6)&65535)<<16);
        g.text(font,Component.literal(energy/1000+"k JE"),12,175,theme.label(),false);
    }
}
