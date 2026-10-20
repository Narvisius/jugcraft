package io.github.jimbozoomer.jugcraft.client;
import io.github.jimbozoomer.jugcraft.nouveau.NouveauPotionMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
public final class NouveauPotionScreen extends AbstractContainerScreen<NouveauPotionMenu>{
    @Override public void extractRenderState(GuiGraphicsExtractor g,int x,int y,float partial){extractBackground(g,x,y,partial);super.extractRenderState(g,x,y,partial);extractTooltip(g,x,y);}
    public NouveauPotionScreen(NouveauPotionMenu m,Inventory inv,Component title){super(m,inv,title,246,165);}
    public void extractBackground(GuiGraphicsExtractor g,int x,int y,float partial){g.fill(0,0,width,height,0xB0080814);OwnerAlchemyArt.frame(g,leftPos,topPos,imageWidth,imageHeight);for(int i=0;i<36;i++){var s=menu.slots.get(i);g.fill(leftPos+s.x-3,topPos+s.y-3,leftPos+s.x+19,topPos+s.y+19,i==menu.selected()?0xffc1a36c:0xff485165);g.fill(leftPos+s.x-1,topPos+s.y-1,leftPos+s.x+17,topPos+s.y+17,0xff15202b);}}
    protected void extractLabels(GuiGraphicsExtractor g,int x,int y){g.text(font,title,16,12,0xfff0e4c0,false);g.text(font,"Select a bottle or filled flask.",16,27,0xffa9c8d1,false);g.text(font,menu.crown()?"Choose a dose to drink instantly.":"One dose per shot.",16,145,0xffa9c8d1,false);}
}
