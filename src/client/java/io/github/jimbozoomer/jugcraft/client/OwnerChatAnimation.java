package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.io.*;
import java.nio.file.*;
import java.util.Properties;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.*;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import com.mojang.blaze3d.platform.InputConstants;

/** Local presentation only: supplied timings/easing with reduced-motion support. */
public final class OwnerChatAnimation {
    private static final Properties SETTINGS=new Properties();
    private static final Path FILE=FabricLoader.getInstance().getConfigDir().resolve("jugcraft-chat-animation.properties");
    public interface Timestamp {long jugcraft$created();}
    private OwnerChatAnimation(){}
    public static long now(){return System.nanoTime()/1_000_000;}
    public static float age(Object line){return line instanceof Timestamp t?Math.max(0,now()-t.jugcraft$created()):800;}
    private static boolean flag(String key){return Boolean.parseBoolean(SETTINGS.getProperty(key,"true"));}
    private static int time(String key,int fallback){try{return Math.clamp(Integer.parseInt(SETTINGS.getProperty(key,""+fallback)),10,800);}catch(NumberFormatException e){return fallback;}}
    public static boolean messages(){return flag("messages")&&!ConcordanceClientOptions.reducedMotion();}
    public static boolean input(){return flag("input")&&!ConcordanceClientOptions.reducedMotion();}
    public static int messageMillis(){return time("message_millis",150);}
    public static int inputMillis(){return time("input_millis",170);}
    public static double getOpacityFactor(float age){return messages()&&flag("opacity")?Math.clamp(age/messageMillis(),0,1):1;}
    public static void wrap(GuiGraphicsExtractor graphics,float displacement,Runnable draw){
        graphics.pose().pushMatrix();try{graphics.pose().translate(0,displacement);draw.run();}finally{graphics.pose().popMatrix();}
    }
    public static void register(){
        if(Files.isRegularFile(FILE))try(var reader=Files.newBufferedReader(FILE)){SETTINGS.load(reader);}catch(IOException e){Jugcraft.LOGGER.warn("Could not read chat presentation settings",e);}
        var settings=KeyMappingHelper.registerKeyMapping(new KeyMapping("key.jugcraft.chat_animation_settings",InputConstants.UNKNOWN.getValue(),PartyClient.CATEGORY));
        ClientTickEvents.END_CLIENT_TICK.register(c->{while(settings.consumeClick())c.gui.setScreen(settings(c.gui.screen()));});
    }
    private static Screen settings(Screen parent){
        var builder=ConfigBuilder.create().setParentScreen(parent).setTitle(Component.literal("Chat presentation"));var cat=builder.getOrCreateCategory(Component.literal("Animation"));var entry=builder.entryBuilder();
        for(var option:new String[][]{{"messages","Animate new messages"},{"opacity","Fade in messages"},{"input","Animate chat input"}}){String key=option[0];cat.addEntry(entry.startBooleanToggle(Component.literal(option[1]),flag(key)).setDefaultValue(true).setSaveConsumer(v->SETTINGS.setProperty(key,v.toString())).build());}
        for(var option:new String[][]{{"message_millis","Message duration (ms)","150"},{"input_millis","Input duration (ms)","170"}}){String key=option[0];int fallback=Integer.parseInt(option[2]);cat.addEntry(entry.startIntSlider(Component.literal(option[1]),time(key,fallback),10,800).setDefaultValue(fallback).setSaveConsumer(v->SETTINGS.setProperty(key,v.toString())).build());}
        builder.setSavingRunnable(()->{try{Files.createDirectories(FILE.getParent());try(var writer=Files.newBufferedWriter(FILE)){SETTINGS.store(writer,"Chat animation only; message transport, signatures and trust indicators are unchanged");}}catch(IOException e){Jugcraft.LOGGER.warn("Could not save chat presentation settings",e);}});return builder.build();
    }
}
