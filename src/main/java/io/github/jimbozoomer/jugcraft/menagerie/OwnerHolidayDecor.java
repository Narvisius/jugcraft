package io.github.jimbozoomer.jugcraft.menagerie;

import com.google.gson.JsonObject;
import net.minecraft.core.*;
import net.minecraft.sounds.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

/** Persistent crafted seasonal decorations; no calendar deletes player builds. */
final class OwnerHolidayDecor extends HouseholdBlock {
    private final String reference;
    OwnerHolidayDecor(Properties p, JsonObject d) {
        super(p, d); reference = d.get("reference_class").getAsString();
        registerDefaultState(set(set(defaultBlockState(), "part", "top"), "shape", "straight"));
    }
    private BlockState joined(BlockState state, BlockGetter level, BlockPos pos) {
        if (reference.equals("SlimCane")) return set(state, "part", level.getBlockState(pos.above()).is(this) ? "bottom" : "top");
        if (!reference.equals("Garland")) return state;
        Direction facing = facing(state);
        var front = level.getBlockState(pos.relative(facing));
        var back = level.getBlockState(pos.relative(facing.getOpposite()));
        String shape = "straight";
        if (front.getBlock() instanceof OwnerHolidayDecor decor && decor.reference.equals("Garland") && facing(front).getAxis() != facing.getAxis())
            shape = facing(front) == facing.getCounterClockWise() ? "outer_left" : "outer_right";
        else if (back.getBlock() instanceof OwnerHolidayDecor decor && decor.reference.equals("Garland") && facing(back).getAxis() != facing.getAxis())
            shape = facing(back) == facing.getCounterClockWise() ? "inner_left" : "inner_right";
        return set(state, "shape", shape);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction direction = context.getHorizontalDirection().getClockWise();
        if (reference.equals("SlimCane")) direction = context.getHorizontalDirection().getOpposite();
        else if (reference.equals("Garland")) direction = context.getHorizontalDirection();
        BlockState state = set(defaultBlockState(), "facing", direction.getName());
        if (getStateDefinition().getProperty("face") != null) {
            Direction clicked = context.getClickedFace();
            state = set(state, "face", clicked == Direction.UP ? "floor" : clicked == Direction.DOWN ? "ceiling" : "wall");
            if (clicked.getAxis().isHorizontal()) state = set(state, "facing", clicked.getName());
        }
        return joined(state, context.getLevel(), context.getClickedPos());
    }
    @Override protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
            Direction direction, BlockPos neighborPos, BlockState neighbor, RandomSource random) { return joined(state, level, pos); }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!reference.equals("StringLights") && !reference.equals("JingleBell") && !reference.equals("CoupleBells")) return InteractionResult.PASS;
        if (!Menagerie.mayChange(level, player, pos) && !io.github.jimbozoomer.jugcraft.town.TownProtection.allowsPublicUse(player, level, pos)) return InteractionResult.FAIL;
        if (!level.isClientSide()) {
            if (reference.equals("StringLights")) level.setBlockAndUpdate(pos, set(state, "twinkle", Boolean.toString(!flag(state, "twinkle"))));
            else level.playSound(null, pos, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, .7F, 1.3F);
        }
        return InteractionResult.SUCCESS;
    }
    @Override protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }
    @Override public boolean publicTownInteraction() { return reference.equals("StringLights") || reference.equals("JingleBell") || reference.equals("CoupleBells"); }
    @Override protected BlockState mirror(BlockState state, Mirror mirror) {
        BlockState result = super.mirror(state, mirror);
        if (mirror != Mirror.NONE) {
            String shape = value(state, "shape");
            if (shape.endsWith("left")) result = set(result, "shape", shape.replace("left", "right"));
            else if (shape.endsWith("right")) result = set(result, "shape", shape.replace("right", "left"));
        }
        return result;
    }
    @Override protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return reference.equals("CrossTextureBlock") || reference.equals("SixDirectionalWeb") ? Shapes.empty() : super.getCollisionShape(state, level, pos, context);
    }
    @Override protected void entityInside(BlockState state, Level level, BlockPos pos, net.minecraft.world.entity.Entity entity,
            net.minecraft.world.entity.InsideBlockEffectApplier effects, boolean movement) {
        if (reference.equals("SixDirectionalWeb")) entity.makeStuckInBlock(state, new net.minecraft.world.phys.Vec3(.25, .05, .25));
    }
}
