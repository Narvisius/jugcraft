package io.github.jimbozoomer.jugcraft.client.update;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Menu-only consent: downloading does not alter the running game. */
public final class UpdateScreen extends Screen {
    private final Screen parent;
    private Button action,back;
    public UpdateScreen(Screen parent){super(Component.literal("JUGCRAFT Updates"));this.parent=parent;}
    @Override protected void init(){
        int w=Math.min(300,width-36),x=(width-w)/2,y=height-54;
        action=addRenderableWidget(Button.builder(Component.literal("Check for updates"),b->{
            switch(JugcraftUpdates.state){case AVAILABLE->JugcraftUpdates.download();case READY->JugcraftUpdates.applyAndQuit();default->JugcraftUpdates.check();}
        }).bounds(x,y,w,20).build());
        back=addRenderableWidget(Button.builder(Component.literal("Back / play installed version"),b->onClose()).bounds(x,y+24,w,20).build());tick();
    }
    @Override public void tick(){
        if(action==null)return;
        action.setMessage(Component.literal(switch(JugcraftUpdates.state){case AVAILABLE->"Download update";case READY->"Apply update & quit";case CHECKING->"Checking...";case DOWNLOADING->"Downloading...";case APPLYING->"Quitting safely...";default->"Check again";}));
        action.active=!JugcraftUpdates.busy();back.active=JugcraftUpdates.state!=JugcraftUpdates.State.APPLYING;
    }
    @Override public void onClose(){if(JugcraftUpdates.state!=JugcraftUpdates.State.APPLYING)minecraft.gui.setScreen(parent);}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta){
        g.fill(0,0,width,height,0xff101a22);g.fill(0,0,width,4,0xff68d5bf);
        int x=Math.max(18,(width-440)/2),w=Math.min(440,width-36),y=22;
        g.text(font,"JUGCRAFT",x,y,0xff8fe8d5,false);y+=18;
        g.text(font,"Same instance. New adventures.",x,y,0xffeeeeee,false);y+=22;
        if(!JugcraftUpdates.current.isEmpty()) {g.text(font,"Installed: "+JugcraftUpdates.current,x,y,0xffbdcbd4,false);y+=14;}
        if(!JugcraftUpdates.latest.isEmpty()){g.text(font,"Channel: "+JugcraftUpdates.latest,x,y,0xffbdcbd4,false);y+=18;}
        for(var line:font.split(Component.literal(JugcraftUpdates.message),w)){if(y>height-112)break;g.text(font,line,x,y,0xffeeeeee,false);y+=11;}
        if(JugcraftUpdates.state==JugcraftUpdates.State.DOWNLOADING){g.fill(x,height-94,x+w,height-88,0xff34414b);g.fill(x,height-94,x+(int)(w*JugcraftUpdates.progress),height-88,0xff68d5bf);}
        g.text(font,"Keeps settings, keybinds and saved worlds.",x,height-78,0xff9baebd,false);
        super.extractRenderState(g,mx,my,delta);
    }
}
