package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.nouveau.*;
import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.Vec3;

/** Real chest inventories, permissions, capacity loss and saving; no research granted by fixtures. */
public class JugNouveauGameTests {
    private static final BlockPos HOME=new BlockPos(1,1,1),CHEST=new BlockPos(3,1,1);
    private static ServerPlayer player(GameTestHelper h){
        var p=h.makeMockServerPlayerInLevel();p.setGameMode(GameType.SURVIVAL);
        var at=h.absoluteVec(new Vec3(1.5,1,2.5));p.snapTo(at.x,at.y,at.z,0,0);RateGate.forget(p.getUUID());return p;
    }
    private static StorageLectern home(GameTestHelper h,ServerPlayer p){
        h.setBlock(HOME,JugNouveau.LECTERN);var be=(StorageLectern)h.getLevel().getBlockEntity(h.absolutePos(HOME));be.placedBy(p);return be;
    }
    private static ChestBlockEntity chest(GameTestHelper h){h.setBlock(CHEST,Blocks.CHEST);return (ChestBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(CHEST));}
    @GameTest(maxTicks=20)
    public void survivalRecipesAndOrdinaryRightClick(GameTestHelper h){
        for(String id:java.util.List.of("bookwyrm_lectern","bookwyrm_charm","dominion_wand"))
            h.assertTrue(h.getLevel().recipeAccess().byKey(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE,
                io.github.jimbozoomer.jugcraft.Jugcraft.id(id))).isPresent(),"Survival recipe must actually load: "+id);
        var p=player(h);var be=home(h,p);chest(h);
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(JugNouveau.CHARM,2));
        click(h,p,HOME);
        h.assertTrue(be.wyrms().size()==1&&p.getMainHandItem().getCount()==1,"Ordinary charm click binds exactly once and spends one charm");
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(JugNouveau.WAND));
        RateGate.forget(p.getUUID());click(h,p,HOME);RateGate.forget(p.getUUID());click(h,p,CHEST);
        h.assertTrue(be.links().equals(java.util.List.of(h.absolutePos(CHEST))),"Wand intercepts an ordinary chest click before chest opening");h.succeed();
    }
    private static void click(GameTestHelper h,ServerPlayer p,BlockPos relative){
        var pos=h.absolutePos(relative);
        net.fabricmc.fabric.api.event.player.UseBlockCallback.EVENT.invoker().interact(p,h.getLevel(),net.minecraft.world.InteractionHand.MAIN_HAND,
            new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(pos),net.minecraft.core.Direction.UP,pos,false));
    }
    @GameTest(maxTicks=20)
    public void lockingAConnectedChestRevokesAccess(GameTestHelper h){
        var p=player(h);var be=home(h,p);var chest=chest(h);be.bind(p);be.link(p,h.absolutePos(CHEST));chest.setItem(0,new ItemStack(Items.DIAMOND,7));
        var menu=new StorageLecternMenu(1,p.getInventory(),be);
        var lock=new net.minecraft.world.LockCode(net.minecraft.advancements.predicates.ItemPredicate.Builder.item()
            .of(h.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ITEM),Items.TRIPWIRE_HOOK).build());
        // Apply a lock as a saved block update. Item-placement components would also
        // replace the container contents, which is not the operation under test.
        var saved=chest.saveWithoutMetadata(h.getLevel().registryAccess());
        var lockTag=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,h.getLevel().registryAccess());
        lock.addToTag(lockTag);saved.merge(lockTag.buildResult());
        chest.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),saved));
        h.assertTrue(chest.isLocked(),"Fixture chest really is locked");
        h.assertTrue(menu.slots.get(0).getItem().isEmpty()&&menu.quickMoveStack(p,0).isEmpty(),"A new lock takes effect on an already-open view");
        h.assertTrue(chest.getItem(0).getCount()==7&&p.getInventory().countItem(Items.DIAMOND)==0,"Locked contents stay untouched");h.succeed();
    }
    @GameTest(maxTicks=20)
    public void ownershipAndBindingCapacity(GameTestHelper h){
        var p=player(h);var be=home(h,p);chest(h);
        h.assertTrue(!be.link(p,h.absolutePos(CHEST)),"A chest needs a bound Book Wyrm");
        h.assertTrue(be.bind(p)&&be.capacity()==8,"A fresh player can bind a wyrm without research");
        h.assertTrue(be.link(p,h.absolutePos(CHEST)),"Owner can link chest");
        h.assertTrue(!be.link(p,h.absolutePos(CHEST)),"Duplicate chest link refused");
        var other=player(h);h.assertTrue(!be.mayUse(other)&&!be.link(other,h.absolutePos(CHEST)),"Another player cannot take over a lectern");
        for(int i=1;i<4;i++)h.assertTrue(be.bind(p),"Four Book Wyrms fit");
        h.assertTrue(!be.bind(p)&&be.capacity()==32,"Fifth wyrm rejected");h.succeed();
    }
    @GameTest(maxTicks=20)
    public void realStorageHasNoDuplicateBackingInventory(GameTestHelper h){
        var p=player(h);var be=home(h,p);var chest=chest(h);be.bind(p);be.link(p,h.absolutePos(CHEST));chest.setItem(0,new ItemStack(Items.DIAMOND,16));
        var a=new StorageLecternMenu(1,p.getInventory(),be);var b=new StorageLecternMenu(2,p.getInventory(),be);
        h.assertTrue(a.slots.get(0).getItem().getCount()==16,"The menu sees the real chest");
        a.quickMoveStack(p,0);
        h.assertTrue(chest.getItem(0).isEmpty()&&b.slots.get(0).getItem().isEmpty(),"Second viewer cannot withdraw a stale copy");
        h.assertTrue(p.getInventory().countItem(Items.DIAMOND)==16,"Exactly sixteen diamonds transferred");
        b.quickMoveStack(p,0);h.assertTrue(p.getInventory().countItem(Items.DIAMOND)==16,"Repeated withdrawal creates nothing");h.succeed();
    }
    @GameTest(maxTicks=20)
    public void unlinkAndMissingChestLeaveItemsSafe(GameTestHelper h){
        var p=player(h);var be=home(h,p);var chest=chest(h);be.bind(p);be.link(p,h.absolutePos(CHEST));chest.setItem(0,new ItemStack(Items.GOLD_INGOT,9));
        var menu=new StorageLecternMenu(1,p.getInventory(),be);
        be.unlink(h.absolutePos(CHEST));
        h.assertTrue(menu.slots.get(0).getItem().isEmpty()&&chest.getItem(0).getCount()==9,"Unlinking never consumes the chest's contents");
        be.link(p,h.absolutePos(CHEST));be.detach(be.wyrms().getFirst());
        h.assertTrue(menu.slots.get(0).getItem().isEmpty()&&chest.getItem(0).getCount()==9,"Loss of a wyrm disables excess links without deleting items");
        h.succeed();
    }
    @GameTest(maxTicks=20)
    public void noMachineOrLongDistanceLinks(GameTestHelper h){
        var p=player(h);var be=home(h,p);be.bind(p);h.setBlock(CHEST,Blocks.FURNACE);
        h.assertTrue(!be.link(p,h.absolutePos(CHEST)),"A furnace is not an ordinary storage chest");
        h.assertTrue(!be.link(p,h.absolutePos(HOME.offset(18,0,0))),"Distant links are rejected without loading chunks");
        p.setGameMode(GameType.SPECTATOR);h.assertTrue(!be.mayUse(p),"Spectators cannot mutate the network");h.succeed();
    }
    @GameTest(maxTicks=20)
    public void linksAndColorsSurviveSave(GameTestHelper h){
        var p=player(h);var be=home(h,p);chest(h);be.bind(p);be.link(p,h.absolutePos(CHEST));
        var saved=be.saveWithoutMetadata(h.getLevel().registryAccess());
        var copy=new StorageLectern(be.getBlockPos(),be.getBlockState());
        copy.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),saved));
        h.assertTrue(copy.links().equals(be.links())&&copy.wyrms().equals(be.wyrms())&&p.getUUID().equals(copy.owner()),"Links, identities and owner survive saving");
        var wyrm=(BookWyrm)h.getLevel().getEntity(be.wyrms().getFirst());wyrm.color(5);
        var output=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,h.getLevel().registryAccess());wyrm.saveWithoutId(output);
        var restored=JugNouveau.BOOK_WYRM.create(h.getLevel(),EntitySpawnReason.LOAD);restored.load(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),output.buildResult()));
        h.assertTrue(restored.color()==5&&be.getBlockPos().equals(restored.home()),"White Book Wyrm keeps its home and appearance");h.succeed();
    }
    @GameTest(maxTicks=20)
    public void remoteMenuStopsWhenLecternRemovedOrPlayerLeaves(GameTestHelper h){
        var p=player(h);var be=home(h,p);var chest=chest(h);be.bind(p);be.link(p,h.absolutePos(CHEST));
        var menu=new StorageLecternMenu(1,p.getInventory(),be);h.assertTrue(menu.stillValid(p),"Owner standing at lectern can use it");
        p.snapTo(p.getX()+20,p.getY(),p.getZ(),0,0);h.assertTrue(!menu.stillValid(p),"Walking away closes access");
        var at=h.absoluteVec(new Vec3(1.5,1,2.5));p.snapTo(at.x,at.y,at.z,0,0);
        h.setBlock(HOME,Blocks.AIR);h.assertTrue(!menu.stillValid(p),"Removing the lectern closes access");
        h.assertTrue(h.getLevel().getBlockEntity(h.absolutePos(CHEST))==chest,"Removing the lectern leaves the chest");h.succeed();
    }
}
