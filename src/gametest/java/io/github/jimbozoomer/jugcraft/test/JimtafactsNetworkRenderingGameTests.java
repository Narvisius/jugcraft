package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.jimtafacts.network.*;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.*;
import net.minecraft.world.level.storage.TagValueInput;

/** Visibility must not reveal saved storage cells, their contents, filters or ownership. */
public final class JimtafactsNetworkRenderingGameTests {
    @GameTest public void driveRenderPacketOnlyContainsAppearance(GameTestHelper h) {
        var pos = new BlockPos(2, 2, 2); h.setBlock(pos, DigitalStorage.BLOCKS.get("drive"));
        var node = h.getBlockEntity(pos, DigitalNode.class); var owner = UUID.randomUUID(); node.owner(owner);
        var cell = new ItemStack(DigitalStorage.ITEMS.get("item_storage_cell_1k"));
        int capacity = ((DigitalStorage.CellItem) cell.getItem()).capacity;
        cell.set(DigitalStorage.CONTENTS, new CellContents(List.of(new CellContents.Entry(new ItemStack(Items.DIAMOND), capacity))));
        node.setItem(0, cell); node.filter(new ItemStack(Items.NETHERITE_INGOT));
        var tag = node.getUpdateTag(h.getLevel().registryAccess());
        String serialized = tag.toString();
        h.assertTrue(!tag.contains("Items") && !tag.contains("owner") && !tag.contains("filter") && !tag.contains("orders"), "Render packet omits private machine data");
        h.assertTrue(!serialized.contains("diamond") && !serialized.contains("netherite") && !serialized.contains(owner.toString()), "Render packet reveals no stored item identities or owner");
        var clientCopy = new DigitalNode(h.absolutePos(pos), node.getBlockState());
        clientCopy.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, h.getLevel().registryAccess(), tag));
        h.assertTrue(clientCopy.visibleCell(0) == 1 && clientCopy.visibleCellStatus(0) == 4, "Client gets original 1k chassis and full-state LED only");
        h.assertTrue(clientCopy.isEmpty(), "Render packet cannot populate a playable inventory");
        node.setItem(0, ItemStack.EMPTY); var removed = node.getUpdateTag(h.getLevel().registryAccess());
        clientCopy.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, h.getLevel().registryAccess(), removed));
        h.assertTrue(clientCopy.visibleCell(0) == 0, "Removed cell disappears from its bay"); h.succeed();
    }
    @GameTest public void exposedWorkshopItemsStripPrivateComponents(GameTestHelper h) {
        var pos = new BlockPos(2, 2, 2); h.setBlock(pos, DigitalStorage.BLOCKS.get("charger"));
        var node = h.getBlockEntity(pos, DigitalNode.class);
        var item = new ItemStack(DigitalStorage.ITEMS.get("certus_quartz_crystal"));
        item.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("PRIVATE_TEST_NOTE"));
        node.setItem(0, item);
        var packet = node.getUpdateTag(h.getLevel().registryAccess());
        h.assertTrue(!packet.toString().contains("PRIVATE_TEST_NOTE"), "Visible charger item loses custom components");
        var client = new DigitalNode(h.absolutePos(pos), node.getBlockState());
        client.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, h.getLevel().registryAccess(), packet));
        h.assertTrue(client.visibleMiddle().is(item.getItem()) && client.visibleMiddle().getCount() == 1, "Original item model still has a bare identity to render"); h.succeed();
    }
}
