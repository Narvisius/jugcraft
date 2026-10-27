package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.aircraft.*;
import io.github.jimbozoomer.jugcraft.client.AircraftSounds;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.*;

/** Real passenger controls, F5 and original atlas rendering in an isolated test world. */
public final class AircraftFlightClientGameTests implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext c){
        try(var world=c.worldBuilder().adjustSettings(s->s.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE)).create()){
            var server=world.getServer();world.getConnection().waitForChunksRender();
            server.runCommand("gamerule minecraft:send_command_feedback false");
            server.runCommand("time set noon");
            var origin=c.computeOnClient(mc->mc.player.blockPosition());
            for(String kind:AircraftSpec.ALL.keySet()){
                server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.stopRiding();
                    for(var old:p.level().getEntitiesOfClass(Aircraft.class,p.getBoundingBox().inflate(150)))old.discard();
                    p.teleportTo(origin.getX(),220,origin.getZ());
                    var a=JugcraftAircraft.TYPES.get(kind).create(p.level(),EntitySpawnReason.COMMAND);
                    a.snapTo(p.getX(),220,p.getZ(),0,0);a.setOwner(p.getUUID());p.level().addFreshEntity(a);
                    for(int i=0;i<a.spec().sockets().size();i++)if(a.spec().sockets().get(i).type().equals("boiler"))a.inventory().setItem(i,new ItemStack(Items.COAL,64));
                    p.startRiding(a);
                });
                c.runOnClient(mc->mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
                c.getInput().holdKey(o->o.keyUp);c.getInput().holdKey(o->o.keyLeft);c.waitTicks(45);
                c.runOnClient(mc->{
                    if(!(mc.player.getVehicle() instanceof Aircraft a))throw new AssertionError("Aircraft passenger sync: "+kind);
                    if(a.power()<.1||a.animatedControl(1,1)<.5||a.bank(1)<=0)throw new AssertionError("Powered steering/animation: "+kind);
                    double distance=mc.gameRenderer.mainCamera().position().distanceTo(mc.player.getEyePosition());
                    if(distance<a.spec().cameraDistance()*.7)throw new AssertionError("F5 remains inside aircraft: "+kind+" distance="+distance);
                    try{
                        var field=AircraftSounds.class.getDeclaredField("ACTIVE");field.setAccessible(true);
                        var loops=(java.util.Map<?,?>)field.get(null);
                        var sound=(net.minecraft.client.resources.sounds.SoundInstance)loops.get(a);
                        if(sound==null||!sound.isRelative()||sound.getX()!=0||sound.getVolume()<=0)throw new AssertionError("Engine loop missing/misplaced: "+kind);
                    }catch(ReflectiveOperationException e){throw new AssertionError(e);}
                });
                c.takeScreenshot("aircraft_oct25_"+kind+"_turning");
                c.getInput().releaseKey(o->o.keyLeft);c.getInput().releaseKey(o->o.keyUp);
                c.runOnClient(mc->mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));c.waitTicks(12);
                c.runOnClient(mc->{
                    var a=(Aircraft)mc.player.getVehicle();
                    if(mc.gameRenderer.mainCamera().position().distanceTo(mc.player.getEyePosition())<a.spec().cameraDistance()*.7)
                        throw new AssertionError("Front F5 remains inside aircraft: "+kind);
                });
                c.takeScreenshot("aircraft_oct25_"+kind+"_front");
            }
            server.runOnServer(s->s.getPlayerList().getPlayers().getFirst().stopRiding());
            c.runOnClient(mc->mc.options.setCameraType(CameraType.FIRST_PERSON));
        }finally{c.getInput().releaseKey(o->o.keyLeft);c.getInput().releaseKey(o->o.keyUp);}
    }
}
