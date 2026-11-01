package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.io.*;
import java.nio.file.*;
import java.util.Properties;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.*;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec2;

/** Native third-person orbit and camera-relative walking; interactions always face the actual aim. */
public final class OwnerThirdPerson {
    private static LocalPlayer owner;
    private static float yaw,pitch,appliedYaw,follow;
    private static int aimTicks;
    private static boolean enabled=true;
    private static final Path FILE=FabricLoader.getInstance().getConfigDir().resolve("jugcraft-third-person.properties");
    private OwnerThirdPerson(){}
    public static void register(){
        if(Files.isRegularFile(FILE))try(var r=Files.newBufferedReader(FILE)){var p=new Properties();p.load(r);enabled=Boolean.parseBoolean(p.getProperty("enabled","true"));}catch(IOException e){Jugcraft.LOGGER.warn("Could not read third-person settings",e);}
        var key=KeyMappingHelper.registerKeyMapping(new KeyMapping("key.jugcraft.third_person_orbit",InputConstants.UNKNOWN.getValue(),PartyClient.CATEGORY));
        ClientTickEvents.START_CLIENT_TICK.register(c->{
            while(key.consumeClick()){enabled=!enabled;try{Files.createDirectories(FILE.getParent());Files.writeString(FILE,"enabled="+enabled+"\n");}catch(IOException e){Jugcraft.LOGGER.warn("Could not save third-person setting",e);}}
            if(!active())return;
            if(aimTicks>0)aimTicks--;
            if(Math.abs(Mth.wrapDegrees(owner.getYRot()-appliedYaw))>60){yaw=owner.getYRot();pitch=owner.getXRot();follow=0;}
            if(c.options.keyAttack.isDown()||c.options.keyUse.isDown()||owner.isUsingItem())aim();
        });
    }
    public static boolean active(){
        var c=Minecraft.getInstance();var p=c.player;
        boolean eligible=enabled&&p!=null&&p.isAlive()&&c.getCameraEntity()==p&&!c.options.getCameraType().isFirstPerson()&&!p.isPassenger()&&!p.isSleeping()&&!p.isFallFlying()&&!p.isSwimming()&&!p.isSpectator();
        if(!eligible){if(owner!=null&&owner==p){owner.setYRot(yaw);owner.setXRot(pitch);}owner=null;aimTicks=0;return false;}
        if(owner!=p){owner=p;yaw=p.getYRot();pitch=p.getXRot();appliedYaw=yaw;follow=0;}
        return true;
    }
    public static boolean turn(Entity entity,double dx,double dy){
        if(entity!=Minecraft.getInstance().player||!active())return false;
        float delta=(float)dx*.15F;yaw+=delta;pitch=Math.clamp(pitch+(float)dy*.15F,-90,90);
        if(aimTicks>0)face(yaw,pitch);
        else {
            if(Math.signum(delta)!=Math.signum(follow))follow=0;follow+=delta;
            if(Math.abs(follow)<=45)face(owner.getYRot()+delta*(1-(float)Math.pow(2,10*Math.abs(follow)/45-10)),owner.getXRot());
        }return true;
    }
    private static void face(float y,float x){owner.setYRot(y);owner.setXRot(x);appliedYaw=y;}
    public static Vec2 movement(Vec2 input){
        if(!active()||aimTicks>0||input.lengthSquared()<.0001)return input;
        float target=yaw-(float)Math.toDegrees(Math.atan2(input.x,input.y));
        float facing=owner.getYRot()+Mth.wrapDegrees(target-owner.getYRot())*.5F;
        face(facing,owner.getXRot()+(pitch-owner.getXRot())*.65F);
        float relative=(float)Math.toRadians(Mth.wrapDegrees(facing-target));
        float speed=(float)Math.sqrt(input.lengthSquared());return new Vec2((float)Math.sin(relative)*speed,(float)Math.cos(relative)*speed);
    }
    public static boolean aim(){if(!active())return false;aimTicks=40;face(yaw,pitch);owner.yRotO=yaw;owner.xRotO=pitch;return true;}
    public static float yaw(Entity entity,float original){return entity==Minecraft.getInstance().player&&active()?yaw:original;}
    public static float pitch(Entity entity,float original){return entity==Minecraft.getInstance().player&&active()?pitch:original;}
}
