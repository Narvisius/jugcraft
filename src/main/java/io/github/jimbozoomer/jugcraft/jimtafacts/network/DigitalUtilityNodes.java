package io.github.jimbozoomer.jugcraft.jimtafacts.network;

import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.BlockHitResult;

/** Small starter power sources and a light-to-redstone sensor; shared JE, ownership and loaded bounds. */
public final class DigitalUtilityNodes {
    public static final String[] BLOCKS = {"crank", "crystal_resonance_generator", "light_detector"};
    private DigitalUtilityNodes() {}
    static void tick(DigitalNode node) {
        var level = node.getLevel(); var pos = node.getBlockPos();
        if (node.kind().equals("crystal_resonance_generator") && !level.hasNeighborSignal(pos)) node.energy.setAmount(Math.min(node.energy.getCapacity(), node.energy.getAmount() + 40));
        if (node.kind().equals("light_detector")) {
            int light = level.getMaxLocalRawBrightness(pos.relative(node.getBlockState().getValue(DigitalNodeBlock.FACING)));
            light = Math.clamp(light, 0, 15);
            if (node.getBlockState().getValue(DigitalLinks.OUTPUT) != light) level.setBlock(pos, node.getBlockState().setValue(DigitalLinks.OUTPUT, light), Block.UPDATE_ALL);
        }
    }
    public static final class Crank extends DigitalNodeBlock implements io.github.jimbozoomer.jugcraft.energy.EnergyConnectable {
        public Crank(Properties p) { super(p, "crank"); }
        @Override public BlockState getStateForPlacement(BlockPlaceContext c) { return defaultBlockState().setValue(FACING, c.getClickedFace()); }
        @Override protected InteractionResult useWithoutItem(BlockState s, Level level, BlockPos pos, Player p, BlockHitResult hit) {
            if (p instanceof ServerPlayer actor && level.getBlockEntity(pos) instanceof DigitalNode node && node.permitted(actor) && RateGate.allow(actor, "jimta_crank", 5)) {
                long added = Math.min(40, node.energy.getCapacity() - node.energy.getAmount());
                if (added > 0) { node.energy.setAmount(node.energy.getAmount() + added); level.blockEvent(pos, this, 1, 1); }
                actor.sendOverlayMessage(Component.literal(node.energy.getAmount() + " JE · turn to generate"));
            }
            return InteractionResult.SUCCESS;
        }
    }
    public static final class Detector extends DigitalNodeBlock {
        public Detector(Properties p) { super(p, "light_detector"); }
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { super.createBlockStateDefinition(b); b.add(DigitalLinks.OUTPUT, DigitalFixtures.Fixture.ODD); }
        @Override public BlockState getStateForPlacement(BlockPlaceContext c) { return defaultBlockState().setValue(FACING, c.getClickedFace()).setValue(DigitalFixtures.Fixture.ODD, Math.floorMod(c.getClickedPos().getX() + c.getClickedPos().getY() + c.getClickedPos().getZ(), 2) != 0); }
        @Override protected boolean isSignalSource(BlockState s) { return true; }
        @Override protected int getSignal(BlockState s, BlockGetter l, BlockPos p, Direction side) { return s.getValue(DigitalLinks.OUTPUT); }
    }
}
