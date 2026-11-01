package io.github.jimbozoomer.jugcraft.menagerie;
import net.minecraft.world.level.levelgen.structure.pools.DimensionPadding;

import com.mojang.serialization.MapCodec;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasLookup;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;

/** Original road-sign anchors, with bounded server-thread destination lookup instead of off-thread world access. */
public final class OwnerRoadSigns extends Structure {
    public static final MapCodec<OwnerRoadSigns> CODEC=simpleCodec(OwnerRoadSigns::new);
    public static final StructureType<OwnerRoadSigns> TYPE=()->CODEC;
    private static final Map<ServerLevel,Long> NEXT_SEARCH=new WeakHashMap<>();
    private static final TagKey<Structure> DESTINATIONS=TagKey.create(Registries.STRUCTURE,Jugcraft.id("supps_road_sign_destinations"));
    public OwnerRoadSigns(StructureSettings settings){super(settings);}
    static void register(){
        Registry.register(BuiltInRegistries.STRUCTURE_TYPE,Jugcraft.id("supps_road_sign"),TYPE);
        Registry.register(BuiltInRegistries.FEATURE_TYPE,Jugcraft.id("supps_road_sign_post"),Post.CODEC);
        ServerLifecycleEvents.SERVER_STOPPED.register(server->NEXT_SEARCH.clear());
    }
    @Override public StructureType<?> type(){return TYPE;}
    @Override protected Optional<GenerationStub> findGenerationPoint(GenerationContext c){
        int x=c.chunkPos().getMiddleBlockX(),z=c.chunkPos().getMiddleBlockZ();
        int y=c.chunkGenerator().getBaseHeight(x,z,Heightmap.Types.WORLD_SURFACE_WG,c.heightAccessor(),c.randomState());
        if(y<40||y>105)return Optional.empty();
        var pool=c.registryAccess().lookupOrThrow(Registries.TEMPLATE_POOL).getOrThrow(ResourceKey.create(Registries.TEMPLATE_POOL,Jugcraft.id("supps_road_sign/start_pool")));
        return JigsawPlacement.addPieces(c,pool,Optional.empty(),1,new BlockPos(x,y,z),false,Optional.empty(),new JigsawStructure.MaxDistance(16),PoolAliasLookup.EMPTY,DimensionPadding.ZERO,LiquidSettings.IGNORE_WATERLOGGING);
    }
    static void tick(ServerLevel level,OwnerSupplementData sign){
        if(!sign.roadPending)return;
        if(sign.owner!=null||sign.boardRevision!=0){sign.roadPending=false;sign.setChanged();return;}
        long now=level.getGameTime();if(now%20!=Math.floorMod(sign.getBlockPos().asLong(),20)||NEXT_SEARCH.getOrDefault(level,Long.MIN_VALUE)>now)return;
        // At most one native locate per ten seconds per dimension; radius is two placement rings.
        // No worker thread touches chunks, entities or registries.
        NEXT_SEARCH.put(level,now+200);sign.roadPending=false;sign.setChanged();
        var holders=level.registryAccess().lookupOrThrow(Registries.STRUCTURE).get(DESTINATIONS).orElse(null);
        if(holders==null){sign.wayTexts[0]="No settlement found";sign.changed();return;}
        var found=level.getChunkSource().getGenerator().findNearestMapStructure(level,holders,sign.getBlockPos(),2,false);
        if(found==null){sign.wayTexts[0]="No settlement found";sign.changed();return;}
        var from=sign.getBlockPos();var to=found.getFirst();double dx=to.getX()-from.getX(),dz=to.getZ()-from.getZ();
        // Native wooden arrow points along +X when its yaw is zero.
        sign.wayAngles[0]=Math.floorMod((int)Math.round(Math.toDegrees(Math.atan2(-dz,dx))/22.5),16);
        sign.wayTexts[0]=Math.round(Math.hypot(dx,dz))+" blocks";sign.changed();
    }
    public record Post() implements Feature {
        static final MapCodec<Post> CODEC=MapCodec.unit(Post::new);
        @Override public MapCodec<Post> codec(){return CODEC;}
        @Override public boolean place(WorldGenLevel level,ChunkGenerator generator,RandomSource random,BlockPos origin){
            origin=level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE_WG,origin);var floor=origin.below();var edits=new LinkedHashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();
            if(origin.getY()<40||origin.getY()>105)return false;
            for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++){
                if(Math.abs(x)==2&&Math.abs(z)==2)continue;
                var at=floor.offset(x,0,z);
                if(!level.ensureCanWrite(at.below())||!level.getBlockState(at).isSolidRender()||level.getBlockState(at).hasBlockEntity())return false;
                for(int y=1;y<=4;y++){var p=at.above(y);if(!level.ensureCanWrite(p)||!level.getBlockState(p).isAir())return false;}
                edits.put(at.below(),Blocks.COBBLESTONE.defaultBlockState());
                if(random.nextFloat()>=((x*x+z*z)/5.2-.15))edits.put(at,(random.nextFloat()<.4?Blocks.MOSSY_COBBLESTONE:Blocks.COBBLESTONE).defaultBlockState());
            }
            edits.put(origin,Blocks.COBBLESTONE_WALL.defaultBlockState());
            var board=origin.above();edits.put(board,OwnerSupplementaries.BLOCKS.get("way_sign").defaultBlockState());
            edits.put(origin.above(2),Blocks.SPRUCE_FENCE.defaultBlockState());
            edits.put(origin.above(3),Blocks.SPRUCE_SLAB.defaultBlockState());
            // The source's illuminated stone detail uses the original vanilla material palette.
            var light=origin.east();edits.put(light,Blocks.STONE.defaultBlockState());edits.put(light.above(),Blocks.LANTERN.defaultBlockState());
            for(var e:edits.entrySet())if(!level.setBlock(e.getKey(),e.getValue(),2))return false;
            if(!(level.getBlockEntity(board) instanceof OwnerSupplementData sign))return false;
            sign.wayBase=Blocks.SPRUCE_FENCE.defaultBlockState();sign.setItem(0,new ItemStack(OwnerSupplementaries.ITEMS.get("way_sign_oak")));sign.wayTexts[0]="Finding settlement…";sign.roadPending=true;sign.textColor=0xFFFFFFFF;sign.setChanged();return true;
        }
    }
}
