package io.github.jimbozoomer.jugcraft.building;

import io.github.jimbozoomer.jugcraft.concordance.Authority;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** One-material carving, using the native server-authoritative recipe picker. */
final class OwnerChisel extends Item {
    private static final Map<Block, List<Block>> FAMILIES = new IdentityHashMap<>();
    OwnerChisel(Properties properties) { super(properties.stacksTo(1)); }

    static void finishFamilies(Map<String, List<Block>> groups) {
        FAMILIES.clear();
        for (var entry : groups.entrySet()) {
            Block base = BuiltInRegistries.BLOCK.getValue(Identifier.parse(entry.getKey()));
            if (base == null || base == Blocks.AIR) throw new IllegalStateException("Unknown carving material " + entry.getKey());
            var family = new ArrayList<Block>(); family.add(base); family.addAll(entry.getValue());
            List<Block> immutable = List.copyOf(family);
            for (Block block : family) FAMILIES.put(block, immutable);
        }
    }

    @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player.isSpectator()) return InteractionResult.FAIL;
        if (!level.isClientSide()) player.openMenu(new SimpleMenuProvider((id, inventory, who) ->
            new StonecutterMenu(id, inventory, ContainerLevelAccess.create(level, player.blockPosition())) {
                @Override public boolean stillValid(Player who) {
                    return who == player && who.isAlive() && !who.isSpectator() && who.level() == level
                        && (who.getMainHandItem().is(OwnerChisel.this) || who.getOffhandItem().is(OwnerChisel.this));
                }
            }, Component.translatable("container.jugcraft.owner_chisel")));
        return InteractionResult.SUCCESS;
    }

    @Override public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel(); Player player = context.getPlayer(); BlockPos pos = context.getClickedPos();
        BlockState old = level.getBlockState(pos); List<Block> family = FAMILIES.get(old.getBlock());
        if (family == null || player == null) return InteractionResult.PASS;
        if (level instanceof ServerLevel server) {
            if (!Authority.mayChange(server, player, pos) || level.getBlockEntity(pos) != null) return InteractionResult.FAIL;
            int index = family.indexOf(old.getBlock());
            Block next = family.get(Math.floorMod(index + (player.isShiftKeyDown() ? -1 : 1), family.size()));
            level.setBlockAndUpdate(pos, next.withPropertiesOf(old));
        }
        return InteractionResult.SUCCESS;
    }

    static final class PoweredPillar extends RotatedPillarBlock {
        PoweredPillar(Properties properties) { super(properties); }
        @Override protected boolean isSignalSource(BlockState state) { return true; }
        @Override protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) { return 15; }
    }
    static final class IcePillar extends IceBlock {
        IcePillar(Properties properties) {
            super(properties); registerDefaultState(defaultBlockState().setValue(BlockStateProperties.AXIS, Direction.Axis.Y));
        }
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(BlockStateProperties.AXIS);
        }
        @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(BlockStateProperties.AXIS, context.getClickedFace().getAxis());
        }
        @Override protected BlockState rotate(BlockState state, Rotation rotation) {
            return RotatedPillarBlock.rotatePillar(state, rotation);
        }
    }
}
