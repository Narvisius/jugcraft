package io.github.jimbozoomer.jugcraft.client.jimtafacts;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks.JimtaScribing;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import io.github.jimbozoomer.jugcraft.client.ScreenKit;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Source forge atlas, ingredient and result previews, finite selection buttons; no typed spell IDs. */
public final class JimtaScribingScreen extends AbstractContainerScreen<JimtaScribing.Menu> implements ScreenKit.Fitted {
    private Button back,next,apply;
    public JimtaScribingScreen(JimtaScribing.Menu menu,Inventory inventory,Component title){super(menu,inventory,title,216,166);}
    @Override protected void init(){super.init();back=button(0,108,50,22,"<");next=button(1,134,50,22,">");apply=button(2,105,18,51,"Create");}
    private Button button(int action,int x,int y,int width,String label){return addRenderableWidget(Button.builder(Component.literal(label),b->{if(minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,action);}).bounds(leftPos+x,topPos+y,width,20).build());}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int x,int y,float partial){
        back.visible=next.visible=menu.mode()==1;back.active=menu.cursor()>0;next.active=menu.cursor()+1<menu.count();apply.active=menu.canApply();
        apply.setMessage(Component.literal(menu.mode()==0?"Inscribe":menu.mode()==1?"Create":"Improve"));
        extractBackground(g,x,y,partial);super.extractRenderState(g,x,y,partial);extractTooltip(g,x,y);
    }
    @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float partial){
        g.fill(0,0,width,height,0xB00C0914);
        g.blit(RenderPipelines.GUI_TEXTURED,Jugcraft.id("textures/gui/jimtafacts/scroll_forge.png"),leftPos,topPos,0,0,216,166,216,166,256,256);
        g.fill(leftPos+7,topPos+8,leftPos+208,topPos+76,0xFF282331);
        g.fill(leftPos+7,topPos+8,leftPos+208,topPos+10,0xFFAF88C7);
        for(int i=0;i<10;i++){var slot=menu.slots.get(i);if(!slot.isActive())continue;int x=leftPos+slot.x,y=topPos+slot.y;
            g.fill(x-1,y-1,x+17,y+17,i>=5?0xFF80648F:0xFF18151D);g.fill(x,y,x+16,y+16,i>=5?0xFF352C40:0xFF5B5266);
        }
    }
    /** The forge and its title, written just above it. */
    @Override public ScreenRectangle panelArea(){return new ScreenRectangle(leftPos,topPos-12,imageWidth,imageHeight+12);}
    @Override protected void extractLabels(GuiGraphicsExtractor g,int mx,int my){
        String label=title.getString();if(font.width(label)>208)label=font.plainSubstrByWidth(label,204);
        g.text(font,label,6,-12,0xFFEBD4FF,true);
        g.text(font,"Inputs",12,10,0xFFE4DAEF,false);g.text(font,menu.mode()==1?"Output":"Preview",166,10,0xFFE4DAEF,false);
        if(menu.mode()==1){g.text(font,"Recipe",12,40,0xFFBDAACD,false);g.text(font,(menu.cursor()+1)+" / "+menu.count(),108,40,0xFFBDAACD,false);}
        else{
            String info=menu.mode()==0?"Book + scroll":"Book + upgrade; armor + ingot";
            var lines=font.split(Component.literal(info),145);for(int i=0;i<lines.size();i++)g.text(font,lines.get(i),12,49+i*10,0xFFDBC9EB,false);
        }
    }
}
