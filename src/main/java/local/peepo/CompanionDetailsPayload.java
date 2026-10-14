package local.peepo;

import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Bounded, changed-only snapshots for the authorized open menu, never entity tracking data. */
public record CompanionDetailsPayload(int menu,List<Row> rows) implements CustomPacketPayload {
    public record Row(String target,String text) {
        public static final StreamCodec<RegistryFriendlyByteBuf,Row> CODEC=StreamCodec.composite(
            ByteBufCodecs.stringUtf8(256),Row::target,ByteBufCodecs.stringUtf8(512),Row::text,Row::new);
    }
    public static final Type<CompanionDetailsPayload> TYPE=new Type<>(PeepoMod.id("companion_job_details"));
    public static final StreamCodec<RegistryFriendlyByteBuf,CompanionDetailsPayload> CODEC=StreamCodec.composite(
        ByteBufCodecs.VAR_INT,CompanionDetailsPayload::menu,Row.CODEC.apply(ByteBufCodecs.list(4)),CompanionDetailsPayload::rows,CompanionDetailsPayload::new);
    public Type<? extends CustomPacketPayload> type(){return TYPE;}
    public static String key(CompanionAssignments.Target target){return target==null?"":target.at().dimension().identifier()+"|"+target.at().pos().asLong()+"|"+target.block();}
}
