package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.nio.file.*;
import java.io.*;
import java.util.Properties;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.*;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.screens.Screen;
import me.shedaniel.clothconfig2.api.ConfigBuilder;

/** Native camera integration of the supplied Zoom controls. Never rewrites the user's FOV or sensitivity options. */
public final class OwnerZoom {
    private static KeyMapping zoom,secondary;
    private static boolean latched,active,previous,deep;
    private static final net.minecraft.util.SmoothDouble smoothX=new net.minecraft.util.SmoothDouble(),smoothY=new net.minecraft.util.SmoothDouble();
    private static KeyMapping stronger,weaker;
    public static boolean hideHud(){return active&&deep&&flag("secondary_hide_hud",false);}
    public static boolean hideSpyglassOverlay(){return active&&flag("hide_spyglass_overlay",false);}
    public static double smoothMouse(double delta,double elapsed,boolean horizontal){
        var smoother=horizontal?smoothX:smoothY;
        if(!active||!flag("cinematic",false)){smoother.reset();return delta;}
        return smoother.getNewDeltaValue(delta,Math.clamp(elapsed*8,0,1));
    }
    private static double factor=1,steps,from=1,target=1;
    private static long changed=System.nanoTime();
    private static final Path FILE=FabricLoader.getInstance().getConfigDir().resolve("jugcraft-zoom.properties");
    private static final Properties values=new Properties();
    private OwnerZoom(){}
    private static boolean flag(String key,boolean fallback){return Boolean.parseBoolean(values.getProperty(key,Boolean.toString(fallback)));}
    private static double number(String key,double fallback,double low,double high){try{double v=Double.parseDouble(values.getProperty(key,Double.toString(fallback)));return Double.isFinite(v)?Math.clamp(v,low,high):fallback;}catch(NumberFormatException e){return fallback;}}
    public static void register(){
        if(Files.isRegularFile(FILE))try(var reader=Files.newBufferedReader(FILE)){values.load(reader);}catch(IOException e){Jugcraft.LOGGER.warn("Could not read local zoom settings",e);}
        // Unbound controls avoid silently taking a gun, vehicle or existing magic key.
        zoom=KeyMappingHelper.registerKeyMapping(new KeyMapping("key.jugcraft.owner_zoom",InputConstants.UNKNOWN.getValue(),PartyClient.CATEGORY));
        secondary=KeyMappingHelper.registerKeyMapping(new KeyMapping("key.jugcraft.owner_zoom_secondary",InputConstants.UNKNOWN.getValue(),PartyClient.CATEGORY));
        stronger=KeyMappingHelper.registerKeyMapping(new KeyMapping("key.jugcraft.owner_zoom_in",InputConstants.UNKNOWN.getValue(),PartyClient.CATEGORY));
        weaker=KeyMappingHelper.registerKeyMapping(new KeyMapping("key.jugcraft.owner_zoom_out",InputConstants.UNKNOWN.getValue(),PartyClient.CATEGORY));
        var settings=KeyMappingHelper.registerKeyMapping(new KeyMapping("key.jugcraft.owner_zoom_settings",InputConstants.UNKNOWN.getValue(),PartyClient.CATEGORY));
        ClientTickEvents.END_CLIENT_TICK.register(client->{
            while(settings.consumeClick())client.gui.setScreen(settings(client.gui.screen()));
            if(client.player==null||client.gui.screen()!=null||!client.isWindowActive()){
                latched=false;active=false;deep=false;previous=false;retarget(1);return;
            }
            boolean down=zoom.isDown();if(flag("toggle",false)&&down&&!previous)latched=!latched;
            previous=down;boolean next=flag("toggle",false)?latched:down;
            if(next&&!active&&!flag("retain_steps",false))steps=0;
            active=next;deep=secondary.isDown();
            while(stronger.consumeClick())if(active)steps=Math.min(number("max_steps",20,1,64),steps+1);
            while(weaker.consumeClick())if(active)steps=Math.max(-7,steps-1);
            double desired=active?number("initial",4,1,32)*Math.pow(number("step_factor",1.2,1.01,4),steps):1;
            if(deep)desired*=number("secondary",4,1,32);
            retarget(Math.clamp(desired,1,128));
        });
    }
    private static void retarget(double next){if(Math.abs(next-target)<.00001)return;sample();from=factor;target=next;changed=System.nanoTime();}
    private static double sample(){
        double duration=number(target>from?"in_seconds":"out_seconds",target>from?1:.5,0,10);
        double elapsed=(System.nanoTime()-changed)/1_000_000_000D;
        double t=duration==0?1:Math.clamp(elapsed/duration,0,1);
        double eased=flag("linear_transition",false)?t:t>=1?1:1-Math.pow(2,-10*t);
        factor=from+(target-from)*eased;return factor;
    }
    public static float fov(float original,boolean hand){
        var client=Minecraft.getInstance();if(client.player==null)return original;
        if(hand&&!flag("hand_fov",true))return original;
        return (float)Math.max(.5,original/sample());
    }
    public static double sensitivity(){return Math.pow(1/sample(),number("sensitivity_percent",100,0,100)/100);}
    public static boolean scroll(double amount){
        var client=Minecraft.getInstance();if(!active||!flag("scroll",true)||client.gui.screen()!=null||client.player==null||amount==0||!Double.isFinite(amount))return false;
        steps=Math.clamp(steps+Math.clamp(amount,-4,4),-7,number("max_steps",20,1,64));
        retarget(Math.clamp(number("initial",4,1,32)*Math.pow(number("step_factor",1.2,1.01,4),steps)*(deep?number("secondary",4,1,32):1),1,128));return true;
    }
    public static Screen settings(Screen parent){
        var builder=ConfigBuilder.create().setParentScreen(parent).setTitle(Component.literal("Jugcraft Zoom"));
        var category=builder.getOrCreateCategory(Component.literal("Camera"));var entries=builder.entryBuilder();
        for(var option:new String[][]{{"toggle","Toggle instead of hold","false"},{"retain_steps","Remember zoom strength","false"},{"scroll","Scroll to adjust while zoomed","true"},{"hand_fov","Zoom held item too","true"},{"cinematic","Smooth mouse while zoomed","false"},{"linear_transition","Linear zoom transitions","false"},{"secondary_hide_hud","Hide HUD during secondary zoom","false"},{"hide_spyglass_overlay","Hide spyglass frame while zooming","false"}}){
            String key=option[0];boolean fallback=Boolean.parseBoolean(option[2]);
            category.addEntry(entries.startBooleanToggle(Component.literal(option[1]),flag(key,fallback)).setDefaultValue(fallback).setSaveConsumer(v->values.setProperty(key,v.toString())).build());
        }
        for(var option:new String[][]{{"initial","Initial magnification","4","1","32"},{"secondary","Secondary magnification","4","1","32"},{"max_steps","Maximum scroll/key zoom steps","20","1","64"},{"sensitivity_percent","Relative mouse sensitivity (%)","100","0","100"}}){
            String key=option[0];int fallback=Integer.parseInt(option[2]),low=Integer.parseInt(option[3]),high=Integer.parseInt(option[4]);
            category.addEntry(entries.startIntSlider(Component.literal(option[1]),(int)number(key,fallback,low,high),low,high).setDefaultValue(fallback).setSaveConsumer(v->values.setProperty(key,v.toString())).build());
        }
        for(String key:new String[]{"in_seconds","out_seconds"})category.addEntry(entries.startDoubleField(Component.literal(key.equals("in_seconds")?"Zoom-in seconds":"Zoom-out seconds"),number(key,key.equals("in_seconds")?1:.5,0,10)).setMin(0).setMax(10).setSaveConsumer(v->values.setProperty(key,v.toString())).build());
        category.addEntry(entries.startDoubleField(Component.literal("Magnification per scroll/key step"),number("step_factor",1.2,1.01,4)).setMin(1.01).setMax(4).setSaveConsumer(v->values.setProperty("step_factor",v.toString())).build());
        builder.setSavingRunnable(()->{try{Files.createDirectories(FILE.getParent());try(var writer=Files.newBufferedWriter(FILE)){values.store(writer,"Local camera controls; key bindings are in Minecraft Controls");}}catch(IOException e){Jugcraft.LOGGER.warn("Could not save zoom settings",e);}});
        return builder.build();
    }
}
