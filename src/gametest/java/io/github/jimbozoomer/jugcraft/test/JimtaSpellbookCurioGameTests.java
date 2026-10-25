package io.github.jimbozoomer.jugcraft.test;

import eu.pb4.trinkets.api.TrinketsApi;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import io.github.jimbozoomer.jugcraft.concordance.rules.FocusPool;
import io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import java.util.UUID;

/** Deferred combined-run tests use actual Trinkets inventories and native effects/Focus. */
public final class JimtaSpellbookCurioGameTests {
    private static ServerPlayer player(GameTestHelper h) {
        var player = h.makeMockServerPlayerInLevel(); player.setGameMode(GameType.SURVIVAL); player.getInventory().clearContent(); return player;
    }
    private static ItemStack equip(ServerPlayer player, String name) {
        var item = JimtaSpellbookCurios.ITEMS.get(name); var stack = new ItemStack(item);
        TrinketsApi.getAttachment(player).getInventory(item.slot).setItem(0, stack); return stack;
    }
    private static void remove(ServerPlayer player, String name) {
        var item = JimtaSpellbookCurios.ITEMS.get(name); TrinketsApi.getAttachment(player).getInventory(item.slot).setItem(0, ItemStack.EMPTY);
    }
    @GameTest public void everySourceCurioFitsNativeSlotsWithoutKnowledgeRequirements(GameTestHelper h) {
        var p = player(h); h.assertTrue(JimtaSpellbookCurios.ITEMS.size() == 21, "Exact source curio set");
        JimtaSpellbookCurios.ITEMS.forEach((name, item) -> {
            var inv = TrinketsApi.getAttachment(p).getInventory(item.slot); var stack = new ItemStack(item);
            h.assertTrue(inv != null && inv.getContainerSize() > 0, "Existing slot for " + name);
            h.assertTrue(stack.is(TagKey.create(Registries.ITEM, Identifier.parse("trinkets:" + item.slot))), "Tagged for native slot " + name);
            h.assertTrue(item.canEquip(stack, inv.getOrCreateSlotAccess(0), p), "No research or power gate");
        }); h.succeed();
    }
    @GameTest public void extraFocusCapacityNeverCreatesFreeFocusOrStaysAfterRemoval(GameTestHelper h) {
        var p = player(h); ConcordanceProgress.setFocus(p, 7);
        p.getInventory().add(new ItemStack(JimtaSpellbookCurios.ITEMS.get("mana_ring")));
        h.assertTrue(ConcordanceProgress.focus(p).capacity() == FocusPool.MAX, "Carried ring has no effect");
        equip(p, "mana_ring"); h.assertTrue(ConcordanceProgress.focus(p).capacity() == 40 && ConcordanceProgress.currentFocus(p) == 7, "Capacity grows without free points");
        ConcordanceProgress.setFocus(p, 35); remove(p, "mana_ring");
        h.assertTrue(ConcordanceProgress.focus(p).capacity() == 20 && ConcordanceProgress.currentFocus(p) == 20, "Removing ring clamps stored Focus");
        equip(p, "mana_ring"); h.assertTrue(ConcordanceProgress.currentFocus(p) == 20, "Reequipping cannot restore clipped Focus"); h.succeed();
    }
    @GameTest public void wardsBlockOnlyTheirNamedHarmAndLeaveHelpfulEffects(GameTestHelper h) {
        var p = player(h); equip(p, "poisonward_ring");
        h.assertTrue(!p.addEffect(new MobEffectInstance(MobEffects.POISON, 200)), "Poison blocked before application");
        h.assertTrue(p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200)), "Beneficial effect still applies");
        remove(p, "poisonward_ring"); h.assertTrue(p.addEffect(new MobEffectInstance(MobEffects.POISON, 200)), "Unequipped ward no longer protects");
        equip(p, "visibility_ring"); p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 200));
        h.assertTrue(!p.hasEffect(MobEffects.BLINDNESS), "Blindness prevented");
        equip(p, "fireward_ring"); h.assertTrue(!JimtaSpellbookCurios.allowDamage(p, p.damageSources().inFire(), 4), "Fire blocked");
        h.assertTrue(JimtaSpellbookCurios.allowDamage(p, p.damageSources().fall(), 4), "Fall still damages"); h.succeed();
    }
    @GameTest public void affinityLootHonorsSchoolAndBindingConsumesRealScroll(GameTestHelper h) {
        var p = player(h); var ring = new ItemStack(JimtaSpellbookCurios.ITEMS.get("affinity_ring"));
        JimtaSpellbookCurios.randomizeRing(ring, RandomSource.create(72), "irons_spellbooks:fire");
        var chosen = JimtaSpellbookCurios.affinity(ring);
        h.assertTrue(!chosen.isEmpty() && JimtaSpellcasting.SPELLS.get(chosen).school().equals("fire"), "Loot school filter retained");
        h.assertTrue(ring.copy().get(DataComponents.CUSTOM_MODEL_DATA).equals(ring.get(DataComponents.CUSTOM_MODEL_DATA)), "Model and affinity components survive copying");
        p.setItemInHand(InteractionHand.MAIN_HAND, ring); p.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(JimtaSpellbooks.ITEMS.get("scroll_heal"), 2));
        ring.getItem().use(h.getLevel(), p, InteractionHand.MAIN_HAND);
        h.assertTrue(JimtaSpellbookCurios.affinity(ring).equals("heal") && p.getOffhandItem().getCount() == 1, "Attunement spends one real scroll");
        TrinketsApi.getAttachment(p).getInventory("hand/ring").setItem(0, ring);
        h.assertTrue(JimtaSpellbookCurios.spellLevelBonus(p, "heal") == 1 && JimtaSpellbookCurios.spellLevelBonus(p, "firebolt") == 0, "Only selected spell gains a level"); h.succeed();
    }
    @GameTest public void revealRingAndSpellTimingAreEquipmentOnly(GameTestHelper h) {
        var p = player(h); var mob = h.spawn(EntityTypes.ZOMBIE, new BlockPos(2, 2, 2)); mob.setInvisible(true);
        h.assertTrue(mob.isInvisibleTo(p), "Invisible without ring"); equip(p, "invisibility_ring");
        h.assertTrue(!mob.isInvisibleTo(p) && !p.isInvisible(), "Wearer sees invisible; does not become invisible");
        equip(p, "cooldown_ring"); h.assertTrue(JimtaSpellbookCurios.cooldownTicks(p, 200) == 170, "Source fifteen-percent cooldown");
        equip(p, "cast_time_ring"); h.assertTrue(Math.abs(JimtaSpellbookCurios.castDurationMultiplier(p) - .85) < .0001, "Source cast speed");
        remove(p, "cast_time_ring"); h.assertTrue(JimtaSpellbookCurios.cooldownTicks(p, 200) == 200 && JimtaSpellbookCurios.castDurationMultiplier(p) == 1, "No lingering timing bonuses"); h.succeed();
    }
    @GameTest public void invisibilityAttackBonusRespectsClaimsAndItsSharedCooldown(GameTestHelper h) {
        var p = player(h); equip(p, "lurker_ring"); p.setInvisible(true); var mob = h.spawn(EntityTypes.ZOMBIE, new BlockPos(2, 2, 2));
        var source = p.damageSources().playerAttack(p);
        try (var claim = TestClaims.open().creature(mob, UUID.randomUUID())) {
            h.assertTrue(JimtaSpellbookCurios.damage(mob, source, 10) == 10, "Protected target grants no bonus or cooldown proc");
        }
        h.assertTrue(JimtaSpellbookCurios.damage(mob, source, 10) == 15, "First permitted invisible strike gains source bonus");
        h.assertTrue(JimtaSpellbookCurios.damage(mob, source, 10) == 10, "Immediate next hit cannot reuse bonus");
        h.assertTrue(Math.abs(JimtaSpellbookCurios.betrayerBonus(40) - .19) < .0001, "Mana conversion retains diminishing steps in shared Focus units"); h.succeed();
    }
}
