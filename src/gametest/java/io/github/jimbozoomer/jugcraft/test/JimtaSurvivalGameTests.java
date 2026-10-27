package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.minecraft.world.phys.*;
import java.util.*;

public final class JimtaSurvivalGameTests {
    private static Item item(String name){return JimtaSpellbooks.ITEMS.get(name);}
    @GameTest public void translationConsumesOneFragmentAndCannotRepeat(GameTestHelper h){
        var p=JimtafactsComputerGameTests.player(h);var at=h.absolutePos(new BlockPos(2,2,2));
        h.getLevel().setBlockAndUpdate(at,Blocks.ENCHANTING_TABLE.defaultBlockState());
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(item("archevoker_logbook_untranslated")));
        p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(item("ancient_knowledge_fragment"),2));
        var use=new UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(at),Direction.UP,at,false));
        p.getMainHandItem().useOn(use);
        h.assertTrue(p.getMainHandItem().is(item("archevoker_logbook_translated"))&&p.getOffhandItem().getCount()==1,"Translation exchanges book and consumes exactly one fragment");
        var pages=p.getMainHandItem().get(net.minecraft.core.component.DataComponents.WRITTEN_BOOK_CONTENT);
        h.assertTrue(pages!=null&&pages.pages().size()==8,"Translated book retains eight readable pages");
        p.getMainHandItem().useOn(new UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(at),Direction.UP,at,false)));
        h.assertTrue(p.getOffhandItem().getCount()==1,"Translated book cannot charge twice");h.succeed();
    }
    @GameTest public void translationRequiresReagentAndPermission(GameTestHelper h){
        var p=JimtafactsComputerGameTests.player(h);var at=h.absolutePos(new BlockPos(2,2,2));
        h.getLevel().setBlockAndUpdate(at,Blocks.ENCHANTING_TABLE.defaultBlockState());
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(item("archevoker_logbook_untranslated")));
        var use=new UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(at),Direction.UP,at,false));
        p.getMainHandItem().useOn(use);
        h.assertTrue(p.getMainHandItem().is(item("archevoker_logbook_untranslated")),"No free translation without reagent");
        p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(item("ancient_knowledge_fragment")));
        try(var claim=TestClaims.open().block(at,UUID.randomUUID())){
            p.getMainHandItem().useOn(use);
            h.assertTrue(p.getMainHandItem().is(item("archevoker_logbook_untranslated"))&&p.getOffhandItem().getCount()==1,"Denied claim preserves both inputs");
        }h.succeed();
    }
    @GameTest public void explorationAndBossTablesActuallyAwardNewItems(GameTestHelper h){
        var p=JimtafactsComputerGameTests.player(h);
        var routes=Map.of("chests/evoker_fort","furled_map_ancient",
            "chests/catacombs/hidden_trough_treasure","lurker_ring",
            "chests/ice_spider_den/dungeon","music_disc_whispers_of_ice",
            "chests/citadel/citadel_tomes","unchained_book",
            "entities/archevoker","archevoker_logbook_untranslated",
            "entities/dead_king_per_player","legendary_spell_book",
            "entities/dead_king_per_player_ominous","tarnished_helmet");
        for(var entry:routes.entrySet()){
            var params=new LootParams.Builder(h.getLevel()).withParameter(LootContextParams.ORIGIN,p.position());
            boolean mob=entry.getKey().startsWith("entities/");
            if(mob)params.withParameter(LootContextParams.THIS_ENTITY,p).withParameter(LootContextParams.DAMAGE_SOURCE,p.damageSources().generic()).withParameter(LootContextParams.LAST_DAMAGE_PLAYER,p);
            var table=h.getLevel().getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,Jugcraft.id("jimtafacts/spellbooks/"+entry.getKey())));
            var context=params.create(mob?LootContextParamSets.ENTITY:LootContextParamSets.CHEST);
            boolean found=false;
            for(int seed=1;seed<=256&&!found;seed++)found=table.getRandomItems(context,seed*0x9E3779B97F4A7C15L).stream().anyMatch(s->s.is(item(entry.getValue())));
            h.assertTrue(found,"Loaded loot table awards "+entry.getValue());
        }h.succeed();
    }
    @GameTest public void debrisFeaturePlacesBuriedOreAndNeverReplacesBuildings(GameTestHelper h){
        var registry=h.getLevel().registryAccess().lookupOrThrow(Registries.FEATURE);
        var feature=registry.getValue(ResourceKey.create(Registries.FEATURE,Jugcraft.id("jimtafacts/spellbooks/ore_arcane_debris")));
        h.assertTrue(feature!=null,"Configured debris feature loads");
        var origin=h.absolutePos(new BlockPos(4,4,4));
        for(var pos:BlockPos.betweenClosed(origin.offset(-4,-4,-4),origin.offset(4,4,4)))h.getLevel().setBlockAndUpdate(pos,Blocks.NETHERRACK.defaultBlockState());
        var ore=JimtaWorkshops.BLOCKS.get("arcane_debris");
        for(int seed=0;seed<32;seed++)feature.place(h.getLevel(),h.getLevel().getChunkSource().getGenerator(),net.minecraft.util.RandomSource.create(seed*0x9E3779B97F4A7C15L),origin);
        boolean placed=false;
        for(var pos:BlockPos.betweenClosed(origin.offset(-4,-4,-4),origin.offset(4,4,4)))placed|=h.getLevel().getBlockState(pos).is(ore);
        h.assertTrue(placed,"Buried Nether stone receives actual debris");
        for(var pos:BlockPos.betweenClosed(origin.offset(-4,-4,-4),origin.offset(4,4,4)))h.getLevel().setBlockAndUpdate(pos,Blocks.STONE_BRICKS.defaultBlockState());
        feature.place(h.getLevel(),h.getLevel().getChunkSource().getGenerator(),net.minecraft.util.RandomSource.create(42),origin);
        for(var pos:BlockPos.betweenClosed(origin.offset(-4,-4,-4),origin.offset(4,4,4)))h.assertTrue(h.getLevel().getBlockState(pos).is(Blocks.STONE_BRICKS),"Ore cannot replace player building materials");h.succeed();
    }
}

