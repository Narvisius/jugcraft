package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.botany.BotanicalHalos;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** Native crafting controls with a separate eleven-recipe memory rail. */
final class BotanicalHaloScreen extends AbstractContainerScreen<BotanicalHalos.Menu> {
    private static final Identifier TABLE=Identifier.withDefaultNamespace("textures/gui/container/crafting_table.png");
    static void register(){MenuScreens.register(BotanicalHalos.MENU,BotanicalHaloScreen::new);}
    BotanicalHaloScreen(BotanicalHalos.Menu menu,Inventory inv,Component title){super(menu,inv,title,238,195);inventoryLabelY=100;}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float dt){extractBackground(g,mx,my,dt);super.extractRenderState(g,mx,my,dt);extractTooltip(g,mx,my);}
    @Override public void extractLabels(GuiGraphicsExtractor g,int x,int y){
        g.text(font,title,8,5,0xFF30352F,false);g.text(font,playerInventoryTitle,8,100,0xFF30352F,false);
        g.text(font,Component.literal("Recipes"),185,6,0xFFE6D68A,false);
        g.text(font,Component.literal("Click: craft / save"),8,76,0xFF435044,false);
        g.text(font,Component.literal("Shift-click: replace · Right-click: forget"),8,87,0xFF435044,false);
    }
    @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float dt){
        g.fill(leftPos,topPos,leftPos+238,topPos+195,0xFF202D28);g.fill(leftPos+2,topPos+2,leftPos+236,topPos+193,0xFF6A7653);
        g.blit(RenderPipelines.GUI_TEXTURED,TABLE,leftPos,topPos,0,0,176,81,256,256);
        g.fill(leftPos+4,topPos+73,leftPos+173,topPos+111,0xFFC6C6C6);
        g.blit(RenderPipelines.GUI_TEXTURED,TABLE,leftPos,topPos+109,0,81,176,85,256,256);
        g.fill(leftPos+181,topPos+3,leftPos+235,topPos+143,0xFF202D28);
        for(int i=0;i<11;i++){var s=menu.getSlot(10+i);g.fill(leftPos+s.x-1,topPos+s.y-1,leftPos+s.x+17,topPos+s.y+17,0xFFB4A261);g.fill(leftPos+s.x,topPos+s.y,leftPos+s.x+16,topPos+s.y+16,0xFF435044);}
        // Original halo glow artwork supplies the ornament; no replacement bitmap is generated.
        g.blit(RenderPipelines.GUI_TEXTURED,Jugcraft.id("textures/newest/jugtania/misc/glow0.png"),leftPos+183,topPos+147,0,0,48,40,48,40);
    }
}
