package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.nouveau.*;
import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import io.github.jimbozoomer.jugcraft.concordance.spirits.ClockworkPorterEntity.Route;
import io.github.jimbozoomer.jugcraft.concordance.spirits.WorkerRoster;
import io.github.jimbozoomer.jugcraft.concordance.worker.Status;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;

public class StarbuncleGameTests {
    private static final BlockPos FROM=new BlockPos(1,1,1),TO=new BlockPos(4,1,1);
    private static ServerPlayer player(GameTestHelper h){var p=h.makeMockServerPlayerInLevel();p.setGameMode(GameType.SURVIVAL);at(h,p,new BlockPos(2,1,2));RateGate.forget(p.getUUID());return p;}
    private static void at(GameTestHelper h,Entity e,BlockPos pos){var v=Vec3.atBottomCenterOf(h.absolutePos(pos));e.snapTo(v.x,v.y,v.z,0,0);}
    private static ChestBlockEntity chest(GameTestHelper h,BlockPos pos){h.setBlock(pos,Blocks.CHEST);return (ChestBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(pos));}
    private static Starbuncle star(GameTestHelper h,ServerPlayer p){var s=JugNouveau.STARBUNCLE.create(h.getLevel(),EntitySpawnReason.MOB_SUMMONED);s.setOwner(p.getUUID());at(h,s,FROM.south());h.getLevel().addFreshEntity(s);return s;}
    private static Route route(GameTestHelper h){return new Route(h.absolutePos(FROM),h.absolutePos(TO),h.getLevel().dimension().identifier().toString());}
    private static void click(GameTestHelper h,ServerPlayer p,BlockPos relative){RateGate.forget(p.getUUID());var pos=h.absolutePos(relative);UseBlockCallback.EVENT.invoker().interact(p,h.getLevel(),InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false));}
    @GameTest(maxTicks=20)
    public void ordinaryCharmAndWandClicksWithoutResearch(GameTestHelper h){
        var p=player(h);chest(h,FROM);chest(h,TO);h.setBlock(new BlockPos(2,0,2),Blocks.STONE);
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(JugNouveau.STARBUNCLE_CHARM,2));click(h,p,new BlockPos(2,0,2));
        var entries=WorkerRoster.of(h.getLevel().getServer()).workers(p.getUUID());
        h.assertTrue(entries.size()==1&&p.getMainHandItem().getCount()==1,"One ordinary charm click spawns and consumes exactly once");
        var s=(Starbuncle)h.getLevel().getEntity(entries.keySet().iterator().next());
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(JugNouveau.WAND));RateGate.forget(p.getUUID());
        s.interact(p,InteractionHand.MAIN_HAND,Vec3.ZERO);click(h,p,FROM);click(h,p,TO);
        h.assertTrue(route(h).equals(s.route()),"Ordinary entity and chest clicks set a route without a research gate");
        h.assertTrue(io.github.jimbozoomer.jugcraft.nouveau.NouveauRecipes.forStation("enchanting_apparatus").stream().anyMatch(r->io.github.jimbozoomer.jugcraft.nouveau.NouveauRecipes.preview(r,h.getLevel().registryAccess()).is(JugNouveau.STARBUNCLE_CHARM)),"Supplied survival charm recipe loads at the apparatus");h.succeed();
    }
    @GameTest(maxTicks=20)
    public void pickupAndArrivalMoveRealItemsWithComponents(GameTestHelper h){
        var p=player(h);var a=chest(h,FROM);var b=chest(h,TO);var s=star(h,p);var named=new ItemStack(Items.DIAMOND,23);named.set(DataComponents.CUSTOM_NAME,Component.literal("Workshop batch"));a.setItem(0,named);
        h.assertTrue(s.assign(p,route(h)),"Owner assigns route");s.think(h.getLevel());
        h.assertTrue(a.getItem(0).getCount()==7&&s.cargo().getCount()==16&&b.isEmpty(),"Only sixteen removed, destination waits for arrival");
        h.assertTrue(!s.assign(p,route(h)),"Cannot reroute while carrying");
        at(h,s,TO.south());s.think(h.getLevel());
        h.assertTrue(s.cargo().isEmpty()&&b.getItem(0).getCount()==16&&ItemStack.isSameItemSameComponents(named,b.getItem(0)),"Arrival deposits exact original components");
        h.succeed();
    }
    @GameTest(maxTicks=20)
    public void twoCouriersCannotCopyOneSourceStack(GameTestHelper h){
        var p=player(h);var a=chest(h,FROM);chest(h,TO);var one=star(h,p);var two=star(h,p);a.setItem(0,new ItemStack(Items.GOLD_INGOT,20));one.assign(p,route(h));two.assign(p,route(h));
        one.think(h.getLevel());two.think(h.getLevel());
        h.assertTrue(a.isEmpty()&&one.cargo().getCount()+two.cargo().getCount()==20,"Second courier sees the already-reduced real inventory");h.succeed();
    }
    @GameTest(maxTicks=20)
    public void fullAndLockedDestinationPreserveCargo(GameTestHelper h){
        var p=player(h);var a=chest(h,FROM);var b=chest(h,TO);var s=star(h,p);a.setItem(0,new ItemStack(Items.DIAMOND,12));s.assign(p,route(h));s.think(h.getLevel());at(h,s,TO.south());
        for(int i=0;i<27;i++)b.setItem(i,new ItemStack(Items.COBBLESTONE,64));
        h.assertTrue(s.think(h.getLevel())==Status.FULL&&s.cargo().getCount()==12,"Full destination retains cargo");
        var saved=b.saveWithoutMetadata(h.getLevel().registryAccess());var tag=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,h.getLevel().registryAccess());
        new net.minecraft.world.LockCode(net.minecraft.advancements.predicates.ItemPredicate.Builder.item().of(h.getLevel().registryAccess().lookupOrThrow(Registries.ITEM),Items.TRIPWIRE_HOOK).build()).addToTag(tag);
        saved.merge(tag.buildResult());b.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),saved));b.setItem(0,ItemStack.EMPTY);
        h.assertTrue(s.think(h.getLevel())==Status.BLOCKED_BY_ACCESS&&s.cargo().getCount()==12&&b.getItem(0).isEmpty(),"New lock blocks delivery immediately without destroying cargo");h.succeed();
    }
    @GameTest(maxTicks=20)
    public void savedCargoRouteOwnerAndColorSurviveReload(GameTestHelper h){
        var p=player(h);var a=chest(h,FROM);var b=chest(h,TO);var s=star(h,p);a.setItem(0,new ItemStack(Items.AMETHYST_SHARD,9));s.assign(p,route(h));s.color(15);s.think(h.getLevel());
        var out=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,h.getLevel().registryAccess());s.saveWithoutId(out);
        var copy=JugNouveau.STARBUNCLE.create(h.getLevel(),EntitySpawnReason.LOAD);copy.load(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),out.buildResult()));
        h.assertTrue(copy.cargo().getCount()==9&&copy.color()==15&&copy.owner().equals(p.getUUID())&&copy.route().equals(route(h)),"Cargo and job persist independently of brain state");
        s.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);at(h,copy,TO.south());copy.think(h.getLevel());
        h.assertTrue(b.getItem(0).getCount()==9&&copy.cargo().isEmpty(),"Reloaded worker finishes its trip once");h.succeed();
    }
    @GameTest(maxTicks=20)
    public void ownershipOfflineAndInvalidRoutesDoNotTransfer(GameTestHelper h){
        var p=player(h);var other=player(h);var a=chest(h,FROM);chest(h,TO);var s=star(h,p);a.setItem(0,new ItemStack(Items.DIAMOND,3));
        h.assertTrue(!s.assign(other,route(h)),"Outsider cannot assign");
        h.assertTrue(!s.assign(p,new Route(h.absolutePos(FROM),h.absolutePos(FROM),route(h).dimension())),"Same endpoint refused");
        h.assertTrue(!s.assign(p,new Route(h.absolutePos(FROM),h.absolutePos(TO.offset(40,0,0)),route(h).dimension())),"Long route refused");
        h.setBlock(TO,Blocks.FURNACE);h.assertTrue(!s.assign(p,route(h)),"Machine slots excluded");chest(h,TO);s.assign(p,route(h));
        s.setOwner(java.util.UUID.randomUUID());h.assertTrue(s.think(h.getLevel())==Status.OWNER_OFFLINE&&a.getItem(0).getCount()==3,"Offline owner does no work by default");
        s.setOwner(p.getUUID());p.setGameMode(GameType.SPECTATOR);h.assertTrue(s.think(h.getLevel())==Status.BLOCKED_BY_ACCESS&&a.getItem(0).getCount()==3,"Spectator cannot indirectly withdraw");h.succeed();
    }
    @GameTest(maxTicks=20)
    public void packingReturnsCargoAndColoredCharmOnce(GameTestHelper h){
        var p=player(h);var a=chest(h,FROM);chest(h,TO);var s=star(h,p);a.setItem(0,new ItemStack(Items.DIAMOND,11));s.assign(p,route(h));s.think(h.getLevel());s.color(6);
        var box=s.getBoundingBox().inflate(3);p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.setShiftKeyDown(true);s.interact(p,InteractionHand.MAIN_HAND,Vec3.ZERO);s.discard();
        var drops=h.getLevel().getEntitiesOfClass(ItemEntity.class,box);int diamonds=0,charms=0;
        for(var drop:drops){var stack=drop.getItem();if(stack.is(Items.DIAMOND))diamonds+=stack.getCount();if(stack.is(JugNouveau.STARBUNCLE_CHARM)){charms+=stack.getCount();h.assertTrue(stack.getOrDefault(JugNouveau.CHARM_COLOR,4)==6,"Charm retains pink color");}}
        h.assertTrue(diamonds==11&&charms==1&&s.cargo().isEmpty(),"Recovery drops actual cargo and charm once");h.succeed();
    }
    @GameTest(maxTicks=20)
    public void summoningStopsAtSavedWorkerLimit(GameTestHelper h){
        var p=player(h);var roster=WorkerRoster.of(h.getLevel().getServer());for(int i=0;i<8;i++)roster.note(p.getUUID(),java.util.UUID.randomUUID(),"starbuncle",Status.IDLE,route(h).dimension(),h.absolutePos(FROM));
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(JugNouveau.STARBUNCLE_CHARM));click(h,p,new BlockPos(2,0,2));
        h.assertTrue(p.getMainHandItem().getCount()==1&&roster.workers(p.getUUID()).size()==8,"Unloaded workers count and a refused summon spends nothing");h.succeed();
    }
}
