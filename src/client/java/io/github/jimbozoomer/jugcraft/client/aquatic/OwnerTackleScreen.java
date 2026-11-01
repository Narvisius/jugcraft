package io.github.jimbozoomer.jugcraft.client.aquatic;
import io.github.jimbozoomer.jugcraft.aquatic.OwnerTackleMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
public final class OwnerTackleScreen extends AbstractContainerScreen<OwnerTackleMenu> {
    public OwnerTackleScreen(OwnerTackleMenu menu,Inventory inv,Component title){super(menu,inv,title,176,169);}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int x,int y,float dt){extractBackground(g,x,y,dt);super.extractRenderState(g,x,y,dt);extractTooltip(g,x,y);}
    @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float dt){
        int x=leftPos,y=topPos;g.fill(0,0,width,height,0xA0081418);g.fill(x-2,y-2,x+178,y+171,0xFF142C30);g.fill(x,y,x+176,y+169,0xFF658D88);g.fill(x+4,y+4,x+172,y+72,0xFF263D40);
        g.text(font,"Fishing tackle",x+10,y+9,0xFFFFE9BC,false);String[] names={"Hook","Bait","Line","Float"};
        for(int i=0;i<4;i++)g.text(font,names[i],x+22+i*36,y+58,0xFFD6E8DE,false);
        for(var s:menu.slots){g.fill(x+s.x-1,y+s.y-1,x+s.x+17,y+s.y+17,0xFFB7B6A0);g.fill(x+s.x,y+s.y,x+s.x+16,y+s.y+16,0xFF253B3B);}
    }
    @Override protected void extractLabels(GuiGraphicsExtractor g,int mx,int my){}
}
