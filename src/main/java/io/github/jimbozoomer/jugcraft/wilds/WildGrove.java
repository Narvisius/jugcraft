package io.github.jimbozoomer.jugcraft.wilds;

import com.mojang.serialization.MapCodec;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.pieces.*;
import net.minecraft.world.level.levelgen.structure.templatesystem.*;
import net.minecraft.world.level.storage.loot.LootTable;

/** Authored grove pieces on natural terrain. Only invoked by new-chunk structure generation. */
public final class WildGrove extends Structure {
    public static final MapCodec<WildGrove> CODEC=simpleCodec(WildGrove::new);
    public static final StructureType<WildGrove> TYPE=()->CODEC;
    public static final StructurePieceType PIECE=(c,t)->new Piece(c.structureTemplateManager(),t);
    private static final ResourceKey<LootTable> CHEST=ResourceKey.create(Registries.LOOT_TABLE,Jugcraft.id("chests/wilds_umvuthana_grove"));
    public WildGrove(StructureSettings settings){super(settings);}
    static void register(){Registry.register(BuiltInRegistries.STRUCTURE_TYPE,Jugcraft.id("wilds_grove"),TYPE);Registry.register(BuiltInRegistries.STRUCTURE_PIECE,Jugcraft.id("wilds_grove"),PIECE);}
    @Override public StructureType<?> type(){return TYPE;}
    private static BlockPos surface(GenerationContext c,int x,int z){return new BlockPos(x,c.chunkGenerator().getBaseHeight(x,z,Heightmap.Types.WORLD_SURFACE_WG,c.heightAccessor(),c.randomState())-1,z);}
    private static boolean dry(GenerationContext c,BlockPos p){return p.getY()>=50&&p.getY()<=100&&c.chunkGenerator().getBaseHeight(p.getX(),p.getZ(),Heightmap.Types.OCEAN_FLOOR_WG,c.heightAccessor(),c.randomState())>=p.getY()+1;}
    @Override protected Optional<GenerationStub> findGenerationPoint(GenerationContext c){
        var center=surface(c,c.chunkPos().getMinBlockX()+7,c.chunkPos().getMinBlockZ()+7);if(!dry(c,center))return Optional.empty();
        return Optional.of(new GenerationStub(center,b->{var pieces=new ArrayList<Piece>();var r=c.random();var rot=Rotation.values()[r.nextInt(4)];
            add(c,b,pieces,"umvuthana_firepit",center,Rotation.values()[r.nextInt(4)],false);
            var throne=center.offset(new BlockPos(0,0,9).rotate(rot));add(c,b,pieces,"umvuthi_throne",surface(c,throne.getX(),throne.getZ()),rot,false);
            scatter(c,b,pieces,center,"umvuthana_platform_",2,2+r.nextInt(2),13,20,30);
            scatter(c,b,pieces,center,"umvuthana_tree_",3,2+r.nextInt(3),13,26,30);
            scatter(c,b,pieces,center,"umvuthana_firepit_small_",2,2+r.nextInt(3),8,22,10);
            scatter(c,b,pieces,center,"umvuthana_spike_",4,7+r.nextInt(10),8,22,10);
        }));
    }
    private static Piece add(GenerationContext c,StructurePiecesBuilder b,List<Piece> pieces,String name,BlockPos at,Rotation rot,boolean collision){
        if(!dry(c,at))return null;var p=new Piece(c.structureTemplateManager(),name,at,rot,c.random().nextInt(5)+5);
        if(collision&&pieces.stream().anyMatch(other->other.getBoundingBox().intersects(p.getBoundingBox())))return null;
        b.addPiece(p);pieces.add(p);return p;
    }
    private static void scatter(GenerationContext c,StructurePiecesBuilder b,List<Piece> pieces,BlockPos center,String prefix,int variants,int count,int near,int far,int tries){
        var r=c.random();for(int i=0;i<count;i++)for(int attempt=0;attempt<tries;attempt++){
            double angle=r.nextDouble()*Math.PI*2;int distance=near+r.nextInt(far-near+1);var at=surface(c,center.getX()+(int)(Math.sin(angle)*distance),center.getZ()+(int)(Math.cos(angle)*distance));
            var rot=Rotation.values()[r.nextInt(4)];String name=prefix+(r.nextInt(variants)+1);var p=add(c,b,pieces,name,at,rot,true);if(p==null)continue;
            if(name.equals("umvuthana_platform_2")){var extension=new Piece(c.structureTemplateManager(),"umvuthana_platform_extend",at,rot,0);if(pieces.stream().noneMatch(o->o!=p&&o.getBoundingBox().intersects(extension.getBoundingBox()))){pieces.add(extension);b.addPiece(extension);}}
            break;
        }
    }
    private static BlockPos offset(String name){return switch(name){case "umvuthana_firepit"->new BlockPos(-3,-2,-3);case "umvuthi_throne"->new BlockPos(-9,0,0);case "umvuthana_platform_1"->new BlockPos(-5,0,-5);case "umvuthana_platform_2"->new BlockPos(0,0,-5);case "umvuthana_platform_extend"->new BlockPos(8,1,-2);case "umvuthana_tree_1"->new BlockPos(-5,1,-3);case "umvuthana_tree_2","umvuthana_tree_3"->new BlockPos(-3,1,-3);case "umvuthana_spike_1"->new BlockPos(-1,1,0);case "umvuthana_firepit_small_1","umvuthana_firepit_small_2"->new BlockPos(-1,0,-1);default->new BlockPos(0,1,0);};}
    public static final class Piece extends TemplateStructurePiece {
        private final String name;private final BlockPos anchor;private final int residents;private int residentBits;private boolean bossPlaced;
        Piece(StructureTemplateManager manager,String name,BlockPos anchor,Rotation rot,int residents){super(PIECE,0,manager,Jugcraft.id("wilds/umvuthana/"+name),"jugcraft:wilds/umvuthana/"+name,settings(rot),anchor.offset(offset(name).rotate(rot)));this.name=name;this.anchor=anchor;this.residents=name.equals("umvuthana_firepit")?residents:0;}
        Piece(StructureTemplateManager manager,CompoundTag tag){super(PIECE,tag,manager,id->settings(Rotation.valueOf(tag.getStringOr("grove_rotation","NONE"))));name=tag.getStringOr("grove_name","");anchor=new BlockPos(tag.getIntOr("grove_x",templatePosition.getX()),tag.getIntOr("grove_y",templatePosition.getY()),tag.getIntOr("grove_z",templatePosition.getZ()));residents=Math.clamp(tag.getIntOr("residents",0),0,9);residentBits=tag.getIntOr("resident_bits",0);bossPlaced=tag.getBooleanOr("boss_placed",false);}
        private static StructurePlaceSettings settings(Rotation rot){return new StructurePlaceSettings().setRotation(rot).setIgnoreEntities(true).addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK);}
        @Override protected void addAdditionalSaveData(StructurePieceSerializationContext c,CompoundTag tag){super.addAdditionalSaveData(c,tag);tag.putString("grove_rotation",placeSettings.getRotation().name());tag.putString("grove_name",name);tag.putInt("grove_x",anchor.getX());tag.putInt("grove_y",anchor.getY());tag.putInt("grove_z",anchor.getZ());tag.putInt("residents",residents);tag.putInt("resident_bits",residentBits);tag.putBoolean("boss_placed",bossPlaced);}
        private static void put(ServerLevelAccessor l,BoundingBox clip,BlockPos p,BlockState s){if(!clip.isInside(p)||l.isOutsideBuildHeight(p))return;if(s.hasProperty(BlockStateProperties.WATERLOGGED))s=s.setValue(BlockStateProperties.WATERLOGGED,l.getFluidState(p).is(net.minecraft.tags.FluidTags.WATER));l.setBlock(p,s,2);}
        private static BlockPos ground(ServerLevelAccessor l,BlockPos p){for(int i=0;i<32&&!l.isOutsideBuildHeight(p)&&!l.getBlockState(p).isSolidRender();i++)p=p.below();return p;}
        private static void support(ServerLevelAccessor l,BoundingBox clip,BlockPos p,RandomSource r,int kind){for(int i=0;i<32&&!l.isOutsideBuildHeight(p)&&!l.getBlockState(p).isSolidRender();i++,p=p.below()){BlockState s=switch(kind){case 0->Blocks.ACACIA_FENCE.defaultBlockState();case 1->(r.nextFloat()<.2?OwnerWilds.block("clawed_log"):Blocks.STRIPPED_JUNGLE_WOOD).defaultBlockState();case 2->Blocks.STRIPPED_MANGROVE_LOG.defaultBlockState();case 3->(r.nextBoolean()?Blocks.STRIPPED_MANGROVE_LOG:Blocks.DYED_TERRACOTTA.red()).defaultBlockState();default->Blocks.AIR.defaultBlockState();};put(l,clip,p,s);}}
        @Override protected void handleDataMarker(String marker,BlockPos at,ServerLevelAccessor l,RandomSource r,BoundingBox clip){
            if(!clip.isInside(at))return;put(l,clip,at,Blocks.AIR.defaultBlockState());var rot=placeSettings.getRotation();
            if(marker.equals("support")||marker.equals("trunk")||marker.equals("leg")||marker.equals("base")){support(l,clip,at,r,switch(marker){case "support"->0;case "trunk"->1;case "leg"->2;default->3;});return;}
            if(marker.equals("umvuthi")){if(bossPlaced)return;var boss=WildUmvuthi.TYPE.create(l.getLevel(),EntitySpawnReason.STRUCTURE);if(boss!=null){boss.setPos(net.minecraft.world.phys.Vec3.atBottomCenterOf(at));boss.setYRot(rot.rotate(Direction.NORTH).toYRot());boss.yBodyRot=boss.getYRot();boss.setYHeadRot(boss.getYRot());boss.setPersistenceRequired();if(l.addFreshEntity(boss))bossPlaced=true;}return;}
            if(marker.equals("chest")||marker.equals("chest_under")){if(marker.equals("chest_under")&&!r.nextBoolean())return;var p=marker.equals("chest")?at:ground(l,at).above();createChest(l,clip,r,p,CHEST,Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING,rot.rotate(Direction.NORTH)));return;}
            if(marker.equals("skull")||marker.equals("campfire")){put(l,clip,ground(l,at).above(),marker.equals("skull")?Blocks.SKELETON_SKULL.defaultBlockState().setValue(SkullBlock.ROTATION,r.nextInt(16)):Blocks.CAMPFIRE.defaultBlockState());return;}
            if(marker.startsWith("mask_")){var dir=Direction.byName(marker.substring(5));if(dir==null||dir.getAxis().isVertical())return;var frame=new ItemFrame(l.getLevel(),at,rot.rotate(dir));frame.setItem(new ItemStack(OwnerWilds.item("umvuthana_mask_"+WildUmvuthana.MASKS[r.nextInt(6)])),false);l.addFreshEntity(frame);return;}
            if(marker.startsWith("spike")){var fields=marker.split("_");int logs=part(fields,1,2),fences=part(fields,2,1),bars=part(fields,3,1),skulls=part(fields,4,0);var p=ground(l,at);for(int i=0;i<logs+fences+bars;i++,p=p.above())put(l,clip,p,(i<logs?Blocks.STRIPPED_MANGROVE_LOG:i<logs+fences?Blocks.MANGROVE_FENCE:Blocks.IRON_BARS).defaultBlockState());if(skulls>0&&r.nextInt(10)==0)put(l,clip,p,Blocks.SKELETON_SKULL.defaultBlockState().setValue(SkullBlock.ROTATION,r.nextInt(16)));return;}
            if(marker.startsWith("stairs_")){var direction=Direction.byName(marker.substring(7));if(direction==null||direction.getAxis().isVertical())return;direction=rot.rotate(direction);var p=at;for(int i=1;i<=4;i++){if(l.getBlockState(p).isSolidRender())return;put(l,clip,p,(r.nextBoolean()?Blocks.ACACIA_SLAB:Blocks.SMOOTH_RED_SANDSTONE_SLAB).defaultBlockState().setValue(SlabBlock.TYPE,i%2==1?SlabType.TOP:SlabType.BOTTOM));p=p.relative(direction);if(i%2==0)p=p.below();}p=p.relative(direction.getOpposite());support(l,clip,p,r,2);p=p.relative(direction);for(int i=0;i<32&&!l.getBlockState(p).isSolidRender();i++,p=p.below())put(l,clip,p,Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING,direction));}
        }
        private static int part(String[] parts,int index,int fallback){try{return index<parts.length?Math.clamp(Integer.parseInt(parts[index]),0,8):fallback;}catch(NumberFormatException e){return fallback;}}
        @Override public void postProcess(WorldGenLevel l,StructureManager structures,ChunkGenerator generator,RandomSource random,BoundingBox clip,ChunkPos chunk,BlockPos pivot){
            super.postProcess(l,structures,generator,random,clip,chunk,pivot);
            // Each slot belongs to exactly one generating chunk and is persisted independently.
            for(int i=0;i<residents;i++){if((residentBits&(1<<i))!=0)continue;double angle=i*Math.PI*2/residents;int x=anchor.getX()+(int)Math.round(Math.cos(angle)*2),z=anchor.getZ()+(int)Math.round(Math.sin(angle)*2);if(x<clip.minX()||x>clip.maxX()||z<clip.minZ()||z>clip.maxZ())continue;residentBits|=1<<i;var at=new BlockPos(x,l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z),z);var resident=WildUmvuthana.TYPE.create(l.getLevel(),EntitySpawnReason.STRUCTURE);if(resident==null)continue;resident.setPos(net.minecraft.world.phys.Vec3.atBottomCenterOf(at));resident.mask(1+random.nextInt(4));resident.setPersistenceRequired();resident.setHomeTo(anchor,25);if(l.noCollision(resident))l.addFreshEntity(resident);}
        }
    }
}
