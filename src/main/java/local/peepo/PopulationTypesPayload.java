package local.peepo;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record PopulationTypesPayload(int menu,int row,BlockPos anchor,List<Identifier> types) implements CustomPacketPayload {
    public static final Type<PopulationTypesPayload> TYPE=new Type<>(PeepoMod.id("population_types"));
    public static final StreamCodec<RegistryFriendlyByteBuf,PopulationTypesPayload> CODEC=StreamCodec.composite(ByteBufCodecs.VAR_INT,PopulationTypesPayload::menu,ByteBufCodecs.VAR_INT,PopulationTypesPayload::row,BlockPos.STREAM_CODEC,PopulationTypesPayload::anchor,ByteBufCodecs.stringUtf8(128).map(Identifier::parse,Identifier::toString).apply(ByteBufCodecs.list(32)),PopulationTypesPayload::types,PopulationTypesPayload::new);
    public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
