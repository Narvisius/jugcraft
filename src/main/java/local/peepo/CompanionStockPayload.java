package local.peepo;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Small changed-only stock snapshot for the open menu, including shared counted-container labels. */
public record CompanionStockPayload(int menu,int row,BlockPos anchor,int revision,List<Entry> entries,List<String> outputs) implements CustomPacketPayload {
    public record Entry(int target,int restock,int stored,int underway,boolean available){
        public static final StreamCodec<RegistryFriendlyByteBuf,Entry> CODEC=StreamCodec.composite(
            ByteBufCodecs.VAR_INT,Entry::target,ByteBufCodecs.VAR_INT,Entry::restock,ByteBufCodecs.VAR_INT,Entry::stored,
            ByteBufCodecs.VAR_INT,Entry::underway,ByteBufCodecs.BOOL,Entry::available,Entry::new);
    }
    public static final Type<CompanionStockPayload> TYPE=new Type<>(PeepoMod.id("companion_stock"));
    public static final StreamCodec<RegistryFriendlyByteBuf,CompanionStockPayload> CODEC=StreamCodec.composite(
        ByteBufCodecs.VAR_INT,CompanionStockPayload::menu,ByteBufCodecs.VAR_INT,CompanionStockPayload::row,
        BlockPos.STREAM_CODEC,CompanionStockPayload::anchor,ByteBufCodecs.VAR_INT,CompanionStockPayload::revision,
        Entry.CODEC.apply(ByteBufCodecs.list(9)),CompanionStockPayload::entries,
        ByteBufCodecs.stringUtf8(128).apply(ByteBufCodecs.list(4)),CompanionStockPayload::outputs,CompanionStockPayload::new);
    public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
