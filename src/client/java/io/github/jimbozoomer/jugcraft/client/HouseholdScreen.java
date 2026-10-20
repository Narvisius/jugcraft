package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.menagerie.HouseholdMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Beveled machine frame with clear ingredient, power and result locations. */
public final class HouseholdScreen extends AbstractContainerScreen<HouseholdMenu> {
    public HouseholdScreen(HouseholdMenu menu,Inventory inv,Component title){super(menu,inv,title);}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int x,int y,float partial){extractBackground(g,x,y,partial);super.extractRenderState(g,x,y,partial);extractTooltip(g,x,y);}
    @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float partial){
        int x=leftPos,y=topPos;
        int color=menu.kind==2?0xFF9B73BE:menu.kind==1?0xFF75B7CE:0xFFD8862C;
        // The old machine texture already contained labels, which collided with these menu-specific captions.
        g.fill(x,y,x+imageWidth,y+imageHeight,0xFF252A2B);
        g.fill(x+2,y+2,x+imageWidth-2,y+imageHeight-2,0xFFE1E3DE);
        g.fill(x+4,y+4,x+imageWidth-4,y+imageHeight-4,0xFFBCC1BC);
        g.fill(x+4,y+17,x+imageWidth-4,y+19,color);
        for(var slot:menu.slots){int sx=x+slot.x,sy=y+slot.y;g.fill(sx-1,sy-1,sx+17,sy+17,0xFF454F4E);g.fill(sx,sy,sx+17,sy+17,0xFFE1E3DE);g.fill(sx,sy,sx+16,sy+16,0xFF8A9591);}
        for(int[] slot:new int[][]{{44,29},{44,55},{116,37}}){int sx=x+slot[0],sy=y+slot[1];g.fill(sx-1,sy-1,sx+17,sy+17,0xFF373737);g.fill(sx,sy,sx+17,sy+17,0xFFE0E0E0);g.fill(sx,sy,sx+16,sy+16,0xFF8B8B8B);}
        g.fill(x+75,y+42,x+103,y+47,0xFF373737);g.fill(x+75,y+42,x+75+menu.progress(28),y+47,color);
        g.fill(x+68,y+69,x+73,y+74,menu.powered()?color:0xFF55585B);
    }
    @Override protected void extractLabels(GuiGraphicsExtractor g,int mx,int my){
        super.extractLabels(g,mx,my);
        g.text(font,"Input",42,20,0xFF303A38,false);g.text(font,"Output",108,26,0xFF303A38,false);
        g.text(font,menu.kind==2?"Amethyst":menu.kind==1?"Ice / snow":"Fuel",68,57,0xFF303A38,false);
        g.text(font,menu.powered()?"Ready":"Idle",77,69,0xFF303A38,false);
    }
}
