package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.jimbozoomer.jugcraft.menagerie.OwnerBlackboards;
import java.util.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Actual sixteen-pixel canvas, one undo checkpoint per stroke, explicit save acknowledgment. */
final class OwnerBlackboardScreen extends Screen {
    private final OwnerBlackboards.Open opening;
    private byte[] pixels;
    private final Deque<byte[]> undo=new ArrayDeque<>();
    private int x,y,cell,color=1,lastX,lastY;
    private boolean drawing,erase,waiting,finished;
    private String status="Left: draw | Right: erase";
    private Button save;
    OwnerBlackboardScreen(OwnerBlackboards.Open p){super(Component.literal("Blackboard"));opening=p;pixels=p.pixels().clone();}
    static void register(){
        ClientPlayNetworking.registerGlobalReceiver(OwnerBlackboards.Open.TYPE,(p,c)->c.client().execute(()->c.client().gui.setScreen(new OwnerBlackboardScreen(p))));
        ClientPlayNetworking.registerGlobalReceiver(OwnerBlackboards.Result.TYPE,(p,c)->c.client().execute(()->{if(c.client().gui.screen() instanceof OwnerBlackboardScreen s&&s.opening.token().equals(p.token())){s.waiting=false;s.finished=true;if(p.ok())s.onClose();else{s.status=p.reason();s.save.active=false;}}}));
    }
    @Override protected void init(){
        cell=Math.max(2,Math.min(12,(height-112)/16));x=(width-16*cell)/2;y=29;
        for(int i=0;i<16;i++){final int index=i;addRenderableWidget(Button.builder(Component.literal(i==0?"X":"■").withColor(OwnerBlackboards.color(i)&0xFFFFFF),b->color=index).bounds(x+i*16*cell/16,y+16*cell+6,Math.max(8,cell),18).build());}
        int bx=(width-296)/2,by=y+16*cell+29;
        addRenderableWidget(Button.builder(Component.literal("Undo"),b->{if(!undo.isEmpty())pixels=undo.pop();}).bounds(bx,by,48,20).build());
        addRenderableWidget(Button.builder(Component.literal("Clear"),b->{checkpoint();Arrays.fill(pixels,(byte)0);}).bounds(bx+50,by,48,20).build());
        addRenderableWidget(Button.builder(Component.literal("Copy"),b->minecraft.keyboardHandler.setClipboard("JUGCRAFT_BOARD:"+Base64.getEncoder().encodeToString(pixels))).bounds(bx+100,by,48,20).build());
        addRenderableWidget(Button.builder(Component.literal("Paste"),b->paste()).bounds(bx+150,by,48,20).build());
        save=addRenderableWidget(Button.builder(Component.literal("Save"),b->{if(!waiting&&!finished){waiting=true;save.active=false;status="Saving...";ClientPlayNetworking.send(new OwnerBlackboards.Save(opening.token(),pixels.clone(),false));}}).bounds(bx+200,by,48,20).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"),b->onClose()).bounds(bx+250,by,48,20).build());save.active=!waiting&&!finished;
    }
    private void checkpoint(){if(undo.size()==32)undo.removeLast();undo.push(pixels.clone());}
    private void paste(){String v=minecraft.keyboardHandler.getClipboard();if(!v.startsWith("JUGCRAFT_BOARD:")||v.length()>400)return;try{byte[] copy=Base64.getDecoder().decode(v.substring(15));if(copy.length!=256)return;for(byte b:copy)if(b<0||b>15)return;checkpoint();pixels=copy;}catch(IllegalArgumentException ignored){status="That clipboard does not contain a board drawing.";}}
    private boolean inside(double mx,double my){return mx>=x&&mx<x+16*cell&&my>=y&&my<y+16*cell;}
    private void paint(int cx,int cy){int dx=Math.abs(cx-lastX),dy=Math.abs(cy-lastY),sx=lastX<cx?1:-1,sy=lastY<cy?1:-1,err=dx-dy;int px=lastX,py=lastY;while(true){pixels[py*16+px]=(byte)(erase?0:color);if(px==cx&&py==cy)break;int e=2*err;if(e>-dy){err-=dy;px+=sx;}if(e<dx){err+=dx;py+=sy;}}lastX=cx;lastY=cy;}
    @Override public boolean mouseClicked(MouseButtonEvent e,boolean twice){
        if(super.mouseClicked(e,twice))return true;
        if(!waiting&&inside(e.x(),e.y())&&(e.button()==InputConstants.MOUSE_BUTTON_LEFT||e.button()==InputConstants.MOUSE_BUTTON_RIGHT)){checkpoint();drawing=true;erase=e.button()==InputConstants.MOUSE_BUTTON_RIGHT;lastX=(int)(e.x()-x)/cell;lastY=(int)(e.y()-y)/cell;paint(lastX,lastY);return true;}return false;
    }
    @Override public boolean mouseDragged(MouseButtonEvent e,double dx,double dy){if(drawing){paint(Math.clamp((int)(e.x()-x)/cell,0,15),Math.clamp((int)(e.y()-y)/cell,0,15));return true;}return super.mouseDragged(e,dx,dy);}
    @Override public boolean mouseReleased(MouseButtonEvent e){if(drawing){drawing=false;return true;}return super.mouseReleased(e);}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float partial){
        extractBackground(g,mx,my,partial);g.fill(x-5,y-5,x+16*cell+5,y+16*cell+5,0xFF624832);
        for(int row=0;row<16;row++)for(int col=0;col<16;col++)g.fill(x+col*cell,y+row*cell,x+(col+1)*cell,y+(row+1)*cell,OwnerBlackboards.color(pixels[row*16+col]));
        g.text(font,title,x,y-20,0xFFFFFFFF,false);g.fill(x+color*cell,y+16*cell+24,x+(color+1)*cell,y+16*cell+26,0xFFFFD970);
        int textY=y+16*cell+53;for(var line:font.split(Component.literal(status),Math.min(width-16,380))){g.text(font,line,Math.max(8,(width-380)/2),textY,0xFFE4D8C0,false);textY+=10;}
        super.extractRenderState(g,mx,my,partial);
    }
    @Override public boolean isPauseScreen(){return false;}
    @Override public void onClose(){if(!finished&&!waiting&&ClientPlayNetworking.canSend(OwnerBlackboards.Save.TYPE))ClientPlayNetworking.send(new OwnerBlackboards.Save(opening.token(),new byte[256],true));super.onClose();}
}
