package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.jimtafacts.industry.FactoryPlanning;
import java.util.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Button-based saved-zone directory with exact full-width coordinates and a plan-view footprint. */
public final class FactoryPlannerScreen extends AbstractContainerScreen<FactoryPlanning.Menu> {
    private final MachineScreenThemes.Theme theme=MachineScreenThemes.screen("logic_controller");
    private final List<Button> zones=new ArrayList<>();private Button copy,delete;
    public FactoryPlannerScreen(FactoryPlanning.Menu menu,Inventory inv,Component title){super(menu,inv,title,340,244);}
    @Override protected void init(){super.init();zones.clear();
        for(int i=0;i<16;i++){final int id=i;zones.add(addRenderableWidget(Button.builder(Component.literal("Zone "+(i+1)),b->send(id)).bounds(leftPos+12+(i%2)*70,topPos+40+(i/2)*20,67,18).build()));}
        copy=addRenderableWidget(Button.builder(Component.literal("Copy to marker"),b->send(16)).bounds(leftPos+160,topPos+194,167,18).build());
        delete=addRenderableWidget(Button.builder(Component.literal("Remove saved zone"),b->send(17)).bounds(leftPos+160,topPos+215,167,18).build());
    }
    private void send(int id){if(minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,id);}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float dt){
        for(int i=0;i<16;i++){zones.get(i).active=i<menu.data.get(0);zones.get(i).setMessage(Component.literal((menu.data.get(1)==i&&i<menu.data.get(0)?"› ":"")+"Zone "+(i+1)));}
        copy.active=delete.active=menu.data.get(0)>0;extractBackground(g,mx,my,dt);super.extractRenderState(g,mx,my,dt);extractTooltip(g,mx,my);
    }
    @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float dt){
        ScreenKit.body(g,theme,leftPos,topPos,imageWidth,imageHeight);ScreenKit.pane(g,theme,leftPos+156,topPos+38,173,152);
        if(menu.data.get(0)==0)return;
        int width=Math.abs(menu.coordinate(3)-menu.coordinate(0))+1,depth=Math.abs(menu.coordinate(5)-menu.coordinate(2))+1;
        int scale=Math.max(1,Math.min(140/width,66/depth));int x=leftPos+242-width*scale/2,y=topPos+151-depth*scale/2;
        g.fill(x-1,y-1,x+width*scale+1,y+depth*scale+1,theme.ok());g.fill(x,y,x+width*scale,y+depth*scale,theme.slotDark());
        for(int i=1;i<width;i++)g.fill(x+i*scale,y,x+i*scale+1,y+depth*scale,0xFF435B55);
        for(int i=1;i<depth;i++)g.fill(x,y+i*scale,x+width*scale,y+i*scale+1,0xFF435B55);
    }
    @Override protected void extractLabels(GuiGraphicsExtractor g,int mx,int my){
        g.text(font,title,12,8,theme.label());g.text(font,"Use a completed Volume Marker on this desk to save it.",12,25,theme.dim());
        g.text(font,menu.data.get(0)+" / 16 saved areas",12,211,theme.dim());
        if(menu.data.get(0)==0){g.text(font,"No saved zones",165,48,theme.dim());return;}
        g.text(font,"      A            B",163,47,theme.dim());
        for(int i=0;i<3;i++)g.text(font,List.of("X","Y","Z").get(i)+": "+menu.coordinate(i)+" / "+menu.coordinate(i+3),163,60+i*13,theme.text());
        g.text(font,"Top view · north ↑",163,103,theme.dim());
    }
}
