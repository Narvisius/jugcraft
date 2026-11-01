package io.github.jimbozoomer.jugcraft.backpack;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.component.CustomData;

/** Authorized open-menu snapshots only; client previews never spawn or tick creatures. */
public record OwnerBackpackPreview(int menu, List<CustomData> creatures) implements CustomPacketPayload {
    @Override public Type<OwnerBackpackPreview> type(){return TYPE;}
    public static final Type<OwnerBackpackPreview> TYPE=new Type<>(Jugcraft.id("backpack_creature_preview"));
    public static final StreamCodec<RegistryFriendlyByteBuf,OwnerBackpackPreview> CODEC=StreamCodec.composite(
        ByteBufCodecs.VAR_INT,OwnerBackpackPreview::menu,
        ByteBufCodecs.fromCodec(CustomData.CODEC).apply(ByteBufCodecs.list(16)),OwnerBackpackPreview::creatures,
        OwnerBackpackPreview::new);
}
