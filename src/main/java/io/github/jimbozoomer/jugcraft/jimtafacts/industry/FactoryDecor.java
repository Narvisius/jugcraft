package io.github.jimbozoomer.jugcraft.jimtafacts.industry;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.*;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.*;

/** Original workshop panels and structural frames; decorative blocks never impersonate a working machine. */
public final class FactoryDecor {
    public static final Map<String,Block> BLOCKS=new LinkedHashMap<>();
    public static Block FRAME;
    private FactoryDecor(){}
    public static void register(){
        for(String name:List.of("frame","panel_blueprint","panel_template","panel_paper","panel_leather","panel_warning","panel_laser")){
            var key=ResourceKey.create(Registries.BLOCK,Jugcraft.id("jimta_factory_"+name));
            var properties=BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).setId(key).strength(2F,6F);
            Block block=name.equals("frame")?new Frame(properties.noOcclusion()):new Block(properties);
            Registry.register(BuiltInRegistries.BLOCK,key,block);BLOCKS.put(name,block);
            var item=ResourceKey.create(Registries.ITEM,key.identifier());Registry.register(BuiltInRegistries.ITEM,item,new BlockItem(block,new Item.Properties().setId(item).useBlockDescriptionPrefix()));
        }
        FRAME=BLOCKS.get("frame");
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS).register(out->BLOCKS.values().forEach(out::accept));
    }
    public static final class Frame extends Block {
        public static final Map<Direction,BooleanProperty> CONNECTIONS=new EnumMap<>(Direction.class);
        static {for(var direction:Direction.values())CONNECTIONS.put(direction,BooleanProperty.create(direction.getName()));}
        private static final VoxelShape CORE=Block.box(4,4,4,12,12,12);
        private static final Map<Direction,VoxelShape> ARMS=Map.of(
            Direction.DOWN,Block.box(4,0,4,12,4,12),Direction.UP,Block.box(4,12,4,12,16,12),
            Direction.NORTH,Block.box(4,4,0,12,12,4),Direction.SOUTH,Block.box(4,4,12,12,12,16),
            Direction.WEST,Block.box(0,4,4,4,12,12),Direction.EAST,Block.box(12,4,4,16,12,12));
        private static final VoxelShape[] SHAPES=new VoxelShape[64];
        static {for(int mask=0;mask<SHAPES.length;mask++){var shape=CORE;for(var direction:Direction.values())if((mask&(1<<direction.ordinal()))!=0)shape=Shapes.or(shape,ARMS.get(direction));SHAPES[mask]=shape;}}
        public Frame(Properties properties){super(properties);var state=defaultBlockState();for(var property:CONNECTIONS.values())state=state.setValue(property,false);registerDefaultState(state);}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){CONNECTIONS.values().forEach(builder::add);}
        @Override public BlockState getStateForPlacement(BlockPlaceContext context){
            var state=defaultBlockState();for(var direction:Direction.values())state=state.setValue(CONNECTIONS.get(direction),context.getLevel().getBlockState(context.getClickedPos().relative(direction)).is(this));return state;
        }
        @Override protected BlockState updateShape(BlockState state,LevelReader world,ScheduledTickAccess ticks,BlockPos pos,Direction side,BlockPos neighbor,BlockState other,RandomSource random){return state.setValue(CONNECTIONS.get(side),other.is(this));}
        @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){int mask=0;for(var direction:Direction.values())if(state.getValue(CONNECTIONS.get(direction)))mask|=1<<direction.ordinal();return SHAPES[mask];}
    }
}
