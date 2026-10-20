package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.nouveau.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public final class NouveauFamiliarScreen extends AbstractContainerScreen<NouveauFamiliarMenu> {
    @Override public void extractRenderState(GuiGraphicsExtractor g,int x,int y,float partial){extractBackground(g,x,y,partial);super.extractRenderState(g,x,y,partial);extractTooltip(g,x,y);}
    public NouveauFamiliarScreen(NouveauFamiliarMenu m,Inventory inventory,Component title){super(m,inventory,title,300,214);}
    @Override protected void init(){super.init();for(int i=0;i<6;i++){final int index=i;String name=NouveauFamiliars.SPECIES.get(i);addRenderableWidget(Button.builder(Component.literal(name.replace('_',' ')),b->minecraft.gameMode.handleInventoryButtonClick(menu.containerId,index)).bounds(leftPos+44+(i%2)*136,topPos+42+(i/2)*42,112,24).build());}addRenderableWidget(Button.builder(Component.literal("Dismiss companion"),b->minecraft.gameMode.handleInventoryButtonClick(menu.containerId,6)).bounds(leftPos+70,topPos+177,160,20).build());}
    @Override public void extractBackground(GuiGraphicsExtractor g,int mouseX,int mouseY,float partial){
        g.fill(0,0,width,height,0xB0080814);OwnerAlchemyArt.frame(g,leftPos,topPos,imageWidth,imageHeight);
        for(int i=0;i<6;i++){int x=leftPos+15+(i%2)*136,y=topPos+42+(i/2)*42;g.fill(x-3,y-3,x+126,y+28,menu.active()==i?0xff465850:0xff29293e);g.item(new ItemStack(NouveauCatalog.item("familiar_"+NouveauFamiliars.SPECIES.get(i))),x,y+3);if(!menu.known(i))g.text(font,"Unbound",x+29,y+27,0xff9e98ad,false);}
    }
    @Override protected void extractLabels(GuiGraphicsExtractor g,int x,int y){g.text(font,title,14,13,0xffe5d7aa,false);g.text(font,"Choose a bound companion.",14,27,0xffb9b3c9,false);}
}
