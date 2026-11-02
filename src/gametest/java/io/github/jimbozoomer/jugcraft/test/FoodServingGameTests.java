package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.agriculture.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;

public final class FoodServingGameTests {
    @GameTest(maxTicks=40)
    public void everyEdibleHasPlacementAndRoundTrips(GameTestHelper h) {
        var player=h.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        var floor=h.absolutePos(new BlockPos(3,1,3)); var at=floor.above();
        h.getLevel().setBlock(floor,Blocks.STONE.defaultBlockState(),Block.UPDATE_ALL);
        player.setPos(floor.getX()+.5,floor.getY()+1,floor.getZ()+3);
        var hit=new BlockHitResult(Vec3.atCenterOf(floor).add(0,.5,0),Direction.UP,floor,false);
        for(var item:BuiltInRegistries.ITEM) {
            var id=BuiltInRegistries.ITEM.getKey(item);
            if(!java.util.Set.of("minecraft","jugcraft").contains(id.getNamespace()) || item.getDefaultInstance().get(DataComponents.FOOD)==null)continue;
            boolean old=MenuDishes.PLACED.stream().anyMatch(d->id.toString().equals("jugcraft:"+d.id()));
            boolean nativeBlock=item instanceof BlockItem bi && bi.getBlock() instanceof io.github.jimbozoomer.jugcraft.menagerie.HouseholdBlock;
            h.assertTrue(old||nativeBlock||item==Items.PUMPKIN_PIE||FoodServings.entries().containsKey(item),"No serving for "+id);
        }
        for(var entry:FoodServings.entries().entrySet()) {
            var stack=new ItemStack(entry.getKey(),2);stack.set(DataComponents.CUSTOM_NAME,Component.literal("Dinner test"));
            var expected=stack.copyWithCount(1);
            player.setItemInHand(InteractionHand.MAIN_HAND,stack);
            player.setShiftKeyDown(false);
            h.assertTrue(FoodServings.place(player,h.getLevel(),InteractionHand.MAIN_HAND,hit)==InteractionResult.PASS,"Normal use unchanged");
            player.setShiftKeyDown(true);
            player.gameMode.useItemOn(player,h.getLevel(),stack,InteractionHand.MAIN_HAND,hit);
            h.assertTrue(h.getLevel().getBlockState(at).is(entry.getValue()),"Placed "+BuiltInRegistries.ITEM.getKey(entry.getKey()));
            h.assertTrue(stack.getCount()==1,"Exactly one consumed");
            var be=(ShowcaseBlockEntity)h.getLevel().getBlockEntity(at);
            h.assertTrue(ItemStack.isSameItemSameComponents(be.get(0),expected),"Components kept in block");
            player.setShiftKeyDown(false);player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
            player.gameMode.useItemOn(player,h.getLevel(),ItemStack.EMPTY,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(at),Direction.UP,at,false));
            h.assertTrue(h.getLevel().getBlockState(at).isAir(),"Pickup removes serving");
            h.assertTrue(ItemStack.isSameItemSameComponents(player.getMainHandItem(),expected)&&player.getMainHandItem().getCount()==1,"Exact named item returned");
        }
        h.succeed();
    }
    @GameTest(maxTicks=30)
    public void breakAndSupportLossDropOnce(GameTestHelper h) {
        var level=h.getLevel();var floor=h.absolutePos(new BlockPos(3,1,3));var at=floor.above();
        var block=FoodServings.entries().get(Items.SUSPICIOUS_STEW);
        var stack=new ItemStack(Items.SUSPICIOUS_STEW);stack.set(DataComponents.CUSTOM_NAME,Component.literal("Saved stew"));
        for(boolean support:java.util.List.of(false,true)) {
            level.setBlock(floor,Blocks.STONE.defaultBlockState(),Block.UPDATE_ALL);
            level.setBlock(at,block.defaultBlockState(),Block.UPDATE_ALL);
            ((ShowcaseBlockEntity)level.getBlockEntity(at)).put(0,stack);
            level.destroyBlock(support?floor:at,true);
            var drops=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(at).inflate(2),e->e.getItem().is(Items.SUSPICIOUS_STEW));
            h.assertTrue(drops.stream().mapToInt(e->e.getItem().getCount()).sum()==1,"One drop after "+(support?"support loss":"break"));
            h.assertTrue(ItemStack.isSameItemSameComponents(drops.getFirst().getItem(),stack),"Drop retains components");
            drops.forEach(e->e.discard());
        }
        h.succeed();
    }
    @GameTest(maxTicks=30)
    public void creativeUnsupportedAndSavedComponents(GameTestHelper h) {
        var level=h.getLevel();var floor=h.absolutePos(new BlockPos(3,1,3));var at=floor.above();
        var player=h.makeMockServerPlayerInLevel();player.setGameMode(GameType.CREATIVE);
        player.setPos(floor.getX()+.5,floor.getY()+1,floor.getZ()+3);player.setShiftKeyDown(true);
        var stack=new ItemStack(Items.BREAD,4);stack.set(DataComponents.CUSTOM_NAME,Component.literal("Saved bread"));
        player.setItemInHand(InteractionHand.MAIN_HAND,stack);
        var hit=new BlockHitResult(Vec3.atCenterOf(floor).add(0,.5,0),Direction.UP,floor,false);
        level.setBlock(floor,Blocks.STONE.defaultBlockState(),Block.UPDATE_ALL);
        FoodServings.place(player,level,InteractionHand.MAIN_HAND,hit);
        h.assertTrue(stack.getCount()==4,"Creative placement preserves stack");
        var be=(ShowcaseBlockEntity)level.getBlockEntity(at);
        var saved=be.saveWithFullMetadata(level.registryAccess());
        var restored=new ShowcaseBlockEntity(FoodServings.ENTITY,at,level.getBlockState(at));
        restored.loadWithComponents(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,level.registryAccess(),saved));
        h.assertTrue(ItemStack.isSameItemSameComponents(restored.get(0),stack)&&restored.get(0).getCount()==1,"Saved serving retains one full item");
        level.removeBlock(at,false);level.removeBlock(floor,false);
        h.assertTrue(FoodServings.place(player,level,InteractionHand.MAIN_HAND,hit)==InteractionResult.FAIL&&stack.getCount()==4,"Unsupported placement loses nothing");
        h.succeed();
    }

}
