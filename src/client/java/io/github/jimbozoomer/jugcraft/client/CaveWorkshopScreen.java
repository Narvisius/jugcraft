package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.menagerie.CaveWorkshop;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Survey controls are buttons, with progress and location kept in the authoritative menu. */
public final class CaveWorkshopScreen extends AbstractContainerScreen<CaveWorkshop> {
    public CaveWorkshopScreen(CaveWorkshop menu,Inventory inv,Component title){super(menu,inv,title,300,220);}
    @Override protected void init(){super.init();for(int i=0;i<CaveWorkshop.NAMES.size();i++){final int choice=i;addRenderableWidget(Button.builder(Component.literal(CaveWorkshop.NAMES.get(i)),b->{if(minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,choice);}).bounds(leftPos+15+(i%2)*139,topPos+39+(i/2)*30,131,24).build());}}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int x,int y,float partial){extractBackground(g,x,y,partial);super.extractRenderState(g,x,y,partial);}
    @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float partial){
        int x=leftPos,y=topPos;g.fill(x-3,y-3,x+303,y+223,0xFF443B30);g.fill(x,y,x+300,y+220,0xFFB29F75);g.fill(x+5,y+5,x+295,y+215,0xFF273638);
        for(int row=0;row<13;row++)g.fill(x+10,y+12+row*16,x+290,y+13+row*16,0xFF304344);
        for(int col=0;col<18;col++)g.fill(x+10+col*16,y+12,x+11+col*16,y+208,0xFF304344);
        g.fill(x+15,y+143,x+285,y+150,0xFF172426);g.fill(x+15,y+143,x+15+(menu.status()==2?270:menu.progress()*270/100),y+150,0xFF8DB89C);
    }
    @Override protected void extractLabels(GuiGraphicsExtractor g,int mx,int my){
        g.text(font,"CAVE SURVEY",15,15,0xFFE9D7AA);g.text(font,"Choose an expedition",15,27,0xFFB8C9B9);
        String status=switch(menu.status()){case 1->"Surveying nearby terrain... "+menu.progress()+"%";case 2->"X "+menu.targetX()+"   Y -16   Z "+menu.targetZ();case 3->"No match within 1,024 blocks. Try farther away.";case 5->"Survey from the Overworld.";case 6->"Survey desks are busy. Try again shortly.";default->"A survey does not load or alter distant chunks.";};
        g.text(font,status,15,160,0xFFE9D7AA);g.text(font,"Bring paper + a cave tablet to the desk",15,184,0xFFB8C9B9);g.text(font,"to keep the location on a physical map.",15,196,0xFFB8C9B9);
    }
}
