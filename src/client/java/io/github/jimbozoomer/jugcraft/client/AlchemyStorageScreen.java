package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.concordance.assay.AlchemyStorageMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** A quiet 54-slot cabinet, with its physical bag lock shown beside the player inventory. */
public final class AlchemyStorageScreen extends AbstractContainerScreen<AlchemyStorageMenu> {
    public static final int PANEL_WIDTH=286,PANEL_HEIGHT=232;
    public AlchemyStorageScreen(AlchemyStorageMenu menu,Inventory inv,Component title){super(menu,inv,title,PANEL_WIDTH,PANEL_HEIGHT);}
    @Override protected void init(){super.init();
        for(int i=0;i<3;i++){final int action=i;var button=addRenderableWidget(new TransmutationScreen.ActionButton(leftPos+207,topPos+40+i*28,64,
            Component.translatable("screen.jugcraft.alchemy_storage."+new String[]{"sort","stack","pause"}[i]),b->{if(minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,action);}));
            button.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("screen.jugcraft.alchemy_storage.controls_help")));
        }
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta){extractBackground(g,mx,my,delta);super.extractRenderState(g,mx,my,delta);extractTooltip(g,mx,my);}
    @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float delta){
        int x=leftPos,y=topPos;
        g.fill(0,0,width,height,0xB0081018);
        OwnerAlchemyArt.frame(g,x,y,imageWidth,imageHeight);

        OwnerAlchemyArt.panel(g,x+20,y+27,175,110,OwnerAlchemyArt.TEAL);
        OwnerAlchemyArt.panel(g,x+20,y+147,175,80,OwnerAlchemyArt.SILVER);
        for(var slot:menu.slots){
            int sx=x+slot.x,sy=y+slot.y;OwnerAlchemyArt.slot(g,"alchchest",sx,sy,11,3);
            if(slot.index>=54&&slot.getContainerSlot()==menu.lockedSlot())g.fill(sx-1,sy+17,sx+17,sy+19,OwnerAlchemyArt.GOLD);
        }
        OwnerAlchemyArt.sigil(g,x+238,y+165,21,OwnerAlchemyArt.VIOLET,false,delta);
        BookArt.icon(g,"jugcraft:alchemical_chest",x+230,y+157);

    }
    @Override protected void extractLabels(GuiGraphicsExtractor g,int mx,int my){
        g.text(font,BookArt.fit(font,title.getString(),180),19,7,0xFFE5DDEB,false);
        g.text(font,BookArt.fit(font,Component.translatable("screen.jugcraft.alchemy_storage.no_nesting").getString(),175),20,18,0xFFA5BCC7,false);
        g.text(font,BookArt.fit(font,Component.translatable("screen.jugcraft.alchemy_storage.inventory").getString(),168),27,138,0xFFCEC5DF,false);
    }
}
