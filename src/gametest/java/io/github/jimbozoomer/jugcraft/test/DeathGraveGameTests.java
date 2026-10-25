package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.grave.*;
import io.github.jimbozoomer.jugcraft.server.PlayerRules;
import eu.pb4.trinkets.api.TrinketsApi;
import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.Vec3;

public class DeathGraveGameTests {
    static ServerPlayer player(GameTestHelper h) {
        var p = h.makeMockServerPlayerInLevel();
        p.setGameMode(GameType.SURVIVAL); p.getInventory().clearContent();
        p.snapTo(Vec3.atCenterOf(h.absolutePos(new BlockPos(2, 2, 2))));
        return p;
    }
    static GraveStore.Grave kill(GameTestHelper h, ServerPlayer p) {
        p.setHealth(0); p.die(h.getLevel().damageSources().genericKill());
        var graves = GraveStore.get(h.getLevel().getServer()).owned(p.getUUID());
        h.assertTrue(graves.size() == 1, "Exactly one grave per death");
        return graves.getFirst();
    }
    static void clear(GameTestHelper h, GraveStore.Grave grave) {
        GraveStore.get(h.getLevel().getServer()).remove(grave.id());
        if (h.getLevel().getBlockState(grave.location().pos()).is(DeathGraves.MARKER)) h.getLevel().removeBlock(grave.location().pos(), false);
    }

    @GameTest
    public void nativeDeathStoresInventoryArmorOffhandAndAccessories(GameTestHelper h) {
        var p = player(h);
        p.getInventory().setItem(0, new ItemStack(Items.DIAMOND, 7));
        p.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        p.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
        TrinketsApi.getAttachment(p).getInventory("chest/spell_focus").setItem(0, new ItemStack(Items.EMERALD, 1));
        var grave = kill(h, p);
        h.assertTrue(grave.count() == 10, "All ten items, including accessory, stored exactly once");
        h.assertTrue(p.getInventory().isEmpty(), "Native death clears the inventory");
        h.assertTrue(TrinketsApi.getAttachment(p).getInventory("chest/spell_focus").isEmpty(), "Accessory cleared after capture");
        h.assertTrue(grave.marked() && h.getLevel().getBlockState(grave.location().pos()).is(DeathGraves.MARKER), "Supplied grave marker placed");
        var stranger = player(h);
        h.assertTrue(DeathGraves.recover(stranger, grave.id()) == 0, "Another player cannot steal the grave");
        h.assertTrue(DeathGraves.recover(p, grave.id()) == 0, "Cannot recover into a dead player before respawn");
        p.setHealth(20);
        h.assertTrue(DeathGraves.recover(p, grave.id()) == 10, "Owner receives exactly ten items");
        h.assertTrue(DeathGraves.recover(p, grave.id()) == 0, "Second click cannot duplicate contents");
        h.assertTrue(GraveStore.get(h.getLevel().getServer()).get(grave.id()) == null, "Empty grave removed");
        h.succeed();
    }

    @GameTest
    public void fullInventoryKeepsRemainderAndRecordsRoundTrip(GameTestHelper h) {
        var p = player(h); p.getInventory().setItem(0, new ItemStack(Items.DIAMOND, 7));
        var grave = kill(h, p);
        var ops = h.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE);
        var store = GraveStore.get(h.getLevel().getServer());
        var restored = GraveStore.CODEC.parse(ops, GraveStore.CODEC.encodeStart(ops, store).getOrThrow()).getOrThrow();
        h.assertTrue(restored.get(grave.id()).count() == 7 && restored.get(grave.id()).owner().equals(p.getUUID()), "Contents and owner survive saved-data serialization");
        p.setHealth(20);
        for (int i = 0; i < 36; i++) p.getInventory().setItem(i, new ItemStack(Items.COBBLESTONE, 64));
        h.assertTrue(DeathGraves.recover(p, grave.id()) == 0, "Full inventory does not drop stored valuables on the ground");
        h.assertTrue(store.get(grave.id()).count() == 7, "All remaining items stay safe");
        p.getInventory().setItem(0, ItemStack.EMPTY);
        h.assertTrue(DeathGraves.recover(p, grave.id()) == 7, "Recover later after making room"); h.succeed();
    }

    @GameTest
    public void keepInventoryEmptyDeathAndOrdinaryDropsMakeNoGrave(GameTestHelper h) {
        var p = player(h); var store = GraveStore.get(h.getLevel().getServer());
        p.drop(new ItemStack(Items.STONE), false, net.minecraft.util.Prediction.SERVER_ONLY);
        h.assertTrue(store.owned(p.getUUID()).isEmpty(), "Normal dropping is not captured");
        PlayerRules.get(h.getLevel().getServer()).set(p.getUUID(), true);
        p.getInventory().setItem(0, new ItemStack(Items.DIAMOND, 7));
        p.setHealth(0); p.die(h.getLevel().damageSources().genericKill());
        h.assertTrue(p.getInventory().countItem(Items.DIAMOND) == 7 && store.owned(p.getUUID()).isEmpty(), "Personal keep-inventory produces no grave");
        PlayerRules.get(h.getLevel().getServer()).set(p.getUUID(), false);
        var empty = player(h); empty.setHealth(0); empty.die(h.getLevel().damageSources().genericKill());
        h.assertTrue(store.owned(empty.getUUID()).isEmpty(), "Empty death makes no empty marker");
        h.succeed();
    }

    @GameTest
    public void globalKeepRuleAndTotemDoNotLeaveGraves(GameTestHelper h) {
        var rules=h.getLevel().getGameRules();boolean old=rules.get(GameRules.KEEP_INVENTORY);
        try {
            rules.set(GameRules.KEEP_INVENTORY,true,h.getLevel().getServer());
            var p=player(h);p.getInventory().setItem(0,new ItemStack(Items.DIAMOND,3));
            p.setHealth(0);p.die(h.getLevel().damageSources().genericKill());
            h.assertTrue(p.getInventory().countItem(Items.DIAMOND)==3&&GraveStore.get(h.getLevel().getServer()).owned(p.getUUID()).isEmpty(),"Global keep rule leaves inventory alone");
        } finally {rules.set(GameRules.KEEP_INVENTORY,old,h.getLevel().getServer());}
        var p=player(h);p.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(Items.TOTEM_OF_UNDYING));p.setHealth(1);
        p.hurtServer(h.getLevel(),h.getLevel().damageSources().generic(),100);
        h.assertTrue(p.isAlive()&&GraveStore.get(h.getLevel().getServer()).owned(p.getUUID()).isEmpty(),"A totem save is not a grave-producing death");h.succeed();
    }

    @GameTest
    public void removedMarkerDoesNotDestroyBelongings(GameTestHelper h) {
        var p = player(h); p.getInventory().setItem(0, new ItemStack(Items.DIAMOND, 7));
        var grave = kill(h, p);
        h.getLevel().setBlockAndUpdate(grave.location().pos(), Blocks.STONE.defaultBlockState());
        p.setHealth(20);
        h.assertTrue(DeathGraves.recover(p, grave.id()) == 7, "Recovery works after administrative marker replacement");
        h.assertTrue(h.getLevel().getBlockState(grave.location().pos()).is(Blocks.STONE), "Recovery never removes the replacement block");
        h.succeed();
    }
}
