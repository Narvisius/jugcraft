package io.github.jimbozoomer.jugcraft.menagerie;

import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;

/** Places adapted owner structure templates through native generation; never modifies an existing save on load. */
public record OwnerTemplateFeature(List<Identifier> templates,int sink) implements Feature {
    public static final MapCodec<OwnerTemplateFeature> CODEC=RecordCodecBuilder.mapCodec(i->i.group(
        Identifier.CODEC.listOf(1,64).fieldOf("templates").forGetter(OwnerTemplateFeature::templates),Codec.intRange(-8,8).optionalFieldOf("sink",0).forGetter(OwnerTemplateFeature::sink)
    ).apply(i,OwnerTemplateFeature::new));
    public OwnerTemplateFeature{templates=List.copyOf(templates);}
    @Override public MapCodec<? extends Feature> codec(){return CODEC;}
    @Override public boolean place(WorldGenLevel l,ChunkGenerator generator,RandomSource random,BlockPos origin){
        var template=l.getLevel().getStructureTemplateManager().get(templates.get(random.nextInt(templates.size())));if(template.isEmpty())return false;
        var size=template.get().getSize();if(size.getX()>64||size.getY()>64||size.getZ()>64)return false;
        var at=origin.offset(-size.getX()/2,-sink,-size.getZ()/2);
        if(!l.ensureCanWrite(at)||!l.ensureCanWrite(at.offset(size.getX()-1,size.getY()-1,size.getZ()-1)))return false;
        return template.get().placeInWorld(l,at,at,new StructurePlaceSettings().setIgnoreEntities(true),random,Block.UPDATE_CLIENTS);
    }
}
