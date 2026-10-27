package io.github.jimbozoomer.jugcraft.jimtafacts;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import eu.pb4.trinkets.api.TrinketDropRule;
import eu.pb4.trinkets.api.TrinketSlotAccess;
import eu.pb4.trinkets.api.TrinketsApi;
import eu.pb4.trinkets.api.callback.TrinketCallback;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceEffects;
import io.github.jimbozoomer.jugcraft.mixin.AlchemicalItemAccess;
import io.github.jimbozoomer.jugcraft.weapons.TwoHanded;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ints.ConstantValue;

/** Native, server-authoritative accessories. Original compiled classes are references, never a runtime dependency. */
public final class Jimtafacts {
    public static final Map<String, Item> ITEMS = new LinkedHashMap<>();
    private static final Set<String> CHESTS = Set.of("minecraft:chests/simple_dungeon", "minecraft:chests/abandoned_mineshaft",
            "minecraft:chests/desert_pyramid", "minecraft:chests/jungle_temple", "minecraft:chests/stronghold_corridor",
            "minecraft:chests/woodland_mansion", "minecraft:chests/ancient_city", "minecraft:chests/end_city_treasure");
    private static final Map<ServerPlayer, State> STATES = new WeakHashMap<>();
    private static final Identifier RUN_SPEED = Jugcraft.id("jimtafacts/running_speed");
    private static final Identifier RUN_STEP = Jugcraft.id("jimtafacts/running_step");
    private static boolean retaliating;

    private static final class State {
        final Map<String, Long> ready = new HashMap<>();
        net.minecraft.world.phys.Vec3 lastPosition;
        long rootedSince = -1;
    }

    private Jimtafacts() {}

