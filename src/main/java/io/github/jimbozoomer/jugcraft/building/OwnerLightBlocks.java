package io.github.jimbozoomer.jugcraft.building;

import com.google.gson.JsonObject;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.town.TownProtection;
import java.util.Locale;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.util.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

/** Supplied lamps: manual switches, redstone edges, dyed shades and stacked posts. */
final class OwnerLightBlocks {
    private OwnerLightBlocks() { }
    static class Lamp extends Block {
        private final OwnerAuthoredShapes shapes;
        private final boolean flame, soul;
        Lamp(Properties properties, JsonObject descriptor) {
            super(properties); flame = descriptor.get("flame").getAsBoolean(); soul = descriptor.get("soul").getAsBoolean();
            registerDefaultState(defaultBlockState().setValue(BlockStateProperties.LIT, true).setValue(BlockStateProperties.POWERED, false));
            shapes = new OwnerAuthoredShapes(this, descriptor.getAsJsonObject("shapes"));
        }
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(BlockStateProperties.LIT, BlockStateProperties.POWERED);
        }
        @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(BlockStateProperties.POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos()));
        }
        protected BlockState switched(BlockState state, boolean on) { return state.setValue(BlockStateProperties.LIT, on); }
        private InteractionResult toggle(BlockState state, Level level, BlockPos pos, Player player) {
            if (state.hasProperty(Column.PART) && (state.getValue(Column.PART) == Part.MIDDLE || state.getValue(Column.PART) == Part.BOTTOM)) return InteractionResult.PASS;
            if (level instanceof ServerLevel server) {
                if (!Authority.mayChange(server, player, pos) && !TownProtection.allowsPublicUse(player, level, pos)) return InteractionResult.FAIL;
                level.setBlockAndUpdate(pos, switched(state, !state.getValue(BlockStateProperties.LIT)));
                level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, .5F, 1);
            }
            return InteractionResult.SUCCESS;
        }
        @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            return toggle(state, level, pos, player);
        }
        @Override protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
            if (stack.getItem() instanceof BlockItem) return InteractionResult.PASS;
            return toggle(state, level, pos, player);
        }
        @Override protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, Orientation orientation, boolean moved) {
            if (!level.isClientSide()) {
                boolean powered = level.hasNeighborSignal(pos);
                if (powered != state.getValue(BlockStateProperties.POWERED))
                    level.setBlockAndUpdate(pos, switched(state, powered).setValue(BlockStateProperties.POWERED, powered));
            }
        }
        @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return shapes == null ? Shapes.block() : shapes.get(state);
        }
        @Override public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            if (flame && state.getValue(BlockStateProperties.LIT)) {
                level.addParticle(soul ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.FLAME, pos.getX() + .5, pos.getY() + .85, pos.getZ() + .5, 0, 0, 0);
                if (random.nextInt(3) == 0) level.addParticle(ParticleTypes.SMOKE, pos.getX() + .5, pos.getY() + .95, pos.getZ() + .5, 0, 0, 0);
            }
        }
    }
    static class Wall extends Lamp {
        Wall(Properties properties, JsonObject descriptor) {
            super(properties, descriptor); registerDefaultState(defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
        }
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            super.createBlockStateDefinition(builder); builder.add(BlockStateProperties.HORIZONTAL_FACING);
        }
        @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
            return super.getStateForPlacement(context).setValue(BlockStateProperties.HORIZONTAL_FACING, context.getHorizontalDirection().getOpposite());
        }
        @Override protected BlockState rotate(BlockState state, Rotation rotation) { return state.setValue(BlockStateProperties.HORIZONTAL_FACING, rotation.rotate(state.getValue(BlockStateProperties.HORIZONTAL_FACING))); }
        @Override protected BlockState mirror(BlockState state, Mirror mirror) { return rotate(state, mirror.getRotation(state.getValue(BlockStateProperties.HORIZONTAL_FACING))); }
    }
    enum Part implements StringRepresentable {
        BASE, BOTTOM, MIDDLE, TOP;
        @Override public String getSerializedName() { return name().toLowerCase(Locale.ROOT); }
    }
    static final class Column extends Wall {
        static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);
        static final BooleanProperty ENABLED = BooleanProperty.create("enabled");
        Column(Properties properties, JsonObject descriptor) {
            super(properties, descriptor); registerDefaultState(defaultBlockState().setValue(PART, Part.BASE).setValue(ENABLED, true));
        }
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            super.createBlockStateDefinition(builder); builder.add(PART, ENABLED);
        }
        private BlockState joined(BlockState state, BlockGetter level, BlockPos pos) {
            boolean above = level.getBlockState(pos.above()).is(this), below = level.getBlockState(pos.below()).is(this);
            return state.setValue(PART, above ? (below ? Part.MIDDLE : Part.BOTTOM) : (below ? Part.TOP : Part.BASE))
                .setValue(BlockStateProperties.LIT, !above && state.getValue(ENABLED));
        }
        @Override protected BlockState switched(BlockState state, boolean on) {
            return state.setValue(ENABLED, on).setValue(BlockStateProperties.LIT, on && (state.getValue(PART) == Part.TOP || state.getValue(PART) == Part.BASE));
        }
        @Override public BlockState getStateForPlacement(BlockPlaceContext context) { return joined(super.getStateForPlacement(context), context.getLevel(), context.getClickedPos()); }
        @Override protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighbor, RandomSource random) {
            return joined(state, level, pos);
        }
    }
    static final class Mounted extends Wall {
        Mounted(Properties properties, JsonObject descriptor) { super(properties, descriptor); registerDefaultState(defaultBlockState().setValue(BlockStateProperties.ATTACH_FACE, AttachFace.FLOOR)); }
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { super.createBlockStateDefinition(builder); builder.add(BlockStateProperties.ATTACH_FACE); }
        @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
            Direction face = context.getClickedFace();
            return super.getStateForPlacement(context).setValue(BlockStateProperties.ATTACH_FACE, face == Direction.UP ? AttachFace.FLOOR : face == Direction.DOWN ? AttachFace.CEILING : AttachFace.WALL)
                .setValue(BlockStateProperties.HORIZONTAL_FACING, face.getAxis().isHorizontal() ? face : context.getHorizontalDirection().getOpposite());
        }
    }
    static final class Dyeable extends Wall {
        static final EnumProperty<DyeColor> COLOR = EnumProperty.create("dye_color", DyeColor.class);
        Dyeable(Properties properties, JsonObject descriptor) { super(properties, descriptor); registerDefaultState(defaultBlockState().setValue(COLOR, DyeColor.WHITE)); }
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { super.createBlockStateDefinition(builder); builder.add(COLOR); }
        @Override protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
            DyeColor color = stack.get(DataComponents.DYE);
            if (color == null) return super.useItemOn(stack, state, level, pos, player, hand, hit);
            if (state.getValue(COLOR) == color) return InteractionResult.PASS;
            if (level instanceof ServerLevel server) {
                if (!Authority.mayChange(server, player, pos)) return InteractionResult.FAIL;
                level.setBlockAndUpdate(pos, state.setValue(COLOR, color)); stack.consume(1, player);
            }
            return InteractionResult.SUCCESS;
        }
    }
    static final class Lantern extends LanternBlock {
        private final OwnerAuthoredShapes shapes;
        Lantern(Properties properties, JsonObject descriptor) { super(properties); shapes = new OwnerAuthoredShapes(this, descriptor.getAsJsonObject("shapes")); }
        @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return shapes == null ? Shapes.block() : shapes.get(state); }
    }
}
