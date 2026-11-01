package io.github.jimbozoomer.jugcraft.wilds;

import com.mojang.serialization.MapCodec;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.Optional;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.pieces.*;
import net.minecraft.world.level.levelgen.structure.templatesystem.*;

/** Authored sleeping Frostmaw site with source elevation and dry-ground rules. */
public final class WildFrostSite extends Structure {
    public static final MapCodec<WildFrostSite> CODEC=simpleCodec(WildFrostSite::new);
    public static final StructureType<WildFrostSite> TYPE=()->CODEC;
    public static final StructurePieceType PIECE=(c,t)->new Piece(c.structureTemplateManager(),t);
    public WildFrostSite(StructureSettings settings){super(settings);}
    static void register(){Registry.register(BuiltInRegistries.STRUCTURE_TYPE,Jugcraft.id("wilds_frost_site"),TYPE);Registry.register(BuiltInRegistries.STRUCTURE_PIECE,Jugcraft.id("wilds_frost_site"),PIECE);}
    @Override public StructureType<?> type(){return TYPE;}
    @Override protected Optional<GenerationStub> findGenerationPoint(GenerationContext c){
        int x=c.chunkPos().getMinBlockX()+7,z=c.chunkPos().getMinBlockZ()+7;
        int y=c.chunkGenerator().getBaseHeight(x,z,Heightmap.Types.WORLD_SURFACE_WG,c.heightAccessor(),c.randomState());
        if(y<50||y>100)return Optional.empty();
        var column=c.chunkGenerator().getBaseColumn(x,z,c.heightAccessor(),c.randomState());
        if(!column.getBlock(y-1).getFluidState().isEmpty())return Optional.empty();
        var anchor=new BlockPos(x,y,z);
        return Optional.of(new GenerationStub(anchor,b->b.addPiece(new Piece(c.structureTemplateManager(),anchor.above(),Rotation.values()[c.random().nextInt(4)]))));
    }
    public static final class Piece extends TemplateStructurePiece {
        Piece(StructureTemplateManager manager,BlockPos at,Rotation rotation){super(PIECE,0,manager,Jugcraft.id("wilds/frostmaw_spawn"),"jugcraft:wilds/frostmaw_spawn",settings(rotation),at);}
        Piece(StructureTemplateManager manager,CompoundTag tag){super(PIECE,tag,manager,id->settings(Rotation.valueOf(tag.getStringOr("rotation","NONE"))));}
        private static StructurePlaceSettings settings(Rotation rotation){return new StructurePlaceSettings().setRotation(rotation).setIgnoreEntities(false).setFinalizeEntities(true).addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK);}
        @Override protected void addAdditionalSaveData(StructurePieceSerializationContext c,CompoundTag tag){super.addAdditionalSaveData(c,tag);tag.putString("rotation",placeSettings.getRotation().name());}
        @Override protected void handleDataMarker(String name,BlockPos at,ServerLevelAccessor level,RandomSource random,BoundingBox clip){}
    }
}
