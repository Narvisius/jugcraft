package io.github.jimbozoomer.jugcraft.menagerie;

import com.google.gson.JsonObject;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.*;

/** Building supplies keep vanilla shapes/placement; incubators hatch native species. */
public final class MenagerieBlocks {
    private MenagerieBlocks(){}
    public static void decorative(String id){
        var key=ResourceKey.create(Registries.BLOCK,Jugcraft.id(id));
        var properties=BlockBehaviour.Properties.of().setId(key).strength(id.contains("feather")?.8F:1.5F).sound(id.contains("feather")?SoundType.WOOL:SoundType.STONE);
        var block=Registry.register(BuiltInRegistries.BLOCK,key,id.contains("feather")?new AllFacing(properties.noOcclusion()):new Block(properties));
        Menagerie.BLOCKS.put(id,block);Menagerie.item(id,p->new BlockItem(block,p.useBlockDescriptionPrefix()));
    }
    public static void register(JsonObject entries){
        entries.entrySet().stream().sorted(java.util.Comparator.comparingInt(e->e.getKey().endsWith("stairs")?1:0)).forEach(e->{
            String id=e.getKey(),name=e.getValue().getAsJsonObject().get("source").getAsString();
            var key=ResourceKey.create(Registries.BLOCK,Jugcraft.id(id));
            var props=BlockBehaviour.Properties.of().setId(key).strength(1.5F).mapColor(MapColor.STONE);
            Block b;
            if(name.endsWith("_slab"))b=new SlabBlock(props);
            else if(name.endsWith("_stairs"))b=new StairBlock(Blocks.STONE_BRICKS.defaultBlockState(),props);
            else if(name.endsWith("_wall"))b=new WallBlock(props);
            else if(name.endsWith("_pane"))b=new IronBarsBlock(props.noOcclusion().sound(SoundType.GLASS));
            else if(name.contains("froglass"))b=new TransparentBlock(props.noOcclusion().sound(SoundType.GLASS));
            else if(name.endsWith("_egg"))b=new Egg(name.replace("_egg",""),props.randomTicks().strength(.5F).noOcclusion());
            else if(name.equals("ant_hill"))b=new Nursery("ant",props.randomTicks());
            else if(name.equals("chrysalis"))b=new Chrysalis(props.randomTicks().noOcclusion());
            else if(name.equals("snail_eggs"))b=new Nursery("snail",props.randomTicks().noOcclusion());
            else if(name.equals("snail_shell"))b=new Shell(props.noOcclusion());
            else if(name.equals("plush_bear"))b=new Facing(props.noOcclusion().sound(SoundType.WOOL));
            else if(name.equals("glow_goop"))b=new Goop(props.noOcclusion().noCollision().lightLevel(s->7));
            else if(name.endsWith("starfish"))b=new MultifaceBlock(props.noOcclusion().noCollision());
            else if(name.equals("duckweed")||name.equals("cattail"))b=new LowDecor(props.noOcclusion().noCollision());
            else b=new Block(props);
            Menagerie.BLOCKS.put(id,Registry.register(BuiltInRegistries.BLOCK,key,b));
            Menagerie.item(id,p->name.endsWith("starfish")?new MenagerieItems.CapturedDecoration(b,p.useBlockDescriptionPrefix()):new BlockItem(b,p.useBlockDescriptionPrefix()));
        });
    }
    private static boolean hatch(ServerLevel l,BlockPos p,String species){
        if(l.getEntitiesOfClass(MenagerieAnimal.class,new net.minecraft.world.phys.AABB(p).inflate(12)).size()>=12)return false;
        var type=Menagerie.TYPES.get("wildlife_"+species);if(type==null)return false;
        var a=type.create(l,EntitySpawnReason.BREEDING);if(a==null)return false;a.setBaby(true);a.snapTo(p.getX()+.5,p.getY()+1,p.getZ()+.5,0,0);
        return l.noCollision(a)&&l.addFreshEntity(a);
    }
    private static final class Egg extends TurtleEggBlock {
        private final String species;
        Egg(String species,Properties p){super(p);this.species=species;}
        @Override protected void randomTick(BlockState state,ServerLevel l,BlockPos p,RandomSource random){
            if(random.nextInt(12)!=0)return;int stage=state.getValue(HATCH);
            if(stage<2)l.setBlock(p,state.setValue(HATCH,stage+1),3);
            else if(hatch(l,p,species)){int eggs=state.getValue(EGGS);if(eggs>1)l.setBlock(p,state.setValue(EGGS,eggs-1).setValue(HATCH,0),3);else l.removeBlock(p,false);}
        }
    }
    private static final class Nursery extends Block {
        static final BooleanProperty OPEN=BooleanProperty.create("open"),HAS_QUEEN=BooleanProperty.create("has_queen");private final String species;
        Nursery(String species,Properties p){super(p);this.species=species;registerDefaultState(defaultBlockState().setValue(OPEN,false).setValue(HAS_QUEEN,species.equals("ant")));}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(OPEN,HAS_QUEEN);}
        @Override protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,net.minecraft.world.entity.player.Player player,net.minecraft.world.phys.BlockHitResult hit){
            if(!species.equals("ant"))return net.minecraft.world.InteractionResult.PASS;
            if(player instanceof net.minecraft.server.level.ServerPlayer p){if(!io.github.jimbozoomer.jugcraft.concordance.Authority.mayChange(p.level(),p,pos))return net.minecraft.world.InteractionResult.FAIL;level.setBlock(pos,state.cycle(OPEN),3);}return net.minecraft.world.InteractionResult.SUCCESS;
        }
        @Override protected void randomTick(BlockState s,ServerLevel l,BlockPos p,RandomSource r){if(species.equals("ant")&&!s.getValue(HAS_QUEEN))return;if(r.nextInt(40)==0&&hatch(l,p,species)&&species.equals("snail"))l.removeBlock(p,false);}
    }
    private static final class Chrysalis extends Block {
        static final IntegerProperty AGE=IntegerProperty.create("age",0,3);
        Chrysalis(Properties p){super(p);registerDefaultState(defaultBlockState().setValue(AGE,0).setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.NORTH));}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(AGE,BlockStateProperties.HORIZONTAL_FACING);}
        @Override protected void randomTick(BlockState s,ServerLevel l,BlockPos p,RandomSource r){if(r.nextInt(10)!=0)return;if(s.getValue(AGE)<3)l.setBlock(p,s.cycle(AGE),3);else if(hatch(l,p,"butterfly"))l.removeBlock(p,false);}
    }
    private static final class Shell extends Block {
        static final EnumProperty<DyeColor> COLOR=EnumProperty.create("color",DyeColor.class);
        Shell(Properties p){super(p);}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(COLOR);}
    }
    private static final class Facing extends Block {
        Facing(Properties p){super(p);}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(BlockStateProperties.HORIZONTAL_FACING);}
        @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext c){return defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,c.getHorizontalDirection().getOpposite());}
    }
    private static final class AllFacing extends Block {
        AllFacing(Properties p){super(p);registerDefaultState(defaultBlockState().setValue(BlockStateProperties.FACING,Direction.UP));}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(BlockStateProperties.FACING);}
        @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext c){return defaultBlockState().setValue(BlockStateProperties.FACING,c.getClickedFace());}
    }
    private static class LowDecor extends Block {
        LowDecor(Properties p){super(p);}
        @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return Block.box(1,0,1,15,3,15);}
    }
    private static final class Goop extends LowDecor implements SimpleWaterloggedBlock {
        static final IntegerProperty GOOP=IntegerProperty.create("goop",1,3);
        Goop(Properties p){super(p);registerDefaultState(defaultBlockState().setValue(GOOP,1).setValue(BlockStateProperties.WATERLOGGED,false));}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(GOOP,BlockStateProperties.WATERLOGGED);}
        @Override protected net.minecraft.world.level.material.FluidState getFluidState(BlockState s){return s.getValue(BlockStateProperties.WATERLOGGED)?net.minecraft.world.level.material.Fluids.WATER.getSource(false):super.getFluidState(s);}
    }
}
