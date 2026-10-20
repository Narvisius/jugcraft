package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.menagerie.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;
import net.minecraft.util.ProblemReporter;

/** Ownership, unique-animal transport and actual runtime registries, not copied-art counts. */
public final class MenagerieGameTests {
    @GameTest(maxTicks=40)
    public void firebirdsShedMatchingFeathersOnTheirSavedProductionTimer(GameTestHelper h){
        var birds=new java.util.ArrayList<MenagerieAnimal>();
        for(int variant=0;variant<4;variant++){
            var bird=Menagerie.TYPES.get("mount_firebird").create(h.getLevel(),EntitySpawnReason.COMMAND);
            bird.variant(variant);bird.setPos(Vec3.atCenterOf(h.absolutePos(new BlockPos(1+variant,2,1))));
            var out=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,h.getLevel().registryAccess());bird.saveWithoutId(out);
            var saved=out.buildResult();saved.putInt("ProduceTicks",1);
            bird.load(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),saved));
            h.getLevel().addFreshEntity(bird);birds.add(bird);
        }
        h.runAfterDelay(10,()->{
            var center=Vec3.atCenterOf(h.absolutePos(new BlockPos(3,2,1)));
            var drops=h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(center,center).inflate(8));
            for(int variant=0;variant<4;variant++){
                var item=Menagerie.item("mount_firebird_feather"+(variant==0?"":Integer.toString(variant+1)));
                h.assertTrue(drops.stream().filter(e->e.getItem().is(item)).mapToInt(e->e.getItem().getCount()).sum()==1,"Exactly one feather for each bird color");
                var out=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,h.getLevel().registryAccess());birds.get(variant).saveWithoutId(out);
                h.assertTrue(out.buildResult().getIntOr("ProduceTicks",0)>5900,"Production cooldown persists instead of producing every tick");
            }
            h.succeed();
        });
    }
    @GameTest(maxTicks=40)
    public void newlyConnectedEggBreedingConsumesOneCycleAndPreservesOwnership(GameTestHelper h){
        for(String id:java.util.List.of("beast_caiman","beast_platypus","beast_terrapin","beast_triops","cavern_atlatitan","cavern_grottoceratops","cavern_relicheirus","cavern_subterranodon","cavern_tremorsaurus","cavern_vallumraptor")){
            var a=Menagerie.TYPES.get(id).create(h.getLevel(),EntitySpawnReason.COMMAND);
            var b=Menagerie.TYPES.get(id).create(h.getLevel(),EntitySpawnReason.COMMAND);
            var pos=Vec3.atCenterOf(h.absolutePos(new BlockPos(1,2,1)));a.setPos(pos);b.setPos(pos);
            var box=new AABB(pos,pos).inflate(3);h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,box).forEach(net.minecraft.world.entity.Entity::discard);
            a.spawnChildFromBreeding(h.getLevel(),b);
            String egg=id.equals("beast_triops")?id+"_eggs":id+"_egg";
            var drops=h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,box);
            h.assertTrue(drops.size()==1&&drops.get(0).getItem().is(Menagerie.item(egg))&&drops.get(0).getItem().getCount()==1,"One appropriate egg: "+id);
            h.assertTrue(a.getAge()==6000&&b.getAge()==6000,"Both parents receive the breeding cooldown: "+id);
            drops.forEach(net.minecraft.world.entity.Entity::discard);
            a.tame(java.util.UUID.randomUUID());a.setAge(0);b.setAge(0);a.spawnChildFromBreeding(h.getLevel(),b);
            h.assertTrue(h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,box).isEmpty(),"Absent owner cannot authorize egg production: "+id);
        }
        h.succeed();
    }
    @GameTest(maxTicks=40)
    public void starfishNetCapturesEveryColorAsAReleasableCreature(GameTestHelper h){
        var p=survivalPlayer(h);
        try{for(int variant=0;variant<4;variant++){
            p.getInventory().clearContent();p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Menagerie.item("wildlife_capture_net")));
            var a=Menagerie.TYPES.get("wildlife_starfish").create(h.getLevel(),EntitySpawnReason.COMMAND);a.variant(variant);a.setPos(p.position());h.getLevel().addFreshEntity(a);
            String color=a.species().variants().get(variant).name();
            h.assertTrue(MenagerieItems.capture(a,p,InteractionHand.MAIN_HAND)&&a.isRemoved(),"Captured living "+color+" starfish");
            ItemStack caught=ItemStack.EMPTY;for(int i=0;i<36;i++)if(p.getInventory().getItem(i).is(Menagerie.item("wildlife_"+color+"_starfish")))caught=p.getInventory().getItem(i);
            h.assertTrue(!caught.isEmpty()&&caught.getItem() instanceof MenagerieItems.CapturedDecoration,"Starfish item can release the captured creature and retain decorative block placement");
            h.assertTrue(caught.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getIntOr("Variant",-1)==variant,"Color preserved in saved capture");
            p.setItemInHand(InteractionHand.MAIN_HAND,caught);
            var place=new BlockPos(2,2,2);h.setBlock(place.below(),net.minecraft.world.level.block.Blocks.STONE);h.setBlock(place,net.minecraft.world.level.block.Blocks.WATER);
            var at=h.absolutePos(place);var context=new UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(at.below()),Direction.UP,at.below(),false));
            h.assertTrue(caught.getItem().useOn(context).consumesAction()&&caught.isEmpty(),"Release consumes the saved starfish exactly once");
            var released=h.getLevel().getEntitiesOfClass(MenagerieAnimal.class,new AABB(at).inflate(1),e->e.species().id().equals("wildlife_starfish"));
            h.assertTrue(released.size()==1&&released.getFirst().variant()==variant,"Released living starfish retains color");released.forEach(net.minecraft.world.entity.Entity::discard);
        }}finally{h.getLevel().getServer().getPlayerList().remove(p);}
        h.succeed();
    }
    private static net.minecraft.server.level.ServerPlayer survivalPlayer(GameTestHelper h){
        // Vanilla's in-level mock always reports creative, even after setGameMode(SURVIVAL).
        var profile=new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"menagerie-test-player");
        var p=new net.minecraft.server.level.ServerPlayer(h.getLevel().getServer(),h.getLevel(),profile,net.minecraft.server.level.ClientInformation.createDefault());
        var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        h.getLevel().getServer().getPlayerList().placeNewPlayer(connection,p,net.minecraft.server.network.CommonListenerCookie.createInitial(profile,false));
        p.setGameMode(GameType.SURVIVAL);p.setPos(Vec3.atCenterOf(h.absolutePos(new BlockPos(1,1,1))));
        h.assertTrue(!p.isCreative()&&!p.isSpectator(),"Fixture exercises real survival costs");return p;
    }

    @GameTest(maxTicks=20)
    public void adaptedBlockLootHonorsQueenAndDoubleSlabState(GameTestHelper h){
        var pos=h.absolutePos(new BlockPos(1,1,1));var hill=Menagerie.BLOCKS.get("wildlife_ant_hill").defaultBlockState();
        var queen=(net.minecraft.world.level.block.state.properties.BooleanProperty)hill.getBlock().getStateDefinition().getProperty("has_queen");
        for(boolean present:java.util.List.of(false,true)){
            var drops=net.minecraft.world.level.block.Block.getDrops(hill.setValue(queen,present),h.getLevel(),pos,null);
            h.assertTrue(drops.stream().filter(s->s.is(Menagerie.item("wildlife_queen_ant"))).mapToInt(ItemStack::getCount).sum()==(present?1:0),"Queen loot follows colony state");
        }
        var slab=Menagerie.BLOCKS.get("wildlife_shellstone_slab").defaultBlockState().setValue(net.minecraft.world.level.block.SlabBlock.TYPE,net.minecraft.world.level.block.state.properties.SlabType.DOUBLE);
        var drops=net.minecraft.world.level.block.Block.getDrops(slab,h.getLevel(),pos,null);
        h.assertTrue(drops.stream().filter(s->s.is(Menagerie.item("wildlife_shellstone_slab"))).mapToInt(ItemStack::getCount).sum()==2,"Double slabs return both slabs");h.succeed();
    }
    @GameTest(maxTicks=20)
    public void woodlandFlowersAndMendingHaveBoundedConsumableUses(GameTestHelper h){
        var p=survivalPlayer(h);var other=h.makeMockServerPlayerInLevel();
        var a=Menagerie.TYPES.get("grove_minipad").create(h.getLevel(),EntitySpawnReason.COMMAND);a.tame(p.getUUID());a.variant(1);a.setNoAi(true);
        var at=h.absolutePos(new BlockPos(1,1,1));a.snapTo(Vec3.atCenterOf(at),0,0);h.getLevel().addFreshEntity(a);
        other.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.SHEARS));a.mobInteract(other,InteractionHand.MAIN_HAND);h.assertTrue(a.variant()==1,"Another player cannot harvest a pet");
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.SHEARS));a.mobInteract(p,InteractionHand.MAIN_HAND);h.assertTrue(a.variant()==3,"Harvest switches to the supplied sheared skin");
        var items=h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(at).inflate(3));h.assertTrue(items.stream().anyMatch(e->e.getItem().is(Menagerie.item("grove_light_pink_minipad_flower_glow"))),"Matching glowing flower is actually produced");
        int count=items.size();a.mobInteract(p,InteractionHand.MAIN_HAND);h.assertTrue(h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(at).inflate(3)).size()==count,"Repeated clicks do not duplicate flowers");
        var saved=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,h.getLevel().registryAccess());a.saveWithoutId(saved);var tag=saved.buildResult();h.assertTrue(tag.getIntOr("FlowerVariant",-1)==1&&tag.getIntOr("FlowerRegrowTicks",0)>0,"Original color and regrowth time persist");
        p.setHealth(4);p.getInventory().setItem(9,new ItemStack(Items.GLOWSTONE_DUST,2));p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Menagerie.item("grove_heal_spell_book_2")));
        var book=p.getMainHandItem();book.getItem().use(p.level(),p,InteractionHand.MAIN_HAND);h.assertTrue(p.getHealth()==12&&p.getInventory().countItem(Items.GLOWSTONE_DUST)==1,"Mending consumes one reagent for eight health");book.getItem().use(p.level(),p,InteractionHand.MAIN_HAND);h.assertTrue(p.getHealth()==12&&p.getInventory().countItem(Items.GLOWSTONE_DUST)==1,"Cooldown prevents repeated activation");h.succeed();
    }
    @GameTest(maxTicks=30)
    public void fableWardFertilizerAndHostilityAreFunctional(GameTestHelper h){
        var p=survivalPlayer(h);var at=h.absolutePos(new BlockPos(1,1,1));p.snapTo(Vec3.atCenterOf(at),0,0);
        var scarecrow=Menagerie.TYPES.get("fable_scarecrow").create(h.getLevel(),EntitySpawnReason.COMMAND);
        h.assertTrue(scarecrow instanceof net.minecraft.world.entity.monster.Enemy&&!scarecrow.isFood(new ItemStack(Items.WHEAT)),"Hostile creatures cannot be bred as farm animals");
        p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD,new ItemStack(Menagerie.item("fable_straw_hat")));
        h.assertTrue(FableCreatures.wards(scarecrow,p)&&FableCreatures.spawnWarded(h.getLevel(),at),"Straw hat wards attacks and nearby natural spawns");
        h.assertTrue(!scarecrow.doHurtTarget(h.getLevel(),p),"Warded player is not attacked");
        h.setBlock(new BlockPos(1,0,1),net.minecraft.world.level.block.Blocks.FARMLAND);h.setBlock(new BlockPos(1,1,1),net.minecraft.world.level.block.Blocks.WHEAT);
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Menagerie.item("fable_fertile_slimeball"),2));
        var use=new UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(at),Direction.UP,at,false));
        h.assertTrue(p.getMainHandItem().useOn(use).consumesAction()&&p.getMainHandItem().getCount()==1,"Fertile slime consumes exactly one item");
        h.assertTrue(h.getLevel().getBlockState(at).getValue(net.minecraft.world.level.block.CropBlock.AGE)>0,"Crop actually grows");
        p.addEffect(new net.minecraft.world.effect.MobEffectInstance(FableCreatures.ROOT,40));
        h.assertTrue(p.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED)<.03,"Root reduces movement speed");h.succeed();
    }
    @GameTest(maxTicks=40)
    public void houndTrapAndWispPurseHaveRealRuntimeBehavior(GameTestHelper h){
        var at=h.absolutePos(new BlockPos(1,1,1));h.setBlock(new BlockPos(1,0,1),net.minecraft.world.level.block.Blocks.STONE);h.setBlock(new BlockPos(1,1,1),Menagerie.BLOCKS.get("fable_hound_trap"));
        var hound=Menagerie.TYPES.get("fable_hound").create(h.getLevel(),EntitySpawnReason.COMMAND);hound.snapTo(Vec3.atBottomCenterOf(at),0,0);hound.setNoAi(true);h.getLevel().addFreshEntity(hound);
        var wisp=Menagerie.TYPES.get("fable_nether_wisp").create(h.getLevel(),EntitySpawnReason.COMMAND);wisp.snapTo(Vec3.atCenterOf(at),0,0);FableCreatures.dropPurse(wisp,h.getLevel());
        var purses=h.getLevel().getEntitiesOfClass(FableCreatures.Purse.class,new AABB(at).inflate(3));h.assertTrue(purses.size()==1&&purses.getFirst().getItem().is(Items.GLOWSTONE_DUST),"Nether wisp has one collectible loot purse");
        var saved=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,h.getLevel().registryAccess());purses.getFirst().saveWithoutId(saved);var restored=FableCreatures.PURSE.create(h.getLevel(),EntitySpawnReason.COMMAND);restored.load(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),saved.buildResult()));h.assertTrue(restored.getItem().is(Items.GLOWSTONE_DUST),"Purse item survives a save");
        h.runAfterDelay(8,()->{h.assertTrue(hound.hasEffect(net.minecraft.world.effect.MobEffects.SLOWNESS),"Walking into the trap slows a wild hound");h.succeed();});
    }
    @GameTest(maxTicks=30)
    public void companionHarvestAndArmorPersistWithoutDuplication(GameTestHelper h){
        var p=survivalPlayer(h);var other=h.makeMockServerPlayerInLevel();
        var snail=Menagerie.TYPES.get("companion_snail").create(h.getLevel(),EntitySpawnReason.COMMAND);snail.tame(p.getUUID());snail.setNoAi(true);snail.setAge(0);snail.snapTo(Vec3.atCenterOf(h.absolutePos(new BlockPos(1,1,1))),0,0);h.getLevel().addFreshEntity(snail);
        other.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.GLASS_BOTTLE));snail.mobInteract(other,InteractionHand.MAIN_HAND);h.assertTrue(snail.readyToHarvest()&&other.getMainHandItem().is(Items.GLASS_BOTTLE),"Stranger cannot harvest a companion");
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.GLASS_BOTTLE));snail.mobInteract(p,InteractionHand.MAIN_HAND);h.assertTrue(p.getMainHandItem().is(Menagerie.item("companion_snail_slime_bottle"))&&!snail.readyToHarvest(),"Harvest converts one bottle and starts regrowth");
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.GLASS_BOTTLE));snail.mobInteract(p,InteractionHand.MAIN_HAND);h.assertTrue(p.getMainHandItem().is(Items.GLASS_BOTTLE),"No repeat harvest during regrowth");
        var out=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,h.getLevel().registryAccess());snail.saveWithoutId(out);var restored=snail.getType().create(h.getLevel(),EntitySpawnReason.COMMAND);restored.load(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),out.buildResult()));h.assertTrue(!((MenagerieAnimal)restored).readyToHarvest(),"Harvest cooldown survives saves and capture");
        var dragonfly=Menagerie.TYPES.get("companion_dragonfly").create(h.getLevel(),EntitySpawnReason.COMMAND);dragonfly.tame(p.getUUID());dragonfly.setAge(0);dragonfly.snapTo(snail.position(),0,0);
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Menagerie.item("companion_diamond_dragonfly_armor")));dragonfly.mobInteract(p,InteractionHand.MAIN_HAND);h.assertTrue(p.getMainHandItem().isEmpty()&&dragonfly.armor().equals("diamond")&&dragonfly.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR)==11,"Dragonfly armor equips with its actual defense and appearance");
        var armor=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,h.getLevel().registryAccess());dragonfly.saveWithoutId(armor);var loaded=Menagerie.TYPES.get("companion_dragonfly").create(h.getLevel(),EntitySpawnReason.COMMAND);loaded.load(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),armor.buildResult()));h.assertTrue(loaded.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR)==11,"Armor defense survives reload");h.succeed();
    }
    @GameTest(maxTicks=20)
    public void predatorsLeaveOwnedAndVanillaTamedAnimalsAlone(GameTestHelper h){
        var predator=Menagerie.TYPES.get("wildlife_lion").create(h.getLevel(),EntitySpawnReason.COMMAND);
        var pig=net.minecraft.world.entity.EntityTypes.PIG.create(h.getLevel(),EntitySpawnReason.COMMAND);h.assertTrue(predator.canHunt(pig),"Wild prey remains huntable");
        var wolf=net.minecraft.world.entity.EntityTypes.WOLF.create(h.getLevel(),EntitySpawnReason.COMMAND);wolf.setTame(true,false);h.assertTrue(!predator.canHunt(wolf),"Vanilla tamed wolves are not prey");
        var pet=Menagerie.TYPES.get("companion_ferret").create(h.getLevel(),EntitySpawnReason.COMMAND);pet.tame(java.util.UUID.randomUUID());h.assertTrue(!predator.canHunt(pet),"Owned imported companions are not prey");h.succeed();
    }

    @GameTest(maxTicks=20)
    public void aquaticSpawnRulesRequireWaterRatherThanASolidFloor(GameTestHelper h){
        var type=Menagerie.TYPES.get("wildlife_bass");var at=h.absolutePos(new BlockPos(1,1,1));
        h.setBlock(new BlockPos(1,0,1),net.minecraft.world.level.block.Blocks.WATER);h.setBlock(new BlockPos(1,1,1),net.minecraft.world.level.block.Blocks.WATER);
        h.assertTrue(net.minecraft.world.entity.SpawnPlacements.checkSpawnRules(type,h.getLevel(),EntitySpawnReason.NATURAL,at,h.getLevel().getRandom()),"Fish can spawn in actual water");
        h.setBlock(new BlockPos(1,0,1),net.minecraft.world.level.block.Blocks.STONE);h.setBlock(new BlockPos(1,1,1),net.minecraft.world.level.block.Blocks.AIR);
        h.assertTrue(!net.minecraft.world.entity.SpawnPlacements.checkSpawnRules(type,h.getLevel(),EntitySpawnReason.NATURAL,at,h.getLevel().getRandom()),"Fish cannot spawn on land");h.succeed();
    }
    @GameTest(maxTicks=20)
    public void everySpeciesConstructsBreedsAndSaves(GameTestHelper h){
        h.assertTrue(Menagerie.TYPES.size()==277,"Complete native roster: original 265 plus twelve recovered undead species");
        for(var entry:Menagerie.TYPES.entrySet()){
            var type=entry.getValue();var a=type.create(h.getLevel(),EntitySpawnReason.COMMAND);var b=type.create(h.getLevel(),EntitySpawnReason.COMMAND);
            h.assertTrue(a!=null&&b!=null,"Construct "+entry.getKey());
            a.variant(a.species().variants().size()-1);a.setAge(0);b.setAge(0);var keeper=java.util.UUID.randomUUID();a.tame(keeper);b.tame(keeper);a.sit(true);
            h.assertTrue(a.isFood(new ItemStack(Menagerie.feed(a.species())))!=a.species().mood().equals("hostile")&&!Menagerie.feed(a.species()).equals(Items.AIR),"Food only breeds non-hostile wildlife: "+entry.getKey());
            var child=(MenagerieAnimal)a.getBreedOffspring(h.getLevel(),b);h.assertTrue(a.species().mood().equals("hostile")?child==null:child!=null&&child.getType()==type&&keeper.equals(child.owner()),"Same-species offspring or non-breedable enemy: "+entry.getKey());
            var out=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,h.getLevel().registryAccess());a.saveWithoutId(out);
            b.load(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),out.buildResult()));
            h.assertTrue(b.variant()==a.variant()&&b.sitting()&&keeper.equals(b.owner()),"Variant, owner, stay survive save "+entry.getKey());
        }h.succeed();
    }
    @GameTest(maxTicks=30)
    public void netPreservesOneAnimalAndRejectsOtherOwner(GameTestHelper h){
        var p=survivalPlayer(h);var q=h.makeMockServerPlayerInLevel();
        var a=Menagerie.TYPES.get("wildlife_butterfly").create(h.getLevel(),EntitySpawnReason.COMMAND);var pos=h.absolutePos(new BlockPos(1,1,1));a.snapTo(pos.getX()+.5,pos.getY()+1,pos.getZ()+.5,0,0);a.tame(p.getUUID());a.variant(1);a.setAge(-1000);h.getLevel().addFreshEntity(a);
        q.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Menagerie.item("wildlife_capture_net")));
        h.assertTrue(!MenagerieItems.capture(a,q,InteractionHand.MAIN_HAND)&&a.isAlive(),"Cannot capture another player's pet");
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Menagerie.item("wildlife_capture_net")));
        h.assertTrue(MenagerieItems.capture(a,p,InteractionHand.MAIN_HAND)&&a.isRemoved(),"Successful capture removes original once");
        ItemStack caught=ItemStack.EMPTY;for(int i=0;i<36;i++){var stack=p.getInventory().getItem(i);if(stack.is(Menagerie.item("wildlife_butterfly")))caught=stack;}
        h.assertTrue(!caught.isEmpty()&&caught.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getStringOr("Keeper","").equals(p.getUUID().toString()),"Stored pet retains owner");
        p.getInventory().removeItemNoUpdate(p.getInventory().findSlotMatchingItem(caught));p.setItemInHand(InteractionHand.MAIN_HAND,caught);
        var result=caught.useOn(new UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false)));
        h.assertTrue(result.consumesAction()&&p.getMainHandItem().isEmpty(),"Release consumes the stored animal");
        var animals=h.getLevel().getEntitiesOfClass(MenagerieAnimal.class,new AABB(pos).inflate(3),m->m.ownedBy(p));
        h.assertTrue(animals.size()==1&&animals.getFirst().isBaby()&&animals.getFirst().variant()==a.variant(),"Exactly one matching baby released");h.succeed();
    }
    @GameTest(maxTicks=20)
    public void survivalBridgesAndEquipmentLoad(GameTestHelper h){
        for(String name:java.util.List.of("menagerie_field_guide","wildlife_hide_leather","wildlife_fur_string","wildlife_gland_fermented_eye","brewing/wildlife_antivenom"))h.assertTrue(h.getLevel().recipeAccess().byKey(ResourceKey.create(Registries.RECIPE,Jugcraft.id(name))).isPresent(),"Recipe loads: "+name);
        for(String part:java.util.List.of("helmet","chestplate","leggings","boots"))h.assertTrue(new ItemStack(Menagerie.item("pocket_solstice_"+part)).has(DataComponents.EQUIPPABLE),"Solstice gear wearable: "+part);
        h.assertTrue(new ItemStack(Menagerie.item("wildlife_music_disc_wild_ones")).has(DataComponents.JUKEBOX_PLAYABLE),"Wildlife disc is playable");h.succeed();
    }
    @GameTest(maxTicks=25)
    public void saddlebagsAndArmorPersistAndStayPrivate(GameTestHelper h){
        var p=survivalPlayer(h);var pos=h.absolutePos(new BlockPos(1,1,1));p.snapTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,0,0);
        var type=Menagerie.TYPES.get("mount_direwolf");var a=type.create(h.getLevel(),EntitySpawnReason.COMMAND);a.snapTo(p.getX(),p.getY(),p.getZ()+1,0,0);a.tame(p.getUUID());a.setNoAi(true);h.getLevel().addFreshEntity(a);
        for(var item:java.util.List.of(Items.SADDLE,Items.CHEST,Items.IRON_HORSE_ARMOR)){p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(item));a.mobInteract(p,InteractionHand.MAIN_HAND);h.assertTrue(p.getMainHandItem().isEmpty(),"Equipment consumes one item");}
        h.assertTrue(a.saddled()&&a.chested()&&a.armor().equals("iron"),"Mount equipped");
        p.setShiftKeyDown(true);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.STICK));a.mobInteract(p,InteractionHand.MAIN_HAND);
        h.assertTrue(p.containerMenu instanceof net.minecraft.world.inventory.ChestMenu,"Owner opens native saddlebags");
        p.getInventory().setItem(9,new ItemStack(Items.DIAMOND,7));p.containerMenu.quickMoveStack(p,27);
        h.assertTrue(p.getInventory().countItem(Items.DIAMOND)==0&&p.containerMenu.getSlot(0).getItem().getCount()==7,"Deposit transfers without duplication");p.closeContainer();
        var out=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,h.getLevel().registryAccess());a.saveWithoutId(out);a.discard();
        var restored=type.create(h.getLevel(),EntitySpawnReason.COMMAND);restored.load(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),out.buildResult()));h.getLevel().addFreshEntity(restored);
        h.assertTrue(restored.saddled()&&restored.chested()&&restored.armor().equals("iron"),"Equipment survives save");
        var other=h.makeMockServerPlayerInLevel();other.setShiftKeyDown(true);other.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.STICK));restored.mobInteract(other,InteractionHand.MAIN_HAND);
        h.assertTrue(other.containerMenu==other.inventoryMenu,"Stranger cannot open cargo");restored.mobInteract(p,InteractionHand.MAIN_HAND);
        h.assertTrue(p.containerMenu.getSlot(0).getItem().is(Items.DIAMOND)&&p.containerMenu.getSlot(0).getItem().getCount()==7,"Cargo survives save exactly once");p.closeContainer();h.succeed();
    }

    @GameTest(maxTicks=20)
    public void furnitureConstructionConsumesExactMaterialsAndNeverDuplicatesPreviews(GameTestHelper h){
        var p=survivalPlayer(h);p.getInventory().clearContent();
        var loaded=h.getLevel().recipeAccess().byKey(ResourceKey.create(Registries.RECIPE,Jugcraft.id("household/constructing/oak_chair"))).orElseThrow();
        var recipe=(HouseholdConstruction)loaded.value();p.getInventory().setItem(0,new ItemStack(Items.OAK_PLANKS,3));
        h.assertTrue(recipe.isSpecial(),"Workbench recipes stay out of vanilla grid placement while remaining available to construction");
        h.assertTrue(!recipe.craft(p)&&p.getInventory().countItem(Items.OAK_PLANKS)==3,"Incomplete materials stay untouched");
        p.getInventory().setItem(1,new ItemStack(Items.OAK_PLANKS,2));h.assertTrue(recipe.craft(p),"Split input stacks fund one craft");
        h.assertTrue(p.getInventory().countItem(Items.OAK_PLANKS)==1&&p.getInventory().countItem(Menagerie.item("household_oak_chair"))==1,"Exactly four planks produce one chair");
        h.assertTrue(!recipe.craft(p)&&p.getInventory().countItem(Menagerie.item("household_oak_chair"))==1,"Repeating without inputs cannot copy preview output");h.succeed();
    }
    @GameTest(maxTicks=20)
    public void capsuleConversionReservesOutputAndPersistsItsInventory(GameTestHelper h){
        var at=h.absolutePos(new BlockPos(1,1,1));h.getLevel().setBlockAndUpdate(at,Menagerie.BLOCKS.get("beast_capsid").defaultBlockState());
        var be=(BeastCapsid)h.getLevel().getBlockEntity(at);be.setItem(0,new ItemStack(Items.COD,2));be.setItem(1,new ItemStack(Items.AMETHYST_SHARD));be.setItem(2,new ItemStack(Items.STONE,64));
        for(int n=0;n<130;n++)BeastCapsid.tick(h.getLevel(),at,be.getBlockState(),be);
        h.assertTrue(be.getItem(0).getCount()==2&&be.getItem(1).getCount()==1,"Blocked output consumes no input or fuel");
        be.setItem(2,ItemStack.EMPTY);for(int n=0;n<120;n++)BeastCapsid.tick(h.getLevel(),at,be.getBlockState(),be);
        h.assertTrue(be.getItem(0).getCount()==1&&be.getItem(2).is(Menagerie.item("beast_cosmic_cod"))&&be.getItem(2).getCount()==1,"One conversion produces one real output");
        var saved=be.saveWithoutMetadata(h.getLevel().registryAccess());be.clearContent();be.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),saved));
        h.assertTrue(be.getItem(0).getCount()==1&&be.getItem(2).getCount()==1,"Actual machine inventory survives serialized reload");h.succeed();
    }
    @GameTest(maxTicks=20)
    public void newVehiclesArrowsAndEveryNewBlockCreateTheirNativeRuntimeObjects(GameTestHelper h){
        var at=h.absolutePos(new BlockPos(1,1,1));
        for(String plant:java.util.List.of("landscape_cattail","landscape_blue_hydrangea","cavern_curly_fern"))h.assertTrue(Menagerie.BLOCKS.get(plant) instanceof net.minecraft.world.level.block.DoublePlantBlock,"Tall owner flora uses paired native placement: "+plant);
        h.assertTrue(Menagerie.BLOCKS.get("cavern_flytrap").defaultBlockState().hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.OPEN),"Flytrap keeps its open model state");
        h.assertTrue(Menagerie.BLOCKS.get("cavern_nuclear_furnace") instanceof net.minecraft.world.level.block.BlastFurnaceBlock,"Legacy single furnace retains actual furnace gameplay");
        for(var entry:Menagerie.BLOCKS.entrySet())if(java.util.List.of("household_","ecology_","beast_","landscape_","cavern_").stream().anyMatch(entry.getKey()::startsWith)){
            var block=entry.getValue();if(block instanceof net.minecraft.world.level.block.EntityBlock factory){var be=factory.newBlockEntity(at,block.defaultBlockState());h.assertTrue(be!=null,"Entity block must create a real saved object: "+entry.getKey());h.assertTrue(be.getType().isValid(block.defaultBlockState()),"Block entity supports "+entry.getKey());}
        }
        for(var type:java.util.List.of(CavernSubmarine.TYPE,CavernSubmarine.BOARD)){var e=type.create(h.getLevel(),EntitySpawnReason.COMMAND);h.assertTrue(e!=null,"Vehicle constructs");var out=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,h.getLevel().registryAccess());e.saveWithoutId(out);var copy=type.create(h.getLevel(),EntitySpawnReason.COMMAND);copy.load(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),out.buildResult()));h.assertTrue(copy.board()==e.board(),"Vehicle identity survives reload");}
        var owner=h.makeMockServerPlayerInLevel();for(String name:OwnerArrow.TYPES.keySet()){var ammo=new ItemStack(Menagerie.item(name));var arrow=((ArrowItem)ammo.getItem()).createArrow(h.getLevel(),ammo,owner,new ItemStack(Items.BOW));h.assertTrue(arrow.getType()==OwnerArrow.TYPES.get(name)&&arrow.getPickupItemStackOrigin().is(ammo.getItem()),"Native arrow preserves ammunition: "+name);}h.succeed();
    }
    @GameTest(maxTicks=20)
    public void ownerRecipeAcquisitionAndCaveBiomesSurviveTheRealDataReload(GameTestHelper h){
        for(String name:java.util.List.of("cavern_limestone_spear","cavern_frostmint_spear")){
            var recipe=h.getLevel().recipeAccess().byKey(ResourceKey.create(Registries.RECIPE,Jugcraft.id(name))).orElseThrow();
            var result=((net.minecraft.world.item.crafting.ShapedRecipe)recipe.value()).assemble(net.minecraft.world.item.crafting.CraftingInput.EMPTY);
            h.assertTrue(result.getCount()==1&&result.getCount()<=result.getMaxStackSize(),"Durable spear recipe yields one valid weapon: "+name);
        }
        for(String name:java.util.List.of("cavern_nuclear_furnace_component","cavern_uranium_rod","armortrim/cavern_polarity",
            "household/workbench_bootstrap","ecology/maple_pancakes_from_cooking_pot","ecology/cutting/maple_pie","mutant/chemical_x"))
            h.assertTrue(h.getLevel().recipeAccess().byKey(ResourceKey.create(Registries.RECIPE,Jugcraft.id(name))).isPresent(),"Native acquisition recipe loads: "+name);
        var biomes=h.getLevel().registryAccess().lookupOrThrow(Registries.BIOME);
        for(String cave:CaveWorkshop.CAVES)h.assertTrue(biomes.get(ResourceKey.create(Registries.BIOME,Jugcraft.id("cavern_"+cave))).isPresent(),"Cave biome loads: "+cave);
        h.assertTrue(h.getLevel().registryAccess().lookupOrThrow(Registries.TRIM_PATTERN).get(ResourceKey.create(Registries.TRIM_PATTERN,Jugcraft.id("cavern_polarity"))).isPresent(),"Polarity pattern is a real smithing pattern");h.succeed();
    }
    @GameTest(maxTicks=20)
    public void everyFaunaHabitatVariantHasAWorkingSurvivalCaptureRoute(GameTestHelper h){
        var p=survivalPlayer(h);
        try{
            for(String species:java.util.List.of("butterfly","dragonfly","pixie")){
                var spec=Menagerie.SPECIES.get("fauna_"+species);
                for(int variant=0;variant<spec.variants().size();variant++){
                    p.getInventory().clearContent();p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Menagerie.item("fauna_bug_net")));
                    String empty=variant%2==0?"fauna_bug_habitat_empty":"fauna_bug_habitat";
                    p.getInventory().setItem(9,new ItemStack(Menagerie.item(empty)));
                    var a=Menagerie.TYPES.get(spec.id()).create(h.getLevel(),EntitySpawnReason.COMMAND);
                    a.variant(variant);a.snapTo(p.getX()+1,p.getY(),p.getZ(),0,0);h.getLevel().addFreshEntity(a);
                    h.assertTrue(MenagerieItems.capture(a,p,InteractionHand.MAIN_HAND),"Capture succeeds for "+spec.id()+"/"+variant);
                    h.assertTrue(a.isRemoved(),"One animal is removed after capture");
                    var expected=Menagerie.item("fauna_bug_habitat_"+spec.variants().get(variant).name()+"_"+species);
                    int count=0;for(int slot=0;slot<36;slot++){
                        var stack=p.getInventory().getItem(slot);
                        if(stack.is(expected)){count+=stack.getCount();h.assertTrue(stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getIntOr("Variant",-1)==variant,"Variant is saved");}
                        h.assertTrue(!stack.is(Menagerie.item(empty)),"Empty habitat is consumed exactly once");
                    }
                    h.assertTrue(count==1,"Exactly one matching habitat is produced");
                    h.assertTrue(p.getMainHandItem().is(Menagerie.item("fauna_bug_net")),"Net is retained");
                }
            }
        }finally{h.getLevel().getServer().getPlayerList().remove(p);}
        h.succeed();
    }
    @GameTest(maxTicks=20)
    public void caveEquipmentNeverLoadsAnUnloadedDestination(GameTestHelper h){
        var p=h.makeMockServerPlayerInLevel();var before=p.position();var at=new BlockPos(16000000,80,16000000);
        h.assertTrue(!h.getLevel().hasChunkAt(at),"Test destination starts unloaded");
        try{
            p.setPos(Vec3.atCenterOf(at));Caverns.tickEquipment(p);
            h.assertTrue(!h.getLevel().hasChunkAt(at),"Environmental equipment tick must not generate its player's destination");
        }finally{p.setPos(before);}
        h.succeed();
    }

    @GameTest(maxTicks=20)
    public void suppliedDecorStatesConnectAndRespondToPower(GameTestHelper h){
        var at=new BlockPos(1,1,1);var next=at.east();
        for(String name:java.util.List.of("beast_rainbow_glass","cavern_depth_glass")){
            var glass=Menagerie.BLOCKS.get(name);h.setBlock(at,glass);h.setBlock(next,glass);
            h.assertTrue(h.getBlockState(at).getValue(net.minecraft.world.level.block.PipeBlock.EAST)&&h.getBlockState(next).getValue(net.minecraft.world.level.block.PipeBlock.WEST),"Adjacent glass connects on both faces after command placement: "+name);
            h.setBlock(next,net.minecraft.world.level.block.Blocks.AIR);
            h.assertTrue(!h.getBlockState(at).getValue(net.minecraft.world.level.block.PipeBlock.EAST),"Removing glass restores its edge: "+name);
            h.setBlock(at,net.minecraft.world.level.block.Blocks.AIR);
        }
        for(String kind:java.util.List.of("stairs","slab","wall")){
            var block=Menagerie.BLOCKS.get("cavern_abyssmarine_brick_"+kind);
            h.assertTrue(!block.defaultBlockState().getValue(io.github.jimbozoomer.jugcraft.diagonal.PoweredDiagonalWallBlock.ACTIVE),"Unpowered masonry starts inactive: "+kind);
            h.setBlock(at,block);h.setBlock(next,net.minecraft.world.level.block.Blocks.REDSTONE_BLOCK);
            var state=h.getBlockState(at);var active=(net.minecraft.world.level.block.state.properties.BooleanProperty)block.getStateDefinition().getProperty("active");
            h.assertTrue(state.getValue(active),"Redstone enables supplied active masonry: "+kind);
            h.setBlock(next,net.minecraft.world.level.block.Blocks.AIR);
            h.assertTrue(!h.getBlockState(at).getValue(active),"Removing power disables masonry: "+kind);h.setBlock(at,net.minecraft.world.level.block.Blocks.AIR);
        }
        h.assertTrue(Menagerie.BLOCKS.get("ecology_thin_ice") instanceof net.minecraft.world.level.block.FrostedIceBlock,"Thin ice uses native aging/melting states");
        var wall=Menagerie.BLOCKS.get("cavern_abyssmarine_brick_wall");
        var diagonal=io.github.jimbozoomer.jugcraft.diagonal.DiagonalWalls.of(wall);
        var active=io.github.jimbozoomer.jugcraft.diagonal.PoweredDiagonalWallBlock.ACTIVE;
        var lit=diagonal.defaultBlockState().setValue(active,true);
        h.assertTrue(lit.getLightEmission()==6&&io.github.jimbozoomer.jugcraft.diagonal.DiagonalWalls.toWall(lit).getValue(active),"Diagonal masonry retains its powered light and state when becoming a normal wall");
        h.assertTrue(Menagerie.BLOCKS.get("cavern_potted_flytrap").defaultBlockState().hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.OPEN),"Potted flytrap preserves both supplied poses");
        h.assertTrue(Menagerie.BLOCKS.get("landscape_bramble_leaves").defaultBlockState().hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING),"Bramble leaves retain directional models");
        for(String name:java.util.List.of("landscape_purple_wildflowers","landscape_white_petals")){
            var block=Menagerie.BLOCKS.get(name);
            h.assertTrue(block instanceof net.minecraft.world.level.block.FlowerBedBlock,"Petal patches use native stacking and bonemeal behavior: "+name);
            var flower=(net.minecraft.world.level.block.FlowerBedBlock)block;
            h.setBlock(at.below(),net.minecraft.world.level.block.Blocks.DIRT);h.setBlock(at,flower);
            h.assertTrue(h.getBlockState(at).getValue(net.minecraft.world.level.block.FlowerBedBlock.AMOUNT)==1,"A new patch starts with one segment");
            flower.performBonemeal(h.getLevel(),h.getLevel().getRandom(),h.absolutePos(at),h.getBlockState(at),net.minecraft.world.level.block.BonemealSource.INTERACTION);
            h.assertTrue(h.getBlockState(at).getValue(net.minecraft.world.level.block.FlowerBedBlock.AMOUNT)==2,"Bonemeal adds a second petal segment");
            h.setBlock(at,net.minecraft.world.level.block.Blocks.AIR);
        }
        h.succeed();
    }

    @GameTest(maxTicks=20)
    public void candyLootRetainsEveryCreaturesColor(GameTestHelper h){
        var animal=Menagerie.TYPES.get("cavern_gummy_bear").create(h.getLevel(),EntitySpawnReason.COMMAND);
        var table=h.getLevel().getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,Jugcraft.id("entities/cavern_gummy_bear")));
        h.assertTrue(table!=net.minecraft.world.level.storage.loot.LootTable.EMPTY,"Candy loot table actually loaded");
        for(int variant=0;variant<animal.species().variants().size();variant++){
            animal.variant(variant);String name=animal.species().variants().get(variant).name(),color=name.substring(name.lastIndexOf('_')+1);int count=0;
            var params=new net.minecraft.world.level.storage.loot.LootParams.Builder(h.getLevel())
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,animal.position())
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.THIS_ENTITY,animal)
                .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.GIFT);
            for(int roll=0;roll<24;roll++)for(var stack:table.getRandomItems(params)){
                if(stack.isEmpty())continue; // The supplied 0..3 / 0..1 pools can legitimately roll no drop.
                String id=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
                h.assertTrue(id.equals("cavern_gelatin_"+color)||id.equals("cavern_sweetish_fish_"+color),"Candy follows saved creature color: "+name+" dropped "+id);
                h.assertTrue(stack.getCount()>0&&stack.getCount()<=3,"Loot keeps its configured count");count+=stack.getCount();
            }
            h.assertTrue(count>0,"Colored candy is obtainable: "+name);
        }
        h.succeed();
    }

    @GameTest(maxTicks=20)
    public void goblinRingRequiresFuelAndCannotRepeatItsHealingDuringCooldown(GameTestHelper h){
        var p=survivalPlayer(h);p.getInventory().clearContent();p.setHealth(10);
        var ring=new ItemStack(Menagerie.item("landscape_ring_of_health"));p.setItemInHand(InteractionHand.MAIN_HAND,ring);
        var unfunded=ring.getItem().use(p.level(),p,InteractionHand.MAIN_HAND);
        h.assertTrue(!unfunded.consumesAction()&&p.getHealth()==10,"No free healing without fuel: result="+unfunded+", health="+p.getHealth()+", creative="+p.isCreative()+", item="+ring.getItem().getClass().getName());
        p.getInventory().setItem(9,new ItemStack(Items.AMETHYST_SHARD,2));
        h.assertTrue(ring.getItem().use(p.level(),p,InteractionHand.MAIN_HAND).consumesAction()&&p.getHealth()==16,"One paid activation heals six");
        h.assertTrue(p.getInventory().countItem(Items.AMETHYST_SHARD)==1&&p.getCooldowns().isOnCooldown(ring),"Consumes one shard and starts cooldown");
        h.assertTrue(!ring.getItem().use(p.level(),p,InteractionHand.MAIN_HAND).consumesAction()&&p.getInventory().countItem(Items.AMETHYST_SHARD)==1&&p.getHealth()==16,"Repeated use cannot bypass cost or cooldown");h.succeed();
    }

    @GameTest(maxTicks=20)
    public void regionalOverlaysPreserveRareSurfaceIslands(GameTestHelper h){
        var registry=h.getLevel().registryAccess().lookupOrThrow(Registries.BIOME);
        var choices=new LandscapeBiomes.Choices(registry.listElements().collect(java.util.stream.Collectors.toSet()));
        var target=net.minecraft.world.level.biome.Climate.target(.4F,.2F,0,0,0,0);
        for(var key:java.util.List.of(net.minecraft.world.level.biome.Biomes.MUSHROOM_FIELDS,ResourceKey.create(Registries.BIOME,Jugcraft.id("flower_isle")))){
            var original=registry.getOrThrow(key);
            for(int cell=-32;cell<=32;cell++)h.assertTrue(choices.pick(original,12345L,cell*192,0,target).is(key),"Rare island survives regional overlay: "+key);
        }
        for(String name:java.util.List.of("landscape_maple_leaf_litter","landscape_webbing"))
            h.assertTrue(!Menagerie.BLOCKS.get(name).defaultBlockState().hasBlockEntity(),"Ordinary foliage has no phantom block entity: "+name);
        h.succeed();
    }
}

