package local.peepo;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Only edits the matching open companion menu and the row identity it displayed. */
public record CompanionEditPayload(int menu,int row,BlockPos anchor,int action,String text) implements CustomPacketPayload {
    public static final Type<CompanionEditPayload> TYPE=new Type<>(PeepoMod.id("companion_edit"));
    public static final StreamCodec<RegistryFriendlyByteBuf,CompanionEditPayload> CODEC=StreamCodec.composite(ByteBufCodecs.VAR_INT,CompanionEditPayload::menu,ByteBufCodecs.VAR_INT,CompanionEditPayload::row,BlockPos.STREAM_CODEC,CompanionEditPayload::anchor,ByteBufCodecs.VAR_INT,CompanionEditPayload::action,ByteBufCodecs.stringUtf8(CompanionNames.LIMIT),CompanionEditPayload::text,CompanionEditPayload::new);
    public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
