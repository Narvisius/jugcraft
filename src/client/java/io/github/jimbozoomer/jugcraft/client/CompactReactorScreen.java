package io.github.jimbozoomer.jugcraft.client;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.jimtafacts.industry.CompactReactor;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Original source reactor panel, with explicit start/stop, heat and unavailable chamber columns. */
public final class CompactReactorScreen extends AbstractContainerScreen<CompactReactor.Menu> {
    private Button control;
    public CompactReactorScreen(CompactReactor.Menu menu,Inventory inventory,Component title){super(menu,inventory,title,232,244);}
    @Override protected void init(){super.init();titleLabelX=26;titleLabelY=7;inventoryLabelY=151;inventoryLabelX=26;
        control=addRenderableWidget(Button.builder(Component.literal("Start"),button->{if(minecraft!=null&&minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,0);}).bounds(leftPos+6,topPos+134,70,16).build());
        control.setTooltip(Tooltip.create(Component.literal("Stop before changing components. Redstone pauses fuel use. Add adjacent chambers to unlock columns. After a fault, cool below 50% before restarting.")));
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mouseX,int mouseY,float delta){
        control.setMessage(Component.literal(menu.data.get(4)!=0?"Stop":menu.data.get(5)!=0?"Reset / start":"Start"));
        control.setTooltip(Tooltip.create(Component.literal("Stop before changing components. Redstone pauses fuel. Cool below 50% after a fault. "+(menu.data.get(7)==1?"Fluid vessel: "+menu.data.get(8)+" mB coolant / "+menu.data.get(9)+" mB hot coolant. No JE is generated in this mode.":"Electrical mode. A complete 5×5×5 vessel enables coolant heat production."))));
        extractBackground(g,mouseX,mouseY,delta);super.extractRenderState(g,mouseX,mouseY,delta);extractTooltip(g,mouseX,mouseY);
    }
    @Override public void extractBackground(GuiGraphicsExtractor g,int mouseX,int mouseY,float delta){
        g.blit(RenderPipelines.GUI_TEXTURED,Jugcraft.id("textures/gui/jimtafacts/compact_reactor.png"),leftPos,topPos,0,0,imageWidth,imageHeight,256,256);
        int columns=menu.data.get(3);for(int c=columns;c<9;c++)g.fill(leftPos+25+c*18,topPos+24,leftPos+43+c*18,topPos+132,0xBB202831);
        int heat=menu.data.get(0),max=Math.max(1,menu.data.get(1)),fill=Math.clamp(heat*48/max,0,48);
        g.fill(leftPos+216,topPos+22,leftPos+226,topPos+70,0xFF25272C);g.fill(leftPos+216,topPos+70-fill,leftPos+226,topPos+70,heat*2>=max?0xFFDA5B36:0xFF5BBD8A);
    }
    @Override protected void extractLabels(GuiGraphicsExtractor g,int mouseX,int mouseY){
        g.text(font,"Component Reactor",26,7,0xFF30363A);g.text(font,"Inventory",26,151,0xFF30363A);
        String status=menu.data.get(5)!=0?"FAULT — cool down":(menu.data.get(7)==1?(menu.data.get(2)*200)+" heat/s  ":(menu.data.get(2)*5)+" JE/t  ")+(menu.data.get(0)*100/Math.max(1,menu.data.get(1)))+"% heat";
        g.text(font,status,80,138,menu.data.get(5)!=0?0xFFFFA575:0xFFE5E7DF);
    }
}
