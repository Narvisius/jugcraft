package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.aircraft.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.*;
import net.minecraft.sounds.SoundSource;

/** Supplied engine recordings follow the aircraft and fade with its synchronized engine power. */
public final class AircraftSounds {
    private static final Map<Aircraft,Loop> ACTIVE=new IdentityHashMap<>();
    private AircraftSounds(){}
    public static void tick(Minecraft c){
        ACTIVE.entrySet().removeIf(e->{var sound=e.getValue();if(e.getKey().isRemoved()||e.getKey().level()!=c.level){sound.finish();return true;}return sound.isStopped();});
        if(c.level==null){ACTIVE.values().forEach(Loop::finish);ACTIVE.clear();return;}
        for(var entity:c.level.entitiesForRendering())if(entity instanceof Aircraft a&&a.power()>.01&&!ACTIVE.containsKey(a)){
            var sound=new Loop(a);ACTIVE.put(a,sound);c.getSoundManager().play(sound);
        }
    }
    private static final class Loop extends AbstractTickableSoundInstance {
        private final Aircraft craft;
        Loop(Aircraft a){super(JugcraftAircraft.SOUNDS.get(switch(a.spec().name()){case "warship"->"warship";case "bamboo_hopper"->"propeller_bamboo_hopper";case "quadrocopter"->"propeller_tiny";case "gyrodyne"->"woosh";default->"propeller";}),SoundSource.NEUTRAL,SoundInstance.createUnseededRandom());craft=a;looping=true;delay=0;volume=.45F*a.power();pitch=.5F+.5F*a.power();x=a.getX();y=a.getY();z=a.getZ();attenuation=Attenuation.LINEAR;position();}
        void finish(){stop();}
        private void position(){
            var player=Minecraft.getInstance().player;
            relative=player!=null&&player.getVehicle()==craft;
            x=relative?0:craft.getX();y=relative?0:craft.getY();z=relative?0:craft.getZ();
        }
        @Override public boolean canStartSilent(){return true;}
        @Override public void tick(){if(craft.isRemoved()||Minecraft.getInstance().level!=craft.level()||craft.power()<.005){stop();return;}position();volume=.45F*craft.power();pitch=.5F+.5F*craft.power();}
    }
}
