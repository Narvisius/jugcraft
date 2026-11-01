package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.menagerie.OwnerCannonMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Clickable aiming and power controls, without typed commands or coordinates. */
final class OwnerCannonScreen extends AbstractContainerScreen<OwnerCannonMenu>{
    private Button fire;
    OwnerCannonScreen(OwnerCannonMenu menu,Inventory inventory,Component title){super(menu,inventory,title,176,225);inventoryLabelY=131;}
    private Button button(String title,int action,int x,int y,int width){return addRenderableWidget(Button.builder(Component.literal(title),b->{if(minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,action);}).bounds(leftPos+x,topPos+y,width,18).build());}
    @Override protected void init(){super.init();for(int i=0;i<4;i++)button(Integer.toString(i+1),i,48+i*27,65,24);button("<",4,9,89,22);button(">",5,34,89,22);button("Up",6,66,89,28);button("Down",7,98,89,34);button("Aim",8,135,89,32);fire=button("Fire",9,72,110,48);button("Ride",10,124,110,43);}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int x,int y,float partial){fire.active=menu.loaded()&&menu.fuse()==0&&menu.cooldown()==0;extractBackground(g,x,y,partial);super.extractRenderState(g,x,y,partial);extractTooltip(g,x,y);}
    @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float partial){
        int x=leftPos,y=topPos;g.fill(x,y,x+imageWidth,y+imageHeight,0xFF251D17);g.fill(x+2,y+2,x+imageWidth-2,y+imageHeight-2,0xFF806547);g.fill(x+6,y+17,x+170,y+130,0xFF302B28);
        for(var slot:menu.slots){int sx=x+slot.x,sy=y+slot.y;g.fill(sx-1,sy-1,sx+17,sy+17,0xFF141414);g.fill(sx,sy,sx+16,sy+16,0xFF706A60);}
        g.text(font,Component.literal("Powder"),x+33,y+26,0xFFEADBC3,false);g.text(font,Component.literal("Payload"),x+103,y+26,0xFFEADBC3,false);
        g.text(font,Component.literal("Power"),x+9,y+70,0xFFEADBC3,false);g.text(font,Component.literal("x"+menu.power()),x+82,y+43,0xFFF3CA73,false);
        String status=menu.fuse()>0?"Firing in "+String.format(java.util.Locale.ROOT,"%.1fs",menu.fuse()/20F):menu.cooldown()>0?"Cooling":menu.loaded()?"Loaded":"Load powder and a payload";
        g.text(font,Component.literal(status),x+9,y+18,0xFFEADBC3,false);
        g.text(font,Component.literal(menu.yaw()+"° / "+(-menu.pitch())+"°"),x+9,y+112,0xFFEADBC3,false);
    }
}
