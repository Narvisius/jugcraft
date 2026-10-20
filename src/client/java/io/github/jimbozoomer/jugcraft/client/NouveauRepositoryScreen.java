package io.github.jimbozoomer.jugcraft.client;
import io.github.jimbozoomer.jugcraft.nouveau.NouveauRepositories;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
public final class NouveauRepositoryScreen extends AbstractContainerScreen<NouveauRepositories.Menu>{
    @Override public void extractRenderState(GuiGraphicsExtractor g,int x,int y,float partial){extractBackground(g,x,y,partial);super.extractRenderState(g,x,y,partial);extractTooltip(g,x,y);}
    public NouveauRepositoryScreen(NouveauRepositories.Menu m,Inventory i,Component t){super(m,i,t,176,240);}
    protected void init(){super.init();addRenderableWidget(Button.builder(Component.literal("‹"),b->minecraft.gameMode.handleInventoryButtonClick(menu.containerId,0)).bounds(leftPos+8,topPos+140,24,18).build());addRenderableWidget(Button.builder(Component.literal("›"),b->minecraft.gameMode.handleInventoryButtonClick(menu.containerId,1)).bounds(leftPos+144,topPos+140,24,18).build());}
    public void extractBackground(GuiGraphicsExtractor g,int x,int y,float partial){g.fill(0,0,width,height,0xB0080814);OwnerAlchemyArt.frame(g,leftPos,topPos,imageWidth,imageHeight);for(var slot:menu.slots){g.fill(leftPos+slot.x-1,topPos+slot.y-1,leftPos+slot.x+17,topPos+slot.y+17,0xff596476);g.fill(leftPos+slot.x,topPos+slot.y,leftPos+slot.x+16,topPos+slot.y+16,0xff141d2c);}}
    protected void extractLabels(GuiGraphicsExtractor g,int x,int y){g.text(font,title,8,10,0xffeee2bb,false);g.text(font,"Repository "+(menu.page()+1)+" / "+Math.max(1,menu.pages()),38,145,0xffb4cdd6,false);}
}
