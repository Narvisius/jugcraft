package io.github.jimbozoomer.jugcraft.jimtafacts.network;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.energy.*;
import java.util.*;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.material.*;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.shapes.*;

/** Original quartz fixture/glass/growth designs, using JE and bounded claim-aware growth. */
public final class DigitalFixtures {
    public static BlockEntityType<GrowthNode> GROWTH;
    public static final String[] BLOCKS = {"quartz_fixture", "quartz_glass", "quartz_vibrant_glass", "growth_accelerator"};
    private DigitalFixtures() {}
    public static void register() {
        for (String name : BLOCKS) {
            var key = ResourceKey.create(Registries.BLOCK, Jugcraft.id("jimta_" + name));
            var p = BlockBehaviour.Properties.ofFullCopy(name.contains("glass") ? Blocks.GLASS : Blocks.IRON_BLOCK).noOcclusion().strength(1.5f).setId(key);
            if (name.equals("quartz_fixture") || name.equals("quartz_vibrant_glass")) p.lightLevel(s -> 15);
            Block b = name.equals("quartz_fixture") ? new Fixture(p) : name.equals("growth_accelerator") ? new Growth(p) : new Glass(p);
            Registry.register(BuiltInRegistries.BLOCK, key, b); DigitalStorage.BLOCKS.put(name, b);
            var itemKey = ResourceKey.create(Registries.ITEM, key.identifier());
            Registry.register(BuiltInRegistries.ITEM, itemKey, new BlockItem(b, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));
        }
        GROWTH = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("jimta_growth_accelerator"),
                FabricBlockEntityTypeBuilder.create(GrowthNode::new, DigitalStorage.BLOCKS.get("growth_accelerator")).build());
        EnergyStorage.SIDED.registerForBlockEntity((node, side) -> side == null || side.getAxis() == node.getBlockState().getValue(Growth.FACING).getAxis() ? node.energy : null, GROWTH);
    }
    public static final class Fixture extends Block implements SimpleWaterloggedBlock {
        public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
        public static final BooleanProperty ODD = BooleanProperty.create("odd"), WATERLOGGED = BlockStateProperties.WATERLOGGED;
        public Fixture(Properties p) { super(p); registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP).setValue(ODD, false).setValue(WATERLOGGED, false)); }
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FACING, ODD, WATERLOGGED); }
        @Override public BlockState getStateForPlacement(BlockPlaceContext c) {
            for (Direction look : c.getNearestLookingDirections()) {
                BlockState s = defaultBlockState().setValue(FACING, look.getOpposite()).setValue(ODD, (c.getClickedPos().getX() + c.getClickedPos().getY() + c.getClickedPos().getZ()) % 2 != 0)
                        .setValue(WATERLOGGED, c.getLevel().getFluidState(c.getClickedPos()).getType() == Fluids.WATER);
                if (s.canSurvive(c.getLevel(), c.getClickedPos())) return s;
            }
            return null;
        }
        @Override protected boolean canSurvive(BlockState s, LevelReader level, BlockPos pos) { Direction face = s.getValue(FACING); return Block.canSupportCenter(level, pos.relative(face.getOpposite()), face); }
        @Override protected BlockState updateShape(BlockState s, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction dir, BlockPos neighbor, BlockState other, RandomSource random) {
            if (s.getValue(WATERLOGGED)) ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
            return s.canSurvive(level, pos) ? s : Blocks.AIR.defaultBlockState();
        }
        @Override protected FluidState getFluidState(BlockState s) { return s.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(s); }
        @Override protected VoxelShape getShape(BlockState s, BlockGetter level, BlockPos pos, CollisionContext c) {
            Direction d = s.getValue(FACING); return Block.box(4.8 - 4.8 * d.getStepX(), 4.8 - 4.8 * d.getStepY(), 4.8 - 4.8 * d.getStepZ(), 11.2 - 4.8 * d.getStepX(), 11.2 - 4.8 * d.getStepY(), 11.2 - 4.8 * d.getStepZ());
        }
        @Override public void animateTick(BlockState s, Level level, BlockPos pos, RandomSource random) {
            if (random.nextFloat() < .02f) { var d = s.getValue(FACING); level.addParticle(ParticleTypes.ELECTRIC_SPARK, pos.getX() + .5 - .3 * d.getStepX(), pos.getY() + .5 - .3 * d.getStepY(), pos.getZ() + .5 - .3 * d.getStepZ(), 0, 0, 0); }
        }
    }
    public static final class Glass extends TransparentBlock {
        public static final Map<Direction, BooleanProperty> CONNECTIONS = Map.of(Direction.DOWN, BooleanProperty.create("down"), Direction.UP, BooleanProperty.create("up"), Direction.NORTH, BooleanProperty.create("north"), Direction.SOUTH, BooleanProperty.create("south"), Direction.WEST, BooleanProperty.create("west"), Direction.EAST, BooleanProperty.create("east"));
        public Glass(Properties p) { super(p); var s = stateDefinition.any(); for (var property : CONNECTIONS.values()) s = s.setValue(property, false); registerDefaultState(s); }
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { CONNECTIONS.values().forEach(b::add); }
        @Override public BlockState getStateForPlacement(BlockPlaceContext c) { var s = defaultBlockState(); for (Direction d : Direction.values()) s = s.setValue(CONNECTIONS.get(d), c.getLevel().getBlockState(c.getClickedPos().relative(d)).getBlock() instanceof Glass); return s; }
        @Override protected BlockState updateShape(BlockState s, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction dir, BlockPos neighbor, BlockState other, RandomSource random) { return s.setValue(CONNECTIONS.get(dir), other.getBlock() instanceof Glass); }
        @Override protected boolean skipRendering(BlockState s, BlockState other, Direction dir) { return other.getBlock() instanceof Glass || super.skipRendering(s, other, dir); }
    }
    public static final class Growth extends Block implements EntityBlock, EnergyConnectable {
        public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
        public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
        public Growth(Properties p) { super(p); registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP).setValue(POWERED, false)); }
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FACING, POWERED); }
        @Override public BlockState getStateForPlacement(BlockPlaceContext c) { return defaultBlockState().setValue(FACING, c.getNearestLookingDirection().getOpposite()); }
        @Override public BlockEntity newBlockEntity(BlockPos p, BlockState s) { return new GrowthNode(p, s); }
        @Override public void setPlacedBy(Level l, BlockPos p, BlockState s, LivingEntity actor, ItemStack stack) { if (actor instanceof ServerPlayer player && l.getBlockEntity(p) instanceof GrowthNode n) { n.owner = player.getUUID(); n.setChanged(); } }
        @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l, BlockState s, BlockEntityType<T> t) { return !l.isClientSide() && t == GROWTH ? (level, pos, state, be) -> ((GrowthNode) be).tick() : null; }
    }
    public static final class GrowthNode extends BlockEntity {
        private UUID owner;
        public final SimpleEnergyStorage energy = new SimpleEnergyStorage(1600, 1600, 0, this::setChanged);
        public GrowthNode(BlockPos p, BlockState s) { super(GROWTH, p, s); }
        public void owner(UUID value) { owner = value; setChanged(); }
        public void tick() {
            if (!(level instanceof ServerLevel server) || Math.floorMod(server.getGameTime() + worldPosition.asLong(), 20) != 0) return;
            ServerPlayer actor = Authority.answering(server, owner);
            boolean on = actor != null && actor.level() == level && actor.isAlive() && !actor.isSpectator() && Authority.mayChange(server, actor, worldPosition) && energy.getAmount() >= 160 && !server.hasNeighborSignal(worldPosition);
            if (getBlockState().getValue(Growth.POWERED) != on) server.setBlock(worldPosition, getBlockState().setValue(Growth.POWERED, on), Block.UPDATE_CLIENTS);
            if (!on) return;
            energy.setAmount(energy.getAmount() - 160); setChanged();
            // At most six loaded budding blocks and one candidate face per block, once per second.
            for (Direction side : Direction.values()) grow(server, actor, worldPosition.relative(side), server.getRandom());
        }
        public static boolean grow(ServerLevel level, ServerPlayer actor, BlockPos pos, RandomSource random) {
            if (!level.isLoaded(pos) || !Authority.mayChange(level, actor, pos)) return false;
            BlockState source = level.getBlockState(pos); boolean certus = source.getBlock() instanceof DigitalMaterials.Budding;
            if (!certus && !source.is(Blocks.BUDDING_AMETHYST) || random.nextInt(5) != 0) return false;
            Direction direction = Direction.getRandom(random); BlockPos target = pos.relative(direction);
            if (!level.isLoaded(target) || !Authority.mayChange(level, actor, target)) return false;
            Block[] stages = certus ? new Block[]{DigitalStorage.BLOCKS.get("small_quartz_bud"), DigitalStorage.BLOCKS.get("medium_quartz_bud"), DigitalStorage.BLOCKS.get("large_quartz_bud"), DigitalStorage.BLOCKS.get("quartz_cluster")}
                    : new Block[]{Blocks.SMALL_AMETHYST_BUD, Blocks.MEDIUM_AMETHYST_BUD, Blocks.LARGE_AMETHYST_BUD, Blocks.AMETHYST_CLUSTER};
            BlockState old = level.getBlockState(target); Block next = BuddingAmethystBlock.canClusterGrowAtState(old) ? stages[0] : null;
            if (old.hasProperty(AmethystClusterBlock.FACING) && old.getValue(AmethystClusterBlock.FACING) == direction)
                for (int i = 0; i < 3; i++) if (old.is(stages[i])) next = stages[i + 1];
            if (next == null || !level.setBlockAndUpdate(target, next.defaultBlockState().setValue(AmethystClusterBlock.FACING, direction).setValue(AmethystClusterBlock.WATERLOGGED, old.getFluidState().getType() == Fluids.WATER))) return false;
            if (certus && next == stages[3] && random.nextInt(12) == 0) {
                String grade = source.is(DigitalStorage.BLOCKS.get("flawed_budding_quartz")) ? "chipped_budding_quartz" : source.is(DigitalStorage.BLOCKS.get("chipped_budding_quartz")) ? "damaged_budding_quartz" : source.is(DigitalStorage.BLOCKS.get("damaged_budding_quartz")) ? "quartz_block" : null;
                if (grade != null) level.setBlockAndUpdate(pos, DigitalStorage.BLOCKS.get(grade).defaultBlockState());
            }
            return true;
        }
        @Override protected void loadAdditional(ValueInput in) { super.loadAdditional(in); owner = in.read("owner", UUIDUtil.CODEC).orElse(null); energy.setAmount(in.getLongOr("energy", 0)); }
        @Override protected void saveAdditional(ValueOutput out) { super.saveAdditional(out); if (owner != null) out.store("owner", UUIDUtil.CODEC, owner); out.putLong("energy", energy.getAmount()); }
    }
}
