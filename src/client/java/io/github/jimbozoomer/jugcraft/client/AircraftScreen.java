package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.aircraft.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** The source equipment/cargo layout with explicit socket labels and live fuel status. */
public final class AircraftScreen extends AbstractContainerScreen<AircraftMenu> {
    private float scale=1;
    public AircraftScreen(AircraftMenu menu,Inventory inv,Component title){super(menu,inv,title,menu.spec.width(),menu.spec.height());inventoryLabelX=(imageWidth-162)/2;inventoryLabelY=menu.spec.inventoryY()-12;}
    @Override protected void init(){super.init();scale=Math.min(1F,Math.min((width-12F)/imageWidth,(height-12F)/imageHeight));leftPos=Math.round((width/scale-imageWidth)/2);topPos=Math.round((height/scale-imageHeight)/2);}
    private net.minecraft.client.input.MouseButtonEvent scaled(net.minecraft.client.input.MouseButtonEvent e){return new net.minecraft.client.input.MouseButtonEvent(e.x()/scale,e.y()/scale,e.buttonInfo());}
    @Override public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent e,boolean twice){return super.mouseClicked(scaled(e),twice);}
    @Override public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent e){return super.mouseReleased(scaled(e));}
    @Override public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent e,double dx,double dy){return super.mouseDragged(scaled(e),dx/scale,dy/scale);}
    @Override public boolean mouseScrolled(double x,double y,double dx,double dy){return super.mouseScrolled(x/scale,y/scale,dx,dy);}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int x,int y,float delta){
        int mx=Math.round(x/scale),my=Math.round(y/scale);g.pose().pushMatrix();g.pose().scale(scale,scale);
        super.extractRenderState(g,mx,my,delta);g.pose().popMatrix();
    }
    @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float delta){
        mx=Math.round(mx/scale);my=Math.round(my/scale);g.pose().pushMatrix();g.pose().scale(scale,scale);
        int x=leftPos,y=topPos;
        g.fill(x,y,x+imageWidth,y+imageHeight,0xFF15222A);g.fill(x+2,y+2,x+imageWidth-2,y+imageHeight-2,0xFF53636B);
        g.fill(x+4,y+4,x+imageWidth-4,y+25,0xFF263842);
        for(var slot:menu.slots){g.fill(x+slot.x-1,y+slot.y-1,x+slot.x+17,y+slot.y+17,0xFF16242B);g.fill(x+slot.x,y+slot.y,x+slot.x+16,y+slot.y+16,0xFF819299);}
        for(int i=0;i<menu.spec.sockets().size();i++){
            var s=menu.spec.sockets().get(i);if(s.type().equals("inventory"))continue;
            String icon=switch(s.type()){case "boiler"->"F";case "booster"->"+";case "upgrade"->"U";case "weapon"->"W";case "dye"->"D";default->"B";};
            if(menu.slots.get(i).getItem().isEmpty())g.text(font,icon,x+s.x()+5,y+s.y()+4,0xFF344B56,false);
            if(mx>=x+s.x()&&mx<x+s.x()+16&&my>=y+s.y()&&my<y+s.y()+16)g.text(font,s.type().equals("boiler")?"Fuel":s.type(),x+8,y+imageHeight-11,0xFFFFFFFF,false);
        }
        g.pose().popMatrix();
    }
    @Override protected void extractLabels(GuiGraphicsExtractor g,int mx,int my){
        g.text(font,title,8,9,0xFFFFFFFF,false);g.text(font,(menu.spec.name().equals("gyrodyne")?"Pedal power":"Fuel "+menu.fuel()/20+"s")+"  ·  Engine "+menu.engine()+"%",8,menu.spec.inventoryY()-25,0xFFF7D388,false);
        g.text(font,Component.translatable("container.inventory"),inventoryLabelX,inventoryLabelY,0xFFFFFFFF,false);
    }
}
