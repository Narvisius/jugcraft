package local.peepo;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record PlannerActionPayload(int hand,int action) implements CustomPacketPayload {
    public static final Type<PlannerActionPayload> TYPE=new Type<>(PeepoMod.id("planner_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf,PlannerActionPayload> CODEC=StreamCodec.composite(ByteBufCodecs.VAR_INT,PlannerActionPayload::hand,ByteBufCodecs.VAR_INT,PlannerActionPayload::action,PlannerActionPayload::new);
    public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
