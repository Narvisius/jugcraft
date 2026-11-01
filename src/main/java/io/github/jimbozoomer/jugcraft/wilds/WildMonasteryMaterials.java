package io.github.jimbozoomer.jugcraft.wilds;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.*;

/** The supplied material processor palette, evaluated deterministically per placed block. */
final class WildMonasteryMaterials implements StructureProcessor {
    static final WildMonasteryMaterials INSTANCE=new WildMonasteryMaterials();
    static final MapCodec<WildMonasteryMaterials> CODEC=MapCodec.unit(INSTANCE);
    @Override public MapCodec<? extends StructureProcessor> codec(){return CODEC;}
    @Override public StructureTemplate.StructureBlockInfo processBlock(LevelReader level,BlockPos origin,BlockPos pivot,BlockPos local,StructureTemplate.StructureBlockInfo info,StructurePlaceSettings settings){
        var state=info.state();var random=settings.getRandom(info.pos());Block replacement=null;
        if(state.is(Blocks.SPONGE))replacement=Blocks.AIR;
        else if(state.is(Blocks.PURPUR_STAIRS))replacement=Blocks.DYED_TERRACOTTA.red();
        else if(state.is(Blocks.CONCRETE.white()))replacement=switch(random.nextInt(3)){case 0->Blocks.DIORITE;case 1->Blocks.CALCITE;default->Blocks.CONCRETE.white();};
        else if(state.is(Blocks.CRIMSON_TRAPDOOR))replacement=Blocks.DARK_OAK_TRAPDOOR;
        else if(random.nextFloat()>.7F){
            if(state.is(Blocks.COBBLED_DEEPSLATE))replacement=Blocks.POLISHED_DEEPSLATE;
            else if(state.is(Blocks.COBBLED_DEEPSLATE_SLAB))replacement=Blocks.POLISHED_DEEPSLATE_SLAB;
            else if(state.is(Blocks.COBBLED_DEEPSLATE_STAIRS))replacement=Blocks.POLISHED_DEEPSLATE_STAIRS;
            else if(state.is(Blocks.COBBLED_DEEPSLATE_WALL))replacement=Blocks.POLISHED_DEEPSLATE_WALL;
            else if(state.is(Blocks.POLISHED_ANDESITE))replacement=Blocks.ANDESITE;
            else if(state.is(Blocks.POLISHED_ANDESITE_STAIRS))replacement=Blocks.ANDESITE_STAIRS;
        }
        if(replacement==null)return info;
        BlockState changed=replacement.withPropertiesOf(state);
        return new StructureTemplate.StructureBlockInfo(info.pos(),changed,info.nbt());
    }
}
