package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.finance.LandRegistry;
import io.github.jimbozoomer.jugcraft.finance.Ledger;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;

/** Real chunk claim for unattended automation and mob-AI tests; always removed on close. */
final class NativeTestClaim implements AutoCloseable {
    private final ServerPlayer owner;
    private final GameTestHelper helper;
    private final BlockPos position;
    NativeTestClaim(GameTestHelper helper, BlockPos position) {
        this.helper=helper;this.position=position;owner=helper.makeMockServerPlayerInLevel();
        var book=Ledger.get(helper.getLevel().getServer()).book();
        LandRegistry.get(helper.getLevel().getServer()).assign(book,LandRegistry.keyOf(position),book.account(owner.getUUID(),"JimtaClaimTest"),0);
        helper.assertTrue(LandRegistry.isClaimed(helper.getLevel(),position),"Native claim is active");
    }
    @Override public void close() {
        LandRegistry.get(helper.getLevel().getServer()).abandon(owner,Ledger.get(helper.getLevel().getServer()).book(),LandRegistry.keyOf(position),true,0);
        owner.discard();
    }
}
