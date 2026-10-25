package io.github.jimbozoomer.jugcraft.client.jimtafacts;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.jimtafacts.industry.WorkshopChute;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class WorkshopChuteScreen extends AbstractContainerScreen<WorkshopChute.Menu> {
    private final Button[] buttons=new Button[7];
    public WorkshopChuteScreen(WorkshopChute.Menu menu,Inventory inv,Component title){super(menu,inv,title,176,196);}
    @Override protected void init(){super.init();titleLabelY=inventoryLabelY=10000;for(int i=0;i<7;i++){int action=i;buttons[i]=addRenderableWidget(Button.builder(Component.empty(),b->{if(minecraft!=null&&minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,action);})
        .bounds(leftPos+8+(i%3)*54,topPos+(i<6?43+(i/3)*20:84),i==6?106:52,18).tooltip(Tooltip.create(Component.literal(i==6?"Collect loose items or pull from the inventory at the wide opening. Redstone pauses the chute.":"Toggle this output. Available outputs share one transfer budget and take turns. The wide opening is the input."))).build());}}
    @Override protected void containerTick(){super.containerTick();for(int i=0;i<6;i++){boolean input=menu.data.get(3)==i;buttons[i].active=!input;buttons[i].setMessage(Component.literal(Direction.values()[i].getName()+ (input?" IN":(menu.data.get(0)&(1<<i))!=0?" +":" −")));}buttons[6].setMessage(Component.literal(menu.data.get(1)==1?"Collection: ON":"Collection: OFF"));}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float dt){extractBackground(g,mx,my,dt);super.extractRenderState(g,mx,my,dt);extractTooltip(g,mx,my);}
    @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float dt){
        g.blit(RenderPipelines.GUI_TEXTURED,Jugcraft.id("textures/gui/jimtafacts/chute.png"),leftPos,topPos,0,0,176,196,256,256);
        g.fill(leftPos+6,topPos+6,leftPos+170,topPos+18,0xff293239);g.text(font,title,leftPos+8,topPos+8,0xffe8eeee,false);
        g.fill(leftPos+115,topPos+85,leftPos+170,topPos+101,0xff293239);g.text(font,menu.data.get(2)+" JE",leftPos+118,topPos+89,0xffd8eeee,false);
        for(var slot:menu.slots){int x=leftPos+slot.x,y=topPos+slot.y;g.fill(x-1,y-1,x+17,y+17,0xff1b2429);g.fill(x,y,x+16,y+16,0xff929b9e);}
    }
    @Override protected void extractLabels(GuiGraphicsExtractor g,int x,int y){}
}
