package io.github.jimbozoomer.jugcraft.storage;

import io.github.jimbozoomer.jugcraft.concordance.Authority;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import java.util.function.Consumer;

/** One server-thread transition, preserving the complete saved inventory, name and components. */
public final class StorageCrateUpgradeItem extends Item {
    public final StorageCrateTier target;
    public StorageCrateUpgradeItem(Properties properties, StorageCrateTier target) { super(properties); this.target = target; }
    @Override public InteractionResult useOn(UseOnContext context) {
        return context.getPlayer() == null ? InteractionResult.FAIL : upgrade(context.getLevel(), context.getClickedPos(), context.getPlayer(), context.getItemInHand());
    }
    public InteractionResult upgrade(Level level, BlockPos pos, Player player, ItemStack stack) {
        if (!(level.getBlockEntity(pos) instanceof StorageCrateBlockEntity old)) return InteractionResult.PASS;
        if (!stack.is(this) || stack.isEmpty() || !old.stillValid(player)) return InteractionResult.FAIL;
        if (!target.follows(old.tier())) {
            if (!level.isClientSide()) player.sendOverlayMessage(Component.translatable("message.jugcraft.storage_crate.requires",
                Component.translatable("block.jugcraft." + StorageCrateTier.values()[target.ordinal() - 1].id())));
            return InteractionResult.FAIL;
        }
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        if (!Authority.mayChange(server, player, pos)) return InteractionResult.FAIL;
        var state = JugcraftStorage.STORAGE_CRATES.get(target).defaultBlockState().setValue(StorageCrateBlock.FACING, old.getBlockState().getValue(StorageCrateBlock.FACING));
        // Construct and validate the entire replacement before touching the placed inventory.
        var loaded = BlockEntity.loadStatic(pos, state, old.saveWithFullMetadata(server.registryAccess()), server.registryAccess());
        if (!(loaded instanceof StorageCrateBlockEntity replacement) || replacement.getContainerSize() != target.slots) return InteractionResult.FAIL;
        for (ServerPlayer viewer : server.players()) if (viewer.containerMenu instanceof StorageCrateMenu menu && menu.isFor(old)) viewer.closeContainer();
        old.upgrading = true;
        if (!server.setBlock(pos, state, Block.UPDATE_CLIENTS)) { old.upgrading = false; return InteractionResult.FAIL; }
        server.setBlockEntity(replacement);
        old.clearContent(); // Invalidate a previously acquired automation reference as well as the old menu.
        replacement.setChanged();
        stack.consume(1, player);
        server.updateNeighborsAt(pos, state.getBlock());
        server.updateNeighbourForOutputSignal(pos, state.getBlock());
        return InteractionResult.SUCCESS;
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("tooltip.jugcraft.storage_crate.upgrade",
            Component.translatable("block.jugcraft." + StorageCrateTier.values()[target.ordinal() - 1].id()), target.slots));
    }
}
