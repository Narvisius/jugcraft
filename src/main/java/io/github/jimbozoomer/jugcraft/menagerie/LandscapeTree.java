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
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

/** Serialized, bounded tree profiles retaining supplied materials and alternate foliage. */
public record LandscapeTree(String style,Holder<BlockStateProvider> trunk,Holder<BlockStateProvider> leaves,
        Holder<BlockStateProvider> alternate,Holder<BlockStateProvider> hanging,int minHeight,int maxHeight,int radius,int width) implements Feature {
    public static final MapCodec<LandscapeTree> CODEC=RecordCodecBuilder.mapCodec(i->i.group(
        Codec.STRING.fieldOf("style").forGetter(LandscapeTree::style),BlockStateProvider.CODEC.fieldOf("trunk").forGetter(LandscapeTree::trunk),
        BlockStateProvider.CODEC.fieldOf("leaves").forGetter(LandscapeTree::leaves),
        BlockStateProvider.CODEC.optionalFieldOf("alternate",BlockStateProvider.holderOf(Blocks.AIR)).forGetter(LandscapeTree::alternate),
        BlockStateProvider.CODEC.optionalFieldOf("hanging",BlockStateProvider.holderOf(Blocks.AIR)).forGetter(LandscapeTree::hanging),
        Codec.intRange(1,40).fieldOf("min_height").forGetter(LandscapeTree::minHeight),Codec.intRange(1,40).fieldOf("max_height").forGetter(LandscapeTree::maxHeight),
        Codec.intRange(1,6).optionalFieldOf("radius",3).forGetter(LandscapeTree::radius),Codec.intRange(1,4).optionalFieldOf("width",1).forGetter(LandscapeTree::width)
    ).apply(i,LandscapeTree::new));
    @Override public MapCodec<? extends Feature> codec(){return CODEC;}
    @Override public boolean place(WorldGenLevel l,ChunkGenerator g,RandomSource r,BlockPos origin){
        var soil=l.getBlockState(origin.below());if(!soil.is(BlockTags.DIRT)&&!soil.is(BlockTags.NYLIUM)&&!soil.is(Blocks.END_STONE)&&!(style.equals("palm")&&soil.is(BlockTags.SAND)))return false;
        int height=minHeight+r.nextInt(Math.max(1,maxHeight-minHeight+1));if(origin.getY()+height+6>=l.getMaxY())return false;
        var plan=new LinkedHashMap<BlockPos,BlockState>();var logs=new ArrayList<BlockPos>();Direction lean=Direction.Plane.HORIZONTAL.getRandomDirection(r);
        for(int y=0;y<height;y++)for(int x=0;x<width;x++)for(int z=0;z<width;z++){
            var at=origin.offset(x,y,z).relative(lean,style.equals("palm")?y/5:0);plan.put(at,trunk.value().getState(l,r,at));logs.add(at);
        }
        var top=origin.above(height-1).relative(lean,style.equals("palm")?(height-1)/5:0);
        boolean cone=Set.of("pine","taiga","redwood","fir").contains(style);
        if(style.equals("palm")){
            foliage(l,r,plan,top.above());for(Direction d:Direction.Plane.HORIZONTAL)for(int n=1;n<=radius+2;n++){var at=top.relative(d,n).above(n<2?1:n>radius?-1:0);if(n<=2){plan.put(at,trunk.value().getState(l,r,at));logs.add(at);}else foliage(l,r,plan,at);foliage(l,r,plan,at.relative(d.getClockWise()));}
        }else if(cone){
            int crown=Math.min(height-1,Math.max(4,height*2/3));for(int y=-crown;y<=1;y++){int spread=y>0?0:Math.min(radius,1+(-y)/3);if(y%2==0&&spread>1)spread--;disk(l,r,plan,top.above(y),spread);}
        }else{
            if(Set.of("big","mahogany","empyreal","umbran","magic","bayou","cypress").contains(style))for(Direction d:Direction.Plane.HORIZONTAL){
                int reach=Math.min(4,radius);var start=top.below(2+r.nextInt(2));for(int n=1;n<=reach;n++){var at=start.relative(d,n).above(n/2);plan.put(at,trunk.value().getState(l,r,at));logs.add(at);}var tip=start.relative(d,reach).above(reach/2);for(int y=-1;y<=1;y++)disk(l,r,plan,tip.above(y),Math.max(1,radius-1-Math.abs(y)));
            }
            for(int y=-2;y<=2;y++)disk(l,r,plan,top.above(y),Math.max(1,radius-Math.max(0,y)));
        }
        // Keep native leaf decay correct, including distant branch crowns.
        for(var entry:plan.entrySet())if(entry.getValue().hasProperty(LeavesBlock.DISTANCE)){
            int distance=7;for(var log:logs)distance=Math.min(distance,log.distManhattan(entry.getKey()));
            entry.setValue(entry.getValue().setValue(LeavesBlock.DISTANCE,Math.clamp(distance,1,7)).setValue(LeavesBlock.PERSISTENT,false));
        }
        if(Set.of("bayou","cypress","umbran").contains(style))for(Direction d:Direction.Plane.HORIZONTAL){var at=origin.relative(d,width);if(l.getBlockState(at).canBeReplaced()){plan.put(at,trunk.value().getState(l,r,at));logs.add(at);}}
        for(var at:List.copyOf(plan.keySet()))if(plan.get(at).hasProperty(LeavesBlock.DISTANCE)&&r.nextInt(8)==0){var below=at.below();var adorn=hanging.value().getState(l,r,below);if(!adorn.isAir()&&!plan.containsKey(below))plan.put(below,adorn);}
        if(plan.size()>16000)return false;
        for(var at:plan.keySet())if(!l.ensureCanWrite(at)||!l.isStateAtPosition(at,s->s.isAir()||s.is(BlockTags.REPLACEABLE_BY_TREES)||s.is(BlockTags.LEAVES)||s.is(Blocks.WATER)))return false;
        plan.forEach((at,s)->l.setBlock(at,s,Block.UPDATE_CLIENTS));return true;
    }
    private void disk(WorldGenLevel l,RandomSource r,Map<BlockPos,BlockState> plan,BlockPos p,int size){for(int x=-size;x<=size;x++)for(int z=-size;z<=size;z++)if(x*x+z*z<=size*size+1)foliage(l,r,plan,p.offset(x,0,z));}
    private void foliage(WorldGenLevel l,RandomSource r,Map<BlockPos,BlockState> plan,BlockPos p){if(plan.containsKey(p))return;var state=r.nextInt(4)==0?alternate.value().getState(l,r,p):Blocks.AIR.defaultBlockState();if(state.isAir())state=leaves.value().getState(l,r,p);if(!state.isAir())plan.put(p,state);}
}
