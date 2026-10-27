package local.peepo;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Values only; the server derives the item and endpoints from the authorized open Filter. */
public record CompanionStockEditPayload(int menu,int row,BlockPos anchor,int revision,int slot,int action,int value,int restock) implements CustomPacketPayload {
    public static final Type<CompanionStockEditPayload> TYPE=new Type<>(PeepoMod.id("companion_stock_edit"));
    public static final StreamCodec<RegistryFriendlyByteBuf,CompanionStockEditPayload> CODEC=StreamCodec.composite(
        ByteBufCodecs.VAR_INT,CompanionStockEditPayload::menu,ByteBufCodecs.VAR_INT,CompanionStockEditPayload::row,
        BlockPos.STREAM_CODEC,CompanionStockEditPayload::anchor,ByteBufCodecs.VAR_INT,CompanionStockEditPayload::revision,
        ByteBufCodecs.VAR_INT,CompanionStockEditPayload::slot,ByteBufCodecs.VAR_INT,CompanionStockEditPayload::action,
        ByteBufCodecs.VAR_INT,CompanionStockEditPayload::value,ByteBufCodecs.VAR_INT,CompanionStockEditPayload::restock,CompanionStockEditPayload::new);
    public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
