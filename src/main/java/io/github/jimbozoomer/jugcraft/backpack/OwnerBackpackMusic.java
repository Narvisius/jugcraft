package io.github.jimbozoomer.jugcraft.backpack;

import java.util.*;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;

/** Real discs, explicit playback and bounded playlists. Sound follows the carrier's player entity. */
final class OwnerBackpackMusic {
    private OwnerBackpackMusic(){}
    private record Playing(MinecraftServer server,Object anchor,ServerLevel level,Holder<JukeboxSong> song,long until,List<UUID> listeners){}
    private static final Set<OwnerBackpackEntity> PLACED=Collections.newSetFromMap(new WeakHashMap<>());
    static void track(OwnerBackpackEntity bag){PLACED.add(bag);}
    private static final Map<ItemStack,Playing> PLAYING=new IdentityHashMap<>();
    private static List<Holder<JukeboxSong>> songs(ItemStack pack){
        var bag=new OwnerBackpackInventory(pack,()->{});var list=new ArrayList<Holder<JukeboxSong>>();
        int max=OwnerBackpacks.has(pack,"advanced_jukebox_upgrade")?16:1;
        for(int i=0;i<bag.getContainerSize()&&list.size()<max;i++)JukeboxSong.fromStack(bag.getItem(i)).ifPresent(list::add);
        return list;
    }
    private static boolean enabled(ItemStack pack){return OwnerBackpacks.has(pack,"jukebox_upgrade")||OwnerBackpacks.has(pack,"advanced_jukebox_upgrade");}
    static boolean button(ServerPlayer player,ItemStack pack,int action){
        if(!enabled(pack))return false;
        var tag=pack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
        if(action==40)tag.putBoolean("PackMusicOn",!tag.getBooleanOr("PackMusicOn",false));
        else if(action==41)tag.putInt("PackMusicIndex",Math.floorMod(tag.getIntOr("PackMusicIndex",0)+1,Math.max(1,songs(pack).size())));
        else if(action==42&&OwnerBackpacks.has(pack,"advanced_jukebox_upgrade")){
            int mode=Math.floorMod(tag.getIntOr("PackMusicMode",0)+1,3);tag.putInt("PackMusicMode",mode);
            player.sendOverlayMessage(net.minecraft.network.chat.Component.literal("Music: "+new String[]{"play once","loop playlist","shuffle"}[mode]));
        }else return false;
        pack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));stop(pack);player.getInventory().setChanged();return true;
    }
    static void tick(MinecraftServer server){
        var seen=Collections.newSetFromMap(new IdentityHashMap<ItemStack,Boolean>());
        for(var player:server.getPlayerList().getPlayers())for(var pack:OwnerBackpacks.carried(player)){
            seen.add(pack);
            if(!player.isAlive()||player.isSpectator()){stop(pack);continue;}
            play(server,pack,player,player.level(),player.position(),player);
        }
        var iterator=PLACED.iterator();
        while(iterator.hasNext()){
            var bag=iterator.next();
            if(bag.isRemoved()||!(bag.getLevel() instanceof ServerLevel level)||level.getServer()!=server||!level.hasChunkAt(bag.getBlockPos())||level.getBlockEntity(bag.getBlockPos())!=bag){iterator.remove();continue;}
            var pack=bag.carrier();seen.add(pack);
            var before=pack.get(DataComponents.CUSTOM_DATA);
            play(server,pack,bag,level,Vec3.atCenterOf(bag.getBlockPos()),null);
            if(!Objects.equals(before,pack.get(DataComponents.CUSTOM_DATA)))bag.setChanged();
        }
        for(var pack:new ArrayList<>(PLAYING.keySet()))if(!seen.contains(pack))stop(pack);
    }
    private static void play(MinecraftServer server,ItemStack pack,Object anchor,ServerLevel level,Vec3 center,ServerPlayer follow){
        var settings=pack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
        if(!enabled(pack)||!settings.getBooleanOr("PackMusicOn",false)){stop(pack);return;}
        var queue=songs(pack);var playing=PLAYING.get(pack);
        if(playing!=null&&(playing.anchor!=anchor||playing.level!=level||!queue.contains(playing.song))){stop(pack);playing=null;}
        if(playing!=null&&server.getTickCount()<playing.until)return;
        int index=Math.floorMod(settings.getIntOr("PackMusicIndex",0),Math.max(1,queue.size()));
        if(playing!=null){
            stop(pack);int mode=settings.getIntOr("PackMusicMode",0);
            if(mode==0){CustomData.update(DataComponents.CUSTOM_DATA,pack,t->t.putBoolean("PackMusicOn",false));return;}
            index=mode==2?level.getRandom().nextInt(Math.max(1,queue.size())):(index+1)%Math.max(1,queue.size());
        }
        if(queue.isEmpty())return;
        int chosen=index;CustomData.update(DataComponents.CUSTOM_DATA,pack,t->t.putInt("PackMusicIndex",chosen));
        var song=queue.get(index);
        if(follow!=null)level.playSeededSound(null,follow,song.value().soundEvent(),SoundSource.RECORDS,1,1,level.getRandom().nextLong());
        else level.playSeededSound(null,center.x,center.y,center.z,song.value().soundEvent(),SoundSource.RECORDS,1,1,level.getRandom().nextLong());
        var listeners=level.players().stream().filter(p->p.distanceToSqr(center)<=4096).map(ServerPlayer::getUUID).toList();
        PLAYING.put(pack,new Playing(server,anchor,level,song,(long)server.getTickCount()+song.value().lengthInTicks()+20,listeners));
    }
    private static void stop(ItemStack pack){
        var previous=PLAYING.remove(pack);if(previous==null)return;
        for(var id:previous.listeners){var listener=previous.server.getPlayerList().getPlayer(id);if(listener!=null)listener.connection.send(new ClientboundStopSoundPacket(previous.song.value().soundEvent().value().location(),SoundSource.RECORDS));}
    }
    static void clear(){PLAYING.clear();PLACED.clear();}
}
