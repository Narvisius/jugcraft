package io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.mixin.StructureTemplatePoolAccessor;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.*;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.pools.*;
import net.minecraft.world.level.levelgen.structure.templatesystem.*;

/** Original layout processors on 26.3. No retrogen or live-world modification pass. */
public final class JimtaStructures {
    public static StructurePoolElementType<TerrainElement> TERRAIN;
    private static final Set<StructureTemplatePool> EXTENDED=Collections.newSetFromMap(new WeakHashMap<>());
    private JimtaStructures(){}
    public static void register(){
        TERRAIN=Registry.register(BuiltInRegistries.STRUCTURE_POOL_ELEMENT,Jugcraft.id("jimtafacts_individual_terrain"),()->TerrainElement.CODEC);
        Registry.register(BuiltInRegistries.STRUCTURE_PROCESSOR,Jugcraft.id("jimtafacts_foundation"),Foundation.CODEC);
        Registry.register(BuiltInRegistries.STRUCTURE_PROCESSOR,Jugcraft.id("jimtafacts_handle_lit"),Lit.CODEC);
        Registry.register(BuiltInRegistries.STRUCTURE_PROCESSOR,Jugcraft.id("jimtafacts_weather_copper"),Copper.CODEC);
        Registry.register(BuiltInRegistries.STRUCTURE_PROCESSOR,Jugcraft.id("jimtafacts_clear_vault"),FreshVault.CODEC);
        Registry.register(BuiltInRegistries.STRUCTURE_PROCESSOR,Jugcraft.id("jimtafacts_randomize_statue"),StatuePose.CODEC);
        ServerLifecycleEvents.SERVER_STARTING.register(server->{
            if(!JimtaSpellbooks.enabled())return;
            var pools=server.registryAccess().lookupOrThrow(Registries.TEMPLATE_POOL);
            var processors=server.registryAccess().lookupOrThrow(Registries.PROCESSOR_LIST).getOrThrow(ResourceKey.create(Registries.PROCESSOR_LIST,Identifier.withDefaultNamespace("mossify_70_percent")));
            for(var biome:List.of("plains","taiga")){
                var pool=pools.getValue(ResourceKey.create(Registries.TEMPLATE_POOL,Identifier.withDefaultNamespace("village/"+biome+"/houses")));
                if(pool==null||!EXTENDED.add(pool))continue;
                var name="jimtafacts/spellbooks/priest_house"+(biome.equals("taiga")?"_taiga":"");
                var element=StructurePoolElement.legacy(Jugcraft.id(name).toString(),processors).apply(StructureTemplatePool.Projection.RIGID);
                ((StructureTemplatePoolAccessor)pool).jugcraft$templates().add(element);
            }
        });
    }
    public static final class TerrainElement extends SinglePoolElement {
        public static final MapCodec<TerrainElement> CODEC=RecordCodecBuilder.mapCodec(i->i.group(
            TerrainElement.<TerrainElement>templateCodec(),TerrainElement.<TerrainElement>processorsCodec(),
            TerrainElement.<TerrainElement>projectionCodec(),TerrainElement.<TerrainElement>overrideLiquidSettingsCodec(),
            TerrainAdjustment.CODEC.optionalFieldOf("terrain_adjustment",TerrainAdjustment.NONE).forGetter(e->e.adjustment)
        ).apply(i,TerrainElement::new));
        private final TerrainAdjustment adjustment;
        public TerrainElement(Either<Identifier,StructureTemplate> template,Holder<StructureProcessorList> processors,StructureTemplatePool.Projection projection,Optional<LiquidSettings> liquid,TerrainAdjustment adjustment){super(template,processors,projection,liquid);this.adjustment=adjustment;}
        public TerrainAdjustment adjustment(){return adjustment;}
        @Override public StructurePoolElementType<?> getType(){return TERRAIN;}
    }
    public record Foundation(BlockState block) implements StructureProcessor {
        public static final MapCodec<Foundation> CODEC=BlockState.CODEC.fieldOf("block").xmap(Foundation::new,Foundation::block);
        @Override public MapCodec<? extends StructureProcessor> codec(){return CODEC;}
        @Override public StructureTemplate.StructureBlockInfo processBlock(LevelReader level,BlockPos origin,BlockPos center,BlockPos local,StructureTemplate.StructureBlockInfo info,StructurePlaceSettings settings){
            // Only the currently generating chunk; never load an adjacent chunk or alter an existing saved world.
            if(!(level instanceof WorldGenRegion region)||local.getY()!=0||info.state().isAir()||!region.getCenter().equals(new ChunkPos(info.pos().getX()>>4,info.pos().getZ()>>4)))return info;
            var pos=info.pos().mutable().move(Direction.DOWN);int budget=level.getHeight();
            while(budget-->0&&pos.getY()>level.getMinY()&&pos.getY()<=level.getMaxY()){
                var current=level.getBlockState(pos);if(!current.isAir()&&current.getFluidState().isEmpty())break;
                level.getChunk(pos).setBlockState(pos,block,0);pos.move(Direction.DOWN);
            }return info;
        }
    }
    public record Lit(double chanceLit,Map<Identifier,Double> byBlock) implements StructureProcessor {
        public static final MapCodec<Lit> CODEC=RecordCodecBuilder.mapCodec(i->i.group(Codec.doubleRange(0,1).fieldOf("chanceLit").forGetter(Lit::chanceLit),Codec.unboundedMap(Identifier.CODEC,Codec.doubleRange(0,1)).optionalFieldOf("byBlock",Map.of()).forGetter(Lit::byBlock)).apply(i,Lit::new));
        @Override public MapCodec<? extends StructureProcessor> codec(){return CODEC;}
        @Override public StructureTemplate.StructureBlockInfo processBlock(LevelReader level,BlockPos origin,BlockPos center,BlockPos local,StructureTemplate.StructureBlockInfo info,StructurePlaceSettings settings){
            if(!info.state().hasProperty(BlockStateProperties.LIT))return info;
            var chance=byBlock.getOrDefault(BuiltInRegistries.BLOCK.getKey(info.state().getBlock()),chanceLit);
            return new StructureTemplate.StructureBlockInfo(info.pos(),info.state().setValue(BlockStateProperties.LIT,settings.getRandom(info.pos()).nextFloat()<chance),info.nbt());
        }
    }
    public record Copper(float bias) implements StructureProcessor {
        public static final MapCodec<Copper> CODEC=Codec.floatRange(0,1).fieldOf("bias").xmap(Copper::new,Copper::bias);
        @Override public MapCodec<? extends StructureProcessor> codec(){return CODEC;}
        @Override public StructureTemplate.StructureBlockInfo processBlock(LevelReader level,BlockPos origin,BlockPos center,BlockPos local,StructureTemplate.StructureBlockInfo info,StructurePlaceSettings settings){
            var state=info.state();if(!(state.getBlock() instanceof WeatheringCopper copper))return info;
            int stages=(int)((bias+(1-bias)*settings.getRandom(info.pos()).nextFloat())*4);
            for(int i=0;i<stages;i++){var next=copper.getNext(state);if(next.isPresent())state=next.get().getBlock().withPropertiesOf(state);}
            return new StructureTemplate.StructureBlockInfo(info.pos(),state,info.nbt());
        }
    }
    public record StatuePose() implements StructureProcessor {
        public static final MapCodec<StatuePose> CODEC=MapCodec.unit(StatuePose::new);
        @Override public MapCodec<? extends StructureProcessor> codec(){return CODEC;}
        @Override public StructureTemplate.StructureBlockInfo processBlock(LevelReader level,BlockPos origin,BlockPos center,BlockPos local,StructureTemplate.StructureBlockInfo info,StructurePlaceSettings settings){
            if(!info.state().is(JimtaStatues.BLOCK)||info.state().getValue(JimtaStatues.Y)!=0)return info;
            var tag=info.nbt()==null?new net.minecraft.nbt.CompoundTag():info.nbt().copy();
            tag.putString("id",Jugcraft.id(JimtaStatues.ID).toString());
            tag.putInt("pose",settings.getRandom(info.pos()).nextInt(JimtaStatues.POSES.size()));
            return new StructureTemplate.StructureBlockInfo(info.pos(),info.state(),tag);
        }
    }
    public record FreshVault() implements StructureProcessor {
        public static final MapCodec<FreshVault> CODEC=MapCodec.unit(FreshVault::new);
        @Override public MapCodec<? extends StructureProcessor> codec(){return CODEC;}
        @Override public StructureTemplate.StructureBlockInfo processBlock(LevelReader level,BlockPos origin,BlockPos center,BlockPos local,StructureTemplate.StructureBlockInfo info,StructurePlaceSettings settings){
            if(info.nbt()==null)return info;
            var id=BuiltInRegistries.BLOCK.getKey(info.state().getBlock());if(!(info.state().getBlock() instanceof VaultBlock)&&!(id.getNamespace().equals("jugcraft")&&id.getPath().endsWith("_vault")))return info;
            var data=info.nbt().copy();data.remove("server_data");data.remove("shared_data");data.remove("rewarded");data.remove("opened");
            return new StructureTemplate.StructureBlockInfo(info.pos(),info.state(),data);
        }
    }
}
