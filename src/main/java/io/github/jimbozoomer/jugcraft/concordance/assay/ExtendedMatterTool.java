package io.github.jimbozoomer.jugcraft.concordance.assay;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceEffects;
import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import io.github.jimbozoomer.jugcraft.concordance.effect.*;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Ordinary tools first; explicitly selected extra work is bounded, paid and performed as the real player. */
public final class ExtendedMatterTool extends Item {
    public enum Kind {
        PICKAXE("pickaxe", "vein", "tunnel"), AXE("axe", "fell"), HAMMER("hammer", "face"),
        SHOVEL("shovel", "face"), HOE("hoe", "cultivate"), SHEARS("shears", "shear"),
        SWORD("sword", "sweep"), KATAR("katar", "fell", "sweep"), MORNING_STAR("morning_star", "face", "tunnel");
        public final String id;
        final String[] modes;
        Kind(String id, String... modes) { this.id = id; this.modes = modes; }
    }
    public static final TagKey<Block> KATAR_BLOCKS = TagKey.create(Registries.BLOCK, Jugcraft.id("mineable/alchemical_katar"));
    public static final TagKey<Block> MORNING_STAR_BLOCKS = TagKey.create(Registries.BLOCK, Jugcraft.id("mineable/alchemical_morning_star"));
    private static final TagKey<Block> ORES = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("c", "ores"));
    private static final Set<UUID> EXPANDING = new HashSet<>();
    public final Kind kind;
    public final boolean red;
    public ExtendedMatterTool(Properties properties, Kind kind, boolean red) { super(properties); this.kind = kind; this.red = red; }
    public int mode(ItemStack stack) { return Math.floorMod(stack.getOrDefault(DarkMatter.MODE, 0), kind.modes.length + 1); }
    public int cost() { return kind == Kind.SWORD || kind == Kind.KATAR && kind.modes.length > 1 ? 16 : kind == Kind.HAMMER || kind == Kind.MORNING_STAR ? 12 : 8; }
    public String selected(ItemStack stack) { return mode(stack) == 0 ? "single" : kind.modes[mode(stack) - 1]; }

    @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer server && RateGate.allow(server, "extended_matter_mode", 4)) {
            var stack = player.getItemInHand(hand);
            stack.set(DarkMatter.MODE, (mode(stack) + 1) % (kind.modes.length + 1));
            server.sendOverlayMessage(Component.translatable("message.jugcraft.extended_matter.mode", stack.getHoverName(),
                    Component.translatable("message.jugcraft.extended_matter.mode." + selected(stack)), cost()));
        }
        return InteractionResult.SUCCESS;
    }

    @Override public InteractionResult useOn(UseOnContext context) {
        Player user = context.getPlayer();
        if (user == null) return InteractionResult.PASS;
        if (user.isShiftKeyDown()) return use(context.getLevel(), user, context.getHand());
        if (context.getLevel() instanceof ServerLevel level && user instanceof ServerPlayer player
                && !Authority.mayChange(level, player, context.getClickedPos())) return InteractionResult.FAIL;
        Item delegate = switch (kind) {
            case HOE -> Items.NETHERITE_HOE;
            case SHOVEL, MORNING_STAR -> Items.NETHERITE_SHOVEL;
            case AXE, KATAR -> Items.NETHERITE_AXE;
            case SHEARS -> Items.SHEARS;
            default -> null;
        };
        if (delegate == null) return InteractionResult.PASS;
        // Vanilla interaction consumes durability from the actual held stack and emits the ordinary game events.
        InteractionResult result = delegate.useOn(context);
        if (!result.consumesAction() && kind == Kind.KATAR) result = Items.NETHERITE_HOE.useOn(context);
        if (result.consumesAction() && kind == Kind.HOE && mode(context.getItemInHand()) > 0
                && user instanceof ServerPlayer player && AlchemicalGear.ready(player) && context.getHand() == InteractionHand.MAIN_HAND) {
            ItemStack stack = context.getItemInHand();
            BlockPos origin = context.getClickedPos();
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0 || stack.isEmpty() || player.getMainHandItem() != stack || AlchemyPower.available(player) < cost()) continue;
                BlockPos at = origin.offset(dx, 0, dz);
                if (!player.level().hasChunkAt(at) || !Authority.mayChange(player.level(), player, at)
                        || player.distanceToSqr(Vec3.atCenterOf(at)) > 64) continue;
                AlchemyPower.pay(player, cost());
                var next = new UseOnContext(player, context.getHand(), new BlockHitResult(Vec3.atCenterOf(at).add(0, .5, 0), Direction.UP, at, false));
                if (!Items.NETHERITE_HOE.useOn(next).consumesAction()) AlchemyPower.refund(player, cost());
            }
        }
        return result;
    }

    @Override public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miner) {
        boolean result = super.mineBlock(stack, level, state, pos, miner);
        if (level instanceof ServerLevel server && miner instanceof ServerPlayer player && mode(stack) > 0
                && !stack.isEmpty() && player.getMainHandItem() == stack && !player.isShiftKeyDown() && player.mayBuild()
                && !player.getAbilities().instabuild && AlchemicalGear.ready(player) && AlchemyPower.available(player) >= cost()
                && player.distanceToSqr(Vec3.atCenterOf(pos)) <= 64 && state.getDestroySpeed(level, pos) > 0
                && matches(state) && EXPANDING.add(player.getUUID())) {
            try { expand(stack, server, state, pos, player); } finally { EXPANDING.remove(player.getUUID()); }
        }
        return result;
    }

    public boolean matches(BlockState state) {
        return switch (kind) {
            case PICKAXE, HAMMER -> state.is(BlockTags.MINEABLE_WITH_PICKAXE);
            case AXE -> state.is(BlockTags.MINEABLE_WITH_AXE);
            case SHOVEL -> state.is(BlockTags.MINEABLE_WITH_SHOVEL);
            case HOE -> state.is(BlockTags.MINEABLE_WITH_HOE);
            case KATAR -> state.is(KATAR_BLOCKS);
            case MORNING_STAR -> state.is(MORNING_STAR_BLOCKS);
            case SHEARS -> state.is(BlockTags.LEAVES);
            default -> false;
        };
    }

    private void expand(ItemStack stack, ServerLevel level, BlockState original, BlockPos origin, ServerPlayer player) {
        String selected = selected(stack);
        float hardness = original.getDestroySpeed(level, origin);
        Direction direction = Math.abs(player.getXRot()) > 45 ? (player.getXRot() > 0 ? Direction.DOWN : Direction.UP) : player.getDirection();
        if (selected.equals("face")) {
            for (int a = -1; a <= 1; a++) for (int b = -1; b <= 1; b++) if (a != 0 || b != 0) {
                BlockPos next = switch (direction.getAxis()) { case X -> origin.offset(0, a, b); case Y -> origin.offset(a, 0, b); case Z -> origin.offset(a, b, 0); };
                breakExtra(stack, level, next, player, hardness);
            }
        } else if (selected.equals("tunnel")) {
            for (int depth = 1; depth <= 4; depth++) if (!breakExtra(stack, level, origin.relative(direction, depth), player, hardness)) break;
        } else if (selected.equals("fell") && original.is(BlockTags.LOGS) || selected.equals("vein") && original.is(ORES)
                || selected.equals("shear") && original.is(BlockTags.LEAVES)) {
            ArrayDeque<BlockPos> queue = new ArrayDeque<>(); Set<BlockPos> seen = new HashSet<>();
            queue.add(origin); seen.add(origin); int broken = 0, limit = selected.equals("fell") ? 24 : red ? 16 : 8;
            while (!queue.isEmpty() && seen.size() < 192 && broken < limit && AlchemyPower.available(player) >= cost()) {
                BlockPos from = queue.removeFirst();
                for (Direction direction2 : Direction.values()) {
                    BlockPos at = from.relative(direction2);
                    if (broken >= limit || seen.size() >= 192) break;
                    if (!seen.add(at) || Math.abs(at.getX() - origin.getX()) > 4 || Math.abs(at.getZ() - origin.getZ()) > 4
                            || Math.abs(at.getY() - origin.getY()) > (selected.equals("fell") ? 16 : 4)
                            || selected.equals("fell") && at.getY() < origin.getY() || !level.hasChunkAt(at)
                            || !level.getBlockState(at).is(original.getBlock())) continue;
                    if (breakExtra(stack, level, at, player, hardness)) { broken++; queue.add(at); }
                }
            }
        }
    }

    private boolean breakExtra(ItemStack stack, ServerLevel level, BlockPos pos, ServerPlayer player, float hardness) {
        if (stack.isEmpty() || player.getMainHandItem() != stack || AlchemyPower.available(player) < cost() || !level.hasChunkAt(pos)
                || !level.isInWorldBounds(pos)) return false;
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || state.hasBlockEntity() || !state.getFluidState().isEmpty() || !matches(state)
                || state.getDestroySpeed(level, pos) < 0 || state.getDestroySpeed(level, pos) > hardness + .001F
                || !isCorrectToolForDrops(stack, state) || !Authority.mayChange(level, player, pos)) return false;
        AlchemyPower.pay(player, cost());
        boolean broken = player.gameMode.destroyBlock(pos);
        if (!broken) AlchemyPower.refund(player, cost());
        return broken;
    }

    @Override public InteractionResult interactLivingEntity(ItemStack stack, Player user, LivingEntity target, InteractionHand hand) {
        if (kind != Kind.SHEARS && kind != Kind.KATAR || !(target instanceof Sheep sheep) || !sheep.readyForShearing()) return InteractionResult.PASS;
        if (!(user instanceof ServerPlayer player)) return InteractionResult.SUCCESS;
        if (player.distanceToSqr(sheep) > 36 || !shear(player, sheep, stack, hand)) return InteractionResult.FAIL;
        if (selected(stack).equals("shear") && !player.isShiftKeyDown() && AlchemicalGear.ready(player)) {
            int count = 0;
            for (Sheep other : player.level().getEntitiesOfClass(Sheep.class, sheep.getBoundingBox().inflate(3))) {
                if (count >= 6 || stack.isEmpty() || AlchemyPower.available(player) < cost()) break;
                if (other == sheep || !other.readyForShearing() || player.distanceToSqr(other) > 36) continue;
                AlchemyPower.pay(player, cost());
                if (shear(player, other, stack, hand)) count++; else AlchemyPower.refund(player, cost());
            }
        }
        return InteractionResult.SUCCESS;
    }

    private static boolean shear(ServerPlayer player, Sheep sheep, ItemStack stack, InteractionHand hand) {
        if (!player.isAlive() || !sheep.isAlive() || !Authority.mayChange(player.level(), player, sheep.blockPosition()) || !Authority.mayStrike(player, sheep)) return false;
        sheep.shear(player.level(), SoundSource.PLAYERS, stack);
        sheep.gameEvent(GameEvent.SHEAR, player);
        stack.hurtAndBreak(1, player, hand);
        return true;
    }

    @Override public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        super.hurtEnemy(stack, target, attacker);
        if (!(attacker instanceof ServerPlayer player) || !selected(stack).equals("sweep") || player.isShiftKeyDown()
                || !AlchemicalGear.ready(player) || !RateGate.allow(player, "matter_sweep", 12)) return;
        sweep(player, target, stack);
    }

    /** Nearby hostile creatures only: no accidental cleaving of pets, villagers or other players. */
    public int sweep(ServerPlayer player, LivingEntity original, ItemStack stack) {
        if (!player.isAlive() || player.isSpectator() || stack.isEmpty() || player.getMainHandItem() != stack || !selected(stack).equals("sweep") || !AlchemicalGear.ready(player)
                || player.distanceToSqr(original) > 36) return 0;
        int hits = 0;
        var cause = Cause.of(player.getUUID(), Cause.Origin.ITEM, "jugcraft:alchemical_sweep", ConcordanceEffects.nextSerial());
        var ledger = new Ledger(new Ledger.Limits(4, 4 * EffectKind.DAMAGE.work, 0));
        for (LivingEntity victim : player.level().getEntitiesOfClass(LivingEntity.class, original.getBoundingBox().inflate(2), e -> e instanceof Enemy && e != original && e.isAlive())) {
            if (hits >= 4 || AlchemyPower.available(player) < cost()) break;
            if (player.distanceToSqr(victim) > 36 || !player.hasLineOfSight(victim)) continue;
            var context = new ConcordanceEffects.Context(player.level(), cause, player, ledger, "cleave", player.position());
            AlchemyPower.pay(player, cost());
            if (ConcordanceEffects.apply(context, EffectSpec.of(EffectKind.DAMAGE, Intent.HARMFUL, red ? 4 : 2, 0), victim).applied()) hits++;
            else AlchemyPower.refund(player, cost());
        }
        return hits;
    }

    @Override public void appendHoverText(ItemStack stack, TooltipContext c, TooltipDisplay d, Consumer<Component> text, TooltipFlag f) {
        text.accept(Component.translatable(getDescriptionId().replace("item.", "tooltip.")).withStyle(ChatFormatting.GRAY));
        text.accept(Component.translatable("tooltip.jugcraft.extended_matter.controls",
                Component.translatable("message.jugcraft.extended_matter.mode." + selected(stack)), cost()).withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}
