package io.github.jimbozoomer.jugcraft.chemistry;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.TallCropBlock;
import io.github.jimbozoomer.jugcraft.town.TownProtection;
import java.util.HashSet;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;

/** Bounded crop-only applications: no persistent fertility, yield multiplier or remote scanning. */
public final class SpecializedFertilizerItem extends Item {
    public enum Blend { NITROGEN, PHOSPHATE, MIXED }
    private static final TagKey<Block> NITROGEN = TagKey.create(Registries.BLOCK, Jugcraft.id("fertilizer_nitrogen_crops"));
    private static final TagKey<Block> PHOSPHATE = TagKey.create(Registries.BLOCK, Jugcraft.id("fertilizer_phosphate_crops"));
    private final Blend blend;
    private final String tooltip;

    public SpecializedFertilizerItem(Properties properties, Blend blend, String id) {
        super(properties);
        this.blend = blend;
        this.tooltip = "tooltip.jugcraft." + id;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> output, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, output, flag);
        output.accept(Component.translatable(tooltip));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level)) return InteractionResult.SUCCESS;
        var player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        Predicate<BlockPos> allowed = at -> player == null ? !TownProtection.shieldsBlock(level, at)
            : player.mayBuild() && !player.isSpectator() && level.mayInteract(player, at)
                && player.mayUseItemAt(at, Direction.UP, stack) && !TownProtection.denies(player, level, at);
        BlockPos center = bottom(level, context.getClickedPos());
        int grown = apply(level, center, allowed);
        if (player != null) player.sendOverlayMessage(grown == 0
            ? Component.translatable("message.jugcraft.fertilizer_no_growth")
            : Component.translatable("message.jugcraft.fertilizer_applied", stack.getHoverName(), grown));
        if (grown == 0) return InteractionResult.PASS;
        stack.consume(1, player);
        return InteractionResult.SUCCESS;
    }

    public int doses(BlockState crop) {
        return blend == Blend.MIXED ? 3 : crop.is(blend == Blend.NITROGEN ? NITROGEN : PHOSPHATE) ? 4 : 1;
    }

    private static BlockPos bottom(ServerLevel level, BlockPos at) {
        BlockState state = level.getBlockState(at);
        return state.getBlock() instanceof TallCropBlock ? at.below(state.getValue(TallCropBlock.SECTION)) : at;
    }

    /** Also used by sprinklers; each plant is treated once even when several tall sections intersect. */
    public int apply(ServerLevel level, BlockPos center, Predicate<BlockPos> allowed) {
        var visited = new HashSet<BlockPos>();
        int grown = 0;
        for (BlockPos cursor : BlockPos.betweenClosed(center.offset(-2, -1, -2), center.offset(2, 1, 2))) {
            if (!level.isLoaded(cursor)) continue;
            BlockPos at = bottom(level, cursor).immutable();
            if (!visited.add(at) || !level.isLoaded(at)) continue;
            BlockState crop = level.getBlockState(at);
            // Jugcraft's tall crops are intentionally separate from vanilla's low-crop tag.
            if (!(crop.is(BlockTags.CROPS) || crop.getBlock() instanceof TallCropBlock)
                    || !footprintAllowed(level, at, allowed)) continue;
            int doses = doses(crop);
            boolean any = false;
            for (int dose = 0; dose < doses; dose++) {
                if (BoneMealItem.growCrop(new ItemStack(Items.BONE_MEAL), level, at)) any = true;
            }
            if (any) {
                grown++;
                level.levelEvent(LevelEvent.PARTICLES_AND_SOUND_PLANT_GROWTH, at, 15);
            }
        }
        return grown;
    }

    private static boolean footprintAllowed(ServerLevel level, BlockPos at, Predicate<BlockPos> allowed) {
        // Tall crops grow upward; stems may produce fruit beside them. Do not grow across a protected border.
        for (int height = 0; height < 3; height++) {
            BlockPos part = at.above(height);
            if (!level.isLoaded(part) || !allowed.test(part)) return false;
        }
        for (Direction side : Direction.Plane.HORIZONTAL) {
            BlockPos part = at.relative(side);
            if (!level.isLoaded(part) || !allowed.test(part)) return false;
        }
        return true;
    }
}
