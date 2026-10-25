package io.github.jimbozoomer.jugcraft.jimtafacts.network;

import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.finance.ClaimTransfers;
import io.github.jimbozoomer.jugcraft.mixin.AlchemicalItemAccess;
import java.util.*;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.AABB;

/** Single loaded block work area, real world drops, no invented recipe outputs and no container destruction. */
public final class DigitalPlanes {
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty LOW_X=net.minecraft.world.level.block.state.properties.BooleanProperty.create("join_low_x"),HIGH_X=net.minecraft.world.level.block.state.properties.BooleanProperty.create("join_high_x"),LOW_Y=net.minecraft.world.level.block.state.properties.BooleanProperty.create("join_low_y"),HIGH_Y=net.minecraft.world.level.block.state.properties.BooleanProperty.create("join_high_y");
    public static final class PlaneBlock extends DigitalNodeBlock{
        public PlaneBlock(Properties p,String kind){super(p,kind);}
        @Override protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<Block,net.minecraft.world.level.block.state.BlockState> b){super.createBlockStateDefinition(b);b.add(LOW_X,HIGH_X,LOW_Y,HIGH_Y);}
    }
    private DigitalPlanes() {}
    static void appearance(DigitalNode node){
        if(!node.kind().endsWith("_plane")||node.getLevel()==null)return;
        var face=node.getBlockState().getValue(DigitalNodeBlock.FACING);
        var left=switch(face){case NORTH,UP,DOWN->net.minecraft.core.Direction.WEST;case SOUTH->net.minecraft.core.Direction.EAST;case EAST->net.minecraft.core.Direction.NORTH;case WEST->net.minecraft.core.Direction.SOUTH;};
        var lower=switch(face){case UP->net.minecraft.core.Direction.NORTH;case DOWN->net.minecraft.core.Direction.SOUTH;default->net.minecraft.core.Direction.DOWN;};
        var old=node.getBlockState();var state=old;
        var sides=new net.minecraft.core.Direction[]{left,left.getOpposite(),lower,lower.getOpposite()};
        var flags=List.of(LOW_X,HIGH_X,LOW_Y,HIGH_Y);
        for(int i=0;i<4;i++){
            var next=node.getBlockPos().relative(sides[i]);
            boolean joined=DigitalParts.at(node.getLevel(),next).stream().anyMatch(other->other.kind().equals(node.kind())&&other.getBlockState().getValue(DigitalNodeBlock.FACING)==face);
            state=state.setValue(flags.get(i),joined);
        }
        if(!state.equals(old))node.updateState(state);
    }
    public static void tick(DigitalNode node, DigitalNetwork network, ServerPlayer actor) {
        if (!node.kind().endsWith("_plane")) return;
        var face = node.getBlockState().getValue(DigitalNodeBlock.FACING); var at = node.getBlockPos().relative(face); var level = actor.level();
        if (!level.isLoaded(at) || !Authority.mayChange(level, actor, at) || !ClaimTransfers.allowed(level, node.getBlockPos(), at)) return;
        var state = level.getBlockState(at); var filter = node.filter();
        if (filter.isEmpty()) { var samples = DigitalUpgrades.samples(node); if (!samples.isEmpty()) filter = samples.getFirst(); }
        if (node.kind().equals("formation_plane")) {
            if (!DigitalUpgrades.samples(node).isEmpty()) filter = network.contents().stream().map(CellContents.Entry::sample).filter(s -> DigitalUpgrades.accepts(node, s)).findFirst().orElse(ItemStack.EMPTY);
            if (filter.isEmpty() || !state.isAir()) return;
            if (filter.getItem() instanceof BlockItem item) {
                var block = item.getBlock();
                if (block instanceof EntityBlock || block instanceof BedBlock || block instanceof DoorBlock || block instanceof DoublePlantBlock) return;
                var placed = block.defaultBlockState(); var shape = placed.getCollisionShape(level, at);
                if (!placed.canSurvive(level, at) || !shape.isEmpty() && !level.noCollision(shape.move(at.getX(), at.getY(), at.getZ()).bounds())) return;
                try (var tx = Transaction.openOuter()) {
                    if (!network.pay(100, tx) || network.extract(filter, 1, tx) != 1) return;
                    if (level.setBlock(at, placed, Block.UPDATE_ALL)) tx.commit();
                }
            } else {
                // Items are ejected intact. Never invoke arbitrary item use or silently strip saved contents.
                try (var tx = Transaction.openOuter()) {
                    if (!network.pay(100, tx)) return;
                    int n = (int) network.extract(filter, Math.min(filter.getMaxStackSize(), DigitalUpgrades.batch(node)), tx); if (n <= 0) return;
                    var dropped = new ItemEntity(level, at.getX() + .5, at.getY() + .3, at.getZ() + .5, filter.copyWithCount(n)); dropped.setDefaultPickUpDelay();
                    if (level.addFreshEntity(dropped)) tx.commit();
                }
            }
        } else {
            var nearby = new ArrayList<ItemEntity>(); level.getEntities(net.minecraft.world.level.entity.EntityTypeTest.forClass(ItemEntity.class), new AABB(at), e -> e.isAlive() && !e.hasPickUpDelay(), nearby, 16);
            for (var dropped : nearby) {
                var reserved = ((AlchemicalItemAccess) dropped).jugcraft$pickupTarget(); var stack = dropped.getItem();
                if (reserved != null && !reserved.equals(actor.getUUID()) || !DigitalUpgrades.accepts(node, stack)) continue;
                try (var tx = Transaction.openOuter()) {
                    int n = (int) network.insert(stack, Math.min(stack.getCount(), DigitalUpgrades.batch(node)), tx);
                    if (n > 0) { tx.commit(); if (n == stack.getCount()) dropped.discard(); else dropped.setItem(stack.copyWithCount(stack.getCount() - n)); return; }
                }
            }
            if (state.isAir() || state.hasBlockEntity() || !state.getFluidState().isEmpty() || state.getDestroySpeed(level, at) < 0) return;
            var tool = new ItemStack(Items.DIAMOND_PICKAXE);
            if (state.requiresCorrectToolForDrops() && !tool.isCorrectToolForDrops(state)) return;
            var drops = Block.getDrops(state, level, at, null, actor, tool);
            if (drops.size() > 64 || drops.stream().noneMatch(s -> DigitalUpgrades.accepts(node, s))) return;
            try (var tx = Transaction.openOuter()) {
                if (!network.pay(100, tx)) return;
                for (var stack : drops) if (network.insert(stack, stack.getCount(), tx) != stack.getCount()) return;
                if (level.destroyBlock(at, false, actor)) tx.commit();
            }
        }
    }
}
