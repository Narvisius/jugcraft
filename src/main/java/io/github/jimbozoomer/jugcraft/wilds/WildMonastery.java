package io.github.jimbozoomer.jugcraft.wilds;

import com.google.gson.*;
import com.mojang.serialization.MapCodec;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.backpack.OwnerBackpacks;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.pieces.*;
import net.minecraft.world.level.levelgen.structure.templatesystem.*;

/** Bounded assembly from the supplied monastery connector graph and weighted template pools. */
public final class WildMonastery extends Structure {
    public static final MapCodec<WildMonastery> CODEC=simpleCodec(WildMonastery::new);
    public static final StructureType<WildMonastery> TYPE=()->CODEC;
    public static final StructurePieceType PIECE=(c,t)->new Piece(c.structureTemplateManager(),t);
    private static JsonObject pools;
    public WildMonastery(StructureSettings s){super(s);}
    static void register(){Registry.register(BuiltInRegistries.STRUCTURE_PROCESSOR,Jugcraft.id("wilds_monastery_materials"),WildMonasteryMaterials.CODEC);pools=OwnerBackpacks.source("newest_wilds_monastery.json").getAsJsonObject("pools");Registry.register(BuiltInRegistries.STRUCTURE_TYPE,Jugcraft.id("wilds_monastery"),TYPE);Registry.register(BuiltInRegistries.STRUCTURE_PIECE,Jugcraft.id("wilds_monastery"),PIECE);}
    @Override public StructureType<?> type(){return TYPE;}
    private static final String PREFIX="mowziesmobs:monastery/", PATH=PREFIX+"path";
    private record Node(Piece piece,JsonObject spec,int depth,Node parent,BoundingBox occupied,BoundingBox check,BoundingBox interior,String tag,boolean furnishing){}
    private record Socket(Node parent,StructureTemplate.StructureBlockInfo connector,long order){}
    private static int value(JsonObject o,String k,int fallback){return o.has(k)?o.get(k).getAsInt():fallback;}
    private static String string(JsonObject o,String k,String fallback){return o.has(k)?o.get(k).getAsString():fallback;}
    private static boolean flag(JsonObject o,String k,boolean fallback){return o.has(k)?o.get(k).getAsBoolean():fallback;}
    private static JsonObject object(JsonObject o,String k){return o.has(k)&&o.get(k).isJsonObject()?o.getAsJsonObject(k):new JsonObject();}
    private static BlockPos vector(JsonObject o,String k){if(!o.has(k))return BlockPos.ZERO;var a=o.getAsJsonArray(k);return a.size()==3?new BlockPos(a.get(0).getAsInt(),a.get(1).getAsInt(),a.get(2).getAsInt()):BlockPos.ZERO;}
    @Override protected Optional<GenerationStub> findGenerationPoint(GenerationContext c){int x=c.chunkPos().getMiddleBlockX(),z=c.chunkPos().getMiddleBlockZ(),y=c.chunkGenerator().getBaseHeight(x,z,Heightmap.Types.WORLD_SURFACE_WG,c.heightAccessor(),c.randomState());if(y<120||y>200)return Optional.empty();var center=new BlockPos(x,y,z);return Optional.of(new GenerationStub(center,b->new Assembly(c,center).build(b)));}
    private static Identifier runtime(String original){return Jugcraft.id("wilds/"+original.substring(original.indexOf(':')+1));}
    private static VoxelShape shape(BoundingBox b){return Shapes.create(AABB.of(b));}
    private static VoxelShape inside(BoundingBox b){return Shapes.create(AABB.of(b).deflate(.25));}
    private static BoundingBox bounds(Vec3i size,JsonObject spec,BlockPos at,Rotation rotation,String minExtra,String maxExtra){var min=vector(spec,"bounds_min_offset").offset(vector(spec,minExtra)).rotate(rotation).offset(at);var max=new BlockPos(size.getX()-1,size.getY()-1,size.getZ()-1).offset(vector(spec,"bounds_max_offset")).offset(vector(spec,maxExtra)).rotate(rotation).offset(at);return BoundingBox.fromCorners(min,max);}
    private static Node make(GenerationContext c,JsonObject spec,BlockPos at,Rotation rotation,Node parent,boolean furnishing){String name=string(spec,"location","");var template=c.structureTemplateManager().get(runtime(name));if(template.isEmpty())return null;var p=new Piece(c.structureTemplateManager(),name,at,rotation);var size=template.get().getSize();var bounds=object(spec,"bounds");var tags=object(spec,"tags");String tag=flag(tags,"inherits_tag",false)&&parent!=null?parent.tag:"";
        if(tags.has("possible_tags")){var choices=tags.getAsJsonArray("possible_tags");int total=0;for(var e:choices)total+=Math.max(0,value(e.getAsJsonObject(),"weight",1));if(total>0){int draw=c.random().nextInt(total);for(var e:choices){draw-=Math.max(0,value(e.getAsJsonObject(),"weight",1));if(draw<0){tag=string(e.getAsJsonObject(),"tag","");break;}}}}
        return new Node(p,spec,parent==null?0:parent.depth+1,parent,bounds(size,bounds,at,rotation,"",""),bounds(size,bounds,at,rotation,"check_bounds_min_offset","check_bounds_max_offset"),bounds.has("interior_bounds_min_offset")||bounds.has("interior_bounds_max_offset")?bounds(size,bounds,at,rotation,"interior_bounds_min_offset","interior_bounds_max_offset"):null,tag,furnishing);
    }
    private static JsonObject conditions(JsonObject spec){
        var rules=spec.deepCopy();for(var e:object(spec,"conditions").entrySet())rules.add(e.getKey(),e.getValue());return rules;
    }
    private static boolean eligible(JsonObject spec,Node parent,int paths,boolean fallback){var rule=conditions(spec);int depth=parent.depth;if(depth<value(rule,"min_depth",0)||value(rule,"max_depth",-1)>=0&&depth>value(rule,"max_depth",23))return false;
        if(!fallback&&(paths<value(rule,"min_required_paths",Integer.MIN_VALUE)||paths>value(rule,"max_allowed_paths",Integer.MAX_VALUE)))return false;
        if(rule.has("forbidden_parents")){var current=parent;for(int i=0;i<value(rule,"forbidden_parents_depth",1)&&current!=null;i++,current=current.parent)for(var forbidden:rule.getAsJsonArray("forbidden_parents"))if(current.piece.original.equals(forbidden.getAsString()))return false;}
        var tags=object(spec,"tags");if(tags.has("needs_tag")){var current=parent;boolean found=false;for(int i=0;i<value(tags,"needs_tag_depth",1)&&current!=null;i++,current=current.parent)if(current.tag.equals(tags.get("needs_tag").getAsString()))found=true;if(!found)return false;}return true;
    }
    private static boolean heightAllowed(GenerationContext c,Node n){var rule=conditions(n.spec);var at=n.occupied.getCenter();int surface=c.chunkGenerator().getBaseHeight(at.getX(),at.getZ(),Heightmap.Types.WORLD_SURFACE_WG,c.heightAccessor(),c.randomState()),relative=n.piece.templateAnchor().getY()-surface;return relative>=value(rule,"min_height",Integer.MIN_VALUE)&&relative<=value(rule,"max_height",Integer.MAX_VALUE)&&n.occupied.minY()>c.heightAccessor().getMinY()&&n.occupied.maxY()<c.heightAccessor().getMaxY();}
    private static boolean attach(StructureTemplate.StructureBlockInfo a,StructureTemplate.StructureBlockInfo b,boolean twoWay){if(a.nbt()==null||b.nbt()==null)return false;var front=JigsawBlock.getFrontFacing(a.state());if(front!=(twoWay?JigsawBlock.getFrontFacing(b.state()):JigsawBlock.getFrontFacing(b.state()).getOpposite())||!a.nbt().getStringOr("target","").equals(b.nbt().getStringOr("name","")))return false;return a.nbt().getStringOr("joint",front.getAxis().isHorizontal()?"aligned":"rollable").equals("rollable")||JigsawBlock.getTopFacing(a.state())==JigsawBlock.getTopFacing(b.state());}
    private record Option(JsonObject spec,boolean fallback){}
    private static List<Option> options(String name,boolean fallbackOnly,RandomSource r){
        var choices=new ArrayList<Option>();var visited=new HashSet<String>();boolean fallback=fallbackOnly;
        if(fallbackOnly&&pools.has(name))name=string(pools.getAsJsonObject(name),"fallback","minecraft:empty");
        for(int depth=0;depth<4&&!name.equals("minecraft:empty")&&visited.add(name);depth++){
            if(!pools.has(name))break;var pool=pools.getAsJsonObject(name);var weighted=new ArrayList<JsonObject>();
            for(var e:pool.getAsJsonArray("elements")){var entry=e.getAsJsonObject();for(int i=0;i<Math.min(32,value(entry,"weight",1));i++)weighted.add(entry.getAsJsonObject("element"));}
            shuffle(weighted,r);var unique=new ArrayList<JsonObject>();for(var option:weighted)if(!unique.contains(option))unique.add(option);
            unique.sort(Comparator.comparingInt(o->value(o,"priority",0)));for(var spec:unique)choices.add(new Option(spec,fallback));
            name=string(pool,"fallback","minecraft:empty");fallback=true;
        }return choices;
    }
    private static <T> void shuffle(List<T> list,RandomSource r){for(int i=list.size()-1;i>0;i--)Collections.swap(list,i,r.nextInt(i+1));}
    private static int priority(Socket s){String name=s.connector.nbt().getStringOr("pool","");if(name.endsWith("test_zone_pool"))return -100;if(name.contains("/interior/")){if(name.contains("stairs_"))return 95;if(name.endsWith("blocker_pool"))return 96;if(name.endsWith("center_pool"))return 97;if(name.endsWith("wall_corner_pool")||name.endsWith("wall_middle_pool"))return 98;return 100;}return 0;}
    private static final class Assembly {
        final GenerationContext c;final BlockPos center;final RandomSource random;final List<Node> nodes=new ArrayList<>();final Map<String,VoxelShape> special=new HashMap<>();final Set<String> handled=new HashSet<>();final PriorityQueue<Socket> pending=new PriorityQueue<>(Comparator.comparingInt(WildMonastery::priority).thenComparingInt(s->s.parent.depth).thenComparingLong(Socket::order));
        VoxelShape occupied=Shapes.empty(),interiorFree=Shapes.empty();int paths,attempts;long order;
        Assembly(GenerationContext c,BlockPos center){this.c=c;this.center=center;random=c.random();}
        void build(StructurePiecesBuilder builder){var rootSpec=pools.getAsJsonObject(PREFIX+"start_pool").getAsJsonArray("elements").get(0).getAsJsonObject().getAsJsonObject("element");var rotation=Rotation.values()[random.nextInt(4)];var root=make(c,rootSpec,center.offset(vector(object(rootSpec,"bounds"),"offset").rotate(rotation)),rotation,null,false);if(root==null)return;add(root,null);
            while(!pending.isEmpty()&&nodes.size()<256&&attempts<12000){var socket=pending.remove();String key=key(socket.connector);if(!handled.add(key))continue;if(PATH.equals(socket.connector.nbt().getStringOr("target","")))paths=Math.max(0,paths-1);if(!join(socket))place(socket);}
            // The clear trial zone must precede buildings, and furnishings must follow their shells.
            nodes.sort(Comparator.comparingInt(n->value(n.spec,"gen_order",n.furnishing?2:0)));for(var node:nodes)builder.addPiece(node.piece);
        }
        String key(StructureTemplate.StructureBlockInfo b){return b.pos().asLong()+":"+JigsawBlock.getFrontFacing(b.state());}
        void add(Node n,BlockPos used){nodes.add(n);var bounds=object(n.spec,"bounds");boolean reserve=flag(bounds,"place_bounds",true)&&!flag(bounds,"ignore_bounds",false);if(n.furnishing){if(reserve)interiorFree=Shapes.join(interiorFree,shape(n.occupied),BooleanOp.ONLY_FIRST);}else if(reserve)occupied=Shapes.or(occupied,shape(n.occupied));if(n.interior!=null)interiorFree=Shapes.or(interiorFree,shape(n.interior));if(bounds.has("special_bounds"))special.merge(bounds.get("special_bounds").getAsString(),shape(n.occupied),Shapes::or);
            int children=0;for(var junction:n.piece.junctions()){if(used!=null&&used.equals(junction.pos())){handled.add(key(junction));continue;}if(junction.nbt()!=null&&!junction.nbt().getStringOr("pool","minecraft:empty").equals("minecraft:empty")){pending.add(new Socket(n,junction,order++));if(PATH.equals(junction.nbt().getStringOr("target","")))children++;}}paths+=value(conditions(n.spec),"num_paths_override",children);
        }
        boolean fits(Node n,Node parent){var bounds=object(n.spec,"bounds");var check=inside(n.check);if(!flag(bounds,"ignore_bounds",false)){if(n.furnishing){if(Shapes.joinIsNotEmpty(check,interiorFree,BooleanOp.ONLY_FIRST))return false;}else{var exceptParent=Shapes.join(occupied,shape(parent.occupied),BooleanOp.ONLY_FIRST);if(Shapes.joinIsNotEmpty(check,exceptParent,BooleanOp.AND))return false;}}
            if(bounds.has("needs_overlap_bounds")&&!Shapes.joinIsNotEmpty(inside(n.occupied),special.getOrDefault(bounds.get("needs_overlap_bounds").getAsString(),Shapes.empty()),BooleanOp.AND))return false;
            return !bounds.has("forbidden_overlap_bounds")||!Shapes.joinIsNotEmpty(inside(n.occupied),special.getOrDefault(bounds.get("forbidden_overlap_bounds").getAsString(),Shapes.empty()),BooleanOp.AND);
        }
        boolean join(Socket socket){
            if(!PATH.equals(socket.connector.nbt().getStringOr("target","")))return false;
            var destination=socket.connector.pos().relative(JigsawBlock.getFrontFacing(socket.connector.state()));
            for(var other:pending){
                if(other.parent==socket.parent||handled.contains(key(other.connector))||!destination.equals(other.connector.pos()))continue;
                if(attach(socket.connector,other.connector,false)&&attach(other.connector,socket.connector,false)){
                    // Both frontiers now meet. Mark the reciprocal socket before
                    // another piece can cap or overwrite the finished junction.
                    handled.add(key(other.connector));paths=Math.max(0,paths-1);return true;
                }
            }return false;
        }
        void place(Socket socket){var data=socket.connector.nbt();String name=data.getStringOr("pool","");boolean furnishing=socket.parent.furnishing||data.getStringOr("target","").equals(PREFIX+"interior");
            for(var option:options(name,socket.parent.depth>=23,random)){var spec=option.spec;if(++attempts>12000)return;if(string(spec,"element_type","").equals("minecraft:empty_pool_element"))return;String location=string(spec,"location","");if(location.isEmpty()||!eligible(spec,socket.parent,paths,option.fallback))continue;var rotations=new ArrayList<>(List.of(Rotation.values()));shuffle(rotations,random);
                for(var rotation:rotations){var model=c.structureTemplateManager().get(runtime(location));if(model.isEmpty())continue;var candidates=new ArrayList<>(model.get().filterBlocks(BlockPos.ZERO,Piece.settings(rotation),Blocks.JIGSAW));shuffle(candidates,random);
                    for(var connection:candidates){if(!attach(socket.connector,connection,flag(spec,"two_way",false)))continue;var at=socket.connector.pos().relative(JigsawBlock.getFrontFacing(socket.connector.state())).subtract(connection.pos());if(Math.abs(at.getX()-center.getX())>140||Math.abs(at.getZ()-center.getZ())>140)continue;var node=make(c,spec,at,rotation,socket.parent,furnishing);if(node==null||!heightAllowed(c,node)||!fits(node,socket.parent))continue;add(node,flag(spec,"two_way",false)?null:at.offset(connection.pos()));return;}
                }
            }
        }
    }
    public static final class Piece extends TemplateStructurePiece {
        final String original;
        Piece(StructureTemplateManager manager,String original,BlockPos at,Rotation rotation){super(PIECE,0,manager,runtime(original),runtime(original).toString(),settings(rotation),at);this.original=original;}
        Piece(StructureTemplateManager manager,CompoundTag tag){super(PIECE,tag,manager,id->settings(Rotation.valueOf(tag.getStringOr("monastery_rotation","NONE"))));original=tag.getStringOr("monastery_original","mowziesmobs:monastery/monastery_center");}
        static StructurePlaceSettings settings(Rotation rotation){return new StructurePlaceSettings().setRotation(rotation).setIgnoreEntities(false).setFinalizeEntities(true).addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK).addProcessor(JigsawReplacementProcessor.INSTANCE).addProcessor(WildMonasteryMaterials.INSTANCE);}
        List<StructureTemplate.StructureBlockInfo> junctions(){return template.filterBlocks(templatePosition,placeSettings,Blocks.JIGSAW);}BlockPos templateAnchor(){return templatePosition;}
        @Override protected void addAdditionalSaveData(StructurePieceSerializationContext c,CompoundTag tag){super.addAdditionalSaveData(c,tag);tag.putString("monastery_rotation",placeSettings.getRotation().name());tag.putString("monastery_original",original);}
        @Override protected void handleDataMarker(String name,BlockPos at,ServerLevelAccessor l,RandomSource r,BoundingBox box){}
        private static void decorate(WorldGenLevel l,BoundingBox clip,BlockPos at,BlockState state){if(!clip.isInside(at)||l.isOutsideBuildHeight(at)||l.getBlockState(at).isSolidRender()||l.getBlockEntity(at)!=null)return;l.setBlock(at,state,2);}
        private void baseDecoration(WorldGenLevel l,BoundingBox clip){
            var bottomDoor=Blocks.DARK_OAK_TRAPDOOR.defaultBlockState();var topDoor=bottomDoor.setValue(TrapDoorBlock.HALF,Half.TOP);var slab=Blocks.DARK_OAK_SLAB.defaultBlockState();var woodStairs=Blocks.DARK_OAK_STAIRS.defaultBlockState().setValue(StairBlock.HALF,Half.TOP);var wall=Blocks.DEEPSLATE_BRICK_WALL.defaultBlockState();var button=Blocks.DARK_OAK_BUTTON.defaultBlockState();var stoneStairs=Blocks.COBBLED_DEEPSLATE_STAIRS.defaultBlockState();
            BlockState[][] pattern={{bottomDoor,slab,bottomDoor,slab,bottomDoor,slab,bottomDoor},{woodStairs,topDoor,slab.setValue(SlabBlock.TYPE,SlabType.TOP),topDoor,slab.setValue(SlabBlock.TYPE,SlabType.TOP),topDoor,woodStairs},{wall,button,null,null,null,button,wall},{wall,stoneStairs,stoneStairs,stoneStairs,stoneStairs,stoneStairs,wall}};
            for(var marker:template.filterBlocks(templatePosition,placeSettings,Blocks.PURPUR_STAIRS)){var facing=marker.state().getValue(StairBlock.FACING).getOpposite();for(int x=0;x<7;x++)for(int y=0;y<4;y++){var state=pattern[y][x];if(state==null)continue;var at=marker.pos().relative(facing).relative(facing.getCounterClockWise(),x-3).above(1-y);if(!clip.isInside(at))continue;var above=l.getBlockState(at.above());if(above.is(Blocks.DARK_OAK_PLANKS)||above.is(Blocks.DARK_OAK_LOG)||above.is(Blocks.STRIPPED_DARK_OAK_LOG))continue;if(state.is(Blocks.COBBLED_DEEPSLATE_STAIRS)&&placeSettings.getRandom(at).nextFloat()>.7F)state=Blocks.POLISHED_DEEPSLATE_STAIRS.defaultBlockState();if(state.hasProperty(BlockStateProperties.HORIZONTAL_FACING))state=state.setValue(BlockStateProperties.HORIZONTAL_FACING,state.getBlock() instanceof StairBlock?facing.getOpposite():facing);decorate(l,clip,at,state);}}
        }
        @Override public void postProcess(WorldGenLevel l,StructureManager manager,ChunkGenerator generator,RandomSource random,BoundingBox clip,ChunkPos chunk,BlockPos pivot){super.postProcess(l,manager,generator,random,clip,chunk,pivot);
            baseDecoration(l,clip);
            // The supplied deepslate support markers extend into terrain, bounded to the generating chunk.
            for(var support:template.filterBlocks(templatePosition,placeSettings,Blocks.COBBLED_DEEPSLATE)){var at=support.pos();if(!clip.isInside(at))continue;for(int i=0;i<80;i++){at=at.below();if(!clip.isInside(at)||l.isOutsideBuildHeight(at)||l.getBlockState(at).isSolidRender()||l.getBlockEntity(at)!=null)break;l.setBlock(at,(random.nextFloat()>.7?Blocks.POLISHED_DEEPSLATE:Blocks.COBBLED_DEEPSLATE).defaultBlockState(),2);}}
            for(var wood:List.of(Blocks.DARK_OAK_PLANKS,Blocks.DARK_OAK_SLAB,Blocks.DARK_OAK_TRAPDOOR))for(var block:template.filterBlocks(templatePosition,placeSettings,wood)){var at=block.pos();if(!clip.isInside(at)||placeSettings.getRandom(at).nextFloat()>=.15F)continue;var state=l.getBlockState(at);if(state.is(Blocks.DARK_OAK_PLANKS)||state.is(Blocks.DARK_OAK_SLAB)&&state.getValue(SlabBlock.TYPE)!=SlabType.TOP){if(l.getBlockState(at.below()).isAir())decorate(l,clip,at.below(),Blocks.HANGING_ROOTS.defaultBlockState());}else if(state.is(Blocks.DARK_OAK_TRAPDOOR)&&state.getValue(TrapDoorBlock.HALF)==Half.TOP&&!state.getValue(TrapDoorBlock.OPEN)&&l.getBlockState(at.above()).isFaceSturdy(l,at.above(),Direction.DOWN)&&!(l.getBlockState(at.above()).getBlock() instanceof WallBlock))l.setBlock(at,Blocks.HANGING_ROOTS.defaultBlockState(),2);}
        }
    }
}
