package io.github.jimbozoomer.jugcraft.jimtafacts.network;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceEffects;
import io.github.jimbozoomer.jugcraft.finance.LandRegistry;
import io.github.jimbozoomer.jugcraft.town.TownProtection;
import java.util.UUID;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.*;
import net.minecraft.core.dispenser.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;

/** Source half-block explosive, 80-tick fuse and water recovery; native ownership/protection added. */
public final class DigitalTinyTnt {
    public static Block BLOCK;
    public static EntityType<Charge> ENTITY;
    public static BlockEntityType<Owner> OWNER;
    private DigitalTinyTnt() {}
    public static void register() {
        var key = ResourceKey.create(Registries.BLOCK, Jugcraft.id("jimta_tiny_tnt"));
        BLOCK = Registry.register(BuiltInRegistries.BLOCK, key, new SmallTnt(BlockBehaviour.Properties.ofFullCopy(Blocks.TNT).noOcclusion().setId(key)));
        DigitalStorage.BLOCKS.put("tiny_tnt", BLOCK);
        var itemKey = ResourceKey.create(Registries.ITEM, key.identifier());
        Registry.register(BuiltInRegistries.ITEM, itemKey, new BlockItem(BLOCK, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));
        OWNER = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("jimta_tiny_tnt_owner"), FabricBlockEntityTypeBuilder.create(Owner::new, BLOCK).build());
        var entityKey = ResourceKey.create(Registries.ENTITY_TYPE, Jugcraft.id("jimta_tiny_tnt"));
        ENTITY = Registry.register(BuiltInRegistries.ENTITY_TYPE, entityKey, EntityType.Builder.<Charge>of(Charge::new, MobCategory.MISC).sized(.49f, .49f).clientTrackingRange(8).updateInterval(1).build(entityKey));
        DispenserBlock.registerBehavior(BLOCK.asItem(), new DefaultDispenseItemBehavior() {
            @Override protected ItemStack execute(BlockSource source, ItemStack stack) {
                BlockPos pos = source.pos().relative(source.state().getValue(DispenserBlock.FACING));
                if (!unclaimed(source.level(), source.pos()) || !unclaimed(source.level(), pos)) return stack;
                spawn(source.level(), pos, null, 80); stack.shrink(1); return stack;
            }
        });
    }
    private static boolean unclaimed(ServerLevel level, BlockPos pos) {
        return level.isLoaded(pos) && level.getWorldBorder().isWithinBounds(pos) && !TownProtection.shieldsBlock(level, pos)
                && (level.dimension() != Level.OVERWORLD || LandRegistry.get(level.getServer()).claim(LandRegistry.keyOf(pos)) == null)
                && Authority.mayChange(level, Authority.unownedTinyTntActor(level), pos);
    }
    public static Charge spawn(ServerLevel level, BlockPos pos, UUID owner, int fuse) {
        Charge e = new Charge(ENTITY, level); e.actor = owner; e.setPos(pos.getX() + .5, pos.getY(), pos.getZ() + .5);
        double a = level.getRandom().nextDouble() * Math.PI * 2; e.setDeltaMovement(-Math.sin(a) * .02, .2, -Math.cos(a) * .02);
        e.setFuse(fuse); e.setBlockState(BLOCK.defaultBlockState()); level.addFreshEntity(e);
        level.playSound(null, pos, SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1, 1); return e;
    }
    public static final class Owner extends BlockEntity {
        UUID actor;
        public Owner(BlockPos p, BlockState s) { super(OWNER, p, s); }
        @Override protected void saveAdditional(ValueOutput out) { super.saveAdditional(out); if (actor != null) out.store("owner", UUIDUtil.CODEC, actor); }
        @Override protected void loadAdditional(ValueInput in) { super.loadAdditional(in); actor = in.read("owner", UUIDUtil.CODEC).orElse(null); }
    }
    public static final class SmallTnt extends Block implements EntityBlock {
        SmallTnt(Properties p) { super(p); }
        @Override public BlockEntity newBlockEntity(BlockPos p, BlockState s) { return new Owner(p, s); }
        @Override protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return Block.box(4, 0, 4, 12, 8, 12); }
        @Override public void setPlacedBy(Level l, BlockPos p, BlockState s, LivingEntity actor, ItemStack stack) {
            if (l.getBlockEntity(p) instanceof Owner o && actor instanceof Player) { o.actor = actor.getUUID(); o.setChanged(); }
            if (l instanceof ServerLevel server && l.hasNeighborSignal(p)) prime(server, p, actor, 80);
        }
        private boolean prime(ServerLevel level, BlockPos pos, LivingEntity igniter, int fuse) {
            if (!level.getBlockState(pos).is(this)) return false;
            UUID owner = igniter instanceof Player ? igniter.getUUID() : level.getBlockEntity(pos) instanceof Owner o ? o.actor : null;
            var actor = Authority.answering(level, owner);
            if (owner == null ? !unclaimed(level, pos) : !Authority.mayChange(level, actor, pos)) return false;
            spawn(level, pos, owner, fuse); level.removeBlock(pos, false); return true;
        }
        @Override protected void neighborChanged(BlockState s, Level l, BlockPos p, Block b, Orientation o, boolean moved) { if (l instanceof ServerLevel server && l.hasNeighborSignal(p)) prime(server, p, null, 80); }
        @Override protected InteractionResult useItemOn(ItemStack stack, BlockState s, Level l, BlockPos p, Player player, InteractionHand hand, BlockHitResult hit) {
            if (!stack.is(Items.FLINT_AND_STEEL) && !stack.is(Items.FIRE_CHARGE)) return InteractionResult.TRY_WITH_EMPTY_HAND;
            if (l instanceof ServerLevel server && prime(server, p, player, 80) && !player.isCreative()) {
                if (stack.is(Items.FLINT_AND_STEEL)) stack.hurtAndBreak(1, player, hand.asEquipmentSlot()); else stack.shrink(1);
            }
            return InteractionResult.SUCCESS;
        }
        @Override protected void onProjectileHit(Level l, BlockState s, BlockHitResult hit, Projectile projectile) { if (l instanceof ServerLevel server && projectile.isOnFire()) prime(server, hit.getBlockPos(), projectile.getOwner() instanceof LivingEntity living ? living : null, 80); }
        @Override public boolean dropFromExplosion(Explosion explosion) { return false; }
        @Override public void wasExploded(ServerLevel level, BlockPos pos, Explosion explosion) {
            // Explosion has removed the block already; its initiating owner is retained where available.
            var igniter = explosion.getIndirectSourceEntity(); UUID owner = igniter instanceof Player ? igniter.getUUID() : null;
            if (owner == null ? unclaimed(level, pos) : Authority.mayChangeFor(level, owner, pos)) spawn(level, pos, owner, 10 + level.getRandom().nextInt(20));
        }
    }
    public static final class Charge extends PrimedTnt {
        UUID actor;
        public Charge(EntityType<? extends Charge> t, Level l) { super(t, l); }
        @Override public LivingEntity getOwner() { return level() instanceof ServerLevel server ? Authority.present(server, actor) : null; }
        @Override protected void addAdditionalSaveData(ValueOutput out) { super.addAdditionalSaveData(out); if (actor != null) out.store("jimta_owner", UUIDUtil.CODEC, actor); }
        @Override protected void readAdditionalSaveData(ValueInput in) { super.readAdditionalSaveData(in); actor = in.read("jimta_owner", UUIDUtil.CODEC).orElse(null); }
        @Override public void tick() {
            // Keep source motion/fuse without calling vanilla's full-size TNT explosion.
            xo = getX(); yo = getY(); zo = getZ(); updateFluidInteraction();
            setDeltaMovement(getDeltaMovement().add(0, -.04, 0)); move(MoverType.SELF, getDeltaMovement()); setDeltaMovement(getDeltaMovement().scale(.98));
            if (onGround()) setDeltaMovement(getDeltaMovement().multiply(.7, -.5, .7));
            if (isInWater()) { if (level() instanceof ServerLevel server) { var item = new ItemEntity(server, getX(), getY(), getZ(), new ItemStack(BLOCK)); item.setDeltaMovement(getDeltaMovement()); server.addFreshEntity(item); discard(); } return; }
            int remaining = getFuse() - 1; setFuse(remaining);
            if (remaining <= 0) { if (level() instanceof ServerLevel server) detonate(server); discard(); }
            else if (level().isClientSide()) level().addParticle(ParticleTypes.SMOKE, getX(), getY() + .25, getZ(), 0, 0, 0);
        }
        private void detonate(ServerLevel level) {
            level.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 4, 2);
            level.sendParticles(ParticleTypes.EXPLOSION, getX(), getY(), getZ(), 1, 0, 0, 0, 0);
            ServerPlayer owner = Authority.answering(level, actor);
            var source = level.damageSources().explosion(this, owner);
            for (var target : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(1.5)).stream().limit(64).toList()) {
                if (owner == null || !Authority.mayStrike(owner, target) || !ConcordanceEffects.mayHarm(owner, target)) continue;
                if (!Authority.mayChange(level, owner, target.blockPosition())) continue;
                target.hurtServer(level, source, 6);
            }
            var explosion = new ServerExplosion(level, this, source, new ExplosionDamageCalculator(), position(), .2f, false, Explosion.BlockInteraction.DESTROY_WITH_DECAY);
            Vec3 center = position().add(0, -.25, 0);
            for (var cursor : BlockPos.betweenClosed(BlockPos.containing(center.add(-2, -2, -2)), BlockPos.containing(center.add(2, 2, 2)))) {
                BlockPos pos = cursor.immutable(); if (!level.isLoaded(pos) || TownProtection.shieldsBlock(level, pos)) continue;
                if (actor == null ? !unclaimed(level, pos) : !Authority.mayChange(level, owner, pos)) continue;
                var state = level.getBlockState(pos); if (state.isAir() || state.getDestroySpeed(level, pos) < 0) continue;
                double force = 2.3 - Vec3.atCenterOf(pos).distanceToSqr(center);
                float resistance = Math.max(state.getBlock().getExplosionResistance(), state.getFluidState().isEmpty() ? 0 : state.getFluidState().getExplosionResistance());
                if (force - (resistance + .3) * .11 <= .01) continue;
                state.onExplosionHit(level, pos, explosion, (stack, drop) -> Block.popResource(level, drop, stack));
            }
        }
    }
}
