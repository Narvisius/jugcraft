package io.github.jimbozoomer.jugcraft.jimtafacts.industry;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.energy.CableBlock;
import io.github.jimbozoomer.jugcraft.fluid.FluidPipeBlock;
import io.github.jimbozoomer.jugcraft.fluid.*;
import io.github.jimbozoomer.jugcraft.logistics.*;
import java.util.*;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityType;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

/** Original pipe skins on shared native logistics. No separate MJ/RF inventory or hidden item simulation. */
public final class IndustrialTransport {
    public static final Map<String, Block> BLOCKS = new LinkedHashMap<>();
    public static final List<String> MATERIALS = List.of("cobblestone", "stone", "andesite", "sandstone", "quartz", "gold");
    private IndustrialTransport() {}
    public static void register() {
        for (String material : MATERIALS) {
            put(material + "_item", new RoutePipe(properties(material + "_item"), material));
            put(material + "_fluid", new RouteFluid(properties(material + "_fluid"), material, switch (material) {
                case "gold" -> 2000; case "quartz" -> 1000; case "stone", "andesite" -> 500; default -> 250;
            }));
            if (!material.equals("andesite")) put(material + "_power", new CableBlock(properties(material + "_power"), switch (material) {
                case "gold" -> 4096; case "quartz" -> 2048; case "stone" -> 1024; default -> 256;
            }));
        }
        put("iron_item", new RoutePipe(properties("iron_item"), "iron"));
        put("iron_fluid", new RouteFluid(properties("iron_fluid"),"iron",1000));
        var fluidSorter=new FluidFilterBlock(properties("diamond_fluid"),2000);
        put("diamond_fluid",fluidSorter);
        ((FabricBlockEntityType)JugcraftFluids.FILTER_ENTITY).addValidBlock(fluidSorter);
        put("wood_fluid",new FluidExtractor(properties("wood_fluid")));
        put("wood_item", new PneumaticExtractorBlock(properties("wood_item"), 8, 16));
        var sorter = new ItemSorterBlock(properties("diamond_item"));
        put("diamond_item", sorter);
        ((FabricBlockEntityType) JugcraftLogistics.SORTER_ENTITY).addValidBlock(sorter);
        put("clay_item",new RoutePipe(properties("clay_item"),"clay"));
        put("clay_fluid",new RouteFluid(properties("clay_fluid"),"clay",500));
        IndustrialTransportSpecial.register();
        IndustrialTransportDevices.register();
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(out -> BLOCKS.values().forEach(out::accept));
    }
    static BlockBehaviour.Properties properties(String suffix) {
        return BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(1.5F).noOcclusion()
                .setId(ResourceKey.create(Registries.BLOCK, Jugcraft.id("jimta_pipe_" + suffix)));
    }
    static void put(String suffix, Block block) {
        String id = "jimta_pipe_" + suffix;
        Registry.register(BuiltInRegistries.BLOCK, Jugcraft.id(id), block);
        var key = ResourceKey.create(Registries.ITEM, Jugcraft.id(id));
        Registry.register(BuiltInRegistries.ITEM, key, new BlockItem(block, new Item.Properties().setId(key).useBlockDescriptionPrefix()));
        BLOCKS.put(id, block);
    }
    public static final class RoutePipe extends ItemPipeBlock {
        public static final EnumProperty<Direction> OUTLET = DirectionalBlock.FACING;
        final String material;
        RoutePipe(Properties properties, String material) {
            super(properties); this.material = material; registerDefaultState(defaultBlockState().setValue(OUTLET, Direction.NORTH));
        }
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { super.createBlockStateDefinition(b); b.add(OUTLET); }
        @Override public BlockState getStateForPlacement(BlockPlaceContext c) { return super.getStateForPlacement(c).setValue(OUTLET, c.getNearestLookingDirection().getOpposite()); }
        @Override public long transferLimit() { return switch (material) { case "gold" -> 64; case "quartz", "iron" -> 32; case "stone", "andesite" -> 16; default -> 8; }; }
        @Override public boolean directionalRouting() {return material.equals("iron");}
        @Override public boolean prefersAdjacentInventory(){return material.equals("clay");}
        @Override public boolean joins(BlockState own, BlockState other) {
            if (!(other.getBlock() instanceof RoutePipe pipe)) return true;
            return !(material.equals("cobblestone") && pipe.material.equals("stone") || material.equals("stone") && pipe.material.equals("cobblestone"));
        }
        @Override public boolean routesTo(BlockState own, Direction direction, BlockState other) {
            return (!material.equals("iron") || direction == own.getValue(OUTLET))
                    && (!material.equals("sandstone") || other.getBlock() instanceof ItemPipeBlock);
        }
        @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (!material.equals("iron")) return InteractionResult.PASS;
            if (!level.isClientSide()) {
                if (!(level instanceof net.minecraft.server.level.ServerLevel server) || !Authority.mayChange(server, player, pos)) return InteractionResult.FAIL;
                Direction next = Direction.values()[(state.getValue(OUTLET).ordinal() + 1) % 6];
                level.setBlock(pos, state.setValue(OUTLET, next), Block.UPDATE_ALL); ItemNetworks.invalidate(level);
                player.sendOverlayMessage(Component.literal("Item outlet: " + next.getName()));
            }
            return InteractionResult.SUCCESS;
        }
    }
    public static final class RouteFluid extends FluidPipeBlock {
        final String material;
        RouteFluid(Properties p,String material,long rate) { super(p,rate);this.material=material;registerDefaultState(defaultBlockState().setValue(DirectionalBlock.FACING,Direction.NORTH)); }
        @Override public boolean prefersAdjacentInventory(){return material.equals("clay");}
        @Override public boolean directionalRouting() {return material.equals("iron");}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b) { super.createBlockStateDefinition(b);b.add(DirectionalBlock.FACING); }
        @Override public BlockState getStateForPlacement(BlockPlaceContext c) { return super.getStateForPlacement(c).setValue(DirectionalBlock.FACING,c.getNearestLookingDirection().getOpposite()); }
        @Override public boolean joins(BlockState own,BlockState other) {
            if(!(other.getBlock() instanceof RouteFluid pipe)) return true;
            return !(material.equals("cobblestone")&&pipe.material.equals("stone")||material.equals("stone")&&pipe.material.equals("cobblestone"));
        }
        @Override public boolean routesTo(BlockState own,Direction direction,BlockState other) {
            return (!material.equals("iron")||direction==own.getValue(DirectionalBlock.FACING))&&(!material.equals("sandstone")||other.getBlock() instanceof FluidPipeBlock);
        }
        @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit) {
            if(!material.equals("iron")) return InteractionResult.PASS;
            if(level instanceof net.minecraft.server.level.ServerLevel server) {
                if(!Authority.mayChange(server,player,pos)) return InteractionResult.FAIL;
                Direction next=Direction.values()[(state.getValue(DirectionalBlock.FACING).ordinal()+1)%6];
                level.setBlock(pos,state.setValue(DirectionalBlock.FACING,next),Block.UPDATE_ALL);FluidNetworks.invalidate(level);
                player.sendOverlayMessage(Component.literal("Fluid outlet: "+next.getName()));
            }
            return InteractionResult.SUCCESS;
        }
    }
    /** Pulls only from a real adjacent tank; never generates fluid or takes world source blocks. */
    public static final class FluidExtractor extends Block implements FluidConnectable {
        FluidExtractor(Properties p) { super(p);registerDefaultState(defaultBlockState().setValue(DirectionalBlock.FACING,Direction.NORTH)); }
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b) { b.add(DirectionalBlock.FACING); }
        @Override public BlockState getStateForPlacement(BlockPlaceContext c) { return defaultBlockState().setValue(DirectionalBlock.FACING,c.getClickedFace().getOpposite()); }
        @Override protected void onPlace(BlockState state,Level level,BlockPos pos,BlockState old,boolean moved) { if(!level.isClientSide())level.scheduleTick(pos,this,8);FluidNetworks.invalidate(level); }
        @Override protected void tick(BlockState state,net.minecraft.server.level.ServerLevel level,BlockPos pos,net.minecraft.util.RandomSource random) {
            level.scheduleTick(pos,this,8);if(level.hasNeighborSignal(pos))return;
            Direction intake=state.getValue(DirectionalBlock.FACING);BlockPos sourcePos=pos.relative(intake);
            if(!level.hasChunkAt(sourcePos)||!io.github.jimbozoomer.jugcraft.finance.ClaimTransfers.allowed(level,pos,sourcePos))return;
            var source=net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage.SIDED.find(level,sourcePos,intake.getOpposite());
            if(source==null)return;
            FluidNetworks.pushToNeighbors(level,pos,source,250*FluidNetworks.DROPLETS_PER_MB,Arrays.stream(Direction.values()).filter(d->d!=intake).toList(),Set.of(sourcePos));
        }
    }
}
