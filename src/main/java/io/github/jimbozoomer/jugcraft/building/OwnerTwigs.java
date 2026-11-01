package io.github.jimbozoomer.jugcraft.building;

import com.google.gson.*;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.registry.OxidizableBlocksRegistry;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.core.particles.*;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.*;

/** Native behaviors and original audio for the supplied natural building collection. */
public final class OwnerTwigs {
    private static final Map<String, SoundEvent> SOUNDS = new HashMap<>();
    public static EntityType<Pebble> PEBBLE;
    public static final TagKey<EntityType<?>> BAMBOO_IMMUNE = TagKey.create(Registries.ENTITY_TYPE, Jugcraft.id("twigworks_bamboo_leaves_slow_immune"));
    private OwnerTwigs() { }
    static void registerSounds() {
        try (var stream = OwnerTwigs.class.getResourceAsStream("/assets/jugcraft/sounds.json")) {
            var sounds = JsonParser.parseReader(new InputStreamReader(Objects.requireNonNull(stream), StandardCharsets.UTF_8)).getAsJsonObject();
            for (String key : sounds.keySet()) if (key.startsWith("twigworks_")) {
                var id = Jugcraft.id(key); SOUNDS.put(key.substring(10), Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id)));
            }
        } catch (IOException e) { throw new IllegalStateException("Supplied building sounds", e); }
    }
    public static SoundEvent sound(String key) { return Objects.requireNonNull(SOUNDS.get(key), "Unregistered Twigs sound " + key); }
    static void finishRegistration() {
        var pots = OwnerBuildingCollection.BLOCKS.values().stream().filter(b -> b instanceof OwnerSiltPot).toArray(Block[]::new);
        OwnerSiltPot.TYPE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("twigworks_silt_pot"), FabricBlockEntityTypeBuilder.create(OwnerSiltPot.Storage::new, pots).build());
        var key = ResourceKey.create(Registries.ENTITY_TYPE, Jugcraft.id("twigworks_pebble"));
        PEBBLE = Registry.register(BuiltInRegistries.ENTITY_TYPE, key, EntityType.Builder.<Pebble>of(Pebble::new, MobCategory.MISC).sized(.25F, .25F).clientTrackingRange(4).updateInterval(10).build(key));
        String[] stages = {"", "exposed_", "weathered_", "oxidized_"};
        for (int i = 0; i < stages.length; i++) {
            Block raw = block(stages[i] + "copper_pillar");
            OxidizableBlocksRegistry.registerWaxable(raw, block("waxed_" + stages[i] + "copper_pillar"));
            if (i < 3) OxidizableBlocksRegistry.registerNextStage(raw, block(stages[i + 1] + "copper_pillar"));
        }
        OwnerTwigsWorld.register();
    }
    static Block block(String name) { return Objects.requireNonNull(OwnerBuildingCollection.BLOCKS.get("twigworks_" + name)); }
    public static final class Copper extends RotatedPillarBlock implements WeatheringCopper {
        private final WeatherState age;
        Copper(Properties p, String name) { super(p); age = name.contains("oxidized") ? WeatherState.OXIDIZED : name.contains("weathered") ? WeatherState.WEATHERED : name.contains("exposed") ? WeatherState.EXPOSED : WeatherState.UNAFFECTED; }
        @Override public WeatherState getAge() { return age; }
        @Override protected boolean isRandomlyTicking(BlockState s) { return WeatheringCopper.getNext(s.getBlock()).isPresent(); }
        @Override protected void randomTick(BlockState s, ServerLevel l, BlockPos p, RandomSource r) { changeOverTime(s, l, p, r); }
    }
    public static final class ShellItem extends BlockItem {
        ShellItem(Block block, Item.Properties p) { super(block, p.stacksTo(1).rarity(Rarity.UNCOMMON)); }
        @Override public InteractionResult use(Level l, Player p, InteractionHand hand) {
            p.startUsingItem(hand);
            if (!l.isClientSide()) {
                l.playSound(null, p.blockPosition(), sound("item.bronzed_seashell.play"), SoundSource.RECORDS, 16, 1);
                l.gameEvent(p, net.minecraft.world.level.gameevent.GameEvent.INSTRUMENT_PLAY, p.position());
                p.getCooldowns().addCooldown(p.getItemInHand(hand), 60);
            }
            return InteractionResult.CONSUME;
        }
        @Override public int getUseDuration(ItemStack s, LivingEntity e) { return 1200; }
        @Override public ItemUseAnimation getUseAnimation(ItemStack s) { return ItemUseAnimation.TOOT_HORN; }
    }
    public static final class PebbleItem extends BlockItem {
        PebbleItem(Block b, Item.Properties p) { super(b, p); }
        @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
            if (level instanceof ServerLevel server) {
                ItemStack stack = player.getItemInHand(hand);
                var pebble = new Pebble(server, player, stack.copyWithCount(1));
                pebble.shootFromRotation(player, player.getXRot(), player.getYRot(), 0, 1.4F, 1);
                if (!server.addFreshEntity(pebble)) return InteractionResult.FAIL;
                server.playSound(null, player.blockPosition(), sound("entity.pebble.throw"), SoundSource.PLAYERS, .5F, 1);
                player.getCooldowns().addCooldown(stack, 10); stack.consume(1, player);
            }
            return InteractionResult.SUCCESS;
        }
    }
    public static final class Pebble extends ThrowableItemProjectile {
        public Pebble(EntityType<? extends Pebble> type, Level level) { super(type, level); }
        Pebble(ServerLevel level, Player player, ItemStack stack) { super(PEBBLE, player, level, stack); }
        @Override protected Item getDefaultItem() { return block("pebble").asItem(); }
        @Override protected void onHit(HitResult hit) {
            super.onHit(hit);
            if (level() instanceof ServerLevel server) {
                if (hit instanceof EntityHitResult target) {
                    if (target.getEntity() instanceof LivingEntity living && getOwner() instanceof ServerPlayer owner && Authority.mayStrike(owner, living))
                        living.hurtServer(server, damageSources().thrown(this, owner), 1);
                } else spawnAtLocation(server, getItem().copyWithCount(1));
                server.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, net.minecraft.world.item.ItemStackTemplate.fromNonEmptyStack(getItem())), getX(), getY(), getZ(), 8, .05, .05, .05, .02);
                discard();
            }
        }
        @Override protected void doWaterSplashEffect() { setDeltaMovement(getDeltaMovement().multiply(1, -.5, 1).scale(.932)); super.doWaterSplashEffect(); }
    }
}
