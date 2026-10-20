package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.nouveau.StorageLecternMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Browse real storage without commands or typing; owner models and color form the visual identity. */
public final class StorageLecternScreen extends AbstractContainerScreen<StorageLecternMenu> {
    private TransmutationScreen.ActionButton previous,next,unlink;
    private int helpStep;
    public StorageLecternScreen(StorageLecternMenu menu,Inventory inventory,Component title){super(menu,inventory,title,304,198);}
    @Override protected void init(){
        super.init();previous=action(0,58,"previous");next=action(1,80,"next");unlink=action(2,102,"unlink");
        var help=addRenderableWidget(new TransmutationScreen.ActionButton(leftPos+198,topPos+173,88,Component.translatable("screen.jugcraft.bookwyrm.help_button"),b->helpStep=(helpStep+1)%4));
        help.setTooltip(Tooltip.create(Component.translatable("screen.jugcraft.bookwyrm.help")));
    }
    private TransmutationScreen.ActionButton action(int action,int y,String label){return addRenderableWidget(new TransmutationScreen.ActionButton(leftPos+198,topPos+y,88,
        Component.translatable("screen.jugcraft.bookwyrm."+label),b->{if(minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,action);}));}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta){
        previous.active=menu.page()>0&&menu.getCarried().isEmpty();next.active=menu.page()+1<menu.pages()&&menu.getCarried().isEmpty();unlink.active=menu.pages()>0&&menu.getCarried().isEmpty();
        extractBackground(g,mx,my,delta);super.extractRenderState(g,mx,my,delta);extractTooltip(g,mx,my);
    }
    @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float delta){
        int x=leftPos,y=topPos;g.fill(0,0,width,height,0xB00C0719);OwnerAlchemyArt.frame(g,x,y,imageWidth,imageHeight);
        OwnerAlchemyArt.panel(g,x+14,y+38,174,62,OwnerAlchemyArt.VIOLET);OwnerAlchemyArt.panel(g,x+14,y+110,174,82,OwnerAlchemyArt.GOLD);
        for(var slot:menu.slots){OwnerAlchemyArt.slot(g,"alchchest",x+slot.x,y+slot.y,11,3);}
        OwnerAlchemyArt.sigil(g,x+277,y+22,12,OwnerAlchemyArt.VIOLET,menu.online(),delta);BookArt.icon(g,"jugcraft:bookwyrm_charm",x+269,y+14);
        var lines=font.split(Component.translatable("screen.jugcraft.bookwyrm.step"+helpStep),88);
        for(int i=0;i<lines.size();i++)g.text(font,lines.get(i),x+198,y+127+i*9,OwnerAlchemyArt.SILVER,false);
        if(!menu.online()){
            g.fill(x+16,y+40,x+186,y+98,0xBC10131F);
            String text=Component.translatable("screen.jugcraft.bookwyrm."+(menu.pages()==0?"empty":"offline")).getString();
            g.text(font,text,x+101-font.width(text)/2,y+65,OwnerAlchemyArt.GOLD,false);
        }
    }
    @Override protected void extractLabels(GuiGraphicsExtractor g,int mx,int my){
        g.text(font,title,18,10,0xFFE8DAFF,false);
        g.text(font,Component.translatable("screen.jugcraft.bookwyrm.wyrms",menu.wyrms()),18,24,OwnerAlchemyArt.VIOLET,false);
        g.text(font,BookArt.fit(font,Component.translatable("screen.jugcraft.bookwyrm.page",menu.pages()==0?0:menu.page()+1,menu.pages()).getString(),90),198,44,OwnerAlchemyArt.SILVER,false);
        g.text(font,Component.translatable("screen.jugcraft.bookwyrm.inventory"),20,103,OwnerAlchemyArt.SILVER,false);
    }
}
