package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.menagerie.OwnerHomeMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Exact-capacity furniture drawers with a compact wooden frame and readable slots. */
final class OwnerHomeScreen extends AbstractContainerScreen<OwnerHomeMenu> {
    OwnerHomeScreen(OwnerHomeMenu menu,Inventory inv,Component title){super(menu,inv,title,176,170);inventoryLabelY=76;}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int x,int y,float partial){extractBackground(g,x,y,partial);super.extractRenderState(g,x,y,partial);extractTooltip(g,x,y);}
    @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float partial){
        int x=leftPos,y=topPos;g.fill(x,y,x+imageWidth,y+imageHeight,0xFF3C2E24);
        g.fill(x+2,y+2,x+imageWidth-2,y+imageHeight-2,0xFFB88D58);g.fill(x+5,y+17,x+imageWidth-5,y+74,0xFF75583C);
        for(var slot:menu.slots){int sx=x+slot.x,sy=y+slot.y;g.fill(sx-1,sy-1,sx+17,sy+17,0xFF382B20);g.fill(sx,sy,sx+17,sy+17,0xFFE1CBA7);g.fill(sx,sy,sx+16,sy+16,0xFF8E806B);}
    }
}
