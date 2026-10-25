package io.github.jimbozoomer.jugcraft.jimtafacts.network;

import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/** A public readout only exposes the owner-selected bare item identity and aggregate amount. */
public final class DigitalDisplays {
    private DigitalDisplays() {}
    public static boolean monitor(String kind) { return kind.equals("storage_monitor") || kind.equals("conversion_monitor"); }
    public static void tick(DigitalNode node, DigitalNetwork network) {
        if (!monitor(node.kind())) return;
        long amount = node.filter().isEmpty() ? 0 : network.contents().stream().filter(e -> ItemStack.isSameItemSameComponents(e.sample(), node.filter())).mapToLong(CellContents.Entry::count).sum();
        if (node.observed != amount) { node.observed = amount; node.setChanged(); }
    }
    public static void interact(DigitalNode node, ServerPlayer player, InteractionHand hand) {
        if (!node.permitted(player) || !RateGate.allow(player, "jimta_monitor", 2)) return;
        var stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) { node.filter(stack); return; }
        var network = DigitalNetwork.find(node, player); tick(node, network); var filter = node.filter();
        if (node.kind().equals("conversion_monitor") && !filter.isEmpty()) {
            if (stack.isEmpty()) {
                int count;
                try (var tx = Transaction.openOuter()) { count = (int) network.extract(filter, filter.getMaxStackSize(), tx); if (count <= 0) return; tx.commit(); }
                var result = filter.copyWithCount(count); player.getInventory().add(result); if (!result.isEmpty()) player.drop(result,false,net.minecraft.util.Prediction.SERVER_ONLY);
            } else if (ItemStack.isSameItemSameComponents(stack, filter)) {
                try (var tx = Transaction.openOuter()) { int count = (int) network.insert(stack, stack.getCount(), tx); if (count > 0) { tx.commit(); stack.shrink(count); } }
            }
        } else player.sendOverlayMessage(net.minecraft.network.chat.Component.literal(filter.isEmpty() ? "Sneak-use an item to select the displayed resource." : node.observed + " stored · ").append(filter.isEmpty() ? net.minecraft.network.chat.Component.empty() : filter.getHoverName()));
    }
}
