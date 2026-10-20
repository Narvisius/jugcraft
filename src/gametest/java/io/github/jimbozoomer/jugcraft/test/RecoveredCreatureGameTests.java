package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.menagerie.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;

/** Integration checks for recovered bodies and the newly supplied undead family. */
public final class RecoveredCreatureGameTests {
    @GameTest
    public void channelledRedirectsExistingLightningOnlyUnderOpenSky(GameTestHelper h) throws Exception {
        var player=h.makeMockServerPlayerInLevel();
        var at=h.absolutePos(new BlockPos(2,30,2));
        player.setPos(Vec3.atCenterOf(at));
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(RottenCreatures.CHANNELLED,200));
        var method=net.minecraft.server.level.ServerLevel.class.getDeclaredMethod("findLightningTargetAround",BlockPos.class);
        method.setAccessible(true);
        h.assertTrue(h.getLevel().canSeeSky(at),"Lightning fixture has open sky");
        h.assertTrue(method.invoke(h.getLevel(),at.offset(4,0,0)).equals(at),"Channelled attracts a nearby existing strike");
        h.getLevel().setBlock(at.above(3),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);
        // The skylight engine updates asynchronously after block placement.
        h.runAfterDelay(20,()->{
            try{
                h.assertTrue(!h.getLevel().canSeeSky(at),"Roof fixture has finished updating skylight");
                h.assertTrue(!method.invoke(h.getLevel(),at.offset(4,0,0)).equals(at),"Roof protects a channelled player");
                h.succeed();
            }catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
            finally{
                h.getLevel().setBlock(at.above(3),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3);
                h.getLevel().getServer().getPlayerList().remove(player);
            }
        });
    }
    @GameTest
    public void suppliedLootUsesNativeCountModifiers(GameTestHelper h) {
        for(String kind:new String[]{"burned","frostbitten"}){
            var animal=Menagerie.TYPES.get("rotten_"+kind).create(h.getLevel(),EntitySpawnReason.COMMAND);
            var key=net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,
                io.github.jimbozoomer.jugcraft.Jugcraft.id("entities/rotten_"+kind));
            var table=h.getLevel().getServer().reloadableRegistries().getLootTable(key);
            h.assertTrue(table!=net.minecraft.world.level.storage.loot.LootTable.EMPTY,"Supplied loot decoded: "+kind);
            var params=new net.minecraft.world.level.storage.loot.LootParams.Builder(h.getLevel())
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,animal.position())
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.THIS_ENTITY,animal)
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.DAMAGE_SOURCE,h.getLevel().damageSources().generic())
                .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.ENTITY);
            var item=Menagerie.ITEMS.get("rotten_"+(kind.equals("burned")?"magma":"frozen")+"_rotten_flesh");
            java.util.Set<Integer> counts=new java.util.HashSet<>();
            for(long seed=1;seed<=64;seed++){
                int count=table.getRandomItems(params,seed).stream().filter(stack->stack.is(item)).mapToInt(ItemStack::getCount).sum();
                h.assertTrue(count>=0&&count<=2,"Original flesh range remains 0..2");counts.add(count);
            }
            h.assertTrue(counts.contains(0)&&counts.contains(2),"Count modifier is active, not ignored as a legacy key: "+kind);
        }
        h.succeed();
    }

    @GameTest
    public void ferrouslimeHeadsResizeAndBoundHealth(GameTestHelper h) {
        var animal=Menagerie.TYPES.get("cavern_ferrouslime").create(h.getLevel(),EntitySpawnReason.COMMAND);
        h.assertTrue(animal instanceof RecoveredFerrouslime,"Native head-count slime factory");
        var slime=(RecoveredFerrouslime)animal;
        float singleWidth=slime.getBbWidth();
        slime.heads(4);
        h.assertTrue(slime.heads()==4&&slime.getMaxHealth()==40,"Original health scales with heads");
        h.assertTrue(slime.getBbWidth()>singleWidth,"Collision expands with visible gel body");
        slime.heads(100);
        h.assertTrue(slime.heads()==16&&slime.getMaxHealth()==100,"Merge size and health stay bounded");
        slime.heads(0);
        h.assertTrue(slime.heads()==1&&slime.getMaxHealth()==10,"Malformed saved head count cannot create empty slime");
        h.succeed();
    }

    @GameTest(maxTicks=50)
    public void segmentsShareParentHealthAndDisappearWithParent(GameTestHelper h) {
        var animal=Menagerie.TYPES.get("beast_anaconda").create(h.getLevel(),EntitySpawnReason.COMMAND);
        animal.setPos(Vec3.atCenterOf(h.absolutePos(new BlockPos(3,2,3))));
        animal.setNoAi(true);animal.setNoGravity(true);h.getLevel().addFreshEntity(animal);
        h.runAfterDelay(5,()->{
            var parts=h.getLevel().getEntitiesOfClass(RecoveredCreaturePart.class,animal.getBoundingBox().inflate(16),p->p.parent()==animal);
            h.assertTrue(parts.size()==7,"Seven original neck/body/tail segments, not duplicate animals");
            float before=animal.getHealth();
            h.assertTrue(parts.getFirst().hurtServer(h.getLevel(),h.getLevel().damageSources().generic(),2),"Segment forwards damage");
            h.assertTrue(animal.getHealth()<before,"Only parent health is authoritative");
            animal.discard();
            h.runAfterDelay(3,()->{h.assertTrue(parts.stream().allMatch(Entity::isRemoved),"Removing parent cleans up every segment");h.succeed();});
        });
    }

    @GameTest(maxTicks=40)
    public void recoveredActionsExpireInsteadOfLoopingForever(GameTestHelper h) {
        var animal=Menagerie.TYPES.get("mutant_mutant_zombie").create(h.getLevel(),EntitySpawnReason.COMMAND);
        animal.setNoAi(true);animal.setPos(Vec3.atCenterOf(h.absolutePos(new BlockPos(2,2,2))));
        h.getLevel().addFreshEntity(animal);animal.recoveredAction("slam_ground");
        h.assertTrue(animal.recoveredActionTime(0)==0,"Action starts on server game time");
        h.runAfterDelay(26,()->{h.assertTrue(animal.recoveredActionTime(0)<0,"Finite original action expires");animal.discard();h.succeed();});
    }

    @GameTest
    public void rottenRosterHasEnemyRulesAndOriginalLackeyWeapons(GameTestHelper h) {
        int count=0;
        for(var entry:Menagerie.TYPES.entrySet())if(entry.getKey().startsWith("rotten_")){
            var animal=entry.getValue().create(h.getLevel(),EntitySpawnReason.COMMAND);
            h.assertTrue(animal instanceof RottenCreatures.Undead&&animal instanceof Enemy,"Native undead type: "+entry.getKey());
            h.assertTrue(!animal.isFood(new ItemStack(Items.ROTTEN_FLESH)),"Hostiles cannot breed from their loot");
            animal.finalizeSpawn(h.getLevel(),h.getLevel().getCurrentDifficultyAt(h.absolutePos(BlockPos.ZERO)),EntitySpawnReason.COMMAND,null);
            if(entry.getKey().endsWith("lackey"))h.assertTrue(animal.getMainHandItem().is(Items.STONE_SWORD),"Original lackey sword");
            if(entry.getKey().equals("rotten_skeleton_lackey"))h.assertTrue(animal.getOffhandItem().is(Items.STONE_SWORD),"Original dual-wielding skeleton");
            if(entry.getKey().equals("rotten_burned"))h.assertTrue(animal.fireImmune(),"Burned survives its lava habitat");
            count++;
        }
        h.assertTrue(count==12,"All twelve supplied undead species registered");
        h.assertTrue(RottenStorage.HUNTER_WOLF!=null,"Separate vanilla-rig hunter wolf registered");h.succeed();
    }
}
