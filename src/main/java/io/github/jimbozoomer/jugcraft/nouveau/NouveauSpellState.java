package io.github.jimbozoomer.jugcraft.nouveau;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.*;

/** Temporary block changes survive restarts and never overwrite a later player edit. */
public final class NouveauSpellState extends SavedData {
    private record Change(BlockPos pos,BlockState before,BlockState after,long until) {
        static final Codec<Change> CODEC=RecordCodecBuilder.create(i->i.group(BlockPos.CODEC.fieldOf("pos").forGetter(Change::pos),
            BlockState.CODEC.fieldOf("before").forGetter(Change::before),BlockState.CODEC.fieldOf("after").forGetter(Change::after),
            Codec.LONG.fieldOf("until").forGetter(Change::until)).apply(i,Change::new));
    }
    private static final Codec<NouveauSpellState> CODEC=Change.CODEC.listOf().xmap(NouveauSpellState::new,s->List.copyOf(s.changes));
    private static final SavedDataType<NouveauSpellState> TYPE=new SavedDataType<>(Jugcraft.id("nouveau_temporary_blocks"),NouveauSpellState::new,CODEC,null);
    private final List<Change> changes;
    private int cursor;
    private NouveauSpellState(){this(List.of());}
    private NouveauSpellState(List<Change> changes){this.changes=new ArrayList<>(changes);}
    public static void register(){ServerTickEvents.END_LEVEL_TICK.register(NouveauSpellState::tick);}
    public static boolean replace(ServerLevel l,BlockPos p,BlockState replacement,int duration){
        if(!l.hasChunkAt(p)||l.getBlockEntity(p)!=null)return false;
        var data=l.getDataStorage().computeIfAbsent(TYPE);if(data.changes.size()>=4096||data.changes.stream().anyMatch(c->c.pos.equals(p)))return false;
        var old=l.getBlockState(p);if(old.equals(replacement))return false;
        if(!l.setBlock(p,replacement,Block.UPDATE_ALL))return false;
        data.changes.add(new Change(p.immutable(),old,replacement,l.getGameTime()+Math.clamp(duration,20,2400)));data.setDirty();return true;
    }
    private static void tick(ServerLevel l){
        var data=l.getDataStorage().computeIfAbsent(TYPE);
        for(int n=0;n<16&&!data.changes.isEmpty();n++){
            data.cursor=Math.floorMod(data.cursor,data.changes.size());var c=data.changes.get(data.cursor);
            if(c.until>l.getGameTime()||!l.hasChunkAt(c.pos)){data.cursor++;continue;}
            if(l.getBlockState(c.pos).equals(c.after)&&l.getBlockEntity(c.pos)==null)l.setBlock(c.pos,c.before,Block.UPDATE_ALL);
            data.changes.remove(data.cursor);data.setDirty();
        }
    }
}
