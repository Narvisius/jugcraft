package io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks;

import eu.pb4.trinkets.api.TrinketDropRule;
import eu.pb4.trinkets.api.TrinketSlotAccess;
import eu.pb4.trinkets.api.TrinketsApi;
import eu.pb4.trinkets.api.callback.TrinketCallback;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceEffects;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import io.github.jimbozoomer.jugcraft.concordance.JugcraftConcordance;
import io.github.jimbozoomer.jugcraft.concordance.rules.FocusPool;
import io.github.jimbozoomer.jugcraft.weapons.TwoHanded;
import java.util.*;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/** Original accessory outcomes on native Trinkets, Focus, spell casting and party/claim authority. */
public final class JimtaSpellbookCurios {
    public static final Map<String, Curio> ITEMS = new LinkedHashMap<>();
    public static final List<String> SCHOOLS = List.of("evocation", "fire", "ice", "lightning", "holy", "ender", "blood", "nature", "eldritch");
    private static final Map<ServerPlayer, State> STATES = new WeakHashMap<>();
    private static final Map<String, String> AFFINITIES = new LinkedHashMap<>();
    private static final String AFFINITY = "jimtafacts_affinity_spell";
    private static final class State {
        final Map<String, Long> ready = new HashMap<>();
        int regeneration;
        long lastRegen = Long.MIN_VALUE;
    }
    private JimtaSpellbookCurios() {}

