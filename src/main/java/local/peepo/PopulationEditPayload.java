package local.peepo;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Menu-scoped: refresh, add, remove, target -/+, retain -/+. */
public record PopulationEditPayload(int menu,int row,BlockPos anchor,int action,Identifier animal) implements CustomPacketPayload {
    public static final Type<PopulationEditPayload> TYPE=new Type<>(PeepoMod.id("population_edit"));
    public static final StreamCodec<RegistryFriendlyByteBuf,PopulationEditPayload> CODEC=StreamCodec.composite(ByteBufCodecs.VAR_INT,PopulationEditPayload::menu,ByteBufCodecs.VAR_INT,PopulationEditPayload::row,BlockPos.STREAM_CODEC,PopulationEditPayload::anchor,ByteBufCodecs.VAR_INT,PopulationEditPayload::action,ByteBufCodecs.stringUtf8(128).map(Identifier::parse,Identifier::toString),PopulationEditPayload::animal,PopulationEditPayload::new);
    public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
