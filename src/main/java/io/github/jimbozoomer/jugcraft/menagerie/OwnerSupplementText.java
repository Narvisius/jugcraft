package io.github.jimbozoomer.jugcraft.menagerie;

import java.util.*;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import net.fabricmc.fabric.api.networking.v1.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.*;

/** Server-authoritative plain-text editing; never interprets commands, JSON styles or click events. */
public final class OwnerSupplementText {
    private record Edit(UUID token,OwnerSupplementData data,long revision,long expires,int slot){}
    private static final Map<UUID,Edit> EDITS=new HashMap<>();
    private static final Map<UUID,Long> LAST_MESSAGE=new HashMap<>();
    private OwnerSupplementText(){}
    public static boolean text(OwnerSupplementData data){return data.block().function.equals("speaker")||data.block().function.equals("doormat")||OwnerWaySigns.sign(data);}
    public record Open(UUID token,String message,boolean speaker,int mode,int range,boolean sign) implements CustomPacketPayload {
        public static final Type<Open> TYPE=new Type<>(Jugcraft.id("supps_text_open"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Open> CODEC=StreamCodec.of((b,p)->{b.writeUUID(p.token);b.writeUtf(p.message,225);b.writeBoolean(p.speaker);b.writeVarInt(p.mode);b.writeVarInt(p.range);b.writeBoolean(p.sign);},b->new Open(b.readUUID(),b.readUtf(225),b.readBoolean(),b.readVarInt(),b.readVarInt(),b.readBoolean()));
        public Type<Open> type(){return TYPE;}
    }
    public record Save(UUID token,String message,int mode,int range,boolean cancel) implements CustomPacketPayload {
        public static final Type<Save> TYPE=new Type<>(Jugcraft.id("supps_text_save"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Save> CODEC=StreamCodec.of((b,p)->{b.writeUUID(p.token);b.writeUtf(p.message,225);b.writeVarInt(p.mode);b.writeVarInt(p.range);b.writeBoolean(p.cancel);},b->new Save(b.readUUID(),b.readUtf(225),b.readVarInt(),b.readVarInt(),b.readBoolean()));
        public Type<Save> type(){return TYPE;}
    }
    public record Result(UUID token,boolean ok,String reason) implements CustomPacketPayload {
        public static final Type<Result> TYPE=new Type<>(Jugcraft.id("supps_text_result"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Result> CODEC=StreamCodec.of((b,p)->{b.writeUUID(p.token);b.writeBoolean(p.ok);b.writeUtf(p.reason,256);},b->new Result(b.readUUID(),b.readBoolean(),b.readUtf(256)));
        public Type<Result> type(){return TYPE;}
    }
    public record Announcement(String message,int mode,boolean antique) implements CustomPacketPayload {
        public static final Type<Announcement> TYPE=new Type<>(Jugcraft.id("supps_announcement"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Announcement> CODEC=StreamCodec.of((b,p)->{b.writeUtf(p.message,256);b.writeVarInt(p.mode);b.writeBoolean(p.antique);},b->new Announcement(b.readUtf(256),b.readVarInt(),b.readBoolean()));
        public Type<Announcement> type(){return TYPE;}
    }
    public static void register(){
        PayloadTypeRegistry.clientboundPlay().register(Open.TYPE,Open.CODEC);PayloadTypeRegistry.serverboundPlay().register(Save.TYPE,Save.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(Result.TYPE,Result.CODEC);PayloadTypeRegistry.clientboundPlay().register(Announcement.TYPE,Announcement.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(Save.TYPE,(p,c)->save(c.player(),p));
        ServerPlayConnectionEvents.DISCONNECT.register((handler,server)->{EDITS.remove(handler.player.getUUID());LAST_MESSAGE.remove(handler.player.getUUID());});
        ServerLifecycleEvents.SERVER_STOPPED.register(s->{EDITS.clear();LAST_MESSAGE.clear();});
    }
    private static boolean usable(ServerPlayer player,OwnerSupplementData data){return player.isAlive()&&!player.isSpectator()&&data.getLevel()==player.level()&&!data.isRemoved()&&player.level().hasChunkAt(data.getBlockPos())&&player.level().getBlockEntity(data.getBlockPos())==data&&player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(data.getBlockPos()))<=64&&text(data)&&data.mayEdit(player)&&!data.boardWaxed;}
    public static void open(ServerPlayer player,OwnerSupplementData data){
        if(!usable(player,data))return;var token=UUID.randomUUID();EDITS.put(player.getUUID(),new Edit(token,data,data.boardRevision,System.nanoTime()+1_800_000_000_000L,-1));
        ServerPlayNetworking.send(player,new Open(token,data.message,data.block().function.equals("speaker"),data.textMode,data.textRange,false));
    }
    public static void openSign(ServerPlayer player,OwnerSupplementData data,int slot){
        if(!usable(player,data)||!OwnerWaySigns.sign(data)||slot<0||slot>1||data.getItem(slot).isEmpty())return;
        var token=UUID.randomUUID();EDITS.put(player.getUUID(),new Edit(token,data,data.boardRevision,System.nanoTime()+1_800_000_000_000L,slot));
        ServerPlayNetworking.send(player,new Open(token,data.wayTexts[slot],false,0,16,true));
    }
    public static String clean(String input){
        var out=new StringBuilder();input.codePoints().filter(c->c=='\n'||!Character.isISOControl(c)&&c!=167).limit(225).forEach(out::appendCodePoint);
        // The protocol counts UTF-16 characters, not code points.
        if(out.length()>225)out.setLength(Character.isHighSurrogate(out.charAt(224))?224:225);return out.toString();
    }
    private static void save(ServerPlayer player,Save packet){
        var edit=EDITS.get(player.getUUID());if(edit==null||!edit.token.equals(packet.token))return;EDITS.remove(player.getUUID());if(packet.cancel)return;
        String error=System.nanoTime()>edit.expires?"Editing expired. Reopen the block.":!usable(player,edit.data)?"You can no longer edit this block.":edit.revision!=edit.data.boardRevision?"Someone else changed this text. Your draft was not saved.":packet.mode<0||packet.mode>3||packet.range<4||packet.range>64?"Invalid speaker settings.":"";
        String value=clean(packet.message);if(error.isEmpty()&&edit.data.block().function.equals("doormat")){var lines=value.split("\n",-1);if(lines.length>3||Arrays.stream(lines).anyMatch(line->line.length()>75))error="A doormat holds three lines of up to 75 characters.";}
        if(error.isEmpty()&&edit.slot>=0&&(value.contains("\n")||value.length()>75||!OwnerWaySigns.sign(edit.data)||edit.data.getItem(edit.slot).isEmpty()))error="A direction sign holds one line of up to 75 characters.";
        if(error.isEmpty()){if(edit.slot>=0)edit.data.wayTexts[edit.slot]=value;else edit.data.message=value;edit.data.textMode=packet.mode;edit.data.textRange=packet.range;edit.data.boardRevision++;edit.data.changed();}
        ServerPlayNetworking.send(player,new Result(packet.token,error.isEmpty(),error));
    }
    static void tick(ServerLevel level,OwnerSupplementData data){
        boolean powered=level.hasNeighborSignal(data.getBlockPos());boolean rising=powered&&data.power==0;
        if(powered!=(data.power>0)){data.power=powered?15:0;data.changed();}
        if(!rising||data.message.isBlank()||level.getGameTime()<data.pulseUntil)return;
        var owner=Authority.present(level,data.owner);if(owner==null||!Authority.mayChange(level,owner,data.getBlockPos()))return;
        data.pulseUntil=level.getGameTime()+100;data.setChanged();
        for(var player:level.players())if(player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(data.getBlockPos()))<=data.textRange*data.textRange){
            // Many clocks/speakers cannot flood a listener by alternating blocks.
            long now=System.nanoTime();if(now-LAST_MESSAGE.getOrDefault(player.getUUID(),Long.MIN_VALUE/2)<2_000_000_000L)continue;
            LAST_MESSAGE.put(player.getUUID(),now);ServerPlayNetworking.send(player,new Announcement("Speaker: "+data.message,data.textMode,data.antiqueText));
        }
    }
}
