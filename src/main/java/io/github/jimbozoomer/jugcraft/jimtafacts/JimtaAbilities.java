package io.github.jimbozoomer.jugcraft.jimtafacts;

import eu.pb4.trinkets.api.TrinketsApi;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import java.util.*;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.*;
import net.minecraft.tags.*;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;

/** Shared server authority for the recovered accessory abilities. Source defaults disable durability. */
public final class JimtaAbilities {
    private static final Map<ServerPlayer, Motion> MOTION = new WeakHashMap<>();
    private static final Identifier MOUNT_SPEED = Jugcraft.id("jimtafacts/mount_speed"), SNOW_SPEED = Jugcraft.id("jimtafacts/snow_speed");
    private static final class Motion {
        boolean jumped, airInput, sneaking;
        int flight = 160, inputUntil;
        long recharge, nextJump;
        LivingEntity mount;
    }
    private JimtaAbilities() {}
    public static void register() {
        PayloadTypeRegistry.serverboundPlay().register(JimtaInput.TYPE, JimtaInput.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(JimtaInput.TYPE, (payload, context) -> context.server().execute(() -> input(context.player(), payload.action())));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> release(handler.getPlayer()));
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> !(entity instanceof ServerPlayer p) || !rescue(p, source));
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> !(entity instanceof ServerPlayer p) || !immune(p, source));
    }
    public static void release(ServerPlayer player) {Motion state=MOTION.remove(player);if(state!=null&&state.mount!=null)modifier(state.mount,MOUNT_SPEED,.25,false);}
    public static boolean disabled(ItemStack stack) {return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBooleanOr("jimtafacts_disabled", false);}
    public static boolean wearing(Entity entity, String name) {return entity instanceof LivingEntity living && Jimtafacts.worn(living).contains(name);}
    public static boolean immune(ServerPlayer p, DamageSource source) {
        var worn = Jimtafacts.worn(p);
        return worn.contains("strider_shoes") && source.is(DamageTypeTags.BURN_FROM_STEPPING)
                || worn.contains("snowshoes") && source.is(DamageTypes.FREEZE)
                || worn.contains("warp_drive") && source.is(DamageTypes.ENDER_PEARL);
    }
    public static boolean fluidWalking(LivingEntity p, FluidState fluid) {
        if (p.isPassenger() || p.isFallFlying() || fluid.isEmpty()) return false;
        var worn = Jimtafacts.worn(p);
        return fluid.is(FluidTags.WATER) && p.isSprinting() && !p.isShiftKeyDown() && worn.contains("aqua_dashers")
                || fluid.is(FluidTags.LAVA) && p.isShiftKeyDown() && worn.contains("strider_shoes");
    }
    public static void input(ServerPlayer p, int action) {
        if (!p.isAlive() || p.isSpectator() || p.isPassenger() || p.getAbilities().flying) return;
        var state = MOTION.computeIfAbsent(p, x -> new Motion());
        if (action == 1 || action == 2) {state.airInput = action == 1; state.inputUntil = p.tickCount + 10; return;}
        if (action != 0 || p.onGround() || p.isInWater() || p.isInLava() || p.isFallFlying() || state.jumped
                || p.level().getGameTime() < state.nextJump || !wearing(p,"cloud_in_a_bottle")) return;
        state.jumped = true; state.nextJump = p.level().getGameTime() + 6;
        var motion = p.getDeltaMovement(); var forward = p.getLookAngle().multiply(1,0,1).normalize().scale(p.isSprinting() ? .22 : 0);
        p.setDeltaMovement(motion.x + forward.x, .5, motion.z + forward.z); p.resetFallDistance(); p.syncVelocity = true;
        p.level().sendParticles(ParticleTypes.CLOUD,p.getX(),p.getY(),p.getZ(),8,.15,.05,.15,.02);
    }
    public static void tick(ServerPlayer p, Set<String> worn) {
        Motion state = MOTION.computeIfAbsent(p, x -> new Motion());
        if (p.onGround() || p.isInWater() || p.isInLava()) {
            state.jumped = false;
            if (p.level().getGameTime() >= state.recharge) state.flight = Math.min(160,state.flight + 3);
        }
        if (worn.contains("helium_flamingo") && state.airInput && p.tickCount <= state.inputUntil && state.flight > 0
                && !p.onGround() && !p.isInWater() && !p.isInLava() && !p.isPassenger() && !p.isFallFlying() && !p.getAbilities().flying) {
            // Source air swimming is finite, so it never grants or revokes creative/other providers' flight abilities.
            state.flight = Math.max(0,state.flight - 4); state.recharge = p.level().getGameTime() + 60;
            p.setDeltaMovement(p.getLookAngle().scale(.32)); p.resetFallDistance(); p.syncVelocity = true;
            if (state.flight == 0) p.sendOverlayMessage(net.minecraft.network.chat.Component.literal("Helium Flamingo needs to recharge on the ground"));
        }
        if ((p.getMainHandItem().getItem() instanceof JimtaUmbrella || p.getOffhandItem().getItem() instanceof JimtaUmbrella)
                && !p.onGround() && !p.isInWater() && !p.isPassenger() && !p.isShiftKeyDown() && p.getDeltaMovement().y < -.12) {
            var velocity=p.getDeltaMovement();p.setDeltaMovement(velocity.x,-.12,velocity.z);p.resetFallDistance();p.syncVelocity=true;
        }
        LivingEntity mount = worn.contains("cowboy_hat") && p.getVehicle() instanceof LivingEntity living ? living : null;
        if (mount != state.mount && state.mount != null) modifier(state.mount, MOUNT_SPEED, .25, false);
        state.mount = mount;if(mount!=null)modifier(mount,MOUNT_SPEED,.25,true);
        // Only snowshoes read the ground, and never through an unloaded chunk: getBlockState would load it synchronously.
        // Game-test mock players stand unticked at the origin (tickCount stays 0), so this ran every tick for each of them.
        BlockPos feet = p.blockPosition();
        boolean snow=worn.contains("snowshoes")&&p.level().hasChunkAt(feet)
                &&(p.level().getBlockState(feet.below()).is(BlockTags.SNOW)||p.level().getBlockState(feet).is(Blocks.POWDER_SNOW));
        modifier(p,SNOW_SPEED,.25,snow);
        if (worn.contains("kitty_slippers") && Jimtafacts.ready(p,"repel",20)) {
            var nearby = new ArrayList<Mob>();
            p.level().getEntities(net.minecraft.world.level.entity.EntityTypeTest.forClass(Mob.class),p.getBoundingBox().inflate(12),
                    m -> (m instanceof Creeper || m instanceof Phantom) && m.getTarget()==p && m.getLastHurtByMob()!=p,nearby,16);
            for (Mob mob:nearby) {mob.setTarget(null);Vec3 away=mob.position().subtract(p.position()).normalize().scale(8).add(mob.position());mob.getNavigation().moveTo(away.x,away.y,away.z,1.2);}
        }
        if (worn.contains("whoopee_cushion") && p.isShiftKeyDown() && !state.sneaking && Jimtafacts.ready(p,"whoopee",40))
            p.level().playSound(null,p.blockPosition(),JimtaMimic.FART,SoundSource.PLAYERS,.7F,.9F+p.getRandom().nextFloat()*.2F);
        state.sneaking = p.isShiftKeyDown();
    }
    private static void modifier(LivingEntity p, Identifier id, double amount, boolean active) {
        var attribute=p.getAttribute(Attributes.MOVEMENT_SPEED);if(attribute==null)return;
        if(active&&!attribute.hasModifier(id))attribute.addTransientModifier(new AttributeModifier(id,amount,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        else if(!active)attribute.removeModifier(id);
    }
    /** A real consume, once, before grave/death handling. Never rescues /kill or void damage. */
    public static boolean rescue(ServerPlayer p, DamageSource source) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || !wearing(p,"chorus_totem")) return false;
        final ItemStack[] found={ItemStack.EMPTY};
        TrinketsApi.getAttachment(p).forEach((slot,stack)->{if(found[0].isEmpty()&&!slot.cosmetic()&&slot.canApplyEffects()
                &&stack.is(Jimtafacts.ITEMS.get("chorus_totem"))&&!disabled(stack))found[0]=stack;});
        if(found[0].isEmpty())return false;
        found[0].shrink(1);p.setHealth(Math.min(9,p.getMaxHealth()));p.removeAllEffects();p.clearFire();p.resetFallDistance();
        for(int i=0;i<48;i++) {
            BlockPos at=p.blockPosition().offset(p.getRandom().nextInt(33)-16,p.getRandom().nextInt(17)-8,p.getRandom().nextInt(33)-16);
            if(!p.level().hasChunkAt(at)||!p.level().getWorldBorder().isWithinBounds(at)||!Authority.mayChange(p.level(),p,at)
                    ||!p.level().getBlockState(at.below()).isSolidRender()||!p.level().getFluidState(at).isEmpty())continue;
            Vec3 point=Vec3.atBottomCenterOf(at).add(.5,0,.5);
            if(p.level().noCollision(p,p.getBoundingBox().move(point.subtract(p.position())))){p.teleportTo(point.x,point.y,point.z);break;}
        }
        p.addEffect(new MobEffectInstance(MobEffects.REGENERATION,100,1));p.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE,100));
        p.level().broadcastEntityEvent(p,(byte)35);return true;
    }
    public static void growNearby(ServerPlayer p) {
        for(int i=0;i<8;i++) {
            BlockPos at=p.blockPosition().offset(p.getRandom().nextInt(5)-2,0,p.getRandom().nextInt(5)-2);
            if(!p.level().hasChunkAt(at)||!Authority.mayChange(p.level(),p,at))continue;
            var block=p.level().getBlockState(at);
            if(block.getBlock() instanceof BonemealableBlock plant && plant.isValidBonemealTarget(p.level(),at,block,BonemealSource.INTERACTION)) {
                plant.performBonemeal(p.level(),p.getRandom(),at,block,BonemealSource.INTERACTION);p.level().levelEvent(1505,at,0);return;
            }
        }
    }
}
