package io.github.jimbozoomer.jugcraft.menagerie;

import com.google.gson.JsonObject;
import io.github.jimbozoomer.jugcraft.agriculture.KitchenCabinetBlockEntity;
import io.github.jimbozoomer.jugcraft.tower.SeatEntity;
import io.github.jimbozoomer.jugcraft.tower.Sittable;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Supplied Macaw furniture on the existing Jugcraft storage/seating/claim services. */
class OwnerJoinedFurniture extends HouseholdBlock {
    static final class Seat extends OwnerJoinedFurniture implements Sittable { Seat(Properties p, JsonObject d) { super(p, d); } }
    static final class Storage extends OwnerJoinedFurniture implements EntityBlock { Storage(Properties p, JsonObject d) { super(p, d); } }
    private final boolean axisOnly;
    OwnerJoinedFurniture(Properties p, JsonObject d) {
        super(p, d);
        axisOnly = d.getAsJsonObject("properties").has("facing") && d.getAsJsonObject("properties").getAsJsonArray("facing").size() == 2;
        var state = defaultBlockState();
        state = set(state, "connection", "single"); state = set(state, "connection", "base"); state = set(state, "connection", "with_leg");
        state = set(state, "shape", "single"); state = set(state, "water", "false");
        registerDefaultState(state);
    }
    private boolean same(BlockGetter level, BlockPos pos, BlockState state, boolean oriented) {
        var other = level.getBlockState(pos);
        return other.is(this) && (!oriented || facing(other) == facing(state));
    }
    private BlockState connected(BlockState state, BlockGetter level, BlockPos pos) {
        boolean n = same(level, pos.north(), state, true), e = same(level, pos.east(), state, true);
        boolean s = same(level, pos.south(), state, true), w = same(level, pos.west(), state, true);
        for (Direction direction : Direction.Plane.HORIZONTAL)
            state = set(state, direction.getName(), Boolean.toString(same(level, pos.relative(direction), state, false)));
        var connection = getStateDefinition().getProperty("connection");
        if (connection != null) {
            if (connection.getValue("top").isPresent()) {
                boolean up = same(level, pos.above(), state, true), down = same(level, pos.below(), state, true);
                state = set(state, "connection", up ? (down ? "middle" : "bottom") : (down ? "top" : "base"));
            } else if (connection.getValue("with_leg").isPresent()) {
                state = set(state, "connection", n && s || e && w ? "center" : "with_leg");
            } else {
                Direction facing = facing(state);
                boolean front = same(level, pos.relative(facing), state, true), back = same(level, pos.relative(facing.getOpposite()), state, true);
                boolean sideways = same(level, pos.relative(facing.getClockWise()), state, true) || same(level, pos.relative(facing.getCounterClockWise()), state, true);
                state = set(state, "connection", sideways ? "single" : front ? (back ? "middle" : "left") : (back ? "right" : "single"));
            }
        }
        if (name.endsWith("_couch")) {
            Direction facing = facing(state);
            boolean front = same(level, pos.relative(facing), state, true), back = same(level, pos.relative(facing.getOpposite()), state, true);
            // Supplied couch meshes use FACING along the couch row, rather than
            // the normal of the backrest. Preserve that coordinate convention.
            boolean side = same(level, pos.relative(facing.getClockWise()), state, false);
            String shape = front ? (back ? "middle" : side ? "left_corner" : "right") : back ? (side ? "right_corner" : "left") : "single";
            state = set(state, "shape", shape);
        }
        return state;
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction direction = name.endsWith("_counter") || kind.equals("sink") ? context.getHorizontalDirection() : context.getHorizontalDirection().getClockWise();
        if (axisOnly) direction = direction.getAxis() == Direction.Axis.X ? Direction.EAST : Direction.NORTH;
        BlockState state = set(defaultBlockState(), "facing", direction.getName());
        double offset = direction.getAxis() == Direction.Axis.Z ? context.getClickLocation().z - context.getClickedPos().getZ()
            : context.getClickLocation().x - context.getClickedPos().getX();
        state = set(state, "hinge", offset < .5 ? "left" : "right");
        return connected(state, context.getLevel(), context.getClickedPos());
    }
    @Override protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
            Direction direction, BlockPos neighborPos, BlockState neighbor, RandomSource random) {
        return connected(state, level, pos);
    }
    @Override public double seatHeight() { return name.endsWith("_couch") || name.endsWith("_chaise") ? 7.0 / 16 : .5; }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (kind.equals("storage")) {
            if (!Menagerie.mayChange(level, player, pos)) return InteractionResult.FAIL;
            if (level instanceof ServerLevel && level.getBlockEntity(pos) instanceof KitchenCabinetBlockEntity cabinet) player.openMenu(cabinet);
            return InteractionResult.SUCCESS;
        }
        if (kind.equals("seat")) {
            if (!Menagerie.mayChange(level, player, pos) && !io.github.jimbozoomer.jugcraft.town.TownProtection.allowsPublicUse(player, level, pos)) return InteractionResult.FAIL;
            if (!level.isClientSide()) SeatEntity.sit(level, pos, player, facing(state).getClockWise(), seatHeight());
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
    @Override protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!kind.equals("sink")) return InteractionResult.TRY_WITH_EMPTY_HAND;
        boolean filled = flag(state, "water");
        boolean fill = stack.is(Items.WATER_BUCKET) && !filled;
        boolean bucket = stack.is(Items.BUCKET) && filled;
        boolean bottle = stack.is(Items.GLASS_BOTTLE) && filled;
        if (!fill && !bucket && !bottle) return InteractionResult.PASS;
        if (!Menagerie.mayChange(level, player, pos)) return InteractionResult.FAIL;
        if (!level.isClientSide()) {
            ItemStack result = fill ? new ItemStack(Items.BUCKET) : bucket ? new ItemStack(Items.WATER_BUCKET)
                : PotionContents.createItemStack(Items.POTION, Potions.WATER);
            level.setBlockAndUpdate(pos, set(state, "water", Boolean.toString(fill)));
            player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, result));
            level.playSound(null, pos, fill ? SoundEvents.BUCKET_EMPTY : bucket ? SoundEvents.BUCKET_FILL : SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, .8F, 1);
        }
        return InteractionResult.SUCCESS;
    }
    @Override protected BlockState rotate(BlockState state, Rotation rotation) {
        Direction direction = rotation.rotate(facing(state));
        if (axisOnly) direction = direction.getAxis() == Direction.Axis.X ? Direction.EAST : Direction.NORTH;
        BlockState result = set(state, "facing", direction.getName());
        for (Direction side : Direction.Plane.HORIZONTAL) result = set(result, rotation.rotate(side).getName(), value(state, side.getName()));
        return result;
    }
    @Override protected BlockState mirror(BlockState state, Mirror mirror) {
        if (mirror == Mirror.NONE) return state;
        BlockState result = rotate(state, mirror.getRotation(facing(state)));
        if (getStateDefinition().getProperty("hinge") != null) result = set(result, "hinge", value(state, "hinge").equals("left") ? "right" : "left");
        for (String property : new String[]{"connection", "shape"}) {
            String value = value(state, property);
            if (value.contains("left")) result = set(result, property, value.replace("left", "right"));
            else if (value.contains("right")) result = set(result, property, value.replace("right", "left"));
        }
        for (Direction side : Direction.Plane.HORIZONTAL) result = set(result, mirror.mirror(side).getName(), value(state, side.getName()));
        return result;
    }
}
