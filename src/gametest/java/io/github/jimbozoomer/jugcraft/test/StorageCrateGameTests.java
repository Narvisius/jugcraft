package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.storage.*;
import io.github.jimbozoomer.jugcraft.finance.*;
import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.item.*;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;

public class StorageCrateGameTests {
    private static final String ARENA = "jugcraft-test:arms_arena";
    private static final BlockPos AT = new BlockPos(4, 2, 4);
    private StorageCrateBlockEntity place(GameTestHelper h, StorageCrateTier tier) {
        h.setBlock(AT, JugcraftStorage.STORAGE_CRATES.get(tier).defaultBlockState().setValue(StorageCrateBlock.FACING, Direction.WEST));
        return h.getBlockEntity(AT, StorageCrateBlockEntity.class);
    }
    private ServerPlayer player(GameTestHelper h, StorageCrateBlockEntity crate) {
        var player = h.makeMockServerPlayerInLevel(); player.setGameMode(GameType.SURVIVAL);
        var p = crate.getBlockPos(); player.snapTo(p.getX()+.5,p.getY(),p.getZ()-1.5,0,0); return player;
    }
    private static void check(boolean ok, String text) { if (!ok) throw new AssertionError(text); }

    @GameTest(structure=ARENA) public void capacitiesTransfersRollbackAndIntegerSlotPersistence(GameTestHelper h) {
        for (var tier : StorageCrateTier.values()) {
            var crate = place(h,tier); check(crate.getContainerSize()==54*(tier.ordinal()+1),"Exact capacity "+tier);
            for (var face : Direction.values()) {
                var port=ItemStorage.SIDED.find(h.getLevel(),crate.getBlockPos(),face); check(port!=null,"Missing face "+face);
                try(var tx=Transaction.openOuter()) { check(port.insert(ItemVariant.of(Items.COBBLESTONE),64L*tier.slots+1,tx)==64L*tier.slots,"Bounded capacity"); }
                check(crate.isEmpty(),"Aborted insertion must roll back");
                try(var tx=Transaction.openOuter()) { check(port.insert(ItemVariant.of(Items.COBBLESTONE),64L*tier.slots,tx)==64L*tier.slots,"Full insertion"); tx.commit(); }
                check(crate.getItem(tier.slots-1).getCount()==64,"Automation reaches final slot");
                check(AbstractContainerMenu.getRedstoneSignalFromContainer(crate)==15,"Full comparator");
                try(var tx=Transaction.openOuter()) { check(port.extract(ItemVariant.of(Items.COBBLESTONE),64L*tier.slots,tx)==64L*tier.slots,"Full extraction");tx.commit(); }
            }
            var tool=new ItemStack(Items.DIAMOND_PICKAXE);tool.set(DataComponents.CUSTOM_NAME,Component.literal("Saved pickaxe"));tool.setDamageValue(13);
            var named=new ItemStack(crate.getBlockState().getBlock());named.set(DataComponents.CUSTOM_NAME,Component.literal("Workshop supplies"));crate.applyComponentsFromItemStack(named);
            crate.setItem(tier.slots-1,tool);crate.setItem(1,new ItemStack(Items.ENDER_PEARL,16));crate.setItem(0,new ItemStack(Items.STONE,64));
            var loaded=(StorageCrateBlockEntity)BlockEntity.loadStatic(crate.getBlockPos(),crate.getBlockState(),crate.saveWithFullMetadata(h.getLevel().registryAccess()),h.getLevel().registryAccess());
            check(loaded!=null && ItemStack.matches(loaded.getItem(tier.slots-1),tool),"Final slot survives save, including slots above 255: "+tier);
            check(loaded.getItem(1).getCount()==16&&loaded.getItem(0).getCount()==64&&loaded.getName().getString().equals("Workshop supplies"),"Stack sizes and name saved");
            check(!crate.collectComponents().has(DataComponents.CONTAINER),"Even creative pick-block must not create a portable 256-slot component");
            crate.clearContent();
        }
        check(CrateBlockEntity.STACKS==32,"Original bulk crate unchanged");h.succeed();
    }

