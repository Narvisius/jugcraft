package io.github.jimbozoomer.jugcraft.building;

import com.google.gson.JsonObject;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.material.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;

/** Supplied bridge shapes with native, bounded placement and connection rules. */
final class OwnerBridgeBlocks {
    private OwnerBridgeBlocks() { }
    enum Connection implements StringRepresentable {
        BASE, MIDDLE_NS, MIDDLE_EW, CORNER_NE, CORNER_NW, CORNER_SE, CORNER_SW, SIDE_N, SIDE_E, SIDE_S, SIDE_W,
        BASE_TOGGLED, MIDDLE_END_N, MIDDLE_END_E, MIDDLE_END_S, MIDDLE_END_W,
        END_N_LEFT, END_N_RIGHT, END_E_LEFT, END_E_RIGHT, END_S_LEFT, END_S_RIGHT, END_W_LEFT, END_W_RIGHT;
        @Override public String getSerializedName() { return name().toLowerCase(Locale.ROOT); }
    }
    enum StairConnection implements StringRepresentable {
        BASE, DOUBLE, LEFT, RIGHT;
        @Override public String getSerializedName() { return name().toLowerCase(Locale.ROOT); }
    }
    enum LightState implements StringRepresentable {
        BRIDGE, STAIR;
        @Override public String getSerializedName() { return name().toLowerCase(Locale.ROOT); }
    }
    static final EnumProperty<Connection> CONNECTION = EnumProperty.create("connection", Connection.class,
        Arrays.copyOf(Connection.values(), 11));
    static final EnumProperty<Connection> ROPE_CONNECTION = EnumProperty.create("connection", Connection.class);
    static final EnumProperty<Direction> AXIS_FACING = EnumProperty.create("facing", Direction.class, Direction.NORTH, Direction.EAST);
    static final BooleanProperty MANUAL = BooleanProperty.create("manual");

    private static Connection transform(Connection value,java.util.function.UnaryOperator<Direction> direction,boolean reflected){
        String name=value.name();
        if(name.equals("BASE")||name.equals("BASE_TOGGLED"))return value;
        if(name.equals("MIDDLE_NS")||name.equals("MIDDLE_EW")){
            Direction axis=name.endsWith("NS")?Direction.NORTH:Direction.EAST;
            return direction.apply(axis).getAxis()==Direction.Axis.Z?Connection.MIDDLE_NS:Connection.MIDDLE_EW;
        }
        if(name.startsWith("CORNER_")){
            var a=direction.apply(compass(name.charAt(7)));var b=direction.apply(compass(name.charAt(8)));
            String ns=a.getAxis()==Direction.Axis.Z?letter(a):letter(b),ew=a.getAxis()==Direction.Axis.X?letter(a):letter(b);
            return Connection.valueOf("CORNER_"+ns+ew);
        }
        String[] tokens=name.split("_");
        for(int i=0;i<tokens.length;i++){
            if(tokens[i].length()==1&&"NESW".contains(tokens[i]))tokens[i]=letter(direction.apply(compass(tokens[i].charAt(0))));
            else if(reflected&&tokens[i].equals("LEFT"))tokens[i]="RIGHT";
            else if(reflected&&tokens[i].equals("RIGHT"))tokens[i]="LEFT";
        }
        return Connection.valueOf(String.join("_",tokens));
    }
    private static Direction compass(char c){return switch(c){case 'N'->Direction.NORTH;case 'E'->Direction.EAST;case 'S'->Direction.SOUTH;default->Direction.WEST;};}
    private static String letter(Direction d){return d.getName().substring(0,1).toUpperCase(Locale.ROOT);}

