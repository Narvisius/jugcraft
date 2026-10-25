package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.aircraft.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.gui.screens.MenuScreens;

public final class AircraftClient {
    private AircraftClient(){}
    private static int since;
    public static float fov(net.minecraft.client.player.AbstractClientPlayer player,float original){
        var c=net.minecraft.client.Minecraft.getInstance();
        return c.player==player&&c.gui.screen()==null&&player.getVehicle() instanceof Aircraft a&&a.hasTelescope()&&c.options.keyUse.isDown()?original*.2F:original;
    }
    public static void register(){
        EntityRendererRegistry.register(JugcraftAircraft.BULLET,net.minecraft.client.renderer.entity.NoopRenderer::new);
        net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry.attachElementAfter(
            net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements.HOTBAR,io.github.jimbozoomer.jugcraft.Jugcraft.id("aircraft_instruments"),(g,d)->{
            var c=net.minecraft.client.Minecraft.getInstance();if(c.player==null||c.gui.screen()!=null||!(c.player.getVehicle() instanceof Aircraft a)||a.instruments()==0)return;
            int cx=g.guiWidth()/2,cy=g.guiHeight()/2,color=0xFFADF2D5;
            if((a.instruments()&1)!=0){
                int y=cy+Math.clamp(Math.round(a.getXRot()),-40,40);
                g.fill(cx-50,y,cx-12,y+1,color);g.fill(cx+12,y,cx+50,y+1,color);
                g.text(c.font,"ALT "+a.getBlockY(),cx+55,cy-4,color,false);
                g.text(c.font,String.format(java.util.Locale.ROOT,"%.0f km/h",a.getDeltaMovement().horizontalDistance()*72),cx-105,cy-4,color,false);
            }
            if((a.instruments()&2)!=0){
                int x=8,y=30;g.fill(x-3,y-3,x+125,y+38,0xC014252B);
                g.text(c.font,String.format(java.util.Locale.ROOT,"HEADING %03d",Math.floorMod(Math.round(a.getYRot()),360)),x,y,color,false);
                g.text(c.font,"ALT "+a.getBlockY()+"  POWER "+Math.round(a.power()*100)+"%",x,y+12,color,false);
                g.text(c.font,String.format(java.util.Locale.ROOT,"CLIMB %+.1f m/s",a.getDeltaMovement().y*20),x,y+24,color,false);
            }
        });
        JugcraftAircraft.TYPES.values().forEach(t->EntityRendererRegistry.register(t,AircraftRenderer::new));
        JugcraftAircraft.MENUS.values().forEach(t->MenuScreens.register(t,AircraftScreen::new));
        ClientTickEvents.END_CLIENT_TICK.register(AircraftSounds::tick);
        ClientTickEvents.END_CLIENT_TICK.register(c->{
            if(c.player==null||c.isPaused()||!(c.player.getVehicle() instanceof Aircraft a)||a.getFirstPassenger()!=c.player||++since<2)return;
            since=0;boolean active=c.gui.screen()==null;var o=c.options;
            ClientPlayNetworking.send(new AircraftInput(active?(o.keyUp.isDown()?1:0)-(o.keyDown.isDown()?1:0):0,
                active?(o.keyLeft.isDown()?1:0)-(o.keyRight.isDown()?1:0):0,
                active?(o.keyJump.isDown()?1:0)-(o.keySprint.isDown()?1:0):0,active&&o.keyAttack.isDown(),active&&o.keyUse.isDown()));
        });
    }
}
