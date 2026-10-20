package io.github.jimbozoomer.jugcraft.menagerie;

import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

/** Data-driven natural details; bounded plans are checked before any blocks are written. */
public record LandscapeFeature(String shape,Holder<BlockStateProvider> primary,Holder<BlockStateProvider> secondary,int radius,int height) implements Feature {
    public static final MapCodec<LandscapeFeature> CODEC=RecordCodecBuilder.mapCodec(i->i.group(
        Codec.STRING.fieldOf("shape").forGetter(LandscapeFeature::shape),BlockStateProvider.CODEC.fieldOf("primary").forGetter(LandscapeFeature::primary),
        BlockStateProvider.CODEC.optionalFieldOf("secondary",BlockStateProvider.holderOf(Blocks.AIR)).forGetter(LandscapeFeature::secondary),
        Codec.intRange(1,12).optionalFieldOf("radius",3).forGetter(LandscapeFeature::radius),Codec.intRange(1,32).optionalFieldOf("height",5).forGetter(LandscapeFeature::height)
    ).apply(i,LandscapeFeature::new));
    @Override public MapCodec<? extends Feature> codec(){return CODEC;}
    @Override public boolean place(WorldGenLevel l,ChunkGenerator generator,RandomSource r,BlockPos origin){
        var plan=new LinkedHashMap<BlockPos,BlockState>();
        switch(shape){
            case "lake"->{
                for(int x=-radius;x<=radius;x++)for(int z=-radius;z<=radius;z++)if(x*x+z*z<radius*radius){
                    for(int y=-Math.min(4,height);y<=1;y++){
                        var at=origin.offset(x,y,z);boolean edge=x*x+z*z>(radius-1)*(radius-1)||y==-Math.min(4,height);
                        if(edge)put(l,r,plan,at,secondary);else if(y<0)put(l,r,plan,at,primary);else plan.put(at,Blocks.AIR.defaultBlockState());
                    }
                }
            }
            case "rock","crystal","pillar","spine","vent"->{
                for(int y=0;y<height;y++){int size=shape.equals("rock")?Math.max(1,(int)(radius*Math.sin(Math.PI*(y+1)/(height+1)))):Math.max(0,radius*(height-y)/height);
                    for(int x=-size;x<=size;x++)for(int z=-size;z<=size;z++)if(x*x+z*z<=size*size+1)put(l,r,plan,origin.offset(x,y,z),primary);
                }
                if(shape.equals("vent"))put(l,r,plan,origin.above(height),secondary);
            }
            case "column"->{for(int y=0;y<height;y++)put(l,r,plan,origin.above(y),primary);}
            case "log"->{Direction d=Direction.Plane.HORIZONTAL.getRandomDirection(r);for(int n=0;n<height;n++){
                var at=origin.relative(d,n);var state=primary.value().getState(l,r,at);if(state.hasProperty(RotatedPillarBlock.AXIS))state=state.setValue(RotatedPillarBlock.AXIS,d.getAxis());plan.put(at,state);}
            }
            case "mushroom","flower","clover"->{
                for(int y=0;y<height;y++)put(l,r,plan,origin.above(y),primary);
                for(int x=-radius;x<=radius;x++)for(int z=-radius;z<=radius;z++)if(shape.equals("clover")?Math.abs(x)>=Math.abs(z)/2: x*x+z*z<=radius*radius+1){
                    var at=origin.offset(x,height,z);put(l,r,plan,at,secondary);if(shape.equals("mushroom")&&Math.max(Math.abs(x),Math.abs(z))==radius)put(l,r,plan,at.below(),secondary);
                }
            }
            case "vine"->{
                if(!l.getBlockState(origin.above()).isSolidRender()&&!l.getBlockState(origin.above()).is(BlockTags.LEAVES))return false;
                for(int y=0;y<height&&l.isEmptyBlock(origin.below(y));y++)put(l,r,plan,origin.below(y),primary);
            }
            case "pad","shelf","ring"->{
                if(shape.equals("pad")&&!l.getFluidState(origin.below()).is(net.minecraft.world.level.material.Fluids.WATER))return false;
                for(int x=-radius;x<=radius;x++)for(int z=-radius;z<=radius;z++)if(x*x+z*z<=radius*radius&&(!shape.equals("ring")||x*x+z*z>Math.max(0,(radius-1)*(radius-1))))put(l,r,plan,origin.offset(x,0,z),primary);
            }
            case "patch","surface"->{
                for(int attempt=0;attempt<Math.min(96,radius*radius*4);attempt++){
                    var at=origin.offset(r.nextInt(radius*2+1)-radius,2,r.nextInt(radius*2+1)-radius);
                    for(int dy=0;dy<6&&(l.isEmptyBlock(at.below())||l.getFluidState(at.below()).is(net.minecraft.world.level.material.Fluids.WATER));dy++)at=at.below();
                    if(shape.equals("surface")){at=at.below();if(l.getBlockState(at).isSolidRender())put(l,r,plan,at,primary);}
                    else if(l.isEmptyBlock(at)||l.getFluidState(at).is(net.minecraft.world.level.material.Fluids.WATER)){var state=primary.value().getState(l,r,at);if(state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED))state=state.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED,l.getFluidState(at).is(net.minecraft.world.level.material.Fluids.WATER));if(state.canSurvive(l,at))plan.put(at,state);}
                }
            }
            default->throw new IllegalStateException("Unknown landscape shape "+shape);
        }
        if(plan.isEmpty()||plan.size()>16000)return false;
        for(var entry:plan.entrySet()){
            var at=entry.getKey();if(!l.ensureCanWrite(at)||l.getBlockEntity(at)!=null||l.getBlockState(at).is(Blocks.BEDROCK))return false;
            if(!Set.of("lake","surface").contains(shape)&&!l.getBlockState(at).canBeReplaced()&&!l.getBlockState(at).is(BlockTags.LEAVES))return false;
        }
        plan.forEach((p,s)->l.setBlock(p,s,Block.UPDATE_CLIENTS));return true;
    }
    private static void put(WorldGenLevel l,RandomSource r,Map<BlockPos,BlockState> plan,BlockPos p,Holder<BlockStateProvider> provider){var state=provider.value().getState(l,r,p);if(!state.isAir())plan.put(p,state);}
}