    static InteractionResult extend(ItemStack stack, Level level, BlockPos target, Player player, InteractionHand hand) {
        if (!(stack.getItem() instanceof BlockItem item)) return InteractionResult.PASS;
        if (level instanceof ServerLevel server) {
            if (!level.hasChunkAt(target) || !Authority.mayChange(server, player, target) || !level.getBlockState(target).isAir())
                return InteractionResult.FAIL;
            return item.place(new BlockPlaceContext(player, hand, stack, new BlockHitResult(Vec3.atCenterOf(target), Direction.UP, target, false)));
        }
        return InteractionResult.SUCCESS;
    }
    static boolean pliers(ItemStack stack) {
        return stack.is(Items.SHEARS) || stack.is(OwnerBuildingCollection.ITEMS.get("bridgeworks_pliers"));
    }

    static class Deck extends Block {
        private final OwnerAuthoredShapes shapes;
        Deck(Properties properties, JsonObject descriptor) {
            super(properties);
            registerDefaultState(defaultBlockState().setValue(connection(), Connection.MIDDLE_NS).setValue(MANUAL, false));
            shapes = new OwnerAuthoredShapes(this, descriptor.getAsJsonObject("shapes"));
        }
        EnumProperty<Connection> connection() { return CONNECTION; }
        @Override protected BlockState rotate(BlockState state,Rotation rotation){
            state=state.setValue(connection(),transform(state.getValue(connection()),rotation::rotate,false));
            if(state.hasProperty(AXIS_FACING))state=state.setValue(AXIS_FACING,rotation.rotate(state.getValue(AXIS_FACING)).getAxis()==Direction.Axis.X?Direction.EAST:Direction.NORTH);
            return state;
        }
        @Override protected BlockState mirror(BlockState state,Mirror mirror){return state.setValue(connection(),transform(state.getValue(connection()),mirror::mirror,mirror!=Mirror.NONE));}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(connection(), MANUAL);
        }
        private BlockState connected(BlockState state, BlockGetter level, BlockPos pos) {
            boolean n = level.getBlockState(pos.north()).is(this), e = level.getBlockState(pos.east()).is(this);
            boolean s = level.getBlockState(pos.south()).is(this), w = level.getBlockState(pos.west()).is(this);
            int mask = (n ? 1 : 0) | (e ? 2 : 0) | (s ? 4 : 0) | (w ? 8 : 0);
            Connection result = switch (mask) {
                case 0, 10 -> Connection.MIDDLE_NS;
                case 5 -> Connection.MIDDLE_EW;
                case 1 -> connection() == ROPE_CONNECTION ? Connection.MIDDLE_END_N : Connection.MIDDLE_EW;
                case 2 -> connection() == ROPE_CONNECTION ? Connection.MIDDLE_END_E : Connection.MIDDLE_NS;
                case 4 -> connection() == ROPE_CONNECTION ? Connection.MIDDLE_END_S : Connection.MIDDLE_EW;
                case 8 -> connection() == ROPE_CONNECTION ? Connection.MIDDLE_END_W : Connection.MIDDLE_NS;
                case 3 -> Connection.CORNER_NE;
                case 9 -> Connection.CORNER_NW;
                case 6 -> Connection.CORNER_SE;
                case 12 -> Connection.CORNER_SW;
                case 14 -> Connection.SIDE_N;
                case 13 -> Connection.SIDE_E;
                case 11 -> Connection.SIDE_S;
                case 7 -> Connection.SIDE_W;
                default -> Connection.BASE;
            };
            return state.setValue(connection(), result);
        }
        @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
            BlockState state = connected(defaultBlockState(), context.getLevel(), context.getClickedPos());
            if (state.hasProperty(AXIS_FACING)) state = state.setValue(AXIS_FACING,
                context.getHorizontalDirection().getAxis() == Direction.Axis.X ? Direction.EAST : Direction.NORTH);
            return state;
        }
        @Override protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks,
                BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighbor, RandomSource random) {
            return state.getValue(MANUAL) ? state : connected(state, level, pos);
        }
        @Override protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                Player player, InteractionHand hand, BlockHitResult hit) {
            if (pliers(stack)) {
                if (level instanceof ServerLevel server) {
                    if (!Authority.mayChange(server, player, pos)) return InteractionResult.FAIL;
                    BlockState next = player.isShiftKeyDown() ? connected(state.setValue(MANUAL, false), level, pos)
                        : state.cycle(connection()).setValue(MANUAL, true);
                    level.setBlockAndUpdate(pos, next);
                    if (!player.isCreative()) stack.hurtAndBreak(1, player, hand.asEquipmentSlot());
                }
                return InteractionResult.SUCCESS;
            }
            if (stack.is(asItem())) return extend(stack, level, pos.relative(player.getDirection()), player, hand);
            return InteractionResult.PASS;
        }
        @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return shapes == null ? Shapes.block() : shapes.get(state);
        }
    }
    static final class Rope extends Deck {
        Rope(Properties properties, JsonObject descriptor) { super(properties, descriptor); }
        @Override EnumProperty<Connection> connection() { return ROPE_CONNECTION; }
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            super.createBlockStateDefinition(builder); builder.add(AXIS_FACING);
        }
    }
    static final class Stairs extends OwnerFactoryBlocks.Facing {
        static final EnumProperty<StairConnection> CONNECTION = EnumProperty.create("connection", StairConnection.class);
        private final OwnerAuthoredShapes shapes;
        Stairs(Properties properties, JsonObject descriptor) {
            super(properties); registerDefaultState(defaultBlockState().setValue(CONNECTION, StairConnection.BASE));
            shapes = new OwnerAuthoredShapes(this, descriptor.getAsJsonObject("shapes"));
        }
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            super.createBlockStateDefinition(builder); builder.add(CONNECTION);
        }
        @Override protected BlockState mirror(BlockState state,Mirror mirror){
            var connection=state.getValue(CONNECTION);
            if(mirror!=Mirror.NONE)connection=switch(connection){case LEFT->StairConnection.RIGHT;case RIGHT->StairConnection.LEFT;default->connection;};
            return state.setValue(FACING,mirror.mirror(state.getValue(FACING))).setValue(CONNECTION,connection);
        }
        private BlockState connected(BlockState state, BlockGetter level, BlockPos pos) {
            Direction facing = state.getValue(FACING);
            BlockState left = level.getBlockState(pos.relative(facing));
            BlockState right = level.getBlockState(pos.relative(facing.getOpposite()));
            boolean l = left.is(this) && left.getValue(FACING) == facing, r = right.is(this) && right.getValue(FACING) == facing;
            return state.setValue(CONNECTION, l && r ? StairConnection.DOUBLE : l ? StairConnection.LEFT : r ? StairConnection.RIGHT : StairConnection.BASE);
        }
        @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
            return connected(defaultBlockState().setValue(FACING, context.getHorizontalDirection().getClockWise()), context.getLevel(), context.getClickedPos());
        }
        @Override protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks,
                BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighbor, RandomSource random) {
            return connected(state, level, pos);
        }
        @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return shapes == null ? Shapes.block() : shapes.get(state);
        }
        @Override protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                Player player, InteractionHand hand, BlockHitResult hit) {
            if (stack.is(asItem())) return extend(stack, level, pos.relative(player.getDirection()).above(), player, hand);
            return InteractionResult.PASS;
        }
    }
    static final class Support extends Block implements SimpleWaterloggedBlock {
        static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
        private final OwnerAuthoredShapes shapes;
        Support(Properties properties, JsonObject descriptor) {
            super(properties); registerDefaultState(defaultBlockState().setValue(WATERLOGGED, false));
            shapes = new OwnerAuthoredShapes(this, descriptor.getAsJsonObject("shapes"));
        }
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(WATERLOGGED); }
        @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(WATERLOGGED, context.getLevel().getFluidState(context.getClickedPos()).is(Fluids.WATER));
        }
        @Override protected FluidState getFluidState(BlockState state) { return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state); }
        @Override protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks,
                BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighbor, RandomSource random) {
            if (state.getValue(WATERLOGGED)) ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
            return state;
        }
        @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return shapes == null ? Shapes.block() : shapes.get(state);
        }
        @Override protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                Player player, InteractionHand hand, BlockHitResult hit) {
            if (!stack.is(asItem())) return InteractionResult.PASS;
            BlockPos target = pos.above();
            for (int i = 0; i < 32 && level.hasChunkAt(target) && level.getBlockState(target).is(this); i++) target = target.above();
            return extend(stack, level, target, player, hand);
        }
    }
    static final class Rail extends OwnerFactoryBlocks.Facing {
        static final BooleanProperty POWERED = BlockStateProperties.POWERED;
        private final OwnerAuthoredShapes shapes;
        Rail(Properties properties, JsonObject descriptor) {
            super(properties); registerDefaultState(defaultBlockState().setValue(POWERED, false));
            shapes = new OwnerAuthoredShapes(this, descriptor.getAsJsonObject("shapes"));
        }
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { super.createBlockStateDefinition(builder); builder.add(POWERED); }
        @Override public BlockState getStateForPlacement(BlockPlaceContext context) { return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getClockWise()); }
        @Override protected boolean isSignalSource(BlockState state) { return true; }
        @Override protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) { return state.getValue(POWERED) ? 10 : 0; }
        @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return shapes == null ? Shapes.block() : shapes.get(state); }
        @Override protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
            if (!stack.is(Items.REDSTONE_TORCH) || state.getValue(POWERED)) return InteractionResult.PASS;
            if (level instanceof ServerLevel server) {
                if (!Authority.mayChange(server, player, pos)) return InteractionResult.FAIL;
                level.setBlockAndUpdate(pos, state.setValue(POWERED, true)); if (!player.isCreative()) stack.shrink(1);
            }
            return InteractionResult.SUCCESS;
        }
        @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (!state.getValue(POWERED)) return InteractionResult.PASS;
            if (level instanceof ServerLevel server) {
                if (!Authority.mayChange(server, player, pos)) return InteractionResult.FAIL;
                level.setBlockAndUpdate(pos, state.setValue(POWERED, false));
                if (!player.isCreative()) popResource(level, pos, new ItemStack(Items.REDSTONE_TORCH));
            }
            return InteractionResult.SUCCESS;
        }
    }
    static final class Light extends OwnerFactoryBlocks.Facing {
        static final EnumProperty<LightState> LIGHT = EnumProperty.create("lightstate", LightState.class);
        private final OwnerAuthoredShapes shapes;
        Light(Properties properties, JsonObject descriptor) {
            super(properties.lightLevel(state -> 14).noCollision());
            registerDefaultState(defaultBlockState().setValue(LIGHT, LightState.BRIDGE));
            shapes = new OwnerAuthoredShapes(this, descriptor.getAsJsonObject("shapes"));
        }
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { super.createBlockStateDefinition(builder); builder.add(LIGHT); }
        @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
            BlockState below = context.getLevel().getBlockState(context.getClickedPos().below());
            if (!(below.getBlock() instanceof Deck) && !(below.getBlock() instanceof Stairs) && !below.isFaceSturdy(context.getLevel(), context.getClickedPos().below(), Direction.UP)) return null;
            return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getClockWise()).setValue(LIGHT, below.getBlock() instanceof Stairs ? LightState.STAIR : LightState.BRIDGE);
        }
        @Override protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks,
                BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighbor, RandomSource random) {
            if (direction != Direction.DOWN) return state;
            if (!(neighbor.getBlock() instanceof Deck) && !(neighbor.getBlock() instanceof Stairs) && !neighbor.isFaceSturdy(level, neighborPos, Direction.UP)) return Blocks.AIR.defaultBlockState();
            return state.setValue(LIGHT, neighbor.getBlock() instanceof Stairs ? LightState.STAIR : LightState.BRIDGE);
        }
        @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return shapes == null ? Shapes.block() : shapes.get(state); }
    }
}
