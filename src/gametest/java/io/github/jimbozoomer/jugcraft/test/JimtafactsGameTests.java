package io.github.jimbozoomer.jugcraft.test;

import eu.pb4.trinkets.api.SlotAttributes;
import eu.pb4.trinkets.api.TrinketsApi;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.jimtafacts.*;
import io.github.jimbozoomer.jugcraft.grave.*;
import io.github.jimbozoomer.jugcraft.server.PlayerRules;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Integration with real Trinkets slots, damage events, vanilla attributes and native grave capture. */
public class JimtafactsGameTests {
    private static ServerPlayer player(GameTestHelper h) {
        var p = h.makeMockServerPlayerInLevel(); p.setGameMode(GameType.SURVIVAL); p.getInventory().clearContent();
        p.snapTo(Vec3.atCenterOf(h.absolutePos(new BlockPos(2, 2, 2)))); return p;
    }
    private static ItemStack equip(ServerPlayer p, String name) {
        var item = (Jimtafacts.Accessory) Jimtafacts.ITEMS.get(name);
        var stack = new ItemStack(item); TrinketsApi.getAttachment(p).getInventory(item.slot).setItem(0, stack); return stack;
    }
    private static void unequip(ServerPlayer p, String name) {
        var item = (Jimtafacts.Accessory) Jimtafacts.ITEMS.get(name);
        TrinketsApi.getAttachment(p).getInventory(item.slot).setItem(0, ItemStack.EMPTY);
    }

    @GameTest public void everyRegisteredAccessoryHasValidSlotAttributesAndNoResearchGate(GameTestHelper h) {
        var p = player(h);
        h.assertTrue(Jimtafacts.ITEMS.size() == 49, "All 49 supplied accessory, food, umbrella and egg items registered");
        Jimtafacts.ITEMS.forEach((name, item) -> {
            h.assertTrue(BuiltInRegistries.ITEM.getValue(Jugcraft.id("jimta_" + name)) == item, "Stable registered item");
            if (!(item instanceof Jimtafacts.Accessory accessory)) return;
            var inv = TrinketsApi.getAttachment(p).getInventory(accessory.slot);
            h.assertTrue(inv != null && inv.getContainerSize() > 0, "Slot exists: " + accessory.slot);
            var stack = new ItemStack(item); var slot = inv.getOrCreateSlotAccess(0);
            h.assertTrue(stack.is(TagKey.create(Registries.ITEM, Identifier.parse("trinkets:" + accessory.slot))), "Slot accepts " + name);
            h.assertTrue(accessory.canEquip(stack, slot, p), "No research prerequisite for " + name);
            accessory.forEachTrinketModifier(stack, slot, p, SlotAttributes.getIdentifier(slot), (attribute, modifier) ->
                    h.assertTrue(attribute.isBound() && Double.isFinite(modifier.amount()), "Valid native attribute for " + name));
        }); h.succeed();
    }

    @GameTest public void carriedItemsGiveNothingButEquippedItemsWork(GameTestHelper h) {
        var p = player(h); p.getInventory().setItem(0, new ItemStack(Jimtafacts.ITEMS.get("snorkel")));
        Jimtafacts.tick(p); h.assertTrue(!p.hasEffect(MobEffects.WATER_BREATHING), "Carried accessory does nothing");
        equip(p, "snorkel"); Jimtafacts.tick(p);
        h.assertTrue(p.hasEffect(MobEffects.WATER_BREATHING), "Equipped snorkel works");
        unequip(p, "snorkel"); p.removeEffect(MobEffects.WATER_BREATHING); Jimtafacts.tick(p);
        h.assertTrue(!p.hasEffect(MobEffects.WATER_BREATHING), "Unequipped snorkel stops"); h.succeed();
    }

    @GameTest public void runningShoesRemoveTheirModifiersWhenTakenOff(GameTestHelper h) {
        var p = player(h); double speed = p.getAttributeValue(Attributes.MOVEMENT_SPEED);
        equip(p, "running_shoes"); p.setSprinting(true); double sprint = p.getAttributeValue(Attributes.MOVEMENT_SPEED);
        Jimtafacts.tick(p); h.assertTrue(p.getAttributeValue(Attributes.MOVEMENT_SPEED) > sprint, "Sprint speed increased");
        unequip(p, "running_shoes"); Jimtafacts.tick(p);
        h.assertTrue(Math.abs(p.getAttributeValue(Attributes.MOVEMENT_SPEED) - sprint) < .000001, "No leaked speed modifier");
        p.setSprinting(false); Jimtafacts.tick(p); h.assertTrue(Math.abs(p.getAttributeValue(Attributes.MOVEMENT_SPEED) - speed) < .000001, "Original speed restored"); h.succeed();
    }

