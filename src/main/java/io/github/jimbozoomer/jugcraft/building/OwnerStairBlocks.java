package io.github.jimbozoomer.jugcraft.building;

import com.google.gson.JsonObject;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import java.util.Locale;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

/** Original stair profiles and independently paid, customizable balcony rails. */
final class OwnerStairBlocks {
    private OwnerStairBlocks() { }
    enum Style implements StringRepresentable {
        CLASSIC, HARP, SMOOTH;
        @Override public String getSerializedName() { return name().toLowerCase(Locale.ROOT); }
    }
    static final EnumProperty<Style> STYLE = EnumProperty.create("style", Style.class);
    static final BooleanProperty TOGGLE = BooleanProperty.create("toggle");
    static InteractionResult customize(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player) {
        if (!stack.is(Items.SHEARS) && !stack.is(OwnerBuildingCollection.ITEMS.get("windowworks_hammer"))) return InteractionResult.PASS;
        if (level instanceof ServerLevel server) {
            if (!Authority.mayChange(server, player, pos)) return InteractionResult.FAIL;
            level.setBlockAndUpdate(pos, player.isShiftKeyDown() && state.hasProperty(TOGGLE) ? state.cycle(TOGGLE) : state.cycle(STYLE));
        }
        return InteractionResult.SUCCESS;
    }
    static class Platform extends Block {
        private final OwnerAuthoredShapes shapes;
        Platform(Properties properties, JsonObject descriptor) {
            super(properties); shapes = new OwnerAuthoredShapes(this, descriptor.getAsJsonObject("shapes"));
        }
        @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return shapes == null ? Shapes.block() : shapes.get(state);
        }
    }
    static class Steps extends Platform {
        Steps(Properties properties, JsonObject descriptor) {
            super(properties, descriptor); registerDefaultState(defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
        }
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(BlockStateProperties.HORIZONTAL_FACING); }
        @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, context.getHorizontalDirection());
        }
        @Override protected BlockState rotate(BlockState state, Rotation rotation) {
            return state.setValue(BlockStateProperties.HORIZONTAL_FACING, rotation.rotate(state.getValue(BlockStateProperties.HORIZONTAL_FACING)));
        }
        @Override protected BlockState mirror(BlockState state, Mirror mirror) { return rotate(state, mirror.getRotation(state.getValue(BlockStateProperties.HORIZONTAL_FACING))); }
    }
    static final class Railing extends Steps {
        Railing(Properties properties, JsonObject descriptor) {
            super(properties, descriptor); registerDefaultState(defaultBlockState().setValue(STYLE, Style.CLASSIC).setValue(TOGGLE, false));
        }
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            super.createBlockStateDefinition(builder); builder.add(STYLE, TOGGLE);
        }
        @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
            var neighbor = context.getLevel().getBlockState(context.getClickedPos().relative(context.getClickedFace().getOpposite()));
            Direction direction = neighbor.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                ? neighbor.getValue(BlockStateProperties.HORIZONTAL_FACING) : context.getHorizontalDirection().getOpposite();
            double x = context.getClickLocation().x - context.getClickedPos().getX();
            double z = context.getClickLocation().z - context.getClickedPos().getZ();
            boolean side = switch (direction) { case NORTH -> x <= .5; case SOUTH -> x > .5; case WEST -> z > .5; default -> z <= .5; };
            return defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, direction).setValue(TOGGLE, side);
        }
        @Override protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
            return customize(stack, state, level, pos, player);
        }
        @Override protected BlockState mirror(BlockState state, Mirror mirror) {
            return mirror == Mirror.NONE ? state : super.mirror(state, mirror).cycle(TOGGLE);
        }
    }
    static final class Balcony extends Platform {
        Balcony(Properties properties, JsonObject descriptor) {
            super(properties, descriptor);
            registerDefaultState(defaultBlockState().setValue(STYLE, Style.CLASSIC).setValue(BlockStateProperties.NORTH, false)
                .setValue(BlockStateProperties.SOUTH, false).setValue(BlockStateProperties.EAST, false).setValue(BlockStateProperties.WEST, false));
        }
        private static BooleanProperty side(Direction direction) {
            return switch (direction) { case NORTH -> BlockStateProperties.NORTH; case SOUTH -> BlockStateProperties.SOUTH;
                case EAST -> BlockStateProperties.EAST; default -> BlockStateProperties.WEST; };
        }
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(STYLE, BlockStateProperties.NORTH, BlockStateProperties.SOUTH, BlockStateProperties.EAST, BlockStateProperties.WEST);
        }
        @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(side(context.getHorizontalDirection()), true);
        }
        @Override protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
            if (stack.is(asItem())) {
                var side = side(player.getDirection());
                if (state.getValue(side)) return InteractionResult.PASS;
                if (level instanceof ServerLevel server) {
                    if (!Authority.mayChange(server, player, pos)) return InteractionResult.FAIL;
                    if (level.setBlockAndUpdate(pos, state.setValue(side, true))) stack.consume(1, player);
                }
                return InteractionResult.SUCCESS;
            }
            return customize(stack, state, level, pos, player);
        }
        @Override protected BlockState rotate(BlockState state, Rotation rotation) {
            BlockState next = state;
            for (Direction direction : Direction.Plane.HORIZONTAL) next = next.setValue(side(rotation.rotate(direction)), state.getValue(side(direction)));
            return next;
        }
        @Override protected BlockState mirror(BlockState state, Mirror mirror) {
            BlockState next = state;
            for (Direction direction : Direction.Plane.HORIZONTAL) next = next.setValue(side(mirror.mirror(direction)), state.getValue(side(direction)));
            return next;
        }
    }
}
