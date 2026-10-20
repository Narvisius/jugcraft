package io.github.jimbozoomer.jugcraft.menagerie;

import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/** Bounded native tree profiles, including leaning palms, maple sapwood and leaf-supported coconuts. */
public record EcologyTree(String wood) implements Feature {
    public static final MapCodec<EcologyTree> CODEC=RecordCodecBuilder.mapCodec(i->i.group(Codec.STRING.fieldOf("wood").forGetter(EcologyTree::wood)).apply(i,EcologyTree::new));
    @Override public MapCodec<? extends Feature> codec(){return CODEC;}
    @Override public boolean place(WorldGenLevel l,ChunkGenerator generator,RandomSource r,BlockPos origin){
        if(!Set.of("maple","walnut","coconut","azalea","flowering_azalea").contains(wood))return false;
        if(!l.getBlockState(origin.below()).is(BlockTags.DIRT)&&!(wood.equals("coconut")&&l.getBlockState(origin.below()).is(BlockTags.SAND)))return false;
        int height=(wood.equals("coconut")?8:6)+r.nextInt(3);var facing=Direction.Plane.HORIZONTAL.getRandomDirection(r);var plan=new LinkedHashMap<BlockPos,BlockState>();
        Block leaf=wood.equals("maple")?Ecology.block(List.of("green_maple_leaves","orange_maple_leaves","red_maple_leaves","yellow_maple_leaves").get(r.nextInt(4))):wood.contains("azalea")?(wood.equals("flowering_azalea")?Blocks.FLOWERING_AZALEA_LEAVES:Blocks.AZALEA_LEAVES):Ecology.block(wood+"_leaves");
        for(int y=0;y<height;y++)plan.put(origin.above(y).relative(facing,wood.equals("coconut")?y/4:0),Ecology.block(wood.equals("maple")&&y==2?"sappy_maple_log":wood+"_log").defaultBlockState());
        var top=origin.above(height-1).relative(facing,wood.equals("coconut")?(height-1)/4:0);
        if(wood.equals("coconut")){
            for(Direction d:Direction.Plane.HORIZONTAL)for(int n=1;n<=4;n++){var at=top.relative(d,n).above(n<2?1:n>3?-1:0);plan.put(at,leaf.defaultBlockState().setValue(LeavesBlock.DISTANCE,Math.min(6,n)).setValue(LeavesBlock.PERSISTENT,false));}
            plan.put(top.above(),leaf.defaultBlockState().setValue(LeavesBlock.DISTANCE,1));
            for(Direction d:Direction.Plane.HORIZONTAL)plan.put(top.relative(d).below(),Ecology.block("hanging_coconut").defaultBlockState().setValue(BlockStateProperties.AGE_3,2));
        }else for(int y=-2;y<=2;y++){int radius=y==2?1:y==-2?2:3;for(int x=-radius;x<=radius;x++)for(int z=-radius;z<=radius;z++)if(x*x+z*z<=radius*radius+1){var at=top.offset(x,y,z);if(!plan.containsKey(at))plan.put(at,leaf.defaultBlockState().setValue(LeavesBlock.DISTANCE,Math.min(6,Math.abs(x)+Math.abs(z)+Math.abs(y))).setValue(LeavesBlock.PERSISTENT,false));}}
        for(var pos:plan.keySet())if(!l.ensureCanWrite(pos)||!l.isStateAtPosition(pos,s->s.isAir()||s.is(BlockTags.REPLACEABLE_BY_TREES)||s.is(BlockTags.LEAVES)))return false;
        plan.forEach((p,s)->l.setBlock(p,s,Block.UPDATE_CLIENTS));return true;
    }
}
