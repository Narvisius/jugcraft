package io.github.jimbozoomer.jugcraft.menagerie;
import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.pools.*;
import net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasLookup;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;

/** Supplied galleon connector graph at sea level; native generation never rewrites old chunks. */
public final class OwnerGalleon extends Structure {
 public static final MapCodec<OwnerGalleon> CODEC=simpleCodec(OwnerGalleon::new);public static final StructureType<OwnerGalleon> TYPE=()->CODEC;
 public OwnerGalleon(StructureSettings s){super(s);}
 static void register(){Registry.register(BuiltInRegistries.STRUCTURE_PROCESSOR,Jugcraft.id("supps_galleon_data"),Data.CODEC);Registry.register(BuiltInRegistries.STRUCTURE_TYPE,Jugcraft.id("supps_galleon"),TYPE);Registry.register(BuiltInRegistries.FEATURE_TYPE,Jugcraft.id("supps_galleon_crew"),Crew.CODEC);}
 @Override public StructureType<?> type(){return TYPE;}
 @Override protected Optional<GenerationStub> findGenerationPoint(GenerationContext c){int x=c.chunkPos().getMiddleBlockX(),z=c.chunkPos().getMiddleBlockZ(),sea=c.chunkGenerator().getSeaLevel();
  // Do not put the supplied deep hull on shore or dry terrain. Eight sample points bound work.
  for(int dx:new int[]{-24,0,24})for(int dz:new int[]{-24,0,24}){int floor=c.chunkGenerator().getBaseHeight(x+dx,z+dz,Heightmap.Types.OCEAN_FLOOR_WG,c.heightAccessor(),c.randomState()),top=c.chunkGenerator().getBaseHeight(x+dx,z+dz,Heightmap.Types.WORLD_SURFACE_WG,c.heightAccessor(),c.randomState());if(floor>sea-10||top<sea-1||top>sea+1)return Optional.empty();}
  var pool=c.registryAccess().lookupOrThrow(Registries.TEMPLATE_POOL).getOrThrow(ResourceKey.create(Registries.TEMPLATE_POOL,Jugcraft.id("supps_galleon/start_pool")));
  return JigsawPlacement.addPieces(c,pool,Optional.empty(),7,new BlockPos(x,sea-3,z),false,Optional.empty(),new JigsawStructure.MaxDistance(80),PoolAliasLookup.EMPTY,DimensionPadding.ZERO,LiquidSettings.IGNORE_WATERLOGGING);
 }
 public static final class Data implements net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor {
  static final Data INSTANCE=new Data();static final MapCodec<Data> CODEC=MapCodec.unit(INSTANCE);
  @Override public MapCodec<? extends net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor> codec(){return CODEC;}
  @Override public net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo processBlock(LevelReader l,BlockPos origin,BlockPos pivot,BlockPos local,net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo info,net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings settings){
   if(info.nbt()==null)return info;var tag=info.nbt().copy();if(tag.contains("CannonYaw")){var vector=OwnerCannon.direction(tag.getFloatOr("CannonYaw",0),0);var point=new BlockPos((int)Math.round(vector.x*100),0,(int)Math.round(vector.z*100)).rotate(settings.getRotation());tag.putFloat("CannonYaw",(float)Math.toDegrees(Math.atan2(-point.getX(),point.getZ())));}
   if(tag.contains("LootTable"))tag.putLong("LootTableSeed",settings.getRandom(info.pos()).nextLong());return new net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo(info.pos(),info.state(),tag);
  }
 }
 public record Crew(String entity,List<String> passengers,int spread,int attempts) implements Feature {
  public static final MapCodec<Crew> CODEC=RecordCodecBuilder.mapCodec(i->i.group(Codec.STRING.fieldOf("entity").forGetter(Crew::entity),Codec.STRING.listOf(0,2).optionalFieldOf("passengers",List.of()).forGetter(Crew::passengers),Codec.intRange(0,4).optionalFieldOf("spread",0).forGetter(Crew::spread),Codec.intRange(1,16).optionalFieldOf("attempts",1).forGetter(Crew::attempts)).apply(i,Crew::new));
  public Crew{passengers=List.copyOf(passengers);}
  @Override public MapCodec<? extends Feature> codec(){return CODEC;}
  @Override public boolean place(WorldGenLevel l,ChunkGenerator g,RandomSource random,BlockPos origin){
   if(entity.equals("urn")){boolean placed=false;for(int n=0;n<attempts;n++){var at=origin.offset(random.nextInt(spread*2+1)-spread,0,random.nextInt(spread*2+1)-spread);if(!l.ensureCanWrite(at)||!l.getBlockState(at).isAir()||!l.getBlockState(at.below()).isFaceSturdy(l,at.below(),Direction.UP))continue;var state=HouseholdBlock.set(OwnerSupplementaries.BLOCKS.get("urn").defaultBlockState(),"treasure","true");if(l.setBlock(at,state,2)&&l.getBlockEntity(at) instanceof OwnerSupplementData urn){urn.setLootTable(ResourceKey.create(Registries.LOOT_TABLE,Jugcraft.id("supps_loot/urn_loot/urn_loot")));urn.setChanged();placed=true;}}return placed;}
   var type=BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse(entity));if(type==null)return false;
   for(int attempt=0;attempt<attempts;attempt++){var at=origin.offset(random.nextInt(spread*2+1)-spread,0,random.nextInt(spread*2+1)-spread);if(!l.ensureCanWrite(at))continue;
    var root=type.create(l.getLevel(),EntitySpawnReason.STRUCTURE);if(root==null)return false;
    if(root instanceof AbstractBoat){at=new BlockPos(at.getX(),g.getSeaLevel(),at.getZ());if(!l.ensureCanWrite(at)||!l.getFluidState(at.below()).is(net.minecraft.tags.FluidTags.WATER))continue;}
    root.setPos(at.getX()+.5,at.getY(),at.getZ()+.5);root.setYRot(random.nextFloat()*360);root.addTag("JugcraftGalleon");if(!l.noCollision(root))continue;
    if(root instanceof Mob mob){mob.finalizeSpawn(l,l.getCurrentDifficultyAt(at),EntitySpawnReason.STRUCTURE,null);mob.setPersistenceRequired();}
    if(root instanceof OwnerCannonBoat boat){boat.setItem(0,new ItemStack(Items.GUNPOWDER,5+random.nextInt(16)));boat.setItem(1,new ItemStack(OwnerSupplementaries.ITEMS.get("cannonball"),7+random.nextInt(10)));}
    for(String passenger:passengers){var passengerType=BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse(passenger));if(passengerType==null)continue;var rider=passengerType.create(l.getLevel(),EntitySpawnReason.STRUCTURE);if(rider==null)continue;rider.setPos(root.position());rider.addTag("JugcraftGalleon");if(rider instanceof Mob mob){mob.finalizeSpawn(l,l.getCurrentDifficultyAt(at),EntitySpawnReason.STRUCTURE,null);mob.setPersistenceRequired();}if(!rider.startRiding(root,true,true))rider.discard();}
    l.addFreshEntityWithPassengers(root);return true;
   }return false;
  }
 }
}
