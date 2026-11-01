package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.menagerie.OwnerSupplementText;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Compact text preview with explicit save feedback; local drafts survive a rejected edit. */
final class OwnerSupplementTextScreen extends Screen {
    private final OwnerSupplementText.Open opening;
    private final EditBox[] lines=new EditBox[3];
    private final String[] draft=new String[3];
    private int left,top,mode,range;
    private boolean waiting,finished;
    private String status="";
    private Button save;
    OwnerSupplementTextScreen(OwnerSupplementText.Open packet){
        super(Component.literal(packet.sign()?"Direction sign":packet.speaker()?"Speaker message":"Doormat lettering"));opening=packet;mode=packet.mode();range=packet.range();
        var existing=packet.message().split("\n",-1);for(int i=0;i<3;i++)draft[i]=i<existing.length?existing[i]:"";
    }
    static void register(){
        ClientPlayNetworking.registerGlobalReceiver(OwnerSupplementText.Open.TYPE,(p,c)->c.client().execute(()->c.client().gui.setScreen(new OwnerSupplementTextScreen(p))));
        ClientPlayNetworking.registerGlobalReceiver(OwnerSupplementText.Result.TYPE,(p,c)->c.client().execute(()->{
            if(c.client().gui.screen() instanceof OwnerSupplementTextScreen screen&&screen.opening.token().equals(p.token())){screen.waiting=false;screen.finished=true;if(p.ok())screen.onClose();else{screen.status=p.reason();screen.save.active=false;}}
        }));
        ClientPlayNetworking.registerGlobalReceiver(OwnerSupplementText.Announcement.TYPE,(p,c)->c.client().execute(()->{
            var client=c.client();var message=io.github.jimbozoomer.jugcraft.menagerie.OwnerAntiqueInk.ink(Component.literal(p.message()),p.antique());
            switch(p.mode()){
                case 1->client.gui.hud.setOverlayMessage(message,false);
                case 2->{client.gui.hud.setTimes(10,60,20);client.gui.hud.setTitle(message);}
                case 3->{if(client.getNarrator().isActive())client.getNarrator().saySystemQueued(message);else client.gui.hud.getChat().addServerSystemMessage(message);}
                default->client.gui.hud.getChat().addServerSystemMessage(message);
            }
        }));
    }
    @Override protected void init(){
        left=(width-300)/2;top=Math.max(4,(height-216)/2);
        for(int i=0;i<(opening.sign()?1:3);i++){
            final int index=i;lines[i]=addRenderableWidget(new EditBox(font,left+16,top+39+i*26,268,20,Component.literal("Line "+(i+1))));
            lines[i].setMaxLength(opening.sign()?75:73);lines[i].setValue(draft[i]);lines[i].setResponder(v->draft[index]=v);
        }
        if(opening.speaker()){
            addRenderableWidget(Button.builder(modeLabel(),b->{mode=(mode+1)%4;b.setMessage(modeLabel());}).bounds(left+16,top+121,170,20).build());
            addRenderableWidget(Button.builder(Component.literal("Range: "+range),b->{range=range>=64?4:Math.min(64,range*2);b.setMessage(Component.literal("Range: "+range));}).bounds(left+190,top+121,94,20).build());
        }
        addRenderableWidget(Button.builder(Component.literal("Copy draft"),b->minecraft.keyboardHandler.setClipboard(message())).bounds(left+16,top+153,88,20).build());
        save=addRenderableWidget(Button.builder(Component.literal("Save"),b->{if(waiting||finished)return;waiting=true;save.active=false;status="Saving…";ClientPlayNetworking.send(new OwnerSupplementText.Save(opening.token(),message(),mode,range,false));}).bounds(left+108,top+153,84,20).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"),b->onClose()).bounds(left+196,top+153,88,20).build());save.active=!waiting&&!finished;
        setInitialFocus(lines[0]);
    }
    private Component modeLabel(){return Component.literal("Display: "+switch(mode){case 1->"Action bar";case 2->"Title";case 3->"Narrator";default->"Chat";});}
    private String message(){if(opening.sign())return draft[0];return String.join("\n",draft).stripTrailing();}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float partial){
        extractBackground(g,mx,my,partial);g.fill(left-3,top-3,left+303,top+213,0xFF3E2C20);g.fill(left,top,left+300,top+210,0xFFB88A53);
        g.fill(left+5,top+5,left+295,top+28,0xFF5F432D);g.text(font,title,left+16,top+12,0xFFFFE4A5,false);
        g.fill(left+8,top+32,left+292,top+145,0xFF302B24);
        if(!opening.speaker())g.text(font,Component.literal("Dyes recolor • Honeycomb protects lettering"),left+16,top+126,0xFFE9D8B3,false);
        int y=top+181;for(var line:font.split(Component.literal(status),268)){g.text(font,line,left+16,y,0xFF211A15,false);y+=10;}
        super.extractRenderState(g,mx,my,partial);
    }
    @Override public boolean isPauseScreen(){return false;}
    @Override public void onClose(){if(!waiting&&!finished&&ClientPlayNetworking.canSend(OwnerSupplementText.Save.TYPE))ClientPlayNetworking.send(new OwnerSupplementText.Save(opening.token(),"",mode,range,true));super.onClose();}
}
