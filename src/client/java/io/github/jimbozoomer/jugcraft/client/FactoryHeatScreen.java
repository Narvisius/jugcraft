package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.jimtafacts.industry.FactoryHeatExchanger;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Live overview of both circuits, including assembly errors and transfer direction. */
public final class FactoryHeatScreen extends AbstractContainerScreen<FactoryHeatExchanger.Menu> {
    private final MachineScreenThemes.Theme theme=MachineScreenThemes.screen("logic_controller");
    public FactoryHeatScreen(FactoryHeatExchanger.Menu menu,Inventory inv,Component title){super(menu,inv,title,320,216);}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float dt){extractBackground(g,mx,my,dt);super.extractRenderState(g,mx,my,dt);extractTooltip(g,mx,my);}
    @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float dt){
        ScreenKit.body(g,theme,leftPos,topPos,imageWidth,imageHeight);
        for(int i=0;i<4;i++){
            int y=topPos+58+i*29;
            ScreenKit.pane(g,theme,leftPos+10,y-15,300,27);
            g.fill(leftPos+17,y,leftPos+302,y+6,theme.slotDark());
            int value=Math.clamp(menu.data.get(i+2),0,16000);
            g.fill(leftPos+18,y+1,leftPos+18+283*value/16000,y+5,i==0?0xFFD59549:i==3?0xFFADC6CA:0xFF5697A0);
        }
    }
    @Override protected void extractLabels(GuiGraphicsExtractor g,int mx,int my){
        g.text(font,title,12,8,theme.label());
        String status=switch(menu.data.get(0)){case 1->"Waiting for hot coolant and water";case 2->"Paused by redstone";case 3->"Exchanging heat";case 4->"Drain both outputs to continue";default->"Incomplete / inaccessible assembly";};
        g.text(font,status,14,27,menu.data.get(0)==3?theme.ok():theme.warn());
        String[] labels={"Start: hot coolant IN","Start: cooled coolant OUT","End: water IN","End: steam OUT"};
        for(int i=0;i<4;i++){g.text(font,labels[i],17,46+i*29,theme.text());g.text(font,menu.data.get(i+2)+" mB",243,46+i*29,theme.dim());}
        g.text(font,"Start > 1–3 middles > End · all face same way",14,169,theme.text());
        g.text(font,"Sections: "+menu.data.get(1)+" · 1 coolant + 1 water = 10 steam",14,183,theme.dim());
        g.text(font,"Each endpoint: bottom OUT · other faces IN",14,197,theme.dim());
    }
}
