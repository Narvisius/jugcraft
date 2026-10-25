package io.github.jimbozoomer.jugcraft.client.jimtafacts;

import io.github.jimbozoomer.jugcraft.jimtafacts.computers.ComputerAudio;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import javax.sound.sampled.AudioFormat;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.sound.v1.FabricSoundInstance;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.*;
import net.minecraft.client.sounds.*;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.valueproviders.ConstantFloat;
import net.minecraft.world.phys.Vec3;

/** Uses Fabric's existing sound stream hook, Minecraft volume controls and at most eight clip channels. */
public final class ComputerAudioClient {
    private static final Map<BlockPos,ClipSound> ACTIVE=new LinkedHashMap<>();
    private ComputerAudioClient(){}
    public static void register(){
        ClientPlayNetworking.registerGlobalReceiver(ComputerAudio.Clip.TYPE,(packet,context)->context.client().execute(()->{
            var client=context.client();if(client.level==null||client.player==null||!client.level.isLoaded(packet.pos())||client.player.distanceToSqr(Vec3.atCenterOf(packet.pos()))>48*48)return;
            var previous=ACTIVE.remove(packet.pos());if(previous!=null)client.getSoundManager().stop(previous);
            while(ACTIVE.size()>=8){var first=ACTIVE.keySet().iterator().next();client.getSoundManager().stop(ACTIVE.remove(first));}
            var sound=new ClipSound(packet.pos(),WorkshopDfpwm.decode(packet.data()));ACTIVE.put(packet.pos(),sound);client.getSoundManager().play(sound);
        }));
        ClientTickEvents.END_CLIENT_TICK.register(client->{
            if(client.level==null){ACTIVE.values().forEach(client.getSoundManager()::stop);ACTIVE.clear();}
            else ACTIVE.entrySet().removeIf(e->e.getValue().isStopped());
        });
    }
    private static final class ClipSound extends AbstractSoundInstance implements TickableSoundInstance,FabricSoundInstance {
        private final byte[] samples;private int age;private final Object world;
        ClipSound(BlockPos pos,byte[] samples){super(FabricSoundInstance.EMPTY_SOUND,SoundSource.RECORDS,SoundInstance.createUnseededRandom());
            this.samples=samples;world=Minecraft.getInstance().level;x=pos.getX()+.5;y=pos.getY()+.5;z=pos.getZ()+.5;volume=.5F;pitch=1;attenuation=Attenuation.LINEAR;}
        @Override public WeighedSoundEvents getOrResolve(SoundManager manager){
            if(soundEvent==null){sound=new Sound(FabricSoundInstance.EMPTY_SOUND,ConstantFloat.of(1),ConstantFloat.of(1),1,Sound.Type.FILE,true,false,48);
                soundEvent=new WeighedSoundEvents(FabricSoundInstance.EMPTY_SOUND,null);soundEvent.addSound(sound);}return soundEvent;
        }
        @Override public CompletableFuture<AudioStream> getAudioStream(SoundBufferLibrary library,Identifier id,boolean loop){
            return CompletableFuture.completedFuture(new AudioStream(){int offset;
                public AudioFormat getFormat(){return new AudioFormat(48000,8,1,false,false);}
                public ByteBuffer read(int requested){int size=Math.min(Math.max(0,requested),samples.length-offset);if(size==0)return null;var out=ByteBuffer.allocateDirect(size);
                    for(int i=0;i<size;i++)out.put((byte)(samples[offset+i]^0x80));offset+=size;return out.flip();}
                public void close(){offset=samples.length;}
            });
        }
        @Override public void tick(){age++;}
        @Override public boolean isStopped(){return Minecraft.getInstance().level!=world||age>20+(samples.length*20/48000);}
    }
}
