package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.menagerie.CavernDeviceMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class CavernDeviceScreen extends AbstractContainerScreen<CavernDeviceMenu> {
    public CavernDeviceScreen(CavernDeviceMenu m,Inventory i,Component t){super(m,i,t,176,222);inventoryLabelY=128;}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int x,int y,float p){extractBackground(g,x,y,p);super.extractRenderState(g,x,y,p);extractTooltip(g,x,y);}
    @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float p){
        int x=leftPos,y=topPos;g.fill(x-2,y-2,x+imageWidth+2,y+imageHeight+2,0xFF11191C);g.fill(x,y,x+imageWidth,y+imageHeight,0xFF687777);g.fill(x+4,y+4,x+imageWidth-4,y+122,0xFF27383B);
        for(var slot:menu.slots){int a=x+slot.x,b=y+slot.y;g.fill(a-1,b-1,a+17,b+17,0xFF172124);g.fill(a,b,a+16,b+16,0xFF48595C);}
        g.fill(x+52,y+40,x+95,y+45,0xFF142126);g.fill(x+52,y+40,x+52+menu.progress()*43/100,y+45,0xFF8BB7AA);
    }
    @Override protected void extractLabels(GuiGraphicsExtractor g,int mx,int my){
        g.text(font,BookArt.fit(font,title.getString(),160),8,6,0xFFE3E5CF);g.text(font,"Input",10,25,0xFFB5CBC6);g.text(font,"Output",105,17,0xFFB5CBC6);
        String fuel=switch(menu.kind()){case 0->"Uranium rod";case 1->"Redstone";case 2->"Sugar";case 4->"Holocoder only";default->"Diamond";};g.text(font,fuel,8,85,0xFFB5CBC6);
        if(menu.kind()==0){g.text(font,"Can",50,55,0xFFE9C27F);g.text(font,"Waste",75,85,0xFFE9C27F);}
        String status=switch(menu.status()){case 1->"Waiting for owner / permission";case 2->"Build a 2 x 2 x 2 reactor";case 3->"Use the lower controller";case 4->"Output is full";case 5->"Supply input, fuel or barrel";case 6->"Remove the sealed waste";case 7->"Insert quarry smasher";case 8->"Place four boundary lights";case 9->"Quarry complete";case 10->"Requires redstone signal";case 11->"Nearby creature limit reached";case 12->"Bind a biome treat first";default->menu.progress()>0?"Working: "+menu.progress()+"%":"Ready";};
        int row=0;for(var line:font.split(Component.literal(status),160)){if(row>=2)break;g.text(font,line,8,99+row++*10,0xFFE3E5CF);}g.text(font,playerInventoryTitle,8,128,0xFF142126,false);
    }
}
