package io.github.jimbozoomer.jugcraft.client;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.ribbits.*;
import java.util.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.resources.sounds.*;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;

/** Original profession/umbrella meshes and positional instrument loops, never server-side audio. */
public final class OwnerRibbitsClient {
    private static final DataTicket<String> MODEL=DataTicket.create("jugcraft_ribbit_model",String.class);
    private static final Map<UUID,Music> PLAYING=new HashMap<>();
    private static Object currentLevel;
    private static final Map<Integer,Map<String,Ribbit>> BANDS=new HashMap<>();
    private static final Map<Integer,List<BandVoice>> VOICES=new HashMap<>();
    private OwnerRibbitsClient(){}
    public static void register(){
        for(var type:OwnerRibbits.TYPES.values())EntityRendererRegistry.register(type,c->new GeoEntityRenderer<Ribbit,EntityRenderState>(c,new GeoModel<>(){
            @Override public Identifier getModelResource(GeoRenderState s){String model=s.getGeckolibData(MODEL);return Jugcraft.id("entity/ribbits/"+(model==null?"nitwit_ribbit":model));}
            @Override public Identifier getTextureResource(GeoRenderState s){return Jugcraft.id("textures/newest/ribbits/entity/ribbit.png");}
            @Override public Identifier getAnimationResource(Ribbit e){return Jugcraft.id("entity/ribbits/ribbit");}
        }){
            {shadowRadius=.35F;}
            @Override public void addRenderData(Ribbit e,Void unused,EntityRenderState s,float partial){s.addGeckolibData(MODEL,e.model());}
        });
        ClientTickEvents.END_CLIENT_TICK.register(client->{
            if(currentLevel!=client.level){PLAYING.values().forEach(Music::end);PLAYING.clear();VOICES.values().forEach(v->v.forEach(BandVoice::end));VOICES.clear();BANDS.clear();currentLevel=client.level;}
            if(client.level==null||client.player==null)return;
            PLAYING.values().removeIf(Music::isStopped);BANDS.clear();
            for(var entity:client.level.getEntitiesOfClass(LivingEntity.class,client.player.getBoundingBox().inflate(48),e->e instanceof Ribbit r&&r.action()==1||e.isUsingItem()&&e.getUseItem().is(OwnerRibbits.MARACA)).stream().limit(128).toList()){
                if(entity instanceof Ribbit r){BANDS.computeIfAbsent(r.bandId(),k->new HashMap<>()).putIfAbsent(r.instrument(),r);continue;}
                if(PLAYING.containsKey(entity.getUUID()))continue;var sound=new Music(entity,"maraca");PLAYING.put(entity.getUUID(),sound);client.getSoundManager().play(sound);
            }
            VOICES.entrySet().removeIf(e->{if(BANDS.containsKey(e.getKey())&&e.getValue().stream().noneMatch(BandVoice::isStopped))return false;e.getValue().forEach(BandVoice::end);return true;});
            for(int id:BANDS.keySet())if(!VOICES.containsKey(id)){
                // All stems start together. Joining members unmute their already-running
                // stem, avoiding a second song phase when an NPC joins an existing band.
                var stems=new ArrayList<BandVoice>();for(String instrument:Ribbit.INSTRUMENTS){var stem=new BandVoice(id,instrument);stems.add(stem);client.getSoundManager().play(stem);}VOICES.put(id,stems);
            }
        });
    }
    private static final class BandVoice extends AbstractTickableSoundInstance {
        final int band;final String instrument;
        BandVoice(int band,String instrument){super(OwnerRibbits.sound("music.ribbit."+instrument),SoundSource.RECORDS,SoundInstance.createUnseededRandom());this.band=band;this.instrument=instrument;looping=true;delay=0;attenuation=Attenuation.LINEAR;refresh();}
        @Override public boolean canStartSilent(){return true;}
        void end(){stop();}
        private void refresh(){var members=BANDS.get(band);if(members==null||members.isEmpty()){stop();return;}var player=members.get(instrument);var location=player==null?members.values().iterator().next():player;volume=player==null?0:.6F;x=location.getX();y=location.getY();z=location.getZ();}
        @Override public void tick(){refresh();}
    }
    private static final class Music extends AbstractTickableSoundInstance {
        final LivingEntity entity;final String instrument;
        Music(LivingEntity entity,String instrument){super(OwnerRibbits.sound("music.ribbit."+instrument),SoundSource.RECORDS,SoundInstance.createUnseededRandom());this.entity=entity;this.instrument=instrument;looping=true;delay=0;volume=.6F;attenuation=Attenuation.LINEAR;position();}
        void position(){x=entity.getX();y=entity.getY();z=entity.getZ();}
        void end(){stop();}
        @Override public void tick(){
            boolean active=entity instanceof Ribbit r?r.action()==1&&r.instrument().equals(instrument):entity.isUsingItem()&&entity.getUseItem().is(OwnerRibbits.MARACA);
            var client=Minecraft.getInstance();if(!entity.isAlive()||entity.level()!=client.level||!active||client.player==null||entity.distanceToSqr(client.player)>64*64){stop();return;}position();
        }
    }
}
