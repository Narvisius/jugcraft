package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.nouveau.NouveauScrying;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Inventory;

/** Detached client camera; no server teleport, spectator mode or remote block interaction. */
public final class NouveauScryingClient {
    private static ArmorStand camera;
    private static int opening;
    private NouveauScryingClient(){}
    public static boolean active(){return camera!=null;}
    public static void register(){
        MenuScreens.register(NouveauScrying.MENU,ScryScreen::new);
        ClientPlayNetworking.registerGlobalReceiver(NouveauScrying.View.TYPE,(packet,ctx)->{
            var c=ctx.client();if(!packet.active()){clear(c);return;}if(c.player==null||c.level==null)return;
            var pos=packet.position();camera=new ArmorStand(c.level,pos.getX()+.5,pos.getY()+.85,pos.getZ()+.5);camera.setInvisible(true);camera.setNoGravity(true);
            camera.setYRot(c.player.getYRot());camera.setXRot(c.player.getXRot());opening=10;c.setCameraEntity(camera);
        });
        ClientPlayConnectionEvents.DISCONNECT.register((h,c)->clear(c));
        ClientTickEvents.END_CLIENT_TICK.register(c->{if(camera==null)return;if(c.player==null||c.level!=camera.level()){clear(c);return;}
            camera.setYRot(c.player.getYRot());camera.setXRot(c.player.getXRot());camera.yRotO=camera.getYRot();camera.xRotO=camera.getXRot();
            if(opening>0)opening--;else if(c.options.keyShift.isDown()||c.gui.screen()!=null){ClientPlayNetworking.send(new NouveauScrying.Exit());clear(c);}
        });
    }
    private static void clear(Minecraft c){camera=null;if(c.player!=null)c.setCameraEntity(c.player);}
    private static final class ScryScreen extends AbstractContainerScreen<NouveauScrying.Menu> {
        ScryScreen(NouveauScrying.Menu menu,Inventory inv,Component title){super(menu,inv,title,326,252);}
        @Override protected void init(){super.init();for(int i=0;i<16;i++){final int index=i;addRenderableWidget(Button.builder(Component.literal("View "+(i+1)),b->minecraft.gameMode.handleInventoryButtonClick(menu.containerId,index)).bounds(leftPos+27+(i%2)*151,topPos+44+(i/2)*23,135,20).build());}}
        @Override public void extractBackground(GuiGraphicsExtractor g,int mouseX,int mouseY,float partial){g.fill(0,0,width,height,0xB0080814);OwnerAlchemyArt.frame(g,leftPos,topPos,imageWidth,imageHeight);
            for(int i=0;i<16;i++){var stack=menu.source(i);if(!stack.isEmpty())g.item(stack,leftPos+8+(i%2)*151,topPos+46+(i/2)*23);}}
        @Override protected void extractLabels(GuiGraphicsExtractor g,int x,int y){g.text(font,title,12,12,0xffeadcb9,false);g.text(font,"Choose a parchment from a nearby pedestal.",12,27,0xffa8c7cf,false);g.text(font,"Nearby crystals · Sneak to return",12,236,0xffa8c7cf,false);}
        @Override public void extractRenderState(GuiGraphicsExtractor g,int mouseX,int mouseY,float partial){extractBackground(g,mouseX,mouseY,partial);super.extractRenderState(g,mouseX,mouseY,partial);extractTooltip(g,mouseX,mouseY);for(int i=0;i<16;i++){var button=(Button)children().get(i);var sample=menu.source(i);button.active=!sample.isEmpty();button.setMessage(sample.isEmpty()?Component.literal("Empty pedestal"):sample.getHoverName());}}
    }
}
