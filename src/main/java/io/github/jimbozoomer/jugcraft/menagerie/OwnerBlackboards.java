package io.github.jimbozoomer.jugcraft.menagerie;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.*;
import net.fabricmc.fabric.api.networking.v1.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.DyeColor;

/** Bounded, revision-checked editing. A replaced board never inherits an old edit session. */
public final class OwnerBlackboards {
    public static final int PIXELS=256;
    private record Edit(UUID token,OwnerSupplementData board,long revision,long expires){}
    private static final Map<UUID,Edit> EDITS=new HashMap<>();
    private OwnerBlackboards(){}
    public static byte[] pixels(byte[] source){
        if(source.length!=PIXELS)return new byte[PIXELS];
        byte[] copy=source.clone();for(int i=0;i<copy.length;i++)if(copy[i]<0||copy[i]>15)copy[i]=0;return copy;
    }
    public static int color(int index){return index==0?0xFF202421:index==1?0xFFF2F1E8:0xFF000000|(index==15?DyeColor.ORANGE:DyeColor.byId(index)).getMapColor().col;}
    public static int index(DyeColor color){return switch(color){case BLACK->0;case WHITE->1;case ORANGE->15;default->color.getId();};}
    public record Open(UUID token,BlockPos pos,byte[] pixels,boolean glowing) implements CustomPacketPayload {
        public static final Type<Open> TYPE=new Type<>(Jugcraft.id("supps_blackboard_open"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Open> CODEC=StreamCodec.of((b,p)->{b.writeUUID(p.token);b.writeBlockPos(p.pos);b.writeBytes(p.pixels);b.writeBoolean(p.glowing);},b->{UUID t=b.readUUID();BlockPos p=b.readBlockPos();byte[] v=new byte[PIXELS];b.readBytes(v);return new Open(t,p,v,b.readBoolean());});
        @Override public Type<Open> type(){return TYPE;}
    }
    public record Save(UUID token,byte[] pixels,boolean cancel) implements CustomPacketPayload {
        public static final Type<Save> TYPE=new Type<>(Jugcraft.id("supps_blackboard_save"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Save> CODEC=StreamCodec.of((b,p)->{b.writeUUID(p.token);b.writeBytes(p.pixels);b.writeBoolean(p.cancel);},b->{UUID t=b.readUUID();byte[] v=new byte[PIXELS];b.readBytes(v);return new Save(t,v,b.readBoolean());});
        @Override public Type<Save> type(){return TYPE;}
    }
    public record Result(UUID token,boolean ok,String reason) implements CustomPacketPayload {
        public static final Type<Result> TYPE=new Type<>(Jugcraft.id("supps_blackboard_result"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Result> CODEC=StreamCodec.of((b,p)->{b.writeUUID(p.token);b.writeBoolean(p.ok);b.writeUtf(p.reason,256);},b->new Result(b.readUUID(),b.readBoolean(),b.readUtf(256)));
        @Override public Type<Result> type(){return TYPE;}
    }
    public static void register(){
        PayloadTypeRegistry.clientboundPlay().register(Open.TYPE,Open.CODEC);PayloadTypeRegistry.clientboundPlay().register(Result.TYPE,Result.CODEC);PayloadTypeRegistry.serverboundPlay().register(Save.TYPE,Save.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(Save.TYPE,(p,c)->save(c.player(),p));
        ServerPlayConnectionEvents.DISCONNECT.register((h,s)->EDITS.remove(h.player.getUUID()));
        ServerLifecycleEvents.SERVER_STOPPED.register(s->EDITS.clear());
    }
    public static void open(ServerPlayer player,OwnerSupplementData board){
        if(!usable(player,board)||board.boardWaxed)return;
        var token=UUID.randomUUID();EDITS.put(player.getUUID(),new Edit(token,board,board.boardRevision,System.nanoTime()+1_800_000_000_000L));
        ServerPlayNetworking.send(player,new Open(token,board.getBlockPos(),board.boardPixels.clone(),board.boardGlow));
    }
    private static boolean usable(ServerPlayer p,OwnerSupplementData d){return p.isAlive()&&!p.isSpectator()&&d.getLevel()==p.level()&&!d.isRemoved()&&p.level().hasChunkAt(d.getBlockPos())&&p.level().getBlockEntity(d.getBlockPos())==d&&p.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(d.getBlockPos()))<=64&&d.block().function.equals("blackboard")&&d.mayEdit(p);}
    private static void save(ServerPlayer player,Save packet){
        var edit=EDITS.get(player.getUUID());if(edit==null||!edit.token.equals(packet.token))return;
        EDITS.remove(player.getUUID());if(packet.cancel)return;
        String error=System.nanoTime()>edit.expires?"This editing session expired. Reopen the board.":!usable(player,edit.board)?"You can no longer edit this board.":edit.board.boardWaxed?"This board was waxed while you were drawing.":edit.revision!=edit.board.boardRevision?"Someone changed this board. Your drawing has not overwritten theirs.":"";
        if(error.isEmpty())for(byte pixel:packet.pixels)if(pixel<0||pixel>15){error="Invalid chalk color.";break;}
        if(error.isEmpty()){edit.board.boardPixels=packet.pixels.clone();edit.board.boardRevision++;edit.board.changed();}
        ServerPlayNetworking.send(player,new Result(packet.token,error.isEmpty(),error));
    }
}
