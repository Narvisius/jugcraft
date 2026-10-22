package io.github.jimbozoomer.jugcraft.menagerie;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.Optional;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.pieces.*;
import net.minecraft.world.level.levelgen.structure.templatesystem.*;

/** Locateable native structures with a cave-floor foundation, preserved template loot and saved pieces. */
public final class CandyLandmark extends Structure {
    public static final MapCodec<CandyLandmark> CODEC=RecordCodecBuilder.mapCodec(i->i.group(
            settingsCodec(i),Identifier.CODEC.fieldOf("template").forGetter(s->s.location)).apply(i,CandyLandmark::new));
    public static final StructureType<CandyLandmark> TYPE=()->CODEC;
    public static final StructurePieceType PIECE=(context,tag)->new Piece(context.structureTemplateManager(),tag);
    private final Identifier location;
    public CandyLandmark(StructureSettings settings,Identifier location){super(settings);this.location=location;}
    public static void register(){
        Registry.register(BuiltInRegistries.STRUCTURE_TYPE,Jugcraft.id("candy_landmark"),TYPE);
        Registry.register(BuiltInRegistries.STRUCTURE_PIECE,Jugcraft.id("candy_landmark"),PIECE);
    }
    @Override public StructureType<?> type(){return TYPE;}
    @Override protected Optional<GenerationStub> findGenerationPoint(GenerationContext c){
        var template=c.structureTemplateManager().get(location);if(template.isEmpty())return Optional.empty();
        var size=template.get().getSize();
        int x=c.chunkPos().getMiddleBlockX(),z=c.chunkPos().getMiddleBlockZ();
        int y=CandyCavity.column(c.seed(),x,z).floor()+1;
        // Leave the river navigable, and reject buildings crossing the cave biome boundary.
        for(int dx=-size.getX()/2;dx<=size.getX()/2;dx+=Math.max(1,size.getX()/4))
            for(int dz=-size.getZ()/2;dz<=size.getZ()/2;dz+=Math.max(1,size.getZ()/4)) {
                if(CandyCavity.column(c.seed(),x+dx,z+dz).river())return Optional.empty();
                if(!c.biomeResolver().getNoiseBiome(Math.floorDiv(x+dx,4),-8,Math.floorDiv(z+dz,4)).is(Jugcraft.id("cavern_candy_cavity")))return Optional.empty();
            }
        y=Math.min(y,CandyCavity.column(c.seed(),x,z).roof()-size.getY()-2);
        var position=new BlockPos(x-size.getX()/2,y,z-size.getZ()/2);
        return Optional.of(new GenerationStub(new BlockPos(x,y,z),builder->builder.addPiece(new Piece(c.structureTemplateManager(),location,position))));
    }
    public static final class Piece extends TemplateStructurePiece {
        public Piece(StructureTemplateManager manager,Identifier id,BlockPos pos){super(PIECE,0,manager,id,id.toString(),settings(),pos);}
        public Piece(StructureTemplateManager manager,CompoundTag tag){super(PIECE,tag,manager,id->settings());}
        private static StructurePlaceSettings settings(){return new StructurePlaceSettings().setIgnoreEntities(true).addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK);}
        @Override protected void handleDataMarker(String name,BlockPos at,ServerLevelAccessor l,RandomSource r,BoundingBox box){}
        @Override public void postProcess(WorldGenLevel l,StructureManager structures,ChunkGenerator generator,RandomSource random,BoundingBox clip,ChunkPos chunk,BlockPos pivot){
            var size=template.getSize();var p=new BlockPos.MutableBlockPos();
            for(int x=Math.max(clip.minX(),templatePosition.getX());x<=Math.min(clip.maxX(),templatePosition.getX()+size.getX()-1);x++)
                for(int z=Math.max(clip.minZ(),templatePosition.getZ());z<=Math.min(clip.maxZ(),templatePosition.getZ()+size.getZ()-1);z++) {
                    int floor=CandyCavity.column(l.getSeed(),x,z).floor();
                    // A solid plinth reaches the natural floor. Clear the whole occupied volume,
                    // including above hills, so sparse template air cannot leave buried interiors.
                    for(int y=Math.min(floor,templatePosition.getY()-1);y<templatePosition.getY()+size.getY();y++) {
                        p.set(x,y,z);if(!clip.isInside(p)||l.getBlockEntity(p)!=null||l.getBlockState(p).is(Blocks.BEDROCK))continue;
                        l.setBlock(p,y<templatePosition.getY()?CandyCavity.block("block_of_chocolate"):Blocks.CAVE_AIR.defaultBlockState(),Block.UPDATE_CLIENTS);
                    }
                }
            super.postProcess(l,structures,generator,random,clip,chunk,pivot);
        }
    }
}
