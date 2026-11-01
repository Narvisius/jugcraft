package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.arcane.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Original Clibano artwork, inventory layout, flame and both progress arrows. */
final class ArcaneClibanoScreen extends AbstractContainerScreen<ArcaneClibanoMenu> {
    static void register(){MenuScreens.register(ForbiddenArcane.CLIBANO_MENU,ArcaneClibanoScreen::new);ArcaneBoatRenderer.register();ArcaneInjectorRenderer.register();ArcaneSight.register();}
    ArcaneClibanoScreen(ArcaneClibanoMenu m,Inventory inv,Component title){super(m,inv,title,176,173);inventoryLabelY=80;}
    @Override protected void init(){super.init();var b=addRenderableWidget(Button.builder(Component.literal("Recover residue"),button->{if(minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,0);}).bounds(leftPos,topPos+imageHeight+5,176,20).build());b.setTooltip(Tooltip.create(Component.literal("Complete residue batches normally enter the output slots automatically. Recover them here if those slots are occupied.")));}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float dt){extractBackground(g,mx,my,dt);super.extractRenderState(g,mx,my,dt);extractTooltip(g,mx,my);}
    @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float dt){
        var texture=Jugcraft.id("textures/newest/forbidden/gui/container/clibano_combustion.png");g.blit(RenderPipelines.GUI_TEXTURED,texture,leftPos,topPos,0,0,176,173,256,256);
        for(int i=0;i<2;i++){int width=Math.clamp(24*menu.value(4+i)/Math.max(1,menu.value(6+i)),0,24);if(width>0)g.blit(RenderPipelines.GUI_TEXTURED,texture,leftPos+83,topPos+34+i*9,176,32+i*9,width,7,256,256);}
        int heat=Math.clamp(15*menu.value(0)/Math.max(1,menu.value(1)),0,15);if(heat>0)g.blit(RenderPipelines.GUI_TEXTURED,texture,leftPos+55,topPos+54-heat,176+menu.value(3)*13,16-heat,12,heat,256,256);
        int residue=Math.clamp(menu.value(8)*47/64,0,47);if(residue>0)g.blit(RenderPipelines.GUI_TEXTURED,texture,leftPos+111,topPos+58,179,21,residue,7,256,256);
    }
    @Override protected void extractTooltip(GuiGraphicsExtractor g,int x,int y){super.extractTooltip(g,x,y);if(x>=leftPos+110&&x<leftPos+162&&y>=topPos+55&&y<topPos+69)g.setTooltipForNextFrame(font,Component.literal("Residue: "+menu.value(8)+" / 64 · soul fire produces bonus residue"),x,y);if(x>=leftPos+50&&x<leftPos+73&&y>=topPos+37&&y<topPos+56)g.setTooltipForNextFrame(font,Component.literal(new String[]{"Fire","Soul fire","Enchanted fire"}[Math.clamp(menu.value(3),0,2)]+" · "+menu.value(2)/20+"s soul charge"),x,y);}
}
