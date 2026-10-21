package io.github.jimbozoomer.jugcraft.finance;

import com.mojang.serialization.Codec;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.*;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** Paid lottery tickets redeem once. Alchemy-created blanks and copied item data cannot drain the bank. */
public final class ScratchTickets extends SavedData {
    public static final Codec<ScratchTickets> CODEC=Codec.unboundedMap(UUIDUtil.STRING_CODEC,Codec.INT)
        .xmap(ScratchTickets::new,t->t.valid);
    private static final SavedDataType<ScratchTickets> TYPE=new SavedDataType<>(Jugcraft.id("exchange_scratch_tickets"),ScratchTickets::new,CODEC,null);
    private static final String KEY="jugcraft_exchange_ticket";
    private final Map<UUID,Integer> valid=new HashMap<>();
    public ScratchTickets() { }
    private ScratchTickets(Map<UUID,Integer> saved) {valid.putAll(saved);}
    public static ScratchTickets get(MinecraftServer server) {return server.overworld().getDataStorage().computeIfAbsent(TYPE);}
    void issue(ItemStack stack,int kind) {
        UUID id=UUID.randomUUID();valid.put(id,kind);
        var tag=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
        tag.putString(KEY,id.toString());stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));setDirty();
    }
    boolean redeem(ItemStack stack,int kind) {
        var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
        try {
            UUID id=UUID.fromString(data.getStringOr(KEY,""));
            if(!Objects.equals(valid.get(id),kind))return false;
            valid.remove(id);setDirty();return true;
        } catch(IllegalArgumentException e) {return false;}
    }
}
