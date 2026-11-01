package io.github.jimbozoomer.jugcraft.arcane;
import net.minecraft.world.level.block.BonemealSource;

import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.finance.ClaimTransfers;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.phys.shapes.*;

/** Native growth using owner blocks. Missing source feature classes are replaced by bounded native footprints. */
public final class ArcaneGrowth {
    private ArcaneGrowth(){}
    private record Job(ServerLevel level,BlockPos pos,UUID owner,Block plant,int remaining){}
    private static final Map<UUID,Job> JOBS=new LinkedHashMap<>();
    public static void register(){
        ServerTickEvents.END_SERVER_TICK.register(server->{int budget=64;var iter=JOBS.entrySet().iterator();var replacements=new ArrayList<Job>();while(iter.hasNext()&&budget>0){var e=iter.next();var job=e.getValue();var player=Authority.present(job.level,job.owner);if(player==null||!job.level.hasChunkAt(job.pos)||!Authority.mayChange(job.level,player,job.pos)){iter.remove();continue;}int left=job.remaining;for(int n=0;n<8&&left>0&&budget>0;n++,left--,budget--){var state=job.level.getBlockState(job.pos);if(!state.is(job.plant)||!(state.getBlock() instanceof BonemealableBlock grow)||!grow.isValidBonemealTarget(job.level,job.pos,state,BonemealSource.INTERACTION)){left=0;break;}grow.performBonemeal(job.level,job.level.getRandom(),job.pos,state,BonemealSource.INTERACTION);}iter.remove();if(left>0)replacements.add(new Job(job.level,job.pos,job.owner,job.plant,left));}for(var job:replacements)JOBS.put(job.owner,job);});
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(s->JOBS.clear());
    }
    public static final class Meal extends BoneMealItem {
        public Meal(Properties p){super(p);}
        @Override public InteractionResult useOn(UseOnContext c){
            var l=c.getLevel();var pos=c.getClickedPos();var state=l.getBlockState(pos);
            boolean soil=state.is(Blocks.FARMLAND),grow=state.getBlock() instanceof BonemealableBlock b&&b.isValidBonemealTarget(l,pos,state,BonemealSource.INTERACTION);
            if(!soil&&!grow)return super.useOn(c);
            if(c.getPlayer() instanceof ServerPlayer p){if(!Authority.mayChange(p.level(),p,pos)||JOBS.containsKey(p.getUUID())||JOBS.size()>=32)return InteractionResult.FAIL;
                // Bonemeal may affect adjacent positions; refuse cross-claim growth before consuming.
                for(var q:BlockPos.betweenClosed(pos.offset(-8,-1,-8),pos.offset(8,24,8)))if(!l.hasChunkAt(q)||!Authority.mayChange(p.level(),p,q)||!ClaimTransfers.allowed(p.level(),pos,q))return InteractionResult.FAIL;
                if(soil)l.setBlockAndUpdate(pos,ForbiddenArcane.block("magical_farmland").defaultBlockState().setValue(FarmlandBlock.MOISTURE,state.getValue(FarmlandBlock.MOISTURE)));
                else JOBS.put(p.getUUID(),new Job(p.level(),pos.immutable(),p.getUUID(),state.getBlock(),1000));
                c.getItemInHand().consume(1,p);l.levelEvent(1505,pos,0);
            }return InteractionResult.SUCCESS;
        }
    }
    public static final class CorruptSoul extends Item {
        public CorruptSoul(Properties p){super(p);}
        @Override public InteractionResult useOn(UseOnContext c){var s=c.getLevel().getBlockState(c.getClickedPos());if(!s.is(Blocks.OAK_SAPLING)&&!s.is(Blocks.DEAD_BUSH))return InteractionResult.PASS;if(c.getPlayer() instanceof ServerPlayer p){if(!Authority.mayChange(p.level(),p,c.getClickedPos()))return InteractionResult.FAIL;p.level().setBlockAndUpdate(c.getClickedPos(),ForbiddenArcane.block("growing_edelwood").defaultBlockState());c.getItemInHand().consume(1,p);}return InteractionResult.SUCCESS;}
    }
    public static final class Plant extends Block implements BonemealableBlock {
        private final boolean fungus;
        public Plant(boolean fungus,Properties p){super(p.noCollision().randomTicks());this.fungus=fungus;}
        @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return fungus?Block.box(4,0,4,12,9,12):Block.box(2,0,2,14,12,14);}
        @Override protected boolean canSurvive(BlockState s,LevelReader l,BlockPos p){var b=l.getBlockState(p.below());return fungus?b.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK,net.minecraft.resources.Identifier.parse("c:stones")))||b.is(Blocks.STONE)||b.is(Blocks.DEEPSLATE):b.is(BlockTags.DIRT)||b.is(Blocks.FARMLAND);}
        @Override protected BlockState updateShape(BlockState s,LevelReader l,ScheduledTickAccess t,BlockPos p,Direction d,BlockPos q,BlockState n,RandomSource r){return s.canSurvive(l,p)?s:Blocks.AIR.defaultBlockState();}
        @Override protected void randomTick(BlockState s,ServerLevel l,BlockPos p,RandomSource r){if(!fungus&&l.getMaxLocalRawBrightness(p.above())>=9&&r.nextInt(7)==0)performBonemeal(l,r,p,s,BonemealSource.INTERACTION);}
        @Override public boolean isValidBonemealTarget(LevelReader l,BlockPos p,BlockState s,BonemealSource source){return true;}
        @Override public boolean isBonemealSuccess(Level l,RandomSource r,BlockPos p,BlockState s,BonemealSource source){return r.nextFloat()<(fungus?.4F:.45F);}
        @Override public void performBonemeal(ServerLevel l,RandomSource r,BlockPos p,BlockState s,BonemealSource source){BlockPos start=p;boolean mega=false;if(fungus)outer:for(int x=-1;x<=0;x++)for(int z=-1;z<=0;z++){var at=p.offset(x,0,z);if(l.getBlockState(at).is(this)&&l.getBlockState(at.east()).is(this)&&l.getBlockState(at.south()).is(this)&&l.getBlockState(at.east().south()).is(this)){start=at;mega=true;break outer;}}new GrowthFeature(fungus,mega).place(l,l.getChunkSource().getGenerator(),r,start);}
    }
    public record GrowthFeature(boolean fungus,boolean mega) implements Feature {
        public static final MapCodec<GrowthFeature> CODEC=RecordCodecBuilder.mapCodec(i->i.group(Codec.BOOL.optionalFieldOf("fungus",false).forGetter(GrowthFeature::fungus),Codec.BOOL.optionalFieldOf("mega",false).forGetter(GrowthFeature::mega)).apply(i,GrowthFeature::new));
        public MapCodec<? extends Feature> codec(){return CODEC;}
        public boolean place(WorldGenLevel l,ChunkGenerator generator,RandomSource r,BlockPos p){
            var soil=l.getBlockState(p.below());if(!fungus&&!soil.is(BlockTags.DIRT))return false;
            int height=(fungus?(mega?9:4):4)+r.nextInt(3);var plan=new LinkedHashMap<BlockPos,BlockState>();int width=mega?2:1;
            var stem=ForbiddenArcane.block(fungus?"fungyss_stem":"edelwood_log").defaultBlockState();
            for(int y=0;y<height;y++)for(int x=0;x<width;x++)for(int z=0;z<width;z++)plan.put(p.offset(x,y,z),stem);
            if(fungus){int radius=mega?4:2;var cap=ForbiddenArcane.block("fungyss_block").defaultBlockState();for(int y=height-2;y<=height;y++)for(int x=-radius;x<width+radius;x++)for(int z=-radius;z<width+radius;z++){boolean edge=x==-radius||z==-radius||x==width+radius-1||z==width+radius-1;if(y==height||edge)plan.put(p.offset(x,y,z),cap.setValue(HugeMushroomBlock.DOWN,y==height-2).setValue(HugeMushroomBlock.UP,y==height).setValue(HugeMushroomBlock.WEST,x==-radius).setValue(HugeMushroomBlock.EAST,x==width+radius-1).setValue(HugeMushroomBlock.NORTH,z==-radius).setValue(HugeMushroomBlock.SOUTH,z==width+radius-1));}}
            else{plan.put(p.above(height-1),ForbiddenArcane.block("carved_edelwood_log").defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.Plane.HORIZONTAL.getRandomDirection(r)).setValue(EdelwoodBlocks.LEAVES,true));for(var d:Direction.Plane.HORIZONTAL)if(r.nextBoolean())plan.put(p.above(height-2).relative(d),ForbiddenArcane.block("edelwood_branch").defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,d));}
            for(var at:plan.keySet()){if(at.getY()<l.getMinY()||at.getY()>=l.getMaxY()||!l.isStateAtPosition(at,s->s.isAir()||s.getBlock() instanceof Plant||s.is(BlockTags.REPLACEABLE_BY_TREES)))return false;if(l instanceof ServerLevel server&&(!server.hasChunkAt(at)||!ClaimTransfers.allowed(server,p,at)))return false;}
            plan.forEach((at,state)->l.setBlock(at,state,3));return true;
        }
    }
}
