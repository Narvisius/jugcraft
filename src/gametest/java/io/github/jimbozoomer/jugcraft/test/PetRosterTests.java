package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.pets.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.BlockPos;
import com.mojang.serialization.JsonOps;

public final class PetRosterTests {
    @GameTest(maxTicks=80) public void recallPreservesIdentityAndOwnership(GameTestHelper h) {
        var p=h.makeMockServerPlayerInLevel(); var other=h.makeMockServerPlayerInLevel();
        var base=h.absolutePos(new BlockPos(1,2,1));p.snapTo(base.getX(),base.getY(),base.getZ(),0,0);
        for(int x=-6;x<=6;x++)for(int z=-6;z<=6;z++)h.getLevel().setBlock(base.offset(x,-1,z),Blocks.STONE.defaultBlockState(),3);
        var wolf=EntityTypes.WOLF.create(h.getLevel(),EntitySpawnReason.COMMAND);wolf.tame(p);wolf.setNoAi(true);wolf.setCustomName(Component.literal("Juniper"));
        wolf.snapTo(base.getX()+1,base.getY(),base.getZ(),0,0);wolf.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.DIAMOND,3));h.getLevel().addFreshEntity(wolf);
        var id=wolf.getUUID();var entry=Pets.enroll(wolf);entry.putInt("xp",2280);PetBehaviors.bonuses(wolf,20);wolf.setHealth(wolf.getMaxHealth());var max=wolf.getMaxHealth();var health=wolf.getHealth();
        h.assertTrue(Pets.action(other,id.toString(),"recall").equals("This is not your pet."),"Other owner cannot recall");
        h.assertTrue(Pets.action(p,id.toString(),"recall").equals("Pet recalled."),"Recall succeeds");
        h.assertTrue(wolf.isRemoved()&&entry.getStringOr("state","").equals("STORED"),"Original removed before stored pet can summon");
        var encoded=PetRoster.CODEC.encodeStart(JsonOps.INSTANCE,PetRoster.get(h.getLevel().getServer())).getOrThrow();
        var restored=PetRoster.CODEC.parse(JsonOps.INSTANCE,encoded).getOrThrow();
        h.assertTrue(restored.entries.get(id).getCompound("entity").isPresent(),"Saved roster preserves entity payload");
        h.runAfterDelay(2,()->{
            h.assertTrue(Pets.action(p,id.toString(),"summon").equals("Pet summoned."),"Safe summon succeeds");
            var pet=(Mob)h.getLevel().getEntity(id);
            h.assertTrue(pet!=null&&pet.getName().getString().equals("Juniper")&&pet.getMaxHealth()==max&&pet.getHealth()==health,"Name UUID health stats preserved");
            h.assertTrue(pet.getMainHandItem().getCount()==3&&pet.getMainHandItem().is(Items.DIAMOND),"Equipment preserved");
            h.assertTrue(Pets.action(p,id.toString(),"summon").equals("Pet is already active."),"Duplicate summon refused");
            var stale=EntityType.loadEntityRecursive(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,p.registryAccess(),restored.entries.get(id).getCompoundOrEmpty("entity")),h.getLevel(),EntitySpawnReason.LOAD,EntityProcessor.NOP);
            pet.discard();
            h.runAfterDelay(2,()->{h.getLevel().addFreshEntity(stale);h.assertTrue(stale.isRemoved(),"Old incarnation cannot return after a later summon");h.succeed();});
        });
    }
    @GameTest(maxTicks=40) public void knockoutSuppressesDeathAndWaitsForRecovery(GameTestHelper h){
        var p=h.makeMockServerPlayerInLevel();var wolf=EntityTypes.WOLF.create(h.getLevel(),EntitySpawnReason.COMMAND);wolf.tame(p);wolf.setNoAi(true);
        var pos=h.absolutePos(new BlockPos(1,2,1));wolf.snapTo(pos.getX(),pos.getY(),pos.getZ(),0,0);h.getLevel().addFreshEntity(wolf);
        wolf.hurtServer(h.getLevel(),wolf.damageSources().generic(),10000);
        var record=PetRoster.get(h.getLevel().getServer()).entries.get(wolf.getUUID());
        h.assertTrue(record!=null&&record.getStringOr("state","").equals("KNOCKED_OUT")&&wolf.isRemoved(),"Lethal damage becomes stored knockout");
        h.assertTrue(Pets.action(p,wolf.getUUID().toString(),"summon").contains("recovering"),"Recovery timer blocks immediate healing loop");
        h.succeed();
    }
    @GameTest(maxTicks=40) public void unloadedRecallNeverCreatesStaleCopy(GameTestHelper h){
        var p=h.makeMockServerPlayerInLevel();var wolf=EntityTypes.WOLF.create(h.getLevel(),EntitySpawnReason.COMMAND);wolf.tame(p);
        var entry=Pets.enroll(wolf);
        h.assertTrue(Pets.action(p,wolf.getUUID().toString(),"recall").startsWith("Recall queued"),"Unloaded recall queued");
        h.assertTrue(entry.getBooleanOr("recall",false),"Queue persisted");
        h.assertTrue(Pets.action(p,wolf.getUUID().toString(),"summon").equals("Pet is already active."),"No summon from stale snapshot");
        h.getLevel().addFreshEntity(wolf);
        h.runAfterDelay(22,()->{h.assertTrue(wolf.isRemoved()&&entry.getStringOr("state","").equals("STORED"),"Queued recall stores the real entity when it loads");h.succeed();});
    }
    @GameTest(maxTicks=40) public void customPetsAndSummonExclusions(GameTestHelper h){
        var p=h.makeMockServerPlayerInLevel();var stranger=h.makeMockServerPlayerInLevel();
        var animal=io.github.jimbozoomer.jugcraft.menagerie.Menagerie.TYPES.get("mount_direwolf").create(h.getLevel(),EntitySpawnReason.COMMAND);
        animal.tame(p.getUUID());animal.variant(1);animal.setCustomName(Component.literal("Fenrir"));animal.equipBody(new ItemStack(Items.DIAMOND_HORSE_ARMOR));
        h.assertTrue(p.getUUID().equals(Pets.owner(animal)),"Custom menagerie ownership adapted");
        h.assertTrue(!local.peepo.MobTransportCrate.owned(stranger,animal),"Another player cannot crate custom pets");
        var entry=Pets.enroll(animal);h.assertTrue(Pets.store(animal,entry,false),"Custom pet stored");
        var restored=(io.github.jimbozoomer.jugcraft.menagerie.MenagerieAnimal)EntityType.loadEntityRecursive(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,p.registryAccess(),entry.getCompoundOrEmpty("entity")),h.getLevel(),EntitySpawnReason.LOAD,EntityProcessor.NOP);
        h.assertTrue(restored.variant()==animal.variant()&&restored.getName().getString().equals("Fenrir")&&restored.getItemBySlot(EquipmentSlot.BODY).is(Items.DIAMOND_HORSE_ARMOR),"Custom variant name and armor restored");
        var familiar=io.github.jimbozoomer.jugcraft.nouveau.NouveauFamiliars.TYPE.create(h.getLevel(),EntitySpawnReason.MOB_SUMMONED);familiar.bind(p.getUUID(),"bookwyrm");
        h.assertTrue(Pets.owner(familiar)==null,"Auto-respawning familiar keeps its own lifecycle");
        h.succeed();
    }
}
