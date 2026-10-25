package io.github.jimbozoomer.jugcraft.client.jimtafacts;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.jimtafacts.industry.IndustrialFluidServices;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Original source GUI sheets, readable native tank gauges and click-only rate controls. */
public final class IndustrialFluidServiceScreen extends AbstractContainerScreen<IndustrialFluidServices.Menu> {
    private Button slower,faster,mode;
    public IndustrialFluidServiceScreen(IndustrialFluidServices.Menu menu,Inventory inv,Component title){super(menu,inv,title,176,218);}
    private void action(int id){if(minecraft!=null&&minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,id);}
    @Override protected void init(){super.init();titleLabelY=inventoryLabelY=10000;
        slower=addRenderableWidget(Button.builder(Component.literal("−"),b->action(0)).bounds(leftPos+8,topPos+170,22,18).build());
        faster=addRenderableWidget(Button.builder(Component.literal("+"),b->action(1)).bounds(leftPos+146,topPos+170,22,18).build());
        mode=addRenderableWidget(Button.builder(Component.empty(),b->action(2)).bounds(leftPos+8,topPos+192,160,18).build());
    }
    @Override protected void containerTick(){super.containerTick();int kind=menu.data.get(0);boolean routing=kind<2;
        slower.visible=faster.visible=mode.visible=routing;slower.active=menu.data.get(4)>0;faster.active=menu.data.get(4)<IndustrialFluidServices.RATES.length-1;
        mode.setMessage(Component.literal(kind==0?(menu.data.get(5)==0?"Per second":"Per tick"):(menu.data.get(5)==0?"Spread across outlets":"One outlet at a time")));
        mode.setTooltip(Tooltip.create(Component.literal(kind==0?"10 JE per successful delivery. The front is the output; all other faces take fluid. Redstone pauses.":"The front is the inlet. Other faces are outlets. Rotates priority each second; never loads chunks. Redstone pauses.")));
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float dt){extractBackground(g,mx,my,dt);super.extractRenderState(g,mx,my,dt);extractTooltip(g,mx,my);}
    @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float dt){
        int kind=Math.clamp(menu.data.get(0),0,3);String name=IndustrialFluidServices.KINDS.get(kind);
        g.blit(RenderPipelines.GUI_TEXTURED,Jugcraft.id("textures/gui/jimtafacts/"+name+".png"),leftPos,topPos,0,0,176,166,256,256);
        // Replace obsolete source input-slot widgets; buckets/cells use the physical tank ports.
        g.fill(leftPos+6,topPos+5,leftPos+170,topPos+80,0xff26353c);g.fill(leftPos+1,topPos+166,leftPos+175,topPos+217,0xff26353c);
        g.text(font,title,leftPos+10,topPos+9,0xffeef6f5,false);
        String fluid=menu.data.get(7)==0?"Empty":BuiltInRegistries.FLUID.getKey(BuiltInRegistries.FLUID.byId(menu.data.get(7))).getPath().replace('_',' ');
        g.text(font,font.plainSubstrByWidth(fluid,148),leftPos+10,topPos+24,0xffbfdadf,false);
        gauge(g,42,menu.data.get(1),0xff527eac);g.text(font,"In: "+menu.data.get(1)+" / 10,000 mB",leftPos+10,topPos+35,0xffedf4f5,false);
        if(kind>=2){gauge(g,63,menu.data.get(2),0xffa0c8db);g.text(font,"Out: "+menu.data.get(2)+" mB distilled",leftPos+10,topPos+56,0xffedf4f5,false);}
        else g.text(font,kind==0?menu.data.get(3)+" / 10,000 JE":"No electricity required",leftPos+10,topPos+59,0xffedf4f5,false);
        if(kind<2){int rate=IndustrialFluidServices.RATES[Math.clamp(menu.data.get(4),0,6)];g.text(font,rate+" mB / delivery",leftPos+35,topPos+175,0xffedf4f5,false);}
        else {String[] states={"Working","Access denied","Redstone pause","Needs power","Input / output blocked","Needs open daylight"};
            g.text(font,states[Math.clamp(menu.data.get(6),0,5)],leftPos+8,topPos+171,0xffedf4f5,false);
            g.text(font,kind==2?"1 water → 1 distilled / 72 ticks":"10 steam → 1 distilled",leftPos+8,topPos+185,0xffb5d4df,false);
            g.text(font,"Input: sides · Output: bottom",leftPos+8,topPos+199,0xffb5d4df,false);}
    }
    private void gauge(GuiGraphicsExtractor g,int y,int amount,int color){g.fill(leftPos+10,topPos+y,leftPos+164,topPos+y+5,0xff101b21);g.fill(leftPos+10,topPos+y,leftPos+10+Math.clamp(amount,0,10000)*154/10000,topPos+y+5,color);}
    @Override protected void extractLabels(GuiGraphicsExtractor g,int x,int y){}
}
