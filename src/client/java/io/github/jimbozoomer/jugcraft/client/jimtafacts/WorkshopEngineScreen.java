package io.github.jimbozoomer.jugcraft.client.jimtafacts;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.jimtafacts.industry.WorkshopEngines;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class WorkshopEngineScreen extends AbstractContainerScreen<WorkshopEngines.Menu> {
    public WorkshopEngineScreen(WorkshopEngines.Menu menu,Inventory inv,Component title){super(menu,inv,title,176,202);}
    @Override protected void init(){super.init();titleLabelY=inventoryLabelY=10000;}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float dt){extractBackground(g,mx,my,dt);super.extractRenderState(g,mx,my,dt);extractTooltip(g,mx,my);}
    @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float dt){
        int kind=Math.clamp(menu.data.get(0),0,5);String source=kind==2?"combustion":kind==5?"dynamo":kind==3?"electric":"stirling";
        g.blit(RenderPipelines.GUI_TEXTURED,Jugcraft.id("textures/gui/jimtafacts/engine_"+source+".png"),leftPos,topPos,0,0,176,176,256,256);
        g.fill(leftPos+5,topPos+5,leftPos+171,topPos+89,0xff263238);g.fill(leftPos+1,topPos+176,leftPos+175,topPos+201,0xff263238);
        g.text(font,title,leftPos+8,topPos+9,0xffebf4f2,false);g.text(font,menu.data.get(1)+" / 32,000 JE",leftPos+8,topPos+23,0xffbfd8dc,false);
        String status=menu.data.get(6)==1?"Cooling after overheat":menu.data.get(8)==0?"Supply redstone to run":"Running when resources fit";
        g.text(font,status,leftPos+8,topPos+178,0xffe4edeb,false);g.text(font,"Power leaves the piston end",leftPos+8,topPos+190,0xffb5cdd4,false);
        if(kind==1){g.text(font,"Burn: "+menu.data.get(2)+" ticks",leftPos+31,topPos+44,0xffe4edeb,false);g.text(font,"Ash output",leftPos+31,topPos+64,0xffb5cdd4,false);}
        else if(kind==2){g.text(font,"Fuel: "+menu.data.get(3)+" mB",leftPos+8,topPos+39,0xffe4edeb,false);g.text(font,"Water: "+menu.data.get(4)+" mB (bottom)",leftPos+8,topPos+52,0xffe4edeb,false);g.text(font,"Heat: "+menu.data.get(5)+" / 100",leftPos+8,topPos+66,0xffe4edeb,false);}
        else{String[] lines=kind==5?new String[]{menu.data.get(7)+" / 4,096 KE", "Back shaft → JE at 85%"}:kind==0?new String[]{"1 JE per second", "Starter redstone piston"}:kind==4?new String[]{"2,048 JE per tick", "Creative owner required"}:new String[]{"JE in: other faces", "JE out: piston end"};
            for(int i=0;i<lines.length;i++)g.text(font,lines[i],leftPos+8,topPos+43+14*i,0xffe4edeb,false);}
        for(var slot:menu.slots)if(slot.isActive()){int x=leftPos+slot.x,y=topPos+slot.y;g.fill(x-1,y-1,x+17,y+17,0xff101a1f);g.fill(x,y,x+16,y+16,0xff879497);}
    }
    @Override protected void extractLabels(GuiGraphicsExtractor g,int x,int y){}
}
