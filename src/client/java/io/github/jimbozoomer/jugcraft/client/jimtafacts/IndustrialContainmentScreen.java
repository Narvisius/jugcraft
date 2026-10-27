package io.github.jimbozoomer.jugcraft.client.jimtafacts;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.jimtafacts.industry.IndustrialContainment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Unchanged supplied case artwork with its original 4x3 slot positions. */
public final class IndustrialContainmentScreen extends AbstractContainerScreen<IndustrialContainment.Menu> {
    public IndustrialContainmentScreen(IndustrialContainment.Menu m,Inventory inv,Component title){super(m,inv,title,176,166);inventoryLabelY=73;}
    @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float dt){g.blit(RenderPipelines.GUI_TEXTURED,Jugcraft.id("textures/gui/jimtafacts/containment_box.png"),leftPos,topPos,0,0,176,166,256,256);}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float dt){extractBackground(g,mx,my,dt);super.extractRenderState(g,mx,my,dt);extractTooltip(g,mx,my);}
    @Override protected void extractLabels(GuiGraphicsExtractor g,int mx,int my){g.text(font,title,8,6,0xff313129);}
}
