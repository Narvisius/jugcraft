package io.github.jimbozoomer.jugcraft.client.aquatic;
import io.github.jimbozoomer.jugcraft.aquatic.OwnerTackleStationMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
public final class OwnerTackleStationScreen extends AbstractContainerScreen<OwnerTackleStationMenu> {
    public OwnerTackleStationScreen(OwnerTackleStationMenu m,Inventory inv,Component title){super(m,inv,title,176,206);}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int x,int y,float dt){extractBackground(g,x,y,dt);super.extractRenderState(g,x,y,dt);extractTooltip(g,x,y);}
    @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float dt){int x=leftPos,y=topPos;g.fill(0,0,width,height,0xA0081418);g.fill(x-2,y-2,x+178,y+208,0xFF142C30);g.fill(x,y,x+176,y+206,0xFF658D88);g.fill(x+4,y+4,x+172,y+112,0xFF263D40);g.text(font,"Tackle box",x+10,y+10,0xFFFFE9BC,false);String[] labels={"Rod","Hook","Bait","Line","Float"};int[] xs={17,53,80,107,134};for(int i=0;i<5;i++)g.text(font,labels[i],x+xs[i]-3,y+54,0xFFD6E8DE,false);for(var slot:menu.slots){g.fill(x+slot.x-1,y+slot.y-1,x+slot.x+17,y+slot.y+17,0xFFB7B6A0);g.fill(x+slot.x,y+slot.y,x+slot.x+16,y+slot.y+16,0xFF253B3B);}}
    @Override protected void extractLabels(GuiGraphicsExtractor g,int mx,int my){}
}
