package io.github.jimbozoomer.jugcraft.powerline;

import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * A power line structure in item form. Placing it builds the whole structure (lifting a tower onto its highest foot
 * on uneven ground) and wires it to the line being built; using it on a lower-tier support of its family upgrades
 * that support in place.
 */
public class LineItem extends BlockItem {
	public final LineKind kind;

	public LineItem(Block anchor, LineKind kind, Properties properties) {
		super(anchor, properties);
		this.kind = kind;
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		BlockPos clicked = context.getClickedPos();
		BlockPos structure = LineRegistry.of(level).anchorOf(clicked);
		if (structure != null) {
			if (level instanceof ServerLevel server && context.getPlayer() != null) {
				InteractionResult upgrade = LinePlacement.upgrade(server, structure, context.getPlayer(), context.getItemInHand(), kind);
				if (upgrade != InteractionResult.PASS) {
					return upgrade;
				}
			} else {
				return InteractionResult.SUCCESS;
			}
		}
		BlockPos at = level.getBlockState(clicked).canBeReplaced() ? clicked : clicked.relative(context.getClickedFace());
		if (!(level instanceof ServerLevel server)) {
			return InteractionResult.SUCCESS;
		}
		BlockPos anchor = LinePlacement.place(server, at, kind, context.getPlayer());
		if (anchor == null) {
			return InteractionResult.FAIL;
		}
		var sound = JugcraftPowerLines.ANCHORS.get(kind).defaultBlockState().getSoundType();
		level.playSound(null, anchor, sound.getPlaceSound(), net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 0.8F);
		if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild) {
			context.getItemInHand().shrink(1);
		}
		return InteractionResult.SUCCESS_SERVER;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, context, display, tooltip, flag);
		tooltip.accept(Component.translatable("tooltip.jugcraft.power_line.capacity", String.format("%,d", kind.capacity))
				.withStyle(net.minecraft.ChatFormatting.GRAY));
		if (!kind.isBox()) {
			tooltip.accept(Component.translatable("tooltip.jugcraft.power_line.size", kind.height, kind.reach)
					.withStyle(net.minecraft.ChatFormatting.GRAY));
		}
		tooltip.accept(Component.translatable("tooltip.jugcraft.power_line." + kind.id).withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
	}
}