    public static void register() {
        JimtaMimic.register();
        JimtaAbilities.register();
        JsonObject catalog;
        try (var in = Jimtafacts.class.getResourceAsStream("/jugcraft/jimtafacts/accessories.json")) {
            if (in == null) throw new IllegalStateException("Missing Jimtafacts accessory catalog");
            catalog = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (java.io.IOException e) { throw new IllegalStateException("Reading Jimtafacts catalog", e); }
        catalog.entrySet().forEach(entry -> {
            String name = entry.getKey(); var data = entry.getValue().getAsJsonObject();
            var key = ResourceKey.create(Registries.ITEM, Jugcraft.id("jimta_" + name));
            var properties = new Item.Properties().setId(key).stacksTo(1).rarity(Rarity.RARE);
            if (name.equals("onion_ring")) properties.food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(2).build());
            Item item = name.equals("mimic_spawn_egg") ? new SpawnEggItem(properties.spawnEgg(JimtaMimic.TYPE))
                    : name.equals("umbrella") ? new JimtaUmbrella(properties) : data.has("nutrition") ? new EndlessFood(properties, data.get("nutrition").getAsInt(), data.get("saturation").getAsFloat())
                    : new Accessory(properties, name, data.get("slot").getAsString(), data.getAsJsonObject("attributes"));
            ITEMS.put(name, Registry.register(BuiltInRegistries.ITEM, key, item));
        });
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(out -> ITEMS.values().forEach(out::accept));
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (var player : server.getPlayerList().getPlayers()) if ((player.tickCount & 3) == 0) tick(player);
        });
        ServerLivingEntityEvents.AFTER_DAMAGE.register(Jimtafacts::afterDamage);
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> !(entity instanceof ServerPlayer p)
                || !source.is(DamageTypeTags.IS_LIGHTNING) || !worn(p).contains("shock_pendant"));
        LootTableEvents.MODIFY.register((key, table, source, registries) -> {
            if (!source.isBuiltin() || !CHESTS.contains(key.identifier().toString())) return;
            var pool = LootPool.lootPool().setRolls(Holder.direct(new ConstantValue(1)))
                    .when(LootItemRandomChanceCondition.randomChance(.2f));
            ITEMS.forEach((name, item) -> { if (!name.equals("eternal_steak") && !name.equals("mimic_spawn_egg")) pool.add(LootItem.lootTableItem(item)); });
            table.withPool(pool);
        });
    }

    /** Cosmetic slots and inventory copies never supply effects; duplicate equipped copies do not stack. */
    public static Set<String> worn(LivingEntity wearer) {
        Set<String> result = new HashSet<>();
        TrinketsApi.getAttachment(wearer).forEach((slot, stack) -> {
            if (!slot.cosmetic() && slot.canApplyEffects() && stack.getItem() instanceof Accessory accessory && !JimtaAbilities.disabled(stack)) result.add(accessory.name);
        });
        return result;
    }

    static boolean ready(ServerPlayer player, String effect, int ticks) {
        var state = STATES.computeIfAbsent(player, p -> new State()); long now = player.level().getGameTime();
        if (state.ready.getOrDefault(effect, Long.MIN_VALUE) > now) return false;
        state.ready.put(effect, now + ticks); return true;
    }

    public static void tick(ServerPlayer player) {
        if (!player.isAlive()) { JimtaAbilities.release(player); return; }
        var equipped = worn(player);
        JimtaAbilities.tick(player, equipped);
        sprintModifier(player, Attributes.MOVEMENT_SPEED, RUN_SPEED, .25, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
                equipped.contains("running_shoes") && player.isSprinting());
        sprintModifier(player, Attributes.STEP_HEIGHT, RUN_STEP, .4, AttributeModifier.Operation.ADD_VALUE,
                equipped.contains("running_shoes") && player.isSprinting());
        if (equipped.contains("snorkel")) player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 12, 0, true, false, false));
        // Long refresh prevents end-of-night-vision flashing. Naturally fades; never clears a player's potion.
        if (equipped.contains("night_vision_goggles")) player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 240, 0, true, false, false));
        if (equipped.contains("scarf_of_invisibility")) player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 12, 0, true, false, false));
        if (equipped.contains("antidote_vessel") && (player.hasEffect(MobEffects.POISON) || player.hasEffect(MobEffects.WITHER)) && ready(player, "antidote", 200)) {
            player.removeEffect(MobEffects.POISON); player.removeEffect(MobEffects.WITHER);
        }
        if (equipped.contains("charm_of_sinking") && player.isUnderWater() && !player.isSwimming() && !player.getAbilities().flying) {
            var motion = player.getDeltaMovement(); player.setDeltaMovement(motion.x, Math.max(-.15, motion.y - .03), motion.z);
            player.syncVelocity = true;
        }
        if (equipped.contains("universal_attractor") && ready(player, "magnet", 8)) attract(player);
        var state = STATES.computeIfAbsent(player, p -> new State());
        boolean stationary = state.lastPosition != null && state.lastPosition.distanceToSqr(player.position()) < .0001;
        state.lastPosition = player.position();
        if (!equipped.contains("rooted_boots") || !player.onGround() || !stationary || !player.level().hasChunkAt(player.blockPosition()) || !player.level().getBlockState(player.blockPosition().below()).is(Blocks.GRASS_BLOCK)) state.rootedSince = -1;
        else if (state.rootedSince < 0) state.rootedSince = player.level().getGameTime();
        else if (player.level().getGameTime() - state.rootedSince >= 200) {
            if (player.getFoodData().needsFood()) { player.getFoodData().eat(1, 0); JimtaAbilities.growNearby(player); }
            state.rootedSince = player.level().getGameTime();
        }
    }

    private static void sprintModifier(ServerPlayer p, Holder<Attribute> attribute, Identifier id, double value,
            AttributeModifier.Operation operation, boolean active) {
        var instance = p.getAttribute(attribute); if (instance == null) return;
        if (active && !instance.hasModifier(id)) instance.addTransientModifier(new AttributeModifier(id, value, operation));
        else if (!active) instance.removeModifier(id);
    }

    /** Bounded, claim-aware pickup attraction. Never reaches through walls, unloaded chunks or another owner's drops. */
    public static void attract(ServerPlayer p) {
        var candidates = new ArrayList<ItemEntity>();
        p.level().getEntities(net.minecraft.world.level.entity.EntityTypeTest.forClass(ItemEntity.class), p.getBoundingBox().inflate(5),
                item -> item.isAlive() && !item.hasPickUpDelay(), candidates, 24);
        for (var item : candidates) {
            if (!p.hasLineOfSight(item) || !Authority.mayChange(p.level(), p, item.blockPosition()) || !Authority.mayChange(p.level(), p, p.blockPosition())) continue;
            var reserved = ((AlchemicalItemAccess) item).jugcraft$pickupTarget();
            if (reserved != null && !reserved.equals(p.getUUID())) continue;
            var direction = p.position().add(0, .5, 0).subtract(item.position());
            if (direction.lengthSqr() < .01) continue;
            item.setDeltaMovement(direction.normalize().scale(.25)); item.syncVelocity = true;
        }
    }

    public static void afterDamage(LivingEntity victim, DamageSource source, float base, float actual, boolean blocked) {
        if (blocked || actual <= 0 || retaliating) return;
        if (victim instanceof ServerPlayer p) {
            var equipped = worn(p);
            if (equipped.contains("cross_necklace")) p.setInvulnerableTime(Math.max(p.getInvulnerableTime(), 30));
            if (equipped.contains("panic_necklace") && ready(p, "panic", 200)) p.addEffect(new MobEffectInstance(MobEffects.SPEED, 100, 1));
            if (equipped.contains("obsidian_skull") && source.is(DamageTypeTags.IS_FIRE) && ready(p, "obsidian", 600)) p.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 200));
            if ((source.is(DamageTypes.PLAYER_ATTACK) || source.is(DamageTypes.MOB_ATTACK) || source.is(DamageTypes.MOB_ATTACK_NO_AGGRO))
                    && source.getEntity() instanceof LivingEntity attacker && source.getDirectEntity() == attacker
                    && attacker != p && attacker.isAlive() && p.distanceToSqr(attacker) <= 36 && mayHarm(p, attacker)) {
                if (equipped.contains("flame_pendant") && ready(p, "flame", 100)) attacker.igniteForSeconds(4);
                float reflected = 0;
                if (equipped.contains("thorn_pendant") && ready(p, "thorn", 60)) reflected += 2;
                if (equipped.contains("shock_pendant") && ready(p, "shock", 100)) reflected += 3;
                if (reflected > 0) {
                    retaliating = true;
                    try { attacker.hurtServer(p.level(), p.damageSources().playerAttack(p), reflected); }
                    finally { retaliating = false; }
                }
            }
        }
        if (source.is(DamageTypes.PLAYER_ATTACK) && source.getEntity() instanceof ServerPlayer p && source.getDirectEntity() == p && victim != p
                && p.distanceToSqr(victim) <= 36 && mayHarm(p, victim)) {
            var equipped = worn(p);
            if (equipped.contains("fire_gauntlet")) victim.igniteForSeconds(4);
            if (equipped.contains("vampiric_glove") && ready(p, "vampire", 20)) p.heal(Math.min(2, actual * .2f));
            if (equipped.contains("withered_bracelet") && ready(p, "wither", 100)) victim.addEffect(new MobEffectInstance(MobEffects.WITHER, 60), p);
        }
    }

    private static boolean mayHarm(ServerPlayer player, LivingEntity target) {
        return ConcordanceEffects.mayHarm(player, target) && TwoHanded.allowed(player, player.level(), target);
    }

    public static final class Accessory extends Item implements TrinketCallback {
        public final String name;
        public final String slot;
        private final Map<String, Double> attributes = new LinkedHashMap<>();
        Accessory(Properties properties, String name, String slot, JsonObject values) {
            super(properties); this.name = name; this.slot = slot;
            values.entrySet().forEach(entry -> attributes.put(entry.getKey(), entry.getValue().getAsDouble()));
        }
        @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
            if (!player.isShiftKeyDown()) return super.use(level, player, hand);
            if (!level.isClientSide()) {
                ItemStack stack = player.getItemInHand(hand); boolean disabled = !JimtaAbilities.disabled(stack);
                net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA, stack, tag -> tag.putBoolean("jimtafacts_disabled", disabled));
                player.sendOverlayMessage(Component.literal(disabled ? "Accessory disabled" : "Accessory enabled"));
            }
            return InteractionResult.SUCCESS;
        }
        @Override public boolean canEquip(ItemStack stack, TrinketSlotAccess slot, LivingEntity wearer) { return wearer instanceof Player; }
        @Override public TrinketDropRule getDropRule(ItemStack stack, TrinketSlotAccess slot, LivingEntity wearer) {
            return slot.cosmetic() ? TrinketDropRule.KEEP : TrinketCallback.super.getDropRule(stack, slot, wearer);
        }
        @Override public void forEachTrinketModifier(ItemStack stack, TrinketSlotAccess slot, LivingEntity wearer, Identifier slotId,
                BiConsumer<Holder<Attribute>, AttributeModifier> out) {
            if (JimtaAbilities.disabled(stack)) return;
            attributes.forEach((id, amount) -> BuiltInRegistries.ATTRIBUTE.get(Identifier.withDefaultNamespace(id)).ifPresent(attribute ->
                    out.accept(attribute, new AttributeModifier(Jugcraft.id("jimtafacts/" + name + "/" + id), amount, AttributeModifier.Operation.ADD_VALUE))));
        }
        @Override public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> lines, TooltipFlag flag) {
            lines.accept(Component.translatable("tooltip.jugcraft.jimta_" + name).withStyle(ChatFormatting.GRAY));
            lines.accept(Component.literal("Accessory: " + slot.replace('/', ' ')).withStyle(ChatFormatting.DARK_AQUA));
        }
    }

    public static final class EndlessFood extends Item {
        private final int nutrition; private final float saturation;
        EndlessFood(Properties properties, int nutrition, float saturation) { super(properties.food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(nutrition).saturationModifier(saturation).build())); this.nutrition = nutrition; this.saturation = saturation; }
        @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
            if (!player.getFoodData().needsFood() || player.getCooldowns().isOnCooldown(player.getItemInHand(hand))) return InteractionResult.FAIL;
            player.startUsingItem(hand); return InteractionResult.CONSUME;
        }
        @Override public ItemUseAnimation getUseAnimation(ItemStack stack) { return ItemUseAnimation.EAT; }
        @Override public int getUseDuration(ItemStack stack, LivingEntity entity) { return 32; }
        @Override public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity eater) {
            if (eater instanceof ServerPlayer player && player.getFoodData().needsFood() && !player.getCooldowns().isOnCooldown(stack)) {
                player.getFoodData().eat(nutrition, saturation); player.getCooldowns().addCooldown(stack, 1200);
            }
            return stack;
        }
        @Override public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> lines, TooltipFlag flag) {
            lines.accept(Component.literal("Reusable food. One-minute cooldown after eating.").withStyle(ChatFormatting.GRAY));
        }
    }
}
