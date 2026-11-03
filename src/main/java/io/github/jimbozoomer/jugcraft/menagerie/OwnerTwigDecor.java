package io.github.jimbozoomer.jugcraft.menagerie;

import com.google.gson.JsonObject;
import io.github.jimbozoomer.jugcraft.building.OwnerTwigs;
import net.minecraft.core.*;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;

/** Source-authored table joins, layered foliage, columns and manual lamps. */
final class OwnerTwigDecor extends HouseholdBlock implements OptionalWaterloggedBlock {
    private final String reference;
    OwnerTwigDecor(Properties p, JsonObject definition) {
        super(p, definition); reference = definition.get("reference_class").getAsString();
        registerDefaultState(set(set(set(defaultBlockState(), "lit", "true"), "layers", "1"), "shape", "tip"));
    }
    private boolean floor() { return reference.equals("FloorItemBlock") || reference.equals("SeashellBlock"); }
    private boolean table(BlockState s) { return s.getBlock() instanceof OwnerTwigDecor b && b.reference.equals("TableBlock"); }
    private BlockState joined(BlockState s, BlockGetter level, BlockPos pos) {
        if (reference.equals("ColumnBlock")) return set(s, "shape", level.getBlockState(pos.above()).getBlock() instanceof OwnerTwigDecor b && b.reference.equals("ColumnBlock") ? "base" : "tip");
        if (!reference.equals("TableBlock")) return s;
        Direction[] a = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
        int connections = 0;
        for (int i = 0; i < 4; i++) {
            Direction first = a[i], second = a[(i + 1) % 4];
            boolean x = table(level.getBlockState(pos.relative(first))), y = table(level.getBlockState(pos.relative(second)));
            if (x) connections++;
            s = set(s, "table_leg" + (i + 1), Boolean.toString(!x && !y || x && y && !table(level.getBlockState(pos.relative(first).relative(second)))));
        }
        // Changing parity propagates diagonal corner changes without a recursive scan.
        return set(s, "update", Boolean.toString(connections % 2 == 0));
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext c) {
        var old = c.getLevel().getBlockState(c.getClickedPos());
        if (reference.equals("BambooLeavesBlock") && old.is(this)) return set(old, "layers", Integer.toString(Math.min(4, Integer.parseInt(value(old, "layers")) + 1)));
        BlockState state = set(defaultBlockState(), "facing", c.getHorizontalDirection().getOpposite().getName());
        if (reference.equals("FacingBlock") || reference.equals("CompactedDripstoneBlock")) state = set(state, "facing", c.getClickedFace().getOpposite().getName());
        state = set(state, "waterlogged", Boolean.toString(c.getLevel().getFluidState(c.getClickedPos()).getType() == Fluids.WATER));
        return joined(state, c.getLevel(), c.getClickedPos());
    }
    @Override protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction dir, BlockPos neighbor, BlockState other, RandomSource random) {
        if (flag(state, "waterlogged")) ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        return !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState() : joined(state, level, pos);
    }
    @Override protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (!floor() && !reference.equals("BambooLeavesBlock")) return true;
        var below = level.getBlockState(pos.below());
        return !below.is(BlockTags.LEAVES) && (below.isFaceSturdy(level, pos.below(), Direction.UP) || !below.getCollisionShape(level, pos.below()).getFaceShape(Direction.UP).isEmpty());
    }
    @Override protected FluidState getFluidState(BlockState state) { return flag(state, "waterlogged") ? Fluids.WATER.getSource(false) : super.getFluidState(state); }
    @Override protected boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        return reference.equals("BambooLeavesBlock") && context.getItemInHand().is(asItem()) && Integer.parseInt(value(state, "layers")) < 4 || super.canBeReplaced(state, context);
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!reference.equals("LampBlock") || player.isShiftKeyDown()) return InteractionResult.PASS;
        if (!Menagerie.mayChange(level, player, pos) && !io.github.jimbozoomer.jugcraft.town.TownProtection.allowsPublicUse(player, level, pos)) return InteractionResult.FAIL;
        if (!level.isClientSide()) {
            boolean on = !flag(state, "lit"); level.setBlockAndUpdate(pos, set(state, "lit", Boolean.toString(on)));
            level.playSound(null, pos, OwnerTwigs.sound("block.lamp." + (on ? "on" : "off")), net.minecraft.sounds.SoundSource.BLOCKS, 1, 1);
        }
        return InteractionResult.SUCCESS;
    }
    @Override protected InteractionResult useItemOn(ItemStack stack, BlockState s, Level l, BlockPos p, Player who, InteractionHand hand, BlockHitResult hit) { return InteractionResult.TRY_WITH_EMPTY_HAND; }
    @Override public boolean publicTownInteraction() { return reference.equals("LampBlock"); }
    @Override protected VoxelShape getCollisionShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return reference.equals("BambooLeavesBlock") ? Shapes.empty() : super.getCollisionShape(s, l, p, c); }
    @Override protected void entityInside(BlockState s, Level l, BlockPos p, Entity entity, InsideBlockEffectApplier effects, boolean edge) {
        if (reference.equals("BambooLeavesBlock") && Integer.parseInt(value(s, "layers")) > 2 && entity instanceof LivingEntity && !entity.getType().builtInRegistryHolder().is(OwnerTwigs.BAMBOO_IMMUNE)) entity.makeStuckInBlock(s, new Vec3(.8, .75, .8));
    }
}