    public static void register() {
        for (var entry : JimtaSpellbooks.data("curios").entrySet()) {
            String name = entry.getKey(); var data = entry.getValue().getAsJsonObject();
            var key = ResourceKey.create(Registries.ITEM, Jugcraft.id(JimtaSpellbooks.PREFIX + name));
            var properties = new Item.Properties().setId(key).stacksTo(1).rarity(Rarity.UNCOMMON);
            if (Set.of("teleportation_amulet", "betrayer_signet", "wicked_bone_ring").contains(name)) properties.fireResistant();
            var item = new Curio(properties, name, data.get("slot").getAsString());
            Registry.register(BuiltInRegistries.ITEM, key, item); ITEMS.put(name, item); JimtaSpellbooks.ITEMS.put(name, item);
        }
        JimtaSpellbooks.data("affinities").entrySet().forEach(entry -> AFFINITIES.put(entry.getKey(), entry.getValue().getAsString()));
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (var player : server.getPlayerList().getPlayers()) if ((player.tickCount & 3) == 0) tick(player);
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> STATES.clear());
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(JimtaSpellbookCurios::allowDamage);
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, base, damage, blocked) -> {
            if (damage > 0 && !blocked && entity instanceof ServerPlayer player && source.getEntity() != null
                    && has(player, "expulsion_ring") && ready(player, "expulsion", 40)) repel(player);
        });
    }

    private static void worn(LivingEntity entity, Consumer<ItemStack> visitor) {
        if (!JimtaSpellbooks.enabled()) return;
        TrinketsApi.getAttachment(entity).forEach((slot, stack) -> {
            if (!slot.cosmetic() && slot.canApplyEffects() && stack.getItem() instanceof Curio) visitor.accept(stack);
        });
    }
    public static int count(LivingEntity entity, String name) {
        int[] amount = {0}; worn(entity, stack -> { if (((Curio) stack.getItem()).name.equals(name)) amount[0]++; });
        return Math.min(4, amount[0]);
    }
    public static boolean has(LivingEntity entity, String name) { return count(entity, name) > 0; }
    public static int focusCapacityBonus(Player player) { return 20 * count(player, "mana_ring") + 5 * count(player, "silver_ring"); }
    public static double focusRegenMultiplier(Player player) { return 1 + .15 * count(player, "amethyst_resonance_charm") + (JimtaSpecialArmor.wearing(player,"tarnished_helmet")?.25:0); }
    private static double reduction(int rings) {
        double attribute = 1 + .15 * rings;
        return attribute <= 1.5 ? 2 - attribute : .25 / (attribute - 1);
    }
    public static int cooldownTicks(Player player, int original) {
        return original <= 0 ? 0 : Math.max(1, (int) (original * reduction(count(player, "cooldown_ring"))));
    }
    public static double castDurationMultiplier(Player player) { return reduction(count(player, "cast_time_ring")); }
    public static double summonDamageMultiplier(LivingEntity owner) {
        return 1 + .15 * (count(owner, "conjurers_talisman") + count(owner, "greater_conjurers_talisman"));
    }
    public static double schoolPowerBonus(LivingEntity owner, String school) {
        return school.equals("eldritch") ? .1 * count(owner, "betrayer_signet") : 0;
    }
    public static int spellLevelBonus(Player player, String spell) {
        String selected = plainSpell(spell); int[] levels = {0};
        worn(player, stack -> { if (((Curio) stack.getItem()).name.equals("affinity_ring") && affinity(stack).equals(selected)) levels[0]++; });
        return Math.min(4, levels[0]);
    }
    private static String plainSpell(String spell) {
        int colon = spell.indexOf(':'); String name = colon >= 0 ? spell.substring(colon + 1) : spell;
        return name.startsWith("jimta_spellbooks_") ? name.substring("jimta_spellbooks_".length()) : name;
    }
    public static String affinity(ItemStack stack) { return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getStringOr(AFFINITY, ""); }
    public static ItemStack randomizeRing(ItemStack stack, RandomSource random) { return randomizeRing(stack, random, ""); }
    public static ItemStack randomizeRing(ItemStack stack, RandomSource random, String school) {
        if (!(stack.getItem() instanceof Curio item) || !item.name.equals("affinity_ring")) return stack;
        String filter = school == null ? "" : plainSpell(school);
        var choices = AFFINITIES.entrySet().stream().filter(e -> filter.isEmpty() || e.getValue().equals(filter))
                .filter(e -> JimtaSpellcasting.SPELLS.containsKey(e.getKey())).map(Map.Entry::getKey).toList();
        if (choices.isEmpty()) return ItemStack.EMPTY;
        bindAffinity(stack, choices.get(random.nextInt(choices.size()))); return stack;
    }
    public static boolean bindAffinity(ItemStack stack, String spell) {
        String name = plainSpell(spell), school = AFFINITIES.get(name);
        if (school == null || !(stack.getItem() instanceof Curio item) || !item.name.equals("affinity_ring")) return false;
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putString(AFFINITY, name));
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of((float) Math.max(0, SCHOOLS.indexOf(school))), List.of(), List.of(), List.of()));
        return true;
    }

    /** One existing native Evasion hit after a successful spell teleport; no second movement system or power gate. */
    public static void afterTeleport(ServerPlayer player) {
        var effect = JimtaWardSpells.EFFECTS.get("evasion");
        if (effect != null && has(player, "teleportation_amulet")) player.addEffect(new MobEffectInstance(effect, 60, 0, false, false, true));
    }
    public static void tick(ServerPlayer player) {
        if (!JimtaSpellbooks.enabled()) { STATES.remove(player); return; }
        if (has(player, "fireward_ring")) player.clearFire();
        if (has(player, "frostward_ring")) player.setTicksFrozen(0);
        if (has(player, "poisonward_ring")) player.removeEffect(MobEffects.POISON);
        if (has(player, "visibility_ring")) { player.removeEffect(MobEffects.BLINDNESS); player.removeEffect(MobEffects.DARKNESS); }
        var state = STATES.computeIfAbsent(player, p -> new State()); long now = player.level().getGameTime();
        int charms = count(player, "amethyst_resonance_charm"); var focus = ConcordanceProgress.focus(player);
        if (charms == 0 || focus.current(now) >= focus.capacity() || !player.isAlive()) {
            state.regeneration = 0; state.lastRegen = now; return;
        }
        if (state.lastRegen == Long.MIN_VALUE) state.lastRegen = now;
        if (now - state.lastRegen >= FocusPool.REGEN_TICKS) {
            // Fractional credit only while worn and below capacity, never a separately spendable mana pool.
            state.lastRegen = now; state.regeneration += 15 * charms;
            if (state.regeneration >= 100) {
                player.setAttached(JugcraftConcordance.FOCUS, focus.gain(now, state.regeneration / 100)); state.regeneration %= 100;
            }
        }
    }
    public static boolean preventsEffect(LivingEntity wearer, MobEffectInstance effect) {
        return effect.is(MobEffects.POISON) && has(wearer, "poisonward_ring")
                || (effect.is(MobEffects.BLINDNESS) || effect.is(MobEffects.DARKNESS)) && has(wearer, "visibility_ring");
    }
    public static boolean allowDamage(LivingEntity entity, DamageSource source, float amount) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return true;
        return !(source.is(DamageTypeTags.IS_FIRE) && has(entity, "fireward_ring")
                || source.is(DamageTypes.FREEZE) && has(entity, "frostward_ring"));
    }
    public static int experience(LivingEntity slain, net.minecraft.world.entity.Entity killer, int amount) {
        if (amount <= 0 || !(killer instanceof Player player)) return amount;
        return (int) Math.min(Integer.MAX_VALUE, Math.floor(amount * Math.pow(1.25, count(player, "emerald_stoneplate_ring"))));
    }
    public static float damage(LivingEntity victim, DamageSource source, float amount) {
        if (amount <= 0 || victim.level().isClientSide() || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return amount;
        if (source.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE,net.minecraft.resources.Identifier.fromNamespaceAndPath("spell_power","all"))) || source.is(DamageTypes.MAGIC) || source.is(DamageTypes.INDIRECT_MAGIC))
            amount *= (float) Math.max(.25, 1 - .15 * count(victim, "heavy_chain_necklace"));
        if (!(source.getEntity() instanceof ServerPlayer attacker) || !mayHarm(attacker, victim)) return amount;
        if (attacker.isInvisible() && has(attacker, "lurker_ring") && ready(attacker, "lurker", 300)) amount *= 1.5F;
        if (victim instanceof Player mage && has(attacker, "betrayer_signet")) {
            int extra = Math.max(0, ConcordanceProgress.focus(mage).capacity() - FocusPool.MAX);
            if (extra > 0 && ready(attacker, "betrayer", 100)) amount *= 1 + betrayerBonus(extra);
        }
        return amount;
    }
    public static float betrayerBonus(int extraFocus) {
        float percent = 0;
        for (int step = 0; step < 10 && extraFocus > 0; step++, extraFocus -= FocusPool.MAX)
            percent += Math.min(FocusPool.MAX, extraFocus) / (float) FocusPool.MAX * (.1F - .01F * step);
        return percent;
    }
    private static boolean ready(ServerPlayer player, String id, int ticks) {
        var state = STATES.computeIfAbsent(player, p -> new State()); long now = player.level().getGameTime();
        if (state.ready.getOrDefault(id, 0L) > now) return false; state.ready.put(id, now + ticks); return true;
    }
    private static boolean mayHarm(ServerPlayer player, LivingEntity target) {
        return ConcordanceEffects.mayHarm(player, target) && TwoHanded.allowed(player, player.level(), target);
    }
    private static void repel(ServerPlayer player) {
        ServerLevel level = player.level();
        for (var target : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(4),
                e -> e != player && e.isAlive() && e.distanceToSqr(player) < 16).stream().limit(16).toList()) {
            if (!mayHarm(player, target) || !player.hasLineOfSight(target)) continue;
            double distance = Math.max(.2, target.distanceToSqr(player));
            double strength = (1 - distance / 16 + .6) * (1 - target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
            var motion = target.getBoundingBox().getCenter().subtract(player.getBoundingBox().getCenter()).normalize().add(0, .1, 0).scale(Math.max(0, strength));
            target.setDeltaMovement(target.getDeltaMovement().add(motion)); target.syncVelocity = true;
        }
        level.sendParticles(ParticleTypes.GUST, player.getX(), player.getY() + .8, player.getZ(), 5, .4, .4, .4, 0);
    }

    public static final class Curio extends Item implements TrinketCallback {
        public final String name, slot;
        Curio(Properties properties, String name, String slot) { super(properties); this.name = name; this.slot = slot; }
        @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
            if (!name.equals("affinity_ring") || !JimtaSpellbooks.enabled() || player.isSpectator()) return InteractionResult.PASS;
            var scroll = player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
            if (!(scroll.getItem() instanceof JimtaSpellbooks.Scroll item) || !AFFINITIES.containsKey(item.spell)) return InteractionResult.PASS;
            if (!level.isClientSide()) { bindAffinity(player.getItemInHand(hand), item.spell); scroll.consume(1, player); }
            return InteractionResult.SUCCESS;
        }
        @Override public boolean canEquip(ItemStack stack, TrinketSlotAccess slot, LivingEntity wearer) { return wearer instanceof Player; }
        @Override public TrinketDropRule getDropRule(ItemStack stack, TrinketSlotAccess slot, LivingEntity wearer) {
            return slot.cosmetic() ? TrinketDropRule.KEEP : TrinketCallback.super.getDropRule(stack, slot, wearer);
        }
        @Override public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> lines, TooltipFlag flag) {
            lines.accept(Component.translatable("tooltip.jugcraft.jimta_spellbooks_" + name).withStyle(ChatFormatting.GRAY));
            if (name.equals("affinity_ring") && !affinity(stack).isEmpty()) lines.accept(Component.literal("Affinity: " + affinity(stack).replace('_', ' ') + " +1 level").withStyle(ChatFormatting.AQUA));
            lines.accept(Component.literal("Accessory: " + slot.replace('/', ' ')).withStyle(ChatFormatting.DARK_AQUA));
        }
    }
}
