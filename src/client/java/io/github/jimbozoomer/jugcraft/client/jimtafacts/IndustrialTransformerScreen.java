package io.github.jimbozoomer.jugcraft.client.jimtafacts;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.jimtafacts.industry.IndustrialTransformers;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class IndustrialTransformerScreen extends AbstractContainerScreen<IndustrialTransformers.Menu> {
    private final Button[] modes=new Button[3];
    public IndustrialTransformerScreen(IndustrialTransformers.Menu menu,Inventory inv,Component title){super(menu,inv,title,176,219);}
    @Override protected void init(){super.init();titleLabelY=inventoryLabelY=10000;
        String[] names={"Automatic (redstone)","Split front → other faces","Combine faces → front"};
        for(int i=0;i<3;i++){int action=i;modes[i]=addRenderableWidget(Button.builder(Component.literal(names[i]),b->{if(minecraft!=null&&minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,action);}).bounds(leftPos+8,topPos+65+20*i,160,19).tooltip(Tooltip.create(Component.literal(i==0?"No signal splits; redstone combines. The marked face is the high-rate port.":"All energy remains JE. No conversion gain, explosion or voltage requirement. Rates are shared across ports each tick."))).build());}
    }
    @Override protected void containerTick(){super.containerTick();for(int i=0;i<3;i++)modes[i].active=i!=menu.data.get(2);}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float dt){extractBackground(g,mx,my,dt);super.extractRenderState(g,mx,my,dt);extractTooltip(g,mx,my);}
    @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float dt){
        g.blit(RenderPipelines.GUI_TEXTURED,Jugcraft.id("textures/gui/jimtafacts/transformer.png"),leftPos,topPos,0,0,176,219,256,256);
        g.fill(leftPos+7,topPos+7,leftPos+169,topPos+61,0xff25323b);int low=menu.data.get(0),high=low*4;
        g.text(font,title,leftPos+10,topPos+10,0xffe2edef,false);
        g.text(font,menu.data.get(3)==0?"Front IN · other faces OUT":"Front OUT · other faces IN",leftPos+10,topPos+24,0xffe2edef,false);
        g.text(font,high+" total JE/t · "+low+" per side",leftPos+10,topPos+37,0xffb6d0d7,false);
        g.text(font,(menu.data.get(1)*4)+" / "+(high*8)+" JE stored",leftPos+10,topPos+50,0xffb6d0d7,false);
    }
    @Override protected void extractLabels(GuiGraphicsExtractor g,int x,int y){}
}