    @GameTest(structure=ARENA) public void upgradesPreserveNameFacingContentsAndRejectStaleMenus(GameTestHelper h) {
        var crate=place(h,StorageCrateTier.BRONZE);var p=player(h,crate);
        var named=new ItemStack(crate.getBlockState().getBlock());named.set(DataComponents.CUSTOM_NAME,Component.literal("Named crate"));crate.applyComponentsFromItemStack(named);
        for (var target : StorageCrateTier.values()) {
            if (target==StorageCrateTier.BRONZE)continue;
            int last=crate.getContainerSize()-1;
            var payload=new ItemStack(Items.DIAMOND_PICKAXE);payload.setDamageValue(target.ordinal()+2);payload.set(DataComponents.CUSTOM_NAME,Component.literal("Tier "+target));
            crate.setItem(last,payload.copy());
            var oldMenu=new StorageCrateMenu(41,p.getInventory(),crate);p.containerMenu=oldMenu;
            var kit=(StorageCrateUpgradeItem)JugcraftStorage.STORAGE_UPGRADES.get(target);var held=new ItemStack(kit,2);p.setItemInHand(InteractionHand.MAIN_HAND,held);
            check(kit.upgrade(h.getLevel(),crate.getBlockPos(),p,held)==InteractionResult.SUCCESS,"Upgrade succeeds "+target);
            var next=h.getBlockEntity(AT,StorageCrateBlockEntity.class);
            check(next.tier()==target&&ItemStack.matches(next.getItem(last),payload)&&next.getItem(last+1).isEmpty(),"Contents preserved, expansion empty");
            check(next.getName().getString().equals("Named crate")&&next.getBlockState().getValue(StorageCrateBlock.FACING)==Direction.WEST,"Name and facing preserved");
            check(held.getCount()==1&&!oldMenu.stillValid(p),"Exactly one kit, old menu invalid");
            oldMenu.clicked(last,0,ContainerInput.PICKUP,p);check(oldMenu.getCarried().isEmpty(),"Stale click cannot extract");
            check(kit.upgrade(h.getLevel(),next.getBlockPos(),p,held)==InteractionResult.FAIL&&held.getCount()==1,"Wrong-tier/repeated request consumes nothing");
            check(h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(next.getBlockPos()).inflate(2)).isEmpty(),"Upgrade never spills items");
            crate=next;
        }
        h.succeed();
    }

    @GameTest(structure=ARENA) public void shiftClickUsesHiddenSlotsAndDeniesRevokedAccess(GameTestHelper h) {
        var crate=place(h,StorageCrateTier.TUNGSTEN);var p=player(h,crate);
        for(int i=0;i<crate.getContainerSize()-1;i++)crate.setItem(i,new ItemStack(Items.STONE,64));
        p.getInventory().setItem(9,new ItemStack(Items.ENDER_PEARL,16));
        var menu=new StorageCrateMenu(41,p.getInventory(),crate);p.containerMenu=menu;
        check(!menu.quickMoveStack(p,crate.getContainerSize()).isEmpty()&&crate.getItem(377).getCount()==16,"Shift-click reaches last hidden slot");
        check(!menu.quickMoveStack(p,377).isEmpty()&&crate.getItem(377).isEmpty(),"Shift-click extracts high slot");
        p.setGameMode(GameType.ADVENTURE);
        check(!menu.stillValid(p),"Live access check after mode change");
        menu.clicked(0,0,ContainerInput.PICKUP,p);check(menu.getCarried().isEmpty()&&crate.getItem(0).getCount()==64,"Denied click preserves contents");
        p.setGameMode(GameType.SURVIVAL);p.snapTo(crate.getBlockPos().getX()+40,crate.getBlockPos().getY(),crate.getBlockPos().getZ(),0,0);
        check(!menu.stillValid(p),"Distance revokes access");h.succeed();
    }

    @GameTest(structure=ARENA) public void concurrentViewersAndChangedClaimCannotDuplicateOrSteal(GameTestHelper h) {
        var crate=place(h,StorageCrateTier.BRONZE);var owner=player(h,crate);var visitor=player(h,crate);
        var first=new StorageCrateMenu(41,owner.getInventory(),crate);var second=new StorageCrateMenu(42,visitor.getInventory(),crate);
        crate.setItem(53,new ItemStack(Items.DIAMOND,7));
        first.clicked(53,0,ContainerInput.PICKUP,owner);second.clicked(53,0,ContainerInput.PICKUP,visitor);
        check(first.getCarried().getCount()==7&&second.getCarried().isEmpty()&&crate.getItem(53).isEmpty(),"Two viewers cannot take the same stack twice");
        first.clicked(53,0,ContainerInput.PICKUP,owner);
        var land=LandRegistry.get(h.getLevel().getServer());var book=Ledger.get(h.getLevel().getServer()).book();long key=LandRegistry.keyOf(crate.getBlockPos());
        try {
            check(land.assign(book,key,book.account(owner.getUUID(),"StorageOwner"),0).ok(),"Claim created");
            check(first.stillValid(owner)&&!second.stillValid(visitor),"Ownership change immediately revokes open visitor menu");
            second.clicked(53,0,ContainerInput.PICKUP,visitor);check(second.getCarried().isEmpty()&&crate.getItem(53).getCount()==7,"Denied visitor extraction");
            var upgrade=(StorageCrateUpgradeItem)JugcraftStorage.STORAGE_UPGRADES.get(StorageCrateTier.STEEL);var kit=new ItemStack(upgrade);
            check(upgrade.upgrade(h.getLevel(),crate.getBlockPos(),visitor,kit)==InteractionResult.FAIL&&kit.getCount()==1,"Visitor cannot upgrade a claimed crate");
        } finally {land.abandon(owner,book,key,true,0);}
        h.succeed();
    }

    @GameTest(structure=ARENA) public void wrongToolsCreativeRemovalAndExplosionsSpillContentsOnce(GameTestHelper h) {
        var crate=place(h,StorageCrateTier.BRONZE);var p=player(h,crate);var pos=crate.getBlockPos();
        crate.setItem(53,new ItemStack(Items.DIAMOND,7));
        check(p.gameMode.destroyBlock(pos),"Survival removal with empty hand");
        checkDrops(h,pos,7,0);
        clearDrops(h,pos);
        crate=place(h,StorageCrateTier.BRONZE);crate.setItem(53,new ItemStack(Items.DIAMOND,7));p.setGameMode(GameType.CREATIVE);
        check(p.gameMode.destroyBlock(pos),"Creative removal");checkDrops(h,pos,7,0);clearDrops(h,pos);
        // Move the blast to the centre of this test's arena.
        pos=h.absolutePos(new BlockPos(12,3,12));h.getLevel().setBlockAndUpdate(pos,JugcraftStorage.STORAGE_CRATES.get(StorageCrateTier.BRONZE).defaultBlockState());
        crate=(StorageCrateBlockEntity)h.getLevel().getBlockEntity(pos);crate.setItem(53,new ItemStack(Items.DIAMOND,7));
        h.getLevel().explode(null,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,4,net.minecraft.world.level.Level.ExplosionInteraction.TNT);
        check(h.getLevel().getBlockState(pos).isAir(),"Explosion removes crate");
        int gems=0,blocks=0;for(var entity:h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(4))) {
            if(entity.getItem().is(Items.DIAMOND))gems+=entity.getItem().getCount();
            if(entity.getItem().is(JugcraftStorage.STORAGE_CRATES.get(StorageCrateTier.BRONZE).asItem()))blocks+=entity.getItem().getCount();
        }
        check(gems==7&&blocks<=1,"Explosion spills once and never duplicates block");h.succeed();
    }
    private void clearDrops(GameTestHelper h,BlockPos pos){for(var item:h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(2)))item.discard();}
    private void checkDrops(GameTestHelper h,BlockPos pos,int expectedGems,int expectedBlocks){
        int gems=0,blocks=0;for(var entity:h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(2))) {
            if(entity.getItem().is(Items.DIAMOND))gems+=entity.getItem().getCount();
            if(entity.getItem().is(JugcraftStorage.STORAGE_CRATES.get(StorageCrateTier.BRONZE).asItem()))blocks+=entity.getItem().getCount();
        }
        check(gems==expectedGems&&blocks==expectedBlocks,"Removal drops: gems="+gems+", blocks="+blocks);
    }

    @GameTest(structure=ARENA) public void breakingDropsExactlyOnceAndCrateIsNotPortable(GameTestHelper h) {
        var crate=place(h,StorageCrateTier.TUNGSTEN);var pos=crate.getBlockPos();
        crate.setItem(0,new ItemStack(Items.DIAMOND,7));crate.setItem(377,new ItemStack(Items.ENDER_PEARL,16));
        h.getLevel().destroyBlock(pos,true);
        crate.preRemoveSideEffects(pos,crate.getBlockState());
        int gems=0,pearls=0,blocks=0;
        for(var drop:h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(2))) {
            var s=drop.getItem();if(s.is(Items.DIAMOND))gems+=s.getCount();if(s.is(Items.ENDER_PEARL))pearls+=s.getCount();
            if(s.is(JugcraftStorage.STORAGE_CRATES.get(StorageCrateTier.TUNGSTEN).asItem())) {blocks+=s.getCount();check(!s.has(DataComponents.CONTAINER),"Dropped crate is empty");}
        }
        check(gems==7&&pearls==16&&blocks==1,"Exactly one empty crate and each saved stack once: "+gems+","+pearls+","+blocks);h.succeed();
    }

    @GameTest public void bothCraftingRoutesAndAllKitsSelectTheIntendedProduct(GameTestHelper h) {
        for(var tier:StorageCrateTier.values()) {
            for(String suffix:tier==StorageCrateTier.BRONZE?List.of(""):List.of("_upgrade","_direct","_kit")) {
                String name=tier.id()+suffix;
                try(var in=getClass().getResourceAsStream("/data/jugcraft/recipe/storage_crates/"+name+".json")) {
                    var json=com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(in,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
                    var ops=net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE,h.getLevel().registryAccess());
                    var stacks=new ArrayList<ItemStack>();
                    for(var row:json.getAsJsonArray("pattern"))for(char symbol:row.getAsString().toCharArray())stacks.add(symbol==' '?ItemStack.EMPTY:
                        new ItemStack(net.minecraft.world.item.crafting.Ingredient.CODEC.parse(ops,json.getAsJsonObject("key").get(String.valueOf(symbol))).getOrThrow().items().findFirst().orElseThrow()));
                    var grid=net.minecraft.world.item.crafting.CraftingInput.of(3,3,stacks);
                    var selected=h.getLevel().recipeAccess().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING,grid,h.getLevel()).orElseThrow();
                    check(ItemStack.matches(selected.value().assemble(grid),StarterWorkshopGameTests.stack(json.getAsJsonObject("result").get("id").getAsString(),1)),"Recipe collision "+name+": "+selected.id());
                    check(stacks.stream().filter(s->!s.isEmpty()).count()==(suffix.equals("_direct")?9:5),"Correct ingredient count");
                }catch(java.io.IOException e){throw new java.io.UncheckedIOException(e);}
            }
        }
        h.succeed();
    }
}
