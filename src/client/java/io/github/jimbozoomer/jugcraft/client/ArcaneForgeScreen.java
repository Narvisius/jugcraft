package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.arcane.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Original forge artwork and slot positions, with synchronized essence amounts. */
final class ArcaneForgeScreen extends AbstractContainerScreen<ArcaneForgeMenu> {
    private static final int[] X={12,24,148,160},UV={177,183,189,195};
    private static final String[] NAMES={"Aureal","Souls","Blood","Experience"};
    static void register(){MenuScreens.register(ForbiddenArcane.FORGE_MENU,ArcaneForgeScreen::new);ArcaneWorkshopRenderer.register();ArcaneJarRenderer.register();ArcaneMissileRenderer.register();ArcaneItemDetails.register();ArcaneSkullRenderer.register();ArcaneClibanoScreen.register();}
    ArcaneForgeScreen(ArcaneForgeMenu menu,Inventory inv,Component title){super(menu,inv,title,176,166);}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float dt){extractBackground(g,mx,my,dt);super.extractRenderState(g,mx,my,dt);extractTooltip(g,mx,my);}
    @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float dt){
        var texture=Jugcraft.id("textures/newest/forbidden/gui/container/hephaestus_forge.png");
        g.blit(RenderPipelines.GUI_TEXTURED,texture,leftPos,topPos,0,0,176,166,256,256);
        g.blit(RenderPipelines.GUI_TEXTURED,texture,leftPos-26,topPos+16,176,61,29,51,256,256);
        g.blit(RenderPipelines.GUI_TEXTURED,texture,leftPos+172,topPos+16,206,61,29,51,256,256);
        for(int i=0;i<4;i++){int height=(int)(32L*menu.essence(i)/ArcaneWorkshop.capacity(menu.tier(),i));height=Math.clamp(height,0,32);if(height>0)g.blit(RenderPipelines.GUI_TEXTURED,texture,leftPos+X[i],topPos+54-height,UV[i],35-height,4,height,256,256);}
        for(int i=Math.min(4,menu.tier());i<4;i++){var slot=menu.getSlot(i);g.blit(RenderPipelines.GUI_TEXTURED,texture,leftPos+slot.x-2,topPos+slot.y-2,176,40,20,20,256,256);}
        if(menu.running()){g.fill(leftPos+58,topPos+63,leftPos+118,topPos+67,0xFF32243A);g.fill(leftPos+58,topPos+63,leftPos+58+60*menu.progress()/1000,topPos+67,0xFFD4A345);}
    }
    @Override public void extractLabels(GuiGraphicsExtractor g,int mx,int my){super.extractLabels(g,mx,my);g.text(font,Component.literal("Tier "+menu.tier()),69,49,0xFF66533D,false);}
    @Override protected void extractTooltip(GuiGraphicsExtractor g,int mx,int my){
        super.extractTooltip(g,mx,my);
        for(int i=0;i<4;i++)if(mx>=leftPos+X[i]-2&&mx<=leftPos+X[i]+6&&my>=topPos+20&&my<=topPos+56)g.setTooltipForNextFrame(font,Component.literal(NAMES[i]+": "+menu.essence(i)+" / "+ArcaneWorkshop.capacity(menu.tier(),i)),mx,my);
    }
}
