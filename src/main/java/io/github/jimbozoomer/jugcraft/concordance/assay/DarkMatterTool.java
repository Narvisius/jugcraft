package io.github.jimbozoomer.jugcraft.concordance.assay;

import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.concordance.RateGate;
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
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Paid extra blocks use the real player's break path: normal durability, loot, enchantments and protection events. */
public final class DarkMatterTool extends Item {
    public enum Kind {
        PICKAXE("pickaxe", 8, new String[]{"single", "vein", "tunnel"}),
        HAMMER("hammer", 16, new String[]{"single", "face"}),
        AXE("axe", 8, new String[]{"single", "fell"});
        public final String id;
        public final int cost;
        final String[] modes;
        Kind(String id, int cost, String[] modes) { this.id = id; this.cost = cost; this.modes = modes; }
    }
    public static final int VEIN_EXTRA = 8, TREE_EXTRA = 16, SCAN_LIMIT = 192;
    private static final TagKey<Block> ORES = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("c", "ores"));
    private static final Set<UUID> EXPANDING = new HashSet<>();
    public final Kind kind;
    DarkMatterTool(Properties properties, Kind kind) { super(properties); this.kind = kind; }

    public int mode(ItemStack stack) { return Math.floorMod(stack.getOrDefault(DarkMatter.MODE, 0), kind.modes.length); }
    private Component modeName(ItemStack stack) { return Component.translatable("message.jugcraft.dark_matter.mode." + kind.modes[mode(stack)]); }

    @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer server && RateGate.allow(server, "dark_matter_mode", 4)) {
            ItemStack stack = player.getItemInHand(hand);
            stack.set(DarkMatter.MODE, (mode(stack) + 1) % kind.modes.length);
            server.sendOverlayMessage(Component.translatable("message.jugcraft.dark_matter.mode", stack.getHoverName(), modeName(stack), kind.cost));
        }
        return InteractionResult.SUCCESS;
    }
    @Override public InteractionResult useOn(UseOnContext context) {
        return context.getPlayer() != null && context.getPlayer().isShiftKeyDown()
                ? use(context.getLevel(), context.getPlayer(), context.getHand()) : InteractionResult.PASS;
    }

    @Override public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miner) {
        boolean result = super.mineBlock(stack, level, state, pos, miner);
        if (level instanceof ServerLevel server && miner instanceof ServerPlayer player && mode(stack) != 0
                && !stack.isEmpty() && player.getMainHandItem() == stack && !player.isShiftKeyDown()
                && player.mayBuild() && !player.getAbilities().instabuild && AlchemicalGear.ready(player)
                && player.distanceToSqr(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5) <= 64
                && AlchemyPower.available(player) >= kind.cost && state.getDestroySpeed(level, pos) > 0
                && state.is(kind == Kind.AXE ? BlockTags.MINEABLE_WITH_AXE : BlockTags.MINEABLE_WITH_PICKAXE)
                && EXPANDING.add(player.getUUID())) {
            try { expand(stack, server, state, pos, player); }
            finally { EXPANDING.remove(player.getUUID()); }
        }
        return result;
    }

    private void expand(ItemStack stack, ServerLevel level, BlockState original, BlockPos origin, ServerPlayer player) {
        Direction direction = Math.abs(player.getXRot()) > 45 ? (player.getXRot() > 0 ? Direction.DOWN : Direction.UP) : player.getDirection();
        float hardness = original.getDestroySpeed(level, origin);
        if (kind == Kind.HAMMER) {
            for (int a = -1; a <= 1; a++) for (int b = -1; b <= 1; b++) if (a != 0 || b != 0) {
                BlockPos next = switch (direction.getAxis()) {
                    case X -> origin.offset(0, a, b); case Y -> origin.offset(a, 0, b); case Z -> origin.offset(a, b, 0);
                };
                breakExtra(stack, level, next, player, hardness);
            }
        } else if (kind == Kind.PICKAXE && mode(stack) == 2) {
            for (int depth = 1; depth <= 2; depth++)
                if (!breakExtra(stack, level, origin.relative(direction, depth), player, hardness)) break;
        } else if (kind == Kind.AXE ? original.is(BlockTags.LOGS) : original.is(ORES)) {
            connected(stack, level, original.getBlock(), origin, player, hardness);
        }
    }

    private void connected(ItemStack stack, ServerLevel level, Block block, BlockPos origin, ServerPlayer player, float hardness) {
        ArrayDeque<BlockPos> queue = new ArrayDeque<>(); Set<BlockPos> seen = new HashSet<>();
        queue.add(origin); seen.add(origin); int broken = 0;
        int limit = kind == Kind.AXE ? TREE_EXTRA : VEIN_EXTRA;
        while (!queue.isEmpty() && broken < limit && seen.size() < SCAN_LIMIT && AlchemyPower.available(player) >= kind.cost) {
            BlockPos from = queue.removeFirst();
            // Six-way connectivity; trees also accept upward diagonals, but never follow roots downward.
            for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) for (int dz = -1; dz <= 1; dz++) {
                if (kind == Kind.AXE ? dy < 0 || dx == 0 && dy == 0 && dz == 0 : Math.abs(dx) + Math.abs(dy) + Math.abs(dz) != 1) continue;
                if (broken >= limit || seen.size() >= SCAN_LIMIT) return;
                BlockPos next = from.offset(dx, dy, dz);
                if (Math.abs(next.getX() - origin.getX()) > 4 || Math.abs(next.getZ() - origin.getZ()) > 4
                        || Math.abs(next.getY() - origin.getY()) > (kind == Kind.AXE ? 16 : 4) || !seen.add(next)
                        || !level.hasChunkAt(next) || !level.getBlockState(next).is(block)) continue;
                if (breakExtra(stack, level, next, player, hardness)) { broken++; queue.add(next); }
            }
        }
    }

    private boolean breakExtra(ItemStack stack, ServerLevel level, BlockPos pos, ServerPlayer player, float hardness) {
        if (!level.hasChunkAt(pos) || !level.isInWorldBounds(pos) || player.getMainHandItem() != stack || stack.isEmpty()
                || AlchemyPower.available(player) < kind.cost) return false;
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || state.hasBlockEntity() || !state.getFluidState().isEmpty()
                || !state.is(kind == Kind.AXE ? BlockTags.MINEABLE_WITH_AXE : BlockTags.MINEABLE_WITH_PICKAXE)
                || state.getDestroySpeed(level, pos) < 0 || state.getDestroySpeed(level, pos) > hardness + .001F
                || !isCorrectToolForDrops(stack, state) || !Authority.mayChange(level, player, pos)) return false;
        // Reserve before callbacks, refund only a refused break. Other ledger changes are not overwritten.
        AlchemyPower.pay(player, kind.cost);
        boolean broken = player.gameMode.destroyBlock(pos);
        if (!broken) AlchemyPower.refund(player, kind.cost);
        return broken;
    }

    @Override public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
            Consumer<Component> text, TooltipFlag flag) {
        text.accept(Component.translatable(getDescriptionId().replace("item.", "tooltip.")).withStyle(ChatFormatting.GRAY));
        text.accept(Component.translatable("tooltip.jugcraft.dark_matter.mode", modeName(stack)).withStyle(ChatFormatting.LIGHT_PURPLE));
        text.accept(Component.translatable("tooltip.jugcraft.dark_matter.controls", kind.cost).withStyle(ChatFormatting.GRAY));
    }
}
