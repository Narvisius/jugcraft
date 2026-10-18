package io.github.jimbozoomer.jugcraft.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Six short sections rather than an unsearchable wall of text; no network writes or research unlocks. */
public final class AlchemyStorageManualScreen extends Screen {
    private int topic;
    private final TransmutationScreen.ActionButton[] tabs=new TransmutationScreen.ActionButton[6];
    public AlchemyStorageManualScreen(){super(Component.translatable("item.jugcraft.alchemy_manual"));}
    private static Component ui(String key){return Component.translatable("screen.jugcraft.alchemy_storage.manual."+key);}
    @Override protected void init(){
        int x=(width-360)/2,y=(height-238)/2;
        for(int i=0;i<6;i++){final int t=i;tabs[i]=addRenderableWidget(new TransmutationScreen.ActionButton(x+13,y+43+i*26,95,ui("title_"+i),b->topic=t));}
        addRenderableWidget(new TransmutationScreen.ActionButton(x+120,y+208,110,Component.translatable("screen.jugcraft.alchemy.gem_controls"),b->minecraft.gui.setScreen(new GemAbilitiesScreen())));
        addRenderableWidget(new TransmutationScreen.ActionButton(x+236,y+208,110,ui("close"),b->onClose()));
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta){
        int x=(width-360)/2,y=(height-238)/2;g.fill(0,0,width,height,0xB0081018);
        OwnerAlchemyArt.frame(g,x,y,360,238);
        OwnerAlchemyArt.panel(g,x+117,y+37,230,164,OwnerAlchemyArt.TEAL);
        BookArt.icon(g,"jugcraft:alchemy_manual",x+16,y+13);
        g.text(font,title,x+39,y+17,0xFFE5DDEB,false);
        g.text(font,ui("title_"+topic),x+129,y+49,OwnerAlchemyArt.GOLD,false);
        g.fill(x+129,y+62,x+335,y+63,0xFF3D5360);
        int yy=y+72;for(var line:font.split(ui("body_"+topic),206)){g.text(font,line,x+129,yy,0xFFCFDADD,false);yy+=11;}
        for(int i=0;i<tabs.length;i++)tabs[i].selected=i==topic;
        super.extractRenderState(g,mx,my,delta);
    }
    @Override public boolean isPauseScreen(){return false;}
}
