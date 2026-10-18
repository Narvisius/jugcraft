package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.assay.KleinStarMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** Reuses the tablet's stone frame, owner sigil and native narrated buttons. No text input. */
public final class KleinStarScreen extends AbstractContainerScreen<KleinStarMenu> {
    private static final Identifier ART=Jugcraft.id("textures/gui/alchemy/transmutation.png");
    private final TransmutationScreen.ActionButton[] buttons=new TransmutationScreen.ActionButton[6];
    public KleinStarScreen(KleinStarMenu menu,Inventory inventory,Component title){super(menu,inventory,title,302,238);}
    private static Component ui(String key,Object...args){return Component.translatable("screen.jugcraft.star."+key,args);}
    @Override protected void init(){
        super.init();
        for(int i=0;i<6;i++){
            final int id=i;
            buttons[i]=addRenderableWidget(new TransmutationScreen.ActionButton(leftPos+18+(i%3)*89,topPos+94+(i/3)*22,88,
                    ui("button_"+i),b->{if(minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,id);}));
            buttons[i].setTooltip(Tooltip.create(ui(i<3?"store_help":"withdraw_help")));
        }
        refresh();
    }
    private void refresh(){for(int i=0;i<6;i++)buttons[i].active=i<3?menu.canStore():menu.canWithdraw();}
    @Override protected void containerTick(){super.containerTick();refresh();}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta){extractBackground(g,mx,my,delta);super.extractRenderState(g,mx,my,delta);extractTooltip(g,mx,my);}
    @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float delta){
        int x=leftPos,y=topPos;
        g.fill(0,0,width,height,0xB0081018);
        OwnerAlchemyArt.frame(g,x,y,imageWidth,imageHeight);

        OwnerAlchemyArt.panel(g,x+14,y+32,274,58,OwnerAlchemyArt.TEAL);
        OwnerAlchemyArt.sigil(g,x+151,y+57,20,OwnerAlchemyArt.VIOLET,menu.stored()>0,delta);
        OwnerAlchemyArt.meter(g,x+19,y+81,264,menu.stored(),menu.capacity(),OwnerAlchemyArt.TEAL);
        OwnerAlchemyArt.panel(g,x+65,y+147,172,81,OwnerAlchemyArt.SILVER);
        for(var slot:menu.slots){
            OwnerAlchemyArt.slot(g,"alchchest",x+slot.x,y+slot.y,11,3);
            if(slot.index==0)OwnerAlchemyArt.socket(g,x+slot.x,y+slot.y,OwnerAlchemyArt.VIOLET);
        }

    }
    private void text(GuiGraphicsExtractor g,Component text,int x,int y,int width,int color){g.text(font,BookArt.fit(font,text.getString(),width),x,y,color,false);}
    @Override protected void extractLabels(GuiGraphicsExtractor g,int mx,int my){
        text(g,ui("title"),18,15,265,0xFFE1DAE9);
        text(g,ui("wallet"),22,39,90,0xFFACA5B8);text(g,Component.literal(String.format("%,d",menu.balance())),22,54,90,0xFFEDDAFF);
        text(g,ui("stored"),195,39,90,0xFFACA5B8);text(g,Component.literal(String.format("%,d",menu.stored())),195,54,90,0xFFB8F0ED);
        text(g,menu.capacity()>0?ui("capacity",String.format("%,d",menu.capacity())):ui("insert"),22,70,262,0xFFC9C6D0);
        text(g,Component.translatable("screen.jugcraft.alchemy.inventory"),70,138,163,0xFFD5D5DE);
    }
}
