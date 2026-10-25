package io.github.jimbozoomer.jugcraft.client.jimtafacts;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.jimtafacts.industry.IndustrialLathe;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Source 120×32 blank preview and GUI, with five explicit no-typing cut buttons. */
public final class IndustrialLatheScreen extends AbstractContainerScreen<IndustrialLathe.Menu> {
    public IndustrialLatheScreen(IndustrialLathe.Menu menu,Inventory inv,Component title){super(menu,inv,title,176,192);}
    private void action(int id){if(minecraft!=null&&minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,id);}
    @Override protected void init(){super.init();titleLabelY=inventoryLabelY=10000;
        for(int i=0;i<5;i++){int part=i;addRenderableWidget(Button.builder(Component.literal("−"),b->action(part)).bounds(leftPos+40+i*24,topPos+15,24,20)
            .tooltip(Tooltip.create(Component.literal("Cut segment "+(i+1)+" by one step. Costs 1,000 KE and one tool use. Cuts cannot be undone."))).build());}
        addRenderableWidget(Button.builder(Component.literal("Finish"),b->action(5)).bounds(leftPos+116,topPos+168,52,20).tooltip(Tooltip.create(Component.literal("Finish a matching profile into its component. Take it from the blank slot."))).build());
        addRenderableWidget(Button.builder(Component.literal("?"),b->{}).bounds(leftPos+28,topPos+57,14,18).tooltip(Tooltip.create(Component.literal(
            "Top slot: blank. Middle slot: lathing tool. Bottom slot: shavings. Supply KE through a shaft; redstone pauses.\nIron profiles:\n4·3·4·3·4 — fluted iron shaft\n3·2·3·2·3 — thin fluted axle\n4·3·2·2·2 — handle\n5·4·3·2·1 — drill head\nWood: 2·3·2·3·2 — four rods\nAn over-cut blank keeps its shape; it cannot regrow material."))).build());
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float dt){extractBackground(g,mx,my,dt);super.extractRenderState(g,mx,my,dt);extractTooltip(g,mx,my);}
    @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float dt){
        g.blit(RenderPipelines.GUI_TEXTURED,Jugcraft.id("textures/gui/jimtafacts/lathe.png"),leftPos,topPos,0,0,176,166,256,256);
        g.fill(leftPos+1,topPos+166,leftPos+175,topPos+191,0xff202c31);
        var blank=menu.contents.getItem(1);int max=IndustrialLathe.width(blank);int[] widths=IndustrialLathe.profile(blank);
        if(max>0){var texture=Jugcraft.id("textures/gui/jimtafacts/turnable_"+(max==3?"wood":"iron")+".png");
            for(int i=0;i<5;i++){int size=Math.round(32F*widths[i]/max),offset=Math.round((32-size)/2F);
                g.text(font,widths[i]+"/"+max,leftPos+42+i*24,topPos+37,0xff404040,false);
                g.blit(RenderPipelines.GUI_TEXTURED,texture,leftPos+40+i*24,topPos+45+offset,i*24,offset,24,size,256,256);
            }
        }
        String[] status={"Ready","Redstone pause","Insert blank/tool","At minimum","Needs 1,000 KE","Clear shavings","Cut complete","Profile unmatched","Finished"};
        g.text(font,menu.data.get(0)+" / 10,000 KE",leftPos+7,topPos+168,0xffc2dedc,false);
        g.text(font,status[Math.clamp(menu.data.get(1),0,8)],leftPos+7,topPos+180,0xffdbe7ea,false);
    }
    @Override protected void extractLabels(GuiGraphicsExtractor g,int x,int y){}
}
