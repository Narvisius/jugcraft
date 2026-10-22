package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.server.*;
import eu.pb4.trinkets.api.TrinketsApi;
import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.gamerules.GameRules;

public final class PlayerRulesGameTests {
    @GameTest
    public void personalRulesPersistAndStayPerUuid(GameTestHelper h) {
        var a=UUID.randomUUID();var b=UUID.randomUUID();var rules=new PlayerRules();rules.set(a,true);
        var restored=PlayerRules.CODEC.parse(NbtOps.INSTANCE,PlayerRules.CODEC.encodeStart(NbtOps.INSTANCE,rules).getOrThrow()).getOrThrow();
        h.assertTrue(restored.keeps(a)&&!restored.keeps(b),"Exactly the chosen UUID survives serialization");
        restored.set(a,false);h.assertFalse(restored.keeps(a),"Admin can revoke the personal override");h.succeed();
    }
    @GameTest
    public void deathAndRespawnKeepOnlyTheSelectedPlayersInventory(GameTestHelper h) {
        var level=h.getLevel();var rules=PlayerRules.get(level.getServer());
        h.assertFalse(level.getGameRules().get(GameRules.KEEP_INVENTORY),"Fixture uses normal inventory drops globally");
        for(boolean keep:new boolean[]{true,false}) {
            var p=h.makeMockServerPlayerInLevel();p.setGameMode(GameType.SURVIVAL);p.getInventory().clearContent();
            p.snapTo(net.minecraft.world.phys.Vec3.atCenterOf(h.absolutePos(new BlockPos(1,2,1))));
            p.getInventory().setItem(0,new ItemStack(Items.DIAMOND,7));p.setItemSlot(EquipmentSlot.HEAD,new ItemStack(Items.IRON_HELMET));
            p.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(Items.SHIELD));p.giveExperiencePoints(100);
            // The focus slot belongs to Jugcraft; the back slot exists only when
            // the optional backpack is installed. Exercise both when available.
            var slots=new ArrayList<String>();slots.add("chest/spell_focus");
            if(TrinketsApi.getAttachment(p).getInventory("chest/back")!=null)slots.add("chest/back");
            for(var slot:slots) {
                var inventory=TrinketsApi.getAttachment(p).getInventory(slot);
                h.assertTrue(inventory!=null,"Core accessory slot exists without optional backpack mods");
                inventory.setItem(0,new ItemStack(Items.EMERALD));
            }
            rules.set(p.getUUID(),keep);p.setHealth(0);p.die(level.damageSources().genericKill());
            h.assertTrue(p.getInventory().countItem(Items.DIAMOND)==(keep?7:0),"Death drops only unflagged inventory");
            for(var slot:slots)h.assertTrue(TrinketsApi.getAttachment(p).getInventory(slot).getItem(0).getCount()==(keep?1:0),"Accessory/backpack slot follows the personal rule");
            var respawn=new net.minecraft.server.level.ServerPlayer(level.getServer(),level,p.getGameProfile(),net.minecraft.server.level.ClientInformation.createDefault());
            respawn.connection=p.connection;
            respawn.restoreFrom(p,false);
            for(var slot:slots)h.assertTrue(TrinketsApi.getAttachment(respawn).getInventory(slot).getItem(0).getCount()==(keep?1:0),"Worn slot is copied on respawn only when retained");
            h.assertTrue(respawn.getInventory().countItem(Items.DIAMOND)==(keep?7:0),"Native respawn restores exactly seven diamonds without duplication");
            h.assertTrue(respawn.getItemBySlot(EquipmentSlot.HEAD).isEmpty()!=keep&&respawn.getOffhandItem().isEmpty()!=keep,"Armour and offhand survive only for the selected player");
            h.assertTrue(respawn.totalExperience==(keep?100:0),"XP follows native keep-inventory semantics");
            rules.set(p.getUUID(),false);
        }
        h.assertFalse(level.getGameRules().get(GameRules.KEEP_INVENTORY),"No global gamerule mutation");h.succeed();
    }
    @GameTest
    public void keepInventoryCommandRequiresAnOperator(GameTestHelper h) {
        var p=h.makeMockServerPlayerInLevel();var dispatcher=h.getLevel().getServer().getCommands().getDispatcher();
        var personal=p.createCommandSourceStack();
        var node=dispatcher.getRoot().getChild("jugcraft").getChild("keepinventory");
        h.assertFalse(node.canUse(personal),"Ordinary players cannot grant inventory immunity");
        h.assertTrue(node.canUse(h.getLevel().getServer().createCommandSourceStack()),"Console/admin can manage personal rules");h.succeed();
    }
    @GameTest(maxTicks=60)
    public void crouchAxeFellsARootedTreeWithNormalDurability(GameTestHelper h) {
        var l=h.getLevel();var root=h.absolutePos(new BlockPos(1,1,1));
        l.setBlockAndUpdate(root.below(),Blocks.DIRT.defaultBlockState());
        for(int y=0;y<5;y++)l.setBlockAndUpdate(root.above(y),Blocks.OAK_LOG.defaultBlockState());
        for(var d:net.minecraft.core.Direction.Plane.HORIZONTAL)l.setBlockAndUpdate(root.above(4).relative(d),Blocks.OAK_LEAVES.defaultBlockState());
        var p=h.makeMockServerPlayerInLevel();p.setGameMode(GameType.SURVIVAL);p.snapTo(net.minecraft.world.phys.Vec3.atCenterOf(root).add(2,0,0));p.setShiftKeyDown(true);
        var axe=new ItemStack(Items.IRON_AXE);p.setItemSlot(EquipmentSlot.MAINHAND,axe);
        h.assertTrue(p.gameMode.destroyBlock(root),"Initial real player break succeeds");
        h.runAfterDelay(8,()->{
            for(int y=0;y<5;y++)h.assertTrue(l.getBlockState(root.above(y)).isAir(),"Queued felling removes the connected trunk");
            h.assertTrue(axe.getDamageValue()==5,"One normal durability cost for each log");
            h.assertTrue(l.getBlockState(root.below()).is(Blocks.DIRT),"Soil is untouched");h.succeed();
        });
    }
    @GameTest
    public void treeDiscoveryRejectsBuildingsAndPersistentCanopies(GameTestHelper h) {
        var l=h.getLevel();var root=h.absolutePos(new BlockPos(1,1,1));l.setBlockAndUpdate(root.below(),Blocks.STONE.defaultBlockState());
        for(int y=1;y<5;y++)l.setBlockAndUpdate(root.above(y),Blocks.OAK_LOG.defaultBlockState());
        for(var d:net.minecraft.core.Direction.Plane.HORIZONTAL)l.setBlockAndUpdate(root.above(4).relative(d),Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT,true));
        h.assertTrue(TreeFelling.discover(l,root,Blocks.OAK_LOG).isEmpty(),"Timber on stone is not a tree");
        l.setBlockAndUpdate(root.below(),Blocks.DIRT.defaultBlockState());
        h.assertTrue(TreeFelling.discover(l,root,Blocks.OAK_LOG).isEmpty(),"Player-placed canopy is not a living tree");h.succeed();
    }
    @GameTest(maxTicks=60)
    public void fellingStopsWhenTheAxeBreaks(GameTestHelper h) {
        var l=h.getLevel();var root=h.absolutePos(new BlockPos(1,1,1));
        l.setBlockAndUpdate(root.below(),Blocks.DIRT.defaultBlockState());
        for(int y=0;y<5;y++)l.setBlockAndUpdate(root.above(y),Blocks.OAK_LOG.defaultBlockState());
        for(var d:net.minecraft.core.Direction.Plane.HORIZONTAL)l.setBlockAndUpdate(root.above(4).relative(d),Blocks.OAK_LEAVES.defaultBlockState());
        var p=h.makeMockServerPlayerInLevel();p.setGameMode(GameType.SURVIVAL);p.snapTo(net.minecraft.world.phys.Vec3.atCenterOf(root).add(2,0,0));p.setShiftKeyDown(true);
        var axe=new ItemStack(Items.IRON_AXE);axe.setDamageValue(axe.getMaxDamage()-2);p.setItemSlot(EquipmentSlot.MAINHAND,axe);
        h.assertTrue(p.gameMode.destroyBlock(root),"Initial chop succeeds");
        h.runAfterDelay(8,()->{
            h.assertTrue(l.getBlockState(root.above()).isAir()&&l.getBlockState(root.above(2)).is(Blocks.OAK_LOG),"Only the two paid-for logs break");
            h.assertTrue(p.getMainHandItem().isEmpty(),"Broken axe stops the queued job");h.succeed();
        });
    }
}