    @GameTest public void antidoteIsBoundedAndDoesNotEraseBeneficialEffects(GameTestHelper h) {
        var p = player(h); equip(p, "antidote_vessel");
        p.addEffect(new MobEffectInstance(MobEffects.POISON, 100)); p.addEffect(new MobEffectInstance(MobEffects.WITHER, 100));
        p.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 100)); Jimtafacts.tick(p);
        h.assertTrue(!p.hasEffect(MobEffects.POISON) && !p.hasEffect(MobEffects.WITHER) && p.hasEffect(MobEffects.STRENGTH), "Only the two harmful effects removed");
        p.addEffect(new MobEffectInstance(MobEffects.POISON, 100)); Jimtafacts.tick(p);
        h.assertTrue(p.hasEffect(MobEffects.POISON), "Cooldown prevents continuous cleansing"); h.succeed();
    }

    @GameTest public void foodSurvivesEatingAndCannotBypassCooldown(GameTestHelper h) {
        var p = player(h); var food = Jimtafacts.ITEMS.get("eternal_steak"); var stack = new ItemStack(food);
        p.getFoodData().setFoodLevel(1); var returned = food.finishUsingItem(stack, h.getLevel(), p);
        h.assertTrue(returned == stack && stack.getCount() == 1 && p.getFoodData().getFoodLevel() == 9, "Reusable meal feeds and remains");
        food.finishUsingItem(new ItemStack(food), h.getLevel(), p);
        h.assertTrue(p.getFoodData().getFoodLevel() == 9, "Another copy cannot bypass item cooldown"); h.succeed();
    }

    @GameTest public void explorationLootAndCookingMakeTheItemsReachable(GameTestHelper h) {
        var input = new net.minecraft.world.item.crafting.SingleRecipeInput(new ItemStack(Jimtafacts.ITEMS.get("everlasting_beef")));
        for (var type : List.of(net.minecraft.world.item.crafting.RecipeType.SMELTING, net.minecraft.world.item.crafting.RecipeType.SMOKING,
                net.minecraft.world.item.crafting.RecipeType.CAMPFIRE_COOKING)) {
            var found = h.getLevel().recipeAccess().getRecipeFor(type, input, h.getLevel());
            h.assertTrue(found.isPresent() && found.get().value().assemble(input).is(Jimtafacts.ITEMS.get("eternal_steak")), "Native cooking route loads: " + type);
        }
        var table = h.getLevel().getServer().reloadableRegistries().getLootTable(net.minecraft.resources.ResourceKey.create(Registries.LOOT_TABLE,
                Identifier.withDefaultNamespace("chests/simple_dungeon")));
        var params = new net.minecraft.world.level.storage.loot.LootParams.Builder(h.getLevel())
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, Vec3.ZERO)
                .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.CHEST);
        int rewards = 0;
        for (long seed = 1; seed <= 128; seed++) {
            long count = table.getRandomItems(params, seed).stream().filter(s -> Jimtafacts.ITEMS.containsValue(s.getItem())).mapToInt(ItemStack::getCount).sum();
            h.assertTrue(count <= 1, "Only one accessory at most per chest"); rewards += count;
        }
        h.assertTrue(rewards > 0, "Existing dungeon loot can actually produce the new accessories"); h.succeed();
    }

    @GameTest public void meleeEffectsRequireDamageAndDoNotHealWithoutBound(GameTestHelper h) {
        var p = player(h); var foe = h.spawn(EntityTypes.ZOMBIE, new BlockPos(3, 2, 2));
        equip(p, "vampiric_glove"); p.setHealth(10);
        Jimtafacts.afterDamage(foe, p.damageSources().indirectMagic(p, p), 100, 100, false);
        h.assertTrue(p.getHealth() == 10, "Player-owned spell damage is not a melee strike");
        Jimtafacts.afterDamage(foe, p.damageSources().playerAttack(p), 100, 0, true);
        h.assertTrue(p.getHealth() == 10, "Blocked hit cannot heal");
        Jimtafacts.afterDamage(foe, p.damageSources().playerAttack(p), 100, 100, false);
        h.assertTrue(p.getHealth() == 12, "Healing capped at two");
        Jimtafacts.afterDamage(foe, p.damageSources().playerAttack(p), 100, 100, false);
        h.assertTrue(p.getHealth() == 12, "Per-second cooldown enforced");
        equip(p, "fire_gauntlet"); Jimtafacts.afterDamage(foe, p.damageSources().playerAttack(p), 1, 1, false);
        h.assertTrue(foe.isOnFire(), "Fire gauntlet ignites real permitted target"); h.succeed();
    }

    @GameTest public void reactionEffectsRespectTriggersAndCooldowns(GameTestHelper h) {
        var p = player(h); equip(p, "panic_necklace");
        ServerLivingEntityEvents.AFTER_DAMAGE.invoker().afterDamage(p, p.damageSources().generic(), 1, 1, false);
        h.assertTrue(p.hasEffect(MobEffects.SPEED), "Damage event triggers panic necklace");
        p.removeEffect(MobEffects.SPEED); Jimtafacts.afterDamage(p, p.damageSources().generic(), 1, 1, false);
        h.assertTrue(!p.hasEffect(MobEffects.SPEED), "Panic cooldown enforced");
        equip(p, "obsidian_skull"); Jimtafacts.afterDamage(p, p.damageSources().generic(), 1, 1, false);
        h.assertTrue(!p.hasEffect(MobEffects.FIRE_RESISTANCE), "Nonfire damage does not trigger skull");
        Jimtafacts.afterDamage(p, p.damageSources().onFire(), 1, 1, false);
        h.assertTrue(p.hasEffect(MobEffects.FIRE_RESISTANCE), "Actual fire damage triggers skull"); h.succeed();
    }

    @GameTest public void magnetLeavesReservedAndDelayedDropsAlone(GameTestHelper h) {
        var p = player(h);
        var reserved = new ItemEntity(h.getLevel(), p.getX()+1, p.getY(), p.getZ(), new ItemStack(Items.DIAMOND));
        reserved.setTarget(UUID.randomUUID()); reserved.setNoPickUpDelay(); reserved.setDeltaMovement(Vec3.ZERO); h.getLevel().addFreshEntity(reserved);
        var delayed = new ItemEntity(h.getLevel(), p.getX()-1, p.getY(), p.getZ(), new ItemStack(Items.EMERALD));
        delayed.setPickUpDelay(100); delayed.setDeltaMovement(Vec3.ZERO); h.getLevel().addFreshEntity(delayed);
        Jimtafacts.attract(p);
        h.assertTrue(reserved.getDeltaMovement().equals(Vec3.ZERO) && delayed.getDeltaMovement().equals(Vec3.ZERO), "No movement of protected drops");
        reserved.discard(); delayed.discard(); h.succeed();
    }

    @GameTest public void secondaryAttacksCannotHarmPartyMembers(GameTestHelper h) {
        var first = player(h); var second = player(h);
        var parties = io.github.jimbozoomer.jugcraft.party.JugcraftParties.manager();
        parties.create(first.getUUID()); parties.invite(first.getUUID(), second.getUUID(), 0); parties.accept(second.getUUID(), 1);
        h.assertTrue(parties.sameParty(first.getUUID(), second.getUUID()), "Test party created");
        equip(first, "fire_gauntlet"); equip(second, "flame_pendant");
        Jimtafacts.afterDamage(second, first.damageSources().playerAttack(first), 2, 2, false);
        h.assertTrue(!first.isOnFire() && !second.isOnFire(), "Neither offensive nor retaliatory fire bypasses party protection");
        h.succeed();
    }

    @GameTest public void accessoriesEnterGravesExactlyOnceAndRespectPersonalKeepInventory(GameTestHelper h) {
        var p = player(h); equip(p, "crystal_heart");
        var grave = DeathGraveGameTests.kill(h, p);
        h.assertTrue(grave.count() == 1 && Jimtafacts.worn(p).isEmpty(), "Accessory stored once and removed from worn slot: count=" + grave.count() + ", worn=" + Jimtafacts.worn(p));
        p.setHealth(20); h.assertTrue(DeathGraves.recover(p, grave.id()) == 1, "Owner recovers accessory");
        h.assertTrue(p.getInventory().countItem(Jimtafacts.ITEMS.get("crystal_heart")) == 1, "No duplicated accessory");
        var kept = player(h); equip(kept, "crystal_heart"); PlayerRules.get(h.getLevel().getServer()).set(kept.getUUID(), true);
        kept.setHealth(0); kept.die(h.getLevel().damageSources().genericKill());
        h.assertTrue(Jimtafacts.worn(kept).contains("crystal_heart") && GraveStore.get(h.getLevel().getServer()).owned(kept.getUUID()).isEmpty(), "Personal keep rule preserves worn item without grave");
        PlayerRules.get(h.getLevel().getServer()).set(kept.getUUID(), false); h.succeed();
    }

    @GameTest public void cloudJumpIsEquipmentGatedAndOnlyOneExtraJump(GameTestHelper h) {
        var p=player(h);p.setOnGround(false);p.setDeltaMovement(Vec3.ZERO);
        JimtaAbilities.input(p,0);h.assertTrue(p.getDeltaMovement().equals(Vec3.ZERO),"No flight without equipment");
        equip(p,"cloud_in_a_bottle");JimtaAbilities.input(p,0);h.assertTrue(p.getDeltaMovement().y>.4,"Extra jump applies");
        p.setDeltaMovement(Vec3.ZERO);JimtaAbilities.input(p,0);h.assertTrue(p.getDeltaMovement().equals(Vec3.ZERO),"Spam cannot add another jump");
        h.succeed();
    }
    @GameTest public void chorusConsumesExactlyOneAndNeverInterceptsKill(GameTestHelper h) {
        var p=player(h);var item=equip(p,"chorus_totem");
        h.assertTrue(!JimtaAbilities.rescue(p,p.damageSources().genericKill())&&item.getCount()==1,"Kill remains authoritative");
        p.setHealth(1);h.assertTrue(JimtaAbilities.rescue(p,p.damageSources().generic()),"Equipped chorus rescues lethal hit");
        h.assertTrue(item.isEmpty()&&p.getHealth()==9,"Consumed once with source recovery health");
        h.assertTrue(!JimtaAbilities.rescue(p,p.damageSources().generic()),"No second rescue without another totem");h.succeed();
    }
    @GameTest public void fishingAndExperienceHooksRequireActuallyWornArtifacts(GameTestHelper h) {
        var p=player(h);var rod=new ItemStack(Items.FISHING_ROD);
        int base=net.minecraft.world.item.enchantment.EnchantmentHelper.getFishingLuckBonus(h.getLevel(),rod,p);
        equip(p,"anglers_hat");h.assertTrue(net.minecraft.world.item.enchantment.EnchantmentHelper.getFishingLuckBonus(h.getLevel(),rod,p)==base+1,"One level of fishing luck");
        h.assertTrue(net.minecraft.world.item.enchantment.EnchantmentHelper.getFishingTimeReduction(h.getLevel(),rod,p)==5,"One level of Lure");
        equip(p,"golden_hook");var victim=h.spawn(EntityTypes.ZOMBIE,1,1,1);
        h.assertTrue(net.minecraft.world.item.enchantment.EnchantmentHelper.processMobExperience(h.getLevel(),p,victim,10)==15,"Extra kill XP");h.succeed();
    }
    @GameTest public void fluidAndSnowAbilitiesDoNotGrantUnconditionalFlight(GameTestHelper h) {
        var p=player(h);equip(p,"aqua_dashers");p.setSprinting(false);
        var water=Blocks.WATER.defaultBlockState().getFluidState();var lava=Blocks.LAVA.defaultBlockState().getFluidState();
        h.assertTrue(!p.canStandOnFluid(water),"Must sprint on water");p.setSprinting(true);
        h.assertTrue(p.canStandOnFluid(water)&&!p.canStandOnFluid(lava),"Water running does not grant lava walking");
        equip(p,"strider_shoes");p.setSprinting(false);p.setShiftKeyDown(true);h.assertTrue(p.canStandOnFluid(lava),"Sneak on lava");
        equip(p,"snowshoes");h.assertTrue(net.minecraft.world.level.block.PowderSnowBlock.canEntityWalkOnPowderSnow(p),"Original snow-walking behavior");h.succeed();
    }
    @GameTest public void pickaxeHeaterUsesRealFurnaceRecipesAndClawsRespectHigherTiers(GameTestHelper h) {
        var p=player(h);var pos=h.absolutePos(new BlockPos(2,1,2));equip(p,"pickaxe_heater");
        var drops=net.minecraft.world.level.block.Block.getDrops(Blocks.IRON_ORE.defaultBlockState(),h.getLevel(),pos,null,p,new ItemStack(Items.DIAMOND_PICKAXE));
        h.assertTrue(!drops.isEmpty()&&drops.stream().allMatch(s->s.is(Items.IRON_INGOT)),"Mined raw iron is smelted once");
        equip(p,"digging_claws");p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,ItemStack.EMPTY);
        h.assertTrue(p.hasCorrectToolForDrops(Blocks.IRON_ORE.defaultBlockState()),"Stone harvest tier supplied by claws");
        h.assertTrue(!p.hasCorrectToolForDrops(Blocks.DIAMOND_ORE.defaultBlockState()),"Claws do not unlock iron-tier diamond ore");h.succeed();
    }
    @GameTest public void mimicAndCampsiteResourcesLoadTogether(GameTestHelper h) {
        h.assertTrue(h.getLevel().registryAccess().lookupOrThrow(Registries.PLACED_FEATURE).get(Jugcraft.id("jimtafacts/campsite")).isPresent(),"Native campsite placement exists");
        var mimic=h.spawn(JimtaMimic.TYPE,2,1,2);h.assertTrue(mimic.getMaxHealth()==60,"Recovered mimic stats");
        var loot=h.getLevel().getServer().reloadableRegistries().getLootTable(net.minecraft.resources.ResourceKey.create(Registries.LOOT_TABLE,Jugcraft.id("entities/jimta_mimic")));
        h.assertTrue(loot!=net.minecraft.world.level.storage.loot.LootTable.EMPTY,"Mimic rewards resolve");h.succeed();
    }
}
